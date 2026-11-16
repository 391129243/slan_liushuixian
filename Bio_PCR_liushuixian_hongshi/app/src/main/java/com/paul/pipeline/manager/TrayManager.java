package com.paul.pipeline.manager;

import android.content.Context;
import android.util.Log;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.WellInfo;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.data.ScanCodeHelper;
import com.paul.pipeline.model.SampleHoleModel;
import com.paul.pipeline.utils.DataTimeUtil;
import com.paul.pipeline.utils.MmkvUtils;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.utils.TimeUtil;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;

public class TrayManager {
    private static final String TAG = "TrayManager";
    private static TrayManager mInstance;
    public static TrayManager getInstance(){
        if (null == mInstance) {
            synchronized (TrayManager.class){
                if (null == mInstance){
                    mInstance = new TrayManager();
                }
            }
        }
        return  mInstance;
    }

    public void clearData(Context context){
        if(traylist!=null) {
            for (int i = 0; i < traylist.size(); ++i) {
                int index = traylist.get(i).index.get();
                traylist.get(i).initModel(index,context.getString(R.string.pipe_main_trayname) + index);
            }
        }
    }

    /**
     * 清除历史批次/项目/调度信息，同时保留各孔样本的条码和编号（serialNum/reagentCode）。
     * 与 clearData 的区别：不重置 holes，仅清除调度/运行状态字段。
     * 清除后通过 MMKV 持久化，重启后也保持清除结果。
     */
    public void clearDataKeepSamples(Context context){
        if(traylist!=null) {
            for (int i = 0; i < traylist.size(); ++i) {
                PipeTrayModel tray = traylist.get(i);
                int index = tray.index.get();
                String name = context.getString(R.string.pipe_main_trayname) + index;
                tray.clearKeepSamples(index, name);
                saveModel(index); // 持久化
            }
        }
    }

    public void init(Context context){
        ArrayList<PipeTrayModel> list = new ArrayList<>();
        list.add(initModel(context, 1));
        list.add(initModel(context, 2));
        list.add(initModel(context, 3));
        list.add(initModel(context, 4));
        list.add(initModel(context, 5));
        list.add(initModel(context, 6));
        list.add(initModel(context, 7));
        list.add(initModel(context, 8));
        setTraylist(list);
    }
    private PipeTrayModel initModel(Context context,int index){
        PipeTrayModel model = MmkvUtils.getObject(ConstantData.nameTray+index,PipeTrayModel.class);
        if(model==null) {
            model = newModel(context, index);
        }
        return model;
    }

    public void saveNewModel(PipeTrayModel model){
        if(model!=null&&traylist!=null) {
            for (int i = 0; i < traylist.size(); ++i) {
                PipeTrayModel tmodel = traylist.get(i);
                if (tmodel!=null&& model.index.get() == tmodel.index.get()) {
                    traylist.set(i, model);
                    MmkvUtils.saveObject(ConstantData.nameTray+model.index.get(), model);
                    break;
                }
            }
        }
    }
    public void saveModel(int index){
        if(traylist!=null) {
            for (int i = 0; i < traylist.size(); ++i) {
                PipeTrayModel model = traylist.get(i);
                if (model!=null && model.index.get()!=null && index == model.index.get()) {
                    MmkvUtils.saveObject(ConstantData.nameTray+index, model);
                }
            }
        }
    }
    ArrayList<PipeTrayModel> traylist = new ArrayList<>();

    public ArrayList<PipeTrayModel> getTraylist() {
        return traylist;
    }

    public void setTraylist(ArrayList<PipeTrayModel> traylist) {
        this.traylist = traylist;
        updatePriority();
    }

    private PipeTrayModel newModel(Context context, int index){
        PipeTrayModel model = new PipeTrayModel(index,context.getString(R.string.pipe_main_trayname) + index);
        return model;
    }

