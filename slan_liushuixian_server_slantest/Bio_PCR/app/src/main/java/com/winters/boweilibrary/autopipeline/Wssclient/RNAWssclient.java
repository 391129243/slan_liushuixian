package com.winters.boweilibrary.autopipeline.Wssclient;

import android.os.Environment;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.message.BinBaseMessage;
import com.winters.boweilibrary.autopipeline.model.message.HeartBeatReplyMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAStatusMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeMessage;
import com.winters.boweilibrary.autopipeline.utils.IPToolsUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

public class RNAWssclient  extends WebSocketClient {

    private static volatile RNAWssclient instance;

    private static RNAStatusItem rNAStatusItem;

    private Thread statusThread;
    private volatile boolean statusThreadRunning = false;

    private RNAWssclient(URI serverUri) {
        super(serverUri);
    }



    /// 平常的设备状态发送线程，status、alarm RNA模块  linuxstatus、linuxalarm代表上位机或者SDK状态
    /// 一直运行
    private void startRNAStatusThread() {
        if (statusThreadRunning) return;
        statusThreadRunning = true;
        statusThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (statusThreadRunning) {
                    try {
                        // 构造 RNAStatusItem 消消息
                        RNAStatusMessage statusMessage = new RNAStatusMessage();
                        statusMessage.setType("RNAStatus");
                        statusMessage.setSeqId(1);
                        statusMessage.setDeviceID(rNAStatusItem.getDeviceID());
                        statusMessage.setDeviceType(rNAStatusItem.getDeviceType());
                        statusMessage.setPosition(rNAStatusItem.getPosition());
                        statusMessage.setStatus(rNAStatusItem.getrNAStatus());
                        statusMessage.setStatus("free");
                        statusMessage.setAlarm("");
                        statusMessage.setLinuxStatus("");
                        statusMessage.setLinuxAlarm("");
                        String statusJson = GsonUtil.toJson(statusMessage,false);
                        send(statusJson);
                        Log.i("WebSocketClient", "Sent RNAStatusMessage: " + statusJson);
                        Thread.sleep(2000); // 每5秒发送一次
                    } catch (Exception e) {
                        Log.e("WebSocketClient", "RNAStatusThread error", e);
                    }
                }
            }
        });
        statusThread.start();
    }

    private void stopRNAStatusThread() {
        statusThreadRunning = false;
        if (statusThread != null) {
            statusThread.interrupt();
            statusThread = null;
        }
    }

    public static RNAWssclient getInstance() {
        if (instance == null) {
            synchronized (RNAWssclient.class) {
                if (instance == null) {

                    String ip = IPToolsUtils.getHostIp();

                    URI uri = URI.create("ws://" + ip + ":8080");

                    //InetSocketAddress myHost = new InetSocketAddress(ip, 8080);
                    //websocketServer = new WebSocketServer(myHost);

                    instance = new RNAWssclient(uri);
                    try {
                        Log.i("WSSClient", "Trying to connect to " + uri);
                        instance.connect();
                        Log.i("WSSClient", "connect() called");

                        rNAStatusItem = new RNAStatusItem();
                        rNAStatusItem.setDeviceType("RNA");
                        rNAStatusItem.setDeviceID("RNA1111");
                        rNAStatusItem.setPosition(1);
                        rNAStatusItem.setrNAStatus("free");
                        rNAStatusItem.setrNAAlarm("");
                        rNAStatusItem.setrNALinuxStatus("free");
                        rNAStatusItem.setrNALinuxAlarm("");

                        instance.startRNAStatusThread(); // 启动定时发送线程


                    } catch (Exception e) {
                        Log.e("WSSClient", "Connect failed", e);
                        return null;
                    }
                }
            }
        }
        return instance;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i("WebSocketClient", "Connected to server");
    }

    @Override
    public void onMessage(String message) {
        Log.i("WebSocketClient", "Received text: " + message);
        Map<String, Object> jsonMap = GsonUtil.fromJson((String) message, Map.class);
        if (jsonMap != null && jsonMap.size() > 0) {
            boolean containsField = jsonMap.containsKey("Type");
            if (containsField) {
                String type = (String) jsonMap.get("Type");
                if (type.equals("HeartBeat")) {
                    Log.i("WebSocketClient", "HeartBeat received: " + message);
                    HeartBeatReplyMessage heartBeatReplyMessage = new HeartBeatReplyMessage();
                    heartBeatReplyMessage.setType("HeartBeatReply");
                    heartBeatReplyMessage.setSeqId(1);
                    heartBeatReplyMessage.setDeviceID(rNAStatusItem.getDeviceID());
                    heartBeatReplyMessage.setDeviceType(rNAStatusItem.getDeviceType());
                    heartBeatReplyMessage.setPosition(rNAStatusItem.getPosition());
                    heartBeatReplyMessage.setVersion(System.currentTimeMillis() + "");
                    String replyJson = GsonUtil.toJson(heartBeatReplyMessage,false);
                    send(replyJson);
                    Log.i("WebSocketClient", "Sent HeartBeatReply: " + replyJson);
                    // 这里可以添加处理 QueryDeviceReply 的逻辑
                } else if (type.equals("QueryDevice")) {
                    Log.i("WebSocketClient", "QueryDevice received: " + message);
                    // 这里可以添加处理 QueryDeviceReply 的逻辑
                } else {
                    Log.i("WebSocketClient", "Other message type received: " + message);
                    // 这里可以添加处理其他消息类型的逻辑
                }
            }
        }

    }
    @Override
    public void onMessage(ByteBuffer bytes) {
        Log.i("WebSocketClient", "Received binary: " + bytes.remaining() + " bytes");
        // 异步处理 parseBinMessage，避免阻塞主线程
        ByteBuffer msgCopy = bytes.asReadOnlyBuffer();

        // 替换为你的实际解析方法
        BinBaseMessage binBaseMessage = com.winters.boweilibrary.autopipeline.utils.BinWssMessageUtils.parseBinMessage(msgCopy);

        if (binBaseMessage != null) {
            Log.i("WebSocketClient", "Parsed BinBaseMessage: Type=" + binBaseMessage.getType() +
                    ", DeviceID=" + binBaseMessage.getDeviceID() +
                    ", FileName=" + binBaseMessage.getFileName() +
                    ", FileSize=" + binBaseMessage.getFileSize());
            // 这里可以添加处理 BinBaseMessage 的逻辑
            if(binBaseMessage.getType().equals("Upgrade")){
                UpgradeMessage upgradeMessage = (UpgradeMessage) binBaseMessage;
                String fileName = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/" + DateUtil.getTimeShort() + upgradeMessage.getFileName();

                rNAStatusItem.setVersion(upgradeMessage.getFileName());

                com.winters.boweilibrary.autopipeline.utils.BinWssMessageUtils.writeBytesToFile(fileName, upgradeMessage.getFileData());

                UpgradeAckMessage upgradeAckMessage = new UpgradeAckMessage();
                upgradeAckMessage.setType("UpgradeAck");
                upgradeAckMessage.setSeqId(1);
                upgradeAckMessage.setDeviceID(rNAStatusItem.getDeviceID());
                upgradeAckMessage.setDeviceType(rNAStatusItem.getDeviceType());
                upgradeAckMessage.setPosition(rNAStatusItem.getPosition());
                upgradeAckMessage.setReply("ok");
                upgradeAckMessage.setError("");
                String ackJson = GsonUtil.toJson(upgradeAckMessage,false);
                Log.i("WebSocketClient", "Sent RNAAckMessage: " + ackJson);
                send(ackJson);

                UpgradeFinishMessage upgradeFinishMessage = new UpgradeFinishMessage();

                upgradeFinishMessage.setType("UpgradeFinish");
                upgradeFinishMessage.setSeqId(1);
                upgradeFinishMessage.setDeviceID(rNAStatusItem.getDeviceID());
                upgradeFinishMessage.setDeviceType(rNAStatusItem.getDeviceType());
                upgradeFinishMessage.setPosition(rNAStatusItem.getPosition());
                upgradeFinishMessage.setReply("ok");
                upgradeFinishMessage.setError("");
                String ackJsonupgradeFinishMessage = GsonUtil.toJson(upgradeFinishMessage,false);
                Log.i("WebSocketClient", "Sent upgradeFinishMessage: " + ackJson);
                send(ackJsonupgradeFinishMessage);
            }
        } else {
            //Log.e("WebSocketClient", "Failed to parse BinBaseMessage");
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i("WebSocketClient", "Connection closed: " + reason);
        stopRNAStatusThread(); // 关闭定时线程
    }

    @Override
    public void onError(Exception ex) {
        Log.e("WebSocketClient", "Error", ex);
        stopRNAStatusThread(); // 关闭定时线程
    }


}