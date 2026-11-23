// Called by the isolated real SDK harness. All application rules in this test are explicitly synthetic.
import assert from 'node:assert/strict';
import { readFileSync, writeFileSync, readdirSync, mkdirSync } from 'node:fs';
import { join, basename } from 'node:path';
import { createHash } from 'node:crypto';
const hash=b=>createHash('sha256').update(b).digest('hex');
export async function verifyAdvancedSdk({ root,run,api,rpc,base,headers,loaded,bundleId }) {
  const evidence={scope:'Real singleplex and melt PCR, real SDK, synthetic rule versions, no instrument',checks:{}};
  const legacy={autoBaseline:true,baselineStart:6,baselineEnd:12,threshold:0.24};
  assert.ok(loaded.parameterSets?.length,'Read must expose actual SDK parameter sets');
  const patch={scope:'SELECTED',projects:[{projectIndex:0,ampAlgorithm:2}],targets:[{projectIndex:0,targetIndex:0,settings:{baselineStart:6,baselineEnd:12,threshold:0.24,autoThreshold:false,qualitativeAnalysis:false,optimizationMode:2,digitalFilter:1}}]};
  const ct=await api('pcr/reanalyze',{experimentId:loaded.experimentId,parameters:{...legacy,...patch},requestId:'advanced-ct'});
  assert.equal(ct.parameterSets[0].AMPAlgorithm,2);assert.equal(ct.parameterSets[0].Targets[0].Paramter.DigitalFilter,1);
  assert.equal(ct.parameterSets[0].Targets[0].Paramter.QualitativeAnalysis,false);
  for(let i=1;i<loaded.parameterSets[0].Targets.length;i++)assert.deepEqual(ct.requestedParameterSets[0].Targets[i],loaded.parameterSets[0].Targets[i]);
  evidence.checks.selectedTargetAndAlgorithmApplied=true;evidence.checks.otherChannelsPreserved=true;
  const folder=readdirSync(join(root,'result')).find(n=>n.startsWith('AIO_AAA_2026-11-16'));
  const source=join(root,'result',folder,'2026-11-16-13-04-40.pcr'),original=readFileSync(source);evidence.meltSourceSha256=hash(original);
  const form=new FormData();form.append('file',new Blob([original]),basename(source));
  const uploaded=await fetch(base+'/api/admin/files',{method:'POST',headers:{Authorization:headers.Authorization},body:form});assert.equal(uploaded.status,200,await uploaded.clone().text());
  const fileId=(await uploaded.json()).FileID;
  const melt=await api('pcr/read',{fileId});
  let projectIndex,targetIndex,target;
  for(let p=0;p<melt.parameterSets.length;p++)for(let t=0;t<melt.parameterSets[p].Targets.length;t++)if(target===undefined&&'MinTemperature' in melt.parameterSets[p].Targets[t].Paramter){projectIndex=p;targetIndex=t;target=melt.parameterSets[p].Targets[t];}
  assert.ok(target,'Actual melt target must expose melt-specific SDK fields');
  const originalSettings=target.Paramter;
  const settings={baselineStart:6,baselineEnd:12,autoThreshold:true,qualitativeAnalysis:false,optimizationMode:2,digitalFilter:1,
    peakHeightThreshold:Math.max(0,originalSettings.PeekHightThreshold/2),minTemperature:originalSettings.MinTemperature+0.1,maxTemperature:originalSettings.MaxTemperature-0.1,meltPeakAlgorithm:originalSettings.MeltPeakAlgorithm===1?0:1,peakType:2};
  const parameters={...legacy,scope:'SELECTED',projects:[{projectIndex,ampAlgorithm:2}],targets:[{projectIndex,targetIndex,settings}]};
  const analyzed=await api('pcr/reanalyze',{experimentId:melt.experimentId,parameters,requestId:'advanced-melt'});
  const effective=analyzed.parameterSets[projectIndex].Targets[targetIndex].Paramter;
  for(const [key,value]of Object.entries({AutoThreshold:true,QualitativeAnalysis:false,OptimizationMode:2,DigitalFilter:1,PeekHightThreshold:settings.peakHeightThreshold,MinTemperature:settings.minTemperature,MaxTemperature:settings.maxTemperature,MeltPeakAlgorithm:settings.meltPeakAlgorithm,PeekType:2}))assert.equal(effective[key],value,key);
  evidence.checks.allMeltFieldsAndAutomaticThresholdApplied=true;
  const candidates=analyzed.experiment.WellResult.flatMap(w=>w.TargetResults.filter(t=>t.Name.toLowerCase().includes(target.Name.toLowerCase())).map(t=>({well:w,target:t}))).filter(x=>x.target.PeakValues?.some(p=>Number.isFinite(p.Tm)&&Number.isFinite(p.Rm)&&p.Rm>=0));
  assert.ok(candidates.length,'Recomputed actual melt peaks required');
  const measured=candidates[0],peak=measured.target.PeakValues.find(p=>Number.isFinite(p.Tm)&&Number.isFinite(p.Rm)&&p.Rm>=0);
  const ruleFiles=[{fileName:'advanced.ct',rules:[{Channel:target.Name,Category:'SIMULATION CT RULE',CTvalue:35,CTvalueDown:35,CTvalueUP:100,CTvalueUP_Suspicious:2}]},
    {fileName:'advanced.peak',rules:[{Channel:target.Name,PEAKSingleRuleEntityList:[{Category:'SIMULATION MELT RULE',PEAKvalue:peak.Tm,PEAKvalueDown:0.5,PEAKvalueUP:0.5,PEAKvalueRM:Math.max(0,peak.Rm/2)}]}]}];
  const policies=['ALL','ANY'].map(mode=>({name:'SIMULATION '+mode,mode,ct:{fileName:'advanced.ct',ruleIndex:0},peak:{fileName:'advanced.peak',ruleIndex:0}}));
  const version=await api('pcr/rules',{baseBundleId:null,label:'SIMULATION real melt rules',files:ruleFiles,combinationRules:policies});
  const judged=await api('pcr/reanalyze',{experimentId:melt.experimentId,parameters,ruleVersionId:version.id,requestId:'advanced-melt-versioned'});
  assert.equal(judged.ruleVersionId,version.id);assert.ok(judged.applicationRuleEvaluation.rows.some(r=>r.type==='PEAK'&&r.status==='POSITIVE'));
  assert.ok(judged.combinedConclusions.rows.length>0);
  for(const row of judged.combinedConclusions.rows) {
    let expected;
    if(row.ctStatus==='UNDETERMINED'||row.peakStatus==='UNDETERMINED')expected='UNDETERMINED';
    else if(row.mode==='ALL')expected=[row.ctStatus,row.peakStatus].includes('NEGATIVE')?'NEGATIVE':row.ctStatus==='POSITIVE'&&row.peakStatus==='POSITIVE'?'POSITIVE':'SUSPICIOUS';
    else expected=[row.ctStatus,row.peakStatus].includes('POSITIVE')?'POSITIVE':[row.ctStatus,row.peakStatus].includes('SUSPICIOUS')?'SUSPICIOUS':'NEGATIVE';
    assert.equal(row.status,expected);
  }
  evidence.checks.editedCtAndPeakRulesAppliedTogether=true;evidence.checks.configuredCombinedConclusionComputed=true;
  const saved=await api('pcr/save',{analysisId:judged.analysisRunId,fileName:'advanced-melt-reanalyzed.pcr'});
  const response=await fetch(base+'/api/admin/files/'+saved.FileID+'/download',{headers});assert.equal(response.status,200);const output=Buffer.from(await response.arrayBuffer());
  const path=join(run,'advanced-melt-reanalyzed.pcr');writeFileSync(path,output);
  const reopened=await rpc('OpenExperiment',{file:path});assert.equal(reopened.ErrorCode,0);
  const reopenedParameters=await rpc('GetExperimentParamters',{name:reopened.Result.Name});assert.equal(reopenedParameters.ErrorCode,0);
  const actual=reopenedParameters.Result[projectIndex].Targets[targetIndex].Paramter;
  for(const field of ['AutoThreshold','QualitativeAnalysis','OptimizationMode','DigitalFilter','PeekHightThreshold','MinTemperature','MaxTemperature','MeltPeakAlgorithm','PeekType'])assert.equal(actual[field],effective[field],field+' persisted in saved PCR');
  await rpc('CloseExperiment',{name:reopened.Result.Name});
  evidence.checks.advancedParametersPersistInsideSavedPcr=true;
  const child=await api('pcr/read',{fileId:saved.FileID,parentAnalysisId:judged.analysisRunId});assert.equal(child.ruleVersionId,version.id);evidence.checks.derivedPcrInheritsEditedRuleVersion=true;
  const artifactDir=join(root,'runtime/simulations','advanced-'+basename(run));mkdirSync(artifactDir,{recursive:true});
  writeFileSync(join(artifactDir,'advanced-melt-reanalyzed.pcr'),output);for(const f of ruleFiles)writeFileSync(join(artifactDir,f.fileName),JSON.stringify(f.rules));
  evidence.artifactDirectory=artifactDir;evidence.analysisRunId=judged.analysisRunId;evidence.ruleVersionId=version.id;evidence.parameters=parameters;
  evidence.effectiveParameterSets=judged.parameterSets;evidence.combinedConclusions=judged.combinedConclusions;evidence.measuredPeak={tm:peak.Tm,rm:peak.Rm};
  assert.equal(hash(readFileSync(source)),evidence.meltSourceSha256);evidence.checks.originalMeltPcrUnchanged=true;
  return evidence;
}
