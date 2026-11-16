package com.winters.boweilibrary.autopipeline.worker;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.Operation;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;

import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.google.gson.reflect.TypeToken;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.message.LogMessageEvent;
import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;
import com.winters.boweilibrary.autopipeline.model.item.QRCodeItem;
import com.winters.boweilibrary.autopipeline.model.message.RobotManagerMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.winters.boweilibrary.autopipeline.constant.Constants;
import com.winters.boweilibrary.autopipeline.model.message.RobotManagerResultMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RobotWorkManager {

    private final static String TAG = "RobotWorkManager";

    public Context mContext;

    private static RobotWorkManager mInstance;

    private String status = "free";

    private String templateName = "";

    private WorkManager workManager;

    private int errorMsgFlag = 0;

    List<OneTimeWorkRequest> queue = new ArrayList<>();

    List<String> queueString = new ArrayList<>();

    // ==== 机械臂单任务入口检查 ====
    /** 最近一次入队的机械臂任务链 id */
    private final List<UUID> activeChainIds = new ArrayList<>();
    private volatile boolean chainActive = false;
    private volatile long chainStartTime = 0;
    /** 入队后 WorkManager 写库前查询不到的宽限时间 */
    private static final long CHAIN_INSERT_GRACE_MS = 3000;

    /** 记录新入队的任务链（observeWorkQueue 中调用，所有入队路径都会经过） */
    private void markChainStarted(List<OneTimeWorkRequest> requests) {
        synchronized (activeChainIds) {
            activeChainIds.clear();
            if (requests != null) {
                for (OneTimeWorkRequest r : requests) {
                    activeChainIds.add(r.getId());
                }
            }
            chainStartTime = System.currentTimeMillis();
            chainActive = !activeChainIds.isEmpty();
        }
    }

    /**
     * 机械臂是否忙：上一个任务链还有未结束的 Work，或仍有 RobotWorker 在运行（含已取消、正在等当前步骤结束的），
     * 或底层模板任务 RNAAndPCRAsyncTask 还没真正结束。
     * @return null 表示空闲，否则为忙的原因
     */
    public String getRobotBusyReason() {
        if (isChainActive()) {
            return "机械臂任务队列未结束";
        }
        if (RobotWorker.isAnyRunning()) {
            return "机械臂动作运行中";
        }
        if (!RNAAndPCRAsyncTask.isIdle()) {
            return "机械臂模板运行中";
        }
        return null;
    }

    public boolean isRobotBusy() {
        return getRobotBusyReason() != null;
    }

    private boolean isChainActive() {
        if (!chainActive || workManager == null) {
            return false;
        }
        List<UUID> ids;
        long startTime;
        synchronized (activeChainIds) {
            ids = new ArrayList<>(activeChainIds);
            startTime = chainStartTime;
        }
        if (ids.isEmpty()) {
            chainActive = false;
            return false;
        }
        try {
            List<WorkInfo> infos = workManager.getWorkInfos(WorkQuery.fromIds(ids)).get(500, TimeUnit.MILLISECONDS);
            if (infos == null || infos.size() < ids.size()) {
                // 刚入队还没写库：按忙处理；超过宽限仍查不到（已被清理）视为结束
                if (System.currentTimeMillis() - startTime < CHAIN_INSERT_GRACE_MS) {
                    return true;
                }
            }
            if (infos != null) {
                for (WorkInfo info : infos) {
                    if (!info.getState().isFinished()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "isChainActive 查询失败，按忙处理: " + e.getMessage());
            return true;
        }
        synchronized (activeChainIds) {
            if (chainStartTime == startTime) {
                chainActive = false;
            }
        }
        return false;
    }

    /** 入口检查：忙则拒绝并记录日志 */
    private boolean rejectIfBusy(String entry) {
        String reason = getRobotBusyReason();
        if (reason != null) {
            Log.e(TAG, entry + " 拒绝：" + reason);
            return true;
        }
        return false;
    }

    public static RobotWorkManager getInstance(){
        if (null == mInstance) {
            synchronized (RobotWorkManager.class){
                if (null == mInstance){
                    mInstance = new RobotWorkManager();
                }
            }
        }
        return  mInstance;
    }


    public void init(Context Context){
        mContext = Context;
        workManager = WorkManager.getInstance(mContext);;
        registerEventBus();
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

    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startRobotWork(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum){

        if(rejectIfBusy("startRobotWork")){
            return false;
        }

        if(itemProperty_source == null || itemProperty_target == null){
            Log.e(TAG, "itemProperty_source or itemProperty_target is null");
            return false;
        }

        if(itemProperty_source.getType().equals("PAN")&&itemProperty_target.getType().equals("PAN")){
            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
            return false;
        }

        if(itemProperty_source.getType().equals("PCR_COVER")||itemProperty_target.getType().equals("PCR_COVER")){
            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
            return false;
        }

        if(itemProperty_source.getType().equals("PCR")&&itemProperty_target.getType().equals("PCR")){
            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
            return false;
        }
        if(itemProperty_source.getType().equals("RNA")&&itemProperty_target.getType().equals("RNA")){
            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
            return false;
        }
//        if(itemProperty_source.getType().contains("RNA")&&itemProperty_target.getType().contains("PCR")){
//            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
//            return false;
//        }
//        if(itemProperty_source.getType().contains("PCR")&&itemProperty_target.getType().contains("RNA")){
//            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
//            return false;
//        }

        if(itemProperty_source.getType().contains("PAN")&&itemProperty_target.getType().contains("RNA")){
            Log.i(TAG, "startGetAndPut_RNA");
            templateName = "startGetAndPut_RNA";
            boolean result = startGetAndPut_RNA(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum);
            return result;
        }

        if(itemProperty_source.getType().contains("RNA")&&itemProperty_target.getType().contains("PAN")){
            Log.i(TAG, "startGetAndPut_RNA");
            templateName = "startGetAndPut_RNA";
            boolean result = startGetAndPut_RNA(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum);
            return result;
        }

        if(itemProperty_source.getType().contains("PAN")&&itemProperty_target.getType().contains("PCR")){
            Log.i(TAG, "startGetAndPut_PCR");
            templateName = "startGetAndPut_PCR";
            boolean result = startGetAndPut_PCR(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum);
            return result;
        }

        if(itemProperty_source.getType().contains("PCR")&&itemProperty_target.getType().contains("PAN")){
            Log.i(TAG, "startGetAndPut_PCR");
            templateName = "startGetAndPut_PCR";
            boolean result = startGetAndPut_PCR(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum);
            return result;
        }

        if(itemProperty_source.getType().contains("RNA")&&itemProperty_target.getType().contains("PCR")){
            Log.i(TAG, "startGet_RNAAndPut_PCR");
            templateName = "startGet_RNAAndPut_PCR";
            boolean result = startGet_RNAAndPut_PCR(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum);
            return result;
        }

        if(itemProperty_source.getType().contains("PCR")&&itemProperty_target.getType().contains("RNA")){
            Log.i(TAG, "startGet_PCRAndPut_RNA");
            templateName = "startGet_PCRAndPut_RNA";
            boolean result = startGet_PCRAndPut_RNA(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum);
            return result;
        }


        return false;
    }

    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean resumeRobotWork(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum, String resumeText ){

        if(rejectIfBusy("resumeRobotWork")){
            return false;
        }

        if(itemProperty_source == null || itemProperty_target == null || resumeText == null){
            Log.e(TAG, "resumeRobotWork 参数为空");
            return false;
        }

        if(itemProperty_source.getType().equals("PAN")&&itemProperty_target.getType().equals("PAN")){
            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
            return false;
        }

        if(itemProperty_source.getType().equals("PCR_COVER")||itemProperty_target.getType().equals("PCR_COVER")){
            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
            return false;
        }

        if(itemProperty_source.getType().equals("PCR")&&itemProperty_target.getType().equals("PCR")){
            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
            return false;
        }
        if(itemProperty_source.getType().equals("RNA")&&itemProperty_target.getType().equals("RNA")){
            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
            return false;
        }
//        if(itemProperty_source.getType().contains("RNA")&&itemProperty_target.getType().contains("PCR")){
//            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
//            return false;
//        }
//        if(itemProperty_source.getType().contains("PCR")&&itemProperty_target.getType().contains("RNA")){
//            Log.e(TAG, "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合");
//            return false;
//        }

        if(itemProperty_source.getType().contains("PAN")&&itemProperty_target.getType().contains("RNA")){
            Log.i(TAG, "startGetAndPut_RNA");
            templateName = "startGetAndPut_RNA";
            boolean result = startGetAndPut_RNA(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum,resumeText);
            return result;
        }

        if(itemProperty_source.getType().contains("RNA")&&itemProperty_target.getType().contains("PAN")){
            Log.i(TAG, "startGetAndPut_RNA");
            templateName = "startGetAndPut_RNA";
            boolean result = startGetAndPut_RNA(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum,resumeText);
            return result;
        }

        if(itemProperty_source.getType().contains("PAN")&&itemProperty_target.getType().contains("PCR")){
            Log.i(TAG, "startGetAndPut_PCR");
            templateName = "startGetAndPut_PCR";
            boolean result = startGetAndPut_PCR(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum,resumeText);
            return result;
        }

        if(itemProperty_source.getType().contains("PCR")&&itemProperty_target.getType().contains("PAN")){
            Log.i(TAG, "startGetAndPut_PCR");
            templateName = "startGetAndPut_PCR";
            boolean result = startGetAndPut_PCR(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum,resumeText);
            return result;
        }

        if(itemProperty_source.getType().contains("RNA")&&itemProperty_target.getType().contains("PCR")){
            Log.i(TAG, "startGet_RNAAndPut_PCR");
            templateName = "startGet_RNAAndPut_PCR";
            boolean result = startGet_RNAAndPut_PCR(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum,resumeText);
            return result;
        }

        if(itemProperty_source.getType().contains("PCR")&&itemProperty_target.getType().contains("RNA")){
            Log.i(TAG, "startGet_PCRAndPut_RNA");
            templateName = "startGet_PCRAndPut_RNA";
            boolean result = startGet_PCRAndPut_RNA(itemProperty_source,itemProperty_target,qRCodeItemsTargrt,panNum,resumeText);
            return result;
        }

        return false;
    }

    //RobotWorkRequest
    public  OneTimeWorkRequest buildRNAWorkRequest(String hexPath, ItemProperty itemProperty, String taskDesc) {
        Data inputData = new Data.Builder()
                .putString(Constants.ACTION, hexPath)
                .putString(Constants.KEY_INPUT_DATA, GsonUtil.toJson(itemProperty,false))
                .putString("TASK_DESC", taskDesc)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RNAWorker.class)
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.SECONDS)
                .build();

        // 自动监听任务状态并监控异常（非lambda写法）
        if (mContext instanceof LifecycleOwner) {
            Log.i(TAG, "observeWork register"  + hexPath + "  " + taskDesc);
            final String desc = taskDesc;
            observeWork(request.getId(), new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if (workInfo != null) {
                        //Log.i(TAG, "RNAWorkRequest 状态: " + workInfo.getState() + ", 任务描述: " + desc);
                        if (workInfo.getState() == WorkInfo.State.FAILED) {
                            Data errorData = workInfo.getOutputData();
                            String errorMsg = errorData.getString(Constants.KEY_RESULT_DATA); // 替换为你的 key
                            Log.e(TAG, "RNAWorkRequest 运行异常: " + workInfo.getState() + ", 任务描述: " + desc);
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, workInfo.getState().name() + ":" + errorMsg));
                        } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            Data errorData = workInfo.getOutputData();
                            String errorMsg = errorData.getString(Constants.KEY_RESULT_DATA); // 替换为你的 key
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, workInfo.getState().name() + ":"+errorMsg));
                            Log.i(TAG, "RNAWorkRequest 成功完成: " + workInfo.getState() + ", 任务描述: " + desc);
                        } else {
                            // 其他状态处理（如 ENQUEUED、RUNNING 等）
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, workInfo.getState().name() + ":"+ ""));
                            Log.i(TAG, "RNAWorkRequest 其他状态: " + workInfo.getState() + ", 任务描述: " + desc);
                        }
                    }
                }
            });
        }else{
            Log.i(TAG, "mContext  is not a instanceof LifecycleOwner");
        }
        return request;

        // 兼容原有无描述调用，需放在带描述参数方法后面
    }

    //RobotWorkRequest
    public OneTimeWorkRequest buildRobotWorkRequest(String action, ItemProperty itemProperty, String taskDesc) {
        Data inputData = new Data.Builder()
                .putString(Constants.ACTION, action)
                .putString(Constants.KEY_INPUT_DATA, GsonUtil.toJson(itemProperty,false))
                .putString("TASK_DESC", taskDesc)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(RobotWorker.class)
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();

        // 自动监听任务状态并监控异常（非lambda写法）
        if (mContext instanceof LifecycleOwner) {
            Log.i(TAG, "observeWork register" + action + "  " + taskDesc);
            final String desc = taskDesc;
            observeWork(request.getId(), new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if (workInfo != null) {
                        //Log.i(TAG, "RobotWorkRequest 状态: " + workInfo.getState() + ", 任务描述: " + desc);
                        if (workInfo.getState() == WorkInfo.State.FAILED) {
                            Data errorData = workInfo.getOutputData();
                            String errorMsg = errorData.getString(Constants.KEY_RESULT_DATA); // 替换为你的 key
                            Log.e(TAG, "RobotWorkRequest 运行异常: " + workInfo.getState() + ", 任务描述: " + desc);
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, errorMsg));
                        } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            Data errorData = workInfo.getOutputData();
                            String errorMsg = errorData.getString(Constants.KEY_RESULT_DATA); // 替换为你的 key
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, errorMsg));
                            Log.i(TAG, "RobotWorkRequest 成功完成: " + workInfo.getState() + ", 任务描述: " + desc);
                        }else {
                            // 其他状态处理（如 ENQUEUED、RUNNING 等）
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, workInfo.getState().name() + ":"+ ""));
                            Log.i(TAG, "RobotWorkRequest 其他状态: " + workInfo.getState() + ", 任务描述: " + desc);
                        }
                    }
                }
            });
        }
        return request;

        // 兼容原有无描述调用，需放在带描述参数方法后面
    }

    //单次任务 enqueueRobotWork，支持多个 Data 参数,input 为 托盘之前通过扫码的样本信息json序列
    public OneTimeWorkRequest buildQRcodeWorkRequest(List<QRCodeItem> qRCodeItems, String taskDesc, int panNum) {
        Data inputData = new Data.Builder()
                .putString(Constants.KEY_INPUT_DATA, GsonUtil.toJson(qRCodeItems,false))
                .putString("TASK_DESC", taskDesc)
                .putInt(Constants.KEY_PANNUM_DATA, panNum)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(QrCodeWorker.class)
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();

        // 自动监听任务状态并监控异常（非lambda写法）
        if (mContext instanceof LifecycleOwner) {
            Log.i(TAG, "observeWork register"  + "QRcodeWork" + "  " + taskDesc);
            final String desc = taskDesc;
            observeWork(request.getId(), new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if (workInfo != null) {
                        //Log.i(TAG, "QRcodeWorkRequest 状态: " + workInfo.getState() + ", 任务描述: " + desc);
                        if (workInfo.getState() == WorkInfo.State.FAILED ) {
                            Data errorData = workInfo.getOutputData();
                            String errorMsg = errorData.getString(Constants.KEY_RESULT_DATA); // 替换为你的 key
                            Log.e(TAG, "QRcodeWorkRequest 运行异常: " + workInfo.getState() + ", 任务描述: " + desc + "--" + errorMsg);
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, errorMsg));
                        } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            Data errorData = workInfo.getOutputData();
                            String errorMsg = errorData.getString(Constants.KEY_RESULT_DATA); // 替换为你的 key
                            String outputData = errorData.getString(Constants.KEY_OUTPUT_DATA); // 替换为你的 key
                            List<QRCodeItem> qRCodeItems_output = GsonUtil.fromJson(outputData,new TypeToken<List<QRCodeItem>>(){});
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, errorMsg));
                            Log.i(TAG, "QRcodeWorkRequest 成功完成: " + workInfo.getState() + ", 任务描述: " + desc);
                            if (null != workQueueListener) {
                                workQueueListener.onQrCodeRevert(qRCodeItems_output,panNum);
                            }
                        }else {
                            // 其他状态处理（如 ENQUEUED、RUNNING 等）
                            EventBus.getDefault().post(new RobotManagerMessage(workInfo.getState(), desc, workInfo.getState().name() + ":"+ ""));
                            Log.i(TAG, "QRcodeWorkRequest 其他状态: " + workInfo.getState() + ", 任务描述: " + desc);
                        }
                    }
                }
            });
        }
        return request;


    }


    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startGetAndPut_PCR(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt, int panNum) {

        try {
            if(itemProperty_source == null || itemProperty_source.getType().contains("RNA")){
                return false;
            }
            if(itemProperty_target == null || itemProperty_target.getType().contains("RNA")){
                return false;
            }

            if(itemProperty_source.getType().contains("PAN") && itemProperty_target.getType().contains("PCR")){


                ItemProperty itemProperty_target_PCR_COVER_target = null;

                ItemProperty itemProperty_target_PCR_COVER_0 = null;

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_target.getPosition()){
                        itemProperty_target_PCR_COVER_target = item;
                    }
                }
                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                        itemProperty_target_PCR_COVER_0 = item;
                    }
                }

                if(itemProperty_target_PCR_COVER_target == null){
                    return false;
                }

                if(itemProperty_target_PCR_COVER_0 == null){
                    return false;
                }



                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();

                //PCR工位取遮光盖盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_target,"get_pcr_cover_from_pcr:" + text);
                //遮光盖放置中转位置
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_0,"put_pcr_cover_to_temp:" + text);
                //料盘取托盘
                OneTimeWorkRequest Request3 =  buildRobotWorkRequest("get",
                        itemProperty_source,"get_pcr_pan_from_pan:" + text);
                //扫码确认
                OneTimeWorkRequest Request4 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);
                /// PCR放托盘
                OneTimeWorkRequest Request5 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_pcr_pan_to_pcr:" + text);
                //中转位置取遮光盖
                OneTimeWorkRequest Request6 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_0,"get_pcr_cover_from_temp:" + text);
                //PCR位置放光盖
                OneTimeWorkRequest Request7 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_target,"put_pcr_cover_to_pcr:" + text);

                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束

                errorMsgFlag = 0;

                // 监听队列
                queue.clear();
                queueString.clear();
                queue.add(Request1);
                queue.add(Request2);
                queue.add(Request3);
                queue.add(Request4);
                queue.add(Request5);
                queue.add(Request6);
                queue.add(Request7);

                queueString.add("get_pcr_cover_from_pcr:" + text);
                queueString.add("put_pcr_cover_to_temp:" + text);
                queueString.add("get_pcr_pan_from_pan:" + text);
                queueString.add("scan_qrcode:" + text);
                queueString.add("put_pcr_pan_to_pcr:" + text);
                queueString.add("get_pcr_cover_from_temp:" + text);
                queueString.add("put_pcr_cover_to_pcr:" + text);



