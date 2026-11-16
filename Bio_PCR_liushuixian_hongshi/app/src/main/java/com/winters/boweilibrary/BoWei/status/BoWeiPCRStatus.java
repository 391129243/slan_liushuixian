package com.winters.boweilibrary.BoWei.status;

import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeData;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeTempretureData;

import java.util.ArrayList;
import java.util.List;

public class BoWeiPCRStatus {

    public static int heartBeatCount  = 0;

    public static int heartBeatCount_reconnect  = 3;

    public static float hotlid_temperature;///热盖温度

    public static boolean Hotlid_temperature_reach;///热盖到温标志

    public static List<BoWeiPCRTupeData> pCRCurrentTupeDataList = new ArrayList<BoWeiPCRTupeData>();

    public static float temperature1;///帕尔贴1温度

    public static float temperature2;///帕尔贴2热盖温度

    public static float temperature3;///帕尔贴3热盖温度

    public static List<BoWeiPCRTupeTempretureData> pCRCurrentTemperatureList = new ArrayList<BoWeiPCRTupeTempretureData>();

    public static boolean pCR_temperature_reach;///PCR导温


}
