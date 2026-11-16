package com.gidi.bio_pcr.bioway.running;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.SLANStepAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bean.ExTemperature;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.pcr.OnParseRunningExCallBack;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomCheckText;
import com.gidi.bio_pcr.view.CustomTitleText;
import com.gidi.bio_pcr.view.CustomValueFormatter;
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
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningError;
import com.winters.boweilibrary.BoWei.bioweyenum.RunningState;
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

public class RunBioWaySecondPCRFragment extends BaseFragment implements View.OnClickListener, OnParseRunningExCallBack {
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
    //曲线，温度曲线，温度颜色，通道
    private LinearLayout mCurveLayout,mTemperatureLayout,mTempColorLayout,mChannelLayout;
    //试管温度
    private CustomTitleText mTubeTempTv,mTubeTemp2Tv,mTubeTemp3Tv;

    private RelativeLayout mRunningChartLayout;
    private HorizontalScrollView mRunningStepLayout;
    private TextView mCountDownTv;

    private RecyclerView programRv;
    private LinearLayout mProgramTitleLayout;
    private LinearLayout mProgramRecycleLayout;
    private List<ConfigSLANSection> mSLANSections;
    private SLANStepAdapter stepAdapter;
    private DecimalFormat nf = new DecimalFormat( "0.00");

    private String before_program = "程序段-1";
    private String before_step = "步骤-1";
    private int before_loop = -1;

    private TextView mStepCheckTv,mChartCheckTv;
    private LineChart mFirstLineChart,mTemperatureChart;
    private CustomCheckText mFAM_ChannelTv,mHEX_ChannelTv,mROX_ChannelTv,mCY5_ChannelTv;

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
    private TextView mHoleOneTv,mHoleTwoTv,mHoleThreeTv,mHoleFourTv,mHoleFiveTv,mHoleSixTv,mHoleSevenTv,mHoleEightTv;
    private boolean isOneCheck,isTwoCheck,isThreeCheck,isFourCheck,isFiveheck,isSixCheck,isSevenCheck,isEightCheck = false;
    private int currentView = 1;

    private boolean isAllCheck = false;
    private TextView mAllHoleCk;
    private CustomCheckText mAllChannelCk;
    private boolean isAllChanelCheck = false;

