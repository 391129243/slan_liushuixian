package com.paul.pipeline.manager;

import android.util.Log;

import com.gidi.bio_pcr.util.ModuleEnableManager;
import com.paul.pipeline.data.DeviceIdMap;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;

import java.util.Arrays;
import java.util.Calendar;

/**
 * 定期重启 SLANPCR client，避免 slansdk 内存泄漏把内存耗尽。
 * <p>
 * 由 {@link com.paul.pipeline.service.APLService} 的调度循环（5 秒一次）调用
 * {@link #tick()}，不做自己的定时器。规则：
 * <ul>
 *   <li>每天一次，过了 {@link #RESTART_HOUR} 点（默认 0 点）才进入当天的重启窗口；</li>
 *   <li>断连（disconnect）和禁用（disable）的模块不重启；</li>
 *   <li>模块正在被调度使用（工位上有盘、或 armStep 不是 FREE，即下一轮调度循环
 *       可能下发模板）时不重启，等本次调度结束、托盘离开工位后再重启；</li>
 *   <li>同一天同一模块只重启一次，跨天自动重新开放。</li>
 * </ul>
 */
public class PCRClientRestartScheduler {
    private static final String TAG = "PCRRestart";

    /** 每天进入重启窗口的小时（0-23），默认凌晨 0 点。 */
    private static final int RESTART_HOUR = 0;

    private static PCRClientRestartScheduler instance;

    /** 每个工位上一次已处理（下发重启或服务端启动当天）的日期，见 {@link #dayKey}。 */
    private final int[] restartedOnDay = new int[DeviceIdMap.PCR_DEVICE_IDS.length];

    public static PCRClientRestartScheduler getInstance() {
        if (instance == null) {
            synchronized (PCRClientRestartScheduler.class) {
                if (instance == null) {
                    instance = new PCRClientRestartScheduler();
                }
            }
        }
        return instance;
    }

    private PCRClientRestartScheduler() {
        // 服务端启动当天视为已处理：否则白天重启服务端后第一轮调度就会把空闲 client 全部重启
        Arrays.fill(restartedOnDay, dayKey(Calendar.getInstance()));
    }

    /** 年+日，跨年不会和去年同一天冲突 */
    private static int dayKey(Calendar c) {
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR);
    }

    /** 不重启的原因，null 表示已下发重启指令 */
    private static final String SKIP_DISCONNECTED = "未连接";
    private static final String SKIP_DISABLED = "已禁用";
    private static final String SKIP_BUSY = "调度占用中";

    /**
     * 调度循环每轮调用一次（APLService 定时器线程）。窗口未到时直接返回，不遍历模块。
     */
    public synchronized void tick() {
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.HOUR_OF_DAY) < RESTART_HOUR) {
            return;
        }
        int today = dayKey(now);
        String[] deviceIds = DeviceIdMap.PCR_DEVICE_IDS;
        for (int i = 0; i < deviceIds.length; i++) {
            if (restartedOnDay[i] == today) {
                continue;
            }
            if (tryRestart(i + 1, deviceIds[i]) == null) {
                restartedOnDay[i] = today;
            }
        }
    }

    /**
     * 手动重启全部 PCR client（设置页-模组配置按钮）。
     * 与定时重启同一套规则：断连、禁用、调度占用中的模块跳过，不会打断正在跑的模板。
     * 已重启的模块记为当天已处理，避免当晚定时任务再重启一次。
     * @return 每个工位的结果，用于界面提示
     */
    public synchronized String restartAllNow() {
        int today = dayKey(Calendar.getInstance());
        StringBuilder sb = new StringBuilder();
        String[] deviceIds = DeviceIdMap.PCR_DEVICE_IDS;
        for (int i = 0; i < deviceIds.length; i++) {
            String reason = tryRestart(i + 1, deviceIds[i]);
            if (reason == null) {
                restartedOnDay[i] = today;
            }
            sb.append("PCR").append(i + 1).append("(").append(deviceIds[i]).append(")：")
                    .append(reason == null ? "已下发重启" : "跳过，" + reason).append("\n");
        }
        Log.i(TAG, "手动重启全部 PCR client:\n" + sb);
        return sb.toString().trim();
    }

    /**
     * 尝试重启一个模块。
     * @return null 表示已下发重启指令；否则为跳过原因（定时任务下轮再试）
     */
    private String tryRestart(int position, String deviceId) {
        WSSConnectionItem item = WSSModuleManager.getInstance().getItemByDeviceID(deviceId);
        // 断连：没有连接项，或连接已关闭
        if (item == null || item.getWebSocketConnection() == null
                || item.getWebSocketConnection().isClosed()
                || item.getWebSocketConnection().isClosing()) {
            return SKIP_DISCONNECTED;
        }
        // 禁用：按 deviceId 判断，与调度层 getFreeModel 的口径一致
        if (!ModuleEnableManager.getInstance().isModuleEnabled(deviceId)) {
            return SKIP_DISABLED;
        }
        // 调度还要用它：工位上有盘，或 armStep 不是 FREE（下一轮可能下发模板）
        if (PCRManager.getInstance().isPcrBusyForSchedule(position)) {
            Log.i(TAG, "PCR" + position + "(" + deviceId + ") 调度占用中，推迟重启");
            return SKIP_BUSY;
        }
        // Force=0：client 端实验仍在运行时会拒绝重启
        String result = WSSModuleManager.getInstance().sendRestartPCRAppWSSMessage(deviceId, 0);
        if (result == null || !result.startsWith("succeed")) {
            Log.e(TAG, "PCR" + position + "(" + deviceId + ") 重启指令发送失败: " + result);
            return "发送失败";
        }
        Log.i(TAG, "PCR" + position + "(" + deviceId + ") 已下发重启指令");
        return null;
    }
}
