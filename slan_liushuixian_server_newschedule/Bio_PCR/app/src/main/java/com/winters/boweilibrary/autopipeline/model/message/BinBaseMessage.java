package com.winters.boweilibrary.autopipeline.model.message;

public class BinBaseMessage {

    private String Type; //消息类型

    private int SeqID;//消息id

    private String DeviceID;//RNA1111  RNA2222  PCR1111 PCR222 ----PCR6666

    private String DeviceType;  ///RNA 或者 PCR

    private int Position;  ///RNA 1-2  PCR 1-6

    private String FileName;

    private int FileSize;

    private String CheckSum;

    private byte[] fileData; //二进制文件数据

    private int Force;

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public int getSeqId() {
        return SeqID;
    }

    public void setSeqId(int seqId) {
        this.SeqID = seqId;
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

    public int getForce() {
        return Force;
    }

    public void setForce(int force) {
        Force = force;
    }


    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public BinBaseMessage() {
    }


}
