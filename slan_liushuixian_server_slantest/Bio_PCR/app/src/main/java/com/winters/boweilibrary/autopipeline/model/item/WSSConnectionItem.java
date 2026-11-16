package com.winters.boweilibrary.autopipeline.model.item;

import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAAckMessage;
import com.winters.boweilibrary.autopipeline.model.event.WSSMessageEvent;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAReqMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeAckMessage;

import org.greenrobot.eventbus.EventBus;
import org.java_websocket.WebSocket;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public class WSSConnectionItem {

    private static final String TAG = "WSSConnectionItem";

    /** 模板下发最大重试次数 */
    private static final int SEND_RETRY_MAX = 5;

    /** 等待 Ack 超时时间（秒） */
    private static final int SEND_WAITING_ACK_SECONDS = 20;

    // ------------------------------------------------------------------ 字段

    private String deviceID;        // 设备唯一标识，如 RNA12345、PCR123456
    private String deviceType;      // 设备类型，如 RNA、PCR
    private String version;         // 软件版本号
    private WebSocket webSocketConnection;
    private String deviceIP;
    private String devicePort;
    private boolean isWSSConnect;   // 是否处于连接状态
    private int position;           // 设备位置，用于机械臂调度
    private int count;              // 连续未收到消息的计数，>=阈值则判定断联

    private boolean isSendingTemplate;      // 是否正在等待模板下发回复
    private int sendingTemplateRetryCount;  // 模板下发失败重试次数

    private Timer timerAck;
    private final Object timerAckLock = new Object();

    // ------------------------------------------------------------------ 构造

    public WSSConnectionItem() {}

    public WSSConnectionItem(WebSocket webSocketConnection) {
        this.webSocketConnection = webSocketConnection;
    }

    // ------------------------------------------------------------------ Getter / Setter

    public String getDeviceID()                         { return deviceID; }
    public void   setDeviceID(String deviceID)          { this.deviceID = deviceID; }

    public String getDeviceType()                       { return deviceType; }
    public void   setDeviceType(String deviceType)      { this.deviceType = deviceType; }

    public String getVersion()                          { return version; }
    public void   setVersion(String version)            { this.version = version; }

    public WebSocket getWebSocketConnection()                           { return webSocketConnection; }
    public void      setWebSocketConnection(WebSocket conn)             { this.webSocketConnection = conn; }

    public String getDeviceIP()                         { return deviceIP; }
    public void   setDeviceIP(String deviceIP)          { this.deviceIP = deviceIP; }

    public String getDevicePort()                       { return devicePort; }
    public void   setDevicePort(String devicePort)      { this.devicePort = devicePort; }

    public boolean isWSSConnect()                       { return isWSSConnect; }
    public void    setWSSConnect(boolean connect)       { isWSSConnect = connect; }

    public int  getPosition()                           { return position; }
    public void setPosition(int position)               { this.position = position; }

    public int  getCount()                              { return count; }
    public void setCount(int count)                     { this.count = count; }

    public boolean isSendingTemplate()                          { return isSendingTemplate; }
    public void    setSendingTemplate(boolean sending)          { isSendingTemplate = sending; }

    public int  getSendingTemplateRetryCount()                          { return sendingTemplateRetryCount; }
    public void setSendingTemplateRetryCount(int count)                 { this.sendingTemplateRetryCount = count; }

    public Timer getTimerAck()                          { return timerAck; }
    public void  setTimerAck(Timer timerAck)            { this.timerAck = timerAck; }

    // ------------------------------------------------------------------ 发送基础消息

    /** 发送文本消息，返回是否成功 */
    public boolean sendWSSMessage(String text) {
        if (!isConnectionAlive()) {
            Log.e(TAG, "sendWSSMessage: connection closed, " + addr());
            return false;
        }
        try {
            webSocketConnection.send(text);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "sendWSSMessage error " + addr() + ": " + e.getMessage());
            return false;
        }
    }

    /** 发送二进制消息，返回是否成功 */
    public boolean sendWSSMessage(ByteBuffer buffer) {
        if (!isConnectionAlive()) {
            Log.e(TAG, "sendWSSMessage(bin): connection closed, " + addr());
            return false;
        }
        try {
            Log.e(TAG, "sendWSSMessage(bin) buffer: " + buffer.remaining() + " bytes");
            webSocketConnection.send(buffer.array());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "sendWSSMessage(bin) error " + addr() + ": " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ 发送模板（含超时重试）

    /**
     * 发送文本模板，发送成功后启动 Ack 超时重试定时器。
     * 若当前正在等待 Ack（isSendingTemplate == true），拒绝本次发送。
     */
    public boolean sendTemplateWSSMessage(String text) {
        if (isSendingTemplate) {
            Log.e(TAG, "sendTemplateWSSMessage: still waiting ack, " + addr());
            return false;
        }
        Log.i(TAG, "sendTemplateWSSMessage: " + addr() + " text=" + text);
        try {
            stopAckTimer();
            isSendingTemplate = true;
            sendingTemplateRetryCount = 0;
            if (!sendWSSMessage(text)) {
                resetSendingState();
                Log.e(TAG, "sendTemplateWSSMessage: send failed, " + addr());
                return false;
            }
            startAckTimer(text);
            return true;
        } catch (Exception e) {
            resetSendingState();
            Log.e(TAG, "sendTemplateWSSMessage error " + addr() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * 发送二进制模板，发送成功后启动 Ack 超时重试定时器。
     * 若当前正在等待 Ack（isSendingTemplate == true），拒绝本次发送。
     */
    public boolean sendTemplateWSSMessage(ByteBuffer buffer, String msgType) {
        if (isSendingTemplate) {
            Log.e(TAG, "sendTemplateWSSMessage(bin): still waiting ack, " + addr());
            return false;
        }
        try {
            stopAckTimer();
            isSendingTemplate = true;
            sendingTemplateRetryCount = 0;
            if (!sendWSSMessage(buffer)) {
                resetSendingState();
                Log.e(TAG, "sendTemplateWSSMessage(bin): send failed, " + addr());
                return false;
            }
            Log.e(TAG, "sendTemplateWSSMessage(bin): send succeed, " + addr());
            startAckTimer(buffer, msgType);
            return true;
        } catch (Exception e) {
            resetSendingState();
            Log.e(TAG, "sendTemplateWSSMessage(bin) error " + addr() + ": " + e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------ Ack 超时定时器（文本）

    /**
     * 启动文本模板的 Ack 超时重试定时器。
     * 超时后自动重发；超过 SEND_RETRY_MAX 次则发送超时错误事件。
     */
    public boolean startRNAAckTimer(String text) {
        return startAckTimer(text);
    }

    public boolean startPCRAckTimer(String text) {
        return startAckTimer(text);
    }

    private boolean startAckTimer(final String text) {
        synchronized (timerAckLock) {
            if (timerAck != null) return false;
            timerAck = new Timer();
            WSSConnectionItem self = this;
            timerAck.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.e(TAG, "AckTimer timeout #" + sendingTemplateRetryCount + " " + addr() + " text=" + text);
                    stopAckTimer();
                    if (++sendingTemplateRetryCount >= SEND_RETRY_MAX) {
                        Log.e(TAG, "AckTimer timeout exceeded max retries, " + addr());
                        // 发送超时错误事件，模拟设备回复
                        if (self instanceof RNAStatusItem) {
                            postRNAAckTimeout(self);
                        } else if (self instanceof PCRStatusItem) {
                            postPCRAckTimeout(self);
                        }
                        resetSendingState();
                        return;
                    }
                    // 重新序列化并更新时间戳后重发
                    RNAReqMessage req = GsonUtil.fromJson(text, RNAReqMessage.class);
                    req.setMSGTime(DateUtil.getCurrentTimeYMDHMS());
                    boolean sent = sendWSSMessage(GsonUtil.toJson(req, false));
                    Log.e(TAG, "AckTimer retry #" + sendingTemplateRetryCount + " " + addr() + (sent ? " ok" : " failed"));
                    if (sent) {
                        startAckTimer(text);
                    }
                    // 发送失败时不重置 isSendingTemplate，等待下一次超时或外部停止
                }
            }, SEND_WAITING_ACK_SECONDS * 1000L);
            return true;
        }
    }

    // ------------------------------------------------------------------ Ack 超时定时器（二进制）

    /**
     * 启动二进制模板的 Ack 超时重试定时器。
     * 超时后自动重发；超过 SEND_RETRY_MAX 次则发送超时错误事件。
     */
    public boolean startRNAAckTimer(ByteBuffer buffer, String msgType) {
        return startAckTimer(buffer, msgType);
    }

    public boolean startPCRAckTimer(ByteBuffer buffer, String msgType) {
        return startAckTimer(buffer, msgType);
    }

    private boolean startAckTimer(final ByteBuffer buffer, final String msgType) {
        synchronized (timerAckLock) {
            if (timerAck != null) return false;
            timerAck = new Timer();
            WSSConnectionItem self = this;
            timerAck.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.e(TAG, "AckTimer(bin) timeout #" + sendingTemplateRetryCount
                            + " " + addr() + " msgType=" + msgType);
                    stopAckTimer();
                    if (++sendingTemplateRetryCount >= SEND_RETRY_MAX) {
                        Log.e(TAG, "AckTimer(bin) timeout exceeded max retries, " + addr());
                        postBinAckTimeout(self, msgType);
                        resetSendingState();
                        return;
                    }
                    boolean sent = sendWSSMessage(buffer);
                    Log.e(TAG, "AckTimer(bin) retry #" + sendingTemplateRetryCount
                            + " " + addr() + (sent ? " ok" : " failed"));
                    if (sent) {
                        startAckTimer(buffer, msgType);
                    } else {
                        // 发送失败，立即上报错误事件
                        postBinSendError(self, msgType);
                        resetSendingState();
                    }
                }
            }, SEND_WAITING_ACK_SECONDS * 1000L);
            return true;
        }
    }

    // ------------------------------------------------------------------ 停止定时器

    public void stopAckTimer() {
        synchronized (timerAckLock) {
            if (timerAck != null) {
                timerAck.cancel();
                timerAck.purge();
                timerAck = null;
            }
        }
    }

    // ------------------------------------------------------------------ 私有辅助

    private boolean isConnectionAlive() {
        return webSocketConnection != null
                && !webSocketConnection.isClosed()
                && !webSocketConnection.isClosing()
                && !webSocketConnection.isFlushAndClose();
    }

    /** 重置发送状态并停止定时器 */
    private void resetSendingState() {
        isSendingTemplate = false;
        sendingTemplateRetryCount = 0;
        stopAckTimer();
    }

    /** 设备地址描述，用于日志 */
    private String addr() {
        return deviceIP + ":" + devicePort + " [" + deviceID + "]";
    }

    // ------------------------------------------------------------------ 超时/错误事件构建

    private static void postRNAAckTimeout(WSSConnectionItem self) {
        RNAAckMessage ack = new RNAAckMessage();
        ack.setType("RNAAck");
        ack.setReply(" error:TIME OUT");
        ack.setDeviceID(self.getDeviceID());
        ack.setPosition(self.getPosition());
        ack.setDeviceType(self.getDeviceType());
        ack.setAlarm("send RNA template ack timeout");
        EventBus.getDefault().post(new WSSMessageEvent(
                EnumMessageEvent.WSS_Message, self, GsonUtil.toJson(ack, false)));
    }

    private static void postPCRAckTimeout(WSSConnectionItem self) {
        PCRAckMessage ack = new PCRAckMessage();
        ack.setType("PCRAck");
        ack.setReply(" ERROR:TIME OUT");
        ack.setDeviceID(self.getDeviceID());
        ack.setPosition(self.getPosition());
        ack.setDeviceType(self.getDeviceType());
        ack.setAlarm("send PCR template ack timeout");
        EventBus.getDefault().post(new WSSMessageEvent(
                EnumMessageEvent.WSS_Message, self, GsonUtil.toJson(ack, false)));
    }

    /** 二进制消息超时事件（SLANPCRReq / Upgrade / RNA Upgrade） */
    private static void postBinAckTimeout(WSSConnectionItem self, String msgType) {
        if (msgType.contains("SLANPCRReq")) {
            PCRAckMessage ack = new PCRAckMessage();
            ack.setType("PCRAck");
            ack.setReply(" ERROR:TIME OUT");
            fillDeviceInfo(ack, self);
            ack.setAlarm("send PCR template ack timeout");
            EventBus.getDefault().post(new WSSMessageEvent(
                    EnumMessageEvent.WSS_Message, self, GsonUtil.toJson(ack, false)));
        } else if (msgType.contains("Upgrade")) {
            UpgradeAckMessage ack = new UpgradeAckMessage();
            ack.setType("UpgradeAck");
            ack.setReply(" ERROR:TIME OUT");
            fillDeviceInfo(ack, self);
            ack.setAlarm(self instanceof RNAStatusItem
                    ? "send RNA Upgrade ack timeout" : "PCR upgrade ack timeout");
            EventBus.getDefault().post(new WSSMessageEvent(
                    EnumMessageEvent.WSS_Message, self, GsonUtil.toJson(ack, false)));
        }
    }

    /** 二进制消息发送失败事件 */
    private static void postBinSendError(WSSConnectionItem self, String msgType) {
        if (msgType.contains("SLANPCRReq")) {
            PCRAckMessage ack = new PCRAckMessage();
            ack.setType("PCRAck");
            ack.setReply(" ERROR:send error");
            fillDeviceInfo(ack, self);
            ack.setAlarm("send PCR template error");
            EventBus.getDefault().post(new WSSMessageEvent(
                    EnumMessageEvent.WSS_Message, self, GsonUtil.toJson(ack, false)));
        } else if (msgType.contains("Upgrade")) {
            UpgradeAckMessage ack = new UpgradeAckMessage();
            ack.setType("UpgradeAck");
            ack.setReply(" ERROR:send error");
            fillDeviceInfo(ack, self);
            ack.setAlarm(self instanceof RNAStatusItem
                    ? "send RNA Upgrade error" : "send PCR template error");
            EventBus.getDefault().post(new WSSMessageEvent(
                    EnumMessageEvent.WSS_Message, self, GsonUtil.toJson(ack, false)));
        }
    }

    /** 将设备信息填入 RNAAckMessage（设备 ID / 位置 / 类型） */
    private static void fillDeviceInfo(RNAAckMessage ack, WSSConnectionItem self) {
        ack.setDeviceID(self.getDeviceID());
        ack.setPosition(self.getPosition());
        ack.setDeviceType(self.getDeviceType());
    }

    /** 将设备信息填入 PCRAckMessage */
    private static void fillDeviceInfo(PCRAckMessage ack, WSSConnectionItem self) {
        ack.setDeviceID(self.getDeviceID());
        ack.setPosition(self.getPosition());
        ack.setDeviceType(self.getDeviceType());
    }

    /** 将设备信息填入 UpgradeAckMessage */
    private static void fillDeviceInfo(UpgradeAckMessage ack, WSSConnectionItem self) {
        ack.setDeviceID(self.getDeviceID());
        ack.setPosition(self.getPosition());
        ack.setDeviceType(self.getDeviceType());
    }
}
