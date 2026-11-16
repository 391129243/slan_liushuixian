package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

/**
 * PCR设备状态消息对应的是宏石的DeviceState
 */
public class PCRStatusMessage extends BaseMessage {

    private String Status;
    private String LinuxStatus;/////running、free、error

    private int CurrentCycle;
    private String CurrentSection;
    private String CurrentStep;
    private int RunningTime;
    private int WaitingTime;
    private int LeftTime;
    private int TotalTime;
    private double Degree;
    private double DegreeTarget;
    private String Info;
    private String Alarm;
    private String LinuxAlarm;/////字符信息描述上位机状态，影响模板运行的LinuxStatus应设置成error
    private String Template;

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

    public double getDegreeTarget() {
        return DegreeTarget;
    }

    public void setDegreeTarget(double degreeTarget) {
        DegreeTarget = degreeTarget;
    }
}
