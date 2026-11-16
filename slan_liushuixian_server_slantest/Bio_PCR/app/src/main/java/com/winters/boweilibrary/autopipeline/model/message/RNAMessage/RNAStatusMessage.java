package com.winters.boweilibrary.autopipeline.model.message.RNAMessage;

import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class RNAStatusMessage extends BaseMessage {

    private String Template;

    private String Status;

    private String LinuxStatus;/////running、free、error

    private int Degree;

    private String Alarm;

    private String LinuxAlarm;/////字符信息描述上位机状态，影响模板运行的LinuxStatus应设置成error

    private String MotorID;

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

    public int getDegree() {
        return Degree;
    }

    public void setDegree(int degree) {
        Degree = degree;
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

    public String getMotorID() {
        return MotorID;
    }

    public void setMotorID(String motorID) {
        MotorID = motorID;
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

    public RNAStatusMessage() {
    }
}
