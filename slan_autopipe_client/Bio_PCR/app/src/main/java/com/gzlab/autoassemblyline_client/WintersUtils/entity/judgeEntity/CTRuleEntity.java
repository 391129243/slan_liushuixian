package com.gzlab.autoassemblyline_client.WintersUtils.entity.judgeEntity;

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

public class CTRuleEntity {

    private boolean IsSelected;

    private boolean IsPostive = false;

    private boolean IsSuspicios = false;

    private String Channel;

    private String Category;

    private double CTvalue;

    private double CTvalueUP;//上限

    private double CTvalueDown;//下限

    private double CTvalueUP_Suspicious;//可疑

    private double actualCt;

    public CTRuleEntity(boolean isSelected, boolean isPostive, boolean isSuspicios, String channel, String category, double CTvalue, double CTvalueUP, double CTvalueDown, double CTvalueUP_Suspicious) {
        IsSelected = isSelected;
        IsPostive = isPostive;
        IsSuspicios = isSuspicios;
        Channel = channel;
        Category = category;
        this.CTvalue = CTvalue;
        this.CTvalueUP = CTvalueUP;
        this.CTvalueDown = CTvalueDown;
        this.CTvalueUP_Suspicious = CTvalueUP_Suspicious;
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

    public String getCategory() {
        return Category;
    }

    public void setCategory(String category) {
        Category = category;
    }

    public double getCTvalue() {
        return CTvalue;
    }

    public void setCTvalue(double CTvalue) {
        this.CTvalue = CTvalue;
    }

    public double getCTvalueUP() {
        return CTvalueUP;
    }

    public void setCTvalueUP(double CTvalueUP) {
        this.CTvalueUP = CTvalueUP;
    }

    public double getCTvalueDown() {
        return CTvalueDown;
    }

    public void setCTvalueDown(double CTvalueDown) {
        this.CTvalueDown = CTvalueDown;
    }

    public boolean isPostive() {
        return IsPostive;
    }

    public void setPostive(boolean postive) {
        IsPostive = postive;
    }

    public double getCTvalueUP_Suspicious() {
        return CTvalueUP_Suspicious;
    }

    public void setCTvalueUP_Suspicious(double CTvalueUP_Suspicious) {
        this.CTvalueUP_Suspicious = CTvalueUP_Suspicious;
    }

    public boolean isSuspicios() {
        return IsSuspicios;
    }

    public void setSuspicios(boolean suspicios) {
        IsSuspicios = suspicios;
    }

    public double getActualCt() {
        return actualCt;
    }

    public void setActualCt(double actualCt) {
        this.actualCt = actualCt;
    }

    public static List<CTRuleEntity> getCTRuleList(File ctFile){

        List<CTRuleEntity> datalist=new ArrayList<CTRuleEntity>();
        FileInputStream inputStream = null;

        try {

            if(!ctFile.exists()){
                ctFile.createNewFile();
                return datalist;
            }

            inputStream = new FileInputStream(ctFile);

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

            datalist = gson.fromJson(strJson, new TypeToken<List<CTRuleEntity>>() {}.getType());
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
