package com.paul.pipeline.listener;

import android.view.View;

import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;

public interface ArmFinishCallback {
    void finish(int arm, ItemProperty aim, ItemProperty source);
    void fail(int arm,ItemProperty aim, ItemProperty source,String error);
}
