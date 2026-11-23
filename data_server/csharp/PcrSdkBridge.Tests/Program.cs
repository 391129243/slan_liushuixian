using System.Text.Json.Nodes;
static void Check(bool ok,string message) { if(!ok)throw new Exception(message); }
static void Reject(Action work,int status=400) { try { work();throw new Exception("Expected rejection"); }catch(BridgeFault e){Check(e.Status==status,e.Message);} }
JsonArray Fixture() => JsonNode.Parse("""
[{"ProjectName":"P1","AMPAlgorithm":0,"Keep":[1,2],"Targets":[
 {"Name":"FAM","Paramter":{"__type":"TargetParamterMelt:#SDK","BeginBaseline":3,"EndBaseline":12,"Threshold":0.12,"AutoThreshold":false,"QualitativeAnalysis":true,"DigitalFilter":2,"OptimizationMode":1,"PeekHightThreshold":100,"MinTemperature":40,"MaxTemperature":95,"MeltPeakAlgorithm":0,"PeekType":0}},
 {"Name":"HEX","Paramter":{"BeginBaseline":3,"EndBaseline":12,"Threshold":0.12,"AutoThreshold":false,"DigitalFilter":2,"OptimizationMode":1}}]},
 {"ProjectName":"P2","AMPAlgorithm":0,"Targets":[{"Name":"FAM","Paramter":{"BeginBaseline":3,"EndBaseline":12,"Threshold":0.12,"AutoThreshold":false,"DigitalFilter":2,"OptimizationMode":1}}]}]
""")!.AsArray();
var settings=new TargetSettings(6,15,0.24,true,false,2,1,200,50,90,1,2);
var p=new AnalysisParameters(true,6,12,0.24,"SELECTED",null,[new(0,2)],[new(0,0,settings)]);
var sets=Fixture();string untouched=sets[1]!.ToJsonString(),hex=sets[0]!["Targets"]![1]!.ToJsonString();
Check(AnalysisParameterEditor.Apply(sets,p)==1,"Exactly one target changed");
Check(sets[1]!.ToJsonString()==untouched&&sets[0]!["Targets"]![1]!.ToJsonString()==hex,"Other projects/targets must be unchanged");
var effective=sets[0]!["Targets"]![0]!["Paramter"]!;
Check(effective["__type"]!.GetValue<string>()=="TargetParamterMelt:#SDK","Preserve polymorphic SDK type");
Check(effective["AutoThreshold"]!.GetValue<bool>()&&!effective["QualitativeAnalysis"]!.GetValue<bool>(),"Boolean settings applied");
Check(effective["MinTemperature"]!.GetValue<double>()==50&&effective["MeltPeakAlgorithm"]!.GetValue<int>()==1&&effective["PeekType"]!.GetValue<int>()==2,"Melt settings applied");
Check(sets[0]!["AMPAlgorithm"]!.GetValue<int>()==2&&effective["DigitalFilter"]!.GetValue<int>()==1,"Algorithm/filter applied");
Reject(()=>AnalysisParameterEditor.Apply(Fixture(),p with {Targets=[new(4,0,settings)]}));
Reject(()=>AnalysisParameterEditor.Apply(Fixture(),p with {Targets=[new(0,0,settings),new(0,0,settings)]}));
Reject(()=>AnalysisParameterEditor.Apply(Fixture(),p with {Targets=[new(0,1,settings)]}),422);
Reject(()=>AnalysisParameterEditor.Apply(Fixture(),p with {Targets=[new(0,0,new(MinTemperature:99))]}));
Reject(()=>AnalysisParameterEditor.Validate(p with {Targets=[new(0,0,new(DigitalFilter:9))]}));
Reject(()=>AnalysisParameterEditor.Validate(p with {Targets=[new(0,0,new(Threshold:double.NaN))]}));
Reject(()=>AnalysisParameterEditor.Validate(p with {Targets=[new(0,0,new(BaselineStart:15,BaselineEnd:6))]}));
sets=Fixture();AnalysisParameterEditor.Apply(sets,new(true,6,12,0.24));
Check(sets[1]!["Targets"]![0]!["Paramter"]!["Threshold"]!.GetValue<double>()==0.24,"Legacy global settings remain compatible");
sets=Fixture();var melt=sets[0]!["Targets"]![0]!["Paramter"]!.AsObject();melt.Remove("__type");
AnalysisParameterEditor.Apply(sets,new(true,6,12,0.24,"SELECTED",null,null,[new(1,0,new(Threshold:0.2))]));
Check(melt["__type"]!.GetValue<string>()=="TargetParamterMelt:#SLAN.API.Model","Unselected melt targets retain their known derived contract type when the full project is submitted");
sets=Fixture();sets[0]!["Targets"]![0]!["Paramter"]!["Threshold"]=0;
Reject(()=>AnalysisParameterEditor.Apply(sets,p with {Projects=null,Targets=[new(0,0,new(AutoThreshold:false))]}));
Console.WriteLine("PASS: selected target, all advanced fields, preservation, selectors, enum/range/melt validation, legacy compatibility");
