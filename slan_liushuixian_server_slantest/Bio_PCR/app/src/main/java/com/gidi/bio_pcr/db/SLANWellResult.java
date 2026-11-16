package com.gidi.bio_pcr.db;

import com.litesuits.orm.db.annotation.Column;
import com.litesuits.orm.db.annotation.NotNull;
import com.litesuits.orm.db.annotation.Table;

import java.io.Serializable;

@Table("SLANWellResult")//反应孔标识
public class SLANWellResult extends BaseModel implements Serializable {
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
     * 实验名称模板名称
     */

    @Column("experimentName")
    private String experimentName;//8个孔一样的批次

    /**
     * 测试开始时间
     */

    @NotNull
    @Column("startTime")
    private String startTime;

    /**
     * 测试结束时间
     */

    @Column("endTime")
    private String endTime;

    @NotNull
    @Column("wellName")
    private String wellName;//反应孔标识

    @Column("wellConclusion")
    private String wellConclusion;//样本结论
    @Column("peakWellConclusion")
    private String peakWellConclusion;

    @Column("roxTargetResults")
    public String roxTargetResults;

    @Column("hexTargetResults")
    public String hexTargetResults;

    @Column("famTargetResults")
    public String famTargetResults;

    @Column("cy5TargetResults")
    public String cy5TargetResults;
    @Column("quasarTargetResults")
    public String quasarTargetResults;
    @Column("attoTargetResults")
    public String attoTargetResults;

    @Column("roxResults")
    private String roxResults;

    @Column("famResults")
    private String famResults;

    @Column("hexResults")
    private String hexResults;

    @Column("cy5Results")
    private String cy5Results;

    @Column("quasarResults")
    private String quasarResults;
    @Column("attoRessults")
    private String attoResults;

    @Column("famPeakResults")
    private String famPeakResults;
    @Column("hexPeakResults")
    private String hexPeakResults;
    @Column("roxPeakResults")
    private String roxPeakResults;
    @Column("cy5PeakResults")
    private String cy5PeakResults;
    @Column("quasarPeakResults")
    private String quasarPeakResults;
    @Column("attoPeakResults")
    private String attoPeakResults;

    @Column("isMelt")
    private boolean isMelt;//定性。定量，熔解曲线

    @Column("isAutoTh")
    private boolean isAutoTh;//自动阈值

    @Column("famThreshold")
    private String famThreshold;
    @Column("hexThreshold")
    private String hexThreshold;
    @Column("roxThreshold")
    private String roxThreshold;
    @Column("cy5Threshold")
    private String cy5Threshold;
    @Column("quasarThreshold")
    private String quasarThreshold;
    @Column("attoThreshold")
    private String attoThreshold;

    @Column("famPeekThreshold")
    private String famPeekThreshold;
    @Column("hexPeekThreshold")
    private String hexPeekThreshold;
    @Column("roxPeekThreshold")
    private String roxPeekThreshold;
    @Column("cy5PeekThreshold")
    private String cy5PeekThreshold;
    @Column("quasarPeekThreshold")
    private String quasarPeekThreshold;
    @Column("attoPeekThreshold")
    private String attoPeekThreshold;

    private boolean isShow = false;
    private boolean isCheck = false;

    public String getReagentCode() {
        return reagentCode;
    }

