package com.gidi.bio_pcr.slan.mgr;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.databinding.FragmentAioExDetailBinding;
import com.gidi.bio_pcr.slan.bean.SLANCurveList;
import com.gidi.bio_pcr.view.LineChartMarkView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图表管理器
 * 职责：统一管理LineChart的绘制、缩放、还原、阈值线等
 */
public class ChartManager {

    private static final String TAG = "ChartManager";

    private final Context context;
    private final LineChart chart;
    private final FragmentAioExDetailBinding binding;

    // 当前曲线类型
    private CurveType currentType = CurveType.RAW;

    // 图表数据缓存
    private List<SLANCurveList> currentData = new ArrayList<>();

    // 阈值线管理
    private final Map<Integer, LimitLine> thresholdLines = new HashMap<>();

    // 缩放状态管理
    private final ZoomStateManager zoomStateManager = new ZoomStateManager();

    // Marker
    private LineChartMarkView markView;

    // 颜色数组（从PcrValue获取）
    private final int[] colorLine = PcrValue.colorline;

    // 通道颜色映射
    private static final Map<Integer, Integer> CHANNEL_COLORS = new HashMap<>();

    static {
        CHANNEL_COLORS.put(1, PcrValue.colorline[0]);  // FAM - 蓝色
        CHANNEL_COLORS.put(2, PcrValue.colorline[1]);  // HEX - 绿色
        CHANNEL_COLORS.put(3, PcrValue.colorline[2]);  // ROX - 橙色
        CHANNEL_COLORS.put(4, PcrValue.colorline[3]);  // CY5 - 红色
        CHANNEL_COLORS.put(5, PcrValue.colorline[4]);  // QUASAR - 紫色
        CHANNEL_COLORS.put(6, PcrValue.colorline[5]);  // ATTO - 暗红
    }

    // 通道名称映射
    private static final Map<Integer, String> CHANNEL_NAMES = new HashMap<>();

    static {
        CHANNEL_NAMES.put(1, "FAM");
        CHANNEL_NAMES.put(2, "HEX");
        CHANNEL_NAMES.put(3, "ROX");
        CHANNEL_NAMES.put(4, "CY5");
        CHANNEL_NAMES.put(5, "QUASAR");
        CHANNEL_NAMES.put(6, "ATTO");
    }

    public ChartManager(Context context, FragmentAioExDetailBinding binding) {
        this.context = context;
        this.binding = binding;
        this.chart = binding.resultCurveChart;
        initChart();
    }

    // ==================== 初始化 ====================

    /**
     * 初始化图表
     */
    private void initChart() {
        // 无数据提示
        chart.setNoDataText("");
        chart.setNoDataTextColor(Color.GRAY);

        // 边框设置
        chart.setDrawBorders(true);
        chart.setBorderColor(Color.BLACK);
        chart.setBorderWidth(1f);

        // 描述
        Description description = new Description();
        description.setText("");
        description.setTextSize(12f);
        chart.setDescription(description);

        // 图例设置
        Legend legend = chart.getLegend();
        legend.setTextSize(10f);
        legend.setEnabled(false);
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextColor(Color.BLACK);

        // X轴设置
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.LTGRAY);
        xAxis.setGridLineWidth(0.5f);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        // 左侧Y轴设置
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setTextSize(10f);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setGranularity(1f);
        leftAxis.setGranularityEnabled(true);

        // 右侧Y轴（禁用）
        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // 触摸交互
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDoubleTapToZoomEnabled(true);

        // 动画
        chart.animateXY(300, 300);

        // Marker
        chart.post(() -> {
            markView = new LineChartMarkView(context);
            chart.setMarker(markView);
        });

