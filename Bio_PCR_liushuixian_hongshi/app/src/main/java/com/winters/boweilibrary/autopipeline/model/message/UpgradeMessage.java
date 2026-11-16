package com.winters.boweilibrary.autopipeline.model.message;

public class UpgradeMessage extends BinBaseMessage {

//    升级消息采用二进制流方式封装发送，数据由包头，JSON结构化数据和非结构化的文件流数据拼接而成，非结构化文件数据大小由JSON结构化信息里给出。二进制数据以小端格式上报，相应以小端格式解析。
//    需要二进制封装
//    "Type":"Upgrade"

    private String Version;

    public String getVersion() {
        return Version;
    }

    public void setVersion(String version) {
        Version = version;
    }

    public UpgradeMessage() {
        super();
    }
}
