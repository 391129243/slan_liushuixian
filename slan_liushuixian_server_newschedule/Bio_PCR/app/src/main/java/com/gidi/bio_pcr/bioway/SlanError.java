package com.gidi.bio_pcr.bioway;

public enum SlanError {
    PROJECT_NO_FOUND(1,"项目不存在"),//项目不存在
    USB_NO_FOUND(2,"串口不存在"),//串口不存在
    RUNNING_ERROR(3,"仪器和模块正在运行中"),//仪器和模块正在运行中
    WELL_ERROR(4,"孔对象不正确"),//孔对象不正确
    PCR_DISCONNECT(5,"PCR硬件通讯通讯断连");
    int code;
    String message;
    SlanError(int code,String message){
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
