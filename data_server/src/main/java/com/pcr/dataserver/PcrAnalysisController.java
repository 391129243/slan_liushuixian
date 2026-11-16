package com.pcr.dataserver;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pcr")
public class PcrAnalysisController {
    private final PcrAnalysisGateway gateway;
    private final ReportService reports;
    private final UploadService uploads;
    public PcrAnalysisController(PcrAnalysisGateway gateway, ReportService reports, UploadService uploads) { this.gateway = gateway; this.reports = reports; this.uploads = uploads; }
    public record ReadRequest(@NotBlank String fileId) { }
    public record Parameters(boolean autoBaseline, @Min(1) @Max(19) int baselineStart,
        @Min(2) @Max(20) int baselineEnd, @DecimalMin("10") @DecimalMax("3000") double threshold) {
        @AssertTrue public boolean isValidRange() { return baselineStart < baselineEnd && Double.isFinite(threshold); }
    }
    public record ReanalyzeRequest(@NotBlank String experimentId, @NotNull @Valid Parameters parameters) { }
    public record SaveRequest(@NotBlank String analysisId,
        @NotBlank @Size(max=255) @Pattern(regexp="[^/\\\\:\\p{Cntrl}]+\\.[pP][cC][rR]") String fileName) { }
    @GetMapping("/capabilities") public JsonNode capabilities() { return gateway.capabilities(); }
    @PostMapping("/read") public JsonNode read(@Valid @RequestBody ReadRequest request) { return gateway.read(reports.sourceFile(request.fileId())); }
    @PostMapping("/reanalyze") public JsonNode reanalyze(@Valid @RequestBody ReanalyzeRequest request) {
        var p = request.parameters();
        return gateway.reanalyze(new PcrAnalysisGateway.Reanalyze(request.experimentId(),
            new PcrAnalysisGateway.AnalysisParameters(p.autoBaseline(), p.baselineStart(), p.baselineEnd(), p.threshold())));
    }
    @PostMapping("/save") public java.util.Map<String,Object> save(@Valid @RequestBody SaveRequest request) throws java.io.IOException {
        JsonNode saved = gateway.save(new PcrAnalysisGateway.Save(request.analysisId(), request.fileName()));
        byte[] bytes = gateway.download(saved.path("outputId").asText());
        if (!Digests.hash("SHA-256",bytes).equals(saved.path("sha256").asText()))
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY,"Saved PCR output checksum mismatch");
        var stored = uploads.file(request.fileName(),bytes);
        return java.util.Map.of("Result","1","FileID",stored.FileID(),"analysisId",request.analysisId(),"mock",false);
    }
}
