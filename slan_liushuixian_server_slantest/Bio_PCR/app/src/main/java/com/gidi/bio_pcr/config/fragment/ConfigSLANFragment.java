package com.gidi.bio_pcr.config.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.ConfigSLANAdapter;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.adapter.DetectRangeAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.DividerDecoration;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.NumKeyboardDialog;
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
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.hongshi.slansdk.model.result.SDKResult;
import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.DigitalFilter;
import com.hongshi.slansdk.slanenum.MeltPeakAlgorithm;
import com.hongshi.slansdk.slanenum.PeekType;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigSLANFragment extends BaseFragment implements View.OnClickListener{
    private ImageView mBackImg;
    private DecimalFormat nf1 = new DecimalFormat( "0.00");
    private DecimalFormat nf2 = new DecimalFormat( "0.0");
    private EditText mProgramNameTv;
    private RecyclerView mSLanProgramRv;//程序
    private LinearLayout mProgramTitleLayout,mProgramRecycleLayout;
    private View mMeltButtomView;
    private LinearLayout mMeltContinueLayout,mMeltStepLayout;
    private TextView mScanModeTv;
    private ConfigSLANAdapter slanStepAdapter;
    private List<ConfigSLANSection> mSLANSections;
    private TextView mAddSectionTv;
    private TextView mAddStepTv;
    private TextView mDeleteTv;
    private TextView mSaveExTv;

    private TextView mBasicInfoTabTv,mProgramTypeTv,mParamTabTv;//基本信息
    private TextView mConfigProgramTv;
    private RelativeLayout mBasicInfoLayout;
    private HorizontalScrollView mProgramLayout;

    private int click_section_index = -1;
    private int click_step_index =-1;

    private int set_section_index = -1;
    private int set_step_index = -1;

  //  private List<ConfigTarget> configTargets;
    private int type=Constants.PROGRAM_TYPE_AMP;//0默认是定性/定量 1：标准熔解曲线
    private boolean isCreateProgram = false;
    /****下拉列表**********/
    private boolean isExpanded = false;
    private List<String> mTypeList,mMeltPeakAlgorithmList,mAmpAlgorithmList;
    private PopupWindow mSpinnerPopWindow,meltAlgPopWindow,mAmpAlgWindow,mDigitalFilterWindow;
    private PopupWindow mAysTypePopWindow,mOptimizationModeWindow;//定性定量,基线优化
    private PopupWindow mPeekTypeWindow;
    private PopupWindow mAddSectionWindow,mAddStepsWindow;
    private ListView mSpnnierLv,mAmpAlgorithmLv;
    private CustomSpinnerAdapter spinnerAdapter,ampAlgorithmAdapter;
    private NoticeDialog noticeDialog;

    /********熔解曲线算法下拉列表********/

    ///////////////////////
    private Project project = null;
    private TextView mReactionTv;
    private CheckBox mFamCk,mHexCk,mRoxCk,mCY5Ck,mQuaSarCk,mAttoCk;
    private boolean isFamThresholdCheck,isHexThresholdCheck,isRoxThresholdCheck,isCY5ThresholdCheck,
            isQuaSarThresholdCheck,isAttoThresholdCheck= false;
    private NumKeyboardDialog numKeyboardDialog;
    private TextView mTitleAmpParamTv,mTitleMeltParamTv;
    //熔解曲线参数
    private LinearLayout mMeltParamLayout;

    private boolean isBasicParam = true;

    //扩增曲线参数
    private int ampAmpAlgorithm = 0;

    private LinearLayout mAMPParamLayout;
    private TextView mAmpAlgorithmTv;
    private View popupView;
    /////////////////////////项目参数////////////////////////
    private RelativeLayout mParamLayout;//项目参数
    private ListView mAmpAlogrithmLv,mMeltParamterLv;//基本参数列表和熔解曲线参数的列表
    private ListView mDetectRangeLv;//定量检测范围
    private DetectRangeAdapter detectRangeAdapter;//定量检测范围
    private AmpAlgorithmAdapter mAmpAlgorithmAdapter;
    private MeltParamterAdapter mMeltParamterAdapter;

    private List<ConfigTarget> mParamTargets;
    private List<ConfigTarget> mDetectRanges = new ArrayList<ConfigTarget>();
    private int mAmpAlogrithm_pos = -1;//基本参数
    private int meltParamter_pos = -1;
    private int detect_range_pos = -1;
    //分析类型,基线优化,数字滤波
    private List<String> mAmpAysTypes,mOptimizationModes,mDighitalFilters;
    //熔解曲线参数
    private List<String> mPeekTypes;
    //交叉干扰
    private LinearLayoutCompat mCrossOneLayout,mCrossTwoLayout,mCrossThreeLayout,mCrossFourLayout;
    private LinearLayoutCompat mCrossFiveLayout, mCrossSixLayout;
    private TextView mCrossTitle1Tv, mCrossTitle2Tv,mCrossTitle3Tv,mCrossTitle4Tv,mCrossTitle5Tv,mCrossTitle6Tv;
    private TextView mCross11Tv, mCross12Tv,mCross13Tv,mCross14Tv,mCross15Tv,mCross16Tv;
    private TextView mCross21Tv, mCross22Tv,mCross23Tv,mCross24Tv,mCross25Tv,mCross26Tv;
    private TextView mCross31Tv, mCross32Tv,mCross33Tv,mCross34Tv,mCross35Tv,mCross36Tv;
    private TextView mCross41Tv, mCross42Tv,mCross43Tv,mCross44Tv,mCross45Tv,mCross46Tv;
    private TextView mCross51Tv, mCross52Tv,mCross53Tv,mCross54Tv,mCross55Tv,mCross56Tv;
    private TextView mCross61Tv, mCross62Tv,mCross63Tv,mCross64Tv,mCross65Tv,mCross66Tv;

    //选择插入步骤的提示
    private List<String> mAddSteps,mAddSections;


    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_config_slan;
    }

    @Override
    protected void initView(View rootView) {
        mBackImg = (ImageView) rootView.findViewById(R.id.config_slan_back_img);
        mProgramNameTv = (EditText)rootView.findViewById(R.id.config_type_program_name_tv);
        mProgramNameTv.addTextChangedListener(new LimitInputTextWatcher(mProgramNameTv));
        mSLanProgramRv = (RecyclerView) rootView.findViewById(R.id.slan_config_pcr_rv);
        mBasicInfoLayout = (RelativeLayout)rootView.findViewById(R.id.config_slan_info_layout) ;
        mProgramLayout = (HorizontalScrollView)rootView.findViewById(R.id.config_slan_program_layout) ;

        mProgramTitleLayout = (LinearLayout) rootView.findViewById(R.id.slan_config_pcr_program_title_layout);
        mProgramRecycleLayout = (LinearLayout) rootView.findViewById(R.id.slan_config_pcr_program_recycle_layout);
        mAddSectionTv = (TextView) rootView.findViewById(R.id.add_slan_section_tv);
        mAddStepTv = (TextView) rootView.findViewById(R.id.add_slan_step_tv);
        mDeleteTv = (TextView)rootView.findViewById(R.id.delete_slan_tv);
        mSaveExTv = (TextView) rootView.findViewById(R.id.save_ex_template_tv);
        mBasicInfoTabTv = (TextView)rootView. findViewById(R.id.config_project_info_tv);
        mParamTabTv = (TextView)rootView.findViewById(R.id.config_slan_param_tv);
        mProgramTypeTv =(TextView)rootView.findViewById(R.id.config_info_program_type_tv) ;
        mConfigProgramTv = (TextView)rootView. findViewById(R.id.config_program_tv);
        mConfigProgramTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
        mParamTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);

        mTitleAmpParamTv = (TextView)rootView.findViewById(R.id.config_slan_param_setting_title_basic_tv_1);
        mTitleMeltParamTv = (TextView)rootView.findViewById(R.id.config_slan_param_setting_title_melt_tv_1);
        mMeltButtomView = LayoutInflater.from(getActivity().getApplicationContext()).inflate(R.layout.config_melt_buttom_layout, null);

        initPopupWindow();
        initAddSectionsWindow();
        initAddStepsWindow();
        //基本信息
        mReactionTv = (TextView)rootView.findViewById(R.id.config_info_reaction_tv) ;
        mFamCk = (CheckBox)rootView.findViewById(R.id.config_slan_fam_ck);
        mHexCk = (CheckBox)rootView.findViewById(R.id.config_slan_hex_ck);
        mRoxCk = (CheckBox)rootView.findViewById(R.id.config_slan_rox_ck);
        mCY5Ck = (CheckBox) rootView.findViewById(R.id.config_slan_cy5_ck);
        mQuaSarCk = (CheckBox)rootView.findViewById(R.id.config_slan_quasar_ck);
        mAttoCk = (CheckBox)rootView.findViewById(R.id.config_slan_atto_ck);

        /**
         * 熔解曲线参数
         */
        mMeltParamLayout = (LinearLayout) rootView.findViewById(R.id.config_slan_melt_param_layout_1);
        initMeltPeakAlgorithm();

        /**
         * 扩增曲线参数
         */
        mAMPParamLayout = (LinearLayout) rootView.findViewById(R.id.config_slan_amp_param_layout_1);
        mAmpAlgorithmTv = (TextView)rootView.findViewById(R.id.config_slan_amp_algorithm_tv_1);
        initAMPAlgorithm();


        mDeleteTv.setVisibility(View.INVISIBLE);
        mAddSectionTv.setVisibility(View.INVISIBLE);
        mAddStepTv.setVisibility(View.INVISIBLE);

        //项目参数
        mParamTargets = new ArrayList<ConfigTarget>();
        mParamLayout = (RelativeLayout)rootView.findViewById(R.id.config_slan_param_layout);
        mAmpAlogrithmLv = (ListView) rootView.findViewById(R.id.config_saln_amp_algorithm_lv);
        mAmpAlgorithmAdapter = new AmpAlgorithmAdapter(getActivity().getApplicationContext());
        mAmpAlogrithmLv.setAdapter(mAmpAlgorithmAdapter);
        mMeltParamterLv = (ListView) rootView.findViewById(R.id.config_slan_melt_paramter_lv);//熔解曲线参数列表
        mMeltParamterAdapter = new MeltParamterAdapter(getActivity().getApplicationContext());
        mMeltParamterLv.setAdapter(mMeltParamterAdapter);

        mDetectRangeLv = (ListView)rootView.findViewById(R.id.config_slan_detect_range_lv);
        detectRangeAdapter =  new DetectRangeAdapter(getActivity().getApplicationContext());
        detectRangeAdapter.setRangeList(mDetectRanges);
        mDetectRangeLv.setAdapter(detectRangeAdapter);
        initAmpAysTypeWindow();//分析类型
        initOptimizationModeWindow();//基线优化
        initAmpDigitalFilter();
        initPeekTypeWindow();

        //交叉干扰
        mCrossOneLayout = (LinearLayoutCompat)rootView.findViewById(R.id.config_slan_channel_1_corsstalk_layout);
        mCrossTwoLayout = (LinearLayoutCompat)rootView.findViewById(R.id.config_slan_channel_2_corsstalk_layout);
        mCrossThreeLayout = (LinearLayoutCompat)rootView.findViewById(R.id.config_slan_channel_3_corsstalk_layout);
        mCrossFourLayout = (LinearLayoutCompat)rootView.findViewById(R.id.config_slan_channel_4_corsstalk_layout);
        mCrossFiveLayout = (LinearLayoutCompat)rootView.findViewById(R.id.config_slan_channel_5_corsstalk_layout);
        mCrossSixLayout = (LinearLayoutCompat)rootView.findViewById(R.id.config_slan_channel_6_corsstalk_layout);
        mCrossTitle1Tv = (TextView) rootView.findViewById(R.id.config_slan_title_1_tv);
        mCrossTitle2Tv = (TextView) rootView.findViewById(R.id.config_slan_title_2_tv);
        mCrossTitle3Tv = (TextView) rootView.findViewById(R.id.config_slan_title_3_tv);
        mCrossTitle4Tv = (TextView) rootView.findViewById(R.id.config_slan_title_4_tv);
        mCrossTitle5Tv = (TextView) rootView.findViewById(R.id.config_slan_title_5_tv);
        mCrossTitle6Tv = (TextView) rootView.findViewById(R.id.config_slan_title_6_tv);
        mCross13Tv = (TextView) rootView.findViewById(R.id.config_slan_13_tv);
        mCross14Tv = (TextView) rootView.findViewById(R.id.config_slan_14_tv);
        mCross15Tv = (TextView) rootView.findViewById(R.id.config_slan_15_tv);
        mCross16Tv = (TextView) rootView.findViewById(R.id.config_slan_16_tv);
        mCross11Tv = (TextView) rootView.findViewById(R.id.config_slan_11_tv);
        mCross12Tv = (TextView) rootView.findViewById(R.id.config_slan_12_tv);
        mCross13Tv = (TextView) rootView.findViewById(R.id.config_slan_13_tv);
        mCross14Tv = (TextView) rootView.findViewById(R.id.config_slan_14_tv);
        mCross15Tv = (TextView) rootView.findViewById(R.id.config_slan_15_tv);
        mCross16Tv = (TextView) rootView.findViewById(R.id.config_slan_16_tv);
        mCross21Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_21_tv);
        mCross22Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_22_tv);
        mCross23Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_23_tv);
        mCross24Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_24_tv);
        mCross25Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_25_tv);
        mCross26Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_26_tv);

        mCross31Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_31_tv);
        mCross32Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_32_tv);
        mCross33Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_33_tv);
        mCross34Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_34_tv);
        mCross35Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_35_tv);
        mCross36Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_36_tv);

        mCross41Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_41_tv);
        mCross42Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_42_tv);
        mCross43Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_43_tv);
        mCross44Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_44_tv);
        mCross45Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_45_tv);
        mCross46Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_46_tv);

        mCross51Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_51_tv);
        mCross52Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_52_tv);
        mCross53Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_53_tv);
        mCross54Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_54_tv);
        mCross55Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_55_tv);
        mCross56Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_56_tv);

        mCross61Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_61_tv);
        mCross62Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_62_tv);
        mCross63Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_63_tv);
        mCross64Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_64_tv);
        mCross65Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_65_tv);
        mCross66Tv = (TextView) rootView.findViewById(R.id.config_slan_cross_66_tv);
        initCrossTalk();
    }
    private void initCrossTalk(){
        mCrossOneLayout.setVisibility(View.GONE);
        mCrossTwoLayout.setVisibility(View.GONE);
        mCrossThreeLayout.setVisibility(View.GONE);
        mCrossFourLayout.setVisibility(View.GONE);
        mCrossFiveLayout.setVisibility(View.GONE);
        mCrossSixLayout.setVisibility(View.GONE);
        mCross11Tv.setVisibility(View.GONE);mCross12Tv.setVisibility(View.GONE);  mCross13Tv.setVisibility(View.GONE);
        mCross14Tv.setVisibility(View.GONE);mCross15Tv.setVisibility(View.GONE);mCross16Tv.setVisibility(View.GONE);
        mCross21Tv.setVisibility(View.GONE);mCross22Tv.setVisibility(View.GONE);  mCross23Tv.setVisibility(View.GONE);
        mCross24Tv.setVisibility(View.GONE);mCross25Tv.setVisibility(View.GONE);mCross26Tv.setVisibility(View.GONE);
        mCross31Tv.setVisibility(View.GONE);mCross32Tv.setVisibility(View.GONE);  mCross33Tv.setVisibility(View.GONE);
        mCross34Tv.setVisibility(View.GONE);mCross35Tv.setVisibility(View.GONE);mCross36Tv.setVisibility(View.GONE);
        mCross41Tv.setVisibility(View.GONE);mCross42Tv.setVisibility(View.GONE);  mCross43Tv.setVisibility(View.GONE);
        mCross44Tv.setVisibility(View.GONE);mCross45Tv.setVisibility(View.GONE);mCross46Tv.setVisibility(View.GONE);
        mCross51Tv.setVisibility(View.GONE);mCross52Tv.setVisibility(View.GONE);  mCross53Tv.setVisibility(View.GONE);
        mCross54Tv.setVisibility(View.GONE);mCross55Tv.setVisibility(View.GONE);mCross56Tv.setVisibility(View.GONE);
        mCross61Tv.setVisibility(View.GONE);mCross62Tv.setVisibility(View.GONE);  mCross63Tv.setVisibility(View.GONE);
        mCross64Tv.setVisibility(View.GONE);mCross65Tv.setVisibility(View.GONE);mCross66Tv.setVisibility(View.GONE);
    }

    @Override
    protected void initListener() {
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageEvent createSlan = new MessageEvent();
                createSlan.setMsgType(MsgType.MSG_CONFIG_SLAN_INFO);
                EventBus.getDefault().post(createSlan);
            }
        });

        mTitleAmpParamTv.setOnClickListener(this);
        mTitleMeltParamTv.setOnClickListener(this);
        mAddSectionTv.setOnClickListener(this);
        mAddStepTv.setOnClickListener(this);
        mDeleteTv.setOnClickListener(this);
        mSaveExTv.setOnClickListener(this);
        mBasicInfoTabTv.setOnClickListener(this);
        mParamTabTv.setOnClickListener(this);
        mConfigProgramTv.setOnClickListener(this);
        mProgramTypeTv.setOnClickListener(this);
        mReactionTv.setOnClickListener(this);
        mAmpAlgorithmTv.setOnClickListener(this);

        mFamCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                isFamThresholdCheck = isCheck;
                checkMeltParamState();
                checkCrossTalk();
            }
        });
        mHexCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                isHexThresholdCheck = isCheck;
                checkMeltParamState();
                checkCrossTalk();
            }
        });
        mRoxCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                isRoxThresholdCheck = isCheck;
                checkMeltParamState();
                checkCrossTalk();
            }
        });
        mCY5Ck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                isCY5ThresholdCheck = isCheck;
                checkMeltParamState();
                checkCrossTalk();
            }
        });
        mQuaSarCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                isQuaSarThresholdCheck = isCheck;
                checkMeltParamState();
                checkCrossTalk();
            }
        });
        mAttoCk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                isAttoThresholdCheck = isCheck;
                checkMeltParamState();
                checkCrossTalk();
            }
        });



        mSLANSections = new ArrayList<ConfigSLANSection>();
        mSLanProgramRv.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        mSLanProgramRv.addItemDecoration(new DividerDecoration(this.getActivity(), 2));
        slanStepAdapter = new ConfigSLANAdapter(getActivity().getApplicationContext());
        slanStepAdapter.setProgramSteps(mSLANSections);
        mSLanProgramRv.setAdapter(slanStepAdapter);
        slanStepAdapter.setOnItemClickListener(new ConfigSLANAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position,int step_index) {
                click_section_index = position;//当前点击的
                click_step_index = step_index;
                ConfigSLANSection section = mSLANSections.get(click_section_index);
                int programType = section.getProgramType();
                if(programType == Constants.PROGRAM_TYPE_MELT){
                    mDeleteTv.setEnabled(false);
                }else {
                    mDeleteTv.setEnabled(true);
                }
            }

            @Override
            public void onAcquisitionClick(int sec_index,int step_index) {
                //获取点击的程序段
                ConfigSLANSection section = mSLANSections.get(sec_index);
                int cycle = section.getCycles();
                //获取程序段的步骤列表
                ArrayList<ConfigSLANSection.Step> items = section.getItems();
                // 判断当前步骤的保持时间是否大于14s
                ConfigSLANSection.Step step = items.get(step_index);
                if(null != items && items.size()>0){
                    int holdtime = step.getHoldTime();
                    /*if(holdtime<14){
                        //"荧光检测步骤保持时间必须大于14s
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.config_info_toast_amp_step_holdtime),Toast.LENGTH_LONG).show();
                        return;
                    }else{
                        if(step.isAcquisition()){
                            Log.i("AAA","11程序段步骤有荧光检测" + sec_index);
                            Log.i("AAA","11步骤有荧光检测" + step_index);
                            step.setAcquisition(false);
                        }else{
                            //判断步骤里是否有荧光检测
                            if(isAcquisition(items)){
                                //有荧光检测，则清除当前程序所有荧光，在对点击的步骤赋值荧光
                             //   Log.i("AAA","11程序段里有荧光检测" + sec_index);
                                for(ConfigSLANSection.Step step1 :items){
                                    step1.setAcquisition(false);
                                }
                            }
                            step.setAcquisition(true);
                        }
                        slanStepAdapter.setProgramSteps(mSLANSections);
                        updateUI();

                    }*/
                    if(step.isAcquisition()){
                        Log.i("AAA","11程序段步骤有荧光检测" + sec_index);
                        Log.i("AAA","11步骤有荧光检测" + step_index);
                        step.setAcquisition(false);
                    }else{
                        //判断步骤里是否有荧光检测
                        if(isAcquisition(items)){
                            //有荧光检测，则清除当前程序所有荧光，在对点击的步骤赋值荧光
                            //   Log.i("AAA","11程序段里有荧光检测" + sec_index);
                            for(ConfigSLANSection.Step step1 :items){
                                step1.setAcquisition(false);
                            }
                        }
                        step.setAcquisition(true);
                    }
                    slanStepAdapter.setProgramSteps(mSLANSections);
                    updateUI();
                }

            }

            @Override
            public void onItemTimeOnClick(View view,int sec_index, int step_index) {
                //获取程序段
                set_section_index = sec_index;
                set_step_index = step_index;
                ConfigSLANSection section = mSLANSections.get(sec_index);
                ConfigSLANSection.Step step = section.getItems().get(step_index);
                int holdtime = step.getHoldTime();
                //保持时间
                showNumKeyboardDialog(3,view,String.valueOf(holdtime),-1);
            }

            @Override
            public void onItemTempOnClick(View view,int sec_index, int step_index,int tempType) {
                set_section_index = sec_index;
                set_step_index = step_index;
                ConfigSLANSection section = mSLANSections.get(sec_index);
                ConfigSLANSection.Step step = section.getItems().get(step_index);

                if(section.getProgramType() == Constants.PROGRAM_TYPE_AMP){
                    double temp = step.getTemperature();
                    showNumKeyboardDialog(4,view,String.valueOf(temp),0);
                }else if(section.getProgramType() == Constants.PROGRAM_TYPE_MELT){
                    if(tempType == 1){
                        showNumKeyboardDialog(4,view,String.valueOf(step.getLeftTemperature()),1);
                    }else if(tempType == 2){
                        showNumKeyboardDialog(4,view,String.valueOf(step.getRightTemperature()),2);
                    }
                }

            }
        });

        //扩增曲线算法参数
        mAmpAlgorithmAdapter.setOnItemClickListener(new AmpAlgorithmAdapter.OnItemClickListener() {
            @Override
            public void onQualitativeAnalysis(View view,int position) {
                mAmpAlogrithm_pos = position;
                //打开分析类型
                expandQualitativeAnalysis(view);
            }

            @Override
            public void onBeginBase(View view,int position) {
                mAmpAlogrithm_pos = position;
                //基线起点
                ConfigTarget configTarget = mParamTargets.get(position);
                String beginBaseline = String.valueOf(configTarget.getBeginBaseline());
                showNumKeyboardDialog(9,view,beginBaseline,-1);
            }

            @Override
            public void onEndbaseLine(View view,int position) {
                mAmpAlogrithm_pos = position;
                //基线终点
                ConfigTarget configTarget = mParamTargets.get(position);
                String endBaseline = String.valueOf(configTarget.getEndBaseline());
                showNumKeyboardDialog(8,view,endBaseline,-1);
            }

            @Override
            public void onOptimizationMode(View view,int position) {
                mAmpAlogrithm_pos = position;
                //基线优化
                expandOptimizationMode(view);
            }

            @Override
            public void onChangeThreshold(View view,int position) {
                mAmpAlogrithm_pos = position;
                //手动阈值
                ConfigTarget configTarget = mParamTargets.get(position);
                String threshold = nf1.format(configTarget.getThreshold());
                showNumKeyboardDialog(10,view,threshold,-1);
            }

            @Override
            public void onDigitalFilter(View view,int position) {
                mAmpAlogrithm_pos = position;
                expandDigitalFilter(view);
            }

            @Override
            public void onAutoThreshold(int position) {
                mAmpAlogrithm_pos = position;
                ConfigTarget configTarget = mParamTargets.get(position);
                if(configTarget.isAutoThreshold()){
                    configTarget.setAutoThreshold(false);
                }else {
                    configTarget.setAutoThreshold(true);
                }
                mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);

            }
        });

        mMeltParamterAdapter.setOnItemClickListener(new MeltParamterAdapter.OnItemClickListener() {
            @Override
            public void onChangeMinTemperature(View view, int position) {
                    //最低温度
                meltParamter_pos = position;
                double min_temp = mParamTargets.get(position).getMinTemperature();
                String minTemp = nf2.format(min_temp);
                showNumKeyboardDialog(11,view,minTemp,-1);
            }

            @Override
            public void onChangeMaxTemperature(View view, int position) {
                    //最高温度
                meltParamter_pos = position;
                double max_temp = mParamTargets.get(position).getMaxTemperature();
                String maxTemp = nf2.format(max_temp);
                showNumKeyboardDialog(12,view,maxTemp,-1);
            }

            @Override
            public void onMeltPeakAlgorithmTv(View view, int position) {
                //熔解曲线算法
                meltParamter_pos = position;
                expandMeltPeakAlg(view);
            }

            @Override
            public void onChangePeekHighthreshold(View view, int position) {
                //峰值宽高
                meltParamter_pos = position;
                double peekHighThreshold = mParamTargets.get(position).getPeekHighThreshold();
                String peekHighThresholdStr = nf1.format(peekHighThreshold);
                showNumKeyboardDialog(13,view,peekHighThresholdStr,-1);
            }

            @Override
            public void onPeekType(View view, int position) {
                //峰型
                meltParamter_pos = position;
                expandPeekType(view);
            }
        });

        detectRangeAdapter.setOnItemClickListener(new DetectRangeAdapter.OnItemClickListener() {
            @Override
            public void onChangeDetectMin(View view, int position) {
                //检测下限
                detect_range_pos = position;
                double detectMin = mDetectRanges.get(position).getDetectMin();
                showNumKeyboardDialog(2,view,nf1.format(detectMin),-1);
            }

            @Override
            public void onChangeDetectMax(View view, int position) {
                //检测上限
                detect_range_pos = position;
                double detectMax = mDetectRanges.get(position).getDetectMax();
                showNumKeyboardDialog(14,view,nf1.format(detectMax),-1);
            }
        });
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            if(!StringUtil.isEmpty(fileName)&& isEdit){
                //编辑项目
                openSLANExTemplate(fileName);
            }else{
                //创建项目
                mParamTargets.clear();
                mProgramNameTv.setText("");
                type = Constants.PROGRAM_TYPE_AMP;
                initThreshold(type);
                mProgramTypeTv.setText(getActivity().getResources().getString(R.string.config_info_aq));

                mTitleMeltParamTv.setVisibility(View.INVISIBLE);
                mTitleAmpParamTv.setTextColor(getActivity().getResources().getColor(R.color.white));
                mTitleAmpParamTv.setBackgroundResource(R.drawable.shape_rect_blue);
                mAMPParamLayout.setVisibility(View.VISIBLE);
                mMeltParamLayout.setVisibility(View.GONE);

            }
        }else{
            initVariable();
            mParamTargets.clear();
            click_step_index = -1;
            click_section_index = -1;
            ampAmpAlgorithm = 0;
            detect_range_pos = -1;
            meltParamter_pos = -1;
            mAmpAlogrithm_pos = -1;
        }
    }

    private void initVariable(){
        isEdit = false;
        project = null;
        isCreateProgram = false;
    }

    private boolean isEdit = false;
    private String fileName;
    public void setEditExFile(String fileName,boolean isEdit){
        this.fileName = fileName;
        this.isEdit = isEdit;
    }
    //判断程序的步骤是否有荧光检测
    private boolean isAcquisition(ArrayList<ConfigSLANSection.Step> steps){
        for(ConfigSLANSection.Step step :steps){
            if(step.isAcquisition()){
                return true;
            }
        }
        return false;
    }

    private void initPopupWindow(){
        mTypeList = new ArrayList<String>();
        mTypeList.add(getActivity().getResources().getString(R.string.config_info_aq));
        mTypeList.add(getActivity().getResources().getString(R.string.config_info_melt));
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        spinnerAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mTypeList);
        mSpnnierLv= (ListView) view.findViewById(R.id.spinner_listview);
        mSpnnierLv.setAdapter(spinnerAdapter);
        mSpinnerPopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mSpinnerPopWindow.setOutsideTouchable(true);
        mSpinnerPopWindow.setFocusable(true);
        mSpinnerPopWindow.setContentView(view);
        mSpinnerPopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
