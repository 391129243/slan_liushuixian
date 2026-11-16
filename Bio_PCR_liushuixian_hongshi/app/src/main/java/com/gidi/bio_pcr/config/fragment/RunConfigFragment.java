package com.gidi.bio_pcr.config.fragment;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.gidi.bio_pcr.bioway.adapter.RunBiowayResultAdapter;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.config.adapter.RunSLANStepAdapter;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;

import com.gidi.bio_pcr.mgr.FtpUploadManager;
import com.gidi.bio_pcr.pcr.OnParseRunningExCallBack;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomCheckText;
import com.gidi.bio_pcr.view.CustomTitleText;
import com.gidi.bio_pcr.view.DividerDecoration;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Step;
import com.hongshi.slansdk.model.experiment.Well;

import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.SampleType;

import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.winters.boweilibrary.BoWei.bean.BiowayExlResult;
import com.winters.boweilibrary.BoWei.bean.RunBiowayResult;
import com.winters.boweilibrary.BoWei.bioweyenum.RunExperimentResult;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningError;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningState;
import com.winters.boweilibrary.BoWei.listener.RunningInfoListener;
import com.winters.boweilibrary.BoWei.model.experiment.ExperimentParam;
import com.winters.boweilibrary.BoWei.model.experiment.RunningInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试创建的SLAN模板文件
 */
public class RunConfigFragment extends BaseFragment implements View.OnClickListener, OnParseRunningExCallBack {
    private CustomTitleText mProjNameTv;
    private CustomTitleText mProjStateTv;//程序状态
    private CustomTitleText mStartTimeTv;
    private CustomTitleText mRunningTimeTv;//运行时间
    private CustomTitleText mRemainTimeTv;//剩余时间
    private CustomTitleText mCurProgramTv;
    private CustomTitleText mCurStepTv;
    private CustomTitleText mCycleTv;//循环
    private CustomTitleText mTempTv;//实验温度
    private CustomTitleText mHoldTimeTv;
    //试管温度
    private CustomTitleText mTubeTempTv,mTubeTemp2Tv,mTubeTemp3Tv;//半导体温度1.半导体温度2.半导体温度3
    private RelativeLayout mRunningChartLayout;
    private LinearLayout mResultExlLayout;//显示表格
    private LinearLayout mResultChartLayout;//显示曲线
    private HorizontalScrollView mRunningStepLayout;
    private TextView mCountDownTv;
    private RecyclerView programRv;
    private LinearLayout mProgramTitleLayout;
    private LinearLayout mProgramRecycleLayout;
    private List<ConfigSLANSection> mSLANSections;
    private RunSLANStepAdapter stepAdapter;
    private DecimalFormat nf = new DecimalFormat( "0.00");

    private String before_program = "程序段-1";
    private String before_step = "步骤-1";
    private int before_loop;
    private  long time = 0;

    private TextView mStepCheckTv,mChartCheckTv;
    private LineChart mFirstLineChart;
    private CustomCheckText mFAM_ChannelTv,mHEX_ChannelTv,mROX_ChannelTv,mCY5_ChannelTv,mQUASAR_ChannelTv,mATTO_ChannelTv;
    private RadioGroup mCurveChartGg;
    private RadioButton mRawCurveRb;
    private boolean isFAMCheck = false;
    private boolean isHEXCheck = false;
    private boolean isROXCheck = false;
    private boolean isCY5Check = false;

    private List<BiowayCurveList> mRawCurveList;
    private List<BiowayCurveList> mCheckRawCurveList;
    private List<BiowayCurveList> mMeltCurveList;
    private List<BiowayCurveList> mCheckMeltCurveList;
    private List<BiowayCurveList> mAMPCurveList;
    private List<BiowayCurveList> mCheckAMPCurveList;

    private List<BiowayCurveList> mPeakCurveList;
    private List<BiowayCurveList> mCheckPeakList ;
    private List<BiowayExlResult> mExlResults;
    private List<BiowayExlResult> mCheckExlResults;
    private ListView mResultListView;
    private RunBiowayResultAdapter resultAdapter;

    private TextView mHoleOneTv,mHoleTwoTv,mHoleThreeTv,mHoleFourTv,mHoleFiveTv,mHoleSixTv,mHoleSevenTv,mHoleEightTv;
    private boolean isOneCheck,isTwoCheck,isThreeCheck,isFourCheck,isFiveCheck,isSixCheck,isSevenCheck,isEightCheck = false;
    private int currentView = 1;
    private boolean isAllCheck = false;
    private TextView mAllHoleCk;
    private CustomCheckText mAllChannelCk;
    private boolean isAllChanelCheck = false;
    private UIHandler mUIHandler;
    /////////////////////////////////////////////////
    private String fileName;
    private TextView mRunStartTv,mRunStopTv;
    private boolean isRunning = false;
    private boolean isShow = false;
    private boolean isChartVisible = true;
    private ImageView mBackImg;

