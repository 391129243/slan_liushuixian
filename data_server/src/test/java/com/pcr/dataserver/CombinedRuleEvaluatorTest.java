package com.pcr.dataserver;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CombinedRuleEvaluatorTest {
    final ObjectMapper mapper=new ObjectMapper();final CombinedRuleEvaluator evaluator=new CombinedRuleEvaluator(mapper);
    @Test void combinationTruthTableAndMissingnessAreExplicit() throws Exception {
        var policies=mapper.readTree("[{\"name\":\"ALL\",\"mode\":\"ALL\",\"ct\":{\"fileName\":\"a.ct\",\"ruleIndex\":0},\"peak\":{\"fileName\":\"b.peak\",\"ruleIndex\":0}},{\"name\":\"ANY\",\"mode\":\"ANY\",\"ct\":{\"fileName\":\"a.ct\",\"ruleIndex\":0},\"peak\":{\"fileName\":\"b.peak\",\"ruleIndex\":0}}]");
        String[][] cases={{"POSITIVE","POSITIVE","POSITIVE","POSITIVE"},{"POSITIVE","NEGATIVE","NEGATIVE","POSITIVE"},{"SUSPICIOUS","POSITIVE","SUSPICIOUS","POSITIVE"},{"SUSPICIOUS","NEGATIVE","NEGATIVE","SUSPICIOUS"},{"NEGATIVE","NEGATIVE","NEGATIVE","NEGATIVE"},{"POSITIVE","UNDETERMINED","UNDETERMINED","UNDETERMINED"}};
        for(var c:cases) {
            var individual=mapper.createObjectNode();var rows=individual.putArray("rows");
            rows.addObject().put("well","A1").put("fileName","a.ct").put("ruleIndex",0).put("status",c[0]);
            rows.addObject().put("well","A1").put("fileName","b.peak").put("ruleIndex",0).put("status",c[1]);
            assertThat(evaluator.evaluate(individual,policies).path("rows").findValuesAsText("status")).containsExactly(c[2],c[3]);
        }
        assertThat(evaluator.evaluate(mapper.createObjectNode(),null).path("status").asText()).isEqualTo("NOT_CONFIGURED");
    }
}
