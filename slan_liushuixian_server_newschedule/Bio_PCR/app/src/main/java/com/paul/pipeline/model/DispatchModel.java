package com.paul.pipeline.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableField;

import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;
import com.paul.pipeline.data.ConstantData;
//调度列表项
public class DispatchModel extends BaseObservable {
    //托盘
    public ObservableField<Integer> index = new ObservableField<>();//序号从1开始
    public ObservableField<String> indexname = new ObservableField<>();//index
    public ObservableField<String> name = new ObservableField<>();//步骤名字
    public ObservableField<String> templatename = new ObservableField<>();//模板 hex prj
    public ObservableField<String> templateno = new ObservableField<>();//模板代码
    public ObservableField<Integer> status = new ObservableField<>();//free running finish
    public ObservableField<String> batch = new ObservableField<>();//
    public ObservableField<String> path = new ObservableField<>();//项目目录
    public ObservableField<String> projectname = new ObservableField<>();//模板名first
    public ObservableField<Integer> targettype = new ObservableField<>();//task pcr rna
    public ObservableField<Integer> target = new ObservableField<>();//pcr 1-6 rna1-2

    public ObservableField<TotalTime> totaltime = new ObservableField<>();
    //tray
    public ObservableField<Integer> tray = new ObservableField<>();//tary 1-8
    //rna
    public ObservableField<Integer> phase = new ObservableField<>();
    public ObservableField<Integer> phasestep = new ObservableField<>();
    //pcr
    public ObservableField<Integer> circle = new ObservableField<>();
    public ObservableField<Integer> firstSecond = new ObservableField<>();
    public ObservableField<Long> runtime = new ObservableField<>();

    public ObservableField<String> createtime = new ObservableField<>();
    public ObservableField<String> starttime = new ObservableField<>();
    public ObservableField<String> stoptime = new ObservableField<>();
    public ObservableField<Integer> devicestatus = new ObservableField<>();//用来显示给用户看的 free running finish
    public ObservableField<Boolean> selected = new ObservableField<>();
    public ObservableField<Boolean> savepcr = new ObservableField<>();
    public ObservableField<Integer> nextdevice = new ObservableField<>();//下一个设备
    public DispatchModel(){
        status.set(ConstantData.STATUS_FREE);
        savepcr.set(false);
        selected.set(false);
        firstSecond.set(0);
        runtime.set(0L);
        circle.set(0);
        phase.set(0);
        phasestep.set(0);
        templateno.set("00");
        nextdevice.set(0);
    }
}
