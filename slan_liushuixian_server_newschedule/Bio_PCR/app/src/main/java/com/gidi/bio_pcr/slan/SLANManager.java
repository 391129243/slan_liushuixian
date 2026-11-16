package com.gidi.bio_pcr.slan;


import android.os.Environment;
import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.CTRuleEntity;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKRuleEntity;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKSingleRuleEntity;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.CtRuleConclusion;
import com.gidi.bio_pcr.bean.ExTemperature;
import com.gidi.bio_pcr.bean.JudgeConclusion;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.PeakRuleConclusion;
import com.gidi.bio_pcr.bean.Threshold;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.pcr.OnDetailExCallback;
import com.gidi.bio_pcr.pcr.OnExperimentCallback;
import com.gidi.bio_pcr.pcr.OnParseRunningExCallBack;
import com.gidi.bio_pcr.pcr.OnUploadCallBack;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.pcr.update.OnUpdateExListener;
import com.gidi.bio_pcr.slan.bean.SLANCurveList;
import com.gidi.bio_pcr.slan.bean.SLANExlResult;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.OkHttpUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.github.mikephil.charting.data.Entry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hongshi.slansdk.common.HSUtils;
import com.hongshi.slansdk.model.Dot;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;
import com.hongshi.slansdk.model.experiment.Experiment;
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.hongshi.slansdk.model.experiment.Temperature;
import com.hongshi.slansdk.model.result.MeltPeak;
import com.hongshi.slansdk.model.result.TargetResult;
import com.hongshi.slansdk.model.result.WellResult;
import com.hongshi.slansdk.service.SLANService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 宏石的控制类
 */
public class SLANManager {

    private static SLANManager mInstance;
    private Experiment mExperiment;
    private List<SLANExlResult> mExlResults = new ArrayList<SLANExlResult>();
    private List<SLANCurveList> mRawCurve = new ArrayList<SLANCurveList>();
    private List<SLANCurveList> mMeltCurve = new ArrayList<SLANCurveList>();
    private List<SLANCurveList> mAMPCurve = new ArrayList<SLANCurveList>();
    private List<SLANCurveList> mPeakCurve = new ArrayList<SLANCurveList>();

    private ArrayList<ExTemperature> mExTemperature = new ArrayList<ExTemperature>();

    private SaveSLANExAsyncTask saveSLANExAsyncTask = null;
    private OnExperimentCallback onExperimentCallback;
    public void setOnExperimentCallback(OnExperimentCallback callback){
        this.onExperimentCallback = callback;
    }
    public void releaseExperimentCallBack(){
        this.onExperimentCallback = null;
    }
    ////详情界面的回调
    private OnDetailExCallback onDetailExCallback;
    public void setOnDetailExCallback(OnDetailExCallback callback){
        this.onDetailExCallback = callback;
    }
    public void releaseOnDetailExCallback(){
        this.onDetailExCallback = null;
    }

    Map<String,Threshold> mThresholds = new HashMap<String,Threshold>();
    private boolean isMelt = false;

    private List<CtRuleConclusion> ct_Conclusions = new ArrayList<CtRuleConclusion>();
    private List<PeakRuleConclusion> peak_Conclusions = new ArrayList<PeakRuleConclusion>();

    public static SLANManager getInstance() {
        if (mInstance == null) {
            mInstance = new SLANManager();
        }
        return mInstance;
    }

    private void clearAll() {
        if (null != mRawCurve) {
            mRawCurve.clear();
        }
        if (null != mAMPCurve) {
            mAMPCurve.clear();
        }
        if (null != mMeltCurve) {
            mMeltCurve.clear();
        }

        if (null != mPeakCurve) {
            mPeakCurve.clear();
        }
        if (null != ct_Conclusions) {
            ct_Conclusions.clear();
        }
        if(null != peak_Conclusions){
            ct_Conclusions.clear();
        }

    }

    public void destroy(){
        clearAll();
        mAMPCurve = null;
        mMeltCurve = null;
        mRawCurve = null;
        mPeakCurve = null;
        ct_Conclusions = null;
		peak_Conclusions = null;
        mExlResults = null;
        mExTemperature = null;
    }


    public void setExperiment(Experiment mExperiment) {
        this.mExperiment = mExperiment;
    }

    public Experiment getExperiment() {
        return mExperiment;
    }

