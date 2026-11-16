package com.paul.pipeline.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkInfo;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.ActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.StepIndexMessage;
import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;
import com.gidi.bio_pcr.WintersUtils.entity.WarningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.activity.SystemInfoActivity;
import com.gidi.bio_pcr.bean.ProjectInfo;
import com.gidi.bio_pcr.bioway.SlanError;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.databinding.ActivityPipeMainBinding;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.listener.FragmentKeyeventListener;
import com.gidi.bio_pcr.mgr.AlarmUtils;
import com.gidi.bio_pcr.slan.bean.SlanFinishResult;
import com.gidi.bio_pcr.slan.fragment.AIoSlanExDetailFragment;
import com.gidi.bio_pcr.slan.fragment.SLANWellDetailFragment;
import com.gidi.bio_pcr.user.activity.UserBiowayActivity;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.LogcatHelper;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.WarnDialog;
import com.google.gson.Gson;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.dialog.ArmErrorDialog;
import com.paul.pipeline.fragment.ArmDetailFragment;
import com.paul.pipeline.fragment.OpendoorFragment;
import com.paul.pipeline.fragment.PcrDetailFragment;
import com.paul.pipeline.fragment.PipeMainFragment;
import com.paul.pipeline.fragment.RecordsFragment;
import com.paul.pipeline.fragment.RnaDetailFragment;
import com.paul.pipeline.fragment.SelfcheckFragment;
import com.paul.pipeline.fragment.SettingFragment;
import com.paul.pipeline.fragment.TrayDetailFragment;
import com.paul.pipeline.fragment.TrayManageFragment;
import com.paul.pipeline.fragment.UserLoginFragment;
import com.paul.pipeline.listener.AlarmListener;
import com.paul.pipeline.listener.ArmErrorCallback;
import com.paul.pipeline.listener.ArmResetCallback;
import com.paul.pipeline.listener.ArmWorkListener;
import com.paul.pipeline.listener.DeviceErrorCallback;
import com.paul.pipeline.listener.TemplateRunable;
import com.paul.pipeline.listener.UIinitCallback;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.AlarmInfoManager;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.manager.ExperimentManager;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.RNAManager;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.message.ArmworkMessage;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.gidi.bio_pcr.user.activity.UserMainActivity;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.paul.pipeline.message.ErrorInfoMessage;
import com.paul.pipeline.message.LogMessageEvent;
import com.winters.boweilibrary.BoWei.model.experiment.RunningInfo;
import com.winters.boweilibrary.autopipeline.AutoPipelineTestActivity;
import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_QRCode;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.event.WSSMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.SLAN_PCRFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.RobotManagerMessage;
import com.winters.boweilibrary.autopipeline.model.message.RobotManagerResultMessage;
import com.winters.boweilibrary.autopipeline.worker.RobotWorkManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class PipeMainActivity extends AppCompatActivity {
    final static String TAG = "PipeMainActivity";
    ActivityPipeMainBinding binding;
    PipeMainViewModel viewModel;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = PipeMainActivity.this;
        mUIHandler = new UIHandler(this);
        /// /初始化串口扫码设备（8联排二维码）
        /// 初始化机械臂
        SerialPortManager_Robot.getInstance().initRobotConnection(this);
        SerialPortManager_QRCode.getInstance().openSerialPortManager_QRCode(this);
        RobotWorkManager.getInstance().init(this);
        RobotWorkManager.getInstance().cancelAllWork();
        APLManager.getInstance().init(this);
        APLManager.getInstance().startWSSserver();
        APLManager.getInstance().setuIinitCallback(uIinitCallback);
//        binding = ActivityPipeMainBinding.inflate(getLayoutInflater());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pipe_main);
        setContentView(binding.getRoot());
        binding.setLifecycleOwner(this);
        // 获取或创建ViewModel实例
        viewModel = new ViewModelProvider(this).get(PipeMainViewModel.class);
        viewModel.initData(this);
//        binding.setViewModel(viewModel);
//        binding.executePendingBindings();
        initBinding();
        initViewModel();
        initFragment();
        AudioManager mAudioManager = (AudioManager)getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int alarmVolume = (int)((maxVolume * 4)/5);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, alarmVolume, AudioManager.FLAG_PLAY_SOUND);
        AlarmInfoManager.getInstance().setAlarmListener(new AlarmListener() {
            @Override
            public void alarm(String text, boolean sound) {
                sendAlarm(text);
                sendAlarmSound(sound);
            }
        });
        initTimer();
        registerEventBus();
        RNAManager.getInstance().setRnaErrorCallback(rnaErrorCallback);
        PCRManager.getInstance().setPcrErrorCallback(pcrErrorCallback);
//        initTask();//啓動任務
    }

