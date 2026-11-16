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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateMultiActivity extends BaseActivity implements View.OnClickListener{
    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    private DecimalFormat nf = new DecimalFormat( "0.00");
    private RadioGroup mTempCtrlModeRg;
    private RadioButton mTempCtrlFastRb,mTempCtrlStandardRb;
    private int temparatureCtrlMode = TemperatureControlMode.Fast.getValue();
    private EditText mTemplateEt,mTotalTimeEt,mSecondNameEt,mThirdNameEt;
    private boolean isFirstRNACheck,isFirstPCRCheck,isCtCheck,isSecondPCRCheck,isJudgePeak= false;
    private boolean isSecondRNACheck = true;
    private CustomImageText mFirstRNATabTv,mFirstPCRTabTv,mSecondRNATabTv,mSecondPCRTabTv,mPeakTabTv,mCtTabTv;
    private RelativeLayout mRNAFileLayout,mPCRFileLayout,mJudgeFileLayout;
    private RecyclerView mRnaFileRv,mPcrFileRv,mJudgeFileRv;
    private TextView mSaveTv,mFirstRNAFileTv,mFirstPcrFileTv,mSecondRNAFileTv,mSecondPcrFileTv;
    private TextView mCreatePcrTv,mCreateHexTv,mCreateCtTv,mCreatePeakTv;

    //判别文件
    private RecyclerView mPeakDetailRv;//判别文件的详情列表
    private ListView mCtFileDetailLv;
    private JudgePeakAdapter judgePeakAdapter;//多重判别文件lv
    private JudgeCtAdapter judgeCtAdapter;
    private List<PEAKRuleEntity> peakRuleEntities;
    private List<CTRuleEntity> ctRuleEntities;
    private RelativeLayout mCtDetailLayout;//显示Ct文件详情
    private RelativeLayout mPeakDetailLayout;////显示Peak文件详情
    //

    private List<String> mPCRFileList,mHexFileList;//上方显示的文件列表
    private List<String> mPeakFileList,mCtFileList;
    private FileNameAdapter pcrFileAdapter,judgeFileAdapter,hexFileAdapter;
    private ConfigSLANAdapter configSLANAdapter;
    private List<ConfigSLANSection>mSLANSections;
    private RecyclerView programRv;//配置的所有的项目的列表
    private LinearLayout mProgramTitleLayout;
    private LinearLayout mProgramRecycleLayout;

    private int click_first_pcr_pos = -1,click_first_hex_pos = -1;
    private int click_sec_pcr_pos = -1;
    //click_sec_hex_pos = -1;
    private int click_ct_pos = -1,click_peak_pos = -1;
    private boolean isFirstHex = true;
    private boolean isFirstPCR = true;
    private boolean isPeak = true;
    private String firstPCRName,secondPcrName,ctFileName,firstHexFileName,secHexFileName,peakFileName;

    //核酸提取
    //1:磁珠活化样本体积,2:裂解温度,3:裂解样本体积,4裂解液体积，5裂解时间
    private TextView mMegneticsampleTv,mSpiltTempTv,mLysisTv,mSpiltTimeTv;
    private TextView mWashTemp_1Tv,mWashVolume_1Tv,mWashTime_1Tv,mWashTemp_2Tv,mWashVolume_2Tv,mWashTime_2Tv
            ,mWashTemp_3Tv,mWashVolume_3Tv,mWashTime_3Tv;

    private TextView magneticlive_tv;

    //洗脱,洗脱液体积
    private TextView mElutionTempTv,mElutionVolumeTv,mElutionTimeTv;
    private TextView mQuantifyVolumeTv;//体积定量
    private ImageView mCloseDoorCkImg,mMegeticCkImg,mSpliteCkImg,mWash1CkImg,mWash2CkImg,mWash3CkImg,
            mElutionCkImg,mQuantifyCkImg,mSecondCkImg;
    private LinearLayout mTwoWashLayout,mThreeWashLayout;
    //液态体系
    private TextView mLiquidSysTv;

    private String CONFIG_JUDGE_PATH = FileUtils.CONFIG_PATH + File.separator + "judge"
            +File.separator;
    private String CONFIG_HEX_PATH = HexFileName_V5.HEXCREATEFILE_PATH;//默认是V5
    private String CONFIG_HEX_SECOND_PATH = HexFileName_V5.HEXFILE_PATH;
    private NoticeDialog noticeDialog;
    private boolean isEditMode = false;
    private String tempLateName= "";
    private String CONFIG_FULLPRC_PCR_TEMPLATE_PATH = FileUtils.CONFIG_TEMPLATE_PATH + "fullProcess" +File.separator;
    private int firstRnaTime ,firstPcrTime,secondRnaTime,secondPcrTime;

    //配置判别文件
    private TextView peakHole1Tv,peakHole2Tv;
    private TextView peakOneTv,peakTwoTv,
            peakOneFileTv,peakTwoFileTv;

    private TextView ctHole1Tv,ctHole2Tv;
    private TextView ctOneTv,ctTwoTv, ctOneFileTv,ctTwoFileTv;
    private Map<Integer, String> holeCtFileNameMap = new HashMap<>();
    // 同理，peak文件的映射
    private Map<Integer, String> holePeakFileNameMap = new HashMap<>();
    private List<TextView> peakHoleTvs,ctHoleTvs;
    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_create_multi;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        //初始化标题栏
        initTitleBar();
        //初始化输入框
        initEditTexts();
        //初始化控温方式选择
        initTempControlMode();
        //初始化核心相关布局
        initCoreViews();
        //判别文件详情
        initJudgeFileViews();

        //核酸提取详情
        initHexViews();
        //初始化孔位选择UI
        initHoleSelectViews();
        createFiles();
        initData();
    }

    /**
     * 初始化标题栏
     */
    private void initTitleBar(){
        mTitleLayout = (RelativeLayout)findViewById(R.id.multi_create_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.create_multi_title));
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
    }

    /**
     * 初始化输入框
     */
    private void initEditTexts(){
        mTotalTimeEt = (EditText)findViewById(R.id.multi_create_totaltime_et);
        mTemplateEt = (EditText)findViewById(R.id.multi_create_template_name_et) ;
        mTemplateEt.addTextChangedListener(new LimitInputTextWatcher(mTemplateEt));
        mSecondNameEt = (EditText)findViewById(R.id.multi_create_template_name_second_et) ;
        mSecondNameEt.addTextChangedListener(new LimitInputTextWatcher(mSecondNameEt));
        mThirdNameEt = (EditText)findViewById(R.id.multi_create_template_name_third_et) ;
        mThirdNameEt.addTextChangedListener(new LimitInputTextWatcher(mThirdNameEt));
    }

    /**
     *   初始化控温方式选择
     */
    private void initTempControlMode(){
        mTempCtrlModeRg = (RadioGroup)findViewById(R.id.multi_create_tempctrl_mode_rg);
        mTempCtrlFastRb = (RadioButton) findViewById(R.id.multi_create_tempctrl_fast);
        mTempCtrlStandardRb= (RadioButton)findViewById(R.id.multi_create_tempctrl_standard);
        mTempCtrlModeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkId) {
                if(checkId == R.id.multi_create_tempctrl_fast){
                    temparatureCtrlMode = TemperatureControlMode.Fast.getValue();
                }else if(checkId == R.id.multi_create_tempctrl_standard){
                    temparatureCtrlMode = TemperatureControlMode.Standard.getValue();
                }
            }
        });
    }
    //初始化核心相关布局
    private void initCoreViews(){
        mRNAFileLayout = (RelativeLayout) findViewById(R.id.multi_create_rna_file_layout);
        mPCRFileLayout = (RelativeLayout) findViewById(R.id.multi_create_pcr_layout);
        mJudgeFileLayout = (RelativeLayout) findViewById(R.id.multi_create_peak_file_layout);
        mFirstRNATabTv = (CustomImageText)findViewById(R.id.multi_create_first_rna_title_tv);
        mFirstPCRTabTv = (CustomImageText)findViewById(R.id.multi_create_first_pcr_title_tv);
        mSecondRNATabTv = (CustomImageText)findViewById(R.id.multi_create_second_rna_title_tv);
        mSecondPCRTabTv = (CustomImageText)findViewById(R.id.multi_create_second_pcr_title_tv);
        mCtTabTv= (CustomImageText)findViewById(R.id.multi_create_judge_title_tv);
        mPeakTabTv = (CustomImageText) findViewById(R.id.multi_create_peak_title_tv);

        mCreateHexTv = (TextView)findViewById(R.id.multi_create_hex_create_tv);
        mCreatePcrTv = (TextView)findViewById(R.id.multi_create_pcr_create_tv);
        mCreateCtTv = (TextView)findViewById(R.id.multi_create_ct_create_tv);
        mCreatePeakTv = (TextView)findViewById(R.id.multi_create_peak_create_tv);
        mRnaFileRv = (RecyclerView)findViewById(R.id.multi_create_rna_list);
        mPcrFileRv = (RecyclerView)findViewById(R.id.multi_create_pcr_list);
        mJudgeFileRv = (RecyclerView)findViewById(R.id.multi_create_judge_list);

        mSaveTv = (TextView)findViewById(R.id.multi_create_save_btn);
        mFirstRNAFileTv = (TextView)findViewById(R.id.multi_create_first_rna_name_tv);
        mFirstPcrFileTv = (TextView)findViewById(R.id.multi_create_first_pcr_name_tv);
        mSecondRNAFileTv = (TextView)findViewById(R.id.multi_create_second_rna_name_tv);
        mSecondPcrFileTv = (TextView)findViewById(R.id.multi_create_second_pcr_name_tv);

        programRv = (RecyclerView) findViewById(R.id.multi_create_slan_program_rv);
        mProgramTitleLayout = (LinearLayout) findViewById(R.id.multi_create_program_title_layout);
        mProgramRecycleLayout = (LinearLayout) findViewById(R.id.multi_create_program_recycle_layout);

    }

    /**
     * 判别文件详情
     */
    private void initJudgeFileViews(){
        mPeakDetailLayout = (RelativeLayout)findViewById(R.id.config_judge_multi_peak_detail_layout);
        peakRuleEntities = new ArrayList<PEAKRuleEntity>();
        ctRuleEntities = new ArrayList<CTRuleEntity>();
        mPeakDetailRv = (RecyclerView)findViewById(R.id.multi_create_judge_peak_list_detail);
        mPeakDetailRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,
                false));
        judgePeakAdapter = new JudgePeakAdapter(getApplicationContext());
        mPeakDetailRv.setAdapter(judgePeakAdapter);
        mCtDetailLayout = (RelativeLayout)findViewById(R.id.config_judge_multi_ct_detail_layout);
        mCtFileDetailLv = (ListView)findViewById(R.id.multi_create_judge_ct_list_detail) ;
        judgeCtAdapter = new JudgeCtAdapter(getApplicationContext());
        mCtFileDetailLv.setAdapter(judgeCtAdapter);
    }

    /**
     * 初始化核酸提取详情
     */
    private void initHexViews(){
        mMegneticsampleTv = (TextView)findViewById(R.id.multi_create_tv_megneticsample);
        mSpiltTempTv = (TextView)findViewById(R.id.multi_create_splittemperature_tv);
        mLysisTv = (TextView)findViewById(R.id.multi_create_splitlijieye_tv);
        mSpiltTimeTv = (TextView)findViewById(R.id.multi_create_splittime_tv);
        mWashTemp_1Tv = (TextView)findViewById(R.id.multi_create_wash1temp_tv);
        mWashVolume_1Tv = (TextView)findViewById(R.id.multi_create_wash1xidiye_tv);
        mWashTime_1Tv = (TextView)findViewById(R.id.multi_create_wash1time_tv);
        mWashTemp_2Tv = (TextView)findViewById(R.id.multi_create_wash2_2temperature_tv);
        mWashVolume_2Tv = (TextView)findViewById(R.id.multi_create_wash2_2xidiye_tv);
        mWashTime_2Tv = (TextView)findViewById(R.id.multi_create_wash2_2time);
        mWashTemp_3Tv = (TextView)findViewById(R.id.multi_create_wash3_3temperature_tv);
        mWashVolume_3Tv = (TextView)findViewById(R.id.multi_create_wash3_3xidiye_tv);
        mWashTime_3Tv = (TextView)findViewById(R.id.multi_create_wash3_3time_tv);
        mElutionTempTv = (TextView)findViewById(R.id.multi_create_elutiontemperature_tv);
        mElutionVolumeTv = (TextView)findViewById(R.id.multi_create_elutionxituoye_tv);
        mElutionTimeTv = (TextView)findViewById(R.id.multi_create_elutiontime_tv);
        mQuantifyVolumeTv = (TextView)findViewById(R.id.multi_create_quantifyvolume);
        mLiquidSysTv = (TextView) findViewById(R.id.multi_freeze_dry_tv);
        magneticlive_tv = (TextView)findViewById(R.id.magneticlive_tv);

        mCloseDoorCkImg = (ImageView)findViewById(R.id.multi_create_closedoor_ck_img);
        mMegeticCkImg = (ImageView) findViewById(R.id.multi_create_megnetic_ck_img);
        mSpliteCkImg = (ImageView) findViewById(R.id.multi_create_split_ck_img);
        mWash1CkImg =(ImageView) findViewById(R.id.multi_create_wash1_ck_img);
        mWash2CkImg=(ImageView) findViewById(R.id.multi_create_wash2_ck_img);
        mWash3CkImg=(ImageView) findViewById(R.id.multi_create_wash3_ck_img);
        mElutionCkImg=(ImageView) findViewById(R.id.multi_create_elution_ck_img);
        mQuantifyCkImg=(ImageView) findViewById(R.id.multi_create_quantify_ck_img);
        mSecondCkImg=(ImageView) findViewById(R.id.multi_create_second_ck_img);
        mTwoWashLayout = (LinearLayout)findViewById(R.id.multi_create_layout_wash2);
        mThreeWashLayout  = (LinearLayout)findViewById(R.id.multi_create_layout_wash3_3);

    }
    /**
     * 初始化孔位选择UI
     */
    private void initHoleSelectViews(){
        ctHole1Tv = (TextView)findViewById(R.id.multi_ct_hole1_tv);
        ctHole2Tv = (TextView)findViewById(R.id.multi_ct_hole2_tv);
        ctOneTv = (TextView)findViewById(R.id.ct_hole_content_1_tv);
        ctTwoTv = (TextView)findViewById(R.id.ct_hole_content_2_tv);
        ctOneFileTv = (TextView)findViewById(R.id.multi_full_ct_1_name_tv) ;
        ctTwoFileTv = (TextView)findViewById(R.id.multi_full_ct_2_name_tv) ;

        peakOneFileTv = (TextView)findViewById(R.id.multi_full_peak_1_name_tv);
        peakTwoFileTv = (TextView)findViewById(R.id.multi_full_peak_2_name_tv);
        peakHole1Tv = (TextView) findViewById(R.id.multi_hole1_tv);
        peakHole2Tv = (TextView) findViewById(R.id.multi_hole2_tv);
        peakOneTv = (TextView)findViewById(R.id.peak_hole_content_1_tv);
        peakTwoTv = (TextView)findViewById(R.id.peak_hole_content_2_tv);

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
        mCreateHexTv.setOnClickListener(this);
        mCreatePcrTv.setOnClickListener(this);
        mCreateCtTv.setOnClickListener(this);
        mCreatePeakTv.setOnClickListener(this);


        mFirstRNATabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mFirstRNATabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mSecondRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mSecondPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.VISIBLE);
                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.GONE);
                isFirstHex = true;
                if(isFirstRNACheck){
                    if(click_first_hex_pos != -1){
                        //显示当前pos未选中状态
                        hexFileAdapter.setTemplateList(mHexFileList);
                        openRNAFile(firstHexFileName);
                    }
                }
            }
        });

        mFirstPCRTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {

                mFirstPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mSecondRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mSecondPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.GONE);
                mPCRFileLayout.setVisibility(View.VISIBLE);
                mJudgeFileLayout.setVisibility(View.GONE);
                isFirstPCR = true;
                if(isFirstPCRCheck){
                    if(click_first_pcr_pos != -1){
                        //显示当前pos未选中状态
                        pcrFileAdapter.setTemplateList(mPCRFileList);
                        pcrFileAdapter.setSelectedPosition(click_first_pcr_pos);
                        Project firstProject =  openSLANExFile(firstPCRName);
                        firstPcrTime = TimerCaculate.PCR_Caculate(firstProject);
                        mTotalTimeEt.setText(firstRnaTime + firstPcrTime+ secondRnaTime + secondPcrTime + "");
                    }
                }
            }
        });

       /* mSecondRNATabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {

                mSecondRNATabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mJudgeTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mSecondPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.VISIBLE);
                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.GONE);
                isFirstHex = false;
                if(isSecondRNACheck){
                    if(click_sec_hex_pos != -1){
                        //显示当前pos未选中状态
                        hexFileAdapter.setTemplateList(mHexFileList);
                        hexFileAdapter.setSelectedPosition(click_sec_hex_pos);
                        openRNAFile(secHexFileName);
                    }
                }
            }
        });*/

        mSecondPCRTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mSecondPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mSecondRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.GONE);
                mPCRFileLayout.setVisibility(View.VISIBLE);
                mJudgeFileLayout.setVisibility(View.GONE);
                isFirstPCR = false;
                if(isSecondPCRCheck){
                    if(click_sec_pcr_pos != -1){
                        //显示当前pos未选中状态
                        pcrFileAdapter.setSelectedPosition(click_sec_pcr_pos);
                        pcrFileAdapter.setTemplateList(mPCRFileList);
                        Project secondProject = openSLANExFile(secondPcrName);
                        secondPcrTime = TimerCaculate.PCR_Caculate(secondProject);
                        mTotalTimeEt.setText(firstRnaTime + firstPcrTime+ secondRnaTime + secondPcrTime + "");
                    }
                }
            }
        });
        //Ct 文件
        mCtTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mSecondRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mSecondPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.GONE);
                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.VISIBLE);
                mPeakDetailLayout.setVisibility(View.GONE);
                mCtDetailLayout.setVisibility(View.VISIBLE);
                judgeFileAdapter.setSelectedPosition(-1);
                isPeak = false;
                if(null != mCtFileList){
                    judgeFileAdapter.setTemplateList(mCtFileList);
                    if(click_ct_pos != -1&& isCtCheck){
                        judgeFileAdapter.setSelectedPosition(click_ct_pos);
                       // openJudgeFile(ctFileName);
                    }
                }
            }
        });

        mPeakTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mSecondRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstRNATabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mFirstPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mSecondPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mRNAFileLayout.setVisibility(View.GONE);
                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.VISIBLE);
                mPeakDetailLayout.setVisibility(View.VISIBLE);
                mCtDetailLayout.setVisibility(View.GONE);
                judgeFileAdapter.setSelectedPosition(-1);
                isPeak = true;
                if(null != mPeakFileList){
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    if(isJudgePeak && click_peak_pos != -1){
                        judgeFileAdapter.setSelectedPosition(click_peak_pos);
                       // openJudgeFile(peakFileName);
                    }
                }
            }
        });

        initHoleSelectListener();
    }

    private void initHoleSelectListener(){
        peakHole1Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!peakHole1Tv.isSelected()){
                    if(peakHole1Tv.isActivated()){
                        peakHole1Tv.setActivated(false);
                        peakHole1Tv.setSelected(false);
                        if (holePeakFileNameMap != null) {
                            holePeakFileNameMap.remove(1);
                        }
                        peakOneFileTv.setText("");
                        peakOneTv.setText("");
                    }else{
                        judgeFileAdapter.setSelectedPosition(-1);
                        peakHole1Tv.setSelected(true);
                        if(peakHole2Tv.isSelected()
                                && !peakHole2Tv.isActivated()
                                && !StringUtil.isEmpty(peakTwoFileTv.getText().toString())){
                            peakHole2Tv.setSelected(false);
                            peakHole2Tv.setActivated(true);
                        }
                    }

                }else{
                    peakHole1Tv.setSelected(false);
                    if (holePeakFileNameMap != null) {
                        holePeakFileNameMap.remove(1);
                    }
                    peakOneFileTv.setText("");
                    peakOneTv.setText("");
                }
            }
        });
        peakHole2Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!peakHole2Tv.isSelected()){
                    if(peakHole2Tv.isActivated()){
                        peakHole2Tv.setActivated(false);
                        peakHole2Tv.setSelected(false);
                        if (holePeakFileNameMap != null) {
                            holePeakFileNameMap.remove(2);
                        }
                        peakTwoFileTv.setText("");
                        peakTwoTv.setText("");
                    }else{
                        peakHole2Tv.setSelected(true);
                        judgeFileAdapter.setSelectedPosition(-1);
                        if(peakHole1Tv.isSelected()
                                && !peakHole1Tv.isActivated()
                                && !StringUtil.isEmpty(peakOneFileTv.getText().toString())){
                            peakHole1Tv.setSelected(false);
                            peakHole1Tv.setActivated(true);
                        }

                    }

                }else{
                    peakHole2Tv.setActivated(false);
                    peakHole2Tv.setSelected(false);
                    if (holePeakFileNameMap != null) {
                        holePeakFileNameMap.remove(2);
                    }
                    peakTwoFileTv.setText("");
                    peakTwoTv.setText("");
                }
            }
        });

        //4个孔位的判别文件

        ctHole1Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!ctHole1Tv.isSelected()){
                    if(ctHole1Tv.isActivated()){
                        ctHole1Tv.setActivated(false);
                        ctHole1Tv.setSelected(false);
                        if (holeCtFileNameMap != null) {
                            holeCtFileNameMap.remove(1);
                        }
                        ctOneFileTv.setText("");
                        ctOneTv.setText("");
                    }else{
                        judgeFileAdapter.setSelectedPosition(-1);
                        ctHole1Tv.setSelected(true);
                        if(ctHole2Tv.isSelected()
                                && !ctHole2Tv.isActivated()
                                && !StringUtil.isEmpty(ctTwoFileTv.getText().toString())){
                            ctHole2Tv.setSelected(false);
                            ctHole2Tv.setActivated(true);
                        }

                    }

                }else{
                    ctHole1Tv.setSelected(false);
                    if (holeCtFileNameMap != null) {
                        holeCtFileNameMap.remove(1);
                    }
                    ctOneFileTv.setText("");
                    ctOneTv.setText("");
                }
            }
        });
        ctHole2Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!ctHole2Tv.isSelected()){
                    if(ctHole2Tv.isActivated()){
                        ctHole2Tv.setActivated(false);
                        ctHole2Tv.setSelected(false);
                        if (holeCtFileNameMap != null) {
                            holeCtFileNameMap.remove(2);
                        }
                        ctTwoFileTv.setText("");
                        ctTwoTv.setText("");
                    }else{
                        judgeFileAdapter.setSelectedPosition(-1);
                        ctHole2Tv.setSelected(true);
                        if(ctHole1Tv.isSelected()
                                && !ctHole1Tv.isActivated()
                                && !StringUtil.isEmpty(ctOneFileTv.getText().toString())){
                            ctHole1Tv.setSelected(false);
                            ctHole1Tv.setActivated(true);
                        }

                    }

                }else{
                    ctHole2Tv.setSelected(false);
                    if (holeCtFileNameMap != null) {
                        holeCtFileNameMap.remove(2);
                    }
                    ctTwoFileTv.setText("");
                    ctTwoTv.setText("");
                }
            }
        });

    }

    @Override
    protected void initData() {
        firstRnaTime = firstPcrTime = secondRnaTime = secondPcrTime = 0;
        mSLANSections = new ArrayList<ConfigSLANSection>();
        programRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        programRv.addItemDecoration(new DividerDecoration(this ,2));
        configSLANAdapter = new ConfigSLANAdapter(getApplicationContext());
        mSLANSections.clear();
        configSLANAdapter.setProgramSteps(mSLANSections);
        programRv.setAdapter(configSLANAdapter);
        mFirstRNATabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
        initRNAFile();
        initPcrFileRv();
        initJudgeFileRv();
        peakHoleTvs = new ArrayList<>(Arrays.asList(
                peakHole1Tv, peakHole2Tv
        ));
        ctHoleTvs = new ArrayList<>(Arrays.asList(
                ctHole1Tv, ctHole2Tv
        ));
    }

    private void initRNAFile(){
        mHexFileList = new ArrayList<String>();
//        PcrApplication.rnaVersion = PreferenceUtil.getInstance(getApplicationContext())
//                .getStringValue(SharedConstant.RNA_SYSTEM,"V5");
        if(PcrApplication.rnaVersion.equals("V5")){
            CONFIG_HEX_PATH = HexFileName_V5.HEXCREATEFILE_PATH;
        }else{

        }

        if(PcrApplication.rnaVersion.equals("V5")){
            CONFIG_HEX_SECOND_PATH = HexFileName_V5.HEXFILE_PATH;
        }else{

        }
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
                if(isFirstHex){
                    click_first_hex_pos = position;
                    firstHexFileName = mHexFileList.get(position);
                    //更新UI
                    isFirstRNACheck = true;
                    mFirstRNAFileTv.setText(firstHexFileName+".hex");
                    openRNAFile( firstHexFileName);
                }/*else{
                    click_sec_hex_pos = position;
                    secHexFileName = mHexFileList.get(position);
                    //更新UI
                    isSecondRNACheck = true;
                    mSecondRNAFileTv.setText(secHexFileName+".hex");
                    openRNAFile( secHexFileName);
                }*/
            }
        });
        secHexFileName = "V5-12多重二次试剂";
        //更新UI
        isSecondRNACheck = true;
        mSecondRNAFileTv.setText(secHexFileName+".hex" + "(固定)");

        File file = new File(CONFIG_HEX_SECOND_PATH +secHexFileName +".hex");
        if(file != null && file.exists()&& file.isFile()){
            HexConfig hexConfig = ConfigManager.getHexConfigFile(file);
            secondRnaTime = TimerCaculate.RNA_Fisrt_Caculate(hexConfig);
        }
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
                if(isFirstPCR){
                    //当前点击的是第一次PCR文件
                    click_first_pcr_pos = position;
                    firstPCRName = mPCRFileList.get(position);
                    //更新UI
                    isFirstPCRCheck = true;
                    mFirstPcrFileTv.setText(firstPCRName+".prj");
                    Project firstProject = openSLANExFile(firstPCRName);
                    firstPcrTime = TimerCaculate.PCR_Caculate(firstProject);
                    mTotalTimeEt.setText(firstRnaTime + firstPcrTime+ secondRnaTime + secondPcrTime + "");

                }else{
                    click_sec_pcr_pos = position;
                    isSecondPCRCheck = true;
                    secondPcrName = mPCRFileList.get(position);
                    mSecondPcrFileTv.setText(secondPcrName+".prj");
                    Project secondProject = openSLANExFile(secondPcrName);
                    secondPcrTime = TimerCaculate.PCR_Caculate(secondProject);
                    mTotalTimeEt.setText(firstRnaTime + firstPcrTime+ secondRnaTime + secondPcrTime + "");
                }
            }
        });

    }


    private void initJudgeFileRv(){
        mCtFileList = new ArrayList<String>();
        mPeakFileList = new ArrayList<String>();
        //编辑的prj文件夹是config找到所有的文件
        LinearLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),5);
        mJudgeFileRv.setLayoutManager(layoutManager);
        mJudgeFileRv.addItemDecoration(new LinearSpacingItemDecoration(this, 6));
        judgeFileAdapter = new FileNameAdapter(getApplicationContext());
        mJudgeFileRv.setAdapter(judgeFileAdapter);
        judgeFileAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if(!isPeak){
                    judgeFileAdapter.setTemplateList(mCtFileList);
                    judgeFileAdapter.setSelectedPosition(position);
                    click_ct_pos = position;
                    String ctFileName = mCtFileList.get(position);
                    //更新UI
                    isCtCheck = true;

                    if(ctHole1Tv.isSelected()){
                        String ctFile_1=  mCtFileList.get(position);
                        ctOneTv.setText(ctFile_1);
                        ctOneFileTv.setText(ctFile_1);
                        holeCtFileNameMap.put(1, ctFile_1);
                    }
                    if(ctHole2Tv.isSelected()){
                        String ctFile_2=  mCtFileList.get(position);
                        ctTwoTv.setText(ctFile_2);
                        ctTwoFileTv.setText(ctFile_2);
                        holeCtFileNameMap.put(2, ctFile_2);
                    }

                    openJudgeFile(ctFileName);
                   /* if(ctRuleEntities != null){
                        ctRuleEntities.clear();
                        judgeCtAdapter.setJudgeCtList(ctRuleEntities);
                    }*/
                }else{
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    judgeFileAdapter.setSelectedPosition(position);
                    click_peak_pos = position;
                    String peakFileName = mPeakFileList.get(position);
                    isJudgePeak = true;
                    if(peakHole1Tv.isSelected()){
                        String peakFile_1=  mPeakFileList.get(position);
                        peakOneTv.setText(peakFile_1);
                        peakOneFileTv.setText(peakFile_1);
                        holePeakFileNameMap.put(1, peakFile_1);
                    }
                    if(peakHole2Tv.isSelected()){
                        String peakFile_2=  mPeakFileList.get(position);
                        peakTwoTv.setText(peakFile_2);
                        peakTwoFileTv.setText(peakFile_2);
                        holePeakFileNameMap.put(2, peakFile_2);
                    }

                    openJudgeFile(peakFileName);
                }

            }
        });

        judgeFileAdapter.setOnItemLongClickListener(new FileNameAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position) {
                if(!isPeak){
                    judgeFileAdapter.setTemplateList(mCtFileList);
                    judgeFileAdapter.setSelectedPosition(-1);
                    click_ct_pos = -1;
                    //更新UI
                    isCtCheck = false;
                    // openJudgeFile(ctFileName);
                }else{
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    judgeFileAdapter.setSelectedPosition(-1);
                    click_peak_pos = -1;
                    isJudgePeak = false;
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
        isEditMode = getIntent().getBooleanExtra("isEdit",false);
        tempLateName = getIntent().getStringExtra("templateName");
        if(isEditMode){
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
            String subFirName = "multi_" + DateFormatUtil.getFileName();
            mTemplateEt.setText(subFirName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHexFile();
        updateJudgeFile();
        updatePCRFile();
        if(isEditMode){
            //编辑多重模板文件
           loadEditModeData();
           isEditMode = false;
        }
    }
    //编辑模式下加载数据
    private void loadEditModeData(){
        String localTempPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + tempLateName+File.separator;
        List<File> editFiles = getConfigTemplateFiles(localTempPath);
        if(null != editFiles &&editFiles.size()>0){
            for(File file :editFiles){
                if(file.getName().endsWith(".hex") && file.getName().startsWith("1-")){
                    //核酸检测文件
                    firstHexFileName  = file.getName().substring(2,file.getName().length()-4);
                    //更新UI
                    isFirstRNACheck = true;
                    mFirstRNAFileTv.setText(firstHexFileName+".hex");
                    int pos = getClickedPosition(firstHexFileName,mHexFileList);
                    click_first_hex_pos = pos;
                    hexFileAdapter.setSelectedPosition(click_first_hex_pos);
                    openRNAFile(firstHexFileName);
                }else if(file.getName().endsWith(".prj") && file.getName().startsWith("1-")){
                    firstPCRName = file.getName().substring(2, file.getName().length() - 4);
                    isFirstPCRCheck = true;
                    mFirstPcrFileTv.setText(firstPCRName + ".prj");
                    int pos = getClickedPosition(firstPCRName,mPCRFileList);
                    click_first_pcr_pos = pos;

                    }else if(file.getName().endsWith(".hex") && file.getName().startsWith("2-")){
                        //核酸检测文件
                        secHexFileName  = file.getName().substring(2,file.getName().length()-4);
                        //更新UI
                        isSecondRNACheck = true;
                        mSecondRNAFileTv.setText(secHexFileName+".hex");
                       /* int pos = getClickedPosition(secHexFileName,mHexFileList);
                        click_sec_hex_pos = pos;*/

                    }else if(file.getName().endsWith(".prj") && file.getName().startsWith("2-")){
                        secondPcrName= file.getName().substring(2, file.getName().length() - 4);
                        isSecondPCRCheck = true;
                        mSecondPcrFileTv.setText(secondPcrName + ".prj");
                        int pos = getClickedPosition(secondPcrName,mPCRFileList);
                        click_sec_pcr_pos = pos;

                }else if(file.getName().endsWith(".peak")){
                    isJudgePeak = true;
                    if(file.getName().startsWith("hole1")){
                        String peakOneFile = file.getName().substring(file.getName().lastIndexOf("_") + 1);
                        peakOneFileTv.setText(peakOneFile);
                        peakOneTv.setText(peakOneFile);
                        int pos = getClickedPosition(peakOneFile,mPeakFileList);
                        click_peak_pos = pos;
                        setMultiSelected(peakHole1Tv, peakHoleTvs.toArray(new TextView[0]));
                        holePeakFileNameMap.put(1,peakOneFile);
                    }else if(file.getName().startsWith("hole2")) {
                        String peakSecondFile = file.getName().substring(file.getName().lastIndexOf("_") + 1);
                        peakTwoFileTv.setText(peakSecondFile);
                        peakTwoTv.setText(peakSecondFile);
                        int pos = getClickedPosition(peakSecondFile, mPeakFileList);
                        click_peak_pos = pos;
                        setMultiSelected(peakHole2Tv, peakHoleTvs.toArray(new TextView[0]));
                        holePeakFileNameMap.put(2, peakSecondFile);
                    }
                }else if(file.getName().endsWith(".ct")){
                    isCtCheck = true;
                    if(file.getName().startsWith("hole1")){
                        String ctOneFile = file.getName().substring(file.getName().lastIndexOf("_") + 1);
                        ctOneFileTv.setText(ctOneFile);
                        ctOneTv.setText(ctOneFile);
                        int pos = getClickedPosition(ctOneFile,mCtFileList);
                        click_ct_pos= pos;
                        setMultiSelected(ctHole1Tv, ctHoleTvs.toArray(new TextView[0]));
                        holeCtFileNameMap.put(1,ctOneFile);
                    }else if(file.getName().startsWith("hole2")){
                        String ctTwoFile = file.getName().substring(file.getName().lastIndexOf("_") + 1);
                        ctTwoFileTv.setText(ctTwoFile);
                        ctTwoTv.setText(ctTwoFile);
                        int pos = getClickedPosition(ctTwoFile,mCtFileList);
                        click_ct_pos= pos;
                        setMultiSelected(ctHole2Tv, ctHoleTvs.toArray(new TextView[0]));
                    }
                }else if(file.getName().equals("totaltime.txt")){
                    //全流程时间
                    TotalTime totalTime = ConfigManager.getTotalTimeFile(file);
                    mTotalTimeEt.setText(totalTime.getTotalTime()+"");
                    temparatureCtrlMode = totalTime.getTemperatureCtrlMode();
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
        if(null != mCtFileList){
            mCtFileList.clear();
            mCtFileList = null;
        }
        if(null != mPeakFileList){
            mPeakFileList.clear();
            mPeakFileList = null;
        }
        if(null != mHexFileList){
            mHexFileList.clear();
            mHexFileList = null;
        }
        if(null != peakRuleEntities){
            peakRuleEntities.clear();
            peakRuleEntities = null;
        }
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
        if(null != holeCtFileNameMap){
            holeCtFileNameMap.clear();
            holeCtFileNameMap = null;
        }
        if(null != holePeakFileNameMap){
            holePeakFileNameMap.clear();
            holePeakFileNameMap = null;
        }
        if(null != peakHoleTvs){
            peakHoleTvs.clear();
            peakHoleTvs = null;
        }
        if(null != ctHoleTvs){
            ctHoleTvs.clear();
            ctHoleTvs = null;
        }
    }

    private void updateHexFile(){
        mHexFileList.clear();
        File[] files = new File(CONFIG_HEX_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != mHexFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".hex")){
                        String name = files[i].getName().substring(0,files[i].getName().length()-4);
                        mHexFileList.add(name);
                       
                    }
                }
            }
			 Collections.sort(mHexFileList);
        }
        hexFileAdapter.setTemplateList(mHexFileList);
    }

    private void updateJudgeFile() {
        // 1. 构造判别文件目录对象，统一路径管理
        File judgeDir = new File(FileUtils.CONFIG_PATH, "judge");
        File[] files = judgeDir.listFiles();

        // 清空旧数据
        mPeakFileList.clear();
        mCtFileList.clear();

        // 遍历文件，分类收集 .peak / .ct 文件名
        if (files != null && files.length > 0) {
            for (File file : files) {
                String fileName = file.getName();
                if (fileName.endsWith(".peak")) {
                    mPeakFileList.add(fileName);
                } else if (fileName.endsWith(".ct")) {
                    mCtFileList.add(fileName);
                }
            }

            // 全部添加完成后仅排序一次
            Collections.sort(mPeakFileList);
            Collections.sort(mCtFileList);
        }

        // 给适配器设置数据源
        if (isPeak) {
            judgeFileAdapter.setTemplateList(mPeakFileList);
        } else {
            judgeFileAdapter.setTemplateList(mCtFileList);
        }
    }


    private void updatePCRFile(){
        File[] files = new File(FileUtils.CONFIG_SLAN_PATH).listFiles();//文件夹下的所有文件
        mPCRFileList.clear();
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != mPCRFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".prj")){
                        String name = files[i].getName().substring(0,files[i].getName().length()-4);
                        mPCRFileList.add(name);
                      
                    }
                }
            }
        }
  		Collections.sort(mPCRFileList);
        pcrFileAdapter.setTemplateList(mPCRFileList);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.multi_create_save_btn){
            if(isFirstPCRCheck && isFirstRNACheck
                    && isSecondPCRCheck &&isSecondRNACheck){
                String templateName = mTemplateEt.getText().toString().trim();
                String secondName = mSecondNameEt.getText().toString().trim();
                String thirdName = mThirdNameEt.getText().toString().trim();
                String totalTime = mTotalTimeEt.getText().toString().trim();
                if(!StringUtil.isEmpty(templateName)
                        &&!StringUtil.isEmpty(totalTime)){
                    if(StringUtil.isEmpty(secondName)&& StringUtil.isEmpty(thirdName)){
                        String name = templateName ;
                        saveMultiTemplateAction(name);
                    }else{
                        String name = templateName + "~"+ secondName + "~" + thirdName;
                        saveMultiTemplateAction(name);
                    }
                }else{
                    if(StringUtil.isEmpty(templateName)){
                        //请输入模板的名字
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
                //创建模板文件不完整
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.create_multi_toast_template_imcomplete),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.multi_create_hex_create_tv){
            Intent createHexIntent = new Intent(CreateMultiActivity.this, ConfigHexActivity.class);
            startActivity(createHexIntent);
        }else if(id == R.id.multi_create_pcr_create_tv){
            //创建pcr项目
            Intent createHexIntent = new Intent(CreateMultiActivity.this, ConfigSLANActivity.class);
            startActivity(createHexIntent);
        }else if(id == R.id.multi_create_peak_create_tv){
            //新建判别文件
            Intent createPeakIntent = new Intent(CreateMultiActivity.this, PeakJudgeActivity.class);
            startActivity(createPeakIntent);
        }else if(id == R.id.multi_create_ct_create_tv){
            //新建判别文件
            Intent createPeakIntent = new Intent(CreateMultiActivity.this, PeakJudgeActivity.class);
            startActivity(createPeakIntent);
        }
    }

    private void saveMultiTemplateAction(String templateName){
        String subFirName = templateName;
        File dir = new File(CONFIG_FULLPRC_PCR_TEMPLATE_PATH, subFirName);
        if (!dir.exists()) {
            dir.mkdirs();
            saveMultiTemplate(subFirName);
        }else{
            //已经存在了文件夹，提示是否覆盖
            showNoticeDialog(getResources().getString(R.string.create_multi_dialog_message_template_exists),subFirName,0);
        }
    }

    /**
     * 保存多重文件
     */
    private void saveMultiTemplate(String subFirName){
        File dir = new File(CONFIG_FULLPRC_PCR_TEMPLATE_PATH, subFirName);
        boolean copyFirstPcrResult = false ;
        boolean copySecondPcrResult = false ;
        boolean copyFirstHexResult = false;
        boolean copySecondHexResult = false;
        boolean copyAllCtFilesResult = true; // 默认成功，有一个失败则变false
        boolean copyAllPeakFilesResult = true;
        boolean saveTotalResult = saveTotalTime(subFirName);
        //首先保存prj文件
        if(!StringUtil.isEmpty(firstPCRName)){
            String newPcrPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator
                    +"1-" +firstPCRName+".prj";
            String oldPcrPath = FileUtils.CONFIG_SLAN_PATH + File.separator
                    +firstPCRName +".prj";
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyFirstPcrResult=  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        if(!StringUtil.isEmpty(secondPcrName)){
            String newPcrPath =CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator
                    +"2-" +secondPcrName+".prj";
            String oldPcrPath = FileUtils.CONFIG_SLAN_PATH + File.separator
                    +secondPcrName +".prj";
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copySecondPcrResult=  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        String holeSourceDir = CONFIG_JUDGE_PATH;
        // 孔文件的目标目录
        String holeTargetDir = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator;
        if (holeCtFileNameMap != null && !holeCtFileNameMap.isEmpty()) {
            for (Map.Entry<Integer, String> entry : holeCtFileNameMap.entrySet()) {
                Integer holeNo = entry.getKey();    // 孔号
                String holeFileName = entry.getValue(); // 孔对应的文件名

                if (StringUtil.isEmpty(holeFileName)) {
                    continue;
                }

                // 源路径 / 目标路径
                String oldHolePath = holeSourceDir + File.separator + holeFileName;
                String newHolePath = holeTargetDir + "hole"+ holeNo +"_"+ holeFileName;

                Log.i("CreateSingleActivity", "复制孔文件【孔号=" + holeNo + "】源：" + oldHolePath);
                Log.i("CreateSingleActivity", "复制孔文件【孔号=" + holeNo + "】目标：" + newHolePath);

                // 复制文件
                boolean copyOne = FileUtils.copyFile(oldHolePath, newHolePath);
                if (!copyOne) {
                    copyAllCtFilesResult = false; // 只要有一个失败，整体标记失败
                    Log.e("CreateSingleActivity", "孔文件复制失败：" + holeFileName);
                }
            }
        }

        if (holePeakFileNameMap != null && !holePeakFileNameMap.isEmpty()) {
            for (Map.Entry<Integer, String> entry : holePeakFileNameMap.entrySet()) {
                Integer holeNo = entry.getKey();    // 孔号
                String holeFileName = entry.getValue(); // 孔对应的文件名

                if (StringUtil.isEmpty(holeFileName)) {
                    continue;
                }

                // 源路径 / 目标路径
                String oldHolePath = holeSourceDir + File.separator + holeFileName;
                String newHolePath = holeTargetDir + "hole"+holeNo + "_"+ holeFileName;

                Log.i("CreateSingleActivity", "复制孔文件【孔号=" + holeNo + "】源：" + oldHolePath);
                Log.i("CreateSingleActivity", "复制孔文件【孔号=" + holeNo + "】目标：" + newHolePath);

                // 复制文件
                boolean copyOne = FileUtils.copyFile(oldHolePath, newHolePath);
                if (!copyOne) {
                    copyAllPeakFilesResult = false; // 只要有一个失败，整体标记失败
                    Log.e("CreateSingleActivity", "孔文件复制失败：" + holeFileName);
                }
            }
        }

        if(!StringUtil.isEmpty(firstHexFileName)){
            String newPcrPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator
                    +"1-"+ firstHexFileName+".hex";
            String oldPcrPath =  CONFIG_HEX_PATH +firstHexFileName +".hex";
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyFirstHexResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        if(!StringUtil.isEmpty(secHexFileName)){
            String newPcrPath = CONFIG_FULLPRC_PCR_TEMPLATE_PATH + subFirName + File.separator
                    +"2-"+ secHexFileName+".hex";
            String oldPcrPath =  CONFIG_HEX_SECOND_PATH +secHexFileName +".hex";
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copySecondHexResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }


        if(copyFirstPcrResult && copyFirstHexResult
                && copySecondHexResult && copySecondPcrResult && saveTotalResult){
            if(holeCtFileNameMap != null && !holeCtFileNameMap.isEmpty()
                    && holePeakFileNameMap != null && !holePeakFileNameMap.isEmpty()){
                if(copyAllPeakFilesResult&& copyAllCtFilesResult){
                    //保存检测项目成功
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    // 保存检测项目失败;
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_fail),Toast.LENGTH_LONG).show();
                    return;
                }
            }else if(holeCtFileNameMap != null && holeCtFileNameMap.isEmpty()
                    && holePeakFileNameMap != null && !holePeakFileNameMap.isEmpty()){
                if(copyAllPeakFilesResult){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    // Log.i("AAA","2222---保存检测项目失败");
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else if(holeCtFileNameMap != null && !holeCtFileNameMap.isEmpty()
                    && holePeakFileNameMap != null && holePeakFileNameMap.isEmpty()){
                if(copyAllCtFilesResult){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    // Log.i("AAA","3333---保存检测项目失败");
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();
            }
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.create_multi_toast_save_fail),Toast.LENGTH_LONG).show();
            if(dir.exists()){
                FileUtils.remove(dir);
                //删除文件夹以及文件
            }
        }
    }

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
        totalTime.setSecondRNATime(secondRnaTime);
        totalTime.setSecondPcrTime(secondPcrTime);
        totalTime.setTemperatureCtrlMode(temparatureCtrlMode);
        String totalTime_str = gson.toJson(totalTime);
        String fileName = "totaltime";
        return  FileUtils.saveFileInSdcard(totalTime_str,savePath,fileName,".txt");
    }

    /**
     * 查看判别文件详情
     * @param fileName
     */

    private void openJudgeFile(String fileName){
        if(!StringUtil.isEmpty(fileName)){
            File file = new File(FileUtils.CONFIG_PATH+ File.separator + "judge/" + fileName);
            if(fileName.endsWith(".ct")){
                mPeakDetailLayout.setVisibility(View.GONE);
                mCtDetailLayout.setVisibility(View.VISIBLE);
                ctRuleEntities.clear();
                ctRuleEntities = CTRuleEntity.getCTRuleList(file);
                judgeCtAdapter.setJudgeCtList(ctRuleEntities);
                judgeCtAdapter.notifyDataSetChanged();
            }else if(fileName.endsWith(".peak")){
                mPeakDetailLayout.setVisibility(View.VISIBLE);
                mCtDetailLayout.setVisibility(View.GONE);
                peakRuleEntities = PEAKRuleEntity.getPEAKRuleList(file);
                judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                judgePeakAdapter.notifyDataSetChanged();
            }

        }

    }

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
                //无法打开此文件
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.config_hex_toast_cannot_open_file),Toast.LENGTH_LONG).show();
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
            if(isFirstRNACheck){
                firstRnaTime =  TimerCaculate.RNA_Fisrt_Caculate(hexConfig);
                Log.e("LLL","firstRnaTime" + firstRnaTime);
            }



            if(hexConfig.getMagneticChoice() == 0){
                //样本磁珠活化
                magneticlive_tv.setText(getResources().getString(R.string.config_hex_step_2_one));
            }else if(hexConfig.getMagneticChoice() == 1){
                magneticlive_tv.setText(getResources().getString(R.string.config_hex_step_2_two));
            }

            //冻干体系
            if(hexConfig.getFreeze_drying() == 0){
                //液态体系
                mLiquidSysTv.setText(getResources().getString(R.string.config_hex_liquid_system));
            }else if(hexConfig.getFreeze_drying() == 1){
                //冻干体系
                mLiquidSysTv.setText(getResources().getString(R.string.config_hex_freeze_drying_system));
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
        mTotalTimeEt.setText(firstRnaTime + firstPcrTime + secondRnaTime + secondPcrTime + "");
    }

    private Project openSLANExFile(String fileName){
        //打开文件夹
        Project openProject = null;
        if(!StringUtil.isEmpty(fileName)){
            ArrayList<Project> mConfigProjects = PcrApplication.slanService.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
            if(null != mConfigProjects && mConfigProjects.size()>0){
                for(Project project :mConfigProjects){
                    if(null != project){
                        if(project.getName().equals(fileName)){
                            Program program = project.getProgram();
                            //转化为String
                            openProject = project;
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
        return openProject;
    }

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
                        layoutParams.setMargins(0, 0, 2, 0);
                        textView.setLayoutParams(layoutParams);
                        int index = i+1;
                        textView.setText(getResources().getString(R.string.runing_pcr_section)+ index);
                    }else if(programType == Constants.PROGRAM_TYPE_MELT){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(0, 0, 2, 0);
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
                        layoutParams.setMargins(0, 0, 2, 0);
                        textView.setLayoutParams(layoutParams);
                        textView.setText(getResources().getString(R.string.running_pcr_recycle_num) + (mSLANSections.get(i).getCycles()));
                    }else if(programType == Constants.PROGRAM_TYPE_MELT){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(0, 0, 2, 0);
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

        }
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
                        slanSection.setInterval(sectionMelt.getInterval());
                        slanSection.setMeltHoldtime(sectionMelt.getHoldTime());

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


    private List<File> getConfigTemplateFiles(String path){
        List<File> configFiles = new ArrayList<>();
        File[] files = new File(path).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != configFiles) {
                    configFiles.add(files[i]);
                    Collections.sort(configFiles);
                }
            }
        }
        return configFiles;
    }





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
                        saveMultiTemplate(subFirName);
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

    /**
     * 多选激活方法：只把传入view设为activated=true，其他原有状态保留，不清除
     * @param targetView 需要标记为已绑定文件的孔位
     * @param allViews 全部孔位数组
     */
    private void setMultiSelected(TextView targetView, TextView... allViews) {
        if (targetView == null || allViews == null) return;
        for (TextView tv : allViews) {
            if (tv == targetView) {
                tv.setSelected(false);
                tv.setActivated(true);
            }
            // 其他view不做任何修改，保留原有activated状态，实现多选
        }
    }

}
