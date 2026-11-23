package com.pcr.dataserver;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ResultBundleController {
    private final ResultBundleService bundles;
    public ResultBundleController(ResultBundleService bundles) { this.bundles=bundles; }
    @PostMapping(value="/api/device/bundles",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultBundleService.Reply device(@RequestPart("file") MultipartFile file,@RequestParam String experiment,
        @RequestParam String manifest,@RequestPart(value="attachments",required=false) List<MultipartFile> attachments,Principal principal) throws IOException {
        return bundles.upload(experiment,manifest,file,attachments,principal.getName(),null);
    }
    @PostMapping(value="/api/admin/bundles",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultBundleService.Reply admin(@RequestPart("file") MultipartFile file,@RequestParam String experiment,
        @RequestParam String manifest,@RequestPart(value="attachments",required=false) List<MultipartFile> attachments,Principal principal) throws IOException {
        return bundles.upload(experiment,manifest,file,attachments,null,principal.getName());
    }
    @GetMapping("/api/admin/reports/{id}/bundles") public List<Map<String,Object>> forReport(@PathVariable String id) { return bundles.forReport(id); }
    @GetMapping("/api/admin/bundles/{id}") public Map<String,Object> detail(@PathVariable String id) { return bundles.get(id); }
    @GetMapping("/api/admin/bundles/{id}/download") public ResponseEntity<byte[]> download(@PathVariable String id) throws IOException { return reply(bundles.download(id),MediaType.parseMediaType("application/zip")); }
    @GetMapping("/api/admin/bundles/{id}/attachments/{name}") public ResponseEntity<byte[]> attachment(@PathVariable String id,@PathVariable String name) throws IOException { return reply(bundles.attachment(id,name),MediaType.APPLICATION_OCTET_STREAM); }
    @GetMapping("/api/admin/pcr/analyses/{id}/bundle") public ResponseEntity<byte[]> analysis(@PathVariable String id) throws IOException { return reply(bundles.downloadAnalysis(id),MediaType.parseMediaType("application/zip")); }
    private ResponseEntity<byte[]> reply(ReportService.Download file,MediaType type) {
        return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.name(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(file.content());
    }
}
