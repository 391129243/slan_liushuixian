// Live browser acceptance; imports the explicitly synthetic rules from the last successful --bundle run.
// Requires start-dataserver-offline.bat and Vue on 5173. Uses the real offline SDK, no instrument.
import { chromium, expect } from '../frontend/node_modules/@playwright/test/index.mjs';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const advanced=process.argv.includes('--advanced');
const proof=JSON.parse(readFileSync(join(root,'test-reports/real-sdk-bundle-2026-11-23.json'),'utf8'));
if(proof.status!=='PASS')throw new Error('Run simulate-real-sdk-reanalysis.mjs --bundle first');
const bytes=readFileSync(proof.source),sha=b=>createHash('sha256').update(b).digest('hex');
const payload=JSON.parse(readFileSync(join(proof.runDirectory,'android-experiment.json'),'utf8'));
payload.Name='SIMULATION CT-PEAK RULE CHECK'; // Never present these rules as the original experiment rules.
const prefix='http://127.0.0.1:8088/api/admin';
const headers={Authorization:'Basic '+Buffer.from('admin:'+(process.env.DATASERVER_ADMIN_PASSWORD||'12345678')).toString('base64')};
const manifest={version:1,files:[]},form=new FormData();
form.append('experiment',JSON.stringify(payload));
for(const [name,role,data]of [[proof.source.split(/[\\/]/).pop(),'PCR',bytes],...['fixture.ct','fixture.peak','fixture.conclusion','judgeConclusion.result'].map(name=>[name,name.endsWith('.ct')?'CT_RULE':name.endsWith('.peak')?'PEAK_RULE':name.endsWith('.conclusion')?'CONCLUSION_ARCHIVE':'DEVICE_CONCLUSION',readFileSync(join(proof.runDirectory,'device-result',name))])]) {
  manifest.files.push({fileName:name,role,size:data.length,sha256:sha(data)});form.append(role==='PCR'?'file':'attachments',new Blob([data]),name);
}
form.append('manifest',JSON.stringify(manifest));
const uploaded=await fetch(prefix+'/bundles',{method:'POST',headers,body:form});
if(uploaded.status!==200)throw new Error(await uploaded.text());
const reply=await uploaded.json();
const browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROME||'C:/Program Files/Google/Chrome/Application/chrome.exe'});
const page=await browser.newPage({viewport:{width:1440,height:1100}});
const previews=join(root,'previews');mkdirSync(previews,{recursive:true});
const evidence={status:'RUNNING',scope:'Live Vue -> Java -> C# -> real offline SDK with explicit synthetic CT/PEAK rules',bundleId:reply.BundleID,reportId:reply.ReportID,checks:{}};
try {
  await page.goto('http://127.0.0.1:5173/?mode=server');
  await page.getByLabel('数据中心密码',{exact:true}).fill(process.env.DATASERVER_ADMIN_PASSWORD||'12345678');
  await page.getByRole('button',{name:'登录数据中心',exact:true}).click();
  await page.locator('tr').filter({hasText:payload.Name}).first().getByRole('button',{name:'查看报告',exact:true}).click();
  await expect(page.getByRole('table',{name:'规则与结论附件'}).getByRole('row')).toHaveCount(5);
  await page.getByRole('button',{name:'读取输入文件',exact:true}).click();
  await expect(page.getByRole('status')).toContainText('已读取 SDK 结果',{timeout:20000});
  if(advanced) {
    await page.getByText('编辑判读规则与 CT / PEAK 综合结论',{exact:true}).click();
    await page.getByLabel('文件 1 通道 1 项目 1 上偏移',{exact:true}).fill('2');
    await page.getByLabel('文件 1 通道 1 项目 1 可疑扩展',{exact:true}).fill('3');
    await page.getByRole('button',{name:'新增综合项目',exact:true}).click();
    await page.getByLabel('新规则版本名称',{exact:true}).fill('SIMULATION UI advanced rules');
    const creating=page.waitForResponse(r=>r.url().endsWith('/api/admin/pcr/rules')&&r.request().method()==='POST');
    await page.getByRole('button',{name:'保存并使用新规则版本',exact:true}).click();
    const created=await creating;expect(created.status()).toBe(200);evidence.ruleVersionId=(await created.json()).id;
    await expect(page.getByLabel('使用规则版本',{exact:true})).toHaveValue(evidence.ruleVersionId);
    await page.getByLabel('按项目和通道配置高级参数',{exact:true}).check();
    await page.getByLabel('选择项目 1 靶标 1',{exact:true}).check();
    await page.getByLabel('项目 1 靶标 1 扩增荧光阈值',{exact:true}).fill('0.24');
    evidence.checks.ruleVersionCreatedFromUi=true;
  }
  const analyzing=page.waitForResponse(r=>r.url().endsWith('/api/admin/pcr/reanalyze')&&r.request().method()==='POST');
  await page.getByRole('button',{name:'执行服务器二次分析',exact:true}).click();
  await expect(page.getByRole('status')).toContainText('结果已保存到数据库',{timeout:20000});
  const analyzed=await analyzing;expect(analyzed.status()).toBe(200);const result=await analyzed.json();
  if(advanced) {
    expect(result.ruleVersionId).toBe(evidence.ruleVersionId);expect(result.targetsChanged).toBe(1);
    expect(result.parameterSets[0].Targets[0].Paramter.Threshold).toBe(0.24);
    await expect(page.getByRole('table',{name:'综合判读结果',exact:true}).getByRole('row')).toHaveCount(9);
    expect(result.combinedConclusions.rows.every(r=>r.status==='UNDETERMINED')).toBe(true);
    evidence.checks.selectedTargetAndExplicitCombinedResultsDisplayed=true;
  }
  const table=page.getByRole('table',{name:'本版本规则判读',exact:true});
  await expect(table.getByRole('row')).toHaveCount(17);
  const a1=table.getByRole('row').filter({hasText:'A1 / FAM'}).filter({hasText:'SIMULATION CT ONLY'});
  await expect(a1).toContainText('39.053621906411');await expect(a1).toContainText('可疑样本');
  await expect(table.getByText('未提供熔解峰数据',{exact:true})).toHaveCount(8);
  evidence.checks.realSdkCtRejudgedAndMissingMeltExplicit=true;
  const filename='ui-rule-check-'+reply.BundleID.slice(0,8)+'.pcr';
  await page.getByLabel('分析输出文件名',{exact:true}).fill(filename);
  const saved=page.waitForResponse(r=>r.url().endsWith('/api/admin/pcr/save')&&r.request().method()==='POST');
  await page.getByRole('button',{name:'保存新 .pcr',exact:true}).click();
  const savedResponse=await saved;expect(savedResponse.status()).toBe(200);evidence.analysisRunId=(await savedResponse.json()).analysisRunId;
  await expect(page.getByRole('button',{name:'下载分析结果包',exact:true})).toBeEnabled();
  if(advanced) {
    await page.locator('.sdk-editor').screenshot({path:join(previews,'advanced-sdk-parameters.png')});
    const editor=page.locator('details.rule-editor');if(await editor.getAttribute('open')===null)await editor.locator('summary').click();
    await editor.screenshot({path:join(previews,'advanced-rule-editor.png')});
  }
  await page.screenshot({path:join(previews,'result-bundle-rules-desktop.png'),fullPage:true});
  if(advanced)await page.screenshot({path:join(previews,'advanced-rules-desktop.png'),fullPage:true});
  await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path:join(previews,'result-bundle-rules-mobile.png'),fullPage:true});
  if(advanced)await page.screenshot({path:join(previews,'advanced-rules-mobile.png'),fullPage:true});
  await page.getByRole('tab',{name:'分析版本',exact:true}).click();
  await page.locator('tr').filter({hasText:evidence.analysisRunId.slice(0,8)}).getByRole('button',{name:'查看版本',exact:true}).click();
  await expect(a1).toContainText('可疑样本');evidence.checks.historyAndResponsiveLayout=true;
  expect(sha(readFileSync(proof.source))).toBe(proof.sourceSha256);evidence.checks.originalUnchanged=true;evidence.status='PASS';
  console.log(JSON.stringify(evidence,null,2));
}catch(error){evidence.status='FAIL';evidence.error=String(error);await page.screenshot({path:join(previews,'result-bundle-rules-error.png'),fullPage:true});throw error;}
finally{writeFileSync(join(root,'test-reports',advanced?'advanced-ui-2026-11-23.json':'result-bundle-ui-2026-11-23.json'),JSON.stringify(evidence,null,2));await browser.close();}
