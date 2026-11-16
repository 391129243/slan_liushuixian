package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

/**
 * 重启 App 指令的回复
 * Reply: "ok" 即将重启；"error" 拒绝重启，原因见 Reason
 */
public class RestartPCRAppReplyMessage extends BaseMessage {

    private String Reply;

    private String Reason;

    public RestartPCRAppReplyMessage() {
        super();
    }

    public String getReply() {
        return Reply;
    }

    public void setReply(String reply) {
        Reply = reply;
    }

    public String getReason() {
        return Reason;
    }

    public void setReason(String reason) {
        Reason = reason;
    }
}
