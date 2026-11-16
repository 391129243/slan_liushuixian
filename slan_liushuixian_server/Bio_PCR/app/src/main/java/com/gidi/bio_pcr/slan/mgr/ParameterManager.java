package com.gidi.bio_pcr.slan.mgr;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.databinding.FragmentAioExDetailBinding;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.NumKeyboardDialog;
import com.hongshi.slansdk.model.experiment.Experiment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 参数管理器
 * 职责：管理扩增参数、熔解参数
 */
public class ParameterManager {

    private static final String TAG = "ParameterManager";

    private final Context context;
    private final FragmentAioExDetailBinding binding;
    private final PopupWindowManager popupManager;

    private AmpAlgorithmAdapter ampAlgorithmAdapter;
    private MeltParamterAdapter meltParamterAdapter;
    private List<ConfigTarget> configTargets = new ArrayList<>();

    private int ampAlgorithm = 0;
    private boolean isMelt = false;

    // 当前选中的参数位置
    private int currentPosition = -1;

    // 数字键盘对话框
    private NumKeyboardDialog numKeyboardDialog;

    // 数字格式化
    private final DecimalFormat nf = new DecimalFormat("0.00");
    private final DecimalFormat nf1 = new DecimalFormat("0.000");

    public ParameterManager(Context context, FragmentAioExDetailBinding binding,
                            PopupWindowManager popupManager) {
        this.context = context;
        this.binding = binding;
        this.popupManager = popupManager;
        initViews();
        initAdapters();
    }

    private void initViews() {
        // 参数切换
        binding.basicParamTitleTv.setOnClickListener(v -> switchToAmpParams());
        binding.meltParamTitleTv.setOnClickListener(v -> switchToMeltParams());
    }

    private void initAdapters() {
        // 扩增曲线算法适配器
        ampAlgorithmAdapter = new AmpAlgorithmAdapter(context);
        ampAlgorithmAdapter.setOnItemClickListener(new AmpAlgorithmAdapter.OnItemClickListener() {
            @Override
            public void onQualitativeAnalysis(View view, int position) {
                currentPosition = position;
                showQualitativeAnalysisPopup(view);
            }

            @Override
            public void onBeginBase(View view, int position) {
                currentPosition = position;
                showNumberKeyboard(DialogType.BEGIN_BASELINE, view);
            }

            @Override
            public void onEndbaseLine(View view, int position) {
                currentPosition = position;
                showNumberKeyboard(DialogType.END_BASELINE, view);
            }

            @Override
            public void onOptimizationMode(View view, int position) {
                currentPosition = position;
                showOptimizationModePopup(view);
            }

            @Override
            public void onChangeThreshold(View view, int position) {
                currentPosition = position;
                showNumberKeyboard(DialogType.THRESHOLD, view);
            }

            @Override
            public void onDigitalFilter(View view, int position) {
                currentPosition = position;
                showDigitalFilterPopup(view);
            }

            @Override
            public void onAutoThreshold(int position) {
                currentPosition = position;
                toggleAutoThreshold();
            }
        });
        binding.ampAlgorithmLv.setAdapter(ampAlgorithmAdapter);

        // 熔解曲线参数适配器
        meltParamterAdapter = new MeltParamterAdapter(context);
        meltParamterAdapter.setOnItemClickListener(new MeltParamterAdapter.OnItemClickListener() {
            @Override
            public void onChangeMinTemperature(View view, int position) {
                currentPosition = position;
                showNumberKeyboard(DialogType.MIN_TEMPERATURE, view);
            }

            @Override
            public void onChangeMaxTemperature(View view, int position) {
                currentPosition = position;
                showNumberKeyboard(DialogType.MAX_TEMPERATURE, view);
            }

            @Override
            public void onMeltPeakAlgorithmTv(View view, int position) {
                currentPosition = position;
                showMeltPeakAlgorithmPopup(view);
            }

            @Override
            public void onChangePeekHighthreshold(View view, int position) {
                currentPosition = position;
                showNumberKeyboard(DialogType.PEEK_THRESHOLD, view);
            }

            @Override
            public void onPeekType(View view, int position) {
                currentPosition = position;
                showPeekTypePopup(view);
            }
        });
        binding.meltParamterLv.setAdapter(meltParamterAdapter);
    }

