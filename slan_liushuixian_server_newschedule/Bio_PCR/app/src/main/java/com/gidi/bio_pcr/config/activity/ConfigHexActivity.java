package com.gidi.bio_pcr.config.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexFileName_V5;
import com.gidi.bio_pcr.WintersUtils.entity.ActionDoneMessage;
import com.gidi.bio_pcr.WintersUtils.entity.MoveActionCmd;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.WarningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.event.ProcedureFinishEvent;
import com.gidi.bio_pcr.WintersUtils.entity.event.SendCmdSuccessEvent;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexConfig;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexUtils;
import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.adapter.FileListAdapter;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.ConfigManager;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.NumKeyboardDialog;
import com.gidi.bio_pcr.view.ScollListView;
import com.winters.boweilibrary.BoWei.BoWeiService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 创建核酸提取文件
 */
public class ConfigHexActivity extends BaseActivity implements View.OnClickListener{
    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    /**
     * 左边显示的列表
     */
    private ArrayList<File> showHexFileList;
    private ListView mShowHexLv;
    private FileListAdapter<File> showHexAdapter;
    private int hex_click_pos = -1;
    private EditText mFileNameEt;//hex文件名称

    private TextView mSaveFileTv,mSaveFileTv_quick,mAllClearTv,mOperateTv,mDelFileTv,mEditFileTv;
    private RelativeLayout mOperateLayout;
    private ScrollView mConfigLayout;
    private NoticeDialog noticeDialog;
    //////////////////////////////////////////////////////////////

    private TextView mStopBtn,mResetBtn;//停止，复位
    private TextView mOpenDoorBtn,mCloseDoorBtn,mDownAndOpenBtn;//开门，关门，下机
    private TextView mRunTestBtn;//运行测试
    private TextView mFinishBtn;//结束命令
    private TextView mClearLogBtn;//清除log
    private ImageView mOperateBackImg;
    private boolean isOperate= false;

    private HexConfig hexConfig = new HexConfig();
    private LinearLayout layout_closedoor;//卡位入盒
    private LinearLayout layout_magnetic;
    private LinearLayout layout_split;
    private LinearLayout layout_wash;
    private LinearLayout layout_wash1;
    private LinearLayout layout_wash23;
    private LinearLayout layout_wash2_2;
    private LinearLayout layout_wash3_2;
    private LinearLayout layout_wash3_3;
    private LinearLayout layout_elution;
    private LinearLayout layout_quantify;

    private CheckBox checkbox_cassete;
    private CheckBox checkbox_magnetic;
    private CheckBox checkbox_split;
    private CheckBox checkbox_wash;
    private CheckBox checkbox_wash1;
    private CheckBox checkbox_wash2_2;
    private CheckBox checkbox_wash3_2;
    private CheckBox checkbox_wash3_3;
    private CheckBox checkbox_elution;
    private CheckBox checkbox_quantify;

//    private CheckBox checkbox_second;

    private RadioButton radiobutton_onestep;
    private RadioButton radiobutton_twostep;
    private RadioButton radiobutton_threestep;

    private RadioButton radiobutton_samplelive;
    private RadioButton radiobutton_splitlive;

    private TextView edittext_megneticsample;
//    private TextView edittext_splitsample;//裂解样本体积
    private TextView edittext_splitlijieye;//裂解液体积
    private TextView edittext_wash1xidiye; //步骤1洗涤液
    private TextView edittext_wash2_2xidiye;//步骤2洗涤液体积
    private TextView edittext_wash3_2xidiye;
    private TextView edittext_wash3_3xidiye;//步骤24-3洗涤液体积
    private TextView edittext_elutionxituoye;//洗脱
    private TextView edittext_quantifyvolume;//体系定量体积
    private TextView edittext_splittemperature;//裂解温度
    private TextView edittext_wash1temperature,edittext_wash2_2temperature,edittext_wash3_2temperature;
    private TextView edittext_wash3_3temperature,edittext_elutiontemperature;

    private TextView edittext_splittime,edittext_wash1time;
    private TextView edittext_wash2_2time,edittext_wash3_2time,edittext_wash3_3time,edittext_elutiontime;

    //液态体系/冻干体系
    private RadioButton mLiquidSysRb,mFreezeDrySysRb;
    private ScollListView listview_log;

    private List<String> stringList = new ArrayList<>(); //创建数据列表
    private ArrayAdapter<String> arrayAdapter;           //创建适配器

    private EditText edittext_description;//描述

    private String CONFIG_HEX_PATH = HexFileName_V5.HEXCREATEFILE_PATH;//默认是V5
    private String CONFIG_HEX_CONFIG_PATH = HexFileName_V5.HEXCONFIG_PATH;//配置目录
    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_hex_generate;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTitleLayout = (RelativeLayout)findViewById(R.id.hex_generate_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.config_hex_activity_title));
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mFileNameEt = (EditText)findViewById(R.id.hex_config_filename_et);
        mShowHexLv = (ListView)findViewById(R.id.hex_show_list) ;
        mSaveFileTv = (TextView)findViewById(R.id.hex_config_save_file_tv);
        mSaveFileTv_quick = (TextView)findViewById(R.id.hex_config_save_file_quick_tv);
        mAllClearTv = (TextView)findViewById(R.id.hex_config_clear_tv);
        mOperateTv = (TextView)findViewById(R.id.hex_config_operate_tv);
        mDelFileTv = (TextView)findViewById(R.id.hex_config_delete_tv);
        mEditFileTv = (TextView)findViewById(R.id.hex_config_edit_tv);
        mOperateLayout = (RelativeLayout)findViewById(R.id.hex_config_operate_layout);
        mConfigLayout = (ScrollView)findViewById(R.id.hex_config_set_layout);

        mRunTestBtn = (TextView)findViewById(R.id.hex_config_run_btn);
        mFinishBtn = (TextView)findViewById(R.id.hex_config_finish_btn);
        mStopBtn = (TextView) findViewById(R.id.hex_config_stop_btn);
        mResetBtn = (TextView)findViewById(R.id.hex_config_reset_btn);
        mOpenDoorBtn = (TextView)findViewById(R.id.hex_config_opendoor_btn);
        mCloseDoorBtn = (TextView)findViewById(R.id.hex_config_closedoor_btn);
        mDownAndOpenBtn =(TextView)findViewById(R.id.hex_config_downandopen_btn);
        mOperateBackImg = (ImageView)findViewById(R.id.hex_config_file_operate_back_img);
        mClearLogBtn = (TextView)findViewById(R.id.hex_config_clearlog_btn);
        mOperateBackImg.setVisibility(View.GONE);


        layout_closedoor = (LinearLayout)findViewById(R.id.layout_closedoor);
        layout_magnetic = (LinearLayout)findViewById(R.id.layout_magnetic);
        layout_split = (LinearLayout)findViewById(R.id.layout_split);
        layout_wash = (LinearLayout)findViewById(R.id.layout_wash);
        layout_wash1 = (LinearLayout)findViewById(R.id.layout_wash1);
        layout_wash23 = (LinearLayout)findViewById(R.id.layout_wash23);
        layout_wash2_2 = (LinearLayout)findViewById(R.id.layout_wash2_2);
        layout_wash3_2 = (LinearLayout)findViewById(R.id.layout_wash3_2);
        layout_wash3_3 = (LinearLayout)findViewById(R.id.layout_wash3_3);
        layout_elution = (LinearLayout)findViewById(R.id.layout_elution);
        layout_quantify = (LinearLayout)findViewById(R.id.layout_quantify);

        checkbox_cassete = (CheckBox)findViewById(R.id.checkbox_cassete);
        checkbox_magnetic = (CheckBox)findViewById(R.id.checkbox_magnetic);
        checkbox_split = (CheckBox)findViewById(R.id.checkbox_split);
        checkbox_wash = (CheckBox)findViewById(R.id.checkbox_wash);
        checkbox_wash1 = (CheckBox)findViewById(R.id.checkbox_wash1);
        checkbox_wash2_2 = (CheckBox)findViewById(R.id.checkbox_wash2_2);
        checkbox_wash3_2 = (CheckBox)findViewById(R.id.checkbox_wash3_2);
        checkbox_wash3_3 = (CheckBox)findViewById(R.id.checkbox_wash3_3);
        checkbox_elution = (CheckBox)findViewById(R.id.checkbox_elution);
        checkbox_quantify = (CheckBox)findViewById(R.id.checkbox_quantify);
