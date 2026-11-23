package com.pcr.dataserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;
import java.nio.file.*;
import java.net.URI;
import java.time.Duration;
import java.util.*;

class ManagedSlanLifecycleTest {
    @TempDir Path temp;
    private ManagedProcessSettings settings(Duration timeout) throws Exception {
        Path vendor=Files.createDirectories(temp.resolve("vendor with spaces"));
        Path exe=vendor.resolve("SLAN.WCFServer.exe");if(!Files.exists(exe)) Files.writeString(exe,"test placeholder, never executed");
        Path dll=temp.resolve("adapter.dll");if(!Files.exists(dll)) Files.writeString(dll,"test placeholder");
        return new ManagedProcessSettings(true,vendor,URI.create("http://127.0.0.1:8186/SlanServerService"),
            URI.create("http://127.0.0.1:5099"),dll,timeout,true);
    }
    private ServerSettings server() { return new ServerSettings(temp.resolve("files"),"test-password-long",90,1024,1024,true); }
    private ProcessHandle process(long pid) { var process=mock(ProcessHandle.class);when(process.pid()).thenReturn(pid);when(process.isAlive()).thenReturn(true);return process; }
    @Test void launchesInVendorDirectoryAndStopsOnlyItsOwnedProcesses() throws Exception {
        var config=settings(Duration.ofSeconds(1));var ops=mock(ManagedProcessOps.class);
        var vendor=process(1);var adapter=process(2);
        when(ops.findVendor(any())).thenReturn(Optional.empty());
        when(ops.launch(any(),anyList(),any(),any(),anyMap())).thenReturn(vendor,adapter);
        when(ops.vendorReady(any())).thenReturn(true);when(ops.adapterReady(any())).thenReturn(true);
        var runtime=new ManagedSlanLifecycle(config,server(),ops);runtime.start();assertThat(runtime.isRunning()).isTrue();
        verify(ops).launch(eq(config.vendorDirectory().resolve("SLAN.WCFServer.exe")),eq(List.of()),eq(config.vendorDirectory()),any(),eq(Map.of()));
        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String,String>> environment=ArgumentCaptor.forClass(Map.class);
        verify(ops).launch(eq(Path.of("dotnet.exe")),eq(List.of(config.adapterDll().toString())),eq(config.adapterDll().getParent()),any(),environment.capture());
        assertThat(environment.getValue()).containsEntry("PCR_VENDOR_ENABLED","true").containsEntry("PCR_INPUT_ROOT",server().storage().toString());
        runtime.stop();verify(ops).stop(adapter);verify(ops).stop(vendor);assertThat(runtime.isRunning()).isFalse();
    }
    @Test void reusesExistingVendorWithoutTerminatingItOnExit() throws Exception {
        var config=settings(Duration.ofSeconds(1));var ops=mock(ManagedProcessOps.class);var vendor=process(1);
        when(ops.findVendor(any())).thenReturn(Optional.of(vendor));when(ops.vendorReady(any())).thenReturn(true);
        when(ops.portOpen(config.adapterUrl())).thenReturn(true);when(ops.adapterReady(any())).thenReturn(true);
        var runtime=new ManagedSlanLifecycle(config,server(),ops);runtime.start();runtime.stop();
        verify(ops,never()).launch(any(),anyList(),any(),any(),anyMap());verify(ops,never()).stop(any());
    }
    @Test void refusesUnverifiedOccupiedPortWithoutLaunchingAnything() throws Exception {
        var config=settings(Duration.ofSeconds(1));var ops=mock(ManagedProcessOps.class);
        when(ops.findVendor(any())).thenReturn(Optional.empty());when(ops.portOpen(config.vendorUrl())).thenReturn(true);
        var runtime=new ManagedSlanLifecycle(config,server(),ops);
        assertThatThrownBy(runtime::start).hasMessageContaining("unverified process");
        verify(ops,never()).launch(any(),anyList(),any(),any(),anyMap());
    }
    @Test void readinessTimeoutCleansUpOnlyNewlyLaunchedVendor() throws Exception {
        var config=settings(Duration.ZERO);var ops=mock(ManagedProcessOps.class);var vendor=process(1);
        when(ops.findVendor(any())).thenReturn(Optional.empty());when(ops.launch(any(),anyList(),any(),any(),anyMap())).thenReturn(vendor);
        var runtime=new ManagedSlanLifecycle(config,server(),ops);
        assertThatThrownBy(runtime::start).hasMessageContaining("did not become ready");verify(ops).stop(vendor);
        assertThat(runtime.isRunning()).isFalse();
    }
    @Test void adapterStartupFailureDoesNotKillReusedVendor() throws Exception {
        var config=settings(Duration.ZERO);var ops=mock(ManagedProcessOps.class);var vendor=process(1);var adapter=process(2);
        when(ops.findVendor(any())).thenReturn(Optional.of(vendor));when(ops.vendorReady(any())).thenReturn(true);
        when(ops.launch(any(),anyList(),any(),any(),anyMap())).thenReturn(adapter);
        var runtime=new ManagedSlanLifecycle(config,server(),ops);
        assertThatThrownBy(runtime::start).hasMessageContaining("did not become ready");
        verify(ops).stop(adapter);verify(ops,never()).stop(vendor);
    }
}
