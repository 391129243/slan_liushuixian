package com.gzlab.autoassemblyline_client.autopipeline.mgr;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.gzlab.autoassemblyline_client.PcrApplication;
import com.gzlab.autoassemblyline_client.WintersUtils.utils.GsonUtil;
import com.gzlab.autoassemblyline_client.autopipeline.data.SaveReqResult;
import com.gzlab.autoassemblyline_client.autopipeline.event.EnumMessageEvent;
import com.gzlab.autoassemblyline_client.autopipeline.event.WSSMessageEvent;
import com.gzlab.autoassemblyline_client.autopipeline.message.PCRReqMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.UpgradeMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.base.BinBaseMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.SLAN_PCRReqMessage;
import com.gzlab.autoassemblyline_client.autopipeline.utils.BinWssMessageUtils;
import com.gzlab.autoassemblyline_client.util.FileUtils;

import org.greenrobot.eventbus.EventBus;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * WebSocket长连接客户端
 */
public class WSSClient extends WebSocketClient {
    private static final String TAG = "WSSClient";

    private static volatile WSSClient instance;
    //============================实例变量==========================//
    private final ExecutorService messageExecutor;           // 消息处理线程池
    private final Handler mainHandler;                       // 主线程Handler
    // TCP 连接超时：不设置时 socket.connect 可能一直卡住，connectBlocking 超时返回后
    // 旧实例仍在后台连接，之后若连上会变成"僵尸连接"
    private static final int SOCKET_CONNECT_TIMEOUT_MS = 5000;
    // 心跳 ping/pong 检测：路由器重启等半开连接，默认 60 秒才发现，缩短到 20 秒
    private static final int CONNECTION_LOST_TIMEOUT_SEC = 20;

