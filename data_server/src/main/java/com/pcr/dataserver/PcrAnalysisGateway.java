package com.pcr.dataserver;

import com.fasterxml.jackson.databind.JsonNode;

/** Boundary between Java business services and the separate C# SDK host. */
public interface PcrAnalysisGateway {
    record SourceFile(String fileId, String fileName, String sourcePath) { }
    record AnalysisParameters(boolean autoBaseline, int baselineStart, int baselineEnd, double threshold) { }
    record Reanalyze(String experimentId, AnalysisParameters parameters) { }
    record Save(String analysisId, String fileName) { }
    JsonNode capabilities();
    JsonNode read(SourceFile source);
    JsonNode reanalyze(Reanalyze request);
    JsonNode save(Save request);
    byte[] download(String outputId);
}
