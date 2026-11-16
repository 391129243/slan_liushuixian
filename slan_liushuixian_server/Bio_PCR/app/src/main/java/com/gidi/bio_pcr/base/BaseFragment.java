package com.gidi.bio_pcr.base;



import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Toast;


import androidx.fragment.app.Fragment;

import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.util.LoadingDialog;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public abstract class BaseFragment extends Fragment {

	public BaseFragmentActivity mActivity;
	protected View rootView;
	
	/**获取布局文件**/
    protected abstract int getLayoutResource();
    
    protected abstract void initView(View rootView);
    
    protected abstract void initListener();

	protected void registerEventBus(){
		if (!EventBus.getDefault().isRegistered(this)) {//加上判断
			EventBus.getDefault().register(this);
		}
	}

	protected void unregisterEventBus(){
		if (EventBus.getDefault().isRegistered(this)) {//加上判断
			EventBus.getDefault().unregister(this);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
//		this.mActivity = (BaseFragmentActivity)getActivity();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if(null == rootView){
    		rootView = inflater.inflate(getLayoutResource(), container, false);
    	}
    	initView(rootView);
    	initListener();
    	rootView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				InputMethodManager manager = (InputMethodManager) getActivity()
						.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (getActivity().getCurrentFocus() != null && getActivity().getCurrentFocus().getWindowToken() != null) {
						manager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
					}
				}
				return false;
			}
		});
		return rootView;
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		mActivity = null;
		if(null != loadingDialog){
			loadingDialog.dismiss();
			loadingDialog = null;
		}
	}



	public BaseFragmentActivity getBaseActivity(){
		return mActivity;
	}
	

	
	public void displayToast(int resourceId){
		if(null != mActivity){
			Toast.makeText(mActivity.getApplicationContext(),
					mActivity.getResources().getString(resourceId),Toast.LENGTH_LONG).show();
		}
	}


	private LoadingDialog loadingDialog;
	public void showLoadingDialog(String message){
		if(null == loadingDialog){
			loadingDialog = new LoadingDialog(getActivity(), message);
			loadingDialog.setCancelable(false);
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

	public boolean startAysnTask(File file){
		List<HexDebugEntity> HexDebugEntityList =  ListDataSaveUtil.getHexDebugEntityList(file);
		if(RNAAndPCRAsyncTask.getInstance().ismIsrunning()){
			Toast.makeText(getActivity().getApplicationContext(),
					"RNAAndPCRAsyncTask is running, please wait",Toast.LENGTH_LONG).show();
			return false;
		}
		if(!file.exists()){
			Toast.makeText(getActivity().getApplicationContext(),
					"template not exist, please check",Toast.LENGTH_LONG).show();
			return false;
		}
		if(HexDebugEntityList == null || HexDebugEntityList.isEmpty()){
			Toast.makeText(getActivity().getApplicationContext(),
					"template error, please check",Toast.LENGTH_LONG).show();
			return false;
		}
		RNAAndPCRAsyncTask.current_template = file.getName();
		RNAAndPCRAsyncTask.current_template_phase = 0;
		RNAAndPCRAsyncTask.current_template_phase_step = 0;
		RNAAndPCRAsyncTask.getInstance().setmContext(getActivity().getApplicationContext());
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
}
