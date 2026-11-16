package com.gidi.bio_pcr.bioway.activity;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.alibaba.fastjson.JSON;
import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;
import com.gidi.bio_pcr.WintersUtils.entity.WarningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.activity.MainActivity;
import com.gidi.bio_pcr.base.BaseFragmentActivity;
import com.gidi.bio_pcr.bean.CtRuleConclusion;
import com.gidi.bio_pcr.bean.JudgeConclusion;
import com.gidi.bio_pcr.bean.PeakRuleConclusion;
import com.gidi.bio_pcr.bean.WellInfo;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.gidi.bio_pcr.bioway.RunningBiowayFragment;
import com.gidi.bio_pcr.bioway.SlanError;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.fragment.detect.EntryInfoFragment;
import com.gidi.bio_pcr.fragment.detect.TemplateFragement;
import com.gidi.bio_pcr.listener.FragmentKeyeventListener;
import com.gidi.bio_pcr.pcr.OnExperimentCallback;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.ScreenShotUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.CustomView;
import com.gidi.bio_pcr.view.DateTimeView;
import com.gidi.bio_pcr.view.WarnDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.hongshi.slansdk.slanenum.TemperatureControlMode;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Enum.SampleType;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.bean.RunBiowayResult;
import com.winters.boweilibrary.BoWei.bioweyenum.RunExperimentResult;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningState;
import com.winters.boweilibrary.BoWei.listener.RunningInfoListener;
import com.winters.boweilibrary.BoWei.model.experiment.ExperimentParam;
import com.winters.boweilibrary.BoWei.model.experiment.RunningInfo;
import com.winters.boweilibrary.BoWei.model.experiment.Well;
import com.winters.boweilibrary.BoweiJni;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.hutool.core.io.FileUtil;

