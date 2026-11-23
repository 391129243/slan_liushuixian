// Real vendor offline SDK + real Java/C# processes. Only the vendor HTTP transport is an isolated test host.
// Prepare ReadPcrOffline.exe/vendor-experiment.json with read-pcr-offline.ps1 first.
import { createServer } from 'node:http';
import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { once } from 'node:events';
import { readFileSync,writeFileSync,mkdirSync,copyFileSync,openSync,closeSync,appendFileSync,readdirSync } from 'node:fs';
import { resolve,join,dirname,basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import { homedir } from 'node:os';
import { createHash,randomBytes } from 'node:crypto';
import assert from 'node:assert/strict';
import { verifyAdvancedSdk } from './verify-advanced-sdk.mjs';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const serving=process.argv.includes('--serve');
const advancedMode=process.argv.includes('--advanced');
const bundleMode=process.argv.includes('--bundle')||advancedMode;
const output=resolve(process.argv.slice(2).find(arg=>!arg.startsWith('--'))||join(root,'target/real-sdk-reanalysis-20261123'));
const sampleFolder=readdirSync(join(root,'result'),{withFileTypes:true}).find(e=>e.isDirectory()&&e.name.includes('单重'));
assert.ok(sampleFolder,'Singleplex reference directory required');
const source=join(root,'result',sampleFolder.name,'2026-10-29-17-15-07.pcr');
const vendor=join(root,'SLAN_9.0.1.260921_API_接口文档与完整程序_20261116','SLAN_9.0.1.260921_API_接口文档与完整程序','SLAN 9.0.1.260921_API');
const run=serving?join(root,'runtime','offline-server'):join(output,'roundtrip-'+Date.now());mkdirSync(run,{recursive:true});
const hash=b=>createHash('sha256').update(b).digest('hex');
const raw=readFileSync(source),sourceSha256=hash(raw);
assert.equal(sourceSha256,'e294c024e1a41a30cb747dd95d4742d3d448049561ea529a21c9bc19123021a7');
const calls=[],pending=new Map(),processes=[];let nextId=0,worker,java,bridge;
const sidecarBytes=new Map();
function processLog(name){return openSync(join(run,name+'.log'),'a');}
function launch(name,command,args,env={}){
  const fd=processLog(name);const child=spawn(command,args,{cwd:run,windowsHide:true,stdio:['ignore',fd,fd],env:{...process.env,...env}});closeSync(fd);processes.push(child);return child;
}
async function stop(child){if(child&&child.exitCode===null&&child.signalCode===null){const end=once(child,'exit');child.kill();await end;}}
async function port(){const s=createServer();s.listen(0,'127.0.0.1');await once(s,'listening');const p=s.address().port;await new Promise(r=>s.close(r));return p;}
async function ready(url,child){for(let i=0;i<600;i++){if(child.exitCode!==null)throw Error('Process exited; inspect '+run);try{if((await fetch(url,{signal:AbortSignal.timeout(1000)})).ok)return;}catch{}await new Promise(r=>setTimeout(r,200));}throw Error('Readiness timeout '+url);}
function rpc(method,body={}){
  return new Promise((resolve,reject)=>{
    const id=String(++nextId),timer=setTimeout(()=>{pending.delete(id);reject(Error('SDK operation timed out: '+method));},40000);
    pending.set(id,{resolve:r=>{clearTimeout(timer);resolve(r);},reject:e=>{clearTimeout(timer);reject(e);}});
    worker.stdin.write(JSON.stringify({id,method,body})+'\n');
  });
}
async function runCommand(name,command,args){
  const child=launch(name,command,args);const [code]=await once(child,'exit');assert.equal(code,0,name+' failed; inspect '+run);
}
function gradleJar(group,artifact,version){
  const folder=join(process.env.GRADLE_USER_HOME||join(homedir(),'.gradle'),'caches/modules-2/files-2.1',group,artifact,version);
  for(const hashDir of readdirSync(folder))for(const name of readdirSync(join(folder,hashDir)))if(name===artifact+'-'+version+'.jar')return join(folder,hashDir,name);
  throw Error('Cached Android dependency missing: '+artifact);
}
const evidence={status:'RUNNING',scope:'REAL vendor Service.SLANService offline APIs; test HTTP transport, real Java and C# bridge; no physical device or existing vendor process modification',
  source,sourceBytes:raw.length,sourceSha256,serviceDllSha256:hash(readFileSync(join(vendor,'Service.dll'))),
  parameters:{autoBaseline:true,baselineStart:6,baselineEnd:12,threshold:0.24},checks:{},runDirectory:run};
const transport=createServer(async(req,res)=>{
  try{
    let text='';for await(const b of req)text+=b;
    const method=req.url.split('/').at(-1),body=JSON.parse(text||'{}');
    if(bundleMode&&!advancedMode&&['LoadExperimentResult','OpenExperiment'].includes(method))for(const [name,bytes]of sidecarBytes)assert.deepEqual(readFileSync(join(dirname(body.file),name)),bytes,'Rule snapshot must be beside private SDK PCR');
    const result=await rpc(method,body);calls.push({method,errorCode:result?.ErrorCode??null,message:result?.Message??null});
    if(method==='GetExperimentParamters')writeFileSync(join(run,'sdk-original-parameters.json'),JSON.stringify(result,null,2));
    if(method==='SetExperimentParamter')writeFileSync(join(run,'sdk-submitted-parameters.json'),JSON.stringify(body.paramter,null,2));
    res.setHeader('Content-Type','application/json');res.end(JSON.stringify({[method+'Result']:result}));
  }catch(error){appendFileSync(join(run,'sdk-errors.log'),String(error)+'\n');res.writeHead(500,{'Content-Type':'application/json'});res.end(JSON.stringify({message:String(error)}));}
});
try{
  if(serving)for(const fixedPort of [8088,5099]){
    const probe=createServer();
    await new Promise((resolve,reject)=>{probe.once('error',()=>reject(Error('Port '+fixedPort+' is already in use; existing services were not stopped.')));probe.listen(fixedPort,'127.0.0.1',resolve);});
    await new Promise(r=>probe.close(r));
  }
  worker=spawn(join(output,'ReadPcrOffline.exe'),[vendor,'--stdio',run],{cwd:output,windowsHide:true,stdio:['pipe','pipe','pipe']});
  let markReady;const initialized=new Promise(r=>markReady=r);
  createInterface({input:worker.stdout}).on('line',line=>{
    if(line==='PCR_RPC_READY')markReady();
    else if(line.startsWith('PCR_VENDOR_METADATA:'))evidence.vendorMetadata=JSON.parse(line.slice('PCR_VENDOR_METADATA:'.length));
    else if(line.startsWith('PCR_RPC:')){
      const response=JSON.parse(line.slice(8)),waiting=pending.get(response.id);pending.delete(response.id);
      if(waiting)response.error?waiting.reject(Error(response.error)):waiting.resolve(response.result);
    }else appendFileSync(join(run,'worker.stdout.log'),line+'\n');
  });
  worker.stderr.on('data',b=>appendFileSync(join(run,'worker.stderr.log'),b));
  worker.on('exit',()=>{for(const waiting of pending.values())waiting.reject(Error('SDK worker exited'));pending.clear();});
  const initTimeout=setTimeout(()=>worker.kill(),20000);
  await Promise.race([initialized,once(worker,'exit').then(()=>{throw Error('SDK initialization failed');})]);clearTimeout(initTimeout);
  transport.listen(0,'127.0.0.1');await once(transport,'listening');
  const bridgePort=serving?5099:await port(),javaPort=serving?8088:await port(),password=serving?(process.env.DATASERVER_ADMIN_PASSWORD||'12345678'):randomBytes(24).toString('hex');
  const bridgeBase='http://127.0.0.1:'+bridgePort,base='http://127.0.0.1:'+javaPort;
  const input=join(run,'files'),work=join(run,'work');mkdirSync(input,{recursive:true});
  bridge=launch('bridge','dotnet',[join(root,'target/csharp-bridge/PcrSdkBridge.dll')],{PCR_BRIDGE_URL:bridgeBase,PCR_VENDOR_ENABLED:'true',
    PCR_VENDOR_URL:'http://127.0.0.1:'+transport.address().port+'/SlanServerService',PCR_INPUT_ROOT:input,PCR_WORK_ROOT:work});
  await ready(bridgeBase+'/health',bridge);
  const jdk=process.env.JAVA_HOME||join(homedir(),'.jdks','jbr-21.0.11');
  const javaEnv={DATASERVER_ADMIN_PASSWORD:password,DATASERVER_PORT:String(javaPort),DATASERVER_BIND:'127.0.0.1',
    DATASERVER_MANAGED_ENABLED:'false',DATASERVER_CSHARP_URL:bridgeBase,DATASERVER_STORAGE:input,DATASERVER_LEGACY_UPLOAD_ENABLED:'true',
    SPRING_PROFILES_ACTIVE:'default',SPRING_DATASOURCE_URL:'jdbc:h2:file:'+join(run,'db','real-sdk').replaceAll('\\','/')+';MODE=MySQL;DATABASE_TO_LOWER=TRUE;WRITE_DELAY=0',
    SPRING_DATASOURCE_USERNAME:'sa',SPRING_DATASOURCE_PASSWORD:''};
  async function startJava(){java=launch('java',join(jdk,'bin','java.exe'),['-jar',join(root,'target/dataserver-0.1.0.jar')],javaEnv);await ready(base+'/actuator/health',java);}
  await startJava();
  const headers={Authorization:'Basic '+Buffer.from('admin:'+password).toString('base64'),'Content-Type':'application/json'};
  async function api(path,body){const response=await fetch(base+'/api/admin/'+path,{method:body===undefined?'GET':'POST',headers,body:body===undefined?undefined:JSON.stringify(body)});assert.equal(response.status,200,await response.clone().text());return response.json();}
  if(serving){
    console.log('Offline SDK server ready: http://127.0.0.1:8088');
    console.log('Open Vue data center: http://127.0.0.1:5173/?mode=server');
    console.log('Real vendor offline SDK; no instrument calls. Database: runtime/offline-server/db/real-sdk.mv.db');
    console.log('Press Ctrl+C to stop the processes started by this launcher.');
    await new Promise(resolve=>{process.once('SIGINT',resolve);process.once('SIGTERM',resolve);process.once('SIGHUP',resolve);});
    evidence.status='STOPPED';
  }else{
  const native=JSON.parse(readFileSync(join(output,'vendor-experiment.json'),'utf8'));
  const mappings={SerialNumber:'SerialNo',UseModule:'Block',Cn:'CnValue',Rn:'RnValue',Wm:'Width',Ym:'YValue'};
  const numeric=new Set(['Ct','CtMean','CtSD','CtCV','Cn','CnValue','CnMean','CnSD','CnCV','Rn','RnValue','Tm','Rm','Wm','Ym','Width','YValue','X','Y']);
  function normalize(v,key=''){if(numeric.has(key)&&typeof v==='string'&&['NaN','Infinity','-Infinity'].includes(v))return null;if(Array.isArray(v))return v.map(x=>normalize(x));if(v&&typeof v==='object')return Object.fromEntries(Object.entries(v).map(([k,x])=>[mappings[k]||k,normalize(x,k)]));return v;}
  const payload=normalize(native);
  let reportId,fileId,bundleId;
  if(bundleMode){
    const device=join(run,'device-result'),cache=join(run,'android-cache');mkdirSync(device,{recursive:true});mkdirSync(cache,{recursive:true});copyFileSync(source,join(device,basename(source)));
    for(const name of readdirSync(dirname(source)))if(/\.(ct|peak|conclusion)$/i.test(name)||name.toLowerCase()==='judgeconclusion.result')sidecarBytes.set(name,readFileSync(join(dirname(source),name)));
    if(![...sidecarBytes.keys()].some(n=>n.endsWith('.ct')))sidecarBytes.set('fixture.ct',Buffer.from(JSON.stringify([{Channel:'FAM',Category:'SIMULATION CT ONLY',CTvalue:35,CTvalueDown:35,CTvalueUP:3,CTvalueUP_Suspicious:2,IsSelected:false}])));
    if(![...sidecarBytes.keys()].some(n=>n.endsWith('.peak')))sidecarBytes.set('fixture.peak',Buffer.from(JSON.stringify([{Channel:'FAM',PEAKSingleRuleEntityList:[{Category:'SIMULATION PEAK ONLY',PEAKvalue:80,PEAKvalueDown:1,PEAKvalueUP:1,PEAKvalueRM:100}]}])));
    if(![...sidecarBytes.keys()].some(n=>n.endsWith('.conclusion')))sidecarBytes.set('fixture.conclusion',Buffer.from('Explicit test conclusion attachment; not clinical rules.'));
    for(const [name,bytes]of sidecarBytes)writeFileSync(join(device,name),bytes);
    evidence.ruleFixtureOrigin='Original judgeConclusion.result copied unchanged; missing CT/PEAK/conclusion attachments are explicit private test fixtures, not recovered original rules.';
    const experimentFile=join(run,'android-experiment.json');writeFileSync(experimentFile,JSON.stringify(payload));
    const android=resolve(process.env.PCR_ANDROID_PROJECT||join(root,'../../..','Bio_PCR_20230927','Bio_PCR'));
    const classpath=[gradleJar('com.google.code.gson','gson','2.8.9'),gradleJar('com.squareup.okhttp3','okhttp','3.12.8'),gradleJar('com.squareup.okio','okio','1.15.0')].join(';');
    const classes=join(run,'android-classes');mkdirSync(classes,{recursive:true});
    await runCommand('android-compile',join(jdk,'bin','javac.exe'),['-encoding','UTF-8','--release','8','-cp',classpath,'-d',classes,join(root,'scripts/java/AndroidBundleUploadProbe.java'),
      ...['SafeExperimentJson','ResultBundleSnapshot','ResultBundleUploader'].map(n=>join(android,'app/src/main/java/com/gidi/bio_pcr/util',n+'.java'))]);
    const replyFile=join(run,'android-upload-reply.json');
    async function androidUpload(){await runCommand('android-upload',join(jdk,'bin','java.exe'),['-cp',classes+';'+classpath,'AndroidBundleUploadProbe',experimentFile,join(device,basename(source)),cache,base+'/upload',replyFile]);return JSON.parse(readFileSync(replyFile,'utf8'));}
    const first=await androidUpload(),retry=await androidUpload();assert.equal(first.BundleID,retry.BundleID);assert.equal(retry.Duplicate,true);
    reportId=first.ReportID;fileId=first.FileID;bundleId=first.BundleID;evidence.bundleId=bundleId;
    assert.equal(readdirSync(cache).length,0);evidence.checks.actualAndroidUploaderAndRetry=true;
    const archived=await api('bundles/'+bundleId);assert.equal(archived.manifest.files.length,sidecarBytes.size+1);
    for(const [name,bytes]of sidecarBytes){const response=await fetch(base+'/api/admin/bundles/'+bundleId+'/attachments/'+encodeURIComponent(name),{headers});assert.equal(response.status,200);assert.deepEqual(Buffer.from(await response.arrayBuffer()),bytes);}
    evidence.checks.ruleAndConclusionBytesPreserved=true;
  }else{
  const uploadedReport=await fetch(base+'/upload',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload)});assert.equal(uploadedReport.status,200);
  reportId=(await uploadedReport.json()).ReportID;
  const form=new FormData();form.append('file',new Blob([raw]),basename(source));
  const uploadedFile=await fetch(base+'/upload',{method:'POST',body:form});assert.equal(uploadedFile.status,200);
  fileId=(await uploadedFile.json()).FileID;
  assert.equal((await fetch(`${base}/api/admin/reports/${reportId}/file/${fileId}`,{method:'PUT',headers})).status,204);
  }
  evidence.reportId=reportId;evidence.inputFileId=fileId;
  const loaded=await api('pcr/read',{fileId,reportId});assert.equal(loaded.sourceSha256,sourceSha256);evidence.checks.realSdkRead=true;
  if(bundleMode){assert.equal(loaded.ruleContext.bundleId,bundleId);assert.equal(loaded.attachments.length,sidecarBytes.size);assert.equal(loaded.applicationRulesApplied,false);evidence.checks.attachmentsRestoredIntoSdkReadWorkdir=true;}
  const request={experimentId:loaded.experimentId,requestId:'real-sdk-reanalysis',parameters:evidence.parameters};
  const analyzed=await api('pcr/reanalyze',request);evidence.analysisRunId=analyzed.analysisRunId;
  writeFileSync(join(run,'sdk-analysis-response.json'),JSON.stringify(analyzed));evidence.checks.realSdkReanalysis=true;
  const history=await api('pcr/analyses/'+analyzed.analysisRunId);assert.equal(history.status,'SUCCEEDED');assert.equal(history.file_save_status,'NOT_SAVED');
  assert.deepEqual(history.result.experiment,analyzed.experiment);assert.deepEqual(history.parameters,evidence.parameters);assert.equal(history.operator_username,'admin');
  if(bundleMode){
    assert.equal(history.ruleSnapshot.bundleId,bundleId);evidence.checks.analysisRuleSnapshotPersisted=true;
    const evaluation=history.result.applicationRuleEvaluation;
    assert.equal(evaluation.engineVersion,'android-slan-rules-v1');assert.equal(evaluation.applicationRulesApplied,true);
    assert.equal(evaluation.status,'PARTIAL'); // Singleplex PCR has no melt peaks; do not label them negative.
    const a1=evaluation.rows.find(r=>r.type==='CT'&&r.sampleWellNames?.includes('A1'));
    assert.equal(a1.status,'SUSPICIOUS');assert.equal(a1.actualCt,39.053621906411);
    assert.ok(evaluation.rows.filter(r=>r.type==='PEAK').every(r=>r.status==='UNDETERMINED'));
    evidence.checks.androidApplicationRulesAppliedToRecomputedCt=true;evidence.checks.missingMeltDataNotCalledNegative=true;
    evidence.applicationRuleEvaluation=evaluation;
  }
  evidence.checks.resultAndParametersPersistedBeforeFileSave=true;
  const saved=await api('pcr/save',{analysisId:analyzed.analysisRunId,fileName:'singleplex-reanalyzed.pcr'});evidence.outputFileId=saved.FileID;
  const downloaded=await fetch(base+'/api/admin/files/'+saved.FileID+'/download',{headers});assert.equal(downloaded.status,200);
  const bytes=Buffer.from(await downloaded.arrayBuffer());const savedPath=join(run,'saved-result.pcr');writeFileSync(savedPath,bytes);
  evidence.outputBytes=bytes.length;evidence.outputSha256=hash(bytes);assert.notEqual(evidence.outputSha256,sourceSha256);
  evidence.checks.actualSdkSavedDifferentPcr=true;
  const reopened=await rpc('LoadExperimentResult',{file:savedPath});writeFileSync(join(run,'sdk-reopened-result.json'),JSON.stringify(reopened));
  const targets=e=>e.WellResult.flatMap(w=>(w.TargetResults||[]).map(t=>({well:w.ID,channel:t.Channel,ct:t.Ct,curve:t.AMPCurve,raw:t.RawCurve})));
  const before=targets(loaded.experiment),after=targets(analyzed.experiment),again=targets(reopened);
  assert.equal(before.length,24);assert.equal(after.length,before.length);assert.deepEqual(again,after);
  assert.deepEqual(after.map(t=>t.raw),before.map(t=>t.raw));
  const changed=after.filter((t,i)=>t.ct!==before[i].ct);assert.ok(changed.length>0,'Actual SDK must recalculate Ct for the changed threshold');
  evidence.changedCtCount=changed.length;evidence.ctComparison=after.map((t,i)=>({well:t.well,channel:t.channel,before:before[i].ct,after:t.ct}));
  evidence.checks.savedPcrReopenedByRealSdk=true;evidence.checks.reopenedCtAndCurvesMatch=true;evidence.checks.originalRawCurvesPreserved=true;evidence.checks.ctChangedBySdk=true;
  // Re-open saved output to verify the persisted actual SDK parameters, then close only this worker-owned experiment.
  const openAgain=await rpc('OpenExperiment',{file:savedPath});assert.equal(openAgain.ErrorCode,0);
  const newParameters=await rpc('GetExperimentParamters',{name:openAgain.Result.Name});assert.equal(newParameters.ErrorCode,0);
  for(const p of newParameters.Result)for(const t of p.Targets){assert.equal(t.Paramter.Threshold,0.24);assert.equal(t.Paramter.BeginBaseline,6);assert.equal(t.Paramter.EndBaseline,12);}
  await rpc('CloseExperiment',{name:openAgain.Result.Name});evidence.checks.savedParametersReopenedCorrectly=true;
  assert.deepEqual((await api('reports/'+reportId)).Experiment,payload);assert.equal(hash(readFileSync(source)),sourceSha256);evidence.checks.sourceAndReportUnchanged=true;
  if(advancedMode)evidence.advanced=await verifyAdvancedSdk({root,run,api,rpc,base,headers,loaded,bundleId});
  await stop(java);await stop(bridge);await stop(worker);await startJava();
  if(advancedMode){const recovered=await api('pcr/analyses/'+evidence.advanced.analysisRunId);assert.deepEqual(recovered.result.combinedConclusions,evidence.advanced.combinedConclusions);assert.equal(recovered.rule_version_id,evidence.advanced.ruleVersionId);evidence.advanced.checks.restartPreservesRulesAdvancedParametersAndCombinedResults=true;}
  const recovered=await api('pcr/analyses/'+analyzed.analysisRunId);assert.deepEqual(recovered.result.experiment,analyzed.experiment);assert.equal(recovered.output_file_id,saved.FileID);
  const recoveredFile=await fetch(base+'/api/admin/files/'+saved.FileID+'/download',{headers});assert.equal(recoveredFile.status,200);assert.equal(hash(Buffer.from(await recoveredFile.arrayBuffer())),evidence.outputSha256);
  evidence.checks.restartPersistenceWithoutSdk=true;
  if(bundleMode){assert.equal(recovered.ruleSnapshot.bundleId,bundleId);assert.deepEqual(recovered.result.applicationRuleEvaluation,history.result.applicationRuleEvaluation);for(const [name,bytes]of sidecarBytes){const response=await fetch(base+'/api/admin/bundles/'+bundleId+'/attachments/'+encodeURIComponent(name),{headers});assert.equal(response.status,200);assert.deepEqual(Buffer.from(await response.arrayBuffer()),bytes);}evidence.checks.ruleSnapshotAvailableAfterRestart=true;}
  assert.ok(calls.every(c=>['GetVersion','LoadExperimentResult','GetAllExperimentName','OpenExperiment','GetExperimentParamters','SetExperimentParamter','GetExperimentData','SaveExperiment','CloseExperiment'].includes(c.method)));
  const artifacts=join(root,'runtime','simulations');mkdirSync(artifacts,{recursive:true});
  evidence.artifactPath=join(artifacts,'singleplex-reanalyzed-'+basename(run)+'.pcr');copyFileSync(savedPath,evidence.artifactPath);
  if(bundleMode){evidence.ruleArtifactDirectory=join(artifacts,'rules-'+basename(run));mkdirSync(evidence.ruleArtifactDirectory,{recursive:true});for(const [name,bytes]of sidecarBytes)writeFileSync(join(evidence.ruleArtifactDirectory,name),bytes);}
  assert.equal(hash(readFileSync(evidence.artifactPath)),evidence.outputSha256);
  evidence.checks.onlyOfflineSdkMethods=true;evidence.status='PASS';evidence.finishedAt=new Date().toISOString();evidence.database=join(run,'db','real-sdk.mv.db');
  console.log(JSON.stringify({status:evidence.status,changedCtCount:evidence.changedCtCount,outputBytes:evidence.outputBytes,checks:evidence.checks,runDirectory:run},null,2));
  }
}catch(error){evidence.status='FAIL';evidence.error=String(error);throw error;}
finally{
  for(const child of processes.reverse())await stop(child);await stop(worker);
  if(transport.listening)await new Promise(r=>transport.close(r));
  evidence.sdkCalls=calls;writeFileSync(join(run,'real-sdk-test-result.json'),JSON.stringify(evidence,null,2));
  if(evidence.status==='PASS')copyFileSync(join(run,'real-sdk-test-result.json'),join(root,'test-reports',advancedMode?'real-sdk-advanced-2026-11-23.json':bundleMode?'real-sdk-bundle-2026-11-23.json':'real-sdk-reanalysis-2026-11-23.json'));
}
