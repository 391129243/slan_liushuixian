// Optional live browser acceptance. Requires start-dataserver-offline.bat and the Vue dev server.
// Uses the actual supplied PCR and real offline SDK. Creates a version in the isolated offline server DB.
import { chromium, expect } from '../frontend/node_modules/@playwright/test/index.mjs';
import { resolve,dirname,join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { readdirSync,readFileSync,writeFileSync,mkdirSync } from 'node:fs';
import { createHash } from 'node:crypto';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const folder=readdirSync(join(root,'result'),{withFileTypes:true}).find(e=>e.isDirectory()&&e.name.includes('单重'));
const source=join(root,'result',folder.name,'2026-10-29-17-15-07.pcr');
const sha=p=>createHash('sha256').update(readFileSync(p)).digest('hex');
const beforeHash=sha(source),browser=await chromium.launch({headless:true,executablePath:process.env.PLAYWRIGHT_CHROME||'C:/Program Files/Google/Chrome/Application/chrome.exe'});
const page=await browser.newPage({viewport:{width:1440,height:1100}});
const preview=join(root,'previews');mkdirSync(preview,{recursive:true});
const evidence={status:'RUNNING',scope:'Actual browser -> Java -> C# -> real offline SLAN SDK',sourceSha256:beforeHash,checks:{}};
try{
  async function login(){
    await page.goto('http://127.0.0.1:5173/?mode=server');
    await page.getByLabel('数据中心密码',{exact:true}).fill('12345678');
    await page.getByRole('button',{name:'登录数据中心',exact:true}).click();
    await expect(page.getByText('已连接数据服务')).toBeVisible({timeout:15000});
    await expect(page.getByRole('button',{name:'上传 .pcr',exact:true})).toBeEnabled();
  }
  await login();
  await page.getByLabel('上传服务器 PCR 文件',{exact:true}).setInputFiles(source);
  await expect(page.getByRole('status')).toContainText(/文件已上传|文件已存在/,{timeout:15000});
  const row=page.locator('tr').filter({hasText:'2026-10-29-17-15-07.pcr'}).first();
  await row.getByRole('button',{name:'读取分析',exact:true}).click();
  await expect(page.getByRole('status')).toContainText('已读取 SDK 结果',{timeout:20000});
  const table=page.getByRole('table',{name:'服务器靶标结果'});
  await expect(table.getByRole('row')).toHaveCount(25);
  await expect(table.getByRole('row').nth(1).locator('td').nth(2)).toHaveText('37.865534');
  await expect(table.getByText('—',{exact:true})).toHaveCount(3);evidence.checks.actualCtAndMissingnessDisplayed=true;
  await expect(page.getByRole('img',{name:'真实扩增实验曲线，横轴为循环数，纵轴为 SDK 信号'})).toBeVisible();
  await expect(table.getByRole('row').nth(1).locator('td').nth(3)).toHaveText('42');evidence.checks.sdkNumericCurveArraysDisplayed=true;
  await page.getByLabel('服务器荧光阈值',{exact:true}).fill('0.24');
  const analyzedResponse=page.waitForResponse(r=>r.url().endsWith('/api/admin/pcr/reanalyze')&&r.request().method()==='POST');
  await page.getByRole('button',{name:'执行服务器二次分析',exact:true}).click();
  const analyzed=await analyzedResponse;expect(analyzed.status()).toBe(200);evidence.analysisRunId=(await analyzed.json()).analysisRunId;
  await expect(page.getByRole('status')).toContainText('结果已保存到数据库',{timeout:20000});
  await expect(table.getByRole('row').nth(1).locator('td').nth(2)).toHaveText('39.053622');evidence.checks.actualSdkCtChangeDisplayed=true;
  await expect(table.getByRole('row').nth(1).locator('td').nth(3)).toHaveText('42');
  await page.getByLabel('分析输出文件名',{exact:true}).fill('ui-singleplex-reanalyzed.pcr');
  const savedResponse=page.waitForResponse(r=>r.url().endsWith('/api/admin/pcr/save')&&r.request().method()==='POST');
  await page.getByRole('button',{name:'保存新 .pcr',exact:true}).click();
  const saved=await savedResponse;expect(saved.status()).toBe(200);evidence.outputFileId=(await saved.json()).FileID;
  await expect(page.getByText('保存账号：admin')).toBeVisible();
  const downloading=page.waitForEvent('download');await page.getByRole('button',{name:'下载结果 .pcr',exact:true}).click();
  const download=await downloading;expect(readFileSync(await download.path()).length).toBeGreaterThan(100000);
  evidence.outputSha256=sha(await download.path());evidence.checks.savedPcrDownloaded=true;
  await page.getByRole('tab',{name:'分析版本',exact:true}).click();
  await page.locator('tr').filter({hasText:evidence.analysisRunId.slice(0,8)}).getByRole('button',{name:'查看版本',exact:true}).click();
  await expect(table.getByRole('row').nth(1).locator('td').nth(2)).toHaveText('39.053622');
  await expect(page.getByRole('button',{name:'下载结果 .pcr',exact:true})).toBeEnabled();
  await page.screenshot({path:join(preview,'data-center-live-desktop.png'),fullPage:true});
  await page.setViewportSize({width:390,height:844});
  expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path:join(preview,'data-center-live-mobile.png'),fullPage:true});evidence.checks.historyAndMobileLayout=true;
  await page.reload();await expect(page.getByRole('heading',{name:'登录数据中心',exact:true})).toBeVisible();
  await login();await page.getByRole('tab',{name:'分析版本',exact:true}).click();
  await page.locator('tr').filter({hasText:evidence.analysisRunId.slice(0,8)}).getByRole('button',{name:'查看版本',exact:true}).click();
  await expect(table.getByRole('row').nth(1).locator('td').nth(2)).toHaveText('39.053622');evidence.checks.reloadAndReauthentication=true;
  const storage=await page.evaluate(()=>JSON.stringify({local:{...localStorage},session:{...sessionStorage}}));
  expect(storage).not.toContain('12345678');expect(storage).not.toContain('Basic ');evidence.checks.passwordNotStoredInBrowser=true;
  expect(sha(source)).toBe(beforeHash);evidence.checks.originalPcrUnchanged=true;evidence.status='PASS';evidence.finishedAt=new Date().toISOString();
  console.log(JSON.stringify(evidence,null,2));
}catch(error){evidence.status='FAIL';evidence.error=String(error);evidence.pageError=await page.locator('.dc-alert').textContent().catch(()=>null);await page.screenshot({path:join(preview,'data-center-live-error.png'),fullPage:true});throw error;}
finally{writeFileSync(join(root,'test-reports','data-center-live-2026-11-23.json'),JSON.stringify(evidence,null,2));await browser.close();}
