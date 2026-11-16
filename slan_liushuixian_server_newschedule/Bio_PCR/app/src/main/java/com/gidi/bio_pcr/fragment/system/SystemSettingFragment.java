package com.gidi.bio_pcr.fragment.system;

import android.content.Intent;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.activity.SystemSettingActivity;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.listener.KeyBoardActionListener;
import com.gidi.bio_pcr.view.SystemKeyBoardEditText;

public class SystemSettingFragment extends BaseFragment implements View.OnClickListener{
    private RelativeLayout mPwtLayout;
    private SystemKeyBoardEditText mSystemPwtEt;
    private Button mPwtBtn;

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_system_setting;
    }

    @Override
    protected void initView(View rootView) {

        mPwtLayout = (RelativeLayout)rootView.findViewById(R.id.system_setting_password_layout);
        mSystemPwtEt = (SystemKeyBoardEditText)rootView.findViewById(R.id.system_settting_password_et);
        mPwtBtn = (Button)rootView.findViewById(R.id.system_setting_password_btn);
    }

    @Override
    protected void initListener() {
        mSystemPwtEt.setOnKeyboardActionListener(new KeyBoardActionListener() {

            @Override
            public void onTextChange(Editable editable) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onComplete() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onClearAll() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onClear() {
                // TODO Auto-generated method stub
            }
        });
        mPwtBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.system_setting_password_btn) {
            String password = mSystemPwtEt.getText().toString().trim();
            String serialName = android.os.Build.SERIAL;
            serialName = "101010";
            if(serialName.equals(mSystemPwtEt.getText().toString())){
                PcrApplication.isAdmin =true;
                Intent intent = new Intent(getActivity(), SystemSettingActivity.class);
                startActivity(intent);
            }else{
                Toast.makeText(getActivity(), "密码错误，请重新输入", Toast.LENGTH_LONG).show();
                mSystemPwtEt.setText("");
                return;
            }
        }
    }
}
