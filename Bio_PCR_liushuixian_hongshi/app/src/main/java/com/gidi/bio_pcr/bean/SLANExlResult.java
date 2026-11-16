package com.gidi.bio_pcr.bean;

import com.gidi.bio_pcr.config.FileBean;
import com.hongshi.slansdk.model.result.MeltPeak;

import java.util.List;

/**
 * 结果页面显示表格
 */
public class SLANExlResult implements Comparable<SLANExlResult>{
    int channelNo;
    String curve_hole;//对应的是("FAM",CY5,HEX,ROX
    double ControlRn;
    double controlCt;
    List<MeltPeak> peakValues;
    double ct;
    String wellNum;//孔号
    boolean isAutoTh;
    boolean isMelt;//1。2是定性，定量。3是熔解曲线
    List<CtResult> results;
    List<PeakResult> peakResults;
    String regentCode;
    String idCard;

    double threshold;

    double peekHighThreshold;

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


    public double getControlRn() {
        return ControlRn;
    }

    public void setControlRn(double controlRn) {
        ControlRn = controlRn;
    }

    public double getControlCt() {
        return controlCt;
    }

    public void setControlCt(double controlCt) {
        this.controlCt = controlCt;
    }

    public double getCt() {
        return ct;
    }

    public void setCt(double ct) {
        this.ct = ct;
    }

    public String getWellNum() {
        return wellNum;
    }

    public void setWellNum(String wellNum) {
        this.wellNum = wellNum;
    }

    public boolean isAutoTh() {
        return isAutoTh;
    }

    public List<CtResult> getResults() {
        return results;
    }

    public void setResults(List<CtResult> results) {
        this.results = results;
    }

    public List<PeakResult> getPeakResults() {
        return peakResults;
    }

    //熔解结论
    public void setPeakResults(List<PeakResult> peakResults) {
        this.peakResults = peakResults;
    }

    public List<MeltPeak> getPeakValues() {
        return peakValues;
    }

    public void setPeakValues(List<MeltPeak> peakValues) {
        this.peakValues = peakValues;
    }

    public boolean isMelt() {
        return isMelt;
    }

    public void setMelt(boolean melt) {
        isMelt = melt;
    }

    public String getRegentCode() {
        return regentCode;
    }

    public void setRegentCode(String regentCode) {
        this.regentCode = regentCode;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

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

    @Override
    public int compareTo(SLANExlResult o) {
        if(this.wellNum.compareTo(o.getWellNum())>0){
            return 1;
        }
        if(this.wellNum.compareTo(o.getWellNum())<0){
            return -1;
        }
        return 0;
    }
}
