package com.gidi.bio_pcr.user.running;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.ActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.StepIndexMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.PreferenceUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

/**
 * 第一次核酸检测
 */
public class UserFirstRNAFragment extends BaseFragment {

    private RelativeLayout mCounterLayout;
    private Chronometer mChronTimer;
    private TextView mRemainTimeTv;
    private ImageView mRemainTimeImg;

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_user_first_rna;
    }

    @Override
    protected void initView(View rootView) {
        initCountView(rootView);
    }

    @Override
    protected void initListener() {

    }

    private void initCountView(View rootView){
        mCounterLayout = (RelativeLayout)rootView.findViewById(R.id.user_running_first_counter_layout) ;
        mChronTimer = (Chronometer)rootView.findViewById(R.id.user_first_rna_chronometer);
        mRemainTimeTv = (TextView) rootView.findViewById(R.id.user_first_rna_counter_tv);
        mRemainTimeImg= (ImageView) rootView.findViewById(R.id.user_first_rna_counter_img);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterEventBus();
        if(null != mChronTimer){
            mChronTimer.stop();
            mChronTimer = null;
        }
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden){
            unregisterEventBus();
        }else {
            registerEventBus();
        }
    }


    /**start/restart chronometer**/
    private void onChronometerStart(boolean recovery){
        Log.i("User", "计时器开始进行计时");
        //正常的倒计时是总的时间，复位重启的倒计时
        if(recovery){
            PcrApplication.runningTime = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                    .getLongValue(SharedConstant.PROCEDURE_RUNNING_TIME, PcrApplication.totalTime);
        }else{
            PcrApplication.runningTime = PcrApplication.totalTime;
        }

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

                PreferenceUtil.getInstance(getActivity().getApplicationContext())
                        .setValueByName(SharedConstant.PROCEDURE_RUNNING_TIME, last_time/1000);
                if(last_time <=0){
                    chronometer.stop();
                }
            }
        });

        mChronTimer.start();
    }

    /**stop chronometer**/
    private void onChronometerStop(){
        if(null!= mChronTimer){
            mChronTimer.stop();
        }
    }

    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()){
            //开始RNA检测的消息进行倒计时
            case MsgType.MSG_START_COUNTDOWN:
                boolean recovery = (boolean)messageEvent.getData();
                onChronometerStart(recovery);
                break;
            case MsgType.MSG_END_COUNTDOWN:
                onChronometerStop();
                break;
            case MsgType.MSG_RNA_FILE_NULL:
               // onChronometerStop();
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_INTERRUOT:
            case MsgType.MSG_SECOND_SLAN_PCR_INTERRUOT:
                //手动中断按钮
                mRemainTimeTv.setTextColor(getActivity().getColor(R.color.red));
                mCounterLayout.setBackgroundResource(R.drawable.countdown_stop_bg);
                mRemainTimeImg.setImageResource(R.drawable.icon_remaining_time_red);
                break;

        }
    }


    /**
     * 运行结束
     * @param procedureFinishEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
        Log.i("User", "RNA测试结束" + procedureFinishEvent.getStatus());

        if(null != procedureFinishEvent){
           // onChronometerStop();
            if(procedureFinishEvent.getStatus().equals("finish")){
                PcrApplication.rNARunningMessage.setStatus("free");
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                mRemainTimeTv.setTextColor(getActivity().getColor(R.color.red));
                mRemainTimeImg.setImageResource(R.drawable.icon_remaining_time_red);
                mCounterLayout.setBackgroundResource(R.drawable.countdown_stop_bg);
            }
        }
    }

    /**
     * 超时消息
     * @param sendCmdTimeoutEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {
        Log.i("User", "RNA测试超时");
        if(null != getActivity()){
            onChronometerStop();
        }
    }


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
        if(hexDebugEntity != null && getActivity() != null){
            Log.i("User", "第一次RNA测试正在进行" + hexDebugEntity.getName() + " 步骤描述" + hexDebugEntity.getDescription() );
        }
    }


    /**
     * 指令正在执行
     * @param rNARunningMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RNARunningMessage rNARunningMessage) {
        Log.i("User", "第一次RNA指令运行" + rNARunningMessage.getID() + "状态" + rNARunningMessage.getStatus());
       // String text = gson.toJson(rNARunningMessage);
        if(rNARunningMessage != null)
        {
            /*if(rNARunningMessage.getStatus().contains("free") || rNARunningMessage.getStatus().contains("done")) {
               .setText(runnning ++ + rNARunningMessage.getStatus()+ ":" + rNARunningMessage.getNO() + rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
            }else{
               mRunningMsgTv.setText(runnning ++ + rNARunningMessage.getStatus()+ ":" + rNARunningMessage.getNO() + rNARunningMessage.getMotorID() + rNARunningMessage.getAlarm());
            }*/
        }
        PcrApplication.rNARunningMessage = rNARunningMessage;
    }

    /**
     * 指令完成
     * @param actionDoneMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ActionDoneMessage actionDoneMessage) {
        if(null != actionDoneMessage && getActivity() != null){
            Log.i("User", "第一次RNA指令完成" + actionDoneMessage.getID() + "状态" + actionDoneMessage.getStatus());
        }
    }
}