//    APLService aPLBinder;
//    private ServiceConnection serviceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName className, IBinder service) {
//            // 服务绑定成功后调用，可以通过service与服务进行通信
//            APLService.APLBinder binder = (APLService.APLBinder) service;
//            aPLBinder = binder.getService();
//            // 使用Service进行接下来的操作...
//
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName arg0) {
//            // 服务意外断开时调用
//            aPLBinder = null;
//        }
//    };
//    public void bindAPLservice(){
//        // 启动任务分配检查
//        Intent intent = new Intent(this, APLService.class);
//        bindService(intent,serviceConnection, Context.BIND_AUTO_CREATE);
//    }
    private FragmentKeyeventListener fragmentKeyeventListener;
    public void  setFragmentKeyeventListener(FragmentKeyeventListener fragmentKeyeventListener) {
        this.fragmentKeyeventListener = fragmentKeyeventListener;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(null != fragmentKeyeventListener){
            fragmentKeyeventListener.onFragmentKeyEvent(event);
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    protected boolean isSecretSetting = false;
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        RobotWorkManager.getInstance().init(this);

        isSecretSetting = PreferenceUtil.getInstance(getApplicationContext())
                .getBooleanValue(SharedConstant.SECRET_SETTING,false);
        if(isSecretSetting){
            RNAAndPCRAsyncTask.retryCount_Value = 100;
        }else{
            RNAAndPCRAsyncTask.retryCount_Value = 20;
        }
        APLManager.getInstance().setArmWorkListener(armWorkListener);//机械臂执行功能
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
        if (null != mUIHandler) {
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        if (null != runnable) {
            runnable = null;
        }
        stopTimer();
    }

   private void initViewModel(){
       viewModel.getNavClickEvent().observe(this, new Observer<Integer>() {
           @Override
           public void onChanged(Integer integer) {
               openNav(integer);
           }
       });
       viewModel.getToastInfo().observe(this, s -> {
           sendToast(s);
       });
       viewModel.getAlertInfo().observe(this, s -> {
           showAlertDialog(null,s);
       });
       viewModel.getProgressInfo().observe(this, s -> {
           if(s==null||s.isEmpty()||s.isBlank()) {
               hideProgressDialog();
           }else{
               showProgressDialog(s);
           }
       });
   }
   // BiowayExDetailFragment biowayExDetailFragment;
   // WellDetailFragment wellDetailFragment;
    SLANWellDetailFragment slanWellDetailFragment;//宏石的样本详情
    RecordsFragment recordsFragment;
    SelfcheckFragment selfcheckFragment;
    PipeMainFragment mainFragment;
    TrayDetailFragment trayDetailFragment;
    UserLoginFragment userLoginFragment;
    OpendoorFragment opendoorFragment;
    TrayManageFragment trayManageFragment;
    RnaDetailFragment rnaDetailFragment;
    PcrDetailFragment pcrDetailFragment;
    ArmDetailFragment armDetailFragment;
    SettingFragment settingFragment;

    AIoSlanExDetailFragment slanExDetailFragment;

    private void initFragment() {
       // biowayExDetailFragment = new BiowayExDetailFragment();
        settingFragment = new SettingFragment();
        recordsFragment = new RecordsFragment();
        selfcheckFragment = new SelfcheckFragment();
        userLoginFragment = new UserLoginFragment();
        mainFragment = new PipeMainFragment();
        trayDetailFragment = new TrayDetailFragment();
        opendoorFragment = new OpendoorFragment();
        trayManageFragment = new TrayManageFragment();
        rnaDetailFragment  = new RnaDetailFragment();
        pcrDetailFragment = new PcrDetailFragment();
        armDetailFragment = new ArmDetailFragment();
       // wellDetailFragment = new WellDetailFragment();
        slanExDetailFragment = new AIoSlanExDetailFragment();
        slanWellDetailFragment = new SLANWellDetailFragment();
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.fragment_container_view,mainFragment)
                .add(R.id.fragment_container_view,selfcheckFragment)
                .add(R.id.fragment_container_view,userLoginFragment)
                .add(R.id.fragment_container_view,trayDetailFragment)
                .add(R.id.fragment_container_view,opendoorFragment)
                .add(R.id.fragment_container_view,trayManageFragment)
                .add(R.id.fragment_container_view,rnaDetailFragment)
                .add(R.id.fragment_container_view,pcrDetailFragment)
                .add(R.id.fragment_container_view,armDetailFragment)
                .add(R.id.fragment_container_view,recordsFragment)
                .add(R.id.fragment_container_view,settingFragment)
                .add(R.id.fragment_container_view,slanWellDetailFragment)
              //  .add(R.id.fragment_container_view,biowayExDetailFragment)
                .add(R.id.fragment_container_view,slanExDetailFragment)
                .hide(mainFragment)
                .hide(selfcheckFragment)
                .hide(settingFragment)
                .hide(recordsFragment)
                .hide(armDetailFragment)
                .hide(pcrDetailFragment)
                .hide(rnaDetailFragment)
                .hide(userLoginFragment)
                .hide(trayDetailFragment)
                .hide(opendoorFragment)
                .hide(trayManageFragment)
                .hide(slanWellDetailFragment)
                .hide(slanExDetailFragment)
                .show(userLoginFragment)
                .commit();
    }
   int lastnav = ConstantData.NAV_LOGIN;
   public void openNav(int value){
       FragmentManager fm = getSupportFragmentManager();
       FragmentTransaction ft = fm.beginTransaction();
       switch(lastnav){
           case ConstantData.NAV_MAIN:
               ft.hide(mainFragment);
               break;
           case ConstantData.NAV_SETTING:
               ft.hide(settingFragment);
               break;
           case ConstantData.NAV_SEIFCHECK:
               ft.hide(selfcheckFragment);
               break;
           case ConstantData.NAV_LOGIN:
               ft.hide(userLoginFragment);
               break;
           case ConstantData.NAV_RECORDS:
               ft.hide(recordsFragment);
               break;
           case ConstantData.NAV_TRAYMANAGE:
               ft.hide(mainFragment);
               ft.hide(trayManageFragment);
               break;
           case ConstantData.NAV_TRAYDETAIL:
               ft.hide(mainFragment);
               ft.hide(trayDetailFragment);
               break;
           case ConstantData.NAV_OPENDOOR:
               ft.hide(mainFragment);
               ft.hide(opendoorFragment);
               break;
           case ConstantData.NAV_DETAILRNA:
               ft.hide(mainFragment);
               ft.hide(rnaDetailFragment);
               break;
           case ConstantData.NAV_DETAILPCR:
               ft.hide(mainFragment);
               ft.hide(pcrDetailFragment);
               break;
           case ConstantData.NAV_DETAILARM:
               ft.hide(mainFragment);
               ft.hide(armDetailFragment);
               break;
           case ConstantData.NAV_WELLDETAIL:
               ft.hide(slanWellDetailFragment);
               break;
           /*case ConstantData.NAV_BiowayExDetail:
               ft.hide(biowayExDetailFragment);*/
           case ConstantData.NAV_SLANExDetail:
               ft.hide(slanExDetailFragment);
               break;
           default:
               break;
       }
        switch(value){
            case ConstantData.NAV_SETTING:
                ft.show(settingFragment);
                break;
            case ConstantData.NAV_SEIFCHECK:
                APLManager.getInstance().SelfCheck = false;
                ft.show(selfcheckFragment);
                break;
            case ConstantData.NAV_LOGIN:
                ft.show(userLoginFragment);
                break;
            case ConstantData.NAV_MAIN:
                APLManager.getInstance().SelfCheck = true;
                ft.show(mainFragment);
                break;
            case ConstantData.NAV_RECORDS:
                ft.show(recordsFragment);
                break;
            case ConstantData.NAV_TRAYDETAIL:
                ft.show(mainFragment);
                ft.show(trayDetailFragment);
                break;
            case ConstantData.NAV_OPENDOOR:
                ft.show(mainFragment);
                ft.show(opendoorFragment);
                break;
            case ConstantData.NAV_TRAYMANAGE:
                ft.show(mainFragment);
                ft.show(trayManageFragment);
                break;
            case ConstantData.NAV_DETAILRNA:
                ft.show(mainFragment);
                ft.show(rnaDetailFragment);
                break;
            case ConstantData.NAV_DETAILPCR:
                ft.show(mainFragment);
                ft.show(pcrDetailFragment);
                break;
            case ConstantData.NAV_DETAILARM:
                ft.show(mainFragment);
                ft.show(armDetailFragment);
                break;
            case ConstantData.NAV_WELLDETAIL:
                ft.show(slanWellDetailFragment);
                break;
           /* case ConstantData.NAV_BiowayExDetail:
                ft.show(biowayExDetailFragment);
                break;*/
            case ConstantData.NAV_SLANExDetail:
                ft.show(slanExDetailFragment);
                break;
            default:
                break;
        }
        ft.commit();
        lastnav = value;
   }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mUIHandler.sendEmptyMessage(Constants.MSG_UPDATE_SYSTEM_TIME);
            mUIHandler.postDelayed(runnable, 1000);
        }
    };
    private void initUpdateSysTime() {
        mUIHandler.postDelayed(runnable, 1000);
    }
    UIHandler mUIHandler;
    private void initBinding() {
        binding.stopImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openTestwinter();
            }
        });
        binding.settingIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNav(ConstantData.NAV_SETTING);
               // openSetting();
            }
        });
        binding.mainBackIv.setOnClickListener(v -> {
                openMain();
        });
        binding.opendoorImg.setOnClickListener(v-> {
            openDoor();
        });

        binding.pcrRecordsTv.setOnClickListener(v -> {
           openResult();
        });
        binding.pcrSoundTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearCountingEnd();
                clearAlarm();
                binding.llAlarm.setVisibility(View.INVISIBLE);
            }
        });
        binding.selfcheckTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNav(ConstantData.NAV_SEIFCHECK);
            }
        });
        initUpdateSysTime();
        initVolumeState();
    }
    private AudioManager mAudioManager;
    private int alarmVolume;
    private boolean isMute = false;

    private void initVolumeState() {
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        alarmVolume = (int) ((maxVolume * 4) / 5);
        int currentVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (0 == currentVol) {
            binding.pcrSoundTv.setImageResource(R.drawable.icon_mute);
            isMute = true;
        } else {
            binding.pcrSoundTv.setImageResource(R.drawable.icon_no_mute);
            isMute = false;
        }
    }

    private void onBack() {
//        if (isPCRRunning || RNAAndPCRAsyncTask.getInstance().ismIsrunning()) {
//            showNoticeDialog(getResources().getString(R.string.running_dialog_title_equipment_in_process),
//                    getResources().getString(R.string.running_dialog_message_equipment_in_process));
//            return;
//        }
//        RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
    }
    private void openMain(){
        if(lastnav==ConstantData.NAV_SEIFCHECK){
            showToast("自检页面中主页按钮不可用");
        }else if(lastnav==ConstantData.NAV_LOGIN){
            showToast("登录页面中主页按钮不可用");
        }else {
            openNav(ConstantData.NAV_MAIN);
        }
    }
    private void openDoor(){
        openNav(ConstantData.NAV_OPENDOOR);
    }
    private void openResult(){
        openNav(ConstantData.NAV_RECORDS);
//        Intent intent = new Intent(PipeMainActivity.this, ResultActivity.class);
//        intent.putExtra("SettingIn",false);
//        startActivity(intent);
    }
    private void openSetting(){
        Intent infoIntent = new Intent(PipeMainActivity.this, SystemInfoActivity.class);
        startActivity(infoIntent);
    }
    private void openUserMain(){
        Intent intent = new Intent(PipeMainActivity.this, UserMainActivity.class);
        startActivity(intent);
    }
    private void openTestwinter() {
        Intent intent = new Intent(PipeMainActivity.this, AutoPipelineTestActivity.class);
        startActivity(intent);
    }
    private void openTestbio() {
        Intent intent = new Intent(PipeMainActivity.this, UserBiowayActivity.class);
        startActivity(intent);
    }
    public void checkTimer(){
        try{
            if(timer==null||timerTask==null)//安排机械臂任务
            {
                Log.e("ArmTaskWorker", "定时检查timer init");
                initTimer();
            }else{
              //  Log.i("ArmTaskWorker", "定时检查timer ok");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ArmTaskWorker", e.getMessage());
        }
    }
    public static class UIHandler extends Handler {

        WeakReference<PipeMainActivity> mActivityReference;

        public UIHandler(PipeMainActivity mActivity) {
            mActivityReference = new WeakReference<PipeMainActivity>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final PipeMainActivity mActivity = mActivityReference.get();
            if (null == mActivity) {
                return;
            }
            switch (msg.what) {
                case ConstantData.MSG_TOAST:
                    if(msg.obj!=null) {
                        String text = (String) msg.obj;
                        mActivity.showToast(text);
                    }
                    break;
                case Constants.MSG_UPDATE_SYSTEM_TIME:
                    mActivity.binding.runningTimeLl.setDateText(DateFormatUtil.getNowDate());
                    mActivity.binding.runningTimeLl.setTimeText(DateFormatUtil.getNowTime());
                    mActivity.checkTimer();
                    break;
                case ConstantData.MSG_ALARM:
                    if(msg.obj!=null) {
                        String text = (String) msg.obj;
                        mActivity.setAlarmContent(text);
                    }
                    break;
                case ConstantData.MSG_ALARMSound:
                    if(msg.obj!=null) {
                        Boolean value = (Boolean) msg.obj;
                        if(value) {
                            mActivity.binding.llAlarm.setVisibility(View.VISIBLE);
                            mActivity.showAlarm();
                        }else{
                            mActivity.binding.llAlarm.setVisibility(View.INVISIBLE);
                            mActivity.clearAlarm();
                        }
                    }
                    break;
                case ConstantData.MSG_SoundBB:
                    if(msg.obj!=null) {
                        Boolean value = (Boolean) msg.obj;
                        if(value) {
                            mActivity.showCountingEnd();
                        }else{
                            mActivity.clearCountingEnd();
                        }
                    }
                    break;
                case ConstantData.MSG_ARMTASK:
                    mActivity.checkArmtask();
                    break;
                case ConstantData.MSG_SAVE_PCR:
                    if(msg.obj!=null) {
                        int index = (int) msg.obj;
                        PCRManager.getInstance().saveModel(index);
                    }
                    break;
                case ConstantData.MSG_SAVE_RNA:
                    if(msg.obj!=null) {
                        int index = (int) msg.obj;
                        RNAManager.getInstance().saveModel(index);
                    }
                    break;
                case ConstantData.MSG_SAVE_TRAY:
                    if(msg.obj!=null) {
                        int index = (int) msg.obj;
                        TrayManager.getInstance().saveModel(index);
                    }
                    break;
                case ConstantData.MSG_SAVE_ARM:
                    if(msg.obj!=null) {
                        int index = (int) msg.obj;
                        ArmManager.getInstance().saveModel(index);
                    }
                    break;
                case ConstantData.MSG_ARM_ERROR:
                    if(msg.obj!=null) {
                        ErrorInfoMessage errormsg = (ErrorInfoMessage) msg.obj;
                        String alarmtext = errormsg.getAlarm()==null?"":errormsg.getAlarm();
                        String text =  alarmtext+ "\n" + mActivity.getString(R.string.error_arm_reset);
//                        if(errormsg.isReset()){
                            text = text + "\n" + mActivity.getString(R.string.error_arm_reset_step);
//                        }
                        mActivity.showArmErrorDialog(ConstantData.DEVICE_ARM, errormsg.getIndex(),text);
                        //mActivity.getString(R.string.pipe_main_linuxalarm)
                    }
                    break;
                case ConstantData.MSG_RNA_ERROR:
                    if(msg.obj!=null) {
                        ErrorInfoMessage errormsg = (ErrorInfoMessage) msg.obj;
                        String text = "";
                        if(errormsg.getAlarm()!=null){
                            text += "alarm:"+ errormsg.getAlarm() +" \n";
                        }
                        if(errormsg.getLinuxalarm()!=null){
                            text +=  "linuxalarm:"+errormsg.getLinuxalarm() +" \n";
                        }
                        if(errormsg.getSendalarm()!=null){
                            text += "sendalarm:"+ errormsg.getSendalarm() +" \n";
                        }
                        if(errormsg.getMotoid()!=null){
                            text += "motoid:"+ errormsg.getMotoid() +" \n";
                        }
                        text += errormsg.getIndex() + " " + mActivity.getString(R.string.error_rna_reset);
                        if(errormsg.isReset()){
                            text += mActivity.getString(R.string.error_arm_reset_step);// mActivity.getString(R.string.error_confirm_reset);
                        }
                        if(errormsg.isAlarmdialog()) {
                            mActivity.showArmErrorDialog(ConstantData.DEVICE_RNA, errormsg.getIndex(), text);
                            //mActivity.getString(R.string.pipe_main_linuxalarm)
                        }
                    }
                    break;
                case ConstantData.MSG_PCR_ERROR:
                    if(msg.obj!=null) {
                        ErrorInfoMessage errormsg = (ErrorInfoMessage) msg.obj;
                        String text = "";
                        if(errormsg.getAlarm()!=null){
                            text += "alarm:"+ errormsg.getAlarm() +" \n";
                        }
                        if(errormsg.getLinuxalarm()!=null){
                            text +=  "linuxalarm:"+errormsg.getLinuxalarm() +" \n";
                        }
                        if(errormsg.getSendalarm()!=null){
                            text += "sendalarm:"+ errormsg.getSendalarm() +" \n";
                        }
                        text += errormsg.getIndex() + " " + mActivity.getString(R.string.error_pcr_reset);
                        if(errormsg.isReset()){
                            text += mActivity.getString(R.string.error_arm_reset_step);//mActivity.getString(R.string.error_confirm_reset);
                        }
                        if(errormsg.isAlarmdialog()) {
                            mActivity.showArmErrorDialog(ConstantData.DEVICE_PCR, errormsg.getIndex(), text);
                            //mActivity.getString(R.string.pipe_main_linuxalarm)
                        }
                    }
                    break;
                case ConstantData.MSG_RESTOPWSS:
                    APLManager.getInstance().stopWSSserver();
                    break;
                case ConstantData.MSG_RESTARTWSS:
                    APLManager.getInstance().startWSSserver();
                    break;
                case ConstantData.MSG_START_PCR:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;
                        Log.i("ArmTaskWorker", "【PCR启动】MSG_START_PCR:"+target+" 清空曲线图");
                        // ⭐ 新实验开始，先清空曲线图
                        mActivity.mainFragment.pcrClear(target);
                        // 然后设置运行状态
                        APLManager.getInstance().checkPcrRunTemplate(target);
                        PCRManager.getInstance().saveModel(target);
                    }
                    break;
                case ConstantData.MSG_Finish_PCR:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;
                        Log.e("ArmTaskWorker", "【Handler收到】MSG_Finish_PCR:"+target);
                        mActivity.mainFragment.pcrFinish(target);
                        boolean checkResult = APLManager.getInstance().checkPcrFinish(target);
                        Log.e("ArmTaskWorker", "【Handler】checkPcrFinish(" + target + ") 返回: " + checkResult);
                        if(checkResult) {
                            PCRManager.getInstance().saveModel(target);
                        } else {
                            Log.e("ArmTaskWorker", "【Handler】checkPcrFinish 返回 false，未触发还盘逻辑");
                        }
                    } else {
                        Log.e("ArmTaskWorker", "【Handler断点】MSG_Finish_PCR msg.obj 为 null！");
                    }
                    break;
                case ConstantData.MSG_FinishError_PCR:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;
                        Log.e("ArmTaskWorker", "MSG_FinishError_PCR:"+target);
                        APLManager.getInstance().NotifyPcrTrayError(target);
                    }
                    break;


                case ConstantData.MSG_Reset_PCR:
                    if(msg.obj!=null) {
                        int target = (int) msg.obj;
                        mActivity.sendToast("PCR"+target+"复位成功");
                        PCRManager.getInstance().saveModel(target);
                    }
                    break;
                case ConstantData.MSG_CLEAR_PCR:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;
                        Log.e("ArmTaskWorker", "MSG_CLEAR_PCR:"+target);
                        if(target>=1&&target<=6) {
                            mActivity.mainFragment.pcrClear(target);
                            PCRManager.getInstance().saveModel(target);
                        }
                    }
                    break;

                case ConstantData.MSG_Finish_SLAN_PCR:
                    if(msg.obj!=null) {
                        SlanFinishResult slanFinishResult = (SlanFinishResult)msg.obj;
                        SLAN_PCRFinishMessage slan_pcrFinishMessage = slanFinishResult.getSlan_pcrFinishMessage();
                        if(null != slan_pcrFinishMessage){
                            int target = slan_pcrFinishMessage.getPosition();
                            String savePcrFilePath = slanFinishResult.getFilePath();
                            Log.e("slanFinish", "【优化】MSG_Finish_SLAN_PCR 收到消息，PCR:" + target + " 立即触发还盘");

                            // ⭐ 优化1：先触发还盘逻辑（不阻塞）
                            if(APLManager.getInstance().checkPcrFinish(target)) {
                                Log.i("slanFinish", "【优化】checkPcrFinish 成功，armStep 已设置为 WORK_BACK");
                                PCRManager.getInstance().saveModel(target);
                            } else {
                                Log.e("slanFinish", "【优化】checkPcrFinish 失败，armStep 未设置为 WORK_BACK");
                            }

                            // ⭐ 优化2：耗时的文件打开/数据库操作提交到后台队列
                            Log.i("slanFinish", "【优化】提交后台任务：打开文件并保存数据库");
                            mActivity.mainFragment.slanPcrFinish(savePcrFilePath, target);
                        }
                    }
                    break;
                case ConstantData.MSG_START_RNA:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;

                        // 🔍 调度分析日志：追踪谁触发了启动消息
                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                        StringBuilder caller = new StringBuilder();
                        for (int i = 3; i < Math.min(stackTrace.length, 8); i++) {
                            caller.append(stackTrace[i].getClassName()).append(".").append(stackTrace[i].getMethodName()).append(" -> ");
                        }
                        Log.e("DispatchAnalysis", String.format(
                            "[启动触发] RNA:%d MSG_START_RNA 被触发，调用栈: %s",
                            target,
                            caller.toString()
                        ));

                        Log.i("ArmTaskWorker", "【RNA启动】MSG_START_RNA:"+target+" 设置运行状态");
                        // ❌ 不要调用 rnaClear - RNA 没有曲线图，且会清空 currentTray 等重要信息
                        // 只设置运行状态即可
                        APLManager.getInstance().checkRnaRunTemplate(target);
                        RNAManager.getInstance().saveModel(target);
                    }
                    break;
                case ConstantData.MSG_Reset_RNA:
                    if(msg.obj!=null) {
                        int target = (int) msg.obj;
                        mActivity.sendToast("核提"+target+"复位成功");
                        RNAManager.getInstance().saveModel(target);
                    }
                    break;
                case ConstantData.MSG_Finish_RNA:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;
                        Log.e("ArmTaskWorker", "MSG_Finish_RNA:"+target);
                        if(APLManager.getInstance().checkRnaFinish(target)){
                            RNAManager.getInstance().saveModel(target);
                            Log.e("ArmTaskWorker", "MSG_Finish_PCR checkRnaFinish:"+target);
                        }
                    }
                    break;
                case ConstantData.MSG_FinishError_RNA:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;
                        Log.e("ArmTaskWorker", "MSG_FinishError_RNA:"+target);
                        APLManager.getInstance().NotifyRnaTrayError(target);
                    }
                    break;
                case ConstantData.MSG_CLEAR_RNA:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;
                        Log.e("ArmTaskWorker", "MSG_CLEAR_RNA:"+target);
                        if(target>=1&&target<=2) {
                            mActivity.mainFragment.rnaClear(target);
                            RNAManager.getInstance().saveModel(target);
                        }
                    }
                    break;
                case ConstantData.MSG_ARM_WORK:
                    if(msg.obj!=null) {
                        ArmworkMessage msge = (ArmworkMessage)msg.obj;
                        mActivity.doArmWork(msge);
                    }
                    break;
                case ConstantData.MSG_CLEAR_TRAY:
                    if(msg.obj!=null) {
                        int target = (int)msg.obj;
                        Log.e("ArmTaskWorker", "MSG_CLEAR_TRAY:"+target);
                        if(target>=1&&target<=8) {
                            mActivity.mainFragment.trayClear(target);
                            TrayManager.getInstance().saveModel(target);
                        }
                    }
                    break;
                case ConstantData.MSG_CLEAR_ALL:
                    mActivity.mainFragment.refreshAll();
                    break;
            }
        }

    }
    public void sendMessage(int index,Object value){
        if(mUIHandler!=null) {
            Message msg = mUIHandler.obtainMessage();
            msg.what = index;
            msg.obj = value;
            mUIHandler.sendMessage(msg);
        }
    }
    public void sendEmptyMessage(int value){
        if(mUIHandler!=null) {
            Message msg = mUIHandler.obtainMessage();
            msg.what = value;
            mUIHandler.sendMessage(msg);
        }
    }
    public void sendAlarmSound(Boolean text){
        if(mUIHandler!=null&&text!=null) {
            Message msg = mUIHandler.obtainMessage();
            msg.obj = text;
            msg.what = ConstantData.MSG_ALARMSound;
            mUIHandler.sendMessage(msg);
        }
    }
    public void sendAlarm(String text){
        if(mUIHandler!=null&&text!=null) {
            Message msg = mUIHandler.obtainMessage();
            msg.obj = text;
            msg.what = ConstantData.MSG_ALARM;
            mUIHandler.sendMessage(msg);
        }
    }
    public void sendToast(String text){
        if(mUIHandler!=null&&text!=null) {
            Message msg = mUIHandler.obtainMessage();
            msg.obj = text;
            msg.what = ConstantData.MSG_TOAST;
            mUIHandler.sendMessage(msg);
        }
    }
    private void showToast(String text){
        Toast.makeText(context, ""+text,Toast.LENGTH_LONG).show();
    }
    public void showAlarm(){
        if(!AlarmUtils.getInstance(getApplicationContext()).isSystemAlarm()){
            AlarmUtils.getInstance(getApplicationContext()).setSystemAlarm(true);
            if(AlarmUtils.getInstance(getApplicationContext()).getPlayPosition()!= 3){
                AlarmUtils.getInstance(getApplicationContext()).startplay(3);
            }
        }
    }

    public void clearAlarm(){
        if(AlarmUtils.getInstance(getApplicationContext()).isSystemAlarm()){
            AlarmUtils.getInstance(getApplicationContext()).setSystemAlarm(false);
            AlarmUtils.getInstance(getApplicationContext()).stopPlay();
        }
    }

    public void showCountingEnd(){
        Log.i("Single","showCountingEnd");
        if(!AlarmUtils.getInstance(getApplicationContext()).isCountEnd()){
            AlarmUtils.getInstance(getApplicationContext()).setCountEnd(true);
            if(AlarmUtils.getInstance(getApplicationContext()).getPlayPosition()!= 2){
                AlarmUtils.getInstance(getApplicationContext()).startplay(2);
            }
        }
    }

    public void clearCountingEnd(){
        Log.i("Single","clearCountingEnd");
        if(AlarmUtils.getInstance(getApplicationContext()).isCountEnd()){
            AlarmUtils.getInstance(getApplicationContext()).setCountEnd(false);
            AlarmUtils.getInstance(getApplicationContext()).stopPlay();
        }
    }
    public void resetErrorArm(){
        ArmManager.getInstance().robotReset(new ArmResetCallback() {
            @Override
            public void finish(boolean result, String message) {

            }
        });
        clearAlarm();
    }
    public void continueErrorArm(){
        ArmManager.getInstance().errorContinue();
        clearAlarm();
    }
    public void resetErrorRNA(int index){
        RNAManager.getInstance().reset(index);
        clearAlarm();
    }
    public void continueErrorRna(int index){
        RNAManager.getInstance().errorContinue(index);
        clearAlarm();
    }
    public void resetErrorPCR(int index){
        PCRManager.getInstance().reset(index);
        clearAlarm();
    }
    public void continueErrorPcr(int index){
        PCRManager.getInstance().errorContinue(index);
        clearAlarm();
    }
    private WarnDialog alertDialog;
    public void showAlertDialog(String title,String message) {
        if(title==null||title.isEmpty()){
            title = getString(R.string.dialog_title_attention);
        }
        if (null != alertDialog) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        if (null == alertDialog) {
            alertDialog = new WarnDialog(this);
            LayoutInflater mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView) view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);
            WarnDialog.Builder builder = new WarnDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setSingleButton(R.string.dialog_ok, (arg0, arg1) -> {
                alertDialog.dismiss();
                alertDialog = null;
            });
            alertDialog = builder.create();
            alertDialog.show();
        }
    }
    boolean ErrorDialogShowing = false;
    private NoticeDialog mErrorDialog;
    //int type, int index,boolean reset
    public void showErrorDialog(String message) {
        if(!ErrorDialogShowing) {
            if (null != mErrorDialog) {
                mErrorDialog.dismiss();
                mErrorDialog = null;
            }
            if (null == mErrorDialog) {
                mErrorDialog = new NoticeDialog(this, R.style.Dialog);
                mErrorDialog.setMessage(message);
                mErrorDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                    @Override
                    public void onConfirmClick() {
                        mErrorDialog.dismiss();
                        ErrorDialogShowing = false;
//                        if(reset) {
//                            if (type == ConstantData.DEVICE_ARM) {
//                                resetErrorArm();
//                            } else if (type == ConstantData.DEVICE_RNA) {
//                                resetErrorRNA(index);
//                            } else if (type == ConstantData.DEVICE_PCR) {
//                                resetErrorPCR(index);
//                            }
//                        }
                        clearAlarm();
                    }
                });

                mErrorDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                    @Override
                    public void onCancelClick() {
                        mErrorDialog.dismiss();
                        ErrorDialogShowing = false;
                        clearAlarm();
                    }
                });
                mErrorDialog.show();
                ErrorDialogShowing = true;
                showAlarm();
            }
        }
    }

    boolean armErrorDialogShowing = false;
    private ArmErrorDialog mArmErrorDialog;
    public void showArmErrorDialog(int type, int index,String message) {
        if(!armErrorDialogShowing) {
            if (null != mArmErrorDialog) {
                mArmErrorDialog.dismiss();
                mArmErrorDialog = null;
            }
            if (null == mArmErrorDialog) {
                ArmErrorDialog.Builder builder = new ArmErrorDialog.Builder(this);
                        builder.setMessage(message);
                builder.setResetButton(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (type == ConstantData.DEVICE_ARM) {
                            resetErrorArm();
                        } else if (type == ConstantData.DEVICE_RNA) {
                            resetErrorRNA(index);
                        } else if (type == ConstantData.DEVICE_PCR) {
                            resetErrorPCR(index);
                        }
                    }
                });
                builder.setCotinueButton(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mArmErrorDialog.dismiss();
                        armErrorDialogShowing = false;
                        if (type == ConstantData.DEVICE_ARM) {
                            continueErrorArm();
                        } else if (type == ConstantData.DEVICE_RNA) {
                            continueErrorRna(index);
                        } else if (type == ConstantData.DEVICE_PCR) {
                            continueErrorPcr(index);
                        }

                    }
                });
                builder.setNegativeButton(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mArmErrorDialog.dismiss();
                        armErrorDialogShowing = false;
                        clearAlarm();
                    }
                });
                mArmErrorDialog = builder.create();
                mArmErrorDialog.show();
                armErrorDialogShowing = true;
                showAlarm();
            }
        }
    }
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
//
//        if (procedureFinishEvent.getTemplateName().contains("downforerror")) {
//            if (procedureFinishEvent.getStatus().equals("finish")) {
//            //    hideProgressDialog();
//            } else if (procedureFinishEvent.getStatus().equals("finish error")) {
//
//                Toast.makeText(getApplicationContext(), "SendCmdTimeoutEvent  :  " + "finish error11111111111", Toast.LENGTH_LONG).show();
//            //    hideProgressDialog();
//                showAlarm();
//                showNoticeDialog(getResources().getString(R.string.dialog_title_check_equipment), "万能下机指令出错，请检查设备!");
//                AlarmInfoManager.getInstance().setText("E19-"+getResources().getString(R.string.e19_alarm_offline_universal_command_error));
//            }
//        } else if (procedureFinishEvent.getTemplateName().contains("downandopen")) {
////            if(PcrApplication.is_SLAN_PCR_connect) {
////                ExperimentManager.getInstance().stopExperiment();
////            }
////            if (procedureFinishEvent.getStatus().equals("finish")) {
////                isdownAndOpen = false;//20250208
////                PcrApplication.rNARunningMessage.setStatus("free");
////            } else if (procedureFinishEvent.getStatus().equals("finish error")) {
////                showAlarm();
////                isdownAndOpen = false;//20250208
////                showNoticeDialog(getResources().getString(R.string.dialog_title_check_equipment), getResources().getString(R.string.running_dialog_message_offline_command_error));
////                AlarmInfoManager.getInstance().setText("E18-"+getResources().getString(R.string.e18_alarm_offline_command_error));
////            }
////            stopCountDown();
//        }else{
//            if(procedureFinishEvent.getStatus().contains("error")){
//
//            }
//
//        }
//    }
    private void setAlarmContent(String text){
        binding.alarmMsgTv.setText(text+"");
    }

    private CustomDialog stopDialog,downDialog,updateDbDialog;
    private void showUpdateDbDialog() {
        if (null != updateDbDialog) {
            updateDbDialog.dismiss();
            updateDbDialog = null;
        }
        if (null == updateDbDialog) {
            updateDbDialog = new CustomDialog(this);
            LayoutInflater mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_interrupt_detect_layout, null);
            TextView hintTv = (TextView) view.findViewById(R.id.dialog_interrupt_hint_tv);
            hintTv.setText(R.string.dialog_message_update_ex);
            CustomDialog.Builder builder = new CustomDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(R.string.dialog_title_prompt);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub

                    ExperimentManager.getInstance().updateExperiment();
                    updateDbDialog.dismiss();
                    updateDbDialog = null;
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    updateDbDialog.dismiss();
                    updateDbDialog = null;
                }
            });
            updateDbDialog = builder.create();
            updateDbDialog.setCancelable(false);
            updateDbDialog.show();
        }
    }
    private LoadingDialog loadingDialog ;
    public void showProgressDialog(String message){
        if(null == loadingDialog && null != this  &&  !this.isFinishing()){
            loadingDialog = new LoadingDialog(this, message);
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }
    }

    /*
     * 隐藏提示加载
     */
    public void hideProgressDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }
    Timer timer;
    TimerTask timerTask;
    private void initTimer(){
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 在这里执行你的后台任务
                sendEmptyMessage(ConstantData.MSG_ARMTASK);
            }
        };
        timer.schedule(timerTask, 1000L, ConstantData.ARMTASK_PERIOD);
    }
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
    private void checkArmtask() {
        try {
    //            Log.i("ArmTaskWorker", "执行后台任务 checkArmTask");
            checkwssStart();
            APLManager.getInstance().refreshConnectinfo();
            APLManager.getInstance().checkArmTask();//安排机械臂任务
        } catch (Exception e) {
            Log.e("ArmTaskWorker", e.getMessage());
        }
    }
