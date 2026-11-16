package com.gidi.bio_pcr.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.R;

import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.base.BaseFragmentActivity;
import com.gidi.bio_pcr.config.activity.ShowConfigActivity;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.fragment.setting.SettingController;
import com.gidi.bio_pcr.service.MyWindowManager;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.CustomLargeDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class SystemSettingActivity extends BaseFragmentActivity implements View.OnClickListener{
    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    private LinearLayout mProcedureConfigTab;
    private LinearLayout mPcrConfigTab;

    private LinearLayout mTemplateTab;
    private LinearLayout mUserManagerTab;//用户管理
    private LinearLayout mLoginInfoTab;
    private LinearLayout mFileInfoTab;
    private LinearLayout mSystemResetTab;
    private LinearLayout mCopyResultTab;

    private LinearLayout mUpdateLinuxTab;
    private LinearLayout mSecretTab;
    private TextView mSecretTv;//copyLineTv,secretLineTv;
    private CustomLargeDialog secretDialog;


    private SettingController fragmentController;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_system_setting;
    }

    @Override
    protected void initView() {
        mTitleLayout = (RelativeLayout)findViewById(R.id.system_setting_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(R.string.system_info_setting);
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mSecretTv = (TextView)mTitleLayout.findViewById(R.id.secret_tv);
        mPcrConfigTab = (LinearLayout) findViewById(R.id.system_setting_tab_pcr_config_rb);
        mProcedureConfigTab = (LinearLayout) findViewById(R.id.system_setting_tab_procedure_rb);
        mUserManagerTab = (LinearLayout) findViewById(R.id.system_setting_tab_hardware_rb);
        mTemplateTab = (LinearLayout) findViewById(R.id.system_setting_tab_template_rb);
        mLoginInfoTab = (LinearLayout) findViewById(R.id.system_setting_tab_login_rb);
        mFileInfoTab = (LinearLayout) findViewById(R.id.system_setting_tab_filebrowser_rb);
        mSystemResetTab = (LinearLayout) findViewById(R.id.system_setting_tab_systemreset_rb);
		mSecretTab  = (LinearLayout) findViewById(R.id.system_setting_tab_secret_rb);
        mSecretTab.setVisibility(View.GONE);
        mCopyResultTab = (LinearLayout) findViewById(R.id.system_setting_tab_result_copy_rb);

        mUpdateLinuxTab= (LinearLayout) findViewById(R.id.system_setting_tab_updatelinux_rb);
    }

    @Override
    protected void initListener() {
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(MyWindowManager.isWindowShowing()){
                    MyWindowManager.removeSmallWindow(getApplicationContext());
                }
                finish();
            }
        });
        mSecretTv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mSecretTab.setVisibility(View.VISIBLE);

                mCopyResultTab.setVisibility(View.VISIBLE);

                return false;
            }
        });

        mPcrConfigTab.setOnClickListener(this);
        mProcedureConfigTab.setOnClickListener(this);
        mUserManagerTab.setOnClickListener(this);
        mTemplateTab.setOnClickListener(this);
        mLoginInfoTab.setOnClickListener(this);
        mFileInfoTab.setOnClickListener(this);
        mSystemResetTab.setOnClickListener(this);
		mSecretTab.setOnClickListener(this);
        mCopyResultTab.setOnClickListener(this);
        mUpdateLinuxTab.setOnClickListener(v -> {
            fragmentController.showFragment(3);
            mUpdateLinuxTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mPcrConfigTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mLoginInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mTemplateTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mUserManagerTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        });
    }

    @Override
    protected void handleMessage(Message msg) {

    }

    protected void initData() {
        fragmentController = SettingController.getInstance(SystemSettingActivity.this, R.id.system_setting_frame_layout);
        setTabSelection(0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        registerEventBus();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCopyResultTab.setVisibility(View.INVISIBLE);

        mSecretTab.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
        if(null != fragmentController){
            fragmentController.destoryController();;
            fragmentController = null;
        }
        if(secretDialog != null){
            secretDialog.dismiss();
            secretDialog = null;
        }
    }

    /**设置底部tab切换**/
    private void setTabSelection(int position){
        fragmentController.showFragment(position);
    }


    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()){
            /*case MsgType.MSG_EXPORT_TEMPLATE:
                PCRManager.importProject(this, new PCRManager.Pagelistener() {
                    @Override
                    public void goback() {
                        Log.i("PCRManager", "importProject");
                    }
                });
                break;
            case  MsgType.MSG_CREATE_TEMPLATE:
                PCRManager.createProject(this, new PCRManager.Pagelistener() {
                    @Override
                    public void goback() {
                        Log.i("PCRManager", "createProject");
                    }
                });
                break;*/

        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.system_setting_tab_pcr_config_rb) {
            fragmentController.showFragment(1);
            mPcrConfigTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mLoginInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mTemplateTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mUserManagerTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_setting_tab_hardware_rb) {
            fragmentController.showFragment(1);
            mPcrConfigTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mLoginInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mTemplateTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mUserManagerTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_setting_tab_template_rb) {
            fragmentController.showFragment(0);
            mPcrConfigTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mLoginInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mTemplateTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mUserManagerTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_setting_tab_login_rb) {
            fragmentController.showFragment(2);
            mPcrConfigTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mLoginInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mTemplateTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mUserManagerTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_setting_tab_filebrowser_rb) {// String packageName = "com.android.rk";
            String packageName = "com.estrongs.android.pop";
            if (isApkInstalled(SystemSettingActivity.this, packageName)) {
                Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                LaunchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(LaunchIntent);
                if (!MyWindowManager.isWindowShowing()) {
                    MyWindowManager.createSmallWindow(getApplicationContext());
                }
            } else {
                Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.android.documentsui");
                LaunchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(LaunchIntent);
                if (!MyWindowManager.isWindowShowing()) {
                    MyWindowManager.createSmallWindow(getApplicationContext());
                }
//                AssetUtils.installAssetApk(SystemSettingActivity.this,"file/ESFileExplorer.apk");
            }
        }else if (id == R.id.system_setting_tab_procedure_rb){
            Intent intent = new Intent(SystemSettingActivity.this, ShowConfigActivity.class);
            startActivity(intent);
        }else if (id == R.id.system_setting_tab_systemreset_rb){

            CustomDialog dialog = new CustomDialog.Builder(SystemSettingActivity.this)
                    .setTitle(getResources().getString(R.string.system_setting_dialog_title_reset))
                    .setMessage(getResources().getString(R.string.system_setting_dialog_message_reset))
                    .setNegativeButton(getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PreferenceUtil.getInstance(getApplicationContext())
                                    .setValueByName(SharedConstant.SYSTEM_INIT,false);
                            dialog.dismiss();
                        }
                    })
                    .create();

            dialog.show();
        }else if(id == R.id.system_setting_tab_secret_rb) {
            //弹出一个对话框
            showSecretDialog();
        }else if(id == R.id.system_setting_tab_result_copy_rb){
            // insertAllExperiments();
            Intent intent = new Intent(SystemSettingActivity.this, CopyResultActivity.class);
            startActivity(intent);
        }
    }

    public static boolean isApkInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean isShow = false;
    private boolean isSecretSetting = false;
    private void showSecretDialog(){
        if(null != secretDialog){
            secretDialog.dismiss();
            secretDialog = null;
        }
        if(null == secretDialog){

            secretDialog = new CustomLargeDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_secret_setting_layout, null);
            EditText editText = (EditText) view.findViewById(R.id.dialog_export_temp_name_et);
            LinearLayout settingLayout = (LinearLayout)view.findViewById(R.id.secret_setting_layout);
            TextView secretOkTv = (TextView)view.findViewById(R.id.secrete_set_ok_tv);
            TextView secretCancelTv = (TextView)view.findViewById(R.id.secrete_set_cancel_tv);
            isSecretSetting = PreferenceUtil.getInstance(getApplicationContext())
                    .getBooleanValue(SharedConstant.SECRET_SETTING,false);
            if(isSecretSetting){
                secretOkTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
                secretCancelTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            }else{
                secretCancelTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
                secretOkTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            }
            secretOkTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    isSecretSetting = true;
                    secretOkTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
                    secretCancelTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
                    if(isSecretSetting){
                        RNAAndPCRAsyncTask.retryCount_Value = 100;
                    }else{
                        RNAAndPCRAsyncTask.retryCount_Value = 20;
                    }
                }
            });
            secretCancelTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    isSecretSetting = false;
                    secretCancelTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
                    secretOkTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
                    if(isSecretSetting){
                        RNAAndPCRAsyncTask.retryCount_Value = 100;
                    }else{
                        RNAAndPCRAsyncTask.retryCount_Value = 20;
                    }
                }
            });
            settingLayout.setVisibility(View.INVISIBLE);
            CustomLargeDialog.Builder builder = new CustomLargeDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle("输入系统密码");
            builder.setPositiveButton(R.string.dialog_ok,  new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub

                    String password  = editText.getText().toString().trim();
                    if(!isShow){
                        if(StringUtil.isEmpty(password)){
                            Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.system_setting_toast_input_password),Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            if(!password.equals("a1234567890")){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.system_setting_toast_password_incorrect),Toast.LENGTH_LONG).show();
                                return;
                            }else{
                                //跳转设置
                                settingLayout.setVisibility(View.VISIBLE);
                                editText.setVisibility(View.GONE);
                                isShow = true;
                                return;
                            }
                        }
                    }else{
                        PreferenceUtil.getInstance(getApplicationContext())
                                .setValueByName(SharedConstant.SECRET_SETTING,isSecretSetting);
                    }

                    secretDialog.dismiss();
                    isShow = false;
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    secretDialog.dismiss();
                    secretDialog = null;
                    isShow =false;
                }
            });
            secretDialog = builder.create();
            secretDialog.show();
        }
    }
}
