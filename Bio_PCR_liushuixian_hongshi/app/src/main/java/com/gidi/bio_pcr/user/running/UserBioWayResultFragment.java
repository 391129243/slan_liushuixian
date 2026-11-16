package com.gidi.bio_pcr.user.running;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.adapter.PeakTypeOptimizeAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.PeaktypeOptimization;
import com.gidi.bio_pcr.bean.ReportResult;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.gidi.bio_pcr.bioway.adapter.RunBiowayResultAdapter;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.user.UserReportAdapter;
import com.gidi.bio_pcr.util.CommonUtil;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomAddSubEdit;
import com.gidi.bio_pcr.view.CustomCheckText;
import com.gidi.bio_pcr.view.CustomLargeDialog;
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
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.ExperimentData.Peak;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.winters.boweilibrary.BoWei.bean.BiowayExlResult;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserBioWayResultFragment extends BaseFragment implements View.OnClickListener{

    private CustomCheckText mFAM_ChannelTv,mHEX_ChannelTv,mROX_ChannelTv,mCY5_ChannelTv;
    private boolean isFAMCheck = false;
    private boolean isHEXCheck = false;
    private boolean isROXCheck = false;
    private boolean isCY5Check = false;

    private TextView mHoleOneTv,mHoleTwoTv,mHoleThreeTv,mHoleFourTv,mHoleFiveTv,mHoleSixTv,mHoleSevenTv,mHoleEightTv;
    private boolean isOneCheck,isTwoCheck,isThreeCheck,isFourCheck,
            isFiveCheck,isSixCheck,isSevenCheck,isEightCheck = false;
    private boolean isOneEnable, isTwoEnable, isThreeEnable, isFourEnable, isFiveEnable, isSixEnable, isSevenEnable, isEightEnable;
    private int currentView = 1;

    private LineChart resultChart;
    private RadioGroup mCurveChartGg;
    //原始数据
    private List<BiowayCurveList> mRawCurveList;//原始数据
    private List<BiowayCurveList> mCheckRawList;
    private List<BiowayCurveList> mAMPCurveList;
    private List<BiowayCurveList> mCheckAMPList;

    private List<BiowayCurveList> mMeltCurveList;
    private List<BiowayCurveList> mCheckMeltList;

    private List<BiowayCurveList> mPeakCurveList;
    private List<BiowayCurveList> mCheckPeakList ;

    private List<BiowayCurveList> showCurveList;
    private List<BiowayExlResult> mExlResults;
    private List<BiowayExlResult> mCheckExlResults;
    private LinearLayout mChannelLayout;
    private LinearLayout mResultChartLayout,mHoleLayout;
    private HorizontalScrollView mResultExlLayout;
    private boolean isChartVisible = true;
    private boolean isExcelVisible = false;
    private boolean isSecAysVisible = false;

    private RadioButton mRawCurve_Rb;
    private ListView mResultListView;
    private RunBiowayResultAdapter resultAdapter;

    private TextView mSecAnalyseTv;
    private double famThreshold,hexThreshold,roxThreshold,cy5Threshold;
    private boolean isFamThreshold,isHexThreshold,isRoxThreshold,isCy5Threshold = false;

    private boolean isMelt = false;
    private boolean isAllCheck = false;
    private TextView mAllHoleCk;
    private CustomCheckText mAllChannelCk;
    private boolean isAllChanelCheck = false;
    private List<ConfigTarget> configTargets;
    private RelativeLayout mSecAnalyseLayout;
    private TextView mBasicParamTitleTv,mMeltParamTitleTv,mAMPAlgorithmTv;//3.扩增曲线算法枚举;
    private LinearLayout mAmpParamterLayout,mMeltParamterLayout;//扩增曲线参数，熔解曲线参数
    private ListView mAmpAlgorithmLv,mMeltParamterLv;//扩增曲线参数列表，熔解曲线参数列表
    private MeltParamterAdapter mMeltParamterAdapter;
    private AmpAlgorithmAdapter mAmpAlgorithmAdapter;
    private NumKeyboardDialog numKeyboardDialog;
    private int ampAlogrithm_pos = -1;
    private int meltParamter_pos = -1;
    private PopupWindow mAysTypePopWindow,mOptimizationModeWindow,mDigitalFilterWindow;//定性定量,基线优化
    private PopupWindow mPeekTypeWindow,meltAlgPopWindow;
    private List<String> mAmpAysTypes,mOptimizationModes,mDighitalFilters;
    private List<String> mPeekTypes,mMeltPeakAlgorithms;
    private TextView mMoreActionTv;
    private List<String> mMoreActions;
    private PopupWindow mMoreActionWindows;
    private boolean isScaleXRaw,isScaleXAmp,isScaleXMelt,isScaleXPeak = false;
    private float min_Raw_X,max_Raw_X,min_Amp_X,max_Amp_X,min_Melt_X,max_Melt_X,min_Peak_X,max_Peak_X;
    private float min_Raw_Y,max_Raw_Y,min_Amp_Y,max_Amp_Y,min_Melt_Y,max_Melt_Y,min_Peak_Y,max_Peak_Y;
    private DecimalFormat nf1 = new DecimalFormat("#0.000");
    private DecimalFormat nf = new DecimalFormat( "0.00");
    private DecimalFormat nf2 = new DecimalFormat( "0.0");
    //扩增曲线算法枚举
    private List<String> mAMPAlgorithmList;
    private PopupWindow mAMPAlgWindow;
    private CustomSpinnerAdapter mAMPAlgSpinerAdapter;
    private ListView mAMPAlgorithmLv;
    private int AMPAlgorithm = 0;//默认
    //峰高优化
    private ListView mPeakHighOptimizeLv;
    private PeakTypeOptimizeAdapter peakTypeOptimizeAdapter;
    private List<PeaktypeOptimization> optimizations;
    private List<Boolean> mEnalbeOptimations;
    private PopupWindow mEnablesPopWindow;
    private int optimationPos = 0;//优化的位置

    //显示报告
    //检测报告
    private TextView mReportTv;

    private RelativeLayout mReportLayout, mResultsLayout;//报告。曲线和表格可互相切换
    private TextView mPrintCurTv, mPrintAllTv;//打印当前，打印全部
    private RadioButton mReportOneRb, mReportTwoRb, mReportThreeRb, mReportFourRb, mReportFiveRb, mReportSixRb, mReportSevenRb, mReportEightRb;
    private ImageView mBackResultImg;//
    private RadioGroup mReportHoleRg;

    private UserReportAdapter userReportAdapter, userReportAdapter_2,userReportAdapter_3,userReportAdapter_4,
            userReportAdapter_5,userReportAdapter_6,userReportAdapter_7,userReportAdapter_8;
    private int reportView = -1;


    private LinearLayout mReportOneLayout,mReportTwoLayout,mReportThreeLayout,mReportFourLayout;
    private LinearLayout mReportFiveLayout,mReportSixLayout,mReportSevenLayout,mReportEightLayout;
    private ListView mReportOneLv,mReportTwoLv,mReportThreeLv,mReportFourLv,mReportFiveLv,mReportSixLv,mReportSevenLv,mReportEightLv;
    private ImageView mBarCodeOneImg,mBarCodeTwoImg,mBarCodeThreeImg,mBarCodeFourImg,mBarCodeFiveImg,mBarCodeSixImg,mBarCodeSevenImg,mBarCodeEightImg;
    //样本编号
    private TextView mIDcardOneTv, mIDcardTwoTv,mIDcardThreeTv,mIDcardFourTv,mIDcardFiveTv,mIDcardSixTv,mIDcardSevenTv,mIDcardEightTv;
    private TextView mItemOneTv, mItemTwoTv,mItemThreeTv,mItemFourTv,mItemFiveTv,mItemSixTv,mItemSevenTv,mItemEightTv;
    //打印报告的检测项目
    private TextView mStartTimeOneTv,mStartTimeTwoTv,mStartTimeThreeTv,mStartTimeFourTv,mStartTimeFiveTv,mStartTimeSixTv,
            mStartTimeSevenTv,mStartTimeEightTv;
    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_user_running_result;
    }

    @Override
    protected void initView(View rootView) {
        resultChart = (LineChart)rootView.findViewById(R.id.user_running_result_bioway_curve_chart);
        if(null != resultChart){
            resultChart.setNoDataText("");
        }
        mChannelLayout = (LinearLayout)rootView.findViewById(R.id.user_running_bioway_channel_1_layout);
        mAllChannelCk = (CustomCheckText)rootView.findViewById(R.id.user_result_bioway_all_channel_check_tv) ;
        mFAM_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.user_running_bioway_result_fam_tv);
        mHEX_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.user_running_bioway_result_hex_tv);
        mROX_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.user_running_bioway_result_rox_tv);
        mCY5_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.user_running_bioway_result_cy5_tv);

        mMoreActionTv =(TextView)rootView.findViewById(R.id.user_running_bioway_ex_result_more_tv) ;
        mHoleOneTv = (TextView) rootView.findViewById(R.id.user_result_bioway_hole_one_btn);
        mHoleTwoTv = (TextView)rootView.findViewById(R.id.user_result_bioway_hole_two_btn);
        mHoleThreeTv = (TextView)rootView.findViewById(R.id.user_result_bioway_hole_three_btn);
        mHoleFourTv = (TextView)rootView.findViewById(R.id.user_result_bioway_hole_four_btn);
        mHoleFiveTv = (TextView)rootView.findViewById(R.id.user_result_bioway_hole_five_btn);
        mHoleSixTv = (TextView)rootView.findViewById(R.id.user_result_bioway_hole_six_btn);
        mHoleSevenTv = (TextView)rootView.findViewById(R.id.user_result_bioway_hole_seven_btn);
        mHoleEightTv = (TextView)rootView.findViewById(R.id.user_result_bioway_hole_eight_btn);
        mCurveChartGg = (RadioGroup)rootView.findViewById(R.id.user_result_bioway_curve_layout);
        mRawCurve_Rb = (RadioButton)rootView.findViewById(R.id.user_result_bioway_rawcurve_rb);
        mRawCurve_Rb.setChecked(true);
        mHoleLayout = (LinearLayout)rootView.findViewById(R.id.user_result_bioway_hole_layout) ;
        mResultExlLayout = (HorizontalScrollView) rootView.findViewById(R.id.user_result_bioway_excel_list_scrollview);
        mResultChartLayout = (LinearLayout)rootView.findViewById(R.id.user_result_bioway_curve_chart_layout);

        mResultListView = (ListView)rootView.findViewById(R.id.user_result_bioway_list_view) ;
        resultAdapter = new RunBiowayResultAdapter(getActivity().getApplicationContext());
        mSecAnalyseTv = (TextView)rootView.findViewById(R.id.user_running_bioway_sec_analyse_tv);
        mAllHoleCk = (TextView)rootView.findViewById(R.id.user_result_bioway_running_all_hole_ck);

        //参数设置
        mSecAnalyseLayout= (RelativeLayout)rootView.findViewById(R.id.user_bioway_result_sec_analyse_layout);
        mBasicParamTitleTv = (TextView)rootView.findViewById(R.id.user_bioway_result_basic_param_title_tv);
        mMeltParamTitleTv = (TextView)rootView.findViewById(R.id.user_bioway_result_melt_param_title_tv);
        mAmpAlgorithmLv = (ListView)rootView.findViewById(R.id.user_bioway_result_sec_analyse_amp_algorithm_lv);
        mAmpParamterLayout = (LinearLayout)rootView.findViewById(R.id.user_bioway_result_sec_analyse_amp_param_layout);
        mMeltParamterLayout = (LinearLayout)rootView.findViewById(R.id.user_bioway_result_sec_analyse_melt_param_layout);
        mMeltParamterLv = (ListView)rootView.findViewById(R.id.user_bioway_result_sec_analyse_melt_paramter_lv);
        mAmpAlgorithmAdapter = new AmpAlgorithmAdapter(getActivity().getApplicationContext());
        mMeltParamterAdapter = new MeltParamterAdapter(getActivity().getApplicationContext());
        mAmpAlgorithmLv.setAdapter(mAmpAlgorithmAdapter);
        mMeltParamterLv.setAdapter(mMeltParamterAdapter);
        //基本参数-扩增曲线算法枚举
        mAMPAlgorithmTv = (TextView) rootView.findViewById(R.id.user_bioway_result_sec_analyse_amp_algorithm_tv);

        //峰型优化
        mPeakHighOptimizeLv = (ListView)rootView. findViewById(R.id.bioway_user_result_sec_analyse_optimization_lv);
        peakTypeOptimizeAdapter = new PeakTypeOptimizeAdapter(getActivity().getApplicationContext());
        mPeakHighOptimizeLv.setAdapter(peakTypeOptimizeAdapter);
        initEnableOptimation();

        mResultsLayout = (RelativeLayout) rootView.findViewById(R.id.result_user_chart_and_excel_layout);
        //打印报告
        mReportTv = (TextView) rootView.findViewById(R.id.running_user_report_tv);
        mReportLayout = (RelativeLayout) rootView.findViewById(R.id.running_user_result_show_report_rl);

        mPrintCurTv = (TextView) rootView.findViewById(R.id.result_user_print_current_tv);
        mPrintAllTv = (TextView) rootView.findViewById(R.id.result_user_print_all_tv);
        mBackResultImg = (ImageView) rootView.findViewById(R.id.result_user_back_result_img);
        mReportHoleRg = (RadioGroup) rootView.findViewById(R.id.result_user_report_hole_rg);


        mReportOneLayout = (LinearLayout) rootView.findViewById(R.id.user_result_report_one_layout);
        mReportThreeLayout = (LinearLayout) rootView.findViewById(R.id.user_result_report_three_layout);
        mReportFourLayout = (LinearLayout) rootView.findViewById(R.id.user_result_report_four_layout);
        mReportFiveLayout = (LinearLayout) rootView.findViewById(R.id.user_result_report_five_layout);
        mReportSixLayout = (LinearLayout) rootView.findViewById(R.id.user_result_report_six_layout);
        mReportSevenLayout = (LinearLayout) rootView.findViewById(R.id.user_result_report_seven_layout);
        mReportEightLayout = (LinearLayout) rootView.findViewById(R.id.user_result_report_eight_layout);

        mReportOneLv = (ListView) mReportOneLayout.findViewById(R.id.user_result_report_lv);
        mIDcardOneTv = (TextView) mReportOneLayout.findViewById(R.id.user_result_idcard_tv);
        mStartTimeOneTv = (TextView) mReportOneLayout.findViewById(R.id.user_result_testtime_tv);
        mItemOneTv = (TextView) mReportOneLayout.findViewById(R.id.user_result_testitem_tv);
        mBarCodeOneImg = (ImageView) mReportOneLayout.findViewById(R.id.result_user_pdf_report_idcard_img);
        userReportAdapter = new UserReportAdapter(getActivity().getApplicationContext());
        mReportOneLv.setAdapter(userReportAdapter);

        //孔二报告2
        mReportTwoLayout = (LinearLayout) rootView.findViewById(R.id.user_result_report_two_layout);
        mReportTwoLv = (ListView) mReportTwoLayout.findViewById(R.id.user_result_report_lv);
        mIDcardTwoTv = (TextView) mReportTwoLayout.findViewById(R.id.user_result_idcard_tv);
        mStartTimeTwoTv = (TextView) mReportTwoLayout.findViewById(R.id.user_result_testtime_tv);
        mItemTwoTv = (TextView) mReportTwoLayout.findViewById(R.id.user_result_testitem_tv);
        mBarCodeTwoImg = (ImageView) mReportTwoLayout.findViewById(R.id.result_user_pdf_report_idcard_img);
        userReportAdapter_2 = new UserReportAdapter(getActivity().getApplicationContext());
        mReportTwoLv.setAdapter(userReportAdapter_2);

        mReportThreeLv = (ListView) mReportThreeLayout.findViewById(R.id.user_result_report_lv);
        mIDcardThreeTv = (TextView) mReportThreeLayout.findViewById(R.id.user_result_idcard_tv);
        mStartTimeThreeTv = (TextView) mReportThreeLayout.findViewById(R.id.user_result_testtime_tv);
        mItemThreeTv = (TextView) mReportThreeLayout.findViewById(R.id.user_result_testitem_tv);
        mBarCodeThreeImg = (ImageView) mReportThreeLayout.findViewById(R.id.result_user_pdf_report_idcard_img);
        userReportAdapter_3 = new UserReportAdapter(getActivity().getApplicationContext());
        mReportThreeLv.setAdapter(userReportAdapter_3);

        mReportFourLv = (ListView) mReportFourLayout.findViewById(R.id.user_result_report_lv);
        mIDcardFourTv = (TextView) mReportFourLayout.findViewById(R.id.user_result_idcard_tv);
        mStartTimeFourTv = (TextView) mReportFourLayout.findViewById(R.id.user_result_testtime_tv);
        mItemFourTv = (TextView) mReportFourLayout.findViewById(R.id.user_result_testitem_tv);
        mBarCodeFourImg = (ImageView) mReportFourLayout.findViewById(R.id.result_user_pdf_report_idcard_img);
        userReportAdapter_4 = new UserReportAdapter(getActivity().getApplicationContext());
        mReportFourLv.setAdapter(userReportAdapter_4);

        mReportFiveLv = (ListView) mReportFiveLayout.findViewById(R.id.user_result_report_lv);
        mIDcardFiveTv = (TextView) mReportFiveLayout.findViewById(R.id.user_result_idcard_tv);
        mStartTimeFiveTv = (TextView) mReportFiveLayout.findViewById(R.id.user_result_testtime_tv);
        mItemFiveTv = (TextView) mReportFiveLayout.findViewById(R.id.user_result_testitem_tv);
        mBarCodeFiveImg = (ImageView) mReportFiveLayout.findViewById(R.id.result_user_pdf_report_idcard_img);
        userReportAdapter_5 = new UserReportAdapter(getActivity().getApplicationContext());
        mReportFiveLv.setAdapter(userReportAdapter_5);

        mReportSixLv = (ListView) mReportSixLayout.findViewById(R.id.user_result_report_lv);
        mIDcardSixTv = (TextView) mReportSixLayout.findViewById(R.id.user_result_idcard_tv);
        mStartTimeSixTv = (TextView) mReportSixLayout.findViewById(R.id.user_result_testtime_tv);
        mItemSixTv = (TextView) mReportSixLayout.findViewById(R.id.user_result_testitem_tv);
        mBarCodeSixImg = (ImageView) mReportSixLayout.findViewById(R.id.result_user_pdf_report_idcard_img);
        userReportAdapter_6 = new UserReportAdapter(getActivity().getApplicationContext());
        mReportSixLv.setAdapter(userReportAdapter_6);

        mReportSevenLv = (ListView) mReportSevenLayout.findViewById(R.id.user_result_report_lv);
        mIDcardSevenTv = (TextView) mReportSevenLayout.findViewById(R.id.user_result_idcard_tv);
        mStartTimeSevenTv = (TextView) mReportSevenLayout.findViewById(R.id.user_result_testtime_tv);
        mItemSevenTv = (TextView) mReportSevenLayout.findViewById(R.id.user_result_testitem_tv);
        mBarCodeSevenImg = (ImageView) mReportSevenLayout.findViewById(R.id.result_user_pdf_report_idcard_img);
        userReportAdapter_7 = new UserReportAdapter(getActivity().getApplicationContext());
        mReportSevenLv.setAdapter(userReportAdapter_7);

        mReportEightLv = (ListView) mReportEightLayout.findViewById(R.id.user_result_report_lv);
        mIDcardEightTv = (TextView) mReportEightLayout.findViewById(R.id.user_result_idcard_tv);
        mStartTimeEightTv = (TextView) mReportEightLayout.findViewById(R.id.user_result_testtime_tv);
        mItemEightTv = (TextView) mReportEightLayout.findViewById(R.id.user_result_testitem_tv);
        mBarCodeEightImg = (ImageView) mReportEightLayout.findViewById(R.id.result_user_pdf_report_idcard_img);
        userReportAdapter_8 = new UserReportAdapter(getActivity().getApplicationContext());
        mReportEightLv.setAdapter(userReportAdapter_8);

        mReportOneRb = (RadioButton) rootView.findViewById(R.id.result_user_report_hole_one_btn);
        mReportTwoRb = (RadioButton) rootView.findViewById(R.id.result_user_report_hole_two_btn);
        mReportThreeRb = (RadioButton) rootView.findViewById(R.id.result_user_report_hole_three_btn);
        mReportFourRb = (RadioButton) rootView.findViewById(R.id.result_user_report_hole_four_btn);
        mReportFiveRb = (RadioButton) rootView.findViewById(R.id.result_user_report_hole_five_btn);
        mReportSixRb = (RadioButton) rootView.findViewById(R.id.result_user_report_hole_six_btn);
        mReportSevenRb = (RadioButton) rootView.findViewById(R.id.result_user_report_hole_seven_btn);
        mReportEightRb = (RadioButton) rootView.findViewById(R.id.result_user_report_hole_eight_btn);

        initVariable();
        initAmpDigitalFilter();
        initAmpAysTypeWindow();
        initMeltPeakAlgorithm();
        initOptimizationModeWindow();
        initPeekTypeWindow();
		initMoreActions();
        initAMPAlgorithm();//基本参数-扩增曲线算法枚举
    }


    private void initVariable(){
        configTargets = new ArrayList<ConfigTarget>();
        mRawCurveList = new ArrayList<BiowayCurveList>();
        mCheckRawList = new ArrayList<BiowayCurveList>();

        mAMPCurveList = new ArrayList<BiowayCurveList>();
        mCheckAMPList = new ArrayList<BiowayCurveList> ();

        mMeltCurveList = new ArrayList<BiowayCurveList>();
        mCheckMeltList = new ArrayList<BiowayCurveList>();

        mPeakCurveList = new ArrayList<BiowayCurveList>();
        mCheckPeakList = new ArrayList<BiowayCurveList>();

        showCurveList = new ArrayList<BiowayCurveList>();
        mExlResults = new ArrayList<BiowayExlResult>();
        mCheckExlResults = new ArrayList<BiowayExlResult>();
        optimizations = new ArrayList<PeaktypeOptimization>();

        resultAdapter.setResultList(mCheckExlResults);
        mResultListView.setAdapter(resultAdapter);
        isOneEnable = isTwoEnable = isThreeEnable = isFourEnable = false;
        isFiveEnable = isSixEnable = isSevenEnable = isEightEnable = false;

    }

    @Override
    protected void initListener() {
        mMoreActionTv.setOnClickListener(this);
        mBasicParamTitleTv.setOnClickListener(this);
        mMeltParamTitleTv.setOnClickListener(this);

        //报告
        mReportTv.setOnClickListener(this);
        mPrintCurTv.setOnClickListener(this);
        mPrintAllTv.setOnClickListener(this);
        mBackResultImg.setOnClickListener(this);

        mReportHoleRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkId) {
                if (checkId == R.id.result_user_report_hole_one_btn) {
                    if (isOneEnable) {
                        reportView = 1;
                        mReportOneLayout.setVisibility(View.VISIBLE);
                        mReportTwoLayout.setVisibility(View.GONE);
                        mReportThreeLayout.setVisibility(View.GONE);
                        mReportFourLayout.setVisibility(View.GONE);
                        mReportFiveLayout.setVisibility(View.GONE);
                        mReportSixLayout.setVisibility(View.GONE);
                        mReportSevenLayout.setVisibility(View.GONE);
                        mReportEightLayout.setVisibility(View.GONE);
                        showReportResult(1);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.running_toast_current_hole_no_report), Toast.LENGTH_LONG).show();
                    }

                } else if (checkId == R.id.result_user_report_hole_two_btn) {
                    //显示孔2的报告
                    if (isTwoEnable) {
                        reportView = 2;
                        mReportOneLayout.setVisibility(View.GONE);
                        mReportTwoLayout.setVisibility(View.VISIBLE);
                        mReportThreeLayout.setVisibility(View.GONE);
                        mReportFourLayout.setVisibility(View.GONE);
                        mReportFiveLayout.setVisibility(View.GONE);
                        mReportSixLayout.setVisibility(View.GONE);
                        mReportSevenLayout.setVisibility(View.GONE);
                        mReportEightLayout.setVisibility(View.GONE);
                        showReportResult(2);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.running_toast_current_hole_no_report), Toast.LENGTH_LONG).show();
                    }

                } else if (checkId == R.id.result_user_report_hole_three_btn) {
                    //显示孔3的报告
                    if (isThreeEnable) {
                        reportView = 3;
                        mReportOneLayout.setVisibility(View.GONE);
                        mReportTwoLayout.setVisibility(View.GONE);
                        mReportThreeLayout.setVisibility(View.VISIBLE);
                        mReportFourLayout.setVisibility(View.GONE);
                        mReportFiveLayout.setVisibility(View.GONE);
                        mReportSixLayout.setVisibility(View.GONE);
                        mReportSevenLayout.setVisibility(View.GONE);
                        mReportEightLayout.setVisibility(View.GONE);
                        showReportResult(3);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.running_toast_current_hole_no_report), Toast.LENGTH_LONG).show();
                    }

                } else if (checkId == R.id.result_user_report_hole_four_btn) {
                    //显示孔4的报告
                    if (isFourEnable) {
                        reportView = 4;
                        mReportOneLayout.setVisibility(View.GONE);
                        mReportTwoLayout.setVisibility(View.GONE);
                        mReportThreeLayout.setVisibility(View.GONE);
                        mReportFourLayout.setVisibility(View.VISIBLE);
                        mReportFiveLayout.setVisibility(View.GONE);
                        mReportSixLayout.setVisibility(View.GONE);
                        mReportSevenLayout.setVisibility(View.GONE);
                        mReportEightLayout.setVisibility(View.GONE);
                        showReportResult(4);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.running_toast_current_hole_no_report),Toast.LENGTH_LONG).show();
                    }

                } else if (checkId == R.id.result_user_report_hole_five_btn) {
                    //显示孔5的报告
                    if (isFiveEnable) {
                        reportView = 5;
                        mReportOneLayout.setVisibility(View.GONE);
                        mReportTwoLayout.setVisibility(View.GONE);
                        mReportThreeLayout.setVisibility(View.GONE);
                        mReportFourLayout.setVisibility(View.GONE);
                        mReportFiveLayout.setVisibility(View.VISIBLE);
                        mReportSixLayout.setVisibility(View.GONE);
                        mReportSevenLayout.setVisibility(View.GONE);
                        mReportEightLayout.setVisibility(View.GONE);
                        showReportResult(5);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.running_toast_current_hole_no_report), Toast.LENGTH_LONG).show();
                    }

                } else if (checkId == R.id.result_user_report_hole_six_btn) {
                    //显示孔6的报告
                    if (isSixEnable) {
                        reportView = 6;
                        mReportOneLayout.setVisibility(View.GONE);
                        mReportTwoLayout.setVisibility(View.GONE);
                        mReportThreeLayout.setVisibility(View.GONE);
                        mReportFourLayout.setVisibility(View.GONE);
                        mReportFiveLayout.setVisibility(View.GONE);
                        mReportSixLayout.setVisibility(View.VISIBLE);
                        mReportSevenLayout.setVisibility(View.GONE);
                        mReportEightLayout.setVisibility(View.GONE);
                        showReportResult(6);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.running_toast_current_hole_no_report), Toast.LENGTH_LONG).show();
                    }
                } else if (checkId == R.id.result_user_report_hole_seven_btn) {
                    //显示孔7的报告
                    if (isSevenEnable) {
                        reportView = 7;
                        mReportOneLayout.setVisibility(View.GONE);
                        mReportTwoLayout.setVisibility(View.GONE);
                        mReportThreeLayout.setVisibility(View.GONE);
                        mReportFourLayout.setVisibility(View.GONE);
                        mReportFiveLayout.setVisibility(View.GONE);
                        mReportSixLayout.setVisibility(View.GONE);
                        mReportSevenLayout.setVisibility(View.VISIBLE);
                        mReportEightLayout.setVisibility(View.GONE);
                        showReportResult(7);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.running_toast_current_hole_no_report), Toast.LENGTH_LONG).show();
                    }

                } else if (checkId == R.id.result_user_report_hole_eight_btn) {
                    //显示孔8的报告
                    if (isEightEnable) {
                        reportView = 8;
                        mReportOneLayout.setVisibility(View.GONE);
                        mReportTwoLayout.setVisibility(View.GONE);
                        mReportThreeLayout.setVisibility(View.GONE);
                        mReportFourLayout.setVisibility(View.GONE);
                        mReportFiveLayout.setVisibility(View.GONE);
                        mReportSixLayout.setVisibility(View.GONE);
                        mReportSevenLayout.setVisibility(View.GONE);
                        mReportEightLayout.setVisibility(View.VISIBLE);
                        showReportResult(8);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(),
                                getActivity().getResources().getString(R.string.running_toast_current_hole_no_report), Toast.LENGTH_LONG).show();
                    }


                }
            }
        });

        mAllChannelCk.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isAllChanelCheck = isCheck;
                if(isAllChanelCheck){
                    isHEXCheck = true;
                    isFAMCheck = true;
                    isROXCheck = true;
                    isCY5Check = true;
                }else{
                    isHEXCheck = false;
                    isFAMCheck = false;
                    isROXCheck = false;
                    isCY5Check = false;
                }
                setChannelState();
                MessageEvent mAllChanelMsg = new MessageEvent();
                mAllChanelMsg.setMsgType(MsgType.MSG_SHOW_ALL_CHANEL_RESULT);
                EventBus.getDefault().post(mAllChanelMsg);
            }

        });

        mFAM_ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isFAMCheck = isCheck;
                if(!isFAMCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isHEXCheck){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mFamMsg = new MessageEvent();
                mFamMsg.setMsgType(MsgType.MSG_SHOW_FAM_CHANNEL_CURVE);
                EventBus.getDefault().post(mFamMsg);
            }
        });

        mHEX_ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isHEXCheck = isCheck;
                if(!isHEXCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isFAMCheck){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mHEXMsg = new MessageEvent();
                mHEXMsg.setMsgType(MsgType.MSG_SHOW_HEX_CHANNEL_CURVE);
                EventBus.getDefault().post(mHEXMsg);

            }
        });

        mROX_ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isROXCheck = isCheck;
                if(!isROXCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isFAMCheck && isCY5Check && isHEXCheck ){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mROXMsg = new MessageEvent();
                mROXMsg.setMsgType(MsgType.MSG_SHOW_ROX_CHANNEL_CURVE);
                EventBus.getDefault().post(mROXMsg);
            }
        });

        mCY5_ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isCY5Check = isCheck;
                if(!isCY5Check){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isHEXCheck && isFAMCheck){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mCY5Msg = new MessageEvent();
                mCY5Msg.setMsgType(MsgType.MSG_SHOW_CY5_CHANNEL_CURVE);
                EventBus.getDefault().post(mCY5Msg);

            }
        });



        mHoleOneTv.setOnClickListener(this);
        mHoleTwoTv.setOnClickListener(this);
        mHoleThreeTv.setOnClickListener(this);
        mHoleFourTv.setOnClickListener(this);
        mHoleFiveTv.setOnClickListener(this);
        mHoleSixTv.setOnClickListener(this);
        mHoleSevenTv.setOnClickListener(this);
        mHoleEightTv.setOnClickListener(this);
        mSecAnalyseTv.setOnClickListener(this);
        mAllHoleCk.setOnClickListener(this);
        mAMPAlgorithmTv.setOnClickListener(this);
        mCurveChartGg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.user_result_bioway_rawcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mReportTv.setVisibility(View.VISIBLE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 1;
                    getAllRawCurve();
                    drawRawCurve();
                } else if (checkedId == R.id.user_result_bioway_ampcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                        mReportTv.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 2;
                    getAllAPMCurve();
                    drawAMPCurve();
                } else if (checkedId == R.id.user_result_bioway_meltcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mReportTv.setVisibility(View.VISIBLE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 3;
                    getAllMeltCurve();
                    drawMeltCurve();
                } else if (checkedId == R.id.user_result_bioway_peakcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mReportTv.setVisibility(View.VISIBLE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 4;
                    getAllPeakCurve();
                    drawPeakCurve();
                } else if (checkedId == R.id.user_result_bioway_list_rb) {
                    currentView = 5;
                    if (!isExcelVisible) {
                        isChartVisible = false;
                        isExcelVisible = true;
                        isSecAysVisible = false;
                    }
                    if(isExcelVisible){
                        mChannelLayout.setVisibility(View.VISIBLE);
                        mHoleLayout.setVisibility(View.VISIBLE);
                        mReportTv.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.VISIBLE);
                        mResultChartLayout.setVisibility(View.GONE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                    }
                    getShowExlResults();
                    MessageEvent messageEvent = new MessageEvent();
                    messageEvent.setMsgType(MsgType.MSG_SHOW_EXCEL_RESULT);
                    EventBus.getDefault().post(messageEvent);
                }else if(checkedId ==R.id.user_result_bioway_set_paramter_rb){
                    currentView = 6;
                    if (!isSecAysVisible) {
                        isChartVisible = false;
                        isExcelVisible = false;
                        isSecAysVisible = true;
                    }
                    if(isSecAysVisible){
                        mChannelLayout.setVisibility(View.GONE);
                        mHoleLayout.setVisibility(View.INVISIBLE);
                        mSecAnalyseTv.setVisibility(View.VISIBLE);
                        mReportTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.GONE);
                        mSecAnalyseLayout.setVisibility(View.VISIBLE);
                        //获取二次分析进行分析
                        if(isMelt){
                            mMeltParamTitleTv.setVisibility(View.VISIBLE);
                        }else {
                            mMeltParamTitleTv.setVisibility(View.INVISIBLE);
                        }
                        if(null != configTargets){
                            if(null != mAmpAlgorithmAdapter){
                                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                            }
                            if(null != mMeltParamterAdapter){
                                mMeltParamterAdapter.setMeltParamter(configTargets);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerEventBus();
    }

    @Override
    public void onResume() {
        super.onResume();
        //绘制曲线

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideLoadingDialog();
        clearAllList();
        unregisterEventBus();
        if(printThread != null && printThread.isAlive()) {
            printThread.interrupt();
            printThread = null;
        }
        markView = null;
        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
        if(null != mPeekTypeWindow){
            mPeekTypeWindow.dismiss();
            mPeekTypeWindow = null;
        }


        if (null != mAMPAlgWindow) {
            mAMPAlgWindow.dismiss();
            mAMPAlgWindow = null;
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

        if (null != mAMPAlgorithmList) {
            mAMPAlgorithmList.clear();
            mAMPAlgorithmList = null;
        }

        if (null != mMoreActionWindows) {
            mMoreActionWindows.dismiss();
            mMoreActionWindows = null;
        }

        if(null != mMoreActions){
            mMoreActions.clear();
            mMoreActions = null;
        }
        if(null != coordinateDialog){
            coordinateDialog.dismiss();
            coordinateDialog = null;
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

    private void setChannelState(){
        if(isFAMCheck){
            mFAM_ChannelTv.setCheckState(R.drawable.item_chanel_check,isFAMCheck);
        }else {
            mFAM_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isFAMCheck);
        }
        if(isHEXCheck){
            mHEX_ChannelTv.setCheckState(R.drawable.item_chanel_check,isHEXCheck);
        }else{
            mHEX_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isHEXCheck);
        }

        if(isROXCheck){
            mROX_ChannelTv.setCheckState(R.drawable.item_chanel_check,isROXCheck);
        }else{
            mROX_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isROXCheck);
        }

        if(isCY5Check){
            mCY5_ChannelTv.setCheckState(R.drawable.item_chanel_check,isCY5Check);
        }else{
            mCY5_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isCY5Check);
        }


        if(isCY5Check && isFAMCheck &&isROXCheck && isHEXCheck){
            isAllChanelCheck = true;
            mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
        }else{
            isAllChanelCheck = false;
            mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
        }
    }

    /**
     * 设置所有的孔位位选定状态
     */
    private void checkWellState(){

        for(BiowayCurveList curveList : showCurveList){

            if(curveList.getWellNum().equals("A1")){
                isOneCheck = true;
                isOneEnable = true;
                mHoleOneTv.setEnabled(true);
                setHodeState(isOneCheck, mHoleOneTv);
            }else if(curveList.getWellNum().equals("A2")){
                isTwoCheck = true;
                isTwoEnable = true;
                mHoleTwoTv.setEnabled(true);
                setHodeState(isTwoCheck, mHoleTwoTv);
            }else if(curveList.getWellNum().equals("A3")){
                isThreeCheck = true;
                isThreeEnable = true;
                mHoleThreeTv.setEnabled(true);
                setHodeState(isThreeCheck, mHoleThreeTv);
            } else if(curveList.getWellNum().equals("A4")){
                isFourCheck = true;
                isFourEnable = true;
                mHoleFourTv.setEnabled(true);
                setHodeState(isFourCheck, mHoleFourTv);
            } else if(curveList.getWellNum().equals("A5")){
                isFiveCheck = true;
                isFiveEnable = true;
                mHoleFiveTv.setEnabled(true);
                setHodeState(isFiveCheck, mHoleFiveTv);
            } else if(curveList.getWellNum().equals("A6")){
                isSixCheck = true;
                isSixEnable = true;
                mHoleSixTv.setEnabled(true);
                setHodeState(isSixCheck, mHoleSixTv);
            } else if(curveList.getWellNum().equals("A7")){
                isSevenCheck = true;
                isSevenEnable = true;
                mHoleSevenTv.setEnabled(true);
                setHodeState(isSevenCheck, mHoleSevenTv);
            } else if(curveList.getWellNum().equals("A8")){
                isEightCheck = true;
                isEightEnable = true;
                mHoleEightTv.setEnabled(true);
                setHodeState(isEightCheck, mHoleEightTv);
            }
        }

        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck) {
            isAllCheck = true;
            setHodeState(isAllCheck, mAllHoleCk);
        }
    }


    private void clearRawList(){
        if(null != mRawCurveList){
            mRawCurveList.clear();
            mRawCurveList = null;
        }
        if(null != mCheckRawList){
            mCheckRawList.clear();
            mCheckRawList = null;
        }
    }

    private void clearAMPList(){
        if(null != mAMPCurveList){
            mAMPCurveList.clear();
            mAMPCurveList = null;
        }
        if(null != mCheckAMPList){
            mCheckAMPList.clear();
            mCheckAMPList = null;
        }
    }

    private void clearMeltList() {

        if (null != mMeltCurveList) {
            mMeltCurveList.clear();
            mMeltCurveList = null;
        }
        if (null != mCheckMeltList) {
            mCheckMeltList.clear();
            mCheckMeltList = null;
        }
    }
    private void clearPeakList(){

        if (null != mPeakCurveList) {
            mPeakCurveList.clear();
            mPeakCurveList = null;
        }
        if (null != mCheckPeakList) {
            mCheckPeakList.clear();
            mCheckPeakList = null;
        }

    }

    private void clearAllList() {
        clearRawList();
        clearAMPList();
        clearMeltList();
        clearPeakList();
        if(null != showCurveList){
            showCurveList.clear();
            showCurveList = null;
        }

        if(null != mExlResults){
            mExlResults.clear();
            mExlResults = null;
        }

        if(null != mCheckExlResults){
            mCheckExlResults.clear();
            mCheckExlResults = null;
        }
        if(null != configTargets){
            configTargets.clear();
            configTargets = null;
        }

        if (null != mReportResults) {
            mReportResults.clear();
            mReportResults = null;
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

    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN ,sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()){
            case MsgType.MSG_FAIL:
                hideLoadingDialog();
                String failMsg = (String)messageEvent.getData();
                Toast.makeText(getActivity().getApplicationContext(),failMsg,Toast.LENGTH_LONG).show();
                break;
            case MsgType.MSG_SHOW_RESULT:
                Log.i("RNA","显示结果");
                showCurveList.clear();
                mRawCurveList.clear();
                mCheckRawList.clear();
                mRawCurveList.addAll(BiowayManager.getInstance().getRawCurve());
                mCheckRawList.addAll(mRawCurveList);
                if(PcrApplication.isHasSecondRNA){
                    showCurveList.addAll(BiowayManager.getInstance().getMeltCurve());
                }else{
                    showCurveList.addAll(BiowayManager.getInstance().getRawCurve());
                }
                for(BiowayCurveList curveList : showCurveList){
                    if(curveList.getChannelNo() == 1){
                        isFAMCheck = true;
                    }else if(curveList.getChannelNo() == 2){
                        isHEXCheck = true;
                    }else if(curveList.getChannelNo() == 3){
                        isROXCheck = true;
                    }else if(curveList.getChannelNo() ==4){
                        isCY5Check = true;
                    }
                }

                checkWellState();
                setChannelState();
                drawchart(mCheckRawList,resultChart);

                //获取实验参数
                getExperimentParameter();
                break;

            case MsgType.MSG_SHOW_SECOND_RESULT:
                //显示二次分析的结果
                hideLoadingDialog();
                showCurveList.clear();
                if(PcrApplication.isHasSecondRNA){
                    showCurveList.addAll(BiowayManager.getInstance().getMeltCurve());
                }else{
                    showCurveList.addAll(BiowayManager.getInstance().getRawCurve());
                }
                for(BiowayCurveList curveList : showCurveList){
                    if(curveList.getChannelNo() == 1){
                        isFAMCheck = true;
                    }else if(curveList.getChannelNo() == 2){
                        isHEXCheck = true;
                    }else if(curveList.getChannelNo() == 3){
                        isROXCheck = true;
                    }else if(curveList.getChannelNo() == 4) {
                        isCY5Check = true;
                    }
                }
                setChannelState();
                checkWellState();
                getExperimentParameter();

                switch (currentView){
                    case 1:
                        getAllRawCurve();
                        drawRawCurve();
                        break;
                    case 2:
                        getAllAPMCurve();
                        drawAMPCurve();
                        break;
                    case 3:
                        getAllMeltCurve();
                        drawMeltCurve();
                        break;
                    case 4:
                        getAllPeakCurve();
                        drawPeakCurve();
                        break;
                    case 5:
                        getShowExlResults();
                        if(null != resultAdapter){
                            Collections.sort(mCheckExlResults);
                            Collections.reverse(mCheckExlResults);
                            resultAdapter.setResultList(mCheckExlResults);
                            resultAdapter.notifyDataSetChanged();
                        }
                        break;

                }
                //弹出对话框提示是否需要保存
                onSaveSecondParseResult();
                break;

            case MsgType.MSG_SHOW_ALL_CHANEL_RESULT:
            case MsgType.MSG_SHOW_FAM_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_HEX_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_ROX_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_CY5_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_QUASAR_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_ATTO_CHANNEL_CURVE:
                switch (currentView){
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
                        getAllMeltCurve();
                        drawMeltCurve();
                        break;

                    case 4:
                        //峰值曲线
                        getAllPeakCurve();
                        drawPeakCurve();
                        break;
                    case 5:
                        getShowExlResults();
                        if(null != resultAdapter){
                            Collections.sort(mCheckExlResults);
                            Collections.reverse(mCheckExlResults);
                            resultAdapter.setResultList(mCheckExlResults);
                            resultAdapter.notifyDataSetChanged();
                        }
                        break;
                }
                break;
            case MsgType.MSG_SHOW_EXCEL_RESULT:
                if(null != resultAdapter){
                    Collections.sort(mCheckExlResults);
                    Collections.reverse(mCheckExlResults);
                    resultAdapter.setResultList(mCheckExlResults);
                    resultAdapter.notifyDataSetChanged();
                }
                break;

            case MsgType.MSG_SAVE_PDF:
                boolean result = (boolean) messageEvent.getData();
                hideLoadingDialog();
                if (result) {
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.running_toast_print_done), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.running_toast_print_fail), Toast.LENGTH_LONG).show();
                }
                break;

            case MsgType.MSG_PRINT_ALL_PDF:
                boolean save_result = (boolean) messageEvent.getData();
                if (save_result) {
                    Log.i("Report", "保存完成");
                    hideLoadingDialog();
                    Toast.makeText(getActivity().getApplicationContext(),
                            getActivity().getResources().getString(R.string.running_toast_print_done), Toast.LENGTH_LONG).show();
                }else {
                    hideLoadingDialog();
                    if(null != printThread && printThread.isAlive()){
                        printThread.interrupt();
                        printThread = null;
                    }
                }


        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.user_result_bioway_hole_one_btn) {
            if (isOneEnable) {
	            isOneCheck = !isOneCheck;
	            if (!isOneCheck) {
	                if (isAllCheck) {
	                    isAllCheck = false;
	                    setHodeState(isAllCheck, mAllHoleCk);
	                }
	            } else {
	                checkAllHole();
	            }
	            setHodeState(isOneCheck, (TextView) v);
	            updateWellResult();
            }
        } else if (id == R.id.user_result_bioway_hole_two_btn) {
            if (isTwoEnable) {
	            isTwoCheck = !isTwoCheck;
	            if (!isTwoCheck) {
	                if (isAllCheck) {
	                    isAllCheck = false;
	                    setHodeState(isAllCheck, mAllHoleCk);
	                }
	            } else {
	                checkAllHole();
	            }
	            setHodeState(isTwoCheck, (TextView) v);
	            updateWellResult();
            }
        } else if (id == R.id.user_result_bioway_hole_three_btn) {
            if (isThreeEnable) {
	            isThreeCheck = !isThreeCheck;
	            if (!isThreeCheck) {
	                if (isAllCheck) {
	                    isAllCheck = false;
	                    setHodeState(isAllCheck, mAllHoleCk);
	                }
	            } else {
	                checkAllHole();
	            }
	            setHodeState(isThreeCheck, (TextView) v);
	            updateWellResult();
            }
        } else if (id == R.id.user_result_bioway_hole_four_btn) {
            if (isFourEnable) {
	            isFourCheck = !isFourCheck;
	            if (!isFourCheck) {
	                if (isAllCheck) {
	                    isAllCheck = false;
	                    setHodeState(isAllCheck, mAllHoleCk);
	                }
	            } else {
	                checkAllHole();
	            }
	            setHodeState(isFourCheck, (TextView) v);
	            updateWellResult();
            }
        } else if (id == R.id.user_result_bioway_hole_five_btn) {
            if (isFiveEnable) {
	            isFiveCheck = !isFiveCheck;
	            if (!isFiveCheck) {
	                if (isAllCheck) {
	                    isAllCheck = false;
	                    setHodeState(isAllCheck, mAllHoleCk);
	                }
	            } else {
	                checkAllHole();
	            }
	            setHodeState(isFiveCheck, (TextView) v);
	            updateWellResult();
            }
        } else if (id == R.id.user_result_bioway_hole_six_btn) {
            if (isSixEnable) {
	            isSixCheck = !isSixCheck;
	            if (!isSixCheck) {
	                if (isAllCheck) {
	                    isAllCheck = false;
	                    setHodeState(isAllCheck, mAllHoleCk);
	                }
	            } else {
	                checkAllHole();
	            }
	            setHodeState(isSixCheck, (TextView) v);
	            updateWellResult();
            }
        } else if (id == R.id.user_result_bioway_hole_seven_btn) {
            if (isSevenEnable) {
	            isSevenCheck = !isSevenCheck;
	            if (!isSevenCheck) {
	                if (isAllCheck) {
	                    isAllCheck = false;
	                    setHodeState(isAllCheck, mAllHoleCk);
	                }
	            } else {
	                checkAllHole();
	            }
	            setHodeState(isSevenCheck, (TextView) v);
	            updateWellResult();
            }
        } else if (id == R.id.user_result_bioway_hole_eight_btn) {
            if (isEightEnable) {
	            isEightCheck = !isEightCheck;
	            if (!isEightCheck) {
	                if (isAllCheck) {
	                    isAllCheck = false;
	                    setHodeState(isAllCheck, mAllHoleCk);
	                }
	            } else {
	                checkAllHole();
	            }
	            setHodeState(isEightCheck, (TextView) v);
	            updateWellResult();
            }
        } else if (id == R.id.user_running_bioway_sec_analyse_tv) {

            secondAnalayse();

        } else if (id == R.id.user_result_bioway_running_all_hole_ck) {
            if (isAllCheck) {
                isAllCheck = false;
                setAllHoleUnCheck();
            } else {
                isAllCheck = true;
                setAllHoleCheck();

            }
            updateWellResult();
            setHodeState(isAllCheck, mAllHoleCk);
        }else if(id == R.id.user_bioway_result_basic_param_title_tv){
            mBasicParamTitleTv.setTextColor(getResources().getColor(R.color.white));
            mBasicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mMeltParamTitleTv.setTextColor(getResources().getColor(R.color.black));
            mMeltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            mAmpParamterLayout.setVisibility(View.VISIBLE);
            mMeltParamterLayout.setVisibility(View.GONE);
            if(null != mAmpAlgorithmAdapter){
                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
            }
        }else if(id == R.id.user_bioway_result_melt_param_title_tv){
            mMeltParamTitleTv.setTextColor(getResources().getColor(R.color.white));
            mMeltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
            mBasicParamTitleTv.setTextColor(getResources().getColor(R.color.black));
            mBasicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
            mAmpParamterLayout.setVisibility(View.GONE);
            mMeltParamterLayout.setVisibility(View.VISIBLE);
            if(null != mMeltParamterAdapter){
                mMeltParamterAdapter.setMeltParamter(configTargets);
            }
        }else if(id == R.id.user_running_bioway_ex_result_more_tv){
            //弹出morewindows
            expandMoreActions(v);
        }else if(id == R.id.user_bioway_result_sec_analyse_amp_algorithm_tv){
            expandAmpAlg(v);
        }else if (id == R.id.running_user_report_tv) {
            //显示报告
            mResultsLayout.setVisibility(View.GONE);
            mReportLayout.setVisibility(View.VISIBLE);
            mChannelLayout.setVisibility(View.GONE);
            mHoleLayout.setVisibility(View.GONE);
            mSecAnalyseTv.setVisibility(View.GONE);
            mReportResults.clear();
            mReportResults.addAll(BiowayManager.getInstance().getExlResults());
            Collections.sort(mReportResults);
            setPrintBtnState();


        } else if (id == R.id.result_user_back_result_img) {
            mResultsLayout.setVisibility(View.VISIBLE);
            mReportLayout.setVisibility(View.GONE);

            mChannelLayout.setVisibility(View.VISIBLE);
            mHoleLayout.setVisibility(View.VISIBLE);
            mSecAnalyseTv.setVisibility(View.INVISIBLE);

        } else if (id == R.id.result_user_print_current_tv) {
            //打印当前报告
            if (reportView != -1 && reportView > 0) {
                switch (reportView){
                    case 1:
                        printCurPdfReport("A"+reportView,mReportOneLayout);
                        break;
                    case 2:
                        printCurPdfReport("A"+reportView,mReportTwoLayout);
                        break;
                    case 3:
                        printCurPdfReport("A"+reportView,mReportThreeLayout);
                        break;
                    case 4:
                        printCurPdfReport("A"+reportView,mReportFourLayout);
                        break;
                    case 5:
                        printCurPdfReport("A"+reportView,mReportFiveLayout);
                        break;
                    case 6:
                        printCurPdfReport("A"+reportView,mReportSixLayout);
                        break;
                    case 7:
                        printCurPdfReport("A"+reportView,mReportSevenLayout);
                        break;
                    case 8:
                        printCurPdfReport("A"+reportView,mReportEightLayout);
                        break;

                }

            }
        } else if (id == R.id.result_user_print_all_tv) {
            printAllPdfReport();
        }

    }

    private void setPrintBtnState() {
        if (isOneEnable) {
            mReportOneRb.setEnabled(true);
        } else {
            mReportOneRb.setEnabled(false);
        }

        if (isTwoEnable) {
            mReportTwoRb.setEnabled(true);
        } else {
            mReportTwoRb.setEnabled(false);
        }

        if (isThreeEnable) {
            mReportThreeRb.setEnabled(true);
        } else {
            mReportThreeRb.setEnabled(false);
        }

        if (isFourEnable) {
            mReportFourRb.setEnabled(true);
        } else {
            mReportFourRb.setEnabled(false);
        }

        if (isFiveEnable) {
            mReportFiveRb.setEnabled(true);
        } else {
            mReportFiveRb.setEnabled(false);
        }

        if (isSixEnable) {
            mReportSixRb.setEnabled(true);
        } else {
            mReportSixRb.setEnabled(false);
        }

        if (isSevenEnable) {
            mReportSevenRb.setEnabled(true);
        } else {
            mReportSevenRb.setEnabled(false);
        }

        if (isEightEnable) {
            mReportEightRb.setEnabled(true);
        } else {
            mReportEightRb.setEnabled(false);
        }

        if (null != mReportResults && mReportResults.size() > 0) {
            BiowayExlResult slanExlResult = mReportResults.get(0);//第一個
            String wellName = slanExlResult.getWellNum();
            if (wellName.equals("A1")) {

                reportView = 1;
                mReportOneRb.setChecked(true);

            } else if (wellName.equals("A2")) {
                reportView = 2;
                mReportTwoRb.setChecked(true);

            } else if (wellName.equals("A3")) {
                reportView = 3;
                mReportThreeRb.setChecked(true);

            } else if (wellName.equals("A4")) {
                reportView = 4;
                mReportFourRb.setChecked(true);

            } else if (wellName.equals("A5")) {
                reportView = 5;
                mReportFiveRb.setChecked(true);

            } else if (wellName.equals("A6")) {
                reportView = 6;
                mReportSixRb.setChecked(true);
            } else if (wellName.equals("A7")) {
                reportView = 7;
                mReportSevenRb.setChecked(true);
            } else if (wellName.equals("A8")) {
                reportView = 8;
                mReportEightRb.setChecked(true);
            }
        }
    }


    private void onSaveSecondParseResult(){
        MessageEvent updateResultMsg = new MessageEvent();
        updateResultMsg.setMsgType(MsgType.MSG_UPDATE_EX_RESULT);
        EventBus.getDefault().postSticky(updateResultMsg);
    }


    private LimitLine initLimitLine(double threshold,String threshold_name,int number){
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


    public void drawchart(List<BiowayCurveList> curve_list, LineChart lineChart) {
        if (curve_list == null) {
            curve_list = new ArrayList<>();
        }
        if(lineChart!=null) {
            Description description = new Description();
            // description.setText("RawCurve");
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
                    } else if (curve_list.get(i).getCurve_hole().equals("HEX")){
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
                // controlRn = curve_list.get(0).getControlRn();
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
            //LimitLine limitLine = initLimitLine(controlRn);
            //leftAxis.addLimitLine(limitLine);


            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线
            xAxis.setLabelCount(curve_list.size(), false);
            lineChart.post(new Runnable() {
                @Override
                public void run() {
                    markView = new LineChartMarkView(getActivity().getApplicationContext());
                    lineChart.setMarker(markView);
                }
            });
            lineChart.invalidate();
        }
    }

    private LineChartMarkView markView;
    public void drawchart(List<BiowayCurveList> curve_list, LineChart lineChart, String describe, int type) {
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
                    } else if (curve_list.get(i).getCurve_hole().equals("HEX")){
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
            if(isCy5Threshold && isCY5Check){
                LimitLine limitLine = initLimitLine(cy5Threshold,"cy5Threshold",3);
                leftAxis.addLimitLine(limitLine);
            }


            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线
            xAxis.setLabelCount(curve_list.size(), false);
            lineChart.post(new Runnable() {
                @Override
                public void run() {
                    markView = new LineChartMarkView(getActivity().getApplicationContext());
                    lineChart.setMarker(markView);
                }
            });
            lineChart.invalidate();
        }
    }

    /**清除图表**/
    public void clearChart(){
        resultChart.clear();
        resultChart.notifyDataSetChanged();
        resultChart.invalidate();
    }



    /*******************************获取所有的扩增曲线值**********************************/
    private void  getAllRawCurve(){
        getClickRawCurveResult();
    }



    private void getAllAPMCurve(){
        getClickAMPCurveResult();
    }

    //根据选择的通道显示扩增曲线/

    private void drawAMPCurve(){
        mCheckAMPList.clear();
        if (null != mAMPCurveList && mAMPCurveList.size() > 0) {
            for (BiowayCurveList curveList : mAMPCurveList) {
                if(curveList.getCurve_hole().equals("FAM")){
                    if (isFAMCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("HEX")){
                    if (isHEXCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("ROX")){
                    if (isROXCheck) {
                        mCheckAMPList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("CY5")){
                    if (isCY5Check) {
                        mCheckAMPList.add(curveList);
                    }
                }
            }
        }
        clearChart();
        if(!isFamThreshold && !isHexThreshold && !isRoxThreshold && !isCy5Threshold){
            drawchart(mCheckAMPList, resultChart);
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
            drawchart(mCheckAMPList, resultChart,"AMPCurve",2);
        }
    }


    //画不同通道的线
    /////////////////////////////////////////////////////////原始数据///////////////////////////////////////////
    private void drawRawCurve(){
        if(null != mRawCurveList){
            mCheckRawList.clear();
            if(mRawCurveList.size()>0){
                for(BiowayCurveList curveList : mRawCurveList){
                    if(curveList.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("HEX")){
                        if(isHEXCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("ROX")){
                        if(isROXCheck){
                            mCheckRawList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("CY5")){
                        if(isCY5Check){
                            mCheckRawList.add(curveList);
                        }
                    }
                }
            }

        }
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
        drawchart(mCheckRawList,resultChart);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /*******************************获取所有的熔解曲线值******Melt***************************/
    private void getAllMeltCurve(){

        getClickMeltCurveResult();
    }


    //根据选择的通道显示扩增曲线/

    private void drawMeltCurve(){
        mCheckMeltList.clear();
        if(null != mMeltCurveList){
            if(mMeltCurveList.size()>0){
                for(BiowayCurveList curveList : mMeltCurveList){
                    if(curveList.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
                            mCheckMeltList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("HEX")){
                        if(isHEXCheck){
                            mCheckMeltList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("ROX")){
                        if(isROXCheck){
                            mCheckMeltList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("CY5")){
                        if(isCY5Check){
                            mCheckMeltList.add(curveList);
                        }
                    }
                }
            }

        }
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
        drawchart(mCheckMeltList,resultChart);
    }

    /**************************************************获取峰值数据*************************************************/
    private void getAllPeakCurve(){
        getClickPeakCurveResult();

    }

    //根据选择的通道显示扩增曲线/

    private void drawPeakCurve(){
        if(null != mPeakCurveList){
            mCheckPeakList.clear();
            if(mPeakCurveList.size()>0){
                for(BiowayCurveList curveList : mPeakCurveList){
                    if(curveList.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
                            mCheckPeakList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("HEX")){
                        if(isHEXCheck){
                            mCheckPeakList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("ROX")){
                        if(isROXCheck){
                            mCheckPeakList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("CY5")){
                        if(isCY5Check){
                            mCheckPeakList.add(curveList);
                        }
                    }
                }
            }

        }
        clearChart();
        XAxis xAxis = resultChart.getXAxis();
        YAxis yAxis = resultChart.getAxisLeft();
        if(isScaleXPeak){
            xAxis.setAxisMaximum(max_Peak_X);
            xAxis.setAxisMinimum(min_Peak_X);
            yAxis.setAxisMaximum(max_Peak_Y);
            yAxis.setAxisMinimum(min_Peak_Y);
        }else{
            yAxis.resetAxisMinimum();
            yAxis.resetAxisMaximum();
            xAxis.resetAxisMaximum();
            xAxis.resetAxisMinimum();
        }
        drawchart(mCheckPeakList,resultChart);
    }



    /**
     * 获取点击孔位后的原始数据
     *
     */
    private void getClickRawCurveResult(){
        mRawCurveList.clear();
        List<BiowayCurveList> rawList = BiowayManager.getInstance().getRawCurve();
        for(BiowayCurveList curveList : rawList){
            if(isOneCheck && curveList.getWellNum().equals("A1")){
                mRawCurveList.add(curveList);
            }else if (isTwoCheck && curveList.getWellNum().equals("A2")) {
                mRawCurveList.add(curveList);
            } else if (isThreeCheck && curveList.getWellNum().equals("A3")) {
                mRawCurveList.add(curveList);
            } else if (isFourCheck && curveList.getWellNum().equals("A4")) {
                mRawCurveList.add(curveList);
            } else if (isFiveCheck && curveList.getWellNum().equals("A5")) {
                mRawCurveList.add(curveList);
            } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                mRawCurveList.add(curveList);
            } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                mRawCurveList.add(curveList);
            } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                mRawCurveList.add(curveList);
            }
        }
    }

    private void getClickAMPCurveResult(){
        mAMPCurveList.clear();
        List<BiowayCurveList> AMPList = BiowayManager.getInstance().getAMPCurve();
        for(BiowayCurveList curveList : AMPList){
            if(isOneCheck && curveList.getWellNum().equals("A1")){
                mAMPCurveList.add(curveList);
            }else if (isTwoCheck && curveList.getWellNum().equals("A2")) {
                mAMPCurveList.add(curveList);

            } else if (isThreeCheck && curveList.getWellNum().equals("A3")) {

                mAMPCurveList.add(curveList);

            } else if (isFourCheck && curveList.getWellNum().equals("A4")) {
                mAMPCurveList.add(curveList);

            } else if (isFiveCheck && curveList.getWellNum().equals("A5")) {
                mAMPCurveList.add(curveList);

            } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                mAMPCurveList.add(curveList);

            } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                mAMPCurveList.add(curveList);

            } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                mAMPCurveList.add(curveList);
            }
        }
    }

    private void getClickMeltCurveResult(){
        mMeltCurveList.clear();
        List<BiowayCurveList> meltList = BiowayManager.getInstance().getMeltCurve();
        if(meltList.size()>0){
            for(BiowayCurveList curveList : meltList){
                if(isOneCheck && curveList.getWellNum().equals("A1")){
                    mMeltCurveList.add(curveList);
                }else if (isTwoCheck && curveList.getWellNum().equals("A2")) {
                    mMeltCurveList.add(curveList);

                } else if (isThreeCheck && curveList.getWellNum().equals("A3")) {

                    mMeltCurveList.add(curveList);

                } else if (isFourCheck && curveList.getWellNum().equals("A4")) {
                    mMeltCurveList.add(curveList);

                } else if (isFiveCheck && curveList.getWellNum().equals("A5")) {
                    mMeltCurveList.add(curveList);

                } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                    mMeltCurveList.add(curveList);

                } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                    mMeltCurveList.add(curveList);

                } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                    mMeltCurveList.add(curveList);
                }
            }
        }

        Log.i("BBA", "getClickMeltCurveResult" +mMeltCurveList.size() );
    }

    private void getClickPeakCurveResult(){
        mPeakCurveList.clear();
        List<BiowayCurveList> peakLisk = BiowayManager.getInstance().getPeakCurve();
        if(peakLisk.size()>0){
            for(BiowayCurveList curveList : peakLisk){

                if(isOneCheck && curveList.getWellNum().equals("A1")){
                    mPeakCurveList.add(curveList);
                }else if (isTwoCheck && curveList.getWellNum().equals("A2")) {
                    mPeakCurveList.add(curveList);

                } else if (isThreeCheck && curveList.getWellNum().equals("A3")) {

                    mPeakCurveList.add(curveList);

                } else if (isFourCheck && curveList.getWellNum().equals("A4")) {
                    mPeakCurveList.add(curveList);

                } else if (isFiveCheck && curveList.getWellNum().equals("A5")) {
                    mPeakCurveList.add(curveList);

                } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                    mPeakCurveList.add(curveList);

                } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                    mPeakCurveList.add(curveList);

                } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                    mPeakCurveList.add(curveList);
                }
            }
        }

        Log.i("BBA", "getClickPeakCurveResult" +mPeakCurveList.size() );
    }

    private void getShowExlResults(){
        mExlResults.clear();
        List<BiowayExlResult> mAllExResult = BiowayManager.getInstance().getExlResults();
        for(BiowayExlResult exlResult : mAllExResult) {

            if (isOneCheck && exlResult.getWellNum().equals("A1")) {
                mExlResults.add(exlResult);
            } else if (isTwoCheck && exlResult.getWellNum().equals("A2")) {
                mExlResults.add(exlResult);

            } else if (isThreeCheck && exlResult.getWellNum().equals("A3")) {

                mExlResults.add(exlResult);

            } else if (isFourCheck && exlResult.getWellNum().equals("A4")) {
                mExlResults.add(exlResult);

            } else if (isFiveCheck && exlResult.getWellNum().equals("A5")) {
                mExlResults.add(exlResult);

            } else if (isSixCheck && exlResult.getWellNum().equals("A6")) {
                mExlResults.add(exlResult);

            } else if (isSevenCheck && exlResult.getWellNum().equals("A7")) {
                mExlResults.add(exlResult);

            } else if (isEightCheck && exlResult.getWellNum().equals("A8")) {
                mExlResults.add(exlResult);
            }
        }

        //判断channel和wellNum
        if(null != mExlResults){
            mCheckExlResults.clear();
            if(mExlResults.size()>0){
                for(BiowayExlResult exlResult : mExlResults){
                    if(exlResult.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
                            mCheckExlResults.add(exlResult);
                        }
                    }else if(exlResult.getCurve_hole().equals("HEX")){
                        if(isHEXCheck){
                            mCheckExlResults.add(exlResult);
                        }
                    }else if(exlResult.getCurve_hole().equals("ROX")){
                        if(isROXCheck){
                            mCheckExlResults.add(exlResult);
                        }
                    }else if(exlResult.getCurve_hole().equals("CY5")){
                        if(isCY5Check){
                            mCheckExlResults.add(exlResult);
                        }
                    }
                }
            }
        }
    }



    /**
     * 点击孔位更新
     */
    private void updateWellResult(){

        //判断哪个没有选中
        getClickAMPCurveResult();
        getClickRawCurveResult();
        getClickMeltCurveResult();
        getClickPeakCurveResult();
        switch (currentView){
            case 1:
                drawRawCurve();
                break;
            case 2:
                drawAMPCurve();
                break;
            case 3:
                drawMeltCurve();
                break;
            case 4:
                drawPeakCurve();
                break;
            case 5:
                getShowExlResults();
                MessageEvent messageEvent = new MessageEvent();
                messageEvent.setMsgType(MsgType.MSG_SHOW_EXCEL_RESULT);
                EventBus.getDefault().post(messageEvent);
                break;
        }
    }



    /**
     *
     * @param check
     * @param textView
     */
    private void setHodeState(boolean check, TextView textView) {
        if (!check) {
            textView.setBackgroundResource(R.drawable.shape_oval_gray_bg);
            textView.setTextColor(getActivity().getResources().getColor(R.color.blue));
        }else{
            textView.setBackgroundResource(R.drawable.shape_oval_blue_bg);
            textView.setTextColor(getActivity().getResources().getColor(R.color.white));
        }
    }

    private void checkAllHole(){
        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck) {
            if(!isAllCheck){
                isAllCheck = true;
                setHodeState(isAllCheck, mAllHoleCk);
            }
        }
    }


    private void setAllHoleCheck(){
        if(isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck){
            isAllCheck = true;
            setHodeState(isAllCheck, mAllHoleCk);
        } else {
            if (isOneEnable) {
                isOneCheck = true;
            }
            if (isTwoEnable) {
                isTwoCheck = true;
            }
            if (isThreeEnable) {
                isThreeCheck = true;
            }
            if (isFourEnable) {
                isFourCheck = true;
            }
            if (isFiveEnable) {
                isFiveCheck = true;
            }
            if (isSixEnable) {
                isSixCheck = true;
            }
            if (isSevenEnable) {
                isSevenCheck = true;
            }
            if (isEightEnable) {
                isEightCheck = true;
            }
            isAllCheck = true;
            setHodeState(isOneCheck,mHoleOneTv);
            setHodeState(isTwoCheck, mHoleTwoTv);
            setHodeState(isThreeCheck, mHoleThreeTv);
            setHodeState(isFourCheck, mHoleFourTv);
            setHodeState(isFiveCheck, mHoleFiveTv);
            setHodeState(isSixCheck, mHoleSixTv);
            setHodeState(isSevenCheck, mHoleSevenTv);
            setHodeState(isEightCheck, mHoleEightTv);
        }

    }


    private void setAllHoleUnCheck() {
        isOneCheck = false;
        isTwoCheck = false;
        isThreeCheck = false;
        isFourCheck = false;
        isFiveCheck = false;
        isSixCheck = false;
        isSevenCheck = false;
        isEightCheck = false;
        setHodeState(isOneCheck, mHoleOneTv);
        setHodeState(isTwoCheck, mHoleTwoTv);
        setHodeState(isThreeCheck, mHoleThreeTv);
        setHodeState(isFourCheck, mHoleFourTv);
        setHodeState(isFiveCheck, mHoleFiveTv);
        setHodeState(isSixCheck, mHoleSixTv);
        setHodeState(isSevenCheck, mHoleSevenTv);
        setHodeState(isEightCheck, mHoleEightTv);
    }

