package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.activity.CopyResultActivity;
import com.gidi.bio_pcr.activity.ResultExportActivity;
import com.gidi.bio_pcr.activity.SystemInfoActivity;
import com.gidi.bio_pcr.activity.SystemSettingActivity;
import com.gidi.bio_pcr.config.activity.ShowConfigActivity;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.databinding.FragmentSettingBinding;
import com.gidi.bio_pcr.fragment.setting.ConfigPCRFragment;
import com.gidi.bio_pcr.fragment.setting.FileOperateFragment;
import com.gidi.bio_pcr.fragment.setting.LoginInfoFragment;
import com.gidi.bio_pcr.fragment.system.GeneralSettingFragment;
import com.gidi.bio_pcr.fragment.system.HelpFragment;
import com.gidi.bio_pcr.fragment.system.SystemInfoFragment;
import com.gidi.bio_pcr.fragment.system.SystemSettingFragment;
import com.gidi.bio_pcr.fragment.system.WlanFragment;
import com.gidi.bio_pcr.service.MyWindowManager;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.CustomLargeDialog;
import com.paul.pipeline.fragment.viewmodel.SettingViewModel;

public class SettingFragment extends Fragment{

    private SettingViewModel mViewModel;

    public static SettingFragment newInstance() {
        return new SettingFragment();
    }
    FragmentSettingBinding binding;
    View rootView;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_setting, container, false);
        rootView = binding.getRoot();
        initBinding();
        return rootView;
      //  return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    private void initBinding() {
        initView();

        initFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(SettingViewModel.class);
        // TODO: Use the ViewModel
    }
    private LinearLayout mDeviceInfoTab;
    private LinearLayout mGeneralTab;
    private LinearLayout mWlanTab;
    private LinearLayout mSystemTab;
    private LinearLayout mFileInfoTab;
    private LinearLayout mExportFileTab;
    private LinearLayout mHelpTab;

    protected void initView() {
        mDeviceInfoTab = (LinearLayout) rootView.findViewById(R.id.system_setting_tab_device_rb);
        mGeneralTab = (LinearLayout) rootView.findViewById(R.id.system_setting_tab_general_rb);
        mWlanTab = (LinearLayout) rootView.findViewById(R.id.system_setting_tab_wlan_rb);
        mSystemTab = (LinearLayout) rootView.findViewById(R.id.system_setting_tab_system_rb);
        mExportFileTab = (LinearLayout)rootView.findViewById(R.id.system_info_export_file_rb);
        mHelpTab = (LinearLayout) rootView.findViewById(R.id.system_info_tab_help_rb);
        mFileInfoTab = (LinearLayout) rootView.findViewById(R.id.system_info_tab_filebrowser_rb);

        mDeviceInfoTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(0);
                mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
                mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            }
        });
        mGeneralTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(1);
                mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
                mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            }
        });
        mWlanTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(2);
                mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
                mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            }
        });
        mSystemTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(PcrApplication.isAdmin){
                    Intent intent = new Intent(getContext(), SystemSettingActivity.class);
                    startActivity(intent);
                    return;
                }
                showFragment(3);
                mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
                mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            }
        });
        mFileInfoTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String packageName = "com.estrongs.android.pop";
                if (isApkInstalled(getContext(), packageName)) {
                    Intent LaunchIntent = getContext().getPackageManager().getLaunchIntentForPackage(packageName);
                    LaunchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(LaunchIntent, 1);
                    if (!MyWindowManager.isWindowShowing()) {
                        MyWindowManager.createSmallWindow(getContext());
                    }
                } else {
                    Intent LaunchIntent = getContext().getPackageManager().getLaunchIntentForPackage("com.android.documentsui");
                    LaunchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(LaunchIntent, 1);
                    if (!MyWindowManager.isWindowShowing()) {
                        MyWindowManager.createSmallWindow(getContext());
                    }
//                AssetUtils.installAssetApk(SystemInfoActivity.this,"file/ESFileExplorer.apk");
                }
            }
        });
        mExportFileTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent exportIntent = new Intent(getContext(), ResultExportActivity.class);
                startActivity(exportIntent);
            }
        });
        mHelpTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(4);
                mHelpTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
                mSystemTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mGeneralTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mWlanTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
                mFileInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            }
        });

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
    SystemInfoFragment systemInfoFragment;
    GeneralSettingFragment generalSettingFragment;
    WlanFragment wlanFragment;
    SystemSettingFragment systemSettingFragment;
    HelpFragment helpFragment;
    private void initFragment(){
        systemInfoFragment =new SystemInfoFragment();
        generalSettingFragment=new GeneralSettingFragment();
        wlanFragment=new WlanFragment();
        systemSettingFragment=new SystemSettingFragment();
        helpFragment=new HelpFragment();
        getChildFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.system_info_frame_layout,systemInfoFragment)
                .add(R.id.system_info_frame_layout,generalSettingFragment)
                .add(R.id.system_info_frame_layout,wlanFragment)
                .add(R.id.system_info_frame_layout,systemSettingFragment)
                .add(R.id.system_info_frame_layout,helpFragment)
                .hide(helpFragment)
                .hide(systemSettingFragment)
                .hide(wlanFragment)
                .hide(generalSettingFragment)
                .show(systemInfoFragment)
                .commit();
        lastfragment = 0;
        mDeviceInfoTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
    }
    int lastfragment;
    private void showFragment(int value){
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        switch(lastfragment){
            case 0:
                ft.hide(systemInfoFragment);
                break;
            case 1:
                ft.hide(generalSettingFragment);
                break;
            case 2:
                ft.hide(wlanFragment);
                break;
            case 3:
                ft.hide(systemSettingFragment);
                break;
            case 4:
                ft.hide(helpFragment);
                break;
        }
        switch(value){
            case 0:
                ft.show(systemInfoFragment);
                break;
            case 1:
                ft.show(generalSettingFragment);
                break;
            case 2:
                ft.show(wlanFragment);
                break;
            case 3:
                ft.show(systemSettingFragment);
                break;
            case 4:
                ft.show(helpFragment);
                break;
        }
        lastfragment = value;
        ft.commit();
    }
}