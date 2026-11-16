package com.paul.pipeline.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DataTimeUtil {
    private static SimpleDateFormat fullsdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat year_month_day_sdf = new SimpleDateFormat("yyyy-MM-dd");

    public static String getCurrDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
        String currDate = sdf.format(new Date());
        return currDate;
    }

    /**使用格林威治时间**/
    public static String getSysFullTime(){
        //TimeZone timeZone = TimeZone.getTimeZone("GMT+00:00");
        //sdf.setTimeZone(timeZone);
        return sdf.format(System.currentTimeMillis());
    }
    private static SimpleDateFormat hsdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    /**使用格林威治时间**/
    public static String getHFullTime(){
        //TimeZone timeZone = TimeZone.getTimeZone("GMT+00:00");
        //sdf.setTimeZone(timeZone);
        return hsdf.format(System.currentTimeMillis());
    }
    /**
     * 获取现在时间
     *
     * @return返回字符串格式 yyyyMMddHHmmss
     */
    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }
}
