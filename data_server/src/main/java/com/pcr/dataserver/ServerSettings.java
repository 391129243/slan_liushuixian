package com.pcr.dataserver;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("dataserver")
public record ServerSettings(Path storage, String adminPassword, int onlineSeconds,
                             int maxFileBytes, int maxJsonBytes, boolean legacyUploadEnabled) { }
