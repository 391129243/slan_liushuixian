package com.gidi.bio_pcr.listener;

import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;

/**
 * 身份证读取接口
 */
public interface ReadCardInterface {

    /**
     * 连接状态
     */
    void updateConnectState(boolean isconnect);

    /**
     * 获取身份信息
     */
    void getIdentityInfo(IDCardInfo idCardInfo);

    /**
     * 获取身份证失败
     */
    void getIdentityInfoFail();


}
