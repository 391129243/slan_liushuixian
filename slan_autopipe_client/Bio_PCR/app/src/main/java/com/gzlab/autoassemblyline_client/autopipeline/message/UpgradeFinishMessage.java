package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

public class UpgradeFinishMessage extends BaseMessage {
//    升级成功将自动重启，重启后通过HeartBeat心跳上报新的版本号。
//    重启失败将自动回退至旧版本启动。

//    "Type":"UpgradeFinish"

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

    public UpgradeFinishMessage(String reply, String error) {
        Reply = reply;
        Error = error;
    }

    public UpgradeFinishMessage(String type, int seqId, String deviceID, String deviceType, int position, String reply, String error) {
        super(type, seqId, deviceID, deviceType, position);
        Reply = reply;
        Error = error;
    }
}
