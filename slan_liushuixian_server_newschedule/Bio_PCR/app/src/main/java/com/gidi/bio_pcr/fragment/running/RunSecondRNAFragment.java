package com.gidi.bio_pcr.fragment.running;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.LinearLayout;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.ActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.StepIndexMessage;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.view.CustomTitleText;
import com.google.gson.Gson;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * 第二次核酸检测
 */
public class RunSecondRNAFragment extends BaseFragment {

    private CustomTitleText mProjNameTv;
    private CustomTitleText mRunningNameTv;
    private CustomTitleText mActionTv;
    private CustomTitleText mRunningMsgTv;
    private CustomTitleText mStepStateTv;
    private CustomTitleText mTotalStepTv;
    private CustomTitleText mDoneStepTv;
    private CustomTitleText mTempTv;

    private Chronometer mChronTimer;
    private LinearLayout mStateLayout;
    private boolean isFlag = false;
    private Gson gson;


    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_running_second_rna;
    }

    @Override
    protected void initView(View rootView) {
        mProjNameTv = (CustomTitleText) rootView.findViewById(R.id.running_rna_project_tv);
        mRunningNameTv = (CustomTitleText) rootView.findViewById(R.id.second_rna_template_tv);
        mActionTv = (CustomTitleText)rootView.findViewById(R.id.second_rna_discription_tv);
        mStepStateTv = (CustomTitleText)rootView.findViewById(R.id.second_rna_step_status_tv);
        mStateLayout = (LinearLayout)rootView.findViewById(R.id.second_rna_counter_layout);
        mTotalStepTv= (CustomTitleText)rootView.findViewById(R.id.second_rna_total_step_tv);
        mDoneStepTv = (CustomTitleText)rootView.findViewById(R.id.second_rna_step_done_tv);
        mTempTv = (CustomTitleText)rootView.findViewById(R.id.second_rna_temp_tv);
        mStateLayout.setBackgroundResource(R.drawable.shape_oval_status_bg);
        initCountView(rootView);
    }

    @Override
    protected void initListener() {

    }

    private void initCountView(View rootView){
        mProjNameTv.setContentText(PcrApplication.rootTempName+ File.separator+"second.hex");
        mChronTimer = (Chronometer)rootView.findViewById(R.id.second_rna_chronometer);
        mChronTimer.setText("00:00:00");
        mChronTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                int textLength = mChronTimer.getText().length();
                if(textLength == 5){
                    mChronTimer.setText("00:" + mChronTimer.getText().toString());
                }else if(textLength == 7){
                    mChronTimer.setText("0" + mChronTimer.getText().toString());
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        gson = new Gson();

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerEventBus();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterEventBus();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden){
//            unregisterEventBus();
        }else {
//            registerEventBus();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       // unregisterEventBus();
        if(null != mChronTimer){
            mChronTimer = null;
        }
        unregisterEventBus();
    }

    /**start/restart chronometer**/
    private void onChronometerStart(){
        Log.i("RNA", "第二次RNA计时器开始进行计时");
        if(!isFlag){
            isFlag = !isFlag;
            mChronTimer.setBase(SystemClock.elapsedRealtime());
            if(mChronTimer.getText().equals("00:00")){
                mChronTimer.setText("00:00:00");
            }
            mChronTimer.start();
        }
    }

    /**stop chronometer**/
    private void onChronometerStop(){
        if(isFlag && null!= mChronTimer){
            isFlag = !isFlag;
            mChronTimer.stop();
            //mRecordTime = SystemClock.elapsedRealtime();
        }
    }
    /**reset chronometer*/
    private void onChronometerReset(){
        mChronTimer.stop();
        mChronTimer.setBase(SystemClock.elapsedRealtime());
        //mRecordTime = 0;
        if(mChronTimer.getText().equals("00:00")){
            mChronTimer.setText("00:00:00");
        }
        isFlag = false;
    }

    /*
     * EventBus接收订阅的消息
     * @param
     /

     */
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()){
            case MsgType.MSG_START_SECOND_RNA_RUNNING:
                onChronometerStart();
                break;
            case MsgType.MSG_RNA_FILE_NULL:
                mStateLayout.setBackgroundResource(R.drawable.shape_oval_red_bg);
                onChronometerStop();
                break;
            case MsgType.MSG_DONE_SECOND_RNA_RUNNING:
                onChronometerStop();
                break;

        }
    }


    /**
     * 运行结束
     * @param procedureFinishEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
        Log.i("RNA", "第二次RNA测试结束" + procedureFinishEvent.getStatus());
        onChronometerStop();
        if(procedureFinishEvent.getStatus().equals("finish")){
            PcrApplication.rNARunningMessage.setStatus("free");
        }else if(procedureFinishEvent.getStatus().equals("finish error")){
            mStateLayout.setBackgroundResource(R.drawable.shape_oval_red_bg);
        }
    }

    /**
     * 超时消息
     * @param sendCmdTimeoutEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {
        Log.i("RNA", "第二次RNA测试超时");
        String text = gson.toJson(sendCmdTimeoutEvent.getMsgPacket());
        mStepStateTv.setContentText("CMD timeout : " + text);
        mStateLayout.setBackgroundResource(R.drawable.shape_oval_red_bg);
        mStepStateTv.setContentText(getActivity().getResources().getString(R.string.running_rna_state_timeout));
        onChronometerStop();

    }

    /**
     * 当前的步骤
     * @param stepIndexMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StepIndexMessage stepIndexMessage) {
        if(stepIndexMessage != null){
            mTotalStepTv.setContentText(stepIndexMessage.getStepTotal() + "");
        }

    }

    /**
     * 当前的步骤
     * @param hexDebugEntity
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(HexDebugEntity hexDebugEntity) {
        Log.i("RNA", "第二次RNA测试正在进行" + hexDebugEntity.getName() + " 步骤描述" + hexDebugEntity.getDescription() );
        if(hexDebugEntity != null && getActivity()!= null){
            String jsonText = gson.toJson(hexDebugEntity);
            mRunningNameTv.setContentText(hexDebugEntity.getName());
            mActionTv.setContentText(hexDebugEntity.getDescription());
            mStepStateTv.setContentText(getActivity().getResources().getString(R.string.running_rna_state_running));
        }

    }


    /**
     * 指令正在执行
     * @param rNARunningMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RNARunningMessage rNARunningMessage) {
        Log.i("RNA", "第二次RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
        String text = gson.toJson(rNARunningMessage);
        if(rNARunningMessage != null)
        {
            if(rNARunningMessage.getStatus().contains("free") || rNARunningMessage.getStatus().contains("done")) {
               // mRunningMsgTv.setText(runnning ++ + rNARunningMessage.getStatus()+ ":" + rNARunningMessage.getNO() + rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
            }else{
                // mRunningMsgTv.setText(runnning ++ + rNARunningMessage.getStatus()+ ":" + rNARunningMessage.getNO() + rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
            }
            mTempTv.setContentText(rNARunningMessage.getDegree() + "" + "℃");
        }
        PcrApplication.rNARunningMessage = rNARunningMessage;
    }

    /**
     * 指令完成
     * @param actionDoneMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ActionDoneMessage actionDoneMessage) {
        Log.i("RNA", "第二次指令完成" + actionDoneMessage.getID() + "状态" + actionDoneMessage.getStatus());

        if(null != actionDoneMessage && null != getActivity()){
            String text = gson.toJson(actionDoneMessage);
            mDoneStepTv.setContentText(String.valueOf(actionDoneMessage.getID()));
//            Toast.makeText(getActivity().getApplicationContext(),"RNARunningMessage:" + text,Toast.LENGTH_LONG).show();
        }
    }

}
