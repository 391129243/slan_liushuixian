package com.gidi.bio_pcr.WintersUtils.entity.event;

/**
 * 程序完成动作
 */
public class ProcedureFinishEvent {

    private String status;

    private String templateName;

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

    /** 本次运行的序号（RNAAndPCRAsyncTask 启动时分配），0 表示未知/旧调用方 */
    private long runId = 0;

    public long getRunId() {
        return runId;
    }

    public ProcedureFinishEvent(String status, String templateName) {
        this.status = status;
        this.templateName = templateName;
    }

    public ProcedureFinishEvent(String status, String templateName, long runId) {
        this.status = status;
        this.templateName = templateName;
        this.runId = runId;
    }
}
