package com.gidi.bio_pcr.WintersUtils.entity;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

//整机急停：   功能位0x05
//        {
//        "ID": 0,
//        "cmd": "stopall"
//        }
//        该指令用于操控整机全局急停
//        作为紧急指令id参数固定为0
public class ActionStopCmd implements ByteBuilder{

    private int ID;
    private String cmd;
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

    @Override
    public byte[] encodeBytes()  {
        if (this == null)
        {
            return null;
        }
        try
        {
            Gson gson = new Gson();
            //转换成json数据，再保存
            String jsonText = gson.toJson(this);
            if (jsonText == null)
            {
                return null;
            }
            byte[] sendBytes = new byte[jsonText.length() + 10];

            byte[] jsonTextBytes = jsonText.getBytes(StandardCharsets.UTF_8);

            sendBytes[0] = 0x5A;
            sendBytes[1] = (byte) 0xA5;
            sendBytes[2] = (byte)((jsonTextBytes.length >> 8) & 0xff);
            sendBytes[3] = (byte)((jsonTextBytes.length) & 0xff);
            sendBytes[4] = 0x05;
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
    public int getMsdID()  {
        return ID;
    }

    @Override
    public ByteBuilder decodeBytes(byte[] rawData)  {
        return null;
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
