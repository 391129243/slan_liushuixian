package com.gidi.bio_pcr.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.ActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.WarningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.bioway.activity.BiowayActivity;
import com.gidi.bio_pcr.bioway.activity.BiowaySingleActivity;
import com.gidi.bio_pcr.config.activity.ShowConfigActivity;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.user.activity.UserMainActivity;
import com.gidi.bio_pcr.util.CommonUtil;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomView;
import com.gidi.bio_pcr.view.WarnDialog;
import com.winters.boweilibrary.BoWei.BoWeiService;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.ref.WeakReference;


/**
 * 登录后进入
 * 主界面
 * 显示版本
 * 时间
 */
public class MainActivity extends BaseActivity implements View.OnClickListener{

    private LinearLayout mDetectTab;
    private LinearLayout mRNATab;//核酸提取
    private LinearLayout mPCRTab;//pcr
    private LinearLayout mSystemInfoTab;
    private LinearLayout mResultTab;
    private RelativeLayout mDoorTab;
    private RelativeLayout mCloseDoorTab;
    private RelativeLayout mResetTab;
    private RelativeLayout mTemplateTab;
    private TextView mVersionTv;
    private TextView mDateTv;
    private TextView mTimeTv;

    private LinearLayout mTopBarLayout;
    private CustomView mPcrStateTv;
    private CustomView mRNAStateTv;

    private TextView mAlarmMsgTv;
    private ImageView mVolumeImg,mBackImg;
    private UIHandler mUIHandler;
    private AudioManager mAudioManager;
    private int alarmVolume;
    private boolean isMute = false;

    private String PCR_RUNNING_TEMPLATE;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_main;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

