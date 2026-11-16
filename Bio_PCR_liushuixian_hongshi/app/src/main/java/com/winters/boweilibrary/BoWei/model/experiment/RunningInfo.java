package com.winters.boweilibrary.BoWei.model.experiment;


import com.gidi.bio_pcr.util.NumberUtils;
import com.winters.boweilibrary.BoWei.bioweyenum.ProjectType;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningError;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningState;

public class RunningInfo {
    private RunningState state;//运行状态枚举
    private String CurrentRunning;//运行状态信息
    private RunningError Error;//错误状态枚举
    private String CurrentError;//运行错误信息
    private Long RemainTime;//剩余时间
    private Long RunningTime;//运行时间
    private String Segment;//正在运行的程序段
    private String Step;//正在运行的步骤
    private Double HotlidSetTemperature;//热盖设置温度
    private Double HotlidTemperature;//热盖温度
    private Double TubeTemperature;//试管温度
    private String SetTemperature;//当前設置温度

    private String  BlockTemperature1;//半导体的温度

    private String  BlockTemperature2;
    private String  BlockTemperature3;

    private int Cycle;//当前程序段已经运行到第几个循环
    private String HoldTime;//当前步骤保持时间
    private ProjectType Type;//项目类型枚举
    private String TypeName;//项目类型名称

    public RunningState getState() {
        return state;
    }

    public void setState(RunningState state) {
        this.state = state;
    }

    public String getCurrentRunning() {
        return CurrentRunning;
    }

    public void setCurrentRunning(String currentRunning) {
        CurrentRunning = currentRunning;
    }

    public RunningError getError() {
        return Error;
    }

    public void setError(RunningError error) {
        Error = error;
    }

    public String getCurrentError() {
        return CurrentError;
    }

    public void setCurrentError(String currentError) {
        CurrentError = currentError;
    }


    public Long getRemainTime() {
        return RemainTime;
    }

    public void setRemainTime(Long remainTime) {
        RemainTime = remainTime;
    }

    public Long getRunningTime() {
        return RunningTime;
    }

    public void setRunningTime(Long runningTime) {
        RunningTime = runningTime;
    }

    public String getSegment() {
        return Segment;
    }

    public void setSegment(String segment) {
        Segment = segment;
    }

    public String getStep() {
        return Step;
    }

    public void setStep(String step) {
        Step = step;
    }

    public Double getHotlidSetTemperature() {
        return HotlidSetTemperature;
    }

    public void setHotlidSetTemperature(Double hotlidSetTemperature) {
        HotlidSetTemperature = hotlidSetTemperature;
    }

    public Double getHotlidTemperature() {
        return HotlidTemperature;
    }

    public void setHotlidTemperature(Double hotlidTemperature) {
        HotlidTemperature = hotlidTemperature;
    }

    public Double getTubeTemperature() {
        return TubeTemperature;
    }

    public void setTubeTemperature(Double tubeTemperature) {
        TubeTemperature = tubeTemperature;
    }

    public String getSetTemperature() {
        return SetTemperature;
    }

    public void setSetTemperature(String setTemperature) {
        SetTemperature = setTemperature;
    }

    public int getCycle() {
        return Cycle;
    }

    public void setCycle(int cycle) {
        Cycle = cycle;
    }

    public String getHoldTime() {
        return HoldTime;
    }

    public void setHoldTime(String holdTime) {
        HoldTime = holdTime;
    }

    public ProjectType getType() {
        return Type;
    }

    public void setType(ProjectType type) {
        Type = type;
    }

    public String getTypeName() {
        return TypeName;
    }

    public void setTypeName(String typeName) {
        TypeName = typeName;
    }

    public String getBlockTemperature1() {
        return BlockTemperature1;
    }

    public void setBlockTemperature1(String blockTemperature1) {
        BlockTemperature1 = blockTemperature1;
    }

    public String getBlockTemperature2() {
        return BlockTemperature2;
    }

    public void setBlockTemperature2(String blockTemperature2) {
        BlockTemperature2 = blockTemperature2;
    }

    public String getBlockTemperature3() {
        return BlockTemperature3;
    }

    public void setBlockTemperature3(String blockTemperature3) {
        BlockTemperature3 = blockTemperature3;
    }



}
