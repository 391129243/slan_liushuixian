package com.winters.boweilibrary.BoWei.utils;

import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;
import com.gidi.bio_pcr.WintersUtils.NumUtils;

import java.util.ArrayList;

public class BoWeiTaiKeHexUtils {

    public static BoWeiPCRStepEntity Create_Hotlid_Temperature_Message(double Hotlid_Temperature, int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("hotlid_temperature_set", "热盖温度设定", "", new ArrayList<Double>(), 1, 1, Hotlid_Temperature, 0, currentSection, currentCycle, currentStep);

        byte[] hotlid_temperature_message = { 0x5E, 0x0A, 0x00, 0x07, 0x0F, (byte) 0xA0, 0x1E };

        int hottid_temperature_int = (int)(Hotlid_Temperature * 100);

        hotlid_temperature_message[4] = (byte)((hottid_temperature_int >> 8) & 0xff);

        hotlid_temperature_message[5] = (byte)(hottid_temperature_int & 0xff);

        int crc = 0;

        for (int i = 0; i < hotlid_temperature_message.length - 1; i++)
        {
            crc = crc + hotlid_temperature_message[i] & 0xFF;
        }

        hotlid_temperature_message[hotlid_temperature_message.length - 1] = (byte)(crc & 0xFF);

        pcrDebugEntity.setPCRCmd(NumUtils.HexListToString16(hotlid_temperature_message));

        return pcrDebugEntity;
    }



    public static BoWeiPCRStepEntity Create_PCR_SETTemperature_Message(double PCR_temperature, double Time, int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pCRStepEntityBoWei = new BoWeiPCRStepEntity("pcr_temperature_set", "PCR温度设定", "", new ArrayList<Double>(), 1, Time, 0, PCR_temperature, currentSection, currentCycle, currentStep);

        byte[] pcr_temperature_message = { 0x5E, 0x06, 0x00, 0x0E, 0x17, 0x70, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF9};

        int pcr_temperature_int = (int)(PCR_temperature * 100);

        pcr_temperature_message[4] = (byte)((pcr_temperature_int >> 8) & 0xff);

        pcr_temperature_message[5] = (byte)(pcr_temperature_int & 0xff);

        int crc = 0;

        for (int i = 0; i < pcr_temperature_message.length - 1; i++)
        {
            crc = crc + pcr_temperature_message[i] & 0xFF;
        }

        pcr_temperature_message[pcr_temperature_message.length - 1] = (byte)(crc & 0xFF);

        pCRStepEntityBoWei.setPCRCmd(NumUtils.HexListToString16(pcr_temperature_message));

        return pCRStepEntityBoWei;
    }


    public static BoWeiPCRStepEntity Create_PCR_SETTemperature_AMP_Message(double PCR_temperature, double Time, int currentSection, String sectionName,int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pCRStepEntityBoWei = new BoWeiPCRStepEntity("pcr_temperature_set", "PCR温度设定", "", new ArrayList<Double>(), 1, Time, 0, PCR_temperature, currentSection, currentCycle, currentStep);

        byte[] pcr_temperature_message = { 0x5E, 0x06, 0x00, 0x0E, 0x17, 0x70, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF9};

        int pcr_temperature_int = (int)(PCR_temperature * 100);

        pcr_temperature_message[4] = (byte)((pcr_temperature_int >> 8) & 0xff);

        pcr_temperature_message[5] = (byte)(pcr_temperature_int & 0xff);

        int crc = 0;

        for (int i = 0; i < pcr_temperature_message.length - 1; i++)
        {
            crc = crc + pcr_temperature_message[i] & 0xFF;
        }

        pcr_temperature_message[pcr_temperature_message.length - 1] = (byte)(crc & 0xFF);

        pCRStepEntityBoWei.setPCRCmd(NumUtils.HexListToString16(pcr_temperature_message));

        pCRStepEntityBoWei.setAcquisitionAMP(true);
        pCRStepEntityBoWei.setSectionName(sectionName);

        return pCRStepEntityBoWei;
    }

    public static BoWeiPCRStepEntity Create_PCR_SETTemperature_Melt_Message(double PCR_temperature, double Time, int currentSection, String sectionName,int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pCRStepEntityBoWei = new BoWeiPCRStepEntity("pcr_temperature_set", "PCR温度设定", "", new ArrayList<Double>(), 1, Time, 0, PCR_temperature, currentSection, currentCycle, currentStep);

        byte[] pcr_temperature_message = { 0x5E, 0x06, 0x00, 0x0E, 0x17, 0x70, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xF9};

        int pcr_temperature_int = (int)(PCR_temperature * 100);

        pcr_temperature_message[4] = (byte)((pcr_temperature_int >> 8) & 0xff);

        pcr_temperature_message[5] = (byte)(pcr_temperature_int & 0xff);

        int crc = 0;

        for (int i = 0; i < pcr_temperature_message.length - 1; i++)
        {
            crc = crc + pcr_temperature_message[i] & 0xFF;
        }

        pcr_temperature_message[pcr_temperature_message.length - 1] = (byte)(crc & 0xFF);

        pCRStepEntityBoWei.setPCRCmd(NumUtils.HexListToString16(pcr_temperature_message));

        pCRStepEntityBoWei.setAcquisitionMelt(true);
        pCRStepEntityBoWei.setSectionName(sectionName);

        return pCRStepEntityBoWei;
    }


