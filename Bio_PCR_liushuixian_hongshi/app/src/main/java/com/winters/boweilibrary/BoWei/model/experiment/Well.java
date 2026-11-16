package com.winters.boweilibrary.BoWei.model.experiment;
/**
 * 孔
 */
public class Well {
    private String Name;//孔号，如：A1，A2（必填）
    private String ProjectName;//项目名称
    private String SampleName;//样本名称
    private int TubeNo;//试管编号
    private String TubeName;//试管名称
    private int Type;//样本类型
    private String Property;//属性
    private String ID;//唯一标识
    private String PatientName;//姓名
    private String Sex;//性别
    private String Age;//年龄
    private String Specimen;//样本类型
    private String SpecimenDate;//采样日期
    private String Doctor;//送检医生
    private String Office;//送检可是
    private String Diagnosis;//诊断
    private String CaseID;//病历号
    private String BedID;//病床号
    private String InPatientID;//住院号
    private String OutPatientID;//门诊号
    private String Remark;//备注


    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getProjectName() {
        return ProjectName;
    }

    public void setProjectName(String projectName) {
        ProjectName = projectName;
    }

    public String getSampleName() {
        return SampleName;
    }

    public void setSampleName(String sampleName) {
        SampleName = sampleName;
    }

    public int getTubeNo() {
        return TubeNo;
    }

    public void setTubeNo(int tubeNo) {
        TubeNo = tubeNo;
    }

    public String getTubeName() {
        return TubeName;
    }

    public void setTubeName(String tubeName) {
        TubeName = tubeName;
    }


    public int getType() {
        return Type;
    }

    public void setType(int type) {
        Type = type;
    }

    public String getProperty() {
        return Property;
    }

    public void setProperty(String property) {
        Property = property;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public String getPatientName() {
        return PatientName;
    }

    public void setPatientName(String patientName) {
        PatientName = patientName;
    }

    public String getSex() {
        return Sex;
    }

    public void setSex(String sex) {
        Sex = sex;
    }

    public String getAge() {
        return Age;
    }

    public void setAge(String age) {
        Age = age;
    }

    public String getSpecimen() {
        return Specimen;
    }

    public void setSpecimen(String specimen) {
        Specimen = specimen;
    }

    public String getSpecimenDate() {
        return SpecimenDate;
    }

    public void setSpecimenDate(String specimenDate) {
        SpecimenDate = specimenDate;
    }

    public String getDoctor() {
        return Doctor;
    }

    public void setDoctor(String doctor) {
        Doctor = doctor;
    }

    public String getOffice() {
        return Office;
    }

    public void setOffice(String office) {
        Office = office;
    }

    public String getDiagnosis() {
        return Diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        Diagnosis = diagnosis;
    }

    public String getCaseID() {
        return CaseID;
    }

    public void setCaseID(String caseID) {
        CaseID = caseID;
    }

    public String getBedID() {
        return BedID;
    }

    public void setBedID(String bedID) {
        BedID = bedID;
    }

    public String getInPatientID() {
        return InPatientID;
    }

    public void setInPatientID(String inPatientID) {
        InPatientID = inPatientID;
    }

    public String getOutPatientID() {
        return OutPatientID;
    }

    public void setOutPatientID(String outPatientID) {
        OutPatientID = outPatientID;
    }

    public String getRemark() {
        return Remark;
    }

    public void setRemark(String remark) {
        Remark = remark;
    }
}
