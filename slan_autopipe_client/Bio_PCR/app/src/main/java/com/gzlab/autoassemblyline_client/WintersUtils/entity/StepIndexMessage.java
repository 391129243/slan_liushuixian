package com.gzlab.autoassemblyline_client.WintersUtils.entity;

public class StepIndexMessage {

    private int stepIndex;

    private int stepTotal;

    public StepIndexMessage(int stepIndex, int stepTotal) {
        this.stepIndex = stepIndex;
        this.stepTotal = stepTotal;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public int getStepTotal() {
        return stepTotal;
    }

    public void setStepTotal(int stepTotal) {
        this.stepTotal = stepTotal;
    }
}
