package com.winters.boweilibrary.BoWei.bioweyenum;

/**
 * 设备状态枚举
 * 空闲中
 * 等待运行
 * 运行中
 * 准备中断
 * 运行中断
 * 运行完成
 * 通信出错
 */
public enum DeviceState {
    None(0,"空闲中"),
    WaitingRunning(1,"等待运行"),
    Running(2,"运行中"),
    WaitingInterrupt(3,"准备中断"),
    Interrupt(4,"运行中断"),
    Finished(5,"运行完成"),
    Error(6,"通信出错");

    private int code;
    private String desc;
    DeviceState(int code,String desc){
        this.code = code;
        this.desc= desc;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
