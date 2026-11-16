package com.paul.pipeline.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;

public class GreenRedView extends LinearLayout {
    private TextView mImgOneView = null;
    private TextView mImgthreeView = null;
    private TextView mImgTwoView = null;
    private Context mContext;


    public GreenRedView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.layout_custom_view ,this, true);
        mImgOneView = (TextView)findViewById(R.id.img_state_one);
        mImgTwoView = (TextView)findViewById(R.id.img_state_two);
        mImgthreeView = (TextView)findViewById(R.id.img_state_three);
        init(context,attrs);

    }
    int content_bg_green;
    int content_bg_red;
    private void init(Context context, AttributeSet attrs){
        TypedArray attrArray = context.obtainStyledAttributes(attrs, R.styleable.CustomView);

        content_bg_green = attrArray.getResourceId(R.styleable.CustomView_content_img, R.drawable.shape_oval_green_bg);
        content_bg_red = attrArray.getResourceId(R.styleable.CustomView_content_img, R.drawable.shape_oval_red_bg);
        mImgOneView.setBackgroundResource(content_bg_green);
        mImgTwoView.setBackgroundResource(content_bg_green);
        mImgthreeView.setBackgroundResource(content_bg_green);
        attrArray.recycle();
    }

//    public void setStateBackgroud(int resId){
//        mImgOneView.setBackgroundResource(resId);
//        mImgTwoView.setBackgroundResource(resId);
//        mImgthreeView.setBackgroundResource(resId);
//    }

    public void setStateBackgroud(boolean value){
        if(value) {
            if(content_bg_green!=0) {
                mImgOneView.setBackgroundResource(content_bg_green);
                mImgTwoView.setBackgroundResource(content_bg_green);
                mImgthreeView.setBackgroundResource(content_bg_green);
            }
        }else{
            if(content_bg_red!=0) {
                mImgOneView.setBackgroundResource(content_bg_red);
                mImgTwoView.setBackgroundResource(content_bg_red);
                mImgthreeView.setBackgroundResource(content_bg_red);
            }
        }
    }


}
