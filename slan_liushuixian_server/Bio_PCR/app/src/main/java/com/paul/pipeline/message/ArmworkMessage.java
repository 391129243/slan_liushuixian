package com.paul.pipeline.message;

import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class ArmworkMessage {

    private int status;
    private int index;
    private int type;//arm 机械臂工作
    private int source;
    private int target;
    private int sourcetype;
    private int targettype;
    private int dispatchindex;
    private String template;
    private String project;
    private String batch;
    private int tray;
    private int armerror;
    private String armerrortext;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public int getArmerror() {
        return armerror;
    }

    public void setArmerror(int armerror) {
        this.armerror = armerror;
    }

    public String getArmerrortext() {
        return armerrortext;
    }

    public void setArmerrortext(String armerrortext) {
        this.armerrortext = armerrortext;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public int getDispatchindex() {
        return dispatchindex;
    }

    public void setDispatchindex(int dispatchindex) {
        this.dispatchindex = dispatchindex;
    }

    public int getSourcetype() {
        return sourcetype;
    }

    public void setSourcetype(int sourcetype) {
        this.sourcetype = sourcetype;
    }

    public int getTargettype() {
        return targettype;
    }

    public void setTargettype(int targettype) {
        this.targettype = targettype;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getTray() {
        return tray;
    }

    public void setTray(int tray) {
        this.tray = tray;
    }
}
