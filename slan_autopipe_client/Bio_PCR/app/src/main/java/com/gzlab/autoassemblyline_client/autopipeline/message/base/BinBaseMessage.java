package com.gzlab.autoassemblyline_client.autopipeline.message.base;

public class BinBaseMessage {

    private String Type; //消息类型

    private int SeqID;//消息id

    private String DeviceID;//RNA1111  RNA2222  PCR1111 PCR222 ----PCR6666

    private String DeviceType;  ///RNA 或者 PCR

    private int Position;  ///RNA 1-2  PCR 1-6

    private String FileName;//发送文件的名称

    private int FileSize;

    private String CheckSum;

    private byte[] fileData; //二进制文件数据

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }


    public String getDeviceID() {
        return DeviceID;
    }

    public void setDeviceID(String deviceID) {
        this.DeviceID = deviceID;
    }

    public String getDeviceType() {
        return DeviceType;
    }

    public void setDeviceType(String deviceType) {
        this.DeviceType = deviceType;
    }

    public int getPosition() {
        return Position;
    }

    public void setPosition(int position) {
        this.Position = position;
    }

    public int getSeqID() {
        return SeqID;
    }

    public void setSeqID(int seqID) {
        SeqID = seqID;
    }

    public String getFileName() {
        return FileName;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public int getFileSize() {
        return FileSize;
    }

    public void setFileSize(int fileSize) {
        FileSize = fileSize;
    }

    public String getCheckSum() {
        return CheckSum;
    }

    public void setCheckSum(String checkSum) {
        CheckSum = checkSum;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public BinBaseMessage() {
    }


}
