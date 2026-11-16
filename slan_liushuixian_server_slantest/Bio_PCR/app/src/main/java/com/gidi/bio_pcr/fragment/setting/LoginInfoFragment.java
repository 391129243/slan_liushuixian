package com.gidi.bio_pcr.fragment.setting;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.util.PreferenceUtil;

public class LoginInfoFragment extends BaseFragment {
    private EditText mAccountEt;
    private EditText mPasswordEt;
    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_login_info;
    }

    @Override
    protected void initView(View rootView) {
        mAccountEt = (EditText)rootView.findViewById(R.id.login_info_account_et);
        mPasswordEt= (EditText)rootView.findViewById(R.id.login_info_pwd_et);
    }

    @Override
    protected void initListener() {
        mAccountEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mPasswordEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String account = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.LOGIN_ACCOUNT,"User");
        String password = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.LOGIN_PASSWORD,"123456");
        mAccountEt.setText(account);
        mPasswordEt.setText(password);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveLoginInfo();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void hideSoftInputMethod() {
        InputMethodManager im = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (getActivity().getCurrentFocus() != null && getActivity().getCurrentFocus().getWindowToken() != null) {
            im.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private void saveLoginInfo(){
        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.LOGIN_ACCOUNT,mAccountEt.getText().toString().trim());
        PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .setValueByName(SharedConstant.LOGIN_PASSWORD,mPasswordEt.getText().toString().trim());
    }


}
