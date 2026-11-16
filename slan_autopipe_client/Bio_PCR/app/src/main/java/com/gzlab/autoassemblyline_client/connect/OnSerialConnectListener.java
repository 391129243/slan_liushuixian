package com.gzlab.autoassemblyline_client.connect;

public interface OnSerialConnectListener {
    void onReceiveSerialMsg(String message);
    void onReceiveRNARunningMsg();
}
