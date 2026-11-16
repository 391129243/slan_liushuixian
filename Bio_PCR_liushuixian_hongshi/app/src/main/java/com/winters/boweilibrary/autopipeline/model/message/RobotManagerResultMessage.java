package com.winters.boweilibrary.autopipeline.model.message;

import androidx.work.WorkInfo;

public class RobotManagerResultMessage extends BaseMessage{

    private WorkInfo.State result;
    private String action;
    private String detail;

    public RobotManagerResultMessage(WorkInfo.State result, String action, String detail) {
        this.result = result;
        this.action = action;
        this.detail = detail;
    }

    public WorkInfo.State getResult() {
        return result;
    }

    public void setResult(WorkInfo.State result) {
        this.result = result;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

}
