package com.gzlab.autoassemblyline_client.event;

/**
 * WSS（主控）连接状态事件
 * 由 WSSModuleManager 连接检查器每 2 秒发布一次，用于界面显示
 */
public class WSSStatusEvent {
    // socket 实际是否已连接
    public boolean connected;
    // 是否正在重连
    public boolean reconnecting;
    // 主控地址
    public String serverUri;
    // 距离下次重连还有多少毫秒（已连接时为 0）
    public long nextRetryMs;
}
