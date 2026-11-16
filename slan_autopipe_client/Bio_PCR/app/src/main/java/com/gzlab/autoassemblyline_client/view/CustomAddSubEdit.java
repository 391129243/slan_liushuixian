package com.gzlab.autoassemblyline_client.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.gzlab.autoassemblyline_client.R;
import com.gzlab.autoassemblyline_client.util.StringUtil;

import java.math.BigDecimal;

public class CustomAddSubEdit extends LinearLayout implements View.OnClickListener {
    private Context mContext;
    private EditText valueEditText;
    private ImageView addImg;
    private ImageView subImg;

    private BigDecimal increment = BigDecimal.ONE;

    public static int MODE_X = 0;
    public static int MODE_Y = 1;
    private int Mode;

    public CustomAddSubEdit(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater.from(context).inflate(R.layout.layout_custom_add_sub_edittext, this, true);

        valueEditText = (EditText) findViewById(R.id.custom_edit);
        subImg = (ImageView) findViewById(R.id.custom_edit_sub_img);
        addImg = (ImageView) findViewById(R.id.custom_edit_add_img);

        subImg.setOnClickListener(this);
        addImg.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        String contentNum = valueEditText.getText().toString();
        if(id == R.id.custom_edit_sub_img){
            decreaseValue();
        }else if(id == R.id.custom_edit_add_img){
            increaseValue();
        }
    }



    /**
     * 获取编辑框信息
     */
    /**获取编辑框的内容**/
    public String getEditText(){
        String text = "";
        if(!StringUtil.isEmpty(valueEditText.getText().toString().trim())){
            text = valueEditText.getText().toString().trim();
        }
        return text;
    }

    private void decreaseValue() {
        try {
            String input = valueEditText.getText().toString().trim();
            if (!input.isEmpty()) {
                BigDecimal currentValue = new BigDecimal(input);
                BigDecimal newValue = currentValue.subtract(increment);
                setEditTextValue(newValue.toString());
            }
        } catch (NumberFormatException e) {
            String input = valueEditText.getText().toString().trim();
            setEditTextValue(input);
        }
    }

    // 增加数值方法
    private void increaseValue() {
        try {
            String input = valueEditText.getText().toString().trim();
            if (!input.isEmpty()) {
                BigDecimal currentValue = new BigDecimal(input);
                BigDecimal newValue = currentValue.add(increment);
                setEditTextValue(newValue.toString());
            }
        } catch (NumberFormatException e) {
            String input = valueEditText.getText().toString().trim();
            setEditTextValue(input);

        }
    }

    // 设置数值方法
    public void setValue(BigDecimal value) {
        valueEditText.setText(value.toString());
        // 将光标移动到文本末尾
        valueEditText.setSelection(valueEditText.getText().length());
    }

    // 更新文本数据方法
    public void setEditTextValue(String value) {
        valueEditText.setText(value);
        // 将光标移动到文本末尾
        valueEditText.setSelection(valueEditText.getText().length());
    }
}
