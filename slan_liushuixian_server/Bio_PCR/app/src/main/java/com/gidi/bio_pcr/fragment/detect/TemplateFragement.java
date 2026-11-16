package com.gidi.bio_pcr.fragment.detect;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.TemplateAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bioway.activity.BiowayActivity;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.listener.FragmentKeyeventListener;
import com.gidi.bio_pcr.mgr.ScanKeyManager;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.gidi.bio_pcr.view.LinearSpacingItemDecoration;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

/**
 * 显示所有的模板列表
 */
public class TemplateFragement extends BaseFragment implements FragmentKeyeventListener {

    private RecyclerView mTempalteView;
    private List<String> tempList;
    private TemplateAdapter mTempAdapter;
    private boolean isTempCheck = false;

    private String endDate;//失效日期
    private String serialNum; //项目号
    private CustomDialog mNoticeDialog;
    private String reagentCode = "";

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_select_template;
    }

    @Override
    protected void initView(View rootView) {

        mTempalteView = (RecyclerView)rootView.findViewById(R.id.template_list_recycleview);
        initVariable();
    }

    @Override
    protected void initListener() {
        mTempAdapter.setOnItemClickListener(new TemplateAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mTempAdapter.setSelectedPosition(position);
                mTempAdapter.setTemplateList(tempList);
                isTempCheck = true;
                String templateName = tempList.get(position);
                PcrApplication.rootTempName = templateName;

                if(isTempCheck){
                    //弹出对话框，
                    showNoticeDialog(0,getActivity().getResources().getString(R.string.dialog_title_prompt),
                            getActivity().getResources().getString(R.string.template_dialog_message_current_select_template)
                                    +"\n" +PcrApplication.rootTempName);
                }
            }
        });

    }


    /*是否是输入状态（输入时扫码监听不拦截）*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanKeyManager = new ScanKeyManager(new ScanKeyManager.OnScanValueListener() {
            @Override
            public void onScanValue(String value) {
                //获取最后两位
                if(!StringUtil.isEmpty(value)){
                    endDate = value.substring(14,20);
                    serialNum = value.substring(value.length()-2,value.length());
                    //弹出对话框
                    //获取当前的日期
                    reagentCode = value.substring(24, 32);
                    String nowDate = DateFormatUtil.getNowDate_2();
                    if(DateFormatUtil.compareDate(endDate,nowDate)){
                        //弹出确定对话框
                        String templateName = getTemplateName();
                        if(!StringUtil.isEmpty(templateName)){
                            PcrApplication.rootTempName = templateName;
                            showNoticeDialog(0,getActivity().getResources().getString(R.string.dialog_title_prompt),
                                    getActivity().getResources().getString(R.string.template_dialog_message_current_select_template)+"\n"+ PcrApplication.rootTempName);
                        }else{
                            showNoticeDialog(1,getActivity().getResources().getString(R.string.dialog_title_prompt),
                                    getActivity().getResources().getString(R.string.template_dialog_message_no_template));
                        }
                    }else {
                        //弹出过期提示对话框//提示红色
                        showNoticeDialog(2,getActivity().getResources().getString(R.string.dialog_title_prompt),
                                getActivity().getResources().getString(R.string.template_dialog_message_expired_reagent));
                    }
                }
            }
        });
    }
    private String getTemplateName(){
        if(!StringUtil.isEmpty(serialNum)){
            for(String templateName :tempList){
                if(templateName.startsWith(serialNum)){
                    return templateName;
                }
            }
        }
        return null;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != tempList){
            tempList.clear();
            tempList = null;
        }
        if(null != mNoticeDialog){
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        slanActivity = null;
        scanKeyManager = null;
    }

    private void initVariable(){
        tempList = new ArrayList<String>();
        LinearLayoutManager layoutManager = new GridLayoutManager(this.getActivity().getApplicationContext(),2);
        mTempalteView.setLayoutManager(layoutManager);
        mTempalteView.addItemDecoration(new LinearSpacingItemDecoration(getActivity(), 12));
        mTempAdapter = new TemplateAdapter(getActivity().getApplicationContext());
        mTempalteView.setAdapter(mTempAdapter);
        mTempAdapter.setTemplateList(tempList);
        mTempAdapter.notifyDataSetChanged();

    }


    public void setTemplateList(List<String> templateList){
        this.tempList = templateList;
        if(null != mTempAdapter){
            mTempAdapter.setTemplateList(this.tempList);
            mTempAdapter.notifyDataSetChanged();
        }
    }

    private BiowayActivity slanActivity;
    private ScanKeyManager scanKeyManager;
    /*是否是输入状态（输入时扫码监听不拦截）*/

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        slanActivity = (BiowayActivity)activity;
        slanActivity.setFragmentKeyeventListener(this);
    }

    @Override
    public boolean onFragmentKeyEvent(KeyEvent event) {
        //  Log.i("BBB","event.getDeviceId()" + event.getDeviceId());
        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            scanKeyManager.analysisKeyEvent(event);
            return true;
        }
        return false;
    }

    private EditText mDescriptionEt;
    private String description = "";
    public void showNoticeDialog(int dialogType,String title, String message) {
        if (null != mNoticeDialog) {
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        if (null == mNoticeDialog) {
            mNoticeDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_template_info_layout, null);
            TextView mHintTxt = (TextView) view.findViewById(R.id.common_dialog_hint_txt);
            mDescriptionEt = (EditText)view.findViewById(R.id.slan_info_discribe_et);
            mDescriptionEt.addTextChangedListener(new LimitInputTextWatcher(mDescriptionEt));
            mHintTxt.setText(message);
            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setTitle(title);
            if(dialogType == 0){
                mDescriptionEt.setVisibility(View.VISIBLE);
            }else if(dialogType == 1){
                mDescriptionEt.setVisibility(View.GONE);
            }else if(dialogType == 2){
                builder.setTitleColor(R.color.red);
                mDescriptionEt.setVisibility(View.GONE);
            }
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (dialogType){
                        case 0:
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.REAGENT_CODE,reagentCode);
                            description = mDescriptionEt.getText().toString();
                            MessageEvent messageEvent = new MessageEvent();
                            messageEvent.setMsgType(MsgType.MSG_TEMPLATE_CAPACITY_DONE);
                            messageEvent.setData(description);
                            EventBus.getDefault().post(messageEvent);
                            break;
                    }
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.setCanceledOnTouchOutside(true);
            mNoticeDialog.show();
        }
    }
}
