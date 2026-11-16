package com.winters.boweilibrary.BoWei.bioweyenum;

/**
 * 运行状态枚举
 * 未运行
 * 等待运行
 * 正在运行
 * 准备中断
 * 中断
 * 完成
 */
public enum RunningState {
    None(0,"未运行"),
    WaitingRunning(1,"等待运行"),
    Running(2,"正在运行"),
    WaitingInterrupt(3,"准备中断"),
    Interrupt(4,"中断"),
    Finished(5,"完成");

    private int code;
    private String name;
    RunningState(int code,String name){
        this.code = code;
        this.name= name;
    }
}
