package com.gidi.bio_pcr.bean;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableField;

public class ConnectModel extends BaseObservable {

    public ObservableField<String> name = new ObservableField<>();//设备名称
    public ObservableField<Integer> index = new ObservableField<>();//设备编号
    public ObservableField<String> type = new ObservableField<>();//设备类型、一体机AIO
    public ObservableField<Boolean> exist = new ObservableField<>();//存在传感器
    public ObservableField<Boolean> rna_connect = new ObservableField<>();//rna连接状态
    public ObservableField<Boolean> pcr_connect = new ObservableField<>();//pcr连接状态
    public ObservableField<Boolean> deviceConnect = new ObservableField<>();
    public ObservableField<Integer> position = new ObservableField<>();//设备编号
    public ObservableField<String> deviceId = new ObservableField<>();
    public ObservableField<String> status = new ObservableField<>();//一体机设备状态
    public ObservableField<String> deviceIp = new ObservableField<String>();//设备IP
    public ObservableField<String> deviceVersion = new ObservableField<>();
    public ObservableField<String> alert = new ObservableField<>();//
    public ObservableField<Boolean> isEnabled = new ObservableField<>();//模组启用状态

}
