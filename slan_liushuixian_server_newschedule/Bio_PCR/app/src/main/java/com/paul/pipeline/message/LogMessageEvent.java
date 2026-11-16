package com.paul.pipeline.message;

import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.model.event.WSSMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;

public class LogMessageEvent extends WSSMessageEvent {
    int logtype;
    Object logtext;

    public int getLogtype() {
        return logtype;
    }

    public void setLogtype(int logtype) {
        this.logtype = logtype;
    }

    public Object getLogtext() {
        return logtext;
    }

    public void setLogtext(Object logtext) {
        this.logtext = logtext;
    }

    public LogMessageEvent(int logtype,Object logtext, EnumMessageEvent msgType, WSSConnectionItem wSSConnectionItem, Object data) {
        super(msgType, wSSConnectionItem, data);
        setLogtype(logtype);
        setLogtext(logtext);
    }
}
