using System.Collections.Concurrent;
using System.Net.Http.Json;
using System.Security.Cryptography;
using System.Text.Json.Nodes;

// Only offline file operations are exposed. No instrument enumeration, start, stop or release.
public sealed class SlanOfflineService
{
    readonly bool enabled = Environment.GetEnvironmentVariable("PCR_VENDOR_ENABLED") == "true";
    readonly HttpClient http;
    readonly string inputRoot;
    readonly string workRoot;
    readonly SemaphoreSlim gate = new(1, 1);
    readonly ConcurrentDictionary<string, Session> sessions = new();
    readonly ConcurrentDictionary<string, Analysis> analyses = new();
    readonly ConcurrentDictionary<string, Output> outputs = new();
    record Sidecar(string FileName, string File, string Sha256, string Role);
    record Session(string File, string Name, string SourceHash, Sidecar[] Attachments);
    record Analysis(string File, string ExperimentId, Sidecar[] Attachments);
    public record Output(string Path, string FileName, string Hash, long Size);

    public SlanOfflineService()
    {
        var uri = new Uri((Environment.GetEnvironmentVariable("PCR_VENDOR_URL") ?? "http://127.0.0.1:8186/SlanServerService").TrimEnd('/') + "/");
        if (!uri.IsLoopback || (uri.Scheme != "http" && uri.Scheme != "https"))
            throw new InvalidOperationException("Vendor URL must be a loopback HTTP endpoint");
        http = new HttpClient { BaseAddress = uri, Timeout = TimeSpan.FromSeconds(30), MaxResponseContentBufferSize = 64 * 1024 * 1024 };
        var token = Environment.GetEnvironmentVariable("PCR_VENDOR_TOKEN");
        if (!string.IsNullOrWhiteSpace(token)) http.DefaultRequestHeaders.Add("X-Auth-Token", token);
        inputRoot = Path.GetFullPath(Environment.GetEnvironmentVariable("PCR_INPUT_ROOT") ?? Path.Combine(Environment.CurrentDirectory, "runtime", "files"));
        workRoot = Path.GetFullPath(Environment.GetEnvironmentVariable("PCR_WORK_ROOT") ?? Path.Combine(Environment.CurrentDirectory, "runtime", "slan-offline"));
        if (enabled) Directory.CreateDirectory(workRoot);
    }
    public async Task<object> Capabilities()
    {
        if (!enabled) return new { bridgeAvailable = true, sdkReady = false, read = false, reanalyze = false, savePcr = false,
            vendorAdapterImplemented = true, message = "Offline adapter configured; PCR_VENDOR_ENABLED is false. No vendor calls performed." };
        await gate.WaitAsync();
        try {
            var version = await Call("GetVersion", new { });
            return new { bridgeAvailable = true, sdkReady = true, read = true, reanalyze = true, savePcr = true,
                vendorVersion = version, message = "Vendor version endpoint reachable. File compatibility still requires verification." };
        } finally { gate.Release(); }
    }
    void RequireEnabled()
    {
        if (!enabled) throw new BridgeFault(501, "SDK_DISABLED", "Offline vendor adapter is disabled; no instrument or vendor process is started automatically.");
    }
    static string Hash(string path) { using var stream = File.OpenRead(path); return Convert.ToHexString(SHA256.HashData(stream)).ToLowerInvariant(); }
    static string Required(JsonObject obj, string name) => obj[name]?.GetValue<string>() is { Length: > 0 } value
        ? value : throw new BridgeFault(502, "VENDOR_SCHEMA", $"Missing vendor field {name}");
    static JsonObject AsObject(JsonNode? node) => node as JsonObject ?? throw new BridgeFault(502, "VENDOR_SCHEMA", "Expected vendor object");
    string ValidateSource(string source)
    {
        if (string.IsNullOrWhiteSpace(source)) throw new BridgeFault(400, "INVALID_SOURCE", "Source path is required");
        var path = Path.GetFullPath(source);
        var prefix = Path.TrimEndingDirectorySeparator(inputRoot) + Path.DirectorySeparatorChar;
        if (!path.StartsWith(prefix, StringComparison.OrdinalIgnoreCase) || !File.Exists(path))
            throw new BridgeFault(400, "INVALID_SOURCE", "Source must be an existing file under PCR_INPUT_ROOT");
        // Do not follow a junction/symlink outside the trusted input directory.
        for (var current = path; current.Length >= inputRoot.Length; current = Path.GetDirectoryName(current) ?? "") {
            if ((File.GetAttributes(current) & FileAttributes.ReparsePoint) != 0)
                throw new BridgeFault(400, "INVALID_SOURCE", "Symlink/reparse paths are not accepted");
            if (current.Equals(inputRoot, StringComparison.OrdinalIgnoreCase)) break;
        }
        long size = new FileInfo(path).Length;
        if (size <= 0 || size > 32 * 1024 * 1024) throw new BridgeFault(400, "INVALID_SOURCE", "File size must be 1 byte to 32 MiB");
        return path;
    }
    string NewWorkFile(string name)
    {
        var directory = Path.Combine(workRoot, Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(directory);
        return Path.Combine(directory, name);
    }
    static string AttachmentRole(string filename)
    {
        if (string.IsNullOrWhiteSpace(filename) || filename.Length > 255 || filename.IndexOfAny(['/', '\\', ':']) >= 0
            || filename.Any(char.IsControl) || filename.IndexOfAny(['"', '<', '>', '|', '?', '*']) >= 0 || filename.EndsWith(' ') || filename.EndsWith('.'))
            throw new BridgeFault(400, "INVALID_RULE_FILE", "Unsafe rule filename");
        string basename = filename.Split('.')[0].Trim().ToUpperInvariant();
        if (System.Text.RegularExpressions.Regex.IsMatch(basename, "^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])$"))
            throw new BridgeFault(400, "INVALID_RULE_FILE", "Reserved rule filename");
        return Path.GetExtension(filename).ToLowerInvariant() switch {
            ".ct" => "CT_RULE", ".peak" => "PEAK_RULE", ".conclusion" => "CONCLUSION_ARCHIVE",
            _ when filename.Equals("judgeConclusion.result", StringComparison.OrdinalIgnoreCase) => "DEVICE_CONCLUSION",
            _ => throw new BridgeFault(400, "INVALID_RULE_FILE", "Unsupported rule attachment type")
        };
    }
    Sidecar[] CopyInputAttachments(RuleAttachmentRequest[]? items, string directory)
    {
        if (items == null) return [];
        if (items.Length > 64) throw new BridgeFault(400, "INVALID_RULE_FILES", "Too many attachments");
        var names = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        var result = new List<Sidecar>();
        foreach (var item in items) {
            if (item == null || AttachmentRole(item.FileName) != item.Role || !names.Add(item.FileName))
                throw new BridgeFault(400, "INVALID_RULE_FILES", "Invalid or duplicate rule attachment");
            var source = ValidateSource(item.SourcePath);
            if (new FileInfo(source).Length > 2 * 1024 * 1024 || Hash(source) != item.Sha256)
                throw new BridgeFault(400, "RULE_CHECKSUM", "Rule file checksum or size mismatch");
            string copy = Path.Combine(directory, item.FileName); File.Copy(source, copy, false);
            if (Hash(copy) != item.Sha256) throw new BridgeFault(400, "RULE_CHECKSUM", "Copied rule file checksum mismatch");
            result.Add(new Sidecar(item.FileName, copy, item.Sha256, item.Role));
        }
        return result.ToArray();
    }
    Sidecar[] CopyFrozenAttachments(Sidecar[] items, string directory)
    {
        return items.Select(item => {
            if (Hash(item.File) != item.Sha256) throw new BridgeFault(422, "RULE_CHECKSUM", "Private rule snapshot changed");
            string copy = Path.Combine(directory, item.FileName); File.Copy(item.File, copy, false);
            if (Hash(copy) != item.Sha256) throw new BridgeFault(422, "RULE_CHECKSUM", "Copied private rule snapshot changed");
            return new Sidecar(item.FileName, copy, item.Sha256, item.Role);
        }).ToArray();
    }
    static object[] AttachmentSummary(Sidecar[] items) => items.Select(item => (object)new {
        fileName = item.FileName, sha256 = item.Sha256, role = item.Role
    }).ToArray();
    public async Task<object> Read(ReadPcrRequest request)
    {
        RequireEnabled();
        await gate.WaitAsync();
        try {
            if (sessions.Count >= 128) throw new BridgeFault(429, "SESSION_LIMIT", "Restart this isolated bridge to clear expired handles");
            var source = ValidateSource(request.SourcePath);
            var copy = NewWorkFile("source.pcr"); File.Copy(source, copy, false);
            var attachments = CopyInputAttachments(request.Attachments, Path.GetDirectoryName(copy)!);
            // Unlike OpenExperiment, LoadExperimentResult returns Experiment directly, not APIExperimentResult.
            var result = AsObject(await Call("LoadExperimentResult", new { file = copy }));
            // Open only our private copy to obtain the complete polymorphic parameter sets for editing.
            var existing = await Checked("GetAllExperimentName", new { }) as JsonArray;
            if(existing is null || existing.Any(n=>n?.GetValue<string>()==Required(result,"Name")))
                throw new BridgeFault(409,"EXPERIMENT_ALREADY_OPEN","Private experiment name is already open");
            string? opened=null;JsonNode? parameterSets;
            try {
                opened=Required(AsObject(await Checked("OpenExperiment",new { file=copy })),"Name");
                parameterSets=await Checked("GetExperimentParamters",new { name=opened });
            } finally { if(opened!=null)await Checked("CloseExperiment",new { name=opened },true); }
            var id = Guid.NewGuid().ToString("N");
            var hash = Hash(copy); sessions[id] = new Session(copy, Required(result, "Name"), hash, attachments);
            return new { experimentId = id, experiment = result, parameterSets, sourceSha256 = hash, attachments = AttachmentSummary(attachments), applicationRulesApplied = false, mock = false };
        } finally { gate.Release(); }
    }
    public async Task<object> Reanalyze(ReanalyzeRequest request)
    {
        RequireEnabled();
        var p = request.Parameters;
        if(p is null)throw new BridgeFault(400,"INVALID_PARAMETERS","Missing parameters");
        AnalysisParameterEditor.Validate(p);
        await gate.WaitAsync();
        try {
            if (request.ExperimentId == null || !sessions.TryGetValue(request.ExperimentId, out var session))
                throw new BridgeFault(404, "SESSION_NOT_FOUND", "Read the file again; experiment handles are process-local");
            if (analyses.Count >= 128) throw new BridgeFault(429, "SESSION_LIMIT", "Maximum analyses reached");
            var names = await Checked("GetAllExperimentName", new { }) as JsonArray
                ?? throw new BridgeFault(502, "VENDOR_SCHEMA", "Opened experiment names must be an array");
            if (names.Any(n => n?.GetValue<string>() == session.Name))
                throw new BridgeFault(409, "EXPERIMENT_ALREADY_OPEN", "Experiment is already open in the vendor instance; it will not be replaced or closed");
            var working = NewWorkFile("analysis.pcr"); File.Copy(session.File, working, false);
            var attachments = request.Attachments is null ? CopyFrozenAttachments(session.Attachments, Path.GetDirectoryName(working)!)
                : CopyInputAttachments(request.Attachments,Path.GetDirectoryName(working)!);
            string? opened = null;
            bool closeAttempted = false;
            try {
                var result = AsObject(await Checked("OpenExperiment", new { file = working }));
                opened = Required(result, "Name");
                // Preserve complete returned polymorphic parameters, rules, filters and cross-talk data.
                var parameterSets = await Checked("GetExperimentParamters", new { name = opened }) as JsonArray
                    ?? throw new BridgeFault(502, "VENDOR_SCHEMA", "Analysis parameters must be an array");
                if (parameterSets.Count == 0) throw new BridgeFault(422, "NO_TARGETS", "Experiment has no analysis parameter sets");
                int count = AnalysisParameterEditor.Apply(parameterSets,p);
                foreach (var set in parameterSets) {
                    var parameter = AsObject(set);
                    var targets = parameter["Targets"] as JsonArray ?? throw new BridgeFault(502, "VENDOR_SCHEMA", "Targets array missing");
                    if (targets.Count > 0) await Checked("SetExperimentParamter", new { name = opened, paramter = parameter }, true);
                }
                var experiment = AsObject(await Checked("GetExperimentData", new { name = opened }));
                var effectiveParameterSets=await Checked("GetExperimentParamters",new { name=opened });
                // SaveExperiment has no output-path argument: it only writes our private working copy.
                await Checked("SaveExperiment", new { name = opened }, true);
                closeAttempted = true;
                await Checked("CloseExperiment", new { name = opened }, true); opened = null;
                var id = Guid.NewGuid().ToString("N"); analyses[id] = new Analysis(working, request.ExperimentId, attachments);
                return new { analysisId = id, experimentId = request.ExperimentId, experiment, parameterSets=effectiveParameterSets, requestedParameterSets=parameterSets, targetsChanged = count, attachments = AttachmentSummary(attachments), applicationRulesApplied = false, mock = false };
            } finally {
                if (opened != null && !closeAttempted) {
                    // Close only a name we successfully opened. Never release a device session.
                    await Checked("CloseExperiment", new { name = opened }, true);
                }
            }
        } finally { gate.Release(); }
    }
    public async Task<object> Save(SavePcrRequest request)
    {
        RequireEnabled();
        if (string.IsNullOrWhiteSpace(request.FileName) || request.FileName.Length > 255 || request.FileName.IndexOfAny(['/', '\\', ':']) >= 0
            || request.FileName.Any(char.IsControl) || !request.FileName.EndsWith(".pcr", StringComparison.OrdinalIgnoreCase))
            throw new BridgeFault(400, "INVALID_FILENAME", "A safe .pcr filename is required");
        await gate.WaitAsync();
        try {
            if (request.AnalysisId == null || !analyses.TryGetValue(request.AnalysisId, out var analysis))
                throw new BridgeFault(404, "ANALYSIS_NOT_FOUND", "Analysis handle not found");
            if (outputs.Count >= 128) throw new BridgeFault(429, "OUTPUT_LIMIT", "Maximum saved outputs reached");
            var path = NewWorkFile(request.FileName); File.Copy(analysis.File, path, false);
            var attachments = CopyFrozenAttachments(analysis.Attachments, Path.GetDirectoryName(path)!);
            var output = new Output(path, request.FileName, Hash(path), new FileInfo(path).Length);
            if (output.Size <= 0 || output.Size > 32 * 1024 * 1024) throw new BridgeFault(422, "INVALID_OUTPUT", "Unexpected saved file size");
            var id = Guid.NewGuid().ToString("N"); outputs[id] = output;
            return new { outputId = id, fileName = output.FileName, sha256 = output.Hash, size = output.Size, attachments = AttachmentSummary(attachments), mock = false };
        } finally { gate.Release(); }
    }
    public Output GetOutput(string id) => outputs.TryGetValue(id, out var output) ? output
        : throw new BridgeFault(404, "OUTPUT_NOT_FOUND", "Saved output not found");

    async Task<JsonNode?> Call(string method, object body)
    {
        try {
            using var response = await http.PostAsJsonAsync(method, body);
            if (!response.IsSuccessStatusCode) throw new BridgeFault(502, "VENDOR_HTTP", $"Vendor {method} returned HTTP {(int)response.StatusCode}");
            var json = await response.Content.ReadFromJsonAsync<JsonNode>();
            return json is JsonObject obj && obj.ContainsKey(method + "Result") ? obj[method + "Result"]?.DeepClone() : json;
        } catch (HttpRequestException) { throw new BridgeFault(503, "VENDOR_UNREACHABLE", "Vendor offline service is not reachable"); }
        catch (TaskCanceledException) { throw new BridgeFault(504, "VENDOR_TIMEOUT", "Vendor call timed out; no automatic retry was attempted"); }
        catch (System.Text.Json.JsonException) { throw new BridgeFault(502, "VENDOR_SCHEMA", "Invalid vendor JSON response"); }
    }
    async Task<JsonNode?> Checked(string method, object body, bool requireTrue = false)
    {
        var response = AsObject(await Call(method, body));
        if (response["ErrorCode"] is not JsonValue code || !code.TryGetValue<int>(out var error))
            throw new BridgeFault(502, "VENDOR_SCHEMA", $"Missing ErrorCode from {method}");
        if (error != 0) throw new BridgeFault(422, "VENDOR_API_ERROR", $"Vendor {method} ErrorCode={error}: {response["Message"]}");
        if (requireTrue && response["Result"]?.GetValue<bool>() != true)
            throw new BridgeFault(422, "VENDOR_FALSE_RESULT", $"Vendor {method} did not succeed");
        return response["Result"]?.DeepClone();
    }
}
public sealed class BridgeFault(int status, string code, string message) : Exception(message)
{
    public int Status { get; } = status;
    public string Code { get; } = code;
}