//        checkbox_second = (CheckBox)findViewById(R.id.checkbox_second);


        radiobutton_onestep = (RadioButton)findViewById(R.id.radiobutton_onestep);
        radiobutton_twostep = (RadioButton)findViewById(R.id.radiobutton_twostep);
        radiobutton_threestep = (RadioButton)findViewById(R.id.radiobutton_threestep);

        radiobutton_samplelive = (RadioButton)findViewById(R.id.radiobutton_samplelive);
        radiobutton_splitlive = (RadioButton)findViewById(R.id.radiobutton_splitlive);

        edittext_megneticsample= (TextView) findViewById(R.id.edittext_megneticsample);
//        edittext_splitsample= (TextView) findViewById(R.id.edittext_splitsample);
        edittext_splitlijieye= (TextView) findViewById(R.id.edittext_splitlijieye);
        edittext_wash1xidiye= (TextView) findViewById(R.id.edittext_wash1xidiye);
        edittext_wash2_2xidiye= (TextView) findViewById(R.id.edittext_wash2_2xidiye);
        edittext_wash3_2xidiye= (TextView) findViewById(R.id.edittext_wash3_2xidiye);
        edittext_wash3_3xidiye= (TextView)findViewById(R.id.edittext_wash3_3xidiye);
        edittext_elutionxituoye= (TextView) findViewById(R.id.edittext_elutionxituoye);
        edittext_quantifyvolume= (TextView) findViewById(R.id.edittext_quantifyvolume);

        edittext_splittemperature= (TextView) findViewById(R.id.edittext_splittemperature);
        edittext_wash1temperature= (TextView) findViewById(R.id.edittext_wash1temperature);
        edittext_wash2_2temperature= (TextView) findViewById(R.id.edittext_wash2_2temperature);
        edittext_wash3_2temperature= (TextView) findViewById(R.id.edittext_wash3_2temperature);
        edittext_wash3_3temperature= (TextView) findViewById(R.id.edittext_wash3_3temperature);
        edittext_elutiontemperature= (TextView) findViewById(R.id.edittext_elutiontemperature);

        edittext_splittime= (TextView) findViewById(R.id.edittext_splittime);
        edittext_wash1time= (TextView) findViewById(R.id.edittext_wash1time);
        edittext_wash2_2time= (TextView)findViewById(R.id.edittext_wash2_2time);
        edittext_wash3_2time= (TextView) findViewById(R.id.edittext_wash3_2time);
        edittext_wash3_3time= (TextView)findViewById(R.id.edittext_wash3_3time);
        edittext_elutiontime= (TextView)findViewById(R.id.edittext_elutiontime);

        mLiquidSysRb = (RadioButton) findViewById(R.id.liquid_system_rb);
        mFreezeDrySysRb = (RadioButton)findViewById(R.id.freeze_drying_system_sb);

        edittext_description= (EditText)findViewById(R.id.edittext_description);

        //显示运行的log的dapter
        listview_log = (ScollListView)findViewById(R.id.hex_config_listview_log);
        arrayAdapter=new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,stringList);
        listview_log.setAdapter(arrayAdapter);
        listview_log.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listview_log.setStackFromBottom(true);

        showHexAdapter = new FileListAdapter<>(this.getApplicationContext());
        mShowHexLv.setAdapter(showHexAdapter);
        showHexFileList = new ArrayList<File>();
//        PcrApplication.rnaVersion = PreferenceUtil.getInstance(getApplicationContext())
//                .getStringValue(SharedConstant.RNA_SYSTEM,"V5");
        if(PcrApplication.rnaVersion.equals("V5")){
            CONFIG_HEX_PATH = HexFileName_V5.HEXCREATEFILE_PATH;
        }else{

        }
        initShowHexFile();
    }

    @Override
    protected void initListener() {
        mSaveFileTv.setOnClickListener(this);
        mSaveFileTv_quick.setOnClickListener(this);
        mAllClearTv.setOnClickListener(this);
        mOperateTv.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mResetBtn.setOnClickListener(this);
        mOpenDoorBtn.setOnClickListener(this);
        mCloseDoorBtn.setOnClickListener(this);
        mDownAndOpenBtn.setOnClickListener(this);
        mRunTestBtn.setOnClickListener(this);
        mFinishBtn.setOnClickListener(this);
        mOperateBackImg.setOnClickListener(this);
        mClearLogBtn.setOnClickListener(this);
        mEditFileTv.setOnClickListener(this);
        mDelFileTv.setOnClickListener(this);
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        //卡位入盒
        checkbox_cassete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setCasseteFlag(true);
                    layout_closedoor.setActivated(true);
                }else{
                    hexConfig.setCasseteFlag(false);
                    layout_closedoor.setActivated(false);
                }
                saveHexConfig();
            }
        });
        //磁珠活化
        checkbox_magnetic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setMagneticFlag(true);
                    layout_magnetic.setActivated(true);
                }else{
                    hexConfig.setMagneticFlag(false);
                    layout_magnetic.setActivated(false);
                }
                saveHexConfig();
            }
        });

        //裂解
        checkbox_split.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setSplitFlag(true);
                    layout_split.setActivated(true);
                }else{
                    hexConfig.setSplitFlag(false);
                    layout_split.setActivated(false);
                }
                saveHexConfig();
            }
        });


        checkbox_wash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setWashFlag(true);
                }else{
                    hexConfig.setWashFlag(false);
                }
                saveHexConfig();
            }
        });

        checkbox_wash1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setWash1Flag(true);
                    layout_wash1.setActivated(true);
                }else{
                    hexConfig.setWash1Flag(false);
                    layout_wash1.setActivated(false);
                }
                saveHexConfig();
            }
        });


        checkbox_wash2_2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setWash22Flag(true);
                    layout_wash2_2.setActivated(true);
                }else{
                    hexConfig.setWash22Flag(false);
                    layout_wash2_2.setActivated(false);
                }
                saveHexConfig();
            }
        });

        checkbox_wash3_2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setWash32Flag(true);
                    layout_wash3_2.setActivated(true);
                }else{
                    hexConfig.setWash32Flag(false);
                    layout_wash3_2.setActivated(false);
                }
                saveHexConfig();
            }
        });

        checkbox_wash3_3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setWash33Flag(true);
                    layout_closedoor.setActivated(true);
                }else{
                    hexConfig.setWash33Flag(false);
                    layout_closedoor.setActivated(false);
                }
                saveHexConfig();
            }
        });

        checkbox_elution.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setElutionFlag(true);
                    layout_elution.setActivated(true);
                }else{
                    hexConfig.setElutionFlag(false);
                    layout_elution.setActivated(false);
                }
                saveHexConfig();
            }
        });

        /**
         * 体系定量
         */
        checkbox_quantify.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setQuantifyFlag(true);
                    layout_quantify.setEnabled(true);
                }else{
                    hexConfig.setQuantifyFlag(false);
                    layout_quantify.setEnabled(false);
                }
                saveHexConfig();
            }
        });


        //多重二次试剂
//        checkbox_second.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if(isChecked){
//                    hexConfig.setSecondQuantifyFlag(true);
//                    layout_quantify.setActivated(true);
//
//                }else{
//                    hexConfig.setSecondQuantifyFlag(false);
//                    layout_quantify.setActivated(false);
//                }
//                if(hexConfig.isSecondQuantifyFlag()){
//                    hexConfig.setType(2);
//                }else{
//                    hexConfig.setType(1);
//                }
//
//                hexConfig.setCasseteFlag(!isChecked);
//                hexConfig.setMagneticFlag(!isChecked);
//                hexConfig.setSplitFlag(!isChecked);
//                hexConfig.setWashFlag(!isChecked);
//                hexConfig.setWash1Flag(!isChecked);
//                hexConfig.setWash22Flag(!isChecked);
//                hexConfig.setWash32Flag(!isChecked);
//                hexConfig.setWash33Flag(!isChecked);
//                hexConfig.setElutionFlag(!isChecked);
//                hexConfig.setQuantifyFlag(!isChecked);
//
//                checkbox_cassete.setChecked(hexConfig.isCasseteFlag());
//                checkbox_magnetic.setChecked(hexConfig.isMagneticFlag());
//                checkbox_split.setChecked(hexConfig.isSplitFlag());
//                checkbox_wash.setChecked(hexConfig.isWashFlag());
//                checkbox_wash1.setChecked(hexConfig.isWash1Flag());
//                checkbox_wash2_2.setChecked(hexConfig.isWash22Flag());
//                checkbox_wash3_2.setChecked(hexConfig.isWash32Flag());
//                checkbox_wash3_3.setChecked(hexConfig.isWash33Flag());
//                checkbox_elution.setChecked(hexConfig.isElutionFlag());
//                checkbox_quantify.setChecked(hexConfig.isQuantifyFlag());
//
//                saveHexConfig();
//            }
//        });


        radiobutton_onestep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setWashStep(1);
                    layout_wash1.setVisibility(View.VISIBLE);
                    layout_wash2_2.setVisibility(View.GONE);
                    layout_wash3_2.setVisibility(View.GONE);
                    layout_wash3_3.setVisibility(View.GONE);
                }
                saveHexConfig();
            }
        });

        radiobutton_twostep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setWashStep(2);
                    layout_wash1.setVisibility(View.VISIBLE);
                    layout_wash2_2.setVisibility(View.VISIBLE);
                    layout_wash3_2.setVisibility(View.GONE);
                    layout_wash3_3.setVisibility(View.GONE);
                }
                saveHexConfig();
            }
        });

        radiobutton_threestep.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setWashStep(3);
                    layout_wash1.setVisibility(View.VISIBLE);
                    layout_wash2_2.setVisibility(View.GONE);
                    layout_wash3_2.setVisibility(View.VISIBLE);
                    layout_wash3_3.setVisibility(View.VISIBLE);
                }
                saveHexConfig();
            }
        });



        radiobutton_samplelive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setMagneticChoice(0);
                }
                saveHexConfig();
            }
        });

        radiobutton_splitlive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    hexConfig.setMagneticChoice(1);
                }
                saveHexConfig();
            }
        });

        //液态体系
        mLiquidSysRb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    hexConfig.setFreeze_drying(0);
                }
                saveHexConfig();
            }
        });

        mFreezeDrySysRb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    hexConfig.setFreeze_drying(1);
                }
                saveHexConfig();
            }
        });

        //样本体积
        edittext_megneticsample.setOnClickListener(this);
