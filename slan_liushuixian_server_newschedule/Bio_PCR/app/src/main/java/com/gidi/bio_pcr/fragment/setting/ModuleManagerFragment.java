package com.gidi.bio_pcr.fragment.setting;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.adapter.ModuleSettingsAdapter;
import com.gidi.bio_pcr.bean.ConnectModel;
import com.gidi.bio_pcr.util.ModuleEnableManager;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.PCRManager;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模组管理修改DeviceId和DeviceName
 */
public class ModuleManagerFragment extends Fragment {
    private static final String TAG = "ModuleManagerFragment";
    private static final int UPDATE_CHECK_UI = 201;
    // position -> deviceId 映射（position 1-6 对应 SLANPCR1111~6666），定义在 DeviceIdMap
    private static final String[] POSITION_DEVICE_IDS = com.paul.pipeline.data.DeviceIdMap.PCR_DEVICE_IDS;
    private static final String[] POSITION_RNA_DEVICE_IDS = com.paul.pipeline.data.DeviceIdMap.RNA_DEVICE_IDS;
    private ModuleSettingsAdapter pcrSettingsAdapter,rnaModuleAdapter;
    private List<ConnectModel> moduleDevices,rnaModuleDevices;
    private ListView pcrModuleLv,rnaModuleLv;
    private CustomDialog moduleIdSettingDialog;
    // 设备ID 弹窗相关
    private PopupWindow mDeviceIdPopupWindow,mRnaDeviceIdPopWindow;
    private ListView mDeviceIdLv,mRnaDeviceIdLv;
    private List<String> mDeviceIdList,mRnaDeviceIdList;
    private CustomSpinnerAdapter mPcrDeviceIdAdapter,mRnaDeviceIdAdapter;


    private PopupWindow mPositionPopupWindow,mRnaPositionPopWindow;
    private ListView mPcrPositionLv,mRnaPositionLv;
    private List<String> mPcrPosList,mRnaPositionList;
    private CustomSpinnerAdapter mPcrPositionAdapter,mRnaPositionAdapter;

    public static ModuleManagerFragment newInstance() {
        return new ModuleManagerFragment();
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }



