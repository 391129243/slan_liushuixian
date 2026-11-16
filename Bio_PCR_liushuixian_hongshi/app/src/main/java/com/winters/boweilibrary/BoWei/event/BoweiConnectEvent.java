package com.winters.boweilibrary.BoWei.event;

public class BoweiConnectEvent {
    private boolean isConnect;

    public BoweiConnectEvent(boolean isConnect){
        this.isConnect = isConnect;
    }

    public boolean isConnect() {
        return isConnect;
    }

    public void setConnect(boolean connect) {
        isConnect = connect;
    }
}
