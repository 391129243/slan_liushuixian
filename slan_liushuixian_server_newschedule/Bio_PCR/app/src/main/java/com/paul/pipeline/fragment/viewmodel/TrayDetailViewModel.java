package com.paul.pipeline.fragment.viewmodel;

import androidx.databinding.ObservableField;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.paul.pipeline.model.SampleHoleModel;
import com.paul.pipeline.model.TemplateModel;

import java.util.ArrayList;

public class TrayDetailViewModel extends ViewModel {
    public ObservableField<String> selecttitle = new ObservableField<>();//显示
    public ObservableField<Integer> selectholeindex = new ObservableField<>();//显示
    public TrayDetailViewModel(){
//        this.radioGroupOption.set(0);
        this.radioGroupTempType.set(0);
        setTemplateType(new Integer(0));
    }
    private final MutableLiveData<String> scaninfo = new MutableLiveData<String>();
    public void setScaninfo(String item) {
        scaninfo.setValue(item);
    }
    public MutableLiveData<String> getScaninfo() {
        return scaninfo;
    }

    private final MutableLiveData<SampleHoleModel> selectedHole = new MutableLiveData<SampleHoleModel>();
    public void setSelectedHole(SampleHoleModel item) {
        selectedHole.setValue(item);
    }
    public MutableLiveData<SampleHoleModel> getSelectedHole() {
        return selectedHole;
    }

//    public ObservableField<Integer> radioGroupOption = new ObservableField<>();//显示
    public ObservableField<String> tempTypeName = new ObservableField<>();//模板類型
    public ObservableField<Integer> radioGroupTempType = new ObservableField<>();//模板類型

    private final MutableLiveData<Integer> templateType = new MutableLiveData<>();
    public void setTemplateType(Integer item) {
        templateType.setValue(item);
    }
    public MutableLiveData<Integer> getTemplateType() {
        return templateType;
    }

    private final MutableLiveData<TemplateModel> templateModel = new MutableLiveData<>();
    public void setTemplateModel(TemplateModel item) {templateModel.setValue(item);}
    public MutableLiveData<TemplateModel> getTemplateModel() {
        return templateModel;
    }

    private final MutableLiveData<TemplateModel> templateSelected = new MutableLiveData<>();
    public void setTemplateSelected(TemplateModel item) {
        templateSelected.setValue(item);
    }
    public MutableLiveData<TemplateModel> getTemplateSelected() {
        return templateSelected;
    }
//    //页面导航
//    private MutableLiveData<Integer> navEvent = new MutableLiveData<>();
//    public LiveData<Integer> getNavEvent() {
//        return navEvent;
//    }
//    public void onNav(int id) {
//        navEvent.setValue(id);
//    }
}