    public void setReagentCode(String reagentCode) {
        this.reagentCode = reagentCode;
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

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getWellName() {
        return wellName;
    }

    public void setWellName(String wellName) {
        this.wellName = wellName;
    }

    public String getWellConclusion() {
        return wellConclusion;
    }

    public void setWellConclusion(String wellConclusion) {
        this.wellConclusion = wellConclusion;
    }

    public String getHexTargetResults() {
        return hexTargetResults;
    }

    public void setHexTargetResults(String hexTargetResults) {
        this.hexTargetResults = hexTargetResults;
    }

    public String getFamTargetResults() {
        return famTargetResults;
    }

    public void setFamTargetResults(String famTargetResults) {
        this.famTargetResults = famTargetResults;
    }

    public String getCy5TargetResults() {
        return cy5TargetResults;
    }

    public void setCy5TargetResults(String cy5TargetResults) {
        this.cy5TargetResults = cy5TargetResults;
    }

    public String getRoxTargetResults() {
        return roxTargetResults;
    }

    public void setRoxTargetResults(String roxTargetResults) {
        this.roxTargetResults = roxTargetResults;
    }

    public String getQuasarTargetResults() {
        return quasarTargetResults;
    }

    public void setQuasarTargetResults(String quasarTargetResults) {
        this.quasarTargetResults = quasarTargetResults;
    }

    public String getAttoTargetResults() {
        return attoTargetResults;
    }

    public void setAttoTargetResults(String attoTargetResults) {
        this.attoTargetResults = attoTargetResults;
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

    public String getQuasarResults() {
        return quasarResults;
    }

    public void setQuasarResults(String quasarResults) {
        this.quasarResults = quasarResults;
    }

    public String getAttoResults() {
        return attoResults;
    }

    public void setAttoResults(String attoResults) {
        this.attoResults = attoResults;
    }

    public String getFamPeakResults() {
        return famPeakResults;
    }

    public void setFamPeakResults(String famPeakResults) {
        this.famPeakResults = famPeakResults;
    }

    public String getHexPeakResults() {
        return hexPeakResults;
    }

    public void setHexPeakResults(String hexPeakResults) {
        this.hexPeakResults = hexPeakResults;
    }

    public String getRoxPeakResults() {
        return roxPeakResults;
    }

    public void setRoxPeakResults(String roxPeakResults) {
        this.roxPeakResults = roxPeakResults;
    }

    public String getCy5PeakResults() {
        return cy5PeakResults;
    }

    public void setCy5PeakResults(String cy5PeakResults) {
        this.cy5PeakResults = cy5PeakResults;
    }

    public String getQuasarPeakResults() {
        return quasarPeakResults;
    }

    public void setQuasarPeakResults(String quasarPeakResults) {
        this.quasarPeakResults = quasarPeakResults;
    }

    public String getAttoPeakResults() {
        return attoPeakResults;
    }

    public void setAttoPeakResults(String attoPeakResults) {
        this.attoPeakResults = attoPeakResults;
    }

    public boolean isMelt() {
        return isMelt;
    }

    public void setMelt(boolean melt) {
        isMelt = melt;
    }

    public boolean isAutoTh() {
        return isAutoTh;
    }

    public void setAutoTh(boolean autoTh) {
        isAutoTh = autoTh;
    }

    public String getFamThreshold() {
        return famThreshold;
    }

    public void setFamThreshold(String famThreshold) {
        this.famThreshold = famThreshold;
    }

    public String getHexThreshold() {
        return hexThreshold;
    }

    public void setHexThreshold(String hexThreshold) {
        this.hexThreshold = hexThreshold;
    }

    public String getRoxThreshold() {
        return roxThreshold;
    }

    public void setRoxThreshold(String roxThreshold) {
        this.roxThreshold = roxThreshold;
    }

    public String getCy5Threshold() {
        return cy5Threshold;
    }

    public void setCy5Threshold(String cy5Threshold) {
        this.cy5Threshold = cy5Threshold;
    }

    public String getQuasarThreshold() {
        return quasarThreshold;
    }

    public void setQuasarThreshold(String quasarThreshold) {
        this.quasarThreshold = quasarThreshold;
    }

    public String getFamPeekThreshold() {
        return famPeekThreshold;
    }

    public void setFamPeekThreshold(String famPeekThreshold) {
        this.famPeekThreshold = famPeekThreshold;
    }

    public String getHexPeekThreshold() {
        return hexPeekThreshold;
    }

    public void setHexPeekThreshold(String hexPeekThreshold) {
        this.hexPeekThreshold = hexPeekThreshold;
    }

    public String getRoxPeekThreshold() {
        return roxPeekThreshold;
    }

    public void setRoxPeekThreshold(String roxPeekThreshold) {
        this.roxPeekThreshold = roxPeekThreshold;
    }

    public String getCy5PeekThreshold() {
        return cy5PeekThreshold;
    }

    public void setCy5PeekThreshold(String cy5PeekThreshold) {
        this.cy5PeekThreshold = cy5PeekThreshold;
    }

    public String getQuasarPeekThreshold() {
        return quasarPeekThreshold;
    }

    public void setQuasarPeekThreshold(String quasarPeekThreshold) {
        this.quasarPeekThreshold = quasarPeekThreshold;
    }

    public String getAttoPeekThreshold() {
        return attoPeekThreshold;
    }

    public void setAttoPeekThreshold(String attoPeekThreshold) {
        this.attoPeekThreshold = attoPeekThreshold;
    }

    public String getAttoThreshold() {
        return attoThreshold;
    }

    public void setAttoThreshold(String attoThreshold) {
        this.attoThreshold = attoThreshold;
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

    public String getPeakWellConclusion() {
        return peakWellConclusion;
    }

    public void setPeakWellConclusion(String peakWellConclusion) {
        this.peakWellConclusion = peakWellConclusion;
    }
}
