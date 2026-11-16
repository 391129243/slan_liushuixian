package com.gidi.bio_pcr.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.utils.AssetUtils;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.activity.ShowConfigActivity;
import com.gidi.bio_pcr.connect.ConnectManager;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.listener.KeyBoardActionListener;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.gidi.bio_pcr.view.SystemKeyBoardEditText;
import com.gidi.bio_pcr.view.WarnDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.ref.WeakReference;


public class LoginActivity extends BaseActivity implements View.OnClickListener{

    private RelativeLayout mSelfCheckLayout;
    private RelativeLayout mLoginLayout;
    private EditText mAccountEt;
    private SystemKeyBoardEditText mPwdEt;
    private Button mLoginBtn;
    private int tryCount = 0;
    private UIHandler mUIHandler;

    private static final int SELF_CHECK = 100;
    private static final int RNA_CONNECTION_FAILED = 101;
    private static final int PCR_CONNECTION_FAILED = 102;
    private static final int RNA_CONNECTION_NORMAL = 103;
    private static final int PCR_CONNECTION_NORMAL = 104;
    private static final int HEARTCHECK = 105;
    private static final int MSG_END_INIT = 106;


    private boolean skip = false;

    private Context context;
    private boolean isRunningFlag = true;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_login;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

        mSelfCheckLayout = (RelativeLayout)findViewById(R.id.login_auto_check_layout) ;
        mLoginLayout = (RelativeLayout)findViewById(R.id.login_account_layout);
        mAccountEt = (EditText) findViewById(R.id.login_account_et) ;
        mAccountEt.addTextChangedListener(new LimitInputTextWatcher(mAccountEt));
        mPwdEt = (SystemKeyBoardEditText)findViewById(R.id.login_pwd_et) ;
        mLoginBtn = (Button)findViewById(R.id.login_btn);
    }


    @Override
    protected void initListener() {

        mLoginBtn.setOnClickListener(this);
        mAccountEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mPwdEt.setOnKeyboardActionListener(new KeyBoardActionListener() {
            @Override
            public void onComplete() {

            }

            @Override
            public void onTextChange(Editable editable) {

            }

            @Override
            public void onClear() {

            }

            @Override
            public void onClearAll() {

            }
        });

    }

    @Override
    protected void initData() {
        FileUtils.COMMON_HEX_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/commonHex_v5/";
        mUIHandler = new UIHandler(this);

        boolean init  = PreferenceUtil.getInstance(getApplicationContext())
                .getBooleanValue(SharedConstant.SYSTEM_INIT,false);
        if(!init) {
            showLoadingDialog(getResources().getString(R.string.login_dialog_message_init));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //////需要加初始化的dialog
                        AssetUtils.copyAssets("commonHex_v5", context, true);
                        AssetUtils.copyAssets("hexFile_v5", context, true);
                        AssetUtils.copyAssets("file", context, true);
                        PreferenceUtil.getInstance(getApplicationContext())
                                .setValueByName(SharedConstant.SYSTEM_INIT,true);
                        if(null != mUIHandler){
                            mUIHandler.sendEmptyMessage(MSG_END_INIT);
                        }
                    } catch (Exception e) {
                        Log.e("LoginActivity", e.getMessage());
                        //throw new RuntimeException(e);
                        if(null != mUIHandler){
                            mUIHandler.sendEmptyMessage(MSG_END_INIT);
                        }
                    }
                }
            }).start();
        }
        autoSelfCheck();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        context =this;
        initData();
        AudioManager mAudioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int alarmVolume = (int)((maxVolume * 4)/5);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, alarmVolume, AudioManager.FLAG_PLAY_SOUND);

    }



    private void autoSelfCheck() {
//        mUIHandler.postDelayed(runnable, 2000);
        mUIHandler.sendEmptyMessageDelayed(HEARTCHECK,5000);
    }

    /////假如PCR、核酸提取下位机的自动重连功能
    public static long  rnaHeartBeatCount = 0;

    private boolean loginFlag = false;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //TODO:延时要干的其他事情
            /////RNA
