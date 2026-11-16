package com.gidi.bio_pcr.WintersUtils.entity.judgeEntity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PEAKRuleEntity {

    //public static List<PEAKRuleEntity> pEAKPEAKRuleEntityList = new ArrayList<PEAKRuleEntity>();

    private boolean IsSelected;

    private String Channel;

    private List<PEAKSingleRuleEntity> PEAKSingleRuleEntityList;

    public PEAKRuleEntity(boolean isSelected, String channel, List<PEAKSingleRuleEntity> PEAKSingleRuleEntityList) {
        IsSelected = isSelected;
        Channel = channel;
        this.PEAKSingleRuleEntityList = PEAKSingleRuleEntityList;
    }

    public boolean isSelected() {
        return IsSelected;
    }

    public void setSelected(boolean selected) {
        IsSelected = selected;
    }

    public String getChannel() {
        return Channel;
    }

    public void setChannel(String channel) {
        Channel = channel;
    }

    public List<PEAKSingleRuleEntity> getPEAKSingleRuleEntityList() {
        return PEAKSingleRuleEntityList;
    }

    public void setPEAKSingleRuleEntityList(List<PEAKSingleRuleEntity> PEAKSingleRuleEntityList) {
        this.PEAKSingleRuleEntityList = PEAKSingleRuleEntityList;
    }


    public static List<PEAKRuleEntity> getPEAKRuleList(File peakFile){

        List<PEAKRuleEntity> datalist=new ArrayList<PEAKRuleEntity>();
        FileInputStream inputStream = null;

        try {

            if(!peakFile.exists()){
                peakFile.createNewFile();
                return datalist;
            }

            inputStream = new FileInputStream(peakFile);

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

            datalist = gson.fromJson(strJson, new TypeToken<List<PEAKRuleEntity>>() {}.getType());
            inputStream.close();
            return datalist;
        } catch (Exception e) {

            e.printStackTrace();
            return datalist;
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
