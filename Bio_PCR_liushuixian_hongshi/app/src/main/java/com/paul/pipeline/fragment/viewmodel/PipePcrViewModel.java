package com.paul.pipeline.fragment.viewmodel;

import androidx.databinding.ObservableField;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PipePcrViewModel extends ViewModel {
    public PipePcrViewModel() {
        radioGroupOption.setValue(0);
    }

    MutableLiveData<Integer> radioGroupOption = new MutableLiveData<>();//pcr显示曲线、步骤

    public MutableLiveData<Integer> getRadioGroupOption() {
        return radioGroupOption;
    }

    public void setRadioGroupOption(int radioGroupOption) {
        this.radioGroupOption.setValue(radioGroupOption);
    }
}