//        popupWindow.showAtLocation(view, Gravity.LEFT,0,0);
        mSpinnerPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapse();
            }
        });

        spinnerAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                int lastType = type;
                if(lastType != position){
                    //重新初始化
                    if(!isFamThresholdCheck && !isHexThresholdCheck && !isRoxThresholdCheck && !isCY5ThresholdCheck
                        && !isQuaSarThresholdCheck && !isAttoThresholdCheck) {
                        //创建
                        type = position;
                        mProgramTypeTv.setText(mTypeList.get(position));
                        collapse();
                        initThreshold(type);

                    }else{
                        //是否确认修改项目类型？确认修改后所有信息将被删除"
                        showNoticeDialog(getActivity().getResources().getString(R.string.config_info_dialog_edit_projecttype)
                                ,0,position);
                    }

                }else{
                    collapse();
                }
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }


    private void initMeltPeakAlgorithm(){
        mMeltPeakAlgorithmList = new ArrayList<String>();
        mMeltPeakAlgorithmList.add(getActivity().getResources().getString(R.string.config_info_meltpeakalgorithm_advance));
        mMeltPeakAlgorithmList.add(getActivity().getResources().getString(R.string.config_info_meltpeakalgorithm_standard));
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter meltAlgorithmAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mMeltPeakAlgorithmList);
        ListView mMeltPeakAlgorithmLv= (ListView) view.findViewById(R.id.spinner_listview);
        mMeltPeakAlgorithmLv.setAdapter(meltAlgorithmAdapter);
        meltAlgPopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        meltAlgPopWindow.setOutsideTouchable(true);
        meltAlgPopWindow.setFocusable(true);
        meltAlgPopWindow.setContentView(view);
        meltAlgPopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
//        popupWindow.showAtLocation(view, Gravity.LEFT,0,0);
        meltAlgPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseMeltPeakAlg();
            }
        });

        meltAlgorithmAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget configTarget = mParamTargets.get(meltParamter_pos);
                configTarget.setMeltPeakAlgorithm(position);
                mMeltParamterAdapter.setMeltParamter(mParamTargets);
                collapseMeltPeakAlg();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private void initAMPAlgorithm(){
        String[] ampAlgorithmArray= getResources().getStringArray(R.array.AMPAlgorithm);
        mAmpAlgorithmList = new ArrayList<String>(ampAlgorithmArray.length);
        Collections.addAll(mAmpAlgorithmList, ampAlgorithmArray);
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        ampAlgorithmAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mAmpAlgorithmList);
        mAmpAlgorithmLv= (ListView) view.findViewById(R.id.spinner_listview);
        mAmpAlgorithmLv.setAdapter(ampAlgorithmAdapter);
        mAmpAlgWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAmpAlgWindow.setOutsideTouchable(true);
        mAmpAlgWindow.setFocusable(true);
        mAmpAlgWindow.setContentView(view);
        mAmpAlgWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
