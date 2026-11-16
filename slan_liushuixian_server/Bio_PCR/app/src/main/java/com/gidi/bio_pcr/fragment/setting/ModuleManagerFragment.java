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
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.PCRManager;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
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
    // position -> deviceId 映射（position 1-6 对应 SLANPCR1111~6666）
    private static final String[] POSITION_DEVICE_IDS = {
            "SLANPCR1111", "SLANPCR2222", "SLANPCR3333",
            "SLANPCR4444", "SLANPCR5555", "SLANPCR6666"
    };
    private ModuleSettingsAdapter moduleSettingsAdapter;
    private List<ConnectModel> moduleDevices;
    private ListView moduleLv;
    private CustomDialog moduleIdSettingDialog;
    // 设备ID 弹窗相关
    private PopupWindow mDeviceIdPopupWindow;
    private ListView mDeviceIdLv;
    private List<String> mDeviceIdList;
    private CustomSpinnerAdapter mDeviceIdAdapter;


    private PopupWindow mPositionPopupWindow;
    private ListView mPositionLv;
    private List<String> mPositionList;
    private CustomSpinnerAdapter mPositionAdapter;

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
    }

    private UIHandler uiHandler;
    private void initData() {
        uiHandler = new UIHandler(this);
        moduleDevices = new ArrayList<>();
    }

    // 初始化 设备ID 选择弹窗
    private void initDeviceIdPopup() {
        String[] channelAarray= getResources().getStringArray(R.array.PCR_DeviceID);
        mDeviceIdList = new ArrayList<String>(channelAarray.length);
        Collections.addAll(mDeviceIdList, channelAarray);
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        mDeviceIdAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mDeviceIdList);
        mDeviceIdLv= (ListView) view.findViewById(R.id.spinner_listview);
        mDeviceIdLv.setAdapter(mDeviceIdAdapter);
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

        mDeviceIdAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String deviceId = mDeviceIdList.get(position);
                if (click_pos >= 0 && click_pos < moduleDevices.size()) {
                    int last_position = moduleDevices.get(click_pos).position.get();
                    settingModuleId(deviceId, getPositionByDeviceId(deviceId), last_position);
                }
                dismissDeviceIdPopup();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    // 初始化 设备Position 选择弹窗
    private void initPositionPopup() {
        String[] channelAarray= getResources().getStringArray(R.array.PCR_DevicePosition);
        mPositionList = new ArrayList<String>(channelAarray.length);
        Collections.addAll(mPositionList, channelAarray);
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        mPositionAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mPositionList);
        mPositionLv= (ListView) view.findViewById(R.id.spinner_listview);
        mPositionLv.setAdapter(mPositionAdapter);
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

        mPositionAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                int set_position = StringUtil.convertToInt(mPositionList.get(position), 1);
                if (click_pos >= 0 && click_pos < moduleDevices.size()) {
                    int last_position = moduleDevices.get(click_pos).position.get();
                    settingModuleId(getDeviceIdByPosition(set_position), set_position, last_position);
                }
                dismissPositionPopup();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private int click_pos= -1;
    private void initBinding(View view){
        moduleLv = (ListView)view.findViewById(R.id.module_manager_lv);
        moduleSettingsAdapter = new ModuleSettingsAdapter(getActivity().getApplicationContext());
        moduleSettingsAdapter.setModuleList(moduleDevices);
        moduleLv.setAdapter(moduleSettingsAdapter);

        moduleSettingsAdapter.setOnItemClickListener(new ModuleSettingsAdapter.OnItemClickListener() {
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

                        // 一体机状态单独赋值
                        if (item instanceof PCRStatusItem) {
                            connectModel.status.set(((PCRStatusItem) item).getpCRStatus());
                        }

                        // 加入列表
                        moduleDevices.add(connectModel);

                    // 新建模型
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


                    mFragment.moduleSettingsAdapter.setModuleList(mFragment.moduleDevices);
                    mFragment.moduleSettingsAdapter.notifyDataSetChanged();
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

    private void dismissDeviceIdPopup() {
        if (mDeviceIdPopupWindow != null && mDeviceIdPopupWindow.isShowing()) {
            mDeviceIdPopupWindow.dismiss();
        }
    }

    //==============【Position弹窗处理】========//
    private void showPositionPopup(View v) {
        if (mPositionPopupWindow == null) {
            initPositionPopup(); // 第一次点击时初始化
        }

        mPositionPopupWindow.showAsDropDown(v); // 替换成你的锚点

    }

    private void dismissPositionPopup() {
        if (mPositionPopupWindow != null && mPositionPopupWindow.isShowing()) {
            mPositionPopupWindow.dismiss();
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

}
