package com.paul.pipeline.message;

public class ErrorInfoMessage {
    int index;
    int type;
    int alarmtype;
    String linuxalarm;
    String alarm;
    String sendalarm;
    String motoid;
    boolean reset;
    boolean alarmdialog;
    ArmworkMessage armworkMessage;

    public ArmworkMessage getArmworkMessage() {
        return armworkMessage;
    }

    public void setArmworkMessage(ArmworkMessage armworkMessage) {
        this.armworkMessage = armworkMessage;
    }

    public boolean isAlarmdialog() {
        return alarmdialog;
    }

    public void setAlarmdialog(boolean alarmdialog) {
        this.alarmdialog = alarmdialog;
    }

    public boolean isReset() {
        return reset;
    }

    public void setReset(boolean reset) {
        this.reset = reset;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getAlarmtype() {
        return alarmtype;
    }

    public void setAlarmtype(int alarmtype) {
        this.alarmtype = alarmtype;
    }

    public String getAlarm() {
        return alarm;
    }

    public void setAlarm(String alarm) {
        this.alarm = alarm;
    }

    public String getLinuxalarm() {
        return linuxalarm;
    }

    public void setLinuxalarm(String linuxalarm) {
        this.linuxalarm = linuxalarm;
    }

    public String getSendalarm() {
        return sendalarm;
    }

    public void setSendalarm(String sendalarm) {
        this.sendalarm = sendalarm;
    }

    public String getMotoid() {
        return motoid;
    }

    public void setMotoid(String motoid) {
        this.motoid = motoid;
    }
}
