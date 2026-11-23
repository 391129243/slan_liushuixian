package com.pcr.dataserver;

import java.nio.file.Path;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dataserver.managed")
public record ManagedProcessSettings(boolean enabled, Path vendorDirectory, URI vendorUrl, URI adapterUrl,
    Path adapterDll, Duration startupTimeout, boolean stopOwnedProcesses) { }
