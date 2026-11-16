package com.gidi.bio_pcr.bioway.asynctask;

import android.os.AsyncTask;
import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.CtRuleConclusion;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.PeakRuleConclusion;
import com.gidi.bio_pcr.bean.Threshold;
import com.gidi.bio_pcr.db.BioWayWellResult;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.ExperimentData.TargetResult;
import com.winters.boweilibrary.BoWei.ExperimentData.WellResult;


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SaveBioWayExTask extends AsyncTask<Experiment,Long,Long> {
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
    public SaveBioWayExTask(boolean isMelt,
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
        this.ct_conclusions = ct_conclusions;
        this.peak_conclusions = peak_conclusions;
        this.templateDir = testItem;
        this.filePath = filePath;
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
        Long result = saveExperimentToDB(2,isMelt,thresholdMaps,mExperiment, filePath,startTime,endTime,
                testItem,templateDir,description,ct_conclusions,peak_conclusions);
        return result;
    }

    @Override
    protected void onPostExecute(Long result) {
        super.onPostExecute(result);
        if(null != onSaveExperimentListener){
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

    private long saveBiowayExperimentResult(int type,
                                            Experiment experiment,
                                            String filePath,
                                            String startTime,
                                            String endTime,
                                            String testItem,
                                            String templateDir,
                                            String description){
        Log.i("BIOWAY","保存实验结果模板" + testItem);
        ExperimentModel experimentModel = new ExperimentModel();
        experimentModel.setName(experiment.getName());
        experimentModel.setAuditor(experiment.getAuditor());
        experimentModel.setBatchNo(experiment.getBatchNo());
        //开始时间也不对，如果有两次PCR，PCR的experiment的开始时间是第二次的
        experimentModel.setStartTime(startTime);
        experimentModel.setEndTime(endTime);
        experimentModel.setNote(experiment.getNote());
        experimentModel.setOperator(experiment.getOperator());
        experimentModel.setDeviceModel("BIOWAY");
        experimentModel.setSerialNumber("");
        experimentModel.setProjectName(experiment.getName());
        experimentModel.setTestItem(testItem);
        experimentModel.setType(PcrApplication.experimentType);
        experimentModel.setTemplateDirName(templateDir);
        experimentModel.setExpType(type);//对应的是丁智还是宏石
        experimentModel.setDescription(description);
        experimentModel.setFilePath(filePath);
        return LiteORMCtrlMgr.getInstance().insertExperiment(experimentModel);
    }

    /**
     *
     * @param type
     * @param isMelt 定性定量，熔解曲线
     * @param experiment
     * @param startTime
     * @param testItem
     * @param templateDir
     * @param ct_conclusions
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
                                   List<CtRuleConclusion> ct_conclusions,
                                   List<PeakRuleConclusion> peak_conclusions){
        long ex_result = saveBiowayExperimentResult(type,experiment,filePath,startTime,endTime,testItem,templateDir,description);
        Log.i("Bioway","保存数据库实验结果saveBiowayExperimentResult--" + ex_result);

        long index = 0;
        //获取所有的反应孔的信息

        List<com.hongshi.slansdk.model.experiment.Target> targets = experiment.getProject().getTargets();

        boolean capture_FAM = false;
        boolean capture_HEX = false;
        boolean capture_ROX = false;
        boolean capture_CY5 = false;
        for(int i=0;i<targets.size();i++){
            if(targets.get(i).getDye().toUpperCase().equals(("FAM"))){
                capture_FAM = true;
            }else if(targets.get(i).getDye().toUpperCase().equals(("HEX"))){
                capture_HEX = true;
            }else if(targets.get(i).getDye().toUpperCase().equals(("ROX"))){
                capture_ROX = true;
            }else if(targets.get(i).getDye().toUpperCase().equals(("CY5"))){
                capture_CY5 = true;
            }
        }

        List<WellResult> wellResults = experiment.getWellResult();
       // Log.i("SLAN","保存实验结果 wellResults 数量" + wellResults.size());

        if(null != wellResults && wellResults.size()>0) {
            // 使用副本过滤，不直接修改原始列表，避免 ConcurrentModificationException
            for (int i = 0; i < wellResults.size(); i++) {

                List<TargetResult> originalTargetResults = wellResults.get(i).getTargetResults();

                if(null != originalTargetResults && originalTargetResults.size()>0) {
                    List<TargetResult> filteredList = new ArrayList<>(originalTargetResults);
                    for (int j = filteredList.size() -1; j >= 0; j--) {
                        String dye = filteredList.get(j).getDye().toUpperCase();
                        if(dye.equals("FAM")){
                            if(!capture_FAM) filteredList.remove(j);
                        }else if(dye.equals("HEX")){
                            if(!capture_HEX) filteredList.remove(j);
                        }else if(dye.equals("ROX")){
                            if(!capture_ROX) filteredList.remove(j);
                        }else if(dye.equals("CY5")){
                            if(!capture_CY5) filteredList.remove(j);
                        }
                    }
                    originalTargetResults.clear();
                    originalTargetResults.addAll(filteredList);
                }
            }
        }

        if(null != wellResults && wellResults.size()>0){
            //开始提取反应孔的信息
            for(int i=0;i<wellResults.size();i++){
               // Log.i("SLAN","保存实验结果 conclusions " + conclusions.size());
                String well_id = wellResults.get(i).getID();//反应孔标识
                PeakRuleConclusion peakConclusion = getSinglePeakConclusion(peak_conclusions,well_id);
                CtRuleConclusion ctConclusion = getSingleCtConclusion(ct_conclusions,well_id);
                if(null != peakConclusion && null != ctConclusion){
                    boolean result = saveBiowayWellResultToDBByAll(isMelt,thresholdMaps,experiment,wellResults.get(i),startTime,testItem,
                            ctConclusion.getConlusionMap(),peakConclusion.getConlusionMap(),
                            ctConclusion.getWellConclusion(),peakConclusion.getWellConclusion());
                    if(result){
                        index++;
                    }
                }else if(null != peakConclusion && null == ctConclusion){
                    boolean result = saveBiowayWellResultToDBByAll(isMelt,thresholdMaps,experiment,wellResults.get(i),startTime,testItem,
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
                    boolean result = saveBiowayWellResultToDBByAll(isMelt,thresholdMaps,experiment,wellResults.get(i),startTime,testItem,null,null,null,null );
                    if(result){
                        index++;
                    }
                }
            }
        }
        Log.i("Bioway","保存实验结果 wellindex" + index);
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
        BioWayWellResult biowayWellResult = new BioWayWellResult();
        biowayWellResult.setReagentCode(wellResult.getTargetResults().get(0).getTubeName());
        biowayWellResult.setSampleName(wellResult.getSampleName());//样本名称,身份证号码
        biowayWellResult.setTestItem(testItem);
        biowayWellResult.setMelt(isMelt);
        biowayWellResult.setStartTime(startTime);
        biowayWellResult.setExperimentName(experiment.getName());//模板名称
        //样本的反应结果
        biowayWellResult.setWellName(wellResult.getID());
        //转化成String
        String ct_conclusion = "";
        if(well_ct_conclusion != null && well_ct_conclusion.size()>0){
            ct_conclusion = gson.toJson(well_ct_conclusion);
        }
        biowayWellResult.setWellConclusion(ct_conclusion);

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
                            biowayWellResult.setFamResults(famResults);
                        }else {
                            String famResults = "";
                            biowayWellResult.setFamResults(famResults);
                        }

                    }
                    String famTargetResults = gson.toJson(result);
                    biowayWellResult.setFamTargetResults(famTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("FAM")){
                            Threshold famThreshold = thresholdMap.get("FAM");
                            biowayWellResult.setFamThreshold(String.valueOf(famThreshold.getThreshold()));
                            biowayWellResult.setFamPeekThreshold(String.valueOf(famThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("fam")){
                            Threshold famThreshold = thresholdMap.get("fam");
                            biowayWellResult.setFamThreshold(String.valueOf(famThreshold.getThreshold()));
                            biowayWellResult.setFamPeekThreshold(String.valueOf(famThreshold.getPeekHighThreshold()));
                        }
                    }

                } else if(result.getChannel()== 2){
                    //Channel = 2==HEX
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> hexResultList  = conclusionMap.get(result.getName());
                        if(hexResultList.size()>0){
                            String hexResults = gson.toJson(hexResultList);
                            biowayWellResult.setHexResults(hexResults);
                        }else{
                            String hexResults = "";
                            biowayWellResult.setHexResults(hexResults);
                        }
                    }
                    String hexTargetResults = gson.toJson(result);
                    biowayWellResult.setHexTargetResults(hexTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("HEX")){
                            Threshold hexThreshold = thresholdMap.get("HEX");
                            biowayWellResult.setHexThreshold(String.valueOf(hexThreshold.getThreshold()));
                            biowayWellResult.setHexPeekThreshold(String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("hex")){
                            Threshold hexThreshold = thresholdMap.get("hex");
                            biowayWellResult.setHexThreshold(String.valueOf(hexThreshold.getThreshold()));
                            biowayWellResult.setHexPeekThreshold(String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }
                    }

                }else if(result.getChannel() == 3){
                    //Channel = 3==ROX
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> roxResultList  = conclusionMap.get(result.getName());
                        if(roxResultList != null && roxResultList.size()>0){
                            String roxResults = gson.toJson(roxResultList);
                            biowayWellResult.setRoxResults(roxResults);
                        }else{
                            String roxResults = "";
                            biowayWellResult.setRoxResults(roxResults);
                        }

                    }
                    String roxTargetResults = gson.toJson(result);
                    biowayWellResult.setRoxTargetResults(roxTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("ROX")){
                            Threshold roxThreshold = thresholdMap.get("ROX");
                            biowayWellResult.setRoxThreshold(String.valueOf(roxThreshold.getThreshold()));
                            biowayWellResult.setRoxPeekThreshold(String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("rox")){
                            Threshold roxThreshold = thresholdMap.get("rox");
                            biowayWellResult.setRoxThreshold(String.valueOf(roxThreshold));
                            biowayWellResult.setRoxPeekThreshold(String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }
                    }

                }else if(result.getChannel() == 4){
                    //Channel = 4==CY5
                    if(conclusionMap.containsKey(result.getName())){
                        List<CtResult> cy5ResultList  = conclusionMap.get(result.getName());
                        if(null != cy5ResultList && cy5ResultList.size()>0){
                            String cy5Results = gson.toJson(cy5ResultList);
                            biowayWellResult.setCy5Results(cy5Results);
                        }else{
                            String cy5Results = "";
                            biowayWellResult.setCy5Results(cy5Results);
                        }

                    }

                    String cy5TargetResults = gson.toJson(result);
                    biowayWellResult.setCy5TargetResults(cy5TargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("CY5")){
                            Threshold cy5Threshold = thresholdMap.get("CY5");
                            biowayWellResult.setCy5Threshold(String.valueOf(cy5Threshold.getThreshold()));
                            biowayWellResult.setCy5PeekThreshold(String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("cy5")){
                            Threshold cy5Threshold = thresholdMap.get("cy5");
                            biowayWellResult.setCy5Threshold(String.valueOf(cy5Threshold.getThreshold()));
                            biowayWellResult.setCy5PeekThreshold(String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }
                    }
                }

            }
        }

        long save_well_result = LiteORMCtrlMgr.getInstance().insertBiowayWellResult(biowayWellResult);
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
    private boolean saveBiowayWellResultToDBByAll(boolean isMelt,
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
        BioWayWellResult biowayWellResult = new BioWayWellResult();
        biowayWellResult.setReagentCode(wellResult.getTargetResults().get(0).getTubeName());
        biowayWellResult.setSampleName(wellResult.getSampleName());//样本名称,身份证号码
        biowayWellResult.setTestItem(testItem);
        biowayWellResult.setMelt(isMelt);
        biowayWellResult.setStartTime(startTime);
        biowayWellResult.setExperimentName(experiment.getName());//模板名称
        //样本的反应结果
        biowayWellResult.setWellName(wellResult.getID());
        //转化成String
        String ct_conclusion = "";
        if(well_ct_conclusion != null&& well_ct_conclusion.size()>0){
            ct_conclusion = gson.toJson(well_ct_conclusion);
        }
        biowayWellResult.setWellConclusion(ct_conclusion);
        String peak_conclusion = "";
        if(well_peak_conclusion != null&& well_peak_conclusion.size()>0){
            peak_conclusion = gson.toJson(peak_conclusion);
        }
        biowayWellResult.setPeakWellConclusion(peak_conclusion);

        List<TargetResult> targetResults = wellResult.getTargetResults();//目标结果
        if(targetResults != null && targetResults.size()>0){

            for(int i=0;i<targetResults.size();i++){
                TargetResult result = (TargetResult)targetResults.get(i);
/*                Log.i("BIOWAY","saveBiowayWellResultToDBByAll---");
                Log.i("BIOWAY","result.getChannel()---" + result.getChannel());
                Log.i("BIOWAY","result.getName`()---" + result.getName());*/
                if(result.getChannel() == 1){
                    //Channel = 1==FAM
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> famResultList  = ct_conclusionMap.get(result.getName());
                            if(famResultList != null && famResultList.size()>0){
                                String famResults = gson.toJson(famResultList);
                                biowayWellResult.setFamResults(famResults);
                            }else {
                                String famResults = "";
                                biowayWellResult.setFamResults(famResults);
                            }
                        }
                    }else{
                        biowayWellResult.setFamResults("");
                    }
                    //fam peak结论
                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> famPeekResultList  = peak_conclusionMap.get(result.getName());
                            if(famPeekResultList != null && famPeekResultList.size()>0){
                                String famPeakResults = gson.toJson(famPeekResultList);
                                biowayWellResult.setFamPeakResults(famPeakResults);
                            }else {
                                String famPeakResults = "";
                                biowayWellResult.setFamPeakResults(famPeakResults);
                            }
                        }
                    }else{
                        String famPeakResults = "";
                        biowayWellResult.setFamPeakResults(famPeakResults);
                    }
                    String famTargetResults = gson.toJson(result);
                    biowayWellResult.setFamTargetResults(famTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("FAM")){
                            Threshold famThreshold = thresholdMap.get("FAM");
                            biowayWellResult.setFamThreshold(String.valueOf(famThreshold.getThreshold()));
                            biowayWellResult.setFamPeekThreshold(String.valueOf(famThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("fam")){
                            Threshold famThreshold = thresholdMap.get("fam");
                            biowayWellResult.setFamThreshold(String.valueOf(famThreshold.getThreshold()));
                            biowayWellResult.setFamPeekThreshold(String.valueOf(famThreshold.getPeekHighThreshold()));
                        }
                    }

                } else if(result.getChannel()== 2){
                    //Channel = 2==HEX
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> hexResultList  = ct_conclusionMap.get(result.getName());
                            if(hexResultList.size()>0){
                                String hexResults = gson.toJson(hexResultList);
                                biowayWellResult.setHexResults(hexResults);
                            }else{
                                String hexResults = "";
                                biowayWellResult.setHexResults(hexResults);
                            }
                        }
                    }else{
                        String hexResults = "";
                        biowayWellResult.setHexResults(hexResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> hexPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(hexPeakResultList.size()>0){
                                String hexPeakResults = gson.toJson(hexPeakResultList);
                                biowayWellResult.setHexPeakResults(hexPeakResults);
                            }else{
                                String hexPeakResults = "";
                                biowayWellResult.setHexPeakResults(hexPeakResults);
                            }
                        }
                    }else{
                        String hexPeakResults = "";
                        biowayWellResult.setHexPeakResults(hexPeakResults);
                    }
                    String hexTargetResults = gson.toJson(result);
                    biowayWellResult.setHexTargetResults(hexTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("HEX")){
                            Threshold hexThreshold = thresholdMap.get("HEX");
                            biowayWellResult.setHexThreshold(String.valueOf(hexThreshold.getThreshold()));
                            biowayWellResult.setHexPeekThreshold(String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("hex")){
                            Threshold hexThreshold = thresholdMap.get("hex");
                            biowayWellResult.setHexThreshold(String.valueOf(hexThreshold.getThreshold()));
                            biowayWellResult.setHexPeekThreshold(String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }
                    }

                }else if(result.getChannel() == 3){
                    //Channel = 3==ROX
                    if(ct_conclusionMap != null){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> roxResultList  = ct_conclusionMap.get(result.getName());
                            if(roxResultList != null && roxResultList.size()>0){
                                String roxResults = gson.toJson(roxResultList);
                                biowayWellResult.setRoxResults(roxResults);
                            }else{
                                String roxResults = "";
                                biowayWellResult.setRoxResults(roxResults);
                            }
                        }
                    }else {
                        String roxResults = "";
                        biowayWellResult.setRoxResults(roxResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> roxPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(roxPeakResultList != null && roxPeakResultList.size()>0){
                                String roxPeakResults = gson.toJson(roxPeakResultList);
                                biowayWellResult.setRoxPeakResults(roxPeakResults);
                            }else{
                                String roxPeakResults = "";
                                biowayWellResult.setRoxPeakResults(roxPeakResults);
                            }
                        }
                    }else{
                        String roxPeakResults = "";
                        biowayWellResult.setRoxPeakResults(roxPeakResults);
                    }
                    String roxTargetResults = gson.toJson(result);
                    biowayWellResult.setRoxTargetResults(roxTargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("ROX")){
                            Threshold roxThreshold = thresholdMap.get("ROX");
                            biowayWellResult.setRoxThreshold(String.valueOf(roxThreshold.getThreshold()));
                            biowayWellResult.setRoxPeekThreshold(String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("rox")){
                            Threshold roxThreshold = thresholdMap.get("rox");
                            biowayWellResult.setRoxThreshold(String.valueOf(roxThreshold));
                            biowayWellResult.setRoxPeekThreshold(String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }
                    }

                }else if(result.getChannel() == 4){
                    //Channel = 4==CY5
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> cy5ResultList  = ct_conclusionMap.get(result.getName());
                            if(null != cy5ResultList && cy5ResultList.size()>0){
                                String cy5Results = gson.toJson(cy5ResultList);
                                biowayWellResult.setCy5Results(cy5Results);
                            }else{
                                String cy5Results = "";
                                biowayWellResult.setCy5Results(cy5Results);
                            }
                        }
                    }else{
                        String cy5Results = "";
                        biowayWellResult.setCy5Results(cy5Results);
                    }
                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> cy5PeakResultList  = peak_conclusionMap.get(result.getName());
                            if(cy5PeakResultList != null && cy5PeakResultList.size()>0){
                                String cy5PeakResults = gson.toJson(cy5PeakResultList);
                                biowayWellResult.setCy5PeakResults(cy5PeakResults);
                            }else{
                                String cy5PeakResults = "";
                                biowayWellResult.setCy5PeakResults(cy5PeakResults);
                            }
                        }
                    }else{
                        String cy5PeakResults = "";
                        biowayWellResult.setCy5PeakResults(cy5PeakResults);
                    }

                    String cy5TargetResults = gson.toJson(result);
                    biowayWellResult.setCy5TargetResults(cy5TargetResults);
                    if(thresholdMap != null && thresholdMap.size()>0){
                        if(thresholdMap.containsKey("CY5")){
                            Threshold cy5Threshold = thresholdMap.get("CY5");
                            biowayWellResult.setCy5Threshold(String.valueOf(cy5Threshold.getThreshold()));
                            biowayWellResult.setCy5PeekThreshold(String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }else if(thresholdMap.containsKey("cy5")){
                            Threshold cy5Threshold = thresholdMap.get("cy5");
                            biowayWellResult.setCy5Threshold(String.valueOf(cy5Threshold.getThreshold()));
                            biowayWellResult.setCy5PeekThreshold(String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }
                    }
                }
            }
        }

        long save_well_result = LiteORMCtrlMgr.getInstance().insertBiowayWellResult(biowayWellResult);
        Log.i("BIOWAY", "save_well_result" + save_well_result);
        if(save_well_result > 0){
            return  true;
        }
        return false;
    }

    private PeakRuleConclusion getSinglePeakConclusion(List<PeakRuleConclusion> peak_conclusions,String wellResult_id) {
        if(null != peak_conclusions && peak_conclusions.size() > 0) {
            for (PeakRuleConclusion conclusion : peak_conclusions) {
                String conclusionWell = conclusion.getTupeNo();//getWellNum
                if (conclusionWell.equals(wellResult_id)) {
                    return conclusion;
                }
            }
        }
        return null;
    }

    private CtRuleConclusion getSingleCtConclusion(List<CtRuleConclusion> ct_conclusions,String wellResult_id) {
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
