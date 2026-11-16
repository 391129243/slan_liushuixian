package com.winters.boweilibrary.BoWei.task;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.hongshi.slansdk.model.experiment.Program;
import com.winters.boweilibrary.BoWei.ExperimentData.Temperature;
import com.winters.boweilibrary.BoWei.serialcom.BoWeiConnectManager;
import com.winters.boweilibrary.BoWei.event.BoWeiPCRProcedureInfoEvent;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;
import com.winters.boweilibrary.BoWei.queue.BoWeiPCRStepEntityActionListQueue;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeData;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeTempretureData;
import com.winters.boweilibrary.BoWei.event.BoWeiProcedureFinishEvent;
import com.winters.boweilibrary.BoWei.event.BoWeiRecvDataInfo;
import com.winters.boweilibrary.BoWei.ExperimentData.Dot;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.status.BoWeiPCRStepStatus;
import com.winters.boweilibrary.BoWei.utils.BoWeiTaiKeHexUtils;
import com.gidi.bio_pcr.WintersUtils.NumUtils;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.google.gson.Gson;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Step;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BoWeiPCRAsyncTask extends AsyncTask<Project, BoWeiPCRStepEntity, BoWeiPCRStepEntity> {

    private final static String TAG = "RNAAndPCRAsyncTask";

    private static BoWeiPCRAsyncTask mInstance;

    private Context mContext;

    private boolean isRunning = false;

    private int timeOfError = 10;

    private Object conditionLock = new Object();

    private int index = 0;

    public static String current_template = "";


    private BoWeiPCRStepEntity currentBoWeiPCRStepEntity;

    private BoWeiPCRStepEntity currentBoWeiPCRStepEntity_backup;

    private boolean isInteruppt = false;

    public  static int retryCount_Value = 9;

    private int retryCount = 0;

    private Project CurrentProject;

    public static BoWeiPCRStepEntityActionListQueue actionListQueue =  new BoWeiPCRStepEntityActionListQueue();

    public static BoWeiPCRStepEntityActionListQueue actionListQueue_showinfo =  new BoWeiPCRStepEntityActionListQueue();

    private Timer timer;

    private long startTime = 0;
    private long runningTime = 0;
    private long remainTime = 0;//整个程序段剩余时间
    private long totalTime = 0;
    private int stepTotal;

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    private int countTemperatureReach = 0;

    private boolean isPCRTemperatureWaiting = false;

    private boolean queryPCRTemperatureFlag =  true;

    private boolean isNeedQueryTemperature = true;

    private String stepDountDownTime;

    private List<ConfigSLANSection> mSLANSections = new ArrayList<ConfigSLANSection>();

    private List<BoWeiPCRStepEntity> BoWeiPCRStepEntityList = new ArrayList<>();

    private Timer timerAMP;
    private TimerTask taskAMP;
    private TimerTask createTask(){
        taskAMP = new TimerTask() {
            @Override
            public void run() {
                totalStepDountDownTime = totalStepDountDownTime - 0.2;
                if(totalStepDountDownTime <= 0)
                {
                    stepDountDownTime = "00:00";
                    timerAMP.cancel();
                    taskAMP.cancel();
                    timerAMP = null;
                    taskAMP = null;
                    totalStepDountDownTime = 0;
                    TempretureQueryThread.getInstance().stopQuery();
//                    lock.lock(); // 获取锁
//                    try {
//                        condition.signal();
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }finally {
//                        lock.unlock(); // 释放锁
//                    }
                    synchronized(lock) {
                        lock.notify();
                    }
                }
                else
                {
                    int actualTime = (int) (totalStepDountDownTime - 3);
                    if(actualTime<=0 ){
                        actualTime = 0;
                    }
                    stepDountDownTime = DateFormatUtil.secondToMS(actualTime);
                    //上报的状态
                    long curTime = System.currentTimeMillis();
                    if(curTime > startTime){
                        runningTime = curTime - startTime;
                    }
                    if(totalTime > runningTime){
                        remainTime = totalTime - runningTime;
                    }
                    EventBus.getDefault().post(new BoWeiPCRProcedureInfoEvent(runningTime,remainTime,
                            stepDountDownTime,currentBoWeiPCRStepEntity));
                }
            }
        };
        return taskAMP;
    }





//    private Lock lock = new ReentrantLock();
//    private Condition condition = lock.newCondition();

    private Object lock = new Object();


    public boolean isInteruppt() {
        return isInteruppt;
    }

    public void setInteruppt(boolean interuppt) {
        synchronized(lock) {
            lock.notify();
        }
        isInteruppt = interuppt;
    }



    private static double totalStepDountDownTime;

    public static BoWeiPCRAsyncTask getInstance(){
        if (null == mInstance) {
            synchronized (BoWeiPCRAsyncTask.class){
                if (null == mInstance){
                    mInstance = new BoWeiPCRAsyncTask();
                }
            }
        }
        return  mInstance;
    }

    public Context getmContext() {
        return mContext;
    }

    public void setmContext(Context mContext) {
        this.mContext = mContext;
    }

    public void stopMachine(){
        //reset();
        resetMachine();
        TempretureQueryThread.getInstance().killQuery();

    }

    public void resetMachine(){
        BoWeiPCRStepEntity pCRStepEntityBoWei = BoWeiTaiKeHexUtils.Create_StopPeltier_Message();
        BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pCRStepEntityBoWei);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("statusled_bright", "状态灯亮", "5E 0F 00 06 01 74", new ArrayList<Double>(), 1, 1, 0, 0,0,0,0);

        BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);

    }

    public void reset(){

        isRunning = false;

        isInteruppt = false;

        actionListQueue.clear();

        actionListQueue_showinfo.clear();

        stopTimer();

        TempretureQueryThread.getInstance().killQuery();

    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if(!EventBus.getDefault().isRegistered(this)){//加上判断
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected BoWeiPCRStepEntity doInBackground(Project... project) {

        BoWeiConnectManager.getInstance().setPCRRunning(true);

        reset();

        TempretureQueryThread.getInstance().setUPThread();


        CurrentProject = project[0];
        totalTime = getProjectTotalTime(CurrentProject)*1000;//单位是秒
        //设置试验开始运行时间
        startTime = System.currentTimeMillis();
        Experiment.getInstance().setStartTime(DateFormatUtil.getSysFullTime());
        if(project != null){
            createPCRStepEntityList(project[0]);
        }else{
            //反馈一个name等内容为null PCRStepEntity，代表指令有问题
            return new BoWeiPCRStepEntity(null, null, null, null, 0, 0, 0, 0, 0, 0, 0);
        }

        ////清空历史队列
        Log.i("RNA", "线程开始运行" + BoWeiPCRStepEntityList.size());

        if(BoWeiPCRStepEntityList == null || BoWeiPCRStepEntityList.isEmpty()){
            //反馈一个name等内容为null PCRStepEntity，代表指令有问题
            return new BoWeiPCRStepEntity(null, null, null, null, 0, 0, 0, 0, 0, 0, 0);
        }

        if(actionListQueue != null){
            actionListQueue.clear();
            actionListQueue_showinfo.clear();
        }else{
            actionListQueue = new BoWeiPCRStepEntityActionListQueue();
            actionListQueue_showinfo = new BoWeiPCRStepEntityActionListQueue();
        }

        List<BoWeiPCRStepEntity> pCRStepEntityListBoWei = BoWeiPCRStepEntityList;

        ////增加新的任务
        isRunning = true;

        index = 0;

        for(int k = 0; k < pCRStepEntityListBoWei.size(); k++){

            BoWeiPCRStepEntity temp = pCRStepEntityListBoWei.get(k);
            temp.setReTryCount(retryCount_Value);
            actionListQueue.enQueue(temp);
            actionListQueue_showinfo.enQueue(temp);
        }

        stepTotal = pCRStepEntityListBoWei.size();

//        TempretureQueryThread.getInstance().startQuery();

        //设置开始时间

        ////开始执行任务
        while(!isInteruppt){

            if (actionListQueue.QueueEmpty()) {
                currentBoWeiPCRStepEntity = null;
                break;
            }else{

            }
            stopTimer();
            currentBoWeiPCRStepEntity = actionListQueue.deQueue();

            Gson gson = new Gson();

            String tempText = gson.toJson(currentBoWeiPCRStepEntity);

            Log.i("BoWeiPCRAsyncTask","步骤详细信息：" + tempText);

            currentBoWeiPCRStepEntity_backup = gson.fromJson(tempText, BoWeiPCRStepEntity.class);

            if (currentBoWeiPCRStepEntity.getReTryCount() < 0 )
            {
                return currentBoWeiPCRStepEntity;//////某条指令执行有问题，返回该指令表示执行中断，返回null则是执行完毕
            }

            if (currentBoWeiPCRStepEntity.getName().equals("pcr_temperature_set")) {
                countTemperatureReach = 0;
                isPCRTemperatureWaiting = false;
                isNeedQueryTemperature = true;
            }
            else
            {
                isNeedQueryTemperature = false;
            }

            boolean result = BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(currentBoWeiPCRStepEntity);

            if(!result){
                //////该指令发送有问题尝试Retry执行
                currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount()-1);
                actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);//每个文件中的操作，例如"油推定量
                actionListQueue_showinfo.enQueueHead(currentBoWeiPCRStepEntity_backup);
                publishProgress(currentBoWeiPCRStepEntity);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }

            }else {
                ////////该指令发送没问题，启动自锁等待回复、启动超时回复计时器
                //startTimer((int) (currentBoWeiPCRStepEntity.getTime() + 1 + timeOfError));
                startTimer((int) (timeOfError));
                publishProgress(currentBoWeiPCRStepEntity);
//                lock.lock();
//                try {
//                    condition.
//                    condition.await();
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }finally {
//                    lock.unlock();
//                }
                synchronized(lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        stopTimer();
        return currentBoWeiPCRStepEntity;


    }

    @Override
    protected void onProgressUpdate(BoWeiPCRStepEntity... values) {
        super.onProgressUpdate(values);
        EventBus.getDefault().post(new BoWeiPCRProcedureInfoEvent(runningTime,remainTime,"",values[0]));
    }

    @Override
    protected void onPostExecute(BoWeiPCRStepEntity s) {
        super.onPostExecute(s);
        reset();
        resetMachine();
        if(EventBus.getDefault().isRegistered(this)){//加上判断
            EventBus.getDefault().unregister(this);
        }
        startTime = 0;
        runningTime = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Experiment.getInstance().setProject(CurrentProject);
                    //String strJson_Config = GsonUtil.toJson(Experiment.getInstance(),true);
                    String strJson_Config = GsonUtil.toJson(Experiment.getInstance(),false);
                    String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/BoWeiResult/";
                    File dir = new File(FILE_PATH);
                    if(!dir.exists()){
                        dir.mkdirs();
                    }
                    String name = DateFormatUtil.getFileName();
                    String fileName = name + ".boweipcrResult";
                    File resultFile = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/BoWeiResult/" + fileName);
                    resultFile.createNewFile();
                    FileOutputStream outputStream = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/BoWeiResult/" + fileName);
                    outputStream.write(strJson_Config.getBytes());
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "save .boweipcrResult FileOutputStream: " + "error" + e.getMessage());
                    EventBus.getDefault().post(new BoWeiProcedureFinishEvent("finish error",current_template,currentBoWeiPCRStepEntity,null));
                }

                if(s == null){///////正常完成
                    Log.e(TAG, "save .boweipcrResult FileOutputStream: " + "normal" + "正常完成");
                    EventBus.getDefault().post(new BoWeiProcedureFinishEvent("finish",current_template,currentBoWeiPCRStepEntity,Experiment.getInstance()));
                }else{
                    /////////异常完成
                    Log.e(TAG, "save .boweipcrResult FileOutputStream: " + "error" + "异常完成");
                    EventBus.getDefault().post(new BoWeiProcedureFinishEvent("finish error",current_template,currentBoWeiPCRStepEntity,null));
                    /////发送关闭加温 ,在现有正在执行的指令上序号等不变，改成关闭温度 heat指令
                }
            }
        }).start();
        BoWeiConnectManager.getInstance().setPCRRunning(false);
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
                currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                Log.i("Bowei_retry", "下发指令" + "" + currentBoWeiPCRStepEntity_backup.getPCRCmd() + ":" + "retry" + (currentBoWeiPCRStepEntity_backup.getReTryCount()));
                stopTimer();
