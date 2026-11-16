package com.gzlab.autoassemblyline_client.WintersUtils.entity;


import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

//报警：   功能位0xA3
//失联报警：   功能位0xA3
//        {
//        "ID": 1,
//        "status": "error",
//        "motorID": "A0",
//        "alarm": "disconnected"
//        }
//        ID回送报警发生时的指令编号，如果在开机自检中发现报警则会回送0
//        Error表示状态错误
//        MotorID代表报警电机地址，字符串类型，回送电机的十六进制地址
//        Alarm代表电机报警类型，字符串类型，disconnected代表电机无反馈失联
//
//        堵转报警：   功能位0xA3
//        {
//        "ID": 2,
//        "status": "error",
//        "motorID": "A0",
//        "alarm": "locked"
//        }
//        ID回送报警发生时的指令编号，如果在开机自检中发现报警则会回送0
//        Locked代表电机堵转
//
//        过热报警：   功能位0xA3
//        {
//        "ID": 2,
//        "status": "error",
//        "alarm": "overheated",
//        "degree": 110
//        }
//        ID回送报警发生时的指令编号，如果在开机自检中发现报警则会回送0
//        overheated代表加热模块过热
//        并会回送加热模块温度
//        作为报警指令配发与所执行指令所对应的id参数
public class WarningMessage implements ByteBuilder{

    private int ID;
    private String status;
    private String motorID;
    private String alarm;
    private int degree;



    public void setID(int ID) {
        this.ID = ID;
    }
    public int getID() {
        return ID;
    }

    public void setStatus(String statu1s) {
        this.status = statu1s;
    }
    public String getStatus() {
        return status;
    }

    public void setMotorID(String motorID) {
        this.motorID = motorID;
    }
    public String getMotorID() {
        return motorID;
    }

    public void setAlarm(String alarm) {
        this.alarm = alarm;
    }
    public String getAlarm() {
        return alarm;
    }

    public int getDegree() {
        return degree;
    }

    public void setDegree(int degree) {
        this.degree = degree;
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
            sendBytes[4] = (byte) 0xA3;
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
        try{
            byte[] dataBytes = new byte[rawData.length - 10];

            System.arraycopy(rawData, 5, dataBytes, 0, dataBytes.length);

            Gson gson = new Gson();

            String text = new String(dataBytes);

            WarningMessage warningMessage = gson.fromJson(text,WarningMessage.class);

            return warningMessage;
        }
        catch (Exception e)
        {
            return null;
        }
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
