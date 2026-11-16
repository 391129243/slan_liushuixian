package com.gzlab.autoassemblyline_client.view;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.gzlab.autoassemblyline_client.R;
import com.gzlab.autoassemblyline_client.util.StringUtil;

public class NumKeyboardDialog extends Dialog{

	private NumKeyboard numKeyboard;
	private EditText mNumKeyEt;
	private TextView mCancelTv;

	private Context context;
	public NumKeyboardDialog(@NonNull Context context) {
		super(context, R.style.loading_dialog);
		this.context = context;
		initView();
	}

	private void initView(){
		View view = LayoutInflater.from(context).inflate(R.layout.dialog_num_keyboard_layout, null);
		numKeyboard = (NumKeyboard) view.findViewById(R.id.dialog_numkeyboard_view);
		mNumKeyEt = (EditText) view.findViewById(R.id.dialog_keyboard_et);
		mCancelTv = (TextView) view.findViewById(R.id.dialog_keyboard_cancel_btn);
		numKeyboard.setOnNumKeyBoardListener(new NumKeyboard.NumKeyBoardListener() {
			@Override
			public void onNumListener(String num) {
				mNumKeyEt.setText(mNumKeyEt.getText().append(num));
				//将光标设置到尾部
				mNumKeyEt.setSelection(mNumKeyEt.getText().toString().length());
			}


			@Override
			public void onDelListener() {
				if (!StringUtil.isEmpty(mNumKeyEt.getText().toString())) {
					//从后往前逐个删除
					Editable editable = mNumKeyEt.getText();
					int length = editable.length();
					mNumKeyEt.setText(editable.delete(length - 1, length));
					//将光标设置到尾部
					mNumKeyEt.setSelection(mNumKeyEt.getText().length());
				}
			}

			@Override
			public void onDownListener() {
				String s = mNumKeyEt.getText().toString();
				if(listener != null){
					if(!StringUtil.isEmpty(s)){
						listener.onKeyBoardDone(s);
					}else {
						Toast.makeText(context,"输入框不能为空",Toast.LENGTH_LONG).show();
					}

				}
			}
		});
		super.setContentView(view);
	}

	@Override
	public void dismiss() {
		// TODO Auto-generated method stub
		View view = getCurrentFocus();
        if(view instanceof TextView){
            InputMethodManager mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            mInputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
 
        super.dismiss();
		
	}

	public View getEditText() {
		return mNumKeyEt;
	}
	//传递数据给输入框对象
	public NumKeyboardDialog setEditText(String s) {
		mNumKeyEt.setText(s);
		return this;
	}

	//取消键监听器
	public void setOnCanlceListener(View.OnClickListener listener) {
		mCancelTv.setOnClickListener(listener);
	}

	public interface  OnKeyBoardDoneListener{
		void onKeyBoardDone(String s);
	}

	private  OnKeyBoardDoneListener listener;

	public void setOnKeyBoardDoneListener(OnKeyBoardDoneListener listener){
		this.listener = listener;
	}

}
