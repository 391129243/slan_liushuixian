package com.gidi.bio_pcr.WintersUtils.constant;

import android.os.Environment;

public class HexFileName_V5 {

    public static final String HEXFILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/PCR/hexFile_v5/";

    public static final String HEXFILE_QUICK_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/PCR/hexFile_v5_quick/";

    public static final String HEXCREATEFILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/PCR/hexCreateFile_v5/";



    public static final String HEXCONFIG_PATH = HEXCREATEFILE_PATH + "hexConfig_v5/";


    //public static double fastSpeed = 6.25f; //2*1/(0.064*5) = 6.25mm/s,(导程为2mm,X=5)--混合速度 快速
    public static double fastSpeed = 12.5f; //2*1/(0.064*5) = 6.25mm/s,(导程为2mm,X=5)--混合速度 快速
    public static double mediumSpeed = 2.5f; //2*1/(0.064*5) = 6.25mm/s,(导程为2mm,X=5)--舱门及备用 中速

    public static double slowSpeed = 1.25f; //2*1/(0.064*5) = 6.25mm/s,(导程为2mm,X=5)--磁吸速度 慢速


    public static double totalHeight = 71.0f;///μm

    public static double bufferHeight = 2.0f;///μm

    public static double areaParm = 19.625f;///μm


    public static String temperatureHexFile = HEXFILE_PATH + "V5-1温控参数.hex";

    public static String casseteHexFile = HEXFILE_PATH + "V5-2卡盒入位.hex";

    public static String magneticHexTupe1File = HEXFILE_PATH + "V5-3磁珠活化管1.hex";

    public static String magneticHexTupe2File = HEXFILE_PATH + "V5-3磁珠活化管2.hex";

    public static String splitHexFile = HEXFILE_PATH + "V5-4裂解.hex";

    public static String wash1HexFile = HEXFILE_PATH + "V5-5洗涤1.hex";

    public static String wash22HexFile = HEXFILE_PATH + "V5-6洗涤22.hex";

    public static String wash32HexFile = HEXFILE_PATH + "V5-7洗涤32.hex";

    public static String wash33HexFile = HEXFILE_PATH + "V5-8洗涤33.hex";

    public static String elutionHexFile = HEXFILE_PATH + "V5-9洗脱.hex";

    public static String quantify5ulHexFile = HEXFILE_PATH + "V5-10定量5ul.hex";

    public static String quantify5ulmoreHexFile = HEXFILE_PATH + "V5-11定量大于5ul.hex";

    public static String secondHexFile = HEXFILE_PATH + "V5-12多重二次试剂.hex";

    public static String systemFreezeDryingHexFile = HEXFILE_PATH + "V5-13冻干体系.hex";

    public static String systemLiquidHexFile = HEXFILE_PATH + "V5-13液态体系.hex";

    public static void setHEXTemplateFIle(){

        temperatureHexFile = HEXFILE_PATH + "V5-1温控参数.hex";

        casseteHexFile = HEXFILE_PATH + "V5-2卡盒入位.hex";

        magneticHexTupe1File = HEXFILE_PATH + "V5-3磁珠活化管1.hex";

        magneticHexTupe2File = HEXFILE_PATH + "V5-3磁珠活化管2.hex";

        splitHexFile = HEXFILE_PATH + "V5-4裂解.hex";

        wash1HexFile = HEXFILE_PATH + "V5-5洗涤1.hex";

        wash22HexFile = HEXFILE_PATH + "V5-6洗涤22.hex";

        wash32HexFile = HEXFILE_PATH + "V5-7洗涤32.hex";

        wash33HexFile = HEXFILE_PATH + "V5-8洗涤33.hex";

        elutionHexFile = HEXFILE_PATH + "V5-9洗脱.hex";

        quantify5ulHexFile = HEXFILE_PATH + "V5-10定量5ul.hex";

        quantify5ulmoreHexFile = HEXFILE_PATH + "V5-11定量大于5ul.hex";

        secondHexFile = HEXFILE_PATH + "V5-12多重二次试剂.hex";

        systemFreezeDryingHexFile = HEXFILE_PATH + "V5-13冻干体系.hex";

        systemLiquidHexFile = HEXFILE_PATH + "V5-13液态体系.hex";
    }

    public static void setHEXTemplateFIle_quick(){
        temperatureHexFile = HEXFILE_QUICK_PATH + "V5-1温控参数.hex";

        casseteHexFile = HEXFILE_QUICK_PATH + "V5-2卡盒入位.hex";

        magneticHexTupe1File = HEXFILE_QUICK_PATH + "V5-3磁珠活化管1.hex";

        magneticHexTupe2File = HEXFILE_QUICK_PATH + "V5-3磁珠活化管2.hex";

        splitHexFile = HEXFILE_QUICK_PATH + "V5-4裂解.hex";

        wash1HexFile = HEXFILE_QUICK_PATH + "V5-5洗涤1.hex";

        wash22HexFile = HEXFILE_QUICK_PATH + "V5-6洗涤22.hex";

        wash32HexFile = HEXFILE_QUICK_PATH + "V5-7洗涤32.hex";

        wash33HexFile = HEXFILE_QUICK_PATH + "V5-8洗涤33.hex";

        elutionHexFile = HEXFILE_QUICK_PATH + "V5-9洗脱.hex";

        quantify5ulHexFile = HEXFILE_QUICK_PATH + "V5-10定量5ul.hex";

        quantify5ulmoreHexFile = HEXFILE_QUICK_PATH + "V5-11定量大于5ul.hex";

        secondHexFile = HEXFILE_QUICK_PATH + "V5-12多重二次试剂.hex";

        systemFreezeDryingHexFile = HEXFILE_QUICK_PATH + "V5-13冻干体系.hex";

        systemLiquidHexFile = HEXFILE_QUICK_PATH + "V5-13液态体系.hex";
    }

}
