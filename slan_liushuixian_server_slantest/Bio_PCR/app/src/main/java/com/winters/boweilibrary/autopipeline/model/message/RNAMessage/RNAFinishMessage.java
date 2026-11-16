package com.winters.boweilibrary.autopipeline.model.message.RNAMessage;

import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class RNAFinishMessage extends BaseMessage {


//    "Type": "RNAFinnish",
//    "SeqId": 1,
//    "DeviceID": "2740835",
//    "DeviceType: "XXXX"，
//    "Position": "1" ////用于上位机返回自己记录的设定位置
//    "Status": "",    //ERROR、Done
//    "info": ,  "",     //正在运行的指令信息，根据许老师要显示的内容进行定制，如果是正常完成为“”，异常则附带异常指令的信息，用于主控判别是否需要断电继续执行或者是复位
//    "Alarm": ""用于弹出错误对话框提示信息
//    "No":"""NO":"test|00|2|0|2025/6/19 10:15:50"/////此处需要根据下位机的回的心跳消息进行组装（
//    因为Reset，需要用到下位机的实时状态信息）////这里也需要带下面的RNARunningStatus的异常信息？？--可能是
//    下边的“NO”、还有指令的一些注释等（是否跟下边的运行时的runningStatus，带宽足够的情况下多点冗余信息方便处理）

    private String Status;//error

    private String Info;

    private String Alarm;

    private String No;

    private String Template;

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
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

    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String template) {
        Template = template;
    }
}
