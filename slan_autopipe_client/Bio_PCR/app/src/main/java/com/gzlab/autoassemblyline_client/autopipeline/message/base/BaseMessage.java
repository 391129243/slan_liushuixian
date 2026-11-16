package com.gzlab.autoassemblyline_client.autopipeline.message.base;

/**
 * 消息的基类
 */
public class BaseMessage {
    private String Type;//消息的类型

    private int SeqID;//消息的条数id

    private String DeviceID;//设备的ID

    private String DeviceType;//设备类型“PCR

    private int Position;//第几号设备

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
