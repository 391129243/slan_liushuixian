package com.gzlab.autoassemblyline_client.autopipeline.message;



import com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Well;
import com.gzlab.autoassemblyline_client.autopipeline.message.base.BinBaseMessage;

import java.util.ArrayList;

/**
 * PCR下发模板请求消息
 */
public class SLAN_PCRReqMessage extends BinBaseMessage {

    private String Template;

    private int CurrentPhase;

    private int CurrentCycle;

    private String CurrentSection;

    private String CurrentStep;

    private int RunningTime;

    private int TotalTime;

    private String BatchID = "";

    private com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment Experiment;

    private ArrayList<Well> wells;

    private int Force;

    public ArrayList<Well> getWells() {
        return wells;
    }

    public void setWells(ArrayList<Well> wells) {
        this.wells = wells;
    }

    ////用于指示是否强制执行   0  不强制  1 强制执行




    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String template) {
        Template = template;
    }

    public int getCurrentPhase() {
        return CurrentPhase;
    }

    public void setCurrentPhase(int currentPhase) {
        CurrentPhase = currentPhase;
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

    public int getTotalTime() {
        return TotalTime;
    }

    public void setTotalTime(int totalTime) {
        TotalTime = totalTime;
    }

    public String getBatchID() {
        return BatchID;
    }

    public void setBatchID(String batchID) {
        BatchID = batchID;
    }

    public int getForce() {
        return Force;
    }

    public void setForce(int force) {
        Force = force;
    }

    public com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment getExperiment() {
        return Experiment;
    }

    public void setExperiment(com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment experiment) {
        Experiment = experiment;
    }

    public SLAN_PCRReqMessage() {
        super();
    }
}
