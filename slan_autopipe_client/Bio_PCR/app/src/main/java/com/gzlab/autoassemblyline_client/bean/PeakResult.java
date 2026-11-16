package com.gzlab.autoassemblyline_client.bean;

public class PeakResult extends Result{

    private double peakValueUp;//上限
    private double peakValueDown;
    private double peakValueRM;//可疑

    private double actualTM;//实际测量TM

    private double actualRM;//实际测量RM


    public double getPeakValueUp() {
        return peakValueUp;
    }

    public void setPeakValueUp(double peakValueUp) {
        this.peakValueUp = peakValueUp;
    }

    public double getPeakValueDown() {
        return peakValueDown;
    }

    public void setPeakValueDown(double peakValueDown) {
        this.peakValueDown = peakValueDown;
    }

    public double getPeakValueRM() {
        return peakValueRM;
    }

    public void setPeakValueRM(double peakValueRM) {
        this.peakValueRM = peakValueRM;
    }

    public double getActualTM() {
        return actualTM;
    }

    public void setActualTM(double actualTM) {
        this.actualTM = actualTM;
    }

    public double getActualRM() {
        return actualRM;
    }

    public void setActualRM(double actualRM) {
        this.actualRM = actualRM;
    }
}
