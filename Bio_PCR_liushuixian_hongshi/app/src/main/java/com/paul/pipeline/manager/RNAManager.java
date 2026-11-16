package com.paul.pipeline.manager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.listener.DeviceErrorCallback;
import com.paul.pipeline.message.ErrorInfoMessage;
import com.paul.pipeline.utils.MmkvUtils;
import com.paul.pipeline.model.ArmStep;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.fragment.model.PipeRNAModel;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.QueryDeviceMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAReqMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAStopMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeMessage;
import com.winters.boweilibrary.autopipeline.worker.RobotWorkManager;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class RNAManager {
    private static final String TAG = "RNAManager";
    private static RNAManager mInstance;
    public static RNAManager getInstance(){
        if (null == mInstance) {
            synchronized (RNAManager.class){
                if (null == mInstance){
                    mInstance = new RNAManager();
                }
            }
        }
        return  mInstance;
    }


    DeviceErrorCallback rnaErrorCallback;

    public void setRnaErrorCallback(DeviceErrorCallback rnaErrorCallback) {
        this.rnaErrorCallback = rnaErrorCallback;
    }
    public void clearData(Context context){
        if(rnalist!=null) {
            for (int i = 0; i < rnalist.size(); ++i) {
                int index = rnalist.get(i).index.get();
                rnalist.get(i).initModel(index, context.getString(R.string.pipe_main_rna_name) + index);
//                rnalist.set(i, newModel(context, index));
            }
        }
    }
    public void init(Context context){
        ArrayList<PipeRNAModel> list = new ArrayList<>();
        list.add(initModel(context, 1));
        list.add(initModel(context, 2));
        setRnalist(list);
    }
    private PipeRNAModel initModel(Context context, int index){
        PipeRNAModel model = MmkvUtils.getObject(ConstantData.nameRNA+index,PipeRNAModel.class);
        if(model==null) {
            model = newModel(context, index);
        }
        return model;
    }

    public void saveModel(int index){
        if(rnalist!=null) {
            for (int i = 0; i < rnalist.size(); ++i) {
                PipeRNAModel model = rnalist.get(i);
                if (model!=null&& model.index.get()!=null && index == model.index.get()) {
                    MmkvUtils.saveObject(ConstantData.nameRNA+index, model);
                }
            }
        }
    }
    private PipeRNAModel newModel(Context context, int index){
        PipeRNAModel model = new PipeRNAModel(index, context.getString(R.string.pipe_main_rna_name) + index);
        return model;
    }
    public void refreshConnectinfo(ConcurrentHashMap<String, WSSConnectionItem> items){
        SerialPortManager_Robot.RobotStatus robotStatus = SerialPortManager_Robot.getInstance().getRobotStatus();
        if(robotStatus!=null) {
            updateExist(1,robotStatus.getSenseByKey("RNA1"));
            updateExist(2,robotStatus.getSenseByKey("RNA2"));
        }else{
            updateExist(1,false);
            updateExist(2,false);
        }
       if(rnalist!=null) {
           for (int i = 0; i < rnalist.size(); i++) {
               PipeRNAModel model = rnalist.get(i);
               boolean connect = false;
               if (items != null) {
                   for (String key : items.keySet()) {
                       WSSConnectionItem item = items.get(key);
                       if (item.getDeviceType() != null && item.getDeviceType().equalsIgnoreCase(ConstantData.nameRNA)
                               && model.index.get() == item.getPosition()) {
                           if (item instanceof RNAStatusItem) {
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
                               RNAStatusItem rNAStatusItem = (RNAStatusItem) item;
                               model = refreshModel(model, rNAStatusItem);
                           } else {
                               Log.e(TAG, "TemplateWSSMessage: " + key + "is not RNA device");
                           }
                           break;
                       }
                   }
               } else {
                   refreshDisconnected(model);
                   Log.e("ArmTaskWorker", "RNA wss items null");
               }
               model.connect.set(connect);
           }
       }
    }
    public void refreshDisconnected(PipeRNAModel model){
        if(model!=null) {
            model.connect.set(false);
        }
    }
    public void updateExist(int index,boolean value){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null){
            model.exist.set(value);
        }
    }
    public boolean getExist(int index){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null) {
            return model.exist.get();
        }
        return false;
    }
    public boolean finishBacktray(int source, int target) {
        PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(source);
        if (model != null) {
            if (model.armStep.get() == ArmStep.BACK) {
                model.runtime.setBacktrayfinish(true);
                model.currentTray.set(0);
                model.armStep.set(ArmStep.FREE);
                model.runtime.setUiinit(true);//界面初始化
                Log.w("ArmTaskWorker", "finishBacktray RNA " + source + " 托盘" + target + " 归位");
                return true;
            }
        }
        return false;
    }
    public boolean finishMoveouttray(int source, int target) {
        PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(source);
        if (model != null) {
            if (model.armStep.get() == ArmStep.MOVE) {
                model.runtime.setMovetrayfinish(true);
                model.currentTray.set(0);
                model.armStep.set(ArmStep.FREE);
                model.runtime.setUiinit(true);//界面初始化
                Log.w("ArmTaskWorker", "finishMoveouttray RNA: " + source + " pcr:" + target + " move");
                return true;
            }
        }
        return false;
    }
    public void updateOpendoorFinish(int index,boolean value){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null){
                model.runtime.setOpendoorfinish(value);
        }
    }
