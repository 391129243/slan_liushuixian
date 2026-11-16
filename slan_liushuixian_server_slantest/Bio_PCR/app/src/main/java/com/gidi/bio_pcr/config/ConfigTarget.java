package com.gidi.bio_pcr.config;


/**
 * 检测目标
 */
public class ConfigTarget implements Comparable<ConfigTarget>{
    private String name;
    private double threshold;
    private int channelNo;

    private int targetIndex;

    private String color;

    private double maxTemperature;

    private double minTemperature;
    private double peekHighThreshold;

    private boolean qualitativeAnalysis;//定性和定量
    private int optimizationMode;//0:不优化，1，自动优化，2手动优化
    private int beginBaseline;
    private int endBaseline;
    private int digitalFilter;//数字滤波
    private boolean autoThreshold;//自动阈值
    private double detectMin;//检测下限
    private double detectMax;//检测上限

    private int peekType;//熔解峰型 0正峰 1负峰 2 正负峰

    private int meltPeakAlgorithm;//熔解曲线算法

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public int getChannelNo() {
        return channelNo;
    }

    public void setChannelNo(int channelNo) {
        this.channelNo = channelNo;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getTargetIndex() {
        return targetIndex;
    }

    public void setTargetIndex(int targetIndex) {
        this.targetIndex = targetIndex;
    }

    public double getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public double getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(double minTemperature) {
        this.minTemperature = minTemperature;
    }

    public int getMeltPeakAlgorithm() {
        return meltPeakAlgorithm;
    }

    public void setMeltPeakAlgorithm(int meltPeakAlgorithm) {
        this.meltPeakAlgorithm = meltPeakAlgorithm;
    }

    public int getBeginBaseline() {
        return beginBaseline;
    }

    public void setBeginBaseline(int beginBaseline) {
        this.beginBaseline = beginBaseline;
    }

    public int getEndBaseline() {
        return endBaseline;
    }

    public void setEndBaseline(int endBaseline) {
        this.endBaseline = endBaseline;
    }

    public int getDigitalFilter() {
        return digitalFilter;
    }

    public void setDigitalFilter(int digitalFilter) {
        this.digitalFilter = digitalFilter;
    }

    public double getPeekHighThreshold() {
        return peekHighThreshold;
    }

    public void setPeekHighThreshold(double peekHighThreshold) {
        this.peekHighThreshold = peekHighThreshold;
    }

    public boolean isQualitativeAnalysis() {
        return qualitativeAnalysis;
    }

    public void setQualitativeAnalysis(boolean qualitativeAnalysis) {
        this.qualitativeAnalysis = qualitativeAnalysis;
    }

    public int getPeekType() {
        return peekType;
    }

    public void setPeekType(int peekType) {
        this.peekType = peekType;
    }

    public boolean isAutoThreshold() {
        return autoThreshold;
    }

    public void setAutoThreshold(boolean autoThreshold) {
        this.autoThreshold = autoThreshold;
    }

    public int getOptimizationMode() {
        return optimizationMode;
    }

    public void setOptimizationMode(int optimizationMode) {
        this.optimizationMode = optimizationMode;
    }

    public double getDetectMin() {
        return detectMin;
    }

    public void setDetectMin(double detectMin) {
        this.detectMin = detectMin;
    }

    public double getDetectMax() {
        return detectMax;
    }

    public void setDetectMax(double detectMax) {
        this.detectMax = detectMax;
    }

    @Override
    public int compareTo(ConfigTarget o) {
        if(this.channelNo > o.channelNo){
            return 1;
        }
        if(this.channelNo < o.channelNo){
            return -1;
        }
        return 0;
    }
}
