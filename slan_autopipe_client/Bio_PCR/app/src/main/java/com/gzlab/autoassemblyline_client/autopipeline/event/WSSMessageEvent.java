package com.gzlab.autoassemblyline_client.autopipeline.event;

/**
 * WSS消息后上报
 */
public class WSSMessageEvent {
    private EnumMessageEvent msgType;

    private Object data;

    private int arg;

    public EnumMessageEvent getMsgType() {
        return msgType;
    }

    public void setMsgType(EnumMessageEvent msgType) {
        this.msgType = msgType;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public int getArg() {
        return arg;
    }

    public void setArg(int arg) {
        this.arg = arg;
    }

    public WSSMessageEvent(EnumMessageEvent msgType, Object data){
        this.msgType = msgType;
        this.data = data;
    }

    public WSSMessageEvent(EnumMessageEvent msgType, Object data, int arg){
        this.msgType = msgType;
        this.data = data;
        this.arg = arg;
    }
}
