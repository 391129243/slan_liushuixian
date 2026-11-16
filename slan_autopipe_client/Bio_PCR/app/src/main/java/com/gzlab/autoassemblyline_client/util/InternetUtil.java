package com.gzlab.autoassemblyline_client.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternetUtil {
	public static interface Type{
		String _WIFI = "WIFI";
		String _GPRS = "GPRS";
		String _CMWAP = "CMWAP";
	}
	
	/**
	 * Determine whether the network is connected
	 * @param context
	 * @return
	 */
	public static boolean isConnect(Context context) {
		try {
			ConnectivityManager connectivity = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity != null) {
				
				NetworkInfo info = connectivity.getActiveNetworkInfo();
				return  info != null && info.isConnected();

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 
	 * @param mContext
	 * @return
	 */
	public static String getNetType(Context mContext){
	      ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);  
    	  State stategprs = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();  
    	  State statewifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
    	  if(State.CONNECTED==statewifi && State.CONNECTED==stategprs){
    		  return Type._WIFI;
    	  }
    	  if(State.CONNECTED!=stategprs  && State.CONNECTED==statewifi){
    		  return Type._WIFI;
    	  }
    	  if(State.CONNECTED!=statewifi  && State.CONNECTED==stategprs){
    		  Uri PREFERRED_APN_URI = Uri.parse("content://telephony/carriers/preferapn");
        	  try {
        		  ContentResolver cr = mContext.getContentResolver();
        	      Cursor cursor = cr.query(PREFERRED_APN_URI,
        	      new String[] { "_id", "apn", "type" }, null, null, null);
        	      cursor.moveToFirst();
        	      if (cursor.isAfterLast()) {
        	    	  return Type._GPRS;
        	      }
        	      String apn = cursor.getString(1);
        	      if (apn.toUpperCase().equals("CMWAP")){
        	    	  return Type._CMWAP;
        	      }else if (apn.toUpperCase().equals("CMNET")){
        	    	  return Type._GPRS;
        	      }else{
        	    	  return Type._GPRS;
        	      }
       	     } catch (Exception ep) {
       	    	 ep.printStackTrace();
       	     }
    	  }
    	  return Type._GPRS;
	}
	

	
	/**
	 * @param mContext
	 * @return
	 */
	public static boolean checkSoftStage(Context mContext){
		if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){  
			return true;
		}else{
			Toast.makeText(mContext, "----", Toast.LENGTH_LONG).show();
			return false;
		}
	}
	
	/**
	 * ���URL��ַ�Ƿ���Ч
	 * @param url
	 * @return
	 */
	public static boolean checkURL(String url){
		try {
			URL mURL = new URL(url);
			HttpURLConnection urlConn = (HttpURLConnection)mURL.openConnection();
			urlConn.connect();
			if(urlConn.getResponseCode() == 200){
				return true;
			}
        } catch (Exception e) {
        	e.printStackTrace();
        	return false;
		}
       return false;
	}
	

	public static String isNaN(String msg){
		Pattern pattern = Pattern.compile("^[+-]?\\d*[.]?\\d*$");  
		Matcher isNum = pattern.matcher(msg);  
		if(isNum.matches()){  
			return Double.parseDouble(msg)+"";
		}else{
			return "no";
		}
	}

	/**
	 * 判断字符串是否为IP地址
	 * 判断该字符长度是否是7-15位之间（0.0.0.0-255.255.255.255.255）
	 * 是否能以小数点分成四段
	 * 每段是否都是数字
	 * 每段数字是否都在0-255之间
	 * @param ipString 输入的字符串
	 * @return
	 */
	public static boolean isIpAddress(String ipString){
		String ipRegex = "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}";	//IP地址的正则表达式
		//如果前三项判断都满足，就判断每段数字是否都位于0-255之间
		if (ipString.matches(ipRegex)) {
			String[] ipArray = ipString.split("\\.");
			for (int i = 0; i < ipArray.length; i++) {
				int number = Integer.parseInt(ipArray[i]);
				//4.判断每段数字是否都在0-255之间
				if (number <0||number>255) {
					return false;
				}
			}
			return true;
		}
		else {
			return false;	//如果与正则表达式不匹配，则返回false
		}
	}

	/**
	 * 判断是否为正确的端口
	 * 0-65535
	 * @param portStr
	 * @return
	 */

	public static boolean isValidPort(String portStr){
		return portStr.matches("\\d+")
				&& Integer.parseInt(portStr) >= 0
				&& Integer.parseInt(portStr) <= 65535;
	}

	public boolean isIpAddress_1(String ipString){
		//1、判断是否是7-15位之间（0.0.0.0-255.255.255.255.255）
		if (ipString.length()<7||ipString.length()>15) {
			return false;
		}
		//2、判断是否能以小数点分成四段
		String[] ipArray = ipString.split("\\.");
		if (ipArray.length != 4) {
			return false;
		}
		for (int i = 0; i < ipArray.length; i++) {
			//3、判断每段是否都是数字
			try {
				int number = Integer.parseInt(ipArray[i]);
				//4.判断每段数字是否都在0-255之间
				if (number <0||number>255) {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		}
		return true;
	}
	
	public static String getLocalIpAddress(Context context){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // 获取32位整型IP地址
        int ipAddress = wifiInfo.getIpAddress();
        
        //返回整型地址转换成“*.*.*.*”地址
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }
	
    public static SecurityMode getSecurityMode(ScanResult scanResult){
        if (scanResult.capabilities.contains("WEP")) {
            return SecurityMode.WEP;
        } else if (scanResult.capabilities.contains("PSK")) {
            return SecurityMode.WPA;//WPA/WPA2 PSK
        } else if (scanResult.capabilities.contains("EAP")) {
            return SecurityMode.WPA2;
        }
        return SecurityMode.OPEN;
    }
    /**
     * 这个枚举用于表示网络加密模式
     */
    public enum SecurityMode {
        OPEN, WEP, WPA, WPA2
    }

}
