package com.winters.boweilibrary.autopipeline.model.item;

public class RNAStatusItem extends WSSConnectionItem{

    //free 空闲状态  acking 回复确认中（用于主控等待接受上位机的模板接受确认）  running 运行中  finish 完成实验及等待下机  error 设备处于错误状态
    private String rNAStatus = "";///下位机的状态  free、running、error /
    /// // RNAReq 有一个force选项 强制在error、ruuning的状态下跑任何新的模板

    private String rNALinuxStatus = "";//free、running、error//
    /// // RNAReq 有一个force选项 强制在error、ruuning的状态下跑任何新的模板

    private String rNALinuxAlarm = "";   ///实时显示上位机的异常文字信息文字描述上位机异常信息

    private int rNADegree; //核提下位机的实施温度

    private String rNASendAlarm = "";//下发模板时的错误，模板错误、CRC、超时不回复等

    private String rNAAlarm = "";//实时显示下位机的异常文字信息文字描述上位机异常信息

    private String rNAMotorId = "";//实时显示下位机的异常文字信息文字描述上位机异常信息

    private String NO = "";///下位机实时运行的指令信息，包括模版、循环、第几条指令等，暂时实时显示

    //正在反应的托盘，调度时需要设置并记忆到sharedperference
    private int rNACurrentTray;  ///这个用调度，记录现在运行的是哪个托盘的

    private String rNACurrentTemplate = ""; //////正在运行的RNA模板

    private String rNARunninginfo = "";/////暂时实时显示运行信息

    private int rNATotalTime = 0;//运行模板需要的总时间，直接显示

    private int rNALeftTime = 0;//正在运行模板剩余时间，直接显示

    private String Info = "";//正在运行模板信息，直接显示

    private int AbnormalReset = 0;//不用管，mgr会根据心跳信息，调用不同reset模板，也不用显示

    public String getrNAStatus() {
        return rNAStatus;
    }

    public void setrNAStatus(String rNAStatus) {
        this.rNAStatus = rNAStatus;
    }

    public int getrNADegree() {
        return rNADegree;
    }

    public void setrNADegree(int rNADegree) {
        this.rNADegree = rNADegree;
    }

    public String getrNAAlarm() {
        return rNAAlarm;
    }


    public void setrNAAlarm(String rNAAlarm) {
        this.rNAAlarm = rNAAlarm;
    }

    public String getrNAMotorId() {
        return rNAMotorId;
    }

    public void setrNAMotorId(String rNAMotorId) {
        this.rNAMotorId = rNAMotorId;
    }

    public String getrNASendAlarm() {
        return rNASendAlarm;
    }

    public void setrNASendAlarm(String rNASendAlarm) {
        this.rNASendAlarm = rNASendAlarm;
    }

    public String getNO() {
        return NO;
    }

    public void setNO(String NO) {
        this.NO = NO;
    }

    public int getrNACurrentTray() {
        return rNACurrentTray;
    }

    public void setrNACurrentTray(int rNACurrentTray) {
        this.rNACurrentTray = rNACurrentTray;
    }

    public String getrNACurrentTemplate() {
        return rNACurrentTemplate;
    }

    public void setrNACurrentTemplate(String rNACurrentTemplate) {
        this.rNACurrentTemplate = rNACurrentTemplate;
    }

    public String getrNARunninginfo() {
        return rNARunninginfo;
    }

    public void setrNARunninginfo(String rNARunninginfo) {
        this.rNARunninginfo = rNARunninginfo;
    }

    public int getrNATotalTime() {
        return rNATotalTime;
    }

    public void setrNATotalTime(int rNATotalTime) {
        this.rNATotalTime = rNATotalTime;
    }

    public int getrNALeftTime() {
        return rNALeftTime;
    }

    public void setrNALeftTime(int rNALeftTime) {
        this.rNALeftTime = rNALeftTime;
    }

    public String getInfo() {
        return Info;
    }

    public void setInfo(String info) {
        Info = info;
    }

    public String getrNALinuxStatus() {
        return rNALinuxStatus;
    }

    public void setrNALinuxStatus(String rNALinuxStatus) {
        this.rNALinuxStatus = rNALinuxStatus;
    }

    public String getrNALinuxAlarm() {
        return rNALinuxAlarm;
    }

    public void setrNALinuxAlarm(String rNALinuxAlarm) {
        this.rNALinuxAlarm = rNALinuxAlarm;
    }

    public int getAbnormalReset() {
        return AbnormalReset;
    }

    public void setAbnormalReset(int abnormalReset) {
        AbnormalReset = abnormalReset;
    }


}
