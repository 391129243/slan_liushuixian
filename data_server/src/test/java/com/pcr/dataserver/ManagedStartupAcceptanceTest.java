package com.pcr.dataserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;

/** Real Windows process test against a purpose-built fixture executable, NEVER the vendor package. */
@EnabledIfEnvironmentVariable(named="RUN_MANAGED_ACCEPTANCE", matches="true")
class ManagedStartupAcceptanceTest {
    @TempDir Path temp;
    int port() throws Exception { try(var socket=new java.net.ServerSocket(0,0,InetAddress.getLoopbackAddress())) { return socket.getLocalPort(); } }
    @Test void hiddenLaunchReadinessWorkingDirectoryAndShutdown() throws Exception {
        var input=Path.of("target","managed-fixture").toAbsolutePath();
        Path directory=Files.createDirectories(temp.resolve("vendor with spaces"));
        try(var walk=Files.walk(input)) {
            for(Path file:walk.toList()) {
                Path output=directory.resolve(input.relativize(file));
                if(Files.isDirectory(file)) Files.createDirectories(output); else Files.copy(file,output);
            }
        }
        int vendorPort=port(),adapterPort=port();
        URI vendorUrl=URI.create("http://127.0.0.1:"+vendorPort+"/SlanServerService");
        URI adapterUrl=URI.create("http://127.0.0.1:"+adapterPort);
        Files.writeString(directory.resolve("listen-url.txt"),"http://127.0.0.1:"+vendorPort);
        var settings=new ManagedProcessSettings(true,directory,vendorUrl,adapterUrl,
            Path.of("target","csharp-bridge","PcrSdkBridge.dll").toAbsolutePath(),Duration.ofSeconds(20),true);
        var server=new ServerSettings(temp.resolve("files"),"not-used-in-test",90,1024,1024,true);
        var mapper=new ObjectMapper();var ops=new WindowsProcessOps(mapper);
        var runtime=new ManagedSlanLifecycle(settings,server,ops);
        ProcessHandle owned=null;
        try {
            runtime.start();assertThat(runtime.isRunning()).isTrue();
            var response=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+vendorPort+"/fixture")).GET().build(),HttpResponse.BodyHandlers.ofString());
            var fixture=mapper.readTree(response.body());assertThat(fixture.path("fixture").asBoolean()).isTrue();
            assertThat(Path.of(fixture.path("directory").asText())).isEqualTo(directory);
            owned=ops.findVendor(directory.resolve("SLAN.WCFServer.exe")).orElseThrow();
            assertThat(ops.adapterReady(adapterUrl)).isTrue();
            // Starting twice is idempotent.
            runtime.start();assertThat(ops.findVendor(directory.resolve("SLAN.WCFServer.exe")).orElseThrow().pid()).isEqualTo(owned.pid());
        } finally { runtime.stop(); }
        for(int i=0;i<40&&(owned.isAlive()||ops.portOpen(adapterUrl));i++) Thread.sleep(100);
        assertThat(owned.isAlive()).isFalse();assertThat(ops.portOpen(adapterUrl)).isFalse();
    }
}
