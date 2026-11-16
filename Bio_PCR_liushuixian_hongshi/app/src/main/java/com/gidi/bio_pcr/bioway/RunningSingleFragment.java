package com.gidi.bio_pcr.bioway;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdTimeoutEvent;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.mgr.AlarmUtils;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomImageText;
import com.gidi.bio_pcr.view.WarnDialog;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RunningSingleFragment extends BaseFragment {
    private CustomImageText mRNATab_Img;
    private CustomImageText mPCRTab_Img;
    private CustomImageText mDoneTab_Img;
    private SlanSingleController singleController;
    private List<File> singleRNAList = new LinkedList<>();
    private List<File> localList = new ArrayList<File>();
    private WarnDialog mNoticeDialog;
    private int curDetectStep = 1;
    private String single_Template_Path;
    private String PCR_RUNNING_TEMPLATE;
    private int PCR_RUNNING_TEMPLATE_PHASE;//第几个PCR文件
    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_slan_single_running;
    }

    @Override
    protected void initView(View rootView) {
        mRNATab_Img = (CustomImageText) rootView.findViewById(R.id.running_single_slan_firstrna_title_tv);
        mPCRTab_Img = (CustomImageText) rootView.findViewById(R.id.running_single_slan_firstpcr_title_tv);
        mDoneTab_Img = (CustomImageText) rootView.findViewById(R.id.running_single_slan_done_title_tv);
        initVariables();
        initLocalFiles();
    }

    @Override
    protected void initListener() {

    }

    private void initVariables(){
        Log.i("Single", "initVariables" );
        singleController = SlanSingleController.getInstance(this, R.id.running_slan_single_item_framelayout);
        singleRNAList = new ArrayList<File>();
        PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");
        PCR_RUNNING_TEMPLATE_PHASE = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getIntValue(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, 0);
        if(PcrApplication.rNARunningMessage == null){
            //正常操作
            //20240429
            if(PcrApplication.experimentType == 1){
                //single RNA
                singleController.showFragment(0);
            }else{
                //single PCR
                singleController.showFragment(1);
            }
            Log.i("Single", "正常运行" + PcrApplication.rootTempName);
        }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")
                && PCR_RUNNING_TEMPLATE.equals("")){
            //正常操作
            //20240429
            if(PcrApplication.experimentType == 1){
                //singleRNA
                singleController.showFragment(0);
            }else{
                //singlePCR
                Log.i("Single", "free--" + "111singlePCR");
                singleController.showFragment(1);
            }
        }else if((PcrApplication.rNARunningMessage.getStatus().contains("running")
                || PcrApplication.rNARunningMessage.getStatus().contains("error")
                || PcrApplication.rNARunningMessage.getStatus().contains("interrupt"))){
//              && PCR_RUNNING_TEMPLATE.equals("")){
            //singleRNA 复位
            String rna_step_num = PcrApplication.rNARunningMessage.getNO();
            if(!StringUtil.isEmpty(rna_step_num)) {
                String[] mRNA_running = rna_step_num.split("\\|",-1);
                String templateNum = mRNA_running[0];
                String mRNA_File = mRNA_running[1];
                String mRNA_Step = mRNA_running[2];
                File templateDir = getRecoveryTemplate(templateNum);
                if(null != templateDir) {
                    PcrApplication.rootTempName = templateDir.getName();
                    Log.i("RNA", "复位" + PcrApplication.rootTempName);
                }
                if(mRNA_File.equals("01")){
                    singleController.showFragment(0);
                }
            }
        }else if(!PCR_RUNNING_TEMPLATE.equals("")) {
            PcrApplication.rootTempName = PCR_RUNNING_TEMPLATE;
            //跳转singlePCR
            singleController.showFragment(1);
        }
    }

    private void initLocalFiles(){
        switch (PcrApplication.experimentType){
            case 1:
                single_Template_Path = FileUtils.SINGLE_RNA_TEMPLATE_PATH +  File.separator
                        + PcrApplication.rootTempName
                        + File.separator ;//找到了模板文件接
                mRNATab_Img.setVisibility(View.VISIBLE);
                mPCRTab_Img.setVisibility(View.GONE);
                mDoneTab_Img.setImageText("2");
                break;

            case 2:
                single_Template_Path = FileUtils.SINGLE_PCR_TEMPLATE_PATH  + File.separator
                        + PcrApplication.rootTempName
                        + File.separator ;//找到了模板文件接
                mRNATab_Img.setVisibility(View.GONE);
                mPCRTab_Img.setVisibility(View.VISIBLE);
                mDoneTab_Img.setVisibility(View.VISIBLE);
                mPCRTab_Img.setImageText("1");
                mDoneTab_Img.setImageText("2");
                break;
            default:
                break;
        }
        File allMoveDir = new File(single_Template_Path);
        if(allMoveDir.isDirectory() && allMoveDir.exists()){
            Collections.addAll(localList, allMoveDir.listFiles());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerEventBus();
        Log.i("Single","onActivityCreated --PCR_RUNNING_TEMPLATE" + PCR_RUNNING_TEMPLATE);


        /////////临时做PCR复检
        if(PcrApplication.experimentType == 2 ){
            //PCR复位
            curDetectStep = 2;
            singleController.showFragment(1);//跳转PCR
            MessageEvent closeDoorMsg = new  MessageEvent();
            closeDoorMsg.setMsgType(MsgType.MSG_END_CLOSE_DOOR);
            closeDoorMsg.setData(true);
            EventBus.getDefault().postSticky(closeDoorMsg);

            MessageEvent finishMsg = new MessageEvent();
            finishMsg.setMsgType(MsgType.MSG_START_SLAN_FIRST_PCR);
            EventBus.getDefault().postSticky(finishMsg);
            onDoneStateChange(1);
            PcrApplication.ex_startTime = DateFormatUtil.getSysFullTime();
            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                    .setValueByName(SharedConstant.START_TIME, DateFormatUtil.getSysFullTime());
            //isPCRRunning
            return;
        }


        if(!PCR_RUNNING_TEMPLATE.equals("")){
            //PCR复位
            recoverySinglePCR();
        }else{
            if((PcrApplication.rNARunningMessage.getStatus().contains("running")
                    || PcrApplication.rNARunningMessage.getStatus().contains("error")
                    || PcrApplication.rNARunningMessage.getStatus().contains("interrupt"))){
                //RNA
                recoverySingleRNA();
            }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")){
                startDetectRNA();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterEventBus();
        stopRNARunning();
        singleController.destoryController();
        singleController = null;
        if(null != localList){
            localList.clear();
            localList = null;
        }

        if(null != singleRNAList){
            singleRNAList.clear();
            singleRNAList = null;
        }

        if(null != mNoticeDialog){
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
    }

    private void startDetectRNA(){
        if(PcrApplication.is_RNA_connect) {
            PcrApplication.ex_startTime = DateFormatUtil.getSysFullTime();
            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                    .setValueByName(SharedConstant.START_TIME, DateFormatUtil.getSysFullTime());
            startRunningSingleRNA(1);
        }
    }

    /**
     * 复位single RNA
     */
    private void recoverySingleRNA() {
        Log.i("Single", "recoverySingleRNA" );
        String rna_step_num = PcrApplication.rNARunningMessage.getNO();
        Log.i("Single", "第一次PCR复位 recoverySingleRNA" + rna_step_num);
        if (!StringUtil.isEmpty(rna_step_num)) {
            String[] mRNA_running = rna_step_num.split("\\|", -1);
            String mTemplateNum = mRNA_running[0];
            String mRNAFileNum = mRNA_running[1];
            String mRNAStep = mRNA_running[2];
            int stepIndex = StringUtil.convertToInt(mRNAStep, 0);
            //tempalte找到模板文件夹名称
            File templateDir = getRecoveryTemplate(mTemplateNum);
            if (null != templateDir) {
                PcrApplication.rootTempName = templateDir.getName();
                single_Template_Path = FileUtils.SINGLE_RNA_TEMPLATE_PATH + File.separator
                        + PcrApplication.rootTempName
                        + File.separator;//找到了模板文件接
                if (mRNAFileNum.equals("01")) {
                    curDetectStep = 1;
                    startRunningSingleRNA(stepIndex);
                }
            }
        }
    }

    /**
     * 复位singlePCR中的PCR文件
     */
    private void recoverySinglePCR(){
        PcrApplication.rootTempName = PCR_RUNNING_TEMPLATE;
        Log.i("Single", "recoverySinglePCR" + PcrApplication.rootTempName);
        if(PCR_RUNNING_TEMPLATE_PHASE == 1){
            //复位第一次PCR
            MessageEvent finishMsg = new MessageEvent();
            finishMsg.setMsgType(MsgType.MSG_START_SLAN_FIRST_PCR);
            EventBus.getDefault().postSticky(finishMsg);
            onDoneStateChange(1);
        }
    }

    private void startRunningSingleRNA(int stepInex){
        Log.i("Single", "开始进行RNA检测" + PcrApplication.ex_startTime);
        singleRNAList.clear();
        mRNATab_Img.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
        if(PcrApplication.experimentType ==1){
            MessageEvent msg = new MessageEvent();
            msg.setMsgType(MsgType.MSG_START_RNA_RUNNING);
            EventBus.getDefault().postSticky(msg);
        }else if(PcrApplication.experimentType == 2){
            //singlePCR不显示核酸，但要进行关门操作
            MessageEvent msg = new MessageEvent();
            msg.setMsgType(MsgType.MSG_START_CLOSE_DOOR);
            EventBus.getDefault().postSticky(msg);
        }
        File allMoveDir = new File(single_Template_Path);
        File[] files = allMoveDir.listFiles();
        for (int i = 0;i<files.length;i++){
            if(files[i].getName().startsWith("first")&& files[i].getName().equals("first.hex")){
                singleRNAList.add(files[i]);
            }
        }
        if(null != singleRNAList && singleRNAList.size()>0){
            List<HexDebugEntity> mHexDebugEntityList = new ArrayList<HexDebugEntity>();



            for (File file : singleRNAList){
                Log.i("Single", "运行的文件名称" + file.getName());
                if(file.isFile()){
                    mHexDebugEntityList.addAll(ListDataSaveUtil.getHexDebugEntityList(file));
                }
            }//先找到RNA
            if(null != mHexDebugEntityList){
                if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
                    Toast.makeText(getActivity().getApplicationContext(), "RNAAndPCRAsyncTask is running, please wait", Toast.LENGTH_LONG).show();
                    return;
                }

                Log.i("Single", "开始进行RNA检测模板" + PcrApplication.rootTempName);
                ////异步全流程
                RNAAndPCRAsyncTask.current_template = PcrApplication.rootTempName;
                RNAAndPCRAsyncTask.current_template_phase = 1;
                RNAAndPCRAsyncTask.current_template_phase_step = stepInex -1;
                //  RNAAndPCRAsyncTask.current_template = PcrApplication.rootTempName +"_"+ PcrApplication.subTempName + "ul";
                RNAAndPCRAsyncTask.getInstance().setmContext(getActivity().getApplicationContext());

                //RNAAndPCRAsyncTask.getInstance().cancel(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    //根据不同的api采用并行模式进行开启
                    //////启动流程的API
                    RNAAndPCRAsyncTask.getInstance().executeOnExecutor(THREAD_POOL_EXECUTOR, mHexDebugEntityList);

                } else {
                    RNAAndPCRAsyncTask.getInstance().execute(mHexDebugEntityList);
                }
            }

        }else{
            //文件不错在
            if(PcrApplication.experimentType ==1){
                MessageEvent RNAFileMsg = new MessageEvent();
                RNAFileMsg.setMsgType(MsgType.MSG_RNA_FILE_NULL);
                RNAFileMsg.setData(1);
                EventBus.getDefault().postSticky(RNAFileMsg);
            }
        }
    }

    private void stopRNARunning(){
        Log.i("Single", "结束RNA检测");
        if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
            RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
            RNAAndPCRAsyncTask.getInstance().setmIsrunning(false);
            RNAAndPCRAsyncTask.clearInstance();
        }
    }


    /*
     * EventBus接收订阅的消息
     * @param messageEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()){
            case MsgType.MSG_RNA_FILE_NULL:
                int file_code = (int)messageEvent.getData();
                if(file_code == 1){
                    stopRNARunning();
                    onDoneErrorStateChange(1);
                    //第一次RNA流程出错，请检查设备
                    showAlarm();
                    showNoticeDialog("请检查设备!","没有第一次RNA流程文件，请检查设备!");
                }
                break;

            case MsgType.MSG_FIRST_SLAN_PCR_DONE:
                Log.i("RNA", "结束第一次PCR检测,跳转" + PcrApplication.isHasSecondRNA);
                onDoneStateChange(2);
                curDetectStep = 3;
                singleController.showFragment(2);
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_INTERRUOT:
            case MsgType.MSG_FIRST_SLAN_PCR_ERROR:
                onDoneErrorStateChange(2);
                break;

            default:
                break;
        }
    }

    /**
     * 接收RNA运行结束
     * @param procedureFinishEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
        //
        switch (curDetectStep){
            case 1:
                //当前的RNA检测，检测完成
                //仅仅做PCR
                if(null != procedureFinishEvent){
                    if(procedureFinishEvent.getStatus().equals("finish")){
                        stopRNARunning();
                        PcrApplication.rNARunningMessage.setStatus("free");
                        if(PcrApplication.experimentType == 1){
                            //singleRNA
                            curDetectStep = 3;//完成rna后跳转完成
                            onDoneStateChange(1);
                            MessageEvent singleRNAMsg = new  MessageEvent();
                            singleRNAMsg.setMsgType(MsgType.MSG_SINGLE_RNA_DONE);
                            singleRNAMsg.setData(true);
                            EventBus.getDefault().postSticky(singleRNAMsg);
                        }else if(PcrApplication.experimentType == 2){
                            //singlePCR
                            curDetectStep = 2;
                            singleController.showFragment(1);//跳转PCR
                            MessageEvent closeDoorMsg = new  MessageEvent();
                            closeDoorMsg.setMsgType(MsgType.MSG_END_CLOSE_DOOR);
                            closeDoorMsg.setData(true);
                            EventBus.getDefault().postSticky(closeDoorMsg);

                            MessageEvent finishMsg = new MessageEvent();
                            finishMsg.setMsgType(MsgType.MSG_START_SLAN_FIRST_PCR);
                            EventBus.getDefault().postSticky(finishMsg);
                            onDoneStateChange(1);
                        }
                    }else if(procedureFinishEvent.getStatus().equals("finish error")){
                        stopRNARunning();
                        onDoneErrorStateChange(1);
                            //第一次RNA流程出错，请检查设备
                        showAlarm();
                        switch (PcrApplication.experimentType){
                            case 1:
                                showNoticeDialog("请检查设备!","RNA流程出错，请检查设备!");
                                break;
                            case 2:
                                MessageEvent closeDoorMsg = new  MessageEvent();
                                closeDoorMsg.setMsgType(MsgType.MSG_END_CLOSE_DOOR);
                                closeDoorMsg.setData(false);
                                EventBus.getDefault().postSticky(closeDoorMsg);
                                break;

                        }

                    }
                }

                break;
        }

    }

    /**
     * 超时消息
     * @param sendCmdTimeoutEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdTimeoutEvent sendCmdTimeoutEvent) {

        Gson gson = new Gson();
        String text = gson.toJson(sendCmdTimeoutEvent.getMsgPacket());
        switch (curDetectStep){
            case 1:
                //当前RNA,完成
                mRNATab_Img.setImageBtnResoure(R.drawable.shape_oval_red_bg,R.color.red);
                stopRNARunning();

                break;

            case 2:
                mPCRTab_Img.setImageBtnResoure(R.drawable.shape_oval_red_bg,R.color.red);
                break;
            default:
                break;

        }

    }

    /**
     * 步骤结束后状态变化
     * @param doneDetectStep 已经完成的步骤
     */
    private void onDoneStateChange(int doneDetectStep){
        switch (doneDetectStep){
            case 1:
                //第一次RNA结束
                switch (PcrApplication.experimentType)
                {
                    case 1:
                        mDoneTab_Img.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                        mRNATab_Img.setNormalState();
                        break;
                    case 2:
                        //完成了关门的核酸步骤，跳转pcr进行中
                        mPCRTab_Img.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                      //  mRNATab_Img.setNormalState();
                        mDoneTab_Img.setNormalState();
                        break;
                }

                break;
            case 2:
                //第一次PCR结束
                mRNATab_Img.setNormalState();
                mPCRTab_Img.setNormalState();
                mDoneTab_Img.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                break;

            default:
                break;
        }
    }

    public void showAlarm(){
        if(!AlarmUtils.getInstance(getActivity().getApplicationContext()).isSystemAlarm()){
            AlarmUtils.getInstance(getActivity().getApplicationContext()).setSystemAlarm(true);
            if(AlarmUtils.getInstance(getActivity().getApplicationContext()).getPlayPosition()!= 3){
                AlarmUtils.getInstance(getActivity().getApplicationContext()).startplay(3);
            }
        }
    }

    public void clearAlarm(){
        if(AlarmUtils.getInstance(getActivity().getApplicationContext()).isSystemAlarm()){
            AlarmUtils.getInstance(getActivity().getApplicationContext()).setSystemAlarm(false);
            AlarmUtils.getInstance(getActivity().getApplicationContext()).stopPlay();
        }
    }


    /**
     * 显示警告对话框
     */
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
                    clearAlarm();
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
    }


    private void onDoneErrorStateChange(int curDetectStep) {
        switch (curDetectStep) {
            case 1:
                //第一次RNA结束
                mRNATab_Img.setImageBtnResoure(R.drawable.shape_oval_red_bg, R.color.red);
                mPCRTab_Img.setNormalState();
                mDoneTab_Img.setImageBtnResoure(R.drawable.shape_oval_red_bg, R.color.red);
                break;
            case 2:
                //第一次PCR
                mRNATab_Img.setNormalState();
                mPCRTab_Img.setImageBtnResoure(R.drawable.shape_oval_red_bg, R.color.red);
                mDoneTab_Img.setImageBtnResoure(R.drawable.shape_oval_red_bg, R.color.red);
                break;
        }
    }

    private File getRecoveryTemplate(String template_num){
        String templatePath = FileUtils.SINGLE_RNA_TEMPLATE_PATH + File.separator;
        File templateRootDir = new File(templatePath);
        if(templateRootDir.exists()&& templateRootDir.isDirectory()){
            File[] rootFiles = templateRootDir.listFiles();
            if(null != rootFiles && rootFiles.length>0){
                for(File file : rootFiles){
                    if(file.getName().startsWith(template_num)) {
                        return file;
                    }
                }

            }
        }
        return null;
    }
}