//        edittext_splitsample.setOnClickListener(this);
        edittext_splitlijieye.setOnClickListener(this);
        edittext_wash1xidiye.setOnClickListener(this);
        edittext_wash2_2xidiye.setOnClickListener(this);
        edittext_wash3_2xidiye.setOnClickListener(this);
        edittext_wash3_3xidiye.setOnClickListener(this);
        edittext_elutionxituoye.setOnClickListener(this);
        edittext_quantifyvolume.setOnClickListener(this);

        /////温度设定
        edittext_splittemperature.setOnClickListener(this);
        edittext_wash1temperature.setOnClickListener(this);
        edittext_wash2_2temperature.setOnClickListener(this);
        edittext_wash3_2temperature.setOnClickListener(this);
        edittext_wash3_3temperature.setOnClickListener(this);
        edittext_elutiontemperature.setOnClickListener(this);

        edittext_splittime.setOnClickListener(this);
        /////时间设定
        edittext_wash1time.setOnClickListener(this);
        edittext_wash2_2time.setOnClickListener(this);
        edittext_wash3_2time.setOnClickListener(this);
        edittext_wash3_3time.setOnClickListener(this);
        edittext_elutiontime.setOnClickListener(this);

        mFileNameEt.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count,int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // TODO Auto-generated method stub
                String temp = editable.toString().trim();
                hexConfig.setFileName(temp);
                saveHexConfig();
            }
        });

        edittext_description.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count,int after) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // TODO Auto-generated method stub
                String temp = editable.toString().trim();
                hexConfig.setDescription(temp);
                saveHexConfig();
            }
        });
    }

    @Override
    protected void initData() {
        String hexConfigString = PreferenceUtil.getInstance(getApplicationContext()).getStringValue(SharedConstant.HEX_FIRST_JSON,"");
        hexConfig = GsonUtil.fromJson(hexConfigString,HexConfig.class);
        if(hexConfig == null){
            hexConfig = new HexConfig();
            String temp = GsonUtil.toJson(hexConfig,true);
            PreferenceUtil.getInstance(getApplicationContext()).setValueByName(SharedConstant.HEX_FIRST_JSON,temp);
        }
        hexConfig.setSecondQuantifyFlag(false);
        checkbox_cassete.setChecked(hexConfig.isCasseteFlag());
        checkbox_magnetic.setChecked(hexConfig.isMagneticFlag());
        checkbox_split.setChecked(hexConfig.isSplitFlag());
        checkbox_wash.setChecked(hexConfig.isWashFlag());
        checkbox_wash1.setChecked(hexConfig.isWash1Flag());
        checkbox_wash2_2.setChecked(hexConfig.isWash22Flag());
        checkbox_wash3_2.setChecked(hexConfig.isWash32Flag());
        checkbox_wash3_3.setChecked(hexConfig.isWash33Flag());
        checkbox_elution.setChecked(hexConfig.isElutionFlag());
        checkbox_quantify.setChecked(hexConfig.isQuantifyFlag());
//        checkbox_second.setChecked(hexConfig.isSecondQuantifyFlag());

        if(hexConfig.getWashStep() == 2){
            radiobutton_twostep.setChecked(true);
        }else if(hexConfig.getWashStep() == 3){
            radiobutton_threestep.setChecked(true);
        }else if(hexConfig.getWashStep() == 1){
            radiobutton_onestep.setChecked(true);
        }


        if(hexConfig.getMagneticChoice() == 0){
            radiobutton_samplelive.setChecked(true);
        }else if(hexConfig.getMagneticChoice() == 1){
            radiobutton_splitlive.setChecked(true);
        }

        if(hexConfig.getFreeze_drying()== 0){
            //液态体系
            mFreezeDrySysRb.setChecked(false);
            mLiquidSysRb.setChecked(true);
        }else if(hexConfig.getFreeze_drying() == 1){
            //冻干体系
            mFreezeDrySysRb.setChecked(true);
            mLiquidSysRb.setChecked(false);
        }

        edittext_megneticsample.setText(NumberUtils.doubleto0String(hexConfig.getMagneticVolume()));
//        edittext_splitsample.setText(NumberUtils.doubleto0String(hexConfig.getSplitLieSampleVolume()));
        edittext_splitlijieye.setText(NumberUtils.doubleto0String(hexConfig.getSplitLieJieYeVolume()));
        edittext_wash1xidiye.setText(NumberUtils.doubleto0String(hexConfig.getWash1XiDiYeVolume()));
        edittext_wash2_2xidiye.setText(NumberUtils.doubleto0String(hexConfig.getWash22XiDiYeVolume()));
        edittext_wash3_2xidiye.setText(NumberUtils.doubleto0String(hexConfig.getWash32XiDiYeVolume()));
        edittext_wash3_3xidiye.setText(NumberUtils.doubleto0String(hexConfig.getWash33XiDiYeVolume()));
        edittext_elutionxituoye.setText(NumberUtils.doubleto0String(hexConfig.getElutionVolume()));
        edittext_quantifyvolume.setText(NumberUtils.doubleto0String(hexConfig.getQuantifyVolume()));

        edittext_splittemperature.setText(NumberUtils.doubleto0String(hexConfig.getSplitTemperature()));
        edittext_wash1temperature.setText(NumberUtils.doubleto0String(hexConfig.getWash1Temperature()));
        edittext_wash2_2temperature.setText(NumberUtils.doubleto0String(hexConfig.getWash22Temperature()));
        edittext_wash3_2temperature.setText(NumberUtils.doubleto0String(hexConfig.getWash32Temperature()));
        edittext_wash3_3temperature.setText(NumberUtils.doubleto0String(hexConfig.getWash33Temperature()));
        edittext_elutiontemperature.setText(NumberUtils.doubleto0String(hexConfig.getElutionTemperature()));

        edittext_splittime.setText(NumberUtils.doubleto0String(hexConfig.getSplitTime()));
        edittext_wash1time.setText(NumberUtils.doubleto0String(hexConfig.getWash1Time()));
        edittext_wash2_2time.setText(NumberUtils.doubleto0String(hexConfig.getWash22Time()));
        edittext_wash3_2time.setText(NumberUtils.doubleto0String(hexConfig.getWash32Time()));
        edittext_wash3_3time.setText(NumberUtils.doubleto0String(hexConfig.getWash33Time()));
        edittext_elutiontime.setText(NumberUtils.doubleto0String(hexConfig.getElutionTime()));

        mFileNameEt.setText(hexConfig.getFileName());
        edittext_description.setText(hexConfig.getDescription());

        arrayAdapter=new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,stringList);

        listview_log.setAdapter(arrayAdapter);
        listview_log.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listview_log.setStackFromBottom(true);
    }

    private void initShowHexFile(){
        showHexFileList.clear();
        File[] files = new File(CONFIG_HEX_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != showHexFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".hex")){
                        showHexFileList.add(files[i]);
                        Collections.sort(showHexFileList);
                    }
                }
            }
        }
      //  Log.i("AAA","initShowHexFile"+showHexFileList.size());
        showHexAdapter.setFileList(showHexFileList);
        showHexAdapter.notifyDataSetChanged();

        mShowHexLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                hex_click_pos = i;
                if(showHexAdapter != null){
                    showHexAdapter.setSelectedPosition(hex_click_pos);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterEventBus();
//        stopRNARunning();
        if(null != stringList){
            stringList.clear();
            stringList = null;
        }
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog = null;
        }

        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.hex_config_operate_tv){
            if(!isOperate){
                mOperateLayout.setVisibility(View.VISIBLE);
                mConfigLayout.setVisibility(View.GONE);
                mOperateBackImg.setVisibility(View.VISIBLE);
                mAllClearTv.setVisibility(View.INVISIBLE);
                mSaveFileTv.setVisibility(View.INVISIBLE);
                isOperate= true;
            }

        }else if(id == R.id.hex_config_file_operate_back_img){
            if(isOperate){
                mOperateLayout.setVisibility(View.GONE);
                mConfigLayout.setVisibility(View.VISIBLE);
                mOperateBackImg.setVisibility(View.GONE);
                mAllClearTv.setVisibility(View.VISIBLE);
                mSaveFileTv.setVisibility(View.VISIBLE);
                isOperate= false;
            }
        }else if(id == R.id.hex_config_save_file_tv){
            saveHexConfigAction();
        }else if(id == R.id.hex_config_save_file_quick_tv){
            saveHexConfigAction_quick();
        }
        else if(id == R.id.hex_config_clear_tv){
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_clearall),1);

        }else if(id == R.id.hex_config_stop_btn){
            //停止
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_stop),4);

        }else if(id == R.id.hex_config_reset_btn){
            //复位
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_reset),7);
        }else if(id == R.id.hex_config_opendoor_btn){
            //开门
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_opendoor),6);
        }else if(id == R.id.hex_config_closedoor_btn){
            //关门
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_closedoor),5);

        }else if(id == R.id.hex_config_downandopen_btn){
            //下机
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_downandopen),8);
        }else if(id == R.id.hex_config_finish_btn){
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_finish),10);
            
        }else if(id == R.id.hex_config_run_btn){
           // runTestAction();
            runHexTestAction();
        }else if(id == R.id.hex_config_clearlog_btn){
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_clearlog),2);

        }else if(id == R.id.hex_config_edit_tv){
            if(hex_click_pos != -1){
               editHexFileAction();
            }else {
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.config_hex_toast_edit)
                        ,Toast.LENGTH_LONG).show();
            }
        }else if(id== R.id.hex_config_delete_tv){
            if(hex_click_pos != -1){
                showNoticeDialog(getResources().getString(R.string.config_hex_config_message_delete),3);
            }else {
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.config_hex_toast_delete),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.edittext_megneticsample){
            //样本体积
            String megnetiSample = edittext_megneticsample.getText().toString();
            showNumKeyboardDialog(1,view,megnetiSample);
        }
