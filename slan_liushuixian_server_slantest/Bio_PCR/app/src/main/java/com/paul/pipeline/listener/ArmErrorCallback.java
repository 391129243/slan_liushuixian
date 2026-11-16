package com.paul.pipeline.listener;

import com.paul.pipeline.message.ArmworkMessage;

public interface ArmErrorCallback {
    void error(ArmworkMessage msg);
}
