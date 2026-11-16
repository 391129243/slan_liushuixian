package com.gidi.bio_pcr.config;

import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKRuleEntity;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexConfig;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigManager {

    /**
     * 读取hex的配置文件
     */
    public static HexConfig getHexConfigFile(File file){
        HexConfig hexConfig = new HexConfig();
        FileInputStream inputStream = null;
        try {
            if(!file.exists()){
                file.createNewFile();
                return hexConfig;
            }
            inputStream = new FileInputStream(file);
            String strJson = FileUtils.fileToStr(inputStream);
            if (StringUtil.isEmpty(strJson)) {
                return hexConfig;
            }
            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            hexConfig = gson.fromJson(strJson,new TypeToken<HexConfig>() {}.getType());
            inputStream.close();
            return hexConfig;
        } catch (Exception e) {
            e.printStackTrace();
            return hexConfig;
        }finally {
            if(inputStream != null){
                try{
                    inputStream.close();
                    inputStream = null;
                }catch (Exception e){
                }
            }
        }
    }


    public static TotalTime getTotalTimeFile(File file){
        TotalTime totalTime = new TotalTime();
        FileInputStream inputStream = null;
        try {
            if(!file.exists()){
                file.createNewFile();
                return totalTime;
            }
            inputStream = new FileInputStream(file);
            String strJson = FileUtils.fileToStr(inputStream);
            if (StringUtil.isEmpty(strJson)) {
                return totalTime;
            }
            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            totalTime = gson.fromJson(strJson,new TypeToken<TotalTime>() {}.getType());
            inputStream.close();
            return totalTime;
        } catch (Exception e) {
            e.printStackTrace();
            return totalTime;
        }finally {
            if(inputStream != null){
                try{
                    inputStream.close();
                    inputStream = null;
                }catch (Exception e){
                }
            }
        }
    }



    public static boolean copyFile(File oldFile, String newDirPath,String fileName,CopyProgressListener listener) {
        try {
            File saveFile = new File(newDirPath, fileName);
            if (!oldFile.exists()) {
                Log.e("copyFile", "copyFile:  oldFile not exist.");
                return false;
            } else if (!oldFile.isFile()) {
                Log.e("copyFile", "copyFile:  oldFile not file.");
                return false;
            } else if (!oldFile.canRead()) {
                Log.e("copyFile", "copyFile:  oldFile cannot read.");
                return false;
            }
            long avi = oldFile.length();
            FileInputStream fis = new FileInputStream(oldFile);
            FileOutputStream fos = new FileOutputStream(saveFile);
            byte[] buffer = new byte[1024];
            int writeCount = 0;
            int byteRead;
            while (-1 != (byteRead = fis.read(buffer))) {
                fos.write(buffer, 0, byteRead);
                writeCount += byteRead;
                if (listener != null) {
                    //回调下载进度
                    listener.copyProgress((int) (writeCount * 100 / avi));
                }
            }

            fis.close();
            fos.flush();
            fos.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public interface CopyProgressListener {
        void copyProgress(int progress);
    }

}
