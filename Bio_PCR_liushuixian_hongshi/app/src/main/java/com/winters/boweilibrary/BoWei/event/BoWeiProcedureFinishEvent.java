package com.winters.boweilibrary.BoWei.event;

import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;

public class BoWeiProcedureFinishEvent {

    private String status;

    private String templateName;

    private BoWeiPCRStepEntity boWeiPCRStepEntity;

    private Experiment experiment;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Experiment getExperiment() {
        return experiment;
    }

    public void setExperiment(Experiment experiment) {
        this.experiment = experiment;
    }

    public BoWeiPCRStepEntity getBoWeiPCRStepEntity() {
        return boWeiPCRStepEntity;
    }

    public void setBoWeiPCRStepEntity(BoWeiPCRStepEntity boWeiPCRStepEntity) {
        this.boWeiPCRStepEntity = boWeiPCRStepEntity;
    }

    public BoWeiProcedureFinishEvent(String status, String templateName) {
        this.status = status;
        this.templateName = templateName;
    }

    public BoWeiProcedureFinishEvent(String status, String templateName, BoWeiPCRStepEntity boWeiPCRStepEntity) {
        this.status = status;
        this.templateName = templateName;
        this.boWeiPCRStepEntity = boWeiPCRStepEntity;
    }

    public BoWeiProcedureFinishEvent(String status, String templateName, BoWeiPCRStepEntity boWeiPCRStepEntity, Experiment experiment) {
        this.status = status;
        this.templateName = templateName;
        this.boWeiPCRStepEntity = boWeiPCRStepEntity;
        this.experiment = experiment;
    }
}
