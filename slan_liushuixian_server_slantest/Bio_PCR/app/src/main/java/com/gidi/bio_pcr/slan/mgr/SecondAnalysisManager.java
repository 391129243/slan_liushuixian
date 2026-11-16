package com.gidi.bio_pcr.slan.mgr;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.databinding.FragmentAioExDetailBinding;
import com.gidi.bio_pcr.pcr.OnDetailExCallback;
import com.gidi.bio_pcr.slan.OnParameterUpdateListener;
import com.gidi.bio_pcr.slan.SLANManager;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.view.CustomDialog;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;
import com.hongshi.slansdk.model.experiment.Experiment;
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.hongshi.slansdk.service.SLANService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 二次分析管理器
 * 职责：管理二次分析流程
 */
public class SecondAnalysisManager {

    private static final String TAG = "SecondAnalysisManager";

    private final Context context;
    private final FragmentAioExDetailBinding binding;
    private final OnDetailExCallback callback;
    private OnParameterUpdateListener parameterUpdateListener;

    // 主线程Handler
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 参数
    private List<ConfigTarget> configTargets = new ArrayList<>();
    private int ampAlgorithm = 0;
    private boolean isMelt = false;
    private ExperimentModel experimentModel;

    // 二次分析用的Experiment
    private Experiment secondExperiment;
    private String startTime;
    private String testItem;

    private String pcrSaveFolderPath;
    private String curTemplatePath;

    // 加载对话框
    private LoadingDialog loadingDialog;
    // 更新对话框
    private CustomDialog updateDialog;

    public SecondAnalysisManager(Context context, FragmentAioExDetailBinding binding,
                                 OnDetailExCallback callback) {
        this.context = context;
        this.binding = binding;
        this.callback = callback;
    }

    /**
     * 设置参数更新监听器
     */
    public void setOnParameterUpdateListener(OnParameterUpdateListener listener) {
        this.parameterUpdateListener = listener;
    }

    // ==================== UI线程辅助方法 ====================

    /**
     * 在主线程执行Runnable
     */
    private void runOnUiThread(Runnable action) {
        if (action != null) {
            mainHandler.post(action);
        }
    }

    /**
     * 在主线程延迟执行Runnable
     */
    private void runOnUiThreadDelayed(Runnable action, long delayMillis) {
        if (action != null) {
            mainHandler.postDelayed(action, delayMillis);
        }
    }

    // ==================== 配置方法 ====================

    public void setConfigTargets(List<ConfigTarget> targets) {
        if (targets != null) {
            this.configTargets.clear();
            this.configTargets.addAll(targets);
        }
    }

    public void setAmpAlgorithm(int algorithm) {
        this.ampAlgorithm = algorithm;
    }

    public void setExperimentModel(ExperimentModel model) {
        this.experimentModel = model;
        if (model != null) {
            this.startTime = model.getStartTime();
            this.testItem = model.getTestItem();
            this.pcrSaveFolderPath = model.getFilePath();
            initTemplatePath(model);
        }
    }

    private void initTemplatePath(ExperimentModel model) {
        int type = model.getType();
        String templateName = model.getTestItem();
        switch (type) {
            case 0:
                curTemplatePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + templateName + File.separator;
                break;
            case 1:
                curTemplatePath = FileUtils.SINGLE_PCR_TEMPLATE_PATH + File.separator + templateName + File.separator;
                break;
            default:
                curTemplatePath = FileUtils.SINGLE_PCR_TEMPLATE_PATH + File.separator + templateName + File.separator;
                break;
        }
        Log.i(TAG, "Template path: " + curTemplatePath);
    }

    // ==================== 主要业务方法 ====================