    /**
     * 解析实验结果
     *
     * @param experiment
     * @param ctConclusions
     * @param peakConclusions
     */
    public boolean pasreSLANExperimentResult(Experiment experiment,
                                          List<CtRuleConclusion> ctConclusions,
                                          List<PeakRuleConclusion> peakConclusions) {
        clearAll();
        mExlResults.clear();
        Log.i("SLANManager","pasreExperimentResult");
        //获取
        this.mExperiment = experiment;
        getExperimentParameter(experiment);
        List<WellResult> wellResults = experiment.getWellResult();
        if (wellResults != null && wellResults.size() > 0) {
            for (WellResult wellResult : wellResults) {
                if (null != wellResult) {
                    List<TargetResult> targetResults = wellResult.getTargetResults();
                    if (targetResults != null && targetResults.size() > 0) {
                        Log.i("SLANManager","targetResults个数" + targetResults.size());
                        for (TargetResult targetResult : targetResults) {
                           SLANCurveList mRawList = parseTargetResultToRaw(wellResult.getID(), wellResult.getSampleName(),targetResult);
                           SLANCurveList mAmpList = parseTargetResultToAMP(wellResult.getID(), wellResult.getSampleName(), targetResult);
                           SLANCurveList mMeltList = parseTargetResultToMelt(wellResult.getID(),  wellResult.getSampleName(),targetResult);
                           SLANCurveList mPeakList = parseTargetResultToPeak(wellResult.getID(),  wellResult.getSampleName(),targetResult);
                           mAMPCurve.add(mAmpList);
                           mRawCurve.add(mRawList);
                           mPeakCurve.add(mPeakList);
                           mMeltCurve.add(mMeltList);

                        }
                    }
                }
            }
            Log.i("RNA","mAMPCurve" + mAMPCurve.size() );
            Log.i("RNA","mRawCurve" + mRawCurve.size() );
            Log.i("RNA","mPeakCurve" + mPeakCurve.size() );
            Log.i("RNA","mMeltCurve" + mMeltCurve.size() );

            if (mRawCurve != null && mRawCurve.size() != 0) {
                mExlResults.clear();
                for (SLANCurveList curveList : mRawCurve) {
                    String wellName = curveList.getWellNum();
                    String hole_Num = curveList.getCurve_hole();//"FAM",CY5,HEX,ROX
                    List<CtResult> results = new ArrayList<>();
                    if (null != ctConclusions && ctConclusions.size() > 0) {
                        for (int k = 0; k < ctConclusions.size(); k++) {
                            String conclusionWell = ctConclusions.get(k).getTupeNo();
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<CtResult>> conlusionMap = ctConclusions.get(k).getConlusionMap();
                                if (conlusionMap.containsKey(hole_Num)) {
                                    results = conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }
                    List<PeakResult> peakResults = new ArrayList<PeakResult>();
                    if (null != peakConclusions && peakConclusions.size() > 0) {
                        for (int k = 0; k < peakConclusions.size(); k++) {
                            String conclusionWell = peakConclusions.get(k).getTupeNo();//getWellNum获取孔号
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<PeakResult>> conlusionMap = peakConclusions.get(k).getConlusionMap();
                                if (conlusionMap.containsKey(hole_Num)) {
                                    peakResults = conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }

                    SLANExlResult exlResult = new SLANExlResult();
                    exlResult.setWellNum(wellName);
                    exlResult.setChannelNo(curveList.getChannelNo());
                    exlResult.setCurve_hole(hole_Num);
                    exlResult.setResults(results);
                    exlResult.setPeakResults(peakResults);
                    exlResult.setMelt(isMelt);//溶解曲綫
                    exlResult.setControlRn(curveList.getControlRn());
                    exlResult.setPeakValues(curveList.getPeakValues());
                    exlResult.setCt(curveList.getCt());
                    exlResult.setIdCard(curveList.getIdCard());
                    if(null != mThresholds && mThresholds.size()>0){
                        if(mThresholds.containsKey(hole_Num)){
                            Threshold threshold = mThresholds.get(hole_Num);
                            exlResult.setThreshold(threshold.getThreshold());
                            exlResult.setPeekHighThreshold(threshold.getPeekHighThreshold());
                        }
                    }
                    mExlResults.add(exlResult);
                }

            } else if (mMeltCurve != null && mMeltCurve.size() != 0) {
                mExlResults.clear();
                for (SLANCurveList curveList : mMeltCurve) {
                    String wellName = curveList.getWellNum();
                    String hole_Num = curveList.getCurve_hole();//rox。fam。。
                    List<CtResult> results = new ArrayList<>();
                    if (null != ctConclusions && ctConclusions.size() > 0) {
                        for (int k = 0; k < ctConclusions.size(); k++) {
                            String conclusionWell = ctConclusions.get(k).getTupeNo();
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<CtResult>> conlusionMap = ctConclusions.get(k).getConlusionMap();
                                if (conlusionMap.containsKey(hole_Num)) {
                                    results = conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }
                    List<PeakResult> peakResults = new ArrayList<PeakResult>();
                    if (null != peakConclusions && peakConclusions.size() > 0) {
                        for (int k = 0; k < peakConclusions.size(); k++) {
                            String conclusionWell = peakConclusions.get(k).getTupeNo();//getWellNum
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<PeakResult>> conlusionMap = peakConclusions.get(k).getConlusionMap();
                                if (conlusionMap.containsKey(hole_Num)) {
                                    peakResults = conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }
                    SLANExlResult exlResult = new SLANExlResult();
                    exlResult.setChannelNo(curveList.getChannelNo());
                    exlResult.setWellNum(wellName);
                    exlResult.setCurve_hole(hole_Num);
                    exlResult.setResults(results);
                    exlResult.setPeakResults(peakResults);
                    exlResult.setMelt(isMelt);//溶解曲綫
                    exlResult.setControlRn(curveList.getControlRn());
                    exlResult.setPeakValues(curveList.getPeakValues());
                    exlResult.setCt(curveList.getCt());
                    exlResult.setIdCard(curveList.getIdCard());
                    if(null != mThresholds && mThresholds.size()>0){
                        if(mThresholds.containsKey(hole_Num)){
                            Threshold threshold = mThresholds.get(hole_Num);
                            exlResult.setThreshold(threshold.getThreshold());
                            exlResult.setPeekHighThreshold(threshold.getPeekHighThreshold());
                        }
                    }
                    mExlResults.add(exlResult);
                }
            }

/*
            if (null != onExperimentCallback) {
                onExperimentCallback.onParseFinish();
            }
*/

        }
        return true;
    }

    /**
     *
     * @param wellName 孔号
     * @param idCard 身份条码
     * @param targetResult
     * @return
     */
    public SLANCurveList parseTargetResultToRaw(String wellName,
                                                String idCard,
                                                TargetResult targetResult) {

        SLANCurveList curve = new SLANCurveList();
        if (null != targetResult) {
            List<Entry> entries = new ArrayList<Entry>();
            if (null != targetResult.getRawCurve() ) {
                ArrayList<Dot> RAWCurve = targetResult.getRawCurve();
                if(RAWCurve.size()>0){
                    for (Dot c : RAWCurve) {
                        if(c != null){
                            Entry entry = new Entry(Float.parseFloat(String.valueOf(c.getX())),
                                    Float.parseFloat(HSUtils.INSTANCE.roundDouble(c.getY(), 4) + ""));//曲线值
                            entries.add(entry);
                        }
                    }
                }
            }

            curve.setCurve_data(entries);
            curve.setWellNum(wellName);
            curve.setChannelNo(targetResult.getChannel());
            curve.setCurve_hole(targetResult.getName());//ROX/
            curve.setCt(targetResult.getCt());
            curve.setControlRn(targetResult.getRnValue());
            curve.setIdCard(idCard);
            if(null == targetResult.getPeakValues()){
                ArrayList<MeltPeak> peakValues = new ArrayList<MeltPeak>();
                curve.setPeakValues(peakValues);
            }else{
                ArrayList<MeltPeak> peakValues = targetResult.getPeakValues();
                curve.setPeakValues(peakValues);
            }
        }

        return curve;
    }

    /**
     * 扩增曲线数据
     * @param targetResult
     * @param idCard 身份信息
     * @return
     */
    public SLANCurveList parseTargetResultToAMP(String wellName,
                                                String idCard,
                                                TargetResult targetResult){

        SLANCurveList curve = new SLANCurveList();
        if (null != targetResult) {
            List<Entry> entries = new ArrayList<Entry>();
            if (null != targetResult.getAMPCurve() ) {
                ArrayList<Dot> AMPCurve = targetResult.getAMPCurve();
                if(AMPCurve.size()>0){
                    for (Dot c : AMPCurve) {
                        if(c != null){
                            Entry entry = new Entry(Float.parseFloat(String.valueOf(c.getX())),
                                    Float.parseFloat(HSUtils.INSTANCE.roundDouble(c.getY(), 4) + ""));//曲线值
                        
                            entries.add(entry);
                        }
                    }
                }
            }
            curve.setCurve_data(entries);
            curve.setWellNum(wellName);
            curve.setIdCard(idCard);
            curve.setChannelNo(targetResult.getChannel());
            curve.setCurve_hole(targetResult.getName());//ROX/
            curve.setCt(targetResult.getCt());
            curve.setControlRn(targetResult.getRnValue());
            if(null == targetResult.getPeakValues()){
                ArrayList<MeltPeak> peakValues = new ArrayList<MeltPeak>();
                curve.setPeakValues(peakValues);
            }else{
                ArrayList<MeltPeak> peakValues = targetResult.getPeakValues();
                curve.setPeakValues(peakValues);
            }
        }
        return curve;
    }


    /**
     * 获取熔解曲线数据
     */
    public SLANCurveList parseTargetResultToMelt(String wellName,
                                                 String idCard,
                                                 TargetResult targetResult){

        SLANCurveList curve = new SLANCurveList();
        if (null != targetResult) {
            List<Entry> entries = new ArrayList<Entry>();
            if (null != targetResult.getRawMeltCurve() ) {
                ArrayList<Dot> RawMeltCurve = targetResult.getRawMeltCurve();
                if(RawMeltCurve.size()>0){
                    for (Dot c : RawMeltCurve) {
                        if(c != null){
                            Entry entry = new Entry(Float.parseFloat(String.valueOf(c.getX())),
                                    Float.parseFloat(HSUtils.INSTANCE.roundDouble(c.getY(), 4) + ""));//曲线值
                            // Log.i("RNA","entry" + entry);
                            entries.add(entry);
                        }
                    }
                }
            }

            curve.setCurve_data(entries);
            curve.setWellNum(wellName);
            curve.setIdCard(idCard);
            curve.setChannelNo(targetResult.getChannel());
            curve.setCurve_hole(targetResult.getName());//ROX/
            curve.setCt(targetResult.getCt());
            curve.setControlRn(targetResult.getRnValue());
            if(null == targetResult.getPeakValues()){
                ArrayList<MeltPeak> peakValues = new ArrayList<MeltPeak>();
                curve.setPeakValues(peakValues);
            }else{
                ArrayList<MeltPeak> peakValues = targetResult.getPeakValues();
                curve.setPeakValues(peakValues);
            }

        }
        return curve;
    }

    public SLANCurveList parseTargetResultToPeak(String wellName,
                                                 String idCard,
                                                 TargetResult targetResult){

        SLANCurveList curve = new SLANCurveList();
        if (null != targetResult) {
            List<Entry> entries = new ArrayList<Entry>();
            if (null != targetResult.getPeakCurve()) {
                ArrayList<Dot> peakCurve = targetResult.getPeakCurve();
                if(peakCurve.size()>0){
                    for (Dot c : peakCurve) {
                        if(c != null){
                            Entry entry = new Entry(Float.parseFloat(String.valueOf(c.getX())),
                                    Float.parseFloat(HSUtils.INSTANCE.roundDouble(c.getY(), 4) + ""));//曲线值
                            entries.add(entry);
                        }
                    }
                }

            }
            curve.setCurve_data(entries);
            curve.setWellNum(wellName);
            curve.setIdCard(idCard);
            curve.setChannelNo(targetResult.getChannel());
            curve.setCurve_hole(targetResult.getName());//ROX/
            curve.setCt(targetResult.getCt());
            curve.setControlRn(targetResult.getRnValue());
            if(null == targetResult.getPeakValues()){
                ArrayList<MeltPeak> peakValues = new ArrayList<MeltPeak>();
                curve.setPeakValues(peakValues);
            }else{
                ArrayList<MeltPeak> peakValues = targetResult.getPeakValues();
                curve.setPeakValues(peakValues);
            }
        }

        return curve;
    }


    public List<SLANCurveList> getRawCurve() {
        return mRawCurve;
    }

    public List<SLANCurveList> getMeltCurve() {
        return mMeltCurve;
    }


    public List<SLANCurveList> getAMPCurve() {

        return mAMPCurve;
    }

    public List<SLANCurveList> getPeakCurve() {
       // Log.i("RNA","getPeakCurve" + mPeakCurve.size());
        return mPeakCurve;
    }

    public List<SLANExlResult> getExlResults() {
      //  Log.i("RNA","getExlResults" + mExlResults.size());
        return mExlResults;
    }

    public ArrayList<ExTemperature> getExTemperature() {
        return mExTemperature;
    }
    
    /**解析运行中的温度数据***/
    private ArrayList<ExTemperature> parseRunExTemperature(List<Temperature> temperatures){
        ArrayList<ExTemperature>  exTemperatures = new ArrayList<ExTemperature>();
        if(null != temperatures && temperatures.size()>0){
            List<Entry> tubeEntries = new ArrayList<Entry>();
            for(int i=0;i<temperatures.size();i+=10){

                Entry entry2 = new Entry(Float.parseFloat(String.valueOf(temperatures.get(i).getTime()/10000000)),
                        Float.parseFloat(HSUtils.INSTANCE.roundDouble(temperatures.get(i).getTubeTemperature(), 2) + ""));
                tubeEntries.add(entry2);
                // 如果已经是最后一个元素，直接跳出循环
                if (i + 10 >= temperatures.size()) {
                    break;
                }
            }
            // 确保包括最后一个元素（如果它不在上面循环中被添加）
            if (!tubeEntries.contains(new Entry(Float.parseFloat(String.valueOf(temperatures.get(temperatures.size()-1).getTime()/10000000)),
                    Float.parseFloat(HSUtils.INSTANCE.roundDouble(temperatures.get(temperatures.size()-1).getTubeTemperature(), 2) + "")))){
                tubeEntries.add(new Entry(Float.parseFloat(String.valueOf(temperatures.get(temperatures.size()-1).getTime()/10000000)),
                        Float.parseFloat(HSUtils.INSTANCE.roundDouble(temperatures.get(temperatures.size()-1).getTubeTemperature(), 2) + "")));

            }

            ExTemperature exTemperature = new ExTemperature();
            exTemperature.setTubeTemperatures(tubeEntries);
            exTemperatures.add(exTemperature);
           // Log.i("AAA","time" + HSUtils.INSTANCE.secondToHMSString(temperatures.get(temperatures.size()-1).getTime() / 10000000));
        }
        return exTemperatures;
    }

    public List<CtRuleConclusion> judgeCTFile(Experiment experiment,
                                              File ctFile,
                                              String startTime){
        List<CtRuleConclusion> ctRuleConclusions = new ArrayList<>();
        List<CTRuleEntity> cTRuleEntityList = CTRuleEntity.getCTRuleList(ctFile);
        List<CTRuleEntity> afterCtRuleList = CTRuleEntity.getCTRuleList(ctFile);

        if(cTRuleEntityList == null || cTRuleEntityList.isEmpty()){
            return null;
        }
        int lengthWell = experiment.getWellResult().size();
        //8个孔
        for(int i = 0;i < lengthWell;i++){
            for(int k = 0; k < cTRuleEntityList.size() ; k++){
                cTRuleEntityList.get(k).setPostive(false);
                cTRuleEntityList.get(k).setSuspicios(false);
            }
            for(int k = 0; k < afterCtRuleList.size() ; k++){
                afterCtRuleList.get(k).setPostive(false);
                afterCtRuleList.get(k).setSuspicios(false);
            }
            //一个well有几个Target就是有几个channel
            int lenghtChannel = experiment.getWellResult().get(i).getTargetResults().size();//

            for(int j = 0;j < lenghtChannel;j++){
                for(int k = 0; k < cTRuleEntityList.size() ; k++){

                    //target对应的channel Name
                    String channelRule = experiment.getWellResult().get(i).getTargetResults().get(j).getName().toLowerCase();
                    String channel = cTRuleEntityList.get(k).getChannel().toLowerCase();

                    if(channelRule.contains(channel)){

                        double up = (cTRuleEntityList.get(k).getCTvalue() + cTRuleEntityList.get(k).getCTvalueUP());

                        double down = (cTRuleEntityList.get(k).getCTvalue() - cTRuleEntityList.get(k).getCTvalueDown());

                        double up_suspicious = cTRuleEntityList.get(k).getCTvalue()+
                                cTRuleEntityList.get(k).getCTvalueUP()+
                                cTRuleEntityList.get(k).getCTvalueUP_Suspicious();

                        double ct = experiment.getWellResult().get(i).getTargetResults().get(j).getCt();
                        if( ct >= down && ct <= up ){
                            afterCtRuleList.get(k).setPostive(true);
                        }else if(ct > up && ct <= up_suspicious){
                            afterCtRuleList.get(k).setSuspicios(true);
                        }
                        afterCtRuleList.get(k).setActualCt(ct);
                        afterCtRuleList.get(k).setCTvalueUP(up);
                        afterCtRuleList.get(k).setCTvalueDown(down);
                        afterCtRuleList.get(k).setCTvalueUP_Suspicious(up_suspicious);
                    }
                }
            }

            ArrayList<String> wellConclusion = new ArrayList<String>();
////////////////////////////////////////////////保存///////////////////////////////////////////////////////////////
            Map<String,List<CtResult>> ctResultMaps = new HashMap<String,List<CtResult>>();
            ctResultMaps.clear();
            for(int k = 0; k < afterCtRuleList.size() ; k++){
                List<CtResult> results = new ArrayList<>();
                if(afterCtRuleList.get(k).isPostive()){
                    wellConclusion.add(afterCtRuleList.get(k).getCategory() + ":" + "阳性");
                    CtResult result = new CtResult();
                    result.setCategory(afterCtRuleList.get(k).getCategory());
                    result.setValue(afterCtRuleList.get(k).getCTvalue());
                    result.setCtValueUp(afterCtRuleList.get(k).getCTvalueUP());
                    result.setCtValueDown(afterCtRuleList.get(k).getCTvalueDown());
                    result.setCtValueUp_Suspicious(afterCtRuleList.get(k).getCTvalueUP_Suspicious());
                    result.setActualCt(afterCtRuleList.get(k).getActualCt());
                    result.setConclusion("阳性");
                    results.add(result);
                }else if(afterCtRuleList.get(k).isSuspicios()){
                    wellConclusion.add(afterCtRuleList.get(k).getCategory() + ":" + "可疑样本");
                    CtResult result = new CtResult();
                    result.setCategory(afterCtRuleList.get(k).getCategory());
                    result.setValue(afterCtRuleList.get(k).getCTvalue());
                    result.setCtValueUp(afterCtRuleList.get(k).getCTvalueUP());
                    result.setCtValueDown(afterCtRuleList.get(k).getCTvalueDown());
                    result.setCtValueUp_Suspicious(afterCtRuleList.get(k).getCTvalueUP_Suspicious());
                    result.setActualCt(afterCtRuleList.get(k).getActualCt());
                    result.setConclusion("可疑样本");
                    results.add(result);
                }else{

                    wellConclusion.add(afterCtRuleList.get(k).getCategory() + ":" + "阴性");
                    CtResult result = new CtResult();
                    result.setCategory(afterCtRuleList.get(k).getCategory());
                    result.setValue(afterCtRuleList.get(k).getCTvalue());
                    result.setCtValueUp(afterCtRuleList.get(k).getCTvalueUP());
                    result.setCtValueDown(afterCtRuleList.get(k).getCTvalueDown());
                    result.setCtValueUp_Suspicious(afterCtRuleList.get(k).getCTvalueUP_Suspicious());
                    result.setActualCt(afterCtRuleList.get(k).getActualCt());
                    result.setConclusion("阴性");
                    results.add(result);
                }
                ctResultMaps.put(afterCtRuleList.get(k).getChannel().toUpperCase(),results);
            }

            CtRuleConclusion ctRuleConclusion = new CtRuleConclusion();
            ctRuleConclusion.setTupeNo(experiment.getWellResult().get(i).getID());//检测的孔位
            ctRuleConclusion.setSampleID(experiment.getWellResult().get(i).getSampleName());//样本名称
            ctRuleConclusion.setTime(startTime);
            ctRuleConclusion.setConlusionMap(ctResultMaps);
            ctRuleConclusion.setWellConclusion(wellConclusion);
            ctRuleConclusions.add(ctRuleConclusion);

        }
        this.mExperiment = experiment;
        return ctRuleConclusions;
    }


    public  List<PeakRuleConclusion> judgePEAKFile(Experiment experiment,
                                                   File peakFile,
                                                   String startTime) {
        List<PeakRuleConclusion> peakRuleConclusions = new ArrayList<PeakRuleConclusion>();
        List<PEAKRuleEntity> pEAKRuleEntityList = PEAKRuleEntity.getPEAKRuleList(peakFile);
        List<PEAKRuleEntity> afterPEAKRuleList = PEAKRuleEntity.getPEAKRuleList(peakFile);

        if(pEAKRuleEntityList == null || pEAKRuleEntityList.isEmpty()){
            return null;
        }

        int lengthWell = experiment.getWellResult().size();//8个孔

        for(int i = 0;i < lengthWell;i++){
            for(int k = 0; k < pEAKRuleEntityList.size() ; k++){
                for(int l = 0; l < pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().size(); l++){
                    pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().get(l).setPostive(false);
                }
            }

            for(int k = 0; k < afterPEAKRuleList.size() ; k++){
                for(int l = 0; l < afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().size(); l++){
                    afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).setPostive(false);
                }
            }

            int lenghtChannel = experiment.getWellResult().get(i).getTargetResults().size();//每个孔有几个targetchannel
            for (int j = 0; j < lenghtChannel; j++) {//有3个channel

                String channelRule = experiment.getWellResult().get(i).getTargetResults().get(j).getName().toLowerCase();//FAM灯
                for (int k = 0; k < pEAKRuleEntityList.size(); k++) {

                    String channel = pEAKRuleEntityList.get(k).getChannel().toLowerCase();
                    if (channelRule.contains(channel)) {

                        for(int l = 0; l < pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().size(); l++){

                            double up = pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalue()
                                    + pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueUP();

                            double down = pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalue()
                                    - pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueDown();


                            double YM = pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueRM();

                            if(experiment.getWellResult().get(i).getTargetResults().get(j).getPeakValues() == null){
                                continue;

                            }

                            for(int m = 0; m < experiment.getWellResult().get(i).getTargetResults().get(j).getPeakValues().size(); m++){
                                MeltPeak meltPeak =  (MeltPeak) experiment.getWellResult().get(i).getTargetResults().get(j).getPeakValues().get(m);
                                double peak = meltPeak.getTm();
                                double peakYM = meltPeak.getRm();//////需要新的SDK
                                if( peak >= down && peak <= up &&  peakYM >= YM){//////////////表格需要显示YM值
                                    afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).setPostive(true);
                                    //  pEAKRuleEntityList.get(k).getPEAKSingleRuleEntityList().get(l).setPEAKvalue(peak);
                                }
                                afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).setActualRM(peakYM);
                                afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).setActualTM(peak);
                                afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).setPEAKvalueUP(up);
                                afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).setPEAKvalueDown(down);
                            }
                        }
                    }
                }
            }//每个well三个target
            ArrayList<String> wellConclusion = new ArrayList<String>();
            ///////////////////////////////////////////保存peak结论/////////////////////////////////////////////

            Map<String,List<PeakResult>> peakResultMaps = new HashMap<String,List<PeakResult>>();
            peakResultMaps.clear();
            Log.i("SLAN","pEAKRuleEntityList.size()" + afterPEAKRuleList.size());
            for(int k = 0; k < afterPEAKRuleList.size() ; k++){
                List<PeakResult> results = new ArrayList<>();
                for(int l = 0; l < afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().size(); l++){
                    PeakResult result = new PeakResult();
                    if(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).isPostive()){
                        wellConclusion.add(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getCategory() + ":" + "阳性" + "\n");
                        result.setCategory(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getCategory());
                        result.setValue(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalue());
                        result.setPeakValueUp(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueUP());
                        result.setPeakValueDown(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueDown());
                        result.setPeakValueRM(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueRM());
                        result.setActualRM(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getActualRM());
                        result.setActualTM(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getActualTM());
                        result.setConclusion("阳性");
                        results.add(result);
                    }else{

                         wellConclusion.add(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getCategory()  + ":" + "阴性" + "\n");
                        result.setCategory(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getCategory());
                        result.setValue(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalue());
                        result.setPeakValueUp(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueUP());
                        result.setPeakValueDown(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueDown());
                        result.setPeakValueRM(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getPEAKvalueRM());
                        result.setActualRM(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getActualRM());
                        result.setActualTM(afterPEAKRuleList.get(k).getPEAKSingleRuleEntityList().get(l).getActualTM());
                        result.setConclusion("阴性");
                        results.add(result);
                    }
                }
                peakResultMaps.put(afterPEAKRuleList.get(k).getChannel().toUpperCase(),results);
            }

            PeakRuleConclusion peakRuleConclusion = new PeakRuleConclusion();
            peakRuleConclusion.setTupeNo(experiment.getWellResult().get(i).getID());
            peakRuleConclusion.setSampleID(experiment.getWellResult().get(i).getSampleName());//样本ID
            peakRuleConclusion.setTime(startTime);
            peakRuleConclusion.setConlusionMap(peakResultMaps);
            peakRuleConclusion.setWellConclusion(wellConclusion);
            peakRuleConclusions.add(peakRuleConclusion);
        }
        this.mExperiment = experiment;
        return peakRuleConclusions;
    }


    /*******************************************2保存数据库**********************************/
    /**
     * 获取实验参数
     * @param experiment
     */
    private void getExperimentParameter(Experiment experiment) {
        isMelt = false;
        mThresholds.clear();
        if (null != PcrApplication.slanService) {
            if (null != experiment) {
                ArrayList<AnalyseParamter> analyseParamters = PcrApplication.slanService
                        .getExperimentParamter(experiment.getName());
                if (analyseParamters != null && analyseParamters.size() > 0) {
                    ArrayList<Target> targets = analyseParamters.get(0).getTargets();
                    if (null != targets && targets.size() > 0) {
                        for (Target target : targets) {
                            if (target.getChannelNo()==1) {
                                TargetParamter famParamer = target.getParamter();
                                Threshold famThreshold  = new Threshold();
                                famThreshold.setThreshold(famParamer.getThreshold());
                                famThreshold.setAutoThreshold(famParamer.getAutoThreshold());
                                if (famParamer instanceof TargetParamterMelt) {
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) famParamer;
                                    famThreshold.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                } else {
                                    isMelt = false;
                                }
                                mThresholds.put("FAM",famThreshold);

                            } else if (target.getChannelNo() == 3) {
                                TargetParamter roxParamer = target.getParamter();
                                Threshold roxThreshold = new Threshold();
                                roxThreshold.setAutoThreshold(roxParamer.getAutoThreshold());
                                roxThreshold.setThreshold(roxParamer.getThreshold());
                                if (roxParamer instanceof TargetParamterMelt) {
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) roxParamer;
                                    roxThreshold.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                } else {
                                    isMelt = false;

                                }
                                mThresholds.put("ROX",roxThreshold);

                            } else if (target.getChannelNo() == 2) {

                                TargetParamter hexParamer = target.getParamter();
                                Threshold hexThreshold = new Threshold();
                                hexThreshold.setThreshold(hexParamer.getThreshold());
                                hexThreshold.setAutoThreshold(hexParamer.getAutoThreshold());
                                if (hexParamer instanceof TargetParamterMelt) {
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) hexParamer;
                                    hexThreshold.setPeekHighThreshold( meltTargetParamer.getPeekHightThreshold());
                                } else {
                                    isMelt = false;

                                }
                                mThresholds.put("HEX",hexThreshold);

                            } else if (target.getChannelNo() == 4) {
                                TargetParamter cy5Paramer = target.getParamter();
                                Threshold cy5Threshold = new Threshold();
                                cy5Threshold.setThreshold(cy5Paramer.getThreshold());
                                cy5Threshold.setAutoThreshold(cy5Paramer.getAutoThreshold());
                                if (cy5Paramer instanceof TargetParamterMelt) {
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) cy5Paramer;
                                    cy5Threshold.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                } else {
                                    isMelt = false;
                                }
                                mThresholds.put("CY5",cy5Threshold);
                            }else if (target.getChannelNo() == 5) {
                                TargetParamter quasarParamer = target.getParamter();
                                Threshold quasarThreshold = new Threshold();
                                quasarThreshold.setThreshold(quasarParamer.getThreshold());
                                quasarThreshold.setAutoThreshold(quasarParamer.getAutoThreshold());
                                if (quasarParamer instanceof TargetParamterMelt) {
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) quasarParamer;
                                    quasarThreshold.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                } else {
                                    isMelt = false;
                                }
                                mThresholds.put("QUASAR",quasarThreshold);
                            }else if (target.getChannelNo() == 6) {
                                TargetParamter attoParamer = target.getParamter();
                                Threshold attoThreshold = new Threshold();
                                attoThreshold.setThreshold(attoParamer.getThreshold());
                                attoThreshold.setAutoThreshold(attoParamer.getAutoThreshold());
                                if (attoParamer instanceof TargetParamterMelt) {
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) attoParamer;
                                    attoThreshold.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                } else {
                                    isMelt = false;
                                }
                                mThresholds.put("ATTO",attoThreshold);
                            }
                        }
                    }
                }

            }
        }
    }

    /**
     * 保存宏石实验结果至数据库
     * @param experiment        实验
     * @param startTime         开始时间
     * @param templateDir       运行模板
     * @param ct_conclusions    扩增结论
     * @param peakConclusions   熔解结论
     */
    public void saveSLANExResultToDB(Experiment experiment,
                                     String filePath,
                                     String startTime,
                                     String endTime,
                                     String templateDir,
                                     String description,
                                     List<CtRuleConclusion> ct_conclusions,
                                     List<PeakRuleConclusion> peakConclusions){
        Log.i("SLANManager","保存数据库" + experiment.getName());
        getExperimentParameter(experiment);
        if(this.mExperiment == null){
            this.mExperiment = experiment;
        }
        Log.i("SLANManager","保存数据库this.mExperiment" + this.mExperiment.getName());
        saveSLANExAsyncTask = new SaveSLANExAsyncTask(isMelt,mThresholds,
                mExperiment,filePath, startTime,endTime,templateDir,description,ct_conclusions,peakConclusions);
        saveSLANExAsyncTask.execute();
        saveSLANExAsyncTask.setOnSaveExperimentListener(new OnSaveExperimentListener() {
            @Override
            public void onSaveSuccess() {
                cancelTask();
                if(null != onExperimentCallback){
                    onExperimentCallback.onSaveExToDbResult(true);
                }
            }

            @Override
            public void onSaveFail() {
                cancelTask();
                if(null != onExperimentCallback){
                    onExperimentCallback.onSaveExToDbResult(false);
                }
            }
        });
    }

    private void cancelTask(){
        if(null != saveSLANExAsyncTask){
            saveSLANExAsyncTask.cancel(true);
            saveSLANExAsyncTask = null;
        }
    }

    /*****************************************二次分析*****************************************/
    /**
     * 二次分析
     * @param configTargets 参数设置值
     * @param ctFile        扩增判别文件
     * @param peakFile      熔解判别文件
     */
    public void secondSLANRunExAnalyse(List<ConfigTarget> configTargets,
                                       int AMPAlgorithm,
                                       File ctFile,
                                       File peakFile,
                                       String startTime){
        //获取实验参数
        if(null != PcrApplication.slanService) {
            ArrayList<AnalyseParamter> analyseParamters = PcrApplication.slanService
                    .getExperimentParamter(mExperiment.getName());
            analyseParamters.get(0).setAMPAlgorithm(AMPAlgorithm);
            ArrayList<Target> targets = analyseParamters.get(0).getTargets();
            if(null !=targets && targets.size()>0) {
                for (Target target : targets) {
                    if (target.getChannelNo()== 1) {
                        ConfigTarget famTarget = getConfigTarget(configTargets,target.getChannelNo());
                        TargetParamter famParamer = target.getParamter();
                        famParamer.setBeginBaseline(famTarget.getBeginBaseline());
                        famParamer.setEndBaseline(famTarget.getEndBaseline());
                        famParamer.setThreshold(famTarget.getThreshold());
                        famParamer.setAutoThreshold(famParamer.getAutoThreshold());
                        if (famParamer instanceof TargetParamterMelt) {
                            TargetParamterMelt meltTargetParamer = (TargetParamterMelt) famParamer;
                            meltTargetParamer.setPeekHightThreshold(famTarget.getPeekHighThreshold());
                        }
                    } else if (target.getChannelNo() == 2) {
                        ConfigTarget hexTarget = getConfigTarget(configTargets,target.getChannelNo());
                        TargetParamter hexParamer = target.getParamter();
                        hexParamer.setBeginBaseline(hexTarget.getBeginBaseline());
                        hexParamer.setEndBaseline(hexTarget.getEndBaseline());
                        hexParamer.setThreshold(hexTarget.getThreshold());
                        hexParamer.setAutoThreshold(hexTarget.isAutoThreshold());
                        if (hexParamer instanceof TargetParamterMelt) {
                            TargetParamterMelt meltTargetParamer = (TargetParamterMelt) hexParamer;
                            meltTargetParamer.setPeekHightThreshold(hexTarget.getPeekHighThreshold());
                        }
                    } else if (target.getChannelNo() == 3) {
                        ConfigTarget roxTarget = getConfigTarget(configTargets,target.getChannelNo());
                        TargetParamter roxParamer = target.getParamter();
                        roxParamer.setBeginBaseline(roxTarget.getBeginBaseline());
                        roxParamer.setEndBaseline(roxTarget.getEndBaseline());
                        roxParamer.setThreshold(roxTarget.getThreshold());
                        roxParamer.setAutoThreshold(roxTarget.isAutoThreshold());
                        if (roxParamer instanceof TargetParamterMelt) {
                            TargetParamterMelt meltTargetParamer = (TargetParamterMelt) roxParamer;
                            meltTargetParamer.setPeekHightThreshold(roxTarget.getPeekHighThreshold());
                        }
                    } else if (target.getChannelNo() == 4) {
                        ConfigTarget cy5Target = getConfigTarget(configTargets,target.getChannelNo());
                        TargetParamter cy5Paramer = target.getParamter();
                        cy5Paramer.setBeginBaseline(cy5Target.getBeginBaseline());
                        cy5Paramer.setEndBaseline(cy5Target.getEndBaseline());
                        cy5Paramer.setThreshold(cy5Target.getThreshold());
                        cy5Paramer.setAutoThreshold(cy5Target.isAutoThreshold());
                        if (cy5Paramer instanceof TargetParamterMelt) {
                            TargetParamterMelt meltTargetParamer = (TargetParamterMelt) cy5Paramer;
                            meltTargetParamer.setPeekHightThreshold(cy5Target.getPeekHighThreshold());
                        }
                    }else if (target.getChannelNo() == 5) {
                        ConfigTarget quasarTarget = getConfigTarget(configTargets,target.getChannelNo());
                        TargetParamter quasarParamer = target.getParamter();
                        quasarParamer.setBeginBaseline(quasarTarget.getBeginBaseline());
                        quasarParamer.setEndBaseline(quasarTarget.getEndBaseline());
                        quasarParamer.setThreshold(quasarTarget.getThreshold());
                        quasarParamer.setAutoThreshold(quasarTarget.isAutoThreshold());
                        if (quasarParamer instanceof TargetParamterMelt) {
                            TargetParamterMelt meltTargetParamer = (TargetParamterMelt) quasarParamer;
                            meltTargetParamer.setPeekHightThreshold(quasarTarget.getPeekHighThreshold());
                        }
                    }else if (target.getChannelNo() == 6) {
                        ConfigTarget attoTarget = getConfigTarget(configTargets,target.getChannelNo());
                        TargetParamter attoParamer = target.getParamter();
                        attoParamer.setBeginBaseline(attoTarget.getBeginBaseline());
                        attoParamer.setEndBaseline(attoTarget.getEndBaseline());
                        attoParamer.setThreshold(attoTarget.getThreshold());
                        attoParamer.setAutoThreshold(attoTarget.isAutoThreshold());
                        if (attoParamer instanceof TargetParamterMelt) {
                            TargetParamterMelt meltTargetParamer = (TargetParamterMelt) attoParamer;
                            meltTargetParamer.setPeekHightThreshold(attoTarget.getPeekHighThreshold());
                        }
                    }
                }
            }
            Experiment sec_experiment = PcrApplication.slanService.setExperimentParamter(mExperiment.getName(),
                    analyseParamters.get(0));
            if(null != sec_experiment){
                List<CtRuleConclusion> conclusions = new ArrayList<CtRuleConclusion>();
                List<PeakRuleConclusion> peakConclusions = new ArrayList<>();
                if(null != ctFile){
                    conclusions = judgeCTFile(sec_experiment, ctFile,startTime);
                }
                if(null != peakFile){
                    peakConclusions = judgePEAKFile(sec_experiment, peakFile,startTime);
                }
                boolean result =  parseSecondExperiment(sec_experiment,conclusions,peakConclusions);
                if(result){
                    if(onExperimentCallback != null ){
                        onExperimentCallback.onSecondParseFinish(true);
                    }
                }else{
                    if(onExperimentCallback != null ){
                        onExperimentCallback.onSecondParseFinish(false);
                    }
                }
            }else {
                if(onExperimentCallback != null ){
                    onExperimentCallback.onSecondParseFinish(false);
                }
            }
        }else {
            if(onExperimentCallback != null ){
                onExperimentCallback.onSecondParseFinish(false);
            }
        }

    }



    /**2 记录界面调用
     * 设置试验参数前必须先获取实验参数
     * @param AMPAlgorithm 扩增曲线算法枚举
     * @param templatePath 模板路径不要“/”
     * @param configTargets true是自动阈值，false手动阈值
     */
    public void secondSLANExAnalyse(List<ConfigTarget> configTargets,
                                    int AMPAlgorithm,
                                    String templatePath,
                                    Experiment experiment,
                                    String startTime){
        //获取实验参数
       if(null != PcrApplication.slanService){
           ArrayList<AnalyseParamter> analyseParamters = PcrApplication.slanService
                   .getExperimentParamter(experiment.getName());
           if(null != analyseParamters.get(0)){
               analyseParamters.get(0).setAMPAlgorithm(AMPAlgorithm);
               ArrayList<Target> targets = analyseParamters.get(0).getTargets();
               if(null !=targets && targets.size()>0){
                   for(Target target :targets){
                       if(target.getChannelNo() != -1){
                           ConfigTarget famTarget = getConfigTarget(configTargets,target.getChannelNo());
                           if(null != famTarget){
                               TargetParamter famParamer = target.getParamter();
                               famParamer.setThreshold(famTarget.getThreshold());
                               famParamer.setBeginBaseline(famTarget.getBeginBaseline());
                               famParamer.setEndBaseline(famTarget.getEndBaseline());
                               famParamer.setQualitativeAnalysis(famTarget.isQualitativeAnalysis());
                               famParamer.setAutoThreshold(famTarget.isAutoThreshold());
                               famParamer.setOptimizationMode(famTarget.getOptimizationMode());
                               famParamer.setDigitalFilter(famTarget.getDigitalFilter());
                               if(famParamer instanceof TargetParamterMelt){
                                   TargetParamterMelt meltTargetParamer = (TargetParamterMelt) famParamer;
                                   meltTargetParamer.setPeekHightThreshold(famTarget.getPeekHighThreshold());
                                   meltTargetParamer.setMinTemperature(famTarget.getMinTemperature());
                                   meltTargetParamer.setMaxTemperature(famTarget.getMaxTemperature());
                                   meltTargetParamer.setMeltPeakAlgorithm(famTarget.getMeltPeakAlgorithm());
                                   meltTargetParamer.setPeekType(famTarget.getPeekType());
                               }
                           }

                       }
                   }
                   Experiment sec_experiment = PcrApplication.slanService.setExperimentParamter(experiment.getName(),
                           analyseParamters.get(0));
                   if(null != sec_experiment){
                       List<CtRuleConclusion> ctConclusions = new ArrayList<>();
                       List<PeakRuleConclusion> peakConclusions = new ArrayList<>();
                       if(!StringUtil.isEmpty(templatePath)){
                           ctConclusions = getAllCtResults(sec_experiment, templatePath,startTime);
                           peakConclusions = getAllPeakResults(sec_experiment,templatePath,startTime);
                       }

                       boolean result = parseSecondExperiment(sec_experiment,ctConclusions,peakConclusions);
                       if(result){
                           if(null != onDetailExCallback){
                               onDetailExCallback.onSecondParseFinish(true);
                           }
                       }else{
                           if(null != onDetailExCallback){
                               onDetailExCallback.onSecondParseFinish(false);
                           }
                       }

                   }else {
                       if(null != onDetailExCallback){
                           onDetailExCallback.onSecondParseFinish(false);
                       }
                   }
               }else{
                   if(null != onDetailExCallback){
                       onDetailExCallback.onSecondParseFinish(false);
                   }
               }
           }else{
               if(null != onDetailExCallback){
                   onDetailExCallback.onSecondParseFinish(false);
               }
           }
       }else{
           if(null != onDetailExCallback){
               onDetailExCallback.onSecondParseFinish(false);
           }
       }
    }


    public boolean parseSecondExperiment(Experiment experiment,
                                      List<CtRuleConclusion> ct_conclusions,
                                      List<PeakRuleConclusion> peakConclusions) {

        clearAll();
        mExlResults.clear();
        getExperimentParameter(experiment);
        List<WellResult> wellResults = experiment.getWellResult();
        if (wellResults != null && wellResults.size() > 0) {
            Log.i("SLAN","wellResults" + wellResults.size());
            for (WellResult wellResult : wellResults) {
                if (null != wellResult) {
                    List<TargetResult> targetResults = wellResult.getTargetResults();
                    if (targetResults != null && targetResults.size() > 0) {

                        for (TargetResult targetResult : targetResults) {

                            SLANCurveList mRawList = parseTargetResultToRaw(wellResult.getID(), wellResult.getSampleName(), targetResult);
                            SLANCurveList mAmpList = parseTargetResultToAMP(wellResult.getID(),  wellResult.getSampleName(),targetResult);
                            SLANCurveList mMeltList = parseTargetResultToMelt(wellResult.getID(), wellResult.getSampleName(), targetResult);
                            SLANCurveList mPeakList = parseTargetResultToPeak(wellResult.getID(),  wellResult.getSampleName(),targetResult);
                            mAMPCurve.add(mAmpList);
                            mRawCurve.add(mRawList);
                            mPeakCurve.add(mPeakList);
                            mMeltCurve.add(mMeltList);

                        }
                    }
                }
            }

            if (mRawCurve != null && mRawCurve.size() != 0) {
                mExlResults.clear();
                for (SLANCurveList curveList : mRawCurve) {
                    String wellName = curveList.getWellNum();
                    String hole_Num = curveList.getCurve_hole();
                    List<CtResult> results = new ArrayList<>();
                    if (null != ct_conclusions && ct_conclusions.size() > 0) {
                        for (int k = 0; k < ct_conclusions.size(); k++) {
                            String conclusionWell = ct_conclusions.get(k).getTupeNo();//getWellName
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<CtResult>> conlusionMap = ct_conclusions.get(k).getConlusionMap();
                                if (conlusionMap.containsKey(hole_Num)) {
                                    results = conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }
                    List<PeakResult> peakResults = new ArrayList<PeakResult>();
                    if (null != peakConclusions && peakConclusions.size() > 0) {
                        for (int k = 0; k < peakConclusions.size(); k++) {
                            String conclusionWell = peakConclusions.get(k).getTupeNo();//getWellNum
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<PeakResult>> peak_conlusionMap = peakConclusions.get(k).getConlusionMap();
                                if (peak_conlusionMap.containsKey(hole_Num)) {
                                    peakResults = peak_conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }
                    SLANExlResult exlResult = new SLANExlResult();
                    exlResult.setChannelNo(curveList.getChannelNo());
                    exlResult.setWellNum(wellName);
                    exlResult.setCurve_hole(hole_Num);
                    exlResult.setResults(results);
                    exlResult.setPeakResults(peakResults);
                    exlResult.setControlRn(curveList.getControlRn());
                    exlResult.setIdCard(curveList.getIdCard());
                    exlResult.setMelt(isMelt);//是否是熔解曲线

                    if(null != mThresholds && mThresholds.size()>0){
                        if(mThresholds.containsKey(hole_Num)){
                            Threshold threshold = mThresholds.get(hole_Num);
                            exlResult.setThreshold(threshold.getThreshold());
                            exlResult.setPeekHighThreshold(threshold.getPeekHighThreshold());
                        }
                    }
                    exlResult.setPeakValues(curveList.getPeakValues());
                    exlResult.setCt(curveList.getCt());
                    mExlResults.add(exlResult);

                }

            } else if (mMeltCurve != null && mMeltCurve.size() != 0) {
                mExlResults.clear();
                for (SLANCurveList curveList : mMeltCurve) {
                    String wellName = curveList.getWellNum();
                    String hole_Num = curveList.getCurve_hole();
                    List<CtResult> results = new ArrayList<>();
                    if (null != ct_conclusions && ct_conclusions.size() > 0) {
                        for (int k = 0; k < ct_conclusions.size(); k++) {
                            String conclusionWell = ct_conclusions.get(k).getConclusion();
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<CtResult>> conlusionMap = ct_conclusions.get(k).getConlusionMap();
                                if (conlusionMap.containsKey(hole_Num)) {
                                    results = conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }
                    List<PeakResult> peakResults = new ArrayList<PeakResult>();
                    if (null != peakConclusions && peakConclusions.size() > 0) {
                        for (int k = 0; k < peakConclusions.size(); k++) {
                            String conclusionWell = peakConclusions.get(k).getTupeNo();//getWellNum
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<PeakResult>> peak_conlusionMap = peakConclusions.get(k).getConlusionMap();
                                if (peak_conlusionMap.containsKey(hole_Num)) {
                                    peakResults = peak_conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }

                    SLANExlResult exlResult = new SLANExlResult();
                    exlResult.setChannelNo(curveList.getChannelNo());
                    exlResult.setWellNum(wellName);
                    exlResult.setCurve_hole(hole_Num);
                    exlResult.setResults(results);
                    exlResult.setPeakResults(peakResults);
                    exlResult.setControlRn(curveList.getControlRn());
                    exlResult.setIdCard(curveList.getIdCard());
                    exlResult.setMelt(isMelt);
                    if(null != mThresholds && mThresholds.size()>0){
                        if(mThresholds.containsKey(hole_Num)){
                            Threshold threshold = mThresholds.get(hole_Num);
                            exlResult.setThreshold(threshold.getThreshold());
                            exlResult.setPeekHighThreshold(threshold.getPeekHighThreshold());
                        }
                    }
                    exlResult.setPeakValues(curveList.getPeakValues());
                    exlResult.setCt(curveList.getCt());
                    mExlResults.add(exlResult);
                }

            }
            updateExperiment(experiment,ct_conclusions,peakConclusions);
            return true;
        }
        return false;
    }

    private void updateExperiment(Experiment experiment,
                                  List<CtRuleConclusion>conclusions,
                                  List<PeakRuleConclusion> peakConclusions){
        this.mExperiment = experiment;
        this.ct_Conclusions = conclusions;
        this.peak_Conclusions = peakConclusions;
    }

/////////////////////////////////////////////////////更新实验结果////////////////////////////////////////////////////////

    /**
     * 详情页面二次更新保存结果
     * @param testItem
     * @param startTime
     */
    public void updateDetailExperiment(String testItem,String startTime){
        if(null != mExperiment){
            getExperimentParameter(mExperiment);
            updateSLANExAsyncTask = new UpdateSLANExAsyncTask(isMelt,mThresholds,mExperiment,testItem,startTime,ct_Conclusions,peak_Conclusions);
            updateSLANExAsyncTask.execute();
            updateSLANExAsyncTask.setOnUpdateExListener(new OnUpdateExListener() {
                @Override
                public void onUpdateDbResult(boolean result) {
                    cancelUpdateTask();
                    if(null != onDetailExCallback){
                        if(result){
                            onDetailExCallback.onUpdateFinish(true);
                        }else{
                            onDetailExCallback.onUpdateFinish(false);
                        }

                    }
                }
            });
        }
    }



    UpdateSLANExAsyncTask updateSLANExAsyncTask = null;

    private void cancelUpdateTask(){
        if(null != updateSLANExAsyncTask){
            updateSLANExAsyncTask.cancel(true);
            updateSLANExAsyncTask = null;
        }
    }



    ///////////////////////////////////////////////解析运行中的结果/////////////////////////////////////

    private OnParseRunningExCallBack onParseRunningExCallBack;
    public void setOnParseRunningExCallBack(OnParseRunningExCallBack callBack){
        this.onParseRunningExCallBack = callBack;
    }

    public void releaseParseRunningExCallBack(){
        this.onParseRunningExCallBack = null;
    }
    /**
     * 解析正在运行的结果
     * @param experiment
     */
    public boolean parseSLANRunningEx(Experiment experiment){
        clearAll();
        List<WellResult> wellResults = experiment.getWellResult();
        if (wellResults != null && wellResults.size() > 0) {
            for (WellResult wellResult :wellResults) {
                if (null != wellResult) {
                    List<TargetResult>targetResults = wellResult.getTargetResults();
                    if (targetResults != null && targetResults.size() > 0) {
                        for (int j = 0; j < targetResults.size(); j++) {
                            SLANCurveList mRawList = parseTargetResultToRaw(wellResult.getID(), wellResult.getSampleName(),targetResults.get(j));
                            SLANCurveList mAmpList = parseTargetResultToAMP(wellResult.getID(), wellResult.getSampleName(),targetResults.get(j));
                            SLANCurveList mMeltList = parseTargetResultToMelt(wellResult.getID(), wellResult.getSampleName(),targetResults.get(j));
                            SLANCurveList mPeakList = parseTargetResultToPeak(wellResult.getID(), wellResult.getSampleName(),targetResults.get(j));
                            mAMPCurve.add(mAmpList);
                            mRawCurve.add(mRawList);
                            mPeakCurve.add(mPeakList);
                            mMeltCurve.add(mMeltList);
                        }
                    }
                }
            }

        }
        //解析温度
        ArrayList<Temperature> mTemperatures = experiment.getTemperatures();
        if(null != mTemperatures && mTemperatures.size()>0){
            mExTemperature = parseRunExTemperature(mTemperatures);

        }
        return  true;
    }

    public void parseRunFinishConfigEx(Experiment experiment,boolean isMelt) {
        clearAll();
        List<WellResult> wellResults = experiment.getWellResult();
        if (wellResults != null && wellResults.size() > 0) {
            for (WellResult wellResult : wellResults) {
                if (null != wellResult) {
                    List<TargetResult> targetResults = wellResult.getTargetResults();
                    if (targetResults != null && targetResults.size() > 0) {
                        for (int j = 0; j < targetResults.size(); j++) {

                            SLANCurveList mRawList = parseTargetResultToRaw(wellResult.getID(),  wellResult.getSampleName(),targetResults.get(j));
                            SLANCurveList mAmpList = parseTargetResultToAMP(wellResult.getID(),  wellResult.getSampleName(),targetResults.get(j));
                            SLANCurveList mMeltList = parseTargetResultToMelt(wellResult.getID(),  wellResult.getSampleName(),targetResults.get(j));
                            SLANCurveList mPeakList = parseTargetResultToPeak(wellResult.getID(),  wellResult.getSampleName(),targetResults.get(j));
                            mAMPCurve.add(mAmpList);
                            mRawCurve.add(mRawList);
                            mPeakCurve.add(mPeakList);
                            mMeltCurve.add(mMeltList);
                        }
                    }
                }
            }
        }

        if(isMelt){
            mExlResults.clear();
            for (SLANCurveList curveList : mMeltCurve) {
                String wellName = curveList.getWellNum();
                String hole_Num = curveList.getCurve_hole();
                List<CtResult> results = new ArrayList<>();
                if (null != ct_Conclusions && ct_Conclusions.size() > 0) {
                    for (int k = 0; k < ct_Conclusions.size(); k++) {
                        String conclusionWell = ct_Conclusions.get(k).getTupeNo();
                        //获取每一个孔位的结论
                        if (wellName.contains(conclusionWell)) {
                            Map<String, List<CtResult>> conlusionMap = ct_Conclusions.get(k).getConlusionMap();
                            if (conlusionMap.containsKey(hole_Num)) {
                                results = conlusionMap.get(hole_Num);
                            }

                        }
                    }
                }

                SLANExlResult exlResult = new SLANExlResult();
                exlResult.setWellNum(wellName);
                exlResult.setCurve_hole(hole_Num);
                exlResult.setResults(results);
                exlResult.setControlRn(curveList.getControlRn());
                //  exlResult.setControlCt(curveList.getControlCt());
                exlResult.setMelt(isMelt);

                if(null != mThresholds && mThresholds.size()>0){
                    if(mThresholds.containsKey(hole_Num)){
                        Threshold threshold = mThresholds.get(hole_Num);
                        exlResult.setThreshold(threshold.getThreshold());
                        exlResult.setPeekHighThreshold(threshold.getPeekHighThreshold());
                    }
                }
                exlResult.setPeakValues(curveList.getPeakValues());
                exlResult.setCt(curveList.getCt());
                mExlResults.add(exlResult);
            }
        }else {
            mExlResults.clear();
            for (SLANCurveList curveList : mRawCurve) {
                String wellName = curveList.getWellNum();
                String hole_Num = curveList.getCurve_hole();
                List<CtResult> results = new ArrayList<>();
                if (null != ct_Conclusions && ct_Conclusions.size() > 0) {
                    for (int k = 0; k < ct_Conclusions.size(); k++) {
                        String conclusionWell = ct_Conclusions.get(k).getTupeNo();
                        //获取每一个孔位的结论
                        if (wellName.contains(conclusionWell)) {
                            Map<String, List<CtResult>> conlusionMap = ct_Conclusions.get(k).getConlusionMap();
                            if (conlusionMap.containsKey(hole_Num)) {
                                results = conlusionMap.get(hole_Num);
                            }

                        }
                    }
                }

                SLANExlResult exlResult = new SLANExlResult();
                exlResult.setWellNum(wellName);
                exlResult.setCurve_hole(hole_Num);
                exlResult.setResults(results);
                exlResult.setControlRn(curveList.getControlRn());
                exlResult.setMelt(isMelt);//是否是熔解曲线

                if (null != mThresholds && mThresholds.size() > 0) {
                    if (mThresholds.containsKey(hole_Num)) {
                        Threshold threshold = mThresholds.get(hole_Num);
                        exlResult.setThreshold(threshold.getThreshold());
                        exlResult.setPeekHighThreshold(threshold.getPeekHighThreshold());
                    }
                }
                exlResult.setPeakValues(curveList.getPeakValues());
                exlResult.setCt(curveList.getCt());
                mExlResults.add(exlResult);
            }
        }

        if (null != onParseRunningExCallBack) {
            onParseRunningExCallBack.onParseExFinish();
        }
    }

    //////////////////////////////////////////////上传数据至服务器///////////////////////////////////////////////////////

    OnUploadCallBack onUploadCallBack;

    public void setOnUploadCallBack(OnUploadCallBack onUploadCallBack) {
        this.onUploadCallBack = onUploadCallBack;
    }

    private static final String UPLOAD_URL = "http://192.168.1.104:80/upload";

   /* *//**
     * 上传数据
     * @param experiment
     *//*
    public void uploadSLANResult(Experiment experiment,
                                 File file,
                                 String uploadUrl){
        Log.i("BBB","uploadUrl" + uploadUrl);
        // 判断网络
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();

        try {
            String uploadMsg = gson.toJson(experiment);
            if(!StringUtil.isEmpty(uploadMsg)) {
                okhttp3.OkHttpClient okHttpClient = OkHttpUtil.getUnsafeOkHttpClient();

                MediaType JSON = MediaType.parse("application/json");
                //创建长数据部分
                RequestBody requestBody = RequestBody.create(JSON, uploadMsg);

               //MultipartBody.Part longDataPart = MultipartBody.Part
               //         .createFormData("longData", UUID.randomUUID().toString(), requestBody);

                //创建文件部分

               // RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"),file);

                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .post(requestBody)
                        .build();

                okHttpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.i("BBB","onFailure" + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String result = response.body().string();
                      *//*  ResultMsg resultMsg = new Gson().fromJson(result, ResultMsg.class);
                        if (resultMsg.Result.equals("1")) {
                           // Log.d(TAG, "run: 上传成功");
                        } else {

                        }*//*
                        Log.i("BBB","上传成功"+ "result" + result);
                      if(null != onUploadCallBack){
                          onUploadCallBack.onUploadResult(true,result);
                      }
                    }
                });
            }
        }catch (Exception e) {
                // TODO: handle exception

        }
    }*/

    /**
     * 通过HTTP post 请求上传至服务器
     *//*
    public void uploadFile(File file){
        //先判断文件是否存在
        try{
            okhttp3.OkHttpClient okHttpClient = OkHttpUtil.getUnsafeOkHttpClient();
            MediaType type = MediaType.parse("multipart/form-data");
            RequestBody fileBody = RequestBody.create(type, file);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(UPLOAD_URL)
                    .post(requestBody)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.i("upload","上传失败");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String result = response.body().string();
                      Log.i("upload","上传成功");
                    if(null != onUploadCallBack){
                        onUploadCallBack.onUploadResult(true,result);
                    }
                }
            });
        }catch (Exception e){

        }
    }*/



////////////////////////////////////////////保存Experiment对象为Json文件//////////////////////////

    /**
     * 运行界面保存Experiment对象
     * @param experiment
     * @param startTime
     * @param subFirName
     */
    public void saveExperimentToJson(Experiment experiment,String startTime, String subFirName){
        if(null == saveJSONAsyncTask){
            saveJSONAsyncTask = new SaveSLANJsonAsyncTask(experiment,subFirName);
            saveJSONAsyncTask.execute();
            saveJSONAsyncTask.setOnSaveExperimentListener(new OnSaveExperimentListener() {
                @Override
                public void onSaveSuccess() {
                    cancelJSONTask();
                    if(null != onExperimentCallback){
                        onExperimentCallback.onSaveJsonResult(true,"保存PCR json文件成功");
                    }
                }

                @Override
                public void onSaveFail() {
                    cancelJSONTask();
                    if(null != onExperimentCallback){
                        onExperimentCallback.onSaveJsonResult(false,"保存PCR json文件失败");
                    }
                }
            });
        }
    }

    /**
     * 详情界面保存Experiment对象
     * @param experiment
     *
     * @param saveFolderPath 文件夹路径
     * @return
     */
    public void saveDetailExToJson(Experiment experiment, String saveFolderPath){
        if(null == saveJSONAsyncTask){
            saveJSONAsyncTask = new SaveSLANJsonAsyncTask(experiment,saveFolderPath);
            saveJSONAsyncTask.execute();
            saveJSONAsyncTask.setOnSaveExperimentListener(new OnSaveExperimentListener() {
                @Override
                public void onSaveSuccess() {
                    cancelJSONTask();
                    if(null != onDetailExCallback){
                        onDetailExCallback.onSaveJsonResult(true,"save json success");
                    }
                }

                @Override
                public void onSaveFail() {
                    cancelJSONTask();
                    if(null != onDetailExCallback){
                        onDetailExCallback.onSaveJsonResult(false,"save json fail");
                    }
                }
            });
        }
    }


    /**
     * 保存Expriment为json
     * @param subFirName 子文件夹名称
     */
    private SaveSLANJsonAsyncTask saveJSONAsyncTask = null;

    private void cancelJSONTask(){
        if(null != saveJSONAsyncTask){
            saveJSONAsyncTask.cancel(true);
            saveJSONAsyncTask = null;
        }
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 根据通道返回对应通道的参数
     * @param configTargets
     * @param channelNo
     * @return
     */
    private ConfigTarget getConfigTarget(List<ConfigTarget> configTargets,int channelNo){
        if(null != configTargets && configTargets.size()>0){
            for(ConfigTarget configTarget :configTargets){
                if(configTarget.getChannelNo() == channelNo){
                    return configTarget;
                }
            }
        }
        return null;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * 复制实验结果为.PCR文件
     * @param experiment
     * @param savePcrFileFolder
     * @return
     */
    public boolean saveResultToPCR(Experiment experiment,
                                   String savePcrFileFolder) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false ;
        }
        if (experiment == null) {
            return false;
        }
        if(null == PcrApplication.slanService){
            return false;
        }

        File dir = new File(savePcrFileFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String saveFileName = DateFormatUtil.getFileName() + ".pcr";
        String savePath = savePcrFileFolder;
        boolean save_result = PcrApplication.slanService.saveExperiment(experiment.getName(), savePath + saveFileName,false);

        /*if(null != ct_Conclusions && null != peak_Conclusions){
            Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();
            JudgeConclusion judgeConclusion = new JudgeConclusion();
            judgeConclusion.setTemplateName(testItem);
            judgeConclusion.setCtRuleConclusions(ct_Conclusions);
            judgeConclusion.setPeakRuleConclusions(peak_Conclusions);
            String judgeConclusionResult = gson.toJson(judgeConclusion);
            File judgeConclusionFile = FileUtils.dumpDeviceInfo(judgeConclusionResult, "judgeConclusion.result",dir);
        }*/

        return save_result;
    }

    //==========================================根据判别文件获取判别结论================================================
    /**
     * 获取所有孔位的 CT 判断结果列表
     * @param folderPath 模板路径，不需要/
     */
    public List<CtRuleConclusion> getAllCtResults(Experiment experiment, String folderPath, String startTime) {
        List<CtRuleConclusion> allCtResults = new ArrayList<>();

        for (WellResult well : experiment.getWellResult()) {
            String wellId = well.getID().trim().toUpperCase();
            int holeNum = getHoleNumberByWellId(wellId);
            File ctFile = new File(folderPath, "hole" + holeNum + ".ct");

            CtRuleConclusion ctConclusion = judgeSingleWellCt(well, ctFile, startTime);
            if (ctConclusion != null) {
                allCtResults.add(ctConclusion);
            }
        }
        return allCtResults;
    }

    /**
     * 获取所有孔位的 Peak 判断结果列表
     */
    public List<PeakRuleConclusion> getAllPeakResults(Experiment experiment, String folderPath, String startTime) {
        // 初始化空集合（线程安全/空指针防护）
        List<PeakRuleConclusion> allPeakResults = new ArrayList<>();

        // 1. 核心空值防护：避免 experiment 或 well 列表为 null 导致崩溃
        if (experiment == null || experiment.getWellResult() == null || experiment.getWellResult().isEmpty()) {
            Log.w("SLANManager", "实验数据为空，无孔结果可处理");
            return allPeakResults;
        }

        // 2. 校验文件夹路径是否有效
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            Log.e("SLANManager", "峰结果文件夹不存在或不是目录：" + folderPath);
            return allPeakResults;
        }

        // 3. 遍历每个孔结果
        for (WellResult well : experiment.getWellResult()) {
            // 孔对象空值防护
            if (well == null) {
                Log.w("SLANManager", "遍历到空的 WellResult 对象，跳过");
                continue;
            }

            // 4. 处理孔 ID（trim + 大写，和你原有逻辑一致）
            String wellId = well.getID();//A1，A2
            if (wellId == null || wellId.trim().isEmpty()) {
                Log.w("SLANManager", "孔 ID 为空，跳过当前孔");
                continue;
            }
            wellId = wellId.trim().toUpperCase();

            // 5. 获取孔号（增加异常防护，防止解析失败）
            int holeNum;
            try {
                holeNum = getHoleNumberByWellId(wellId);
            } catch (Exception e) {
                Log.e("SLANManager", "解析孔号失败，wellId：" + wellId, e);
                continue;
            }
            Log.i("SLANManager", "wellId=" + wellId + "，holeNum=" + holeNum);

            // 6. 拼接峰文件路径（安全拼接，避免路径错误）
            File peakFile = new File(folder, "hole" + holeNum + ".peak");

            // 7. 校验峰文件是否存在
            if (!peakFile.exists() || !peakFile.isFile()) {
                Log.w("SLANManager", "峰文件不存在：" + peakFile.getAbsolutePath());
                continue;
            }

            // 8. 执行峰判断逻辑（核心业务）
            try {
                PeakRuleConclusion peakConclusion = judgeSingleWellPeak(well, peakFile, startTime);
                // 非空才加入结果集
                if (peakConclusion != null) {
                    allPeakResults.add(peakConclusion);
                    Log.i("SLANManager", "孔 " + wellId + " 峰结论生成成功");
                } else {
                    Log.w("SLANManager", "孔 " + wellId + " 未生成峰结论");
                }
            } catch (Exception e) {
                Log.e("SLANManager", "处理孔 " + wellId + " 峰结论时发生异常", e);
            }
        }

        Log.i("SLANManager", "所有孔处理完成，有效峰结论总数：" + allPeakResults.size());
        return allPeakResults;
    }

    /* 【核心匹配逻辑】

     * B1,B2,B3,B4,B5,B6,B7,B8 → hole2
     * A1,A2,A3,A4,A5,A6,A7,A8 → hole1
     */
    private int getHoleNumberByWellId(String wellId) {
        if (wellId == null || wellId.length() < 2) {
            return -1;
        }

        char row = wellId.charAt(0);
        int col;
        try {
            col = Integer.parseInt(wellId.substring(1));
        } catch (Exception e) {
            return -1;
        }

        // 验证列号是否在1-8范围内
        if (col < 1 || col > 8) {
            return -1;
        }

        if (row == 'A') {
            return 1;  // A1-A8 全部对应孔1
        } else if (row == 'B') {
            return 2;  // B1-B8 全部对应孔2
        }

        return -1;
    }

    //====================================单个孔CT判断===================//
    private CtRuleConclusion judgeSingleWellCt(WellResult well,File ctFile,String startTime){
        // 修复：与 PEAK 读取保持一致,改用 Gson TypeToken,避免 fastjson 与 Gson 不兼容
        List<CTRuleEntity> ruleList = readJsonFile(ctFile,
                new com.google.gson.reflect.TypeToken<List<CTRuleEntity>>() {}) ;
        if(ruleList ==null || ruleList.isEmpty()){
            return null;
        }
        //重置状态
        for(CTRuleEntity rule :ruleList){
            rule.setPostive(false);
            rule.setSuspicios(false);
        }
        //匹配通道并判断
        for(TargetResult target:well.getTargetResults()){
            String targetCh = target.getName().toLowerCase();
            for(CTRuleEntity rule :ruleList){
                if(targetCh.contains(rule.getChannel().toLowerCase())){
                    double ct = target.getCt();
                    double base = rule.getCTvalue();
                    double down = base - rule.getCTvalueDown();
                    double up = base + rule.getCTvalueUP();
                    double susUp = up+rule.getCTvalueUP_Suspicious();

                    rule.setActualCt(ct);
                    rule.setCTvalueDown(down);
                    rule.setCTvalueUP(up);
                    rule.setCTvalueUP_Suspicious(susUp);
                    if(ct>=down && ct<=up){
                        rule.setPostive(true);
                    }else if(ct>up && ct<= susUp){
                        rule.setSuspicios(true);
                    }
                }
            }
        }
        //组装结论
        Map<String,List<CtResult>> map = new HashMap<>();
        List<String> texts = new ArrayList<>();
        for(CTRuleEntity rule : ruleList){
            CtResult res = new CtResult();
            res.setCategory(rule.getCategory());
            res.setActualCt(rule.getActualCt());
            res.setValue(rule.getCTvalue());
            res.setCtValueDown(rule.getCTvalueDown());
            res.setCtValueUp(rule.getCTvalueUP());
            res.setCtValueUp_Suspicious(rule.getCTvalueUP_Suspicious());
            if(rule.isPostive()){
                res.setConclusion("阳性");
                texts.add(rule.getCategory()+":阳性");
            }else if(rule.isSuspicios()){
                res.setConclusion("可疑");
                texts.add(rule.getCategory()+":可疑");
            }else{
                res.setConclusion("阴性");
                texts.add(rule.getCategory()+":阴性");
            }
            map.computeIfAbsent(rule.getChannel().toUpperCase(), k -> new ArrayList<>()).add(res);
        }
        CtRuleConclusion conclusion = new CtRuleConclusion();
        conclusion.setTupeNo(well.getID());
        conclusion.setSampleID(well.getSampleName());
        conclusion.setTime(startTime);
        conclusion.setConlusionMap(map);
        conclusion.setWellConclusion(texts);
        return conclusion;
    }

    private PeakRuleConclusion judgeSingleWellPeak(WellResult well, File peakFile, String startTime) {
        // 1. 读取规则文件
        // 修复：原代码用 fastjson 的 TypeReference,改为 Gson 的 TypeToken 与 readJsonFile 新签名一致
        List<PEAKRuleEntity> ruleList = readJsonFile(peakFile,
                new com.google.gson.reflect.TypeToken<List<PEAKRuleEntity>>() {});
        if (ruleList == null || ruleList.isEmpty()) {
            return null;
        }

        // 2. 重置所有规则为阴性（正确）
        for (PEAKRuleEntity rule : ruleList) {
            if (rule.getPEAKSingleRuleEntityList() == null) continue;
            for (PEAKSingleRuleEntity s : rule.getPEAKSingleRuleEntityList()) {
                s.setPostive(false);
                s.setActualTM(0.0);
                s.setActualRM(0.0);
            }
        }

        // 3. 遍历每个目标通道，匹配规则并判断峰
        if (well.getTargetResults() == null || well.getTargetResults().isEmpty()) {
            // 没有目标结果 → 所有规则保持阴性，不修改
        } else {
            for (TargetResult target : well.getTargetResults()) {
                if (target.getName() == null || target.getName().isEmpty()) continue;

                String targetCh = target.getName().toLowerCase();

                // 匹配规则通道
                for (PEAKRuleEntity rule : ruleList) {
                    if (rule.getChannel() == null) continue;
                    if (!targetCh.contains(rule.getChannel().toLowerCase())) {
                        continue; // 通道不匹配，直接跳过
                    }

                    // 通道匹配 → 开始判断峰
                    for (PEAKSingleRuleEntity singleRule : rule.getPEAKSingleRuleEntityList()) {
                        // ==============================================
                        // 【关键修复1】每次都用原始基准值计算，不修改实体！
                        // ==============================================
                        double baseTm = singleRule.getPEAKvalue();
                        double downRange = baseTm - singleRule.getPEAKvalueDown();
                        double upRange = baseTm + singleRule.getPEAKvalueUP();
                        double requireRm = singleRule.getPEAKvalueRM();

                        // 如果没有峰，直接跳过判断
                        if (target.getPeakValues() == null || target.getPeakValues().isEmpty()) {
                            continue;
                        }

                        // ==============================================
                        // 【关键修复2】只要有一个峰符合 → 阳性，不覆盖！
                        // ==============================================
                        boolean isPositive = false;
                        double lastTm = 0.0;
                        double lastRm = 0.0;

                        for (MeltPeak p : target.getPeakValues()) {
                            lastTm = p.getTm();
                            lastRm = p.getRm();

                            // 判断条件：Tm在区间内 + RM达标
                            if (p.getTm() >= downRange && p.getTm() <= upRange && p.getRm() >= requireRm) {
                                isPositive = true;
                                // 找到一个符合就可以退出，提升效率
                                break;
                            }
                        }

                        // 最终赋值（只写一次，不循环覆盖）
                        singleRule.setActualTM(lastTm);
                        singleRule.setActualRM(lastRm);
                        if (isPositive) {
                            singleRule.setPostive(true);
                        }
                    }
                }
            }
        }

        // 4. 组装结论（修复：只保留有意义的规则结果）
        Map<String, List<PeakResult>> map = new HashMap<>();
        List<String> texts = new ArrayList<>();

        for (PEAKRuleEntity rule : ruleList) {
            if (rule.getPEAKSingleRuleEntityList() == null) continue;

            for (PEAKSingleRuleEntity s : rule.getPEAKSingleRuleEntityList()) {
                PeakResult res = new PeakResult();
                res.setCategory(s.getCategory());
                res.setActualTM(s.getActualTM());
                res.setActualRM(s.getActualRM());
                res.setValue(s.getPEAKvalue());
                res.setPeakValueDown(s.getPEAKvalueDown());
                res.setPeakValueUp(s.getPEAKvalueUP());
                res.setPeakValueRM(s.getPEAKvalueRM());
                res.setConclusion(s.isPostive() ? "阳性" : "阴性");

                texts.add(s.getCategory() + ":" + res.getConclusion());
                map.computeIfAbsent(rule.getChannel().toUpperCase(), k -> new ArrayList<>()).add(res);
            }
        }

        // 5. 返回最终结论
        PeakRuleConclusion conclusion = new PeakRuleConclusion();
        conclusion.setTupeNo(well.getID());
        conclusion.setSampleID(well.getSampleName());
        conclusion.setTime(startTime);
        conclusion.setConlusionMap(map);
        conclusion.setWellConclusion(texts);
        return conclusion;
    }

    //===================================工具方法================================//
    /**
     * 修复：原实现用 fastjson + Files.readAllBytes + 平台默认编码,在三种场景下会崩溃:
     * 1. peak 文件存在但是 0 字节(模板刚创建,未写入规则),fastjson.parseObject("", ...) 抛异常
     * 2. fastjson 与工程其他位置统一的 Gson 不一致,Double 自定义序列化结果不兼容
     * 3. 未指定 UTF-8 编码,理论上可能踩到 BOM
     *
     * 修复方案：与 PEAKRuleEntity.getPEAKRuleList / PeakJudgeActivity 一致,使用 Gson + Double 自定义序列化器
     */
    private <T> T readJsonFile(File file, com.google.gson.reflect.TypeToken<T> type) {
        try {
            // 1. 空文件保护：peak 文件刚 createNewFile() 时是 0 字节,以前会直接抛异常
            if (file == null || !file.exists() || file.length() == 0) {
                Log.w("SLANManager", "readJsonFile: 文件为空或不存在: " + (file == null ? "null" : file.getAbsolutePath()));
                return null;
            }

            // 2. 显式指定 UTF-8 编码,避免依赖平台默认编码
            String json = new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);

            // 3. 空字符串保护：理论上 length() > 0 已经过滤,但保留防御性检查
            if (json == null || json.trim().isEmpty()) {
                Log.w("SLANManager", "readJsonFile: JSON 内容为空: " + file.getAbsolutePath());
                return null;
            }

            // 4. 与工程其他位置统一使用 Gson + Double 自定义序列化器
            //    (Double 整数时输出 longValue,避免出现 10.0 vs 10 的差异)
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
                        @Override
                        public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                            if (src == src.longValue())
                                return new JsonPrimitive(src.longValue());
                            return new JsonPrimitive(src);
                        }
                    })
                    .create();

            return gson.fromJson(json, type.getType());
        } catch (Exception e) {
            Log.e("SLANManager", "readJsonFile 解析失败: " + file.getAbsolutePath() + " err=" + e.getMessage());
            // 不再 e.printStackTrace(),改为 Log.e,避免刷屏
            return null;
        }
    }

}
