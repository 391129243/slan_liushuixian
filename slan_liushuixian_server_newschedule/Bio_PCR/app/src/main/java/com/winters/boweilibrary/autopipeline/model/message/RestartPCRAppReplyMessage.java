package com.winters.boweilibrary.autopipeline.model.message;

/**
 * SLANPCR client 对 RestartPCRApp 的回复
 * Reply: "ok" 表示即将重启；"error" 表示拒绝，原因见 Reason
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
