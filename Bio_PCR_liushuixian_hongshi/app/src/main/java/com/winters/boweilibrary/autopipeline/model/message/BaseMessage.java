package com.winters.boweilibrary.autopipeline.model.message;

public class BaseMessage {

    private String Type; //消息类型

    private int SeqID;//消息id

    private String DeviceID;//RNA1111  RNA2222  PCR1111 PCR222 ----PCR6666

    private String DeviceType;  ///RNA 或者 PCR

    private int Position;  ///RNA 1-2  PCR 1-6

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public int getSeqId() {
        return SeqID;
    }

    public void setSeqId(int seqId) {
        this.SeqID = seqId;
    }

    public String getDeviceID() {
        return DeviceID;
    }

    public void setDeviceID(String deviceID) {
        this.DeviceID = deviceID;
    }

    public String getDeviceType() {
        return DeviceType;
    }

    public void setDeviceType(String deviceType) {
        this.DeviceType = deviceType;
    }

    public int getPosition() {
        return Position;
    }

    public void setPosition(int position) {
        this.Position = position;
    }

    public BaseMessage() {
    }

    public BaseMessage(String type, int seqId, String deviceID, String deviceType, int position) {
        this.Type = type;
        this.SeqID = seqId;
        this.DeviceID = deviceID;
        this.DeviceType = deviceType;
        this.Position = position;
    }

}