//        popupWindow.showAtLocation(view, Gravity.LEFT,0,0);
        mAmpAlgWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseAmpAlg();
            }
        });

        ampAlgorithmAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                mAmpAlgorithmTv.setText(mAmpAlgorithmList.get(position));
                ampAmpAlgorithm = position;
                collapseAmpAlg();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }
    private boolean isAmpAlgExpanded = false;
    //扩增曲线算法
    private void collapseAmpAlg(){
        if (mAmpAlgWindow != null && mAmpAlgWindow.isShowing()) {
            mAmpAlgWindow.dismiss();
        }
        isAlgorithmExpanded = false;
    }

    //展开扩增曲线算法
    private void expandAmpAlg(View view){
        if(mAmpAlgorithmList== null && mAmpAlgorithmList.isEmpty()){
            return;
        }
        mAmpAlgWindow.showAsDropDown(view);
        isAlgorithmExpanded = true;
    }



    /**
     * 初始化扩增程序
     * 两个程序段：第一个程序段循环数量：1，第二个程序段的循环数量是45
     * 第一个程序段没有荧光检测，第二个程序段的第二个步骤进行荧光检测
     */
    private void initSLANAmpSections(){
        mSLANSections.clear();
        ArrayList<ConfigSLANSection.Step> slanItems1 = new ArrayList<>();
        ConfigSLANSection.Step step1 = new ConfigSLANSection.Step();
        step1.setTemperature((double)50);
        step1.setStep_index(0);//step的序号
        step1.setStepType(Constants.PROGRAM_TYPE_AMP);
        step1.setHoldTime(120);
        step1.setAcquisition(false);
        step1.setSectionIndex(0);//程序段的position
        ConfigSLANSection.Step step2= new ConfigSLANSection.Step();
        step2.setTemperature((double)95);
        step2.setStep_index(1);//step的序号
        step2.setStepType(Constants.PROGRAM_TYPE_AMP);
        step2.setHoldTime(300);
        step2.setAcquisition(false);
        step2.setSectionIndex(0);//程序段的position
        slanItems1.add(step1);
        slanItems1.add(step2);
        ConfigSLANSection section1 = new ConfigSLANSection();
        section1.setItems(slanItems1);
        section1.setIdx(0);
        section1.setName("程序段1");
        section1.setCycles(1);
        section1.setProgramType(Constants.PROGRAM_TYPE_AMP);
        mSLANSections.add(section1);

        //第二个程序段
        ArrayList<ConfigSLANSection.Step> slanItems2 = new ArrayList<>();
        ConfigSLANSection.Step step3 = new ConfigSLANSection.Step();
        step3.setTemperature((double)95);
        step3.setStep_index(0);//step的序号
        step3.setStepType(Constants.PROGRAM_TYPE_AMP);
        step3.setHoldTime(15);
        step3.setAcquisition(false);
        step3.setSectionIndex(1);//程序段的position
        ConfigSLANSection.Step step4 = new ConfigSLANSection.Step();
        step4.setTemperature((double)60);
        step4.setStep_index(1);//step的序号
        step4.setStepType(Constants.PROGRAM_TYPE_AMP);
        step4.setHoldTime(40);
        step4.setSectionIndex(1);//程序段的position
        step4.setAcquisition(true);
        slanItems2.add(step3);
        slanItems2.add(step4);
        ConfigSLANSection section2 = new ConfigSLANSection();
        section2.setItems(slanItems2);
        section2.setIdx(1);
        section2.setName("程序段2");
        section2.setCycles(45);
        section2.setProgramType(Constants.PROGRAM_TYPE_AMP);
        mSLANSections.add(section2);

        click_section_index = 0;
        click_step_index = 0;
        slanStepAdapter.setSelectedPosition(0);
        updateUI();
    }

    /**
     *  初始化标准熔解程序
     *  第一个程序段是扩增程序（一个步骤 保持时间），
     *  第二个程序段是熔解程序（温度60-90,）
     */
    private void initSLANMeltSections(){
        mSLANSections.clear();
        ArrayList<ConfigSLANSection.Step> slanItems1 = new ArrayList<>();
        ConfigSLANSection.Step step1 = new ConfigSLANSection.Step();
        step1.setTemperature((double)80);
        step1.setStep_index(0);//step的序号
        step1.setStepType(Constants.PROGRAM_TYPE_AMP);
        step1.setHoldTime(120);
        step1.setAcquisition(false);
        step1.setSectionIndex(0);//程序段的position
        slanItems1.add(step1);
        ConfigSLANSection section1 = new ConfigSLANSection();
        section1.setItems(slanItems1);
        section1.setIdx(0);
        section1.setName("程序段1");
        section1.setCycles(12);
        section1.setProgramType(Constants.PROGRAM_TYPE_AMP);
        mSLANSections.add(section1);

        ArrayList<ConfigSLANSection.Step> slanItems2 = new ArrayList<>();
        ConfigSLANSection.Step step2 = new ConfigSLANSection.Step();
        step2.setSectionIndex(1);
        step2.setStep_index(0);
        step2.setTemperature((double)90);
        step2.setStepType(Constants.PROGRAM_TYPE_MELT);
        step2.setLeftTemperature((double)35);
        step2.setRightTemperature((double)85);
        slanItems2.add(step2);
        ConfigSLANSection section2 = new ConfigSLANSection();
        section2.setIdx(1);
        section2.setName("熔解程序段");
        section2.setScanMode(Constants.MELT_SCANMODE_STEP);//0：Continue模式//1:Step模式
        section2.setRate((double)0.1);//升温速率
        section2.setInterval((double)0.1);
        section2.setMeltHoldtime(10);
        section2.setItems(slanItems2);
        section2.setProgramType(Constants.PROGRAM_TYPE_MELT);
        mSLANSections.add(section2);

        click_section_index = 0;
        click_step_index = 0;
        slanStepAdapter.setSelectedPosition(0);
        updateUI();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != mSLANSections){
            mSLANSections.clear();
            mSLANSections = null;
        }

        if(null != changeModeDialog){
            changeModeDialog.dismiss();
            changeModeDialog = null;
        }

        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }

        if(null != mSpinnerPopWindow){
            mSpinnerPopWindow.dismiss();
            mSpinnerPopWindow = null;
        }

        if(null != mTypeList){
            mTypeList.clear();
            mTypeList = null;
        }

        if(mMeltPeakAlgorithmList != null){
            mMeltPeakAlgorithmList.clear();
            mMeltPeakAlgorithmList = null;
        }
        if(null != meltAlgPopWindow){
            meltAlgPopWindow.dismiss();
            meltAlgPopWindow = null;
        }
        //
        if(null != mAmpAlgorithmList){
            mAmpAlgorithmList.clear();
            mAmpAlgorithmList = null;
        }
        if(null != mAmpAlgWindow){
            mAmpAlgWindow.dismiss();
            mAmpAlgWindow = null;
        }

        if(null != mConfigProjects){
            mConfigProjects.clear();
            mConfigProjects = null;
        }

        if(null != mAmpAysTypes){
            mAmpAysTypes.clear();
            mAmpAysTypes = null;
        }
        if(null != mAysTypePopWindow){
            mAysTypePopWindow.dismiss();
            mAysTypePopWindow = null;
        }

        if(null != mOptimizationModes){
            mOptimizationModes.clear();
            mOptimizationModes = null;
        }

        if(null != mPeekTypeWindow){
            mPeekTypeWindow.dismiss();
            mPeekTypeWindow = null;
        }
        if(null != mPeekTypes){
            mPeekTypes.clear();
            mPeekTypes = null;
        }
        if(null != mDetectRanges){
            mDetectRanges.clear();
            mDetectRanges = null;
        }
        if(null != mParamTargets){
            mParamTargets.clear();
            mParamTargets = null;
        }
        if(null != mAddSectionWindow){
            mAddSectionWindow.dismiss();
            mAddSectionWindow = null;
        }
        if(null != mAddStepsWindow){
            mAddStepsWindow.dismiss();
            mAddStepsWindow = null;
        }
        if(null != mAddSections){
            mAddSections.clear();
            mAddSections = null;
        }
        if(null != mAddSteps){
            mAddSteps.clear();
            mAddSteps = null;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.config_slan_param_setting_title_melt_tv_1){
            //熔解曲线参数
            isBasicParam = false;
            mTitleMeltParamTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mTitleAmpParamTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            mTitleAmpParamTv.setTextColor(getActivity().getResources().getColor(R.color.black));
            mTitleMeltParamTv.setTextColor(getActivity().getResources().getColor(R.color.white));
            mAMPParamLayout.setVisibility(View.GONE);
            mMeltParamLayout.setVisibility(View.VISIBLE);
            showMeltParam();
        }else if(id == R.id.config_slan_param_setting_title_basic_tv_1){
            //基本参数
            isBasicParam =true;
            mTitleAmpParamTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mTitleMeltParamTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            mTitleAmpParamTv.setTextColor(getActivity().getResources().getColor(R.color.white));
            mTitleMeltParamTv.setTextColor(getActivity().getResources().getColor(R.color.black));
            mAMPParamLayout.setVisibility(View.VISIBLE);
            mMeltParamLayout.setVisibility(View.GONE);
            showBasicParam();
        } else if(id == R.id.add_slan_section_tv){
            if(click_section_index != -1 && click_step_index != -1){
                expandAddSection(view);
            }else {
                //请选择要添加的位置
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_info_toast_add_position),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.add_slan_step_tv){
            if(click_section_index != -1 && click_step_index != -1){
                expandAddStep(view);
            }else{
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_info_toast_add_position),Toast.LENGTH_LONG).show();
            }
        }else if(id ==R.id.delete_slan_tv){
            if(click_section_index != -1){
                deleteSectionAndStep();
            }else{
                //请选择步骤
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_info_toast_select_step),Toast.LENGTH_LONG).show();
            }

        }else if(id == R.id.save_ex_template_tv){
            String name = mProgramNameTv.getText().toString().trim();
            if(StringUtil.isEmpty(name)){
                //请输入项目名称
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_info_toast_project_name),Toast.LENGTH_LONG).show();
            }else{
                if(isHasSameSlanFile(name)){
                    //有相同的项目文件,是否覆盖
                    showNoticeDialog(getActivity().getResources().getString(R.string.config_info_dialog_same_project),
                            1,-1);
                }else {
                    saveExperimentTemplate(name);
                }
            }
        }else if(id == R.id.config_project_info_tv){
            //基本信息
            showBasicInfo();
        }else if(id == R.id.config_program_tv){
            if(!isFamThresholdCheck && !isHexThresholdCheck && !isCY5ThresholdCheck && !isRoxThresholdCheck
                && !isQuaSarThresholdCheck && !isAttoThresholdCheck){
                //必须勾选项目参数才可以创建程序
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_info_toast_check_experiment_parameter),Toast.LENGTH_LONG).show();
            }else{
                //构建程序
                showBasicProgram();
            }

        }else if(id == R.id.config_slan_param_tv){
            if(!isFamThresholdCheck && !isHexThresholdCheck && !isCY5ThresholdCheck && !isRoxThresholdCheck
                    && !isQuaSarThresholdCheck && !isAttoThresholdCheck){
                //必须勾选项目通道
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_info_toast_check_channel),Toast.LENGTH_LONG).show();
            }else{
                //显示项目参数
                showExperimentParam();
            }
        }else if(id == R.id.config_info_program_type_tv){
            if(isExpanded){
                //展开
                collapse();
            }else{
                //没有展开，点击展开
                expand();
            }
        }
        else if(id == R.id.config_info_reaction_tv){
            String reaction = mReactionTv.getText().toString();
            showNumKeyboardDialog(7,mReactionTv,reaction,-1);
        }else if(id == R.id.config_slan_amp_algorithm_tv_1){
            if(!isAmpAlgExpanded){
                expandAmpAlg(view);
            }else {
                collapseAmpAlg();
            }
        }
    }
    //展开
    private void expand(){
        if(mTypeList == null && mTypeList.isEmpty()){
            return;
        }
        /*textView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_order_black));
        imageView.setImageResource(R.drawable.expand_arrows_fold);*/
        mSpinnerPopWindow.showAsDropDown(mProgramTypeTv);
        isExpanded = true;
    }

    //收缩
    private void collapse(){
        if (mSpinnerPopWindow != null && mSpinnerPopWindow.isShowing()) {
            mSpinnerPopWindow.dismiss();
        }

        isExpanded = false;
    }

    private boolean isAlgorithmExpanded = false;
    private void expandMeltPeakAlg(View view){
        if(mMeltPeakAlgorithmList== null && mMeltPeakAlgorithmList.isEmpty()){
            return;
        }
        /*textView.setTextColor(ContextCompat.getColor(getContext(), R.color.text_order_black));
        imageView.setImageResource(R.drawable.expand_arrows_fold);*/
        meltAlgPopWindow.showAsDropDown(view);
        isAlgorithmExpanded = true;
    }

    private void collapseMeltPeakAlg(){
        if (meltAlgPopWindow != null && meltAlgPopWindow.isShowing()) {
            meltAlgPopWindow.dismiss();
        }
        isAlgorithmExpanded = false;
    }

    //显示信息和项目参数
    private void showBasicInfo(){
        mBasicInfoLayout.setVisibility(View.VISIBLE);
        mProgramLayout.setVisibility(View.GONE);
        mParamLayout.setVisibility(View.GONE);
        mDeleteTv.setVisibility(View.INVISIBLE);
        mAddSectionTv.setVisibility(View.INVISIBLE);
        mAddStepTv.setVisibility(View.INVISIBLE);
        mBasicInfoTabTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
        mParamTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
        mConfigProgramTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
    }

    /**
     * 显示程序
     */
    private void showBasicProgram(){
        if(!isEdit){
            //isEdit false表示创建而不是编辑
            if(!isCreateProgram){
                if(type == Constants.PROGRAM_TYPE_AMP){
                    initSLANAmpSections();
                }else if(type == Constants.PROGRAM_TYPE_MELT){
                    initSLANMeltSections();
                }
                isCreateProgram = true;
            }
        }
        mBasicInfoLayout.setVisibility(View.GONE);
        mProgramLayout.setVisibility(View.VISIBLE);
        mParamLayout.setVisibility(View.GONE);
        mDeleteTv.setVisibility(View.VISIBLE);
        mAddSectionTv.setVisibility(View.VISIBLE);
        mAddStepTv.setVisibility(View.VISIBLE);
        mBasicInfoTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
        mParamTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
        mConfigProgramTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
    }

    /**
     * 显示项目参数
     */
    private void showExperimentParam(){
        mBasicInfoLayout.setVisibility(View.GONE);
        mProgramLayout.setVisibility(View.GONE);
        mParamLayout.setVisibility(View.VISIBLE);
        mDeleteTv.setVisibility(View.INVISIBLE);
        mAddSectionTv.setVisibility(View.INVISIBLE);
        mAddStepTv.setVisibility(View.INVISIBLE);
        mBasicInfoTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
        mParamTabTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
        mConfigProgramTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
        if(null != mParamTargets){
            if(null != mAmpAlgorithmAdapter){
                mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
            }

            if(null != mMeltParamterAdapter){
                mMeltParamterAdapter.setMeltParamter(mParamTargets);
            }
        }
    }


    /**
     * 修改恢复到原值Threshold
     */
    private void initThreshold(int type){
        mCY5Ck.setChecked(false);
        mFamCk.setChecked(false);
        mHexCk.setChecked(false);
        mRoxCk.setChecked(false);
        mQuaSarCk.setChecked(false);
        mAttoCk.setChecked(false);
        mParamTargets.clear();
        mDetectRanges.clear();
        if(null != mAmpAlgorithmAdapter){
            mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
        }
        if(null != mMeltParamterAdapter){
            mMeltParamterAdapter.setMeltParamter(mParamTargets);
        }
        if(type == Constants.PROGRAM_TYPE_AMP){
            mAMPParamLayout.setVisibility(View.VISIBLE);
            mMeltParamLayout.setVisibility(View.GONE);
            mTitleMeltParamTv.setVisibility(View.INVISIBLE);

        }else {
            mTitleMeltParamTv.setVisibility(View.VISIBLE);
            mMeltParamLayout.setVisibility(View.GONE);
            mAMPParamLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 交叉干扰
     */
    private void checkCrossTalk(){
        initCrossTalk();
        if(isFamThresholdCheck){
            mCrossOneLayout.setVisibility(View.VISIBLE);
            mCrossTitle1Tv.setVisibility(View.VISIBLE);
            mCross11Tv.setVisibility(View.VISIBLE);
            if(isHexThresholdCheck){
                mCross12Tv.setVisibility(View.VISIBLE);
                mCross21Tv.setVisibility(View.VISIBLE);

            }
            if(isRoxThresholdCheck){
                mCross13Tv.setVisibility(View.VISIBLE);
                mCross31Tv.setVisibility(View.VISIBLE);
            }
            if(isCY5ThresholdCheck){
                mCross14Tv.setVisibility(View.VISIBLE);
                mCross41Tv.setVisibility(View.VISIBLE);
            }
            if(isQuaSarThresholdCheck){
                mCross15Tv.setVisibility(View.VISIBLE);
                mCross51Tv.setVisibility(View.VISIBLE);

            }
            if(isAttoThresholdCheck){
                mCross16Tv.setVisibility(View.VISIBLE);
                mCross61Tv.setVisibility(View.VISIBLE);
            }
        }else{
            mCrossOneLayout.setVisibility(View.GONE);
            mCrossTitle1Tv.setVisibility(View.GONE);
        }

        if(isHexThresholdCheck){
            mCrossTitle2Tv.setVisibility(View.VISIBLE);
            mCrossTwoLayout.setVisibility(View.VISIBLE);
            mCross22Tv.setVisibility(View.VISIBLE);
            if(isRoxThresholdCheck){
                mCross23Tv.setVisibility(View.VISIBLE);
                mCross32Tv.setVisibility(View.VISIBLE);
            }
            if(isCY5ThresholdCheck){
                mCross24Tv.setVisibility(View.VISIBLE);
                mCross42Tv.setVisibility(View.VISIBLE);
            }
            if(isQuaSarThresholdCheck){
                mCross25Tv.setVisibility(View.VISIBLE);
                mCross52Tv.setVisibility(View.VISIBLE);
            }
            if(isAttoThresholdCheck){
                mCross26Tv.setVisibility(View.VISIBLE);
                mCross62Tv.setVisibility(View.VISIBLE);
            }
        }else{
            mCrossTitle2Tv.setVisibility(View.GONE);
            mCrossTwoLayout.setVisibility(View.GONE);
        }

        if(isRoxThresholdCheck){
            mCrossThreeLayout.setVisibility(View.VISIBLE);
            mCrossTitle3Tv.setVisibility(View.VISIBLE);
            mCross33Tv.setVisibility(View.VISIBLE);

            if(isCY5ThresholdCheck){
                mCross34Tv.setVisibility(View.VISIBLE);
                mCross43Tv.setVisibility(View.VISIBLE);
            }
            if(isQuaSarThresholdCheck){
                mCross35Tv.setVisibility(View.VISIBLE);
                mCross53Tv.setVisibility(View.VISIBLE);
            }
            if(isAttoThresholdCheck){
                mCross36Tv.setVisibility(View.VISIBLE);
                mCross63Tv.setVisibility(View.VISIBLE);
            }
        }else{
            mCrossThreeLayout.setVisibility(View.GONE);
            mCrossTitle3Tv.setVisibility(View.GONE);
        }

        if(isCY5ThresholdCheck){
            mCrossFourLayout.setVisibility(View.VISIBLE);
            mCross44Tv.setVisibility(View.VISIBLE);
            mCrossTitle4Tv.setVisibility(View.VISIBLE);
            if(isQuaSarThresholdCheck){
                mCross45Tv.setVisibility(View.VISIBLE);
                mCross54Tv.setVisibility(View.VISIBLE);
            }
            if(isAttoThresholdCheck){
                mCross46Tv.setVisibility(View.VISIBLE);
                mCross64Tv.setVisibility(View.VISIBLE);
            }
        }else{
            mCrossFiveLayout.setVisibility(View.GONE);
            mCrossTitle4Tv.setVisibility(View.GONE);
        }

        if(isQuaSarThresholdCheck){
            mCrossFiveLayout.setVisibility(View.VISIBLE);
            mCross55Tv.setVisibility(View.VISIBLE);
            mCrossTitle5Tv.setVisibility(View.VISIBLE);
            if(isAttoThresholdCheck){
                mCross56Tv.setVisibility(View.VISIBLE);
                mCross65Tv.setVisibility(View.VISIBLE);
            }
        }else{
            mCrossFiveLayout.setVisibility(View.GONE);
            mCrossTitle5Tv.setVisibility(View.GONE);
        }

        if(isAttoThresholdCheck){
            mCrossSixLayout.setVisibility(View.VISIBLE);
            mCross66Tv.setVisibility(View.VISIBLE);
            mCrossTitle6Tv.setVisibility(View.VISIBLE);

        }else{
            mCrossSixLayout.setVisibility(View.GONE);
            mCrossTitle6Tv.setVisibility(View.GONE);
        }
    }

    private void checkMeltParamState(){
        if(isFamThresholdCheck){
            ConfigTarget mFAMConfigTarget = new ConfigTarget();
            mFAMConfigTarget.setName("FAM");
            mFAMConfigTarget.setChannelNo(1);
            mFAMConfigTarget.setTargetIndex(1);
            mFAMConfigTarget.setColor("#FF0000FF");

            mFAMConfigTarget.setQualitativeAnalysis(true);//定性还是定量
            mFAMConfigTarget.setBeginBaseline(6);
            mFAMConfigTarget.setEndBaseline(12);
            mFAMConfigTarget.setDigitalFilter(DigitalFilter.High.getValue());
            mFAMConfigTarget.setThreshold((double)0.12);
            mFAMConfigTarget.setAutoThreshold(false);
            mFAMConfigTarget.setOptimizationMode(1);//自动优化
            mFAMConfigTarget.setDetectMin((double)0);
            mFAMConfigTarget.setDetectMax((double)10000000000L);

            mFAMConfigTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm.Advance.getValue());
            mFAMConfigTarget.setMaxTemperature(85);
            mFAMConfigTarget.setMinTemperature(35);
            mFAMConfigTarget.setPeekHighThreshold((double) 10);
            mFAMConfigTarget.setPeekType(PeekType.Positive.getValue());
            if(null != mParamTargets){
                if(mParamTargets.size() == 0){
                    mParamTargets.add(mFAMConfigTarget);
                }else if(mParamTargets.size()>0){
                    if(!isContainConfigTarget(1)){
                        mParamTargets.add(mFAMConfigTarget);
                    }
                }
            }
        }else {
            if(null != mParamTargets && mParamTargets.size()>0){
                ConfigTarget mFamConfigTarget = getConfigTarget(1);
                if(null != mFamConfigTarget){
                    mParamTargets.remove(mFamConfigTarget);
                }
            }
        }

        if(isHexThresholdCheck){
            ConfigTarget mHexConfigTarget = new ConfigTarget();
            mHexConfigTarget.setName("HEX");
            mHexConfigTarget.setChannelNo(2);
            mHexConfigTarget.setTargetIndex(2);
            mHexConfigTarget.setColor("#FF00FF00");

            mHexConfigTarget.setQualitativeAnalysis(true);//定性还是定量
            mHexConfigTarget.setBeginBaseline(6);
            mHexConfigTarget.setEndBaseline(12);
            mHexConfigTarget.setDigitalFilter(DigitalFilter.High.getValue());
            mHexConfigTarget.setThreshold((double)0.12);
            mHexConfigTarget.setAutoThreshold(false);
            mHexConfigTarget.setOptimizationMode(1);//自动优化
            mHexConfigTarget.setDetectMin((double)0);
            mHexConfigTarget.setDetectMax((double)10000000000L);

            mHexConfigTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm.Advance.getValue());
            mHexConfigTarget.setMaxTemperature(85);
            mHexConfigTarget.setMinTemperature(35);
            mHexConfigTarget.setPeekHighThreshold((double) 10);
            mHexConfigTarget.setPeekType(PeekType.Positive.getValue());
            if(null != mParamTargets){
                if(mParamTargets.size() == 0){
                    mParamTargets.add(mHexConfigTarget);
                }else if(mParamTargets.size()>0){
                    if(!isContainConfigTarget(2)){
                        mParamTargets.add(mHexConfigTarget);
                    }
                }
            }
        }else {
            if(null != mParamTargets && mParamTargets.size()>0){
                ConfigTarget mHexConfigTarget = getConfigTarget(2);
                if(null != mHexConfigTarget){
                    mParamTargets.remove(mHexConfigTarget);
                }
            }
        }

        if(isRoxThresholdCheck){
            ConfigTarget mRoxConfigTarget = new ConfigTarget();
            mRoxConfigTarget.setName("ROX");
            mRoxConfigTarget.setChannelNo(3);
            mRoxConfigTarget.setTargetIndex(3);
            mRoxConfigTarget.setColor("#FFFF8000");

            mRoxConfigTarget.setQualitativeAnalysis(true);//定性还是定量
            mRoxConfigTarget.setBeginBaseline(6);
            mRoxConfigTarget.setEndBaseline(12);
            mRoxConfigTarget.setDigitalFilter(DigitalFilter.High.getValue());
            mRoxConfigTarget.setThreshold((double)0.12);
            mRoxConfigTarget.setAutoThreshold(false);
            mRoxConfigTarget.setOptimizationMode(1);//自动优化
            mRoxConfigTarget.setDetectMin((double)0);
            mRoxConfigTarget.setDetectMax((double)10000000000L);

            mRoxConfigTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm.Advance.getValue());
            mRoxConfigTarget.setMaxTemperature(85);
            mRoxConfigTarget.setMinTemperature(35);
            mRoxConfigTarget.setPeekHighThreshold((double) 10);
            mRoxConfigTarget.setPeekType(PeekType.Positive.getValue());
            if(null != mParamTargets){
                if(mParamTargets.size() == 0){
                    mParamTargets.add(mRoxConfigTarget);
                }else if(mParamTargets.size()>0){
                    if(!isContainConfigTarget(3)){
                        mParamTargets.add(mRoxConfigTarget);
                    }
                }
            }
        }else {
            if(null != mParamTargets && mParamTargets.size()>0){
                ConfigTarget mRoxConfigTarget = getConfigTarget(3);
                if(null != mRoxConfigTarget){
                    mParamTargets.remove(mRoxConfigTarget);
                }
            }
        }

        if(isCY5ThresholdCheck){
            ConfigTarget mCY5ConfigTarget = new ConfigTarget();
            mCY5ConfigTarget.setName("CY5");
            mCY5ConfigTarget.setChannelNo(4);
            mCY5ConfigTarget.setTargetIndex(4);
            mCY5ConfigTarget.setColor("#FFFF0000");

            mCY5ConfigTarget.setQualitativeAnalysis(true);//定性还是定量
            mCY5ConfigTarget.setBeginBaseline(6);
            mCY5ConfigTarget.setEndBaseline(12);
            mCY5ConfigTarget.setDigitalFilter(DigitalFilter.High.getValue());
            mCY5ConfigTarget.setThreshold((double)0.12);
            mCY5ConfigTarget.setAutoThreshold(false);
            mCY5ConfigTarget.setOptimizationMode(1);//自动优化
            mCY5ConfigTarget.setDetectMin((double)0);
            mCY5ConfigTarget.setDetectMax((double)10000000000L);


            mCY5ConfigTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm.Advance.getValue());
            mCY5ConfigTarget.setMaxTemperature(85);
            mCY5ConfigTarget.setMinTemperature(35);
            mCY5ConfigTarget.setPeekHighThreshold((double) 10);
            mCY5ConfigTarget.setPeekType(PeekType.Positive.getValue());
            if(null != mParamTargets){
                if(mParamTargets.size() == 0){
                    mParamTargets.add(mCY5ConfigTarget);
                }else if(mParamTargets.size()>0){
                    if(!isContainConfigTarget(4)){
                        mParamTargets.add(mCY5ConfigTarget);
                    }
                }
            }
        }else {
            if(null != mParamTargets && mParamTargets.size()>0){
                ConfigTarget mCY5aConfigTarget = getConfigTarget(4);
                if(null != mCY5aConfigTarget){
                    mParamTargets.remove(mCY5aConfigTarget);
                }
            }
        }

        if(isQuaSarThresholdCheck){
            ConfigTarget mQuaConfigTarget = new ConfigTarget();
            mQuaConfigTarget.setName("QUASAR 705");
            mQuaConfigTarget.setChannelNo(5);
            mQuaConfigTarget.setTargetIndex(5);
            mQuaConfigTarget.setColor("#FF8000FF");

            mQuaConfigTarget.setQualitativeAnalysis(true);//定性还是定量
            mQuaConfigTarget.setBeginBaseline(6);
            mQuaConfigTarget.setEndBaseline(12);
            mQuaConfigTarget.setDigitalFilter(DigitalFilter.High.getValue());
            mQuaConfigTarget.setThreshold((double)0.12);
            mQuaConfigTarget.setAutoThreshold(false);
            mQuaConfigTarget.setOptimizationMode(1);//自动优化
            mQuaConfigTarget.setDetectMin((double)0);
            mQuaConfigTarget.setDetectMax((double)10000000000L);


            mQuaConfigTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm.Advance.getValue());
            mQuaConfigTarget.setMaxTemperature(85);
            mQuaConfigTarget.setMinTemperature(35);
            mQuaConfigTarget.setPeekHighThreshold((double) 10);
            mQuaConfigTarget.setPeekType(PeekType.Positive.getValue());
            if(null != mParamTargets){
                if(mParamTargets.size() == 0){
                    mParamTargets.add(mQuaConfigTarget);
                }else if (mParamTargets.size()>0){
                    if(!isContainConfigTarget(5)){
                        mParamTargets.add(mQuaConfigTarget);
                    }
                }
            }
        }else {
            if(null != mParamTargets && mParamTargets.size()>0){
                ConfigTarget mQuaConfigTarget = getConfigTarget(5);
                if(null != mQuaConfigTarget){
                    mParamTargets.remove(mQuaConfigTarget);
                }
            }
        }

        if(isAttoThresholdCheck){
            ConfigTarget mAttoConfigTarget = new ConfigTarget();
            mAttoConfigTarget.setName("ATTO 425");
            mAttoConfigTarget.setChannelNo(6);
            mAttoConfigTarget.setTargetIndex(6);
            mAttoConfigTarget.setColor("#FF800000");

            mAttoConfigTarget.setQualitativeAnalysis(true);//定性还是定量
            mAttoConfigTarget.setBeginBaseline(6);
            mAttoConfigTarget.setEndBaseline(12);
            mAttoConfigTarget.setDigitalFilter(DigitalFilter.High.getValue());
            mAttoConfigTarget.setThreshold((double)0.12);
            mAttoConfigTarget.setAutoThreshold(false);
            mAttoConfigTarget.setOptimizationMode(1);//自动优化
            mAttoConfigTarget.setDetectMin((double)0);
            mAttoConfigTarget.setDetectMax((double)10000000000L);

            mAttoConfigTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm.Advance.getValue());
            mAttoConfigTarget.setMaxTemperature(85);
            mAttoConfigTarget.setMinTemperature(35);
            mAttoConfigTarget.setPeekHighThreshold((double) 10);
            mAttoConfigTarget.setPeekType(PeekType.Positive.getValue());
            if(null != mParamTargets){
                if(mParamTargets.size() == 0){
                    mParamTargets.add(mAttoConfigTarget);
                }else if(mParamTargets.size()>0){
                    if(!isContainConfigTarget(6)){
                        mParamTargets.add(mAttoConfigTarget);
                    }
                }
            }
        }else {
            if(null != mParamTargets && mParamTargets.size()>0){
                ConfigTarget mAttoConfigTarget = getConfigTarget(6);
                if(null != mAttoConfigTarget){
                    mParamTargets.remove(mAttoConfigTarget);
                }
            }
        }
    }

    /**
     * 判断是否有符合标准的
     */
    private ConfigTarget getConfigTarget(int channelNo){
        if(null != mParamTargets && mParamTargets.size()>0){
            for(ConfigTarget configTarget :mParamTargets){
                if(configTarget.getChannelNo() == channelNo){
                    return configTarget;
                }
            }
        }
        return null;
    }

    private boolean isContainConfigTarget(int channelNo){
        if(null != mParamTargets && mParamTargets.size()>0){
            for(ConfigTarget configTarget :mParamTargets){
                if(configTarget.getChannelNo() == channelNo){
                    return true;
                }
            }
        }
        return false;
    }

    //基本参数显示
    private void showBasicParam(){
        //扩增曲线参数
        mMeltParamLayout.setVisibility(View.GONE);
        mAMPParamLayout.setVisibility(View.VISIBLE);
        if(null != mParamTargets && mAmpAlgorithmAdapter != null){
            mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
        }
    }

    /**
     * 显示熔解参数
     */
    private void showMeltParam() {
        if(null != mParamTargets){
            mMeltParamterAdapter.setMeltParamter(mParamTargets);
        }
    }


    /**
     * 添加在当前程序段前
     */
    private void addSlanSectionAtBefore(){
        if(null != mSLANSections &&mSLANSections.size()>0){
            if(click_section_index < mSLANSections.size()){
                //需要添加的程序段的index
                int index = click_section_index;
                click_section_index = click_section_index + 1;
                ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<>();
                ConfigSLANSection.Step step = new ConfigSLANSection.Step();
                step.setTemperature((double)95);
                step.setStep_index(0);
                step.setStepType(Constants.PROGRAM_TYPE_AMP);
                step.setHoldTime(10);
                step.setSectionIndex(index);
                slanItems.add(step);

                ConfigSLANSection section = new ConfigSLANSection();
                section.setItems(slanItems);
                section.setIdx(index);
                int section_pos = index+1;
                section.setName("程序段"+section_pos);
                section.setCycles(1);
                section.setProgramType(Constants.PROGRAM_TYPE_AMP);
                mSLANSections.add(index,section);


                for(int i =0 ;i<mSLANSections.size();i++){
                    ConfigSLANSection section1 = mSLANSections.get(i);
                    section1.setIdx(i);
                    ArrayList<ConfigSLANSection.Step> items1 = section1.getItems();
                    if(items1 != null && items1.size()>0){
                        for(int k=0; k<items1.size();k++){
                            ConfigSLANSection.Step step1 = items1.get(k);
                            step1.setSectionIndex(i);
                            step1.setStep_index(k);
                        }
                    }
                }
                updateUI();
            }
        }
    }

    /**
     * 添加在程序段的最后
     */
    private void addSLANSectionAtLast(){
        if(null != mSLANSections &&mSLANSections.size()>0){
            if(click_section_index < mSLANSections.size()){
                //需要添加的程序段的index
                int index = mSLANSections.size();
                ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<>();
                ConfigSLANSection.Step step = new ConfigSLANSection.Step();
                step.setTemperature((double)95);
                step.setStep_index(0);
                step.setStepType(Constants.PROGRAM_TYPE_AMP);
                step.setHoldTime(10);
                step.setSectionIndex(index);
                slanItems.add(step);

                ConfigSLANSection section = new ConfigSLANSection();
                section.setItems(slanItems);
                section.setIdx(index);
                int section_pos = index+1;
                section.setName("程序段"+section_pos);
                section.setCycles(1);
                section.setProgramType(Constants.PROGRAM_TYPE_AMP);
                mSLANSections.add(index,section);


                for(int i =0 ;i<mSLANSections.size();i++){
                    ConfigSLANSection section1 = mSLANSections.get(i);
                    section1.setIdx(i);
                    ArrayList<ConfigSLANSection.Step> items1 = section1.getItems();
                    if(items1 != null && items1.size()>0){
                        for(int k=0; k<items1.size();k++){
                            ConfigSLANSection.Step step1 = items1.get(k);
                            step1.setSectionIndex(i);
                            step1.setStep_index(k);
                        }
                    }
                }
                updateUI();
            }
        }
    }



    /**
     * 添加在点击的程序段后面
     * 添加程序段
     */
    private void addSLANSectionAtAfter(){
        if(null != mSLANSections &&mSLANSections.size()>0){
            if(click_section_index < mSLANSections.size()){
                int index = click_section_index + 1;
                ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<>();
                ConfigSLANSection.Step step = new ConfigSLANSection.Step();
                step.setTemperature((double)95);
                step.setStep_index(0);
                step.setStepType(Constants.PROGRAM_TYPE_AMP);
                step.setHoldTime(10);
                step.setSectionIndex(index);
                slanItems.add(step);

                ConfigSLANSection section = new ConfigSLANSection();
                section.setItems(slanItems);
                section.setIdx(index);
                int section_pos = index+1;
                section.setName("程序段"+section_pos);
                section.setCycles(1);
                section.setProgramType(Constants.PROGRAM_TYPE_AMP);
                mSLANSections.add(index,section);


                for(int i =0 ;i<mSLANSections.size();i++){
                    ConfigSLANSection section1 = mSLANSections.get(i);
                    section1.setIdx(i);
                    ArrayList<ConfigSLANSection.Step> items1 = section1.getItems();
                    if(items1 != null && items1.size()>0){
                        for(int k=0; k<items1.size();k++){
                            ConfigSLANSection.Step step1 = items1.get(k);
                            step1.setSectionIndex(i);
                            step1.setStep_index(k);
                        }
                    }
                }
                updateUI();
            }
        }
    }

    /**
     * 添加在当前的点击的步骤之前
     */
    private void addSLANStepAtBefore(){
        if(null != mSLANSections && mSLANSections.size()>0){
            if(click_section_index < mSLANSections.size()){
                ConfigSLANSection section =  mSLANSections.get(click_section_index);
                int programType = section.getProgramType();
                //  确定点击的当前的Section是扩增
                if(programType == Constants.PROGRAM_TYPE_AMP){
                    //添加步骤的items
                    ArrayList<ConfigSLANSection.Step> items = section.getItems();
                    ConfigSLANSection.Step step = new ConfigSLANSection.Step();
                    step.setTemperature((double)95);
                    step.setStepType(Constants.PROGRAM_TYPE_AMP);
                    step.setHoldTime(10);
                    int stepIndex = click_step_index;
                    step.setStep_index(stepIndex);
                    step.setSectionIndex(click_section_index);
                    items.add(step);
                    //重新编号
                    for(int i=0;i<items.size()-1;i++){
                        ConfigSLANSection.Step old_step = items.get(i);
                        if(i<click_step_index){
                            old_step.setStep_index(i);
                        }else if(i>click_step_index){
                            old_step.setStep_index(i+1);
                        }else {
                            //插入的步骤,插入后市插入的步骤是当前点击的步骤的index，当前点击是index+1
                            old_step.setStep_index(i+1);
                        }
                    }
                    Collections.sort(items);
                    int select_pos = 0;
                    for(int i=0;i<mSLANSections.size();i++){
                        if(i <click_section_index){
                            select_pos += mSLANSections.get(i).getItems().size();
                        }else if(click_section_index == i){
                            click_step_index = click_step_index+1;
                            select_pos = select_pos + click_step_index;
                        }
                    }
                    slanStepAdapter.setProgramSteps(mSLANSections);
                    //Log.i("AAA","click_step_index" + click_step_index);
                    //Log.i("AAA","setSelectedPosition" + select_pos);
                    slanStepAdapter.setSelectedPosition(select_pos);
                    updateUI();
                }else{
                    //熔解程序段不能添加步骤
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.config_info_toast_add_step_in_meltsection),Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * 添加在当前的点击的步骤之后
     */
    private void addSLANStepAtAfter(){
        if(null != mSLANSections && mSLANSections.size()>0){
            if(click_section_index < mSLANSections.size()){
                ConfigSLANSection section =  mSLANSections.get(click_section_index);
                int programType = section.getProgramType();
                //  确定点击的当前的Section是扩增
                if(programType == Constants.PROGRAM_TYPE_AMP){
                    //添加步骤的items
                    ArrayList<ConfigSLANSection.Step> items = section.getItems();
                    //插入的步骤
                    ConfigSLANSection.Step step = new ConfigSLANSection.Step();
                    step.setTemperature((double)95);
                    step.setStepType(Constants.PROGRAM_TYPE_AMP);
                    step.setHoldTime(10);
                    int stepIndex = click_step_index+1;
                    step.setStep_index(stepIndex);
                    step.setSectionIndex(click_section_index);
                    items.add(step);

                    //重新编号
                    for(int i=0;i<items.size()-1;i++){
                        ConfigSLANSection.Step old_step = items.get(i);
                        if(i<click_step_index){
                            old_step.setStep_index(i);
                        }else if(i>click_step_index){
                            old_step.setStep_index(i+1);
                        }else {
                            //插入的步骤,插入后市插入的步骤是当前点击的步骤的index，当前点击是index+1
                            click_step_index = i;
                            old_step.setStep_index(click_step_index);
                        }
                    }
                    Collections.sort(items);
                    //点击位置更新
                    int select_pos = 0;
                    for(int i=0;i<mSLANSections.size();i++){
                        if(i <click_section_index){
                            select_pos += mSLANSections.get(i).getItems().size();
                        }else if(click_section_index == i){
                            select_pos = select_pos + click_step_index;
                        }
                    }
                    slanStepAdapter.setProgramSteps(mSLANSections);
                    slanStepAdapter.setSelectedPosition(select_pos);
                    updateUI();
                }else{
                    //熔解程序段不能添加步骤
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.config_info_toast_add_step_in_meltsection),Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * 添加步骤到最后
     */
    private void addSLANStepAtLast(){
        if(null != mSLANSections && mSLANSections.size()>0){
            if(click_section_index < mSLANSections.size()){
                ConfigSLANSection section =  mSLANSections.get(click_section_index);
                int programType = section.getProgramType();
                //  确定点击的当前的Section是扩增
                if(programType == Constants.PROGRAM_TYPE_AMP){
                    //添加步骤的items
                    ArrayList<ConfigSLANSection.Step> items = section.getItems();
                    //插入的步骤
                    ConfigSLANSection.Step step = new ConfigSLANSection.Step();
                    step.setTemperature((double)95);
                    step.setStepType(Constants.PROGRAM_TYPE_AMP);
                    step.setHoldTime(10);
                    step.setStep_index(items.size());
                    step.setSectionIndex(click_section_index);
                    items.add(step);
                    Collections.sort(items);

                    int select_pos = 0;
                    for(int i=0;i<mSLANSections.size();i++){
                        if(i <click_section_index){
                            select_pos += mSLANSections.get(i).getItems().size();
                        }else if(click_section_index == i){
                            select_pos = select_pos + click_step_index;
                        }
                    }
                    slanStepAdapter.setProgramSteps(mSLANSections);
                    slanStepAdapter.setSelectedPosition(select_pos);
                    updateUI();
                }else{
                    //熔解程序段不能添加步骤
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.config_info_toast_add_step_in_meltsection),Toast.LENGTH_LONG).show();
                }
            }
        }
    }



    /**
     * 添加一个步骤:只有扩增才可以添加步骤
     */
    private void addSLANStep(){
        //  确定点击的当前的Section
        if(null != mSLANSections && mSLANSections.size()>0){
            if(click_section_index < mSLANSections.size()){
                ConfigSLANSection section =  mSLANSections.get(click_section_index);
                int programType = section.getProgramType();
                if(programType == Constants.PROGRAM_TYPE_AMP){
                    //添加步骤的items
                    ArrayList<ConfigSLANSection.Step> items = section.getItems();
                    ConfigSLANSection.Step step = new ConfigSLANSection.Step();
                    step.setTemperature((double)95);
                    step.setStepType(Constants.PROGRAM_TYPE_AMP);
                    step.setHoldTime(10);
                    step.setStep_index(items.size());
                    step.setSectionIndex(click_section_index);
                    items.add(step);
                    slanStepAdapter.setProgramSteps(mSLANSections);
                    updateUI();
                }else{
                    //熔解程序段不能添加步骤
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.config_info_toast_add_step_in_meltsection),Toast.LENGTH_LONG).show();
                }
            }
        }

    }

    /**
     * 删除程序或b步骤
     * 当前的熔解程序模式不能删除熔解步骤
     * 当前的扩增程序最后一个程序段和一个步骤是不能删除的
     */
    private void deleteSectionAndStep(){
        //
        if(null != mSLANSections && mSLANSections.size()>0){
            if(mSLANSections.size() >1){
                if(click_section_index < mSLANSections.size()){
                    ConfigSLANSection section = mSLANSections.get(click_section_index);
                    ArrayList<ConfigSLANSection.Step> items = section.getItems();
                    if(items.size()>1){
                        if(click_step_index < items.size()){
                            items.remove(click_step_index);
                        }
                    }else if(items.size() == 1){
                        mSLANSections.remove(section);
                    }

                    for(int i =0 ;i<mSLANSections.size();i++){
                        ConfigSLANSection section1 = mSLANSections.get(i);
                        section1.setIdx(i);
                        ArrayList<ConfigSLANSection.Step> items1 = section1.getItems();
                        if(items1 != null && items1.size()>0){
                            for(int k=0; k<items1.size();k++){
                                ConfigSLANSection.Step step = items1.get(k);
                                step.setSectionIndex(i);
                                step.setStep_index(k);
                            }
                        }
                    }
                    updateUI();
                    click_section_index = -1;
                    click_step_index = -1;
                    slanStepAdapter.setSelectedPosition(click_section_index);
                }
            }else{
                //还有一个程序段
                if(click_section_index == 0){
                    ConfigSLANSection section = mSLANSections.get(click_section_index);
                    ArrayList<ConfigSLANSection.Step> items = section.getItems();
                    if(items.size()>1){
                        if(click_step_index < items.size()){
                            items.remove(click_step_index);
                        }
                    }else if(items.size() == 1){
                        //最后一个步骤不能删除
                        Toast.makeText(getActivity().getApplicationContext(),
                                        getActivity().getResources().getString(R.string.config_info_toast_delete_last_step),Toast.LENGTH_LONG)
                                .show();
                    }

                    for(int i =0 ;i<mSLANSections.size();i++){
                        ConfigSLANSection section1 = mSLANSections.get(i);
                        section1.setIdx(i);
                        ArrayList<ConfigSLANSection.Step> items1 = section1.getItems();
                        if(items1 != null && items1.size()>0){
                            for(int k=0; k<items1.size();k++){
                                ConfigSLANSection.Step step = items1.get(k);
                                step.setSectionIndex(i);
                                step.setStep_index(k);
                            }
                        }
                    }
                    updateUI();
                    click_section_index = -1;
                    click_step_index = -1;
                    slanStepAdapter.setSelectedPosition(click_section_index);
                }
            }

        }

    }


    //获取交叉干扰值
    private ArrayList< ArrayList< Double>> getCorssTalk(){
        ArrayList<ArrayList< Double>> mCorssTalks = new  ArrayList<ArrayList< Double>>();
        ArrayList< Double> corssTalk = new ArrayList<Double>();
        for(int i = 0;i<8;i++){
            corssTalk.add((double)0);
        }
        if(null != mParamTargets && mParamTargets.size()>0){
            if(type == Constants.PROGRAM_TYPE_AMP){
                for(int i=0;i<mParamTargets.size();i++){
                    mCorssTalks.add(corssTalk);
                }
            }else if(type == Constants.PROGRAM_TYPE_MELT){
                for(int i=0;i<mParamTargets.size();i++){
                    mCorssTalks.add(corssTalk);
                }
            }

        }
        return mCorssTalks;
    }

    //获取目标列表
    private ArrayList<Target> getTargets(){
        ArrayList<Target> targets = new ArrayList<>();
        if(null != mParamTargets &&mParamTargets.size()>0){
            if(type == Constants.PROGRAM_TYPE_AMP){
                //定性/定量
                for(ConfigTarget singleConfigTar : mParamTargets){
                    TargetParamter targetParamter= new TargetParamter();
                    targetParamter.setAutoThreshold(singleConfigTar.isAutoThreshold());
                    targetParamter.setBeginBaseline(singleConfigTar.getBeginBaseline());
                    targetParamter.setDetectionMax((double)singleConfigTar.getDetectMax());
                    targetParamter.setDetectionMin((double)singleConfigTar.getDetectMin());
                    targetParamter.setDigitalFilter(singleConfigTar.getDigitalFilter());
                    targetParamter.setEndBaseline(singleConfigTar.getEndBaseline());
                    targetParamter.setOptimizationMode(singleConfigTar.getOptimizationMode());
                    targetParamter.setQualitativeAnalysis(singleConfigTar.isQualitativeAnalysis());
                    targetParamter.setThreshold(singleConfigTar.getThreshold());

                    Target target = new Target();
                    target.setChannelNo(singleConfigTar.getChannelNo());
                    target.setName(singleConfigTar.getName());
                    target.setTargetIndex(singleConfigTar.getTargetIndex());
                    target.setTubeNo(1);
                    target.setTubeName("管1");
                    target.setDye(singleConfigTar.getName());
                    target.setParamter(targetParamter);
                    target.setColor(singleConfigTar.getColor());
                    targets.add(target);
                }

            }else if(type == Constants.PROGRAM_TYPE_MELT){
                //标准熔解曲线
                for(ConfigTarget configTarget :mParamTargets){
                    TargetParamterMelt targetParamterMelt = new TargetParamterMelt();
                    targetParamterMelt.setQualitativeAnalysis(configTarget.isQualitativeAnalysis());
                    targetParamterMelt.setBeginBaseline(configTarget.getBeginBaseline());
                    targetParamterMelt.setEndBaseline(configTarget.getEndBaseline());
                    targetParamterMelt.setAutoThreshold(configTarget.isAutoThreshold());
                    targetParamterMelt.setThreshold(configTarget.getThreshold());
                    targetParamterMelt.setOptimizationMode(configTarget.getOptimizationMode());
                    targetParamterMelt.setDigitalFilter(configTarget.getDigitalFilter());
                    targetParamterMelt.setDetectionMax(configTarget.getDetectMax());
                    targetParamterMelt.setDetectionMin(configTarget.getDetectMin());
                    targetParamterMelt.setMaxTemperature((double)configTarget.getMaxTemperature());
                    targetParamterMelt.setMinTemperature((double)configTarget.getMinTemperature());
                    targetParamterMelt.setNoiseThreshold(0);

                    targetParamterMelt.setPeekHightThreshold(configTarget.getPeekHighThreshold());
                    targetParamterMelt.setPeekWidthThreshold((double)0);
                    targetParamterMelt.setPeekType(configTarget.getPeekType());
                    targetParamterMelt.setMeltPeakAlgorithm(configTarget.getMeltPeakAlgorithm());


                    Target target = new Target();
                    target.setChannelNo(configTarget.getChannelNo());
                    target.setName(configTarget.getName());
                    target.setTargetIndex(configTarget.getTargetIndex());
                    target.setTubeNo(1);
                    target.setTubeName("管1");
                    target.setDye(configTarget.getName());
                    target.setParamter(targetParamterMelt);
                    target.setColor(configTarget.getColor());
                    targets.add(target);
                }
            }
        }

        return targets;
    }


    //获取程序
    private Program getProgram(List<ConfigSLANSection> slanSections){
        Program program = new Program();

        ArrayList<SectionAMP> SectionAMPs = new ArrayList<SectionAMP>();
        ArrayList<SectionMelt> SectionMelts = new ArrayList<SectionMelt>();
        program.setSectionMelts(SectionMelts);
        program.setSectionAMPs(SectionAMPs);
        ArrayList<Section> Sections = new ArrayList<Section>();

        if(slanSections !=null && slanSections.size()>0){
            for(int i = 0;i<slanSections.size();i++){
                ConfigSLANSection configSLANSection = slanSections.get(i);
                int section_index = i+1;
                int programType = configSLANSection.getProgramType();
                ArrayList<ConfigSLANSection.Step> slanItems = configSLANSection.getItems();
                if(programType == Constants.PROGRAM_TYPE_AMP){
                    //扩增程序
                    SectionAMP sectionAMP = new SectionAMP();
                    sectionAMP.setCycles(configSLANSection.getCycles());
                    sectionAMP.setName("程序段"+section_index);
                    sectionAMP.setIdx(configSLANSection.getIdx());
                    ArrayList<Step> Items = new ArrayList<Step>();
                    if(slanItems != null && slanItems.size()>0){
                        for(int j = 0;j<slanItems.size();j++){
                            ConfigSLANSection.Step slanStep = slanItems.get(j);
                            Step step = new Step();
                            step.setHoldTime(slanStep.getHoldTime());
                            step.setIsAdvProgram(false);
                            step.setIsAcquisition(slanStep.isAcquisition());
                            int step_index = j+1;
                            step.setName("步骤"+step_index);
                            step.setRamp(0);
                            step.setStartCycle(1);
                            step.setTempVarValue(0);
                            step.setTemperature(slanStep.getTemperature());

                            Items.add(step);
                        }
                    }
                    sectionAMP.setItems(Items);
                    Sections.add(sectionAMP);
                }else if(programType == Constants.PROGRAM_TYPE_MELT){
                    //熔解程序
                    SectionMelt sectionMelt = new SectionMelt();
                    sectionMelt.setName(configSLANSection.getName());
                    sectionMelt.setHoldTime(configSLANSection.getMeltHoldtime());
                    sectionMelt.setRate(configSLANSection.getRate());
                    sectionMelt.setScanMode(configSLANSection.getScanMode());
                    sectionMelt.setInterval(configSLANSection.getInterval());
                    sectionMelt.setIdx(configSLANSection.getIdx());
                  //  sectionMelt.setInterval((double)1);
                    if(slanItems != null && slanItems.size()>0){
                        for(int j = 0;j<slanItems.size();j++){
                            ConfigSLANSection.Step step = slanItems.get(j);
                            sectionMelt.setStartTemp(step.getLeftTemperature());
                            sectionMelt.setEndTemp(step.getRightTemperature());
                        }
                    }
                    Sections.add(sectionMelt);
                }
            }
        }
        program.setSections(Sections);
        return program;
    }



    //先保存configTargets
    private void saveExperimentTemplate(String name){
        if(null == PcrApplication.slanService){

        }
        int reaction = StringUtil.convertToInt(mReactionTv.getText().toString(),30);
        //保存configTargets
        Collections.sort(mParamTargets);
       // Log.i("AAA","saveExperimentTemplate configTargets" + mParamTargets.size());
        if(mParamTargets != null && mParamTargets.size()>0){
            //判断是定性还是标准曲线
            Project newProject = new Project();
            //交叉干扰
            ArrayList<ArrayList< Double>> CorssTalk = getCorssTalk();
            newProject.setCorssTalk(CorssTalk);
            newProject.setType(type);
            newProject.setNote("");
            newProject.setName(name);//项目名称
            newProject.setUnit("copies/ml");
            newProject.setReaction(reaction);//反应体系
            newProject.setAMPAlgorithm(ampAmpAlgorithm);//
            newProject.setTubeSortByColumn(true);//排序方式
            //目标列表
            ArrayList<Target> Targets= getTargets();
            newProject.setTargets(Targets);
            //项目程序
            Program program = getProgram(mSLANSections);
            newProject.setProgram(program);

           /* Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                @Override
                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                    if (src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }
            }).create();
            String s = gson.toJson(newProject);
            Log.i("AAA","S"+ s);*/
            List<Project> projects = PcrApplication.slanService.getProjects(FileUtils.CONFIG_SLAN_PATH);
            SDKResult result = PcrApplication.slanService.addProject(newProject);
            if (result.getCode() == 0){
                //添加项目成功
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_info_toast_add_project_success),Toast.LENGTH_LONG).show();
            } else{
                Log.i("AAA","添加失败"+ result.getCode());
                showNoticeDialog(result.getMessage(),2,-1);
                Toast.makeText(getActivity().getApplicationContext(),result.getMessage(),Toast.LENGTH_LONG).show();
            }
            saveSlanProject(name);
        }else{
            //请勾选实验参数
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.config_info_toast_select_ex_parameter),Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 保存PCR项目
     * @param name
     */
    private void saveSlanProject(String name){
        if(null == PcrApplication.slanService){
            Toast.makeText(getActivity().getApplicationContext()
            ,"slanServeice 为空无法保存",Toast.LENGTH_LONG).show();
            return;
        }
        boolean save_result = PcrApplication.slanService.saveProject(name, FileUtils.CONFIG_SLAN_PATH + File.separator + name + ".prj");
        Log.i("AAA","保存结果"+ save_result);
        if(save_result){
            //保存项目成功
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.config_info_toast_save_project_success),Toast.LENGTH_LONG).show();
        }else {
            //保存项目失败
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.config_info_toast_save_project_fail),Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 刷新UI
     */
    private void updateUI(){
        if(null != mSLANSections && mSLANSections.size()>0){
            if(null != mProgramTitleLayout){
                mProgramTitleLayout.removeAllViews();
            }
            for(int i = 0; i<mSLANSections.size();i++){
                TextView textView = new TextView(getActivity().getApplicationContext());
                textView.setBackgroundResource(R.color.blue);
                textView.setTextSize(20);
                textView.setGravity(Gravity.CENTER);
                textView.setTextColor(0xffffffff);

                int programType = mSLANSections.get(i).getProgramType();
                if(programType == Constants.PROGRAM_TYPE_AMP){
                    final LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(163*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(0, 0, 2, 0);
                    textView.setLayoutParams(layoutParams);
                    int index = i+1;
                    textView.setText(getActivity().getResources().getString(R.string.runing_pcr_section)+ index);
                }else if(programType == Constants.PROGRAM_TYPE_MELT){
                    final LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(0, 0, 2, 0);
                    textView.setLayoutParams(layoutParams);
                    textView.setText(getActivity().getResources().getString(R.string.runing_pcr_section_melt));
                }
                mProgramTitleLayout.addView(textView);
            }

            slanStepAdapter.setProgramSteps(mSLANSections);
            //初始化标题与初始化
            if(null != mProgramRecycleLayout){
                mProgramRecycleLayout.removeAllViews();
                if(null != mMeltButtomView){
                    if(mMeltButtomView.getParent() != null){
                        mProgramRecycleLayout.removeView(mMeltButtomView);
                    }
                    mProgramRecycleLayout.removeAllViews();
                }
            }
            for(int i = 0; i<mSLANSections.size();i++){

                int programType = mSLANSections.get(i).getProgramType();
                if(programType == Constants.PROGRAM_TYPE_AMP){
                    final LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(163*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(0, 0, 2, 0);
                    TextView textView = new TextView(getActivity().getApplicationContext());
                    textView.setBackgroundResource(R.color.color_recycle);
                    textView.setTextSize(18);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(0xffffffff);
                    textView.setLayoutParams(layoutParams);
                    textView.setText(getResources().getString(R.string.running_pcr_recycle_num) + (mSLANSections.get(i).getCycles()));
                    int k = i;
                    textView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String s = StringUtil.getNumOfMsg(textView.getText().toString().trim());
                            //获取循环次数
                            set_section_index = k;
                            showNumKeyboardDialog(0,textView,s,-1);
                        }
                    });
                    mProgramRecycleLayout.addView(textView);
                }else if(programType == Constants.PROGRAM_TYPE_MELT){
                    final LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(0, 0, 2, 0);
                    mMeltButtomView.setLayoutParams(layoutParams);
                    mMeltContinueLayout = (LinearLayout)mMeltButtomView.findViewById(R.id.melt_section_buttom_continue_layout);
                    mMeltStepLayout = (LinearLayout)mMeltButtomView.findViewById(R.id.melt_section_buttom_step_layout);
                    //扫描模式
                    mScanModeTv = (TextView)mMeltButtomView.findViewById(R.id.melt_section_scanmode_tv);
                    TextView rateTv = (TextView)mMeltButtomView.findViewById(R.id.melt_section_rate_tv);
                   //扫描间隔模式

                    TextView intervalTv  = (TextView) mMeltButtomView.findViewById(R.id.melt_section_interval_tv);
                    TextView holdTimeTv = (TextView)  mMeltButtomView.findViewById(R.id.melt_section_holdtime_tv);

                    scanMode = mSLANSections.get(i).getScanMode();
                    if(scanMode == Constants.MELT_SCANMODE_CONTINUE){
                        mMeltContinueLayout.setVisibility(View.VISIBLE);
                        mMeltStepLayout.setVisibility(View.GONE);
                        mScanModeTv.setText("Continuous");
                        rateTv.setText(nf1.format(mSLANSections.get(i).getRate()));

                    }else{
                        mMeltContinueLayout.setVisibility(View.GONE);
                        mMeltStepLayout.setVisibility(View.VISIBLE);
                        mScanModeTv.setText("Step");
                        intervalTv.setText(nf1.format(mSLANSections.get(i).getInterval()));
                        holdTimeTv.setText(nf2.format(mSLANSections.get(i).getMeltHoldtime()));
                    }


                    mScanModeTv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String scanmode = mScanModeTv.getText().toString();
                            showChangeScanmode(scanmode);
                        }
                    });


                    mMeltContinueLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //编辑熔解速率
                            String rate = rateTv.getText().toString();
                            showNumKeyboardDialog(1,rateTv,rate,-1);
                        }
                    });

                    //step模式扫描间隔

                    LinearLayout intervalLayout = (LinearLayout)mMeltButtomView.findViewById(R.id.melt_step_interval_layout);
                    intervalLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String interval = intervalTv.getText().toString();
                            showNumKeyboardDialog(15,intervalTv,interval,-1);
                        }
                    });
                    //holdtime
                    LinearLayout meltHoldtimeLayout = (LinearLayout)mMeltButtomView.findViewById(R.id.melt_section_interval_holdtime_layout) ;
                    meltHoldtimeLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String holdtime = holdTimeTv.getText().toString();
                            showNumKeyboardDialog(16,holdTimeTv,holdtime,-1);
                        }
                    });

                    mProgramRecycleLayout.addView(mMeltButtomView);
                }
            }
        }
        //section_index = mSLANSections.size()-1;
    }


    /**
     * 修改扫描模式
     */
    private int scanMode = Constants.MELT_SCANMODE_CONTINUE;
    private CustomDialog changeModeDialog;
    private void showChangeScanmode(String scanmode){
        if(null != changeModeDialog){
            changeModeDialog.dismiss();
            changeModeDialog = null;
        }

        if(null == changeModeDialog){
            changeModeDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_change_melt_scanmode_layout, null);
            RadioButton continueMode = (RadioButton)view.findViewById(R.id.dialog_scanmode_continuous_tv);
            RadioButton stepMode = (RadioButton)view.findViewById(R.id.dialog_scanmode_step_tv);
            RadioGroup scanmodeRg =  (RadioGroup)view.findViewById(R.id.dialog_scanmode_rg);
            scanmodeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int checkId) {
                    if(checkId == R.id.dialog_scanmode_continuous_tv){
                        continueMode.setTextColor(getActivity().getResources().getColor(R.color.white));
                        stepMode.setTextColor(getActivity().getResources().getColor(R.color.blue));
                    }else if(checkId == R.id.dialog_scanmode_step_tv){
                        continueMode.setTextColor(getActivity().getResources().getColor(R.color.blue));
                        stepMode.setTextColor(getActivity().getResources().getColor(R.color.white));
                    }
                }
            });
            if(scanmode.equals("Step")){
                stepMode.setChecked(true);
                continueMode.setChecked(false);
                continueMode.setTextColor(getActivity().getResources().getColor(R.color.blue));
                stepMode.setTextColor(getActivity().getResources().getColor(R.color.white));
            }else if(scanmode.equals("Continuous")){
                stepMode.setChecked(false);
                continueMode.setChecked(true);
                continueMode.setTextColor(getActivity().getResources().getColor(R.color.white));
                stepMode.setTextColor(getActivity().getResources().getColor(R.color.blue));
            }

            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    if(continueMode.isChecked()){
                        scanMode = Constants.MELT_SCANMODE_CONTINUE;
                    }else if(stepMode.isChecked()){
                        scanMode = Constants.MELT_SCANMODE_STEP;
                    }
                    updateScanMode();
                    changeModeDialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    changeModeDialog.dismiss();
                    changeModeDialog = null;
                }
            });
            changeModeDialog = builder.create();
            changeModeDialog.show();
        }

    }

    private void updateScanMode(){
        if(null != mMeltContinueLayout && null != mMeltStepLayout){
            if(scanMode == Constants.MELT_SCANMODE_CONTINUE){
                mMeltContinueLayout.setVisibility(View.VISIBLE);
                mMeltStepLayout.setVisibility(View.GONE);
                mScanModeTv.setText("Continuous");
            }else{
                mMeltContinueLayout.setVisibility(View.GONE);
                mMeltStepLayout.setVisibility(View.VISIBLE);
                mScanModeTv.setText("Step");
            }
        }

    }

    /**@param dialogType
     * 2：阈值
     * 4:温度
     * 显示数字键盘输入
     * 5:熔解曲线参数设置
     * 6：基线起点/终点设置
     * 7：反应体系
     */
    private void showNumKeyboardDialog(int dialogType, View view,String s, int tempType){
        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
        if(numKeyboardDialog == null){
            numKeyboardDialog = new NumKeyboardDialog(getActivity());

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
                    if(dialogType == Constants.PROGRAM_TYPE_MELT){
                        ((TextView)view).setText(s);
                        //升温速率，输入范围是0.01——0.06摄氏度/s
                        double rate = (double)(StringUtil.convertToFloat(s,0.00f));
                        if(rate <0.01f){
                            rate = 0.01f;
                        }else if(rate >0.06f){
                            rate = 0.06f;
                        }
                        for(ConfigSLANSection section : mSLANSections){
                            if(section.getProgramType()== Constants.PROGRAM_TYPE_MELT){
                                section.setRate(rate);
                            }
                        }
                        updateUI();

                    }else if(dialogType == Constants.PROGRAM_TYPE_AMP){
                        //更新mSLANSections
                        //扩增的循环次数
                        int recycle = StringUtil.convertToInt(s,0);
                        if (recycle<1){
                            recycle = 1;
                        }else if(recycle>99){
                            recycle = 99;
                        }
                        ((TextView)view).setText(getResources().getString(R.string.running_pcr_recycle_num) + String.valueOf(recycle));
                        ConfigSLANSection section = mSLANSections.get(set_section_index);
                        section.setCycles(recycle);
                        updateUI();
                    }else if(dialogType == 2){
                        //检测下限
                        //0-10000，000，000
                        double detectMin = (double)StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget rangConfig = mDetectRanges.get(detect_range_pos);
                        rangConfig.setDetectMin(detectMin);
                        ConfigTarget configTarget = getConfigTarget(rangConfig.getChannelNo());
                        configTarget.setDetectMin(detectMin);
                        detectRangeAdapter.setRangeList(mDetectRanges);
                        if(null != mAmpAlgorithmAdapter && null != mMeltParamterAdapter){
                            mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                            mMeltParamterAdapter.setMeltParamter(mParamTargets);
                        }
                    }else if(dialogType ==3){
                        //设置保持时间
                        int time = StringUtil.convertToInt(s,0);
                        ConfigSLANSection section = mSLANSections.get(set_section_index);
                        ConfigSLANSection.Step step = section.getItems().get(set_step_index);
                        step.setHoldTime(time);
                        updateUI();
                    }else if(dialogType==4){
                        //设置温度
                        if(tempType == 0){
                            double temp = (double)StringUtil.convertToFloat(s,0.00f);
                            if(temp<4.0f){
                                temp = 4.0f;
                            }else if(temp>99.0f){
                                temp = 99.0f;
                            }
                            ConfigSLANSection section = mSLANSections.get(set_section_index);
                            ConfigSLANSection.Step step = section.getItems().get(set_step_index);
                            step.setTemperature(temp);
                            updateUI();
                        }else if(tempType == 1){
                            //熔解的低温
                            double temp = (double)StringUtil.convertToFloat(s,0.00f);
                            if(temp<4.0f){
                                temp = 4.0f;
                            }else if(temp>99.0f){
                                temp = 99.0f;
                            }
                            ConfigSLANSection section = mSLANSections.get(set_section_index);
                            ConfigSLANSection.Step step = section.getItems().get(set_step_index);
                            step.setLeftTemperature(temp);
                            updateUI();
                        }else if(tempType == 2){
                            //熔解的高温
                            double temp = (double)StringUtil.convertToFloat(s,0.00f);
                            if(temp<4.0f){
                                temp = 4.0f;
                            }else if(temp>99.0f){
                                temp = 99.0f;
                            }
                            ConfigSLANSection section = mSLANSections.get(set_section_index);
                            ConfigSLANSection.Step step = section.getItems().get(set_step_index);
                            step.setRightTemperature(temp);
                            updateUI();
                        }

                    }else if(dialogType == 5){
                        //熔解曲线参数设置
                        ((TextView)view).setText(s);
                    }else if(dialogType ==6){
                        //扩增曲线算法
                        //如果是小数就化整数
                        int base = (int)StringUtil.convertToFloat(s,1.0f);
                        if(base < 1){
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_baseline),Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            ((TextView)view).setText(base + "");
                        }

                    }else if(dialogType == 7){
                        //反應體系
                        int reaction = (int)StringUtil.convertToInt(s,15);
                        if(reaction < 15){
                            //不能小于15ul
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_reaction_min),Toast.LENGTH_LONG).show();
                            return;
                        }else if(reaction>100){
                            //不能大于100ul
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_reaction_max),Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            ((TextView)view).setText(reaction + "");
                        }
                    }else if(dialogType == 8){
                        //基线终点
                        int endbase = (int)StringUtil.convertToFloat(s,1.0f);
                        if(endbase < 1){
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_baseline),Toast.LENGTH_LONG).show();
                            return;
                        }else{
                           ConfigTarget configTarget = mParamTargets.get(mAmpAlogrithm_pos);
                           configTarget.setEndBaseline(endbase);
                           mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                        }
                    }else if(dialogType == 9){
                        //基线起点
                        int beginbase = (int)StringUtil.convertToFloat(s,1.0f);
                        if(beginbase < 1){
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_baseline),Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            ConfigTarget configTarget = mParamTargets.get(mAmpAlogrithm_pos);
                            configTarget.setBeginBaseline(beginbase);
                            mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                        }
                    }else if(dialogType == 10){
                        //手动阈值
                        double threshold = (double) StringUtil.convertToFloat(s,0.00f);
                        if(threshold < 0.01f){
                            threshold = 0.01f;
                        }
                        ConfigTarget configTarget = mParamTargets.get(mAmpAlogrithm_pos);
                        configTarget.setThreshold(threshold);
                        mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                    }else if(dialogType == 11){
                        //最低温度
                        double minTemp = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = mParamTargets.get(meltParamter_pos);
                        configTarget.setMinTemperature(minTemp);
                        mMeltParamterAdapter.setMeltParamter(mParamTargets);
                    }else if(dialogType == 12){
                        //最高温度
                        double maxTemp = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = mParamTargets.get(meltParamter_pos);
                        configTarget.setMaxTemperature(maxTemp);
                        mMeltParamterAdapter.setMeltParamter(mParamTargets);
                    }else if(dialogType == 13){
                        //f峰高阈值
                        double peekHighThreshold = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = mParamTargets.get(meltParamter_pos);
                        configTarget.setPeekHighThreshold(peekHighThreshold);
                        mMeltParamterAdapter.setMeltParamter(mParamTargets);
                    }else if(dialogType == 14){
                        double detectMax = (double)StringUtil.convertToFloat(s,0.00f);
                        if(detectMax > (double)10000000000L){
                            Toast.makeText(getActivity().getApplicationContext(),
                                    "不能大于 10000000000",Toast.LENGTH_LONG).show();
                            return;
                        }else {
                            ConfigTarget rangConfig = mDetectRanges.get(detect_range_pos);
                            rangConfig.setDetectMax(detectMax);

                            ConfigTarget configTarget = getConfigTarget(rangConfig.getChannelNo());
                            configTarget.setDetectMax(detectMax);
                            detectRangeAdapter.setRangeList(mDetectRanges);
                            if(null != mAmpAlgorithmAdapter && null != mMeltParamterAdapter){
                                mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                                mMeltParamterAdapter.setMeltParamter(mParamTargets);
                            }
                        }

                    }else if(dialogType == 15){
                        //熔解扫描间隔
                        ((TextView)view).setText(s);
                        double interval = (double)(StringUtil.convertToFloat(s,0.1f));
                        if(interval >0.01f){
                            for(ConfigSLANSection section : mSLANSections){
                                if(section.getProgramType()== Constants.PROGRAM_TYPE_MELT){
                                    section.setInterval(interval);
                                    section.setRate(interval);
                                }
                            }
                            updateUI();
                        }else {
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_scan_interval),Toast.LENGTH_LONG).show();
                            return;
                        }

                    }else if(dialogType == 16){
                        //熔解保持时间
                        int holdtime = (int)(StringUtil.convertToInt(s,10));
                        if(holdtime<1){
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_holdtime_min),Toast.LENGTH_LONG).show();
                            return;
                        }else if(holdtime >99){
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_holdtime_max),Toast.LENGTH_LONG).show();
                            return;
                        }
                        ((TextView)view).setText(String.valueOf(holdtime));
                        for(ConfigSLANSection section : mSLANSections){
                            if(section.getProgramType()== Constants.PROGRAM_TYPE_MELT){
                                section.setMeltHoldtime(holdtime);
                            }
                        }
                        updateUI();
                    }
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.show();
        }
    }
    //
    private ArrayList<Project> mConfigProjects = new ArrayList<Project>();

    //打开检测项
    private  void openSLANExTemplate(String fileName){
        //打开文件夹
        if(null == PcrApplication.slanService){
            Toast.makeText(getActivity().getApplicationContext(),
                    "slanService为空不能打开文件",Toast.LENGTH_LONG).show();
            return;
        }
        Log.i("AAA","openSLANExTemplate name" +fileName);
        mConfigProjects.clear();
        mConfigProjects = PcrApplication.slanService.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
        if(null != mConfigProjects && mConfigProjects.size()>0){
            for(Project project :mConfigProjects){
                if(null != project){
                    if(project.getName().equals(fileName)){
                        Program program = project.getProgram();
                        //转化为String
                        this.project = project;
                        ArrayList<Section> sections = program.getSections();
                        if(null != sections){
                            getSLANSections(sections);
                        }
                        //更新其他的基本信息
                    }
                }
            }

            updateBasicInfo();
            updateUI();

            isCreateProgram = true;
        }
    }
    private void initBasicInfo(){
        mFamCk.setChecked(false);
        mHexCk.setChecked(false);
        mRoxCk.setChecked(false);
        mCY5Ck.setChecked(false);
        mQuaSarCk.setChecked(false);
        mAttoCk.setChecked(false);
    }


    private void updateBasicInfo(){
        if(null != project){
            mProgramNameTv.setText(fileName);
            type = project.getType();
            if(project.getType() == Constants.PROGRAM_TYPE_MELT){
                mProgramTypeTv.setText(getActivity().getResources().getString(R.string.config_info_melt));
                mTitleMeltParamTv.setVisibility(View.VISIBLE);
                mMeltParamLayout.setVisibility(View.GONE);
                mAMPParamLayout.setVisibility(View.VISIBLE);
            }else if(project.getType() == Constants.PROGRAM_TYPE_AMP){
                mProgramTypeTv.setText(getActivity().getResources().getString(R.string.config_info_aq));
                mMeltParamLayout.setVisibility(View.GONE);
                mTitleMeltParamTv.setVisibility(View.GONE);
                mAMPParamLayout.setVisibility(View.VISIBLE);
                ampAmpAlgorithm = project.getAMPAlgorithm();
                if(ampAmpAlgorithm == 0){
                    mAmpAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_default));
                }else if(ampAmpAlgorithm == 1){
                    mAmpAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_subtraction));
                }else if(ampAmpAlgorithm == 2){
                    mAmpAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_relativeLinear));
                }
            }

            mReactionTv.setText(project.getReaction()+"");

            initBasicInfo();
            mParamTargets.clear();
            ArrayList<Target> targets = project.getTargets();
            if(targets != null && targets.size()>0){
                ///Log.i("AAA","targets" + targets.size());
                for(Target target : targets){
                    if(target.getName() != null){
                        if(target.getChannelNo()==1){
                            isFamThresholdCheck = true;
                            TargetParamter famParamer = target.getParamter();
                            ConfigTarget famTarget = new ConfigTarget();
                            famTarget.setTargetIndex(1);
                            famTarget.setName("FAM");
                            famTarget.setChannelNo(1);
                            famTarget.setColor("#FF0000FF");
                            //扩增曲线参数
                            famTarget.setQualitativeAnalysis(famParamer.getQualitativeAnalysis());
                            int famBeginbase = famParamer.getBeginBaseline();
                            if(famBeginbase == 0){
                                famBeginbase = 1;
                            }
                            famTarget.setBeginBaseline(famBeginbase);
                            famTarget.setEndBaseline(famParamer.getEndBaseline());
                            famTarget.setDigitalFilter(famParamer.getDigitalFilter());
                            famTarget.setThreshold(famParamer.getThreshold());
                            famTarget.setAutoThreshold(famParamer.getAutoThreshold());
                            famTarget.setOptimizationMode(famParamer.getOptimizationMode());
                            famTarget.setDetectMin(famParamer.getDetectionMin());
                            famTarget.setDetectMax(famParamer.getDetectionMax());
                            if(famParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) famParamer;
                                //famAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();
                                famTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                famTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                famTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                famTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                famTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            mParamTargets.add(famTarget);
                            mFamCk.setChecked(true);
                        }else if(target.getChannelNo()==2){
                            isHexThresholdCheck = true;

                            TargetParamter hexParamer = target.getParamter();
                            ConfigTarget hexTarget = new ConfigTarget();
                            hexTarget.setTargetIndex(2);
                            hexTarget.setName("HEX");
                            hexTarget.setChannelNo(2);
                            hexTarget.setColor("#FF00FF00");
                            hexTarget.setQualitativeAnalysis(hexParamer.getQualitativeAnalysis());//定性还是定量
                            int hexBeginbase = hexParamer.getBeginBaseline();
                            if(hexBeginbase == 0){
                                hexBeginbase = 1;
                            }
                            hexTarget.setBeginBaseline(hexBeginbase);
                            hexTarget.setEndBaseline(hexParamer.getEndBaseline());
                            hexTarget.setDigitalFilter(hexParamer.getDigitalFilter());
                            hexTarget.setAutoThreshold(hexParamer.getAutoThreshold());
                            hexTarget.setThreshold(hexParamer.getThreshold());
                            hexTarget.setAutoThreshold(hexParamer.getAutoThreshold());
                            hexTarget.setOptimizationMode(hexParamer.getOptimizationMode());
                            hexTarget.setDetectMin(hexParamer.getDetectionMin());
                            hexTarget.setDetectMax(hexParamer.getDetectionMax());
                            if(hexParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) hexParamer;
                                hexTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                hexTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                hexTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                hexTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                hexTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            mParamTargets.add(hexTarget);
                            mHexCk.setChecked(true);
                        }else if(target.getChannelNo()==3){
                            isRoxThresholdCheck = true;

                            TargetParamter roxParamer = target.getParamter();
                            ConfigTarget roxTarget = new ConfigTarget();
                            roxTarget.setTargetIndex(3);
                            roxTarget.setName("ROX");
                            roxTarget.setChannelNo(3);
                            roxTarget.setColor("#FFFF8000");
                            roxTarget.setQualitativeAnalysis(roxParamer.getQualitativeAnalysis());//定性还是定量
                            int roxBeginbase = roxParamer.getBeginBaseline();
                            if(roxBeginbase == 0){
                                roxBeginbase =1 ;
                            }
                            roxTarget.setBeginBaseline(roxBeginbase);
                            roxTarget.setEndBaseline(roxParamer.getEndBaseline());
                            roxTarget.setDigitalFilter(roxParamer.getDigitalFilter());
                            roxTarget.setThreshold(roxParamer.getThreshold());
                            roxTarget.setAutoThreshold(roxParamer.getAutoThreshold());
                            roxTarget.setOptimizationMode(roxParamer.getOptimizationMode());
                            roxTarget.setDetectMin(roxParamer.getDetectionMin());
                            roxTarget.setDetectMax(roxParamer.getDetectionMax());
                            if(roxParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) roxParamer;
                                //roxAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();

                                roxTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                roxTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                roxTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                roxTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                roxTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            mParamTargets.add(roxTarget);
                            mRoxCk.setChecked(true);
                        }else if(target.getChannelNo()==4){
                            isCY5ThresholdCheck = true;
                            TargetParamter cy5Paramer = target.getParamter();
                            ConfigTarget cy5Target = new ConfigTarget();
                            cy5Target.setTargetIndex(4);
                            cy5Target.setName("CY5");
                            cy5Target.setChannelNo(4);
                            cy5Target.setColor("#FFFF0000");
                            cy5Target.setQualitativeAnalysis(cy5Paramer.getQualitativeAnalysis());//定性还是定量
                            int cy5BeginBase = cy5Paramer.getBeginBaseline();
                            if(cy5BeginBase == 0){
                                cy5BeginBase =1 ;
                            }
                            cy5Target.setBeginBaseline(cy5BeginBase);
                            cy5Target.setEndBaseline(cy5Paramer.getEndBaseline());
                            cy5Target.setDigitalFilter(cy5Paramer.getDigitalFilter());
                            cy5Target.setThreshold(cy5Paramer.getThreshold());
                            cy5Target.setAutoThreshold(cy5Paramer.getAutoThreshold());
                            cy5Target.setOptimizationMode(cy5Paramer.getOptimizationMode());
                            cy5Target.setDetectMin(cy5Paramer.getDetectionMin());
                            cy5Target.setDetectMax(cy5Paramer.getDetectionMax());
                            if(cy5Paramer instanceof TargetParamterMelt){

                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) cy5Paramer;
                              //  cy5Algorithm = meltTargetParamer.getMeltPeakAlgorithm();
                                cy5Target.setMinTemperature(meltTargetParamer.getMinTemperature());
                                cy5Target.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                cy5Target.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                cy5Target.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                cy5Target.setPeekType(meltTargetParamer.getPeekType());
                            }
                            mParamTargets.add(cy5Target);
                            mCY5Ck.setChecked(true);
                        }else if(target.getChannelNo()==5){
                            isQuaSarThresholdCheck = true;

                            TargetParamter quasarParamer = target.getParamter();
                            ConfigTarget quasarTarget = new ConfigTarget();
                            quasarTarget.setTargetIndex(5);
                            quasarTarget.setName("QUASAR 705");
                            quasarTarget.setChannelNo(5);
                            quasarTarget.setColor("#FF8000FF");
                            quasarTarget.setQualitativeAnalysis(quasarParamer.getQualitativeAnalysis());//定性还是定量
                            int quaSarBeginBase = quasarParamer.getBeginBaseline();
                            if(quaSarBeginBase == 0){
                                quaSarBeginBase =1 ;
                            }
                            quasarTarget.setBeginBaseline(quaSarBeginBase);
                            quasarTarget.setEndBaseline(quasarParamer.getEndBaseline());
                            quasarTarget.setDigitalFilter(quasarParamer.getDigitalFilter());
                            quasarTarget.setThreshold(quasarParamer.getThreshold());
                            quasarTarget.setAutoThreshold(quasarParamer.getAutoThreshold());
                            quasarTarget.setOptimizationMode(quasarParamer.getOptimizationMode());
                            quasarTarget.setDetectMin(quasarParamer.getDetectionMin());
                            quasarTarget.setDetectMax(quasarParamer.getDetectionMax());
                            if(quasarParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) quasarParamer;
                                //quaSarAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();
                                quasarTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                quasarTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                quasarTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                quasarTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                quasarTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            mParamTargets.add(quasarTarget);
                            mQuaSarCk.setChecked(true);
                        }else if(target.getChannelNo()==6){
                            isAttoThresholdCheck = true;
                            TargetParamter attoParamer = target.getParamter();
                            ConfigTarget attoTarget = new ConfigTarget();
                            attoTarget.setTargetIndex(6);
                            attoTarget.setName("ATTO 425");
                            attoTarget.setChannelNo(6);
                            attoTarget.setColor("#FF800000");
                            attoTarget.setQualitativeAnalysis(attoParamer.getQualitativeAnalysis());//定性还是定量
                            int attoBeginBase = attoParamer.getBeginBaseline();
                            if(attoBeginBase == 0){
                                attoBeginBase =1 ;
                            }

                            attoTarget.setBeginBaseline(attoBeginBase);
                            attoTarget.setEndBaseline(attoParamer.getEndBaseline());
                            attoTarget.setDigitalFilter(attoParamer.getDigitalFilter());
                            attoTarget.setThreshold(attoParamer.getThreshold());
                            attoTarget.setAutoThreshold(attoParamer.getAutoThreshold());
                            attoTarget.setOptimizationMode(attoParamer.getOptimizationMode());
                            attoTarget.setDetectMin(attoParamer.getDetectionMin());
                            attoTarget.setDetectMax(attoParamer.getDetectionMax());
                            if(attoParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) attoParamer;
                                //attoAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();
                                attoTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                attoTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                attoTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                attoTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                attoTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            mParamTargets.add(attoTarget);
                            mAttoCk.setChecked(true);
                        }
                    }
                }
            }
            if(mParamTargets != null){
                mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                mMeltParamterAdapter.setMeltParamter(mParamTargets);//熔解曲线参数
                mDetectRanges.clear();
                if(mParamTargets.size()>0){
                    for(ConfigTarget configTarget :mParamTargets){
                            if(!configTarget.isQualitativeAnalysis()){
                                mDetectRanges.add(configTarget);
                            }
                        }
                        Collections.sort(mDetectRanges);
                    }
                if(null != detectRangeAdapter){
                    detectRangeAdapter.setRangeList(mDetectRanges);
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
                        ConfigSLANSection.Step slanStep = new ConfigSLANSection.Step();
                        slanStep.setStepName("");
                        slanStep.setLeftTemperature(sectionMelt.getStartTemp());
                        slanStep.setRightTemperature(sectionMelt.getEndTemp());
                        slanStep.setSectionIndex(sectionMelt.getIdx());
                        slanStep.setStep_index(0);
                        slanStep.setTemperature(sectionMelt.getEndTemp());
                        slanStep.setHoldTime(sectionMelt.getHoldTime());
                        slanStep.setStepType(Constants.PROGRAM_TYPE_MELT);
                        slanItems.add(slanStep);
                        slanSection.setItems(slanItems);
                        mSLANSections.add(slanSection);
                    }
                }

            }
        }
    }

    /**
     *
     * @param message
     * @param dialogType
     * 0：切换标准熔解/定性定量
     * 1：保存时有相同的文件名
     * @param type_position
     */
    private void showNoticeDialog(String message,int dialogType,int type_position){
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog =null;
        }

        if(null == noticeDialog){
            noticeDialog =new NoticeDialog(this.getActivity(),R.style.Dialog);
            noticeDialog.setMessage(message);
            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    noticeDialog.dismiss();
                   //重新编辑
                    if(dialogType == 0){
                        type = type_position;
                        collapse();
                        mProgramTypeTv.setText(mTypeList.get(type_position));
                        initThreshold(type);
                        isCreateProgram = false;
                        isEdit = false;
                    }else if(dialogType == 1){
                        String name = mProgramNameTv.getText().toString().trim();
                        //弹出对话框提示
                        String filePath = FileUtils.CONFIG_SLAN_PATH + File.separator + name +".prj";
                        File file = new File(filePath);
                        if(file != null &&file.isFile()) {
                            boolean del_result = file.delete();
                            Log.i("AAA", "项目已存在删除" + del_result);
                            if (del_result) {
                                saveExperimentTemplate(name);
                            }
                        }
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

    private boolean isHasSameSlanFile(String fileName){
        if(null == PcrApplication.slanService){
            return false;
        }
        List<Project> projects = PcrApplication.slanService.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
        if(null != projects && projects.size()>0) {
            for (Project project : projects) {
                if (null != project) {
                    if (project.getName().equals(fileName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    ///////////////////////////////////////////下拉框////////////////////////////////////////////////////
    /**
     * 添加程序段的提示window
     */
    private void initAddSectionsWindow(){
        mAddSections = new ArrayList<String>();
        mAddSections.add(getActivity().getResources().getString(R.string.config_info_position_after_section));
        mAddSections.add(getActivity().getResources().getString(R.string.config_info_position_before_section));
        mAddSections.add(getActivity().getResources().getString(R.string.config_info_position_last_section));
        popupView= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter addSectionAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mAddSections);
        ListView mAddSectionLv= (ListView) popupView.findViewById(R.id.spinner_listview);

        mAddSectionLv.setAdapter(addSectionAdapter);
        mAddSectionWindow =new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAddSectionWindow.setOutsideTouchable(true);
        mAddSectionWindow.setFocusable(true);
        mAddSectionWindow.setContentView(popupView);
        mAddSectionWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mAddSectionWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseAddSection();
            }
        });

        addSectionAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                switch (position){
                    case 0:
                        addSLANSectionAtAfter();
                        break;
                    case 1:
                        addSlanSectionAtBefore();
                        break;
                    case 2:
                        addSLANSectionAtLast();
                        break;
                }
               collapseAddSection();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private void collapseAddSection(){
        if (mAddSectionWindow != null && mAddSectionWindow.isShowing()) {
            mAddSectionWindow.dismiss();
        }
    }

    //展开扩增曲线算法
    private void expandAddSection(View view){
        if(mAddSections== null && mAddSections.isEmpty()){
            return;
        }
        mAddSectionWindow.showAsDropDown(view);
    }


    private void initAddStepsWindow(){
        mAddSteps = new ArrayList<String>();
        mAddSteps.add(getActivity().getResources().getString(R.string.config_info_position_after_step));
        mAddSteps.add(getActivity().getResources().getString(R.string.config_info_position_before_step));
        mAddSteps.add(getActivity().getResources().getString(R.string.config_info_position_last_step));
        popupView= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter addStepAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mAddSteps);
        ListView mAddStepLv= (ListView) popupView.findViewById(R.id.spinner_listview);

        mAddStepLv.setAdapter(addStepAdapter);
        mAddStepsWindow =new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAddStepsWindow.setOutsideTouchable(true);
        mAddStepsWindow.setFocusable(true);
        mAddStepsWindow.setContentView(popupView);
        mAddStepsWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mAddStepsWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseAddStep();
            }
        });

        addStepAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                switch (position){
                    case 0:
                        addSLANStepAtAfter();
                        break;
                    case 1:
                        addSLANStepAtBefore();
                        break;
                    case 2:
                        addSLANStepAtLast();
                        break;
                }
                collapseAddStep();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private void collapseAddStep(){
        if (mAddStepsWindow != null && mAddStepsWindow.isShowing()) {
            mAddStepsWindow.dismiss();
        }
    }

    private void expandAddStep(View view){
        if(mAddSteps== null && mAddSteps.isEmpty()){
            return;
        }
        mAddStepsWindow.showAsDropDown(view);
    }
    /**
     * 基本参数-定性/定量
     */
    private boolean isAysTypeExpanded = false;//
    private void collapseQualitativeAnalysis(){
        if (mAysTypePopWindow != null && mAysTypePopWindow.isShowing()) {
            mAysTypePopWindow.dismiss();
        }
        isAysTypeExpanded = false;
    }

    //展开扩增曲线算法
    private void expandQualitativeAnalysis(View view){
        if(mAmpAysTypes== null && mAmpAysTypes.isEmpty()){
            return;
        }
        mAysTypePopWindow.showAsDropDown(view);
        isAysTypeExpanded = true;
    }


    private void initAmpAysTypeWindow(){
        mAmpAysTypes = new ArrayList<String>();
        mAmpAysTypes.add(getActivity().getResources().getString(R.string.config_info_ampays_qualitative));
        mAmpAysTypes.add(getActivity().getResources().getString(R.string.config_info_ampays_quantitative));
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter mAmpAysTypeAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mAmpAysTypes);
        ListView mAmpAysTypeLv= (ListView) view.findViewById(R.id.spinner_listview);
        mAmpAysTypeLv.setAdapter(mAmpAysTypeAdapter);
        mAysTypePopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAysTypePopWindow.setOutsideTouchable(true);
        mAysTypePopWindow.setFocusable(true);
        mAysTypePopWindow.setContentView(view);
        mAysTypePopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mAysTypePopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseQualitativeAnalysis();
            }
        });

        mAmpAysTypeAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if(position == 0){
                    ConfigTarget configTarget = mParamTargets.get(mAmpAlogrithm_pos);
                    configTarget.setQualitativeAnalysis(true);//定性
                    configTarget.setDetectMin((double)0);
                    configTarget.setDetectMax((double)10000000000L);
                }else{
                    ConfigTarget configTarget = mParamTargets.get(mAmpAlogrithm_pos);
                    configTarget.setQualitativeAnalysis(false);//定量
                }
                if(null != detectRangeAdapter && null != mDetectRanges){
                    mDetectRanges.clear();
                    for(ConfigTarget configTarget :mParamTargets){
                        if(!configTarget.isQualitativeAnalysis()){
                            mDetectRanges.add(configTarget);
                        }
                    }

                    detectRangeAdapter.setRangeList(mDetectRanges);
                }

                mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                collapseQualitativeAnalysis();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    /**
     * 基本参数-基线优化下拉框
     */

    private void collapseOptimizationMode(){
        if (mOptimizationModeWindow != null && mOptimizationModeWindow.isShowing()) {
            mOptimizationModeWindow.dismiss();
        }
    }

    //展开基线优化
    private void expandOptimizationMode(View view){
        if(mOptimizationModes== null && mOptimizationModes.isEmpty()){
            return;
        }
        mOptimizationModeWindow.showAsDropDown(view);
    }

    private void initOptimizationModeWindow(){
        mOptimizationModes = new ArrayList<String>();
        mOptimizationModes.add(getActivity().getResources().getString(R.string.config_info_optimizationmode_na));
        mOptimizationModes.add(getActivity().getResources().getString(R.string.config_info_optimizationmode_auto));
        mOptimizationModes.add(getActivity().getResources().getString(R.string.config_info_optimizationmode_manual));
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter optimizationModeAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mOptimizationModes);
        ListView mOptimizationModeLv= (ListView) view.findViewById(R.id.spinner_listview);
        mOptimizationModeLv.setAdapter(optimizationModeAdapter);
        mOptimizationModeWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mOptimizationModeWindow.setOutsideTouchable(true);
        mOptimizationModeWindow.setFocusable(true);
        mOptimizationModeWindow.setContentView(view);
        mOptimizationModeWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mOptimizationModeWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseOptimizationMode();
            }
        });

        optimizationModeAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget configTarget = mParamTargets.get(mAmpAlogrithm_pos);
                configTarget.setOptimizationMode(position);

                mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                collapseOptimizationMode();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    /**
     * 扩增曲线算法-数字滤波
     */
    private void initAmpDigitalFilter(){
        String[] digitalFilters= getResources().getStringArray(R.array.AMPDigitalFiler);
        mDighitalFilters = new ArrayList<String>(digitalFilters.length);
        Collections.addAll(mDighitalFilters, digitalFilters);
        popupView= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter digitalFilterAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mDighitalFilters);
        ListView mDigitalFilterLv= (ListView) popupView.findViewById(R.id.spinner_listview);
        mDigitalFilterLv.setAdapter(digitalFilterAdapter);
        mDigitalFilterWindow =new PopupWindow(popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mDigitalFilterWindow.setOutsideTouchable(true);
        mDigitalFilterWindow.setFocusable(true);
        mDigitalFilterWindow.setContentView(popupView);
        mDigitalFilterWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mDigitalFilterWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseDigitalFilter();
            }
        });

        digitalFilterAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget configTarget = mParamTargets.get(mAmpAlogrithm_pos);
                configTarget.setDigitalFilter(position);
                mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                collapseDigitalFilter();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    //扩增数字滤波
    private void collapseDigitalFilter(){
        if (mDigitalFilterWindow != null && mDigitalFilterWindow.isShowing()) {
            mDigitalFilterWindow.dismiss();
        }
    }

    //展开数字滤波
    private void expandDigitalFilter(View view){
        if(mDighitalFilters== null && mDighitalFilters.isEmpty()){
            return;
        }
        mDigitalFilterWindow.showAsDropDown(view);
        /*popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight =  popupView.getMeasuredHeight();
        int[] location = new int[2];
        // 获得位置 这里的v是目标控件，就是你要放在这个v的上面还是下面
        view.getLocationOnScreen(location);
        mDigitalFilterWindow.showAtLocation(view, Gravity.NO_GRAVITY, (location[0]),
                location[1]-3*popupHeight);
        isDigitalFilterExpanded = true;*/
    }

    /**
     * 熔解曲线参数-熔解峰型
     */
    private boolean isPeekTypeExpanded = false;//
    //扩增曲线算法
    private void collapsePeekType(){
        if (mPeekTypeWindow != null && mPeekTypeWindow.isShowing()) {
            mPeekTypeWindow.dismiss();
        }
        isPeekTypeExpanded = false;
    }

    //展开扩增曲线算法
    private void expandPeekType(View view){
        if(mPeekTypes== null && mPeekTypes.isEmpty()){
            return;
        }
        mPeekTypeWindow.showAsDropDown(view);
        isPeekTypeExpanded = true;
    }

    private void initPeekTypeWindow(){
        mPeekTypes = new ArrayList<String>();
        mPeekTypes.add(getActivity().getResources().getString(R.string.config_info_peak_type_positive));
        mPeekTypes.add(getActivity().getResources().getString(R.string.config_info_peak_type_negative));
        mPeekTypes.add(getActivity().getResources().getString(R.string.config_info_peak_type_positive_negative));
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter peekTypeAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mPeekTypes);
        ListView mPeekTypeLv= (ListView) view.findViewById(R.id.spinner_listview);
        mPeekTypeLv.setAdapter(peekTypeAdapter);
        mPeekTypeWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mPeekTypeWindow.setOutsideTouchable(true);
        mPeekTypeWindow.setFocusable(true);
        mPeekTypeWindow.setContentView(view);
        mPeekTypeWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mPeekTypeWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapsePeekType();
            }
        });

        peekTypeAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ConfigTarget configTarget = mParamTargets.get(meltParamter_pos);
                configTarget.setPeekType(position);
                mMeltParamterAdapter.setMeltParamter(mParamTargets);
                collapsePeekType();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }


}
