package com.winters.boweilibrary.autopipeline.model.message.PCRMessage;

import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class PCRReqMessage extends BaseMessage {

    private String Template;

    private Experiment Experiment;

    private int CurrentPhase;

    private int CurrentCycle;

    private String CurrentSection;

    private String CurrentStep;

    private int RunningTime;

    private int TotalTime;

    private String BatchID ;

    private int Force;  ////用于指示是否强制执行   0  不强制  1 强制执行

    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String template) {
        Template = template;
    }

    public Experiment getExperiment() {
        return Experiment;
    }

    public void setExperiment(Experiment experiment) {
        Experiment = experiment;
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

    public int isForce() {
        return Force;
    }

    public int getForce() {
        return Force;
    }

    public void setForce(int force) {
        Force = force;
    }

    public String getBatchID() {
        return BatchID;
    }

    public void setBatchID(String batchID) {
        BatchID = batchID;
    }
}
