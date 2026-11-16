package com.gidi.bio_pcr.WintersUtils.entity;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

//////指令正在执行：   功能位0xA1  2秒上发一次，充当心跳包
//单片机将每隔2s向上位机回送指令，回送指令内容为下位机所处状态
//        单片机在指令完成，报警发生的时候，会即刻回送一条指令
//        上位机在发送指令之后，单片机会即刻回送一条响应指令代表接收到
//        指令正在执行：   功能位0xA1
//        运行状态
//        {
//        "ID": 1,
//        "status": "running",
//        "NO.": "step1-1|12",
//        "degree": 90,
//        }
//
//        空闲状态
//        {
//        "ID": 1,
//        "status": "free",
//        "NO.": "step1-1|12",
//        "degree": 90,
//        }
//
//        ID对应上位机下发指令编号，回送时与下发的指令编号相同
//        Status代表下位机状态，字符串类型，running表示正在执行指令过程中
//        degree代表加热模块温度，每次回送状态时均会回送温度信息
public class RNARunningMessage implements ByteBuilder{

    private int ID;
    private String status;
    private String NO;
    private int degree;
    private int Sense;
    private String alarm;
    private String motorID;

    public String getAlarm() {
        return alarm;
    }

    public void setAlarm(String alarm) {
        this.alarm = alarm;
    }

    public String getMotorID() {
        return motorID;
    }

    public void setMotorID(String motorID) {
        this.motorID = motorID;
    }


    public void setID(int ID) {
        this.ID = ID;
    }
    public int getID() {
        return ID;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getStatus() {
        return status;
    }

    public void setNO(String NO) {
        this.NO = NO;
    }
    public String getNO() {
        return NO;
    }

    public void setDegree(int degree) {
        this.degree = degree;
    }
    public int getDegree() {
        return degree;
    }

    public int getSense() {
        return Sense;
    }

    public void setSense(int sense) {
        Sense = sense;
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
            sendBytes[4] = (byte) 0xA1;
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

            RNARunningMessage rNARunningMessage = gson.fromJson(text,RNARunningMessage.class);

            return rNARunningMessage;
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

    public RNARunningMessage(int ID, String status, String NO, int degree, String alarm, String motorID) {
        this.ID = ID;
        this.status = status;
        this.NO = NO;
        this.degree = degree;
        this.alarm = alarm;
        this.motorID = motorID;
    }

    public RNARunningMessage(int ID, String status, String NO, int degree, int sense, String alarm, String motorID) {
        this.ID = ID;
        this.status = status;
        this.NO = NO;
        this.degree = degree;
        Sense = sense;
        this.alarm = alarm;
        this.motorID = motorID;
    }

    public RNARunningMessage() {
    }
}
