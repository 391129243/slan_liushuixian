package com.paul.pipeline.listener;

public interface DeviceWorkFinishListenser {
    void closedoor(int index, String template);
    void opendoor(int index, String template);
}
