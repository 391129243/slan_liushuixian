package com.paul.pipeline.manager;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipeDoorModel;
import com.paul.pipeline.listener.ArmErrorCallback;
import com.paul.pipeline.listener.ArmWorkListener;
import com.paul.pipeline.listener.BackTrayCallback;
import com.paul.pipeline.listener.DeviceWorkFinishListenser;
import com.paul.pipeline.listener.UIinitCallback;
import com.paul.pipeline.message.ResultMessage;
import com.paul.pipeline.utils.DataTimeUtil;
import com.paul.pipeline.data.LocalFileHelper;
import com.paul.pipeline.listener.ArmFinishCallback;
import com.paul.pipeline.message.ArmworkMessage;
import com.paul.pipeline.model.ArmStep;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.model.PcrSchedulingState;
import com.paul.pipeline.model.RnaSchedulingState;
import com.paul.pipeline.model.TemplateModel;
import com.paul.pipeline.model.TrayDispatchSnapshot;
import com.paul.pipeline.scheduling.SchedulingEngine;
import com.paul.pipeline.scheduling.ArmCallbackEngine;
import com.paul.pipeline.scheduling.ArmCallbackPort;
import com.paul.pipeline.model.ArmItemSnapshot;
import com.paul.pipeline.scheduling.SchedulingPort;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.fragment.model.PipeRNAModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.gidi.bio_pcr.util.ModuleEnableManager;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_QRCode;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.worker.RobotWorkManager;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.HeartBeatMessage;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//调度总管理
public class APLManager {
    private static final String TAG = "APLManager";
    private static APLManager mInstance;

