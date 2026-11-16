package com.paul.pipeline.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.paul.pipeline.fragment.model.PipePCRModel;

public class LinuxDeviceModel {
    public MutableLiveData<Integer> index = new MutableLiveData<>();
    public MutableLiveData<String> deviceid = new MutableLiveData<>();
    public MutableLiveData<String> ipport = new MutableLiveData<>();
    public MutableLiveData<String> type = new MutableLiveData<>();
    public MutableLiveData<String> position = new MutableLiveData<>();
    public MutableLiveData<String> version = new MutableLiveData<>();
    public MutableLiveData<Boolean> selected = new MutableLiveData<>();
    public MutableLiveData<String> result = new MutableLiveData<>();
    public MutableLiveData<Integer> updatestatus = new MutableLiveData<>();

    public LinuxDeviceModel(){
        updatestatus.setValue(0);
    }
    public void updateInfo(PipePCRModel model){
        index.setValue(model.index.get());
    }
}
