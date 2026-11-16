package com.winters.boweilibrary.BoWei.event;

import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeData;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeTempretureData;

import java.util.List;

public class BoWeiRecvDataInfo {

    private String recvData;

    private String hex;

    private String cmd;

    private String result;

    private boolean isPCRTemperatureReach;

    private List<BoWeiPCRTupeTempretureData> temperature;

    private List<BoWeiPCRTupeData> fluorescence;

    public String getRecvData() {
        return recvData;
    }

    public void setRecvData(String recvData) {
        this.recvData = recvData;
    }

    public String getHex() {
        return hex;
    }

    public void setHex(String hex) {
        this.hex = hex;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isPCRTemperatureReach() {
        return isPCRTemperatureReach;
    }

    public void setPCRTemperatureReach(boolean PCRTemperatureReach) {
        isPCRTemperatureReach = PCRTemperatureReach;
    }

    public List<BoWeiPCRTupeTempretureData> getTemperature() {
        return temperature;
    }

    public void setTemperature(List<BoWeiPCRTupeTempretureData> temperature) {
        this.temperature = temperature;
    }

    public List<BoWeiPCRTupeData> getFluorescence() {
        return fluorescence;
    }

    public void setFluorescence(List<BoWeiPCRTupeData> fluorescence) {
        this.fluorescence = fluorescence;
    }

    public BoWeiRecvDataInfo(String obj, String hex, String cmd, String result)
    {
        this.recvData = obj;

        this.hex = hex;

        this.cmd = cmd;

        this.result = result;
    }

    public BoWeiRecvDataInfo(String obj, String hex, String cmd, String result, List<BoWeiPCRTupeTempretureData> Temp, List<BoWeiPCRTupeData> Flu)
    {
        this.recvData = obj;

        this.hex = hex;

        this.cmd = cmd;

        this.result = result;

        this.temperature = Temp;

        this.fluorescence = Flu;
    }

    public BoWeiRecvDataInfo(String obj, String hex, String cmd, String result, List<BoWeiPCRTupeTempretureData> Temp, List<BoWeiPCRTupeData> Flu, boolean isPCRTemperatureReach)
    {
        this.recvData = obj;

        this.hex = hex;

        this.cmd = cmd;

        this.result = result;

        this.temperature = Temp;

        this.fluorescence = Flu;

        this.isPCRTemperatureReach = isPCRTemperatureReach;
    }
}