    public static APLManager getInstance() {
        if (null == mInstance) {
            synchronized (APLManager.class) {
                if (null == mInstance) {
                    mInstance = new APLManager();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context) {
        initTemplateList();
        initDoorModel();
        TrayManager.getInstance().init(context);
        ArmManager.getInstance().init(context);
        RNAManager.getInstance().init(context);
        PCRManager.getInstance().init(context);
    }

    public void clearData(Context context) {
        TrayManager.getInstance().clearData(context);
        ArmManager.getInstance().clearData(context);
        RNAManager.getInstance().clearData(context);
        PCRManager.getInstance().clearData(context);
        refreshAll();
    }

    /**
     * 一键清除数据（保留样本条码/编号）。
     * 清除托盘的历史批次、项目、调度目标位置等信息；
     * 保留各孔的样本号（serialNum）和条码（reagentCode）。
     * ARM/RNA/PCR 设备状态仍完整清除（与 clearData 一致）。
     */
    public void clearDataKeepSamples(Context context) {
        TrayManager.getInstance().clearDataKeepSamples(context);
        ArmManager.getInstance().clearData(context);
        RNAManager.getInstance().clearData(context);
        PCRManager.getInstance().clearData(context);
        refreshAll();
    }

    public void refreshAll(){
        if (uIinitCallback != null) {
            uIinitCallback.finish(0, 0);
        }
    }
    public void clearTray(int tray){
        if (uIinitCallback != null) {
            uIinitCallback.finish(ConstantData.DEVICE_TRAY, tray);
        }
    }
    PipeDoorModel pipeDoorModel;

    public void initDoorModel() {
        pipeDoorModel = new PipeDoorModel();
        pipeDoorModel.armhold.set(false);
        pipeDoorModel.doorhold.set(false);
        pipeDoorModel.doorlocked.set(false);
        pipeDoorModel.doorposition.set(false);
    }

    public PipeDoorModel getPipeDoorModel() {
        return pipeDoorModel;
    }

    public void setPipeDoorModel(PipeDoorModel pipeDoorModel) {
        this.pipeDoorModel = pipeDoorModel;
    }

    public ArrayList<String> tempListfull;
    public ArrayList<String> tempListrna;
    public ArrayList<String> tempListpcr;

    public void initTemplateList() {
        tempListfull = LocalFileHelper.getFullTemplateList();
        tempListrna = LocalFileHelper.getRNATemplateList();
        tempListpcr = LocalFileHelper.getPCRTemplateList();
    }

    public boolean WSSStart = false;//启动是否成功
    public boolean SelfCheck = false;//启动是否成功

    public boolean startWSSserver() {
        Log.i(TAG, "startWSSserver");
        boolean result = WSSModuleManager.getInstance().startWSSserver();
        return result;
    }

    public void stopWSSserver() {
        WSSModuleManager.getInstance().stopWSSserver();
    }

    public String getKey(int type, int index) {
        ConcurrentHashMap<String, WSSConnectionItem> items = WSSModuleManager.getInstance().getWssConnectionItems();
        String typestr = "";
        switch (type) {
            case ConstantData.DEVICE_RNA:
                typestr = ConstantData.nameRNA;
                break;
            case ConstantData.DEVICE_PCR:
                typestr = ConstantData.namePCR;
                break;
        }
        if (typestr != null && !typestr.isEmpty() && items != null) {
            for (String key : items.keySet()) {
                WSSConnectionItem item = items.get(key);
                if (item.getDeviceType() != null
                        && item.getDeviceType().equalsIgnoreCase(typestr)
                        && item.getPosition() == index) {
                    switch (type) {
                        case ConstantData.DEVICE_RNA:
                            typestr = ConstantData.nameRNA;
                            if (item instanceof RNAStatusItem) {
                                return key;
//                                    model.deviceIP.set(item.getDeviceIP());
//                                    model.devicePort.set(item.getDevicePort());
                            }
                            break;
                        case ConstantData.DEVICE_PCR:
                            typestr = ConstantData.namePCR;
                            if (item instanceof PCRStatusItem) {
                                return key;
//                                    model.deviceIP.set(item.getDeviceIP());
//                                    model.devicePort.set(item.getDevicePort());
                            }
                            break;
                    }
                }
            }
        }
        return null;
    }

    public void refreshConnectinfo() {
        refreshDoor();
        ArmManager.getInstance().refreshConnectinfo();
        TrayManager.getInstance().refreshConnectinfo();
        if (WSSStart) {
            ConcurrentHashMap<String, WSSConnectionItem> datas = WSSModuleManager.getInstance().getWssConnectionItems();
            PCRManager.getInstance().refreshConnectinfo(datas);
            RNAManager.getInstance().refreshConnectinfo(datas);
        }
    }

    private void refreshDoor() {
        SerialPortManager_Robot.RobotStatus status = SerialPortManager_Robot.getInstance().getRobotStatus();
        if (status != null) {
            getPipeDoorModel().doorposition.set(status.getSenseByKey("DOOR"));
            getPipeDoorModel().armhold.set(ArmManager.getInstance().armCanuse());
        }
    }

    public ResultMessage openDoor(Context context) {
        getPipeDoorModel().doorhold.set(true);
        ResultMessage resultMessage = new ResultMessage();
        if (ArmManager.getInstance().armCanuse()) {
            if (doorOpen()) {
                if (getPipeDoorModel().doorposition.get()) {
                    resultMessage.setResult(true);
                    resultMessage.setMessage(context.getString(R.string.pipe_main_traystatus_done) + "调度已停止，门已解锁，请手动拉开滑轨门！");
                } else {
                    resultMessage.setResult(true);
                    resultMessage.setMessage(context.getString(R.string.do_wait) + "调度已停止，门已解锁，托盘处于拉开状态！");
                }
            } else {
                resultMessage.setResult(false);
                resultMessage.setMessage("调度已停止，解锁失败，请重试！");
            }
        } else {
            resultMessage.setResult(false);
            resultMessage.setMessage(context.getString(R.string.toast_open_door_arm_busy));
        }
        return resultMessage;
    }

    public ResultMessage closeDoor(Context context) {
        getPipeDoorModel().doorhold.set(false);
        ResultMessage resultMessage = new ResultMessage();
        if (getPipeDoorModel().doorposition.get() && getPipeDoorModel().doorlocked.get()) {
            boolean result = SerialPortManager_Robot.getInstance().operateDoor(0);
            if (result) {
                resultMessage.setResult(true);
                resultMessage.setMessage("已处于关闭状态！");
            } else {
                resultMessage.setResult(false);
                resultMessage.setMessage("门上锁失败，请重试！");
            }
        } else {
            if (ArmManager.getInstance().armCanuse()) {
                if (getPipeDoorModel().doorposition.get()) {
                    if (doorClose()) {
                        resultMessage.setResult(true);
                        resultMessage.setMessage("门已上锁！");
                    } else {
                        resultMessage.setResult(false);
                        resultMessage.setMessage("门上锁失败，请重试！");
                    }
                } else {
                    resultMessage.setResult(false);
                    resultMessage.setMessage("门上锁失败，请确认托盘架是否推入到位！");
                }
            } else {
                resultMessage.setResult(false);
                resultMessage.setMessage("禁止操作！机械臂正在运行,请等待机械臂停止后操作！");
            }
        }
        return resultMessage;
    }

    public boolean doorOpen() {
        boolean result = SerialPortManager_Robot.getInstance().operateDoor(1);
        if (result) {
            getPipeDoorModel().doorlocked.set(false);
        }
        return result;
    }

    public boolean doorClose() {
        boolean result = SerialPortManager_Robot.getInstance().operateDoor(0);
        if (result) {
            getPipeDoorModel().doorlocked.set(true);
        }
        return result;
    }

    UIinitCallback uIinitCallback;

    /**
     * 核提 PCR界面初始化
     * @param uIinitCallback
     */
    public void setuIinitCallback(UIinitCallback uIinitCallback) {
        this.uIinitCallback = uIinitCallback;
    }

    //检查机械臂状态，空闲时立即执行，无空闲则等待
    public void checkArmTask() {
        try {
            checkrnaRun();
            checkpcrRun();
            checktrayend();
            //TODO fordebugtest
//            if (true) {//开门事件优先级最高
            if (!getPipeDoorModel().doorhold.get() && !ArmManager.getInstance().isDoorOpen()) {//开门事件优先级最高
                if (WSSStart) {
                    //TODO fordebugtest
//                    if (true) {//开门事件优先级最高
                    if (ArmManager.getInstance().armCanuse()) {
                        if(!checktrayerror()) {
                            boolean trayb = checktrayurgent();
                            if (trayb) {
                                Log.i("ArmTaskWorker", "机械臂无任务，等待调度");
                            } else {
                                Log.w("ArmTaskWorker", "托盘调度,机械臂任务中");
                            }
                        }else {
                            Log.w("ArmTaskWorker", "托盘异常调度,机械臂执行任务中");
                        }
                    } else {
                        Log.i("ArmTaskWorker", "机械臂不可用，不调度");
                    }
                } else {
                    Log.i("ArmTaskWorker", "wss资源为0，不调度");
                }
            } else {
                Log.i("ArmTaskWorker", "已开门，不调度");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void checkpcrRun() {
        if (WSSStart&&SelfCheck) {
            List<PipePCRModel> list = PCRManager.getInstance().getPcrlist();
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    PipePCRModel m = list.get(i);
                    // 检查模组是否启用
                    String deviceId = m.deviceID.get();
                    boolean isEnabled = (deviceId == null || deviceId.isEmpty())
                        ? true
                        : ModuleEnableManager.getInstance().isModuleEnabled(deviceId);
                    if (!isEnabled) {
                        continue; // 跳过未启用的模组
                    }
                    if ((m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                            && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE))) {
                        ArmStep step = m.armStep.get();
                        if (step == ArmStep.RUN) {
                            if (Boolean.FALSE.equals(m.runtime.isExcutestart())) {
                                if (m.currentTray.get() != null) {
                                    DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                    if (dispatchModel == null) {
                                        Log.e("ArmTaskWorker", "checkpcrRun 找不到调度数据 pcr:" + m.index.get() + " tray:" + m.currentTray.get());
                                    } else {
                                        boolean result = startRunTemplate(m.index.get(), ConstantData.DEVICE_PCR,
                                                m.currentTray.get(), dispatchModel.index.get());
                                        if (result) {
                                            m.runtime.setExcutestart(true);
                                            m.runtime.setExcutefinish(false);
                                            Log.w("ArmTaskWorker", "startRunTemplate 成功 pcr:" + m.index.get());
                                        } else {
                                            Log.e("ArmTaskWorker", "startRunTemplate 失败 pcr:" + m.index.get());
                                        }
                                    }
                                }else{
                                    Log.e("ArmTaskWorker", "checkpcrRun 调度托盘数据null pcr:" + m.index.get());
                                }
                            }
                        }
                    }
                }
            }
        }else{
            Log.w("ArmTaskWorker", "checkpcrRun WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
        }
    }

    private void checkrnaRun() {
        if (WSSStart&&SelfCheck) {
            List<PipeRNAModel> list = RNAManager.getInstance().getRnalist();
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    PipeRNAModel m = list.get(i);
                    // 检查模组是否启用
                    String deviceId = m.deviceID.get();
                    boolean isEnabled = (deviceId == null || deviceId.isEmpty())
                        ? true
                        : ModuleEnableManager.getInstance().isModuleEnabled(deviceId);
                    if (!isEnabled) {
                        continue; // 跳过未启用的模组
                    }
                    if ((m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                            && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE))) {
                        ArmStep step = m.armStep.get();
                        if (step == ArmStep.RUN) {
                            if (Boolean.FALSE.equals(m.runtime.isExcutestart())) {
                                if (m.currentTray.get() != null) {
                                    DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                    if(dispatchModel==null) {
                                        Log.e("ArmTaskWorker", "checkrnaRun 找不到调度数据 rna:" + m.index.get() +" tray:"+m.currentTray.get());
                                    } else {
                                        boolean result = startRunTemplate(m.index.get(), ConstantData.DEVICE_RNA, m.currentTray.get(),
                                                dispatchModel.index.get());
                                        if (result) {
                                            m.runtime.setExcutestart(true);
                                            m.runtime.setExcutefinish(false);
                                            Log.w("ArmTaskWorker", "startRunTemplate 成功 rna:" + m.index.get());
                                        } else {
                                            Log.e("ArmTaskWorker", "startRunTemplate 失败 rna:" + m.index.get());
                                        }
                                    }
                                }else{
                                    Log.e("ArmTaskWorker", "checkrnaRun 调度托盘数据null rna:" + m.index.get());
                                }
                            }
                        }
                    }
                }
            }
        }else{
            Log.w("ArmTaskWorker", "checkrnaRun WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
        }
    }

    private boolean checkrnaclosedoor(int index) {
        PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(index);
        if(m!=null) {
            if(Boolean.TRUE.equals(m.runtime.isClosedoor()) && !RNAManager.getInstance().isRnaUsable(index)){
                // 未连接/已禁用的核提无法执行关门，也不会去开门：清掉标记跳过，避免占住机械臂
                m.runtime.setClosedoor(false);
                Log.w("ArmTaskWorker", "rna: " + index + " 未连接或已禁用，跳过关门");
                return true;
            }
            if(Boolean.TRUE.equals(m.runtime.isClosedoor())){
                boolean result = ArmManager.getInstance().closeRNADoor(index, new ArmFinishCallback() {
                    @Override
                    public void finish(int arm, ItemProperty aim, ItemProperty source) {

                    }

                    @Override
                    public void fail(int arm, ItemProperty aim, ItemProperty source, String error) {

                    }
                });
                if(result){
                    m.runtime.setClosedoor(false);
                    return false;
                }
            }
        }
        return true;
    }

    ///处理核提调度
    ///第 3 步重构：决策逻辑已抽到 SchedulingEngine（不依赖 databinding），
    ///本方法退化为"建快照 → 委托引擎 → 回写"。行为与原实现逐行等价。
    private boolean checkrnawork(int index, int tray) {
        RnaSchedulingState s = schedulingPort.snapshotRna(index);
        if (s == null) {
            Log.e("ArmTaskWorker", "checkrnawork model数据为空 rna:" + index + " tray:" + tray);
            return true;
        }
        // 检查模组是否启用
        if (!s.isEnabled) {
            return true; // 未启用的模组返回idle
        }
        if (s.currentTray != tray) {
            Log.e("ArmTaskWorker", "checkrnawork 托盘数据不一致 rna :" + index + " rnatray" + s.currentTray + " tray:" + tray);
            return true;
        }
        boolean idle = schedulingEngine.stepRna(s, tray);
        schedulingPort.applyRna(index, s);
        return idle;
    }

    ///处理PCR调度
    ///第 3 步重构：决策逻辑已抽到 SchedulingEngine.stepPcr（不依赖 databinding），
    ///本方法退化为"建快照 → 委托引擎 → 回写"。行为与原实现逐行等价。
    private boolean checkpcrwork(int index, int tray) {
        PcrSchedulingState s = schedulingPort.snapshotPcr(index);
        if (s == null) {
            Log.e("ArmTaskWorker", "checkpcrwork model数据为空 pcr:" + index + " tray:" + tray);
            return true;
        }
        // 检查模组是否启用
        if (!s.isEnabled) {
            return true; // 未启用的模组返回idle
        }
        if (s.currentTrayKnown && s.currentTray != tray) {
            Log.e("ArmTaskWorker", "checkpcrwork 托盘数据不一致 pcr:" + index + " pcrtray" + s.currentTray + " tray:" + tray);
            return true;
        }
        boolean idle = schedulingEngine.stepPcr(s, tray);
        schedulingPort.applyPcr(index, s);
        return idle;
    }

    ///第 4 步重构：编排逻辑已抽到 SchedulingEngine（databinding-free），此处退化为委托。
    private void checktrayend() {
        schedulingEngine.checkTrayEnd();
    }
    private boolean checktrayerror() {
        return schedulingEngine.checkTrayError();
    }
    private boolean checktrayurgent() {
        return schedulingEngine.checkTrayUrgent();
    }

    /***
     * 处理异常，取回托盘
     * @param trayindex
     * @return
     */
    private boolean checktrayerror(int trayindex) {
        return schedulingEngine.checkTrayError(trayindex);
    }

    /**
     * 分配盘位和调度机械臂
     *
     * @param trayindex
     * @return
     */
    private boolean checktrayRnawork(int trayindex) {
        return schedulingEngine.checkTrayRnawork(trayindex);
    }
    /**
     * 分配盘位和调度机械臂
     *
     * @param trayindex
     * @return
     */
    private boolean checktrayPcrwork(int trayindex) {
        return schedulingEngine.checkTrayPcrwork(trayindex);
    }

    ArmWorkListener armWorkListener;

    public void setArmWorkListener(ArmWorkListener armWorkListener) {
        this.armWorkListener = armWorkListener;
    }

    /** 释放设备调度占用：armStep 置 FREE 并触发界面初始化。供停止时释放尚未取盘的设备。 */
    public void markDeviceScheduleDone(int deviceType, int deviceIndex) {
        schedulingPort.markDeviceScheduleDone(deviceType, deviceIndex);
    }

    //第 3 步：RNA/PCR 调度引擎与其端口适配（适配到现有 Manager，行为不变）
    private final SchedulingPort schedulingPort = new AplSchedulingPort();
    private final SchedulingEngine schedulingEngine = new SchedulingEngine(schedulingPort);

    private class AplSchedulingPort implements SchedulingPort {
        /** 是否处于开门保持态（doorhold） */
        @Override
        public boolean isDoorHold() {
            return getPipeDoorModel().doorhold.get();
        }

        /** WSS 资源是否就绪 */
        @Override
        public boolean isWssStart() {
            return WSSStart;
        }

        /** 自检是否完成 */
        @Override
        public boolean isSelfCheck() {
            return SelfCheck;
        }

        /** 机械臂是否可用（ArmManager.armCanuse） */
        @Override
        public boolean isArmCanuse() {
            return ArmManager.getInstance().armCanuse();
        }

        /** 当前托盘的调度序号；getCurrentDispatch 为 null 返回 -1 */
        @Override
        public int currentDispatchIndex(int currentTray) {
            DispatchModel d = TrayManager.getInstance().getCurrentDispatch(currentTray);
            return d == null ? -1 : d.index.get();
        }

        /** 空闲 PCR 序号；无空闲返回 -1 */
        @Override
        public int findFreePcrIndex() {
            PipePCRModel rm = PCRManager.getInstance().getFreeModel();
            return rm == null ? -1 : rm.index.get();
        }

        /** 空闲 RNA 序号；无空闲返回 -1（供 PCR MOVE 用） */
        @Override
        public int findFreeRnaIndex() {
            PipeRNAModel rm = RNAManager.getInstance().getFreeModel();
            return rm == null ? -1 : rm.index.get();
        }

        /** RNA 界面初始化：usererror 清零 + uIinitCallback.finish(DEVICE_RNA) + 日志 */
        @Override
        public void fireUiInit(int rnaIndex) {
            PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(rnaIndex);
            if (m != null) {
                m.usererror.set(0);
            }
            if (uIinitCallback != null) {
                uIinitCallback.finish(ConstantData.DEVICE_RNA, rnaIndex);
                Log.w("ArmTaskWorker", "界面初始化 成功 rna:" + rnaIndex);
            }
            Log.i("ArmTaskWorker", "界面初始化 rna " + rnaIndex);
        }

        /** 转发：启动 RNA 模板运行（startRunTemplate, DEVICE_RNA） */
        @Override
        public boolean startRunTemplateRna(int rnaIndex, int tray, int dispatchIndex) {
            return startRunTemplate(rnaIndex, ConstantData.DEVICE_RNA, tray, dispatchIndex);
        }

        /** 转发：依据当前托盘解析调度并取盘（createArmTaskGetTray） */
        @Override
        public boolean createGetTray(int currentTray) {
            DispatchModel d = TrayManager.getInstance().getCurrentDispatch(currentTray);
            return d != null && createArmTaskGetTray(d);
        }

        /** 转发：RNA→PCR 移盘（createArmMoveTray） */
        @Override
        public boolean createMoveTray(int rnaIndex, int pcrIndex, int tray, int dispatchIndex) {
            return createArmMoveTray(rnaIndex, ConstantData.DEVICE_RNA, pcrIndex, ConstantData.DEVICE_PCR, tray, dispatchIndex);
        }

        /** 转发：调度项设备状态置完成（updateDsiapatchDeviceStatus=FINISH） */
        @Override
        public void markDispatchDeviceFinish(int tray, int dispatchIndex) {
            TrayManager.getInstance().updateDsiapatchDeviceStatus(tray, dispatchIndex, ConstantData.STATUS_FINISH);
        }

        /** 转发：RNA 送回托盘（createArmBackTray） */
        @Override
        public boolean createBackTray(int rnaIndex, int tray, int dispatchIndex) {
            return createArmBackTray(rnaIndex, ConstantData.DEVICE_RNA, tray, dispatchIndex);
        }

        /** 从 PipeRNAModel 预计算构建 RNA 调度快照；model 为 null 返回 null */
        @Override
        public RnaSchedulingState snapshotRna(int rnaIndex) {
            PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(rnaIndex);
            if (m == null) {
                return null;
            }
            // 同步块保护快照创建过程,确保读取的字段一致性
            synchronized (m) {
                RnaSchedulingState s = new RnaSchedulingState();
                s.index = m.index.get();
                s.currentTray = m.currentTray.get() == null ? 0 : m.currentTray.get();
                s.statusFree = m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                        && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE);
                s.exist = Boolean.TRUE.equals(m.exist.get());
                s.armStep = m.armStep.get();
                s.runtime = m.runtime;
                String deviceId = m.deviceID.get();
                s.isEnabled = (deviceId != null && !deviceId.isEmpty())
                    ? ModuleEnableManager.getInstance().isModuleEnabled(deviceId)
                    : true;
                return s;
            }
        }

        /** 把引擎改动（当前仅 armStep）回写到 PipeRNAModel */
        @Override
        public void applyRna(int rnaIndex, RnaSchedulingState state) {
            PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(rnaIndex);
            if (m != null && state != null && m.armStep.get() != state.armStep) {
                m.armStep.set(state.armStep);
            }
        }

        /** PCR 界面初始化：usererror 清零 + uIinitCallback.finish(DEVICE_PCR) + 日志 */
        @Override
        public void fireUiInitPcr(int pcrIndex) {
            PipePCRModel m = PCRManager.getInstance().getPipePCRModel(pcrIndex);
            if (m != null) {
                m.usererror.set(0);
            }
            if (uIinitCallback != null) {
                uIinitCallback.finish(ConstantData.DEVICE_PCR, pcrIndex);
                Log.w("ArmTaskWorker", "界面初始化成功 pcr:" + pcrIndex);
            }
            Log.i("ArmTaskWorker", "界面初始化 pcr " + pcrIndex);
        }

        /** 转发：启动 PCR 模板运行（startRunTemplate, DEVICE_PCR） */
        @Override
        public boolean startRunTemplatePcr(int pcrIndex, int tray, int dispatchIndex) {
            return startRunTemplate(pcrIndex, ConstantData.DEVICE_PCR, tray, dispatchIndex);
        }

        /** 转发：PCR→RNA 移盘（createArmMoveTray） */
        @Override
        public boolean createMovePcr(int pcrIndex, int rnaIndex, int tray, int dispatchIndex) {
            return createArmMoveTray(pcrIndex, ConstantData.DEVICE_PCR, rnaIndex, ConstantData.DEVICE_RNA, tray, dispatchIndex);
        }

        /** 转发：PCR 送回托盘（createArmBackTray） */
        @Override
        public boolean createBackPcr(int pcrIndex, int tray, int dispatchIndex) {
            return createArmBackTray(pcrIndex, ConstantData.DEVICE_PCR, tray, dispatchIndex);
        }

        /** 从 PipePCRModel 预计算构建 PCR 调度快照；model 为 null 返回 null */
        @Override
        public PcrSchedulingState snapshotPcr(int pcrIndex) {
            PipePCRModel m = PCRManager.getInstance().getPipePCRModel(pcrIndex);
            if (m == null) {
                return null;
            }
            // 同步块保护快照创建过程,确保读取的字段一致性
            synchronized (m) {
                PcrSchedulingState s = new PcrSchedulingState();
                s.index = m.index.get();
                s.currentTrayKnown = m.currentTray.get() != null;
                s.currentTray = m.currentTray.get() == null ? 0 : m.currentTray.get();
                s.statusFree = m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                        && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE);
                s.exist = Boolean.TRUE.equals(m.exist.get());
                s.armStep = m.armStep.get();
                s.runtime = m.runtime;
                String deviceId = m.deviceID.get();
                s.isEnabled = (deviceId != null && !deviceId.isEmpty())
                    ? ModuleEnableManager.getInstance().isModuleEnabled(deviceId)
                    : true;
                return s;
            }
        }

        /** 把引擎改动（当前仅 armStep）回写到 PipePCRModel */
        @Override
        public void applyPcr(int pcrIndex, PcrSchedulingState state) {
            PipePCRModel m = PCRManager.getInstance().getPipePCRModel(pcrIndex);
            if (m != null && state != null && m.armStep.get() != state.armStep) {
                m.armStep.set(state.armStep);
            }
        }

        // ===== CONTINUATION_MARKER_APL_1 =====

        // ===== 第 4 步：tray 编排适配 =====

        /** 取托盘列表（供本类各遍历方法复用） */
        private ArrayList<PipeTrayModel> trays() {
            return TrayManager.getInstance().getTraylist();
        }

        /** 把 DispatchModel 的 ObservableField 预计算成引擎用的纯 POJO 快照（含空值位） */
        private TrayDispatchSnapshot toSnapshot(DispatchModel dm) {
            if (dm == null) {
                return null;
            }
            // 同步块保护快照创建过程，确保读取的字段一致性
            synchronized (dm) {
                TrayDispatchSnapshot s = new TrayDispatchSnapshot();
                s.dispatchIndex = dm.index.get() == null ? 0 : dm.index.get();
                s.statusKnown = dm.status.get() != null;
                s.status = dm.status.get() == null ? ConstantData.STATUS_FREE : dm.status.get();
                s.targettypeKnown = dm.targettype.get() != null;
                s.targettype = dm.targettype.get() == null ? 0 : dm.targettype.get();
                s.targetKnown = dm.target.get() != null;
                s.target = dm.target.get() == null ? 0 : dm.target.get();
                s.devicestatusKnown = dm.devicestatus.get() != null;
                s.devicestatus = dm.devicestatus.get() == null ? 0 : dm.devicestatus.get();
                return s;
            }
        }

        /** 托盘总数 */
        @Override
        public int trayCount() {
            return trays().size();
        }

        /** 列表下标 i 处托盘的 index */
        @Override
        public int trayIndexAt(int i) {
            return trays().get(i).index.get();
        }

        /** 列表下标 i 处托盘及其 index 是否均非空 */
        @Override
        public boolean trayIndexKnownAt(int i) {
            PipeTrayModel t = trays().get(i);
            return t != null && t.index.get() != null;
        }

        /** 列表下标 i 处列表项是否非空 */
        @Override
        public boolean trayExistsAt(int i) {
            return trays().get(i) != null;
        }

        /** 列表下标 i 处托盘是否加急 */
        @Override
        public boolean trayUrgentAt(int i) {
            PipeTrayModel t = trays().get(i);
            return t != null && t.urgent.get() != null && Boolean.TRUE.equals(t.urgent.get());
        }

        /** 托盘状态是否等于给定值（忽略大小写） */
        @Override
        public boolean trayStatusIs(int trayIndex, String status) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            return t != null && t.status.get() != null && t.status.get().equalsIgnoreCase(status);
        }

