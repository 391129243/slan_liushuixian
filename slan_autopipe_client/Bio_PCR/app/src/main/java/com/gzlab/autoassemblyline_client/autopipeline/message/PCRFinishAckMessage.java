package com.gzlab.autoassemblyline_client.autopipeline.message;

import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;

public class PCRFinishAckMessage extends BaseMessage {

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
