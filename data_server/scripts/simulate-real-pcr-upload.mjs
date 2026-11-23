// Uses actual vendor-exported data. Never substitutes synthetic Ct/curve values.
import { readFileSync, writeFileSync, mkdirSync, openSync, closeSync, existsSync } from 'node:fs';
import { resolve, dirname, join, basename } from 'node:path';
import { fileURLToPath } from 'node:url';
import { homedir } from 'node:os';
import { spawn } from 'node:child_process';
import { createServer } from 'node:net';
import { once } from 'node:events';
import { createHash, randomBytes } from 'node:crypto';
import assert from 'node:assert/strict';

const root=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const output=resolve(process.argv[2]||join(root,'target/real-pcr-upload-20261121'));
// node scripts/simulate-real-pcr-upload.mjs <reader-output-directory> [source.pcr]
const referencePath=join('AIO_AAA_2026-11-16-11-01-01_07-自研试剂ab管多重测试_20261020','2026-11-16-13-04-40.pcr');
const defaultSource=existsSync(join(root,referencePath)) ? join(root,referencePath) : join(root,'result',referencePath);
const source=process.argv[3] ? resolve(process.argv[3]) : defaultSource;
const raw=readFileSync(source),native=JSON.parse(readFileSync(join(output,'vendor-experiment.json'),'utf8'));
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const originalHash=sha(raw);
if(source===defaultSource)assert.equal(originalHash,'577afff986549e59d29f611358a776b5e1c0439bc34d4504cbda7889724031e2');
const run=join(output,'http-'+Date.now());mkdirSync(run,{recursive:true});
const mappings={SerialNumber:'SerialNo',UseModule:'Block',Cn:'CnValue',Rn:'RnValue',Wm:'Width',Ym:'YValue'};
const unavailableKeys=new Set(['Ct','CtMean','CtSD','CtCV','Cn','CnValue','CnMean','CnSD','CnCV','Rn','RnValue','Tm','Rm','Wm','Ym','Width','YValue','X','Y']);
let normalizedNumbers=0;
function normalize(value,key=''){
  if(unavailableKeys.has(key)&&typeof value==='string'&&['NaN','Infinity','-Infinity'].includes(value)){normalizedNumbers++;return null;}
  if(Array.isArray(value))return value.map(item=>normalize(item));
  if(value&&typeof value==='object')return Object.fromEntries(Object.entries(value).map(([k,v])=>[mappings[k]||k,normalize(v,k)]));
  return value;
}
// Use the Experiment envelope used by the Android SDK. Do not send the C# response wrapper.
const legacyNative=Object.fromEntries(['Name','StartTime','EndTime','Operator','Auditor','Note','PlateNo','BatchNo','SerialNumber','UseModule','DeviceModel','WellResult','TargetStd']
  .filter(key=>key in native).map(key=>[mappings[key]||key,native[key]]));
const payload=normalize(legacyNative),body=JSON.stringify(payload);
writeFileSync(join(output,'legacy-with-nonfinite.json'),JSON.stringify(legacyNative));
writeFileSync(join(output,'upload-experiment.json'),body);
const targets=native.WellResult.flatMap(well=>well.TargetResults||[]);
assert.ok(targets.some(t=>(t.RawMeltCurve?.length||t.RawCurve?.length||t.AMPCurve?.length||0)>0),'Actual curve data is required; do not use an uninitialized SDK result');
const validCt=targets.filter(t=>typeof t.Ct==='number'&&Number.isFinite(t.Ct));
const nonFiniteCt=targets.filter(t=>typeof t.Ct==='string'&&['NaN','Infinity','-Infinity'].includes(t.Ct));
const statistics={wells:native.WellResult.length,targets:targets.length,channels:[...new Set(targets.map(t=>t.Channel))],
  rawAmplificationPoints:targets.reduce((n,t)=>n+(t.RawCurve?.length||0),0),
  amplificationPoints:targets.reduce((n,t)=>n+(t.AMPCurve?.length||0),0),
  rawMeltPoints:targets.reduce((n,t)=>n+(t.RawMeltCurve?.length||0),0),peakCurvePoints:targets.reduce((n,t)=>n+(t.PeakCurve?.length||0),0),
  peaks:targets.reduce((n,t)=>n+(t.PeakValues?.length||0),0),validCt:validCt.length,
  unavailableCt:targets.filter(t=>t.Ct==null).length+nonFiniteCt.length,
  nonFiniteCt:nonFiniteCt.length,ctMin:validCt.length?Math.min(...validCt.map(t=>t.Ct)):null,
  ctMax:validCt.length?Math.max(...validCt.map(t=>t.Ct)):null};
