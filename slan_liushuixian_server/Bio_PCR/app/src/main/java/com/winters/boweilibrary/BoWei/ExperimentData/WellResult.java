package com.winters.boweilibrary.BoWei.ExperimentData;

import com.winters.boweilibrary.BoWei.ExperimentData.Enum.SampleType;

import java.util.ArrayList;
import java.util.List;

public class WellResult {

    /// <summary>
    /// 反应孔标识，比如：A1、A2等等
    /// </summary>
    private String ID = "";

    /// <summary>
    /// 样本所在反应空名称，比如：三管实验[A1,A2,A3]
    /// </summary>
    private List<String> SampleWellNames = new ArrayList<>();

    /// <summary>
    /// 样本类型
    /// </summary>
    private SampleType SampleType = com.winters.boweilibrary.BoWei.ExperimentData.Enum.SampleType.Unknown;

    /// <summary>
    /// 样本类型名称
    /// </summary>
    private String SampleTypeName = "";


    /// <summary>
    /// 样本名称
    /// </summary>
    private String SampleName = "";


    /// <summary>
    /// 复管编号
    /// </summary>
    private String CompoundTubeNo = "";

    /// <summary>
    /// 样本患者信息
    /// </summary>
    private PatientInfo PatientInfo = new PatientInfo();

    /// <summary>
    /// 反应孔包含的目标结果
    /// </summary>
    private List<TargetResult> TargetResults = new ArrayList<>();


    /// <summary>
    /// 样本结论
    /// </summary>
    private List<String> SampleConclusion = new ArrayList<>();

    /// <summary>
    /// 样本中间结果
    /// </summary>
    private List<MidValue> SampleMidValue = new ArrayList<MidValue>();


    /// <summary>
    /// 孔结论
    /// </summary>
    private List<String> WellConclusion = new ArrayList<String>();

    /// <summary>
    /// 样本中间结果
    /// </summary>
    private List<MidValue> WellMidValue = new ArrayList<MidValue>();

    public String getID() {
        return ID;
    }

    public void setID(String iD) {
        this.ID = iD;
    }

    public List<String> getSampleWellNames() {
        return SampleWellNames;
    }

    public void setSampleWellNames(List<String> sampleWellNames) {
        this.SampleWellNames = sampleWellNames;
    }

    public SampleType getSampleType() {
        return SampleType;
    }

    public void setSampleType(SampleType sampleType) {
        this.SampleType = sampleType;
    }

    public String getSampleTypeName() {
        return SampleTypeName;
    }

    public void setSampleTypeName(String sampleTypeName) {
        this.SampleTypeName = sampleTypeName;
    }

    public String getSampleName() {
        return SampleName;
    }

    public void setSampleName(String sampleName) {
        this.SampleName = sampleName;
    }

    public String getCompoundTubeNo() {
        return CompoundTubeNo;
    }

    public void setCompoundTubeNo(String compoundTubeNo) {
        this.CompoundTubeNo = compoundTubeNo;
    }

    public PatientInfo getPatientInfo() {
        return PatientInfo;
    }

    public void setPatientInfo(PatientInfo patientInfo) {
        this.PatientInfo = patientInfo;
    }

    public List<TargetResult> getTargetResults() {
        return TargetResults;
    }

    public void setTargetResults(List<TargetResult> targetResults) {
        this.TargetResults = targetResults;
    }

    public List<String> getSampleConclusion() {
        return SampleConclusion;
    }

    public void setSampleConclusion(List<String> sampleConclusion) {
        this.SampleConclusion = sampleConclusion;
    }

    public List<MidValue> getSampleMidValue() {
        return SampleMidValue;
    }

    public void setSampleMidValue(List<MidValue> sampleMidValue) {
        this.SampleMidValue = sampleMidValue;
    }

    public List<String> getWellConclusion() {
        return WellConclusion;
    }

    public void setWellConclusion(List<String> wellConclusion) {
        this.WellConclusion = wellConclusion;
    }

    public List<MidValue> getWellMidValue() {
        return WellMidValue;
    }

    public void setWellMidValue(List<MidValue> wellMidValue) {
        this.WellMidValue = wellMidValue;
    }

    public WellResult(String iD, List<String> sampleWellNames, SampleType sampleType, String sampleTypeName, String sampleName, String compoundTubeNo, PatientInfo patientInfo, List<TargetResult> targetResults, List<String> sampleConclusion, List<MidValue> sampleMidValue, List<String> wellConclusion, List<MidValue> wellMidValue) {
        this.ID = iD;
        this.SampleWellNames = sampleWellNames;
        this.SampleType = sampleType;
        this.SampleTypeName = sampleTypeName;
        this.SampleName = sampleName;
        this.CompoundTubeNo = compoundTubeNo;
        this.PatientInfo = patientInfo;
        this.TargetResults = targetResults;
        this.SampleConclusion = sampleConclusion;
        this.SampleMidValue = sampleMidValue;
        this.WellConclusion = wellConclusion;
        this.WellMidValue = wellMidValue;
    }

    public WellResult() {
    }
}
