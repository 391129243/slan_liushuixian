package com.gidi.bio_pcr.WintersUtils.utils;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexSingleEntity;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by XianBo.Geng on 2017/1/18.
 */
public class ListDataSaveUtil {


    public static String SDPATH = Environment.getExternalStorageDirectory().toString()
            + "/" + "PCR"+"/"+"Template"+"/"+"Step2-2"+"/"+"100";

    public ListDataSaveUtil() {

    }

    public static void scanFile(Context context) {
        Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scanIntent.setData(Uri.fromFile(new File(SDPATH)));
        context.sendBroadcast(scanIntent);
    }


    /**
     * 保存List
     * @param name
     * @param datalist
     */
    public static <T> void setDataList(String name , List<T> datalist) {

        FileOutputStream outputStream = null;

        if (null == datalist || datalist.size() < 0)
            return;

        Gson gson = new Gson();
        //转换成json数据，再保存
        String strJson = gson.toJson(datalist);

        File dir = new File(SDPATH);
        if(!dir.exists()){
            dir.mkdir();
        }

        File file = new File(SDPATH + "/" + name);
        if(dir.exists()){

            try {
                file.createNewFile();

                outputStream = new FileOutputStream(SDPATH + "/" + name);

                outputStream.write(strJson.getBytes());
                outputStream.flush();
                outputStream.close();


            } catch (IOException e) {

                e.printStackTrace();

            }
        }

    }

    public static boolean setHexConfig(String name , HexConfig data) {

        FileOutputStream outputStream = null;

        if (null == data) {
            return false;
        }

        Gson gson = new Gson();
        //转换成json数据，再保存
        String strJson = gson.toJson(data);

        File file = new File(name);

        try {
            File fileParent = file.getParentFile();
            if(!fileParent.exists()) {
                fileParent.mkdirs();
            }
            file.createNewFile();
            outputStream = new FileOutputStream(name);
            outputStream.write(strJson.getBytes());
            outputStream.flush();
            outputStream.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;

        }
    }

    /**
     * 保存List
     * @param name
     * @param datalist
     */
    public static boolean setHexDebugEntityList(String name , List<HexDebugEntity> datalist) {

        FileOutputStream outputStream = null;

        if (null == datalist || datalist.size() < 0)
            return false;;

        Gson gson = new Gson();
        //转换成json数据，再保存
        String strJson = gson.toJson(datalist);

        File file = new File(name);

        try {
            File fileParent = file.getParentFile();
            if(!fileParent.exists()) {
                fileParent.mkdirs();
            }
            file.createNewFile();
            outputStream = new FileOutputStream(name);
            outputStream.write(strJson.getBytes());
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取List
     * @param file
     * @return
     */
    public static List<HexDebugEntity> getHexDebugEntityList(File file) {

        List<HexDebugEntity> datalist=new ArrayList<HexDebugEntity>();
        FileInputStream inputStream = null;

        try {

            if(!file.exists()){
                file.createNewFile();
                return datalist;
            }

            inputStream = new FileInputStream(file);

            String strJson = fileToStr(inputStream);

            if (null == strJson || strJson == "") {
                return datalist;
            }

            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            datalist = gson.fromJson(strJson, new TypeToken<List<HexDebugEntity>>() {}.getType());
            if (datalist == null) {
                datalist = new ArrayList<HexDebugEntity>();
            }
            inputStream.close();

            HexDebugEntity finish = new HexDebugEntity();
            finish.setDescription("finish");
            finish.setName("finish");
            finish.setNO("");
            finish.setHexActionList(new ArrayList<HexSingleEntity>());
            finish.setSelected(true);
            finish.setLoop(1);

            datalist.add(0,finish);

            datalist.add(finish);

            return datalist;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ListDataSaveUtil",e.getMessage());
            return datalist;
        }finally {
            if(inputStream != null){
                try{
                    inputStream.close();
                    inputStream = null;
                }catch (Exception e){
                    e.printStackTrace();
                    return datalist;
                }
            }
        }
    }









    /**
     * 获取List
     * @param name
     * @return
     */
    public static List<HexDebugEntity> getHexDebugEntityList(String name) {

        FileInputStream inputStream = null;
        List<HexDebugEntity> datalist=new ArrayList<HexDebugEntity>();

        try {

            File dir = new File(SDPATH);
            if(!dir.exists()){
                dir.mkdir();
            }

            File file = new File(SDPATH + "/" + name);
            if(!file.exists()){
                file.createNewFile();
                return datalist;
            }

            inputStream = new FileInputStream(SDPATH + "/" + name);


            String strJson = fileToStr(inputStream);



            //ByteArrayOutputStream bout = new ByteArrayOutputStream();
            //byte[] bufffer = new byte[inputStream.available()];
            //int len = 0 ;
            //while((len = fileInputStream.read(bufffer)) != -1){
            //    bout.write(bufffer,0,len);
            //}
            //byte[] content = bout.toByteArray();
            //String strJson = new String(content);
            //fileInputStream.close();



            if (null == strJson || strJson == "") {
                return datalist;
            }

            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            datalist = gson.fromJson(strJson, new TypeToken<List<HexDebugEntity>>() {}.getType());
            if (datalist == null) {
                datalist = new ArrayList<HexDebugEntity>();
            }


            inputStream.close();

            return datalist;


        } catch (Exception e) {

            e.printStackTrace();
            return datalist;
        }

    }

    public static String fileToStr(InputStream inputStream) throws IOException {
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        // 数组长度
        byte[] buffer = new byte[1024];
        // 初始长度
        int len = 0;
        // 循环
        while ((len = inputStream.read(buffer)) != -1) {
            arrayOutputStream.write(buffer, 0, len);
//            return arrayOutputStream.toString();
        }
        //Log.i(TAG, arrayOutputStream.toString());
        return arrayOutputStream.toString();
    }

}