//                queue = java.util.Arrays.asList(Request1, Request2, Request3, Request4, Request5, Request6, Request7);
                observeWorkQueue(queue);

                workManager
                        .beginWith(Request1)
                        .then(Request2)
                        .then(Request3)
                        .then(Request4)
                        .then(Request5)
                        .then(Request6)
                        .then(Request7)
                        .enqueue();
                return true;
            }if(itemProperty_source.getType().contains("PCR") && itemProperty_target.getType().contains("PAN")){

                ItemProperty itemProperty_target_PCR_COVER_source = null;

                ItemProperty itemProperty_target_PCR_COVER_0 = null;

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_source.getPosition()){
                        itemProperty_target_PCR_COVER_source = item;
                    }
                }
                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                        itemProperty_target_PCR_COVER_0 = item;
                    }
                }

                if(itemProperty_target_PCR_COVER_source == null){
                    return false;
                }

                if(itemProperty_target_PCR_COVER_0 == null){
                    return false;
                }

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();

                //PCR工位取遮光盖盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_source,"get_pcr_cover_from_pcr:" + text);
                //遮光盖放置中转位置
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_0,"put_pcr_cover_to_temp:" + text);
                //料盘取托盘
                OneTimeWorkRequest Request3 =  buildRobotWorkRequest("get",
                        itemProperty_source,"get_pcr_pan_from_pcr:" + text);
                //扫码确认
                OneTimeWorkRequest Request4 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);
                /// PCR放托盘
                OneTimeWorkRequest Request5 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_pcr_pan_to_pan:" + text);
                //中转位置取遮光盖
                OneTimeWorkRequest Request6 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_0,"get_pcr_cover_from_temp:" + text);
                //PCR位置放光盖
                OneTimeWorkRequest Request7 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_source,"put_pcr_cover_to_pcr:" + text);
                //修复放盖子问题
                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束

                errorMsgFlag = 0;

                // 监听队列
