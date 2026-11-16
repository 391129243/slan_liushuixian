package com.winters.boweilibrary.BoWei.event;


import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;

public class BoWeiSendCmdFailedEvent {

    private BoWeiPCRStepEntity msgPacket;

    public BoWeiSendCmdFailedEvent(BoWeiPCRStepEntity msgPacket) {
        this.msgPacket = msgPacket;
    }

    public BoWeiPCRStepEntity getMsgPacket() {
        return msgPacket;
    }

    public void setMsgPacket(BoWeiPCRStepEntity msgPacket) {
        this.msgPacket = msgPacket;
    }
}
