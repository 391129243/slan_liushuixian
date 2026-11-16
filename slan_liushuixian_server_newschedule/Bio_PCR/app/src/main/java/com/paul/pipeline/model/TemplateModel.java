package com.paul.pipeline.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.ObservableArrayList;
import androidx.databinding.ObservableField;

import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;

import java.util.ArrayList;
import java.util.List;

/*
//模板文件内容
 */
public class TemplateModel extends BaseObservable {
    public ObservableField<Integer> index = new ObservableField<>();//界面唯一ID
    public ObservableField<String> name = new ObservableField<>();//名
    public ObservableField<String> firstName = new ObservableField<>();//名
    public ObservableField<String> secondName = new ObservableField<>();//名
    public ObservableField<String> thirdName = new ObservableField<>();//名
    public ObservableField<String> trim_name = new ObservableField<>();//去掉分隔符号~的名
    public ObservableField<String> path = new ObservableField<>();//项目地址
    public ObservableField<Integer> templateType = new ObservableField<>();//模板类型 1-single 2-multi 3-rna 4-pcr
    public ObservableField<Integer> selectType = new ObservableField<>();//模板类型 0-full 1-rna 2-pcr
    public ObservableField<Boolean> selected = new ObservableField<>();//选择
    public ObservableField<Boolean> usect = new ObservableField<>();
    public ObservableField<String> ct = new ObservableField<>();
    public ObservableField<Boolean> usepeak = new ObservableField<>();
    public ObservableField<String> peak = new ObservableField<>();
    public ObservableField<List<String>> peakList = new ObservableField<>(new ArrayList<>());
    public ObservableField<List<String>> ctList = new ObservableField<>(new ArrayList<>());

    public ObservableField<TotalTime> totaltime = new ObservableField<>();
    public ObservableField<Integer> temparatureCtrl = new ObservableField<>();//0fast 1standard
    public ObservableField<String> templateno = new ObservableField<>();//
    //single
    public ObservableField<String> single_hex = new ObservableField<>();
    public ObservableField<String> single_prj = new ObservableField<>();
    //nulti
    public ObservableField<String> multi_hex_first = new ObservableField<>();
    public ObservableField<String> multi_prj_first = new ObservableField<>();
    public ObservableField<String> multi_hex_second = new ObservableField<>();
    public ObservableField<String> multi_prj_second = new ObservableField<>();
    //RNA
    public ObservableField<String> rna_hex = new ObservableField<>();
    //PCR
    public ObservableField<String> pcr_prj = new ObservableField<>();

    public TemplateModel() {
        this.selected.set(false);
    }

}
