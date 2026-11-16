package com.winters.boweilibrary.autopipeline.model.message;

public class QueryDeviceReplyMessage extends BaseMessage {

    private String Reply;

    public String getReply() {
        return Reply;
    }

    public void setReply(String reply) {
        Reply = reply;
    }

    public QueryDeviceReplyMessage() {
        super();
    }

    public QueryDeviceReplyMessage(String type, int seqId, String deviceId, String deviceType, int position, String reply) {
        super(type, seqId, deviceId, deviceType, position);
        Reply = reply;
    }
}
