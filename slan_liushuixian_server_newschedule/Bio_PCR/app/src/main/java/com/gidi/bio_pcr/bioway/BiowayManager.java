package com.gidi.bio_pcr.bioway;


import static java.lang.Double.NaN;

import android.os.Environment;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.CTRuleEntity;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKRuleEntity;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.CtRuleConclusion;
import com.gidi.bio_pcr.bean.ExTemperature;
import com.gidi.bio_pcr.bean.JudgeConclusion;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.PeakRuleConclusion;
import com.gidi.bio_pcr.bean.PeaktypeOptimization;
import com.gidi.bio_pcr.bean.Threshold;
import com.gidi.bio_pcr.bioway.asynctask.SaveBioWayExTask;
import com.gidi.bio_pcr.bioway.asynctask.UpdateBiowayExTask;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.pcr.OnDetailExCallback;
import com.gidi.bio_pcr.pcr.OnExperimentCallback;
import com.gidi.bio_pcr.pcr.OnParseRunningExCallBack;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.pcr.update.OnUpdateExListener;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.github.mikephil.charting.data.Entry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hongshi.slansdk.common.HSUtils;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Dot;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.ExperimentData.Peak;
import com.winters.boweilibrary.BoWei.ExperimentData.TargetResult;
import com.winters.boweilibrary.BoWei.ExperimentData.Temperature;
import com.winters.boweilibrary.BoWei.ExperimentData.WellResult;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.winters.boweilibrary.BoWei.bean.BiowayExlResult;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BiowayManager {

    private List<BiowayCurveList> mRawCurve = new ArrayList<BiowayCurveList>();
    private List<BiowayCurveList> mMeltCurve = new ArrayList<BiowayCurveList>();
    private List<BiowayCurveList> mAMPCurve = new ArrayList<BiowayCurveList>();
    private List<BiowayCurveList> mPeakCurve = new ArrayList<BiowayCurveList>();
    private List<BiowayExlResult> mExlResults = new ArrayList<BiowayExlResult>();
    private Map<String,Threshold> mThresholds = new HashMap<String,Threshold>();
    private List<CtRuleConclusion> ct_Conclusions = new ArrayList<CtRuleConclusion>();
    private List<PeakRuleConclusion> peak_Conclusions = new ArrayList<PeakRuleConclusion>();

    private ArrayList<ExTemperature> mExTemperature = new ArrayList<ExTemperature>();
    private boolean isMelt = false;
    private static BiowayManager mInstance;

    private OnExperimentCallback onExperimentCallback;
    public void setOnExperimentCallback(OnExperimentCallback callback){
       this.onExperimentCallback = callback;
    }
    public void releaseExperimentCallBack(){
        this.onExperimentCallback = null;
    }

    //数据库记录界面回调
    private OnDetailExCallback onDetailExCallback;
    public void setOnDetailExCallback(OnDetailExCallback callback){
        this.onDetailExCallback = callback;
    }
    public void releaseOnDetailExCallback(){
        this.onDetailExCallback = null;
    }

    private Experiment mExperiment;
    public static BiowayManager getInstance() {
        if (mInstance == null) {
            mInstance = new BiowayManager();
        }
        return mInstance;
    }

    public List<BiowayCurveList> getRawCurve() {
        return mRawCurve;
    }

    public List<BiowayCurveList> getMeltCurve() {
        return mMeltCurve;
    }

    public List<BiowayCurveList> getAMPCurve() {
        return mAMPCurve;
    }

    public void setAMPCurve(List<BiowayCurveList> mAMPCurve) {
        this.mAMPCurve = mAMPCurve;
    }

    public List<BiowayCurveList> getPeakCurve() {
        return mPeakCurve;
    }

    public Experiment getExperiment() {
        return mExperiment;
    }

    public void setExperiment(Experiment mExperiment) {
        this.mExperiment = mExperiment;
    }

    public ArrayList<ExTemperature> getExTemperature() {
        return mExTemperature;
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

    private SaveBioWayExTask saveExToDbAsyncTask;
    //////////////////////////////////////////////保存////////////////////////////////////////////////////

    /**
     * 保存数据库
     * @param experiment
     * @param filePath 文件夹路径，不是全部的路径
     * @param startTime
     * @param endTime
     * @param templateDir
     * @param ct_conclusions
     * @param peakConclusions
     */
    public void saveBiowayExResultToDB(Experiment experiment,
                                       String filePath,
                                       String startTime,
                                       String endTime,
                                       String templateDir,
                                       String description,
                                       List<CtRuleConclusion> ct_conclusions,
                                       List<PeakRuleConclusion> peakConclusions){

        getExperimentParamter(experiment);
        if(this.mExperiment == null){
            this.mExperiment = experiment;
        }
        Log.i("BioWay","保存数据库this.mExperiment" + this.mExperiment.getName());
        saveExToDbAsyncTask = new SaveBioWayExTask(isMelt,mThresholds,mExperiment,filePath, startTime,endTime,
                templateDir,description,ct_conclusions,peakConclusions);
        saveExToDbAsyncTask.execute();
        saveExToDbAsyncTask.setOnSaveExperimentListener(new OnSaveExperimentListener() {
            @Override
            public void onSaveSuccess() {
                if(onExperimentCallback != null){
                    onExperimentCallback.onSaveExToDbResult(true);
                }
                cancelTask();
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
        if(null != saveExToDbAsyncTask){
            saveExToDbAsyncTask.cancel(true);
            saveExToDbAsyncTask = null;
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


    public void parseExperimentResult(Experiment experiment,
                                      List<CtRuleConclusion> conclusions,
                                      List<PeakRuleConclusion> peakConclusions) {
        clearAll();
        mExlResults.clear();

        //获取
        this.mExperiment = experiment;
        getExperimentParamter(experiment);
        List<WellResult> wellResults = experiment.getWellResult();
        if (wellResults != null && wellResults.size() > 0) {
            for (WellResult wellResult : wellResults) {
                if (null != wellResult) {
                    List<TargetResult> targetResults = wellResult.getTargetResults();
                    if (targetResults != null && targetResults.size() > 0) {

                        for (TargetResult targetResult : targetResults) {
                            BiowayCurveList mRawList = parseTargetResultToRaw(wellResult.getID(),wellResult.getSampleName(), targetResult);
                            BiowayCurveList mAmpList = parseTargetResultToAMP(wellResult.getID(),wellResult.getSampleName(), targetResult);
                            BiowayCurveList mMeltList = parseTargetResultToMelt(wellResult.getID(),wellResult.getSampleName(), targetResult);
                            BiowayCurveList mPeakList = parseTargetResultToPeak(wellResult.getID(),wellResult.getSampleName(), targetResult);
                            mAMPCurve.add(mAmpList);
                            mRawCurve.add(mRawList);
                            mPeakCurve.add(mPeakList);
                            mMeltCurve.add(mMeltList);

                        }
                    }
                }
            }
            Log.i("BioWay","mAMPCurve" + mAMPCurve.size() );
            Log.i("BioWay","mRawCurve" + mRawCurve.size() );
            Log.i("BioWay","mPeakCurve" + mPeakCurve.size() );
            Log.i("BioWay","mMeltCurve" + mMeltCurve.size() );

            if (mRawCurve != null && mRawCurve.size() != 0) {
                mExlResults.clear();
                for (BiowayCurveList curveList : mRawCurve) {
                    String wellName = curveList.getWellNum();
                    String hole_Num = curveList.getCurve_hole();//"FAM",CY5,HEX,ROX
                    List<CtResult> results = new ArrayList<>();
                    if (null != conclusions && conclusions.size() > 0) {
                        for (int k = 0; k < conclusions.size(); k++) {
                            String conclusionWell = conclusions.get(k).getTupeNo();//getWellNum
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<CtResult>> conlusionMap = conclusions.get(k).getConlusionMap();
                                if (conlusionMap.containsKey(hole_Num)) {
                                    results = conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }
                    List<PeakResult> peakResults = new ArrayList<>();
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

                    BiowayExlResult exlResult = new BiowayExlResult();
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
                for (BiowayCurveList curveList : mMeltCurve) {
                    String wellName = curveList.getWellNum();
                    String hole_Num = curveList.getCurve_hole();//rox。fam。。
                    List<CtResult> results = new ArrayList<>();
                    if (null != conclusions && conclusions.size() > 0) {
                        for (int k = 0; k < conclusions.size(); k++) {
                            String conclusionWell = conclusions.get(k).getTupeNo();//getWellNum
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<CtResult>> conlusionMap = conclusions.get(k).getConlusionMap();
                                if (conlusionMap.containsKey(hole_Num)) {
                                    results = conlusionMap.get(hole_Num);
                                }

                            }
                        }
                    }
                    List<PeakResult> peakResults = new ArrayList<>();
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
                    BiowayExlResult exlResult = new BiowayExlResult();
                    exlResult.setChannelNo(curveList.getChannelNo());
                    exlResult.setWellNum(wellName);
                    exlResult.setCurve_hole(hole_Num);
                    exlResult.setResults(results);
                    exlResult.setPeakResults(peakResults);
                    exlResult.setMelt(isMelt);//溶解曲綫
                    exlResult.setControlRn(curveList.getControlRn());
                    exlResult.setPeakValues(curveList.getPeakValues());
                    exlResult.setCt(curveList.getCt());

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

            if(onExperimentCallback != null){
                onExperimentCallback.onParseFinish();
            }
        }
    }

    public BiowayCurveList parseTargetResultToRaw(String wellName,
                                                  String idCard,
                                                  TargetResult targetResult) {

        BiowayCurveList curve = new BiowayCurveList();
        if (null != targetResult) {
            List<Entry> entries = new ArrayList<Entry>();
            if (null != targetResult.getRawCurve() ) {
                List<Double> RawCurve = targetResult.getRawCurve();
                if(RawCurve.size()>0){
                    for(int i=0;i<RawCurve.size(); i++){
                        Entry entry = new Entry(i,
                                Float.parseFloat(HSUtils.INSTANCE.roundDouble(RawCurve.get(i), 4) + ""));//曲线值
                        entries.add(entry);
                    }
                }
            }

            curve.setCurve_data(entries);
            curve.setWellNum(wellName);
            curve.setChannelNo(targetResult.getChannel());
            curve.setCurve_hole(targetResult.getName());//ROX/
            curve.setCt(targetResult.getCt());
            curve.setControlRn(targetResult.getRn());
            curve.setIdCard(idCard);
            if(null == targetResult.getPeakValues()){
                List<Peak> peakValues = new ArrayList<Peak>();
                curve.setPeakValues(peakValues);
            }else{
                List<Peak> peakValues = targetResult.getPeakValues();
                curve.setPeakValues(peakValues);
            }
        }

        return curve;
    }

    /**
     * 扩增曲线数据
     * @param targetResult
     * @return
     */
    public BiowayCurveList parseTargetResultToAMP(String wellName,
                                                  String idCard,
                                                  TargetResult targetResult){
        BiowayCurveList curve = new BiowayCurveList();
        if (null != targetResult) {
            List<Entry> entries = new ArrayList<Entry>();
            if (null != targetResult.getAmpCurve() ) {
                List<Double> AMPCurve = targetResult.getAmpCurve();
                if(AMPCurve.size()>0){
                    for(int i=0;i<AMPCurve.size();i++){
                        Entry entry = new Entry(i,
                                StringUtil.DoubleToFloat(AMPCurve.get(i),0.0000f));
                               // Float.parseFloat(HSUtils.INSTANCE.roundDouble(AMPCurve.get(i), 4) + ""));//曲线值
                        entries.add(entry);
                    }

                }
            }
            curve.setCurve_data(entries);
            curve.setWellNum(wellName);
            curve.setChannelNo(targetResult.getChannel());
            curve.setCurve_hole(targetResult.getName());//ROX/
            curve.setCt(targetResult.getCt());
            curve.setControlRn(targetResult.getRn());
            curve.setIdCard(idCard);
            if(null == targetResult.getPeakValues()){
                List<Peak> peakValues = new ArrayList<Peak>();
                curve.setPeakValues(peakValues);
            }else{
                List<Peak> peakValues = targetResult.getPeakValues();
                curve.setPeakValues(peakValues);
            }
        }
        return curve;
    }


    /**
     * 获取熔解曲线数据
     */
    public BiowayCurveList parseTargetResultToMelt(String wellName,
                                                   String idCard,
                                                   TargetResult targetResult){

        BiowayCurveList curve = new BiowayCurveList();
        if (null != targetResult) {
            List<Entry> entries = new ArrayList<Entry>();
            if (null != targetResult.getRawMeltCurve() ) {
                List<Dot> RawMeltCurve = targetResult.getRawMeltCurve();
              //  Log.i("BiowayManager","parseTargetResultToMelt-- RawMeltCurve" + RawMeltCurve.size());
                if(RawMeltCurve.size()>0){
                    for (Dot c : RawMeltCurve) {
                        if(c != null){
                            Entry entry = new Entry(Float.parseFloat(String.valueOf(c.getX())),
                                    Float.parseFloat(HSUtils.INSTANCE.roundDouble(c.getY(), 4) + ""));//曲线值
                            entries.add(entry);

                        }
                    }
                }
            }
            Log.i("BiowayManager","parseTargetResultToMelt entries" + entries.size());
            curve.setCurve_data(entries);
            curve.setWellNum(wellName);
            curve.setChannelNo(targetResult.getChannel());
            curve.setCurve_hole(targetResult.getName());//ROX/
            curve.setCt(targetResult.getCt());
            curve.setControlRn(targetResult.getRn());
            curve.setIdCard(idCard);
            if(null == targetResult.getPeakValues()){
                List<Peak> peakValues = new ArrayList<Peak>();
                curve.setPeakValues(peakValues);
            }else{
                List<Peak> peakValues = targetResult.getPeakValues();
                curve.setPeakValues(peakValues);
            }
        }
        return curve;
    }

    public BiowayCurveList parseTargetResultToPeak(String wellName,
                                                   String idCard,
                                                   TargetResult targetResult){

        BiowayCurveList curve = new BiowayCurveList();
        if (null != targetResult) {
            List<Entry> entries = new ArrayList<Entry>();
            if (null != targetResult.getPeakCurve()) {
                List<Dot> peakCurve = targetResult.getPeakCurve();
                if(peakCurve.size()>0){
                    for (Dot c : peakCurve) {
                        if(c != null){
                            if(c.getY() != NaN){
                                Entry entry = new Entry(Float.parseFloat(String.valueOf(c.getX())),
                                        Float.parseFloat(HSUtils.INSTANCE.roundDouble(c.getY(), 4) + ""));//曲线值
                                entries.add(entry);
                            }else{
                                Entry entry = new Entry(Float.parseFloat(String.valueOf(c.getX())),
                                        Float.parseFloat(HSUtils.INSTANCE.roundDouble(0.0, 4) + ""));//曲线值
                                entries.add(entry);
                            }

                        }
                    }
                }

            }
            curve.setCurve_data(entries);
            curve.setWellNum(wellName);
            curve.setChannelNo(targetResult.getChannel());
            curve.setCurve_hole(targetResult.getName());//ROX/
            curve.setCt(targetResult.getCt());
            curve.setControlRn(targetResult.getRn());
            curve.setIdCard(idCard);
            if(null == targetResult.getPeakValues()){
                List<Peak> peakValues = new ArrayList<Peak>();
                curve.setPeakValues(peakValues);
            }else{
                List<Peak> peakValues = targetResult.getPeakValues();
                curve.setPeakValues(peakValues);
            }
        }

        return curve;
    }

    private void getExperimentParamter(Experiment experiment) {
        isMelt = false;
        mThresholds.clear();
        if (null != BoWeiService.getInstance()) {
            if (null != experiment) {
                ArrayList<AnalyseParamter> analyseParamters = BoWeiService.getInstance()
                        .getExperimentParamter(experiment);
                if (analyseParamters != null && analyseParamters.size() > 0) {
                    Log.i("BioWayManager","getExperimentParamter" );
                    ArrayList<Target> targets = analyseParamters.get(0).getTargets();
                    if (null != targets && targets.size() > 0) {
                        for (Target target : targets) {
                            if (target.getChannelNo()==1) {
                                TargetParamter famParamer = target.getParamter();
                                Log.i("BioWayManager","famParamer" + famParamer.getThreshold());
                                Log.i("BioWayManager","getEndBaseline" + famParamer.getEndBaseline());
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
                            }
                        }
                    }
                }

            }
        }
    }



    public List<BiowayExlResult> getExlResults() {
        return mExlResults;
    }

    /*****************************************二次分析*****************************************/
    /**
     * 二次分析
     * @param configTargets 参数设置值
     * @param optimizations 峰型优化
     * @param ctFile        扩增判别文件
     * @param peakFile      熔解判别文件
     */
    public void secondSLANRunExAnalyse(List<ConfigTarget> configTargets,
                                       List<PeaktypeOptimization> optimizations,
                                       int AMPAlgorithm,
                                       File ctFile,
                                       File peakFile,
                                       String startTime){
        //获取实验参数
        Log.i("BiowayManager","secondSLANRunExAnalyse--mExperiment-- " + mExperiment);
        if(mExperiment != null) {
            ArrayList<AnalyseParamter> analyseParamters = BoWeiService.getInstance()
                    .getExperimentParamter(mExperiment);
            analyseParamters.get(0).setAMPAlgorithm(AMPAlgorithm);
            ArrayList<Target> targets = analyseParamters.get(0).getTargets();
            if(null !=targets && targets.size()>0) {
                for (Target target : targets) {
                    if (target.getChannelNo()== 1) {
                        ConfigTarget famTarget = getConfigTarget(configTargets,target.getChannelNo());
                        TargetParamter famParamer = target.getParamter();
                        famParamer.setThreshold(famTarget.getThreshold());
                        famParamer.setBeginBaseline(famTarget.getBeginBaseline());
                        famParamer.setEndBaseline(famTarget.getEndBaseline());
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
                    }
                }
            }
            Experiment sec_experiment = BoWeiService.getInstance().setExperimentParameter(mExperiment,optimizations,
                    analyseParamters.get(0));
            if(null != sec_experiment){
                List<CtRuleConclusion> conclusions = new ArrayList<CtRuleConclusion>();
                List<PeakRuleConclusion> peakConclusions = new ArrayList<PeakRuleConclusion>();
                if(null != ctFile){
                    conclusions = judgeCTFile(sec_experiment, ctFile,startTime);
                }
                if(null != peakFile){
                    peakConclusions = judgePEAKFile(sec_experiment, peakFile,startTime);
                }
                boolean result = parseSecondExperiment(sec_experiment,conclusions,peakConclusions);
                if(null != onExperimentCallback){
                    if(result){
                        onExperimentCallback.onSecondParseFinish(true);
                    }else {
                        onExperimentCallback.onSecondParseFinish(false);
                    }
                }
            }else {
                if(onExperimentCallback != null ){
                    onExperimentCallback.onSecondParseFinish(false);
                }
            }
        }else {
            if(onExperimentCallback != null){
                onExperimentCallback.onSecondParseFinish(false);
            }
        }
    }

    /**2 记录界面调用
     * 设置试验参数前必须先获取实验参数
     * @param AMPAlgorithm 扩增曲线算法
     * @param configTargets true是自动阈值，false手动阈值
     */
    public void secondBiowayExAnalyse(List<ConfigTarget> configTargets,
                                      List<PeaktypeOptimization>optimizations,
                                      int AMPAlgorithm,
                                      File ctFile,
                                      File peakFile,
                                      Experiment experiment,
                                      String startTime){
        //获取实验参数
        if(null != BoWeiService.getInstance()){
            ArrayList<AnalyseParamter> analyseParamters = BoWeiService.getInstance()
                    .getExperimentParamter(experiment);
            if(null != analyseParamters.get(0)){
                ArrayList<Target> targets = analyseParamters.get(0).getTargets();
                analyseParamters.get(0).setAMPAlgorithm(AMPAlgorithm);
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

                    Experiment sec_experiment = BoWeiService.getInstance().setExperimentParameter(experiment,
                            optimizations,
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
                        Log.i("AAA","记录界面"+ peakConclusions.size());
                        boolean result = parseSecondExperiment(sec_experiment,conclusions,peakConclusions);
                        if(null != onDetailExCallback){
                            if(result){
                                onDetailExCallback.onSecondParseFinish(true);
                            }else{
                                onDetailExCallback.onSecondParseFinish(false);
                            }
                        }
                    }else {
                        if(onDetailExCallback != null ){
                            onDetailExCallback.onSecondParseFinish(false);
                        }
                    }
                }else{
                    if(onDetailExCallback != null ){
                        onDetailExCallback.onSecondParseFinish(false);
                    }
                }
            }else{
                if(onDetailExCallback != null ){
                    onDetailExCallback.onSecondParseFinish(false);
                }
            }
        }else{
            if(onDetailExCallback != null ){
                onDetailExCallback.onSecondParseFinish(false);
            }
        }
    }


    public boolean parseSecondExperiment(Experiment experiment,
                                      List<CtRuleConclusion> conclusions,
                                      List<PeakRuleConclusion> peakConclusions) {

        clearAll();
        mExlResults.clear();
        getExperimentParamter(experiment);
        Log.i("BioWayManager","parseSecondExperiment isMelt" + isMelt);
        List<WellResult> wellResults = experiment.getWellResult();
        if (wellResults != null && wellResults.size() > 0) {
            for (WellResult wellResult : wellResults) {
                if (null != wellResult) {
                    List<TargetResult> targetResults = wellResult.getTargetResults();
                    if (targetResults != null && targetResults.size() > 0) {

                        for (TargetResult targetResult : targetResults) {
                            BiowayCurveList mRawList = parseTargetResultToRaw(wellResult.getID(), wellResult.getSampleName(),targetResult);
                            BiowayCurveList mAmpList = parseTargetResultToAMP(wellResult.getID(),wellResult.getSampleName(), targetResult);
                            BiowayCurveList mMeltList = parseTargetResultToMelt(wellResult.getID(),wellResult.getSampleName(), targetResult);
                            BiowayCurveList mPeakList = parseTargetResultToPeak(wellResult.getID(),wellResult.getSampleName(), targetResult);
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
                for (BiowayCurveList curveList : mRawCurve) {
                    String wellName = curveList.getWellNum();
                    String hole_Num = curveList.getCurve_hole();
                    List<CtResult> results = new ArrayList<>();
                    if (null != conclusions && conclusions.size() > 0) {
                        for (int k = 0; k < conclusions.size(); k++) {
                            String conclusionWell = conclusions.get(k).getTupeNo();//getWellNum
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<CtResult>> conlusionMap = conclusions.get(k).getConlusionMap();
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
                    BiowayExlResult exlResult = new BiowayExlResult();
                    exlResult.setChannelNo(curveList.getChannelNo());
                    exlResult.setWellNum(wellName);
                    exlResult.setCurve_hole(hole_Num);
                    exlResult.setResults(results);
                    exlResult.setPeakResults(peakResults);
                    exlResult.setControlRn(curveList.getControlRn());

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
                for (BiowayCurveList curveList : mMeltCurve) {
                    String wellName = curveList.getWellNum();
                    String hole_Num = curveList.getCurve_hole();
                    List<CtResult> results = new ArrayList<>();
                    if (null != conclusions && conclusions.size() > 0) {
                        for (int k = 0; k < conclusions.size(); k++) {
                            String conclusionWell = conclusions.get(k).getTupeNo();//getWellNum
                            //获取每一个孔位的结论
                            if (wellName.contains(conclusionWell)) {
                                Map<String, List<CtResult>> conlusionMap = conclusions.get(k).getConlusionMap();
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

                    BiowayExlResult exlResult = new BiowayExlResult();
                    exlResult.setChannelNo(curveList.getChannelNo());
                    exlResult.setWellNum(wellName);
                    exlResult.setCurve_hole(hole_Num);
                    exlResult.setResults(results);
                    exlResult.setPeakResults(peakResults);
                    exlResult.setControlRn(curveList.getControlRn());
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
            updateExperiment(experiment,conclusions,peakConclusions);
            return true;
        }
        return false;
    }

    private void updateExperiment(Experiment experiment, List<CtRuleConclusion>conclusions, List<PeakRuleConclusion> peakConclusions){

        this.mExperiment = experiment;
        this.ct_Conclusions = conclusions;
        this.peak_Conclusions = peakConclusions;

    }

    //////////////////////////////////////////////更是数据库结果////////////////////////////////////////////

    /**
     * 结果详情页面更新数据库结果
     * @param testItem
     * @param startTime
     */
    public void updateDetailExperiment(String testItem,String startTime){
        if(null != mExperiment){
            getExperimentParamter(mExperiment);
            updateBiowayExTask = new UpdateBiowayExTask(isMelt,mThresholds,mExperiment,testItem,startTime,ct_Conclusions,peak_Conclusions);
            updateBiowayExTask.execute();
            updateBiowayExTask.setOnUpdateExListener(new OnUpdateExListener() {
                @Override
                public void onUpdateDbResult(boolean result) {
                    Log.i("BIOWAY","onUpdateDbResult" + result);
                    if(onDetailExCallback != null){
                        onDetailExCallback.onUpdateFinish(result);
                    }
                    cancelUpdateTask();
                }
            });
        }else {
            if(onDetailExCallback != null){
                onDetailExCallback.onUpdateFinish(false);
            }
        }
    }
    /**
     * 二次更新保存结果
     * @param testItem
     * @param startTime
     */
    UpdateBiowayExTask updateBiowayExTask = null;
    public void updateBiowayExperiment(String testItem,String startTime){
        if(null != mExperiment){
            getExperimentParamter(mExperiment);
            updateBiowayExTask = new UpdateBiowayExTask(isMelt,mThresholds,mExperiment,testItem,startTime,ct_Conclusions,peak_Conclusions);
            updateBiowayExTask.execute();
            updateBiowayExTask.setOnUpdateExListener(new OnUpdateExListener() {
                @Override
                public void onUpdateDbResult(boolean result) {
                    Log.i("BIOWAY","onUpdateDbResult" + result);
                    if(onExperimentCallback != null ){
                        onExperimentCallback.onUpdateFinish(result);
                    }
                    cancelUpdateTask();
                }
            });
        }else {
            if(onExperimentCallback != null){
                onExperimentCallback.onUpdateFinish(false);
            }
        }

    }

    private void cancelUpdateTask(){
        if(null != updateBiowayExTask){
            updateBiowayExTask.cancel(true);
            updateBiowayExTask = null;
        }
    }




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

    /****************************************实验结论*****************************************/
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
                                Peak meltPeak =  (Peak) experiment.getWellResult().get(i)
                                        .getTargetResults().get(j).getPeakValues().get(m);
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



    public void parseRunFinishConfigEx(Experiment experiment,boolean isMelt) {
        clearAll();
        List<WellResult> wellResults = experiment.getWellResult();
        if (wellResults != null && wellResults.size() > 0) {
            for (WellResult wellResult : wellResults) {
                if (null != wellResult) {
                    List<TargetResult> targetResults = wellResult.getTargetResults();
                    if (targetResults != null && targetResults.size() > 0) {
                        for (int j = 0; j < targetResults.size(); j++) {

                            BiowayCurveList mRawList = parseTargetResultToRaw(wellResult.getID(),wellResult.getSampleName(), targetResults.get(j));
                            BiowayCurveList mAmpList = parseTargetResultToAMP(wellResult.getID(), wellResult.getSampleName(),targetResults.get(j));
                            BiowayCurveList mMeltList = parseTargetResultToMelt(wellResult.getID(), wellResult.getSampleName(),targetResults.get(j));
                            BiowayCurveList mPeakList = parseTargetResultToPeak(wellResult.getID(), wellResult.getSampleName(),targetResults.get(j));
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
            for (BiowayCurveList curveList : mMeltCurve) {
                String wellName = curveList.getWellNum();
                String hole_Num = curveList.getCurve_hole();
                List<CtResult> results = new ArrayList<>();
                if (null != ct_Conclusions && ct_Conclusions.size() > 0) {
                    for (int k = 0; k < ct_Conclusions.size(); k++) {
                        String conclusionWell = ct_Conclusions.get(k).getTupeNo();//getWellNum
                        //获取每一个孔位的结论
                        if (wellName.contains(conclusionWell)) {
                            Map<String, List<CtResult>> conlusionMap = ct_Conclusions.get(k).getConlusionMap();
                            if (conlusionMap.containsKey(hole_Num)) {
                                results = conlusionMap.get(hole_Num);
                            }

                        }
                    }
                }

                BiowayExlResult exlResult = new BiowayExlResult();
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
            for (BiowayCurveList curveList : mRawCurve) {
                String wellName = curveList.getWellNum();
                String hole_Num = curveList.getCurve_hole();
                List<CtResult> results = new ArrayList<>();
                if (null != ct_Conclusions && ct_Conclusions.size() > 0) {
                    for (int k = 0; k < ct_Conclusions.size(); k++) {
                        String conclusionWell = ct_Conclusions.get(k).getTupeNo();//getWellNum
                        //获取每一个孔位的结论
                        if (wellName.contains(conclusionWell)) {
                            Map<String, List<CtResult>> conlusionMap = ct_Conclusions.get(k).getConlusionMap();
                            if (conlusionMap.containsKey(hole_Num)) {
                                results = conlusionMap.get(hole_Num);
                            }

                        }
                    }
                }

                BiowayExlResult exlResult = new BiowayExlResult();
                exlResult.setWellNum(wellName);
                exlResult.setCurve_hole(hole_Num);
                exlResult.setResults(results);
                exlResult.setControlRn(curveList.getControlRn());
                exlResult.setMelt(isMelt);//是否是熔解曲线
                exlResult.setIdCard(curveList.getIdCard());
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
                            BiowayCurveList mRawList = parseTargetResultToRaw(wellResult.getID(),wellResult.getSampleName(),targetResults.get(j));
                            BiowayCurveList mAmpList = parseTargetResultToAMP(wellResult.getID(),wellResult.getSampleName(),targetResults.get(j));
                            BiowayCurveList mMeltList = parseTargetResultToMelt(wellResult.getID(),wellResult.getSampleName(),targetResults.get(j));
                            BiowayCurveList mPeakList = parseTargetResultToPeak(wellResult.getID(),wellResult.getSampleName(),targetResults.get(j));
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
        return true;
    }

    ///////////////////////////解析项目温度////////////////////////////

    /**解析运行中的温度数据***/
    private ArrayList<ExTemperature> parseRunExTemperature(List<Temperature> temperatures){
        ArrayList<ExTemperature>  exTemperatures = new ArrayList<ExTemperature>();
        if(null != temperatures && temperatures.size()>0){
            List<Entry> blockEntries = new ArrayList<Entry>();
            for(int i=0;i<temperatures.size();i+=10){

                Entry entry1 = new Entry(Float.parseFloat(String.valueOf(temperatures.get(i).getTime())),

                        StringUtil.DoubleToFloat(temperatures.get(i).getBlockTemperature(),0.00f));
                blockEntries.add(entry1);
                // 如果已经是最后一个元素，直接跳出循环
                if (i + 10 >= temperatures.size()) {
                    break;
                }
            }
            // 确保包括最后一个元素（如果它不在上面循环中被添加）
            if (!blockEntries.contains(new Entry(Float.parseFloat(String.valueOf(temperatures.get(temperatures.size()-1).getTime())),
                    StringUtil.DoubleToFloat(temperatures.get(temperatures.size()-1).getBlockTemperature(), 0.00f)))){
                blockEntries.add(new Entry(Float.parseFloat(String.valueOf(temperatures.get(temperatures.size()-1).getTime())),
                        StringUtil.DoubleToFloat(temperatures.get(temperatures.size()-1).getBlockTemperature(), 0.00f)));

            }

            ExTemperature exTemperature = new ExTemperature();
            exTemperature.setBlockTemperatures(blockEntries);
            exTemperatures.add(exTemperature);
            // Log.i("AAA","time" + HSUtils.INSTANCE.secondToHMSString(temperatures.get(temperatures.size()-1).getTime() / 10000000));
        }
        return exTemperatures;
    }

    /**
     * 结果详情页面保存.biowaypcrResult文件
     * @param experiment
     * @param savePath
     */
    public void saveDetailExToPCR(Experiment experiment,
                                  String testItem,
                                  String savePath) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if(onDetailExCallback != null ){
                onDetailExCallback.onSaveJsonResult(false,"保存PCR失败,sdcard不能保存");
            }
            return;
        }
        if (experiment == null) {
            Log.i("BiowayManager", "saveResultToPCR experiment == null");
            if(onDetailExCallback != null){
                onDetailExCallback.onSaveJsonResult(false,"保存PCR失败,Experiment为空");
            }
            return;
        }

        File dir = new File(savePath);
        if(!dir.exists()){
            dir.mkdirs();
        }
        try {
            String saveFileName = DateFormatUtil.getFileName();
            BoWeiService.getInstance().saveExperiment(experiment, saveFileName, savePath, new OnSaveExperimentListener() {
                @Override
                public void onSaveSuccess() {
                    Log.e("BiowayManager","saveResultToPCR onSaveSuccess" );
                    if(onDetailExCallback != null ){
                        onDetailExCallback.onSaveJsonResult(true,"保存PCR文件成功");
                    }
                }

                @Override
                public void onSaveFail() {
                    Log.e("BiowayManager","saveResultToPCR onSaveFail" );
                    if(onDetailExCallback != null){
                        onDetailExCallback.onSaveJsonResult(false,"保存PCR文件失败");
                    }
                }
            });


        }catch (Exception e){
            Log.e("saveResultToPCR",e.getMessage());
            if(onDetailExCallback != null){
                onDetailExCallback.onSaveJsonResult(false,"保存PCR文件失败");
            }
        }

        if(null != ct_Conclusions && null != peak_Conclusions){
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
        }
    }

    public void saveResultToPCR(Experiment experiment,
                                   String testItem,
                                   String subFirName) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if(onExperimentCallback != null){
                onExperimentCallback.onSaveJsonResult(false,"保存PCR失败,sdcard不能保存");
            }
            return;
        }
        if (experiment == null) {
            Log.i("BiowayManager", "saveResultToPCR experiment == null");
            if(onExperimentCallback != null){
                onExperimentCallback.onSaveJsonResult(false,"保存PCR失败,Experiment为空");
            }
            return;
        }

        File dir = new File(FileUtils.BIOWAY_RESULT_PATH, subFirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            String saveFileName = DateFormatUtil.getFileName();
            String savePath = FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;
            BoWeiService.getInstance().saveExperiment(experiment, saveFileName, savePath, new OnSaveExperimentListener() {
                @Override
                public void onSaveSuccess() {
                    Log.e("BiowayManager","saveResultToPCR onSaveSuccess" );
                    if(onExperimentCallback != null){
                        onExperimentCallback.onSaveJsonResult(true,"保存PCR文件成功");
                    }
                }

                @Override
                public void onSaveFail() {
                    Log.e("BiowayManager","saveResultToPCR onSaveFail" );
                    if(onExperimentCallback != null ){
                        onExperimentCallback.onSaveJsonResult(false,"保存PCR文件失败");
                    }
                }
            });


        }catch (Exception e){
            Log.e("saveResultToPCR",e.getMessage());
            if(onExperimentCallback != null){
                onExperimentCallback.onSaveJsonResult(false,"保存PCR文件失败");
            }
        }

        if(null != ct_Conclusions && null != peak_Conclusions){
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
        }
    }
}
