package com.paul.pipeline.manager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.WellInfo;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.service.SLANService;
import com.itextpdf.io.util.DateTimeUtil;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.data.LocalFileHelper;
import com.paul.pipeline.fragment.model.PipeRNAModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.listener.DeviceErrorCallback;
import com.paul.pipeline.message.ErrorInfoMessage;
import com.paul.pipeline.utils.DataTimeUtil;
import com.paul.pipeline.utils.MmkvUtils;
import com.paul.pipeline.model.ArmStep;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Enum.SampleType;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.model.experiment.Well;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRReqMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRStopMessage;
import com.winters.boweilibrary.autopipeline.model.message.QueryDeviceMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PCRManager {
    private static final String TAG = "PCRManager";
    private static PCRManager mInstance;
    public static PCRManager getInstance(){
        if (null == mInstance) {
            synchronized (PCRManager.class){
                if (null == mInstance){
                    mInstance = new PCRManager();
                }
            }
        }
        return  mInstance;
    }

    DeviceErrorCallback pcrErrorCallback;

    public void setPcrErrorCallback(DeviceErrorCallback pcrErrorCallback) {
        this.pcrErrorCallback = pcrErrorCallback;
    }

    ArrayList<PipePCRModel> pcrlist = new ArrayList<>();

    public ArrayList<PipePCRModel> getPcrlist() {
        return pcrlist;
    }

    public void setPcrlist(ArrayList<PipePCRModel> pcrlist) {
        this.pcrlist = pcrlist;
    }

    public void clearData(Context context){
        if(pcrlist!=null) {
            for (int i = 0; i < pcrlist.size(); ++i) {
                int index = pcrlist.get(i).index.get();
                pcrlist.get(i).initModel(index, context.getString(R.string.pipe_main_pcr_name) + index);
//                pcrlist.set(i, newModel(context, index));
            }
        }
        pcrExperimentMap.put(1,new Experiment());
        pcrExperimentMap.put(2,new Experiment());
        pcrExperimentMap.put(3,new Experiment());
        pcrExperimentMap.put(4,new Experiment());
        pcrExperimentMap.put(5,new Experiment());
        pcrExperimentMap.put(6,new Experiment());
    }
    public void init(Context context){
        ArrayList<PipePCRModel> list = new ArrayList<>();
        list.add(initModel(context,1));
        list.add(initModel(context,2));
        list.add(initModel(context,3));
        list.add(initModel(context,4));
        list.add(initModel(context,5));
        list.add(initModel(context,6));
        setPcrlist(list);
        pcrExperimentMap.put(1,new Experiment());
        pcrExperimentMap.put(2,new Experiment());
        pcrExperimentMap.put(3,new Experiment());
        pcrExperimentMap.put(4,new Experiment());
        pcrExperimentMap.put(5,new Experiment());
        pcrExperimentMap.put(6,new Experiment());
    }
    private PipePCRModel initModel(Context context,int index){
        PipePCRModel model = MmkvUtils.getObject(ConstantData.namePCR+index,PipePCRModel.class);
        if(model==null) {
            model = newModel(context, index);
        }
        return model;
    }

    public void saveModel(int index){
        if(pcrlist!=null) {
            for (int i = 0; i < pcrlist.size(); ++i) {
                PipePCRModel model = pcrlist.get(i);
                if (model!=null&& model.index.get()!=null && model.index.get() == index) {
                    MmkvUtils.saveObject(ConstantData.namePCR+index, model);
                }
            }
        }
    }

    private PipePCRModel newModel(Context context, int index){
        PipePCRModel model = new PipePCRModel(index, context.getString(R.string.pipe_main_pcr_name) + index);
//        model.linuxStatus.set( "free");
//        model.status.set("free");
//        model.connect.set("已连接");
//        model.degree.set("25");
        return model;
    }
    ConcurrentHashMap<Integer,Experiment> pcrExperimentMap = new ConcurrentHashMap<>();
    public Experiment getPcrExperiment(int index){
        if(pcrExperimentMap!=null) {
            if (pcrExperimentMap.containsKey(index)) {
                return pcrExperimentMap.get(index);
            }
        }
        return null;
    }
    public void setPcrExperiment(int index,Experiment experiment){
        if(pcrExperimentMap!=null) {
            pcrExperimentMap.put(index,experiment);
        }
    }
    public void updateCover(int index,boolean value){
        PipePCRModel model = getPipePCRModel(index);
        if(model!=null) {
            model.cover.set(value);
        }
    }
    public void updateExist(int index,boolean value){
        PipePCRModel model = getPipePCRModel(index);
        if(model!=null) {
                model.exist.set(value);
        }
    }
    public void updateExecuteStart(int index,boolean value){
        PipePCRModel model = getPipePCRModel(index);
        if(model!=null) {
            model.runtime.setExcutestart(value);
            if(value){
                String startTime = DateFormatUtil.getSysFullTime();
                model.startTime.set(startTime);
            }
            saveModel(index);
        }
    }
//    public void updateTray(int index,int value){
//        PipePCRModel model = getPipePCRModel(index);
//        if(model!=null) {
//                model.tray.set(value);
//        }
//    }
//    public void updateMovetraystart(int index,boolean value){
//        PipePCRModel model = getPipePCRModel(index);
//        if(model!=null) {
//            model.movetraystart.set(value);
//            saveModel(index);
//        }
//    }
//    public void updateBacktraystart(int index,boolean value){
//        PipePCRModel model = getPipePCRModel(index);
//        if(model!=null) {
//                model.backtraystart.set(value);
//            saveModel(index);
//        }
//    }
    // 测试-更新流程图
//    public void updateDispatch(int index,String batch,String path,String project,String template){
//        PipePCRModel model = getPipePCRModel(index);
//        if(model!=null) {
////            model.batch.set(batch);
//            DispatchModel dm = new DispatchModel();
//            dm.batch.set(batch);
//            dm.path.set(path);
//            dm.projectname.set(project);
//            dm.templatename.set(template);
//            model.dispatch.set(dm);
//        }
//    }
//    public void updateGettraystart(int index,boolean value){
//        PipePCRModel model = getPipePCRModel(index);
//        if(model!=null) {
//            model.gettraystart.set(value);//解除占用
//            saveModel(index);
//        }
//    }
    public void updateStarttime(int index,String value){
        PipePCRModel model = getPipePCRModel(index);
        if(model!=null) {
            model.startTime.set(value);
        }
    }
    public void updateTestrun(int index,boolean value){
        PipePCRModel model = getPipePCRModel(index);
        if(model!=null) {
            model.testrun.set(value);
        }
    }
    public void refreshConnectinfo(ConcurrentHashMap<String, WSSConnectionItem> items) {
        SerialPortManager_Robot.RobotStatus robotStatus = SerialPortManager_Robot.getInstance().getRobotStatus();
        if (robotStatus != null) {
            updateExist(1, robotStatus.getSenseByKey("PCR1"));
            updateExist(2, robotStatus.getSenseByKey("PCR2"));
            updateExist(3, robotStatus.getSenseByKey("PCR3"));
            updateExist(4, robotStatus.getSenseByKey("PCR4"));
            updateExist(5, robotStatus.getSenseByKey("PCR5"));
            updateExist(6, robotStatus.getSenseByKey("PCR6"));

            updateCover(1, robotStatus.getSenseByKey("PCR_COVER1"));
            updateCover(2, robotStatus.getSenseByKey("PCR_COVER2"));
            updateCover(3, robotStatus.getSenseByKey("PCR_COVER3"));
            updateCover(4, robotStatus.getSenseByKey("PCR_COVER4"));
            updateCover(5, robotStatus.getSenseByKey("PCR_COVER5"));
            updateCover(6, robotStatus.getSenseByKey("PCR_COVER6"));
        } else {
            updateExist(1, false);
            updateExist(2, false);
            updateExist(3, false);
            updateExist(4, false);
            updateExist(5, false);
            updateExist(6, false);

            updateCover(1, false);
            updateCover(2, false);
            updateCover(3, false);
            updateCover(4, false);
            updateCover(5, false);
            updateCover(6, false);
        }
        if(pcrlist!=null) {
            for (int i = 0; i < pcrlist.size(); i++) {
                PipePCRModel model = pcrlist.get(i);
                boolean connect = false;
                if (items != null) {
                    for (String key : items.keySet()) {
                        WSSConnectionItem item = items.get(key);
                        if (item.getDeviceType() != null && item.getDeviceType().equalsIgnoreCase(ConstantData.namePCR) &&
                                model.index.get() == item.getPosition()) {
                            if (item instanceof PCRStatusItem) {
                                if (item.getWebSocketConnection().isClosing()
                                        || item.getWebSocketConnection().isClosed()
                                        || item.getWebSocketConnection().isFlushAndClose()) {
                                    connect = false;
                                } else {
                                    connect = true;
                                }
                                model.key.set(key);
                                model.deviceIP.set(item.getDeviceIP());
                                model.devicePort.set(item.getDevicePort());
                                PCRStatusItem statusItem = (PCRStatusItem) item;
                                model = refreshModel(model, statusItem);
                            } else {
                                Log.e(TAG, "TemplateWSSMessage: " + key + "is not PCR device");
                            }
                            break;
                        }
                    }
                } else {
                    refreshDisconnected(model);
                    Log.e("ArmTaskWorker", "PCR wss items null");
                }
                model.connect.set(connect);
            }
        }
    }
    public void refreshDisconnected(PipePCRModel model){
        if(model!=null) {
            model.connect.set(false);
        }
    }
    public PipePCRModel refreshModel(PipePCRModel model, PCRStatusItem status){
        if(status.getpCRLinuxStatus()!=null) {
            model.linuxStatus.set(status.getpCRLinuxStatus());
        }
        model.linuxAlarm.set(status.getpCRLinuxAlarm());
        if(status.getpCRStatus()!=null) {
            model.status.set(status.getpCRStatus());
        }
        model.sendAlarm.set(status.getpCRSendAlarm());
        model.alarm.set(status.getpCRAlarm());
        model.degree.set(status.getpCRDegree());
        model.degreeTarget.set(status.getDegreeTarget());
        model.totalCycle.set(status.getpCRTotalCycle());
       // model.currentTray.set(status.getpCRCurrentTray());
        model.currentTemplate.set(status.getpCRCurrentTemplate());
        model.currentSection.set(status.getpCRCurrentSection());
        model.currentCircle.set(status.getpCRCurrentCycle());
        model.currentStep.set(status.getpCRCurrentStep());
        model.leftTime.set(status.getpCRLeftTime());
        model.totalTime.set(status.getpCRTotalTime());
        model.waitTime.set(status.getpCRWaitTime());
        model.runningTime.set(status.getpCRRunningTime());
        model.info.set(status.getInfo());
        model.version.set(status.getVersion());
        if(status.getExperiment()!=null) {
            PCRManager.getInstance().setPcrExperiment(model.index.get(),status.getExperiment());
        }
        if(status.getpCRStatus()!=null&&status.getpCRStatus().equalsIgnoreCase(ConstantData.str_ERROR)){
            model.allstatus.set(ConstantData.str_ERROR);
        }else if(status.getpCRLinuxStatus()!=null&&status.getpCRLinuxStatus().equalsIgnoreCase(ConstantData.str_ERROR)){
            model.allstatus.set(ConstantData.str_ERROR);
        }else if(model.usererror.get()!=null&&model.usererror.get()!=0){
            model.allstatus.set(ConstantData.str_ERROR);
        }else{
            model.allstatus.set(status.getpCRStatus());
        }
        createAlarm(model.index.get(),model.alarm.get(),model.linuxAlarm.get(),model.sendAlarm.get(),model.alarmenable.get());
        return model;
    }
    private void createAlarm(int index,String alarm,String linuxalarm,String sendalarm,boolean alarmdialog){
        boolean send = false;
        ErrorInfoMessage msg = new ErrorInfoMessage();
        msg.setIndex(index);
        msg.setType(ConstantData.DEVICE_PCR);
        if(alarm!=null&&!alarm.isEmpty()&&!alarm.isBlank()){
            msg.setAlarm(alarm);
            send = true;
        }
        if(linuxalarm!=null&&!linuxalarm.isEmpty()&&!linuxalarm.isBlank()){
            msg.setLinuxalarm(linuxalarm);
            send = true;
        }
        if(sendalarm!=null&&!sendalarm.isEmpty()&&!sendalarm.isBlank()){
            msg.setSendalarm(sendalarm);
            send = true;
        }
        if(send){
            msg.setAlarmdialog(alarmdialog);
            msg.setReset(true);
            if(pcrErrorCallback!=null){
                pcrErrorCallback.error(msg);
            }
        }
    }
    public PipePCRModel getFreeModel() {
        for (int i = 0; i < pcrlist.size();i++) {
            PipePCRModel m = pcrlist.get(i);
            if(m.connect.get() && !m.exist.get()
                    && m.armStep.get() == ArmStep.FREE
                    && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                    && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE)){
                Log.w("ArmTaskWorker", "找到空闲pcr: "+m.index.get());
                return m;
            }
            Log.w("ArmTaskWorker", "空闲pcr: "+m.index.get()+" connect: "+m.connect.get()
                    +" exist："+m.exist.get()+" armStep："+m.armStep.get()+" status："+m.status.get()+" linuxStatus："+m.linuxStatus.get());
        }
        Log.i("ArmTaskWorker", "pcr:无空闲 ");
        return null;
    }
    public PipePCRModel getPipePCRModel(int index){
        for(int i=0;i<pcrlist.size();i++){
            PipePCRModel model = pcrlist.get(i);
            if(model.index.get()==index) {
                return model;
            }
        }
        return null;
    }
    public boolean getExist(int index){
        PipePCRModel model = getPipePCRModel(index);
        if(model!=null) {
            return model.exist.get();
        }
        return false;
    }

    public boolean finishBacktray(int source, int target) {
        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(source);
        if (model != null) {
            if (model.armStep.get() == ArmStep.BACK) {
                model.runtime.setBacktrayfinish(true);
                model.currentTray.set(0);
                model.armStep.set(ArmStep.FREE);
                model.runtime.setUiinit(true);//界面初始化
                Log.w("ArmTaskWorker", "finishBacktray pcr" + source + " 托盘" + target + " 归位");
                return true;
            }
        }
        return false;
    }

    public boolean finishMoveouttray(int source, int target) {
        PipePCRModel model = PCRManager.getInstance().getPipePCRModel(source);
        if (model != null) {
            if (model.armStep.get() == ArmStep.MOVE) {
                model.runtime.setMovetrayfinish(true);
                model.currentTray.set(0);
                model.armStep.set(ArmStep.FREE);
                model.runtime.setUiinit(true);//界面初始化
                Log.w("ArmTaskWorker", "finishMoveouttray pcr" + source + " rna:" + target + " move");
                return true;
            }
        }
        return false;
    }
    public void clearDispatch(int index){
        PipePCRModel model = getPipePCRModel(index);
        if(model!=null) {
            if (model.index.get() == index) {
                model.armStep.set(ArmStep.FREE);
                saveModel(index);
            }
        }
    }
    public void updateCurrentTray(int index,int tray,int dispatchindex){
        PipePCRModel m = getPipePCRModel(index);
        if(m!=null) {
            m.currentTray.set(tray);
            m.dispatchindex.set(dispatchindex);
            saveModel(index);
        }
    }
    public boolean setCurrentTemplate(int index, DispatchModel dm,int status){
        PipePCRModel m = getPipePCRModel(index);
        m.currentTray.set(dm.tray.get());
        m.dispatchindex.set(dm.index.get());
        m.armStep.set(ArmStep.GET);
        if(status==0) {
            m.runtime.setGettraystart(false);
            m.runtime.setGettrayfinish(false);
        }else{
            m.runtime.setGettraystart(true);
            m.runtime.setGettrayfinish(true);
        }
        saveModel(index);
        return true;
    }
    public DispatchModel getCurrentTemplate(int index){
        PipePCRModel m = getPipePCRModel(index);
        if(m!=null&&m.currentTray.get()!=null){
            return TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
        }
        return null;
    }
    int seqid;
    public int getSeqid(){
        if((seqid+1)==Integer.MAX_VALUE){
            seqid=0;
        }
        return seqid++;
    }
    public boolean testexecute1(int index){
        Log.e(TAG,"pcr execute test run:"+index);
        String key = APLManager.getInstance().getKey(ConstantData.DEVICE_PCR,index);
        if(key==null) {
            Log.e("ArmTaskWorker","pcr execute key null:"+index);
            return false;
        }
        String nowtime = DateFormatUtil.getSysFullTime();
        updateStarttime(index,nowtime);
        updateTestrun(index,true);
        String batch = "PCR"+index+"PAN0-13-"+ DataTimeUtil.getHFullTime();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/";
//        updateDispatch(index,batch,path,"99~test","first.prj");
        String result = ExecutePCR(key,getSeqid(),batch,1000,0,"99~test",
                "second.prj",path,0,0,2,new ArrayList<>());
        if(result==null) {
            return true;
        }else{
            Log.e(TAG,"pcr test execute:"+result);
            return false;
        }
    }
    public boolean testexecute2(int index){
        Log.e(TAG,"pcr execute test run:"+index);
        String key = APLManager.getInstance().getKey(ConstantData.DEVICE_PCR,index);
        if(key==null) {
            Log.e("ArmTaskWorker","pcr execute key null:"+index);
            return false;
        }
        String nowtime = DateFormatUtil.getSysFullTime();
        updateStarttime(index,nowtime);
        updateTestrun(index,true);
        String batch = "PCR"+index+"PAN0-13-"+ DataTimeUtil.getHFullTime();
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/";
//        updateDispatch(index,batch,path,"99~test","second.prj");
        String result = ExecutePCR(key,getSeqid(),batch,1000,0,"99~test",
                "second.prj",path,0,0,2,new ArrayList<>());
        if(result==null) {
            return true;
        }else{
            Log.e(TAG,"pcr test execute:"+result);
            return false;
        }
    }
    public String errorContinue(int index){
        try {
            return execute(index);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", "pcr errorContinue : " + e.getMessage());
            return "Exception:"+e.getMessage();
        }
    }
    public String execute(int index){
        DispatchModel msg = getCurrentTemplate(index);
        if(msg==null){
            Log.e(TAG,"pcr execute msg null:"+index);
            return "调度信息不存在";
        }
        PipeTrayModel traym = TrayManager.getInstance().getPipeTrayModel(msg.tray.get());
        if(traym==null){
            Log.e(TAG,"pcr execute tray null:"+ msg.tray.get());
            return "托盘信息不存在";
        }
        String key = APLManager.getInstance().getKey(ConstantData.DEVICE_PCR,index);
        if(key==null) {
            Log.e("ArmTaskWorker","pcr execute key null:"+index);
            return "";
        }
        String batch = msg.batch.get();
        long runtime = msg.runtime.get();
        String template = msg.templatename.get();
        String project = msg.projectname.get();
        String path = msg.path.get();
        int phase = msg.phase.get();
        int circle = msg.circle.get();
        long totaltime = 0l;
        int firstsecond = msg.firstSecond.get();
        if(msg.totaltime.get()!=null) {
            totaltime = msg.totaltime.get().getTotalTime();
            if (firstsecond == 1) {
                totaltime = msg.totaltime.get().getFirstPcrTime();
            } else if (firstsecond == 2) {
                totaltime = msg.totaltime.get().getSecondPcrTime();
            }
        }
        ArrayList<WellInfo> wells = traym.wellInfolist;
        if(batch!=null&&template!=null&&wells!=null) {
            String result = ExecutePCR(key, getSeqid(), batch, totaltime, runtime, project,template,path, phase, circle, firstsecond, wells);
            if (result != null) {
                Log.e(TAG, "pcr  execute:" + result);
                Log.e(TAG, "pcr  path:" + path);
                Log.e(TAG, "pcr  template:" + template);
            }
            return result;
        }else {
            Log.e(TAG, "pcr  execute null");
            return "运行信息不存在";
        }
    }
    public boolean stop(int index){
        String key = APLManager.getInstance().getKey(ConstantData.DEVICE_PCR,index);
        if(key==null){
            Log.e("ArmTaskWorker","pcr:"+index+" key null");
            return false;
        }
        String result = StopPCR(key,getSeqid());
        if(result==null) {
            Log.i("ArmTaskWorker","pcr stop:"+index);
            return true;
        }else{
            Log.e("ArmTaskWorker","pcr stop fail:"+result);
            return false;
        }
    }
    public boolean reset(int index){
        String batch ="111";
        DispatchModel msg = getCurrentTemplate(index);
        if(msg==null){
            Log.e("ArmTaskWorker","pcr reset msg null:"+index);
        }else{
            batch = msg.batch.get();
        }
        String sendkey = APLManager.getInstance().getKey(ConstantData.DEVICE_PCR,index);
        if(sendkey==null){
            Log.e("ArmTaskWorker","reset pcr:"+index+" key null");
            return false;
        }
        String result = ResetPCR(sendkey,getSeqid(),batch);
        if(result==null) {
            return true;
        }else{
            Log.e("ArmTaskWorker","pcr reset fail:"+result);
            return false;
        }
    }
    public String QueryDevicePCR(String selectWSSConnectionItemKey,int seqid) {
        QueryDeviceMessage queryDeviceMessage = new QueryDeviceMessage();
        queryDeviceMessage.setType("QueryDevice");
        queryDeviceMessage.setSeqId(seqid);
        queryDeviceMessage.setPosition(seqid);
        queryDeviceMessage.setDeviceType("PCR");
        queryDeviceMessage.setDeviceID("PCR" + seqid);
        queryDeviceMessage.setCommand("setPCR_position");
        if (selectWSSConnectionItemKey == null) {
            return "请选择要发送模板的模块";
        }
        String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey, queryDeviceMessage);
        if (!result.contains("error")) {
         //   APLManager.getInstance().refreshWss();
            return null;
         //   Toast.makeText(mContext, "sendWSSMessage:" + selectWSSConnectionItemKey + "--" + "QueryDevice succeed", Toast.LENGTH_LONG).show();
        } else {
            return "sendWSSMessage:" + selectWSSConnectionItemKey + "--" + "QueryDevice error";
        }
    }

     public String StopPCR(String selectWSSConnectionItemKey,int seqid) {
         if(selectWSSConnectionItemKey == null){
             return "请选择要发送的模块";
         }
        PCRStopMessage pCRStopMessage = new PCRStopMessage();
        pCRStopMessage.setSeqId(seqid);//111
        pCRStopMessage.setType("PCRStop");

        String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,pCRStopMessage);