//        else if(id == R.id.edittext_splitsample){
//            String splite_sample = edittext_splitsample.getText().toString();
//            showNumKeyboardDialog(2,view,splite_sample);
//        }
        else if(id == R.id.edittext_splitlijieye){
            String splite_liejieye = edittext_splitlijieye.getText().toString();
            showNumKeyboardDialog(3,view,splite_liejieye);
        }else if(id == R.id.edittext_wash1xidiye){
            //步骤4-1洗涤液
            String wash1_xidiye = edittext_wash1xidiye.getText().toString();
            showNumKeyboardDialog(4,view,wash1_xidiye);
        }else if(id == R.id.edittext_wash2_2xidiye){
            //步骤4-2洗涤液
            String wash2_2_xidiye = edittext_wash2_2xidiye.getText().toString();
            showNumKeyboardDialog(5,view,wash2_2_xidiye);
        }else if(id == R.id.edittext_wash3_2xidiye){
            String wash3_2_xidiye = edittext_wash3_2xidiye.getText().toString();
            showNumKeyboardDialog(6,view,wash3_2_xidiye);
        }else if(id == R.id.edittext_wash3_3xidiye){
            String wash3_3_xidiye = edittext_wash3_3xidiye.getText().toString();
            showNumKeyboardDialog(7,view,wash3_3_xidiye);
        }else if (id == R.id.edittext_elutionxituoye){
            //洗脱液体积
            String xituoye = edittext_elutionxituoye.getText().toString();
            showNumKeyboardDialog(8,view,xituoye);
        }else if(id == R.id.edittext_quantifyvolume){
            //体系定量
            String quantifyvolume = edittext_quantifyvolume.getText().toString();
            showNumKeyboardDialog(9,view,quantifyvolume);
        }else if(id == R.id.edittext_splittemperature){
            //裂解温度
            String splittemperature = edittext_splittemperature.getText().toString();
            showNumKeyboardDialog(10,view,splittemperature);
        }else if(id == R.id.edittext_wash1temperature){
            String wash1temperature = edittext_wash1temperature.getText().toString();
            showNumKeyboardDialog(11,view,wash1temperature);
        }else if(id == R.id.edittext_wash2_2temperature){
            String wash2_2temperature = edittext_wash2_2temperature.getText().toString();
            showNumKeyboardDialog(12,view,wash2_2temperature);
        }else if(id == R.id.edittext_wash3_2temperature){
            String wash3_2temperature = edittext_wash3_2temperature.getText().toString();
            showNumKeyboardDialog(13,view,wash3_2temperature);
        }else if(id == R.id.edittext_wash3_3temperature){
            String wash3_3temperature = edittext_wash3_3temperature.getText().toString();
            showNumKeyboardDialog(14,view,wash3_3temperature);
        }else if(id == R.id.edittext_elutiontemperature){
            //洗脱温度
            String elutiontemperature = edittext_elutiontemperature.getText().toString();
            showNumKeyboardDialog(15,view,elutiontemperature);
        }else if(id == R.id.edittext_splittime){
            //裂解时间
            String splite_time = edittext_splittime.getText().toString();
            showNumKeyboardDialog(16,view,splite_time);
        }else if(id == R.id.edittext_wash1time){
            //裂解时间
            String wash1_time = edittext_wash1time.getText().toString();
            showNumKeyboardDialog(17,view,wash1_time);
        }else if(id == R.id.edittext_wash2_2time){
            //裂解时间
            String wash22_time = edittext_wash2_2time.getText().toString();
            showNumKeyboardDialog(18,view,wash22_time);
        }else if(id == R.id.edittext_wash3_2time){
            String wash32_time = edittext_wash3_2time.getText().toString();
            showNumKeyboardDialog(19,view,wash32_time);
        }else if(id == R.id.edittext_wash3_3time){
            String wash33_time = edittext_wash3_3time.getText().toString();
            showNumKeyboardDialog(20,view,wash33_time);
        }else if(id == R.id.edittext_elutiontime){
            String elutiontime = edittext_elutiontime.getText().toString();
            showNumKeyboardDialog(21,view,elutiontime);
        }
    }

    /**
     *
     * @param message
     * @param dialog_Type 1：添加 2：删除
     */
    private void showNoticeDialog(String message,int dialog_Type){
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog =null;
        }

        if(null == noticeDialog){
            noticeDialog =new NoticeDialog(this,R.style.Dialog);
            noticeDialog.setMessage(message);
            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    noticeDialog.dismiss();
                    //删除
                    switch (dialog_Type){
                        case 1:
                            clearAllHexConfig();
                            break;
                        case 2:
                            clearLogAction();
                            break;
                        case 3:
                            //删除核酸提取文件
                            delHexFileAction();
                            break;
                        case 4:
                            stopAction();
                            break;
                        case 5:
                            closeDoorAction();
                            break;
                        case 6:
                            openDoorAction();
                            break;
                        case 7:
                            resetAction();
                            break;
                        case 8:
                            downAndOpenAction();
                            break;
                        case 9:
                            String fileName = mFileNameEt.getText().toString();
                            saveHexConfigFile(fileName);
                            break; 						
						case 10:
							finishCMD();
                            break;
                        default:
                            break;
                    }

                }
            });

            noticeDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                @Override
                public void onCancelClick() {
                    noticeDialog.dismiss();
                    noticeDialog = null;
                }
            });

            noticeDialog.show();
        }
    }

    private void saveHexConfig() {
        String temp = GsonUtil.toJson(hexConfig, true);
        PreferenceUtil.getInstance(getApplicationContext()).setValueByName(SharedConstant.HEX_FIRST_JSON, temp);
    }

    /**
     * 保存填写的配置
     */
    private void saveHexConfigAction(){

        String fileName = mFileNameEt.getText().toString();
        if(StringUtil.isEmpty(fileName)){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.config_hex_toast_save_file_name)
                    ,Toast.LENGTH_LONG).show();
            return;
        }

        hexConfig.setFileName(fileName);
        if(hexConfig.isSecondQuantifyFlag()){
            hexConfig.setType(2);
        }else{
            hexConfig.setType(1);
        }

        if(hexConfig.isMagneticFlag()){
            if(hexConfig.getMagneticVolume()<=0.0f || hexConfig.getMagneticVolume()>400.0f){
                showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_magnetic),-1);
                return;
            }
        }
        if(hexConfig.isSplitFlag()){
            if(hexConfig.getSplitLieJieYeVolume()<=0.0f || hexConfig.getSplitLieJieYeVolume()>800.0f){
                showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_splite_volume),-1);
                return;
            }
            if(hexConfig.getSplitTemperature()<10.0f || hexConfig.getSplitTemperature()>105.0f){
                showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_splite_temp),-1);
                return;
            }
            if(hexConfig.getSplitTime()<=0.0f){
                showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_splite_time),-1);
                return;
            }
        }

        if(hexConfig.isWashFlag()){
            if(hexConfig.getWashStep()==1){
                if(hexConfig.isWash1Flag()){
                    if(hexConfig.getWash1XiDiYeVolume()<180.0f || hexConfig.getWash1XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_step_one_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash1Temperature()<10.0f || hexConfig.getWash1Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_step_one_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash1Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_step_one_washing_time),-1);
                        return;
                    }
                }
            }else if(hexConfig.getWashStep()==2){
                if(hexConfig.isWash1Flag()){
                    if(hexConfig.getWash1XiDiYeVolume()<180.0f || hexConfig.getWash1XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_one_washing_volume ),-1);
                        return;
                    }
                    if(hexConfig.getWash1Temperature()<10.0f || hexConfig.getWash1Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_one_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash1Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_one_washing_time),-1);
                        return;
                    }
                }
                if(hexConfig.isWash22Flag()){
                    if(hexConfig.getWash22XiDiYeVolume()<180.0f || hexConfig.getWash22XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_second_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash22Temperature()<10.0f || hexConfig.getWash22Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_second_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash22Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_second_washing_time),-1);
                        return;
                    }
                }
            }else if(hexConfig.getWashStep()==3){
                if(hexConfig.isWash1Flag()){
                    if(hexConfig.getWash1XiDiYeVolume()<180.0f || hexConfig.getWash1XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_first_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash1Temperature()<10.0f || hexConfig.getWash1Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_first_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash1Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_first_washing_time),-1);
                        return;
                    }
                }
                if(hexConfig.isWash32Flag()){
                    if(hexConfig.getWash32XiDiYeVolume()<180.0f || hexConfig.getWash32XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_second_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash32Temperature()<10.0f || hexConfig.getWash32Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_second_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash32Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_second_washing_time),-1);
                        return;
                    }
                }
                if(hexConfig.isWash33Flag()){
                    if(hexConfig.getWash33XiDiYeVolume()<180.0f || hexConfig.getWash33XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_third_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash33Temperature()<10.0f || hexConfig.getWash33Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_third_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash33Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_third_washing_time),-1);
                        return;
                    }
                }
            }

            if(hexConfig.isElutionFlag()){
                if(hexConfig.getElutionVolume()<=0.0f || hexConfig.getElutionVolume()>800.0f){
                    showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_elution_volume),-1);
                    return;
                }
                if(hexConfig.getElutionTemperature()<10.0f || hexConfig.getElutionTemperature()>105.0f){
                    showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_elution_temp),-1);
                    return;
                }
                if(hexConfig.getElutionTime()<=0.0f){
                    showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_elution_time),-1);
                    return;
                }
            }
            if(hexConfig.isQuantifyFlag()){
                if(hexConfig.getQuantifyVolume()<=0.0f || hexConfig.getQuantifyVolume()>=50.0f){
                    showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_quantitative_volume),-1);
                    return;
                }

            }

        }

        //String hexFileName = fileName + ".hexConfig";
       // Log.i("AAA","saveHexConfigAction ---fileName" + fileName);
        if(isSameHexFile(fileName)){
            //存在相同的名字的模板
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_same_name),9);
        }else{
            saveHexConfigFile(fileName);
        }
    }

    /**
     * 保存填写的配置
     */
    private void saveHexConfigAction_quick(){

        String fileName = mFileNameEt.getText().toString();
        if(StringUtil.isEmpty(fileName)){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.config_hex_toast_save_file_name)
                    ,Toast.LENGTH_LONG).show();
            return;
        }

        if(fileName.contains("_quick")){
            fileName = fileName;
        }else{
            fileName = fileName + "_quick";
        }

        hexConfig.setFileName(fileName);
        if(hexConfig.isSecondQuantifyFlag()){
            hexConfig.setType(2);
        }else{
            hexConfig.setType(1);
        }

        if(hexConfig.isMagneticFlag()){
            if(hexConfig.getMagneticVolume()<=0.0f || hexConfig.getMagneticVolume()>400.0f){
                showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_magnetic),-1);
                return;
            }
        }
        if(hexConfig.isSplitFlag()){
            if(hexConfig.getSplitLieJieYeVolume()<=0.0f || hexConfig.getSplitLieJieYeVolume()>800.0f){
                showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_splite_volume),-1);
                return;
            }
            if(hexConfig.getSplitTemperature()<10.0f || hexConfig.getSplitTemperature()>105.0f){
                showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_splite_temp),-1);
                return;
            }
            if(hexConfig.getSplitTime()<=0.0f){
                showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_splite_time),-1);
                return;
            }
        }

        if(hexConfig.isWashFlag()){
            if(hexConfig.getWashStep()==1){
                if(hexConfig.isWash1Flag()){
                    if(hexConfig.getWash1XiDiYeVolume()<180.0f || hexConfig.getWash1XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_step_one_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash1Temperature()<10.0f || hexConfig.getWash1Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_step_one_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash1Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_step_one_washing_time),-1);
                        return;
                    }
                }
            }else if(hexConfig.getWashStep()==2){
                if(hexConfig.isWash1Flag()){
                    if(hexConfig.getWash1XiDiYeVolume()<180.0f || hexConfig.getWash1XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_one_washing_volume ),-1);
                        return;
                    }
                    if(hexConfig.getWash1Temperature()<10.0f || hexConfig.getWash1Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_one_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash1Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_one_washing_time),-1);
                        return;
                    }
                }
                if(hexConfig.isWash22Flag()){
                    if(hexConfig.getWash22XiDiYeVolume()<180.0f || hexConfig.getWash22XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_second_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash22Temperature()<10.0f || hexConfig.getWash22Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_second_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash22Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_two_step_second_washing_time),-1);
                        return;
                    }
                }
            }else if(hexConfig.getWashStep()==3){
                if(hexConfig.isWash1Flag()){
                    if(hexConfig.getWash1XiDiYeVolume()<180.0f || hexConfig.getWash1XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_first_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash1Temperature()<10.0f || hexConfig.getWash1Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_first_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash1Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_first_washing_time),-1);
                        return;
                    }
                }
                if(hexConfig.isWash32Flag()){
                    if(hexConfig.getWash32XiDiYeVolume()<180.0f || hexConfig.getWash32XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_second_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash32Temperature()<10.0f || hexConfig.getWash32Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_second_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash32Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_second_washing_time),-1);
                        return;
                    }
                }
                if(hexConfig.isWash33Flag()){
                    if(hexConfig.getWash33XiDiYeVolume()<180.0f || hexConfig.getWash33XiDiYeVolume()>840.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_third_washing_volume),-1);
                        return;
                    }
                    if(hexConfig.getWash33Temperature()<10.0f || hexConfig.getWash33Temperature()>105.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_third_washing_temp),-1);
                        return;
                    }
                    if(hexConfig.getWash33Time()<=0.0f){
                        showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_three_step_third_washing_time),-1);
                        return;
                    }
                }
            }

            if(hexConfig.isElutionFlag()){
                if(hexConfig.getElutionVolume()<=0.0f || hexConfig.getElutionVolume()>800.0f){
                    showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_elution_volume),-1);
                    return;
                }
                if(hexConfig.getElutionTemperature()<10.0f || hexConfig.getElutionTemperature()>105.0f){
                    showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_elution_temp),-1);
                    return;
                }
                if(hexConfig.getElutionTime()<=0.0f){
                    showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_elution_time),-1);
                    return;
                }
            }
            if(hexConfig.isQuantifyFlag()){
                if(hexConfig.getQuantifyVolume()<=0.0f || hexConfig.getQuantifyVolume()>=50.0f){
                    showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_quantitative_volume),-1);
                    return;
                }

            }

        }

        //String hexFileName = fileName + ".hexConfig";
        // Log.i("AAA","saveHexConfigAction ---fileName" + fileName);