        /** 托盘状态是否非空 */
        @Override
        public boolean trayHasStatus(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            return t != null && t.status.get() != null;
        }

        /** 托盘当前调度序号（原为拆箱读取、字段恒有值） */
        @Override
        public int trayDispatchIndex(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            return t.dispatchindex.get(); // 原为拆箱读取，字段恒有值
        }

        /** 调度表大小；dispatchlist==null 返回 -1 */
        @Override
        public int trayDispatchSize(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            if (t == null || t.dispatchlist == null) {
                return -1;
            }
            return t.dispatchlist.size();
        }

        /** 托盘存在传感器是否为真（写回守卫用） */
        @Override
        public boolean trayExistFlag(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            return t != null && t.exist.get() != null && t.exist.get();
        }

        /** 调度表非空且错误调度序号非空 */
        @Override
        public boolean trayHasErrorDispatchIndex(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            return t != null && t.dispatchlist != null && t.errorDispatchindex.get() != null;
        }

        /** 错误调度序号 */
        @Override
        public int trayErrorDispatchIndex(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            return t.errorDispatchindex.get();
        }

        /** 当前调度项快照（复用 TrayManager.getCurrentDispatch 的 index 匹配） */
        @Override
        public TrayDispatchSnapshot currentTrayDispatch(int trayIndex) {
            return toSnapshot(TrayManager.getInstance().getCurrentDispatch(trayIndex));
        }

        /** 错误调度项快照：按 edindex/status/devicestatus 三元 OR 选择，含空值守卫 */
        @Override
        public TrayDispatchSnapshot findErrorDispatch(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            if (t == null || t.dispatchlist == null || t.errorDispatchindex.get() == null) {
                return null;
            }
            int edindex = t.errorDispatchindex.get();
            for (int j = 0; j < t.dispatchlist.size(); j++) {
                DispatchModel dm = t.dispatchlist.get(j);
                if (edindex == dm.index.get()
                        || (dm.status.get() != null && dm.status.get() == ConstantData.STATUS_ERROR)
                        || (dm.devicestatus.get() != null && dm.devicestatus.get() == ConstantData.STATUS_ERROR)) {
                    return toSnapshot(dm);
                }
            }
            return null;
        }

        /** 分配空闲 RNA 给当前调度项：set target/status=RUN + setCurrentTemplate + setCurrentDispatch */
        @Override
        public void allocateFreeRnaToDispatch(int trayIndex, int dispatchIndex) {
            PipeTrayModel tray = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            DispatchModel dm = TrayManager.getInstance().getCurrentDispatch(trayIndex);
            if (tray == null || dm == null) {
                return;
            }
            PipeRNAModel rm = RNAManager.getInstance().getFreeModel();
            if (rm != null) {
                dm.target.set(rm.index.get());
                dm.status.set(ConstantData.STATUS_RUN);
                RNAManager.getInstance().setCurrentTemplate(rm.index.get(), dm, 0);
                TrayManager.getInstance().setCurrentDispatch(tray.index.get(), tray.dispatchindex.get());
            }
        }

        /** 分配空闲 PCR 给当前调度项（与 RNA 对称） */
        @Override
        public void allocateFreePcrToDispatch(int trayIndex, int dispatchIndex) {
            PipeTrayModel tray = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            DispatchModel dm = TrayManager.getInstance().getCurrentDispatch(trayIndex);
            if (tray == null || dm == null) {
                return;
            }
            PipePCRModel rm = PCRManager.getInstance().getFreeModel();
            if (rm != null) {
                dm.target.set(rm.index.get());
                dm.status.set(ConstantData.STATUS_RUN);
                PCRManager.getInstance().setCurrentTemplate(rm.index.get(), dm, 0);
                TrayManager.getInstance().setCurrentDispatch(tray.index.get(), tray.dispatchindex.get());
            }
        }

        /** 推进托盘调度序号 */
        @Override
        public void advanceTrayDispatch(int trayIndex, int newDispatchIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            if (t != null) {
                t.dispatchindex.set(newDispatchIndex);
            }
        }

        /** 托盘标记复位为空闲（FLAG_FREE） */
        @Override
        public void setTrayFlagFree(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            if (t != null) {
                t.flagstatus.set(ConstantData.FLAG_FREE);
            }
        }

        /** 托盘状态置完成（str_FINISH） */
        @Override
        public void setTrayStatusFinish(int trayIndex) {
            PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(trayIndex);
            if (t != null) {
                t.status.set(ConstantData.str_FINISH);
            }
        }

        /** 当前调度项状态置完成（STATUS_FINISH） */
        @Override
        public void setDispatchStatusFinish(int trayIndex, int dispatchIndex) {
            DispatchModel dm = TrayManager.getInstance().getCurrentDispatch(trayIndex);
            if (dm != null) {
                dm.status.set(ConstantData.STATUS_FINISH);
            }
        }

        /** 转发：下发 LED 任务（外部类 createLedTask） */
        @Override
        public void createLedTask(int trayIndex, int arg) {
            APLManager.this.createLedTask(trayIndex, arg);
        }

        /** 转发：持久化托盘（TrayManager.saveModel） */
        @Override
        public void saveTray(int trayIndex) {
            TrayManager.getInstance().saveModel(trayIndex);
        }

