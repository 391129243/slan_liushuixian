package com.winters.boweilibrary.BoWei.bioweyenum;

/**
 * 错误状态枚举
 * 运行正常 0
 * 设备连接出错 1
 * 通信出错 2
 * 关闭热盖出错 3
 * 滑盖未到位 4
 * 打开热盖出错 5
 * 发送命令出错 6
 * 模块超温出错 7
 * 热盖超温出错 8
 * 模块升降文出错 9
 * 热盖升温出错 10
 * 荧光复位失败 11
 * 设备信息错误 12
 *
 */
public enum RunningError {
    None(0,"运行正常"),
    DeviceConnectError(1,"设备连接出错"),
    CommunicationError(2,"通信出错"),
    CloseHotlidError(3,"关闭热盖出错"),
    SliderNotClose(4,"滑盖未到位"),
    OpenHotlidError(5,"打开热盖出错"),
    SendCommandError(6,"发送命令出错"),
    BlockOverTemperatureError(7,"模块超温出错"),
    HotlidOverTemperatureError(8,"热盖超温出错"),
    BlockTemperatureUpDownError(9,"模块升降温出错"),
    HotlidTemperatureRiseError(10,"热盖升温出错"),
    FluorescenceResetError(11,"荧光复位失败"),
    DeviceInfoError(12,"设备信息错误"),
    Interrupt(13,"手动中断");
    private int code;
    private String name;
    RunningError(int code,String name){
        this.code = code;
        this.name= name;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
