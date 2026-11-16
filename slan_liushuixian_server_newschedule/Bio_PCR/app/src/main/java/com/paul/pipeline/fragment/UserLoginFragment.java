package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.paul.pipeline.utils.DialogUtil;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentUserLoginBinding;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.db.LoginUser;
import com.gidi.bio_pcr.util.CommonUtil;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.viewmodel.UserLoginViewModel;

import java.util.ArrayList;
import java.util.List;

public class UserLoginFragment extends Fragment {
    final static String TAG = "UserLoginFragment";
    private UserLoginViewModel mViewModel;

    public static UserLoginFragment newInstance() {
        return new UserLoginFragment();
    }

    FragmentUserLoginBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.i(TAG,"onCreateView");
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_user_login, container, false);
        initBinding();
        return binding.getRoot();
      //  return inflater.inflate(R.layout.fragment_user_login, container, false);
    }
    PipeMainViewModel mainviewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(UserLoginViewModel.class);
        mainviewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
    }
    private void initBinding() {
        binding.untouchview.setOnClickListener(v -> {
            //不实现，禁止外围区域触摸
        });
        binding.userLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    loginCheck();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
//        //默认绑定一个
//        binding.keyboardview.setEditText(binding.loginAccountEt);
//        binding.loginAccountEt.setmIKeyboardView(binding.keyboardview);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTask();
        if(null != loginUsers){
            loginUsers.clear();
            loginUsers = null;
        }
    }

    private List<LoginUser> loginUsers;//获取所有的用户信息

    public void setLoginUsers(List<LoginUser> loginUsers) {
        this.loginUsers = loginUsers;
    }

    //查询账号密码的数据表，是否在数据表内
    protected void initData() {
        loginUsers = new ArrayList<LoginUser>();
        queryAccount();
    }
    private void gotoMain(String username){
        PcrApplication.operator = username;
        mainviewModel.onNavClicked(ConstantData.NAV_SEIFCHECK);
    }
    private void loginCheck() throws Exception {
        String password = binding.loginPwdEt.getText().toString().trim();
        String account = binding.loginAccountEt.getText().toString().trim();
        if(account.equals("User") && password.equals("101010")){
            //可以进入
            gotoMain(account);
            return;
        }
        // 先找到账号匹配的用户，再校验密码；只提示一次。
        // 原来逐个用户比较，每个账号不同的用户都会弹一次"账号错误"，
        // 多用户时登录成功的同时还会留下一个错误弹窗在主页上
        LoginUser matched = null;
        if(null != loginUsers){
            for(LoginUser user : loginUsers){
                if(isSameUserName(user)){
                    matched = user;
                    break;
                }
            }
        }
        if(matched == null){
            DialogUtil.showMessage(getContext(), getResources().getString(R.string.toast_login_user_account_error));
        }else if(isPasswordMatch(password, matched)){
            //有相同的密码.登录成功
            gotoMain(account);
        }else{
            DialogUtil.showMessage(getContext(), getResources().getString(R.string.sys_setting_sys_password_error));
        }
    }

    private boolean isSameUserName(LoginUser user){
        String account = binding.loginAccountEt.getText().toString().trim();
        if(user.getUserAccount().equals(account)){
            return true;
        }
        return false;
    }

    private boolean isSameUserPassword(String inputPassword,LoginUser user){
        if(user.getUserPassword().equals(inputPassword)){
            return true;
        }
        return false;
    }

    /**
     * 验证用户输入的密码进行加密是否与数据库中存储的密码相同
     */
    private boolean isPasswordMatch(String inputPassword, LoginUser user)throws Exception {
        return CommonUtil.encryptAEC(inputPassword).equals(user.getUserPassword());
    }

    private SearchAsyncTask searchAsyncTask;
    /**
     * 查询账号
     */
    private void queryAccount(){
        if(null == searchAsyncTask){
            searchAsyncTask = new SearchAsyncTask();
            searchAsyncTask.execute();
        }
    }

    private void cancelTask(){
        if(null != searchAsyncTask){
            searchAsyncTask.cancel(true);
            searchAsyncTask = null;
        }
    }

    private class SearchAsyncTask extends AsyncTask<Void, Integer, List<LoginUser>> {

        public SearchAsyncTask(){}

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //后台进程的执行
        @Override
        protected List<LoginUser> doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            List<LoginUser> loginUsers = LiteORMCtrlMgr.getInstance().getAllLoginUser();
            return loginUsers;
        }

        @Override
        protected void onPostExecute(List<LoginUser> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            setLoginUsers(result);
            cancelTask();
        }
    }

}