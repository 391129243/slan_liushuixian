package com.gidi.bio_pcr.slan.bean;

import android.widget.TextView;

import java.util.Objects;

/**
 * 单个孔位状态封装，消除 isA1Check isA1Enable 零散变量
 */
public class WellState {
    public final String wellName;
    public TextView textView;
    public boolean enable;
    public boolean checked;

    public WellState(String wellName){
        this.wellName = wellName;
    }

    public void reset(){
        enable = false;
        checked = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WellState wellState = (WellState) o;
        return Objects.equals(wellName, wellState.wellName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wellName);
    }
}