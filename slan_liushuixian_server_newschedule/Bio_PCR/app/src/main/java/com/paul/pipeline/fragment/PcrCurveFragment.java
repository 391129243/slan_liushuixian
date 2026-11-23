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

import com.paul.pipeline.utils.DialogUtil;

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
import com.gidi.bio_pcr.slan.SLANHelper;
import com.gidi.bio_pcr.slan.SLANManager;
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
import com.hongshi.slansdk.service.SLANService;
import com.itextpdf.io.util.DateTimeUtil;
import com.paul.pipeline.data.BiowayHelper;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.PCRFinishTaskExecutor;
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
import java.text.SimpleDateFormat;
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

        slanHelper = new SLANHelper();
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
        // 新实验开始时清空所有图表
        clearAllCharts();
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
        ResultMessage resultMessage = onBiowayExperimentFinish(pcrExperiment,starttime,batchno,pcrindex);
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
    public ResultMessage saveBiowayExperiment(Experiment experiment,String starttime,String batchno,int pcr) {
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
            //把模板里的规则文件(.ct/.peak)复制到结果目录,二次分析读结果目录而不是模板目录
            File resultDir = new File(FileUtils.BIOWAY_RESULT_PATH, subFirName);
            File templateDir = new File(FileUtils.FULL_PROCESS_TEMPLATE_PATH, PcrApplication.rootTempName);
            int copied = FileUtils.copyRuleFiles(templateDir, resultDir);
            Log.i(TAG, "copyRuleFiles " + copied + " to " + resultDir.getAbsolutePath());
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
                DialogUtil.showMessage(getContext(), "保存PCR成功!");
                Log.e(TAG,"onSaveSuccess" );
            }

            @Override
            public void onSaveFail() {
                DialogUtil.showMessage(getContext(), "保存PCR失败!");
                Log.e(TAG,"onSaveFail" );
            }
        });
    }
    /**
     * 第一次SLAN pcr结束
     * 保存pcr文件以及保存数据库文件
     */
    private ResultMessage onBiowayExperimentFinish(Experiment newExperiment ,String starttime,String batchno,int pcr) {
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
                    return saveBiowayExperiment(newExperiment, starttime, batchno,pcr);
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
    private LineChart rawChart;  // Raw 曲线专用图表
    private LineChart meltChart; // Melt 曲线专用图表
    private LineChart resultChart; // 保持兼容性，指向当前活动的图表
    // private TextView openFileTv;

    private MiniCheckText mAllCheckTv,mFamChannelTv,mHexChannelTv,mRoxChannelTv,mCY5ChannelTv;

    private boolean isAllChanelCheck = false;
    private boolean isAllCheck = false;
    private boolean isFAMCheck = false;
    private boolean isHEXCheck = false;
    private boolean isROXCheck = false;
    private boolean isCY5Check = false;
    private boolean isAttoCheck = false;
    private boolean isQuasarCheck =false;
    private RadioGroup mCurveChartGg;
    private boolean isA1Check,isA2Check,isA3Check,isA4Check,
            isA5Check,isA6Check,isA7Check,isA8Check = false;
    private boolean isB1Check,isB2Check,isB3Check,isB4Check,
            isB5Check,isB6Check,isB7Check,isB8Check = false;
    private List<BiowayCurveList> mRawCurveList;//原始数据
    private List<BiowayCurveList> mCheckRawList;
    private List<BiowayCurveList> mAMPCurveList;
    private List<BiowayCurveList> mCheckAMPList;

    private List<BiowayCurveList> mMeltCurveList;
    private List<BiowayCurveList> mCheckMeltList;

    private List<BiowayCurveList> mPeakCurveList;
    private List<BiowayCurveList> mCheckPeakList ;

    private List<BiowayCurveList> showCurveList;

    private int currentView = 1;


    private LinearLayout mRawChartLayout;  // Raw 曲线布局
    private LinearLayout mMeltChartLayout; // Melt 曲线布局
    private LinearLayout mResultChartLayout; // 保持兼容性，指向当前活动的布局
    private boolean isChartVisible = true;

    private RelativeLayout mSecAnalyseLayout;
    private TextView mBasicParamTitleTv,mMeltParamTitleTv,mAmpAlgorithmTv;
    private LinearLayout mAmpParamterLayout,mMeltParamterLayout;//扩增曲线参数，熔解曲线参数
    private ListView mAmpAlgorithmLv,mMeltParamterLv;//扩增曲线参数列表，熔解曲线参数列表
    private MeltParamterAdapter mMeltParamterAdapter;
    private AmpAlgorithmAdapter mAmpAlgorithmAdapter;

    private NumKeyboardDialog numKeyboardDialog;

    private List<ConfigTarget> configTargets;
    private boolean iscy5Threshold,isFamThreshold, isHexThreshold, isRoxThreshold = false;
    private double famThreshold,hexThreshold,roxThreshold,cy5Threshold;

    private Gson gson;

    private DecimalFormat nf = new DecimalFormat( "0.00");
    private void initpcrview(){
        ///显示结果
        rawChart = (LineChart)mview.findViewById(R.id.bowei_running_result_slan_raw_chart);
        meltChart = (LineChart)mview.findViewById(R.id.bowei_running_result_slan_melt_chart);
        resultChart = rawChart; // 默认指向 Raw 图表

        mRawChartLayout = (LinearLayout) mview.findViewById(R.id.bowei_result_slan_raw_chart_layout);
        mMeltChartLayout = (LinearLayout) mview.findViewById(R.id.bowei_result_slan_melt_chart_layout);

        mAllCheckTv = (MiniCheckText)mview.findViewById(R.id.bowei_result_slan_all_channel_check_tv);
        mFamChannelTv = (MiniCheckText)mview.findViewById(R.id.bowei_running_slan_result_fam_tv) ;
        mHexChannelTv = (MiniCheckText)mview.findViewById(R.id.bowei_running_slan_result_hex_tv) ;
        mRoxChannelTv= (MiniCheckText)mview.findViewById(R.id.bowei_running_slan_result_rox_tv) ;
        mCY5ChannelTv= (MiniCheckText)mview.findViewById(R.id.bowei_running_slan_result_cy5_tv) ;
        mResultChartLayout = mRawChartLayout; // 默认指向 Raw 布局

        initChartViewListener();
    }
    private void initVariable(){

        isFamThreshold = isHexThreshold = isRoxThreshold = iscy5Threshold = false;
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


    }


    private void initChartViewListener(){
        initVariable();
        mCurveChartGg = (RadioGroup)mview.findViewById(R.id.bowei_result_slan_curve_layout);

        mCurveChartGg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.bowei_result_slan_rawcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                    }
                    if(isChartVisible){
                        // 切换到 Raw 图表 - 只改变可见性，不重新渲染
                        mRawChartLayout.setVisibility(View.VISIBLE);
                        mMeltChartLayout.setVisibility(View.GONE);
                        resultChart = rawChart;
                        mResultChartLayout = mRawChartLayout;
                        if (mSecAnalyseLayout != null) mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 1;
                    getClickRawCurve();
                    // 移除 drawRawCurve() 调用 - 图表在后台已保持同步更新
//                } else if (checkedId == R.id.bowei_result_slan_ampcurve_rb) {
//                    if (!isChartVisible) {
//                        isChartVisible = true;
//                    }
//                    if(isChartVisible){
//                        //  mChannelLayout.setVisibility(View.VISIBLE);
//                        //  mHoleLayout.setVisibility(View.VISIBLE);
//                        //     mSecAnalyseTv.setVisibility(View.GONE);
//                        mResultChartLayout.setVisibility(View.VISIBLE);
//                        if (mSecAnalyseLayout != null) mSecAnalyseLayout.setVisibility(View.GONE);
//                    }
//                    currentView = 2;
//                    getClickAMPCurve();
//                    drawAMPCurve();
                } else if (checkedId == R.id.bowei_result_slan_meltcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                    }
                    if(isChartVisible){
                        // 切换到 Melt 图表 - 只改变可见性，不重新渲染
                        mRawChartLayout.setVisibility(View.GONE);
                        mMeltChartLayout.setVisibility(View.VISIBLE);
                        resultChart = meltChart;
                        mResultChartLayout = mMeltChartLayout;
                        if (mSecAnalyseLayout != null) mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 3;
                    getClickMeltCurve();
                    // 移除 drawMeltCurve() 调用 - 图表在后台已保持同步更新
//                } else if (checkedId == R.id.bowei_result_slan_peakcurve_rb) {
//                    if (!isChartVisible) {
//                        isChartVisible = true;
//                    }
//                    if(isChartVisible){
//                        // mChannelLayout.setVisibility(View.VISIBLE);
//                        // mHoleLayout.setVisibility(View.VISIBLE);
//                        //    mSecAnalyseTv.setVisibility(View.GONE);
//                        mResultChartLayout.setVisibility(View.VISIBLE);
//                        if (mSecAnalyseLayout != null) mSecAnalyseLayout.setVisibility(View.GONE);
//                    }
//                    currentView = 4;
//                    getClickPeakCurve();
//                    drawPeakCurve();
                }
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
                Log.i(TAG,"onParseFinish");
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


        // 孔位通道默认全选
        isA1Check = isA2Check = isA3Check = isA4Check = isA5Check = isA6Check = isA7Check = isA8Check = true;
        isB1Check = isB2Check = isB3Check = isB4Check = isB5Check = isB6Check = isB7Check = isB8Check = true;

        // 颜色通道默认全选
        isFAMCheck = true;
        isHEXCheck = true;
        isROXCheck = true;
        isCY5Check = true;
        isAllChanelCheck = true;


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
                    if(isROXCheck && isCY5Check && isHEXCheck
                        && isAttoCheck && isQuasarCheck){
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
                    if(isROXCheck && isCY5Check && isFAMCheck
                            && isAttoCheck && isQuasarCheck){
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
                    if(isFAMCheck && isCY5Check && isHEXCheck
                            && isAttoCheck && isQuasarCheck){
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
                    if(isROXCheck && isHEXCheck && isFAMCheck
                            && isAttoCheck && isQuasarCheck){
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


        // 设置UI显示为全选状态
        mAllCheckTv.setCheckState(R.drawable.item_chanel_check, isAllChanelCheck);
        mFamChannelTv.setCheckState(R.drawable.item_chanel_check, isFAMCheck);
        mHexChannelTv.setCheckState(R.drawable.item_chanel_check, isHEXCheck);
        mRoxChannelTv.setCheckState(R.drawable.item_chanel_check, isROXCheck);
        mCY5ChannelTv.setCheckState(R.drawable.item_chanel_check, isCY5Check);

        setAllholeChecked();
    }

    private void setAllholeChecked() {

        updateWellResult();

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



    private void updateWellResult(){
        // 同时更新两个图表（后台保持同步），这样切换时只需改变可见性
        // Raw 曲线
        getClickRawCurve();
        drawRawCurve(false);

        // Melt 曲线
        getClickMeltCurve();
        drawMeltCurve(false);
    }



    private void drawRawCurve(){
        drawRawCurve(false);
    }

    private void drawRawCurve(boolean forceClear){
        if(null != mRawCurveList){
            mCheckRawList.clear();
            if(mRawCurveList.size()>0){
                for(BiowayCurveList curveList : mRawCurveList){
                    if(curveList.getCurve_hole().equalsIgnoreCase("FAM")){
                        if(isFAMCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equalsIgnoreCase("HEX")){
                        if(isHEXCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equalsIgnoreCase("ROX")){
                        if(isROXCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equalsIgnoreCase("CY5")){
                        if(isCY5Check){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equalsIgnoreCase("QUASAR")){
                        if (isQuasarCheck) {
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equalsIgnoreCase("ATTO")){
                        if (isAttoCheck) {
                            mCheckRawList.add(curveList);
                        }
                    }
                }
            }

        }
        // forceClear 参数已废弃，统一在 clearModel() 中清空图表
        XAxis xAxis = rawChart.getXAxis();
        YAxis yAxis = rawChart.getAxisLeft();
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
        drawchart(mCheckRawList, rawChart, "RawCurve");
    }

    private void drawAMPCurve(){
        mCheckAMPList.clear();
        if (null != mAMPCurveList && mAMPCurveList.size() > 0) {
            for (BiowayCurveList curveList : mAMPCurveList) {
                if(curveList.getCurve_hole().equalsIgnoreCase("FAM")){
                    if (isFAMCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equalsIgnoreCase("HEX")){
                    if (isHEXCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equalsIgnoreCase("ROX")){
                    if (isROXCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equalsIgnoreCase("CY5")){
                    if (isCY5Check) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equalsIgnoreCase("QUASAR")){
                    if (isQuasarCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equalsIgnoreCase("ATTO")){
                    if (isAttoCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }
            }
        }
        // AMP 曲线已注释，这个 clearChart 调用可以删除或保留以备将来使用
        // clearChart();
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
        drawMeltCurve(false);
    }

    private void drawMeltCurve(boolean forceClear) {
        mCheckMeltList.clear();
        if (null != mMeltCurveList) {
            if (mMeltCurveList.size() > 0) {
                for (BiowayCurveList curveList : mMeltCurveList) {
                    if (curveList.getCurve_hole().equalsIgnoreCase("FAM")) {
                        if (isFAMCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equalsIgnoreCase("HEX")) {
                        if (isHEXCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equalsIgnoreCase("ROX")) {
                        if (isROXCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equalsIgnoreCase("CY5")) {
                        if (isCY5Check) {
                            mCheckMeltList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equalsIgnoreCase("QUASAR")){
                        if (isQuasarCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equalsIgnoreCase("ATTO")){
                        if (isAttoCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    }
                }
            }

        }
        // forceClear 参数已废弃，统一在 clearModel() 中清空图表
      /*  XAxis xAxis = meltChart.getXAxis();
        YAxis yAxis = meltChart.getAxisLeft();
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

        drawchart(mCheckMeltList, meltChart, "MeltCurve");

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
            // Peak 曲线已注释，这个 clearChart 调用可以删除或保留以备将来使用
            // clearChart();
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
            if(isA1Check && curveList.getWellNum().equals("A1")){
                mRawCurveList.add(curveList);
            }else if (isA2Check && curveList.getWellNum().equals("A2")) {
                mRawCurveList.add(curveList);
            } else if (isA3Check && curveList.getWellNum().equals("A3")) {
                mRawCurveList.add(curveList);
            } else if (isA4Check && curveList.getWellNum().equals("A4")) {
                mRawCurveList.add(curveList);
            } else if (isA5Check && curveList.getWellNum().equals("A5")) {
                mRawCurveList.add(curveList);
            } else if (isA6Check && curveList.getWellNum().equals("A6")) {
                mRawCurveList.add(curveList);
            } else if (isA7Check && curveList.getWellNum().equals("A7")) {
                mRawCurveList.add(curveList);
            } else if (isA8Check && curveList.getWellNum().equals("A8")) {
                mRawCurveList.add(curveList);
            } else if (isB1Check && curveList.getWellNum().equals("B1")) {
                mRawCurveList.add(curveList);
            } else if (isB2Check && curveList.getWellNum().equals("B2")) {
                mRawCurveList.add(curveList);
            } else if (isB3Check && curveList.getWellNum().equals("B3")) {
                mRawCurveList.add(curveList);
            } else if (isB4Check && curveList.getWellNum().equals("B4")) {
                mRawCurveList.add(curveList);
            } else if (isB5Check && curveList.getWellNum().equals("B5")) {
                mRawCurveList.add(curveList);
            } else if (isB6Check && curveList.getWellNum().equals("B6")) {
                mRawCurveList.add(curveList);
            } else if (isB7Check && curveList.getWellNum().equals("B7")) {
                mRawCurveList.add(curveList);
            } else if (isB8Check && curveList.getWellNum().equals("B8")) {
                mRawCurveList.add(curveList);
            }
        }
    }
    //
    private void getClickAMPCurve(){
        mAMPCurveList.clear();
        List<BiowayCurveList> AMPList = biowayHelper.getAMPCurve();
        for(BiowayCurveList curveList : AMPList){
            if(isA1Check && curveList.getWellNum().equals("A1")){
                mAMPCurveList.add(curveList);
            }else if (isA2Check && curveList.getWellNum().equals("A2")) {
                mAMPCurveList.add(curveList);

            } else if (isA3Check && curveList.getWellNum().equals("A3")) {
                mAMPCurveList.add(curveList);
            } else if (isA4Check && curveList.getWellNum().equals("A4")) {
                mAMPCurveList.add(curveList);

            } else if (isA5Check && curveList.getWellNum().equals("A5")) {
                mAMPCurveList.add(curveList);

            } else if (isA6Check && curveList.getWellNum().equals("A6")) {
                mAMPCurveList.add(curveList);

            } else if (isA7Check && curveList.getWellNum().equals("A7")) {
                mAMPCurveList.add(curveList);

            } else if (isA8Check && curveList.getWellNum().equals("A8")) {
                mAMPCurveList.add(curveList);
            } else if (isB1Check && curveList.getWellNum().equals("B1")) {
                mAMPCurveList.add(curveList);
            } else if (isB2Check && curveList.getWellNum().equals("B2")) {
                mAMPCurveList.add(curveList);
            } else if (isB3Check && curveList.getWellNum().equals("B3")) {
                mAMPCurveList.add(curveList);
            } else if (isB4Check && curveList.getWellNum().equals("B4")) {
                mAMPCurveList.add(curveList);
            } else if (isB5Check && curveList.getWellNum().equals("B5")) {
                mAMPCurveList.add(curveList);
            } else if (isB6Check && curveList.getWellNum().equals("B6")) {
                mAMPCurveList.add(curveList);
            } else if (isB7Check && curveList.getWellNum().equals("B7")) {
                mAMPCurveList.add(curveList);
            } else if (isB8Check && curveList.getWellNum().equals("B8")) {
                mAMPCurveList.add(curveList);
            }
        }
    }

    private void getClickMeltCurve(){
        mMeltCurveList.clear();
        List<BiowayCurveList> meltList = biowayHelper.getMeltCurve();
        if(meltList.size()>0){
            for(BiowayCurveList curveList : meltList){
                if(isA1Check && curveList.getWellNum().equals("A1")){
                    mMeltCurveList.add(curveList);
                }else if (isA2Check && curveList.getWellNum().equals("A2")) {
                    mMeltCurveList.add(curveList);

                } else if (isA3Check && curveList.getWellNum().equals("A3")) {

                    mMeltCurveList.add(curveList);

                } else if (isA4Check && curveList.getWellNum().equals("A4")) {
                    mMeltCurveList.add(curveList);

                } else if (isA5Check && curveList.getWellNum().equals("A5")) {
                    mMeltCurveList.add(curveList);

                } else if (isA6Check && curveList.getWellNum().equals("A6")) {
                    mMeltCurveList.add(curveList);

                } else if (isA7Check && curveList.getWellNum().equals("A7")) {
                    mMeltCurveList.add(curveList);

                } else if (isA8Check && curveList.getWellNum().equals("A8")) {
                    mMeltCurveList.add(curveList);
                } else if (isB1Check && curveList.getWellNum().equals("B1")) {
                    mMeltCurveList.add(curveList);
                } else if (isB2Check && curveList.getWellNum().equals("B2")) {
                    mMeltCurveList.add(curveList);
                } else if (isB3Check && curveList.getWellNum().equals("B3")) {
                    mMeltCurveList.add(curveList);
                } else if (isB4Check && curveList.getWellNum().equals("B4")) {
                    mMeltCurveList.add(curveList);
                } else if (isB5Check && curveList.getWellNum().equals("B5")) {
                    mMeltCurveList.add(curveList);
                } else if (isB6Check && curveList.getWellNum().equals("B6")) {
                    mMeltCurveList.add(curveList);
                } else if (isB7Check && curveList.getWellNum().equals("B7")) {
                    mMeltCurveList.add(curveList);
                } else if (isB8Check && curveList.getWellNum().equals("B8")) {
                    mMeltCurveList.add(curveList);
                }
            }
        }

        Log.i(TAG, "getClickMeltCurveResult" +mMeltCurveList.size() );
    }

    private void getClickPeakCurve(){
        mPeakCurveList.clear();
        List<BiowayCurveList> peakLisk = biowayHelper.getPeakCurve();
        if(peakLisk.size()>0){
            for(BiowayCurveList curveList : peakLisk){

                if(isA1Check && curveList.getWellNum().equals("A1")){
                    mPeakCurveList.add(curveList);
                }else if (isA2Check && curveList.getWellNum().equals("A2")) {
                    mPeakCurveList.add(curveList);

                } else if (isA3Check && curveList.getWellNum().equals("A3")) {

                    mPeakCurveList.add(curveList);

                } else if (isA4Check && curveList.getWellNum().equals("A4")) {
                    mPeakCurveList.add(curveList);

                } else if (isA5Check && curveList.getWellNum().equals("A5")) {
                    mPeakCurveList.add(curveList);

                } else if (isA6Check && curveList.getWellNum().equals("A6")) {
                    mPeakCurveList.add(curveList);

                } else if (isA7Check && curveList.getWellNum().equals("A7")) {
                    mPeakCurveList.add(curveList);

                } else if (isA8Check && curveList.getWellNum().equals("A8")) {
                    mPeakCurveList.add(curveList);
                } else if (isB1Check && curveList.getWellNum().equals("B1")) {
                    mPeakCurveList.add(curveList);
                } else if (isB2Check && curveList.getWellNum().equals("B2")) {
                    mPeakCurveList.add(curveList);
                } else if (isB3Check && curveList.getWellNum().equals("B3")) {
                    mPeakCurveList.add(curveList);
                } else if (isB4Check && curveList.getWellNum().equals("B4")) {
                    mPeakCurveList.add(curveList);
                } else if (isB5Check && curveList.getWellNum().equals("B5")) {
                    mPeakCurveList.add(curveList);
                } else if (isB6Check && curveList.getWellNum().equals("B6")) {
                    mPeakCurveList.add(curveList);
                } else if (isB7Check && curveList.getWellNum().equals("B7")) {
                    mPeakCurveList.add(curveList);
                } else if (isB8Check && curveList.getWellNum().equals("B8")) {
                    mPeakCurveList.add(curveList);
                }
            }
        }

    }

    /**清除图表**/
    public void clearChart(LineChart chart){
        if (chart != null) {
            chart.clear();
            chart.notifyDataSetChanged();
            chart.invalidate();
        }
    }

    /**清除所有图表（新实验开始时调用）**/
    public void clearAllCharts(){
        clearChart(rawChart);
        clearChart(meltChart);
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

            // 禁用手势缩放和拖动
            lineChart.setScaleEnabled(false);
            lineChart.setDragEnabled(false);
            lineChart.setPinchZoom(false);
            lineChart.setDoubleTapToZoomEnabled(false);

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
                    }else if (curve_list.get(i).getCurve_hole().equals("ATTO")) {
                        my_color = PcrValue.colorline[4];//红色
                    }else if (curve_list.get(i).getCurve_hole().equals("QUASAR")) {
                        my_color = PcrValue.colorline[5];//红色
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

            // 智能重设和计算 X/Y 轴的物理极值范围，防止局部更新时出现"前、后坐标或曲线显示不全"的问题
            xAxis.resetAxisMinimum();
            xAxis.resetAxisMaximum();

            float absoluteMinX = Float.MAX_VALUE;
            float absoluteMaxX = -Float.MAX_VALUE;
            float absoluteMinY = Float.MAX_VALUE;
            float absoluteMaxY = -Float.MAX_VALUE;
            boolean hasData = false;

            if (describe != null && describe.equals("MeltCurve")) {
                Log.d("MeltCurve", "开始遍历曲线数据，curve_list.size = " + curve_list.size());
            }

            for (BiowayCurveList curve : curve_list) {
                if (curve.getCurve_data() != null && !curve.getCurve_data().isEmpty()) {
                    hasData = true;
                    if (describe != null && describe.equals("MeltCurve")) {
                        Log.d("MeltCurve", "曲线 " + curve.getCurve_hole() + " 数据点数量: " + curve.getCurve_data().size());
                    }
                    for (com.github.mikephil.charting.data.Entry entry : curve.getCurve_data()) {
                        if (entry != null) {
                            if (entry.getX() < absoluteMinX) absoluteMinX = entry.getX();
                            if (entry.getX() > absoluteMaxX) absoluteMaxX = entry.getX();
                            if (entry.getY() < absoluteMinY) absoluteMinY = entry.getY();
                            if (entry.getY() > absoluteMaxY) absoluteMaxY = entry.getY();
                        }
                    }
                }
            }

            if (describe != null && describe.equals("MeltCurve")) {
                Log.d("MeltCurve", "数据遍历完成，hasData = " + hasData);
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

            // fitScreen() 之后再设置自定义坐标范围，避免被重置
            if (describe != null && describe.equals("RawCurve")) {
                // PCR Raw 曲线：最小循环必须固定从 cycle 1 开始，最大展示到 40 循环（如果数据中存在更大循环例如 45 或 50，则取更大值）
                xAxis.setAxisMinimum(1f);
                float finalMaxX = Math.max(40f, hasData ? absoluteMaxX : 40f);
                xAxis.setAxisMaximum(finalMaxX);
                xAxis.setSpaceMin(0f);
                xAxis.setSpaceMax(0f);
            } else if (describe != null && describe.equals("MeltCurve")) {
                // Melt 曲线：X 轴根据实际数据范围动态设置，Y 轴也根据实际数据优化以提升渲染速度
                if (hasData) {
                    // X 轴：使用实际数据的温度范围，留 2°C 余量
                    float finalMinX = Math.max(20f, absoluteMinX - 2f);  // 最小不低于 20°C
                    float finalMaxX = Math.min(100f, absoluteMaxX + 2f); // 最大不超过 100°C

                    Log.d("MeltCurve", "数据范围 - absoluteMinX: " + absoluteMinX + ", absoluteMaxX: " + absoluteMaxX);
                    Log.d("MeltCurve", "设置X轴 - finalMinX: " + finalMinX + ", finalMaxX: " + finalMaxX);

                    xAxis.setAxisMinimum(finalMinX);
                    xAxis.setAxisMaximum(finalMaxX);

                    // Y 轴：根据实际数据范围设置，留 10% 余量，大幅减少绘制点数提升性能
                    float yRange = absoluteMaxY - absoluteMinY;
                    float yPadding = yRange * 0.1f; // 上下各留 10% 余量

                    Log.d("MeltCurve", "Y轴范围 - absoluteMinY: " + absoluteMinY + ", absoluteMaxY: " + absoluteMaxY);
                    Log.d("MeltCurve", "设置Y轴 - min: " + (absoluteMinY - yPadding) + ", max: " + (absoluteMaxY + yPadding));

                    leftAxis.setAxisMinimum(absoluteMinY - yPadding);
                    leftAxis.setAxisMaximum(absoluteMaxY + yPadding);
                    leftAxis.setSpaceTop(0f);  // 已手动留余量，不再需要 spaceTop
                    leftAxis.setSpaceBottom(0f);
                } else {
                    // 无数据时使用默认范围
                    Log.d("MeltCurve", "无数据，使用默认范围");
                    xAxis.setAxisMinimum(50f);
                    xAxis.setAxisMaximum(95f);
                }
                xAxis.setSpaceMin(0f);
                xAxis.setSpaceMax(0f);
            } else {
                xAxis.setSpaceMin(0.5f);
                xAxis.setSpaceMax(0.5f);
            }

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
            DialogUtil.showMessage(getContext(), "没有结果文件");
        }

        //参数设置

    }

    /**
     * 设置所有的孔位位选定状态
     */
    private void checkWellState(){

        for(BiowayCurveList curveList : showCurveList){

            if(curveList.getWellNum().equalsIgnoreCase("A1")){
                isA1Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("A2")){
                isA2Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("A3")){
                isA3Check = true;
            } else if(curveList.getWellNum().equalsIgnoreCase("A4")){
                isA4Check = true;
            } else if(curveList.getWellNum().equalsIgnoreCase("A5")){
                isA5Check = true;
            } else if(curveList.getWellNum().equalsIgnoreCase("A6")){
                isA6Check = true;
            } else if(curveList.getWellNum().equalsIgnoreCase("A7")){
                isA7Check = true;
            } else if(curveList.getWellNum().equalsIgnoreCase("A8")){
                isA8Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("B1")){
                isB1Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("B2")){
                isB2Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("B3")){
                isB3Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("B4")){
                isB4Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("B5")){
                isB5Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("B6")){
                isB6Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("B7")){
                isB7Check = true;
            }else if(curveList.getWellNum().equalsIgnoreCase("B8")){
                isB8Check = true;
            }
        }

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

    //===================================保存宏石实验结果=================================================//
//xxs20251120
    private SLANHelper slanHelper;
    //宏石保存实验结果需要打开保存的pcr文件获取Experiment对象
    public void openAndSaveSLANExperiment(String pcrFilepath){
        long startTime = System.currentTimeMillis();
        Log.i("slanFinish", "【优化】openAndSaveSLANExperiment 开始，文件: " + pcrFilepath);

        // ⭐ 优化：不再使用 runOnUiThread，全程在后台线程执行
        PCRFinishTaskExecutor.getInstance().submit(() -> {
            try {
                Log.i("slanFinish", "【优化】后台线程开始打开文件");
                com.hongshi.slansdk.model.experiment.Experiment slanExperiment =
                    PcrApplication.slanService.openExperiment(pcrFilepath, true);

                if (null != slanExperiment) {
                    Log.i("slanFinish", "【优化】文件打开成功，开始保存数据库");
                    // ⭐ 关键修改：saveSLANExperiment 也在后台线程执行，不阻塞主线程
                    ResultMessage resultMessage = saveSLANExperiment(slanExperiment,pcrFilepath);

                    long cost = System.currentTimeMillis() - startTime;
                    if (resultMessage.isResult()) {
                        Log.i("slanFinish", "【优化】保存成功，总耗时: " + cost + "ms");
                    } else {
                        Log.e("slanFinish", "【优化】保存失败: " + resultMessage.getMessage() + " 耗时: " + cost + "ms");
                    }
                } else {
                    Log.e("slanFinish", "【优化】文件打开失败");
                    // Toast 需要在主线程显示
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                            DialogUtil.showError(getContext(), "结果文件无法打开")
                        );
                    }
                }
            } catch (Exception e) {
                Log.e("slanFinish", "【优化】openAndSaveSLANExperiment 异常: " + e.getMessage(), e);
            }
        });

        Log.i("slanFinish", "【优化】openAndSaveSLANExperiment 任务已提交到后台队列，不阻塞调用线程");
    }


    /**
     *
     * @param slanExperiment
     * @param pcrFilePath 客户端上传的pcr文件临时保存
     * @return
     */
    private ResultMessage saveSLANExperiment(com.hongshi.slansdk.model.experiment.Experiment slanExperiment,
                                             String pcrFilePath){
        Log.e("slanFinish","saveSLANExperiment--slanExperiment " + slanExperiment.getName());
        ResultMessage resultMessage = new ResultMessage();
        if(slanExperiment != null) {
            if (model.currentTray.get() == null) {
                Log.w("saveSLANExperiment", ".currentTray.set(0)");
                model.currentTray.set(0);
            }
            int trayindex = model.currentTray.get();

            // 从 DispatchModel 获取开始时间（用户点击 save_btn 时记录的时间）
            DispatchModel dispatchModel = TrayManager.getInstance().getCurrentDispatch(trayindex);
            String starttime = "";

            if (dispatchModel != null && dispatchModel.createtime != null && dispatchModel.createtime.get() != null) {
                starttime = dispatchModel.createtime.get();
                model.startTime.set(starttime);
                Log.i("slanFinish", "使用 DispatchModel.createtime 作为开始时间: " + starttime);
            } else {
                // 如果 DispatchModel 中没有，使用当前时间作为备用方案
                String nowtime = DateFormatUtil.getSysFullTime();
                starttime = nowtime;
                model.startTime.set(nowtime);
                Log.w("slanFinish", "DispatchModel 中没有开始时间，使用当前时间: " + nowtime);
            }

            String tempno = "";
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
            String templatePath = dispatchModel.path.get();
            Log.e("slanFinish","batchno--" + batchno);
            Log.e("slanFinish","starttime--" + starttime);
            Log.e("slanFinish","templatePath--" + templatePath);
            Log.e("slanFinish","templatename--" + tempname);
            if(canSave(templatePath,tempname)){
                resultMessage = onSaveSlanResult(slanExperiment,starttime,batchno,templatePath,model.index.get(),pcrFilePath);
            }else{
                resultMessage.setMessage("不能保存实验结果："+ tempname);
                resultMessage.setResult(false);
                Log.e("slanFinish","不能保存实验结果：" + tempname);
            }
            return resultMessage;
        }else{
            resultMessage.setResult(false);
            resultMessage.setMessage("saveExperiment model为空" + DateFormatUtil.getCurrDate());
            Log.i(TAG,"saveExperiment model为空");
            return resultMessage;
        }
    }

    /**
     *
     * @param slanExperiment 保存的实验
     * @param startTime 实验开始时间
     * @param batchNo 保存的文件夹名称
     * @param templatePath 模板路径
     * @param pcrIndex 第几个PCR运行的结果
     * @param pcrFilePath 上传的pcr文件临时保存的路径
     * @return
     */
    private ResultMessage onSaveSlanResult(com.hongshi.slansdk.model.experiment.Experiment slanExperiment,
                                             String startTime,
                                             String batchNo,
                                             String templatePath,
                                             int pcrIndex,
                                             String pcrFilePath){
        ResultMessage resultMessage = new ResultMessage();
        try {
            String subFirName = batchNo;//保存的路径
            Log.i("slanFinish", "ononSaveSlanResult ok batchNo:"+ batchNo );
            saveSLANExperimentToDb(slanExperiment,subFirName,startTime,templatePath,batchNo,pcrIndex);
            saveSLANExperimentToFile(slanExperiment,subFirName,pcrFilePath);
            return resultMessage;
        }catch (Exception e){
            e.getMessage();

            resultMessage.setResult(false);
            Log.e(TAG,"onSaveSLANResult错误==:"+e.getMessage());
            return resultMessage;
        }

    }
    /**
     * 保存宏石数据库,保存savePath
     * @param slanExperiment
     * @param startTime
     * @param templatePath 模板路径
     * @param subFirName 保存pcr文件的父目录
     */
    private void saveSLANExperimentToDb(com.hongshi.slansdk.model.experiment.Experiment slanExperiment,
                                        String subFirName,
                                        String startTime,
                                        String templatePath,
                                        String batchNo,
                                        int pcr) {
        Log.e("slanFinish", "saveSLANExperimentToDb templatePath模板路径===:"+templatePath);
        Log.e("slanFinish", "saveSLANExperimentToDb subFirName:"+subFirName+" pcr:"+pcr);

        try {
            if(slanExperiment == null){
                return;
            }
            if(slanExperiment != null) {
                //需要模板的路径
                if(!StringUtil.isEmpty(templatePath)){
                    ctConclusions.clear();
                    ctConclusions = SLANManager.getInstance().getAllCtResults(slanExperiment, templatePath,PcrApplication.ex_startTime);
                    Log.i("slanFinish","==ctConclusions===" + ctConclusions.size());
                    peakConclusions.clear();
                    peakConclusions = SLANManager.getInstance()
                            .getAllPeakResults(slanExperiment, templatePath,PcrApplication.ex_startTime);
                }

                String endTime = DateFormatUtil.getSysFullTime();
                String savePath = FileUtils.SLAN_RESULT_PATH + File.separator + subFirName + File.separator;
                slanHelper.saveSLANExResultToDB(slanExperiment,savePath, startTime,endTime, batchNo,description, ctConclusions,peakConclusions);
            }

        } catch (Exception e) {
            e.getMessage();
            Log.e("ArmTaskWorker","onSLANExperimentFinish:"+e.getMessage());
        }
    }

    /**
     * 保存宏石Experiment到结果文件pcr
     * @param slanExperiment
     *
     */
    private void saveSLANExperimentToFile(com.hongshi.slansdk.model.experiment.Experiment slanExperiment,
                                          String subFirName,
                                          String pcrFilePath){
        Log.e("slanFinish","saveSLANExperimentToFile ==saveBatchNo=="+ subFirName);
        Log.e("slanFinish","保存实验结果为PCR文件");
        slanHelper.saveResultToPcrFile(slanExperiment,subFirName,pcrFilePath);
    }

    /**
     * 判断是否允许保存
     *
     * @param folderPath 文件夹路径，如 /storage/emulated/0/PCR/Template/singlePCR/06-达安新冠快速
     * @param tempname   传入的模板名
     * @return true 允许保存，false 不允许
     */
    public static boolean canSave(String folderPath, String tempname) {
        if (folderPath == null || tempname == null || tempname.isEmpty()) {
            return false;
        }

        File dir = new File(folderPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }

        File firstPrj = new File(dir, "first.prj");
        File secondPrj = new File(dir, "second.prj");

        boolean hasFirst = firstPrj.exists();
        boolean hasSecond = secondPrj.exists();

        if (hasFirst && hasSecond) {
            // 两个都存在：只有 second.prj 才能保存
            return "second.prj".equals(tempname);
        } else if (hasFirst) {
            // 只有 first.prj：只有 first.prj 才能保存
            return "first.prj".equals(tempname);
        }

        // 其他情况（都不存在，或只有 second.prj）不允许保存
        return false;
    }

}