        /** 设备是否已回盘完成（runtime.backtrayfinish && armStep==BACK），按设备类型取 RNA/PCR */
        @Override
        public boolean deviceBackFinishedInBack(int deviceType, int deviceIndex) {
            if (deviceType == ConstantData.DEVICE_RNA) {
                PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(deviceIndex);
                return m != null && m.runtime.isBacktrayfinish() && m.armStep.get() == ArmStep.BACK;
            } else if (deviceType == ConstantData.DEVICE_PCR) {
                PipePCRModel m = PCRManager.getInstance().getPipePCRModel(deviceIndex);
                return m != null && m.runtime.isBacktrayfinish() && m.armStep.get() == ArmStep.BACK;
            }
            return false;
        }

        /** 标记设备调度完成：armStep=FREE + runtime.uiinit=true，按设备类型取 RNA/PCR */
        @Override
        public void markDeviceScheduleDone(int deviceType, int deviceIndex) {
            if (deviceType == ConstantData.DEVICE_RNA) {
                PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(deviceIndex);
                if (m != null) {
                    m.armStep.set(ArmStep.FREE);
                    m.runtime.setUiinit(true);
                }
            } else if (deviceType == ConstantData.DEVICE_PCR) {
                PipePCRModel m = PCRManager.getInstance().getPipePCRModel(deviceIndex);
                if (m != null) {
                    m.armStep.set(ArmStep.FREE);
                    m.runtime.setUiinit(true);
                }
            }
        }

        /** 转发：委托 RNA 设备链路一步（外部类 checkrnawork） */
        @Override
        public boolean delegateRna(int deviceIndex, int trayIndex) {
            return checkrnawork(deviceIndex, trayIndex);
        }

        /** 转发：委托 PCR 设备链路一步（外部类 checkpcrwork） */
        @Override
        public boolean delegatePcr(int deviceIndex, int trayIndex) {
            return checkpcrwork(deviceIndex, trayIndex);
        }

        /** 转发：关核提门（外部类 checkrnaclosedoor） */
        @Override
        public boolean closeRnaDoor(int rnaIndex) {
            return checkrnaclosedoor(rnaIndex);
        }

