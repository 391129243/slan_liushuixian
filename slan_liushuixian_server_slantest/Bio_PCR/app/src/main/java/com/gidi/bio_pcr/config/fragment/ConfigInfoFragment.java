package com.gidi.bio_pcr.config.fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.universalUtils.TimerCaculate;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.ConfigSLANAdapter;
import com.gidi.bio_pcr.config.ConfigSLANSection;

import com.gidi.bio_pcr.config.adapter.FileNameAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomTitleText;
import com.gidi.bio_pcr.view.DividerDecoration;
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
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.PeekType;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 展示所有的项目的信息的
 */
public class ConfigInfoFragment extends BaseFragment implements View.OnClickListener{

    private DecimalFormat nf = new DecimalFormat( "0.00");
    private RecyclerView programRv;//配置的所有的项目的列表
    private RecyclerView projectRv;
    private TextView mDeleteTv,mCreateTv,mTestRunTv,mEditTv;
    private LinearLayout mProgramTitleLayout;
    private LinearLayout mProgramRecycleLayout;
    private List<String> mSlanFileNames;
    private List<ConfigSLANSection> mSLANSections;
    private ConfigSLANAdapter configSLANAdapter;
    private FileNameAdapter slanFileAdapter;
    private String fileName;
    private Project project = null;
    private int click_position = -1;

    private CustomTitleText mProgramNameTv,mProgramTypeTv,mReactionTv,mUnitTv,mTimeTv;
    private TextView mTitleAmpParamTv,mTitleMeltParamTv;
    private double famPeekHightThd,hexPeekHightThd,roxPeekHightThd,cy5PeekHightThd,quasarPeekHightThd,attoPeekHightThd;

    private CheckBox mFamCk,mHexCk,mRoxCk,mCY5Ck,mQuasarCk,mAttoCk;
    private TextView mProgramTabTv,mParamTabTv;
    private RelativeLayout mParamLayout;
    private HorizontalScrollView mProgramLayout ;
    private NoticeDialog noticeDialog;

    //熔解曲线参数
    private LinearLayout mMeltParamLayout;

    //扩增曲线参数
    private LinearLayout mAMPParamLayout;//扩增曲线参数
    private ListView mAmpAlogrithmLv;
    private AmpAlgorithmAdapter ampAlgorithmAdapter;
    private List<ConfigTarget> configTargets;
    private TextView mAmpAlgorithmTv;//扩增曲线类型