//            Log.i("LoginActivity","runnable");
//            if( rnaHeartBeatCount++ > 4 ){
//                ////RNA下位机没有心跳消息
//                PcrApplication.is_RNA_connect = false;
//                initRNACom();
//                rnaHeartBeatCount = 0;
//                if(loginFlag){
//                    mUIHandler.sendEmptyMessage(RNA_CONNECTION_FAILED);
//                }
//            }else{
//                if(loginFlag){
//                    mUIHandler.sendEmptyMessage(RNA_CONNECTION_NORMAL);
//                }
//            }
//            /////PCR
//            if(!PcrApplication.isDewit) {
//                if (PcrApplication.is_SLAN_PCR_connect) {
//                    mUIHandler.sendEmptyMessage(PCR_CONNECTION_NORMAL);
//                } else {
//                    initPCR();
//                    mUIHandler.sendEmptyMessage(PCR_CONNECTION_NORMAL);
//                }
//            }
//
//            if(!loginFlag) {
//                mUIHandler.sendEmptyMessage(SELF_CHECK);
//            }
//            mUIHandler.postDelayed(runnable, 1000);
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
//        registerEventBus();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unRegisterEventBus();
        isRunningFlag = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }

        runnable= null;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.login_btn) {
            String serialName = android.os.Build.SERIAL;

            serialName = "101010";
            String password = PreferenceUtil.getInstance(getApplicationContext())
                    .getStringValue(SharedConstant.LOGIN_PASSWORD,"123456");
            if(mPwdEt.getText().toString().equals(password)){
                PcrApplication.isAdmin =false;
                loginFlag = true;
            }else if(serialName.endsWith(mPwdEt.getText().toString())){
                PcrApplication.isAdmin =true;
                loginFlag = true;
                //测试

            }else if(mPwdEt.getText().toString().equals("222222")){
                loginFlag = true;
                PcrApplication.isAdmin =true;
                Intent intent = new Intent(LoginActivity.this, ShowConfigActivity.class);//////选择复位还是继续继续执行RNA片段页面
                startActivity(intent);
                return;
            }else{
                Toast.makeText(this.getApplicationContext(), "密码错误，请重新输入", Toast.LENGTH_LONG).show();
                mPwdEt.setText("");
                return;
            }
            ////跳过reset及recovery页面
            if(skip){
                //   Intent intent = new Intent(LoginActivity.this, MainActivity.class);
               // startActivity(intent);
                openMainSetting();
                return;
            }

            boolean flag = PreferenceUtil.getInstance(getApplicationContext()).getBooleanValue(SharedConstant.IS_BOOT_UP_NEED_RESET, false);
            String PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getApplicationContext())
                    .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");
            Log.i("Single", " login PCR_RUNNING_TEMPLATE" + PCR_RUNNING_TEMPLATE);
            /////添加shared
            if (flag && PcrApplication.rNARunningMessage.getStatus().contains("free") && PCR_RUNNING_TEMPLATE.equals("")) {
                PreferenceUtil.getInstance(getApplicationContext())
                        .setValueByName(SharedConstant.START_TIME, "");
                resetSystem();

            } else if (PcrApplication.rNARunningMessage.getStatus().contains("free") && PCR_RUNNING_TEMPLATE.equals("")) {
                PreferenceUtil.getInstance(getApplicationContext())
                        .setValueByName(SharedConstant.START_TIME, "");
                //ceshi
                openMainSetting();
//                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                startActivity(intent);
            } else if ((PcrApplication.rNARunningMessage.getStatus().contains("running")
                    || PcrApplication.rNARunningMessage.getStatus().contains("error")
                    || PcrApplication.rNARunningMessage.getStatus().contains("interrupt"))
                    && PCR_RUNNING_TEMPLATE.equals("")) {
                //
                Intent intent = new Intent(LoginActivity.this, RecoveryActivity.class);//////选择复位还是继续继续执行RNA片段页面
                startActivity(intent);
            } else if (!PCR_RUNNING_TEMPLATE.equals("")) {
                Intent intent = new Intent(LoginActivity.this, RecoveryActivity.class);//////选择复位还是继续继续执行PCR页面页面
                startActivity(intent);
            }
        }
    }

    private void openMainSetting() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
       // Intent intent = new Intent(LoginActivity.this, SystemInfoActivity.class);
        startActivity(intent);
    }

    /**
     * 自检结果处理
     */
    private void seftCheckSuccess(){
        mSelfCheckLayout.setVisibility(View.GONE);
        mLoginLayout.setVisibility(View.VISIBLE);
        String account = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.LOGIN_ACCOUNT,"User");
        String password = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.LOGIN_PASSWORD,"123456");
        mAccountEt.setText("User");