    public static BoWeiPCRStepEntity Create_Moter_Reset_AMP_Message(int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("motor_reset", "扫描电机复位", "5E 07 00 06 13 7E", new ArrayList<Double>(), 1, 1, 0, 0 ,currentSection ,currentCycle ,currentStep);
        pcrDebugEntity.setAcquisitionAMP(true);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_Moter_Reset_Melt_Message(int currentSection,String sectionName, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("motor_reset", "扫描电机复位", "5E 07 00 06 13 7E", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionAMP(true);
        pcrDebugEntity.setSectionName(sectionName);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_Moter_Reset_Message(int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("motor_reset", "扫描电机复位", "5E 07 00 06 13 7E", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionAMP(false);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_Loopstart_Message(int Loop, int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity hexDebugEntity = new BoWeiPCRStepEntity("loopstart", "循环开始点", "", new ArrayList<Double>(), Loop, 1, 0, 0, currentSection, currentCycle, currentStep);
        return hexDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_Loopend_Message(int Loop, int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity hexDebugEntity = new BoWeiPCRStepEntity("loopend", "循环结束点", "", new ArrayList<Double>(), Loop, 1, 0, 0, currentSection, currentCycle, currentStep);
        return hexDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_Heartbeat_Message()
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("set_heartbeat", "握手", "5E 01 00 05 64", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_GetCurrentPCRTemperature_Message()
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("get_current_temperature", "获取PCR当前温度", "5E 43 00 06 00 A7", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_GetCurrentPCRTemperature_Message_Pre(int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("get_current_temperature_pre", "获取熔解荧光点前温度", "5E 43 00 06 00 A7", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionMelt(true);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_GetCurrentPCRTemperature_Message_Post(int currentSection,String sectionName, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("get_current_temperature_post", "获取熔解荧光点后温度", "5E 43 00 06 00 A7", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionMelt(true);
        pcrDebugEntity.setSectionName(sectionName);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_Scan_AMP_Message(int currentSection, String sectionName,int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("motor_scan", "扫描荧光（4通道FAM、CY5、HEX、ROX）", "5E 07 00 0B 15 1D 4C 01 32 C8 E9", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionAMP(true);
        pcrDebugEntity.setSectionName(sectionName);
        return pcrDebugEntity;
    }


    public static BoWeiPCRStepEntity Create_Scan_Melt_Message(int currentSection, String sectionName,int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("motor_scan", "扫描荧光（4通道FAM、CY5、HEX、ROX）", "5E 07 00 0B 15 1D 4C 01 32 C8 E9", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionMelt(true);
        pcrDebugEntity.setSectionName(sectionName);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_GetAMPFluorescenceData_Message(int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("pcr_fluorescence_get_amp", "获取PCR数据（4通道FAM、CY5、HEX、ROX）", "5E 0C 00 07 01 01 73", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionAMP(true);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_GetAMPFluorescenceData_Message_Start(int currentSection, int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("pcr_fluorescence_get_amp_start", "获取PCR数据（4通道FAM、CY5、HEX、ROX）", "5E 0C 00 07 01 01 73", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionAMP(true);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_GetAMPFluorescenceData_Message_End(int currentSection, String sectionName,int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("pcr_fluorescence_get_amp_end", "获取PCR数据（4通道FAM、CY5、HEX、ROX）", "5E 0C 00 07 01 01 73", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionAMP(true);
        pcrDebugEntity.setSectionName(sectionName);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_GetMeltFluorescenceData_Message(int currentSection, String sectionName,int currentCycle, int currentStep)
    {
        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("pcr_fluorescence_get_melt", "获取PCR数据（4通道FAM、CY5、HEX、ROX）", "5E 0C 00 07 01 01 73", new ArrayList<Double>(), 1, 1, 0, 0, currentSection, currentCycle, currentStep);
        pcrDebugEntity.setAcquisitionMelt(true);
        pcrDebugEntity.setSectionName(sectionName);
        return pcrDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_StatusLedBlink_Message()
    {
        BoWeiPCRStepEntity hexDebugEntity = new BoWeiPCRStepEntity("statusled_blink", "状态灯闪", "5E 0F 00 06 02 75", new ArrayList<Double>(), 1, 1, 0, 0 ,0 ,0 ,0);
        return hexDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_StatusLedBright_Message()
    {
        BoWeiPCRStepEntity hexDebugEntity = new BoWeiPCRStepEntity("statusled_bright", "状态灯亮", "5E 0F 00 06 01 74", new ArrayList<Double>(), 1, 1, 0, 0 ,0 ,0 ,0);
        return hexDebugEntity;
    }

    public static BoWeiPCRStepEntity Create_StopPeltier_Message()
    {
        BoWeiPCRStepEntity hexDebugEntity = new BoWeiPCRStepEntity("stop_peltier", "停止波尔贴", "5E 06 00 0E FF FF 0F A0 0F A0 00 64 00 32", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
        return hexDebugEntity;
    }

}
