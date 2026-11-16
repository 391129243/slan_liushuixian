package com.winters.boweilibrary.autopipeline.model.message.PCRMessage;

import com.winters.boweilibrary.autopipeline.model.message.BinBaseMessage;

public class SLAN_PCRFinishMessage extends BinBaseMessage
{


    private String Status;
    private int CurrentCycle;
    private int TotalCycle;
    private String CurrentSection;
    private String CurrentStep;
    private int RunningTime;
    private int WaitingTime;
    private int LeftTime;
    private int TotalTime;
    private double Degree;
    private String Info;
    private String Alarm;
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

    public int getTotalCycle() {
        return TotalCycle;
    }

    public void setTotalCycle(int totalCycle) {
        TotalCycle = totalCycle;
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

    public SLAN_PCRFinishMessage() {
        super();
    }
}
