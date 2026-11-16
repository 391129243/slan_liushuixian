package com.winters.boweilibrary.autopipeline.mgr;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.slan.bean.SlanFinishResult;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.util.WSSLogHelper;
import com.google.gson.Gson;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.message.LogMessageEvent;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;
import com.winters.boweilibrary.autopipeline.model.message.BinBaseMessage;
import com.winters.boweilibrary.autopipeline.model.message.HeartBeatMessage;
import com.winters.boweilibrary.autopipeline.model.message.HeartBeatReplyMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRCurveMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRFinishAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRLogMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRReqMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRRunningStatusMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRStatusMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.SLAN_PCRFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.QueryDeviceReplyMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAFinishAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNALogMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAReqMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNARunningStatusMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAStatusMessage;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.event.WSSMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeMessage;
import com.winters.boweilibrary.autopipeline.utils.BinWssMessageUtils;
import com.winters.boweilibrary.autopipeline.wssserver.WebSocketServer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WSSModuleManager {

    private static final String TAG = "WSSModuleManager";

    // 独立单线程池处理 PCRCurve，有界队列防止积压
    private final ExecutorService pcrCurveExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(20),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    // 独立单线程池处理 PCRFinish / RNAFinish，不与 pcrCurveExecutor 共享
    private final ExecutorService pcrFinishExecutor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(10),
            new ThreadPoolExecutor.DiscardOldestPolicy());

    // ------------------------------------------------------------------ 字段
    private final ConcurrentHashMap<String, WSSConnectionItem> wssConnectionItems = new ConcurrentHashMap<>();
    private final Object wssConnectionItemsLock = new Object();

    private static WSSModuleManager mInstance;
    private HandleDisconnectThread mHandleDisThread;
    private int msqid = 0;
    private boolean flag_wss = false;

    // ============================================================ 单例 & 生命周期

    public static WSSModuleManager getInstance() {
        if (mInstance == null) {
            synchronized (WSSModuleManager.class) {
                if (mInstance == null) {
                    mInstance = new WSSModuleManager();
                }
            }
        }
        return mInstance;
    }

    public boolean startWSSserver() {
        boolean startWss = WebSocketServer.ready();
        registerEventBus();
        if (startWss) {
            flag_wss = true;
            if (mHandleDisThread == null || mHandleDisThread.isInterrupted()) {
                mHandleDisThread = new HandleDisconnectThread();
                mHandleDisThread.start();
            }
            return true;
        }
        return false;
    }

    public void stopWSSserver() {
        try {
            flag_wss = false;
            WebSocketServer.Stop();
            unRegisterEventBus();
        } catch (Exception e) {
            Log.e(TAG, "stopWSSserver: " + e.getMessage());
        }
    }

    public boolean getWSSConnectStatus() {
        return flag_wss;
    }

    private void registerEventBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    private void unRegisterEventBus() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    // ============================================================ 查询连接

    public ConcurrentHashMap<String, WSSConnectionItem> getWssConnectionItems() {
        return wssConnectionItems;
    }

    public WSSConnectionItem getItemByTypeAndPosition(String type, int position) {
        try {
            for (WSSConnectionItem item : wssConnectionItems.values()) {
                if (item.getDeviceType() == null) continue;
                if (item.getDeviceType().equals(type) && item.getPosition() == position) {
                    return item;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getItemByTypeAndPosition: " + e.getMessage());
        }
        return null;
    }

    public WSSConnectionItem getItemByDeviceID(String deviceID) {
        try {
            for (WSSConnectionItem item : wssConnectionItems.values()) {
                if (item.getDeviceID() == null) continue;
                if (item.getDeviceID().equals(deviceID)) return item;
            }
        } catch (Exception e) {
            Log.e(TAG, "getItemByDeviceID: " + e.getMessage());
        }
        return null;
    }

    public WSSConnectionItem getItemByIP(String ip) {
        try {
            for (WSSConnectionItem item : wssConnectionItems.values()) {
                if (item.getDeviceIP() == null) continue;
                if (item.getDeviceIP().equals(ip)) return item;
            }
        } catch (Exception e) {
            Log.e(TAG, "getItemByIP: " + e.getMessage());
        }
        return null;
    }

    public WSSConnectionItem getItemByIPAndPort(String ip, String port) {
        try {
            for (WSSConnectionItem item : wssConnectionItems.values()) {
                if (item.getDeviceIP() == null || item.getDevicePort() == null) continue;
                if (item.getDeviceIP().equals(ip) && item.getDevicePort().equals(port)) {
                    return item;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getItemByIPAndPort: " + e.getMessage());
        }
        return null;
    }

    public static String getKeyFromWSSConn(WebSocket connection) {
        if (connection == null) return "";
        InetSocketAddress addr = connection.getRemoteSocketAddress();
        return addr.getAddress() + ":" + addr.getPort();
    }

    // ============================================================ 发送消息

    /** 发送通用 JSON 消息 */
    public String sendWSSMessage(String key, Object object) {
        if (wssConnectionItems.isEmpty()) {
            Log.e(TAG, "sendWSSMessage: no connections");
            return "error:" + key + " no connection";
        }
        if (key == null) {
            Log.e(TAG, "sendWSSMessage: key is null");
            return "error: key is null";
        }
        WSSConnectionItem conn = wssConnectionItems.get(key);
        if (conn == null) {
            Log.e(TAG, "sendWSSMessage: no item for key=" + key);
            return "error:" + key + " no connection";
        }
        try {
            BaseMessage baseMessage = (BaseMessage) object;
            if (!baseMessage.getType().equals("QueryDevice")) {
                baseMessage.setDeviceType(conn.getDeviceType());
                baseMessage.setDeviceID(conn.getDeviceID());
                baseMessage.setPosition(conn.getPosition());
            }
            boolean result  = conn.sendWSSMessage(new Gson().toJson(object));
            if(result) {
                return "succeed:" + key;
            }else{
                Log.e(TAG, "sendWSSMessage: " + "error"   + key);
                return "error:" + key;
            }
        } catch (Exception e) {
            Log.e(TAG, "sendWSSMessage: " + e.getMessage());
            return "error:" + key + " sendWSSMessage error";
        }
    }

    /** 发送 RNA 复位指令 */
    public String sendRNAResetWSSMessage(String key, int force) {
        if (key == null) {
            Log.e(TAG, "sendRNAResetWSSMessage: key is null");
            return "error: no rna connection";
        }
        if (wssConnectionItems.isEmpty()) {
            Log.e(TAG, "sendRNAResetWSSMessage: no connections");
            return "error: no rna connection";
        }
        WSSConnectionItem conn = wssConnectionItems.get(key);
        if (!(conn instanceof RNAStatusItem)) {
            Log.e(TAG, "sendRNAResetWSSMessage: " + key + " is not RNA device");
            return "error:" + key + " is not RNA device";
        }
        RNAStatusItem rNAStatusItem = (RNAStatusItem) conn;
        RNAReqMessage req = new RNAReqMessage();
        req.setSeqId(111);
        req.setBatchID("124");
        req.setTotalTime(100);
        req.setType("RNAReq");
        req.setCurrentPhase(0);
        req.setCurrentPhaseStep(0);
        req.setForce(force);
        try {
            String filePath;
            if (rNAStatusItem.getAbnormalReset() == 1) {
                filePath = FileUtils.COMMON_HEX_PATH + "99-reset.hex";
                req.setTemplate("99~reset~reset.hex");
            } else {
                filePath = FileUtils.COMMON_HEX_PATH + "95-reset_exception.hex";
                req.setTemplate("95~reset~reset-exception.hex");
            }
            List<HexDebugEntity> hexList = ListDataSaveUtil.getHexDebugEntityList(new File(filePath));
            req.setWorkTemplate(hexList);
            sendRNATemplateWSSMessage(key, req);
            return "succeed:" + key + " send reset succeed";
        } catch (Exception e) {
            Log.e(TAG, "sendRNAResetWSSMessage: " + e.getMessage());
            return "error:" + key + " send error " + e.getMessage();
        }
    }

    /** 发送 RNA 模板 */
    public String sendRNATemplateWSSMessage(String key, RNAReqMessage req) {
        if (wssConnectionItems.isEmpty()) {
            return "error: no rna connection";
        }
        WSSConnectionItem conn = wssConnectionItems.get(key);
        if (!(conn instanceof RNAStatusItem)) {
            Log.e(TAG, "sendRNATemplateWSSMessage: " + key + " is not RNA device");
            return "error:" + key + " is not RNA device";
        }
        RNAStatusItem item = (RNAStatusItem) conn;
        if (req.getForce() == 0) {
            boolean isResetOrOpenClose = req.getTemplate().contains("reset")
                    || req.getTemplate().contains("open")
                    || req.getTemplate().contains("close");
            if (isResetOrOpenClose && item.getrNAStatus().equals("acking")) {
                Log.e(TAG, "sendRNATemplateWSSMessage: " + key + " status not fit: " + item.getrNAStatus());
                return "error:" + key + " status not fit: " + item.getrNAStatus();
            }
            if (!isResetOrOpenClose && item.getrNAStatus().contains("error")) {
                Log.e(TAG, "sendRNATemplateWSSMessage: " + key + " status not fit: " + item.getrNAStatus());
                return "error:" + key + " status not fit: " + item.getrNAStatus();
            }
            if (!item.getrNAAlarm().equals("")) {
                Log.e(TAG, "sendRNATemplateWSSMessage: " + key + " alarm: " + item.getrNAAlarm());
            }
        }
        req.setDeviceType(item.getDeviceType());
        req.setPosition(item.getPosition());
        req.setDeviceID(item.getDeviceID());
        req.setMSGTime(DateUtil.getCurrentTimeYMDHMS());
        boolean sent = item.sendTemplateWSSMessage(GsonUtil.toJson(req, false));
        if (!sent) {
            item.setrNASendAlarm("send template failed");
            return "error:" + key + " send template failed";
        }
        item.setrNAStatus("acking");
        return "succeed:" + key + " sendRNATemplateWSSMessage acking";
    }

    /** 发送 PCR 模板 */
    public String sendPCRTemplateWSSMessage(String key, PCRReqMessage req) {
        if (wssConnectionItems.isEmpty()) {
            return "error: no PCR connection";
        }
        WSSConnectionItem conn = wssConnectionItems.get(key);
        if (!(conn instanceof PCRStatusItem)) {
            Log.e(TAG, "sendPCRTemplateWSSMessage: " + key + " is not PCR device");
            return "error:" + key + " is not PCR device";
        }
        PCRStatusItem item = (PCRStatusItem) conn;
        if (req.getForce() == 0) {
            if (item.getpCRLinuxStatus().equals("running")
                    || item.getpCRLinuxStatus().equals("acking")
                    || item.getpCRStatus().equals("error")
                    || item.getpCRLinuxStatus().equals("error")) {
                Log.e(TAG, "sendPCRTemplateWSSMessage: " + key + " status=" + item.getpCRStatus()
                        + " linuxStatus=" + item.getpCRLinuxStatus());
                return "error:" + key + " status not fit: " + item.getpCRStatus();
            }
            if (!item.getpCRAlarm().equals("")) {
                Log.e(TAG, "sendPCRTemplateWSSMessage: " + key + " alarm: " + item.getpCRAlarm());
                return "error:" + key + " alarm: " + item.getpCRAlarm();
            }
        }
        req.setDeviceID(item.getDeviceID());
        req.setDeviceType(item.getDeviceType());
        req.setPosition(item.getPosition());
        boolean sent = item.sendTemplateWSSMessage(GsonUtil.toJson(req, false));
        if (!sent) {
            item.setpCRSendAlarm("send template failed");
            return "error:" + key + " send template failed";
        }
        item.setpCRLinuxStatus("acking");
        Log.i(TAG, "sendPCRTemplateWSSMessage: " + key + " succeed");
        return "succeed:" + key + " sendPCRTemplateWSSMessage acking";
    }

    /** 发送 SLAN PCR 二进制模板 */
    public String sendSLAN_PCRTemplateWSSMessage(String key, BinBaseMessage binMsg, String filePath) {
        PCRStatusItem item = requirePCRItem(key, "sendSLAN_PCRTemplateWSSMessage");
        if (item == null) return "error:" + key + " is not PCR device";
        if (binMsg.getForce() == 0) {
            if (isPCRBusy(item)) {
                Log.e(TAG, "sendSLAN_PCRTemplateWSSMessage: " + key + " busy");
                return "error:" + key + " status not fit: " + item.getpCRStatus();
            }
            if (!ConstantData.NULL.equals(item.getpCRAlarm())) {
                return "error:" + key + " alarm: " + item.getpCRAlarm();
            }
        }
        binMsg.setDeviceID(item.getDeviceID());
        binMsg.setDeviceType(item.getDeviceType());
        binMsg.setPosition(item.getPosition());
        binMsg.setType("SLANPCRReq");
        ByteBuffer buf = BinWssMessageUtils.packetBinMessage(binMsg, filePath);
        if(buf == null || buf.array().length <= 0) {
            Log.e(TAG, "sendSLAN_PCRTemplateWSSMessage: " + key + " packetBinMessage failed");
            return "error:" + key + " packetBinMessage failed";
        }
        Log.e(TAG, "sendSLAN_PCRTemplateWSSMessage: buf.array().length" + buf.array().length);
        if (!item.sendTemplateWSSMessage(buf, binMsg.getType())) {
            item.setpCRSendAlarm("send template failed");
            return "error:" + key + " send template failed";
        }
        item.setpCRLinuxStatus("acking");
        Log.e(TAG, "sendSLAN_PCRTemplateWSSMessage: " + key + " succeed");
        return "succeed:" + key + " sendSLAN_PCRTemplateWSSMessage acking";
    }

    /** 发送 PCR 升级包 */
    public String sendPCRUpgradeWSSMessage(String key, UpgradeMessage upgradeMsg, String filePath) {
        PCRStatusItem item = requirePCRItem(key, "sendPCRUpgradeWSSMessage");
        if (item == null) return "error:" + key + " is not PCR device";
        if (upgradeMsg.getForce() == 0) {
            if (item.getpCRLinuxStatus().equals("running") || item.getpCRLinuxStatus().equals("acking")) {
                return "error:" + key + " status not fit: " + item.getpCRStatus();
            }
            if (!item.getpCRAlarm().equals("")) {
                return "error:" + key + " alarm: " + item.getpCRAlarm();
            }
        }
        upgradeMsg.setDeviceID(item.getDeviceID());
        upgradeMsg.setDeviceType(item.getDeviceType());
        upgradeMsg.setPosition(item.getPosition());
        upgradeMsg.setVersion(filePath);
        ByteBuffer buf = BinWssMessageUtils.packetBinMessage(upgradeMsg, filePath);
        if (!item.sendWSSMessage(buf)) {
            item.setpCRSendAlarm("send PCRUpgrade failed");
            return "error:" + key + " sendPCRUpgradeWSSMessage failed";
        }
        return "succeed:" + key + " sendPCRUpgradeWSSMessage OK";
    }

    /** 发送 RNA 升级包 */
    public String sendRNAUpgradeWSSMessage(String key, BinBaseMessage binMsg, String filePath) {
        if (wssConnectionItems.isEmpty()) return "error: no RNA connection";
        WSSConnectionItem conn = wssConnectionItems.get(key);
        if (conn == null) return "error:" + key + " not exist";
        if (!(conn instanceof RNAStatusItem)) return "error:" + key + " is not RNA device";
        RNAStatusItem item = (RNAStatusItem) conn;
        if (binMsg.getForce() == 0) {
            if (item.getrNALinuxStatus().equals("running") || item.getrNALinuxStatus().equals("acking")) {
                return "error:" + key + " status not fit: " + item.getrNAStatus();
            }
            if (!item.getrNAAlarm().equals("")) {
                return "error:" + key + " alarm: " + item.getrNAAlarm();
            }
        }
        binMsg.setDeviceID(item.getDeviceID());
        binMsg.setDeviceType(item.getDeviceType());
        binMsg.setPosition(item.getPosition());
        ByteBuffer buf = BinWssMessageUtils.packetBinMessage(binMsg, filePath);
        if (!item.sendTemplateWSSMessage(buf, binMsg.getType())) {
            item.setrNASendAlarm("send RNAUpgrade failed");
            return "error:" + key + " sendRNAUpgradeWSSMessage failed";
        }
        item.setrNALinuxStatus("acking");
        return "succeed:" + key + " sendRNAUpgradeWSSMessage OK";
    }

    // ============================================================ EventBus 入口

    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventMainThread(WSSMessageEvent messageEvent) {
        try {
            EnumMessageEvent msgType = messageEvent.getMsgType();
            if (msgType.equals(EnumMessageEvent.WSS_ServerStart)) {

                handleServerStart(messageEvent);
            } else if (msgType.equals(EnumMessageEvent.WSS_Connect)) {
                handleConnect(messageEvent);
            } else if (msgType.equals(EnumMessageEvent.WSS_Error)) {
                handleError(messageEvent);
            } else if (msgType.equals(EnumMessageEvent.WSS_Disconnect)) {
                handleDisconnect(messageEvent);
            } else if (msgType.equals(EnumMessageEvent.WSS_Message)) {
                handleTextMessage(messageEvent);
            } else if (msgType.equals(EnumMessageEvent.WSS_BinMessage)) {
                handleBinMessage(messageEvent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "onEventMainThread error: " + e.getMessage());
        }
    }

    // ============================================================ 连接事件处理

    private void handleServerStart(WSSMessageEvent event) {
        Log.i(TAG, "WSS_ServerStart: " + event.getData());
    }

    private void handleConnect(WSSMessageEvent event) {
        // 连接建立由 WSS_Message 中设备首次上报消息时处理，此处暂不处理
    }

    private void handleError(WSSMessageEvent event) {
        Log.e(TAG, "WSS_Error: " + event.getData());
        if (event.getwSSConnectionItem() == null) {
            Log.e(TAG, "WSS_Error: connectionItem is null");
            return;
        }
        if (event.getwSSConnectionItem().getWebSocketConnection() == null) {
            Log.e(TAG, "WSS_Error: webSocketConnection is null, deviceID=" + event.getwSSConnectionItem().getDeviceID());
            return;
        }
        InetSocketAddress addr = event.getwSSConnectionItem().getWebSocketConnection().getRemoteSocketAddress();
        if (addr == null) return;
        String ip = addr.getAddress().toString();
        String port = String.valueOf(addr.getPort());
        WSSConnectionItem item = getItemByIPAndPort(ip, port);
        if (item != null) {
            item.setWSSConnect(false);
            setDisconnectStatus(item);
            Log.e(TAG, "WSS_Error: device=" + item.getDeviceID()
                    + " type=" + item.getDeviceType()
                    + " pos=" + item.getPosition()
                    + " addr=" + ip + ":" + port);
        }
        EventBus.getDefault().post(new WSSMessageEvent(
                EnumMessageEvent.WSS_Error, event.getwSSConnectionItem(), "WSSManager_UpdateConnection WSS_Error"));
    }

    private void handleDisconnect(WSSMessageEvent event) {
        List<String> disconnectedKeys = new ArrayList<>();
        for (Map.Entry<String, WSSConnectionItem> entry : wssConnectionItems.entrySet()) {
            WSSConnectionItem item = entry.getValue();
            if (item != null && isConnectionClosed(item)) {
                disconnectedKeys.add(entry.getKey());
            }
        }
        StringBuilder ips = new StringBuilder();
        synchronized (wssConnectionItemsLock) {
            for (String key : disconnectedKeys) {
                WSSConnectionItem item = wssConnectionItems.get(key);
                if (item != null) {
                    setDisconnectStatus(item);
                }
                ips.append("---").append(key);
                Log.i(TAG, "WSS_Disconnect: removed key=" + key + " data=" + event.getData());
            }
        }
        EventBus.getDefault().post(new WSSMessageEvent(
                EnumMessageEvent.WSSManager_UpdateConnection, event.getwSSConnectionItem(),
                "WSS_Disconnect" + ips));
    }

    // ============================================================ 文本消息路由

    private void handleTextMessage(WSSMessageEvent event) {
        if (event.getwSSConnectionItem() == null) {
            Log.e(TAG, "handleTextMessage: connectionItem is null");
            return;
        }
        if (event.getwSSConnectionItem().getWebSocketConnection() == null) {
            Log.e(TAG, "handleTextMessage: webSocketConnection is null");
            return;
        }
        InetSocketAddress addr = event.getwSSConnectionItem().getWebSocketConnection().getRemoteSocketAddress();
        if (addr == null) return;

        long parseStartTime = System.currentTimeMillis();
        Log.i(TAG, "parseBaseMessage START time: " + parseStartTime);
        BaseMessage baseMessage = GsonUtil.fromJson((String) event.getData(), BaseMessage.class);
        long parseEndTime = System.currentTimeMillis();
        Log.i(TAG, "parseBaseMessage END time: " + parseEndTime + " cost: " + ((parseEndTime - parseStartTime) / 1000.0) + "s");

        String key = baseMessage.getDeviceID();
        String deviceType = baseMessage.getDeviceType();
        String rawJson = (String) event.getData();
        WSSConnectionItem item = wssConnectionItems.get(key);
        if (item == null) {
            Log.e(TAG, "handleTextMessage: item null for key=" + key + ", creating new item");
            item = createConnectionItem(deviceType, key, addr, event.getwSSConnectionItem().getWebSocketConnection());
            if (item != null) wssConnectionItems.put(key, item);
            //return;
        }

        item.setDeviceIP(addr.getAddress().toString());
        item.setDevicePort(String.valueOf(addr.getPort()));
        item.setWebSocketConnection(event.getwSSConnectionItem().getWebSocketConnection());
        item.setCount(0);
        item.setWSSConnect(true);

        Map<String, Object> jsonMap = GsonUtil.fromJson(rawJson, Map.class);
        if (jsonMap == null || jsonMap.isEmpty() || !jsonMap.containsKey("Type")) {
            Log.i(TAG, "handleTextMessage: recv key=" + key + " data=" + rawJson);
            return;
        }

        String type = (String) jsonMap.get("Type");
        switch (type) {
            case "QueryDeviceReply":  onQueryDeviceReply(rawJson, item);                   break;
            case "HeartBeatReply":    onHeartBeatReply(rawJson, item, key);                break;
            case "RNAStatus":         onRNAStatus(rawJson, item, key);                     break;
            case "RNAAck":            onRNAAck(rawJson, item, key);                        break;
            case "RNARunningStatus":  onRNARunningStatus(rawJson, item, key);              break;
            case "RNAFinish":         onRNAFinish(rawJson, item, key);                     break;
            case "RNALog":            onRNALog(rawJson, item, key);                        break;
            case "PCRStatus":         onPCRStatus(rawJson, item, key);                     break;
            case "PCRAck":            onPCRAck(rawJson, item, key);                        break;
            case "PCRRunningStatus":  onPCRRunningStatus(rawJson, item, key);              break;
            case "PCRCurve":          onPCRCurve(rawJson, item, key);                      break;
            case "PCRFinish":         onPCRFinish(rawJson, item, key);                     break;
            case "PCRLog":            onPCRLog(rawJson, item, key);                        break;
            case "UpgradeAck":        onUpgradeAck(rawJson, item, key);                    break;
            //TODO PCRFinishACK 用于PCR停止命令的上位机反馈
            //TODO RNAFinishACK 用于PCR停止命令的上位机反馈
            default:
                Log.w(TAG, "handleTextMessage: unknown type=" + type + " key=" + key);
                break;
        }
        Log.i(TAG, "handleTextMessage: done type=" + type + " key=" + key);
    }

    // ============================================================ 各类型文本消息处理

    private void onQueryDeviceReply(String rawJson, WSSConnectionItem item, String key) {
        QueryDeviceReplyMessage msg = GsonUtil.fromJson(rawJson, QueryDeviceReplyMessage.class);
        item.setPosition(msg.getPosition());
        item.setDeviceID(msg.getDeviceID());
        item.setDeviceType(msg.getDeviceType());
        Log.i(TAG, "QueryDeviceReply: key=" + key + " deviceID=" + msg.getDeviceID());
    }

    // 兼容旧代码，不带 key 参数的重载
    private void onQueryDeviceReply(String rawJson, WSSConnectionItem item) {
        onQueryDeviceReply(rawJson, item, item.getDeviceID());
    }

    private void onHeartBeatReply(String rawJson, WSSConnectionItem item, String key) {
        HeartBeatReplyMessage msg = GsonUtil.fromJson(rawJson, HeartBeatReplyMessage.class);
        if (msg.getDeviceType().contains("PCR") && item instanceof PCRStatusItem) {
            PCRStatusItem s = (PCRStatusItem) item;
            s.setWSSConnect(true);
            s.setCount(0);
            s.setDeviceType(msg.getDeviceType());
            s.setDeviceID(item.getDeviceID());
            s.setDeviceIP(item.getDeviceIP());
            s.setPosition(msg.getPosition());
            s.setVersion(msg.getVersion());
            s.setDevicePort(item.getDevicePort());
            s.setWebSocketConnection(item.getWebSocketConnection());
            Log.i(TAG, "HeartBeatReply PCR: key=" + key);
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_UpdateConnection, s, "heartbeat pcr:" + key));
        } else if (msg.getDeviceType().contains("RNA") && item instanceof RNAStatusItem) {
            RNAStatusItem s = (RNAStatusItem) item;
            s.setVersion(msg.getVersion());
            s.setDeviceType(msg.getDeviceType());
            s.setDeviceID(item.getDeviceID());
            s.setDeviceIP(item.getDeviceIP());
            s.setPosition(msg.getPosition());
            s.setDevicePort(item.getDevicePort());
            s.setWebSocketConnection(item.getWebSocketConnection());
            Log.i(TAG, "HeartBeatReply RNA: key=" + key);
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_UpdateConnection, s, "heartbeat rna:" + key));
        }
    }

    private void onRNAStatus(String rawJson, WSSConnectionItem item, String key) {
        RNAStatusMessage msg = GsonUtil.fromJson(rawJson, RNAStatusMessage.class);
        if (item instanceof RNAStatusItem) {
            applyRNAStatusFields((RNAStatusItem) item, msg);
        }
        Log.i(TAG, "RNAStatus: key=" + key + " status=" + msg.getStatus() + " alarm=" + msg.getAlarm());
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_UpdateRNAStatus, item, "RNAStatus update:" + key));
    }

    private void onRNAAck(String rawJson, WSSConnectionItem item, String key) {
        if (!(item instanceof RNAStatusItem)) return;
        RNAAckMessage msg = GsonUtil.fromJson(rawJson, RNAAckMessage.class);
        RNAStatusItem s = (RNAStatusItem) item;
        if (msg.getReply().contains("ok")) {
            s.setrNASendAlarm("");
            s.setrNAStatus("running");
            s.setSendingTemplate(false);
            s.stopAckTimer();
            Log.e(TAG, "RNAAck OK: alarm=" + msg.getAlarm() + " deviceID=" + msg.getDeviceID());
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_RNAAck_OK, s, "RNAAck OK:" + key));
        } else if (msg.getReply().contains("error")) {
            s.setrNASendAlarm(msg.getAlarm());
            s.setrNAStatus("free");
            s.stopAckTimer();
            s.setSendingTemplate(false);
            Log.e(TAG, "RNAAck error: reply=" + msg.getReply() + " alarm=" + msg.getAlarm());
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_RNAAck_Error, s, "RNAAck error:" + key));
        }
    }

    private void onRNARunningStatus(String rawJson, WSSConnectionItem item, String key) {
        if (!(item instanceof RNAStatusItem)) return;
        RNARunningStatusMessage msg = GsonUtil.fromJson(rawJson, RNARunningStatusMessage.class);
        RNAStatusItem s = (RNAStatusItem) item;
        s.setrNAAlarm(msg.getAlarm());
        s.setrNADegree(msg.getDegree());
        s.setNO(msg.getNo());
        s.setrNAStatus(msg.getStatus());
        s.setrNALeftTime(msg.getLeftTime());
        s.setrNATotalTime(msg.getTotalTime());
        s.setInfo(msg.getInfo());
        s.setrNARunninginfo(msg.getInfo());
        s.setrNACurrentTemplate(msg.getTemplate());
        s.setrNALinuxStatus(msg.getLinuxStatus());
        s.setrNALinuxAlarm(msg.getLinuxAlarm());
        Log.e(TAG, "RNARunningStatus: key=" + key + " no=" + msg.getNo());
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_UpdateRNARunningStatus, s, "RNARunningStatus:" + key));
    }

    private void onRNAFinish(String rawJson, WSSConnectionItem item, String key){
        if (!(item instanceof RNAStatusItem)) return;
        RNAFinishMessage msg = GsonUtil.fromJson(rawJson, RNAFinishMessage.class);
        RNAStatusItem s = (RNAStatusItem) item;
        if (msg.getStatus().equals("done") || msg.getStatus().equals("free") || msg.getStatus().equals("ok")) {
            s.setrNAAlarm(msg.getAlarm());
            s.setNO(msg.getNo());
            s.setrNAStatus(msg.getTemplate().contains("reset") ? "free" : msg.getStatus());
            s.setrNALeftTime(0);
            s.setrNATotalTime(0);
            s.setrNACurrentTemplate(msg.getTemplate());
            s.setrNARunninginfo("");
            s.setInfo(msg.getInfo());
            Log.i(TAG, "RNAFinish done/free/ok: key=" + key + " status=" + msg.getStatus());
        } else {
            s.setrNAAlarm(msg.getAlarm());
            s.setNO(msg.getNo());
            s.setrNAStatus(msg.getStatus());
            s.setInfo(msg.getInfo());
            Log.i(TAG, "RNAFinish other: key=" + key + " status=" + msg.getStatus()
                    + " no=" + msg.getNo() + " alarm=" + msg.getAlarm());
        }
        RNAFinishAckMessage ack = buildRNAFinishAck(s);
        boolean sent = s.sendWSSMessage(GsonUtil.toJson(ack, false));
        // 将等待 + 通知移入异步线程，避免阻塞 POSTING 线程
        pcrFinishExecutor.execute(() -> {
            try {
                // RNAFinishAck 可能会触发设备重启，等待设备重启完成后再更新 UI
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "onRNAFinish sleep interrupted: " + e.getMessage());
            }
            if (sent) {
                Log.e(TAG, "RNAFinishAck succeed: key=" + key);
            } else {
                Log.e(TAG, "RNAFinishAck failed: key=" + key);
            }
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_RNARunningFinish, s, msg));
        });
    }

    private void onRNALog(String rawJson, WSSConnectionItem item, String key) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "onRNALog START time: " + startTime);
        if (!(item instanceof RNAStatusItem)) {
            Log.i(TAG, item.getDeviceID() + " onRNALog END time: " + System.currentTimeMillis() + " cost: " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
            return;
        }
        RNAStatusItem s = (RNAStatusItem) item;
        RNALogMessage msg = GsonUtil.fromJson(rawJson, RNALogMessage.class);
        Log.e(TAG, "RNALog: key=" + key + " file=" + msg.getFileName());
        EventBus.getDefault().post(new LogMessageEvent(
                ConstantData.LOG_RNA, rawJson, EnumMessageEvent.WSS_Message, s, msg));
        long endTime = System.currentTimeMillis();
        Log.i(TAG, item.getDeviceID() + " onRNALog END time: " + endTime + " cost: " + ((endTime - startTime) / 1000.0) + "s");
    }

    private void onPCRStatus(String rawJson, WSSConnectionItem item, String key) {
        PCRStatusMessage msg = GsonUtil.fromJson(rawJson, PCRStatusMessage.class);
        applyPCRStatusFields((PCRStatusItem) item, msg);
        Log.i(TAG, "PCRStatus: key=" + key + " status=" + msg.getStatus() + " alarm=" + msg.getAlarm());
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_UpdatePCRStatus, item, "PCRStatus update:" + key));
    }

    private void onPCRAck(String rawJson, WSSConnectionItem item, String key) {
        if (!(item instanceof PCRStatusItem)) return;
        PCRAckMessage msg = GsonUtil.fromJson(rawJson, PCRAckMessage.class);
        PCRStatusItem s = (PCRStatusItem) item;
        if (msg.getReply().contains("ok")) {
            s.setpCRSendAlarm("");
            s.setpCRStatus("running");
            s.setSendingTemplate(false);
            s.stopAckTimer();
            Log.e(TAG, "PCRAck OK: alarm=" + msg.getAlarm());
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_PCRAck_OK, s, "PCRAck OK:" + key));
        } else if (msg.getReply().contains("error")) {
            s.setpCRSendAlarm(msg.getAlarm());
            s.setpCRStatus("error");
            s.stopAckTimer();
            s.setSendingTemplate(false);
            Log.e(TAG, "PCRAck error: reply=" + msg.getReply());
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_PCRAck_Error, s, "PCRAck error:" + key));
        }
    }

    private void onPCRRunningStatus(String rawJson, WSSConnectionItem item, String key) {
        if (!(item instanceof PCRStatusItem)) return;
        PCRRunningStatusMessage msg = GsonUtil.fromJson(rawJson, PCRRunningStatusMessage.class);
        PCRStatusItem s = (PCRStatusItem) item;
        s.setpCRAlarm(msg.getAlarm());
        s.setpCRDegree(msg.getDegree());
        s.setpCRStatus(msg.getStatus());
        s.setpCRCurrentTemplate(msg.getTemplate());
        s.setpCRCurrentSection(msg.getCurrentSection());
        s.setpCRCurrentStep(msg.getCurrentStep());
        s.setpCRTotalCycle(msg.getTotalCycle());
        s.setpCRCurrentCycle(msg.getCurrentCycle());
        s.setpCRLeftTime(msg.getLeftTime());
        s.setpCRTotalTime(msg.getTotalTime());
        s.setpCRRunningTime(msg.getRunningTime());
        s.setpCRWaitTime(msg.getWaitingTime());
        s.setInfo(msg.getInfo());
        s.setDegreeTarget(msg.getDegreeTarget());
        s.setpCRLinuxStatus(msg.getLinuxStatus());
        s.setpCRLinuxAlarm(msg.getLinuxAlarm());
        Log.i(TAG, "PCRRunningStatus: key=" + key + " status=" + msg.getStatus());
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_UpdatePCRRunningStatus, s, "PCRRunningStatus:" + key));
    }

    private void onPCRCurve(final String rawJson, final WSSConnectionItem item, final String key) {
        pcrCurveExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                Log.i(TAG, "onPCRCurve async START time: " + startTime);
                if (!(item instanceof PCRStatusItem)) {
                    Log.i(TAG, item.getDeviceID() + " onPCRCurve async END time: " + System.currentTimeMillis() + " cost: " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
                    return;
                }
                try {
                    if (StringUtil.isEmpty(rawJson)) {
                        Log.i(TAG, item.getDeviceID() + " onPCRCurve async END time: " + System.currentTimeMillis() + " cost: " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
                        return;
                    }
                    PCRCurveMessage msg = GsonUtil.fromJson(rawJson, PCRCurveMessage.class);
                    PCRStatusItem s = (PCRStatusItem) item;
                    Experiment experiment = msg.getExperiment();
                    if (experiment == null) {
                        Log.e(TAG, "PCRCurve: experiment is null, key=" + key + " template=" + msg.getTemplate());
                        Log.i(TAG, item.getDeviceID() + " onPCRCurve async END time: " + System.currentTimeMillis() + " cost: " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
                        return;
                    }

                    boolean hasCachedProject = false;
                    Experiment oldExperiment = s.getExperiment();
                    if (oldExperiment != null && oldExperiment.getProject() != null && s.getpCRCurrentTemplate() != null) {
                        if (s.getpCRCurrentTemplate().equals(msg.getTemplate())) {
                            com.hongshi.slansdk.model.experiment.Project cachedProj = oldExperiment.getProject();
                            if (cachedProj != null && !StringUtil.isEmpty(cachedProj.getName())) {
                                hasCachedProject = true;
                                experiment.setProject(cachedProj);
                            }
                        }
                    }

                    if (!hasCachedProject) {
                        try {
                            com.google.gson.JsonObject rootElement = com.google.gson.JsonParser.parseString(rawJson).getAsJsonObject();
                            com.google.gson.JsonElement expElement = rootElement.get("Experiment");
                            if (expElement != null) {
                                com.google.gson.JsonObject expGsonObj = null;
                                if (expElement.isJsonObject()) {
                                    expGsonObj = expElement.getAsJsonObject();
                                } else if (expElement.isJsonPrimitive() && expElement.getAsJsonPrimitive().isString()) {
                                    String expStr = expElement.getAsString();
                                    if (!StringUtil.isEmpty(expStr)) {
                                        expGsonObj = com.google.gson.JsonParser.parseString(expStr).getAsJsonObject();
                                    }
                                }
                                if (expGsonObj != null && expGsonObj.has("Project")) {
                                    com.google.gson.JsonElement projectGson = expGsonObj.get("Project");
                                    if (projectGson != null && projectGson.isJsonObject()) {
                                        JSONObject projectObj = new JSONObject(projectGson.toString());
                                        experiment.setProject(BoWeiService.getInstance().createNewProgect(projectObj));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "PCRCurve fast project parsing error: " + e.getMessage());
                        }
                    }
                    s.setExperiment(experiment);
                    s.setpCRCurrentTemplate(msg.getTemplate());
                    Log.i(TAG, "PCRCurve: key=" + key + " template=" + msg.getTemplate());
                    EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_UpdatePCRCurve, s, "PCRCurve:" + key));
                } catch (Exception e) {
                    Log.e(TAG, "PCRCurve parse error: " + e.getMessage());
                }
                long endTime = System.currentTimeMillis();
                Log.i(TAG, item.getDeviceID() + " onPCRCurve async END time: " + endTime + " cost: " + ((endTime - startTime) / 1000.0) + "s");
            }
        });
    }

    private void onPCRFinish(String rawJson, WSSConnectionItem item, String key) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "onPCRFinish START time: " + startTime);
        if (!(item instanceof PCRStatusItem)) {
            Log.i(TAG, item.getDeviceID() + " onPCRFinish END time: " + System.currentTimeMillis() + " cost: " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
            return;
        }
        PCRFinishMessage msg = GsonUtil.fromJson(rawJson, PCRFinishMessage.class);
        PCRStatusItem s = (PCRStatusItem) item;
        applyPCRFinishFields(s, msg);
        Log.i(TAG, "PCRFinish: key=" + key + " status=" + msg.getStatus());
        PCRFinishAckMessage ack = buildPCRFinishAck(s);
        boolean sent = s.sendWSSMessage(GsonUtil.toJson(ack, false));
        Log.i(TAG, item.getDeviceID() + " onPCRFinish send ack END time: " + System.currentTimeMillis() + " cost: " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
        // 将等待 + 通知移入异步线程，避免阻塞 POSTING 线程
        pcrFinishExecutor.execute(() -> {
            try {
                // PCRFinishAck 可能会触发设备重启，等待设备重启完成后再更新 UI
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "onPCRFinish sleep interrupted: " + e.getMessage());
            }
            if (sent) {
                Log.e(TAG, "PCRFinishAck succeed: key=" + key);
            } else {
                Log.e(TAG, "PCRFinishAck failed: key=" + key);
            }
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_PCRRunningFinish, s, msg));
            Log.i(TAG, s.getDeviceID() + " onPCRFinish async END time: " + System.currentTimeMillis() + " cost: " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
        });
    }

    private void onPCRLog(String rawJson, WSSConnectionItem item, String key) {
        long startTime = System.currentTimeMillis();
        Log.i(TAG, "onPCRLog START time: " + startTime);
        if (!(item instanceof PCRStatusItem) || rawJson == null) {
            Log.i(TAG, item.getDeviceID() + " onPCRLog END time: " + System.currentTimeMillis() + " cost: " + ((System.currentTimeMillis() - startTime) / 1000.0) + "s");
            return;
        }
        PCRStatusItem s = (PCRStatusItem) item;
        PCRLogMessage msg = GsonUtil.fromJson(rawJson, PCRLogMessage.class);
        Log.e(TAG, "PCRLog: key=" + key + " file=" + msg.getFileName());
        EventBus.getDefault().post(new LogMessageEvent(
                ConstantData.LOG_PCR, rawJson, EnumMessageEvent.WSS_Message, s, msg));
        long endTime = System.currentTimeMillis();
        Log.i(TAG, item.getDeviceID() + " onPCRLog END time: " + endTime + " cost: " + ((endTime - startTime) / 1000.0) + "s");
    }

    private void onUpgradeAck(String rawJson, WSSConnectionItem item, String key) {
        UpgradeAckMessage msg = GsonUtil.fromJson(rawJson, UpgradeAckMessage.class);

        // ⭐ 优化：移到后台线程执行，避免阻塞 WSS 消息处理
        pcrFinishExecutor.execute(() -> {
            try {
                // 等待设备处理升级结果，避免与后续消息冲突
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "onUpgradeAck sleep interrupted: " + e.getMessage());
            }

            if (msg.getDeviceType().contains("PCR") && item instanceof PCRStatusItem) {
                PCRStatusItem s = (PCRStatusItem) item;
                s.stopAckTimer();
                s.setSendingTemplate(false);
                s.setpCRLinuxStatus(msg.getReply() + msg.getError());
            } else if (msg.getDeviceType().contains("RNA") && item instanceof RNAStatusItem) {
                RNAStatusItem s = (RNAStatusItem) item;
                s.stopAckTimer();
                s.setSendingTemplate(false);
                s.setrNALinuxStatus(msg.getReply() + msg.getError());
            }
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_UpdateConnection, item, "upgrade:" + key));
        });
    }

    // ============================================================ 二进制消息处理

    private void handleBinMessage(WSSMessageEvent event)  {
        BinBaseMessage binMsg = (BinBaseMessage) event.getData();
        InetSocketAddress addr = event.getwSSConnectionItem().getWebSocketConnection().getRemoteSocketAddress();
        String key = binMsg.getDeviceID();
        WSSConnectionItem item = wssConnectionItems.get(key);
        if (item == null) {
            Log.e(TAG, "handleBinMessage: item null key=" + key);
            return;
        }
        item.setCount(0);
        item.setWSSConnect(true);

        if (binMsg instanceof SLAN_PCRFinishMessage) {

            onSLANPCRFinish((SLAN_PCRFinishMessage) binMsg, item, key, addr);

        } else if (binMsg instanceof RNALogMessage && item instanceof RNAStatusItem) {
            RNALogMessage msg = (RNALogMessage) binMsg;
            String deviceID = msg.getDeviceID() != null ? msg.getDeviceID() : item.getDeviceID();
            int dataSize = (msg.getFileData() != null) ? msg.getFileData().length : 0;

            Log.e(TAG, "【RNALog(bin)】收到日志: key=" + key +
                ", file=" + msg.getFileName() +
                ", deviceID=" + deviceID +
                ", size=" + dataSize + " bytes");
            Log.i(TAG, "【RNALog(bin)】开始触发保存到本地文件...");

            // 保存 RNA 日志到本地文件
            saveRNALogToFile(msg, item);

            // 发送 EventBus 消息（用于界面显示等）
            EventBus.getDefault().post(new LogMessageEvent(
                    ConstantData.LOG_RNA, binMsg.getFileData(),
                    EnumMessageEvent.WSS_LOGMessage, (RNAStatusItem) item, msg));

            Log.i(TAG, "【RNALog(bin)】保存任务已提交到异步队列");

        } else if (binMsg instanceof PCRLogMessage && item instanceof PCRStatusItem) {
            PCRLogMessage msg = (PCRLogMessage) binMsg;
            String deviceID = msg.getDeviceID() != null ? msg.getDeviceID() : item.getDeviceID();
            int dataSize = (msg.getFileData() != null) ? msg.getFileData().length : 0;

            Log.e(TAG, "【PCRLog(bin)】收到日志: key=" + key +
                ", file=" + msg.getFileName() +
                ", deviceID=" + deviceID +
                ", size=" + dataSize + " bytes");
            Log.i(TAG, "【PCRLog(bin)】开始触发保存到本地文件...");

            // 保存 PCR 日志到本地文件
            savePCRLogToFile(msg, item);

            // 发送 EventBus 消息（用于界面显示等）
            EventBus.getDefault().post(new LogMessageEvent(
                    ConstantData.LOG_PCR, binMsg.getFileData(),
                    EnumMessageEvent.WSS_LOGMessage, (PCRStatusItem) item, msg));

            Log.i(TAG, "【PCRLog(bin)】保存任务已提交到异步队列");
        }
    }

    /**
     * 保存 RNA 日志到本地文件（二进制消息）
     * 参考 6in1 项目的实现
     */
    private void saveRNALogToFile(final RNALogMessage msg, final WSSConnectionItem item) {
        if (msg == null || item == null) {
            return;
        }

        // 使用 pcrFinishExecutor 异步执行
        pcrFinishExecutor.submit(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                try {
                    String deviceID = msg.getDeviceID();
                    if (deviceID == null || deviceID.isEmpty()) {
                        deviceID = item.getDeviceID();
                    }

                    // RNALogMessage 是二进制消息，从 getFileData 获取内容
                    byte[] logData = msg.getFileData();

                    if (logData != null && logData.length > 0) {
                        // 在异步线程中执行字符串转换（可能耗时）
                        String logContent = new String(logData, "UTF-8");
                        WSSLogHelper.getInstance().saveLog("RNA", deviceID, logContent);
                        Log.i(TAG, "保存 RNA 日志成功: deviceID=" + deviceID +
                            ", size=" + logData.length +
                            " 耗时=" + (System.currentTimeMillis() - startTime) + "ms");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "保存 RNA 日志失败: " + e.getMessage() +
                        " 耗时=" + (System.currentTimeMillis() - startTime) + "ms");
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 保存 PCR 日志到本地文件（二进制消息）
     * 参考 6in1 项目的实现
     */
    private void savePCRLogToFile(final PCRLogMessage msg, final WSSConnectionItem item) {
        if (msg == null || item == null) {
            return;
        }

        // 使用 pcrFinishExecutor 异步执行
        pcrFinishExecutor.submit(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                try {
                    String deviceID = msg.getDeviceID();
                    if (deviceID == null || deviceID.isEmpty()) {
                        deviceID = item.getDeviceID();
                    }

                    // PCRLogMessage 是二进制消息，从 getFileData 获取内容
                    byte[] logData = msg.getFileData();

                    if (logData != null && logData.length > 0) {
                        // 在异步线程中执行字符串转换（可能耗时）
                        String logContent = new String(logData, "UTF-8");
                        WSSLogHelper.getInstance().saveLog("PCR", deviceID, logContent);
                        Log.i(TAG, "保存 PCR 日志成功: deviceID=" + deviceID +
                            ", size=" + logData.length +
                            " 耗时=" + (System.currentTimeMillis() - startTime) + "ms");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "保存 PCR 日志失败: " + e.getMessage() +
                        " 耗时=" + (System.currentTimeMillis() - startTime) + "ms");
                    e.printStackTrace();
                }
            }
        });
    }

    private void onSLANPCRFinish(SLAN_PCRFinishMessage fin, WSSConnectionItem item,
                                 String key, InetSocketAddress addr) {
        if (!(item instanceof PCRStatusItem)) return;
        PCRStatusItem s = (PCRStatusItem) item;
        Log.d(TAG, "===接收到结束的模组上传的消息SLAN_PCRFinish: addr=" + addr.getAddress() + ":" + addr.getPort()
                + " deviceID=" + fin.getDeviceID() + " template=" + fin.getTemplate());
        Log.d("slanFinish", "===接收到结束的模组上传的消息SLAN_PCRFinish: addr=" + addr.getAddress() + ":" + addr.getPort()
                + " deviceID=" + fin.getDeviceID() + " template=" + fin.getFileName());
        s.setpCRAlarm(fin.getAlarm());
        s.setpCRDegree(fin.getDegree());
        s.setpCRStatus(fin.getStatus());
        s.setpCRCurrentTemplate(fin.getTemplate());
        s.setpCRCurrentSection(fin.getCurrentSection());
        s.setpCRCurrentStep(fin.getCurrentStep());
        s.setpCRTotalCycle(fin.getTotalCycle());
        s.setpCRCurrentCycle(fin.getCurrentCycle());
        s.setpCRLeftTime(fin.getLeftTime());
        s.setpCRTotalTime(fin.getTotalTime());
        s.setpCRRunningTime(fin.getRunningTime());
        s.setpCRWaitTime(fin.getWaitingTime());
        s.setInfo(fin.getInfo());

        // 原有普通事件先注释/移除，改到保存成功后再发带路径版本
        // EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_SLANPCRRunningFinish, s, fin));

        // 闭包提前拷贝
        final PCRStatusItem finalS = s;
        final SLAN_PCRFinishMessage finalFin = fin;
        final String deviceID = s.getDeviceID();
        final String finDeviceID = fin.getDeviceID();
        final byte[] fileBytes = fin.getFileData();

        pcrFinishExecutor.submit(() -> {
            boolean saveOk;
            StringBuilder filePathSb = new StringBuilder();
            try {
                Thread.sleep(1000);
                saveOk = saveBinFile(fileBytes, deviceID, finDeviceID, filePathSb);
            } catch (InterruptedException e) {
                Log.e(TAG, finDeviceID + " 延时被中断，判定保存失败: " + e.getMessage());
                Log.e("slanFinish", finDeviceID + " 延时被中断，判定保存失败: " + e.getMessage());
                saveOk = false;
            }

            try {
                PCRFinishAckMessage ack = buildPCRFinishAck(finalS);
                if (saveOk) {
                    // ===== 文件保存成功 =====
                    String filePath = filePathSb.toString();
                    // 构造结果对象
                    SlanFinishResult slanFinishResult = new SlanFinishResult();
                    slanFinishResult.setFilePath(filePath);
                    slanFinishResult.setSlan_pcrFinishMessage(finalFin);
                    // 发送携带文件路径的 EventBus 事件
                    EventBus.getDefault().post(new WSSMessageEvent(
                            EnumMessageEvent.WSSManager_SLANPCRRunningFinish,
                            finalS,
                            slanFinishResult
                    ));
                    Log.e("slanFinish", finDeviceID + " ==发送结束更新消息===");
                    // 设置成功应答
                    ack.setReply("ok");
                } else {
                    // ===== 文件保存失败 =====
                    ack.setReply("error");
                }
                // 发送对应 ACK
                finalS.sendWSSMessage(GsonUtil.toJson(ack, false));
            } catch (Exception e) {
                Log.e(TAG, finDeviceID + " 处理结果/发送ACK异常: " + e.getMessage(), e);
                Log.e("slanFinish", finDeviceID + " 处理结果/发送ACK异常: " + e.getMessage(), e);
            }
        });
    }

    // ============================================================ 辅助方法

    /** 将 PCRStatusMessage 的所有字段写入 PCRStatusItem（消除重复赋值代码）*/
    private void applyPCRStatusFields(PCRStatusItem s, PCRStatusMessage msg) {
        s.setDeviceType(msg.getDeviceType());
        s.setDeviceID(msg.getDeviceID());
        s.setPosition(msg.getPosition());
        s.setpCRAlarm(msg.getAlarm());
        s.setpCRDegree(msg.getDegree());
        s.setpCRStatus(msg.getStatus());
        s.setpCRCurrentTemplate(msg.getTemplate());
        s.setpCRCurrentSection(msg.getCurrentSection());
        s.setpCRCurrentStep(msg.getCurrentStep());
        s.setpCRTotalCycle(msg.getTotalCycle());
        s.setpCRCurrentCycle(msg.getCurrentCycle());
        s.setpCRLeftTime(msg.getLeftTime());
        s.setpCRTotalTime(msg.getTotalTime());
        s.setpCRRunningTime(msg.getRunningTime());
        s.setpCRWaitTime(msg.getWaitingTime());
        s.setInfo(msg.getInfo());
        s.setDegreeTarget(msg.getDegreeTarget());
        s.setpCRLinuxStatus(msg.getLinuxStatus());
        s.setpCRLinuxAlarm(msg.getLinuxAlarm());
    }

    /** 将 PCRFinishMessage 的所有字段写入 PCRStatusItem */
    private void applyPCRFinishFields(PCRStatusItem s, PCRFinishMessage msg) {
        s.setpCRAlarm(msg.getAlarm());
        s.setpCRDegree(msg.getDegree());
        s.setpCRStatus(msg.getStatus());
        s.setpCRCurrentTemplate(msg.getTemplate());
        s.setpCRCurrentSection(msg.getCurrentSection());
        s.setpCRCurrentStep(msg.getCurrentStep());
        s.setpCRTotalCycle(msg.getTotalCycle());
        s.setpCRCurrentCycle(msg.getCurrentCycle());
        s.setpCRLeftTime(msg.getLeftTime());
        s.setpCRTotalTime(msg.getTotalTime());
        s.setpCRRunningTime(msg.getRunningTime());
        s.setpCRWaitTime(msg.getWaitingTime());
        s.setInfo(msg.getInfo());
    }

    /** 将 RNAStatusMessage 的所有字段写入 RNAStatusItem（消除重复赋值代码）*/
    private void applyRNAStatusFields(RNAStatusItem s, RNAStatusMessage msg) {
        s.setDeviceType(msg.getDeviceType());
        s.setDeviceID(msg.getDeviceID());
        s.setPosition(msg.getPosition());
        s.setrNAAlarm(msg.getAlarm());
        s.setrNAMotorId(msg.getMotorID());
        s.setrNADegree(msg.getDegree());
        s.setNO(msg.getNo());
        s.setrNALinuxStatus(msg.getLinuxStatus());
        s.setrNALinuxAlarm(msg.getLinuxAlarm());
        s.setrNAStatus(msg.getStatus());
        s.setrNACurrentTemplate(msg.getTemplate());
        s.setAbnormalReset(parseAbnormalReset(msg.getNo()));
    }

    /** 从 no 字段解析 abnormalReset 标志 */
    private int parseAbnormalReset(String no) {
        if (StringUtil.isEmpty(no)) return 0;
        String[] parts = no.split("\\|", -1);
        return (parts.length >= 4) ? NumberUtils.StringtoInt(parts[3]) : 0;
    }

    /** 构建 RNA 完成应答消息 */
    private RNAFinishAckMessage buildRNAFinishAck(RNAStatusItem s) {
        RNAFinishAckMessage ack = new RNAFinishAckMessage();
        ack.setType("RNAFinnishAck");
        ack.setDeviceType(s.getDeviceType());
        ack.setDeviceID(s.getDeviceID());
        ack.setPosition(s.getPosition());
        ack.setSeqId(111);
        ack.setReply("ok");
        return ack;
    }

    /** 构建 PCR 完成应答消息 */
    private PCRFinishAckMessage buildPCRFinishAck(PCRStatusItem s) {
        PCRFinishAckMessage ack = new PCRFinishAckMessage();
        ack.setType(EnumMessageEvent.WSS_PCR_Finish_Ack.getMessage());
        ack.setDeviceType(s.getDeviceType());
        ack.setDeviceID(s.getDeviceID());
        ack.setPosition(s.getPosition());
        ack.setSeqId(111);
        ack.setReply("ok");
        return ack;
    }

    private PCRFinishAckMessage buildPCRFinishAck(PCRStatusItem s,boolean result) {
        PCRFinishAckMessage ack = new PCRFinishAckMessage();
        ack.setType(EnumMessageEvent.WSS_PCR_Finish_Ack.getMessage());
        ack.setDeviceType(s.getDeviceType());
        ack.setDeviceID(s.getDeviceID());
        ack.setPosition(s.getPosition());
        ack.setSeqId(111);
        if(result){
            ack.setReply("ok");
        }else{
            ack.setReply("error");
        }

        return ack;
    }

    /** 根据设备类型创建新的连接 Item */
    private WSSConnectionItem createConnectionItem(String deviceType, String key,
                                                    InetSocketAddress addr, WebSocket conn) {
        WSSConnectionItem item = null;
        if ("RNA".equals(deviceType)) {
            item = new RNAStatusItem();
        } else if ("PCR".equals(deviceType) || "SLANPCR".equals(deviceType)) {
            item = new PCRStatusItem();
        }
        if (item != null) {
            item.setDeviceType(deviceType);
            item.setDeviceID(key);
            item.setDeviceIP(addr.getAddress().toString());
            item.setDevicePort(String.valueOf(addr.getPort()));
            item.setWebSocketConnection(conn);
            Log.i(TAG, "createConnectionItem: new " + deviceType + " key=" + key);
        }
        return item;
    }

    /** 检查 PCR 连接是否处于忙状态 */
    private boolean isPCRBusy(PCRStatusItem item) {
        return "running".equals(item.getpCRLinuxStatus())
                || "acking".equals(item.getpCRLinuxStatus())
                || "error".equals(item.getpCRStatus())
                || "error".equals(item.getpCRLinuxStatus());
    }

    /** 设置断线状态 */
    private void setDisconnectStatus(WSSConnectionItem item) {
        item.setWSSConnect(false);
        if (item instanceof PCRStatusItem) {
            PCRStatusItem s = (PCRStatusItem) item;
            s.setpCRLinuxStatus("disconnect");
            s.setpCRStatus("disconnect");
        } else if (item instanceof RNAStatusItem) {
            RNAStatusItem s = (RNAStatusItem) item;
            s.setrNALinuxStatus("disconnect");
            s.setrNAStatus("disconnect");
        }
    }

    /** 判断连接是否已关闭 */
    private boolean isConnectionClosed(WSSConnectionItem item) {
        WebSocket ws = item.getWebSocketConnection();
        return ws.isClosing() || ws.isClosed() || ws.isFlushAndClose();
    }

    /** 获取并校验 PCRStatusItem，失败时返回 null */
    private PCRStatusItem requirePCRItem(String key, String caller) {
        if (wssConnectionItems.isEmpty()) {
            Log.e(TAG, caller + ": no connections");
            return null;
        }
        WSSConnectionItem conn = wssConnectionItems.get(key);
        if (conn == null) {
            Log.e(TAG, caller + ": key not exist=" + key);
            return null;
        }
        if (!(conn instanceof PCRStatusItem)) {
            Log.e(TAG, caller + ": " + key + " is not PCR device");
            return null;
        }
        return (PCRStatusItem) conn;
    }

    /** 保存 bin 文件到外部存储 PCR/S*/
    private boolean saveBinFile(byte[] bin, String deviceID, String finDeviceID, StringBuilder filePathOut) {
        if (bin == null) {
            Log.e(TAG, finDeviceID + " 保存bin文件失败：字节数组为空");
            Log.e("slanFinish", finDeviceID + " 保存bin文件失败：字节数组为空");
            return false;
        }
        String fileName = deviceID + "-" + DateFormatUtil.getFileName() + ".pcr";
        Log.e("slanFinish", " fileName==="+ fileName);
        try {
            File file = new File(FileUtils.SLAN_RESULT_PATH, fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bin);
            }
            String fullPath = file.getAbsolutePath();
            filePathOut.append(fullPath);
            Log.d("slanFinish", finDeviceID + " 保存bin文件成功: " + fullPath);
            Log.d(TAG, finDeviceID + " 保存bin文件成功: " + fullPath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, finDeviceID + " 保存bin文件失败: " + e.getMessage(), e);
            Log.e("slanFinish", finDeviceID + " 保存bin文件失败: " + e.getMessage(), e);
            return false;
        }
    }


    // ============================================================ 心跳检测

    // 静态 Handler 防止内存泄漏
    private static class StatusHandler extends Handler {
        private final WeakReference<WSSModuleManager> ref;
        StatusHandler(WSSModuleManager mgr) {
            ref = new WeakReference<>(mgr);
        }
        @Override
        public void handleMessage(Message msg) {
            WSSModuleManager mgr = ref.get();
            if (mgr != null) mgr.checkLineStatus();
        }
    }

    private final Handler mStatusHandler = new StatusHandler(this);

    private class HandleDisconnectThread extends Thread {
        @Override
        public void run() {
            while (flag_wss) {
                try {
                    Thread.sleep(5000L);
                    if (mStatusHandler != null) {
                        mStatusHandler.sendEmptyMessage(0);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "HandleDisconnectThread interrupted: " + e.getMessage());
                }
            }
        }
    }

    private void checkLineStatus() {
        if (wssConnectionItems.isEmpty()) {
            EventBus.getDefault().post(new WSSMessageEvent(
                    EnumMessageEvent.WSSManager_UpdateConnection, null, "wssConnectionItems is empty"));
            return;
        }
        List<String> toRemove = new ArrayList<>();
        synchronized (wssConnectionItemsLock) {
            // 第一轮：发送心跳，收集需要移除的 key
            for (Map.Entry<String, WSSConnectionItem> entry : wssConnectionItems.entrySet()) {
                String key = entry.getKey();
                WSSConnectionItem item = entry.getValue();
                if (isConnectionClosed(item)) {
                    item.setWSSConnect(false);
//                    item.stopAckTimer();
                    setDisconnectStatus(item);
                    //toRemove.add(key);
                    Log.i(TAG, "checkLineStatus: closed key=" + key);
                } else {
                    HeartBeatMessage hb = new HeartBeatMessage();
                    hb.setSeqId(msqid++);
                    hb.setDeviceID(item.getDeviceID());
                    hb.setDeviceType(item.getDeviceType());
                    hb.setPosition(item.getPosition());
                    hb.setType("HeartBeat");
                    hb.setTime(DateUtil.getTime());
                    item.setCount(item.getCount() + 1);
                    String result = sendWSSMessage(key, hb);
                    Log.i(TAG, "checkLineStatus check close key=" + key + "--" + item.getDevicePort()
                            + (result.contains("error") ? " failed" : " succeed") );
                }
            }
            // 第二轮：检查超时未回复
            for (Map.Entry<String, WSSConnectionItem> entry : wssConnectionItems.entrySet()) {
                WSSConnectionItem item = entry.getValue();
                Log.i(TAG, "checkLineStatus heartbeat key=" + item.getDeviceID()
                        + "count : " + item.getCount());
                if (/*isConnectionClosed(item) ||*/ item.getCount() >= 4) {
                    Log.i(TAG, "checkLineStatus heartbeat key=" + item.getDeviceID()
                            + "exceed retry count");
                    item.setWSSConnect(false);
//                    item.stopAckTimer();
                    toRemove.add(entry.getKey());
                }
            }
            // 关闭并移除断连的 item
            for (String key : toRemove) {
                WSSConnectionItem item = wssConnectionItems.get(key);
                if (item != null && !item.isWSSConnect()) {
                    Log.e(TAG, "checkLineStatus: disconnecting key=" + key);
                    item.getWebSocketConnection().close(CloseFrame.ABNORMAL_CLOSE);
                    setDisconnectStatus(item);
                }
            }
        }
        EventBus.getDefault().post(new WSSMessageEvent(
                EnumMessageEvent.WSSManager_UpdateConnection, null, "checkLineStatus done"));
    }
}
