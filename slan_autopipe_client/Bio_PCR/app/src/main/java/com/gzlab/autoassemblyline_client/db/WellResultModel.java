package com.gzlab.autoassemblyline_client.db;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.NotNull;
import com.litesuits.orm.db.annotation.Table;

import java.io.Serializable;

/**
 * 检测结果
 */
@Table("WellResultModel")//反应孔标识
public class WellResultModel extends BaseModel implements Serializable {
    private static final long serialVersionUID=1L;
    /**
     * 试剂卡条码
     */
    @NotNull
    @Column("reagentCode")
    private String reagentCode;


    /**
     * 样本名称，身份证号
     */
    @NotNull
    @Column("sampleName")
    private String sampleName;

    /**
     * 测试项目
     */

    @Column("testItem")
    private String testItem;

    /**
     * 项目名称模板名称
     */

    @Column("experimentName")
    private String experimentName;//8个孔一样的批次

    /**
     * 测试开始时间
     */

    @Column("startTime")
    private String startTime;


    /**
     * 测试结束时间
     */

    @Column("endTime")
    private String endTime;

    /**
     *反应孔名称
     */

    @Column("wellName")
    public String wellName;

    //项目类型1定性、2绝对定量，3标准熔解曲线

    @Column("expType")
    public int expType;

    //检测通道类型，多选(FAM,CY5,HEX,ROX)String[]channel 对象用逗号隔开进行String存储，是染料

    @Column("channel")
    public  String channel;

    /**
     * 反应孔结论
     */

    @Column("conclusion")
    public String conclusion;

    //种类

    //反应孔包含的目标结果TargetResult[] TargetResults
    @Column("mROXtargetResults")
    public String mROXtargetResults;

    @Column("mHEXtargetResults")
    public String mHEXtargetResults;

    @Column("mFAMtargetResults")
    public String mFAMtargetResults;

    @Column("mCY5targetResults")
    public String mCY5targetResults;

    @Column("roxResults")
    private String roxResults;

    @Column("famResults")
    private String famResults;

    @Column("hexResults")
    private String hexResults;

    @Column("cy5Results")
    private String cy5Results;


    private boolean isShow = false;

    private boolean isCheck = false;

    public String getReagentCode() {
        return reagentCode;
    }

    public void setReagentCode(String reagentCode) {
        this.reagentCode = reagentCode;
    }


    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getTestItem() {
        return testItem;
    }

    public void setTestItem(String testItem) {
        this.testItem = testItem;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getWellName() {
        return wellName;
    }

    public void setWellName(String wellName) {
        this.wellName = wellName;
    }

    public int getExpType() {
        return expType;
    }

    public void setExpType(int expType) {
        this.expType = expType;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getmROXtargetResults() {
        return mROXtargetResults;
    }

    public void setmROXtargetResults(String mROXtargetResults) {
        this.mROXtargetResults = mROXtargetResults;
    }

    public String getmHEXtargetResults() {
        return mHEXtargetResults;
    }

    public void setmHEXtargetResults(String mHEXtargetResults) {
        this.mHEXtargetResults = mHEXtargetResults;
    }

    public String getmFAMtargetResults() {
        return mFAMtargetResults;
    }

    public void setmFAMtargetResults(String mFAMtargetResults) {
        this.mFAMtargetResults = mFAMtargetResults;
    }

    public String getmCY5targetResults() {
        return mCY5targetResults;
    }

    public void setmCY5targetResults(String mCY5targetResults) {
        this.mCY5targetResults = mCY5targetResults;
    }

    public String getRoxResults() {
        return roxResults;
    }

    public void setRoxResults(String roxResults) {
        this.roxResults = roxResults;
    }

    public String getFamResults() {
        return famResults;
    }

    public void setFamResults(String famResults) {
        this.famResults = famResults;
    }

    public String getHexResults() {
        return hexResults;
    }

    public void setHexResults(String hexResults) {
        this.hexResults = hexResults;
    }

    public String getCy5Results() {
        return cy5Results;
    }

    public void setCy5Results(String cy5Results) {
        this.cy5Results = cy5Results;
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
}
