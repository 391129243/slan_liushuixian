package com.gidi.bio_pcr.slan.bean;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.List;

/**
 * 自定义LineDataSet，增加channelNo字段
 */
public class CustomLineDataSet extends LineDataSet {
    private int channelNo;

    public CustomLineDataSet(List<Entry> entries, String label) {
        super(entries, label);
    }

    public void setChannelNo(int channelNo) {
        this.channelNo = channelNo;
    }

    public int getChannelNo() {
        return channelNo;
    }
}
