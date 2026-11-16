package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.utils.AssetUtils;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.databinding.FragmentSelfcheckBinding;
import com.gidi.bio_pcr.mgr.AlarmUtils;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.WarnDialog;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.adapter.ConnectListAdapter;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.fragment.viewmodel.SelfcheckViewModel;
import com.paul.pipeline.listener.ArmFinishCallback;
import com.paul.pipeline.listener.ArmResetCallback;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.RNAManager;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.model.ConnectModel;
import com.paul.pipeline.fragment.model.PipeArmModel;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.fragment.model.PipeRNAModel;
import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_QRCode;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.event.WSSMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRFinishMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAFinishMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class SelfcheckFragment extends Fragment {
    final static String TAG = "SelfcheckFragment";
    private SelfcheckViewModel mViewModel;

    public static SelfcheckFragment newInstance() {
        return new SelfcheckFragment();
    }

    FragmentSelfcheckBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.i(TAG,"onCreateView");
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_selfcheck, container, false);
        initBinding();
        return binding.getRoot();
     //   return inflater.inflate(R.layout.fragment_selfcheck, container, false);
    }

    PipeMainViewModel mainviewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(SelfcheckViewModel.class);
        mainviewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        initRNAListCycleView();
        initPCRListCycleView();
        initTrayList();
    }
    //PipeMainActivity mainActivity;
//    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
    ////        if(activity instanceof AppCompatActivity) {
