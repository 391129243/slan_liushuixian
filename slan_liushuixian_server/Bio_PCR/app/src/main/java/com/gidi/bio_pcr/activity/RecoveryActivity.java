package com.gidi.bio_pcr.activity;

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
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.WarningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.bioway.activity.BiowayActivity;
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
public class RecoveryActivity extends BaseActivity implements View.OnClickListener {
    private LinearLayout mRecoveryTab;
    private LinearLayout mResetTab;
    private LinearLayout mMainTab;

    private CustomView mPcrStateTv;
    private CustomView mRNAStateTv;
    private TextView mAlarmMsgTv;

    private TextView mLaststepInfoTextview;

    private int experientType;
    private String experimentTypeName;
    private String PCR_RUNNING_TEMPLATE;
    private int PCR_RUNNING_TEMPLATE_PHASE;//运行的文件first还是second
    private String PCR_RUNNING_TEMPLATE_STARTTIME;
    private UIHandler mHandler;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_recovery_layout;
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
        experientType = PreferenceUtil.getInstance(getApplicationContext())
                .getIntValue(SharedConstant.EXPERIMENT_TYPE,0);//默认全流程
        switch (experientType){
            case 0:
                experimentTypeName = "实验类型：全流程";
                break;
            case 1:
                experimentTypeName = "实验类型：核酸提取";
                break;
            case 2:
                experimentTypeName = "实验类型：PCR";
                break;
            default:
                experimentTypeName = "实验类型：全流程";
                break;
        }
        PcrApplication.experimentType = experientType;
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
        Log.i("RecoveryActivity","experientType" + experientType);
        String lastRunningInfo = PcrApplication.rNARunningMessage.getNO();
        Log.i("RecoveryActivity","lastRunningInfo" + lastRunningInfo);
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
                File templateDir = getRecoveryTemplate(templateNum);
                Log.i("RecoveryActivity","templateDir---" + templateDir);
                File templateDir_commonhex = getRecoveryTemplate_commonhex(templateNum);
                if(null != templateDir) {
                    PcrApplication.rootTempName = templateDir.getName();
                    Log.i("RecoveryActivity", "复位" + PcrApplication.rootTempName);
                    if(rna_file.equals("01")){
                        recovery_info = experimentTypeName + "-" + "（异常步骤）" + "模板" + "：" + PcrApplication.rootTempName
                                + "：" + "RNA提取阶段" + rna_file + "\n"  + "发生时间：" + PcrApplication.rna_interuppt_time;
                    }else if(rna_file.equals("02")){
                        recovery_info = experimentTypeName + "-" + "（异常步骤）：" + "模板" + "：" + PcrApplication.rootTempName
                                + "：" + "RNA提取阶段" + rna_file + "\n"  + "发生时间：" + PcrApplication.rna_interuppt_time;
                    }
                }else if(null != templateDir_commonhex) {
                    PcrApplication.rootTempName = templateDir_commonhex.getName();
                    Log.i("RecoveryActivity", "复位" + PcrApplication.rootTempName);
                    recovery_info = experimentTypeName + "-" + "（异常步骤）" + "通用动作模板" + "：" + PcrApplication.rootTempName +  "\n"  + "发生时间：" + PcrApplication.rna_interuppt_time;

                }

            }
