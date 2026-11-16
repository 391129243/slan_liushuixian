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
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_QRCode;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.HeartBeatMessage;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.reflect.TypeToken;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import java.lang.reflect.Type;

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
            if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                if (WSSStart) {
                    if (ArmManager.getInstance().armCanuse()) {
                        if(!checktrayerror()) {
                            ///真正调度PCR及核提的地方，优先核提，再到PCR
                            boolean trayb = checktrayurgent();
                            if (trayb) {
                                Log.i("ArmTaskWorker", "机械臂无任务，等待调度");
                            } else {
                                Log.w("ArmTaskWorker", "托盘调度,机械臂任务中");
                            }
                        }else {
                            Log.e("ArmTaskWorker", "托盘异常调度,机械臂执行任务中");
                        }
                    } else {
                        Log.e("ArmTaskWorker", "机械臂不可用，不调度");
                    }
                } else {
                    Log.e("ArmTaskWorker", "wss资源为0，不调度");
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
            ArrayList<PipePCRModel> list = PCRManager.getInstance().pcrlist;
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    PipePCRModel m = list.get(i);
                    if ((m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                            && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE))) {
                        ArmStep step = m.armStep.get();
                        if (step == ArmStep.RUN) {
                            if (!m.runtime.isExcutestart()) {
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
            ArrayList<PipeRNAModel> list = RNAManager.getInstance().rnalist;
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    PipeRNAModel m = list.get(i);
                    if ((m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                            && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE))) {
                        ArmStep step = m.armStep.get();
                        if (step == ArmStep.RUN) {
                            if (!m.runtime.isExcutestart()) {
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
            if(m.runtime.isClosedoor()){
                boolean result = ArmManager.getInstance().closeRNADoor(index, new ArmFinishCallback() {
                    /** 关门完成回调：此处无需额外处理（关门成功与否由下方 result 判定） */
                    @Override
                    public void finish(int arm, ItemProperty aim, ItemProperty source) {

                    }

                    /** 关门失败回调：此处不做处理 */
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
    ///本方法退化为“建快照 → 委托引擎 → 回写”。行为与原实现逐行等价。
    private boolean checkrnawork(int index, int tray) {
        RnaSchedulingState s = schedulingPort.snapshotRna(index);
        if (s == null) {
            Log.e("ArmTaskWorker", "checkrnawork model数据为空 rna:" + index + " tray:" + tray);
            return true;
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
    ///本方法退化为“建快照 → 委托引擎 → 回写”。行为与原实现逐行等价。
    private boolean checkpcrwork(int index, int tray) {
        PcrSchedulingState s = schedulingPort.snapshotPcr(index);
        if (s == null) {
            Log.e("ArmTaskWorker", "checkpcrwork model数据为空 pcr:" + index + " tray:" + tray);
            return true;
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

    //第 3 步：RNA 调度引擎与其端口适配（适配到现有 Manager，行为不变）
    private final SchedulingPort schedulingPort = new AplSchedulingPort();
    private final SchedulingEngine schedulingEngine = new SchedulingEngine(schedulingPort);

    /**
     * SchedulingPort 的具体实现：把引擎的抽象调用适配到现有 Manager/门状态/回调。
     * 作为内部类可直接访问 APLManager 的 WSSStart/SelfCheck/pipeDoorModel/uIinitCallback
     * 及 createArm 系列 / startRunTemplate 等既有方法，保证行为与原 checkrnawork 逐行一致。
     */
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
            RnaSchedulingState s = new RnaSchedulingState();
            s.index = m.index.get();
            s.currentTray = m.currentTray.get() == null ? 0 : m.currentTray.get();
            s.statusFree = m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                    && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE);
            s.exist = Boolean.TRUE.equals(m.exist.get());
            s.armStep = m.armStep.get();
            s.runtime = m.runtime;
            return s;
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
            PcrSchedulingState s = new PcrSchedulingState();
            s.index = m.index.get();
            s.currentTrayKnown = m.currentTray.get() != null;
            s.currentTray = m.currentTray.get() == null ? 0 : m.currentTray.get();
            s.statusFree = m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                    && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE);
            s.exist = Boolean.TRUE.equals(m.exist.get());
            s.armStep = m.armStep.get();
            s.runtime = m.runtime;
            return s;
        }

        /** 把引擎改动（当前仅 armStep）回写到 PipePCRModel */
        @Override
        public void applyPcr(int pcrIndex, PcrSchedulingState state) {
            PipePCRModel m = PCRManager.getInstance().getPipePCRModel(pcrIndex);
            if (m != null && state != null && m.armStep.get() != state.armStep) {
                m.armStep.set(state.armStep);
            }
        }

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
                armErrorCallback.error(msg);
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
        final ArmCallbackEngine getEngine = new ArmCallbackEngine(new AplArmCallbackPort(msg, armErrorCallback));
        final int fDispatchindex = msg.getDispatchindex();
        String rstr = ArmManager.getInstance().startArmRobot(source,sourcetype,target,targettype,new ArmFinishCallback() {
            /** 取托盘完成回调：委托引擎（送达目标 RNA/PCR 后置 gettrayfinish 并启动对应模板） */
            @Override
            public void finish(int arm, ItemProperty aimitem, ItemProperty sourceitem) {
                getEngine.onGetFinish(ArmItemSnapshot.of(aimitem), ArmItemSnapshot.of(sourceitem));
            }

            /** 取托盘失败回调：委托引擎（回传错误、把该调度项标为异常 STATUS_ERROR） */
            @Override
            public void fail(int arm, ItemProperty aimitem, ItemProperty sourceitem, String error) {
                Log.e("ArmTaskWorker", "机械臂取托盘 :" + sourcetype + source +" 来源:"+ targettype + target + "  回调 fail error:" + error);
                getEngine.onGetFail(ArmItemSnapshot.of(sourceitem), source, fDispatchindex, error);
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
        final ArmCallbackEngine backEngine = new ArmCallbackEngine(new AplArmCallbackPort(msg, armErrorCallback));
        final int fTarget = target;
        final int fDispatchindex = dispatchindex;
        String rstr = ArmManager.getInstance().startArmRobot(source,sourcetype,target,targettype,new ArmFinishCallback() {

            /** 送回托盘完成回调：委托引擎（标该调度项完成，并按源设备类型回收 finishBacktray） */
            @Override
            public void finish(int arm, ItemProperty aimitem, ItemProperty sourceitem) {
                backEngine.onBackFinish(ArmItemSnapshot.of(aimitem), ArmItemSnapshot.of(sourceitem), fTarget, fDispatchindex);
            }

            /** 送回托盘失败回调：委托引擎（回传错误、把该调度项标为异常 STATUS_ERROR） */
            @Override
            public void fail(int arm, ItemProperty aimitem, ItemProperty sourceitem, String error) {
                Log.e("ArmTaskWorker", "机械臂送回托盘:" + target + " 来源："+ sourcetype + source+" 回调 fail " + error);
                backEngine.onBackFail(ArmItemSnapshot.of(aimitem), fTarget, fDispatchindex, error);
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

        final ArmCallbackEngine moveEngine = new ArmCallbackEngine(new AplArmCallbackPort(msg, armErrorCallback));
        final int fTrayindex = trayindex;
        final int fDispatchindex = dispatchindex;
        String rstr = ArmManager.getInstance().startArmRobot(source,sourcetype,target,targettype,new ArmFinishCallback() {

            /** 移动托盘完成回调：委托引擎（源设备移出 + 目标设备更新下一步调度并启动模板） */
            @Override
            public void finish(int arm, ItemProperty aimitem, ItemProperty sourceitem) {
                moveEngine.onMoveFinish(ArmItemSnapshot.of(aimitem), ArmItemSnapshot.of(sourceitem), fTrayindex, fDispatchindex);
            }

            /** 移动托盘失败回调：委托引擎（先标该调度项 STATUS_ERROR，再回传错误） */
            @Override
            public void fail(int arm, ItemProperty aimitem, ItemProperty sourceitem, String error) {
                Log.e("ArmTaskWorker", "机械臂移动托盘 回调 fail:" + error);
                moveEngine.onMoveFail(fTrayindex, fDispatchindex, error);
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
            model.runtime.setExcutefinish(true);
            DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(model.currentTray.get());
            if(dispatchModel==null) {
                Log.e("ArmTaskWorker", "checkRnaFinish 找不到调度数据 rna:" + target +" tray:"+model.currentTray.get());
            } else {
                if (dispatchModel.nextdevice.get() != null && dispatchModel.nextdevice.get() > 0) {
                    model.runtime.setMovetraystart(false);
                    model.runtime.setMovetrayfinish(false);
                    model.armStep.set(ArmStep.MOVE);
                    Log.e("ArmTaskWorker", "rna :" + target + " WORK_MOVE ");
                } else {
                    model.runtime.setBacktraystart(false);
                    model.runtime.setBacktrayfinish(false);
                    model.armStep.set(ArmStep.BACK);
                    Log.e("ArmTaskWorker", "rna :" + target + " WORK_BACK ");
                }
                if (dispatchModel.index.get() != null) {
                    int tray = model.currentTray.get();
                    int dispatchindex = dispatchModel.index.get();
                    TrayManager.getInstance().updateDsiapatchStoptime(tray, dispatchindex);
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
            Log.e("ArmTaskWorker", "PCR:"+target+" checkPcrFinish Ok");
            model.runtime.setExcutefinish(true);
            DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(model.currentTray.get());
            if (dispatchModel == null) {
                Log.e("ArmTaskWorker", "checkPcrFinish 找不到调度数据 pcr:" + target + " tray:" + model.currentTray.get());
            } else {
                int tray = model.currentTray.get();
                int dispatchindex = dispatchModel.index.get();
                if (dispatchModel.nextdevice.get() != null && dispatchModel.nextdevice.get() > 0) {
                    model.runtime.setMovetraystart(false);
                    model.runtime.setMovetrayfinish(false);
                    model.armStep.set(ArmStep.MOVE);
                    Log.e("ArmTaskWorker", "finish PCR:" + target + " WORK_MOVE ");
                } else {
                    model.runtime.setBacktraystart(false);
                    model.runtime.setBacktrayfinish(false);
                    model.armStep.set(ArmStep.BACK);
                    Log.e("ArmTaskWorker", "finish PCR:" + target + " WORK_BACK ");
                }
                TrayManager.getInstance().updateDsiapatchStoptime(tray, dispatchindex);
                TrayManager.getInstance().updateDsiapatchDeviceStatus(tray, dispatchindex, ConstantData.STATUS_FINISH);
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
            // 验证调度文件合法性
            String validationError = validateDispatchFiles(tm);
            if (validationError != null) {
                // 返回 null 表示验证失败，调用方应检查并显示错误
                Log.e(TAG, "调度文件验证失败: " + validationError);
                return null;
            }

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

    /**
     * 验证调度文件合法性
     * @param tm 模板模型
     * @return 错误信息，如果验证通过返回 null
     */
    private String validateDispatchFiles(TemplateModel tm) {
        if (tm == null) {
            return "模板数据为空";
        }

        String path = tm.path.get();
        if (path == null || path.isEmpty()) {
            return "模板路径为空";
        }

        Integer templateType = tm.templateType.get();
        if (templateType == null) {
            return "模板类型未定义";
        }

        // 根据模板类型验证相应的文件
        switch (templateType) {
            case ConstantData.TEMPLATE_FULL_SINGLE:
                return validateSingleTemplate(path, tm.single_hex.get(), tm.single_prj.get());

            case ConstantData.TEMPLATE_FULL_MULTI:
                String error = validateMultiTemplate(path,
                    tm.multi_hex_first.get(), tm.multi_prj_first.get(),
                    tm.multi_hex_second.get(), tm.multi_prj_second.get());
                return error;

            case ConstantData.TEMPLATE_RNA:
                return validateHexFile(path, tm.rna_hex.get());

            case ConstantData.TEMPLATE_PCR:
                return validatePrjFile(path, tm.pcr_prj.get());

            default:
                return "未知的模板类型: " + templateType;
        }
    }

    /**
     * 验证单一模板的文件
     */
    private String validateSingleTemplate(String path, String hexFile, String prjFile) {
        String hexError = validateHexFile(path, hexFile);
        if (hexError != null) {
            return hexError;
        }
        return validatePrjFile(path, prjFile);
    }

    /**
     * 验证多步模板的文件
     */
    private String validateMultiTemplate(String path, String hexFile1, String prjFile1,
                                         String hexFile2, String prjFile2) {
        String error = validateHexFile(path, hexFile1);
        if (error != null) {
            return error;
        }
        error = validatePrjFile(path, prjFile1);
        if (error != null) {
            return error;
        }
        error = validateHexFile(path, hexFile2);
        if (error != null) {
            return error;
        }
        return validatePrjFile(path, prjFile2);
    }

    /**
     * 验证 .hex 文件
     * 检查文件是否可以被 JSON 解析成 List<HexDebugEntity>，且 size 不为 0
     */
    private String validateHexFile(String path, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "HEX 文件名为空";
        }

        File file = new File(path, fileName);
        if (!file.exists()) {
            return "HEX 文件不存在: " + fileName;
        }

        if (!file.isFile()) {
            return "HEX 路径不是文件: " + fileName;
        }

        // 读取文件内容并尝试解析
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            String jsonContent = fileToString(inputStream);

            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                return "HEX 文件内容为空: " + fileName;
            }

            // 尝试解析为 List<HexDebugEntity>
            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();

            List<HexDebugEntity> list = gson.fromJson(jsonContent,
                new TypeToken<List<HexDebugEntity>>() {}.getType());

            if (list == null || list.size() == 0) {
                return "HEX 文件解析后数据为空: " + fileName;
            }

            return null; // 验证通过

        } catch (Exception e) {
            Log.e(TAG, "HEX 文件解析失败: " + fileName, e);
            return "HEX 文件格式错误，无法解析: " + fileName + "\n" + e.getMessage();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭文件流失败", e);
                }
            }
        }
    }

    /**
     * 验证 .prj 文件
     * 检查文件大小是否大于 0
     */
    private String validatePrjFile(String path, String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "PRJ 文件名为空";
        }

        File file = new File(path, fileName);
        if (!file.exists()) {
            return "PRJ 文件不存在: " + fileName;
        }

        if (!file.isFile()) {
            return "PRJ 路径不是文件: " + fileName;
        }

        long fileSize = file.length();
        if (fileSize == 0) {
            return "PRJ 文件大小为 0: " + fileName;
        }

        return null; // 验证通过
    }

    /**
     * 将 FileInputStream 转换为字符串
     */
    private String fileToString(FileInputStream inputStream) throws IOException {
        byte[] buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        return new String(buffer, "UTF-8");
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
