package com.gidi.bio_pcr.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import com.gidi.bio_pcr.constant.SharedConstant;
import com.google.gson.Gson;

import java.util.Locale;

public class LocaleUtils {
    /**
     * 中文
     */
    public static final Locale LOCALE_CHINESE = Locale.CHINA;
    /**
     * 英文
     */
    public static final Locale LOCALE_ENGLISH = Locale.US;



    /**
     * 获取用户设置的Locale
     * @param pContext Context
     * @return Locale
     */
    public static Locale getUserLocale(Context pContext) {
        String _LocaleJson = PreferenceUtil.getInstance(pContext).getStringValue(SharedConstant.LOCALE_KEY,"");
        return jsonToLocale(_LocaleJson);
    }
    /**
     * 获取当前的Locale
     * @param pContext Context
     * @return Locale
     */
    public static Locale getCurrentLocale(Context pContext) {
        Locale _Locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0有多语言设置获取顶部的语言
            _Locale = pContext.getResources().getConfiguration().getLocales().get(0);
        } else {
            _Locale = pContext.getResources().getConfiguration().locale;
        }
        return _Locale;
    }
    /**
     * 保存用户设置的Locale
     * @param pContext Context
     * @param pUserLocale Locale
     */
    public static void saveUserLocale(Context pContext, Locale pUserLocale) {
      //20250317
        String _LocaleJson = localeToJson(pUserLocale);
        Log.i("GGG","_LocaleJson---" + _LocaleJson);
        PreferenceUtil.getInstance(pContext).setValueByName(SharedConstant.LOCALE_KEY,_LocaleJson);
    }
    /**
     * Locale转成json
     * @param pUserLocale UserLocale
     * @return json String
     */
    private static String localeToJson(Locale pUserLocale) {
        Gson _Gson = new Gson();
        return _Gson.toJson(pUserLocale);
    }
    /**
     * json转成Locale
     * @param pLocaleJson LocaleJson
     * @return Locale
     */
    private static Locale jsonToLocale(String pLocaleJson) {
        Gson _Gson = new Gson();
        if(pLocaleJson.equals("")){
            return Locale.CHINESE;
        }else {
            return _Gson.fromJson(pLocaleJson, Locale.class);
        }
    }
    /**
     * 更新Locale
     * @param pContext Context
     * @param pNewUserLocale New User Locale
     */
    public static void updateLocale(Context pContext, Locale pNewUserLocale) {
        if (needUpdateLocale(pContext, pNewUserLocale)) {
            Configuration _Configuration = pContext.getResources().getConfiguration();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                _Configuration.setLocale(pNewUserLocale);
            } else {
                _Configuration.locale =pNewUserLocale;
            }
            DisplayMetrics _DisplayMetrics = pContext.getResources().getDisplayMetrics();
            pContext.createConfigurationContext(_Configuration);
            pContext.getResources().updateConfiguration(_Configuration, _DisplayMetrics);
            saveUserLocale(pContext, pNewUserLocale);
        }
    }
    /**
     * 判断需不需要更新
     * @param pContext Context
     * @param pNewUserLocale New User Locale
     * @return true / false
     */
    public static boolean needUpdateLocale(Context pContext, Locale pNewUserLocale) {
        Log.i("GGG","needUpdateLocale" + pNewUserLocale);
        Log.i("GGG","getCurrentLocale(pContext).equals(pNewUserLocale)" + getCurrentLocale(pContext).equals(pNewUserLocale));
        return pNewUserLocale != null && !getUserLocale(pContext).equals(pNewUserLocale);
    }
}
