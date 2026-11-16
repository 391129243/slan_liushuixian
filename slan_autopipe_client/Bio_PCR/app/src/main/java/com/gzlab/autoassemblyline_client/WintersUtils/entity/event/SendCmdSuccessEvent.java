package com.gzlab.autoassemblyline_client.WintersUtils.entity.event;

import com.gzlab.autoassemblyline_client.WintersUtils.entity.ByteBuilder;

public class SendCmdSuccessEvent {

    private ByteBuilder msgPacket;

    public SendCmdSuccessEvent(ByteBuilder msgPacket) {
        this.msgPacket = msgPacket;
    }

    public ByteBuilder getMsgPacket() {
        return msgPacket;
    }

    public void setMsgPacket(ByteBuilder msgPacket) {
        this.msgPacket = msgPacket;
    }
}
