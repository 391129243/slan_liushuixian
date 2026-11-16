package com.gzlab.autoassemblyline_client.db;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.NotNull;
import com.litesuits.orm.db.annotation.Table;

@Table("ExperimentModel")//反应孔标识
public class ExperimentModel extends BaseModel {

    //项目名称

    @Column("Name")
    public String Name;

    //项目开始时间
    @NotNull
    @Column("StartTime")
    public String StartTime;

    @NotNull
    @Column("EndTime")
    //项目结束时间
    public String EndTime;


    @Column("Operator")
    //操作证
    public String Operator;

    @Column("Auditor")
    //审核者
    public String Auditor;


    @Column("Note")
    //备注
    public String Note;

    //版号

    @Column("PlateNo")
    public String PlateNo;

    //批次号

    @Column("BatchNo")
    public String  BatchNo;

    //设备编号

    @Column("SerialNumber")
    public String SerialNumber;

    //设备型号

    @Column("DeviceModel")
    public String DeviceModel;

    //自定义列名称

    //String[] CustomColumns
    public String[] CustomColumns;

    //反应孔结果列表WellResult[]pcrWellResult
    public String pcrWellResult;

    //模板名称
    @Column("ProjectName")
    public String  ProjectName;
    @NotNull
    @Column("testItem")
    private String testItem;
    @NotNull
    @Column("templateDirName")
    private String templateDirName;


    @Column("expType")
    private int expType = 1;//项目类型,默认是丁智,2是宏石

    @Column("Type")
    private int Type =0;//0是全流程，2是单PCR

    //项目描述
    @Column("description")
    private String description;


    private boolean isShow = false;//显示checkbox
    private boolean isCheck = false;//是否选中

    public String getName() {
        return Name;
    }

    public String getStartTime() {
        return StartTime;
    }

    public String getEndTime() {
        return EndTime;
    }

    public String getOperator() {
        return Operator;
    }

    public String getAuditor() {
        return Auditor;
    }

    public String getNote() {
        return Note;
    }

    public String getPlateNo() {
        return PlateNo;
    }

    public String getBatchNo() {
        return BatchNo;
    }

    public String getSerialNumber() {
        return SerialNumber;
    }

    public String getDeviceModel() {
        return DeviceModel;
    }

    public String[] getCustomColumns() {
        return CustomColumns;
    }

    public String getPcrWellResult() {
        return pcrWellResult;
    }

    public String getProjectName() {
        return ProjectName;
    }

    public void setName(String name) {
        Name = name;
    }

    public void setStartTime(String startTime) {
        StartTime = startTime;
    }

    public void setEndTime(String endTime) {
        EndTime = endTime;
    }

    public void setOperator(String operator) {
        Operator = operator;
    }

    public void setAuditor(String auditor) {
        Auditor = auditor;
    }

    public void setNote(String note) {
        Note = note;
    }

    public void setPlateNo(String plateNo) {
        PlateNo = plateNo;
    }

    public void setBatchNo(String batchNo) {
        BatchNo = batchNo;
    }

    public void setSerialNumber(String serialNumber) {
        SerialNumber = serialNumber;
    }

    public void setDeviceModel(String deviceModel) {
        DeviceModel = deviceModel;
    }

    public void setCustomColumns(String[] customColumns) {
        CustomColumns = customColumns;
    }

    public void setPcrWellResult(String pcrWellResult) {
        this.pcrWellResult = pcrWellResult;
    }

    public void setProjectName(String projectName) {
        ProjectName = projectName;
    }

    public void setTestItem(String testItem) {
        this.testItem = testItem;
    }

    public String getTestItem() {
        return testItem;
    }

    public String getTemplateDirName() {
        return templateDirName;
    }

    public void setTemplateDirName(String templateDirName) {
        this.templateDirName = templateDirName;
    }


    public int getExpType() {
        return expType;
    }

    public void setExpType(int expType) {
        this.expType = expType;
    }

    public boolean isShow() {
        return isShow;
    }

    public void setShow(boolean show) {
        isShow = show;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setCheck(boolean check) {
        isCheck = check;
    }

    public int getType() {
        return Type;
    }

    public void setType(int type) {
        Type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
