package com.winters.boweilibrary.autopipeline.model.message;

public class HeartBeatReplyMessage extends BaseMessage {

    private String Version;

    public String getVersion() {
        return Version;
    }

    public void setVersion(String version) {
        Version = version;
    }

    public HeartBeatReplyMessage() {
        super();
    }
}
