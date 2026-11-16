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

public record ReadPcrRequest(string FileId, string FileName, string SourcePath);
public record AnalysisParameters(bool AutoBaseline, int BaselineStart, int BaselineEnd, double Threshold);
public record ReanalyzeRequest(string ExperimentId, AnalysisParameters Parameters);
public record SavePcrRequest(string AnalysisId, string FileName);
