package com.gidi.bio_pcr.WintersUtils.entity;

public class TotalTime {

    private long totalTime;

    private int firstRNATime = 0;//默认
    private int firstPcrTime= 0;

    private int secondRNATime= 0;
    private int secondPcrTime= 0;


    private int TemperatureCtrlMode = 2;//常规升温：0，快速升温2

    public long getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public int getTemperatureCtrlMode() {
        return TemperatureCtrlMode;
    }

    public void setTemperatureCtrlMode(int temperatureCtrlMode) {
        TemperatureCtrlMode = temperatureCtrlMode;
    }

    public int getFirstRNATime() {
        return firstRNATime;
    }

    public void setFirstRNATime(int firstRNATime) {
        this.firstRNATime = firstRNATime;
    }

    public int getFirstPcrTime() {
        return firstPcrTime;
    }

    public void setFirstPcrTime(int firstPcrTime) {
        this.firstPcrTime = firstPcrTime;
    }

    public int getSecondRNATime() {
        return secondRNATime;
    }

    public void setSecondRNATime(int secondRNATime) {
        this.secondRNATime = secondRNATime;
    }

    public int getSecondPcrTime() {
        return secondPcrTime;
    }

    public void setSecondPcrTime(int secondPcrTime) {
        this.secondPcrTime = secondPcrTime;
    }
}
