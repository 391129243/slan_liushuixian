package com.pcr.dataserver;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Boundary between Java business services and the separate C# SDK host. */
public interface PcrAnalysisGateway {
    record RuleAttachment(String fileName,String sourcePath,String sha256,String role) { }
    record SourceFile(String fileId,String fileName,String sourcePath,List<RuleAttachment> attachments) {
        public SourceFile(String fileId,String fileName,String sourcePath) { this(fileId,fileName,sourcePath,List.of()); }
    }
    record TargetSettings(Integer baselineStart,Integer baselineEnd,Double threshold,Boolean autoThreshold,
        Boolean qualitativeAnalysis,Integer optimizationMode,Integer digitalFilter,Double peakHeightThreshold,
        Double minTemperature,Double maxTemperature,Integer meltPeakAlgorithm,Integer peakType) { }
    record ProjectOverride(int projectIndex,Integer ampAlgorithm) { }
    record TargetOverride(int projectIndex,int targetIndex,TargetSettings settings) { }
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    record AnalysisParameters(boolean autoBaseline, int baselineStart, int baselineEnd, double threshold,
        String scope,TargetSettings defaults,List<ProjectOverride> projects,List<TargetOverride> targets) {
        public AnalysisParameters(boolean autoBaseline,int baselineStart,int baselineEnd,double threshold) {
            this(autoBaseline,baselineStart,baselineEnd,threshold,null,null,null,null);
        }
    }
    record Reanalyze(String experimentId, AnalysisParameters parameters,List<RuleAttachment> attachments) {
        public Reanalyze(String experimentId,AnalysisParameters parameters) { this(experimentId,parameters,null); }
    }
    record Save(String analysisId, String fileName) { }
    JsonNode capabilities();
    JsonNode read(SourceFile source);
    JsonNode reanalyze(Reanalyze request);
    JsonNode save(Save request);
    byte[] download(String outputId);
}
