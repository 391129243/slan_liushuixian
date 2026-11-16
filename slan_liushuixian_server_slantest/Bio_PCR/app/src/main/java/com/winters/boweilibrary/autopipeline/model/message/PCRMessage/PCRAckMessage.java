package com.winters.boweilibrary.autopipeline.model.message.PCRMessage;

import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class PCRAckMessage extends BaseMessage {

    private String Reply;

    private String Alarm;

    public String getReply() {
        return Reply;
    }

    public void setReply(String reply) {
        Reply = reply;
    }

    public String getAlarm() {
        return Alarm;
    }

    public void setAlarm(String alarm) {
        Alarm = alarm;
    }
}