//    private void initTask() {
//        Log.i("ArmTaskWorker", "定时保活任務");
////        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)  // 例如，需要网络连接才能执行任务
////                .build();
//        //                        .setConstraints(constraints)
//        // 必须大于15分钟执行一次
//        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(ArmTaskWorker.class, 15, TimeUnit.MINUTES).build();
//        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
//        workManager.enqueueUniquePeriodicWork("AutoPipeLineArmTaskWorker", ExistingPeriodicWorkPolicy.KEEP, periodicWorkRequest);
//        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.getId()).observe(this, new Observer<WorkInfo>() {
//            @Override
//            public void onChanged(WorkInfo workInfo) {
//                if(workInfo!=null&&workInfo.getState().isFinished()){
//                    Log.w("ArmTaskWorker", "aplservice 检查结果："+workInfo.getState().toString());
//                    switch(workInfo.getState()){
//                        case FAILED:
//                            bindAPLservice();
//                            Log.w("ArmTaskWorker", "aplservice 启动");
//                        break;
//                        case SUCCEEDED:
//                            Log.i("ArmTaskWorker", "aplservice 运行正常");
//                            break;
//                    }
//                }
//            }
//        });
//    }

    private void sendARMError(ArmworkMessage armmsg){
        ErrorInfoMessage msg = new ErrorInfoMessage();
        msg.setIndex(1);
        msg.setType(ConstantData.DEVICE_ARM);
        msg.setReset(true);
        msg.setArmworkMessage(armmsg);
        msg.setAlarm(armmsg.getArmerrortext());
        sendMessage(ConstantData.MSG_ARM_ERROR,msg);
    }

    DeviceErrorCallback rnaErrorCallback = new DeviceErrorCallback() {
        @Override
        public void error(ErrorInfoMessage message) {
            sendMessage(ConstantData.MSG_RNA_ERROR,message);
        }
    };
    DeviceErrorCallback pcrErrorCallback = new DeviceErrorCallback() {
        @Override
        public void error(ErrorInfoMessage message) {
            sendMessage(ConstantData.MSG_PCR_ERROR,message);
        }
    };
    ArmErrorCallback armErrorCallback = new ArmErrorCallback() {
        @Override
        public void error(ArmworkMessage msg) {
            Log.e("ArmTaskWorker", "机械臂运行失败！");
            sendToast("机械臂运行失败！");
//            ArmManager.getInstance().stop();
            sendARMError(msg);
        }
    };
    ArmWorkListener armWorkListener = new ArmWorkListener() {
        @Override
        public void executeWork(ArmworkMessage msg) {
            sendMessage(ConstantData.MSG_ARM_WORK,msg);
        }
    };
    //界面初始化
    UIinitCallback uIinitCallback = new UIinitCallback() {
        @Override
        public void finish(int type,int value) {
            switch(type){
                case ConstantData.DEVICE_RNA:
                    if(value>=1&&value<=2) {
                       sendMessage(ConstantData.MSG_CLEAR_RNA,value);
                    }
                    break;
                case ConstantData.DEVICE_PCR:
                    if(value>=1&&value<=6){
                        sendMessage(ConstantData.MSG_CLEAR_PCR,value);
                    }
                    break;
                case ConstantData.DEVICE_TRAY:
                    if(value>=1&&value<=7){
                        sendMessage(ConstantData.MSG_CLEAR_TRAY,value);
                    }
                    break;
                default:
                    sendMessage(ConstantData.MSG_CLEAR_ALL,value);
                    break;
            }
        }
    };

    private void doArmWork(ArmworkMessage msg){
        if(msg!=null) {
            switch(msg.getType()){
                case ConstantData.TASK_TRAY_LED:
                    try {
                        ArmManager.getInstance().setLed(msg.getTarget(),msg.getStatus());//开关led
                    } catch (Exception e) {
                        Log.e(TAG, "setLed" + e.getMessage());
                    }
                    break;
                case ConstantData.TASK_TRAY_MOVE:
                    Log.e("ArmTaskWorker", "机械臂移动托盘："+ msg.getSource() +"到"+msg.getTarget());
                    String moveresult = APLManager.getInstance().startArmTaskMoveTray(msg,armErrorCallback);
                    if(moveresult!=null){
                        sendAlarm(" " + moveresult);
                    }
                    break;
                case ConstantData.TASK_START_RNA:
                    Log.e("ArmTaskWorker", "机械臂 启动RNA"+msg.getTarget()+" 托盘"+msg.getSource());
                    String rnastartresult = APLManager.getInstance().startArmTaskGetTray(msg,armErrorCallback);
                    if(rnastartresult!=null){
                        sendAlarm(" " + rnastartresult);
                    }
                    break;
                case ConstantData.TASK_FINISH_RNA:
                    Log.e("ArmTaskWorker", "机械臂 结束RNA"+ msg.getSource() +" 托盘"+msg.getTarget());
                    String rnafinishresult = APLManager.getInstance().startArmTaskBackTray(msg,armErrorCallback);
                    if(rnafinishresult!=null){
                        sendAlarm(" " + rnafinishresult);
                    }
                    break;
                case ConstantData.TASK_START_PCR:
                    Log.e("ArmTaskWorker", "机械臂 启动PCR"+msg.getTarget()+" 托盘"+msg.getSource());
                    String pcrstartresult = APLManager.getInstance().startArmTaskGetTray(msg,armErrorCallback);
                    if(pcrstartresult!=null){
                        sendAlarm(" " + pcrstartresult);
                    }
                    break;
                case ConstantData.TASK_FINISH_PCR:
                    Log.e("ArmTaskWorker", "机械臂 结束PCR"+ msg.getSource() +" 托盘"+msg.getTarget());
                    String pcrfinishresult = APLManager.getInstance().startArmTaskBackTray(msg,armErrorCallback);
                    if(pcrfinishresult!=null){
                        sendAlarm(" " + pcrfinishresult);
                    }
                    break;
                case ConstantData.TASK_RUN_PCR:
                    Log.e("ArmTaskWorker", "启动PCR模板"+msg.getTarget());
                    sendpcrrun(msg.getTarget());
                    break;
                case ConstantData.TASK_RUN_RNA:
                    Log.e("ArmTaskWorker", "启动RNA模板"+msg.getTarget());
                    sendrnarun(msg.getTarget());
                    break;
                case ConstantData.TASK_ARM_ERROR:
                    Log.e("ArmTaskWorker", "机械臂异常");
                    sendAlarm(msg.getArmerrortext()+"");
                    break;

            }
        }else{
            Log.e("ArmTaskWorker", "ArmworkMessage msg null");
        }
    }

    protected void registerEventBus(){
        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }
    protected void unRegisterEventBus(){
        if(EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().unregister(this);
        }
    }

    TemplateRunable rnastartrunnable = new TemplateRunable() {

        public int target=0;
        @Override
        public TemplateRunable setIndex(Integer value) {
            target = value;
            return this;
        }

        @Override
        public void run() {
            sendMessage(ConstantData.MSG_START_RNA,target);
        }
    };

    public void sendrnarun(int target){
        rnastartrunnable.setIndex(target);
        mUIHandler.postDelayed(rnastartrunnable, ConstantData.RNATASK_PERIOD);
    }

    TemplateRunable pcrstartrunnable = new TemplateRunable() {

        public int pcr_target=0;
        @Override
        public TemplateRunable setIndex(Integer value) {
            pcr_target = value;
            return this;
        }

        @Override
        public void run() {
            sendMessage(ConstantData.MSG_START_PCR,pcr_target);
        }
    };

    public void sendpcrrun(int target){
        pcrstartrunnable.setIndex(target);
        mUIHandler.postDelayed(pcrstartrunnable, ConstantData.PCRTASK_PERIOD);
    }

    Runnable restartrunnable = new Runnable() {
        @Override
        public void run() {
            sendout = true;
            mUIHandler.sendEmptyMessage(ConstantData.MSG_RESTARTWSS);
        }
    };
    Runnable restoprunnable = new Runnable() {
        @Override
        public void run() {
            mUIHandler.sendEmptyMessage(ConstantData.MSG_RESTOPWSS);
        }
    };

    Runnable saverunnable = new Runnable() {
        @Override
        public void run() {
            sendMessage(ConstantData.MSG_SAVE_TRAY, 1);
            sendMessage(ConstantData.MSG_SAVE_TRAY, 2);
            sendMessage(ConstantData.MSG_SAVE_TRAY, 3);
            sendMessage(ConstantData.MSG_SAVE_TRAY, 4);
            sendMessage(ConstantData.MSG_SAVE_TRAY, 5);
            sendMessage(ConstantData.MSG_SAVE_TRAY, 6);
            sendMessage(ConstantData.MSG_SAVE_TRAY, 7);
            sendMessage(ConstantData.MSG_SAVE_TRAY, 8);

            sendMessage(ConstantData.MSG_SAVE_PCR, 1);
            sendMessage(ConstantData.MSG_SAVE_PCR, 2);
            sendMessage(ConstantData.MSG_SAVE_PCR, 3);
            sendMessage(ConstantData.MSG_SAVE_PCR, 4);
            sendMessage(ConstantData.MSG_SAVE_PCR, 5);
            sendMessage(ConstantData.MSG_SAVE_PCR, 6);

            sendMessage(ConstantData.MSG_SAVE_RNA, 1);
            sendMessage(ConstantData.MSG_SAVE_RNA, 2);

            sendMessage(ConstantData.MSG_SAVE_ARM, 1);
        }
    };
    boolean sendout = true;
    private void restartwss() {
        if(sendout) {
            sendout = false;
            mUIHandler.postDelayed(restoprunnable, 1000);
            mUIHandler.postDelayed(restartrunnable, 5000);
        }
    }
    private void checkwssStart(){
        boolean wssStatus = WSSModuleManager.getInstance().getWSSConnectStatus();
        if(!wssStatus){
            restartwss();
        }
        ConcurrentHashMap<String, WSSConnectionItem> items = WSSModuleManager.getInstance().getWssConnectionItems();
        if(items==null||items.size()==0){
            APLManager.getInstance().WSSStart = false;
        }else{
            APLManager.getInstance().WSSStart = true;
        }
        if(mUIHandler!=null) {
            mUIHandler.postDelayed(saverunnable, 2000);
        }
    }
    /*
     * EventBus接收订阅的消息
     * 注意：二进制日志（PCRLog(bin)/RNALog(bin)）已由 WSSLogHelper 保存，这里不再处理
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LogMessageEvent messageEvent) {
        if (messageEvent != null) {
            // 二进制日志（WSS_LOGMessage）已由 WSSLogHelper 保存到 /Log/yyyyMMdd/ModuleName/DeviceID/
            // 这里不再重复保存，直接跳过
            if(messageEvent.getMsgType().equals(EnumMessageEvent.WSS_LOGMessage)) {
                return;
            }
        }
    }

    /**
     * 将日志内容安全转为 String：
     * - byte[]  → Base64 字符串（二进制 bin 文件日志）
     * - String  → 原样返回
     * - 其它    → Object.toString()
     */
    private static String toLogString(Object obj) {
        if (obj == null) return "";
        if (obj instanceof byte[]) {
            // Convert byte[] to UTF-8 string instead of Base64 encoding
            return new String((byte[]) obj, java.nio.charset.StandardCharsets.UTF_8);
        }
        return obj.toString();
    }
    /**
     * 指令正在执行
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RobotManagerResultMessage robotManagerResultMessage) {

        if(robotManagerResultMessage != null )
        {
//            String text = GsonUtil.toJson(robotManagerResultMessage,false);
//            RobotWorkManagerRunningMessageText.append(text + "\n");
//            RobotWorkManagerRunningMessageText.scrollTo(0,0);
            //RobotWorkManagerRunningMessageText.setText(text);
            if(robotManagerResultMessage.getResult() == WorkInfo.State.FAILED){
                Log.w("ArmTaskWorker", "onEventMainThread(RobotManagerResultMessage)" + "--" +
                        robotManagerResultMessage.getAction() + "--" +
                        robotManagerResultMessage.getResult().name() + "--" +
                        robotManagerResultMessage.getDetail());
                ArmManager.getInstance().lastAction = robotManagerResultMessage.getAction();
                ArmManager.getInstance().lastDetail = robotManagerResultMessage.getDetail();
            }
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
        if(procedureFinishEvent.getTemplateName().equalsIgnoreCase(ConstantData.STR_ARMRESET)||
                procedureFinishEvent.getTemplateName().equalsIgnoreCase(ConstantData.STR_RESET)) {
            if (procedureFinishEvent.getStatus().equals(ConstantData.STR_FINISH)) {
                ArmManager.getInstance().updateFinish(1,true);
                Log.i("ArmTaskWorker", "arm reset finish");
            }else if(procedureFinishEvent.getStatus().equals(ConstantData.STR_FINISHERROR)){
//                ArmManager.getInstance().updateFinish(1,true);
                Log.i("ArmTaskWorker", "arm reset finish error");
            }
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WSSMessageEvent messageEvent) {
        EnumMessageEvent eme = messageEvent.getMsgType();
        switch (eme){
            case WSS_ServerStart:
                APLManager.getInstance().WSSStart = true;
                Log.i(TAG, "WSS_ServerStart:" + (String) messageEvent.getData());
                //     APLManager.getInstance().refreshConnectinfo();
                break;
            case WSS_Error:
                checkwssStart();
                Log.i(TAG, "WSS_Error345:" + (String) messageEvent.getData());
                //    APLManager.getInstance().refreshConnectinfo();
                break;
            case WSSManager_UpdateConnection:
                Log.i(TAG, "updateConnection:" + (String) messageEvent.getData());
                //     APLManager.getInstance().refreshConnectinfo();
                break;
            case WSSManager_UpdateRNAStatus:
                Log.i(TAG, "WSSManager_UpdateRNAStatus:" + messageEvent.getData());
                //     APLManager.getInstance().refreshConnectinfo();
                break;
            case WSSManager_UpdateRNARunningStatus:
                Log.i(TAG, "WSSManager_UpdateRNARunningStatus:" + messageEvent.getData());
                //    APLManager.getInstance().refreshConnectinfo();
                break;
            case WSSManager_RNAAck_OK:
                Log.i(TAG, "WSSManager_RNAAck_OK_Message:" + messageEvent.getData());
                //     APLManager.getInstance().refreshConnectinfo();
                break;
            case WSSManager_RNAAck_Error:
                Log.i(TAG, "WSSManager_RNAAck_Error_AckMessage:" + messageEvent.getData());

                break;
            case WSSManager_RNARunningFinish ://rna开关门，运行模板都会调用
                WSSConnectionItem item = messageEvent.getwSSConnectionItem();
                int rnaposition = item.getPosition();
                Log.i("ArmTaskWorker", "WSSManager_RNARunningFinish index:" + item.getPosition());
                Log.i("ArmTaskWorker", "WSSManager_RNARunningFinish devicetype:" + item.getDeviceType());
                if (item.getDeviceType().equalsIgnoreCase(ConstantData.nameRNA)) {
                    if(messageEvent.getData() instanceof RNAFinishMessage){
                        RNAFinishMessage message = (RNAFinishMessage)messageEvent.getData();
                        String template = message.getTemplate();
                        Log.i("ArmTaskWorker", "WSSManager_RNARunningFinish template:" + template);
                        if(template!=null) {
                            if(template.contains(ConstantData.RNA_opendoor)){
                                Log.i("ArmTaskWorker", "WSSManager_RNARunningFinish opendoor:"+item.getPosition());
                                Log.i("ArmTaskWorker", "取托盘 opendoor rna:"+rnaposition);
                                RNAManager.getInstance().updateOpendoorFinish(rnaposition,true);
                            }else if(template.contains(ConstantData.RNA_closedoor)){
                                Log.i("ArmTaskWorker", "WSSManager_RNARunningFinish closedoor:"+item.getPosition());
                                Log.i("ArmTaskWorker", "取托盘 closedoor rna:"+rnaposition);
                                RNAManager.getInstance().updateOpendoorFinish(rnaposition, false);
                            }else if(template.contains(ConstantData.RESET_EXCEPTION)){
                                sendMessage(ConstantData.MSG_Reset_RNA,rnaposition);
                                Log.i("ArmTaskWorker", "WSSManager_RNARunningFinish reset:"+item.getPosition());
                            }else {
                                if(message!=null&&message.getStatus()!=null&&message.getStatus().contains(ConstantData.str_ERROR)){
                                    sendMessage(ConstantData.MSG_FinishError_RNA,rnaposition);
                                    Log.e("ArmTaskWorker", "WSSManager_RNARunningFinish error:" + template + " index:" + item.getPosition());
                                }else {
                                    sendMessage(ConstantData.MSG_Finish_RNA, rnaposition);
                                    Log.i("ArmTaskWorker", "WSSManager_RNARunningFinish template:" + template + " index:" + item.getPosition());
                                }
                            }
                        }
                    }
                }
                break;
            case WSSManager_PCRRunningFinish:
                WSSConnectionItem wssitem = messageEvent.getwSSConnectionItem();
                int position = wssitem.getPosition();
                Log.i("ArmTaskWorker", "【PCR结束事件】PCRRunningFinish index:" + position);
                Log.i("ArmTaskWorker", "【PCR结束事件】devicetype:" + wssitem.getDeviceType());
                if (wssitem.getDeviceType().equalsIgnoreCase(ConstantData.namePCR) ) {
                    Log.i("ArmTaskWorker", "【PCR结束事件】设备类型匹配 PCR");
                    if(messageEvent.getData() instanceof PCRFinishMessage){
                        PCRFinishMessage message = (PCRFinishMessage)messageEvent.getData();
                        String template = message.getTemplate();
                        Log.i("ArmTaskWorker", "【PCR结束事件】template:" + template + " status:" + message.getStatus());
                        if(template!=null) {
                            if(template.contains(ConstantData.RESET_EXCEPTION)){
                                sendMessage(ConstantData.MSG_Reset_PCR,position);
                                Log.i("ArmTaskWorker", "【PCR结束事件】发送 MSG_Reset_PCR:"+position);
                            }else {
                                if(message!=null&&message.getStatus()!=null && message.getStatus().contains(ConstantData.str_ERROR)){
                                   sendMessage(ConstantData.MSG_FinishError_PCR,position);
                                    Log.e("ArmTaskWorker", "【PCR结束事件】发送 MSG_FinishError_PCR:" + position);
                                }else {
                                    sendMessage(ConstantData.MSG_Finish_PCR, position);
                                    Log.i("ArmTaskWorker", "【PCR结束事件】发送 MSG_Finish_PCR:" + position);
                                }
                            }
                        } else {
                            Log.e("ArmTaskWorker", "【PCR结束事件断点】template 为 null，无法发送消息！position:" + position);
                        }
                    } else {
                        Log.e("ArmTaskWorker", "【PCR结束事件断点】messageEvent.getData() 不是 PCRFinishMessage 类型！");
                    }
                } else {
                    Log.e("ArmTaskWorker", "【PCR结束事件断点】设备类型不匹配：" + wssitem.getDeviceType());
                }
                break;
            case WSSManager_SLANPCRRunningFinish:
                //宏石程序运行结束上报消息
                Log.e("slanFinish","Activity接受pcrfinish更新消息，主控保存PCR文件成功后更新状态，并保存数据库");
                WSSConnectionItem finish_wssitem = messageEvent.getwSSConnectionItem();
                SlanFinishResult slanFinishResult =(SlanFinishResult) messageEvent.getData();
                int pcr_pos = finish_wssitem.getPosition();
                Log.d("slanFinish", "SLANPCRRunningFinish index:" + pcr_pos+"devicetype:==" + finish_wssitem.getDeviceType());

                if (finish_wssitem.getDeviceType().equalsIgnoreCase(ConstantData.namePCR) ) {
                    SLAN_PCRFinishMessage message = slanFinishResult.getSlan_pcrFinishMessage();
                    String saveFilePath = slanFinishResult.getFilePath();
                    String template = message.getTemplate();
                    Log.d("slanFinish", "SLANPCRRunningFinish template:" + template);
                    if(template!=null) {
                        if(template.contains(ConstantData.RESET_EXCEPTION)){
                            sendMessage(ConstantData.MSG_Reset_PCR,pcr_pos);
                            Log.d("slanFinish", "PCRRunningFinish reset:"+pcr_pos);
                        }else {
                            if(message!=null&&message.getStatus()!=null && message.getStatus().contains(ConstantData.str_ERROR)){
                                sendMessage(ConstantData.MSG_FinishError_PCR,pcr_pos);
                                Log.e("slanFinish", "WSSManager_RNARunningFinish error:" + template + " index:" + pcr_pos);
                            }else {
                                sendMessage(ConstantData.MSG_Finish_SLAN_PCR, slanFinishResult);
                                Log.i("slanFinish", "PCRRunningFinish template:" + template + "" + pcr_pos);
                            }

                        }
                    }

                }
                break;
            case WSSManager_UpdatePCRStatus:
                Log.i(TAG, "WSSManager_UpdatePCRStatus:" + messageEvent.getData());
                //wssConnectionAdapter.notifyDataSetChanged();
                break;
            case WSSManager_UpdatePCRRunningStatus:
                Log.i(TAG, "WSSManager_UpdatePCRRunningStatus:" + messageEvent.getData());
                //wssConnectionAdapter.notifyDataSetChanged();
                break;
            case WSSManager_PCRAck_OK:
                Log.i(TAG, "WSSManager_PCRAck_OK_Message:" + messageEvent.getData());
                //wssConnectionAdapter.notifyDataSetChanged();
                break;
            case WSSManager_PCRAck_Error:
                Log.i(TAG, "WSSManager_PCRAck_Error_AckMessage:" + messageEvent.getData());
                 //wssConnectionAdapter.notifyDataSetChanged();
                break;
            case WSSManager_UpdatePCRCurve:
                Log.i(TAG, "WSSManager_UpdatePCRCurve:" + messageEvent.getData());
//                PCRStatusItem pCRStatusItem = (PCRStatusItem) messageEvent.getwSSConnectionItem();
//                Experiment experiment = pCRStatusItem.getExperiment();
                break;
            default:
                //        APLManager.getInstance().refreshConnectinfo();
                break;
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RobotManagerMessage robotManagerMessage) {

        if(robotManagerMessage != null )
        {
//            String text = GsonUtil.toJson(robotManagerMessage,false);
            String act = robotManagerMessage.getAction();
            if(act!=null && (act.contains("pan") ||act.contains("pcr_cover") )){
                ArmManager.getInstance().updateAction(1,act);
            }
            //RobotWorkManagerRunningMessageText.setText(text);
            if(robotManagerMessage.getResult() == WorkInfo.State.FAILED){
                Log.e("ROBOT", "onEventMainThread(robotManagerMessage)" + "--" +
                        robotManagerMessage.getAction() + "--" +
                        robotManagerMessage.getResult().name() + "--" +
                        robotManagerMessage.getDetail());
            }
        }
    }
    private boolean isdownAndOpen = false;
    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()) {
            case MsgType.MSG_RNA_CONNECT_STATE:
                if (PcrApplication.is_RNA_connect) {
                    if(PcrApplication.is_SLAN_PCR_connect){
                        AlarmInfoManager.getInstance().setText("PCR DISSCONNECT");
                    }else {
                        AlarmInfoManager.getInstance().setText("E4-"+getResources().getString(R.string.e4_alarm_pcr_hardware_disconnect));
                    }
                } else {
                    if(!isSecretSetting){
                        if(!PcrApplication.is_SLAN_PCR_connect){
                            AlarmInfoManager.getInstance().setText(getResources().getString(R.string.e3_e4_alarm_both_hardware_disconnect));
                        }else {
                            AlarmInfoManager.getInstance().setText("E3-"+getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                        }
                        //RNA断连
                    }
                }
                break;
            case MsgType.MSG_PCR_CONNECT_STATE:
                if (PcrApplication.is_SLAN_PCR_connect) {
                    if(PcrApplication.is_RNA_connect){
                        AlarmInfoManager.getInstance().setText("RNA DISSCONNECT");
                    }else{
                        AlarmInfoManager.getInstance().setText("E3-"+getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                    }
                } else {
                    if(!isSecretSetting){
                        if(!PcrApplication.is_RNA_connect){
                            AlarmInfoManager.getInstance().setText(getResources().getString(R.string.e3_e4_alarm_both_hardware_disconnect));
                        }else{
                            AlarmInfoManager.getInstance().setText("E4-"+getResources().getString(R.string.e4_alarm_pcr_hardware_disconnect));
                        }
                    }
                }
                break;
            case MsgType.MSG_TEMPLATE_CAPACITY_DONE:
                ProjectInfo projectInfo = (ProjectInfo) messageEvent.getData();
                String reagentCode = projectInfo.getReagentCode();
//                description = projectInfo.getDescription();
//                replaceFragment(mTemplateFragment, mEntryInfoFragment);
//                mTemplateFragment = null;
//                getTotalTime();
                break;

            case MsgType.MSG_ENTRY_INFO_DONE:
//                replaceFragment(mEntryInfoFragment, mUserBiowayFragment);
//                mEntryInfoFragment = null;
//                List<WellInfo> list = (List<WellInfo>) messageEvent.getData();
//                wellInfos.clear();
//                wellInfos.addAll(list);
//                //将信息存入文件
//                if (null != wellInfos) {
//                    String detectInfo = JSON.toJSONString(wellInfos);
//                    boolean saveResult = FileUtils.dumpDeviceInfo(detectInfo, "detectInfo");
//                }
//                //保存当前运行的模板
//                PreferenceUtil.getInstance(getApplicationContext())
//                        .setValueByName(SharedConstant.RUNNING_TEMPLATE,PcrApplication.rootTempName);
                break;
            case MsgType.MSG_START_RNA_RUNNING :
                boolean recovery = (boolean)messageEvent.getData();
                Log.i("User", "开始检测第一次RNA检测");
//                if(!recovery){
//                    startCountDown(false);
//                }
                break;

            case MsgType.MSG_START_SLAN_FIRST_PCR:
                Log.i("User", "开始检测第一次PCR");
                String slan_FilePath1 = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName;
                //   startBioWayPCR(slan_FilePath1, "first", 1);
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_DONE:
                Log.i("User", "结束第一次PCR检测");
//                if (PcrApplication.isHasSecondRNA) {
//                    resetPcrRecMark();
//                }else{
//                    onSLANExperimentFinish();
//                }
                break;

            case MsgType.MSG_START_SLAN_SECOND_PCR:
                Log.i("User", "开始检测第二步PCR");
                String slan_FilePath2 = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName;
                //    startBioWayPCR(slan_FilePath2, "second", 2);
                break;
            case MsgType.MSG_SECOND_SLAN_PCR_DONE:
                Log.i("User", "结束第二步PCR检测");
//                resetPcrRecMark();
//                onSLANExperimentFinish();
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_INTERRUOT:

                if(messageEvent.getData() != null) {
                    RunningInfo firstCode = (RunningInfo) messageEvent.getData();
                    String wanring = firstCode.getCurrentError();
                    //  Log.i("RNA", "第一步PCR检测中断 firstCode" + firstCode.get);
                    Log.i("RNA", "第一步PCR检测中断 getCurrentError" + wanring);
                    showAlarm();
                    showAlertDialog(getResources().getString(R.string.dialog_title_check_equipment),
                            getResources().getString(R.string.running_dialog_message_first_pcr_inerrupt)
                                    + " " + wanring);
                    AlarmInfoManager.getInstance().setText("E13-" + getResources().getString(R.string.e13_alarm_first_step_pcr_interrupt)
                            +" "
                            + wanring);
                }else{
                    Log.i("User", "第一步PCR手动检测中断 firstCode");
                    showAlarm();
                    showAlertDialog(getResources().getString(R.string.dialog_title_attention),
                            getResources().getString(R.string.running_dialog_message_manualfirst_pcr_inerrupt));
                    AlarmInfoManager.getInstance().setText("E12-" + getResources().getString(R.string.e12_alarm_manual_first_step_pcr_interrupt));
                }
                //   stopCountDown();
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_ERROR:
                if(!isSecretSetting) {
                    SlanError firsterror = (SlanError) messageEvent.getData();
                    Log.i("User", "第一次PCR检测错误 firstCode" + firsterror);
                    showAlarm();
                    showAlertDialog(getResources().getString(R.string.login_dialog_title_check_device),
                            getResources().getString(R.string.e14_alarm_first_step_pcr_run_error)
                                    + "," + firsterror.getMessage()
                                    + getResources().getString(R.string.dialog_title_check_equipment));
                    AlarmInfoManager.getInstance().setText("E14-" +  getResources().getString(R.string.e14_alarm_first_step_pcr_run_error)
                            + firsterror.getMessage());
//                    stopCountDown();
                }
                break;
            case MsgType.MSG_SECOND_SLAN_PCR_INTERRUOT:
                if(messageEvent.getData() != null) {
                    RunningInfo secondCode = (RunningInfo) messageEvent.getData();
                    String wanring = secondCode.getCurrentError();
                    Log.i("User", "第二步PCR检测中断 firstCode" + secondCode);
                    Log.i("User", "第二步PCR检测中断 getCurrentError" + wanring);
                    showAlarm();
                    showAlertDialog(getResources().getString(R.string.login_dialog_title_check_device),
                            getResources().getString(R.string.running_dialog_message_second_pcr_interrupt)
                                    + " " + wanring);
                    AlarmInfoManager.getInstance().setText("E16-"+getResources().getString(R.string.e16_alarm_second_step_pcr_interrupt));
                }else{
                    Log.i("User", "第二步PCR手动检测中断 firstCode");
                    showAlarm();
                    showAlertDialog(getResources().getString(R.string.dialog_title_attention),
                            getResources().getString(R.string.running_dialog_message_manual_second_pcr_interrupt));
                    AlarmInfoManager.getInstance().setText("E15-"+getResources().getString(R.string.e15_alarm_manual_second_step_pcr_interrupt));
                }
//                stopCountDown();
                break;
            case MsgType.MSG_SECOND_SLAN_PCR_ERROR:
                if(!isSecretSetting) {
                    Log.i("User", "第二步PCR检测错误");
                    SlanError secondError = (SlanError) messageEvent.getData();
                    showAlarm();
                    showAlertDialog(getResources().getString(R.string.login_dialog_title_check_device),
                            getResources().getString(R.string.e17_alarm_second_step_pcr_run_error)
                                    +","+ secondError.getMessage());
                    AlarmInfoManager.getInstance().setText("E17-"+getResources().getString(R.string.e17_alarm_second_step_pcr_run_error)
                            + "," + secondError.getMessage());
//                    stopCountDown();
                }
                break;

            case MsgType.MSG_UPDATE_EX_RESULT:
//                showUpdateDbDialog();
                break;

            case MsgType.MSG_SHOW_ALARM_MESSAGE:
                String alarmMsg = (String) messageEvent.getData();
                AlarmInfoManager.getInstance().setText(alarmMsg+"");
                break;
        }
    }

    /*
     * 超时消息
     * @param sendCmdTimeoutEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {
        Gson gson = new Gson();
        String text = gson.toJson(sendCmdTimeoutEvent.getMsgPacket());
        AlarmInfoManager.getInstance().setText("E20"+  getResources().getString(R.string.e20_command_timeout) + text);
        //stopAnimation_Interupt();//20250208
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WarningMessage warningMessage) {
        String json = GsonUtil.toJson(warningMessage, true);
        Log.e("User", "警报消息 :" + json);
        if(!isSecretSetting) {
            AlarmInfoManager.getInstance().setText(warningMessage.getAlarm() + "-" +
                    warningMessage.getMotorID() + "-" + warningMessage.getStatus());
        }
    }
    /**
     * 指令正在执行
     *
     * @param rNARunningMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RNARunningMessage rNARunningMessage) {

        if (rNARunningMessage != null) {
            if (rNARunningMessage.getStatus().contains("free") || rNARunningMessage.getStatus().contains("done")) {
                //     Log.i("User", "RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
            } else {
                //      Log.i("User", "RNA指令运行---" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
            }
         //   PcrApplication.rNARunningMessage = rNARunningMessage;
        }
    }


    public void resetPcrRecMark(){
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE, "");
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, 0);
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE_STEP, 0);
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.EXPERIMENT_TYPE, 0);
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.EX_TOTAL_TIME,0);
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.PROCEDURE_RUNNING_TIME,0);
        PreferenceUtil.getInstance(this.getApplicationContext())
                .setValueByName(SharedConstant.RUNNING_TEMPLATE,"");
    }

    public void setPcrRecMark(int pcr_file_num,int pcr_step){
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE, PcrApplication.rootTempName);
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, pcr_file_num);
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE_STEP, pcr_step);
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_STARTTIME, DateUtil.getCurrentTimeYMDHMS());
    }

    /**
     * 显示倒计时
     */
    private void getTotalTime(){
        //读取倒计时文件
        String timeFilePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName
                + File.separator + "totaltime.txt";
        File file = new File(timeFilePath);
        if(file.exists()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(timeFilePath);
                String text = ListDataSaveUtil.fileToStr(inputStream);
                TotalTime totalTime = GsonUtil.fromJson(text,TotalTime.class);
                PcrApplication.totalTime = totalTime.getTotalTime();
            } catch (Exception e) {
                PcrApplication.totalTime = 5000;
            }

        }else{
            PcrApplication.totalTime = 10800;
        }
        PcrApplication.runningTime = PcrApplication.totalTime;
        PreferenceUtil.getInstance(getApplicationContext())
                .setValueByName(SharedConstant.EX_TOTAL_TIME,PcrApplication.totalTime);
    }

    private void stopCountDown(){
        MessageEvent countdownMsg = new MessageEvent();
        countdownMsg.setMsgType(MsgType.MSG_END_COUNTDOWN);
        EventBus.getDefault().postSticky(countdownMsg);
    }
    //20250221
    private void startCountDown(boolean recovery){
        Log.i("PipeMainActivity", "startCountDown" );
        MessageEvent countdownMsg = new MessageEvent();
        countdownMsg.setData(recovery);
        countdownMsg.setMsgType(MsgType.MSG_START_COUNTDOWN);
        EventBus.getDefault().postSticky(countdownMsg);
    }

    //first rna
