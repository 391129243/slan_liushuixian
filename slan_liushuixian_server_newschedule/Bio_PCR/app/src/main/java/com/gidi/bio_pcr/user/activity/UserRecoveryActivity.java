package com.gidi.bio_pcr.user.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.WarningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.bioway.activity.BiowaySingleActivity;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomView;
import com.gidi.bio_pcr.view.WarnDialog;
import com.winters.boweilibrary.BoWei.serialcom.BoWeiConnectManager;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;
import com.winters.boweilibrary.BoWei.utils.BoWeiTaiKeHexUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * 复位界面
 */
public class UserRecoveryActivity extends BaseActivity implements View.OnClickListener {
    private LinearLayout mRecoveryTab;
    private LinearLayout mResetTab;
    private LinearLayout mMainTab;

    private CustomView mPcrStateTv;
    private CustomView mRNAStateTv;
    private TextView mAlarmMsgTv;

    private TextView mLaststepInfoTextview;

    private int experimentType;
    private String experimentTypeName;
    private String PCR_RUNNING_TEMPLATE;
    private int PCR_RUNNING_TEMPLATE_PHASE;//运行的文件first还是second
    private String PCR_RUNNING_TEMPLATE_STARTTIME;
    private UIHandler mHandler;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_user_recovery_layout;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mRecoveryTab = (LinearLayout)findViewById(R.id.main_tab_continue_layout);
        mResetTab = (LinearLayout)findViewById(R.id.recovery_tab_reset_layout);
        mMainTab = (LinearLayout)findViewById(R.id.recovery_tab_tomain_layout);
        mPcrStateTv = (CustomView)findViewById(R.id.recovery_top_pcr_state_img);
        mRNAStateTv = (CustomView)findViewById(R.id.recovery_top_rna_state_img) ;
        mAlarmMsgTv = (TextView)findViewById(R.id.recovery_top_alarm_msg_tv);
        mLaststepInfoTextview = (TextView)findViewById(R.id.laststep_info_textview);
    }

    @Override
    protected void initListener() {
        mRecoveryTab.setOnClickListener(this);
        mResetTab.setOnClickListener(this);
        mMainTab.setOnClickListener(this);
    }

    @Override
    protected void initData() {
        experimentType = PreferenceUtil.getInstance(getApplicationContext())
                .getIntValue(SharedConstant.EXPERIMENT_TYPE,0);//默认全流程
        switch (experimentType){
            case 0:
                experimentTypeName = getResources().getString(R.string.recovery_ex_type_full_procee);
                break;
            case 1:
                experimentTypeName = getResources().getString(R.string.recovery_ex_type_rna);
                break;
            case 2:
                experimentTypeName =getResources().getString(R.string.recovery_ex_type_pcr);
                break;
            default:
                experimentTypeName = getResources().getString(R.string.recovery_ex_type_full_procee);
                break;
        }
        PcrApplication.experimentType = experimentType;
        PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");
        PCR_RUNNING_TEMPLATE_PHASE = PreferenceUtil.getInstance(getApplicationContext())
                .getIntValue(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, 0);
        PCR_RUNNING_TEMPLATE_STARTTIME = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE_STARTTIME, "");
        String start_time = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.START_TIME,"");
        PcrApplication.ex_startTime = start_time;
        Log.i("RecoveryActivity","PCR_RUNNING_TEMPLATE" + PCR_RUNNING_TEMPLATE);
        Log.i("RecoveryActivity","PCR_RUNNING_TEMPLATE_PHASE" + PCR_RUNNING_TEMPLATE_PHASE);
        Log.i("RecoveryActivity","experientType" + experimentType);
        String lastRunningInfo = PcrApplication.rNARunningMessage.getNO();
        if(lastRunningInfo!=null){
            String[] info = lastRunningInfo.split("\\|",-1);
            if(info != null && info.length>=5){
                PcrApplication.rna_interuppt_time = info[4];
            }else if(info != null && info.length>=4){
                PcrApplication.rna_interuppt_time = "";
            }
        }

        String recovery_info = "";

        if((PcrApplication.rNARunningMessage.getStatus().contains("running")
                || PcrApplication.rNARunningMessage.getStatus().contains("error")
                || PcrApplication.rNARunningMessage.getStatus().contains("interrupt"))
                && PCR_RUNNING_TEMPLATE.equals("")){
            //RNA
            String rna_step_num = PcrApplication.rNARunningMessage.getNO();
            if(!StringUtil.isEmpty(rna_step_num)) {
                String[] mRNA_running = rna_step_num.split("\\|",-1);
                String templateNum = mRNA_running[0];
                String rna_file = "00";
                if(mRNA_running.length>=3){
                    rna_file = mRNA_running[1];
                }
                String rna_step = "00";
                if(mRNA_running.length>=3){
                    rna_step = mRNA_running[2];
                }
                File templateDir = getRecoveryTemplate1(templateNum);
                Log.i("UserRecoveryActivity","templateDir---" + templateDir);
                File templateDir_commonhex = getRecoveryTemplate_commonhex(templateNum);
                if(null != templateDir) {
                    PcrApplication.rootTempName = templateDir.getName();
                    Log.i("UserRecoveryActivity", "复位" + PcrApplication.rootTempName);
                    if(rna_file.equals("01")){
                        recovery_info = experimentTypeName + "-" + "（"
                                + getResources().getString(R.string.recovery_abnormal_step)
                                +"）："
                                + getResources().getString(R.string.recovery_template)
                                + "：" + PcrApplication.rootTempName
                                + "：" + getResources().getString(R.string.recovery_rna_extraction_process) + rna_file + "\n"
                                + getResources().getString(R.string.recovery_interrupt_time) + PcrApplication.rna_interuppt_time;
                    }else if(rna_file.equals("02")){
                        recovery_info = experimentTypeName + "-" +  "（"
                                +getResources().getString(R.string.recovery_abnormal_step)+
                                "）："
                                + getResources().getString(R.string.recovery_template)
                                + "：" + PcrApplication.rootTempName
                                + "：" + getResources().getString(R.string.recovery_rna_extraction_process) + rna_file + "\n"
                                + getResources().getString(R.string.recovery_interrupt_time) + PcrApplication.rna_interuppt_time;
                    }
                }else if(null != templateDir_commonhex) {
                    PcrApplication.rootTempName = templateDir_commonhex.getName();
                    Log.i("UserRecoveryActivity", "复位" + PcrApplication.rootTempName);
                    recovery_info = experimentTypeName + "- ("
                            +getResources().getString(R.string.recovery_abnormal_step) +"）"
                            + getResources().getString(R.string.recovery_general_action_template) + "：" + PcrApplication.rootTempName +  "\n"
                            + getResources().getString(R.string.recovery_interrupt_time) + PcrApplication.rna_interuppt_time;

                }

            }
//            recovery_info = recovery_info + "\n"  + "发生时间:" + PcrApplication.rna_interuppt_time;
        }else if(!PCR_RUNNING_TEMPLATE.equals("")) {
            //PCR复位
            PcrApplication.rootTempName = PCR_RUNNING_TEMPLATE;
            File file = new File(PCR_RUNNING_TEMPLATE);

            switch (experimentType){
                case 0:
                    if (PCR_RUNNING_TEMPLATE_PHASE == 1) {
                        file = new File(FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PCR_RUNNING_TEMPLATE + "/first.prj");
                    }else if (PCR_RUNNING_TEMPLATE_PHASE == 2) {
                        file = new File(FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PCR_RUNNING_TEMPLATE + "/second.prj");
                    }
                    break;
                case 2:
                    //single PCR
                    if (PCR_RUNNING_TEMPLATE_PHASE == 1) {
                        file = new File(FileUtils.SINGLE_PCR_TEMPLATE_PATH + File.separator + PCR_RUNNING_TEMPLATE + "/first.prj");
                    }else if (PCR_RUNNING_TEMPLATE_PHASE == 2) {
                        file = new File(FileUtils.SINGLE_PCR_TEMPLATE_PATH + File.separator  + PCR_RUNNING_TEMPLATE + "/second.prj");
                    }
                    break;
                default:
                    break;
            }


            if(file.exists()){
                if (PCR_RUNNING_TEMPLATE_PHASE == 1) {
                    recovery_info = experimentTypeName + "-" + "（"
                            + getResources().getString(R.string.recovery_abnormal_step) + "）"
                            + getResources().getString(R.string.recovery_template) + "：" + PCR_RUNNING_TEMPLATE
                            +  "；" + getResources().getString(R.string.recovery_pcr_process_one) +  "\n"
                            + getResources().getString(R.string.recovery_interrupt_time)
                            + PCR_RUNNING_TEMPLATE_STARTTIME;
                } else if (PCR_RUNNING_TEMPLATE_PHASE == 2) {
                    recovery_info = experimentTypeName + "-" + "（"
                            + getResources().getString(R.string.recovery_abnormal_step) + "）"
                            + getResources().getString(R.string.recovery_template) + "：" + PCR_RUNNING_TEMPLATE
                            +  "；" + getResources().getString(R.string.recovery_pcr_process_two) +  "\n"
                            + getResources().getString(R.string.recovery_interrupt_time)
                            + PCR_RUNNING_TEMPLATE_STARTTIME;
                } else {

                }
            }
//            recovery_info = recovery_info + "\n"  + "发生时间:" + PCR_RUNNING_TEMPLATE_STARTTIME;
        }
        mLaststepInfoTextview.setText(recovery_info);
        if(recovery_info.equals("")){
            mResetTab.callOnClick();//自动复位
        }
    }

    /**
     * 获取复位的模板文件夹
     * @param template_num
     * @return
     */
    private File getRecoveryTemplate(String template_num){
        //判断当前的模式，是全流程还是
        String templatePath = "";
        switch (experimentType){
            case 0:
                //全流程
                templatePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator;
                break;
            case 1:
                //singleRNA
                templatePath = FileUtils.SINGLE_RNA_TEMPLATE_PATH + File.separator;
                break;
            case 2:
                templatePath = FileUtils.SINGLE_PCR_TEMPLATE_PATH + File.separator;
                break;
        }

        File templateRootDir = new File(templatePath);
        if(templateRootDir.exists()&& templateRootDir.isDirectory()){
            File[] rootFiles = templateRootDir.listFiles();
            if(null != rootFiles && rootFiles.length>0){
                for(File file : rootFiles){
                    if(file.getName().startsWith(template_num)){
                        return file;
                    }
                }

            }
        }
        return null;
    }

    private File getRecoveryTemplate1(String template_num){
        //判断当前的模式，是全流程还是
        String templatePath = "";
        String templateName = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.RUNNING_TEMPLATE,"");
        switch (experimentType){
            case 0:
                //全流程
                templatePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator;
                break;
            case 1:
                //singleRNA
                templatePath = FileUtils.SINGLE_RNA_TEMPLATE_PATH + File.separator;

                break;
            case 2:
                templatePath = FileUtils.SINGLE_PCR_TEMPLATE_PATH + File.separator;
                break;
        }

        File templateRootDir = new File(templatePath);
        if(templateRootDir.exists()&& templateRootDir.isDirectory()){
            File[] rootFiles = templateRootDir.listFiles();
            if(null != rootFiles && rootFiles.length>0){
                for(File file : rootFiles){
                    if(file.getName().startsWith(template_num)
                        &&file.getName().equals(templateName)){
                        return file;
                    }
                }

            }
        }
        return null;
    }

    private File getRecoveryTemplate_commonhex(String template_num){
        String templatePath_commonhex = FileUtils.COMMON_HEX_PATH + File.separator;
        File templateRootDir_commonhex = new File(templatePath_commonhex);
        if(templateRootDir_commonhex.exists()&& templateRootDir_commonhex.isDirectory()){
            File[] rootFiles = templateRootDir_commonhex.listFiles();
            if(null != rootFiles && rootFiles.length>0){
                for(File file : rootFiles){
                    if(file.getName().startsWith(template_num)){
                        return file;
                    }
                }

            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new UIHandler(this);
        registerEventBus();
        initData();
        checkConnectState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
        if(null != mHandler){
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.main_tab_continue_layout) {//跳转至running
            mRecoveryTab.setEnabled(false);
            if(PcrApplication.is_RNA_connect&&PcrApplication.is_SLAN_PCR_connect ){
            }else{
                showNoticeDialog(getResources().getString(R.string.dialog_title_warning),
                        getResources().getString(R.string.dialog_message_equipment_module_problem));
                mRecoveryTab.setEnabled(true);
                return;
            }
            if(null != mHandler){
                mHandler.sendEmptyMessageDelayed(MsgType.MSG_BUTTON_ENABLE,5000);
            }
            if(!PCR_RUNNING_TEMPLATE.equals("")){
                //PCR复位
                if(experimentType == 0){
                    Intent PCR_Intent = new Intent(UserRecoveryActivity.this, UserBiowayActivity.class);
                    PCR_Intent.putExtra("recovery", true);
                    startActivity(PCR_Intent);
                    finish();
                }else if(experimentType == 2){
                    Intent PCR_Intent = new Intent(UserRecoveryActivity.this, BiowaySingleActivity.class);
                    PCR_Intent.putExtra("recovery", true);
                    startActivity(PCR_Intent);
                    finish();
                }

            }else{
                if ((PcrApplication.rNARunningMessage.getStatus().contains("running")
                        || PcrApplication.rNARunningMessage.getStatus().contains("error")
                        || PcrApplication.rNARunningMessage.getStatus().contains("interrupt"))) {
//                    && PCR_RUNNING_TEMPLATE.equals("")) {
                    //跳转的是RNA
                    // 复位操作
                    String rna_step_num = PcrApplication.rNARunningMessage.getNO();
                    Log.i("RecoveryActivity", "rna_step_num11" + rna_step_num);
                    if (!StringUtil.isEmpty(rna_step_num)) {
                        String[] mRNA_running = rna_step_num.split("\\|", -1);
                        String templateNum = mRNA_running[0];
                        String rna_file = "00";
                        if (mRNA_running.length >= 3) {
                            rna_file = mRNA_running[1];
                        }
                        String rna_step = "00";
                        if (mRNA_running.length >= 3) {
                            rna_step = mRNA_running[2];
                        }

                        File templateDir = getRecoveryTemplate1(templateNum);
                        File templateDir_commonhex = getRecoveryTemplate_commonhex(templateNum);
                        if (null != templateDir_commonhex) {
                            continueCommonHex(templateDir_commonhex, Integer.valueOf(rna_step));
                            mRecoveryTab.setEnabled(true);
                            return;
                        }
                    }
                    rna_step_num = PcrApplication.rNARunningMessage.getNO();
                    Log.i("RecoveryActivity", "rna_step_num11" + rna_step_num);
                    if (!StringUtil.isEmpty(rna_step_num)) {
                        switch (experimentType) {
                            case 0:
                                //全流程
                                Intent intent = new Intent(UserRecoveryActivity.this, UserBiowayActivity.class);
                                intent.putExtra("recovery", true);
                                startActivity(intent);
                                finish();
                                break;
                            case 1:
                                //single RNA
                                Intent singleIntent = new Intent(UserRecoveryActivity.this, BiowaySingleActivity.class);
                                singleIntent.putExtra("recovery", true);
                                startActivity(singleIntent);
                                finish();

                        }
                    }
                }
            }
        } else if (id == R.id.recovery_tab_reset_layout) {//执行下机操作
            if(PcrApplication.is_RNA_connect){
            }else{
                showNoticeDialog(getResources().getString(R.string.dialog_title_warning),
                        getResources().getString(R.string.dialog_message_equipment_module_problem));
                return;
            }
            Log.i("RecoveryActivity","recovery reset");
            resetSystem();
        }else if (id == R.id.recovery_tab_tomain_layout) {//执行下机操作
            Intent intent = new Intent(UserRecoveryActivity.this, UserMainActivity.class);
            startActivity(intent);
            finish();
        }
    }


    private void checkConnectState(){

        if(PcrApplication.is_RNA_connect){
            mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }

        if( PcrApplication.is_SLAN_PCR_connect){
            mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
    }

    /*
     * 超时消息
     * @param sendCmdTimeoutEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {

    }

    int warning = 1;
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WarningMessage warningMessage) {

        mAlarmMsgTv.setText(warningMessage.getAlarm() + "-" +
                warningMessage.getMotorID() + "-" + warningMessage.getStatus());

    }

    /**
     * 指令正在执行
     * @param rNARunningMessage
     */

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RNARunningMessage rNARunningMessage) {
        Log.i("RecoveryActivity", "RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
        if(rNARunningMessage != null )
        {
            PcrApplication.is_RecRunningMesaage = true;
            PcrApplication.rNARunningMessage = rNARunningMessage;
            if(rNARunningMessage.getAlarm()!=null && !rNARunningMessage.getAlarm().equals(""))

                mAlarmMsgTv.setText(rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {

        if(true){
            if(procedureFinishEvent.getStatus().equals("finish")){
                hideProgressDialog();
                PreferenceUtil.getInstance(this.getApplicationContext()).setValueByName(SharedConstant.IS_BOOT_UP_NEED_RESET, false);
                Intent intent = new Intent(UserRecoveryActivity.this, UserMainActivity.class);
                startActivity(intent);
                finish();
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                Toast.makeText(UserRecoveryActivity.this,"LoginActivity reset TimeoutEvent  :  " + "finish error",Toast.LENGTH_LONG).show();
                hideProgressDialog();
                showNoticeDialog(getResources().getString(R.string.login_dialog_title_check_device),
                        getResources().getString(R.string.login_dialog_message_reset_error));
            }
            stopRNARunning();
            if(procedureFinishEvent.getTemplateName().contains("reset")){
                if (procedureFinishEvent.getStatus().equals("finish")) {
                    PcrApplication.rNARunningMessage.setStatus("free");
                    resetPcrRecMark();
                    BoWeiPCRStepEntity pCRStepEntityBoWei = BoWeiTaiKeHexUtils.Create_StopPeltier_Message();
                    BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pCRStepEntityBoWei);
                }
            }
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
                if(PcrApplication.is_RNA_connect){
                    mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    if(PcrApplication.is_SLAN_PCR_connect){
                        mAlarmMsgTv.setText("");
                    }else {
                        mAlarmMsgTv.setText("E4-"+getResources().getString(R.string.e4_alarm_pcr_hardware_disconnect));
                    }
                }else{
                    mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
                    mAlarmMsgTv.setText("E3-"+getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                }
            break;
            case MsgType.MSG_PCR_CONNECT_STATE:
                if( PcrApplication.is_SLAN_PCR_connect){
                    mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    if(PcrApplication.is_RNA_connect){
                        mAlarmMsgTv.setText("");
                    }else{
                        mAlarmMsgTv.setText("E3-"+getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
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

    private void resetSystem(){
        if(PcrApplication.is_RNA_connect){
            ////异步全流程
            resetPcrRecMark();
            String rna_step_num = PcrApplication.rNARunningMessage.getNO();
            int flag = 0;
            String downandopen_filePath = "";
            if(!StringUtil.isEmpty(rna_step_num)) {
                String[] mRNA_running = rna_step_num.split("\\|",-1);
                if (mRNA_running.length >= 5) {
                    flag = NumberUtils.StringtoInt(mRNA_running[3]);
                }
            }
            if(flag == 1){
                downandopen_filePath = FileUtils.COMMON_HEX_PATH + "99-reset.hex";
            }else if(flag == 0){
                downandopen_filePath = FileUtils.COMMON_HEX_PATH + "95-reset_exception.hex";
            }

            File resetfile = new File(downandopen_filePath);
            boolean result = startAysnTask(resetfile);
            if(result) {
                showProgressDialog(getResources().getString(R.string.dialog_title_wait),
                        getResources().getString(R.string.dialog_message_reset));
            }else{
                showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                        getResources().getString(R.string.dialog_message_reset_error));
            }
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
        }
    }

    private void continueCommonHex(File file, int stepIndex){
        if(PcrApplication.is_RNA_connect){
            ////异步全流程
            String commonHexfilePath = FileUtils.COMMON_HEX_PATH + file.getName();
            File commonHexfile = new File(commonHexfilePath);
            boolean result = startAysnTask(commonHexfile,stepIndex);
            if(result) {
                showProgressDialog(getResources().getString(R.string.dialog_title_wait),
                        file.getName());
            }else{
                showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                        file.getName() + ":" + "error");
            }
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void showNoticeDialog(String title, String message){
        if(null != mNoticeDialog){
            if(mNoticeDialog.isShowing()){
                return;
            }else{
                mNoticeDialog = null;
            }
        }
        if(null == mNoticeDialog){
            mNoticeDialog = new WarnDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView)view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);
            WarnDialog.Builder builder = new WarnDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setSingleButton(R.string.dialog_skipselfcheck, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    clearAlarm();
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                    Intent intent = new Intent(UserRecoveryActivity.this, UserMainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
    }


    public class UIHandler extends Handler {

        WeakReference<UserRecoveryActivity> mActivityReference;

        public UIHandler(UserRecoveryActivity mActivity) {
            mActivityReference = new WeakReference<UserRecoveryActivity>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            final UserRecoveryActivity mActivity = mActivityReference.get();
            if (null == mActivity) {
                return;
            }
            switch (msg.what) {
                case MsgType.MSG_BUTTON_ENABLE:
                    mActivity.mRecoveryTab.setEnabled(true);
                    break;
            }
        }
    }
}
