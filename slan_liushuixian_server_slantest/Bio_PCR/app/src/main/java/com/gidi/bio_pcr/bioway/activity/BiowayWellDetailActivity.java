package com.gidi.bio_pcr.bioway.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.bean.Result;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.db.BioWayWellResult;
import com.gidi.bio_pcr.service.MyWindowManager;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.ScreenShotUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomTitleText;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.WarnDialog;
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
import com.google.gson.reflect.TypeToken;
import com.winters.boweilibrary.BoWei.ExperimentData.Peak;
import com.winters.boweilibrary.BoWei.ExperimentData.TargetResult;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.winters.boweilibrary.printer.PdfPrintHelper;


import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 宏石孔位详情
 */
public class BiowayWellDetailActivity extends BaseActivity implements View.OnClickListener{

    private UIHandler mUIHandler;
    private RelativeLayout main_root_layout;
    private RelativeLayout mTitleLayout,mPdfLayout,mWellDetailLayout;
    private TextView mTitleTv;
    private TextView mScreenshotTv,mShowPdfTv;
    private ImageView mBackImg;
    private CustomTitleText mSampleNumTv;
    private CustomTitleText mRegentCodeTv;
    private CustomTitleText mTestItemTv;
    private TextView mConclusionTv,mPeakConclusionTv;
    private CustomTitleText mTestTimeTv;
    private List<BiowayCurveList> mAMPCurveList;
    private List<BiowayCurveList> mPeakCurveList;
    private LineChart mAMPLineChart;
    private LineChart mPeakLineChart;
    private LineChart mAMPPdfChart;

    private String startTime, testItem,endTime;
    private ImageView pdfBackImg;
    private TextView mSavePdfTv;
    private NoticeDialog noticeDialog;
    public WarnDialog warnDialog;
    private TextView mPdfSampleNameTv,mPdfWellNumTv,mPdfTestItemTv,mPdfTypeTv,mPdfConclusionTv;
    private TextView mPdfTargetTv,mPdfTargetCtValueTv,mPdfRealCtValueTv,mPdfRealRmValueTv,mPdfRealTmValueTv;
    private TextView mPdfControlTitleTv,mPdfRealCtTitleTv,mPdfRealRmTitleTv,mPdfRealTmTitleTv;
    private TextView mTestDateTv,mStartTimeTv,mEndTimeTv,mExperimentNumTv;
    //样品名称，反应孔
    private String sampleName,wellNum,conclusion;
    private boolean type;
    private Gson gson;
    private List<String> mTargetNames;
    private List<Double> mTargetCtValues;
    private List<Double> mControlPeakValues;

    private List<String> mRealCtValues;
    private List<String> mConclusions,mPeak_conclusions;
    private List<Peak> mPeakValues = new ArrayList<Peak>();
    private DecimalFormat df = new DecimalFormat("#0.00");
    private boolean isFamThreshold = false;
    private boolean isHexThreshold = false;
    private boolean isRoxThreshold = false;
    private boolean isCy5Threshold= false;
    private double famThreshold,hexThreshold,roxThreshold,cy5Threshold;

    private static final int MSG_SAVE_PDF = 300;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_bioway_result_well_detail;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        main_root_layout = (RelativeLayout) findViewById(R.id.bioway_well_root_layout);
        mTitleLayout = (RelativeLayout) findViewById(R.id.result_bioway_detail_title_layout);
        mTitleTv = (TextView) mTitleLayout.findViewById(R.id.tab_title_tv);
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mTitleTv.setText(R.string.result_test_result_detail);
        mSampleNumTv = (CustomTitleText)findViewById(R.id.result_well_bioway_detail_samplename_tv);
        mRegentCodeTv = (CustomTitleText)findViewById(R.id.result_well_bioway_detail_regentcode_tv);
        mTestItemTv = (CustomTitleText)findViewById(R.id.result_well_bioway_detail_testitem_tv) ;
        mConclusionTv = (TextView)findViewById(R.id.result_well_bioway_conclusion_content_tv);
        mPeakConclusionTv = (TextView)findViewById(R.id.result_well_bioway_conclusion_2_content_tv) ;
        mTestTimeTv = (CustomTitleText)findViewById(R.id.result_well_bioway_detail_testtime_tv);
        mAMPLineChart= (LineChart) findViewById(R.id.result_well_bioway_detail_curve_one_chart);
        mPeakLineChart= (LineChart) findViewById(R.id.result_well_bioway_detail_curve_two_chart);
        mScreenshotTv = (TextView) findViewById(R.id.result_well_bioway_screenshot_tv);
        mPdfLayout = (RelativeLayout) findViewById(R.id.well_bioway_detail_pdf_layout);
        mWellDetailLayout = (RelativeLayout)findViewById(R.id.well_bioway_detail_layout) ;
        mAMPPdfChart = (LineChart) findViewById(R.id.result_well_bioway_pdf_chart) ;
        pdfBackImg = (ImageView)findViewById(R.id.well_bioway_show_pdf_back_img) ;
        mShowPdfTv = (TextView) findViewById(R.id.result_well_bioway_show_pdf_tv);
        mSavePdfTv = (TextView) findViewById(R.id.well_bioway_pdf_save_tv);

