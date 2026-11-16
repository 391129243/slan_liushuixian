package com.gzlab.autoassemblyline_client.WintersUtils.entity.procedureEntity;

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

    public ProcedureFinishEvent(String status, String templateName) {
        this.status = status;
        this.templateName = templateName;
    }
}
