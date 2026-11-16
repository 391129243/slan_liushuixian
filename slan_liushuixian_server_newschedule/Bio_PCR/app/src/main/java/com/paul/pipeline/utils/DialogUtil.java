package com.paul.pipeline.utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.gidi.bio_pcr.R;

/**
 * 流水线模块统一 Dialog 工具类
 * 用于替代 Toast，提供更明显的用户提示
 */
public class DialogUtil {

    /**
     * 显示信息提示对话框（只有确定按钮）- 使用自定义样式
     *
     * @param context 上下文
     * @param message 提示消息
     */
    public static void showMessage(Context context, String message) {
        showMessage(context, "提示", message);
    }

    /**
     * 显示信息提示对话框（只有确定按钮）- 旧版本兼容
     * 保留原始实现供需要系统样式的地方使用
     */
    public static void showMessageSystemStyle(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        new AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    /**
     * 显示信息提示对话框（自定义标题）- 使用自定义样式
     *
     * @param context 上下文
     * @param title   标题
     * @param message 提示消息
     */
    public static void showMessage(Context context, String title, String message) {
        showCustomDialog(context, title, message, null);
    }

    /**
     * 显示自定义样式对话框
     *
     * @param context  上下文
     * @param title    标题
     * @param message  消息内容
     * @param listener 确定按钮监听器（可为null）
     */
    private static void showCustomDialog(Context context, String title, String message,
                                         final DialogInterface.OnClickListener listener) {
        if (context == null || message == null) {
            return;
        }

        try {
            // 创建自定义布局
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pipeline_message, null);
            TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
            TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
            TextView tvConfirm = dialogView.findViewById(R.id.tv_dialog_confirm);

            // 设置内容
            if (title != null) {
                tvTitle.setText(title);
            }
            tvMessage.setText(message);

            // 创建对话框
            final Dialog dialog = new Dialog(context, R.style.PipelineDialogTheme);
            dialog.setContentView(dialogView);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(true);

            // 确定按钮点击事件
            tvConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onClick(null, DialogInterface.BUTTON_POSITIVE);
                    }
                    dialog.dismiss();
                }
            });

            dialog.show();
        } catch (Exception e) {
            // 如果自定义样式失败，回退到系统样式
            e.printStackTrace();
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("确定", listener)
                    .show();
        }
    }

    /**
     * 显示错误提示对话框
     *
     * @param context 上下文
     * @param message 错误消息
     */
    public static void showError(Context context, String message) {
        showMessage(context, "错误", message);
    }

    /**
     * 显示警告提示对话框
     *
     * @param context 上下文
     * @param message 警告消息
     */
    public static void showWarning(Context context, String message) {
        showMessage(context, "警告", message);
    }

    /**
     * 显示确认对话框（带取消按钮）
     *
     * @param context  上下文
     * @param message  提示消息
     * @param listener 确定按钮监听器
     */
    public static void showConfirm(Context context, String message, DialogInterface.OnClickListener listener) {
        showConfirm(context, "确认", message, "确定", "取消", listener);
    }

    /**
     * 显示确认对话框（自定义标题和按钮文字）
     *
     * @param context      上下文
     * @param title        标题
     * @param message      提示消息
     * @param positiveText 确定按钮文字
     * @param negativeText 取消按钮文字
     * @param listener     确定按钮监听器
     */
    public static void showConfirm(Context context, String title, String message,
                                    String positiveText, String negativeText,
                                    DialogInterface.OnClickListener listener) {
        if (context == null || message == null) {
            return;
        }
        // 确认对话框仍使用系统样式，因为需要两个按钮
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, listener)
                .setNegativeButton(negativeText, null)
                .show();
    }
}
