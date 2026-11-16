package com.gidi.bio_pcr.fragment.detect;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.ProgressDialogUtil;
import com.gidi.bio_pcr.view.WarnDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

/**
 * 舱门关闭进行下一步
 */
public class DetectWaitFragment extends BaseFragment implements OnClickListener{

    private TextView mOpenBtn;
    private RelativeLayout mWaitLayout;

    private TextView mCountDownTv;//倒计时显示
    private Dialog mProgressDialog;
    private WarnDialog mNoticeDialog;


    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_detect_tray_status;
    }

    @Override
    protected void initView(View rootView) {

        mOpenBtn = (TextView) rootView.findViewById(R.id.detect_wait_open_btn);
        mWaitLayout = (RelativeLayout) rootView.findViewById(R.id.detect_tray_wait_layout);
        mCountDownTv = (TextView) rootView.findViewById(R.id.detect_wait_tray_tv);
      //  initProgressDlg();
    }

    @Override
    protected void initListener() {
        mOpenBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.detect_wait_open_btn) {
            /////临时
            if(PcrApplication.experimentType == 2){
                finishWait();
                return;
            }


            if (PcrApplication.is_RNA_connect) {
                ////异步全流程
                String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "96-opendoor.hex";
                File file = new File(downandopen_filePath);

                boolean result = startAysnTask(file);
                if (result) {
                    showProgressDialog(getActivity().getResources().getString(R.string.dialog_title_wait),
                            getActivity().getResources().getString(R.string.dialog_message_opendoor));
                } else {
                    showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_prompt),
                            getActivity().getResources().getString(R.string.dialog_message_open_error));
                }


            } else {
                Toast.makeText(getActivity().getApplicationContext(),"RNA串口断连",Toast.LENGTH_LONG).show();
            }

        }
    }



    /**
     * 打开舱门等待放入测试
     */
    private void openTray(){
        mOpenBtn.setVisibility(View.GONE);
        mWaitLayout.setVisibility(View.VISIBLE);
        //开启启动线程倒计时显示
        startCounterTimer();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerEventBus();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(null != mProgressDialog){
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        if(null != mNoticeDialog){
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterEventBus();
    }

    private void startCounterTimer(){
        CountDownTimer countDownTimer = new CountDownTimer(5*1000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if(null != mCountDownTv){
                    mCountDownTv.setText(millisUntilFinished/1000 + "s");
                }
            }

            @Override
            public void onFinish() {
                //跳转
               finishWait();
               cancel();
            }
        }.start();
    }


    private void finishWait(){
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setMsgType(MsgType.MSG_WAIT_OPEN_DONE);
        EventBus.getDefault().post(messageEvent);
    }

    private void openDoorError(String message){
        MessageEvent messageEvent = new MessageEvent();
        messageEvent.setMsgType(MsgType.MSG_SHOW_ALARM_MESSAGE);
        messageEvent.setData(message);
        EventBus.getDefault().post(messageEvent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
        if(procedureFinishEvent.getTemplateName().contains("opendoor")){
            if(procedureFinishEvent.getStatus().equals("finish")){
                hideProgressDialog();
                PcrApplication.rNARunningMessage.setStatus("free");
                finishWait();
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                hideProgressDialog();
                showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_open_check_device),
                        getActivity().getResources().getString(R.string.dialog_message_open_tray_close));
                openDoorError("E11 " + getActivity().getResources().getString(R.string.dialog_message_open_tray_close));
            }
            stopRNARunning();
        }
    }

    /*
     * 超时消息
     * @param sendCmdTimeoutEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {
        stopRNARunning();
        hideProgressDialog();
    }



    private void stopRNARunning(){
        Log.i("RNA", "结束开舱门操作");
        if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
            RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
            RNAAndPCRAsyncTask.getInstance().setmIsrunning(false);
        }
    }

    /**
     * 显示进度
     * @param title
     * @param message
     */
    private void showProgressDialog(String title, String message){
        if(null == mProgressDialog){
            mProgressDialog = ProgressDialogUtil.createLoadingDialog(getActivity(), message);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }
    }

    /*
     * 隐藏提示加载
     */
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }


    private void showNoticeDialog(String title, String message){
        if(null != mNoticeDialog){
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        if(null == mNoticeDialog){
            mNoticeDialog = new WarnDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView)view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);
            WarnDialog.Builder builder = new WarnDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setSingleButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
    }

}