public class BiowayActivity extends BaseFragmentActivity implements View.OnClickListener,
        OnExperimentCallback {

    private RelativeLayout main_root_layout;
    private TemplateFragement mTemplateFragment;
    private EntryInfoFragment mEntryInfoFragment;
    private RunningBiowayFragment mRunningBiowayFragment;
    private FragmentManager mFragmentManager;

    private DateTimeView mDateTimeView;
    private CustomView mRNAStateView, mPCRStateView;
    private TextView mAlarmMsgTv;
    private ImageView mSceenShotBtn;
    private ImageView mStopBtn;

    private ImageView mVolumeImg;

    private CustomDialog stopDialog,downDialog,updateDbDialog;
    private WarnDialog mNoticeDialog;
    private List<File> localList = new ArrayList<File>();
    private List<CtRuleConclusion> ctConclusions;
    private List<PeakRuleConclusion> peakConclusions;
    private UIHandler mUIHandler;
    private ImageView mBackImg;

    private List<String> mHongShiTempList;
    private List<WellInfo> wellInfos;//患者的身份信息

    private LinearLayout running_counter_layout;

    private boolean isPCRRunning = false;
/*    private TextView mRuningStateTv;
    private ImageView mRunningStateImg;
    private Animation animation;*/
    private int current_pcr = 1;

    private AudioManager mAudioManager;
    private int alarmVolume;
    private boolean isMute = false;
    private String PCR_RUNNING_TEMPLATE = "";

    private Chronometer mChronTimer;
    private ImageView countAlarmImg;
    //控温方式
    private int temperatureCtrlMode = TemperatureControlMode.Fast.getValue();
    //项目描述
    private String description = "";

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
    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_bioway_pcr;
    }

    @Override
    protected void initView() {
        main_root_layout = (RelativeLayout) findViewById(R.id.bioway_detect_main_root_layout);
        mBackImg = (ImageView) findViewById(R.id.bioway_detect_tab_back_img);
        mStopBtn = (ImageView) findViewById(R.id.bioway_detect_tab_stop_img);
        mSceenShotBtn = (ImageView) findViewById(R.id.bioway_detect_running_sceentshot_tv);
        mVolumeImg = (ImageView) findViewById(R.id.bioway_pcr_sound_tv);
        mRNAStateView = (CustomView) findViewById(R.id.bioway_detect_runing_rna_state_img);
        mPCRStateView = (CustomView) findViewById(R.id.bioway_detect_runing_coval_state_img);
        mAlarmMsgTv = (TextView) findViewById(R.id.bioway_detect_alarm_msg_tv);
        mDateTimeView = (DateTimeView) findViewById(R.id.bioway_detect_running_time_ll);
        mSceenShotBtn.setVisibility(View.GONE);
        mStopBtn.setVisibility(View.GONE);
        /*mRuningStateTv = (TextView) findViewById(R.id.bioway_detect_tab_state_tv);
        mRunningStateImg = (ImageView) findViewById(R.id.bioway_detect_tab_state_img);*/
        mChronTimer = (Chronometer)findViewById(R.id.bioway_running_detect_chronometer);
        running_counter_layout = (LinearLayout)findViewById(R.id.bioway_running_counter_layout);
        countAlarmImg = (ImageView) findViewById(R.id.bioway_counting_alarm_img);
    }

    @Override
    protected void initListener() {
        initVariable();
        countAlarmImg.setOnClickListener(this);
        mBackImg.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mSceenShotBtn.setOnClickListener(this);
        //设备状态监听
        mVolumeImg.setOnClickListener(this);

        //设备运行信息监听
        if (null != BoWeiService.getInstance()) {
            BoWeiService.getInstance().setRunningInfoListener(new RunningInfoListener() {
                @Override
                public void getRunningInfo(@NonNull String serialNo, byte block, @NonNull com.winters.boweilibrary.BoWei.model.experiment.RunningInfo info) {
                    Log.e("BIOWAY", "info.getState()" + info.getState());
                    Log.e("BIOWAY", "info.getSegment()" + info.getSegment());
                    Log.e("BIOWAY ", "info.getStep()" + info.getStep());
                    Log.e("BIOWAY ", "info.getCurrentError()" + info.getCurrentError());
                    Log.e("BIOWAY ", "info.getError()" + info.getError());
                    if (current_pcr == 1) {
                        if (info.getState() == RunningState.Finished) {
                            MessageEvent fist_done = new MessageEvent();
                            //实验状态完成
                            Log.e("Bioway ", "PCR实验状态完成");
                            fist_done.setMsgType(MsgType.MSG_FIRST_SLAN_PCR_DONE);
                            fist_done.setData(info);
                            EventBus.getDefault().postSticky(fist_done);
                            isPCRRunning = false;
                        } else if (info.getState() == RunningState.Running) {
                            MessageEvent first_run = new MessageEvent();
                            first_run.setMsgType(MsgType.MSG_FIRST_SLAN_PCR_RUNNING);
                            first_run.setData(info);
                            EventBus.getDefault().postSticky(first_run);
                        } else if (info.getState() == RunningState.Interrupt) {
                            isPCRRunning = false;
                            MessageEvent first_error = new MessageEvent();
                            first_error.setMsgType(MsgType.MSG_FIRST_SLAN_PCR_INTERRUOT);
                            first_error.setData(info);
                            EventBus.getDefault().postSticky(first_error);
                        }

                    } else if (current_pcr == 2) {
                        if (info.getState() == RunningState.Finished) {
                            //实验状态完成
                            isPCRRunning = false;
                            MessageEvent second_done = new MessageEvent();
                            second_done.setMsgType(MsgType.MSG_SECOND_SLAN_PCR_DONE);
                            second_done.setData(info);
                            EventBus.getDefault().postSticky(second_done);
                        } else if (info.getState() == RunningState.Running) {
                            MessageEvent second_run = new MessageEvent();
                            second_run.setMsgType(MsgType.MSG_SECOND_SLAN_PCR_RUNNING);
                            second_run.setData(info);
                            EventBus.getDefault().postSticky(second_run);
                        } else if (info.getState() == RunningState.Interrupt) {
                            isPCRRunning = false;
                            MessageEvent second_error = new MessageEvent();
                            second_error.setMsgType(MsgType.MSG_SECOND_SLAN_PCR_INTERRUOT);
                            second_error.setData(info);
                            EventBus.getDefault().postSticky(second_error);
                        }

                    }
                }
            });
        }
    }

    @Override
    protected void handleMessage(Message msg) {

    }

    private void initVariable() {
        mUIHandler = new UIHandler(this);
        mTemplateFragment = new TemplateFragement();
        mEntryInfoFragment = new EntryInfoFragment();
        mRunningBiowayFragment = new RunningBiowayFragment();
        wellInfos = new ArrayList<WellInfo>();
        ctConclusions = new ArrayList<CtRuleConclusion>();
        peakConclusions = new ArrayList<PeakRuleConclusion>();
      /*  animation = AnimationUtils.loadAnimation(
                getApplicationContext(), R.anim.loading_animation);*/
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        initUpdateSysTime();
        initVolumeState();
        BiowayManager.getInstance().setOnExperimentCallback(this);
        boolean recovery = getIntent().getBooleanExtra("recovery", false);

        if (recovery) {
            repalceFragment(1);
            getTotalTime();
            startCountDown();

        } else {
            //正常启动
            repalceFragment(0);
        }

    }


    @Override
    protected void onResume() {
        super.onResume();
        //检查本地的模板列表
        initLocalFile();
        checkPortState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
        dismissDialog();
        clearList();
        if (null != mUIHandler) {
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        if (null != runnable) {
            runnable = null;
        }
        mAudioManager = null;
        //stopAnimation();
        //animation = null;
        stopCountDown();
        mChronTimer = null;
        //////////////注意此处，以后可能要修改
        stopExperiment();
        BiowayManager.getInstance().releaseExperimentCallBack();
    }

    private void initUpdateSysTime() {
        mUIHandler.postDelayed(runnable, 1000);
    }

    private void initVolumeState() {
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        alarmVolume = (int) ((maxVolume * 4) / 5);
        int currentVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (0 == currentVol) {
            mVolumeImg.setImageResource(R.drawable.icon_mute);
            isMute = true;
        } else {
            mVolumeImg.setImageResource(R.drawable.icon_no_mute);
            isMute = false;
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //TODO:延时要干的其他事情
            mUIHandler.sendEmptyMessage(Constants.MSG_UPDATE_SYSTEM_TIME);
            mUIHandler.postDelayed(runnable, 1000);
        }
    };


    private void repalceFragment(int position) {
        if (null == mFragmentManager) {
            mFragmentManager = getSupportFragmentManager();
        }
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        switch (position) {
            case 0:
                fragmentTransaction.replace(R.id.bioway_activity_frame_layout, mTemplateFragment);
                break;
            case 1:
                fragmentTransaction.replace(R.id.bioway_activity_frame_layout, mRunningBiowayFragment);
                break;

            default:
                break;
        }
        fragmentTransaction.commit();

    }

    public void replaceFragment(Fragment from, Fragment to) {
        if (null == mFragmentManager) {
            mFragmentManager = getSupportFragmentManager();
        }
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.bioway_activity_frame_layout, to);
        fragmentTransaction.commit();
    }

    /**
     * 是否是宏石的模板
     *
     * @return
     */
    private boolean isHongshiPCRTemplate(String subFileDirPath) {
        //每个模板文件夹中的文件
        File[] subFiles = new File(subFileDirPath).listFiles();
        if (subFiles != null && subFiles.length > 0) {
            for (File subTempFile : subFiles) {
                if(subTempFile.isDirectory()){
                    continue;
                }
                String str = subTempFile.getName(); // 要判断的字符串
                boolean isLowerCase = true; // 初始化标志位为true表示全部字母都为小写
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if (!Character.isLetter(c)) {
                        continue; // 如果不是字母则跳过该字符
                    } else if (!Character.isLowerCase(c)) {
                        isLowerCase = false; // 若存在大写字母，将标志位设置为false
                        break; // 结束遍历
                    }
                }
                if(isLowerCase){
                    continue;
                } else{

                    String newPath_temp = subFileDirPath + "/" + subTempFile.getName().toLowerCase() + "_temp";
                    File newFile_temp;
                    String newPath = subFileDirPath + "" + subTempFile.getName().toLowerCase();
                    File newFile = new File(newPath);
                    File oldFile = new File(subTempFile.getAbsolutePath());
                    /////大小写不敏感
                    // 如果旧文件存在，则重命名它
                    if (oldFile.exists()) {
                        /////大小写不敏感,做一个缓冲文件
                        FileUtil.rename(oldFile,newPath_temp,false,true);
                        newFile_temp = new File(newPath_temp);
                        FileUtil.rename(newFile_temp,newPath,false,true);
//                        if (oldFile.renameTo(newFile)) {
//                            Log.i("File", "Renamed successfully");
//                        } else {
//                            Log.i("File", "Rename failed");
//                        }
                    } else {
                        Log.i("File", "Source file does not exist");
                    }

                }
            }
            File[] subFiles_new = new File(subFileDirPath).listFiles();
            for (File subTempFile_new : subFiles_new) {
                if (subTempFile_new.getName().endsWith("prj")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查询本地模板列表
     */
    private void initLocalFile() {
        mHongShiTempList = new ArrayList<String>();
        File[] files = new File(FileUtils.FULL_PROCESS_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                //分别进行文件夹查询判断是丁智还是宏石的模板
                String subFileDirPath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + files[i].getName()
                        + File.separator;
                //每个模板文件夹中的文件
                boolean isHongshiPCRTemplate = isHongshiPCRTemplate(subFileDirPath);
                if (isHongshiPCRTemplate) {
                    if (null != mHongShiTempList) {
                        mHongShiTempList.add(files[i].getName());
                        Collections.sort(mHongShiTempList);
                    }
                }
            }
        }
        if (null != mTemplateFragment) {
            mTemplateFragment.setTemplateList(mHongShiTempList);
        }
    }

    /**
     * 检查串口连接
     */
    private void checkPortState() {
        if (PcrApplication.is_RNA_connect) {
            mRNAStateView.setStateBackgroud(R.drawable.shape_oval_green_bg);
        } else {
            mRNAStateView.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }

        if (PcrApplication.is_SLAN_PCR_connect) {
            mPCRStateView.setStateBackgroud(R.drawable.shape_oval_green_bg);
        } else {
            mPCRStateView.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
    }

    private void dismissDialog() {
        if (null != stopDialog) {
            stopDialog.dismiss();
            stopDialog = null;
        }
        if (null != downDialog) {
            downDialog.dismiss();
            downDialog = null;
        }
        if (null != updateDbDialog) {
            updateDbDialog.dismiss();
            updateDbDialog = null;
        }
        if (null != mNoticeDialog) {
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }

        if(null != loadingDialog){
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }

    private void clearList() {
        if (null != localList) {
            localList.clear();
            localList = null;
        }

        if (null != wellInfos) {
            wellInfos.clear();
            wellInfos = null;
        }

        if (null != mHongShiTempList) {
            mHongShiTempList.clear();
            mHongShiTempList = null;
        }
        if (null != ctConclusions) {
            ctConclusions.clear();
            ctConclusions = null;
        }
        if(null != peakConclusions){
            peakConclusions.clear();
            peakConclusions = null;
        }
    }

/*    private void startAnimation() {
        if (null != animation) {
            mRunningStateImg.startAnimation(animation);
        }
        mRuningStateTv.setText(R.string.running);
    }

    private void stopAnimation() {
        if (null != animation) {
            mRunningStateImg.clearAnimation();
        }
        mRuningStateTv.setText(R.string.running_finish);
    }

    private void stopAnimation_Interupt() {
        if (null != animation) {
            mRunningStateImg.clearAnimation();
        }
        mRuningStateTv.setText(R.string.running_interupt);
        mRuningStateTv.setTextColor(getResources().getColor(R.color.red,null));
    }

    /**
     * 获取当前模板的所有文件
     */
    private void getLocalFileList() {
        String localFilePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName
                + File.separator;
        File allFileDir = new File(localFilePath);
        if (allFileDir.isDirectory() && allFileDir.exists()) {
            Collections.addAll(localList, allFileDir.listFiles());
        }
    }


    private void onBack() {
        if (isPCRRunning || RNAAndPCRAsyncTask.getInstance().ismIsrunning()) {
            showNoticeDialog(getResources().getString(R.string.running_dialog_title_equipment_in_process),
                    getResources().getString(R.string.running_dialog_message_equipment_in_process));
            return;
        }
        RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
        Intent intent = new Intent(BiowayActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    private void showInterruptDialog() {
        if (null != stopDialog) {
            stopDialog.dismiss();
            stopDialog = null;
        }
        if (null == stopDialog) {
            stopDialog = new CustomDialog(BiowayActivity.this);
            LayoutInflater mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_interrupt_detect_layout, null);
            CustomDialog.Builder builder = new CustomDialog.Builder(BiowayActivity.this);
            builder.setContentView(view);
            builder.setTitle(R.string.dialog_title_prompt);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub

                    RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
                    if(isPCRRunning){
                        stopExperiment();
                        if (current_pcr == 1) {
                            MessageEvent first_error = new MessageEvent();
                            first_error.setMsgType(MsgType.MSG_FIRST_SLAN_PCR_INTERRUOT);
                            first_error.setData(null);
                            EventBus.getDefault().postSticky(first_error);
                        } else if (current_pcr == 2) {
                            MessageEvent second_error = new MessageEvent();
                            second_error.setMsgType(MsgType.MSG_SECOND_SLAN_PCR_INTERRUOT);
                            second_error.setData(null);
                            EventBus.getDefault().postSticky(second_error);
                        }
                    }
                  //  stopAnimation_Interupt();//20259298
                    isPCRRunning = false;
                    stopDialog.dismiss();
                    stopDialog = null;
                    stopCountDown();
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    stopDialog.dismiss();
                    stopDialog = null;
                }
            });
            stopDialog = builder.create();
            stopDialog.setCancelable(false);
            stopDialog.show();
        }
    }

    /**
     * 进行第一次SLANPCE
     *
     * @param messageEvent
     */
    ////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()) {
            case MsgType.MSG_RNA_CONNECT_STATE:
                if (PcrApplication.is_RNA_connect) {
                    mRNAStateView.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    if(PcrApplication.is_SLAN_PCR_connect){
                        mAlarmMsgTv.setText("");
                    }else {
                        mAlarmMsgTv.setText("E4-"+getResources().getString(R.string.e4_alarm_pcr_hardware_disconnect));
                    }
                } else {
                    if(!isSecretSetting){
                        mRNAStateView.setStateBackgroud(R.drawable.shape_oval_red_bg);
                        if(!PcrApplication.is_SLAN_PCR_connect){
                            mAlarmMsgTv.setText(getResources().getString(R.string.e3_e4_alarm_both_hardware_disconnect));
                        }else {
                            mAlarmMsgTv.setText("E3-"+getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                        }
                    }else{
                        mRNAStateView.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    }
                }
                break;
            case MsgType.MSG_PCR_CONNECT_STATE:
                if (PcrApplication.is_SLAN_PCR_connect) {
                    mPCRStateView.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    if(PcrApplication.is_RNA_connect){
                        mAlarmMsgTv.setText("");
                    }else{
                        mAlarmMsgTv.setText("E3-"+getResources().getString(R.string.e3_alarm_rna_extraction_hardware_disconnect));
                    }
                } else {
                    if(!isSecretSetting){
                        mPCRStateView.setStateBackgroud(R.drawable.shape_oval_red_bg);
                        if(!PcrApplication.is_RNA_connect){
                            mAlarmMsgTv.setText(getResources().getString(R.string.e3_e4_alarm_both_hardware_disconnect));
                        }else{
                            mAlarmMsgTv.setText("E4-"+getResources().getString(R.string.e4_alarm_pcr_hardware_disconnect));
                        }
                    }else{
                        mPCRStateView.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    }
                }
                break;
            case MsgType.MSG_TEMPLATE_CAPACITY_DONE:
                replaceFragment(mTemplateFragment, mEntryInfoFragment);
                mTemplateFragment = null;
                description = (String) messageEvent.getData();
                getTotalTime();
                openDoor();//开门
                break;

            case MsgType.MSG_ENTRY_INFO_DONE:
                replaceFragment(mEntryInfoFragment, mRunningBiowayFragment);
                mEntryInfoFragment = null;
                List<WellInfo> list = (List<WellInfo>) messageEvent.getData();
                wellInfos.clear();
                wellInfos.addAll(list);
                //将信息存入文件
                if (null != wellInfos) {
                    String detectInfo = JSON.toJSONString(wellInfos);
                    boolean saveResult = FileUtils.dumpDeviceInfo(detectInfo, "detectInfo");
                }
                startCountDown();
                mStopBtn.setVisibility(View.VISIBLE);
                break;

            case MsgType.MSG_START_SLAN_FIRST_PCR:
                Log.i("BIOWAY", "开始检测第一次PCR");
                String slan_FilePath1 = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName;
                startBioWayPCR(slan_FilePath1, "first", 1);
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_DONE:
                Log.i("BIOWAY", "结束第一次PCR检测");
                if (PcrApplication.isHasSecondRNA) {
                    resetPcrRecMark();
                }else{
                    onSLANExperimentFinish();
                }
                break;

            case MsgType.MSG_START_SLAN_SECOND_PCR:
                Log.i("BIOWAY", "开始检测第二步PCR");
                String slan_FilePath2 = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName;
                startBioWayPCR(slan_FilePath2, "second", 2);
                break;
            case MsgType.MSG_SECOND_SLAN_PCR_DONE:
                Log.i("BIOWAY", "结束第二步PCR检测");
                resetPcrRecMark();
                onSLANExperimentFinish();
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_INTERRUOT:

                if(messageEvent.getData() != null) {
                    RunningInfo firstCode = (RunningInfo) messageEvent.getData();
                    String warning = firstCode.getCurrentError();
                  //  Log.i("RNA", "第一步PCR检测中断 firstCode" + firstCode.get);
                    Log.i("RNA", "第一步PCR检测中断 getCurrentError" + warning);
                    showAlarm();
                    showNoticeDialog(getResources().getString(R.string.dialog_title_check_equipment),
                            getResources().getString(R.string.running_dialog_message_first_pcr_inerrupt)
                                    + " " + warning);
                    mAlarmMsgTv.setText("E13-" + getResources().getString(R.string.e13_alarm_first_step_pcr_interrupt)
                            +" "
                            + warning);
                }else{
                    Log.i("RNA", "第一步PCR手动检测中断 firstCode");
                    showAlarm();
                    showNoticeDialog(getResources().getString(R.string.dialog_title_attention),
                            getResources().getString(R.string.running_dialog_message_manualfirst_pcr_inerrupt));
                    mAlarmMsgTv.setText("E12-" + getResources().getString(R.string.e12_alarm_manual_first_step_pcr_interrupt));
                }
                stopCountDown();
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_ERROR:
                if(!isSecretSetting) {
                    SlanError firstError = (SlanError) messageEvent.getData();
                    Log.i("RNA", "第一次PCR检测错误 firstCode" + firstError);
                    showAlarm();
                    showNoticeDialog(getResources().getString(R.string.dialog_title_check_equipment),
                            getResources().getString(R.string.e14_alarm_first_step_pcr_run_error)
                                    + "," + firstError.getMessage()
                                    + getResources().getString(R.string.dialog_title_check_equipment));
                    mAlarmMsgTv.setText("E14-" + getResources().getString(R.string.e14_alarm_first_step_pcr_run_error) + firstError.getMessage());
                    mChronTimer.stop();
                }
                break;
            case MsgType.MSG_SECOND_SLAN_PCR_INTERRUOT:
                if(messageEvent.getData() != null) {
                    RunningInfo secondCode = (RunningInfo) messageEvent.getData();
                    String wanring = secondCode.getCurrentError();
                    Log.i("RNA", "第二步PCR检测中断 firstCode" + secondCode);
                    Log.i("RNA", "第二步PCR检测中断 getCurrentError" + wanring);
                    showAlarm();
                    showNoticeDialog(getResources().getString(R.string.dialog_title_check_equipment),
                            getResources().getString(R.string.running_dialog_message_second_pcr_interrupt) + " " + wanring);
                    mAlarmMsgTv.setText("E16-" + getResources().getString(R.string.e16_alarm_second_step_pcr_interrupt));
                }else{
                    Log.i("RNA", "第二步PCR手动检测中断 firstCode");
                    showAlarm();
                    showNoticeDialog(getResources().getString(R.string.dialog_title_attention),
                            getResources().getString(R.string.running_dialog_message_manual_second_pcr_interrupt));
                    mAlarmMsgTv.setText("E15-" + getResources().getString(R.string.e15_alarm_manual_second_step_pcr_interrupt));
                }
                stopCountDown();
                break;
            case MsgType.MSG_SECOND_SLAN_PCR_ERROR:
                if(!isSecretSetting) {
                    Log.i("RNA", "第二步PCR检测错误");
                    SlanError secondError = (SlanError) messageEvent.getData();
                    showAlarm();
                    showNoticeDialog("请检查设备", "第二步PCR运行错误,"+ secondError.getMessage());
                    mAlarmMsgTv.setText("E17-第二步PCR运行错误," + secondError.getMessage());
                    stopCountDown();
                }
                break;

            case MsgType.MSG_UPDATE_EX_RESULT:
                stopCountDown();
                showUpdateDbDialog();
                break;

            case MsgType.MSG_SHOW_ALARM_MESSAGE:
                String alarmMsg = (String) messageEvent.getData();
                mAlarmMsgTv.setText(alarmMsg);
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
        mAlarmMsgTv.setText("E20-"+ getResources().getString(R.string.e20_command_timeout) + text);
        //stopAnimation_Interupt();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WarningMessage warningMessage) {
        String json = GsonUtil.toJson(warningMessage, true);
        Log.e("WarningMessage", "警报消息 :" + json);
        if(!isSecretSetting) {
            mAlarmMsgTv.setText(warningMessage.getAlarm() + "-" +
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
                Log.i("RNA", "RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
            } else {

                Log.i("RNA", "RNA指令运行---" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
            }
            PcrApplication.rNARunningMessage = rNARunningMessage;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {

        if (procedureFinishEvent.getTemplateName().contains("downforerror")) {
            if (procedureFinishEvent.getStatus().equals("finish")) {
                hideLoadingDialog();
            } else if (procedureFinishEvent.getStatus().equals("finish error")) {

                Toast.makeText(getApplicationContext(), "SendCmdTimeoutEvent  :  " + "finish error11111111111", Toast.LENGTH_LONG).show();
                hideLoadingDialog();
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.dialog_title_check_equipment),
                        "万能下机指令出错，请检查设备!");
                mAlarmMsgTv.setText("E19-"+getResources().getString(R.string.e19_alarm_offline_universal_command_error));
            }
        } else if (procedureFinishEvent.getTemplateName().contains("downandopen")) {
            if(PcrApplication.is_SLAN_PCR_connect) {
//                stopExperiment();
            }
            if (procedureFinishEvent.getStatus().equals("finish")) {
               // hideProgressDialog();
                isdownAndOpen = false;//20250208
                PcrApplication.rNARunningMessage.setStatus("free");
                //showNoticeDialog(getResources().getString(R.string.dialog_title_prompt), "操作完成");
                //stopAnimation();
            } else if (procedureFinishEvent.getStatus().equals("finish error")) {

               // hideProgressDialog();
                showAlarm();
                isdownAndOpen = false;//20250208
                //stopAnimation_Interupt();//20250208
                showNoticeDialog(getResources().getString(R.string.dialog_title_check_equipment),
                        getResources().getString(R.string.running_dialog_message_offline_command_error));
                mAlarmMsgTv.setText("E18-"+getResources().getString(R.string.e18_alarm_offline_command_error));
            }
            stopCountDown();
        }else{
            if(procedureFinishEvent.getStatus().contains("error")){
               // stopAnimation_Interupt();
            }
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.bioway_detect_tab_back_img) {
            onBack();
        } else if (id == R.id.bioway_detect_tab_stop_img) {
            if(!isdownAndOpen){
                if (isPCRRunning || RNAAndPCRAsyncTask.getInstance().ismIsrunning()) {
                    showInterruptDialog();
                } else {
                    if (!isPCRRunning) {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.running_toast_pcr_program_finish),Toast.LENGTH_LONG).show();
                    }
                    if (!RNAAndPCRAsyncTask.getInstance().ismIsrunning()) {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.running_toast_rna_program_finish),Toast.LENGTH_LONG).show();
                    }
                }
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.running_toast_stop_in_downandopen),Toast.LENGTH_LONG).show();
            }

        } else if (id == R.id.bioway_detect_running_sceentshot_tv) {
            if(!isdownAndOpen){
                screenShot();
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.running_toast_stop_in_downandopen),Toast.LENGTH_LONG).show();
            }

        } else if (id == R.id.bioway_pcr_sound_tv) {
            if (isMute) {
                isMute = false;
            } else {
                isMute = true;
            }
            setVolumeState(isMute);
        }else if(id == R.id.bioway_counting_alarm_img){
            clearCountingEnd();
            countAlarmImg.setVisibility(View.INVISIBLE);
        }
    }


    /**
     * 万能下机操作
     */
    private void downAndOpen_Error() {
        String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "95-downforerror.hex";
        File file = new File(downandopen_filePath);

        if (!RNAAndPCRAsyncTask.getInstance().ismIsrunning() && !isPCRRunning) {
            ////异步全流程
            List<HexDebugEntity> HexDebugEntityList = ListDataSaveUtil.getHexDebugEntityList(file);
            if (RNAAndPCRAsyncTask.getInstance().ismIsrunning()) {
                Toast.makeText(getApplicationContext(), "RNAAndPCRAsyncTask is running, please wait", Toast.LENGTH_LONG).show();
                return;
            }
            RNAAndPCRAsyncTask.current_template = file.getName();
            RNAAndPCRAsyncTask.current_template_phase = 0;
            RNAAndPCRAsyncTask.current_template_phase_step = 0;
            RNAAndPCRAsyncTask.getInstance().setmContext(BiowayActivity.this);

            //RNAAndPCRAsyncTask.getInstance().cancel(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                //根据不同的api采用并行模式进行开启
                //////启动流程的API
                RNAAndPCRAsyncTask.getInstance().executeOnExecutor(THREAD_POOL_EXECUTOR, HexDebugEntityList);

            } else {
                RNAAndPCRAsyncTask.getInstance().execute(HexDebugEntityList);
            }

            showLoadingDialog("下机指令执行中，请等待");

        } else {
            if (RNAAndPCRAsyncTask.getInstance().ismIsrunning()) {
                Toast.makeText(getApplicationContext(),"RNA程序还没结束",Toast.LENGTH_LONG).show();
            } else if (!isPCRRunning) {
                Toast.makeText(getApplicationContext(),"PCR程序还没中断",Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * 自动下机操作
     */
    private boolean isdownAndOpen = false;
    /**
     * 下机操作
     */
    private void downAndOpen() {
        resetPcrRecMark();
        String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "97-downandopen.hex";
        File file = new File(downandopen_filePath);

        if (!RNAAndPCRAsyncTask.getInstance().ismIsrunning() && !isPCRRunning) {
            ////异步全流程
            List<HexDebugEntity> HexDebugEntityList = ListDataSaveUtil.getHexDebugEntityList(file);
            if (RNAAndPCRAsyncTask.getInstance().ismIsrunning()) {
                Toast.makeText(getApplicationContext(), "RNAAndPCRAsyncTask is running, please wait", Toast.LENGTH_LONG).show();
                return;
            }
            RNAAndPCRAsyncTask.current_template = file.getName();
            RNAAndPCRAsyncTask.current_template_phase = 0;
            RNAAndPCRAsyncTask.current_template_phase_step = 0;
            RNAAndPCRAsyncTask.getInstance().setmContext(BiowayActivity.this);

            //RNAAndPCRAsyncTask.getInstance().cancel(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                //根据不同的api采用并行模式进行开启
                //////启动流程的API
                RNAAndPCRAsyncTask.getInstance().executeOnExecutor(THREAD_POOL_EXECUTOR, HexDebugEntityList);

            } else {
                RNAAndPCRAsyncTask.getInstance().execute(HexDebugEntityList);
            }

           // showProgressDialog( "下机指令执行中，请等待");

        } else {
            if (RNAAndPCRAsyncTask.getInstance().ismIsrunning()) {
                isdownAndOpen = false;
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.running_toast_rna_program_no_finish),Toast.LENGTH_LONG).show();
            } else if (!isPCRRunning) {
                isdownAndOpen = false;
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.running_toast_pcr_program_no_interrupt),Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openDoor(){
        MessageEvent openMsg = new MessageEvent();
        openMsg.setMsgType(MsgType.MSG_OPEN_DOOR);
        EventBus.getDefault().postSticky(openMsg);
    }

    /**
     * 截屏操作
     **/
    private void screenShot() {
        final ImageView screenshot_Bmp;
        String aftertime = DateFormatUtil.convertDateFormat(PcrApplication.ex_startTime);
        String subFirName = aftertime + "_" + PcrApplication.rootTempName;
        File dir = new File(FileUtils.IMAGE_PATH, subFirName);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        Bitmap bitmap = ScreenShotUtil.saveScreenShot(BiowayActivity.this, dir);
        screenshot_Bmp = new ImageView(this);
        screenshot_Bmp.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        screenshot_Bmp.setImageBitmap(bitmap);
        main_root_layout.addView(screenshot_Bmp);
        ScaleAnimation scaleAnimation = (ScaleAnimation) AnimationUtils
                .loadAnimation(BiowayActivity.this.getApplicationContext(), R.anim.screen_cut);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                if (main_root_layout != null)
                    main_root_layout.removeView(screenshot_Bmp);
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.running_toast_screenshot_done),Toast.LENGTH_LONG).show();
            }
        });
        screenshot_Bmp.startAnimation(scaleAnimation);

    }

    private void setVolumeState(boolean isMute) {
        if (isMute) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
            mVolumeImg.setImageResource(R.drawable.icon_mute);
            //如果警报
            clearAlarm();
        } else {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, alarmVolume, AudioManager.FLAG_PLAY_SOUND);
            mVolumeImg.setImageResource(R.drawable.icon_no_mute);
        }
    }


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

                    updateExperiment();
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

    /**
     * 更新实验结果
     */
    private void updateExperiment() {
        String ex_testItem = PcrApplication.rootTempName;
        String ex_startTime = PcrApplication.ex_startTime;
        if (!StringUtil.isEmpty(ex_testItem) && !StringUtil.isEmpty(ex_startTime)) {
            updateBiowayResultToPCR(ex_startTime, ex_testItem);
            BiowayManager.getInstance().updateBiowayExperiment(ex_testItem, ex_startTime);
        } else {
            if (StringUtil.isEmpty(ex_testItem)) {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.running_toast_save_pcr_item_error),Toast.LENGTH_LONG).show();
            } else if (StringUtil.isEmpty(ex_startTime)) {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.running_toast_save_pcr_time_error),Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 第一次SLAN pcr结束
     * 保存pcr文件以及保存数据库文件
     */

    private void onSLANExperimentFinish() {
        getLocalFileList();
        File ctFile = FileUtils.getFile(localList, ".ct");
        File peakFile = FileUtils.getFile(localList, ".peak");
        try {
            boolean isSecret = PreferenceUtil.getInstance(getApplicationContext())
                    .getBooleanValue(SharedConstant.SECRET_SETTING,false);
            Experiment experiment = BoWeiService.getInstance().getExperimentResult();
            BoweiJni.analysisExperimentLoessFilter(experiment);
            if(isSecret) {
                //打开准备的好的.pcr文件
                String path = getPCRFilePath();
                Experiment des_experiment = null;
                if(!path.equals("")){
                    des_experiment = BoWeiService.getInstance().openExperiment(path);
                }
                if(null != des_experiment){
                    if (ctFile != null) {
                        ctConclusions.clear();
                        ctConclusions = BiowayManager.getInstance()
                                .judgeCTFile(des_experiment, ctFile,PcrApplication.ex_startTime);
                    }
                    if (peakFile != null) {
                        peakConclusions.clear();
                        peakConclusions = BiowayManager.getInstance()
                                .judgePEAKFile(des_experiment, peakFile,PcrApplication.ex_startTime);
                    }
                }else{
                    if (ctFile != null) {
                        ctConclusions.clear();
                        ctConclusions = BiowayManager.getInstance()
                                .judgeCTFile(experiment, ctFile,PcrApplication.ex_startTime);
                    }
                    if (peakFile != null) {
                        peakConclusions.clear();
                        peakConclusions = BiowayManager.getInstance()
                                .judgePEAKFile(experiment, peakFile,PcrApplication.ex_startTime);
                    }
                }
            }else{
                if (experiment == null) {
                    Log.e("RNA", "getExperimentResult" + experiment);
                    return;
                }
                ctConclusions.clear();
                peakConclusions.clear();
                if (ctFile != null) {
                    ctConclusions = BiowayManager.getInstance()
                            .judgeCTFile(experiment, ctFile,PcrApplication.ex_startTime);
                }
                if (peakFile != null) {
                    peakConclusions = BiowayManager.getInstance()
                            .judgePEAKFile(experiment, peakFile,PcrApplication.ex_startTime);
                }
            }
            saveBiowayExperiment(experiment);
        } catch (Exception e) {
            e.getMessage();
            Log.e("onSLANExperimentFinish",e.getMessage());
        }


    }

    @Override
    public void onSaveExToDbResult(boolean result) {
        Log.i("Bioway", "onSaveExToDbResult" + result);
        if (result) {
            // Toast.makeText(getApplicationContext(),"保存数据库成功",Toast.LENGTH_LONG).show();
            //删除detectInfo文件
            File dir = new File(FileUtils.INFO_PATH);
            File[] files = dir.listFiles();
            if (null != files && files.length > 0) {
                for (File file : files) {
                    file.delete();
                }
            }
            Log.i("Bioway", "保存数据库成功");
        } else {
            Log.i("Bioway", "保存数据库失败");
        }
    }

    /**
     * 解析结束，跳转
     */
    @Override
    public void onParseFinish() {
        Log.i("Bioway", "解析完成");
        MessageEvent resultMsg = new MessageEvent();
        resultMsg.setMsgType(MsgType.MSG_SHOW_RESULT);
        EventBus.getDefault().postSticky(resultMsg);
        mUIHandler.sendEmptyMessage(MsgType.MSG_PARSE_FINISH);
    }

    @Override
    public void onSaveJsonResult(boolean result,String message) {
        //
        Log.i("Bioway","保存json结果" + result);
    }

    //二次分析解析的结果
    @Override
    public void onSecondParseFinish(boolean result) {

        if (result) {
            MessageEvent resultMsg = new MessageEvent();
            resultMsg.setMsgType(MsgType.MSG_SHOW_SECOND_RESULT);
            EventBus.getDefault().postSticky(resultMsg);
        } else {
            String failMsg = getResources().getString(R.string.toast_second_analyse_fail);
            MessageEvent failEvent = new MessageEvent();
            failEvent.setMsgType(1);
            failEvent.setData(failMsg);
            EventBus.getDefault().postSticky(failEvent);
        }
    }

    //二次分析结果更新
    @Override
    public void onUpdateFinish(boolean result) {
        if (result) {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.result_ex_detail_toast_update_result_success),Toast.LENGTH_LONG).show();
            Log.i("Bioway", "更新结果成功");
        } else {
            Log.i("Bioway", "更新结果失败");
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.result_ex_detail_toast_update_result_fail),Toast.LENGTH_LONG).show();
        }
    }


    public static class UIHandler extends Handler {

        WeakReference<BiowayActivity> mActivityReference;

        public UIHandler(BiowayActivity mActivity) {
            mActivityReference = new WeakReference<BiowayActivity>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final BiowayActivity mActivity = mActivityReference.get();
            if (null == mActivity) {
                return;
            }

            switch (msg.what) {
                case Constants.MSG_UPDATE_SYSTEM_TIME:
                    mActivity.mDateTimeView.setDateText(DateFormatUtil.getNowDate());
                    mActivity.mDateTimeView.setTimeText(DateFormatUtil.getNowTime());
                    break;

                case MsgType.MSG_PARSE_FINISH:
                    mActivity.mSceenShotBtn.setVisibility(View.VISIBLE);
                    mActivity.mSceenShotBtn.setEnabled(true);
                    // mActivity.stopAnimation();//20250208
                    mActivity.downAndOpen();
                    break;

                case MsgType.MSG_COUNTDOWN_END:
                    mActivity.showCountingEnd();
                    mActivity.countAlarmImg.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }



    private LoadingDialog loadingDialog ;
    public void showLoadingDialog(String message){
        if(null == loadingDialog && null != this  &&  !this.isFinishing()){
            loadingDialog = new LoadingDialog(this, message);
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }
    }

    /*
     * 隐藏提示加载
     */
    public void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }


    /**
     * 开始进行运行实验
     *
     * @param pcrFilePath
     */

    private void startBioWayPCR(String pcrFilePath,String pcrProjectName,int currentPcr){
        this.current_pcr = currentPcr;
        PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");
        if (PCR_RUNNING_TEMPLATE.equals("")) {
            if(current_pcr == 1){
                setPcrRecMark(1,0);

            }else if(current_pcr == 2){
                setPcrRecMark(2,0);
            }
        }
        if(PcrApplication.is_SLAN_PCR_connect){
            if (true) {
                ArrayList<Well> wells = new ArrayList<>();
                if(null != wellInfos && wellInfos.size()>0) {
                    for (int i = 1; i <= wellInfos.size(); i++) {
                        WellInfo wellInfo = wellInfos.get(i - 1);
                        Well well = new Well();
                        //孔名称
                        well.setName(wellInfo.getWellName());
                        //项目名称
                        well.setProjectName(pcrProjectName);
                        //患者信息.唯一标示
                        well.setID("A" + (i));
                        //姓名
                        well.setPatientName("PatientName");
                        //性别
                        well.setSex("Sex");
                        //年龄
                        well.setAge("Age");
                        //标本类型
                        well.setSpecimen("Specimen");
                        //采样日期
                        well.setSpecimenDate("SpecimenDate");
                        //送检医生
                        well.setDoctor("Doctor");
                        //送检科室
                        well.setOffice("Office");
                        //诊断
                        well.setDiagnosis("Diagnosis");
                        //病历号
                        well.setCaseID("caseID");
                        //病床号
                        well.setBedID("BedID");
                        //住院号
                        well.setInPatientID("InPatientID");
                        //门诊号
                        well.setOutPatientID("OutPatientID");
                        //备注
                        well.setRemark("Remark");
                        //样本名称
                        well.setSampleName(wellInfo.getSampleName());//身份證號碼
                        //试管编号
                        well.setTubeNo(i);//编号
                        //试管名称
                        well.setTubeName(wellInfo.getReagentCode());
                        //样本类型
                        well.setType(SampleType.Unknown.getValue());
                        //病历号
                        well.setCaseID(wellInfo.getSampleName());
                        //属性
                        well.setProperty("Property");
                        wells.add(well);
                    }
                }else{
                    //将保存的
                    String filePath = FileUtils.INFO_PATH + "detectInfo.txt";
                    String detectInfoString = FileUtils.getStringFromSDFile(filePath);
                    Type type = new TypeToken<List<WellInfo>>() {
                    }.getType();
                    Gson gson = new Gson();
                    wellInfos = gson.fromJson(detectInfoString, type);
                    if(null != wellInfos && wellInfos.size()>0){
                        for (int i = 1; i <= wellInfos.size(); i++) {
                            WellInfo wellInfo = wellInfos.get(i - 1);
                            Well well = new Well();
                            //孔名称
                            well.setName(wellInfo.getWellName());
                            //项目名称
                            well.setProjectName(pcrProjectName);
                            //患者信息
                            //唯一标示
                            well.setID("A" + (i));
                            //姓名
                            well.setPatientName("PatientName");
                            //性别
                            well.setSex("Sex");
                            //年龄
                            well.setAge("Age");
                            //标本类型
                            well.setSpecimen("Specimen");
                            //采样日期
                            well.setSpecimenDate("SpecimenDate");
                            //送检医生
                            well.setDoctor("Doctor");
                            //送检科室
                            well.setOffice("Office");
                            //诊断
                            well.setDiagnosis("Diagnosis");
                            //病历号
                            well.setCaseID("caseID");
                            //病床号
                            well.setBedID("BedID");
                            //住院号
                            well.setInPatientID("InPatientID");
                            //门诊号
                            well.setOutPatientID("OutPatientID");
                            //备注
                            well.setRemark("Remark");
                            //样本信息
                            //身份证信息
                            well.setSampleName(wellInfo.getSampleName());
                            //试管编号
                            well.setTubeNo(1);
                            //试管名称
                            well.setTubeName(wellInfo.getReagentCode());
                            //病历号
                            well.setCaseID(wellInfo.getSampleName());
                            //样本类型
                            well.setType(SampleType.Unknown.getValue());
                            //属性
                            well.setProperty("Property");
                            wells.add(well);
                        }
                    }
                }
                ExperimentParam runExParam = new ExperimentParam();
                runExParam.setPath(pcrFilePath);
                runExParam.setName(pcrProjectName);
                runExParam.setWells(wells);

                if(null != runExParam){
                    RunBiowayResult runExResult = BoWeiService.getInstance().runExperiment(runExParam);
                    Log.i("BIOWAY", "runExResult" + runExResult);
                    if (runExResult.getRunExperimentResult() == RunExperimentResult.Normal) {
                        //运行成功
                        isPCRRunning = true;
                        if (current_pcr == 1) {
                            MessageEvent bioway_start_event = new MessageEvent();
                            bioway_start_event.setMsgType(MsgType.MSG_START_SLAN_FIRST_PCR_SUCCESS);
                            bioway_start_event.setData(runExResult.getProject());
                            EventBus.getDefault().postSticky(bioway_start_event);
                            setPcrRecMark(1, 0);

                        } else if (current_pcr == 2) {
                            MessageEvent bioway_second_event = new MessageEvent();
                            bioway_second_event.setMsgType(MsgType.MSG_START_SLAN_SECOND_PCR_SUCCESS);
                            bioway_second_event.setData(runExResult.getProject());
                            EventBus.getDefault().postSticky(bioway_second_event);
                            setPcrRecMark(2, 0);
                        }
                    } else {
                        isPCRRunning = false;
                        if (current_pcr == 1) {
                            MessageEvent hs_first_error = new MessageEvent();
                            hs_first_error.setMsgType(MsgType.MSG_FIRST_SLAN_PCR_ERROR);
                            if (runExResult.getRunExperimentResult() == RunExperimentResult.ProjectNoFindError) {
                                hs_first_error.setData(SlanError.PROJECT_NO_FOUND);
                            } else if (runExResult.getRunExperimentResult() == RunExperimentResult.RunningError) {
                                hs_first_error.setData(SlanError.RUNNING_ERROR);
                            } else if (runExResult.getRunExperimentResult() == RunExperimentResult.USBNoFindError) {
                                hs_first_error.setData(SlanError.USB_NO_FOUND);
                            } else if (runExResult.getRunExperimentResult() == RunExperimentResult.WellError) {
                                hs_first_error.setData(SlanError.WELL_ERROR);
                            }
                            EventBus.getDefault().postSticky(hs_first_error);
                        } else if (current_pcr == 2) {
                            MessageEvent hs_second_error = new MessageEvent();
                            hs_second_error.setMsgType(MsgType.MSG_SECOND_SLAN_PCR_ERROR);
                            if (runExResult.getRunExperimentResult() == RunExperimentResult.ProjectNoFindError) {
                                hs_second_error.setData(SlanError.PROJECT_NO_FOUND);
                            } else if (runExResult.getRunExperimentResult()== RunExperimentResult.RunningError) {
                                hs_second_error.setData(SlanError.RUNNING_ERROR);
                            } else if (runExResult.getRunExperimentResult() == RunExperimentResult.USBNoFindError) {
                                hs_second_error.setData(SlanError.USB_NO_FOUND);
                            } else if (runExResult.getRunExperimentResult() == RunExperimentResult.WellError) {
                                hs_second_error.setData(SlanError.WELL_ERROR);
                            }
                            EventBus.getDefault().postSticky(hs_second_error);
                        }
                    }
                }
            }
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.running_toast_pcr_disconnect),Toast.LENGTH_LONG).show();
            isPCRRunning = false;
            if (current_pcr == 1) {
                MessageEvent hs_first_error = new MessageEvent();
                hs_first_error.setMsgType(MsgType.MSG_FIRST_SLAN_PCR_ERROR);
                hs_first_error.setData(SlanError.PCR_DISCONNECT);
                EventBus.getDefault().postSticky(hs_first_error);
            } else if (current_pcr == 2) {
                MessageEvent hs_second_error = new MessageEvent();
                hs_second_error.setMsgType(MsgType.MSG_SECOND_SLAN_PCR_ERROR);
                hs_second_error.setData(SlanError.PCR_DISCONNECT);
                EventBus.getDefault().postSticky(hs_second_error);
            }
        }
    }



    /**
     * 更新PCR文件
     */
    private void updateBiowayResultToPCR(String startTime, String testItem) {
        Experiment experiment = BiowayManager.getInstance().getExperiment();
        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName;
        if(StringUtil.isEmpty(description)){
            subFirName = afterTime + "_" + testItem;
        }else{
            subFirName = afterTime + "_" + testItem + "_" + description;
        }
        File jsonDir = new File(FileUtils.BIOWAY_RESULT_PATH, subFirName);
        if (jsonDir.exists()) {
            File[] files = jsonDir.listFiles();
            if (files.length > 0) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        saveResultToPCR(experiment, subFirName);
    }

    /****************************************实验操作***********************************************/
    public void saveBiowayExperiment(Experiment experiment) {
        boolean isSecret = PreferenceUtil.getInstance(getApplicationContext())
                .getBooleanValue(SharedConstant.SECRET_SETTING,false);
        String testItem = PcrApplication.rootTempName;
        String startTime = PcrApplication.ex_startTime;
        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName;
        if(StringUtil.isEmpty(description)){
            subFirName = afterTime + "_" + testItem;
        }else{
            subFirName = afterTime + "_" + testItem + "_" + description;
        }
        Experiment experiment_device = experiment;
        if(isSecret){
            //打开准备的好的.pcr文件
            String path = getPCRFilePath();
            Experiment des_experiment = null;
            if(!path.equals("")){
                des_experiment = BoWeiService.getInstance().openExperiment(path);
            }
            if(null != des_experiment){
                saveResultToPCR(des_experiment,subFirName);
                saveExperimentResult(des_experiment,subFirName);
                String src_starttime = experiment_device.getStartTime();
                String src_endtime = experiment_device.getEndTime();
                String file_name = testItem + PcrApplication.ex_startTime;
                des_experiment.setStartTime(src_starttime);
                des_experiment.setEndTime(src_endtime);
                des_experiment.setName(file_name);
            }else{
                saveResultToPCR(experiment_device,subFirName);
                saveExperimentResult(experiment_device,subFirName);

            }
        }else{
            //数据库
            saveExperimentResult(experiment_device,subFirName);
            //本地
            saveResultToPCR(experiment_device,subFirName);
        }
        //上传实验数据
    }

    public  void saveResultToPCR(Experiment experiment, String subFirName) {
        Log.e("RNA", "saveResultToPCR");
       if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
           Toast.makeText(getApplicationContext(),"保存PCR文件失败,内存位置有误",Toast.LENGTH_LONG).show();
           return ;
       }
       if (experiment == null) {
           Toast.makeText(getApplicationContext(),"保存PCR文件失败,Experiment为空",Toast.LENGTH_LONG).show();
           return;
       }

       File dir = new File(FileUtils.BIOWAY_RESULT_PATH, subFirName);
       if (!dir.exists()) {
           dir.mkdirs();
       }
                //////蒋老师意思，屏蔽测试
        String saveFileName = DateFormatUtil.getFileName();
        String savePath = FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;
        BoWeiService.getInstance()
                .saveExperiment(experiment, saveFileName, savePath, new OnSaveExperimentListener() {
                    @Override
                    public void onSaveSuccess() {
                        Toast.makeText(getApplicationContext(),"保存PCR成功",Toast.LENGTH_LONG).show();
                        Log.e("save","onSaveSuccess" );
                    }

                    @Override
                    public void onSaveFail() {
                        Toast.makeText(getApplicationContext(),"保存PCR失败",Toast.LENGTH_LONG).show();
                        Log.e("save","onSaveFail" );
                    }
                });

        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        if(null != ctConclusions && null != peakConclusions){
            //打印出来
            JudgeConclusion judgeConclusion = new JudgeConclusion();
            judgeConclusion.setTemplateName(PcrApplication.rootTempName);
            judgeConclusion.setCtRuleConclusions(ctConclusions);
            judgeConclusion.setPeakRuleConclusions(peakConclusions);
            String judgeConclusionResult = gson.toJson(judgeConclusion);
            File judgeConclusionFile = FileUtils.dumpDeviceInfo(judgeConclusionResult, "judgeConclusion.result",dir);
        }
    }


    /**
     * 解析和保存数据库
     * @param experiment
     */
    private void saveExperimentResult(Experiment experiment,
                                      String subFirName) {

        try {
            if (experiment == null) {
                return;
            } else {
                //解析实验
                String endTime = DateFormatUtil.getSysFullTime();
                String savePath = FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;
                BiowayManager.getInstance().parseExperimentResult(experiment, ctConclusions,peakConclusions);
                BiowayManager.getInstance().saveBiowayExResultToDB(experiment,savePath,
                        PcrApplication.ex_startTime,endTime, PcrApplication.rootTempName,description ,ctConclusions,peakConclusions);
            }
        } catch (Exception e) {
            Log.e("getExperimentResult", "getExperimentResult = " + e.getMessage());
        }
    }




    /**
     * 停止实验
     */
    public void stopExperiment() {
        BoWeiService.getInstance().stopExperiment((byte) 0x01);
    }

