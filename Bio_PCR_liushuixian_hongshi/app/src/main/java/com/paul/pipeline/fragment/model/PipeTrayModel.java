package com.paul.pipeline.fragment.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableField;

import com.gidi.bio_pcr.bean.WellInfo;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.data.ScanCodeHelper;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.model.SampleHoleModel;
import com.paul.pipeline.model.TemplateModel;

import java.util.ArrayList;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;

public class PipeTrayModel extends BaseObservable {
    public ObservableField<Integer> index = new ObservableField<>();//界面唯一ID
    public ObservableField<String> name = new ObservableField<>();
    public ObservableField<String> status = new ObservableField<>();
    public ObservableField<Boolean> urgent = new ObservableField<>();
    public ObservableField<Integer> dispatchindex = new ObservableField<>();//调度运行到
    public ObservableField<DispatchModel> dispatch = new ObservableField<>();//调度
    public ObservableArrayList<SampleHoleModel> holes = new ObservableArrayList<>();
    public ObservableField<String> batch = new ObservableField<>();//批次
    public ObservableField<String> project = new ObservableField<>();//项目
    public ObservableField<String> templatenumber = new ObservableField<>();//检测项目
    public ObservableField<String> enddate = new ObservableField<>();//检测项目
    public ObservableArrayList<WellInfo> wellInfolist = new ObservableArrayList<>();//病人信息
    public ObservableArrayList<DispatchModel> dispatchlist = new ObservableArrayList<>();//调度清单
    public ObservableField<TemplateModel> template = new ObservableField<>();
    //    public ObservableField<Experiment> experiment = new ObservableField<>();//实验数据
    public ObservableField<String> runningtime = new ObservableField<>();
    public ObservableField<Integer> flagstatus = new ObservableField<>();//状态标记,start,stop,pause,continue
    public ObservableField<String> deviceID = new ObservableField<>();//": "2740835",
    public ObservableField<String> deviceType = new ObservableField<>();//: "XXXX"，
    public ObservableField<Boolean> exist = new ObservableField<>();//存在传感器
    public ObservableField<Long> createTime = new ObservableField<>();//提交时间
    public ObservableField<Integer> errorDispatchindex = new ObservableField<>();//是否完成

    public PipeTrayModel(){
        init();
    }
    public PipeTrayModel(int index, String name) {
        initModel(index,name);
    }

    public void initModel(int index, String name){
        this.name.set(name);
        this.index.set(index);
        init();
    }

    /**
     * 清除历史批次/项目/调度信息，同时保留各孔样本的条码（reagentCode）和编号（serialNum）。
     * <p>
     * 清除：batch、project、template、dispatchlist、dispatchindex、status、flagstatus、
     *       errorDispatchindex、createTime、runningtime、urgent。
     * 保留：holes 中每孔的 serialNum/reagentCode 及解析字段；exist（硬件传感器）；
     *       templatenumber、enddate（试剂基础信息）。
     * wellInfolist 从保留的 holes 重建，保持一致性。
     *
     * @param index 托盘序号
     * @param name  托盘名称
     */
    public void clearKeepSamples(int index, String name) {
        this.name.set(name);
        this.index.set(index);

        // 先把当前 holes 里的样本数据全部保存下来
        ArrayList<SampleHoleModel> savedHoles = new ArrayList<>(holes);

        // 重置调度 / 批次 / 项目 / 运行状态字段
        this.status.set(ConstantData.str_FREE);
        this.dispatchindex.set(1);
        this.urgent.set(false);
        this.flagstatus.set(ConstantData.FLAG_FREE);
        this.errorDispatchindex.set(0);
        this.createTime.set(0L);
        this.batch.set(null);
        this.project.set(null);
        this.template.set(null);
        this.runningtime.set(null);
        this.dispatch.set(null);
        this.dispatchlist.clear();

        // 重建 holes 和 wellInfolist，仅恢复保留的样本数据（条码+编号）
        this.holes.clear();
        this.wellInfolist.clear();
        for (SampleHoleModel hole : savedHoles) {
            this.holes.add(hole);
            String pos = "A" + (hole.index.get() != null ? hole.index.get() : "");
            String sn  = hole.serialNum.get()    != null ? hole.serialNum.get()    : "";
            String rc  = hole.reagentCode.get()  != null ? hole.reagentCode.get()  : "";
            this.wellInfolist.add(new WellInfo(pos, sn, rc));
        }
    }

    private void init(){
        this.status.set(ConstantData.str_FREE);
        this.exist.set(false);
        this.dispatchindex.set(1);
        this.urgent.set(false);
        this.flagstatus.set(ConstantData.FLAG_FREE);
        this.errorDispatchindex.set(0);
        this.createTime.set(0L);
        this.dispatchlist.clear();

        holes.clear();
        wellInfolist.clear();
        templatenumber.set("KP03");
        enddate.set("260112");
        for (int i = 1; i <= 8; i++) {
            String code ="260112101008702KP03";
            String num = "25152674"+i;
            SampleHoleModel hole = new SampleHoleModel(i);
            hole.serialNum.set(num);
            hole.reagentCode.set(code);
            hole.status.set(1);
            hole = ScanCodeHelper.parseSampleHoleModel(hole);
            holes.add(hole);
            wellInfolist.add(new WellInfo("A"+i,num,code));
        }

    }

}
