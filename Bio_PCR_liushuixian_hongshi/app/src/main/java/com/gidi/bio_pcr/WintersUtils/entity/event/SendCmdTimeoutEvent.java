package com.gidi.bio_pcr.WintersUtils.entity.event;

import com.gidi.bio_pcr.WintersUtils.entity.ByteBuilder;

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
