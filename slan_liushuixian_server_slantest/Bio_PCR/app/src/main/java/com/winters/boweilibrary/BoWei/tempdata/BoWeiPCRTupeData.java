package com.winters.boweilibrary.BoWei.tempdata;

public class BoWeiPCRTupeData {
    private int num;///PCR温度

    private double temperature_pre;///

    private  double temperature_post;///

    private double fluorescence_FAM;///

    private double fluorescence_CY5;///

    private double fluorescence_HEX;///

    private double fluorescence_ROX;///

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public double getTemperature_pre() {
        return temperature_pre;
    }

    public void setTemperature_pre(double temperature_pre) {
        this.temperature_pre = temperature_pre;
    }

    public double getTemperature_post() {
        return temperature_post;
    }

    public void setTemperature_post(double temperature_post) {
        this.temperature_post = temperature_post;
    }

    public double getFluorescence_FAM() {
        return fluorescence_FAM;
    }

    public void setFluorescence_FAM(double fluorescence_FAM) {
        this.fluorescence_FAM = fluorescence_FAM;
    }

    public double getFluorescence_CY5() {
        return fluorescence_CY5;
    }

    public void setFluorescence_CY5(double fluorescence_CY5) {
        this.fluorescence_CY5 = fluorescence_CY5;
    }

    public double getFluorescence_HEX() {
        return fluorescence_HEX;
    }

    public void setFluorescence_HEX(double fluorescence_HEX) {
        this.fluorescence_HEX = fluorescence_HEX;
    }

    public double getFluorescence_ROX() {
        return fluorescence_ROX;
    }

    public void setFluorescence_ROX(double fluorescence_ROX) {
        this.fluorescence_ROX = fluorescence_ROX;
    }

    public BoWeiPCRTupeData(int num, double temperature_pre, double temperature_post, double fluorescence_FAM, double fluorescence_CY5, double fluorescence_HEX, double fluorescence_ROX)
    {
        this.num = num;
        this.temperature_pre = temperature_pre;
        this.temperature_post = temperature_post;
        this.fluorescence_FAM = fluorescence_FAM;
        this.fluorescence_CY5 = fluorescence_CY5;
        this.fluorescence_HEX = fluorescence_HEX;
        this.fluorescence_ROX = fluorescence_ROX;
    }
}
