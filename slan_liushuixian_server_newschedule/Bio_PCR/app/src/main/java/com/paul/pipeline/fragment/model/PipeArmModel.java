package com.paul.pipeline.fragment.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableField;

import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.message.ArmworkMessage;

public class PipeArmModel extends BaseObservable {
    public ObservableField<Integer> index = new ObservableField<>();//界面唯一ID
    public ObservableField<String> name= new ObservableField<>();
    public ObservableField<Integer> sourcePosition= new ObservableField<>();
    public ObservableField<Integer> targetPosition= new ObservableField<>();
    //机械臂当前持有/暂存的遮光盖来源PCR工位(1-6)，抓盖到暂存位时持久化，用于一键还盘时按来源归还
    public ObservableField<Integer> pcrCoverSource= new ObservableField<>();

    public ObservableField<String> target = new ObservableField<>();//目标位置
    public ObservableField<String> source = new ObservableField<>();//原位置

    public ObservableField<String> status = new ObservableField<>();//状态  free、running、error
    public ObservableField<String> no= new ObservableField<>();//实时运行的指令信息，包括第几条指令等，暂时实时显示
    public ObservableField<String> degree= new ObservableField<>();//温度
    public ObservableField<String> alarm= new ObservableField<>();//异常信息
    public ObservableField<String> motorID= new ObservableField<>();//马达信息
    public ObservableField<Boolean> connect= new ObservableField<>();//是否连接
    public ObservableField<String> taskRunning= new ObservableField<>();

    public ObservableField<ArmworkMessage> currentArmworkMessage = new ObservableField<>();
    public ObservableField<Boolean> autosamplecode = new ObservableField<>();//是否自动更正编码位置
    public ObservableField<String> deviceID= new ObservableField<>();//"2740835",
    public ObservableField<String> deviceType= new ObservableField<>();// "XXXX"，
    public ObservableField<String> deviceIP = new ObservableField<>();//ip
    public ObservableField<String> devicePort= new ObservableField<>();// port
    public ObservableField<String> key = new ObservableField<>();//下发地址
    public ObservableField<Boolean> exist = new ObservableField<>();//存在传感器
    public ObservableField<Boolean> finish = new ObservableField<>();//是否完成
    public ObservableField<Integer> tray = new ObservableField<>();//托盘原位
    public ObservableField<Integer> armwork = new ObservableField<>();//当前任务
    public ObservableField<String> action = new ObservableField<>();//机械臂信息
    //机械臂手上物品：EMPTY / UNKNOWN / PAN / RNA / PCR / PCR_COVER（null 视为 UNKNOWN），由取/放成功的传感器校验结果维护
    public ObservableField<String> holdingType = new ObservableField<>();
    //手上物品的来源位置（holdingType 为物品类型时有效）
    public ObservableField<Integer> holdingPosition = new ObservableField<>();

    public PipeArmModel(){

    }

    public PipeArmModel(int index, String name) {
        initModel(index,name);
    }
    public void initModel(int index, String name){
        this.index.set(index);
        this.name.set(name);
        init();
    }
    public void init(){
        this.deviceID.set(ConstantData.nameARM+""+index);
        this.deviceType.set(ConstantData.nameARM);
        this.status.set(ConstantData.str_ERROR);
        this.tray.set(0);
        this.connect.set(false);
        this.finish.set(true);
        this.exist.set(false);
        this.autosamplecode.set(false);
        this.armwork.set(ConstantData.ARM_FREE);
    }
}
