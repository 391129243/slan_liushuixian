package com.gidi.bio_pcr.activity;

import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.base.BaseFragmentActivity;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.fragment.result.ResultController;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.view.DateTimeView;


public class ResultActivity extends BaseFragmentActivity implements View.OnClickListener {
    private ResultController resultController;
    private LinearLayout mExperimentTab;
    private LinearLayout mWellResultTab;

    private ImageView mBackImg;
    private DateTimeView mDateTimeView;
    public boolean isSettingIn = false;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_result_1;
    }

    @Override
    protected void initView() {

        mExperimentTab = (LinearLayout)findViewById(R.id.result_experiment_tab);
        mWellResultTab = (LinearLayout)findViewById(R.id.result_wellresult_tab);
        mBackImg = (ImageView)findViewById(R.id.result_tab_back_img);
        mDateTimeView =(DateTimeView)findViewById(R.id.result_time_ll);
    }

    @Override
    protected void initListener() {
        mExperimentTab.setOnClickListener(this);
        mWellResultTab.setOnClickListener(this);
        mBackImg.setOnClickListener(this);
    }

    @Override
    protected void handleMessage(Message msg) {
        switch (msg.what){
            case Constants.MSG_UPDATE_SYSTEM_TIME:
                mDateTimeView.setDateText(DateFormatUtil.getNowDate());
                mDateTimeView.setTimeText(DateFormatUtil.getNowTime());
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        finish();
        if(null != resultController){
            resultController.destoryController();
            resultController = null;
        }
    }

    protected void initData() {
        isSettingIn = getIntent().getBooleanExtra("SettingIn",false);
        resultController = ResultController.getInstance(ResultActivity.this, R.id.result_frame_layout);
        setTabSelection(0);
        mExperimentTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
    }


    /**设置底部tab切换**/
    private void setTabSelection(int position){
        resultController.showFragment(position);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.result_experiment_tab) {
            setTabSelection(0);
            mExperimentTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mWellResultTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.result_wellresult_tab) {
            setTabSelection(1);
            mWellResultTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
            mExperimentTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
        } else if (id == R.id.result_tab_back_img) {
            finish();
        }
    }

}
