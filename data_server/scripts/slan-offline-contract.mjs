// Protocol fixture only. Does NOT run vendor executables or parse a real PCR file.
import { createServer } from 'node:http';
import { spawn } from 'node:child_process';
import { mkdirSync, writeFileSync, readFileSync, openSync, closeSync, existsSync } from 'node:fs';
import { dirname, resolve, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { once } from 'node:events';
import { homedir } from 'node:os';
import assert from 'node:assert/strict';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const cwd=join(root,'target','slan-contract-'+Date.now());
mkdirSync(join(cwd,'input'),{recursive:true});
const source=join(cwd,'input','source.bin');
const original=Buffer.from('FAKE-PCR-CONTRACT-FIXTURE'); writeFileSync(source,original);
const calls=[]; let openedFile,openedName; let failSet=false,collide=false;
const experiment={Name:'厂商实际名称',SerialNumber:'REF-001',WellResult:[{ID:'A1',TargetResults:[{Ct:27.1,RawCurve:[{X:1,Y:100}]}]}]};
const parameter={ProjectName:'P1',AMPAlgorithm:0,CorssTalk:[[1]],BasicRule:{Keep:true},Targets:[{Name:'FAM',ChannelNo:1,Paramter:{__type:'TargetParamter:#SLAN.API.Model',BeginBaseline:2,EndBaseline:12,AutoThreshold:true,Threshold:100,DigitalFilter:2,OptimizationMode:0}}]};
const vendor=createServer(async(req,res)=>{
  let text='';for await(const chunk of req)text+=chunk;
  const method=req.url.split('/').at(-1), body=JSON.parse(text||'{}');calls.push({method,body});
  let result;
  switch(method){
    case 'GetVersion':result='9.0.1.260921';break;
    case 'LoadExperimentResult':assert.notEqual(body.file,source);assert.ok(existsSync(body.file));result={ErrorCode:0,Result:experiment};break;
    case 'GetAllExperimentName':result={ErrorCode:0,Result:collide?['厂商实际名称']:[]};break;
    case 'OpenExperiment':openedFile=body.file;openedName=experiment.Name;result={ErrorCode:0,Result:experiment};break;
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
  const loaded=await post('/v1/pcr/read',{fileId:'f1',fileName:'source.pcr',sourcePath:source});assert.equal(loaded.experiment.Name,experiment.Name);assert.equal(loaded.mock,false);
  const reanalyze={experimentId:loaded.experimentId,parameters:{autoBaseline:false,baselineStart:3,baselineEnd:15,threshold:500}};
  const analyzed=await post('/v1/pcr/reanalyze',reanalyze);assert.equal(analyzed.experiment.WellResult[0].TargetResults[0].Ct,28.2);
  const saved=await post('/v1/pcr/save',{analysisId:analyzed.analysisId,fileName:'复算结果.pcr'});
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
  const javaFd=openSync(join(cwd,'java.log'),'a');
  java=spawn(join(jdk,'bin',process.platform==='win32'?'java.exe':'java'),['-jar',join(root,'target/dataserver-0.1.0.jar')],{
    cwd,windowsHide:true,stdio:['ignore',javaFd,javaFd],env:{...process.env,
      DATASERVER_ADMIN_PASSWORD:password,DATASERVER_PORT:String(javaPort),DATASERVER_BIND:'127.0.0.1',DATASERVER_CSHARP_URL:base,
      DATASERVER_STORAGE:join(cwd,'input'),DATASERVER_LEGACY_UPLOAD_ENABLED:'true',SPRING_PROFILES_ACTIVE:'default',
      SPRING_DATASOURCE_URL:'jdbc:h2:file:'+join(cwd,'db','contract').replaceAll('\\','/')+';MODE=MySQL;DATABASE_TO_LOWER=TRUE;WRITE_DELAY=0',
      SPRING_DATASOURCE_USERNAME:'sa',SPRING_DATASOURCE_PASSWORD:''}});closeSync(javaFd);
  for(let i=0;i<120;i++){if(java.exitCode!==null)throw Error('Java exited');try{if((await fetch(javaBase+'/actuator/health')).ok)break;}catch{}await new Promise(r=>setTimeout(r,150));}
  const form=new FormData();form.append('file',new Blob([original]),'source.pcr');
  const uploaded=await fetch(javaBase+'/upload',{method:'POST',body:form});assert.equal(uploaded.status,200);const fileId=(await uploaded.json()).FileID;
  async function javaPost(op,body){const response=await fetch(javaBase+'/api/admin/pcr/'+op,{method:'POST',headers:{Authorization:auth,'Content-Type':'application/json'},body:JSON.stringify(body)});assert.equal(response.status,200,await response.clone().text());return response.json();}
  const javaRead=await javaPost('read',{fileId});
  const javaAnalysis=await javaPost('reanalyze',{...reanalyze,experimentId:javaRead.experimentId});
  const javaSave=await javaPost('save',{analysisId:javaAnalysis.analysisId,fileName:'final.pcr'});assert.equal(javaSave.Result,'1');
  const javaDownload=await fetch(javaBase+'/api/admin/files/'+javaSave.FileID+'/download',{headers:{Authorization:auth}});
  assert.equal(javaDownload.status,200);assert.equal(Buffer.from(await javaDownload.arrayBuffer()).toString(),'FAKE-SAVED-BY-FIXTURE');
  const originalDownload=await fetch(javaBase+'/api/admin/files/'+fileId+'/download',{headers:{Authorization:auth}});
  assert.deepEqual(Buffer.from(await originalDownload.arrayBuffer()),original);
  console.log('PASS: OFFLINE PROTOCOL FIXTURE ONLY — wrapped WCF responses, exact SDK parameter spelling, preserved __type/rules, private copies, save/download, failure cleanup, collision rejection, no instrument operations. No real SDK process or PCR parsing performed.');
}finally{for(const process of [java,child]){if(process&&process.exitCode===null&&process.signalCode===null){const end=once(process,'exit');process.kill();await end;}}await new Promise(r=>vendor.close(r));}
