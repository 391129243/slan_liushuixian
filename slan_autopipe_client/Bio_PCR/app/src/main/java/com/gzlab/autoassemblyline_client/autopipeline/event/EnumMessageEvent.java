package com.gzlab.autoassemblyline_client.autopipeline.event;

public enum EnumMessageEvent {
    WSS_ServerClient(1,"onStart"),//新接入connection
    WSS_Connect(2,"onOpen"),//新接入connection
    WSS_CLOSE(3,"onClose"),//connection关闭
    WSS_Error(4,"onError"),//connection异常
    WSS_Message(5,"onMessage"),//connection接收到消息

    WSS_HeartBeat_Reply(100,"HeartBeatReply"),
    WSS_HeartBeat(101,"HeartBeat"),
    WSS_QueryDevice(102,"QueryDevice"),
    WSS_QueryDevice_Reply(103,"QueryDeviceReply"),
    WSS_PCR_STATUS(104,"PCRStatus"),//PCR状态：类似设备状态

    WSS_PCR_Req(105,"PCRReq"),
    WSS_PCR_Ack(106,"PCRAck"),
    WSS_PCR_Running_Status(107,"PCRRunningStatus"),
    WSS_PCR_Curve(108,"PCRCurve"),
    WSS_PCR_Finish(109,"PCRFinish"),
    //宏石PCR结束

    WSS_SLAN_REQ(110,"SLANPCRReq"),//模板请求
    WSS_SLAN_PCR_Ack(111,"PCRAck"),
    WSS_SLAN_PCR_RESET(112,"PCRReset"),//复位PCR
    WSS_SLAN_PCR_RESET_Finish(113,"PCRResetFinish"),//复位PCR
    WSS_PCR_SLAN_Finish(119,"SLANPCRFinish"),

    WSS_PCR_Finish_Ack(120,"PCRFinishAck"),

    WSS_PCR_STOP(121,"PCRStop"),

    WSS_SLAN_PCR_LOG(122,"SLANPCRLog"),

    WSS_RESTART_PCR_APP(130,"RestartPCRApp"),//主控下发重启App
    WSS_RESTART_PCR_APP_Reply(131,"RestartPCRAppReply"),

    WSS_UGRAGE(212,"Ugrade"),
    WSS_UGRAGE_Ack(213,"UgradeAck"),
    WSS_UGRAGE_Finish(214,"UgradeFinish"),

    WSSManager_PCR_REQ_UPDATE(200,"SLANPCR_Req_Update"),
    WSSManager_PCR_QUERYDEVICE(300,"SLANPCR_QueryDevice"),
    WSSManager_Update_PCR_Status(301,"UpdatePcrStatus");



    int code;
    String type;
    EnumMessageEvent(int code, String message){
        this.code = code;
        this.type = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