    private UIHandler mUIHandler;
    private ArrayList<ExTemperature> mExTemperatures;
    private boolean isChartVisible = false;
    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_bioway_running_second_pcr;
    }

    @Override
    protected void initView(View rootView) {

        mProjNameTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_project_tv);
        mProjStateTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_project_state_tv);
        mStartTimeTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_start_time_tv);
        mRunningTimeTv= (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_running_time_tv);
        mRemainTimeTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_lasttime_tv);
        mCurProgramTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_current_program_tv) ;
        mCurStepTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_current_step_tv);
        mTempTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_project_temp_tv);
        mTubeTempTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_project_tubetemp_tv);
        mTubeTemp2Tv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_project_tubetemp_2_tv);
        mTubeTemp3Tv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_project_tubetemp_3_tv);
        mCycleTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_project_loop_tv);
        mCountDownTv = (TextView)rootView.findViewById(R.id.bioway_second_pcr_countdown_tv);
        mHoldTimeTv = (CustomTitleText)rootView.findViewById(R.id.bioway_running_second_pcr_project_holdtime_tv);

        mRunningStepLayout = (HorizontalScrollView)rootView.findViewById(R.id.bioway_second_pcr_step_layout);
        mRunningChartLayout = (RelativeLayout)rootView.findViewById(R.id.bioway_second_pcr_running_chart_layout) ;

        mProgramTitleLayout = (LinearLayout)rootView.findViewById(R.id.bioway_second_pcr_program_title_layout);
        mProgramRecycleLayout = (LinearLayout)rootView.findViewById(R.id.bioway_second_pcr_program_recycle_layout);
        programRv = (RecyclerView)rootView.findViewById(R.id.bioway_second_pcr_rv);
        mSLANSections = new ArrayList<ConfigSLANSection>();
        programRv.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        programRv.addItemDecoration(new DividerDecoration(getActivity(), 2));
        stepAdapter = new SLANStepAdapter(getActivity().getApplicationContext());
        mSLANSections.clear();
        stepAdapter.setProgramSteps(mSLANSections);
        programRv.setAdapter(stepAdapter);
        initChartView(rootView);
        initVariable();
    }


    private void initChartView(View rootView){
        mStepCheckTv = (TextView)rootView.findViewById(R.id.bioway_second_running_step_tv) ;
        mChartCheckTv  = (TextView)rootView.findViewById(R.id.bioway_second_running_curve_tv);
        mFirstLineChart = (LineChart)rootView.findViewById(R.id.bioway_second_pcr_running_result_curve_chart);
        if(null != mFirstLineChart){
            mFirstLineChart.setNoDataText("");
        }
        mCurveLayout = (LinearLayout)rootView.findViewById(R.id.bioway_second_curve_chart_layout);
        mChannelLayout = (LinearLayout)rootView.findViewById(R.id.bioway_second_pcr_running_channel_layout);
        mTemperatureLayout = (LinearLayout)rootView.findViewById(R.id.bioway_second_temperature_curve_chart_layout);
        mTempColorLayout = (LinearLayout)rootView.findViewById(R.id.bioway_second_pcr_running_temp_color_layout);
        mTemperatureChart = (LineChart)rootView.findViewById(R.id.bioway_second_pcr_running_temperature_curve_chart);
        mFAM_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.bioway_second_pcr_running_result_fam_tv);
        mHEX_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.bioway_second_pcr_running_result_hex_tv);
        mROX_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.bioway_second_pcr_running_result_rox_tv);
        mCY5_ChannelTv = (CustomCheckText)rootView.findViewById(R.id.bioway_second_pcr_running_result_cy5_tv);

        mHoleOneTv = (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_hole_one_tv);
        mHoleTwoTv = (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_hole_two_tv);
        mHoleThreeTv = (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_hole_three_tv);
        mHoleFourTv = (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_hole_four_tv);
        mHoleFiveTv = (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_hole_five_tv);
        mHoleSixTv = (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_hole_six_tv);
        mHoleSevenTv= (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_hole_seven_tv);
        mHoleEightTv = (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_hole_eight_tv);
        mCurveChartGg = (RadioGroup)rootView.findViewById(R.id.bioway_second_pcr_running_curve_layout);
        mRawCurveRb = (RadioButton)rootView.findViewById(R.id.bioway_second_pcr_running_rawcurve_rb);
        mRawCurveRb.setChecked(true);
        mAllHoleCk = (TextView)rootView.findViewById(R.id.bioway_second_pcr_running_all_hole_ck) ;
        mAllChannelCk = (CustomCheckText)rootView.findViewById(R.id.bioway_second_pcr_all_channel_check_tv);
    }


    private void initVariable(){
        mRawCurveList = new ArrayList<BiowayCurveList>();
        mCheckRawCurveList = new ArrayList<BiowayCurveList>();
        mAMPCurveList = new ArrayList<BiowayCurveList>();
        mCheckAMPCurveList = new ArrayList<BiowayCurveList>();
        mMeltCurveList = new ArrayList<BiowayCurveList>();
        mCheckMeltCurveList = new ArrayList<BiowayCurveList>();
        mExTemperatures = new ArrayList<ExTemperature>();
    }


    @Override
    protected void initListener() {
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
                    if(isROXCheck && isHEXCheck && isFAMCheck){
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
                if (checkedId == R.id.bioway_second_pcr_running_rawcurve_rb) {
                    currentView = 1;
                    mCurveLayout.setVisibility(View.VISIBLE);
                    mChannelLayout.setVisibility(View.VISIBLE);
                    mTemperatureLayout.setVisibility(View.GONE);
                    mTempColorLayout.setVisibility(View.GONE);
                    showCurve();
                } else if (checkedId == R.id.bioway_second_pcr_running_ampcurve_rb) {
                    currentView = 2;
                    mCurveLayout.setVisibility(View.VISIBLE);
                    mChannelLayout.setVisibility(View.VISIBLE);
                    mTemperatureLayout.setVisibility(View.GONE);
                    mTempColorLayout.setVisibility(View.GONE);
                    showCurve();
                } else if (checkedId == R.id.bioway_second_pcr_running_meltcurve_rb) {
                    currentView = 3;
                    mCurveLayout.setVisibility(View.VISIBLE);
                    mChannelLayout.setVisibility(View.VISIBLE);
                    mTemperatureLayout.setVisibility(View.GONE);
                    mTempColorLayout.setVisibility(View.GONE);
                    showCurve();
                }else if(checkedId == R.id.bioway_second_pcr_running_temperature_rb){
                    currentView = 4;
                    mCurveLayout.setVisibility(View.GONE);
                    mChannelLayout.setVisibility(View.GONE);
                    mTemperatureLayout.setVisibility(View.VISIBLE);
                    mTempColorLayout.setVisibility(View.VISIBLE);
                    showCurve();
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
        registerEventBus();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // registerEventBus();
        mUIHandler = new UIHandler(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerEventBus();
        BiowayManager.getInstance().setOnParseRunningExCallBack(this);
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden){
            unregisterEventBus();
            if(null != mUIHandler){
                mUIHandler.removeCallbacksAndMessages(null);
                mUIHandler = null;
            }

        }else {
            parseExRegularly();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("AAA","second pcr onStop");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i("BIOWAY","second pcr onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterEventBus();
        runnable = null;
        if(null != mSLANSections){
            mSLANSections.clear();
            mSLANSections = null;
        }
        clearList();
        BiowayManager.getInstance().releaseParseRunningExCallBack();
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
        if(null != mExTemperatures){
            mExTemperatures.clear();
            mExTemperatures = null;
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
    private long time = 0;
    private void updateRunningState(RunningInfo runningInfo){
        mProjNameTv.setContentText(File.separator+ PcrApplication.rootTempName+File.separator+"second.prj");

        mProjStateTv.setContentText(runningInfo.getCurrentRunning());//实验状态
        mCurProgramTv.setContentText(runningInfo.getSegment()); //当前程序段
        mCurStepTv.setContentText(runningInfo.getStep());//当前程序的步骤
        mRunningTimeTv.setContentText(DateFormatUtil.formatTime(runningInfo.getRunningTime()));
       // mRunningTimeTv.setContentText(HSUtils.INSTANCE.secondToHMSString(time / 1000));
        mRemainTimeTv.setContentText(DateFormatUtil.formatTime(runningInfo.getRemainTime()));
        mCycleTv.setContentText(runningInfo.getCycle() + "");
        mTempTv.setContentText(runningInfo.getSetTemperature() + "°C");//设置的温度
        mTubeTempTv.setContentText(runningInfo.getBlockTemperature1()+ "°C");
        mTubeTemp2Tv.setContentText(runningInfo.getBlockTemperature2()+ "°C");
        mTubeTemp3Tv.setContentText(runningInfo.getBlockTemperature3()+ "°C");
        mHoldTimeTv.setContentText(runningInfo.getHoldTime() + "");
        //实验状态完成/中断，错误时，计时重置
        if (runningInfo.getState() == RunningState.Finished ||
                runningInfo.getState() == RunningState.Interrupt ||
                runningInfo.getError() != RunningError.None) {
        }
        //运行出错，显示错误信息
        if (runningInfo.getError() != RunningError.None){
            Toast.makeText(getActivity().getApplicationContext(),runningInfo.getCurrentError(),
                    Toast.LENGTH_LONG).show();
        }

        String cur_program  = runningInfo.getSegment();
        String cur_step = runningInfo.getStep();
        int loop = runningInfo.getCycle();
        int program_index = getProgramIndex(cur_program);
        if (!StringUtil.isEmpty(before_program) &&!before_program.equals(cur_program)
                || !StringUtil.isEmpty(before_step)&&!before_step.equals(cur_step) || before_loop != loop){

            int step_num = -1;
            if(cur_step != null &&cur_step.contains("步骤")){
                String temp = cur_step.replace("步骤","");;
                step_num = StringUtil.convertToInt(temp,0);
            }else{
                step_num = 1;
            }

            if(null != stepAdapter){
                if(!StringUtil.isEmpty(cur_program)&& cur_program.equals("熔解程序段")){
                    step_num = 1;
                    stepAdapter.setCurrentPosition(step_num,program_index);
                }else{
                    Log.i("BIOWAY","step_num" +step_num );
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
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()){

            case MsgType.MSG_START_SLAN_SECOND_PCR:
                //找到PCR文件
                break;
            case MsgType.MSG_START_SLAN_SECOND_PCR_SUCCESS:
                Log.e("BIOWAY","22--second pcr success" );
                Project project = (Project)messageEvent.getData();
                if(project != null && project.getName().contains("second")){
                    Program program = project.getProgram();
                    //转化为String
                    ArrayList<Section> sections = program.getSections();
                    if(null != sections){
                        getSLANSections(sections);
                    }

                }
                Log.i("BIOWAY","mSLANSections" + mSLANSections.size());
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
                         //   textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_rate) + nf.format(mSLANSections.get(i).getRate())+"℃/s");
                            textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_scanmode_step) + "  "
                                    + getActivity().getResources().getString(R.string.ruuning_pcr_interval) + nf.format(mSLANSections.get(i).getInterval())+"℃" + " "
                                    + getActivity().getResources().getString(R.string.ruuning_pcr_melt_holdtime) + mSLANSections.get(i).getMeltHoldtime()+"s");
                        }
                        mProgramRecycleLayout.addView(textView);
                    }
                }

                //程序文件
                break;

            case MsgType.MSG_SECOND_SLAN_PCR_RUNNING:
                RunningInfo runningInfo = (RunningInfo) messageEvent.getData();
                if(null != runningInfo){
                    updatePcrRuningState(runningInfo);

                }

                break;
            case MsgType.MSG_SECOND_SLAN_PCR_DONE:
                RunningInfo doneInfo = (RunningInfo) messageEvent.getData();
                if(null != doneInfo){
                    //更新界面状态
                    updatePcrRuningState(doneInfo);
                }
                break;
            case MsgType.MSG_SECOND_SLAN_PCR_INTERRUOT:
            case MsgType.MSG_SECOND_SLAN_PCR_ERROR:
                 mCountDownTv.setBackgroundResource(R.drawable.shape_oval_red_stroke_bg);
                 mCountDownTv.setTextColor(getActivity().getResources().getColor(R.color.red));
                break;

            case MsgType.MSG_UPDATE_RUNNING_EX:
                if(!isCY5Check && !isFAMCheck && !isHEXCheck && !isROXCheck && mMeltCurveList.size()==0){
                    //第一次
                    List<BiowayCurveList> lists = BiowayManager.getInstance().getMeltCurve();
                    for(BiowayCurveList curveList : lists){
                        if(!StringUtil.isEmpty(curveList.getCurve_hole())){
                            if(curveList.getCurve_hole().equals("FAM")){
                                isFAMCheck = true;
                            } else if(curveList.getCurve_hole().equals("HEX")){
                                isHEXCheck = true;
                            }else if(curveList.getCurve_hole().equals("ROX")){
                                isROXCheck = true;
                            }else if(curveList.getCurve_hole().equals("CY5")){
                                isCY5Check = true;
                            }
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
                        ConfigSLANSection slanSection1 = new ConfigSLANSection();
                        slanSection1.setName(sectionMelt.getName());
                        slanSection1.setIdx(sectionMelt.getIdx());
                        slanSection1.setProgramType(Constants.PROGRAM_TYPE_MELT);//熔解段
                        slanSection1.setRate(sectionMelt.getRate());
                        slanSection1.setScanMode(sectionMelt.getScanMode());
                        if(sectionMelt.getInterval()== 0.1f|| sectionMelt.getInterval()== 0.2f
                                || sectionMelt.getInterval() == 0.05f){
                            slanSection1.setInterval(sectionMelt.getInterval());
                        }else{
                            slanSection1.setInterval((double)0.1f);
                        }
                        slanSection1.setMeltHoldtime(sectionMelt.getHoldTime());

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
                        slanSection1.setItems(slanItems);
                        mSLANSections.add(slanSection1);
                    }
                }

            }
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
        }else{
            mHEX_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isHEXCheck);
        }

        if(isFAMCheck){
            mFAM_ChannelTv.setCheckState(R.drawable.item_chanel_check,isFAMCheck);
        }else {
            mFAM_ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isFAMCheck);
        }


        if(isCY5Check && isFAMCheck &&isROXCheck && isHEXCheck ){
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
                && isFiveheck && isSixCheck && isSevenCheck && isEightCheck){
            isAllCheck = true;
            setHodeState(isAllCheck,mAllHoleCk);
        }else {
            isOneCheck = true;isTwoCheck = true;isThreeCheck = true;isFourCheck = true;
            isFiveheck = true;isSixCheck = true;isSevenCheck = true;isEightCheck = true;
            isAllCheck = true;
            setHodeState(isOneCheck,mHoleOneTv);
            setHodeState(isTwoCheck, mHoleTwoTv);
            setHodeState(isThreeCheck, mHoleThreeTv);
            setHodeState(isFourCheck, mHoleFourTv);
            setHodeState(isFiveheck, mHoleFiveTv);
            setHodeState(isSixCheck, mHoleSixTv);
            setHodeState(isSevenCheck, mHoleSevenTv);
            setHodeState(isEightCheck, mHoleEightTv);
        }

    }


    private void setAllHoleUnCheck(){
        isOneCheck = false;isTwoCheck = false;isThreeCheck = false;
        isFourCheck = false;isFiveheck = false;isSixCheck = false;
        isSevenCheck = false; isEightCheck = false;
        setHodeState(isOneCheck,mHoleOneTv);
        setHodeState(isTwoCheck, mHoleTwoTv);
        setHodeState(isThreeCheck, mHoleThreeTv);
        setHodeState(isFourCheck, mHoleFourTv);
        setHodeState(isFiveheck, mHoleFiveTv);
        setHodeState(isSixCheck, mHoleSixTv);
        setHodeState(isSevenCheck, mHoleSevenTv);
        setHodeState(isEightCheck, mHoleEightTv);
    }

    private void checkAllHole(){
        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveheck && isSixCheck && isSevenCheck && isEightCheck) {
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
            } else if(curveList.getWellNum().equals("A5")){
                isFiveheck = true;
                setHodeState(isFiveheck, mHoleFiveTv);
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

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线
            xAxis.setLabelCount(curve_list.size(), false);
            lineChart.invalidate();
        }
    }


    /**清除图表**/
    public void clearChart(){
        mFirstLineChart.clear();
        mFirstLineChart.notifyDataSetChanged();
        mFirstLineChart.invalidate();
    }

    private void showCurve(){
        if(isChartVisible){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    switch (currentView){
                        case 1:
                            //原始数据
                            boolean raw_result = getAllRawCurve();
                            if(raw_result){
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        drawRawCurve();
                                    }
                                });
                            }
                            break;

                        case 2:
                            //扩增数据
                            boolean amp_result = getAllAMPCurve();
                            if(amp_result){
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        drawAmpCurve();
                                    }
                                });
                            }
                            break;

                        case 3:
                            //熔解数据
                            boolean result = getAllMeltCurve();
                            if(result){
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        drawMeltCurve();
                                    }
                                });
                            }
                            break;

                        case 4:
                            boolean temp_result = getTemperatures();
                            if(temp_result && isChartVisible){
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        drawTemperatureCurve();
                                    }
                                });
                            }
                            break;

                    }

                }
            }).start();
        }
    }


    /**
     * 获取选中孔号的列表
     */
    private boolean getAllRawCurve(){
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

            } else if (isFiveheck && curveList.getWellNum().equals("A5")) {
                mRawCurveList.add(curveList);

            } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                mRawCurveList.add(curveList);

            } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                mRawCurveList.add(curveList);

            } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                mRawCurveList.add(curveList);
            }
        }

        mCheckRawCurveList.clear();
        if(null != mRawCurveList && mRawCurveList.size()>0){
            for(BiowayCurveList curveList : mRawCurveList){
                if(curveList.getCurve_hole().equals("FAM")){
                    if(isFAMCheck){
                        mCheckRawCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("HEX")){
                    if(isHEXCheck){
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
        return true;
    }


    private void drawRawCurve(){
        clearChart();
        drawchart(mCheckRawCurveList, mFirstLineChart,"RawCurve");
    }

    /**
     * 获取扩增曲线数值
     */
    private boolean getAllAMPCurve(){
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

            } else if (isFiveheck && curveList.getWellNum().equals("A5")) {
                mAMPCurveList.add(curveList);

            } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                mAMPCurveList.add(curveList);

            } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                mAMPCurveList.add(curveList);

            } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                mAMPCurveList.add(curveList);
            }
        }
        mCheckAMPCurveList.clear();
        if (null != mAMPCurveList && mAMPCurveList.size() > 0) {
            for (BiowayCurveList curveList : mAMPCurveList) {
                if(curveList.getCurve_hole().equals("FAM")){
                    if (isFAMCheck) {
                        mCheckAMPCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("HEX")){
                    if (isHEXCheck) {
                        mCheckAMPCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("ROX")){
                    if (isROXCheck) {
                        mCheckAMPCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("CY5")){
                    if (isCY5Check) {
                        mCheckAMPCurveList.add(curveList);
                    }
                }
            }
        }
        return true;
    }

    private void drawAmpCurve() {
        clearChart();
        drawchart(mCheckAMPCurveList, mFirstLineChart,"AMPCurve");
    }

    /**
     * 获取扩增曲线数值
     */
    private boolean getAllMeltCurve(){
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

            } else if (isFiveheck && curveList.getWellNum().equals("A5")) {
                mMeltCurveList.add(curveList);

            } else if (isSixCheck && curveList.getWellNum().equals("A6")) {
                mMeltCurveList.add(curveList);

            } else if (isSevenCheck && curveList.getWellNum().equals("A7")) {
                mMeltCurveList.add(curveList);

            } else if (isEightCheck && curveList.getWellNum().equals("A8")) {
                mMeltCurveList.add(curveList);
            }
        }
        mCheckMeltCurveList.clear();
        if (null != mMeltCurveList && mMeltCurveList.size() > 0) {
            for (BiowayCurveList curveList : mMeltCurveList) {
                if(curveList.getCurve_hole().equals("FAM")){
                    if (isFAMCheck) {
                        mCheckMeltCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("HEX")){
                    if (isHEXCheck) {
                        mCheckMeltCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("ROX")){
                    if (isROXCheck) {
                        mCheckMeltCurveList.add(curveList);
                    }
                }else if(curveList.getCurve_hole().equals("CY5")){
                    if (isCY5Check) {
                        mCheckMeltCurveList.add(curveList);
                    }
                }
            }
        }
        return true;
    }

    private void drawMeltCurve() {
        clearChart();
        drawchart(mCheckMeltCurveList, mFirstLineChart,"MeltCurve");
    }

    private boolean getTemperatures(){
        return true;
    }

    private void drawTemperatureCurve(){
        clearTempChart();
        drawTempChart(mExTemperatures,mTemperatureChart,"Temperature");
    }

    public void clearTempChart(){
        mTemperatureChart.clear();
        mTemperatureChart.notifyDataSetChanged();
        mTemperatureChart.invalidate();
    }

    public void drawTempChart(List<ExTemperature> curve_list, LineChart lineChart, String describe) {
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
            for (int i = 0; i < curve_list.size(); i++) {
                //entries列表
                LineDataSet set1 = new LineDataSet(curve_list.get(i).getBlockTemperatures(), "blockTemperature");
                //设置每个树状图的颜色，一共有两个方法setColors和setColor
                set1.setCircleSize(1f);
                set1.setColor(PcrValue.colorline[1]);
                set1.setCircleColor(PcrValue.colorline[1]);
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

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线
            xAxis.setLabelCount(12, true);
            xAxis.setValueFormatter(new CustomValueFormatter());
            lineChart.invalidate();
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bioway_second_running_step_tv) {
            mRunningStepLayout.setVisibility(View.VISIBLE);
            mRunningChartLayout.setVisibility(View.GONE);
            mStepCheckTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mChartCheckTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            isChartVisible = false;
        } else if (id == R.id.bioway_second_running_curve_tv) {
            mRunningStepLayout.setVisibility(View.GONE);
            mRunningChartLayout.setVisibility(View.VISIBLE);
            mChartCheckTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mStepCheckTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            isChartVisible  = true;
        } else if (id == R.id.bioway_second_pcr_running_hole_one_tv) {
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
        } else if (id == R.id.bioway_second_pcr_running_hole_two_tv) {
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
        } else if (id == R.id.bioway_second_pcr_running_hole_three_tv) {
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
        } else if (id == R.id.bioway_second_pcr_running_hole_four_tv) {
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
        } else if (id == R.id.bioway_second_pcr_running_hole_five_tv) {
            isFiveheck = !isFiveheck;
            if (!isFiveheck) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setHodeState(isAllCheck, mAllHoleCk);
                }
            } else {
                checkAllHole();
            }
            setHodeState(isFiveheck, (TextView) v);
            showCurve();
        } else if (id == R.id.bioway_second_pcr_running_hole_six_tv) {
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
        } else if (id == R.id.bioway_second_pcr_running_hole_seven_tv) {
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
        } else if (id == R.id.bioway_second_pcr_running_hole_eight_tv) {
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
        } else if (id == R.id.bioway_second_pcr_running_all_hole_ck) {
            if (isAllCheck) {
                isAllCheck = false;
                setAllHoleUnCheck();
            } else {
                isAllCheck = true;
                setAllHoleCheck();

            }
            setHodeState(isAllCheck, mAllHoleCk);
            showCurve();
        }
    }

    @Override
    public void onParseRunningFinish() {
        MessageEvent updateRunEx = new MessageEvent();
        updateRunEx.setMsgType(MsgType.MSG_UPDATE_RUNNING_EX);
        EventBus.getDefault().post(updateRunEx);
    }

    @Override
    public void onParseExFinish() {

    }

    private void pasreRunningEx(){
        try {
            if(null != BoWeiService.getInstance()){
                Experiment experiment = BoWeiService.getInstance().getExperimentResult();
                if(null == experiment){
                    return;
                }
                String starttime = experiment.getStartTime();
                mStartTimeTv.setContentText(starttime);
                BiowayManager.getInstance().parseSLANRunningEx(experiment);
            }

        }catch (Exception e){
            e.getMessage();
        }

    }

    public static class UIHandler extends Handler {

        WeakReference<RunBioWaySecondPCRFragment> mFragmentReference;

        public UIHandler(RunBioWaySecondPCRFragment mFragment) {
            mFragmentReference = new WeakReference<RunBioWaySecondPCRFragment>(mFragment);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            final RunBioWaySecondPCRFragment mFragment = mFragmentReference.get();
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
}
