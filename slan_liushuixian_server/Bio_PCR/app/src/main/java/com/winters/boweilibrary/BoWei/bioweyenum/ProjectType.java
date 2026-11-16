package com.winters.boweilibrary.BoWei.bioweyenum;

/**
 * 实验类型枚举
 */
public enum ProjectType {
    AQ(0,"定性定量"),
    Melt(1,"标准熔解曲线");
    private int code;
    private String name;
    ProjectType(int code,String name){
        this.code = code;
        this.name= name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

}
