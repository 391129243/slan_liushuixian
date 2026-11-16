package com.winters.boweilibrary.BoWei.model.experiment;

import com.hongshi.slansdk.model.experiment.TargetParamter;

/**
 * 目标
 */
public class Target {
    private String Name;//目标名称
    private int TubeNo;//试管编号（从1开始）
    private String TubeName;//试管名称
    private int ChannelNo;//通道编号（从1开始）
    private  int TargetIndex;//编号（从1开始）
    private String Dye;//染料名称
    private String Color;//颜色ARGB值，例如：黑色（#FF000000），蓝色（#FF0000FF）
    private TargetParamter Paramter;//目标参数，根据实验类型创建不同的参数对象
}
