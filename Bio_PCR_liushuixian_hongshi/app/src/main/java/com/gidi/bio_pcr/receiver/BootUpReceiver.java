package com.gidi.bio_pcr.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.PreferenceUtil;

import org.greenrobot.eventbus.EventBus;

public class BootUpReceiver extends BroadcastReceiver{

	private static final String ACTION_BOOTUP = "android.intent.action.BOOT_COMPLETED";


	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(intent.getAction().equals(ACTION_BOOTUP)){
			Log.i("sendSerialCmd", "--BootUpReceiver onCreate111--");
			restoreVariables(context);

		}
		MessageEvent msgEvent = new MessageEvent();
		msgEvent.setMsgType(MsgType.MSG_BOOT_UP);
		EventBus.getDefault().post(msgEvent);
		PreferenceUtil.getInstance(context.getApplicationContext()).setValueByName(SharedConstant.IS_BOOT_UP_NEED_RESET, true);
	}

	public void restoreVariables(Context context){
		if(!PcrApplication.isBootUp){
			PcrApplication.isBootUp = true;
		}
	}
}