        mRNATab  = (LinearLayout) findViewById(R.id.main_full_rna_tab);
        mPCRTab = (LinearLayout) findViewById(R.id.main_full_pcr_tab);
        mDetectTab = (LinearLayout) findViewById(R.id.main_full_procee_tab);
        mResultTab = (LinearLayout) findViewById(R.id.main_tab_record_layout);
        mSystemInfoTab = (LinearLayout)findViewById(R.id.main_tab_info_layout);
        mDoorTab = (RelativeLayout)findViewById(R.id.main_tab_opendoor_layout);
        mCloseDoorTab =  (RelativeLayout)findViewById(R.id.main_tab_closedoor_layout);
        mResetTab = (RelativeLayout)findViewById(R.id.main_tab_reset_layout);
        mTemplateTab = (RelativeLayout)findViewById(R.id.main_tab_template_edit_layout);
        mVersionTv = (TextView)findViewById(R.id.main_version_tv);
        mDateTv = (TextView)findViewById(R.id.main_date_tv);
        mTimeTv = (TextView)findViewById(R.id.main_time_tv);
        mTopBarLayout = (LinearLayout)findViewById(R.id.main_top_state_layout);
        mPcrStateTv = (CustomView)findViewById(R.id.main_top_pcr_state_img);
        mRNAStateTv = (CustomView)findViewById(R.id.main_top_rna_state_img) ;
        mAlarmMsgTv = (TextView)findViewById(R.id.main_top_alarm_msg_tv);
        mVolumeImg = (ImageView)findViewById(R.id.main_top_sound_tv);
        mBackImg = (ImageView) findViewById(R.id.main_top_back_img);
    }

    @Override
    protected void initListener() {
        mPCRTab.setOnClickListener(this);
        mRNATab.setOnClickListener(this);
        mDetectTab.setOnClickListener(this);
        mResultTab.setOnClickListener(this);
        mSystemInfoTab.setOnClickListener(this);
        mDoorTab.setOnClickListener(this);
        mCloseDoorTab.setOnClickListener(this);
        mResetTab.setOnClickListener(this);
        mVolumeImg.setOnClickListener(this);
        mTemplateTab.setOnClickListener(this);
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, UserMainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void initData() {
        mUIHandler = new UIHandler(this);
//        String version = CommonUtil.getAppVersionName(getApplicationContext());
        String version = "V1.0.21_bowei";
        mVersionTv.setText("Version:" + version);
        String nowDate = DateFormatUtil.getNowDate();
        mDateTv.setText(nowDate);
        String time = DateFormatUtil.getNowTime();
        mTimeTv.setText(time);
        mAudioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        alarmVolume = (int)((maxVolume * 4)/5);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        initData();
        checkConnectState();
        initUpdateSysTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PcrApplication.experimentType = 0;//默认全流程
        PcrApplication.ex_startTime = "";
        checkSdcardSize();
        initVolumeState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
        stopRNARunning();
        finish();
        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        mAudioManager = null;
    }

    private void checkConnectState(){

        mTopBarLayout.setVisibility(View.VISIBLE);
        if(PcrApplication.is_RNA_connect){
            mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }

        if(PcrApplication.is_SLAN_PCR_connect){
            mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
    }

    /**
     * 检查sdcard的容量
     */
    private void checkSdcardSize(){
        long availableSdcardSize = CommonUtil.getAvailableSDcardSize();
       Log.i("MainActivity","availableSdcardSize"+ availableSdcardSize);
        if(availableSdcardSize < 1024*1024*1024L){
            //弹出提示对话框
            showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),getResources().getString(R.string.dialog_message_insufficient_storage_space));
            mAlarmMsgTv.setText("E6-"+getResources().getString(R.string.alarm_disk_full));
        }
    }

    private void initVolumeState(){

        int currentVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(0 == currentVol){
            mVolumeImg.setImageResource(R.drawable.icon_mute);
            isMute = true;
        }else{
            mVolumeImg.setImageResource(R.drawable.icon_no_mute);
            isMute = false;
        }
    }


    //定时更新时间
    private void initUpdateSysTime() {
        mUIHandler.postDelayed(runnable, 1000);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //TODO:延时要干的其他事情
            mUIHandler.sendEmptyMessage(Constants.MSG_UPDATE_SYSTEM_TIME);
            mUIHandler.postDelayed(runnable, 1000);
        }
    };





    public static class UIHandler extends Handler {

        WeakReference<MainActivity> mActivityReference;
        public UIHandler(MainActivity mActivity) {
            mActivityReference = new WeakReference<MainActivity>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final MainActivity mActivity = mActivityReference.get();
            if (null == mActivity) {
                return;
            }

            switch (msg.what) {
                case Constants.MSG_UPDATE_SYSTEM_TIME:
                    mActivity.mDateTv.setText(DateFormatUtil.getNowDate());
                    mActivity.mTimeTv.setText(DateFormatUtil.getNowTime());
                    break;
            }
        }
    }


    @Override
    public void onClick(View v) {
        PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");

        int id = v.getId();
        if(id == R.id.main_full_rna_tab){
//            if(!PcrApplication.is_RNA_connect|| !PcrApplication.is_SLAN_PCR_connect){
//                showNoticeDialog(getResources().getString(R.string.dialog_title_warning),
//                        getResources().getString(R.string.dialog_message_equipment_module_problem));
//                return;
//            }
//            if(PcrApplication.rNARunningMessage == null||!PCR_RUNNING_TEMPLATE.equals("")){
//                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_abnormal),
//                        getResources().getString(R.string.dialog_message_reset_first));
//            }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")) {

                PcrApplication.experimentType = 1;
                PreferenceUtil.getInstance(getApplicationContext())
                        .setValueByName(SharedConstant.EXPERIMENT_TYPE,PcrApplication.experimentType);
                Intent singleRna = new Intent(MainActivity.this, BiowaySingleActivity.class);
                startActivity(singleRna);
                finish();
//            }else{
//                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_running),
//                        getResources().getString(R.string.dialog_message_reset_first));
//            }

        }else if(id == R.id.main_full_pcr_tab){
//            if(!PcrApplication.is_RNA_connect|| !PcrApplication.is_SLAN_PCR_connect){
//                showNoticeDialog(getResources().getString(R.string.dialog_title_warning)
//                        ,getResources().getString(R.string.dialog_message_equipment_module_problem));
//                return;
//            }
//            if(PcrApplication.rNARunningMessage == null||!PCR_RUNNING_TEMPLATE.equals("")){
//                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_abnormal),
//                        getResources().getString(R.string.dialog_message_reset_first));
//            }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")) {

                PcrApplication.experimentType = 2;
                PreferenceUtil.getInstance(getApplicationContext())
                        .setValueByName(SharedConstant.EXPERIMENT_TYPE,PcrApplication.experimentType);
                Intent singlePcr = new Intent(MainActivity.this, BiowaySingleActivity.class);
                startActivity(singlePcr);
                finish();
//            }else{
//                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_running),
//                        getResources().getString(R.string.dialog_message_reset_first));
//            }

        }else if(id == R.id.main_full_procee_tab){
//            if(!PcrApplication.is_RNA_connect||!PcrApplication.is_SLAN_PCR_connect ){
//                showNoticeDialog(getResources().getString(R.string.dialog_title_warning)
//                        ,getResources().getString(R.string.dialog_message_equipment_module_problem));
//                return;
//            }
//            if(PcrApplication.rNARunningMessage == null||!PCR_RUNNING_TEMPLATE.equals("")){
//                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_abnormal),
//                        getResources().getString(R.string.dialog_message_reset_first));
//            }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")) {
                PcrApplication.experimentType = 0;
                PreferenceUtil.getInstance(getApplicationContext())
                        .setValueByName(SharedConstant.EXPERIMENT_TYPE,PcrApplication.experimentType);
                Intent detectIntent = new Intent(MainActivity.this, BiowayActivity.class);
                startActivity(detectIntent);
                finish();
//            }else{
//                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_running),
//                        getResources().getString(R.string.dialog_message_reset_first));
//            }
        }else if(id == R.id.main_tab_record_layout){
            Intent recordIntent = new Intent(MainActivity.this, ResultActivity.class);
            recordIntent.putExtra("SettingIn",true);
            startActivity(recordIntent);
        }else if(id ==  R.id.main_tab_info_layout){
            Intent infoIntent = new Intent(MainActivity.this, SystemInfoActivity.class);
            startActivity(infoIntent);
        }else if(id == R.id. main_tab_template_edit_layout){
            Intent templateIntent = new Intent(MainActivity.this, ShowConfigActivity.class);
            startActivity(templateIntent);
        }else if (id == R.id.main_tab_opendoor_layout) {//                downanDopen_Error();
            if(PcrApplication.rNARunningMessage == null||!PCR_RUNNING_TEMPLATE.equals("")){
                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_abnormal),
                        getResources().getString(R.string.dialog_message_reset_first));
            }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")){
                openDoor();
            }else{
                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_running),
                        getResources().getString(R.string.dialog_message_reset_first));
            }
        } else if (id == R.id.main_tab_closedoor_layout) {
            if(PcrApplication.rNARunningMessage == null||!PCR_RUNNING_TEMPLATE.equals("")){
                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_abnormal),
                        getResources().getString(R.string.dialog_message_reset_first));
            }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")) {
                closeDoor();
            }else{
                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_running),
                        getResources().getString(R.string.dialog_message_reset_first));
            }
        }else if(id == R.id.main_tab_reset_layout){

            showResetNoticeDialog(getResources().getString(R.string.dialog_title_exception_reset),
                    getResources().getString(R.string.dialog_message_exception_reset));

        }else if(id == R.id.main_top_sound_tv){
            if(isMute){
                isMute = false;
            }else {
                isMute = true;
            }
            setVolumeState(isMute);
        }
    }

    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()){
            case MsgType.MSG_RNA_CONNECT_STATE:
                Log.e("MSG_RNA_CONNECT_STATE", "is_RNA_connect"+ PcrApplication.is_RNA_connect);
                if(PcrApplication.is_RNA_connect){
                    mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    if(PcrApplication.is_SLAN_PCR_connect){
                        mAlarmMsgTv.setText("");
                    }else {
                        mAlarmMsgTv.setText("E4-"+getResources().getString(R.string.e4_alarm_pcr_hardware_disconnect));
                    }
                }else{
                    mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
                    mAlarmMsgTv.setText("E3-"+ getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                }
                break;
            case MsgType.MSG_PCR_CONNECT_STATE:
                Log.e("MSG_PCR_CONNECT_STATE", "is_SLAN_PCR_connect"+ PcrApplication.is_SLAN_PCR_connect);
                if(PcrApplication.is_SLAN_PCR_connect){
                    mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    if(PcrApplication.is_RNA_connect){
                        mAlarmMsgTv.setText("");
                    }else{
                        mAlarmMsgTv.setText("E3-"+ getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                    }
                }else {
                    mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
                    if(!PcrApplication.is_RNA_connect){
                        mAlarmMsgTv.setText(getResources().getString(R.string.e3_e4_alarm_both_hardware_disconnect));
                    }else{
                        mAlarmMsgTv.setText("E4-"+getResources().getString(R.string.e4_alarm_pcr_hardware_disconnect));
                    }
                }

                break;

        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
        if(procedureFinishEvent.getTemplateName().contains("reset")||
                procedureFinishEvent.getTemplateName().contains("opendoor")||
                procedureFinishEvent.getTemplateName().contains("downandopen")||
                procedureFinishEvent.getTemplateName().contains("closedoor")) {
            if (procedureFinishEvent.getStatus().equals("finish")) {
                hideProgressDialog();
            } else if (procedureFinishEvent.getStatus().equals("finish error")) {
                hideProgressDialog();
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.dialog_title_check_equipment)
                        , getResources().getString(R.string.dialog_message_command_error));
            }
            stopRNARunning();
        }
        if(procedureFinishEvent.getTemplateName().contains("reset")){
            if (procedureFinishEvent.getStatus().equals("finish")) {
                resetPcrRecMark();
                PcrApplication.rNARunningMessage.setStatus("free");
                if(PcrApplication.is_SLAN_PCR_connect) {
                    //SLANService.INSTANCE.stopExperiment(PcrApplication.devices.get(0).getSerialNo(), (byte) 0x01);
                    BoWeiService.getInstance().stopExperiment((byte) 0x01);
                }
            }
        }
    }


    /**
     * 系统复位
     */

    private void resetSystem(){
        if(PcrApplication.is_RNA_connect){
            ////异步全流程
            resetPcrRecMark();
            if(null != PcrApplication.rNARunningMessage){
                String rna_step_num = PcrApplication.rNARunningMessage.getNO();
                int flag = 0;
                String reset_filePath = "";
                if(!StringUtil.isEmpty(rna_step_num)) {
                    String[] mRNA_running = rna_step_num.split("\\|",-1);
                    if (mRNA_running.length >= 5) {
                        flag = NumberUtils.StringtoInt(mRNA_running[3]);
                    }
                }
                if(flag == 1){
                    reset_filePath = FileUtils.COMMON_HEX_PATH + "99-reset.hex";
                }else if(flag == 0){
                    reset_filePath = FileUtils.COMMON_HEX_PATH + "95-reset_exception.hex";
                }
                File resetfile = new File(reset_filePath);
                boolean result = startAysnTask(resetfile);
                if(result) {
                    showProgressDialog(getResources().getString(R.string.dialog_title_wait),
                            getResources().getString(R.string.dialog_message_reset));
                }else{
                    showAlarm();
                    showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                            getResources().getString(R.string.dialog_message_reset_error));
                    mAlarmMsgTv.setText("E10" + getResources().getString(R.string.dialog_message_reset_error));
                }
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
        }
    }



    /**
     * 开舱门
     */
    private void downanDopen_Error(){
        if(PcrApplication.is_RNA_connect){
            String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "95-downforerror.hex";
            File downandopenfile = new File(downandopen_filePath);
            boolean result = startAysnTask(downandopenfile);
            if(result){
                showProgressDialog(getResources().getString(R.string.dialog_title_wait)
                        ,getResources().getString(R.string.dialog_message_opendoor));
            }else{
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                        getResources().getString(R.string.dialog_message_open_error));
                mAlarmMsgTv.setText("E9 " +  getResources().getString(R.string.dialog_message_open_error));

            }

        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 开舱门
     */
    protected void openDoor(){
        if(PcrApplication.is_RNA_connect){
            String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "96-opendoor.hex";
            File downandopenfile = new File(downandopen_filePath);
            boolean result = startAysnTask(downandopenfile);
            if(result){
                showProgressDialog(getResources().getString(R.string.dialog_title_wait)
                        ,getResources().getString(R.string.dialog_message_opendoor));
            }else{
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                        getResources().getString(R.string.dialog_message_open_error));
            }

        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
        }
    }


    /**
     * 开舱门
     */
    protected void closeDoor(){
        if(PcrApplication.is_RNA_connect){
            String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "98-closedoor.hex";
            File downandopenfile = new File(downandopen_filePath);
            boolean result = startAysnTask(downandopenfile);
            if(result){
                showProgressDialog(getResources().getString(R.string.dialog_title_wait)
                        ,getResources().getString(R.string.dialog_message_closedoor));
            }else{
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                        getResources().getString(R.string.dialog_message_close_error));
            }

        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RNARunningMessage rNARunningMessage) {
        Log.i("RNA", "RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
        Log.i("RNA", "RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getNO());

        if(rNARunningMessage != null )
        {
            LoginActivity.rnaHeartBeatCount = 0;
            PcrApplication.is_RNA_connect = true;
            PcrApplication.is_RecRunningMesaage = true;
            PcrApplication.rNARunningMessage = rNARunningMessage;
        }
    }

    int warning = 1;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WarningMessage warningMessage) {
        String json = GsonUtil.toJson(warningMessage, true);
        Log.e("WarningMessage", "警报消息 :"  + warning++ + json);
        mAlarmMsgTv.setText(warning + ":" + warningMessage.getAlarm() + "-" +
                warningMessage.getMotorID() + "-" + warningMessage.getStatus() + "-" + warningMessage.getDegree());

    }
    int actionfinish = 0 ;
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ActionDoneMessage actionDoneMessage) {

        String json = GsonUtil.toJson(actionDoneMessage, true);
        Log.e("WarningMessage", "完成消息 :"+ ++actionfinish + json);

    }

    private void setVolumeState(boolean isMute){
        if(isMute){
//            mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, AudioManager.FLAG_PLAY_SOUND);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
            mVolumeImg.setImageResource(R.drawable.icon_mute);
            //如果警报
            clearAlarm();
        }else {
//            mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, alarmVolume, AudioManager.FLAG_PLAY_SOUND);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, alarmVolume, AudioManager.FLAG_PLAY_SOUND);
            mVolumeImg.setImageResource(R.drawable.icon_no_mute);
        }
    }

    public void showResetNoticeDialog(String title, String message){
        if(null != mConfirmDialog){
            mConfirmDialog.dismiss();
            mConfirmDialog = null;
        }
        if(null == mConfirmDialog){
            mConfirmDialog = new WarnDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView)view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);
            WarnDialog.Builder builder = new WarnDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setSingleButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mConfirmDialog.dismiss();
                    mConfirmDialog = null;
                    resetSystem();
                }
            });

            mConfirmDialog = builder.create();
            mConfirmDialog.setCanceledOnTouchOutside(true);
            mConfirmDialog.show();
        }
    }
}
