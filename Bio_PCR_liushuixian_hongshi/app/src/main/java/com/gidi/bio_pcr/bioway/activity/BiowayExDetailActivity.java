package com.gidi.bio_pcr.bioway.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
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

import com.alibaba.fastjson.JSON;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.adapter.PeakTypeOptimizeAdapter;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.JudgeConclusion;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.PeaktypeOptimization;
import com.gidi.bio_pcr.bean.QueryCondition;
import com.gidi.bio_pcr.bean.Result;
import com.gidi.bio_pcr.bean.WellInfo;
import com.gidi.bio_pcr.bioway.adapter.BiowayResultAdapter;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.db.BioWayWellResult;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;

import com.gidi.bio_pcr.mgr.FtpUploadManager;
import com.gidi.bio_pcr.pcr.OnDetailExCallback;
import com.gidi.bio_pcr.pcr.OnExperimentCallback;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;

import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;

import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.ExperimentData.Peak;
import com.winters.boweilibrary.BoWei.ExperimentData.TargetResult;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.winters.boweilibrary.BoWei.bean.BiowayExlResult;
import com.gidi.bio_pcr.bioway.BiowayManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BiowayExDetailActivity extends BaseActivity implements View.OnClickListener , OnDetailExCallback {

    private final String TAG = "BiowayExDetailActivity";
    private RelativeLayout main_root_layout;
    private RelativeLayout mTitleLayout;
    private ImageView mBackImg;
    private TextView mScreenShotTv,mSaveJsonTv,mMoreActionTv;
    private CustomTitleText mTestTimeTv;
    private TextView mTestItemTv;
    private LinearLayout mChannelLayout,mHoleLayout;
    private CustomCheckText mFAM_ChannelTv, mHEX_ChannelTv, mROX_ChannelTv, mCY5_ChannelTv;
    private boolean isFAMCheck = false;
    private boolean isHEXCheck = false;
    private boolean isROXCheck = false;
    private boolean isCY5Check = false;

    private TextView mHoleOneTv, mHoleTwoTv, mHoleThreeTv, mHoleFourTv, mHoleFiveTv, mHoleSixTv, mHoleSevenTv, mHoleEightTv;
    private boolean isOneCheck, isTwoCheck, isThreeCheck, isFourCheck, isFiveheck, isSixCheck, isSevenCheck, isEightCheck = false;

    private String curTemplatePath;

    private LineChart resultChart;
    private RadioGroup mCurveChartGg;
    //原始数据

    private List<BiowayCurveList> mRawCurveList;//原始数据

    private List<BiowayCurveList> mShowCurveList,mCheckRawList;

    private List<BiowayCurveList> mAMPCurvList,mCheckAMPList;

    private List<BiowayCurveList> mMeltCurvList,mCheckMeltList;

    private List<BiowayCurveList> mPeakCurvList,mCheckPeakList;

    private List<BiowayExlResult> mExlResults,mCheckExlResults;//对应的是e选择rox，fam等的结果列表
    private List<WellInfo> mWellInfos;
    private List<File> localList;

    private LinearLayout mResultChartLayout;
    private HorizontalScrollView mResultExlLayout;

    private boolean isChartVisible = true;
    private boolean isExcelVisible = false;
    private boolean isSecAysVisible = false;
    private boolean isAllChanelCheck = false;
    private boolean isAllCheck = false;

    private TextView mAllHoleCk;
    private CustomCheckText mAllChannelCk;
    private ListView mResultListView;
    private BiowayResultAdapter resultAdapter;
    private RadioButton mRawCurve_Rb;
    //阈值和峰高阈值
    private ResultSearchAsyncTask searchAsyncTask;
    private List<BioWayWellResult> wellResults;
    private List<BioWayWellResult> checkWellResult;
    private int currentView = 1;

    private boolean isFamThreshold,isHexThreshold,isRoxThreshold,iscy5Threshold = false;

    private double famThreshold,hexThreshold,roxThreshold,cy5Threshold;

    private boolean isMelt = false;//是熔解还是扩增
    private TextView mSecAnalyseTv;
    private String startTime, testItem,saveFileDirPath;
    private CustomDialog updateDbDialog;
    private List<BiowayCurveList> mOriginRaw = new ArrayList<BiowayCurveList>();
    private List<BiowayCurveList> mOriginAMP = new ArrayList<BiowayCurveList>();
    private List<BiowayCurveList> mOriginMelt = new ArrayList<BiowayCurveList>();
    private List<BiowayCurveList> mOriginPeak= new ArrayList<BiowayCurveList>();
    private List<BiowayExlResult> mOriginExResults = new ArrayList<BiowayExlResult>();
    private List<ConfigTarget> configTargets;
    private RelativeLayout mSecAnalyseLayout;
    private TextView mBasicParamTitleTv,mMeltParamTitleTv,mAMPAlgorithmTv;
    private LinearLayout mAmpParamterLayout,mMeltParamterLayout;//扩增曲线参数，熔解曲线参数
    private ListView mAmpAlgorithmLv,mMeltParamterLv;//扩增曲线参数列表，熔解曲线参数列表
    private AmpAlgorithmAdapter mAmpAlgorithmAdapter;
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

    private String description;

    //峰高优化
    private ListView mPeakHighOptimizeLv;
    private PeakTypeOptimizeAdapter peakTypeOptimizeAdapter;
    private List<PeaktypeOptimization> optimizations;
    private List<Boolean> mEnalbeOptimations;
    private PopupWindow mEnablesPopWindow;
    private int optimationPos;//优化的位置

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_bioway_ex_detail;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        main_root_layout = (RelativeLayout) findViewById(R.id.bioway_ex_detail_root_layout);
        mTitleLayout = (RelativeLayout) findViewById(R.id.bioway_ex_result_detail_title_layout);
        mBackImg = (ImageView) mTitleLayout.findViewById(R.id.tab_back_img);
        TextView titleTv = (TextView) mTitleLayout.findViewById(R.id.tab_title_tv);
        titleTv.setText(R.string.result_record);
        mScreenShotTv = (TextView) findViewById(R.id.bioway_result_screenshot_tv);
        mSaveJsonTv= (TextView) findViewById(R.id.bioway_result_save_json_tv);
        mTestItemTv = (TextView) findViewById(R.id.bioway_result_ex_testitem__tv);
        mTestTimeTv = (CustomTitleText) findViewById(R.id.bioway_result_ex_detail_time_tv);
        resultChart = (LineChart) findViewById(R.id.bioway_ex_result_curve_chart);
        if (null != resultChart) {
            resultChart.setNoDataText("");
        }
        mHoleLayout = (LinearLayout)findViewById(R.id.bioway_result_hole_layout);
        mChannelLayout = (LinearLayout)findViewById(R.id.bioway_ex_result_chanel_layout) ;
        mFAM_ChannelTv = (CustomCheckText) findViewById(R.id.bioway_ex_result_fam_tv);
        mHEX_ChannelTv = (CustomCheckText) findViewById(R.id.bioway_ex_result_hex_tv);
        mROX_ChannelTv = (CustomCheckText) findViewById(R.id.bioway_ex_result_rox_tv);
        mCY5_ChannelTv = (CustomCheckText) findViewById(R.id.bioway_ex_result_cy5_tv);

        mAllChannelCk = (CustomCheckText) findViewById(R.id.bioway_ex_result_all_channel_check_tv);
        mHoleOneTv = (TextView) findViewById(R.id.bioway_result_hole_one_btn);
        mHoleTwoTv = (TextView) findViewById(R.id.bioway_result_hole_two_btn);
        mHoleThreeTv = (TextView) findViewById(R.id.bioway_result_hole_three_btn);
        mHoleFourTv = (TextView) findViewById(R.id.bioway_result_hole_four_btn);
        mHoleFiveTv = (TextView) findViewById(R.id.bioway_result_hole_five_btn);
        mHoleSixTv = (TextView) findViewById(R.id.bioway_result_hole_six_btn);
        mHoleSevenTv = (TextView) findViewById(R.id.bioway_result_hole_seven_btn);
        mHoleEightTv = (TextView) findViewById(R.id.bioway_result_hole_eight_btn);
        mCurveChartGg = (RadioGroup) findViewById(R.id.bioway_ex_result_curve_rb_layout);
        mRawCurve_Rb = (RadioButton) findViewById(R.id.bioway_ex_result_rawcurve_rb);
        mRawCurve_Rb.setChecked(true);
        mResultListView = (ListView) findViewById(R.id.bioway_ex_result_list_view);
        resultAdapter = new BiowayResultAdapter(getApplicationContext());
        mSecAnalyseTv = (TextView) findViewById(R.id.bioway_ex_result_sec_analyse_tv);

        mResultExlLayout = (HorizontalScrollView) findViewById(R.id.bioway_ex_result_list_scrollview);
        mResultChartLayout = (LinearLayout) findViewById(R.id.bioway_ex_curve_chart_layout);
        mAllHoleCk = (TextView) findViewById(R.id.bioway_result_all_hole_ck);

        //二次分析页面
        mSecAnalyseTv.setVisibility(View.INVISIBLE);
        mSecAnalyseLayout = (RelativeLayout)findViewById(R.id.bioway_ex_result_sec_analyse_layout);
        mBasicParamTitleTv = (TextView) findViewById(R.id.bioway_ex_result_basic_param_title_tv);
        mMeltParamTitleTv = (TextView) findViewById(R.id.bioway_ex_result_melt_param_title_tv);
        mAMPAlgorithmTv = (TextView)findViewById(R.id.bioway_ex_result_sec_analyse_amp_algorithm_tv);//扩增曲线算法
        initAmpAlgorithmSpinner();


        mAmpParamterLayout = (LinearLayout)findViewById(R.id.bioway_ex_result_sec_analyse_amp_param_layout);
        mAmpAlgorithmLv = (ListView) findViewById(R.id.bioway_ex_result_sec_analyse_amp_algorithm_lv);
        mAmpAlgorithmAdapter = new AmpAlgorithmAdapter(getApplicationContext());
        mAmpAlgorithmLv.setAdapter(mAmpAlgorithmAdapter);
        mMeltParamterLayout = (LinearLayout) findViewById(R.id.bioway_ex_result_sec_analyse_melt_param_layout);
        mMeltParamterLv = (ListView) findViewById(R.id.bioway_ex_result_sec_analyse_melt_paramter_lv);
        mMeltParamterAdapter = new MeltParamterAdapter(getApplicationContext());
        mMeltParamterLv.setAdapter(mMeltParamterAdapter);
        mMoreActionTv = (TextView) findViewById(R.id.bioway_ex_result_more_tv);

        //峰型优化
        mPeakHighOptimizeLv = (ListView) findViewById(R.id.bioway_ex_result_sec_analyse_optimization_lv);
        peakTypeOptimizeAdapter = new PeakTypeOptimizeAdapter(getApplicationContext());
        mPeakHighOptimizeLv.setAdapter(peakTypeOptimizeAdapter);
        initEnableOptimation();
    }


    @Override
    protected void initListener() {
        mScreenShotTv.setOnClickListener(this);
        mSaveJsonTv.setOnClickListener(this);
        mMoreActionTv.setOnClickListener(this);
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mAllChannelCk.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isAllChanelCheck = isCheck;
                if (isAllChanelCheck) {
                    isHEXCheck = true;
                    isFAMCheck = true;
                    isROXCheck = true;
                    isCY5Check = true;
                } else {
                    isHEXCheck = false;
                    isFAMCheck = false;
                    isROXCheck = false;
                    isCY5Check = false;
                }
                setChannelState();
                MessageEvent mAllChanelMsg = new MessageEvent();
                mAllChanelMsg.setMsgType(MsgType.MSG_SHOW_ALL_CHANEL_RESULT);
                EventBus.getDefault().post (mAllChanelMsg);
            }

        });


        mFAM_ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isFAMCheck = isCheck;
                if (!isFAMCheck) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isHEXCheck ){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mFamMsg = new MessageEvent();
                mFamMsg.setMsgType(MsgType.MSG_SHOW_FAM_CHANNEL_CURVE);
                EventBus.getDefault().post (mFamMsg);
            }
        });

        mHEX_ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isHEXCheck = isCheck;
                if (!isHEXCheck) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isFAMCheck ){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mHEXMsg = new MessageEvent();
                mHEXMsg.setMsgType(MsgType.MSG_SHOW_HEX_CHANNEL_CURVE);
                EventBus.getDefault().post (mHEXMsg);

            }
        });

        mROX_ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isROXCheck = isCheck;
                if (!isROXCheck) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isFAMCheck && isCY5Check && isHEXCheck){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mROXMsg = new MessageEvent();
                mROXMsg.setMsgType(MsgType.MSG_SHOW_ROX_CHANNEL_CURVE);
                EventBus.getDefault().post (mROXMsg);
            }
        });

        mCY5_ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isCY5Check = isCheck;
                if (!isCY5Check) {
                    if (isAllChanelCheck) {
                        isAllChanelCheck = false;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isHEXCheck && isFAMCheck ){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mCY5Msg = new MessageEvent();
                mCY5Msg.setMsgType(MsgType.MSG_SHOW_CY5_CHANNEL_CURVE);
                EventBus.getDefault().post (mCY5Msg);

            }
        });


        mAllHoleCk.setOnClickListener(this);
        mHoleOneTv.setOnClickListener(this);
        mHoleTwoTv.setOnClickListener(this);
        mHoleThreeTv.setOnClickListener(this);
        mHoleFourTv.setOnClickListener(this);
        mHoleFiveTv.setOnClickListener(this);
        mHoleSixTv.setOnClickListener(this);
        mHoleSevenTv.setOnClickListener(this);
        mHoleEightTv.setOnClickListener(this);
        mSecAnalyseTv.setOnClickListener(this);
        mBasicParamTitleTv.setOnClickListener(this);
        mMeltParamTitleTv.setOnClickListener(this);
        mAMPAlgorithmTv.setOnClickListener(this);

        BiowayManager.getInstance().setOnDetailExCallback(this);
        mCurveChartGg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.bioway_ex_result_rawcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isSecAysVisible = false;
                        isExcelVisible = false;
                    }
                    if(isChartVisible){
                        mSecAnalyseTv.setVisibility(View.INVISIBLE);
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 1;
                    getAllRawCurve();
                    drawRawCurve();
                } else if (checkedId == R.id.bioway_ex_result_ampcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        mSecAnalyseTv.setVisibility(View.INVISIBLE);
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 2;
                    getAllAPMCurve();
                    drawAMPCurve();
                } else if (checkedId == R.id.bioway_ex_result_meltcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        mSecAnalyseTv.setVisibility(View.INVISIBLE);
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 3;
                    getAllMelturve();
                    drawMeltCurve();
                } else if (checkedId == R.id.bioway_ex_result_peakcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        mSecAnalyseTv.setVisibility(View.INVISIBLE);
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 4;
                    getAllPeakCurve();
                    drawPeakCurve();
                } else if (checkedId == R.id.bioway_ex_result_list_rb) {
                    currentView = 5;
                    if (!isExcelVisible) {
                        isExcelVisible = true;
                        isChartVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isExcelVisible){
                        mSecAnalyseTv.setVisibility(View.INVISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.VISIBLE);
                        mResultChartLayout.setVisibility(View.GONE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                        getShowExlResults();
                        MessageEvent update = new MessageEvent();
                        update.setMsgType(MsgType.MSG_UPDATE_EXPERIMENT_EXCEL);
                        EventBus.getDefault().post(update);
                    }

                }else if(checkedId == R.id.bioway_ex_result_sec_analyse_rb){
                    //二次分析界面
                    //参数设置
                    if (!isSecAysVisible) {
                        isChartVisible = false;
                        isExcelVisible = false;
                        isSecAysVisible = true;
                    }
                    if(isSecAysVisible){
                        mSecAnalyseTv.setVisibility(View.VISIBLE);
                        mChannelLayout.setVisibility(View.INVISIBLE);
                        mHoleLayout.setVisibility(View.INVISIBLE);
                        mSecAnalyseLayout.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.GONE);

                        String second_file_path = getSecondAyseFilePath();
                        if(StringUtil.isEmpty(second_file_path)){
                            Toast.makeText(getApplicationContext(), "没有二次分析文件", Toast.LENGTH_SHORT).show();
                            return;
                        }else{
                            if (null != BoWeiService.getInstance()) {
                                if(null != mSec_Experiment){
                                    mSec_Experiment = null;
                                }
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        //正在打开文件获取
                                        Experiment openExperiment = BoWeiService.getInstance().openExperiment(second_file_path);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if(openExperiment != null){
                                                    mSec_Experiment = openExperiment;
                                                    getBiowayExParameter(openExperiment);
                                                }else{
                                                    Toast.makeText(getApplicationContext(), "无法进行二次分析",
                                                            Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                            }
                                        });
                                    }
                                }).start();
                            }else{
                                Toast.makeText(getApplicationContext(), "无法进行二次分析", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        });

        mAmpAlgorithmAdapter.setOnItemClickListener(new AmpAlgorithmAdapter.OnItemClickListener() {
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
                    mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
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

        //峰型优化
        peakTypeOptimizeAdapter.setOnItemClickListener(new PeakTypeOptimizeAdapter.OnItemClickListener() {
            @Override
            public void onEnableOptimization(View view, int position) {
                //是否启用true/false
                optimationPos = position;
                expandEnableOptimation(view);
            }

            @Override
            public void onEnhanceLower(View view, int position) {
                optimationPos = position;
                if(null != optimizations && optimizations.size()>0){
                    double enhance_lower = optimizations.get(position).getEnhance_lower();
                    showNumKeyboardDialog(8,view,nf2.format(enhance_lower),-1);
                }
            }

            @Override
            public void onEnhanceUpper(View view, int position) {
                optimationPos = position;
                if(null != optimizations && optimizations.size()>0){
                    double enhance_upper = optimizations.get(position).getEnhance_upper();
                    showNumKeyboardDialog(9,view,nf2.format(enhance_upper),-1);
                }
            }

            @Override
            public void onEnhanceGain(View view, int position) {
                //增强增益
                optimationPos = position;
                if(null != optimizations && optimizations.size()>0){
                    double enhanceGain = optimizations.get(position).getEnhanceGain();
                    showNumKeyboardDialog(10,view,nf2.format(enhanceGain),-1);
                }
            }

            @Override
            public void onWeaken(View view, int position) {
                //削弱峰高阈值
                optimationPos = position;
                if(null != optimizations && optimizations.size()>0){
                    double weaken = optimizations.get(position).getWeaken();
                    showNumKeyboardDialog(11,view,nf2.format(weaken),-1);
                }
            }

            @Override
            public void onWeakenGain(View view, int position) {
                //削弱增益
                optimationPos = position;
                if(null != optimizations && optimizations.size()>0){
                    double weakenGain = optimizations.get(position).getWeakenGain();
                    showNumKeyboardDialog(12,view,nf2.format(weakenGain),-1);
                }
            }

            @Override
            public void onSlopeThreshold(View view, int position) {
                //斜率阈值
                optimationPos = position;
                if(null != optimizations && optimizations.size()>0){
                    double slopeThreshold = optimizations.get(position).getSlopeThreshold();
                    showNumKeyboardDialog(13,view,nf2.format(slopeThreshold),-1);
                }
            }
        });
    }

    @Override
    protected void initData() {
        wellResults = new ArrayList<BioWayWellResult>();
        mRawCurveList = new ArrayList<BiowayCurveList>();
        mCheckRawList = new ArrayList<BiowayCurveList>();

        mAMPCurvList = new ArrayList<BiowayCurveList>();
        mCheckAMPList = new ArrayList<BiowayCurveList>();

        mMeltCurvList = new ArrayList<BiowayCurveList>();
        mCheckMeltList = new ArrayList<BiowayCurveList>();

        mPeakCurvList = new ArrayList<BiowayCurveList>();
        mCheckPeakList = new ArrayList<BiowayCurveList>();

        mExlResults = new ArrayList<BiowayExlResult>();
        mCheckExlResults = new ArrayList<BiowayExlResult>();
        mShowCurveList = new ArrayList<BiowayCurveList>();
        resultAdapter.setResultList(mCheckExlResults);
        mResultListView.setAdapter(resultAdapter);
        checkWellResult = new ArrayList<BioWayWellResult>();
        mWellInfos = new ArrayList<WellInfo>();
        localList = new ArrayList<File>();
        configTargets = new ArrayList<ConfigTarget>();
        optimizations = new ArrayList<PeaktypeOptimization>();
        initPeekTypeWindow();
        initAmpAysTypeWindow();
        initOptimizationModeWindow();
        initAmpDigitalFilter();
        initMeltPeakAlgorithm();
        initMoreActions();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerEventBus();
        initData();
        setChannelState();

        ExperimentModel experimentModel = (ExperimentModel) getIntent().getSerializableExtra("item");
        if (null != experimentModel) {
            startTime = experimentModel.getStartTime();
            testItem = experimentModel.getTestItem();
            saveFileDirPath = experimentModel.getFilePath();
            description = experimentModel.getDescription();
            mTestTimeTv.setContentText(startTime);
            mTestItemTv.setText(testItem);
            //是全流程还是单PCR
            int Type = experimentModel.getType();
            String curTempDir = experimentModel.getTemplateDirName();
            switch (Type){
                case 0:
                    curTemplatePath = FileUtils.FULL_PROCESS_TEMPLATE_PATH+ File.separator + curTempDir
                            + File.separator ;
                    break;
                case 2:
                    curTemplatePath = FileUtils.SINGLE_PCR_TEMPLATE_PATH+ File.separator + curTempDir
                            + File.separator ;
                    break;
                default:
                    break;
            }


        }

    }

    private void initMain(){
        initFiles();
        isScaleXRaw = false;
        isScaleXAmp = false;
        isScaleXMelt = false;
        isScaleXPeak = false;
        //查询实验结果详情
        queryExDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initMain();
    }

    @Override
    protected void onStop() {
        super.onStop();
        BiowayManager.getInstance().setExperiment(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BiowayManager.getInstance().releaseOnDetailExCallback();
        unRegisterEventBus();
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

        if(null != mEnablesPopWindow){
            mEnablesPopWindow.dismiss();
            mEnablesPopWindow = null;
        }
        if(null != optimizations){
            optimizations.clear();
            optimizations = null;
        }
        if(null != mEnalbeOptimations){
            mEnalbeOptimations.clear();
            mEnalbeOptimations = null;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bioway_result_screenshot_tv) {
            screenShot();
            // screenShotAmpImg();
        }else if(id == R.id.bioway_result_save_json_tv){
            saveExJsonFile();
        } else if (id == R.id.bioway_result_hole_one_btn) {
            isOneCheck = !isOneCheck;
            if (!isOneCheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHoleState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }
            setHoleState(isOneCheck, (TextView) v);
            updateWellResult();
        } else if (id == R.id.bioway_result_hole_two_btn) {
            isTwoCheck = !isTwoCheck;
            if (!isTwoCheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHoleState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }
            setHoleState(isTwoCheck, (TextView) v);
            updateWellResult();
        } else if (id == R.id.bioway_result_hole_three_btn) {
            isThreeCheck = !isThreeCheck;
            if (!isThreeCheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHoleState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }
            setHoleState(isThreeCheck, (TextView) v);
            updateWellResult();
        } else if (id == R.id.bioway_result_hole_four_btn) {
            isFourCheck = !isFourCheck;
            if (!isFourCheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHoleState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }
            setHoleState(isFourCheck, (TextView) v);
            updateWellResult();
        } else if (id == R.id.bioway_result_hole_five_btn) {
            isFiveheck = !isFiveheck;
            if (!isFiveheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHoleState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }
            setHoleState(isFiveheck, (TextView) v);
            updateWellResult();
        } else if (id == R.id.bioway_result_hole_six_btn) {
            isSixCheck = !isSixCheck;
            if (!isSixCheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHoleState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }
            setHoleState(isSixCheck, (TextView) v);
            updateWellResult();
        } else if (id == R.id.bioway_result_hole_seven_btn) {
            isSevenCheck = !isSevenCheck;
            if (!isSevenCheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHoleState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }
            setHoleState(isSevenCheck, (TextView) v);
            updateWellResult();
        } else if (id == R.id.bioway_result_hole_eight_btn) {
            isEightCheck = !isEightCheck;
            if (!isEightCheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHoleState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }

            setHoleState(isEightCheck, (TextView) v);
            updateWellResult();
        } else if (id == R.id.bioway_ex_result_sec_analyse_tv) {
            //先进行文件文判断
            if(null != mSec_Experiment){
                secondAnalyse(mSec_Experiment);
            }

        } else if (id == R.id.bioway_result_all_hole_ck) {
            if (isAllCheck) {
                isAllCheck = false;
                setAllHoleUnCheck();
            } else {
                isAllCheck = true;
                setAllHoleCheck();
            }
            setHoleState(isAllCheck, mAllHoleCk);
            updateWellResult();
        }else if(id == R.id.bioway_ex_result_basic_param_title_tv){
            mBasicParamTitleTv.setTextColor(getResources().getColor(R.color.white));
            mBasicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mMeltParamTitleTv.setTextColor(getResources().getColor(R.color.black));
            mMeltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            mAmpParamterLayout.setVisibility(View.VISIBLE);
            mMeltParamterLayout.setVisibility(View.GONE);
            if(null != mAmpAlgorithmAdapter){
                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
            }
        }else if(id == R.id.bioway_ex_result_melt_param_title_tv){
            mMeltParamTitleTv.setTextColor(getResources().getColor(R.color.white));
            mMeltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mBasicParamTitleTv.setTextColor(getResources().getColor(R.color.black));
            mBasicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            mAmpParamterLayout.setVisibility(View.GONE);
            mMeltParamterLayout.setVisibility(View.VISIBLE);
            if(null != mMeltParamterAdapter){
                mMeltParamterAdapter.setMeltParamter(configTargets);
            }
        }else if(id == R.id.bioway_ex_result_more_tv){
            //更多
            expandMoreActions(v);
        }else if(id == R.id.bioway_ex_result_sec_analyse_amp_algorithm_tv){
            //扩增曲线算法枚举
            expandAmpAlg(v);
        }
    }



    private void setChannelState() {
        if (isROXCheck) {
            mROX_ChannelTv.setCheckState(R.drawable.item_chanel_check, isROXCheck);
        } else {
            mROX_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isROXCheck);
        }
        if (isCY5Check) {
            mCY5_ChannelTv.setCheckState(R.drawable.item_chanel_check, isCY5Check);
        } else {
            mCY5_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isCY5Check);
        }

        if (isHEXCheck) {
            mHEX_ChannelTv.setCheckState(R.drawable.item_chanel_check, isHEXCheck);
        } else {
            mHEX_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isHEXCheck);
        }

        if (isFAMCheck) {
            mFAM_ChannelTv.setCheckState(R.drawable.item_chanel_check, isFAMCheck);
        } else {
            mFAM_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck, isFAMCheck);
        }



        if (isCY5Check && isFAMCheck && isROXCheck && isHEXCheck ) {
            isAllChanelCheck = true;
            mAllChannelCk.setCheckState(R.drawable.item_chanel_check, isAllChanelCheck);
        } else {
            isAllChanelCheck = false;
            mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck, isAllChanelCheck);
        }
    }

    private void initFiles(){
        localList.clear();
        File allFileDir = new File(curTemplatePath);
        if (null != allFileDir) {
            if (allFileDir.isDirectory() && allFileDir.exists()) {
                Collections.addAll(localList, allFileDir.listFiles());
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
        if (!StringUtil.isEmpty(saveFileDirPath)) {
            //找文件
            File file = getBiowayExPCRFile(saveFileDirPath);
            if (file != null && file.exists()) {
                //读取.PCR文件
                return file.getAbsolutePath();
            }
        }
        return "";
    }
    /**
     * 获取实验参数
     */
    private void getBiowayExParameter(Experiment experiment){
        Log.e(TAG,"openExperiment-getSlope_thres_FAM-" + experiment.getSlope_thres_FAM());
        if(null != BoWeiService.getInstance() && configTargets != null){
            iscy5Threshold = isFamThreshold = isHexThreshold = isRoxThreshold = false;
            configTargets.clear();
            optimizations.clear();
            if(null != experiment){
                //峰型优化
                ArrayList<AnalyseParamter> analyseParameters = BoWeiService.getInstance()
                        .getExperimentParamter(experiment);
                Log.i(TAG,"analyseParamters--" +  analyseParameters.size());
                if(analyseParameters != null && analyseParameters.size()>0){
                    ArrayList<Target> targets = analyseParameters.get(0).getTargets();
                    AMPAlgorithm = analyseParameters.get(0).getAMPAlgorithm();
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

                                PeaktypeOptimization famOptimation = new PeaktypeOptimization();
                                famOptimation.setDyeName("FAM");
                                famOptimation.setChannelNo(1);
                                famOptimation.setOptimization_enable(experiment.isOptimization_enabl_FAM());
                                famOptimation.setEnhance_lower(experiment.getEnhance_lower_FAM());
                                famOptimation.setEnhance_upper(experiment.getEnhance_upper_FAM());
                                famOptimation.setEnhanceGain(experiment.getEnhance_gain_FAM());
                                famOptimation.setWeaken(experiment.getWeaken_FAM());
                                famOptimation.setWeakenGain(experiment.getWeaken_gain_FAM());
                                famOptimation.setSlopeThreshold(experiment.getSlope_thres_FAM());
                                optimizations.add(famOptimation);
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

                                PeaktypeOptimization roxOptimation = new PeaktypeOptimization();
                                roxOptimation.setDyeName("ROX");
                                roxOptimation.setChannelNo(3);
                                roxOptimation.setOptimization_enable(experiment.isOptimization_enabl_ROX());
                                roxOptimation.setEnhance_lower(experiment.getEnhance_lower_ROX());
                                roxOptimation.setEnhance_upper(experiment.getEnhance_upper_ROX());
                                roxOptimation.setEnhanceGain(experiment.getEnhance_gain_ROX());
                                roxOptimation.setWeaken(experiment.getWeaken_ROX());
                                roxOptimation.setWeakenGain(experiment.getWeaken_gain_ROX());
                                roxOptimation.setSlopeThreshold(experiment.getSlope_thres_ROX());
                                optimizations.add(roxOptimation);
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

                                PeaktypeOptimization hexOptimation = new PeaktypeOptimization();
                                hexOptimation.setDyeName("HEX");
                                hexOptimation.setChannelNo(2);
                                hexOptimation.setOptimization_enable(experiment.isOptimization_enabl_HEX());
                                hexOptimation.setEnhance_lower(experiment.getEnhance_lower_HEX());
                                hexOptimation.setEnhance_upper(experiment.getEnhance_upper_HEX());
                                hexOptimation.setEnhanceGain(experiment.getEnhance_gain_HEX());
                                hexOptimation.setWeaken(experiment.getWeaken_HEX());
                                hexOptimation.setWeakenGain(experiment.getWeaken_gain_HEX());
                                hexOptimation.setSlopeThreshold(experiment.getSlope_thres_HEX());
                                optimizations.add(hexOptimation);

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

                                PeaktypeOptimization cy5Optimation = new PeaktypeOptimization();
                                cy5Optimation.setDyeName("CY5");
                                cy5Optimation.setChannelNo(4);
                                cy5Optimation.setOptimization_enable(experiment.isOptimization_enabl_CY5());
                                cy5Optimation.setEnhance_lower(experiment.getEnhance_lower_CY5());
                                cy5Optimation.setEnhance_upper(experiment.getEnhance_upper_CY5());
                                cy5Optimation.setEnhanceGain(experiment.getEnhance_gain_CY5());
                                cy5Optimation.setWeaken(experiment.getWeaken_CY5());
                                cy5Optimation.setWeakenGain(experiment.getWeaken_gain_CY5());
                                cy5Optimation.setSlopeThreshold(experiment.getSlope_thres_CY5());
                                optimizations.add(cy5Optimation);
                            }
                        }
                    }
                }
            }
        }
        if(isMelt){
            mMeltParamTitleTv.setVisibility(View.VISIBLE);
        }else{
            mMeltParamTitleTv.setVisibility(View.INVISIBLE);
        }

        if(AMPAlgorithm == 0){
            mAMPAlgorithmTv.setText(getResources().getString(R.string.config_info_ampalgorithm_default));
        }else if(AMPAlgorithm == 1){
            mAMPAlgorithmTv.setText(getResources().getString(R.string.config_info_ampalgorithm_subtraction));
        }else if(AMPAlgorithm == 2){
            mAMPAlgorithmTv.setText(getResources().getString(R.string.config_info_ampalgorithm_relativeLinear));
        }
        if(null != configTargets){
            if(null != mAmpAlgorithmAdapter){
                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
            }
            if(null != mMeltParamterAdapter){
                mMeltParamterAdapter.setMeltParamter(configTargets);
            }
        }

        if(null != optimizations){
            if(null != peakTypeOptimizeAdapter){
                peakTypeOptimizeAdapter.setOptimizations(optimizations);
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
                hideProgressDialog();
                String failMsg = (String)messageEvent.getData();
                Toast.makeText(getApplicationContext(),failMsg,Toast.LENGTH_LONG).show();
                break;
            case MsgType.MSG_SHOW_ALL_CHANEL_RESULT:
            case MsgType.MSG_SHOW_FAM_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_HEX_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_ROX_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_CY5_CHANNEL_CURVE:
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
                hideProgressDialog();
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.result_ex_toast_second_analyse_done)
                        ,Toast.LENGTH_LONG).show();
                mRawCurveList.clear();
                mRawCurveList.addAll(BiowayManager.getInstance().getRawCurve());
                mAMPCurvList.clear();
                mAMPCurvList.addAll(BiowayManager.getInstance().getAMPCurve());
                mMeltCurvList.clear();
                mMeltCurvList.addAll(BiowayManager.getInstance().getMeltCurve());
                mPeakCurvList.clear();
                mPeakCurvList.addAll(BiowayManager.getInstance().getPeakCurve());
                mExlResults.clear();
                mExlResults.addAll(BiowayManager.getInstance().getExlResults());
                setOriginCurveList();
                //将试管信息同步到二次分析的结果
                if(null != mExlResults && mExlResults.size()>0){
                    for(BiowayExlResult exlResult :mExlResults){
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
                Experiment experiment = BiowayManager.getInstance().getExperiment();
                getBiowayExParameter(experiment);
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
                break;
        }
    }

    /**
     * 二次分析后，获得
     */

    private LimitLine initLimitLine(double threshold, String threshold_name, int number) {
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



    private LineChartMarkView markView;
    public void drawchart(List<BiowayCurveList> curve_list, LineChart lineChart, String describe) {
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
                if (!StringUtil.isEmpty(curve_list.get(i).getCurve_hole())) {
                    if (curve_list.get(i).getCurve_hole().equals("FAM")) {
                        my_color = PcrValue.colorline[0];//蓝色
                    } else if (curve_list.get(i).getCurve_hole().equals("HEX")) {
                        my_color = PcrValue.colorline[1];//绿色
                    } else if (curve_list.get(i).getCurve_hole().equals("ROX")) {
                        my_color = PcrValue.colorline[2];//黄色
                    } else if (curve_list.get(i).getCurve_hole().equals("CY5")) {
                        my_color = PcrValue.colorline[3];//红色
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
            xAxis.setLabelCount(curve_list.size(), false);

            lineChart.post(new Runnable() {
                @Override
                public void run() {
                    markView = new LineChartMarkView(BiowayExDetailActivity.this.getApplicationContext());
                    lineChart.setMarker(markView);
                }
            });
            lineChart.invalidate();
        }
    }


    public void drawChart(List<BiowayCurveList> curve_list, LineChart lineChart, String describe,
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
                if (!StringUtil.isEmpty(curve_list.get(i).getCurve_hole())) {
                    if (curve_list.get(i).getCurve_hole().equals("FAM")) {
                        my_color = PcrValue.colorline[0];//蓝色
                    } else if (curve_list.get(i).getCurve_hole().equals("HEX")) {
                        my_color = PcrValue.colorline[1];//绿色
                    } else if (curve_list.get(i).getCurve_hole().equals("ROX")) {
                        my_color = PcrValue.colorline[2];//黄色
                    } else if (curve_list.get(i).getCurve_hole().equals("CY5")) {
                        my_color = PcrValue.colorline[3];//红色
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

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线
            xAxis.setLabelCount(curve_list.size(), false);
            //
            Log.i("AAA","curve_list.size()" + curve_list.size());
            lineChart.post(new Runnable() {
                @Override
                public void run() {
                    markView = new LineChartMarkView(BiowayExDetailActivity.this.getApplicationContext());
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
        if (null != resultChart) {
            resultChart.clear();
            resultChart.notifyDataSetChanged();
            resultChart.invalidate();
        }

    }

    /////////////////////////////////////////////////////////原始数据///////////////////////////////////////////
    private void drawRawCurve() {
        clearChart();
        XAxis xAxis = resultChart.getXAxis();
        YAxis yAxis = resultChart.getAxisLeft();
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
        drawchart(mCheckRawList, resultChart, "RawCurve");
    }


    private void getAllRawCurve() {
        if (null != mRawCurveList) {
            mCheckRawList.clear();
            if (mRawCurveList.size() > 0) {
                for (BiowayCurveList curveList : mRawCurveList) {
                    if(!StringUtil.isEmpty(curveList.getCurve_hole())){
                        if(curveList.getCurve_hole().equals("FAM")) {
                            if (isFAMCheck) {
                                mCheckRawList.add(curveList);
                            }
                        }else if (curveList.getCurve_hole().equals("HEX")) {
                            if (isHEXCheck) {
                                mCheckRawList.add(curveList);
                            }
                        }else if (curveList.getCurve_hole().equals("ROX")) {
                            if (isROXCheck) {
                                mCheckRawList.add(curveList);
                            }
                        }else if (curveList.getCurve_hole().equals("CY5")) {
                            if (isCY5Check) {
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
        if (null != mAMPCurvList && mAMPCurvList.size()>0) {
            mCheckAMPList.clear();
            for(BiowayCurveList curveList : mAMPCurvList) {
                if(!StringUtil.isEmpty(curveList.getCurve_hole())){
                    if(curveList.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
                            mCheckAMPList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("HEX")) {
                        if(isHEXCheck){
                            mCheckAMPList.add(curveList);
                        }
                    }else if (curveList.getCurve_hole().equals("ROX")) {
                        if(isROXCheck){
                            mCheckAMPList.add(curveList);
                        }
                    }else if (curveList.getCurve_hole().equals("CY5")) {
                        if(isCY5Check){
                            mCheckAMPList.add(curveList);
                        }
                    }
                }
            }
        }

    }

    //根据选择的通道显示扩增曲线/

    private void drawAMPCurve() {
        clearChart();
        if(!isFamThreshold&& !isHexThreshold && !isRoxThreshold && !iscy5Threshold){
            drawchart(mCheckAMPList, resultChart, "AMPCurve");
        }else{
            XAxis xAxis = resultChart.getXAxis();
            YAxis yAxis = resultChart.getAxisLeft();
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
            drawChart(mCheckAMPList, resultChart, "AMPCurve",2);
        }
    }


    /*******************************获取所有的熔解曲线值******Melt***************************/
    private void getAllMelturve() {
        Log.i("BIOWAY", "getAllMelturve" + mMeltCurvList.size());
        if (null != mMeltCurvList) {
            mCheckMeltList.clear();
            if (mMeltCurvList.size() > 0) {
                for (BiowayCurveList curveList : mMeltCurvList) {
                    if(!StringUtil.isEmpty(curveList.getCurve_hole())){
                        if(curveList.getCurve_hole().equals("FAM")){
                            if(isFAMCheck) {
                                mCheckMeltList.add(curveList);
                            }
                        } else if(curveList.getCurve_hole().equals("HEX")) {
                            if(isHEXCheck) {
                                mCheckMeltList.add(curveList);
                            }

                        } else if(curveList.getCurve_hole().equals("ROX")) {
                            if(isROXCheck) {
                                mCheckMeltList.add(curveList);
                            }
                        } else if(curveList.getCurve_hole().equals("CY5")) {
                            if(isCY5Check) {
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
        XAxis xAxis = resultChart.getXAxis();
        YAxis yAxis = resultChart.getAxisLeft();
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
     /*   Log.i("BIOWAY","X"+ mCheckMeltList.get(0).getCurve_data().get(0).getX());
        Log.i("BIOWAY","y"+ mCheckMeltList.get(0).getCurve_data().get(0).getY());*/
        drawchart(mCheckMeltList, resultChart, "MeltCurve");
    }

    /**************************************************获取峰值数据*************************************************/

    private void getAllPeakCurve() {
        if (null != mPeakCurvList) {
            mCheckPeakList.clear();
            if (mPeakCurvList.size() > 0) {
                for (BiowayCurveList curveList : mPeakCurvList) {
                    if(!StringUtil.isEmpty(curveList.getCurve_hole())){
                        if (curveList.getCurve_hole().equals("FAM")) {
                            if (isFAMCheck) {
                                mCheckPeakList.add(curveList);
                            }
                        } else if (curveList.getCurve_hole().equals("HEX")) {
                            if (isHEXCheck) {
                                mCheckPeakList.add(curveList);
                            }
                        } else if (curveList.getCurve_hole().equals("ROX")) {
                            if (isROXCheck) {
                                mCheckPeakList.add(curveList);
                            }
                        } else if (curveList.getCurve_hole().equals("CY5")) {
                            if (isCY5Check) {
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
        XAxis xAxis = resultChart.getXAxis();
        YAxis yAxis = resultChart.getAxisLeft();
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
        drawchart(mCheckPeakList, resultChart, "PeakCurve");
    }


    /**
     * @param check
     * @param textView
     */
    private void setHoleState(boolean check, TextView textView) {
        textView.setClickable(true);
        if (!check) {
            textView.setBackgroundResource(R.drawable.shape_oval_gray_bg);
            textView.setTextColor(getResources().getColor(R.color.blue));
        } else {
            textView.setBackgroundResource(R.drawable.shape_oval_blue_bg);
            textView.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void checkAllHole(){
        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveheck && isSixCheck && isSevenCheck && isEightCheck) {
            if(!isAllCheck){
                isAllCheck = true;
                setHoleState(isAllCheck, mAllHoleCk);
            }
        }
    }


    private void setAllHoleCheck() {
        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveheck && isSixCheck && isSevenCheck && isEightCheck) {
            isAllCheck = true;
            setHoleState(isAllCheck, mAllHoleCk);
        } else {
            isOneCheck = true;
            isTwoCheck = true;
            isThreeCheck = true;
            isFourCheck = true;
            isFiveheck = true;
            isSixCheck = true;
            isSevenCheck = true;
            isEightCheck = true;
            setHoleState(isOneCheck, mHoleOneTv);
            setHoleState(isTwoCheck, mHoleTwoTv);
            setHoleState(isThreeCheck, mHoleThreeTv);
            setHoleState(isFourCheck, mHoleFourTv);
            setHoleState(isFiveheck, mHoleFiveTv);
            setHoleState(isSixCheck, mHoleSixTv);
            setHoleState(isSevenCheck, mHoleSevenTv);
            setHoleState(isEightCheck, mHoleEightTv);
            setHoleState(isAllCheck, mAllHoleCk);
        }
    }

    private void setAllHoleUnCheck() {
        isOneCheck = false;
        isTwoCheck = false;
        isThreeCheck = false;
        isFourCheck = false;
        isFiveheck = false;
        isSixCheck = false;
        isSevenCheck = false;
        isEightCheck = false;
        setHoleState(isOneCheck, mHoleOneTv);
        setHoleState(isTwoCheck, mHoleTwoTv);
        setHoleState(isThreeCheck, mHoleThreeTv);
        setHoleState(isFourCheck, mHoleFourTv);
        setHoleState(isFiveheck, mHoleFiveTv);
        setHoleState(isSixCheck, mHoleSixTv);
        setHoleState(isSevenCheck, mHoleSevenTv);
        setHoleState(isEightCheck, mHoleEightTv);
    }


    ///////////////////////////////////查询一次检测所有孔位记录///////////////////////////////////////////////////

    public static class ResultSearchAsyncTask extends AsyncTask<Void, Integer, List<BioWayWellResult>> {

        private WeakReference<BiowayExDetailActivity> weakfrag;
        private QueryCondition queryCondition = null;

        public ResultSearchAsyncTask(BiowayExDetailActivity activity, QueryCondition queryCondition) {
            weakfrag = new WeakReference<BiowayExDetailActivity>(activity);
            this.queryCondition = queryCondition;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            final BiowayExDetailActivity mActvity = weakfrag.get();
            super.onPreExecute();
            if (null == mActvity) {
                return;
            }
            if (null != mActvity.wellResults) {
                mActvity.wellResults.clear();
            }

        }

        //后台进程的执行
        @Override
        protected List<BioWayWellResult> doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            final BiowayExDetailActivity mActivity = weakfrag.get();
            mActivity.wellResults = LiteORMCtrlMgr.getInstance().searchBiowayWellResult(queryCondition);
            return mActivity.wellResults;

        }

        @Override
        protected void onPostExecute(List<BioWayWellResult> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            final BiowayExDetailActivity mActvity = weakfrag.get();
            if (null !=mActvity.wellResults) {
                //进行解析
                if (mActvity.wellResults.size() > 0) {
                    Log.i("BIOWAY", "ResultSearchAsyncTask result" + mActvity.wellResults.size());
                    mActvity.parseWellModelResults();
                } else {
                    Toast.makeText(mActvity.getApplicationContext()
                            ,mActvity.getResources().getString(R.string.toast_no_data),Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(mActvity.getApplicationContext()
                        ,mActvity.getResources().getString(R.string.toast_no_data),Toast.LENGTH_LONG).show();
            }
            mActvity.cancelTask();
        }
    }

    private void cancelTask() {
        if (null != searchAsyncTask) {
            searchAsyncTask.cancel(true);
            searchAsyncTask = null;
        }
        hideProgressDialog();
    }


    private void excuteTask(QueryCondition queryCondition) {
        if (null == searchAsyncTask) {
            searchAsyncTask = new ResultSearchAsyncTask(BiowayExDetailActivity.this, queryCondition);
            searchAsyncTask.execute();
        }
    }

    private LoadingDialog loadingDialog ;
    public void showLoadingDialog(String message){
        if(null == loadingDialog){
            loadingDialog = new LoadingDialog(BiowayExDetailActivity.this, message);
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }
    }

    /*
     * 隐藏提示加载
     */
    public void hideProgressDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }
/************************************************查询数据库解析孔位********************************************************/

    /**
     * 1.解析从数据库中查询的一次实验8个孔位的结果
     * 显示当前结果又几个孔位是有数据的（选中的状态）
     * 2.获取选中的所有的孔位结果
     */
    private void parseWellModelResults() {
        //最初的孔状态
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
        mHoleOneTv.setClickable(false);
        mHoleTwoTv.setClickable(false);
        mHoleThreeTv.setClickable(false);
        mHoleFourTv.setClickable(false);
        mHoleFiveTv.setClickable(false);
        mHoleSixTv.setClickable(false);
        mHoleSevenTv.setClickable(false);
        mHoleEightTv.setClickable(false);

        mHoleOneTv.setVisibility(View.INVISIBLE);
        mHoleTwoTv.setVisibility(View.INVISIBLE);
        mHoleThreeTv.setVisibility(View.INVISIBLE);
        mHoleFourTv.setVisibility(View.INVISIBLE);
        mHoleFiveTv.setVisibility(View.INVISIBLE);
        mHoleSixTv.setVisibility(View.INVISIBLE);
        mHoleSevenTv.setVisibility(View.INVISIBLE);
        mHoleEightTv.setVisibility(View.INVISIBLE);

        if (null != wellResults && wellResults.size() > 0){
            for (BioWayWellResult wellResultModel : wellResults) {
                if (wellResultModel.getWellName().equals("A1")) {
                    isOneCheck = true;
                    mHoleOneTv.setVisibility(View.VISIBLE);
                    setHoleState(isOneCheck, mHoleOneTv);
                } else if (wellResultModel.getWellName().equals("A2")) {
                    isTwoCheck = true;
                    mHoleTwoTv.setVisibility(View.VISIBLE);
                    setHoleState(isTwoCheck, mHoleTwoTv);
                } else if (wellResultModel.getWellName().equals("A3")) {
                    isThreeCheck = true;
                    mHoleThreeTv.setVisibility(View.VISIBLE);
                    setHoleState(isThreeCheck, mHoleThreeTv);
                } else if (wellResultModel.getWellName().equals("A4")) {
                    isFourCheck = true;
                    mHoleFourTv.setVisibility(View.VISIBLE);
                    setHoleState(isFourCheck, mHoleFourTv);
                } else if (wellResultModel.getWellName().equals("A5")) {
                    isFiveheck = true;
                    mHoleFiveTv.setVisibility(View.VISIBLE);
                    setHoleState(isFiveheck, mHoleFiveTv);
                } else if (wellResultModel.getWellName().equals("A6")) {
                    isSixCheck = true;
                    mHoleSixTv.setVisibility(View.VISIBLE);
                    setHoleState(isSixCheck, mHoleSixTv);
                } else if (wellResultModel.getWellName().equals("A7")) {
                    isSevenCheck = true;
                    mHoleSevenTv.setVisibility(View.VISIBLE);
                    setHoleState(isSevenCheck, mHoleSevenTv);
                } else if (wellResultModel.getWellName().equals("A8")) {
                    isEightCheck = true;
                    mHoleEightTv.setVisibility(View.VISIBLE);
                    setHoleState(isEightCheck, mHoleEightTv);
                }

                WellInfo wellInfo =
                        new WellInfo(wellResultModel.getWellName(),wellResultModel.getSampleName(),
                                wellResultModel.getReagentCode());
                if(!mWellInfos.contains(wellInfo)){
                    mWellInfos.add(wellInfo);
                }
            }
        }

        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveheck && isSixCheck && isSevenCheck && isEightCheck) {
            isAllCheck = true;
            setHoleState(isAllCheck, mAllHoleCk);
        }
    }




    private void setCheckedWellResults() {
        checkWellResult.clear();
        if (null != wellResults && wellResults.size() > 0) {
            for (BioWayWellResult wellResultModel : wellResults) {
                if (isOneCheck && wellResultModel.getWellName().equals("A1")) {
                    checkWellResult.add(wellResultModel);
                } else if (isTwoCheck && wellResultModel.getWellName().equals("A2")) {
                    checkWellResult.add(wellResultModel);

                } else if (isThreeCheck && wellResultModel.getWellName().equals("A3")) {

                    checkWellResult.add(wellResultModel);

                } else if (isFourCheck && wellResultModel.getWellName().equals("A4")) {
                    checkWellResult.add(wellResultModel);

                } else if (isFiveheck && wellResultModel.getWellName().equals("A5")) {
                    checkWellResult.add(wellResultModel);

                } else if (isSixCheck && wellResultModel.getWellName().equals("A6")) {
                    checkWellResult.add(wellResultModel);

                } else if (isSevenCheck && wellResultModel.getWellName().equals("A7")) {
                    checkWellResult.add(wellResultModel);

                } else if (isEightCheck && wellResultModel.getWellName().equals("A8")) {
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setChannelState();
                            drawchart(mCheckRawList, resultChart, "RawCurve");
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
        for (BioWayWellResult wellResultModel : checkWellResult) {
            parseWellResult(wellResultModel);
        }
        Log.e("BIOWAY","mMeltCurvList" + mMeltCurvList.size());
        if (null != mRawCurveList && mRawCurveList.size() > 0) {
            for (BiowayCurveList curveList : mRawCurveList) {
                if (!StringUtil.isEmpty(curveList.getCurve_hole())) {
                    if(curveList.getCurve_hole().equals("FAM")){
                        isFAMCheck = true;
                    } else if(curveList.getCurve_hole().equals("HEX")) {
                        isHEXCheck = true;
                    } else if(curveList.getCurve_hole().equals("ROX")) {
                        isROXCheck = true;
                    } else if(curveList.getCurve_hole().equals("CY5")) {
                        isCY5Check = true;
                    }
                }
            }
        }

        setOriginCurveList();
        mCheckRawList.clear();
        mCheckRawList.addAll(mRawCurveList);
        return true;
    }



    private void setOriginCurveList(){
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
            for (BiowayCurveList slanCurveList : mOriginRaw) {
                if(null != slanCurveList.getWellNum()){
                    if (isOneCheck && slanCurveList.getWellNum().equals("A1")) {
                        mRawCurveList.add(slanCurveList);
                    } else if (isTwoCheck && slanCurveList.getWellNum().equals("A2")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isThreeCheck && slanCurveList.getWellNum().equals("A3")) {
                        mRawCurveList.add(slanCurveList);
                    } else if (isFourCheck && slanCurveList.getWellNum().equals("A4")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isFiveheck && slanCurveList.getWellNum().equals("A5")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isSixCheck && slanCurveList.getWellNum().equals("A6")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isSevenCheck && slanCurveList.getWellNum().equals("A7")) {
                        mRawCurveList.add(slanCurveList);

                    } else if (isEightCheck && slanCurveList.getWellNum().equals("A8")) {
                        mRawCurveList.add(slanCurveList);
                    }
                }

            }
        }
        Log.i("SLAN", "updateWellResult解析结果 mRawCurvList" + mRawCurveList.size());
    }

    private void setCheckWellAMPCurve() {
        if (null != mOriginAMP && mOriginAMP.size() > 0) {
            mAMPCurvList.clear();
            for(BiowayCurveList slanCurveList :mOriginAMP){
                if(null != slanCurveList.getWellNum()){
                    if (isOneCheck && slanCurveList.getWellNum().equals("A1")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isTwoCheck && slanCurveList.getWellNum().equals("A2")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isThreeCheck && slanCurveList.getWellNum().equals("A3")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isFourCheck && slanCurveList.getWellNum().equals("A4")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isFiveheck && slanCurveList.getWellNum().equals("A5")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isSixCheck && slanCurveList.getWellNum().equals("A6")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isSevenCheck && slanCurveList.getWellNum().equals("A7")) {
                        mAMPCurvList.add(slanCurveList);
                    } else if (isEightCheck && slanCurveList.getWellNum().equals("A8")) {
                        mAMPCurvList.add(slanCurveList);
                    }
                }

            }
        }
        Log.i("SLAN", "updateWellResult解析结果 mAMPCurvList" + mAMPCurvList.size());
    }

    private void setCheckWellMeltCurve() {

        if (null != mOriginMelt && mOriginMelt.size() > 0) {
            mMeltCurvList.clear();
            for(BiowayCurveList slanCurveList :mOriginMelt){
                if(null != slanCurveList.getWellNum()){
                    if (isOneCheck && slanCurveList.getWellNum().equals("A1")) {
                        mMeltCurvList.add(slanCurveList);
                    } else if (isTwoCheck && slanCurveList.getWellNum().equals("A2")) {
                        mMeltCurvList.add(slanCurveList);
                    } else if (isThreeCheck && slanCurveList.getWellNum().equals("A3")) {
                        mMeltCurvList.add(slanCurveList);
                    } else if (isFourCheck && slanCurveList.getWellNum().equals("A4")) {
                        mMeltCurvList.add(slanCurveList);

                    } else if (isFiveheck && slanCurveList.getWellNum().equals("A5")) {
                        mMeltCurvList.add(slanCurveList);

                    } else if (isSixCheck && slanCurveList.getWellNum().equals("A6")) {
                        mMeltCurvList.add(slanCurveList);

                    } else if (isSevenCheck && slanCurveList.getWellNum().equals("A7")) {
                        mMeltCurvList.add(slanCurveList);

                    } else if (isEightCheck && slanCurveList.getWellNum().equals("A8")) {
                        mMeltCurvList.add(slanCurveList);
                    }
                }

            }

        }

    }

    private void setCheckWellPeakCurve() {
        if (null != mOriginPeak && mOriginPeak.size() > 0) {
            mPeakCurvList.clear();
            for(BiowayCurveList slanCurveList :mOriginPeak){
                if(null != slanCurveList.getWellNum()){
                    if (isOneCheck && slanCurveList.getWellNum().equals("A1")) {
                        mPeakCurvList.add(slanCurveList);
                    } else if (isTwoCheck && slanCurveList.getWellNum().equals("A2")) {
                        mPeakCurvList.add(slanCurveList);
                    } else if (isThreeCheck && slanCurveList.getWellNum().equals("A3")) {
                        mPeakCurvList.add(slanCurveList);
                    } else if (isFourCheck && slanCurveList.getWellNum().equals("A4")) {
                        mPeakCurvList.add(slanCurveList);

                    } else if (isFiveheck && slanCurveList.getWellNum().equals("A5")) {
                        mPeakCurvList.add(slanCurveList);

                    } else if (isSixCheck && slanCurveList.getWellNum().equals("A6")) {
                        mPeakCurvList.add(slanCurveList);

                    } else if (isSevenCheck && slanCurveList.getWellNum().equals("A7")) {
                        mPeakCurvList.add(slanCurveList);

                    } else if (isEightCheck && slanCurveList.getWellNum().equals("A8")) {
                        mPeakCurvList.add(slanCurveList);
                    }
                }

            }
        }
        Log.i("SLAN", "updateWellResult解析结果 mPeakCurvList" + mPeakCurvList.size());
    }

    private void setCheckWellExResult() {
        if (null != mOriginExResults && mOriginExResults.size() > 0) {
            mExlResults.clear();
            for(BiowayExlResult exlResult :mOriginExResults){
                if (isOneCheck && exlResult.getWellNum().equals("A1")) {
                    mExlResults.add(exlResult);
                } else if (isTwoCheck && exlResult.getWellNum().equals("A2")) {
                    mExlResults.add(exlResult);
                } else if (isThreeCheck && exlResult.getWellNum().equals("A3")) {
                    mExlResults.add(exlResult);
                } else if (isFourCheck && exlResult.getWellNum().equals("A4")) {
                    mExlResults.add(exlResult);

                } else if (isFiveheck && exlResult.getWellNum().equals("A5")) {
                    mExlResults.add(exlResult);

                } else if (isSixCheck && exlResult.getWellNum().equals("A6")) {
                    mExlResults.add(exlResult);

                } else if (isSevenCheck && exlResult.getWellNum().equals("A7")) {
                    mExlResults.add(exlResult);

                } else if (isEightCheck && exlResult.getWellNum().equals("A8")) {
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
                for (BiowayExlResult exlResult : mExlResults) {
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
                    }
                }
            }
        }
    }
    /**
     * @param biowayWellResult 每一个WellResult对应4个channel的结论
     */
    private void parseWellResult(BioWayWellResult biowayWellResult) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        String regentCode = biowayWellResult.getReagentCode();
        String idCard = biowayWellResult.getSampleName();
        isMelt = biowayWellResult.isMelt();

        String roxTargetResultStr = biowayWellResult.getRoxTargetResults();

        String hexTargetResultStr = biowayWellResult.getHexTargetResults();
        String famTargetResultStr = biowayWellResult.getFamTargetResults();
        String cy5TargetResultStr = biowayWellResult.getCy5TargetResults();


        String fam_threshold = biowayWellResult.getFamThreshold();
        String hex_threshold = biowayWellResult.getHexThreshold();
        String rox_threshold = biowayWellResult.getRoxThreshold();
        String cy5_threshold = biowayWellResult.getCy5Threshold();


        TargetResult mHexTargetResult = JSON.parseObject(hexTargetResultStr, TargetResult.class);
        TargetResult mFAMTargetResult = JSON.parseObject(famTargetResultStr, TargetResult.class);
        TargetResult mCY5TargetResult = JSON.parseObject(cy5TargetResultStr, TargetResult.class);
        TargetResult mROXTargetResult = JSON.parseObject(roxTargetResultStr, TargetResult.class);



        BiowayCurveList hex_amp_curve = BiowayManager.getInstance()
                .parseTargetResultToAMP(biowayWellResult.getWellName(),biowayWellResult.getSampleName(), mHexTargetResult);
        BiowayCurveList hex_peak_curve = BiowayManager.getInstance()
                .parseTargetResultToPeak(biowayWellResult.getWellName(),biowayWellResult.getSampleName(), mHexTargetResult);
        BiowayCurveList hex_raw_curve = BiowayManager.getInstance()
                .parseTargetResultToRaw(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mHexTargetResult);

        BiowayCurveList hex_melt_curve = BiowayManager.getInstance()
                .parseTargetResultToMelt(biowayWellResult.getWellName(),biowayWellResult.getSampleName(), mHexTargetResult);

        BiowayCurveList fam_amp_curve = BiowayManager.getInstance()
                .parseTargetResultToAMP(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mFAMTargetResult);
        BiowayCurveList fam_peak_curve = BiowayManager.getInstance()
                .parseTargetResultToPeak(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mFAMTargetResult);
        BiowayCurveList fam_raw_curve = BiowayManager.getInstance()
                .parseTargetResultToRaw(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mFAMTargetResult);
        BiowayCurveList fam_melt_curve = BiowayManager.getInstance()
                .parseTargetResultToMelt(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mFAMTargetResult);


        BiowayCurveList rox_amp_curve = BiowayManager.getInstance()
                .parseTargetResultToAMP(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mROXTargetResult);
        BiowayCurveList rox_peak_curve = BiowayManager.getInstance()
                .parseTargetResultToPeak(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mROXTargetResult);
        BiowayCurveList rox_raw_curve = BiowayManager.getInstance()
                .parseTargetResultToRaw(biowayWellResult.getWellName(),biowayWellResult.getSampleName(), mROXTargetResult);
        BiowayCurveList rox_melt_curve = BiowayManager.getInstance()
                .parseTargetResultToMelt(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),  mROXTargetResult);

        BiowayCurveList cy5_amp_curve = BiowayManager.getInstance()
                .parseTargetResultToAMP(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mCY5TargetResult);
        BiowayCurveList cy5_peak_curve = BiowayManager.getInstance()
                .parseTargetResultToPeak(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),mCY5TargetResult);
        BiowayCurveList cy5_raw_curve = BiowayManager.getInstance()
                .parseTargetResultToRaw(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mCY5TargetResult);
        BiowayCurveList cy5_melt_curve = BiowayManager.getInstance()
                .parseTargetResultToMelt(biowayWellResult.getWellName(), biowayWellResult.getSampleName(), mCY5TargetResult);


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

        String roxResults = biowayWellResult.getRoxResults();//rox对应的结论
        String hexResults = biowayWellResult.getHexResults();
        String famResults = biowayWellResult.getFamResults();
        String cy5Results = biowayWellResult.getCy5Results();


        /*List<Result> roxResultList = gson.fromJson(roxResults, new TypeToken<List<Result>>(){}.getType());

        List<Result> hexResultList = gson.fromJson(hexResults, new TypeToken<List<Result>>(){}.getType());
        List<Result> famResultList = gson.fromJson(famResults, new TypeToken<List<Result>>(){}.getType());
        List<Result> cy5ResultList = gson.fromJson(cy5Results, new TypeToken<List<Result>>(){}.getType());*/
        List<CtResult> famResultList = JSON.parseArray(famResults, CtResult.class);
        List<CtResult> hexResultList = JSON.parseArray(hexResults, CtResult.class);
        List<CtResult> roxResultList = JSON.parseArray(roxResults, CtResult.class);
        List<CtResult> cy5ResultList = JSON.parseArray(cy5Results, CtResult.class);


        List<PeakResult> famPeakResult = JSON.parseArray(biowayWellResult.getFamPeakResults(), PeakResult.class);
        List<PeakResult> hexPeakResult = JSON.parseArray(biowayWellResult.getHexPeakResults(), PeakResult.class);
        List<PeakResult> roxPeakResult = JSON.parseArray(biowayWellResult.getRoxPeakResults(), PeakResult.class);
        List<PeakResult> cy5PeakResult = JSON.parseArray(biowayWellResult.getCy5PeakResults(), PeakResult.class);


        BiowayExlResult famExlResult = new BiowayExlResult();
        famExlResult.setWellNum(biowayWellResult.getWellName());
        famExlResult.setChannelNo(1);
        famExlResult.setCurve_hole("FAM");
        famExlResult.setResults(famResultList);
        famExlResult.setPeakResults(famPeakResult);
        famExlResult.setRegentCode(regentCode);
        famExlResult.setIdCard(idCard);
        famExlResult.setMelt(isMelt);
        if (null != mFAMTargetResult) {
            famExlResult.setCt(mFAMTargetResult.getCt());
            famExlResult.setControlRn(mFAMTargetResult.getRn());
            famExlResult.setPeakValues(mFAMTargetResult.getPeakValues());
        }
        if(!StringUtil.isEmpty(fam_threshold)){
            isFamThreshold = true;
            famThreshold = (double) StringUtil.convertToFloat(fam_threshold,0.00f);
        }
        if(!StringUtil.isEmpty(biowayWellResult.getFamPeekThreshold())){
            double famPeekThreshold = (double) StringUtil.convertToFloat(biowayWellResult.getFamPeekThreshold(),0.00f);
            famExlResult.setPeekHighThreshold(famPeekThreshold);
        }

        famExlResult.setThreshold(famThreshold);
        mExlResults.add(famExlResult);


        BiowayExlResult hexExlResult = new BiowayExlResult();
        hexExlResult.setWellNum(biowayWellResult.getWellName());
        hexExlResult.setChannelNo(2);
        hexExlResult.setCurve_hole("HEX");
        hexExlResult.setResults(hexResultList);
        hexExlResult.setPeakResults(hexPeakResult);
        hexExlResult.setRegentCode(regentCode);
        hexExlResult.setIdCard(idCard);
        hexExlResult.setMelt(isMelt);
        if (null != mHexTargetResult) {
            hexExlResult.setCt(mHexTargetResult.getCt());
            hexExlResult.setControlRn(mHexTargetResult.getRn());
            hexExlResult.setPeakValues(mHexTargetResult.getPeakValues());
        }
        if(!StringUtil.isEmpty(hex_threshold)){
            isHexThreshold = true;
            hexThreshold = (double) StringUtil.convertToFloat(hex_threshold,0.00f);
        }
        hexExlResult.setThreshold(hexThreshold);
        if(!StringUtil.isEmpty(biowayWellResult.getHexPeekThreshold())){
            double hexPeakThreshold = (double)StringUtil.convertToFloat(biowayWellResult.getHexPeekThreshold(),0.00f);
            hexExlResult.setPeekHighThreshold(hexPeakThreshold);
        }

        mExlResults.add(hexExlResult);


        BiowayExlResult roxExlResult = new BiowayExlResult();
        roxExlResult.setWellNum(biowayWellResult.getWellName());
        roxExlResult.setChannelNo(3);
        roxExlResult.setCurve_hole("ROX");
        roxExlResult.setResults(roxResultList);
        roxExlResult.setPeakResults(roxPeakResult);
        roxExlResult.setRegentCode(regentCode);
        roxExlResult.setIdCard(idCard);
        roxExlResult.setMelt(isMelt);
        if (null != mROXTargetResult) {
            roxExlResult.setCt(mROXTargetResult.getCt());
            roxExlResult.setControlRn(mROXTargetResult.getRn());
            roxExlResult.setPeakValues(mROXTargetResult.getPeakValues());

        }
        if(!StringUtil.isEmpty(rox_threshold)){
            isRoxThreshold = true;
            roxThreshold = (double) StringUtil.convertToFloat(rox_threshold,0.00f);
        }
        roxExlResult.setThreshold(roxThreshold);
        if(!StringUtil.isEmpty(biowayWellResult.getRoxPeekThreshold())){
            double roxPeekThreshold = (double) StringUtil.convertToFloat(biowayWellResult.getRoxPeekThreshold(),0.00f);
            roxExlResult.setPeekHighThreshold(roxPeekThreshold);
        }
        mExlResults.add(roxExlResult);


        BiowayExlResult cy5ExlResult = new BiowayExlResult();
        cy5ExlResult.setWellNum(biowayWellResult.getWellName());
        cy5ExlResult.setChannelNo(4);
        cy5ExlResult.setCurve_hole("CY5");
        cy5ExlResult.setResults(cy5ResultList);
        cy5ExlResult.setPeakResults(cy5PeakResult);
        cy5ExlResult.setRegentCode(regentCode);
        cy5ExlResult.setIdCard(idCard);
        cy5ExlResult.setMelt(isMelt);
        if (null != mCY5TargetResult) {
            cy5ExlResult.setCt(mCY5TargetResult.getCt());
            cy5ExlResult.setControlRn(mCY5TargetResult.getRn());
            cy5ExlResult.setPeakValues(mCY5TargetResult.getPeakValues());
        }
        if(!StringUtil.isEmpty(cy5_threshold)){
            iscy5Threshold = true;
            cy5Threshold = (double) StringUtil.convertToFloat(cy5_threshold,0.00f);
        }
        cy5ExlResult.setThreshold(cy5Threshold);

        if(!StringUtil.isEmpty(biowayWellResult.getCy5PeekThreshold())){
            double cy5PeekThreshold = (double) StringUtil.convertToFloat(biowayWellResult.getCy5PeekThreshold(),0.00f);
            cy5ExlResult.setPeekHighThreshold(cy5PeekThreshold);
        }
        mExlResults.add(cy5ExlResult);


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
        Toast.makeText(getApplicationContext(),
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
            MessageEvent messageEvent = new MessageEvent();
            messageEvent.setMsgType(MsgType.MSG_SHOW_EX_SECOND_RESULT);
            EventBus.getDefault().post (messageEvent);

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
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.result_ex_detail_toast_update_result_success),Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext()
                    ,getResources().getString(R.string.result_ex_detail_toast_update_result_fail),Toast.LENGTH_LONG).show();
        }
    }

    private File getBiowayExPCRFile(String subFileDir) {
       /* String afterTime = DateFormatUtil.convertDateFormat(startTime);
        String resultDirPath;
        if (StringUtil.isEmpty(description)){
            resultDirPath = FileUtils.BIOWAY_RESULT_PATH + afterTime + "_" + testItem  + "/";
        }else{
            resultDirPath = FileUtils.BIOWAY_RESULT_PATH + afterTime + "_" + testItem  + "_" + description+ "/";
        }*/
        Log.i(TAG, "打开保存文件的文件夹getBiowayExPCRFilegetBiowayExPCRFile path" + subFileDir);
        File fileDir = new File(subFileDir);
        if (fileDir.exists() && fileDir.isDirectory()) {
            File[] files = fileDir.listFiles();//所有的json文件
            if(files != null && files.length>0){
                for(File file : files){
                    if(file.getName().endsWith(".boweipcrResult")){
                        Log.i(TAG, "getBiowayExPCRFile name" + file.getName());
                        return file;
                    }
                }
            }
        }
        return null;
    }

    private boolean isHasExJsonFile(String startTime, String testItem){
        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        String resultDirPath;
        if(StringUtil.isEmpty(description)){
            resultDirPath = FileUtils.BIOWAY_RESULT_PATH + afterTime + "_" + testItem + "_" + description + "/";
        }else{
            resultDirPath = FileUtils.BIOWAY_RESULT_PATH + afterTime + "_" + testItem;
        }
        Log.i(TAG, "getSLANExPCRFile path" + resultDirPath);
        File fileDir = new File(resultDirPath);
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
     * 二次分析
     * 将json的数据转化成Experiment
     *
     */
    private void secondAnalyse(Experiment sec_experiment) {
        if(null != configTargets && configTargets.size()>0){
            File allFileDir = new File(curTemplatePath);
            List<File> localList = new ArrayList<File>();
            if (null != allFileDir) {
                if (allFileDir.isDirectory() && allFileDir.exists()) {
                    Collections.addAll(localList, allFileDir.listFiles());
                }
                if (null != localList && localList.size() > 0) {
                    final File ctFile = FileUtils.getFile(localList, ".ct");
                    final File peakFile = FileUtils.getFile(localList, ".peak");
                    showLoadingDialog(getResources().getString(R.string.dialog_message_second_analyse));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BiowayManager.getInstance().secondBiowayExAnalyse(configTargets,optimizations,AMPAlgorithm,
                                    ctFile, peakFile,sec_experiment,startTime);
                        }
                    }).start();

                }else{
                    showLoadingDialog(getResources().getString(R.string.dialog_message_second_analyse));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BiowayManager.getInstance().secondBiowayExAnalyse(configTargets,optimizations,AMPAlgorithm,
                                    null, null,sec_experiment,startTime);
                        }
                    }).start();
                }
            } else {
                Toast.makeText(getApplicationContext(),"没有判断文件，无法进行二次分析",Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(getApplicationContext(),"沒有实验参数不可以进行二次分析",Toast.LENGTH_LONG).show();
        }


    }




    ///////////////////////////////////////********二次分析***********************************************/////
    /**
     * 更新实验
     */
    private void updateExperiment() {
        //更新数据库
        BiowayManager.getInstance().updateDetailExperiment(testItem, startTime);
        //更新文件
        if(!StringUtil.isEmpty(saveFileDirPath)){
            Experiment experiment = BiowayManager.getInstance().getExperiment();
            updateBiowayResultToPCR(experiment);
        }else{
            Toast.makeText(getApplicationContext(),"保存更新失败",Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBiowayResultToPCR(Experiment experiment){
        if(!StringUtil.isEmpty(saveFileDirPath)){
            File jsonDir = new File(saveFileDirPath);
            if(jsonDir.exists()){
                File[] files = jsonDir.listFiles();
                if(files.length>0){
                    for(File file : files){
                        file.delete();
                    }
                }
            }
            BiowayManager.getInstance().saveDetailExToPCR(experiment,testItem,saveFileDirPath);
        }
    }


    private void showUpdateDbDialog() {
        if (null != updateDbDialog) {
            updateDbDialog.dismiss();
            updateDbDialog = null;
        }
        if (null == updateDbDialog) {
            updateDbDialog = new CustomDialog(BiowayExDetailActivity.this);
            LayoutInflater mInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_interrupt_detect_layout, null);
            TextView hintTv = (TextView) view.findViewById(R.id.dialog_interrupt_hint_tv);
            hintTv.setText(R.string.dialog_message_update_ex);
            CustomDialog.Builder builder = new CustomDialog.Builder(BiowayExDetailActivity.this);
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
            Bitmap bitmap = ScreenShotUtil.saveScreenShot((BiowayExDetailActivity) this, dir);
            screenshot_Bmp = new ImageView(this);
            screenshot_Bmp.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            screenshot_Bmp.setImageBitmap(bitmap);
            main_root_layout.addView(screenshot_Bmp);
            ScaleAnimation scaleAnimation = (ScaleAnimation) AnimationUtils
                    .loadAnimation(this.getApplicationContext(), R.anim.screen_cut);
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
                    if (main_root_layout != null)
                        main_root_layout.removeView(screenshot_Bmp);
                    Toast.makeText(getApplicationContext(),"screenshot done",Toast.LENGTH_LONG).show();
                }
            });
            screenshot_Bmp.startAnimation(scaleAnimation);
        }
    }

    /**
     * 将Ex保存为Json格式
     */
    private void saveExJsonFile(){
        /*if (!StringUtil.isEmpty(startTime) && !StringUtil.isEmpty(testItem)) {
            //找文件
            File file = getSLANExPCRFile(startTime, testItem);
            if (file != null && file.exists()) {
                String file_path = file.getAbsolutePath();
                if(isHasExJsonFile(startTime,testItem)){
                    Toast.makeText(getApplicationContext(),"已经保存了项目的Json文件",Toast.LENGTH_LONG).show();
                    return;
                }
                if(!StringUtil.isEmpty(file_path)){
                    Experiment open_ex = BoWeiService.getInstance().openExperiment(file_path);
                    if(null != open_ex){
                        SLANManager.getInstance().saveResultToJSON(open_ex,startTime,testItem);
                    }else {
                        Toast.makeText(getApplicationContext(),"无法获取项目",Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"项目结果PCR文件不存在",Toast.LENGTH_LONG).show();
                }
            }else {
                Toast.makeText(getApplicationContext(),"项目结果PCR文件不存在",Toast.LENGTH_LONG).show();
            }
        }*/
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
                        int endbase = (int)StringUtil.convertToFloat(s,1.0f);
                        if(endbase < 1){
                            Toast.makeText(getApplicationContext(),"不能小于1",Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                            configTarget.setEndBaseline(endbase);
                            mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                        }
                    }else if(dialogType == 3){
                        //基线起点
                        int beginbase = (int)StringUtil.convertToFloat(s,1.0f);
                        if(beginbase < 1){
                            Toast.makeText(getApplicationContext(),"不能小于1",Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                            configTarget.setBeginBaseline(beginbase);
                            mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                        }
                    }else if(dialogType == 4){
                        //手动阈值
                        double threshold = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                        configTarget.setThreshold(threshold);
                        mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                    }else if(dialogType == 5){
                        //最低温度
                        double minTemp = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                        configTarget.setMinTemperature(minTemp);
                        mMeltParamterAdapter.setMeltParamter(configTargets);
                    }else if(dialogType == 6){
                        //最高温度
                        double maxTemp = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                        configTarget.setMaxTemperature(maxTemp);
                        mMeltParamterAdapter.setMeltParamter(configTargets);
                    }else if(dialogType == 7){
                        //峰高阈值
                        double peekHighThreshold = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                        configTarget.setPeekHighThreshold(peekHighThreshold);
                        mMeltParamterAdapter.setMeltParamter(configTargets);
                    }else if(dialogType == 8){
                        //增强峰高阈值下限
                        double enhanceLower = (double) StringUtil.convertToFloat(s,0.0f);
                        PeaktypeOptimization optimization = optimizations.get(optimationPos);
                        optimization.setEnhance_lower(enhanceLower);
                        peakTypeOptimizeAdapter.setOptimizations(optimizations);
                    }else if(dialogType == 9){
                        //增强峰高阈值上限
                        double enhanceUpper = (double) StringUtil.convertToFloat(s,0.0f);
                        PeaktypeOptimization optimization = optimizations.get(optimationPos);
                        optimization.setEnhance_upper(enhanceUpper);
                        peakTypeOptimizeAdapter.setOptimizations(optimizations);
                    }else if(dialogType == 10){
                        //增强增益
                        double enhanceGain = (double) StringUtil.convertToFloat(s,0.0f);
                        PeaktypeOptimization optimization = optimizations.get(optimationPos);
                        optimization.setEnhanceGain(enhanceGain);
                        peakTypeOptimizeAdapter.setOptimizations(optimizations);
                    }else if(dialogType == 11){
                        //削弱峰高阈值
                        double weaken= (double) StringUtil.convertToFloat(s,0.0f);
                        PeaktypeOptimization optimization = optimizations.get(optimationPos);
                        optimization.setWeaken(weaken);
                        peakTypeOptimizeAdapter.setOptimizations(optimizations);
                    }else if(dialogType == 12){
                        //削弱增益
                        double weakenGain = (double) StringUtil.convertToFloat(s,0.0f);
                        PeaktypeOptimization optimization = optimizations.get(optimationPos);
                        optimization.setWeakenGain(weakenGain);
                        peakTypeOptimizeAdapter.setOptimizations(optimizations);
                    }else if(dialogType == 13){
                        //斜率阈值
                        double slopeThreshold = (double) StringUtil.convertToFloat(s,0.0f);
                        PeaktypeOptimization optimization = optimizations.get(optimationPos);
                        optimization.setSlopeThreshold(slopeThreshold);
                        peakTypeOptimizeAdapter.setOptimizations(optimizations);
                    }
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.show();
        }
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
        View view= LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        ampAlgSpinnerAdapter=new CustomSpinnerAdapter(getApplicationContext(),mAmpAlgorithmList);
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
                mAMPAlgorithmTv.setText(mAmpAlgorithmList.get(position));
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
        View view= LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter mAmpAysTypeAdapter=new CustomSpinnerAdapter(getApplicationContext(),mAmpAysTypes);
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

                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
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
        View view= LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter optimizationModeAdapter=new CustomSpinnerAdapter(getApplicationContext(),mOptimizationModes);
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

                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
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
        View view = LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter digitalFilterAdapter=new CustomSpinnerAdapter(getApplicationContext(),mDighitalFilters);
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
                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
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
        View view= LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter peekTypeAdapter=new CustomSpinnerAdapter(getApplicationContext(),mPeekTypes);
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
        View view= LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter meltAlgorithmAdapter=new CustomSpinnerAdapter(getApplicationContext(),mMeltPeakAlgorithms);
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

    ////////////////////////////////////峰型优化////////////////////////////////////////////////////
    /**
     * 是否启用
     */
    private void collapseEnableOptimation(){
        if (mEnablesPopWindow != null && mEnablesPopWindow.isShowing()) {
            mEnablesPopWindow.dismiss();
        }
    }

    private void expandEnableOptimation(View view){
        if( mEnalbeOptimations== null && mEnalbeOptimations.isEmpty()){
            return;
        }
        mEnablesPopWindow.showAsDropDown(view);
    }

    private void initEnableOptimation(){
        mEnalbeOptimations = new ArrayList<Boolean>();
        mEnalbeOptimations.add(true);
        mEnalbeOptimations.add(false);
        View view= LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter enableAdapter=new CustomSpinnerAdapter(getApplicationContext(),mEnalbeOptimations);
        ListView mEnableLv= (ListView) view.findViewById(R.id.spinner_listview);
        mEnableLv.setAdapter(enableAdapter);
        mEnablesPopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mEnablesPopWindow.setOutsideTouchable(true);
        mEnablesPopWindow.setFocusable(true);
        mEnablesPopWindow.setContentView(view);
        mEnablesPopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mEnablesPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                collapsePeekType();
            }
        });

        enableAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                PeaktypeOptimization optimization = optimizations.get(optimationPos);
                optimization.setOptimization_enable(mEnalbeOptimations.get(position));
                peakTypeOptimizeAdapter.setOptimizations(optimizations);
                collapseEnableOptimation();
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
        View view= LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter mAmpAysTypeAdapter=new CustomSpinnerAdapter(getApplicationContext(),mActions);
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
            XAxis xAxis = resultChart.getXAxis();
            YAxis yAxis = resultChart.getAxisLeft();
            if(yAxis != null && xAxis != null){
                if(Float.isInfinite(yAxis.getAxisMaximum())){
                    Toast.makeText(getApplicationContext(),"没有数据不能进行坐标缩放",Toast.LENGTH_LONG).show();
                    return;
                }else{
                    showCoodidateDialog(currentView,(int)Math.round(xAxis.getAxisMinimum()),(int)Math.round(xAxis.getAxisMaximum())
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
            YAxis yAxis = resultChart.getAxisLeft();
            XAxis xAxis = resultChart.getXAxis();
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
    private void showCoodidateDialog(int dialog_type,int min_x,int max_x,String min_y,String max_y){
        if(null != coordinateDialog){
            coordinateDialog.dismiss();
            coordinateDialog = null;
        }
        if(null == coordinateDialog){
            coordinateDialog = new CustomLargeDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_custom_coodidate_scale_layout, null);
            CustomAddSubEdit minX_Et = (CustomAddSubEdit) view.findViewById(R.id.dialog_coodidate_min_x_et);
            CustomAddSubEdit maxX_Et = (CustomAddSubEdit) view.findViewById(R.id.dialog_coodidate_max_x_et);
            CustomAddSubEdit minY_Et = (CustomAddSubEdit)  view.findViewById(R.id.dialog_coodidate_y_min_et);
            CustomAddSubEdit maxY_Et = (CustomAddSubEdit)  view.findViewById(R.id.dialog_coodidate_Y_max_et);
            minX_Et.setEditTextValue(String.valueOf(min_x));
            maxX_Et.setEditTextValue(String.valueOf(max_x));
            minY_Et.setEditTextValue(min_y);
            maxY_Et.setEditTextValue(max_y);
            CustomLargeDialog.Builder builder = new CustomLargeDialog.Builder(this);
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
                        XAxis xAxis = resultChart.getXAxis();
                        YAxis yAxis = resultChart.getAxisLeft();
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
                                drawchart(mCheckRawList, resultChart, "RawCurve");
                                break;
                            case 2:
                                isScaleXAmp = true;
                                min_Amp_X = X_min;
                                max_Amp_X = X_max;
                                min_Amp_Y = Y_min;
                                max_Amp_Y = Y_max;
                                clearChart();
                                drawchart(mCheckAMPList, resultChart, "AmpCurve");
                                break;
                            case 3:
                                isScaleXMelt = true;
                                min_Melt_X = X_min;
                                max_Melt_X = X_max;
                                min_Melt_Y = Y_min;
                                max_Melt_Y = Y_max;
                                clearChart();
                                drawchart(mCheckMeltList, resultChart, "MeltCurve");
                                break;
                            case 4:
                                isScaleXPeak = true;
                                min_Peak_X = X_min;
                                max_Peak_X = X_max;
                                min_Peak_Y = Y_min;
                                max_Peak_Y = Y_max;
                                clearChart();
                                drawchart(mCheckPeakList, resultChart, "PeakCurve");
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
}
