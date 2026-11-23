package com.pcr.dataserver;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pcr")
public class PcrAnalysisController {
    private final PcrAnalysisGateway gateway;
    private final PcrAnalysisService analyses;
    public PcrAnalysisController(PcrAnalysisGateway gateway,PcrAnalysisService analyses) { this.gateway=gateway;this.analyses=analyses; }
    public record ReadRequest(@NotBlank @Size(max=36) String fileId,
        @Size(max=36) String reportId,@Size(max=64) String parentAnalysisId,@Size(max=36) String bundleId) { }
    public record Parameters(boolean autoBaseline, @Min(1) @Max(19) int baselineStart,
        @Min(2) @Max(20) int baselineEnd, @DecimalMin(value="0",inclusive=false) double threshold,
        String scope,PcrAnalysisGateway.TargetSettings defaults,
        @Size(max=128) java.util.List<PcrAnalysisGateway.ProjectOverride> projects,
        @Size(max=1024) java.util.List<PcrAnalysisGateway.TargetOverride> targets) {
        @AssertTrue public boolean isValidRange() { return baselineStart < baselineEnd && Double.isFinite(threshold); }
    }
    public record ReanalyzeRequest(@NotBlank @Size(max=64) String experimentId,@NotNull @Valid Parameters parameters,
        @Pattern(regexp="[A-Za-z0-9_.:-]{1,64}") String requestId,@Size(max=36) String ruleVersionId) { }
    public record SaveRequest(@NotBlank String analysisId,
        @NotBlank @Size(max=255) @Pattern(regexp="[^/\\\\:\\p{Cntrl}]+\\.[pP][cC][rR]") String fileName) { }
    @GetMapping("/capabilities") public JsonNode capabilities() { return gateway.capabilities(); }
    @PostMapping("/read") public JsonNode read(@Valid @RequestBody ReadRequest request,Principal principal) {
        return analyses.read(request.fileId(),request.reportId(),request.parentAnalysisId(),request.bundleId(),principal.getName());
    }
    @PostMapping("/reanalyze") public JsonNode reanalyze(@Valid @RequestBody ReanalyzeRequest request,Principal principal) {
        var p = request.parameters();
        return analyses.reanalyze(request.experimentId(),
            new PcrAnalysisGateway.AnalysisParameters(p.autoBaseline(),p.baselineStart(),p.baselineEnd(),p.threshold(),p.scope(),p.defaults(),p.projects(),p.targets()),request.requestId(),principal.getName(),request.ruleVersionId());
    }
    @PostMapping("/save") public Map<String,Object> save(@Valid @RequestBody SaveRequest request,Principal principal) {
        return analyses.save(request.analysisId(),request.fileName(),principal.getName());
    }
    @GetMapping("/analyses") public Map<String,Object> list(@RequestParam(required=false) String reportId,
        @RequestParam(required=false) String inputFileId,@RequestParam(defaultValue="0") @Min(0) int offset,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int limit) {
        return analyses.list(reportId,inputFileId,offset,limit);
    }
    @GetMapping("/analyses/{id}") public Map<String,Object> detail(@PathVariable String id) { return analyses.detail(id); }
    @GetMapping("/analyses/{id}/result") public JsonNode result(@PathVariable String id) { return analyses.result(id); }
}
