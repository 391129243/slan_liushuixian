package com.winters.boweilibrary.BoWei.listener;

import androidx.annotation.NonNull;

import com.winters.boweilibrary.BoWei.model.experiment.RunningInfo;


/**
 * 运行状态监听
 */
public interface RunningInfoListener {
    void getRunningInfo(@NonNull String serialNo, byte block, @NonNull RunningInfo info);
}
