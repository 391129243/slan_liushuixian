package com.gidi.bio_pcr.slan.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.fastjson.JSON;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.QueryCondition;

import com.gidi.bio_pcr.bean.WellInfo;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.databinding.FragmentAioExDetailBinding;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.db.SLANWellResult;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.pcr.OnDetailExCallback;
import com.gidi.bio_pcr.slan.SLANManager;
import com.gidi.bio_pcr.slan.adapter.CtrlSLANResultAdapter;
import com.gidi.bio_pcr.slan.bean.SLANCurveList;
import com.gidi.bio_pcr.slan.bean.SLANExlResult;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.ScreenShotUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomAddSubEdit;
import com.gidi.bio_pcr.view.CustomCheckText;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.CustomLargeDialog;
import com.gidi.bio_pcr.view.CustomTitleText;
import com.gidi.bio_pcr.view.LineChartMarkView;
import com.gidi.bio_pcr.view.NumKeyboardDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;
import com.hongshi.slansdk.model.experiment.Experiment;
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.hongshi.slansdk.model.result.TargetResult;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AIoExDetailFragment extends Fragment implements OnDetailExCallback {
    private final String TAG = "AIoExDetailFragment";
    private SLANExDetailViewModel mViewModel;

    public static AIoExDetailFragment newInstance() {
        return new AIoExDetailFragment();
    }

    View rootv;
    private FragmentAioExDetailBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_aio_ex_detail, container, false);
        rootv = binding.getRoot();
        initViews();
        return rootv;
    }

    PipeMainViewModel mainViewModel;
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        registerEventBus();
        mViewModel = new ViewModelProvider(this).get(SLANExDetailViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainViewModel.getPcrExperimentModel().observe(requireActivity(), new Observer<ExperimentModel>() {
            @Override
            public void onChanged(ExperimentModel model) {
                if(model!=null) {
                   /* if(mRawCurve_Rb!=null) {
                        mRawCurve_Rb.setChecked(true);
                    }*/
                    experimentModel = model;
                    initExprimentModel();
                    initData();
                    setChannelState();
                    initMain();
                }
            }
        });
    }
    private RelativeLayout mTitleLayout;
    private ImageView mBackImg;


    private boolean isFAMCheck = false;
    private boolean isHEXCheck = false;
    private boolean isROXCheck = false;
    private boolean isCY5Check = false;
    private boolean isQuaSarCheck = false;
    private boolean isAttoCheck = false;


    private boolean isA1Check, isA2Check, isA3Check, isA4Check, isA5Check, isA6Check, isA7Check, isA8Check = false;
    private boolean isA1Enable,isA2Enable,isA3Enable,isA4Enable,isA5Enable,isA6Enable,isA7Enable,isA8Enable;
    private boolean isB1Enable,isB2Enable,isB3Enable,isB4Enable,isB5Enable,isB6Enable,isB7Enable,isB8Enable;
    private boolean isB1Check,isB2Check,isB3Check,isB4Check,isB5Check,isB6Check,isB7Check,isB8Check;
    private String curTemplatePath;


    //原始数据

    private List<SLANCurveList> mRawCurveList;//原始数据

    private List<SLANCurveList> mShowCurveList,mCheckRawList;

    private List<SLANCurveList> mAMPCurvList,mCheckAMPList;

    private List<SLANCurveList> mMeltCurvList,mCheckMeltList;

    private List<SLANCurveList> mPeakCurvList,mCheckPeakList;

    private List<SLANExlResult> mExlResults,mCheckExlResults;//对应的是e选择rox，fam等的结果列表
    private List<WellInfo> mWellInfos;
    private List<File> localList;

    private boolean isChartVisible = true;
    private boolean isExcelVisible = false;
    private boolean isSecAysVisible = false;
    private boolean isAllChanelCheck = false;


    private CtrlSLANResultAdapter resultAdapter;

    //阈值和峰高阈值
    private ResultSearchAsyncTask searchAsyncTask;
    private List<SLANWellResult> wellResults;
    private List<SLANWellResult> checkWellResult;
    private int currentView = 1;

    private boolean isFamThreshold,isHexThreshold,isRoxThreshold,iscy5Threshold = false;
    private boolean isQuasarThreshold = false;
    private boolean isAttoThreshold = false;
    private double famThreshold,hexThreshold,roxThreshold,cy5Threshold,quasarThreshold,attoThreshold;

    private boolean isMelt = false;//是熔解还是扩增
    private String startTime, testItem,description;
    private CustomDialog updateDbDialog;
    private List<SLANCurveList> mOriginRaw = new ArrayList<SLANCurveList>();
    private List<SLANCurveList> mOriginAMP = new ArrayList<SLANCurveList>();
    private List<SLANCurveList> mOriginMelt = new ArrayList<SLANCurveList>();
    private List<SLANCurveList> mOriginPeak= new ArrayList<SLANCurveList>();
    private List<SLANExlResult> mOriginExResults = new ArrayList<SLANExlResult>();
    private List<ConfigTarget> configTargets;

    private AmpAlgorithmAdapter mAmpParamterAdapter;
    private MeltParamterAdapter mMeltParamterAdapter;
    private NumKeyboardDialog numKeyboardDialog;
    private int ampAlogrithm_pos = -1;
    private int meltParamter_pos = -1;
    private Experiment mSec_Experiment = null;
    private PopupWindow mAysTypePopWindow,mOptimizationModeWindow,mDigitalFilterWindow;//定性定量,基线优化
    private PopupWindow mPeekTypeWindow,meltAlgPopWindow;
    private List<String> mAmpAysTypes,mOptimizationModes,mDighitalFilters;
    private List<String> mPeekTypes,mMeltPeakAlgorithms;
    //操作
    private PopupWindow mMoreActionWindows;
    private List<String> mActions;
    private DecimalFormat nf = new DecimalFormat( "0.00");
    private DecimalFormat nf1 = new DecimalFormat("0.000");
    private DecimalFormat nf2 = new DecimalFormat( "0.0");
    private boolean isScaleXRaw,isScaleXAmp,isScaleXMelt,isScaleXPeak = false;
    private float min_Raw_X,max_Raw_X,min_Amp_X,max_Amp_X,min_Melt_X,max_Melt_X,min_Peak_X,max_Peak_X;
    private float min_Raw_Y,max_Raw_Y,min_Amp_Y,max_Amp_Y,min_Melt_Y,max_Melt_Y,min_Peak_Y,max_Peak_Y;
    private int Type = 0;

    //扩增曲线算法枚举
    private List<String> mAMPAlgorithmList;
    private PopupWindow mAMPAlgWindow;
    private CustomSpinnerAdapter mAMPAlgSpinerAdapter;
    private ListView mAMPAlgorithmLv;


    private  String pcrFileFolder = "";


    private boolean isSampleOneSelected = false;
    private boolean isSampleTwoSelected = false;
    private boolean isSampleThreeSelected = false;
    private boolean isSampleFourSelected = false;
    private boolean isSampleFiveSelected = false;
    private boolean isSampleSixSelected = false;
    private boolean isSampleSevenSelected = false;
    private boolean isSampleEightSelected = false;

    // 样本1对应的孔位（A8/B8）
    private static final List<String> SAMPLE_1_WELLS = Arrays.asList("A8","B8");
    // 样本2对应的孔位（A7/B7）
    private static final List<String> SAMPLE_2_WELLS = Arrays.asList("A7","B7");
    // 样本3对应的孔位（A6/B6）
    private static final List<String> SAMPLE_3_WELLS = Arrays.asList("A6","B6");
    // 样本4对应的孔位（A5/B5）
    private static final List<String> SAMPLE_4_WELLS = Arrays.asList("A5","B5");
    // 样本5对应的孔位（A4/B4）
    private static final List<String> SAMPLE_5_WELLS = Arrays.asList("A4","B4");
    // 样本6对应的孔位（A3/B3）
    private static final List<String> SAMPLE_6_WELLS = Arrays.asList("A3", "B3");
    // 样本7对应的孔位（A2/B2）
    private static final List<String> SAMPLE_7_WELLS = Arrays.asList("A2", "B2");

    // 样本7对应的孔位（A1/B1）
    private static final List<String> SAMPLE_8_WELLS = Arrays.asList("A1", "B1");

    // 曲线Marker只创建一次
    private LineChartMarkView markView;
    // 临时集合复用，防止不断创建ArrayList
    private final List<ILineDataSet> tempDataSet = new ArrayList<>();

    private void initViews() {
        initTitleViews();
        if (null != binding.resultCurveChart) {
            binding.resultCurveChart.setNoDataText("");
        }

        binding.rawcurveRb.setChecked(true);
        resultAdapter = new CtrlSLANResultAdapter(getContext());

        //初始化参数设置页面
        initParamSettingViews();

        //初始化按钮点击效果
        initActionClick();
        initListener();
        initData();
        setChannelState();
        initAllChannel(false);
    }

    private void initTitleViews(){

        mTitleLayout = (RelativeLayout) rootv.findViewById(R.id.slan_ex_result_detail_title_layout);
        mBackImg = (ImageView) mTitleLayout.findViewById(R.id.tab_back_img);
        TextView titleTv = (TextView) mTitleLayout.findViewById(R.id.tab_title_tv);
        titleTv.setText(R.string.result_record);
    }

    /**
     * 初始化参数设置页面
     */
    private void initParamSettingViews(){
        binding.secAnalyseTv.setVisibility(View.INVISIBLE);
        binding.basicParamTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.basicParamTitleTv.setTextColor(getResources().getColor(R.color.white));
                binding.basicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
                binding.meltParamTitleTv.setTextColor(getResources().getColor(R.color.black));
                binding.meltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
                binding.ampParamLayout.setVisibility(View.VISIBLE);//扩增参数layout
                binding.meltParamLayout.setVisibility(View.GONE);//溶解曲线参数layout
                if(null != mAmpParamterAdapter){
                    mAmpParamterAdapter.setAmpAlgorithm(configTargets);
                }
            }
        });
        binding.meltParamTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.meltParamTitleTv.setTextColor(getResources().getColor(R.color.white));
                binding.meltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
                binding.basicParamTitleTv.setTextColor(getResources().getColor(R.color.black));
                binding.basicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
                binding.ampParamLayout.setVisibility(View.GONE);//扩增参数layout
                binding.meltParamLayout.setVisibility(View.VISIBLE);//溶解曲线参数layout
                if(null != mMeltParamterAdapter){
                    mMeltParamterAdapter.setMeltParamter(configTargets);
                }
            }
        });
        //扩增曲线算法
        binding.ampAlgorithmTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expandAmpAlg(view);
            }
        });

        initAmpAlgorithmSpinner();

        mAmpParamterAdapter = new AmpAlgorithmAdapter(getContext());
        binding.ampAlgorithmLv.setAdapter(mAmpParamterAdapter);//扩增曲线参数列表

        mMeltParamterAdapter = new MeltParamterAdapter(getContext());
        binding.meltParamterLv.setAdapter(mMeltParamterAdapter);//溶解曲线参数列表
    }
    /**
     * 保存json,截图、更多等动作
     */
    private void initActionClick(){
        binding.secAnalyseTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != mSec_Experiment){
                    secondAnalyse(mSec_Experiment);
                }
            }
        });
        binding.saveJsonTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveExJsonFile();
            }
        });

        binding.resultScreenshotTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                screenShot();
            }
        });

        binding.resultMoreTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expandMoreActions(view);
            }
        });
    }


    protected void initListener() {

        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                finish();
                if(mainViewModel!=null) {
                    mainViewModel.onNavClicked(ConstantData.NAV_RECORDS);
                }
            }
        });

        binding.allChannelCheckTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                initAllChannel(isCheck);
            }

        });


        binding.famChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isFAMCheck = isCheck;
                if (!isFAMCheck) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isHEXCheck && isQuaSarCheck && isAttoCheck){
                        isAllChanelCheck = true;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mFamMsg = new MessageEvent();
                mFamMsg.setMsgType(MsgType.MSG_SHOW_FAM_CHANNEL_CURVE);
                EventBus.getDefault().post (mFamMsg);
            }
        });

        binding.hexChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isHEXCheck = isCheck;
                if (!isHEXCheck) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isFAMCheck && isQuaSarCheck && isAttoCheck){
                        isAllChanelCheck = true;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mHEXMsg = new MessageEvent();
                mHEXMsg.setMsgType(MsgType.MSG_SHOW_HEX_CHANNEL_CURVE);
                EventBus.getDefault().post (mHEXMsg);

            }
        });

        binding.roxChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isROXCheck = isCheck;
                if (!isROXCheck) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isFAMCheck && isCY5Check && isHEXCheck&& isQuaSarCheck && isAttoCheck){
                        isAllChanelCheck = true;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mROXMsg = new MessageEvent();
                mROXMsg.setMsgType(MsgType.MSG_SHOW_ROX_CHANNEL_CURVE);
                EventBus.getDefault().post (mROXMsg);
            }
        });

        binding.cy5ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isCY5Check = isCheck;
                if (!isCY5Check) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isHEXCheck && isFAMCheck && isQuaSarCheck && isAttoCheck){
                        isAllChanelCheck = true;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mCY5Msg = new MessageEvent();
                mCY5Msg.setMsgType(MsgType.MSG_SHOW_CY5_CHANNEL_CURVE);
                EventBus.getDefault().post (mCY5Msg);

            }
        });
        binding.quasarChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isQuaSarCheck = isCheck;
                if (!isQuaSarCheck) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isHEXCheck && isFAMCheck && isCY5Check && isAttoCheck){
                        isAllChanelCheck = true;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mQuaSarMsg = new MessageEvent();
                mQuaSarMsg.setMsgType(MsgType.MSG_SHOW_QUASAR_CHANNEL_CURVE);
                EventBus.getDefault().post (mQuaSarMsg);

            }
        });
        binding.attoChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isAttoCheck = isCheck;
                if (!isAttoCheck) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isHEXCheck && isFAMCheck && isCY5Check && isQuaSarCheck){
                        isAllChanelCheck = true;
                        binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mAttoMsg = new MessageEvent();
                mAttoMsg.setMsgType(MsgType.MSG_SHOW_ATTO_CHANNEL_CURVE);
                EventBus.getDefault().post (mAttoMsg);
            }
        });


        //样本1
        binding.resultSampleOneLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 切换样本1选中状态
                isSampleOneSelected = !isSampleOneSelected;
                // 更新UI样式
                if (isSampleOneSelected) {
                    filterAndShowSampleWells(SAMPLE_1_WELLS);
                    binding.resultSampleOneLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);

                } else {
                    binding.resultSampleOneLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                    // 取消选中时，清空所有孔位显示（或清空，根据需求调整）
                    filterAndClearSampleWells(SAMPLE_1_WELLS);
                }
            }
        });
        //样本2
        binding.resultSampleTwoLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!isSampleTwoSelected){
                    isSampleTwoSelected = true;
                    filterAndShowSampleWells(SAMPLE_2_WELLS);
                    binding.resultSampleTwoLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                }else{
                    isSampleTwoSelected = false;
                    filterAndClearSampleWells(SAMPLE_2_WELLS);
                    binding.resultSampleTwoLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                }
            }
        });
        //样本3
        binding.resultSampleThreeLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isSampleThreeSelected){
                    isSampleThreeSelected = true;
                    filterAndShowSampleWells(SAMPLE_3_WELLS);
                    binding.resultSampleThreeLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                }else{
                    filterAndClearSampleWells(SAMPLE_3_WELLS);
                    isSampleThreeSelected = false;
                    binding.resultSampleThreeLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                }
            }
        });
        binding.resultSampleFourLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isSampleFourSelected){
                    isSampleFourSelected = true;
                    filterAndShowSampleWells(SAMPLE_4_WELLS);
                    binding.resultSampleFourLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                }else{
                    isSampleFourSelected = false;
                    filterAndClearSampleWells(SAMPLE_4_WELLS);
                    binding.resultSampleFourLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                }
            }
        });
        binding.resultSampleFiveLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isSampleFiveSelected){
                    isSampleFiveSelected = true;
                    filterAndShowSampleWells(SAMPLE_5_WELLS);
                    binding.resultSampleFiveLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                }else{
                    isSampleFiveSelected = false;
                    filterAndClearSampleWells(SAMPLE_5_WELLS);
                    binding.resultSampleFiveLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                }
            }
        });
        binding.resultSampleSixLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isSampleSixSelected){
                    isSampleSixSelected = true;
                    filterAndShowSampleWells(SAMPLE_6_WELLS);
                    binding.resultSampleSixLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                }else{
                    isSampleSixSelected = false;
                    filterAndClearSampleWells(SAMPLE_6_WELLS);
                    binding.resultSampleSixLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                }
            }
        });
        binding.resultSampleSevenLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isSampleSevenSelected){
                    isSampleSevenSelected = true;
                    filterAndShowSampleWells(SAMPLE_7_WELLS);
                    binding.resultSampleSevenLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                }else{
                    isSampleSevenSelected = false;
                    filterAndClearSampleWells(SAMPLE_7_WELLS);
                    binding.resultSampleSevenLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                }
            }
        });
        binding.resultSampleEightLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isSampleEightSelected){
                    isSampleEightSelected = true;
                    filterAndShowSampleWells(SAMPLE_8_WELLS);
                    binding.resultSampleEightLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                }else{
                    isSampleEightSelected = false;
                    filterAndClearSampleWells(SAMPLE_8_WELLS);
                    binding.resultSampleEightLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                }
            }
        });
        binding.sampleEightA1Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isA1Enable){
                    isA1Check = !isA1Check;
                    if (!isA1Check) {

                        if(!isA1Check && !isB1Check){

                            isSampleEightSelected = false;
                            Log.i(TAG,"==isSampleFourSelected=="+isSampleEightSelected );
                            binding.resultSampleEightLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleEightSelected = true;
                        binding.resultSampleEightLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isA1Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleEightB1Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isB1Enable){
                    isB1Check = !isB1Check;
                    if (!isB1Check) {
                        if(!isB1Check && !isA1Check){
                            isSampleEightSelected = false;
                            binding.resultSampleEightLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleEightSelected = true;
                        binding.resultSampleEightLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isB1Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleSevenA2Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isA2Enable){
                    isA2Check = !isA2Check;
                    if (!isA2Check) {
                        if(!isA2Check && !isB2Check){
                            isSampleSevenSelected = false;
                            binding.resultSampleSevenLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleSevenSelected = true;
                        binding.resultSampleSevenLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isA2Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleSevenB2Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isB2Enable){
                    isB2Check = !isB2Check;
                    if (!isB2Check) {
                        if(!isB2Check && !isA2Check){
                            isSampleSevenSelected = false;
                            binding.resultSampleSevenLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleSevenSelected = true;
                        binding.resultSampleSevenLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isB2Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleSixA3Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isA3Enable){
                    isA3Check = !isA3Check;
                    if (!isA3Check) {
                        if(!isA3Check&& !isB3Check){
                            isSampleSixSelected = false;
                            binding.resultSampleSixLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleSixSelected = true;
                        binding.resultSampleSixLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isA3Check, (TextView) view);
                    updateWellResult();
                }
            }
        });


        binding.sampleSixB3Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isB3Enable){
                    isB3Check = !isB3Check;
                    if (!isB3Check) {
                        if(!isB3Check && !isA3Check){
                            isSampleSixSelected = false;
                            binding.resultSampleSixLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleSixSelected = true;
                        binding.resultSampleSixLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isB3Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleFiveA4Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isA4Enable){
                    isA4Check = !isA4Check;
                    if (!isA4Check) {
                        if(!isA4Check&& !isB4Check){
                            isSampleFiveSelected = false;
                            binding.resultSampleFiveLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleFiveSelected = true;
                        binding.resultSampleFiveLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isA4Check, (TextView) view);
                    updateWellResult();
                }
            }
        });

        binding.sampleFiveB4Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isB4Enable){
                    isB4Check = !isB4Check;
                    if (!isB4Check) {
                        if(!isB4Check  && !isA4Check){
                            isSampleFiveSelected = false;
                            binding.resultSampleFiveLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleFiveSelected = true;
                        binding.resultSampleFiveLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isB4Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleFourA5Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isA5Enable){
                    isA5Check = !isA5Check;
                    if (!isA5Check) {
                        if(!isA5Check && !isB5Check){
                            isSampleFourSelected = false;
                            binding.resultSampleFourLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleFourSelected = true;
                        binding.resultSampleFourLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isA5Check, (TextView) view);
                    updateWellResult();
                }
            }
        });

        binding.sampleFourB5Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isB5Enable){
                    isB5Check = !isB5Check;
                    if (!isB5Check) {
                        if( !isB5Check && !isA5Check){
                            isSampleFourSelected = false;
                            binding.resultSampleFourLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleFourSelected = true;
                        binding.resultSampleFourLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isB5Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleThreeA6Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isA6Enable){
                    isA6Check = !isA6Check;
                    if (!isA6Check) {
                        if(!isA6Check && !isB6Check){
                            isSampleThreeSelected= false;
                            binding.resultSampleThreeLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else{
                        isSampleThreeSelected = true;
                        binding.resultSampleThreeLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isA6Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleThreeB6Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isB6Enable){
                    isB6Check = !isB6Check;
                    if (!isB6Check) {
                        if(!isB6Check && !isA6Check){
                            isSampleThreeSelected = false;
                            binding.resultSampleThreeLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleThreeSelected = true;
                        binding.resultSampleThreeLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isB6Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleTwoA7Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isA7Enable){
                    isA7Check = !isA7Check;
                    if (!isA7Check) {
                        if(!isB7Check &&!isA7Check){
                            isSampleTwoSelected = false;
                            binding.resultSampleTwoLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleTwoSelected = true;
                        binding.resultSampleTwoLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isA7Check, (TextView) view);
                    updateWellResult();
                }
            }
        });

        binding.sampleTwoB7Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isB7Enable){
                    isB7Check = !isB7Check;
                    if (!isB7Check) {
                        if(!isB7Check  && !isA7Check){
                            isSampleTwoSelected = false;
                            binding.resultSampleTwoLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleTwoSelected = true;
                        binding.resultSampleTwoLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isB7Check, (TextView) view);
                    updateWellResult();
                }
            }
        });

        binding.sampleOneA8Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isA8Enable){
                    isA8Check = !isA8Check;
                    if (!isA8Check) {
                        if(!isB8Check && !isA8Check){
                            isSampleOneSelected = false;
                            binding.resultSampleOneLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleOneSelected = true;
                        binding.resultSampleOneLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isA8Check, (TextView) view);
                    updateWellResult();
                }
            }
        });
        binding.sampleOneB8Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isB8Enable){
                    isB8Check = !isB8Check;
                    if (!isB8Check) {
                        if(!isB8Check && !isA8Check ){
                            isSampleOneSelected = false;
                            binding.resultSampleOneLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
                        }
                    } else {
                        isSampleOneSelected = true;
                        binding.resultSampleOneLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
                    }
                    setHodeState(isB8Check, (TextView) view);
                    updateWellResult();
                }
            }
        });

        SLANManager.getInstance().setOnDetailExCallback(this);
        binding.curveRbRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rawcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isSecAysVisible = false;
                        isExcelVisible = false;
                    }
                    if(isChartVisible){
                        binding.secAnalyseTv.setVisibility(View.INVISIBLE);
                        binding.channelLayout.setVisibility(View.VISIBLE);
                        binding.holeLayout.setVisibility(View.VISIBLE);
                        // mUploadHoleLayout.setVisibility(View.GONE);
                        binding.resultExlLayout.setVisibility(View.GONE);
                        binding.curveChartLayout.setVisibility(View.VISIBLE);
                        binding.secAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 1;
                    getAllRawCurve();
                    drawRawCurve();
                } else if (checkedId == R.id.ampcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        binding.secAnalyseTv.setVisibility(View.INVISIBLE);
                        binding.channelLayout.setVisibility(View.VISIBLE);
                        binding.holeLayout.setVisibility(View.VISIBLE);
                        binding.resultExlLayout.setVisibility(View.GONE);
                        binding.curveChartLayout.setVisibility(View.VISIBLE);
                        binding.secAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 2;
                    getAllAPMCurve();
                    drawAMPCurve();
                } else if (checkedId == R.id.slan_ex_result_meltcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        binding.secAnalyseTv.setVisibility(View.INVISIBLE);
                        binding.channelLayout.setVisibility(View.VISIBLE);
                        binding.holeLayout.setVisibility(View.VISIBLE);
                        // mUploadHoleLayout.setVisibility(View.GONE);
                        binding.resultExlLayout.setVisibility(View.GONE);
                        binding.curveChartLayout.setVisibility(View.VISIBLE);
                        binding.secAnalyseLayout.setVisibility(View.GONE);

                    }
                    currentView = 3;
                    getAllMelturve();
                    drawMeltCurve();
                } else if (checkedId == R.id.slan_ex_result_peakcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        binding.secAnalyseTv.setVisibility(View.INVISIBLE);
                        binding.channelLayout.setVisibility(View.VISIBLE);
                        binding.holeLayout.setVisibility(View.VISIBLE);
                        //mUploadHoleLayout.setVisibility(View.GONE);
                        binding.resultExlLayout.setVisibility(View.GONE);
                        binding.curveChartLayout.setVisibility(View.VISIBLE);
                        binding.secAnalyseLayout.setVisibility(View.GONE);

                    }
                    currentView = 4;
                    getAllPeakCurve();
                    drawPeakCurve();
                } else if (checkedId == R.id.slan_ex_result_list_rb) {
                    currentView = 5;
                    if (!isExcelVisible) {
                        isExcelVisible = true;
                        isChartVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isExcelVisible){
                        binding.secAnalyseTv.setVisibility(View.INVISIBLE);
                        binding.holeLayout.setVisibility(View.VISIBLE);
                        // mUploadHoleLayout.setVisibility(View.GONE);
                        binding.channelLayout.setVisibility(View.VISIBLE);
                        binding.resultExlLayout.setVisibility(View.VISIBLE);
                        binding.curveChartLayout.setVisibility(View.GONE);
                        binding.secAnalyseLayout.setVisibility(View.GONE);

                        getShowExlResults();
                        MessageEvent update = new MessageEvent();
                        update.setMsgType(MsgType.MSG_UPDATE_EXPERIMENT_EXCEL);
                        EventBus.getDefault().post(update);
                    }

                }else if(checkedId == R.id.slan_ex_result_sec_analyse_rb){
                    //二次分析界面
                    //参数设置
                    Log.e(TAG,"===打开参数设置===");
                    if (!isSecAysVisible) {
                        isChartVisible = false;
                        isExcelVisible = false;
                        isSecAysVisible = true;
                    }
                    if(isSecAysVisible){
                        binding.secAnalyseTv.setVisibility(View.VISIBLE);
                        binding.channelLayout.setVisibility(View.INVISIBLE);
                        binding.holeLayout.setVisibility(View.INVISIBLE);
                        // mUploadHoleLayout.setVisibility(View.GONE);
                        binding.secAnalyseLayout.setVisibility(View.VISIBLE);
                        binding.resultExlLayout.setVisibility(View.GONE);
                        binding.curveChartLayout.setVisibility(View.GONE);

                        String second_file_path = getSecondAyseFilePath();
                        if(StringUtil.isEmpty(second_file_path)){
                            Toast.makeText(getContext().getApplicationContext(),
                                    getResources().getString(R.string.result_ex_detail_no_second_file), Toast.LENGTH_SHORT).show();
                            return;
                        }else{
                            if (null != PcrApplication.slanService) {
                                if(null != mSec_Experiment){
                                    mSec_Experiment = null;
                                }
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //正在打开文件获取
                                        Experiment sec_experiment = PcrApplication.slanService.openExperiment(second_file_path,true);
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if(sec_experiment != null){
                                                    mSec_Experiment = sec_experiment;
                                                    getSLANExperimentParamter(sec_experiment);
                                                }else{
                                                    Toast.makeText(getContext().getApplicationContext(),
                                                            getResources().getString(R.string.result_ex_detail_toast_cannot_second_analyse),
                                                            Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                            }
                                        });
                                    }
                                }).start();
                            }else{
                                Toast.makeText(getContext().getApplicationContext(),
                                        getResources().getString(R.string.result_ex_detail_toast_cannot_second_analyse), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        });
        mAmpParamterAdapter.setOnItemClickListener(new AmpAlgorithmAdapter.OnItemClickListener() {
            @Override
            public void onQualitativeAnalysis(View view, int position) {
                ampAlogrithm_pos = position;
                expandQualitativeAnalysis(view);
            }

            @Override
            public void onBeginBase(View view, int position) {
                ampAlogrithm_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    ConfigTarget configTarget = configTargets.get(position);
                    String beginBaseline = String.valueOf(configTarget.getBeginBaseline());
                    showNumKeyboardDialog(3,view,beginBaseline,-1);
                }

            }

            @Override
            public void onEndbaseLine(View view, int position) {
                ampAlogrithm_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    ConfigTarget configTarget = configTargets.get(position);
                    String endBaseline = String.valueOf(configTarget.getEndBaseline());
                    showNumKeyboardDialog(2,view,endBaseline,-1);
                }

            }

            @Override
            public void onOptimizationMode(View view, int position) {
                ampAlogrithm_pos = position;
                expandOptimizationMode(view);
            }

            @Override
            public void onChangeThreshold(View view, int position) {
                ampAlogrithm_pos = position;
                if(null != configTargets &&configTargets.size()>0){
                    ConfigTarget configTarget = configTargets.get(position);
                    String threshold = nf.format(configTarget.getThreshold());
                    showNumKeyboardDialog(4,view,threshold,-1);
                }

            }

            @Override
            public void onDigitalFilter(View view, int position) {
                ampAlogrithm_pos = position;
                expandDigitalFilter(view);
            }

            @Override
            public void onAutoThreshold(int position) {
                ampAlogrithm_pos = position;
                if(null != configTargets &&configTargets.size()>0){
                    ConfigTarget configTarget = configTargets.get(position);
                    if(configTarget.isAutoThreshold()){
                        configTarget.setAutoThreshold(false);
                    }else {
                        configTarget.setAutoThreshold(true);
                    }
                    mAmpParamterAdapter.setAmpAlgorithm(configTargets);
                }
            }
        });

        mMeltParamterAdapter.setOnItemClickListener(new MeltParamterAdapter.OnItemClickListener() {
            @Override
            public void onChangeMinTemperature(View view, int position) {
                meltParamter_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    double min_temp = configTargets.get(position).getMinTemperature();
                    String minTemp = String.valueOf(min_temp);
                    showNumKeyboardDialog(5,view,minTemp,-1);
                }
            }

            @Override
            public void onChangeMaxTemperature(View view, int position) {
                meltParamter_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    double max_temp = configTargets.get(position).getMaxTemperature();
                    String maxTemp = String.valueOf(max_temp);
                    showNumKeyboardDialog(6,view,maxTemp,-1);
                }
            }

            @Override
            public void onMeltPeakAlgorithmTv(View view, int position) {
                meltParamter_pos = position;
                expandMeltPeakAlg(view);
            }

            @Override
            public void onChangePeekHighthreshold(View view, int position) {
                meltParamter_pos = position;
                if(null != configTargets && configTargets.size()>0){
                    double peekHighThreshold = configTargets.get(position).getPeekHighThreshold();
                    String peekHighThresholdStr = String.valueOf(peekHighThreshold);
                    showNumKeyboardDialog(7,view,peekHighThresholdStr,-1);
                }

            }

            @Override
            public void onPeekType(View view, int position) {
                meltParamter_pos = position;
                expandPeekType(view);
            }
        });

    }

    private void initAllChannel(boolean isCheck) {
        isAllChanelCheck = isCheck;
        if (isAllChanelCheck) {
            isHEXCheck = true;
            isFAMCheck = true;
            isROXCheck = true;
            isCY5Check = true;
            isQuaSarCheck = true;
            isAttoCheck = true;
        } else {
            isHEXCheck = false;
            isFAMCheck = false;
            isROXCheck = false;
            isCY5Check = false;
            isQuaSarCheck = false;
            isAttoCheck = false;
        }
        setChannelState();
        MessageEvent mAllChanelMsg = new MessageEvent();
        mAllChanelMsg.setMsgType(MsgType.MSG_SHOW_ALL_CHANEL_RESULT);
        EventBus.getDefault().post(mAllChanelMsg);
    }

    protected void initData() {
        wellResults = new ArrayList<SLANWellResult>();
        mRawCurveList = new ArrayList<SLANCurveList>();
        mCheckRawList = new ArrayList<SLANCurveList>();

        mAMPCurvList = new ArrayList<SLANCurveList>();
        mCheckAMPList = new ArrayList<SLANCurveList>();

        mMeltCurvList = new ArrayList<SLANCurveList>();
        mCheckMeltList = new ArrayList<SLANCurveList>();

        mPeakCurvList = new ArrayList<SLANCurveList>();
        mCheckPeakList = new ArrayList<SLANCurveList>();

        mExlResults = new ArrayList<SLANExlResult>();
        mCheckExlResults = new ArrayList<SLANExlResult>();
        mShowCurveList = new ArrayList<SLANCurveList>();
        resultAdapter.setResultList(mCheckExlResults);
        binding.resultExlLv.setAdapter(resultAdapter);
        checkWellResult = new ArrayList<SLANWellResult>();
        mWellInfos = new ArrayList<WellInfo>();

        configTargets = new ArrayList<ConfigTarget>();
        isA1Enable = isA2Enable = isA3Enable = isA4Enable = false;
        isA5Enable = isA6Enable = isA7Enable = isA8Enable = false;

        isB1Enable = isB2Enable = isB3Enable = isB4Enable = false;
        isB5Enable = isB6Enable = isB7Enable = isB8Enable = false;

        isB1Check = isB2Check = isB3Check = isB4Check = false;
        isB5Check = isB6Check = isB7Check = isB8Check = false;
        initPeekTypeWindow();
        initAmpAysTypeWindow();
        initOptimizationModeWindow();
        initAmpDigitalFilter();
        initMeltPeakAlgorithm();
        initMoreActions();
        initAMPAlgorithm();
    }
    ExperimentModel experimentModel;
    private void initExprimentModel(){
        // ExperimentModel experimentModel = (ExperimentModel) getIntent().getSerializableExtra("item");
        if (null != experimentModel) {
            pcrFileFolder = experimentModel.getFilePath();

            startTime = experimentModel.getStartTime();
            testItem = experimentModel.getTestItem();
            description = experimentModel.getDescription();
            binding.resultExTimeTv.setContentText(startTime);
            binding.resultExTestitemTv.setText(testItem);
            //是全流程还是单PCR
            int Type = experimentModel.getType();
            Log.i(TAG,"===文件保存的Type==" + Type);
            String templateName = experimentModel.getTestItem();//模板名称
            switch (Type){
                case 0:
                    curTemplatePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH+ File.separator + templateName
                            + File.separator ;
                    break;
                case ConstantData.TYPE_PCR:
                    curTemplatePath = FileUtils.SINGLE_PCR_TEMPLATE_PATH+ File.separator + templateName
                            + File.separator ;
                    break;
                default:
                    break;
            }
            Log.i(TAG,"===文件保存的curTemplatePath==" + curTemplatePath);
            initFiles();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onResume() {
        super.onResume();
//        initMain();
    }
    private void initMain(){

        isScaleXRaw = false;
        isScaleXAmp = false;
        isScaleXMelt = false;
        isScaleXPeak = false;
        //查询实验结果详情
        queryExDetail();
    }
    @Override
    public void onStop() {
        super.onStop();
        SLANManager.getInstance().setExperiment(null);
       // SLANManager.getInstance().releaseOnDetailExCallback();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unRegisterEventBus();
        markView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SLANManager.getInstance().releaseOnDetailExCallback();

        clearAllList();
        cancelTask();
        markView = null;
        mSec_Experiment = null;
        if (null != updateDbDialog) {
            updateDbDialog.dismiss();
            updateDbDialog = null;
        }

        if(null != loadingDialog){
            loadingDialog.dismiss();
            loadingDialog = null;
        }
        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
        if(null != configTargets){
            configTargets.clear();
            configTargets = null;
        }

        if(null != mPeekTypeWindow){
            mPeekTypeWindow.dismiss();
            mPeekTypeWindow = null;
        }
        if(null != mPeekTypes){
            mPeekTypes.clear();
            mPeekTypes = null;
        }

        if(null != mAmpAysTypes){
            mAmpAysTypes.clear();
            mAmpAysTypes = null;
        }
        if(null != mAysTypePopWindow){
            mAysTypePopWindow.dismiss();
            mAysTypePopWindow = null;
        }

        if(null != mOptimizationModeWindow){
            mOptimizationModeWindow.dismiss();
            mOptimizationModeWindow = null;
        }
        if(null != mOptimizationModes){
            mOptimizationModes.clear();
            mOptimizationModes = null;
        }

        if(null != mDigitalFilterWindow){
            mDigitalFilterWindow.dismiss();
            mDigitalFilterWindow = null;
        }
        if(null != mDighitalFilters){
            mDighitalFilters.clear();
            mDighitalFilters = null;
        }

        if(null != meltAlgPopWindow){
            meltAlgPopWindow.dismiss();
            meltAlgPopWindow = null;
        }
        if(null !=mMeltPeakAlgorithms){
            mMeltPeakAlgorithms.clear();
            mMeltPeakAlgorithms = null;
        }

        if(null != coordinateDialog){
            coordinateDialog.dismiss();
            coordinateDialog = null;
        }
        if(null != mActions){
            mActions.clear();
            mActions = null;
        }

        if(null != mMoreActionWindows){
            mMoreActionWindows.dismiss();
            mMoreActionWindows = null;
        }


        mainViewModel = null;
        mViewModel = null;
    }


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


    private void setChannelState() {
        if (isROXCheck) {
            binding.roxChannelTv.setCheckState(R.drawable.item_chanel_check, isROXCheck);
        } else {
            binding.roxChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isROXCheck);
        }
        if (isCY5Check) {
            binding.cy5ChannelTv.setCheckState(R.drawable.item_chanel_check, isCY5Check);
        } else {
            binding.cy5ChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isCY5Check);
        }

        if (isHEXCheck) {
            binding.hexChannelTv.setCheckState(R.drawable.item_chanel_check, isHEXCheck);
        } else {
            binding.hexChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isHEXCheck);
        }

        if (isFAMCheck) {
            binding.famChannelTv.setCheckState(R.drawable.item_chanel_check, isFAMCheck);
        } else {
            binding.famChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isFAMCheck);
        }

        if (isQuaSarCheck) {
            binding.quasarChannelTv.setCheckState(R.drawable.item_chanel_check, isQuaSarCheck);
        } else {
            binding.quasarChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isQuaSarCheck);
        }
        if (isAttoCheck) {
            binding.attoChannelTv.setCheckState(R.drawable.item_chanel_check, isAttoCheck);
        } else {
            binding.attoChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isAttoCheck);
        }

        if (isCY5Check && isFAMCheck && isROXCheck && isHEXCheck && isQuaSarCheck && isAttoCheck) {
            isAllChanelCheck = true;
            binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_check, isAllChanelCheck);
        } else {
            isAllChanelCheck = false;
            binding.allChannelCheckTv.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
        }
    }

    private void initFiles(){
        localList = new ArrayList<File>();
        localList.clear();
        if(!StringUtil.isEmpty(curTemplatePath)){
            File allFileDir = new File(curTemplatePath);
            if (null != allFileDir) {
                if (allFileDir.isDirectory() && allFileDir.exists()) {
                    Collections.addAll(localList, allFileDir.listFiles());
                }
            }
        }

    }

    /**
     * 查询实验结果详情
     */
    private void queryExDetail(){
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setTestStartTime(startTime);
        queryCondition.setTestItem(testItem);
        queryCondition.setIdcardNum("");
        queryCondition.setRegentCode("");
        showLoadingDialog(getResources().getString(R.string.dialog_message_query_data));
        excuteTask(queryCondition);
    }


    /**************************************二次分析*************************************/

    /**
     * 1.获取二次分析所需要的文件
     * 获取博维的结果
     */
    private String getSecondAyseFilePath(){
        String pcrFolderPath = experimentModel.getFilePath();
        Log.e(TAG,"===二次分析文件文件夹路径===" + pcrFolderPath );
        if (!StringUtil.isEmpty(pcrFolderPath)) {
            //找文件
            File file = getSLANExPCRFile(pcrFolderPath);
            if (file != null && file.exists()) {
                //读取.PCR文件
                return file.getAbsolutePath();
            }
        }
        return "";
    }

    /**
     * 获取保存在本地结果文件夹的PCR文件
     * @return
     */
    private File getSLANExPCRFile(String pcrFolderPath) {
        Log.i(TAG, "getSLANExPCRFile path" + pcrFolderPath);
        File fileDir = new File(pcrFolderPath);
        if (fileDir.exists() && fileDir.isDirectory()) {
            File[] files = fileDir.listFiles();//所有的json文件
            if(files != null && files.length>0){
                for(File file : files){
                    if(file.getName().endsWith(".pcr")){
                        return file;
                    }
                }
            }
        }
        return null;
    }

    private boolean isHasExJsonFile(String pcrFileFolder){
        Log.i(TAG, "isHasExJsonFile path" + pcrFileFolder);
        File fileDir = new File(pcrFileFolder);
        if (fileDir.exists() && fileDir.isDirectory()) {
            File[] files = fileDir.listFiles();//所有的json文件
            if(files != null && files.length>0){
                for(File file : files){
                    if(file.getName().endsWith(".json")){
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * 获取实验参数
     */
    /**
     * 获取项目参数
     */
    private void getSLANExperimentParamter(Experiment experiment){
        if(null != PcrApplication.slanService && configTargets != null){
            iscy5Threshold = isFamThreshold = isHexThreshold = isRoxThreshold = false;
            isQuasarThreshold = isAttoThreshold = false;
            configTargets.clear();
            if(null != experiment){
                ArrayList<AnalyseParamter> analyseParamters = PcrApplication.slanService
                        .getExperimentParamter(experiment.getName());
                if(analyseParamters != null && analyseParamters.size()>0){
                    ArrayList<Target> targets = analyseParamters.get(0).getTargets();
                    AMPAlgorithm = analyseParamters.get(0).getAMPAlgorithm();
                    if(null != targets && targets.size()>0){
                        //目标名称
                        for(Target target : targets){
                            if(target.getChannelNo() == 1){
                                TargetParamter famParamer = target.getParamter();
                                isFamThreshold = true;
                                ConfigTarget famTarget = new ConfigTarget();
                                famTarget.setName("FAM");
                                famTarget.setTargetIndex(1);
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
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) famParamer;
                                    famTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    famTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    famTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    famTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    famTarget.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }
                                configTargets.add(famTarget);
                                famThreshold = famParamer.getThreshold();
                            }else if(target.getChannelNo() == 3){
                                isRoxThreshold = true;
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
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) roxParamer;
                                    roxTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    roxTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    roxTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    roxTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    roxTarget.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }
                                roxThreshold = roxParamer.getThreshold();
                                configTargets.add(roxTarget);
                            }else if(target.getChannelNo() == 2){
                                isHexThreshold = true;
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
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) hexParamer;
                                    hexTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    hexTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    hexTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    hexTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    hexTarget.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }
                                configTargets.add(hexTarget);
                                hexThreshold = hexParamer.getThreshold();
                            }else if(target.getChannelNo() == 4){
                                iscy5Threshold = true;
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
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) cy5Paramer;
                                    cy5Target.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    cy5Target.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    cy5Target.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    cy5Target.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    cy5Target.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }
                                cy5Threshold = cy5Paramer.getThreshold();
                                configTargets.add(cy5Target);
                            }else if(target.getChannelNo() == 5){
                                isQuasarThreshold = true;
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
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) quasarParamer;
                                    quasarTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    quasarTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    quasarTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    quasarTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    quasarTarget.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }
                                quasarThreshold = quasarParamer.getThreshold();
                                configTargets.add(quasarTarget);
                            }else if(target.getChannelNo() == 6){
                                isAttoThreshold = true;
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
                                    isMelt = true;
                                    TargetParamterMelt meltTargetParamer = (TargetParamterMelt) attoParamer;
                                    attoTarget.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                    attoTarget.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                    attoTarget.setMinTemperature(meltTargetParamer.getMinTemperature());
                                    attoTarget.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                    attoTarget.setPeekType(meltTargetParamer.getPeekType());
                                }else{
                                    isMelt = false;
                                }
                                attoThreshold = attoParamer.getThreshold();
                                configTargets.add(attoTarget);
                            }
                        }
                    }
                }
            }
        }
        if(isMelt){
            binding.meltParamTitleTv.setVisibility(View.VISIBLE);
        }else{
            binding.meltParamTitleTv.setVisibility(View.INVISIBLE);
        }
        if(AMPAlgorithm == 0){
            binding.ampAlgorithmTv.setText(getResources().getString(R.string.config_info_ampalgorithm_default));
        }else if(AMPAlgorithm == 1){
            binding.ampAlgorithmTv.setText(getResources().getString(R.string.config_info_ampalgorithm_subtraction));
        }else if(AMPAlgorithm == 2){
            binding.ampAlgorithmTv.setText(getResources().getString(R.string.config_info_ampalgorithm_relativeLinear));
        }
        if(null != configTargets){
            if(null != mAmpParamterAdapter){
                mAmpParamterAdapter.setAmpAlgorithm(configTargets);
            }
            if(null != mMeltParamterAdapter){
                mMeltParamterAdapter.setMeltParamter(configTargets);
            }
        }
    }



    private void clearRawList() {
        if (null != mRawCurveList) {
            mRawCurveList.clear();
            mRawCurveList = null;
        }
        if (null != mCheckRawList) {
            mCheckRawList.clear();
            mCheckRawList = null;
        }
    }

    private void clearAMPList() {
        if (null != mAMPCurvList) {
            mAMPCurvList.clear();
            mAMPCurvList = null;
        }
        if (null != mCheckAMPList) {
            mCheckAMPList.clear();
            mCheckAMPList = null;
        }
    }

    private void clearMeltList() {

        if (null != mMeltCurvList) {
            mMeltCurvList.clear();
            mMeltCurvList = null;
        }
        if (null != mCheckMeltList) {
            mCheckMeltList.clear();
            mCheckMeltList = null;
        }
    }

    private void clearPeakList() {
        if (null != mPeakCurvList) {
            mPeakCurvList.clear();
            mPeakCurvList = null;
        }
        if (null != mCheckPeakList) {
            mCheckPeakList.clear();
            mCheckPeakList = null;
        }
    }


    private void clearAllList() {
        clearAMPList();
        clearRawList();
        clearMeltList();
        clearPeakList();
        if(null != localList){
            localList.clear();
            localList = null;
        }
        if(null != mShowCurveList){
            mShowCurveList.clear();
            mShowCurveList = null;
        }
        if (null != wellResults) {
            wellResults.clear();
            wellResults = null;
        }
        if (null != checkWellResult) {
            checkWellResult.clear();
            checkWellResult = null;
        }

        if (null != mExlResults) {
            mExlResults.clear();
            mExlResults = null;
        }

        if (null != mCheckExlResults) {
            mCheckExlResults.clear();
            mCheckExlResults = null;
        }

        if(null != mWellInfos){
            mWellInfos.clear();
            mWellInfos = null;
        }

        if(null != mOriginRaw){
            mOriginRaw.clear();
            mOriginRaw = null;
        }

        if(null != mOriginMelt){
            mOriginMelt.clear();
            mOriginMelt = null;
        }

        if(null != mOriginPeak){
            mOriginPeak.clear();
            mOriginPeak = null;
        }

        if(null != mOriginAMP){
            mOriginAMP.clear();
            mOriginAMP = null;
        }
        if(configTargets != null){
            configTargets.clear();
            configTargets = null;
        }
    }


    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()) {
            case MsgType.MSG_FAIL:
                hideLoadingDialog();
                String failMsg = (String)messageEvent.getData();
                Toast.makeText(getContext(),failMsg,Toast.LENGTH_LONG).show();
                break;
            case MsgType.MSG_SHOW_ALL_CHANEL_RESULT:
            case MsgType.MSG_SHOW_FAM_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_HEX_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_ROX_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_CY5_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_QUASAR_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_ATTO_CHANNEL_CURVE:
                switch (currentView) {
                    case 1:
                        //raw原始数据
                        getAllRawCurve();
                        drawRawCurve();
                        break;
                    case 2:
                        //扩增数据
                        getAllAPMCurve();
                        drawAMPCurve();
                        break;
                    case 3:
                        //熔解数据
                        getAllMelturve();
                        drawMeltCurve();
                        break;
                    case 4:
                        //峰值曲线
                        getAllPeakCurve();
                        drawPeakCurve();
                        break;
                    case 5:
                        getShowExlResults();
                        if (null != resultAdapter) {
                            Collections.sort(mCheckExlResults);
                            Collections.reverse(mCheckExlResults);
                            resultAdapter.setResultList(mCheckExlResults);
                            resultAdapter.notifyDataSetChanged();
                        }
                        break;
                }
                break;

            case MsgType.MSG_UPDATE_EXPERIMENT_EXCEL:
                if (null != resultAdapter) {
                    Collections.sort(mCheckExlResults);
                    Collections.reverse(mCheckExlResults);
                    resultAdapter.setResultList(mCheckExlResults);
                    resultAdapter.notifyDataSetChanged();
                }
                break;

            case MsgType.MSG_SHOW_EX_SECOND_RESULT:
                //二次分析结果
                boolean sec_result = (Boolean)messageEvent.getData();
                if(sec_result){
                    hideLoadingDialog();
                    Toast.makeText(getContext().getApplicationContext(),
                            getResources().getString(R.string.result_ex_toast_second_analyse_done),Toast.LENGTH_LONG).show();
                    mRawCurveList.clear();
                    mRawCurveList.addAll(SLANManager.getInstance().getRawCurve());
                    mAMPCurvList.clear();
                    mAMPCurvList.addAll(SLANManager.getInstance().getAMPCurve());
                    mMeltCurvList.clear();
                    mMeltCurvList.addAll(SLANManager.getInstance().getMeltCurve());
                    mPeakCurvList.clear();
                    mPeakCurvList.addAll(SLANManager.getInstance().getPeakCurve());
                    mExlResults.clear();
                    mExlResults.addAll(SLANManager.getInstance().getExlResults());
                    setOnrignCurveList();
                    //将试管信息同步到二次分析的结果
                    if(null != mExlResults && mExlResults.size()>0){
                        for(SLANExlResult exlResult :mExlResults){
                            if(null != mWellInfos && mWellInfos.size()>0){
                                for (int i = 0; i < mWellInfos.size(); i++){
                                    WellInfo wellInfo = mWellInfos.get(i);
                                    if(wellInfo.getWellName().equals(exlResult.getWellNum())){
                                        exlResult.setIdCard(wellInfo.getSampleName());
                                        exlResult.setRegentCode(wellInfo.getReagentCode());
                                    }
                                }
                            }
                        }
                    }
                    Experiment experiment = SLANManager.getInstance().getExperiment();
                    getSLANExperimentParamter(experiment);
                    switch (currentView) {
                        case 1:
                            getAllRawCurve();
                            drawRawCurve();
                            break;
                        case 2:
                            getAllAPMCurve();
                            drawAMPCurve();
                            break;
                        case 3:
                            getAllMelturve();
                            drawMeltCurve();
                            break;
                        case 4:
                            getAllPeakCurve();
                            drawPeakCurve();
                            break;
                        case 5:
                            getShowExlResults();
                            if (null != resultAdapter) {
                                Collections.sort(mCheckExlResults);
                                Collections.reverse(mCheckExlResults);
                                resultAdapter.setResultList(mCheckExlResults);
                                resultAdapter.notifyDataSetChanged();
                            }
                            break;

                    }

                    showUpdateDbDialog();
                }else{
                    //二次分析失败
                    hideLoadingDialog();
                    Toast.makeText(getContext().getApplicationContext(),
                            getResources().getString(R.string.toast_second_analyse_fail),Toast.LENGTH_LONG).show();
                }

                break;
        }
    }

    /**
     * 二次分析后，获得
     */

    private LimitLine initLimitLine(double threshold,String threshold_name,int number) {
        LimitLine limitLine = new LimitLine((float) threshold, threshold_name);
        limitLine.setLineWidth(2f);  //设置线宽
        limitLine.setTextSize(10f);   //设置限制线上label字体大小
        switch (number){
            case 0:
                limitLine.setLineColor(PcrValue.colorline[0]);
                break;
            case 1:
                limitLine.setLineColor(PcrValue.colorline[1]);
                break;
            case 2:
                limitLine.setLineColor(PcrValue.colorline[2]);
                break;
            case 3:
                limitLine.setLineColor(PcrValue.colorline[3]);
                break;
            case 4:
                limitLine.setLineColor(PcrValue.colorline[4]);
                break;
            case 5:
                limitLine.setLineColor(PcrValue.colorline[5]);
                break;
        }
        limitLine.setTextColor(R.color.blue);  //设置限制线上label字体的颜色
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);//标签位置
        limitLine.disableDashedLine();
        return limitLine;
    }




    public void drawchart(List<SLANCurveList> curve_list, LineChart lineChart, String describe) {
        if (curve_list == null) {
            curve_list = new ArrayList<>();
        }
        if (lineChart != null) {
            Description description = new Description();
            description.setText(describe);
            lineChart.setDescription(description);
            lineChart.setDrawBorders(true);
            //设置chart边框线颜色
            lineChart.setBorderColor(Color.BLACK);
            //设置chart边框线宽度
            lineChart.setBorderWidth(1f);

            List<ILineDataSet> lineDataSets = new ArrayList<>();
            int my_color = PcrValue.colorline[0];
            for (int i = 0; i < curve_list.size(); i++) {
                //entries列表
                LineDataSet set1 = new LineDataSet(curve_list.get(i).getCurve_data(), curve_list.get(i).getCurve_hole());
                //设置每个树状图的颜色，一共有两个方法setColors和setColor
                set1.setCircleSize(1f);
                if (-1 != curve_list.get(i).getChannelNo()) {
                    if (curve_list.get(i).getChannelNo()==1) {
                        my_color = PcrValue.colorline[0];//蓝色
                    } else if (curve_list.get(i).getChannelNo() == 2) {
                        my_color = PcrValue.colorline[1];//绿色
                    } else if (curve_list.get(i).getChannelNo() == 3) {
                        my_color = PcrValue.colorline[2];//黄色
                    } else if (curve_list.get(i).getChannelNo() ==4) {
                        my_color = PcrValue.colorline[3];//红色
                    }else if (curve_list.get(i).getChannelNo() == 5) {
                        my_color = PcrValue.colorline[4];//红色
                    }else if (curve_list.get(i).getChannelNo() == 6) {
                        my_color = PcrValue.colorline[5];//红色
                    }
                }

                set1.setColor(my_color);
                set1.setCircleColor(my_color);
                //线模式为圆滑曲线(默认折线)
                // 性能最优：LINEAR 线性，不要 CUBIC_BEZIER（贝塞尔计算极耗CPU）
                set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                set1.setDrawValues(false);// 不显示具体值
                set1.setDrawCircleHole(true);
                //设置数据点值得字的大小
                //设置图例线的样式
                set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
                //设置图例的文字大小
                set1.setFormSize(15.f);
                lineDataSets.add(set1);
                //controlRn = curve_list.get(0).getControlRn();
            }
            //Step4: List 的数据放在 BarData 里
            LineData lineData = new LineData(lineDataSets);
            //设置树状图上的字的字号
            lineData.setValueTextSize(10);
            lineData.setDrawValues(false);// 不显示具体值
            lineChart.setData(lineData);
            Legend legend = lineChart.getLegend();
            legend.setTextSize(10f);
            legend.setEnabled(false);
            YAxis rightYAxis = lineChart.getAxisRight();
            rightYAxis.setEnabled(false); //右侧Y轴不显示
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.removeAllLimitLines();
            leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            leftAxis.setDrawGridLines(true);

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线
            //      xAxis.setLabelCount(curve_list.size(), false);



            lineChart.post(new Runnable() {
                @Override
                public void run() {
                    markView = new LineChartMarkView(getContext());
                    lineChart.setMarker(markView);
                }
            });
            lineChart.invalidate();
        }
    }

    public void drawchart(List<SLANCurveList> curve_list, LineChart lineChart, String describe,
                          int type) {
        if (curve_list == null) {
            curve_list = new ArrayList<>();
        }
        if (lineChart != null) {
            Description description = new Description();
            description.setText(describe);
            lineChart.setDescription(description);
            lineChart.setDrawBorders(true);
            //设置chart边框线颜色
            lineChart.setBorderColor(Color.BLACK);
            //设置chart边框线宽度
            lineChart.setBorderWidth(1f);

            List<ILineDataSet> lineDataSets = new ArrayList<>();
            int my_color = PcrValue.colorline[0];
            for (int i = 0; i < curve_list.size(); i++) {
                //entries列表
                LineDataSet set1 = new LineDataSet(curve_list.get(i).getCurve_data(), curve_list.get(i).getCurve_hole());
                //设置每个树状图的颜色，一共有两个方法setColors和setColor
                set1.setCircleSize(1f);
                if (-1 != curve_list.get(i).getChannelNo()) {
                    if (curve_list.get(i).getChannelNo() == 1) {
                        my_color = PcrValue.colorline[0];//蓝色
                    } else if (curve_list.get(i).getChannelNo() == 2) {
                        my_color = PcrValue.colorline[1];//绿色
                    } else if (curve_list.get(i).getChannelNo()== 3) {
                        my_color = PcrValue.colorline[2];//黄色
                    } else if (curve_list.get(i).getChannelNo() == 4) {
                        my_color = PcrValue.colorline[3];//红色
                    }else if (curve_list.get(i).getChannelNo() == 5) {
                        my_color = PcrValue.colorline[4];//红色
                    }else if (curve_list.get(i).getChannelNo() == 6) {
                        my_color = PcrValue.colorline[5];//红色
                    }
                }

                set1.setColor(my_color);
                set1.setCircleColor(my_color);
                //线模式为圆滑曲线(默认折线)
                set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                set1.setDrawValues(false);// 不显示具体值
                set1.setDrawCircleHole(true);
                //设置数据点值得字的大小
                //设置图例线的样式
                set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
                //设置图例的文字大小
                set1.setFormSize(15.f);
                lineDataSets.add(set1);
            }
            //Step4: List 的数据放在 BarData 里
            LineData lineData = new LineData(lineDataSets);
            //设置树状图上的字的字号
            lineData.setValueTextSize(10);
            lineData.setDrawValues(false);// 不显示具体值
            lineChart.setData(lineData);
            Legend legend = lineChart.getLegend();
            legend.setTextSize(10f);
            legend.setEnabled(false);
            YAxis rightYAxis = lineChart.getAxisRight();
            rightYAxis.setEnabled(false); //右侧Y轴不显示
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.removeAllLimitLines();
            leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
            leftAxis.setDrawGridLines(true);

            if(isFamThreshold && isFAMCheck){
                LimitLine limitLine = initLimitLine(famThreshold,"famThreshold",0);
                leftAxis.addLimitLine(limitLine);
            }
            if(isHexThreshold && isHEXCheck){
                LimitLine limitLine = initLimitLine(hexThreshold,"hexThreshold",1);
                leftAxis.addLimitLine(limitLine);
            }
            if(isRoxThreshold && isROXCheck){
                LimitLine limitLine = initLimitLine(roxThreshold,"roxThreshold",2);
                leftAxis.addLimitLine(limitLine);
            }
            if(iscy5Threshold && isCY5Check){
                LimitLine limitLine = initLimitLine(cy5Threshold,"cy5Threshold",3);
                leftAxis.addLimitLine(limitLine);
            }
            if(isQuasarThreshold && isQuaSarCheck){
                LimitLine limitLine = initLimitLine(quasarThreshold,"quasarThreshold",4);
                leftAxis.addLimitLine(limitLine);
            }
            if(isAttoThreshold && isAttoCheck){
                LimitLine limitLine = initLimitLine(attoThreshold,"attoThreshold",5);
                leftAxis.addLimitLine(limitLine);
            }


            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线
            //   xAxis.setLabelCount(curve_list.size(), false);

            // Log.i("AAA","curve_list.size()" + curve_list.size());
            lineChart.post(new Runnable() {
                @Override
                public void run() {
                    markView = new LineChartMarkView(getContext());
                    lineChart.setMarker(markView);
                }
            });

            lineChart.invalidate();
        }
    }


    /**
     * 清除图表
     **/
    public void clearChart() {
        if (null != binding.resultCurveChart) {
            binding.resultCurveChart.clear();
            binding.resultCurveChart.notifyDataSetChanged();
            binding.resultCurveChart.invalidate();
        }

    }

    /////////////////////////////////////////////////////////原始数据///////////////////////////////////////////
    private void drawRawCurve() {
        clearChart();
        XAxis xAxis = binding.resultCurveChart.getXAxis();
        YAxis yAxis = binding.resultCurveChart.getAxisLeft();
        if(isScaleXRaw){
            xAxis.setAxisMaximum(max_Raw_X);
            xAxis.setAxisMinimum(min_Raw_X);
            yAxis.setAxisMaximum(max_Raw_Y);
            yAxis.setAxisMinimum(min_Raw_Y);
        }else{
            yAxis.resetAxisMinimum();
            yAxis.resetAxisMaximum();
            xAxis.resetAxisMaximum();
            xAxis.resetAxisMinimum();
        }
        drawchart(mCheckRawList, binding.resultCurveChart, "RawCurve");
    }


    private void getAllRawCurve() {
        if (null != mRawCurveList) {
            mCheckRawList.clear();
            if (mRawCurveList.size() > 0) {
                for (SLANCurveList curveList : mRawCurveList) {
                    if(-1 != curveList.getChannelNo()){
                     //   Log.i(TAG,"==getChannelNo====" + curveList.getChannelNo());
                        if(curveList.getChannelNo()== 1) {
                            if (isFAMCheck) {
                                mCheckRawList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 2) {
                            if (isHEXCheck) {
                                mCheckRawList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 3) {
                            if (isROXCheck) {
                                mCheckRawList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 4) {
                            if (isCY5Check) {
                                mCheckRawList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 5) {
                            if (isQuaSarCheck) {
                                mCheckRawList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 6) {
                            if (isAttoCheck) {
                                mCheckRawList.add(curveList);
                            }
                        }
                    }
                }
            }

        }
    }



    /*******************************获取所有的扩增曲线值**********************************/
    private void getAllAPMCurve() {
        if (null != mAMPCurvList) {
            mCheckAMPList.clear();
            if (mAMPCurvList.size() > 0) {
                for (SLANCurveList curveList : mAMPCurvList) {
                    if (curveList.getChannelNo() != -1){
                        if(curveList.getChannelNo() == 1){
                            if(isFAMCheck){
                                mCheckAMPList.add(curveList);
                            }
                        }else if(curveList.getChannelNo() ==2) {
                            if(isHEXCheck){
                                mCheckAMPList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 3) {
                            if(isROXCheck){
                                mCheckAMPList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 4) {
                            if(isCY5Check){
                                mCheckAMPList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 5) {
                            if(isQuaSarCheck){
                                mCheckAMPList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 6) {
                            if(isAttoCheck){
                                mCheckAMPList.add(curveList);
                            }
                        }
                    }
                }

            }
        }

    }

    //根据选择的通道显示扩增曲线/

    private void drawAMPCurve() {
        clearChart();
        if(!isFamThreshold&& !isHexThreshold && !isRoxThreshold && !iscy5Threshold
                && !isQuasarThreshold && !isAttoThreshold){
            drawchart(mCheckAMPList, binding.resultCurveChart, "AMPCurve");
        }else{
            XAxis xAxis = binding.resultCurveChart.getXAxis();
            YAxis yAxis = binding.resultCurveChart.getAxisLeft();
            if(isScaleXAmp){
                //坐标缩放
                xAxis.setAxisMaximum(max_Amp_X);
                xAxis.setAxisMinimum(min_Amp_X);
                yAxis.setAxisMaximum(max_Amp_Y);
                yAxis.setAxisMinimum(min_Amp_Y);
            }else{
                yAxis.resetAxisMinimum();
                yAxis.resetAxisMaximum();
                xAxis.resetAxisMaximum();
                xAxis.resetAxisMinimum();
            }
            drawchart(mCheckAMPList, binding.resultCurveChart, "AMPCurve",2);
        }
    }


    /*******************************获取所有的熔解曲线值******Melt***************************/
    private void getAllMelturve() {
        Log.i("SLAN", "getAllMelturve" + mMeltCurvList.size());
        if (null != mMeltCurvList) {
            mCheckMeltList.clear();
            if (mMeltCurvList.size() > 0) {
                for (SLANCurveList curveList : mMeltCurvList) {
                    if(curveList.getChannelNo() != -1){
                        if(curveList.getChannelNo() == 1){
                            if(isFAMCheck) {
                                mCheckMeltList.add(curveList);
                            }
                        } else if(curveList.getChannelNo() == 2) {
                            if(isHEXCheck) {
                                mCheckMeltList.add(curveList);
                            }

                        } else if(curveList.getChannelNo() == 3) {
                            if(isROXCheck) {
                                mCheckMeltList.add(curveList);
                            }
                        } else if(curveList.getChannelNo() == 4) {
                            if(isCY5Check) {
                                mCheckMeltList.add(curveList);
                            }
                        }else if(curveList.getChannelNo() == 5){
                            if(isQuaSarCheck) {
                                mCheckMeltList.add(curveList);
                            }
                        }else if(curveList.getChannelNo() == 6){
                            if(isAttoCheck) {
                                mCheckMeltList.add(curveList);
                            }
                        }
                    }
                }
            }

        }

    }

    //根据选择的通道显示扩增曲线/

    private void drawMeltCurve() {
        clearChart();
        XAxis xAxis = binding.resultCurveChart.getXAxis();
        YAxis yAxis = binding.resultCurveChart.getAxisLeft();
        if(isScaleXMelt){
            xAxis.setAxisMaximum(max_Melt_X);
            xAxis.setAxisMinimum(min_Melt_X);
            yAxis.setAxisMaximum(max_Melt_Y);
            yAxis.setAxisMinimum(min_Melt_Y);
        }else{
            yAxis.resetAxisMaximum();
            yAxis.resetAxisMinimum();
            xAxis.resetAxisMaximum();
            xAxis.resetAxisMinimum();
        }
        drawchart(mCheckMeltList, binding.resultCurveChart, "MeltCurve");
    }
    /**************************************************获取峰值数据*************************************************/


    private void getAllPeakCurve() {
        if (null != mPeakCurvList) {
            mCheckPeakList.clear();
            if (mPeakCurvList.size() > 0) {
                for (SLANCurveList curveList : mPeakCurvList) {
                    if(null != curveList.getCurve_hole()){
                        if (curveList.getChannelNo() == 1) {
                            if (isFAMCheck) {
                                mCheckPeakList.add(curveList);
                            }
                        } else if (curveList.getChannelNo() == 2) {
                            if (isHEXCheck) {
                                mCheckPeakList.add(curveList);
                            }
                        } else if (curveList.getChannelNo() == 3) {
                            if (isROXCheck) {
                                mCheckPeakList.add(curveList);
                            }
                        } else if (curveList.getChannelNo() == 4) {
                            if (isCY5Check) {
                                mCheckPeakList.add(curveList);
                            }
                        }else if (curveList.getChannelNo() == 5) {
                            if (isQuaSarCheck) {
                                mCheckPeakList.add(curveList);
                            }
                        } else if (curveList.getChannelNo() == 6) {
                            if (isAttoCheck) {
                                mCheckPeakList.add(curveList);
                            }
                        }
                    }

                }

            }
        }
    }

    //根据选择的通道显示扩增曲线/

    private void drawPeakCurve() {
        clearChart();
        XAxis xAxis = binding.resultCurveChart.getXAxis();
        YAxis yAxis = binding.resultCurveChart.getAxisLeft();
        if(isScaleXPeak){
            xAxis.setAxisMaximum(max_Peak_X);
            xAxis.setAxisMinimum(min_Peak_X);
            yAxis.setAxisMinimum(min_Peak_Y);
            yAxis.setAxisMaximum(max_Peak_Y);
        }else{
            xAxis.resetAxisMinimum();
            xAxis.resetAxisMaximum();
            yAxis.resetAxisMaximum();
            yAxis.resetAxisMinimum();
        }
        drawchart(mCheckPeakList, binding.resultCurveChart, "PeakCurve");
    }



    /**
     * @param check
     * @param textView
     */
    private void setHodeState(boolean check, TextView textView) {
        textView.setClickable(true);
        if (!check) {
            textView.setBackgroundResource(R.drawable.shape_oval_gray_bg);
            textView.setTextColor(getResources().getColor(R.color.blue));
        } else {
            textView.setBackgroundResource(R.drawable.shape_oval_blue_bg);
            textView.setTextColor(getResources().getColor(R.color.white));
        }
    }






    ///////////////////////////////////查询一次检测所有孔位记录///////////////////////////////////////////////////

    private class ResultSearchAsyncTask extends AsyncTask<Void, Integer, List<SLANWellResult>> {

//        private WeakReference<BiowayExDetailActivity> weakfrag;
        private QueryCondition queryCondition = null;

        public ResultSearchAsyncTask(Activity activity, QueryCondition queryCondition) {
//            weakfrag = new WeakReference<BiowayExDetailActivity>(activity);
            this.queryCondition = queryCondition;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            if (null != wellResults) {
                wellResults.clear();
            }

        }

        //后台进程的执行
        @Override
        protected List<SLANWellResult> doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
//            final BiowayExDetailActivity mActivity = weakfrag.get();
            Log.i("Search","queryCondition getIdcardNum" + queryCondition.getIdcardNum());
            Log.i("Search","queryCondition getTestStartTime" + queryCondition.getTestStartTime());
            Log.i("Search","queryCondition getTestItem" + queryCondition.getTestItem());
            Log.i("Search","queryCondition getRegentCode" + queryCondition.getRegentCode());
            wellResults = LiteORMCtrlMgr.getInstance().searchSLANWellResult(queryCondition);
            Log.i("Search","queryCondition wellResults" + wellResults.size());
            return wellResults;

        }

        @Override
        protected void onPostExecute(List<SLANWellResult> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
//            final BiowayExDetailActivity mActvity = weakfrag.get();
            if (null !=wellResults) {
                //进行解析
                if (wellResults.size() > 0) {
                    Log.i("Search", "ResultSearchAsyncTask result" + wellResults.size());
                    parseWellModelResults();
                } else {
                    Toast.makeText(getContext(),getContext().getResources().getString(R.string.toast_no_data),Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(getContext()
                        ,getContext().getResources().getString(R.string.toast_no_data),Toast.LENGTH_LONG).show();
            }
            cancelTask();

        }
    }

    private void cancelTask() {
        if (null != searchAsyncTask) {
            searchAsyncTask.cancel(true);
            searchAsyncTask = null;
        }
        hideLoadingDialog();
    }


    private void excuteTask(QueryCondition queryCondition) {
        if (null == searchAsyncTask) {
            searchAsyncTask = new ResultSearchAsyncTask(getActivity(), queryCondition);
            searchAsyncTask.execute();
        }
    }

    private LoadingDialog loadingDialog ;
    public void showLoadingDialog(String message){
        if(null == loadingDialog){
            loadingDialog = new LoadingDialog(getContext(), message);
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
//    /*
//     * 隐藏提示加载
//     */
//    public void hideProgressDialog() {
//        if (loadingDialog != null && loadingDialog.isShowing()) {
//            loadingDialog.dismiss();
//            loadingDialog = null;
//        }
//    }
/************************************************查询数据库解析孔位********************************************************/

    /**
     * 1.解析从数据库中查询的一次实验8个孔位的结果
     * 显示当前结果又几个孔位是有数据的（选中的状态）
     * 2.获取选中的所有的孔位结果
     */
    private void parseWellModelResults() {
        //最初的孔状态
        resetAllWellSelection();
        checkWellState();
        setCheckedWellResults();
        showWellResult2();
    }

    /**
     * 1.获取数据根据数据结果设置所有的孔位的选定状态
     * 提取所有检测的信息,选中状态的孔位信息List<SLANWellResult> checkWellResult
     */
    private void checkWellState() {
        mWellInfos.clear();
        if (null != wellResults && wellResults.size() > 0){
            for (SLANWellResult wellResultModel : wellResults) {
                Log.i(TAG,"==wellResultModel.getWellName()=="+ wellResultModel.getWellName());
                if (wellResultModel.getWellName().equals("A1")) {
                    isA1Check = true;
                    isA1Enable = true;
                    binding.sampleEightA1Tv.setEnabled(isA1Enable);
                    setHodeState(isA1Check, binding.sampleEightA1Tv);
                    isSampleEightSelected = true;
                } else if (wellResultModel.getWellName().equals("A2")) {
                    isA2Check = true;
                    isA2Enable = true;
                    binding.sampleSevenA2Tv.setEnabled(isA2Enable);
                    setHodeState(isA2Check, binding.sampleSevenA2Tv);
                    isSampleSevenSelected = true;
                } else if (wellResultModel.getWellName().equals("A3")) {
                    isA3Check = true;
                    isA3Enable = true;
                    binding.sampleSixA3Tv.setEnabled(isA3Enable);
                    setHodeState(isA3Check, binding.sampleSixA3Tv);
                    isSampleSixSelected = true;
                } else if (wellResultModel.getWellName().equals("A4")) {
                    isA4Check = true;
                    isA4Enable = true;
                    binding.sampleFiveA4Tv.setEnabled(isA4Enable);
                    setHodeState(isA4Check, binding.sampleFiveA4Tv);
                    isSampleFiveSelected = true;
                } else if (wellResultModel.getWellName().equals("A5")) {
                    isA5Check = true;
                    isA5Enable = true;
                    binding.sampleFourA5Tv.setEnabled(isA5Enable);
                    setHodeState(isA5Check, binding.sampleFourA5Tv);
                    isSampleFourSelected = true;
                } else if (wellResultModel.getWellName().equals("A6")) {
                    isA6Check = true;
                    isA6Enable= true;
                    binding.sampleThreeA6Tv.setEnabled(isA6Enable);
                    setHodeState(isA6Check, binding.sampleThreeA6Tv);
                    isSampleThreeSelected = true;
                } else if (wellResultModel.getWellName().equals("A7")) {
                    isA7Check = true;
                    isA7Enable = true;
                    binding.sampleTwoA7Tv.setEnabled(isA7Enable);
                    setHodeState(isA7Check, binding.sampleTwoA7Tv);
                    isSampleTwoSelected = true;
                } else if (wellResultModel.getWellName().equals("A8")) {
                    isA8Check = true;
                    isA8Enable = true;
                    binding.sampleOneA8Tv.setEnabled(isA8Enable);
                    setHodeState(isA8Check, binding.sampleOneA8Tv);
                    isSampleOneSelected = true;
                }else if (wellResultModel.getWellName().equals("B1")) {
                    isB1Enable = true;
                    isB1Check = true;
                    binding.sampleEightB1Tv.setEnabled(isB1Enable);
                    setHodeState(isB1Check, binding.sampleEightB1Tv);
                    isSampleEightSelected = true;
                }else if (wellResultModel.getWellName().equals("B2")) {
                    isB2Enable = true;
                    isB2Check = true;
                    binding.sampleSevenB2Tv.setEnabled(isB2Enable);
                    setHodeState(isB2Check, binding.sampleSevenB2Tv);
                    isSampleSevenSelected = true;
                }else if (wellResultModel.getWellName().equals("B3")) {
                    isB3Enable = true;
                    isB3Check = true;
                    binding.sampleSixB3Tv.setEnabled(isB3Enable);
                    setHodeState(isB3Check, binding.sampleSixB3Tv);
                    isSampleSixSelected = true;
                }else if (wellResultModel.getWellName().equals("B4")) {
                    isB4Enable = true;
                    isB4Check = true;
                    binding.sampleFiveB4Tv.setEnabled(isB4Enable);
                    setHodeState(isB4Check, binding.sampleFiveB4Tv);
                    isSampleFiveSelected = true;
                }else if (wellResultModel.getWellName().equals("B5")) {
                    isB5Enable = true;
                    isB5Check = true;
                    binding.sampleFourB5Tv.setEnabled(isB5Enable);
                    setHodeState(isB5Check, binding.sampleFourB5Tv);
                    isSampleFourSelected = true;
                }else if (wellResultModel.getWellName().equals("B6")) {
                    isB6Enable = true;
                    isB6Check = true;
                    binding.sampleThreeB6Tv.setEnabled(isB6Enable);
                    setHodeState(isB6Check, binding.sampleThreeB6Tv);
                    isSampleThreeSelected = true;
                }else if (wellResultModel.getWellName().equals("B7")) {
                    isB7Enable = true;
                    isB7Check = true;
                    binding.sampleTwoB7Tv.setEnabled(isB7Enable);
                    setHodeState(isB7Check, binding.sampleTwoB7Tv);
                    isSampleTwoSelected  = true;
                }else if (wellResultModel.getWellName().equals("B8")) {
                    isB8Enable = true;
                    isB8Check = true;
                    binding.sampleOneB8Tv.setEnabled(isB8Enable);
                    setHodeState(isB8Check, binding.sampleOneB8Tv);
                    isSampleOneSelected  = true;
                }

                WellInfo wellInfo =
                        new WellInfo(wellResultModel.getWellName(),wellResultModel.getSampleName(),
                                wellResultModel.getReagentCode());
                if(!mWellInfos.contains(wellInfo)){
                    mWellInfos.add(wellInfo);
                }
            }
        }


        if (isSampleOneSelected) {
            binding.resultSampleOneLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
        } else {
            binding.resultSampleOneLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
        }

        if(isSampleTwoSelected){
            binding.resultSampleTwoLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
        } else {
            binding.resultSampleTwoLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
        }
        if(isSampleThreeSelected){
            binding.resultSampleThreeLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
        } else {
            binding.resultSampleThreeLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
        }
        if(isSampleFourSelected){
            binding.resultSampleFourLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_pressed_bg);
        } else {
            binding.resultSampleFourLl.setBackgroundResource(R.drawable.shape_rect_blue_stroke_12_normal_bg);
        }
    }



    private void setCheckedWellResults() {
        checkWellResult.clear();
        if (null != wellResults && wellResults.size() > 0) {
            for (SLANWellResult wellResultModel : wellResults) {
                if (isA1Check && wellResultModel.getWellName().equals("A1")) {
                    checkWellResult.add(wellResultModel);
                } else if (isA2Check && wellResultModel.getWellName().equals("A2")) {
                    checkWellResult.add(wellResultModel);

                } else if (isA3Check && wellResultModel.getWellName().equals("A3")) {

                    checkWellResult.add(wellResultModel);

                } else if (isA4Check && wellResultModel.getWellName().equals("A4")) {
                    checkWellResult.add(wellResultModel);

                } else if (isA5Check && wellResultModel.getWellName().equals("A5")) {
                    checkWellResult.add(wellResultModel);

                } else if (isA6Check && wellResultModel.getWellName().equals("A6")) {
                    checkWellResult.add(wellResultModel);

                } else if (isA7Check && wellResultModel.getWellName().equals("A7")) {
                    checkWellResult.add(wellResultModel);

                } else if (isA8Check && wellResultModel.getWellName().equals("A8")) {
                    checkWellResult.add(wellResultModel);
                }else if (isB1Check && wellResultModel.getWellName().equals("B1")) {
                    checkWellResult.add(wellResultModel);

                } else if (isB2Check && wellResultModel.getWellName().equals("B2")) {
                    checkWellResult.add(wellResultModel);
                }else if (isB3Check && wellResultModel.getWellName().equals("B3")) {
                    checkWellResult.add(wellResultModel);

                }else if (isB4Check && wellResultModel.getWellName().equals("B4")) {
                    checkWellResult.add(wellResultModel);
                }else if (isB5Check && wellResultModel.getWellName().equals("B5")) {
                    checkWellResult.add(wellResultModel);

                }else if (isB6Check && wellResultModel.getWellName().equals("B6")) {
                    checkWellResult.add(wellResultModel);
                }else if (isB7Check && wellResultModel.getWellName().equals("B7")) {
                    checkWellResult.add(wellResultModel);

                }else if (isB8Check && wellResultModel.getWellName().equals("B8")) {
                    checkWellResult.add(wellResultModel);
                }
            }
        }

    }

    private void showWellResult2() {
        showLoadingDialog(getResources().getString(R.string.result_ex_detail_dialog_drawing));
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = getShowWellResult();
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setChannelState();
                            drawchart(mCheckRawList, binding.resultCurveChart, "RawCurve");
                            hideLoadingDialog();
                        }
                    });
                }
            }
        }).start();
    }


    /**
     * 解析所有选中的孔位，获取所有曲线的数值
     */
    private boolean getShowWellResult(){
        mAMPCurvList.clear();
        mRawCurveList.clear();
        mMeltCurvList.clear();
        mPeakCurvList.clear();
        mExlResults.clear();
        for (SLANWellResult wellResultModel : checkWellResult) {
            parseWellResult(wellResultModel);
        }

        if (null != mRawCurveList && mRawCurveList.size() > 0) {
            for (SLANCurveList curveList : mRawCurveList) {
                if (-1 != curveList.getChannelNo()) {
                    if(curveList.getChannelNo() == 1){
                        isFAMCheck = true;
                    } else if (curveList.getChannelNo() == 2) {
                        isHEXCheck = true;
                    } else if (curveList.getChannelNo() == 3) {
                        isROXCheck = true;
                    } else if (curveList.getChannelNo() == 4) {
                        isCY5Check = true;
                    }else if (curveList.getChannelNo() == 5) {
                        isQuaSarCheck = true;
                    } else if (curveList.getChannelNo() == 6) {
                        isAttoCheck = true;
                    }
                }
            }
        }

        setOnrignCurveList();
        mCheckRawList.clear();
        mCheckRawList.addAll(mRawCurveList);
        Log.i(TAG,"===famcheck===" + isFAMCheck);
        Log.i(TAG,"===hexcheck===" + isHEXCheck);
        Log.i(TAG,"===roxcheck===" + isROXCheck);
        Log.i(TAG,"===cy5check===" + isCY5Check);
        setChannelState();

        return true;
    }


    private void setOnrignCurveList(){
        mOriginRaw.clear();
        mOriginRaw.addAll(mRawCurveList);
        mOriginAMP.clear();
        mOriginAMP.addAll(mAMPCurvList);
        mOriginMelt.clear();
        mOriginMelt.addAll(mMeltCurvList);
        mOriginPeak.clear();
        mOriginPeak.addAll(mPeakCurvList);
        mOriginExResults.clear();
        mOriginExResults.addAll(mExlResults);
    }



    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    private void setCheckWellRawCurve() {

        if (null != mOriginRaw && mOriginRaw.size() > 0) {
            mRawCurveList.clear();
            for (SLANCurveList slanCurveList : mOriginRaw) {
                if(null != slanCurveList.getWellNum()){
                    if (isA1Check && slanCurveList.getWellNum().equals("A1")) {
                        mRawCurveList.add(slanCurveList);
                    } else if (isA2Check && slanCurveList.getWellNum().equals("A2")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isA3Check && slanCurveList.getWellNum().equals("A3")) {
                        mRawCurveList.add(slanCurveList);
                    } else if (isA4Check && slanCurveList.getWellNum().equals("A4")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isA5Check && slanCurveList.getWellNum().equals("A5")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isA6Check && slanCurveList.getWellNum().equals("A6")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isA7Check && slanCurveList.getWellNum().equals("A7")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isA8Check && slanCurveList.getWellNum().equals("A8")) {
                        mRawCurveList.add(slanCurveList);
                    }else if(isB1Check && slanCurveList.getWellNum().equals("B1")){
                        mRawCurveList.add(slanCurveList);
                    }else if(isB2Check && slanCurveList.getWellNum().equals("B2")){
                        mRawCurveList.add(slanCurveList);
                    }else if(isB3Check && slanCurveList.getWellNum().equals("B3")){
                        mRawCurveList.add(slanCurveList);
                    }else if(isB4Check && slanCurveList.getWellNum().equals("B4")){
                        mRawCurveList.add(slanCurveList);
                    }else if(isB5Check && slanCurveList.getWellNum().equals("B5")){
                        mRawCurveList.add(slanCurveList);
                    }else if(isB6Check && slanCurveList.getWellNum().equals("B6")){
                        mRawCurveList.add(slanCurveList);
                    }else if(isB7Check && slanCurveList.getWellNum().equals("B7")){
                        mRawCurveList.add(slanCurveList);
                    }else if(isB8Check && slanCurveList.getWellNum().equals("B8")){
                        mRawCurveList.add(slanCurveList);
                    }
                }

            }
        }
        Log.i("AIO", "updateWellResult解析结果 mRawCurvList" + mRawCurveList.size());
    }

    private void setCheckWellAMPCurve() {
        if (null != mOriginAMP && mOriginAMP.size() > 0) {
            mAMPCurvList.clear();
            for(SLANCurveList slanCurveList :mOriginAMP){
                if(null != slanCurveList.getWellNum()){
                    if (isA1Check && slanCurveList.getWellNum().equals("A1")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isA2Check && slanCurveList.getWellNum().equals("A2")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isA3Check && slanCurveList.getWellNum().equals("A3")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isA4Check && slanCurveList.getWellNum().equals("A4")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isA5Check && slanCurveList.getWellNum().equals("A5")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isA6Check && slanCurveList.getWellNum().equals("A6")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isA7Check && slanCurveList.getWellNum().equals("A7")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isA8Check && slanCurveList.getWellNum().equals("A8")) {
                        mAMPCurvList.add(slanCurveList);
                    }else if (isB1Check && slanCurveList.getWellNum().equals("B1")) {
                        mAMPCurvList.add(slanCurveList);
                    }else if (isB2Check && slanCurveList.getWellNum().equals("B2")) {
                        mAMPCurvList.add(slanCurveList);
                    }else if(isB3Check && slanCurveList.getWellNum().equals("B3")){
                        mAMPCurvList.add(slanCurveList);
                    }else if(isB4Check && slanCurveList.getWellNum().equals("B4")){
                        mAMPCurvList.add(slanCurveList);
                    }else if(isB5Check && slanCurveList.getWellNum().equals("B5")){
                        mAMPCurvList.add(slanCurveList);
                    }else if(isB6Check && slanCurveList.getWellNum().equals("B6")){
                        mAMPCurvList.add(slanCurveList);
                    }else if(isB7Check && slanCurveList.getWellNum().equals("B7")){
                        mAMPCurvList.add(slanCurveList);
                    }else if(isB8Check && slanCurveList.getWellNum().equals("B8")){
                        mAMPCurvList.add(slanCurveList);
                    }
                }

            }
        }
        Log.i("AIO", "updateWellResult解析结果 mAMPCurvList" + mAMPCurvList.size());
    }

    private void setCheckWellMeltCurve() {

        if (null != mOriginMelt && mOriginMelt.size() > 0) {
            mMeltCurvList.clear();
            for(SLANCurveList slanCurveList :mOriginMelt){
                if(null != slanCurveList.getWellNum()){
                    if (isA1Check && slanCurveList.getWellNum().equals("A1")) {
                        mMeltCurvList.add(slanCurveList);
                    } else if (isA2Check && slanCurveList.getWellNum().equals("A2")) {
                        mMeltCurvList.add(slanCurveList);
                    } else if (isA3Check && slanCurveList.getWellNum().equals("A3")) {
                        mMeltCurvList.add(slanCurveList);
                    } else if (isA4Check && slanCurveList.getWellNum().equals("A4")) {
                        mMeltCurvList.add(slanCurveList);

                    } else if (isA5Check && slanCurveList.getWellNum().equals("A5")) {
                        mMeltCurvList.add(slanCurveList);

                    } else if (isA6Check && slanCurveList.getWellNum().equals("A6")) {
                        mMeltCurvList.add(slanCurveList);

                    } else if (isA7Check && slanCurveList.getWellNum().equals("A7")) {
                        mMeltCurvList.add(slanCurveList);

                    } else if (isA8Check && slanCurveList.getWellNum().equals("A8")) {
                        mMeltCurvList.add(slanCurveList);
                    }else if (isB1Check && slanCurveList.getWellNum().equals("B1")) {
                        mMeltCurvList.add(slanCurveList);

                    }else if(isB2Check && slanCurveList.getWellNum().equals("B2")) {
                        mMeltCurvList.add(slanCurveList);
                    }else if(isB3Check && slanCurveList.getWellNum().equals("B3")) {
                        mMeltCurvList.add(slanCurveList);
                    }else if(isB4Check && slanCurveList.getWellNum().equals("B4")) {
                        mMeltCurvList.add(slanCurveList);
                    }else if(isB5Check && slanCurveList.getWellNum().equals("B5")) {
                        mMeltCurvList.add(slanCurveList);
                    }else if(isB6Check && slanCurveList.getWellNum().equals("B6")) {
                        mMeltCurvList.add(slanCurveList);
                    }else if(isB7Check && slanCurveList.getWellNum().equals("B7")) {
                        mMeltCurvList.add(slanCurveList);
                    }else if(isB8Check && slanCurveList.getWellNum().equals("B8")) {
                        mMeltCurvList.add(slanCurveList);
                    }
                }
            }
        }
    }

    private void setCheckWellPeakCurve() {
        if (null != mOriginPeak && mOriginPeak.size() > 0) {
            mPeakCurvList.clear();
            for(SLANCurveList slanCurveList :mOriginPeak){
                if(null != slanCurveList.getWellNum()){
                    if (isA1Check && slanCurveList.getWellNum().equals("A1")) {
                        mPeakCurvList.add(slanCurveList);
                    } else if (isA2Check && slanCurveList.getWellNum().equals("A2")) {
                        mPeakCurvList.add(slanCurveList);
                    } else if (isA3Check && slanCurveList.getWellNum().equals("A3")) {
                        mPeakCurvList.add(slanCurveList);
                    } else if (isA4Check && slanCurveList.getWellNum().equals("A4")) {
                        mPeakCurvList.add(slanCurveList);

                    } else if (isA5Check && slanCurveList.getWellNum().equals("A5")) {
                        mPeakCurvList.add(slanCurveList);

                    } else if (isA6Check && slanCurveList.getWellNum().equals("A6")) {
                        mPeakCurvList.add(slanCurveList);

                    } else if (isA7Check && slanCurveList.getWellNum().equals("A7")) {
                        mPeakCurvList.add(slanCurveList);

                    } else if (isA8Check && slanCurveList.getWellNum().equals("A8")) {
                        mPeakCurvList.add(slanCurveList);
                    }else if(isB1Check && slanCurveList.getWellNum().equals("B1")) {
                        mPeakCurvList.add(slanCurveList);

                    }else if(isB2Check && slanCurveList.getWellNum().equals("B2")) {
                        mPeakCurvList.add(slanCurveList);
                    }else if(isB3Check && slanCurveList.getWellNum().equals("B3")) {
                        mPeakCurvList.add(slanCurveList);
                    }else if(isB4Check && slanCurveList.getWellNum().equals("B4")) {
                        mPeakCurvList.add(slanCurveList);
                    }else if(isB5Check && slanCurveList.getWellNum().equals("B5")) {
                        mPeakCurvList.add(slanCurveList);
                    }else if(isB6Check && slanCurveList.getWellNum().equals("B6")) {
                        mPeakCurvList.add(slanCurveList);
                    }else if(isB7Check && slanCurveList.getWellNum().equals("B7")) {
                        mPeakCurvList.add(slanCurveList);
                    }else if(isB8Check && slanCurveList.getWellNum().equals("B8")) {
                        mPeakCurvList.add(slanCurveList);
                    }
                }

            }
        }
        Log.i("AIO", "updateWellResult解析结果 mPeakCurvList" + mPeakCurvList.size());
    }

    private void setCheckWellExResult() {
        if (null != mOriginExResults && mOriginExResults.size() > 0) {
            mExlResults.clear();
            for(SLANExlResult exlResult :mOriginExResults){
                if (isA1Check && exlResult.getWellNum().equals("A1")) {
                    mExlResults.add(exlResult);
                } else if (isA2Check && exlResult.getWellNum().equals("A2")) {
                    mExlResults.add(exlResult);
                } else if (isA3Check && exlResult.getWellNum().equals("A3")) {
                    mExlResults.add(exlResult);
                } else if (isA4Check && exlResult.getWellNum().equals("A4")) {
                    mExlResults.add(exlResult);

                } else if (isA5Check && exlResult.getWellNum().equals("A5")) {
                    mExlResults.add(exlResult);

                } else if (isA6Check && exlResult.getWellNum().equals("A6")) {
                    mExlResults.add(exlResult);

                } else if (isA7Check && exlResult.getWellNum().equals("A7")) {
                    mExlResults.add(exlResult);

                } else if (isA8Check && exlResult.getWellNum().equals("A8")) {
                    mExlResults.add(exlResult);
                }else if (isB1Check && exlResult.getWellNum().equals("B1")) {
                    mExlResults.add(exlResult);

                } else if (isB2Check && exlResult.getWellNum().equals("B2")) {
                    mExlResults.add(exlResult);
                }else if (isB3Check && exlResult.getWellNum().equals("B3")) {
                    mExlResults.add(exlResult);

                } else if (isB4Check && exlResult.getWellNum().equals("B4")) {
                    mExlResults.add(exlResult);
                }else if (isB5Check && exlResult.getWellNum().equals("B5")) {
                    mExlResults.add(exlResult);

                } else if (isB6Check && exlResult.getWellNum().equals("B6")) {
                    mExlResults.add(exlResult);
                }else if (isB7Check && exlResult.getWellNum().equals("B7")) {
                    mExlResults.add(exlResult);

                } else if (isB8Check && exlResult.getWellNum().equals("B8")) {
                    mExlResults.add(exlResult);
                }
            }
        }

    }
    /**
     * 点击孔位更新
     */
    private void updateWellResult() {
        Log.i("SLAN", "点击孔位更新" );
        setCheckWellRawCurve();
        setCheckWellAMPCurve();
        setCheckWellMeltCurve();
        setCheckWellPeakCurve();
        setCheckWellExResult();

        switch (currentView) {
            case 1:
                getAllRawCurve();
                drawRawCurve();
                break;
            case 2:
                getAllAPMCurve();
                drawAMPCurve();
                break;
            case 3:
                getAllMelturve();
                drawMeltCurve();
                break;
            case 4:
                getAllPeakCurve();
                drawPeakCurve();
                break;
            case 5:
                getShowExlResults();
                MessageEvent update = new MessageEvent();
                update.setMsgType(MsgType.MSG_UPDATE_EXPERIMENT_EXCEL);
                EventBus.getDefault().post (update);
                break;
        }
    }


    private void getShowExlResults() {
        //判断channel和wellNum
        if (null != mExlResults) {
            mCheckExlResults.clear();
            if (mExlResults.size() > 0) {
                for (SLANExlResult exlResult : mExlResults) {
                    if (exlResult.getChannelNo() == 1) {
                        if (isFAMCheck) {
                            mCheckExlResults.add(exlResult);
                        }
                    }else if (exlResult.getChannelNo() == 2) {
                        if (isHEXCheck) {
                            mCheckExlResults.add(exlResult);
                        }
                    } else if (exlResult.getChannelNo() == 3) {
                        if (isROXCheck) {
                            mCheckExlResults.add(exlResult);
                        }
                    }else if (exlResult.getChannelNo() == 4) {
                        if (isCY5Check) {
                            mCheckExlResults.add(exlResult);
                        }
                    }else if (exlResult.getChannelNo() == 5) {
                        if (isQuaSarCheck) {
                            mCheckExlResults.add(exlResult);
                        }
                    }else if (exlResult.getChannelNo() == 6) {
                        if (isAttoCheck) {
                            mCheckExlResults.add(exlResult);
                        }
                    }

                }
            }
        }
    }

    /**
     * @param slanWellResult 每一个WellResult对应4个channel的结论
     */
    private void parseWellResult(SLANWellResult slanWellResult) {

        String regentCode = slanWellResult.getReagentCode();
        String idCard = slanWellResult.getSampleName();
        isMelt = slanWellResult.isMelt();

        String mRoxTargetResults = slanWellResult.getRoxTargetResults();
        String mHexTargetResults = slanWellResult.getHexTargetResults();
        String mFamTargetResults = slanWellResult.getFamTargetResults();
        String mCy5TargetResults = slanWellResult.getCy5TargetResults();
        String mQuasarTargetResults = slanWellResult.getQuasarTargetResults();
        String mAttoTargetResults = slanWellResult.getAttoTargetResults();

        String fam_threshold = slanWellResult.getFamThreshold();
        String hex_threshold = slanWellResult.getHexThreshold();
        String rox_threshold = slanWellResult.getRoxThreshold();
        String cy5_threshold = slanWellResult.getCy5Threshold();
        String quasar_threshold = slanWellResult.getQuasarThreshold();
        String atto_threshold = slanWellResult.getAttoThreshold();

        TargetResult mHexTargetResult = JSON.parseObject(mHexTargetResults, TargetResult.class);
        TargetResult mFAMTargetResult = JSON.parseObject(mFamTargetResults, TargetResult.class);
        TargetResult mCY5TargetResult = JSON.parseObject(mCy5TargetResults, TargetResult.class);
        TargetResult mROXTargetResult = JSON.parseObject(mRoxTargetResults, TargetResult.class);



        SLANCurveList hex_amp_curve = SLANManager.getInstance().parseTargetResultToAMP(slanWellResult.getWellName(), slanWellResult.getSampleName(),mHexTargetResult);
        SLANCurveList hex_peak_curve = SLANManager.getInstance().parseTargetResultToPeak(slanWellResult.getWellName(), slanWellResult.getSampleName(),mHexTargetResult);
        SLANCurveList hex_raw_curve = SLANManager.getInstance().parseTargetResultToRaw(slanWellResult.getWellName(), slanWellResult.getSampleName(), mHexTargetResult);
        SLANCurveList hex_melt_curve = SLANManager.getInstance().parseTargetResultToMelt(slanWellResult.getWellName(), slanWellResult.getSampleName(), mHexTargetResult);

        SLANCurveList fam_amp_curve = SLANManager.getInstance().parseTargetResultToAMP(slanWellResult.getWellName(), slanWellResult.getSampleName(), mFAMTargetResult);
        SLANCurveList fam_peak_curve = SLANManager.getInstance().parseTargetResultToPeak(slanWellResult.getWellName(), slanWellResult.getSampleName(), mFAMTargetResult);
        SLANCurveList fam_raw_curve = SLANManager.getInstance().parseTargetResultToRaw(slanWellResult.getWellName(), slanWellResult.getSampleName(), mFAMTargetResult);
        SLANCurveList fam_melt_curve = SLANManager.getInstance().parseTargetResultToMelt(slanWellResult.getWellName(), slanWellResult.getSampleName(), mFAMTargetResult);


        SLANCurveList rox_amp_curve = SLANManager.getInstance().parseTargetResultToAMP(slanWellResult.getWellName(), slanWellResult.getSampleName(), mROXTargetResult);
        SLANCurveList rox_peak_curve = SLANManager.getInstance().parseTargetResultToPeak(slanWellResult.getWellName(), slanWellResult.getSampleName(), mROXTargetResult);
        SLANCurveList rox_raw_curve = SLANManager.getInstance().parseTargetResultToRaw(slanWellResult.getWellName(),slanWellResult.getSampleName(), mROXTargetResult);
        SLANCurveList rox_melt_curve = SLANManager.getInstance().parseTargetResultToMelt(slanWellResult.getWellName(),  slanWellResult.getSampleName(),mROXTargetResult);

        SLANCurveList cy5_amp_curve = SLANManager.getInstance().parseTargetResultToAMP(slanWellResult.getWellName(), slanWellResult.getSampleName(), mCY5TargetResult);
        SLANCurveList cy5_peak_curve = SLANManager.getInstance().parseTargetResultToPeak(slanWellResult.getWellName(),slanWellResult.getSampleName(),mCY5TargetResult);
        SLANCurveList cy5_raw_curve = SLANManager.getInstance().parseTargetResultToRaw(slanWellResult.getWellName(), slanWellResult.getSampleName(), mCY5TargetResult);
        SLANCurveList cy5_melt_curve = SLANManager.getInstance().parseTargetResultToMelt(slanWellResult.getWellName(), slanWellResult.getSampleName(), mCY5TargetResult);


        mAMPCurvList.add(fam_amp_curve);
        mAMPCurvList.add(hex_amp_curve);
        mAMPCurvList.add(rox_amp_curve);
        mAMPCurvList.add(cy5_amp_curve);


        mPeakCurvList.add(fam_peak_curve);
        mPeakCurvList.add(hex_peak_curve);
        mPeakCurvList.add(rox_peak_curve);
        mPeakCurvList.add(cy5_peak_curve);

        mRawCurveList.add(fam_raw_curve);
        mRawCurveList.add(hex_raw_curve);
        mRawCurveList.add(rox_raw_curve);
        mRawCurveList.add(cy5_raw_curve);

        mMeltCurvList.add(fam_melt_curve);
        mMeltCurvList.add(hex_melt_curve);
        mMeltCurvList.add(rox_melt_curve);
        mMeltCurvList.add(cy5_melt_curve);

        String roxResults = slanWellResult.getRoxResults();//rox对应的结论
        String hexResults = slanWellResult.getHexResults();
        String famResults = slanWellResult.getFamResults();
        String cy5Results = slanWellResult.getCy5Results();
        String quasarResults = slanWellResult.getQuasarResults();
        String attoResults = slanWellResult.getAttoResults();

        /*List<Result> roxResultList = gson.fromJson(roxResults, new TypeToken<List<Result>>(){}.getType());

        List<Result> hexResultList = gson.fromJson(hexResults, new TypeToken<List<Result>>(){}.getType());
        List<Result> famResultList = gson.fromJson(famResults, new TypeToken<List<Result>>(){}.getType());
        List<Result> cy5ResultList = gson.fromJson(cy5Results, new TypeToken<List<Result>>(){}.getType());*/
        List<CtResult> famResultList = JSON.parseArray(famResults, CtResult.class);
        List<CtResult> hexResultList = JSON.parseArray(hexResults, CtResult.class);
        List<CtResult> roxResultList = JSON.parseArray(roxResults, CtResult.class);
        List<CtResult> cy5ResultList = JSON.parseArray(cy5Results, CtResult.class);
        List<CtResult> quasarResultList = JSON.parseArray(quasarResults, CtResult.class);
        List<CtResult> attoResultList = JSON.parseArray(attoResults, CtResult.class);

        List<PeakResult> famPeakResult = JSON.parseArray(slanWellResult.getFamPeakResults(), PeakResult.class);
        List<PeakResult> hexPeakResult = JSON.parseArray(slanWellResult.getHexPeakResults(), PeakResult.class);
        List<PeakResult> roxPeakResult = JSON.parseArray(slanWellResult.getRoxPeakResults(), PeakResult.class);
        List<PeakResult> cy5PeakResult = JSON.parseArray(slanWellResult.getCy5PeakResults(), PeakResult.class);
        List<PeakResult> quasarPeakResult = JSON.parseArray(slanWellResult.getQuasarPeakResults(), PeakResult.class);
        List<PeakResult> attoPeakResult = JSON.parseArray(slanWellResult.getAttoPeakResults(), PeakResult.class);

        SLANExlResult famExlResult = new SLANExlResult();
        famExlResult.setWellNum(slanWellResult.getWellName());
        famExlResult.setChannelNo(1);
        famExlResult.setCurve_hole("FAM");
        famExlResult.setResults(famResultList);
        famExlResult.setPeakResults(famPeakResult);
        famExlResult.setRegentCode(regentCode);
        famExlResult.setIdCard(idCard);
        famExlResult.setMelt(isMelt);
        if (null != mFAMTargetResult) {
            famExlResult.setCt(mFAMTargetResult.getCt());
            famExlResult.setControlRn(mFAMTargetResult.getRnValue());
            famExlResult.setPeakValues(mFAMTargetResult.getPeakValues());
        }
        if(!StringUtil.isEmpty(fam_threshold)){
            isFamThreshold = true;
            famThreshold = (double) StringUtil.convertToFloat(fam_threshold,0.00f);
        }
        if(!StringUtil.isEmpty(slanWellResult.getFamPeekThreshold())){
            double famPeekThreshold = (double) StringUtil.convertToFloat(slanWellResult.getFamPeekThreshold(),0.00f);
            famExlResult.setPeekHighThreshold(famPeekThreshold);
        }

        famExlResult.setThreshold(famThreshold);
        mExlResults.add(famExlResult);


        SLANExlResult hexExlResult = new SLANExlResult();
        hexExlResult.setWellNum(slanWellResult.getWellName());
        hexExlResult.setChannelNo(2);
        hexExlResult.setCurve_hole("HEX");
        hexExlResult.setResults(hexResultList);
        hexExlResult.setPeakResults(hexPeakResult);
        hexExlResult.setRegentCode(regentCode);
        hexExlResult.setIdCard(idCard);
        hexExlResult.setMelt(isMelt);
        if (null != mHexTargetResult) {
            hexExlResult.setCt(mHexTargetResult.getCt());
            hexExlResult.setControlRn(mHexTargetResult.getRnValue());
            hexExlResult.setPeakValues(mHexTargetResult.getPeakValues());
        }
        if(!StringUtil.isEmpty(hex_threshold)){
            isHexThreshold = true;
            hexThreshold = (double) StringUtil.convertToFloat(hex_threshold,0.00f);
        }
        hexExlResult.setThreshold(hexThreshold);
        if(!StringUtil.isEmpty(slanWellResult.getHexPeekThreshold())){
            double hexPeakThreshold = (double)StringUtil.convertToFloat(slanWellResult.getHexPeekThreshold(),0.00f);
            hexExlResult.setPeekHighThreshold(hexPeakThreshold);
        }

        mExlResults.add(hexExlResult);


        SLANExlResult roxExlResult = new SLANExlResult();
        roxExlResult.setWellNum(slanWellResult.getWellName());
        roxExlResult.setChannelNo(3);
        roxExlResult.setCurve_hole("ROX");
        roxExlResult.setResults(roxResultList);
        roxExlResult.setPeakResults(roxPeakResult);
        roxExlResult.setRegentCode(regentCode);
        roxExlResult.setIdCard(idCard);
        roxExlResult.setMelt(isMelt);
        if (null != mROXTargetResult) {
            roxExlResult.setCt(mROXTargetResult.getCt());
            roxExlResult.setControlRn(mROXTargetResult.getRnValue());
            roxExlResult.setPeakValues(mROXTargetResult.getPeakValues());

        }
        if(!StringUtil.isEmpty(rox_threshold)){
            isRoxThreshold = true;
            roxThreshold = (double) StringUtil.convertToFloat(rox_threshold,0.00f);
        }
        roxExlResult.setThreshold(roxThreshold);
        if(!StringUtil.isEmpty(slanWellResult.getRoxPeekThreshold())){
            double roxPeekThreshold = (double) StringUtil.convertToFloat(slanWellResult.getRoxPeekThreshold(),0.00f);
            roxExlResult.setPeekHighThreshold(roxPeekThreshold);
        }
        mExlResults.add(roxExlResult);


        SLANExlResult cy5ExlResult = new SLANExlResult();
        cy5ExlResult.setWellNum(slanWellResult.getWellName());
        cy5ExlResult.setChannelNo(4);
        cy5ExlResult.setCurve_hole("CY5");
        cy5ExlResult.setResults(cy5ResultList);
        cy5ExlResult.setPeakResults(cy5PeakResult);
        cy5ExlResult.setRegentCode(regentCode);
        cy5ExlResult.setIdCard(idCard);
        cy5ExlResult.setMelt(isMelt);
        if (null != mCY5TargetResult) {
            cy5ExlResult.setCt(mCY5TargetResult.getCt());
            cy5ExlResult.setControlRn(mCY5TargetResult.getRnValue());
            cy5ExlResult.setPeakValues(mCY5TargetResult.getPeakValues());
        }
        if(!StringUtil.isEmpty(cy5_threshold)){
            iscy5Threshold = true;
            cy5Threshold = (double) StringUtil.convertToFloat(cy5_threshold,0.00f);
        }
        cy5ExlResult.setThreshold(cy5Threshold);

        if(!StringUtil.isEmpty(slanWellResult.getCy5PeekThreshold())){
            double cy5PeekThreshold = (double) StringUtil.convertToFloat(slanWellResult.getCy5PeekThreshold(),0.00f);
            cy5ExlResult.setPeekHighThreshold(cy5PeekThreshold);
        }

        mExlResults.add(cy5ExlResult);

        if(!StringUtil.isEmpty(mQuasarTargetResults)){
            TargetResult quasarTargetResult  = JSON.parseObject(mQuasarTargetResults, TargetResult.class);
            SLANCurveList quasar_amp_curve = SLANManager.getInstance().parseTargetResultToAMP(slanWellResult.getWellName(),  slanWellResult.getSampleName(),quasarTargetResult);
            SLANCurveList quasar_peak_curve = SLANManager.getInstance().parseTargetResultToPeak(slanWellResult.getWellName(),slanWellResult.getSampleName(),quasarTargetResult);
            SLANCurveList quasar_raw_curve = SLANManager.getInstance().parseTargetResultToRaw(slanWellResult.getWellName(),  slanWellResult.getSampleName(),quasarTargetResult);
            SLANCurveList quasar_Melt_curve = SLANManager.getInstance().parseTargetResultToMelt(slanWellResult.getWellName(), slanWellResult.getSampleName(), quasarTargetResult);
            mAMPCurvList.add(quasar_amp_curve);
            mRawCurveList.add(quasar_raw_curve);
            mPeakCurvList.add(quasar_peak_curve);
            mMeltCurvList.add(quasar_Melt_curve);

            SLANExlResult quasarExlResult = new SLANExlResult();
            quasarExlResult.setWellNum(slanWellResult.getWellName());
            quasarExlResult.setChannelNo(5);
            quasarExlResult.setCurve_hole("QUASAR");
            quasarExlResult.setResults(quasarResultList);
            quasarExlResult.setPeakResults(quasarPeakResult);
            quasarExlResult.setRegentCode(regentCode);
            quasarExlResult.setIdCard(idCard);
            quasarExlResult.setMelt(isMelt);
            if (null != quasarTargetResult) {
                quasarExlResult.setCt(quasarTargetResult.getCt());
                quasarExlResult.setControlRn(quasarTargetResult.getRnValue());
                quasarExlResult.setPeakValues(quasarTargetResult.getPeakValues());
            }
            if(!StringUtil.isEmpty(quasar_threshold)){
                isQuasarThreshold = true;
                quasarThreshold = (double) StringUtil.convertToFloat(quasar_threshold,0.00f);
            }
            quasarExlResult.setThreshold(quasarThreshold);
            if(!StringUtil.isEmpty(slanWellResult.getQuasarPeekThreshold())){
                double quasarPeekThreshold = (double) StringUtil.convertToFloat(slanWellResult.getQuasarPeekThreshold(),0.00f);
                quasarExlResult.setPeekHighThreshold(quasarPeekThreshold);
            }
            mExlResults.add(quasarExlResult);
        }


        if(!StringUtil.isEmpty(mAttoTargetResults)){
            TargetResult  attoTargetResult  = JSON.parseObject(mAttoTargetResults, TargetResult.class);
            SLANCurveList atto_amp_curve = SLANManager.getInstance().parseTargetResultToAMP(slanWellResult.getWellName(),  slanWellResult.getSampleName(),attoTargetResult);
            SLANCurveList atto_peak_curve = SLANManager.getInstance().parseTargetResultToPeak(slanWellResult.getWellName(),slanWellResult.getSampleName(),attoTargetResult);
            SLANCurveList atto_raw_curve = SLANManager.getInstance().parseTargetResultToRaw(slanWellResult.getWellName(), slanWellResult.getSampleName(), attoTargetResult);
            SLANCurveList atto_melt_curve = SLANManager.getInstance().parseTargetResultToMelt(slanWellResult.getWellName(), slanWellResult.getSampleName(), attoTargetResult);
            mAMPCurvList.add(atto_amp_curve);
            mRawCurveList.add(atto_raw_curve);
            mPeakCurvList.add(atto_peak_curve);
            mMeltCurvList.add(atto_melt_curve);

            SLANExlResult attoExlResult = new SLANExlResult();
            attoExlResult.setWellNum(slanWellResult.getWellName());
            attoExlResult.setChannelNo(6);
            attoExlResult.setCurve_hole("ATTO");
            attoExlResult.setResults(attoResultList);
            attoExlResult.setPeakResults(attoPeakResult);
            attoExlResult.setRegentCode(regentCode);
            attoExlResult.setIdCard(idCard);
            attoExlResult.setMelt(isMelt);
            if (null != attoTargetResult) {
                attoExlResult.setCt(attoTargetResult.getCt());
                attoExlResult.setControlRn(attoTargetResult.getRnValue());
                attoExlResult.setPeakValues(attoTargetResult.getPeakValues());
            }
            if(!StringUtil.isEmpty(atto_threshold)){
                isAttoThreshold = true;
                attoThreshold = (double) StringUtil.convertToFloat(atto_threshold,0.00f);
            }
            attoExlResult.setThreshold(attoThreshold);
            if(!StringUtil.isEmpty(slanWellResult.getAttoPeekThreshold())){
                double attoPeekThreshold = (double) StringUtil.convertToFloat(slanWellResult.getAttoPeekThreshold(),0.00f);
                attoExlResult.setPeekHighThreshold(attoPeekThreshold);
            }
            mExlResults.add(attoExlResult);
        }

    }
    ///////////////////////////////////////////弹出正在查询数据库的对话框4///////////////////////////////////////////////

    public boolean isUIThread() {
        return Thread.currentThread().getId() == Looper.getMainLooper().getThread().getId();
    }
/////////////////////////////////////////////////二次解析//////////////////////////////////////////////////


    /**
     * 保存PCR文件为.boweipcrResult
     * @param result
     * @param message
     */
    @Override
    public void onSaveJsonResult(boolean result,String message) {
        Toast.makeText(getContext(),
                message,Toast.LENGTH_LONG).show();
    }


    /**
     * 二次解析結束回调
     * 二次分析成功提示是否更新数据库
     */
    @Override
    public void onSecondParseFinish(boolean result) {
        Log.i(TAG,"onSecondParseFinish" + result + isUIThread());
        if (result) {
            MessageEvent secondParseMsg = new MessageEvent();
            secondParseMsg.setData(result);
            secondParseMsg.setMsgType(MsgType.MSG_SHOW_EX_SECOND_RESULT);
            EventBus.getDefault().post (secondParseMsg);

        } else {
            String failMsg = getResources().getString(R.string.toast_second_analyse_fail);
            MessageEvent failEvent = new MessageEvent();
            failEvent.setMsgType(1);
            failEvent.setData(failMsg);
            EventBus.getDefault().post(failEvent);
        }
    }

    /**
     * 更新结束
     *
     * @param result
     */
    @Override
    public void onUpdateFinish(boolean result) {
        if (result) {
            //查询重新显示
            Toast.makeText(getContext().getApplicationContext()
                    ,getResources().getString(R.string.result_ex_detail_toast_update_result_success),Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext().getApplicationContext()
                    ,getResources().getString(R.string.result_ex_detail_toast_update_result_fail),Toast.LENGTH_LONG).show();
        }
    }


    /**
     * 二次分析
     * 将json的数据转化成Experiment,模板名称不为空
     *
     */
    private void secondAnalyse(Experiment sec_experiment) {
        if(null != configTargets && configTargets.size()>0){
            if(!StringUtil.isEmpty(testItem)){
                File allFileDir = new File(curTemplatePath);
                List<File> localList = new ArrayList<File>();
                if (null != allFileDir) {
                    if (allFileDir.isDirectory() && allFileDir.exists()) {
                        Collections.addAll(localList, allFileDir.listFiles());
                    }
                }
                if (null != localList && localList.size() > 0) {
                    final File ctFile = FileUtils.getFile(localList, ".ct");
                    final File peakFile = FileUtils.getFile(localList, ".peak");
                    showLoadingDialog(getResources().getString(R.string.dialog_message_second_analyse));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SLANManager.getInstance().secondSLANExAnalyse(configTargets,AMPAlgorithm,
                                    ctFile, peakFile,sec_experiment, startTime);
                        }
                    }).start();

                }else {
                    showLoadingDialog(getResources().getString(R.string.dialog_message_second_analyse));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SLANManager.getInstance().secondSLANExAnalyse(configTargets, AMPAlgorithm
                                    , null, null, sec_experiment, startTime);
                        }
                    }).start();
                }
            }else{
                    showLoadingDialog(getResources().getString(R.string.dialog_message_second_analyse));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SLANManager.getInstance().secondSLANExAnalyse(configTargets,AMPAlgorithm
                                    , null, null,sec_experiment,startTime);
                        }
                    }).start();
            }
        }else {
            Toast.makeText(getContext().getApplicationContext(),
                getResources().getString(R.string.result_ex_toast_no_second_file),Toast.LENGTH_LONG).show();

        }
    }




    ///////////////////////////////////////********二次分析***********************************************/////
    /**
     * 更新实验
     */
    private void updateExperiment() {
        //更新数据库
        SLANManager.getInstance().updateDetailExperiment(testItem, startTime);
        //更新文件
        if(!StringUtil.isEmpty(testItem)&& !StringUtil.isEmpty(startTime)){
            Experiment experiment = SLANManager.getInstance().getExperiment();
            updateSLANResultToPCR(experiment,startTime,testItem);
        }else{
            if(StringUtil.isEmpty(testItem)){
                // ToastUtils.showToast(getApplicationContext(), "测试项目格式有误");
            }else if(StringUtil.isEmpty(startTime)){
                // ToastUtils.showToast(getApplicationContext(), "保存项目的时间格式有误");
            }
        }
    }

    /**
     * 更新PCR文件和json文件
     * @param experiment
     * @param startTime
     * @param testItem
     */
    private void updateSLANResultToPCR(Experiment experiment,String startTime,String testItem){
        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName;
        if(StringUtil.isEmpty(description)){
            subFirName = afterTime+"_"+ testItem;
        }else{
            subFirName = afterTime+"_"+ testItem + "_" + description;
        }
        File jsonDir = new File(FileUtils.SLAN_RESULT_PATH, subFirName);
        if(jsonDir.exists()){
            File[] files = jsonDir.listFiles();
            if(files.length>0){
                for(File file : files){
                    file.delete();
                }
            }
        }
        SLANManager.getInstance().saveDetailExToJson(experiment,subFirName);
        saveResultToPCR(experiment,testItem,subFirName);
    }

    public void saveResultToPCR(Experiment experiment,String testItem,String subFirName){
        Log.e(TAG, "saveResultToPCR" );
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean savePCRresult = SLANManager.getInstance().saveResultToPCR(experiment,pcrFileFolder);
                Log.i(TAG,"saveResultToPCR--savePCRresult" + savePCRresult);
            }
        }).start();

    }

    private void showUpdateDbDialog() {
        if (null != updateDbDialog) {
            updateDbDialog.dismiss();
            updateDbDialog = null;
        }
        if (null == updateDbDialog) {
            updateDbDialog = new CustomDialog(getContext());
            LayoutInflater mInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_interrupt_detect_layout, null);
            TextView hintTv = (TextView) view.findViewById(R.id.dialog_interrupt_hint_tv);
            hintTv.setText(R.string.dialog_message_update_ex);
            CustomDialog.Builder builder = new CustomDialog.Builder(getContext());
            builder.setContentView(view);
            builder.setTitle(R.string.dialog_title_prompt);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub

                    updateExperiment();
                    updateDbDialog.dismiss();
                    updateDbDialog = null;
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    updateDbDialog.dismiss();
                    updateDbDialog = null;
                }
            });
            updateDbDialog = builder.create();
            updateDbDialog.setCancelable(false);
            updateDbDialog.show();
        }
    }



    /**
     * 截屏操作
     **/
    private void screenShot (){
        final ImageView screenshot_Bmp;
        if(!StringUtil.isEmpty(startTime) && !StringUtil.isEmpty(testItem)){
            String aftertime = DateFormatUtil.convertDateFormat(startTime);
            String subFirName = aftertime + "_" + testItem;

            File dir = new File(FileUtils.IMAGE_PATH, subFirName);
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs();
            }
            Bitmap bitmap = ScreenShotUtil.saveScreenShot(getActivity(),dir);
            screenshot_Bmp = new ImageView(getContext());
            screenshot_Bmp.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            screenshot_Bmp.setImageBitmap(bitmap);
            binding.rootLl.addView(screenshot_Bmp);
            ScaleAnimation scaleAnimation = (ScaleAnimation) AnimationUtils
                    .loadAnimation(getContext(), R.anim.screen_cut);
            scaleAnimation.setAnimationListener(new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation arg0) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onAnimationRepeat(Animation arg0) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onAnimationEnd(Animation arg0) {
                    if (binding.rootLl != null)
                        binding.rootLl.removeView(screenshot_Bmp);
                    Toast.makeText(getContext(),"screenshot done",Toast.LENGTH_LONG).show();
                }
            });
            screenshot_Bmp.startAnimation(scaleAnimation);
        }
    }


    /**
     * 将Ex保存为Json格式
     */
    private void saveExJsonFile(){
            //找文件
        if(!StringUtil.isEmpty(pcrFileFolder)){
            File file = getSLANExPCRFile(pcrFileFolder);
            if (file != null && file.exists()) {
                String file_path = file.getAbsolutePath();
                if(isHasExJsonFile(pcrFileFolder)){
                    Toast.makeText(getContext().getApplicationContext(),
                            getResources().getString(R.string.result_ex_detail_toast_pcr_json_exist),Toast.LENGTH_LONG).show();
                    return;
                }
                if(StringUtil.isEmpty(file_path)){
                    Toast.makeText(getContext().getApplicationContext(),
                            getResources().getString(R.string.result_ex_detail_toast_pcr_file_no_exist),Toast.LENGTH_LONG).show();
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Experiment open_ex = PcrApplication.slanService.openExperiment(file_path,true);
                        if(null != open_ex){
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    SLANManager.getInstance().saveDetailExToJson(open_ex,pcrFileFolder);
                                }
                            });
                        }
                    }

                }).start();

            }else {
                Toast.makeText(getContext().getApplicationContext(),
                        getResources().getString(R.string.result_ex_detail_toast_pcr_file_no_exist),Toast.LENGTH_LONG).show();
            }
        }


    }

    /********************************************************************************************/
    /**
     * 扩增曲线参数修改
     */
    private void showNumKeyboardDialog(int dialogType, View view,String s, int tempType){
        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
        if(numKeyboardDialog == null){
            numKeyboardDialog = new NumKeyboardDialog(getContext());
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
                    if(dialogType == 1){
                        //检测下限
                        //0-10000，000，000
                       /* double detectMin = (double)StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget rangConfig = mDetectRanges.get(detect_range_pos);
                        rangConfig.setDetectMin(detectMin);

                        ConfigTarget configTarget = getConfigTarget(rangConfig.getChannelNo());
                        configTarget.setDetectMin(detectMin);
                        detectRangeAdapter.setRangeList(mDetectRanges);
                        if(null != mAmpAlgorithmAdapter && null != mMeltParamterAdapter){
                            mAmpAlgorithmAdapter.setAmpAlgorithm(mParamTargets);
                            mMeltParamterAdapter.setMeltParamter(mParamTargets);
                        }*/
                    }else if(dialogType == 2){
                        //基线终点
                        updateEndBaseline(s);
                    }else if(dialogType == 3){
                        //基线起点
                        updateBeginBaseline(s);

                    }else if(dialogType == 4){
                        //手动阈值
                        updateThreshold(s);

                    }else if(dialogType == 5){
                        //最低温度
                        updateMinTemperature(s);
                    }else if(dialogType == 6){
                        //最高温度
                        updateMaxTemperature(s);
                    }else if(dialogType == 7){
                        //峰高阈值
                        updatePeekHighThreshold(s);
                    }
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.show();
        }
    }

    private void updateEndBaseline(String value) {
        int endbase = (int)StringUtil.convertToFloat(value,1.0f);
        if(endbase < 1){
            Toast.makeText(getContext(),"不能小于1",Toast.LENGTH_LONG).show();
            return;
        }else{
            ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
            configTarget.setEndBaseline(endbase);
            mAmpParamterAdapter.setAmpAlgorithm(configTargets);
        }
    }
    //基线起点
    private void updateBeginBaseline(String value) {
        int beginbase = (int)StringUtil.convertToFloat(value,1.0f);
        if(beginbase < 1){
            Toast.makeText(getContext(),"不能小于1",Toast.LENGTH_LONG).show();
            return;
        }else{
            ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
            configTarget.setBeginBaseline(beginbase);
            mAmpParamterAdapter.setAmpAlgorithm(configTargets);
        }
    }
    //手动阈值
    private void updateThreshold(String value) {
        double threshold = (double) StringUtil.convertToFloat(value,0.00f);
        ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
        configTarget.setThreshold(threshold);
        mAmpParamterAdapter.setAmpAlgorithm(configTargets);
    }
    //更新溶解参数最低温度
    private void updateMinTemperature(String value) {
        double minTemp = (double) StringUtil.convertToFloat(value,0.00f);
        configTargets.get(meltParamter_pos).setMinTemperature(minTemp);
        mMeltParamterAdapter.setMeltParamter(configTargets);
    }
    //溶解参数最高温度
    private void updateMaxTemperature(String value) {
        double maxTemp = StringUtil.convertToFloat(value, 0.00f);
        configTargets.get(meltParamter_pos).setMaxTemperature(maxTemp);
        mMeltParamterAdapter.setMeltParamter(configTargets);
    }

    private void updatePeekHighThreshold(String value) {
        double peekHighThreshold = StringUtil.convertToFloat(value, 0.00f);
        configTargets.get(meltParamter_pos).setPeekHighThreshold(peekHighThreshold);
        mMeltParamterAdapter.setMeltParamter(configTargets);
    }

    /**扩增曲线算法
     * 相对荧光值法-log
     * 绝对荧光值法
     * 相对荧光值法-线性
     */
    private ListView mAmpAlgSpinnerLv;
    private List<String> mAmpAlgorithmList;
    private PopupWindow mAmpAlgWindow;
    private CustomSpinnerAdapter ampAlgSpinnerAdapter;
    private int AMPAlgorithm = 0;//扩增曲线算法枚举 = 0;//扩增曲线算法枚举
    private void initAmpAlgorithmSpinner(){
        String[] ampAlgorithmArray= getResources().getStringArray(R.array.AMPAlgorithm);
        mAmpAlgorithmList = new ArrayList<String>(ampAlgorithmArray.length);
        Collections.addAll(mAmpAlgorithmList, ampAlgorithmArray);
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        ampAlgSpinnerAdapter=new CustomSpinnerAdapter(getContext(),mAmpAlgorithmList);
        mAmpAlgSpinnerLv= (ListView) view.findViewById(R.id.spinner_listview);
        mAmpAlgSpinnerLv.setAdapter(ampAlgSpinnerAdapter);
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

        ampAlgSpinnerAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                binding.ampAlgorithmTv.setText(mAmpAlgorithmList.get(position));
                AMPAlgorithm = position;
                collapseAmpAlg();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    //扩增曲线算法
    private void collapseAmpAlg(){
        if (mAmpAlgWindow != null && mAmpAlgWindow.isShowing()) {
            mAmpAlgWindow.dismiss();
        }
    }

    //展开扩增曲线算法
    private void expandAmpAlg(View view){
        if(mAmpAlgorithmList== null && mAmpAlgorithmList.isEmpty()){
            return;
        }
        mAmpAlgWindow.showAsDropDown(view);
    }


    /**
     * 基本参数-分析类型定性/定量
     */
    private void collapseQualitativeAnalysis(){
        if (mAysTypePopWindow != null && mAysTypePopWindow.isShowing()) {
            mAysTypePopWindow.dismiss();
        }
    }

    private void expandQualitativeAnalysis(View view){
        if(mAmpAysTypes== null && mAmpAysTypes.isEmpty()){
            return;
        }
        mAysTypePopWindow.showAsDropDown(view);
    }

    private void initAmpAysTypeWindow(){
        mAmpAysTypes = new ArrayList<String>();
        mAmpAysTypes.add(getResources().getString(R.string.config_info_ampays_qualitative));
        mAmpAysTypes.add(getResources().getString(R.string.config_info_ampays_quantitative));
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter mAmpAysTypeAdapter=new CustomSpinnerAdapter(getContext(),mAmpAysTypes);
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
                    ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                    configTarget.setQualitativeAnalysis(true);//定性
                }else{
                    ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                    configTarget.setQualitativeAnalysis(false);//定量
                }

                mAmpParamterAdapter.setAmpAlgorithm(configTargets);
                collapseQualitativeAnalysis();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }
    //展开基本参数-基线优化
    // private boolean isOptimizationModeExpanded = false;//
    private void collapseOptimizationMode(){
        if (mOptimizationModeWindow != null && mOptimizationModeWindow.isShowing()) {
            mOptimizationModeWindow.dismiss();
        }
    }


    private void expandOptimizationMode(View view){
        if(mOptimizationModes== null && mOptimizationModes.isEmpty()){
            return;
        }
        mOptimizationModeWindow.showAsDropDown(view);
    }

    private void initOptimizationModeWindow(){
        mOptimizationModes = new ArrayList<String>();
        mOptimizationModes.add(getResources().getString(R.string.config_info_optimizationmode_na));
        mOptimizationModes.add(getResources().getString(R.string.config_info_optimizationmode_auto));
        mOptimizationModes.add(getResources().getString(R.string.config_info_optimizationmode_manual));
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter optimizationModeAdapter=new CustomSpinnerAdapter(getContext(),mOptimizationModes);
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
                ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                configTarget.setOptimizationMode(position);

                mAmpParamterAdapter.setAmpAlgorithm(configTargets);
                collapseOptimizationMode();
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

    //展开扩增曲线算法
    private void expandDigitalFilter(View view){
        if(mDighitalFilters== null && mDighitalFilters.isEmpty()){
            return;
        }
        mDigitalFilterWindow.showAsDropDown(view);
    }
    private void initAmpDigitalFilter(){
        String[] digitalFilters= getResources().getStringArray(R.array.AMPDigitalFiler);
        mDighitalFilters = new ArrayList<String>(digitalFilters.length);
        Collections.addAll(mDighitalFilters, digitalFilters);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter digitalFilterAdapter=new CustomSpinnerAdapter(getContext(),mDighitalFilters);
        ListView mDigitalFilterLv= (ListView) view.findViewById(R.id.spinner_listview);
        mDigitalFilterLv.setAdapter(digitalFilterAdapter);
        mDigitalFilterWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mDigitalFilterWindow.setOutsideTouchable(true);
        mDigitalFilterWindow.setFocusable(true);
        mDigitalFilterWindow.setContentView(view);
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
                ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                configTarget.setDigitalFilter(position);
                mAmpParamterAdapter.setAmpAlgorithm(configTargets);
                collapseDigitalFilter();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }
    /**
     * 熔解曲线参数-熔解峰型
     */
    private void collapsePeekType(){
        if (mPeekTypeWindow != null && mPeekTypeWindow.isShowing()) {
            mPeekTypeWindow.dismiss();
        }
    }

    private void expandPeekType(View view){
        if( mPeekTypes== null && mPeekTypes.isEmpty()){
            return;
        }
        mPeekTypeWindow.showAsDropDown(view);
    }

    private void initPeekTypeWindow(){
        mPeekTypes = new ArrayList<String>();
        mPeekTypes.add(getResources().getString(R.string.config_info_peak_type_positive));
        mPeekTypes.add(getResources().getString(R.string.config_info_peak_type_negative));
        mPeekTypes.add(getResources().getString(R.string.config_info_peak_type_positive_negative));
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter peekTypeAdapter=new CustomSpinnerAdapter(getContext(),mPeekTypes);
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
                ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                configTarget.setPeekType(position);
                mMeltParamterAdapter.setMeltParamter(configTargets);
                collapsePeekType();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    //熔解曲线参数-熔解算法
    private void expandMeltPeakAlg(View view){
        if(mMeltPeakAlgorithms== null && mMeltPeakAlgorithms.isEmpty()){
            return;
        }
        meltAlgPopWindow.showAsDropDown(view);
    }

    private void collapseMeltPeakAlg(){
        if (meltAlgPopWindow != null && meltAlgPopWindow.isShowing()) {
            meltAlgPopWindow.dismiss();
        }
    }
    private void initMeltPeakAlgorithm(){
        mMeltPeakAlgorithms = new ArrayList<String>();
        mMeltPeakAlgorithms.add(getResources().getString(R.string.config_info_meltpeakalgorithm_standard));
        mMeltPeakAlgorithms.add(getResources().getString(R.string.config_info_meltpeakalgorithm_advance));
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter meltAlgorithmAdapter=new CustomSpinnerAdapter(getContext(),mMeltPeakAlgorithms);
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
                ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                configTarget.setMeltPeakAlgorithm(position);
                mMeltParamterAdapter.setMeltParamter(configTargets);
                collapseMeltPeakAlg();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }



    private void initAMPAlgorithm(){
        String[] ampAlgorithmArray= getResources().getStringArray(R.array.AMPAlgorithm);
        mAMPAlgorithmList = new ArrayList<String>(ampAlgorithmArray.length);
        Collections.addAll(mAMPAlgorithmList, ampAlgorithmArray);
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        mAMPAlgSpinerAdapter=new CustomSpinnerAdapter(getContext().getApplicationContext(),mAMPAlgorithmList);
        mAMPAlgorithmLv= (ListView) view.findViewById(R.id.spinner_listview);
        mAMPAlgorithmLv.setAdapter(mAMPAlgSpinerAdapter);
        mAMPAlgWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mAMPAlgWindow.setOutsideTouchable(true);
        mAMPAlgWindow.setFocusable(true);
        mAMPAlgWindow.setContentView(view);
        mAMPAlgWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
//        popupWindow.showAtLocation(view, Gravity.LEFT,0,0);
        mAMPAlgWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseAmpAlg();
            }
        });

        mAMPAlgSpinerAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                binding.ampAlgorithmTv.setText(mAMPAlgorithmList.get(position));
                AMPAlgorithm = position;
                collapseAmpAlg();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }



    //////////////////////////////////////////////////////More操作////////////////////////////////////////////////////////////////

    private void collapseMoreActions(){
        if (mActions != null && mMoreActionWindows.isShowing()) {
            mMoreActionWindows.dismiss();
        }
    }

    //展开扩增曲线算法
    private void expandMoreActions(View view){
        if(mActions== null && mActions.isEmpty()){
            return;
        }
        mMoreActionWindows.showAsDropDown(view);
    }

    private void initMoreActions(){
        mActions = new ArrayList<String>();
        mActions.add(getResources().getString(R.string.running_result_coordinate_scaling));
        mActions.add(getResources().getString(R.string.running_result_coordinate_restoration));
        View view= LayoutInflater.from(getContext()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter mAmpAysTypeAdapter=new CustomSpinnerAdapter(getContext(),mActions);
        ListView mAmpAysTypeLv= (ListView) view.findViewById(R.id.spinner_listview);
        mAmpAysTypeLv.setAdapter(mAmpAysTypeAdapter);
        mMoreActionWindows =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mMoreActionWindows.setOutsideTouchable(true);
        mMoreActionWindows.setFocusable(true);
        mMoreActionWindows.setContentView(view);
        mMoreActionWindows.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mMoreActionWindows.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapseMoreActions();
            }
        });

        mAmpAysTypeAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                switch (position){
                    case 0:
                        //z坐标缩放
                        scaleCoodidate();
                        break;
                    case 1:
                        //坐标还原
                        resetCoodidate();
                        break;
                }
                collapseMoreActions();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    /**
     * 坐标缩放
     */
    private void scaleCoodidate(){
        if(currentView == 1 || currentView == 2 || currentView == 3 || currentView ==4){
            XAxis xAxis = binding.resultCurveChart.getXAxis();
            YAxis yAxis = binding.resultCurveChart.getAxisLeft();
            if(yAxis != null && xAxis != null){
                if(Float.isInfinite(yAxis.getAxisMaximum())){
                    Toast.makeText(getContext(),"没有数据不能进行坐标缩放",Toast.LENGTH_LONG).show();
                    return;
                }else{
                    showCoordidateDialog(currentView,(int)Math.round(xAxis.getAxisMinimum()),(int)Math.round(xAxis.getAxisMaximum())
                            ,nf1.format(yAxis.getAxisMinimum()),nf1.format(yAxis.getAxisMaximum()));
                }
            }
        }
    }

    /**
     * 坐标还原
     */
    private void resetCoodidate(){
        if(currentView == 1 || currentView == 2 || currentView == 3 || currentView == 4){
            YAxis yAxis = binding.resultCurveChart.getAxisLeft();
            XAxis xAxis = binding.resultCurveChart.getXAxis();
            if(yAxis != null){
                if(Float.isInfinite(yAxis.getAxisMaximum())){
                }else{
                    if(currentView == 1){
                        isScaleXRaw = false;
                        drawRawCurve();

                    }else if(currentView == 2){
                        isScaleXAmp = false;
                        drawAMPCurve();

                    }else if(currentView == 3){
                        isScaleXMelt = false;
                        drawMeltCurve();

                    }else if(currentView == 4){
                        isScaleXPeak = false;
                        drawPeakCurve();
                    }
                }
            }
        }
    }
    private CustomLargeDialog coordinateDialog;
    /**
     * 坐标缩放和还原
     */
    private void showCoordidateDialog(int dialog_type,int min_x,int max_x,String min_y,String max_y){
        if(null != coordinateDialog){
            coordinateDialog.dismiss();
            coordinateDialog = null;
        }
        if(null == coordinateDialog){
            coordinateDialog = new CustomLargeDialog(getContext());
            LayoutInflater mInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_custom_coodidate_scale_layout, null);
            CustomAddSubEdit minX_Et = (CustomAddSubEdit) view.findViewById(R.id.dialog_coodidate_min_x_et);
            CustomAddSubEdit maxX_Et = (CustomAddSubEdit) view.findViewById(R.id.dialog_coodidate_max_x_et);
            CustomAddSubEdit minY_Et = (CustomAddSubEdit)  view.findViewById(R.id.dialog_coodidate_y_min_et);
            CustomAddSubEdit maxY_Et = (CustomAddSubEdit)  view.findViewById(R.id.dialog_coodidate_Y_max_et);
            minX_Et.setEditTextValue(String.valueOf(min_x));
            maxX_Et.setEditTextValue(String.valueOf(max_x));
            minY_Et.setEditTextValue(min_y);
            maxY_Et.setEditTextValue(max_y);
            CustomLargeDialog.Builder builder = new CustomLargeDialog.Builder(getContext());
            builder.setContentView(view);
            builder.setTitle(R.string.dialog_title_scale_coordinate);
            builder.setPositiveButton(R.string.dialog_ok,  new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    if(!StringUtil.isEmpty(minX_Et.getEditText())
                            && !StringUtil.isEmpty(maxX_Et.getEditText())
                            && !StringUtil.isEmpty(minY_Et.getEditText())
                            && !StringUtil.isEmpty(maxY_Et.getEditText())){
                        int X_min = StringUtil.convertToInt(minX_Et.getEditText(),min_x);
                        int X_max = StringUtil.convertToInt(maxX_Et.getEditText(),max_x);
                        float Y_min =  StringUtil.convertToFloat(minY_Et.getEditText(),Float.parseFloat(min_y));
                        float Y_max=  StringUtil.convertToFloat(maxY_Et.getEditText(),Float.parseFloat(max_y));
                        XAxis xAxis = binding.resultCurveChart.getXAxis();
                        YAxis yAxis = binding.resultCurveChart.getAxisLeft();
                        xAxis.setAxisMinimum((float) X_min);
                        xAxis.setAxisMaximum((float) X_max);
                        yAxis.setAxisMinimum(Y_min);
                        yAxis.setAxisMaximum(Y_max);
                        switch (dialog_type){
                            case 1:
                                isScaleXRaw = true;
                                min_Raw_X = X_min;
                                max_Raw_X = X_max;
                                min_Raw_Y = Y_min;
                                max_Raw_Y = Y_max;
                                clearChart();
                                drawchart(mCheckRawList, binding.resultCurveChart, "RawCurve");
                                break;
                            case 2:
                                isScaleXAmp = true;
                                min_Amp_X = X_min;
                                max_Amp_X = X_max;
                                min_Amp_Y = Y_min;
                                max_Amp_Y = Y_max;
                                clearChart();
                                drawchart(mCheckAMPList, binding.resultCurveChart, "AmpCurve");
                                break;
                            case 3:
                                isScaleXMelt = true;
                                min_Melt_X = X_min;
                                max_Melt_X = X_max;
                                min_Melt_Y = Y_min;
                                max_Melt_Y = Y_max;
                                clearChart();
                                drawchart(mCheckMeltList, binding.resultCurveChart, "MeltCurve");
                                break;
                            case 4:
                                isScaleXPeak = true;
                                min_Peak_X = X_min;
                                max_Peak_X = X_max;
                                min_Peak_Y = Y_min;
                                max_Peak_Y = Y_max;
                                clearChart();
                                drawchart(mCheckPeakList, binding.resultCurveChart, "PeakCurve");
                                break;
                        }
                        coordinateDialog.dismiss();
                        coordinateDialog = null;
                    }else{
                        return;
                    }
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    coordinateDialog.dismiss();
                    coordinateDialog = null;
                }
            });
            coordinateDialog = builder.create();
            coordinateDialog.setCancelable(false);
            coordinateDialog.setCanceledOnTouchOutside(true);
            coordinateDialog.show();
        }
    }


    ////////////////////////////////////////////////////上传文件或HL7上传//////////////////////////////////////////////////////////////

