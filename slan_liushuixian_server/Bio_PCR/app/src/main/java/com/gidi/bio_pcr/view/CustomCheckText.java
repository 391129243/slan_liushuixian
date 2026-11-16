package com.gidi.bio_pcr.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.provider.ContactsContract;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;

public class CustomCheckText extends LinearLayout {

    private TextView mImgView = null;
    private TextView mTextView = null;
    private Context mContext;
    private LinearLayout mParentLayout;
    private boolean isCheck = false;
    private int backgroud_bg;

    private OnCheckBtnClickListener mCtmImgBtnListener;
    public void setOnClickListener(OnCheckBtnClickListener customImgBtnClickListener){
        //在setter中把这个接口的实现赋值给这个loginview的上面定义的接口
        this.mCtmImgBtnListener = customImgBtnClickListener;

    }
    //接口
    public interface OnCheckBtnClickListener{
        void OnButtonClick(boolean isCheck);//传的参数
    }

    public CustomCheckText(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.layout_custom_check_text, this, true);
        mImgView = (TextView)findViewById(R.id.custom_check_img);
        mTextView = (TextView)findViewById(R.id.custom_check_tv);
        mParentLayout = (LinearLayout)findViewById(R.id.parent_check_layout);
        init(context,attrs);
        mParentLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setChecked();
            }
        });

    }
    public void setChecked(){
        if(isCheck){
            isCheck = false;
            mImgView.setBackgroundResource(R.drawable.item_chanel_uncheck);
        }else {
            isCheck = true;
            mImgView.setBackgroundResource(R.drawable.item_chanel_check);
        }
        if(null != mCtmImgBtnListener){
            mCtmImgBtnListener.OnButtonClick(isCheck);
        }
    }
    private void init(Context context, AttributeSet attrs){
        TypedArray attrArray = context.obtainStyledAttributes(attrs, R.styleable.CustomCheckText);
        String content_text = attrArray.getString(R.styleable.CustomCheckText_content_text);
        backgroud_bg = attrArray.getResourceId(R.styleable.CustomCheckText_background_img,
                R.drawable.shape_channel_fam_bg);
        int content_bg = attrArray.getResourceId(R.styleable.CustomCheckText_content_img, R.drawable.item_chanel_uncheck);
        int title_color = attrArray.getColor(R.styleable.CustomCheckText_content_text_color,
                mContext.getResources().getColor(R.color.white));
        float title_size = attrArray.getDimension(R.styleable.CustomCheckText_content_text_size,
                mContext.getResources().getDimension(R.dimen.pipemain_text12));
        mImgView.setBackgroundResource(content_bg);
        mTextView.setText(content_text);
        mTextView.setTextColor(title_color);
        mTextView.setTextSize(title_size);
        mParentLayout.setBackgroundResource(backgroud_bg);
        attrArray.recycle();
    }

    public void setBackgroundResource(int resId){
        backgroud_bg = resId;
        mParentLayout.setBackgroundResource(backgroud_bg);
    }

    public void setCheckState(int resId,boolean isCheck){
        this.isCheck = isCheck;
        mImgView.setBackgroundResource(resId);
    }

    /*设置文字接口*/
    public void setText(String str){
        mTextView.setText(str);
    }
    /*设置文字大小*/
    public void setTextSize(float size){
        mTextView.setTextSize(size);
    }

    /*设置文字大小*/
    public void setTextColor(int resId){
        mTextView.setTextColor(resId);
    }




}
