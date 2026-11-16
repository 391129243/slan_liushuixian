package com.paul.pipeline.fragment.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableField;

import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.model.ArmStep;
import com.paul.pipeline.model.DeviceRuntimeState;
import com.paul.pipeline.model.DispatchModel;

public class PipeRNAModel extends BaseObservable {
    public ObservableField<Integer> index = new ObservableField<>();//界面唯一ID
    public ObservableField<String> name= new ObservableField<>();
    public ObservableField<String> version = new ObservableField<>();
    public ObservableField<String> deviceID= new ObservableField<>();//"2740835",
    public ObservableField<String> deviceType= new ObservableField<>();// "XXXX"，
    public ObservableField<String> linuxStatus= new ObservableField<>();
    public ObservableField<String> linuxAlarm= new ObservableField<>();
    public ObservableField<String> sendAlarm= new ObservableField<>();
    public ObservableField<String> motoid = new ObservableField<>();//
    public ObservableField<String> status= new ObservableField<>();//状态  free、running、error、finish 完成
    public ObservableField<String> no = new ObservableField<>();//实时运行的指令信息，包括第几条指令等，暂时实时显示
    public ObservableField<String> degree= new ObservableField<>();//温度
    public ObservableField<String> alarm= new ObservableField<>();//异常信息
    public ObservableField<Boolean> alarmenable = new ObservableField<>();//异常信息
    public ObservableField<String> currentTemplate= new ObservableField<>();//模板
    public ObservableField<Integer> currentTray = new ObservableField<>();//托盘信息
    public ObservableField<Boolean> connect= new ObservableField<>();//是否连接
    public ObservableField<Integer> dispatchindex = new ObservableField<>();//调度信息
//    public ObservableField<DispatchModel> dispatch = new ObservableField<>();//调度信息
    public ObservableField<String> allstatus = new ObservableField<>();//状态  free、error
    public ObservableField<String> runninginfo = new ObservableField<>();/////暂时实时显示运行信息
    public ObservableField<Integer> totalTime = new ObservableField<>();//运行模板需要的总时间，直接显示
    public ObservableField<Integer> leftTime = new ObservableField<>();//正在运行模板剩余时间，直接显示
    public ObservableField<String> info = new ObservableField<>();//运行模板信息，直接显示
    public ObservableField<String> deviceIP = new ObservableField<>();//ip
    public ObservableField<String> devicePort = new ObservableField<>();// port
    public ObservableField<String> key = new ObservableField<>();//下发地址
    public ObservableField<Boolean> exist = new ObservableField<>();//存在传感器
    public ObservableField<String> startTime = new ObservableField<>();//
    public ObservableField<String> stopTime = new ObservableField<>();//
    public ObservableField<ArmStep> armStep = new ObservableField<>();//调度步骤 1取盘 2模板运行 3送盘
    //调度运行态：原先散落的一组调度布尔，收拢到普通 POJO，不再参与 databinding
    public DeviceRuntimeState runtime = new DeviceRuntimeState();
    public ObservableField<Integer> usererror= new ObservableField<>();//0默认  1用户异常
    public PipeRNAModel(){

    }
    public PipeRNAModel(int index, String name) {
        initModel(index,name);
    }

    public void initModel(int index, String name){
        this.index.set(index);
        this.name.set(name);
        this.deviceID.set(ConstantData.nameARM+""+index);
        this.deviceType.set(ConstantData.nameARM);
        this.status.set(ConstantData.str_ERROR);
        this.linuxStatus.set(ConstantData.str_ERROR);
        this.currentTray.set(0);
        this.connect.set(false);
        this.exist.set(false);
        this.armStep.set(ArmStep.FREE);
        this.runtime.setOpendoorfinish(false);
        this.runtime.setExcutestart(false);
        this.runtime.setExcutefinish(false);
        this.runtime.setBacktraystart(false);
        this.runtime.setBacktrayfinish(false);
        this.alarmenable.set(false);
        this.runtime.setUiinit(false);
        this.usererror.set(0);
        this.runtime.setMovetraystart(false);
        this.runtime.setMovetrayfinish(false);
        this.dispatchindex.set(0);
        this.runtime.setClosedoor(false);
    }
}
