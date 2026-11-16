package com.gidi.bio_pcr.fragment.setting;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.UserManagerAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.db.DatabaseHelper;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.db.LoginUser;
import com.gidi.bio_pcr.util.CommonUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.NoticeDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 普通用户管理，添加/删除普通用户账号
 */
public class UserManagerFragment extends BaseFragment implements View.OnClickListener{

    private ListView mLoginUserLv;
    private UserManagerAdapter userManagerAdapter;
    private EditText mAccountEt,mPasswordEt;
    private TextView mAddBtn,mQueryBtn;//新增和查询
    private List<LoginUser> loginUserList;
    private SearchAsyncTask searchAsyncTask;
    private NoticeDialog mNoticeDialog;

    private CustomDialog mEditDialog;

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_pcr_config;
    }

    @Override
    protected void initView(View rootView) {
        mAccountEt = (EditText) rootView.findViewById(R.id.account_manager_name_et);
        mPasswordEt = (EditText)rootView.findViewById(R.id.account_manager_password_et) ;
        mAddBtn = (TextView)rootView.findViewById(R.id.account_manager_add_tv);
        mLoginUserLv = (ListView)rootView.findViewById(R.id.account_manager_show_listview);
        userManagerAdapter = new UserManagerAdapter(getActivity().getApplicationContext());
        mLoginUserLv.setAdapter(userManagerAdapter);
        initData();
    }

    @Override
    protected void initListener() {
        mAddBtn.setOnClickListener(this);
        userManagerAdapter.setOnItemClickListener(new UserManagerAdapter.OnItemClickListener() {
            @Override
            public void onItemDelete(int item_position) {
                //删除
                LoginUser del_user = loginUserList.get(item_position);

                showDeleteDialog(getActivity().getResources().getString(R.string.dialog_manager_user_delete_message)
                        +del_user.getUserAccount()+"?",del_user,1);
            }

            @Override
            public void onItemEdit(int item_position) throws Exception {
                //编辑
                LoginUser edit_user = loginUserList.get(item_position);
                showEditDialog("",edit_user);
            }
        });
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.account_manager_add_tv){
            //添加账号
            addAccount();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelTask();
        if(null != loginUserList){
            loginUserList.clear();
            loginUserList = null;
        }
        if(null != mNoticeDialog){
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        if(null != mEditDialog){
            mEditDialog.dismiss();
            mEditDialog = null;
        }


    }

    private void initData(){
        loginUserList = new ArrayList<LoginUser>();
        queryAccount();
    }

    /**
     * 添加账号
     */
    private void addAccount(){
        try{
            String password = mPasswordEt.getText().toString().trim();
            String account = mAccountEt.getText().toString().trim();
            if(StringUtil.isEmpty(account)){
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.toast_login_user_account_empty),Toast.LENGTH_LONG).show();
            }else if(StringUtil.isEmpty(password)){
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.toast_login_user_password_empty),Toast.LENGTH_LONG).show();
            }else{
                if(loginUserList.size() == 12){
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.toast_login_user_limit),Toast.LENGTH_LONG).show();
                    return;
                }
                LoginUser user = new LoginUser();
                user.setUserAccount(account);
                String encryptPwd = CommonUtil.encryptAEC(password);
                user.setUserPassword(encryptPwd);
                if(!loginUserList.contains(user)){
                    //插入数据库
                    long result = LiteORMCtrlMgr.getInstance().insertLoginUser(user);
                    if(result >0){
                        Toast.makeText(getActivity().getApplicationContext()
                                ,getActivity().getResources().getString(R.string.toast_add_user_success)
                                ,Toast.LENGTH_LONG).show();
                        //重新查询更新列表
                        queryAccount();
                    }
                }else{
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.toast_login_user_same)
                            ,Toast.LENGTH_LONG).show();
                }
            }
        }catch (Exception e){
            e.getMessage();
        }


    }

    private void updateLoginUsers(LoginUser user,LoginUser updateUser){
        try {
            HashMap<String, Object> updateMap = new HashMap<String, Object>();
            updateMap.put(DatabaseHelper.COLUMN_USER_ACCOUNT, updateUser.getUserAccount());
            updateMap.put(DatabaseHelper.COLUMN_USER_PASSWORD,CommonUtil.encryptAEC(updateUser.getUserPassword()));
            int update_result = LiteORMCtrlMgr.getInstance().updateLoginUser(user,updateMap);
            if(update_result>0){
                queryAccount();
            }
        }catch (Exception e){
            e.getMessage();
        }

    }

    private void deleteLoginUser(LoginUser user){
        int result = LiteORMCtrlMgr.getInstance().deleteLoginUser(user);
        if(result >0){
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_export_toast_delete_success)
                    ,Toast.LENGTH_LONG).show();
            queryAccount();
        }
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

        private WeakReference<UserManagerFragment> weakfrag;

        public SearchAsyncTask(UserManagerFragment fragment){
            weakfrag = new WeakReference<UserManagerFragment>(fragment);
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            final UserManagerFragment mFragment = weakfrag.get();
            super.onPreExecute();
            if(null == mFragment){
                return;
            }
        }

        //后台进程的执行
        @Override
        protected List<LoginUser> doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            final UserManagerFragment mFragment = weakfrag.get();
            mFragment.loginUserList = LiteORMCtrlMgr.getInstance().getAllLoginUser();
            return  mFragment.loginUserList;
        }

        @Override
        protected void onPostExecute(List<LoginUser> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            final UserManagerFragment mFragment = weakfrag.get();
            if(null != mFragment.loginUserList){
                mFragment.userManagerAdapter.setLoginUserList(mFragment.loginUserList);
                if(mFragment.loginUserList.size() == 0){
                    Toast.makeText(mFragment.getActivity().getApplicationContext()
                            ,mFragment.getActivity().getResources().getString(R.string.result_ex_toast_query_no)
                            ,Toast.LENGTH_LONG).show();
                }
            }
            mFragment.cancelTask();
        }
    }

    private void showDeleteDialog(String message, final LoginUser user, int dialogType){
        if(null != mNoticeDialog ){
            mNoticeDialog.dismiss();
            mNoticeDialog =null;
        }
        if(null == mNoticeDialog){
            mNoticeDialog =new NoticeDialog(getActivity(),R.style.Dialog);
            mNoticeDialog.setMessage( message);
            mNoticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    mNoticeDialog.dismiss();
                    if(dialogType == 1){
                        deleteLoginUser(user);
                    }
                }
            });

            mNoticeDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                @Override
                public void onCancelClick() {
                    mNoticeDialog.dismiss();
                }
            });
            mNoticeDialog.show();
        }
    }

    private EditText mDialogAccountEt;
    private EditText mDialogPwdEt;
    private void showEditDialog(String message,final LoginUser user) throws Exception {
        if (null != mEditDialog) {
            mEditDialog.dismiss();
            mEditDialog = null;
        }
        if (null == mEditDialog) {
            mEditDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_edit_loginuser_layout, null);
            mDialogAccountEt = (EditText)view.findViewById(R.id.dialog_edit_account_et);
            mDialogPwdEt = (EditText)view.findViewById(R.id.dialog_edit_password_et);
            mDialogAccountEt.setText(user.getUserAccount());
            mDialogPwdEt.setText(CommonUtil.decryptAEC(user.getUserPassword()));
            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(view);

            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String account = mDialogAccountEt.getText().toString().trim();
                    String password = mDialogPwdEt.getText().toString().trim();
                    LoginUser updateLoginUser = new LoginUser();
                    updateLoginUser.setUserAccount(account);
                    updateLoginUser.setUserPassword(password);
                    try {
                        updateLoginUsers(user,updateLoginUser);
                    }catch (Exception e){
                        e.getMessage();
                    }

                    mEditDialog.dismiss();
                    mEditDialog = null;
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mEditDialog.dismiss();
                    mEditDialog = null;
                }
            });

            mEditDialog = builder.create();
            mEditDialog.setCanceledOnTouchOutside(true);
            mEditDialog.show();
        }
    }
}
