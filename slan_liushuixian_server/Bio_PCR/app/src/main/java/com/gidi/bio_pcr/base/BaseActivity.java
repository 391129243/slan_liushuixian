package com.gidi.bio_pcr.base;

import java.io.File;
import java.util.List;
import java.util.Locale;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexUtils;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.mgr.AlarmUtils;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.LocaleUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.ProgressDialogUtil;
import com.gidi.bio_pcr.util.SolftInputUtil;
import com.gidi.bio_pcr.view.WarnDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;


public abstract class BaseActivity extends Activity {

	private static final String TAG = "BaseActivity";

	protected abstract int getLayoutId();
	//抽象函数用于继承
	protected abstract void initViews(Bundle savedInstanceState);	
	
    protected abstract void initListener();
    
	protected abstract void initData();
	private Dialog mActionProgressDlg;
	public WarnDialog mNoticeDialog;
	public WarnDialog mConfirmDialog;
	public Context mContext;
	public Context primaryBaseActivity;//THIS WILL KEEP ORIGINAL INSTANCE
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
		initViews(savedInstanceState);
		initListener();
		mContext = this;
		HexUtils.checkSetting(mContext);

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(null != mActionProgressDlg){
			mActionProgressDlg.dismiss();
			mActionProgressDlg = null;
		}
		if(null != mNoticeDialog){
			mNoticeDialog.dismiss();
			mNoticeDialog = null;
		}

		if(null != loadingDialog){
			loadingDialog.dismiss();
			loadingDialog = null;
		}
		primaryBaseActivity = null;
		mContext = null;
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

	public boolean startAysnTask(File file,int stepIndex){
		List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(file);
		if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
			//ToastUtils.showToast(getApplicationContext(),"RNAAndPCRAsyncTask is running, please wait");
			Toast.makeText(this.getApplicationContext(),
					"RNAAndPCRAsyncTask is running, please wait",Toast.LENGTH_LONG).show();
			return false;
		}
		if(!file.exists()){
		//	ToastUtils.showToast(getApplicationContext(),"template not exist, please check");
			Toast.makeText(this.getApplicationContext(),
					"template not exist, please check",Toast.LENGTH_LONG).show();
			return false;
		}
		if(HexDebugEntityList == null || HexDebugEntityList.isEmpty()){
			//ToastUtils.showToast(getApplicationContext(),"template error, please check");
			Toast.makeText(this.getApplicationContext(),
					"template error, please check",Toast.LENGTH_LONG).show();
			return false;
		}
		RNAAndPCRAsyncTask.current_template = file.getName();
		RNAAndPCRAsyncTask.current_template_phase = 0;
		RNAAndPCRAsyncTask.current_template_phase_step = stepIndex - 1 ;
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

