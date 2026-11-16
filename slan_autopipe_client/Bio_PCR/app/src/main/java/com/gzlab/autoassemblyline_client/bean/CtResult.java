package com.gzlab.autoassemblyline_client.bean;

public class CtResult extends Result{


    private double ctValueUp;//上限
    private double ctValueDown;
    private double ctValueUp_Suspicious;//可疑

    private double actualCt;


    public double getCtValueUp() {
        return ctValueUp;
    }

    public void setCtValueUp(double ctValueUp) {
        this.ctValueUp = ctValueUp;
    }

    public double getCtValueDown() {
        return ctValueDown;
    }

    public void setCtValueDown(double ctValueDown) {
        this.ctValueDown = ctValueDown;
    }

    public double getCtValueUp_Suspicious() {
        return ctValueUp_Suspicious;
    }

    public void setCtValueUp_Suspicious(double ctValueUp_Suspicious) {
        this.ctValueUp_Suspicious = ctValueUp_Suspicious;
    }

    public double getActualCt() {
        return actualCt;
    }

    public void setActualCt(double actualCt) {
        this.actualCt = actualCt;
    }
}
