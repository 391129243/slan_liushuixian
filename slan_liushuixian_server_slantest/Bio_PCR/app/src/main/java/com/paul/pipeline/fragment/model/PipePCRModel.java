package com.paul.pipeline.fragment.model;

import android.util.Log;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableField;

import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.model.DispatchModel;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;

public class PipePCRModel extends BaseObservable {
    public ObservableField<Integer> index = new ObservableField<>();//界面唯一ID
    public ObservableField<String> name= new ObservableField<>();
    public ObservableField<String> version = new ObservableField<>();
    public ObservableField<String> deviceID= new ObservableField<>();//"2740835",
    public ObservableField<String> deviceType= new ObservableField<>();// "XXXX"，
    public ObservableField<String> linuxStatus= new ObservableField<>();
    public ObservableField<String> linuxAlarm= new ObservableField<>();
    public ObservableField<String> sendAlarm= new ObservableField<>();
    public ObservableField<String> status = new ObservableField<>();//状态  free、running、error、finish 完成
    public ObservableField<Double> degree = new ObservableField<>();//温度
    public ObservableField<Double> degreeTarget = new ObservableField<>();//温度
    public ObservableField<String> alarm = new ObservableField<>();//异常信息
    public ObservableField<Boolean> alarmenable = new ObservableField<>();//异常信息
    public ObservableField<String> allstatus = new ObservableField<>();//状态  free、error
    //正在反应的托盘，调度时需要设置并记忆到sharedperference
    public ObservableField<String> currentTemplate = new ObservableField<>();////实时运行的指令信息，包括第几条指令等，暂时实时显示
    public ObservableField<String> currentSection = new ObservableField<>();//模板
    public ObservableField<String> currentStep= new ObservableField<>();//模板
    public ObservableField<Integer> currentCircle = new ObservableField<>();//信息
    public ObservableField<Integer> totalCycle = new ObservableField<>();//信息
    public ObservableField<Integer> currentTray = new ObservableField<>();//托盘信息
    public ObservableField<Integer> dispatchindex = new ObservableField<>();//调度信息
    public ObservableField<DispatchModel> currentDispatch = new ObservableField<>(); //当前调度记录（完整数据）
    public ObservableField<Boolean> cover = new ObservableField<>();//盖子
    public ObservableField<Boolean> connect = new ObservableField<>();//是否连接
    public ObservableField<String> startTime = new ObservableField<>();//
    public ObservableField<String> stopTime = new ObservableField<>();//
    public ObservableField<Boolean> testrun = new ObservableField<>();//是否测试模板
    public ObservableField<Integer> runningTime = new ObservableField<>();//
    public ObservableField<Integer> totalTime = new ObservableField<>();//运行模板需要的总时间，直接显示
    public ObservableField<Integer> leftTime = new ObservableField<>();//正在运行模板剩余时间，直接显示
    public ObservableField<Integer> waitTime = new ObservableField<>();//正在运行模板等待时间，直接显示
    public ObservableField<String> info = new ObservableField<>();//运行模板信息，直接显示
    public ObservableField<Boolean> exist = new ObservableField<>();//存在传感器
//    public ObservableField<Experiment> experiment = new ObservableField<>();//实验数据
    public ObservableField<String> deviceIP = new ObservableField<>();//ip
    public ObservableField<String> devicePort= new ObservableField<>();// port
    public ObservableField<String> key = new ObservableField<>();//下发地址
    public ObservableField<Integer> armStep = new ObservableField<>();//调度步骤 1取盘 2模板运行 3送盘
    public ObservableField<Boolean> uiinit = new ObservableField<>();//界面初始化

    public ObservableField<Boolean> gettrayfinish = new ObservableField<>();//托盘是否完成
    public ObservableField<Boolean> gettraystart = new ObservableField<>();//托盘是否开始
    public ObservableField<Boolean> excutestart = new ObservableField<>();//是否运行
    public ObservableField<Boolean> excutefinish = new ObservableField<>();//运行结束
    public ObservableField<Boolean> backtraystart = new ObservableField<>();//送回托盘启动
    public ObservableField<Boolean> backtrayfinish = new ObservableField<>();//送回托盘完成
    public ObservableField<Integer> usererror= new ObservableField<>();//0默认  1用户异常
    public ObservableField<Boolean> movetraystart = new ObservableField<>();//移动托盘启动
    public ObservableField<Boolean> movetrayfinish = new ObservableField<>();//移动托盘完成
    public PipePCRModel(){

    }
    public PipePCRModel(int index, String name) {
        initModel(index,name);
    }
    public void initModel(int index, String name){
        this.deviceID.set(ConstantData.namePCR+""+index);
        this.deviceType.set(ConstantData.namePCR);
        this.status.set(ConstantData.str_ERROR);
        this.linuxStatus.set(ConstantData.str_ERROR);
//        Log.w("initModel", ".currentTray.set(0)");
//        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//        for (StackTraceElement element : stackTrace) {
//            Log.d(".currentTray.set(0)", element.toString());
//        }
        this.currentTray.set(0);
        this.currentCircle.set(0);
        this.index.set(index);
        this.name.set(name);
        this.connect.set(false);
        this.armStep.set(0);
        this.exist.set(false);
        this.excutestart.set(false);
        this.excutefinish.set(false);
        this.backtraystart.set(false);
        this.backtrayfinish.set(false);
        this.gettraystart.set(false);
        this.gettrayfinish.set(false);
        this.alarmenable.set(false);
        this.uiinit.set(false);
        this.usererror.set(0);
        this.movetraystart.set(false);
        this.movetrayfinish.set(false);
        this.dispatchindex.set(0);
        this.currentDispatch.set(null); // 清空当前调度记录
        testrun.set(false);
    }
}