	public void stopRNARunning(){
		Log.i("RNA", "---设备停止");
		if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
			RNAAndPCRAsyncTask.getInstance().setIsinteruppt(true);
			RNAAndPCRAsyncTask.getInstance().setmIsrunning(false);
		}
	}



	/**
	 * 显示进度
	 * @param title
	 * @param message
	 */
	public void showProgressDialog(String title, String message){
		if(null == mActionProgressDlg){
			mActionProgressDlg = ProgressDialogUtil.createLoadingDialog(this, message);
			mActionProgressDlg.setCancelable(false);
			mActionProgressDlg.setCanceledOnTouchOutside(false);
			mActionProgressDlg.show();
		}
	}

	/*
	 * 隐藏提示加载
	 */
	public void hideProgressDialog() {
		if (mActionProgressDlg != null && mActionProgressDlg.isShowing()) {
			mActionProgressDlg.dismiss();
			mActionProgressDlg = null;
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

	/**
	 * 复位
	 */
	public void resetPcrRecMark(){
		PreferenceUtil.getInstance(this.getApplicationContext())
				.setValueByName(SharedConstant.RUNNING_TEMPLATE,"");
		PreferenceUtil.getInstance(this.getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE, "");
		PreferenceUtil.getInstance(this.getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE, 0);
		PreferenceUtil.getInstance(this.getApplicationContext())
				.setValueByName(SharedConstant.PCR_RUNNING_TEMPLATE_PHASE_STEP, 0);
	}

	public void showNoticeDialog(String title, String message){
		if(null != mNoticeDialog){
			mNoticeDialog.dismiss();
			mNoticeDialog = null;
		}
		if(null == mNoticeDialog){
			mNoticeDialog = new WarnDialog(this);
			LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View view = (View)mInflater.inflate(R.layout.dialog_common_hint_layout, null);
			TextView mHintTxt = (TextView)view.findViewById(R.id.common_dialog_hint_txt);
			mHintTxt.setText(message);
			WarnDialog.Builder builder = new WarnDialog.Builder(this);
			builder.setContentView(view);
			builder.setTitle(title);
			builder.setSingleButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// TODO Auto-generated method stub
					clearAlarm();
					mNoticeDialog.dismiss();
					mNoticeDialog = null;
				}
			});

			mNoticeDialog = builder.create();
			mNoticeDialog.show();
		}
	}


	/**
	 * 开舱门
	 */
	protected void openDoor(){
		if(PcrApplication.is_RNA_connect){
			String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "96-opendoor.hex";
			File downandopenfile = new File(downandopen_filePath);
			boolean result = startAysnTask(downandopenfile);
			if(result){
				showProgressDialog(getResources().getString(R.string.dialog_title_wait)
						,getResources().getString(R.string.dialog_message_opendoor));
			}else{
				showAlarm();
				showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
						getResources().getString(R.string.dialog_message_open_error));
			}

		}else{
			//ToastUtils.showToast(this, "RNA串口断连");
			Toast.makeText(this.getApplicationContext(),
					"RNA串口断连",Toast.LENGTH_LONG).show();
		}
	}


	/**
	 * 开舱门
	 */
	protected void closeDoor(){
		if(PcrApplication.is_RNA_connect){
			String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "98-closedoor.hex";
			File downandopenfile = new File(downandopen_filePath);
			boolean result = startAysnTask(downandopenfile);
			if(result){
				showProgressDialog(getResources().getString(R.string.dialog_title_wait)
						,getResources().getString(R.string.dialog_message_closedoor));
			}else{
				showAlarm();
				showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
						getResources().getString(R.string.dialog_message_close_error));
			}

		}else{
			Toast.makeText(this.getApplicationContext(),
					"RNA串口断连",Toast.LENGTH_LONG).show();
		}
	}

	protected void downandopen(){
		if(PcrApplication.is_RNA_connect){
			String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "97-downandopen.hex";
			File downandopenfile = new File(downandopen_filePath);
			boolean result = startAysnTask(downandopenfile);
			if(result){
				showProgressDialog(getResources().getString(R.string.dialog_title_wait)
						,getResources().getString(R.string.dialog_message_downandopen_ing));
			}else{
				showAlarm();
				showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
						getResources().getString(R.string.dialog_message_downandopen));
			}

		}else{
			//ToastUtils.showToast(this, "RNA串口断连");
			Toast.makeText(this.getApplicationContext(),
					"RNA串口断连",Toast.LENGTH_LONG).show();
		}
	}

	protected void finishCMD(){
		if(PcrApplication.is_RNA_connect){
			String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "100-finishcmd.hex";
			File downandopenfile = new File(downandopen_filePath);
			boolean result = startAysnTask(downandopenfile);
			if(result){
				showProgressDialog(getResources().getString(R.string.dialog_title_wait)
						,getResources().getString(R.string.dialog_message_finishcmd));
			}else{
				showAlarm();
				showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
						getResources().getString(R.string.dialog_message_finishcmd));
			}

		}else{
			//ToastUtils.showToast(this, "RNA串口断连");
			Toast.makeText(this.getApplicationContext(),
					"RNA串口断连",Toast.LENGTH_LONG).show();
		}
	}

	///////下位机串口操作


	///////识别CH340芯片的USB驱动


	@Override
	public boolean onTouchEvent(MotionEvent event) {
		hideSolftKeyboard(event);
		return super.onTouchEvent(event);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		hideSolftKeyboard(event);
		return super.dispatchTouchEvent(event);
	}


	/**
	 * 隐藏软键盘
	 */
	protected void hideSolftKeyboard(MotionEvent event){
		try{
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				View focusView = getCurrentFocus();
				//除了点击EidtText自身区域的其他任何区域，都将键盘收起
				if(null != focusView && null != focusView.getWindowToken()
						&& !SolftInputUtil.isTouchView(event, focusView)){
					SolftInputUtil.hideSoftInputFromWindow(this);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	private LoadingDialog loadingDialog;
	public void showLoadingDialog(String message){
		if(null == loadingDialog){
			loadingDialog = new LoadingDialog(this, message);
			loadingDialog.setCancelable(false);
			loadingDialog.setCanceledOnTouchOutside(false);
			loadingDialog.show();
		}
	}

	/*
	 * 隐藏提示加载
	 */
	public void hideLoadingDialog() {
		if (loadingDialog != null && loadingDialog.isShowing()) {
			loadingDialog.dismiss();
			loadingDialog = null;
		}
	}

	public int getClickedPosition(String name,List<String> list){
		if(list != null && list.size()>0){
			for(int i=0;i<list.size();i++){
				if(list.get(i).equals(name)){
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	protected void attachBaseContext(Context newBase) {
		primaryBaseActivity = newBase;
		Locale newLocale = LocaleUtils.getUserLocale(newBase);
		Log.i("BaseActivity","attachBaseContext" + newLocale);
		Context context = MyContextWrapper.wrap(newBase,newLocale);
		super.attachBaseContext(context);
	}
}
