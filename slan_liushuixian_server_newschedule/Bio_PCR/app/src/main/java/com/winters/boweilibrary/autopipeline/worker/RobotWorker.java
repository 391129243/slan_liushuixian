package com.winters.boweilibrary.autopipeline.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.winters.boweilibrary.autopipeline.constant.Constants.ACTION;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_INPUT_DATA;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_RESULT_DATA;

public class RobotWorker extends Worker {

    private final static String TAG = "RobotWorker";

    /** 同一时刻只允许一个 RobotWorker 驱动机械臂 */
    private static final Semaphore ARM_PERMIT = new Semaphore(1, true);
    /** 获取运行许可的最长等待：覆盖复位抢占时，被停的上一个 worker 等当前步骤结束的时间 */
    private static final long PERMIT_WAIT_MS = 90_000;
    /** 被取消后，等待当前步骤结束（收到 finish 事件）的最长时间 */
    private static final long STOP_DRAIN_TIMEOUT_MS = 60_000;
    /** 模板已空闲但一直没收到属于本次运行的 finish 事件的容忍时间 */
    private static final long IDLE_NO_EVENT_TIMEOUT_MS = 5_000;
    private static final long WAIT_SLICE_MS = 500;

    /** 正在 doWork 中的 RobotWorker 数量（包括已被取消、正在等当前步骤结束的） */
    private static final AtomicInteger RUNNING_COUNT = new AtomicInteger(0);

    public static boolean isAnyRunning() {
        return RUNNING_COUNT.get() > 0;
    }

    private final Object lock = new Object();

    private ProcedureFinishEvent procedureFinishEvent;

    /** 硬件模板是否已由本 worker 启动 */
    private boolean hardwareStarted = false;
    /** 本 worker 启动的模板/运行序号，只接受与之匹配的 finish 事件 */
    private boolean expectReady = false;
    private long expectedRunId = 0;
    private String expectedTemplate = null;
    /** 启动返回前就到达的 finish 事件，绑定后再筛选 */
    private final List<ProcedureFinishEvent> earlyEvents = new ArrayList<>();

