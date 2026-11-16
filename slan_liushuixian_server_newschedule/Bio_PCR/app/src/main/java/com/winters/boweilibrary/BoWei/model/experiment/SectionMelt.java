package com.winters.boweilibrary.BoWei.model.experiment;

/**
 * 熔解程序段
 */
public class SectionMelt extends Section{
    private String Name;//名称
    private int ScanMode;//扫描方式枚举
    private Double Rate;//速率（°C/s）
    private Double Interval;//扫描间隔（°C）
    private int HoldTime;//保持时间（s）
    private Double StartTemp;//起始温度（°C）
    private Double EndTemp;//起始温度（°C）

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public int getScanMode() {
        return ScanMode;
    }

    public void setScanMode(int scanMode) {
        ScanMode = scanMode;
    }

    public Double getRate() {
        return Rate;
    }

    public void setRate(Double rate) {
        Rate = rate;
    }

    public Double getInterval() {
        return Interval;
    }

    public void setInterval(Double interval) {
        Interval = interval;
    }

    public int getHoldTime() {
        return HoldTime;
    }

    public void setHoldTime(int holdTime) {
        HoldTime = holdTime;
    }

    public Double getStartTemp() {
        return StartTemp;
    }

    public void setStartTemp(Double startTemp) {
        StartTemp = startTemp;
    }

    public Double getEndTemp() {
        return EndTemp;
    }

    public void setEndTemp(Double endTemp) {
        EndTemp = endTemp;
    }
}
