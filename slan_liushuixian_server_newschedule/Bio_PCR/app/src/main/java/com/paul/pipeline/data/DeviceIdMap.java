package com.paul.pipeline.data;

/**
 * 工位与 deviceId 的固定映射。
 * position 从 1 开始：1-6 对应 SLANPCR1111~6666，1-2 对应 RNA1111~RNA2222。
 * 原先定义在 ModuleManagerFragment 里，调度层也要用，所以提到公共位置。
 */
public class DeviceIdMap {
    private DeviceIdMap() {
    }

    public static final String[] PCR_DEVICE_IDS = {
            "SLANPCR1111", "SLANPCR2222", "SLANPCR3333",
            "SLANPCR4444", "SLANPCR5555", "SLANPCR6666"
    };

    public static final String[] RNA_DEVICE_IDS = {
            "RNA1111", "RNA2222"
    };

    /** @param position 工位号，从 1 开始；越界返回 null */
    public static String pcrDeviceId(int position) {
        int index = position - 1;
        if (index < 0 || index >= PCR_DEVICE_IDS.length) {
            return null;
        }
        return PCR_DEVICE_IDS[index];
    }
}