//        mPwdEt.setText(password);
    }


    public class UIHandler extends Handler {

        WeakReference<LoginActivity> mActivityReference;

        public UIHandler(LoginActivity mActivity) {
            mActivityReference = new WeakReference<LoginActivity>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            final LoginActivity mActivity = mActivityReference.get();
            if (null == mActivity) {
                return;
            }
            switch (msg.what) {
                case MSG_END_INIT:
                    hideLoadingDialog();
                    break;
                case HEARTCHECK:
                    if( rnaHeartBeatCount++ > 4 ){
                        ////RNA下位机没有心跳消息
                        Log.i("RNA","reconnect");
                        Log.i("RNA","is_RNA_connect"+ PcrApplication.is_RNA_connect  );
                         // 20250102修改//
                        PcrApplication.is_RNA_connect = false;
                        ConnectManager.getInstance().initRNAConnection(getApplicationContext());
                        rnaHeartBeatCount = 0;
                        if(loginFlag){
                            mUIHandler.sendEmptyMessage(RNA_CONNECTION_FAILED);
                        }
                    }else{
                        if(loginFlag){
                            mUIHandler.sendEmptyMessage(RNA_CONNECTION_NORMAL);
                        }
                    }
                    /////PCR
                    if (PcrApplication.is_SLAN_PCR_connect) {
                        mUIHandler.sendEmptyMessage(PCR_CONNECTION_NORMAL);
                    } else {
                        Log.i("LoginActivity","PCR reCONNECT");
                        mUIHandler.sendEmptyMessage(PCR_CONNECTION_FAILED);
                    }
                    if(!loginFlag) {
                        mUIHandler.sendEmptyMessage(SELF_CHECK);
                    }
                    mUIHandler.sendEmptyMessageDelayed(HEARTCHECK,2000);
                    break;
                case RNA_CONNECTION_FAILED:
                case RNA_CONNECTION_NORMAL:
                    MessageEvent stateMsg_RNA = new MessageEvent();
                    stateMsg_RNA.setMsgType(MsgType.MSG_RNA_CONNECT_STATE);
                    stateMsg_RNA.setData(PcrApplication.is_RNA_connect);
                    EventBus.getDefault().post(stateMsg_RNA);
                    break;
                case PCR_CONNECTION_FAILED:
                case PCR_CONNECTION_NORMAL:
                    MessageEvent stateMsg_PCR = new MessageEvent();
                    stateMsg_PCR.setMsgType(MsgType.MSG_PCR_CONNECT_STATE);
                    stateMsg_PCR.setData(PcrApplication.is_SLAN_PCR_connect);
                    EventBus.getDefault().post(stateMsg_PCR);
                    break;
                case SELF_CHECK:
                    if(skip){
                        return;
                    }

                    if(PcrApplication.is_RNA_connect && PcrApplication.is_SLAN_PCR_connect&& PcrApplication.is_RecRunningMesaage) {
                        tryCount = 0;
                        clearAlarm();
                        if(null != mActivity.mNoticeDialog){
                            if(mActivity.mNoticeDialog.isShowing()){
                                mActivity.mNoticeDialog.dismiss();
                            }
                        }

                        mActivity.seftCheckSuccess();
                    }else{
                        mActivity.mSelfCheckLayout.setVisibility(View.VISIBLE);
                        mActivity.mLoginLayout.setVisibility(View.GONE);
                            //mActivity.mLoginLayout.setBackground(getResources().getDrawable(R.drawable.system_selfcheck,null));
                        mActivity.tryCount++;
                        if( mActivity.tryCount > 3 ){
                            String warning = "";
                            if(!PcrApplication.is_RNA_connect||!PcrApplication.is_RecRunningMesaage){
                                warning = "  （核酸提取模块有问题）";
                            }
                            if(!PcrApplication.is_SLAN_PCR_connect){
                                warning = warning + "  " + getResources().getString(R.string.login_dialog_slan_pcr_error);
                            }

                            warning = warning + "，"+ getResources().getString(R.string.login_dialog_check_or_restart);
                            showAlarm();
                            mActivity.showNoticeDialog(getResources().getString(R.string.dialog_title_prompt), warning);
                            return;
                        }
                    }
                    break;
            }
        }
    }




    /*
     * EventBus接收订阅的消息
     * @param messageEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {

    }


    /**
     * 系统复位
     */

    private void resetSystem(){
        if(PcrApplication.is_RNA_connect){
            ////异步全流程
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
//            String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "99-reset.hex";
            File resetfile = new File(downandopen_filePath);
            boolean result = startAysnTask(resetfile);
            if(result) {
                showProgressDialog(getResources().getString(R.string.dialog_title_wait),
                        getResources().getString(R.string.dialog_message_reset));
            }else{
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                        getResources().getString(R.string.dialog_message_reset_error));
            }
        }else{
            Toast.makeText(LoginActivity.this,
                    getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
            hideProgressDialog();
            showAlarm();
            showNoticeDialog(getResources().getString(R.string.login_dialog_title_check_device),
                    getResources().getString(R.string.login_dialog_message_reset_error_in_disconnect));
        }
    }



    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {

        if(procedureFinishEvent.getTemplateName().contains("reset")){
            if(procedureFinishEvent.getStatus().equals("finish")){
                resetPcrRecMark();
                hideProgressDialog();
                seftCheckSuccess();
                PreferenceUtil.getInstance(this.getApplicationContext()).setValueByName(SharedConstant.IS_BOOT_UP_NEED_RESET, false);
                if(isRunningFlag){
                    openMainSetting();
//                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                    startActivity(intent);
                }
                PcrApplication.rNARunningMessage.setStatus("free");
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                Toast.makeText(LoginActivity.this,"LoginActivity reset TimeoutEvent  :  " + "finish error",Toast.LENGTH_LONG).show();
                hideProgressDialog();
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.login_dialog_title_check_device),
                        getResources().getString(R.string.login_dialog_message_reset_error));
            }
            stopRNARunning();

            if(PcrApplication.is_SLAN_PCR_connect) {
              //  SLANService.INSTANCE.stopExperiment(PcrApplication.devices.get(0).getSerialNo(), (byte) 0x01);
            }

        }
    }

    /**
     * 指令正在执行
     * @param rNARunningMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RNARunningMessage rNARunningMessage) {
        Log.i("RNA", "RNA指令运行1" + rNARunningMessage.getID() + "getStatus" + rNARunningMessage.getStatus());
        Log.i("RNA", "RNA指令运行2" + rNARunningMessage.getID() + "getNO" + rNARunningMessage.getNO());
        Log.i("RNA", "RNA指令运行3" + rNARunningMessage.getID() + "getDegree" + rNARunningMessage.getDegree());

        if(rNARunningMessage != null )
        {
            rnaHeartBeatCount = 0;
            PcrApplication.is_RNA_connect = true;
            PcrApplication.is_RecRunningMesaage = true;
            PcrApplication.rNARunningMessage = rNARunningMessage;
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
                    skip = true;
                    seftCheckSuccess();
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
    }


}