    public void loadSecondAnalysisData() {
        if (experimentModel == null) {
            runOnUiThread(() ->
                    Toast.makeText(context, "请先加载实验数据", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        String filePath = getSecondAnalysisFilePath();
        if (TextUtils.isEmpty(filePath)) {
            runOnUiThread(() ->
                    Toast.makeText(context, context.getResources().getString(R.string.result_ex_detail_no_second_file),
                            Toast.LENGTH_SHORT).show()
            );
            return;
        }

        showLoading(context.getResources().getString(R.string.dialog_message_query_data));

        new Thread(() -> {
            try {
                Experiment experiment = PcrApplication.slanService.openExperiment(filePath, true);
                runOnUiThread(() -> {
                    hideLoading();
                    if (experiment != null) {
                        secondExperiment = experiment;
                        getExperimentParameters(experiment);
                        Toast.makeText(context, "二次分析数据加载成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context,
                                context.getResources().getString(R.string.result_ex_detail_toast_cannot_second_analyse),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(context, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    public void startSecondAnalysis(ExperimentModel model) {
        if (model == null) {
            runOnUiThread(() ->
                    Toast.makeText(context, "无实验数据", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        this.experimentModel = model;
        this.startTime = model.getStartTime();
        this.testItem = model.getTestItem();

        initTemplatePath(model);

        if (configTargets.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(context, "请先配置分析参数", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        String pcrFilePath = getPcrFilePath(model.getFilePath());
        if (TextUtils.isEmpty(pcrFilePath)) {
            runOnUiThread(() ->
                    Toast.makeText(context, "PCR文件不存在", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        showLoading(context.getResources().getString(R.string.dialog_message_second_analyse));

        new Thread(() -> {
            try {
                Experiment experiment = PcrApplication.slanService.openExperiment(pcrFilePath, true);
                if (experiment == null) {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(context, "打开PCR文件失败", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                secondExperiment = experiment;

                List<File> templateFiles = getTemplateFiles(curTemplatePath);
                File ctFile = findFile(templateFiles, ".ct");
                File peakFile = findFile(templateFiles, ".peak");

                Log.i(TAG, "CT file: " + (ctFile != null ? ctFile.getAbsolutePath() : "null"));
                Log.i(TAG, "Peak file: " + (peakFile != null ? peakFile.getAbsolutePath() : "null"));

                SLANManager.getInstance().secondSLANExAnalyse(
                        configTargets,
                        ampAlgorithm,
                        ctFile,
                        peakFile,
                        experiment,
                        startTime
                );

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(context, "二次分析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ==================== 获取Experiment参数 ====================

    private void getExperimentParameters(Experiment experiment) {
        if (experiment == null || PcrApplication.slanService == null) {
            Log.e(TAG, "Experiment or SLANService is null");
            return;
        }

        try {
            configTargets.clear();

            ArrayList<AnalyseParamter> analyseParamters = PcrApplication.slanService
                    .getExperimentParamter(experiment.getName());

            if (analyseParamters == null || analyseParamters.isEmpty()) {
                Log.w(TAG, "No analyse paramters found");
                return;
            }

            AnalyseParamter analyseParamter = analyseParamters.get(0);
            ampAlgorithm = analyseParamter.getAMPAlgorithm();

            ArrayList<Target> targets = analyseParamter.getTargets();
            if (targets == null || targets.isEmpty()) {
                Log.w(TAG, "No targets found");
                return;
            }

            for (Target target : targets) {
                ConfigTarget configTarget = parseTargetToConfigTarget(target);
                if (configTarget != null) {
                    configTargets.add(configTarget);
                }
            }

            runOnUiThread(() -> {
                updateParameterUI();
                // 通知外部参数已更新
                notifyParameterUpdated();
            });

            Log.i(TAG, "Loaded " + configTargets.size() + " channel parameters");

        } catch (Exception e) {
            Log.e(TAG, "Error getting experiment parameters: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private ConfigTarget parseTargetToConfigTarget(Target target) {
        if (target == null) {
            return null;
        }

        int channelNo = target.getChannelNo();
        TargetParamter paramter = target.getParamter();

        if (paramter == null) {
            Log.w(TAG, "Paramter is null for channel " + channelNo);
            return null;
        }

        ConfigTarget config = new ConfigTarget();
        config.setChannelNo(channelNo);
        config.setName(getChannelName(channelNo));
        config.setColor(getChannelColor(channelNo));
        config.setTargetIndex(channelNo);

        // 扩增参数
        config.setQualitativeAnalysis(paramter.getQualitativeAnalysis());

        int beginBaseline = paramter.getBeginBaseline();
        if (beginBaseline == 0) {
            beginBaseline = 1;
        }
        config.setBeginBaseline(beginBaseline);
        config.setEndBaseline(paramter.getEndBaseline());
        config.setDigitalFilter(paramter.getDigitalFilter());
        config.setThreshold(paramter.getThreshold());
        config.setAutoThreshold(paramter.getAutoThreshold());
        config.setOptimizationMode(paramter.getOptimizationMode());
        config.setDetectMin(paramter.getDetectionMin());
        config.setDetectMax(paramter.getDetectionMax());

        // 熔解参数
        if (paramter instanceof TargetParamterMelt) {
            isMelt = true;
            TargetParamterMelt meltParam = (TargetParamterMelt) paramter;
            config.setMinTemperature(meltParam.getMinTemperature());
            config.setMaxTemperature(meltParam.getMaxTemperature());
            config.setMeltPeakAlgorithm(meltParam.getMeltPeakAlgorithm());
            config.setPeekHighThreshold(meltParam.getPeekHightThreshold());
            config.setPeekType(meltParam.getPeekType());
        }

        return config;
    }

    /**
     * 通知参数已更新
     */
    private void notifyParameterUpdated() {
        if (parameterUpdateListener != null) {
            // 通知扩增参数更新
            parameterUpdateListener.onAmpParametersUpdated(configTargets, ampAlgorithm);

            // 如果是熔解实验，通知熔解参数更新
            if (isMelt) {
                parameterUpdateListener.onMeltParametersUpdated(configTargets);
            }
        }
    }
    //=========================UI更新=====================================
    private void updateParameterUI() {
        //1.更新扩增算法
        String[] algorithms = context.getResources().getStringArray(R.array.AMPAlgorithm);
        if (ampAlgorithm >= 0 && ampAlgorithm < algorithms.length) {
            binding.ampAlgorithmTv.setText(algorithms[ampAlgorithm]);
        }
        //2显示/隐藏熔解参数标签
        binding.meltParamTitleTv.setVisibility(isMelt ? View.VISIBLE : View.GONE);
        binding.meltParamLayout.setVisibility(isMelt ? View.VISIBLE : View.GONE);
        // 5. 切换参数Tab
        if (isMelt) {
            switchToMeltParams();
        } else {
            switchToAmpParams();
        }
    }

    private void switchToAmpParams() {
        binding.basicParamTitleTv.setTextColor(context.getResources().getColor(R.color.white));
        binding.basicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
        binding.meltParamTitleTv.setTextColor(context.getResources().getColor(R.color.black));
        binding.meltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
        binding.ampParamLayout.setVisibility(View.VISIBLE);
        binding.meltParamLayout.setVisibility(View.GONE);
    }

    private void switchToMeltParams() {
        binding.meltParamTitleTv.setTextColor(context.getResources().getColor(R.color.white));
        binding.meltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
        binding.basicParamTitleTv.setTextColor(context.getResources().getColor(R.color.black));
        binding.basicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
        binding.ampParamLayout.setVisibility(View.GONE);
        binding.meltParamLayout.setVisibility(View.VISIBLE);
    }

    private String getChannelName(int channelNo) {
        switch (channelNo) {
            case 1: return "FAM";
            case 2: return "HEX";
            case 3: return "ROX";
            case 4: return "CY5";
            case 5: return "QUASAR 705";
            case 6: return "ATTO 425";
            default: return "Channel " + channelNo;
        }
    }

    private String getChannelColor(int channelNo) {
        switch (channelNo) {
            case 1: return "#FF0000FF";
            case 2: return "#FF00FF00";
            case 3: return "#FFFF8000";
            case 4: return "#FFFF0000";
            case 5: return "#FF8000FF";
            case 6: return "#FF800000";
            default: return "#FF000000";
        }
    }

    // ==================== 文件操作 ====================

    private String getSecondAnalysisFilePath() {
        if (experimentModel == null) {
            return null;
        }
        return getPcrFilePath(experimentModel.getFilePath());
    }

    private String getPcrFilePath(String folderPath) {
        if (TextUtils.isEmpty(folderPath)) {
            return null;
        }
        File dir = new File(folderPath);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".pcr")) {
                        return file.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }

    private List<File> getTemplateFiles(String templatePath) {
        List<File> files = new ArrayList<>();
        if (TextUtils.isEmpty(templatePath)) {
            return files;
        }
        File dir = new File(templatePath);
        if (dir.exists() && dir.isDirectory()) {
            File[] fileArray = dir.listFiles();
            if (fileArray != null) {
                Collections.addAll(files, fileArray);
            }
        }
        return files;
    }

    private File findFile(List<File> files, String extension) {
        for (File file : files) {
            if (file.getName().endsWith(extension)) {
                return file;
            }
        }
        return null;
    }

    // ==================== 更新对话框 ====================

    public void showUpdateDialog() {
        if (updateDialog != null) {
            updateDialog.dismiss();
            updateDialog = null;
        }

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_interrupt_detect_layout, null);
        TextView hintTv = view.findViewById(R.id.dialog_interrupt_hint_tv);
        hintTv.setText(R.string.dialog_message_update_ex);

        CustomDialog.Builder builder = new CustomDialog.Builder(context);
        builder.setContentView(view);
        builder.setTitle(R.string.dialog_title_prompt);
        builder.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            updateExperiment();
            dismissUpdateDialog();
        });
        builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
            dismissUpdateDialog();
        });

        updateDialog = builder.create();
        updateDialog.setCancelable(false);
        updateDialog.show();
    }

    /**
     * 更新实验数据到数据库
     */
    private void updateExperiment() {
        if (TextUtils.isEmpty(pcrSaveFolderPath)) {
            runOnUiThread(() ->
                    Toast.makeText(context, "实验信息不完整", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        showLoading("正在更新数据...");

        new Thread(() -> {
            try {
                SLANManager.getInstance().updateDetailExperiment(testItem, startTime);

                if (secondExperiment != null) {
                    saveResultToPCR(secondExperiment);
                }

                runOnUiThread(() -> {
                    hideLoading();
                    if (callback != null) {
                        callback.onUpdateFinish(true);
                    }
                    Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    hideLoading();
                    if (callback != null) {
                        callback.onUpdateFinish(false);
                    }
                    Toast.makeText(context, "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveResultToPCR(Experiment experiment) {
        try {

            SLANManager.getInstance().saveDetailExToJson(experiment, pcrSaveFolderPath);
            SLANManager.getInstance().saveResultToPCR(experiment, pcrSaveFolderPath);

            Log.i(TAG, "Saved result to: " + pcrSaveFolderPath);

        } catch (Exception e) {
            Log.e(TAG, "Error saving result: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void dismissUpdateDialog() {
        if (updateDialog != null) {
            updateDialog.dismiss();
            updateDialog = null;
        }
    }

    // ==================== 加载对话框 ====================

    private void showLoading(String message) {
        runOnUiThread(() -> {
            if (loadingDialog == null) {
                loadingDialog = new LoadingDialog(context, message);
                loadingDialog.setCancelable(false);
                loadingDialog.show();
            } else {
               // loadingDialog.setMessage(message);
                if (!loadingDialog.isShowing()) {
                    loadingDialog.show();
                }
            }
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
                loadingDialog = null;
            }
        });
    }

    // ==================== 资源释放 ====================

    public void release() {
        hideLoading();
        dismissUpdateDialog();
        if (configTargets != null) {
            configTargets.clear();
        }
        secondExperiment = null;
        experimentModel = null;
        // 移除所有Handler回调
        mainHandler.removeCallbacksAndMessages(null);
        Log.i(TAG, "Released resources");
    }

    // ==================== Getter ====================

    public Experiment getSecondExperiment() {
        return secondExperiment;
    }

    public List<ConfigTarget> getConfigTargets() {
        return configTargets;
    }

    public int getAmpAlgorithm() {
        return ampAlgorithm;
    }

    public boolean isMelt() {
        return isMelt;
    }

    public boolean isConfigReady() {
        return !configTargets.isEmpty() && experimentModel != null;
    }
}