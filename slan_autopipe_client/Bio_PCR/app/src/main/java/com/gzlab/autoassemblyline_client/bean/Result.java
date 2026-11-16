package com.gzlab.autoassemblyline_client.bean;

public class Result {

    private String category;//病毒种类
    private String conclusion;//病毒种类对应的结论
    private double value;////判断的ct值和peakvalue

    private String range;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
