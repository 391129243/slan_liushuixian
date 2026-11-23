package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AndroidRuleEvaluatorTest {
    final ObjectMapper mapper=new ObjectMapper();
    final AndroidRuleEvaluator evaluator=new AndroidRuleEvaluator(mapper);
    static final String CT="[{\"Channel\":\"FAM\",\"Category\":\"示例Ct\",\"CTvalue\":35,\"CTvalueDown\":5,\"CTvalueUP\":3,\"CTvalueUP_Suspicious\":2,\"IsSelected\":false}]";
    static final String PEAK="[{\"Channel\":\"HEX\",\"PEAKSingleRuleEntityList\":[{\"Category\":\"示例峰\",\"PEAKvalue\":80,\"PEAKvalueDown\":1,\"PEAKvalueUP\":2,\"PEAKvalueRM\":100,\"IsSelected\":false}]}]";
    AndroidRuleEvaluator.Source source(String name,String role,String json) {
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);return new AndroidRuleEvaluator.Source(name,role,Digests.hash("SHA-256",bytes),bytes);
    }
    ObjectNode experiment() { var ex=mapper.createObjectNode();ex.putArray("WellResult");return ex; }
    ObjectNode target(ObjectNode ex,String well,String name) {
        return ((ArrayNode)ex.path("WellResult")).addObject().put("ID",well).putArray("TargetResults").addObject().put("Name",name);
    }
    @Test void ctBoundariesFollowAndroidAndSelectedIsOnlyAnEditorFlag() {
        var ex=experiment();double[] values={29.999,30,38,38.0001,40,40.0001,0};
        for(int i=0;i<values.length;i++)target(ex,"A"+i,"target-fAm").put("Ct",values[i]);
        var result=evaluator.evaluate(ex,List.of(source("single.ct","CT_RULE",CT)));
        assertThat(result.path("rows").findValuesAsText("status")).containsExactly("NEGATIVE","POSITIVE","POSITIVE","SUSPICIOUS","SUSPICIOUS","NEGATIVE","NEGATIVE");
        assertThat(result.path("status").asText()).isEqualTo("EVALUATED");assertThat(result.path("applicationRulesApplied").asBoolean()).isTrue();
    }
    @Test void missingCtAndMissingChannelNeverBecomeNegativeAndNeverReusePriorWellValues() {
        var ex=experiment();target(ex,"A1","FAM").put("Ct",35);target(ex,"A2","FAM").putNull("Ct");
        target(ex,"A3","HEX").put("Ct",35);target(ex,"A4","FAM").put("Ct","Infinity");target(ex,"A5","FAM").put("Ct","NaN");
        var result=evaluator.evaluate(ex,List.of(source("single.ct","CT_RULE",CT)));
        assertThat(result.path("rows").findValuesAsText("status")).containsExactly("POSITIVE","UNDETERMINED","UNDETERMINED","NEGATIVE","UNDETERMINED");
        assertThat(result.path("rows").get(1).path("actualCt").isNull()).isTrue();assertThat(result.path("status").asText()).isEqualTo("PARTIAL");
    }
    @Test void peakRequiresBothTmWindowAndRmAndRetainsAllMatchingEvidence() {
        var ex=experiment();var peaks=target(ex,"A1","HEX").putArray("PeakValues");
        peaks.addObject().put("Tm",79).put("Rm",100);peaks.addObject().put("Tm",82).put("Rm",101);peaks.addObject().put("Tm",85).put("Rm",900);
        target(ex,"A2","HEX").putArray("PeakValues").addObject().put("Tm",80).put("Rm",99.999);
        target(ex,"A3","HEX").putArray("PeakValues");target(ex,"A4","HEX").putNull("PeakValues");
        target(ex,"A5","HEX").putArray("PeakValues").addObject().put("Tm",80).putNull("Rm");
        var result=evaluator.evaluate(ex,List.of(source("multi.peak","PEAK_RULE",PEAK)));
        assertThat(result.path("rows").findValuesAsText("status")).containsExactly("POSITIVE","NEGATIVE","NEGATIVE","UNDETERMINED","UNDETERMINED");
        var first=result.path("rows").get(0);assertThat(first.path("matchedPeaks")).hasSize(2);assertThat(first.path("observedPeaks")).hasSize(3);
        assertThat(first.path("matchedPeaks").get(0).path("tm").asDouble()).isEqualTo(79);
    }
    @Test void invalidRulesAreExplicitAndConclusionsAreNeverTreatedAsRules() {
        var ex=experiment();target(ex,"A1","FAM").put("Ct",35);
        var invalid=source("broken.ct","CT_RULE","[{\"Channel\":\"FAM\",\"CTvalue\":35}]");
        var result=evaluator.evaluate(ex,List.of(invalid,source("old.conclusion","CONCLUSION_ARCHIVE","not JSON")));
        assertThat(result.path("status").asText()).isEqualTo("INVALID_RULES");assertThat(result.path("applicationRulesApplied").asBoolean()).isFalse();assertThat(result.path("errors")).hasSize(1);
        assertThat(evaluator.evaluate(ex,List.of(source("old.conclusion","CONCLUSION_ARCHIVE","[]"))).path("status").asText()).isEqualTo("MISSING_RULES");
        var wrongHash=new AndroidRuleEvaluator.Source("hash.ct","CT_RULE","0".repeat(64),CT.getBytes(StandardCharsets.UTF_8));
        assertThat(evaluator.evaluate(ex,List.of(wrongHash)).path("status").asText()).isEqualTo("INVALID_RULES");
        var partial=evaluator.evaluate(ex,List.of(invalid,source("valid.ct","CT_RULE",CT)));
        assertThat(partial.path("status").asText()).isEqualTo("PARTIAL");assertThat(partial.path("rows").get(0).path("status").asText()).isEqualTo("POSITIVE");
    }
    @Test void realVendorMeltPeakSchemaUsesRmAndDoesNotConfuseItWithYm() {
        // One peak copied from the supplied multi-target PCR's offline SDK JSON; no sample identity.
        var ex=experiment();var peak=target(ex,"A1","HEX").putArray("PeakValues").addObject()
            .put("Tm",48.46000000000019).put("Rm",35.357431704741714).put("Wm",5.920000000000087).put("Ym",28.775796868566903);
        var source=source("vendor-schema.peak","PEAK_RULE",PEAK.replace("80","48.46").replace("100","30"));
        assertThat(evaluator.evaluate(ex,List.of(source)).path("rows").get(0).path("status").asText()).isEqualTo("POSITIVE");
        peak.put("Rm",29).put("Ym",900);
        assertThat(evaluator.evaluate(ex,List.of(source)).path("rows").get(0).path("status").asText()).isEqualTo("NEGATIVE");
    }
    @Test void sameChannelRulesArePreservedAndMultipleTargetsAreNotSilentlyOverwritten() {
        var ex=experiment();target(ex,"A1","FAM").put("Ct",35);
        var rules=List.of(source("one.ct","CT_RULE",CT),source("two.ct","CT_RULE",CT.replace("示例Ct","另一项目")));
        var result=evaluator.evaluate(ex,rules);assertThat(result.path("rows")).hasSize(2);
        ((ArrayNode)ex.path("WellResult").get(0).path("TargetResults")).addObject().put("Name","FAM2").put("Ct",40);
        result=evaluator.evaluate(ex,rules);assertThat(result.path("rows").findValuesAsText("reason")).containsExactly("AMBIGUOUS_CHANNEL","AMBIGUOUS_CHANNEL");assertThat(result.path("applicationRulesApplied").asBoolean()).isFalse();
    }
}
