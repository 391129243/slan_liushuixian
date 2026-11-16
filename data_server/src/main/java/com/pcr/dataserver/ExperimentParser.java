package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.io.IOException;
import java.util.*;
import org.springframework.stereotype.Component;

/** Gson serializes SDK private capitalized fields, not the Java getter names. */
@Component
public class ExperimentParser {
    private final ObjectMapper mapper;
    private final ServerSettings settings;
    public ExperimentParser(ObjectMapper mapper,ServerSettings settings) { this.mapper=mapper; this.settings=settings; }
    public record Parsed(ObjectNode experiment,String serial,String name,String start,String end,
                         String fingerprint,int wells,int targets) { }
    public Parsed parse(byte[] raw) {
        if(raw.length==0 || raw.length>settings.maxJsonBytes()) throw ApiException.bad("Invalid experiment JSON size");
        try {
            JsonNode tree=mapper.readTree(raw);
            if(!(tree instanceof ObjectNode object)) throw ApiException.bad("Experiment JSON object required");
            String serial=required(object,"SerialNo",96),name=required(object,"Name",255);
            String start=optional(object,"StartTime",128),end=optional(object,"EndTime",128);
            JsonNode wells=object.get("WellResult");
            if(wells==null || !wells.isArray()) throw ApiException.bad("WellResult array required");
            int count=0;
            for(JsonNode well:wells) {
                if(!well.isObject()) throw ApiException.bad("WellResult entries must be objects");
                JsonNode targets=well.get("TargetResults");
                if(targets!=null && !targets.isNull()) {
                    if(!targets.isArray()) throw ApiException.bad("TargetResults must be an array");
                    for(JsonNode target:targets) {
                        if(!target.isObject()) throw ApiException.bad("TargetResults entries must be objects");
                        JsonNode ct=target.get("Ct");
                        if(ct!=null && !ct.isNull() && !ct.isNumber()) throw ApiException.bad("Ct must be numeric");
                    }
                    count+=targets.size();
                }
            }
            String fingerprint=Digests.sha256(mapper.writeValueAsString(canonical(object)));
            return new Parsed(object,serial,name,start,end,fingerprint,wells.size(),count);
        } catch(IOException e) { throw ApiException.bad("Invalid Experiment JSON"); }
    }
    private String required(ObjectNode node,String name,int max) {
        String value=optional(node,name,max);
        if(value==null || value.isBlank()) throw ApiException.bad(name+" is required");
        return value;
    }
    private String optional(ObjectNode node,String name,int max) {
        JsonNode value=node.get(name);
        if(value==null || value.isNull()) return null;
        if(!value.isTextual() || value.textValue().length()>max || value.textValue().chars().anyMatch(c -> c<32))
            throw ApiException.bad("Invalid "+name);
        return value.textValue();
    }
    private JsonNode canonical(JsonNode node) {
        if(node.isObject()) {
            ObjectNode out=mapper.createObjectNode(); var names=new ArrayList<String>();
            node.fieldNames().forEachRemaining(names::add); Collections.sort(names);
            names.forEach(name -> out.set(name,canonical(node.get(name)))); return out;
        }
        if(node.isArray()) { ArrayNode out=mapper.createArrayNode(); node.forEach(n -> out.add(canonical(n))); return out; }
        if(node.isNumber()) return DecimalNode.valueOf(node.decimalValue().stripTrailingZeros());
        return node;
    }
}
