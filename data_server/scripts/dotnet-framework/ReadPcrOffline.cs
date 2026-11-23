using System;
using System.IO;
using System.Reflection;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System.Collections.Generic;

// Runs the supplied vendor's OFFLINE public file API, without service startup or instrument calls.
// Compile with the Windows .NET Framework compiler and run only on a private input copy.
public static class ReadPcrOffline
{
    static string vendor;
    static int failures;
    static string diagnostics;
    [STAThread]
    public static int Main(string[] args)
    {
        if (args.Length != 3) { Console.Error.WriteLine("Expected vendor-directory, private-copy.pcr, output.json"); return 2; }
        vendor = Path.GetFullPath(args[0]);
        AppDomain.CurrentDomain.AssemblyResolve += delegate(object sender, ResolveEventArgs resolve) {
            var requested = new AssemblyName(resolve.Name);
            if (requested.Name == "MyMath") {
                var loader = Assembly.LoadFrom(Path.Combine(vendor, "Images.dll")).GetType("Images.DumpInvoke");
                loader.GetField("DebugMode").SetValue(null, false);
                return (Assembly)loader.GetMethod("GetDefaultMath").Invoke(null, null);
            }
            var name = requested.Name + ".dll";
            foreach (var folder in new[] { vendor, Path.Combine(vendor, "DLL") }) {
                string path = Path.Combine(folder, name);
                if (File.Exists(path)) {
                    var candidate = AssemblyName.GetAssemblyName(path);
                    if (!String.Equals(BitConverter.ToString(candidate.GetPublicKeyToken() ?? new byte[0]),
                        BitConverter.ToString(requested.GetPublicKeyToken() ?? new byte[0]), StringComparison.OrdinalIgnoreCase)) continue;
                    return Assembly.LoadFrom(path);
                }
            }
            return null;
        };
        failures = 0;
        bool logging = false;
        diagnostics = Path.Combine(Path.GetDirectoryName(Path.GetFullPath(args[2])), "reader-diagnostics.log");
        AppDomain.CurrentDomain.FirstChanceException += delegate(object sender, System.Runtime.ExceptionServices.FirstChanceExceptionEventArgs error) {
            if (logging || failures >= 30) return;
            logging = true;
            try { failures++; File.AppendAllText(diagnostics, error.Exception.ToString() + Environment.NewLine, Encoding.UTF8); }
            finally { logging = false; }
        };
        try { return Execute(args[1], args[2]); }
        catch (Exception error) { Console.Error.WriteLine(error.ToString()); return 1; }
    }
    // Separate method prevents JIT dependency loading before the resolver is installed.
    [System.Runtime.CompilerServices.MethodImpl(System.Runtime.CompilerServices.MethodImplOptions.NoInlining)]
    static int Execute(string source, string output)
    {
        // Vendor models require this UI-only color resource even for their offline file API.
        var application = new System.Windows.Application();
        application.Resources["ColorDataGridCellFillGrey"] = System.Windows.Media.Colors.Gray;
        Images.DumpInvoke.Init();
        var service = new Service.SLANService();
        if (source == "--stdio") return ServeOffline(service, output);
        var experiment = service.LoadExperimentResult(Path.GetFullPath(source));
        if (experiment == null || experiment.WellResult == null || experiment.WellResult.Length == 0)
            throw new InvalidOperationException("Vendor did not return a populated experiment");
        string json = JsonConvert.SerializeObject(experiment, Formatting.None);
        File.WriteAllText(output, json, new UTF8Encoding(false));
        Console.WriteLine("Offline vendor result exported; no device APIs called.");
        return 0;
    }

