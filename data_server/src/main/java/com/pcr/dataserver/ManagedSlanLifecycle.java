package com.pcr.dataserver;

import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.slf4j.*;
import org.springframework.context.SmartLifecycle;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(ManagedProcessSettings.class)
public class ManagedSlanLifecycle implements SmartLifecycle {
    private static final Logger log=LoggerFactory.getLogger(ManagedSlanLifecycle.class);
    private final ManagedProcessSettings settings;
    private final ServerSettings server;
    private final ManagedProcessOps ops;
    private ProcessHandle vendor,adapter;
    private boolean ownVendor,ownAdapter;
    private volatile boolean running;
    public ManagedSlanLifecycle(ManagedProcessSettings settings,ServerSettings server,ManagedProcessOps ops) {
        this.settings=settings;this.server=server;this.ops=ops;
    }
    private void local(URI uri) {
        try {
            if(!Set.of("http","https").contains(uri.getScheme()) || !InetAddress.getByName(uri.getHost()).isLoopbackAddress())
                throw new IllegalStateException("Managed service URLs must be loopback HTTP addresses");
        } catch(UnknownHostException error) { throw new IllegalStateException("Invalid managed service host",error); }
    }
    @Override public synchronized void start() {
        if(!settings.enabled() || running) return;
        local(settings.vendorUrl());local(settings.adapterUrl());
        Path directory=settings.vendorDirectory().toAbsolutePath().normalize();
        Path exe=directory.resolve("SLAN.WCFServer.exe");
        Path dll=settings.adapterDll().toAbsolutePath().normalize();
        if(!Files.isRegularFile(exe)) throw new IllegalStateException("SLAN.WCFServer.exe not found: "+exe+"; set DATASERVER_SLAN_DIR");
        if(!Files.isRegularFile(dll)) throw new IllegalStateException("C# adapter is not built: "+dll+"; run dotnet build csharp/PcrSdkBridge -o target/csharp-bridge");
        Path logs=Path.of("runtime","managed").toAbsolutePath();
        try {
            Optional<ProcessHandle> existing=ops.findVendor(exe);
            if(existing.isPresent()) { vendor=existing.get();log.info("Reusing SLAN.WCFServer PID {}",vendor.pid()); }
            else {
                if(ops.portOpen(settings.vendorUrl())) throw new IllegalStateException("SLAN HTTP port is occupied by an unverified process; refusing a duplicate launch");
                vendor=ops.launch(exe,List.of(),directory,logs.resolve("vendor"),Map.of());ownVendor=true;
                log.info("Started SLAN.WCFServer PID {} in {}",vendor.pid(),directory);
            }
            await(() -> vendor.isAlive() && ops.vendorReady(settings.vendorUrl()),vendor,"SLAN GetVersion (check executable logs, HTTP permissions and configured URL)");
            if(ops.portOpen(settings.adapterUrl())) {
                if(!ops.adapterReady(settings.adapterUrl())) throw new IllegalStateException("Adapter port is occupied or existing adapter has SDK calls disabled; refusing to replace it");
                log.info("Reusing ready C# offline adapter at {}",settings.adapterUrl());
            } else {
                Path input=server.storage().toAbsolutePath().normalize();Files.createDirectories(input);
                var env=new HashMap<String,String>();env.put("PCR_VENDOR_ENABLED","true");env.put("PCR_VENDOR_URL",settings.vendorUrl().toString());
                env.put("PCR_BRIDGE_URL",settings.adapterUrl().toString());env.put("PCR_INPUT_ROOT",input.toString());
                env.put("PCR_WORK_ROOT",Path.of("runtime","slan-offline").toAbsolutePath().normalize().toString());
                adapter=ops.launch(Path.of("dotnet.exe"),List.of(dll.toString()),dll.getParent(),logs.resolve("adapter"),env);ownAdapter=true;
                await(() -> adapter.isAlive() && ops.adapterReady(settings.adapterUrl()),adapter,"C# offline adapter");
                log.info("Started C# offline adapter PID {}",adapter.pid());
            }
            running=true;
        } catch(Exception error) {
            cleanup();
            throw error instanceof RuntimeException runtime ? runtime : new IllegalStateException("Managed startup failed",error);
        }
    }
    private void await(BooleanSupplier ready,ProcessHandle process,String description) {
        long started=System.nanoTime(), timeout=settings.startupTimeout().toNanos();
        while(!ready.getAsBoolean()) {
            if(!process.isAlive()) throw new IllegalStateException(description+" exited; see runtime/managed logs");
            if(System.nanoTime()-started>=timeout) throw new IllegalStateException(description+" did not become ready within "+settings.startupTimeout());
            ops.pause();
        }
    }
    private void cleanup() {
        if(ownAdapter) ops.stop(adapter);
        if(ownVendor) ops.stop(vendor);
        ownAdapter=false;ownVendor=false;adapter=null;vendor=null;running=false;
    }
    @Override public synchronized void stop() {
        if(settings.stopOwnedProcesses()) cleanup(); else running=false;
    }
    @Override public boolean isRunning() { return running; }
    @Override public int getPhase() { return -100; }
}
