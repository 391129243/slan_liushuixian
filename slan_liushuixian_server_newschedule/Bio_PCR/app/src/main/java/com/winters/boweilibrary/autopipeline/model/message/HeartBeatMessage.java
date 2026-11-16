package com.winters.boweilibrary.autopipeline.model.message;

public class HeartBeatMessage  extends BaseMessage {

    private String Time;

    public String getTime() {
        return Time;
    }

    public void setTime(String time) {
        Time = time;
    }
}
