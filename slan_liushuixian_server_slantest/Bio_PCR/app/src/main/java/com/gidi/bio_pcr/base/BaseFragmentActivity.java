package com.gidi.bio_pcr.base;


import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.os.Handler;

import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.mgr.AlarmUtils;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LocaleUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class BaseFragmentActivity extends FragmentActivity {

	//抽象函数用于继承
	protected abstract int getLayoutId();
	
	protected abstract void initView();
	
	protected abstract void initListener();


	/**
	 * 子类要处理 handler的消息，重写该方法
	 */
	protected abstract void handleMessage(Message msg);
	private WeakHandler weakHandler;

	protected boolean isSecretSetting = false;

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
	public void onEvent() {

	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(getLayoutId());
		weakHandler = new WeakHandler(this);
		initView();
		initListener();
		initUpdateSysTime();

	}


	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(null != weakHandler){
			weakHandler.removeCallbacksAndMessages(null);
			weakHandler = null;
		}
		runnable = null;

	}


	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		isSecretSetting = PreferenceUtil.getInstance(getApplicationContext())
				.getBooleanValue(SharedConstant.SECRET_SETTING,false);
		if(isSecretSetting){
			RNAAndPCRAsyncTask.retryCount_Value = 100;
		}else{
			RNAAndPCRAsyncTask.retryCount_Value = 20;
		}
	}



	/**
	 * Shielding back key
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {  
        if (keyCode == KeyEvent.KEYCODE_BACK) {  
            return true;  
        }
        return false;  
    }


	//定时更新时间
	private void initUpdateSysTime() {
		weakHandler.postDelayed(runnable, 1000);
	}

	Runnable runnable = new Runnable() {
		@Override
		public void run() {
			//TODO:延时要干的其他事情
			weakHandler.sendEmptyMessage(Constants.MSG_UPDATE_SYSTEM_TIME);
			weakHandler.postDelayed(runnable, 1000);
		}
	};

	/**
	 * activity 跳转
	 *
	 * @param target      需要跳转的 Activity
	 * @param bundle      bundle
	 * @param isCloseSelf 是否关闭当前的 activity
	 */
	public void openActivity(Class<?> target, Bundle bundle, boolean isCloseSelf) {
		Intent intent = new Intent(this, target);
		if (bundle != null) {
			intent.putExtras(bundle);
		}
		startActivity(intent);
		if (isCloseSelf) {
			finish();
		}
	}

	/**
	 * activity 跳转（不需要传值）
	 *
	 * @param target 需要跳转的Activity
	 */
	public void openActivity(Class<?> target) {
		openActivity(target, null, false);
	}

	/**
	 * 判断当前线程是否在主线程
	 */



	/**
	 * 弱引用 handler
	 */
	public static class WeakHandler extends Handler {
		WeakReference weakReference;

		WeakHandler(BaseFragmentActivity activity) {
			this.weakReference = new WeakReference<>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			BaseFragmentActivity activity = (BaseFragmentActivity) weakReference.get();
			switch (msg.what) {
				default:
					if (activity != null) {
						activity.handleMessage(msg);
					}
					break;
			}
		}
	}

	public void showAlarm(){
		if(!AlarmUtils.getInstance(getApplicationContext()).isSystemAlarm()){
			AlarmUtils.getInstance(getApplicationContext()).setSystemAlarm(true);
			if(AlarmUtils.getInstance(getApplicationContext()).getPlayPosition()!= 3){
				AlarmUtils.getInstance(getApplicationContext()).startplay(3);
			}
		}
	}

	public void clearAlarm(){
		if(AlarmUtils.getInstance(getApplicationContext()).isSystemAlarm()){
			AlarmUtils.getInstance(getApplicationContext()).setSystemAlarm(false);
			AlarmUtils.getInstance(getApplicationContext()).stopPlay();
		}
	}

	public void showCountingEnd(){
		Log.i("Single","showCountingEnd");
		if(!AlarmUtils.getInstance(getApplicationContext()).isCountEnd()){
			AlarmUtils.getInstance(getApplicationContext()).setCountEnd(true);
			if(AlarmUtils.getInstance(getApplicationContext()).getPlayPosition()!= 2){
				AlarmUtils.getInstance(getApplicationContext()).startplay(2);
			}
		}
	}

	public void clearCountingEnd(){
		Log.i("Single","clearCountingEnd");
		if(AlarmUtils.getInstance(getApplicationContext()).isCountEnd()){
			AlarmUtils.getInstance(getApplicationContext()).setCountEnd(false);
			AlarmUtils.getInstance(getApplicationContext()).stopPlay();
		}
	}


	public void resetPcrRecMark(){
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE, "");
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, 0);
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE_STEP, 0);
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.EXPERIMENT_TYPE, 0);
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.EX_TOTAL_TIME,0);
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.PROCEDURE_RUNNING_TIME,0);
		PreferenceUtil.getInstance(this.getApplicationContext())
				.setValueByName(SharedConstant.RUNNING_TEMPLATE,"");
	}

	public void setPcrRecMark(int pcr_file_num,int pcr_step){
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE, PcrApplication.rootTempName);
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, pcr_file_num);
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE_STEP, pcr_step);
		PreferenceUtil.getInstance(getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_STARTTIME, DateUtil.getCurrentTimeYMDHMS());
	}


	public String getPCRFilePath(){
		String path = "";
		if(PcrApplication.rootTempName.contains("01-")){
			File file = new File(FileUtils.SECRET1_3_flu_repeat_fam_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("02-")){
			File file = new File(FileUtils.SECRET2_3_flu_repeat_rox_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("03-")){
			File file = new File(FileUtils.SECRET3_3_flu_repeat_hex_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("04-")){
			File file = new File(FileUtils.SECRET4_3_flu_repeat_cy5_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("05-")){
			File file = new File(FileUtils.SECRET5_5_sample_repeat_high_1_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("06-")){
			File file = new File(FileUtils.SECRET6_5_sample_repeat_high_2_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("07-")){
			File file = new File(FileUtils.SECRET7_5_sample_repeat_median_1_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("08-")){
			File file = new File(FileUtils.SECRET8_5_sample_repeat_median_2_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("09-")){
			File file = new File(FileUtils.SECRET9_5_sample_repeat_low_1_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("10-")){
			File file = new File(FileUtils.SECRET10_5_sample_repeat_low_2_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("11-")){
			File file = new File(FileUtils.SECRET11_6_sample_linear_AMP_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("12-")){
			File file = new File(FileUtils.SECRET12_6_sample_linear_AMP_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("13-")){
			File file = new File(FileUtils.SECRET13_3_sample_linear_AMP_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("14-")){
			File file = new File(FileUtils.SECRET14_8_flu_linear_fam_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("15-")){
			File file = new File(FileUtils.SECRET15_7_flu_linear_fam_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("16-")){
			File file = new File(FileUtils.SECRET16_8_flu_linear_rox_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("17-")){
			File file = new File(FileUtils.SECRET17_7_flu_linear_rox_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("18-")){
			File file = new File(FileUtils.SECRET18_8_flu_linear_hex_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("19-")){
			File file = new File(FileUtils.SECRET19_7_flu_linear_hex_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("20-")){
			File file = new File(FileUtils.SECRET20_8_flu_linear_cy5_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("21-")){
			File file = new File(FileUtils.SECRET21_7_flu_linear_cy5_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("22-")){
			File file = new File(FileUtils.SECRET22_5_extraction_consistence_high_1_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("23-")){
			File file = new File(FileUtils.SECRET23_5_extraction_consistence_high_2_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("24-")){
			File file = new File(FileUtils.SECRET24_5_extraction_consistence_median_1_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("25-")){
			File file = new File(FileUtils.SECRET25_5_extraction_consistence_median_2_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("26-")){
			File file = new File(FileUtils.SECRET26_5_extraction_consistence_low_1_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}else if(PcrApplication.rootTempName.contains("27-")){
			File file = new File(FileUtils.SECRET27_5_extraction_consistence_low_2_PATH);
			List<File> listFile  =  Arrays.asList(file.listFiles());
			if(listFile != null && listFile.size()!= 0){
				path = listFile.get(0).getAbsolutePath();
			}
		}
		return path;
	}


	public boolean startAysnTask(File file){
		List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(file);
		if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
			Toast.makeText(getApplicationContext(),"RNAAndPCRAsyncTask is running, please wait",Toast.LENGTH_LONG).show();
			return false;
		}
		if(!file.exists()){
			Toast.makeText(getApplicationContext(),"template not exist, please check",Toast.LENGTH_LONG).show();
			return false;
		}
		if(HexDebugEntityList == null || HexDebugEntityList.isEmpty()){
			Toast.makeText(getApplicationContext(),"template error, please check",Toast.LENGTH_LONG).show();
			return false;
		}
		RNAAndPCRAsyncTask.current_template = file.getName();
		RNAAndPCRAsyncTask.current_template_phase = 0;
		RNAAndPCRAsyncTask.current_template_phase_step = 0;
		RNAAndPCRAsyncTask.getInstance().setmContext(getApplicationContext());
		//RNAAndPCRAsyncTask.getInstance().cancel(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			//根据不同的api采用并行模式进行开启
			//////启动流程的API
			RNAAndPCRAsyncTask.getInstance().executeOnExecutor(THREAD_POOL_EXECUTOR, HexDebugEntityList);

		} else {
			RNAAndPCRAsyncTask.getInstance().execute(HexDebugEntityList);
		}
		return true;
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		Locale newLocale = LocaleUtils.getUserLocale(newBase);
		Context context = MyContextWrapper.wrap(newBase,newLocale);
		super.attachBaseContext(context);
	}
}
