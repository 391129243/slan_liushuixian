package com.gidi.bio_pcr.mgr;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.gidi.bio_pcr.listener.OnWifiConnectListener;

import java.util.ArrayList;
import java.util.List;

/***
 * 负责wifi的扫描、连接、判断wifi是否可用
 * @author 80657
 *
 */
public class WifiCtrlManager {
	private static final String TAG = "WifiCtrlManager";
	
	private WifiManager wifiManager;
	private ConnectivityManager connectivityManager;
	private OnWifiConnectListener onWifiConnectListener;
	
	private static WifiCtrlManager mInstance = null;

	public WifiCtrlManager(Context context){
		wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
	}
	
	public static WifiCtrlManager getInstance(Context context){
		if(null == mInstance){
			synchronized (WifiCtrlManager.class) {
				if(null == mInstance){
					mInstance = new WifiCtrlManager(context);
				}
			}
		}
		return mInstance;
	}

	/**scan all wifi**/
	public void scanWifi(){
		if(!isEnableWifi()){
			return;
		}
		wifiManager.startScan();
	}

	/***
	 * open start scan wifi
	 * @return
	 */
	public boolean startScan(){
		if(null != wifiManager){
			return wifiManager.startScan();
		}
		return false;
	}

	/**
	 * 开启wifi功能
	 */
	public void openWifi(){
		if(null != wifiManager){
			Log.i("WLAN", "----openWifi----" + wifiManager.isWifiEnabled());
			if(!wifiManager.isWifiEnabled()){
				wifiManager.setWifiEnabled(true);
			}
		}
	}


	public void closeWifi(){
		if(wifiManager.isWifiEnabled()){
			wifiManager.setWifiEnabled(false);
		}
	}
	
	/**open or close wifi 当wifi是打开时，调用则为关闭，wifi关闭时，调用则打开**/
	
	public void openCloseWifi(){
		/**判断当前wifi的状态**/
		if(null != wifiManager){
			if(isEnableWifi()){
				wifiManager.setWifiEnabled(false);
			}else {
				wifiManager.setWifiEnabled(true);
			}
		}
	}


	/**判断wifi是否可用 true 表示wifi打开，false 表示 wifi 关闭**/
	
	public boolean isEnableWifi(){
		return wifiManager.isWifiEnabled();
	}

	/**判断当前网络是否处于WiFi连接状态**/
	public boolean isWifiConnected(){
		NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if(wifiNetworkInfo.isConnected()){
			return true;
		}else if(wifiNetworkInfo.isAvailable()){
			return false;
		}
		return false;
	}



