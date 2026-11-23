import { spawn } from 'node:child_process';
import { mkdirSync, openSync, closeSync } from 'node:fs';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { homedir } from 'node:os';
import { createServer } from 'node:net';
import { once } from 'node:events';
import { randomBytes } from 'node:crypto';
import assert from 'node:assert/strict';
const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const cwd=join(root,'target','bridge-smoke-'+Date.now()); mkdirSync(cwd,{recursive:true});
const processes=[];
async function port(){ const server=createServer();server.listen(0,'127.0.0.1');await once(server,'listening');const p=server.address().port;await new Promise(r=>server.close(r));return p; }
function run(command,args,env){ const fd=openSync(join(cwd,'processes.log'),'a');const child=spawn(command,args,{cwd,env:{...process.env,...env},windowsHide:true,stdio:['ignore',fd,fd]});closeSync(fd);processes.push(child);return child; }
async function ready(url,child){ for(let i=0;i<120;i++){ if(child.exitCode!==null)throw Error('Process exited');try{if((await fetch(url)).ok)return;}catch{}await new Promise(r=>setTimeout(r,250));}throw Error('Startup timeout'); }
const bridgePort=await port(), javaPort=await port(), password=randomBytes(20).toString('hex');
const base=`http://127.0.0.1:${javaPort}`;
const authorization='Basic '+Buffer.from('admin:'+password).toString('base64');
const headers={Authorization:authorization,'Content-Type':'application/json'};
try{
  const bridge=run('dotnet',[join(root,'target/csharp-bridge/PcrSdkBridge.dll')],{PCR_BRIDGE_URL:`http://127.0.0.1:${bridgePort}`,PCR_VENDOR_ENABLED:'false'});
  await ready(`http://127.0.0.1:${bridgePort}/health`,bridge);
  const jdk=process.env.JAVA_HOME||join(homedir(),'.jdks','jbr-21.0.11');
  const java=run(join(jdk,'bin',process.platform==='win32'?'java.exe':'java'),['-jar',join(root,'target/dataserver-0.1.0.jar')],{
    DATASERVER_PORT:String(javaPort),DATASERVER_BIND:'127.0.0.1',DATASERVER_ADMIN_PASSWORD:password,DATASERVER_MANAGED_ENABLED:'false',
    DATASERVER_CSHARP_URL:`http://127.0.0.1:${bridgePort}`,DATASERVER_STORAGE:join(cwd,'files'),
    SPRING_PROFILES_ACTIVE:'default',DATASERVER_LEGACY_UPLOAD_ENABLED:'true',
    SPRING_DATASOURCE_URL:'jdbc:h2:file:'+join(cwd,'db','test').replaceAll('\\','/')+';MODE=MySQL;DATABASE_TO_LOWER=TRUE;WRITE_DELAY=0',
    SPRING_DATASOURCE_USERNAME:'sa',SPRING_DATASOURCE_PASSWORD:''
  });
  await ready(base+'/actuator/health',java);
  const caps=await fetch(base+'/api/admin/pcr/capabilities',{headers});assert.equal(caps.status,200);assert.equal((await caps.json()).sdkReady,false);
  const form=new FormData();form.append('file',new Blob(['placeholder-not-real-pcr']),'test.pcr');
  const uploaded=await fetch(base+'/upload',{method:'POST',body:form});assert.equal(uploaded.status,200);const fileId=(await uploaded.json()).FileID;
  for(const [operation,body,status] of [
    ['read',{fileId},501],
    ['reanalyze',{experimentId:'future-sdk-handle',parameters:{autoBaseline:true,baselineStart:3,baselineEnd:15,threshold:300}},404],
    ['save',{analysisId:'future-analysis-handle',fileName:'result.pcr'},404]
  ]){
    const response=await fetch(base+'/api/admin/pcr/'+operation,{method:'POST',headers,body:JSON.stringify(body)});
    assert.equal(response.status,status,operation+' must reject disabled SDK or unregistered handles, not fake success');
  }
  const invalid=await fetch(base+'/api/admin/pcr/save',{method:'POST',headers,body:JSON.stringify({analysisId:'a',fileName:'../bad.pcr'})});
  assert.equal(invalid.status,400);
  console.log('PASS: Java -> real C# process capabilities; disabled read returns 501; unknown analysis/session handles return 404; path validation rejects traversal.');
}finally{
  for(const child of processes.reverse()){if(child.exitCode===null&&child.signalCode===null){const end=once(child,'exit');child.kill();await end;}}
}