//                //queue.clear();;
//                queue = java.util.Arrays.asList(Request1, Request2, Request3, Request4, Request5, Request6, Request7);
                queue.clear();;
                queueString.clear();
                queue.add(Request1);
                queue.add(Request2);
                queue.add(Request3);
                queue.add(Request4);
                queue.add(Request5);
                queue.add(Request6);
                queue.add(Request7);

                queueString.add("get_pcr_cover_from_pcr:" + text);
                queueString.add("put_pcr_cover_to_temp:" + text);
                queueString.add("get_pcr_pan_from_pcr:" + text);
                queueString.add("scan_qrcode:" + text);
                queueString.add("put_pcr_pan_to_pan:" + text);
                queueString.add("get_pcr_cover_from_temp:" + text);
                queueString.add("put_pcr_cover_to_pcr:" + text);

                observeWorkQueue(queue);

                workManager
                        .beginWith(Request1)
                        .then(Request2)
                        .then(Request3)
                        .then(Request4)
                        .then(Request5)
                        .then(Request6)
                        .then(Request7)
                        .enqueue();

                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startGetAndPut_PCR : " + e.getMessage());
            return false;
        }
        return false;
    }

    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startGetAndPut_PCR(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt, int panNum,String resumeText) {

        try {
            if(itemProperty_source == null || itemProperty_source.getType().contains("RNA")){
                return false;
            }
            if(itemProperty_target == null || itemProperty_target.getType().contains("RNA")){
                return false;
            }

            if(itemProperty_source.getType().contains("PAN") && itemProperty_target.getType().contains("PCR")){

                ItemProperty itemProperty_target_PCR_COVER_target = null;

                ItemProperty itemProperty_target_PCR_COVER_0 = null;

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_target.getPosition()){
                        itemProperty_target_PCR_COVER_target = item;
                    }
                }
                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                        itemProperty_target_PCR_COVER_0 = item;
                    }
                }

                if(itemProperty_target_PCR_COVER_target == null){
                    return false;
                }

                if(itemProperty_target_PCR_COVER_0 == null){
                    return false;
                }

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();

                //PCR工位取遮光盖盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_target,"get_pcr_cover_from_pcr:" + text);
                //遮光盖放置中转位置
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_0,"put_pcr_cover_to_temp:" + text);
                //料盘取托盘
                OneTimeWorkRequest Request3 =  buildRobotWorkRequest("get",
                        itemProperty_source,"get_pcr_pan_from_pan:" + text);
                //扫码确认
                OneTimeWorkRequest Request4 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);
                /// PCR放托盘
                OneTimeWorkRequest Request5 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_pcr_pan_to_pcr:" + text);
                //中转位置取遮光盖
                OneTimeWorkRequest Request6 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_0,"get_pcr_cover_from_temp:" + text);
                //PCR位置放光盖
                OneTimeWorkRequest Request7 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_target,"put_pcr_cover_to_pcr:" + text);

                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束

                errorMsgFlag = 0;
                // 监听队列
                queue.clear();
                queueString.clear();;
//                queue = java.util.Arrays.asList(Request1, Request2, Request3, Request4, Request5, Request6, Request7);

                if(resumeText.contains("get_pcr_cover_from_pcr")){
                    queue.add(Request1);
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);

                    queueString.add("get_pcr_cover_from_pcr:" + text);
                    queueString.add("put_pcr_cover_to_temp:" + text);
                    queueString.add("get_pcr_pan_from_pan:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);
                    workManager
                        .beginWith(Request1)
                        .then(Request2)
                        .then(Request3)
                        .then(Request4)
                        .then(Request5)
                        .then(Request6)
                        .then(Request7)
                        .enqueue();
                }else if(resumeText.contains("put_pcr_cover_to_temp")){
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);

                    queueString.add("put_pcr_cover_to_temp:" + text);
                    queueString.add("get_pcr_pan_from_pan:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);
                    workManager
                            .beginWith(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_pan_from_pan")){
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("get_pcr_pan_from_pan:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("scan_qrcode")){
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_pan_to_pcr")){
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_cover_from_temp")){
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_cover_to_pcr")){
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request7)
                            .enqueue();
                }
//                observeWorkQueue(queue);

//                workManager.beginWith(queue).enqueue();

//                workManager
//                        .beginWith(Request1)
//                        .then(Request2)
//                        .then(Request3)
//                        .then(Request4)
//                        .then(Request5)
//                        .then(Request6)
//                        .then(Request7)
//                        .enqueue();
                return true;
            }if(itemProperty_source.getType().contains("PCR") && itemProperty_target.getType().contains("PAN")){

                ItemProperty itemProperty_target_PCR_COVER_source = null;

                ItemProperty itemProperty_target_PCR_COVER_0 = null;

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_source.getPosition()){
                        itemProperty_target_PCR_COVER_source = item;
                    }
                }
                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                        itemProperty_target_PCR_COVER_0 = item;
                    }
                }

                if(itemProperty_target_PCR_COVER_source == null){
                    return false;
                }

                if(itemProperty_target_PCR_COVER_0 == null){
                    return false;
                }

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();

                //PCR工位取遮光盖盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_source,"get_pcr_cover_from_pcr:" + text);
                //遮光盖放置中转位置
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_0,"put_pcr_cover_to_temp:" + text);
                //料盘取托盘
                OneTimeWorkRequest Request3 =  buildRobotWorkRequest("get",
                        itemProperty_source,"get_pcr_pan_from_pcr:" + text);
                //扫码确认
                OneTimeWorkRequest Request4 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);
                /// PCR放托盘
                OneTimeWorkRequest Request5 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_pcr_pan_to_pan:" + text);
                //中转位置取遮光盖
                OneTimeWorkRequest Request6 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_0,"get_pcr_cover_from_temp:" + text);
                //PCR位置放光盖
                OneTimeWorkRequest Request7 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_source,"put_pcr_cover_to_pcr:" + text);
                //修复放盖子问题
                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;
                // 监听队列
                queue.clear();
                queueString.clear();
