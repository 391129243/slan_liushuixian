package com.gzlab.autoassemblyline_client.util;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.gzlab.autoassemblyline_client.R;



public class LoadingDialog extends Dialog {
    TextView tvLoadingTx;
    ImageView ivLoading;
    private Handler handler = new Handler();
    private Animation animation;

    public LoadingDialog(Context context) {
        this(context, R.style.loading_dialog, "玩命加载中...");
    }

    public LoadingDialog(Context context, String string) {
        this(context, R.style.loading_dialog, string);
    }

    protected LoadingDialog(Context context, int theme, String string) {
        super(context, theme);
        setCanceledOnTouchOutside(false);//点击其他区域时   true  关闭弹窗  false  不关闭弹窗
        setContentView(R.layout.dialog_loading_layout);//加载布局
        tvLoadingTx = (TextView) findViewById(R.id.tv_loading_tx);
        tvLoadingTx.setText(string);
        ivLoading = (ImageView)findViewById(R.id.iv_loading);
        // 加载动画
        animation = AnimationUtils.loadAnimation(
                context, R.anim.loading_animation);

        // 使用ImageView显示动画
        getWindow().getAttributes().gravity = Gravity.CENTER;//居中显示
        getWindow().getAttributes().dimAmount = 0.5f;//背景透明度  取值范围 0 ~ 1
    }

    public void show() {
        super.show();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
               // ad.start();
                ivLoading.startAnimation(animation);
            }
        }, 200);
    }

    //关闭弹窗
    @Override
    public void dismiss() {
        super.dismiss();
    }
}
