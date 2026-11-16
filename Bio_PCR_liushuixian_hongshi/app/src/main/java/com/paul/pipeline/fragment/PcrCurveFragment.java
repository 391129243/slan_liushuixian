package com.paul.pipeline.fragment;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.bean.CtRuleConclusion;
import com.gidi.bio_pcr.bean.PeakRuleConclusion;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.gidi.bio_pcr.bioway.adapter.RunBiowayResultAdapter;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.databinding.FragmentPcrCurveBinding;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.pcr.OnExperimentCallback;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomCheckText;
import com.gidi.bio_pcr.view.LineChartMarkView;
import com.gidi.bio_pcr.view.NumKeyboardDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.itextpdf.io.util.DateTimeUtil;
import com.paul.pipeline.data.BiowayHelper;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.message.ResultMessage;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.utils.DataTimeUtil;
import com.paul.pipeline.view.MiniCheckText;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.ExperimentData.WellResult;
import com.winters.boweilibrary.BoWei.ExperimentData.TargetResult;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.winters.boweilibrary.BoWei.bean.BiowayExlResult;
import com.winters.boweilibrary.BoweiJni;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PcrCurveFragment extends Fragment {
    private static final String TAG = "PcrCurveFragment";
    private static final String ARG_PARAM2 = "index";
    private int pcrindex;

    public PcrCurveFragment() {
        // Required empty public constructor
    }

    public static PcrCurveFragment newInstance(int index) {
        PcrCurveFragment fragment = new PcrCurveFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM2, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pcrindex = getArguments().getInt(ARG_PARAM2);
        }
        initData();
    }
    private Experiment lastParsedExperiment = null;
    private int lastDotsCount = -1;

    private int getDotsSize(Experiment exp) {
        if (exp == null) return 0;
        try {
            List<WellResult> wells = exp.getWellResult();
            if (wells == null || wells.isEmpty()) return 0;
            WellResult firstWell = wells.get(0);
            if (firstWell == null) return 0;
            List<TargetResult> targets = firstWell.getTargetResults();
            if (targets == null || targets.isEmpty()) return 0;
            TargetResult firstTarget = targets.get(0);
            if (firstTarget == null) return 0;
            return firstTarget.getRawCurve() != null ? firstTarget.getRawCurve().size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void initData(){
        biowayHelper = new BiowayHelper();
        ctConclusions = new ArrayList<CtRuleConclusion>();
        peakConclusions = new ArrayList<PeakRuleConclusion>();
        lastParsedExperiment = null;
        lastDotsCount = -1;
    }
    View mview;
    FragmentPcrCurveBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pcr_curve, container, false);
        mview = binding.getRoot();
        initBinding();
        return mview;
    }

    private void initBinding() {
        initpcrview();
    }
    PipePCRModel model;
    PipeMainViewModel mainViewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        initModel();
    }

    public void initModel(){
        if(mainViewModel!=null) {
            model = mainViewModel.findPipePCRModel(getContext(), pcrindex);
            if(model!=null) {
                binding.setModel(model);
                binding.executePendingBindings();
            }
        }
    }
    public void clearModel(){
        initData();
        initpcrview();
        initModel();
        refreshExperiment();
    }
