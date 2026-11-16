package com.gzlab.autoassemblyline_client.autopipeline.mgr;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.gzlab.autoassemblyline_client.PcrApplication;
import com.gzlab.autoassemblyline_client.WintersUtils.utils.GsonUtil;
import com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Well;
import com.gzlab.autoassemblyline_client.autopipeline.data.ConstantData;
import com.gzlab.autoassemblyline_client.autopipeline.data.SaveReqResult;
import com.gzlab.autoassemblyline_client.autopipeline.enumtype.EnumError;
import com.gzlab.autoassemblyline_client.autopipeline.enumtype.EnumLinuxAlarm;
import com.gzlab.autoassemblyline_client.autopipeline.event.EnumMessageEvent;
import com.gzlab.autoassemblyline_client.autopipeline.event.WSSMessageEvent;
import com.gzlab.autoassemblyline_client.autopipeline.message.HeartBeatMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.HeartBeatReplyMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.PCRAckMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.PCRFinishAckMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.PCRFinishMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.PCRStatusMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.QueryDeviceMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.QueryDeviceReplyMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.RestartPCRAppMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.RestartPCRAppReplyMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.UpgradeAckMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.UpgradeFinishMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.UpgradeMessage;
import com.gzlab.autoassemblyline_client.constant.SharedConstant;
import com.gzlab.autoassemblyline_client.event.WSSStatusEvent;
import com.gzlab.autoassemblyline_client.util.CommonUtil;
import com.gzlab.autoassemblyline_client.util.InternetUtil;
import com.gzlab.autoassemblyline_client.util.PreferenceUtil;
import com.hongshi.slansdk.model.experiment.RunningInfo;
import com.hongshi.slansdk.slanenum.DeviceState;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WSS模块管理器（优化版）
 * 职责：WSS连接管理、消息收发、设备状态上报
 */
public class WSSModuleManager {
    private static final String TAG = "WSSModuleManager";
    //常量配置
    private static final int CONNECT_CHECK_INTERVAL = 2000;   // 检查器节拍：每 2 秒看一次是否该重连
    private static final int INITIAL_DELAY = 4000;
    private static final long CONNECT_TIMEOUT_SEC = 5;   // connectBlocking 握手超时
    // 重连指数退避：失败后等待时长 2s→4s→8s→...→30s 封顶，连接成功后重置为 2s。
    // 检查器仍每 2 秒跑一次，但只有过了“下次允许重连时刻”才真正发起，避免长断网时高频空连。
    private static final long RECONNECT_BACKOFF_MIN_MS = 2000L;
    private static final long RECONNECT_BACKOFF_MAX_MS = 30000L;
    private static final int MAX_FINISH_RETRY = 100000;//一直循环发送
    private static final long FINISH_ACK_TIMEOUT = 60000;//30秒
    private static final long FINISH_RETRY_DELAY_MS = 1000L; // 重发间隔，避免同步递归栈溢出

    //=======================单例=============================
    private static volatile WSSModuleManager mInstance;
    //====================上下文==============================
    private Context appContext;
    //====================客户端=============================
    private volatile WSSClient wssClient = null;

