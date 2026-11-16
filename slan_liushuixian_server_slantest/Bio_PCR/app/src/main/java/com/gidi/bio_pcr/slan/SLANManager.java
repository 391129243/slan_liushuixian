package com.gidi.bio_pcr.slan;


import android.os.Environment;
import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.CTRuleEntity;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKRuleEntity;
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
     * @param configTargets true是自动阈值，false手动阈值
     */
    public void secondSLANExAnalyse(List<ConfigTarget> configTargets,
                                    int AMPAlgorithm,
                                    File ctFile,
                                    File peakFile,
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
                       List<CtRuleConclusion> conclusions = new ArrayList<>();
                       List<PeakRuleConclusion> peakConclusions = new ArrayList<>();
                       if(null != ctFile){
                           conclusions = judgeCTFile(sec_experiment, ctFile,startTime);
                       }
                       if(null != peakFile){
                           peakConclusions = judgePEAKFile(sec_experiment, peakFile,startTime);
                       }
                       boolean result = parseSecondExperiment(sec_experiment,conclusions,peakConclusions);
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
}
