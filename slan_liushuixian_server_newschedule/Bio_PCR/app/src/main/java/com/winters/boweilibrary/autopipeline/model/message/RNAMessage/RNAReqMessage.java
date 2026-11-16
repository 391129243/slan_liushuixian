package com.winters.boweilibrary.autopipeline.model.message.RNAMessage;

import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

import java.util.List;

public class RNAReqMessage extends BaseMessage {

    private String Template;

    private List<HexDebugEntity> WorkTemplate;

    private int CurrentPhase;

    private int CurrentPhaseStep;

    private String BatchID = "";

    private int TotalTime = 0;

    private String MSGTime = "";

    private int force = 0;////用于指示是否强制执行   0  不强制  1 强制执行  //不管任何状态和处于下发模板的状态

    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String template) {
        Template = template;
    }

    public List<HexDebugEntity>  getWorkTemplate() {
        return WorkTemplate;
    }

    public void setWorkTemplate(List<HexDebugEntity>  workTemplate) {
        WorkTemplate = workTemplate;
    }

    public String getBatchID() {
        return BatchID;
    }

    public void setBatchID(String batchID) {
        BatchID = batchID;
    }

    public int getTotalTime() {
        return TotalTime;
    }

    public void setTotalTime(int totalTime) {
        TotalTime = totalTime;
    }



    public int getCurrentPhase() {
        return CurrentPhase;
    }

    public void setCurrentPhase(int currentPhase) {
        CurrentPhase = currentPhase;
    }

    public int getCurrentPhaseStep() {
        return CurrentPhaseStep;
    }

    public void setCurrentPhaseStep(int currentPhaseStep) {
        CurrentPhaseStep = currentPhaseStep;
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public String getMSGTime() {
        return MSGTime;
    }

    public void setMSGTime(String MSGTime) {
        this.MSGTime = MSGTime;
    }
}
