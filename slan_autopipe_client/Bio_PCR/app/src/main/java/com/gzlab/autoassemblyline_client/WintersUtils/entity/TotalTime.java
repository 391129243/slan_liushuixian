package com.gzlab.autoassemblyline_client.WintersUtils.entity;

public class TotalTime {

    private long totalTime;

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
}
