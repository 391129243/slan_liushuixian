package com.paul.pipeline.listener;

import android.view.View;

public interface OnItemChildClickListener {
    /**
     * 子组件点击事件
     * @param view
     * @param position
     */
    void onItemChildClick(View view, int position);
}