    public PipeTrayModel getPipeTrayModel(int index){
        if(traylist!=null) {
            for (int i = 0; i < traylist.size(); i++) {
                PipeTrayModel model = traylist.get(i);
                if(model!=null) {
                    if (model.index.get()!=null && model.index.get() == index) {
                        return model;
                    }
                }
            }
        }
        return null;
    }
    //按提交时间优先级排队,先提交排前面
    public void updatePriority(){
        if(traylist!=null) {
            Collections.sort(traylist, new Comparator<PipeTrayModel>() {
                @Override
                public int compare(PipeTrayModel tray1, PipeTrayModel tray2) {
                    long t1 = 0;
                    if(tray1!=null&&tray1.createTime.get()!=null){
                        t1 = tray1.createTime.get();
                    }
                    long t2= 0;
                    if(tray2!=null&&tray2.createTime.get()!=null){
                        t2 = tray2.createTime.get();
                    }
                    if(t1<t2){
                        return -1;
                    }else if(t1==t2){
                        return 0;
                    }else{
                        return 1;
                    }
                }
            });
        }
    }

    public void stopTray(int index){
        PipeTrayModel m = getPipeTrayModel(index);
        if(m!=null) {
            m.status.set(ConstantData.str_ERROR);
        }
    }
    public void continueTray(int index){
        PipeTrayModel m = getPipeTrayModel(index);
        if(m!=null) {
            m.status.set(ConstantData.str_RUN);
        }
    }
    public void errorDispatch(int index,int errordispatchindex,int target,int tagettype){
        PipeTrayModel m = getPipeTrayModel(index);
        if(m!=null) {
            m.status.set(ConstantData.str_ERROR);
            Log.e("ArmTaskWorker", "errorDispatch tray: " + index);
            if(errordispatchindex > 0) {
                m.errorDispatchindex.set(errordispatchindex);
                Log.e("ArmTaskWorker", "errorDispatchindex: " + errordispatchindex);
                if (m.dispatchlist != null) {
                    if (errordispatchindex <= m.dispatchlist.size()) {
                        for (int i = 0; i < m.dispatchlist.size(); i++) {
                            DispatchModel dm = m.dispatchlist.get(i);
                            if (dm.index.get()!=null && errordispatchindex == dm.index.get()) {
                                m.dispatchlist.get(i).status.set(ConstantData.STATUS_ERROR);
                                m.dispatchlist.get(i).devicestatus.set(ConstantData.STATUS_ERROR);
                                dm.target.set(target);
                                dm.targettype.set(tagettype);
                                Log.e("ArmTaskWorker", "errorDispatch status error i:"+i);
                                break;
                            }
                        }
                    }
                }else{
                    Log.e("ArmTaskWorker", "errorDispatch list错误 tray: " + index +" errordispatchindex:"+errordispatchindex);
                }
            }else{
                Log.e("ArmTaskWorker", "errorDispatch index错误 tray: " + index +" errordispatchindex:"+errordispatchindex);
            }
        }
    }
    public int getStatus(int index){
        PipeTrayModel m = getPipeTrayModel(index);
        if(m!=null) {
            String status = m.status.get();
            if (status.equalsIgnoreCase(ConstantData.str_FREE)) {
                return ConstantData.STATUS_FREE;
            } else if (status.equalsIgnoreCase(ConstantData.str_RUN)) {
                return ConstantData.STATUS_RUN;
            } else if (status.equalsIgnoreCase(ConstantData.str_FINISH)) {
                return ConstantData.STATUS_FINISH;
            } else if (status.equalsIgnoreCase(ConstantData.str_ERROR)) {
                return ConstantData.STATUS_ERROR;
            }
        }
        return ConstantData.STATUS_ERROR;
    }

    /**
     * 样本信息列表
     * @param index
     * @return
     */
    public ArrayList<SampleHoleModel> getSampleList(int index){
        PipeTrayModel model = getPipeTrayModel(index);
        if(model!=null) {
            return model.holes;
        }
        return null;
    }

    /**
     * 样本信息列表
     * @param index
     * @param list
     * @return
     */
    public boolean setSampleList(int index,ArrayList<SampleHoleModel> list){
        PipeTrayModel model = getPipeTrayModel(index);
        if(model!=null) {
            model.holes.clear();
            for(int i=0;i<list.size();i++){
                SampleHoleModel hole  = ScanCodeHelper.parseSampleHoleModel(list.get(i));
                list.set(i,hole);
            }
            model.holes.addAll(list);
            setWellinfo(index);
            return true;
        }else{
            return false;
        }
    }

