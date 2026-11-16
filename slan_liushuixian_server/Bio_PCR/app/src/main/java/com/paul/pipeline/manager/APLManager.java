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
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.model.TemplateModel;
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
import java.util.Iterator;
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
            if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
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
            ArrayList<PipePCRModel> list = PCRManager.getInstance().pcrlist;
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    PipePCRModel m = list.get(i);
                    if ((m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                            && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE))) {
                        int step = m.armStep.get();
                        if (step == ConstantData.WORK_RUN) {
                            if (Boolean.FALSE.equals(m.excutestart.get())) {
                                if (m.currentTray.get() != null) {
                                    DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                    if (dispatchModel == null) {
                                        Log.e("ArmTaskWorker", "checkpcrRun 找不到调度数据 pcr:" + m.index.get() + " tray:" + m.currentTray.get());
                                    } else {
                                        boolean result = startRunTemplate(m.index.get(), ConstantData.DEVICE_PCR,
                                                m.currentTray.get(), dispatchModel.index.get());
                                        if (result) {
                                            m.excutestart.set(true);
                                            m.excutefinish.set(false);
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
                        int step = m.armStep.get();
                        if (step == ConstantData.WORK_RUN) {
                            if (Boolean.FALSE.equals(m.excutestart.get())) {
                                if (m.currentTray.get() != null) {
                                    DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                    if(dispatchModel==null) {
                                        Log.e("ArmTaskWorker", "checkrnaRun 找不到调度数据 rna:" + m.index.get() +" tray:"+m.currentTray.get());
                                    } else {
                                        boolean result = startRunTemplate(m.index.get(), ConstantData.DEVICE_RNA, m.currentTray.get(),
                                                dispatchModel.index.get());
                                        if (result) {
                                            m.excutestart.set(true);
                                            m.excutefinish.set(false);
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
            if(Boolean.TRUE.equals(m.closedoor.get())){
                boolean result = ArmManager.getInstance().closeRNADoor(index, new ArmFinishCallback() {
                    @Override
                    public void finish(int arm, ItemProperty aim, ItemProperty source) {

                    }

                    @Override
                    public void fail(int arm, ItemProperty aim, ItemProperty source, String error) {

                    }
                });
                if(result){
                    m.closedoor.set(false);
                    return false;
                }
            }
        }
        return true;
    }

    ///处理核提调度
    private boolean checkrnawork(int index, int tray) {
        PipeRNAModel m = RNAManager.getInstance().getPipeRNAModel(index);
        if(m!=null){
            if(m.currentTray.get() != tray){
                Log.e("ArmTaskWorker", "checkrnawork 托盘数据不一致 rna :" +index +" rnatray"+ m.currentTray.get() + " tray:"+tray);
                return true;
            }

            int step = m.armStep.get();
            Log.i("ArmTaskWorker", "rna:" + index + " armStep:" + step + " status:" + m.status.get() + " linuxStatus:" + m.linuxStatus.get());

            // WORK_BACK 和 WORK_MOVE 无需等待设备空闲，单独处理（避免被 status 门控拦截）
            if (step == ConstantData.WORK_BACK) {
                if (Boolean.FALSE.equals(m.backtraystart.get())) {
                    if (!getPipeDoorModel().doorhold.get()) {
                        if (WSSStart) {
                            Log.e("ArmTaskWorker", "【RNA还盘触发】createArmBackTray rna:" + m.index.get() + " tray:" + tray);
                            int trayindex = m.currentTray.get();
                            DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(trayindex);
                            if (dispatchModel == null) {
                                Log.e("ArmTaskWorker", "【RNA还盘断点2】WORK_BACK 找不到调度数据 rna:" + m.index.get() + " tray:" + trayindex);
                            } else {
                                int dispatchindex = dispatchModel.index.get();
                                boolean result = createArmBackTray(m.index.get(), ConstantData.DEVICE_RNA, trayindex, dispatchindex);
                                if (result) {
                                    TrayManager.getInstance().updateDsiapatchDeviceStatus(trayindex, dispatchindex, ConstantData.STATUS_FINISH);
                                    m.backtraystart.set(true);
                                    m.backtrayfinish.set(false);
                                    Log.w("ArmTaskWorker", "【RNA还盘成功】createArmBackTray 成功 rna:" + m.index.get()+ " tray:" + trayindex);
                                    return false;
                                } else {
                                    Log.e("ArmTaskWorker", "【RNA还盘失败】createArmBackTray 失败 rna:" + m.index.get()+ " tray:" + trayindex);
                                }
                            }
                        } else {
                            Log.w("ArmTaskWorker", "【RNA还盘断点3】RNA WORK_BACK WSSStart:" + WSSStart);
                        }
                    } else {
                        Log.w("ArmTaskWorker", "【RNA还盘断点4】RNA WORK_BACK opendoor");
                    }
                } else {
                    Log.w("ArmTaskWorker", "【RNA还盘断点5】RNA WORK_BACK backtraystart is true");
                }
                return true; // WORK_BACK 处理完成，不再往下走
            }

            if (step == ConstantData.WORK_MOVE) {
                if (Boolean.FALSE.equals(m.movetraystart.get())) {
                    if (!getPipeDoorModel().doorhold.get()) {
                        if (WSSStart) {
                            int trayindex = m.currentTray.get();
                            DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(trayindex);
                            if (dispatchModel == null) {
                                Log.e("ArmTaskWorker", "【RNA移盘断点】WORK_MOVE 找不到调度数据 rna:" + m.index.get() + " tray:" + trayindex);
                            } else {
                                int dispatchindex = dispatchModel.index.get();
                                // 动态查找空闲 PCR（与原逻辑保持一致）
                                PipePCRModel targetPcr = PCRManager.getInstance().getFreeModel();
                                if (targetPcr != null) {
                                    boolean result = createArmMoveTray(m.index.get(), ConstantData.DEVICE_RNA, targetPcr.index.get(), ConstantData.DEVICE_PCR, trayindex, dispatchindex);
                                    if (result) {
                                        TrayManager.getInstance().updateDsiapatchDeviceStatus(trayindex, dispatchindex, ConstantData.STATUS_FINISH);
                                        m.movetraystart.set(true);
                                        m.movetrayfinish.set(false);
                                        Log.w("ArmTaskWorker", "【RNA移盘成功】createArmMoveTray 成功 rna:" + m.index.get() + " → pcr:" + targetPcr.index.get());
                                        return false;
                                    } else {
                                        Log.e("ArmTaskWorker", "【RNA移盘失败】createArmMoveTray 失败 rna:" + m.index.get() + " pcr:" + targetPcr.index.get());
                                    }
                                } else {
                                    // 找不到空闲 PCR，转为还盘
                                    m.backtraystart.set(false);
                                    m.backtrayfinish.set(false);
                                    m.armStep.set(ConstantData.WORK_BACK);
                                    Log.e("ArmTaskWorker", "【RNA移盘断点】WORK_MOVE 找不到空闲 PCR → 转为 WORK_BACK rna:" + m.index.get());
                                }
                            }
                        } else {
                            Log.w("ArmTaskWorker", "【RNA移盘断点】RNA WORK_MOVE WSSStart:" + WSSStart);
                        }
                    } else {
                        Log.w("ArmTaskWorker", "【RNA移盘断点】RNA WORK_MOVE opendoor");
                    }
                }
                return true; // WORK_MOVE 处理完成，不再往下走
            }

            // 其他 step 需要 status == FREE 门控
            if ((m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                && m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE))) {
            switch (step) {
                case ConstantData.WORK_FREE:
                    if (Boolean.TRUE.equals(m.uiinit.get())) {
                        //TODO fordebugtest
//                        if(false){
                        if(m.exist.get()){
                            Log.w("ArmTaskWorker", "存在托盘，不可初始化 rna:" + m.index.get());
                        }else {
                            m.uiinit.set(false);
                            m.usererror.set(0);
                            if (uIinitCallback != null) {
                                uIinitCallback.finish(ConstantData.DEVICE_RNA, m.index.get());
                                Log.w("ArmTaskWorker", "界面初始化 成功 rna:" + m.index.get());
                            }
                            Log.i("ArmTaskWorker", "界面初始化 rna " + m.index.get());
                        }
                    }
                    break;
                case ConstantData.WORK_RUN:
                    if (Boolean.FALSE.equals(m.excutestart.get())) {
                        if (WSSStart&&SelfCheck) {
                            DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                            if(dispatchModel==null) {
                                Log.e("ArmTaskWorker", "WORK_RUN 找不到调度数据 rna:" + m.index.get() +" tray:"+m.currentTray.get());
                            } else {
                                int dispatchindex = dispatchModel.index.get();
                                boolean result = startRunTemplate(m.index.get(), ConstantData.DEVICE_RNA, tray, dispatchindex);
                                if (result) {
                                    m.excutestart.set(true);
                                    m.excutefinish.set(false);
                                    Log.w("ArmTaskWorker", "startRunTemplate 成功 rna:" + m.index.get());
                                } else {
                                    Log.e("ArmTaskWorker", "startRunTemplate 失败 rna:" + m.index.get());
                                }
                            }
                        }else{
                            Log.w("ArmTaskWorker", "rna WORK_RUN WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
                        }
                    }
                    break;
                case ConstantData.WORK_GET:
                    if (Boolean.FALSE.equals(m.gettraystart.get())) {
                        if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                            if (WSSStart&&SelfCheck) {
                                //TODO fordebugtest
//                                if (true) {
                                if (ArmManager.getInstance().armCanuse()) {
                                    DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                    if(dispatchModel==null) {
                                        Log.e("ArmTaskWorker", "WORK_GET 找不到调度数据 rna:" + m.index.get() +" tray:"+m.currentTray.get());
                                    } else {
                                        boolean result = createArmTaskGetTray(dispatchModel);
                                        if (result) {
                                            m.gettraystart.set(true);
                                            m.gettrayfinish.set(false);
                                            Log.w("ArmTaskWorker", "createArmTaskGetTray 成功 rna:" + m.index.get()+" tray:"+m.currentTray.get());
                                            return false;
                                        } else {
                                            Log.e("ArmTaskWorker", "createArmTaskGetTray 失败 rna:" + m.index.get()+" tray:"+m.currentTray.get());
                                        }
                                    }
                                }
                            }else{
                                Log.w("ArmTaskWorker", "rna WORK_GET WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
                            }
                        }
                    }
                    break;
                case ConstantData.WORK_MOVE:
                    // 已在前面单独处理，不会走到这里
                    Log.w("ArmTaskWorker", "【异常】RNA WORK_MOVE 走到了 switch 内部，说明 status 已经是 FREE");
                    if (Boolean.FALSE.equals(m.movetraystart.get())) {
                        if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                            if (WSSStart&&SelfCheck) {
                                //TODO fordebugtest
//                                if (true) {
                                if (ArmManager.getInstance().armCanuse()) {
                                    Log.e("ArmTaskWorker", "createArmMoveTray rna:" + m.index.get());
                                    DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                    if(dispatchModel==null) {
                                        Log.e("ArmTaskWorker", "WORK_MOVE 找不到调度数据 rna:" + m.index.get() +" tray:"+m.currentTray.get());
                                    } else {
                                        int dispatchindex = dispatchModel.index.get();
                                        int trayindex = m.currentTray.get();
                                        PipePCRModel rm = PCRManager.getInstance().getFreeModel();
                                        if (rm != null) {
                                            boolean result = createArmMoveTray(m.index.get(), ConstantData.DEVICE_RNA, rm.index.get(), ConstantData.DEVICE_PCR, trayindex,dispatchindex);
                                            if (result) {
                                                TrayManager.getInstance().updateDsiapatchDeviceStatus(trayindex,dispatchindex,ConstantData.STATUS_FINISH);
                                                m.movetraystart.set(true);
                                                m.movetrayfinish.set(false);
                                                Log.w("ArmTaskWorker", "createArmMoveTray 成功 rna:" + m.index.get()+" pcr:"+ rm.index.get());
                                                return false;
                                            } else {
                                                Log.e("ArmTaskWorker", "createArmMoveTray 失败 rna:" + m.index.get()+" pcr:"+ rm.index.get());
                                            }
                                        }else{
                                            m.backtraystart.set(false);
                                            m.backtrayfinish.set(false);
                                            m.armStep.set(ConstantData.WORK_BACK);
                                            Log.e("ArmTaskWorker", "WORK_MOVE rna :" + m.index.get() + " 找不到空闲pcr-还托盘WORK_BACK ");
                                        }
                                    }
                                }else{
                                    Log.w("ArmTaskWorker", "rna WORK_MOVE arm busy");
                                }
                            }else{
                                Log.w("ArmTaskWorker", "rna WORK_MOVE WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
                            }
                        }else{
                            Log.w("ArmTaskWorker", "rna WORK_MOVE opendoor");
                        }
                    }else{
                        Log.w("ArmTaskWorker", "rna WORK_MOVE movetraystart is true");
                    }
                    break;
                case ConstantData.WORK_BACK:
                    // 已在前面单独处理，不会走到这里
                    Log.w("ArmTaskWorker", "【异常】RNA WORK_BACK 走到了 switch 内部，说明 status 已经是 FREE");
                    if (Boolean.FALSE.equals(m.backtraystart.get())) {
                        if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                            if (WSSStart) {
                                //TODO fordebugtest
//                                if (true) {
                                if (ArmManager.getInstance().armCanuse()) {
                                    int trayindex = m.currentTray.get();
                                    Log.e("ArmTaskWorker", "createArmBackTray rna:" + m.index.get());
                                    DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(trayindex);
                                    if(dispatchModel==null) {
                                        Log.e("ArmTaskWorker", "WORK_BACK 找不到调度数据 rna:" + m.index.get() +" tray:"+trayindex);
                                    } else {
                                        int dispatchindex = dispatchModel.index.get();
                                        boolean result = createArmBackTray(m.index.get(), ConstantData.DEVICE_RNA, trayindex, dispatchindex);
                                        if (result) {
                                            TrayManager.getInstance().updateDsiapatchDeviceStatus(trayindex,dispatchindex,ConstantData.STATUS_FINISH);
                                            m.backtraystart.set(true);
                                            m.backtrayfinish.set(false);
                                            Log.w("ArmTaskWorker", "checkrnawork createArmBackTray 成功 rna:" + m.index.get()+" tray:"+trayindex);
                                            return false;
                                        } else {
                                            Log.e("ArmTaskWorker", "checkrnawork createArmBackTray 失败 rna:" + m.index.get()+" tray:"+trayindex);
                                        }
                                    }
                                }else{
                                    Log.w("ArmTaskWorker", "rna WORK_BACK arm busy");
                                }
                            }else{
                                Log.w("ArmTaskWorker", "rna WORK_BACK WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
                            }
                        }else{
                            Log.w("ArmTaskWorker", "rna WORK_BACK opendoor");
                        }
                    }else{
                        Log.w("ArmTaskWorker", "rna WORK_BACK start is true");
                    }
                    break;
                }
            }
        }else{
            Log.e("ArmTaskWorker", "checkrnawork model数据为空 rna:" + index + " tray:" + tray);
        }
        return true;//默认是空闲
    }

    ///处理PCR调度
    private boolean checkpcrwork(int index, int tray) {
        PipePCRModel m = PCRManager.getInstance().getPipePCRModel(index);
        if(m!=null) {
            if (m.currentTray.get() != null) {
                if (m.currentTray.get() != tray) {
                    Log.e("ArmTaskWorker", "checkpcrwork 托盘数据不一致 pcr:" + index + " pcrtray" + m.currentTray.get() + " tray:" + tray);
                    return true;
                }
            }

            int step = m.armStep.get();
            Log.i("ArmTaskWorker", "pcr:" + index + " armStep:" + step + " status:" + m.status.get() + " linuxStatus:" + m.linuxStatus.get());

            // WORK_BACK 无需等待设备空闲，单独处理（避免被 status 门控拦截）
            if (step == ConstantData.WORK_BACK) {
                if (Boolean.FALSE.equals(m.backtraystart.get())) {
                    if (!getPipeDoorModel().doorhold.get()) {
                        if (WSSStart) {
                            Log.e("ArmTaskWorker", "【还盘触发】createArmBackTray pcr:" + m.index.get() + " tray:" + tray);
                            int trayindex = m.currentTray.get();
                            DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(trayindex);
                            if (dispatchModel == null) {
                                Log.e("ArmTaskWorker", "【还盘断点2】WORK_BACK 找不到调度数据 pcr:" + m.index.get() + " tray:" + trayindex);
                            } else {
                                int dispatchindex = dispatchModel.index.get();
                                boolean result = createArmBackTray(m.index.get(), ConstantData.DEVICE_PCR, trayindex, dispatchindex);
                                if (result) {
                                    TrayManager.getInstance().updateDsiapatchDeviceStatus(trayindex, dispatchindex, ConstantData.STATUS_FINISH);
                                    m.backtraystart.set(true);
                                    m.backtrayfinish.set(false);
                                    Log.w("ArmTaskWorker", "【还盘成功】createArmBackTray 成功 pcr:" + m.index.get()+ " tray:" + trayindex);
                                    return false;
                                } else {
                                    Log.e("ArmTaskWorker", "【还盘失败】createArmBackTray 失败 pcr:" + m.index.get()+ " tray:" + trayindex);
                                }
                            }
                        } else {
                            Log.w("ArmTaskWorker", "【还盘断点3】PCR WORK_BACK WSSStart:" + WSSStart);
                        }
                    } else {
                        Log.w("ArmTaskWorker", "【还盘断点4】PCR WORK_BACK opendoor");
                    }
                } else {
                    Log.w("ArmTaskWorker", "【还盘断点5】PCR WORK_BACK backtraystart is true");
                }
                return true; // WORK_BACK 处理完成，不再往下走
            }

            // 其他 step 需要 status == FREE 门控
            if ((m.status.get() != null && m.status.get().equalsIgnoreCase(ConstantData.str_FREE) &&
                    m.linuxStatus.get() != null && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE))) {
                Log.i("ArmTaskWorker", " pcr: " + index + " armStep:" + step);
                switch (step) {
                    case ConstantData.WORK_FREE:
                        if (Boolean.TRUE.equals(m.uiinit.get())) {
                            //TODO fordebugtest
//                            if(false){
                            if(m.exist.get()){
                                Log.w("ArmTaskWorker", "存在托盘，不可初始化 pcr:" + m.index.get());
                            }else {
                                m.uiinit.set(false);
                                m.usererror.set(0);
                                if (uIinitCallback != null) {
                                    uIinitCallback.finish(ConstantData.DEVICE_PCR, m.index.get());
                                    Log.w("ArmTaskWorker", "界面初始化成功 pcr:" + m.index.get());
                                }
                                Log.i("ArmTaskWorker", "界面初始化 pcr " + m.index.get());
                            }
                        }
                        break;
                    case ConstantData.WORK_GET:
                        if (Boolean.FALSE.equals(m.gettraystart.get())) {
                            if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                                if (WSSStart && SelfCheck) {
                                    //TODO fordebugtest
//                                    if (true) {
                                    if (ArmManager.getInstance().armCanuse()) {
                                        DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                        if (dispatchModel == null) {
                                            Log.e("ArmTaskWorker", "WORK_GET 找不到调度数据 pcr:" + m.index.get() + " tray:" + m.currentTray.get());
                                        } else {
                                            boolean result = createArmTaskGetTray(dispatchModel);
                                            if (result) {
                                                m.gettraystart.set(true);
                                                m.gettrayfinish.set(false);
                                                Log.w("ArmTaskWorker", "createArmTaskGetTray 成功 pcr:" + m.index.get()+" tray:"+m.currentTray.get());
                                                return false;
                                            } else {
                                                Log.e("ArmTaskWorker", "createArmTaskGetTray 失败 pcr:" + m.index.get()+" tray:"+m.currentTray.get());
                                            }
                                        }
                                    }
                                }else{
                                    Log.w("ArmTaskWorker", "PCR WORK_GET WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
                                }
                            }
                        }
                        break;
                    case ConstantData.WORK_RUN:
                        if (Boolean.FALSE.equals(m.excutestart.get())) {
                            Log.i("ArmTaskWorker", "【启动PCR】WORK_RUN pcr:" + m.index.get() + " tray:" + m.currentTray.get() + " excutestart:false，准备启动模板");
                            if (WSSStart && SelfCheck) {
                                DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                if (dispatchModel == null) {
                                    Log.e("ArmTaskWorker", "【启动PCR断点】找不到调度数据 pcr:" + m.index.get() + " tray:" + m.currentTray.get() + " dispatchindex:" + m.dispatchindex.get());
                                    Log.e("ArmTaskWorker", "【启动PCR断点】可能原因：1.调度数据被覆盖 2.托盘调度索引不匹配 3.托盘数据异常");
                                } else {
                                    int dispatchindex = dispatchModel.index.get();
                                    Log.i("ArmTaskWorker", "【启动PCR】找到调度数据 pcr:" + m.index.get() + " tray:" + m.currentTray.get() + " dispatch:" + dispatchindex);
                                    boolean result = startRunTemplate(m.index.get(), ConstantData.DEVICE_PCR, tray, dispatchindex);
                                    if (result) {
                                        m.excutestart.set(true);
                                        m.excutefinish.set(false);
                                        Log.w("ArmTaskWorker", "【启动PCR成功】pcr:" + m.index.get() + " tray:" + m.currentTray.get());
                                    } else {
                                        Log.e("ArmTaskWorker", "【启动PCR失败】startRunTemplate 返回 false，pcr:" + m.index.get());
                                    }
                                }
                            }else{
                                Log.w("ArmTaskWorker", "【启动PCR断点】PCR WORK_RUN 门控未通过 WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
                            }
                        } else {
                            Log.v("ArmTaskWorker", "【启动PCR】pcr:" + m.index.get() + " excutestart:true，已启动，跳过");
                        }
                        break;
                    case ConstantData.WORK_MOVE:
                        if (Boolean.FALSE.equals(m.movetraystart.get())) {
                            if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                                if (WSSStart && SelfCheck) {
                                    //TODO fordebugtest
//                                    if (true) {
                                    if (ArmManager.getInstance().armCanuse()) {
                                        Log.e("ArmTaskWorker", "WORK_MOVE pcr:" + m.index.get());
                                        DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
                                        if (dispatchModel == null) {
                                            Log.e("ArmTaskWorker", "WORK_MOVE 找不到调度数据 pcr:" + m.index.get() + " tray:" + m.currentTray.get());
                                        } else {
                                            int dispatchindex = dispatchModel.index.get();
                                            int trayindex = m.currentTray.get();
                                            PipeRNAModel rm = RNAManager.getInstance().getFreeModel();
                                            if (rm != null) {
                                                Log.e("ArmTaskWorker", "找到空闲rna: "+ rm.index.get());
                                                boolean result = createArmMoveTray(m.index.get(), ConstantData.DEVICE_PCR, rm.index.get(), ConstantData.DEVICE_RNA, trayindex, dispatchindex);
                                                if (result) {
                                                    TrayManager.getInstance().updateDsiapatchDeviceStatus(trayindex, dispatchindex, ConstantData.STATUS_FINISH);
                                                    m.movetraystart.set(true);
                                                    m.movetrayfinish.set(false);
                                                    Log.w("ArmTaskWorker", "createArmMoveTray 成功 pcr:" + m.index.get() +" rna:"+rm.index.get());
                                                    return false;
                                                } else {
                                                    Log.e("ArmTaskWorker", "createArmMoveTray 失败 pcr:" + m.index.get()+" rna:"+rm.index.get());
                                                }
                                            }else{
                                                m.backtraystart.set(false);
                                                m.backtrayfinish.set(false);
                                                m.armStep.set(ConstantData.WORK_BACK);
                                                Log.e("ArmTaskWorker", "WORK_MOVE pcr :" + m.index.get() + " 找不到空闲rna-还托盘WORK_BACK ");
                                            }
                                        }
                                    }else{
                                        Log.w("ArmTaskWorker", "PCR WORK_MOVE arm busy");
                                    }
                                }else{
                                    Log.w("ArmTaskWorker", "PCR WORK_MOVE WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
                                }
                            }else{
                                Log.w("ArmTaskWorker", "PCR WORK_MOVE opendoor");
                            }
                        }else{
                            Log.w("ArmTaskWorker", "PCR WORK_MOVE movetraystart is true");
                        }
                        break;
                    case ConstantData.WORK_BACK:
                        // 已在前面单独处理，不会走到这里
                        Log.w("ArmTaskWorker", "【异常】WORK_BACK 走到了 switch 内部，说明 status 已经是 FREE");
                        if (Boolean.FALSE.equals(m.backtraystart.get())) {
                            if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                                if (WSSStart) {
                                    //TODO fordebugtest
//                                    if (true) {
                                    if (ArmManager.getInstance().armCanuse()) {
                                        Log.e("ArmTaskWorker", "createArmBackTray pcr:" + m.index.get());
                                        int trayindex = m.currentTray.get();
                                        DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(trayindex);
                                        if (dispatchModel == null) {
                                            Log.e("ArmTaskWorker", "WORK_BACK 找不到调度数据 pcr:" + m.index.get() + " tray:" + trayindex);
                                        } else {
                                            int dispatchindex = dispatchModel.index.get();
                                            boolean result = createArmBackTray(m.index.get(), ConstantData.DEVICE_PCR, trayindex, dispatchindex);
                                            if (result) {
                                                TrayManager.getInstance().updateDsiapatchDeviceStatus(trayindex, dispatchindex, ConstantData.STATUS_FINISH);
                                                m.backtraystart.set(true);
                                                m.backtrayfinish.set(false);
                                                Log.w("ArmTaskWorker", "createArmBackTray 成功 pcr:" + m.index.get()+ " tray:" + trayindex);
                                                return false;
                                            } else {
                                                Log.e("ArmTaskWorker", "createArmBackTray 失败 pcr:" + m.index.get()+ " tray:" + trayindex);
                                            }
                                        }
                                    }else{
                                        Log.w("ArmTaskWorker", "PCR WORK_BACK arm busy");
                                    }
                                }else{
                                    Log.w("ArmTaskWorker", "PCR WORK_BACK WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
                                }
                            }else{
                                Log.w("ArmTaskWorker", "PCR WORK_BACK opendoor");
                            }
                        }else{
                            Log.w("ArmTaskWorker", "PCR WORK_BACK start is true");
                        }
                        break;
                }
            }
        }else{
            Log.e("ArmTaskWorker", "checkpcrwork model数据为空 pcr:" + index + " tray:" + tray);
        }
        return true;
    }

    private void checktrayend() {
        ArrayList<PipeTrayModel> trays = TrayManager.getInstance().getTraylist();
        for (int i = 0; i < trays.size(); i++) {
            PipeTrayModel tray = trays.get(i);
            if (tray != null) {
                int trayindex = tray.index.get();
                int traydispatchindex = tray.dispatchindex.get();
                ArrayList<DispatchModel> dispatchlist = tray.dispatchlist;
                if (dispatchlist != null) {
                    int traydispatchsize = dispatchlist.size();
                    Log.i("ArmTaskWorker", "tray" + trayindex + " dispatchindex " + traydispatchindex + " dispatchlistsize " + traydispatchsize);
                    if (tray.status.get() != null) {
                        if (tray.status.get().equalsIgnoreCase(ConstantData.str_RUN)) {
                            if (traydispatchindex > 0 && traydispatchindex > traydispatchsize) {
                                tray.flagstatus.set(ConstantData.FLAG_FREE);
                                //调度结束，分析是否完成
                                tray.status.set(ConstantData.str_FINISH);
                                createLedTask(trayindex, 0);// 关闭led
                                Log.w("ArmTaskWorker", "tray " + trayindex + " 调度结束！");
                                TrayManager.getInstance().saveModel(trayindex);
                            } else if (traydispatchindex <= 0) {
                                Log.i("ArmTaskWorker", "未开始 dispatchindex 0 tray:" + tray.index.get());
                            } else if (traydispatchindex > 0 && traydispatchindex <= traydispatchsize) {
                                for (int j = 0; j < dispatchlist.size(); j++) {
                                    DispatchModel dm = dispatchlist.get(j);
                                    if (traydispatchindex == dm.index.get()) {
                                        Log.i("ArmTaskWorker", "checktrayend tray " + trayindex + " DispatchModel " + dm.index.get() + " status " + dm.status.get());
                                        if (dm.status.get() == ConstantData.STATUS_FINISH) {
                                            if((traydispatchindex + 1) <= traydispatchsize) {
                                                tray.dispatchindex.set(traydispatchindex + 1);
                                            }else{
                                                tray.flagstatus.set(ConstantData.FLAG_FREE);
                                                //调度结束
                                                tray.status.set(ConstantData.str_FINISH);
                                                Log.w("ArmTaskWorker", "tray " + trayindex + " 整盘结束调度完成");
                                                createLedTask(trayindex, 0);// 关闭led
                                                TrayManager.getInstance().saveModel(trayindex);
                                            }
                                        } else if (dm.status.get() == ConstantData.STATUS_RUN) {
                                            if(dm.targettype.get()!=null) {
                                                if (dm.targettype.get() == ConstantData.TYPE_RNA) {
                                                    PipeRNAModel rnam = RNAManager.getInstance().getPipeRNAModel(dm.target.get());
                                                    if (rnam != null) {
                                                        if (rnam.backtrayfinish.get() != null && rnam.backtrayfinish.get()
                                                                && rnam.armStep.get() == ConstantData.WORK_BACK) {
                                                            rnam.armStep.set(ConstantData.WORK_FREE);
                                                            rnam.uiinit.set(true);//界面初始化
                                                            Log.w("ArmTaskWorker", "调度单列完成 tray " + trayindex + " rna :" + dm.target.get());
                                                            dm.status.set(ConstantData.STATUS_FINISH);
                                                            if ((traydispatchindex + 1) <= traydispatchsize) {
                                                                tray.dispatchindex.set(traydispatchindex + 1);
                                                            } else {
                                                                tray.flagstatus.set(ConstantData.FLAG_FREE);
                                                                //调度结束，分析是否完成
                                                                tray.status.set(ConstantData.str_FINISH);
                                                                Log.w("ArmTaskWorker", "tray " + trayindex + " 整盘调度完成");
                                                                createLedTask(trayindex, 0);// 关闭led
                                                            }
                                                            TrayManager.getInstance().saveModel(trayindex);
                                                        }
                                                    }
                                                } else if (dm.targettype.get() == ConstantData.TYPE_PCR) {
                                                    PipePCRModel pcrm = PCRManager.getInstance().getPipePCRModel(dm.target.get());
                                                    if (pcrm != null) {
                                                        if (pcrm.backtrayfinish.get() != null && pcrm.backtrayfinish.get()
                                                                && pcrm.armStep.get() == ConstantData.WORK_BACK) {
                                                            pcrm.armStep.set(ConstantData.WORK_FREE);
                                                            pcrm.uiinit.set(true);//界面初始化
                                                            Log.w("ArmTaskWorker", "调度单列完成 tray " + tray.index.get() + " pcr :" + dm.target.get());
                                                            dm.status.set(ConstantData.STATUS_FINISH);
                                                            if ((traydispatchindex + 1) <= traydispatchsize) {
                                                                tray.dispatchindex.set(traydispatchindex + 1);
                                                            } else {
                                                                tray.flagstatus.set(ConstantData.FLAG_FREE);
                                                                //调度结束，分析是否完成
                                                                tray.status.set(ConstantData.str_FINISH);
                                                                Log.w("ArmTaskWorker", "tray " + trayindex + " 整盘调度完成");
                                                                createLedTask(trayindex, 0);// 关闭led
                                                            }
                                                            TrayManager.getInstance().saveModel(trayindex);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    } else {
                        Log.w("ArmTaskWorker", "checktrayend tray " + trayindex + " 状态为空！");
                    }
                } else {
                    Log.w("ArmTaskWorker", "checktrayend tray " + trayindex + " 调度表为空！");
                }
            }
        }
    }
    private boolean checktrayerror() {
        try{
            ArrayList<PipeTrayModel> trays = TrayManager.getInstance().getTraylist();
            //异常处理
            for (int i = 0; i < trays.size(); i++) {
                PipeTrayModel tray = trays.get(i);
                if (tray.status.get() != null && tray.status.get().equalsIgnoreCase(ConstantData.str_ERROR) && tray.index.get()!=null) {
                    boolean result = checktrayerror(tray.index.get());
                    if (!result) {
                        Log.e("ArmTaskWorker", "error tray :" + tray.index.get());
                        return true;
                    }
                }
            }
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    private boolean checktrayurgent() {
        boolean result = true;
        result = checkrnaclosedoor(1);
        if (!result) {
            Log.e("ArmTaskWorker", "rna: 1  closedoor");
            return false;
        }
        result = checkrnaclosedoor(2);
        if (!result) {
            Log.e("ArmTaskWorker", "rna: 2  closedoor");
            return false;
        }
        if(SelfCheck) {
            ArrayList<PipeTrayModel> trays = TrayManager.getInstance().getTraylist();
            //加急RNA
            for (int i = 0; i < trays.size(); i++) {
                PipeTrayModel tray = trays.get(i);
                if (tray.urgent.get() != null && Boolean.TRUE.equals(tray.urgent.get())) {
                    result = checktrayRnawork(tray.index.get());
                    if (!result) {
                        Log.e("ArmTaskWorker", "urgent rna tray:" + tray.index.get());
                        return false;
                    }
                }
            }
            //加急PCR
            for (int i = 0; i < trays.size(); i++) {
                PipeTrayModel tray = trays.get(i);
                if (tray.urgent.get() != null && Boolean.TRUE.equals(tray.urgent.get())) {
                    result = checktrayPcrwork(tray.index.get());
                    if (!result) {
                        Log.e("ArmTaskWorker", "urgent pcr tray:" + tray.index.get());
                        return false;
                    }
                }
            }
            //普通RNA
            for (int i = 0; i < trays.size(); i++) {
                PipeTrayModel tray = trays.get(i);
                result = checktrayRnawork(tray.index.get());
                if (!result) {
                    Log.e("ArmTaskWorker", "normal rna tray:" + tray.index.get());
                    return false;
                }
            }
            //普通PCR
            for (int i = 0; i < trays.size(); i++) {
                PipeTrayModel tray = trays.get(i);
                result = checktrayPcrwork(tray.index.get());
                if (!result) {
                    Log.e("ArmTaskWorker", "normal pcr tray:" + tray.index.get());
                    return false;
                }
            }
        }else{
            Log.w("ArmTaskWorker", "checktrayurgent WSSStart:" + WSSStart +" SelfCheck:"+SelfCheck);
        }
        return result;
    }

    /***
     * 处理异常，取回托盘
     * @param trayindex
     * @return
     */
    private boolean checktrayerror(int trayindex) {
        boolean result = true;//默认是空闲
        PipeTrayModel tray = TrayManager.getInstance().getPipeTrayModel(trayindex);
        if (tray != null) {
            if (tray.dispatchlist != null && tray.errorDispatchindex.get()!=null) {
                ArrayList<DispatchModel> dispatchlist = tray.dispatchlist;
                int edindex = tray.errorDispatchindex.get();
                int traydispatchsize = dispatchlist.size();
                Log.i("ArmTaskWorker", "tray" + trayindex + " error_dispatchindex " + edindex + " dispatchlistsize " + traydispatchsize);
                if (tray.status.get() != null) {
                    if (tray.status.get().equalsIgnoreCase(ConstantData.str_ERROR)) {
                        if (edindex > 0 && traydispatchsize > 0 && edindex <= traydispatchsize) {
                            for (int j = 0; j < traydispatchsize; j++) {
                                DispatchModel dm = dispatchlist.get(j);
                                if (edindex == dm.index.get() || (dm.status.get()!=null && dm.status.get() == ConstantData.STATUS_ERROR) ||
                                        (dm.devicestatus.get()!=null &&  dm.devicestatus.get() == ConstantData.STATUS_ERROR)) {
                                        Log.i("ArmTaskWorker", "tray" + trayindex + " error_DispatchModel_index " + dm.index.get() + " status " + dm.status.get());
                                        if (dm.targettype.get() == ConstantData.TYPE_RNA) {
                                            Log.w("ArmTaskWorker", "调度单列error tray " + trayindex + " rna :" + dm.target.get());
                                            if(dm.target.get()!=null) {
                                                boolean rnab = checkrnawork(dm.target.get(), trayindex);
                                                PipeRNAModel rnam = RNAManager.getInstance().getPipeRNAModel(dm.target.get());
                                                if (rnam != null) {
                                                    if (rnam.backtrayfinish.get() && rnam.armStep.get() == ConstantData.WORK_BACK
                                                    //TODO fordebugtest
//                                                        ) {
                                                            && tray.exist.get()!=null && tray.exist.get()) {
                                                        tray.flagstatus.set(ConstantData.FLAG_FREE);
                                                        rnam.armStep.set(ConstantData.WORK_FREE);
                                                        Log.w("ArmTaskWorker", "tray " + trayindex + " rna "+ rnam.index.get() +" 调度完成");
                                                        rnam.uiinit.set(true);//界面初始化
                                                    }
                                                }
                                                Log.w("ArmTaskWorker", "tray error " + trayindex + " rna"+ dm.target.get() +" 调度 是否空闲:" + rnab);
                                                if (!rnab) {
                                                    Log.w("ArmTaskWorker", "rna " + dm.target.get() + " ERROR work ");
                                                    return false;
                                                }
                                            }
                                        } else if (dm.targettype.get() == ConstantData.TYPE_PCR) {
                                            Log.w("ArmTaskWorker", "调度单列error tray " + trayindex + " pcr :" + dm.target.get());
                                            if (dm.target.get() != null) {
                                                boolean pcrb = checkpcrwork(dm.target.get(), trayindex);
                                                PipePCRModel pcrm = PCRManager.getInstance().getPipePCRModel(dm.target.get());
                                                if (pcrm != null) {
                                                    if (pcrm.backtrayfinish.get() && pcrm.armStep.get() == ConstantData.WORK_BACK
                                                            //TODO fordebugtest
//                                                            ) {
                                                            && tray.exist.get()!=null && tray.exist.get()) {
                                                        tray.flagstatus.set(ConstantData.FLAG_FREE);
                                                        pcrm.armStep.set(ConstantData.WORK_FREE);
                                                        Log.w("ArmTaskWorker", "tray " + trayindex + " pcr "+ pcrm.index.get() +" error work 调度完成");
                                                        pcrm.uiinit.set(true);//界面初始化
                                                    }
                                                }
                                                Log.w("ArmTaskWorker", "tray error " + trayindex + " pcr"+dm.target.get()+" 调度 是否空闲:" + pcrb);
                                                if (!pcrb) {
                                                    Log.w("ArmTaskWorker", "pcr " + dm.target.get() + " ERROR work ");
                                                    return false;
                                                }
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                    }
                } else {
                    Log.w("ArmTaskWorker", "checktrayerror tray " + trayindex + " 状态为空！");
                }
            } else {
                Log.w("ArmTaskWorker", "checktrayerror tray " + trayindex + " 调度表为空！");
            }
        }

        return result;
    }

    /**
     * 分配盘位和调度机械臂
     *
     * @param trayindex
     * @return
     */
    private boolean checktrayRnawork(int trayindex) {
        boolean result = true;//默认是空闲
        PipeTrayModel tray = TrayManager.getInstance().getPipeTrayModel(trayindex);
        if (tray != null) {
            int traydispatchindex = tray.dispatchindex.get();
            ArrayList<DispatchModel> dispatchlist = tray.dispatchlist;
            if (dispatchlist != null) {
                int traydispatchsize = dispatchlist.size();
                Log.i("ArmTaskWorker", "tray" + trayindex + " dispatchindex " + traydispatchindex + " dispatchlistsize " + traydispatchsize);
                if (tray.status.get() != null && tray.status.get().equalsIgnoreCase(ConstantData.str_RUN)) {
                    if (traydispatchindex > 0 && traydispatchindex <= traydispatchsize) {
                            for (int j = 0; j < dispatchlist.size(); j++) {
                                DispatchModel dm = dispatchlist.get(j);
                                if (traydispatchindex == dm.index.get()) {
                                    Log.i("ArmTaskWorker", "checktrayRnawork  tray:"+ trayindex +" DispatchModel " + dm.index.get() + " status " + dm.status.get());
                                    if (dm.status.get() == ConstantData.STATUS_RUN) {
                                        if (dm.targettype.get() == ConstantData.TYPE_RNA) {
                                            boolean rnab = checkrnawork(dm.target.get(), trayindex);
                                            Log.i("ArmTaskWorker", "rna " + dm.target.get() + " work " + rnab);
                                            if (!rnab) {
                                                return false;
                                            }
                                        }
                                    } else if (dm.status.get() == ConstantData.STATUS_FREE) {
                                        Log.i("ArmTaskWorker", "【调度分配】checktrayRnawork tray:"+trayindex+" DispatchModel " + dm.index.get() + " STATUS_FREE，准备分配设备");
                                        if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                                            if (WSSStart&&SelfCheck) {
                                                //TODO fordebugtest
//                                                if (true) {
                                                if (ArmManager.getInstance().armCanuse()) {
                                                    if (dm.targettype.get() == ConstantData.DEVICE_RNA) {
                                                        PipeRNAModel rm = RNAManager.getInstance().getFreeModel();
                                                        if (rm != null) {
                                                            Log.w("ArmTaskWorker", "【调度分配】找到空闲 RNA:"+rm.index.get()+" 分配给 tray:"+trayindex+" dispatch:"+dm.index.get());
                                                            Log.w("ArmTaskWorker", "【调度分配前】RNA:"+rm.index.get()+" armStep:"+rm.armStep.get()+" currentTray:"+rm.currentTray.get()+" dispatchindex:"+rm.dispatchindex.get());
                                                            dm.target.set(rm.index.get());
                                                            dm.status.set(ConstantData.STATUS_RUN);
                                                            RNAManager.getInstance().setCurrentTemplate(rm.index.get(), dm, 0);
                                                            TrayManager.getInstance().setCurrentDispatch(tray.index.get(), tray.dispatchindex.get());
                                                            Log.w("ArmTaskWorker", "【调度分配后】RNA:"+rm.index.get()+" armStep:"+rm.armStep.get()+" currentTray:"+rm.currentTray.get()+" dispatchindex:"+rm.dispatchindex.get());
                                                        } else {
                                                            Log.w("ArmTaskWorker", "【调度分配】找不到空闲 RNA，tray:"+trayindex+" dispatch:"+dm.index.get()+" 等待下次调度");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                } else {
                    Log.w("ArmTaskWorker", "checktrayRnawork tray " + trayindex + " 状态为空！");
                }
            } else {
                Log.w("ArmTaskWorker", "checktrayRnawork tray " + trayindex + " 调度表为空！");
            }
        }

        return result;
    }
    /**
     * 分配盘位和调度机械臂
     *
     * @param trayindex
     * @return
     */
    private boolean checktrayPcrwork(int trayindex) {
        boolean result = true;//默认是空闲
        PipeTrayModel tray = TrayManager.getInstance().getPipeTrayModel(trayindex);
        if (tray != null) {
            int traydispatchindex = tray.dispatchindex.get();
            ArrayList<DispatchModel> dispatchlist = tray.dispatchlist;
            if (dispatchlist != null) {
                int traydispatchsize = dispatchlist.size();
                Log.i("ArmTaskWorker", "tray" + trayindex + " dispatchindex " + traydispatchindex + " dispatchlistsize " + traydispatchsize);
                if (tray.status.get() != null && tray.status.get().equalsIgnoreCase(ConstantData.str_RUN)) {
                    if (traydispatchindex > 0 && traydispatchindex <= traydispatchsize) {
                            for (int j = 0; j < dispatchlist.size(); j++) {
                                DispatchModel dm = dispatchlist.get(j);
                                if (traydispatchindex == dm.index.get()) {
                                    Log.i("ArmTaskWorker", "checktrayPcrwork tray:"+trayindex+" DispatchModel " + dm.index.get() + " status " + dm.status.get());
                                    if (dm.status.get() == ConstantData.STATUS_RUN) {
                                        Log.i("ArmTaskWorker", " dmtargettype: " + dm.targettype.get());
                                        if (dm.targettype.get() == ConstantData.TYPE_PCR) {
                                            boolean pcrb = checkpcrwork(dm.target.get(), tray.index.get());
                                            Log.i("ArmTaskWorker", "pcr " + dm.target.get() + " work " + pcrb);
                                            if (!pcrb) {
                                                return false;
                                            }
                                        }
                                    } else if (dm.status.get() == ConstantData.STATUS_FREE) {
                                        Log.i("ArmTaskWorker", "【调度分配】checktrayPcrwork tray:"+trayindex+" DispatchModel " + dm.index.get() + " STATUS_FREE，准备分配设备");
                                        if (!getPipeDoorModel().doorhold.get()) {//开门事件优先级最高
                                            if (WSSStart&&SelfCheck) {
                                                //TODO fordebugtest
//                                                if (true) {
                                                if (ArmManager.getInstance().armCanuse()) {
                                                    if (dm.targettype.get() == ConstantData.DEVICE_PCR) {
                                                        PipePCRModel rm = PCRManager.getInstance().getFreeModel();
                                                        if (rm != null) {
                                                            Log.w("ArmTaskWorker", "【调度分配】找到空闲 PCR:"+rm.index.get()+" 分配给 tray:"+trayindex+" dispatch:"+dm.index.get());
                                                            Log.w("ArmTaskWorker", "【调度分配前】PCR:"+rm.index.get()+" armStep:"+rm.armStep.get()+" currentTray:"+rm.currentTray.get()+" dispatchindex:"+rm.dispatchindex.get());
                                                            dm.target.set(rm.index.get());
                                                            dm.status.set(ConstantData.STATUS_RUN);
                                                            PCRManager.getInstance().setCurrentTemplate(rm.index.get(), dm,0);
                                                            TrayManager.getInstance().setCurrentDispatch(tray.index.get(), tray.dispatchindex.get());
                                                            Log.w("ArmTaskWorker", "【调度分配后】PCR:"+rm.index.get()+" armStep:"+rm.armStep.get()+" currentTray:"+rm.currentTray.get()+" dispatchindex:"+rm.dispatchindex.get());
                                                        } else {
                                                            Log.w("ArmTaskWorker", "【调度分配】找不到空闲 PCR，tray:"+trayindex+" dispatch:"+dm.index.get()+" 等待下次调度");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                } else {
                    Log.w("ArmTaskWorker", "checktrayPcrwork tray " + trayindex + " 状态为空！");
                }
            } else {
                Log.w("ArmTaskWorker", "checktrayPcrwork tray " + trayindex + " 调度表为空！");
            }
        }

        return result;
    }


    ArmWorkListener armWorkListener;

    public void setArmWorkListener(ArmWorkListener armWorkListener) {
        this.armWorkListener = armWorkListener;
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
                        if (model.armStep.get() == ConstantData.WORK_GET) {
                            model.gettrayfinish.set(true);
//                            PCRManager.getInstance().updateTray(target, source);
                            model.armStep.set(ConstantData.WORK_RUN);
                            model.excutestart.set(false);
                            model.excutefinish.set(false);

                            // 🔍 调度分析日志：机械臂送达RNA，启动模板
                            Log.e("DispatchAnalysis", String.format(
                                "[机械臂送达] RNA:%d armStep:WORK_GET→WORK_RUN currentTray:%d dispatchIndex:%d 准备启动模板",
                                aimitem.getPosition(),
                                model.currentTray.get(),
                                model.dispatchindex.get()
                            ));

                            Log.w("ArmTaskWorker", "【回调立即切换】RNA:"+aimitem.getPosition()+" armStep→WORK_RUN，防止空窗期被重复调度");
                            createRnaRunTemplate(aimitem.getPosition());//临时措施，启动rna模板
                        }
                    } else if (aimitem.getType().equalsIgnoreCase(ConstantData.namePCR)) {//targettype == ConstantData.DEVICE_PCR &&
                        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(aimitem.getPosition());
                        if (model.armStep.get() == ConstantData.WORK_GET) {
                            model.gettrayfinish.set(true);
//                            PCRManager.getInstance().updateTray(target, source);
                            model.armStep.set(ConstantData.WORK_RUN);
                            model.excutestart.set(false);
                            model.excutefinish.set(false);

                            // 🔍 调度分析日志：机械臂送达PCR，启动模板
                            Log.e("DispatchAnalysis", String.format(
                                "[机械臂送达] PCR:%d armStep:WORK_GET→WORK_RUN currentTray:%d dispatchIndex:%d 准备启动模板",
                                aimitem.getPosition(),
                                model.currentTray.get(),
                                model.dispatchindex.get()
                            ));

                            Log.w("ArmTaskWorker", "【回调立即切换】PCR:"+aimitem.getPosition()+" armStep→WORK_RUN，防止空窗期被重复调度");
                            createPcrRunTemplate(aimitem.getPosition());//临时措施，启动pcr模板
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
                    armErrorCallback.error(msg);
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
                    armErrorCallback.error(msg);
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
                                if (targetModel != null && targetModel.armStep.get() == ConstantData.WORK_GET) {
                                    targetModel.armStep.set(ConstantData.WORK_RUN);
                                    targetModel.excutestart.set(false);
                                    targetModel.excutefinish.set(false);
                                    Log.w("ArmTaskWorker", "【MOVE回调立即切换】RNA:"+aimitem.getPosition()+" armStep→WORK_RUN，防止空窗期被重复调度");
                                }
                            }
                            createRnaRunTemplate(aimitem.getPosition());//临时措施，启动rna模板
                            break;
                        case ConstantData.namePCR:
                            TrayManager.getInstance().updateDsiapatchNext(trayindex, dispatchindex, ConstantData.DEVICE_PCR, aimitem.getPosition());
                            {
                                PipePCRModel targetModel = PCRManager.getInstance().getPipePCRModel(aimitem.getPosition());
                                if (targetModel != null && targetModel.armStep.get() == ConstantData.WORK_GET) {
                                    targetModel.armStep.set(ConstantData.WORK_RUN);
                                    targetModel.excutestart.set(false);
                                    targetModel.excutefinish.set(false);
                                    Log.w("ArmTaskWorker", "【MOVE回调立即切换】PCR:"+aimitem.getPosition()+" armStep→WORK_RUN，防止空窗期被重复调度");
                                }
                            }
                            createPcrRunTemplate(aimitem.getPosition());//临时措施，启动pcr模板
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
                    armErrorCallback.error(msg);
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
        if (model != null && model.armStep.get() == ConstantData.WORK_GET) {
            model.excutestart.set(false);
            model.excutefinish.set(false);
            model.armStep.set(ConstantData.WORK_RUN);
            Log.e("ArmTaskWorker", "rna :"+rna +" WORK_RUN ");
        }
    }

    public boolean checkRnaFinish(int target) {
        PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(target);
        if (model != null && model.armStep.get() == ConstantData.WORK_RUN) {
            Log.e("ArmTaskWorker", "RNA:"+target+" checkRnaFinish Ok currentTray:" + model.currentTray.get());
            model.excutefinish.set(true);

            // ✅ 优先从模型自身的 currentDispatch 获取调度信息
            DispatchModel dispatchModel = model.currentDispatch.get();

            if(dispatchModel==null) {
                // 回退到通过 tray+dispatchindex 查找
                Log.e("ArmTaskWorker", "【RNA还盘断点1】currentDispatch为空，尝试通过 currentTray 查找");
                dispatchModel = TrayManager.getInstance().getCurrentDispatch(model.currentTray.get());
            }

            if(dispatchModel==null) {
                Log.e("ArmTaskWorker", "【RNA还盘断点1】checkRnaFinish 找不到调度数据 rna:" + target +" tray:"+model.currentTray.get() + " → 强制设置 WORK_BACK");

                // 🔍 调度分析日志：找不到调度数据
                Log.e("DispatchAnalysis", String.format(
                    "[完成判断-异常] RNA:%d armStep:WORK_RUN currentTray:%d dispatchIndex:%d 找不到调度数据 → 强制还盘 armStep→WORK_BACK",
                    target,
                    model.currentTray.get(),
                    model.dispatchindex.get()
                ));

                // 即使找不到 dispatch 也要设置 WORK_BACK，让 arm 能来取盘
                model.backtraystart.set(false);
                model.backtrayfinish.set(false);
                model.armStep.set(ConstantData.WORK_BACK);
                return true;
            } else {
                if (dispatchModel.nextdevice.get() != null && dispatchModel.nextdevice.get() > 0) {
                    model.movetraystart.set(false);
                    model.movetrayfinish.set(false);
                    model.armStep.set(ConstantData.WORK_MOVE);

                    // 🔍 调度分析日志：移动到下一设备
                    Log.e("DispatchAnalysis", String.format(
                        "[完成判断-移盘] RNA:%d armStep:WORK_RUN→WORK_MOVE tray:%d dispatchIndex:%d nextDevice:%d 准备移到PCR",
                        target,
                        model.currentTray.get(),
                        dispatchModel.index.get(),
                        dispatchModel.nextdevice.get()
                    ));

                    Log.e("ArmTaskWorker", "rna :" + target + " WORK_MOVE ");
                } else {
                    model.backtraystart.set(false);
                    model.backtrayfinish.set(false);
                    model.armStep.set(ConstantData.WORK_BACK);

                    // 🔍 调度分析日志：直接还盘
                    Log.e("DispatchAnalysis", String.format(
                        "[完成判断-还盘] RNA:%d armStep:WORK_RUN→WORK_BACK tray:%d dispatchIndex:%d nextDevice:%d 准备还盘",
                        target,
                        model.currentTray.get(),
                        dispatchModel.index.get(),
                        dispatchModel.nextdevice.get() != null ? dispatchModel.nextdevice.get() : 0
                    ));

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
                model.excutefinish.set(true);
                model.backtraystart.set(false);
                model.backtrayfinish.set(false);
                model.armStep.set(ConstantData.WORK_BACK);
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
        if (model != null && model.armStep.get() == ConstantData.WORK_GET) {
            model.excutestart.set(false);
            model.excutefinish.set(false);
            model.armStep.set(ConstantData.WORK_RUN);
            Log.e("ArmTaskWorker", "PCR:"+pcr +" WORK_RUN ");
        }
    }

    public boolean checkPcrFinish(int target) {
        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(target);
        if (model != null && model.armStep.get() == ConstantData.WORK_RUN) {
            Log.e("ArmTaskWorker", "PCR:"+target+" checkPcrFinish Ok currentTray:" + model.currentTray.get());
            model.excutefinish.set(true);

            // ✅ 优先从模型自身的 currentDispatch 获取调度信息
            DispatchModel dispatchModel = model.currentDispatch.get();

            if(dispatchModel==null) {
                // 回退到通过 tray+dispatchindex 查找
                Log.e("ArmTaskWorker", "【还盘断点1】currentDispatch为空，尝试通过 currentTray 查找");
                dispatchModel = TrayManager.getInstance().getCurrentDispatch(model.currentTray.get());
            }

            if (dispatchModel == null) {
                Log.e("ArmTaskWorker", "【还盘断点1】checkPcrFinish 找不到调度数据 pcr:" + target + " tray:" + model.currentTray.get() + " → 强制设置 WORK_BACK");

                // 🔍 调度分析日志：找不到调度数据
                Log.e("DispatchAnalysis", String.format(
                    "[完成判断-异常] PCR:%d armStep:WORK_RUN currentTray:%d dispatchIndex:%d 找不到调度数据 → 强制还盘 armStep→WORK_BACK",
                    target,
                    model.currentTray.get(),
                    model.dispatchindex.get()
                ));

                // 即使找不到 dispatch 也要设置 WORK_BACK，让 arm 能来取盘
                model.backtraystart.set(false);
                model.backtrayfinish.set(false);
                model.armStep.set(ConstantData.WORK_BACK);
                return true;
            } else {
                int tray = model.currentTray.get();
                int dispatchindex = dispatchModel.index.get();
                if (dispatchModel.nextdevice.get() != null && dispatchModel.nextdevice.get() > 0) {
                    model.movetraystart.set(false);
                    model.movetrayfinish.set(false);
                    model.armStep.set(ConstantData.WORK_MOVE);

                    // 🔍 调度分析日志：移动到下一设备
                    Log.e("DispatchAnalysis", String.format(
                        "[完成判断-移盘] PCR:%d armStep:WORK_RUN→WORK_MOVE tray:%d dispatchIndex:%d nextDevice:%d 准备移到RNA",
                        target,
                        tray,
                        dispatchindex,
                        dispatchModel.nextdevice.get()
                    ));

                    Log.e("ArmTaskWorker", "finish PCR:" + target + " WORK_MOVE ");
                } else {
                    model.backtraystart.set(false);
                    model.backtrayfinish.set(false);
                    model.armStep.set(ConstantData.WORK_BACK);

                    // 🔍 调度分析日志：直接还盘
                    Log.e("DispatchAnalysis", String.format(
                        "[完成判断-还盘] PCR:%d armStep:WORK_RUN→WORK_BACK tray:%d dispatchIndex:%d nextDevice:%d 准备还盘",
                        target,
                        tray,
                        dispatchindex,
                        dispatchModel.nextdevice.get() != null ? dispatchModel.nextdevice.get() : 0
                    ));

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
                model.excutefinish.set(true);
                model.backtraystart.set(false);
                model.backtrayfinish.set(false);
                model.armStep.set(ConstantData.WORK_BACK);
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
