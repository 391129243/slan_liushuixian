package com.gidi.bio_pcr.bean;

public class Threshold {
    double threshold;
    double peekHighThreshold;

    boolean isAutoThreshold;

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getPeekHighThreshold() {
        return peekHighThreshold;
    }

    public void setPeekHighThreshold(double peekHighThreshold) {
        this.peekHighThreshold = peekHighThreshold;
    }

    public boolean isAutoThreshold() {
        return isAutoThreshold;
    }

    public void setAutoThreshold(boolean autoThreshold) {
        isAutoThreshold = autoThreshold;
    }
}