        /** 日志钩子：按级别转发到 android.util.Log.*（测试里由 mock no-op） */
        @Override
        public void log(int level, String tag, String msg) {
            switch (level) {
                case Log.VERBOSE: Log.v(tag, msg); break;
                case Log.DEBUG:   Log.d(tag, msg); break;
                case Log.INFO:    Log.i(tag, msg); break;
                case Log.WARN:    Log.w(tag, msg); break;
                case Log.ERROR:   Log.e(tag, msg); break;
                default:          Log.i(tag, msg); break;
            }
        }
    }

    /**
     * 机械臂命令回调端口适配：每次 startArmTask* 调用新建一个实例，
     * 闭包捕获当前 ArmworkMessage + armErrorCallback，把 ArmCallbackEngine 的意图 1:1 落到现有 Manager。
     * 行为与原匿名 ArmFinishCallback 逐行等价。
     */
    private class AplArmCallbackPort implements ArmCallbackPort {
        private final ArmworkMessage msg;
        private final ArmErrorCallback armErrorCallback;

        AplArmCallbackPort(ArmworkMessage msg, ArmErrorCallback armErrorCallback) {
            this.msg = msg;
            this.armErrorCallback = armErrorCallback;
        }

        /** Get.finish 守卫：目标设备 armStep 是否为 GET。 */
        @Override
        public boolean deviceArmStepIsGet(int deviceType, int index) {
            if (deviceType == ConstantData.DEVICE_RNA) {
                PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(index);
                return m != null && m.armStep.get() == ArmStep.GET;
            } else if (deviceType == ConstantData.DEVICE_PCR) {
                PipePCRModel m = PCRManager.getInstance().getPipePCRModel(index);
                return m != null && m.armStep.get() == ArmStep.GET;
            }
            return false;
        }

        /** 置目标设备 runtime.gettrayfinish = true。 */
        @Override
        public void setDeviceGettrayfinish(int deviceType, int index) {
            if (deviceType == ConstantData.DEVICE_RNA) {
                PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(index);
                if (m != null) m.runtime.setGettrayfinish(true);
            } else if (deviceType == ConstantData.DEVICE_PCR) {
                PipePCRModel m = PCRManager.getInstance().getPipePCRModel(index);
                if (m != null) m.runtime.setGettrayfinish(true);
            }
        }

        /** 启动对应设备模板：createRnaRunTemplate / createPcrRunTemplate。 */
        @Override
        public void createRunTemplate(int deviceType, int index) {
            if (deviceType == ConstantData.DEVICE_RNA) {
                createRnaRunTemplate(index);
            } else if (deviceType == ConstantData.DEVICE_PCR) {
                createPcrRunTemplate(index);
            }
        }

        /** 送回托盘完成：RNAManager/PCRManager.finishBacktray(srcPos, aimPos)。 */
        @Override
        public void finishBacktray(int deviceType, int srcPos, int aimPos) {
            if (deviceType == ConstantData.DEVICE_RNA) {
                RNAManager.getInstance().finishBacktray(srcPos, aimPos);
            } else if (deviceType == ConstantData.DEVICE_PCR) {
                PCRManager.getInstance().finishBacktray(srcPos, aimPos);
            }
        }

        /** 移出托盘完成：RNAManager/PCRManager.finishMoveouttray(srcPos, aimPos)。 */
        @Override
        public void finishMoveouttray(int deviceType, int srcPos, int aimPos) {
            if (deviceType == ConstantData.DEVICE_RNA) {
                RNAManager.getInstance().finishMoveouttray(srcPos, aimPos);
            } else if (deviceType == ConstantData.DEVICE_PCR) {
                PCRManager.getInstance().finishMoveouttray(srcPos, aimPos);
            }
        }

        /** 更新调度项的下一步目标设备。 */
        @Override
        public void updateDispatchNext(int trayIndex, int dispatchIndex, int deviceType, int aimPos) {
            TrayManager.getInstance().updateDsiapatchNext(trayIndex, dispatchIndex, deviceType, aimPos);
        }

        /** 更新调度项状态。 */
        @Override
        public void updateDispatchStatus(int trayIndex, int dispatchIndex, int status) {
            TrayManager.getInstance().updateDsiapatchStatus(trayIndex, dispatchIndex, status);
        }

        /** 回传机械臂错误：msg.setArmerrortext + armErrorCallback.error(msg)。 */
        @Override
        public void notifyArmError(String errorText) {
            msg.setArmerrortext(errorText);
            if (armErrorCallback != null) {
                try {
                    armErrorCallback.error(msg);
                } catch (Exception e) {
                    Log.e("APLManager", "armErrorCallback.error threw exception: " + e.getMessage(), e);
                }
            }
        }

        /** 日志钩子：转真 android.util.Log。 */
        @Override
        public void log(int level, String tag, String msg) {
            switch (level) {
                case Log.VERBOSE: Log.v(tag, msg); break;
                case Log.DEBUG:   Log.d(tag, msg); break;
                case Log.INFO:    Log.i(tag, msg); break;
                case Log.WARN:    Log.w(tag, msg); break;
                case Log.ERROR:   Log.e(tag, msg); break;
                default:          Log.i(tag, msg); break;
            }
        }
    }

    public boolean createArmTaskGetTray(DispatchModel dispatch) {
        if (dispatch != null) {
            ArmworkMessage msg = new ArmworkMessage();
            int tray = dispatch.tray.get();
            int target = dispatch.target.get();
            int targettype = dispatch.targettype.get();
            if (targettype == ConstantData.DEVICE_RNA) {
                msg.setType(ConstantData.TASK_START_RNA);
                msg.setIndex(1);//默认是机械臂1
                Log.e("ArmTaskWorker", "tray: " + tray + " rna: " + target);
            } else if (targettype == ConstantData.DEVICE_PCR) {
                msg.setType(ConstantData.TASK_START_PCR);
                msg.setIndex(1);//默认是机械臂1
                Log.e("ArmTaskWorker", "tray: " + tray + " pcr: " + target);
            }
            msg.setSource(tray);
            msg.setTarget(target);
            msg.setSourcetype(ConstantData.DEVICE_TRAY);
            msg.setTargettype(targettype);
            msg.setTray(tray);
            msg.setDispatchindex(dispatch.index.get());
            msg.setProject(dispatch.projectname.get());
            msg.setTemplate(dispatch.templatename.get());
            msg.setBatch(dispatch.batch.get());
            //  EventBus.getDefault().postSticky(msg);
            Log.e("ArmTaskWorker", "tray: " + tray + " dispatch: " + dispatch.index.get());
            if (dispatch.index.get() == 1) {
                ArmManager.getInstance().setCooler(tray, 25);
            }
            if (armWorkListener != null) {
                armWorkListener.executeWork(msg);
            } else {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
    public boolean createArmMoveTray(int source, int sourcetype, int target,int targettype, int tray,int dispatchindex) {
        ArmworkMessage msg = new ArmworkMessage();
        msg.setSource(source);
        msg.setSourcetype(sourcetype);
        msg.setTarget(target);
        msg.setTargettype(targettype);
        msg.setDispatchindex(dispatchindex);
        msg.setTray(tray);
        msg.setType(ConstantData.TASK_TRAY_MOVE);
        Log.i("ArmTaskWorker", "createArm move source:"+source +" sourcetype:"+sourcetype
                + " target:"+ target + " targettype:"+targettype +" dispatchindex:"+dispatchindex);
        msg.setIndex(1);//arm
//        EventBus.getDefault().postSticky(msg);
        if (armWorkListener != null) {
            armWorkListener.executeWork(msg);
        } else {
            return false;
        }
        return true;
    }

    public boolean createArmBackTray(int source, int sourcetype, int tray, int dispatchindex) {
        ArmworkMessage msg = new ArmworkMessage();
        msg.setSource(source);
        msg.setSourcetype(sourcetype);
        msg.setTarget(tray);
        msg.setTargettype(ConstantData.DEVICE_TRAY);
        msg.setDispatchindex(dispatchindex);
        msg.setTray(tray);
        if (sourcetype == ConstantData.DEVICE_RNA) {
            msg.setType(ConstantData.TASK_FINISH_RNA);
            Log.i("ArmTaskWorker", "createArm rna backtray");
        } else if (sourcetype == ConstantData.DEVICE_PCR) {
            msg.setType(ConstantData.TASK_FINISH_PCR);
            Log.i("ArmTaskWorker", "createArm pcr backtray");
        }
        msg.setIndex(1);//arm
//        EventBus.getDefault().postSticky(msg);
        if (armWorkListener != null) {
            armWorkListener.executeWork(msg);
        } else {
            return false;
        }
        return true;
    }

    public String startArmTaskGetTray(ArmworkMessage msg, ArmErrorCallback armErrorCallback) {
        int source = msg.getSource();
        int sourcetype = msg.getSourcetype();
        int target = msg.getTarget();
        int targettype = msg.getTargettype();
        Log.e("ArmTaskWorker", "机械臂 startArmTaskGetTray"
                + " source:" + source + " sourcetype:" + sourcetype
                + " target:" + target + " targettype:" + targettype);
        ArmManager.getInstance().updateArmworkMessage(1, msg);
        String rstr = ArmManager.getInstance().startArmRobot(source,sourcetype,target,targettype,new ArmFinishCallback() {
            @Override
            public void finish(int arm, ItemProperty aimitem, ItemProperty sourceitem) {
                if(sourceitem!=null && aimitem!=null) {
                    Log.e("ArmTaskWorker", "机械臂回调 取托盘 source：" + sourceitem.getType() +
                            sourceitem.getPosition() + " 已送达目标位： " + aimitem.getType() + aimitem.getPosition());
                    if (aimitem.getType().equalsIgnoreCase(ConstantData.nameRNA)) {
                        PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(aimitem.getPosition());
                        if (model == null) {
                            Log.e("APLManager", "startArmTaskGetTray finish: RNA model " + aimitem.getPosition() + " is null, skipping");
                            return;
                        }
                        if (model.armStep.get() == ArmStep.GET) {
                            model.runtime.setGettrayfinish(true);
//                            PCRManager.getInstance().updateTray(target, source);
                            model.armStep.set(ArmStep.RUN);
                            model.runtime.setExcutestart(false);
                            model.runtime.setExcutefinish(false);

                            // 🔍 调度分析日志：机械臂送达RNA，启动模板
                            Integer currentTray = model.currentTray.get();
                            Integer dispatchIndex = model.dispatchindex.get();
                            Log.e("DispatchAnalysis", String.format(
                                "[机械臂送达] RNA:%d armStep:WORK_GET→WORK_RUN currentTray:%s dispatchIndex:%s 准备启动模板",
                                aimitem.getPosition(),
                                currentTray != null ? currentTray.toString() : "null",
                                dispatchIndex != null ? dispatchIndex.toString() : "null"
                            ));

                            Log.w("ArmTaskWorker", "【回调立即切换】RNA:"+aimitem.getPosition()+" armStep→WORK_RUN，防止空窗期被重复调度");
                            createRnaRunTemplate(aimitem.getPosition());//临时措施，启动rna模板
                        }
                    } else if (aimitem.getType().equalsIgnoreCase(ConstantData.namePCR)) {//targettype == ConstantData.DEVICE_PCR &&
                        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(aimitem.getPosition());
                        if (model == null) {
                            Log.e("APLManager", "startArmTaskGetTray finish: PCR model " + aimitem.getPosition() + " is null, skipping");
                            return;
                        }
                        if (model.armStep.get() == ArmStep.GET) {
                            model.runtime.setGettrayfinish(true);
                            model.armStep.set(ArmStep.RUN);
                            model.runtime.setExcutestart(false);
                            model.runtime.setExcutefinish(false);

                            // 🔍 调度分析日志：机械臂送达PCR，启动模板
                            Integer currentTray = model.currentTray.get();
                            Integer dispatchIndex = model.dispatchindex.get();
                            Log.e("DispatchAnalysis", String.format(
                                "[机械臂送达] PCR:%d armStep:WORK_GET→WORK_RUN currentTray:%s dispatchIndex:%s 准备启动模板",
                                aimitem.getPosition(),
                                currentTray != null ? currentTray.toString() : "null",
                                dispatchIndex != null ? dispatchIndex.toString() : "null"
                            ));

                            Log.w("ArmTaskWorker", "【回调立即切换】PCR:"+aimitem.getPosition()+" armStep→WORK_RUN，等待调度循环下发模板");
                            // 不再直接调用 createPcrRunTemplate，由调度循环（checkpcrRun）检测到 RUN 状态后统一下发
                            // 避免机械臂尚未完成其他移盘操作时 PCR 提前开始
                        }
                    }
                }else{
                    Log.e("ArmTaskWorker", "机械臂回调数据错误 startArmTaskGetTray finish null");
                }
            }

            @Override
            public void fail(int arm, ItemProperty aimitem, ItemProperty sourceitem, String error) {
                Log.e("ArmTaskWorker", "机械臂取托盘 :" + sourcetype + source +" 来源:"+ targettype + target + "  回调 fail error:" + error);
                msg.setArmerrortext(error);
                if (armErrorCallback != null) {
                    try {
                        armErrorCallback.error(msg);
                    } catch (Exception e) {
                        Log.e("APLManager", "armErrorCallback.error threw exception: " + e.getMessage(), e);
                    }
                }
                if(sourceitem!=null) {
                    if (source == sourceitem.getPosition()) {
                        TrayManager.getInstance().updateDsiapatchStatus(source, msg.getDispatchindex(), ConstantData.STATUS_ERROR);
                    }else{
                        Log.e("ArmTaskWorker", "机械臂回调数据不一致 startArmTaskBackTray fail source tray:"+source +" sourceitem "+sourceitem.getPosition());
                    }
                }else{
                    Log.e("ArmTaskWorker", "机械臂回调数据错误 startArmTaskGetTray fail null");
                }
            }
        },source);
        if (rstr != null) {
            TrayManager.getInstance().updateDsiapatchStatus(source, msg.getDispatchindex(), ConstantData.STATUS_ERROR);
            Log.e("ArmTaskWorker", "机械臂 开始失败:" + rstr);
            return "机械臂取托盘" + source + " 失败,原因:" + rstr;
        } else {
            Log.e("ArmTaskWorker", "机械臂 开始成功");
            return null;
        }
    }

    public String startArmTaskBackTray(ArmworkMessage msg, ArmErrorCallback armErrorCallback) {
        int source = msg.getSource();
        int sourcetype = msg.getSourcetype();
        int target = msg.getTarget();
        int targettype = msg.getTargettype();
        int dispatchindex = msg.getDispatchindex();
        Log.e("ArmTaskWorker", "机械臂 返回托盘"
                + " source:" + source + " sourcetype:" + sourcetype
                + " target:" + target + " targettype:" + targettype);
        boolean trayexist = TrayManager.getInstance().getExist(target);
        //TODO fordebugtest
//        if(false){
        if (trayexist) {
            TrayManager.getInstance().updateDsiapatchStatus(target, msg.getDispatchindex(), ConstantData.STATUS_ERROR);
            return "托盘" + target + " 位置已存在样品!";
        }
        switch (sourcetype) {
            case ConstantData.DEVICE_RNA:
//                TrayManager.getInstance().updateDsiapatchStoptime(target,dispatchindex);
                TrayManager.getInstance().updateDsiapatchDeviceStatus(target,dispatchindex,ConstantData.STATUS_FINISH);
                //ArmManager.getInstance().setRNATray(source, target);
                break;
            case ConstantData.DEVICE_PCR:
//                TrayManager.getInstance().updateDsiapatchStoptime(target,dispatchindex);
                TrayManager.getInstance().updateDsiapatchDeviceStatus(target,dispatchindex,ConstantData.STATUS_FINISH);
                //ArmManager.getInstance().setPCRTray(source, target);
                break;
        }
        ArmManager.getInstance().updateArmworkMessage(1, msg);
        String rstr = ArmManager.getInstance().startArmRobot(source,sourcetype,target,targettype,new ArmFinishCallback() {

            @Override
            public void finish(int arm, ItemProperty aimitem, ItemProperty sourceitem) {
                TrayManager.getInstance().updateDsiapatchStatus(target,dispatchindex,ConstantData.STATUS_FINISH);
                if(sourceitem!=null&&aimitem!=null) {
                    Log.e("ArmTaskWorker", "机械臂 送回托盘"
                            + " source:" + sourceitem.getPosition() + " sourcetype:" + sourceitem.getType()
                            + " target:" + aimitem.getPosition() + " targettype:" + aimitem.getType());
                    Log.e("ArmTaskWorker", "机械臂 送回托盘 arm:" + arm);
                    Log.e("ArmTaskWorker", "机械臂 送回托盘：" + aimitem.getPosition() + " 来自： " + sourceitem.getPosition());
                    switch (sourceitem.getType()) {
                        case ConstantData.nameRNA:
                            RNAManager.getInstance().finishBacktray(sourceitem.getPosition(), aimitem.getPosition());
                            break;
                        case ConstantData.namePCR:
                            PCRManager.getInstance().finishBacktray(sourceitem.getPosition(), aimitem.getPosition());
                            break;
                        case ConstantData.nameTray:
                            break;
                    }
                }else{
                    Log.e("ArmTaskWorker", "机械臂回调数据错误 startArmTaskBackTray finish null");
                }
            }

            @Override
            public void fail(int arm, ItemProperty aimitem, ItemProperty sourceitem, String error) {
                Log.e("ArmTaskWorker", "机械臂送回托盘:" + target + " 来源："+ sourcetype + source+" 回调 fail " + error);
                msg.setArmerrortext(error);
                if (armErrorCallback != null) {
                    try {
                        armErrorCallback.error(msg);
                    } catch (Exception e) {
                        Log.e("APLManager", "armErrorCallback.error threw exception: " + e.getMessage(), e);
                    }
                }
                if (aimitem!=null) {
                    if(target== aimitem.getPosition()) {
                        TrayManager.getInstance().updateDsiapatchStatus(target, msg.getDispatchindex(), ConstantData.STATUS_ERROR);
                    }else{
                        Log.e("ArmTaskWorker", "机械臂回调数据不一致 startArmTaskBackTray fail target tray:"+target +" aimitem"+aimitem.getPosition());
                    }
                }else{
                    Log.e("ArmTaskWorker", "机械臂回调数据错误 startArmTaskBackTray fail null");
                }
            }
        },target);
        if (rstr != null) {
            TrayManager.getInstance().updateDsiapatchStatus(target, msg.getDispatchindex(), ConstantData.STATUS_ERROR);
            Log.i("ArmTaskWorker", "机械臂送回托盘" + target + " 运行失败-" + rstr);
            return "托盘" + target + " 送回任务失败,原因:" + rstr;
        } else {
            Log.e("ArmTaskWorker", "机械臂送回托盘" + target + " 运行成功");
            return null;
        }
    }

    public String startArmTaskMoveTray(ArmworkMessage msg, ArmErrorCallback armErrorCallback) {
        int source = msg.getSource();
        int sourcetype = msg.getSourcetype();
        int target = msg.getTarget();
        int targettype = msg.getTargettype();
        int trayindex = msg.getTray();
        int dispatchindex = msg.getDispatchindex();
        Log.e("ArmTaskWorker", "机械臂 移动托盘"
                + " source:" + source + " sourcetype:" + sourcetype
                + " target:" + target + " targettype:" + targettype);
        switch (sourcetype) {
            case ConstantData.DEVICE_RNA:
                Log.e("ArmTaskWorker", "机械臂 移动托盘 source RNA"+source);
                //TODO fordebugtest
//                if(false){
                if (PCRManager.getInstance().getExist(target)) {
                    Log.e("ArmTaskWorker", "PCR" + target + " 位置已存在样品!");
                    TrayManager.getInstance().updateDsiapatchStatus(trayindex, msg.getDispatchindex(), ConstantData.STATUS_ERROR);
                    return "PCR" + target + " 位置已存在样品!";
                }
                PCRManager.getInstance().updateCurrentTray(target,trayindex,dispatchindex);//提前更新托盘来源
//                ArmManager.getInstance().setRNAPCR(source, target);
                Log.e("ArmTaskWorker", "机械臂 移动托盘 target PCR"+target);
                break;
            case ConstantData.DEVICE_PCR:
                Log.e("ArmTaskWorker", "机械臂 移动托盘 source PCR"+source);
                //TODO fordebugtest
//                if(false){
                if (RNAManager.getInstance().getExist(target)) {
                    Log.e("ArmTaskWorker", "RNA" + target + " 位置已存在样品!");
                    TrayManager.getInstance().updateDsiapatchStatus(trayindex, msg.getDispatchindex(), ConstantData.STATUS_ERROR);
                    return "RNA" + target + " 位置已存在样品!";
                }
                RNAManager.getInstance().updateCurrentTray(target,trayindex,dispatchindex);//提前更新托盘来源
//                ArmManager.getInstance().setPCRRNA(source, target);
                Log.e("ArmTaskWorker", "机械臂 移动托盘 target RNA"+target);
                break;
        }
        ArmManager.getInstance().updateArmworkMessage(1, msg);

        String rstr = ArmManager.getInstance().startArmRobot(source,sourcetype,target,targettype,new ArmFinishCallback() {

            @Override
            public void finish(int arm, ItemProperty aimitem, ItemProperty sourceitem) {
                TrayManager.getInstance().updateDsiapatchStatus(trayindex,dispatchindex,ConstantData.STATUS_FINISH);
                if(sourceitem!=null&&aimitem!=null) {
                    Log.e("ArmTaskWorker", "机械臂移动托盘"
                            + " source:" + sourceitem.getPosition() + " sourcetype:" + sourceitem.getType()
                            + " target:" + aimitem.getPosition() + " targettype:" + aimitem.getType());
                    switch (sourceitem.getType()) {
                        case ConstantData.nameRNA:
                            RNAManager.getInstance().finishMoveouttray(sourceitem.getPosition(), aimitem.getPosition());
                            break;
                        case ConstantData.namePCR:
                            PCRManager.getInstance().finishMoveouttray(sourceitem.getPosition(), aimitem.getPosition());
                            break;
                        case ConstantData.nameTray:
                            break;
                    }
                    switch (aimitem.getType()) {
                        case ConstantData.nameRNA:
                            TrayManager.getInstance().updateDsiapatchNext(trayindex,dispatchindex,ConstantData.DEVICE_RNA,aimitem.getPosition());
                            {
                                PipeRNAModel targetModel = RNAManager.getInstance().getPipeRNAModel(aimitem.getPosition());
                                if (targetModel != null && targetModel.armStep.get() == ArmStep.GET) {
                                    targetModel.armStep.set(ArmStep.RUN);
                                    targetModel.runtime.setExcutestart(false);
                                    targetModel.runtime.setExcutefinish(false);
                                    Log.w("ArmTaskWorker", "【MOVE回调立即切换】RNA:"+aimitem.getPosition()+" armStep→WORK_RUN，防止空窗期被重复调度");
                                }
                            }
                            createRnaRunTemplate(aimitem.getPosition());//临时措施，启动rna模板
                            break;
                        case ConstantData.namePCR:
                            TrayManager.getInstance().updateDsiapatchNext(trayindex, dispatchindex, ConstantData.DEVICE_PCR, aimitem.getPosition());
                            {
                                PipePCRModel targetModel = PCRManager.getInstance().getPipePCRModel(aimitem.getPosition());
                                if (targetModel != null && targetModel.armStep.get() == ArmStep.GET) {
                                    targetModel.armStep.set(ArmStep.RUN);
                                    targetModel.runtime.setExcutestart(false);
                                    targetModel.runtime.setExcutefinish(false);
                                    Log.w("ArmTaskWorker", "【MOVE回调立即切换】PCR:"+aimitem.getPosition()+" armStep→WORK_RUN，等待调度循环下发模板");
                                    // 不再直接调用 createPcrRunTemplate，由调度循环（checkpcrRun）统一下发
                                    // 避免机械臂尚未完成其他操作时 PCR 提前开始
                                }
                            }
                            break;
                        case ConstantData.nameTray:
                            break;
                    }
                }else{
                    Log.e("ArmTaskWorker", "机械臂回调数据错误 startArmTaskMoveTray fail null");
                }
            }

            @Override
            public void fail(int arm, ItemProperty aimitem, ItemProperty sourceitem, String error) {
                Log.e("ArmTaskWorker", "机械臂移动托盘 回调 fail:" + error);
                TrayManager.getInstance().updateDsiapatchStatus(trayindex, dispatchindex, ConstantData.STATUS_ERROR);
                msg.setArmerrortext(error);
                if (armErrorCallback != null) {
                    try {
                        armErrorCallback.error(msg);
                    } catch (Exception e) {
                        Log.e("APLManager", "armErrorCallback.error threw exception: " + e.getMessage(), e);
                    }
                }
            }
        },trayindex);
        if (rstr != null) {
            TrayManager.getInstance().updateDsiapatchStatus(trayindex, dispatchindex, ConstantData.STATUS_ERROR);
            Log.i("ArmTaskWorker", "机械臂移动托盘 运行失败-" + rstr);
            return "机械臂移动托盘,任务失败,原因:" + rstr;
        } else {
            Log.e("ArmTaskWorker", "机械臂移动托盘 运行成功");
            return null;
        }
    }


    public boolean startRunTemplate(int target, int targettype, int tray, int dispatchindex) {
        Log.i("ArmTaskWorker", "startRunTemplate run target:" + target + " tray:" + tray + " dispatchindex:" + dispatchindex);
        if (targettype == ConstantData.DEVICE_RNA) {
            Log.w("ArmTaskWorker", "rna 运行 开始:" + target);
            boolean result = RNAManager.getInstance().execute(target);
            if (result) {
                TrayManager.getInstance().updateDsiapatchDeviceStatus(tray, dispatchindex, ConstantData.STATUS_RUN);
                TrayManager.getInstance().updateDsiapatchStarttime(tray, dispatchindex);
                RNAManager.getInstance().updateExecuteStart(target, true);
                Log.w("ArmTaskWorker", "rna 运行 成功 " + target);
                return true;
            } else {
                TrayManager.getInstance().updateDsiapatchDeviceStatus(tray, dispatchindex, ConstantData.STATUS_ERROR);
                RNAManager.getInstance().updateExecuteStart(target, false);
                Log.e("ArmTaskWorker", "rna 运行 失败 " + target);
                return false;
            }
        } else if (targettype == ConstantData.DEVICE_PCR) {
            Log.w("ArmTaskWorker", "开始运行pcr :" + target);
            String result = PCRManager.getInstance().execute(target);
            if (result==null) {
                TrayManager.getInstance().updateDsiapatchDeviceStatus(tray, dispatchindex, ConstantData.STATUS_RUN);
                TrayManager.getInstance().updateDsiapatchStarttime(tray, dispatchindex);
                PCRManager.getInstance().updateExecuteStart(target, true);
                Log.i("ArmTaskWorker", "pcr 运行 成功 " + target);
                return true;
            } else {
                TrayManager.getInstance().updateDsiapatchDeviceStatus(tray, dispatchindex, ConstantData.STATUS_ERROR);
                TrayManager.getInstance().updateDsiapatchStatus(tray, dispatchindex, ConstantData.STATUS_ERROR);
                PCRManager.getInstance().updateExecuteStart(target, false);
                Log.e("ArmTaskWorker", "pcr 运行 失败 " + target);
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean createRnaRunTemplate(int target) {
        ArmworkMessage msg = new ArmworkMessage();
        msg.setTarget(target);
        msg.setType(ConstantData.TASK_RUN_RNA);
//        EventBus.getDefault().postSticky(msg);
        if (armWorkListener != null) {
            armWorkListener.executeWork(msg);
        } else {
            return false;
        }
        return true;
    }

    public void createLedTask(int target, int value) {
        ArmworkMessage msg = new ArmworkMessage();
        msg.setTarget(target);
        msg.setType(ConstantData.TASK_TRAY_LED);
        msg.setStatus(value);
//        EventBus.getDefault().postSticky(msg);
        if (armWorkListener != null) {
            armWorkListener.executeWork(msg);
        }
    }

    public void checkRnaRunTemplate(int rna) {
        PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(rna);
        if (model != null && model.armStep.get() == ArmStep.GET) {
            model.runtime.setExcutestart(false);
            model.runtime.setExcutefinish(false);
            model.armStep.set(ArmStep.RUN);
            Log.e("ArmTaskWorker", "rna :"+rna +" WORK_RUN ");
        }
    }

    public boolean checkRnaFinish(int target) {
        PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(target);
        if (model != null && model.armStep.get() == ArmStep.RUN) {
            Integer currentTray = model.currentTray.get();
            if (currentTray == null) {
                Log.e("ArmTaskWorker", "RNA:" + target + " checkRnaFinish currentTray is null, skipping");
                return false;
            }

            Log.e("ArmTaskWorker", "RNA:"+target+" checkRnaFinish Ok currentTray:" + currentTray);
            model.runtime.setExcutefinish(true);

            // ✅ 优先从模型自身的 currentDispatch 获取调度信息
            DispatchModel dispatchModel = model.currentDispatch.get();

            if(dispatchModel==null) {
                // 回退到通过 tray+dispatchindex 查找
                Log.e("ArmTaskWorker", "【RNA还盘断点1】currentDispatch为空，尝试通过 currentTray 查找");
                dispatchModel = TrayManager.getInstance().getCurrentDispatch(currentTray);
            }

            if(dispatchModel==null) {
                Log.e("ArmTaskWorker", "【RNA还盘断点1】checkRnaFinish 找不到调度数据 rna:" + target +" tray:"+ currentTray + " → 强制设置 WORK_BACK");

                // 🔍 调度分析日志：找不到调度数据
                Integer dispatchIndex = model.dispatchindex.get();
                Log.e("DispatchAnalysis", String.format(
                    "[完成判断-异常] RNA:%d armStep:WORK_RUN currentTray:%d dispatchIndex:%d 找不到调度数据 → 强制还盘 armStep→WORK_BACK",
                    target,
                    currentTray,
                    dispatchIndex != null ? dispatchIndex : -1
                ));

                // 即使找不到 dispatch 也要设置 WORK_BACK，让 arm 能来取盘
                model.runtime.setBacktraystart(false);
                model.runtime.setBacktrayfinish(false);
                model.armStep.set(ArmStep.BACK);
                return true;
            } else {
                if (dispatchModel.nextdevice.get() != null && dispatchModel.nextdevice.get() > 0) {
                    model.runtime.setMovetraystart(false);
                    model.runtime.setMovetrayfinish(false);
                    model.armStep.set(ArmStep.MOVE);

                    // 🔍 调度分析日志：移动到下一设备
                    Log.e("DispatchAnalysis", String.format(
                        "[完成判断-移盘] RNA:%d armStep:WORK_RUN→WORK_MOVE tray:%d dispatchIndex:%d nextDevice:%d 准备移到PCR",
                        target,
                        currentTray,
                        dispatchModel.index.get(),
                        dispatchModel.nextdevice.get()
                    ));

                    Log.e("ArmTaskWorker", "rna :" + target + " WORK_MOVE ");
                } else {
                    model.runtime.setBacktraystart(false);
                    model.runtime.setBacktrayfinish(false);
                    model.armStep.set(ArmStep.BACK);

                    // 🔍 调度分析日志：直接还盘
                    Log.e("DispatchAnalysis", String.format(
                        "[完成判断-还盘] RNA:%d armStep:WORK_RUN→WORK_BACK tray:%d dispatchIndex:%d nextDevice:%d 准备还盘",
                        target,
                        currentTray,
                        dispatchModel.index.get(),
                        dispatchModel.nextdevice.get() != null ? dispatchModel.nextdevice.get() : 0
                    ));

                    Log.e("ArmTaskWorker", "rna :" + target + " WORK_BACK ");
                }
                if (dispatchModel.index.get() != null) {
                    int dispatchindex = dispatchModel.index.get();
                    TrayManager.getInstance().updateDsiapatchStoptime(currentTray, dispatchindex);
//                TrayManager.getInstance().updateDsiapatchDeviceStatus(tray, dispatchindex, ConstantData.STATUS_FINISH);
                }
            }
            return true;
        } else {
            return false;
        }

    }

    public void checkRnaFinishError(int target) {
        PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(target);
        if (model != null) {
//            if (model.armStep.get() == ConstantData.WORK_RUN) {
                model.runtime.setExcutefinish(true);
                model.runtime.setBacktraystart(false);
                model.runtime.setBacktrayfinish(false);
                model.armStep.set(ArmStep.BACK);
            Log.e("ArmTaskWorker", "rna :"+target +" WORK_BACK ");
//            }
        }
    }

    public void NotifyRnaTrayError(int target) {
        PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(target);
        if (model != null) {
            if (model.currentTray.get() != null && model.dispatchindex.get()!=null) {
                Log.e("ArmTaskWorker", "NotifyRnaTrayError RNA " + target + " tray" + model.currentTray.get());
                TrayManager.getInstance().errorDispatch(model.currentTray.get(),model.dispatchindex.get(),target,ConstantData.DEVICE_RNA);
                //通知托盘发生错误
            }
        }
    }

    public boolean createPcrRunTemplate(int target) {
        ArmworkMessage msg = new ArmworkMessage();
        msg.setTarget(target);
        msg.setType(ConstantData.TASK_RUN_PCR);
//        EventBus.getDefault().postSticky(msg);
        if (armWorkListener != null) {
            armWorkListener.executeWork(msg);
        } else {
            return false;
        }
        return true;
    }

    public void checkPcrRunTemplate(int pcr) {
        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(pcr);
        if (model != null && model.armStep.get() == ArmStep.GET) {
            model.runtime.setExcutestart(false);
            model.runtime.setExcutefinish(false);
            model.armStep.set(ArmStep.RUN);
            Log.e("ArmTaskWorker", "PCR:"+pcr +" WORK_RUN ");
        }
    }

    public boolean checkPcrFinish(int target) {
        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(target);
        if (model != null && model.armStep.get() == ArmStep.RUN) {
            Integer currentTray = model.currentTray.get();
            if (currentTray == null) {
                Log.e("ArmTaskWorker", "PCR:" + target + " checkPcrFinish currentTray is null, skipping");
                return false;
            }

            Log.e("ArmTaskWorker", "PCR:"+target+" checkPcrFinish Ok currentTray:" + currentTray);
            model.runtime.setExcutefinish(true);

            // ✅ 优先从模型自身的 currentDispatch 获取调度信息
            DispatchModel dispatchModel = model.currentDispatch.get();

            if(dispatchModel==null) {
                // 回退到通过 tray+dispatchindex 查找
                Log.e("ArmTaskWorker", "【还盘断点1】currentDispatch为空，尝试通过 currentTray 查找");
                dispatchModel = TrayManager.getInstance().getCurrentDispatch(currentTray);
            }

            if (dispatchModel == null) {
                Log.e("ArmTaskWorker", "【还盘断点1】checkPcrFinish 找不到调度数据 pcr:" + target + " tray:" + currentTray + " → 强制设置 WORK_BACK");

                // 🔍 调度分析日志：找不到调度数据
                Integer dispatchIndex = model.dispatchindex.get();
                Log.e("DispatchAnalysis", String.format(
                    "[完成判断-异常] PCR:%d armStep:WORK_RUN currentTray:%d dispatchIndex:%d 找不到调度数据 → 强制还盘 armStep→WORK_BACK",
                    target,
                    currentTray,
                    dispatchIndex != null ? dispatchIndex : -1
                ));

                // 即使找不到 dispatch 也要设置 WORK_BACK，让 arm 能来取盘
                model.runtime.setBacktraystart(false);
                model.runtime.setBacktrayfinish(false);
                model.armStep.set(ArmStep.BACK);
                return true;
            } else {
                int dispatchindex = dispatchModel.index.get();
                if (dispatchModel.nextdevice.get() != null && dispatchModel.nextdevice.get() > 0) {
                    model.runtime.setMovetraystart(false);
                    model.runtime.setMovetrayfinish(false);
                    model.armStep.set(ArmStep.MOVE);

                    // 🔍 调度分析日志：移动到下一设备
                    Log.e("DispatchAnalysis", String.format(
                        "[完成判断-移盘] PCR:%d armStep:WORK_RUN→WORK_MOVE tray:%d dispatchIndex:%d nextDevice:%d 准备移到RNA",
                        target,
                        currentTray,
                        dispatchindex,
                        dispatchModel.nextdevice.get()
                    ));

                    Log.e("ArmTaskWorker", "finish PCR:" + target + " WORK_MOVE ");
                } else {
                    model.runtime.setBacktraystart(false);
                    model.runtime.setBacktrayfinish(false);
                    model.armStep.set(ArmStep.BACK);

                    // 🔍 调度分析日志：直接还盘
                    Log.e("DispatchAnalysis", String.format(
                        "[完成判断-还盘] PCR:%d armStep:WORK_RUN→WORK_BACK tray:%d dispatchIndex:%d nextDevice:%d 准备还盘",
                        target,
                        currentTray,
                        dispatchindex,
                        dispatchModel.nextdevice.get() != null ? dispatchModel.nextdevice.get() : 0
                    ));

                    Log.e("ArmTaskWorker", "finish PCR:" + target + " WORK_BACK ");
                }
                TrayManager.getInstance().updateDsiapatchStoptime(currentTray, dispatchindex);
                TrayManager.getInstance().updateDsiapatchDeviceStatus(currentTray, dispatchindex, ConstantData.STATUS_FINISH);
            }
            return true;
        } else {
            return false;
        }
    }

    public void checkPcrFinishError(int target) {
        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(target);
        if (model != null) {
//            if (model.armStep.get() == ConstantData.WORK_RUN) {
                model.runtime.setExcutefinish(true);
                model.runtime.setBacktraystart(false);
                model.runtime.setBacktrayfinish(false);
                model.armStep.set(ArmStep.BACK);
            Log.e("ArmTaskWorker", "error PCR:"+target +" WORK_BACK ");
//            }
        }
    }

    // ==== 机械臂异常"跳过此盘" ====
    public interface SkipTrayCallback {
        /** 主线程回调；message 用于提示 */
        void onResult(boolean ok, String message);
    }

    private static final int SKIP_SAMPLE_COUNT = 10;        // 5 秒内采样 10 次
    private static final long SKIP_SAMPLE_INTERVAL_MS = 500;
    private final Handler skipHandler = new Handler(android.os.Looper.getMainLooper());

    /**
     * 跳过机械臂异常对应的托盘：托盘标为异常并停止调度（不自动取回），其他盘继续。
     * 涉及的 PCR/RNA 在 5 秒内多次确认"盘和盖都已处理好"才自动释放为空闲，否则保持占用。
     * @param msg 异常弹窗对应的机械臂任务
     * @return null 表示已跳过（设备释放结果稍后经 callback 通知），否则为拒绝原因
     */
    public String skipErrorTray(ArmworkMessage msg, SkipTrayCallback callback) {
        if (msg == null || msg.getTray() <= 0) {
            return "没有异常托盘信息，无法跳过";
        }
        final int tray = msg.getTray();
        String busy = RobotWorkManager.getInstance().getRobotBusyReason();
        if (busy != null) {
            return "机械臂运行中（" + busy + "），请等待当前动作结束";
        }
        SerialPortManager_Robot.RobotStatus status = SerialPortManager_Robot.getInstance().getRobotStatus();
        if (status == null) {
            return "读取不到机械臂传感器，无法跳过";
        }
        if (Boolean.TRUE.equals(status.getSenseByKey("ARM"))) {
            return "机械臂手上有东西，请先人工取下（盘放回原料盘位）后再跳过";
        }
        if (Boolean.TRUE.equals(status.getSenseByKey("PCR_COVER0"))) {
            return "盖子暂存位上有盖子，请先人工把盖子放回所属 PCR 工位后再跳过";
        }

        // 托盘：标为异常并停止调度；清掉 errorDispatchindex，避免异常调度再自动去取回
        PipeTrayModel t = TrayManager.getInstance().getPipeTrayModel(tray);
        if (t == null) {
            return "找不到托盘" + tray;
        }
        t.status.set(ConstantData.str_ERROR);
        t.flagstatus.set(ConstantData.FLAG_STOP);
        t.errorDispatchindex.set(null);
        TrayManager.getInstance().saveModel(tray);
        ArmManager.getInstance().clearResumableFailure("跳过托盘" + tray);
        Log.e("ArmTaskWorker", "跳过托盘 " + tray + " 已标为异常并停止调度 dispatch:" + msg.getDispatchindex()
                + " source:" + msg.getSourcetype() + "-" + msg.getSource() + " target:" + msg.getTargettype() + "-" + msg.getTarget());

        // 涉及的设备：源/目标中的 RNA、PCR
        final List<int[]> devices = new ArrayList<>();
        if (msg.getSourcetype() == ConstantData.DEVICE_RNA || msg.getSourcetype() == ConstantData.DEVICE_PCR) {
            devices.add(new int[]{msg.getSourcetype(), msg.getSource()});
        }
        if (msg.getTargettype() == ConstantData.DEVICE_RNA || msg.getTargettype() == ConstantData.DEVICE_PCR) {
            devices.add(new int[]{msg.getTargettype(), msg.getTarget()});
        }
        if (devices.isEmpty()) {
            if (callback != null) callback.onResult(true, "托盘" + tray + "已跳过");
            return null;
        }
        sampleAndRelease(tray, devices, 0, new boolean[devices.size()], callback);
        return null;
    }

    /** 每 500ms 采样一次；某台设备任一次不满足即判定不能释放 */
    private void sampleAndRelease(final int tray, final List<int[]> devices, final int round,
                                  final boolean[] failed, final SkipTrayCallback callback) {
        for (int i = 0; i < devices.size(); i++) {
            if (!failed[i] && !isDeviceSafeToRelease(devices.get(i)[0], devices.get(i)[1], tray)) {
                failed[i] = true;
            }
        }
        if (round + 1 < SKIP_SAMPLE_COUNT) {
            skipHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sampleAndRelease(tray, devices, round + 1, failed, callback);
                }
            }, SKIP_SAMPLE_INTERVAL_MS);
            return;
        }
        StringBuilder sb = new StringBuilder("托盘" + tray + "已跳过。");
        boolean allReleased = true;
        for (int i = 0; i < devices.size(); i++) {
            int type = devices.get(i)[0];
            int index = devices.get(i)[1];
            String name = (type == ConstantData.DEVICE_PCR ? "PCR" : "核提") + index;
            if (failed[i]) {
                allReleased = false;
                sb.append(name).append(" 状态未确认（盘/盖未处理好或设备未空闲），保持占用，请人工检查后到设备页复位；");
                Log.e("ArmTaskWorker", "跳过托盘 " + tray + " " + name + " 不释放");
            } else {
                schedulingPort.markDeviceScheduleDone(type, index);
                sb.append(name).append(" 已释放为空闲；");
                Log.e("ArmTaskWorker", "跳过托盘 " + tray + " " + name + " 已释放为空闲");
            }
        }
        if (callback != null) {
            callback.onResult(allReleased, sb.toString());
        }
    }

    /**
     * 设备能否释放：已连接、不在运行/异常、设备里没有盘；PCR 还要求盖子已盖回。
     * 设备当前绑定的是别的托盘时不释放。
     */
    private boolean isDeviceSafeToRelease(int type, int index, int tray) {
        SerialPortManager_Robot.RobotStatus status = SerialPortManager_Robot.getInstance().getRobotStatus();
        if (status == null) {
            return false;
        }
        Integer currentTray;
        Boolean connect;
        String st;
        String linux;
        String key;
        if (type == ConstantData.DEVICE_PCR) {
            PipePCRModel m = PCRManager.getInstance().getPipePCRModel(index);
            if (m == null) return false;
            currentTray = m.currentTray.get();
            connect = m.connect.get();
            st = m.status.get();
            linux = m.linuxStatus.get();
            key = "PCR" + index;
            // PCR 必须已盖回盖子
            if (!Boolean.TRUE.equals(status.getSenseByKey("PCR_COVER" + index))) {
                return false;
            }
        } else {
            PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(index);
            if (m == null) return false;
            currentTray = m.currentTray.get();
            connect = m.connect.get();
            st = m.status.get();
            linux = m.linuxStatus.get();
            key = "RNA" + index;
        }
        if (currentTray != null && currentTray > 0 && currentTray != tray) {
            return false;
        }
        if (!Boolean.TRUE.equals(connect)) {
            return false;
        }
        if (isBusyOrError(st) || isBusyOrError(linux)) {
            return false;
        }
        // 设备里没有盘；暂存位没有盖；手上没有东西
        return !Boolean.TRUE.equals(status.getSenseByKey(key))
                && !Boolean.TRUE.equals(status.getSenseByKey("PCR_COVER0"))
                && !Boolean.TRUE.equals(status.getSenseByKey("ARM"));
    }

    private static boolean isBusyOrError(String s) {
        return s == null || s.equalsIgnoreCase(ConstantData.str_RUN) || s.equalsIgnoreCase(ConstantData.str_ERROR);
    }

    public void NotifyPcrTrayError(int target) {
        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(target);
        if (model != null) {
            if (model.currentTray.get() != null && model.dispatchindex.get() != null) {
                Log.e("ArmTaskWorker", "NotifyPcrTrayError PCR:" + target + " tray:" + model.currentTray.get() +" dispatch:"+model.dispatchindex.get());
                TrayManager.getInstance().errorDispatch(model.currentTray.get(),model.dispatchindex.get(),target,ConstantData.DEVICE_PCR);
                //通知托盘发生错误
            }
        }
    }

//    public void setTrayExperiment(int tray, int pcr) {
//        PipePCRModel pcrm = PCRManager.getInstance().getPipePCRModel(pcr);
//        if (pcrm != null) {
//            PipeTrayModel traym = TrayManager.getInstance().getPipeTrayModel(tray);
//            if (traym != null) {
//                traym.experiment.set(pcrm.experiment.get());
//            }
//        }
//    }

    int HeartBeatSeqid = 0;

    public String testHeartBeat(String selectWSSConnectionItemKey) {
        HeartBeatSeqid++;
        HeartBeatMessage heartBeatMessage = new HeartBeatMessage();
        heartBeatMessage.setSeqId(HeartBeatSeqid);
//            heartBeatMessage.setDeviceID("master");
//            heartBeatMessage.setDeviceType("master");
        heartBeatMessage.setType("HeartBeat");
        heartBeatMessage.setTime(DateUtil.getTime());
        if (selectWSSConnectionItemKey == null) {
            return "请选择要发送消息的模块";
        }
        String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey, heartBeatMessage);
        if (!result.contains("error")) {
            return null;
            //  Toast.makeText(mContext, "sendWSSMessage:" + selectWSSConnectionItemKey + "--" + "heartBeatMessage succeed", Toast.LENGTH_LONG).show();
        } else {
            return "sendWSSMessage:" + selectWSSConnectionItemKey + "--" + "heartBeatMessage error";
        }
    }

    public void TestQRCode() {
        SerialPortManager_QRCode.getInstance().sendStart();
    }

    public ArrayList<DispatchModel> createDispatchList(Context context, String batch, String templateno, int tray, TemplateModel tm) {
        ArrayList<DispatchModel> list = new ArrayList<>();
        if (tm != null && tm.templateType.get() != null) {
            int type = tm.templateType.get();
            switch (type) {
                case ConstantData.TEMPLATE_FULL_SINGLE:
                    list = createDispatchListSingle(context, batch, templateno, tray, tm);
                    break;
                case ConstantData.TEMPLATE_FULL_MULTI:
                    list = createDispatchListMulti(context, batch, templateno, tray, tm);
                    break;
                case ConstantData.TEMPLATE_RNA:
                    list = createDispatchListRNA(context, batch, templateno, tray, tm);
                    break;
                case ConstantData.TEMPLATE_PCR:
                    list = createDispatchListPCR(context, batch, templateno, tray, tm);
                    break;
                default:
                    break;
            }
        }
        return list;
    }

    public ArrayList<DispatchModel> createDispatchListSingle(Context context, String batch, String templateno, int tray, TemplateModel tm) {
        ArrayList<DispatchModel> list = new ArrayList<>();
        DispatchModel m1 = new DispatchModel();
        m1.index.set(1);
        m1.indexname.set("1");
        m1.name.set(context.getString(R.string.rna_extraction));
        m1.batch.set(batch);
        m1.targettype.set(ConstantData.DEVICE_RNA);
        m1.tray.set(tray);
        m1.createtime.set(DataTimeUtil.getSysFullTime());
        m1.projectname.set(tm.name.get());
        m1.templatename.set(tm.single_hex.get());
        m1.path.set(tm.path.get());
        m1.totaltime.set(tm.totaltime.get());
        m1.phase.set(0);
        m1.phasestep.set(0);
        m1.circle.set(0);
        m1.firstSecond.set(1);
        m1.runtime.set(0L);
        m1.templateno.set(templateno);
        m1.nextdevice.set(ConstantData.DEVICE_PCR);
        list.add(m1);
        DispatchModel m2 = new DispatchModel();
        m2.index.set(2);
        m2.indexname.set("2");
        m2.name.set(context.getString(R.string.single_template_first_pcr));
        m2.batch.set(batch);
        m2.targettype.set(ConstantData.DEVICE_PCR);
        m2.tray.set(tray);
        m2.createtime.set(DataTimeUtil.getSysFullTime());
        m2.projectname.set(tm.name.get());
        m2.templatename.set(tm.single_prj.get());
        m2.path.set(tm.path.get());
        m2.totaltime.set(tm.totaltime.get());
        m2.phase.set(0);
        m2.phasestep.set(0);
        m2.circle.set(0);
        m2.firstSecond.set(1);
        m2.runtime.set(0L);
        m2.templateno.set(templateno);
        m2.savepcr.set(true);
        m2.nextdevice.set(0);
        list.add(m2);
        return list;
    }

    public ArrayList<DispatchModel> createDispatchListMulti(Context context, String batch, String templateno, int tray, TemplateModel tm) {
        ArrayList<DispatchModel> list = new ArrayList<>();
        DispatchModel m1 = new DispatchModel();
        m1.index.set(1);
        m1.indexname.set("1");
        m1.name.set(context.getString(R.string.rna_extraction) + "1");
        m1.batch.set(batch);
        m1.targettype.set(ConstantData.DEVICE_RNA);
        m1.tray.set(tray);
        m1.createtime.set(DataTimeUtil.getSysFullTime());
        m1.projectname.set(tm.name.get());
        m1.templatename.set(tm.multi_hex_first.get());
        m1.path.set(tm.path.get());
        m1.totaltime.set(tm.totaltime.get());
        m1.phase.set(0);
        m1.phasestep.set(0);
        m1.circle.set(0);
        m1.firstSecond.set(1);
        m1.runtime.set(0L);
        m1.templateno.set(templateno);
        m1.nextdevice.set(ConstantData.DEVICE_PCR);
        list.add(m1);
        DispatchModel m2 = new DispatchModel();
        m2.index.set(2);
        m2.indexname.set("2");
        m2.name.set(context.getString(R.string.multi_template_first_pcr) + "1");
        m2.batch.set(batch);
        m2.targettype.set(ConstantData.DEVICE_PCR);
        m2.tray.set(tray);
        m2.createtime.set(DataTimeUtil.getSysFullTime());
        m2.projectname.set(tm.name.get());
        m2.templatename.set(tm.multi_prj_first.get());
        m2.path.set(tm.path.get());
        m2.totaltime.set(tm.totaltime.get());
        m2.phase.set(0);
        m2.phasestep.set(0);
        m2.circle.set(0);
        m2.firstSecond.set(1);
        m2.runtime.set(0L);
        m2.templateno.set(templateno);
        m2.nextdevice.set(ConstantData.DEVICE_RNA);
        list.add(m2);
        DispatchModel m3 = new DispatchModel();
        m3.index.set(3);
        m3.indexname.set("3");
        m3.name.set(context.getString(R.string.rna_extraction) + "2");
        m3.batch.set(batch);
        m3.targettype.set(ConstantData.DEVICE_RNA);
        m3.tray.set(tray);
        m3.createtime.set(DataTimeUtil.getSysFullTime());
        m3.projectname.set(tm.name.get());
        m3.templatename.set(tm.multi_hex_second.get());
        m3.path.set(tm.path.get());
        m3.totaltime.set(tm.totaltime.get());
        m3.phase.set(0);
        m3.phasestep.set(0);
        m3.circle.set(0);
        m3.firstSecond.set(2);
        m3.runtime.set(0L);
        m3.templateno.set(templateno);
        m3.nextdevice.set(ConstantData.DEVICE_PCR);
        list.add(m3);
        DispatchModel m4 = new DispatchModel();
        m4.index.set(4);
        m4.indexname.set("4");
        m4.name.set(context.getString(R.string.multi_template_first_pcr) + "2");
        m4.batch.set(batch);
        m4.targettype.set(ConstantData.DEVICE_PCR);
        m4.tray.set(tray);
        m4.createtime.set(DataTimeUtil.getSysFullTime());
        m4.projectname.set(tm.name.get());
        m4.templatename.set(tm.multi_prj_second.get());
        m4.path.set(tm.path.get());
        m4.totaltime.set(tm.totaltime.get());
        m4.phase.set(0);
        m4.phasestep.set(0);
        m4.circle.set(0);
        m4.firstSecond.set(2);
        m4.runtime.set(0L);
        m4.templateno.set(templateno);
        m4.savepcr.set(true);
        m4.nextdevice.set(0);
        list.add(m4);
        return list;
    }

    public ArrayList<DispatchModel> createDispatchListRNA(Context context, String batch, String templateno, int tray, TemplateModel tm) {
        ArrayList<DispatchModel> list = new ArrayList<>();
        DispatchModel m1 = new DispatchModel();
        m1.index.set(1);
        m1.indexname.set("1");
        m1.name.set(context.getString(R.string.rna_extraction));
        m1.batch.set(batch);
        m1.targettype.set(ConstantData.DEVICE_RNA);
        m1.tray.set(tray);
        m1.createtime.set(DataTimeUtil.getSysFullTime());
        m1.projectname.set(tm.name.get());
        m1.templatename.set(tm.rna_hex.get());
        m1.path.set(tm.path.get());
        m1.totaltime.set(tm.totaltime.get());
        m1.phase.set(0);
        m1.phasestep.set(0);
        m1.circle.set(0);
        m1.firstSecond.set(0);
        m1.runtime.set(0L);
        m1.templateno.set(templateno);
        m1.nextdevice.set(0);
        list.add(m1);
        return list;
    }

    public ArrayList<DispatchModel> createDispatchListPCR(Context context, String batch, String templateno, int tray, TemplateModel tm) {
        ArrayList<DispatchModel> list = new ArrayList<>();
        DispatchModel m1 = new DispatchModel();
        m1.index.set(1);
        m1.indexname.set("1");
        m1.name.set(context.getString(R.string.single_template_first_pcr));
        m1.batch.set(batch);
        m1.targettype.set(ConstantData.DEVICE_PCR);
        m1.tray.set(tray);
        m1.createtime.set(DataTimeUtil.getSysFullTime());
        m1.projectname.set(tm.name.get());
        m1.templatename.set(tm.pcr_prj.get());
        m1.path.set(tm.path.get());
        m1.totaltime.set(tm.totaltime.get());
        m1.phase.set(0);
        m1.phasestep.set(0);
        m1.circle.set(0);
        m1.firstSecond.set(0);
        m1.runtime.set(0L);
        m1.templateno.set(templateno);
        m1.savepcr.set(true);
        m1.nextdevice.set(0);
        list.add(m1);
        return list;
    }
}
