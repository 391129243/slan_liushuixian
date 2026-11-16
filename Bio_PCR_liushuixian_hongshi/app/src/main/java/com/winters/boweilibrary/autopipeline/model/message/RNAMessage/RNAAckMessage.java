package com.winters.boweilibrary.autopipeline.model.message.RNAMessage;

import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class RNAAckMessage extends BaseMessage {

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
