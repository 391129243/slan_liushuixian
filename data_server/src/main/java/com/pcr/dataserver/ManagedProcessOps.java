package com.pcr.dataserver;

import java.nio.file.Path;
import java.net.URI;
import java.util.*;

/** OS boundary kept separate so lifecycle tests never start a vendor executable. */
public interface ManagedProcessOps {
    Optional<ProcessHandle> findVendor(Path executable);
    boolean portOpen(URI url);
    ProcessHandle launch(Path executable, List<String> arguments, Path directory, Path logPrefix, Map<String,String> environment);
    boolean vendorReady(URI url);
    boolean adapterReady(URI url);
    void pause();
    void stop(ProcessHandle process);
}