//    public void refreshExperiment(){
//        if(binding!=null) {
//            binding.executePendingBindings();
//        }
//        openBiowayExperiment();
//        updateWellResult();
//    }

    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean isRefreshing = false;

    public void refreshExperiment() {
        if (isRefreshing) {
            return;
        }
        isRefreshing = true;

        if (binding != null && getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (binding != null) {
                            binding.executePendingBindings();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "binding executePendingBindings error: " + e.getMessage());
                    }
                }
            });
        }

        refreshExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // 获取上次的解析/数据状态
                    Experiment prevExperiment = lastParsedExperiment;
                    int prevDotsCount = lastDotsCount;

                    // 1. 在后台线程执行非常耗时的解析
                    openBiowayExperiment();

                    // 2. 只有在实验发生改变或新获取的数据点有增长/变化时，才触发出图/重新绘制
                    if (lastParsedExperiment != prevExperiment || lastDotsCount != prevDotsCount) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        updateWellResult();
                                    } catch (Exception e) {
                                        Log.e(TAG, "updateWellResult error on UI thread: " + e.getMessage());
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "refreshExperiment background error: " + e.getMessage(), e);
                } finally {
                    isRefreshing = false;
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (refreshExecutor != null) {
                refreshExecutor.shutdown();
            }
        } catch (Exception e) {
            Log.e(TAG, "shutdown refreshExecutor error: " + e.getMessage());
        }
    }


    public ResultMessage saveTestExperiment(){
        ResultMessage resultMessage = new ResultMessage();
        pcrExperiment = PCRManager.getInstance().getPcrExperiment(pcrindex);
        if(pcrExperiment!=null) {
            String nowtime = DateFormatUtil.getSysFullTime();
            if(model.startTime.get()==null){
                model.startTime.set(nowtime);
            }
            String batchno = "PCR"+pcrindex+"PAN0-13-"+ DataTimeUtil.getHFullTime();
            resultMessage = saveNowExperiment(pcrExperiment,model.startTime.get(),batchno,pcrindex);
            return resultMessage;
        }else{
            resultMessage.setResult(false);
            resultMessage.setMessage("saveExperiment experiment为空" + DataTimeUtil.getCurrDate());
            Log.i(TAG,"saveExperiment experiment为空");
            return resultMessage;
        }
    }
    public boolean checkSavePcr(){
        if(model == null){
            return false;
        }
        if(model.currentTray.get()==null){
            return false;
        }
        DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(model.currentTray.get());
        if (dispatchModel == null) {
            return false;
        }
        if(dispatchModel.savepcr.get()!=null){
            return dispatchModel.savepcr.get();
        }else{
            return false;
        }
    }
    public ResultMessage saveExperiment(){
        ResultMessage resultMessage = new ResultMessage();
        if(model!=null){
            int pcrindex = model.index.get();
            int trayindex = model.currentTray.get();
            if(model.currentTray.get()==null){
                trayindex = 0;
            }
            if(model.startTime.get()==null){
                String nowtime = DateFormatUtil.getSysFullTime();
                model.startTime.set(nowtime);
            }
            String starttime = model.startTime.get();
            String tempno = "";
            DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(trayindex);
            if(dispatchModel!=null && dispatchModel.templateno.get()!=null){
                tempno =  dispatchModel.templateno.get();
            }
            if(tempno==null||tempno==""){
                tempno = "KK00";
            }
            String tempname = "";
            if(dispatchModel!=null && dispatchModel.templatename.get()!=null){
                tempname =  dispatchModel.templatename.get();
            }
            if(tempname==null||tempname==""){
                tempname = "null";
            }
            String batchno = "";
            if(dispatchModel!=null && dispatchModel.batch.get()!=null){
                batchno = dispatchModel.batch.get();
            }
            if(batchno==""){
                batchno = TrayManager.getInstance().createBatchCode(tempno,tempname,trayindex);
            }
            pcrExperiment = PCRManager.getInstance().getPcrExperiment(pcrindex);
            if(pcrExperiment!=null) {
                resultMessage = saveNowExperiment(pcrExperiment,starttime,batchno,pcrindex);
                return resultMessage;
            }else{
                resultMessage.setResult(false);
                resultMessage.setMessage("saveExperiment experiment为空" + DataTimeUtil.getCurrDate());
                Log.i(TAG,"saveExperiment experiment为空");
                return resultMessage;
            }
        }else{
            resultMessage.setResult(false);
            resultMessage.setMessage("saveExperiment model为空" + DataTimeUtil.getCurrDate());
            Log.i(TAG,"saveExperiment model为空");
            return resultMessage;
        }
    }
    private ResultMessage saveNowExperiment(Experiment pcrExperiment,String starttime,String batchno,int pcrindex){
        ResultMessage resultMessage = onSLANExperimentFinish(pcrExperiment,starttime,batchno,pcrindex);
        if(resultMessage.isResult()){
            Log.i(TAG, "saveNowExperiment success batch:"+ batchno
                    +" startTime:"+starttime +" experiment:"+pcrExperiment.getName());
        }else{
            Log.e(TAG, "saveNowExperiment fail batch:"+ batchno
                    +" startTime:"+starttime +" experiment:"+pcrExperiment.getName());
        }
        return resultMessage;
    }
    private List<CtRuleConclusion> ctConclusions;
    private List<PeakRuleConclusion> peakConclusions;
    private List<File> localList = new ArrayList<File>();
    /**
     * 获取当前模板的所有文件
     */
    private void getLocalFileList() {
        String localFilePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName
                + File.separator;
        File allFileDir = new File(localFilePath);
        if (allFileDir.isDirectory() && allFileDir.exists()) {
            Collections.addAll(localList, allFileDir.listFiles());
        }
    }
    public String getPCRFilePath(String name){
        String path = "";
        if(name.contains("01-")){
            File file = new File(FileUtils.SECRET1_3_flu_repeat_fam_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("02-")){
            File file = new File(FileUtils.SECRET2_3_flu_repeat_rox_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("03-")){
            File file = new File(FileUtils.SECRET3_3_flu_repeat_hex_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("04-")){
            File file = new File(FileUtils.SECRET4_3_flu_repeat_cy5_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("05-")){
            File file = new File(FileUtils.SECRET5_5_sample_repeat_high_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("06-")){
            File file = new File(FileUtils.SECRET6_5_sample_repeat_high_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("07-")){
            File file = new File(FileUtils.SECRET7_5_sample_repeat_median_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("08-")){
            File file = new File(FileUtils.SECRET8_5_sample_repeat_median_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("09-")){
            File file = new File(FileUtils.SECRET9_5_sample_repeat_low_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("10-")){
            File file = new File(FileUtils.SECRET10_5_sample_repeat_low_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("11-")){
            File file = new File(FileUtils.SECRET11_6_sample_linear_AMP_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("12-")){
            File file = new File(FileUtils.SECRET12_6_sample_linear_AMP_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("13-")){
            File file = new File(FileUtils.SECRET13_3_sample_linear_AMP_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("14-")){
            File file = new File(FileUtils.SECRET14_8_flu_linear_fam_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("15-")){
            File file = new File(FileUtils.SECRET15_7_flu_linear_fam_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("16-")){
            File file = new File(FileUtils.SECRET16_8_flu_linear_rox_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("17-")){
            File file = new File(FileUtils.SECRET17_7_flu_linear_rox_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("18-")){
            File file = new File(FileUtils.SECRET18_8_flu_linear_hex_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("19-")){
            File file = new File(FileUtils.SECRET19_7_flu_linear_hex_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("20-")){
            File file = new File(FileUtils.SECRET20_8_flu_linear_cy5_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("21-")){
            File file = new File(FileUtils.SECRET21_7_flu_linear_cy5_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("22-")){
            File file = new File(FileUtils.SECRET22_5_extraction_consistence_high_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("23-")){
            File file = new File(FileUtils.SECRET23_5_extraction_consistence_high_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("24-")){
            File file = new File(FileUtils.SECRET24_5_extraction_consistence_median_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("25-")){
            File file = new File(FileUtils.SECRET25_5_extraction_consistence_median_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("26-")){
            File file = new File(FileUtils.SECRET26_5_extraction_consistence_low_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(name.contains("27-")){
            File file = new File(FileUtils.SECRET27_5_extraction_consistence_low_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }
        return path;
    }
    /****************************************实验操作***********************************************/
    public ResultMessage saveSLANExperiment(Experiment experiment,String starttime,String batchno,int pcr) {
        ResultMessage resultMessage = new ResultMessage();
        try {
            String testItem = batchno;
            String startTime = starttime;
            String afterTime = DateFormatUtil.convertDateFormat(startTime);
//        String afterTime = DataTimeUtil.getHFullTime();
            String subFirName;
            if (StringUtil.isEmpty(description)) {
                subFirName = afterTime + "-PCR" + pcr + "_" + testItem;
            } else {
                subFirName = afterTime + "-PCR" + pcr + "_" + testItem + "_" + description;
            }
            Experiment experiment_device = experiment;
            //数据库
            saveExperimentResult(experiment_device,subFirName, starttime, batchno, description);
            //本地
            saveResultToPCR(experiment_device, subFirName);
//
            //上传实验数据
            resultMessage.setResult(true);
            return resultMessage;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,"saveSLANExperiment pcr"+pcr + e.getMessage());
            resultMessage.setResult(false);
            resultMessage.setMessage("saveSLANExperiment:"+e.getMessage());
            return resultMessage;
        }
    }
    public void saveResultToPCR(Experiment experiment, String subFirName) {

        Log.e(TAG, "saveResultToPCR");
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e(TAG, "saveResultToPCR MEDIA_MOUNTED null");
            return ;
        }
        if (experiment == null) {
            Log.e(TAG, "saveResultToPCR experiment null");
            return;
        }

        File dir = new File(FileUtils.BIOWAY_RESULT_PATH, subFirName);
        if (!dir.exists()) {
            Log.e(TAG, "saveResultToPCR mkdirs:"+dir.getAbsolutePath());
            dir.mkdirs();
        }
        //////蒋老师意思，屏蔽测试
        String saveFileName = DateFormatUtil.getFileName();
        Log.e(TAG, "saveResultToPCR saveFileName:"+saveFileName);
        String savePath = FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;
        Log.e(TAG, "saveResultToPCR savePath:"+savePath);
        BoWeiService.getInstance().saveExperiment(experiment, saveFileName, savePath, new OnSaveExperimentListener() {
            @Override
            public void onSaveSuccess() {
                Toast.makeText(getContext(),"保存PCR成功!",Toast.LENGTH_LONG).show();
                Log.e(TAG,"onSaveSuccess" );
            }

            @Override
            public void onSaveFail() {
                Toast.makeText(getContext(),"保存PCR失败!",Toast.LENGTH_LONG).show();
                Log.e(TAG,"onSaveFail" );
            }
        });
    }
    /**
     * 第一次SLAN pcr结束
     * 保存pcr文件以及保存数据库文件
     */
    private ResultMessage onSLANExperimentFinish(Experiment newExperiment ,String starttime,String batchno,int pcr) {
        ResultMessage resultMessage = new ResultMessage();
        Log.e(TAG, "onSLANExperimentFinish batch:"+batchno+" pcr:"+pcr);
        getLocalFileList();
        File ctFile = FileUtils.getFile(localList, ".ct");
        File peakFile = FileUtils.getFile(localList, ".peak");
        try {
//            String resultFilePath = "/sdcard/PCR/temp"+ pcrindex +".boweipcrResult";
            if(newExperiment != null) {
//                BoWeiService.getInstance().saveExperiment(experiment, "temp"+ pcrindex, "/sdcard/PCR/", new OnSaveExperimentListener() {
//                    @Override
//                    public void onSaveSuccess() {
//                        Log.e(TAG,"onSaveSuccess" );
//                        // String resultFilePath = "/mnt/sdcard/PCR/BoWeiResult/产物标准熔曲(005).boweipcrResult";
//                        Experiment newExperiment = BoWeiService.getInstance().openExperiment(resultFilePath);
//                        BoweiJni.analysisExperimentLoessFilter(newExperiment);
//
//                    }
//
//                    @Override
//                    public void onSaveFail() {
//                        Log.e(TAG,"onSaveFail" );
//                    }
//                });
                try{
                    boolean isSecret = PreferenceUtil.getInstance(getContext()).getBooleanValue(SharedConstant.SECRET_SETTING,false);
//            Experiment experiment = BoWeiService.getInstance().getExperimentResult();
                    //BoweiJni boweiJni = new BoweiJni();
//              String text = boweiJni.analysisExperiment(experiment);
                    BoweiJni.analysisExperimentLoessFilter(newExperiment);
                    if(isSecret) {
                        //打开准备的好的.pcr文件
                        String path = getPCRFilePath(batchno);
                        Experiment des_experiment = null;
                        if(!path.equals("")){
                            des_experiment = BoWeiService.getInstance().openExperiment(path);
                        }
                        if(null != des_experiment){
                            if (ctFile != null) {
                                ctConclusions.clear();
                                ctConclusions = BiowayManager.getInstance()
                                        .judgeCTFile(des_experiment, ctFile,PcrApplication.ex_startTime);
                            }
                            if (peakFile != null) {
                                peakConclusions.clear();
                                peakConclusions = BiowayManager.getInstance()
                                        .judgePEAKFile(des_experiment, peakFile,PcrApplication.ex_startTime);
                            }
                        }else{
                            if (ctFile != null) {
                                ctConclusions.clear();
                                ctConclusions = BiowayManager.getInstance()
                                        .judgeCTFile(newExperiment, ctFile,PcrApplication.ex_startTime);
                            }
                            if (peakFile != null) {
                                peakConclusions.clear();
                                peakConclusions = BiowayManager.getInstance()
                                        .judgePEAKFile(newExperiment, peakFile,PcrApplication.ex_startTime);
                            }
                        }
                    }else{
                        if (newExperiment == null) {
                            Log.e(TAG, "getExperimentResult null " + newExperiment.getName());
                            resultMessage.setResult(false);
                            resultMessage.setMessage("onSLANExperiment2:"+"实验数据为空");
                            return resultMessage;
                        }
                        if (ctFile != null) {
                            ctConclusions.clear();
                            ctConclusions = BiowayManager.getInstance().judgeCTFile(newExperiment, ctFile,PcrApplication.ex_startTime);
                        }
                        if (peakFile != null) {
                            peakConclusions.clear();
                            peakConclusions = BiowayManager.getInstance()
                                    .judgePEAKFile(newExperiment, peakFile,PcrApplication.ex_startTime);
                        }
                    }
                    return saveSLANExperiment(newExperiment, starttime, batchno,pcr);
                } catch (Exception e) {
                    e.getMessage();
                    Log.e(TAG,"onSLANExperimentFinish:"+e.getMessage());
                    resultMessage.setResult(false);
                    resultMessage.setMessage("onSLANExperimentFinish2:"+e.getMessage());
                    return resultMessage;
                }
            }else {
                resultMessage.setResult(false);
                resultMessage.setMessage("onSLANExperiment1: 实验数据为空");
                return resultMessage;
            }

        } catch (Exception e) {
            e.getMessage();
            Log.e(TAG,"onSLANExperimentFinish:"+e.getMessage());
            resultMessage.setResult(false);
            resultMessage.setMessage("onSLANExperimentFinish1:"+e.getMessage());
            return resultMessage;
        }
    }
    /**
     * 解析和保存数据库
     * @param experiment
     */
    public void saveExperimentResult(Experiment experiment,
                                     String subFirName,
                                     String starttime,
                                     String batchno,
                                     String des) {
        try {
            if (experiment == null) {
                Log.e(TAG, "saveExperimentResult experiment 不能为 null");
            } else {
                //解析实验
                String endTime = DateFormatUtil.getSysFullTime();
                String savePath = FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;
                Log.e(TAG, "saveExperimentResult experiment endTime："+endTime);
                biowayHelper.parseExperimentResult(experiment, ctConclusions,peakConclusions);
                biowayHelper.saveBiowayExResultToDB(experiment,savePath, starttime,endTime, batchno,des, ctConclusions,peakConclusions);
            }
        } catch (Exception e) {
            Log.e(TAG, "saveExperimentResult error = " + e.getMessage());
        }
    }

    private String description;
    public BiowayHelper biowayHelper;
    private LineChart resultChart;
    // private TextView openFileTv;
    private TextView mOneTv,mTwoTv,mThreeTv,mFourTv,mFiveTv,mSixTv,mSevenTv,mEightTv,mAllHoleTv;
    private MiniCheckText mAllCheckTv,mFamChannelTv,mHexChannelTv,mRoxChannelTv,mCY5ChannelTv;

    private boolean isAllChanelCheck = false;
    private boolean isAllCheck = false;
    private boolean isFAMCheck = false;
    private boolean isHEXCheck = false;
    private boolean isROXCheck = false;
    private boolean isCY5Check = false;
    private RadioGroup mCurveChartGg;
    private boolean isOneCheck,isTwoCheck,isThreeCheck,isFourCheck,
            isFiveCheck,isSixCheck,isSevenCheck,isEightCheck = false;
    private List<BiowayCurveList> mRawCurveList;//原始数据
    private List<BiowayCurveList> mCheckRawList;
    private List<BiowayCurveList> mAMPCurveList;
    private List<BiowayCurveList> mCheckAMPList;

    private List<BiowayCurveList> mMeltCurveList;
    private List<BiowayCurveList> mCheckMeltList;

    private List<BiowayCurveList> mPeakCurveList;
    private List<BiowayCurveList> mCheckPeakList ;

    private List<BiowayCurveList> showCurveList;
    private List<BiowayExlResult> mExlResults;
    private List<BiowayExlResult> mCheckExlResults;
    private int currentView = 1;

    private HorizontalScrollView mResultExlLayout;//表格界面
    private LinearLayout mResultChartLayout;
    private boolean isChartVisible = true;
    private boolean isExcelVisible = false;
    private boolean isSecAysVisible = false;
    //参数设置
    private boolean isMelt = false;
    private RelativeLayout mSecAnalyseLayout;
    private TextView mBasicParamTitleTv,mMeltParamTitleTv,mAmpAlgorithmTv;
    private LinearLayout mAmpParamterLayout,mMeltParamterLayout;//扩增曲线参数，熔解曲线参数
    private ListView mAmpAlgorithmLv,mMeltParamterLv;//扩增曲线参数列表，熔解曲线参数列表
    private MeltParamterAdapter mMeltParamterAdapter;
    private AmpAlgorithmAdapter mAmpAlgorithmAdapter;

    private NumKeyboardDialog numKeyboardDialog;
    private int ampAlogrithm_pos = -1;
    private int meltParamter_pos = -1;
    private PopupWindow mAysTypePopWindow,mOptimizationModeWindow,mDigitalFilterWindow;//定性定量,基线优化
    private PopupWindow mPeekTypeWindow,meltAlgPopWindow;
    private List<String> mAmpAysTypes,mOptimizationModes,mDighitalFilters;
    private List<String> mPeekTypes,mMeltPeakAlgorithms;
    private List<ConfigTarget> configTargets;
    private boolean iscy5Threshold,isFamThreshold, isHexThreshold, isRoxThreshold = false;
    private double famThreshold,hexThreshold,roxThreshold,cy5Threshold;

    //显示表格结果
    private ListView mResultListView;
    private RunBiowayResultAdapter resultAdapter;

    //二次分析按钮
    //  private TextView mSecAnalyseTv;
    private Gson gson;

    private DecimalFormat nf = new DecimalFormat( "0.00");
    private void initpcrview(){
        ///显示结果
        resultChart = (LineChart)mview.findViewById(R.id.bowei_running_result_slan_curve_chart) ;
        mOneTv = (TextView)mview.findViewById(R.id.bowei_result_slan_hole_one_btn) ;
        mTwoTv = (TextView)mview.findViewById(R.id.bowei_result_slan_hole_two_btn) ;
        mThreeTv = (TextView)mview.findViewById(R.id.bowei_result_slan_hole_three_btn) ;
        mFourTv = (TextView)mview.findViewById(R.id.bowei_result_slan_hole_four_btn) ;
        mFiveTv = (TextView)mview.findViewById(R.id.bowei_result_slan_hole_five_btn) ;
        mSixTv = (TextView)mview.findViewById(R.id.bowei_result_slan_hole_six_btn) ;
        mSevenTv = (TextView)mview.findViewById(R.id.bowei_result_slan_hole_seven_btn) ;
        mEightTv= (TextView)mview.findViewById(R.id.bowei_result_slan_hole_eight_btn) ;
        mAllHoleTv = (TextView)mview.findViewById(R.id.bowei_result_slan_running_all_hole_ck) ;
        mAllCheckTv = (MiniCheckText)mview.findViewById(R.id.bowei_result_slan_all_channel_check_tv);
        mFamChannelTv = (MiniCheckText)mview.findViewById(R.id.bowei_running_slan_result_fam_tv) ;
        mHexChannelTv = (MiniCheckText)mview.findViewById(R.id.bowei_running_slan_result_hex_tv) ;
        mRoxChannelTv= (MiniCheckText)mview.findViewById(R.id.bowei_running_slan_result_rox_tv) ;
        mCY5ChannelTv= (MiniCheckText)mview.findViewById(R.id.bowei_running_slan_result_cy5_tv) ;

        initChartViewListener();
    }
    private void initVariable(){
        gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        configTargets = new ArrayList<ConfigTarget>();
        mRawCurveList = new ArrayList<BiowayCurveList>();
        mCheckRawList = new ArrayList<BiowayCurveList>();

        mAMPCurveList = new ArrayList<BiowayCurveList>();
        mCheckAMPList = new ArrayList<BiowayCurveList> ();

        mMeltCurveList = new ArrayList<BiowayCurveList>();
        mCheckMeltList = new ArrayList<BiowayCurveList>();

        mPeakCurveList = new ArrayList<BiowayCurveList>();
        mCheckPeakList = new ArrayList<BiowayCurveList>();

        showCurveList = new ArrayList<BiowayCurveList>();
        mExlResults = new ArrayList<BiowayExlResult>();
        mCheckExlResults = new ArrayList<BiowayExlResult>();

        initParamSettingView();
    }
    private void initParamSettingView(){
        //    mSecAnalyseTv = (TextView)mview.findViewById(R.id.bowei_running_slan_sec_analyse_tv);
        mResultChartLayout = (LinearLayout)mview.findViewById(R.id.bowei_result_slan_curve_chart_layout);
        mSecAnalyseLayout = (RelativeLayout) mview.findViewById(R.id.bowei_slan_result_sec_analyse_layout);
        mBasicParamTitleTv = (TextView)mview.findViewById(R.id.bowei_slan_result_basic_param_title_tv);
        mMeltParamTitleTv = (TextView)mview.findViewById(R.id.bowei_slan_result_melt_param_title_tv);
        mAmpAlgorithmLv = (ListView)mview.findViewById(R.id.bowei_slan_result_sec_analyse_amp_algorithm_lv);
        mAmpParamterLayout = (LinearLayout)mview.findViewById(R.id.bowei_slan_result_sec_analyse_amp_param_layout);
        mMeltParamterLayout = (LinearLayout)mview.findViewById(R.id.bowei_slan_result_sec_analyse_melt_param_layout);
        mMeltParamterLv = (ListView)mview.findViewById(R.id.bowei_slan_result_sec_analyse_melt_paramter_lv);
        mAmpAlgorithmAdapter = new AmpAlgorithmAdapter(getContext());
        mMeltParamterAdapter = new MeltParamterAdapter(getContext());
        mAmpAlgorithmLv.setAdapter(mAmpAlgorithmAdapter);
        mMeltParamterLv.setAdapter(mMeltParamterAdapter);

        mResultExlLayout = (HorizontalScrollView)mview.findViewById(R.id.bowei_result_slan_excel_list_scrollview) ;
        mResultListView = (ListView)mview.findViewById(R.id.bowei_result_slan_list_view) ;
        resultAdapter = new RunBiowayResultAdapter(getContext());
        resultAdapter.setResultList(mCheckExlResults);
        mResultListView.setAdapter(resultAdapter);

        initAmpDigitalFilter();
        initAmpAysTypeWindow();
        initMeltPeakAlgorithm();
        initOptimizationModeWindow();
        initPeekTypeWindow();
    }

    private void initChartViewListener(){
        initVariable();
        mCurveChartGg = (RadioGroup)mview.findViewById(R.id.bowei_result_slan_curve_layout);
        mBasicParamTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //基本参数
                mBasicParamTitleTv.setTextColor(getResources().getColor(R.color.white));
                mBasicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
                mMeltParamTitleTv.setTextColor(getResources().getColor(R.color.black));
                mMeltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
                mAmpParamterLayout.setVisibility(View.VISIBLE);
                mMeltParamterLayout.setVisibility(View.GONE);
                if(null != mAmpAlgorithmAdapter){
                    mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                }
            }
        });
        mMeltParamTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //熔解曲线参数
                mMeltParamTitleTv.setTextColor(getResources().getColor(R.color.white));
                mMeltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
                mBasicParamTitleTv.setTextColor(getResources().getColor(R.color.black));
                mBasicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
                mAmpParamterLayout.setVisibility(View.GONE);
                mMeltParamterLayout.setVisibility(View.VISIBLE);
                if(null != mMeltParamterAdapter){
                    mMeltParamterAdapter.setMeltParamter(configTargets);
                }
            }
        });
        mCurveChartGg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.bowei_result_slan_rawcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        //  mChannelLayout.setVisibility(View.VISIBLE);
                        //   mHoleLayout.setVisibility(View.VISIBLE);
                        //       mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 1;
                    getClickRawCurve();
                    drawRawCurve();
                } else if (checkedId == R.id.bowei_result_slan_ampcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        //  mChannelLayout.setVisibility(View.VISIBLE);
                        //  mHoleLayout.setVisibility(View.VISIBLE);
                        //     mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 2;
                    getClickAMPCurve();
                    drawAMPCurve();
                } else if (checkedId == R.id.bowei_result_slan_meltcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        //  mChannelLayout.setVisibility(View.VISIBLE);
                        //  mHoleLayout.setVisibility(View.VISIBLE);
                        //      mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 3;
                    getClickMeltCurve();
                    drawMeltCurve();
                } else if (checkedId == R.id.bowei_result_slan_peakcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        // mChannelLayout.setVisibility(View.VISIBLE);
                        // mHoleLayout.setVisibility(View.VISIBLE);
                        //    mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 4;
                    getClickPeakCurve();
                    drawPeakCurve();
                }
//                else if (checkedId == R.id.bowei_result_slan_list_rb) {
//                    currentView = 5;
//                    if (!isExcelVisible) {
//                        isChartVisible = false;
//                        isExcelVisible = true;
//                        isSecAysVisible = false;
//                    }
//                    if(isExcelVisible){
//                        // mChannelLayout.setVisibility(View.VISIBLE);
//                        //   mHoleLayout.setVisibility(View.VISIBLE);
//                        mResultExlLayout.setVisibility(View.VISIBLE);
//                        mResultChartLayout.setVisibility(View.GONE);
//                        mSecAnalyseLayout.setVisibility(View.GONE);
//                        mSecAnalyseTv.setVisibility(View.GONE);
//                    }
//                    getShowExlResults();
//                    MessageEvent messageEvent = new MessageEvent();
//                    messageEvent.setMsgType(MsgType.MSG_SHOW_EXCEL_RESULT);
//                    EventBus.getDefault().post(messageEvent);
//                }else if(checkedId ==R.id.bowei_result_slan_set_paramter_rb){
//                    //参数设置
//                    currentView = 6;
//                    if (!isSecAysVisible) {
//                        isChartVisible = false;
//                        isExcelVisible = false;
//                        isSecAysVisible = true;
//                    }
//                    if(isSecAysVisible){
//                        //  mChannelLayout.setVisibility(View.GONE);
//                        //  mHoleLayout.setVisibility(View.INVISIBLE);
//                        mSecAnalyseTv.setVisibility(View.VISIBLE);
//                        mResultExlLayout.setVisibility(View.GONE);
//                        mResultChartLayout.setVisibility(View.GONE);
//                        mSecAnalyseLayout.setVisibility(View.VISIBLE);
//                        //获取二次分析进行分析
//                        if(isMelt){
//                            mMeltParamTitleTv.setVisibility(View.VISIBLE);
//                        }else {
//                            mMeltParamTitleTv.setVisibility(View.INVISIBLE);
//                        }
//                        if(null != configTargets){
//                            if(null != mAmpAlgorithmAdapter){
//                                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
//                            }
//                            if(null != mMeltParamterAdapter){
//                                mMeltParamterAdapter.setMeltParamter(configTargets);
//                            }
//                        }
//                    }
//                }
            }
        });

        biowayHelper.setOnExperimentCallback(new OnExperimentCallback() {
            @Override
            public void onSaveExToDbResult(boolean result) {
                Log.i("AAA","onParseFinish");
            }

            @Override
            public void onParseFinish() {
                //解析完成显示数据
                Log.i("AAA","onParseFinish");
                MessageEvent resultMsg = new MessageEvent();
                resultMsg.setMsgType(MsgType.MSG_SHOW_RESULT);
                EventBus.getDefault().postSticky(resultMsg);
            }

            @Override
            public void onSaveJsonResult(boolean result, String message) {

            }

            @Override
            public void onSecondParseFinish(boolean result) {
                Log.i("BBB","二次分析解析" +result);
                if (result) {
                    MessageEvent resultMsg = new MessageEvent();
                    resultMsg.setMsgType(MsgType.MSG_SHOW_SECOND_RESULT);
                    EventBus.getDefault().postSticky(resultMsg);
                } else {
                    /*String failMsg = getResources().getString(R.string.toast_second_analyse_fail);
                    MessageEvent failEvent = new MessageEvent();
                    failEvent.setMsgType(1);
                    failEvent.setData(failMsg);
                    EventBus.getDefault().postSticky(failEvent);*/
                }
            }

            @Override
            public void onUpdateFinish(boolean result) {

            }
        });
//        openFileTv = (TextView)mview.findViewById(R.id.bowei_open_file_tv) ;
//        openFileTv.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                try {
//                    openBiowayExFile();
//                } catch (JSONException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        });

        isOneCheck = isTwoCheck = isThreeCheck = isFourCheck = isFiveCheck = isSixCheck = isSevenCheck = isEightCheck = false;
        mOneTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isOneCheck = !isOneCheck;
                if (!isOneCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isOneCheck, (TextView) view);
                updateWellResult();
            }
        });
        mTwoTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isTwoCheck = !isTwoCheck;
                if (!isTwoCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isTwoCheck, (TextView) view);
                updateWellResult();
            }
        });
        mThreeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isThreeCheck = !isThreeCheck;
                if (!isThreeCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isThreeCheck, (TextView) view);
                updateWellResult();
            }
        });
        mFourTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFourCheck = !isFourCheck;
                if (!isFourCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isFourCheck, (TextView) view);
                updateWellResult();
            }
        });
        mFiveTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFiveCheck = !isFiveCheck;
                if (!isFiveCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isFiveCheck, (TextView) view);
                updateWellResult();
            }
        });
        mSixTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSixCheck = !isSixCheck;
                if (!isSixCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isSixCheck, (TextView) view);
                updateWellResult();
            }
        });
        mSevenTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSevenCheck = !isSevenCheck;
                if (!isSevenCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isSevenCheck, (TextView) view);
                updateWellResult();
            }
        });
        mEightTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isEightCheck = !isEightCheck;
                if (!isEightCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isEightCheck, (TextView) view);
                updateWellResult();
            }
        });
        mAllHoleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAllholeChecked();
            }
        });

        mFamChannelTv.setOnClickListener(new MiniCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isFAMCheck = isCheck;
                if(!isFAMCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isHEXCheck){
                        isAllChanelCheck = true;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mFamMsg = new MessageEvent();
                mFamMsg.setMsgType(MsgType.MSG_SHOW_FAM_CHANNEL_CURVE);
                EventBus.getDefault().post(mFamMsg);
            }
        });
        mHexChannelTv.setOnClickListener(new MiniCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isHEXCheck = isCheck;
                if(!isHEXCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isFAMCheck ){
                        isAllChanelCheck = true;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mHEXMsg = new MessageEvent();
                mHEXMsg.setMsgType(MsgType.MSG_SHOW_HEX_CHANNEL_CURVE);
                EventBus.getDefault().post(mHEXMsg);
            }
        });
        mRoxChannelTv.setOnClickListener(new MiniCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isROXCheck = isCheck;
                if(!isROXCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isFAMCheck && isCY5Check && isHEXCheck){
                        isAllChanelCheck = true;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mROXMsg = new MessageEvent();
                mROXMsg.setMsgType(MsgType.MSG_SHOW_ROX_CHANNEL_CURVE);
                EventBus.getDefault().post(mROXMsg);
            }
        });
        mCY5ChannelTv.setOnClickListener(new MiniCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isCY5Check = isCheck;
                if(!isCY5Check){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isHEXCheck && isFAMCheck){
                        isAllChanelCheck = true;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mCY5Msg = new MessageEvent();
                mCY5Msg.setMsgType(MsgType.MSG_SHOW_CY5_CHANNEL_CURVE);
                EventBus.getDefault().post(mCY5Msg);
            }
        });
        mAllCheckTv.setOnClickListener(new MiniCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
               setAllcheckHole(isCheck);
            }
        });

        mAmpAlgorithmAdapter.setOnItemClickListener(new AmpAlgorithmAdapter.OnItemClickListener() {
            @Override
            public void onQualitativeAnalysis(View view, int position) {
                ampAlogrithm_pos = position;
                expandQualitativeAnalysis(view);
            }

            @Override
            public void onBeginBase(View view, int position) {
                ampAlogrithm_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    ConfigTarget configTarget = configTargets.get(position);
                    String beginBaseline = String.valueOf(configTarget.getBeginBaseline());
                    showNumKeyboardDialog(3,view,beginBaseline,-1);
                }
            }

            @Override
            public void onEndbaseLine(View view, int position) {
                ampAlogrithm_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    ConfigTarget configTarget = configTargets.get(position);
                    String endBaseline = String.valueOf(configTarget.getEndBaseline());
                    showNumKeyboardDialog(2,view,endBaseline,-1);
                }
            }

            @Override
            public void onOptimizationMode(View view, int position) {
                ampAlogrithm_pos = position;
                expandOptimizationMode(view);
            }

            @Override
            public void onChangeThreshold(View view, int position) {
                ampAlogrithm_pos = position;
                if(null != configTargets &&configTargets.size()>0){
                    ConfigTarget configTarget = configTargets.get(position);
                    String threshold = nf.format(configTarget.getThreshold());
                    showNumKeyboardDialog(4,view,threshold,-1);
                }
            }

            @Override
            public void onDigitalFilter(View view, int position) {
                ampAlogrithm_pos = position;
                expandDigitalFilter(view);
            }

            @Override
            public void onAutoThreshold(int position) {
                ampAlogrithm_pos = position;
                if(null != configTargets &&configTargets.size()>0){
                    ConfigTarget configTarget = configTargets.get(position);
                    if(configTarget.isAutoThreshold()){
                        configTarget.setAutoThreshold(false);
                    }else {
                        configTarget.setAutoThreshold(true);
                    }
                    mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                }
            }
        });

        mMeltParamterAdapter.setOnItemClickListener(new MeltParamterAdapter.OnItemClickListener() {
            @Override
            public void onChangeMinTemperature(View view, int position) {
                meltParamter_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    double min_temp = configTargets.get(position).getMinTemperature();
                    String minTemp = String.valueOf(min_temp);
                    showNumKeyboardDialog(5,view,minTemp,-1);
                }
            }

            @Override
            public void onChangeMaxTemperature(View view, int position) {
                meltParamter_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    double max_temp = configTargets.get(position).getMaxTemperature();
                    String maxTemp = String.valueOf(max_temp);
                    showNumKeyboardDialog(6,view,maxTemp,-1);
                }
            }

            @Override
            public void onMeltPeakAlgorithmTv(View view, int position) {
                meltParamter_pos = position;
                expandMeltPeakAlg(view);
            }

            @Override
            public void onChangePeekHighthreshold(View view, int position) {
                meltParamter_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    double peekHighThreshold = configTargets.get(position).getPeekHighThreshold();
                    String peekHighThresholdStr = String.valueOf(peekHighThreshold);
                    showNumKeyboardDialog(7,view,peekHighThresholdStr,-1);
                }
            }

            @Override
            public void onPeekType(View view, int position) {
                meltParamter_pos = position;
                expandPeekType(view);
            }
        });

        //二次分析
