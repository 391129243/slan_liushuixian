// Test substitute only: does not load vendor code or connect to instruments.
var builder = WebApplication.CreateBuilder(args);
builder.WebHost.UseUrls(File.ReadAllText("listen-url.txt").Trim());
var app = builder.Build();
app.MapPost("/SlanServerService/GetVersion", () => Results.Text("{\"GetVersionResult\":\"0.0.0.1\"}", "application/json"));
app.MapGet("/fixture", () => Results.Ok(new { fixture = true, directory = Environment.CurrentDirectory, pid = Environment.ProcessId }));
app.Run();
