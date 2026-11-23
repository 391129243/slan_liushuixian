package com.pcr.dataserver;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final DeviceService devices;
    private final ReportService reports;
    private final UploadService uploads;
    private final ServerSettings settings;
    public AdminController(DeviceService devices,ReportService reports,UploadService uploads,ServerSettings settings) {
        this.devices=devices;this.reports=reports;this.uploads=uploads;this.settings=settings;
    }
    @GetMapping("/summary") public Map<String,Object> summary() { return reports.summary(); }
    @PostMapping("/devices") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,String> create(@Valid @RequestBody DeviceService.CreateDevice input) { return devices.create(input); }
    @GetMapping("/devices")
    public List<Map<String,Object>> devices(@RequestParam(defaultValue="0") @Min(0) int offset,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int limit) { return devices.list(offset,limit); }
    public record Enabled(@NotNull Boolean enabled) { }
    @PatchMapping("/devices/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable String id,@Valid @RequestBody Enabled input) { devices.enabled(id,input.enabled()); }
    @PostMapping("/devices/{id}/key") public Map<String,String> rotate(@PathVariable String id) { return devices.rotate(id); }
    @GetMapping("/reports")
    public Map<String,Object> list(@RequestParam(required=false) String serial,
        @RequestParam(defaultValue="0") @Min(0) int offset,@RequestParam(defaultValue="20") @Min(1) @Max(100) int limit) {
        return reports.list(serial,offset,limit);
    }
    @GetMapping("/reports/{id}") public Map<String,Object> detail(@PathVariable String id) throws IOException { return reports.detail(id); }
    @GetMapping("/reports/{id}/source") public ResponseEntity<byte[]> source(@PathVariable String id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=experiment.json")
            .body(reports.source(id).getBytes(StandardCharsets.UTF_8));
    }
    @GetMapping("/files") public List<Map<String,Object>> files(@RequestParam(defaultValue="0") @Min(0) int offset,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int limit) { return reports.files(offset,limit); }
    @PostMapping(value="/files",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadService.UploadReply upload(@RequestPart("file") MultipartFile file,Principal principal) throws IOException {
        try(InputStream stream=file.getInputStream()) {
            return uploads.file(file.getOriginalFilename(),UploadController.readBounded(stream,settings.maxFileBytes()),principal.getName(),"ADMIN_UPLOAD");
        }
    }
    @GetMapping("/files/{id}/download") public ResponseEntity<byte[]> download(@PathVariable String id) throws IOException {
        var file=reports.download(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(file.name(),StandardCharsets.UTF_8).build().toString())
            .body(file.content());
    }
    @PutMapping("/reports/{report}/file/{file}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attach(@PathVariable String report,@PathVariable String file) { reports.attach(report,file); }
}
