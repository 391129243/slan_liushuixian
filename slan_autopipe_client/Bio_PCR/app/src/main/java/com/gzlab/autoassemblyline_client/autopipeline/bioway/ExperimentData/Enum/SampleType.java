package com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Enum;

public enum SampleType {

    /// <summary>
    /// 样品
    /// </summary>

    Unknown(0),
    /// <summary>
    /// 标准品
    /// </summary>

    Standard(1),
    /// <summary>
    /// 阳性对照
    /// </summary>

    HighPositive(2),
    /// <summary>
    /// 临界阳性对照
    /// </summary>

    LowPositive(3),
    /// <summary>
    /// 阴性对照
    /// </summary>

    Negative(4),
    /// <summary>
    /// 无模板对照
    /// </summary>

    NTC(5),
    /// <summary>
    /// 校准品（质控品）
    /// </summary>

    QC(6),
    /// <summary>
    /// 重测样品
    /// </summary>

    RetestSample(7),
    /// <summary>
    /// 参比样品
    /// </summary>

    Calibrator(8),
    /// <summary>
    /// 最后（只用作标识）
    /// </summary>

    Last(9),

    Custom(1000);//自定义类型

    SampleType(int i) {
        this.value = i;
    }

    int value;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
