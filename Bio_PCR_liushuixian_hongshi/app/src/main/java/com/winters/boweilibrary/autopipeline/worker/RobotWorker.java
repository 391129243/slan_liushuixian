package com.winters.boweilibrary.autopipeline.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static com.winters.boweilibrary.autopipeline.constant.Constants.ACTION;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_INPUT_DATA;
import static com.winters.boweilibrary.autopipeline.constant.Constants.KEY_RESULT_DATA;

public class RobotWorker extends Worker {

    private final static String TAG = "RobotWorker";


    private Object lock = new Object();

    private ProcedureFinishEvent procedureFinishEvent;

    public RobotWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.i(TAG, "Robot_get running -1 doWork");

        Log.i(TAG, "start RobotWorker");

        boolean ARMSensor =  SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey("ARM");

        Log.i(TAG, "RobotWorker ARMSensor:" + ARMSensor);

        // 方式二：获取 JSON 字符串并反序列化
        String configJson = getInputData().getString(KEY_INPUT_DATA);
        String action = getInputData().getString(ACTION);
        ItemProperty config = null;
        Log.i(TAG, action +  "----"  +  configJson);
        if(!action.equals("reset")) {

            if (configJson != null) {
                config = GsonUtil.fromJson(configJson, ItemProperty.class);
            } else {
                Log.e(TAG, "robot detail failed: " + configJson);
                Data errorData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "error:模板问题")
                        .build();
                return Result.failure(errorData);
            }

            Log.i(TAG, "Robot_get running -1 doWork" + config.getType() + config.getPosition());

            if (config == null) {
                Log.e(TAG, "robot detail failed: " + configJson);
                Data errorData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "error:模板问题")
                        .build();
                return Result.failure(errorData);
            }
        }

        boolean result = false;

        registerEventBus();

//        try {
//            Thread.sleep(200);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        if(action.equals("get")){
            result = SerialPortManager_Robot.getInstance().robotGet(config);
        }else if(action.equals("put")){
            result = SerialPortManager_Robot.getInstance().robotPut(config);
        }else if(action.equals("reset")){
            result = SerialPortManager_Robot.getInstance().robotReset();
        }

        if(!result){
            Log.i(TAG, "robot running: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:运行失败")
                    .build();
            unRegisterEventBus();
            return Result.failure(errorData);

        }

        synchronized(lock) {
            try {
                Log.i(TAG, "lock wait");
                lock.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Log.i(TAG, "lock unlock");
        unRegisterEventBus();

        // 使用 WorkManager 的运行尝试计数，允许最多两次 retry（总共最多执行3次）
        int runAttempt = getRunAttemptCount();

        if(action.equals("reset")){

            if(procedureFinishEvent.getStatus().equals("finish")){
                Log.i(TAG, "RobotWorker finish succeed: " + action);
                Data succeedData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "succeed")
                        .build();
                return Result.success(succeedData);

            }else{
                Log.i(TAG, "RobotWorker finish error: " + action);
                Data errorData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "error:运行失败")
                        .build();
                return Result.failure(errorData);
            }
        }

        if(procedureFinishEvent.getStatus().equals("finish")){
            ///加入夹子的是否夹起的判断
            ///等待位置传感器的反馈，带超时和轮询机制
            boolean result_new = false;
            String result_text = "";
            int maxAttempts = 8;  // 4秒 / 500ms = 8次
            int attemptCount = 0;
            long startTime = System.currentTimeMillis();

            Log.i(TAG, "RobotWorker 开始传感器状态轮询，超时时间: 4秒, target:" + config.getType() + config.getPosition());

            while (attemptCount < maxAttempts && !result_new) {
                try {
                    Thread.sleep(500);  // 每次等待 500ms
                    attemptCount++;

                    boolean ARMSensor_new_status = SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey("ARM");
                    boolean obj_new_status = SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey(config.getType() + config.getPosition());

                    Log.i(TAG, "RobotWorker 第" + attemptCount + "次轮询 - ARMSensor:" + ARMSensor_new_status + ", obj_status:" + obj_new_status);

                    if(action.equals("get")){
                        // get 操作：ARM传感器应该有物体(true)，目标位置应该为空(false)
                        if(ARMSensor_new_status == true && obj_new_status == false){
                            result_new = true;
                            long elapsedTime = System.currentTimeMillis() - startTime;
                            Log.i(TAG, "RobotWorker get 操作成功，耗时: " + elapsedTime + "ms");
                        }
                    } else if(action.equals("put")){
                        // put 操作：ARM传感器应该为空(false)，目标位置应该有物体(true)
                        if(ARMSensor_new_status == false && obj_new_status == true){
                            result_new = true;
                            long elapsedTime = System.currentTimeMillis() - startTime;
                            Log.i(TAG, "RobotWorker put 操作成功，耗时: " + elapsedTime + "ms");
                        }
                    }

                } catch (InterruptedException e) {
                    Log.e(TAG, "RobotWorker 传感器轮询被中断", e);
                    throw new RuntimeException(e);
                }
            }

            // 轮询结束后的结果判断
            if (!result_new) {
                // 超时后，获取最终状态进行判断
                boolean ARMSensor_final = SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey("ARM");
                boolean obj_final = SerialPortManager_Robot.getInstance().getRobotStatus().getSenseByKey(config.getType() + config.getPosition());

                if(action.equals("get")){
                    result_text = "get " + config.getType() + config.getPosition() + " failed - 超时(4秒), ARM传感器:" + ARMSensor_final + ", 目标传感器:" + obj_final;
                } else if(action.equals("put")){
                    result_text = "put " + config.getType() + config.getPosition() + " failed - 超时(4秒), ARM传感器:" + ARMSensor_final + ", 目标传感器:" + obj_final;
                }
                Log.e(TAG, "RobotWorker " + result_text);
            }

            if(result_new) {
                Log.i(TAG, "RobotWorker finish succeed: " + configJson);
                Data succeedData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "succeed")
                        .build();
                return Result.success(succeedData);
            }else{
                Log.i(TAG, "RobotWorker finish error: " + configJson + result_text);
                Data errorData = new Data.Builder()
                        .putString(KEY_RESULT_DATA, "error:运行失败:" + result_text)
                        .build();
                return Result.failure(errorData);
            }
        }else{
            Log.i(TAG, "finish error: " + configJson);
            Data errorData = new Data.Builder()
                    .putString(KEY_RESULT_DATA, "error:运行失败")
                    .build();
            return Result.failure(errorData);
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
     * 运行结束
     * @param result
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventMainThread(ProcedureFinishEvent result) throws InterruptedException {
        Log.i("RobotWorker", "RobotWorker : " + GsonUtil.toJson(result,false));
        if(null != result){
            procedureFinishEvent = result;
//            Thread.sleep(200);
            synchronized (lock) {
                try {
                    lock.notify();
                    Log.i(TAG, "lock notify");
                } catch (Exception e) {
                    Log.i(TAG, "error:" + e.getMessage());
                }
            }
        }
    }


}



