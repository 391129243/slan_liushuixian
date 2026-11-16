package com.winters.boweilibrary.autopipeline.enumtype;

public enum EnumMessageEvent {
    WSS_ServerStart(1,"onStart"),//新接入connection
    WSS_Connect(2,"onOpen"),//新接入connection
    WSS_Disconnect(3,"onClose"),//connection关闭
    WSS_Error(4,"onError"),//connection异常
    WSS_Message(5,"onMessage"),//connection接收到字符消息
    WSS_BinMessage(6,"onBinMessage"),//connection接收到二进制消息
    WSS_LOGMessage(7,"onLogMessage"),//connection接收到二进制消息


    WSSManager_UpdateConnection(50,"UpdateConnection"),//UpdateConnection更新Adapter


    WSSManager_UpdateRNAStatus(100,"UpdateRNAStatus"),//用于处理下发RNA模板的回复处理
    WSSManager_RNAAck_OK(101,"RNAAckOKMessage"),//用于处理下发RNA模板的正常ACK回复处理
    WSSManager_RNAAck_Error(102,"WSSManager_RNAAck_Error_AckMessage"),//用于处理下发错误ACK回复处理
    WSSManager_UpdateRNARunningStatus(103,"UpdateRNARunningStatus"),//用于更新RNA核提的实时运行信息
    WSSManager_RNARunningFinish(104,"RNARunningFinish"),//用于RNA完成信息

    WSSManager_UpdatePCRStatus(200,"UpdatePCRStatus"),//用于处理下发PCR模板的回复处理
    WSSManager_PCRAck_OK(201,"PCRAckOKMessage"),//用于处理下发PCR模板的正常ACK回复处理
    WSSManager_PCRAck_Error(202,"WSSManager_PCRAck_Error_AckMessage"),//用于处理下发错误ACK回复处理
    WSSManager_UpdatePCRRunningStatus(203,"UpdatePCRRunningStatus"),//用于更新PCR核提的实时运行信息
    WSSManager_UpdatePCRCurve(204,"UpdatePCRCurve"),//用于更新PCR核提的实时运行信息
    WSSManager_PCRRunningFinish(205,"PCRRunningFinish"),//用于更新PCR完成

    WSSManager_SLANPCRRunningFinish(206,"SLANPCRRunningFinish"),//用于更新宏石PCR完成

    WSSManager_UpgradeAck(301,"UpgradeAck"),//用于固件更新的ACK回复处理
    WSSManager_UpgradeFinish(302,"UpgradeFinish"),//用于固件更新的的完成回复处理


    Max_event(1000,"RNAAckMessage");//最大消息界限


    int code;
    String message;
    EnumMessageEvent(int code,String message){
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
