package com.paul.pipeline.listener;

import com.paul.pipeline.message.ArmworkMessage;

public interface ArmWorkListener {
    void executeWork(ArmworkMessage msg);
}