    private View rootView;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //不要复用每次都加载
        rootView = inflater.inflate(R.layout.fragment_module_manager, container, false);
        initBinding(rootView);
        initDeviceIdPopup();
        initPositionPopup();
        initRNADeviceIdPopup();
        initRnaPositionPopup();
        return  rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!isHidden()) {
            startPolling();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            stopPolling();
        } else {
            startPolling();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != uiHandler){
            uiHandler.removeCallbacks(runnable);
            uiHandler.removeCallbacksAndMessages(null);
            uiHandler = null;
        }
        if(null != moduleDevices){
            moduleDevices.clear();
            moduleDevices = null;
        }

        if(null != moduleIdSettingDialog){
            moduleIdSettingDialog.dismiss();
            moduleIdSettingDialog = null;
        }

        if(null != mDeviceIdPopupWindow){
            mDeviceIdPopupWindow.dismiss();
            mDeviceIdPopupWindow = null;
        }

        if(null != mPositionPopupWindow){
            mPositionPopupWindow.dismiss();
            mPositionPopupWindow = null;
        }

        if(null != rnaModuleDevices){
            rnaModuleDevices.clear();
            rnaModuleDevices = null;
        }

        if(null != mRnaDeviceIdList){
            mRnaDeviceIdList.clear();
            mRnaDeviceIdList = null;
        }
        if(null != mRnaDeviceIdPopWindow){
            mRnaDeviceIdPopWindow.dismiss();
            mRnaDeviceIdPopWindow = null;
        }
        if(null != mRnaPositionPopWindow){
            mRnaPositionPopWindow.dismiss();
            mRnaPositionPopWindow = null;
        }
    }

    private UIHandler uiHandler;
    private void initData() {
        uiHandler = new UIHandler(this);
        moduleDevices = new ArrayList<>();
        rnaModuleDevices  = new ArrayList<>();
    }

    // 初始化 设备ID 选择弹窗
    private void initDeviceIdPopup() {
        String[] channelAarray= getResources().getStringArray(R.array.PCR_DeviceID);
        mDeviceIdList = new ArrayList<String>(channelAarray.length);
        Collections.addAll(mDeviceIdList, channelAarray);
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        mPcrDeviceIdAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mDeviceIdList);
        mDeviceIdLv= (ListView) view.findViewById(R.id.spinner_listview);
        mDeviceIdLv.setAdapter(mPcrDeviceIdAdapter);
        mDeviceIdPopupWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mDeviceIdPopupWindow.setOutsideTouchable(true);
        mDeviceIdPopupWindow.setFocusable(true);
        mDeviceIdPopupWindow.setContentView(view);
        mDeviceIdPopupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mDeviceIdPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                click_pos = -1;
            }
        });

        mPcrDeviceIdAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String deviceId = mDeviceIdList.get(position);
                if (click_pos >= 0 && click_pos < moduleDevices.size()) {
                    int last_position = moduleDevices.get(click_pos).position.get();
                    settingModuleId(deviceId, getPositionByDeviceId(deviceId), last_position);
                }
                dismissPcrDeviceIdPopup();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    // 初始化 设备Position 选择弹窗
    private void initPositionPopup() {
        String[] channelAarray= getResources().getStringArray(R.array.PCR_DevicePosition);
        mPcrPosList = new ArrayList<String>(channelAarray.length);
        Collections.addAll(mPcrPosList, channelAarray);
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        mPcrPositionAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mPcrPosList);
        mPcrPositionLv= (ListView) view.findViewById(R.id.spinner_listview);
        mPcrPositionLv.setAdapter(mPcrPositionAdapter);
        mPositionPopupWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mPositionPopupWindow.setOutsideTouchable(true);
        mPositionPopupWindow.setFocusable(true);
        mPositionPopupWindow.setContentView(view);
        mPositionPopupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mPositionPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                click_pos = -1;
            }
        });

        mPcrPositionAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                int set_position = StringUtil.convertToInt(mPcrPosList.get(position), 1);
                if (click_pos >= 0 && click_pos < moduleDevices.size()) {
                    int last_position = moduleDevices.get(click_pos).position.get();
                    settingModuleId(getDeviceIdByPosition(set_position), set_position, last_position);
                }
                dismissPcrPosPopup();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    //======================================c核提模组========================================
    private int click_rna_id_pos = -1;
    private void initRNADeviceIdPopup(){
        String[] channelAarray= getResources().getStringArray(R.array.RNA_DeviceID);
        mRnaDeviceIdList = new ArrayList<String>(channelAarray.length);
        Collections.addAll(mRnaDeviceIdList, channelAarray);
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        mRnaDeviceIdAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mRnaDeviceIdList);
        mRnaDeviceIdLv= (ListView) view.findViewById(R.id.spinner_listview);
        mRnaDeviceIdLv.setAdapter(mRnaDeviceIdAdapter);
        mRnaDeviceIdPopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mRnaDeviceIdPopWindow.setOutsideTouchable(true);
        mRnaDeviceIdPopWindow.setFocusable(true);
        mRnaDeviceIdPopWindow.setContentView(view);
        mRnaDeviceIdPopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mRnaDeviceIdPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                click_rna_id_pos = -1;
            }
        });

        mRnaDeviceIdAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String deviceId = mRnaDeviceIdList.get(position);
                if (click_rna_id_pos >= 0 && click_rna_id_pos < rnaModuleDevices.size()) {
                    int last_position = rnaModuleDevices.get(click_rna_id_pos).position.get();
                    settingModuleId(deviceId, getPositionByDeviceId(deviceId), last_position);
                }
                dismissRnaDeviceIdPopup();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private void initRnaPositionPopup() {
        String[] channelAarray= getResources().getStringArray(R.array.RNA_DevicePosition);
        mRnaPositionList = new ArrayList<String>(channelAarray.length);
        Collections.addAll(mRnaPositionList, channelAarray);
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        mRnaPositionAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mRnaPositionList);
        mRnaPositionLv= (ListView) view.findViewById(R.id.spinner_listview);
        mRnaPositionLv.setAdapter(mRnaPositionAdapter);
        mRnaPositionPopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mRnaPositionPopWindow.setOutsideTouchable(true);
        mRnaPositionPopWindow.setFocusable(true);
        mRnaPositionPopWindow.setContentView(view);
        mRnaPositionPopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mRnaPositionPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                click_rna_id_pos = -1;
            }
        });

        mRnaPositionAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                int set_position = StringUtil.convertToInt(mRnaPositionList.get(position), 1);
                if (click_rna_id_pos >= 0 && click_rna_id_pos < mRnaPositionList.size()) {
                    int last_position = rnaModuleDevices.get(click_rna_id_pos).position.get();
                    settingRnaModuleId(getDeviceIdByPosition(set_position), set_position, last_position);
                }
                dismissRnaPositionPopup();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }




    private int click_pos= -1;
    private void initBinding(View view){
        pcrModuleLv = (ListView)view.findViewById(R.id.module_manager_lv);
        pcrSettingsAdapter = new ModuleSettingsAdapter(getActivity().getApplicationContext());
        pcrSettingsAdapter.setModuleList(moduleDevices);
        pcrModuleLv.setAdapter(pcrSettingsAdapter);

        //rna的模组列表
        rnaModuleLv = (ListView)view.findViewById(R.id.module_manager_rna_lv);
        rnaModuleAdapter = new ModuleSettingsAdapter(getActivity().getApplicationContext());
        rnaModuleAdapter.setModuleList(rnaModuleDevices);//改
        rnaModuleLv.setAdapter(rnaModuleAdapter);

        view.findViewById(R.id.module_restart_all_pcr_btn).setOnClickListener(v -> confirmRestartAllPcr());

        pcrSettingsAdapter.setOnItemClickListener(new ModuleSettingsAdapter.OnItemClickListener() {
            @Override
            public void onItemDeviceIdClick(View v,int item_position) {
                //显示所有的列表可以选择的id名称
                click_pos = item_position;
                showDeviceIdPopup(v);

            }

            @Override
            public void onItemPositionClick(View v,int item_position) {
                //可以选择所有的position
                click_pos = item_position;
                showPositionPopup(v);
            }

            @Override
            public void onItemEnableSwitchChanged(int position, String deviceId, boolean isEnabled) {
                // 处理启用/禁用开关变化
                handleModuleEnableChanged(deviceId, isEnabled);
            }
        });

        rnaModuleAdapter.setOnItemClickListener(new ModuleSettingsAdapter.OnItemClickListener() {
            @Override
            public void onItemDeviceIdClick(View v, int item_position) {
                click_rna_id_pos = item_position;
                showRnaDeviceIdPopup(v);

            }

            @Override
            public void onItemPositionClick(View v, int item_position) {
                click_rna_id_pos = item_position;
                showRnaPositionPopup(v);
            }

            @Override
            public void onItemEnableSwitchChanged(int position, String deviceId, boolean isEnabled) {
                // 处理 RNA 模组启用/禁用开关变化
                handleModuleEnableChanged(deviceId, isEnabled);
            }
        });

    }



    private void startPolling() {
        if(null == uiHandler){
            uiHandler = new UIHandler(this);
        }
        if(null != uiHandler) {
            uiHandler.removeCallbacks(runnable);
            uiHandler.postDelayed(runnable, 1000);
        }
    }

    private void stopPolling() {
        if(null != uiHandler) {
            uiHandler.removeCallbacks(runnable);
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            refreshConnectInfo();
            if(null != uiHandler){
                uiHandler.postDelayed(runnable, 2000);
            }
        }
    };

    private void refreshConnectInfo(){
        Log.i(TAG,"===refreshConnectInfo==");
        if (APLManager.getInstance().WSSStart) {
            ConcurrentHashMap<String, WSSConnectionItem> datas = WSSModuleManager.getInstance().getWssConnectionItems();
            if (datas == null || datas.isEmpty()) {
                return;
            }
            Log.i(TAG,"===datas=="+ datas.size());

            // 清空旧列表
            moduleDevices.clear();
            rnaModuleDevices.clear();

            // 遍历所有连接项，只拿 有IP、类型正确 的设备
            for (WSSConnectionItem item : datas.values()) {
                // 过滤条件：设备类型正确 + IP 不为空
                if (item.getDeviceType() != null
                        && item.getDeviceType().equalsIgnoreCase(ConstantData.namePCR)
                        && item.getDeviceIP() != null
                        && !item.getDeviceIP().isEmpty()) {
                    Log.e(TAG,"==item.isWSSConnect()==="+ item.isWSSConnect());

                    ConnectModel connectModel = new ConnectModel();

                    // 赋值
                    connectModel.deviceIp.set(item.getDeviceIP());
                    connectModel.deviceVersion.set(item.getVersion());
                    connectModel.position.set(item.getPosition());
                    connectModel.deviceId.set(item.getDeviceID());
                    connectModel.deviceConnect.set(item.isWSSConnect());

                    // 加载启用状态
                    boolean isEnabled = ModuleEnableManager.getInstance(getContext())
                            .isModuleEnabled(item.getDeviceID());
                    connectModel.isEnabled.set(isEnabled);

                    // 一体机状态单独赋值
                    if (item instanceof PCRStatusItem) {
                        connectModel.status.set(((PCRStatusItem) item).getpCRStatus());
                    }

                    // 加入列表
                    moduleDevices.add(connectModel);

                    // 新建模型
                }

                //rna列表
                if (item.getDeviceType() != null
                        && item.getDeviceType().equalsIgnoreCase(ConstantData.nameRNA)
                        && item.getDeviceIP() != null
                        && !item.getDeviceIP().isEmpty()) {
                    Log.e(TAG,"==item.isWSSConnect()==="+ item.isWSSConnect());

                    ConnectModel rna_connectModel = new ConnectModel();

                    // 赋值
                    rna_connectModel.deviceIp.set(item.getDeviceIP());
                    rna_connectModel.deviceVersion.set(item.getVersion());
                    rna_connectModel.position.set(item.getPosition());
                    rna_connectModel.deviceId.set(item.getDeviceID());
                    rna_connectModel.deviceConnect.set(item.isWSSConnect());

                    // 加载启用状态
                    boolean isEnabled = ModuleEnableManager.getInstance(getContext())
                            .isModuleEnabled(item.getDeviceID());
                    rna_connectModel.isEnabled.set(isEnabled);

                    // 一体机状态单独赋值
                    if (item instanceof RNAStatusItem) {
                        rna_connectModel.status.set(((RNAStatusItem) item).getrNAStatus());
                    }

                    // 加入列表
                    rnaModuleDevices.add(rna_connectModel);
                }
            }
        }

        if (uiHandler != null) {
            uiHandler.sendEmptyMessage(UPDATE_CHECK_UI);
        }
    }


    public static class UIHandler extends Handler {
        WeakReference<ModuleManagerFragment> mFragmentReference;

        public UIHandler(ModuleManagerFragment mFragment) {
            mFragmentReference = new WeakReference<ModuleManagerFragment>(mFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            final ModuleManagerFragment mFragment = mFragmentReference.get();
            if (null == mFragment) {
                return;
            }
            switch (msg.what) {
                case UPDATE_CHECK_UI:
                    if(!APLManager.getInstance().WSSStart){
                        if(null != mFragment.moduleDevices && mFragment.moduleDevices.size()>0){
                            for(ConnectModel model: mFragment.moduleDevices){
                                model.deviceConnect.set(false);
                            }
                        }
                    }
                    if(!APLManager.getInstance().WSSStart){
                        if(null != mFragment.rnaModuleDevices && mFragment.rnaModuleDevices.size()>0){
                            for(ConnectModel model: mFragment.rnaModuleDevices){
                                model.deviceConnect.set(false);
                            }
                        }
                    }

                    mFragment.pcrSettingsAdapter.setModuleList(mFragment.moduleDevices);
                    mFragment.pcrSettingsAdapter.notifyDataSetChanged();

                    mFragment.rnaModuleAdapter.setModuleList(mFragment.rnaModuleDevices);//改
                    mFragment.rnaModuleAdapter.notifyDataSetChanged();
                    break;

                default:
                    break;
            }
        }
    }

    //下发QueryMessage
    private void settingModuleId(String deviceId, int position,int last_position) {
        if(isDeviceIdExists(deviceId)) {

            String errorMsg = "当前需要设置模组的id和position已存在，不能重复设置";
            showResultDialog(false, errorMsg);
            return;
        }
        String result = PCRManager.getInstance().sendQueryDeviceMsg(last_position,position, deviceId);
        Log.i(TAG,"===result=="+ result);
        if (!TextUtils.isEmpty(result) && result.contains("succeed")) {
            // 成功：提示用户，列表数据由 refreshConnectInfo 轮询刷新
            showResultDialog(true, getString(R.string.module_id_set_success));
        } else {
            // 失败
            String errorMsg = TextUtils.isEmpty(result) ? getString(R.string.unknown_error) : result;
            showResultDialog(false, errorMsg);
        }
    }


    private void settingRnaModuleId(String deviceId, int position,int last_position) {
        if(isRnaDeviceIdExists(deviceId)) {

            String errorMsg = "当前需要设置模组的id和position已存在，不能重复设置";
            showResultDialog(false, errorMsg);
            return;
        }
        String result = PCRManager.getInstance().sendQueryDeviceMsg(last_position,position, deviceId);
        Log.i(TAG,"===result=="+ result);
        if (!TextUtils.isEmpty(result) && result.contains("succeed")) {
            // 成功：提示用户，列表数据由 refreshConnectInfo 轮询刷新
            showResultDialog(true, getString(R.string.module_id_set_success));
        } else {
            // 失败
            String errorMsg = TextUtils.isEmpty(result) ? getString(R.string.unknown_error) : result;
            showResultDialog(false, errorMsg);
        }
    }


    /**
     * 处理模组启用/禁用开关变化
     * @param deviceId 设备ID
     * @param isEnabled 是否启用
     */
    private void handleModuleEnableChanged(String deviceId, boolean isEnabled) {
        if (deviceId == null || deviceId.isEmpty()) {
            return;
        }

        // 保存启用状态到 SharedPreferences
        boolean success = ModuleEnableManager.getInstance(getContext()).setModuleEnabled(deviceId, isEnabled);

        if (success) {
            String status = isEnabled ? "已启用" : "已禁用";
            String message = "模组 " + deviceId + " " + status + "\n\n设置将在重启应用后生效";

            // 显示提示对话框
            showRestartRequiredDialog(message);
        } else {
            // 使用 Dialog 显示错误提示
            showErrorDialog("保存失败，请重试");
        }
    }

    /** 手动重启全部 PCR client：先确认，再在后台线程下发（发送走网络，不放主线程） */
    private void confirmRestartAllPcr() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        new android.app.AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom)
                .setTitle("提示")
                .setMessage("将重启所有空闲的PCR程序。\n未连接、已禁用、调度占用中（有盘或待运行模板）的模组会跳过。\n确定重启吗？")
                .setPositiveButton("确定", (d, w) -> new Thread(() -> {
                    String result = com.paul.pipeline.manager.PCRClientRestartScheduler
                            .getInstance().restartAllNow();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> showRestartRequiredDialog(result));
                    }
                }, "RestartAllPCR").start())
                .setNegativeButton("取消", null)
                .setCancelable(true)
                .show();
    }

    /**
     * 显示"需要重启"提示对话框
     * @param message 提示信息
     */
    private void showRestartRequiredDialog(String message) {
        // 防止页面销毁导致崩溃
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom);

        builder.setTitle("提示");
        builder.setMessage(message);
        builder.setPositiveButton("我知道了", null);
        builder.setCancelable(true);
        builder.show();
    }

    /**
     * 显示错误提示对话框
     * @param message 错误信息
     */
    private void showErrorDialog(String message) {
        // 防止页面销毁导致崩溃
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom);

        builder.setTitle("错误");
        builder.setMessage(message);
        builder.setPositiveButton("确定", null);
        builder.setCancelable(true);
        builder.show();
    }

    /**
     * 显示设置结果弹窗
     * @param isSuccess 是否成功
     * @param message 提示信息
     */

    private void showResultDialog(boolean isSuccess, String message) {
        // 防止页面销毁导致崩溃
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        // 带自定义主题的系统原生 Dialog
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom);

        builder.setTitle(isSuccess ? "Success" : "Failed");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.setCancelable(true);
        builder.show();
    }

    //显示DeviceID的弹窗
    private void showDeviceIdPopup(View v) {
        if (mDeviceIdPopupWindow == null) {
            initDeviceIdPopup(); // 第一次点击时初始化
        }

        mDeviceIdPopupWindow.showAsDropDown(v); // 替换成你的锚点

    }

    private void dismissPcrDeviceIdPopup() {
        if (mDeviceIdPopupWindow != null && mDeviceIdPopupWindow.isShowing()) {
            mDeviceIdPopupWindow.dismiss();
        }
    }

    private void showRnaDeviceIdPopup(View v) {
        if (mRnaDeviceIdPopWindow == null) {
            initRNADeviceIdPopup(); // 第一次点击时初始化
        }

        mRnaDeviceIdPopWindow.showAsDropDown(v); // 替换成你的锚点
    }

    private void dismissRnaDeviceIdPopup() {
        if (mRnaDeviceIdPopWindow != null && mRnaDeviceIdPopWindow.isShowing()) {
            mRnaDeviceIdPopWindow.dismiss();
        }
    }

    //==============【Position弹窗处理】========//
    private void showPositionPopup(View v) {
        if (mPositionPopupWindow == null) {
            initPositionPopup(); // 第一次点击时初始化
        }

        mPositionPopupWindow.showAsDropDown(v); // 替换成你的锚点

    }

    private void dismissPcrPosPopup() {
        if (mPositionPopupWindow != null && mPositionPopupWindow.isShowing()) {
            mPositionPopupWindow.dismiss();
        }
    }

    private void showRnaPositionPopup(View v) {
        if (mRnaPositionPopWindow == null) {
            initRnaPositionPopup(); // 第一次点击时初始化
        }

        mRnaPositionPopWindow.showAsDropDown(v); // 替换成你的锚点

    }
    private void dismissRnaPositionPopup() {
        if (mRnaPositionPopWindow != null && mRnaPositionPopWindow.isShowing()) {
            mRnaPositionPopWindow.dismiss();
        }
    }

    // position -> deviceId（position 1-6 对应 SLANPCR1111~6666）
    private int getPositionByDeviceId(String deviceId) {
        for (int i = 0; i < POSITION_DEVICE_IDS.length; i++) {
            if (POSITION_DEVICE_IDS[i].equals(deviceId)) {
                return i + 1;
            }
        }
        return 1;
    }

    private String getDeviceIdByPosition(int position) {
        if (position >= 1 && position <= POSITION_DEVICE_IDS.length) {
            return POSITION_DEVICE_IDS[position - 1];
        }
        return POSITION_DEVICE_IDS[0];
    }

    /**
     * 判断 DeviceID 是否已经在列表中存在
     */
    private boolean isDeviceIdExists(String deviceId) {
        if (moduleDevices == null || moduleDevices.isEmpty()) {
            return false;
        }
        for (ConnectModel model : moduleDevices) {
            if (model != null && deviceId.equals(model.deviceId.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断 RNA模组的DeviceID 是否已经在列表中存在
     */
    private boolean isRnaDeviceIdExists(String deviceId) {
        if (rnaModuleDevices == null || rnaModuleDevices.isEmpty()) {
            return false;
        }
        for (ConnectModel model : rnaModuleDevices) {
            if (model != null && deviceId.equals(model.deviceId.get())) {
                return true;
            }
        }
        return false;
    }

}