    private void setWellinfo(int index){
        PipeTrayModel model = getPipeTrayModel(index);
        if(model!=null&&model.holes!=null) {
            ArrayList<WellInfo> wells = new ArrayList<>();
            for(int i=0;i<model.holes.size();i++) {
                int hole = model.holes.get(i).index.get();
                String sample= model.holes.get(i).serialNum.get();
                String reagentCode= model.holes.get(i).reagentCode.get();
                WellInfo holewellInfo = new WellInfo("A" + hole, sample, reagentCode);
                wells.add(holewellInfo);
            }
            model.wellInfolist.clear();
            model.wellInfolist.addAll(wells);
        }
    }

    public boolean getExist(int index){
        PipeTrayModel model = getPipeTrayModel(index);
        if(model!=null) {
            return model.exist.get();
        }
        return false;
    }
    public void updateExist(int index,boolean value){
        PipeTrayModel model = getPipeTrayModel(index);
        if(model!=null) {
                model.exist.set(value);
        }
    }
    public DispatchModel getDispatchModel(int tray,int dispatchindex){
        PipeTrayModel model = getPipeTrayModel(tray);
        if(model!=null) {
            if(model.dispatchlist!=null) {
                for(int i=0;i<model.dispatchlist.size();i++) {
                    DispatchModel dm = model.dispatchlist.get(i);
                    if(dispatchindex==dm.index.get()) {
                        return dm;
                    }
                }
            }
        }
        return null;
    }
    public DispatchModel getCurrentDispatch(int tray){
        PipeTrayModel model = getPipeTrayModel(tray);
        if(model!=null) {
            if (model.dispatchlist != null && model.dispatchindex.get()!=null) {
                for (int j = 0; j < model.dispatchlist.size(); j++) {
                    DispatchModel dm = model.dispatchlist.get(j);
                    if (model.dispatchindex.get() == dm.index.get()) {
                        return dm;
                    }
                }
            }
        }
        return null;
    }
    //托盘到位后更新调度信息
    public void updateDsiapatchNext(int tray,int dispatchindex,int targettype,int target){
        PipeTrayModel model = getPipeTrayModel(tray);
        if(model!=null) {
            if(model.dispatchlist!=null) {
                int nextindex = dispatchindex+1;
                ArrayList<DispatchModel> dmlist = model.dispatchlist;
                if(nextindex <= dmlist.size()) {
                    model.dispatchindex.set(nextindex);
                    Log.e("ArmTaskWorker", "托盘调度表:"+tray +" dispatchindex:"+dispatchindex +" next:"+nextindex);
                    if (dmlist != null) {
                        for (int j = 0; j < dmlist.size(); j++) {
                            DispatchModel dm = dmlist.get(j);
                            if (nextindex == dm.index.get() && targettype == dm.targettype.get()) {
                                Log.e("ArmTaskWorker", "托盘调度表:"+tray +" targettype:"+targettype +" target:"+target);
                                dm.target.set(target);
                                dm.status.set(ConstantData.STATUS_RUN);
                                if (targettype == ConstantData.DEVICE_PCR) {
                                    PCRManager.getInstance().setCurrentTemplate(target, dm, 1);
                                } else if (targettype == ConstantData.DEVICE_RNA) {
                                    RNAManager.getInstance().setCurrentTemplate(target, dm, 1);
                                }
                                model.dispatch.set(dm);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    public void updateDsiapatchStoptime(int index,int dispatchindex){
        DispatchModel model = getDispatchModel(index,dispatchindex);
        if(model!=null) {
            String time = DataTimeUtil.getSysFullTime();
            model.stoptime.set(time);
            saveModel(index);
            Log.e("ArmTaskWorker", "调度表:"+index +" dispatchindex:"+dispatchindex +" stoptime:"+time);
        }
    }
    public void updateDsiapatchStarttime(int index,int dispatchindex){
        DispatchModel model = getDispatchModel(index,dispatchindex);
        if(model!=null) {
            String time = DataTimeUtil.getSysFullTime();
            model.starttime.set(time);
            saveModel(index);
            Log.e("ArmTaskWorker", "调度表:"+index +" dispatchindex:"+dispatchindex +" starttime:"+time);
        }
    }
    public void updateDsiapatchDeviceStatus(int tray,int dispatchindex,int value){
        DispatchModel model = getDispatchModel(tray,dispatchindex);
        if(model!=null) {
            model.devicestatus.set(value);
            Log.e("ArmTaskWorker", "调度表:"+tray +" dispatchindex:"+dispatchindex +" devicestatus:"+value);
        }
    }

    public void updateDsiapatchStatus(int tray,int dispatchindex,int value){
        DispatchModel model = getDispatchModel(tray,dispatchindex);
        if(model!=null) {
            model.status.set(value);
            Log.e("ArmTaskWorker", "调度表 pan:"+tray +" dispatchindex:"+dispatchindex +" status:"+value);
        }
    }
    public void updateDsiapatchTarget(int index,int dispatchindex,int value){
        DispatchModel model = getDispatchModel(index,dispatchindex);
        if(model!=null) {
            model.target.set(value);
            saveModel(index);
            Log.e("ArmTaskWorker", "调度表:"+index +" dispatchindex:"+dispatchindex +" target:"+value);
        }
    }
    public void refreshConnectinfo(){
        SerialPortManager_Robot.RobotStatus robotStatus = SerialPortManager_Robot.getInstance().getRobotStatus();
        if(robotStatus!=null) {
            updateExist(1, robotStatus.getSenseByKey("PAN1"));
            updateExist(2, robotStatus.getSenseByKey("PAN2"));
            updateExist(3, robotStatus.getSenseByKey("PAN3"));
            updateExist(4, robotStatus.getSenseByKey("PAN4"));
            updateExist(5, robotStatus.getSenseByKey("PAN5"));
            updateExist(6, robotStatus.getSenseByKey("PAN6"));
            updateExist(7, robotStatus.getSenseByKey("PAN7"));
            updateExist(8, robotStatus.getSenseByKey("PAN8"));
        }else{
            updateExist(1, false);
            updateExist(2, false);
            updateExist(3, false);
            updateExist(4, false);
            updateExist(5, false);
            updateExist(6, false);
            updateExist(7, false);
            updateExist(8, false);
        }
    }
//    public String updateBatch(int tray,String templatenumber){
//        String batch = "";
//        String temp ="00";
//        batch = createBatchCode(templatenumber, tray);
//        return batch;
//    }
    //日期时间+检测+托盘
    public String createBatchCode(String num,String name,int tray){
        String tempno = "";
        if(num!=null) {
            if (num.length() < 1) {
                tempno = "00" + num;
            } else if (num.length() < 2) {
                tempno = "0" + num;
            } else {
                tempno = "" + num;
            }
        }
        String code = "PAN" + tray + "-"+ DataTimeUtil.getHFullTime()+ "-" + tempno + "-" + name;
        return code;
    }

    //ConstantData.STATUS_FINISH
    public void setCurrentDispatch(int index,int dispatchindex) {
        for (int i = 0; i < traylist.size(); i++) {
            PipeTrayModel model = traylist.get(i);
            if (model.index.get() == index) {
                ArrayList<DispatchModel> dispatchlist = model.dispatchlist;
                for (int j = 0; j < dispatchlist.size(); j++) {
                    DispatchModel dm = dispatchlist.get(j);
                    if (dm.index.get() == dispatchindex) {
                        model.dispatch.set(dm);
                        saveModel(index);
                        break;
                    }
                }
            }
        }
    }
    public void updateCurrentDispatchStatus(int index,int dispatchindex,int status) {
        for (int i = 0; i < traylist.size(); i++) {
            PipeTrayModel model = traylist.get(i);
            if (model.index.get() == index) {
                ArrayList<DispatchModel> dispatchlist = model.dispatchlist;
                for (int j = 0; j < dispatchlist.size(); j++) {
                    DispatchModel dm = dispatchlist.get(j);
                    if (dm.index.get() == dispatchindex) {
                            dm.status.set(status);
                            model.dispatch.set(dm);
                            break;
                    }
                }
            }
        }
    }
}
