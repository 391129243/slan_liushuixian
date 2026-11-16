package com.gzlab.autoassemblyline_client.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.gzlab.autoassemblyline_client.listener.WifiReceiverActionListener;
import com.gzlab.autoassemblyline_client.mgr.WifiCtrlManager;

import java.util.List;

/***
 * 接收wifi开关状态
 * 接收wifi连接的广播
 * @author 80657
 *
 */
public class WifiReceiver extends BroadcastReceiver {

	private WifiReceiverActionListener wifiReceiverActionListener;


	
	public WifiReceiver(WifiReceiverActionListener listener){
		this.wifiReceiverActionListener = listener;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		String action = intent.getAction();
		if(null == wifiReceiverActionListener){
			return;
		}
		if(action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
			//wifi开关变化通知
			handlerWifiState(intent);
			
		}else if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
			
			handlerConnectState(intent);
			
		}else if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
			//wifi扫描结果通知
			Log.i("WLAN", "handlerWifiScanResult---");
			handlerWifiScanResult(context.getApplicationContext());
		}
	}

	private void handlerWifiState(Intent intent){
		   int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
		   switch (wifiState) {
		   //wifi 已经打开
		   case WifiManager.WIFI_STATE_ENABLED:
			   if(null != wifiReceiverActionListener){
				   wifiReceiverActionListener.onWifiOpened();
			   }
			   break;
	       //wifi 正在打开
		   case WifiManager.WIFI_STATE_ENABLING:
			   if(null != wifiReceiverActionListener){
				   wifiReceiverActionListener.onWifiOpening();
			   }
			   break;
		   case WifiManager.WIFI_STATE_DISABLED:
			   if(null != wifiReceiverActionListener){
				   wifiReceiverActionListener.onWifiClosed();
			   }
			   break;
		   case WifiManager.WIFI_STATE_DISABLING:
			   if(null != wifiReceiverActionListener){
				   wifiReceiverActionListener.onWifiClosing();
			   }
			   break;
           case WifiManager.WIFI_STATE_UNKNOWN:
               //未知状态..
               break;
			default:
				break;
		}
	}
	
	/**处理wifi连接发生改变的状态**/
	private void handlerConnectState(Intent intent){
		NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);		 
		
		if(null != networkInfo && networkInfo.isConnected()){
            //连接上了,就把wifi的信息传出去
			
            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (wifiInfo != null) {
                //把结果回传出去
            	wifiReceiverActionListener.onWifiConnected(wifiInfo,networkInfo);
            }
		}
	}
	
	private void  handlerWifiScanResult(Context context){
		List<ScanResult> list = getScanResult(context);
		Log.i("WLAN", "getScanResult---" + list.size());
		if(null != wifiReceiverActionListener){
			wifiReceiverActionListener.onWifiResultBack(list);
		}
	}
	

	private List<ScanResult> getScanResult(Context context){

		return WifiCtrlManager.getInstance(context).getAroundWifis();
	}
	


}
