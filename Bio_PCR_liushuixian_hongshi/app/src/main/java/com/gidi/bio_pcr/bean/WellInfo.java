package com.gidi.bio_pcr.bean;

public class WellInfo {
    private String wellName;

    private String sampleName;//样本名称就是身份证号

    private String reagentCode;//试剂卡条码


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

}
