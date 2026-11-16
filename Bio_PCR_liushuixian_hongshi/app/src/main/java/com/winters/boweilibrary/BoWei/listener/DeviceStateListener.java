package com.winters.boweilibrary.BoWei.listener;


import com.winters.boweilibrary.BoWei.bioweyenum.DeviceState;

/**
 * 设备状态监听
 */
public interface DeviceStateListener {
    public abstract void getDeviceInfo(DeviceState deviceState);
}
