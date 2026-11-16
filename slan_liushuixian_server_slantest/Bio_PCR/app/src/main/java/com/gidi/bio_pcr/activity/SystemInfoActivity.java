package com.gidi.bio_pcr.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.base.BaseFragmentActivity;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.fragment.system.SystemController;
import com.gidi.bio_pcr.service.MyWindowManager;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.view.DateTimeView;

import org.greenrobot.eventbus.EventBus;

/**
 * 系统信息界面
 */
public class SystemInfoActivity extends BaseFragmentActivity implements View.OnClickListener{
    private RelativeLayout mTitleLayout;
    private DateTimeView mDateTimeView;
    private ImageView mBackImg;

    private LinearLayout mDeviceInfoTab;
    private LinearLayout mGeneralTab;
    private LinearLayout mWlanTab;
    private LinearLayout mSystemTab;
    private LinearLayout mFileInfoTab;
    private LinearLayout mExportFileTab;
    private LinearLayout mHelpTab;
    private SystemController fragmentController;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_system_info;
    }

    @Override
    protected void initView() {
        mTitleLayout = (RelativeLayout)findViewById(R.id.system_info_title_layout);
        mDateTimeView = (DateTimeView) mTitleLayout.findViewById(R.id.tab_time_ll);
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img) ;
        mDeviceInfoTab = (LinearLayout) findViewById(R.id.system_setting_tab_device_rb);
        mGeneralTab = (LinearLayout) findViewById(R.id.system_setting_tab_general_rb);
        mWlanTab = (LinearLayout) findViewById(R.id.system_setting_tab_wlan_rb);
        mSystemTab = (LinearLayout) findViewById(R.id.system_setting_tab_system_rb);
        mExportFileTab = (LinearLayout)findViewById(R.id.system_info_export_file_rb);
        mHelpTab = (LinearLayout) findViewById(R.id.system_info_tab_help_rb);
        mFileInfoTab = (LinearLayout) findViewById(R.id.system_info_tab_filebrowser_rb);
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
        mDeviceInfoTab.setOnClickListener(this);
        mGeneralTab.setOnClickListener(this);
        mWlanTab.setOnClickListener(this);
        mSystemTab.setOnClickListener(this);
        mFileInfoTab.setOnClickListener(this);
        mExportFileTab.setOnClickListener(this);
        mHelpTab.setOnClickListener(this);

    }

    @Override
    protected void handleMessage(Message msg) {
        switch (msg.what){
            case Constants.MSG_UPDATE_SYSTEM_TIME:
                mDateTimeView.setDateText(DateFormatUtil.getNowDate());
                mDateTimeView.setTimeText(DateFormatUtil.getNowTime());
                MessageEvent event = new MessageEvent();
                event.setMsgType(MsgType.MSG_UPDATE_SYSTEM_TIME);
                EventBus.getDefault().post(event);
                break;
        }

    }

    protected void initData() {

        fragmentController = SystemController.getInstance(SystemInfoActivity.this, R.id.system_info_frame_layout);
        setTabSelection(0);
        mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(MyWindowManager.isWindowShowing()){
//            MyWindowManager.removeSmallWindow(getApplicationContext());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragmentController.destoryController();
        fragmentController = null;

    }

    /**设置底部tab切换**/
    private void setTabSelection(int position){
        fragmentController.showFragment(position);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.system_setting_tab_device_rb) {
            fragmentController.showFragment(0);
            mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_setting_tab_general_rb) {
            fragmentController.showFragment(1);
            mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_setting_tab_wlan_rb) {
            fragmentController.showFragment(2);
            mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_setting_tab_system_rb) {
            if(PcrApplication.isAdmin){
                Intent intent = new Intent(SystemInfoActivity.this, SystemSettingActivity.class);
                startActivity(intent);
                return;
            }
            fragmentController.showFragment(3);
            mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_info_tab_help_rb){
            fragmentController.showFragment(4);
            mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.system_info_tab_filebrowser_rb) {


            String packageName = "com.estrongs.android.pop";
            if (isApkInstalled(SystemInfoActivity.this, packageName)) {
                Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
                LaunchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(LaunchIntent, 1);
                if (!MyWindowManager.isWindowShowing()) {
                    MyWindowManager.createSmallWindow(getApplicationContext());
                }
            } else {
                Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage("com.android.documentsui");
                LaunchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(LaunchIntent, 1);
                if (!MyWindowManager.isWindowShowing()) {
                    MyWindowManager.createSmallWindow(getApplicationContext());
                }
//                AssetUtils.installAssetApk(SystemInfoActivity.this,"file/ESFileExplorer.apk");
            }
        }else if(id== R.id.system_info_export_file_rb){
            Intent exportIntent = new Intent(SystemInfoActivity.this,ResultExportActivity.class);
            startActivity(exportIntent);
        }
    }

    public static boolean isApkInstalled(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

}
