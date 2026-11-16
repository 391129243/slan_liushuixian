package com.gzlab.autoassemblyline_client.bean;

public class QuantityRuleEntity {
    private String targetName;//检测目标
    private double cnValueUp;
    private double cnValueDown;
    private String conclusion;
    private double actualCn;
    private String downSymbol;
    private String upSymbol;
    private String valueType;//数据类型

    private boolean is_select = false;


    public QuantityRuleEntity(boolean is_select,
                              String targetName,
                              double cnValueUp,
                              double cnValueDown,
                              double actualCn,
                              String conclusion,
                              String downSymbol,
                              String upSymbol,
                              String valueType){
        this.is_select = is_select;
        this.targetName = targetName;
        this.cnValueUp = cnValueUp;
        this.cnValueDown = cnValueDown;
        this.conclusion = conclusion;
        this.downSymbol = downSymbol;
        this.upSymbol = upSymbol;
        this.actualCn = actualCn;
        this.valueType = valueType;

    }

    public QuantityRuleEntity(){

    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public double getCnValueUp() {
        return cnValueUp;
    }

    public void setCnValueUp(double cnValueUp) {
        this.cnValueUp = cnValueUp;
    }

    public double getCnValueDown() {
        return cnValueDown;
    }

    public void setCnValueDown(double cnValueDown) {
        this.cnValueDown = cnValueDown;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public double getActualCn() {
        return actualCn;
    }

    public void setActualCn(double actualCn) {
        this.actualCn = actualCn;
    }

    public String getDownSymbol() {
        return downSymbol;
    }

    public void setDownSymbol(String downSymbol) {
        this.downSymbol = downSymbol;
    }

    public String getUpSymbol() {
        return upSymbol;
    }

    public void setUpSymbol(String upSymbol) {
        this.upSymbol = upSymbol;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
}
