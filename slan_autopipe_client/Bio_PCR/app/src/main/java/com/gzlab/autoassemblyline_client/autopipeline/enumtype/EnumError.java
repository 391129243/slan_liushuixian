package com.gzlab.autoassemblyline_client.autopipeline.enumtype;

public enum EnumError {
    ERROR_PCR_Ack_PROJECTNOFIND_ERROR(1,"ProjectNoFindError"),
    ERROR_PCR_Ack_USBNOFIND_ERROR(2,"USBNoFindError"),
    ERROR_PCR_Ack_RUNNING_ERROR(3,"RunningError"),
    ERROR_PCR_Ack_WELL_ERROR(4,"WellError"),
    ERROR_PCR_Ack_PCR_DISCONNECT(5,"PCRDisconnect"),



    ERROR_UPDATE_NOFILEUPDATE(7,"NoUpdateFile"),
    ERROR_UPDATE_FAIL(8,"UpdateFail"),
    ERROR_UPDATE_VERSION_SAME(9,"SameVersion");


    int code;
    String error;
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

    EnumError(int code, String error) {
        this.code = code;
        this.error = error;
    }
}
