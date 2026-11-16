package com.gidi.bio_pcr.WintersUtils.entity.procedureEntity;

import com.gidi.bio_pcr.WintersUtils.entity.ByteBuilder;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

public class DoorActionDoneMessage implements ByteBuilder {

    private int ID;
    private String status;
    private String NO;
    private int degree;
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


    @Override
    public byte[] encodeBytes() {
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
            sendBytes[4] = (byte) 0xA2;
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
    public ByteBuilder decodeBytes(byte[] rawData) {
        try{
            byte[] dataBytes = new byte[rawData.length - 10];

            System.arraycopy(rawData, 5, dataBytes, 0, dataBytes.length);

            Gson gson = new Gson();

            String text = new String(dataBytes);

            DoorActionDoneMessage dooractionDoneMessage = gson.fromJson(text,DoorActionDoneMessage.class);

            return dooractionDoneMessage;
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
