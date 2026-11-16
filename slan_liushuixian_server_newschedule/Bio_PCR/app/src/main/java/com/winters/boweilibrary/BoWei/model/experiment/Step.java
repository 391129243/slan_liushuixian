package com.winters.boweilibrary.BoWei.model.experiment;

/**
 * 程序段步骤
 */
public class Step {
    private String Name;//名称
    private Double Temperature;//设置温度
    private int HoldTime;//保持时间（单位：秒）
    private Boolean IsAcquisition;//是否荧光检测
    private Boolean IsAdvProgram;//是否高级编程
    private int StartCycle;//高级编程的起始循环
    private Double TempVarValue;//温度变化值（°C）（大于0：递增。小于0：递减）
    private int TimeVarValue;//时间变化值（单位：秒）（大于0：递增。小于0：递减）
    private Double Ramp;//升降温速率(°C/s)（等于0：忽视。大于0：使用）

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public Double getTemperature() {
        return Temperature;
    }

    public void setTemperature(Double temperature) {
        Temperature = temperature;
    }

    public int getHoldTime() {
        return HoldTime;
    }

    public void setHoldTime(int holdTime) {
        HoldTime = holdTime;
    }

    public Boolean getAcquisition() {
        return IsAcquisition;
    }

    public void setAcquisition(Boolean acquisition) {
        IsAcquisition = acquisition;
    }

    public Boolean getAdvProgram() {
        return IsAdvProgram;
    }

    public void setAdvProgram(Boolean advProgram) {
        IsAdvProgram = advProgram;
    }

    public int getStartCycle() {
        return StartCycle;
    }

    public void setStartCycle(int startCycle) {
        StartCycle = startCycle;
    }

    public Double getTempVarValue() {
        return TempVarValue;
    }

    public void setTempVarValue(Double tempVarValue) {
        TempVarValue = tempVarValue;
    }

    public int getTimeVarValue() {
        return TimeVarValue;
    }

    public void setTimeVarValue(int timeVarValue) {
        TimeVarValue = timeVarValue;
    }

    public Double getRamp() {
        return Ramp;
    }

    public void setRamp(Double ramp) {
        Ramp = ramp;
    }
}
