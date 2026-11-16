package com.gidi.bio_pcr.fragment.system;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.WifiConnectAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bean.MScanWifi;
import com.gidi.bio_pcr.constant.BroadcastActions;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.listener.OnNetWorkChangedListener;
import com.gidi.bio_pcr.listener.WifiReceiverActionListener;
import com.gidi.bio_pcr.mgr.WifiCtrlManager;
import com.gidi.bio_pcr.receiver.NetWorkChangeReceiver;
import com.gidi.bio_pcr.receiver.WifiReceiver;
import com.gidi.bio_pcr.view.WifiNoticeDialog;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class WlanFragment extends BaseFragment implements OnNetWorkChangedListener,View.OnClickListener {


    private NetWorkChangeReceiver netWorkReceiver;

    private RelativeLayout ctrlWifiLayout;
    private Context mContext;
    private ImageView openWlanCk;
    private TextView mWifiStatusTxt;
    private TextView conStatusTxt;
    private TextView seachingTxt;
    private ListView mWifiListView;
    private WifiConnectAdapter wifiConnectAdapter;
    private WifiManager wifiManager;
    private WifiReceiver wifiReceiver;
    private WifiReceiverActionListener wifiReceiverActionListener;
    private List<ScanResult> scanResultList;//网络连接列表
    private List<MScanWifi> mWifiScanList;
    private WifiNoticeDialog connectDialog;
    private WifiNoticeDialog forgetDialog;
    private WifiNoticeDialog reconnectDialog;
    private ConnectWifiAsyncTask connectWifiAsyncTask;
    private UIHandler mUIHandler;
    private boolean isWifiOpen = false;

    private static final int MSG_OPEN_WIFI_STATE = 602;
    private static final int MSG_CLOSE_WIFI_STATE = 603;
    private static final int MSG_WIFI_SCANRESULT = 600;
    private static final int MSG_UPDATE_WIFILIST = 604;

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_sys_wlan;
    }

    @Override
    protected void initView(View rootView) {
        ctrlWifiLayout = (RelativeLayout)rootView.findViewById(R.id.wlan_state_layout) ;
        openWlanCk = (ImageView) rootView.findViewById(R.id.wifi_control_checkbox);
        mWifiStatusTxt = (TextView)rootView.findViewById(R.id.wlan_txt);
        conStatusTxt = (TextView)rootView.findViewById(R.id.connect_status_txt);
        mWifiListView = (ListView)rootView.findViewById(R.id.wifi_listview);
        seachingTxt = (TextView)rootView.findViewById(R.id.wifi_seaching_txt);
        if(WifiCtrlManager.getInstance(getActivity().getApplicationContext()).isEnableWifi()){
            mWifiStatusTxt.setText(R.string.wlan_open);
            isWifiOpen = true;
            openWlanCk.setImageResource(R.drawable.icon_wifi_on);
            ctrlWifiLayout.setBackgroundColor(getActivity().getResources()
                    .getColor(R.color.color_open_wifi));
        }else{
            mWifiStatusTxt.setText(R.string.wlan_close);
            isWifiOpen = false;
            openWlanCk.setImageResource(R.drawable.icon_wifi_off);
            ctrlWifiLayout.setBackgroundColor(getActivity().getResources()
                    .getColor(R.color.white));
        }
    }

    @Override
    protected void initListener() {
        initData();
        ctrlWifiLayout.setOnClickListener(this);
        wifiManager = (WifiManager)getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                // TODO Auto-generated method stub
                MScanWifi mScanWifi= mWifiScanList.get(position);
                ScanResult scanResult = mScanWifi.getScanResult();
                WifiConfiguration config = isExits(scanResult.SSID);
                if(config == null){
                    //伟配置，连接过
                    //有密码和无密码
                    if(SecurityMode.OPEN.equals(getSecurityMode(scanResult))){
                        //无密码
                        Log.i("WLAN", "----connect not password----");
                        connecStatus(true);
                        WifiConfiguration wifiConfiguration = createWifiConfiguration(scanResult.SSID, "", SecurityMode.OPEN);
                        connectWifiByPwd(wifiConfiguration);
                    }else{
                        //有密码
                        Log.i("WLAN", "----connect by password----");
                        showConnectWifiDialog(scanResult);
                    }
                }else{
                    //1配置，连接的要disconenct

                    if(isGivenWifiConnected(scanResult.SSID)){
                        showForgetWifiDialog(mScanWifi);
                    }else{
                        //2配置过，但没连接则重新连接
                        showReconnectDialog(scanResult,config);
                    }
                }
            }
        });
    }
    protected void initData() {
        // TODO Auto-generated method stub
        mContext = getActivity().getApplicationContext();
        wifiConnectAdapter = new WifiConnectAdapter(getActivity().getApplicationContext());
        mWifiListView.setAdapter(wifiConnectAdapter);
        scanResultList = new ArrayList<ScanResult>();
        mWifiScanList = new ArrayList<MScanWifi>();
        mUIHandler = new UIHandler(this);
        initWifiStatus();
        registerReceiver();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUIHandler = new UIHandler(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContext = null;
        if(null != scanResultList){
            scanResultList.clear();
            scanResultList = null;
        }

        if(null != mWifiScanList){
            mWifiScanList.clear();
            mWifiScanList = null;
        }

        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        cancelDialog();
        unRegisterReceiver();
    }

    private void registerReceiver(){
        wifiReceiverActionListener = new WifiReceiverActionListener() {
            //搜索返回wifi列表
            @Override
            public void onWifiResultBack(List<ScanResult> list) {
                // TODO Auto-generated method stub
                /***已经连接的wifi**/
                Log.i("WLAN", "onWifiResultBack---" + list.size());
                if(list.size() > 0){
                    seachingTxt.setVisibility(View.GONE);
                    List<ScanResult> tmpList = list;
                    if(null != scanResultList){
                        scanResultList.clear();
                        for(ScanResult tempScanResult : tmpList){
                            if(isGivenWifiConnected(tempScanResult.SSID) && !scanResultList.contains(tempScanResult)){
                                //已经连接
                                scanResultList.add(tempScanResult);
                            }
                            if(isGivenWifiSaved(tempScanResult.SSID)&& !scanResultList.contains(tempScanResult)){

                                scanResultList.add(tempScanResult);
                            }
                        }

                        for (ScanResult tmp : scanResultList) {

                            tmpList.remove(tmp);
                        }
                        scanResultList.addAll(tmpList);
                    }

                    mWifiScanList.clear();
                    for (ScanResult scanResult : scanResultList) {

                        int wifiLevel = wifiManager.calculateSignalLevel(scanResult.level, 4);
                        String securityMode = getSecurityModeString(scanResult);
                        MScanWifi mScanWifi = new MScanWifi(scanResult,securityMode, wifiLevel);
                        mWifiScanList.add(mScanWifi);
                    }
                    mUIHandler.sendEmptyMessage(MSG_WIFI_SCANRESULT);
                }

            }

            @Override
            public void onWifiOpening() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onWifiOpened() {
                // TODO Auto-generated method stub
                mWifiStatusTxt.setText(R.string.wlan_open);
            }

            @Override
            public void onWifiConnected(WifiInfo wifiInfo, NetworkInfo networkInfo) {
                // TODO Auto-generated method stub
                if(null != mUIHandler){
                    Message msg = Message.obtain();
                    msg.obj = networkInfo;
                    msg.what = Constants.MSG_WIFI_CONNECT_STATE;
                    mUIHandler.sendMessage(msg);
                }
            }

            @Override
            public void onWifiClosing() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onWifiClosed() {
                // TODO Auto-generated method stub
                clearWifi();
                mWifiStatusTxt.setText(R.string.wlan_close);
                scanResultList.clear();
                mWifiScanList.clear();
                mUIHandler.sendEmptyMessage(Constants.MSG_WIFI_TURN_OFF);
            }
        };
        wifiReceiver = new WifiReceiver(wifiReceiverActionListener);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getActivity().registerReceiver(wifiReceiver, intentFilter);

        getActivity().registerReceiver(netWorkReceiver = new NetWorkChangeReceiver(),
                new IntentFilter(BroadcastActions.NETWORK_CONNECTION_CHANGE));
        netWorkReceiver.registerListener(this);
    }

    private void unRegisterReceiver(){
        if(null != wifiReceiver){
            getActivity().unregisterReceiver(wifiReceiver);
            wifiReceiver = null;
        }
        if(null != netWorkReceiver){
            getActivity().unregisterReceiver(netWorkReceiver);
            netWorkReceiver.unregisterListener(this);
            netWorkReceiver = null;
        }
    }


    private void cancelDialog(){
        if(null != connectDialog){
            connectDialog.dismiss();
            connectDialog = null;
        }
        if(null != forgetDialog){
            forgetDialog.dismiss();
            forgetDialog = null;
        }

        if(null != reconnectDialog){
            reconnectDialog.dismiss();
            reconnectDialog = null;
        }
    }

    private void initWifiStatus(){
        if(null!= wifiManager){
            Log.i("BBB","initWifiStatus"+ wifiManager.isWifiEnabled());
            if(wifiManager.isWifiEnabled()){
                openWlanCk.setImageResource(R.drawable.icon_wifi_on);
                ctrlWifiLayout.setBackgroundColor(getActivity().getResources()
                        .getColor(R.color.color_open_wifi));
                isWifiOpen = true;
            }else{
                openWlanCk.setImageResource(R.drawable.icon_wifi_off);
                ctrlWifiLayout.setBackgroundColor(getActivity().getResources()
                        .getColor(R.color.white));
                isWifiOpen = false;
            }
        }
    }

    private void connecStatus(boolean flag){
        if(flag){
            conStatusTxt.setText(R.string.wlan_connecting);
            conStatusTxt.setVisibility(View.VISIBLE);
        }else{
            conStatusTxt.setVisibility(View.INVISIBLE);
        }
    }





/**************************************************wifi 操作**********************************************************/


    /****
     * close wifi
     */
    private void clearWifi(){
        if(null != scanResultList){
            scanResultList.clear();
            wifiConnectAdapter.setWifiList(mWifiScanList);
            wifiConnectAdapter.notifyDataSetChanged();
        }
    }



    /****
     * 新开线程是使用wifi配置文件连接wifi
     */
    private void connectWifiByPwd(WifiConfiguration wifiConfiguration){
        Log.i("WLAN", "connectWifiByPwd");
        if(null == connectWifiAsyncTask){
            connectWifiAsyncTask = new ConnectWifiAsyncTask(wifiConfiguration);
            connectWifiAsyncTask.execute();
        }
    }


    /***
     * 判断系统是否保存当前wifi的信息
     * @param SSID
     * @return
     */
    private WifiConfiguration isExits(String SSID){
        //得到配置好的网络
        @SuppressLint("MissingPermission")
        List<WifiConfiguration> existConfigs = wifiManager.getConfiguredNetworks();
        for(WifiConfiguration wifiConfiguration : existConfigs){
            if(wifiConfiguration.SSID.equals("\"" + SSID + "\"")){
                Log.i("WLAN", "wifiConfiguration.SSID---" + " " + wifiConfiguration.SSID);
                return wifiConfiguration;
            }
        }
        return null;
    }


    /**
     * 判断当前是否已经连接
     * @param ssid
     * @return
     */
    private boolean isGivenWifiConnected(String ssid){
        return WifiCtrlManager.getInstance(mContext).isWifiConnected()
                && WifiCtrlManager.getInstance(mContext).getCurrentWifiSSID().equals(ssid);

    }

    private boolean isGivenWifiSaved(String ssid){
        @SuppressLint("MissingPermission")
        List<WifiConfiguration> existConfigs = wifiManager.getConfiguredNetworks();
        for(WifiConfiguration wifiConfiguration : existConfigs){
            if(WifiCtrlManager.getInstance(mContext).getCurrentWifiSSID(wifiConfiguration.SSID)
                    .equals(ssid) && wifiConfiguration.status != 0){
                return true;
            }
        }
        return false;
    }



    private String getSecurityModeString(ScanResult scanResult){
        if (scanResult.capabilities.contains("WEP")) {
            return "WEP";
        } else if (scanResult.capabilities.contains("WPA-PSK") && !scanResult.capabilities.contains("WPA2-PSK")) {
            return "WPA-PSK";
        } else if (!scanResult.capabilities.contains("WPA-PSK") && scanResult.capabilities.contains("WPA2-PSK")) {
            return "WPA2-PSK";
        } else if(scanResult.capabilities.contains("WPA-PSK") && scanResult.capabilities.contains("WPA2-PSK")){
            return "WPA/WPA2-PSK";
        } else if(scanResult.capabilities.contains("EAP")){
            return "WPA_EAP";
        }
        return "None";
    }


    private SecurityMode getSecurityMode(ScanResult scanResult){
        if (scanResult.capabilities.contains("WEP")) {
            return SecurityMode.WEP;
        } else if (scanResult.capabilities.contains("PSK")) {
            return SecurityMode.WPA;//WPA/WPA2 PSK
        } else if (scanResult.capabilities.contains("EAP")) {
            return SecurityMode.WPA2;
        }
        return SecurityMode.OPEN;
    }

    @Override
    public void onNetWorkChange(boolean isConnected, int type) {
        /*if(isConnected){
            wifiImg.setVisibility(View.VISIBLE);
        }else{
            wifiImg.setVisibility(View.INVISIBLE);
        }*/
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.wlan_state_layout){
            if(isWifiOpen){
                //wifi是开的点击则关闭
                isWifiOpen = false;
                openWlanCk.setImageResource(R.drawable.icon_wifi_off);
                //背景颜色变白色
                ctrlWifiLayout.setBackgroundColor(getActivity().getResources().getColor(R.color.white));
                WifiCtrlManager.getInstance(mContext).closeWifi();
                if(null != mUIHandler){
                    mUIHandler.sendEmptyMessage(MSG_CLOSE_WIFI_STATE);
                }
            }else {
                //wifi是关闭的点击则开启
                isWifiOpen = true;
                openWlanCk.setImageResource(R.drawable.icon_wifi_on);
                ctrlWifiLayout.setBackgroundColor(getActivity().getResources()
                        .getColor(R.color.color_open_wifi));
                //背景颜色变淡蓝色
                WifiCtrlManager.getInstance(mContext).openWifi();
                WifiCtrlManager.getInstance(mContext).startScan();
                if(null != mUIHandler){
                    mUIHandler.sendEmptyMessage(MSG_OPEN_WIFI_STATE);
                }
            }
        }
    }

    /**
     * 这个枚举用于表示网络加密模式
     */
    public enum SecurityMode {
        OPEN, WEP, WPA, WPA2
    }
    /**
     * 生成wifi配置文件
     * @param SSID:wifi账号
     * @param pwd 输入的wifi密码
     * @param securityMode 加密模式
     */
    private WifiConfiguration createWifiConfiguration(String SSID, String pwd, SecurityMode securityMode){

        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        //支持这个配置身份证协议
        wifiConfiguration.allowedAuthAlgorithms.clear();
        //组密码支持这个配置的设置
        wifiConfiguration.allowedGroupCiphers.clear();
        //支持组密码管理协议
        wifiConfiguration.allowedKeyManagement.clear();
        //一组两两对WPA密码呗该配置支持
        wifiConfiguration.allowedPairwiseCiphers.clear();
        //支持安全协议
        wifiConfiguration.allowedProtocols.clear();
        wifiConfiguration.SSID = "\"" + SSID + "\"";
        if(securityMode == SecurityMode.OPEN){
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        }else if(securityMode == SecurityMode.WEP){
            wifiConfiguration.hiddenSSID = true;
            wifiConfiguration.wepKeys[0] = "\"" + pwd + "\"";
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfiguration.wepTxKeyIndex = 0;
        } else if(securityMode == SecurityMode.WPA){
            wifiConfiguration.preSharedKey = "\"" + pwd + "\"";
            wifiConfiguration.hiddenSSID = true;
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
        }
        return wifiConfiguration;
    }
