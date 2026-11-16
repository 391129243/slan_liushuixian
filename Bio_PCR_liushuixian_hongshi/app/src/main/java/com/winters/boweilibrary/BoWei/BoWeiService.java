package com.winters.boweilibrary.BoWei;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;


import com.gidi.bio_pcr.bean.PeaktypeOptimization;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.hongshi.slansdk.service.SLANService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.ExperimentData.WellResult;
import com.winters.boweilibrary.BoWei.bean.RunBiowayResult;
import com.winters.boweilibrary.BoWei.bean.SaveExperimentTask;
import com.winters.boweilibrary.BoWei.bioweyenum.DeviceState;
import com.winters.boweilibrary.BoWei.bioweyenum.ProjectType;
import com.winters.boweilibrary.BoWei.bioweyenum.RunExperimentResult;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningError;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningState;
import com.winters.boweilibrary.BoWei.event.BoWeiPCRProcedureInfoEvent;
import com.winters.boweilibrary.BoWei.event.BoWeiProcedureFinishEvent;
import com.winters.boweilibrary.BoWei.event.BoWeiRecvDataInfo;
import com.winters.boweilibrary.BoWei.event.BoweiConnectEvent;
import com.winters.boweilibrary.BoWei.listener.DeviceStateListener;
import com.winters.boweilibrary.BoWei.listener.RunningInfoListener;
import com.winters.boweilibrary.BoWei.model.device.Device;
import com.winters.boweilibrary.BoWei.model.experiment.ExperimentParam;
import com.winters.boweilibrary.BoWei.model.experiment.RunningInfo;
import com.winters.boweilibrary.BoWei.model.experiment.Well;
import com.winters.boweilibrary.BoWei.serialcom.BoWeiConnectManager;
import com.winters.boweilibrary.BoWei.task.BoWeiPCRAsyncTask;
import com.winters.boweilibrary.BoweiJni;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BoWeiService {

    private Context context;
    private RunningInfo runningInfo;
    private String blockTemperature1;
    private String blockTemperature2;
    private String blockTemperature3;
    private boolean isRunning;

    private ProjectType type = ProjectType.AQ;//项目类型
    private Project runProject = null;


    private static BoWeiService mInstance;
    public static  BoWeiService getInstance(){
        if (null == mInstance) {
            synchronized (BoWeiService.class){
                if (null == mInstance){
                    mInstance = new BoWeiService();
                }
            }
        }
        return  mInstance;
    }

    public  BoWeiService(){


    }

    /**
     * 初始化服务
     * 初始化连接
     */
    public void init(Context context){
        this.context = context;
        runningInfo = new RunningInfo();
        BoWeiConnectManager.getInstance().initBoWeiConnection(context);
        registerEventBus();
    }


    /**
     * 释放服务
     */
    public void destroy(){
        unRegisterEventBus();
    }

    protected void registerEventBus(){
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    protected void unRegisterEventBus(){
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }




    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoWeiRecvDataInfo boWeiRecvDataInfo) {
        if(boWeiRecvDataInfo.getCmd().contains("set_heartbeat")){
            return;
        }
        if(boWeiRecvDataInfo.getCmd().contains("get_current_temperature")){
            blockTemperature1 = NumberUtils.doubleto2String(boWeiRecvDataInfo.getTemperature().get(0).getTemperature());
            blockTemperature2 = NumberUtils.doubleto2String(boWeiRecvDataInfo.getTemperature().get(3).getTemperature());
            blockTemperature3 = NumberUtils.doubleto2String(boWeiRecvDataInfo.getTemperature().get(6).getTemperature());
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoWeiPCRProcedureInfoEvent boWeiPCRProcedureInfoEvent) {
        isRunning = true;
        runningInfo.setSegment(boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getSectionName());
        runningInfo.setCycle(boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getCurrentCycle());//循环
        runningInfo.setStep("步骤"+boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getCurrentStep()+"");//步骤
        runningInfo.setRemainTime(boWeiPCRProcedureInfoEvent.getRemainTime());
        runningInfo.setRunningTime(boWeiPCRProcedureInfoEvent.getRuningTime());
        runningInfo.setHoldTime(boWeiPCRProcedureInfoEvent.getHoldTime());
        runningInfo.setState(RunningState.Running);
        runningInfo.setCurrentRunning(RunningState.Running.toString());
        runningInfo.setError(RunningError.None);
        runningInfo.setCurrentError(RunningError.None.toString());
        runningInfo.setBlockTemperature1(blockTemperature1);
        runningInfo.setBlockTemperature2(blockTemperature2);
        runningInfo.setBlockTemperature3(blockTemperature3);
        runningInfo.setType(type);
        runningInfo.setTypeName(type.getName());
        if(boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getPCR_temperature() != 0.0f){
            runningInfo.setSetTemperature (NumberUtils.doubleto1String(boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getPCR_temperature()));//实验温度
        }

        if(null != runningInfoListener){
            runningInfoListener.getRunningInfo("",(byte) 0x01,runningInfo);
        }

        if(null != deviceStateListener){
            deviceStateListener.getDeviceInfo(DeviceState.Running);
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoWeiProcedureFinishEvent procedureFinishEvent) {
       // Log.i(TAG,"procedureFinishEvent.getStatus()" + procedureFinishEvent.getStatus());
        if(true){
            if(procedureFinishEvent.getStatus().equals("finish")){
                runningInfo.setState(RunningState.Finished);//运行状态枚举
                runningInfo.setCurrentRunning(RunningState.Finished.toString());
                runningInfo.setError(RunningError.None);
                runningInfo.setCurrentError(RunningError.None.toString());
                //完成
                if(null != deviceStateListener){
                    deviceStateListener.getDeviceInfo(DeviceState.None);
                }
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                runningInfo.setState(RunningState.Interrupt);//运行状态枚举
                runningInfo.setCurrentRunning(RunningState.Interrupt.toString());
                if(null != deviceStateListener){
                    deviceStateListener.getDeviceInfo(DeviceState.Interrupt);
                }
            }
            isRunning = false;
            if(null != runningInfoListener){
                runningInfoListener.getRunningInfo("",(byte) 0x01,runningInfo);
            }
        }
    }

    /**
     * 设备连接状态上报
     * @param connectEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoweiConnectEvent connectEvent) {
        if(null != deviceStateListener){
            boolean isConnect = connectEvent.isConnect();

            if(isConnect){
                if(!isRunning){
                    deviceStateListener.getDeviceInfo(DeviceState.None);
                }
            }else {
                deviceStateListener.getDeviceInfo(DeviceState.Error);
            }
        }
    }

    /**
     * 设备状态监听
     */
    private DeviceStateListener deviceStateListener;
    public void setDeviceStateListener(DeviceStateListener listener){
        this.deviceStateListener = listener;
    }

    /**
     * 设备运行信息监听
     */
    private RunningInfoListener runningInfoListener;
    public void setRunningInfoListener(RunningInfoListener listener){
        this.runningInfoListener = listener;
    }



    /**
     * 获取已经连接的设备列表
     */
    public ArrayList<Device> getDevice(){
        ArrayList<Device> devices = new ArrayList<>();
        return  devices;
    }


    /**
     * 打开实验文件
     * @param filepath 文件路径
     * @return
     */
    public Experiment openExperiment(String filepath){
        Log.e("BiowayExDetailActivity","openExperiment--" + filepath);
        Experiment experiment = new Experiment();
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                // 处理 NaN 和 Infinity，避免生成非法 JSON
                if (src == null || Double.isNaN(src) || Double.isInfinite(src)) {
                    return new JsonPrimitive(0);
                }
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        try {
            String exResultStr = FileUtils.getStringFromSDFile(filepath);
            Log.e("BiowayExDetailActivity","exResultStr" + exResultStr);
            if(!StringUtil.isEmpty(exResultStr)){
                JSONObject jsonObject  = new JSONObject(exResultStr);
                String Auditor= jsonObject.getString("Auditor");
                experiment.setAuditor(Auditor);
                String BatchNo = jsonObject.getString("BatchNo");
                experiment.setBatchNo(BatchNo);
                String EndTime = jsonObject.getString("EndTime");
                experiment.setEndTime(EndTime);
                String Name =  jsonObject.getString("Name");
                experiment.setEndTime(Name);
                String Note= jsonObject.getString("Note");
                experiment.setNote(Note);
                String PlateNo =  jsonObject.getString("PlateNo");
                experiment.setPlateNo(PlateNo);


                JSONObject OperatorObj = jsonObject.optJSONObject("Operator");
                if(OperatorObj != null) {
                    String Operator = jsonObject.getString("Operator");
                    Log.e("BiowayExDetailActivity","Operator" + Operator);
                    experiment.setOperator(Operator);
                }


                String startTime= jsonObject.getString("StartTime");
                experiment.setStartTime(startTime);

                JSONArray CustomColumnsAry = jsonObject.getJSONArray("CustomColumns");//List<String>
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> CustomColumns = gson.fromJson(CustomColumnsAry.toString(),listType);
                experiment.setCustomColumns(CustomColumns);

                JSONArray WellResultAry = jsonObject.getJSONArray("WellResult");//List<String>
                Type WellResultType = new TypeToken<List<WellResult>>(){}.getType();
                List<WellResult> WellResult = gson.fromJson(WellResultAry.toString(),WellResultType);
                experiment.setWellResult(WellResult);

                JSONObject Obj = jsonObject.optJSONObject("Project");
                if(Obj!=null) {
                    JSONObject projectObj = jsonObject.getJSONObject("Project");
                    Project slanProject = createNewProgect(projectObj);
                    experiment.setProject(slanProject);
                }
                //峰型优化

                boolean optimization_enabl_FAM = jsonObject.getBoolean("optimization_enabl_FAM");
                experiment.setOptimization_enabl_FAM(optimization_enabl_FAM);
                boolean optimization_enabl_HEX = jsonObject.getBoolean("optimization_enabl_HEX");
                experiment.setOptimization_enabl_HEX(optimization_enabl_HEX);
                boolean optimization_enabl_ROX= jsonObject.getBoolean("optimization_enabl_ROX");
                experiment.setOptimization_enabl_ROX(optimization_enabl_ROX);
                boolean optimization_enabl_CY5 = jsonObject.getBoolean("optimization_enabl_CY5");
                experiment.setOptimization_enabl_CY5(optimization_enabl_CY5);

                double enhance_gain_FAM = jsonObject.getDouble("enhance_gain_FAM");
                experiment.setEnhance_gain_FAM(enhance_gain_FAM);
                double enhance_gain_HEX = jsonObject.getDouble("enhance_gain_HEX");
                experiment.setEnhance_gain_HEX(enhance_gain_HEX);
                double enhance_gain_ROX = jsonObject.getDouble("enhance_gain_ROX");
                experiment.setEnhance_gain_ROX(enhance_gain_ROX);
                double enhance_gain_CY5 = jsonObject.getDouble("enhance_gain_CY5");
                experiment.setEnhance_gain_CY5(enhance_gain_CY5);

                double enhance_lower_FAM = jsonObject.getDouble("enhance_lower_FAM");
                experiment.setEnhance_lower_FAM(enhance_lower_FAM);
                double enhance_lower_HEX = jsonObject.getDouble("enhance_lower_HEX");
                experiment.setEnhance_lower_HEX(enhance_lower_HEX);
                double enhance_lower_ROX = jsonObject.getDouble("enhance_lower_ROX");
                experiment.setEnhance_lower_ROX(enhance_lower_ROX);
                double enhance_lower_CY5 = jsonObject.getDouble("enhance_lower_CY5");
                experiment.setEnhance_lower_CY5(enhance_lower_CY5);

                double enhance_upper_FAM = jsonObject.getDouble("enhance_upper_FAM");
                experiment.setEnhance_upper_FAM(enhance_upper_FAM);
                double enhance_upper_HEX = jsonObject.getDouble("enhance_upper_HEX");
                experiment.setEnhance_upper_HEX(enhance_upper_HEX);
                double enhance_upper_ROX = jsonObject.getDouble("enhance_upper_ROX");
                experiment.setEnhance_upper_ROX(enhance_upper_ROX);
                double enhance_upper_CY5 = jsonObject.getDouble("enhance_upper_CY5");
                experiment.setEnhance_upper_CY5(enhance_upper_CY5);

                double weaken_FAM = jsonObject.getDouble("weaken_FAM");
                experiment.setWeaken_FAM(weaken_FAM);
                double weaken_HEX = jsonObject.getDouble("weaken_HEX");
                experiment.setWeaken_HEX(weaken_HEX);
                double weaken_ROX = jsonObject.getDouble("weaken_ROX");
                experiment.setWeaken_ROX(weaken_ROX);
                double weaken_CY5 = jsonObject.getDouble("weaken_CY5");
                experiment.setWeaken_CY5(weaken_CY5);

                double weaken_gain_FAM = jsonObject.getDouble("weaken_gain_FAM");
                experiment.setWeaken_gain_FAM(weaken_gain_FAM);
                double weaken_gain_HEX = jsonObject.getDouble("weaken_gain_HEX");
                experiment.setWeaken_gain_HEX(weaken_gain_HEX);
                double weaken_gain_ROX = jsonObject.getDouble("weaken_gain_ROX");
                experiment.setWeaken_gain_ROX(weaken_gain_ROX);
                double weaken_gain_CY5 = jsonObject.getDouble("weaken_gain_CY5");
                experiment.setWeaken_gain_CY5(weaken_gain_CY5);

                double slope_thres_FAM = jsonObject.getDouble("slope_thres_FAM");
                experiment.setSlope_thres_FAM(slope_thres_FAM);
                double slope_thres_HEX = jsonObject.getDouble("slope_thres_HEX");
                experiment.setSlope_thres_HEX(slope_thres_HEX);
                double slope_thres_ROX = jsonObject.getDouble("slope_thres_ROX");
                experiment.setSlope_thres_CY5(slope_thres_ROX);
                double slope_thres_CY5 = jsonObject.getDouble("slope_thres_CY5");
                experiment.setSlope_thres_CY5(slope_thres_CY5);

            }


        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return experiment;
    }




    /**
     * 保存实验文件
     * @param name 实验名称不需要后缀
     * @param filePath 实验路径
     * @return 保存是否成功
     */
    private SaveExperimentTask saveExperimentTask = null;
    boolean save_result = false;
    public void saveExperiment(Experiment experiment ,
                                  String name,
                                  String filePath,
                                  OnSaveExperimentListener listener){

        saveExperimentTask = new SaveExperimentTask(experiment,name,filePath);
        saveExperimentTask.execute();
        saveExperimentTask.setOnSaveExperimentListener(listener);/*{
            @Override
            public void onSaveSuccess() {
                save_result = true;
                Log.i("BioWay","saveExperiment 保存成功");
                cancelSaveExTask();
            }

            @Override
            public void onSaveFail() {
                save_result = false;
                cancelSaveExTask();
            }
        });*/
        //return save_result;
    }

    private void cancelSaveExTask(){
        if(null != saveExperimentTask){
            saveExperimentTask.cancel(true);
            saveExperimentTask = null;
        }
    }

    /*
     * 运行实验
     * @param experimentParam
     * @return
     */
    public RunBiowayResult runExperiment(ExperimentParam experimentParam){
        //打开PRJ文件获取
        RunBiowayResult runResult = new RunBiowayResult();
        if(experimentParam == null){
            runResult.setRunExperimentResult(RunExperimentResult.ParamNull);
            runResult.setProject(null);
            return runResult;
        }
        ArrayList<Well> wells = experimentParam.getWells();
        String path = experimentParam.getPath();
        String fileName = experimentParam.getName();
        if(!StringUtil.isEmpty(path)){
            //找到prj文件
            runProject = getSLANProject(fileName,path);
            if(null != runProject){
                int projectType = runProject.getType();
                if(projectType == ProjectType.AQ.getCode()){
                    type = ProjectType.AQ;
                }else{
                    type = ProjectType.Melt;
                }
                if(Experiment.getInstance() != null&&  wells != null){
                    Experiment.getInstance().setExperimentWells(wells);
                }
                Log.i("BoweiService","runProject---" + runProject.getName());
                Log.i("BoweiService","BoWeiPCRAsyncTask.getInstance().ismIsrunning()---" + BoWeiPCRAsyncTask.getInstance().isRunning());
                if(BoWeiPCRAsyncTask.getInstance().isRunning()){
                    runResult.setRunExperimentResult(RunExperimentResult.RunningError);
                    runResult.setProject(null);
                    return runResult;
                }
                BoWeiPCRAsyncTask.current_template = fileName;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    //根据不同的api采用并行模式进行开启
                    //////启动流程的API
                    BoWeiPCRAsyncTask.getInstance().executeOnExecutor(THREAD_POOL_EXECUTOR, runProject);
                } else {
                    BoWeiPCRAsyncTask.getInstance().execute(runProject);
                }
            }else {
                runResult.setRunExperimentResult(RunExperimentResult.ParamNull);
                runResult.setProject(null);
                return runResult;
            }
            runResult.setRunExperimentResult(RunExperimentResult.Normal);
            runResult.setProject(runProject);
            return runResult;
        }else{
            runResult.setRunExperimentResult(RunExperimentResult.PathError);
            runResult.setProject(null);
            return runResult;
        }
    }

    /**
     * 获取实验项目
     * @param fileName
     * @param path
     * @return
     */
    private Project getSLANProject(String fileName,String path){
        //打开文件夹
        if(!StringUtil.isEmpty(fileName)){
            ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(path);//获取目录中的项目
            if(null != mConfigProjects && mConfigProjects.size()>0){
                for(Project project :mConfigProjects){
                    if(null != project){
                        if(project.getName().equals(fileName)){
                            //转化为String
                           return project;
                        }
                    }
                }
            }
        }
        return null;
    }



    /**
     * 停止实验
     * //@param serialNo 仪器编号
     * @param block 设备运行模块，每个模块占1位(0：未使用， 1：已使用)（必填）
     */
    public void stopExperiment(Byte block){
        if(BoWeiPCRAsyncTask.getInstance().isRunning()){
            BoWeiPCRAsyncTask.getInstance().setInteruppt(true);
            BoWeiPCRAsyncTask.getInstance().stopMachine();
        }else{
            BoWeiPCRAsyncTask.getInstance().setInteruppt(true);
            BoWeiPCRAsyncTask.getInstance().stopMachine();
        }
        runProject = null;
    }

    /**
     * 获取实验结果
     * @return
     */
    public Experiment getExperimentResult(){
        if(null != runProject){
            Experiment.getInstance().setProject(runProject);
            Experiment experiment =  Experiment.getInstance();
            return experiment;
        }
        return null;
    }

    /*
     * 获取实验参数
     * @param experiment 博偉实验项目对象
     * @return 实验参数列表
     */
    public ArrayList<AnalyseParamter> getExperimentParamter(Experiment experiment){
        ArrayList<AnalyseParamter> analyseParamters = new ArrayList<AnalyseParamter>();
        if(null == experiment){
            return null;
        }
        //获取运行的项目
        Project project = experiment.getProject();
        if(null != project){
            AnalyseParamter analyseParamter =
                    new AnalyseParamter(project.getName(),
                            project.getType(),
                            project.getAMPAlgorithm(),
                            project.getTargets(),
                            project.getCorssTalk());
            analyseParamters.add(analyseParamter);
            return analyseParamters;
        }else{
            return null;
        }
    }


      /**
     * 设置实验参数
     * @param sourceExperiment 需要设置实验参数的博维实验结果
     * @param parameter 实验参数
     * @return
     */
    public Experiment setExperimentParameter(Experiment sourceExperiment,
                                            List<PeaktypeOptimization> optimizations,
                                            AnalyseParamter parameter){
        Experiment experiment = sourceExperiment;
        if(null == sourceExperiment){
            return null;
        }
        Project project = sourceExperiment.getProject();
        if(null != project){
            if(null != parameter){
                ArrayList<Target> inputTargets = parameter.getTargets();
                project.setTargets(inputTargets);
                experiment.setProject(project);
                if(null != optimizations && optimizations.size()>0){
                    for(PeaktypeOptimization optimization : optimizations){
                        if(optimization.getDyeName().equals("FAM")){
                            experiment.setOptimization_enabl_FAM(optimization.isOptimization_enable());
                            experiment.setEnhance_lower_FAM(optimization.getEnhance_lower());
                            experiment.setEnhance_upper_FAM(optimization.getEnhance_upper());
                            experiment.setEnhance_gain_FAM(optimization.getEnhanceGain());
                            experiment.setWeaken_FAM(optimization.getWeaken());
                            experiment.setWeaken_gain_FAM(optimization.getWeakenGain());
                            experiment.setSlope_thres_FAM(optimization.getSlopeThreshold());
                        }else if(optimization.getDyeName().equals("HEX")){
                            experiment.setOptimization_enabl_HEX(optimization.isOptimization_enable());
                            experiment.setEnhance_lower_HEX(optimization.getEnhance_lower());
                            experiment.setEnhance_upper_HEX(optimization.getEnhance_upper());
                            experiment.setEnhance_gain_HEX(optimization.getEnhanceGain());
                            experiment.setWeaken_HEX(optimization.getWeaken());
                            experiment.setWeaken_gain_HEX(optimization.getWeakenGain());
                            experiment.setSlope_thres_HEX(optimization.getSlopeThreshold());
                        }else if(optimization.getDyeName().equals("ROX")){
                            experiment.setOptimization_enabl_ROX(optimization.isOptimization_enable());
                            experiment.setEnhance_lower_ROX(optimization.getEnhance_lower());
                            experiment.setEnhance_upper_ROX(optimization.getEnhance_upper());
                            experiment.setEnhance_gain_ROX(optimization.getEnhanceGain());
                            experiment.setWeaken_ROX(optimization.getWeaken());
                            experiment.setWeaken_gain_ROX(optimization.getWeakenGain());
                            experiment.setSlope_thres_ROX(optimization.getSlopeThreshold());
                        }else if(optimization.getDyeName().equals("CY5")){
                            experiment.setOptimization_enabl_CY5(optimization.isOptimization_enable());
                            experiment.setEnhance_lower_CY5(optimization.getEnhance_lower());
                            experiment.setEnhance_upper_CY5(optimization.getEnhance_upper());
                            experiment.setEnhance_gain_CY5(optimization.getEnhanceGain());
                            experiment.setWeaken_CY5(optimization.getWeaken());
                            experiment.setWeaken_gain_CY5(optimization.getWeakenGain());
                            experiment.setSlope_thres_CY5(optimization.getSlopeThreshold());
                        }
                    }
                }
                BoweiJni.analysisExperimentLoessFilter(experiment);
            }
            return experiment;
        }else {
            return null;
        }

    }



    /**
     * 创建一个新项目
     * @param projectObj
     * @return
     */
    public Project createNewProgect(JSONObject projectObj){
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                // 处理 NaN 和 Infinity，避免生成非法 JSON
                if (src == null || Double.isNaN(src) || Double.isInfinite(src)) {
                    return new JsonPrimitive(0);
                }
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        Project newProject = new Project();
        try {
            String name = projectObj.getString("Name");
            newProject.setName(name);

            String note = projectObj.getString("Note");
            newProject.setNote(note);

            int type= projectObj.getInt("Type");
            newProject.setType(type);

            int AMPAlgorithm= projectObj.getInt("AMPAlgorithm");
            newProject.setAMPAlgorithm(AMPAlgorithm);

            int reaction = projectObj.getInt("Reaction");
            newProject.setReaction(reaction);

            boolean TubeSortByColumn = projectObj.getBoolean("TubeSortByColumn");
            newProject.setTubeSortByColumn(TubeSortByColumn);

           /* JSONArray CorssTalkArray = projectObj.getJSONArray("CorssTalk");
            Type CorssTalkType = new TypeToken<List<ArrayList< Double>>>() {}.getType();
            ArrayList< ArrayList< Double>> CorssTalk = gson.fromJson(CorssTalkArray.toString(),CorssTalkType);
            newProject.setCorssTalk(CorssTalk);*/

            JSONObject CorssTalkObj = projectObj.optJSONObject("CorssTalk");
            if(CorssTalkObj != null) {
                JSONArray CorssTalkArray = projectObj.getJSONArray("CorssTalk");
                if (CorssTalkArray != null) {
                    Type CorssTalkType = new TypeToken<List<ArrayList<Double>>>() {
                    }.getType();
                    ArrayList<ArrayList<Double>> CorssTalk = gson.fromJson(CorssTalkArray.toString(), CorssTalkType);
                    newProject.setCorssTalk(CorssTalk);
                }
            }

            ArrayList<Target> Targets = new ArrayList<Target>();
            JSONArray targetArray = projectObj.getJSONArray("Targets");
            //定性定量、熔解曲线

            for(int i= 0;i<targetArray.length();i++){
                Target target = new Target();
                JSONObject targetObj = targetArray.getJSONObject(i);

                String targetName = targetObj.getString("Name");
                int tubeNo = targetObj.getInt("TubeNo");
                String tubeName = targetObj.getString("TubeName");
                int channelNo = targetObj.getInt("ChannelNo");
                int targetIndex = targetObj.getInt("TargetIndex");
                String dye = targetObj.getString("Dye");
                String color = targetObj.getString("Color");
                target.setName(targetName);
                target.setTubeNo(tubeNo);
                target.setTubeName(tubeName);
                target.setChannelNo(channelNo);
                target.setTargetIndex(targetIndex);
                target.setDye(dye);
                target.setColor(color);

                JSONObject paramterObj = targetObj.getJSONObject("Paramter");
                if(paramterObj.has("MeltPeakAlgorithm")){
                    TargetParamter targetParamterMelt = gson.fromJson(paramterObj.toString(),TargetParamterMelt.class);
                    target.setParamter(targetParamterMelt);
                }else{
                    TargetParamter targetParamter = gson.fromJson(paramterObj.toString(),TargetParamter.class);
                    target.setParamter(targetParamter);
                }
                Targets.add(target);
            }
            newProject.setTargets(Targets);

            Program newProgram = new Program();
            ArrayList<Section> Sections = new ArrayList<Section>();
            ArrayList<SectionAMP> SectionAMPs = new ArrayList<SectionAMP>();
            ArrayList<SectionMelt> SectionMelts = new ArrayList<SectionMelt>();
            newProgram.setSectionMelts(SectionMelts);
            newProgram.setSectionAMPs(SectionAMPs);
            JSONObject programObj = projectObj.getJSONObject("Program");
            JSONArray Array = programObj.getJSONArray("Sections");
            for (int i = 0; i < Array.length(); i++) {
                JSONObject arrayObj = Array.getJSONObject(i);
                if(arrayObj.has("StartTemp")){
                    //熔解
                    SectionMelt sectionMelt = gson.fromJson(arrayObj.toString(),SectionMelt.class);
                    Sections.add(sectionMelt);
                }else{
                    SectionAMP sectionAmp = gson.fromJson(arrayObj.toString(),SectionAMP.class);
                    Sections.add(sectionAmp);
                }
            }
            newProgram.setSections(Sections);
            newProject.setProgram(newProgram);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return newProject;
    }


}