/*    *//**
     * 继续实验
     *//*
    public void continueExperiment() {
        SLANService.INSTANCE.continueExperiment(PcrApplication.devices.get(0).getSerialNo(), (byte) 0x01);
    }*/

    /**
     * 获取实验参数
     */
    /************************************************************************************************/
    public void showNoticeDialog(String title, String message) {
        if (null != mNoticeDialog) {
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        if (null == mNoticeDialog) {
            mNoticeDialog = new WarnDialog(BiowayActivity.this);
            LayoutInflater mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView) view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);
            WarnDialog.Builder builder = new WarnDialog.Builder(BiowayActivity.this);
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setSingleButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    clearAlarm();
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
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
                .setValueByName(SharedConstant.PROCEDURE_RUNNING_TIME, PcrApplication.runningTime);
    }

    private void stopCountDown(){
        if(mChronTimer != null){
            mChronTimer.stop();
        }
    }

    /**
     * 倒计时
     */
   private void startCountDown(){

       PcrApplication.runningTime = PcrApplication.totalTime;
       // 倒计时
       mChronTimer.setBase(SystemClock.elapsedRealtime() + PcrApplication.runningTime*1000);
       // 倒计时，false 为正计时
       mChronTimer.setCountDown(true);
       //
       mChronTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
           @Override
           public void onChronometerTick(Chronometer chronometer) {
               if(chronometer.getBase() <=0){
                   chronometer.stop();
               }
               long base = chronometer.getBase();
               long cur1 = SystemClock.elapsedRealtime();
               long last_time = base - cur1;
               String totalTime = DateFormatUtil.secondToTime(PcrApplication.totalTime);
               int textLength = mChronTimer.getText().length();
               if(textLength == 5){
                   mChronTimer.setText("00:" + mChronTimer.getText().toString() + " / " + totalTime);
               }else if(textLength == 7){
                   mChronTimer.setText("0" + mChronTimer.getText().toString() + " / " + totalTime);
               }
               PreferenceUtil.getInstance(getApplicationContext())
                       .setValueByName(SharedConstant.PROCEDURE_RUNNING_TIME, last_time/1000);
               if(last_time <=0){
                   chronometer.stop();
                    if(null != mUIHandler){
                        mUIHandler.sendEmptyMessage(MsgType.MSG_COUNTDOWN_END);
                    }
                }
            }
        });

       mChronTimer.start();
   }
}
