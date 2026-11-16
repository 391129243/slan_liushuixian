package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

public class PCRReqMessage extends BaseMessage {

    private String Template;

    private int Force;

    /// /用于指示是否强制执行   0  不强制  1 强制执行

    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String template) {
        Template = template;
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
}