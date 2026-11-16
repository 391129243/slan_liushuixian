package com.gidi.bio_pcr.user.running;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

import android.app.Activity;
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
import com.gidi.bio_pcr.user.activity.UserBiowayActivity;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
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

public  class UserBiowayFragment extends BaseFragment {

    private UserController userController;

    private int curDetectStep = 1;
    private String fileDirPath;
    private List<File> localList = new ArrayList<File>();
    private List<File>firstRNAList = new LinkedList<>();
    private List<File>secondRNAList = new LinkedList<>();

    private WarnDialog mNoticeDialog;

    private String PCR_RUNNING_TEMPLATE;
    private int PCR_RUNNING_TEMPLATE_PHASE;

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_user_bioway_running;
    }

    @Override
    protected void initView(View rootView) {
        initVariables();
        initLocalFiles();
    }

    /**
     * 1.复位：判断是RNA复位还是PCR复位
     * 2.
     */
    private void initVariables(){
        userController = UserController.getInstance(this, R.id.user_running_bioway_item_framelayout);
        //复位操作
        PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");
        PCR_RUNNING_TEMPLATE_PHASE = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getIntValue(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, 0);

        if(PcrApplication.rNARunningMessage == null){
            //正常操作
            userController.showFragment(0);
            Log.i("SLAN","正常操作 ");
        }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")&& PCR_RUNNING_TEMPLATE.equals("")){
            //正常操作
            userController.showFragment(0);
            Log.i("SLAN","正常操作 ");
        }else if((PcrApplication.rNARunningMessage.getStatus().contains("running")
                || PcrApplication.rNARunningMessage.getStatus().contains("error")
                || PcrApplication.rNARunningMessage.getStatus().contains("interrupt"))){
//                && PCR_RUNNING_TEMPLATE.equals("")){
            //RNA
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
                    userController.showFragment(0);
                }else if(mRNA_File.equals("02")){
                    userController.showFragment(0);
                }
            }
            Log.i("Bioway","核酸提取 ");
        }else if(!PCR_RUNNING_TEMPLATE.equals("")) {
            PcrApplication.rootTempName = PCR_RUNNING_TEMPLATE;
            if (PCR_RUNNING_TEMPLATE_PHASE == 1) {
                userController.showFragment(0);
                Log.i("Bioway","PCR扩增 ");
            } else if (PCR_RUNNING_TEMPLATE_PHASE == 2) {
                userController.showFragment(0);
                Log.i("Bioway","PCR扩增&熔解 ");
            } else {
                userController.showFragment(0);
            }

        }

    }


    @Override
    protected void initListener() {

    }

    /**
     * 1.所有运行文件的目录
     *
     *
     */
    private void initLocalFiles(){
        fileDirPath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName
                + File.separator ;//找到了模板文件接
        File allMoveDir = new File(fileDirPath);
        if(allMoveDir.isDirectory() && allMoveDir.exists()){
            Collections.addAll(localList, allMoveDir.listFiles());
        }
        PcrApplication.isHasSecondRNA = isHasSecondRNA();

    }

    private boolean isHasSecondRNA(){
        for(File file:localList){
            if(file.getName().toLowerCase().startsWith("second")&& file.getName().toLowerCase().contains("prj")){
                return true;
            }
        }
        return false;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerEventBus();
        PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");
        PCR_RUNNING_TEMPLATE_PHASE = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getIntValue(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, 0);

        if((PcrApplication.rNARunningMessage.getStatus().contains("running")
                || PcrApplication.rNARunningMessage.getStatus().contains("error")
                || PcrApplication.rNARunningMessage.getStatus().contains("interrupt"))){
            //RNA
            recoveryStartRNA();
        }else if(!PCR_RUNNING_TEMPLATE.equals("")){
            recoveryStartPCR();
        }else if(PcrApplication.rNARunningMessage.getStatus().contains("free")){
            startDetectRNA();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterEventBus();
        stopRNARunning();
        userController.destoryController();
        userController = null;
        if(null != localList){
            localList.clear();
            localList = null;
        }

        if(null != firstRNAList){
            firstRNAList.clear();
            firstRNAList = null;
        }
        if(null != secondRNAList){
            secondRNAList.clear();
            secondRNAList = null;
        }
        if(null != mNoticeDialog){
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        userBiowayActivity = null;
    }


    private void startDetectRNA(){
        if(PcrApplication.is_RNA_connect){
            PcrApplication.ex_startTime = DateFormatUtil.getSysFullTime();
            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                    .setValueByName(SharedConstant.START_TIME, DateFormatUtil.getSysFullTime());
            startRunningFirstRNA(1,false);
        }else{
            // ToastUtils.showToast(getActivity().getApplicationContext(), "RNA串口断连");
        }
    }


    /**
     * 复位继续RNA
     */
    private void recoveryStartRNA(){
        String rna_step_num = PcrApplication.rNARunningMessage.getNO();
        Log.i("RNA", "第一次RNA复位 recoveryStartRNA" + rna_step_num);
        if(!StringUtil.isEmpty(rna_step_num)) {
            String[] mRNA_running = rna_step_num.split("\\|",-1);
            String mTemplateNum = mRNA_running[0];
            String mRNAFileNum = mRNA_running[1];
            String mRNAStep = mRNA_running[2];
            int stepIndex = StringUtil.convertToInt(mRNAStep,0);
            //tempalte找到模板文件夹名称
            File templateDir = getRecoveryTemplate(mTemplateNum);
            Log.e("UserRecoveryActivity", "RNA复位 recoveryStartRNA" + templateDir.getName());
            if(null != templateDir){
                PcrApplication.rootTempName = templateDir.getName();
                fileDirPath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + PcrApplication.rootTempName
                        + File.separator ;//找到了模板文件接
                if(mRNAFileNum.equals("01")){
                    curDetectStep = 1;
                    startRunningFirstRNA(stepIndex,true);
                }else if(mRNAFileNum.equals("02")){
                    curDetectStep = 3;
                    onDoneStateChange(2);
                    startRunningSecondRNA(stepIndex,true);
                }
            }
        }
    }

    /**
     * PCR复位
     */
    private void recoveryStartPCR(){

        PcrApplication.rootTempName = PCR_RUNNING_TEMPLATE;
        if(PCR_RUNNING_TEMPLATE_PHASE == 1){
            //复位第一次PCR
            Log.i("RNA", "第一次PCR复位 recoveryStartPCR" + PcrApplication.rootTempName);
            userController.showFragment(0);//跳转PCR
            MessageEvent finishMsg = new MessageEvent();
            finishMsg.setMsgType(MsgType.MSG_START_SLAN_FIRST_PCR);
            EventBus.getDefault().postSticky(finishMsg);
            onDoneStateChange(1);
        }else if(PCR_RUNNING_TEMPLATE_PHASE == 2){
            //复位第二次PCR
            Log.i("RNA", "第二次PCR复位 recoveryStartPCR" + PcrApplication.rootTempName );
            userController.showFragment(0);//跳转第二次PCR
            MessageEvent finishMsg = new MessageEvent();
            finishMsg.setMsgType(MsgType.MSG_START_SLAN_SECOND_PCR);
            EventBus.getDefault().postSticky(finishMsg);
            onDoneStateChange(3);
        }
    }

    private File getRecoveryTemplate(String template_num){
        String templatePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator;
        String templateName = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.RUNNING_TEMPLATE,"");
        File templateRootDir = new File(templatePath);
        if(templateRootDir.exists()&& templateRootDir.isDirectory()){
            File[] rootFiles = templateRootDir.listFiles();
            if(null != rootFiles && rootFiles.length>0){
                for(File file : rootFiles){
                    if(file.getName().startsWith(template_num)
                        && file.getName().equals(templateName)) {
                        return file;
                    }
                }

            }
        }
        return null;
    }


    /**
     * 开始进行第一次RNA检测
     * 1.检测进度变化第一次RNA变成进行中状态
     * 2.发送开始第一次RNA运行的消息，第一个RNA接收到开始计时
     * 3.从文本文件夹中找出需要运行的模板
     */
    private void startRunningFirstRNA(int stepInex,boolean recovery){

        Log.i("RNA", "开始进行第一次RNA检测" + PcrApplication.ex_startTime);
        Log.e("UserBiowayActivity", "UserBiowayFragment 开始进行第一次RNA检测 recoveryStartRNA"  + recovery);
        firstRNAList.clear();
        MessageEvent msg = new MessageEvent();
        msg.setMsgType(MsgType.MSG_START_RNA_RUNNING);
        msg.setData(recovery);
        EventBus.getDefault().postSticky(msg);
        File allMoveDir = new File(fileDirPath);
        File[] files = allMoveDir.listFiles();
        for (int i = 0;i<files.length;i++){
            if(files[i].getName().toLowerCase().startsWith("first")
                    && files[i].getName().toLowerCase().equals("first.hex")){
                firstRNAList.add(files[i]);
            }
        }
        if(null != firstRNAList && firstRNAList.size()>0){
            List<HexDebugEntity> mHexDebugEntityList = new ArrayList<HexDebugEntity>();
            for (File file : firstRNAList){
                Log.i("RNA", "运行的文件名称" + file.getName());
                if(file.isFile()){
                    mHexDebugEntityList.addAll(ListDataSaveUtil.getHexDebugEntityList(file));
                }
            }//先找到RNA
            if(null != mHexDebugEntityList){
                if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
                    Toast.makeText(getActivity().getApplicationContext(), "RNAAndPCRAsyncTask is running, please wait", Toast.LENGTH_LONG).show();
                    return;
                }
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
            MessageEvent RNAFileMsg = new MessageEvent();
            RNAFileMsg.setMsgType(MsgType.MSG_RNA_FILE_NULL);
            RNAFileMsg.setData(1);
            EventBus.getDefault().postSticky(RNAFileMsg);
        }

    }

    /**
     * 开始进行第二次RNA检测
     * 1.检测进度变化
     */
    private void startRunningSecondRNA(int stepIndex,boolean recovery){
        Log.i("RNA", "开始进行第二次RNA检测");
        Log.i("UserRecoveryActivity", "UserBiowayFragment 开始进行第二次RNA检测 --recovery" + recovery);
        secondRNAList.clear();
        MessageEvent msg = new MessageEvent();
        msg.setMsgType(MsgType.MSG_START_SECOND_RNA_RUNNING);
        msg.setData(recovery);
        EventBus.getDefault().postSticky(msg);
        File allMoveDir = new File(fileDirPath);
        File[] files = allMoveDir.listFiles();
        for (int i = 0;i<files.length;i++){
            if(files[i].getName().toLowerCase().startsWith("second")&& files[i].getName().toLowerCase().equals("second.hex")){
                secondRNAList.add(files[i]);
            }

        }
        if(null != secondRNAList && secondRNAList.size()>0){
            List<HexDebugEntity> mHexDebugEntityList = new ArrayList<HexDebugEntity>();
            for (File file : secondRNAList){
                Log.i("RNA", "第二次RNA运行的文件名称" + file.getName());
                if(file.isFile()){
                    mHexDebugEntityList.addAll(ListDataSaveUtil.getHexDebugEntityList(file));
                }

            }//先找到RNA
            if(null != mHexDebugEntityList){
                if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
                    Toast.makeText(getActivity().getApplicationContext(), "RNAAndPCRAsyncTask is running, please wait", Toast.LENGTH_LONG).show();
                    return;
                }
                ////异步全流程
                // RNAAndPCRAsyncTask.current_template = PcrApplication.rootTempName +"_"+ PcrApplication.subTempName + "ul";
                RNAAndPCRAsyncTask.current_template = PcrApplication.rootTempName;
                RNAAndPCRAsyncTask.current_template_phase = 2;
                RNAAndPCRAsyncTask.current_template_phase_step = stepIndex - 1;
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
            MessageEvent RNAFileMsg = new MessageEvent();
            RNAFileMsg.setMsgType(MsgType.MSG_RNA_FILE_NULL);
            RNAFileMsg.setData(2);
            EventBus.getDefault().postSticky(RNAFileMsg);
        }

    }


    private void stopRNARunning(){
        Log.i("RNA", "结束RNA检测");
        if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
            RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
            RNAAndPCRAsyncTask.getInstance().setmIsrunning(false);
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
                    showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_check_equipment),
                            getActivity().getResources().getString(R.string.running_fragment_dialog_message_first_rna_file_null));
                }else if(file_code == 2){
                    stopRNARunning();
                    onDoneErrorStateChange(3);
                    //第一次RNA流程出错，请检查设备
                    showAlarm();
                    showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_check_equipment),
                            getActivity().getResources().getString(R.string.running_fragment_dialog_message_second_rna_file_null));
                }
                break;

            case MsgType.MSG_FIRST_SLAN_PCR_DONE:
                Log.i("RNA", "结束第一次PCR检测,跳转" + PcrApplication.isHasSecondRNA);
                if(PcrApplication.isHasSecondRNA){
                    //如果有第二次PCR
                    onDoneStateChange(2);
                    curDetectStep = 3;
                    if(userBiowayActivity.isActivityPause){
                        userController.showFragment1(0);
                    }else{
                        userController.showFragment(0);
                    }
                    startRunningSecondRNA(1,false);
                }else{
                    onDoneStateChange(4);
                    curDetectStep = 5;
                    if(userBiowayActivity.isActivityPause){
                        userController.showFragment1(1);
                    }else{
                        userController.showFragment(1);//结果
                    }

                }
                break;
            case MsgType.MSG_FIRST_SLAN_PCR_INTERRUOT:
            case MsgType.MSG_FIRST_SLAN_PCR_ERROR:
                onDoneErrorStateChange(2);
                break;
            case MsgType.MSG_SECOND_SLAN_PCR_DONE:
                //运行结束PCR运行
                onDoneStateChange(4);
                curDetectStep = 5;
                if(userBiowayActivity.isActivityPause){
                    userController.showFragment1(1);
                }else{
                    userController.showFragment(1);//跳转结果
                }

                break;
            case MsgType.MSG_SECOND_SLAN_PCR_INTERRUOT:
            case MsgType.MSG_SECOND_SLAN_PCR_ERROR:
                onDoneErrorStateChange(4);
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
                //第一次RNA,完成
                curDetectStep = 2;
                if(null != procedureFinishEvent){
                    if(procedureFinishEvent.getStatus().equals("finish")){
                        stopRNARunning();
                        if(userBiowayActivity.isActivityPause){
                            userController.showFragment1(0);//跳转PCR
                        }else {
                            userController.showFragment(0);//跳转PCR
                        }

                        MessageEvent finishMsg = new MessageEvent();
                        finishMsg.setMsgType(MsgType.MSG_START_SLAN_FIRST_PCR);
                        EventBus.getDefault().postSticky(finishMsg);
                        onDoneStateChange(1);
                        PcrApplication.rNARunningMessage.setStatus("free");
                    }else if(procedureFinishEvent.getStatus().equals("finish error")){
                        stopRNARunning();
                        onDoneErrorStateChange(1);
                        //第一次RNA流程出错，请检查设备
                        showAlarm();
                        showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_check_equipment)
                                ,getActivity().getResources().getString(R.string.running_fragment_dialog_message_first_rna_error));

                    }
                }

                break;


            case 3:
                //当前第二步RNA,完成
                onDoneStateChange(3);//第三步时第二次RNA
                curDetectStep = 4;
                if(null != procedureFinishEvent){
                    if(procedureFinishEvent.getStatus().equals("finish")){
                        stopRNARunning();//结束第二次RNA
                        if(userBiowayActivity.isActivityPause){
                            userController.showFragment1(0);//跳转PCR
                        }else {
                            userController.showFragment(0);//跳转PCR
                        }
                       // userController.showFragment(0);//跳转第二次PCR
                        MessageEvent finishMsg = new MessageEvent();
                        finishMsg.setMsgType(MsgType.MSG_START_SLAN_SECOND_PCR);
                        EventBus.getDefault().postSticky(finishMsg);
                        onDoneStateChange(3);
                    }else if(procedureFinishEvent.getStatus().equals("finish error")){
                        stopRNARunning();
                        //第二次RNA流程出错
                        onDoneErrorStateChange(3);
                        showAlarm();
                        showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_check_equipment),
                                getActivity().getResources().getString(R.string.running_fragment_dialog_message_second_rna_error));
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
                stopRNARunning();
                break;

            case 2:
                break;
            case 3:
                //当前RNA,完成
                stopRNARunning();
                break;

            case 4:
                break;
            default:
                break;

        }

    }

    /**
     * 步骤结束后状态变化
     * @param curDetectStep
     */
    private void onDoneStateChange(int curDetectStep){
        switch (curDetectStep){
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
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

                break;
            case 2:

                break;
            case 3:
                break;
            case 4:

                break;
        }
    }

    private UserBiowayActivity userBiowayActivity;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        userBiowayActivity = (UserBiowayActivity) activity;
    }
}
