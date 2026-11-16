package com.gzlab.autoassemblyline_client.autopipeline.mgr;

import android.content.Context;
import android.util.Log;

import com.hongshi.slansdk.model.ServiceParams;
import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.ConnectType;
import com.hongshi.slansdk.slanenum.DeviceType;

/**
 *
 * SLANService 管理类
 * 封装SLANService的初始化，配置和业务逻辑
 */
public class SLANServiceManager {
    private static final String TAG = "SLANServiceManager";
    private static volatile SLANServiceManager INSTANCE;
    private SLANService slanService;
    private Context context;

    private SLANServiceManager(Context context){
        this.context = context;
    }

    //获取单例实例
    public static SLANServiceManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SLANServiceManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SLANServiceManager(context);
                }
            }
        }
        return INSTANCE;
    }


    public SLANService initSLANService(DeviceType deviceType) throws Exception {
        return initSLANService(false, ConnectType.USB,deviceType);
    }

    public static int[] getDefaultBlock() {
        int[] defaultBlock = new int[1];
        defaultBlock[0] = 1;
        return defaultBlock;
    }


    public SLANService initSLANService(boolean isDebug, ConnectType connectType, DeviceType deviceType) throws Exception {
        if (slanService != null) {
            return slanService;
        }

        try {
            ServiceParams params = new ServiceParams();
            params.setConnectType(connectType);
            params.setDeviceType(deviceType);
            params.setDebug(isDebug);

            slanService = new SLANService(context, params);
            return slanService;
        } catch (Exception e) {
            Log.e(TAG, "SLANService 初始化失败", e);
            throw new Exception("初始化失败：" + e.getMessage(), e);
        }
    }

    // 获取已初始化的 SLANService 实例
    public SLANService getSlanService() {
        return slanService;
    }

    // 检查是否已初始化
    public boolean isInitialized() {
        return slanService != null;
    }

    // 销毁资源
    public void destroy() {
        if (slanService != null) {
            // 如果 SLANService 有释放资源的方法，此处补充
            // slanService.release();
            slanService = null;
        }
        INSTANCE = null;
    }

    /**
     * 通过字符串匹配对应的 DeviceType 枚举值
     * @param deviceTypeStr 设备类型字符串（如 "SLAN_16H"）
     * @return 匹配的枚举值
     * @throws IllegalArgumentException 匹配失败时抛出异常
     */
    public static DeviceType fromString(String deviceTypeStr) {
        // 空值校验
        if (deviceTypeStr == null || deviceTypeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("设备类型字符串不能为空");
        }

        // 遍历枚举常量，匹配名称（忽略大小写，增强兼容性）
        for (DeviceType type : DeviceType.values()) {
            if (type.name().equalsIgnoreCase(deviceTypeStr.trim())) {
                return type;
            }
        }

        // 匹配失败时抛出明确异常，方便定位问题
        throw new IllegalArgumentException("未找到匹配的 DeviceType 枚举值：" + deviceTypeStr);
    }

    /**
     * 重载方法：匹配失败时返回默认值（更友好的容错方案）
     * @param deviceTypeStr 设备类型字符串
     * @param defaultValue 匹配失败时的默认值
     * @return 匹配的枚举值 或 默认值
     */
    public static DeviceType fromString(String deviceTypeStr, DeviceType defaultValue) {
        try {
            return fromString(deviceTypeStr);
        } catch (IllegalArgumentException e) {
            // 打印日志，方便调试
            Log.w("DeviceType", "匹配设备类型失败，使用默认值：" + e.getMessage());
            return defaultValue;
        }
    }
}