        Log.d(TAG, "Chart initialized");
    }

    // ==================== 绘制曲线 ====================

    /**
     * 绘制曲线
     */
    public void drawCurve(List<SLANCurveList> curveList, CurveType type) {
        this.currentType = type;
        this.currentData = curveList != null ? curveList : new ArrayList<>();

        clearChart();

        if (currentData.isEmpty()) {
            chart.setData(null);
            chart.invalidate();
            Log.d(TAG, "No data to draw for type: " + type.getChineseName());
            return;
        }

        // 创建数据集
        List<ILineDataSet> dataSets = createDataSets(currentData);

        if (dataSets.isEmpty()) {
            chart.setData(null);
            chart.invalidate();
            return;
        }

        // 设置数据
        LineData lineData = new LineData(dataSets);
        lineData.setDrawValues(false);

        // 更新描述
        Description description = chart.getDescription();
        description.setText(type.getChineseName());
        chart.setDescription(description);

        chart.setData(lineData);

        // 应用缩放状态
        applyZoomState(type);

        // 刷新图表
        chart.invalidate();

        Log.d(TAG, "Draw curve completed, type: " + type.getChineseName() +
                ", data size: " + currentData.size());
    }

    /**
     * 创建数据集
     */
    private List<ILineDataSet> createDataSets(List<SLANCurveList> curveList) {
        List<ILineDataSet> dataSets = new ArrayList<>();

        for (SLANCurveList curve : curveList) {
            if (curve == null || curve.getCurve_data() == null ||
                    curve.getCurve_data().isEmpty()) {
                continue;
            }

            LineDataSet dataSet = createDataSet(curve);
            if (dataSet != null) {
                dataSets.add(dataSet);
            }
        }

        return dataSets;
    }

    /**
     * 创建单个数据集
     */
    private LineDataSet createDataSet(SLANCurveList curve) {
        int channelNo = curve.getChannelNo();
        int color = getChannelColor(channelNo);
        String label = getCurveLabel(curve);

        LineDataSet dataSet = new LineDataSet(curve.getCurve_data(), label);

        // 颜色设置
        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        dataSet.setCircleHoleColor(color);

        // 线样式
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);
        dataSet.setLineWidth(2f);

        // 点样式
        dataSet.setCircleSize(1.5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setDrawCircles(false);  // 默认不画点，数据多时性能更好

        // 值显示
        dataSet.setDrawValues(false);
        dataSet.setDrawHighlightIndicators(true);

        // 填充
        dataSet.setDrawFilled(false);

        // 虚线效果（用于图例）
        dataSet.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        dataSet.setFormSize(15f);

        return dataSet;
    }

    /**
     * 获取曲线标签
     */
    private String getCurveLabel(SLANCurveList curve) {
        String hole = curve.getCurve_hole();
        String well = curve.getWellNum();
        int channelNo = curve.getChannelNo();

        StringBuilder label = new StringBuilder();

        if (well != null && !well.isEmpty()) {
            label.append(well);
        }

        if (hole != null && !hole.isEmpty()) {
            if (label.length() > 0) {
                label.append("-");
            }
            label.append(hole);
        }

        if (label.length() == 0) {
            String channelName = CHANNEL_NAMES.get(channelNo);
            label.append(channelName != null ? channelName : "CH" + channelNo);
        }

        return label.toString();
    }

    /**
     * 获取通道颜色
     */
    private int getChannelColor(int channelNo) {
        Integer color = CHANNEL_COLORS.get(channelNo);
        return color != null ? color : Color.BLACK;
    }

    // ==================== 阈值线 ====================

    /**
     * 添加阈值线
     */
    public void addThresholdLine(String label, double threshold, int channelNo) {
        YAxis leftAxis = chart.getAxisLeft();

        // 移除旧的相同通道阈值线
        removeThresholdLine(channelNo);

        float value = (float) threshold;
        int color = getChannelColor(channelNo);

        LimitLine limitLine = new LimitLine(value, label);
        limitLine.setLineWidth(2f);
        limitLine.setTextSize(10f);
        limitLine.setLineColor(color);
        limitLine.setTextColor(color);
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLine.enableDashedLine(10f, 10f, 0f);

        leftAxis.addLimitLine(limitLine);
        thresholdLines.put(channelNo, limitLine);

        chart.invalidate();
        Log.d(TAG, "Added threshold line for channel " + channelNo + ": " + threshold);
    }

    /**
     * 移除阈值线
     */
    public void removeThresholdLine(int channelNo) {
        YAxis leftAxis = chart.getAxisLeft();
        LimitLine line = thresholdLines.remove(channelNo);
        if (line != null) {
            leftAxis.removeLimitLine(line);
            chart.invalidate();
            Log.d(TAG, "Removed threshold line for channel " + channelNo);
        }
    }

    /**
     * 清除所有阈值线
     */
    public void clearThresholdLines() {
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        thresholdLines.clear();
        chart.invalidate();
        Log.d(TAG, "Cleared all threshold lines");
    }

    /**
     * 批量添加阈值线
     */
    public void addThresholdLines(Map<Integer, Double> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Double> entry : thresholds.entrySet()) {
            int channelNo = entry.getKey();
            double value = entry.getValue();
            String label = CHANNEL_NAMES.get(channelNo) + "阈值";
            addThresholdLine(label, value, channelNo);
        }
    }

    // ==================== 缩放控制 ====================

    /**
     * 缩放图表
     */
    public void scaleZoom(float minX, float maxX, float minY, float maxY) {
        if (minX >= maxX || minY >= maxY) {
            Log.w(TAG, "Invalid zoom range");
            return;
        }

        XAxis xAxis = chart.getXAxis();
        YAxis yAxis = chart.getAxisLeft();

        xAxis.setAxisMinimum(minX);
        xAxis.setAxisMaximum(maxX);
        yAxis.setAxisMinimum(minY);
        yAxis.setAxisMaximum(maxY);

        // 保存缩放状态
        zoomStateManager.saveState(currentType, minX, maxX, minY, maxY);

        chart.invalidate();
        Log.d(TAG, "Zoom applied: X[" + minX + ", " + maxX + "], Y[" + minY + ", " + maxY + "]");
    }

    /**
     * 重置缩放
     */
    public void resetZoom() {
        XAxis xAxis = chart.getXAxis();
        YAxis yAxis = chart.getAxisLeft();

        xAxis.resetAxisMinimum();
        xAxis.resetAxisMaximum();
        yAxis.resetAxisMinimum();
        yAxis.resetAxisMaximum();

        // 清除缩放状态
        zoomStateManager.clearState(currentType);

        chart.invalidate();
        Log.d(TAG, "Zoom reset for type: " + currentType.getChineseName());
    }

    /**
     * 应用缩放状态
     */
    private void applyZoomState(CurveType type) {
        ZoomStateManager.ZoomInfo info = zoomStateManager.getState(type);
        if (info != null) {
            XAxis xAxis = chart.getXAxis();
            YAxis yAxis = chart.getAxisLeft();

            xAxis.setAxisMinimum(info.minX);
            xAxis.setAxisMaximum(info.maxX);
            yAxis.setAxisMinimum(info.minY);
            yAxis.setAxisMaximum(info.maxY);

            Log.d(TAG, "Applied zoom state for type: " + type.getChineseName());
        }
    }

    /**
     * 是否已缩放
     */
    public boolean isZoomed() {
        return zoomStateManager.hasState(currentType);
    }

    /**
     * 获取当前缩放信息
     */
    public ZoomStateManager.ZoomInfo getCurrentZoomInfo() {
        return zoomStateManager.getState(currentType);
    }

    // ==================== 坐标轴控制 ====================

    /**
     * 设置X轴范围
     */
    public void setXAxisRange(float min, float max) {
        if (min >= max) return;
        XAxis xAxis = chart.getXAxis();
        xAxis.setAxisMinimum(min);
        xAxis.setAxisMaximum(max);
        chart.invalidate();
    }

    /**
     * 设置Y轴范围
     */
    public void setYAxisRange(float min, float max) {
        if (min >= max) return;
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(min);
        yAxis.setAxisMaximum(max);
        chart.invalidate();
    }

    /**
     * 自动适配坐标轴
     */
    public void autoFitAxis() {
        XAxis xAxis = chart.getXAxis();
        YAxis yAxis = chart.getAxisLeft();

        xAxis.resetAxisMinimum();
        xAxis.resetAxisMaximum();
        yAxis.resetAxisMinimum();
        yAxis.resetAxisMaximum();

        chart.invalidate();
    }

    // ==================== 图表操作 ====================

    /**
     * 清除图表
     */
    public void clearChart() {
        chart.clear();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    /**
     * 刷新图表
     */
    public void refresh() {
        chart.invalidate();
    }

    /**
     * 显示/隐藏图例
     */
    public void showLegend(boolean show) {
        chart.getLegend().setEnabled(show);
        chart.invalidate();
    }

    /**
     * 设置描述文本
     */
    public void setDescription(String text) {
        Description description = chart.getDescription();
        description.setText(text);
        chart.invalidate();
    }

    // ==================== 数据更新 ====================

    /**
     * 更新数据
     */
    public void updateData(List<SLANCurveList> newData) {
        this.currentData = newData != null ? newData : new ArrayList<>();
        drawCurve(this.currentData, this.currentType);
    }

    /**
     * 追加数据
     */
    public void appendData(List<SLANCurveList> additionalData) {
        if (additionalData == null || additionalData.isEmpty()) {
            return;
        }
        this.currentData.addAll(additionalData);
        drawCurve(this.currentData, this.currentType);
    }

    // ==================== Getter/Setter ====================

    public CurveType getCurrentType() {
        return currentType;
    }

    public List<SLANCurveList> getCurrentData() {
        return currentData;
    }

    public LineChart getChart() {
        return chart;
    }

    public Map<Integer, LimitLine> getThresholdLines() {
        return thresholdLines;
    }

    // ==================== 资源释放 ====================

    /**
     * 释放资源
     */
    public void release() {
        if (markView != null) {
            markView = null;
        }
        clearChart();
        clearThresholdLines();
        zoomStateManager.clearAll();
        currentData.clear();
        Log.d(TAG, "ChartManager released");
    }

    // ==================== 内部类 ====================

    /**
     * 缩放状态管理器
     */
    public static class ZoomStateManager {

        private final Map<CurveType, ZoomInfo> stateMap = new HashMap<>();

        /**
         * 保存缩放状态
         */
        public void saveState(CurveType type, float minX, float maxX, float minY, float maxY) {
            if (type == null) return;
            stateMap.put(type, new ZoomInfo(minX, maxX, minY, maxY));
        }

        /**
         * 获取缩放状态
         */
        public ZoomInfo getState(CurveType type) {
            if (type == null) return null;
            return stateMap.get(type);
        }

        /**
         * 清除缩放状态
         */
        public void clearState(CurveType type) {
            if (type == null) return;
            stateMap.remove(type);
        }

        /**
         * 是否有缩放状态
         */
        public boolean hasState(CurveType type) {
            if (type == null) return false;
            return stateMap.containsKey(type);
        }

        /**
         * 清除所有状态
         */
        public void clearAll() {
            stateMap.clear();
        }

        /**
         * 缩放信息
         */
        public static class ZoomInfo {
            public final float minX;
            public final float maxX;
            public final float minY;
            public final float maxY;

            public ZoomInfo(float minX, float maxX, float minY, float maxY) {
                this.minX = minX;
                this.maxX = maxX;
                this.minY = minY;
                this.maxY = maxY;
            }

            @Override
            public String toString() {
                return "ZoomInfo{" +
                        "minX=" + minX +
                        ", maxX=" + maxX +
                        ", minY=" + minY +
                        ", maxY=" + maxY +
                        '}';
            }
        }
    }

    // ==================== 工厂方法（可选） ====================

    /**
     * 创建默认的阈值线
     */
    public static LimitLine createDefaultThresholdLine(String label, double value, int color) {
        LimitLine line = new LimitLine((float) value, label);
        line.setLineWidth(2f);
        line.setTextSize(10f);
        line.setLineColor(color);
        line.setTextColor(color);
        line.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        line.enableDashedLine(10f, 10f, 0f);
        return line;
    }
}