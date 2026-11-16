package com.paul.pipeline.listener;

import com.paul.pipeline.message.ErrorInfoMessage;

public interface DeviceErrorCallback {
    void error(ErrorInfoMessage message);
}
