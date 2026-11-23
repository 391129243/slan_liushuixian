var builder = WebApplication.CreateBuilder(args);
builder.Services.AddSingleton<SlanOfflineService>();
// Vendor .NET Framework process remains separate; this host uses its documented offline HTTP API.
builder.WebHost.UseUrls(Environment.GetEnvironmentVariable("PCR_BRIDGE_URL") ?? "http://127.0.0.1:5099");
var app = builder.Build();
app.Use(async (context, next) => {
    try { await next(); }
    catch (BridgeFault error) {
        context.Response.StatusCode = error.Status;
        await context.Response.WriteAsJsonAsync(new { code = error.Code, message = error.Message });
    }
});
app.MapGet("/health", () => Results.Ok(new { status = "ok", service = "pcr-sdk-bridge" }));
app.MapGet("/v1/capabilities", (SlanOfflineService sdk) => sdk.Capabilities());
app.MapPost("/v1/pcr/read", (ReadPcrRequest request, SlanOfflineService sdk) => sdk.Read(request));
app.MapPost("/v1/pcr/reanalyze", (ReanalyzeRequest request, SlanOfflineService sdk) => sdk.Reanalyze(request));
app.MapPost("/v1/pcr/save", (SavePcrRequest request, SlanOfflineService sdk) => sdk.Save(request));
app.MapGet("/v1/pcr/files/{id}", (string id, SlanOfflineService sdk) => {
    var output = sdk.GetOutput(id);
    return Results.File(output.Path, "application/octet-stream", output.FileName);
});
app.Run();

public record ReadPcrRequest(string FileId, string FileName, string SourcePath, RuleAttachmentRequest[]? Attachments = null);
public record RuleAttachmentRequest(string FileName, string SourcePath, string Sha256, string Role);
public record TargetSettings(int? BaselineStart=null,int? BaselineEnd=null,double? Threshold=null,bool? AutoThreshold=null,
    bool? QualitativeAnalysis=null,int? OptimizationMode=null,int? DigitalFilter=null,double? PeakHeightThreshold=null,
    double? MinTemperature=null,double? MaxTemperature=null,int? MeltPeakAlgorithm=null,int? PeakType=null);
public record ProjectOverride(int ProjectIndex,int? AmpAlgorithm);
public record TargetOverride(int ProjectIndex,int TargetIndex,TargetSettings Settings);
public record AnalysisParameters(bool AutoBaseline,int BaselineStart,int BaselineEnd,double Threshold,
    string? Scope=null,TargetSettings? Defaults=null,ProjectOverride[]? Projects=null,TargetOverride[]? Targets=null);
public record ReanalyzeRequest(string ExperimentId, AnalysisParameters Parameters,RuleAttachmentRequest[]? Attachments=null);
public record SavePcrRequest(string AnalysisId, string FileName);
