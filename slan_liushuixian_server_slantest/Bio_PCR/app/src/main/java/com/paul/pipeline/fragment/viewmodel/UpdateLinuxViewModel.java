package com.paul.pipeline.fragment.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.paul.pipeline.model.LinuxDeviceModel;

import java.util.ArrayList;

public class UpdateLinuxViewModel extends ViewModel {
    public MutableLiveData<String> filename = new MutableLiveData<>();
    public MutableLiveData<String> filepath = new MutableLiveData<>();
    public MutableLiveData<String> filetype = new MutableLiveData<>();
    public MutableLiveData<String> fileversion = new MutableLiveData<>();
    public MutableLiveData<String> filealert = new MutableLiveData<>();
    public MutableLiveData<Integer> updatestatus = new MutableLiveData<>();//0free 1start 2finish
    public MutableLiveData<String> updateall = new MutableLiveData<>();
    public MutableLiveData<String> updatefinish = new MutableLiveData<>();
    public MutableLiveData<String> updatefail = new MutableLiveData<>();
    public MutableLiveData<String> updateversion = new MutableLiveData<>();
    public MutableLiveData<ArrayList<LinuxDeviceModel>> devicelist = new MutableLiveData<>();
    public UpdateLinuxViewModel() {
        ArrayList<LinuxDeviceModel> list = new ArrayList<>();

//        LinuxDeviceModel model1 = new LinuxDeviceModel();
//        model1.index.setValue(1);
//        model1.deviceid.setValue("pcr111");
//        model1.type.setValue("PCR");
//        model1.position.setValue("1");
//        model1.selected.setValue(false);
//        list.add(model1);
//        LinuxDeviceModel model2 = new LinuxDeviceModel();
//        model2.index.setValue(1);
//        model2.deviceid.setValue("rna111");
//        model2.type.setValue("RNA");
//        model2.position.setValue("1");
//        model2.selected.setValue(false);
//        list.add(model2);

        devicelist.setValue(list);
        updatestatus.setValue(0);
        updateall.setValue("");
        updatefinish.setValue("");
        updatefail.setValue("");
    }
}