//                lock.lock(); // 获取锁
//                try {
//                    condition.signal();
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }finally {
//                    lock.unlock(); // 释放锁
//                }
                synchronized(lock) {
                    lock.notify();
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
            timer = null;
        }
    }



    private List<BoWeiPCRTupeTempretureData> temperature_preList;
    private List<BoWeiPCRTupeTempretureData> temperature_postList;

    private List<BoWeiPCRTupeData> fluorescenceList_Raw;

    private List<List<BoWeiPCRTupeData>> fluorescenceList_Raw_List;

    private List<BoWeiPCRTupeData> fluorescenceList_Melt;
    private int CountTemperatureReach = 0;

    private static int AMPcount = 5;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoWeiRecvDataInfo boWeiRecvDataInfo) {


        //上报的状态
        long curTime = System.currentTimeMillis();
        if(curTime > startTime){
            runningTime = curTime - startTime;
        }
        if(totalTime > runningTime){
            remainTime = totalTime - runningTime;
        }
        EventBus.getDefault().post(new BoWeiPCRProcedureInfoEvent(runningTime,remainTime,
                stepDountDownTime,currentBoWeiPCRStepEntity));



        if(boWeiRecvDataInfo.getCmd().contains("set_heartbeat") ){
            return;
        }

        if (fluorescenceList_Raw_List == null || fluorescenceList_Raw_List.size() == 0)
        {
            fluorescenceList_Raw_List = new ArrayList<List<BoWeiPCRTupeData>>();
            for (int k = 0; k < AMPcount; k++){
                List<BoWeiPCRTupeData> fluorescenceList_RawItem = new ArrayList<BoWeiPCRTupeData>();
                for (int i = 0; i < 8; i++)
                {
                    BoWeiPCRTupeData pCRTupeDataBoWei = new BoWeiPCRTupeData(0, 0, 0, 0, 0, 0, 0);
                    fluorescenceList_RawItem.add(pCRTupeDataBoWei);
                }
                fluorescenceList_Raw_List.add(fluorescenceList_RawItem);
            }
        }

        if (fluorescenceList_Raw == null)
        {
            fluorescenceList_Raw = new ArrayList<>();
            for (int i = 0; i < 8; i++)
            {
                BoWeiPCRTupeData pCRTupeDataBoWei = new BoWeiPCRTupeData(0, 0, 0, 0, 0, 0, 0);
                fluorescenceList_Raw.add(pCRTupeDataBoWei);
            }
        }

        if (fluorescenceList_Melt == null)
        {
            fluorescenceList_Melt = new ArrayList<BoWeiPCRTupeData>();

            for (int i = 0; i < 8; i++)
            {
                BoWeiPCRTupeData pCRTupeDataBoWei = new BoWeiPCRTupeData(0, 0, 0, 0, 0, 0, 0);
                fluorescenceList_Melt.add(pCRTupeDataBoWei);
            }
        }

        if(currentBoWeiPCRStepEntity == null)
        {
            return;
        }
        else
        {
            BoWeiPCRStepStatus.currentReturnStatus = boWeiRecvDataInfo.getRecvData() + "--" + boWeiRecvDataInfo.getResult();

            if(boWeiRecvDataInfo.getCmd().contains("statusled") && currentBoWeiPCRStepEntity.getName().contains("statusled"))
            {
                if (boWeiRecvDataInfo.getResult().contains("OK"))
                {

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    synchronized(lock) {
                        lock.notify();
                    }
                }
                else
                {
                    currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity.getReTryCount()-1);
                    actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
//                    try {
//                        Thread.sleep(2000);
//                        condition.signal();
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }finally {
//                        lock.unlock(); // 释放锁
//                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    synchronized(lock) {
                        lock.notify();
                    }
                }
            }
            else if((boWeiRecvDataInfo.getCmd().equals("get_current_temperature_unreach")|| boWeiRecvDataInfo.getCmd().equals("get_current_temperature_reach")) && currentBoWeiPCRStepEntity.getName().equals("pcr_temperature_set"))
            {

                Experiment.getInstance().getTemperatures().add(new Temperature(
                        boWeiRecvDataInfo.getTemperature().get(0).getTemperature(),
                        0,0,System.currentTimeMillis() - startTime));

                if (isPCRTemperatureWaiting)
                {
                    ////reach位直接下一步
                    if (currentBoWeiPCRStepEntity.isAcquisitionAMP() && boWeiRecvDataInfo.isPCRTemperatureReach())
                    {
                       // TempretureQueryThread.getInstance().stopQuery();
                        isPCRTemperatureWaiting = false;
                        totalStepDountDownTime = currentBoWeiPCRStepEntity.getTime() + 3;
                        if(timerAMP == null) {
                            timerAMP = new Timer();
                            TimerTask task = createTask();
                            timerAMP.schedule(task, 0, 200);
                        }
                        return;
                    }
                    ////////////
                    if (currentBoWeiPCRStepEntity.isAcquisitionMelt() && boWeiRecvDataInfo.isPCRTemperatureReach())
                    {
                        TempretureQueryThread.getInstance().stopQuery();
                        isPCRTemperatureWaiting = false;
                        totalStepDountDownTime = currentBoWeiPCRStepEntity.getTime();
                        if(timerAMP == null) {
                            timerAMP = new Timer();
                            TimerTask task = createTask();
                            timerAMP.schedule(task, 0, 200);
                        }
                        synchronized(lock) {
                            lock.notify();
                        }
                        return;
                    }
                }
                else
                {

                }
            }
            else if (boWeiRecvDataInfo.getCmd().equals("get_current_temperature_reach") && currentBoWeiPCRStepEntity.getName().equals("get_current_temperature_pre"))
            {
                if (boWeiRecvDataInfo.getResult().contains("OK"))
                {
                    temperature_preList = boWeiRecvDataInfo.getTemperature();
                    BoWeiPCRStepStatus.currentReturnStatus = boWeiRecvDataInfo.getRecvData() + "--" + boWeiRecvDataInfo.getResult();

                    for (int i = 0; i < temperature_preList.size(); i++)
                    {
                        fluorescenceList_Melt.get(i).setTemperature_pre(temperature_preList.get(i).getTemperature());
                    }
                }
                else
                {
                    currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                    actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                }

                synchronized(lock) {
                    lock.notify();
                }
            }
            else if (boWeiRecvDataInfo.getCmd().equals("get_current_temperature_reach") && currentBoWeiPCRStepEntity.getName().equals("get_current_temperature_post"))
            {
                Experiment.getInstance().getTemperatures().add(new Temperature(
                        boWeiRecvDataInfo.getTemperature().get(0).getTemperature(),
                        0,0,System.currentTimeMillis() - startTime));

                Log.e("get_current_temperature_post",boWeiRecvDataInfo.getRecvData() + "--" + boWeiRecvDataInfo.getResult());
                if (boWeiRecvDataInfo.getResult().contains("OK"))
                {

                    temperature_postList = boWeiRecvDataInfo.getTemperature();
                    BoWeiPCRStepStatus.currentReturnStatus = boWeiRecvDataInfo.getRecvData() + "--" + boWeiRecvDataInfo.getResult();
                    for (int i = 0; i < temperature_postList.size(); i++)
                    {
                        fluorescenceList_Melt.get(i).setTemperature_post(temperature_postList.get(0).getTemperature());
                    }
                }
                else
                {
                    currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                    actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized(lock) {
                    lock.notify();
                }
            }
            else if (boWeiRecvDataInfo.getCmd().contains("pcr_fluorescence_get") && currentBoWeiPCRStepEntity.getName().contains("pcr_fluorescence_get_amp"))
            {
                /////增加5次采样及滤波
                if (boWeiRecvDataInfo.getResult().contains("OK"))
                {
                    if (boWeiRecvDataInfo.getFluorescence() == null || boWeiRecvDataInfo.getFluorescence().size() == 0)
                    {
                        currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                        actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                        synchronized(lock) {
                            lock.notify();
                        }
                        return;
                    }

                    if (currentBoWeiPCRStepEntity.getName().equals("pcr_fluorescence_get_amp_start"))
                    {
                        fluorescenceList_Raw_List.clear();

                        List<BoWeiPCRTupeData> fluorescenceList_RawItem = new ArrayList<>();

                        for (int i = 0; i < 8; i++)
                        {
                            BoWeiPCRTupeData pCRTupeDataBoWei = new BoWeiPCRTupeData(0, 0, 0, 0, 0, 0, 0);
                            pCRTupeDataBoWei.setFluorescence_FAM(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_FAM());
                            pCRTupeDataBoWei.setFluorescence_HEX(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_HEX());
                            pCRTupeDataBoWei.setFluorescence_ROX(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_ROX());
                            pCRTupeDataBoWei.setFluorescence_CY5(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_CY5());
                            fluorescenceList_RawItem.add(pCRTupeDataBoWei);
                        }
                        fluorescenceList_Raw_List.add(fluorescenceList_RawItem);
                    }
                    else if(currentBoWeiPCRStepEntity.getName().equals("pcr_fluorescence_get_amp"))
                    {
                        List<BoWeiPCRTupeData> fluorescenceList_RawItem = new ArrayList<>();

                        for (int i = 0; i < 8; i++)
                        {
                            BoWeiPCRTupeData pCRTupeDataBoWei = new BoWeiPCRTupeData(0, 0, 0, 0, 0, 0, 0);
                            pCRTupeDataBoWei.setFluorescence_FAM(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_FAM());
                            pCRTupeDataBoWei.setFluorescence_HEX(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_HEX());
                            pCRTupeDataBoWei.setFluorescence_ROX(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_ROX());
                            pCRTupeDataBoWei.setFluorescence_CY5(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_CY5());
                            fluorescenceList_RawItem.add(pCRTupeDataBoWei);
                        }
                        fluorescenceList_Raw_List.add(fluorescenceList_RawItem);
                    }
                    else if (currentBoWeiPCRStepEntity.getName().equals("pcr_fluorescence_get_amp_end"))
                    {
                        //////使用多次滤波时需要注释掉
                        fluorescenceList_Raw_List.clear();

                        List<BoWeiPCRTupeData> fluorescenceList_RawItem = new ArrayList<>();

                        for (int i = 0; i < 8; i++)
                        {
                            BoWeiPCRTupeData pCRTupeDataBoWei = new BoWeiPCRTupeData(0, 0, 0, 0, 0, 0, 0);
                            pCRTupeDataBoWei.setFluorescence_FAM(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_FAM());
                            pCRTupeDataBoWei.setFluorescence_HEX(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_HEX());
                            pCRTupeDataBoWei.setFluorescence_ROX(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_ROX());
                            pCRTupeDataBoWei.setFluorescence_CY5(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_CY5());
                            fluorescenceList_RawItem.add(pCRTupeDataBoWei);
                        }
                        fluorescenceList_Raw_List.add(fluorescenceList_RawItem);
                        ////////采集做滤波
                        for (int i = 0; i < fluorescenceList_Raw.size(); i++)
                        {
                            fluorescenceList_Raw.get(i).setFluorescence_FAM(0);
                            fluorescenceList_Raw.get(i).setFluorescence_HEX(0);
                            fluorescenceList_Raw.get(i).setFluorescence_ROX(0);
                            fluorescenceList_Raw.get(i).setFluorescence_CY5(0);

                            for (int j = 0; j < fluorescenceList_Raw_List.size(); j++)
                            {
                                fluorescenceList_Raw.get(i).setFluorescence_FAM(fluorescenceList_Raw.get(i).getFluorescence_FAM() + fluorescenceList_Raw_List.get(j).get(i).getFluorescence_FAM());
                                fluorescenceList_Raw.get(i).setFluorescence_HEX(fluorescenceList_Raw.get(i).getFluorescence_HEX() + fluorescenceList_Raw_List.get(j).get(i).getFluorescence_HEX());
                                fluorescenceList_Raw.get(i).setFluorescence_ROX(fluorescenceList_Raw.get(i).getFluorescence_ROX() + fluorescenceList_Raw_List.get(j).get(i).getFluorescence_ROX());
                                fluorescenceList_Raw.get(i).setFluorescence_CY5(fluorescenceList_Raw.get(i).getFluorescence_CY5() + fluorescenceList_Raw_List.get(j).get(i).getFluorescence_CY5());
                            }
                            fluorescenceList_Raw.get(i).setFluorescence_FAM(fluorescenceList_Raw.get(i).getFluorescence_FAM() / fluorescenceList_Raw_List.size());
                            fluorescenceList_Raw.get(i).setFluorescence_HEX(fluorescenceList_Raw.get(i).getFluorescence_HEX() / fluorescenceList_Raw_List.size());
                            fluorescenceList_Raw.get(i).setFluorescence_ROX(fluorescenceList_Raw.get(i).getFluorescence_ROX() / fluorescenceList_Raw_List.size());
                            fluorescenceList_Raw.get(i).setFluorescence_CY5(fluorescenceList_Raw.get(i).getFluorescence_CY5() / fluorescenceList_Raw_List.size());
                        }

                        for (int i = 0; i < fluorescenceList_Raw.size(); i++)
                        {
                            for (int j = 0; j < Experiment.getInstance().getWellResult().get(i).getTargetResults().size(); j++)
                            {
                                if (Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getDye().equals("FAM"))
                                {
                                    Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getRawCurve().add(fluorescenceList_Raw.get(i).getFluorescence_FAM());
                                }
                                else if (Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getDye().equals("HEX"))
                                {
                                    Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getRawCurve().add(fluorescenceList_Raw.get(i).getFluorescence_HEX());
                                }
                                else if (Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getDye().equals("ROX"))
                                {
                                    Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getRawCurve().add(fluorescenceList_Raw.get(i).getFluorescence_ROX());
                                }
                                else if (Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getDye().equals("CY5"))
                                {
                                    Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getRawCurve().add(fluorescenceList_Raw.get(i).getFluorescence_CY5());
                                }
                            }
                        }
                        ////////显示实时曲线todo
                        //PCRCurveData.AddAOriginData(fluorescenceList_Raw);
                        fluorescenceList_Raw_List.clear();

                    }
                }
                else
                {
                    currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                    actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                }
                synchronized(lock) {
                    lock.notify();
                }
            }
            else if (boWeiRecvDataInfo.getCmd().contains("pcr_fluorescence_get") && currentBoWeiPCRStepEntity.getName().contains("pcr_fluorescence_get_melt"))
            {
                if (boWeiRecvDataInfo.getResult().contains("OK"))
                {
                    //Thread.Sleep(000);
                    //fluorescenceList_Melt = obj.Fluorescence;

                    if (boWeiRecvDataInfo.getFluorescence() == null || boWeiRecvDataInfo.getFluorescence().size() == 0)
                    {
                        currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                        actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                    }
                    else
                    {
                        for (int i = 0; i < boWeiRecvDataInfo.getFluorescence().size(); i++)
                        {
                            fluorescenceList_Melt.get(i).setFluorescence_FAM(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_FAM());
                            fluorescenceList_Melt.get(i).setFluorescence_HEX(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_HEX());
                            fluorescenceList_Melt.get(i).setFluorescence_ROX(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_ROX());
                            fluorescenceList_Melt.get(i).setFluorescence_CY5(boWeiRecvDataInfo.getFluorescence().get(i).getFluorescence_CY5());
                        }
                        for (int i = 0; i < fluorescenceList_Melt.size(); i++)
                        {
                            for (int j = 0; j < Experiment.getInstance().getWellResult().get(i).getTargetResults().size(); j++)
                            {
                                if (Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getDye().equals("FAM"))
                                {
                                    //double temperature = (fluorescenceList_Melt[i].Temperature_pre + fluorescenceList_Melt[i].Temperature_post) / 2;
                                    double temperature = fluorescenceList_Melt.get(i).getTemperature_post();
                                    double fluro = fluorescenceList_Melt.get(i).getFluorescence_FAM();
                                    Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getRawMeltCurve().add(new Dot(temperature, fluro));
                                }
                                else if (Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getDye().equals("HEX"))
                                {
                                    //double temperature = (fluorescenceList_Melt[i].Temperature_pre + fluorescenceList_Melt[i].Temperature_post) / 2;
                                    double temperature = fluorescenceList_Melt.get(i).getTemperature_post();
                                    double fluro = fluorescenceList_Melt.get(i).getFluorescence_HEX();
                                    Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getRawMeltCurve().add(new Dot(temperature, fluro));
                                }
                                else if (Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getDye().equals("ROX"))
                                {
                                    //double temperature = (fluorescenceList_Melt[i].Temperature_pre + fluorescenceList_Melt[i].Temperature_post) / 2;
                                    double temperature = fluorescenceList_Melt.get(i).getTemperature_post();
                                    double fluro = fluorescenceList_Melt.get(i).getFluorescence_ROX();
                                    Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getRawMeltCurve().add(new Dot(temperature, fluro));
                                }
                                else if (Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getDye().equals("CY5"))
                                {
                                    //double temperature = (fluorescenceList_Melt[i].Temperature_pre + fluorescenceList_Melt[i].Temperature_post) / 2;
                                    double temperature = fluorescenceList_Melt.get(i).getTemperature_post();
                                    double fluro = fluorescenceList_Melt.get(i).getFluorescence_CY5();
                                    Experiment.getInstance().getWellResult().get(i).getTargetResults().get(j).getRawMeltCurve().add(new Dot(temperature, fluro));
                                }
                            }

                        }
                        ///////原始熔解曲线显示
                        //PCRCurveData.AddAMeltData(fluorescenceList_Melt);
                    }
                }
                else
                {
                    currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                    actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                }
                synchronized(lock) {
                    lock.notify();
                }
            }
            else if (boWeiRecvDataInfo.getCmd().contains("motor_") && currentBoWeiPCRStepEntity.getName().contains("motor_reset"))
            {
                if (boWeiRecvDataInfo.getResult().contains("OK"))
                {
                    Log.i("motor_reset", "-----ok" + "test");
                }
                else
                {
                    Log.i("motor_reset", "-----error" + "test");
                    currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                    actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                synchronized(lock) {
                    lock.notify();
                }
            }
            else if (boWeiRecvDataInfo.getCmd().equals("motor_") && currentBoWeiPCRStepEntity.getName().equals("motor_scan"))
            {
                if (boWeiRecvDataInfo.getResult().contains("OK"))
                {
                    if (currentBoWeiPCRStepEntity.isAcquisitionAMP())
                    {
//                        try {
//                            Thread.sleep(300);
//                        } catch (InterruptedException e) {
//                            throw new RuntimeException(e);
//                        }
                    }
                }
                else
                {
                    currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                    actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                }
                synchronized(lock) {
                    lock.notify();
                }
            }
            else if (boWeiRecvDataInfo.getCmd().equals("pcr_temperature_set") && currentBoWeiPCRStepEntity.getName().equals("pcr_temperature_set"))
            {

                if (boWeiRecvDataInfo.getResult().contains("OK"))
                {
                    /////等待升温过程触发，不用触发下一条动作
                    ///并启动温度巡检线程
                    isPCRTemperatureWaiting = true;
                    TempretureQueryThread.getInstance().startQuery();
                    stopTimer();////停止回复ACKtimer
                    Log.i("pcr_temperature_set", "-----ok" + currentBoWeiPCRStepEntity.getPCR_temperature());

                }
                else
                {
                    currentBoWeiPCRStepEntity_backup.setReTryCount(currentBoWeiPCRStepEntity_backup.getReTryCount() - 1);
                    actionListQueue.enQueueHead(currentBoWeiPCRStepEntity_backup);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    synchronized(lock) {
                        lock.notify();
                    }
                }

            }
        }

    }


    private void createPCRStepEntityList(Project boweiProject){

        BoWeiPCRStepEntityList = new ArrayList<>();

        BoWeiPCRStepEntityList.clear();

        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_StatusLedBlink_Message());
        ///没有热盖
        //PCRStepEntity.PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Hotlid_Temperature_Message(105));

        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Moter_Reset_Message(0, 1, 0));

        double temperturePre = 0.0f;

        for (int i = 0 ; i < boweiProject.getProgram().getSections().size(); i++)
        {

            if(boweiProject.getProgram().getSections().get(i) instanceof SectionAMP)
            {
                //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Moter_Reset_AMP_Message(i + 1, 1, 1));

                SectionAMP sectionAMP = (SectionAMP) boweiProject.getProgram().getSections().get(i);

                if(sectionAMP.getCycles() == 1 || sectionAMP.getCycles() == 0)
                {
                    for (int k = 0; k < sectionAMP.getItems().size(); k++)
                    {
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_PCR_SETTemperature_AMP_Message(sectionAMP.getItems().get(k).getTemperature() , sectionAMP.getItems().get(k).getHoldTime(),i + 1, sectionAMP.getName(),1,k + 1));
                        if (i == 0 && k == 0)
                        {
                            if (sectionAMP.getItems().get(k).getTemperature() >= 30 )
                            {
                                BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                            }
                            else
                            {
                                BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                            }
                        }
                        else
                        {
                            if (sectionAMP.getItems().get(k).getTemperature() > temperturePre)
                            {
                                BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                            }
                            else if(sectionAMP.getItems().get(k).getTemperature() < temperturePre)
                            {
                                BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                            }
                        }


                        temperturePre = sectionAMP.getItems().get(k).getTemperature();



                        if (sectionAMP.getItems().get(k).getIsAcquisition())
                        {
                            //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Moter_Reset_AMP_Message(i + 1, 1, k + 1));
                            /////增加5次采样及滤波
                            //for (int oo = 0;oo < 5; oo++)
                            //{
                            //    if (oo == 0)
                            //    {
                            //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, 1, k + 1));
                            //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message_Start(i + 1, 1, k + 1));
                            //    }else if(oo == 4)
                            //    {
                            BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, sectionAMP.getName(),1, k + 1));
                            BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message_End(i + 1, sectionAMP.getName(),1, k + 1));
                            //    }else
                            //    {
                            //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, 1, k + 1));
                            //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message(i + 1, 1, k + 1));
                            //    }
                            //}
                        }
                    }
                }
                else
                {
                    //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Loopstart_Message(sectionAMP.Cycles));
                    for (int j = 0; j < sectionAMP.getCycles(); j++)
                    {
                        for (int k = 0; k < sectionAMP.getItems().size(); k++)
                        {
                            BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_PCR_SETTemperature_AMP_Message(sectionAMP.getItems().get(k).getTemperature(), sectionAMP.getItems().get(k).getHoldTime(),i + 1, sectionAMP.getName(),j + 1, k + 1));
                            if (i == 0 && k == 0)
                            {
                                if (sectionAMP.getItems().get(k).getTemperature() >= 30)
                                {
                                    BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                                }
                                else
                                {
                                    BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                                }
                            }
                            else
                            {
                                if (sectionAMP.getItems().get(k).getTemperature() > temperturePre)
                                {
                                    BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                                }
                                else if(sectionAMP.getItems().get(k).getTemperature() < temperturePre)
                                {
                                    BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                                }
                            }


                            temperturePre = sectionAMP.getItems().get(k).getTemperature();


                            if (sectionAMP.getItems().get(k).getIsAcquisition())
                            {
                                //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Moter_Reset_AMP_Message(i + 1, j + 1, k + 1));
                                //for (int oo = 0; oo < 5; oo++)
                                //{
                                //    if (oo == 0)
                                //    {
                                //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, j + 1, k + 1));
                                //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message_Start(i + 1, j + 1, k + 1));
                                //    }
                                //    else if (oo == 4)
                                //    {
                                BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, sectionAMP.getName(),j + 1, k + 1));
                                BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message_End(i + 1, sectionAMP.getName(),j + 1, k + 1));
                                //    }
                                //    else
                                //    {
                                //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, j + 1, k + 1));
                                //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message(i + 1, 1, k + 1));
                                //    }

                                //}

                            }
                        }
                    }

                    //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Loopend_Message(sectionAMP.Cycles));
                }
            }
            else if (boweiProject.getProgram().getSections().get(i) instanceof SectionMelt)
            {

                SectionMelt sectionMelt = (SectionMelt) boweiProject.getProgram().getSections().get(i);

                //double totalSteps_Melt = ((sectionMelt.getEndTemp() - sectionMelt.getStartTemp()) / (sectionMelt.getInterval() * 0.1f)) + 1.0f ;

                double totalSteps_Melt;
                //double totalSteps_Melt = ((sectionMelt.getEndTemp() - sectionMelt.getStartTemp()) / (sectionMelt.getInterval() * 0.1f)) + 1.0f ;
                if(sectionMelt.getInterval() < 0.1f || sectionMelt.getInterval() > 0.2f ){
                    totalSteps_Melt = ((sectionMelt.getEndTemp() - sectionMelt.getStartTemp()) / (0.1f)) + 1.0f ;
                }else{
                    totalSteps_Melt = ((sectionMelt.getEndTemp() - sectionMelt.getStartTemp()) / (sectionMelt.getInterval())) + 1.0f ;
                }

                //if (sectionMelt.getScanMode() == MeltScanMode.Step)
                if (true)
                {

                    BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_PCR_SETTemperature_AMP_Message(sectionMelt.getStartTemp(), 30 , i + 1, sectionMelt.getName(),1, 0));
                    //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_Melt_Message(i + 1, 1, 0));////获取荧光点数值
                    if (sectionMelt.getStartTemp() > temperturePre)
                    {
                        BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                    }
                    else if(sectionMelt.getStartTemp() < temperturePre)
                    {
                        BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                    }
                    temperturePre = sectionMelt.getEndTemp();
                    BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Moter_Reset_Melt_Message(i + 1,sectionMelt.getName(), 1, 0));
                    for (int k = 0; k < totalSteps_Melt; k++)
                    {
//                        double temperature = sectionMelt.getStartTemp() + k * sectionMelt.getInterval();
                        double temperature;
                        if(sectionMelt.getInterval() < 0.1f || sectionMelt.getInterval() > 0.2f ){
                            temperature = sectionMelt.getStartTemp() + k * (0.1f);
                        }else{
                            temperature = sectionMelt.getStartTemp() + k * sectionMelt.getInterval();
                        }
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_PCR_SETTemperature_Melt_Message(temperature, 0.2, i + 1, sectionMelt.getName(),1, k + 1));/////需要停留多长时间？？？？？
                        //BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Moter_Reset_Melt_Message(i + 1, sectionMelt.getName(), 1,k + 1));
                        //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetCurrentPCRTemperature_Message_Pre(i + 1, 1, k + 1));////获取前温度
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Scan_Melt_Message(i + 1, sectionMelt.getName(),1, k + 1));////获取荧光点数值
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_GetCurrentPCRTemperature_Message_Post(i + 1, sectionMelt.getName(),1, k + 1));////获取后温度
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_GetMeltFluorescenceData_Message(i + 1, sectionMelt.getName(),1, k + 1));
                    }
                }
            }
        }
        //关闭热盖
        ////PCRStepEntity.PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Hotlid_Temperature_Message(200));
        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Moter_Reset_Message(0,0,0));
        //BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_StopPeltier_Message());
        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_StatusLedBright_Message());
        for (int t = 0; t < BoWeiPCRStepEntityList.size(); t++)
        {
            BoWeiPCRStepEntityList.get(t).setMemoryNote(t + "");
        }
        Log.i("BoweiService","BoWeiPCRStepEntityList" + BoWeiPCRStepEntityList.size());
    }


    /**
     * 获取项目的时间
     * @param project
     * @return
     */
    private int getProjectTotalTime(Project project){
        int time = 0;
        double lastTemperature = 35.0f;
        if(project != null){
            Program program = project.getProgram();
            ArrayList<Section> sections = program.getSections();
            if(null != sections && sections.size()>0){

                for(int i= 0;i<sections.size();i++) {
                    Section section = sections.get(i);
                    if (section != null) {
                        if (section instanceof SectionAMP) {
                            SectionAMP sectionAMP = (SectionAMP) section;
                            //扩增程序段
                            //当前程序段的循环
                            int sectionAmpTime = 0;
                            if (sectionAMP.getItems() != null && sectionAMP.getItems().size() > 0) {
                                for (int k = 0; k < sectionAMP.getItems().size(); k++) {
                                    Step step = sectionAMP.getItems().get(k);
                                    if (null != step) {
                                        double currentTemp = step.getTemperature();//获取扩增当前步骤的温度
                                        if(currentTemp > lastTemperature){
                                            BigDecimal divider1 = new BigDecimal(currentTemp)
                                                    .subtract(new BigDecimal(lastTemperature));
                                            int valTime1 = divider1.divide(new BigDecimal(4.0)).intValue();
                                           // time = time + valTime1;
                                            sectionAmpTime = sectionAmpTime + valTime1;
                                          //  Log.i("mmm","valTime1" + valTime1);
                                          //  Log.i("mmm","sectionAmpTime" + sectionAmpTime);
                                        }else if(lastTemperature > currentTemp){
                                            BigDecimal divider2 = new BigDecimal(lastTemperature)
                                                    .subtract(new BigDecimal(currentTemp));
                                            int valTime2 = divider2.divide(new BigDecimal(4.0)).intValue();
                                           // time = time + valTime2;
                                            sectionAmpTime = sectionAmpTime + valTime2;
                                         //   Log.i("mmm","sectionAmpTime2" + sectionAmpTime);
                                           // Log.i("mmm","valTime2" + valTime2);
                                        }
                                        lastTemperature = currentTemp;
                                        int stepTime = step.getHoldTime();
                                      //  time = time + stepTime;
                                        sectionAmpTime = sectionAmpTime + stepTime;
                                    }

                                }
                            }
                            int recyle = sectionAMP.getCycles();
                            time = time + sectionAmpTime*recyle + recyle*8;
                         //   Log.i("mmm","当前的程序段time" + time);
                        } else if (section instanceof SectionMelt) {
                            SectionMelt sectionMelt = (SectionMelt) section;
                            double rate = sectionMelt.getRate();
                            double startTemp = sectionMelt.getStartTemp();
                            double endTemp = sectionMelt.getEndTemp();

                            BigDecimal result1 = new BigDecimal(endTemp).subtract(new BigDecimal(startTemp));
                            BigDecimal result2 = result1.divide(new BigDecimal(rate),2,BigDecimal.ROUND_HALF_UP);
                            int melttime = result2.intValue();
                            time = time + melttime+20;
                            Log.i("mmm","melttime" + melttime);
                            lastTemperature = endTemp;
                        }
                    }


                }
            }
        }
        Log.i("mmm","time" + time);
        return time;
    }
}
