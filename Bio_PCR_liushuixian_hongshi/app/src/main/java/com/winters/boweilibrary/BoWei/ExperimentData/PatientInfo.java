package com.winters.boweilibrary.BoWei.ExperimentData;

public class PatientInfo {
    /// <summary>
    /// 唯一标示
    /// </summary>
    private String ID = "";

    /// <summary>
    /// 姓名
    /// </summary>
    private String Name =  "";

    /// <summary>
    /// 性别
    /// </summary>
    private String Sex = "";

    /// <summary>
    /// 年龄
    /// </summary>
    private String Age = "";

    /// <summary>
    /// 标本类型
    /// </summary>
    private String Specimen = "";


    /// <summary>
    /// 采样日期
    /// </summary>
    private String SpecimenDate = "";


    /// <summary>
    /// 送检医生
    /// </summary>
    private String Doctor = "";

    /// <summary>
    /// 送检科室
    /// </summary>
    private String Office = "";

    /// <summary>
    /// 诊断
    /// </summary>
    private String Diagnosis = "";

    /// <summary>
    /// 病历号
    /// </summary>
    private String CaseID = "";

    /// <summary>
    /// 病床号
    /// </summary>
    private String BedID = "";

    /// <summary>
    /// 住院号
    /// </summary>
    private String InPatientID = "";

    /// <summary>
    /// 门诊号
    /// </summary>
    private String OutPatientID = "";

    /// <summary>
    /// 备注
    /// </summary>
    private String Remark = "";


    /// <summary>
    /// 自定义
    /// </summary>

    private String[] CustomData = new String[10];

    public String getID() {
        return ID;
    }

    public void setID(String iD) {
        this.ID = iD;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public String getSex() {
        return Sex;
    }

    public void setSex(String sex) {
        this.Sex = sex;
    }

    public String getAge() {
        return Age;
    }

    public void setAge(String age) {
        this.Age = age;
    }

    public String getSpecimen() {
        return Specimen;
    }

    public void setSpecimen(String specimen) {
        this.Specimen = specimen;
    }

    public String getSpecimenDate() {
        return SpecimenDate;
    }

    public void setSpecimenDate(String specimenDate) {
        this.SpecimenDate = specimenDate;
    }

    public String getDoctor() {
        return Doctor;
    }

    public void setDoctor(String doctor) {
        this.Doctor = doctor;
    }

    public String getOffice() {
        return Office;
    }

    public void setOffice(String office) {
        this.Office = office;
    }

    public String getDiagnosis() {
        return Diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.Diagnosis = diagnosis;
    }

    public String getCaseID() {
        return CaseID;
    }

    public void setCaseID(String caseID) {
        this.CaseID = caseID;
    }

    public String getBedID() {
        return BedID;
    }

    public void setBedID(String bedID) {
        this.BedID = bedID;
    }

    public String getInPatientID() {
        return InPatientID;
    }

    public void setInPatientID(String inPatientID) {
        this.InPatientID = inPatientID;
    }

    public String getOutPatientID() {
        return OutPatientID;
    }

    public void setOutPatientID(String outPatientID) {
        this.OutPatientID = outPatientID;
    }

    public String getRemark() {
        return Remark;
    }

    public void setRemark(String remark) {
        this.Remark = remark;
    }

    public String[] getCustomData() {
        return CustomData;
    }

    public void setCustomData(String[] customData) {
        this.CustomData = customData;
    }

    public PatientInfo(String iD, String name, String sex, String age, String specimen, String specimenDate, String doctor, String office, String diagnosis, String caseID, String bedID, String inPatientID, String outPatientID, String remark, String[] customData) {
        this.ID = iD;
        this.Name = name;
        this.Sex = sex;
        this.Age = age;
        this.Specimen = specimen;
        this.SpecimenDate = specimenDate;
        this.Doctor = doctor;
        this.Office = office;
        this.Diagnosis = diagnosis;
        this.CaseID = caseID;
        this.BedID = bedID;
        this.InPatientID = inPatientID;
        this.OutPatientID = outPatientID;
        this.Remark = remark;
        this.CustomData = customData;
    }

    public PatientInfo() {
    }
}
