package com.gzlab.autoassemblyline_client.bean;

public class DefineWell {
    //反应孔类型
    int sampleType;
    //反应孔属性
    String property;

    public DefineWell(int sampleType, String property) {
        this.sampleType = sampleType;
        this.property = property;
    }

    public int getSampleType() {
        return sampleType;
    }

    public void setSampleType(int sampleType) {
        this.sampleType = sampleType;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }
}
