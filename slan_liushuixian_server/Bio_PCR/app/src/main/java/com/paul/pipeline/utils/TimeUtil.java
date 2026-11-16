package com.paul.pipeline.utils;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TimeUtil {

    public static String secondsToTimeSimple(int seconds) {
        int hours = seconds / 3600; // 计算小时数
        int minutes = (seconds % 3600) / 60; // 计算剩余分钟数
        int remainingSeconds = seconds % 60; // 计算剩余秒数
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
    private static SimpleDateFormat second_sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);

    public static String getCurrentTimeSecond(){
        return second_sdf.format(System.currentTimeMillis());
    }
}
