package com.winters.boweilibrary.BoWei.tempdata;

public class BoWeiPCRTupeTempretureData {

    private int num;///热盖温度

    private double temperature;///

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public BoWeiPCRTupeTempretureData(int num, double temperature)
    {
        this.num = num;
        this.temperature = temperature;
    }

}
