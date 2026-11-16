package com.gidi.bio_pcr.slan;

import android.os.AsyncTask;
import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.CtRuleConclusion;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.PeakRuleConclusion;
import com.gidi.bio_pcr.bean.Threshold;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.db.SLANWellResult;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hongshi.slansdk.model.experiment.Experiment;
import com.hongshi.slansdk.model.result.TargetResult;
import com.hongshi.slansdk.model.result.WellResult;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SaveSLANExAsyncTask extends AsyncTask<Experiment,Long,Long> {
    private Experiment mExperiment;
    private OnSaveExperimentListener onSaveExperimentListener;
    private List<CtRuleConclusion> ct_conclusions = new ArrayList<CtRuleConclusion>();
    private List<PeakRuleConclusion> peak_conclusions = new ArrayList<PeakRuleConclusion>();
    private String startTime,endTime;
    private String testItem;
    private String templateDir;

    private String description;
    private boolean isMelt;
    private String filePath;


    private Map<String,Threshold> thresholdMaps;

    public void setOnSaveExperimentListener(OnSaveExperimentListener listener){
        this.onSaveExperimentListener = listener;
    }

    /**
     *
     * @param isMelt
     * @param thresholds
     * @param experiment
     * @param startTime
     * @param testItem
     * @param ct_conclusions
     */
    public SaveSLANExAsyncTask(boolean isMelt,
                               Map<String, Threshold> thresholds,
                               Experiment experiment,
                               String filePath,
                               String startTime,
                               String endTime,
                               String testItem,
                               String description,
                               List<CtRuleConclusion> ct_conclusions,
                               List<PeakRuleConclusion> peak_conclusions){
        this.mExperiment = experiment;
        this.filePath = filePath;
        this.ct_conclusions = ct_conclusions;
        this.peak_conclusions = peak_conclusions;
        this.templateDir = testItem;
        this.startTime = startTime;
        this.endTime = endTime;
        this.testItem = testItem;
        this.description = description;
        this.isMelt = isMelt;
        this.thresholdMaps = thresholds;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected void onCancelled(Long result) {
        super.onCancelled(result);
    }

    @Override
    protected Long doInBackground(Experiment... experiments) {
        Long result = saveExperimentToDB(1,isMelt,thresholdMaps,mExperiment,filePath,
                startTime,endTime,testItem,templateDir,description,ct_conclusions,peak_conclusions);
        return result;
    }

    @Override
    protected void onPostExecute(Long result) {
        super.onPostExecute(result);
        if(null != onSaveExperimentListener){
            Log.i("ABC","保存数据库结果result" + result);
            if(result>0){
                onSaveExperimentListener.onSaveSuccess();
            }else{
                onSaveExperimentListener.onSaveFail();
            }
        }
    }


    /**
     *
     * @param type
     * @param experiment
     * @param startTime
     * @param testItem
     * @param templateDir
     * @return
     */

    private long saveSLANExperimentResult(int type,
                                          Experiment experiment,
                                          String filePath,
                                          String startTime,
                                          String endTime,
                                          String testItem,
                                          String templateDir,
                                          String description){
        Log.i("SLAN","保存实验结果模板" + testItem);
        ExperimentModel experimentModel = new ExperimentModel();
        experimentModel.setName(experiment.getName());
        experimentModel.setAuditor(experiment.getAuditor());
        experimentModel.setBatchNo(experiment.getBatchNo());
        //开始时间也不对，如果有两次PCR，PCR的experiment的开始时间是第二次的
        experimentModel.setStartTime(startTime);
        experimentModel.setEndTime(endTime);
        experimentModel.setNote(experiment.getNote());
        experimentModel.setOperator(experiment.getOperator());
        experimentModel.setDeviceModel(experiment.getDeviceModel());
        experimentModel.setSerialNumber(experiment.getSerialNo());
        experimentModel.setProjectName(experiment.getName());
        experimentModel.setTestItem(testItem);
        experimentModel.setType(PcrApplication.experimentType);
        experimentModel.setTemplateDirName(templateDir);
        experimentModel.setDescription(description);
        experimentModel.setExpType(type);//对应的是丁智还是宏石
        experimentModel.setFilePath(filePath);
        return LiteORMCtrlMgr.getInstance().insertExperiment(experimentModel);
    }

    /**
     *
     * @param type
     * @param isMelt 定性定量，熔解曲线
     * @param experiment
     * @param filePath 保存pcr文件的文件夹路径
     * @param startTime
     * @param testItem
     * @param templateDir
     * @param conclusions
     * @return
     */
    public long saveExperimentToDB(int type,
                                   boolean isMelt,
                                   Map<String,Threshold>thresholdMaps,
                                   Experiment experiment,
                                   String filePath,
                                   String startTime,
                                   String endTime,
                                   String testItem,
                                   String templateDir,
                                   String description,
                                   List<CtRuleConclusion> conclusions,
                                   List<PeakRuleConclusion> peak_conclusions){
        long ex_result = saveSLANExperimentResult(type,experiment,filePath,startTime,endTime,testItem,templateDir,description);
        Log.i("slanFinish","==saveExperimentToDB保存项目结果 Experiment" + ex_result);
        long index = 0;
        //获取所有的反应孔的信息
        List<WellResult> wellResults = experiment.getWellResult();
       // Log.i("SLAN","保存项目结果 wellResults 数量" + wellResults.size());
        if(null != wellResults && wellResults.size()>0){
            //开始提取反应孔的信息
            for(int i=0;i<wellResults.size();i++){
               // Log.i("SLAN","保存项目结果 conclusions " + conclusions.size());
                String well_id = wellResults.get(i).getID();//反应孔标识
                PeakRuleConclusion peakConclusion = getSinglePeakConclusion(peak_conclusions,well_id);
                CtRuleConclusion ctConclusion = getSingleCtConclusion(ct_conclusions,well_id);
                if(null != peakConclusion && null != ctConclusion){
                    boolean result = saveSLANWellResultToDBByAll(isMelt,thresholdMaps,experiment,wellResults.get(i),startTime,testItem,
                            ctConclusion.getConlusionMap(),peakConclusion.getConlusionMap(),
                            ctConclusion.getWellConclusion(),peakConclusion.getWellConclusion());
                    if(result){
                        index++;
                    }
                }else if(null != peakConclusion && null == ctConclusion){
                    boolean result = saveSLANWellResultToDBByAll(isMelt,thresholdMaps,experiment,wellResults.get(i),startTime,testItem,
                            null,peakConclusion.getConlusionMap(),null,peakConclusion.getWellConclusion());
                    if(result){
                        index++;
                    }
                }else if(null == peakConclusion && null != ctConclusion){
                    boolean result = saveSLANWellResultToDBByCt(isMelt,thresholdMaps,experiment,wellResults.get(i),startTime,testItem,
                            ctConclusion.getConlusionMap(),ctConclusion.getWellConclusion());
                    if(result){
                        index++;
                    }
                }else {
                    boolean result = saveSLANWellResultToDBByAll(isMelt,thresholdMaps,experiment,wellResults.get(i),startTime,testItem,null,null,null,null );
                    if(result){
                        index++;
                    }
                }
            }
        }
        Log.i("SLAN","保存项目结果 wellindex" + index);
        return index;
    }


    /**
     * 每一个反应孔的信息
     * @param wellResult
     * @param isMelt:定性定量，熔解曲线
     */
    private boolean saveSLANWellResultToDBByCt(boolean isMelt,
                                               Map<String,Threshold>thresholdMap,
                                               Experiment experiment,
                                               WellResult wellResult,
                                               String startTime,
                                               String testItem,
                                               Map<String,List<CtResult>> conclusionMap,
                                               List<String>well_ct_conclusion){
        //样本的信息

        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        SLANWellResult slanWellResult = new SLANWellResult();
        slanWellResult.setReagentCode(wellResult.getTargetResults().get(0).getTubeName());
        slanWellResult.setSampleName(wellResult.getSampleName());//样本名称,身份证号码
        slanWellResult.setTestItem(testItem);
        slanWellResult.setMelt(isMelt);
        slanWellResult.setStartTime(startTime);
        slanWellResult.setExperimentName(experiment.getName());//模板名称
        //样本的反应结果
        slanWellResult.setWellName(wellResult.getID());
        //转化成String
        String ct_conclusion = "";
        if(well_ct_conclusion != null && well_ct_conclusion.size()>0){
            ct_conclusion = gson.toJson(well_ct_conclusion);
        }
        slanWellResult.setWellConclusion(ct_conclusion);

        List<TargetResult> targetResults = wellResult.getTargetResults();//目标结果
        if(targetResults != null && targetResults.size()>0){

            for(int i=0;i<targetResults.size();i++){
                TargetResult result = (TargetResult)targetResults.get(i);
                if(result.getChannel()== 1){
                    //Channel = 1==FAM
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> famResultList  = conclusionMap.get(result.getName());
                        if(famResultList != null && famResultList.size()>0){
                            String famResults = gson.toJson(famResultList);
                            slanWellResult.setFamResults(famResults);
                        }else {
                            String famResults = "";
                            slanWellResult.setFamResults(famResults);
                        }

                    }
                    String famTargetResults = gson.toJson(result);
                    slanWellResult.setFamTargetResults(famTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("FAM")){
                            Threshold famThreshold = thresholdMap.get("FAM");
                            slanWellResult.setFamThreshold(String.valueOf(famThreshold.getThreshold()));
                            slanWellResult.setFamPeekThreshold(String.valueOf(famThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("fam")){
                            Threshold famThreshold = thresholdMap.get("fam");
                            slanWellResult.setFamThreshold(String.valueOf(famThreshold.getThreshold()));
                            slanWellResult.setFamPeekThreshold(String.valueOf(famThreshold.getPeekHighThreshold()));
                        }
                    }

                } else if(result.getChannel()== 2){
                    //Channel = 2==HEX
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> hexResultList  = conclusionMap.get(result.getName());
                        if(hexResultList.size()>0){
                            String hexResults = gson.toJson(hexResultList);
                            slanWellResult.setHexResults(hexResults);
                        }else{
                            String hexResults = "";
                            slanWellResult.setHexResults(hexResults);
                        }
                    }
                    String hexTargetResults = gson.toJson(result);
                    slanWellResult.setHexTargetResults(hexTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("HEX")){
                            Threshold hexThreshold = thresholdMap.get("HEX");
                            slanWellResult.setHexThreshold(String.valueOf(hexThreshold.getThreshold()));
                            slanWellResult.setHexPeekThreshold(String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("hex")){
                            Threshold hexThreshold = thresholdMap.get("hex");
                            slanWellResult.setHexThreshold(String.valueOf(hexThreshold.getThreshold()));
                            slanWellResult.setHexPeekThreshold(String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }
                    }

                }else if(result.getChannel() == 3){
                    //Channel = 3==ROX
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> roxResultList  = conclusionMap.get(result.getName());
                        if(roxResultList != null && roxResultList.size()>0){
                            String roxResults = gson.toJson(roxResultList);
                            slanWellResult.setRoxResults(roxResults);
                        }else{
                            String roxResults = "";
                            slanWellResult.setRoxResults(roxResults);
                        }

                    }
                    String roxTargetResults = gson.toJson(result);
                    slanWellResult.setRoxTargetResults(roxTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("ROX")){
                            Threshold roxThreshold = thresholdMap.get("ROX");
                            slanWellResult.setRoxThreshold(String.valueOf(roxThreshold.getThreshold()));
                            slanWellResult.setRoxPeekThreshold(String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("rox")){
                            Threshold roxThreshold = thresholdMap.get("rox");
                            slanWellResult.setRoxThreshold(String.valueOf(roxThreshold));
                            slanWellResult.setRoxPeekThreshold(String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }
                    }

                }else if(result.getChannel() == 4){
                    //Channel = 4==CY5
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> cy5ResultList  = conclusionMap.get(result.getName());
                        if(null != cy5ResultList && cy5ResultList.size()>0){
                            String cy5Results = gson.toJson(cy5ResultList);
                            slanWellResult.setCy5Results(cy5Results);
                        }else{
                            String cy5Results = "";
                            slanWellResult.setCy5Results(cy5Results);
                        }

                    }

                    String cy5TargetResults = gson.toJson(result);
                    slanWellResult.setCy5TargetResults(cy5TargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("CY5")){
                            Threshold cy5Threshold = thresholdMap.get("CY5");
                            slanWellResult.setCy5Threshold(String.valueOf(cy5Threshold.getThreshold()));
                            slanWellResult.setCy5PeekThreshold(String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("cy5")){
                            Threshold cy5Threshold = thresholdMap.get("cy5");
                            slanWellResult.setCy5Threshold(String.valueOf(cy5Threshold.getThreshold()));
                            slanWellResult.setCy5PeekThreshold(String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }
                    }
                }else if(result.getChannel() == 5){
                    //Channel = 4==CY5
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> quaResultList  = conclusionMap.get(result.getName());
                        if(null != quaResultList && quaResultList.size()>0){
                            String quaResults = gson.toJson(quaResultList);
                            slanWellResult.setQuasarResults(quaResults);
                        }else{
                            String quaResults = "";
                            slanWellResult.setQuasarResults(quaResults);
                        }

                    }

                    String quasarTargetResults = gson.toJson(result);
                    slanWellResult.setQuasarTargetResults(quasarTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("QUASAR")){
                            Threshold quasarThreshold = thresholdMap.get("QUASAR");
                            slanWellResult.setQuasarThreshold(String.valueOf(quasarThreshold.getThreshold()));
                            slanWellResult.setQuasarPeekThreshold(String.valueOf(quasarThreshold.getPeekHighThreshold()));
                        }
                    }
                }else if(result.getChannel() == 6){
                    //Channel = 4==CY5
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> attoResultList  = conclusionMap.get(result.getName());
                        if(null != attoResultList && attoResultList.size()>0){
                            String attoResults = gson.toJson(attoResultList);
                            slanWellResult.setAttoResults(attoResults);
                        }else{
                            String attoResults = "";
                            slanWellResult.setAttoResults(attoResults);
                        }

                    }

                    String attoTargetResults = gson.toJson(result);
                    slanWellResult.setAttoTargetResults(attoTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("ATTO")){
                            Threshold attoThreshold = thresholdMap.get("ATTO");
                            slanWellResult.setAttoThreshold(String.valueOf(attoThreshold.getThreshold()));
                            slanWellResult.setAttoPeekThreshold(String.valueOf(attoThreshold.getPeekHighThreshold()));
                        }
                    }
                }

            }
        }

        long save_well_result = LiteORMCtrlMgr.getInstance().insertSLANWellResult(slanWellResult);
        Log.i("SLAN", "save_well_result" + save_well_result);
        if(save_well_result > 0){
           return  true;
        }
        return false;
    }

    /**
     * 每一个反应孔的信息
     * @param wellResult
     * @param isMelt:定性定量，熔解曲线
     */
    private boolean saveSLANWellResultToDBByAll(boolean isMelt,
                                                Map<String,Threshold>thresholdMap,
                                                Experiment experiment,
                                                WellResult wellResult,
                                                String startTime,
                                                String testItem,
                                                Map<String,List<CtResult>> ct_conclusionMap,
                                                Map<String,List<PeakResult>> peak_conclusionMap,
                                                List<String> well_ct_conclusion,
                                                List<String> well_peak_conclusion){
        //样本的信息

        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        SLANWellResult slanWellResult = new SLANWellResult();
        slanWellResult.setReagentCode(wellResult.getTargetResults().get(0).getTubeName());
        slanWellResult.setSampleName(wellResult.getSampleName());//样本名称,身份证号码
        slanWellResult.setTestItem(testItem);
        slanWellResult.setMelt(isMelt);
        slanWellResult.setStartTime(startTime);
        slanWellResult.setExperimentName(experiment.getName());//模板名称
        //样本的反应结果
        slanWellResult.setWellName(wellResult.getID());
        //转化成String
        String ct_conclusion = "";
        if(well_ct_conclusion != null&& well_ct_conclusion.size()>0){
            ct_conclusion = gson.toJson(well_ct_conclusion);
        }
        slanWellResult.setWellConclusion(ct_conclusion);
        String peak_conclusion = "";
        if(well_peak_conclusion != null&& well_peak_conclusion.size()>0){
            peak_conclusion = gson.toJson(peak_conclusion);
        }
        slanWellResult.setPeakWellConclusion(peak_conclusion);

        List<TargetResult> targetResults = wellResult.getTargetResults();//目标结果
        if(targetResults != null && targetResults.size()>0){

            for(int i=0;i<targetResults.size();i++){
                TargetResult result = (TargetResult)targetResults.get(i);
                if(result.getChannel()== 1){
                    //Channel = 1==FAM
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> famResultList  = ct_conclusionMap.get(result.getName());
                            if(famResultList != null && famResultList.size()>0){
                                String famResults = gson.toJson(famResultList);
                                slanWellResult.setFamResults(famResults);
                            }else {
                                String famResults = "";
                                slanWellResult.setFamResults(famResults);
                            }
                        }
                    }else{
                        slanWellResult.setFamResults("");
                    }
                    //fam peak结论
                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> famPeekResultList  = peak_conclusionMap.get(result.getName());
                            if(famPeekResultList != null && famPeekResultList.size()>0){
                                String famPeakResults = gson.toJson(famPeekResultList);
                                slanWellResult.setFamPeakResults(famPeakResults);
                            }else {
                                String famPeakResults = "";
                                slanWellResult.setFamPeakResults(famPeakResults);
                            }
                        }
                    }else{
                        String famPeakResults = "";
                        slanWellResult.setFamPeakResults(famPeakResults);
                    }
                    String famTargetResults = gson.toJson(result);
                    slanWellResult.setFamTargetResults(famTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("FAM")){
                            Threshold famThreshold = thresholdMap.get("FAM");
                            slanWellResult.setFamThreshold(String.valueOf(famThreshold.getThreshold()));
                            slanWellResult.setFamPeekThreshold(String.valueOf(famThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("fam")){
                            Threshold famThreshold = thresholdMap.get("fam");
                            slanWellResult.setFamThreshold(String.valueOf(famThreshold.getThreshold()));
                            slanWellResult.setFamPeekThreshold(String.valueOf(famThreshold.getPeekHighThreshold()));
                        }
                    }

                } else if(result.getChannel()== 2){
                    //Channel = 2==HEX
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> hexResultList  = ct_conclusionMap.get(result.getName());
                            if(hexResultList.size()>0){
                                String hexResults = gson.toJson(hexResultList);
                                slanWellResult.setHexResults(hexResults);
                            }else{
                                String hexResults = "";
                                slanWellResult.setHexResults(hexResults);
                            }
                        }
                    }else{
                        String hexResults = "";
                        slanWellResult.setHexResults(hexResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> hexPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(hexPeakResultList.size()>0){
                                String hexPeakResults = gson.toJson(hexPeakResultList);
                                slanWellResult.setHexPeakResults(hexPeakResults);
                            }else{
                                String hexPeakResults = "";
                                slanWellResult.setHexPeakResults(hexPeakResults);
                            }
                        }
                    }else{
                        String hexPeakResults = "";
                        slanWellResult.setHexPeakResults(hexPeakResults);
                    }
                    String hexTargetResults = gson.toJson(result);
                    slanWellResult.setHexTargetResults(hexTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("HEX")){
                            Threshold hexThreshold = thresholdMap.get("HEX");
                            slanWellResult.setHexThreshold(String.valueOf(hexThreshold.getThreshold()));
                            slanWellResult.setHexPeekThreshold(String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("hex")){
                            Threshold hexThreshold = thresholdMap.get("hex");
                            slanWellResult.setHexThreshold(String.valueOf(hexThreshold.getThreshold()));
                            slanWellResult.setHexPeekThreshold(String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }
                    }

                }else if(result.getChannel() == 3){
                    //Channel = 3==ROX
                    if(ct_conclusionMap != null){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> roxResultList  = ct_conclusionMap.get(result.getName());
                            if(roxResultList != null && roxResultList.size()>0){
                                String roxResults = gson.toJson(roxResultList);
                                slanWellResult.setRoxResults(roxResults);
                            }else{
                                String roxResults = "";
                                slanWellResult.setRoxResults(roxResults);
                            }
                        }
                    }else {
                        String roxResults = "";
                        slanWellResult.setRoxResults(roxResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> roxPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(roxPeakResultList != null && roxPeakResultList.size()>0){
                                String roxPeakResults = gson.toJson(roxPeakResultList);
                                slanWellResult.setRoxPeakResults(roxPeakResults);
                            }else{
                                String roxPeakResults = "";
                                slanWellResult.setRoxPeakResults(roxPeakResults);
                            }
                        }
                    }else{
                        String roxPeakResults = "";
                        slanWellResult.setRoxPeakResults(roxPeakResults);
                    }
                    String roxTargetResults = gson.toJson(result);
                    slanWellResult.setRoxTargetResults(roxTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("ROX")){
                            Threshold roxThreshold = thresholdMap.get("ROX");
                            slanWellResult.setRoxThreshold(String.valueOf(roxThreshold.getThreshold()));
                            slanWellResult.setRoxPeekThreshold(String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("rox")){
                            Threshold roxThreshold = thresholdMap.get("rox");
                            slanWellResult.setRoxThreshold(String.valueOf(roxThreshold));
                            slanWellResult.setRoxPeekThreshold(String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }
                    }

                }else if(result.getChannel() == 4){
                    //Channel = 4==CY5
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> cy5ResultList  = ct_conclusionMap.get(result.getName());
                            if(null != cy5ResultList && cy5ResultList.size()>0){
                                String cy5Results = gson.toJson(cy5ResultList);
                                slanWellResult.setCy5Results(cy5Results);
                            }else{
                                String cy5Results = "";
                                slanWellResult.setCy5Results(cy5Results);
                            }
                        }
                    }else{
                        String cy5Results = "";
                        slanWellResult.setCy5Results(cy5Results);
                    }
                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> cy5PeakResultList  = peak_conclusionMap.get(result.getName());
                            if(cy5PeakResultList != null && cy5PeakResultList.size()>0){
                                String cy5PeakResults = gson.toJson(cy5PeakResultList);
                                slanWellResult.setCy5PeakResults(cy5PeakResults);
                            }else{
                                String cy5PeakResults = "";
                                slanWellResult.setCy5PeakResults(cy5PeakResults);
                            }
                        }
                    }else{
                        String cy5PeakResults = "";
                        slanWellResult.setCy5PeakResults(cy5PeakResults);
                    }

                    String cy5TargetResults = gson.toJson(result);
                    slanWellResult.setCy5TargetResults(cy5TargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("CY5")){
                            Threshold cy5Threshold = thresholdMap.get("CY5");
                            slanWellResult.setCy5Threshold(String.valueOf(cy5Threshold.getThreshold()));
                            slanWellResult.setCy5PeekThreshold(String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("cy5")){
                            Threshold cy5Threshold = thresholdMap.get("cy5");
                            slanWellResult.setCy5Threshold(String.valueOf(cy5Threshold.getThreshold()));
                            slanWellResult.setCy5PeekThreshold(String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }
                    }
                }else if(result.getChannel() == 5){
                    //Channel = 5==QUASAR
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> quaResultList  = ct_conclusionMap.get(result.getName());
                            if(null != quaResultList && quaResultList.size()>0){
                                String quaResults = gson.toJson(quaResultList);
                                slanWellResult.setQuasarResults(quaResults);
                            }else{
                                String quaResults = "";
                                slanWellResult.setQuasarResults(quaResults);
                            }
                        }
                    }else {
                        String quaResults = "";
                        slanWellResult.setQuasarResults(quaResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> quasarPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(quasarPeakResultList != null && quasarPeakResultList.size()>0){
                                String quasarPeakResults = gson.toJson(quasarPeakResultList);
                                slanWellResult.setQuasarPeakResults(quasarPeakResults);
                            }else{
                                String quasarPeakResults = "";
                                slanWellResult.setQuasarPeakResults(quasarPeakResults);
                            }
                        }
                    }else {
                        String quasarPeakResults = "";
                        slanWellResult.setQuasarPeakResults(quasarPeakResults);
                    }

                    String quasarTargetResults = gson.toJson(result);
                    slanWellResult.setQuasarTargetResults(quasarTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("QUASAR")){
                            Threshold quasarThreshold = thresholdMap.get("QUASAR");
                            slanWellResult.setQuasarThreshold(String.valueOf(quasarThreshold.getThreshold()));
                            slanWellResult.setQuasarPeekThreshold(String.valueOf(quasarThreshold.getPeekHighThreshold()));
                        }
                    }
                }else if(result.getChannel() == 6){
                    //Channel = 6=ATTO
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> attoResultList  = ct_conclusionMap.get(result.getName());
                            if(null != attoResultList && attoResultList.size()>0){
                                String attoResults = gson.toJson(attoResultList);
                                slanWellResult.setAttoResults(attoResults);
                            }else{
                                String attoResults = "";
                                slanWellResult.setAttoResults(attoResults);
                            }
                        }
                    }else{
                        String attoResults = "";
                        slanWellResult.setAttoResults(attoResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> attoPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(attoPeakResultList != null && attoPeakResultList.size()>0){
                                String attoPeakResults = gson.toJson(attoPeakResultList);
                                slanWellResult.setAttoPeakResults(attoPeakResults);
                            }else{
                                String attoPeakResults = "";
                                slanWellResult.setAttoPeakResults(attoPeakResults);
                            }
                        }
                    }else{
                        String attoPeakResults = "";
                        slanWellResult.setQuasarPeakResults(attoPeakResults);
                    }

                    String attoTargetResults = gson.toJson(result);
                    slanWellResult.setAttoTargetResults(attoTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("ATTO")){
                            Threshold attoThreshold = thresholdMap.get("ATTO");
                            slanWellResult.setAttoThreshold(String.valueOf(attoThreshold.getThreshold()));
                            slanWellResult.setAttoPeekThreshold(String.valueOf(attoThreshold.getPeekHighThreshold()));
                        }
                    }
                }

            }
        }

        long save_well_result = LiteORMCtrlMgr.getInstance().insertSLANWellResult(slanWellResult);
        Log.i("SLAN", "save_well_result" + save_well_result);
        if(save_well_result > 0){
            return  true;
        }
        return false;
    }

    private PeakRuleConclusion getSinglePeakConclusion(List<PeakRuleConclusion> peak_conclusions,
                                               String wellResult_id) {
        if(null != peak_conclusions && peak_conclusions.size() > 0) {
            for (PeakRuleConclusion conclusion : peak_conclusions) {
                String conclusionWell = conclusion.getTupeNo();//getWellNum孔号
                if (conclusionWell.equals(wellResult_id)) {
                    return conclusion;
                }
            }
        }
        return null;
    }

    private CtRuleConclusion getSingleCtConclusion(List<CtRuleConclusion> ct_conclusions,
                                             String wellResult_id) {
        if(null != ct_conclusions && ct_conclusions.size() > 0) {
            for (CtRuleConclusion conclusion : ct_conclusions) {
                String conclusionWell = conclusion.getTupeNo();
                if (conclusionWell.equals(wellResult_id)) {
                    return conclusion;
                }
            }
        }
        return null;
    }
}