//    /*
//     * EventBus接收订阅的消息
//     * @param
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
//    public void onEvent(MessageEvent messageEvent) {
//
////
//    }


//    /**
//     * 运行结束
//     * @param procedureFinishEvent
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
//        Log.i("User", "arm测试结束" + procedureFinishEvent.getStatus());

//        if(null != procedureFinishEvent){
//            // onChronometerStop();
//            if(procedureFinishEvent.getStatus().equals("finish")){
//                PcrApplication.rNARunningMessage.setStatus("free");
//            }else if(procedureFinishEvent.getStatus().equals("finish error")){
//                mRemainTimeTv.setTextColor(getActivity().getColor(R.color.red));
//                mRemainTimeImg.setImageResource(R.drawable.icon_remaining_time_red);
//                mCounterLayout.setBackgroundResource(R.drawable.countdown_stop_bg);
//            }
//        }
//    }

//    /**
//     * 超时消息
//     * @param sendCmdTimeoutEvent
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {
//        Log.i("User", "RNA测试超时");
////        if(null != getActivity()){
////            onChronometerStop();
////        }
//    }


    /**
     * 当前的步骤
     * @param stepIndexMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StepIndexMessage stepIndexMessage) {
        if(stepIndexMessage != null){
        }
    }

    /**
     * 当前的步骤
     * @param hexDebugEntity
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(HexDebugEntity hexDebugEntity) {
//        if(hexDebugEntity != null && getActivity() != null){
//            Log.i("User", "第一次RNA测试正在进行" + hexDebugEntity.getName() + " 步骤描述" + hexDebugEntity.getDescription() );
//        }
    }


//    /**
//     * 指令正在执行
//     * @param rNARunningMessage
//     */
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    public void onEventMainThread(RNARunningMessage rNARunningMessage) {
//        Log.i("User", "第一次RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
//        // String text = gson.toJson(rNARunningMessage);
//        if(rNARunningMessage != null)
//        {
//            /*if(rNARunningMessage.getStatus().contains("free") || rNARunningMessage.getStatus().contains("done")) {
//               .setText(runnning ++ + rNARunningMessage.getStatus()+ ":" + rNARunningMessage.getNO() + rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
//            }else{
//               mRunningMsgTv.setText(runnning ++ + rNARunningMessage.getStatus()+ ":" + rNARunningMessage.getNO() + rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
//            }*/
//        }
//        PcrApplication.rNARunningMessage = rNARunningMessage;
//    }

    /**
     * 指令完成
     * @param actionDoneMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ActionDoneMessage actionDoneMessage) {
//        if(null != actionDoneMessage && getActivity() != null){
//            Log.i("User", "第一次RNA指令完成" + actionDoneMessage.getID() + "状态" + actionDoneMessage.getStatus());
//        }
    }

    //first rna
}