    //熔解曲线参数
    private ListView mMeltParamterLv;
    private MeltParamterAdapter meltParamterAdapter;


    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_config_info;
    }

    @Override
    protected void initView(View rootView) {

        mProgramTitleLayout = (LinearLayout)rootView.findViewById(R.id.config_info_slan_program_title_layout);
        mProgramRecycleLayout = (LinearLayout)rootView.findViewById(R.id.config_info_slan_program_recycle_layout);
        programRv = (RecyclerView)rootView.findViewById(R.id.config_info_slan_program_rv);
        mSLANSections = new ArrayList<ConfigSLANSection>();
        programRv.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        programRv.addItemDecoration(new DividerDecoration(getActivity(), 2));
        configSLANAdapter = new ConfigSLANAdapter(getActivity().getApplicationContext());
        mSLANSections.clear();
        configSLANAdapter.setProgramSteps(mSLANSections);
        programRv.setAdapter(configSLANAdapter);
        initConfigRv();
        mDeleteTv = (TextView)rootView.findViewById(R.id.config_info_delete_tv);
        mCreateTv = (TextView)rootView.findViewById(R.id.config_info_create_tv);
        mTestRunTv = (TextView)rootView.findViewById(R.id.config_info_test_tv);
        mEditTv = (TextView)rootView.findViewById(R.id.config_info_edit_tv);

        mProgramNameTv = (CustomTitleText) rootView.findViewById(R.id.config_info_project_name_tv);
        mProgramTypeTv = (CustomTitleText) rootView.findViewById(R.id.config_info_project_type_tv);
        mReactionTv= (CustomTitleText) rootView.findViewById(R.id.config_info_project_reaction_tv);
        mUnitTv = (CustomTitleText) rootView.findViewById(R.id.config_info_project_unit_tv);
        mTimeTv = (CustomTitleText) rootView.findViewById(R.id.config_info_project_time_tv);

        mProgramTabTv = (TextView)rootView.findViewById(R.id.config_info_program_tab_tv) ;
        mParamTabTv =  (TextView)rootView.findViewById(R.id.config_info_param_tab_tv) ;
        mProgramLayout = (HorizontalScrollView) rootView.findViewById(R.id.config_info_slan_step_layout);
        mParamLayout = (RelativeLayout) rootView.findViewById(R.id.config_info_param_layout);


        mFamCk = (CheckBox)rootView.findViewById(R.id.config_info_fam_ck);
        mHexCk = (CheckBox)rootView.findViewById(R.id.config_info_hex_ck);
        mRoxCk = (CheckBox)rootView.findViewById(R.id.config_info_rox_ck);
        mCY5Ck = (CheckBox)rootView.findViewById(R.id.config_info_cy5_ck);
        mQuasarCk = (CheckBox)rootView.findViewById(R.id.config_info_quasar_ck);
        mAttoCk = (CheckBox)rootView.findViewById(R.id.config_info_atto_ck);
        mTitleAmpParamTv = (TextView)rootView.findViewById(R.id.config_info_param_setting_title_basic_tv);
        mTitleMeltParamTv = (TextView)rootView.findViewById(R.id.config_info_param_setting_title_melt_tv);

        mMeltParamLayout = (LinearLayout) rootView.findViewById(R.id.config_info_melt_param_layout);
        //扩增曲线参数
        configTargets = new ArrayList<ConfigTarget>();
        mAMPParamLayout = (LinearLayout) rootView.findViewById(R.id.config_info_amp_param_layout);
        mAmpAlgorithmTv = (TextView)rootView.findViewById(R.id.config_info_amp_algorithm_tv);
        mAmpAlogrithmLv = (ListView) rootView.findViewById(R.id.config_info_amp_algorithm_lv);
        ampAlgorithmAdapter = new AmpAlgorithmAdapter(getActivity().getApplicationContext());
        mAmpAlogrithmLv.setAdapter(ampAlgorithmAdapter);
        //熔解曲线参数
        mMeltParamterLv = (ListView) rootView.findViewById(R.id.config_info_melt_paramter_lv);
        meltParamterAdapter = new MeltParamterAdapter(getActivity().getApplicationContext());
        mMeltParamterLv.setAdapter(meltParamterAdapter);

    }

    @Override
    protected void initListener() {
        mDeleteTv.setOnClickListener(this);
        mCreateTv.setOnClickListener(this);
        mTestRunTv.setOnClickListener(this);
        mProgramTabTv.setOnClickListener(this);
        mParamTabTv.setOnClickListener(this);
        mEditTv.setOnClickListener(this);
        mTitleAmpParamTv.setOnClickListener(this);
        mTitleMeltParamTv.setOnClickListener(this);
        mProgramTabTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
        mParamTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            //重新显示
            mProgramLayout.setVisibility(View.VISIBLE);
            mParamLayout.setVisibility(View.GONE);
            mProgramTabTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mParamTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mTitleAmpParamTv.setTextColor(getActivity().getResources().getColor(R.color.white));
            mTitleAmpParamTv.setBackgroundResource(R.drawable.shape_rect_blue);
            initOpenProgram();
            //显示第一个人
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(noticeDialog != null){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
        if(null != configTargets){
            configTargets.clear();
            configTargets = null;
        }
    }

    private void initConfigRv(){
        mSlanFileNames = new ArrayList<String>();

        //编辑的prj文件夹是config，找到所有的文件
        projectRv = (RecyclerView)rootView.findViewById(R.id.config_info_project_recycleview);
        LinearLayoutManager layoutManager = new GridLayoutManager(this.getActivity().getApplicationContext(),5);
        projectRv.setLayoutManager(layoutManager);
        projectRv.addItemDecoration(new LinearSpacingItemDecoration(getActivity(), 6));
        slanFileAdapter = new FileNameAdapter(getActivity().getApplicationContext());
        slanFileAdapter.setTemplateList(mSlanFileNames);
        projectRv.setAdapter(slanFileAdapter);
        slanFileAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                click_position = position;
                slanFileAdapter.setTemplateList(mSlanFileNames);
                slanFileAdapter.setSelectedPosition(click_position);
                fileName = mSlanFileNames.get(position);
                openSLANExFile(fileName);
            }
        });
    }


    public void setConfigProjectList(List<String> configNames){
        this.mSlanFileNames = configNames;
        if(null != slanFileAdapter){
            slanFileAdapter.setTemplateList(mSlanFileNames);
        }
        //initOpenProgram();
    }


    private void updateProgramUI(){

        if(null != mSLANSections){
           // Log.i("AAA","mSLANSections--"+ mSLANSections.size());
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
                    TextView textView = new TextView(getActivity().getApplicationContext());
                    textView.setBackgroundResource(R.color.blue);
                    textView.setTextSize(20);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(0xffffffff);

                    int programType = mSLANSections.get(i).getProgramType();
                    if(programType == Constants.PROGRAM_TYPE_AMP){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(162*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(1, 0, 0, 0);
                        textView.setLayoutParams(layoutParams);
                        int index = i+1;
                        textView.setText(getActivity().getResources().getString(R.string.runing_pcr_section)+ index);
                    }else if(programType == Constants.PROGRAM_TYPE_MELT){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(1, 0, 1, 0);
                        textView.setLayoutParams(layoutParams);
                        textView.setText(getActivity().getResources().getString(R.string.runing_pcr_section_melt));
                    }
                    mProgramTitleLayout.addView(textView);
                }

                configSLANAdapter.setProgramSteps(mSLANSections);

                if(null != mProgramRecycleLayout){
                    mProgramRecycleLayout.removeAllViews();
                }
                for(int i = 0; i<mSLANSections.size();i++){
                    TextView textView = new TextView(getActivity().getApplicationContext());
                    textView.setBackgroundResource(R.color.color_recycle);
                    textView.setTextSize(18);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(0xffffffff);

                    int programType = mSLANSections.get(i).getProgramType();
                    if(programType == Constants.PROGRAM_TYPE_AMP){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(162*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(1, 0, 0, 0);
                        textView.setLayoutParams(layoutParams);
                        textView.setText(getActivity().getResources().getString(R.string.running_pcr_recycle_num) + (mSLANSections.get(i).getCycles()));
                    }else if(programType == Constants.PROGRAM_TYPE_MELT){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(1, 0, 1, 0);
                        textView.setLayoutParams(layoutParams);
                        textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_scanmode_step) + " "
                                 + getActivity().getResources().getString(R.string.ruuning_pcr_interval) + nf.format(mSLANSections.get(i).getInterval())+"℃" + " "
                                 + getActivity().getResources().getString(R.string.ruuning_pcr_melt_holdtime) + mSLANSections.get(i).getMeltHoldtime()+"s");
                    }
                    mProgramRecycleLayout.addView(textView);
                }

            }
        }
    }

    /**
     * 初始化初始信息
     */
    private void initBasicInfo(){
        mFamCk.setChecked(false);
        mHexCk.setChecked(false);
        mRoxCk.setChecked(false);
        mCY5Ck.setChecked(false);
        mQuasarCk.setChecked(false);
        mAttoCk.setChecked(false);


    }

    /**
     * 更新基本信息
     */
    private void updateBasicInfo(){
        if(null != project){


            int time = TimerCaculate.PCR_Caculate(project);

            mTimeTv.setContentText(time + "s");

            int programType = project.getType();
            int ampAlgorithm = project.getAMPAlgorithm();
            mProgramNameTv.setContentText(fileName);
            mMeltParamLayout.setVisibility(View.GONE);
            mAMPParamLayout.setVisibility(View.VISIBLE);
            mTitleAmpParamTv.setTextColor(getActivity().getResources().getColor(R.color.white));
            mTitleAmpParamTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mTitleMeltParamTv.setTextColor(getActivity().getResources().getColor(R.color.black));
            mTitleMeltParamTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            if(programType == Constants.PROGRAM_TYPE_MELT){
                mProgramTypeTv.setContentText(getActivity().getResources().getString(R.string.config_info_melt));
                mTitleMeltParamTv.setVisibility(View.VISIBLE);

            }else if(programType == Constants.PROGRAM_TYPE_AMP){
                mProgramTypeTv.setContentText(getActivity().getResources().getString(R.string.config_info_aq));
                mTitleMeltParamTv.setVisibility(View.INVISIBLE);
            }
            if(ampAlgorithm == 0){
                mAmpAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_default));
            }else if(ampAlgorithm == 1){
                mAmpAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_subtraction));
            }else if(ampAlgorithm == 2){
                mAmpAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_relativeLinear));
            }

            mReactionTv.setContentText(project.getReaction()+" μl");
            mUnitTv.setContentText(project.getUnit());
            initBasicInfo();
            configTargets.clear();
            ArrayList<Target> targets = project.getTargets();
            if(targets != null && targets.size()>0){
                for(Target target : targets){
                    if(target.getName() != null){
                        if(target.getChannelNo() == 1){
                            mFamCk.setChecked(true);
                            ConfigTarget famTarget = new ConfigTarget();
                            famTarget.setTargetIndex(1);
                            famTarget.setName("FAM");
                            famTarget.setChannelNo(1);
                            //扩增曲线参数
                            TargetParamter famParamer = target.getParamter();
                            famTarget.setBeginBaseline(famParamer.getBeginBaseline());
                            famTarget.setQualitativeAnalysis(famParamer.getQualitativeAnalysis());
                            famTarget.setEndBaseline(famParamer.getEndBaseline());
                            famTarget.setDigitalFilter(famParamer.getDigitalFilter());
                            famTarget.setThreshold(famParamer.getThreshold());
                            famTarget.setAutoThreshold(famParamer.getAutoThreshold());
                            famTarget.setOptimizationMode(famParamer.getOptimizationMode());
                            famTarget.setDetectMin(famParamer.getDetectionMin());
                            famTarget.setDetectMax(famParamer.getDetectionMax());
                            //熔解参数
                            if(famParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) famParamer;
                                famPeekHightThd = meltTargetParamer.getPeekHightThreshold();
                                int MeltPeakAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();
                                famTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm);
                                famTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                famTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                famTarget.setThreshold(meltTargetParamer.getThreshold());
                                famTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                            }
                            configTargets.add(famTarget);

                        }else if(target.getChannelNo() == 2){
                            mHexCk.setChecked(true);
                            ConfigTarget hexTarget = new ConfigTarget();
                            hexTarget.setTargetIndex(2);
                            hexTarget.setName("HEX");
                            hexTarget.setChannelNo(2);
                            TargetParamter hexParamer = target.getParamter();
                            hexParamer.setQualitativeAnalysis(hexParamer.getQualitativeAnalysis());
                            hexTarget.setBeginBaseline(hexParamer.getBeginBaseline());
                            hexTarget.setEndBaseline(hexParamer.getEndBaseline());
                            hexTarget.setDigitalFilter(hexParamer.getDigitalFilter());
                            hexTarget.setThreshold(hexParamer.getThreshold());
                            hexTarget.setAutoThreshold(hexParamer.getAutoThreshold());
                            hexTarget.setOptimizationMode(hexParamer.getOptimizationMode());
                            hexTarget.setDetectMin(hexParamer.getDetectionMin());
                            hexTarget.setDetectMax(hexParamer.getDetectionMax());
                            //熔解参数
                            if(hexParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) hexParamer;
                                hexPeekHightThd = meltTargetParamer.getPeekHightThreshold();
                                hexTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                hexTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                hexTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                hexTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                hexTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            configTargets.add(hexTarget);

                        }else if(target.getChannelNo() == 3){
                            mRoxCk.setChecked(true);
                            ConfigTarget roxTarget = new ConfigTarget();
                            roxTarget.setTargetIndex(3);
                            roxTarget.setName("ROX");
                            roxTarget.setChannelNo(3);
                            TargetParamter roxParamer = target.getParamter();
                            roxTarget.setQualitativeAnalysis(roxParamer.getQualitativeAnalysis());
                            roxTarget.setBeginBaseline(roxParamer.getBeginBaseline());
                            roxTarget.setEndBaseline(roxParamer.getEndBaseline());
                            roxTarget.setDigitalFilter(roxParamer.getDigitalFilter());
                            roxTarget.setThreshold(roxParamer.getThreshold());
                            roxTarget.setAutoThreshold(roxParamer.getAutoThreshold());
                            roxTarget.setOptimizationMode(roxParamer.getOptimizationMode());
                            roxTarget.setDetectMin(roxParamer.getDetectionMin());
                            roxTarget.setDetectMax(roxParamer.getDetectionMax());
                            //熔解参数
                            if(roxParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) roxParamer;
                                roxPeekHightThd = meltTargetParamer.getPeekHightThreshold();
                                int MeltPeakAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();

                                roxTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm);
                                roxTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                roxTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                roxTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                roxTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            configTargets.add(roxTarget);

                        }else if(target.getChannelNo() == 4){
                            mCY5Ck.setChecked(true);
                            ConfigTarget cy5Target = new ConfigTarget();
                            cy5Target.setTargetIndex(4);
                            cy5Target.setName("CY5");
                            cy5Target.setChannelNo(4);
                            TargetParamter cy5Paramer = target.getParamter();
                            cy5Target.setQualitativeAnalysis(cy5Paramer.getQualitativeAnalysis());
                            cy5Target.setBeginBaseline(cy5Paramer.getBeginBaseline());
                            cy5Target.setEndBaseline(cy5Paramer.getEndBaseline());
                            cy5Target.setDigitalFilter(cy5Paramer.getDigitalFilter());
                            cy5Target.setThreshold(cy5Paramer.getThreshold());
                            cy5Target.setAutoThreshold(cy5Paramer.getAutoThreshold());
                            cy5Target.setOptimizationMode(cy5Paramer.getOptimizationMode());
                            cy5Target.setDetectMin(cy5Paramer.getDetectionMin());
                            cy5Target.setDetectMax(cy5Paramer.getDetectionMax());
                            if(cy5Paramer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) cy5Paramer;
                                cy5PeekHightThd = meltTargetParamer.getPeekHightThreshold();
                                int  MeltPeakAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();

                                cy5Target.setMinTemperature(meltTargetParamer.getMinTemperature());
                                cy5Target.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                cy5Target.setMeltPeakAlgorithm(MeltPeakAlgorithm);
                                cy5Target.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                cy5Target.setPeekType(meltTargetParamer.getPeekType());
                            }
                            configTargets.add(cy5Target);

                        }else if(target.getChannelNo() == 5){
                            mQuasarCk.setChecked(true);
                            ConfigTarget quasarTarget = new ConfigTarget();
                            quasarTarget.setTargetIndex(5);
                            quasarTarget.setName("QUASAR 705");
                            quasarTarget.setChannelNo(5);
                            TargetParamter quasarParamer = target.getParamter();
                            quasarTarget.setQualitativeAnalysis(quasarParamer.getQualitativeAnalysis());
                            quasarTarget.setBeginBaseline(quasarParamer.getBeginBaseline());
                            quasarTarget.setEndBaseline(quasarParamer.getEndBaseline());
                            quasarTarget.setDigitalFilter(quasarParamer.getDigitalFilter());
                            quasarTarget.setThreshold(quasarParamer.getThreshold());
                            quasarTarget.setAutoThreshold(quasarParamer.getAutoThreshold());
                            quasarTarget.setOptimizationMode(quasarParamer.getOptimizationMode());
                            quasarTarget.setDetectMin(quasarParamer.getDetectionMin());
                            quasarTarget.setDetectMax(quasarParamer.getDetectionMax());
                            if(quasarParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) quasarParamer;
                                quasarPeekHightThd = meltTargetParamer.getPeekHightThreshold();
                                int  MeltPeakAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();

                                quasarTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                quasarTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                quasarTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm);
                                quasarTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                quasarTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            configTargets.add(quasarTarget);

                        }else if(target.getChannelNo() == 6){
                            mAttoCk.setChecked(true);
                            ConfigTarget attoTarget = new ConfigTarget();
                            attoTarget.setTargetIndex(6);
                            attoTarget.setName("ATTO 425");
                            attoTarget.setChannelNo(6);
                            TargetParamter attoParamer = target.getParamter();
                            attoTarget.setQualitativeAnalysis(attoParamer.getQualitativeAnalysis());
                            attoTarget.setBeginBaseline(attoParamer.getBeginBaseline());
                            attoTarget.setEndBaseline(attoParamer.getEndBaseline());
                            attoTarget.setDigitalFilter(attoParamer.getDigitalFilter());
                            attoTarget.setThreshold(attoParamer.getThreshold());
                            attoTarget.setAutoThreshold(attoParamer.getAutoThreshold());
                            attoTarget.setDetectMin(attoParamer.getDetectionMin());
                            attoTarget.setDetectMax(attoParamer.getDetectionMax());
                            attoTarget.setOptimizationMode(attoParamer.getOptimizationMode());
                            if(attoParamer instanceof TargetParamterMelt){
                                TargetParamterMelt meltTargetParamer = (TargetParamterMelt) attoParamer;
                                attoPeekHightThd = meltTargetParamer.getPeekHightThreshold();
                                int  MeltPeakAlgorithm = meltTargetParamer.getMeltPeakAlgorithm();
                                attoTarget.setMeltPeakAlgorithm(MeltPeakAlgorithm);
                                attoTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                attoTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                attoTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                attoTarget.setPeekType(meltTargetParamer.getPeekType());
                            }
                            configTargets.add(attoTarget);
                        }
                    }
                }
            }

            if(configTargets != null){
                if(null != ampAlgorithmAdapter){
                    ampAlgorithmAdapter.setAmpAlgorithm(configTargets);
                }

                if(null != meltParamterAdapter){
                    meltParamterAdapter.setMeltParamter(configTargets);
                }
            }

        }

    }

    private  void openSLANExFile(String fileName){
        //打开文件夹
        if(null == PcrApplication.slanService){
            return;
        }
        if(StringUtil.isEmpty(fileName)){
            return;
        }
        ArrayList<Project> mConfigProjects = PcrApplication.slanService.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
        if(null != mConfigProjects && mConfigProjects.size()>0){
            for(Project project :mConfigProjects){
                if(null != project){
                    if(project.getName().equals(fileName)){

                          /*  Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                                @Override
                                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                                    if (src == src.longValue())
                                        return new JsonPrimitive(src.longValue());
                                    return new JsonPrimitive(src);
                                }
                            }).create();
                            String s = gson.toJson(project);
                            Log.i("AAA","S"+ s);*/
                        Program program = project.getProgram();
                        //转化为String
                        this.project = project;
                        ArrayList<Section> sections = program.getSections();
                        if(null != sections){
                            getSLANSections(sections);
                        }
                    }
                }
            }

            updateBasicInfo();
            updateProgramUI();
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

    //删除项目
    private void deleteProject(String fileName){
        String filePath = FileUtils.CONFIG_SLAN_PATH + File.separator + fileName +".prj";
        File file = new File(filePath);
        if(file != null &&click_position != -1){
            FileUtils.remove(file);
            mSlanFileNames.remove(click_position);
            slanFileAdapter.setTemplateList(mSlanFileNames);
            slanFileAdapter.notifyDataSetChanged();
        }
        //默认打开第一个
        initOpenProgram();
    }

    /**
     * 编辑项目
     */


    private void initOpenProgram(){
        if(mSlanFileNames != null && mSlanFileNames.size()>0){
            slanFileAdapter.setTemplateList(mSlanFileNames);
            slanFileAdapter.setSelectedPosition(0);
            click_position = 0;
            fileName = mSlanFileNames.get(0);
            //更新UI
            openSLANExFile(fileName);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.config_info_delete_tv){
            if(!StringUtil.isEmpty(fileName)){
                //弹出对话框提示是否确认删除项目
                showNoticeDialog("请确认是否删除项目？");
            }else{
                Toast.makeText(getActivity().getApplicationContext(),"请选择要删除的项目",Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.config_info_create_tv){
            //创建
            MessageEvent createSlan = new MessageEvent();
            createSlan.setMsgType(MsgType.MSG_CREATE_SLAN);
            EventBus.getDefault().post(createSlan);
        }else if(id== R.id.config_info_edit_tv){
            if(!StringUtil.isEmpty(fileName)&& click_position != -1){
                //弹出对话框提示是否确认删除项目
                MessageEvent edit = new MessageEvent();
                String name = mSlanFileNames.get(click_position);
                edit.setData(name);
                edit.setMsgType(MsgType.MSG_CONFIG_SLAN_EDIT);
                EventBus.getDefault().postSticky(edit);
            }else{
                //请选中
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_slan_toast_test_project),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.config_info_test_tv){
            //运行测试
            if(!StringUtil.isEmpty(fileName)&& click_position != -1){
                MessageEvent run = new MessageEvent();
                String name = mSlanFileNames.get(click_position);
                run.setData(name);
                run.setMsgType(MsgType.MSG_CONFIG_SLAN_RUN);
                EventBus.getDefault().post(run);
            }else{
                //请选中
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.config_slan_toast_test_project),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.config_info_program_tab_tv){
            mProgramLayout.setVisibility(View.VISIBLE);
            mParamLayout.setVisibility(View.GONE);
            mParamTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mProgramTabTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
        }else if(id == R.id.config_info_param_tab_tv){
            mProgramLayout.setVisibility(View.GONE);
            mParamLayout.setVisibility(View.VISIBLE);
            mProgramTabTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mParamTabTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
        }else if(id == R.id.config_info_param_setting_title_basic_tv) {
            //基本参数
            mTitleAmpParamTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mTitleMeltParamTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            mTitleAmpParamTv.setTextColor(getActivity().getResources().getColor(R.color.white));
            mTitleMeltParamTv.setTextColor(getActivity().getResources().getColor(R.color.black));
            mAMPParamLayout.setVisibility(View.VISIBLE);
            mMeltParamLayout.setVisibility(View.GONE);
        }else if(id == R.id.config_info_param_setting_title_melt_tv){
            //显示熔解参数
            mTitleMeltParamTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mTitleAmpParamTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            mTitleAmpParamTv.setTextColor(getActivity().getResources().getColor(R.color.black));
            mTitleMeltParamTv.setTextColor(getActivity().getResources().getColor(R.color.white));
            mAMPParamLayout.setVisibility(View.GONE);
            mMeltParamLayout.setVisibility(View.VISIBLE);
        }
    }

    private void showNoticeDialog(String message){
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
                    //删除
                    deleteProject(fileName);
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
}
