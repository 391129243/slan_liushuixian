package com.winters.boweilibrary.autopipeline.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexSingleEntity;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.event.WSSMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAReqMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.winters.boweilibrary.autopipeline.constant.Constants.ACTION;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_INPUT_DATA;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_RESULT_DATA;

public class RNAWorker extends Worker {

    private final static String TAG = "RNAWorker";

    private final static int timeout = 120 * 1000; // 120秒超时

    // 等待模块（设备状态 + Linux 状态）都变 free 的最长时间
    private final static long FREE_WAIT_TIMEOUT = 120 * 1000; // 120秒
    // 轮询模块状态的间隔
    private final static long FREE_WAIT_POLL_INTERVAL = 1000; // 1秒

    private Object lock = new Object();

    private RNAFinishMessage rNAFinishMessage;

    private String type;

    private int position;

    public RNAWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // 方式二：获取 JSON 字符串并反序列化
        String configJson = getInputData().getString(KEY_INPUT_DATA);
        String action = getInputData().getString(ACTION);
        ItemProperty config = null;
        File actionFile = null;

        // 使用 WorkManager 的运行尝试计数，允许最多两次 retry（总共最多执行3次）
        int runAttempt = getRunAttemptCount();

        if (configJson != null) {
            config = GsonUtil.fromJson(configJson, ItemProperty.class);
            Log.e(TAG, "RNAWorker detail: " + configJson);
        } else {
            Log.e(TAG, "RNAWorker target detail failed: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:" + configJson + "目标问题")
                    .build();
            return Result.failure(errorData);
        }

        if (action != null) {
            Log.e(TAG, "RNAWorker action detail: " + action);
            actionFile=  new File(action);
        } else {
            Log.e(TAG, "RNAWorker action detail failed: " + action);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:" + action + "模板问题")
                    .build();
            return Result.failure(errorData);
        }

        if (config == null) {
            Log.e(TAG, "RNAWorker target detail failed: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:" + configJson + "目标问题")
                    .build();
            return Result.failure(errorData);
        }

        if(!actionFile.exists()){
            Log.e(TAG, "RNAWorker action detail failed: " + action);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:" + action + "模板问题")
                    .build();
            return Result.failure(errorData);
        }

        RNAReqMessage rNAReqMessage = new RNAReqMessage();
        rNAReqMessage.setSeqId(111);
        rNAReqMessage.setBatchID(124+"");
        rNAReqMessage.setTotalTime(20);
        rNAReqMessage.setType("RNAReq");
        rNAReqMessage.setTemplate(actionFile.getName());
        rNAReqMessage.setCurrentPhase(0);
        rNAReqMessage.setCurrentPhaseStep(0);

        List<HexDebugEntity> HexDebugEntityList =
                ListDataSaveUtil.getHexDebugEntityList(actionFile);

        if(actionFile.getName().contains("first")||actionFile.getName().contains("second")) {
            String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "97-downandopen.hex";
            File downandopenfile = new File(downandopen_filePath);

            List<HexDebugEntity> HexDebugEntityList_downandopen =
                    ListDataSaveUtil.getHexDebugEntityList(downandopenfile);

            HexDebugEntityList.addAll(HexDebugEntityList_downandopen);
        }

        rNAReqMessage.setWorkTemplate(HexDebugEntityList);

        type = config.getType();

        position = config.getPosition();

        WSSConnectionItem selectWSSConnectionItem = WSSModuleManager.getInstance().getItemByTypeAndPosition(type,position);

        if(selectWSSConnectionItem == null){
            // 设备不在连接列表里（离线）：重试也连不上，立即失败，不占住机械臂任务队列
            Log.i(TAG, "RNAWorker  selectWSSConnectionItem: null :" +  config.getType() + "-" + config.getPosition()
                    + " 设备未连接，不重试");
            Log.i(TAG, "finish error: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:" + config.getType() + config.getPosition() + " 未连接")
                    .build();
            return Result.failure(errorData);
        }

        String selectWSSConnectionItemKey = selectWSSConnectionItem.getDeviceID();

        String result = WSSModuleManager.getInstance().sendRNATemplateWSSMessage(selectWSSConnectionItemKey,rNAReqMessage);

        if (result.contains("error")) {
            Log.i(TAG, "RNAWorker  failed: " + result);
            Log.i(TAG, "finish error: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:" + GsonUtil.toJson(rNAFinishMessage,false))
                    .build();
            if (runAttempt < 4) {
                Log.i(TAG, "will retry after 10s, currentRunAttempt=" + runAttempt);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "sleep interrupted before retry: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
                return Result.retry();
            } else {
                Log.i(TAG, "exceeded retry limit, failing. currentRunAttempt=" + runAttempt);
                return Result.failure(errorData);
            }
        }

        registerEventBus();