const ctByWell=native.WellResult.map(well=>({wellNames:well.SampleWellNames,
  targets:(well.TargetResults||[]).map(t=>({name:t.Name,channel:t.Channel,
    ct:typeof t.Ct==='number'&&Number.isFinite(t.Ct)?t.Ct:null,
    sdkCtState:typeof t.Ct==='number'?'finite':t.Ct??'missing',
    rawPoints:t.RawCurve?.length||0,amplificationPoints:t.AMPCurve?.length||0}))}));
const listener=createServer();listener.listen(0,'127.0.0.1');await once(listener,'listening');const port=listener.address().port;await new Promise(r=>listener.close(r));
const base=`http://127.0.0.1:${port}`,password=randomBytes(24).toString('hex');
const admin={Authorization:'Basic '+Buffer.from('admin:'+password).toString('base64')};
const jdk=process.env.JAVA_HOME||join(homedir(),'.jdks','jbr-21.0.11');let child;
async function start(){
  const fd=openSync(join(run,'dataserver.log'),'a');
  child=spawn(join(jdk,'bin','java.exe'),['-jar',join(root,'target/dataserver-0.1.0.jar')],{cwd:run,windowsHide:true,stdio:['ignore',fd,fd],env:{...process.env,
    DATASERVER_MANAGED_ENABLED:'false',DATASERVER_PORT:String(port),DATASERVER_BIND:'127.0.0.1',DATASERVER_ADMIN_PASSWORD:password,
    DATASERVER_STORAGE:join(run,'files'),DATASERVER_LEGACY_UPLOAD_ENABLED:'true',SPRING_PROFILES_ACTIVE:'default',
    SPRING_DATASOURCE_URL:'jdbc:h2:file:'+join(run,'db','upload-test').replaceAll('\\','/')+';MODE=MySQL;DATABASE_TO_LOWER=TRUE;WRITE_DELAY=0',
    SPRING_DATASOURCE_USERNAME:'sa',SPRING_DATASOURCE_PASSWORD:''}});closeSync(fd);
  for(let i=0;i<180;i++){if(child.exitCode!==null)throw Error('Dataserver exited; inspect its log');try{if((await fetch(base+'/actuator/health')).ok)return;}catch{}await new Promise(r=>setTimeout(r,200));}
  throw Error('Dataserver readiness timeout');
}
async function stop(){if(child&&child.exitCode===null&&child.signalCode===null){const end=once(child,'exit');child.kill();await end;}}
const evidence={source:source,sourceBytes:raw.length,sourceSha256:originalHash,statistics,ctByWell,mappings,normalizedNumbers,
  dataSource:'Actual vendor Service.SLANService.LoadExperimentResult on a private input copy; no hardware calls',
  normalization:'Non-finite numeric results explicitly encoded as JSON null, never zero. This is a compatibility normalization, not unchanged Android Gson serialization.',
  database:join(run,'db','upload-test.mv.db'),status:'RUNNING',checks:{}};
