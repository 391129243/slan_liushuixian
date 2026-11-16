package com.winters.boweilibrary.autopipeline.model.message.RNAMessage;

import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class RNAFinishAckMessage extends BaseMessage {

//    {
//        "Type": "RNAFinnishAck",
//            "SeqId": 1,
//            "DeviceID": "2740835",
//            "DeviceType: "XXXX"，
//        "Position": "1" ////用于上位机返回自己记录的设定位置
//        "Reply": "OK",
//    }

    private String Reply;

    public String getReply() {
        return Reply;
    }

    public void setReply(String reply) {
        Reply = reply;
    }
}