        synchronized (lock) {
            try {
                lock.wait(timeout); // 最长等待120秒
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        unRegisterEventBus();

        if (rNAFinishMessage == null) {
            // 超时未收到任何 Finish 消息
            Log.i(TAG, "finish timeout, no RNAFinishMessage: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error: RNAWorker wait finish timeout")
                    .build();
            if (runAttempt < 4) {
                Log.i(TAG, "will retry after 10s, currentRunAttempt=" + runAttempt);
                sleepQuietly(10000);
                return Result.retry();
            } else {
                Log.i(TAG, "exceeded retry limit, failing. currentRunAttempt=" + runAttempt);
                return Result.failure(errorData);
            }
        }

        if (rNAFinishMessage.getStatus().equals("ok")) {
            // Finish 仅代表模板执行完毕，模块可能仍在复位/重启（见 onRNAFinish 的 FinishAck 可能触发重启）。
            // 必须等到对应模块的设备状态和 Linux 状态都为 free，才算该步骤真正完成，
            // 否则机械臂关门后立刻启动下一个 RNA 实验会与未结束的流程冲突导致失败。
            boolean moduleFree = waitUntilModuleFree(selectWSSConnectionItemKey);
            if (!moduleFree) {
                Log.i(TAG, "finish ok but module not free in time: " + configJson);
                Data errorData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "error: RNAWorker module not free: "
                                + GsonUtil.toJson(rNAFinishMessage, false))
                        .build();
                if (runAttempt < 4) {
                    Log.i(TAG, "will retry after 10s, currentRunAttempt=" + runAttempt);
                    sleepQuietly(10000);
                    return Result.retry();
                } else {
                    Log.i(TAG, "exceeded retry limit, failing. currentRunAttempt=" + runAttempt);
                    return Result.failure(errorData);
                }
            }
            Log.i(TAG, "finish succeed: " + configJson);
            Data succeedData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "ok:" + GsonUtil.toJson(rNAFinishMessage,false))
                    .build();
            Log.i(TAG, "Robot_get running -2 RNAWorker finish");
            return Result.success(succeedData);
        } else {
            Log.i(TAG, "finish error: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:" + GsonUtil.toJson(rNAFinishMessage,false))
                    .build();
            if (runAttempt < 4) {
                Log.i(TAG, "will retry after 10s, currentRunAttempt=" + runAttempt);
                sleepQuietly(10000);
                return Result.retry();
            } else {
                Log.i(TAG, "exceeded retry limit, failing. currentRunAttempt=" + runAttempt);
                return Result.failure(errorData);
            }
        }

    }

    /**
     * 等待模块真正空闲：设备状态(rNAStatus)和 Linux 状态(rNALinuxStatus)都为 free。
     * 轮询直到两者都 free，或达到超时时间。
     *
     * @param deviceKey WSS 连接 key（deviceID）
     * @return true 表示模块已完全空闲；false 表示超时仍未空闲或出现 error
     */
    private boolean waitUntilModuleFree(String deviceKey) {
        final long deadline = System.currentTimeMillis() + FREE_WAIT_TIMEOUT;
        while (System.currentTimeMillis() < deadline) {
            WSSConnectionItem conn = WSSModuleManager.getInstance().getItemByDeviceID(deviceKey);
            if (conn instanceof RNAStatusItem) {
                RNAStatusItem item = (RNAStatusItem) conn;
                String deviceStatus = item.getrNAStatus();
                String linuxStatus = item.getrNALinuxStatus();

                // 出现 error 状态直接判定失败，无需继续等待
                if ("error".equals(deviceStatus) || "error".equals(linuxStatus)) {
                    Log.e(TAG, "waitUntilModuleFree: " + deviceKey + " error. deviceStatus="
                            + deviceStatus + " linuxStatus=" + linuxStatus);
                    return false;
                }

                if ("free".equals(deviceStatus) && "free".equals(linuxStatus)) {
                    Log.i(TAG, "waitUntilModuleFree: " + deviceKey + " is free");
                    return true;
                }
                Log.i(TAG, "waitUntilModuleFree: " + deviceKey + " waiting. deviceStatus="
                        + deviceStatus + " linuxStatus=" + linuxStatus);
            } else {
                Log.e(TAG, "waitUntilModuleFree: " + deviceKey + " not a RNAStatusItem");
            }
            sleepQuietly(FREE_WAIT_POLL_INTERVAL);
        }
        Log.e(TAG, "waitUntilModuleFree: " + deviceKey + " timeout");
        return false;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Log.e(TAG, "sleep interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }


    protected void registerEventBus() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    protected void unRegisterEventBus() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
    }

    /**
     * 运行结束
     *
     * @param messageEvent
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventMainThread(WSSMessageEvent messageEvent) {
        if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_RNARunningFinish)){
            WSSConnectionItem item = messageEvent.getwSSConnectionItem();
            // 修复：原代码 if(...position == position); 多了一个分号，导致判断失效，
            // 任意 RNA 模块的 Finish 都会唤醒本 worker。此处恢复为真正的类型+位置匹配。
            if (item.getDeviceType().equals(type) && item.getPosition() == position) {
                Log.i(TAG, "WSSManager_RNARunningFinish:" +
                        GsonUtil.toJson(messageEvent.getData(),false));
                rNAFinishMessage = (RNAFinishMessage) messageEvent.getData();
                synchronized (lock) {
                    try {
                        lock.notify();
                    } catch (Exception e) {
                        Log.i(TAG, "error:" + e.getMessage());
                    }
                }
            }
        }
    }

}