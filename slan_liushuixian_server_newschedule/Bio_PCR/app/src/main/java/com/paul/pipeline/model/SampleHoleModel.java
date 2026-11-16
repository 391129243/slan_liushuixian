package com.paul.pipeline.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableField;

public class SampleHoleModel extends BaseObservable {
    public ObservableField<Integer> index = new ObservableField<>();//孔号
    public ObservableField<String> name = new ObservableField<>();//A1-A8,B1-B8
    public ObservableField<String> scaninfo = new ObservableField<>();
    public ObservableField<String> serialNum = new ObservableField<>(); //样本号
    public ObservableField<String> reagentCode = new ObservableField<>();//试剂码
    public ObservableField<Integer> status = new ObservableField<>();//0
    public ObservableField<Boolean> selected = new ObservableField<>();

    public ObservableField<String> produceddate = new ObservableField<>();//试剂码 生产日期
    public ObservableField<String> year = new ObservableField<>();//试剂码 质保 年
    public ObservableField<String> month = new ObservableField<>();//试剂码 提取月份
    public ObservableField<String> producednum = new ObservableField<>();//试剂码 编号
    public ObservableField<String> info = new ObservableField<>();//试剂码 项目号
    public ObservableField<String> infopath = new ObservableField<>();//试剂码 项目号 目录
    public ObservableField<String> infotemplate = new ObservableField<>();//试剂码 项目号 模板号
    public ObservableField<String> enddate = new ObservableField<>();//试剂码 有效期
    public ObservableField<Boolean> canuse = new ObservableField<>();//试剂码 保质期内
    public SampleHoleModel(int index,String name) {
        this.index.set(index);
        this.status.set(0);
        this.selected.set(false);
        this.name = new ObservableField<>(name);
    }

    public SampleHoleModel(int index) {
        this.index.set(index);
        this.status.set(0);
        this.selected.set(false);
        this.name = new ObservableField<>("");
    }
}
