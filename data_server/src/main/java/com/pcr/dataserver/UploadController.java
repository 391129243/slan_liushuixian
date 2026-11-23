package com.pcr.dataserver;

import jakarta.servlet.http.HttpServletRequest;
import java.io.*;
import java.security.Principal;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UploadController {
    private final ExperimentParser parser;
    private final DeviceService devices;
    private final UploadService uploads;
    private final ServerSettings settings;
    private final ResultBundleService bundles;
    public UploadController(ExperimentParser parser,DeviceService devices,UploadService uploads,ServerSettings settings,ResultBundleService bundles) {
        this.parser=parser; this.devices=devices; this.uploads=uploads; this.settings=settings;
        this.bundles=bundles;
    }
    @PostMapping(value="/upload",consumes=MediaType.APPLICATION_JSON_VALUE)
    public UploadService.UploadReply legacyResult(HttpServletRequest request) throws IOException {
        byte[] bytes=readBounded(request.getInputStream(),settings.maxJsonBytes());
        var experiment=parser.parse(bytes);
        devices.registerLegacy(experiment.serial());
        return uploads.result(experiment,bytes,null);
    }
    @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object legacyFile(@RequestPart("file") MultipartFile file,
        @RequestParam(required=false) String experiment,@RequestParam(required=false) String manifest,
        @RequestPart(value="attachments",required=false) List<MultipartFile> attachments) throws IOException {
        if(experiment!=null || manifest!=null || attachments!=null) return bundles.upload(experiment,manifest,file,attachments,null,null);
        try(InputStream stream=file.getInputStream()) {
            return uploads.file(file.getOriginalFilename(),readBounded(stream,settings.maxFileBytes()));
        }
    }
    @PostMapping(value="/api/device/reports",consumes=MediaType.APPLICATION_JSON_VALUE)
    public UploadService.UploadReply authenticatedResult(HttpServletRequest request,Principal principal) throws IOException {
        byte[] bytes=readBounded(request.getInputStream(),settings.maxJsonBytes());
        return uploads.result(parser.parse(bytes),bytes,principal.getName());
    }
    static byte[] readBounded(InputStream stream,int max) throws IOException {
        byte[] bytes=stream.readNBytes(max+1);
        if(bytes.length>max) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Upload too large");
        if(bytes.length==0) throw ApiException.bad("Empty upload");
        return bytes;
    }
}
