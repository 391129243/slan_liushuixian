package com.gidi.bio_pcr.config.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.constant.HexFileName_V5;
import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.CTRuleEntity;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKRuleEntity;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexConfig;
import com.gidi.bio_pcr.WintersUtils.universalUtils.TimerCaculate;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.ConfigManager;
import com.gidi.bio_pcr.config.ConfigSLANActivity;
import com.gidi.bio_pcr.config.adapter.ConfigSLANAdapter;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.config.adapter.FileNameAdapter;
import com.gidi.bio_pcr.config.adapter.JudgeCtAdapter;
import com.gidi.bio_pcr.config.adapter.JudgePeakAdapter;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomImageText;
import com.gidi.bio_pcr.view.DividerDecoration;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.gidi.bio_pcr.view.LinearSpacingItemDecoration;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Step;
import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.TemperatureControlMode;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * 作者：xxs
 * 创建单重模板文件夹
 */
public class CreateSingleActivity extends BaseActivity implements View.OnClickListener{

    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    private DecimalFormat nf = new DecimalFormat( "0.00");
    //控温模式
    private RadioGroup mTempCtrlModeRg;
    private RadioButton mTempCtrlFastRb,mTempCtrlStandardRb;
    private int temparatureCtrlMode = TemperatureControlMode.Fast.getValue();

    private EditText mTemplateEt,mTotalTimeEt,mSecondNameEt,mThirdNameEt;;
    private boolean isFirstRNACheck,isFirstPCRCheck,isCtCheck,isPeakCheck = false;
    private boolean isPeak = true;
    private CustomImageText mRNATabTv,mPCRTabTv,mCtTabTv,mPeakTabTv;
    private RelativeLayout mRNAFileLayout,mPCRFileLayout,mJudgeFileLayout;
    private RecyclerView mRnaFileRv,mPcrFileRv,mJudgeFileRv;
    private TextView mSaveTv,mCreatePcrTv,mCreateHexTv,mRnaFileTv,mPcrFileTv,mJudgeFileTv,mPeakFileTv;
    private TextView mCreateCtJudgeTv,mCreatePeakJudgeTv;

    ////////判别文件的详情
    private ListView mCtFileDetailLv;//判别文件的详情列表
    private JudgeCtAdapter judgeCtAdapter;//单重判别文件lv
    private List<CTRuleEntity> ctRuleEntities;
    private List<PEAKRuleEntity> peakRuleEntities;
    private RecyclerView mPeakDetailRv;//判别文件的详情列表
    private JudgePeakAdapter judgePeakAdapter;//多重判别文件lv
    private RelativeLayout mPeakDetailLayout,mCtDetailLayout;//显示Peak文件的layout与Ct文件的Layout
    //////
    private List<ConfigSLANSection>mSLANSections;
    private List<String> mPCRFileList,mJudgeFileList,mHexFileList,mPeakFileList;//上方显示的文件列表
    private ConfigSLANAdapter configSLANAdapter;
    private FileNameAdapter pcrFileAdapter,judgeFileAdapter,hexFileAdapter;
    private RecyclerView programRv;//配置的所有的项目的列表
    private LinearLayout mProgramTitleLayout;
    private LinearLayout mProgramRecycleLayout;
    private Project project = null;
    private int click_position = -1,judge_ct_pos = -1,click_hex = -1,judge_peak_pos = -1;
    private String pcrFileName,ctFileName,hexFileName,peakFileName;

    //核酸提取文件详情
    //1:磁珠活化样本体积,2:裂解温度,3:裂解样本体积,4裂解液体积，5裂解时间
    private TextView mMegneticsampleTv,mSpiltTempTv,mLysisTv,mSpiltTimeTv;
    private TextView mWashTemp_1Tv,mWashVolume_1Tv,mWashTime_1Tv,mWashTemp_2Tv,mWashVolume_2Tv,mWashTime_2Tv
            ,mWashTemp_3Tv,mWashVolume_3Tv,mWashTime_3Tv;
    private TextView magneticlive_tv;
    //洗脱,洗脱液体积
    private TextView mElutionTempTv,mElutionVolumeTv,mElutionTimeTv;
    private TextView mQuantifyVolumeTv;//体积定量
    //液态体系
    private TextView mLiquidSysTv;
    private ImageView mCloseDoorCkImg,mMegeticCkImg,mSpliteCkImg,mWash1CkImg,mWash2CkImg,mWash3CkImg,
            mElutionCkImg,mQuantifyCkImg,mSecondCkImg;
    private LinearLayout mTwoWashLayout,mThreeWashLayout;
    private String CONFIG_JUDGE_PATH =FileUtils.CONFIG_PATH + File.separator + "judge" + File.separator;
    private String CONFIG_HEX_PATH = HexFileName_V5.HEXCREATEFILE_PATH;//默认是V5
    private NoticeDialog noticeDialog;
    private boolean isEdit = false;
    private String tempLateName= "";

