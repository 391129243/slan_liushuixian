package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Application rules ported from SLANManager.judgeCTFile/judgePEAKFile, independently of SDK parameters. */
@Component
public class AndroidRuleEvaluator {
    public static final String VERSION="android-slan-rules-v1";
    public record Source(String fileName,String role,String sha256,byte[] bytes) { }
    private record Rule(Source source,int index,String channel,String category,boolean ct,double center,double low,double high,double suspicious,double rm) { }
    private final ObjectMapper mapper;
    public AndroidRuleEvaluator(ObjectMapper mapper) { this.mapper=mapper; }

    public ObjectNode evaluate(JsonNode experiment,List<Source> sources) {
        var result=mapper.createObjectNode().put("engineVersion",VERSION).put("status","MISSING_RULES").put("applicationRulesApplied",false);
        var provenance=result.putArray("sources");var errors=result.putArray("errors");var rows=result.putArray("rows");
        var rules=new ArrayList<Rule>();
        for(var source:sources) {
            if(!Set.of("CT_RULE","PEAK_RULE").contains(source.role()))continue;
            provenance.addObject().put("fileName",source.fileName()).put("sha256",source.sha256()).put("role",source.role());
            try {
                if(!Digests.hash("SHA-256",source.bytes()).equals(source.sha256()))throw new IllegalArgumentException("Rule checksum mismatch");
                var parsed=mapper.readTree(source.bytes());
                if(parsed==null || !parsed.isArray() || parsed.isEmpty())throw new IllegalArgumentException("Expected a nonempty Android JSON rule array");
                var fileRules=new ArrayList<Rule>();int index=0;
                for(var group:parsed) {
                    String channel=text(group,"Channel");
                    if("CT_RULE".equals(source.role()))fileRules.add(ct(source,index++,channel,group));
                    else {
                        var items=group.path("PEAKSingleRuleEntityList");
                        if(!items.isArray() || items.isEmpty())throw new IllegalArgumentException("PEAKSingleRuleEntityList must be a nonempty array");
                        for(var item:items)fileRules.add(peak(source,index++,channel,item));
                    }
                    if(fileRules.size()+rules.size()>1024)throw new IllegalArgumentException("Too many application rules (maximum 1024)");
                }
                rules.addAll(fileRules);
            } catch(java.io.IOException | IllegalArgumentException error) {
                errors.addObject().put("fileName",source.fileName()).put("message",error.getMessage());
            }
        }
        if(provenance.isEmpty())return result;
        var wells=experiment.path("WellResult");
        if(!wells.isArray() || wells.isEmpty() || (long)wells.size()*rules.size()>100000) {
            errors.addObject().put("message","Missing wells or application rule result limit exceeded");
            return result.put("status","INVALID_DATA");
        }
        boolean incomplete=false;int evaluated=0;
        for(var well:wells)for(var rule:rules) {
            var row=rows.addObject().put("fileName",rule.source.fileName()).put("ruleIndex",rule.index)
                .put("type",rule.ct?"CT":"PEAK").put("well",well.path("ID").asText()).put("sampleName",well.path("SampleName").asText())
                .put("channel",rule.channel).put("category",rule.category).put("center",rule.center).put("lower",rule.low).put("upper",rule.high)
                .put("status","UNDETERMINED").put("conclusion","未判定");
            if(well.path("SampleWellNames").isArray())row.set("sampleWellNames",well.path("SampleWellNames").deepCopy());
            if(rule.ct)row.put("suspiciousUpper",rule.suspicious);else row.put("minimumRm",rule.rm);
            var matches=new ArrayList<JsonNode>();
            for(var target:well.path("TargetResults")) {
                // Android uses TargetResult.Name.toLowerCase().contains(rule.Channel.toLowerCase()).
                String name=target.path("Name").asText(target.path("Dye").asText());
                if(name.toLowerCase(Locale.ROOT).contains(rule.channel.toLowerCase(Locale.ROOT)))matches.add(target);
            }
            if(matches.size()!=1)row.put("reason",matches.isEmpty()?"MISSING_CHANNEL":"AMBIGUOUS_CHANNEL");
            else if(rule.ct)evaluateCt(row,rule,matches.getFirst());else evaluatePeak(row,rule,matches.getFirst());
            if("UNDETERMINED".equals(row.path("status").asText()))incomplete=true;else evaluated++;
        }
        result.put("applicationRulesApplied",evaluated>0);
        result.put("status",rules.isEmpty()?"INVALID_RULES":!errors.isEmpty() || incomplete?"PARTIAL":"EVALUATED");
        return result;
    }
    private Rule ct(Source source,int index,String channel,JsonNode node) {
        double center=number(node,"CTvalue"),low=center-offset(node,"CTvalueDown"),high=center+offset(node,"CTvalueUP"),sus=high+offset(node,"CTvalueUP_Suspicious");
        finite(low,high,sus);return new Rule(source,index,channel,text(node,"Category"),true,center,low,high,sus,0);
    }
    private Rule peak(Source source,int index,String channel,JsonNode node) {
        double center=number(node,"PEAKvalue"),low=center-offset(node,"PEAKvalueDown"),high=center+offset(node,"PEAKvalueUP"),rm=offset(node,"PEAKvalueRM");
        finite(low,high);return new Rule(source,index,channel,text(node,"Category"),false,center,low,high,0,rm);
    }
    private void evaluateCt(ObjectNode row,Rule rule,JsonNode target) {
        JsonNode value=target.path("Ct");Double ct=measurement(value);
        row.set("actualCt",ct==null?mapper.nullNode():mapper.valueToTree(ct));
        if(ct==null) {
            if(value.isTextual() && Set.of("Infinity","+Infinity","-Infinity").contains(value.asText())) {
                row.put("reason","NON_AMPLIFYING_CT");conclude(row,"NEGATIVE","阴性");
            } else row.put("reason","MISSING_OR_INVALID_CT");
        } else if(ct>=rule.low && ct<=rule.high)conclude(row,"POSITIVE","阳性");
        else if(ct>rule.high && ct<=rule.suspicious)conclude(row,"SUSPICIOUS","可疑样本");
        else conclude(row,"NEGATIVE","阴性");
    }
    private void evaluatePeak(ObjectNode row,Rule rule,JsonNode target) {
        var values=target.path("PeakValues");
        var observed=row.putArray("observedPeaks");var matched=row.putArray("matchedPeaks");
        if(!values.isArray()) { row.put("reason","MISSING_PEAK_DATA");return; }
        boolean invalid=false;
        for(var peak:values) {
            Double tm=measurement(peak.path("Tm")),rm=measurement(peak.path("Rm"));
            if(tm==null || rm==null) { invalid=true;continue; }
            var item=observed.addObject().put("tm",tm).put("rm",rm);
            if(tm>=rule.low && tm<=rule.high && rm>=rule.rm)matched.add(item.deepCopy());
        }
        if(!matched.isEmpty())conclude(row,"POSITIVE","阳性");
        else if(invalid)row.put("reason","INVALID_PEAK_DATA");
        else conclude(row,"NEGATIVE","阴性");
    }
    private void conclude(ObjectNode row,String status,String text) { row.put("status",status).put("conclusion",text); }
    private static Double measurement(JsonNode value) { return value.isNumber() && Double.isFinite(value.doubleValue())?value.doubleValue():null; }
    private static String text(JsonNode node,String field) {
        var value=node.path(field);if(!value.isTextual() || value.asText().isBlank())throw new IllegalArgumentException("Missing "+field);return value.asText();
    }
    private static double number(JsonNode node,String field) {
        var value=measurement(node.path(field));if(value==null)throw new IllegalArgumentException("Missing or nonfinite "+field);return value;
    }
    private static double offset(JsonNode node,String field) { double value=number(node,field);if(value<0)throw new IllegalArgumentException("Negative "+field);return value; }
    private static void finite(double... values) { for(double value:values)if(!Double.isFinite(value))throw new IllegalArgumentException("Rule range overflow"); }
}