    private boolean isMelt = false;
    private boolean isDone = false;

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_run_config;
    }

    @Override
    protected void initView(View rootView) {

        mBackImg = (ImageView)rootView.findViewById(R.id.config_run_back_img);
        mRunStartTv = (TextView)rootView.findViewById(R.id.config_run_start_tv) ;
        mRunStopTv = (TextView)rootView.findViewById(R.id.config_run_stop_tv) ;

        mProjNameTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_project_tv);
        mProjStateTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_project_state_tv);
        mStartTimeTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_start_time_tv);
        mRunningTimeTv= (CustomTitleText)rootView.findViewById(R.id.run_config_slan_running_time_tv);
        mRemainTimeTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_lasttime_tv);
        mCurProgramTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_current_program_tv) ;
        mCurStepTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_current_step_tv);
        mTempTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_project_temp_tv);
        mTubeTempTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_project_tubetemp_tv);
        mTubeTemp2Tv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_project_tubetemp_2_tv);
        mTubeTemp3Tv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_project_tubetemp_3_tv);
        mCycleTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_project_loop_tv);
        mCountDownTv  = (TextView)rootView.findViewById(R.id.run_config_slan_pcr_countdown_tv);
        mHoldTimeTv = (CustomTitleText)rootView.findViewById(R.id.run_config_slan_project_holdtime_tv);
        mRunningStepLayout = (HorizontalScrollView)rootView.findViewById(R.id.run_config_slan_step_layout);
        mRunningChartLayout = (RelativeLayout)rootView.findViewById(R.id.run_config_slan_running_chart_layout) ;

        mResultChartLayout = (LinearLayout)rootView.findViewById(R.id.run_config_slan_result_curve_chart_layout);
        mResultExlLayout = (LinearLayout)rootView.findViewById(R.id.run_config_slan_result_slan_excel_layout);
        mResultListView = (ListView)rootView.findViewById(R.id.run_config_result_slan_list_view);
        resultAdapter =  new RunBiowayResultAdapter(getActivity().getApplicationContext());


        mProgramTitleLayout = (LinearLayout)rootView.findViewById(R.id.run_config_slan_program_title_layout);
        mProgramRecycleLayout = (LinearLayout)rootView.findViewById(R.id.run_config_slan_program_recycle_layout);
        programRv = (RecyclerView)rootView.findViewById(R.id.run_config_slan_rv);
        mSLANSections = new ArrayList<ConfigSLANSection>();
        programRv.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        programRv.addItemDecoration(new DividerDecoration(getActivity(), 2));
        stepAdapter = new RunSLANStepAdapter(getActivity().getApplicationContext());
        mSLANSections.clear();
        stepAdapter.setProgramSteps(mSLANSections);
        programRv.setAdapter(stepAdapter);


        initChartView(rootView);
        initVariable();

    }


    private void initChartView(View rootView){
        mStepCheckTv = (TextView)rootView.findViewById(R.id.run_config_slan_running_step_tv) ;
        mChartCheckTv  = (TextView)rootView.findViewById(R.id.run_config_slan_running_curve_tv);
        mFirstLineChart = (LineChart)rootView.findViewById(R.id.run_config_slan_running_result_curve_chart);
        if(null != mFirstLineChart){
            mFirstLineChart.setNoDataText("");
        }
        mFAM_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.run_config_slan_running_result_fam_tv);
        mHEX_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.run_config_slan_running_result_hex_tv);
        mROX_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.run_config_slan_running_result_rox_tv);
        mCY5_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.run_config_slan_running_result_cy5_tv);
        mQUASAR_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.run_config_slan_running_result_quasar_tv);
        mATTO_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.run_config_slan_running_result_atto_tv);

        mHoleOneTv = (TextView)rootView.findViewById(R.id.run_config_slan_running_hole_one_tv);
        mHoleTwoTv = (TextView)rootView.findViewById(R.id.run_config_slan_running_hole_two_tv);
        mHoleThreeTv = (TextView)rootView.findViewById(R.id.run_config_slan_running_hole_three_tv);
        mHoleFourTv = (TextView)rootView.findViewById(R.id.run_config_slan_running_hole_four_tv);
        mHoleFiveTv = (TextView)rootView.findViewById(R.id.run_config_slan_running_hole_five_tv);
        mHoleSixTv = (TextView)rootView.findViewById(R.id.run_config_slan_running_hole_six_tv);
        mHoleSevenTv= (TextView)rootView.findViewById(R.id.run_config_slan_running_hole_seven_tv);
        mHoleEightTv = (TextView)rootView.findViewById(R.id.run_config_slan_running_hole_eight_tv);
        mCurveChartGg = (RadioGroup)rootView.findViewById(R.id.run_config_slan_running_curve_layout);
        mRawCurveRb = (RadioButton)rootView.findViewById(R.id.run_config_slan_running_rawcurve_rb);
        mRawCurveRb.setChecked(true);
        mAllHoleCk = (TextView)rootView.findViewById(R.id.run_config_slan_running_all_hole_ck) ;
        mAllChannelCk = (CustomCheckText)rootView.findViewById(R.id.run_config_slan_all_channel_check_tv);
    }


    private void initVariable(){
        mRawCurveList = new ArrayList<BiowayCurveList>();
        mCheckRawCurveList = new ArrayList<BiowayCurveList>();
        mAMPCurveList = new ArrayList<BiowayCurveList>();
        mCheckAMPCurveList = new ArrayList<BiowayCurveList>();
        mMeltCurveList = new ArrayList<BiowayCurveList>();
        mCheckMeltCurveList = new ArrayList<BiowayCurveList>();

        mPeakCurveList = new ArrayList<BiowayCurveList>();
        mCheckPeakList = new ArrayList<BiowayCurveList>();

        mExlResults = new ArrayList<BiowayExlResult>();
        mCheckExlResults = new ArrayList<BiowayExlResult>();

        resultAdapter.setResultList(mCheckExlResults);
        mResultListView.setAdapter(resultAdapter);
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
        if(null != BoWeiService.getInstance()){
            BoWeiService.getInstance().setRunningInfoListener(new RunningInfoListener() {
                @Override
                public void getRunningInfo(@NonNull String serialNo, byte block, @NonNull RunningInfo info) {
                    if(info.getState() == RunningState.Finished){
//                            resetPcrRecMark();
                        MessageEvent done = new MessageEvent();
                        //实验状态完成
                        done.setMsgType(MsgType.MSG_UPDATE_CONFIG_SLAN_DONE);
                        done.setData(info);
                        EventBus.getDefault().post(done);
                        isRunning = false;
                    }else if(info.getState() == RunningState.Running){
                        MessageEvent first_run = new MessageEvent();
                        first_run.setMsgType(MsgType.MSG_RUN_CONFIG_SLAN_RUNNING);
                        first_run.setData(info);
                        EventBus.getDefault().post(first_run);

                    }else if(info.getState() == RunningState.Interrupt){
                        MessageEvent error = new MessageEvent();
                        error.setMsgType(MsgType.MSG_UPDATE_CONFIG_SLAN_ERROR);
                        error.setData(info);
                        EventBus.getDefault().post(error);
//                            resetPcrRecMark();
                    }
                }


            });
        }

        mRunStartTv.setOnClickListener(this);
        mRunStopTv.setOnClickListener(this);
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
                showCurve();
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
                showCurve();
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
                    if(isHEXCheck && isCY5Check && isFAMCheck){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                showCurve();
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
                showCurve();
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
                    if(isROXCheck && isHEXCheck && isFAMCheck ){
                        isAllChanelCheck = true;
                        mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                showCurve();
            }
        });


        mCurveChartGg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.run_config_slan_running_rawcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                    }
                    currentView = 1;
                    getAllRawCurve();
                    drawRawCurve();
                } else if (checkedId == R.id.run_config_slan_running_ampcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                    }
                    currentView = 2;
                    getAllAMPCurve();
                    drawAmpCurve();
                } else if (checkedId == R.id.run_config_slan_running_meltcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                    }
                    currentView = 3;
                    getAllMeltCurve();
                    drawMeltCurve();
                }else if (checkedId == R.id.run_config_result_slan_peakcurve_rb) {
                    //熔解峰值原始峰值曲线
                    if (!isChartVisible) {
                        isChartVisible = true;
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                    }
                    currentView = 4;
                    getAllPeakCurve();
                    drawPeakCurve();
                }else if(checkedId == R.id.run_config_result_slan_list_rb){
                    //运行结束才能显示结果表格
                    Log.i("RUN_CONFIG","isRunning" + isRunning);
                    if(isRunning){
                        //运行未结束，无法查看结果
                        Toast.makeText(getActivity().getApplicationContext()
                                    ,getActivity().getResources().getString(R.string.run_config_toast_run_check_result),Toast.LENGTH_LONG).show();
                    }else{
                        if (isChartVisible) {
                            isChartVisible = false;
                            mResultExlLayout.setVisibility(View.VISIBLE);
                            mResultChartLayout.setVisibility(View.GONE);

                        }
                        getShowExlResults();
                        currentView = 5;
                    }

                }
            }
        });

        mStepCheckTv.setOnClickListener(this);
        mChartCheckTv.setOnClickListener(this);
        mHoleOneTv.setOnClickListener(this);
        mHoleTwoTv.setOnClickListener(this);
        mHoleThreeTv.setOnClickListener(this);
        mHoleFourTv.setOnClickListener(this);
        mHoleFiveTv.setOnClickListener(this);
        mHoleSixTv.setOnClickListener(this);
        mHoleSevenTv.setOnClickListener(this);
        mHoleEightTv.setOnClickListener(this);
        mAllHoleCk.setOnClickListener(this);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mUIHandler = new UIHandler(this);
        BiowayManager.getInstance().setOnParseRunningExCallBack(this);
    }




    @Override
    public void onResume() {
        super.onResume();
        registerEventBus();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterEventBus();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden){
//            unregisterEventBus();
            if(isRunning){
                isRunning = false;
              //  stopExperiment();
            }
            isDone = false;
        }else {
//            registerEventBus();
            parseExRegularly();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        runnable = null;
        if(null != mSLANSections){
            mSLANSections.clear();
            mSLANSections = null;
        }
        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        clearList();
        BiowayManager.getInstance().setExperiment(null);

    }



    private void clearList(){
        if(null != mRawCurveList){
            mRawCurveList.clear();
            mRawCurveList = null;
        }
        if(null != mCheckRawCurveList){
            mCheckRawCurveList.clear();
            mCheckRawCurveList = null;
        }

        if(null != mMeltCurveList){
            mMeltCurveList.clear();
            mMeltCurveList = null;
        }
        if(null != mCheckMeltCurveList){
            mCheckMeltCurveList.clear();
            mCheckMeltCurveList = null;
        }

        if(null != mAMPCurveList){
            mAMPCurveList.clear();
            mAMPCurveList = null;
        }
        if(null != mCheckAMPCurveList){
            mCheckAMPCurveList.clear();
            mCheckAMPCurveList = null;
        }

        if(null != mPeakCurveList){
            mPeakCurveList.clear();
            mPeakCurveList = null;
        }

        if(null != mCheckPeakList){
            mCheckPeakList.clear();
            mCheckPeakList = null;
        }

        if(null != mExlResults){
            mExlResults.clear();
            mExlResults = null;
        }

        if(null != mCheckExlResults){
            mCheckExlResults.clear();
            mCheckExlResults = null;
        }
    }


    private void parseExRegularly() {
        if(null != mUIHandler){
            mUIHandler.postDelayed(runnable, 5 *1000);
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //TODO:延时要干的其他事情
            mUIHandler.sendEmptyMessage(Constants.MSG_PARSE_RUNNING_EX);
            mUIHandler.postDelayed(runnable, 180* 1000);
        }
    };



    /**
     * 解析程序运行的数据
     * 、、@param Experiment
     */
    private void updatePcrRuningState(RunningInfo runningInfo){
        if(null != runningInfo) {
            updateRunningState(runningInfo);
            updateCountDown(runningInfo);
        }

    }

    /**
     * 更新程序运行状态
     * @param runningInfo
     */
    private void updateRunningState(RunningInfo runningInfo){

        mProjStateTv.setContentText(runningInfo.getCurrentRunning());//实验状态
        mCurProgramTv.setContentText(runningInfo.getSegment()); //当前程序段
        mCurStepTv.setContentText(runningInfo.getStep());//当前程序的步骤

        mRunningTimeTv.setContentText(DateFormatUtil.formatHMS(runningInfo.getRunningTime()));
        mRemainTimeTv.setContentText(DateFormatUtil.formatTime(runningInfo.getRemainTime()));

        mCycleTv.setContentText(runningInfo.getCycle() + "");
        mTempTv.setContentText(runningInfo.getSetTemperature() + "°C");//设置的温度
        mHoldTimeTv.setContentText(runningInfo.getHoldTime());
        mTubeTempTv.setContentText(runningInfo.getBlockTemperature1()+ "°C");
        mTubeTemp2Tv.setContentText(runningInfo.getBlockTemperature2()+ "°C");
        mTubeTemp3Tv.setContentText(runningInfo.getBlockTemperature3()+ "°C");
        mHoldTimeTv.setContentText(runningInfo.getHoldTime() + "");

        //实验状态完成/中断，错误时，计时重置
        if (runningInfo.getState() == RunningState.Finished ||
                runningInfo.getState() == RunningState.Interrupt ||
                runningInfo.getError() != RunningError.None) {
            time = 0;
        }
        //运行出错，显示错误信息
        if (runningInfo.getError() != RunningError.None){
            Toast.makeText(getActivity().getApplicationContext(),runningInfo.getCurrentError(),
                    Toast.LENGTH_LONG).show();
        }


        String cur_program  = runningInfo.getSegment();//当前程序段
        String cur_step = runningInfo.getStep();//当前步骤
        int loop = runningInfo.getCycle();
        int program_index = getProgramIndex(cur_program);

        Log.i("RunSLANStepAdapter111","setCurrentPosition 当前步骤"  + cur_step);
        if(null != mSLANSections && mSLANSections.size()>0){
            if (!StringUtil.isEmpty(before_program)&&!before_program.equals(cur_program)
                    || !StringUtil.isEmpty(before_step)&&!before_step.equals(cur_step) || before_loop != loop){
                int step_num = -1;
                if(cur_step.contains("步骤")){
                    String temp = cur_step.replace("步骤","");;
                    step_num = StringUtil.convertToInt(temp,0);
                }else{
                    step_num = 1;
                }
                if(null != stepAdapter){
                    Log.i("BIOWAY","step_num" +step_num );
                    if(!StringUtil.isEmpty(cur_program) &&cur_program.equals("熔解程序段")){
                        step_num = 1;
                        stepAdapter.setCurrentPosition(step_num,program_index);
                    }else{
                        stepAdapter.setCurrentPosition(step_num,program_index);
                    }

                    before_program = cur_program;
                    before_step = cur_step;
                    before_loop = loop;
                }

                //更新剩余循环
                if(null != mProgramRecycleLayout){
                    if(program_index != -1){
                        ConfigSLANSection section =  mSLANSections.get(program_index);
                        if(section.getProgramType() == Constants.PROGRAM_TYPE_AMP){
                            View view = mProgramRecycleLayout.getChildAt(program_index);
                            if( view instanceof  TextView){
                                TextView textView = (TextView)view;
                                //剩余循环
                                int recylceNum = mSLANSections.get(program_index).getCycles();
                                textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_last_recycle) + (recylceNum-loop));
                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * 返程序段的index
     */
    private int getProgramIndex(String cur_program_name){
        if(null != mSLANSections && mSLANSections.size()>0){
            for(int i= 0;i<mSLANSections.size();i++){
                if(mSLANSections.get(i).getName().equals(cur_program_name)){
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * 更新倒计时
     * @param
     */
    public void updateCountDown(RunningInfo runningInfo){
        //剩余时间
        mCountDownTv.setText(DateFormatUtil.formatHMS(runningInfo.getRemainTime()));
    }




    /*
     * EventBus接收订阅的消息
     * @param messageEvent
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()){
            case MsgType.MSG_UPDATE_CONFIG_SLAN_DONE:
                //保存测试文件
                Log.i("run_config","完成PCR流程");

                //重新获取数据
                onRunPcrFinish();
                isRunning = false;
                break;
            case MsgType.MSG_CONFIG_SLAN_RUN:
                fileName = (String)messageEvent.getData();
                //打开
                openSLANExFile(fileName);
                break;
            case MsgType.MSG_RUN_CONFIG_SLAN_RUNNING:
                RunningInfo runningInfo = (RunningInfo) messageEvent.getData();
                if(null != runningInfo){
                    updatePcrRuningState(runningInfo);
                }
                if(!isShow){
                    isShow = true;
                    mCountDownTv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                    mCountDownTv.setTextColor(getActivity().getResources().getColor(R.color.blue));
                }
                break;
            case MsgType.MSG_UPDATE_CONFIG_SLAN_ERROR:

                int error_Code = ((RunningInfo)messageEvent.getData()).getError().getCode();
                if(isShow){
                    isShow = false;
                    mCountDownTv.setBackgroundResource(R.drawable.shape_oval_red_stroke_bg);
                    mCountDownTv.setTextColor(getActivity().getResources().getColor(R.color.red));
                }
                break;

            case MsgType.MSG_UPDATE_CONFIG_RUNNING_EX:
                Log.i("run_config","解析正在运行的experiment");
                if(!isCY5Check && !isFAMCheck && !isHEXCheck && !isROXCheck && mRawCurveList.size()==0){
                    //第一次
                    List<BiowayCurveList> lists = BiowayManager.getInstance().getRawCurve();
                    for(BiowayCurveList curveList : lists){
                        if(curveList.getCurve_hole().equals("HEX")){
                            isHEXCheck = true;
                        }else if(curveList.getCurve_hole().equals("ROX")){
                            isROXCheck = true;
                        }else if(curveList.getCurve_hole().equals("FAM")){
                            isFAMCheck = true;
                        }else if(curveList.getCurve_hole().equals("CY5")){
                            isCY5Check = true;

                        }
                    }
                    setChannelState();
                    checkWellState(lists);
                }

                // 更新结果绘制的图
                showCurve();
                break;

        }
    }


    private void setChannelState(){
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

        if(isHEXCheck){
            mHEX_ChannelTv.setCheckState(R.drawable.item_chanel_check,isHEXCheck);
        }else {
            mHEX_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isHEXCheck);
        }

        if(isFAMCheck){
            mFAM_ChannelTv.setCheckState(R.drawable.item_chanel_check,isFAMCheck);
        }else {
            mFAM_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isFAMCheck);
        }

        if(isCY5Check && isFAMCheck &&isROXCheck && isHEXCheck){
            isAllChanelCheck = true;
            mAllChannelCk.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
        }else{
            isAllChanelCheck = false;
            mAllChannelCk.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
        }
    }
    private void setHodeState(boolean check,TextView textView){
        if(!check){
            textView.setBackgroundResource(R.drawable.shape_oval_gray_bg);
            textView.setTextColor(getActivity().getResources().getColor(R.color.blue));
        }else{
            textView.setBackgroundResource(R.drawable.shape_oval_blue_bg);
            textView.setTextColor(getActivity().getResources().getColor(R.color.white));
        }
    }

    private void setAllHoleCheck(){
        if(isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck){
            isAllCheck = true;
            setHodeState(isAllCheck,mAllHoleCk);
        }else {
            isOneCheck = true;isTwoCheck = true;isThreeCheck = true;isFourCheck = true;
            isFiveCheck = true;isSixCheck = true;isSevenCheck = true;isEightCheck = true;
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

    private void setAllHoleUnCheck(){
        isOneCheck = false;isTwoCheck = false;isThreeCheck = false;
        isFourCheck = false;isFiveCheck = false;isSixCheck = false;
        isSevenCheck = false; isEightCheck = false;
        setHodeState(isOneCheck,mHoleOneTv);
        setHodeState(isTwoCheck, mHoleTwoTv);
        setHodeState(isThreeCheck, mHoleThreeTv);
        setHodeState(isFourCheck, mHoleFourTv);
        setHodeState(isFiveCheck, mHoleFiveTv);
        setHodeState(isSixCheck, mHoleSixTv);
        setHodeState(isSevenCheck, mHoleSevenTv);
        setHodeState(isEightCheck, mHoleEightTv);
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

    private void checkWellState(List<BiowayCurveList> list){
        for(BiowayCurveList curveList : list){
            if(curveList.getWellNum().equals("A1")){
                isOneCheck = true;
                setHodeState(isOneCheck, mHoleOneTv);
            }else if(curveList.getWellNum().equals("A2")){
                isTwoCheck = true;
                setHodeState(isTwoCheck, mHoleTwoTv);
            }else if(curveList.getWellNum().equals("A3")){
                isThreeCheck = true;
                setHodeState(isThreeCheck, mHoleThreeTv);
            } else if(curveList.getWellNum().equals("A4")){
                isFourCheck = true;
                setHodeState(isFourCheck, mHoleFourTv);
                showCurve();
            } else if(curveList.getWellNum().equals("A5")){
                isFiveCheck = true;
                setHodeState(isFiveCheck, mHoleFiveTv);
            } else if(curveList.getWellNum().equals("A6")){
                isSixCheck = true;
                setHodeState(isSixCheck, mHoleSixTv);
            } else if(curveList.getWellNum().equals("A7")){
                isSevenCheck = true;
                setHodeState(isSevenCheck, mHoleSevenTv);
            } else if(curveList.getWellNum().equals("A8")){
                isEightCheck = true;
                setHodeState(isEightCheck, mHoleEightTv);
            }
        }
        setAllHoleCheck();
    }

    /**
     * 获取选中孔号的列表
     */
    private void getAllRawCurve(){
        mRawCurveList.clear();
        List<BiowayCurveList> mRawList = BiowayManager.getInstance().getRawCurve();
        for(BiowayCurveList curveList : mRawList){

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


    private void drawRawCurve(){
        mCheckRawCurveList.clear();
        if(null != mRawCurveList && mRawCurveList.size()>0){
            for(BiowayCurveList curveList : mRawCurveList){
                if(curveList.getCurve_hole().equals("HEX")){
                    if(isHEXCheck){
                        mCheckRawCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("FAM")){
                    if(isFAMCheck){
                        mCheckRawCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("ROX")){
                    if(isROXCheck){
                        mCheckRawCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("CY5")){
                    if(isCY5Check){
                        mCheckRawCurveList.add(curveList);
                    }
                }
            }
        }
        clearChart();
        drawchart(mCheckRawCurveList, mFirstLineChart,"RawCurve");
    }

    /**
     * 获取扩增曲线数值
     */
    private void getAllAMPCurve(){
        mAMPCurveList.clear();
        List<BiowayCurveList> mAmpList = BiowayManager.getInstance().getAMPCurve();
        for(BiowayCurveList curveList : mAmpList){

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

    private void drawAmpCurve() {
        mCheckAMPCurveList.clear();
        if (null != mAMPCurveList && mAMPCurveList.size() > 0) {
            for (BiowayCurveList curveList : mAMPCurveList) {
                if (curveList.getCurve_hole().equals("HEX")) {
                    if (isHEXCheck) {
                        mCheckAMPCurveList.add(curveList);
                    }
                } else if (curveList.getCurve_hole().equals("FAM")) {
                    if (isFAMCheck) {
                        mCheckAMPCurveList.add(curveList);
                    }
                } else if (curveList.getCurve_hole().equals("ROX")) {
                    if (isROXCheck) {
                        mCheckAMPCurveList.add(curveList);
                    }
                } else if (curveList.getCurve_hole().equals("CY5")) {
                    if (isCY5Check) {
                        mCheckAMPCurveList.add(curveList);
                    }
                }
            }
        }
        clearChart();
        drawchart(mCheckAMPCurveList, mFirstLineChart,"AMPCurve");
    }

    /**
     * 获取扩增曲线数值
     */
    private void getAllMeltCurve(){
        mMeltCurveList.clear();
        List<BiowayCurveList> mMeltList = BiowayManager.getInstance().getMeltCurve();
        for(BiowayCurveList curveList : mMeltList){

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

    private void drawMeltCurve() {
        mCheckMeltCurveList.clear();
        if (null != mMeltCurveList && mMeltCurveList.size() > 0) {
            for (BiowayCurveList curveList : mMeltCurveList) {
                if (curveList.getCurve_hole().equals("HEX")) {
                    if (isHEXCheck) {
                        mCheckMeltCurveList.add(curveList);
                    }
                } else if (curveList.getCurve_hole().equals("FAM")) {
                    if (isFAMCheck) {
                        mCheckMeltCurveList.add(curveList);
                    }
                } else if (curveList.getCurve_hole().equals("ROX")) {
                    if (isROXCheck) {
                        mCheckMeltCurveList.add(curveList);
                    }
                } else if (curveList.getCurve_hole().equals("CY5")) {
                    if (isCY5Check) {
                        mCheckMeltCurveList.add(curveList);
                    }
                }
            }
        }
        clearChart();
        drawchart(mCheckMeltCurveList, mFirstLineChart,"MeltCurve");
    }

    private void getAllPeakCurve(){
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

        Log.i("run_config", "getClickPeakCurveResult" +mPeakCurveList.size() );
    }

    private void  drawPeakCurve(){
        if(null != mPeakCurveList){
            mCheckPeakList.clear();
            if(mPeakCurveList.size()>0){
                for(BiowayCurveList curveList : mPeakCurveList){

                    if(curveList.getCurve_hole().equals("HEX")){
                        if(isHEXCheck){
                            mCheckPeakList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("ROX")){
                        if(isROXCheck){
                            mCheckPeakList.add(curveList);
                        }
                    }else if(curveList.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
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
        drawchart(mCheckPeakList,mFirstLineChart,"PeakCurve");
    }

    /**
     * 表格结果
     */
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
                    if(exlResult.getCurve_hole().equals("HEX")){
                        if(isHEXCheck){
                            mCheckExlResults.add(exlResult);
                        }
                    }else if(exlResult.getCurve_hole().equals("ROX")){
                        if(isROXCheck){
                            mCheckExlResults.add(exlResult);
                        }
                    }else if(exlResult.getCurve_hole().equals("FAM")){
                        if(isFAMCheck){
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


    public void drawchart(List<BiowayCurveList> curve_list, LineChart lineChart, String describe) {
        if (curve_list == null) {
            curve_list = new ArrayList<>();
        }
        if(lineChart!=null) {
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
                if(curve_list.get(i).getCurve_hole() != null){
                    if (curve_list.get(i).getCurve_hole().contains("FAM")) {
                        my_color = PcrValue.colorline[0];//蓝色
                    } else if (curve_list.get(i).getCurve_hole().contains("HEX")) {
                        my_color = PcrValue.colorline[1];//绿色
                    } else if (curve_list.get(i).getCurve_hole().contains("ROX")) {
                        my_color = PcrValue.colorline[2];//黄色
                    } else if (curve_list.get(i).getCurve_hole().contains("CY5")) {
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
            lineChart.invalidate();
        }
    }

    public void clearChart(){
        mFirstLineChart.clear();
        mFirstLineChart.notifyDataSetChanged();
        mFirstLineChart.invalidate();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.run_config_slan_running_step_tv) {
            mRunningStepLayout.setVisibility(View.VISIBLE);
            mRunningChartLayout.setVisibility(View.GONE);
            mStepCheckTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mChartCheckTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
        } else if (id == R.id.run_config_slan_running_curve_tv) {
            mRunningStepLayout.setVisibility(View.GONE);
            mRunningChartLayout.setVisibility(View.VISIBLE);
            mChartCheckTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mStepCheckTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
        } else if (id == R.id.run_config_slan_running_hole_one_tv) {
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
            showCurve();
        } else if (id == R.id.run_config_slan_running_hole_two_tv) {
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
            showCurve();
        } else if (id == R.id.run_config_slan_running_hole_three_tv) {
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
            showCurve();
        } else if (id == R.id.run_config_slan_running_hole_four_tv) {
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
            showCurve();
        } else if (id == R.id.run_config_slan_running_hole_five_tv) {
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
            showCurve();
        } else if (id == R.id.run_config_slan_running_hole_six_tv) {
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
            showCurve();
        } else if (id == R.id.run_config_slan_running_hole_seven_tv) {
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
            showCurve();
        } else if (id == R.id.run_config_slan_running_hole_eight_tv) {
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
            showCurve();
        } else if (id == R.id.run_config_slan_running_all_hole_ck) {
            if (isAllCheck) {
                isAllCheck = false;
                setAllHoleUnCheck();
            } else {
                isAllCheck = true;
                setAllHoleCheck();

            }
            setHodeState(isAllCheck, mAllHoleCk);
        } else if(id == R.id.config_run_start_tv){
            //开始运行程序
            if(!isRunning){
                if(!StringUtil.isEmpty(fileName)){
                    runExperiment(fileName);
                }else{
                    Toast.makeText(getActivity().getApplicationContext(),"选中的文件是空",Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(getActivity().getApplicationContext(),"已经有程序正在运行",Toast.LENGTH_LONG).show();
            }
        } else if(id == R.id.config_run_stop_tv){
            if(isRunning){
                stopExperiment();
            }else{
                Toast.makeText(getActivity().getApplicationContext(),"没有程序正在运行无需停止",Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showCurve(){
        switch (currentView){
            case 1:
                getAllRawCurve();
                drawRawCurve();
                break;
            case 2:
                getAllAMPCurve();
                drawAmpCurve();
                break;
            case 3:
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
                    resultAdapter.setResultList(mCheckExlResults);
                    resultAdapter.notifyDataSetChanged();
                }
                break;
            default:
                break;
        }
    }

    /**
     * 解析正在运行的实验结果的回调
     */
    @Override
    public void onParseRunningFinish() {
        //解析完成可以绘制曲线
        MessageEvent updateRunEx = new MessageEvent();
        updateRunEx.setMsgType(MsgType.MSG_UPDATE_CONFIG_RUNNING_EX);
        EventBus.getDefault().post(updateRunEx);
    }

    @Override
    public void onParseExFinish() {
        isRunning = false;

    }

    private void pasreRunningEx(){
        try {
            if(PcrApplication.is_SLAN_PCR_connect){
                Experiment experiment = BoWeiService.getInstance().getExperimentResult();
                if(null == experiment){
                    return;
                }
                String starttime = experiment.getStartTime();
              /*  long temp = Long.parseLong(starttime);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String timeString = format.format(temp/10000);*/
                mStartTimeTv.setContentText(starttime);
                BiowayManager.getInstance().parseSLANRunningEx(experiment);
            }

        }catch (Exception e){
            e.getMessage();
        }

    }

    public class UIHandler extends Handler {

        WeakReference<RunConfigFragment> mFragmentReference;

        public UIHandler(RunConfigFragment mFragment) {
            mFragmentReference = new WeakReference<RunConfigFragment>(mFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            final RunConfigFragment mFragment = mFragmentReference.get();
            if (null == mFragment) {
                return;
            }
            switch (msg.what) {
                case Constants.MSG_PARSE_RUNNING_EX:
                    mFragment.pasreRunningEx();
                    break;
            }
        }
    }



    private void updateProgramUI(){
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
                            new LinearLayout.LayoutParams(162*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(0, 0, 2, 0);
                    textView.setLayoutParams(layoutParams);
                    int index = i+1;
                    textView.setText(getActivity().getResources().getString(R.string.runing_pcr_section)+ index);
                }else if(programType == Constants.PROGRAM_TYPE_MELT){
                    final LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(162*2,LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(0, 0, 2, 0);
                    textView.setLayoutParams(layoutParams);
                    textView.setText(getActivity().getResources().getString(R.string.runing_pcr_section_melt));
                }
                mProgramTitleLayout.addView(textView);
            }

            stepAdapter.setProgramSteps(mSLANSections);

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
                    layoutParams.setMargins(0, 0, 2, 0);
                    textView.setLayoutParams(layoutParams);
                    textView.setText(getActivity().getResources().getString(R.string.running_pcr_recycle_num) + (mSLANSections.get(i).getCycles()));
                }else if(programType == Constants.PROGRAM_TYPE_MELT){
                    final LinearLayout.LayoutParams layoutParams =
                            new LinearLayout.LayoutParams(162*2,LinearLayout.LayoutParams.MATCH_PARENT);
                    layoutParams.setMargins(0, 0, 2, 0);
                    textView.setLayoutParams(layoutParams);

                    int scanmode = mSLANSections.get(i).getScanMode();
                    if(scanmode == Constants.MELT_SCANMODE_CONTINUE){
                        textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_scanmode_continuous) + " "
                                + getActivity().getResources().getString(R.string.ruuning_pcr_rate) + nf.format(mSLANSections.get(i).getRate())+"℃/s" );
                    }else if(scanmode == Constants.MELT_SCANMODE_STEP){
                        textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_scanmode_step) + " "
                                + getActivity().getResources().getString(R.string.ruuning_pcr_interval) + nf.format(mSLANSections.get(i).getInterval())+"℃" + " "
                                + getActivity().getResources().getString(R.string.ruuning_pcr_melt_holdtime) + mSLANSections.get(i).getMeltHoldtime()+"s");
                    }
                }
                mProgramRecycleLayout.addView(textView);

            }
        }
    }

    private  void openSLANExFile(String fileName){
        //打开文件夹
        if(!StringUtil.isEmpty(fileName)){
            ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
            if(null != mConfigProjects && mConfigProjects.size()>0){
                for(Project project :mConfigProjects){
                    if(null != project){
                        if(project.getName().contains(fileName)){
                            Program program = project.getProgram();
                            if(project.getType() == Constants.PROGRAM_TYPE_MELT){
                                isMelt = true;
                            }else if(project.getType() == Constants.PROGRAM_TYPE_AMP){
                                isMelt = false;
                            }
                           /* Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

                                @Override
                                public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                                    if (src == src.longValue())
                                        return new JsonPrimitive(src.longValue());
                                    return new JsonPrimitive(src);
                                }
                            }).create();
                            String s = gson.toJson(project);
                            Log.i("AAA","测试项目--"+ s);*/
                            //转化为String
                            ArrayList<Section> sections = program.getSections();
                            if(null != sections){
                                getSLANSections(sections);
                            }

                        }
                    }
                }

                mProjNameTv.setContentText(fileName);
                updateProgramUI();
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
     * 运行程序
     */
    private void runExperiment(String fileName){
        ArrayList<Well> wells = new ArrayList<>();
        for (int i = 1; i<9;i++) {
            Well well = new Well();
            well.setName("A"+i);
            well.setProjectName(fileName);
            //患者信息
            well.setID(i + "");
            well.setPatientName("PatientName");
            well.setSex("Sex");
            well.setAge("Age");
            well.setSpecimen("Specimen");
            well.setSpecimenDate("SpecimenDate");
            well.setDoctor("Doctor");

            well.setOffice("Office");

            well.setDiagnosis("Diagnosis");

            well.setCaseID("CaseID");

            well.setBedID("BedID");

            well.setInPatientID("InPatientID");

            well.setOutPatientID("OutPatientID");

            well.setRemark("Remark");

            //样品信息

            well.setSampleName("样品1");

            well.setTubeNo(1);

            well.setTubeName("试管1");

            well.setType(SampleType.Unknown.getValue());

            well.setProperty("Property");

            wells.add(well);

        }
        if(PcrApplication.is_SLAN_PCR_connect){
            ExperimentParam runExParam = new ExperimentParam();
            runExParam.setPath(FileUtils.CONFIG_SLAN_PATH);
            runExParam.setName(fileName);
            RunBiowayResult runExResult = BoWeiService.getInstance().runExperiment(runExParam);
            if(runExResult.getRunExperimentResult() == RunExperimentResult.Normal){
                //运行成
                isRunning = true;
            }else {
                isRunning = false;
            }
        }else{
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.run_config_pcr_device_disconnect),Toast.LENGTH_LONG).show();
        }

    }


    private void stopExperiment(){
        BoWeiService.getInstance().stopExperiment((byte) 0x01);
        isRunning = false;
    }

    private void onRunPcrFinish(){
        Log.i("RUN_CONFIG" ,"onRunPcrFinish");
        Experiment experiment = BoWeiService.getInstance().getExperimentResult();
        saveRunPcrResultFile(experiment,fileName);
        //解析结束后的
        BiowayManager.getInstance().parseRunFinishConfigEx(experiment,isMelt);
    }
    /**
     * 保存测试PCR文件保存的.PCR文件
     */
    private void saveRunPcrResultFile(Experiment experiment ,String fileName){
        if(!StringUtil.isEmpty(fileName)){
            saveResultToPCR(experiment, fileName);
        }
    }

    public boolean saveResultToPCR(Experiment experiment, String fileName) {
        Log.e("run_config", "saveResultToPCR");
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        if (experiment == null) {
            return false;
        }

        try {
            String saveFileName = DateFormatUtil.getFileName() + "_"+fileName+ ".pcr";
            String savePath = FileUtils.CONFIG_RESULT_PATH ;
            File dir = new File(savePath);
            if(!dir.exists()){
                dir.mkdirs();
            }
            boolean save_result = SLANService.INSTANCE.saveExperiment(experiment.getName(), savePath + saveFileName);
            if (save_result) {
                //保存PCR文件成功
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.run_config_toast_save_pcr_file_success),Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.run_config_toast_save_pcr_file_fail),Toast.LENGTH_LONG).show();
                return false;
            }
            if(save_result){
                //////FTP上报.PCR服务器
                FtpUploadManager.getInstance().upLoadFile(savePath + saveFileName,saveFileName,savePath);
            }
            return true;
        } catch (Exception e) {
            Log.e("saveResultToPCR",e.getMessage());
            return false;
        }
    }
}
