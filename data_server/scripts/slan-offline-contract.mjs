// Protocol fixture only. Does NOT run vendor executables or parse a real PCR file.
import { createServer } from 'node:http';
import { spawn } from 'node:child_process';
import { mkdirSync, writeFileSync, readFileSync, openSync, closeSync, existsSync } from 'node:fs';
import { dirname, resolve, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { once } from 'node:events';
import { homedir } from 'node:os';
import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const cwd=join(root,'target','slan-contract-'+Date.now());
mkdirSync(join(cwd,'input'),{recursive:true});
const source=join(cwd,'input','source.bin');
const original=Buffer.from('FAKE-PCR-CONTRACT-FIXTURE'); writeFileSync(source,original);
const ruleBytes=Buffer.from('[{"Channel":"FAM","CTvalue":35}]');
const ruleSource=join(cwd,'input','original.ct');writeFileSync(ruleSource,ruleBytes);
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const calls=[]; let openedFile,openedName; let failSet=false,collide=false;
const experiment={Name:'厂商实际名称',SerialNumber:'REF-001',WellResult:[{ID:'A1',TargetResults:[{Ct:27.1,RawCurve:[{X:1,Y:100}]}]}]};
const parameter={ProjectName:'P1',AMPAlgorithm:0,CorssTalk:[[1]],BasicRule:{Keep:true},Targets:[{Name:'FAM',ChannelNo:1,Paramter:{__type:'TargetParamter:#SLAN.API.Model',BeginBaseline:2,EndBaseline:12,AutoThreshold:true,Threshold:100,DigitalFilter:2,OptimizationMode:0}}]};
const vendor=createServer(async(req,res)=>{
  let text='';for await(const chunk of req)text+=chunk;
  const method=req.url.split('/').at(-1), body=JSON.parse(text||'{}');calls.push({method,body});
  let result;
  switch(method){
    case 'GetVersion':result='9.0.1.260921';break;
    case 'LoadExperimentResult':assert.notEqual(body.file,source);assert.ok(existsSync(body.file));assert.deepEqual(readFileSync(join(dirname(body.file),'original.ct')),ruleBytes);result=experiment;break;
    case 'GetAllExperimentName':result={ErrorCode:0,Result:collide?['厂商实际名称']:[]};break;
    case 'OpenExperiment':assert.deepEqual(readFileSync(join(dirname(body.file),'original.ct')),ruleBytes);openedFile=body.file;openedName=experiment.Name;result={ErrorCode:0,Result:experiment};break;
    case 'GetExperimentParamters':assert.equal(body.name,openedName);result={ErrorCode:0,Result:[parameter]};break;
    case 'SetExperimentParamter':{
      assert.equal(body.name,openedName);assert.equal(body.paramter.Targets[0].Paramter.__type,'TargetParamter:#SLAN.API.Model');
      assert.equal(body.paramter.Targets[0].Paramter.DigitalFilter,2);assert.deepEqual(body.paramter.BasicRule,{Keep:true});
      assert.equal(body.paramter.Targets[0].Paramter.Threshold,500);assert.equal(body.paramter.Targets[0].Paramter.AutoThreshold,false);
      assert.equal(body.paramter.Targets[0].Paramter.OptimizationMode,2);
      result={ErrorCode:failSet?10:0,Result:!failSet,Message:failSet?'fixture error':''};break;
    }
    case 'GetExperimentData':result={ErrorCode:0,Result:{...experiment,WellResult:[{ID:'A1',TargetResults:[{Ct:28.2}]}]}};break;
    case 'SaveExperiment':assert.equal(body.name,openedName);writeFileSync(openedFile,'FAKE-SAVED-BY-FIXTURE');result={ErrorCode:0,Result:true};break;
    case 'CloseExperiment':assert.equal(body.name,openedName);openedFile=null;openedName=null;result={ErrorCode:0,Result:true};break;
    default:res.writeHead(500);res.end('Forbidden/unexpected vendor method');return;
  }
  res.setHeader('Content-Type','application/json');res.end(JSON.stringify({[method+'Result']:result}));
});
vendor.listen(0,'127.0.0.1');await once(vendor,'listening');
const vendorPort=vendor.address().port;
const reserve=createServer();reserve.listen(0,'127.0.0.1');await once(reserve,'listening');const bridgePort=reserve.address().port;await new Promise(r=>reserve.close(r));
const base=`http://127.0.0.1:${bridgePort}`;
const fd=openSync(join(cwd,'bridge.log'),'a');
const child=spawn('dotnet',[join(root,'target/csharp-bridge/PcrSdkBridge.dll')],{cwd,windowsHide:true,stdio:['ignore',fd,fd],env:{...process.env,
  PCR_BRIDGE_URL:base,PCR_VENDOR_ENABLED:'true',PCR_VENDOR_URL:`http://127.0.0.1:${vendorPort}/SlanServerService`,PCR_INPUT_ROOT:join(cwd,'input'),PCR_WORK_ROOT:join(cwd,'work')}});closeSync(fd);
let java;
async function post(path,body,status=200){const response=await fetch(base+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});assert.equal(response.status,status,JSON.stringify(await response.clone().json()));return response.json();}
try{
  for(let i=0;i<120;i++){if(child.exitCode!==null)throw Error('Bridge exited');try{if((await fetch(base+'/health')).ok)break;}catch{}await new Promise(r=>setTimeout(r,100));}
  const cap=await(await fetch(base+'/v1/capabilities')).json();assert.equal(cap.sdkReady,true);
  const attachments=[{fileName:'original.ct',sourcePath:ruleSource,sha256:sha(ruleBytes),role:'CT_RULE'}];
  const loaded=await post('/v1/pcr/read',{fileId:'f1',fileName:'source.pcr',sourcePath:source,attachments});assert.equal(loaded.experiment.Name,experiment.Name);assert.equal(loaded.mock,false);
  assert.equal(loaded.attachments.length,1);assert.equal(loaded.applicationRulesApplied,false);
  await post('/v1/pcr/read',{fileId:'bad-hash',fileName:'source.pcr',sourcePath:source,attachments:[{...attachments[0],sha256:'0'.repeat(64)}]},400);
  writeFileSync(ruleSource,'changed after SDK read');
  const reanalyze={experimentId:loaded.experimentId,parameters:{autoBaseline:false,baselineStart:3,baselineEnd:15,threshold:500}};
  const analyzed=await post('/v1/pcr/reanalyze',reanalyze);assert.equal(analyzed.experiment.WellResult[0].TargetResults[0].Ct,28.2);
  const saved=await post('/v1/pcr/save',{analysisId:analyzed.analysisId,fileName:'复算结果.pcr'});
  assert.equal(saved.attachments[0].sha256,sha(ruleBytes));
  const downloaded=Buffer.from(await(await fetch(base+'/v1/pcr/files/'+saved.outputId)).arrayBuffer());assert.equal(downloaded.toString(),'FAKE-SAVED-BY-FIXTURE');
  assert.deepEqual(readFileSync(source),original);
  failSet=true;const before=calls.length;await post('/v1/pcr/reanalyze',reanalyze,422);
  assert.equal(calls.at(-1).method,'CloseExperiment');assert.ok(!calls.slice(before).some(c=>c.method==='SaveExperiment'));
  collide=true;const beforeCollision=calls.length;await post('/v1/pcr/reanalyze',reanalyze,409);assert.deepEqual(calls.slice(beforeCollision).map(c=>c.method),['GetAllExperimentName']);
  await post('/v1/pcr/read',{fileId:'bad',fileName:'bad.pcr',sourcePath:join(root,'README.md')},400);
  await post('/v1/pcr/save',{analysisId:analyzed.analysisId,fileName:'../bad.pcr'},400);
  assert.ok(!calls.some(c=>/Device|RunExperiment|StopExperiment|ReleaseExperiment|AddProject/.test(c.method)));
  failSet=false;collide=false;
  // Exercise the complete Java -> C# -> vendor protocol fixture -> saved file download path.
  const socket=createServer();socket.listen(0,'127.0.0.1');await once(socket,'listening');const javaPort=socket.address().port;await new Promise(r=>socket.close(r));
  const javaBase=`http://127.0.0.1:${javaPort}`,password='contract-only-test-password';
  const auth='Basic '+Buffer.from('admin:'+password).toString('base64');
  const jdk=process.env.JAVA_HOME||join(homedir(),'.jdks','jbr-21.0.11');
  const javaEnv={...process.env,
      DATASERVER_ADMIN_PASSWORD:password,DATASERVER_PORT:String(javaPort),DATASERVER_BIND:'127.0.0.1',DATASERVER_CSHARP_URL:base,DATASERVER_MANAGED_ENABLED:'false',
      DATASERVER_STORAGE:join(cwd,'input'),DATASERVER_LEGACY_UPLOAD_ENABLED:'true',SPRING_PROFILES_ACTIVE:'default',
      SPRING_DATASOURCE_URL:'jdbc:h2:file:'+join(cwd,'db','contract').replaceAll('\\','/')+';MODE=MySQL;DATABASE_TO_LOWER=TRUE;WRITE_DELAY=0',
      SPRING_DATASOURCE_USERNAME:'sa',SPRING_DATASOURCE_PASSWORD:''};
  async function startJava(){
    const javaFd=openSync(join(cwd,'java.log'),'a');
    java=spawn(join(jdk,'bin',process.platform==='win32'?'java.exe':'java'),['-jar',join(root,'target/dataserver-0.1.0.jar')],{
      cwd,windowsHide:true,stdio:['ignore',javaFd,javaFd],env:javaEnv});closeSync(javaFd);
    for(let i=0;i<600;i++){if(java.exitCode!==null)throw Error('Java exited');try{if((await fetch(javaBase+'/actuator/health')).ok)return;}catch{}await new Promise(r=>setTimeout(r,200));}
    throw Error('Java readiness timeout');
  }
  async function stop(process){if(process&&process.exitCode===null&&process.signalCode===null){const end=once(process,'exit');process.kill();await end;}}
  await startJava();
  async function javaPost(op,body,status=200){const response=await fetch(javaBase+'/api/admin/pcr/'+op,{method:'POST',headers:{Authorization:auth,'Content-Type':'application/json'},body:JSON.stringify(body)});assert.equal(response.status,status,await response.clone().text());return response.json();}
  async function javaGet(path){const response=await fetch(javaBase+'/api/admin/'+path,{headers:{Authorization:auth}});assert.equal(response.status,200);return response.json();}
  const reportBody={Name:'原始设备实验',SerialNo:'REF-001',Operator:'device-operator',WellResult:experiment.WellResult};
  const form=new FormData();form.append('file',new Blob([original]),'source.pcr');form.append('attachments',new Blob([ruleBytes]),'original.ct');
  form.append('experiment',JSON.stringify(reportBody));form.append('manifest',JSON.stringify({version:1,files:[{fileName:'source.pcr',role:'PCR',sha256:sha(original),size:original.length},{fileName:'original.ct',role:'CT_RULE',sha256:sha(ruleBytes),size:ruleBytes.length}]}));
  const uploaded=await fetch(javaBase+'/api/admin/bundles',{method:'POST',headers:{Authorization:auth},body:form});assert.equal(uploaded.status,200,await uploaded.clone().text());
  const bundleReply=await uploaded.json(),fileId=bundleReply.FileID,reportId=bundleReply.ReportID,bundleId=bundleReply.BundleID;
  const javaRead=await javaPost('read',{fileId,reportId});assert.equal(javaRead.reportId,reportId);
  const analysisRequest={...reanalyze,experimentId:javaRead.experimentId,requestId:'persistent-contract-analysis'};
  const javaAnalysis=await javaPost('reanalyze',analysisRequest),runId=javaAnalysis.analysisRunId;
  const beforeReplay=calls.length;assert.equal((await javaPost('reanalyze',analysisRequest)).analysisRunId,runId);assert.equal(calls.length,beforeReplay);
  const beforeSave=await javaGet('pcr/analyses/'+runId);
  assert.equal(beforeSave.status,'SUCCEEDED');assert.equal(beforeSave.file_save_status,'NOT_SAVED');assert.equal(beforeSave.output_file_id,null);
  assert.equal(beforeSave.operator_username,'admin');assert.equal(beforeSave.report_id,reportId);assert.equal(beforeSave.input_file_id,fileId);
  assert.equal(beforeSave.sdk_version,'9.0.1.260921');assert.deepEqual(beforeSave.parameters,analysisRequest.parameters);
  assert.equal(beforeSave.bundle_id,bundleId);assert.equal(beforeSave.ruleSnapshot.bundleId,bundleId);assert.equal(beforeSave.ruleSnapshot.evaluationStatus,'ARCHIVED_NOT_EVALUATED');
  assert.deepEqual(beforeSave.result.experiment,javaAnalysis.experiment);
  const javaSave=await javaPost('save',{analysisId:runId,fileName:'final.pcr'});assert.equal(javaSave.Result,'1');
  const savedHistory=await javaGet('pcr/analyses/'+runId);assert.equal(savedHistory.output_file_id,javaSave.FileID);assert.equal(savedHistory.file_save_status,'SAVED');
  const javaDownload=await fetch(javaBase+'/api/admin/files/'+javaSave.FileID+'/download',{headers:{Authorization:auth}});
  assert.equal(javaDownload.status,200);assert.equal(Buffer.from(await javaDownload.arrayBuffer()).toString(),'FAKE-SAVED-BY-FIXTURE');
  const originalDownload=await fetch(javaBase+'/api/admin/files/'+fileId+'/download',{headers:{Authorization:auth}});
  assert.deepEqual(Buffer.from(await originalDownload.arrayBuffer()),original);
  failSet=true;await javaPost('reanalyze',{...analysisRequest,requestId:'failed-contract-analysis'},422);failSet=false;
  const history=await javaGet('pcr/analyses?reportId='+reportId);assert.equal(history.total,2);
  const failed=history.items.find(row=>row.status==='FAILED');assert.ok(failed);assert.equal((await javaGet('pcr/analyses/'+failed.id)).result,null);
  assert.deepEqual((await javaGet('reports/'+reportId)).Experiment,reportBody);
  // Both process-local SDK handle dictionaries disappear. Stored JSON and saved PCR must remain usable.
  await stop(java);await stop(child);await startJava();
  const recovered=await javaGet('pcr/analyses/'+runId);assert.deepEqual(recovered.result,beforeSave.result);assert.deepEqual(recovered.parameters,beforeSave.parameters);
  assert.equal(recovered.ruleSnapshot.bundleId,bundleId);
  const recoveredRule=await fetch(javaBase+'/api/admin/bundles/'+bundleId+'/attachments/original.ct',{headers:{Authorization:auth}});assert.equal(recoveredRule.status,200);assert.deepEqual(Buffer.from(await recoveredRule.arrayBuffer()),ruleBytes);
  assert.equal(recovered.output_file_id,javaSave.FileID);assert.equal((await javaGet('pcr/analyses?reportId='+reportId)).total,2);
  const afterRestart=await fetch(javaBase+'/api/admin/files/'+javaSave.FileID+'/download',{headers:{Authorization:auth}});
  assert.equal(afterRestart.status,200);assert.equal(Buffer.from(await afterRestart.arrayBuffer()).toString(),'FAKE-SAVED-BY-FIXTURE');
  assert.equal((await javaPost('reanalyze',analysisRequest)).analysisRunId,runId);
  const repeatedSave=await javaPost('save',{analysisId:runId,fileName:'final.pcr'});assert.equal(repeatedSave.FileID,javaSave.FileID);assert.equal(repeatedSave.Duplicate,true);
  assert.deepEqual((await javaGet('reports/'+reportId)).Experiment,reportBody);
  const evidence={status:'PASS',scope:'PROTOCOL FIXTURE ONLY; no real vendor process, PCR parsing, or physical device',
    checks:{resultPersistedBeforeSave:true,parametersAndOperatorPersisted:true,sdkVersionRecorded:true,reportAndFileLinked:true,
      requestIdReplayWithoutSdk:true,saveIdempotent:true,failedAnalysisRecorded:true,originalReportUnchanged:true,
      javaRestartWithBridgeStopped:true,resultAndFileAvailableAfterRestart:true,ruleHashValidated:true,privateRuleSnapshotUnchanged:true,analysisRuleBundlePersistsAfterRestart:true},
    reportId,bundleId,inputFileId:fileId,analysisRunId:runId,outputFileId:javaSave.FileID,failedAnalysisRunId:failed.id,
    database:join(cwd,'db','contract.mv.db'),finishedAt:new Date().toISOString()};
  writeFileSync(join(cwd,'history-test-result.json'),JSON.stringify(evidence,null,2));
  console.log(JSON.stringify(evidence,null,2));
}finally{for(const process of [java,child]){if(process&&process.exitCode===null&&process.signalCode===null){const end=once(process,'exit');process.kill();await end;}}await new Promise(r=>vendor.close(r));}