//                queue = java.util.Arrays.asList(Request1, Request2, Request3, Request4, Request5, Request6, Request7);

                if(resumeText.contains("get_pcr_cover_from_pcr")){
                    queue.add(Request1);
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("get_pcr_cover_from_pcr:" + text);
                    queueString.add("put_pcr_cover_to_temp:" + text);
                    queueString.add("get_pcr_pan_from_pcr:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_pcr_pan_to_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request1)
                            .then(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_cover_to_temp")){
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("put_pcr_cover_to_temp:" + text);
                    queueString.add("get_pcr_pan_from_pcr:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_pcr_pan_to_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_pan_from_pcr")){
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("get_pcr_pan_from_pcr:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_pcr_pan_to_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("scan_qrcode")){
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_pcr_pan_to_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_pan_to_pan")){
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("put_pcr_pan_to_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request5)
                            .then(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_cover_from_temp")){
                    queue.add(Request6);
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request6)
                            .then(Request7)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_cover_to_pcr")){
                    queue.add(Request7);
                    observeWorkQueue(queue);

                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    workManager
                            .beginWith(Request7)
                            .enqueue();
                }
                // 已在上面各分支中按顺序链式入队；此处原有的 beginWith(queue) 会把同一批请求再并行入队一次，已移除

//                workManager
//                        .beginWith(Request1)
//                        .then(Request2)
//                        .then(Request3)
//                        .then(Request4)
//                        .then(Request5)
//                        .then(Request6)
//                        .then(Request7)
//                        .enqueue();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startGetAndPut_PCR : " + e.getMessage());
            return false;
        }
        return false;
    }

    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startGetAndPut_RNA(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum) {

        try {
            if(itemProperty_source == null || itemProperty_source.getType().contains("PCR")){
                return false;
            }
            if(itemProperty_target == null || itemProperty_target.getType().contains("PCR")){
                return false;
            }

            if(itemProperty_source.getType().contains("PAN") && itemProperty_target.getType().contains("RNA")){

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();
                //料盘取托盘
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_source,"get_rna_pan_from_pan:" + text);
                //扫码确认
                OneTimeWorkRequest Request2 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,
                        "scan_qrcode:" + text,panNum);
                ///RNA伸出托盘
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "96-opendoor.hex";
                ///RNA收托盘
                String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "98-closedoor.hex";
                OneTimeWorkRequest Request3 =  buildRNAWorkRequest(filePath,
                        itemProperty_target,"open_rna_pan:" + text);
                /// RNA放托盘
                OneTimeWorkRequest Request4 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_rna_pan_to_rna:" + text);
                OneTimeWorkRequest Request5 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_target,"close_rna_pan:" + text);

                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;

                // 监听队列
                ////queue.clear();;
//                queue = java.util.Arrays.asList(Request1, Request2, Request3, Request4, Request5);
                queue.clear();
                queue.add(Request1);
                queue.add(Request2);
                queue.add(Request3);
                queue.add(Request4);
                queue.add(Request5);

                queueString.clear();

                queueString.add("get_rna_pan_from_pan:" + text);
                queueString.add("scan_qrcode:" + text);
                queueString.add("open_rna_pan:" + text);
                queueString.add("put_rna_pan_to_rna:" + text);
                queueString.add("close_rna_pan:" + text);


                observeWorkQueue(queue);

                workManager
                        .beginWith(Request1)
                        .then(Request2)
                        .then(Request3)
                        .then(Request4)
                        .then(Request5)
                        .enqueue();

                return true;
            }if(itemProperty_source.getType().contains("RNA") && itemProperty_target.getType().contains("PAN")){
                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();
                ///RNA伸出托盘
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "96-opendoor.hex";
                ///RNA收托盘
                String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "98-closedoor.hex";
                OneTimeWorkRequest Request1 =  buildRNAWorkRequest(filePath,
                        itemProperty_source,"open_rna_pan:" + text);
                //RNA取托盘
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("get",
                        itemProperty_source,"get_rna_pan_from_rna:" + text);
                //扫码确认
                OneTimeWorkRequest Request3 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);
                OneTimeWorkRequest Request4 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_rna_pan_to_pan:" + text);
                OneTimeWorkRequest Request5 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_source,"close_rna_pan:" + text);

                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;
                // 监听队列
//                //queue.clear();;
//                queue = java.util.Arrays.asList(Request1, Request2, Request3, Request4, Request5);
                queue.clear();
                queue.add(Request1);
                queue.add(Request2);
                queue.add(Request3);
                queue.add(Request4);
                queue.add(Request5);

                queueString.clear();

                queueString.add("open_rna_pan:" + text);
                queueString.add("get_rna_pan_from_rna:" + text);
                queueString.add("scan_qrcode:" + text);
                queueString.add("put_rna_pan_to_pan:" + text);
                queueString.add("close_rna_pan:" + text);

                observeWorkQueue(queue);

                workManager
                        .beginWith(Request1)
                        .then(Request2)
                        .then(Request3)
                        .then(Request4)
                        .then(Request5)
                        .enqueue();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startGetAndPut_PCR : " + e.getMessage());
            return false;
        }
        return false;
    }



    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startGet_RNAAndPut_PCR(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum) {

        try {
            if(itemProperty_source == null
                    || itemProperty_source.getType().contains("PCR")
                    || itemProperty_source.getType().contains("PAN")){
                return false;
            }
            if(itemProperty_target == null
                    || itemProperty_target.getType().contains("RNA")
                    || itemProperty_source.getType().contains("PAN")){
                return false;
            }

            if(itemProperty_source.getType().contains("RNA") && itemProperty_target.getType().contains("PCR")){

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();

                ///RNA伸出托盘
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "96-opendoor.hex";
                ///RNA收托盘
                String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "98-closedoor.hex";

                ItemProperty itemProperty_target_PCR_COVER_source = null;

                ItemProperty itemProperty_target_PCR_COVER_0 = null;

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_target.getPosition()){
                        itemProperty_target_PCR_COVER_source = item;
                    }
                }
                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                        itemProperty_target_PCR_COVER_0 = item;
                    }
                }

                if(itemProperty_target_PCR_COVER_source == null){
                    Log.e(TAG, "startGet_RNAAndPut_PCR  : " + "itemProperty_target_PCR_COVER_source == null");
                    return false;
                }

                if(itemProperty_target_PCR_COVER_0 == null){
                    Log.e(TAG, "startGet_RNAAndPut_PCR : " + "itemProperty_target_PCR_COVER_0 == null");
                    return false;
                }

                //PCR工位取遮光盖盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_source,"get_pcr_cover_from_pcr:" + text);
                //遮光盖放置中转位置
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_0,"put_pcr_cover_to_temp:" + text);

                /// 伸出RNA
                OneTimeWorkRequest Request3 =  buildRNAWorkRequest(filePath,
                        itemProperty_source,"open_rna_pan:" + text);

                //RNA取托盘
                OneTimeWorkRequest Request4=  buildRobotWorkRequest("get",
                        itemProperty_source,"get_pcr_pan_from_rna:" + text);
                //扫码确认
                OneTimeWorkRequest Request5 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);

                OneTimeWorkRequest Request6 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_source,"close_rna_pan:" + text);

                /// PCR放托盘
                OneTimeWorkRequest Request7 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_pcr_pan_to_pcr:" + text);

                //中转位置取遮光盖
                OneTimeWorkRequest Request8 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_0,"get_pcr_cover_from_temp:" + text);
                //PCR位置放光盖
                OneTimeWorkRequest Request9 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_source,"put_pcr_cover_to_pcr:" + text);


                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;


                queue.clear();
                queue.add(Request1);
                queue.add(Request2);
                queue.add(Request3);
                queue.add(Request4);
                queue.add(Request5);
                queue.add(Request6);
                queue.add(Request7);
                queue.add(Request8);
                queue.add(Request9);

                queueString.clear();

                queueString.add("get_pcr_cover_from_pcr:" + text);
                queueString.add("put_pcr_cover_to_temp:" + text);
                queueString.add("open_rna_pan:" + text);
                queueString.add("get_pcr_pan_from_rna:" + text);
                queueString.add("scan_qrcode:" + text);
                queueString.add("close_rna_pan:" + text);
                queueString.add("put_pcr_pan_to_pcr:" + text);
                queueString.add("get_pcr_cover_from_temp:" + text);
                queueString.add("put_pcr_cover_to_pcr:" + text);

                observeWorkQueue(queue);

                Operation enqueue = workManager
                        .beginWith(Request1)
                        .then(Request2)
                        .then(Request3)
                        .then(Request4)
                        .then(Request5)
                        .then(Request6)
                        .then(Request7)
                        .then(Request8)
                        .then(Request9)
                        .enqueue();

                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startGetAndPut_PCR : " + e.getMessage());
            return false;
        }
        return false;
    }
    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startGet_RNAAndPut_PCR(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum,String resumeText) {

        try {
            if(itemProperty_source == null
                    || itemProperty_source.getType().contains("PCR")
                    || itemProperty_source.getType().contains("PAN")){
                return false;
            }
            if(itemProperty_target == null
                    || itemProperty_target.getType().contains("RNA")
                    || itemProperty_source.getType().contains("PAN")){
                return false;
            }

            if(itemProperty_source.getType().contains("RNA") && itemProperty_target.getType().contains("PCR")){

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();

                ///RNA伸出托盘
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "96-opendoor.hex";
                ///RNA收托盘
                String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "98-closedoor.hex";

                ItemProperty itemProperty_target_PCR_COVER_source = null;

                ItemProperty itemProperty_target_PCR_COVER_0 = null;

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_target.getPosition()){
                        itemProperty_target_PCR_COVER_source = item;
                    }
                }
                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                        itemProperty_target_PCR_COVER_0 = item;
                    }
                }

                if(itemProperty_target_PCR_COVER_source == null){
                    Log.e(TAG, "startGet_RNAAndPut_PCR  : " + "itemProperty_target_PCR_COVER_source == null");
                    return false;
                }

                if(itemProperty_target_PCR_COVER_0 == null){
                    Log.e(TAG, "startGet_RNAAndPut_PCR : " + "itemProperty_target_PCR_COVER_0 == null");
                    return false;
                }

                //PCR工位取遮光盖盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_source,"get_pcr_cover_from_pcr:" + text);
                //遮光盖放置中转位置
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_0,"put_pcr_cover_to_temp:" + text);

                /// 伸出RNA
                OneTimeWorkRequest Request3 =  buildRNAWorkRequest(filePath,
                        itemProperty_source,"open_rna_pan:" + text);

                //RNA取托盘
                OneTimeWorkRequest Request4=  buildRobotWorkRequest("get",
                        itemProperty_source,"get_pcr_pan_from_rna:" + text);
                //扫码确认
                OneTimeWorkRequest Request5 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);

                OneTimeWorkRequest Request6 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_source,"close_rna_pan:" + text);

                /// PCR放托盘
                OneTimeWorkRequest Request7 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_pcr_pan_to_pcr:" + text);

                //中转位置取遮光盖
                OneTimeWorkRequest Request8 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_0,"get_pcr_cover_from_temp:" + text);
                //PCR位置放光盖
                OneTimeWorkRequest Request9 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_source,"put_pcr_cover_to_pcr:" + text);

                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;

                queue.clear();

                Log.e(TAG, "startGet_RNAAndPut_PCR resume resumeText : " + resumeText);

