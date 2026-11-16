package com.gzlab.autoassemblyline_client.autopipeline.enumtype;
//宏石报上来的错误
public enum EnumAlarm {
    ERROR_NONE(0,""),
    ERROR_PCR_TEMPLATE_NULL(1,"GetProjectFail"),//获取项目失败

    ERROR_Interrupt(2,"Run Interrupted"),
    ERROR_PCR_Communication_Error(8,"CommunicationError");

    private int code;
    private String error;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    EnumAlarm(int code, String alarm) {
        this.code = code;
        this.error = alarm;
    }
}
