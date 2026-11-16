package com.gzlab.autoassemblyline_client.WintersUtils.entity;


import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

//加热控制：   功能位0x02
//        {
//        "ID": 1,
//        "cmd": "heat",
//        "NO.": "step1-1|12",
//        "temperature": 90
//        }
//        Temperature代表温度参数，整数类型，参数为0时代表停止加热
public class HeatActionCmd implements ByteBuilder {
    private int ID;
    private String cmd;
    private String NO;
    private int temperature;
    public void setID(int ID) {
        this.ID = ID;
    }
    public int getID() {
        return ID;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }
    public String getCmd() {
        return cmd;
    }

    public void setNO(String NO) {
        this.NO = NO;
    }
    public String getNO() {
        return NO;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }
    public int getTemperature() {
        return temperature;
    }

    @Override
    public byte[] encodeBytes()  {
        if (this == null) {
            return null;
        }
        try {

            Gson gson = new Gson();
            //转换成json数据，再保存
            String jsonText = gson.toJson(this);
            if (jsonText == null) {
                return null;
            }
            byte[] sendBytes = new byte[jsonText.length() + 10];

            byte[] jsonTextBytes = jsonText.getBytes(StandardCharsets.UTF_8);

            sendBytes[0] = 0x5A;
            sendBytes[1] = (byte) 0xA5;
            sendBytes[2] = (byte)((jsonTextBytes.length >> 8) & 0xff);
            sendBytes[3] = (byte)((jsonTextBytes.length) & 0xff);
            sendBytes[4] = 0x02;
            sendBytes[sendBytes.length - 1] = (byte) 0xFF;
            sendBytes[sendBytes.length - 2] = (byte) 0xFC;
            sendBytes[sendBytes.length - 3] = (byte) 0xFF;
            System.arraycopy(jsonTextBytes, 0, sendBytes, 5, jsonTextBytes.length);
            byte[] crc = getCRCBytes(jsonTextBytes);
            sendBytes[sendBytes.length - 5] = crc[0];
            sendBytes[sendBytes.length - 4] = crc[1];
            return sendBytes;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    public ByteBuilder decodeBytes(byte[] rawData)  {
        return null;
    }

    @Override
    public int getMsdID()  {
        return ID;
    }

    private  byte[] getCRCBytes(byte[] jsonTextBytes)
    {
        byte[] crc = new byte[2];

        int sum = 0;

        for(byte a:jsonTextBytes)
        {
            sum += a;
        }

        crc[0] = (byte)((sum >> 8) & 0xFF);

        crc[1] = (byte)((sum) & 0xFF);

        return crc;
    }
}
