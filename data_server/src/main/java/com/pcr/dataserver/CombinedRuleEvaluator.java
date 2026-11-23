package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Explicit user-configured Boolean combinations; never infer a diagnostic policy from filenames. */
@Component
public class CombinedRuleEvaluator {
    private final ObjectMapper mapper;
    public CombinedRuleEvaluator(ObjectMapper mapper) { this.mapper=mapper; }
    public ObjectNode evaluate(JsonNode individual,JsonNode policies) {
        var result=mapper.createObjectNode().put("engineVersion","ct-peak-combination-v1").put("status","NOT_CONFIGURED");
        var rows=result.putArray("rows");
        if(policies==null || !policies.isArray() || policies.isEmpty())return result;
        Set<String> wells=new LinkedHashSet<>();for(var row:individual.path("rows"))wells.add(row.path("well").asText());
        for(String well:wells)for(var policy:policies) {
            JsonNode ct=find(individual,well,policy.path("ct")),peak=find(individual,well,policy.path("peak"));
            String a=ct.path("status").asText("UNDETERMINED"),b=peak.path("status").asText("UNDETERMINED");
            String status;
            if(a.equals("UNDETERMINED") || b.equals("UNDETERMINED"))status="UNDETERMINED";
            else if(policy.path("mode").asText().equals("ALL"))status=a.equals("NEGATIVE")||b.equals("NEGATIVE")?"NEGATIVE":a.equals("POSITIVE")&&b.equals("POSITIVE")?"POSITIVE":"SUSPICIOUS";
            else status=a.equals("POSITIVE")||b.equals("POSITIVE")?"POSITIVE":a.equals("SUSPICIOUS")||b.equals("SUSPICIOUS")?"SUSPICIOUS":"NEGATIVE";
            var row=rows.addObject().put("well",well).put("name",policy.path("name").asText()).put("mode",policy.path("mode").asText())
                .put("ctStatus",a).put("peakStatus",b).put("status",status).put("conclusion",switch(status){case "POSITIVE"->"阳性";case "NEGATIVE"->"阴性";case "SUSPICIOUS"->"可疑样本";default->"未判定";});
            row.set("ct",policy.path("ct").deepCopy());row.set("peak",policy.path("peak").deepCopy());
            if(!ct.path("sampleWellNames").isMissingNode())row.set("sampleWellNames",ct.path("sampleWellNames").deepCopy());
        }
        return result.put("status",rows.isEmpty()?"MISSING_DATA":rows.findValuesAsText("status").contains("UNDETERMINED")?"PARTIAL":"EVALUATED");
    }
    private JsonNode find(JsonNode evaluation,String well,JsonNode reference) {
        for(var row:evaluation.path("rows"))if(row.path("well").asText().equals(well)&&row.path("fileName").asText().equals(reference.path("fileName").asText())&&row.path("ruleIndex").asInt(-1)==reference.path("ruleIndex").asInt(-2))return row;
        return mapper.missingNode();
    }
}
