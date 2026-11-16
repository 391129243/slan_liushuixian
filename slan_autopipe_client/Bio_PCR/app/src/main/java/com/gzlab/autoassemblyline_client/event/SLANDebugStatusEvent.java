package com.gzlab.autoassemblyline_client.event;

/**
 * SLAN PCR 调试状态事件
 * 用于在界面显示 SLANModuleManager 的关键状态信息
 */
public class SLANDebugStatusEvent {
    // 连接状态
    public boolean isPcrConnected;
    public String deviceState;

    // 运行状态
    public boolean isRunning;
    public String status;
    public String linuxStatus;

    // 当前实验
    public String pcrTemplateName;
    public String runningPcrName;

    // 告警信息
    public String alarm;
    public String linuxAlarm;

    // 错误计数
    public int slanErrorCount;
    public int reconnectCount;

    // 守卫状态
    public boolean finishReported;
    public int finishSessionToken;
    public int currentSessionToken;

    // 文件路径
    public String currentTemplateDirPath;
    public String experimentFilePath;

    // 时间戳
    public long timestamp;

    public SLANDebugStatusEvent() {
        this.timestamp = System.currentTimeMillis();
    }
}
