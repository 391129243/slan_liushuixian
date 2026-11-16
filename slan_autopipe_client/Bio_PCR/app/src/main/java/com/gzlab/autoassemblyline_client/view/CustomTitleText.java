package com.gzlab.autoassemblyline_client.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gzlab.autoassemblyline_client.R;


public class CustomTitleText extends LinearLayout implements OnClickListener {

	private TextView mTitleTv = null;
    private TextView mContentTv = null;
    private Context mContext;
    private int content_color;
    private int title_color;
    private OnImageButtonClickListener mCtmImageButtonListener;
    public void setOnImageButtonClickListener(OnImageButtonClickListener customImgBtnClickListener){
        //在setter中把这个接口的实现赋值给这个loginview的上面定义的接口
         this.mCtmImageButtonListener = customImgBtnClickListener;

   }
   //接口
   public interface OnImageButtonClickListener{
       public void CustomImageButtonClicked(View v);//传的参数
   }

    public CustomTitleText(Context context, AttributeSet attrs) {
        super(context, attrs);  
        // TODO Auto-generated constructor stub  
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.layout_custom_titletext, this, true);
        mTitleTv = (TextView)findViewById(R.id.custom_title_text_title_tv);
        mContentTv = (TextView)findViewById(R.id.custom_title_text_content_tv);
        init(context,attrs);          
          
    }  
    
    private void init(Context context, AttributeSet attrs){
    	TypedArray attrArray = context.obtainStyledAttributes(attrs, R.styleable.CustomTitleText);
    	String title_text = attrArray.getString(R.styleable.CustomTitleText_title_text);
    	String content_text = attrArray.getString(R.styleable.CustomTitleText_content_text);
        //int font_size = attrArray.getDimension(R.styleable.CustomTitleText_content_font_size,)
        title_color = attrArray.getColor(R.styleable.CustomTitleText_title_text_color,
                getResources().getColor(R.color.white));

        content_color = attrArray.getColor(R.styleable.CustomTitleText_content_text_color,
                getResources().getColor(R.color.orange));


        mTitleTv.setTextColor(title_color);
        mTitleTv.setText(title_text);

        mContentTv.setText(content_text);
        mContentTv.setTextColor(content_color);
    	attrArray.recycle();
    }
    

    public void setContentText(String contentText){
        mContentTv.setText(contentText);
    }



    
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		if(null != mCtmImageButtonListener){
			mCtmImageButtonListener.CustomImageButtonClicked(v);
		}
	}   
    
}
