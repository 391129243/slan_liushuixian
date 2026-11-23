package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class WindowsProcessOps implements ManagedProcessOps {
    private final ObjectMapper mapper;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    public WindowsProcessOps(ObjectMapper mapper) { this.mapper=mapper; }
    @Override public Optional<ProcessHandle> findVendor(Path executable) {
        List<ProcessHandle> matches=ProcessHandle.allProcesses().filter(process -> process.info().command().map(command -> {
            try {
                Path path=Path.of(command);
                return path.getFileName().toString().equalsIgnoreCase(executable.getFileName().toString()) && Files.isSameFile(path,executable);
            } catch(Exception error) { return false; }
        }).orElse(false)).toList();
        if(matches.size()>1) throw new IllegalStateException("Multiple matching SLAN.WCFServer processes already exist; stop duplicates before starting dataserver");
        return matches.stream().findFirst();
    }
    @Override public boolean portOpen(URI uri) {
        try(var socket=new Socket()) {
            socket.connect(new InetSocketAddress(uri.getHost(),port(uri)),500);return true;
        } catch(Exception error) { return false; }
    }
    private int port(URI uri) { return uri.getPort()>=0?uri.getPort():uri.getScheme().equals("https")?443:80; }
    @Override public ProcessHandle launch(Path executable,List<String> arguments,Path directory,Path logs,Map<String,String> environment) {
        if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows"))
            throw new IllegalStateException("Managed SLAN startup requires Windows; set DATASERVER_MANAGED_ENABLED=false for simulation");
        try {
            Files.createDirectories(logs.toAbsolutePath().getParent());
            // Paths and arguments are passed through environment variables, never interpolated as PowerShell code.
            String script="$ErrorActionPreference='Stop'; $options=@{FilePath=$env:PCR_CHILD_EXECUTABLE;WorkingDirectory=$env:PCR_CHILD_DIRECTORY;WindowStyle='Hidden';PassThru=$true;RedirectStandardOutput=$env:PCR_CHILD_STDOUT;RedirectStandardError=$env:PCR_CHILD_STDERR};"
                +"if($env:PCR_CHILD_ARGUMENTS){$options.ArgumentList=$env:PCR_CHILD_ARGUMENTS};$p=Start-Process @options;$p.Id";
            var builder=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-Command",script);
            var env=builder.environment();env.putAll(environment);
            env.put("PCR_CHILD_EXECUTABLE",executable.toString());env.put("PCR_CHILD_DIRECTORY",directory.toString());
            env.put("PCR_CHILD_ARGUMENTS",String.join(" ",arguments.stream().map(WindowsProcessOps::quote).toList()));
            env.put("PCR_CHILD_STDOUT",logs.toAbsolutePath()+".stdout.log");env.put("PCR_CHILD_STDERR",logs.toAbsolutePath()+".stderr.log");
            // Child processes may inherit wrapper handles on Windows. Use a regular PID file instead of waiting for pipe EOF.
            Path pidFile=Path.of(logs.toAbsolutePath()+".launch.pid");
            builder.redirectOutput(pidFile.toFile()).redirectError(Path.of(logs.toAbsolutePath()+".launch.stderr.log").toFile());
            Process launcher=builder.start();
            if(!launcher.waitFor(15,TimeUnit.SECONDS)) { launcher.destroyForcibly();throw new IllegalStateException("Timed out launching managed process"); }
            String output=Files.readString(pidFile,StandardCharsets.UTF_8).trim();
            if(launcher.exitValue()!=0) throw new IllegalStateException("Could not launch "+executable.getFileName()+"; verify permissions, executable and log directory");
            long pid=Long.parseLong(output);
            return ProcessHandle.of(pid).orElseThrow(() -> new IllegalStateException(executable.getFileName()+" exited during launch; inspect "+logs));
        } catch(InterruptedException error) { Thread.currentThread().interrupt();throw new IllegalStateException("Managed startup interrupted",error); }
        catch(java.io.IOException error) { throw new IllegalStateException("Managed process launch failed",error); }
    }
    static String quote(String argument) {
        if(argument.contains("\"") || argument.endsWith("\\")) throw new IllegalArgumentException("Unexpected process argument");
        return "\""+argument+"\"";
    }
    private JsonNode request(URI base,String suffix,boolean post) {
        try {
            URI uri=URI.create(base.toString().replaceAll("/+$","")+suffix);
            var request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2));
            if(post) request.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}"));
            else request.GET();
            String token=System.getenv("PCR_VENDOR_TOKEN");
            if(post && token!=null && !token.isBlank()) request.header("X-Auth-Token",token);
            var response=client.send(request.build(),HttpResponse.BodyHandlers.ofString());
            return response.statusCode()==200 ? mapper.readTree(response.body()) : null;
        } catch(Exception error) { if(error instanceof InterruptedException) Thread.currentThread().interrupt();return null; }
    }
    @Override public boolean vendorReady(URI uri) {
        JsonNode node=request(uri,"/GetVersion",true);
        if(node!=null && node.has("GetVersionResult")) node=node.get("GetVersionResult");
        return node!=null && node.isTextual() && node.asText().matches("\\d+(\\.\\d+){1,4}");
    }
    @Override public boolean adapterReady(URI uri) {
        JsonNode health=request(uri,"/health",false);
        if(health==null || !health.path("service").asText().equals("pcr-sdk-bridge")) return false;
        JsonNode capabilities=request(uri,"/v1/capabilities",false);
        return capabilities!=null && capabilities.path("bridgeAvailable").asBoolean() && capabilities.path("sdkReady").asBoolean();
    }
    @Override public void pause() {
        try { Thread.sleep(250); } catch(InterruptedException error) { Thread.currentThread().interrupt();throw new IllegalStateException("Startup interrupted",error); }
    }
    @Override public void stop(ProcessHandle process) {
        if(process==null || !process.isAlive()) return;
        process.destroy();
        try { process.onExit().get(5,TimeUnit.SECONDS); }
        catch(InterruptedException error) { Thread.currentThread().interrupt(); }
        catch(Exception error) {
            if(process.isAlive()) {
                process.destroyForcibly();
                try { process.onExit().get(5,TimeUnit.SECONDS); }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                catch(Exception ignored) { /* may still be releasing handles; never kill unrelated descendants */ }
            }
        }
    }
}