//        mSecAnalyseTv.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                secondAnalayse();
//            }
//        });
        mAllCheckTv.setChecked();
        setAllholeChecked();
    }

    private void setAllholeChecked() {
        if (isAllCheck) {
            isAllCheck = false;
            setAllHoleUnCheck();
        } else {
            isAllCheck = true;
            setAllHoleCheck();

        }
        updateWellResult();
        setHodeState(isAllCheck, mAllHoleTv);
    }

    private void setAllcheckHole(boolean isCheck) {
        isAllChanelCheck = isCheck;
        if(isAllChanelCheck){
            isHEXCheck = true;
            isFAMCheck = true;
            isROXCheck = true;
            isCY5Check = true;

        }else{
            isHEXCheck = false;
            isFAMCheck = false;
            isROXCheck = false;
            isCY5Check = false;
        }
        setChannelState();
//        MessageEvent mAllChanelMsg = new MessageEvent();
//        mAllChanelMsg.setMsgType(MsgType.MSG_SHOW_ALL_CHANEL_RESULT);
//        EventBus.getDefault().post(mAllChanelMsg);
    }

    /*二次分析
     * 对话框输入数值点击确认二次分析，调用SLANManager方法
     */
    private void secondAnalayse(){
        if(null != configTargets && configTargets.size()>0){
            showLoadingDialog("二次分析中...");
            //将模板下的判断文件传入
            new Thread(new Runnable() {
                @Override
                public void run() {
                    biowayHelper.secondSLANRunExAnalyse(configTargets, null,0,null,null,null);
                }
            }).start();

        }

    }

    private void  setChannelState(){
        if(isFAMCheck){
            mFamChannelTv.setCheckState(R.drawable.item_chanel_check,isFAMCheck);
        }else {
            mFamChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isFAMCheck);
        }
        if(isHEXCheck){
            mHexChannelTv.setCheckState(R.drawable.item_chanel_check,isHEXCheck);
        }else{
            mHexChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isHEXCheck);
        }

        if(isROXCheck){
            mRoxChannelTv.setCheckState(R.drawable.item_chanel_check,isROXCheck);
        }else{
            mRoxChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isROXCheck);
        }

        if(isCY5Check){
            mCY5ChannelTv.setCheckState(R.drawable.item_chanel_check,isCY5Check);
        }else{
            mCY5ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isCY5Check);
        }


        if(isCY5Check && isFAMCheck &&isROXCheck && isHEXCheck ){
            isAllChanelCheck = true;
            mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
        }else{
            isAllChanelCheck = false;
            mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
        }
    }

    /**
     *
     * @param check
     * @param textView
     */
    private void setHodeState(boolean check,TextView textView){
        textView.setClickable(true);

        if(!check){
            textView.setBackgroundResource(R.drawable.shape_oval_gray_bg);
            textView.setTextColor(getResources().getColor(R.color.blue));
        }else{
            textView.setBackgroundResource(R.drawable.shape_oval_blue_bg);
            textView.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void checkAllHole(){
        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck) {
            if(!isAllCheck){
                isAllCheck = true;
                setHodeState(isAllCheck, mAllHoleTv);
            }
        }
    }

    private void setAllHoleCheck(){
        if(isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck){
            isAllCheck = true;
            setHodeState(isAllCheck,mAllHoleTv);
        }else {
            isOneCheck = true;isTwoCheck = true;isThreeCheck = true;isFourCheck = true;
            isFiveCheck = true;isSixCheck = true;isSevenCheck = true;isEightCheck = true;
            isAllCheck = true;
            setHodeState(isOneCheck,mOneTv);
            setHodeState(isTwoCheck, mTwoTv);
            setHodeState(isThreeCheck, mThreeTv);
            setHodeState(isFourCheck, mFourTv);
            setHodeState(isFiveCheck, mFiveTv);
            setHodeState(isSixCheck, mSixTv);
            setHodeState(isSevenCheck, mSevenTv);
            setHodeState(isEightCheck, mEightTv);
        }

    }


    private void setAllHoleUnCheck(){
        isOneCheck = false;isTwoCheck = false;isThreeCheck = false;
        isFourCheck = false;isFiveCheck = false;isSixCheck = false;
        isSevenCheck = false; isEightCheck = false;
        setHodeState(isOneCheck,mOneTv);
        setHodeState(isTwoCheck, mTwoTv);
        setHodeState(isThreeCheck, mThreeTv);
        setHodeState(isFourCheck, mFourTv);
        setHodeState(isFiveCheck, mFiveTv);
        setHodeState(isSixCheck, mSixTv);
        setHodeState(isSevenCheck, mSevenTv);
        setHodeState(isEightCheck, mEightTv);
    }


    private void updateWellResult(){
        // 根据当前的视图只计算和解析正在展示的曲线，极大节约计算时间与内存占用
        switch (currentView){
            case 1:
                getClickRawCurve();
                drawRawCurve();
                break;
            case 2:
                getClickAMPCurve();
                drawAMPCurve();
                break;
            case 3:
                getClickMeltCurve();
                drawMeltCurve();
                break;
            case 4:
                getClickPeakCurve();
                drawPeakCurve();
                break;
            case 5:
                getShowExlResults();
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setMsgType(MsgType.MSG_SHOW_EXCEL_RESULT);
                EventBus.getDefault().post(messageEvent);
                break;
        }
    }


    private void getShowExlResults(){
        mExlResults.clear();
        List<BiowayExlResult> mAllExResult = biowayHelper.getExlResults();
        for(BiowayExlResult exlResult : mAllExResult) {

            if (isOneCheck && exlResult.getWellNum().equals("A1")) {
                mExlResults.add(exlResult);
            } else if (isTwoCheck && exlResult.getWellNum().equals("A2")) {
                mExlResults.add(exlResult);

            } else if (isThreeCheck && exlResult.getWellNum().equals("A3")) {

                mExlResults.add(exlResult);

            } else if (isFourCheck && exlResult.getWellNum().equals("A4")) {
                mExlResults.add(exlResult);

            } else if (isFiveCheck && exlResult.getWellNum().equals("A5")) {
                mExlResults.add(exlResult);

            } else if (isSixCheck && exlResult.getWellNum().equals("A6")) {
                mExlResults.add(exlResult);

            } else if (isSevenCheck && exlResult.getWellNum().equals("A7")) {
                mExlResults.add(exlResult);

            } else if (isEightCheck && exlResult.getWellNum().equals("A8")) {
                mExlResults.add(exlResult);
            }
        }

        //判断channel和wellNum
        if(null != mExlResults){
            mCheckExlResults.clear();
            if(mExlResults.size()>0){
                for(BiowayExlResult exlResult : mExlResults){
                    if(exlResult.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
                            mCheckExlResults.add(exlResult);
                        }
                    }else if(exlResult.getCurve_hole().equals("HEX")){
                        if(isHEXCheck){
                            mCheckExlResults.add(exlResult);
                        }
                    }else if(exlResult.getCurve_hole().equals("ROX")){
                        if(isROXCheck){
                            mCheckExlResults.add(exlResult);
                        }
                    }else if(exlResult.getCurve_hole().equals("CY5")){
                        if(isCY5Check){
                            mCheckExlResults.add(exlResult);
                        }
                    }
                }
            }
        }
    }

    private void drawRawCurve(){
        if(null != mRawCurveList){
            mCheckRawList.clear();
            if(mRawCurveList.size()>0){
                for(BiowayCurveList curveList : mRawCurveList){
                    if(curveList.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("HEX")){
                        if(isHEXCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("ROX")){
                        if(isROXCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("CY5")){
                        if(isCY5Check){
                            mCheckRawList.add(curveList);
                        }
                    }
                }
            }

        }
        clearChart();
        XAxis xAxis = resultChart.getXAxis();
        YAxis yAxis = resultChart.getAxisLeft();
       /* if(isScaleXRaw){
            xAxis.setAxisMaximum(max_Raw_X);
            xAxis.setAxisMinimum(min_Raw_X);
            yAxis.setAxisMaximum(max_Raw_Y);
            yAxis.setAxisMinimum(min_Raw_Y);
        }else{
            yAxis.resetAxisMinimum();
            yAxis.resetAxisMaximum();
            xAxis.resetAxisMaximum();
            xAxis.resetAxisMinimum();
        }*/
        drawchart(mCheckRawList,resultChart,"RawCurve");
    }

    private void drawAMPCurve(){
        mCheckAMPList.clear();
        if (null != mAMPCurveList && mAMPCurveList.size() > 0) {
            for (BiowayCurveList curveList : mAMPCurveList) {
                if(curveList.getCurve_hole().equals("FAM")){
                    if (isFAMCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("HEX")){
                    if (isHEXCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("ROX")){
                    if (isROXCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("CY5")){
                    if (isCY5Check) {
                        mCheckAMPList.add(curveList);
                    }
                }
            }
        }
        clearChart();
        /*if(!isFamThreshold && !isHexThreshold && !isRoxThreshold && !iscy5Threshold ){
            drawchart(mCheckAMPList, resultChart,"AMPCurve");
        }else{
            XAxis xAxis = resultChart.getXAxis();
            YAxis yAxis = resultChart.getAxisLeft();
            *//*if(isScaleXAmp){
                //坐标缩放
                xAxis.setAxisMaximum(max_Amp_X);
                xAxis.setAxisMinimum(min_Amp_X);
                yAxis.setAxisMaximum(max_Amp_Y);
                yAxis.setAxisMinimum(min_Amp_Y);
            }else{
                yAxis.resetAxisMinimum();
                yAxis.resetAxisMaximum();
                xAxis.resetAxisMaximum();
                xAxis.resetAxisMinimum();
            }*//*
            drawchart(mCheckAMPList, resultChart,"AMPCurve");
        }*/
        drawchart(mCheckAMPList, resultChart,"AMPCurve");
    }

    private void drawMeltCurve() {
        mCheckMeltList.clear();
        if (null != mMeltCurveList) {
            if (mMeltCurveList.size() > 0) {
                for (BiowayCurveList curveList : mMeltCurveList) {
                    if (curveList.getCurve_hole().equals("FAM")) {
                        if (isFAMCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("HEX")) {
                        if (isHEXCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("ROX")) {
                        if (isROXCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("CY5")) {
                        if (isCY5Check) {
                            mCheckMeltList.add(curveList);
                        }
                    }
                }
            }

        }
        clearChart();
      /*  XAxis xAxis = resultChart.getXAxis();
        YAxis yAxis = resultChart.getAxisLeft();
        if(isScaleXMelt){
            xAxis.setAxisMaximum(max_Melt_X);
            xAxis.setAxisMinimum(min_Melt_X);
            yAxis.setAxisMaximum(max_Melt_Y);
            yAxis.setAxisMinimum(min_Melt_Y);
        }else{
            yAxis.resetAxisMaximum();
            yAxis.resetAxisMinimum();
            xAxis.resetAxisMaximum();
            xAxis.resetAxisMinimum();
        }*/
       /* String melt = gson.toJson(mCheckMeltList);
        FileUtils.dumpDeviceInfo(melt,"Melt");
*/

        drawchart(mCheckMeltList, resultChart, "MeltCurve");

    }

    private void drawPeakCurve(){
        if(null != mPeakCurveList) {
            mCheckPeakList.clear();
            if (mPeakCurveList.size() > 0) {
                for (BiowayCurveList curveList : mPeakCurveList) {
                    if (curveList.getCurve_hole().equals("FAM")) {
                        if (isFAMCheck) {
                            mCheckPeakList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("HEX")) {
                        if (isHEXCheck) {
                            mCheckPeakList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("ROX")) {
                        if (isROXCheck) {
                            mCheckPeakList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("CY5")) {
                        if (isCY5Check) {
                            mCheckPeakList.add(curveList);
                        }
                    }
                }

            }
            clearChart();
       /* XAxis xAxis = resultChart.getXAxis();
        YAxis yAxis = resultChart.getAxisLeft();
        if(isScaleXPeak){
            xAxis.setAxisMaximum(max_Peak_X);
            xAxis.setAxisMinimum(min_Peak_X);
            yAxis.setAxisMaximum(max_Peak_Y);
            yAxis.setAxisMinimum(min_Peak_Y);
        }else{
            yAxis.resetAxisMinimum();
            yAxis.resetAxisMaximum();
            xAxis.resetAxisMaximum();
            xAxis.resetAxisMinimum();
        }*/
            drawchart(mCheckPeakList, resultChart, "PeakCurve");
        }
    }
    private void getClickRawCurve(){
        mRawCurveList.clear();
        List<BiowayCurveList> rawList = biowayHelper.getRawCurve();
        for(BiowayCurveList curveList : rawList){
            if(isOneCheck && curveList.getWellNum().equals("A1")){
                mRawCurveList.add(curveList);
            }else if (isTwoCheck && curveList.getWellNum().equals("A2")) {
                mRawCurveList.add(curveList);
            } else if (isThreeCheck && curveList.getWellNum().equals("A3")) {
                mRawCurveList.add(curveList);
            } else if (isFourCheck && curveList.getWellNum().equals("A4")) {
                mRawCurveList.add(curveList);
            } else if (isFiveCheck && curveList.getWellNum().equals("A5")) {
                mRawCurveList.add(curveList);
            } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                mRawCurveList.add(curveList);
            } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                mRawCurveList.add(curveList);
            } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                mRawCurveList.add(curveList);
            }
        }
    }
    //
    private void getClickAMPCurve(){
        mAMPCurveList.clear();
        List<BiowayCurveList> AMPList = biowayHelper.getAMPCurve();
        for(BiowayCurveList curveList : AMPList){
            if(isOneCheck && curveList.getWellNum().equals("A1")){
                mAMPCurveList.add(curveList);
            }else if (isTwoCheck && curveList.getWellNum().equals("A2")) {
                mAMPCurveList.add(curveList);

            } else if (isThreeCheck && curveList.getWellNum().equals("A3")) {
                mAMPCurveList.add(curveList);
            } else if (isFourCheck && curveList.getWellNum().equals("A4")) {
                mAMPCurveList.add(curveList);

            } else if (isFiveCheck && curveList.getWellNum().equals("A5")) {
                mAMPCurveList.add(curveList);

            } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                mAMPCurveList.add(curveList);

            } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                mAMPCurveList.add(curveList);

            } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                mAMPCurveList.add(curveList);
            }
        }
    }

    private void getClickMeltCurve(){
        mMeltCurveList.clear();
        List<BiowayCurveList> meltList = biowayHelper.getMeltCurve();
        if(meltList.size()>0){
            for(BiowayCurveList curveList : meltList){
                if(isOneCheck && curveList.getWellNum().equals("A1")){
                    mMeltCurveList.add(curveList);
                }else if (isTwoCheck && curveList.getWellNum().equals("A2")) {
                    mMeltCurveList.add(curveList);

                } else if (isThreeCheck && curveList.getWellNum().equals("A3")) {

                    mMeltCurveList.add(curveList);

                } else if (isFourCheck && curveList.getWellNum().equals("A4")) {
                    mMeltCurveList.add(curveList);

                } else if (isFiveCheck && curveList.getWellNum().equals("A5")) {
                    mMeltCurveList.add(curveList);

                } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                    mMeltCurveList.add(curveList);

                } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                    mMeltCurveList.add(curveList);

                } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                    mMeltCurveList.add(curveList);
                }
            }
        }

        Log.i("AAA", "getClickMeltCurveResult" +mMeltCurveList.size() );
    }

    private void getClickPeakCurve(){
        mPeakCurveList.clear();
        List<BiowayCurveList> peakLisk = biowayHelper.getPeakCurve();
        if(peakLisk.size()>0){
            for(BiowayCurveList curveList : peakLisk){

                if(isOneCheck && curveList.getWellNum().equals("A1")){
                    mPeakCurveList.add(curveList);
                }else if (isTwoCheck && curveList.getWellNum().equals("A2")) {
                    mPeakCurveList.add(curveList);

                } else if (isThreeCheck && curveList.getWellNum().equals("A3")) {

                    mPeakCurveList.add(curveList);

                } else if (isFourCheck && curveList.getWellNum().equals("A4")) {
                    mPeakCurveList.add(curveList);

                } else if (isFiveCheck && curveList.getWellNum().equals("A5")) {
                    mPeakCurveList.add(curveList);

                } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                    mPeakCurveList.add(curveList);

                } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                    mPeakCurveList.add(curveList);

                } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                    mPeakCurveList.add(curveList);
                }
            }
        }

    }

    /**清除图表**/
    public void clearChart(){
        resultChart.clear();
        resultChart.notifyDataSetChanged();
        resultChart.invalidate();
    }

    private LineChartMarkView markView;
    public void drawchart(List<BiowayCurveList> curve_list, LineChart lineChart, String describe) {
        if (curve_list == null) {
            curve_list = new ArrayList<>();
        }
        if (lineChart != null) {
            Description description = new Description();
            description.setText(describe);
            lineChart.setDescription(description);
            lineChart.setDrawBorders(true);
            //设置chart边框线颜色
            lineChart.setBorderColor(Color.BLACK);
            //设置chart边框线宽度
            lineChart.setBorderWidth(1f);

            List<ILineDataSet> lineDataSets = new ArrayList<>();
            int my_color = PcrValue.colorline[0];

            for (int i = 0; i < curve_list.size(); i++) {
                //entries列表
                LineDataSet set1 = new LineDataSet(curve_list.get(i).getCurve_data(), curve_list.get(i).getCurve_hole());
                //设置每个树状图的颜色，一共有两个方法setColors和setColor
                set1.setDrawCircles(false);
                set1.setDrawCircleHole(false);
                if (!StringUtil.isEmpty(curve_list.get(i).getCurve_hole())) {
                    if (curve_list.get(i).getCurve_hole().equals("FAM")) {
                        my_color = PcrValue.colorline[0];//蓝色
                    } else if (curve_list.get(i).getCurve_hole().equals("HEX")){
                        my_color = PcrValue.colorline[1];//绿色
                    } else if (curve_list.get(i).getCurve_hole().equals("ROX")) {
                        my_color = PcrValue.colorline[2];//黄色
                    } else if (curve_list.get(i).getCurve_hole().equals("CY5")) {
                        my_color = PcrValue.colorline[3];//红色
                    }
                }

                set1.setColor(my_color);
                set1.setCircleColor(my_color);
                //线模式为圆滑曲线(默认折线)
                set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                set1.setDrawValues(false);// 不显示点值
                set1.setDrawCircleHole(false);
                //设置数据点值得字的大小
                //设置图例线的样式
                set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
                //设置图例的文字大小
                set1.setFormSize(15.f);
                lineDataSets.add(set1);
                //controlRn = curve_list.get(0).getControlRn();
            }
            //Step4: List 的数据放在 BarData 里
            LineData lineData = new LineData(lineDataSets);
            //设置树状图上的字的字号
            lineData.setValueTextSize(10);
            lineData.setDrawValues(false);// 不显示具体值
            lineChart.setData(lineData);
            Legend legend = lineChart.getLegend();
            legend.setTextSize(10f);
            legend.setEnabled(false);
            YAxis rightYAxis = lineChart.getAxisRight();
            rightYAxis.setEnabled(false); //右侧Y轴不显示
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.removeAllLimitLines();
            leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            leftAxis.setDrawGridLines(true);

            // Y-Axis ranges reset with 12% top/bottom paddings to prevent clipping on extrema
            leftAxis.resetAxisMinimum();
            leftAxis.resetAxisMaximum();
            leftAxis.setSpaceTop(12f);
            leftAxis.setSpaceBottom(12f);

            if(isFamThreshold && isFAMCheck){
                LimitLine limitLine = initLimitLine(famThreshold,"famThreshold",0);
                leftAxis.addLimitLine(limitLine);
            }
            if(isHexThreshold && isHEXCheck){
                LimitLine limitLine = initLimitLine(hexThreshold,"hexThreshold",1);
                leftAxis.addLimitLine(limitLine);
            }
            if(isRoxThreshold && isROXCheck){
                LimitLine limitLine = initLimitLine(roxThreshold,"roxThreshold",2);
                leftAxis.addLimitLine(limitLine);
            }
            if(iscy5Threshold && isCY5Check){
                LimitLine limitLine = initLimitLine(cy5Threshold,"cy5Threshold",3);
                leftAxis.addLimitLine(limitLine);
            }

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线

            // 智能重设和计算 X 轴的物理极值范围，防止局部更新时出现“前、后坐标或曲线显示不全”的问题
            xAxis.resetAxisMinimum();
            xAxis.resetAxisMaximum();

            float absoluteMinX = Float.MAX_VALUE;
            float absoluteMaxX = -Float.MAX_VALUE;
            boolean hasData = false;
            for (BiowayCurveList curve : curve_list) {
                if (curve.getCurve_data() != null && !curve.getCurve_data().isEmpty()) {
                    hasData = true;
                    for (com.github.mikephil.charting.data.Entry entry : curve.getCurve_data()) {
                        if (entry != null) {
                            if (entry.getX() < absoluteMinX) absoluteMinX = entry.getX();
                            if (entry.getX() > absoluteMaxX) absoluteMaxX = entry.getX();
                        }
                    }
                }
            }

            if (describe != null && (describe.equals("RawCurve") || describe.equals("AMPCurve"))) {
                // PCR 扩增/循环：最小循环必须固定从 cycle 1 开始，最大展示到 40 循环（如果数据中存在更大循环例如 45 或 50，则取更大值）
                xAxis.setAxisMinimum(1f);
                float finalMaxX = Math.max(40f, hasData ? absoluteMaxX : 40f);
                xAxis.setAxisMaximum(finalMaxX);
                xAxis.setSpaceMin(0f);
                xAxis.setSpaceMax(0f);
            } else if (describe != null && (describe.equals("MeltCurve") || describe.equals("PeakCurve"))) {
                // 熔解/峰值反应温度：标准温度通常覆盖 50°C 至 95°C。对最小值和最大值取最安全物理边界
                float finalMinX = Math.min(50f, hasData ? absoluteMinX : 50f);
                float finalMaxX = Math.max(95f, hasData ? absoluteMaxX : 95f);
                xAxis.setAxisMinimum(finalMinX);
                xAxis.setAxisMaximum(finalMaxX);
                xAxis.setSpaceMin(0.5f);
                xAxis.setSpaceMax(0.5f);
            } else {
                xAxis.setSpaceMin(0.5f);
                xAxis.setSpaceMax(0.5f);
            }

            if (lineChart.getMarker() == null && getContext() != null) {
                lineChart.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getContext() != null) {
                            markView = new LineChartMarkView(getContext());
                            lineChart.setMarker(markView);
                        }
                    }
                });
            }

            // 归位历史手势缩放状态，确保在任何更新情况下都是全屏视图
            lineChart.fitScreen();

            // 双重刷新保证曲线在界面上的绘制准确无死角
            if (lineChart.getData() != null) {
                lineChart.getData().notifyDataChanged();
            }
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        }
    }

    private LimitLine initLimitLine(double threshold,String threshold_name,int number){
        LimitLine limitLine = new LimitLine((float) threshold, threshold_name);
        limitLine.setLineWidth(2f);  //设置线宽
        limitLine.setTextSize(10f);   //设置限制线上label字体大小
        switch (number){
            case 0:
                limitLine.setLineColor(PcrValue.colorline[0]);
                break;
            case 1:
                limitLine.setLineColor(PcrValue.colorline[1]);
                break;
            case 2:
                limitLine.setLineColor(PcrValue.colorline[2]);
                break;
            case 3:
                limitLine.setLineColor(PcrValue.colorline[3]);
                break;
        }
        limitLine.setTextColor(R.color.blue);  //设置限制线上label字体的颜色
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);//标签位置
        limitLine.disableDashedLine();
        return limitLine;

    }

    /**
     * 打开保存的结果文件 ????????????????????????????
     */
    private void openBiowayExFile() throws JSONException {
        Log.i("AAA","openBiowayExFile");


        String resultFilePath = "/sdcard/PCR/temp.boweipcrResult";
        // String resultFilePath = "/mnt/sdcard/PCR/BoWeiResult/产物标准熔曲(005).boweipcrResult";
        Experiment experiment = BoWeiService.getInstance().openExperiment(resultFilePath);
        Log.i("AAA","experiment" + experiment);
        //保存
        /*if(experiment != null){
            BoWeiService.getInstance(getApplicationContext())
                    .saveExperiment(experiment,"aaa","/sdcard/PCR/BoWeiResult/");
        }*/

        //解析文件显示结果
        if(experiment != null){
            Log.i("AAA","experiment:" + "parseExperimentResult");
            biowayHelper.parseExperimentResult(experiment,null,null);
        }else{
            Toast.makeText(getContext(),"没有结果文件",Toast.LENGTH_LONG);
        }

        //参数设置

    }

    /**
     * 设置所有的孔位位选定状态
     */
    private void checkWellState(){

        for(BiowayCurveList curveList : showCurveList){

            if(curveList.getWellNum().equals("A1")){
                isOneCheck = true;
                setHodeState(isOneCheck, mOneTv);
            }else if(curveList.getWellNum().equals("A2")){
                isTwoCheck = true;
                setHodeState(isTwoCheck, mTwoTv);
            }else if(curveList.getWellNum().equals("A3")){
                isThreeCheck = true;
                setHodeState(isThreeCheck, mThreeTv);
            } else if(curveList.getWellNum().equals("A4")){
                isFourCheck = true;
                setHodeState(isFourCheck, mFourTv);
            } else if(curveList.getWellNum().equals("A5")){
                isFiveCheck = true;
                setHodeState(isFiveCheck, mFiveTv);
            } else if(curveList.getWellNum().equals("A6")){
                isSixCheck = true;
                setHodeState(isSixCheck, mSixTv);
            } else if(curveList.getWellNum().equals("A7")){
                isSevenCheck = true;
                setHodeState(isSevenCheck, mSevenTv);
            } else if(curveList.getWellNum().equals("A8")){
                isEightCheck = true;
                setHodeState(isEightCheck, mEightTv);
            }
        }

        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck) {
            isAllCheck = true;
            setHodeState(isAllCheck, mAllHoleTv);
        }
    }

    private void getExperimentParamter(){
        iscy5Threshold = isFamThreshold = isHexThreshold = isRoxThreshold = false;
        configTargets.clear();
        if(true){
            if(null != biowayHelper.getExperiment()){
                Experiment experiment = biowayHelper.getExperiment();
                ArrayList<AnalyseParamter> analyseParamters = BoWeiService.getInstance()
                        .getExperimentParamter(experiment);
                if(analyseParamters != null && analyseParamters.size()>0){
                    ArrayList<Target> targets = analyseParamters.get(0).getTargets();
                    if(null != targets && targets.size()>0){
                        //目标名称
                        for(Target target : targets){
                            if(target.getChannelNo()==1){
                                isFamThreshold = true;
                                TargetParamter famParamer = target.getParamter();
                                famThreshold = famParamer.getThreshold();
                                ConfigTarget famTarget = new ConfigTarget();
                                famTarget.setName("FAM");
                                famTarget.setTargetIndex(1);
                                famTarget.setChannelNo(1);
                                famTarget.setColor("#FF0000FF");
                                //扩增曲线参数
                                famTarget.setQualitativeAnalysis(famParamer.getQualitativeAnalysis());
                                int famBeginbase = famParamer.getBeginBaseline();
                                if(famBeginbase == 0){
                                    famBeginbase = 1;
                                }
                                famTarget.setBeginBaseline(famBeginbase);
                                famTarget.setEndBaseline(famParamer.getEndBaseline());
                                famTarget.setDigitalFilter(famParamer.getDigitalFilter());
                                famTarget.setThreshold(famParamer.getThreshold());
                                famTarget.setAutoThreshold(famParamer.getAutoThreshold());
                                famTarget.setOptimizationMode(famParamer.getOptimizationMode());
                                famTarget.setDetectMin(famParamer.getDetectionMin());
                                famTarget.setDetectMax(famParamer.getDetectionMax());
                                if(famParamer instanceof TargetParamterMelt){
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) famParamer;
                                    famTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    famTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    famTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    famTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    famTarget.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;

                                }

                                configTargets.add(famTarget);
                            }else if(target.getChannelNo() == 3){
                                isRoxThreshold = true;
                                TargetParamter roxParamer = target.getParamter();
                                ConfigTarget roxTarget = new ConfigTarget();
                                roxTarget.setTargetIndex(3);
                                roxTarget.setName("ROX");
                                roxTarget.setChannelNo(3);
                                roxTarget.setColor("#FFFF8000");
                                roxTarget.setQualitativeAnalysis(roxParamer.getQualitativeAnalysis());//定性还是定量
                                int roxBeginbase = roxParamer.getBeginBaseline();
                                if(roxBeginbase == 0){
                                    roxBeginbase =1 ;
                                }
                                roxTarget.setBeginBaseline(roxBeginbase);
                                roxTarget.setEndBaseline(roxParamer.getEndBaseline());
                                roxTarget.setDigitalFilter(roxParamer.getDigitalFilter());
                                roxTarget.setThreshold(roxParamer.getThreshold());
                                roxTarget.setAutoThreshold(roxParamer.getAutoThreshold());
                                roxTarget.setOptimizationMode(roxParamer.getOptimizationMode());
                                roxTarget.setDetectMin(roxParamer.getDetectionMin());
                                roxTarget.setDetectMax(roxParamer.getDetectionMax());
                                roxThreshold = roxParamer.getThreshold();
                                if(roxParamer instanceof TargetParamterMelt){
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) roxParamer;
                                    roxTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    roxTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    roxTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    roxTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    roxTarget.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }

                                configTargets.add(roxTarget);
                            }else if(target.getChannelNo() == 2){
                                isHexThreshold = true;
                                TargetParamter hexParamer = target.getParamter();
                                hexThreshold = hexParamer.getThreshold();
                                ConfigTarget hexTarget = new ConfigTarget();
                                hexTarget.setTargetIndex(2);
                                hexTarget.setName("HEX");
                                hexTarget.setChannelNo(2);
                                hexTarget.setColor("#FF00FF00");
                                hexTarget.setQualitativeAnalysis(hexParamer.getQualitativeAnalysis());//定性还是定量
                                int hexBeginbase = hexParamer.getBeginBaseline();
                                if(hexBeginbase == 0){
                                    hexBeginbase = 1;
                                }
                                hexTarget.setBeginBaseline(hexBeginbase);
                                hexTarget.setEndBaseline(hexParamer.getEndBaseline());
                                hexTarget.setDigitalFilter(hexParamer.getDigitalFilter());
                                hexTarget.setAutoThreshold(hexParamer.getAutoThreshold());
                                hexTarget.setThreshold(hexParamer.getThreshold());
                                hexTarget.setAutoThreshold(hexParamer.getAutoThreshold());
                                hexTarget.setOptimizationMode(hexParamer.getOptimizationMode());
                                hexTarget.setDetectMin(hexParamer.getDetectionMin());
                                hexTarget.setDetectMax(hexParamer.getDetectionMax());
                                if(hexParamer instanceof TargetParamterMelt){
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) hexParamer;
                                    hexTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    hexTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    hexTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    hexTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    hexTarget.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }
                                configTargets.add(hexTarget);
                            }else if(target.getChannelNo()== 4){
                                iscy5Threshold = true;
                                TargetParamter cy5Paramer = target.getParamter();
                                cy5Threshold = cy5Paramer.getThreshold();
                                ConfigTarget cy5Target = new ConfigTarget();
                                cy5Target.setTargetIndex(4);
                                cy5Target.setName("CY5");
                                cy5Target.setChannelNo(4);
                                cy5Target.setColor("#FFFF0000");
                                cy5Target.setQualitativeAnalysis(cy5Paramer.getQualitativeAnalysis());//定性还是定量
                                int cy5BeginBase = cy5Paramer.getBeginBaseline();
                                if(cy5BeginBase == 0){
                                    cy5BeginBase =1 ;
                                }
                                cy5Target.setBeginBaseline(cy5BeginBase);
                                cy5Target.setEndBaseline(cy5Paramer.getEndBaseline());
                                cy5Target.setDigitalFilter(cy5Paramer.getDigitalFilter());
                                cy5Target.setThreshold(cy5Paramer.getThreshold());
                                cy5Target.setAutoThreshold(cy5Paramer.getAutoThreshold());
                                cy5Target.setOptimizationMode(cy5Paramer.getOptimizationMode());
                                cy5Target.setDetectMin(cy5Paramer.getDetectionMin());
                                cy5Target.setDetectMax(cy5Paramer.getDetectionMax());
                                if(cy5Paramer instanceof TargetParamterMelt){
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) cy5Paramer;
                                    cy5Target.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    cy5Target.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    cy5Target.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    cy5Target.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    cy5Target.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }
                                configTargets.add(cy5Target);
                            }
                        }
                    }
                }

            }
        }
        Log.i("AAA","isMelt" + isMelt);
        if(null != configTargets){
            if(null != mAmpAlgorithmAdapter){
                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
            }
            if(null != mMeltParamterAdapter){
                mMeltParamterAdapter.setMeltParamter(configTargets);
            }
        }
    }
    /**
     * 扩增曲线参数修改
     */

    private void showNumKeyboardDialog(int dialogType, View view,String s, int tempType){
        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
        if(numKeyboardDialog == null){
            numKeyboardDialog = new NumKeyboardDialog(getContext());
            numKeyboardDialog.setEditText(s);
            numKeyboardDialog.setOnCanlceListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.setOnKeyBoardDoneListener(new NumKeyboardDialog.OnKeyBoardDoneListener() {
                @Override
                public void onKeyBoardDone(String s) {
                    if(dialogType == 1){
                        //检测下限
                        //0-10000，000，000
                       /* double detectMin = (double)StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget rangConfig = mDetectRanges.get(detect_range_pos);
                        rangConfig.setDetectMin(detectMin);

                        ConfigTarget configTarget = getConfigTarget(rangConfig.getChannelNo());
                        configTarget.setDetectMin(detectMin);
                        detectRangeAdapter.setRangeList(mDetectRanges);
                        if(null != mAmpAlgorithmAdapter && null != mMeltParamterAdapter){
                            mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                            mMeltParamterAdapter.setMeltParamter(mParamTargets);
                        }*/
                    }else if(dialogType == 2){
                        //基线终点
                        int endbase = (int) StringUtil.convertToFloat(s,1.0f);
                        if(endbase < 1){
                            Toast.makeText(getContext(),"不能小于1",Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                            configTarget.setEndBaseline(endbase);
                            mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                        }
                    }else if(dialogType == 3){
                        //基线起点
                        int beginbase = (int)StringUtil.convertToFloat(s,1.0f);
                        if(beginbase < 1){
                            Toast.makeText(getContext(),"不能小于1",Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                            configTarget.setBeginBaseline(beginbase);
                            mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                        }
                    }else if(dialogType == 4){
                        //手动阈值
                        double threshold = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                        configTarget.setThreshold(threshold);
                        mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                    }else if(dialogType == 5){
                        //最低温度
                        double minTemp = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                        configTarget.setMinTemperature(minTemp);
                        mMeltParamterAdapter.setMeltParamter(configTargets);
                    }else if(dialogType == 6){
                        //最高温度
                        double maxTemp = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                        configTarget.setMaxTemperature(maxTemp);
                        mMeltParamterAdapter.setMeltParamter(configTargets);
                    }else if(dialogType == 7){
                        //f峰高阈值
                        double peekHighThreshold = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                        configTarget.setPeekHighThreshold(peekHighThreshold);
                        mMeltParamterAdapter.setMeltParamter(configTargets);
                    }
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.show();
        }
    }

    /**
     * 定性/定量
     */
    private void collapseQualitativeAnalysis(){
        if (mAysTypePopWindow != null && mAysTypePopWindow.isShowing()) {
            mAysTypePopWindow.dismiss();
        }
    }

    //展开扩增曲线算法
    private void expandQualitativeAnalysis(View view){
        if(mAmpAysTypes== null && mAmpAysTypes.isEmpty()){
            return;
        }
        mAysTypePopWindow.showAsDropDown(view);
    }

    private void initAmpAysTypeWindow(){
        mAmpAysTypes = new ArrayList<String>();
        mAmpAysTypes.add("定性");
        mAmpAysTypes.add("定量");
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter mAmpAysTypeAdapter=new CustomSpinnerAdapter(getContext(),mAmpAysTypes);
        ListView mAmpAysTypeLv= (ListView) view.findViewById(R.id.spinner_listview);
        mAmpAysTypeLv.setAdapter(mAmpAysTypeAdapter);
        mAysTypePopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAysTypePopWindow.setOutsideTouchable(true);
        mAysTypePopWindow.setFocusable(true);
        mAysTypePopWindow.setContentView(view);
        mAysTypePopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mAysTypePopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseQualitativeAnalysis();
            }
        });

        mAmpAysTypeAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if(position == 0){
                    ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                    configTarget.setQualitativeAnalysis(true);//定性
                }else{
                    ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                    configTarget.setQualitativeAnalysis(false);//定量
                }

                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                collapseQualitativeAnalysis();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private boolean isOptimizationModeExpanded = false;//
    //扩增曲线算法
    private void collapseOptimizationMode(){
        if (mOptimizationModeWindow != null && mOptimizationModeWindow.isShowing()) {
            mOptimizationModeWindow.dismiss();
        }
        isOptimizationModeExpanded = false;
    }

    //展开扩增曲线算法
    private void expandOptimizationMode(View view){
        if(mOptimizationModes== null && mOptimizationModes.isEmpty()){
            return;
        }
        mOptimizationModeWindow.showAsDropDown(view);
        isOptimizationModeExpanded = true;
    }

    private void initOptimizationModeWindow(){
        mOptimizationModes = new ArrayList<String>();
        mOptimizationModes.add("不优化");
        mOptimizationModes.add("自动优化");
        mOptimizationModes.add("手动优化");
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter optimizationModeAdapter=new CustomSpinnerAdapter(getContext(),mOptimizationModes);
        ListView mOptimizationModeLv= (ListView) view.findViewById(R.id.spinner_listview);
        mOptimizationModeLv.setAdapter(optimizationModeAdapter);
        mOptimizationModeWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mOptimizationModeWindow.setOutsideTouchable(true);
        mOptimizationModeWindow.setFocusable(true);
        mOptimizationModeWindow.setContentView(view);
        mOptimizationModeWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mOptimizationModeWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseOptimizationMode();
            }
        });

        optimizationModeAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                configTarget.setOptimizationMode(position);

                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                collapseOptimizationMode();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private boolean isDigitalFilterExpanded = false;
    //扩增数字滤波
    private void collapseDigitalFilter(){
        if (mDigitalFilterWindow != null && mDigitalFilterWindow.isShowing()) {
            mDigitalFilterWindow.dismiss();
        }
        isDigitalFilterExpanded = false;
    }

    //展开扩增曲线算法
    private void expandDigitalFilter(View view){
        if(mDighitalFilters== null && mDighitalFilters.isEmpty()){
            return;
        }
        mDigitalFilterWindow.showAsDropDown(view);
        isDigitalFilterExpanded = true;
    }
    private void initAmpDigitalFilter(){
        String[] digitalFilters= getResources().getStringArray(R.array.AMPDigitalFiler);
        mDighitalFilters = new ArrayList<String>(digitalFilters.length);
        Collections.addAll(mDighitalFilters, digitalFilters);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter digitalFilterAdapter=new CustomSpinnerAdapter(getContext(),mDighitalFilters);
        ListView mDigitalFilterLv= (ListView) view.findViewById(R.id.spinner_listview);
        mDigitalFilterLv.setAdapter(digitalFilterAdapter);
        mDigitalFilterWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mDigitalFilterWindow.setOutsideTouchable(true);
        mDigitalFilterWindow.setFocusable(true);
        mDigitalFilterWindow.setContentView(view);
        mDigitalFilterWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mDigitalFilterWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseDigitalFilter();
            }
        });

        digitalFilterAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                configTarget.setDigitalFilter(position);
                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                collapseDigitalFilter();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }
    /**
     * 熔解曲线参数-熔解峰型
     */
    //扩增曲线算法
    private void collapsePeekType(){
        if (mPeekTypeWindow != null && mPeekTypeWindow.isShowing()) {
            mPeekTypeWindow.dismiss();
        }
    }

    //展开扩增曲线算法
    private void expandPeekType(View view){
        if( mPeekTypes== null && mPeekTypes.isEmpty()){
            return;
        }
        mPeekTypeWindow.showAsDropDown(view);
    }

    private void initPeekTypeWindow(){
        mPeekTypes = new ArrayList<String>();
        mPeekTypes.add("正峰");
        mPeekTypes.add("负峰");
        mPeekTypes.add("正负峰");
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter peekTypeAdapter=new CustomSpinnerAdapter(getContext(),mPeekTypes);
        ListView mPeekTypeLv= (ListView) view.findViewById(R.id.spinner_listview);
        mPeekTypeLv.setAdapter(peekTypeAdapter);
        mPeekTypeWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mPeekTypeWindow.setOutsideTouchable(true);
        mPeekTypeWindow.setFocusable(true);
        mPeekTypeWindow.setContentView(view);
        mPeekTypeWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mPeekTypeWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapsePeekType();
            }
        });

        peekTypeAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                configTarget.setPeekType(position);
                mMeltParamterAdapter.setMeltParamter(configTargets);
                collapsePeekType();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    //熔解曲线参数-熔解算法
    private void expandMeltPeakAlg(View view){
        if(mMeltPeakAlgorithms== null && mMeltPeakAlgorithms.isEmpty()){
            return;
        }
        meltAlgPopWindow.showAsDropDown(view);
    }

    private void collapseMeltPeakAlg(){
        if (meltAlgPopWindow != null && meltAlgPopWindow.isShowing()) {
            meltAlgPopWindow.dismiss();
        }
    }
    private void initMeltPeakAlgorithm(){
        mMeltPeakAlgorithms = new ArrayList<String>();
        mMeltPeakAlgorithms.add("标准算法");
        mMeltPeakAlgorithms.add("高级算法");
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter meltAlgorithmAdapter=new CustomSpinnerAdapter(getContext(),mMeltPeakAlgorithms);
        ListView mMeltPeakAlgorithmLv= (ListView) view.findViewById(R.id.spinner_listview);
        mMeltPeakAlgorithmLv.setAdapter(meltAlgorithmAdapter);
        meltAlgPopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        meltAlgPopWindow.setOutsideTouchable(true);
        meltAlgPopWindow.setFocusable(true);
        meltAlgPopWindow.setContentView(view);
        meltAlgPopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        meltAlgPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseMeltPeakAlg();
            }
        });

        meltAlgorithmAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                configTarget.setMeltPeakAlgorithm(position);
                mMeltParamterAdapter.setMeltParamter(configTargets);
                collapseMeltPeakAlg();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private LoadingDialog loadingDialog ;
    public void showLoadingDialog(String message){
        if(null == loadingDialog){
            loadingDialog = new LoadingDialog(getContext(), message);
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }
    }

    /*
     * 隐藏提示加载
     */
    public void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }
    Experiment pcrExperiment;
    /**
     * 打开保存的结果文件
     */
    public void openBiowayExperiment(){
        if(model!=null) {
            int index = model.index.get();
            Experiment exp = PCRManager.getInstance().getPcrExperiment(index);
            if (exp != null) {
                int dotsCount = getDotsSize(exp);
                if (exp == lastParsedExperiment && dotsCount == lastDotsCount) {
                    // 没有温度循环更新或没有新数据点，直接重用先前的数据列表，极大减轻 UI 耗时
                    return;
                }
                pcrExperiment = exp;
                lastParsedExperiment = exp;
                lastDotsCount = dotsCount;

                biowayHelper.setExperiment(pcrExperiment);
                Log.i(TAG, "openBiowayExperiment index:" + index + " dots: " + dotsCount);
                biowayHelper.parseExperimentResult(pcrExperiment, null, null);
            }
        }
    }
}