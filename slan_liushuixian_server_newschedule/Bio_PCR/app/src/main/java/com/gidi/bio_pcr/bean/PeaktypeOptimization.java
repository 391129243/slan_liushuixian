package com.gidi.bio_pcr.bean;

public class PeaktypeOptimization {

    private String dyeName;//染料
    private int channelNo;//通道
    private boolean optimization_enable;//是否启用
    private double enhance_lower;
    private double enhance_upper;
    private double enhanceGain;//增强增益
    private double weaken;//削弱峰高阈值
    private double weakenGain;//削弱增益
    private double slopeThreshold;//斜率阈值

    public String getDyeName() {
        return dyeName;
    }

    public void setDyeName(String dyeName) {
        this.dyeName = dyeName;
    }

    public int getChannelNo() {
        return channelNo;
    }

    public void setChannelNo(int channelNo) {
        this.channelNo = channelNo;
    }

    public boolean isOptimization_enable() {
        return optimization_enable;
    }

    public void setOptimization_enable(boolean optimization_enable) {
        this.optimization_enable = optimization_enable;
    }

    public double getEnhance_lower() {
        return enhance_lower;
    }

    public void setEnhance_lower(double enhance_lower) {
        this.enhance_lower = enhance_lower;
    }

    public double getEnhance_upper() {
        return enhance_upper;
    }

    public void setEnhance_upper(double enhance_upper) {
        this.enhance_upper = enhance_upper;
    }

    public double getEnhanceGain() {
        return enhanceGain;
    }

    public void setEnhanceGain(double enhanceGain) {
        this.enhanceGain = enhanceGain;
    }

    public double getWeaken() {
        return weaken;
    }

    public void setWeaken(double weaken) {
        this.weaken = weaken;
    }

    public double getWeakenGain() {
        return weakenGain;
    }

    public void setWeakenGain(double weakenGain) {
        this.weakenGain = weakenGain;
    }

    public double getSlopeThreshold() {
        return slopeThreshold;
    }

    public void setSlopeThreshold(double slopeThreshold) {
        this.slopeThreshold = slopeThreshold;
    }
}
