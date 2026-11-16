package com.winters.boweilibrary.BoWei.ExperimentData;

public class Temperature {

    private double BlockTemperature;
    private double TubeTemperature;
    private double HotlidTemperature;
    private Long Time;

    public double getBlockTemperature() {
        return BlockTemperature;
    }

    public void setBlockTemperature(double blockTemperature) {
        BlockTemperature = blockTemperature;
    }

    public double getTubeTemperature() {
        return TubeTemperature;
    }

    public void setTubeTemperature(double tubeTemperature) {
        TubeTemperature = tubeTemperature;
    }

    public double getHotlidTemperature() {
        return HotlidTemperature;
    }

    public void setHotlidTemperature(double hotlidTemperature) {
        HotlidTemperature = hotlidTemperature;
    }

    public Long getTime() {
        return Time;
    }

    public void setTime(Long time) {
        Time = time;
    }

    public Temperature(double blockTemperature, double tubeTemperature, double hotlidTemperature, Long time) {
        BlockTemperature = blockTemperature;
        TubeTemperature = tubeTemperature;
        HotlidTemperature = hotlidTemperature;
        Time = time;
    }
}
