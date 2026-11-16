package com.paul.pipeline.manager;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.listener.ArmResetCallback;
import com.paul.pipeline.message.ArmworkMessage;
import com.paul.pipeline.message.ResultMessage;
import com.paul.pipeline.model.SampleHoleModel;
import com.paul.pipeline.utils.MmkvUtils;
import com.paul.pipeline.listener.ArmFinishCallback;
import com.paul.pipeline.fragment.model.PipeArmModel;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.worker.RobotWorkManager;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;
import com.winters.boweilibrary.autopipeline.model.item.QRCodeItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ArmManager {
    private static final String TAG = "ArmManager";
    private static ArmManager mInstance;
    public static ArmManager getInstance(){
        if (null == mInstance) {
            synchronized (ArmManager.class){
                if (null == mInstance){
                    mInstance = new ArmManager();
                }
            }
        }
        return  mInstance;
    }
    public ArmManager(){

    }

    public List<ItemProperty> itemPropertyList = new ArrayList<>();//机械臂对象
    //    public List<QRCodeItem> qRCodeItems = new ArrayList<>();
    public ItemProperty itemProperty_source;//源位置
    public ItemProperty itemProperty_target;//目标位置

    public void init(Context context){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        initArmModel(context);
        RobotWorkManager.getInstance().setWorkQueueListener(new RobotWorkManager.WorkQueueListener() {
            @Override
            public void onAllTasksCompleted(String text) {
                updateFinish(1,true);
                if(text!=null&&text.contains(ConstantData.RESET_EXCEPTION)) {
                    if (armResetCallback != null) {
                        armResetCallback.finish(true, text);
                    }
                }else {
                    setcallback(true, text);
                }
            }

            @Override
            public void onTaskFailed(String text) {
                if(text!=null&&text.contains(ConstantData.RESET_EXCEPTION)) {
                    if (armResetCallback != null) {
                        armResetCallback.finish(false, text);
                    }
                }else {
                    setcallback(false, text);
                }
            }

            @Override
            public void onQrCodeRevert(List<QRCodeItem> qRCodeItems, int panNum) {
                if(qRCodeItems!=null&&qRCodeItems.size()!=0) {
                    checkHoles(panNum,qRCodeItems);
                }
            }
        });
    }

    public void clearData(Context context){
        if(armlist!=null) {
            for (int i = 0; i < armlist.size(); ++i) {
                int index = armlist.get(i).index.get();
                armlist.get(i).initModel(index,context.getString(R.string.pipe_main_arm_name));
//                armlist.set(i, newModel(context, index));
            }
        }
    }
    public void initArmModel(Context context){
        ArrayList<PipeArmModel> list = new ArrayList<>();
        list.add(initPipeArmModel(context,1));
        setArmlist(list);
    }

    private PipeArmModel initPipeArmModel(Context context, int index){
        PipeArmModel model = MmkvUtils.getObject(ConstantData.nameARM+index,PipeArmModel.class);
        if(model==null) {
            model = newModel(context, index);
        }
        model.finish.set(true);
        return model;
    }

    public void saveModel(int index){
        if(armlist!=null) {
            for (int i = 0; i < armlist.size(); ++i) {
                PipeArmModel model = armlist.get(i);
                if (model!=null && index == model.index.get()) {
                    MmkvUtils.saveObject(ConstantData.nameARM+index, model);
                }
            }
        }
    }
    private PipeArmModel newModel(Context context, int index){
        PipeArmModel model = new PipeArmModel(index,context.getString(R.string.pipe_main_arm_name));
        model.finish.set(true);
        return model;
    }
    public void refreshConnectinfo(){
        for (int i = 0; i < armlist.size(); i++) {
            PipeArmModel model = armlist.get(i);
            if (model.index.get() == 1) {
                refreshModel(model);
            }
        }
    }

    public PipeArmModel refreshModel(PipeArmModel model){
        SerialPortManager_Robot.RobotStatus status = SerialPortManager_Robot.getInstance().getRobotStatus();
        if(status!=null) {
//            Log.e("ArmTaskWorker", "机械臂 connect:"+status.isROBOTconnect());
//            Log.e("ArmTaskWorker", "机械臂 status:"+status.getStatus());
            model.exist.set(status.getSenseByKey("ARM"));
            model.status.set(status.getStatus());
            model.connect.set(status.isROBOTconnect());
            model.degree.set("" + status.getDegree());
            model.no.set(status.getNO());
            model.motorID.set(status.getMotorID());
            model.taskRunning.set(status.isROBOTTaskRunning() == true ? "运行中" : "空闲");
            model.alarm.set(status.getAlarm());
            if(itemProperty_source!=null){
                model.source.set(itemProperty_source.getType()+itemProperty_source.getPosition());
            }else {
                model.source.set("无");
            }
            if(itemProperty_target!=null){
                model.target.set(itemProperty_target.getType()+itemProperty_target.getPosition());
            }else {
                model.target.set("无");
            }
        }else{
            model.exist.set(false);
            model.connect.set(false);
            model.source.set("");
            model.target.set("");
        }
        return model;
    }
    ArrayList<PipeArmModel> armlist = new ArrayList<>();
    public ArrayList<PipeArmModel> getArmlist() {
        return armlist;
    }

    public void setArmlist(ArrayList<PipeArmModel> armlist) {
        this.armlist = armlist;
    }

    public PipeArmModel getArmModel(int index){
        for(int i=0;i<armlist.size();i++){
            PipeArmModel model = armlist.get(i);
            if(model.index.get()==index) {
                return model;
            }
        }
        return null;
    }

    public void updateArmworkMessage(int index, ArmworkMessage value){
        for(int i=0;i<armlist.size();i++){
            PipeArmModel model = armlist.get(i);
            if(model.index.get()==index) {
                model.currentArmworkMessage.set(value);
                saveModel(1);
                break;
            }
        }
    }
    public void updateAction(int index,String value){
        for(int i=0;i<armlist.size();i++){
            PipeArmModel model = armlist.get(i);
            if(model.index.get()==index) {
                model.action.set(value);
                break;
            }
        }
    }
    public void updateFinish(int index,boolean value){
        for(int i=0;i<armlist.size();i++){
            PipeArmModel model = armlist.get(i);
            if(model.index.get()==index) {
                model.finish.set(value);
                break;
            }
        }
    }
    public void updateTray(int index,int tray){
        for(int i=0;i<armlist.size();i++){
            PipeArmModel model = armlist.get(i);
            if(model.index.get()==index) {
                model.tray.set(tray);
                break;
            }
        }
    }
    public void updateArmwork(int index,int value){
        for(int i=0;i<armlist.size();i++){
            PipeArmModel model = armlist.get(i);
            if(model.index.get()==index) {
                model.armwork.set(value);
                break;
            }
        }
    }
    public void setSource(int type,int index){
        switch(type){
            case ConstantData.DEVICE_TRAY:
                itemProperty_source = getItemPropertyTray(index);
                saveSource();
                break;
            case ConstantData.DEVICE_RNA:
                itemProperty_source = getItemPropertyRNA(index);
                saveSource();
                break;
            case ConstantData.DEVICE_PCR:
                itemProperty_source = getItemPropertyPCR(index);
                saveSource();
                break;
            default:
                itemProperty_source = null;
                saveSource();
                break;
        }
    }
    public void setTarget(int type,int index){
        switch(type){
            case ConstantData.DEVICE_TRAY:
                itemProperty_target = getItemPropertyTray(index);
                saveTarget();
                break;
            case ConstantData.DEVICE_RNA:
                itemProperty_target = getItemPropertyRNA(index);
                saveTarget();
                break;
            case ConstantData.DEVICE_PCR:
                itemProperty_target = getItemPropertyPCR(index);
                saveTarget();
                break;
            default:
                itemProperty_target = null;
                saveTarget();
                break;
        }
    }
    private void saveSource(){
        PipeArmModel model = getArmModel(1);
        if(model!=null) {
            model.source.set(itemProperty_source != null ? itemProperty_source.getType() + itemProperty_source.getPosition() : "");
            if(itemProperty_source != null) {
                model.sourcePosition.set( itemProperty_source.getPosition());
            }
        }
    }
    private void saveTarget(){
        PipeArmModel model = getArmModel(1);
        if(model!=null) {
            model.target.set(itemProperty_target != null ? itemProperty_target.getType() + itemProperty_target.getPosition() : "");
            if(itemProperty_target != null) {
                model.targetPosition.set( itemProperty_target.getPosition());
            }
        }
    }

    public ItemProperty getItemPropertyTray(int index){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        if(itemPropertyList!=null) {
            for (ItemProperty item : itemPropertyList) {
                if (item.getType().equalsIgnoreCase(ConstantData.nameTray)) {
                    if (item.getPosition() == index) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public ItemProperty getItemPropertyRNA(int index){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        if(itemPropertyList!=null) {
            for (ItemProperty item : itemPropertyList) {
                if (item.getType().equalsIgnoreCase(ConstantData.nameRNA)) {
                    if (item.getPosition() == index) {
                        return item;
                    }
                }
            }
        }
        return null;
    }
    public ItemProperty getItemPropertyPCR(int index){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        if(itemPropertyList!=null) {
            for (ItemProperty item : itemPropertyList) {
                if (item.getType().equalsIgnoreCase(ConstantData.namePCR)) {
                    if (item.getPosition() == index) {
                        return item;
                    }
                }
            }
        }
        return null;
    }
    public ItemProperty getItemPropertyPCRCover(int index){
        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();
        if(itemPropertyList!=null) {
            for (ItemProperty item : itemPropertyList) {
                if (item.getType().equalsIgnoreCase(ConstantData.namePCRCover)) {
                    if (item.getPosition() == index) {
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public void setFree(){
        itemProperty_source = null;
        itemProperty_target = null;
    }

    ArmFinishCallback armFinishCallback;

    public void setArmFinishCallback(ArmFinishCallback armFinishCallback) {
        this.armFinishCallback = armFinishCallback;
    }
    ArmResetCallback armResetCallback;

    public void setArmResetCallback(ArmResetCallback armResetCallback) {
        this.armResetCallback = armResetCallback;
    }

    private void setcallback(boolean result, String text){
        Log.e("ArmTaskWorker", "arm1 callback :"+result);
        if(result) {
            if (armFinishCallback != null) {
                armFinishCallback.finish(1, itemProperty_target, itemProperty_source);
            }
            setFree();
        }else{
            if (armFinishCallback != null) {
                armFinishCallback.fail(1, itemProperty_target, itemProperty_source,text);
            }
        }
    }

    public String startGet(int type,int index) {
//        if(armCanuse()) {
            setSource(type,index);
            if (itemProperty_source == null) {
                return "请选择源位置或者目标位置";
            } else {
                try {
                    updateArmwork(1,ConstantData.ARM_GET);
                    saveModel(1);
                    SerialPortManager_Robot.getInstance().robotGet(itemProperty_source);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ArmTaskWorker", "startGet : " + e.getMessage());
                    return "startGet : " + e.getMessage();
                }
            }
            return null;
//        }else{
//            return "机械臂忙！";
//        }
    }

    public String startPut(int type,int index) {
            setTarget(type, index);
            if (itemProperty_target == null) {
                return "请选择源位置或者目标位置";
            } else {
                try {
                    updateArmwork(1,ConstantData.ARM_PUT);
                    saveModel(1);
                    SerialPortManager_Robot.getInstance().robotPut(itemProperty_target);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("ArmTaskWorker", "stop : " + e.getMessage());
                    return "stop : " + e.getMessage();
                }
            }
            return null;
    }
    public void stop() {
        try {
            updateArmwork(1,ConstantData.ARM_STOP);
            saveModel(1);
            SerialPortManager_Robot.getInstance().robotStop();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", "stop : " + e.getMessage());
        }
    }

    public String lastAction;
    public String lastDetail;
    ///异常继续
    public String errorContinue(){

        if(lastAction==null || !lastAction.contains(":")){
            return "动作数据错误";
        }
        String[] actions = lastAction.split(":");

        if(itemPropertyList==null){
            return "设备数据为空";
        }
        ItemProperty itemProperty_temp_source = null;

        ItemProperty itemProperty_temp_target = null;

        if(actions[1]!= null){
            for(ItemProperty itemProperty : itemPropertyList){
                if((itemProperty.getType() + itemProperty.getPosition()).equals(actions[1])){
                    itemProperty_temp_source = itemProperty;
                }
            }
        }

        if(actions[2]!= null){
            for(ItemProperty itemProperty : itemPropertyList){
                if((itemProperty.getType() + itemProperty.getPosition()).equals(actions[2])){
                    itemProperty_temp_target = itemProperty;
                }
            }
        }

        Log.i(TAG, "resumeRobotWork : actions[1] " + actions[1] );
        Log.i(TAG, "resumeRobotWork : actions[2] " + actions[2] );

        if(itemProperty_temp_source == null || itemProperty_temp_target == null){
            return "resumeRobotWork : error";
        }

        int panNum = 0;
        if(itemProperty_source.getType().equals("PAN")){
            panNum = itemProperty_source.getPosition();
        }else if(itemProperty_target.getType().equals("PAN")){
            panNum = itemProperty_target.getPosition();
        }
        ArrayList<SampleHoleModel> samples = TrayManager.getInstance().getSampleList(panNum);
        List<QRCodeItem> qRCodeItems = getQRCodeItems(samples);
        boolean result = false;
        try {
            updateArmwork(1,ConstantData.ARM_CONTINUE);
            saveModel(1);
            result = RobotWorkManager.getInstance().resumeRobotWork(itemProperty_temp_source, itemProperty_temp_target, qRCodeItems, panNum, lastAction);
            if (!result) {
                Log.i(TAG, "resumeRobotWork : error");
                return "resumeRobotWork : error";
            } else {
                ArmworkMessage msg = getArmModel(1).currentArmworkMessage.get();
                if (msg != null) {
                    //再试一次
                    TrayManager.getInstance().updateDsiapatchStatus(panNum, msg.getDispatchindex(), ConstantData.STATUS_RUN);
                }

                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", "robotReset : " + e.getMessage());
            return "Exception:"+e.getMessage();
        }
    }
    //异常复位
    public boolean robotReset(ArmResetCallback resetCallback) {
        boolean result = false;
        try {
            updateArmwork(1,ConstantData.ARM_RESET);
            setArmResetCallback(resetCallback);
            saveModel(1);
//            result = SerialPortManager_Robot.getInstance().robotReset();
            result = RobotWorkManager.getInstance().startSingleReset();
            if (result) {
                updateFinish(1,true);
                Log.i("ArmTaskWorker", "robotReset : " + result);
            } else {
                Log.e("ArmTaskWorker", "robotReset : " + result);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", "robotReset : " + e.getMessage());
            return false;
        }
    }
    //复位弃用
//    public boolean reset() {
//        boolean result = SerialPortManager_Robot.getInstance().robotReset();
//        return result;
//    }
    private List<QRCodeItem> getQRCodeItems(ArrayList<SampleHoleModel> samples){
        List<QRCodeItem> qrlist = new ArrayList<>();
        if(samples!=null) {
            for (int i = 0; i < samples.size(); i++) {
                SampleHoleModel model = samples.get(i);
                QRCodeItem item = new QRCodeItem();
                item.setIndex(model.index.get());
                item.setQrcode(model.scaninfo.get());
                qrlist.add(item);
            }
        }
        return qrlist;
    }

    private ArrayList<SampleHoleModel> checkHoles(int tray,List<QRCodeItem> qrlist){
        boolean flag = false;
        PipeArmModel armmodel = getArmModel(1);
        if(armmodel!=null){
            flag = armmodel.autosamplecode.get();
        }
        ArrayList<SampleHoleModel> holelist = TrayManager.getInstance().getSampleList(tray);
        if(flag) {
            if(qrlist!=null) {
                for (int i = 0; i < qrlist.size(); i++) {
                    QRCodeItem item = qrlist.get(i);
                    String code = item.getQrcode();
                    int itemindex = item.getIndex();
                    if (code != null && !code.isBlank() && !code.isEmpty()) {
                        for (int j = 0; j < holelist.size(); j++) {
                            SampleHoleModel model = holelist.get(j);
                            if (itemindex == model.index.get()) {
                                if (!code.equalsIgnoreCase(model.scaninfo.get())) {
                                    model.scaninfo.set(code);
                                }
                            }
                        }
                    }
                }
            }
        }
        return holelist;
    }

    public String startArmRobot(int source,int sourcetype,int target,int targettype,ArmFinishCallback finishCallback,int tray) {
        if(armCanuse()) {
            setSource (sourcetype,source);
            setTarget(targettype,target);
            if(itemProperty_source == null ){
                return "请选择源位置或者目标位置";
            }
            if(itemProperty_target == null){
                return "请选择源位置或者目标位置";
            }
            if(itemProperty_source.getType().equals("PAN")&&itemProperty_target.getType().equals("PAN")){
                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
            }
            if(itemProperty_source.getType().equals("PCR")&&itemProperty_target.getType().equals("PCR")){
                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
            }
            if(itemProperty_source.getType().equals("RNA")&&itemProperty_target.getType().equals("RNA")){
                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
            }
//            if(itemProperty_source.getType().equals("RNA")&&itemProperty_target.getType().equals("PCR")){
//                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
//            }
//            if(itemProperty_source.getType().equals("PCR")&&itemProperty_target.getType().equals("RNA")){
//                return "请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合";
//            }
            try {
                int panNum = 0;
                if(itemProperty_source.getType().equals("PAN")){
                    panNum = itemProperty_source.getPosition();
                }else if(itemProperty_target.getType().equals("PAN")){
                    panNum = itemProperty_target.getPosition();
                }else{
                    panNum = tray;
                }
                updateTray(1,tray);//记住盘来源
                ArrayList<SampleHoleModel> samples = TrayManager.getInstance().getSampleList(panNum);
                List<QRCodeItem> qRCodeItems = getQRCodeItems(samples);
                updateFinish(1,false);
                updateArmwork(1,ConstantData.ARM_START);
                saveModel(1);
                setArmFinishCallback(finishCallback);
                boolean result = RobotWorkManager.getInstance().startRobotWork(itemProperty_source, itemProperty_target, qRCodeItems,panNum);
                if (!result) {
                    updateFinish(1,true);
                    Log.e("ArmTaskWorker", "startRobotWork : error");
                    return "startRobotWork : error";
                } else {
                    Log.i("ArmTaskWorker", "startRobotWork : succeed");
                    return null;
                }
            }catch (Exception e){
                e.printStackTrace();
                Log.e("ArmTaskWorker", "startRobotWork : " + e.getMessage());
                return "startRobotWork : exception";
            }
        }else{
            return "机械臂忙！";
        }
    }

    public boolean putTray(int index,ArmFinishCallback finishCallback){
        try{
            itemProperty_source = null;
            itemProperty_target = getItemPropertyTray(index);
            if(itemProperty_target==null){
                Log.e(TAG, "putTray 找不到目标 tray"+index);
                return false;
            }
            updateFinish(1, false);
            updateArmwork(1,ConstantData.ARM_PUTTRAY);
            saveModel(1);
            setArmFinishCallback(finishCallback);
            boolean result = RobotWorkManager.getInstance().startSingleAcion(itemProperty_source,itemProperty_target,"put_to_pan");
            if(!result){
                updateFinish(1,true);
                Log.e(TAG, "startSingleAcion put_to_pan : error");
                // Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "putTray : " + e.getMessage());
            return false;
        }
    }

    public boolean putPcrCover(int index,ArmFinishCallback finishCallback){
        try{
            itemProperty_source = null;
            itemProperty_target = getItemPropertyPCRCover(index);
            if(itemProperty_target == null){
                Log.e(TAG, "putPcrCover 找不到目标 pcr"+index);
                return false;
            }
            updateFinish(1, false);
            updateArmwork(1,ConstantData.ARM_PCRCOVER);
            saveModel(1);
            setArmFinishCallback(finishCallback);
            boolean result = RobotWorkManager.getInstance().startSingleAcion(itemProperty_source, itemProperty_target,"put_pcr_cover_to_pcr");
            Log.e(TAG, "startSingleAcion put_pcr_cover_to_pcr : target "+itemProperty_target.getType()+itemProperty_target.getPosition());
            if(!result){
                updateFinish(1,true);
                Log.e(TAG, "startSingleAcion put_pcr_cover_to_pcr : error");
                //Toast.makeText(mContext, "startSingleAcion  put_pcr_cover_to_pcr : error",Toast.LENGTH_LONG).show();
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "putPcrCover : " + e.getMessage());
            return false;
        }
    }

    public boolean putTempPcrCover(int index,ArmFinishCallback finishCallback){
        try {
            itemProperty_source = getItemPropertyPCRCover(0);
            itemProperty_target = getItemPropertyPCRCover(index);
            if (itemProperty_target == null) {
                Log.e(TAG, "putTempPcrCover 找不到目标 pcr" + index);
                return false;
            }
            updateFinish(1, false);
            updateArmwork(1,ConstantData.ARM_TEMPPCRCOVER);
            saveModel(1);
            setArmFinishCallback(finishCallback);
            boolean result = RobotWorkManager.getInstance().startSingleAcion(itemProperty_source, itemProperty_target, "put_pcr_cover_from_temp_to_pcr");
            Log.e(TAG, "startSingleAcion put_pcr_cover_from_temp_to_pcr : target " + itemProperty_target.getType() + itemProperty_target.getPosition());
            if (!result) {
                updateFinish(1, true);
                Log.e(TAG, "startSingleAcion put_pcr_cover_from_temp_to_pcr : error");
                //Toast.makeText(mContext, "startSingleAcion  put_pcr_cover_to_pcr : error",Toast.LENGTH_LONG).show();
            }
            return result;
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "putTempPcrCover : " + e.getMessage());
            return false;
        }
    }

    public boolean closeRNADoor(int index,ArmFinishCallback finishCallback){
        itemProperty_source = null;
        itemProperty_target = getItemPropertyRNA(index);
        if(itemProperty_target==null){
            Log.e(TAG, "closeRNADoor 找不到目标 rna"+index);
            return false;
        }
        updateArmwork(1,ConstantData.ARM_CLOSERNA);
        saveModel(1);
        setArmFinishCallback(finishCallback);
        boolean result = RobotWorkManager.getInstance().startSingleAcion(itemProperty_source,itemProperty_target,"close_rna_pan");
        if(!result){
            Log.e(TAG, "startSingleAcion close_rna_pan: error");
            // Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
        }
        return result;
    }

    public boolean openRNADoor(int index,ArmFinishCallback finishCallback){
        itemProperty_source = null;
        itemProperty_target = getItemPropertyRNA(index);
        if(itemProperty_target==null){
            Log.e(TAG, "openRNADoor 找不到目标 rna"+index);
            return false;
        }
        updateArmwork(1,ConstantData.ARM_OPENRNA);
        saveModel(1);
        setArmFinishCallback(finishCallback);
        boolean result = RobotWorkManager.getInstance().startSingleAcion(itemProperty_source,itemProperty_target,"open_rna_pan");
        if(!result){
            Log.e(TAG, "startSingleAcion open_rna_pan : error");
            // Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
        }
        return result;
    }

    //复位专用
    public boolean config(ArmResetCallback resetCallback) {
        boolean result = false;
        try {
            updateArmwork(1,ConstantData.ARM_RESET);
            saveModel(1);
            setArmResetCallback(resetCallback);
            result = SerialPortManager_Robot.getInstance().robotArmConfig();
            if (result) {
                updateFinish(1, true);
                Log.i("ArmTaskWorker", "robotArmConfig : " + result);
            } else {
                Log.e("ArmTaskWorker", "robotArmConfig : " + result);
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "config : " + e.getMessage());
            return false;
        }
        return result;
    }
    public boolean clearFinish(){
        updateFinish(1, true);
        Log.e("ArmTaskWorker", "clearFinish :机械臂清除占用 ");
        return true;
    }
    public boolean armFinishCMD(){
        boolean result = false;
        try{
            result = SerialPortManager_Robot.getInstance().finishCMD();
            if(result){
                setFree();
            }
            Log.i("ArmTaskWorker", "arm1 finishCMD:"+result);
        }catch (Exception e){
            e.printStackTrace();
            Log.e("ArmTaskWorker", "armFinishCMD : " + e.getMessage());
            return false;
        }
        return result;
    }
    public boolean armCanuse(){
        //TODO fordebugtest
        if (true) {
            return true;
        }
        int index = 1;
        boolean result = true;
        PipeArmModel m = getArmModel(index);
        if(m==null){
            Log.i("ArmTaskWorker", "arm1 : null");
            return false;
        }
        if(!m.connect.get()){
            Log.i("ArmTaskWorker", "arm1 : disconnect");
            return false;
        }
        if(m.status.get()==null){
            Log.i("ArmTaskWorker","arm1 :status null");
            return false;
        }
        if (!m.status.get().equalsIgnoreCase(ConstantData.str_FREE)) {
            Log.i("ArmTaskWorker", "arm1 : not free is " + m.status.get());
            return false;
        }
        if(m.finish.get()==null || !m.finish.get()){
            Log.i("ArmTaskWorker", "arm1 : not finish");
            return false;
        }
        //TODO fordebugtest
        if (false) {
//        if(m.exist.get()==null || m.exist.get()){
            Log.i("ArmTaskWorker", "arm1 : exist");
            return false;
        }
        return result;
    }
    public boolean armSelfcheckCanuse(){
        //TODO fordebugtest
        if (true) {
            return true;
        }
        int index = 1;
        boolean result = true;
        PipeArmModel m = getArmModel(index);
        if(m==null){
            Log.i("ArmTaskWorker", "arm1 : null");
            return false;
        }
        if(!m.connect.get()){
            Log.i("ArmTaskWorker", "arm1 : disconnect");
            return false;
        }
        if(m.status.get()==null){
            Log.i("ArmTaskWorker","arm1 :status null");
            return false;
        }
        if (!m.status.get().equalsIgnoreCase(ConstantData.str_FREE)) {
            Log.i("ArmTaskWorker", "arm1 : not free is " + m.status.get());
            return false;
        }
        if(m.finish.get()==null || !m.finish.get()){
            Log.i("ArmTaskWorker", "arm1 : not finish");
            return false;
        }
        return result;
    }
    /**
     *
     * @param tray 盘位
     * @param temperatrue 0表示关闭
     * @return
     */
    public boolean setCooler(int tray,int temperatrue) {
        try {
            boolean result = SerialPortManager_Robot.getInstance().setCooler(tray, temperatrue);
            return result;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    /**
     *
     * @param tray
     * @param on_off
     * @return
     */
    public boolean setLed(int tray,int on_off) {
        try {
            boolean result = SerialPortManager_Robot.getInstance().setLed(tray, on_off);
            return result;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }


//    /**
//     * 指令正在执行
//     * @param robotManagerMessage
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(RobotManagerMessage robotManagerMessage) {
//
//        if (robotManagerMessage != null) {
//            //  String text = GsonUtil.toJson(robotManagerMessage, false);
//            //  callback(robotManagerMessage.isResult());
//        }
//    }
//    /**
//     * 指令正在执行
//     * @param robotRunningMessage
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(RNARunningMessage robotRunningMessage) {
//        Log.i("ROBOT", "ROBOT指令运行1" + robotRunningMessage.getID() + "getStatus" + robotRunningMessage.getStatus());
//        Log.i("ROBOT", "ROBOT指令运行2" + robotRunningMessage.getID() + "getNO" + robotRunningMessage.getNO());
//        Log.i("ROBOT", "ROBOT指令运行3" + robotRunningMessage.getID() + "getDegree" + robotRunningMessage.getDegree());
//
//        if(robotRunningMessage != null )
//        {
//            String text = GsonUtil.toJson(robotRunningMessage,false);
////            RobotRunningMessageText.append(text + "\n");
////            RobotRunningMessageText.scrollTo(0,0);
//        }
//    }

//    /*
//     * EventBus接收订阅的消息
//     * @param
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEvent(MessageEvent messageEvent) {
//        switch (messageEvent.getMsgType()) {
//            case MsgType.MSG_ROBOT_CONNECT_STATE:
//                Log.e("MSG_ROBOT_CONNECT_STATE", "is_ROBOT_connect" + SerialPortManager_Robot.getInstance().isIs_ROBOT_connect());
//                if (SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()) {
////                    robot_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
////                    robot_alarm_msg_tv.setText("");
//                    arm1Connected = true;
//                } else {
////                    robot_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
////                    robot_alarm_msg_tv.setText("E3-机械臂断连");
//                    arm1Connected = false;
//                }
//                break;
//        }
//    }
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(QRCodeMessageEvent qRCodeMessageEvent) {
//
//        String text = qRCodeMessageEvent.getQRCodeMessage();
//
//        qRCodeItems.clear();
//
//        String regex = "#P#|#D#"; // 按照 "#P#" 和 "#D#" 分割
//
//        // 使用正则表达式分割字符串
//        String[] parts = text.split(regex);
//
//        // 输出分割结果
//        int i = 1;
//        for (String part : parts) {
//            if (!part.isEmpty()) { // 过滤空字符串
//                // (438,140)0100614141999996901234567890
//                // 使用正则表达式提取括号内的数字和后续的字符串
//                String regex1 = "\\((\\d+),(\\d+)\\)(\\d+)";
//                Pattern pattern = Pattern.compile(regex1);
//                Matcher matcher = pattern.matcher(part);
//
//                if (matcher.find()) {
//                    String x = matcher.group(1); // 提取第一个数字 438
//                    String y = matcher.group(2); // 提取第二个数字 140
//                    String qrcode = matcher.group(3); // 提取后续的字符串 0100614141999996901234567890
//
//                    QRCodeItem qRCodeItem = new QRCodeItem();
//
//                    qRCodeItem.setIndex(i++);
//                    qRCodeItem.setX_coordinate(NumberUtils.StringtoInt(x));
//                    qRCodeItem.setY_coordinate(NumberUtils.StringtoInt(y));
//                    qRCodeItem.setQrcode(qrcode);
//                    qRCodeItems.add(qRCodeItem);
//
//                    Log.i(TAG,"Part 1: " + x);
//                    Log.i(TAG,"Part 2: " + y);
//                    Log.i(TAG,"Part 3: " + qrcode);
//                }
//            }
//        }
//
////        qRCodeAdapter.notifyDataSetChanged();
//
//
//    }
}


