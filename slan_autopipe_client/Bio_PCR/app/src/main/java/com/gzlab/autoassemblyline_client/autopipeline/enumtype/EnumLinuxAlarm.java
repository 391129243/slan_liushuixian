package com.gzlab.autoassemblyline_client.autopipeline.enumtype;

/**
 * 1.模板运行失败
 * 2.手动中断
 */
public enum EnumLinuxAlarm {

    LinuxAlarm_None(0,""),
    ERROR_Run_Project_Error(1,"RunProjectError"),//运行结果失败

    ERROR_Receive_Project_Error(2,"ReceiveProjecrError"),//接收模板错误

    ERROR_PCR_DISCONNECT(10,"PCRDisconnect"),//PCR断连
    ERROR_Manual_Interrupt(3,"ManualInterrupt"),

    ERROR_Interrupt(5,"Run Interrupted"),
    ERROR_Communication_Error(6,"CommunicationError"),
    ERROR_PCR_GET_EXPERIMENT_FAIL(7,"GetExperimentFail"),
    ERROR_PCR_RECONNECT_FAILED(8,"PcrReconnectFail");


    public int getCode() {
        return code;
    }
    private int code;
    private String LinuxAlarm;

    public void setCode(int code) {
        this.code = code;
    }

    public String getLinuxAlarm() {
        return LinuxAlarm;
    }

    public void setLinuxAlarm(String linuxAlarm) {
        LinuxAlarm = linuxAlarm;
    }

     EnumLinuxAlarm(int code, String linuxAlarm) {
        this.code = code;
        this.LinuxAlarm = linuxAlarm;
    }
}
