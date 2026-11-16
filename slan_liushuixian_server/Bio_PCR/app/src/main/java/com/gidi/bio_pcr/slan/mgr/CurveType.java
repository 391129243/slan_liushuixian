package com.gidi.bio_pcr.slan.mgr;

import com.gidi.bio_pcr.R;

/**
 * 曲线类型枚举
 * 用于标识当前显示的曲线类型
 */
public enum CurveType {

    /**
     * 原始曲线
     */
    RAW(R.id.rawcurve_rb, true, "RawCurve", "原始曲线"),

    /**
     * 扩增曲线
     */
    AMP(R.id.ampcurve_rb, true, "AMPCurve", "扩增曲线"),

    /**
     * 熔解曲线
     */
    MELT(R.id.meltcurve_rb, true, "MeltCurve", "熔解曲线"),

    /**
     * 峰值曲线
     */
    PEAK(R.id.peakcurve_rb, true, "PeakCurve", "峰值曲线"),

    /**
     * Excel数据列表
     */
    EXCEL(R.id.result_list_rb, false, "Excel", "数据列表"),

    /**
     * 二次分析
     */
    SECOND_ANALYSIS(R.id.sec_analyse_rb, false, "SecondAnalysis", "二次分析");

    private final int id;              // 对应的View ID
    private final boolean isChart;     // 是否为图表类型
    private final String description;  // 英文描述
    private final String chineseName;  // 中文名称

    CurveType(int id, boolean isChart, String description, String chineseName) {
        this.id = id;
        this.isChart = isChart;
        this.description = description;
        this.chineseName = chineseName;
    }

    /**
     * 根据View ID获取对应的CurveType
     */
    public static CurveType fromId(int id) {
        for (CurveType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return RAW; // 默认返回原始曲线
    }

    /**
     * 根据名称获取对应的CurveType
     */
    public static CurveType fromName(String name) {
        if (name == null) {
            return RAW;
        }
        for (CurveType type : values()) {
            if (type.name().equalsIgnoreCase(name) ||
                    type.description.equalsIgnoreCase(name) ||
                    type.chineseName.equals(name)) {
                return type;
            }
        }
        return RAW;
    }

    /**
     * 判断是否为图表类型（需要显示LineChart）
     */
    public boolean isChartType() {
        return isChart;
    }

    /**
     * 判断是否为Excel列表类型
     */
    public boolean isExcelType() {
        return this == EXCEL;
    }

    /**
     * 判断是否为二次分析类型
     */
    public boolean isSecondAnalysisType() {
        return this == SECOND_ANALYSIS;
    }

    /**
     * 获取对应的View ID
     */
    public int getId() {
        return id;
    }

    /**
     * 获取英文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 获取中文名称
     */
    public String getChineseName() {
        return chineseName;
    }

    @Override
    public String toString() {
        return chineseName;
    }
}