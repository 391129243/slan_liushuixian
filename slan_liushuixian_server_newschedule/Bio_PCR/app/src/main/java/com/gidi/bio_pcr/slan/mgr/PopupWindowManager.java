package com.gidi.bio_pcr.slan.mgr;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;

import java.util.List;

/**
 * 弹窗管理器
 * 职责：管理所有PopupWindow的创建和显示
 *
 * 调用方：
 * 1. ParameterManager - 显示定性/定量、基线优化、数字滤波、峰类型等弹窗
 * 2. ActionManager - 显示更多操作弹窗
 * 3. AIoExDetailFragment - 显示扩增算法选择弹窗
 */
public class PopupWindowManager {

    private final Context context;
    private PopupWindow currentPopup;

    public PopupWindowManager(Context context) {
        this.context = context;
    }

    /**
     * 显示列表选择弹窗（通用方法）
     *
     * @param anchor 锚点View
     * @param items 选项列表
     * @param listener 点击监听器
     */
    public void showListPopup(View anchor, List<String> items,
                              CustomSpinnerAdapter.OnItemClickListener listener) {
        if (items == null || items.isEmpty()) return;

        dismiss();

        View view = LayoutInflater.from(context).inflate(R.layout.layout_custom_spinner, null);
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(context, items);
        ListView listView = view.findViewById(R.id.spinner_listview);
        listView.setAdapter(adapter);

        currentPopup = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        currentPopup.setOutsideTouchable(true);
        currentPopup.setFocusable(true);
        currentPopup.setBackgroundDrawable(new ColorDrawable(
                context.getResources().getColor(R.color.color_system_rb_bg_normal)));

        // 设置点击外部自动关闭
        currentPopup.setOnDismissListener(() -> {
            // 可以在这里添加关闭时的回调
        });

        adapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (listener != null) {
                    listener.onItemClick(position);
                }
                // 注意：这里不自动dismiss，让调用方决定何时关闭
                // 调用方可以在回调中调用 dismiss()
            }

            @Override
            public void onItemLongClick(int position) {
                if (listener != null) {
                    listener.onItemLongClick(position);
                }
            }
        });

        currentPopup.showAsDropDown(anchor);
    }

    /**
     * 显示列表选择弹窗（带宽度控制）
     */
    public void showListPopup(View anchor, List<String> items, int width, int height,
                              CustomSpinnerAdapter.OnItemClickListener listener) {
        if (items == null || items.isEmpty()) return;

        dismiss();

        View view = LayoutInflater.from(context).inflate(R.layout.layout_custom_spinner, null);
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(context, items);
        ListView listView = view.findViewById(R.id.spinner_listview);
        listView.setAdapter(adapter);

        currentPopup = new PopupWindow(view, width, height, true);
        currentPopup.setOutsideTouchable(true);
        currentPopup.setFocusable(true);
        currentPopup.setBackgroundDrawable(new ColorDrawable(
                context.getResources().getColor(R.color.color_system_rb_bg_normal)));

        adapter.setOnItemClickListener(listener);
        currentPopup.showAsDropDown(anchor);
    }

    /**
     * 关闭当前弹窗
     */
    public void dismiss() {
        if (currentPopup != null && currentPopup.isShowing()) {
            currentPopup.dismiss();
            currentPopup = null;
        }
    }

    /**
     * 判断弹窗是否正在显示
     */
    public boolean isShowing() {
        return currentPopup != null && currentPopup.isShowing();
    }

    /**
     * 获取当前弹窗
     */
    public PopupWindow getCurrentPopup() {
        return currentPopup;
    }

    /**
     * 释放资源
     */
    public void release() {
        dismiss();
    }
}