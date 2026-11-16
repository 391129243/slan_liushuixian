package com.winters.boweilibrary.autopipeline.Wssclient;

import android.os.Environment;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.message.BinBaseMessage;
import com.winters.boweilibrary.autopipeline.model.message.HeartBeatReplyMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRStatusMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeAckMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeMessage;
import com.winters.boweilibrary.autopipeline.utils.IPToolsUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

public class PCRWssclient extends WebSocketClient {

    private static volatile PCRWssclient instance;

    private static PCRStatusItem pCRStatusItem;

    private Thread statusThread;
    private volatile boolean statusThreadRunning = false;

    private PCRWssclient(URI serverUri) {
        super(serverUri);
    }

    /// 平常的设备状态发送线程，status、alarm 宏石PCR模块  linuxstatus、linuxalarm代表上位机或者SDK状态
    /// 运行PCR流程时发送状态实时运行信息
    private void startPCRRunningStatusThread() {

    }


    /// 运行PCR流程时20秒间隔或者更大间隔发送曲线状态
    private void startPCRCurveThread() {

    }

    /// 平常的设备状态发送线程，status、alarm 宏石PCR模块  linuxstatus、linuxalarm代表上位机或者SDK状态
    /// 一直运行
    private void startPCRStatusThread() {
        if (statusThreadRunning) return;
        statusThreadRunning = true;
        statusThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (statusThreadRunning) {
                    try {
                        // 构造 PCRStatusItem 消消息
                        PCRStatusMessage statusMessage = new PCRStatusMessage();
                        statusMessage.setType("PCRStatus");
                        statusMessage.setSeqId(1);
                        statusMessage.setDeviceID(pCRStatusItem.getDeviceID());
                        statusMessage.setDeviceType(pCRStatusItem.getDeviceType());
                        statusMessage.setPosition(pCRStatusItem.getPosition());
                        statusMessage.setStatus(pCRStatusItem.getpCRStatus());
                        statusMessage.setStatus("free");
                        statusMessage.setAlarm("");
                        statusMessage.setLinuxStatus("");
                        statusMessage.setLinuxAlarm("");
                        String statusJson = GsonUtil.toJson(statusMessage,false);
                        send(statusJson);
                        Log.i("WebSocketClient", "Sent PCRStatusMessage: " + statusJson);
                        Thread.sleep(2000); // 每5秒发送一次
                    } catch (Exception e) {
                        Log.e("WebSocketClient", "PCRStatusThread error", e);
                    }
                }
            }
        });
        statusThread.start();
    }

    private void stopPCRStatusThread() {
        statusThreadRunning = false;
        if (statusThread != null) {
            statusThread.interrupt();
            statusThread = null;
        }
    }

    public static PCRWssclient getInstance() {
        if (instance == null) {
            synchronized (PCRWssclient.class) {
                if (instance == null) {

                    String ip = IPToolsUtils.getHostIp();

                    URI uri = URI.create("ws://" + ip + ":8080");

                    //InetSocketAddress myHost = new InetSocketAddress(ip, 8080);
                    //websocketServer = new WebSocketServer(myHost);

                    instance = new PCRWssclient(uri);
                    try {
                        Log.i("WSSClient", "Trying to connect to " + uri);
                        instance.connect();
                        Log.i("WSSClient", "connect() called");

                        pCRStatusItem = new PCRStatusItem();
                        pCRStatusItem.setDeviceType("PCR");
                        pCRStatusItem.setDeviceID("PCR1111");
                        pCRStatusItem.setPosition(1);
                        pCRStatusItem.setpCRStatus("free");
                        pCRStatusItem.setpCRAlarm("");
                        pCRStatusItem.setpCRLinuxStatus("free");
                        pCRStatusItem.setpCRLinuxAlarm("");

                        instance.startPCRStatusThread(); // 启动定时发送线程

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
                    heartBeatReplyMessage.setDeviceID(pCRStatusItem.getDeviceID());
                    heartBeatReplyMessage.setDeviceType(pCRStatusItem.getDeviceType());
                    heartBeatReplyMessage.setPosition(pCRStatusItem.getPosition());
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

                pCRStatusItem.setVersion(upgradeMessage.getFileName());

                com.winters.boweilibrary.autopipeline.utils.BinWssMessageUtils.writeBytesToFile(fileName, upgradeMessage.getFileData());

                UpgradeAckMessage upgradeAckMessage = new UpgradeAckMessage();
                upgradeAckMessage.setType("UpgradeAck");
                upgradeAckMessage.setSeqId(1);
                upgradeAckMessage.setDeviceID(pCRStatusItem.getDeviceID());
                upgradeAckMessage.setDeviceType(pCRStatusItem.getDeviceType());
                upgradeAckMessage.setPosition(pCRStatusItem.getPosition());
                upgradeAckMessage.setReply("ok");
                upgradeAckMessage.setError("");
                String ackJson = GsonUtil.toJson(upgradeAckMessage,false);
                Log.i("WebSocketClient", "Sent upgradeAckMessage: " + ackJson);
                send(ackJson);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                UpgradeFinishMessage upgradeFinishMessage = new UpgradeFinishMessage();

                upgradeFinishMessage.setType("UpgradeFinish");
                upgradeFinishMessage.setSeqId(1);
                upgradeFinishMessage.setDeviceID(pCRStatusItem.getDeviceID());
                upgradeFinishMessage.setDeviceType(pCRStatusItem.getDeviceType());
                upgradeFinishMessage.setPosition(pCRStatusItem.getPosition());
                upgradeFinishMessage.setReply("ok");
                upgradeFinishMessage.setError("");
                String ackJsonupgradeFinishMessage = GsonUtil.toJson(upgradeFinishMessage,false);
                Log.i("WebSocketClient", "Sent UpgradeFinishMessage: " + ackJson);
                send(ackJsonupgradeFinishMessage);

            }
        } else {
            //Log.e("WebSocketClient", "Failed to parse BinBaseMessage");
        }

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i("WebSocketClient", "Connection closed: " + reason);
        stopPCRStatusThread(); // 关闭定时线程
    }

    @Override
    public void onError(Exception ex) {
        Log.e("WebSocketClient", "Error", ex);
        stopPCRStatusThread(); // 关闭定时线程
    }


}
