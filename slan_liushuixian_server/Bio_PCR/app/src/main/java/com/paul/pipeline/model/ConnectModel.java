package com.paul.pipeline.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableField;

public class ConnectModel extends BaseObservable {
    public ObservableField<String> name = new ObservableField<>();
    public ObservableField<Integer> index = new ObservableField<>();
    public ObservableField<String> type = new ObservableField<>();//tray arm pcr rna
    public ObservableField<Boolean> exist = new ObservableField<>();//存在传感器
    public ObservableField<Boolean> connect = new ObservableField<>();//连接状态
    public ObservableField<String> status = new ObservableField<>();//
    public ObservableField<String> linuxstatus = new ObservableField<>();//
    public ObservableField<String> alert = new ObservableField<>();//
    public ObservableField<Boolean> cover = new ObservableField<>();//盖子存在传感器
    public ObservableField<String> source = new ObservableField<>();//
}
