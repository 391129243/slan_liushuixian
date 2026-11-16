package com.gidi.bio_pcr.user.activity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.db.LoginUser;
import com.gidi.bio_pcr.util.CommonUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录首页
 */
public class HomePageActivity extends BaseActivity {
    private EditText mAccountEt;//账号
    private EditText mPasswordEt;
    private Button mLoginBtn;
    private List<LoginUser> loginUsers;//获取所有的用户信息
    private SearchAsyncTask searchAsyncTask;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_user_homepage;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mLoginBtn = (Button) findViewById(R.id.user_login_btn);
        mAccountEt = (EditText) findViewById(R.id.homepage_login_account_et);
        mPasswordEt = (EditText) findViewById(R.id.homepage_login_pwd_et);
    }

    @Override
    protected void initListener() {
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    loginCheck();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    //查询账号密码的数据表，是否在数据表内
    @Override
    protected void initData() {
        loginUsers = new ArrayList<LoginUser>();
        queryAccount();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTask();
        if(null != loginUsers){
            loginUsers.clear();
            loginUsers = null;
        }
    }

    private void loginCheck() throws Exception {
        String password = mPasswordEt.getText().toString().trim();
        String account = mAccountEt.getText().toString().trim();
        if(account.equals("User") && password.equals("101010")){
            //可以进入
            Intent intent  = new Intent(HomePageActivity.this, UserLoginActivity.class);
          //  Intent intent  = new Intent(HomePageActivity.this, UserMainActivity.class);
            startActivity(intent);
            finish();
        }else{
            if(null != loginUsers && loginUsers.size()>0){
                for(LoginUser user : loginUsers){
                  if(isSameUserName(user)){
                      //有相同的用户名
                      if(isPasswordMatch(password,user)){
                          //有相同的密码.登录成功
                          Intent intent  = new Intent(HomePageActivity.this, UserLoginActivity.class);
                          startActivity(intent);
                          finish();
                      }else{
                          Toast.makeText(getApplicationContext(),
                                  getResources().getString(R.string.sys_setting_sys_password_error),Toast.LENGTH_LONG).show();
                      }
                  }else{
                      Toast.makeText(getApplicationContext(),getResources()
                              .getString(R.string.toast_login_user_account_error),Toast.LENGTH_LONG).show();
                  }
                }
            }
        }

    }

    private boolean isSameUserName(LoginUser user){
        String account = mAccountEt.getText().toString().trim();
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


    /**
     * 查询账号
     */
    private void queryAccount(){
        if(null == searchAsyncTask){
            searchAsyncTask = new SearchAsyncTask(this);
            searchAsyncTask.execute();
        }
    }

    private void cancelTask(){
        if(null != searchAsyncTask){
            searchAsyncTask.cancel(true);
            searchAsyncTask = null;
        }
    }

    public static class SearchAsyncTask extends AsyncTask<Void, Integer, List<LoginUser>> {

        private WeakReference<HomePageActivity> weakActivity;

        public SearchAsyncTask(HomePageActivity activity){
            weakActivity = new WeakReference<HomePageActivity>(activity);
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            final HomePageActivity mActivity = weakActivity.get();
            super.onPreExecute();
            if(null == mActivity){
                return;
            }
        }

        //后台进程的执行
        @Override
        protected List<LoginUser> doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            final HomePageActivity mActivity = weakActivity.get();
            mActivity.loginUsers = LiteORMCtrlMgr.getInstance().getAllLoginUser();
            return   mActivity.loginUsers;
        }

        @Override
        protected void onPostExecute(List<LoginUser> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            final HomePageActivity mActivity = weakActivity.get();
            mActivity.cancelTask();
        }
    }

}
