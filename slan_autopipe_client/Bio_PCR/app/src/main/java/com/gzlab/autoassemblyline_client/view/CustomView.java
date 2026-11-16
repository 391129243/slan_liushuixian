package com.gzlab.autoassemblyline_client.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gzlab.autoassemblyline_client.R;

public class CustomView extends LinearLayout {
    private TextView mImgOneView = null;
    private TextView mImgthreeView = null;
    private TextView mImgTwoView = null;
    private Context mContext;


    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.layout_custom_view ,this, true);
        mImgOneView = (TextView)findViewById(R.id.img_state_one);
        mImgTwoView = (TextView)findViewById(R.id.img_state_two);
        mImgthreeView = (TextView)findViewById(R.id.img_state_three);
        init(context,attrs);

    }

    private void init(Context context, AttributeSet attrs){
        TypedArray attrArray = context.obtainStyledAttributes(attrs, R.styleable.CustomView);

        int content_bg = attrArray.getResourceId(R.styleable.CustomView_content_img, R.drawable.shape_oval_blue_bg);

        mImgOneView.setBackgroundResource(content_bg);
        mImgTwoView.setBackgroundResource(content_bg);
        mImgthreeView.setBackgroundResource(content_bg);
        attrArray.recycle();
    }

    public void setStateBackgroud(int resId){
        mImgOneView.setBackgroundResource(resId);
        mImgTwoView.setBackgroundResource(resId);
        mImgthreeView.setBackgroundResource(resId);
    }




}