    // Test transport around actual vendor methods, NOT a substitute algorithm or instrument server.
    // The only methods reachable over stdin are offline operations on private files in this work root.
    static int ServeOffline(Service.SLANService service, string workRoot)
    {
        Console.InputEncoding = new UTF8Encoding(false);
        Console.OutputEncoding = new UTF8Encoding(false);
        string root = Path.GetFullPath(workRoot).TrimEnd(Path.DirectorySeparatorChar) + Path.DirectorySeparatorChar;
        var owned = new HashSet<string>(StringComparer.Ordinal);
        var allowed = new HashSet<string> { "GetVersion", "LoadExperimentResult", "GetAllExperimentName", "OpenExperiment",
            "GetExperimentParamters", "SetExperimentParamter", "GetExperimentData", "SaveExperiment", "CloseExperiment" };
        Console.WriteLine("PCR_VENDOR_METADATA:" + JsonConvert.SerializeObject(new {
            serviceProductVersion = System.Diagnostics.FileVersionInfo.GetVersionInfo(Path.Combine(vendor, "Service.dll")).ProductVersion
        }));
        Console.WriteLine("PCR_RPC_READY"); Console.Out.Flush();
        string line;
        try {
            while ((line = Console.ReadLine()) != null) {
                string id = null;
                try {
                    var request = JObject.Parse(line); id = (string)request["id"];
                    string method = (string)request["method"];
                    if (!allowed.Contains(method)) throw new InvalidOperationException("Only allowlisted offline methods are available");
                    failures = 0; File.AppendAllText(diagnostics, "OFFLINE METHOD: " + method + Environment.NewLine, Encoding.UTF8);
                    var body = request["body"] as JObject ?? new JObject();
                    if (method == "LoadExperimentResult" || method == "OpenExperiment") {
                        string file = Path.GetFullPath((string)body["file"]);
                        if (!file.StartsWith(root, StringComparison.OrdinalIgnoreCase) || !File.Exists(file))
                            throw new InvalidOperationException("SDK input must be a private work-root file");
                    }
                    if (body["name"] != null && !owned.Contains((string)body["name"]))
                        throw new InvalidOperationException("Experiment was not opened by this isolated worker");
                    var operation = typeof(Service.SLANService).GetMethod(method);
                    var parameters = operation.GetParameters(); var values = new object[parameters.Length];
                    for (int i = 0; i < parameters.Length; i++) {
                        var value = body[parameters[i].Name];
                        if (value == null) throw new InvalidOperationException("Missing offline method parameter " + parameters[i].Name);
                        // Match WCF's known-type handling rather than erasing TargetParamterMelt to its base class.
                        var serializer = JsonSerializer.Create(new JsonSerializerSettings { Converters = { new TargetParameterConverter() } });
                        values[i] = value.ToObject(parameters[i].ParameterType,serializer);
                    }
                    object returned = operation.Invoke(service, values);
                    JToken result = returned == null ? JValue.CreateNull() : JToken.FromObject(returned);
                    if (method == "OpenExperiment" && (int?)result["ErrorCode"] == 0) {
                        string name = (string)result["Result"]["Name"]; owned.Add(name);
                        var management = Assembly.LoadFrom(Path.Combine(vendor, "SExperiment.dll")).GetType("SExperiment.ExperimentRunManagement");
                        var manager = management.GetMethod("GetInstance").Invoke(null, null);
                        var loaded = management.GetMethod("GetExperimentByName").Invoke(manager, new object[] { name });
                        var type = loaded.GetType();
                        // A standalone offline host has no vendor UI to refresh the loaded model status.
                        // Use its PUBLIC completeness-based refresh; never assign Completed or bypass CanSave.
                        string beforeStatus = type.GetProperty("Status").GetValue(loaded, null).ToString();
                        type.GetMethod("RefreshStatus").Invoke(loaded, null);
                        Console.WriteLine("PCR_MODEL_STATUS:" + JsonConvert.SerializeObject(new {
                            beforeStatus = beforeStatus,
                            status = type.GetProperty("Status").GetValue(loaded, null).ToString(),
                            enoughData = type.GetMethod("HasEnoughData").Invoke(loaded, null),
                            canSave = type.GetMethod("CanSave").Invoke(loaded, null)
                        }));
                    }
                    if (method == "CloseExperiment" && (int?)result["ErrorCode"] == 0) owned.Remove((string)body["name"]);
                    Console.WriteLine("PCR_RPC:" + JsonConvert.SerializeObject(new { id = id, result = result }));
                } catch (Exception error) {
                    var actual = error is TargetInvocationException && error.InnerException != null ? error.InnerException : error;
                    Console.WriteLine("PCR_RPC:" + JsonConvert.SerializeObject(new { id = id, error = actual.ToString() }));
                }
                Console.Out.Flush();
            }
        } finally {
            foreach (string name in owned) { try { service.CloseExperiment(name); } catch { } }
        }
        return 0;
    }

    sealed class TargetParameterConverter : JsonConverter
    {
        public override bool CanConvert(Type type) { return type == typeof(SLAN.API.Model.TargetParamter); }
        public override bool CanWrite { get { return false; } }
        public override object ReadJson(JsonReader reader,Type type,object existing,JsonSerializer serializer)
        {
            var value=JObject.Load(reader);
            string marker=(string)value["__type"];
            if(marker=="TargetParamterMelt:#SLAN.API.Model")return value.ToObject<SLAN.API.Model.TargetParamterMelt>();
            if(marker!=null && marker!="TargetParamter:#SLAN.API.Model")throw new InvalidOperationException("Unsupported target parameter data-contract type");
            return value.ToObject<SLAN.API.Model.TargetParamter>();
        }
        public override void WriteJson(JsonWriter writer,object value,JsonSerializer serializer) { throw new NotSupportedException(); }
    }
}