//        if(fileName.contains("_quick")){
//            fileName = fileName;
//        }else{
//            fileName = fileName + "_quick";
//        }

        if(isSameHexFile(fileName)){
            //存在相同的名字的模板
            showNoticeDialog(getResources().getString(R.string.config_hex_dialog_message_same_name),9);
        }else{
            saveHexConfigFile_quick(fileName);
        }
    }

    private void saveHexConfigFile(String fileName){
        Boolean result = HexUtils.saveHexFile(ConfigHexActivity.this,fileName,hexConfig);
        if(!result){
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.config_hex_toast_save_fail),Toast.LENGTH_LONG).show();;
        }else{
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.config_hex_toast_save_success),Toast.LENGTH_LONG).show();;
            initShowHexFile();
        }
    }

    private void saveHexConfigFile_quick(String fileName){
        Boolean result = HexUtils.saveHexFile_quick(ConfigHexActivity.this,fileName,hexConfig);
        if(!result){
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.config_hex_toast_save_fail),Toast.LENGTH_LONG).show();;
        }else{
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.config_hex_toast_save_success),Toast.LENGTH_LONG).show();;
            initShowHexFile();
        }
    }

    /**
     * 清空填写的配置
     */
    private void  clearAllHexConfig(){
        hexConfig = new HexConfig();
        String temp = GsonUtil.toJson(hexConfig,true);
        PreferenceUtil.getInstance(getApplicationContext()).setValueByName(SharedConstant.HEX_FIRST_JSON,temp);
        initData();
    }

    private void  stopAction(){
        stopRNARunning();
    }

    /**
     * 复位
     */
    private void resetAction(){
       resetSystem();
    }

    /**
     * 开门
     */
    private void openDoorAction(){
        openDoor();
    }

    /**
     * 关门操作
     */
    private void closeDoorAction(){
        closeDoor();
    }

    /**
     * 下机
     */
    private void downAndOpenAction(){
        downandopen();
    }

   /* private void runTestAction(File file){
        dialogDelete = new AlertDialog.Builder(ConfigHexActivity.this)
                .setTitle("确认")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Boolean result = HexUtils.saveHexFile(ConfigHexActivity.this,"temp",hexConfig);
                        if(!result){
                            Toast.makeText(getApplicationContext(),"试运行失败，请检查",Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            Toast.makeText(getApplicationContext(),"保存临时文件成功",Toast.LENGTH_LONG).show();
                        }

                        startAysnTask(new File(HexUtils.HEXCREATEFILE_PATH + "temp.hex"));
                    }
                })
                .create();
        dialogDelete.show();
    }
*/
    /**
     * 运行测试
     */
    private void runHexTestAction(){
        if(hex_click_pos != -1){
            File file = showHexFileList.get(hex_click_pos);
            if(file != null){
                mFileNameEt.setText(file.getName().substring(0,file.getName().length()-4));
                startAysnTask(file);
            }

        }else{
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.config_hex_toast_test_file),Toast.LENGTH_LONG).show();
        }
    }

    private void clearLogAction(){
        if(stringList!=null){
            stringList.clear();
        }
        arrayAdapter.notifyDataSetChanged();
    }

    /**
     * 编辑核酸提取文件
     */
    private void editHexFileAction(){
        File file = showHexFileList.get(hex_click_pos);
        if(!StringUtil.isEmpty(file.getName())){
            //
            String fileName = file.getName().substring(0,file.getName().length()-4);
            String configHexFilePath = CONFIG_HEX_CONFIG_PATH + fileName + ".hexConfig";
            if(null != configHexFilePath){
                File config_hex_file = new File(configHexFilePath);
                if(config_hex_file != null && config_hex_file.exists()&& config_hex_file.isFile()){
                    Log.i("ConfigHexActivity","openRNAFile" +config_hex_file.getName() );
                    HexConfig hexConfig = ConfigManager.getHexConfigFile(config_hex_file);
                    if(hexConfig != null){
                        updateHexFileUI(hexConfig);
                    }
                }
            }else{
                Toast.makeText(getApplicationContext()
                        ,getResources().getString(R.string.config_hex_toast_cannot_open_file),Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.config_hex_toast_cannot_open_file),Toast.LENGTH_LONG).show();
        }
    }

    private void updateHexFileUI(HexConfig _hexConfig){
        this.hexConfig = _hexConfig;
        checkbox_cassete.setChecked(hexConfig.isCasseteFlag());
        checkbox_magnetic.setChecked(hexConfig.isMagneticFlag());
        checkbox_split.setChecked(hexConfig.isSplitFlag());
        checkbox_wash.setChecked(hexConfig.isWashFlag());
        checkbox_wash1.setChecked(hexConfig.isWash1Flag());
        checkbox_wash2_2.setChecked(hexConfig.isWash22Flag());
        checkbox_wash3_2.setChecked(hexConfig.isWash32Flag());
        checkbox_wash3_3.setChecked(hexConfig.isWash33Flag());
        checkbox_elution.setChecked(hexConfig.isElutionFlag());
        checkbox_quantify.setChecked(hexConfig.isQuantifyFlag());
//        checkbox_second.setChecked(hexConfig.isSecondQuantifyFlag());

        if(hexConfig.getWashStep() == 2){
            radiobutton_twostep.setChecked(true);
        }else if(hexConfig.getWashStep() == 3){
            radiobutton_threestep.setChecked(true);
        }else if(hexConfig.getWashStep() == 1){
            radiobutton_onestep.setChecked(true);
        }

        if(hexConfig.getMagneticChoice() == 0){
            radiobutton_samplelive.setChecked(true);
        }else if(hexConfig.getMagneticChoice() == 1){
            radiobutton_splitlive.setChecked(true);
        }

        edittext_megneticsample.setText(NumberUtils.doubleto0String(hexConfig.getMagneticVolume()));
//        edittext_splitsample.setText(NumberUtils.doubleto0String(hexConfig.getSplitLieSampleVolume()));
        edittext_splitlijieye.setText(NumberUtils.doubleto0String(hexConfig.getSplitLieJieYeVolume()));
        edittext_wash1xidiye.setText(NumberUtils.doubleto0String(hexConfig.getWash1XiDiYeVolume()));
        edittext_wash2_2xidiye.setText(NumberUtils.doubleto0String(hexConfig.getWash22XiDiYeVolume()));
        edittext_wash3_2xidiye.setText(NumberUtils.doubleto0String(hexConfig.getWash32XiDiYeVolume()));
        edittext_wash3_3xidiye.setText(NumberUtils.doubleto0String(hexConfig.getWash33XiDiYeVolume()));
        edittext_elutionxituoye.setText(NumberUtils.doubleto0String(hexConfig.getElutionVolume()));
        edittext_quantifyvolume.setText(NumberUtils.doubleto0String(hexConfig.getQuantifyVolume()));

        edittext_splittemperature.setText(NumberUtils.doubleto0String(hexConfig.getSplitTemperature()));
        edittext_wash1temperature.setText(NumberUtils.doubleto0String(hexConfig.getWash1Temperature()));
        edittext_wash2_2temperature.setText(NumberUtils.doubleto0String(hexConfig.getWash22Temperature()));
        edittext_wash3_2temperature.setText(NumberUtils.doubleto0String(hexConfig.getWash32Temperature()));
        edittext_wash3_3temperature.setText(NumberUtils.doubleto0String(hexConfig.getWash33Temperature()));
        edittext_elutiontemperature.setText(NumberUtils.doubleto0String(hexConfig.getElutionTemperature()));

        edittext_splittime.setText(NumberUtils.doubleto0String(hexConfig.getSplitTime()));
        edittext_wash1time.setText(NumberUtils.doubleto0String(hexConfig.getWash1Time()));
        edittext_wash2_2time.setText(NumberUtils.doubleto0String(hexConfig.getWash22Time()));
        edittext_wash3_2time.setText(NumberUtils.doubleto0String(hexConfig.getWash32Time()));
        edittext_wash3_3time.setText(NumberUtils.doubleto0String(hexConfig.getWash33Time()));
        edittext_elutiontime.setText(NumberUtils.doubleto0String(hexConfig.getElutionTime()));

        mFileNameEt.setText(hexConfig.getFileName());
        edittext_description.setText(hexConfig.getDescription());

        if(hexConfig.getFreeze_drying()== 0){
            //液态体系
            mFreezeDrySysRb.setChecked(false);
            mLiquidSysRb.setChecked(true);
        }else if(hexConfig.getFreeze_drying() == 1){
            //冻干体系
            mFreezeDrySysRb.setChecked(true);
            mLiquidSysRb.setChecked(false);
        }



    }


    /**
     * 删除核酸提取文件
     */
    private void delHexFileAction(){
        if(hex_click_pos != -1){
            File file = showHexFileList.get(hex_click_pos);
            //删除所选的文件
            String deletePath = CONFIG_HEX_PATH + file.getName();//
            //删除配置文件
            String config_hex_path = CONFIG_HEX_CONFIG_PATH + file.getName().substring(0,file.getName().length()-4)+"hexConfig";
            boolean result1 =  FileUtils.deleteByPath(deletePath);//删除存储
            boolean result2 =  FileUtils.deleteByPath(config_hex_path);//删除存储
            showHexFileList.remove(file);
            showHexAdapter.setFileList(showHexFileList);
            showHexAdapter.notifyDataSetChanged();
            hex_click_pos = -1;
            showHexAdapter.setSelectedPosition(hex_click_pos);
        }
    }

    /**
     * 系统复位
     */

    private void resetSystem(){
        if(PcrApplication.is_RNA_connect){
            ////异步全流程
            if(null != PcrApplication.rNARunningMessage){
                String rna_step_num = PcrApplication.rNARunningMessage.getNO();
                int flag = 0;
                String downandopen_filePath = "";
                if(!StringUtil.isEmpty(rna_step_num)) {
                    String[] mRNA_running = rna_step_num.split("\\|",-1);
                    if (mRNA_running.length >= 5) {
                        flag = NumberUtils.StringtoInt(mRNA_running[3]);
                    }
                }
                if(flag == 1){
                    downandopen_filePath = FileUtils.COMMON_HEX_PATH + "99-reset.hex";
                }else if(flag == 0){
                    downandopen_filePath = FileUtils.COMMON_HEX_PATH + "95-reset_exception.hex";
                }
                File resetfile = new File(downandopen_filePath);
                boolean result = startAysnTask(resetfile);
                if(result) {
                    showProgressDialog(getResources().getString(R.string.dialog_title_wait),
                            getResources().getString(R.string.dialog_message_reset));
                }else{
                    showAlarm();
                    showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                            getResources().getString(R.string.dialog_message_reset_error));
                }
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.toast_rna_serial_disconnect),Toast.LENGTH_LONG).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RNARunningMessage rNARunningMessage) {
        Log.i("RNA", "RNA指令运行" + rNARunningMessage.getID() + "状态：" + rNARunningMessage.getStatus());
        Log.i("RNA", "RNA指令运行" + rNARunningMessage.getID() + "指令：" + rNARunningMessage.getNO());
        Log.i("RNA", "RNA指令运行" + rNARunningMessage.getID() + "警报：" + rNARunningMessage.getAlarm());
        String text = GsonUtil.toJson(rNARunningMessage, true);
        Log.i("RNA", "RNA指令运行" + rNARunningMessage.getID() + "消息全文：" + text);
        if(rNARunningMessage != null )
        {
            PcrApplication.is_RecRunningMesaage = true;
            PcrApplication.rNARunningMessage = rNARunningMessage;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ProcedureFinishEvent procedureFinishEvent) {
        if(procedureFinishEvent.getTemplateName().contains("reset")||
                procedureFinishEvent.getTemplateName().contains("opendoor")||
                procedureFinishEvent.getTemplateName().contains("downandopen")||
                procedureFinishEvent.getTemplateName().contains("finishcmd")||
                procedureFinishEvent.getTemplateName().contains("closedoor")) {
            if (procedureFinishEvent.getStatus().equals("finish")) {
                hideProgressDialog();
            } else if (procedureFinishEvent.getStatus().equals("finish error")) {
                hideProgressDialog();
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.dialog_title_open_check_device),
                        getResources().getString(R.string.config_hex_dialog_message_error_instruction));
            }
            stopRNARunning();
        }
        if(procedureFinishEvent.getTemplateName().contains("reset")){
            if (procedureFinishEvent.getStatus().equals("finish")) {
                resetPcrRecMark();
                PcrApplication.rNARunningMessage.setStatus("free");
                if(PcrApplication.is_SLAN_PCR_connect) {
                    BoWeiService.getInstance().stopExperiment((byte) 0x01);
                }
            }
        }

        if(procedureFinishEvent.getTemplateName().contains("temp")){
            if(procedureFinishEvent.getStatus().equals("finish")){
                stopRNARunning();
                MessageEvent finishMsg = new MessageEvent();
                finishMsg.setMsgType(MsgType.MSG_START_FIRST_PCR);
                EventBus.getDefault().postSticky(finishMsg);
                PcrApplication.rNARunningMessage.setStatus("free");
                showNoticeDialog(getResources().getString(R.string.dialog_title_prompt),
                        getResources().getString(R.string.config_hex_dialog_message_instruction_completed));
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                stopRNARunning();
                //第一次RNA流程出错，请检查设备
                showAlarm();
                showNoticeDialog(getResources().getString(R.string.dialog_title_open_check_device),
                        getResources().getString(R.string.config_hex_dialog_message_error_process));

            }
        }
    }

    /**
     * 指令完成
     * @param actionDoneMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ActionDoneMessage actionDoneMessage) {
        if(null != actionDoneMessage ) {
            if (actionDoneMessage.getStatus().contains("error")) {
                Log.i("RNA", "指令错误" + actionDoneMessage.getID() + "状态" + actionDoneMessage.getStatus());
                String text = GsonUtil.toJson(actionDoneMessage, true);
//                Toast.makeText(HexGenerateFirstActivity.this, "actionDoneMessage:" + text, Toast.LENGTH_LONG).show();
                stringList.add(actionDoneMessage.getNO() + "---" + actionDoneMessage.getStatus() + "---" + DateUtil.getCurrentTimeYMDHMS());
                //            setListViewHeightBasedOnChildren(listview_log);
                arrayAdapter.notifyDataSetChanged();
                listview_log.setSelection(arrayAdapter.getCount() - 1);
            }else{
                Log.i("RNA", "指令完成!!!!!!!!" + actionDoneMessage.getID() + "状态" + actionDoneMessage.getStatus());
                String text = GsonUtil.toJson(actionDoneMessage, true);
//                Toast.makeText(HexGenerateFirstActivity.this, "actionDoneMessage:" + text, Toast.LENGTH_LONG).show();
                stringList.add(actionDoneMessage.getNO() + "---" + actionDoneMessage.getStatus() + "---" + DateUtil.getCurrentTimeYMDHMS());
                //            setListViewHeightBasedOnChildren(listview_log);
                arrayAdapter.notifyDataSetChanged();
                listview_log.setSelection(arrayAdapter.getCount() - 1);
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WarningMessage warningMessage) {
        if(null != warningMessage ) {
            String json = GsonUtil.toJson(warningMessage, true);
            Log.e("WarningMessage", "警报消息 :" + json);
            stringList.add("WarningMessage:" + json);
            arrayAdapter.notifyDataSetChanged();
            listview_log.setSelection(arrayAdapter.getCount()-1);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(SendCmdSuccessEvent sendCmdSuccessEvent) {
        if(null != sendCmdSuccessEvent ){
            MoveActionCmd moveActionCmd = (MoveActionCmd) sendCmdSuccessEvent.getMsgPacket();
            String json = GsonUtil.toJson(moveActionCmd,true);
            Log.i("SendCmdSuccessEvent RNA", "指令发送 :"  + json);
            String text = GsonUtil.toJson(sendCmdSuccessEvent,true);
//            Toast.makeText(HexGenerateFirstActivity.this,"moveActionCmd:" + text,Toast.LENGTH_LONG).show();

            String description = "";
            int i = 0;
            try{

                description =  RNAAndPCRAsyncTask.actionListQueue_showinfo.getList().get(moveActionCmd.getID()-1).getDescription();

                i = moveActionCmd.getID();

            }catch (Exception e){
                e.printStackTrace();
            }

            stringList.add(i + "--" + description + "--" +  text);

//            setListViewHeightBasedOnChildren(listview_log);
            arrayAdapter.notifyDataSetChanged();
            listview_log.setSelection(arrayAdapter.getCount()-1);
        }
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        if(listView == null) return;
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }
        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    private boolean isSameHexFile(String name){
        if(null != showHexFileList && showHexFileList.size()>0){
            for(File file :showHexFileList){
                String hexFileName = file.getName().substring(0,file.getName().length()-4);
                if(hexFileName.equals(name)){
                    return true;
                }
            }
        }
        return false;
    }

    private NumKeyboardDialog numKeyboardDialog;
    private void showNumKeyboardDialog(int dialogType, View view,String s){
        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
        if(numKeyboardDialog == null){
            numKeyboardDialog = new NumKeyboardDialog(this);

            numKeyboardDialog.setEditText(s);
            numKeyboardDialog.setOnCanlceListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.setOnKeyBoardDoneListener(new NumKeyboardDialog.OnKeyBoardDoneListener() {
                @Override
                public void onKeyBoardDone(String s) {
                    switch (dialogType){
                        case 1:
                            //样本体积
                            double value = NumberUtils.StringtoDouble(s);
                            if(value <= 1.0f ||value > 840.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else{
                                hexConfig.setMagneticVolume(value);
                                hexConfig.setSplitLieSampleVolume(value);
                                edittext_megneticsample.setText(NumberUtils.doubleto0String(value));
                                saveHexConfig();
                            }
                            break;
                        case 2:
                            double spliteSample = NumberUtils.StringtoDouble(s);
                            if(spliteSample <= 0.0f ||spliteSample > 400.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_splite_sample_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else{
                                hexConfig.setMagneticVolume(spliteSample);
                                hexConfig.setSplitLieSampleVolume(spliteSample);
//                                edittext_splitsample.setText(NumberUtils.doubleto0String(spliteSample));
                                saveHexConfig();
                            }
                            break;
                        case 3:
                            double spliteLieJieYe = NumberUtils.StringtoDouble(s);
                            if(spliteLieJieYe <= 0.0f ||spliteLieJieYe >800.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_splite_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_splitlijieye.setText(NumberUtils.doubleto0String(spliteLieJieYe));
                                hexConfig.setSplitLieJieYeVolume(spliteLieJieYe);
                                saveHexConfig();
                            }
                            break;
                        case 4:
                            double wash1_xidiye = NumberUtils.StringtoDouble(s);
                            if(wash1_xidiye >840.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else if(wash1_xidiye<180.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_wash1xidiye.setText(NumberUtils.doubleto0String(wash1_xidiye));
                                hexConfig.setWash1XiDiYeVolume(wash1_xidiye);
                                saveHexConfig();
                            }
                            break;
                        case 5:
                            double wash2_2xidiye = NumberUtils.StringtoDouble(s);
                            if(wash2_2xidiye >840.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else if(wash2_2xidiye<180.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_wash2_2xidiye.setText(NumberUtils.doubleto0String(wash2_2xidiye));
                                hexConfig.setWash22XiDiYeVolume(wash2_2xidiye);
                                saveHexConfig();
                            }
                            break;
                        case 6:
                            double wash3_2xidiye = NumberUtils.StringtoDouble(s);
                            if(wash3_2xidiye >840.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else if(wash3_2xidiye<180.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_wash3_2xidiye.setText(NumberUtils.doubleto0String(wash3_2xidiye));
                                hexConfig.setWash32XiDiYeVolume(wash3_2xidiye);
                                saveHexConfig();
                            }
                            break;

                        case 7:
                            double wash3_3xidiye = NumberUtils.StringtoDouble(s);
                            if(wash3_3xidiye >840.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else if(wash3_3xidiye<180.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_wash3_3xidiye.setText(NumberUtils.doubleto0String(wash3_3xidiye));
                                hexConfig.setWash33XiDiYeVolume(wash3_3xidiye);
                                saveHexConfig();
                            }
                            break;

                        case 8:
                            //洗脱液体积
                            double elutionVolume = NumberUtils.StringtoDouble(s);
                            if(elutionVolume >840.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_elution_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else if(elutionVolume<=0.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_elution_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_elutionxituoye.setText(NumberUtils.doubleto0String(elutionVolume));
                                hexConfig.setElutionVolume(elutionVolume);
                                saveHexConfig();
                            }
                            break;

                        case 9:
                            //体系定量
                            double quantifyvolume = NumberUtils.StringtoDouble(s);
                            if(quantifyvolume < 5.0f ||quantifyvolume >200.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_quantative_volume),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_quantifyvolume.setText(NumberUtils.doubleto0String(quantifyvolume));
                                hexConfig.setQuantifyVolume(quantifyvolume);
                                saveHexConfig();
                            }
                            break;
                        case 10:
                            //裂解温度
                            double splittemperature = NumberUtils.StringtoDouble(s);
                            if(splittemperature < 10 ||splittemperature >105.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_splite_temp),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_splittemperature.setText(NumberUtils.doubleto0String(splittemperature));
                                hexConfig.setSplitTemperature(splittemperature);
                                saveHexConfig();
                            }
                            break;
                        case 11:
                            double wash1_temp = NumberUtils.StringtoDouble(s);
                            if(wash1_temp < 10 ||wash1_temp >105.0f){
                                //洗涤温度范围值是10-105℃
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_temp),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_wash1temperature.setText(NumberUtils.doubleto0String(wash1_temp));
                                hexConfig.setWash1Temperature(wash1_temp);
                                saveHexConfig();
                            }
                            break;
                        case 12:
                            //洗涤4-2温度
                            double wash2_2_temp = NumberUtils.StringtoDouble(s);
                            if(wash2_2_temp < 10 ||wash2_2_temp >105.0f){
                                //洗涤2温度范围值是10-105℃
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_temp_2),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_wash2_2temperature.setText(NumberUtils.doubleto0String(wash2_2_temp));
                                hexConfig.setWash22Temperature(wash2_2_temp);
                                saveHexConfig();
                            }
                            break;
                        case 13:
                            double wash3_2_temp = NumberUtils.StringtoDouble(s);
                            if(wash3_2_temp < 10 ||wash3_2_temp >105.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_temp_2),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_wash3_2temperature.setText(NumberUtils.doubleto0String(wash3_2_temp));
                                hexConfig.setWash32Temperature(wash3_2_temp);
                                saveHexConfig();
                            }
                            break;
                        case 14:
                            double wash3_3_temp = NumberUtils.StringtoDouble(s);
                            if(wash3_3_temp < 10 ||wash3_3_temp >105.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_washing_temp_3),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_wash3_3temperature.setText(NumberUtils.doubleto0String(wash3_3_temp));
                                hexConfig.setWash33Temperature(wash3_3_temp);
                                saveHexConfig();
                            }
                            break;
                        case 15:
                            //洗脱温度
                            double elution_temp = NumberUtils.StringtoDouble(s);
                            if(elution_temp < 10 ||elution_temp >105.0f){
                                Toast.makeText(getApplicationContext(),
                                        getResources().getString(R.string.config_hex_toast_elution_temp),Toast.LENGTH_LONG).show();
                                return;
                            }else {
                                edittext_elutiontemperature.setText(NumberUtils.doubleto0String(elution_temp));
                                hexConfig.setElutionTemperature(elution_temp);
                                saveHexConfig();
                            }
                            break;
                        case 16:
                            //洗脱温度
                            double splite_time = NumberUtils.StringtoDouble(s);
                            edittext_splittime.setText(NumberUtils.doubleto0String(splite_time));
                            hexConfig.setSplitTime(splite_time);
                            saveHexConfig();
                            break;
                        case 17:
                            //洗涤1温度
                            double wash1_time = NumberUtils.StringtoDouble(s);
                            edittext_wash1time.setText(NumberUtils.doubleto0String(wash1_time));
                            hexConfig.setWash1Time(wash1_time);
                            saveHexConfig();
                            break;
                        case 18:
                            //洗涤1温度
                            double wash22_time = NumberUtils.StringtoDouble(s);
                            edittext_wash2_2time.setText(NumberUtils.doubleto0String(wash22_time));
                            hexConfig.setWash22Time(wash22_time);
                            saveHexConfig();
                            break;

                        case 19:
                            //洗涤1温度
                            double wash32_time = NumberUtils.StringtoDouble(s);
                            edittext_wash3_2time.setText(NumberUtils.doubleto0String(wash32_time));
                            hexConfig.setWash32Time(wash32_time);
                            saveHexConfig();
                            break;
                        case 20:
                            //洗涤33时间
                            double wash33_time = NumberUtils.StringtoDouble(s);
                            edittext_wash3_3time.setText(NumberUtils.doubleto0String(wash33_time));
                            hexConfig.setWash33Time(wash33_time);
                            saveHexConfig();
                            break;
                        case 21:
                            double elutiontime = NumberUtils.StringtoDouble(s);
                            edittext_elutiontime.setText(NumberUtils.doubleto0String(elutiontime));
                            hexConfig.setElutionTime(elutiontime);
                            saveHexConfig();
                            break;
                        default:
                            break;
                    }

                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.show();
        }
    }
}