//    public void updateTray(int index,int value){
//        PipeRNAModel model = getPipeRNAModel(index);
//        if(model!=null){
//                model.tray.set(value);
//        }
//    }
    public void updateExecuteStart(int index,boolean value){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null){
            model.runtime.setExcutestart(value);
            if(value){
                String startTime = DateFormatUtil.getSysFullTime();
                model.startTime.set(startTime);
            }
            saveModel(index);
        }
    }
    public void updateMovetraystart(int index,boolean value){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null){
            model.runtime.setMovetraystart(value);
            saveModel(index);
        }
    }
    public void updateBacktraystart(int index,boolean value){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null){
            model.runtime.setBacktraystart(value);
            saveModel(index);
        }
    }
    public void updateGettraystart(int index,boolean value){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null) {
            model.runtime.setGettraystart(value);//解除占用
            saveModel(index);
        }
    }
    public void clearDispatch(int index){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null) {
            if (model.index.get() == index) {
                model.armStep.set(ArmStep.FREE);
                saveModel(index);
            }
        }
    }
    public void updateCurrentTray(int index,int tray,int dispatchindex){
        PipeRNAModel m = getPipeRNAModel(index);
        if(m!=null) {
            m.currentTray.set(tray);
            m.dispatchindex.set(dispatchindex);
            saveModel(index);
        }
    }
    public boolean setCurrentTemplate(int index, DispatchModel dm,int status){
        PipeRNAModel m = getPipeRNAModel(index);
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
        PipeRNAModel m = getPipeRNAModel(index);
        if(m!=null&&m.currentTray.get()!=null){
            return TrayManager.getInstance().getCurrentDispatch(m.currentTray.get());
        }
        return null;
    }
    ArrayList<PipeRNAModel> rnalist = new ArrayList<>();

    public ArrayList<PipeRNAModel> getRnalist() {
        return rnalist;
    }

    public void setRnalist(ArrayList<PipeRNAModel> rnalist) {
        this.rnalist = rnalist;
    }

    public PipeRNAModel refreshModel(PipeRNAModel model,RNAStatusItem status){
        if(status.getrNALinuxStatus()!=null) {
            model.linuxStatus.set(status.getrNALinuxStatus());
        }
        model.linuxAlarm.set(status.getrNALinuxAlarm());
        if(status.getrNAStatus()!=null) {
            model.status.set(status.getrNAStatus());
        }
        model.sendAlarm.set(status.getrNASendAlarm());
        model.alarm.set(status.getrNAAlarm());
        model.leftTime.set(status.getrNALeftTime());
        model.totalTime.set(status.getrNATotalTime());
        model.info.set(status.getInfo());
        model.runninginfo.set(status.getrNARunninginfo());
        model.degree.set(""+status.getrNADegree());
        model.no.set(status.getNO());
        model.motoid.set(status.getrNAMotorId());
        model.currentTemplate.set(status.getrNACurrentTemplate());
       // model.currentTray.set(status.getrNACurrentTray());
        model.version.set(status.getVersion());
        if(status.getrNAStatus()!=null&&status.getrNAStatus().equalsIgnoreCase(ConstantData.str_ERROR)){
            model.allstatus.set(ConstantData.str_ERROR);
        }else if(status.getrNALinuxStatus()!=null&&status.getrNALinuxStatus().equalsIgnoreCase(ConstantData.str_ERROR)){
            model.allstatus.set(ConstantData.str_ERROR);
        }else if(model.usererror.get()!=null&&model.usererror.get()!=0){
            model.allstatus.set(ConstantData.str_ERROR);
        }else{
            model.allstatus.set(status.getrNAStatus());
        }
        createAlarm(model.index.get(),model.alarm.get(),model.linuxAlarm.get(),model.sendAlarm.get(),model.motoid.get(),model.alarmenable.get());
        return model;
    }
    private void createAlarm(int index,String alarm,String linuxalarm,String sendalarm,String motoid,boolean alarmdialog){
        boolean send = false;
        ErrorInfoMessage msg = new ErrorInfoMessage();
        msg.setIndex(index);
        msg.setType(ConstantData.DEVICE_RNA);
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
            msg.setMotoid(motoid);
            if(rnaErrorCallback!=null){
                rnaErrorCallback.error(msg);
            }
        }
    }
    public PipeRNAModel getFreeModel() {
        for (int i = 0; i < rnalist.size();i++) {
            PipeRNAModel m = rnalist.get(i);
            if(m.connect.get() && !m.exist.get()
                    && m.armStep.get() == ArmStep.FREE
                    && m.status.get().equalsIgnoreCase(ConstantData.str_FREE)
                    && m.linuxStatus.get().equalsIgnoreCase(ConstantData.str_FREE)){
                Log.w("ArmTaskWorker", "找到空闲判断rna: "+m.index.get());
                return m;
            }
            Log.w("ArmTaskWorker", "空闲判断rna: "+m.index.get()+" connect: "+m.connect.get()
                    +" exist："+m.exist.get()+" armStep："+m.armStep.get()+" status："+m.status.get()
                    +" linuxStatus："+m.linuxStatus.get());
        }
        Log.i("ArmTaskWorker", "rna:无空闲 ");
        return null;
    }
    public PipeRNAModel getPipeRNAModel(int index){
        if(rnalist!=null) {
            for (int i = 0; i < rnalist.size(); i++) {
                PipeRNAModel model = rnalist.get(i);
                if(model!=null) {
                    if (model.index.get() == index) {
                        return model;
                    }
                }
            }
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

    public boolean setCloseRNADoor(int index){
        PipeRNAModel model = getPipeRNAModel(index);
        if(model!=null){
            model.runtime.setClosedoor(true);
            return true;
        }else{
            return false;
        }
    }

    public boolean reset(int index){
        String key = APLManager.getInstance().getKey(ConstantData.DEVICE_RNA,index);
        if(key==null) {
            Log.e("ArmTaskWorker","rna test reset key null:"+index);
            return false;
        }
        String result = ExecuteResetForce(key);
        if(result.contains("succeed")) {
            return true;
        }else{
            return false;
        }
    }
//    public boolean resetException(int index){
//        String key = APLManager.getInstance().getKey(ConstantData.TYPE_RNA,index);
//        if(key==null) {
//            Log.e("ArmTaskWorker","rna test resetException key null:"+index);
//            return false;
//        }
//        String result = ExecuteResetExpetion(key);
//        if(result.contains("succeed")) {
//
//            return true;
//        }else{
//            return false;
//        }
//    }
    public boolean testexecute(int index){
        String key = APLManager.getInstance().getKey(ConstantData.DEVICE_RNA,index);
        if(key==null) {
            Log.e("ArmTaskWorker","rna test execute key null:"+index);
            return false;
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/" ;
        String result = ExecuteRNA(key,getSeqid(),"20251112",1000L, "99~test","first.hex",path,0,0);
        if(result==null) {
            Log.w("ArmTaskWorker","rna test execute :"+index +" key:"+key);
            return true;
        }else{
            Log.e("ArmTaskWorker","rna test execute fail:"+result);
            return false;
        }
    }
    public boolean execute(int index){
        DispatchModel msg = getCurrentTemplate(index);
        if(msg==null){
            Log.e("ArmTaskWorker","rna execute msg null:"+index);
            return false;
        }
        String key = APLManager.getInstance().getKey(ConstantData.DEVICE_RNA,index);
        if(key==null) {
            Log.e("ArmTaskWorker","rna execute key null:"+index);
            return false;
        }
        long totaltime = 0l;
        int firstsecond = msg.firstSecond.get();
        if(msg.totaltime.get()!=null) {
            totaltime = msg.totaltime.get().getTotalTime();
            if (firstsecond == 1) {
                totaltime = msg.totaltime.get().getFirstRNATime();
            } else if (firstsecond == 2) {
                totaltime = msg.totaltime.get().getSecondRNATime();
            }
        }
        String result = ExecuteRNA(key,getSeqid(),msg.batch.get(),totaltime,msg.projectname.get(),
                msg.templatename.get(),msg.path.get(),msg.phase.get(),msg.phasestep.get());
        if(result==null) {
            Log.w("ArmTaskWorker","rna execute :"+index +" key:"+key
                    +" Totaltime:"+msg.totaltime.get()+" Template:"+ msg.templatename.get());
            return true;
        }else{
            Log.e("ArmTaskWorker","rna execute fail:"+result);
            return false;
        }
    }

    public String setRNA(int index, String selectWSSConnectionItemKey){
        QueryDeviceMessage queryDeviceMessage = new QueryDeviceMessage();
        queryDeviceMessage.setType("QueryDevice");
        queryDeviceMessage.setSeqId(index);
        queryDeviceMessage.setPosition(index);
        queryDeviceMessage.setDeviceType("RNA");
        queryDeviceMessage.setDeviceID("RNA" + index);
        queryDeviceMessage.setCommand("setRNA_position");
        if(selectWSSConnectionItemKey == null){
            return "请选择要发送模板的模块";
        }
        String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
        if(!result.contains("error")){
//            APLManager.getInstance().refreshWss();
            return null;//"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed";
        }else{
            return "sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error";
        }
    }

    public String ExecuteRNA(String selectWSSConnectionItemKey,int seqid,String batchid,long time,
                             String projectname,String template,String path,int phase, int phasestep) {
        RNAReqMessage rNAReqMessage = new RNAReqMessage();
        rNAReqMessage.setSeqId(seqid);//111
        rNAReqMessage.setBatchID(batchid);//123
        rNAReqMessage.setTotalTime((int)time);//1000
//            rNAReqMessage.setDeviceID("master");
//            rNAReqMessage.setDeviceType("master");
        rNAReqMessage.setType("RNAReq");
        rNAReqMessage.setTemplate(projectname+"-"+template);//"13~xinguan~first.hex"
        rNAReqMessage.setCurrentPhase(phase);//0
        rNAReqMessage.setCurrentPhaseStep(phasestep);//0

        try {
//            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
//                    + "/PCR/" +projectname +"/"+ template;// "first.hex";

            String filePath = path + File.separator + template;
            Log.e("ExecuteRNA", filePath);
            List<HexDebugEntity> HexDebugEntityList = ListDataSaveUtil.getHexDebugEntityList(new File(filePath));

            Log.e("ExecuteRNA", filePath + "-" + HexDebugEntityList.size());

            rNAReqMessage.setWorkTemplate(HexDebugEntityList);

            if(selectWSSConnectionItemKey == null){
                return "请选择要发送模板的模块";
            }
            String result = WSSModuleManager.getInstance().sendRNATemplateWSSMessage(selectWSSConnectionItemKey,rNAReqMessage);
//            APLManager.getInstance().refreshWss();
            if(result.contains("succeed")){
                return null;
            }else {
                return result;
            }
            //Toast.makeText(mContext, "sendRNATemplateWSSMessage" +  result,Toast.LENGTH_LONG).show();
           // wssConnectionAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
          //  Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            return e.getMessage();
        }

    }
    public String errorContinue(int index){
        try {
            DispatchModel msg = getCurrentTemplate(index);
            if(msg==null){
                Log.e("ArmTaskWorker","rna execute msg null:"+index);
                return "调度信息不存在";
            }
            String key = APLManager.getInstance().getKey(ConstantData.DEVICE_RNA,index);
            if(key==null) {
                Log.e("ArmTaskWorker","rna execute key null:"+index);
                return "找不到key";
            }
            long totaltime = 0l;
            int firstsecond = msg.firstSecond.get();
            if(msg.totaltime.get()!=null) {
                totaltime = msg.totaltime.get().getTotalTime();
                if (firstsecond == 1) {
                    totaltime = msg.totaltime.get().getFirstRNATime();
                } else if (firstsecond == 2) {
                    totaltime = msg.totaltime.get().getSecondRNATime();
                }
            }
            String result = resumeRna(key,getSeqid(),msg.batch.get(),(int)totaltime,msg.projectname.get(),
                    msg.templatename.get(),msg.path.get());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", "rna errorContinue : " + e.getMessage());
            return "Exception:"+e.getMessage();
        }
    }

    public String resumeRna(String selectWSSConnectionItemKey,int seqid,String batchid,int time,
                            String projectname,String template,String path){
        RNAReqMessage rNAReqMessage = new RNAReqMessage();
        rNAReqMessage.setSeqId(seqid);//111
        rNAReqMessage.setBatchID(batchid);//123+""
        rNAReqMessage.setTotalTime(time);//1000
        rNAReqMessage.setType("RNAReq");
        rNAReqMessage.setTemplate(projectname+"-"+template);//"13~xinguan~first.hex"
        rNAReqMessage.setForce(1);

        try {
//            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
//                    + "/PCR/" + "first.hex";
            String filePath = path + File.separator + template;
            List<HexDebugEntity> HexDebugEntityList = ListDataSaveUtil.getHexDebugEntityList(new File(filePath));
//            if (filePath.contains("first") || filePath.contains("second")){
//                String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "97-downandopen.hex";
//                File downandopenfile = new File(downandopen_filePath);
//                List<HexDebugEntity> HexDebugEntityList_downandopen = ListDataSaveUtil.getHexDebugEntityList(downandopenfile);
//                HexDebugEntityList.addAll(HexDebugEntityList_downandopen);
//            }

            rNAReqMessage.setWorkTemplate(HexDebugEntityList);

            WSSConnectionItem wSSConnectionItem = WSSModuleManager.getInstance().getWssConnectionItems().get(selectWSSConnectionItemKey);

            if(wSSConnectionItem == null){
               // Toast.makeText(mContext, "请选择要发送模板的RNA模块",Toast.LENGTH_LONG).show();
                return "请选择要发送模板的RNA模块";
            }

            if(wSSConnectionItem instanceof RNAStatusItem == false){
              //  Toast.makeText(mContext, "请选择RNA模块",Toast.LENGTH_LONG).show();
                return "请选择RNA模块";

            }

            String status = ((RNAStatusItem)wSSConnectionItem).getrNAStatus();

            String lastRunningInfo = ((RNAStatusItem)wSSConnectionItem).getNO();

            if(status.equals("free") || status.equals("ok")|| status.equals("running")){
               // Toast.makeText(mContext, "当前模块状态为"+ status + "，不能恢复运行",Toast.LENGTH_LONG).show();
                return "当前模块状态为"+ status + "，不能恢复运行";

            }else{
                Log.i("ResumeRNA","lastRunningInfo" + lastRunningInfo);
                if(lastRunningInfo!=null){
                    String[] info = lastRunningInfo.split("\\|",-1);
                    if(info != null && info.length>=5){
                        String rna_interuppt_time = info[4];
                        rNAReqMessage.setCurrentPhase(Integer.parseInt(info[1]));
                        rNAReqMessage.setCurrentPhaseStep(Integer.parseInt(info[2]));
                        Log.i("ResumeRNA","ResumeRNA" + rna_interuppt_time + "--" + info[1] + "--" + info[2]);

                    }else if(info != null && info.length>=4){
                        String rna_interuppt_time = "";
                        rNAReqMessage.setCurrentPhase(Integer.parseInt(info[1]));
                        rNAReqMessage.setCurrentPhaseStep(Integer.parseInt(info[2]));
                        Log.i("ResumeRNA","ResumeRNA" + rna_interuppt_time + "--" + info[1] + "--" + info[2]);
                    }
                }
            }

            if(selectWSSConnectionItemKey == null){
              //  Toast.makeText(mContext, "请选择要发送模板的RNA模块",Toast.LENGTH_LONG).show();
                return "请选择要发送模板的RNA模块";
            }
            String result = WSSModuleManager.getInstance().sendRNATemplateWSSMessage(selectWSSConnectionItemKey,rNAReqMessage);
          //  Toast.makeText(mContext, "sendRNATemplateWSSMessage" +  result,Toast.LENGTH_LONG).show();
            //wssConnectionAdapter.notifyDataSetChanged();
            if(result.contains("succeed")){
                return null;
            }
            return result;
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
            //Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            return "发生异常："+e.getMessage();
        }
    }

    public String ExecuteReset(String selectWSSConnectionItemKey,int seqid,String batchid,int time) {
        RNAReqMessage rNAReqMessage = new RNAReqMessage();
        rNAReqMessage.setSeqId(seqid);
        rNAReqMessage.setBatchID(batchid);
        rNAReqMessage.setTotalTime(time);
        //rNAReqMessage.setDeviceID("master");
        //rNAReqMessage.setDeviceType("master");
        rNAReqMessage.setType("RNAReq");
        rNAReqMessage.setTemplate("99~reset~reset.hex");
        //rNAReqMessage.setTemplate("99");
        rNAReqMessage.setCurrentPhase(0);
        rNAReqMessage.setCurrentPhaseStep(0);
        try {
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/PCR/commonHex_v5/" + "99-reset.hex";
            List<HexDebugEntity> HexDebugEntityList =
                    ListDataSaveUtil.getHexDebugEntityList(new File(filePath));

            rNAReqMessage.setWorkTemplate(HexDebugEntityList);

            if(selectWSSConnectionItemKey == null){
                return "请选择要发送模板的模块";
            }
            String result = WSSModuleManager.getInstance().sendRNATemplateWSSMessage(selectWSSConnectionItemKey,rNAReqMessage);
//            APLManager.getInstance().refreshWss();
            return result;
        } catch (Exception e) {
            Log.i(TAG, e.getMessage());
            return e.getMessage();
        }

    }

//    public String ExecuteResetExpetion(String selectWSSConnectionItemKey) {
//        String result = WSSModuleManager.getInstance().sendRNAResetWSSMessage(selectWSSConnectionItemKey,0);
//        return result;
//    }

    ///
    /// 强制复位
    public String ExecuteResetForce(String selectWSSConnectionItemKey) {
        String result = WSSModuleManager.getInstance().sendRNAResetWSSMessage(selectWSSConnectionItemKey, 1);
        return result;
    }

    public String StopRNA(String selectWSSConnectionItemKey) {
        RNAStopMessage rNAStopMessage = new RNAStopMessage();
        rNAStopMessage.setSeqId(111);
        rNAStopMessage.setType("RNAStop");
        if (selectWSSConnectionItemKey == null) {
            return "请选择要发送的模块";
        }
        String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey, rNAStopMessage);
        return result;
    }

    public boolean stopRnaWork(int index){
        String key = APLManager.getInstance().getKey(ConstantData.DEVICE_RNA,index);
        if(key==null){
            Log.e("ArmTaskWorker","rna:"+index+" key null");
            return false;
        }
        String result = RNAManager.getInstance().StopRNA(key);
        if(result.contains("succeed")) {
            Log.i("ArmTaskWorker","rna stop:"+index);
            return true;
        }else{
            Log.e("ArmTaskWorker","rna stop fail:"+index);
            return false;
        }
    }
    public boolean updateRna(String key,String version,String filepath) {
       // Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/" + filename
        //String filename = "rna_app_rna_01_00_18_20251009.tar.gz";
//        filename.replace("rna_app_", "").replace(".tar.gz", "")
        UpgradeMessage upgradeMessage = new UpgradeMessage();
        upgradeMessage.setForce(1);
        upgradeMessage.setSeqId(getSeqid());
        upgradeMessage.setType("Upgrade");
        upgradeMessage.setVersion(version);
        try {
            if (key == null) {
                Log.e(TAG, "updateRna fail key null:"+key);
               // Toast.makeText(mContext, "请选择要发送模板的RNA模块", Toast.LENGTH_LONG).show();
                return false;
            }
            if (version == null) {
                Log.e(TAG, "updateRna fail version null:"+version);
                return false;
            }
            if (filepath == null) {
                Log.e(TAG, "updateRna fail filepath null:"+filepath);
                return false;
            }
            String result = WSSModuleManager.getInstance().sendRNAUpgradeWSSMessage(key, upgradeMessage,filepath);
            if(result.contains("succeed")){
                return true;
            }
            Log.e(TAG, "updateRna error:"+result);
            return false;
            //Toast.makeText(mContext, "sendRNAUpgradeWSSMessage" + result, Toast.LENGTH_LONG).show();
            //wssConnectionAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "updateRna exception:"+e.getMessage());
            e.printStackTrace();
            return false;
            //Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
