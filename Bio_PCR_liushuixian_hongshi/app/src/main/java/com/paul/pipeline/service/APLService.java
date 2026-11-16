package com.paul.pipeline.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.gidi.bio_pcr.util.LogcatHelper;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.message.LogMessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

public class APLService extends Service {
    final static String TAG = "APLService";
    public APLService() {
    }
    public class APLBinder extends Binder {
        void checkTask(){
            Log.i("ArmTaskWorker", "APLBinder checkTask");
            checkTimer();
        }
        void startTask() {
            initTimer();
            Log.i("ArmTaskWorker", "APLBinder startTask");
        }
        public APLService getService() {
            return APLService.this;
        }

    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("ArmTaskWorker", "Service onBind");
        checkTimer();
       return new APLBinder();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        registerEventBus();
        initPostHandler();
        Log.i("ArmTaskWorker", "Service Created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("ArmTaskWorker", "Service onStartCommand");
        checkTimer();
        return START_STICKY;
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
    Timer timer;
    TimerTask timerTask;
    private void initTimer(){
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 在这里执行你的后台任务
                try{
                    Log.i("ArmTaskWorker", "执行刷新后台任务 refreshConnectinfo checkArmTask");
                    APLManager.getInstance().refreshConnectinfo();
                    APLManager.getInstance().checkArmTask();//安排机械臂任务
                } catch (Exception e) {
                    Log.e("ArmTaskWorker", e.getMessage());
                }
            }
        };
        timer.schedule(timerTask, 1000L, ConstantData.ARMTASK_PERIOD);
    }
    public void checkTimer(){
        try{
            if(timer==null||timerTask==null)//安排机械臂任务
            {
                Log.i("ArmTaskWorker", "定时检查timer init");
                initTimer();
            }else{
                Log.i("ArmTaskWorker", "定时检查timer ok");
            }
        } catch (Exception e) {
            Log.e("ArmTaskWorker", e.getMessage());
        }
    }
    private final Handler handler = new Handler();
    private Runnable runnable;
    private void initPostHandler(){
        runnable = new Runnable() {
            @Override
            public void run() {
                checkTimer();
                handler.postDelayed(this, ConstantData.ARMTASK_PERIOD*6); // 每秒执行一次
            }
        };
        handler.post(runnable);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
        stopTimer();
        if(handler!=null) {
            handler.removeCallbacks(runnable);
        }
        Log.i("ArmTaskWorker", "Service Destroyed");
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
     * 将日志内容安全转为 String：
     * - byte[]  → Base64 字符串（二进制 bin 文件日志）
     * - String  → 原样返回
     * - 其它    → Object.toString()
     */
    private static String toLogString(Object obj) {
        if (obj == null) return "";
        if (obj instanceof byte[]) {
            return android.util.Base64.encodeToString((byte[]) obj, android.util.Base64.DEFAULT);
        }
        return obj.toString();
    }

    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LogMessageEvent messageEvent) {
        if(messageEvent!=null){
            switch(messageEvent.getLogtype()){
                case ConstantData.LOG_PCR:
                    LogcatHelper.getInstance(getApplicationContext())
                            .appendlog(messageEvent.getLogtype(), toLogString(messageEvent.getLogtext()));
                    break;
                case ConstantData.LOG_RNA:
                    LogcatHelper.getInstance(getApplicationContext())
                            .appendlog(messageEvent.getLogtype(), toLogString(messageEvent.getLogtext()));
                    break;
                // LOG_ARM 已删除：ARM 日志改由 RobotWorkManager 直写 logcat，不再单独落盘
            }
        }
    }
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(WSSMessageEvent messageEvent) {
//        EnumMessageEvent eme = messageEvent.getMsgType();
//        switch (eme){
//            case WSS_ServerStart:
//                APLManager.getInstance().WSSStart = true;
//                Log.i(TAG, "WSS_ServerStart:" + (String) messageEvent.getData());
//           //     APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSS_Error:
//                APLManager.getInstance().WSSStart = false;
//                Log.i(TAG, "WSS_Error345:" + (String) messageEvent.getData());
//            //    APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_UpdateConnection:
//                Log.i(TAG, "updateConnection:" + (String) messageEvent.getData());
//           //     APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_UpdateRNAStatus:
//                Log.i(TAG, "WSSManager_UpdateRNAStatus:" + messageEvent.getData());
//           //     APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_UpdateRNARunningStatus:
//                Log.i(TAG, "WSSManager_UpdateRNARunningStatus:" + messageEvent.getData());
//            //    APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_RNAAck_OK:
//                Log.i(TAG, "WSSManager_RNAAck_OK_Message:" + messageEvent.getData());
//           //     APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_RNAAck_Error:
//                Log.i(TAG, "WSSManager_RNAAck_Error_AckMessage:" + messageEvent.getData());
//           //     APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_UpdatePCRStatus:
//                Log.i(TAG, "WSSManager_UpdatePCRStatus:" + messageEvent.getData());
//             //   APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_UpdatePCRRunningStatus:
//                Log.i(TAG, "WSSManager_UpdatePCRRunningStatus:" + messageEvent.getData());
//             //   APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_PCRAck_OK:
//                Log.i(TAG, "WSSManager_PCRAck_OK_Message:" + messageEvent.getData());
//            //    APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_PCRAck_Error:
//                Log.i(TAG, "WSSManager_PCRAck_Error_AckMessage:" + messageEvent.getData());
//            //    APLManager.getInstance().refreshConnectinfo();
//                break;
//            case WSSManager_RNARunningFinish :
//                WSSConnectionItem item = messageEvent.getwSSConnectionItem();
//                Log.i("ArmTaskWorker", "WSSManager_RNARunningFinish:" + messageEvent.getData());
//                if (item.getDeviceType().equalsIgnoreCase(ConstantData.nameRNA)) {
//                    RNAManager.getInstance().updateFinish(item.getPosition(),true);
//                }
//                break;
//            default:
//        //        APLManager.getInstance().refreshConnectinfo();
//                break;
//        }
//    }
//
//    //first rna

//
//
//    /**
//     * 运行结束
//     * @param procedureFinishEvent
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
//        Log.i("User", "arm测试结束" + procedureFinishEvent.getStatus());
//
////        if(null != procedureFinishEvent){
////            // onChronometerStop();
////            if(procedureFinishEvent.getStatus().equals("finish")){
////                PcrApplication.rNARunningMessage.setStatus("free");
////            }else if(procedureFinishEvent.getStatus().equals("finish error")){
////                mRemainTimeTv.setTextColor(getActivity().getColor(R.color.red));
////                mRemainTimeImg.setImageResource(R.drawable.icon_remaining_time_red);
////                mCounterLayout.setBackgroundResource(R.drawable.countdown_stop_bg);
////            }
////        }
//    }
//
//    /**
//     * 超时消息
//     * @param sendCmdTimeoutEvent
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {
//        Log.i("User", "RNA测试超时");
////        if(null != getActivity()){
////            onChronometerStop();
////        }
//    }
//
//
//    /**
//     * 当前的步骤
//     * @param stepIndexMessage
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(StepIndexMessage stepIndexMessage) {
//        if(stepIndexMessage != null){
//        }
//    }
//
//    /**
//     * 当前的步骤
//     * @param hexDebugEntity
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(HexDebugEntity hexDebugEntity) {
////        if(hexDebugEntity != null && getActivity() != null){
////            Log.i("User", "第一次RNA测试正在进行" + hexDebugEntity.getName() + " 步骤描述" + hexDebugEntity.getDescription() );
////        }
//    }
//
//
//    /**
//     * 指令正在执行
//     * @param rNARunningMessage
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(RNARunningMessage rNARunningMessage) {
//        Log.i("User", "第一次RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
//        // String text = gson.toJson(rNARunningMessage);
//        if(rNARunningMessage != null)
//        {
//            /*if(rNARunningMessage.getStatus().contains("free") || rNARunningMessage.getStatus().contains("done")) {
//               .setText(runnning ++ + rNARunningMessage.getStatus()+ ":" + rNARunningMessage.getNO() + rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
//            }else{
//               mRunningMsgTv.setText(runnning ++ + rNARunningMessage.getStatus()+ ":" + rNARunningMessage.getNO() + rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
//            }*/
//        }
//        PcrApplication.rNARunningMessage = rNARunningMessage;
//    }
//
//    /**
//     * 指令完成
//     * @param actionDoneMessage
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(ActionDoneMessage actionDoneMessage) {
////        if(null != actionDoneMessage && getActivity() != null){
////            Log.i("User", "第一次RNA指令完成" + actionDoneMessage.getID() + "状态" + actionDoneMessage.getStatus());
////        }
//    }

    //first rna


}