package com.paul.pipeline.utils;

import android.os.Parcelable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MmkvUtils {
    private static MMKV mkv;

    private MmkvUtils() {

        mkv = MMKV.defaultMMKV();

    }

    public static MmkvUtils getInstance() {

        return SingletonHolder.sInstance;

    }

//静态内部类

    private static class SingletonHolder {

        private static final MmkvUtils sInstance = new MmkvUtils();

    }

    /**

     保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法

     @param key

     @param object

     */

    public static void encode(String key, Object object) {

        if (object instanceof String) {

            mkv.encode(key, (String) object);

        } else if (object instanceof Integer) {

            mkv.encode(key, (Integer) object);

        } else if (object instanceof Boolean) {

            mkv.encode(key, (Boolean) object);

        } else if (object instanceof Float) {

            mkv.encode(key, (Float) object);

        } else if (object instanceof Long) {

            mkv.encode(key, (Long) object);

        } else if (object instanceof Double) {

            mkv.encode(key, (Double) object);

        } else if (object instanceof byte[]) {

            mkv.encode(key, (byte[]) object);

        } else {

            mkv.encode(key, object.toString());

        }

    }

    public static void encodeSet(String key, Set sets) {

        mkv.encode(key, sets);

    }

    public static void encodeParcelable(String key, Parcelable obj) {

        mkv.encode(key, obj);

    }

    /**

     得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值

     @param key

     @param defaultObject

     @return

     */

    public static Object decode(String key, Object defaultObject) {

        if (defaultObject instanceof String) {

            return mkv.decodeString(key, (String) defaultObject);

        } else if (defaultObject instanceof Integer) {

            return mkv.decodeInt(key, (Integer) defaultObject);

        } else if (defaultObject instanceof Boolean) {

            return mkv.decodeBool(key, (Boolean) defaultObject);

        } else if (defaultObject instanceof Float) {

            return mkv.decodeFloat(key, (Float) defaultObject);

        } else if (defaultObject instanceof Long) {

            return mkv.decodeLong(key, (Long) defaultObject);

        } else if (defaultObject instanceof Double) {

            return mkv.decodeDouble(key, (Double) defaultObject);

        } else if (defaultObject instanceof byte[]) {

            return mkv.decodeBytes(key, (byte[]) defaultObject);

        }

        return defaultObject;

    }

    /**

     得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     */

    public static Integer decodeInt(String key) {

        return mkv.decodeInt(key, 0);

    }

    public static Double decodeDouble(String key) {

        return mkv.decodeDouble(key, 0.00);

    }

    public static Long decodeLong(String key) {

        return mkv.decodeLong(key, 0L);

    }

    public static Boolean decodeBoolean(String key) {

        return mkv.decodeBool(key, false);

    }

    public static Float decodeFloat(String key) {

        return mkv.decodeFloat(key, 0F);

    }

    public static byte[] decodeBytes(String key) {

        return mkv.decodeBytes(key);

    }

    public static String decodeString(String key) {

        return mkv.decodeString(key,"");

    }

    public static Set decodeStringSet(String key) {

        return mkv.decodeStringSet(key, Collections.emptySet());

    }

    public static Parcelable decodeParcelable(String key, Class clz) {

        return mkv.decodeParcelable(key, clz);

    }

    /**

     移除某个key对

     @param key

     */

    public static void removeKey(String key) {

        mkv.removeValueForKey(key);

    }

    /**

     移除多个key对

     @param key

     */

    public static void removeKeys(String[] key) {

        mkv.removeValuesForKeys(key);

    }

    /**

     获取全部key对
     */

    public static String[] getAllKeys() {

        return mkv.allKeys();

    }

    /**

     含有某个key

     @param key

     @return

     */

    public static boolean hasKey(String key) {

        return mkv.containsKey(key);

    }

    /**

     含有某个key

     @param key

     @return

     */

    public static boolean have(String key) {

        return mkv.contains(key);

    }

    /**

     清除所有key
     */

    public static void clearAll() {

        mkv.clearAll();

    }

    /**

     获取操作对象

     @return

     */

    public static MMKV getMkv() {

        return mkv;

    }

    // 统一配置并缓存Gson实例，排除DataBinding回调及UI类，避免OOM
    private static final Gson GSON = new GsonBuilder().setExclusionStrategies(new com.google.gson.ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(com.google.gson.FieldAttributes f) {
            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            String name = clazz.getName();
            return name.equals("androidx.databinding.PropertyChangeRegistry")
                    || name.equals("android.view.View")
                    || name.equals("android.content.Context")
                    || name.equals("android.app.Activity");
        }
    }).create();

    public static <T> Boolean setArray(String name,List<T> list) {
        MMKV kv = MMKV.defaultMMKV();
        if (list == null || list.size() == 0) { //清空
            kv.putInt(name + "size", 0);
            int size = kv.getInt(name + "size", 0);
            for (int i = 0; i < size; i++) {
                if (kv.getString(name + i, null) != null) {
                    kv.remove(name + i);
                }
            }
        } else {
            kv.putInt(name + "size", list.size());
            if (list.size() > 20) {
                list.remove(0);   //只保留后20条记录
            }
            for (int i = 0; i < list.size(); i++) {
                kv.remove(name + i);
                kv.remove(GSON.toJson(list.get(i)));//删除重复数据 先删后加
                kv.putString(name + i, GSON.toJson(list.get(i)));
            }
        }
        return kv.commit();
    }

    /*
     *用途:加载sp的数组 name-自定义集合的名称
     */
    public static <T> ArrayList<T> getArray(String name, T bean) {
        MMKV kv = MMKV.defaultMMKV();
        ArrayList<T> list = new ArrayList<T>();
        int size = kv.getInt(name + "size", 0);
        for (int i = 0; i < size; i++) {
            if (kv.getString(name + i, null) != null) {
                try {
                    list.add((T) GSON.fromJson(kv.getString(name + i, null), bean.getClass()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        return list;
    }

    public static <T> Boolean saveObject(String name,T obj) {
        MMKV kv = MMKV.defaultMMKV();
        if (obj != null) { //清空
           String content = GSON.toJson(obj);
           if(kv.containsKey(name)) {
               kv.remove(name);
           }
           kv.putString(name,content);
        }
        return kv.commit();
    }
    public static <T> T getObject(String name,Class<T> claasz) {
        MMKV kv = MMKV.defaultMMKV();
        if (name != null) { //清空
            if(kv.containsKey(name)) {
                String content = kv.getString(name,"");
                if(content!=null&&!content.isEmpty()){
                    T obj =  new Gson().fromJson(content,claasz);
                    return obj;
                }
            }
        }
        return null;
    }
}

//工具类使用
///**
//
// 工具类使用
// */
//
//private void iniSpUtils() {
//
//    mStr.append(“工具类使用\n”);
//
//    mStr.append(“boolean数据类型 \n”);
//
//    SpUtils.getInstance().encode(“bool”, true);
//
//    mStr.append(“key=>bool，value=>” + SpUtils.getInstance().decode(“bool”, false) + " \n\n");
//
//    mStr.append(“int数据类型 \n”);
//
//    SpUtils.getInstance().encode(“int”, 2020);
//
//    mStr.append(“key=>int，value=>” + SpUtils.getInstance().decode(“int”, 0) + " \n\n");
//
//    mStr.append(“long数据类型 \n”);
//
//    SpUtils.getInstance().encode(“long”, Long.MAX_VALUE);
//
//    mStr.append(“key=>long，value=>” + SpUtils.getInstance().decode(“long”, 0L) + " \n\n");
//
//    mStr.append(“float数据类型 \n”);
//
//    SpUtils.getInstance().encode(“float”, 3.14f);
//
//    mStr.append(“key=>float，value=>” + SpUtils.getInstance().decode(“float”, 0f) + " \n\n");
//
//    mStr.append(“double数据类型 \n”);
//
//    SpUtils.getInstance().encode(“double”, 3.141596265356677897);
//
//    mStr.append(“key=>double，value=>” + SpUtils.getInstance().decode(“double”, 3.14) + " \n\n");
//
//    mStr.append(“string数据类型 \n”);
//
//    SpUtils.getInstance().encode(“string”, “我是 MMKV”);
//
//    mStr.append(“key=>string，value=>” + SpUtils.getInstance().decode(“string”, “”) + " \n\n");
//
//    mStr.append(“byte[]数据类型 \n”);
//
//    byte[] bytes = {‘m’, ‘m’, ‘k’, ‘v’};
//
//    SpUtils.getInstance().encode(“bytes”, bytes);
//
//    byte[] bytes1 = {‘1’, ‘2’, ‘3’};
//
//    mStr.append(“key=>bytes，value=>” + new String((byte[]) SpUtils.getInstance().decode(“bytes”, bytes1)) + " \n\n");
//
//    mStr.append(“Set数据类型 \n”);
//
//    HashSet set = new HashSet<>();
//
//    set.add(“H”);
//
//    set.add(“U”);
//
//}
