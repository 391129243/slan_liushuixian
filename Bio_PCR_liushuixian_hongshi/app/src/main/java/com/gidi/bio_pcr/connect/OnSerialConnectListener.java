package com.gidi.bio_pcr.connect;

import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;

public interface OnSerialConnectListener {
    void onReceiveSerialMsg(String message);
    void onReceiveRNARunningMsg();
}
