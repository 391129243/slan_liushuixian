package com.gidi.bio_pcr.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.gidi.bio_pcr.R;

public class NoticeDialog extends Dialog {

    private Button okBtn;//确定按钮
    private Button cancelBtn;//取消按钮
    private TextView messageTv;//消息提示文本
    private String messageStr;//从外界设置的消息文本
    //确定文本和取消文本的显示的内容
    private String yesStr, noStr;
    private OnCancelClickListener onCancelClickListener;//取消按钮被点击了的监听器
    private OnOkClickListener onOkClickListener;//确定按钮被点击了的监听器

    public NoticeDialog(Context context, int themeResId) {
        super(context, themeResId);
    }


    public interface OnCancelClickListener{
        void onCancelClick();
    }

    public interface OnOkClickListener{
        void onConfirmClick();
    }

    public void setOnCancelOnclickListener(OnCancelClickListener listener){
        this.onCancelClickListener = listener;
    }

    public void setOnOkClickListener(OnOkClickListener listener){
        this.onOkClickListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_notice_layout);
        //空白处不能取消动画
        setCanceledOnTouchOutside(false);

        //初始化界面控件
        initView();

        //初始化界面数据
        initData();
        //初始化界面控件的事件
        initListener();

    }

    private void initView(){
        messageTv = findViewById(R.id.custom_dialog_message_tv);
        okBtn = findViewById(R.id.custom_dialog_ok_btn);
        cancelBtn = findViewById(R.id.custom_dialog_cancel_btn);
    }

    private void initData(){

        if (messageStr != null) {
            messageTv.setText(messageStr);
        }
        //如果设置按钮文字
        if (yesStr != null) {
            okBtn.setText(yesStr);
        }
        if (noStr != null) {
            cancelBtn.setText(noStr);
        }
    }

    private void initListener(){
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != onOkClickListener){
                    onOkClickListener.onConfirmClick();
                }
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != onCancelClickListener){
                    onCancelClickListener.onCancelClick();
                }
            }
        });
    }

    public void setMessage(String message) {
        messageStr = message;
    }
}
