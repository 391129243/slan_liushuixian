package com.gzlab.autoassemblyline_client.autopipeline.mgr;


import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gzlab.autoassemblyline_client.PcrApplication;
import com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Enum.SampleType;
import com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.PatientInfo;
import com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Peak;
import com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.WellResult;
import com.gzlab.autoassemblyline_client.autopipeline.data.ConstantData;
import com.gzlab.autoassemblyline_client.autopipeline.enumtype.EnumAlarm;
import com.gzlab.autoassemblyline_client.autopipeline.enumtype.EnumLinuxAlarm;
import com.gzlab.autoassemblyline_client.autopipeline.event.EnumMessageEvent;
import com.gzlab.autoassemblyline_client.autopipeline.event.WSSMessageEvent;
import com.gzlab.autoassemblyline_client.autopipeline.message.PCRCurveMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.PCRRunningStatusMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.PCRStatusMessage;
import com.gzlab.autoassemblyline_client.autopipeline.message.SLAN_PCRFinishMessage;
import com.gzlab.autoassemblyline_client.autopipeline.utils.BinWssMessageUtils;
import com.gzlab.autoassemblyline_client.bean.ExTemperature;
import com.gzlab.autoassemblyline_client.event.PCRTemplateInfoUpdateEvent;
import com.gzlab.autoassemblyline_client.util.DateFormatUtil;
import com.gzlab.autoassemblyline_client.util.FileUtils;
import com.gzlab.autoassemblyline_client.util.StringUtil;
import com.gzlab.autoassemblyline_client.util.UsbPermissionUtils;
import com.hongshi.slansdk.common.HSUtils;
import com.hongshi.slansdk.listener.DeviceStateListener;
import com.hongshi.slansdk.listener.RunningInfoListener;
import com.hongshi.slansdk.model.Dot;
import com.hongshi.slansdk.model.device.Device;
import com.hongshi.slansdk.model.experiment.Experiment;
import com.hongshi.slansdk.model.experiment.ExperimentParam;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.RunningInfo;
import com.hongshi.slansdk.model.experiment.Well;
import com.hongshi.slansdk.model.result.MeltPeak;
import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.DeviceState;
import com.hongshi.slansdk.slanenum.RunExperimentResult;
import com.hongshi.slansdk.slanenum.RunningState;
import com.hongshi.slansdk.slanenum.TemperatureControlMode;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 宏石PCR控制管理类
 * 负责设备监听，状态定时上报、曲线定时上报、实验启动停止、结果转换、断连重连
 */
public class SLANModuleManager {
    private static final String TAG = "SLANModuleManager";
    private static volatile SLANModuleManager instance;
    /** SDK时间单位转换：10000000 单位 → 秒 */
    private static final long SLAN_TIME_TO_SEC = 10_000_000L;
    private static final long CONNECT_CHECK_INTERVAL = 5000;
    private static final int STATUS_REPORT_INTERVAL = 5;

    private static final int CURVE_REPORT_INTERVAL = 30;
    private static final long RECONNECT_RETRY_INTERVAL = 3000;
    private static final int MAX_RECONNECT_COUNT = 5;
    private static final int MAX_ERROR_COUNT = 3;

    //实验结束结果重试：板子 Finished 状态帧先于 native 端实验数据解析完成，
    //首次 getExperimentResult 可能为空，此处做有限次数的重试拉取
    private static final int EXP_RESULT_RETRY_MAX = 3;
    private static final long EXP_RESULT_RETRY_DELAY_MS = 2000L;