/*    private void uploadExperimentResult(){
        int uploadMode = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getIntValue(SharedConstant.UPLOAD_MODE,0);
        switch (uploadMode){
            case 0:
                //http
                uploadExResultByHttp();
                break;
            case 1:
                //hl7以太网
                uploadExResultByHL7();
                break;
            case 2:
                //hl7串口
                uploadExResultByHL7Serial();
                break;
        }
    }

    *//**
     * 获取当前的结论路径并上传HL7
     *//*
    private void uploadExResultByHL7(){
        String hl7_Ip = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.HL7_IP,"192.168.0.254");
        String hl7_port = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.HL7_PORT,"63395");

        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        //获取当前的结论的路径
        if(!StringUtil.isEmpty(hl7_Ip) && !StringUtil.isEmpty(hl7_port)){
            int port = StringUtil.convertToInt(hl7_port,63395);
            if(!StringUtil.isEmpty(description)){
                conclusionFilePath = FileUtils.BIOWAY_RESULT_PATH + afterTime + "_" + testItem + "_" + description
                        + "/"
                        + "judgeConclusion.result";
            }else{
                conclusionFilePath = FileUtils.BIOWAY_RESULT_PATH + afterTime + "_" + testItem
                        + "/"
                        + "judgeConclusion.result";
            }
            if(!InternetUtil.isConnect(getActivity().getApplicationContext())){
                Toast.makeText(getActivity().getApplicationContext(),"网络未连接",Toast.LENGTH_LONG).show();
                return;
            }
            if(!StringUtil.isEmpty(conclusionFilePath)){
                showLoadingDialog("正在使用HL7上传中，请等待....");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                       *//* List<String> uploadResults = new ArrayList<String>();
                        uploadResults.clear();
                        uploadResults.add("succeed" +" " + "A1");
                        uploadResults.add("succeed" +" " + "A3");
                        uploadResults.add("succeed" +" " + "A4");
                        uploadResults.add("succeed" +" " + "A5");
                        uploadResults.add("error" +" " + "A8");*//*
                        Log.e("NNN","hl7_Ip"+hl7_Ip);
                        // OruR01Wrapper.test_one_by_one();
                        List<String> uploadResults_01 = OruR01Wrapper.upload_01(conclusionFilePath,hl7_Ip,port);
                        if(null != uploadResults_01 && uploadResults_01.size()>0){
                            Log.e("sendResult_HL7_MLLP","uploadResults_01"+uploadResults_01.size());
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //更新上传结果
                                    hideLoadingDialog();
                                    updateUploadResultByHL7(uploadResults_01);
                                }
                            });
                        }
                    }
                }).start();

            }else{
                isUploading = false;
                mUploadTv.setBackgroundResource(0);
            }
        }
    }

    *//**
     * 上传单个HL7数据
     * @param wellName
     *//*
    private void uploadSingeSingleByHl7(String wellName){

        String hl7_Ip = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.HL7_IP,"192.168.0.254");
        String hl7_port = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.HL7_PORT,"63395");

        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        //获取当前的结论的路径
        if(!InternetUtil.isConnect(getActivity().getApplicationContext())){
            Toast.makeText(getActivity().getApplicationContext(),"网络未连接",Toast.LENGTH_LONG).show();
            return;
        }
        if(!StringUtil.isEmpty(hl7_Ip) && !StringUtil.isEmpty(hl7_port)) {
            int port = StringUtil.convertToInt(hl7_port, 63395);
            if (!StringUtil.isEmpty(description)) {
                conclusionFilePath = FileUtils.BIOWAY_RESULT_PATH + afterTime + "_" + testItem + "_" + description
                        + "/"
                        + "judgeConclusion.result";
            } else {
                conclusionFilePath = FileUtils.BIOWAY_RESULT_PATH + afterTime + "_" + testItem
                        + "/"
                        + "judgeConclusion.result";
            }
            showLoadingDialog("正在上传单个孔位结果，请等待....");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = OruR01Wrapper.uploadSingle(conclusionFilePath,hl7_Ip,port,wellName);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(null != result){
                                updateUploadSingleByHL7(result ,wellName);
                            }
                        }
                    });
                }
            }).start();

        }
    }

    private void updateUploadWellState(TextView textView,String result){
        Drawable successImg = getResources().getDrawable(R.drawable.icon_upload_success);
        successImg.setBounds(0,0,successImg.getMinimumWidth(),successImg.getMinimumHeight());
        Drawable failImg = getResources().getDrawable(R.drawable.icon_upload_fail);
        failImg.setBounds(0,0,failImg.getMinimumWidth(),failImg.getMinimumHeight());
        if(result.contains("succeed")){
            textView.setCompoundDrawables(null,null,successImg,null);
        }else {
            textView.setEnabled(true);
            textView.setCompoundDrawables(null,null,failImg,null);
        }
    }
    *//**
     * 更新单个结果
     *//*
    private void updateUploadSingleByHL7(String result,String wellName){
        if(wellName.equals("A1")){
            updateUploadWellState(mUploadHoleOneTv,result);
        }else if(wellName.equals("A2")){
            updateUploadWellState(mUploadHoleTwoTv,result);
        }else if(wellName.equals("A3")){
            updateUploadWellState(mUploadHoleThreeTv,result);
        }else if(wellName.equals("A4")){
            updateUploadWellState(mUploadHoleFourTv,result);
        }else if(wellName.equals("A5")){
            updateUploadWellState(mUploadHoleFiveTv,result);
        }else if(wellName.equals("A6")){
            updateUploadWellState(mUploadHoleSixTv,result);
        }else if(wellName.equals("A7")){
            updateUploadWellState(mUploadHoleSevenTv,result);
        }else if(wellName.equals("A8")){
            updateUploadWellState(mUploadHoleEightTv,result);
        }
    }

    *//**
     * 更新HL7的上传结果
     *//*
    private void updateUploadResultByHL7(List<String> uploadResults){
        int success_count = 0;
        mHoleLayout.setVisibility(View.GONE);
        mUploadHoleLayout.setVisibility(View.VISIBLE);
        Drawable successImg = getResources().getDrawable(R.drawable.icon_upload_success);
        successImg.setBounds(0,0,successImg.getMinimumWidth(),successImg.getMinimumHeight());
        Drawable failImg = getResources().getDrawable(R.drawable.icon_upload_fail);
        failImg.setBounds(0,0,failImg.getMinimumWidth(),failImg.getMinimumHeight());
        for(String result : uploadResults){
            if(result.contains("A1")){
                if(result.contains("succeed")){
                    success_count++;
                    mUploadHoleOneTv.setCompoundDrawables(null,null,successImg,null);
                }else{
                    mUploadHoleOneTv.setEnabled(true);
                    mUploadHoleOneTv.setCompoundDrawables(null,null,failImg,null);
                }
            }
            else if(result.contains("A2")){
                if(result.contains("succeed")){
                    success_count++;
                    mUploadHoleTwoTv.setCompoundDrawables(null,null,successImg,null);
                }else{
                    mUploadHoleTwoTv.setEnabled(true);
                    mUploadHoleTwoTv.setCompoundDrawables(null,null,failImg,null);
                }
            }else if(result.contains("A3")){
                if(result.contains("succeed")){
                    success_count++;
                    mUploadHoleThreeTv.setCompoundDrawables(null,null,successImg,null);
                }else{
                    mUploadHoleThreeTv.setEnabled(true);
                    mUploadHoleThreeTv.setCompoundDrawables(null,null,failImg,null);
                }
            }else if(result.contains("A4")){
                if(result.contains("succeed")){
                    success_count++;
                    mUploadHoleFourTv.setCompoundDrawables(null,null,successImg,null);
                }else{
                    mUploadHoleFourTv.setEnabled(true);
                    mUploadHoleFourTv.setCompoundDrawables(null,null,failImg,null);
                }
            }else if(result.contains("A5")){
                if(result.contains("succeed")){
                    success_count++;
                    mUploadHoleFiveTv.setCompoundDrawables(null,null,successImg,null);
                }else{
                    mUploadHoleFiveTv.setEnabled(true);
                    mUploadHoleFiveTv.setCompoundDrawables(null,null,failImg,null);
                }
            }else if(result.contains("A6")){
                if(result.contains("succeed")){
                    success_count++;
                    mUploadHoleSixTv.setCompoundDrawables(null,null,successImg,null);
                }else{
                    mUploadHoleSixTv.setEnabled(true);
                    mUploadHoleSixTv.setCompoundDrawables(null,null,failImg,null);
                }
            }else if(result.contains("A7")){
                if(result.contains("succeed")){
                    success_count++;
                    mUploadHoleSevenTv.setCompoundDrawables(null,null,successImg,null);
                }else{
                    mUploadHoleSevenTv.setEnabled(true);
                    mUploadHoleSevenTv.setCompoundDrawables(null,null,failImg,null);
                }
            }else if(result.contains("A8")){
                if(result.contains("succeed")){
                    success_count++;
                    mUploadHoleEightTv.setCompoundDrawables(null,null,successImg,null);
                }else{
                    mUploadHoleEightTv.setEnabled(true);
                    mUploadHoleEightTv.setCompoundDrawables(null,null,failImg,null);
                }
            }
        }
        if(success_count == uploadResults.size()){
            isUploading = false;
            mUploadTv.setBackgroundResource(0);
        }
    }

    private void setUploadGoneState(){
        mUploadHoleLayout.setVisibility(View.GONE);
        isUploading = false;
        mUploadTv.setBackgroundResource(0);
    }

    *//**
     * 上报结果数据和文件
     *//*
    private void uploadExResultByHttp(){
        String url = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.UPLOAD_URL,
                        InternetUtil.getLocalIpAddress(getActivity().getApplicationContext()));
        String port = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getStringValue(SharedConstant.PORT,"80");

        String UploadUrl = "http://"+url+":"+port+"/upload";
        //String UploadUrl = "http://www.baidu.com";
        String file_path = getSecondAyseFilePath();
        File file = new File(file_path);
        if(StringUtil.isEmpty(file_path)){
            Toast.makeText(getActivity().getApplicationContext(),
                    getResources().getString(R.string.result_ex_detail_toast_no_file_upload), Toast.LENGTH_SHORT).show();
            return;
        }else {
            Log.i("BBB","CONNECT" + InternetUtil.isConnect(getActivity().getApplicationContext()));
            if(!InternetUtil.isConnect(getActivity().getApplicationContext())){
                Toast.makeText(getActivity().getApplicationContext(),
                        getResources().getString(R.string.result_ex_detail_no_network), Toast.LENGTH_SHORT).show();
                return;
            }
            if (null != SLANService.INSTANCE) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //正在打开文件获取
                        Experiment experiment = SLANService.INSTANCE.openExperiment(file_path);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(experiment != null){
                                    SLANManager.getInstance().uploadSLANResult(experiment,file,UploadUrl);
                                }else{
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            getResources().getString(R.string.result_ex_detail_toast_no_file_upload),
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        });
                    }
                }).start();
            }
        }

    }

    private void uploadExResultByHL7Serial(){

    }*/

    private void filterAndShowSampleWells(List<String> targetWells){
        for(String wellName :targetWells){
            switch (wellName){
                case "A1":
                    if(isA1Enable){
                        isA1Check = true;
                        setHodeState(isA1Check, binding.sampleEightA1Tv);
                    }
                    break;

                case "A2":
                    if(isA2Enable){
                        isA2Check = true;
                        setHodeState(isA2Check, binding.sampleSevenA2Tv);
                    }
                    break;

                case "A3":
                    if(isA3Enable){
                        isA3Check = true;
                        setHodeState(isA3Check, binding.sampleSixA3Tv);
                    }

                    break;
                case "A4":
                    if(isA4Enable){
                        isA4Check = true;
                        setHodeState(isA4Check, binding.sampleFiveA4Tv);
                    }

                    break;
                case "A5":
                    if(isA5Enable){
                        isA5Check = true;
                        setHodeState(isA5Check, binding.sampleFourA5Tv);
                    }
                    break;

                case "A6":
                    if(isA6Enable){
                        isA6Check = true;
                        setHodeState(isA6Check, binding.sampleThreeA6Tv);
                    }
                    break;

                case "A7":
                    if(isA7Enable){
                        isA7Check = true;
                        setHodeState(isA7Check, binding.sampleTwoA7Tv);
                    }
                    break;

                case "A8":
                    if(isA8Enable){
                        isA8Check = true;
                        setHodeState(isA8Check, binding.sampleOneA8Tv);
                    }
                    break;

                case "B1":
                    if(isB1Enable){
                        isB1Check = true;
                        setHodeState(isB1Check, binding.sampleEightB1Tv);
                    }
                    break;

                case "B2":
                    if(isB2Enable){
                        isB2Check = true;
                        setHodeState(isB2Check, binding.sampleSevenB2Tv);
                    }
                    break;

                case "B3":
                    if(isB3Enable){
                        isB3Check = true;
                        setHodeState(isB3Check, binding.sampleSixB3Tv);
                    }
                    break;

                case "B4":
                    if(isB4Enable){
                        isB4Check = true;
                        setHodeState(isB4Check, binding.sampleFiveB4Tv);
                    }
                    break;

                case "B5":
                    if(isB5Enable){
                        isB5Check = true;
                        setHodeState(isB5Check, binding.sampleFourB5Tv);
                    }
                    break;

                case "B6":
                    if(isB6Enable){
                        isB6Check = true;
                        setHodeState(isB6Check, binding.sampleThreeB6Tv);
                    }
                    break;

                case "B7":
                    if(isB7Enable){
                        isB7Check = true;
                        setHodeState(isB7Check, binding.sampleTwoB7Tv);
                    }
                    break;

                case "B8":
                    if(isB8Enable){
                        isB8Check = true;
                        setHodeState(isB8Check, binding.sampleOneB8Tv);
                    }
                    break;
            }
        }
        updateWellResult();//更新孔位绘制图表
    }



    /**
     * 筛选指定样本的孔位数据并重新绘制图表
     * @param targetWells 目标孔位列表（如样本1的A7/A8/B7/B8）
     */
    private void filterAndClearSampleWells(List<String> targetWells) {

        // 2. 选中目标孔位并更新UI
        for (String wellName : targetWells) {
            switch (wellName) {
                case "A1":
                    if(isA1Enable){
                        isA1Check = false;
                        setHodeState(isA1Check, binding.sampleEightA1Tv);
                    }
                    break;

                case "A2":
                    if(isA2Enable){
                        isA2Check = false;
                        setHodeState(isA2Check, binding.sampleSevenA2Tv);
                    }
                    break;

                case "A3":
                    if(isA3Enable){
                        isA3Check = false;
                        setHodeState(isA3Check, binding.sampleSixA3Tv);
                    }

                    break;
                case "A4":
                    if(isA4Enable){
                        isA4Check = false;
                        setHodeState(isA4Check, binding.sampleFiveA4Tv);
                    }

                    break;
                case "A5":
                    if(isA5Enable){
                        isA5Check = false;
                        setHodeState(isA5Check, binding.sampleFourA5Tv);
                    }
                    break;

                case "A6":
                    if(isA6Enable){
                        isA6Check = false;
                        setHodeState(isA6Check, binding.sampleThreeA6Tv);
                    }
                    break;

                case "A7":
                    if(isA7Enable){
                        isA7Check = false;
                        setHodeState(isA7Check, binding.sampleTwoA7Tv);
                    }
                    break;

                case "A8":
                    if(isA8Enable){
                        isA8Check = false;
                        setHodeState(isA8Check, binding.sampleOneA8Tv);
                    }
                    break;

                case "B1":
                    if(isB1Enable){
                        isB1Check = false;
                        setHodeState(isB1Check, binding.sampleEightB1Tv);
                    }
                    break;

                case "B2":
                    if(isB2Enable){
                        isB2Check = false;
                        setHodeState(isB2Check, binding.sampleSevenB2Tv);
                    }
                    break;

                case "B3":
                    if(isB3Enable){
                        isB3Check = false;
                        setHodeState(isB3Check, binding.sampleSixB3Tv);
                    }
                    break;

                case "B4":
                    if(isB4Enable){
                        isB4Check = false;
                        setHodeState(isB4Check, binding.sampleFiveB4Tv);
                    }
                    break;

                case "B5":
                    if(isB5Enable){
                        isB5Check = false;
                        setHodeState(isB5Check, binding.sampleFourB5Tv);
                    }
                    break;

                case "B6":
                    if(isB6Enable){
                        isB6Check = false;
                        setHodeState(isB6Check, binding.sampleThreeB6Tv);
                    }
                    break;

                case "B7":
                    if(isB7Enable){
                        isB7Check = false;
                        setHodeState(isB7Check, binding.sampleTwoB7Tv);
                    }
                    break;

                case "B8":
                    if(isB8Enable){
                        isB8Check = false;
                        setHodeState(isB8Check, binding.sampleOneB8Tv);
                    }
                    break;
            }
        }

        // 3. 更新孔位结果并重新绘制图表
        updateWellResult();
    }

     private void resetAllWellSelection() {
        // 重置所有孔位选中
        isA1Check = isA2Check = isA3Check = isA4Check =
                isA5Check = isA6Check = isA7Check = isA8Check = false;
        isA1Enable = isA2Enable = isA3Enable = isA4Enable = isA5Enable
                = isA6Enable = isA7Enable = isA8Enable = false;

        isB1Check = isB2Check = isB3Check = isB4Check =
                isB5Check = isB6Check = isB7Check = isB8Check = false;
        isB1Enable = isB2Enable = isB3Enable = isB4Enable = isB5Enable = isB6Enable
                = isB7Enable = isB8Enable = false;

        // 重置样本选中状态
        isSampleOneSelected = false;
        isSampleTwoSelected = false;
        isSampleThreeSelected = false;
        isSampleFourSelected = false;
        isSampleFiveSelected = false;
        isSampleSixSelected = false;
        isSampleSevenSelected = false;
        isSampleEightSelected = false;
    }

    private void resetAllWellUi() {
        setHodeState(false, binding.sampleEightA1Tv);
        setHodeState(false, binding.sampleSevenA2Tv);
        setHodeState(false, binding.sampleSixA3Tv);
        setHodeState(false, binding.sampleFiveA4Tv);

        setHodeState(false, binding.sampleFourA5Tv);
        setHodeState(false, binding.sampleThreeA6Tv);
        setHodeState(false, binding.sampleTwoA7Tv);
        setHodeState(false, binding.sampleOneA8Tv);

        setHodeState(false, binding.sampleEightB1Tv);
        setHodeState(false, binding.sampleSevenB2Tv);
        setHodeState(false, binding.sampleSixB3Tv);
        setHodeState(false, binding.sampleFiveB4Tv);

        setHodeState(false, binding.sampleFourB5Tv);
        setHodeState(false, binding.sampleThreeB6Tv);
        setHodeState(false, binding.sampleTwoB7Tv);
        setHodeState(false, binding.sampleOneB8Tv);
    }

    //===============================绘制曲线======================================//
    // 抽取通道颜色方法，消除大量if-else
    private int getChannelColor(int channelNo) {
        switch (channelNo) {
            case 1: return PcrValue.colorline[0];
            case 2: return PcrValue.colorline[1];
            case 3: return PcrValue.colorline[2];
            case 4: return PcrValue.colorline[3];
            case 5: return PcrValue.colorline[4];
            case 6: return PcrValue.colorline[5];
            default: return PcrValue.colorline[0];
        }
    }


}