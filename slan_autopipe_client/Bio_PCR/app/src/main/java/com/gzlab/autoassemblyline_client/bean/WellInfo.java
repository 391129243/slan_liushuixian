package com.gzlab.autoassemblyline_client.bean;

import com.hongshi.slansdk.slanenum.SampleType;

public class WellInfo {
    private String wellName;

    private String sampleName;//样本名称就是身份证号

    private String reagentCode;//试剂卡条码

    private String property;//反应孔属性，如果是待测样品就是Property
    private int sampleType = SampleType.Unknown.getValue();//反应孔样本类型，

    public WellInfo(){

    }

    public WellInfo(String wellName, String sampleName, String reagentCode) {
        this.wellName = wellName;
        this.sampleName = sampleName;
        this.reagentCode = reagentCode;
    }


    public String getWellName() {
        return wellName;
    }

    public void setWellName(String wellName) {
        this.wellName = wellName;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getReagentCode() {
        return reagentCode;
    }

    public void setReagentCode(String reagentCode) {
        this.reagentCode = reagentCode;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public int getSampleType() {
        return sampleType;
    }

    public void setSampleType(int sampleType) {
        this.sampleType = sampleType;
    }
}
