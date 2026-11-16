package com.gzlab.autoassemblyline_client.bean;

import java.util.List;

public class JudgeConclusion {
    private String templateName;
    private List<PeakRuleConclusion> peakRuleConclusions;
    private List<CtRuleConclusion> ctRuleConclusions;

    public List<PeakRuleConclusion> getPeakRuleConclusions() {
        return peakRuleConclusions;
    }

    public void setPeakRuleConclusions(List<PeakRuleConclusion> peakRuleConclusions) {
        this.peakRuleConclusions = peakRuleConclusions;
    }

    public List<CtRuleConclusion> getCtRuleConclusions() {
        return ctRuleConclusions;
    }

    public void setCtRuleConclusions(List<CtRuleConclusion> ctRuleConclusions) {
        this.ctRuleConclusions = ctRuleConclusions;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