    private final ReentrantLock wssLock = new ReentrantLock();
    private final AtomicBoolean isWssOpen = new AtomicBoolean(false);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);

    private final AtomicInteger seqId = new AtomicInteger(0);
    //wss的连接状态

    //===========================现场池，重连相关==========================
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> reconnectCheckFuture;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 退避状态：下次允许重连的时刻(SystemClock.elapsedRealtime 基准)与当前退避时长
    private volatile long mNextReconnectAtMs = 0L;
    private volatile long mCurrentBackoffMs = RECONNECT_BACKOFF_MIN_MS;

    private final Map<String, Object> messageCache = new ConcurrentHashMap<>();
    private static boolean timeInit = false;

    public WSSModuleManager(Context context){
        this.appContext = context;
        initThreadPools();
    }

    public static WSSModuleManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (WSSModuleManager.class) {
                if (mInstance == null) {
                    mInstance = new WSSModuleManager(context);
                }
            }
        }
        return mInstance;
    }



    private void initThreadPools(){
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WSS-Connection-Checker");
            t.setDaemon(true);
            return t;
        });

    }
    /**
     * 开始进行WSS连接
     * @return
     */
    public void initConnection(){
        //开始进行主控连接
        initializeWssClient();
        registerEventBus();
        startWssConnection();
        startConnectionChecker();
        Log.i(TAG,"WSS连接初始化完成");

    }


    private void registerEventBus(){
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    private void unRegisterEventBus(){
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }


    // ====================== EventBus 注册 ==============================
    private void initializeWssClient(){
        Log.e(TAG,"initializeWssClient --wssClient" + wssClient);
        wssLock.lock();
        try {
            if (wssClient != null) {
                safeCloseWssClient();
            }
            wssClient = WSSClient.getInstance();
            Log.d(TAG, "WSS客户端实例创建成功" + wssClient);
        }finally {
            wssLock.unlock();
        }
    }

    private void safeCloseWssClient() {
        if (wssClient != null) {
            try {
                // 必须用 shutdown() 而非 close()：每个 WSSClient 实例内部持有一个
                // newSingleThreadExecutor（messageExecutor）。close() 只关 socket，
                // 不会回收该线程池。由于每次重连都会 new 一个新的 WSSClient，
                // 仅 close() 会导致旧实例的线程池永久泄漏（线程号可涨到数万），最终 OOM。
                wssClient.shutdown();
            } catch (Exception e) {
                Log.w(TAG, "关闭WSS客户端异常", e);
            }
        }
    }

    //=====================wss连接管理==================================
    /**
     * 启动WSS连接（完全异步）
     */
    public void startWssConnection() {
        Log.i(TAG, "开始异步WSS连接");

        if (!checkNetworkAvailable()) {
            scheduleReconnect();
            return;
        }

        executeInScheduler(new Runnable() {
            @Override
            public void run() {
                wssLock.lock();
                try {
                    // 重置连接状态
                    resetWssClient();

                    // 再次检查网络
                    if (!checkNetworkAvailable()) {
                        notifyConnectionFailed("网络不可用");
                        return;
                    }

                    // 发起连接：用 connectBlocking 确认握手真正完成，
                    // 不能用异步 connect() 后乐观地置为“已连接”——若 TCP 连接静默卡住
                    // （onOpen/onError 都不触发），状态会永远停在“已连接”，重连检查器
                    // 再也不会重试，导致断连后永不恢复。
                    if (wssClient != null && !wssClient.isOpen()) {
                        boolean connected = wssClient.connectBlocking(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS);
                        Log.i(TAG, "WSS连接结果: " + connected);
                        updateConnectionState(connected);
                        if (!connected) {
                            handleConnectionError(new Exception("连接超时或失败"));
                        }
                    } else {
                        Log.w(TAG, "WSS客户端不可用或已连接");
                    }
                }catch (Exception e){
                    Log.e(TAG, "WSS连接异常", e);
                    handleConnectionError(e);
                }finally {
                    wssLock.unlock();
                }
            }
        });
    }

    private void resetWssClient() {
        safeCloseWssClient();
        wssClient = null;
        wssClient = WSSClient.getInstance();
    }

    private void updateConnectionState(boolean open) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                doUpdateWssStatus(open);
            }
        });
    }

    private void doUpdateWssStatus(boolean open) {
        isWssOpen.set(open);
        if (open) {
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Connect, "连接成功"));
        }
    }

    private void handleConnectionError(Exception e) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                isWssOpen.set(false);
                EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Error, e.getMessage()));
                scheduleReconnect();
            }
        });
    }

    private void notifyConnectionFailed(String reason) {
        mainHandler.post(() -> {
            isWssOpen.set(false);
            Log.w(TAG, "连接失败: " + reason);
            scheduleReconnect();
        });
    }

    //===========================连接检查器========================
    private void startConnectionChecker(){
        if (reconnectCheckFuture != null && !reconnectCheckFuture.isDone()) {
            return;
        }

        reconnectCheckFuture = scheduler.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        performConnectionCheck();
                    }
                },
                INITIAL_DELAY,
                CONNECT_CHECK_INTERVAL,
                TimeUnit.MILLISECONDS
        );
        Log.d(TAG, "连接检查器启动");
    }

    //执行连接检查
    private void performConnectionCheck(){
        // 以 socket 实际状态为准：isWssOpen 由异步事件维护，可能与真实状态不一致
        // （例如回调丢失、半开连接），不一致时纠正，否则检查器会永远认为已连接而不重连
        WSSClient client = wssClient;
        boolean socketOpen = client != null && client.isOpen();
        if (isWssOpen.get() && !socketOpen && !isReconnecting.get()) {
            Log.w(TAG, "连接状态不一致：isWssOpen=true 但 socket 未连接，纠正为断开");
            isWssOpen.set(false);
        }
        publishWssStatus(socketOpen);

        boolean open = isWssOpen.get();
        boolean reconnecting = isReconnecting.get();
        Log.d(TAG, String.format("连接检查: open=%s, socketOpen=%s, reconnecting=%s", open, socketOpen, reconnecting));
        if (!open && !reconnecting) {
            // 退避闸门：未到“下次允许重连时刻”就跳过本轮，避免长断网时每 2 秒狂连
            if (!isBackoffElapsed()) {
                return;
            }
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 执行 WebSocket 重连操作
                    reconnectWss();
                }
            });
        }
        if (open && reconnecting){
            isReconnecting.set(false);
        }
    }



    //================================重连逻辑==============================
    private void reconnectWss(){
        Log.i("WSSClient", "reconnectWss 执行重连");
        if(!isReconnecting.compareAndSet(false, true)){
            Log.d(TAG, "正在重连中，忽略本次请求");
            return;
        }
        // 3. 网络检查
        if (!InternetUtil.isConnect(appContext)) {
            Log.i(TAG, "重连失败：无网络");
            // 即使无网络也设置重连状态，避免 CPU 空转，稍后再次尝试
            scheduleReconnect();
            return;
        }
        Log.i(TAG, "开始执行WSS重连");
        executeInScheduler(new Runnable() {
            @Override
            public void run() {
                wssLock.lock();
                try {
                    //执行重连的任务
                    performReconnectTask();
                }finally {
                    wssLock.unlock();
                }
            }
        });
    }

    /**
     * 执行重连的任务
     */
    private void performReconnectTask(){
        try{
            // 排队期间可能已经连上（如 startWssConnection 刚成功），不要把好连接拆掉
            if (wssClient != null && wssClient.isOpen()) {
                Log.i(TAG, "当前连接已打开，跳过本次重连");
                postReconnectSuccess();
                return;
            }
            //步骤1.关闭旧的连接
            safeCloseWssClient();
            wssClient = null;

            //创建新客户端
            wssClient = WSSClient.getInstance();
            //检查网络状态
            if(!checkNetworkAvailable()){
                Log.w(TAG, "重连时网络已断开");
                postReconnectFailed();
                return;
            }
            //4.执行连接：connectBlocking 确认握手真正完成后才算重连成功，
            //  否则继续走失败分支，让检查器下轮再试，避免“假连接”卡死。
            if(wssClient != null && !wssClient.isOpen()){
                boolean connected = wssClient.connectBlocking(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS);
                if (connected) {
                    Log.i(TAG, "重连成功");
                    postReconnectSuccess();
                } else {
                    Log.w(TAG, "重连失败：连接超时");
                    postReconnectFailed();
                }
            }else {
                Log.w(TAG, "重连失败：客户端不可用");
                postReconnectFailed();
            }
        }catch (Exception e){
            handleReconnectException(e);
        }
    }

    /**
     * 通知主线程：重连成功
     */
    private void postReconnectSuccess() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                isWssOpen.set(true);
                isReconnecting.set(false);
                resetReconnectBackoff();   // 连接成功，退避复位，下次断连可立即重连
                // 重连成功后，如果有未确认的Finish消息，重置重试计数并重新发送
                // 避免WSS断线期间Finish消息丢失导致上位机收不到实验结束通知
                if (mCurrentBuffer != null && isWaitingAckTask.get()) {
                    Log.i(TAG, "重连成功，检测到待确认的Finish消息，重新发送");
                    stopAckTimer();
                    mCurrentRetry = 0;
                    isRetryScheduled.set(false);
                    startSendAndWaitAck();
                }
            }
        });
    }

    /**
     * 通知主线程：重连失败
     */
    private void postReconnectFailed() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                // 走统一失败入口：解锁 + 武装退避，让检查器按退避时长而非固定 2 秒再试
                scheduleReconnect();
            }
        });
    }

    /**
     * 处理重连异常
     */
    private void handleReconnectException(Exception e) {
        Log.e(TAG, "重连异常", e);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                isWssOpen.set(false);
                isReconnecting.set(false);
                EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_Error, e.getMessage()));
                scheduleReconnect();
            }
        });
    }

    /**
     * 统一的重连失败入口：解锁 isReconnecting + 按指数退避推后“下次允许重连时刻”。
     * 不再自己起延时任务——真正的重连由每 2 秒的检查器在闸门到期后发起，避免双入口。
     */
    private void scheduleReconnect() {
        isReconnecting.set(false);
        if (isDestroyed.get()) {
            return;
        }
        // 推后下次重连闸门，并把退避时长翻倍(封顶 30s)，供下一次失败使用
        mNextReconnectAtMs = SystemClock.elapsedRealtime() + mCurrentBackoffMs;
        long used = mCurrentBackoffMs;
        mCurrentBackoffMs = Math.min(mCurrentBackoffMs * 2, RECONNECT_BACKOFF_MAX_MS);
        Log.i(TAG, "重连退避：本次等待 " + used + "ms，下次退避 " + mCurrentBackoffMs + "ms");
    }

    /** 连接成功后调用：退避时长与闸门全部复位，让下次断连能立即(2s)重连。 */
    private void resetReconnectBackoff() {
        mCurrentBackoffMs = RECONNECT_BACKOFF_MIN_MS;
        mNextReconnectAtMs = 0L;
    }

    /** 检查器据此判断是否已过退避闸门、可以发起重连。 */
    private boolean isBackoffElapsed() {
        return SystemClock.elapsedRealtime() >= mNextReconnectAtMs;
    }

    /** 每次检查都发布一次 WSS 状态给界面（检查器线程调用，界面用 MAIN 线程订阅） */
    private void publishWssStatus(boolean socketOpen) {
        WSSStatusEvent event = new WSSStatusEvent();
        event.connected = socketOpen;
        event.reconnecting = isReconnecting.get();
        event.serverUri = "ws://" + PcrApplication.wssServerIP + ":" + PcrApplication.wssServerPort;
        long waitMs = mNextReconnectAtMs - SystemClock.elapsedRealtime();
        event.nextRetryMs = socketOpen ? 0 : Math.max(0, waitMs);
        EventBus.getDefault().post(event);
    }

    //关闭wss连接
    public void closeWssConnection(){
       try{
           if(null != wssClient){
               // 同 safeCloseWssClient：用 shutdown() 释放内部线程池，避免泄漏
               wssClient.shutdown();
           }
       }catch (Exception e){
            e.printStackTrace();
       }finally {
           wssClient = null;
       }
    }

    /**
     * 对外暴露的 WSS 连接状态查询，供日志上传等模块在断连时暂停工作。
     */
    public boolean isWssConnected() {
        return isWssOpen.get()
                && wssClient != null
                && wssClient.isOpen();
    }

    // ====================== 工具方法 ======================
    private void executeInScheduler(Runnable task) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.execute(task);
        } else {
            Log.e(TAG, "调度器不可用");
            mainHandler.post(task);
        }
    }

    //===========================wss消息收发==============================
    /**
     * 向主控发送WSS消息
     */
    public boolean sendWSSMessage(Object object) {
        if(object == null){
            return false;
        }
        if (wssClient == null
                || wssClient.isClosed()
                || wssClient.isClosing()
                || wssClient.isFlushAndClose()) {
            return false;
        }
        try {
            String sendMsg = GsonUtil.toJson(object,false);
            wssClient.send(sendMsg);
            Log.d(TAG, "消息发送成功: " + object.getClass().getSimpleName());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "消息发送异常: "+ e);
            return false;

        }
    }

    public boolean sendByteWSSMessage(ByteBuffer text){
        if(text == null){
            return false;
        }
        if(wssClient == null
                ||wssClient.isClosed()
                ||wssClient.isClosing()
                ||wssClient.isFlushAndClose()){
            return false;
        }
        try {
            byte[] bytes = text.array();
            wssClient.send(bytes);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "发送二进制消息异常", e);
            return false;
        }
    }

    /**
     * 发送日志消息（二进制）
     * @param fileName 文件名（相对路径，如 "2026-09-07/14-30-15.log"）
     * @param logData 日志数据
     * @param length 数据长度
     * @return 是否发送成功
     */
    public boolean sendLogMessage(String fileName, byte[] logData, int length) {
        if (fileName == null || logData == null || length <= 0) {
            Log.e(TAG, "PCRLog参数无效: fileName=" + fileName + ", length=" + length);
            return false;
        }
        if (wssClient == null || wssClient.isClosed() || wssClient.isClosing() || wssClient.isFlushAndClose()) {
            Log.w(TAG, "PCRLog发送失败: WSS未连接");
            return false;
        }

        try {
            Log.i(TAG, "构造PCRLog消息: " + fileName);

            // 构造 PCRLogMessage
            com.gzlab.autoassemblyline_client.autopipeline.model.message.PCRLogMessage logMsg =
                    new com.gzlab.autoassemblyline_client.autopipeline.model.message.PCRLogMessage();
            int seqId = generateSeqId();
            logMsg.setType("PCRLog");
            logMsg.setSeqID(seqId);  // 注意：方法名是 setSeqID（大写ID）
            logMsg.setDeviceID(PcrApplication.deviceId);
            logMsg.setDeviceType(ConstantData.namePCR);
            logMsg.setPosition(PcrApplication.devicePos);
            logMsg.setFileName(fileName);
            logMsg.setFileSize(length);

            // 复制实际有效的字节
            byte[] actualData = new byte[length];
            System.arraycopy(logData, 0, actualData, 0, length);

            // 计算MD5
            String md5 = com.gzlab.autoassemblyline_client.autopipeline.utils.BinWssMessageUtils.getBytesMD5(actualData);
            logMsg.setCheckSum(md5);
            logMsg.setFileData(actualData);

            // 使用 BinWssMessageUtils 序列化（自动添加魔数、JSON长度等）
            String jsonStr = GsonUtil.toJson(logMsg, false);
            byte[] jsonBytes = jsonStr.getBytes("UTF-8");

            byte[] header = com.gzlab.autoassemblyline_client.autopipeline.utils.BinWssMessageUtils.intToBigEndian(0x5AA5FCFF);
            byte[] headerlen = com.gzlab.autoassemblyline_client.autopipeline.utils.BinWssMessageUtils.intToBigEndian(jsonBytes.length);

            // 拼接 header + headerlen + jsonBytes + fileData
            int totalLen = header.length + headerlen.length + jsonBytes.length + actualData.length;
            ByteBuffer buffer = ByteBuffer.allocate(totalLen);
            buffer.put(header);
            buffer.put(headerlen);
            buffer.put(jsonBytes);
            buffer.put(actualData);
            buffer.flip();

            Log.i(TAG, "PCRLog消息详情: SeqId=" + seqId + ", DeviceID=" + PcrApplication.deviceId +
                    ", Position=" + PcrApplication.devicePos + ", FileSize=" + length + ", CheckSum=" + md5);
            Log.i(TAG, "序列化完成: 总大小=" + totalLen + " 字节 (魔数4B+JSON长度4B+JSON+数据)");

            boolean sent = sendByteWSSMessage(buffer);
            if (sent) {
                Log.i(TAG, "PCRLog消息发送成功: " + fileName + ", SeqId=" + seqId);
            } else {
                Log.e(TAG, "PCRLog消息发送失败: WSS发送异常");
            }
            return sent;
        } catch (Exception e) {
            Log.e(TAG, "PCRLog发送异常: " + fileName, e);
            return false;
        }
    }

    private int seqIdCounter = 0;
    private synchronized int generateSeqId() {
        return ++seqIdCounter;
    }

    // Finish消息重传
    // ====================== Finish 消息重传相关 ======================
    private ByteBuffer mCurrentBuffer;
    private int mCurrentRetry = 0;
    // 标记：是否存在一条【正在等待ACK的Finish任务】
    private final AtomicBoolean isWaitingAckTask = new AtomicBoolean(false);

    private final AtomicBoolean isRetryScheduled = new AtomicBoolean(false);
    private final Handler ackTimeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable ackTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            handleAckTimeout();
        }
    };

    /**
     * 外部入口：发送结束消息并等待ACK
     * 【同一时间只允许运行一个等待ACK任务】
     */
    public boolean sendFinishResultWithAck(ByteBuffer dataBuffer) {
        if(dataBuffer == null){
            return false;
        }
        // 关键：如果已有任务在等待ACK，直接拒绝，禁止重复发起
        if(!isWaitingAckTask.compareAndSet(false,true)){
            Log.w(TAG, "已有Finish消息正在发送"+ isWaitingAckTask.get());
            return false;
        }
        try {
            mCurrentBuffer = dataBuffer;
            mCurrentRetry = 0;
            isRetryScheduled.set(false);
            startSendAndWaitAck();
            return true;
        }catch (Exception e){
            Log.e(TAG, "发送Finish消息异常", e);
            onSendFinalFailed();
            return false;
        }

    }

    private void startSendAndWaitAck() {
        // 每次真正尝试发送前重置调度标志，允许本轮ACK超时后再次触发重发
        isRetryScheduled.set(false);
        boolean sent = sendByteWSSMessage(mCurrentBuffer);
        Log.i(TAG,"发送实验结束的信息和文件=="  + sent);
        if (!sent) {
            // 发送失败走重发流程（不直接递归，避免 WebsocketNotConnectedException 时栈溢出）
            retrySendFinishMsg();
            return;
        }
        //发送成功【不要释放锁】，持续持有直到ACK到达/重试耗尽
        // 启动ACK超时
        startAckTimer();
    }


    private void startAckTimer() {
        stopAckTimer();
        ackTimeoutHandler.postDelayed(ackTimeoutRunnable, FINISH_ACK_TIMEOUT);
        Log.d(TAG, "ACK定时器启动，超时时间: " + FINISH_ACK_TIMEOUT + "ms");
    }

    /**
     * 停止 ACK 超时定时器
     */
    private void stopAckTimer() {
        ackTimeoutHandler.removeCallbacks(ackTimeoutRunnable);
        Log.d(TAG, "ACK定时器取消");
    }

    /**
     * 处理ACK超时(在主线程执行)
     */
    private void handleAckTimeout() {
        // isWaitingAckTask 为 false 表示 ACK 已收到（锁已释放），无需重发
        if (!isWaitingAckTask.get()) {
            Log.d(TAG, "ACK已收到，忽略超时任务");
            return;
        }

        // 防止重复调度重发任务
        if (!isRetryScheduled.compareAndSet(false, true)) {
            Log.d(TAG, "重发任务已调度，忽略重复");
            return;
        }

        Log.w(TAG, "ACK超时，触发重发，当前重试次数: " + mCurrentRetry);
        retrySendFinishMsg();
    }



    private void retrySendFinishMsg() {
        mCurrentRetry++;
        if (mCurrentRetry > MAX_FINISH_RETRY) {
            Log.e(TAG, "超过最大重发次数(" + MAX_FINISH_RETRY + ")，发送失败");
            onSendFinalFailed();
            return;
        }
        Log.i(TAG, "重发Finish消息，第" + mCurrentRetry + "次，延迟" + FINISH_RETRY_DELAY_MS + "ms");
        // 用 postDelayed 打断同步递归：WSS 断连时直接递归会在 MAX_FINISH_RETRY 次内压爆调用栈
        ackTimeoutHandler.postDelayed(this::startSendAndWaitAck, FINISH_RETRY_DELAY_MS);
    }

    private void onSendFinalFailed(){
        Log.e(TAG, "==超过重新发送结束的次数，发送失败=== " );
        mCurrentBuffer = null;
        mCurrentRetry = 0;
        isWaitingAckTask.set(false);
        isRetryScheduled.set(false);
        stopAckTimer();
    }

    //==============================EventBus消息处理==================================
    /**
     * 处理接收到的WSS消息
     * 1.处理接收消息
     * 1.1.查询并设置PCR设备ID和position
     * 1.2,接收心跳消息
     * 1.3.收到PCR硬件上传的设备状态消息
     */
    private final Object wssConnectionItemslock = new Object();
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WSSMessageEvent messageEvent) {
        if(messageEvent == null){
            return;
        }
        synchronized(wssConnectionItemslock) {
            EnumMessageEvent eme = messageEvent.getMsgType();
            Log.d(TAG, "收到消息: " + eme);
            switch (eme) {
                case WSS_CLOSE:
                    handlerDisconnect();
                    break;

                case WSS_Connect:
                    handleWssConnect();
                    break;

                case WSS_Error:
                    handlerError();
                    break;

                case WSS_HeartBeat:
                    //2.心跳消息
                    handleHeartBeat(messageEvent);
                    break;

                case WSS_QueryDevice:
                    Log.i("WSSClient", "===接收查询设备消息==");
                    handleQueryDevice(messageEvent);
                    break;

                case WSS_RESTART_PCR_APP:
                    Log.i(TAG, "===接收主控重启App消息===");
                    handleRestartPCRApp(messageEvent);
                    break;

                case WSS_PCR_STOP:
                    Log.i("pcrStop", "===接收宏石停止PCR消息==");
                    handlerStop();
                    break;

                case WSSManager_PCR_REQ_UPDATE:
                    handlerPcrReq(messageEvent);
                    break;

                case WSS_PCR_Finish_Ack:
                    Log.i(TAG, "===接收实验结束信息的回复ACK===");
                    handleFinishAck(messageEvent);
                    break;

                case WSS_UGRAGE:
                    Log.i(TAG, "===接收主控发送的升级指令===");
                    handleUpgrade(messageEvent);
                    break;

                case WSS_PCR_Ack:
                    handlePcrAck(messageEvent);
                    break;

                case WSS_SLAN_PCR_RESET:
                    handleReset((String) messageEvent.getData());
                    break;

                case WSS_SLAN_PCR_RESET_Finish:
                    if(messageEvent.getArg() == 1){
                        Log.i(TAG, "===PCR复位完成的消息===");
                        sendPCRFinishOk((String) messageEvent.getData());
                    }else{
                        Log.i(TAG, "===PCR复位失败的消息===");
                        sendPCRFinishError((String) messageEvent.getData());
                    }
                    break;
                default:
                    break;
            }

        }
    }

    private void handlerDisconnect(){
        Log.i(TAG, "服务器已经断开,接收到WSS关闭的消息,尝试重新连接");
        isWssOpen.set(false);
        //scheduleReconnect();
    }

    private void handlerError(){
        isWssOpen.set(false);
       // scheduleReconnect();
        Log.i(TAG, "wss连接错误");
    }

    private void handleWssConnect(){
        isWssOpen.set(true);
        isReconnecting.set(false);
        resetReconnectBackoff();   // 连接成功，退避复位
        Log.i(TAG, "接收到WSS连接成功的消息");
    }

    /**
     * 处理发送模板的请求消息
     */
    private void handlerPcrReq(WSSMessageEvent messageEvent){
        SaveReqResult saveReqResult = (SaveReqResult) messageEvent.getData();
        if(null != saveReqResult && saveReqResult.isSaveResult()){
            String saveDirPath = saveReqResult.getSaveDirPath();
            String templateName = saveReqResult.getTemplateName();
            String fileName = saveReqResult.getPcrFileName();
            ArrayList<Well> wells = (ArrayList<Well>)saveReqResult.getSlan_pcrReqMessage().getWells();
            Log.e(TAG, "==成功保存模板===" + " 模板的路径==" + saveDirPath+"==名称==="+ templateName);
            Log.e(TAG, "==成功保存模板===" + " 模板的名称=fileName=" + fileName);
            SLANModuleManager.getInstance().startSLANPCR(saveDirPath, wells, templateName,fileName);
        }else{
            Log.e(TAG, "==保存模板失败==="+ EnumLinuxAlarm.ERROR_Receive_Project_Error.getLinuxAlarm());
            SLANModuleManager.getInstance().setLinuxAlarmAndStatus(EnumLinuxAlarm.ERROR_Receive_Project_Error.getLinuxAlarm(),
                    ConstantData.str_ERROR);
            String templateName = (saveReqResult != null) ? saveReqResult.getTemplateName() : "";
            sendPCRReqAckError(EnumLinuxAlarm.ERROR_Receive_Project_Error.getLinuxAlarm(),templateName);
        }
    }

    private void handleFinishAck(WSSMessageEvent event){
        try {
            Log.e(TAG, "接收到主控接受了PCR文件回复消息receiver_finish_ack");
            PCRFinishAckMessage ack = GsonUtil.fromJson((String) event.getData(), PCRFinishAckMessage.class);
            if("error".equalsIgnoreCase(ack.getReply())){
                Log.w(TAG, "Finish ACK返回错误，重新发送");
                //不再由上层主动重发！统一交给本类定时器一套重试机制，避免双重触发
//                SLANModuleManager.getInstance().reSendFinishMsg();
//                SLANModuleManager.getInstance().setLinuxAlarmAndStatus(EnumLinuxAlarm.str_ERROR.getLinuxAlarm(),ConstantData.str_ERROR);
            }else {
                Log.i(TAG, "Finish ACK接收成功");
                resetFinishState();
                SLANModuleManager.getInstance().setLinuxAlarmAndStatus(EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm(),ConstantData.str_FREE);
            }
        }catch (Exception e){
            Log.e(TAG, "处理Finish ACK异常", e);
        }
    }

    /**
     * 重置 Finish 状态
     */
    private void resetFinishState(){
        mCurrentRetry = 0;
        mCurrentBuffer = null;
        isWaitingAckTask.set(false);
        isRetryScheduled.set(false);
        stopAckTimer();
    }

    /**
     * 处理升级指令消息
     */
    private void handleUpgrade(WSSMessageEvent event){
        int saveUpdateResult = event.getArg();//1:表示保存成功，0表示保存失败
        if(saveUpdateResult != 1){
            Log.e(TAG, "升级版本保存失败");
            sendPCRUpdateAckError(EnumError.ERROR_UPDATE_NOFILEUPDATE);
            return;
        }
        UpgradeMessage upgradeMsg = (UpgradeMessage) event.getData();
        String version = upgradeMsg.getVersion();
        int force = upgradeMsg.getForce();
        //是否强制更新
        if (force == 1) {
            //强制更新
            Log.i(TAG, "===强制客户端升级===");
            performVersionUpdate();
            return;
        } else {
            //比较版本，如果接收的版本高则进行更新
            String localVersion = CommonUtil.getAppVersionName(appContext);
            if (!localVersion.equals(version)) {
                Log.i(TAG, "版本更新: " + localVersion + " -> " + version);
                performVersionUpdate();
            } else {
                Log.i(TAG, "版本相同，无需更新");
                sendPCRUpdateAckError(EnumError.ERROR_UPDATE_VERSION_SAME);
            }
        }
    }

    private void performVersionUpdate(){
        boolean update_suc = SLANModuleManager.getInstance().installUpdateApk();
        if(update_suc){
            sendPCRUpdateFinishOk();
        }else {
            sendPCRUpdateFinishError(EnumError.ERROR_UPDATE_FAIL);
        }
    }

    /**
     * 运行PCR程序的ACK
     */
    private void handlePcrAck(WSSMessageEvent event){
        int arg = event.getArg();
        switch (arg) {
            case 0:
                //运行成功
                sendPCRAckOk((String) event.getData());
                break;
            case 1:
                sendPCRReqAckError(EnumError.ERROR_PCR_Ack_PROJECTNOFIND_ERROR.getError(),(String) event.getData());
                break;
            case 2:
                sendPCRReqAckError(EnumError.ERROR_PCR_Ack_USBNOFIND_ERROR.getError(),(String) event.getData());
                break;
            case 3:
                sendPCRReqAckError(EnumError.ERROR_PCR_Ack_RUNNING_ERROR.getError(),(String) event.getData());
                break;
            case 4:
                sendPCRReqAckError(EnumError.ERROR_PCR_Ack_WELL_ERROR.getError(),(String) event.getData());
                break;
            case 5:
                sendPCRReqAckError(EnumError.ERROR_PCR_Ack_PCR_DISCONNECT.getError(),(String) event.getData());
                break;
        }
    }

    private void handlerStop(){
        SLANModuleManager.getInstance().setLinuxAlarmAndStatus(EnumLinuxAlarm.ERROR_Manual_Interrupt.getLinuxAlarm(),ConstantData.str_ERROR);
        SLANModuleManager.getInstance().stopSLANPcr();
    }

    /**
     * 执行时间同步
     */
    private void performTimeSync(HeartBeatMessage heartBeatMsg) {
        try {
            //第一次收到心跳消息，进行时间同步
            String time = heartBeatMsg.getTime();
            if(time != null && !time.isEmpty()) {
                CommonUtil.setSystemTime(time, appContext);
                timeInit = true;
                Log.e("WSSClient", "第一次收到心跳消息，进行时间同步" + time);
            }
        } catch (Exception e) {
            Log.e(TAG, "时间同步失败", e);
        }
    }

    /**
     * 解决接收心跳消息
     */
    private void handleHeartBeat(WSSMessageEvent event) {
        isWssOpen.set(true);
        String message = (String) event.getData();
        HeartBeatMessage heartBeatMsg = GsonUtil.fromJson(message, HeartBeatMessage.class);
        if (heartBeatMsg != null) {
            int msgSeqId = heartBeatMsg.getSeqId();
            HeartBeatReplyMessage reply = createHeartBeatReply(msgSeqId);
            // 第一次收到心跳消息，进行时间同步
            if (!timeInit) {
                performTimeSync(heartBeatMsg);
            }
            boolean send = sendWSSMessage(reply);
            Log.d(TAG, "==发送心跳回复消息是否成功===" + send);
        }
    }

    private HeartBeatReplyMessage createHeartBeatReply(int msgSeqId) {
        HeartBeatReplyMessage reply = new HeartBeatReplyMessage();
        reply.setType(EnumMessageEvent.WSS_HeartBeat_Reply.getType());
        reply.setDeviceID(PcrApplication.deviceId);
        reply.setPosition(PcrApplication.devicePos);
        reply.setDeviceType(ConstantData.namePCR);
        reply.setSeqId(msgSeqId);
        reply.setVersion(CommonUtil.getAppVersionName(appContext));//获取当前版本
        return reply;
    }

    //=================================处理查询消息=======================
    private void handleQueryDevice(WSSMessageEvent event){
        try {
            String data = (String) event.getData();
            Map<String, Object> jsonMap = GsonUtil.fromJson(data, Map.class);

            if (jsonMap == null || !jsonMap.containsKey("Type")) {
                return;
            }

            String type = (String) jsonMap.get("Type");
            if (!EnumMessageEvent.WSS_QueryDevice.getType().equals(type)) {
                return;
            }

            QueryDeviceMessage queryMsg = GsonUtil.fromJson(data, QueryDeviceMessage.class);
            processQueryDevice(queryMsg);

        } catch (Exception e) {
            Log.e(TAG, "处理查询设备消息异常", e);
        }
    }

    private void processQueryDevice(QueryDeviceMessage queryMsg) {
        String deviceId = queryMsg.getDeviceID();
        String deviceType = queryMsg.getDeviceType();
        QueryDeviceReplyMessage reply = createQueryReply(queryMsg.getSeqId());

        if (ConstantData.PCR_TYPE.equals(deviceType)) {
            // 更新设备信息
            PcrApplication.deviceId = deviceId;
            PcrApplication.devicePos = queryMsg.getPosition();
            PreferenceUtil.getInstance(appContext).setValueByName(SharedConstant.DEVICE_ID, deviceId);
            PreferenceUtil.getInstance(appContext).setValueByName(SharedConstant.DEVICE_POS, queryMsg.getPosition());

            reply.setDeviceID(deviceId);
            reply.setReply("ok");
            Log.i(TAG, "设备查询成功: deviceId=" + deviceId + ", position=" + queryMsg.getPosition());
            EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSSManager_PCR_QUERYDEVICE,queryMsg));
        } else {
            reply.setDeviceID(PcrApplication.deviceId);
            reply.setReply("error");
            Log.w(TAG, "设备查询失败: 不支持的设备类型 " + deviceType);
        }

        sendWSSMessage(reply);
    }

    private QueryDeviceReplyMessage createQueryReply(int seqId) {
        QueryDeviceReplyMessage reply = new QueryDeviceReplyMessage();
        reply.setType(EnumMessageEvent.WSS_QueryDevice_Reply.getType());
        reply.setSeqId(seqId++);
        reply.setDeviceType(ConstantData.namePCR);
        reply.setPosition(PcrApplication.devicePos);
        return reply;
    }

    //=================================处理重启App消息=======================
    //回复发出后等待一段时间再杀进程，保证回复已写出 socket
    private static final long RESTART_REPLY_DELAY_MS = 1000L;
    //强制重启时先停止实验，给 slansdk 停止留出时间
    private static final long RESTART_AFTER_STOP_DELAY_MS = 3000L;
    //杀进程后多久拉起 MainActivity
    private static final long RESTART_LAUNCH_DELAY_MS = 1000L;
    private final AtomicBoolean isRestarting = new AtomicBoolean(false);

    private void handleRestartPCRApp(WSSMessageEvent event) {
        RestartPCRAppMessage restartMsg;
        try {
            restartMsg = GsonUtil.fromJson((String) event.getData(), RestartPCRAppMessage.class);
        } catch (Exception e) {
            Log.e(TAG, "解析重启App消息异常", e);
            return;
        }
        if (restartMsg == null) {
            return;
        }

        RestartPCRAppReplyMessage reply = new RestartPCRAppReplyMessage();
        reply.setType(EnumMessageEvent.WSS_RESTART_PCR_APP_Reply.getType());
        reply.setSeqId(restartMsg.getSeqId());
        reply.setDeviceID(PcrApplication.deviceId);
        reply.setDeviceType(ConstantData.namePCR);
        reply.setPosition(PcrApplication.devicePos);

        if (isRestarting.get()) {
            reply.setReply("error");
            reply.setReason("restarting");
            sendWSSMessage(reply);
            return;
        }

        boolean running = SLANModuleManager.getInstance().isPcrRunning();
        boolean force = restartMsg.getForce() == 1;
        if (running && !force) {
            Log.w(TAG, "实验运行中，拒绝重启App");
            reply.setReply("error");
            reply.setReason("pcr running");
            sendWSSMessage(reply);
            return;
        }

        isRestarting.set(true);
        reply.setReply("ok");
        sendWSSMessage(reply);

        long delay = RESTART_REPLY_DELAY_MS;
        if (running) {
            Log.w(TAG, "强制重启：先停止正在运行的实验");
            handlerStop();
            delay = RESTART_AFTER_STOP_DELAY_MS;
        }
        Log.i(TAG, "===" + delay + "ms 后重启App===");
        mainHandler.postDelayed(() -> CommonUtil.restartApp(appContext, RESTART_LAUNCH_DELAY_MS), delay);
    }

    /**
     * 接收到pcr复位消息
     */
    private void handleReset(String template) {
        // 立即停止
        Log.i("PCR复位", "handleReset");
        try{
            WSSModuleManager.getInstance(appContext).sendPCRAckOk(template);
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
        SLANModuleManager.getInstance().stopSLANPcr();
        // 延迟 3 秒重置
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SLANModuleManager.getInstance().resetSlanPcr(template);
            }
        }, 2000); // 2000ms = 2秒
    }
    //===============================Ack消息发送===========================================

    /**
     * 上报运行失败Ack
     */
    private void sendPCRReqAckError(String alarm, String template) {
        PCRAckMessage msg = createPCRAckMessage("error", alarm, template);
        sendWSSMessage(msg);
        Log.w(TAG, "发送PCR ACK Error: " + alarm);
    }

    /**
     * 上报运行成功的消息
     */
    private void sendPCRAckOk(String template){
        Log.e(TAG,"PCR复位，sendPCRAckOk"  + template);
        PCRAckMessage msg = createPCRAckMessage("ok", "",template);
        sendWSSMessage(msg);
    }

    private PCRAckMessage createPCRAckMessage(String reply, String alarm, String template) {
        PCRAckMessage msg = new PCRAckMessage();
        msg.setType(EnumMessageEvent.WSS_PCR_Ack.getType());
        msg.setTemplate(template);
        msg.setDeviceType(ConstantData.namePCR);
        msg.setDeviceID(PcrApplication.deviceId);
        msg.setPosition(PcrApplication.devicePos);
        msg.setSeqId(seqId.incrementAndGet());
        msg.setReply(reply);
        msg.setAlarm(alarm);
        return msg;
    }

    private void sendPCRFinishOk(String template){
        Log.e("PCR复位",template + "成功，sendPCRFinishOk"  + template);
        PCRFinishMessage msg = createPCRFinishMessage("ok", "",template);
        sendWSSMessage(msg);
    }

    private void sendPCRFinishError(String template){
        Log.e("PCR复位",template + "失败，sendPCRFinishError"  + template);
        PCRFinishMessage msg = createPCRFinishMessage("error", "error",template);
        sendWSSMessage(msg);
    }

    private PCRFinishMessage createPCRFinishMessage(String reply, String alarm, String template) {
        PCRFinishMessage msg = new PCRFinishMessage();
        msg.setType(EnumMessageEvent.WSS_PCR_Finish.getType());
        msg.setTemplate(template);
        msg.setDeviceType(ConstantData.namePCR);
        msg.setDeviceID(PcrApplication.deviceId);
        msg.setPosition(PcrApplication.devicePos);
        msg.setSeqId(seqId.incrementAndGet());
        msg.setAlarm(alarm);
        return msg;
    }


   //===============================升级消息Ack发送==================================================
   private void sendPCRUpdateAckOk() {
        UpgradeAckMessage msg = createUpgradeAckMessage("ok", "");
        sendWSSMessage(msg);
    }

    private void sendPCRUpdateAckError(EnumError error) {
        UpgradeAckMessage msg = createUpgradeAckMessage("error", error.getError());
        sendWSSMessage(msg);
    }

    private UpgradeAckMessage createUpgradeAckMessage(String reply, String error) {
        UpgradeAckMessage msg = new UpgradeAckMessage();
        msg.setType(EnumMessageEvent.WSS_UGRAGE_Ack.getType());
        msg.setDeviceType(ConstantData.namePCR);
        msg.setDeviceID(PcrApplication.deviceId);
        msg.setPosition(PcrApplication.devicePos);
        msg.setSeqId(seqId.incrementAndGet());
        msg.setReply(reply);
        msg.setError(error);
        return msg;
    }

    //================================升级结果消息发送==============================================
    //更新结果
    private void sendPCRUpdateFinishOk() {
        UpgradeFinishMessage msg = createUpgradeFinishMessage("ok", "");
        msg.setType(EnumMessageEvent.WSS_UGRAGE_Finish.getType());
        sendWSSMessage(msg);
    }
    private void sendPCRUpdateFinishError(EnumError error) {
        UpgradeFinishMessage msg = createUpgradeFinishMessage("error", error.getError());
        msg.setType(EnumMessageEvent.WSS_UGRAGE_Finish.getType());
        sendWSSMessage(msg);
    }
    private UpgradeFinishMessage createUpgradeFinishMessage(String reply, String error) {
        UpgradeFinishMessage msg = new UpgradeFinishMessage(reply, error);
        msg.setDeviceType(ConstantData.namePCR);
        msg.setDeviceID(PcrApplication.deviceId);
        msg.setPosition(PcrApplication.devicePos);
        msg.setSeqId(seqId.incrementAndGet());
        return msg;
    }






    //=================================工具方法==============================
    private boolean checkNetworkAvailable() {
        return InternetUtil.isConnect(appContext);
    }

    // ====================== 销毁 ======================
    public void destroy() {
        if (isDestroyed.compareAndSet(false, true)) {
            Log.i(TAG, "开始销毁WSSModuleManager");

            unRegisterEventBus();
            closeWssConnection();

            // 取消定时任务
            cancelScheduledTasks();

            // 关闭线程池
            shutdownThreadPools();

            // 清理Handler
            mainHandler.removeCallbacksAndMessages(null);
            ackTimeoutHandler.removeCallbacksAndMessages(null);

            // 清理资源
            clearResources();

            mInstance = null;
            Log.i(TAG, "WSSModuleManager已销毁");
        }
    }

    private void cancelScheduledTasks() {
        if (reconnectCheckFuture != null) {
            reconnectCheckFuture.cancel(false);
            reconnectCheckFuture = null;
        }
        stopAckTimer();
    }

    private void shutdownThreadPools() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void clearResources() {
        mCurrentBuffer = null;
        messageCache.clear();
    }


    // ====================== Getter/Setter ======================

    public boolean isWssOpen() {
        return isWssOpen.get();
    }

}