        mPdfControlTitleTv = (TextView)findViewById(R.id.well_bioway_pdf_control_peak_ct_title);
        mPdfRealCtTitleTv= (TextView)findViewById(R.id.well_bioway_pdf_real_ct_title);
        mPdfRealRmTitleTv= (TextView)findViewById(R.id.well_bioway_pdf_peak_rm_title);
        mPdfRealTmTitleTv= (TextView)findViewById(R.id.well_bioway_pdf_peak_tm_title);
        //实验编号
        mExperimentNumTv = (TextView)findViewById(R.id.well_bioway_pdf_ex_num_content_tv);
        mStartTimeTv = (TextView)findViewById(R.id.bioway_pdf_ex_starttime_content_tv) ;
        mEndTimeTv= (TextView)findViewById(R.id.bioway_pdf_ex_endtime_content_tv);
        mTestDateTv = (TextView)findViewById(R.id.bioway_pdf_ex_date_content_tv);
        mPdfSampleNameTv = (TextView) findViewById(R.id.bioway_pdf_sample_name_tv);
        mPdfTestItemTv = (TextView) findViewById(R.id.bioway_pdf_testitem_tv);
        mPdfTypeTv = (TextView) findViewById(R.id.bioway_pdf_test_type_tv);
        mPdfWellNumTv = (TextView) findViewById(R.id.bioway_pdf_wellnum_tv);
        mPdfTargetTv = (TextView)findViewById(R.id.bioway_pdf_test_target_type_tv);
        mPdfTargetCtValueTv = (TextView)findViewById(R.id.bioway_pdf_test_target_ct_tv);//ctfile里的ct值
        mPdfRealRmValueTv = (TextView)findViewById(R.id.well_bioway_pdf_real_rm_value_tv);
        mPdfRealTmValueTv = (TextView)findViewById(R.id.well_bioway_pdf_real_tm_value_tv);
        mPdfRealCtValueTv  = (TextView)findViewById(R.id.well_bioway_pdf_real_ct_tv);
        mPdfConclusionTv = (TextView) findViewById(R.id.well_pdf_bioway_conclusion_tv);

    }

    @Override
    protected void initListener() {
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mScreenshotTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                screenShot();
            }
        });

        mShowPdfTv.setOnClickListener(this);
        pdfBackImg.setOnClickListener(this);
        mSavePdfTv.setOnClickListener(this);
    }

    @Override
    protected void initData() {
        mUIHandler = new UIHandler(this);
        mAMPCurveList = new ArrayList<BiowayCurveList>();
        mPeakCurveList = new ArrayList<BiowayCurveList>();
        mTargetNames = new ArrayList<String>();
        mTargetCtValues = new ArrayList<Double>();
        mControlPeakValues= new ArrayList<Double>();
        mRealCtValues = new ArrayList<String>();
        mConclusions = new ArrayList<String>();
        gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        //绘制曲线
        BioWayWellResult slanWellResult = (BioWayWellResult) getIntent().getSerializableExtra("item");
        sampleName = slanWellResult.getSampleName();
        //反应孔
        wellNum = slanWellResult.getWellName();
        type = slanWellResult.isMelt();
        mSampleNumTv.setContentText(slanWellResult.getSampleName());
        mRegentCodeTv.setContentText(slanWellResult.getReagentCode());
        startTime = slanWellResult.getStartTime();
        testItem = slanWellResult.getTestItem();
        endTime = slanWellResult.getEndTime();
        mTestItemTv.setContentText(testItem);
        mTestTimeTv.setContentText(startTime);
        parseWellResult(slanWellResult);
        String ct_conclusion = slanWellResult.getWellConclusion();
        String peak_conclusion = slanWellResult.getPeakWellConclusion();
        mPeakConclusionTv.setText(peak_conclusion);
        mConclusionTv.setText(ct_conclusion);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(MyWindowManager.isWindowShowing()){
            MyWindowManager.removeSmallWindow(getApplicationContext());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!MyWindowManager.isWindowShowing()) {
            MyWindowManager.createSmallWindow(getApplicationContext());
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        if(null != mAMPCurveList){
            mAMPCurveList.clear();
            mAMPCurveList = null;
        }

        if(null != mPeakCurveList){
            mPeakCurveList.clear();
            mPeakCurveList = null;
        }
        if(null != mTargetNames){
            mTargetNames.clear();
            mTargetNames = null;
        }

        if(null != mTargetCtValues){
            mTargetCtValues.clear();
            mTargetCtValues = null;
        }
        if(null != mControlPeakValues){
            mControlPeakValues.clear();
            mControlPeakValues = null;
        }
        if(null != mRealCtValues){
            mRealCtValues.clear();
            mRealCtValues = null;
        }
        if(null != mConclusions){
            mConclusions.clear();
            mConclusions = null;
        }
        if(null != mPeak_conclusions){
            mPeak_conclusions.clear();
            mPeak_conclusions = null;
        }
        if(null != mPeakValues){
            mPeakValues.clear();
            mPeakValues = null;
        }

        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
        if(warnDialog != null){
            warnDialog.dismiss();
            warnDialog = null;
        }
        gson = null;
        mAMPLineChart.clear();
        mPeakLineChart.clear();
        mAMPPdfChart.clear();
        if(MyWindowManager.isWindowShowing()){
            MyWindowManager.removeSmallWindow(getApplicationContext());
        }
    }

    private void showPdfContent(){
        mExperimentNumTv.setText(testItem);
        mTestDateTv.setText(DateFormatUtil.getDate(startTime));
        mStartTimeTv.setText(startTime);
        mEndTimeTv.setText(endTime);

        mPdfSampleNameTv.setText(sampleName);
        mPdfWellNumTv.setText(wellNum);
        mPdfTestItemTv.setText(testItem);
        if(type){
            mPdfTypeTv.setText(getResources().getString(R.string.well_detail_pdf_melt_curve));
            mPdfControlTitleTv.setText(getResources().getString(R.string.well_detail_pdf_report_target_peak));
            mPdfRealCtTitleTv.setVisibility(View.GONE);
            mPdfRealRmTitleTv.setVisibility(View.VISIBLE);
            mPdfRealTmTitleTv.setVisibility(View.VISIBLE);
            mPdfRealCtValueTv.setVisibility(View.GONE);
            mPdfRealRmValueTv.setVisibility(View.VISIBLE);
            mPdfRealTmValueTv.setVisibility(View.VISIBLE);
            drawchart(mPeakCurveList, mAMPPdfChart,"PeakCurve");
            StringBuilder tmValueStr = new StringBuilder();
            StringBuilder rmValueStr = new StringBuilder();
            if(null != mPeakValues&& mPeakValues.size()>0){
                for(Peak meltPeak : mPeakValues){
                    tmValueStr.append(df.format(meltPeak.getTm())).append("\n");
                    rmValueStr.append(df.format(meltPeak.getRm())).append("\n");
                }
            }
            mPdfRealRmValueTv.setText(rmValueStr.toString());
            mPdfRealTmValueTv.setText(tmValueStr.toString());
        }else {
            mPdfTypeTv.setText(getResources().getString(R.string.well_detail_pdf_amp_qualitative));
            mPdfControlTitleTv.setText(getResources().getString(R.string.well_detail_pdf_report_target_ct));
            mPdfRealCtTitleTv.setVisibility(View.VISIBLE);
            mPdfRealRmTitleTv.setVisibility(View.GONE);
            mPdfRealTmTitleTv.setVisibility(View.GONE);
            mPdfRealCtValueTv.setVisibility(View.VISIBLE);
            mPdfRealRmValueTv.setVisibility(View.GONE);
            mPdfRealTmValueTv.setVisibility(View.GONE);
            if(!isFamThreshold&& !isHexThreshold && !isRoxThreshold && !isCy5Threshold){
                drawchart(mAMPCurveList, mAMPPdfChart, "AMPCurve");
            } else{
                drawchart(mAMPCurveList, mAMPPdfChart, "AMPCurve");
            }
            mPdfRealCtValueTv.setText(getRealCtValues());
        }
        mPdfConclusionTv.setText(getTargetConclusion());
        mPdfTargetTv.setText(getTargetNames());
        mPdfTargetCtValueTv.setText(getTargetCtValues());

    }

    /**
     * 检测目标
     * @return
     */
    private String getTargetNames(){
        StringBuilder builder = new StringBuilder();
        if(mTargetNames !=null && mTargetNames.size()>0){
            if(mTargetNames.size() == 1){
                for(String s:mTargetNames){
                    builder.append(s);
                }
            }else {
                for(String s:mTargetNames){
                    builder.append(s).append("\n");
                }
            }

            return builder.toString();
        }else{
            return "";
        }
    }

    /**
     * 实际Ct值
     * @return
     */
    private String getRealCtValues(){
        StringBuilder builder = new StringBuilder();
        if(mRealCtValues !=null && mRealCtValues.size()>0){
            if(mRealCtValues.size() == 1){
                for(String s:mRealCtValues){
                    builder.append(s);
                }
            }else {
                for(String s:mRealCtValues){
                    builder.append(s).append("\n");
                }
            }
            return builder.toString();
        }else{
            return "";
        }
    }

    /**
     * 目标Ct值
     * @return
     */
    private String getTargetCtValues(){
        StringBuilder builder = new StringBuilder();
        if(mTargetCtValues !=null && mTargetCtValues.size()>0){
            if(mTargetCtValues.size() == 1){
                for(Double s:mTargetCtValues){
                    builder.append(s+"");
                }
            }else {
                for(Double s:mTargetCtValues){
                    builder.append(s+"").append("\n");
                }
            }
            return builder.toString();
        }else{
            return "";
        }
    }

    private String getTargetConclusion(){
        StringBuilder builder = new StringBuilder();
        if(mConclusions !=null && mConclusions.size()>0){
            if(mConclusions.size() == 1){
                for(String s:mConclusions){
                    builder.append(s+"");
                }
            }else {
                for(String s:mConclusions){
                    builder.append(s+"").append("\n");
                }
            }
            return builder.toString();
        }else{
            return "";
        }
    }


    private String getConclusion(String conclusion){
        if(!StringUtil.isEmpty(conclusion)){
            String con_result = conclusion.substring(1,conclusion.length()-1);
            StringBuilder builder = new StringBuilder();
            if(null != con_result){
                String[] conclusions = con_result.split(",");
                if(null != conclusions && conclusions.length>0){
                    int index = 1;
                    for(String s: conclusions){
                        String s1 =  s.replace("\\n","");
                        builder.append(index).append(".").append(s1).append(" ");
                        index++;
                    }
                }
            }
            return builder.toString();
        }else {
            return conclusion;
        }


    }


    /**
     *
     * @param
     */
    private void parseWellResult(BioWayWellResult biowayWellResult){
        String mROXtargetResults = biowayWellResult.getRoxTargetResults();
        String mHEXtargetResults = biowayWellResult.getHexTargetResults();
        String mFAMtargetResults = biowayWellResult.getFamTargetResults();
        String mCY5targetResults = biowayWellResult.getCy5TargetResults();

        String fam_threshold = biowayWellResult.getFamThreshold();
        String hex_threshold = biowayWellResult.getHexThreshold();
        String rox_threshold = biowayWellResult.getRoxThreshold();
        String cy5_threshold = biowayWellResult.getCy5Threshold();

        TargetResult hexTargetResult = gson.fromJson(mHEXtargetResults, TargetResult.class);
        TargetResult famTargetResult =  gson.fromJson(mFAMtargetResults,TargetResult.class);
        TargetResult roxTargetResult =  gson.fromJson(mROXtargetResults,TargetResult.class);
        TargetResult cy5TargetResult =  gson.fromJson(mCY5targetResults,TargetResult.class);

        BiowayCurveList hex_amp_curve = BiowayManager.getInstance()
                .parseTargetResultToAMP(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),hexTargetResult);
        BiowayCurveList hex_peak_curve = BiowayManager.getInstance()
                .parseTargetResultToPeak(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),hexTargetResult);

        BiowayCurveList fam_amp_curve = BiowayManager.getInstance()
                .parseTargetResultToAMP(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),famTargetResult);
        BiowayCurveList fam_peak_curve = BiowayManager.getInstance()
                .parseTargetResultToPeak(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),famTargetResult);

        BiowayCurveList rox_amp_curve = BiowayManager.getInstance()
                .parseTargetResultToAMP(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),roxTargetResult);
        BiowayCurveList rox_peak_curve = BiowayManager.getInstance()
                .parseTargetResultToPeak(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),roxTargetResult);

        BiowayCurveList cy5_amp_curve = BiowayManager.getInstance()
                .parseTargetResultToAMP(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),cy5TargetResult);
        BiowayCurveList cy5_peak_curve = BiowayManager.getInstance()
                .parseTargetResultToPeak(biowayWellResult.getWellName(),biowayWellResult.getSampleName(),cy5TargetResult);
        mTargetNames.clear();
        mTargetCtValues.clear();
        mRealCtValues.clear();
        mConclusions.clear();
        mPeakValues.clear();

        if(null != famTargetResult){
            mRealCtValues.add(df.format(famTargetResult.getCt()));
            if(null != mPeakValues){
                if(null == famTargetResult.getPeakValues()){
                    ArrayList<Peak> peakValues = new ArrayList<Peak>();
                    mPeakValues.addAll(peakValues);
                }else{
                    mPeakValues.addAll(famTargetResult.getPeakValues());
                }
            }
        }

        if(null != hexTargetResult){
            mRealCtValues.add(df.format(hexTargetResult.getCt()));
            if(null == hexTargetResult.getPeakValues()){
                ArrayList<Peak> peakValues = new ArrayList<Peak>();
                mPeakValues.addAll(peakValues);
            }else{
                mPeakValues.addAll(hexTargetResult.getPeakValues());
            }
        }
        if(null != roxTargetResult){
            mRealCtValues.add(df.format(roxTargetResult.getCt()));
            if(null == roxTargetResult.getPeakValues()){
                ArrayList<Peak> peakValues = new ArrayList<Peak>();
                mPeakValues.addAll(peakValues);
            }else{
                mPeakValues.addAll(roxTargetResult.getPeakValues());
            }
        }
        if(null != cy5TargetResult){
            mRealCtValues.add(df.format(cy5TargetResult.getCt()));
            if(null == cy5TargetResult.getPeakValues()){
                ArrayList<Peak> peakValues = new ArrayList<Peak>();
                mPeakValues.addAll(peakValues);
            }else{
                mPeakValues.addAll(cy5TargetResult.getPeakValues());
            }
        }
        //阈值
        if(!StringUtil.isEmpty(fam_threshold)){
            isFamThreshold = true;
            famThreshold = (double) StringUtil.convertToFloat(fam_threshold,0.00f);
        }
        if(!StringUtil.isEmpty(hex_threshold)){
            isHexThreshold = true;
            hexThreshold = (double) StringUtil.convertToFloat(hex_threshold,0.00f);
        }

        if(!StringUtil.isEmpty(rox_threshold)){
            isRoxThreshold = true;
            roxThreshold = (double) StringUtil.convertToFloat(rox_threshold,0.00f);
        }

        if(!StringUtil.isEmpty(cy5_threshold)){
            isCy5Threshold = true;
            cy5Threshold = (double) StringUtil.convertToFloat(cy5_threshold,0.00f);
        }


        if(!StringUtil.isEmpty(biowayWellResult.getFamResults())){
            List<Result> famResultList = gson.fromJson(biowayWellResult.getFamResults(), new TypeToken<List<Result>>(){}.getType());
            if(null != famResultList && famResultList.size()>0){
                for(Result result :famResultList){
                    mTargetNames.add(result.getCategory());
                    mTargetCtValues.add(result.getValue());
                    mConclusions.add(result.getConclusion());
                }
            }
        }

        if(!StringUtil.isEmpty(biowayWellResult.getHexResults())){
            List<Result> hexResultList = gson.fromJson(biowayWellResult.getHexResults(), new TypeToken<List<Result>>(){}.getType());
            if(null != hexResultList && hexResultList.size()>0){
                for(Result result :hexResultList){
                    mTargetNames.add(result.getCategory());
                    mTargetCtValues.add(result.getValue());
                    mConclusions.add(result.getConclusion());
                }
            }
        }
        if(!StringUtil.isEmpty(biowayWellResult.getRoxResults())){
            List<Result> roxResultList = gson.fromJson(biowayWellResult.getRoxResults(), new TypeToken<List<Result>>(){}.getType());
            if(null != roxResultList && roxResultList.size()>0){
                for(Result result :roxResultList){
                    mTargetNames.add(result.getCategory());
                    mTargetCtValues.add(result.getValue());
                    mConclusions.add(result.getConclusion());
                }
            }
        }

        if(!StringUtil.isEmpty(biowayWellResult.getCy5Results())){
            List<Result> cy5ResultList = gson.fromJson(biowayWellResult.getCy5Results(), new TypeToken<List<Result>>(){}.getType());
            if(null != cy5ResultList && cy5ResultList.size()>0){
                for(Result result :cy5ResultList){
                    mTargetNames.add(result.getCategory());
                    mTargetCtValues.add(result.getValue());
                    mConclusions.add(result.getConclusion());
                }
            }
        }


        mAMPCurveList.clear();
        mAMPCurveList.add(fam_amp_curve);
        mAMPCurveList.add(hex_amp_curve);
        mAMPCurveList.add(rox_amp_curve);
        mAMPCurveList.add(cy5_amp_curve);

        mPeakCurveList.clear();
        mPeakCurveList.add(fam_peak_curve);
        mPeakCurveList.add(hex_peak_curve);
        mPeakCurveList.add(rox_peak_curve);
        mPeakCurveList.add(cy5_peak_curve);

        drawchart(mAMPCurveList,mAMPLineChart,"AMPCurve");
        drawchart(mPeakCurveList, mPeakLineChart,"PeakCurve");
    }


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
                if (-1 != curve_list.get(i).getChannelNo()) {
                    if (curve_list.get(i).getChannelNo() == 1) {
                        my_color = PcrValue.colorline[0];//蓝色
                    } else if (curve_list.get(i).getChannelNo() == 2) {
                        my_color = PcrValue.colorline[1];//绿色
                    } else if (curve_list.get(i).getChannelNo()== 3) {
                        my_color = PcrValue.colorline[2];//黄色
                    } else if (curve_list.get(i).getChannelNo() == 4) {
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

            if(isFamThreshold ){
                LimitLine limitLine = initLimitLine(famThreshold,"famThreshold",0);
                leftAxis.addLimitLine(limitLine);
            }
            if(isHexThreshold ){
                LimitLine limitLine = initLimitLine(hexThreshold,"hexThreshold",1);
                leftAxis.addLimitLine(limitLine);
            }
            if(isRoxThreshold ){
                LimitLine limitLine = initLimitLine(roxThreshold,"roxThreshold",2);
                leftAxis.addLimitLine(limitLine);
            }
            if(isCy5Threshold ){
                LimitLine limitLine = initLimitLine(cy5Threshold,"cy5Threshold",3);
                leftAxis.addLimitLine(limitLine);
            }

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setGranularity(1);//设置X轴每个点之间数据的间隔
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //设置x标签显示的在底部
            xAxis.setDrawGridLines(true);//不画X轴网格线
            xAxis.setLabelCount(curve_list.size(), false);

         /*   lineChart.post(new Runnable() {
                @Override
                public void run() {
                    markView = new LineChartMarkView(SLANExDetailActivity.this.getApplicationContext());
                    lineChart.setMarker(markView);
                }
            });
*/
            lineChart.invalidate();
        }
    }




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

        }
        limitLine.setTextColor(R.color.blue);  //设置限制线上label字体的颜色
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);//标签位置
        limitLine.disableDashedLine();
        return limitLine;
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
            Bitmap bitmap = ScreenShotUtil.saveScreenShot((BiowayWellDetailActivity) this, dir);
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

    private boolean createPdf(View view,String file_path){
        if(android.os.Build.VERSION.SDK_INT>= android.os.Build.VERSION_CODES.KITKAT) {

            FileOutputStream outputStream =null;

            try {
                outputStream = new FileOutputStream(new File(file_path));

                PdfDocument document =new PdfDocument();

                PdfDocument.PageInfo pageInfo =new PdfDocument.PageInfo.Builder(

                        (int)view.getWidth(),(int)view.getWidth(), 1).create();
               /* Log.i("BBB","ps.getHeight()" + ps.getHeight());
                Log.i("BBB","ps.getWidth()" + ps.getWidth());*/
                PdfDocument.Page page = document.startPage(pageInfo);

//此处是与官方的不同之处，官方的view.draw(page.getCanvas()) 无非也是在page返回的canvas对象上绘图，那么我们可以获取SurfaceView对象的Bitmap后再绘制到page的//canvas上即可          

                view.draw(page.getCanvas());

                document.finishPage(page);
                document.writeTo(outputStream);
                document.close();

                return true;
            }catch (Exception e){
                e.getMessage();
                return false;
            }
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.result_well_bioway_show_pdf_tv){
            mPdfLayout.setVisibility(View.VISIBLE);
            mWellDetailLayout.setVisibility(View.GONE);
            showPdfContent();
        }else if(id == R.id.well_bioway_pdf_save_tv){
            showNoticeDialog("确认保存PDF文件?");
        }else if(id == R.id.well_bioway_show_pdf_back_img){
            mPdfLayout.setVisibility(View.GONE);
            mWellDetailLayout.setVisibility(View.VISIBLE);
        }
    }

    public static class UIHandler extends Handler {

        WeakReference<BiowayWellDetailActivity> mActivityReference;

        public UIHandler(BiowayWellDetailActivity mActivity) {
            mActivityReference = new WeakReference<BiowayWellDetailActivity>(mActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final BiowayWellDetailActivity mActivity = mActivityReference.get();
            if (null == mActivity) {
                return;
            }
            switch (msg.what){
                case MSG_SAVE_PDF:
                    boolean result = (boolean) msg.obj;
                    mActivity.hideLoadingDialog();
                    if(result){
                        mActivity.showWarnDialog(mActivity.getResources().getString(R.string.dialog_title)
                                ,mActivity.getResources().getString(R.string.well_detail_dialog_message_save_pdf_success));

                    }else {
                        mActivity.showWarnDialog(mActivity.getResources().getString(R.string.dialog_title)
                                ,mActivity.getResources().getString(R.string.well_detail_dialog_message_save_pdf_fail));
                    }
                    break;
            }
        }
    }


    private void saveSlanWellPdfReport(){
        if(!StringUtil.isEmpty(startTime) && !StringUtil.isEmpty(testItem)) {
            String aftertime = DateFormatUtil.convertDateFormat(startTime);
            String subFirName = aftertime + "_" + testItem;

            File dir = new File(FileUtils.RESULT_EXPORT_PATH, subFirName);
            if (!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs();
            }
            String file_path = FileUtils.RESULT_EXPORT_PATH + subFirName + File.separator + wellNum + ".pdf";
            File file = new File(file_path);
            if(file.exists()){
                PdfPrintHelper.printDocument(file_path,primaryBaseActivity);
                if (!MyWindowManager.isWindowShowing()) {
                    MyWindowManager.createSmallWindow(getApplicationContext());
                }
                return;
            }
            showLoadingDialog(getResources().getString(R.string.well_detail_dialog_message_saving_pdf_file));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ScrollView contentView = (ScrollView) findViewById(R.id.show_pdf_view);
                    boolean save_result = createPdf(contentView,file_path);
                    if(null != mUIHandler){
                        Message message = Message.obtain();
                        message.obj = save_result;
                        message.what = MSG_SAVE_PDF;
                        mUIHandler.sendMessage(message);
                    }

                }
            }).start();
        }else{
            Toast.makeText(getApplicationContext(),"无法保存PDF文件",Toast.LENGTH_LONG).show();
        }

    }


    private void showNoticeDialog(String message){
        saveSlanWellPdfReport();
//        if(null != noticeDialog){
//            noticeDialog.dismiss();
//            noticeDialog =null;
//        }
//        if(null == noticeDialog){
//            noticeDialog = new NoticeDialog(SLANWellDetailActivity.this,R.style.Dialog);
//            noticeDialog.setMessage(message);
//            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
//                @Override
//                public void onConfirmClick() {
//                    noticeDialog.dismiss();
//                    saveSlanWellPdfReport();
//                }
//            });
//
//            noticeDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
//                @Override
//                public void onCancelClick() {
//                    noticeDialog.dismiss();
//                }
//            });
//            noticeDialog.show();
//        }
    }

    public void showWarnDialog(String title, String message){
        if(null != warnDialog){
            warnDialog.dismiss();
            warnDialog = null;
        }
        if(null == warnDialog){
            warnDialog = new WarnDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView)view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);
            WarnDialog.Builder builder = new WarnDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setSingleButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    warnDialog.dismiss();
                    warnDialog = null;
                }
            });

            warnDialog = builder.create();
            warnDialog.show();
        }
    }
}
