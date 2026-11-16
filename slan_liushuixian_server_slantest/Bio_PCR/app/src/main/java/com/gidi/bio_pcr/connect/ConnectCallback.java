package com.gidi.bio_pcr.connect;

import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;

public interface ConnectCallback {
    void serialConnectSuccess(Object object);
    void serialConnectFail(Object object);
    //robot
    void serialRevData(RNARunningMessage rNARunningMessage);
    void serialSendData();
}
