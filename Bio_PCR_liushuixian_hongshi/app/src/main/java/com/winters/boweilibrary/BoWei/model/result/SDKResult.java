package com.winters.boweilibrary.BoWei.model.result;

/**
 * 返回数据
 */
public class SDKResult {
    private int Code;//返回代码，0--成功，其他参见错误码对照表
    private String Message;//返回信息
    private String Data;//返回数据

    public int getCode() {
        return Code;
    }

    public void setCode(int code) {
        Code = code;
    }

    public String getMessage() {
        return Message;
    }

    public void setMessage(String message) {
        Message = message;
    }

    public String getData() {
        return Data;
    }

    public void setData(String data) {
        Data = data;
    }
}