async function sendJson(text,status=200){const response=await fetch(base+'/upload',{method:'POST',headers:{'Content-Type':'application/json'},body:text});assert.equal(response.status,status,await response.clone().text());return response.json();}
async function uploadFile(){const form=new FormData();form.append('file',new Blob([raw]),basename(source));const response=await fetch(base+'/upload',{method:'POST',body:form});assert.equal(response.status,200);return response.json();}
async function detail(id){const response=await fetch(base+'/api/admin/reports/'+id,{headers:admin});assert.equal(response.status,200);return response.json();}
try{
  await start();
  const gsonFile=join(output,'android-original-upload.json.txt');
  if(existsSync(gsonFile)) {
    const originalGson=readFileSync(gsonFile,'utf8');
    assert.throws(()=>JSON.parse(originalGson));
    assert.equal((await sendJson(originalGson,400)).Result,'0');
    evidence.checks.originalGsonNonStandardNaNRejected=true;
  }
  const unmapped=await sendJson(JSON.stringify(native),400);assert.equal(unmapped.Result,'0');evidence.checks.csharpShapeRequiresMapping=true;
  if(nonFiniteCt.length){const invalid=await sendJson(JSON.stringify(legacyNative),400);assert.equal(invalid.Result,'0');evidence.checks.nonFiniteCtStringsAreRejected=true;}
  const first=await sendJson(body);assert.equal(first.Result,'1');assert.equal(first.Duplicate,false);evidence.reportId=first.ReportID;
  const retry=await sendJson(body);assert.equal(retry.ReportID,first.ReportID);assert.equal(retry.Duplicate,true);evidence.checks.jsonRetryDeduplicated=true;
  const concurrent=await Promise.all(Array.from({length:4},()=>sendJson(body)));assert.ok(concurrent.every(r=>r.ReportID===first.ReportID&&r.Duplicate));evidence.checks.concurrentRetriesDeduplicated=true;
  const stored=await detail(first.ReportID);assert.deepEqual(stored.Experiment,payload);assert.equal(stored.well_count,statistics.wells);assert.equal(stored.target_count,statistics.targets);
  const storedTargets=stored.Experiment.WellResult.flatMap(well=>well.TargetResults||[]);
  for(let i=0;i<targets.length;i++){
    const ct=targets[i].Ct,expectedCt=typeof ct==='string'&&['NaN','Infinity','-Infinity'].includes(ct)?null:ct;
    assert.equal(storedTargets[i].Ct,expectedCt,'Ct must preserve finite precision and non-finite missingness');
    for(const curve of ['RawCurve','AMPCurve','RawMeltCurve','PeakCurve']) {
      if(curve in targets[i])assert.deepEqual(storedTargets[i][curve],normalize(targets[i][curve]));
    }
  }
  evidence.checks.allDataRoundTripIncludingCtAndCurves=true;
  evidence.checks.finiteCtValuesUnchanged=true;
  evidence.checks.nonFiniteCtEncodedAsNull=true;
  const expectedOperator=typeof payload.Operator==='string'&&payload.Operator.trim()?payload.Operator:null;
  assert.equal(stored.operator_name,expectedOperator);assert.equal(stored.operator_user_id,null);
  assert.equal(stored.upload_source,'LEGACY_HTTP');assert.equal(stored.uploaded_by_device_id,null);
  evidence.checks.operatorMetadataMatchesSource=true;
  const exported=await fetch(base+'/api/admin/reports/'+first.ReportID+'/source',{headers:admin});assert.equal(await exported.text(),body);evidence.checks.jsonSourceByteExact=true;
  const file=await uploadFile(),again=await uploadFile();assert.equal(file.Result,'1');assert.equal(file.FileID,again.FileID);assert.equal(again.Duplicate,true);evidence.fileId=file.FileID;evidence.checks.binaryRetryDeduplicated=true;
  const link=await fetch(`${base}/api/admin/reports/${first.ReportID}/file/${file.FileID}`,{method:'PUT',headers:admin});assert.equal(link.status,204);
  const downloaded=await fetch(`${base}/api/admin/files/${file.FileID}/download`,{headers:admin});assert.equal(downloaded.status,200);assert.equal(sha(Buffer.from(await downloaded.arrayBuffer())),originalHash);evidence.checks.downloadHashMatchesSource=true;
  const summary=await(await fetch(base+'/api/admin/summary',{headers:admin})).json();assert.equal(summary.reports,1);assert.equal(summary.files,1);evidence.summary=summary;
  await stop();await start();
  const recovered=await detail(first.ReportID);assert.deepEqual(recovered.Experiment,payload);assert.equal(recovered.file_id,file.FileID);
  assert.equal(recovered.operator_name,expectedOperator);assert.equal(recovered.upload_source,'LEGACY_HTTP');
  const recoveredFile=await fetch(`${base}/api/admin/files/${file.FileID}/download`,{headers:admin});
  assert.equal(recoveredFile.status,200);assert.equal(sha(Buffer.from(await recoveredFile.arrayBuffer())),originalHash);
  assert.equal((await sendJson(body)).ReportID,first.ReportID);evidence.checks.restartPersistence=true;
  assert.equal(sha(readFileSync(source)),originalHash);evidence.checks.sourceUnmodified=true;
  evidence.status='PASS';evidence.finishedAt=new Date().toISOString();evidence.uploadJsonBytes=Buffer.byteLength(body);
  console.log(JSON.stringify({status:evidence.status,...statistics,checks:evidence.checks,reportId:evidence.reportId,fileId:evidence.fileId},null,2));
}catch(error){evidence.status='FAIL';evidence.error=String(error);throw error;}
finally{await stop();writeFileSync(join(output,'upload-test-result.json'),JSON.stringify(evidence,null,2));}