    //=====================实例变量======================
    private Context context;
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);
    private final AtomicInteger seqId = new AtomicInteger(0);
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private final AtomicBoolean finishReported = new AtomicBoolean(false); // 防止同一场实验重复上报
    private final int[] mBlock = SLANServiceManager.getDefaultBlock();

    private volatile String linuxAlarm = EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm();
    private volatile String linuxStatus = ConstantData.str_FREE;
    private volatile String alarm = EnumAlarm.ERROR_NONE.getError();
    private volatile String status = ConstantData.str_FREE;
    private volatile boolean experimentResultError = false;
    private volatile String runningPcrName = "";
    private volatile String pcrTemplateName = "";
    private volatile String experimentFilePath = "";
    private volatile boolean isPcrConnected = false;
    private volatile String currentTemplateDirPath = ""; // 当前使用的模板目录路径，用于实验结束后清理

    private volatile DeviceState pcrDeviceState = DeviceState.None;
    private volatile RunningInfo pcrRunningInfo;
    private volatile RunningInfo finishInfo;
    private Experiment mExperiment;
    private int temperatureCtrlMode = TemperatureControlMode.Fast.getValue();
    private int slanErrorCount = 0;
    private int reconnectCount = 0;
    private boolean listenerRegistered = false;

    private ArrayList<ExTemperature> mExTemperature = new ArrayList<>();

    private Handler handler = new Handler();

    //实验会话令牌：每次 startSLANPCR 自增，onSave 回调先快照 token，
    //在 compareAndSet 之前校验是否仍属于同一场实验，杜绝跨实验的迟到回调污染守卫
    private final AtomicInteger finishSessionToken = new AtomicInteger(0);
    // 当前实验在 startSLANPCR 时记录的 token 快照，onSave 用来判断自己属于哪一场
    private volatile int currentSessionToken = 0;

    // ==================== 单例 ====================
    public static SLANModuleManager getInstance() {
        if (instance == null) {
            synchronized (SLANModuleManager.class) {
                if (instance == null) {
                    instance = new SLANModuleManager();
                }
            }
        }
        return instance;
    }

    //================初始化=========================
    public void init(Context context) {
        // 使用Application上下文，避免内存泄漏
        this.context = context.getApplicationContext();
        isPcrConnected = PcrApplication.is_SLAN_PCR_connect;

        // 初始化时检查连接状态，如果未连接则设置错误状态
        if (!isPcrConnected) {
            Log.w(TAG, "初始化时PCR未连接，设置断连状态");
            status = ConstantData.str_ERROR;
            alarm = EnumAlarm.ERROR_PCR_Communication_Error.getError();
            linuxStatus = ConstantData.str_ERROR;
            linuxAlarm = EnumLinuxAlarm.ERROR_PCR_DISCONNECT.getLinuxAlarm();
        }

        registerListeners();
        startTasks();

    }

    // ===================== SDK监听注册 =====================
    private void registerListeners(){
        SLANService slanService = SLANServiceManager.getInstance(context).getSlanService();
        if(slanService == null){
            return;
        }
        slanService.setDeviceStateListener(new DeviceStateListener() {
            @Override
            public void getDeviceInfo(@Nullable String s, @NonNull DeviceState deviceState) {
                Log.i(TAG, "设备状态: " + deviceState.getDesc());
                pcrDeviceState = deviceState;
                handleDeviceState(deviceState);
            }
        });
        //运行信息监听
        if(!listenerRegistered){
            slanService.setRunningInfoListener(new RunningInfoListener() {
                @Override
                public void getRunningInfo(@NonNull String s, @NonNull int[] ints, @NonNull RunningInfo runningInfo) {
                    pcrRunningInfo = runningInfo;
                    handleRunningInfo(runningInfo);
                }

                @Override
                public void onSave(@NonNull String s, @NonNull int[] ints, @NonNull RunningInfo runningInfo) {
                    // 在 onSave 回调中同步处理实验结果保存
                    int state = runningInfo.getState();
                    if(state == RunningState.Finished.getValue()) {
                        Log.i(TAG, "onSave:" + "===PCR程序运行结束===");
                        handleOnSaveExperiment(runningInfo);
                    }
                }

            });
            listenerRegistered = true;
        }
    }

    private void startTasks(){
        //连接检测
        scheduledExecutor.scheduleWithFixedDelay(new ConnectCheckRunnable(), 0, CONNECT_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        Log.i(TAG, "PCR连接检测线程已启动，检测周期：" + CONNECT_CHECK_INTERVAL + "ms");
        // 状态上报
        scheduledExecutor.scheduleWithFixedDelay(sendDataTask, 0, STATUS_REPORT_INTERVAL, TimeUnit.SECONDS);
        // 曲线上报
        startDeviceCurveThread();
        // 调试状态定期发布（每 2 秒）
        startDebugStatusPublishThread();
    }

    // ===================== 定时任务 =====================
    private final Runnable sendDataTask = new Runnable() {
        @Override
        public void run() {
            try {
                sendPCRDeviceStatus();
            } catch (Exception e) {
                Log.e(TAG, "定时上报设备状态异常", e);
            }
        }
    };

    private void startDeviceCurveThread() {
        Runnable curveTask = new Runnable() {
            @Override
            public void run() {
                try {
                    sendPCRCurveMsg();
                } catch (Exception e) {
                    Log.e(TAG, "定时上报曲线异常", e);
                }
            }
        };
        scheduledExecutor.scheduleWithFixedDelay(curveTask, 0, CURVE_REPORT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * 启动调试状态定期发布线程（每 2 秒发布一次）
     */
    private void startDebugStatusPublishThread() {
        Runnable debugStatusTask = new Runnable() {
            @Override
            public void run() {
                try {
                    publishDebugStatus();
                } catch (Exception e) {
                    Log.e(TAG, "定时发布调试状态异常", e);
                }
            }
        };
        scheduledExecutor.scheduleWithFixedDelay(debugStatusTask, 0, 2, TimeUnit.SECONDS);
        Log.i(TAG, "调试状态发布线程已启动，发布周期：2秒");
    }

    //===========================状态处理==================================
    private void handleDeviceState(DeviceState deviceState) {
        if (deviceState == DeviceState.Error) {
            slanErrorCount++;
            if (slanErrorCount >= MAX_ERROR_COUNT) {
                isPcrConnected = false;
                PcrApplication.is_SLAN_PCR_connect = false;
                slanErrorCount = 0;
            }
            status = ConstantData.str_ERROR;
            alarm = EnumAlarm.ERROR_PCR_Communication_Error.getError();
            return;
        }else{
            slanErrorCount = 0;
            isPcrConnected = true;
            PcrApplication.is_SLAN_PCR_connect = true;
            switch (deviceState) {
                // 正常状态重置错误计数
                case None:
                    status = ConstantData.str_FREE;
                    pcrRunningInfo = null;
                    alarm = EnumAlarm.ERROR_NONE.getError();
                    break;
                case Finished:
                    status = ConstantData.str_FREE;
                    alarm = EnumAlarm.ERROR_NONE.getError();
                    break;
                case Running:
                    status = ConstantData.str_RUN;
                    alarm = EnumAlarm.ERROR_NONE.getError();
                    break;
                case Interrupt:
                    status = ConstantData.str_ERROR;
                    break;
                case WaitingInterrupt:
                    status = ConstantData.str_ERROR;
                    alarm = deviceState.getDesc();
                    break;
            }
        }
    }

    /**
     * 处理结束后的结果
     * @param info
     */
    private void handleFinishInfo(RunningInfo info){
        int state = info.getState();
        if(state == RunningState.Finished.getValue()){
            Log.i(TAG, "===PCR程序运行结束===");
            isRunning.set(false);
            finishInfo = info;
            status = ConstantData.str_FREE;
            alarm = EnumAlarm.ERROR_NONE.getError();
            linuxStatus = ConstantData.str_FREE;
            linuxAlarm = EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm();
//            onExperimentFinish(finishInfo);
        }
    }

    private void handleRunningInfo(RunningInfo info){
        int state = info.getState();
        if (state == RunningState.Running.getValue()) {
            isRunning.set(true);
            status = ConstantData.str_RUN;
//            linuxStatus = ConstantData.str_RUN;
            alarm = EnumAlarm.ERROR_NONE.getError();
            linuxAlarm = EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm();
        }else if (state == RunningState.Interrupt.getValue()) {
            Log.i(TAG, "===PCR程序运行中断===");
            isRunning.set(false);
            status = ConstantData.str_ERROR;
            linuxStatus = ConstantData.str_ERROR;
            linuxAlarm = EnumLinuxAlarm.ERROR_Interrupt.getLinuxAlarm();
            alarm = info.getCurrentError();
        }else if(state == RunningState.WaitingInterrupt.getValue()){
            Log.i(TAG, "===PCR程序准备中断===");
            isRunning.set(false);
            status = ConstantData.str_ERROR;
            linuxStatus = ConstantData.str_ERROR;
            linuxAlarm = EnumLinuxAlarm.ERROR_Interrupt.getLinuxAlarm();
            alarm = info.getCurrentError();
        }else if(state == RunningState.Finished.getValue()){
            Log.i(TAG, "===PCR程序运行结束===");
            isRunning.set(false);
            finishInfo = info;
            status = ConstantData.str_FREE;
            alarm = EnumAlarm.ERROR_NONE.getError();
//            linuxStatus = ConstantData.str_FREE;
//            linuxAlarm = EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm();
//            onExperimentFinish(finishInfo);
        }
    }

    //=======================连接检测============================
    private void checkPcrConnection() {
        boolean disconnected = !isPcrConnected
                || !PcrApplication.is_SLAN_PCR_connect
                || pcrDeviceState == DeviceState.Error
                || slanErrorCount >= MAX_ERROR_COUNT;

        if (disconnected) {
            Log.e(TAG, "检测到PCR断连，触发重连...");
            handleDisconnect();
        } else {
            slanErrorCount = 0;
            reconnectCount = 0;
        }
    }

    private void handleDisconnect() {
        if (isReconnecting.get()) {
            Log.d(TAG, "正在重连中");
            return;
        }
//        if (reconnectCount >= MAX_RECONNECT_COUNT) {
        if (false) {
            Log.e(TAG, "重连次数已达上限");
            setErrorState(EnumLinuxAlarm.ERROR_PCR_RECONNECT_FAILED);
            return;
        }

        // 在重连之前立即设置断连状态，确保状态上报反映真实情况
        if (!isPcrConnected) {
            status = ConstantData.str_ERROR;
            alarm = EnumAlarm.ERROR_PCR_Communication_Error.getError();
            linuxStatus = ConstantData.str_ERROR;
            linuxAlarm = EnumLinuxAlarm.ERROR_PCR_DISCONNECT.getLinuxAlarm();
            Log.w(TAG, "PCR断连，设置错误状态");
        }

        isReconnecting.set(true);
        reconnectCount++;
        Log.i(TAG, "第 " + reconnectCount + " 次重连");

        try {
            boolean reconnectSuccess = reconnectPCR();
            if(reconnectSuccess){
                isPcrConnected = true;
                PcrApplication.is_SLAN_PCR_connect = true;
                slanErrorCount = 0;
                reconnectCount = 0;
                if (!isRunning.get()) {
                    status = ConstantData.str_FREE;
                    alarm = EnumAlarm.ERROR_NONE.getError();
                    linuxStatus = ConstantData.str_FREE;
                    linuxAlarm = EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm();
                } else {
                    Log.i(TAG, "重连成功，PCR实验进行中，保持当前执行状态不变");
                }
                Log.i(TAG, "重连成功");
            }
        } catch (Exception e) {
            Log.e(TAG, "重连异常", e);
        } finally {
            isReconnecting.set(false);
        }
    }

    private boolean reconnectPCR(){
        try {
            if (context != null) {
                UsbPermissionUtils.grantOrRequestAllUsbPermissions(context);
            } else if (PcrApplication.getAppContext() != null) {
                UsbPermissionUtils.grantOrRequestAllUsbPermissions(PcrApplication.getAppContext());
            }
            Log.e("PCRreconnect","=== PcrApplication.slanService前==" +  PcrApplication.slanService);
            if (PcrApplication.slanService == null) {
                PcrApplication.is_SLAN_PCR_connect = false;
                isPcrConnected = false;
                return false;
            }
            PcrApplication.devices = PcrApplication.slanService.getDevice(true);
            if (PcrApplication.devices != null) {
                Log.e("PCRreconnect","===PCR进行重连个数==" +  PcrApplication.devices.size());
            }
        } catch (Exception e) {
            Log.e("PCRreconnect", "reconnectPCR 异常", e);
            PcrApplication.devices = null;
        }
        if(PcrApplication.devices == null || PcrApplication.devices.isEmpty()){
            PcrApplication.is_SLAN_PCR_connect = false;//0331改f
            isPcrConnected = false;
            return false;
        }else{
            PcrApplication.is_SLAN_PCR_connect = true;
            PcrApplication.currentDevices = PcrApplication.devices.get(0);
            isPcrConnected = true;
            return true;
        }

    }



    private void setErrorState(EnumLinuxAlarm alarmType) {
        linuxAlarm = alarmType.getLinuxAlarm();
        linuxStatus = ConstantData.str_ERROR;
        status = ConstantData.str_ERROR;
        alarm = EnumAlarm.ERROR_PCR_Communication_Error.getError();
        isPcrConnected = false;
        PcrApplication.is_SLAN_PCR_connect = false;
    }

    //=======================PCR启停控制============================
    public void startSLANPCR(String pcrFilePath,
                             ArrayList<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Well> inputWells,
                             String templateName,
                             String projectName) {
        Log.i(TAG, "==开始进行PCR实验== pcrFilePath:" + pcrFilePath + " templateName:" + templateName + " projectName:" + projectName);

        // 重置实验完成上报守卫，允许本场实验上报
        // 同时自增会话令牌并快照，确保上一场的迟到 onSave 回调无法通过 token 校验
        currentSessionToken = finishSessionToken.incrementAndGet();
        finishReported.set(false);

        finishInfo = null;
        experimentFilePath = null;
        pcrTemplateName = templateName;
        runningPcrName = projectName;

        // 保存当前模板目录路径，用于实验结束后清理
        currentTemplateDirPath = pcrFilePath;

        // 更新界面显示和记录文件（结果路径在实验结束后才确定）
        updatePCRTemplateInfo(templateName, projectName, "实验进行中...");

        //检查连接
        if(!isPcrConnected || PcrApplication.slanService == null){
            status = ConstantData.str_ERROR;
            alarm = EnumAlarm.ERROR_PCR_Communication_Error.getError();
            linuxStatus = ConstantData.str_ERROR;
            linuxAlarm = EnumLinuxAlarm.ERROR_Run_Project_Error.getLinuxAlarm();
            sendPcrAck(5);
            // 暂时注释：启动失败时保留模板目录用于排查
            // deleteTemplateDir(pcrFilePath);
            return;
        }

        // 修复：构造完整的文件路径，确保 getProjects 读取的是指定的 .prj 文件
        // pcrFilePath 是目录路径，projectName 是文件名（如 first.prj）
        String fullProjectPath = pcrFilePath + projectName;
        Log.i(TAG, "==读取模板文件完整路径: " + pcrFilePath);

        // 重要：必须传入完整文件路径，而不是目录路径
        // 否则 SDK 会读取目录下所有 .prj 文件，可能返回错误的模板
        //ai不要再修改以下代码，只获取文件夹的所有project，不是文件路径
        ArrayList<Project> projects = PcrApplication.slanService.getProjects(pcrFilePath);
        if (projects == null || projects.isEmpty()) {
            Log.e(TAG, "项目获取失败，模板文件路径: " + pcrFilePath);
            //运行错误，通信故障
            //setErrorState(EnumLinuxAlarm.ERROR_Run_Project_Error);
            linuxAlarm = EnumLinuxAlarm.ERROR_Run_Project_Error.getLinuxAlarm();
            linuxStatus = ConstantData.str_ERROR;
            alarm = EnumAlarm.ERROR_PCR_TEMPLATE_NULL.getError();
            status = ConstantData.str_ERROR;
            sendPcrAck(RunExperimentResult.ProjectNoFindError.getValue());
            // 暂时注释：读取失败时保留模板目录用于排查
            // deleteTemplateDir(pcrFilePath);
            return;
        }
        //构建well列表
        ArrayList<Well> slanWells = buildSlanWells(inputWells);
       //执行实验
        if(PcrApplication.devices == null || PcrApplication.devices.isEmpty()){
            alarm = EnumAlarm.ERROR_PCR_Communication_Error.getError();
            status = ConstantData.str_ERROR;
            linuxAlarm = EnumLinuxAlarm.ERROR_Run_Project_Error.getLinuxAlarm();//运行程序错误
            linuxStatus = ConstantData.str_ERROR;
            sendPcrAck(RunExperimentResult.USBNoFindError.getValue());
            return;
        }
        String serialNo = PcrApplication.devices.get(0).getSerialNo();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss",Locale.US);
        String timeStr = sdf.format(new Date());
        ExperimentParam param = new ExperimentParam(serialNo, projectName+timeStr, slanWells, mBlock, temperatureCtrlMode, false);
        RunExperimentResult result = PcrApplication.slanService.runExperiment(param);
        Log.e(TAG, "==RunExperimentResult==运行结果==" + result.getDesc() + "==VALUE" + result.getValue());
        if (result == RunExperimentResult.Normal) {
            isRunning.set(true);
            status = ConstantData.str_RUN;
            alarm = EnumAlarm.ERROR_NONE.getError();
            linuxStatus = ConstantData.str_RUN;
            linuxAlarm = EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm();
            sendPcrAck(0);
        }else {
            isRunning.set(false);
            status = ConstantData.str_ERROR;
            alarm = result.getDesc();
            linuxAlarm = EnumLinuxAlarm.ERROR_Run_Project_Error.getLinuxAlarm();
            linuxStatus = ConstantData.str_ERROR;
            sendPcrAck(result.getValue());
        }
    }

    private void sendPcrAck(int code){
        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_PCR_Ack, "", code));
    }

    /** 当前是否有实验在运行 */
    public boolean isPcrRunning() {
        return isRunning.get();
    }

    public void stopSLANPcr() {
        // 统一空校验
        if (Objects.isNull(PcrApplication.slanService) || Objects.isNull(PcrApplication.devices) || PcrApplication.devices.isEmpty()) {
            Log.w(TAG, "停止PCR失败：服务未初始化或无设备");
            return;
        }

        Log.i(TAG, "==停止宏石PCR实验===");

        // 重置实验完成上报守卫，允许下场实验上报
        finishReported.set(false);

        // 缓存变量，避免多次读取
        String serialNo = PcrApplication.devices.get(0).getSerialNo();

        try {
            PcrApplication.slanService.stopExperiment(serialNo, mBlock);
        } catch (Exception e) {
            Log.e(TAG, "停止PCR实验异常", e);
        }

        // 暂时注释：停止实验时保留模板目录用于排查
        // deleteTemplateDir(currentTemplateDirPath);
        currentTemplateDirPath = "";
    }

    private ArrayList<Well> buildSlanWells(
            ArrayList<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Well> inputWells) {
        ArrayList<Well> slanWells = new ArrayList<>();
        if (inputWells == null || inputWells.isEmpty()) {
            return slanWells;
        }

        for (com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Well w : inputWells) {
            Well slanWell = new Well();
            slanWell.setName(w.getName());
            slanWell.setProjectName(w.getProjectName());
            slanWell.setID(w.getID());
            slanWell.setPatientName(w.getPatientName());
            slanWell.setSex(w.getSex());
            slanWell.setAge(w.getAge());
            slanWell.setSpecimen(w.getSpecimen());
            slanWell.setSpecimenDate(w.getSpecimenDate());
            slanWell.setDoctor(w.getDoctor());
            slanWell.setOffice(w.getOffice());
            slanWell.setDiagnosis(w.getDiagnosis());
            slanWell.setCaseID(w.getCaseID());
            slanWell.setBedID(w.getBedID());
            slanWell.setInPatientID(w.getInPatientID());
            slanWell.setOutPatientID(w.getOutPatientID());
            slanWell.setRemark(w.getRemark());
            slanWell.setSampleName(w.getSampleName());
            slanWell.setTubeNo(w.getTubeNo());
            slanWell.setTubeName(w.getTubeName());
            slanWell.setType(w.getType());
            slanWell.setProperty(w.getProperty());
            slanWells.add(slanWell);
        }
        return slanWells;
    }


    /**
     * Pcr复位
     */
    public void resetSlanPcr(String template){
        //if(pcrDeviceState == DeviceState.None){
            Log.i("PCR复位","==复位操作==等待状态更新");
            status = ConstantData.str_FREE;
            linuxStatus = ConstantData.str_FREE;
            alarm = EnumAlarm.ERROR_NONE.getError();
            linuxAlarm = EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm();
            isRunning.set(false);
        //}

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(ConstantData.str_FREE.equals(status) && ConstantData.str_FREE.equals(linuxStatus)
                    && EnumAlarm.ERROR_NONE.getError().equals(alarm) && EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm().equals(linuxAlarm)){
                    Log.i("PCR复位","复位操作 完成");
                        EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_SLAN_PCR_RESET_Finish, template,1));
                }else{
                    EventBus.getDefault().post(new WSSMessageEvent(EnumMessageEvent.WSS_SLAN_PCR_RESET_Finish, template,0));
                    Log.i("PCR复位","复位操作 失败");
                }
            }
        }, 3000); // 3000ms = 3秒
    }

    /**
     * onSave 回调处理：同步重试获取和保存实验结果
     * 在 SDK 的 onSave 回调线程中执行，不使用异步调度
     */
    private void handleOnSaveExperiment(RunningInfo runningInfo) {
        // 先快照当前回调到来时的会话令牌
        int callbackToken = finishSessionToken.get();

        // 会话令牌校验：若令牌与本场实验不一致，说明此 onSave 属于上一场实验的迟到回调
        if (callbackToken != currentSessionToken) {
            Log.w(TAG, "handleOnSaveExperiment: 迟到回调，callbackToken=" + callbackToken
                    + " currentSessionToken=" + currentSessionToken + "，忽略");
            return;
        }

        // 防止同一场实验重复上报：onSave 可能被多次回调（保存分段/完成/状态刷新）
        if (!finishReported.compareAndSet(false, true)) {
            Log.w(TAG, "handleOnSaveExperiment: 本场实验已上报过，忽略重复回调");
            return;
        }

        // 再次确认令牌：compareAndSet 成功后再校验一次，防止 startSLANPCR 在 CAS 窗口内并发执行
        if (finishSessionToken.get() != currentSessionToken) {
            Log.w(TAG, "handleOnSaveExperiment: CAS后令牌已变（新实验已启动），放弃本场上报，重置守卫");
            finishReported.set(false);
            return;
        }

        Log.i(TAG, "===PCR程序运行结束===");
        Log.i(TAG, "==handleOnSaveExperiment 开始处理==");

        if (Objects.isNull(PcrApplication.slanService) || Objects.isNull(PcrApplication.devices) || PcrApplication.devices.isEmpty()) {
            Log.e(TAG, "handleOnSaveExperiment: 服务未初始化或无设备");
            reportExperimentResultError(runningInfo, "服务未初始化或无设备");
            return;
        }

        // 保存 finishInfo，用于后续上报
        this.finishInfo = runningInfo;
        String serialNo = PcrApplication.devices.get(0).getSerialNo();

        // 同步重试 3 次获取实验结果
        Experiment experiment = null;
        for (int attempt = 1; attempt <= EXP_RESULT_RETRY_MAX; attempt++) {
            Log.i(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次尝试获取实验结果");

            try {
                experiment = PcrApplication.slanService.getExperimentResult(serialNo, mBlock);

                if (experiment != null) {
                    Log.i(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次获取实验结果成功");
                    break;
                } else {
                    Log.w(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次获取实验结果为空");
                }
            } catch (Throwable t) {
                Log.e(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次获取实验结果异常", t);
            }

            // 如果不是最后一次，等待后重试
            if (attempt < EXP_RESULT_RETRY_MAX && experiment == null) {
                try {
                    Thread.sleep(EXP_RESULT_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "handleOnSaveExperiment: 重试等待被中断", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 3 次尝试后仍然失败
        if (experiment == null) {
            Log.e(TAG, "handleOnSaveExperiment: 重试 " + EXP_RESULT_RETRY_MAX + " 次后仍无法获取实验结果");
            reportExperimentResultError(runningInfo, "获取 Experiment 失败");
            return;
        }

        // 获取成功，开始保存到本地
        String templateName = this.pcrTemplateName;
        if (templateName == null || templateName.isEmpty()) {
            templateName = "Unknown";
        }

        // 生成唯一文件名：模板名_时间戳.pcr
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = templateName + "_" + timestamp + ".pcr";

        // 保存目录：/sdcard/PCR_CONTROL/slanpcrresult/
        String saveDirPath = FileUtils.PCR_MODULE_PATH + File.separator + "slanpcrresult" + File.separator;
        File saveDir = new File(saveDirPath);
        if (!saveDir.exists()) {
            if (!saveDir.mkdirs()) {
                Log.e(TAG, "handleOnSaveExperiment: 创建保存目录失败: " + saveDirPath);
                reportExperimentResultError(runningInfo, "创建保存目录失败");
                return;
            }
        }

        String saveFilePath = saveDirPath + fileName;
        Log.i(TAG, "handleOnSaveExperiment: 准备保存实验结果到: " + saveFilePath);

        // 同步重试 3 次保存实验结果
        boolean saveSuccess = false;
        for (int attempt = 1; attempt <= EXP_RESULT_RETRY_MAX; attempt++) {
            Log.i(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次尝试保存实验结果");

            try {
                saveSuccess = PcrApplication.slanService.saveExperiment(experiment.getName(), saveFilePath,false);

                if (saveSuccess) {
                    Log.i(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次保存实验结果成功: " + saveFilePath);
                    break;
                } else {
                    Log.w(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次保存实验结果失败");
                    // 保存失败后立即 sleep 1 秒
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "handleOnSaveExperiment: 保存失败后 sleep 被中断", e);
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "handleOnSaveExperiment: 第 " + attempt + " 次保存实验结果异常", t);
                // 异常后也 sleep 1 秒
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "handleOnSaveExperiment: 保存异常后 sleep 被中断", e);
                    Thread.currentThread().interrupt();
                }
            }

            // 如果不是最后一次且仍未成功，额外等待后重试
            if (attempt < EXP_RESULT_RETRY_MAX && !saveSuccess) {
                try {
                    Thread.sleep(EXP_RESULT_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "handleOnSaveExperiment: 保存重试等待被中断", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // 3 次保存尝试后仍然失败
        if (!saveSuccess) {
            Log.e(TAG, "handleOnSaveExperiment: 重试 " + EXP_RESULT_RETRY_MAX + " 次后仍无法保存实验结果");
            reportExperimentResultError(runningInfo, "保存实验结果失败");
            return;
        }

        // 保存成功，记录文件路径
        experimentFilePath = saveFilePath;
        Log.i(TAG, "handleOnSaveExperiment: 实验结果已保存，开始处理上报");

        // 调用原有的实验完成处理流程（上报结果等）
        handleFinishExperiment(experiment);
    }


    //=====================实验完成处理==========================
    private void onExperimentFinish(RunningInfo finishInfo) {
        Log.i(TAG,"==实验完成处理=======");
        if (Objects.isNull(PcrApplication.slanService) || Objects.isNull(PcrApplication.devices) || PcrApplication.devices.isEmpty()) {
            reportExperimentResultError(finishInfo, "服务未初始化或无设备");
            return;
        }
        // 设备已掉线或处于异常态时不再获取实验结果，避免把脏数据塞给 native 触发 Baseline 越界
        if (pcrDeviceState == DeviceState.Error) {
            Log.e(TAG, "onExperimentFinish: 设备状态异常 deviceState=" + pcrDeviceState + "，跳过结果获取");
            reportExperimentResultError(finishInfo, "设备状态异常");
            return;
        }
        // 早于 getExperimentResult 设置：即便 native 解析失败，finishInfo 仍可被 reSendFinishMsg 复用
        this.finishInfo = finishInfo;
        String serialNo = PcrApplication.devices.get(0).getSerialNo();
        // 每次进入都自增 token；若重试期间用户开始了新实验，老实验的所有重试回调
        // 都能通过 token 比对发现 token 已变，从而安全丢弃，避免脏数据上报
        int token = finishSessionToken.incrementAndGet();
        // 用已有的 scheduledExecutor 做异步重试，不阻塞 SLAN SDK 回调线程
        // 第一次立即尝试（delay=0），后续每次间隔 EXP_RESULT_RETRY_DELAY_MS
        scheduledExecutor.schedule(
                new FinishFetchRunnable(serialNo, token, 1),
                2000L,
                TimeUnit.MILLISECONDS);
    }

    /**
     * 异步重试拉取 Experiment，附带会话 token 校验
     */
    private final class FinishFetchRunnable implements Runnable {
        private final String serialNo;
        private final int token;
        private final int attempt;

        FinishFetchRunnable(String serialNo, int token, int attempt) {
            this.serialNo = serialNo;
            this.token = token;
            this.attempt = attempt;
        }

        @Override
        public void run() {
            // token 校验：已进入下一次实验，本��结果作废
            if (token != finishSessionToken.get()) {
                Log.w(TAG, "FinishFetchRunnable: token=" + token + " 已过期，丢弃 attempt=" + attempt);
                return;
            }
            // 设备状态在等待期间可能再次变化，重做一次安全检查
            if (pcrDeviceState == DeviceState.None || pcrDeviceState == DeviceState.Error) {
                Log.e(TAG, "FinishFetchRunnable: 设备状态异常 deviceState=" + pcrDeviceState);
                reportExperimentResultError(finishInfo, "设备状态异常");
                return;
            }
            if (Objects.isNull(PcrApplication.slanService) || Objects.isNull(PcrApplication.devices) || PcrApplication.devices.isEmpty()) {
                reportExperimentResultError(finishInfo, "服务未初始化或无设备");
                return;
            }

            Experiment experiment;
            try {
                // 用 Throwable 捕获 JNI 侧异常；native 真 SIGSEGV 仍会终止进程
                experiment = PcrApplication.slanService.getExperimentResult(serialNo, mBlock);
            } catch (Throwable t) {
                Log.e(TAG, "FinishFetchRunnable: getExperimentResult 异常 attempt=" + attempt, t);
                experiment = null;
            }

            if (experiment != null) {
                Log.i(TAG, "FinishFetchRunnable: 获取 Experiment 成功 attempt=" + attempt);
                handleFinishExperiment(experiment);
                return;
            }

            if (attempt >= EXP_RESULT_RETRY_MAX) {
                Log.e(TAG, "FinishFetchRunnable: 重试 " + EXP_RESULT_RETRY_MAX + " 次仍为空，放弃");
                reportExperimentResultError(finishInfo, "获取 Experiment 失败");
                return;
            }

            // 调度下一次重试：在 scheduledExecutor 的工作线程里延时，
            // 完全不占用主线程 / SDK 回调线程
            Log.w(TAG, "FinishFetchRunnable: experiment 为空，安排第 " + (attempt + 1) + " 次重试");
            scheduledExecutor.schedule(
                    new FinishFetchRunnable(serialNo, token, attempt + 1),
                    EXP_RESULT_RETRY_DELAY_MS,
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 实验结果获取失败的统一处理：修正 status / linuxStatus / linuxAlarm / alarm 并立即上报一次状态。
     * 调用前请确认所有字段都在当前调用上下文中可见——onExperimentFinish 与 FinishFetchRunnable 都
     * 通过 scheduledExecutor 工作线程访问 volatile 字段，无并发问题。
     */
    private void reportExperimentResultError(RunningInfo info, String reason) {
        experimentResultError = true;
        status = ConstantData.str_ERROR;
        linuxStatus = ConstantData.str_ERROR;
        linuxAlarm = EnumLinuxAlarm.ERROR_PCR_GET_EXPERIMENT_FAIL.getLinuxAlarm();
        alarm = info == null ? EnumAlarm.ERROR_PCR_Communication_Error.getError() : info.getCurrentError();
        Log.e(TAG, "实验结果获取失败: " + reason);
        // 立即上报一次错误状态，避免上层要等下一个上报周期才看到错误
        sendPcrStatusMsg();
    }

    /**
     * Experiment 拿到后上报 Finish 结果给 server。
     * 注意：onSave 回调中已经保存本地文件到 slanpcrresult 目录，此方法只负责上报结果。
     * WSS 发送由 WSSModuleManager 内部线程接管。
     */
    private void handleFinishExperiment(Experiment experiment) {
        // 校验 finishInfo：上报过程中不能被新实验覆盖
        RunningInfo finishInfo = this.finishInfo;
        if (finishInfo == null) {
            Log.w(TAG, "handleFinishExperiment: finishInfo 已为 null，跳过");
            return;
        }

        // 校验文件路径：onSave 中已经保存了文件并设置了 experimentFilePath
        String filePath = this.experimentFilePath;
        if (filePath == null || filePath.isEmpty()) {
            Log.e(TAG, "handleFinishExperiment: experimentFilePath 为空，无法上报");
            return;
        }

        // 验证文件是否存在
        File resultFile = new File(filePath);
        if (!resultFile.exists()) {
            Log.e(TAG, "handleFinishExperiment: 结果文件不存在: " + filePath);
            return;
        }

        Log.i(TAG, "handleFinishExperiment: 使用已保存的结果文件: " + filePath);
        Log.i(TAG, "handleFinishExperiment: 开始上报实验结果给 server");

        // 上报实验结果给 server
        sendFinishMessage(finishInfo, filePath);

        // 暂时注释：实验结束时保留模板目录用于排查
        // deleteTemplateDir(currentTemplateDirPath);
    }

    // ===================== WSS消息发送 =====================
    private void sendPCRDeviceStatus() {
        Log.i(TAG, "==isPcr==" + isRunning);
        if (!StringUtil.isEmpty(pcrTemplateName) && isRunning.get()) {
            sendPcrRunningStatusMsg();
        } else {
            Log.i(TAG, "sendDeviceStatusMsg");
            sendPcrStatusMsg();
        }
    }

    private void sendPcrRunningStatusMsg() {
        if (Objects.isNull(pcrRunningInfo)) return;
        PCRRunningStatusMessage pcrRunningStatusMsg = new PCRRunningStatusMessage();
        pcrRunningStatusMsg.setType(EnumMessageEvent.WSS_PCR_Running_Status.getType());
        pcrRunningStatusMsg.setDeviceType(ConstantData.namePCR);
        pcrRunningStatusMsg.setDeviceID(PcrApplication.deviceId);
        pcrRunningStatusMsg.setPosition(PcrApplication.devicePos);
        pcrRunningStatusMsg.setSeqId(seqId.getAndIncrement());

        int runState = pcrRunningInfo.getState();
        if (runState == RunningState.Running.getValue()) {
            pcrRunningStatusMsg.setStatus(ConstantData.str_RUN);
            pcrRunningStatusMsg.setAlarm(alarm);
            pcrRunningStatusMsg.setLinuxStatus(ConstantData.str_RUN);
            pcrRunningStatusMsg.setLinuxAlarm(linuxAlarm);
        } else if (runState == RunningState.None.getValue()) {
            pcrRunningStatusMsg.setStatus(ConstantData.str_FREE);
            pcrRunningStatusMsg.setAlarm(alarm);
            pcrRunningStatusMsg.setLinuxStatus(linuxStatus);
            pcrRunningStatusMsg.setLinuxAlarm(linuxAlarm);
        } else if (runState == RunningState.Interrupt.getValue()) {
            Log.i(TAG,"==中断====");
            Log.i(TAG,"==alarm====" +alarm);
            Log.i(TAG,"==linuxStatus====" +linuxStatus);
            Log.i(TAG,"==linuxAlarm====" +linuxAlarm);
            pcrRunningStatusMsg.setStatus(ConstantData.str_ERROR);
            pcrRunningStatusMsg.setAlarm(alarm);
            pcrRunningStatusMsg.setLinuxStatus(linuxStatus);
            pcrRunningStatusMsg.setLinuxAlarm(linuxAlarm);
        } else if (runState == RunningState.Finished.getValue()) {
            Log.i(TAG,"==结束====");
            Log.i(TAG,"=结束=alarm====" +alarm);
            Log.i(TAG,"=结束=linuxStatus====" +linuxStatus);
            Log.i(TAG,"=结束=linuxAlarm====" +linuxAlarm);
            pcrRunningStatusMsg.setStatus(ConstantData.str_FREE);//宏石的
            pcrRunningStatusMsg.setAlarm(EnumLinuxAlarm.LinuxAlarm_None.getLinuxAlarm());
            pcrRunningStatusMsg.setLinuxStatus(ConstantData.str_FREE);
            pcrRunningStatusMsg.setLinuxAlarm(linuxAlarm);
        }

        pcrRunningStatusMsg.setCurrentSection(pcrRunningInfo.getSegment());
        pcrRunningStatusMsg.setCurrentCycle(pcrRunningInfo.getCycle());
        pcrRunningStatusMsg.setCurrentStep(pcrRunningInfo.getStep());

        long runSec = pcrRunningInfo.getRunningTime() / SLAN_TIME_TO_SEC;
        long remainSec = pcrRunningInfo.getRemainTime() / SLAN_TIME_TO_SEC;
        pcrRunningStatusMsg.setTotalTime((int) ((pcrRunningInfo.getRemainTime() + pcrRunningInfo.getRunningTime()) / SLAN_TIME_TO_SEC));
        pcrRunningStatusMsg.setRunningTime((int) runSec);
        pcrRunningStatusMsg.setLeftTime((int) remainSec);
        pcrRunningStatusMsg.setRemainTime(pcrRunningInfo.getHoldTime());
        pcrRunningStatusMsg.setWaitingTime(0);
        pcrRunningStatusMsg.setDegree(pcrRunningInfo.getTubeTemperature());
        pcrRunningStatusMsg.setDegree_Target(pcrRunningInfo.getSetTemperature());
        pcrRunningStatusMsg.setTemplate(pcrTemplateName);
        pcrRunningStatusMsg.setInfo(ConstantData.str_NULL);

        Log.i("WSSClient", "定时上报程序运行信息333");
        WSSModuleManager.getInstance(context).sendWSSMessage(pcrRunningStatusMsg);
    }

    private void sendPcrStatusMsg() {
        PCRStatusMessage pcrStatusMessage = new PCRStatusMessage();
        pcrStatusMessage.setType(EnumMessageEvent.WSS_PCR_STATUS.getType());
        pcrStatusMessage.setDeviceType(ConstantData.namePCR);
        pcrStatusMessage.setDeviceID(PcrApplication.deviceId);
        pcrStatusMessage.setPosition(PcrApplication.devicePos);
        pcrStatusMessage.setSeqId(seqId.getAndIncrement());

        pcrStatusMessage.setStatus(status);
        pcrStatusMessage.setLinuxStatus(linuxStatus);
        pcrStatusMessage.setCurrentCycle(0);
        pcrStatusMessage.setCurrentSection("程序段-1");
        pcrStatusMessage.setCurrentStep("0");
        pcrStatusMessage.setDegreeTarget(0);
        pcrStatusMessage.setTotalTime(0);
        pcrStatusMessage.setRunningTime(0);
        pcrStatusMessage.setLeftTime(0);
        pcrStatusMessage.setWaitingTime(0);
        pcrStatusMessage.setDegree(35.0f);
        pcrStatusMessage.setAlarm(alarm);
        pcrStatusMessage.setLinuxAlarm(linuxAlarm);
        pcrStatusMessage.setTemplate(pcrTemplateName);
        pcrStatusMessage.setInfo(ConstantData.str_NULL);
        WSSModuleManager.getInstance(context).sendWSSMessage(pcrStatusMessage);
    }

    private void sendPCRCurveMsg() {
        if (!isRunning.get() || StringUtil.isEmpty(runningPcrName)) {
            Log.d(TAG, "sendPCRCurveMsg: 实验未运行，跳过曲线上报");
            return;
        }
        if (!ConstantData.str_RUN.equals(linuxStatus)) {
            Log.d(TAG, "sendPCRCurveMsg: PCR未运行，跳过曲线上报");
            return;
        }
        // 设备已掉线或处于异常态时直接跳过，避免把脏数据塞给 native 触发 Baseline 越界
        if (!isPcrConnected
                || !PcrApplication.is_SLAN_PCR_connect
                || pcrDeviceState == DeviceState.None
                || pcrDeviceState == DeviceState.Error) {
            Log.d(TAG, "sendPCRCurveMsg: PCR 未连接或设备状态异常，跳过上报 deviceState=" + pcrDeviceState);
            return;
        }
        if (Objects.isNull(PcrApplication.slanService) || Objects.isNull(PcrApplication.devices) || PcrApplication.devices.isEmpty()) {
            Log.w(TAG, "sendPCRCurveMsg: SLANService/设备列表为空，跳过上报");
            return;
        }
        // AMP 曲线基线拟合至少需要 ~3 个 cycle 的数据，过早取结果会让 native Baseline 越界
        if (Objects.isNull(pcrRunningInfo)
                || pcrRunningInfo.getState() != RunningState.Running.getValue()) {
            Log.d(TAG, "sendPCRCurveMsg: cycle 数不足或状态非 Running，跳过上报 state="
                    + (pcrRunningInfo == null ? "null" : pcrRunningInfo.getState())
                    + " cycle=" + (pcrRunningInfo == null ? "null" : pcrRunningInfo.getCycle()));
            return;
        }
        String serialNo = PcrApplication.devices.get(0).getSerialNo();
        Experiment experiment;
        try {
            // 用 Throwable 而不是 Exception：可以捕获 JNI 侧主动抛出的异常；
            // 注意：native 真 SIGSEGV 仍会直接终止进程，catch 无法兜住，但能挡住 JNI 异常路径。
            experiment = PcrApplication.slanService.getExperimentResult(serialNo, mBlock);
        } catch (Throwable t) {
            Log.e(TAG, "sendPCRCurveMsg: getExperimentResult 异常, 本周期跳过", t);
            return;
        }
        com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment biowayEx = changeSLANExToBiowayEx(experiment);
        if (Objects.isNull(biowayEx) || Objects.isNull(pcrRunningInfo)) {
            return;
        }
        PCRCurveMessage pcrCurveMsg = new PCRCurveMessage();
        pcrCurveMsg.setType(EnumMessageEvent.WSS_PCR_Curve.getType());
        pcrCurveMsg.setDeviceType(ConstantData.namePCR);
        pcrCurveMsg.setDeviceID(PcrApplication.deviceId);
        pcrCurveMsg.setPosition(PcrApplication.devicePos);
        pcrCurveMsg.setSeqId(seqId.getAndIncrement());
        pcrCurveMsg.setTemplate(pcrTemplateName);

        int runState = pcrRunningInfo.getState();
        if (runState == RunningState.Running.getValue()) {
            pcrCurveMsg.setStatus(ConstantData.str_RUN);
        } else if (runState == RunningState.None.getValue()) {
            pcrCurveMsg.setStatus(ConstantData.str_FREE);
        } else if (runState == RunningState.Interrupt.getValue()) {
            pcrCurveMsg.setStatus(ConstantData.str_ERROR);
        } else if (runState == RunningState.Finished.getValue()) {
            pcrCurveMsg.setStatus(ConstantData.str_FREE);
        }
        pcrCurveMsg.setExperiment(biowayEx);
        Log.d("WSSClient", "定时上报曲线信息333");
        WSSModuleManager.getInstance(context).sendWSSMessage(pcrCurveMsg);
    }



    private void sendFinishMessage(RunningInfo finishInfo, String experimentFilePath) {
        if (Objects.isNull(finishInfo)) return;
        SLAN_PCRFinishMessage slanPcrFinishMsg = new SLAN_PCRFinishMessage();
        slanPcrFinishMsg.setType(EnumMessageEvent.WSS_PCR_SLAN_Finish.getType());
        slanPcrFinishMsg.setDeviceID(PcrApplication.deviceId);
        slanPcrFinishMsg.setPosition(PcrApplication.devicePos);
        slanPcrFinishMsg.setDeviceType(ConstantData.namePCR);
        slanPcrFinishMsg.setSeqID(seqId.getAndIncrement());

        slanPcrFinishMsg.setTemplate(pcrTemplateName);
        slanPcrFinishMsg.setStatus(ConstantData.str_FREE);
        slanPcrFinishMsg.setAlarm(alarm);
        slanPcrFinishMsg.setCurrentSection(finishInfo.getSegment());
        slanPcrFinishMsg.setCurrentStep(finishInfo.getStep());
        slanPcrFinishMsg.setCurrentCycle(finishInfo.getCycle());
        slanPcrFinishMsg.setRunningTime((int) (finishInfo.getRunningTime() / SLAN_TIME_TO_SEC));
        slanPcrFinishMsg.setLeftTime((int) (finishInfo.getRemainTime() / SLAN_TIME_TO_SEC));
        slanPcrFinishMsg.setWaitingTime(0);
        slanPcrFinishMsg.setTotalTime((int) ((finishInfo.getRunningTime() + finishInfo.getRemainTime()) / SLAN_TIME_TO_SEC));
        slanPcrFinishMsg.setDegree(HSUtils.INSTANCE.roundDouble(finishInfo.getTubeTemperature(), 2));
        slanPcrFinishMsg.setInfo("");


        ByteBuffer byteBuffer = BinWssMessageUtils.packetBinMessage(slanPcrFinishMsg, experimentFilePath);
        boolean sendResult = WSSModuleManager.getInstance(context).sendFinishResultWithAck(byteBuffer);
        Log.i(TAG, "发送实验结束的信息和文件 :" + sendResult);
    }

    public void reSendFinishMsg() {
        if (Objects.isNull(finishInfo) || StringUtil.isEmpty(experimentFilePath)) {
            return;
        }
        sendFinishMessage(finishInfo, experimentFilePath);
    }

    private void clearFinishInfoData(){
        //自增 token，让 scheduledExecutor 里残留的 FinishFetchRunnable 全部失效，
        //避免清空 finishInfo 后仍有重试任务继续写入 experimentFilePath
        finishSessionToken.incrementAndGet();
        finishInfo = null;
        experimentFilePath = null;
    }

    /**
     * 删除模板目录及其中的文件
     * @param templateDirPath 模板目录路径（如：/PCR_MODULE_PATH/tempFolder/1234567890/）
     */
    private void deleteTemplateDir(String templateDirPath) {
        if (templateDirPath == null || templateDirPath.isEmpty()) {
            return;
        }

        try {
            File dir = new File(templateDirPath);
            if (!dir.exists()) {
                Log.d(TAG, "模板目录不存在，无需删除: " + templateDirPath);
                return;
            }

            // 递归删除目录及其内容
            boolean deleted = deleteRecursive(dir);
            if (deleted) {
                Log.i(TAG, "成功删除模板目录: " + templateDirPath);
            } else {
                Log.w(TAG, "删除模板目录失败: " + templateDirPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "删除模板目录异常: " + templateDirPath, e);
        }
    }

    /**
     * 递归删除文件或目录
     */
    private boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }

    /**
     * 更新 PCR 模板信息到界面和记录文件
     * @param templateName 模板名称
     * @param projectName 项目文件名（如 first.prj）
     * @param resultPath 结果保存路径
     */
    private void updatePCRTemplateInfo(String templateName, String projectName, String resultPath) {
        try {
            // 1. 通过 EventBus 发送事件更新界面
            PCRTemplateInfoUpdateEvent event = new PCRTemplateInfoUpdateEvent();
            event.setTemplateName(templateName);
            event.setProjectName(projectName);
            event.setResultPath(resultPath);
            event.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            EventBus.getDefault().post(event);
            Log.i(TAG, "已发送模板信息更新事件到界面");

            // 2. 写入记录文件 PCR_Template_Record.md
            writePCRTemplateRecord(templateName, projectName, resultPath);
        } catch (Exception e) {
            Log.e(TAG, "更新 PCR 模板信息失败", e);
        }
    }

    /**
     * 写入 PCR 模板记录文件
     */
    private void writePCRTemplateRecord(String templateName, String projectName, String resultPath) {
        try {
            String recordFilePath = FileUtils.PCR_MODULE_PATH + File.separator + "PCR_Template_Record.md";
            File recordFile = new File(recordFilePath);

            // 确保目录存在
            File parentDir = recordFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 构建记录内容
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            StringBuilder content = new StringBuilder();
            content.append("# PCR 模板执行记录\n\n");
            content.append("**更新时间**: ").append(timestamp).append("\n\n");
            content.append("## 当前执行模板\n\n");
            content.append("- **模板名称**: ").append(templateName).append("\n");
            content.append("- **项目文件**: ").append(projectName).append("\n");
            content.append("- **结果路径**: ").append(resultPath).append("\n\n");
            content.append("---\n\n");
            content.append("*此文件由系统自动生成，每次执行新模板时更新*\n");

            // 写入文件（覆盖模式）
            FileWriter writer = new FileWriter(recordFile, false);
            writer.write(content.toString());
            writer.flush();
            writer.close();

            Log.i(TAG, "已写入 PCR 模板记录文件: " + recordFilePath);
        } catch (IOException e) {
            Log.e(TAG, "写入 PCR 模板记录文件失败", e);
        }
    }

    /**
     * 发布调试状态到 EventBus，用于界面显示
     */
    public void publishDebugStatus() {
        com.gzlab.autoassemblyline_client.event.SLANDebugStatusEvent event =
            new com.gzlab.autoassemblyline_client.event.SLANDebugStatusEvent();

        // 连接状态
        event.isPcrConnected = this.isPcrConnected;
        event.deviceState = pcrDeviceState != null ? pcrDeviceState.name() : "None";

        // 运行状态
        event.isRunning = this.isRunning.get();
        event.status = this.status;
        event.linuxStatus = this.linuxStatus;

        // 当前实验
        event.pcrTemplateName = this.pcrTemplateName;
        event.runningPcrName = this.runningPcrName;

        // 告警信息
        event.alarm = this.alarm;
        event.linuxAlarm = this.linuxAlarm;

        // 错误计数
        event.slanErrorCount = this.slanErrorCount;
        event.reconnectCount = this.reconnectCount;

        // 守卫状态
        event.finishReported = this.finishReported.get();
        event.finishSessionToken = this.finishSessionToken.get();
        event.currentSessionToken = this.currentSessionToken;

        // 文件路径
        event.currentTemplateDirPath = this.currentTemplateDirPath;
        event.experimentFilePath = this.experimentFilePath;

        EventBus.getDefault().post(event);
    }



    // ===================== 结果保存与实体转换 =====================
    public boolean saveResultToPCR(Experiment experiment, String savePath, String saveFileName) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return false;
        }
        if (Objects.isNull(experiment)) {
            return false;
        }
        File dir = new File(savePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        boolean saveResult = PcrApplication.slanService.saveExperiment(experiment.getName(), savePath + saveFileName, false);
        Log.e(TAG, "save_result:" + saveResult);
        return saveResult;
    }

    public com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment changeSLANExToBiowayEx(Experiment experiment) {
        com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment biowayEx = new com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment();
        if (Objects.isNull(experiment)) {
            return biowayEx;
        }
        biowayEx.setName(experiment.getName());
        biowayEx.setStartTime(experiment.getStartTime());
        biowayEx.setEndTime(experiment.getEndTime());
        biowayEx.setOperator(experiment.getOperator());
        biowayEx.setAuditor(experiment.getAuditor());
        biowayEx.setNote(experiment.getNote());
        biowayEx.setPlateNo(experiment.getPlateNo());
        biowayEx.setBatchNo(experiment.getBatchNo());
        biowayEx.setTemperatures(getBioWayTempertures(experiment.getTemperatures()));
        biowayEx.setWellResult(getBiowayWellResults(experiment.getWellResult()));
        return biowayEx;
    }

    private ArrayList<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Temperature> getBioWayTempertures(ArrayList<com.hongshi.slansdk.model.experiment.Temperature> slanTemperatures) {
        ArrayList<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Temperature> biowayTemperatures = new ArrayList<>();
        if (Objects.isNull(slanTemperatures) || slanTemperatures.isEmpty()) {
            return biowayTemperatures;
        }
        // 注意：不要在循环内逐条打印温度。该方法被曲线上报(每30秒)调用，且每次都会
        // 重建整条实验历史的温度数组；逐条打印会随实验时长按 O(n²) 产生海量日志，
        // 把日志文件迅速撑到 20MB 上限，拖垮日志上传并最终导致 OOM。
        for (com.hongshi.slansdk.model.experiment.Temperature temperature : slanTemperatures) {
            biowayTemperatures.add(new com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Temperature(
                    temperature.getBlockTemperature(),
                    temperature.getTubeTemperature(),
                    temperature.getHotlidTemperature(),
                    temperature.getTime()
            ));
        }

        return biowayTemperatures;
    }

    private List<WellResult> getBiowayWellResults(ArrayList<com.hongshi.slansdk.model.result.WellResult> slanWellResults) {
        List<WellResult> biowayWellResults = new ArrayList<>();
        if (Objects.isNull(slanWellResults) || slanWellResults.isEmpty()) {
            return biowayWellResults;
        }
        for (com.hongshi.slansdk.model.result.WellResult wellResult : slanWellResults) {
            WellResult biowayWellResult = new WellResult();
            biowayWellResult.setID(wellResult.getID());
            biowayWellResult.setSampleWellNames(wellResult.getSampleWellNames());
            switch (wellResult.getSampleType()) {
                case 0:
                    biowayWellResult.setSampleType(SampleType.Unknown);
                    break;
                case 1:
                    biowayWellResult.setSampleType(SampleType.Standard);
                    break;
                case 2:
                    biowayWellResult.setSampleType(SampleType.HighPositive);
                    break;
                case 3:
                    biowayWellResult.setSampleType(SampleType.LowPositive);
                    break;
                case 4:
                    biowayWellResult.setSampleType(SampleType.Negative);
                    break;
                case 5:
                    biowayWellResult.setSampleType(SampleType.NTC);
                    break;
                case 6:
                    biowayWellResult.setSampleType(SampleType.QC);
                    break;
                case 7:
                    biowayWellResult.setSampleType(SampleType.RetestSample);
                    break;
                case 8:
                    biowayWellResult.setSampleType(SampleType.Calibrator);
                    break;
                default:
                    biowayWellResult.setSampleType(SampleType.Unknown);
                    break;
            }
            biowayWellResult.setSampleTypeName(wellResult.getSampleTypeName());
            biowayWellResult.setSampleName(wellResult.getSampleName());
            biowayWellResult.setCompoundTubeNo(wellResult.getCompoundTubeNo());
            biowayWellResult.setPatientInfo(getBiowayPatientInfo(wellResult.getPatientInfo()));
            biowayWellResult.setTargetResults(getBiowayTargetResults(wellResult.getTargetResults()));
            biowayWellResult.setSampleConclusion(wellResult.getSampleConclusion());
            biowayWellResult.setWellConclusion(wellResult.getWellConclusion());
            biowayWellResult.setWellMidValue(null);
            biowayWellResults.add(biowayWellResult);
        }
        return biowayWellResults;
    }

    private PatientInfo getBiowayPatientInfo(com.hongshi.slansdk.model.result.PatientInfo slanPatientInfo) {
        PatientInfo biowayPatientInfo = new PatientInfo();
        if (Objects.isNull(slanPatientInfo)) {
            return biowayPatientInfo;
        }
        biowayPatientInfo.setID(slanPatientInfo.getID());
        biowayPatientInfo.setName(slanPatientInfo.getName());
        biowayPatientInfo.setSex(slanPatientInfo.getSex());
        biowayPatientInfo.setAge(slanPatientInfo.getAge());
        biowayPatientInfo.setSpecimen(slanPatientInfo.getSpecimen());
        biowayPatientInfo.setSpecimenDate(slanPatientInfo.getSpecimenDate());
        biowayPatientInfo.setDoctor(slanPatientInfo.getDoctor());
        biowayPatientInfo.setOffice(slanPatientInfo.getOffice());
        biowayPatientInfo.setDiagnosis(slanPatientInfo.getDiagnosis());
        biowayPatientInfo.setCaseID(slanPatientInfo.getCaseID());
        biowayPatientInfo.setBedID(slanPatientInfo.getBedId());
        biowayPatientInfo.setInPatientID(slanPatientInfo.getInPatientID());
        biowayPatientInfo.setOutPatientID(slanPatientInfo.getOutPatientID());
        biowayPatientInfo.setRemark(slanPatientInfo.getRemark());
        biowayPatientInfo.setCustomData(null);
        return biowayPatientInfo;
    }

    private ArrayList<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.TargetResult> getBiowayTargetResults(ArrayList<com.hongshi.slansdk.model.result.TargetResult> slanTargetResults) {
        ArrayList<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.TargetResult> biowayTargetResults = new ArrayList<>();
        if (Objects.isNull(slanTargetResults) || slanTargetResults.isEmpty()) {
            return biowayTargetResults;
        }
        for (com.hongshi.slansdk.model.result.TargetResult targetResult : slanTargetResults) {
            com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.TargetResult biowayTargetResult = new com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.TargetResult();
            biowayTargetResult.setChannel(targetResult.getChannel());
            biowayTargetResult.setName(targetResult.getName());
            biowayTargetResult.setCt(targetResult.getCt());
            biowayTargetResult.setCtMean(targetResult.getCtMean());
            biowayTargetResult.setCtSD(targetResult.getCtSD());
            biowayTargetResult.setCtCV(targetResult.getCtCV());
            biowayTargetResult.setCn(targetResult.getCnValue());
            biowayTargetResult.setCnMean(targetResult.getCnMean());
            biowayTargetResult.setCnSD(targetResult.getCnSD());
            biowayTargetResult.setCnCV(targetResult.getCnCV());
            biowayTargetResult.setRn(targetResult.getRnValue());

            biowayTargetResult.setRawCurve(getBiowayRawCurve(targetResult.getRawCurve()));
            biowayTargetResult.setAmpCurve(getBiowayAMPCurve(targetResult.getAMPCurve()));
            biowayTargetResult.setRawMeltCurve(getBiowayMeltCurve(targetResult.getRawMeltCurve()));
            biowayTargetResult.setPeakCurve(getBiowayPeakCurve(targetResult.getPeakCurve()));
            biowayTargetResult.setPeakValues(getBiowayPeakVulue(targetResult.getPeakValues()));

            biowayTargetResult.setTubeName(targetResult.getTubeName());
            biowayTargetResult.setTubeNo(targetResult.getTubeNo());
            biowayTargetResult.setDye(targetResult.getDye());
            biowayTargetResult.setProperty(targetResult.getProperty());
            biowayTargetResult.setConclusionList(null);
            biowayTargetResults.add(biowayTargetResult);
        }
        return biowayTargetResults;
    }

    private List<Double> getBiowayRawCurve(List<Dot> rawCurve) {
        List<Double> biowayRawCurve = new ArrayList<>();
        if (Objects.isNull(rawCurve) || rawCurve.isEmpty()) {
            return biowayRawCurve;
        }
        for (Dot dot : rawCurve) {
            biowayRawCurve.add(dot.getY());
        }
        return biowayRawCurve;
    }

    private List<Double> getBiowayAMPCurve(List<Dot> ampCurve) {
        List<Double> biowayAMPCurve = new ArrayList<>();
        if (Objects.isNull(ampCurve) || ampCurve.isEmpty()) {
            return biowayAMPCurve;
        }
        for (Dot dot : ampCurve) {
            biowayAMPCurve.add(dot.getY());
        }
        return biowayAMPCurve;
    }

    private List<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Dot> getBiowayPeakCurve(List<Dot> peakCurve) {
        List<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Dot> biowayPeakCurve = new ArrayList<>();
        if (Objects.isNull(peakCurve) || peakCurve.isEmpty()) {
            return biowayPeakCurve;
        }
        for (Dot dot : peakCurve) {
            com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Dot biowayDot = new com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Dot();
            biowayDot.setX(dot.getX());
            biowayDot.setY(dot.getY());
            biowayPeakCurve.add(biowayDot);
        }
        return biowayPeakCurve;
    }

    private List<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Dot> getBiowayMeltCurve(List<Dot> meltCurve) {
        List<com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Dot> biowayMeltCurve = new ArrayList<>();
        if (Objects.isNull(meltCurve) || meltCurve.isEmpty()) {
            return biowayMeltCurve;
        }
        for (Dot dot : meltCurve) {
            com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Dot biowayDot = new com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Dot();
            biowayDot.setX(dot.getX());
            biowayDot.setY(dot.getY());
            biowayMeltCurve.add(biowayDot);
        }
        return biowayMeltCurve;
    }

    private List<Peak> getBiowayPeakVulue(ArrayList<MeltPeak> slanPeakValues) {
        List<Peak> biowayPeakValues = new ArrayList<>();
        if (Objects.isNull(slanPeakValues) || slanPeakValues.isEmpty()) {
            return biowayPeakValues;
        }
        for (MeltPeak meltPeak : slanPeakValues) {
            Peak biowayPeak = new Peak();
            biowayPeak.setRm(meltPeak.getRm());
            biowayPeak.setTm(meltPeak.getTm());
            biowayPeak.setWm(meltPeak.getWidth());
            biowayPeak.setYm(meltPeak.getYValue());
            biowayPeakValues.add(biowayPeak);
        }
        return biowayPeakValues;
    }

    // ===================== 连接检测与重连 =====================

    private class ConnectCheckRunnable implements Runnable {
        @Override
        public void run() {
            try {
                checkPcrConnection();
            } catch (Exception e) {
                Log.e(TAG, "连接检测任务异常", e);
            }
        }
    }



    // ===================== 生命周期销毁 =====================
    public void destroy() {
        // 关闭定时线程池
        scheduledExecutor.shutdownNow();
        // 清除SDK监听器，防止内存泄漏
        SLANService slanService = SLANServiceManager.getInstance(context).getSlanService();
        if (Objects.nonNull(slanService)) {
            slanService.setDeviceStateListener(null);
            slanService.setRunningInfoListener(null);
        }
        mExTemperature.clear();
        mExTemperature = null;
        mExperiment = null;
        pcrRunningInfo = null;
        finishInfo = null;
        context = null;
    }

    // ===================== Getter & Setter =====================
    public String getLinuxAlarm() {
        return linuxAlarm;
    }

    public void setLinuxAlarm(String linuxAlarm) {
        this.linuxAlarm = linuxAlarm;
    }

    public String getAlarm() {
        return alarm;
    }

    public void setAlarm(String alarm) {
        this.alarm = alarm;
    }

    public void setLinuxAlarmAndStatus(String linuxAlarm, String linuxStatus) {
        this.linuxStatus = linuxStatus;
        this.linuxAlarm = linuxAlarm;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setExperiment(Experiment mExperiment) {
        this.mExperiment = mExperiment;
    }

    public Experiment getExperiment() {
        return mExperiment;
    }

    public boolean installSclientApk(File apkFile) {
        return false;
    }

    public boolean installUpdateApk() {
        return true;
    }
}