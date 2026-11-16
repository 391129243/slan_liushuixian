package com.winters.boweilibrary.BoWei.bean;

import com.winters.boweilibrary.BoWei.ExperimentData.Peak;
import com.github.mikephil.charting.data.Entry;

import java.util.List;

public class BiowayCurveList {
    int channelNo;
    String curve_hole;//对应的是("FAM",CY5,HEX,ROX
    List<Entry> curve_data;
    double ControlRn;
    double ct;
    String wellNum;//孔号

    List<Peak> peakValues;

    private String idCard;


    public int getChannelNo() {
        return channelNo;
    }

    public void setChannelNo(int channelNo) {
        this.channelNo = channelNo;
    }

    public String getCurve_hole() {
        return curve_hole;
    }

    public void setCurve_hole(String curve_hole) {
        this.curve_hole = curve_hole;
    }

    public List<Entry> getCurve_data() {
        return curve_data;
    }

    public void setCurve_data(List<Entry> curve_data) {
        this.curve_data = curve_data;
    }


    public double getCt() {
        return ct;
    }

    public void setCt(double ct) {
        this.ct = ct;
    }

    public double getControlRn() {
        return ControlRn;
    }

    public void setControlRn(double controlRn) {
        ControlRn = controlRn;
    }

    public String getWellNum() {
        return wellNum;
    }

    public void setWellNum(String wellNum) {
        this.wellNum = wellNum;
    }


    public List<Peak> getPeakValues() {
        return peakValues;
    }

    public void setPeakValues(List<Peak> peakValues) {
        this.peakValues = peakValues;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }
}