    private WSSClient(URI serverUri) {
        super(serverUri, new Draft_6455(), null, SOCKET_CONNECT_TIMEOUT_MS);
        setConnectionLostTimeout(CONNECTION_LOST_TIMEOUT_SEC);
        this.messageExecutor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 是否为当前有效实例。每次重连都会 new 新实例，旧实例的 onOpen/onClose/onError
     * 可能在新实例创建之后才回调，若不过滤会把 WSSModuleManager 的连接状态改错
     * （例如旧实例 onOpen 把状态置为已连接，而当前实例其实没连上，检查器就不再重连）
     */
    private boolean isCurrent() {
        return this == instance;
    }

    public static WSSClient getInstance() {
        // 关键：为空 或 已关闭 就重建
        Log.e(TAG, "【新建连接】 instance = " + instance);
       // Log.e(TAG, "【新建连接】 instance.isOpen() = " + instance.isOpen());
        if (instance == null || !instance.isOpen()) {
            synchronized (WSSClient.class) {
                // 每次都重新获取最新IP端口
                String ip = PcrApplication.wssServerIP;
                String port = PcrApplication.wssServerPort;

                URI uri = URI.create("ws://" + ip + ":" + port);
                Log.e(TAG, "【新建连接】 URI = " + uri);

                instance = new WSSClient(uri);
            }
        }
        return instance;
    }

    /**
     * 关闭连接并释放消息处理线程池
     */
    public void shutdown() {
        try {
            close();
        } catch (Exception ignored) {
        }
        if (messageExecutor != null) {
            messageExecutor.shutdownNow();
        }
    }



    @Override
    public void onOpen(ServerHandshake handshakedata) {
        if (!isCurrent()) {
            // 已被替换的旧实例迟到连上：直接关掉，避免服务端出现多条连接
            Log.w(TAG, "过期实例 onOpen，关闭: " + this);
            try { close(); } catch (Exception ignored) { }
            return;
        }
        Log.i(TAG, "Connected to server");
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Connect,""));
    }

    @Override
    public void onMessage(String message) {
        if (!isCurrent() || messageExecutor.isShutdown()) {
            return;
        }
        Log.i(TAG, "Received text: " + (message.length() > 100 ? message.substring(0, 100) + "..." : message));
        // 使用线程池处理，避免阻塞WebSocket线程
        messageExecutor.execute(() -> handleTextMessage(message));

    }
    @Override
    public void onMessage(ByteBuffer bytes) {
        if (!isCurrent() || messageExecutor.isShutdown()) {
            return;
        }
        Log.i(TAG, "Received binary: " + bytes.remaining() + " bytes");
        // 使用线程池处理，避免阻塞WebSocket线程
        ByteBuffer msgCopy = bytes.asReadOnlyBuffer();
        messageExecutor.execute(() -> handleBinaryMessage(msgCopy));

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (!isCurrent()) {
            Log.i(TAG, "过期实例 onClose，忽略: code=" + code + " reason=" + reason);
            return;
        }
        Log.i("WSSClient", "Connection closed: code=" + code + " reason=" + reason + " remote=" + remote);
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_CLOSE,reason));
    }

    @Override
    public void onError(Exception ex) {
        if (!isCurrent()) {
            Log.i(TAG, "过期实例 onError，忽略: " + ex.getMessage());
            return;
        }
        Log.e("WSSClient", "onError() 连接出错：" + ex.getMessage());
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Error,ex.getMessage()));
    }

    // ==================== 消息处理 ====================

    /**
     * 处理文本消息
     * @param message
     */
    private void handleTextMessage(String message) {
        try {
            String realMessage = message.replaceAll("[\\x00-\\x1F\\x7F]+$", "").trim();
            Map<String, Object> jsonMap = GsonUtil.fromJson(realMessage, Map.class);

            if (jsonMap == null || jsonMap.isEmpty()) {
                Log.w(TAG, "解析JSON失败或为空");
                return;
            }

            String type = (String) jsonMap.get("Type");
            if (type == null) {
                Log.w(TAG, "消息中无Type字段");
                return;
            }



            // 处理不同类型的消息
            switch (type) {
                case "HeartBeat":
                    Log.i(TAG, "收到心跳消息");
                    postEvent(EnumMessageEvent.WSS_HeartBeat, message);
                    break;

                case "QueryDevice":
                    Log.i(TAG, "收到设备查询消息");
                    postEvent(EnumMessageEvent.WSS_QueryDevice, message);
                    break;

                case "RestartPCRApp":
                    Log.i(TAG, "收到重启App消息");
                    postEvent(EnumMessageEvent.WSS_RESTART_PCR_APP, message);
                    break;

                case "PCRFinishAck":
                    Log.i(TAG, "收到PCR完成确认消息");
                    postEvent(EnumMessageEvent.WSS_PCR_Finish_Ack, message);
                    break;

                case "PCRStop":
                    Log.i(TAG, "收到PCR停止消息");
                    postEvent(EnumMessageEvent.WSS_PCR_STOP, message);
                    break;
                case "PCRReq" :
                    Log.i("PCR复位", "收到PCR复位的消息");
                    PCRReqMessage pCRReqMessage = GsonUtil.fromJson(message, PCRReqMessage.class);
                    if (pCRReqMessage != null && pCRReqMessage.getTemplate().contains("reset")) {
                        postEvent(EnumMessageEvent.WSS_SLAN_PCR_RESET, pCRReqMessage.getTemplate());
                    }
                    break;
                default:
                    Log.i(TAG, "收到其他类型消息: " + type);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "处理文本消息异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理二进制消息
     */
    private void handleBinaryMessage(ByteBuffer bytes) {
        try {
            BinBaseMessage binBaseMessage = BinWssMessageUtils.parseBinMessage(bytes);
            if (binBaseMessage == null) {
                Log.w(TAG, "解析二进制消息失败");
                return;
            }

            String type = binBaseMessage.getType();
            Log.i(TAG, "解析到二进制消息: Type=" + type + ", DeviceID=" + binBaseMessage.getDeviceID());

            // 处理不同类型的二进制消息
            if (EnumMessageEvent.WSS_SLAN_REQ.getType().equals(type)) {
                handleSLANRequest((SLAN_PCRReqMessage) binBaseMessage);
            } else if (EnumMessageEvent.WSS_UGRAGE.getType().equals(type)) {
                handleUpgradeRequest((UpgradeMessage) binBaseMessage);
            } else {
                Log.i(TAG, "未知二进制消息类型: " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "处理二进制消息异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理SLAN请求
     */
    private void handleSLANRequest(SLAN_PCRReqMessage slanReqMessage) {
        String templateName = slanReqMessage.getTemplate();
        String fileName = slanReqMessage.getFileName();
        // 为每个模板创建独立的时间戳+模板名目录，便于后续排查
        String timestamp = String.valueOf(System.currentTimeMillis());
        // 清理模板名称中的非法字符，避免文件系统路径问题
        String safeTemplateName = templateName.replaceAll("[\\\\/:*?\"<>|]", "_");
        String saveDirPath = FileUtils.PCR_MODULE_PATH + File.separator + "tempFolder" + File.separator + timestamp + "_" + safeTemplateName + File.separator;
        String filePath = saveDirPath + fileName;

        Log.i(TAG, "收到PCR模板请求: templateName=" + templateName + ", fileName=" + fileName);
        Log.i(TAG, "模板保存路径: " + filePath);

        // 保存模板文件
        boolean saveResult = BinWssMessageUtils.writeBytesToFile(filePath, slanReqMessage.getFileData());

        // 封装结果
        SaveReqResult saveReqResult = new SaveReqResult();
        saveReqResult.setSaveFilePath(filePath);
        saveReqResult.setSlan_pcrReqMessage(slanReqMessage);
        saveReqResult.setTemplateName(templateName);
        saveReqResult.setPcrFileName(fileName);
        saveReqResult.setSaveDirPath(saveDirPath);
        saveReqResult.setSaveResult(saveResult);

        Log.i(TAG, "保存PCR模板结果: " + saveResult + ", 完整路径: " + filePath);

        // 发送EventBus消息
        EventBus.getDefault().post(
                new WSSMessageEvent(EnumMessageEvent.WSSManager_PCR_REQ_UPDATE, saveReqResult, saveResult ? 1 : 0)
        );
    }

    /**
     * 处理升级请求
     */
    private void handleUpgradeRequest(UpgradeMessage upgradeMessage) {
        String fileNamePath = FileUtils.UPDATE_PATH + upgradeMessage.getFileName();
        Log.i(TAG, "收到升级请求: fileName=" + upgradeMessage.getFileName());

        boolean saveResult = BinWssMessageUtils.writeBytesToFile(fileNamePath, upgradeMessage.getFileData());

        int resultCode = saveResult ? 1 : 0;
        EventBus.getDefault().post(
                new WSSMessageEvent(EnumMessageEvent.WSS_UGRAGE, upgradeMessage, resultCode)
        );

        Log.i(TAG, "升级文件保存结果: " + saveResult);
    }

    // ==================== 工具方法 ====================

    /**
     * 在主线程发送EventBus事件
     */
    private void postEvent(EnumMessageEvent event, String data) {
        mainHandler.post(() -> {
            EventBus.getDefault().post(new WSSMessageEvent(event, data));
        });
    }

    private void postEvent(EnumMessageEvent event, Object data, int arg) {
        mainHandler.post(() -> EventBus.getDefault().post(new WSSMessageEvent(event, data, arg)));
    }

}
