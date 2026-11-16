package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

/**
 * PCR运行状态消息上报
 */
public class PCRRunningStatusMessage extends BaseMessage {

    private String Status;
    private String LinuxStatus;/////running、free、error
    private int CurrentCycle;//当前循环
    private String CurrentSection;//当前程序段
    private String CurrentStep;//当前程序步骤
    private int RemainTime;///单步骤剩余时间 对应当前步骤保持时间HoldTime
    private int RunningTime;///整体已运行时间//对应RunningTime
    private int WaitingTime;///单步骤等待时间
    private int LeftTime;///整体剩余时间//对应RemainTime
    private int TotalTime;///整体预估总时间
    private double Degree;///实时温度
    private double DegreeTarget;///当前设置的温度SetTemperature
    private String Info;
    private String Alarm;
    private String LinuxAlarm;/////字符信息描述上位机状态，影响模板运行的LinuxStatus应设置成error
    private String Template;//当前运行的模板名称

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public int getCurrentCycle() {
        return CurrentCycle;
    }

    public void setCurrentCycle(int currentCycle) {
        CurrentCycle = currentCycle;
    }


    public String getCurrentSection() {
        return CurrentSection;
    }

    public void setCurrentSection(String currentSection) {
        CurrentSection = currentSection;
    }

    public String getCurrentStep() {
        return CurrentStep;
    }

    public void setCurrentStep(String currentStep) {
        CurrentStep = currentStep;
    }

    public int getRunningTime() {
        return RunningTime;
    }

    public void setRunningTime(int runningTime) {
        RunningTime = runningTime;
    }

    public int getWaitingTime() {
        return WaitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        WaitingTime = waitingTime;
    }

    public int getLeftTime() {
        return LeftTime;
    }

    public void setLeftTime(int leftTime) {
        LeftTime = leftTime;
    }

    public int getTotalTime() {
        return TotalTime;
    }

    public void setTotalTime(int totalTime) {
        TotalTime = totalTime;
    }

    public double getDegree() {
        return Degree;
    }

    public void setDegree(double degree) {
        Degree = degree;
    }

    public String getInfo() {
        return Info;
    }

    public void setInfo(String info) {
        Info = info;
    }

    public String getAlarm() {
        return Alarm;
    }

    public void setAlarm(String alarm) {
        Alarm = alarm;
    }

    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String template) {
        Template = template;
    }

    public String getLinuxStatus() {
        return LinuxStatus;
    }

    public void setLinuxStatus(String linuxStatus) {
        LinuxStatus = linuxStatus;
    }

    public String getLinuxAlarm() {
        return LinuxAlarm;
    }

    public void setLinuxAlarm(String linuxAlarm) {
        LinuxAlarm = linuxAlarm;
    }

    public int getRemainTime() {
        return RemainTime;
    }

    public void setRemainTime(int remainTime) {
        RemainTime = remainTime;
    }

    public double getDegreeTarget() {
        return DegreeTarget;
    }

    public void setDegree_Target(double degreeTarget) {
        DegreeTarget = degreeTarget;
    }
}
