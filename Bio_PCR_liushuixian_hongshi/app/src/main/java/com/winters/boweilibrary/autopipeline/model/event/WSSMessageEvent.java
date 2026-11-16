package com.winters.boweilibrary.autopipeline.model.event;

import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;

public class WSSMessageEvent {

    private EnumMessageEvent msgType;

    private WSSConnectionItem wSSConnectionItem;

    private Object data;

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

    public WSSConnectionItem getwSSConnectionItem() {
        return wSSConnectionItem;
    }

    public void setwSSConnectionItem(WSSConnectionItem wSSConnectionItem) {
        this.wSSConnectionItem = wSSConnectionItem;
    }

    public WSSMessageEvent(EnumMessageEvent msgType, WSSConnectionItem wSSConnectionItem, Object data) {
        this.msgType = msgType;
        this.wSSConnectionItem = wSSConnectionItem;
        this.data = data;
    }

}