//                queue.add(Request1);
//                queue.add(Request2);
//                queue.add(Request3);
//                queue.add(Request4);
//                queue.add(Request5);
//                queue.add(Request6);
//                queue.add(Request7);
//                queue.add(Request8);
//                queue.add(Request9);

                queueString.clear();

//                queueString.add("get_pcr_cover_from_pcr:" + text);
//                queueString.add("put_pcr_cover_to_temp:" + text);
//                queueString.add("open_rna_pan:" + text);
//                queueString.add("get_pcr_pan_from_rna:" + text);
//                queueString.add("scan_qrcode:" + text);
//                queueString.add("close_rna_pan:" + text);
//                queueString.add("put_pcr_pan_to_pcr:" + text);
//                queueString.add("get_pcr_cover_from_temp:" + text);
//                queueString.add("put_pcr_cover_to_pcr:" + text);

//                observeWorkQueue(queue);
//
//                Operation enqueue = workManager
//                        .beginWith(Request1)
//                        .then(Request2)
//                        .then(Request3)
//                        .then(Request4)
//                        .then(Request5)
//                        .then(Request6)
//                        .then(Request7)
//                        .then(Request8)
//                        .then(Request9)
//                        .enqueue();

                if(resumeText.contains("get_pcr_cover_from_pcr")){
                    queue.add(Request1);
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("get_pcr_cover_from_pcr:" + text);
                    queueString.add("put_pcr_cover_to_temp:" + text);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("get_pcr_pan_from_rna:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request1)
                            .then(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_cover_to_temp")){
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("put_pcr_cover_to_temp:" + text);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("get_pcr_pan_from_rna:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("open_rna_pan")){
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("open_rna_pan:" + text);
                    queueString.add("get_pcr_pan_from_rna:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_pan_from_rna")){
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("get_pcr_pan_from_rna:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("scan_qrcode")){
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("scan_qrcode:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("close_rna_pan")){
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("close_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_pan_to_pcr")){
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_cover_from_temp")){
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_pan_to_pcr")){
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_cover_from_temp")){
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_pan_to_pcr")){
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("put_pcr_pan_to_pcr:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_cover_to_pcr")){
                    queue.add(Request9);

                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request9)
                            .enqueue();
                }

                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startGetAndPut_PCR : " + e.getMessage());
            return false;
        }
        return false;
    }

    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startGet_PCRAndPut_RNA(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum) {

        try {
            if(itemProperty_source == null
                    || itemProperty_source.getType().contains("RNA")
                    || itemProperty_source.getType().contains("PAN")){
                return false;
            }
            if(itemProperty_target == null
                    || itemProperty_target.getType().contains("PCR")
                    || itemProperty_source.getType().contains("PAN")){
                return false;
            }

            if(itemProperty_source.getType().contains("PCR") && itemProperty_target.getType().contains("RNA")){

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();

                ///RNA伸出托盘
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "96-opendoor.hex";
                ///RNA收托盘
                String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "98-closedoor.hex";

                ItemProperty itemProperty_source_PCR_COVER = null;

                ItemProperty itemProperty_source_PCR_COVER_0 = null;

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_source.getPosition()){
                        itemProperty_source_PCR_COVER = item;
                    }
                }
                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                        itemProperty_source_PCR_COVER_0 = item;
                    }
                }

                if(itemProperty_source_PCR_COVER == null){
                    Log.e(TAG, "startGet_RNAAndPut_PCR  : " + "itemProperty_source_PCR_COVER == null");
                    return false;
                }

                if(itemProperty_source_PCR_COVER_0 == null){
                    Log.e(TAG, "startGet_RNAAndPut_PCR : " + "itemProperty_target_PCR_COVER_0 == null");
                    return false;
                }

                //PCR工位取遮光盖盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_source_PCR_COVER,"get_pcr_cover_from_pcr:" + text);
                //遮光盖放置中转位置
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("put",
                        itemProperty_source_PCR_COVER_0,"put_pcr_cover_to_temp:" + text);

                //RNA取托盘
                OneTimeWorkRequest Request3=  buildRobotWorkRequest("get",
                        itemProperty_source,"get_pcr_pan_from_PCR:" + text);

                //扫码确认
                OneTimeWorkRequest Request4 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);

                /// 伸出RNA
                OneTimeWorkRequest Request5 =  buildRNAWorkRequest(filePath,
                        itemProperty_target,"open_rna_pan:" + text);

                //RNA取托盘
                OneTimeWorkRequest Request6=  buildRobotWorkRequest("put",
                        itemProperty_target,"put_pcr_pan_to_rna:" + text);

                OneTimeWorkRequest Request7 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_target,"close_rna_pan:" + text);

                //中转位置取遮光盖
                OneTimeWorkRequest Request8 =  buildRobotWorkRequest("get",
                        itemProperty_source_PCR_COVER_0,"get_pcr_cover_from_temp:" + text);
                //PCR位置放光盖
                OneTimeWorkRequest Request9 =  buildRobotWorkRequest("put",
                        itemProperty_source_PCR_COVER,"put_pcr_cover_to_pcr:" + text);


                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;

                queue.clear();
                queue.add(Request1);
                queue.add(Request2);
                queue.add(Request3);
                queue.add(Request4);
                queue.add(Request5);
                queue.add(Request6);
                queue.add(Request7);
                queue.add(Request8);
                queue.add(Request9);

                queueString.clear();

                queueString.add("get_pcr_cover_from_pcr:" + text);
                queueString.add("put_pcr_cover_to_temp:" + text);
                queueString.add("get_pcr_pan_from_PCR:" + text);
                queueString.add("scan_qrcode:" + text);
                queueString.add("open_rna_pan:" + text);
                queueString.add("put_pcr_pan_to_rna:" + text);
                queueString.add("close_rna_pan:" + text);
                queueString.add("get_pcr_cover_from_temp:" + text);
                queueString.add("put_pcr_cover_to_pcr:" + text);

                observeWorkQueue(queue);

                Operation enqueue = workManager
                        .beginWith(Request1)
                        .then(Request2)
                        .then(Request3)
                        .then(Request4)
                        .then(Request5)
                        .then(Request6)
                        .then(Request7)
                        .then(Request8)
                        .then(Request9)
                        .enqueue();

                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startGetAndPut_PCR : " + e.getMessage());
            return false;
        }
        return false;
    }

    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startGet_PCRAndPut_RNA(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum,String resumeText) {

        try {
            if(itemProperty_source == null
                    || itemProperty_source.getType().contains("RNA")
                    || itemProperty_source.getType().contains("PAN")){
                return false;
            }
            if(itemProperty_target == null
                    || itemProperty_target.getType().contains("PCR")
                    || itemProperty_source.getType().contains("PAN")){
                return false;
            }

            if(itemProperty_source.getType().contains("PCR") && itemProperty_target.getType().contains("RNA")){

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();

                ///RNA伸出托盘
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "96-opendoor.hex";
                ///RNA收托盘
                String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "98-closedoor.hex";

                ItemProperty itemProperty_source_PCR_COVER = null;

                ItemProperty itemProperty_source_PCR_COVER_0 = null;

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_source.getPosition()){
                        itemProperty_source_PCR_COVER = item;
                    }
                }
                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                        itemProperty_source_PCR_COVER_0 = item;
                    }
                }

                if(itemProperty_source_PCR_COVER == null){
                    Log.e(TAG, "startGet_RNAAndPut_PCR  : " + "itemProperty_target_PCR_COVER_source == null");
                    return false;
                }

                if(itemProperty_source_PCR_COVER_0 == null){
                    Log.e(TAG, "startGet_RNAAndPut_PCR : " + "itemProperty_target_PCR_COVER_0 == null");
                    return false;
                }

                //PCR工位取遮光盖盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_source_PCR_COVER,"get_pcr_cover_from_pcr:" + text);
                //遮光盖放置中转位置
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("put",
                        itemProperty_source_PCR_COVER_0,"put_pcr_cover_to_temp:" + text);

                //RNA取托盘
                OneTimeWorkRequest Request3=  buildRobotWorkRequest("get",
                        itemProperty_source,"get_pcr_pan_from_PCR:" + text);

                //扫码确认
                OneTimeWorkRequest Request4 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);

                /// 伸出RNA
                OneTimeWorkRequest Request5 =  buildRNAWorkRequest(filePath,
                        itemProperty_target,"open_rna_pan:" + text);

                //RNA取托盘
                OneTimeWorkRequest Request6=  buildRobotWorkRequest("put",
                        itemProperty_target,"put_pcr_pan_to_rna:" + text);

                OneTimeWorkRequest Request7 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_target,"close_rna_pan:" + text);

                //中转位置取遮光盖
                OneTimeWorkRequest Request8 =  buildRobotWorkRequest("get",
                        itemProperty_source_PCR_COVER_0,"get_pcr_cover_from_temp:" + text);
                //PCR位置放光盖
                OneTimeWorkRequest Request9 =  buildRobotWorkRequest("put",
                        itemProperty_source_PCR_COVER,"put_pcr_cover_to_pcr:" + text);


                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;

                queue.clear();
