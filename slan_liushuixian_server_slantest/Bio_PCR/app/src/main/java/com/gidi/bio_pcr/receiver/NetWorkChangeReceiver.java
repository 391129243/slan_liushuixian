package com.gidi.bio_pcr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.gidi.bio_pcr.listener.OnNetWorkChangedListener;

import java.util.ArrayList;
import java.util.List;


public class NetWorkChangeReceiver extends BroadcastReceiver {

	public static String NETWORK_CONNECTION_CHANGE = "android.net.conn.CONNECTIVITY_CHANGE";

	private List<OnNetWorkChangedListener> mNetworkChangedListeners = new ArrayList<OnNetWorkChangedListener>();
	
	public void registerListener(OnNetWorkChangedListener listener){
		if(!mNetworkChangedListeners.contains(listener)){
			mNetworkChangedListeners.add(listener);
		}
	}
	
	public void unregisterListener(OnNetWorkChangedListener listener){
		if(mNetworkChangedListeners.contains(listener)){
			mNetworkChangedListeners.remove(listener);
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(intent.getAction().equals(NETWORK_CONNECTION_CHANGE)){
			ConnectivityManager mConnectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
			if(null != mNetworkInfo && mNetworkInfo.isAvailable()){
				if(mNetworkChangedListeners != null && mNetworkChangedListeners.size()>0){
					for(OnNetWorkChangedListener listener : mNetworkChangedListeners){
						listener.onNetWorkChange(mNetworkInfo.isConnected(), mNetworkInfo.getType());
					}
				}
			}else{				
				
				if(mNetworkChangedListeners != null && mNetworkChangedListeners.size()>0){
					for(OnNetWorkChangedListener listener : mNetworkChangedListeners){
						listener.onNetWorkChange(false, 0);
					}
				}
			}
		}	
	}	
}
