package com.gidi.bio_pcr;

import android.os.Environment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
  /*  public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }*/


    public void test_channel() {
       /* double[] A1_RawCurve = {536.0,533.0,528.0,520.0,517.0,515.0};
        String s = JSON.toJSONString(A1_RawCurve);*/


/*
        String s = "/storage/123456/Template/12345/56789/12345";

        String path


        if(s.contains("Template")){
            int index = s.indexOf("Template");
            String str = s.substring(index+8, s.length());
            System.out.print(str);
        }
*/

       // int day = getDaysBetweenDay("2021-03-01", "2020-03-01");
       // System.out.print(day);
       // getValidDay();
       // int totalDay = DateFormatUtil.getDaysBetweenDay(getValidDay(), Constants.PRODUCTION_DATE);
       // System.out.print(totalDay);

       // int usedDay = DateFormatUtil.getDaysBetweenDay(DateFormatUtil.getNowDate(),Constants.PRODUCTION_DATE);
      //  System.out.print(usedDay);

/*        String conclsion = "甲流:阴性\\n乙流:阴性\\n新冠:阴性\\n博卡:阴性\\n";

        String[] list = conclsion.split("\\\\n");

        for(int i=0;i<list.length;i++){
            System.out.print(list[i]);
        */
        //String rna_step_num = "";
        /*String[] mRNA_running = rna_step_num.split("\\|");
        String mTemplateNum = mRNA_running[0];
        String mRNAFileNum = mRNA_running[1];
        String mRNAStep = mRNA_running[2];*/
       // System.out.print(rna_step_num);
       /* System.out.print(mRNAFileNum);
        System.out.print(mRNAStep);*/
        String step = "步骤1";
        String str = step.substring(0,1);
        System.out.print(str);
    }


    private String getValidDay(){
        String productDay = Constants.PRODUCTION_DATE;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar thisDay = Calendar.getInstance();
        thisDay.set(Calendar.YEAR, Integer.valueOf(productDay.substring(0,4)));
        thisDay.set(Calendar.MONTH, Integer.valueOf(productDay.substring(5,7))-1);	//月份 从0 开始
        thisDay.set(Calendar.DAY_OF_MONTH, Integer.valueOf(productDay.substring(8,10)));
        thisDay.add(Calendar.YEAR,5);
        return sdf.format(thisDay.getTime());


    }

    public static String[] splitString(String input, String mark) {
        String[] spilteResult = input.split(mark);
        return spilteResult;
    }


    public static int getDaysBetweenDay(String end_day1,String start_day2){
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try{
            Date end_date_1 = format.parse(end_day1);
            Date start_date_2 = format.parse(start_day2);
            Calendar fromCalendar = Calendar.getInstance();
            fromCalendar.setTime(start_date_2);
            fromCalendar.set(Calendar.HOUR_OF_DAY, 0);
            fromCalendar.set(Calendar.MINUTE, 0);
            fromCalendar.set(Calendar.SECOND, 0);
            fromCalendar.set(Calendar.MILLISECOND, 0);

            Calendar toCalendar = Calendar.getInstance();
            toCalendar.setTime(end_date_1);
            toCalendar.set(Calendar.HOUR_OF_DAY, 0);
            toCalendar.set(Calendar.MINUTE, 0);
            toCalendar.set(Calendar.SECOND, 0);
            toCalendar.set(Calendar.MILLISECOND, 0);

            return (int) ((toCalendar.getTime().getTime() - fromCalendar.getTime().getTime()) / (1000 * 60 * 60 * 24));
        }catch (Exception e){

        }



        return -1;
    }


    public static String getStringFromSDFile(String filePath) {
        InputStreamReader inputReader = null;
        BufferedReader bufReader = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            } else if (!file.isFile()) {
                return null;
            } else if (!file.canRead()) {
                return null;
            }
            inputReader = new InputStreamReader(new FileInputStream(file));
            bufReader = new BufferedReader(inputReader);
            String line = "";
            String Result = "";
            while ((line = bufReader.readLine()) != null)
                Result += line;
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufReader != null) {
                    bufReader.close();
                    bufReader = null;
                }
                if (inputReader != null) {
                    inputReader.close();
                    inputReader = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return null;

    }
}