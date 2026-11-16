package com.winters.boweilibrary.autopipeline.model.message.PCRMessage;


import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class SLANPCRLogMessage extends BaseMessage {
    public String fileName;
    // 本次上传日志文本内容
    public String logContent;
    // 是否是全量文件上传标记
    public boolean isFullFile;
    // 本次文件总长度（辅助校验）
    public long totalFileLen;

    public boolean isCrashLog;

    public String getFileName() {
        return fileName;
    }

    public String getLogContent() {
        return logContent;
    }

    public boolean isFullFile() {
        return isFullFile;
    }

    public long getTotalFileLen() {
        return totalFileLen;
    }

    public boolean isCrashLog() {
        return isCrashLog;
    }
}
