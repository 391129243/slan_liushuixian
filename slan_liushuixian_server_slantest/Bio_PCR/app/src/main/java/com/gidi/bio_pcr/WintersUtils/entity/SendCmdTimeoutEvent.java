package com.gidi.bio_pcr.WintersUtils.entity;

public class SendCmdTimeoutEvent {

    private ByteBuilder msgPacket;

    public SendCmdTimeoutEvent(ByteBuilder msgPacket) {
        this.msgPacket = msgPacket;
    }

    public ByteBuilder getMsgPacket() {
        return msgPacket;
    }

    public void setMsgPacket(ByteBuilder msgPacket) {
        this.msgPacket = msgPacket;
    }
}