/////////////////////////////////////////////////////////二次分析////////////////////////////////////////////////////////////


    /*二次分析
     * 对话框输入数值点击确认二次分析，调用SLANManager方法
     */
    private void secondAnalayse(){
        if(null != configTargets && configTargets.size()>0){
            showLoadingDialog(getActivity().getResources()
                    .getString(R.string.running_result_dialog_second_analyse));
            //将模板下的判断文件传入
            String fileDirPath = null;
            if(PcrApplication.experimentType == 0){
                //全流程
                fileDirPath = FileUtils.FULL_PROCESS_TEMPLATE_PATH  + File.separator + PcrApplication.rootTempName
                        + File.separator;
            }else if(PcrApplication.experimentType == 1){
                fileDirPath = FileUtils.SINGLE_PCR_TEMPLATE_PATH  + File.separator + PcrApplication.rootTempName
                        + File.separator;
            }
           if(!StringUtil.isEmpty(fileDirPath)){
               File allMoveDir = new File(fileDirPath);
               List<File> localList = new ArrayList<File>();
               if(allMoveDir.isDirectory() && allMoveDir.exists()){
                   Collections.addAll(localList, allMoveDir.listFiles());
               }
               File ctFile = FileUtils.getFile(localList,".ct");
               File peakFile = FileUtils.getFile(localList, ".peak");
               new Thread(new Runnable() {
                   @Override
                   public void run() {
                       BiowayManager.getInstance().secondSLANRunExAnalyse(configTargets, optimizations,AMPAlgorithm,
                               ctFile,peakFile,PcrApplication.ex_startTime);
                   }
               }).start();
           }else{
               new Thread(new Runnable() {
                   @Override
                   public void run() {
                       BiowayManager.getInstance().secondSLANRunExAnalyse(configTargets, optimizations,AMPAlgorithm,
                               null,null,PcrApplication.ex_startTime);
                   }
               }).start();
           }
        }

    }



    /***********************************************************************/
    /**
     * 二次分析获取实验参数
     */
    private void getExperimentParameter(){
        isCy5Threshold = isFamThreshold = isHexThreshold = isRoxThreshold = false;
        configTargets.clear();
        Log.i("BIOWAY","result---getExperimentParamter" + BiowayManager.getInstance().getExperiment());
        optimizations.clear();
        if(null != BiowayManager.getInstance().getExperiment()){
            Experiment experiment = BiowayManager.getInstance().getExperiment();
            ArrayList<AnalyseParamter> analyseParamters = BoWeiService.getInstance()
                    .getExperimentParamter(experiment);
            if(analyseParamters != null && analyseParamters.size()>0){
                    AMPAlgorithm = analyseParamters.get(0).getAMPAlgorithm();
                ArrayList<Target> targets = analyseParamters.get(0).getTargets();
                if(null != targets && targets.size()>0){
                        //目标名称
                    for(Target target : targets){
                        if(target.getChannelNo()==1){
                            isFamThreshold = true;
                            TargetParamter famParamer = target.getParamter();
                            famThreshold = famParamer.getThreshold();
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
                            roxThreshold = roxParamer.getThreshold();
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
                            hexThreshold = hexParamer.getThreshold();
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
                        }else if(target.getChannelNo()== 4){
                            isCy5Threshold = true;
                            TargetParamter cy5Paramer = target.getParamter();
                            cy5Threshold = cy5Paramer.getThreshold();
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
                                cy5Target.setMinTemperature(meltTargetParamer.getMinTemperature());
                                cy5Target.setMaxTemperature(meltTargetParamer.getMaxTemperature());
                                cy5Target.setMeltPeakAlgorithm(meltTargetParamer.getMeltPeakAlgorithm());
                                cy5Target.setPeekHighThreshold(meltTargetParamer.getPeekHightThreshold());
                                cy5Target.setPeekType(meltTargetParamer.getPeekType());
                            }else{
                                isMelt = false;
                            }
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

        if (AMPAlgorithm == 0) {
            mAMPAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_default));
        } else if (AMPAlgorithm == 1) {
            mAMPAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_subtraction));
        } else if (AMPAlgorithm == 2) {
            mAMPAlgorithmTv.setText(getActivity().getResources().getString(R.string.config_info_ampalgorithm_relativeLinear));
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
    /**
     * 扩增曲线参数修改
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
                        int endbase = (int) StringUtil.convertToFloat(s,1.0f);
                        if(endbase < 1){
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_baseline),Toast.LENGTH_LONG).show();
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
                            Toast.makeText(getActivity().getApplicationContext(),
                                    getActivity().getResources().getString(R.string.config_info_toast_baseline),Toast.LENGTH_LONG).show();
                            return;
                        }else{
                            ConfigTarget configTarget = configTargets.get(ampAlogrithm_pos);
                            configTarget.setBeginBaseline(beginbase);
                            mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                        }
                    }else if(dialogType == 4){
                        //手动阈值
                        double threshold = (double) StringUtil.convertToFloat(s, 0.00f);
                        if (threshold < 0.01f) {
                            threshold = 0.01f;
                        }
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
                        //f峰高阈值
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

    /**
     * 定性/定量
     */
    private void collapseQualitativeAnalysis(){
        if (mAysTypePopWindow != null && mAysTypePopWindow.isShowing()) {
            mAysTypePopWindow.dismiss();
        }
    }

    //展开扩增曲线算法
    private void expandQualitativeAnalysis(View view){
        if(mAmpAysTypes== null && mAmpAysTypes.isEmpty()){
            return;
        }
        mAysTypePopWindow.showAsDropDown(view);
    }

    /**
     * 定性定量
     */
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

    //扩增曲线算法
    private void collapseOptimizationMode(){
        if (mOptimizationModeWindow != null && mOptimizationModeWindow.isShowing()) {
            mOptimizationModeWindow.dismiss();
        }
    }

    //展开扩增曲线算法
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
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter digitalFilterAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mDighitalFilters);
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
    private void collapsePeekType() {
        if (mPeekTypeWindow != null && mPeekTypeWindow.isShowing()) {
            mPeekTypeWindow.dismiss();
        }
        // isPeekTypeExpanded = false;
    }

    private void expandPeekType(View view) {
        if (mPeekTypes == null && mPeekTypes.isEmpty()) {
            return;
        }
        mPeekTypeWindow.showAsDropDown(view);
        //  isPeekTypeExpanded = true;
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

    //熔解曲线参数-熔解算法-高级算法，标准算法
    private void expandMeltPeakAlg(View view) {
        if (mMeltPeakAlgorithms == null && mMeltPeakAlgorithms.isEmpty()) {
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
        mMeltPeakAlgorithms.add(getActivity().getResources().getString(R.string.config_info_meltpeakalgorithm_standard));
        mMeltPeakAlgorithms.add(getActivity().getResources().getString(R.string.config_info_meltpeakalgorithm_advance));
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter meltAlgorithmAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mMeltPeakAlgorithms);
        ListView mMeltPeakAlgorithmLv= (ListView) view.findViewById(R.id.spinner_listview);
        mMeltPeakAlgorithmLv.setAdapter(meltAlgorithmAdapter);
        meltAlgPopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        meltAlgPopWindow.setOutsideTouchable(true);
        meltAlgPopWindow.setFocusable(true);
        meltAlgPopWindow.setContentView(view);
        meltAlgPopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
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

    //////////////////////////扩增曲线算法枚举////////////////////////////

    /**
     * 基本参数-扩增曲线算法
     * 相对荧光值法-log
     * 绝对荧光值法
     * 相对荧光值法-线性
     */
    private void initAMPAlgorithm() {
        String[] ampAlgorithmArray = getResources().getStringArray(R.array.AMPAlgorithm);
        mAMPAlgorithmList = new ArrayList<String>(ampAlgorithmArray.length);
        Collections.addAll(mAMPAlgorithmList, ampAlgorithmArray);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner, null);
        mAMPAlgSpinerAdapter = new CustomSpinnerAdapter(getActivity().getApplicationContext(), mAMPAlgorithmList);
        mAMPAlgorithmLv = (ListView) view.findViewById(R.id.spinner_listview);
        mAMPAlgorithmLv.setAdapter(mAMPAlgSpinerAdapter);
        mAMPAlgWindow = new PopupWindow(view,
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
                mAMPAlgorithmTv.setText(mAMPAlgorithmList.get(position));
                AMPAlgorithm = position;
                collapseAmpAlg();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private void collapseAmpAlg() {
        if (mAMPAlgWindow != null && mAMPAlgWindow.isShowing()) {
            mAMPAlgWindow.dismiss();
        }
    }

    private void expandAmpAlg(View view) {
        if (mAMPAlgorithmList == null && mAMPAlgorithmList.isEmpty()) {
            return;
        }
        mAMPAlgWindow.showAsDropDown(view);
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
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter enableAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mEnalbeOptimations);
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

    //////////////////////////////////////////////more更多操作///////////////////////////////////////////////////////////////////////

    private void collapseMoreActions(){
        if (mMoreActions != null && mMoreActionWindows.isShowing()) {
            mMoreActionWindows.dismiss();
        }
    }

    //展开扩增曲线算法
    private void expandMoreActions(View view){
        if(mMoreActions== null && mMoreActions.isEmpty()){
            return;
        }
        mMoreActionWindows.showAsDropDown(view, view.getWidth(),-view.getHeight());
    }

    private void initMoreActions(){
        mMoreActions = new ArrayList<String>();
        mMoreActions.add(getActivity().getResources().getString(R.string.running_result_coordinate_scaling));
        mMoreActions.add(getActivity().getResources().getString(R.string.running_result_coordinate_restoration));
        View view= LayoutInflater.from(getActivity()).inflate(R.layout.layout_custom_spinner,null);
        CustomSpinnerAdapter mAmpAysTypeAdapter=new CustomSpinnerAdapter(getActivity().getApplicationContext(),mMoreActions);
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
                    Toast.makeText(getActivity().getApplicationContext(),
                           getActivity().getResources().getString(R.string.running_result_toast_cannot_scale),Toast.LENGTH_LONG).show();
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
    private void resetCoodidate() {
        if (currentView == 1 || currentView == 2 || currentView == 3 || currentView == 4) {
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
            coordinateDialog = new CustomLargeDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_custom_coodidate_scale_layout, null);
            CustomAddSubEdit minX_Et = (CustomAddSubEdit) view.findViewById(R.id.dialog_coodidate_min_x_et);
            CustomAddSubEdit maxX_Et = (CustomAddSubEdit) view.findViewById(R.id.dialog_coodidate_max_x_et);
            CustomAddSubEdit minY_Et = (CustomAddSubEdit)  view.findViewById(R.id.dialog_coodidate_y_min_et);
            CustomAddSubEdit maxY_Et = (CustomAddSubEdit)  view.findViewById(R.id.dialog_coodidate_Y_max_et);
            minX_Et.setEditTextValue(String.valueOf(min_x));
            maxX_Et.setEditTextValue(String.valueOf(max_x));
            minY_Et.setEditTextValue(min_y+"");
            maxY_Et.setEditTextValue(max_y+"");
            CustomLargeDialog.Builder builder = new CustomLargeDialog.Builder(getActivity());
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
                                drawchart(mCheckRawList, resultChart);
                                break;
                            case 2:
                                isScaleXAmp = true;
                                min_Amp_X = X_min;
                                max_Amp_X = X_max;
                                min_Amp_Y = Y_min;
                                max_Amp_Y = Y_max;
                                clearChart();
                                drawchart(mCheckAMPList, resultChart);
                                break;
                            case 3:
                                isScaleXMelt = true;
                                min_Melt_X = X_min;
                                max_Melt_X = X_max;
                                min_Melt_Y = Y_min;
                                max_Melt_Y = Y_max;
                                clearChart();
                                drawchart(mCheckMeltList, resultChart);
                                break;
                            case 4:
                                isScaleXPeak = true;
                                min_Peak_X = X_min;
                                max_Peak_X = X_max;
                                min_Peak_Y = Y_min;
                                max_Peak_Y = Y_max;
                                clearChart();
                                drawchart(mCheckPeakList, resultChart);
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


    //////////////////////////////////////////显示报告结果/////////////////////////////////////////////////////
    private List<BiowayExlResult> mReportResults = new ArrayList<BiowayExlResult>();

    private void showReportResult(int hole_num) {
        Log.e("Report", "showReportResult---" + hole_num);
        switch (hole_num) {
            case 1:
                List<BiowayExlResult> mCkReportResults_1 = new ArrayList<BiowayExlResult>();
                mCkReportResults_1.clear();
                if (null != mReportResults && mReportResults.size() > 0) {
                    for (BiowayExlResult exlResult : mReportResults) {
                        if (exlResult.getWellNum().equals("A1")) {
                            mCkReportResults_1.add(exlResult);
                        }
                    }
                }

                List<ReportResult> reportResults_1 = getPrintResults(mCkReportResults_1);
                userReportAdapter.setResultList(reportResults_1);
                mStartTimeOneTv.setText(PcrApplication.ex_startTime);
                mItemOneTv.setText(PcrApplication.rootTempName);
                mIDcardOneTv.setText(mCkReportResults_1.get(0).getIdCard());
                Bitmap idcardCodeBitmap = CommonUtil.generateBarcodeBitmap(mCkReportResults_1.get(0).getIdCard());
                mBarCodeOneImg.setImageBitmap(idcardCodeBitmap);
                Log.e("Report", "mCkReportResults111---" + mCkReportResults_1.size());
                break;

            case 2:
                List<BiowayExlResult> mCkReportResults_2 = new ArrayList<BiowayExlResult>();
                mCkReportResults_2.clear();
                if (null != mReportResults && mReportResults.size() > 0) {
                    for (BiowayExlResult exlResult : mReportResults) {
                        if (exlResult.getWellNum().equals("A2")) {
                            mCkReportResults_2.add(exlResult);
                        }
                    }
                }
                List<ReportResult> reportResults_2 = getPrintResults(mCkReportResults_2);
                userReportAdapter_2.setResultList(reportResults_2);
                mStartTimeTwoTv.setText(PcrApplication.ex_startTime);
                mItemTwoTv.setText(PcrApplication.rootTempName);
                mIDcardTwoTv.setText(mCkReportResults_2.get(0).getIdCard());
                Bitmap idcardCodeBitmap2 = CommonUtil.generateBarcodeBitmap(mCkReportResults_2.get(0).getIdCard());
                mBarCodeTwoImg.setImageBitmap(idcardCodeBitmap2);
                Log.i("Report", "reportResults_2222---" + reportResults_2.size());
                break;

            case 3:
                List<BiowayExlResult> mCkReportResults_3= new ArrayList<BiowayExlResult>();
                mCkReportResults_3.clear();
                if(null != mReportResults && mReportResults.size()>0){
                    for(BiowayExlResult exlResult : mReportResults) {
                        if(exlResult.getWellNum().equals("A3")){
                            mCkReportResults_3.add(exlResult);
                        }
                    }
                }
                List<ReportResult> reportResults_3 = getPrintResults(mCkReportResults_3);
                userReportAdapter_3.setResultList(reportResults_3);
                mStartTimeThreeTv.setText(PcrApplication.ex_startTime);
                mItemThreeTv.setText(PcrApplication.rootTempName);
                mIDcardThreeTv.setText(mCkReportResults_3.get(0).getIdCard());
                Bitmap idcardCodeBitmap3 = CommonUtil.generateBarcodeBitmap(mCkReportResults_3.get(0).getIdCard());
                mBarCodeThreeImg.setImageBitmap(idcardCodeBitmap3);
                Log.i("Report", "reportResults_3333--" + reportResults_3.size());
                break;

            case 4:
                List<BiowayExlResult> mCkReportResults_4= new ArrayList<BiowayExlResult>();
                mCkReportResults_4.clear();
                if(null != mReportResults && mReportResults.size()>0){
                    for(BiowayExlResult exlResult : mReportResults) {
                        if(exlResult.getWellNum().equals("A4")){
                            mCkReportResults_4.add(exlResult);

                        }
                    }
                }
                List<ReportResult> reportResults_4 = getPrintResults(mCkReportResults_4);
                userReportAdapter_4.setResultList(reportResults_4);
                mStartTimeFourTv.setText(PcrApplication.ex_startTime);
                mItemFourTv.setText(PcrApplication.rootTempName);
                mIDcardFourTv.setText(mCkReportResults_4.get(0).getIdCard());
                Bitmap idcardCodeBitmap4 = CommonUtil.generateBarcodeBitmap(mCkReportResults_4.get(0).getIdCard());
                mBarCodeFourImg.setImageBitmap(idcardCodeBitmap4);
                Log.i("Report", "reportResults_4444--" + reportResults_4.size());
                break;

            case 5:
                List<BiowayExlResult> mCkReportResults_5= new ArrayList<BiowayExlResult>();
                mCkReportResults_5.clear();
                if(null != mReportResults && mReportResults.size()>0){
                    for(BiowayExlResult exlResult : mReportResults) {
                        if(exlResult.getWellNum().equals("A5")){
                            mCkReportResults_5.add(exlResult);
                        }
                    }
                }
                List<ReportResult> reportResults_5 = getPrintResults(mCkReportResults_5);
                userReportAdapter_5.setResultList(reportResults_5);
                mStartTimeFiveTv.setText(PcrApplication.ex_startTime);
                mItemFiveTv.setText(PcrApplication.rootTempName);
                mIDcardFiveTv.setText(mCkReportResults_5.get(0).getIdCard());
                Bitmap idcardCodeBitmap5 = CommonUtil.generateBarcodeBitmap(mCkReportResults_5.get(0).getIdCard());
                mBarCodeFiveImg.setImageBitmap(idcardCodeBitmap5);
                Log.i("Report", "reportResults_5555--" + reportResults_5.size());
                break;

            case 6:
                List<BiowayExlResult> mCkReportResults_6= new ArrayList<BiowayExlResult>();
                mCkReportResults_6.clear();
                if(null != mReportResults && mReportResults.size()>0){
                    for(BiowayExlResult exlResult : mReportResults) {
                        if(exlResult.getWellNum().equals("A6")){
                            mCkReportResults_6.add(exlResult);

                        }
                    }
                }
                List<ReportResult> reportResults_6 = getPrintResults(mCkReportResults_6);
                userReportAdapter_6.setResultList(reportResults_6);
                mStartTimeSixTv.setText(PcrApplication.ex_startTime);
                mItemSixTv.setText(PcrApplication.rootTempName);
                mIDcardSixTv.setText(mCkReportResults_6.get(0).getIdCard());
                Bitmap idcardCodeBitmap6 = CommonUtil.generateBarcodeBitmap(mCkReportResults_6.get(0).getIdCard());
                mBarCodeSixImg.setImageBitmap(idcardCodeBitmap6);
                Log.i("Report", "reportResults_6666--" + reportResults_6.size());
                break;

            case 7:
                List<BiowayExlResult> mCkReportResults_7= new ArrayList<BiowayExlResult>();
                mCkReportResults_7.clear();
                if(null != mReportResults && mReportResults.size()>0){
                    for(BiowayExlResult exlResult : mReportResults) {
                        if(exlResult.getWellNum().equals("A7")){
                            mCkReportResults_7.add(exlResult);
                        }
                    }
                }
                List<ReportResult> reportResults_7 = getPrintResults(mCkReportResults_7);
                userReportAdapter_7.setResultList(reportResults_7);
                mStartTimeSevenTv.setText(PcrApplication.ex_startTime);
                mItemSevenTv.setText(PcrApplication.rootTempName);
                mIDcardSevenTv.setText(mCkReportResults_7.get(0).getIdCard());
                Bitmap idcardCodeBitmap7 = CommonUtil.generateBarcodeBitmap(mCkReportResults_7.get(0).getIdCard());
                mBarCodeSevenImg.setImageBitmap(idcardCodeBitmap7);
                Log.i("Report", "reportResults_7777--" + reportResults_7.size());
                break;

            case 8:
                List<BiowayExlResult> mCkReportResults_8= new ArrayList<BiowayExlResult>();
                mCkReportResults_8.clear();
                if(null != mReportResults && mReportResults.size()>0){
                    for(BiowayExlResult exlResult : mReportResults) {
                        if(exlResult.getWellNum().equals("A8")){
                            mCkReportResults_8.add(exlResult);
                        }
                    }
                }
                List<ReportResult> reportResults_8 = getPrintResults(mCkReportResults_8);
                userReportAdapter_8.setResultList(reportResults_8);
                mStartTimeEightTv.setText(PcrApplication.ex_startTime);
                mItemEightTv.setText(PcrApplication.rootTempName);
                mIDcardEightTv.setText(mCkReportResults_8.get(0).getIdCard());
                Bitmap idcardCodeBitmap8 = CommonUtil.generateBarcodeBitmap(mCkReportResults_8.get(0).getIdCard());
                mBarCodeEightImg.setImageBitmap(idcardCodeBitmap8);
                Log.i("Report", "reportResults_8888--" + reportResults_8.size());
                break;
        }
    }


    private List<ReportResult> getPrintResults(List<BiowayExlResult> mCheckExlResults) {
        List<ReportResult> printResults = new ArrayList<ReportResult>();
        if (null != mCheckExlResults && mCheckExlResults.size() > 0) {
            printResults.clear();
            Log.e("Report", "试剂序号---" + mCheckExlResults.get(0).getIdCard());
            for (int i = 0; i < mCheckExlResults.size(); i++) {
                BiowayExlResult slanExlResult = mCheckExlResults.get(i);
                StringBuilder rmValueStr = new StringBuilder();
                StringBuilder peakValueStr = new StringBuilder();
                List<Peak> peakValues = slanExlResult.getPeakValues();

                if (null != peakValues && peakValues.size() > 0) {
                    if (peakValues.size() == 1) {
                        peakValueStr.append(nf.format(peakValues.get(0).getTm()));
                        rmValueStr.append(nf.format(peakValues.get(0).getRm()));
                    } else if (peakValues.size() > 1) {
                        for (int j = 0; j < peakValues.size(); j++) {
                            if (j == peakValues.size() - 1) {
                                peakValueStr.append(nf.format(peakValues.get(j).getTm()));
                                rmValueStr.append(nf.format(peakValues.get(j).getRm()));
                            } else {
                                peakValueStr.append(nf.format(peakValues.get(j).getTm())).append("\n");
                                rmValueStr.append(nf.format(peakValues.get(j).getRm())).append("\n");
                            }
                        }
                    }
                }

                List<CtResult> results = slanExlResult.getResults();//扩增的结论
                List<PeakResult> peakresults = slanExlResult.getPeakResults();
                if (null != results && results.size() > 0) {
                    for (CtResult result : results) {
                        ReportResult reportResult = new ReportResult();
                        reportResult.setCategory(result.getCategory());
                        reportResult.setConclusion(result.getConclusion());
                        reportResult.setCtValue(slanExlResult.getCt() + "");
                        reportResult.setReference("阴");
                        printResults.add(reportResult);
                    }
                }

                if (null != peakresults && peakresults.size() > 0) {
                    for (PeakResult peakResult : peakresults) {
                        ReportResult reportResult = new ReportResult();
                        reportResult.setCategory(peakResult.getCategory());
                        reportResult.setConclusion(peakResult.getConclusion());
                        reportResult.setRmValue(rmValueStr + "");
                        reportResult.setTmValue(peakValues + "");
                        reportResult.setReference(peakResult.getValue() + "");//controlPeak
                        printResults.add(reportResult);
                    }
                }
            }
        }
        return printResults;
    }


    /*
     * 打印当前报告
     * 先把当前报告保存成pdf后再打印
     */
    private void printCurPdfReport(String wellNum,LinearLayout pdfLayout){
        if(!StringUtil.isEmpty(PcrApplication.ex_startTime) && !StringUtil.isEmpty(PcrApplication.rootTempName)) {
            String aftertime = DateFormatUtil.convertDateFormat(PcrApplication.ex_startTime);
            String subFirName = aftertime + "_" + PcrApplication.rootTempName;

            File dir = new File(FileUtils.PRINT_PDF_PATH, subFirName);
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs();
            }
            String file_path = FileUtils.PRINT_PDF_PATH + subFirName + File.separator + wellNum + ".pdf";
            showLoadingDialog(getResources().getString(R.string.running_dialog_message_save_and_print_pdf));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    LinearLayout contentView = pdfLayout;
                    File pdfFile = new File(file_path);
                    if(pdfFile.exists()){
                        pdfFile.delete();
                    }
                    boolean save_result = CommonUtil.createPdf(getActivity().getApplicationContext(),
                            contentView,file_path,1);
                    //保存成功可以打印
                    if(save_result){
                        //进行打印
                    }
                    MessageEvent pdfMsg = new MessageEvent();
                    pdfMsg.setMsgType(MsgType.MSG_SAVE_PDF);
                    pdfMsg.setData(save_result);
                    EventBus.getDefault().postSticky(pdfMsg);

                }
            }).start();
        }else{
            Toast.makeText(getActivity().getApplicationContext(),getActivity().getResources().getString(R.string.running_toast_cannot_save_and_print_pdf)
                    ,Toast.LENGTH_LONG).show();
        }
    }


    private void printPdfReport(String wellNum){

        if(!StringUtil.isEmpty(PcrApplication.ex_startTime) && !StringUtil.isEmpty(PcrApplication.rootTempName)) {
            String aftertime = DateFormatUtil.convertDateFormat(PcrApplication.ex_startTime);
            String subFirName = aftertime + "_" + PcrApplication.rootTempName;

            File dir = new File(FileUtils.PRINT_PDF_PATH, subFirName);
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs();
            }

            String file_path = FileUtils.PRINT_PDF_PATH + subFirName + File.separator + wellNum + ".pdf";;
            File pdfFile = new File(file_path);
            if(pdfFile.exists()){
                pdfFile.delete();
            }
            ScrollView contentView = (ScrollView)getActivity().findViewById(R.id.user_result_all_report_scrollview);
            boolean save_result = CommonUtil.createPdf(getActivity().getApplicationContext(),
                    contentView,file_path,2);
            //保存成功可以打印
            MessageEvent pdfMsg = new MessageEvent();
            pdfMsg.setMsgType(MsgType.MSG_PRINT_ALL_PDF);
            pdfMsg.setData(save_result);
            EventBus.getDefault().postSticky(pdfMsg);

        }else{
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.running_toast_cannot_save_and_print_pdf)
                    ,Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 统一保存所有的可打印的pdf在目录下打印
     */
    int save_pdf_result = 0;
    private Thread printThread;


    private void printAllPdfReport() {
        showLoadingDialog(getActivity().getResources().getString(R.string.running_dialog_message_save_all_report_pdf));
        if (isOneEnable) {
            mReportOneLayout.setVisibility(View.VISIBLE);
            showReportResult(1);
        }
        if (isTwoEnable) {
            mReportTwoLayout.setVisibility(View.VISIBLE);
            showReportResult(2);
        }
        if (isThreeEnable) {
            mReportThreeLayout.setVisibility(View.VISIBLE);
            showReportResult(3);
        }
        if (isFourEnable) {
            mReportFourLayout.setVisibility(View.VISIBLE);
            showReportResult(4);
        }
        if (isFiveEnable) {
            mReportFiveLayout.setVisibility(View.VISIBLE);
            showReportResult(5);
        }

        if (isSixEnable) {
            mReportSixLayout.setVisibility(View.VISIBLE);
            showReportResult(6);
        }

        if (isSevenEnable) {
            mReportSevenLayout.setVisibility(View.VISIBLE);
            showReportResult(7);
        }

        if (isEightEnable) {
            mReportEightLayout.setVisibility(View.VISIBLE);
            showReportResult(8);
        }

        if(printThread != null && printThread.isAlive()){
            printThread.interrupt();
            printThread = null;
        }
        printThread = new Thread(new Runnable() {
            @Override
            public void run() {
                printPdfReport("All_Report");
            }
        });
        printThread.start();
    }

}