////            mainActivity = (PipeMainActivity) activity;
////        }
//        initData();
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    public void onResume() {
        super.onResume();
        startReturntray = false;
        autoreturntray = false;
        autoreset = false;
        autoclearall = false;
        APLManager.getInstance().SelfCheck = false;
        registerEventBus();
        refreshSourceData();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (hidden) {
            APLManager.getInstance().SelfCheck = true;
            //System.out.println("界面不可见");
            unRegisterEventBus();
        }else{
            APLManager.getInstance().SelfCheck = false;
            registerEventBus();
            refreshSourceData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unRegisterEventBus();
    }

    private void initBinding() {
        binding.untouchview.setOnClickListener(v -> {
            //不实现，禁止外围区域触摸
        });
        binding.skipBtn.setOnClickListener(v->{
            skipselfcheck();
        });
        binding.restartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                APLManager.getInstance().stopWSSserver();
                APLManager.getInstance().startWSSserver();
                showText("服务器启动中！");
            }
        });
        binding.llArm1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openArmDetail(1);
            }
        });
        binding.llQrscaner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openQRDetail(1);
            }
        });
        binding.autoresetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoResetAll();
            }
        });
        binding.autobacktrayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoReturnAll();
            }
        });
        binding.autoclearallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllData();
            }
        });
        binding.llDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }
    private void showToast(String text){
        Toast.makeText(getContext(),text, Toast.LENGTH_LONG).show();
    }
    private void sendMessage(int index,int text){
        if(mUIHandler!=null){
            Message msg = mUIHandler.obtainMessage();
            msg.what = index;
            msg.obj = text;
            mUIHandler.sendMessage(msg);
        }
    }
    private void sendArmAlert(String text){
        if(mUIHandler!=null){
            Message msg = mUIHandler.obtainMessage();
            msg.what = MSG_ALERT_ARM;
            msg.obj = text;
            mUIHandler.sendMessage(msg);
        }
    }
    private void showText(String text){
       if(mUIHandler!=null){
           Message msg = mUIHandler.obtainMessage();
           msg.what = MSG_TEXT_SHOW;
           msg.obj = text;
           mUIHandler.sendMessage(msg);
       }
    }
    private void showTrayText(String text){
        if(mUIHandler!=null){
            Message msg = mUIHandler.obtainMessage();
            msg.what = MSG_TEXT_SHOW_TRAY;
            msg.obj = text;
            mUIHandler.sendMessage(msg);
        }
    }
    private void appendText(String text){
        if(mUIHandler!=null){
            Message msg = mUIHandler.obtainMessage();
            msg.what = MSG_TEXT_APPEND;
            msg.obj = text;
            mUIHandler.sendMessage(msg);
        }
    }
    boolean FLAG_REFRESH = true;
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            refreshExistInfo();
            refreshConnectInfo();
            refreshAllTray();
            // 状态已实时刷新，再驱动还盘会话：每 ~3s 推进一步
            if(autoReturnSession){
                returnTickCounter++;
                if(returnTickCounter >= RETURN_TICK_STEP){
                    returnTickCounter = 0;
                    driveAutoReturn();
                }
            }
            if(FLAG_REFRESH) {
                mUIHandler.postDelayed(runnable, 1000);
            }
        }
    };
    private void autoSelfCheck() {
        FLAG_REFRESH = true;
        mUIHandler.postDelayed(runnable, 1000);
      //  mUIHandler.sendEmptyMessageDelayed(HEARTCHECK,5000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FLAG_REFRESH = false;
        autoReturnSession = false;   // 离开页面停止还盘会话，避免后台空跑
        mNoticeDialog = null;
        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        runnable= null;
    }

    UIHandler mUIHandler;
    protected void initData() {
        skip = false;
        FileUtils.COMMON_HEX_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/commonHex_v5/";
        mUIHandler = new UIHandler();
        autoSelfCheck();
//        boolean init  = PreferenceUtil.getInstance(getContext())
//                .getBooleanValue(SharedConstant.SYSTEM_INIT,false);
//        if(!init) {
//            if(null != mUIHandler){
//                mUIHandler.sendEmptyMessage(MSG_START_INIT);
//            }
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        //////需要加初始化的dialog
//                        AssetUtils.copyAssets("commonHex_v5", getContext(), true);
//                        AssetUtils.copyAssets("hexFile_v5", getContext(), true);
//        AssetUtils.copyAssets("hexFile_v5_quick", getContext(), true);
//                        AssetUtils.copyAssets("file", getContext(), true);
//                        PreferenceUtil.getInstance(getContext())
//                                .setValueByName(SharedConstant.SYSTEM_INIT,true);
//                        if(null != mUIHandler){
//                            mUIHandler.sendEmptyMessage(MSG_END_INIT);
//                        }
//                    } catch (Exception e) {
//                        Log.e(TAG, e.getMessage());
//                        //throw new RuntimeException(e);
//                        if(null != mUIHandler){
//                            mUIHandler.sendEmptyMessage(MSG_END_INIT);
//                        }
//                    }
//                }
//            }).start();
//        }
    }
    public void clearAlarm(){
        if(AlarmUtils.getInstance(getContext()).isSystemAlarm()){
            AlarmUtils.getInstance(getContext()).setSystemAlarm(false);
            AlarmUtils.getInstance(getContext()).stopPlay();
        }
    }
    private boolean skip = false;
    private void skipselfcheck(){
        clearAlarm();
        skip = true;
        APLManager.getInstance().SelfCheck = true;
        selfCheckSuccess();
    }
    public WarnDialog mNoticeDialog;
    public void showNoticeDialog(String title, String message){
        if(null != mNoticeDialog){
            if(mNoticeDialog.isShowing()){
                return;
            }else{
                mNoticeDialog = null;
            }
        }
        if(null == mNoticeDialog){
            mNoticeDialog = new WarnDialog(getContext());
            LayoutInflater mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView)view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);
            WarnDialog.Builder builder = new WarnDialog.Builder(getContext());
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setSingleButton(R.string.dialog_skipselfcheck, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mNoticeDialog.dismiss();

                    skipselfcheck();
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
    }
    ArrayList<ConnectModel> rnalist;
    ConnectListAdapter rnaListAdapter;
    private void initRNAListCycleView(){
        rnalist = new ArrayList<>();
        ConnectModel model1 = new ConnectModel();
        model1.index.set(1);
        model1.type.set(ConstantData.nameRNA);
        model1.name.set(getString(R.string.rna1));
        model1.source.set("无");
        rnalist.add(model1);
        ConnectModel model2 = new ConnectModel();
        model2.name.set(getString(R.string.rna2));
        model2.index.set(2);
        model2.type.set(ConstantData.nameRNA);
        model2.source.set("无");
        rnalist.add(model2);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false);
        //1.设置LinearLayoutManager ListView
        binding.rnaRv.setLayoutManager(layoutManager);
        binding.rnaRv.setHasFixedSize(true);
        //3.设置Adapter
        rnaListAdapter = new ConnectListAdapter(getContext(), rnalist, (view, position) -> openRNADetail(position));
        binding.rnaRv.setAdapter(rnaListAdapter);
        rnaListAdapter.notifyDataSetChanged();
    }
    ArrayList<ConnectModel> pcrlist;
    ConnectListAdapter pcrListAdapter;
    private void initPCRListCycleView(){
        pcrlist = new ArrayList<>();
        ConnectModel model1 = new ConnectModel();
        model1.index.set(1);
        model1.type.set(ConstantData.namePCR);
        model1.name.set(getString(R.string.pcr1));
        model1.source.set("无");
        pcrlist.add(model1);
        ConnectModel model2 = new ConnectModel();
        model2.name.set(getString(R.string.pcr2));
        model2.index.set(2);
        model2.type.set(ConstantData.namePCR);
        model2.source.set("无");
        pcrlist.add(model2);
        ConnectModel model3 = new ConnectModel();
        model3.index.set(3);
        model3.type.set(ConstantData.namePCR);
        model3.name.set(getString(R.string.pcr3));
        model3.source.set("无");
        pcrlist.add(model3);
        ConnectModel model4 = new ConnectModel();
        model4.name.set(getString(R.string.pcr4));
        model4.index.set(4);
        model4.type.set(ConstantData.namePCR);
        model4.source.set("无");
        pcrlist.add(model4);
        ConnectModel model5 = new ConnectModel();
        model5.index.set(5);
        model5.type.set(ConstantData.namePCR);
        model5.name.set(getString(R.string.pcr5));
        model5.source.set("无");
        pcrlist.add(model5);
        ConnectModel model6 = new ConnectModel();
        model6.name.set(getString(R.string.pcr6));
        model6.index.set(6);
        model6.type.set(ConstantData.namePCR);
        model6.source.set("无");
        pcrlist.add(model6);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL,false);
        //1.设置LinearLayoutManager ListView
        binding.pcrRv.setLayoutManager(layoutManager);
        binding.pcrRv.setHasFixedSize(true);
        //3.设置Adapter
        pcrListAdapter = new ConnectListAdapter(getContext(), pcrlist, (view, position) -> openPCRDetail(position));
        binding.pcrRv.setAdapter(pcrListAdapter);
        pcrListAdapter.notifyDataSetChanged();
    }
    ArrayList<ConnectModel> traylist = new ArrayList<>();
    private void initTrayList() {
        traylist = new ArrayList<>();
        ConnectModel model1 = new ConnectModel();
        model1.index.set(1);
        model1.type.set(ConstantData.nameTray);
        model1.name.set(getString(R.string.pan1));
        model1.source.set("无");
        traylist.add(model1);
        ConnectModel model2 = new ConnectModel();
        model2.name.set(getString(R.string.pan2));
        model2.index.set(2);
        model2.type.set(ConstantData.nameTray);
        model2.source.set("无");
        traylist.add(model2);
        ConnectModel model3 = new ConnectModel();
        model3.index.set(3);
        model3.type.set(ConstantData.nameTray);
        model3.name.set(getString(R.string.pan3));
        model3.source.set("无");
        traylist.add(model3);
        ConnectModel model4 = new ConnectModel();
        model4.name.set(getString(R.string.pan4));
        model4.index.set(4);
        model4.type.set(ConstantData.nameTray);
        model4.source.set("无");
        traylist.add(model4);
        ConnectModel model5 = new ConnectModel();
        model5.index.set(5);
        model5.type.set(ConstantData.nameTray);
        model5.name.set(getString(R.string.pan5));
        model5.source.set("无");
        traylist.add(model5);
        ConnectModel model6 = new ConnectModel();
        model6.name.set(getString(R.string.pan6));
        model6.index.set(6);
        model6.type.set(ConstantData.nameTray);
        model6.source.set("无");
        traylist.add(model6);
        ConnectModel model7 = new ConnectModel();
        model7.name.set(getString(R.string.pan7));
        model7.index.set(7);
        model7.type.set(ConstantData.nameTray);
        model7.source.set("无");
        traylist.add(model7);
        ConnectModel model8 = new ConnectModel();
        model8.name.set(getString(R.string.pan8));
        model8.index.set(8);
        model8.type.set(ConstantData.nameTray);
        model8.source.set("无");
        traylist.add(model8);
    }
    private void openRNADetail(int position) {
        String text = "是否确定复位 核提"+position+" ?";
        showErrorDialog(text,ConstantData.DEVICE_RNA,position);
    }
    private void openPCRDetail(int position) {
        String text = "是否确定复位 PCR"+position+"  ?";
        showErrorDialog(text,ConstantData.DEVICE_PCR,position);
    }
    private void openArmDetail(int position) {
        String text = "是否确定复位 机械臂 ?";
        showErrorDialog(text,ConstantData.DEVICE_ARM,position);
    }
    private void openQRDetail(int position) {
        String text = "是否确定重连 扫码枪 ?";
        showErrorDialog(text,ConstantData.DEVICE_QRSCANER,position);
    }

    private void openDoor(int position) {
        String text = "是否锁门 ?";
        showErrorDialog(text,ConstantData.DEVICE_DOOR,position);
    }

    private NoticeDialog mErrorDialog;
    public void showErrorDialog(String message,int type, int index ) {
        if(null != mErrorDialog){
            mErrorDialog.dismiss();
            mErrorDialog =null;
        }
        if(null == mErrorDialog){
            mErrorDialog = new NoticeDialog(getContext(),R.style.Dialog);
            mErrorDialog.setMessage(message);
            mErrorDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    mErrorDialog.dismiss();
                    resetError(type,index);
                }
            });

            mErrorDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                @Override
                public void onCancelClick() {
                    mErrorDialog.dismiss();
                }
            });
            mErrorDialog.show();
        }
    }

    private void resetError(int type, int index) {
        switch(type){
            case ConstantData.DEVICE_ARM:
                boolean armresult = ArmManager.getInstance().robotReset(new ArmResetCallback() {
                    @Override
                    public void finish(boolean result, String message) {
                        if(result) {
                            sendArmAlert("复位完成");
                        }else{
                            sendArmAlert("复位失败");
                        }
                    }
                });
                if(!armresult) {
                    sendArmAlert("复位发送失败");
                }else{
                    sendArmAlert("复位中");
                }
                break;
            case ConstantData.DEVICE_RNA:
                boolean rnaresult = RNAManager.getInstance().reset(index);
                if(!rnaresult) {
                    updateRnaAlert(index,"复位发送失败");
                }else{
                    updateRnaAlert(index,"复位中");
                }
                break;
            case ConstantData.DEVICE_PCR:
                boolean pcrresult = PCRManager.getInstance().reset(index);
                if(!pcrresult) {
                    updatePcrAlert(index,"复位发送失败");
                }else{
                    updatePcrAlert(index,"复位中");
                }
                break;
            case ConstantData.DEVICE_QRSCANER:
                showText("扫码枪 重连中");
                boolean QRresult = SerialPortManager_QRCode.getInstance().sendStart();
                if(!QRresult) {
                    showText("扫码枪 重连失败");
                }
                break;
            default:
                showText("");
                break;
        }
    }

    public void updateTrayExist(int index,boolean value){
        for(int i=0;i<traylist.size();i++){
            ConnectModel model = traylist.get(i);
            if(model.index.get()==index){
                model.exist.set(value);
            }
        }
    }
    public void updatePcrCover(int index,boolean value){
        for(int i=0;i<pcrlist.size();i++){
            ConnectModel model = pcrlist.get(i);
            if(model.index.get()==index){
                model.cover.set(value);
            }
        }
        if(pcrListAdapter!=null) {
            pcrListAdapter.notifyDataSetChanged();
        }
    }
    public void updatePcrExist(int index,boolean value){
        for(int i=0;i<pcrlist.size();i++){
            ConnectModel model = pcrlist.get(i);
            if(model.index.get()==index){
                model.exist.set(value);
            }
        }
        if(pcrListAdapter!=null) {
            pcrListAdapter.notifyDataSetChanged();
        }
    }
    public void updateRnaExist(int index,boolean value){
        for(int i=0;i<rnalist.size();i++){
            ConnectModel model = rnalist.get(i);
            if(model.index.get()==index){
                model.exist.set(value);
            }
        }
        if(rnaListAdapter!=null) {
            rnaListAdapter.notifyDataSetChanged();
        }
    }
    public void updatePcrAlert(int index,String value){
        for(int i=0;i<pcrlist.size();i++){
            ConnectModel model = pcrlist.get(i);
            if(model.index.get()==index){
                model.alert.set(value);
            }
        }
    }
    public void updateRnaAlert(int index,String value){
        for(int i=0;i<rnalist.size();i++){
            ConnectModel model = rnalist.get(i);
            if(model.index.get()==index){
                model.alert.set(value);
            }
        }
    }
    public void updateArmConnect(int index,boolean value){
        if(index==1) {
            if (value) {
                binding.ARMConnectImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
            } else {
                binding.ARMConnectImg.setStateBackgroud(R.drawable.shape_oval_red_bg);
            }
        }
    }
    public void updatePcrSource(int index,String value){
        for(int i=0;i<pcrlist.size();i++){
            ConnectModel model = pcrlist.get(i);
            if(model.index.get()==index){
                model.source.set(value);
            }
        }
        if(pcrListAdapter!=null) {
            pcrListAdapter.notifyDataSetChanged();
        }
    }
    public void updateRnaSource(int index,String value){
        for(int i=0;i<rnalist.size();i++){
            ConnectModel model = rnalist.get(i);
            if(model.index.get()==index){
                model.source.set(value);
            }
        }
        if(rnaListAdapter!=null) {
            rnaListAdapter.notifyDataSetChanged();
        }
    }
    public void updatePcrConnect(int index,boolean connect,String status,String linuxstatus){
        for(int i=0;i<pcrlist.size();i++){
            ConnectModel model = pcrlist.get(i);
            if(model.index.get()==index){
                model.connect.set(connect);
                model.status.set(status);
                model.linuxstatus.set(linuxstatus);
            }
        }
        if(pcrListAdapter!=null) {
            pcrListAdapter.notifyDataSetChanged();
        }
    }
    public void updateRnaConnect(int index,boolean connect,String status,String linuxstatus){
        for(int i=0;i<rnalist.size();i++){
            ConnectModel model = rnalist.get(i);
            if(model.index.get()==index){
                model.connect.set(connect);
                model.status.set(status);
                model.linuxstatus.set(linuxstatus);
            }
        }
        if(rnaListAdapter!=null) {
            rnaListAdapter.notifyDataSetChanged();
        }
    }
    boolean STATUS_PCECOVER0 = false;
    boolean STATUS_DOOR = false;
    boolean STATUS_ARM = false;
    public void refreshExistInfo(){
        SerialPortManager_Robot.RobotStatus robotStatus = SerialPortManager_Robot.getInstance().getRobotStatus();
        if(robotStatus.getSenseByKey("PAN1") == true){
            binding.PAN1StateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else{
            binding.PAN1StateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        if(robotStatus.getSenseByKey("PAN2") == true){
            binding.PAN2StateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else{
            binding.PAN2StateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        if(robotStatus.getSenseByKey("PAN3") == true){
            binding.PAN3StateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.PAN3StateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        if(robotStatus.getSenseByKey("PAN4") == true){
            binding.PAN4StateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.PAN4StateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        if(robotStatus.getSenseByKey("PAN5") == true){
            binding.PAN5StateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.PAN5StateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        if(robotStatus.getSenseByKey("PAN6") == true){
            binding.PAN6StateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.PAN6StateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        if(robotStatus.getSenseByKey("PAN7") == true){
            binding.PAN7StateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.PAN7StateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        if(robotStatus.getSenseByKey("PAN8") == true){
            binding.PAN8StateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.PAN8StateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        STATUS_PCECOVER0 = robotStatus.getSenseByKey("PCR_COVER0");
        if(robotStatus.getSenseByKey("PCR_COVER0") == true){
            binding.pcrcovertempStateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.pcrcovertempStateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        STATUS_DOOR = robotStatus.getSenseByKey("DOOR");
        if(robotStatus.getSenseByKey("DOOR") == true){
            binding.doorStateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.doorStateImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        STATUS_ARM = robotStatus.getSenseByKey("ARM");
        if(robotStatus.getSenseByKey("ARM") == true){
            binding.armExistImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            binding.armExistImg.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
        updateTrayExist(1,robotStatus.getSenseByKey("PAN1"));
        updateTrayExist(2,robotStatus.getSenseByKey("PAN2"));
        updateTrayExist(3,robotStatus.getSenseByKey("PAN3"));
        updateTrayExist(4,robotStatus.getSenseByKey("PAN4"));
        updateTrayExist(5,robotStatus.getSenseByKey("PAN5"));
        updateTrayExist(6,robotStatus.getSenseByKey("PAN6"));
        updateTrayExist(7,robotStatus.getSenseByKey("PAN7"));
        updateTrayExist(8,robotStatus.getSenseByKey("PAN8"));

        updateRnaExist(1,robotStatus.getSenseByKey("RNA1"));
        updateRnaExist(2,robotStatus.getSenseByKey("RNA2"));

        updatePcrExist(1,robotStatus.getSenseByKey("PCR1"));
        updatePcrExist(2,robotStatus.getSenseByKey("PCR2"));
        updatePcrExist(3,robotStatus.getSenseByKey("PCR3"));
        updatePcrExist(4,robotStatus.getSenseByKey("PCR4"));
        updatePcrExist(5,robotStatus.getSenseByKey("PCR5"));
        updatePcrExist(6,robotStatus.getSenseByKey("PCR6"));

        updatePcrCover(1,robotStatus.getSenseByKey("PCR_COVER1"));
        updatePcrCover(2,robotStatus.getSenseByKey("PCR_COVER2"));
        updatePcrCover(3,robotStatus.getSenseByKey("PCR_COVER3"));
        updatePcrCover(4,robotStatus.getSenseByKey("PCR_COVER4"));
        updatePcrCover(5,robotStatus.getSenseByKey("PCR_COVER5"));
        updatePcrCover(6,robotStatus.getSenseByKey("PCR_COVER6"));

    }

    private void refreshSourceData() {
        ArrayList<PipeRNAModel> rnamodels = RNAManager.getInstance().getRnalist();
        for(PipeRNAModel model:rnamodels){
            if(model.currentTray.get()!=null&&model.currentTray.get()>0){
                if(model.exist.get()!=null&&model.exist.get()) {
                    updateRnaSource(model.index.get(), "托盘" + model.currentTray.get());
                }else{
                    updatePcrSource(model.index.get(), "无");
                }
            }
        }
        ArrayList<PipePCRModel> pcrmodels = PCRManager.getInstance().getPcrlist();
        for(PipePCRModel model:pcrmodels){
            if(model.currentTray.get()!=null&&model.currentTray.get()>0){
                if(model.exist.get()!=null&&model.exist.get()) {
                    updatePcrSource(model.index.get(), "托盘" + model.currentTray.get());
                }else{
                    updatePcrSource(model.index.get(), "无");
                }
            }
        }
    }
    boolean autoreset = false;//默认不会一键自动复位
    public void autoResetAll(){
        if(!autoreset) {
            autoreset = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 切换到 UI 线程更新界面
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resetError(ConstantData.DEVICE_ARM, 1);
                        }
                    });
                    resetError(ConstantData.DEVICE_QRSCANER, 1);

                    resetError(ConstantData.DEVICE_RNA, 1);
                    resetError(ConstantData.DEVICE_RNA, 2);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    resetError(ConstantData.DEVICE_PCR, 1);
                    resetError(ConstantData.DEVICE_PCR, 2);
                    resetError(ConstantData.DEVICE_PCR, 3);
                    resetError(ConstantData.DEVICE_PCR, 4);
                    resetError(ConstantData.DEVICE_PCR, 5);
                    resetError(ConstantData.DEVICE_PCR, 6);
                    autoreset = false;
                    // 切换到 UI 线程更新界面
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showText("");
                            binding.autobacktrayBtn.setVisibility(View.VISIBLE);
                        }
                    });
                }
            }).start();
        }else{
            showText("一键复位中，请不要重复点击！");
        }
    }
    boolean checkalltray = true;
    public void refreshAllTray(){
        String temp_text = "";
        checkalltray = true;
        ArrayList<PipeTrayModel> traymodels = TrayManager.getInstance().getTraylist();
        for(PipeTrayModel model:traymodels) {
            if(model.index.get()>=1 && model.index.get()<=7) {
                if (!model.exist.get()) {
                    checkalltray = false;
                    temp_text += "托盘" + model.index.get()+" ";
                }
            }
        }
        if(checkalltray){
            binding.autoclearallBtn.setVisibility(View.VISIBLE);
            showTrayText("托盘已全部归位！");
            // 仅当已批量标 BACK（确实经历过还盘）才判定会话完成，避免开局托盘本就归位时误结束
            if(startReturntray && (!autoReturnSession || returnMarked)){
                autoreturntray = false;
                startReturntray = false;
                autoReturnSession = false;
                showText("一键归还托盘任务运行完成！");
            }
        }else{
            showTrayText( temp_text + " 未归位！");
            binding.autobacktrayBtn.setVisibility(View.VISIBLE);
        }
    }
    boolean checkconnect = true;
    String alerttext = "";
    public void refreshConnectInfo(){
        checkconnect = true;
        ArrayList<PipeRNAModel> rnamodels = RNAManager.getInstance().getRnalist();
        for(PipeRNAModel model:rnamodels){
            if(!model.connect.get()){
                checkconnect = false;
                alerttext = "核提"+model.index.get()+"未连接";
            }
            updateRnaConnect(model.index.get(),model.connect.get(),model.status.get(),model.linuxStatus.get());
            if(model.status.get()!=null&&model.status.get().equalsIgnoreCase(ConstantData.str_ERROR)) {
                checkconnect = false;
                alerttext = "核提" + model.index.get() + " 异常，请复位！";
            }
        }
        ArrayList<PipePCRModel> pcrmodels = PCRManager.getInstance().getPcrlist();
        for(PipePCRModel model:pcrmodels){
            if(!model.connect.get()){
                checkconnect = false;
                alerttext = "PCR"+model.index.get()+"未连接";
            }
            updatePcrConnect(model.index.get(),model.connect.get(),model.status.get(),model.linuxStatus.get());
            if(model.status.get()!=null&&model.status.get().equalsIgnoreCase(ConstantData.str_ERROR)){
                checkconnect = false;
                alerttext = "PCR"+model.index.get()+" 异常，请复位！";
            }
        }
        ArrayList<PipeArmModel> armmodels = ArmManager.getInstance().getArmlist();
        for(PipeArmModel model:armmodels){
            if(!model.connect.get()){
                checkconnect = false;
                alerttext = "机械臂"+"未连接";
            }
            updateArmConnect(model.index.get(),model.connect.get());
            if(model.status.get()!=null&&model.status.get().equalsIgnoreCase(ConstantData.str_ERROR)){
                checkconnect = false;
                alerttext = getString(R.string.error_arm_reset);
                binding.armStatusImg.setStateBackgroud(R.drawable.shape_oval_red_bg);
            } else {
                binding.armStatusImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
            }
            if(model.finish.get()!=null && model.finish.get()){
                binding.armFinishImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
            }else {
                binding.armFinishImg.setStateBackgroud(R.drawable.shape_oval_red_bg);
            }
        }
        boolean qrcode = SerialPortManager_QRCode.getInstance().deviceQRCode;
        if (qrcode) {
            binding.scanerStateImg.setStateBackgroud(R.drawable.shape_oval_green_bg);
        } else {
          //  checkconnect = false;
            alerttext = getString(R.string.error_scaner_reset);
            binding.scanerStateImg.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(!WSSModuleManager.getInstance().getWSSConnectStatus()){
            binding.restartBtn.setVisibility(View.VISIBLE);
            checkconnect = false;
            alerttext = getString(R.string.error_wss_disconnect);
        }else{
            binding.restartBtn.setVisibility(View.GONE);
        }
        if(checkconnect){
//            binding.skipBtn.setVisibility(View.VISIBLE);
            binding.userLoginSelfcheckTv.setText(getString(R.string.all_device_connected));
//            binding.userLoginSelfcheckTv.setText(getString(R.string.system_self_check_done));
        }else {
            binding.userLoginSelfcheckTv.setText(alerttext+"");
//            binding.skipBtn.setVisibility(View.GONE);
        }
    }
    private boolean armHasTray(){
//        if(STATUS_ARM) {
            PipeArmModel model = ArmManager.getInstance().getArmModel(1);
            if(model.action.get()!=null && model.action.get().contains("pan")){
                return true;
            }
//        }
        return false;
    }
    private int findTrayFrom(){
        int position = 0;
        PipeArmModel model = ArmManager.getInstance().getArmModel(1);
        if (model != null && model.tray.get() != null) {
            position = model.tray.get();
        }
        if(position==0) {
            if (model.source.get() != null && model.source.get().contains(ConstantData.nameTray) && model.sourcePosition.get() != null) {
                if (model.sourcePosition.get() >= 1 && model.sourcePosition.get() <= 8) {
                    position = model.sourcePosition.get();
                }
            } else if (model.target.get() != null && model.target.get().contains(ConstantData.nameTray) && model.targetPosition.get() != null) {
                if (model.targetPosition.get() >= 1 && model.targetPosition.get() <= 8) {
                    position = model.targetPosition.get();
                }
            }
        }
        boolean checkresult = true;
        if(position!=0) {
            checkresult = checkTrayEmpty(position);
        }
        if(position==0||!checkresult){
            for(int i=0;i<traylist.size();i++){
                ConnectModel cmodel = traylist.get(i);
                if(cmodel!=null && cmodel.exist.get()!=null && !cmodel.exist.get() && cmodel.index.get()!=null){
                    position= cmodel.index.get();
                    break;
                }
            }
        }
        if(position==0) {
            showText("寻找 PCR盖子来源 失败，请手动处理！");
        }
        return position;
    }
    private boolean checkTrayEmpty(int position){
        for(int i=0;i<traylist.size();i++){
            ConnectModel model = traylist.get(i);
            if(model.index.get()!=null && position == model.index.get() && model.exist.get()!=null && !model.exist.get()){
                showText("找到托盘来源位置：PAN"+position);
                return true;
            }
        }
        showText("托盘来源位置：PAN"+position+" 检测存在，无法放回！");
        return false;
    }
    private int findPcrCoverFrom(){
        int position = 0;
        PipeArmModel model = ArmManager.getInstance().getArmModel(1);
        if(model.source.get()!=null&&model.source.get().contains(ConstantData.namePCRCover)&&model.sourcePosition.get()!=null){
            if(model.sourcePosition.get()>=1&&model.sourcePosition.get()<=6) {
                position = model.sourcePosition.get();
            }
        }else if(model.target.get()!=null&&model.target.get().contains(ConstantData.namePCRCover)&&model.targetPosition.get()!=null){
            if(model.targetPosition.get()>=1&&model.targetPosition.get()<=6) {
                position = model.targetPosition.get();
            }
        }
        boolean checkresult = true;
        if(position!=0){
            showText("找到PCR盖子来源位置：PCR"+position);
            checkresult = checkPcrCoverEmpty(position);
        }
        if(position==0||!checkresult){
            for(int i=0;i<pcrlist.size();i++){
                ConnectModel cmodel = pcrlist.get(i);
                if(cmodel!=null && cmodel.cover.get()!=null && !cmodel.cover.get() && cmodel.index.get()!=null){
                    position = cmodel.index.get();
                    showText("找到PCR盖子 空闲位置：PCR"+position);
                    break;
                }
            }
        }
        if(position==0) {
            showText("寻找 PCR盖子来源 失败，请手动处理！");
        }
        return position;
    }
    private boolean checkPcrCoverEmpty(int position){
        if(position==0){
            showText("PCR盖子来源位置获取失败，无法放回！");
            return false;
        }
        for(int i=0;i<pcrlist.size();i++){
            ConnectModel model = pcrlist.get(i);
            if(model.index.get()!=null && position == model.index.get() && model.cover.get()!=null && !model.cover.get()){
                return true;
            }
        }
        showText("PCR盖子来源位置：PCR"+position+" 检测存在盖子，无法放回！");
        return false;
    }
    private boolean armHasPCRCover(){
//        if(STATUS_ARM) {
            PipeArmModel model = ArmManager.getInstance().getArmModel(1);
            if(model.action.get()!=null && model.action.get().contains("pcr_cover")){
                return true;
            }
//            int countCoverLost = 0;
//            for(int i=0;i<pcrlist.size();i++){
//                ConnectModel model = pcrlist.get(i);
//                if(!model.cover.get()){
//                    countCoverLost++;
//                }
//            }
//            if(countCoverLost==1){
//                if(STATUS_PCECOVER0) {
//                    showText("PCR盖子存在盖子位！");
//                    return 0;
//                }else{
//                    showText("机械臂拿的是PCR盖子！");
//                    int position = findPcrCoverFrom();
//                    if(position>0) {
//                        if (checkPcrCoverEmpty(position)) {
//                            return position;
//                        }else{
//                            for(int i=0;i<pcrlist.size();i++){
//                                ConnectModel model = pcrlist.get(i);
//                                if(!model.cover.get()){
//                                    return model.index.get();
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//            if(countCoverLost==2){
//                showText("PCR盖子存在两个缺失！");
//                int position = findPcrCoverFrom();
//                if(position>0) {
//                    if (checkPcrCoverEmpty(position)) {
//                        return position;
//                    }else{
//                        for(int i=0;i<pcrlist.size();i++){
//                            ConnectModel model = pcrlist.get(i);
//                            if(!model.cover.get()){
//                                return model.index.get();
//                            }
//                        }
//                    }
//                }
//            }
//        }
        return false;
    }

//    private int tempHasPCRCover(){
//        if(STATUS_PCECOVER0) {
//            int position = findPcrCoverFrom();
//            if(position>0) {
//                if (checkPcrCoverEmpty(position)) {
//                    return position;
//                }else{
//                   for(int i=0;i<pcrlist.size();i++){
//                        ConnectModel model = pcrlist.get(i);
//                        if(!model.cover.get()){
//                            return model.index.get();
//                        }
//                   }
//                }
//            }
//        }
//        return 0;
//    }

    private boolean returnArmTray(){
        if(!STATUS_DOOR){
            showText("请先关门！");
            return false;
        }else {
            PipeArmModel model = ArmManager.getInstance().getArmModel( 1);
            if (model != null) {
                if (model.connect.get() != null && model.connect.get()) {
                    if (model.status.get() != null && model.status.get().equalsIgnoreCase(ConstantData.str_ERROR)) {
                        showText(getString(R.string.error_arm_reset) + ",请先复位机械臂！！！");
                        return false;
                    } else if(STATUS_ARM) {
                        if(armHasPCRCover()) {
                            if (ArmManager.getInstance().armSelfcheckCanuse()) {
                                int pcrtarget = findPcrCoverFrom();
                                if(pcrtarget!=0) {
                                    boolean result = ArmManager.getInstance().putPcrCover(pcrtarget, new ArmFinishCallback() {
                                        @Override
                                        public void finish(int arm, ItemProperty aim, ItemProperty source) {
                                            // 完成不再递归自链，交由刷新循环 driveAutoReturn 推进下一步
                                            returnFailCount = 0;
                                        }

                                        @Override
                                        public void fail(int arm, ItemProperty aim, ItemProperty source, String error) {
                                            returnFailCount++;
                                            showText("机械臂放回 PCR" + pcrtarget + "盖子 运行失败，请人工处理！");
                                        }
                                    });
                                    if (!result) {
                                        showText("机械臂放回 PCR" + pcrtarget + "盖子 失败！");
                                    } else {
                                        showText("机械臂放回 PCR" + pcrtarget + "盖子运行中！");
                                    }
                                }
                                return false;
                            } else {
                                showText("机械臂有盖，机械臂运行中，请稍后再试！");
                                return false;
                            }
                         } else if (armHasTray()) {
                            if (ArmManager.getInstance().armSelfcheckCanuse()) {
                                int traytarget = findTrayFrom();
                                if(traytarget!=0) {
                                    boolean result = ArmManager.getInstance().putTray(traytarget, new ArmFinishCallback() {
                                        @Override
                                        public void finish(int arm, ItemProperty aim, ItemProperty source) {
                                            // 完成不再递归自链，交由刷新循环 driveAutoReturn 推进下一步
                                            returnFailCount = 0;
                                        }

                                        @Override
                                        public void fail(int arm, ItemProperty aim, ItemProperty source, String error) {
                                            returnFailCount++;
                                            showText("机械臂放回 托盘" + traytarget + " 失败,请人工处理！");
                                        }
                                    });
                                    if (!result) {
                                        showText("机械臂放回 托盘" + traytarget + " 失败！");
                                    } else {
                                        showText("机械臂放回 托盘" + traytarget + " 运行中！");
                                    }
                                }
                                return false;
                            } else {
                                showText("机械臂有盘，机械臂运行中，请稍后再试！");
                                return false;
                            }
                        } else{
                            showText("机械臂有东西，无法判断是什么！");
                            return false;
                        }
                    }else if (STATUS_PCECOVER0) {
                        if (ArmManager.getInstance().armSelfcheckCanuse()) {
                            int pcrtarget = findPcrCoverFrom();
                            if(pcrtarget!=0) {
                                boolean result = ArmManager.getInstance().putTempPcrCover(pcrtarget, new ArmFinishCallback() {
                                    @Override
                                    public void finish(int arm, ItemProperty aim, ItemProperty source) {
                                        // 完成不再递归自链，交由刷新循环 driveAutoReturn 推进下一步
                                        returnFailCount = 0;
                                    }

                                    @Override
                                    public void fail(int arm, ItemProperty aim, ItemProperty source, String error) {
                                        returnFailCount++;
                                        showText("机械臂从 盖子暂存位 抓取,放回 PCR" + pcrtarget + "盖子 运行失败，请人工处理！");
                                    }
                                });
                                if (!result) {
                                    showText("机械臂从 盖子暂存位 抓取,放回 PCR" + pcrtarget + "盖子 失败！");
                                } else {
                                    showText("机械臂从 盖子暂存位 抓取,放回 PCR" + pcrtarget + "盖子 运行中！");
                                }
                            }
                            return false;
                        } else {
                            showText("盖子存放位有盖，机械臂运行中，请稍后再试！");
                            return false;
                        }
                    } else {
                        return true;
                    }
                } else {
                    showText("机械臂未连接！");
                    return false;
                }
            } else {
                showText("找不到机械臂数据！");
                return false;
            }
        }
    }
    boolean startReturntray = false;
    boolean autoreturntray = false;

    // ==== 一键还盘全自动状态机（由刷新循环驱动，等状态实时刷新后再决策）====
    boolean autoReturnSession = false;   // 还盘会话进行中
    boolean returnMarked = false;        // 是否已批量标 BACK（一次性）
    int returnFailCount = 0;             // 连续硬失败次数
    int returnTickCounter = 0;           // 刷新循环节流计数
    static final int RETURN_TICK_STEP = 3;   // 每 ~3s 推进一步（runnable 周期 1s）
    static final int RETURN_FAIL_MAX = 3;    // 连续失败达此值则停止会话，提示人工

    /**
     * 「一键还盘」按钮：仅开启会话，真正推进交给刷新循环 driveAutoReturn()。
     * 不再靠回调递归自链——避免机械臂状态未回刷的空档里断链、需用户手点。
     */
    public void autoReturnAll(){
        if(autoReturnSession){
            showText("一键还盘进行中，请勿重复点击！");
            return;
        }
        autoReturnSession = true;
        returnMarked = false;
        returnFailCount = 0;
        returnTickCounter = 0;
        startReturntray = true;   // 复用：供 refreshAllTray 完成提示
        showText("一键还盘已启动，自动清空机械臂并归还托盘…");
    }

    /**
     * 还盘会话每 tick 推进一步（由 runnable 每 ~3s 调用一次，调用前循环已刷新实时状态）。
     * 每次至多下发一条清空命令，忙时静默等待；仅机械臂 ERROR / 连续硬失败才停止会话。
     */
    private void driveAutoReturn(){
        if(!autoReturnSession) return;
        if(returnFailCount >= RETURN_FAIL_MAX){
            autoReturnSession = false;
            showText("一键还盘多次失败，请人工处理机械臂后重试！");
            return;
        }
        if(!STATUS_DOOR){                    // 门未关：保持会话等待
            showTrayText("请先关门以继续一键还盘！");
            return;
        }
        PipeArmModel arm = ArmManager.getInstance().getArmModel(1);
        if(arm==null || arm.connect.get()==null || !arm.connect.get()){
            return;                          // 机械臂未连：等待
        }
        if(arm.status.get()!=null && arm.status.get().equalsIgnoreCase(ConstantData.str_ERROR)){
            autoReturnSession = false;       // 机械臂 ERROR：硬停，提示复位
            showText(getString(R.string.error_arm_reset) + ",请先复位机械臂！！！");
            return;
        }
        if(!ArmManager.getInstance().armSelfcheckCanuse()){
            return;                          // 忙/未回 FREE：静默跳过，等下一 tick 状态实时刷新
        }
        // returnArmTray()：机械臂手上有盖/盘/临时盖则下发一条清空命令并返回 false；手已空返回 true
        if(returnArmTray()){
            if(!returnMarked){
                markBackAllOnce();
                returnMarked = true;
                showText("机械臂已清空，开始归还托盘…");
            }
        }
    }

    /** 一次性批量标 BACK+ERROR（仅在机械臂手空后调用一次），后续取盘交 5s checktrayerror 自动逐台执行。 */
    private void markBackAllOnce(){
        RNAManager.getInstance().setCloseRNADoor(1);
        RNAManager.getInstance().setCloseRNADoor(2);
        for (int i = 0; i < pcrlist.size(); i++) {
            ConnectModel model = pcrlist.get(i);
            if (model.exist.get()) {
                APLManager.getInstance().NotifyPcrTrayError(model.index.get());
                APLManager.getInstance().checkPcrFinishError(model.index.get());
            }
        }
        for (int i = 0; i < rnalist.size(); i++) {
            ConnectModel model = rnalist.get(i);
            if (model.exist.get()) {
                APLManager.getInstance().NotifyRnaTrayError(model.index.get());
                APLManager.getInstance().checkRnaFinishError(model.index.get());
            }
        }
    }
    boolean autoclearall = false;
    /**
     * 数据全清（保留样本条码/编号）。
     * 清除托盘的历史批次、项目、调度目标位置等信息；
     * 保留各孔的样本号（serialNum）和试剂条码（reagentCode）。
     */
    private void clearAllData(){
        if (!autoclearall) {
            autoclearall = true;
            APLManager.getInstance().clearDataKeepSamples(getContext());
            showText("一键清除数据成功！");
        } else {
            showText("一键清除数据已完成，请不要重复点击！");
        }
    }
    /**
     * 自检结果处理
     */
    private void selfCheckSuccess(){
        mainviewModel.onNavClicked(ConstantData.NAV_MAIN);
//        mainActivity.openNav(ConstantData.NAV_LOGIN);
//        if(loginFlag){
//            return;
//        }
//
//        if(skip){
//            loginFlag = true;
//            Intent intent = new Intent(UserLoginActivity.this, UserMainActivity.class);
//            startActivity(intent);
//            return;
//        }
//
//        boolean flag = PreferenceUtil.getInstance(getApplicationContext()).getBooleanValue(SharedConstant.IS_BOOT_UP_NEED_RESET, false);
//        String PCR_RUNNING_TEMPLATE = PreferenceUtil.getInstance(getApplicationContext())
//                .getStringValue(SharedConstant.PCR_RUNNING_TEMPLATE, "");
//        Log.i("Single", " login PCR_RUNNING_TEMPLATE" + PCR_RUNNING_TEMPLATE);
//        /////添加shared
//        if (flag && PcrApplication.rNARunningMessage.getStatus().contains("free") && PCR_RUNNING_TEMPLATE.equals("")) {
//            PreferenceUtil.getInstance(getApplicationContext())
//                    .setValueByName(SharedConstant.START_TIME, "");
//            loginFlag = true;
//            resetSystem();
//
//        } else if (PcrApplication.rNARunningMessage.getStatus().contains("free") && PCR_RUNNING_TEMPLATE.equals("")) {
//            PreferenceUtil.getInstance(getApplicationContext())
//                    .setValueByName(SharedConstant.START_TIME, "");
//            loginFlag = true;
//            Intent intent = new Intent(UserLoginActivity.this, UserMainActivity.class);
//            startActivity(intent);
//        } else if ((PcrApplication.rNARunningMessage.getStatus().contains("running")
//                || PcrApplication.rNARunningMessage.getStatus().contains("error")
//                || PcrApplication.rNARunningMessage.getStatus().contains("interrupt"))
//                && PCR_RUNNING_TEMPLATE.equals("")) {
//            //
//            loginFlag = true;
//            if (!resetFlag) {
//                Intent intent = new Intent(UserLoginActivity.this, UserRecoveryActivity.class);//////选择复位还是继续继续执行RNA片段页面
//                startActivity(intent);
//            }
//        } else if (!PCR_RUNNING_TEMPLATE.equals("")) {
//            loginFlag = true;
//            Intent intent = new Intent(UserLoginActivity.this, UserRecoveryActivity.class);//////选择复位还是继续继续执行PCR页面页面
//            startActivity(intent);
//        }

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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WSSMessageEvent messageEvent) {
        EnumMessageEvent eme = messageEvent.getMsgType();
        switch (eme) {
            case WSSManager_RNARunningFinish ://rna开关门，运行模板都会调用
                WSSConnectionItem item = messageEvent.getwSSConnectionItem();
                int rnaposition = item.getPosition();
                if (item.getDeviceType().equalsIgnoreCase(ConstantData.nameRNA)) {
                    if (messageEvent.getData() instanceof RNAFinishMessage) {
                        RNAFinishMessage message = (RNAFinishMessage) messageEvent.getData();
                        String template = message.getTemplate();
                        if (template != null) {
                            if(template.contains(ConstantData.RESET_EXCEPTION)){
                                if(message!=null&&message.getStatus()!=null&&message.getStatus().contains(ConstantData.str_ERROR)){
                                    sendMessage(MSG_ResetError_RNA,rnaposition);
                                }else {
                                    sendMessage(MSG_Reset_RNA,rnaposition);
                                }
                            }
                        }
                    }
                }
                break;
            case WSSManager_PCRRunningFinish:
                WSSConnectionItem wssitem = messageEvent.getwSSConnectionItem();
                int pcrposition = wssitem.getPosition();
                if (wssitem.getDeviceType().equalsIgnoreCase(ConstantData.namePCR) ) {
                    if(messageEvent.getData() instanceof PCRFinishMessage){
                        PCRFinishMessage message = (PCRFinishMessage)messageEvent.getData();
                        String template = message.getTemplate();
                        if(template!=null) {
                            if(template.contains(ConstantData.RESET_EXCEPTION)){
                                if(message!=null&&message.getStatus()!=null && message.getStatus().contains(ConstantData.str_ERROR)){
                                    sendMessage(MSG_ResetError_PCR,pcrposition);
                                }else {
                                    sendMessage(MSG_Reset_PCR,pcrposition);
                                }
                            }
                        }
                    }
                }
                break;
        }
    }
    private static final int SELF_CHECK = 100;
    private static final int MSG_ResetError_RNA = 101;
    private static final int MSG_ResetError_PCR = 102;
    private static final int MSG_Reset_RNA = 103;
    private static final int MSG_Reset_PCR = 104;
    private static final int HEARTCHECK = 105;
    private static final int MSG_END_INIT = 106;
    private static final int MSG_START_INIT  = 107;
    private static final int MSG_TEXT_SHOW = 108;
    private static final int MSG_TEXT_APPEND = 109;
    private static final int MSG_TEXT_SHOW_TRAY = 110;
    private static final int MSG_ALERT_ARM = 111;
    public class UIHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_START_INIT:
                    if(mainviewModel!=null)
                    mainviewModel.setProgressInfo(getResources().getString(R.string.login_dialog_message_init));
                    break;
                case MSG_END_INIT:
                    if(mainviewModel!=null)
                    mainviewModel.setProgressInfo("");
                    break;
                case HEARTCHECK:
//                    if( rnaHeartBeatCount++ > 4 ){
//                        ////RNA下位机没有心跳消息
//                        Log.i("RNA","reconnect");
//                        Log.i("RNA","is_RNA_connect"+ PcrApplication.is_RNA_connect  );
//                        // 20250102修改//
//                        PcrApplication.is_RNA_connect = false;
//                        ConnectManager.getInstance().initRNAConnection(getApplicationContext());
//                        rnaHeartBeatCount = 0;
//                        if(loginFlag){
//                            mUIHandler.sendEmptyMessage(RNA_CONNECTION_FAILED);
//                        }
//                    }else{
//                        if(loginFlag){
//                            mUIHandler.sendEmptyMessage(RNA_CONNECTION_NORMAL);
//                        }
//                    }
//                    /////PCR
//                    if (PcrApplication.is_SLAN_PCR_connect) {
//                        mUIHandler.sendEmptyMessage(PCR_CONNECTION_NORMAL);
//                    } else {
//                        Log.i("LoginActivity","PCR reCONNECT");
//                        mUIHandler.sendEmptyMessage(PCR_CONNECTION_FAILED);
//                    }

                    mUIHandler.sendEmptyMessage(SELF_CHECK);
                    break;
                case SELF_CHECK:
                    selfCheckSuccess();
//                    if(skip){
//                        return;
//                    }
//
//                    if(PcrApplication.is_RNA_connect && PcrApplication.is_SLAN_PCR_connect&& PcrApplication.is_RecRunningMesaage) {
//                        tryCount = 0;
//                        clearAlarm();
//                        if(null != mActivity.mNoticeDialog){
//                            if(mActivity.mNoticeDialog.isShowing()){
//                                mActivity.mNoticeDialog.dismiss();
//                            }
//                        }
//
//                        mActivity.selfCheckSuccess();
//                    }else{
//                        mActivity.mSelfCheckLayout.setVisibility(View.VISIBLE);
//
//                        mActivity.tryCount++;
//                        if( mActivity.tryCount > 3 ){
//                            String warning = "";
//                            if(!PcrApplication.is_RNA_connect||!PcrApplication.is_RecRunningMesaage){
//                                warning = " "+ getResources().getString(R.string.login_dialog_rna_extraction_error);
//                            }
//                            if(!PcrApplication.is_SLAN_PCR_connect){
//                                warning = warning + "  " + getResources().getString(R.string.login_dialog_slan_pcr_error);
//                            }
//                            warning = warning + "，"+ getResources().getString(R.string.login_dialog_check_or_restart);
//                            showAlarm();
//                            mActivity.showNoticeDialog(getResources().getString(R.string.dialog_title_prompt), warning);
//                            return;
//                        }
//                    }
                    break;
                case MSG_TEXT_SHOW:
                  if(msg.obj!=null&&binding.alertTv!=null){
                      binding.alertTv.setText(""+(String)msg.obj);
                  }
                    break;
                case MSG_ALERT_ARM:
                    if(msg.obj!=null&&binding.armTv!=null){
                        binding.armTv.setText(""+(String)msg.obj);
                    }
                    break;

                case MSG_TEXT_SHOW_TRAY:
                    if(msg.obj!=null&&binding.alltrayTv!=null){
                        binding.alltrayTv.setText(""+(String)msg.obj);
                    }
                    break;
                case MSG_TEXT_APPEND:
                    if(msg.obj!=null&&binding.alertTv!=null){
                        binding.alertTv.setText(binding.alertTv.getText()+" "+(String)msg.obj);
                    }
                    break;
                case MSG_Reset_RNA:
                    if(msg.obj!=null){
                        updateRnaAlert((int)msg.obj,"复位成功");
                    }
                    break;
                case MSG_ResetError_RNA:
                    if(msg.obj!=null){
                        updateRnaAlert((int)msg.obj,"复位失败");
                    }
                    break;
                case MSG_Reset_PCR:
                    if(msg.obj!=null){
                        updatePcrAlert((int)msg.obj,"复位成功");
                    }
                    break;
                case MSG_ResetError_PCR:
                    if(msg.obj!=null){
                        updatePcrAlert((int)msg.obj,"复位失败");
                    }
                    break;

            }
        }
    }
}