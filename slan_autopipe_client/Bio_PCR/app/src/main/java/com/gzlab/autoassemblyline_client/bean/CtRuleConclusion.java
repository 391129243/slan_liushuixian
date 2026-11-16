package com.gzlab.autoassemblyline_client.bean;

import java.util.List;
import java.util.Map;

public class CtRuleConclusion {

    private String conclusion;//阴性阳性可疑
    private String sampleID;//样本ID
    private String time;//检测时间
    private String tupeNo;//检测孔位

    Map<String, List<CtResult>> conlusionMap;

    private List<String> wellConclusion;


    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getSampleID() {
        return sampleID;
    }

    public void setSampleID(String sampleID) {
        this.sampleID = sampleID;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTupeNo() {
        return tupeNo;
    }

    public void setTupeNo(String tupeNo) {
        this.tupeNo = tupeNo;
    }

    public Map<String, List<CtResult>> getConlusionMap() {
        return conlusionMap;
    }

    public void setConlusionMap(Map<String, List<CtResult>> conlusionMap) {
        this.conlusionMap = conlusionMap;
    }

    public List<String> getWellConclusion() {
        return wellConclusion;
    }

    public void setWellConclusion(List<String> wellConclusion) {
        this.wellConclusion = wellConclusion;
    }
}
