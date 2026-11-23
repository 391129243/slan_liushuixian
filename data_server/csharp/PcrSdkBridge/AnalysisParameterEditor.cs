using System.Text.Json.Nodes;
using System.Text.Json;

// SDK enum values verified against supplied SLAN.API.dll 9.0.1.260921, never inferred from UI positions.
public static class AnalysisParameterEditor
{
    static void Bad(string message) => throw new BridgeFault(400, "INVALID_PARAMETERS", message);
    static void EnumValue(int? value, int max, string field) { if (value is < 0 || value > max) Bad($"Invalid {field}"); }
    static void Number(double? value, string field, bool nonnegative = false) {
        if (value.HasValue && (!double.IsFinite(value.Value) || (nonnegative && value < 0))) Bad($"Invalid {field}");
    }
    public static void Validate(AnalysisParameters p) {
        if(p.BaselineStart<1 || p.BaselineEnd>20 || p.BaselineStart>=p.BaselineEnd || !double.IsFinite(p.Threshold) || p.Threshold<=0) Bad("Invalid baseline or threshold");
        if(p.Scope is not null and not "ALL" and not "SELECTED") Bad("scope must be ALL or SELECTED");
        if((p.Targets?.Length??0)>1024 || (p.Projects?.Length??0)>128) Bad("Too many parameter overrides");
        if(p.Scope=="SELECTED" && (p.Targets?.Length??0)==0 && (p.Projects?.Length??0)==0) Bad("Select at least one project or target");
        ValidateSettings(p.Defaults);
        var projects=new HashSet<int>();var targets=new HashSet<(int,int)>();
        foreach(var item in p.Projects??[]) {
            if(item is null || item.ProjectIndex<0 || !projects.Add(item.ProjectIndex)) Bad("Invalid or duplicate project selector");
            EnumValue(item!.AmpAlgorithm,2,"AMPAlgorithm");
        }
        foreach(var item in p.Targets??[]) {
            if(item is null || item.ProjectIndex<0 || item.TargetIndex<0 || !targets.Add((item.ProjectIndex,item.TargetIndex)) || item.Settings is null) Bad("Invalid or duplicate target selector");
            ValidateSettings(item!.Settings);
        }
    }
    static void ValidateSettings(TargetSettings? s) {
        if(s is null)return;
        if(s.BaselineStart is <1 or >19 || s.BaselineEnd is <2 or >20 || (s.BaselineStart.HasValue&&s.BaselineEnd.HasValue&&s.BaselineStart>=s.BaselineEnd)) Bad("Invalid target baseline");
        Number(s.Threshold,"threshold");if(s.Threshold<=0)Bad("Threshold must be positive");
        EnumValue(s.OptimizationMode,2,"optimizationMode");EnumValue(s.DigitalFilter,2,"digitalFilter");
        EnumValue(s.MeltPeakAlgorithm,1,"meltPeakAlgorithm");EnumValue(s.PeakType,2,"peakType");
        Number(s.PeakHeightThreshold,"peakHeightThreshold",true);Number(s.MinTemperature,"minTemperature");Number(s.MaxTemperature,"maxTemperature");
        if(s.MinTemperature.HasValue&&s.MaxTemperature.HasValue&&s.MinTemperature>=s.MaxTemperature)Bad("Minimum melt temperature must be below maximum");
    }
    public static int Apply(JsonArray sets,AnalysisParameters p) {
        Validate(p);if(sets.Count==0)throw new BridgeFault(422,"NO_TARGETS","No analysis parameter sets");
        foreach(var item in p.Projects??[])if(item.ProjectIndex>=sets.Count)Bad("Project selector does not exist");
        foreach(var item in p.Targets??[])if(item.ProjectIndex>=sets.Count || sets[item.ProjectIndex]?["Targets"] is not JsonArray a || item.TargetIndex>=a.Count)Bad("Target selector does not exist");
        int count=0;
        for(int projectIndex=0;projectIndex<sets.Count;projectIndex++) {
            var project=sets[projectIndex] as JsonObject??throw new BridgeFault(502,"VENDOR_SCHEMA","Invalid project parameters");
            var projectOverride=p.Projects?.SingleOrDefault(o=>o.ProjectIndex==projectIndex);
            if(projectOverride?.AmpAlgorithm is int algorithm)project["AMPAlgorithm"]=algorithm;
            var targets=project["Targets"] as JsonArray??throw new BridgeFault(502,"VENDOR_SCHEMA","Missing targets");
            for(int targetIndex=0;targetIndex<targets.Count;targetIndex++) {
                var selected=p.Targets?.SingleOrDefault(o=>o.ProjectIndex==projectIndex&&o.TargetIndex==targetIndex);
                var settings=targets[targetIndex]?["Paramter"] as JsonObject??throw new BridgeFault(502,"VENDOR_SCHEMA","Missing target settings");
                // WCF needs the known derived data-contract type; some JSON transports omit it on reads.
                if(settings.ContainsKey("MinTemperature") && settings.ContainsKey("MaxTemperature") && !settings.ContainsKey("__type"))
                    settings["__type"]="TargetParamterMelt:#SLAN.API.Model";
                if(p.Scope=="SELECTED" && selected is null)continue;
                if(p.Scope!="SELECTED") {
                    settings["BeginBaseline"]=p.BaselineStart;settings["EndBaseline"]=p.BaselineEnd;
                    settings["OptimizationMode"]=p.AutoBaseline?1:2;settings["AutoThreshold"]=false;settings["Threshold"]=p.Threshold;
                }
                Patch(settings,p.Defaults);Patch(settings,selected?.Settings);
                bool baselineChanged=p.Scope!="SELECTED" || p.Defaults?.BaselineStart!=null || p.Defaults?.BaselineEnd!=null || selected?.Settings.BaselineStart!=null || selected?.Settings.BaselineEnd!=null || p.Defaults?.OptimizationMode==2 || selected?.Settings.OptimizationMode==2;
                CheckEffective(settings,baselineChanged);count++;
            }
        }
        if(count==0 && (p.Projects?.Length??0)==0)throw new BridgeFault(422,"NO_TARGETS","No target parameters selected");return count;
    }
    static void Patch(JsonObject o,TargetSettings? s) {
        if(s is null)return;
        void Put<T>(string key,T? value)where T:struct { if(value.HasValue) { if(!o.ContainsKey(key))throw new BridgeFault(422,"UNSUPPORTED_PARAMETER",$"Target does not support {key}");o[key]=System.Text.Json.JsonSerializer.SerializeToNode(value.Value); } }
        Put("BeginBaseline",s.BaselineStart);Put("EndBaseline",s.BaselineEnd);Put("Threshold",s.Threshold);Put("AutoThreshold",s.AutoThreshold);
        Put("QualitativeAnalysis",s.QualitativeAnalysis);Put("OptimizationMode",s.OptimizationMode);Put("DigitalFilter",s.DigitalFilter);
        Put("PeekHightThreshold",s.PeakHeightThreshold);Put("MinTemperature",s.MinTemperature);Put("MaxTemperature",s.MaxTemperature);
        Put("MeltPeakAlgorithm",s.MeltPeakAlgorithm);Put("PeekType",s.PeakType);
    }
    static void CheckEffective(JsonObject s,bool baselineChanged) {
        var start=s["BeginBaseline"]?.GetValue<int>();var end=s["EndBaseline"]?.GetValue<int>();
        if(baselineChanged&&(start is null || end is null || start<1 || end>20 || start>=end))Bad("Effective baseline must satisfy 1 <= start < end <= 20");
        if(s["AutoThreshold"]?.GetValue<bool>()==false) {
            double? threshold=ReadNumber(s["Threshold"]);
            if(threshold is null || !double.IsFinite(threshold.Value) || threshold<=0)Bad("Manual threshold must be a positive finite number");
        }
        if(s["MinTemperature"] is JsonNode min&&s["MaxTemperature"] is JsonNode max&&ReadNumber(min)>=ReadNumber(max))Bad("Invalid effective melt range");
    }
    static double? ReadNumber(JsonNode? value) {
        if(value is null)return null;
        try { return value.Deserialize<double>(); }catch(JsonException){Bad("Target contains a nonnumeric parameter");return null;}
    }
}
