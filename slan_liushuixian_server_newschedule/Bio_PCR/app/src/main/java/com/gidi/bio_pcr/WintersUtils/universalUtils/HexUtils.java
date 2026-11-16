package com.gidi.bio_pcr.WintersUtils.universalUtils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.WintersUtils.NumUtils;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexSingleEntity;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.util.NumberUtils;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HexUtils {

//    public static double fastSpeed = 6.25f; //2*1/(0.064*5) = 6.25mm/s,(导程为2mm,X=5)--混合速度 快速
    public static double fastSpeed = 12.5f; //2*1/(0.064*5) = 6.25mm/s,(导程为2mm,X=5)--混合速度 快速

    public static double mediumSpeed = 1.25f; //2*1/(0.064*5) = 6.25mm/s,(导程为2mm,X=5)--舱门及备用 中速

    public static double slowSpeed = 0.625f; //2*1/(0.064*5) = 6.25mm/s,(导程为2mm,X=5)--磁吸速度 慢速


    public static double totalHeight = 67.5f;///μm

    public static double bufferHeight = 2.0f;///μm

    public static double areaParm = 19.625f;///μm

    public static double subXituoVolume = 100.0f;///μm

    public static String HEXFILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/PCR/hexFile_v5/";

    public static String HEXCREATEFILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/PCR/hexCreateFile_v5/";

    public static String HEXCONFIG_PATH = HEXCREATEFILE_PATH + "hexConfig_v5/";




    public static String temperatureHexFile = HEXFILE_PATH + "V5-1温控参数.hex";
    public static String splitTemperatureParm= "split_temperature_setting";


    public static String casseteHexFile = HEXFILE_PATH + "V5-2卡盒入位.hex";

    public static String magneticHexFile = HEXFILE_PATH + "V5-3磁珠活化管1.hex";
    public static String magneticSampleVolumeParm= "yangben_height_setting";
    public static String magneticLiejieyeVolumeParm= "liejieye_height_setting";
    public static String magneticTotalVolumeParm= "total_height_setting";

    public static String splitHexFile = HEXFILE_PATH + "V5-4裂解.hex";
    public static String splitSampleVolumeParm= "yangben_height_setting";
    public static String splitTotalVolumeParm= "total_height_setting";
    public static String splitLoopParm= "total_mixloop_setting";
    public static String splitRelativeTotalVolumeParm= "total_relative_height_setting";
    public static String splitLiejieyeVolumeParm= "liejieye_height_setting";

    public static String wash1HexFile = HEXFILE_PATH + "V5-5洗涤1.hex";
    public static String wash1TemperatureParm= "xidiye_temperature_setting";
    public static String wash1XidiyeVolumeParm= "xidiye_height_setting";
    public static String wash1LoopParm= "xidiye_mixloop_setting";
    public static String wash1XidiyeRelativeVolumeParm= "xidiye_relative_height_setting";

    public static String wash22HexFile = HEXFILE_PATH + "V5-6洗涤22.hex";
    public static String wash22TemperatureParm= "xidiye_temperature_setting";
    public static String wash22XidiyeVolumeParm= "xidiye_height_setting";
    public static String wash22LoopParm= "xidiye_mixloop_setting";
    public static String wash22XidiyeRelativeVolumeParm= "xidiye_relative_height_setting";

    public static String wash32HexFile = HEXFILE_PATH + "V5-7洗涤32.hex";
    public static String wash32TemperatureParm= "xidiye_temperature_setting";
    public static String wash32XidiyeVolumeParm= "xidiye_height_setting";
    public static String wash32LoopParm= "xidiye_mixloop_setting";

    public static String wash33HexFile = HEXFILE_PATH + "V5-8洗涤33.hex";
    public static String wash33TemperatureParm= "xidiye_temperature_setting";
    public static String wash33XidiyeVolumeParm= "xidiye_height_setting";
    public static String wash33LoopParm= "xidiye_mixloop_setting";

    public static String elutionHexFile = HEXFILE_PATH + "V5-9洗脱.hex";
    public static String elutionTemperatureParm= "xituoye_temperature_setting";
    public static String elutionVolumeParm= "xituoye_height_setting";
    public static String elutionLoopParm= "xituoye_mixloop_setting";
    public static String elutionRelativeVolumeParm= "xituoye_relative_height_setting";


    public static String quantify5ulmoreHexFile = HEXFILE_PATH + "V5-11定量.hex";
    public static String quantifyVolumeParm= "dingliang_height_setting";

    public static String secondHexFile = HEXFILE_PATH + "V5-12多重二次试剂.hex";

    public static String systemFreezeDryingHexFile = HEXFILE_PATH + "V5-13冻干体系.hex";
    public static String systemLiquidHexFile = HEXFILE_PATH + "V5-13液态体系.hex";


    public static List<HexDebugEntity> readFromSubHEX(String filename){

        String fullFilePath = filename;

        File file = new File(fullFilePath);

        if(!file.exists()){
            return null;
        }

        List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(file);

        return HexDebugEntityList;

    }

    public static List<HexDebugEntity> setSplitTemperatureHex(HexConfig hexConfig){
        List<HexDebugEntity> temperatireHexDebugEntityList = readFromSubHEX(temperatureHexFile);
        if(temperatireHexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < temperatireHexDebugEntityList.size(); i++)
        {
            if(temperatireHexDebugEntityList.get(i).getDescription().contains(splitTemperatureParm)){
                for (int j = 0; j < temperatireHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(temperatireHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(splitTemperatureParm)){
                        temperatireHexDebugEntityList.get(i).getHexActionList().get(j).setHex(NumberUtils.doubleto0String(hexConfig.getSplitTemperature()));

                        temperatireHexDebugEntityList.get(i).setDescription
                                (temperatireHexDebugEntityList.get(i).getDescription()+ ":" + NumberUtils.doubleto0String(hexConfig.getSplitTemperature()));

                        temperatireHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (temperatireHexDebugEntityList.get(i).getDescription()+ ":" + NumberUtils.doubleto0String(hexConfig.getSplitTemperature()));


                    }
                }
            }
        }
        return temperatireHexDebugEntityList;
    }

    public static List<HexDebugEntity> readCasseteHex(){
        List<HexDebugEntity> casseteHexDebugEntityList = readFromSubHEX(casseteHexFile);
        if(casseteHexDebugEntityList == null){
            return null;
        }
        return casseteHexDebugEntityList;
    }

    //////////新的计算高度方法
    public static double caculateAbsHeight(String description,String label, double volume){

        String text = description.replace(label,"");

        double height = 0;

        if(text.equals("")){
            height =  positionWithBufferCalculate(volume);
            return height;
        }

        String symbol = text.substring(text.length()-1);

        String num = text.substring(0,text.length()-1);

        height = NumberUtils.StringtoDouble(num);

        if(symbol.equals("-")){

            height = height*1000 - positionWithBufferCalculateNew(volume);

        }else if(symbol.equals("+")){

            height = height*1000 + positionWithBufferCalculateNew(volume);

        }else{

            height =  positionWithBufferCalculate(volume);

        }
        return height;
    }


    public static List<HexDebugEntity> setMagneticSampleHex(HexConfig hexConfig){
        List<HexDebugEntity> magneticHexDebugEntityList = readFromSubHEX(magneticHexFile);
        if(magneticHexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < magneticHexDebugEntityList.size(); i++)
        {
            if(magneticHexDebugEntityList.get(i).getDescription().contains(magneticSampleVolumeParm)){
                for (int j = 0; j < magneticHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(magneticSampleVolumeParm)){

                        double height = caculateAbsHeight(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),magneticSampleVolumeParm, hexConfig.getMagneticVolume());

                        String newHex = getNewHexByPosition(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        magneticHexDebugEntityList.get(i).setDescription
                                (magneticHexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getMagneticVolume());

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getMagneticVolume());

                    }
                }
            }else if(magneticHexDebugEntityList.get(i).getDescription().contains(magneticTotalVolumeParm)){
                for (int j = 0; j < magneticHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(magneticTotalVolumeParm)){
                        ///////////////模板有问题
                        double height = caculateAbsHeight(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),magneticTotalVolumeParm,
                                hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume());

                        String newHex = getNewHexByPosition(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        magneticHexDebugEntityList.get(i).setDescription
                                (magneticHexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume());

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  (hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume()));

                    }
                }
            }else if(magneticHexDebugEntityList.get(i).getDescription().contains(magneticLiejieyeVolumeParm)){

                ////裂解液高度
                for (int j = 0; j < magneticHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(magneticLiejieyeVolumeParm)){

                        double height = caculateAbsHeight(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),magneticLiejieyeVolumeParm, hexConfig.getSplitLieJieYeVolume());

                        String newHex = getNewHexByPosition(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        magneticHexDebugEntityList.get(i).setDescription
                                (magneticHexDebugEntityList.get(i).getDescription() + ";" +  hexConfig.getSplitLieJieYeVolume());

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" + hexConfig.getSplitLieJieYeVolume());

                    }
                }

            }
        }
        return magneticHexDebugEntityList;
    }


    public static List<HexDebugEntity> setMagneticLiejieyeHex(HexConfig hexConfig){
        List<HexDebugEntity> magneticHexDebugEntityList = readFromSubHEX(magneticHexFile);
        if(magneticHexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < magneticHexDebugEntityList.size(); i++)
        {
            if(magneticHexDebugEntityList.get(i).getDescription().contains(magneticLiejieyeVolumeParm)){
                for (int j = 0; j < magneticHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(magneticLiejieyeVolumeParm)){

                        double height = caculateAbsHeight(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),magneticLiejieyeVolumeParm, hexConfig.getSplitLieJieYeVolume());

                        String newHex = getNewHexByPosition(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        magneticHexDebugEntityList.get(i).setDescription
                                (magneticHexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getMagneticVolume());

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getSplitLieJieYeVolume());

                    }
                }
            }else if(magneticHexDebugEntityList.get(i).getDescription().contains(magneticTotalVolumeParm)){
                for (int j = 0; j < magneticHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(magneticTotalVolumeParm)){
                        ///////////////模板有问题
                        double height = caculateAbsHeight(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),magneticTotalVolumeParm,
                                hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume());

                        String newHex = getNewHexByPosition(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        magneticHexDebugEntityList.get(i).setDescription
                                (magneticHexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume());

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  (hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume()));

                    }
                }
            }else if(magneticHexDebugEntityList.get(i).getDescription().contains(magneticLiejieyeVolumeParm)){

                ////裂解液高度
                for (int j = 0; j < magneticHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(magneticLiejieyeVolumeParm)){

                        double height = caculateAbsHeight(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),magneticLiejieyeVolumeParm, hexConfig.getSplitLieJieYeVolume());

                        String newHex = getNewHexByPosition(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        magneticHexDebugEntityList.get(i).setDescription
                                (magneticHexDebugEntityList.get(i).getDescription() + ";" +  hexConfig.getSplitLieJieYeVolume());

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" + hexConfig.getSplitLieJieYeVolume());

                    }
                }

            }else if(magneticHexDebugEntityList.get(i).getDescription().contains(magneticSampleVolumeParm)){
                for (int j = 0; j < magneticHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(magneticSampleVolumeParm)){

                        double height = caculateAbsHeight(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),magneticSampleVolumeParm, hexConfig.getMagneticVolume());

                        String newHex = getNewHexByPosition(magneticHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        magneticHexDebugEntityList.get(i).setDescription
                                (magneticHexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getMagneticVolume());

                        magneticHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (magneticHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getMagneticVolume());

                    }
                }
            }
        }
        return magneticHexDebugEntityList;
    }


    public static List<HexDebugEntity> setSplitHex(HexConfig hexConfig){
        List<HexDebugEntity> splitHexDebugEntityList = readFromSubHEX(splitHexFile);
        if(splitHexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < splitHexDebugEntityList.size(); i++)
        {
            if(splitHexDebugEntityList.get(i).getDescription().contains(splitSampleVolumeParm)){
                for (int j = 0; j < splitHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(splitSampleVolumeParm)){

                        double height = caculateAbsHeight(splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),splitSampleVolumeParm, hexConfig.getSplitLieSampleVolume());

                        String newHex = getNewHexByPosition(splitHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        splitHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        splitHexDebugEntityList.get(i).setDescription
                                (splitHexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getSplitLieSampleVolume());

                        splitHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getSplitLieSampleVolume());

                    }
                }

            }else if(splitHexDebugEntityList.get(i).getDescription().contains(splitTotalVolumeParm)){
                for (int j = 0; j < splitHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(splitTotalVolumeParm)){
                        ///////////////模板有问题
                        double height = caculateAbsHeight(splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),splitTotalVolumeParm,
                                hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume());

                        String newHex = getNewHexByPosition(splitHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        splitHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        splitHexDebugEntityList.get(i).setDescription
                                (splitHexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume());

                        splitHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  (hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume()));

                    }
                }
                ////相对高度
                for (int j = 0; j < splitHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(splitRelativeTotalVolumeParm)){

                        double height =  distanceCalculate(hexConfig.getSplitLieJieYeVolume());

                        String newHex = getNewHexByPosition(splitHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        splitHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        splitHexDebugEntityList.get(i).setDescription
                                (splitHexDebugEntityList.get(i).getDescription() + ";" +  hexConfig.getSplitLieJieYeVolume());

                        splitHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  (hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume()));

                    }
                }
                ////裂解液高度
                for (int j = 0; j < splitHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(splitLiejieyeVolumeParm)){

                        double height = caculateAbsHeight(splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),splitLiejieyeVolumeParm, hexConfig.getSplitLieJieYeVolume());

                        String newHex = getNewHexByPosition(splitHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        splitHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        splitHexDebugEntityList.get(i).setDescription
                                (splitHexDebugEntityList.get(i).getDescription() + ";" +  hexConfig.getSplitLieJieYeVolume());

                        splitHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (splitHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" + hexConfig.getSplitLieJieYeVolume());

                    }
                }
            }else if(splitHexDebugEntityList.get(i).getDescription().contains(splitLoopParm)){
                double loop =  loopCalculate(hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume(),hexConfig.getSplitTime());
                splitHexDebugEntityList.get(i).setLoop((int) loop);
            }
        }
        return splitHexDebugEntityList;
    }


    public static List<HexDebugEntity> setWash1Hex(HexConfig hexConfig){
        List<HexDebugEntity> wash1HexDebugEntityList = readFromSubHEX(wash1HexFile);
        if(wash1HexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < wash1HexDebugEntityList.size(); i++)
        {
            if(wash1HexDebugEntityList.get(i).getDescription().contains(wash1TemperatureParm)){
                for (int j = 0; j < wash1HexDebugEntityList.get(i).getHexActionList().size(); j++){

                    if(wash1HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash1TemperatureParm)){

                        wash1HexDebugEntityList.get(i).getHexActionList().get(j).setHex(NumberUtils.doubleto0String(hexConfig.getWash1Temperature()));

                        wash1HexDebugEntityList.get(i).setDescription
                                (wash1HexDebugEntityList.get(i).getDescription() + ";" + NumberUtils.doubleto0String(hexConfig.getWash1Temperature()));

                        wash1HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash1HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  NumberUtils.doubleto0String(hexConfig.getWash1Temperature()));

                    }
                }
            }else if(wash1HexDebugEntityList.get(i).getDescription().contains(wash1XidiyeVolumeParm)){
                for (int j = 0; j < wash1HexDebugEntityList.get(i).getHexActionList().size(); j++){

                    if(wash1HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash1XidiyeVolumeParm)){

                        double height = caculateAbsHeight(wash1HexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),wash1XidiyeVolumeParm, hexConfig.getWash1XiDiYeVolume());

                        String newHex = getNewHexByPosition(wash1HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        wash1HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        wash1HexDebugEntityList.get(i).setDescription
                                (wash1HexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getWash1XiDiYeVolume());

                        wash1HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash1HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getWash1XiDiYeVolume());

                    }
                }
                ///////设置相对高度
                for (int j = 0; j < wash1HexDebugEntityList.get(i).getHexActionList().size(); j++){

                    if(wash1HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash1XidiyeRelativeVolumeParm)){

                        double height =  distanceCalculate(hexConfig.getWash1XiDiYeVolume());

                        String newHex = getNewHexByPosition(wash1HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        wash1HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        wash1HexDebugEntityList.get(i).setDescription
                                (wash1HexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getWash1XiDiYeVolume());

                        wash1HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash1HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getWash1XiDiYeVolume());

                    }
                }
            }else if(wash1HexDebugEntityList.get(i).getDescription().contains(wash1LoopParm)){
                double loop =  loopCalculate(hexConfig.getWash1XiDiYeVolume(),hexConfig.getWash1Time());
                wash1HexDebugEntityList.get(i).setLoop((int) loop);
            }
        }
        return wash1HexDebugEntityList;
    }

    public static List<HexDebugEntity> setWash22Hex(HexConfig hexConfig){
        List<HexDebugEntity> wash22HexDebugEntityList = readFromSubHEX(wash22HexFile);
        if(wash22HexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < wash22HexDebugEntityList.size(); i++)
        {
            if(wash22HexDebugEntityList.get(i).getDescription().contains(wash22TemperatureParm)){
                for (int j = 0; j < wash22HexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(wash22HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash22TemperatureParm)){
                        wash22HexDebugEntityList.get(i).getHexActionList().get(j).setHex(hexConfig.getWash22Temperature()+"");

                        wash22HexDebugEntityList.get(i).setDescription
                                (wash22HexDebugEntityList.get(i).getDescription() + ";" + NumberUtils.doubleto0String(hexConfig.getWash22Temperature()));

                        wash22HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash22HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  NumberUtils.doubleto0String(hexConfig.getWash22Temperature()));

                    }
                }
            }else if(wash22HexDebugEntityList.get(i).getDescription().contains(wash22XidiyeVolumeParm)){
                for (int j = 0; j < wash22HexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(wash22HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash22XidiyeVolumeParm)){

                        double height = caculateAbsHeight(wash22HexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),wash22XidiyeVolumeParm, hexConfig.getWash22XiDiYeVolume());

                        String newHex = getNewHexByPosition(wash22HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        wash22HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        wash22HexDebugEntityList.get(i).setDescription
                                (wash22HexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getWash22XiDiYeVolume());

                        wash22HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash22HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getWash22XiDiYeVolume());

                    }
                }
                //////设置相对高度
                for (int j = 0; j < wash22HexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(wash22HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash22XidiyeRelativeVolumeParm)){

                        double height =  distanceCalculate(hexConfig.getWash22XiDiYeVolume());

                        String newHex = getNewHexByPosition(wash22HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        wash22HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        wash22HexDebugEntityList.get(i).setDescription
                                (wash22HexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getWash22XiDiYeVolume());

                        wash22HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash22HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getWash22XiDiYeVolume());

                    }
                }
            }else if(wash22HexDebugEntityList.get(i).getDescription().contains(wash22LoopParm)){
                double loop =  loopCalculate(hexConfig.getWash22XiDiYeVolume(),hexConfig.getWash22Time());
                wash22HexDebugEntityList.get(i).setLoop((int) loop);
            }
        }
        return wash22HexDebugEntityList;
    }

    public static List<HexDebugEntity> setWash32Hex(HexConfig hexConfig){
        List<HexDebugEntity> wash32HexDebugEntityList = readFromSubHEX(wash32HexFile);
        if(wash32HexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < wash32HexDebugEntityList.size(); i++)
        {
            if(wash32HexDebugEntityList.get(i).getDescription().contains(wash32TemperatureParm)){
                for (int j = 0; j < wash32HexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(wash32HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash32TemperatureParm)){
                        wash32HexDebugEntityList.get(i).getHexActionList().get(j).setHex(hexConfig.getWash32Temperature()+"");

                        wash32HexDebugEntityList.get(i).setDescription
                                (wash32HexDebugEntityList.get(i).getDescription() + ";" + NumberUtils.doubleto0String(hexConfig.getWash32Temperature()));

                        wash32HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash32HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  NumberUtils.doubleto0String(hexConfig.getWash32Temperature()));
                    }
                }
            }else if(wash32HexDebugEntityList.get(i).getDescription().contains(wash32XidiyeVolumeParm)){
                for (int j = 0; j < wash32HexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(wash32HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash32XidiyeVolumeParm)){

                        double height = caculateAbsHeight(wash32HexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),wash32XidiyeVolumeParm, hexConfig.getWash32XiDiYeVolume());

                        String newHex = getNewHexByPosition(wash32HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        wash32HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        wash32HexDebugEntityList.get(i).setDescription
                                (wash32HexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getWash32XiDiYeVolume());

                        wash32HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash32HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getWash32XiDiYeVolume());


                    }
                }
            }else if(wash32HexDebugEntityList.get(i).getDescription().contains(wash32LoopParm)){

                double loop =  loopCalculate(hexConfig.getWash32XiDiYeVolume(),hexConfig.getWash32Time());

                wash32HexDebugEntityList.get(i).setLoop((int) loop);

            }
        }
        return wash32HexDebugEntityList;
    }

    public static List<HexDebugEntity> setWash33Hex(HexConfig hexConfig){
        List<HexDebugEntity> wash33HexDebugEntityList = readFromSubHEX(wash33HexFile);
        if(wash33HexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < wash33HexDebugEntityList.size(); i++)
        {
            if(wash33HexDebugEntityList.get(i).getDescription().contains(wash33TemperatureParm)){
                for (int j = 0; j < wash33HexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(wash33HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash33TemperatureParm)){
                        wash33HexDebugEntityList.get(i).getHexActionList().get(j).setHex(hexConfig.getWash33Temperature()+"");

                        wash33HexDebugEntityList.get(i).setDescription
                                (wash33HexDebugEntityList.get(i).getDescription() + ";" + NumberUtils.doubleto0String(hexConfig.getWash33Temperature()));

                        wash33HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash33HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  NumberUtils.doubleto0String(hexConfig.getWash33Temperature()));

                    }
                }
            }else if(wash33HexDebugEntityList.get(i).getDescription().contains(wash33XidiyeVolumeParm)){
                for (int j = 0; j < wash33HexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(wash33HexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(wash33XidiyeVolumeParm)){

                        double height = caculateAbsHeight(wash33HexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),wash33XidiyeVolumeParm, hexConfig.getWash33XiDiYeVolume());

                        String newHex = getNewHexByPosition(wash33HexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        wash33HexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        wash33HexDebugEntityList.get(i).setDescription
                                (wash33HexDebugEntityList.get(i).getDescription() + ";" + hexConfig.getWash33XiDiYeVolume());

                        wash33HexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (wash33HexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getWash33XiDiYeVolume());

                    }
                }
            }else if(wash33HexDebugEntityList.get(i).getDescription().contains(wash33LoopParm)){
                double loop =  loopCalculate(hexConfig.getWash33XiDiYeVolume(),hexConfig.getWash33Time());

               wash33HexDebugEntityList.get(i).setLoop((int) loop);

            }
        }
        return wash33HexDebugEntityList;
    }

    public static List<HexDebugEntity> setXiduoHex(HexConfig hexConfig){
        List<HexDebugEntity> XiduoHexDebugEntityList = readFromSubHEX(elutionHexFile);
        if(XiduoHexDebugEntityList == null){
            return null;
        }
        for (int i = 0; i < XiduoHexDebugEntityList.size(); i++)
        {
            if(XiduoHexDebugEntityList.get(i).getDescription().contains(elutionTemperatureParm)){
                for (int j = 0; j < XiduoHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(XiduoHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(elutionTemperatureParm)){
                        XiduoHexDebugEntityList.get(i).getHexActionList().get(j).setHex(hexConfig.getElutionTemperature()+"");

                        XiduoHexDebugEntityList.get(i).setDescription
                                (XiduoHexDebugEntityList.get(i).getDescription() + ";" + NumberUtils.doubleto0String(hexConfig.getElutionTemperature()));

                        XiduoHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (XiduoHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  NumberUtils.doubleto0String(hexConfig.getElutionTime()));

                    }
                }
            }else if(XiduoHexDebugEntityList.get(i).getDescription().contains(elutionVolumeParm)){
                for (int j = 0; j < XiduoHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(XiduoHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(elutionVolumeParm)){

                        //int height =  positionWithBufferCalculate(hexConfig.getElutionVolume() - subXituoVolume);

                        double height = caculateAbsHeight(XiduoHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),elutionVolumeParm, hexConfig.getElutionVolume());

                        String newHex = getNewHexByPosition(XiduoHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        XiduoHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                    }
                }
                //////设置相对高度
                for (int j = 0; j < XiduoHexDebugEntityList.get(i).getHexActionList().size(); j++){
                    if(XiduoHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(elutionRelativeVolumeParm)){

                        double height =  distanceCalculate(hexConfig.getElutionVolume() - subXituoVolume);

                        String newHex = getNewHexByPosition(XiduoHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        XiduoHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                    }
                }

            }else if(XiduoHexDebugEntityList.get(i).getDescription().contains(elutionLoopParm)){
                double loop =  loopCalculate(hexConfig.getElutionVolume(),hexConfig.getElutionTime());

                XiduoHexDebugEntityList.get(i).setLoop((int) loop);

            }
        }
        return XiduoHexDebugEntityList;
    }


    public static List<HexDebugEntity> setDingliangHex(HexConfig hexConfig) {

        List<HexDebugEntity> DingliangHexDebugEntityList = readFromSubHEX(quantify5ulmoreHexFile);
        if (DingliangHexDebugEntityList == null) {
            return null;
        }
        for (int i = 0; i < DingliangHexDebugEntityList.size(); i++) {
            if (DingliangHexDebugEntityList.get(i).getDescription().contains(quantifyVolumeParm)) {
                for (int j = 0; j < DingliangHexDebugEntityList.get(i).getHexActionList().size(); j++) {
                    if(DingliangHexDebugEntityList.get(i).getHexActionList().get(j).getDescription().contains(quantifyVolumeParm)){

                        //double height =  distanceCalculate(hexConfig.getQuantifyVolume() - 5.0f);

                        double height = caculateAbsHeight(DingliangHexDebugEntityList.get(i).getHexActionList().get(j).getDescription(),quantifyVolumeParm, hexConfig.getQuantifyVolume());

                        String newHex = getNewHexByPosition(DingliangHexDebugEntityList.get(i).getHexActionList().get(j).getHex(), (int) height);

                        DingliangHexDebugEntityList.get(i).getHexActionList().get(j).setHex(newHex);

                        DingliangHexDebugEntityList.get(i).setDescription
                                (DingliangHexDebugEntityList.get(i).getDescription() + ":" + hexConfig.getQuantifyVolume());

                        DingliangHexDebugEntityList.get(i).getHexActionList().get(j).setDescription
                                (DingliangHexDebugEntityList.get(i).getHexActionList().get(j).getDescription() + ":" +  hexConfig.getQuantifyVolume());

                    }
                }
            }
        }
        return DingliangHexDebugEntityList;
    }

    public static List<HexDebugEntity> setSecondHex(HexConfig hexConfig){
        List<HexDebugEntity> secondHexDebugEntityList = readFromSubHEX(secondHexFile);
        if(secondHexDebugEntityList == null){
            return null;
        }
        return secondHexDebugEntityList;
    }


    public static List<HexDebugEntity> setSystemFreezeDryingHex(HexConfig hexConfig){

        List<HexDebugEntity> systemHexDebugEntityList = null;
        if(hexConfig.getFreeze_drying() == 0){
            systemHexDebugEntityList = readFromSubHEX(systemLiquidHexFile);
        }else if(hexConfig.getFreeze_drying() == 1){
            systemHexDebugEntityList = readFromSubHEX(systemFreezeDryingHexFile);
        }
        
        return systemHexDebugEntityList;
    }


    public static List<HexDebugEntity> generateFirstHex(HexConfig hexConfig){

        List<HexDebugEntity>  hexDebugEntityList= new ArrayList<HexDebugEntity>();

        List<HexDebugEntity> tempList = setSplitTemperatureHex(hexConfig);
        if(tempList!=null && hexConfig.isSplitFlag()){
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }

        tempList = readCasseteHex();
        if(tempList!=null && hexConfig.isCasseteFlag()){
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }

        if(hexConfig.getMagneticChoice()==0){
            tempList = setMagneticSampleHex(hexConfig);
        }else if(hexConfig.getMagneticChoice()==1){
            tempList = setMagneticLiejieyeHex(hexConfig);
        }
        if(tempList!=null && hexConfig.isMagneticFlag()){
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }

        tempList = setSplitHex(hexConfig);
        if(tempList!=null && hexConfig.isSplitFlag()){
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }

        tempList = setWash1Hex(hexConfig);
        if(tempList!=null && hexConfig.isWashFlag()){
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }


        if(hexConfig.getWashStep() == 2&& hexConfig.isWashFlag()){
            tempList = setWash22Hex(hexConfig);
            if(tempList!=null){
                hexDebugEntityList.addAll(tempList);
            }else if(tempList ==null){
                return null;
            }
        }else if(hexConfig.getWashStep() == 3&& hexConfig.isWashFlag()){
            tempList = setWash32Hex(hexConfig);
            if(tempList!=null){
                hexDebugEntityList.addAll(tempList);
            }else if(tempList ==null){
                return null;
            }
            tempList = setWash33Hex(hexConfig);
            if(tempList!=null){
                hexDebugEntityList.addAll(tempList);
            }else if(tempList ==null){
                return null;
            }
        }

        tempList = setXiduoHex(hexConfig);
        if(tempList!=null && hexConfig.isElutionFlag()){
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }


        tempList = setDingliangHex(hexConfig);
        if(tempList!=null && hexConfig.isQuantifyFlag()){
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }


        tempList = setSystemFreezeDryingHex(hexConfig);
        if(tempList!=null && hexConfig.isQuantifyFlag()){
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }

        tempList = setSecondHex(hexConfig);
        if (tempList != null && hexConfig.isSecondQuantifyFlag()) {
            hexDebugEntityList.addAll(tempList);
        }else if(tempList ==null){
            return null;
        }

        HexDebugEntity finishEntity = new HexDebugEntity();
        finishEntity.setSelected(true);
        finishEntity.setDescription("完成");
        finishEntity.setLoop(1);
        finishEntity.setName("finish");
        finishEntity.setNO("");
        finishEntity.setHexActionList(new ArrayList<HexSingleEntity>());
        finishEntity.setParms(null);

        hexDebugEntityList.add(finishEntity);

        return hexDebugEntityList;

    }


    public static List<HexDebugEntity> generateSecondHex(HexConfig hexConfig) {

        List<HexDebugEntity> hexDebugEntityList = new ArrayList<HexDebugEntity>();

        List<HexDebugEntity> tempList = setSecondHex(hexConfig);
        if (tempList != null && hexConfig.isSecondQuantifyFlag()) {
            hexDebugEntityList.addAll(tempList);
        }
        return hexDebugEntityList;
    }


    public static boolean deleteHexFile(HexConfig hexConfig){
        File file = new File(HEXCREATEFILE_PATH + hexConfig.getFileName() + ".hex");
        File file_Config = new File(HEXCONFIG_PATH + hexConfig.getFileName() + ".hexConfig");
        if(file.exists()){
            file.delete();
        }
        if(file_Config.exists()){
            file.delete();
        }
        return true;
    }


    public static void checkSetting(Context context){

//        PcrApplication.rnaVersion = PreferenceUtil.getInstance(context.getApplicationContext())
//                .getStringValue(SharedConstant.RNA_SYSTEM,"V5");

        if(PcrApplication.rnaVersion.equals("V5")){

            HEXFILE_PATH = HexFileName_V5.HEXFILE_PATH;
            HEXCREATEFILE_PATH = HexFileName_V5.HEXCREATEFILE_PATH;
            HEXCONFIG_PATH = HexFileName_V5.HEXCONFIG_PATH;

            fastSpeed = HexFileName_V5.fastSpeed;

            mediumSpeed = HexFileName_V5.mediumSpeed;

            slowSpeed = HexFileName_V5.slowSpeed;

            totalHeight = HexFileName_V5.totalHeight;///μm

            bufferHeight = HexFileName_V5.bufferHeight;///μm

            areaParm = HexFileName_V5.areaParm;///μm

            temperatureHexFile = HexFileName_V5.temperatureHexFile;
            casseteHexFile = HexFileName_V5.casseteHexFile;

            magneticHexFile = HexFileName_V5.magneticHexTupe1File;


            splitHexFile = HexFileName_V5.splitHexFile;
            wash1HexFile = HexFileName_V5.wash1HexFile;
            wash22HexFile = HexFileName_V5.wash22HexFile;
            wash32HexFile = HexFileName_V5.wash32HexFile;
            wash33HexFile = HexFileName_V5.wash33HexFile;
            elutionHexFile = HexFileName_V5.elutionHexFile;
            quantify5ulmoreHexFile = HexFileName_V5.quantify5ulmoreHexFile;
            secondHexFile = HexFileName_V5.secondHexFile;
            systemFreezeDryingHexFile = HexFileName_V5.systemFreezeDryingHexFile;
            systemLiquidHexFile = HexFileName_V5.systemLiquidHexFile;
        }else{

        }
    }


    public static boolean saveHexFile(Context context, String fileName, HexConfig hexConfig){

//        PcrApplication.rnaVersion = PreferenceUtil.getInstance(context.getApplicationContext())
//                .getStringValue(SharedConstant.RNA_SYSTEM,"V5");

        if(PcrApplication.rnaVersion.equals("V5")){

            HEXFILE_PATH = HexFileName_V5.HEXFILE_PATH;
            HEXCREATEFILE_PATH = HexFileName_V5.HEXCREATEFILE_PATH;
            HEXCONFIG_PATH = HexFileName_V5.HEXCONFIG_PATH;

            fastSpeed = HexFileName_V5.fastSpeed;

            mediumSpeed = HexFileName_V5.mediumSpeed;

            slowSpeed = HexFileName_V5.slowSpeed;

            totalHeight = HexFileName_V5.totalHeight;///μm

            bufferHeight = HexFileName_V5.bufferHeight;///μm

            areaParm = HexFileName_V5.areaParm;///μm

            HexFileName_V5.setHEXTemplateFIle();

            temperatureHexFile = HexFileName_V5.temperatureHexFile;
            casseteHexFile = HexFileName_V5.casseteHexFile;

            if(hexConfig.getMagneticChoice() == 0){
                magneticHexFile = HexFileName_V5.magneticHexTupe1File;
            }else if(hexConfig.getMagneticChoice() == 1){
                magneticHexFile = HexFileName_V5.magneticHexTupe2File;
            }
            splitHexFile = HexFileName_V5.splitHexFile;
            wash1HexFile = HexFileName_V5.wash1HexFile;
            wash22HexFile = HexFileName_V5.wash22HexFile;
            wash32HexFile = HexFileName_V5.wash32HexFile;
            wash33HexFile = HexFileName_V5.wash33HexFile;
            elutionHexFile = HexFileName_V5.elutionHexFile;
            quantify5ulmoreHexFile = HexFileName_V5.quantify5ulmoreHexFile;
            secondHexFile = HexFileName_V5.secondHexFile;
            systemFreezeDryingHexFile = HexFileName_V5.systemFreezeDryingHexFile;
            systemLiquidHexFile = HexFileName_V5.systemLiquidHexFile;
        }else{

        }


        FileOutputStream outputStream = null;
        List<HexDebugEntity> hexList = generateFirstHex(hexConfig);
        if(hexList == null){
            return false;
        }

        hexConfig.setFileHexPath(HEXCREATEFILE_PATH + fileName + ".hex");

        hexConfig.setFileHexConfigPath(HEXCONFIG_PATH + fileName + ".hexConfig");

        File HexDir = new File(HEXCREATEFILE_PATH);
        if(!HexDir.exists()){
            HexDir.mkdirs();
        }

        File ConfigDir = new File(HEXCONFIG_PATH);
        if(!ConfigDir.exists()){
            ConfigDir.mkdirs();
        }



        String strJson = GsonUtil.toJson(hexList,true);

        String strJson_Config = GsonUtil.toJson(hexConfig,true);

        File file = new File(HEXCREATEFILE_PATH + fileName + ".hex");
        File file_Config = new File(HEXCONFIG_PATH + fileName + ".hexConfig");

        if(HexDir.exists()){
            try {
                file.createNewFile();
                outputStream = new FileOutputStream(HEXCREATEFILE_PATH + fileName + ".hex");
                outputStream.write(strJson.getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }


        if(ConfigDir.exists()){
            try {
                file_Config.createNewFile();
                outputStream = new FileOutputStream(HEXCONFIG_PATH + fileName + ".hexConfig");
                outputStream.write(strJson_Config.getBytes());
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }


    public static boolean saveHexFile_quick(Context context, String fileName, HexConfig hexConfig){

//        PcrApplication.rnaVersion = PreferenceUtil.getInstance(context.getApplicationContext())
//                .getStringValue(SharedConstant.RNA_SYSTEM,"V5");

        if(PcrApplication.rnaVersion.equals("V5")){

            HEXFILE_PATH = HexFileName_V5.HEXFILE_PATH;
            HEXCREATEFILE_PATH = HexFileName_V5.HEXCREATEFILE_PATH;
            HEXCONFIG_PATH = HexFileName_V5.HEXCONFIG_PATH;

            fastSpeed = HexFileName_V5.fastSpeed;

            mediumSpeed = HexFileName_V5.mediumSpeed;

            slowSpeed = HexFileName_V5.slowSpeed;

            totalHeight = HexFileName_V5.totalHeight;///μm

            bufferHeight = HexFileName_V5.bufferHeight;///μm

            areaParm = HexFileName_V5.areaParm;///μm

            HexFileName_V5.setHEXTemplateFIle_quick();

            temperatureHexFile = HexFileName_V5.temperatureHexFile;
            casseteHexFile = HexFileName_V5.casseteHexFile;

            if(hexConfig.getMagneticChoice() == 0){
                magneticHexFile = HexFileName_V5.magneticHexTupe1File;
            }else if(hexConfig.getMagneticChoice() == 1){
                magneticHexFile = HexFileName_V5.magneticHexTupe2File;
            }
            splitHexFile = HexFileName_V5.splitHexFile;
            wash1HexFile = HexFileName_V5.wash1HexFile;
            wash22HexFile = HexFileName_V5.wash22HexFile;
            wash32HexFile = HexFileName_V5.wash32HexFile;
            wash33HexFile = HexFileName_V5.wash33HexFile;
            elutionHexFile = HexFileName_V5.elutionHexFile;
            quantify5ulmoreHexFile = HexFileName_V5.quantify5ulmoreHexFile;
            secondHexFile = HexFileName_V5.secondHexFile;
            systemFreezeDryingHexFile = HexFileName_V5.systemFreezeDryingHexFile;
            systemLiquidHexFile = HexFileName_V5.systemLiquidHexFile;
        }else{

        }


        FileOutputStream outputStream = null;
        List<HexDebugEntity> hexList = generateFirstHex(hexConfig);
        if(hexList == null){
            return false;
        }

        hexConfig.setFileHexPath(HEXCREATEFILE_PATH + fileName + ".hex");

        hexConfig.setFileHexConfigPath(HEXCONFIG_PATH + fileName + ".hexConfig");

        File HexDir = new File(HEXCREATEFILE_PATH);
        if(!HexDir.exists()){
            HexDir.mkdirs();
        }

        File ConfigDir = new File(HEXCONFIG_PATH);
        if(!ConfigDir.exists()){
            ConfigDir.mkdirs();
        }



        String strJson = GsonUtil.toJson(hexList,true);

        String strJson_Config = GsonUtil.toJson(hexConfig,true);

        File file = new File(HEXCREATEFILE_PATH + fileName + ".hex");
        File file_Config = new File(HEXCONFIG_PATH + fileName + ".hexConfig");

        if(HexDir.exists()){
            try {
                file.createNewFile();
                outputStream = new FileOutputStream(HEXCREATEFILE_PATH + fileName + ".hex");
                outputStream.write(strJson.getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }


        if(ConfigDir.exists()){
            try {
                file_Config.createNewFile();
                outputStream = new FileOutputStream(HEXCONFIG_PATH + fileName + ".hexConfig");
                outputStream.write(strJson_Config.getBytes());
                outputStream.flush();
                outputStream.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /*


     */

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



    public static File[] getHexConfigFileList() {

        File dir = new File(HEXCONFIG_PATH);

        if(dir.exists()) {
            File[] fileList = dir.listFiles();
            return fileList;
        }else{
            return null;
        }
    }

    public static List<HexConfig> getHexConfigListFromFile() {

        File dir = new File(HEXCONFIG_PATH);

        List<HexConfig> hexConfigList = new ArrayList<>();

        if(dir.exists()){
            File[] fileList = dir.listFiles();
            if(fileList == null) {
                return hexConfigList;
            }else{
                for (int i = 0;i < fileList.length;i++){

                    if(fileList[i].getName().contains(".hexConfig")){

                        FileInputStream inputStream = null;

                        try {

                            inputStream = new FileInputStream(fileList[i]);

                            String strJson = fileToStr(inputStream);

                            if (null == strJson || strJson == "") {
                                continue;
                            }

                            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                                @Override
                                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                                    if (src == src.longValue())
                                        return new JsonPrimitive(src.longValue());
                                    return new JsonPrimitive(src);
                                }
                            }).create();
                            HexConfig hexConfig = gson.fromJson(strJson, new TypeToken<HexConfig>() {}.getType());
                            inputStream.close();
                            hexConfigList.add(hexConfig);
                        } catch (Exception e) {
                            Log.e("HexUtils" ,e.getMessage());
                        }finally {
                            if(inputStream != null){
                                try{
                                    inputStream.close();
                                    inputStream = null;
                                }catch (Exception e){
                                    Log.e("HexUtils" ,e.getMessage());
                                }
                            }
                        }

                    }


                }
            }

        }else{
            dir.mkdirs();

        }
        return hexConfigList;
    }

    ////绝对位置计算
    public static double positionWithBufferCalculate(double volume) {

        double volumeHeight = (double) ((totalHeight -  bufferHeight - (volume/areaParm))*1000);

        if(volumeHeight<0){

            volumeHeight = 0;

        }

        return volumeHeight;
    }

    ////绝对位置计算
    public static double positionWithBufferCalculateNew(double volume) {

        double volumeHeight = (double) ((volume/areaParm)*1000);

        if(volumeHeight<0){

            volumeHeight = 0;

        }

        return volumeHeight;
    }
    /////相对位移
    public static double distanceCalculate(double volume) {

        double Height = (double) (((volume/areaParm))*1000);

        return Height;
    }

//    public static int loopCalculate(double volume,double time) {
//
//        int loop = (int) Math.ceil((time * fastSpeed) /(volume*2/areaParm));
//
//        if(loop<1){
//
//            loop = 1;
//        }
//
//        return loop;
//    }

    public static double loopCalculate(double volume,double time) {

        double loop = (double) Math.ceil((time * fastSpeed) / ((volume/areaParm + 2 * fastSpeed)*2));

        if(loop<1){

            loop = 1;
        }

        return loop;
    }


    public static String getNewHexByPosition(String hex, int positionInt){

        byte[] hexList = NumUtils.StringList16To16HexList(hex);

        hexList[6] = (byte)((positionInt >> 24) & 0xff);
        hexList[7] = (byte)((positionInt >> 16) & 0xff);
        hexList[8] = (byte)((positionInt >> 8) & 0xff);
        hexList[9] = (byte)(positionInt & 0xff);

        int crc = 0;

        for (int i = 2; i <= 9; i++)
        {
            crc = crc + (int)(hexList[i]&0xFF);
        }

        hexList[10] = (byte)((crc >> 8) & 0xff);
        hexList[11] = (byte)(crc & 0xff);

        String hexcommand = NumUtils.HexListToString16(hexList).toUpperCase();

        return hexcommand;

    }


}
