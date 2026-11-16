package com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData;

import java.util.ArrayList;
import java.util.List;

public class TargetResult {
    /// <summary>
    /// 检测通道
    /// </summary>
    private int Channel = 0;

    /// <summary>
    /// 检测目标，比如HBV
    /// </summary>
    private String Name = "";

    /// <summary>
    /// Ct值
    /// </summary>
    private double Ct = 0.0f;

    /// <summary>
    /// CtMean
    /// </summary>
    private double CtMean = 0.0f;


    /// <summary>
    /// CtSD
    /// </summary>
    private double CtSD = 0.0f;


    /// <summary>
    /// CtCV
    /// </summary>
    private double CtCV = 0.0f;


    /// <summary>
    /// Cn
    /// </summary>
    private double Cn = 0.0f;


    /// <summary>
    /// CnMean
    /// </summary>
    private double CnMean = 0.0f;


    /// <summary>
    /// CnSD
    /// </summary>
    private double CnSD = 0.0f;


    /// <summary>
    /// CnCV
    /// </summary>
    private double CnCV = 0.0f;


    /// <summary>
    /// Rn
    /// </summary>
    private double Rn = 0.0f;


    /// <summary>
    /// 原始曲线数据
    /// </summary>
    private List<Double> RawCurve = new ArrayList<Double>();


    /// <summary>
    /// 扩增曲线数据
    /// </summary>
    private List<Double> AMPCurve = new ArrayList<Double>();

    /// <summary>
    /// 原始熔解曲线数据
    /// </summary>
    private List<Dot> RawMeltCurve = new ArrayList<Dot>();


    /// <summary>
    /// 峰值曲线数据
    /// </summary>
    private List<Dot> PeakCurve = new ArrayList<Dot>();


    /// <summary>
    /// 熔解峰数据
    /// </summary>
    private List<Peak> PeakValues = new ArrayList<Peak>();


    /// <summary>
    /// 试管名称
    /// </summary>
    private String TubeName = "";

    /// <summary>
    /// 试管编号
    /// </summary>
    private int TubeNo = 0;

    /// <summary>
    /// 染料名称
    /// </summary>
    private String Dye ="";

    /// <summary>
    /// 属性
    /// </summary>
    private String Property = "";

    /// <summary>
    /// 目标结论
    /// </summary>
    private List<String> ConclusionList  = new ArrayList<String>();

    /// <summary>
    /// 中间结果
    /// </summary>
    private List<MidValue> MidValue = new ArrayList<MidValue>();

    public int getChannel() {
        return Channel;
    }

    public void setChannel(int channel) {
        this.Channel = channel;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public double getCt() {
        return Ct;
    }

    public void setCt(double ct) {
        this.Ct = ct;
    }

    public double getCtMean() {
        return CtMean;
    }

    public void setCtMean(double ctMean) {
        this.CtMean = ctMean;
    }

    public double getCtSD() {
        return CtSD;
    }

    public void setCtSD(double ctSD) {
        this.CtSD = ctSD;
    }

    public double getCtCV() {
        return CtCV;
    }

    public void setCtCV(double ctCV) {
        this.CtCV = ctCV;
    }

    public double getCn() {
        return Cn;
    }

    public void setCn(double cn) {
        this.Cn = cn;
    }

    public double getCnMean() {
        return CnMean;
    }

    public void setCnMean(double cnMean) {
        this.CnMean = cnMean;
    }

    public double getCnSD() {
        return CnSD;
    }

    public void setCnSD(double cnSD) {
        this.CnSD = cnSD;
    }

    public double getCnCV() {
        return CnCV;
    }

    public void setCnCV(double cnCV) {
        this.CnCV = cnCV;
    }

    public double getRn() {
        return Rn;
    }

    public void setRn(double rn) {
        this.Rn = rn;
    }

    public List<Double> getRawCurve() {
        return RawCurve;
    }

    public void setRawCurve(List<Double> rawCurve) {
        this.RawCurve = rawCurve;
    }

    public List<Double> getAmpCurve() {
        return AMPCurve;
    }

    public void setAmpCurve(List<Double> aMPCurve) {
        this.AMPCurve = aMPCurve;
    }

    public List<Dot> getRawMeltCurve() {
        return RawMeltCurve;
    }

    public void setRawMeltCurve(List<Dot> rawMeltCurve) {
        this.RawMeltCurve = rawMeltCurve;
    }

    public List<Dot> getPeakCurve() {
        return PeakCurve;
    }

    public void setPeakCurve(List<Dot> peakCurve) {
        this.PeakCurve = peakCurve;
    }

    public List<Peak> getPeakValues() {
        return PeakValues;
    }

    public void setPeakValues(List<Peak> peakValues) {
        this.PeakValues = peakValues;
    }

    public String getTubeName() {
        return TubeName;
    }

    public void setTubeName(String tubeName) {
        this.TubeName = tubeName;
    }

    public int getTubeNo() {
        return TubeNo;
    }

    public void setTubeNo(int tubeNo) {
        this.TubeNo = tubeNo;
    }

    public String getDye() {
        return Dye;
    }

    public void setDye(String dye) {
        this.Dye = dye;
    }

    public String getProperty() {
        return Property;
    }

    public void setProperty(String property) {
        this.Property = property;
    }

    public List<String> getConclusionList() {
        return ConclusionList;
    }

    public void setConclusionList(List<String> conclusionList) {
        this.ConclusionList = conclusionList;
    }

    public List<MidValue> getMidValue() {
        return MidValue;
    }

    public void setMidValue(List<MidValue> midValue) {
        this.MidValue = midValue;
    }

    public TargetResult(int channel, String name, double ct, double ctMean, double ctSD, double ctCV, double cn, double cnMean, double cnSD, double cnCV, double rn, List<Double> rawCurve, List<Double> aMPCurve, List<Dot> rawMeltCurve, List<Dot> peakCurve, List<Peak> peakValues, String tubeName, int tubeNo, String dye, String property, List<String> conclusionList, List<MidValue> midValue) {
        this.Channel = channel;
        this.Name = name;
        this.Ct = ct;
        this.CtMean = ctMean;
        this.CtSD = ctSD;
        this.CtCV = ctCV;
        this.Cn = cn;
        this.CnMean = cnMean;
        this.CnSD = cnSD;
        this.CnCV = cnCV;
        this.Rn = rn;
        this.RawCurve = rawCurve;
        this.AMPCurve = aMPCurve;
        this.RawMeltCurve = rawMeltCurve;
        this.PeakCurve = peakCurve;
        this.PeakValues = peakValues;
        this.TubeName = tubeName;
        this.TubeNo = tubeNo;
        this.Dye = dye;
        this.Property = property;
        this.ConclusionList = conclusionList;
        this.MidValue = midValue;
    }

    public TargetResult() {
    }
}
