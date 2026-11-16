package com.winters.boweilibrary.BoWei.model.device;

import java.util.ArrayList;

public class TargetStd {
    private String TargetName;//检测目标名称
    private String Slope;//标准曲线斜率
    private String Intercept;//标准曲线截距
    private String r_Value;//标准曲线相关系数
    private String Efficiency;//扩增效率
    private ArrayList<Integer> StdDotWellNo;//标准品数据点所在孔的编号列表
}
