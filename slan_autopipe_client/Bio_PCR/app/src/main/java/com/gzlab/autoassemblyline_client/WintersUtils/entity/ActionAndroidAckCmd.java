package com.gzlab.autoassemblyline_client.WintersUtils.entity;


import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

//通讯响应：   功能位0x03
//        {
//        "ID": 1,
//        "cmd": "PCreceived"
//        }
//        该指令用于回复反馈信息已接收到
//        作为响应指令配发与回送指令所对应的id参数
//        （不对单片机的通讯响应指令做响应）
public class ActionAndroidAckCmd implements ByteBuilder {
    private int ID;
    private String cmd;

    public ActionAndroidAckCmd() {

    }

    public ActionAndroidAckCmd(int ID, String cmd) {
        this.ID = ID;
        this.cmd = cmd;
    }

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

            sendBytes[0] = (byte)0x5A;
            sendBytes[1] = (byte) 0xA5;
            sendBytes[2] = (byte)((jsonTextBytes.length >> 8) & 0xff);
            sendBytes[3] = (byte)((jsonTextBytes.length) & 0xff);
            sendBytes[4] = (byte)0x03;
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
