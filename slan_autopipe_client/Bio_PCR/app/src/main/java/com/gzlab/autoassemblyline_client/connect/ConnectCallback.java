package com.gzlab.autoassemblyline_client.connect;

public interface ConnectCallback {
    void serialConnectSuccess(Object object);
    void serialConnectFail(Object object);
}