/**********************************************************************************************************/

    /**弹出连接WiFi的dialog**/
    private void showConnectWifiDialog(final ScanResult scanResult){
        if(null != connectDialog){
            connectDialog.dismiss();
            connectDialog = null;
        }
        if(null == connectDialog){
            connectDialog = new WifiNoticeDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_connect_wifi, null);
            TextView ssidTv = (TextView)view.findViewById(R.id.dialog_wifi_ssid_tv);
            final String ssid = scanResult.SSID;
            final EditText mPwdEt = (EditText)view.findViewById(R.id.dialog_wifi_pwd_et);
            CheckBox pwdVisibleCk = (CheckBox)view.findViewById(R.id.dialog_show_pwd_ck);
            WifiNoticeDialog.Builder builder = new WifiNoticeDialog.Builder(getActivity());
            builder.setContentView(view);
            ssidTv.setText(ssid);
            pwdVisibleCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isCheck) {
                    // TODO Auto-generated method stub
                    if(isCheck){
                        //显示
                        mPwdEt.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                        mPwdEt.setSelection(mPwdEt.getText().toString().length());
                    }else{
                        mPwdEt.setTransformationMethod(PasswordTransformationMethod.getInstance());
                        mPwdEt.setSelection(mPwdEt.getText().toString().length());
                    }
                }
            });

            builder.setConnectButton(R.string.dialog_connect, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    String pwd = mPwdEt.getText().toString().trim();
                    if(pwd.equals("")){
                        displayToast(R.string.toast_wifi_null);
                        return;
                    }
                    if(pwd.length() < 8){
                        displayToast(R.string.toast_wifi_length);
                        return;
                    }
                    SecurityMode mode = getSecurityMode(scanResult);
                    Log.i("WLAN", "--mode---" + mode);
                    //如果其他的在连接应该断开连接
                    WifiConfiguration wifiConfiguration = createWifiConfiguration(ssid, pwd, mode);
                    connectWifiByPwd(wifiConfiguration);
                    connecStatus(true);
                    connectDialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    connectDialog.dismiss();
                    connectDialog = null;
                }
            });
            connectDialog = builder.create();
            connectDialog.show();
        }
    }


    /**断开WiFi的dialog
     * 正在连接的有forget 和 cancel状态
     * **/
    private void showForgetWifiDialog(final MScanWifi scanWifi){
        if(null != forgetDialog){
            forgetDialog.dismiss();
            forgetDialog = null;
        }

        if(null == forgetDialog){
            forgetDialog = new WifiNoticeDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_message_disconnect_wifi, null);
            TextView ssidTv = (TextView)view.findViewById(R.id.dialog_wifi_ssid_tv);
            TextView statusTv = (TextView)view.findViewById(R.id.dialog_wifi_status_txt);
            TextView signalTv = (TextView)view.findViewById(R.id.dialog_wifi_signal_strength_txt);
            TextView securityTv = (TextView)view.findViewById(R.id.dialog_wifi_security_txt);
            WifiNoticeDialog.Builder builder = new WifiNoticeDialog.Builder(getActivity());
            builder.setContentView(view);
            ssidTv.setText(scanWifi.getScanResult().SSID);
            statusTv.setText(mContext.getString(R.string.wlan_connect));
            signalTv.setText(scanWifi.getWifiLevel()+"");
            securityTv.setText(scanWifi.getSecurityMode() + "");
            builder.setForgetButton(R.string.dialog_forget, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    //	断开当前wifi
                    int networkId = WifiCtrlManager.getInstance(mContext).getNetworkId();
                    boolean isForget = WifiCtrlManager.getInstance(mContext).forgetWifi(scanWifi.getScanResult().SSID);
                    if(isForget){
                        mUIHandler.sendEmptyMessage(MSG_UPDATE_WIFILIST);
                    }
                    forgetDialog.dismiss();
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    forgetDialog.dismiss();
                    forgetDialog = null;
                }
            });

            forgetDialog = builder.create();
            forgetDialog.show();
        }
    }

    /****
     * 已经连接过的只有forget、connect、cancel状态
     */
    private void showReconnectDialog(final ScanResult scanResult,final WifiConfiguration wifiConfiguration){
        if(null != reconnectDialog){
            reconnectDialog.dismiss();
            reconnectDialog = null;
        }
        if(null == reconnectDialog){
            reconnectDialog = new WifiNoticeDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_message_reconnect_wifi, null);
            TextView ssidTv = (TextView)view.findViewById(R.id.dialog_wifi_ssid_tv);
            WifiNoticeDialog.Builder builder = new WifiNoticeDialog.Builder(getActivity());
            builder.setContentView(view);
            ssidTv.setText(scanResult.SSID);
            builder.setForgetButton(R.string.dialog_forget, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    //	断开当前wifi
                    //int networkId = getNetworkId();
                    Log.i("WLAN", "showReconnectDialog--forget---scanResult.SSID--" + scanResult.SSID);
                    boolean isForget = WifiCtrlManager.getInstance(mContext).forgetWifi(scanResult.SSID);
                    if(isForget){
                        mUIHandler.sendEmptyMessage(MSG_UPDATE_WIFILIST);
                    }
                    reconnectDialog.dismiss();
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    reconnectDialog.dismiss();
                    reconnectDialog = null;
                }
            });
            builder.setConnectButton(R.string.dialog_connect, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    connecStatus(true);
                    connectWifiByPwd(wifiConfiguration);
                    reconnectDialog.dismiss();
                }
            });

            reconnectDialog = builder.create();
            reconnectDialog.show();
        }
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////
    class ConnectWifiAsyncTask  extends AsyncTask<Void,Void, Boolean> {

        private WifiConfiguration mWifiConfiguration;
        public ConnectWifiAsyncTask(WifiConfiguration wifiConfiguration){
            this.mWifiConfiguration = wifiConfiguration;
        }


        @Override
        protected Boolean doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            boolean result = false;
            int networkId = wifiManager.addNetwork(mWifiConfiguration);
            result = wifiManager.enableNetwork(networkId, true);
            Log.i("WLAN", "doInBackground Wifi--netId---" + networkId);
            return result;
        }

        @Override
        protected void onPostExecute(Boolean Boolean) {
            // TODO Auto-generated method stub
            super.onPostExecute(Boolean);
            if(null != connectWifiAsyncTask){
                connectWifiAsyncTask.cancel(true);
                connectWifiAsyncTask = null;
            }
        }
    }


    public static class UIHandler extends Handler {

        WeakReference<WlanFragment> weakReference;

        public UIHandler(WlanFragment mFragment){
            weakReference = new WeakReference<WlanFragment>(mFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            final WlanFragment mFragment = weakReference.get();
            if(null == mFragment){
                return;
            }
            switch (msg.what) {
                case MSG_OPEN_WIFI_STATE:
                    mFragment.mWifiStatusTxt.setText(R.string.wlan_open);
                    break;
                case MSG_CLOSE_WIFI_STATE:
                    mFragment.mWifiStatusTxt.setText(R.string.wlan_close);
                    break;
                case MSG_WIFI_SCANRESULT:
                    mFragment.wifiConnectAdapter.setWifiList(mFragment.mWifiScanList);
                    mFragment.wifiConnectAdapter.notifyDataSetChanged();
                    break;

                case Constants.MSG_WIFI_CONNECT_STATE:

                    NetworkInfo networkInfo = (NetworkInfo)msg.obj;
                    if(networkInfo.getState().equals(NetworkInfo.State.CONNECTED)){
                        mFragment.connecStatus(false);
                    }else if(networkInfo.getState().equals(NetworkInfo.State.DISCONNECTED)){
                        Log.i("WLAN", "UIHandler---disconnected" + networkInfo.getState());
                    }
                    mFragment.wifiConnectAdapter.setWifiList(mFragment.mWifiScanList);
                    mFragment.wifiConnectAdapter.notifyDataSetChanged();
                    break;

                case MSG_UPDATE_WIFILIST:
                    WifiCtrlManager.getInstance(mFragment.getActivity().getApplicationContext()).startScan();
                    break;

                case Constants.MSG_WIFI_TURN_OFF:
                    mFragment.wifiConnectAdapter.emptyWifiList();
                    mFragment.seachingTxt.setText(R.string.wlan_turn_off_status_hint);
                    mFragment.seachingTxt.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
        }


    }
}
