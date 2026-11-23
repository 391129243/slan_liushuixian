package com.pcr.dataserver;

import com.fasterxml.jackson.databind.JsonNode;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pcr/rules")
public class RuleVersionController {
    private final RuleVersionService service;
    public RuleVersionController(RuleVersionService service) { this.service=service; }
    @GetMapping public List<Map<String,Object>> list(@RequestParam(required=false)String bundleId) { return service.list(bundleId); }
    @GetMapping("/draft") public JsonNode draft(@RequestParam(required=false)String bundleId,@RequestParam(required=false)String versionId) { return service.draft(bundleId,versionId); }
    @GetMapping("/{id}") public Map<String,Object> get(@PathVariable String id) { return service.get(id); }
    @PostMapping public Map<String,Object> create(@RequestBody RuleVersionService.Create request,Principal principal) { return service.create(request,principal.getName()); }
}
