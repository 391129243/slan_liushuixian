package com.gidi.bio_pcr.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;


public class CustomMiniImageText extends LinearLayout{

    private LinearLayout rootLayout;
	private TextView mImgView = null;
    private TextView mTextView = null;
    private Context mContext;
    private int content_color;
    private int img_content_color;
    private OnImageButtonClickListener mCtmImageButtonListener;
    public void setOnImageButtonClickListener(OnImageButtonClickListener customImgBtnClickListener){
        //在setter中把这个接口的实现赋值给这个loginview的上面定义的接口
         this.mCtmImageButtonListener = customImgBtnClickListener;

   }
   //接口
   public interface OnImageButtonClickListener{
       public void CustomImageButtonClicked(View v);//传的参数
    //   public void onImageLongClick(View v);
   }

    public CustomMiniImageText(Context context, AttributeSet attrs) {
        super(context, attrs);  
        // TODO Auto-generated constructor stub  
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.layout_custom_imagetext_mini, this, true);
        mImgView = (TextView)findViewById(R.id.img);
        mTextView = (TextView)findViewById(R.id.text);
        rootLayout = (LinearLayout)findViewById(R.id.parent_layout) ;
        rootLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != mCtmImageButtonListener){
                    mCtmImageButtonListener.CustomImageButtonClicked(v);
                }
            }
        });
        /*rootLayout.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(null != mCtmImageButtonListener){
                    mCtmImageButtonListener.onImageLongClick(view);
                }
                return false;
            }
        });*/
        init(context,attrs);          
          
    }
    String content_text;
    String image_text;

    public String getContent_text() {
        return content_text;
    }

    public void setContent_text(String content_text) {
        this.content_text = content_text;
    }

    public String getImage_text() {
        return image_text;
    }

    public void setImage_text(String image_text) {
        this.image_text = image_text;
    }

    private void init(Context context, AttributeSet attrs){
    	TypedArray attrArray = context.obtainStyledAttributes(attrs, R.styleable.CustomImageText);
        content_text = attrArray.getString(R.styleable.CustomImageText_content_text);
    	image_text = attrArray.getString(R.styleable.CustomImageText_image_content_text);
    	content_color = attrArray.getColor(R.styleable.CustomImageText_content_text_color, getResources().getColor(R.color.black));
    	int content_bg = attrArray.getResourceId(R.styleable.CustomImageText_content_img, R.drawable.shape_oval_blue_bg);
    	img_content_color = attrArray.getColor(R.styleable.CustomImageText_image_content_text_color, getResources().getColor(R.color.white));
    	mImgView.setBackgroundResource(content_bg);
    	mImgView.setTextColor(img_content_color);
        mImgView.setText(image_text);

    	mTextView.setText(content_text);
    	mTextView.setTextColor(content_color);


    	attrArray.recycle();
    }
    
    public void setImageBtnResoure(int resId, int color){
    	this.content_color = color;
    	mImgView.setBackgroundResource(resId);
    	mTextView.setTextColor(mContext.getResources().getColor(content_color));
    }

    public void setNormalState(){
        mImgView.setBackgroundResource(R.drawable.shape_oval_blue_bg);
        mTextView.setTextColor(mContext.getResources().getColor(R.color.black));
        mImgView.setTextColor(mContext.getResources().getColor(R.color.white));
    }

    public void setImageText(String str){
        mImgView.setText(str);
    }
    public void setImageText(int resId){
        mImgView.setText(resId);
    }

    public void setContentText(String str){
        mTextView.setText(str);
    }

    public void setContentText(int resId){
        mTextView.setText(resId);
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
