package com.gidi.bio_pcr.WintersUtils.AsyncTask;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.WintersUtils.NumUtils;
import com.gidi.bio_pcr.WintersUtils.entity.ActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.ActionMcuAckMessage;
import com.gidi.bio_pcr.WintersUtils.entity.MoveActionCmd;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.StepIndexMessage;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.queue.HexDebugEntityActionListQueue;
import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.connect.ConnectManager;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.google.gson.Gson;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.service.SLANService;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class RNAAndPCRAsyncTask extends AsyncTask<List<HexDebugEntity>, HexDebugEntity, HexDebugEntity> {

    private final static String TAG = "RNAAndPCRAsyncTask";

    private static RNAAndPCRAsyncTask mInstance;

    private Context mContext;

    private boolean mIsrunning = false;

    private int sleep = 200;

    private int actionTime_RNA = 60;

    private int actionTime_Heat = 60;

    private int actionTime_PCR = 7200;

    private String currentAction = "";

    private int index = 0;

    public static String current_template = "";

    public static int current_template_phase = 0;

    public static int current_template_phase_step = 0;

    HexDebugEntity currentHexDebugEntity = null;

    HexDebugEntity currentHexDebugEntity_backup = new HexDebugEntity();

    private boolean Isinteruppt = false;

    public static int retryCount_Value = 3;

    private int retryCount = 0;

    public boolean isIsinteruppt() {
        return Isinteruppt;
    }

    public void setIsinteruppt(boolean isinteruppt) {
        Isinteruppt = isinteruppt;
    }

    public static HexDebugEntityActionListQueue actionListQueue =  new HexDebugEntityActionListQueue();

    public static HexDebugEntityActionListQueue actionListQueue_showinfo =  new HexDebugEntityActionListQueue();

    private Timer timer;


    private int stepIndex;

    private int stepTotal;

    private Object lock = new Object();

    public boolean ismIsrunning() {
        return mIsrunning;
    }

    public void setmIsrunning(boolean mIsrunning) {
        this.mIsrunning = mIsrunning;
    }

    public static RNAAndPCRAsyncTask getInstance(){
        if (null == mInstance) {
            synchronized (RNAAndPCRAsyncTask.class){
                if (null == mInstance){
                    mInstance = new RNAAndPCRAsyncTask();
                }
            }
        }
        return  mInstance;
    }

    public static void clearInstance(){
        mInstance = null;
    }

    public Context getmContext() {
        return mContext;
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    public void reset(){

        mIsrunning = false;

        currentAction = "";

        Isinteruppt = false;

        actionListQueue.clear();

        actionListQueue_showinfo.clear();

        stopTimer();

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(!EventBus.getDefault().isRegistered(this)){//加上判断
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected HexDebugEntity doInBackground(List<HexDebugEntity>... HexDebugEntitys) {
        ////清空历史队列
        Log.i("RNA", "线程开始运行" + HexDebugEntitys[0].size());
        long startTime = System.currentTimeMillis();
        if(HexDebugEntitys[0] == null || HexDebugEntitys[0].isEmpty()){
            //反馈一个HexDebugEntity，代表指令有问题
            return new HexDebugEntity(false, "null", null, "null",0, null);
        }

        retryCount_Value = 3;

        if(actionListQueue != null){
            actionListQueue.clear();
            actionListQueue_showinfo.clear();
        }else{
            actionListQueue = new HexDebugEntityActionListQueue();
            actionListQueue_showinfo = new HexDebugEntityActionListQueue();
        }

        List<HexDebugEntity> hexDebugEntityList = HexDebugEntitys[0];

        List<HexDebugEntity> moveActionCmdList_temp = new ArrayList<HexDebugEntity>();


        int loopstartIndex = 0;
        int loopendIndex = 0;
        boolean loopstartFlag = false;
        int loopCount = 0;



        for (int i = 0; i < hexDebugEntityList.size(); i++)
        {

            if (hexDebugEntityList.get(i).getName().equals("loopstart"))
            {
                loopstartIndex = i;
                loopstartFlag = true;
                loopCount = hexDebugEntityList.get(i).getLoop();
                continue;
            }

            if (hexDebugEntityList.get(i).getName().equals("loopend") && loopstartFlag)
            {
                loopendIndex = i;

                int b = 0;

                while (loopCount-- > 0)
                {
                    b++;

                    for (int k = loopstartIndex+1; k <= loopendIndex -1; k++)
                    {
                        HexDebugEntity hexDebugEntity = hexDebugEntityList.get(k);

                        Gson gson = new Gson();

                        String jsonString = gson.toJson(hexDebugEntity);

                        HexDebugEntity hexDebugEntityNew = gson.fromJson(jsonString,HexDebugEntity.class);

                        String text = hexDebugEntityNew.getName() + ":" + (k + 1) + "" + "b" + (loopCount+1);

                        hexDebugEntityNew.setNO(text);

                        moveActionCmdList_temp.add(hexDebugEntityNew);

                    }
                }
                loopstartIndex = 0;
                loopendIndex = 0;
                loopstartFlag = false;
                loopCount = 0;
                continue;
            }
            else if (hexDebugEntityList.get(i).getName().equals("loopend") && !loopstartFlag)
            {
                loopstartIndex = 0;
                loopendIndex = 0;
                loopstartFlag = false;
                loopCount = 0;
                continue;
            }
            else if (loopstartFlag)
            {
                continue;
            }

            HexDebugEntity hexDebugEntity = hexDebugEntityList.get(i);

            Gson gson = new Gson();

            String jsonString = gson.toJson(hexDebugEntity);

            HexDebugEntity hexDebugEntityNew = gson.fromJson(jsonString,HexDebugEntity.class);

            String text = hexDebugEntityNew.getName() + ":" + (i + 1) + "" + "b" + 1;

            hexDebugEntityNew.setNO(text);

            moveActionCmdList_temp.add(hexDebugEntityNew);

        }

        reset();
        ////增加新的任务
        mIsrunning = true;

        index = 0;

        stepTotal = moveActionCmdList_temp.size();
        /////构造心跳的动作队列
        String[] templateName = current_template.split("-");

        for(int k = current_template_phase_step ;k < moveActionCmdList_temp.size();k++){
            String stepIndexString = String.format("%04d", k+1);
            HexDebugEntity temp = moveActionCmdList_temp.get(k);
            temp.setRetryCount(retryCount_Value);
            temp.setNO(templateName[0] + "|" + String.format("%02d", current_template_phase) + "|" + stepIndexString);
            actionListQueue.enQueue(temp);//每个文件中的操作，例如"油推定量
            actionListQueue_showinfo.enQueue(temp);
        }
        //////////用于断点前的温度指令查找及恢复
        for(int k = current_template_phase_step - 1 ;k >=0 ;k--){
            String stepIndexString = String.format("%04d", k+1);
            HexDebugEntity temp = moveActionCmdList_temp.get(k);
            temp.setRetryCount(retryCount_Value);
            temp.setNO(templateName[0] + "|" + String.format("%02d", current_template_phase) + "|" + stepIndexString);
            if(temp.getName().contains("heat")) {
                actionListQueue.enQueueHead(temp);//每个文件中的操作，例如"油推定量
                actionListQueue_showinfo.enQueueHead(temp);
                break;
            }
        }

        ////开始执行任务

        Log.i(TAG, "Robot_get running 2");

        while(!Isinteruppt){

            if (actionListQueue.QueueEmpty()) {
                currentHexDebugEntity = null;
                break;
            }else{

            }

            stopTimer();

            currentHexDebugEntity = actionListQueue.deQueue();

            retryCount = currentHexDebugEntity.getRetryCount();

//            Gson gson = new Gson();

//            String tempText = gson.toJson(currentHexDebugEntity);

//            currentHexDebugEntity_backup = gson.fromJson(tempText,HexDebugEntity.class);



            currentHexDebugEntity_backup.setName(currentHexDebugEntity.getName());
            currentHexDebugEntity_backup.setHexActionList(currentHexDebugEntity.getHexActionList());
            currentHexDebugEntity_backup.setLoop(currentHexDebugEntity.getLoop());
            currentHexDebugEntity_backup.setNO(currentHexDebugEntity.getNO());
            currentHexDebugEntity_backup.setRetryCount(currentHexDebugEntity.getRetryCount());

            /////////增加PCR预热开启和预热关闭功能

//            if(currentHexDebugEntity.getName().toLowerCase().equals("startpcrpreheat")){
//                int temperature = NumberUtils.StringtoInt(currentHexDebugEntity.getHexActionList().get(0).getHex());
//                if(temperature == 0){
//                    continue;
//                }
//                ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(FileUtils.COMMON_HEX_PATH);
//                if(mConfigProjects != null && mConfigProjects.size() >= 0){
//                    for (Project project:mConfigProjects) {
//                        if(project.getName().equals("heat")){
////                            if(PcrApplication.is_SLAN_PCR_connect) {
//                            //修改：20240826修改
//                          //  SLANService.INSTANCE.startPreHeat(PcrApplication.devices.get(0).getSerialNo(), "heat", (byte) 0x01, temperature);
////                            }
//                        }
//                        break;
//                    }
//                }
//                continue;
//            }else if(currentHexDebugEntity.getName().toLowerCase().equals("stoppcrpreheat")){
//                ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(FileUtils.COMMON_HEX_PATH);
//                if(mConfigProjects != null && mConfigProjects.size() >= 0){
//                    for (Project project:mConfigProjects) {
//                        if(project.getName().equals("heat")){
////                            if(PcrApplication.is_SLAN_PCR_connect) {
//                            //修改：20240826修改
//                         //   SLANService.INSTANCE.stopPreHeat(PcrApplication.devices.get(0).getSerialNo(), (byte) 0x01);
////                            }
//                        }
//                        break;
//                    }
//                }
//                continue;
//            }

            //////////默认getLoop都为1
            for(int i = 0; i < currentHexDebugEntity.getLoop();i++){

//                currentHexDebugEntity_backup.setLoop(currentHexDebugEntity_backup.getLoop()-i);   /////////改了可能有问题，待定

                currentHexDebugEntity_backup.setLoop(currentHexDebugEntity.getLoop()-i);///////////////

                if(Isinteruppt){

                    return currentHexDebugEntity;
                }

                //index++;

                index = stepTotal - actionListQueue.QueueLength();

                if(currentHexDebugEntity.getName().equals("heat")) {

                    MoveActionCmd moveActionCmd = new MoveActionCmd();
                    Log.i(TAG, "Robot_get running heat 3"  +  moveActionCmd.getCmd());
                    moveActionCmd.setID(index);
                    moveActionCmd.setCmd(currentHexDebugEntity.getName());
                    //moveActionCmd.setNO(/*current_template + */"" + currentHexDebugEntity.getNO() + "" + "s"  + (currentHexDebugEntity.getLoop()-i) + "");
                    //currentHexDebugEntity.setName(current_template + "" + currentHexDebugEntity.getName() + "" + "s" + (currentHexDebugEntity.getLoop()-i)  + "");

                    String stringDate = DateUtil.getStringDate();

                    moveActionCmd.setNO(currentHexDebugEntity.getNO() + "|" + stringDate);

//                    Log.i("RNA", "下发指令" +current_template + "" + currentHexDebugEntity.getNO() + "" + "s" + (currentHexDebugEntity.getLoop()-i) + "");
                    List<String> hexList = new ArrayList<String>();
                    for(int j = 0 ; j < currentHexDebugEntity.getHexActionList().size();j++){

                        hexList.add(currentHexDebugEntity.getHexActionList().get(j).getHex());

                    }
                    moveActionCmd.setHex(hexList);
                    ConnectManager.getInstance().sendSerialCmdMsg_NeedAck(moveActionCmd, true);
                    Log.i("AAATEST","currentHexDebugEntity.getName()" + currentHexDebugEntity.getName() + currentAction);
                    currentAction = moveActionCmd.getNO();
                    startTimer(actionTime_Heat);
                } else if(currentHexDebugEntity.getName().contains("move")){
                    //其他

                    MoveActionCmd moveActionCmd = new MoveActionCmd();
                    moveActionCmd.setID(index);
                    moveActionCmd.setCmd(currentHexDebugEntity.getName());
                    String stringDate = DateUtil.getStringDate();
                    moveActionCmd.setNO(currentHexDebugEntity.getNO() + "|" + stringDate);
                    List<String> hexList = new ArrayList<String>();
                    for(int j = 0 ; j < currentHexDebugEntity.getHexActionList().size();j++){

                        hexList.add(currentHexDebugEntity.getHexActionList().get(j).getHex());

                    }
                    moveActionCmd.setHex(hexList);
                    Log.i(TAG, "Robot_get running move 3"  +  moveActionCmd.getCmd());
                    ConnectManager.getInstance().sendSerialCmdMsg_NeedAck(moveActionCmd, true);
                    Log.i("AAATEST","currentHexDebugEntity.getName()" + currentHexDebugEntity.getName() + currentAction);
                    currentAction = moveActionCmd.getNO();
                    startTimer(actionTime_RNA);
                }else{
                    //其他
                    Log.i("AAA","currentHexDebugEntity.getName()" + currentHexDebugEntity.getName());
                    MoveActionCmd moveActionCmd = new MoveActionCmd();
                    moveActionCmd.setID(index);
                    moveActionCmd.setCmd(currentHexDebugEntity.getName());
                    //moveActionCmd.setNO(/*current_template + */"" + currentHexDebugEntity.getNO() + "" + "s"  + (currentHexDebugEntity.getLoop()-i) + "");


                    String stringDate = DateUtil.getStringDate();

                    moveActionCmd.setNO(currentHexDebugEntity.getNO() + "|" + stringDate);

                    //currentHexDebugEntity.setName(current_template + "" + currentHexDebugEntity.getName() + "" + "s" + (currentHexDebugEntity.getLoop()-i)  + "");
//                    Log.i("RNA", "下发指令" +current_template + "" + currentHexDebugEntity.getNO() + "" + "s" + (currentHexDebugEntity.getLoop()-i) + "");
                    List<String> hexList = new ArrayList<String>();
                    for(int j = 0 ; j < currentHexDebugEntity.getHexActionList().size();j++){

                        hexList.add(currentHexDebugEntity.getHexActionList().get(j).getHex());

                    }

                    moveActionCmd.setHex(hexList);
                    Log.i(TAG, "Robot_get running others 3 " +  moveActionCmd.getCmd());
                    ConnectManager.getInstance().sendSerialCmdMsg_NeedAck(moveActionCmd, true);
                    Log.i("AAATEST","currentHexDebugEntity.getName()" + currentHexDebugEntity.getName() + currentAction);
                    currentAction = moveActionCmd.getNO();
                    startTimer(actionTime_RNA);
                }
                publishProgress(currentHexDebugEntity);
                //////此处优化可用event等待之类类似PCR检测
                while(!currentAction.equals("") && !Isinteruppt){
                    synchronized(lock){
                        try {
                            lock.wait(sleep);
                        } catch (InterruptedException e) {
                            return currentHexDebugEntity;
                        }
                    }
                }

            }

        }
        stopTimer();
        long endTime = System.currentTimeMillis();
        long interval = (endTime - startTime)/1000;
        long last_duration = PreferenceUtil.getInstance(getmContext()).getLongValue(SharedConstant.RNA_EXTRACTION_DURATION,(long)0);
        PreferenceUtil.getInstance(getmContext()).setValueByName(SharedConstant.RNA_EXTRACTION_DURATION,last_duration+interval);
        return currentHexDebugEntity;


    }

    @Override
    protected void onProgressUpdate(HexDebugEntity... values) {
        super.onProgressUpdate(values);
        EventBus.getDefault().post(values[0]);
        EventBus.getDefault().post(new StepIndexMessage(stepIndex,stepTotal));
    }

    @Override
    protected void onPostExecute(HexDebugEntity s) {
        super.onPostExecute(s);
        reset();
        if(EventBus.getDefault().isRegistered(this)){//加上判断
            EventBus.getDefault().unregister(this);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(s == null){
                    EventBus.getDefault().post(new ProcedureFinishEvent("finish",current_template));
                }else{
                    //增加中断时的关闭加热块指令
                    EventBus.getDefault().post(new ProcedureFinishEvent("finish error",current_template));
                    /////发送关闭加温 ,在现有正在执行的指令上序号等不变，改成关闭温度 heat指令
                    MoveActionCmd moveActionCmd = new MoveActionCmd();
                    moveActionCmd.setID(index);
                    moveActionCmd.setCmd("heat");
                    //moveActionCmd.setNO(/*current_template + */"" + currentHexDebugEntity.getNO() + "" + "s"  + (currentHexDebugEntity.getLoop()-i) + "");
                    //currentHexDebugEntity.setName(current_template + "" + currentHexDebugEntity.getName() + "" + "s" + (currentHexDebugEntity.getLoop()-i)  + "");
                    String stringDate = DateUtil.getStringDate();
                    if(currentHexDebugEntity != null) {
                        moveActionCmd.setNO(currentHexDebugEntity.getNO() + "|" + stringDate);
                    }else{
                        moveActionCmd.setNO("null" + "|" + stringDate);
                    }
//                    Log.i("RNA", "下发指令" +current_template + "" + currentHexDebugEntity.getNO() + "" + "s" + (currentHexDebugEntity.getLoop()-i) + "");
                    List<String> hexList = new ArrayList<String>();
                    hexList.add("0");
                    moveActionCmd.setHex(hexList);
                    ConnectManager.getInstance().sendSerialCmdMsg_NoAck(moveActionCmd);
                }
            }
        }).start();

//        ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(FileUtils.COMMON_HEX_PATH);
//        if(mConfigProjects != null && mConfigProjects.size() >= 0){
//            for (Project project:mConfigProjects) {
//                if(project.getName().equals("heat")){
//                    SLANService.INSTANCE.stopPreHeat(PcrApplication.devices.get(0).getSerialNo(), (byte)0x01);
//                }
//                break;
//            }
//        }
//        SLANService.INSTANCE.stopExperiment(PcrApplication.devices.get(0).getSerialNo(), (byte) 0x01);
        mInstance = null;
    }


    private void startTimer(int timeOut) {
        if(timer !=null){
            timer.cancel();
            timer.purge();
            timer = null;
        }else{
            //timer = new Timer();
        }
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    /*Toast.makeText(mContext,"ActionDoneMessage  time out:" + currentAction,Toast.LENGTH_LONG).show();
                    Log.i(TAG, "ActionDoneMessage  time out:" + currentAction);*/
                    synchronized(lock){
                        try {

                            if(--retryCount <= 0 ){
                                Isinteruppt = true;
                                currentAction = "";
                                Log.i("RNA_retry_over", "下发指令" +currentHexDebugEntity_backup + "" + currentHexDebugEntity_backup.getNO() + "" + "s" + (currentHexDebugEntity_backup.getRetryCount()));
                                lock.notify();
                                stopTimer();
                            }else{
                                currentHexDebugEntity_backup.setRetryCount(currentHexDebugEntity_backup.getRetryCount()-1);
                                actionListQueue.enQueueHead(currentHexDebugEntity_backup);
                                currentAction = "";
                                Log.i("RNA_retry", "下发指令"  + "" + currentHexDebugEntity_backup.getNO() + ":" + "s" + (currentHexDebugEntity_backup.getRetryCount()));
                                lock.notify();
                                stopTimer();
                            }

                        } catch (Exception e) {
                            //ignore
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(task, timeOut * 1000);
    }

    private void stopTimer() {
        if(timer!= null)
        {
            timer.cancel();
            timer.purge();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {

        Gson gson = new Gson();

        String text = gson.toJson(sendCmdTimeoutEvent.getMsgPacket());

        Toast.makeText(mContext,"SendCmdTimeoutEvent  :  " + text,Toast.LENGTH_LONG).show();

        Log.i(TAG, "SendCmdTimeoutEvent  :  " + text);

        synchronized(lock) {
            Isinteruppt = true;
            lock.notify();
            currentAction = "";
        }

    }

    /////监听串口消息
    // do not remove this method. It is required for EventBus to answer ConnectedEvent.
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ActionMcuAckMessage actionMcuAckMessage) {

        Gson gson = new Gson();
        String text = gson.toJson(actionMcuAckMessage);
        //Toast.makeText(mContext,"ActionMcuAckMessage:" + text,Toast.LENGTH_LONG).show();
        if(actionMcuAckMessage.getAlarm().equals("")){

        }else{
            Toast.makeText(mContext,"指令不能执行，请检查，错误原因:" + actionMcuAckMessage.getAlarm() ,Toast.LENGTH_LONG).show();
            Log.i(TAG, "指令不能执行，请检查，错误原因:" + actionMcuAckMessage.getAlarm());
            Isinteruppt = true;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ActionDoneMessage actionDoneMessage) {

        Gson gson = new Gson();

        String text = gson.toJson(actionDoneMessage);

//        Toast.makeText(mContext, "ActionDoneMessage done:" + text, Toast.LENGTH_SHORT).show();

        Log.i(TAG, "ActionDoneMessage done:" + text);

        //if (actionDoneMessage.getNO().equals(currentAction) && actionDoneMessage.getID() == index) {
        if(null != actionDoneMessage) {
            if (!actionDoneMessage.getStatus().contains("error")){
                synchronized (lock) {
                    try {
                        currentAction = "";
                        lock.notify();
                        stopTimer();
                    } catch (Exception e) {
                        Log.i(TAG, "error:" + e.getMessage());
                    }
                }
            }else{
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        synchronized(lock){
                            try {
                                Thread.sleep(3000);
                                if(--retryCount <= 0 ){
                                    Isinteruppt = true;
                                    currentAction = "";
                                    Log.i("RNA_retry_over", "下发指令" +currentHexDebugEntity_backup + "" + currentHexDebugEntity_backup.getNO() + "" + "s" + (currentHexDebugEntity_backup.getRetryCount()));
                                    lock.notify();
                                    stopTimer();
                                }else{
                                    currentHexDebugEntity_backup.setRetryCount(currentHexDebugEntity_backup.getRetryCount()-1);
                                    actionListQueue.enQueueHead(currentHexDebugEntity_backup);
                                    currentAction = "";
                                    Log.i("RNA_retry", "下发指令"  + "" + currentHexDebugEntity_backup.getNO() + ":" + "s" + (currentHexDebugEntity_backup.getRetryCount()));
                                    lock.notify();
                                    stopTimer();
                                }

                            } catch (Exception e) {
                                //ignore
                            }
                        }
                    }
                }).start();
            }
        }
    }

}
