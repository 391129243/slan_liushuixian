package com.gidi.bio_pcr.user.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.ActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.WarningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.activity.MainActivity;
import com.gidi.bio_pcr.activity.ResultActivity;
import com.gidi.bio_pcr.activity.SystemInfoActivity;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.paul.pipeline.activity.PipeMainActivity;
import com.gidi.bio_pcr.util.CommonUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.winters.boweilibrary.BoWei.BoWeiService;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;


/**
 * 登录后进入
 * 主界面
 * 显示版本
 * 时间
 */
public class UserMainActivity extends BaseActivity implements View.OnClickListener{

    private LinearLayout mDetectTab;
    private LinearLayout mRecordTab;//核酸提取
    private LinearLayout mSettingTab;

    private LinearLayout mTopBarLayout;

    private TextView mAlarmMsgTv;
    private ImageView mVolumeImg;
    private CustomDialog mSettingDialog;
    private AudioManager mAudioManager;
    private int alarmVolume;
    private boolean isMute = false;

    private String PCR_RUNNING_TEMPLATE;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_user_main;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

        mRecordTab  = (LinearLayout) findViewById(R.id.user_main_tab_record_layout);
        mDetectTab = (LinearLayout) findViewById(R.id.user_main_full_procee_tab);
        mSettingTab = (LinearLayout)findViewById(R.id.user_main_tab_setting_layout);
        mTopBarLayout = (LinearLayout)findViewById(R.id.user_main_top_state_layout);
        mAlarmMsgTv = (TextView)findViewById(R.id.user_main_top_alarm_msg_tv);
        mVolumeImg = (ImageView)findViewById(R.id.user_main_top_sound_tv);
    }

    @Override
    protected void initListener() {
        mRecordTab.setOnClickListener(this);
        mDetectTab.setOnClickListener(this);
        mSettingTab.setOnClickListener(this);
        mVolumeImg.setOnClickListener(this);
    }

    @Override
    protected void initData() {
        mAudioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        alarmVolume = (int)((maxVolume * 4)/5);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        initData();
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
        mAudioManager = null;

    }



    /**
     * 检查sdcard的容量
     */
    private void checkSdcardSize(){
        long availableSdcardSize = CommonUtil.getAvailableSDcardSize();
        Log.i("UserMainActvity","availableSdcardSize"+ availableSdcardSize);
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


    @Override
    public void onClick(View v) {
        PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");

        int id = v.getId();
        if(id == R.id.user_main_full_procee_tab){
//            if(PcrApplication.is_RNA_connect&&PcrApplication.is_SLAN_PCR_connect ){
//
//            }else{
//                showNoticeDialog(getResources().getString(R.string.dialog_title_warning),
//                        getResources().getString(R.string.dialog_message_equipment_module_problem));
//                return;
//            }
//            if(PcrApplication.rNARunningMessage == null||!PCR_RUNNING_TEMPLATE.equals("")){
//                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_abnormal),
//                        getResources().getString(R.string.dialog_message_reset_first));
//            }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")) {
//                PcrApplication.experimentType = 0;
//                PreferenceUtil.getInstance(getApplicationContext())
//                        .setValueByName(SharedConstant.EXPERIMENT_TYPE,PcrApplication.experimentType);
                Intent detectIntent = new Intent(UserMainActivity.this, UserBiowayActivity.class);
                startActivity(detectIntent);

//            Intent detectIntent = new Intent(UserMainActivity.this, PipeMainActivity.class);
//            startActivity(detectIntent);

                finish();
//            }else{
//                showNoticeDialog(getResources().getString(R.string.dialog_title_equipment_running),
//                        getResources().getString(R.string.dialog_message_reset_first));
//            }
        }else if(id == R.id.user_main_tab_record_layout){
            Intent recordIntent = new Intent(UserMainActivity.this, ResultActivity.class);
            recordIntent.putExtra("SettingIn",false);
            startActivity(recordIntent);
        }else if(id ==  R.id.user_main_tab_setting_layout){
            //弹出输入密码的对话框，输入正确101010,则进入
            showSettingDialog();
        }  else if(id == R.id.user_main_top_sound_tv){
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
                    if(PcrApplication.is_SLAN_PCR_connect){
                        mAlarmMsgTv.setText("");
                        mTopBarLayout.setVisibility(View.INVISIBLE);
                    }else {
                        mTopBarLayout.setVisibility(View.VISIBLE);
                        mAlarmMsgTv.setText("E4-"+getResources().getString(R.string.e4_alarm_pcr_hardware_disconnect));
                    }
                }else{
                    mTopBarLayout.setVisibility(View.VISIBLE);
                    mAlarmMsgTv.setText("E3-"+getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                }
                break;
            case MsgType.MSG_PCR_CONNECT_STATE:
                Log.e("MSG_PCR_CONNECT_STATE", "is_SLAN_PCR_connect"+ PcrApplication.is_SLAN_PCR_connect);
                if(PcrApplication.is_SLAN_PCR_connect){
                    if(PcrApplication.is_RNA_connect){
                        mTopBarLayout.setVisibility(View.GONE);
                        mAlarmMsgTv.setText("");
                    }else{
                        mTopBarLayout.setVisibility(View.VISIBLE);
                        mAlarmMsgTv.setText("E3-"+getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                    }
                }else {
                    if(!PcrApplication.is_RNA_connect){
                        mTopBarLayout.setVisibility(View.VISIBLE);
                        mAlarmMsgTv.setText(getResources().getString(R.string.e3_e4_alarm_both_hardware_disconnect));
                    }else{
                        mTopBarLayout.setVisibility(View.VISIBLE);
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
                showNoticeDialog(getResources().getString(R.string.login_dialog_title_check_device),
                        getResources().getString(R.string.config_hex_dialog_message_error_instruction));
            }
            stopRNARunning();
        }
        if(procedureFinishEvent.getTemplateName().contains("reset")){
            if (procedureFinishEvent.getStatus().equals("finish")) {
                resetPcrRecMark();
                PcrApplication.rNARunningMessage.setStatus("free");
                if(PcrApplication.is_SLAN_PCR_connect) {
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
            UserLoginActivity.rnaHeartBeatCount = 0;
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

    public void showSettingDialog(){
        if(null != mSettingDialog){
            mSettingDialog.dismiss();
            mSettingDialog = null;
        }
        if (null == mNoticeDialog) {
            mSettingDialog = new CustomDialog(this);
            LayoutInflater mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_setting_layout, null);
            EditText pwdEt = (EditText) view.findViewById(R.id.dialog_password_et);
            CustomDialog.Builder builder = new CustomDialog.Builder(this);
            builder.setContentView(view);

            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String password = PreferenceUtil.getInstance(getApplicationContext())
                            .getStringValue(SharedConstant.SETTING_PASSWORD,"101010");
                    if(pwdEt.getText().toString().trim().equals(password)){
//                        Intent infoIntent = new Intent(UserMainActivity.this, MainActivity.class);
                        Intent infoIntent = new Intent(UserMainActivity.this, SystemInfoActivity.class);
                        startActivity(infoIntent);
                    }else{
                        Toast.makeText(getApplicationContext(),"密码错误,请重新输入",Toast.LENGTH_LONG).show();
                    }
                    mSettingDialog.dismiss();
                    mSettingDialog = null;
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mSettingDialog.dismiss();
                    mSettingDialog = null;
                }
            });

            mSettingDialog = builder.create();
            mSettingDialog.setCanceledOnTouchOutside(true);
            mSettingDialog.show();
        }
    }

}