    // ============ 数字键盘相关方法 ============

    /**
     * 对话框类型常量
     */
    public static class DialogType {
        public static final int BEGIN_BASELINE = 3;    // 基线起点
        public static final int END_BASELINE = 2;       // 基线终点
        public static final int THRESHOLD = 4;          // 阈值
        public static final int MIN_TEMPERATURE = 5;    // 最低温度
        public static final int MAX_TEMPERATURE = 6;    // 最高温度
        public static final int PEEK_THRESHOLD = 7;     // 峰高阈值
    }

    /**
     * 显示数字键盘对话框
     *
     * @param dialogType 对话框类型
     * @param anchor 锚点View（用于获取位置，暂未使用）
     */
    private void showNumberKeyboard(int dialogType, View anchor) {
        // 获取当前配置
        ConfigTarget config = getCurrentConfig();
        if (config == null) {
            Toast.makeText(context, "请先选择参数", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前值作为默认值
        String defaultValue = getDefaultValue(dialogType, config);

        // 关闭旧的对话框
        dismissNumKeyboard();

        // 创建新的对话框
        numKeyboardDialog = new NumKeyboardDialog(context);
        numKeyboardDialog.setEditText(defaultValue);

        // 取消按钮
        numKeyboardDialog.setOnCanlceListener(v -> dismissNumKeyboard());

        // 确认按钮
        numKeyboardDialog.setOnKeyBoardDoneListener(s -> {
            if (handleNumberInput(dialogType, s)) {
                dismissNumKeyboard();
            }
        });

        numKeyboardDialog.show();
    }

    /**
     * 获取当前参数的默认值
     */
    private String getDefaultValue(int dialogType, ConfigTarget config) {
        switch (dialogType) {
            case DialogType.BEGIN_BASELINE:
                return String.valueOf(config.getBeginBaseline());
            case DialogType.END_BASELINE:
                return String.valueOf(config.getEndBaseline());
            case DialogType.THRESHOLD:
                return nf.format(config.getThreshold());
            case DialogType.MIN_TEMPERATURE:
                return nf1.format(config.getMinTemperature());
            case DialogType.MAX_TEMPERATURE:
                return nf1.format(config.getMaxTemperature());
            case DialogType.PEEK_THRESHOLD:
                return nf1.format(config.getPeekHighThreshold());
            default:
                return "0";
        }
    }

    /**
     * 处理数字输入
     *
     * @param dialogType 对话框类型
     * @param input 用户输入的值
     * @return 是否处理成功
     */
    private boolean handleNumberInput(int dialogType, String input) {
        if (input == null || input.isEmpty()) {
            Toast.makeText(context, "请输入有效数值", Toast.LENGTH_SHORT).show();
            return false;
        }

        ConfigTarget config = getCurrentConfig();
        if (config == null) {
            Toast.makeText(context, "参数异常，请重试", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            switch (dialogType) {
                case DialogType.BEGIN_BASELINE:
                    return updateBeginBaseline(config, input);
                case DialogType.END_BASELINE:
                    return updateEndBaseline(config, input);
                case DialogType.THRESHOLD:
                    return updateThreshold(config, input);
                case DialogType.MIN_TEMPERATURE:
                    return updateMinTemperature(config, input);
                case DialogType.MAX_TEMPERATURE:
                    return updateMaxTemperature(config, input);
                case DialogType.PEEK_THRESHOLD:
                    return updatePeekHighThreshold(config, input);
                default:
                    Toast.makeText(context, "未知参数类型", Toast.LENGTH_SHORT).show();
                    return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(context, "请输入有效数字", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * 更新基线起点
     */
    private boolean updateBeginBaseline(ConfigTarget config, String value) {
        int beginbase = (int) StringUtil.convertToFloat(value, 1.0f);
        if (beginbase < 1) {
            Toast.makeText(context, "基线起点不能小于1", Toast.LENGTH_LONG).show();
            return false;
        }
        // 验证不能大于等于终点
        if (beginbase >= config.getEndBaseline()) {
            Toast.makeText(context, "基线起点不能大于等于终点", Toast.LENGTH_LONG).show();
            return false;
        }
        config.setBeginBaseline(beginbase);
        updateAmpAdapter();
        Toast.makeText(context, "基线起点已更新为 " + beginbase, Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * 更新基线终点
     */
    private boolean updateEndBaseline(ConfigTarget config, String value) {
        int endbase = (int) StringUtil.convertToFloat(value, 1.0f);
        if (endbase < 1) {
            Toast.makeText(context, "基线终点不能小于1", Toast.LENGTH_LONG).show();
            return false;
        }
        // 验证不能小于等于起点
        if (endbase <= config.getBeginBaseline()) {
            Toast.makeText(context, "基线终点不能小于等于起点", Toast.LENGTH_LONG).show();
            return false;
        }
        config.setEndBaseline(endbase);
        updateAmpAdapter();
        Toast.makeText(context, "基线终点已更新为 " + endbase, Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * 更新阈值
     */
    private boolean updateThreshold(ConfigTarget config, String value) {
        double threshold = StringUtil.convertToFloat(value, 0.00f);
        if (threshold < 0) {
            Toast.makeText(context, "阈值不能为负数", Toast.LENGTH_LONG).show();
            return false;
        }
        config.setThreshold(threshold);
        updateAmpAdapter();
        Toast.makeText(context, "阈值已更新为 " + nf.format(threshold), Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * 更新最低温度
     */
    private boolean updateMinTemperature(ConfigTarget config, String value) {
        double minTemp = StringUtil.convertToFloat(value, 0.00f);
        if (minTemp < 0) {
            Toast.makeText(context, "温度不能为负数", Toast.LENGTH_LONG).show();
            return false;
        }
        // 验证不能大于等于最高温度
        if (minTemp >= config.getMaxTemperature()) {
            Toast.makeText(context, "最低温度不能大于等于最高温度", Toast.LENGTH_LONG).show();
            return false;
        }
        config.setMinTemperature(minTemp);
        updateMeltAdapter();
        Toast.makeText(context, "最低温度已更新为 " + nf1.format(minTemp), Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * 更新最高温度
     */
    private boolean updateMaxTemperature(ConfigTarget config, String value) {
        double maxTemp = StringUtil.convertToFloat(value, 0.00f);
        if (maxTemp < 0) {
            Toast.makeText(context, "温度不能为负数", Toast.LENGTH_LONG).show();
            return false;
        }
        // 验证不能小于等于最低温度
        if (maxTemp <= config.getMinTemperature()) {
            Toast.makeText(context, "最高温度不能小于等于最低温度", Toast.LENGTH_LONG).show();
            return false;
        }
        config.setMaxTemperature(maxTemp);
        updateMeltAdapter();
        Toast.makeText(context, "最高温度已更新为 " + nf1.format(maxTemp), Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * 更新峰高阈值
     */
    private boolean updatePeekHighThreshold(ConfigTarget config, String value) {
        double peekHighThreshold = StringUtil.convertToFloat(value, 0.00f);
        if (peekHighThreshold < 0) {
            Toast.makeText(context, "峰高阈值不能为负数", Toast.LENGTH_LONG).show();
            return false;
        }
        config.setPeekHighThreshold(peekHighThreshold);
        updateMeltAdapter();
        Toast.makeText(context, "峰高阈值已更新为 " + nf1.format(peekHighThreshold), Toast.LENGTH_SHORT).show();
        return true;
    }

    /**
     * 关闭数字键盘
     */
    private void dismissNumKeyboard() {
        if (numKeyboardDialog != null) {
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
    }

    // ============ 弹窗显示方法 ============

    /**
     * 显示定性/定量分析弹窗
     */
    private void showQualitativeAnalysisPopup(View anchor) {
        List<String> items = new ArrayList<>();
        items.add("定性");
        items.add("定量");

        popupManager.showListPopup(anchor, items, new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget config = getCurrentConfig();
                if (config != null) {
                    config.setQualitativeAnalysis(position == 0);
                    updateAmpAdapter();
                }
                popupManager.dismiss();
            }

            @Override
            public void onItemLongClick(int position) {}
        });
    }

    /**
     * 显示基线优化模式弹窗
     */
    private void showOptimizationModePopup(View anchor) {
        List<String> items = new ArrayList<>();
        items.add("无");
        items.add("自动");
        items.add("手动");

        popupManager.showListPopup(anchor, items, new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget config = getCurrentConfig();
                if (config != null) {
                    config.setOptimizationMode(position);
                    updateAmpAdapter();
                }
                popupManager.dismiss();
            }

            @Override
            public void onItemLongClick(int position) {}
        });
    }

    /**
     * 显示数字滤波弹窗
     */
    private void showDigitalFilterPopup(View anchor) {
        List<String> items = new ArrayList<>();
        items.add("无滤波");
        items.add("低通滤波");
        items.add("高通滤波");
        items.add("带通滤波");

        popupManager.showListPopup(anchor, items, new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget config = getCurrentConfig();
                if (config != null) {
                    config.setDigitalFilter(position);
                    updateAmpAdapter();
                }
                popupManager.dismiss();
            }

            @Override
            public void onItemLongClick(int position) {}
        });
    }

    /**
     * 显示熔解峰算法弹窗
     */
    private void showMeltPeakAlgorithmPopup(View anchor) {
        List<String> items = new ArrayList<>();
        items.add("标准");
        items.add("高级");

        popupManager.showListPopup(anchor, items, new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget config = getCurrentConfig();
                if (config != null) {
                    config.setMeltPeakAlgorithm(position);
                    updateMeltAdapter();
                }
                popupManager.dismiss();
            }

            @Override
            public void onItemLongClick(int position) {}
        });
    }

    /**
     * 显示峰类型弹窗
     */
    private void showPeekTypePopup(View anchor) {
        List<String> items = new ArrayList<>();
        items.add("正峰");
        items.add("负峰");
        items.add("正负峰");

        popupManager.showListPopup(anchor, items, new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget config = getCurrentConfig();
                if (config != null) {
                    config.setPeekType(position);
                    updateMeltAdapter();
                }
                popupManager.dismiss();
            }

            @Override
            public void onItemLongClick(int position) {}
        });
    }

    /**
     * 显示扩增算法选择弹窗
     */
    public void showAmpAlgorithmPopup(View anchor) {
        String[] algorithms = context.getResources().getStringArray(R.array.AMPAlgorithm);
        List<String> items = new ArrayList<>();
        for (String alg : algorithms) {
            items.add(alg);
        }

        popupManager.showListPopup(anchor, items, new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ampAlgorithm = position;
                binding.ampAlgorithmTv.setText(items.get(position));
                popupManager.dismiss();
            }

            @Override
            public void onItemLongClick(int position) {}
        });
    }

    // ============ 辅助方法 ============

    private void toggleAutoThreshold() {
        ConfigTarget config = getCurrentConfig();
        if (config != null) {
            config.setAutoThreshold(!config.isAutoThreshold());
            updateAmpAdapter();
            String status = config.isAutoThreshold() ? "开启" : "关闭";
            Toast.makeText(context, "自动阈值已" + status, Toast.LENGTH_SHORT).show();
        }
    }

    private ConfigTarget getCurrentConfig() {
        if (currentPosition >= 0 && currentPosition < configTargets.size()) {
            return configTargets.get(currentPosition);
        }
        return null;
    }

    //更新扩增算法参数列表
    private void updateAmpAdapter() {
        if (ampAlgorithmAdapter != null) {
            ampAlgorithmAdapter.setAmpAlgorithm(configTargets);
        }
    }

    private void updateMeltAdapter() {
        if (meltParamterAdapter != null) {
            meltParamterAdapter.setMeltParamter(configTargets);
        }
    }

    // ============ 参数切换 ============

    private void switchToAmpParams() {
        binding.basicParamTitleTv.setTextColor(context.getResources().getColor(R.color.white));
        binding.basicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
        binding.meltParamTitleTv.setTextColor(context.getResources().getColor(R.color.black));
        binding.meltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
        binding.ampParamLayout.setVisibility(View.VISIBLE);
        binding.meltParamLayout.setVisibility(View.GONE);

        if (ampAlgorithmAdapter != null) {
            ampAlgorithmAdapter.setAmpAlgorithm(configTargets);
        }
    }

    private void switchToMeltParams() {
        binding.meltParamTitleTv.setTextColor(context.getResources().getColor(R.color.white));
        binding.meltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
        binding.basicParamTitleTv.setTextColor(context.getResources().getColor(R.color.black));
        binding.basicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
        binding.ampParamLayout.setVisibility(View.GONE);
        binding.meltParamLayout.setVisibility(View.VISIBLE);

        if (meltParamterAdapter != null) {
            meltParamterAdapter.setMeltParamter(configTargets);
        }
    }

    // ============ 从Experiment加载参数 ============

    public void loadParameters(Experiment experiment) {
        if (experiment == null) return;
        configTargets.clear();
        // TODO: 实际解析逻辑
        updateUI();
    }

    private void updateUI() {
        String[] algorithms = context.getResources().getStringArray(R.array.AMPAlgorithm);
        if (ampAlgorithm >= 0 && ampAlgorithm < algorithms.length) {
            binding.ampAlgorithmTv.setText(algorithms[ampAlgorithm]);
        }

        if (ampAlgorithmAdapter != null) {
            ampAlgorithmAdapter.setAmpAlgorithm(configTargets);
        }
        if (meltParamterAdapter != null) {
            meltParamterAdapter.setMeltParamter(configTargets);
        }

        binding.meltParamTitleTv.setVisibility(isMelt ? View.VISIBLE : View.GONE);
    }

    // ============ 设置配置数据 ============

    /**
     * 设置配置目标列表（由外部传入）
     */
    public void setConfigTargets(List<ConfigTarget> targets) {
        if (targets != null) {
            this.configTargets.clear();
            this.configTargets.addAll(targets);
            updateUI();
        }
    }

    /**
     * 设置扩增算法
     */
    public void setAmpAlgorithm(int algorithm) {
        this.ampAlgorithm = algorithm;
        updateUI();
    }

    // ============ Getter/Setter ============

    public List<ConfigTarget> getConfigTargets() {
        return configTargets;
    }

    public int getAmpAlgorithm() {
        return ampAlgorithm;
    }

    public boolean isMelt() {
        return isMelt;
    }

    public AmpAlgorithmAdapter getAmpAlgorithmAdapter() {
        return ampAlgorithmAdapter;
    }

    public MeltParamterAdapter getMeltParamterAdapter() {
        return meltParamterAdapter;
    }

    public void release() {
        dismissNumKeyboard();
        if (ampAlgorithmAdapter != null) {
            ampAlgorithmAdapter = null;
        }
        if (meltParamterAdapter != null) {
            meltParamterAdapter = null;
        }
        configTargets.clear();
    }
}