package com.winters.boweilibrary.autopipeline.model.message.RNAMessage;

import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class RNARunningStatusMessage extends BaseMessage {

        //"Type": "RNARunningStatus",
        //             "SeqId": 1,
        //             "DeviceID": "2740835",
        //             "DeviceType: "XXXX"，
        //            "Position": "1" ////用于上位机返回自己记录的设定位置
        //            "Status": "",    //Free、Running、ERROR
        //            "LeftTime": "",
        //            "TotalTime": "",
        //            "Degree": 39,
        //            "info": 39,      //正在运行的指令信息，根据许老师要显示的内容进行定制
        //            "Alarm": ""
        //            "Status": "",    Free、Running、ERROR
        //"Template": "13~新冠~first.hex",   ////许老师自己定义名字，如果是Rest指令则不需要判断状态，没校验错误就可以立刻执行
        //"No":"""NO":"test|00|2|0|2025/6/19 10:15:50"/////此处需要根据下位机的回

    private String Template;

    private String Status;

    private String LinuxStatus;/////running、free、error

    private int LeftTime;

    private int TotalTime;

    private String LinuxAlarm;/////字符信息描述上位机状态，影响模板运行的LinuxStatus应设置成error

    private int Degree;

    private String Info;

    private String Alarm;

    private String No;

    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String template) {
        Template = template;
    }

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public int getLeftTime() {
        return LeftTime;
    }

    public void setLeftTime(int leftTime) {
        LeftTime = leftTime;
    }

    public int getTotalTime() {
        return TotalTime;
    }

    public void setTotalTime(int totalTime) {
        TotalTime = totalTime;
    }

    public int getDegree() {
        return Degree;
    }

    public void setDegree(int degree) {
        Degree = degree;
    }

    public String getInfo() {
        return Info;
    }

    public void setInfo(String info) {
        Info = info;
    }

    public String getAlarm() {
        return Alarm;
    }

    public void setAlarm(String alarm) {
        Alarm = alarm;
    }

    public String getNo() {
        return No;
    }

    public void setNo(String no) {
        No = no;
    }

    public String getLinuxStatus() {
        return LinuxStatus;
    }

    public void setLinuxStatus(String linuxStatus) {
        LinuxStatus = linuxStatus;
    }

    public String getLinuxAlarm() {
        return LinuxAlarm;
    }

    public void setLinuxAlarm(String linuxAlarm) {
        LinuxAlarm = linuxAlarm;
    }
}
