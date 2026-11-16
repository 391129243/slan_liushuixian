package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

public class UpgradeAckMessage extends BaseMessage {

//    上位机收到升级的WSS消息后，解析包头，获取JSON结构化描述信息长度，读取JSON结构化信息并解析，获取升级版本信息。读取版本文件，并进行MD5校验后回复Ack信息。
//    "Type":"UpgradeAck"

    private String Reply;

    private String Error;


    public String getReply() {
        return Reply;
    }

    public void setReply(String reply) {
        Reply = reply;
    }

    public String getError() {
        return Error;
    }

    public void setError(String error) {
        Error = error;
    }

    public UpgradeAckMessage() {
        super();
    }

    public UpgradeAckMessage(String reply, String error) {
        Reply = reply;
        Error = error;
    }

    public UpgradeAckMessage(String type, int seqId, String deviceID, String deviceType, int position, String reply, String error) {
        super(type, seqId, deviceID, deviceType, position);
        Reply = reply;
        Error = error;
    }
}
