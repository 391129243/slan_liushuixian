package com.paul.pipeline.fragment.model;

import androidx.databinding.ObservableField;

public class PipeDoorModel {
    public ObservableField<Boolean> armhold = new ObservableField<>();//标记
    public ObservableField<Boolean> doorhold = new ObservableField<>();//标记
    public ObservableField<Boolean> doorlocked = new ObservableField<>();//是否锁止
    public ObservableField<Boolean> doorposition = new ObservableField<>();//是否入位
}