    private int firstRnaTime = 0;
    private int firstPcrTime = 0;
    private String CONFIG_FULLPRC_PCR_TEMPLATE_PATH = FileUtils.CONFIG_TEMPLATE_PATH + "fullProcess" +File.separator;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_single_create;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTitleLayout = (RelativeLayout)findViewById(R.id.single_create_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.create_single_title));
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mTotalTimeEt=(EditText)findViewById(R.id.single_create_totaltime_et);
        mTemplateEt = (EditText)findViewById(R.id.single_create_template_name_et);
        mTemplateEt.addTextChangedListener(new LimitInputTextWatcher(mTemplateEt));
        mSecondNameEt  = (EditText)findViewById(R.id.single_create_template_name_second_et);
        mThirdNameEt = (EditText)findViewById(R.id.single_create_template_name_third_et);
        mSecondNameEt.addTextChangedListener(new LimitInputTextWatcher(mSecondNameEt));
        mThirdNameEt.addTextChangedListener(new LimitInputTextWatcher(mThirdNameEt));
        mTempCtrlModeRg = (RadioGroup)findViewById(R.id.single_create_tempctrl_mode_rg);
        mTempCtrlFastRb = (RadioButton) findViewById(R.id.single_create_tempctrl_fast);
        mTempCtrlStandardRb= (RadioButton)findViewById(R.id.single_create_tempctrl_standard);

        mRNAFileLayout = (RelativeLayout) findViewById(R.id.single_create_rna_file_layout);
        mPCRFileLayout = (RelativeLayout) findViewById(R.id.single_create_pcr_layout);
        mJudgeFileLayout = (RelativeLayout) findViewById(R.id.single_create_judge_file_layout);
        mRNATabTv = (CustomImageText)findViewById(R.id.single_create_rna_title_tv);
        mPCRTabTv = (CustomImageText)findViewById(R.id.single_create_pcr_title_tv);
        mCtTabTv= (CustomImageText)findViewById(R.id.single_create_judge_title_tv);
        mPeakTabTv = (CustomImageText)findViewById(R.id.single_create_peak_title_tv);

        mRnaFileRv = (RecyclerView)findViewById(R.id.single_create_rna_list);
        mPcrFileRv = (RecyclerView)findViewById(R.id.single_create_pcr_list);
        mJudgeFileRv = (RecyclerView)findViewById(R.id.single_create_judge_list);
        mSaveTv = (TextView)findViewById(R.id.single_create_save_btn);
        mCreatePcrTv = (TextView)findViewById(R.id.single_create_pcr_create_tv);
        mCreateHexTv = (TextView)findViewById(R.id.single_create_hex_create_tv);
        mCreateCtJudgeTv = (TextView)findViewById(R.id.single_create_ct_create_tv) ;
        mCreatePeakJudgeTv = (TextView)findViewById(R.id.single_create_peak_create_tv) ;
        mRnaFileTv = (TextView)findViewById(R.id.single_create_rna_name_tv);
        mPcrFileTv = (TextView)findViewById(R.id.single_create_pcr_name_tv);
        mJudgeFileTv = (TextView)findViewById(R.id.single_create_judge_name_tv);
        mPeakFileTv = (TextView)findViewById(R.id.single_create_peak_name_tv);
        programRv = (RecyclerView) findViewById(R.id.single_create_slan_program_rv);
        mProgramTitleLayout = (LinearLayout) findViewById(R.id.single_create_program_title_layout);
        mProgramRecycleLayout = (LinearLayout) findViewById(R.id.single_create_program_recycle_layout);

        //判别文件详情
        mCtDetailLayout = (RelativeLayout)findViewById(R.id.config_judge_single_ct_layout) ;
        mCtFileDetailLv = (ListView)mCtDetailLayout.findViewById(R.id.config_create_judge_ct_detail_lv) ;
        judgeCtAdapter = new JudgeCtAdapter(getApplicationContext());
        mCtFileDetailLv.setAdapter(judgeCtAdapter);

        mPeakDetailLayout = (RelativeLayout)findViewById(R.id.config_judge_single_peak_layout) ;
        mPeakDetailRv = (RecyclerView)mPeakDetailLayout.findViewById(R.id.config_judge_peak_list_detail_rv);
        judgePeakAdapter = new JudgePeakAdapter(getApplicationContext());
        mPeakDetailRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,
                false));
        mPeakDetailRv.setAdapter(judgePeakAdapter);


        //核酸提取详情
        mMegneticsampleTv = (TextView)findViewById(R.id.single_create_tv_megneticsample);
        mSpiltTempTv = (TextView)findViewById(R.id.single_create_splittemperature_tv);

        mLysisTv = (TextView)findViewById(R.id.single_create_splitlijieye_tv);
        mSpiltTimeTv = (TextView)findViewById(R.id.single_create_splittime_tv);
        mWashTemp_1Tv = (TextView)findViewById(R.id.single_create_wash1temp_tv);
        mWashVolume_1Tv = (TextView)findViewById(R.id.single_create_wash1xidiye_tv);
        mWashTime_1Tv = (TextView)findViewById(R.id.single_create_wash1time_tv);
        mWashTemp_2Tv = (TextView)findViewById(R.id.single_create_wash2_2temperature_tv);
        mWashVolume_2Tv = (TextView)findViewById(R.id.single_create_wash2_2xidiye_tv);
        mWashTime_2Tv = (TextView)findViewById(R.id.single_create_wash2_2time);
        mWashTemp_3Tv = (TextView)findViewById(R.id.single_create_wash3_3temperature_tv);
        mWashVolume_3Tv = (TextView)findViewById(R.id.single_create_wash3_3xidiye_tv);
        mWashTime_3Tv = (TextView)findViewById(R.id.single_create_wash3_3time_tv);
        mElutionTempTv = (TextView)findViewById(R.id.single_create_elutiontemperature_tv);
        mElutionVolumeTv = (TextView)findViewById(R.id.single_create_elutionxituoye_tv);
        mElutionTimeTv = (TextView)findViewById(R.id.single_create_elutiontime_tv);
        mQuantifyVolumeTv = (TextView)findViewById(R.id.single_create_quantifyvolume);
        mLiquidSysTv = (TextView)findViewById(R.id.single_create_freeze_dry_tv);
        magneticlive_tv = (TextView)findViewById(R.id.magneticlive_tv);

        mTwoWashLayout = (LinearLayout)findViewById(R.id.single_create_layout_wash2);
        mThreeWashLayout  = (LinearLayout)findViewById(R.id.single_create_layout_wash3_3);
        mTwoWashLayout.setVisibility(View.GONE);
        mThreeWashLayout.setVisibility(View.GONE);
        mCloseDoorCkImg = (ImageView)findViewById(R.id.single_create_close_door_ck_img);
        mMegeticCkImg = (ImageView) findViewById(R.id.single_create_megnetic_ck_img);
        mSpliteCkImg = (ImageView) findViewById(R.id.single_create_split_ck_img);
        mWash1CkImg =(ImageView) findViewById(R.id.single_create_wash1_ck_img);
        mWash2CkImg=(ImageView) findViewById(R.id.single_create_wash2_ck_img);
        mWash3CkImg=(ImageView) findViewById(R.id.single_create_wash3_ck_img);
        mElutionCkImg=(ImageView) findViewById(R.id.single_create_elution_ck_img);
        mQuantifyCkImg=(ImageView) findViewById(R.id.single_create_quantify_ck_img);
        mSecondCkImg=(ImageView) findViewById(R.id.single_create_second_ck_img);
        createFiles();
        initData();
    }

    @Override
    protected void initListener() {
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mSaveTv.setOnClickListener(this);
        mCreatePcrTv.setOnClickListener(this);
        mCreateHexTv.setOnClickListener(this);
        mCreateCtJudgeTv.setOnClickListener(this);
        mCreatePeakJudgeTv.setOnClickListener(this);

        mTempCtrlModeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkId) {
                if(checkId == R.id.single_create_tempctrl_fast){
                    temparatureCtrlMode = TemperatureControlMode.Fast.getValue();
                }else if(checkId == R.id.single_create_tempctrl_standard){
                    temparatureCtrlMode = TemperatureControlMode.Standard.getValue();
                }
            }
        });


        mRNATabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mRNATabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.VISIBLE);
                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.GONE);

            }
        });

        mPCRTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.GONE);
                mPCRFileLayout.setVisibility(View.VISIBLE);
                mJudgeFileLayout.setVisibility(View.GONE);
                if(click_position != -1 && mPCRFileList != null){
                    if(mPCRFileList.size()>0){
                        pcrFileAdapter.setSelectedPosition(click_position);
                        openSLANExFile(pcrFileName);
                    }
                }
            }
        });

        mCtTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.GONE);
                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.VISIBLE);
                mPeakDetailLayout.setVisibility(View.GONE);
                mCtDetailLayout.setVisibility(View.VISIBLE);
                judgeFileAdapter.setSelectedPosition(-1);
                isPeak = false;
                // Log.i("AAA","isJudgeCheck--" + isJudgeCheck);
                if(mJudgeFileList != null){
                    judgeFileAdapter.setTemplateList(mJudgeFileList);
                    if(isCtCheck && judge_ct_pos != -1){
                        judgeFileAdapter.setSelectedPosition(judge_ct_pos);
                        openJudgeFile(ctFileName);
                    }
                }
            }
        });

        mPeakTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.GONE);
                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.VISIBLE);
                mPeakDetailLayout.setVisibility(View.VISIBLE);
                mCtDetailLayout.setVisibility(View.GONE);
                judgeFileAdapter.setSelectedPosition(-1);
                isPeak = true;
                if(mPeakFileList != null){
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    if(isPeakCheck && judge_peak_pos != -1){
                        judgeFileAdapter.setSelectedPosition(judge_peak_pos);
                        openJudgeFile(peakFileName);
                    }
                }
            }
        });
    }

    @Override
    protected void initData() {
        //初始化模板名称
        firstRnaTime = firstPcrTime = 0;
        ctRuleEntities = new ArrayList<CTRuleEntity>();
        peakRuleEntities = new ArrayList<PEAKRuleEntity>();
        mSLANSections = new ArrayList<ConfigSLANSection>();
        programRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        programRv.addItemDecoration(new DividerDecoration(this ,2));
        configSLANAdapter = new ConfigSLANAdapter(getApplicationContext());
        mSLANSections.clear();
        configSLANAdapter.setProgramSteps(mSLANSections);
        programRv.setAdapter(configSLANAdapter);
        mRNATabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
        initRNAFile();
        initPcrFileRv();
        initJudgeFileRv();
    }

    private void initRNAFile(){
        mHexFileList = new ArrayList<String>();
        //编辑的prj文件夹是config找到所有的文件
        LinearLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),5);
        mRnaFileRv.setLayoutManager(layoutManager);
        mRnaFileRv.addItemDecoration(new LinearSpacingItemDecoration(this, 6));
        hexFileAdapter = new FileNameAdapter(getApplicationContext());
        hexFileAdapter.setTemplateList(mHexFileList);
        mRnaFileRv.setAdapter(hexFileAdapter);
        hexFileAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                hexFileAdapter.setTemplateList(mHexFileList);
                hexFileAdapter.setSelectedPosition(position);
                click_hex = position;
                hexFileName = mHexFileList.get(position);
                // Log.i("AAA","HEX ITEM" + position + "hexFileName" + hexFileName);
                //更新UI
                isFirstRNACheck = true;
                mRnaFileTv.setText(hexFileName+".hex");
                openRNAFile( hexFileName);
            }
        });
    }

    private void initPcrFileRv(){
        mPCRFileList = new ArrayList<String>();
        //编辑的prj文件夹是config找到所有的文件
        LinearLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),5);
        mPcrFileRv.setLayoutManager(layoutManager);
        mPcrFileRv.addItemDecoration(new LinearSpacingItemDecoration(this, 6));
        pcrFileAdapter = new FileNameAdapter(getApplicationContext());
        pcrFileAdapter.setTemplateList(mPCRFileList);
        mPcrFileRv.setAdapter(pcrFileAdapter);
        pcrFileAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                pcrFileAdapter.setTemplateList(mPCRFileList);
                pcrFileAdapter.setSelectedPosition(position);
                click_position = position;
                pcrFileName = mPCRFileList.get(position);
                // Log.i("AAA","PCR ITEM" + position + "pcrFileName" + pcrFileName);
                //更新UI
                isFirstPCRCheck = true;
                mPcrFileTv.setText(pcrFileName + ".prj");
                openSLANExFile(pcrFileName);
            }
        });
    }


    private void initJudgeFileRv(){
        mJudgeFileList = new ArrayList<String>();
        mPeakFileList = new ArrayList<String>();
        //编辑的prj文件夹是config找到所有的文件
        LinearLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),5);
        mJudgeFileRv.setLayoutManager(layoutManager);
        mJudgeFileRv.addItemDecoration(new LinearSpacingItemDecoration(this, 6));
        judgeFileAdapter = new FileNameAdapter(getApplicationContext());
        //  judgeFileAdapter.setTemplateList(mJudgeFileList);
        mJudgeFileRv.setAdapter(judgeFileAdapter);
        judgeFileAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if(!isPeak){
                    judgeFileAdapter.setTemplateList(mJudgeFileList);
                    judgeFileAdapter.setSelectedPosition(position);
                    judge_ct_pos = position;
                    ctFileName = mJudgeFileList.get(position);
                    //更新UI
                    isCtCheck = true;
                    mJudgeFileTv.setText(ctFileName);
                    openJudgeFile(ctFileName);
                }else{
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    judgeFileAdapter.setSelectedPosition(position);
                    judge_peak_pos = position;
                    peakFileName = mPeakFileList.get(position);
                    isPeakCheck = true;
                    mPeakFileTv.setText(peakFileName);
                    openJudgeFile(peakFileName);
                }
            }
        });

        judgeFileAdapter.setOnItemLongClickListener(new FileNameAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position) {
                if(!isPeak){
                    judgeFileAdapter.setTemplateList(mJudgeFileList);
                    judgeFileAdapter.setSelectedPosition(-1);
                    judge_ct_pos = -1;
                    ctFileName ="";
                    //更新UI
                    isCtCheck = false;
                    mJudgeFileTv.setText(ctFileName);
                    mCtDetailLayout.setVisibility(View.VISIBLE);
                    mPeakDetailLayout.setVisibility(View.GONE);
                    if(ctRuleEntities != null){
                        ctRuleEntities.clear();
                        judgeCtAdapter.setJudgeCtList(ctRuleEntities);
                    }
                }else{
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    judgeFileAdapter.setSelectedPosition(-1);
                    judge_peak_pos = -1;
                    peakFileName = "";
                    isPeakCheck = false;
                    mPeakFileTv.setText(peakFileName);
                    mPeakDetailLayout.setVisibility(View.VISIBLE);
                    mCtDetailLayout.setVisibility(View.GONE);
                    if(null != peakRuleEntities){
                        peakRuleEntities.clear();
                        judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                    }
                }
            }
        });
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isEdit = getIntent().getBooleanExtra("isEdit",false);
        //模板名称
        tempLateName = getIntent().getStringExtra("templateName");

        if(isEdit){
            //模板的文件夹路径
            if(!StringUtil.isEmpty(tempLateName)){
                if(tempLateName.contains("~")){
                    String[] names = tempLateName.split("~");
                    if(names != null &&names.length>0){
                        switch (names.length){
                            case 1:
                                mTemplateEt.setText(names[0]);
                                break;
                            case 2:
                                mTemplateEt.setText(names[0]);
                                mSecondNameEt.setText(names[1]);
                                break;
                            case 3:
                                mTemplateEt.setText(names[0]);
                                mSecondNameEt.setText(names[1]);
                                mThirdNameEt.setText(names[2]);
                                break;
                        }

                    }
                }else{
                    mTemplateEt.setText(tempLateName);
                }
            }
        }else{
            String subFirName = "single_" + DateFormatUtil.getFileName();
            mTemplateEt.setText(subFirName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHexFile();
        updateJudgeFile();
        updatePCRFile();
        if(isEdit){
            String localTempPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + tempLateName+File.separator;
            List<File> editFiles = FileUtils.getFileList(localTempPath);
            if(null != editFiles &&editFiles.size()>0){
                for(File file :editFiles){
                    if(file.getName().endsWith(".hex")){
                        //核酸检测文件
                        hexFileName  = file.getName().substring(0,file.getName().length()-4);
                        //更新UI
                        isFirstRNACheck = true;
                        mRnaFileTv.setText(hexFileName+".hex");
                        int pos = getClickedPosition(hexFileName,mHexFileList);
                        click_hex = pos;
                        hexFileAdapter.setSelectedPosition(click_hex);
                        openRNAFile(hexFileName);
                    }else if(file.getName().endsWith(".prj")){
                        pcrFileName = file.getName().substring(0, file.getName().length() - 4);
                        isFirstPCRCheck = true;
                        mPcrFileTv.setText(pcrFileName + ".prj");
                        int pos = getClickedPosition(pcrFileName,mPCRFileList);
                        click_position = pos;
                        // Log.i("AAA","edit click" + click_position);

                    }else if(file.getName().endsWith(".ct")){
                        ctFileName = file.getName();
                        isCtCheck = true;
                        mJudgeFileTv.setText(ctFileName);
                        int pos = getClickedPosition(ctFileName,mJudgeFileList);
                        judge_ct_pos= pos;
                        // Log.i("AAA","edit judge click" + judge_click_position);
                    }else if(file.getName().endsWith(".peak")){
                        peakFileName = file.getName();
                        isPeakCheck = true;
                        mPeakFileTv.setText(peakFileName);
                        int pos = getClickedPosition(peakFileName,mPeakFileList);
                        judge_peak_pos = pos;

                    }else if(file.getName().equals("totaltime.txt")){
                        //全流程时间
                        TotalTime totalTime = ConfigManager.getTotalTimeFile(file);
                        mTotalTimeEt.setText(totalTime.getTotalTime()+"");
                        temparatureCtrlMode = totalTime.getTemperatureCtrlMode();
                        Log.i("LLL","temparatureCtrlMode" + temparatureCtrlMode);
                        if(temparatureCtrlMode == TemperatureControlMode.Fast.getValue()){
                            //快速
                            mTempCtrlFastRb.setChecked(true);
                        }else if(temparatureCtrlMode == TemperatureControlMode.Standard.getValue()){
                            //标准
                            mTempCtrlStandardRb.setChecked(true);
                        }
                    }
                }
            }
            isEdit = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mSLANSections){
            mSLANSections.clear();
            mSLANSections = null;
        }
        if(null != mPCRFileList){
            mPCRFileList.clear();
            mPCRFileList = null;
        }
        if(null != mHexFileList){
            mHexFileList.clear();
            mHexFileList = null;
        }
        if(null != mJudgeFileList){
            mJudgeFileList.clear();
            mJudgeFileList = null;
        }
        if(null != mPeakFileList){
            mPeakFileList.clear();
            mPeakFileList = null;
        }
        if(null != peakRuleEntities){
            peakRuleEntities.clear();
            peakRuleEntities = null;
        }
        if(null != ctRuleEntities){
            ctRuleEntities.clear();
            ctRuleEntities = null;
        }
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
        project = null;
    }

    private void updateHexFile(){

//        PcrApplication.rnaVersion = PreferenceUtil.getInstance(getApplicationContext())
//                .getStringValue(SharedConstant.RNA_SYSTEM,"V5");
        if(PcrApplication.rnaVersion.equals("V5")){
            CONFIG_HEX_PATH = HexFileName_V5.HEXCREATEFILE_PATH;
        }else{

        }
        File[] files = new File(CONFIG_HEX_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            mHexFileList.clear();
            for (int i = 0; i < files.length; i++) {
                if (null != mHexFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".hex")){
                        String name = files[i].getName().substring(0,files[i].getName().length()-4);
                        mHexFileList.add(name);
                        Collections.sort(mHexFileList);
                    }
                }
            }
        }
        hexFileAdapter.setTemplateList(mHexFileList);
    }

    private void updateJudgeFile(){
        mPeakFileList.clear();
        mJudgeFileList.clear();
        File[] files = new File(FileUtils.CONFIG_PATH + File.separator + "judge").listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {

            for (int i = 0; i < files.length; i++) {
                if (null != mPeakFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".peak")){
                        String name = files[i].getName();
                        mPeakFileList.add(name);
                        Collections.sort(mPeakFileList);
                    }
                }
                if (null != mJudgeFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".ct")){
                        String name = files[i].getName();
                        mJudgeFileList.add(name);
                        Collections.sort(mJudgeFileList);
                    }
                }
            }
        }
        if(isPeak){
            judgeFileAdapter.setTemplateList(mPeakFileList);
        }else {
            judgeFileAdapter.setTemplateList(mJudgeFileList);
        }
    }

    private void updatePCRFile(){
        mPCRFileList.clear();
        File[] files = new File(FileUtils.CONFIG_SLAN_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != mPCRFileList) {
                    if (files[i].getName() != null && files[i].getName().contains(".prj")) {
                        String name = files[i].getName().substring(0, files[i].getName().length() - 4);
                        mPCRFileList.add(name);
                        Collections.sort(mPCRFileList);
                    }
                }
            }
        }
        pcrFileAdapter.setTemplateList(mPCRFileList);
    }

    //保存单重模板
    private void saveSingleTemplateAction(String templateName){
        //首先創建保存的文件夾
        String subFirName = templateName;
        File dir = new File(CONFIG_FULLPRC_PCR_TEMPLATE_PATH, subFirName);
        if (!dir.exists()) {
            dir.mkdirs();
            saveSingleTemplate(subFirName);
        }else{
            showNoticeDialog(getResources().getString(R.string.create_multi_dialog_message_template_exists),subFirName,0);
        }
    }

    private void saveSingleTemplate(String subFirName){
        //首先保存prj文件
        File dir = new File(CONFIG_FULLPRC_PCR_TEMPLATE_PATH, subFirName);
        boolean copyPcrResult = false ;
        boolean copyCtResult = false;
        boolean copyPeakResult = false;
        boolean copyHexResult = false;
        boolean saveTotalResult = saveTotalTime(subFirName);
        if(!StringUtil.isEmpty(pcrFileName)){
            String newPcrPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator
                    +pcrFileName+".prj";
            String oldPcrPath = FileUtils.CONFIG_SLAN_PATH + File.separator
                    +pcrFileName +".prj";
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyPcrResult=  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        if(!StringUtil.isEmpty(ctFileName)){
            String newPcrPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator
                    + ctFileName;
            Log.i("CreateSingleActivity" ,"111----" +newPcrPath );
            String oldPcrPath = CONFIG_JUDGE_PATH +ctFileName;
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyCtResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        if(!StringUtil.isEmpty(peakFileName)){
            String newPcrPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator
                    + peakFileName;
            Log.i("CreateSingleActivity" ,"222----" +newPcrPath );
            String oldPcrPath = CONFIG_JUDGE_PATH +peakFileName;
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyPeakResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        if(!StringUtil.isEmpty(hexFileName)){
            String newPcrPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator
                    + hexFileName+".hex";
            String oldPcrPath =  CONFIG_HEX_PATH +hexFileName +".hex";
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyHexResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }



        if(copyPcrResult && copyHexResult && saveTotalResult){
            if(!StringUtil.isEmpty(ctFileName) && !StringUtil.isEmpty(peakFileName)){
                if(copyCtResult && copyPeakResult){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else if(!StringUtil.isEmpty(ctFileName) && StringUtil.isEmpty(peakFileName)){
                if(copyCtResult){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else if(StringUtil.isEmpty(ctFileName) && !StringUtil.isEmpty(peakFileName)){
                if(copyPeakResult){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
            }

        }else{
            Log.i("CreateSingleActivity","copyHexResult" + copyHexResult);
            Log.i("CreateSingleActivity","copyPcrResult" + copyPcrResult);
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
            if(dir.exists()){
                FileUtils.remove(dir);
                //删除文件夹以及文件
            }
        }
    }

    /**
     * 保存总的模板运行时间与控温方式TemperatureControlMode
     * @param subFirName
     * @return
     */
    private boolean saveTotalTime(String subFirName){
        String savePath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator;
        String time = mTotalTimeEt.getText().toString().trim();
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();


        long l_time =  Long.parseLong(time);
        TotalTime totalTime = new TotalTime();
        totalTime.setTotalTime(l_time);
        totalTime.setFirstRNATime(firstRnaTime);
        totalTime.setFirstPcrTime(firstPcrTime);
        totalTime.setTemperatureCtrlMode(temparatureCtrlMode);
        String totalTime_str = gson.toJson(totalTime);
        String fileName = "totaltime";
        return  FileUtils.saveFileInSdcard(totalTime_str,savePath,fileName,".txt");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.single_create_save_btn){
            if(isFirstPCRCheck && isFirstRNACheck ){
                String templateName = mTemplateEt.getText().toString().trim();
                String secondName = mSecondNameEt.getText().toString().trim();
                String thirdName = mThirdNameEt.getText().toString().trim();
                String totalTime = mTotalTimeEt.getText().toString().trim();
                if(!StringUtil.isEmpty(templateName)
                        && !StringUtil.isEmpty(totalTime)){
                    if(StringUtil.isEmpty(secondName)&& StringUtil.isEmpty(thirdName)){
                        String name = templateName ;
                        saveSingleTemplateAction(name);
                    }else{
                        String name = templateName + "~"+ secondName + "~" + thirdName;
                        saveSingleTemplateAction(name);
                    }

                }else{
                    if(StringUtil.isEmpty(templateName)){
                        //模板名称不能为空
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.create_multi_toast_template_name),Toast.LENGTH_LONG).show();
                    }

                    if(StringUtil.isEmpty(totalTime)){
                        //全流程时间不能为空
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.create_multi_toast_running_time),Toast.LENGTH_LONG).show();
                    }
                }
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.create_multi_toast_template_imcomplete),Toast.LENGTH_LONG).show();
            }
        }else if(id ==R.id.single_create_pcr_create_tv){
            Intent createHexIntent = new Intent(CreateSingleActivity.this, ConfigSLANActivity.class);
            startActivity(createHexIntent);
        }else if(id == R.id.single_create_hex_create_tv){
            Intent createHexIntent = new Intent(CreateSingleActivity.this, ConfigHexActivity.class);
            startActivity(createHexIntent);
        }else if(id == R.id.single_create_ct_create_tv){
            Intent createCtIntent = new Intent(CreateSingleActivity.this, JudgeCtActivity.class);
            startActivity(createCtIntent);
        }else if(id == R.id.single_create_peak_create_tv){
            Intent createPeakIntent = new Intent(CreateSingleActivity.this, PeakJudgeActivity.class);
            startActivity(createPeakIntent);
        }
    }

    /**
     * 打开显示判别文件详情
     * @param fileName
     */
    private void openJudgeFile(String fileName){
        if(!StringUtil.isEmpty(fileName)){
            File file = new File(FileUtils.CONFIG_PATH+ File.separator + "judge/" + fileName);
            if(fileName.endsWith(".ct")){
                if(null != ctRuleEntities){
                    ctRuleEntities.clear();
                    ctRuleEntities = CTRuleEntity.getCTRuleList(file);
                    judgeCtAdapter.setJudgeCtList(ctRuleEntities);
                    judgeCtAdapter.notifyDataSetChanged();
                }
            }else if(fileName.endsWith(".peak")){
                if(null != peakRuleEntities){
                    peakRuleEntities.clear();
                    peakRuleEntities = PEAKRuleEntity.getPEAKRuleList(file);
                    judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                    judgePeakAdapter.notifyDataSetChanged();
                }

            }

        }

    }

    /**
     * 核酸提取文件夹详情
     * @param fileName
     */
    /**
     * 查看核酸检测文件
     * @param fileName
     */
    private void openRNAFile(String fileName){
        if(!StringUtil.isEmpty(fileName)){
            //先判断是V4版本还是V5版本
            String path = null;
            if(PcrApplication.rnaVersion.equals("V4")){
                //从/mnt/sdcard/PCR/hexCreateFile_v4/hexConfig_v4打开所需要的文件

            }else if(PcrApplication.rnaVersion.equals("V5")){
                //从/mnt/sdcard/PCR/hexCreateFile_v5/hexConfig_v5打开所需要的文件
                path = HexFileName_V5.HEXCONFIG_PATH + fileName + ".hexConfig";
            }

            if(null != path){
                File file = new File(path);

                if(file != null && file.exists()&& file.isFile()){
                    HexConfig hexConfig = ConfigManager.getHexConfigFile(file);
                    initHexVaribale();
                    updateHexFileUI(hexConfig);
                }
            }else{
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.config_hex_toast_cannot_open_file),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initHexVaribale(){
        mMegneticsampleTv.setText("");
        //裂解温度
        mSpiltTempTv.setText("");

        //裂解液体积
        mLysisTv.setText("");
        //裂解时间
        mSpiltTimeTv.setText("");
        mWashTemp_1Tv.setText("");
        mWashVolume_1Tv.setText("");
        mWashTime_1Tv.setText("");

        //三部洗涤
        mWashTemp_2Tv.setText("");
        mWashVolume_2Tv.setText("");
        mWashTime_2Tv.setText("");

        mWashTemp_3Tv.setText("");
        mWashVolume_3Tv.setText("");
        mWashTime_3Tv.setText("");


        mElutionTempTv.setText("");
        mElutionVolumeTv.setText("");
        mElutionTimeTv.setText("");
        mQuantifyVolumeTv.setText("");
    }

    private void updateHexFileUI(HexConfig hexConfig){
        if(null != hexConfig){
            firstRnaTime = TimerCaculate.RNA_Fisrt_Caculate(hexConfig);
            if(hexConfig.getMagneticChoice() == 0){
                magneticlive_tv.setText(getResources().getString(R.string.config_hex_step_2_one));
            }else if(hexConfig.getMagneticChoice() == 1){
                magneticlive_tv.setText(getResources().getString(R.string.config_hex_step_2_two));
            }

            if(hexConfig.getFreeze_drying() == 0){
                //液态体系
                mLiquidSysTv.setText(R.string.config_hex_liquid_system);
            }else if(hexConfig.getFreeze_drying() == 1){
                //冻干体系
                mLiquidSysTv.setText(R.string.config_hex_freeze_drying_system);
            }

            //磁珠活化样样本体积
            mMegneticsampleTv.setText(hexConfig.getMagneticVolume()+"");
            //裂解温度
            mSpiltTempTv.setText(hexConfig.getSplitTemperature()+"");

            //裂解液体积
            mLysisTv.setText(hexConfig.getSplitLieJieYeVolume()+"");
            //裂解时间
            mSpiltTimeTv.setText(hexConfig.getSplitTime()+"");
            mWashTemp_1Tv.setText(hexConfig.getWash1Temperature()+"");
            mWashVolume_1Tv.setText(hexConfig.getWash1XiDiYeVolume()+"");
            mWashTime_1Tv.setText(hexConfig.getWash1Time()+"");
            //卡盒入位
            if(hexConfig.isCasseteFlag()){
                mCloseDoorCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mCloseDoorCkImg.setImageResource(R.drawable.check_normal);
            }
            //磁珠活化
            if(hexConfig.isMagneticFlag()){
                mMegeticCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mMegeticCkImg.setImageResource(R.drawable.check_normal);
            }
            if(hexConfig.isSplitFlag()){
                mSpliteCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mSpliteCkImg.setImageResource(R.drawable.check_normal);
            }


            if(hexConfig.isElutionFlag()){
                mElutionCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mElutionCkImg.setImageResource(R.drawable.check_normal);
            }
            if(hexConfig.isQuantifyFlag()){
                mQuantifyCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mQuantifyCkImg.setImageResource(R.drawable.check_normal);
            }
            if(hexConfig.isSecondQuantifyFlag()){
                mSecondCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mSecondCkImg.setImageResource(R.drawable.check_normal);
            }
            if(hexConfig.isWash1Flag()){
                mWash1CkImg.setImageResource(R.drawable.check_pressed);
            }else {
                mWash1CkImg.setImageResource(R.drawable.check_normal);
            }

            if(hexConfig.getWashStep() == 3){
                //三部洗涤
                mTwoWashLayout.setVisibility(View.VISIBLE);
                mThreeWashLayout.setVisibility(View.VISIBLE);
                mWashTemp_2Tv.setText(hexConfig.getWash32Temperature()+"");
                mWashVolume_2Tv.setText(hexConfig.getWash32XiDiYeVolume()+"");
                mWashTime_2Tv.setText(hexConfig.getWash32Time()+"");
                mWashTemp_3Tv.setText(hexConfig.getWash33Temperature()+"");
                mWashVolume_3Tv.setText(hexConfig.getWash33XiDiYeVolume()+"");
                mWashTime_3Tv.setText(hexConfig.getWash33Time()+"");
                if(hexConfig.isWash32Flag()){
                    mWash2CkImg.setImageResource(R.drawable.check_pressed);
                }else {
                    mWash2CkImg.setImageResource(R.drawable.check_normal);
                }
                if(hexConfig.isWash33Flag()){
                    mWash3CkImg.setImageResource(R.drawable.check_pressed);
                }else {
                    mWash3CkImg.setImageResource(R.drawable.check_normal);
                }
            }else if(hexConfig.getWashStep() == 2){

                mTwoWashLayout.setVisibility(View.VISIBLE);
                mThreeWashLayout.setVisibility(View.GONE);
                mWashTemp_2Tv.setText(hexConfig.getWash22Temperature()+"");
                mWashVolume_2Tv.setText(hexConfig.getWash22XiDiYeVolume()+"");
                mWashTime_2Tv.setText(hexConfig.getWash22Time()+"");

                if(hexConfig.isWash22Flag()){
                    mWash2CkImg.setImageResource(R.drawable.check_pressed);
                }else {
                    mWash2CkImg.setImageResource(R.drawable.check_normal);
                }

            }else if(hexConfig.getWashStep() == 1){
                mTwoWashLayout.setVisibility(View.GONE);
                mThreeWashLayout.setVisibility(View.GONE);

            }
            mElutionTempTv.setText(hexConfig.getElutionTemperature()+"");
            mElutionVolumeTv.setText(hexConfig.getElutionVolume()+"");
            mElutionTimeTv.setText(hexConfig.getElutionTime()+"");
            mQuantifyVolumeTv.setText(hexConfig.getQuantifyVolume()+"");
        }
        mTotalTimeEt.setText(firstRnaTime+firstPcrTime+"");
    }

    private  void openSLANExFile(String fileName){
        //打开文件夹
        if(!StringUtil.isEmpty(fileName)){
            ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
            if(null != mConfigProjects && mConfigProjects.size()>0){
                for(Project project :mConfigProjects){
                    if(null != project){
                        if(project.getName().equals(fileName)){
                            Program program = project.getProgram();
                            //转化为String
                            this.project = project;
                            firstPcrTime = TimerCaculate.PCR_Caculate(project);
                            ArrayList<Section> sections = program.getSections();
                            if(null != sections){
                                getSLANSections(sections);
                            }
                        }
                    }
                }

                updateProgramUI();

            }
        }
    }

    /**
     * 更新程序段UI
     */
    private void updateProgramUI(){
        if(null != mSLANSections){
            if(mSLANSections.size() == 0){
                if(null != mProgramTitleLayout){
                    mProgramTitleLayout.removeAllViews();
                }
                configSLANAdapter.setProgramSteps(mSLANSections);
                if(null != mProgramRecycleLayout){
                    mProgramRecycleLayout.removeAllViews();
                }
            }else if(mSLANSections.size()>0){
                if(null != mProgramTitleLayout){
                    mProgramTitleLayout.removeAllViews();
                }
                for(int i = 0; i<mSLANSections.size();i++){
                    TextView textView = new TextView(getApplicationContext());
                    textView.setBackgroundResource(R.color.blue);
                    textView.setTextSize(20);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(0xffffffff);

                    int programType = mSLANSections.get(i).getProgramType();
                    if(programType == Constants.PROGRAM_TYPE_AMP){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(162*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(1, 0, 1, 0);
                        textView.setLayoutParams(layoutParams);
                        int index = i+1;
                        textView.setText(getResources().getString(R.string.runing_pcr_section)+ index);
                    }else if(programType == Constants.PROGRAM_TYPE_MELT){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(1, 0, 1, 0);
                        textView.setLayoutParams(layoutParams);
                        textView.setText(getResources().getString(R.string.runing_pcr_section_melt));
                    }
                    mProgramTitleLayout.addView(textView);
                }

                configSLANAdapter.setProgramSteps(mSLANSections);

                if(null != mProgramRecycleLayout){
                    mProgramRecycleLayout.removeAllViews();
                }
                for(int i = 0; i<mSLANSections.size();i++){
                    TextView textView = new TextView(getApplicationContext());
                    textView.setBackgroundResource(R.color.color_recycle);
                    textView.setTextSize(18);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(0xffffffff);


                    int programType = mSLANSections.get(i).getProgramType();
                    if(programType == Constants.PROGRAM_TYPE_AMP){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(162*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(1, 0, 1, 0);
                        textView.setLayoutParams(layoutParams);
                        textView.setText(getResources().getString(R.string.running_pcr_recycle_num) + (mSLANSections.get(i).getCycles()));
                    }else if(programType == Constants.PROGRAM_TYPE_MELT){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(1, 0, 1, 0);
                        textView.setLayoutParams(layoutParams);
                        int scanmode = mSLANSections.get(i).getScanMode();
                        if(scanmode == Constants.MELT_SCANMODE_CONTINUE){
                            textView.setText(getResources().getString(R.string.ruuning_pcr_scanmode_continuous) + " "
                                    + getResources().getString(R.string.ruuning_pcr_rate) + nf.format(mSLANSections.get(i).getRate())+"℃/s" );
                        }else if(scanmode == Constants.MELT_SCANMODE_STEP){
                            textView.setText(getResources().getString(R.string.ruuning_pcr_scanmode_step) + " "
                                    + getResources().getString(R.string.ruuning_pcr_interval) + nf.format(mSLANSections.get(i).getInterval())+"℃" + " "
                                    + getResources().getString(R.string.ruuning_pcr_melt_holdtime) + mSLANSections.get(i).getMeltHoldtime()+"s");
                        }
                    }
                    mProgramRecycleLayout.addView(textView);

                }
            }
            //更新参数
        }
        mTotalTimeEt.setText(firstRnaTime+firstPcrTime+"");
    }

    private void getSLANSections(ArrayList<Section> sections){
        if(sections.size()>0){
            mSLANSections.clear();
            for(int i= 0;i<sections.size();i++){
                Section section = sections.get(i);
                if(section != null){
                    if(section instanceof SectionAMP){
                        SectionAMP sectionAMP = (SectionAMP) section;
                        ConfigSLANSection slanSection = new ConfigSLANSection();
                        slanSection.setName(sectionAMP.getName());
                        slanSection.setCycles(sectionAMP.getCycles());
                        slanSection.setIdx(sectionAMP.getIdx());
                        slanSection.setProgramType(Constants.PROGRAM_TYPE_AMP);//扩增
                        ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<ConfigSLANSection.Step>();
                        if(sectionAMP.getItems() != null && sectionAMP.getItems().size()>0){
                            for(int k=0;k<sectionAMP.getItems().size();k++){
                                Step step = sectionAMP.getItems().get(k);
                                if(null != step){
                                    ConfigSLANSection.Step slanStep = new ConfigSLANSection.Step();
                                    slanStep.setStepName(step.getName());
                                    slanStep.setTemperature(step.getTemperature());
                                    slanStep.setHoldTime(step.getHoldTime());
                                    slanStep.setStepType(Constants.PROGRAM_TYPE_AMP);
                                    slanStep.setStep_index(k);
                                    slanStep.setAcquisition(step.getIsAcquisition());
                                    slanStep.setSectionIndex(sectionAMP.getIdx());
                                    slanItems.add(slanStep);
                                }
                            }
                        }
                        slanSection.setItems(slanItems);
                        mSLANSections.add(slanSection);
                    }else if(section instanceof SectionMelt){
                        SectionMelt sectionMelt = (SectionMelt) section;
                        ConfigSLANSection slanSection = new ConfigSLANSection();
                        slanSection.setName(sectionMelt.getName());
                        slanSection.setIdx(sectionMelt.getIdx());
                        slanSection.setProgramType(Constants.PROGRAM_TYPE_MELT);//熔解段
                        slanSection.setRate(sectionMelt.getRate());
                        slanSection.setScanMode(sectionMelt.getScanMode());
                        slanSection.setMeltHoldtime(sectionMelt.getHoldTime());
                        slanSection.setInterval(sectionMelt.getInterval());

                        ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<>();
                        ConfigSLANSection.Step meltStep = new ConfigSLANSection.Step();
                        meltStep.setStepName("");
                        meltStep.setLeftTemperature(sectionMelt.getStartTemp());
                        meltStep.setRightTemperature(sectionMelt.getEndTemp());
                        meltStep.setSectionIndex(sectionMelt.getIdx());
                        meltStep.setStep_index(0);
                        meltStep.setAcquisition(true);
                        meltStep.setTemperature(sectionMelt.getEndTemp());
                        meltStep.setHoldTime(sectionMelt.getHoldTime());
                        meltStep.setStepType(Constants.PROGRAM_TYPE_MELT);
                        slanItems.add(meltStep);
                        slanSection.setItems(slanItems);
                        mSLANSections.add(slanSection);
                    }
                }

            }
        }else{
            mSLANSections.clear();
        }
    }

    ////////////////////////////////////编辑文件夹///////////////////////////////////////




    private void showNoticeDialog(String message,String subFirName,int dialogType){
        if(null != noticeDialog ){
            noticeDialog.dismiss();
            noticeDialog =null;
        }
        if(null == noticeDialog){
            noticeDialog =new NoticeDialog(this,R.style.Dialog);
            noticeDialog.setMessage(message+ subFirName);
            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    noticeDialog.dismiss();
                    if(dialogType == 0) {
                        //
                        //清空文件夹中的文件
                        File dir = new File(CONFIG_FULLPRC_PCR_TEMPLATE_PATH, subFirName);
                        File[] files = dir.listFiles();//将file子目录及子文件放进文件数组
                        if (files != null) {//如果包含文件进行删除操作
                            for (int i = 0; i < files.length; i++) {
                                if (files[i].isFile()) {//删除子文件
                                    files[i].delete();
                                }
                            }
                        }
                        saveSingleTemplate(subFirName);
                    }
                }
            });

            noticeDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                @Override
                public void onCancelClick() {
                    noticeDialog.dismiss();
                }
            });
            noticeDialog.show();
        }
    }

    private void createFiles(){
        File createSlanDir = new File(FileUtils.CONFIG_SLAN_PATH);
        if(!createSlanDir.exists()){
            createSlanDir.mkdirs();
        }

        File createJudgeDir = new File(FileUtils.CONFIG_PATH + File.separator + "judge");
        if(!createJudgeDir.exists()){
            createJudgeDir.mkdirs();
        }
    }
}