//         APLManager.getInstance().refreshWss();
         if(result.contains("succeed")){
             return null;
         }else {
             return result;
         }
    }
    /// /
    /// SEQID 自增
    /// 批号
    public String ResetPCR(String selectWSSConnectionItemKey,int seqid,String batchid) {
        if (selectWSSConnectionItemKey == null) {
            return "请选择要发送模板的模块";
        }
        PCRReqMessage pCRReqMessage = new PCRReqMessage();
        pCRReqMessage.setForce(1);
        pCRReqMessage.setSeqId(seqid);//111
        //pCRReqMessage.setBatchID(1234 + "");//1234
		pCRReqMessage.setBatchID(batchid);//1234
//            pCRReqMessage.setDeviceID("master");
//            pCRReqMessage.setDeviceType("master");
        pCRReqMessage.setType("PCRReq");
//            pCRReqMessage.setPosition(1);
        pCRReqMessage.setTemplate("0~reset~reset.prj");
        pCRReqMessage.setCurrentPhase(0);
        pCRReqMessage.setCurrentCycle(0);
        pCRReqMessage.setCurrentSection("");
        pCRReqMessage.setCurrentStep("");
        pCRReqMessage.setRunningTime(0);
        pCRReqMessage.setTotalTime(1000);

        ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/PCR/");//获取目录中的项目

        Program program = null;

        Project projectSend = null;

        if (null != mConfigProjects && mConfigProjects.size() > 0) {
            for (Project project : mConfigProjects) {
                if (null != project) {
                    if (project.getName().contains("reset")) {
                        program = project.getProgram();
                        projectSend = project;
                    }
                }
            }
        }
        if (program == null) {
            return "reset.prj 不存在";
        }
        try {
            Experiment.getInstance().setProject(projectSend);
            ArrayList<Well> wells = new ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                Well well = new Well();
                //孔名称
                well.setName("A" + (i));
                //项目名称
                well.setProjectName("reset.prj");
                //患者信息.唯一标示
                well.setID("A" + (i));
                //姓名
                well.setPatientName("PatientName");
                //性别
                well.setSex("Sex");
                //年龄
                well.setAge("Age");
                //标本类型
                well.setSpecimen("Specimen");
                //采样日期
                well.setSpecimenDate("SpecimenDate");
                //送检医生
                well.setDoctor("Doctor");
                //送检科室
                well.setOffice("Office");
                //诊断
                well.setDiagnosis("Diagnosis");
                //病历号
                well.setCaseID("caseID");
                //病床号
                well.setBedID("BedID");
                //住院号
                well.setInPatientID("InPatientID");
                //门诊号
                well.setOutPatientID("OutPatientID");
                //备注
                well.setRemark("Remark");
                //样本名称
                well.setSampleName("A" + (i));//身份證號碼
                //试管编号
                well.setTubeNo(i);//编号
                //试管名称
                well.setTubeName("A" + (i));
                //样本类型
                well.setType(SampleType.Unknown.getValue());
                //病历号
                well.setCaseID("A" + (i));
                //属性
                well.setProperty("Property");
                wells.add(well);
            }

            Experiment.getInstance().setExperimentWells(wells);

            Experiment.getInstance().setAuditor(PcrApplication.operator);
            Experiment.getInstance().setBatchNo(batchid);//"20251231"
            Experiment.getInstance().setNote("test");
            Experiment.getInstance().setOperator(PcrApplication.operator);
            Experiment.getInstance().setName("test");
            Experiment.getInstance().setCustomColumns(new ArrayList<String>());
            Experiment.getInstance().setPlateNo("test");

            Experiment.getInstance().setExperimentWells(wells);
            pCRReqMessage.setExperiment(Experiment.getInstance());

            String result = WSSModuleManager.getInstance().sendPCRTemplateWSSMessage(selectWSSConnectionItemKey, pCRReqMessage);
//            APLManager.getInstance().refreshWss();
            if(result.contains("succeed")){
                return null;
            }else {
                return result;
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
            return e.getMessage();
        }
    }
    public String ExecutePCR(String selectWSSConnectionItemKey,int seqid,String batchid,long totaltime,long runtime,
                             String projectname,String template,String prjpath,int phase,int cycle,int firstorsecond,ArrayList<WellInfo> wellInfos){
        String tempflag = "first";
        if(firstorsecond==1){
            tempflag = "first";
        }else if(firstorsecond==2){
            tempflag = "second";
        }
//        if(template.contains(".")) {
//            tempflag = LocalFileHelper.getFileNameNoEx(template);//去掉后缀名.prj
//        }
        if(selectWSSConnectionItemKey == null){
            return "请选择要发送模板的模块";
        }
        PCRReqMessage pCRReqMessage = new PCRReqMessage();
        pCRReqMessage.setSeqId(seqid);//111
        pCRReqMessage.setBatchID(batchid);//1234
//            pCRReqMessage.setDeviceID("master");
//            pCRReqMessage.setDeviceType("master");
        pCRReqMessage.setType("PCRReq");
//            pCRReqMessage.setPosition(1);
        pCRReqMessage.setTemplate(projectname+"-"+template);//"13~新冠~first.prj"
        pCRReqMessage.setCurrentPhase(phase);
        pCRReqMessage.setCurrentCycle(cycle);
        pCRReqMessage.setCurrentSection("");
        pCRReqMessage.setCurrentStep("");
        pCRReqMessage.setRunningTime((int)runtime);//0
        pCRReqMessage.setTotalTime((int)totaltime);//1000

        Log.e(TAG, "ExecutePCR path:"+prjpath);

        ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(prjpath);//FileUtils.CONFIG_SLAN_PATH
                //Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/");//获取目录中的项目
        Program program = null;
        Project projectSend = null;
        if(null != mConfigProjects && mConfigProjects.size()>0) {
            for (Project project : mConfigProjects) {
                if (null != project) {
                   // Log.e("ArmTaskWorker", "project name:"+project.getName());
                    if (project.getName().equals(tempflag)) {
                        program = project.getProgram();
                        projectSend = project;
                    }
                }
            }
        }
        if(program == null){
            return template+".prj 不存在";
        }
        try {
            Experiment.getInstance().setProject(projectSend);
            ArrayList<Well> wells = getWells(wellInfos,template +".prj");

            Experiment.getInstance().setExperimentWells(wells);
            Experiment.getInstance().setAuditor(PcrApplication.operator);
            Experiment.getInstance().setBatchNo(batchid);
            Experiment.getInstance().setNote("test");
            Experiment.getInstance().setOperator(PcrApplication.operator);
            Experiment.getInstance().setName(template);
            Experiment.getInstance().setCustomColumns(new ArrayList<String>());
            Experiment.getInstance().setPlateNo("test");
//            Experiment.getInstance().setExperimentWells(wells);
            pCRReqMessage.setExperiment(Experiment.getInstance());

            String result = WSSModuleManager.getInstance().sendPCRTemplateWSSMessage(selectWSSConnectionItemKey,pCRReqMessage);
            if(result.contains("succeed")){
                return null;
            }else {
                return result;
            }
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
            return e.getMessage();
        }
    }
    public ArrayList<Well> initWells(){
        ArrayList<Well> wells = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            Well well = new Well();
            //孔名称
            well.setName("A" + (i));
            //项目名称
            well.setProjectName("second.prj");
            //患者信息.唯一标示
            well.setID("A" + (i));
            //姓名
            well.setPatientName("PatientName");
            //性别
            well.setSex("Sex");
            //年龄
            well.setAge("Age");
            //标本类型
            well.setSpecimen("Specimen");
            //采样日期
            well.setSpecimenDate("SpecimenDate");
            //送检医生
            well.setDoctor("Doctor");
            //送检科室
            well.setOffice("Office");
            //诊断
            well.setDiagnosis("Diagnosis");
            //病历号
            well.setCaseID("caseID");
            //病床号
            well.setBedID("BedID");
            //住院号
            well.setInPatientID("InPatientID");
            //门诊号
            well.setOutPatientID("OutPatientID");
            //备注
            well.setRemark("Remark");
            //样本名称
            well.setSampleName("A" + (i));//身份證號碼
            //试管编号
            well.setTubeNo(i);//编号
            //试管名称
            well.setTubeName("A" + (i));
            //样本类型
            well.setType(SampleType.Unknown.getValue());
            //病历号
            well.setCaseID("A" + (i));
            //属性
            well.setProperty("Property");
            wells.add(well);
        }
        return wells;
    }
    public ArrayList<Well> getWells(ArrayList<WellInfo> wellInfos,String template){
        ArrayList<Well> wells = new ArrayList<>();
        if(null != wellInfos && wellInfos.size()>0){
            for (int i = 1; i <= wellInfos.size(); i++) {
                WellInfo wellInfo = wellInfos.get(i - 1);
                Well well = new Well();
                //孔名称
                well.setName(wellInfo.getWellName());
                //项目名称
                well.setProjectName(template);
                //患者信息
                //唯一标示
                well.setID("A" + (i));
                //姓名
                well.setPatientName("PatientName");
                //性别
                well.setSex("Sex");
                //年龄
                well.setAge("Age");
                //标本类型
                well.setSpecimen("Specimen");
                //采样日期
                well.setSpecimenDate("SpecimenDate");
                //送检医生
                well.setDoctor("Doctor");
                //送检科室
                well.setOffice("Office");
                //诊断
                well.setDiagnosis("Diagnosis");
                //病历号
                well.setCaseID("caseID");
                //病床号
                well.setBedID("BedID");
                //住院号
                well.setInPatientID("InPatientID");
                //门诊号
                well.setOutPatientID("OutPatientID");
                //备注
                well.setRemark("Remark");
                //样本信息
                //身份证信息
                well.setSampleName(wellInfo.getSampleName());
                //试管编号
                well.setTubeNo(i);
                //试管名称
                well.setTubeName(wellInfo.getReagentCode());
                //病历号
                well.setCaseID(wellInfo.getSampleName());
                //样本类型
                well.setType(SampleType.Unknown.getValue());
                //属性
                well.setProperty("Property");
                wells.add(well);
            }
        }else{
            wells = initWells();
        }
        return wells;
    }

    public boolean updatePcr(String key,String version,String filepath) {
      //  String filename = "pcr_app_pcr_01_00_18_20251009.tar.gz";
        //filename.replace("pcr_app_","").replace(".tar.gz","")
        UpgradeMessage upgradeMessage = new UpgradeMessage();
        upgradeMessage.setSeqId(getSeqid());
        upgradeMessage.setForce(1);
        upgradeMessage.setType("Upgrade");
        upgradeMessage.setVersion(version);
        try {
            if (key == null) {
                Log.e(TAG, "updatePcr fail key null:"+key);
                // Toast.makeText(mContext, "请选择要发送模板的PCR模块", Toast.LENGTH_LONG).show();
                return false;
            }
            if (version == null) {
                Log.e(TAG, "updatePcr fail version null:"+version);
                return false;
            }
            if (filepath == null) {
                Log.e(TAG, "updatePcr fail filepath null:"+filepath);
                return false;
            }
            String result = WSSModuleManager.getInstance().sendPCRUpgradeWSSMessage(key, upgradeMessage,filepath);
            if(result.contains("succeed")){
                return true;
            }
            Log.e(TAG, "updatePcr error:"+result);
            return false;
          //  Toast.makeText(mContext, "sendPCRUpgradeWSSMessage" +  result,Toast.LENGTH_LONG).show();
            //wssConnectionAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG,"updatePcr exception:"+ e.getMessage());
            e.printStackTrace();
            return false;
            //Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

//    public void saveExperiment(int index,OnSaveExperimentListener listener){
//        PipePCRModel model = getPipePCRModel(index);
//        if(model!=null){
//            String resultFilePath = "/sdcard/PCR/"+"PCR"+index+".boweipcrResult";
//            Log.e("ArmTaskWorker","saveExperimentFile filePath"+resultFilePath);
//            BoWeiService.getInstance().saveExperiment(model.experiment.get(), "PCR"+index, Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/",listener);
//        }
//    }
//    public void saveExperimentFile(Experiment experiment, String batch,OnSaveExperimentListener listener){
//        new OnSaveExperimentListener() {
//            @Override
//            public void onSaveSuccess() {
//                Log.e(TAG,"save onSaveSuccess:" +batch);
//            }
//
//            @Override
//            public void onSaveFail() {
//                Log.e(TAG,"save onSaveFail:"+ batch);
//            }
//        }
//                            new Thread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    Experiment open_experiment = BoWeiService.getInstance().openExperiment(resultFilePath);

        //BoweiJni boweiJni = new BoweiJni();
        //BoweiJni.analysisExperimentLoessFilter(open_experiment);
        //    String content = GsonUtil.toJson(open_experiment,false);
        //     Log.e("AAA","content"+content);
//                                    runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            if(null != open_experiment){
//                                                openBiowayExperiment(open_experiment);
//                                            }
//                                        }
//                                    });
//                                }
//                            }).start();
//    }
//    public String ExecutePCRFirst(String selectWSSConnectionItemKey,int seqid,String batchid,int totaltime,int runtime,String
//            template,int phase,int cycle){
//
//        if(selectWSSConnectionItemKey == null){
//            return "请选择要发送模板的模块";
//        }
//        PCRReqMessage pCRReqMessage = new PCRReqMessage();
//        pCRReqMessage.setSeqId(seqid);//111
//        pCRReqMessage.setBatchID(batchid);//1234
////            pCRReqMessage.setDeviceID("master");
////            pCRReqMessage.setDeviceType("master");
//        pCRReqMessage.setType("PCRReq");
////            pCRReqMessage.setPosition(1);
//        pCRReqMessage.setTemplate(template);//"13~新冠~first.prj"
//        pCRReqMessage.setCurrentPhase(phase);
//        pCRReqMessage.setCurrentCycle(cycle);
//        pCRReqMessage.setCurrentSection("");
//        pCRReqMessage.setCurrentStep("");
//        pCRReqMessage.setRunningTime(runtime);//0
//        pCRReqMessage.setTotalTime(totaltime);//1000
//
//        ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(Environment.getExternalStorageDirectory().getAbsolutePath()
//                + "/PCR/");//获取目录中的项目
//
//        Program program = null;
//
//        Project projectSend = null;
//
//        if(null != mConfigProjects && mConfigProjects.size()>0) {
//            for (Project project : mConfigProjects) {
//                if (null != project) {
//                    if (project.getName().contains("first")) {
//
//                        program = project.getProgram();
//
//                        projectSend = project;
//
//                    }
//                }
//            }
//        }
//
//        if(program == null){
//            return "first.prj 不存在";
//        }
//
//        try {
//
//            Experiment.getInstance().setProject(projectSend);
//
//            ArrayList<Well> wells = new ArrayList<>();
//
//            for (int i = 1; i <= 8; i++) {
//                Well well = new Well();
//                //孔名称
//                well.setName("A" + (i));
//                //项目名称
//                well.setProjectName("first.prj");
//                //患者信息.唯一标示
//                well.setID("A" + (i));
//                //姓名
//                well.setPatientName("PatientName");
//                //性别
//                well.setSex("Sex");
//                //年龄
//                well.setAge("Age");
//                //标本类型
//                well.setSpecimen("Specimen");
//                //采样日期
//                well.setSpecimenDate("SpecimenDate");
//                //送检医生
//                well.setDoctor("Doctor");
//                //送检科室
//                well.setOffice("Office");
//                //诊断
//                well.setDiagnosis("Diagnosis");
//                //病历号
//                well.setCaseID("caseID");
//                //病床号
//                well.setBedID("BedID");
//                //住院号
//                well.setInPatientID("InPatientID");
//                //门诊号
//                well.setOutPatientID("OutPatientID");
//                //备注
//                well.setRemark("Remark");
//                //样本名称
//                well.setSampleName("A" + (i));//身份證號碼
//                //试管编号
//                well.setTubeNo(i);//编号
//                //试管名称
//                well.setTubeName("A" + (i));
//                //样本类型
//                well.setType(SampleType.Unknown.getValue());
//                //病历号
//                well.setCaseID("A" + (i));
//                //属性
//                well.setProperty("Property");
//                wells.add(well);
//            }
//
//            Experiment.getInstance().setExperimentWells(wells);
//
//            Experiment.getInstance().setAuditor("winters");
//            Experiment.getInstance().setBatchNo("20251231");
//            Experiment.getInstance().setNote("test");
//            Experiment.getInstance().setOperator("winters");
//            Experiment.getInstance().setName("test");
//            Experiment.getInstance().setCustomColumns(new ArrayList<String>());
//            Experiment.getInstance().setPlateNo("test");
//
//            Experiment.getInstance().setExperimentWells(wells);
//
//            pCRReqMessage.setExperiment(Experiment.getInstance());
//
//            String result = WSSModuleManager.getInstance().sendPCRTemplateWSSMessage(selectWSSConnectionItemKey,pCRReqMessage);
////            APLManager.getInstance().refreshWss();
//            if(result.contains("succeed")){
//                return null;
//            }else {
//                return result;
//            }
//        } catch (Exception e) {
//            Log.i(TAG, e.getMessage());
//            return e.getMessage();
//        }
//    }
//    public String ExecutePCRSecond(String selectWSSConnectionItemKey,int seqid,String batchid,
//                                   int totaltime,int runtime,String template,int phase,int cycle) {
//
//        if(selectWSSConnectionItemKey == null){
//            return "请选择要发送模板的模块";
//        }
//        PCRReqMessage pCRReqMessage = new PCRReqMessage();
//        pCRReqMessage.setSeqId(seqid);//111
////            pCRReqMessage.setDeviceID("master");
////            pCRReqMessage.setDeviceType("master");
//        pCRReqMessage.setBatchID(batchid);
//        pCRReqMessage.setType("PCRReq");
////            pCRReqMessage.setPosition(2);
//        pCRReqMessage.setTemplate(template);//"13~新冠~second.prj"
//        pCRReqMessage.setCurrentPhase(phase);//0
//        pCRReqMessage.setCurrentCycle(cycle);//0
//        pCRReqMessage.setCurrentSection("");
//        pCRReqMessage.setCurrentStep("");
//        pCRReqMessage.setRunningTime(runtime);//0
//        pCRReqMessage.setTotalTime(totaltime);//1000
//
//        ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(Environment.getExternalStorageDirectory().getAbsolutePath()
//                + "/PCR/");//获取目录中的项目
//        Program program = null;
//        Project projectSend = null;
//
//        if(null != mConfigProjects && mConfigProjects.size()>0) {
//            for (Project project : mConfigProjects) {
//                if (null != project) {
//                    if (project.getName().contains("second")) {
//                        program = project.getProgram();
//                        projectSend = project;
//                    }
//                }
//            }
//        }
//
//        if(program == null){
//            return template +" 不存在";
//        }
//        try {
//            Experiment.getInstance().setProject(projectSend);
//            ArrayList<Well> wells = new ArrayList<>();
//            for (int i = 1; i <= 8; i++) {
//                Well well = new Well();
//                //孔名称
//                well.setName("A" + (i));
//                //项目名称
//                well.setProjectName("first.prj");
//                //患者信息.唯一标示
//                well.setID("A" + (i));
//                //姓名
//                well.setPatientName("PatientName");
//                //性别
//                well.setSex("Sex");
//                //年龄
//                well.setAge("Age");
//                //标本类型
//                well.setSpecimen("Specimen");
//                //采样日期
//                well.setSpecimenDate("SpecimenDate");
//                //送检医生
//                well.setDoctor("Doctor");
//                //送检科室
//                well.setOffice("Office");
//                //诊断
//                well.setDiagnosis("Diagnosis");
//                //病历号
//                well.setCaseID("caseID");
//                //病床号
//                well.setBedID("BedID");
//                //住院号
//                well.setInPatientID("InPatientID");
//                //门诊号
//                well.setOutPatientID("OutPatientID");
//                //备注
//                well.setRemark("Remark");
//                //样本名称
//                well.setSampleName("A" + (i));//身份證號碼
//                //试管编号
//                well.setTubeNo(i);//编号
//                //试管名称
//                well.setTubeName("A" + (i));
//                //样本类型
//                well.setType(SampleType.Unknown.getValue());
//                //病历号
//                well.setCaseID("A" + (i));
//                //属性
//                well.setProperty("Property");
//                wells.add(well);
//            }
//
//            Experiment.getInstance().setExperimentWells(wells);
//
//            Experiment.getInstance().setAuditor("winters");
//            Experiment.getInstance().setBatchNo("20251231");
//            Experiment.getInstance().setNote("test");
//            Experiment.getInstance().setOperator("winters");
//            Experiment.getInstance().setName("test");
//            Experiment.getInstance().setCustomColumns(new ArrayList<String>());
//            Experiment.getInstance().setPlateNo("test");
//
//            Experiment.getInstance().setExperimentWells(wells);
//
//            pCRReqMessage.setExperiment(Experiment.getInstance());
//            String result = WSSModuleManager.getInstance().sendPCRTemplateWSSMessage(selectWSSConnectionItemKey,pCRReqMessage);
////            APLManager.getInstance().refreshWss();
//            return result;
//        } catch (Exception e) {
//            Log.i(TAG, e.getMessage());
//            return e.getMessage();
//        }
//
//    }
}