	/**获取当前连接WiFi的信息**/
	public WifiInfo getConnectInfo(){
		try {
			return wifiManager.getConnectionInfo();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 获取当前连接的wifi的networkId
	 * @return
	 */
	public int getNetworkId(){
		int networkId = wifiManager.getConnectionInfo().getNetworkId();
		return networkId;
	}


	public String getConnectWifiSSID(){
		if(isWifiConnected()){
			return getCurrentWifiSSID(wifiManager.getConnectionInfo().getSSID());
		}else{
			return "";
		}
	}
	
	/**断开WiFi连接
	 * @param netId
	 * @return 是否断开
	 * **/
	public Boolean disconnectWifi(int netId){
		boolean isDisable = wifiManager.disableNetwork(netId);
		boolean isDisconnect = wifiManager.disconnect();
		return isDisable && isDisconnect;
	}



	/**忘记连接的wifi账号和密码***/
	public boolean forgetWifi(String scanresult_ssid){

		List<WifiConfiguration> wifiConfigs = wifiManager.getConfiguredNetworks();
		for (WifiConfiguration wifiConfig : wifiConfigs) {
			String ssid = wifiConfig.SSID;
			Log.i(TAG, "removeWifiBySsid ssid=" + ssid);
			if (ssid.equals("\"" + scanresult_ssid + "\"")) {
				Log.i(TAG, "removeWifiBySsid success, SSID = " + wifiConfig.SSID + " netId = " + String.valueOf(wifiConfig.networkId));
				wifiManager.removeNetwork(wifiConfig.networkId);
				return wifiManager.saveConfiguration();
			}
		}
		return false;
	}

	
	public List<ScanResult> getWifiScanResult(){
		return wifiManager.getScanResults();
	}
	
	/**判断配置在系统中是否存在
	 **
	 * */
	private WifiConfiguration isExits(WifiConfiguration config){
		List<WifiConfiguration> existConfigs = wifiManager.getConfiguredNetworks();
		if(null != existConfigs && existConfigs.size()>0){
			for(WifiConfiguration wificonfig :existConfigs){
				if(wificonfig.SSID.equals(config.SSID)){
					config.networkId = wificonfig.networkId;
					return config;
				}
			}
		}
		return null;
	}

	/**
	 * 获取附近的wifi
	 * @return
	 */
	public List<ScanResult> getAroundWifis(){
		List<ScanResult> scanResults = wifiManager.getScanResults();
		Log.i("WLAN", "getAroundWifis---" + scanResults.size());
		List<ScanResult> newScanResults = new ArrayList<ScanResult>();
		for(ScanResult scanResult : scanResults){
			int position = getItemPosition(newScanResults,scanResult);
			if(position != -1){
				if(newScanResults.get(position).level < scanResult.level){
					newScanResults.remove(position);
					newScanResults.add(position,scanResult);
				}
			}else{
				newScanResults.add(scanResult);
			}
		}
		return newScanResults;
	}

	/**
	 * 返回item在list中的坐标
	 */
	private int getItemPosition(List<ScanResult> list, ScanResult item){
		for(int i = 0;i<list.size();i++){
			if(item.SSID.equals(list.get(i).SSID))
				return i;
		}
		return -1;
	}
	
    /**
     * 获取NetworkId
     *
     * @param scanResult 扫描到的WIFI信息
     * @return 如果有配置信息则返回配置的networkId 如果没有配置过则返回-1
     */
	public int getNetworkIdFromConfig(ScanResult scanResult){
		String SSID = String.format("\"%s\"", scanResult.SSID);
		List<WifiConfiguration> existConfigs = wifiManager.getConfiguredNetworks();
		for (WifiConfiguration existingConfig : existConfigs) {
            if (existingConfig.SSID.equals(SSID)) {
                return existingConfig.networkId;
            }
        }
		return -1;
	}

	//得到当前连接的wifi ssid
	public String getCurrentWifiSSID(String configurationSSID){

		if (configurationSSID.substring(0, 1).equals("\"")
				&& configurationSSID.substring(configurationSSID.length() - 1).equals("\"")) {
			return configurationSSID.substring(1, configurationSSID.length() - 1);
		}

		return "";
	}


	//得到当前连接的wifi ssid
	public String getCurrentWifiSSID(){
		String ssid = wifiManager.getConnectionInfo().getSSID();
		if (ssid.substring(0, 1).equals("\"")
				&& ssid.substring(ssid.length() - 1).equals("\"")) {
			return ssid.substring(1, ssid.length() - 1);
		}
		return "";
	}



	/****
	 * 通过密码连接wifi
	 */
	private void connectWifiByPwd( ScanResult scanResult,  String pwd, OnWifiConnectListener listener){
		String SSID = scanResult.SSID;		
		//加密方式
		SecurityMode  securityMode = getSecurityMode(scanResult);
		
		//生成配置文件
		WifiConfiguration wifiConfiguration = createWifiConfiguration(SSID, pwd, securityMode);			
	}
	
	/***
	 * 通过networkI的连接到wifi
	 * @param SSID :wifi名称
	 * @param listener 连接的监听器
	 */
	
	public void connectWifiByNetWorkId(String SSID,int networkId, OnWifiConnectListener listener){
		onWifiConnectListener = listener;
		//连接开始的回调
		onWifiConnectListener.onStart(SSID);
		//判断networkid是否有效 若是-1表示参数不正确
		if(-1 != networkId){
			if(null != onWifiConnectListener){
				onWifiConnectListener.onFailure(SSID);
				onWifiConnectListener.onFinish();
				onWifiConnectListener = null;
			}
			return;
		}
		
		//获取当前的网络连接
		WifiInfo wifiInfo = getConnectInfo();
		if(null != wifiInfo){
			//断开当前的连接
			boolean isDisconnect = disconnectWifi(wifiInfo.getNetworkId());
			if(!isDisconnect){
				//断开当前网络失败
				if(null != onWifiConnectListener){
					onWifiConnectListener.onFailure(SSID);
					onWifiConnectListener.onFinish();
					onWifiConnectListener = null;
				}
				return;
			}
		}
		
		//连接wifi
		boolean isEnable = wifiManager.enableNetwork(networkId, true);
		if(!isEnable){
			//连接失败
			if(null != onWifiConnectListener){
				onWifiConnectListener.onFailure(SSID);
				onWifiConnectListener.onFinish();
				onWifiConnectListener = null;
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
     * 获取WiFi的加密方式
     */
    public SecurityMode getSecurityMode(ScanResult scanResult){
    	String capabilities =  scanResult.capabilities;
    	if(capabilities.contains("WPA")){
    		return SecurityMode.WPA;
    	} else if(capabilities.contains("WEP")){
    		return SecurityMode.WEP;
    	} else if(capabilities.contains("WPA2")){
    		return SecurityMode.WPA2;
    	} else{
        	return SecurityMode.OPEN;
    	}

    }
    
    /**
     * 生成wifi配置文件
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
}