//            recovery_info = recovery_info + "\n"  + "发生时间:" + PcrApplication.rna_interuppt_time;
        }else if(!PCR_RUNNING_TEMPLATE.equals("")) {
            //PCR复位
            PcrApplication.rootTempName = PCR_RUNNING_TEMPLATE;
            File file = new File(PCR_RUNNING_TEMPLATE);

            switch (experientType){
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
                    recovery_info = experimentTypeName + "-" + "（异常步骤）" + "模板" + "：" + PCR_RUNNING_TEMPLATE +  "；" + "PCR第一阶段" +  "\n"  + "发生时间：" + PCR_RUNNING_TEMPLATE_STARTTIME;
                } else if (PCR_RUNNING_TEMPLATE_PHASE == 2) {
                    recovery_info = experimentTypeName + "-" + "（异常步骤）" + "模板" + "：" + PCR_RUNNING_TEMPLATE +  "；" + "PCR第二阶段" +  "\n"  + "发生时间：" + PCR_RUNNING_TEMPLATE_STARTTIME;
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
        switch (experientType){
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
        registerEventBus();
        mHandler = new UIHandler(this);
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
                showNoticeDialog("警告","设备模块有问题，请检查");
                mRecoveryTab.setEnabled(true);
                return;
            }
            if(null != mHandler){
                mHandler.sendEmptyMessageDelayed(MsgType.MSG_BUTTON_ENABLE,5000);
            }
            if(!PCR_RUNNING_TEMPLATE.equals("")){
                //PCR复位
                if(experientType == 0){
                    Intent PCR_Intent = new Intent(RecoveryActivity.this, BiowayActivity.class);
                    PCR_Intent.putExtra("recovery", true);
                    startActivity(PCR_Intent);
                    finish();
                }else if(experientType == 2){
                    Intent PCR_Intent = new Intent(RecoveryActivity.this, BiowaySingleActivity.class);
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

                        File templateDir = getRecoveryTemplate(templateNum);
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
                        switch (experientType) {
                            case 0:
                                //全流程
                                Intent rnaIntent = new Intent(RecoveryActivity.this, BiowayActivity.class);
                                rnaIntent.putExtra("recovery", true);
                                startActivity(rnaIntent);
                                finish();
                                break;
                            case 1:
                                //single RNA
                                Intent singleIntent = new Intent(RecoveryActivity.this, BiowaySingleActivity.class);
                                singleIntent.putExtra("recovery", true);
                                startActivity(singleIntent);
                                finish();

                        }
                    }
                }
            }
        } else if (id == R.id.recovery_tab_reset_layout) {//执行下机操作
            if(!PcrApplication.is_RNA_connect){
                showNoticeDialog("警告","设备模块有问题，请检查");
                return;
            }
            Log.i("RecoveryActivity","recovery reset");
            resetSystem();
        }else if (id == R.id.recovery_tab_tomain_layout) {//执行下机操作
            Intent intent = new Intent(RecoveryActivity.this, MainActivity.class);
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

//        if(procedureFinishEvent.getTemplateName().contains("reset")){
        if(true){
            if(procedureFinishEvent.getStatus().equals("finish")){
                hideProgressDialog();
                PreferenceUtil.getInstance(this.getApplicationContext()).setValueByName(SharedConstant.IS_BOOT_UP_NEED_RESET, false);
                Intent intent = new Intent(RecoveryActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                Toast.makeText(RecoveryActivity.this,"LoginActivity reset TimeoutEvent  :  " + "finish error",Toast.LENGTH_LONG).show();
                hideProgressDialog();
                showNoticeDialog("请检查及重启设备","复位出错，请检查设备并重启!");
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
                        mAlarmMsgTv.setText("E4-PCR硬件通讯通讯断连");
                    }
                }else{
                    mRNAStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
                    mAlarmMsgTv.setText("E3-核提硬件通讯通讯断连");
                }
            break;
            case MsgType.MSG_PCR_CONNECT_STATE:
                if( PcrApplication.is_SLAN_PCR_connect){
                    mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    if(PcrApplication.is_RNA_connect){
                        mAlarmMsgTv.setText("");
                    }else{
                        mAlarmMsgTv.setText("E3-核提硬件通讯通讯断连");
                    }
                }else {
                    mPcrStateTv.setStateBackgroud(R.drawable.shape_oval_red_bg);
                    if(!PcrApplication.is_RNA_connect){
                        mAlarmMsgTv.setText("E3-核提硬件通讯通讯断连/E4-PCR硬件通讯通讯断连");
                    }else{
                        mAlarmMsgTv.setText("E4-PCR硬件通讯通讯断连");
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
            Toast.makeText(getApplicationContext(),"RNA串口断连",Toast.LENGTH_LONG).show();
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
            Toast.makeText(getApplicationContext(),"RNA串口断连",Toast.LENGTH_LONG).show();
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
                    Intent intent = new Intent(RecoveryActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
    }


    public class UIHandler extends Handler {

        WeakReference<RecoveryActivity> mActivityReference;

        public UIHandler(RecoveryActivity mActivity) {
            mActivityReference = new WeakReference<RecoveryActivity>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            final RecoveryActivity mActivity = mActivityReference.get();
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