//                queue.add(Request1);
//                queue.add(Request2);
//                queue.add(Request3);
//                queue.add(Request4);
//                queue.add(Request5);
//                queue.add(Request6);
//                queue.add(Request7);
//                queue.add(Request8);
//                queue.add(Request9);

                queueString.clear();

//                queueString.add("get_pcr_cover_from_pcr:" + text);
//                queueString.add("put_pcr_cover_to_temp:" + text);
//                queueString.add("get_pcr_pan_from_PCR:" + text);
//                queueString.add("scan_qrcode:" + text);
//                queueString.add("open_rna_pan:" + text);
//                queueString.add("put_pcr_pan_to_rna:" + text);
//                queueString.add("close_rna_pan:" + text);
//                queueString.add("get_pcr_cover_from_temp:" + text);
//                queueString.add("put_pcr_cover_to_pcr:" + text);

//                observeWorkQueue(queue);
//
//                Operation enqueue = workManager
//                        .beginWith(Request1)
//                        .then(Request2)
//                        .then(Request3)
//                        .then(Request4)
//                        .then(Request5)
//                        .then(Request6)
//                        .then(Request7)
//                        .then(Request8)
//                        .then(Request9)
//                        .enqueue();

                if(resumeText.contains("get_pcr_cover_from_pcr")){
                    queue.add(Request1);
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("get_pcr_cover_from_pcr:" + text);
                    queueString.add("put_pcr_cover_to_temp:" + text);
                    queueString.add("get_pcr_pan_from_PCR:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request1)
                            .then(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_cover_to_temp")){
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("put_pcr_cover_to_temp:" + text);
                    queueString.add("get_pcr_pan_from_PCR:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_pan_from_PCR")){
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("get_pcr_pan_from_PCR:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request3)
                            .then(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("scan_qrcode")){
                    queue.add(Request4);
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("scan_qrcode:" + text);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request4)
                            .then(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("open_rna_pan")){
                    queue.add(Request5);
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("open_rna_pan:" + text);
                    queueString.add("put_pcr_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request5)
                            .then(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_pan_to_rna")){
                    queue.add(Request6);
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("put_pcr_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request6)
                            .then(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("close_rna_pan")){
                    queue.add(Request7);
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("close_rna_pan:" + text);
                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request7)
                            .then(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("get_pcr_cover_from_temp")){
                    queue.add(Request8);
                    queue.add(Request9);

                    queueString.add("get_pcr_cover_from_temp:" + text);
                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request8)
                            .then(Request9)
                            .enqueue();
                }else if(resumeText.contains("put_pcr_cover_to_pcr")){
                    queue.add(Request9);

                    queueString.add("put_pcr_cover_to_pcr:" + text);

                    observeWorkQueue(queue);

                    Operation enqueue = workManager
                            .beginWith(Request9)
                            .enqueue();
                }

                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startGetAndPut_PCR : " + e.getMessage());
            return false;
        }
        return false;
    }


    // 单次任务 enqueueRobotWork，支持多个 Data 参数
    public boolean startGetAndPut_RNA(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum,String resumeText) {

        try {
            if(itemProperty_source == null || itemProperty_source.getType().contains("PCR")){
                return false;
            }
            if(itemProperty_target == null || itemProperty_target.getType().contains("PCR")){
                return false;
            }

            if(itemProperty_source.getType().contains("PAN") && itemProperty_target.getType().contains("RNA")){

                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();
                //料盘取托盘
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_source,"get_rna_pan_from_pan:" + text);
                //扫码确认
                OneTimeWorkRequest Request2 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,
                        "scan_qrcode:" + text,panNum);
                ///RNA伸出托盘
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "96-opendoor.hex";
                ///RNA收托盘
                String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "98-closedoor.hex";
                OneTimeWorkRequest Request3 =  buildRNAWorkRequest(filePath,
                        itemProperty_target,"open_rna_pan:" + text);
                /// RNA放托盘
                OneTimeWorkRequest Request4 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_rna_pan_to_rna:" + text);
                OneTimeWorkRequest Request5 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_target,"close_rna_pan:" + text);

                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;

                // 监听队列
                queue.clear();
                queueString.clear();

                if(resumeText.contains("get_rna_pan_from_pan")){
                    queue.add(Request1);
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("get_rna_pan_from_pan:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("put_rna_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);

                    workManager
                            .beginWith(Request1)
                            .then(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .enqueue();
                }else  if(resumeText.contains("scan_qrcode")){
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("put_rna_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                            .beginWith(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .enqueue();
                }else  if(resumeText.contains("open_rna_pan")){
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("put_rna_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                            .beginWith(Request3)
                            .then(Request4)
                            .then(Request5)
                            .enqueue();
                }else  if(resumeText.contains("put_rna_pan_to_rna")){
                    queue.add(Request4);
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("put_rna_pan_to_rna:" + text);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                            .beginWith(Request4)
                            .then(Request5)
                            .enqueue();
                }else  if(resumeText.contains("close_rna_pan")){
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                            .beginWith(Request5)
                            .enqueue();
                }

//                observeWorkQueue(queue);
//
//                workManager.beginWith(queue).enqueue();

                return true;
            }if(itemProperty_source.getType().contains("RNA") && itemProperty_target.getType().contains("PAN")){
                String text = itemProperty_source.getType() + itemProperty_source.getPosition() + ":" +
                        itemProperty_target.getType() + itemProperty_target.getPosition();
                ///RNA伸出托盘
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "96-opendoor.hex";
                ///RNA收托盘
                String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "98-closedoor.hex";
                OneTimeWorkRequest Request1 =  buildRNAWorkRequest(filePath,
                        itemProperty_source,"open_rna_pan:" + text);
                //RNA取托盘
                OneTimeWorkRequest Request2 =  buildRobotWorkRequest("get",
                        itemProperty_source,"get_rna_pan_from_rna:" + text);
                //扫码确认
                OneTimeWorkRequest Request3 =  buildQRcodeWorkRequest(qRCodeItemsTargrt,"scan_qrcode:" + text,panNum);
                OneTimeWorkRequest Request4 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_rna_pan_to_pan:" + text);
                OneTimeWorkRequest Request5 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_source,"close_rna_pan:" + text);

                // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
                errorMsgFlag = 0;

                // 监听队列
                //queue.clear();;
//                queue = java.util.Arrays.asList(Request1, Request2, Request3, Request4, Request5);
                queue.clear();
//                queue.add(Request1);
//                queue.add(Request2);
//                queue.add(Request3);
//                queue.add(Request4);
//                queue.add(Request5);

                queueString.clear();

//                workManager
//                        .beginWith(Request1)
//                        .then(Request2)
//                        .then(Request3)
//                        .then(Request4)
//                        .then(Request5)
//                        .enqueue();

                if(resumeText.contains("open_rna_pan")){
                    queue.add(Request1);
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("open_rna_pan:" + text);
                    queueString.add("get_rna_pan_from_rna:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_rna_pan_to_pan:" + text);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                        .beginWith(Request1)
                        .then(Request2)
                        .then(Request3)
                        .then(Request4)
                        .then(Request5)
                        .enqueue();
                }else  if(resumeText.contains("get_rna_pan_from_rna")){
                    queue.add(Request2);
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("get_rna_pan_from_rna:" + text);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_rna_pan_to_pan:" + text);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                            .beginWith(Request2)
                            .then(Request3)
                            .then(Request4)
                            .then(Request5)
                            .enqueue();
                }else  if(resumeText.contains("scan_qrcode")){
                    queue.add(Request3);
                    queue.add(Request4);
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("scan_qrcode:" + text);
                    queueString.add("put_rna_pan_to_pan:" + text);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                            .beginWith(Request3)
                            .then(Request4)
                            .then(Request5)
                            .enqueue();
                }else  if(resumeText.contains("put_rna_pan_to_pan")){
                    queue.add(Request4);
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("put_rna_pan_to_pan:" + text);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                            .beginWith(Request4)
                            .then(Request5)
                            .enqueue();
                }else  if(resumeText.contains("close_rna_pan")){
                    queue.add(Request5);
                    observeWorkQueue(queue);
                    queueString.add("close_rna_pan:" + text);
                    workManager
                            .beginWith(Request5)
                            .enqueue();
                }




                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "startGetAndPut_PCR : " + e.getMessage());
            return false;
        }
        return false;
    }

    /**
     * 机械臂异常复位：唯一允许抢占的动作。
     * 先请求当前模板在正在执行的步骤完成后停止，再取消队列；
     * 被取消的 RobotWorker 会等硬件真正停下才释放运行许可，复位 worker 拿到许可后才会下发复位模板。
     * @return
     */
    public boolean startSingleReset() {
        try {
            Log.i(TAG, "startSingleAcion : " + "startSingleReset" );
            templateName = "reset_robot";
            SerialPortManager_Robot.getInstance().robotStopAfterCurrentStep();
            queue.clear();
            queueString.clear();
            workManager.cancelAllWork();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "startSingleReset : " + e.getMessage());
            }
            OneTimeWorkRequest Request1 =  buildRobotWorkRequest("reset",
                    null,"reset_robot:" + "reset_robot");
            queue.add(Request1);
            queueString.add("reset_robot:" + "reset_robot");
            observeWorkQueue(queue);
            Operation enqueue = workManager.beginWith(Request1).enqueue();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "startSingleReset : " + e.getMessage());
            return false;
        }

    }
    // 单次动作，single action 用于异常放回的特殊情况
    public boolean startSingleAcion(ItemProperty itemProperty_source,ItemProperty itemProperty_target, String action) {

        try {
            if(rejectIfBusy("startSingleAcion " + action)){
                return false;
            }

            if(itemProperty_target == null){
                Log.e(TAG, "startSingleAcion : " + "itemProperty_target" + " is null");
                return false;
            }

            if(action == null || action.isEmpty()){
                Log.e(TAG, "startSingleAcion : " + "action" + " is null or empty");
                return false;
            }

            ///RNA伸出托盘
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/PCR/commonHex_v5/" + "96-opendoor.hex";
            ///RNA收托盘
            String filePath_closedoor = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/PCR/commonHex_v5/" + "98-closedoor.hex";

            ItemProperty itemProperty_target_PCR_COVER_target = null;

            ItemProperty itemProperty_target_PCR_COVER_0 = null;


            for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                if(item.getType().contains("PCR_COVER") && item.getPosition() == 0){
                    itemProperty_target_PCR_COVER_0 = item;
                }
            }



            if(itemProperty_target_PCR_COVER_0 == null){
                Log.e(TAG, "startSingleAcion : " + "itemProperty_target_PCR_COVER_0 == null");
                return false;
            }

            // 不再用 cancelAllWork() 抢占：入口已保证上一个机械臂任务已结束
            queue.clear();
            queueString.clear();

            if(itemProperty_source != null && itemProperty_source.getType().contains("PCR_COVER")
                    && itemProperty_target != null && itemProperty_target.getType().contains("PCR_COVER")
                    && action.contains("put_pcr_cover_from_temp_to_pcr")){

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_target.getPosition()){
                        itemProperty_target_PCR_COVER_target = item;
                    }
                }

                if(itemProperty_target_PCR_COVER_target == null){
                    Log.e(TAG, "startSingleAcion  : " + "itemProperty_target_PCR_COVER_source == null"  + action);
                    return false;
                }

                //中转位置取遮光盖
                OneTimeWorkRequest Request1 =  buildRobotWorkRequest("get",
                        itemProperty_target_PCR_COVER_0,"get_pcr_cover_from_temp:" + action);

                //PCR位置放光盖
                OneTimeWorkRequest Request9 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_target,"put_pcr_cover_to_pcr:" + action);

                queue.add(Request1);
                queue.add(Request9);
                queueString.add("get_pcr_cover_from_temp:" + action);
                queueString.add("put_pcr_cover_to_pcr:" + action);
                observeWorkQueue(queue);
                Operation enqueue = workManager
                        .beginWith(Request1)
                        .then(Request9)
                        .enqueue();

            }else if(itemProperty_target != null && itemProperty_target.getType().contains("PCR_COVER")
                    && action.contains("put_pcr_cover_to_pcr")){

                for(ItemProperty item : SerialPortManager_Robot.itemPropertyList){
                    if(item.getType().contains("PCR_COVER") && item.getPosition() == itemProperty_target.getPosition()){
                        itemProperty_target_PCR_COVER_target = item;
                    }
                }

                if(itemProperty_target_PCR_COVER_target == null){
                    Log.e(TAG, "startSingleAcion  : " + "itemProperty_target_PCR_COVER_target == null"  + action);
                    return false;
                }

                //PCR位置放光盖
                OneTimeWorkRequest Request9 =  buildRobotWorkRequest("put",
                        itemProperty_target_PCR_COVER_target,"put_pcr_cover_to_pcr:" + action);

                queue.add(Request9);
                queueString.add("put_pcr_cover_to_pcr:" + action);
                observeWorkQueue(queue);
                Operation enqueue = workManager
                        .beginWith(Request9)
                        .enqueue();

            }else if(action.contains("close_rna_pan")
                    && itemProperty_target.getType().contains("RNA")){
                OneTimeWorkRequest Request6 =  buildRNAWorkRequest(filePath_closedoor,
                        itemProperty_target,"close_rna_pan:" + action);
                queue.add(Request6);
                queueString.add("close_rna_pan:" + action);
                observeWorkQueue(queue);
                Operation enqueue = workManager
                        .beginWith(Request6)
                        .enqueue();
            }else if(action.contains("open_rna_pan")
                    && itemProperty_target.getType().contains("RNA")){
                OneTimeWorkRequest Request6 =  buildRNAWorkRequest(filePath,
                        itemProperty_target,"open_rna_pan:" + action);
                queue.add(Request6);
                queueString.add("open_rna_pan:" + action);
                observeWorkQueue(queue);
                Operation enqueue = workManager
                        .beginWith(Request6)
                        .enqueue();
            }else if(action.contains("put_to_pan")
                    && itemProperty_target.getType().contains("PAN")){
                OneTimeWorkRequest Request4 =  buildRobotWorkRequest("put",
                        itemProperty_target,"put_to_pan:" + action);
                queue.add(Request4);
                queueString.add("put_to_pan:" + action);
                observeWorkQueue(queue);
                Operation enqueue = workManager
                        .beginWith(Request4)
                        .enqueue();
            }else{
                Log.e(TAG, "startSingleAcion : " + "error cmd:" + action);
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "startSingleAcion : " + "error cmd:" + action);
            Log.e(TAG, "startSingleAcion : " + "error cmd:" + e.getMessage());
            return false;
        }
    }

    // 单次任务 enqueueRobotWork，支持多个 Data 参数,input 为 托盘之前通过扫码的样本信息json序列
    public void enqueueQRcodeWork(String input) {
        Data inputData = new Data.Builder()
                .putString(Constants.KEY_INPUT_DATA, input)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(QrCodeWorker.class)
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();

        workManager.enqueue(request);
    }

    public void cancelAllWork(){
        workManager.cancelAllWork();

    }

    // 监听任务状态
    public void observeWork(UUID workId, Observer<WorkInfo> observer) {
        workManager.getWorkInfoByIdLiveData(workId).observe((LifecycleOwner) mContext, observer);
    }

    /**
     * 指令正在执行
     * @param robotManagerMessage
     */
    @Subscribe(threadMode = ThreadMode.POSTING)
    public void onEventMainThread(RobotManagerMessage robotManagerMessage) {

        if (robotManagerMessage != null) {
            String text = GsonUtil.toJson(robotManagerMessage, false);
            if(text!=null){
                /////存储log - ARM 日志已取消，因为 logcat 已包含相同内容
                // EventBus.getDefault().post(new LogMessageEvent(ConstantData.LOG_ARM, text, EnumMessageEvent.WSS_Message, null, robotManagerMessage));
            }
        }
    }

    // 队列监听接口
    public interface WorkQueueListener {
        /** @param chainTag 启动该任务链时 setPendingChainTag 设置的标记，0 表示调用方未设置 */
        void onAllTasksCompleted(String templateName, long chainTag);
        /** @param failedStep 失败的那一步（queueString 文字）；被取消（CANCELLED）时为 null */
        void onTaskFailed(String workId, long chainTag, String failedStep);
        void onQrCodeRevert(List<QRCodeItem> qRCodeItems, int panNum);
    }

    /**
     * 调用方在调用 start/resume 前设置，observeWorkQueue 把它绑定到新入队的任务链上，
     * 链结束时随 onAllTasksCompleted/onTaskFailed 回传，用于找回"启动这条链的那个回调"。
     * 调用方在 start/resume 返回后应调用 clearPendingChainTag()。
     */
    private long pendingChainTag = 0;

    public void setPendingChainTag(long tag) {
        pendingChainTag = tag;
    }

    public void clearPendingChainTag() {
        pendingChainTag = 0;
    }

    // ==== 机械臂手上物品上报（由 RobotWorker 在每次取/放结束后上报） ====
    /** 取成功（传感器校验通过）：手上 = 该物品 */
    public static final int HOLD_ITEM = 1;
    /** 放成功（传感器校验通过）：手上为空 */
    public static final int HOLD_EMPTY = 2;
    /** 取中途失败/取消且手上有东西：无法确定手上是什么 */
    public static final int HOLD_UNKNOWN = 3;
    /** 放失败/取消且手上仍有东西：仍是原来的物品（原来未知则仍未知） */
    public static final int HOLD_KEEP = 4;

    public interface ArmHoldingListener {
        /**
         * @param kind HOLD_ITEM / HOLD_EMPTY / HOLD_UNKNOWN / HOLD_KEEP
         * @param type 物品类型（PAN/RNA/PCR/PCR_COVER），仅 HOLD_ITEM 有效
         * @param position 物品位置，仅 HOLD_ITEM 有效
         * @param reason 日志用
         */
        void onArmHoldingChanged(int kind, String type, int position, String reason);
    }

    private volatile ArmHoldingListener armHoldingListener;
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public void setArmHoldingListener(ArmHoldingListener listener) {
        this.armHoldingListener = listener;
    }

    /** RobotWorker 调用：切到主线程通知（先于该 Work 状态变化的 LiveData 回调到达） */
    void reportArmHolding(final int kind, final String type, final int position, final String reason) {
        Log.i(TAG, "reportArmHolding kind:" + kind + " " + type + position + " " + reason);
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                ArmHoldingListener l = armHoldingListener;
                if (l != null) {
                    l.onArmHoldingChanged(kind, type, position, reason);
                }
            }
        });
    }

    private WorkQueueListener workQueueListener;

    public void setWorkQueueListener(WorkQueueListener listener) {
        this.workQueueListener = listener;
    }

    // 监听一组任务队列
    // 成员变量
    private LiveData<List<WorkInfo>> lastWorkInfosLiveData;
    private Observer<List<WorkInfo>> lastWorkInfosObserver;
    private void observeWorkQueue(List<OneTimeWorkRequest> requests) {
        markChainStarted(requests);
        // 每条链固定自己的标记和模板名，不受之后其他链改写共享字段影响
        final long chainTag = pendingChainTag;
        final String chainTemplate = templateName;
        if (mContext instanceof LifecycleOwner && requests != null && !requests.isEmpty()) {
            List<UUID> ids = new java.util.ArrayList<>();
            for (OneTimeWorkRequest req : requests) {
                ids.add(req.getId());
            }
            // 先移除上一次的监听
            if (lastWorkInfosLiveData != null && lastWorkInfosObserver != null) {
                lastWorkInfosLiveData.removeObserver(lastWorkInfosObserver);
            }
            // 新的监听
            lastWorkInfosLiveData = workManager.getWorkInfosLiveData(WorkQuery.fromIds(ids));
            lastWorkInfosObserver = new Observer<List<WorkInfo>>() {
                @Override
                public void onChanged(List<WorkInfo> workInfos) {
                    String lastAction = "";
                    String lastDetail = "";
                    String failedStep = null;
                    boolean allFinished = false;
                    Log.e(TAG, "RobotWorkManager workInfos size:" + workInfos.size());
                    if(workInfos.size() == 0){
                        return;
                    }
                    int i = 0;

                    for (int j = 0; j < requests.size(); j++) {
                        boolean breakFlag = false;
                        for (WorkInfo info : workInfos) {
                            if(requests.get(j).getId().toString().contains(info.getId().toString())){
                                if (info.getState() == WorkInfo.State.FAILED || info.getState() == WorkInfo.State.CANCELLED) {
                                    String text = queueString.get(j);
                                    EventBus.getDefault().post(new RobotManagerResultMessage(info.getState(), text, info.getState().name()));
                                    lastAction = text;
                                    lastDetail = info.getState().name();
                                    if (info.getState() == WorkInfo.State.FAILED) {
                                        failedStep = text;
                                    }
                                    Log.e(TAG, "RobotWorkManager RobotManagerResultMessage 任务描述 finish error or cancelled " + text);
                                    breakFlag = true;
                                    break;

                                }
                            }
                        }
                        if(breakFlag){
                            break;
                        }
                    }

                    for (WorkInfo info : workInfos) {
                        if (info.getState() == WorkInfo.State.FAILED|| info.getState() == WorkInfo.State.CANCELLED) {
                            Log.e(TAG, "RobotWorkManager queuelist finish error or cancelled " +
                                    info.getId().toString() +
                                    " state:" + info.getState().name());
                            if (lastWorkInfosLiveData != null && lastWorkInfosObserver != null) {
                                lastWorkInfosLiveData.removeObserver(lastWorkInfosObserver);
                            }
                            if (workQueueListener != null) {
//                                workQueueListener.onTaskFailed(lastAction + "--" + lastDetail);
                                workQueueListener.onTaskFailed(chainTemplate, chainTag, failedStep);
                                allFinished = false;

                            }
                            return;
                        }
                    }

                    for (WorkInfo info : workInfos) {
                        if(info.getState() == WorkInfo.State.SUCCEEDED){
                            Log.e(TAG, "RobotWorkManager queuelist last workId: " +
                                    info.getId().toString() +
                                    " state:" + info.getState().name());
                            if(i == workInfos.size() - 1){
                                allFinished = true;
                            }
                        }else{
                            allFinished = false;
                            break;
                        }
                        i++;
                    }
                    if (allFinished) {
                        Log.e(TAG, "RobotWorkManager queuelist finish succeed begin");
                        for (WorkInfo info : workInfos) {
                                Log.e(TAG, "RobotWorkManager queuelist finish succeed workId: " +
                                        info.getId().toString() +
                                        " state:" + info.getState().name());
                        }
                        Log.e(TAG, "RobotWorkManager queuelist finish succeed finish");
                        if (lastWorkInfosLiveData != null && lastWorkInfosObserver != null) {
                            lastWorkInfosLiveData.removeObserver(lastWorkInfosObserver);
                        }
                    }


                    if (allFinished && workQueueListener != null) {
                        Log.e(TAG, "RobotWorkManager queuelist finish succeed onAllTasksCompleted");
                        workQueueListener.onAllTasksCompleted(chainTemplate, chainTag);
                    }
                }
            };

            lastWorkInfosLiveData.observe((LifecycleOwner) mContext, lastWorkInfosObserver);
        }
    }

}