    public RobotWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        RUNNING_COUNT.incrementAndGet();
        try {
            Log.i(TAG, "Robot_get running -1 doWork");
            if (!acquirePermit()) {
                if (isStopped()) {
                    Log.i(TAG, "RobotWorker 等待运行许可时已被取消");
                    return failure("error:已取消");
                }
                Log.e(TAG, "RobotWorker 获取机械臂运行许可超时，另一个 RobotWorker 仍在运行");
                return failure("error:机械臂忙");
            }
            try {
                if (isStopped()) {
                    Log.i(TAG, "RobotWorker 已取消，不再启动");
                    return failure("error:已取消");
                }
                return doRobotWork();
            } finally {
                unRegisterEventBus();
                ARM_PERMIT.release();
            }
        } finally {
            RUNNING_COUNT.decrementAndGet();
        }
    }

    /** 分片等待运行许可，期间被取消则立即放弃 */
    private boolean acquirePermit() {
        long deadline = System.currentTimeMillis() + PERMIT_WAIT_MS;
        try {
            while (true) {
                if (ARM_PERMIT.tryAcquire(WAIT_SLICE_MS, TimeUnit.MILLISECONDS)) {
                    return true;
                }
                if (isStopped() || System.currentTimeMillis() >= deadline) {
                    return false;
                }
                Log.i(TAG, "RobotWorker 等待上一个 RobotWorker 结束");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private Result doRobotWork() {

        Log.i(TAG, "start RobotWorker");

        boolean ARMSensor =  SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey("ARM");

        Log.i(TAG, "RobotWorker ARMSensor:" + ARMSensor);

        // 方式二：获取 JSON 字符串并反序列化
        String configJson = getInputData().getString(KEY_INPUT_DATA);
        String action = getInputData().getString(ACTION);
        ItemProperty config = null;
        Log.i(TAG, action +  "----"  +  configJson);
        if(!"reset".equals(action)) {

            if (configJson != null) {
                config = GsonUtil.fromJson(configJson, ItemProperty.class);
            }

            if (config == null) {
                Log.e(TAG, "robot detail failed: " + configJson);
                return failure("error:模板问题");
            }

            Log.i(TAG, "Robot_get running -1 doWork" + config.getType() + config.getPosition());
        }

        boolean result = false;

        registerEventBus();

        SerialPortManager_Robot.clearLastStartedOnThisThread();
        if("get".equals(action)){
            result = SerialPortManager_Robot.getInstance().robotGet(config);
        }else if("put".equals(action)){
            result = SerialPortManager_Robot.getInstance().robotPut(config);
        }else if("reset".equals(action)){
            result = SerialPortManager_Robot.getInstance().robotReset();
        }

        if(!result){
            Log.i(TAG, "robot running: " + configJson);
            return failure("error:运行失败");
        }

        synchronized (lock) {
            hardwareStarted = true;
            expectedRunId = SerialPortManager_Robot.getLastStartedRunIdOnThisThread();
            expectedTemplate = SerialPortManager_Robot.getLastStartedTemplateOnThisThread();
            expectReady = true;
            for (ProcedureFinishEvent e : earlyEvents) {
                if (matches(e)) {
                    procedureFinishEvent = e;
                    break;
                }
            }
            earlyEvents.clear();
            Log.i(TAG, "RobotWorker bind template:" + expectedTemplate + " runId:" + expectedRunId);
        }
        if (isStopped()) {
            // onStopped 可能在 hardwareStarted 置位前触发而未停硬件，这里补一次
            SerialPortManager_Robot.getInstance().robotStopAfterCurrentStep();
        }

        boolean timeout = false;
        synchronized(lock) {
            Log.i(TAG, "lock wait");
            long idleSince = 0;
            long stopSince = 0;
            while (procedureFinishEvent == null) {
                long now = System.currentTimeMillis();
                if (RNAAndPCRAsyncTask.isIdle()) {
                    if (idleSince == 0) idleSince = now;
                    if (now - idleSince >= IDLE_NO_EVENT_TIMEOUT_MS) {
                        Log.e(TAG, "RobotWorker 模板已结束，但未收到本次运行的 finish 事件 runId:" + expectedRunId);
                        timeout = true;
                        break;
                    }
                } else {
                    idleSince = 0;
                }
                if (isStopped()) {
                    if (stopSince == 0) stopSince = now;
                    if (now - stopSince >= STOP_DRAIN_TIMEOUT_MS) {
                        Log.e(TAG, "RobotWorker 已取消，等待当前步骤结束超时 runId:" + expectedRunId);
                        timeout = true;
                        break;
                    }
                }
                try {
                    lock.wait(WAIT_SLICE_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    timeout = true;
                    break;
                }
            }
        }
        Log.i(TAG, "lock unlock");
        unRegisterEventBus();

        if (isStopped()) {
            Log.i(TAG, "RobotWorker 已取消，硬件已停止: " + action + " " + configJson);
            reportHolding(action, config, false, true, "已取消");
            return failure("error:已取消");
        }
        if (timeout || procedureFinishEvent == null) {
            reportHolding(action, config, false, true, "未收到完成事件");
            return failure("error:运行失败:未收到完成事件");
        }

        if("reset".equals(action)){

            if(procedureFinishEvent.getStatus().equals("finish")){
                Log.i(TAG, "RobotWorker finish succeed: " + action);
                Data succeedData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "succeed")
                        .build();
                return Result.success(succeedData);

            }else{
                Log.i(TAG, "RobotWorker finish error: " + action);
                return failure("error:运行失败");
            }
        }

        if(procedureFinishEvent.getStatus().equals("finish")){
            ///加入夹子的是否夹起的判断
            ///等待 位置传感器的反馈
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                reportHolding(action, config, false, false, "等待传感器被中断");
                return failure("error:已取消");
            }

            boolean ARMSensor_new_status =  SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey("ARM");

            boolean obj_new_status =  SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey(config.getType() + config.getPosition());

            Log.i(TAG, "RobotWorker ARMSensor_new_status:" + ARMSensor_new_status);

            Log.i(TAG, "RobotWorker obj_new_status:" + obj_new_status);

            Log.i(TAG, "RobotWorker targetname:" + config.getType() + config.getPosition());

            boolean result_new = true;

            String result_text = "";

            if("get".equals(action)){
                if(ARMSensor_new_status == true && obj_new_status == false){
                    result_new = true;
                }else{
                    result_new= false;
                    result_text = "get " + config.getType() + config.getPosition() + "faliled";
                    Log.i(TAG, "RobotWorker result_text:" + result_text);
                }
            }else if("put".equals(action)){
                if(ARMSensor_new_status == false && obj_new_status == true){
                    result_new = true;
                }else{
                    result_new= false;
                    result_text = "put " + config.getType() + config.getPosition() + "faliled";
                    Log.i(TAG, "RobotWorker result_text:" + result_text);
                }
            }
            // 传感器校验(result_new)：抓取/放置失败判为失败
            reportHolding(action, config, result_new, false, result_new ? "传感器校验通过" : result_text);
            if(result_new) {
                Log.i(TAG, "RobotWorker finish succeed: " + configJson);
                Data succeedData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "succeed")
                        .build();
                return Result.success(succeedData);
            }else{
                Log.i(TAG, "RobotWorker finish error: " + configJson + result_text);
                return failure("error:运行失败:" + result_text);
            }
        }else{
            Log.i(TAG, "finish error: " + configJson);
            reportHolding(action, config, false, true, "模板 finish error");
            return failure("error:运行失败");
        }

    }

    /**
     * 上报取/放结束后机械臂手上的物品（硬件已动作后的每条退出路径都要调用）。
     * 复位不改变手上物品，不上报。
     * @param verified 传感器校验通过
     * @param settle 是否先等 2s 让传感器稳定（校验分支已等过则传 false）
     */
    private void reportHolding(String action, ItemProperty config, boolean verified, boolean settle, String reason) {
        if (config == null || !("get".equals(action) || "put".equals(action))) {
            return;
        }
        RobotWorkManager mgr = RobotWorkManager.getInstance();
        String what = action + " " + config.getType() + config.getPosition() + " " + reason;
        if (verified) {
            if ("get".equals(action)) {
                mgr.reportArmHolding(RobotWorkManager.HOLD_ITEM, config.getType(), config.getPosition(), what);
            } else {
                mgr.reportArmHolding(RobotWorkManager.HOLD_EMPTY, null, 0, what);
            }
            return;
        }
        if (settle) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Boolean arm = SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey("ARM");
        if (arm == null || !arm) {
            mgr.reportArmHolding(RobotWorkManager.HOLD_EMPTY, null, 0, what + " (ARM传感器:无)");
        } else if ("get".equals(action)) {
            // 取失败/中途停止但手上有东西：不能确定抓的是不是目标
            mgr.reportArmHolding(RobotWorkManager.HOLD_UNKNOWN, null, 0, what + " (ARM传感器:有)");
        } else {
            // 放失败/中途停止且手上仍有东西：仍是原来的物品
            mgr.reportArmHolding(RobotWorkManager.HOLD_KEEP, null, 0, what + " (ARM传感器:有)");
        }
    }

    private static Result failure(String text) {
        Data errorData = new Data.Builder()
                .putString(KEY_RESULT_DATA, text)
                .build();
        return Result.failure(errorData);
    }

    /** finish 事件是否属于本 worker 启动的那次运行（调用方需持有 lock） */
    private boolean matches(ProcedureFinishEvent e) {
        if (expectedRunId > 0 && e.getRunId() > 0) {
            return expectedRunId == e.getRunId();
        }
        if (expectedTemplate != null) {
            return expectedTemplate.equals(e.getTemplateName());
        }
        // 未能拿到本次运行信息（非 startAysnTask 启动的路径），保持原行为
        return true;
    }

    /**
     * WorkManager 取消本任务时回调（cancelAllWork / 链上前序失败等）。
     * 停止硬件：当前步骤执行完后不再下发后续步骤；doWork 会等到真正停下后才退出并释放运行许可。
     */
    @Override
    public void onStopped() {
        super.onStopped();
        boolean stopHardware;
        synchronized (lock) {
            stopHardware = hardwareStarted && procedureFinishEvent == null;
            lock.notifyAll();
        }
        Log.i(TAG, "RobotWorker onStopped stopHardware:" + stopHardware + " runId:" + expectedRunId);
        if (stopHardware) {
            SerialPortManager_Robot.getInstance().robotStopAfterCurrentStep();
        }
    }

    protected void registerEventBus(){
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }
    protected void unRegisterEventBus(){
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

    /**
     * 运行结束：只接受本 worker 启动的那次运行的 finish 事件
     * @param result
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventMainThread(ProcedureFinishEvent result) {
        Log.i("RobotWorker", "RobotWorker : " + GsonUtil.toJson(result,false));
        if(null == result){
            return;
        }
        synchronized (lock) {
            if (!expectReady) {
                earlyEvents.add(result);
                return;
            }
            if (procedureFinishEvent != null) {
                return;
            }
            if (!matches(result)) {
                Log.w(TAG, "RobotWorker 忽略不属于本次运行的 finish 事件: " + result.getTemplateName()
                        + " runId:" + result.getRunId() + " 期望:" + expectedTemplate + " runId:" + expectedRunId);
                return;
            }
            procedureFinishEvent = result;
            lock.notifyAll();
            Log.i(TAG, "lock notify");
        }
    }
}
