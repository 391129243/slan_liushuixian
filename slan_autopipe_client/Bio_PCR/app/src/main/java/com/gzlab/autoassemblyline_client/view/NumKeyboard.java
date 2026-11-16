package com.gzlab.autoassemblyline_client.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gzlab.autoassemblyline_client.R;

public class NumKeyboard extends FrameLayout implements View.OnClickListener{
    public NumKeyboard(@NonNull Context context) {
        super(context);
        initView();
    }

    public NumKeyboard(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public NumKeyboard(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView(){
        View numView = LayoutInflater.from(this.getContext()).inflate(R.layout.layout_custom_num_keyboard, null);
        numView.findViewById(R.id.num_keyboard_1_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_2_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_3_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_4_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_5_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_6_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_7_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_8_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_9_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_0_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_point_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_delete_tv).setOnClickListener(this);
        numView.findViewById(R.id.num_keyboard_done_tv).setOnClickListener(this);
        LayoutParams params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        this.addView(numView, params);
    }
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.num_keyboard_0_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(0+"");
            }
        }else if(id == R.id.num_keyboard_1_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(1+"");
            }
        }else if(id == R.id.num_keyboard_2_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(2+"");
            }
        }else if(id == R.id.num_keyboard_3_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(3+"");
            }
        }else if(id == R.id.num_keyboard_4_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(4+"");
            }
        }else if(id == R.id.num_keyboard_5_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(5+"");
            }
        }else if(id == R.id.num_keyboard_6_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(6+"");
            }
        }else if(id == R.id.num_keyboard_7_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(7+"");
            }
        }else if(id == R.id.num_keyboard_8_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(8+"");
            }
        }else if(id == R.id.num_keyboard_9_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(9+"");
            }
        }else if(id == R.id.num_keyboard_point_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onNumListener(".");
            }
        }else if(id == R.id.num_keyboard_delete_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onDelListener();
            }
        }else if(id == R.id.num_keyboard_done_tv){
            if(null != onNumKeyBoardListener){
                onNumKeyBoardListener.onDownListener();
            }
        }

    }

    public void setOnNumKeyBoardListener(NumKeyBoardListener onNumKeyBoardListener) {
        this.onNumKeyBoardListener = onNumKeyBoardListener;
    }

    public NumKeyBoardListener onNumKeyBoardListener;

    //事件监听-接口
    public interface NumKeyBoardListener {
        //点击数字按钮时的监听回调
        void onNumListener(String num);
        //点击删除按钮时的监听回调
        void onDelListener();
        //点击完成按钮时的监听回调
        void onDownListener();
    }
}
