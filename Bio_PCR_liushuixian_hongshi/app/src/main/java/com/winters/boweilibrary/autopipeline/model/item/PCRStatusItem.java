package com.winters.boweilibrary.autopipeline.model.item;

import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;

public class PCRStatusItem extends WSSConnectionItem{


    private String pCRStatus = "";
    private String pCRLinuxStatus = "";
    private String pCRLinuxAlarm = "";
    private double pCRDegree;
    private double DegreeTarget;
    private String pCRAlarm;
    private String  pCRSendAlarm = "";
    //正在反应的托盘，调度时需要设置并记忆到sharedperference
    private int pCRCurrentTray;
    private String pCRCurrentTemplate = "";
    private String pCRCurrentSection = "";
    private String pCRCurrentStep = "";
    private int pCRCurrentCycle;
    private int pCRTotalCycle;
    private String Info = "";
    private int pCRTotalTime;
    private int pCRLeftTime;
    private int pCRRunningTime;
    private int pCRWaitTime;

    private Experiment Experiment;

    private com.hongshi.slansdk.model.experiment.Experiment slanExperiment;

    public com.hongshi.slansdk.model.experiment.Experiment getSlanExperiment() {
        return slanExperiment;
    }

    public void setSlanExperiment(com.hongshi.slansdk.model.experiment.Experiment slanExperiment) {
        this.slanExperiment = slanExperiment;
    }

    public String getpCRStatus() {
        return pCRStatus;
    }

    public void setpCRStatus(String pCRStatus) {
        this.pCRStatus = pCRStatus;
    }

    public double getpCRDegree() {
        return pCRDegree;
    }

    public void setpCRDegree(double pCRDegree) {
        this.pCRDegree = pCRDegree;
    }

    public String getpCRAlarm() {
        return pCRAlarm;
    }

    public void setpCRAlarm(String pCRAlarm) {
        this.pCRAlarm = pCRAlarm;
    }

    public int getpCRCurrentTray() {
        return pCRCurrentTray;
    }

    public void setpCRCurrentTray(int pCRCurrentTray) {
        this.pCRCurrentTray = pCRCurrentTray;
    }

    public String getpCRCurrentTemplate() {
        return pCRCurrentTemplate;
    }

    public void setpCRCurrentTemplate(String pCRCurrentTemplate) {
        this.pCRCurrentTemplate = pCRCurrentTemplate;
    }

    public String getpCRCurrentSection() {
        return pCRCurrentSection;
    }

    public void setpCRCurrentSection(String pCRCurrentSection) {
        this.pCRCurrentSection = pCRCurrentSection;
    }

    public String getpCRCurrentStep() {
        return pCRCurrentStep;
    }

    public void setpCRCurrentStep(String pCRCurrentStep) {
        this.pCRCurrentStep = pCRCurrentStep;
    }

    public int getpCRCurrentCycle() {
        return pCRCurrentCycle;
    }

    public void setpCRCurrentCycle(int pCRCurrentCycle) {
        this.pCRCurrentCycle = pCRCurrentCycle;
    }

    public int getpCRTotalCycle() {
        return pCRTotalCycle;
    }

    public void setpCRTotalCycle(int pCRTotalCycle) {
        this.pCRTotalCycle = pCRTotalCycle;
    }

    public String getpCRSendAlarm() {
        return pCRSendAlarm;
    }

    public void setpCRSendAlarm(String pCRSendAlarm) {
        this.pCRSendAlarm = pCRSendAlarm;
    }

    public String getInfo() {
        return Info;
    }

    public void setInfo(String info) {
        Info = info;
    }

    public int getpCRTotalTime() {
        return pCRTotalTime;
    }

    public void setpCRTotalTime(int pCRTotalTime) {
        this.pCRTotalTime = pCRTotalTime;
    }

    public int getpCRLeftTime() {
        return pCRLeftTime;
    }

    public void setpCRLeftTime(int pCRLeftTime) {
        this.pCRLeftTime = pCRLeftTime;
    }

    public int getpCRRunningTime() {
        return pCRRunningTime;
    }

    public void setpCRRunningTime(int pCRRunningTime) {
        this.pCRRunningTime = pCRRunningTime;
    }

    public int getpCRWaitTime() {
        return pCRWaitTime;
    }

    public void setpCRWaitTime(int pCRWaitTime) {
        this.pCRWaitTime = pCRWaitTime;
    }

    public String getpCRLinuxStatus() {
        return pCRLinuxStatus;
    }

    public void setpCRLinuxStatus(String pCRLinuxStatus) {
        this.pCRLinuxStatus = pCRLinuxStatus;
    }

    public String getpCRLinuxAlarm() {
        return pCRLinuxAlarm;
    }

    public void setpCRLinuxAlarm(String pCRLinuxAlarm) {
        this.pCRLinuxAlarm = pCRLinuxAlarm;
    }

    public Experiment getExperiment() {
        return Experiment;
    }

    public void setExperiment(Experiment experiment) {
        Experiment = experiment;
    }

    public double getDegreeTarget() {
        return DegreeTarget;
    }

    public void setDegreeTarget(double degreeTarget) {
        DegreeTarget = degreeTarget;
    }
}
