package com.winters.boweilibrary.BoWei.event;

import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;

public class BoWeiSendCmdTimeoutEvent {

    private BoWeiPCRStepEntity msgPacket;

    public BoWeiSendCmdTimeoutEvent(BoWeiPCRStepEntity msgPacket) {
        this.msgPacket = msgPacket;
    }

    public BoWeiPCRStepEntity getMsgPacket() {
        return msgPacket;
    }

    public void setMsgPacket(BoWeiPCRStepEntity msgPacket) {
        this.msgPacket = msgPacket;
    }
}
