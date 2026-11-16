package com.gidi.bio_pcr.bioway.asynctask;

import android.os.AsyncTask;
import android.util.Log;

import com.gidi.bio_pcr.bean.Conclusion;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.CtRuleConclusion;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.PeakRuleConclusion;
import com.gidi.bio_pcr.bean.Result;
import com.gidi.bio_pcr.bean.Threshold;
import com.gidi.bio_pcr.db.BioWayWellResult;
import com.gidi.bio_pcr.db.DatabaseHelper;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.pcr.update.OnUpdateExListener;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UpdateBiowayExTask extends AsyncTask<Experiment,Long,Long> {
    private Experiment mExperiment;
    private OnUpdateExListener onUpdateExListener;
    private String testItem;
    private String startTime;

    private boolean isMelt;

    private List<CtRuleConclusion> ct_conclusions = new ArrayList<CtRuleConclusion>();
    private List<PeakRuleConclusion> peak_conclusions = new ArrayList<PeakRuleConclusion>();
    private Map<String,Threshold> thresholdsMap = new HashMap<String,Threshold>();

    public void setOnUpdateExListener(OnUpdateExListener listener){
        this.onUpdateExListener = listener;
    }

    public UpdateBiowayExTask(boolean isMelt,
                              Map<String, Threshold> thresholds,
                              Experiment experiment,
                              String testItem,
                              String startTime,
                              List<CtRuleConclusion>ct_conclusions,
                              List<PeakRuleConclusion>peak_conclusions){
        this.mExperiment = experiment;
        this.ct_conclusions = ct_conclusions;
        this.peak_conclusions = peak_conclusions;
        this.startTime = startTime;
        this.testItem = testItem;
        this.isMelt  = isMelt;
        this.thresholdsMap = thresholds;
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
        Long result = (long)updateExperimentToDB(isMelt,thresholdsMap,mExperiment,testItem,startTime,ct_conclusions,peak_conclusions);

        return result;
    }

    @Override
    protected void onPostExecute(Long result) {
        super.onPostExecute(result);
        if(null != onUpdateExListener){
            if(result>0){
                onUpdateExListener.onUpdateDbResult(true);
            }else{
                onUpdateExListener.onUpdateDbResult(false);
            }
        }
    }



    public long updateExperimentToDB(boolean isMelt,
                                     Map<String,Threshold> thresholdsMap,
                                     Experiment experiment,
                                     String testItem,
                                     String start_time,
                                     List<CtRuleConclusion> ct_conclusions,
                                     List<PeakRuleConclusion> peak_conclusions){
        long index = 0;
        //根据
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
            //开始提取反应孔的信息
            for (int i = 0; i < wellResults.size(); i++) {

                List<TargetResult>  targetResults= wellResults.get(i).getTargetResults();

                if(null != targetResults && targetResults.size()>0) {

                    for (int j = targetResults.size() -1 ; j >= 0; j--) {
                        if(targetResults.get(j).getDye().toUpperCase().equals(("FAM"))){
                            if(!capture_FAM){
                                targetResults.remove(j);
                            }
                        }else if(targetResults.get(j).getDye().toUpperCase().equals(("HEX"))){
                            if(!capture_HEX){
                                targetResults.remove(j);
                            }
                        }else if(targetResults.get(j).getDye().toUpperCase().equals(("ROX"))){
                            if(!capture_ROX){
                                targetResults.remove(j);
                            }
                        }else if(targetResults.get(j).getDye().toUpperCase().equals(("CY5"))){
                            if(!capture_CY5){
                                targetResults.remove(j);
                            }
                        }

                    }
                }
            }
        }

        List<BioWayWellResult> lastWellResults = LiteORMCtrlMgr.getInstance().getBiowayExperimentWell(testItem, start_time);

        if(null != wellResults && wellResults.size()>0){
            //开始提取反应孔的信息
            for(int i=0;i<wellResults.size();i++){
                String well_id = wellResults.get(i).getID();//反应孔标识
                PeakRuleConclusion peakConclusion = getSinglePeakConclusion(peak_conclusions,well_id);
                CtRuleConclusion ctConclusion = getSingleCtConclusion(ct_conclusions,well_id);
                if(null != ctConclusion && null !=peakConclusion){
                    BioWayWellResult updateWellResult = getLastSLANWellName(wellResults.get(i).getID(),lastWellResults);
                    boolean result = updateBiowayWellResultToDB(isMelt,thresholdsMap,wellResults.get(i),
                            updateWellResult,ctConclusion.getConlusionMap(),peakConclusion.getConlusionMap(),
                            ctConclusion.getWellConclusion(),peakConclusion.getWellConclusion());
                    if(result){
                        index++;
                    }
                }else if(null != ctConclusion && null==peakConclusion){
                    BioWayWellResult updateWellResult = getLastSLANWellName(wellResults.get(i).getID(),lastWellResults);
                    boolean result = updateBiowayWellResultToDB(isMelt,thresholdsMap,wellResults.get(i),
                            updateWellResult,ctConclusion.getConlusionMap(),null,ctConclusion.getWellConclusion(),null);
                    if(result){
                        index++;
                    }
                }else if(null == ctConclusion && null!=peakConclusion){
                    BioWayWellResult updateWellResult = getLastSLANWellName(wellResults.get(i).getID(),lastWellResults);
                    boolean result = updateBiowayWellResultToDB(isMelt,thresholdsMap,wellResults.get(i),
                            updateWellResult,null,peakConclusion.getConlusionMap(),null,peakConclusion.getWellConclusion());
                    if(result){
                        index++;
                    }
                }else{
                    BioWayWellResult updateWellResult = getLastSLANWellName(wellResults.get(i).getID(),lastWellResults);
                    Log.i("update","updateWellResult name "+updateWellResult.getWellName());
                    boolean result = updateBiowayWellResultToDB(isMelt,thresholdsMap,wellResults.get(i),updateWellResult);
                    if(result){
                        index++;
                    }
                }


            }

            Log.i("update", "index" + index);


        }
        return index;
    }


    private boolean updateBiowayWellResultToDB(boolean isMelt,
                                               Map<String,Threshold> thresholdsMap,
                                               WellResult wellResult,
                                               BioWayWellResult updateSlanWell){
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();

        HashMap<String, Object> updateMap = new HashMap<String, Object>();

        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CONCLUSION, "");
        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_PEAK_CONCLUSION, "");

        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ISMELT,isMelt);
        List<TargetResult> targetResults = wellResult.getTargetResults();//目标结果

        if(targetResults != null && targetResults.size()>0){
            for(int i=0;i<targetResults.size();i++){
                TargetResult result = (TargetResult)targetResults.get(i);
                if(result.getChannel() == 1){
                    //Channel = 1= FAM
                    if(thresholdsMap !=null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("FAM")){
                            Threshold famThreshold = thresholdsMap.get("FAM");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_THRESHOLD,String.valueOf(famThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_PEEKHIGH_THRESHOLD,String.valueOf(famThreshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("fam")){
                            Threshold famThreshold = thresholdsMap.get("fam");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_THRESHOLD,String.valueOf(famThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_PEEKHIGH_THRESHOLD,String.valueOf(famThreshold.getPeekHighThreshold()));
                        }
                    }
                    String famResults = "";
                    String famPeakResults = "";
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_RESULT,famResults);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_PEAK_RESULT,famPeakResults);
                    String famTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_TARGETRESULT,
                            famTargetResults);

                } else if(result.getChannel() == 2){
                    //Channel = 2= HEX
                    if(thresholdsMap !=null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("HEX")){
                            Threshold hexThreshold = thresholdsMap.get("HEX");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_THRESHOLD,String.valueOf(hexThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_PEEKHIGH_THRESHOLD,String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("hex")){
                            Threshold hexThreshold = thresholdsMap.get("hex");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_THRESHOLD,String.valueOf(hexThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_PEEKHIGH_THRESHOLD,String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }
                    }
                    String hexResults = "";
                    String hexPeakResults = "";
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_RESULT,hexResults);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_PEAK_RESULT,hexPeakResults);
                    String hexTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_TARGETRESULT, hexTargetResults);

                }else if(result.getChannel()== 3){
                    //Channel = 2= ROX
                    if(thresholdsMap !=null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("ROX")){
                            Threshold roxThreshold = thresholdsMap.get("ROX");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_THRESHOLD,String.valueOf(roxThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_PEEKHIGH_THRESHOLD,String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("rox")){
                            Threshold roxThreshold = thresholdsMap.get("rox");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_THRESHOLD,String.valueOf(roxThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_PEEKHIGH_THRESHOLD,String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }
                    }
                    String roxResults = "";
                    String roxPeakResults ="";
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_RESULT,roxResults);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_PEAK_RESULT,roxPeakResults);
                    String roxTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_TARGETRESULT,
                            roxTargetResults);

                }else if(result.getChannel() == 4){
                    //Channel = 4= CY5
                    if(thresholdsMap !=null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("CY5")){
                            Threshold cy5Threshold = thresholdsMap.get("CY5");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_THRESHOLD,String.valueOf(cy5Threshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_PEEKHIGH_THRESHOLD,String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("cy5")){
                            Threshold cy5Threshold = thresholdsMap.get("cy5");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_THRESHOLD,String.valueOf(cy5Threshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_PEEKHIGH_THRESHOLD,String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }
                    }
                    String cy5Results = "";
                    String cytPeakResults ="";
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_RESULT,cy5Results);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_PEAK_RESULT,cytPeakResults);
                    String cy5TargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_TARGETRESULT,
                            cy5TargetResults);
                }else if(result.getChannel() == 5){
                    //Channel = 4= CY5
                    if(thresholdsMap !=null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("QUASAR")){
                            Threshold quasarThreshold = thresholdsMap.get("QUASAR");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_THRESHOLD,String.valueOf(quasarThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_PEEKHIGH_THRESHOLD,String.valueOf(quasarThreshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("quasar")){
                            Threshold quasarThreshold = thresholdsMap.get("quasar");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_THRESHOLD,String.valueOf(quasarThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_PEEKHIGH_THRESHOLD,String.valueOf(quasarThreshold.getPeekHighThreshold()));
                        }
                    }
                    String quasarResults = "";
                    String quasarPeakResults ="";
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_RESULT,quasarResults);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_PEAK_RESULT,quasarPeakResults);
                    String cy5TargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_TARGETRESULT,
                            cy5TargetResults);
                }else if(result.getChannel() == 6){

                    if(thresholdsMap !=null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("ATTO")){
                            Threshold attoThreshold = thresholdsMap.get("ATTO");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_THRESHOLD,String.valueOf(attoThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_PEEKHIGH_THRESHOLD,String.valueOf(attoThreshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("atto")){
                            Threshold attoThreshold = thresholdsMap.get("atto");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_THRESHOLD,String.valueOf(attoThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_PEEKHIGH_THRESHOLD,String.valueOf(attoThreshold.getPeekHighThreshold()));
                        }
                    }
                    String attoResults = "";
                    String attoPeakResults ="";
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_RESULT,attoResults);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_PEAK_RESULT,attoPeakResults);
                    String attoTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_TARGETRESULT,
                            attoTargetResults);
                }

            }
        }


        int update_well_result = LiteORMCtrlMgr.getInstance()
                .updateBiowayWellResult(updateSlanWell,updateMap);
        Log.i("update", "update_well_result" + update_well_result);
        if(update_well_result > 0){
            return  true;
        }
        return false;
    }


    /**
     * 每一个反应孔的信息
     * @param wellResult
     */
    private boolean updateBiowayWellResultToDB(boolean isMelt,
                                             Map<String,Threshold> thresholdsMap,
                                             WellResult wellResult,
                                             BioWayWellResult lastWellResult,
                                             Map<String,List<CtResult>> ct_conclusionMap,
                                             Map<String,List<PeakResult>> peak_conclusionMap,
                                             List<String> ct_well_conclusion,
                                             List<String> peak_well_conclusion){
        //样本的信息
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        HashMap<String, Object> updateMap = new HashMap<String, Object>();

        if(null != ct_well_conclusion && ct_well_conclusion.size()>0){
            String ct_conclusion = gson.toJson(ct_well_conclusion);
            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CONCLUSION, ct_conclusion);
        }else{
            String ct_conclusion = "";
            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CONCLUSION, ct_conclusion);
        }

        if(null != peak_well_conclusion && peak_well_conclusion.size()>0){
            String peak_conclusion = gson.toJson(peak_well_conclusion);
            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_PEAK_CONCLUSION, peak_conclusion);
        }else{
            String peak_conclusion = "";
            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_PEAK_CONCLUSION, peak_conclusion);
        }
        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ISMELT,isMelt);
        List<TargetResult> targetResults = wellResult.getTargetResults();//目标结果

        if(targetResults != null && targetResults.size()>0){
            for(int i=0;i<targetResults.size();i++){
                TargetResult result = (TargetResult)targetResults.get(i);
                if(result.getChannel() == 1){
                    //Channel = 1= FAM
                    if(ct_conclusionMap != null){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> famResultList  = ct_conclusionMap.get(result.getName());
                            if(famResultList != null && famResultList.size()>0){
                                String famResults = gson.toJson(famResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_RESULT,famResults);
                            }else {
                                String famResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_RESULT,famResults);
                            }
                        }
                    }else {
                        String famResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_RESULT,famResults);
                    }
                    if(peak_conclusionMap != null){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> famPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(famPeakResultList != null && famPeakResultList.size()>0){
                                String famPeakResults = gson.toJson(famPeakResultList);
                                Log.i("AAA","UPDATE == famPeakResults" + famPeakResults);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_PEAK_RESULT,famPeakResults);
                            }else {
                                String famPeakResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_PEAK_RESULT,famPeakResults);
                            }
                        }
                    }else {
                        String famPeakResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_PEAK_RESULT,famPeakResults);
                    }

                    if(thresholdsMap != null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("FAM")){
                            Threshold famThreshold = thresholdsMap.get("FAM");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_THRESHOLD,String.valueOf(famThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_PEEKHIGH_THRESHOLD,String.valueOf(famThreshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("fam")){
                            Threshold famThreshold = thresholdsMap.get("fam");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_THRESHOLD,String.valueOf(famThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_PEEKHIGH_THRESHOLD,String.valueOf(famThreshold.getPeekHighThreshold()));
                        }
                    }
                    String famTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_FAM_TARGETRESULT,
                            famTargetResults);

                } else if(result.getChannel() == 2){
                    //Channel = 2= HEX
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> hexResultList  = ct_conclusionMap.get(result.getName());
                            if(hexResultList != null && hexResultList.size()>0){
                                String hexResults = gson.toJson(hexResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_RESULT,hexResults);

                            }else{
                                String hexResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_RESULT,hexResults);
                            }
                        }
                    }else {
                        String hexResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_RESULT,hexResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> hexPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(hexPeakResultList != null && hexPeakResultList.size()>0){
                                String hexPeakResults = gson.toJson(hexPeakResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_PEAK_RESULT,hexPeakResults);
                            }else {
                                String hexPeakResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_PEAK_RESULT,hexPeakResults);
                            }
                        }
                    }else {
                        String hexPeakResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_PEAK_RESULT,hexPeakResults);
                    }
                    if(thresholdsMap != null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("HEX")){
                            Threshold hexThreshold = thresholdsMap.get("HEX");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_THRESHOLD,String.valueOf(hexThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_PEEKHIGH_THRESHOLD,String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("hex")){
                            Threshold hexThreshold = thresholdsMap.get("hex");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_THRESHOLD,String.valueOf(hexThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_PEEKHIGH_THRESHOLD,String.valueOf(hexThreshold.getPeekHighThreshold()));
                        }
                    }
                    String hexTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_HEX_TARGETRESULT, hexTargetResults);

                }else if(result.getChannel() == 3){
                    //Channel = 3= ROX
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> roxResultList  = ct_conclusionMap.get(result.getName());
                            if(roxResultList != null && roxResultList.size()>0){
                                String roxResults = gson.toJson(roxResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_RESULT,roxResults);
                            }else{
                                String roxResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_RESULT,roxResults);
                            }
                        }
                    }else {
                        String roxResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_RESULT,roxResults);
                    }
                    //peak结论
                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> roxPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(roxPeakResultList != null && roxPeakResultList.size()>0){
                                String roxPeakResults = gson.toJson(roxPeakResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_PEAK_RESULT,roxPeakResults);
                            }else {
                                String roxPeakResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_PEAK_RESULT,roxPeakResults);
                            }
                        }
                    }else {
                        String roxPeakResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_PEAK_RESULT,roxPeakResults);
                    }
                    if(thresholdsMap != null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("ROX")){
                            Threshold roxThreshold = thresholdsMap.get("ROX");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_THRESHOLD,String.valueOf(roxThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_PEEKHIGH_THRESHOLD,String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("rox")){
                            Threshold roxThreshold = thresholdsMap.get("rox");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_THRESHOLD,String.valueOf(roxThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_PEEKHIGH_THRESHOLD,String.valueOf(roxThreshold.getPeekHighThreshold()));
                        }
                    }
                    String roxTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ROX_TARGETRESULT,
                            roxTargetResults);

                }else if(result.getChannel() == 4){
                    //Channel = 4= CY5
                    if(ct_conclusionMap != null){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> cy5ResultList  = ct_conclusionMap.get(result.getName());
                            if(null != cy5ResultList && cy5ResultList.size()>0){
                                String cy5Results = gson.toJson(cy5ResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_RESULT,cy5Results);
                            }else{
                                String cy5Results = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_RESULT,cy5Results);
                            }
                        }
                    }else {
                        String cy5Results = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_RESULT,cy5Results);
                    }
                    //peak结论
                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> cy5PeakResultList  = peak_conclusionMap.get(result.getName());
                            if(cy5PeakResultList != null && cy5PeakResultList.size()>0){
                                String cy5PeakResults = gson.toJson(cy5PeakResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_PEAK_RESULT,cy5PeakResults);
                            }else {
                                String cy5PeakResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_PEAK_RESULT,cy5PeakResults);
                            }
                        }
                    }else {
                        String cy5PeakResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_PEAK_RESULT,cy5PeakResults);
                    }
                    if(thresholdsMap != null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("CY5")){
                            Threshold cy5Threshold = thresholdsMap.get("CY5");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_THRESHOLD,String.valueOf(cy5Threshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_PEEKHIGH_THRESHOLD,String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }else if(thresholdsMap.containsKey("cy5")){
                            Threshold cy5Threshold = thresholdsMap.get("cy5");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_THRESHOLD,String.valueOf(cy5Threshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_PEEKHIGH_THRESHOLD,String.valueOf(cy5Threshold.getPeekHighThreshold()));
                        }
                    }
                    String cy5TargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_CY5_TARGETRESULT,
                            cy5TargetResults);
                }else if(result.getChannel() == 5){
                    //Channel = 5= QUASAR
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> quasarResultList  = ct_conclusionMap.get(result.getName());
                            if(null != quasarResultList && quasarResultList.size()>0){
                                String quasarResults = gson.toJson(quasarResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_RESULT,quasarResults);
                            }else{
                                String quasarResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_RESULT,quasarResults);
                            }
                        }
                    }else{
                        String quasarResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_RESULT,quasarResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> quasarPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(quasarPeakResultList != null && quasarPeakResultList.size()>0){
                                String quasarPeakResults = gson.toJson(quasarPeakResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_PEAK_RESULT,quasarPeakResults);
                            }else {
                                String quasarPeakResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_PEAK_RESULT,quasarPeakResults);
                            }
                        }
                    }else {
                        String quasarPeakResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_PEAK_RESULT,quasarPeakResults);
                    }
                    if(thresholdsMap != null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("QUASAR")){
                            Threshold quasarThreshold = thresholdsMap.get("QUASAR");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_THRESHOLD,String.valueOf(quasarThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_PEEKHIGH_THRESHOLD,String.valueOf(quasarThreshold.getPeekHighThreshold()));
                        }
                    }
                    String quasarTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_QUASAR_TARGETRESULT, quasarTargetResults);

                }else if(result.getChannel() == 6){
                    //Channel = 5= QUASAR
                    if(null != ct_conclusionMap){
                        if(ct_conclusionMap.containsKey(result.getName())){
                            List<CtResult> attoResultList  = ct_conclusionMap.get(result.getName());
                            if(null != attoResultList && attoResultList.size()>0){
                                String attoResults = gson.toJson(attoResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_RESULT,attoResults);
                            }else{
                                String attoResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_RESULT,attoResults);
                            }
                        }
                    }else{
                        String attoResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_RESULT,attoResults);
                    }

                    if(null != peak_conclusionMap){
                        if(peak_conclusionMap.containsKey(result.getName())){
                            List<PeakResult> attoPeakResultList  = peak_conclusionMap.get(result.getName());
                            if(attoPeakResultList != null && attoPeakResultList.size()>0){
                                String attoPeakResults = gson.toJson(attoPeakResultList);
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_PEAK_RESULT,attoPeakResults);
                            }else {
                                String attoPeakResults = "";
                                updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_PEAK_RESULT,attoPeakResults);
                            }
                        }
                    }else {
                        String attoPeakResults = "";
                        updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_PEAK_RESULT,attoPeakResults);
                    }
                    if(thresholdsMap != null && thresholdsMap.size()>0){
                        if(thresholdsMap.containsKey("ATTO")){
                            Threshold attoThreshold = thresholdsMap.get("ATTO");
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_THRESHOLD,String.valueOf(attoThreshold.getThreshold()));
                            updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_PEEKHIGH_THRESHOLD,String.valueOf(attoThreshold.getPeekHighThreshold()));
                        }
                    }
                    String attoTargetResults = gson.toJson(result);
                    updateMap.put(DatabaseHelper.COLUMN_SLAN_WELL_ATTO_TARGETRESULT,
                            attoTargetResults);
                }

            }
        }


        int update_well_result = LiteORMCtrlMgr.getInstance()
                .updateBiowayWellResult(lastWellResult,updateMap);
        Log.i("update", "update_well_result" + update_well_result);
        if(update_well_result > 0){
           return  true;
        }
        return false;
    }

    private BioWayWellResult getLastSLANWellName(String wellName,List<BioWayWellResult> lastWellResults){

        for(BioWayWellResult slanWellResult :lastWellResults){
            if(slanWellResult.getWellName().contains(wellName)){
                return slanWellResult;
            }
        }
        return null;
    }

    private PeakRuleConclusion getSinglePeakConclusion(List<PeakRuleConclusion> peak_conclusions,
                                                       String wellResult_id) {
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

    private CtRuleConclusion getSingleCtConclusion(List<CtRuleConclusion> ct_conclusions,
                                                   String wellResult_id) {
        if(null != ct_conclusions && ct_conclusions.size() > 0) {
            for (CtRuleConclusion conclusion : ct_conclusions) {
                String conclusionWell = conclusion.getTupeNo();//getWellNum
                if (conclusionWell.equals(wellResult_id)) {
                    return conclusion;
                }
            }
        }
        return null;
    }
}
