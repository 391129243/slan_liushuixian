package com.gzlab.autoassemblyline_client.receiver;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.gzlab.autoassemblyline_client.MainActivity;
import com.gzlab.autoassemblyline_client.PcrApplication;

public class BootUpReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.e("WSSClient", "--BootUpReceiver onCreate111--" + intent.getAction());
		if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){

			restoreVariables(context);
			Intent lancherIntent = new Intent(context, MainActivity.class);
			lancherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(lancherIntent);

		}
		/*MessageEvent msgEvent = new MessageEvent();
		msgEvent.setMsgType(MsgType.MSG_BOOT_UP);
		EventBus.getDefault().post(msgEvent);
		PreferenceUtil.getInstance(context.getApplicationContext()).setValueByName(SharedConstant.IS_BOOT_UP_NEED_RESET, true);*/
	}

	public void restoreVariables(Context context){

	}
}
