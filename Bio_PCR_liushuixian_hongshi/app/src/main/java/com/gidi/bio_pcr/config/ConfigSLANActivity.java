package com.gidi.bio_pcr.config;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.base.BaseFragmentActivity;
import com.gidi.bio_pcr.config.fragment.ConfigInfoFragment;
import com.gidi.bio_pcr.config.fragment.ConfigSLANFragment;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.FileUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置宏石的项目
 */
public class ConfigSLANActivity extends BaseFragmentActivity implements View.OnClickListener{
    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    private List<String> mSlanFileNames;
    private ConfigController configController;
    private ConfigInfoFragment configInfoFragment;
    private ConfigSLANFragment configSLANFragment;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_config_slan;
    }

    @Override
    protected void initView() {
        mTitleLayout = (RelativeLayout)findViewById(R.id.config_slan_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(R.string.config_project_title);
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
    }


    @Override
    protected void initListener() {
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void handleMessage(Message msg) {

    }


    protected void initData() {
        mSlanFileNames = new ArrayList<String>();
        File config_slan_dir = new File(FileUtils.CONFIG_SLAN_PATH);
        if(!config_slan_dir.exists()){
            config_slan_dir.mkdirs();
        }
        File[] files = new File(FileUtils.CONFIG_SLAN_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != mSlanFileNames) {
                    if(files[i].getName() != null && files[i].getName().contains(".prj")){
                        String name = files[i].getName().substring(0,files[i].getName().length()-4);
                        mSlanFileNames.add(name);
                        Collections.sort(mSlanFileNames);
                    }
                }
            }
        }
        if(null != configInfoFragment){
            configInfoFragment.setConfigProjectList(mSlanFileNames);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        configController = ConfigController.getInstance(ConfigSLANActivity.this, R.id.config_slan_frame_layout);
        configController.showFragment(0);
        configInfoFragment = (ConfigInfoFragment) configController.getFragment(0);
        configSLANFragment = (ConfigSLANFragment) configController.getFragment(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
        if(mSlanFileNames != null){
            mSlanFileNames.clear();
            mSlanFileNames = null;
        }
        if(null != configController){
            configController.destoryController();
            configController= null;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {
        switch (messageEvent.getMsgType()) {
            case MsgType.MSG_CREATE_SLAN:
                configController.showFragment(1);
                break;
            case MsgType.MSG_CONFIG_SLAN_INFO:
                initData();
                configController.showFragment(0);
                break;
            case MsgType.MSG_CONFIG_SLAN_RUN:
                configController.showFragment(2);
                break;
            case MsgType.MSG_CONFIG_SLAN_EDIT:
                String fileName = (String)messageEvent.getData();
                if(null != configSLANFragment){
                    configSLANFragment.setEditExFile(fileName,true);
                }
                configController.showFragment(1);
                break;
        }
    }
}
