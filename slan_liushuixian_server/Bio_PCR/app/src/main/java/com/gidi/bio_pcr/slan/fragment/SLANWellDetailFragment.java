package com.gidi.bio_pcr.slan.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;


import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.Result;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.databinding.FragmentSlanWellDetailBinding;
import com.gidi.bio_pcr.db.SLANWellResult;
import com.gidi.bio_pcr.service.MyWindowManager;
import com.gidi.bio_pcr.slan.SLANManager;
import com.gidi.bio_pcr.slan.bean.SLANCurveList;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.ScreenShotUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.WarnDialog;
import com.github.mikephil.charting.BuildConfig;
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
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.hongshi.slansdk.model.result.MeltPeak;
import com.hongshi.slansdk.model.result.TargetResult;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;


import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SLANWellDetailFragment extends Fragment {

    private static final String TAG = "SLANWellDetailFragment";

    /** 荧光通道编号（与硬件协议保持一致） */
    private static final int CHANNEL_FAM    = 1;
    private static final int CHANNEL_HEX    = 2;
    private static final int CHANNEL_ROX    = 3;
    private static final int CHANNEL_CY5    = 4;
    private static final int CHANNEL_QUASAR = 5;
    private static final int CHANNEL_ATTO   = 6;

    private static final int MSG_SAVE_PDF = 300;

    /** A4 单页像素宽高（与 slan_well_detail.xml 中 ScrollView 保持一致） */
    private static final int PDF_PAGE_WIDTH  = 1240;
    private static final int PDF_PAGE_HEIGHT = 1754;

    private SLANWellResult slanWellResult;
    private SLANWellDetailViewModel mViewModel;
    private PipeMainViewModel mainViewModel;
    private Observer<SLANWellResult> mResultObserver;

    View rootv;

    private UIHandler mUIHandler;
    private List<SLANCurveList> mAMPCurveList;
    private List<SLANCurveList> mPeakCurveList;
    private LineChart mAMPLineChart;
    private LineChart mPeakLineChart;
    private LineChart mAMPPdfChart;

    private String startTime, testItem, endTime;
    private NoticeDialog noticeDialog;
    public WarnDialog warnDialog;
    private String sampleName, wellNum, conclusion;
    private boolean type;
    private Gson gson;
    private List<String> mTargetNames;
    private List<Double> mTargetCtValues;
    private List<Double> mControlPeakValues;
    private List<String> mRealCtValues;
    private List<String> mConclusions, mPeak_conclusions;
    private List<MeltPeak> mPeakValues = new ArrayList<>();
    /** 预渲染的 PDF 内容缓存（RGB_565，省内存），保存 PDF 时直接复用，避免每次重做 measure/draw */
    private Bitmap mPdfBitmap;
    private final DecimalFormat df = new DecimalFormat("#0.00");
    private boolean isFamThreshold, isHexThreshold;
    private boolean isRoxThreshold = false;
    private boolean isCy5Threshold = false;
    private boolean isQuaThreshold, isAttoThreshold;
    private double famThreshold, hexThreshold, roxThreshold, cy5Threshold, quasarThreshold, attoThreshold;
    private FragmentSlanWellDetailBinding  binding;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSlanWellDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(SLANWellDetailViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);

        initData();
        initListener();

        mResultObserver = new Observer<SLANWellResult>() {
            @Override
            public void onChanged(SLANWellResult result) {
                if (result == null || !isAdded()) {
                    return;
                }
                slanWellResult = result;
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "slanWellResult=" + slanWellResult);
                }
                initResult();
            }
        };
        mainViewModel.getPcrSLANWellResult().observe(requireActivity(), mResultObserver);
    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mainViewModel != null && mResultObserver != null) {
            mainViewModel.getPcrSLANWellResult().removeObserver(mResultObserver);
        }
        mainViewModel = null;
        mResultObserver = null;

        if (mUIHandler != null) {
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        binding = null;
    }

    protected void initListener() {
        if (binding == null) {
            return;
        }
        binding.wellDetailTitleLayout.tabBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mainViewModel != null) {
                    mainViewModel.onNavClicked(ConstantData.NAV_RECORDS);
                }
            }
        });
        binding.screenshotTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                screenShot();
            }
        });
        binding.showPdfTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding == null || binding.wellBiowayDetailPdfLayout == null
                        || binding.wellDetailLayout == null) {
                    return;
                }
                binding.wellBiowayDetailPdfLayout.setVisibility(View.VISIBLE);
                binding.wellDetailLayout.setVisibility(View.GONE);
                showPdfContent();
            }
        });
        binding.wellShowPdfBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding == null || binding.wellBiowayDetailPdfLayout == null
                        || binding.wellDetailLayout == null) {
                    return;
                }
                binding.wellBiowayDetailPdfLayout.setVisibility(View.GONE);
                binding.wellDetailLayout.setVisibility(View.VISIBLE);
            }
        });
        binding.pdfSaveTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNoticeDialog(getString(R.string.well_detail_dialog_message_save_pdf_confirm));
            }
        });
    }

    private void initResult() {
        if (binding == null || slanWellResult == null) {
            return;
        }
        sampleName = slanWellResult.getSampleName();
        wellNum = slanWellResult.getWellName();
        type = slanWellResult.isMelt();
        binding.samplenameTv.setContentText(slanWellResult.getSampleName());
        binding.regentcodeTv.setContentText(slanWellResult.getReagentCode());
        startTime = slanWellResult.getStartTime();
        testItem = slanWellResult.getTestItem();
        endTime = slanWellResult.getEndTime();
        binding.testitemTv.setContentText(testItem);
        binding.testtimeTv.setContentText(startTime);
        parseWellResult(slanWellResult);
        binding.peakContentTv.setText(slanWellResult.getPeakWellConclusion());
        binding.ctContentTv.setText(slanWellResult.getWellConclusion());
    }

    protected void initData() {
        isFamThreshold = isHexThreshold = isRoxThreshold
                = isCy5Threshold = isQuaThreshold = isAttoThreshold = false;
        mUIHandler = new UIHandler(this);
        mAMPCurveList = new ArrayList<>();
        mPeakCurveList = new ArrayList<>();
        mTargetNames = new ArrayList<>();
        mTargetCtValues = new ArrayList<>();
        mControlPeakValues = new ArrayList<>();
        mRealCtValues = new ArrayList<>();
        mConclusions = new ArrayList<>();
        // 设置好图表引用，避免 initResult 阶段空指针
        if (binding != null) {
            mAMPLineChart = binding.resultWellBiowayDetailCurveOneChart;
            mPeakLineChart = binding.resultWellBiowayDetailCurveTwoChart;
            mAMPPdfChart = binding.resultWellBiowayPdfChart;
            binding.wellDetailTitleLayout.tabTitleTv.setText(R.string.result_test_result_detail);
        }
        gson = new GsonBuilder()
                .registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
                    @Override
                    public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                        if (src == null) {
                            return JsonNull.INSTANCE;
                        }
                        // NaN / Infinity 不能直接作为 JSON 数字，序列化为字符串
                        if (src.isNaN() || src.isInfinite()) {
                            return new JsonPrimitive(src.toString());
                        }
                        if (src == src.longValue()) {
                            return new JsonPrimitive(src.longValue());
                        }
                        return new JsonPrimitive(src);
                    }
                })
                .create();
    }

    private void showPdfContent() {
        if (binding == null) {
            return;
        }
        binding.wellBiowayPdfExNumContentTv.setText(testItem);
        binding.biowayPdfExDateContentTv.setText(DateFormatUtil.getDate(startTime));
        binding.biowayPdfExStarttimeContentTv.setText(startTime);
        binding.biowayPdfExEndtimeContentTv.setText(endTime);

        binding.biowayPdfSampleNameTv.setText(sampleName);
        binding.biowayPdfWellnumTv.setText(wellNum);
        binding.biowayPdfTestitemTv.setText(testItem);

        if (type) {
            binding.biowayPdfTestTypeTv.setText(getString(R.string.well_detail_pdf_melt_curve));
            binding.wellBiowayPdfControlPeakCtTitle.setText(getString(R.string.well_detail_pdf_report_target_peak));
            binding.wellBiowayPdfRealCtTitle.setVisibility(View.GONE);
            binding.wellBiowayPdfPeakRmTitle.setVisibility(View.VISIBLE);
            binding.wellBiowayPdfPeakTmTitle.setVisibility(View.VISIBLE);
            binding.wellBiowayPdfRealCtTv.setVisibility(View.GONE);
            binding.wellBiowayPdfRealRmValueTv.setVisibility(View.VISIBLE);
            binding.wellBiowayPdfRealTmValueTv.setVisibility(View.VISIBLE);
            drawchart(mPeakCurveList, mAMPPdfChart, "PeakCurve");
            StringBuilder tmValueStr = new StringBuilder();
            StringBuilder rmValueStr = new StringBuilder();
            if (mPeakValues != null && !mPeakValues.isEmpty()) {
                for (MeltPeak meltPeak : mPeakValues) {
                    tmValueStr.append(df.format(meltPeak.getTm())).append("\n");
                    rmValueStr.append(df.format(meltPeak.getRm())).append("\n");
                }
            }
            binding.wellBiowayPdfRealRmValueTv.setText(rmValueStr.toString());
            binding.wellBiowayPdfRealTmValueTv.setText(tmValueStr.toString());
        } else {
            binding.biowayPdfTestTypeTv.setText(getString(R.string.well_detail_pdf_amp_qualitative));
            binding.wellBiowayPdfControlPeakCtTitle.setText(getString(R.string.well_detail_pdf_report_target_ct));
            binding.wellBiowayPdfRealCtTitle.setVisibility(View.VISIBLE);
            binding.wellBiowayPdfPeakRmTitle.setVisibility(View.GONE);
            binding.wellBiowayPdfPeakTmTitle.setVisibility(View.GONE);
            binding.wellBiowayPdfRealCtTv.setVisibility(View.VISIBLE);
            binding.wellBiowayPdfRealRmValueTv.setVisibility(View.GONE);
            binding.wellBiowayPdfRealTmValueTv.setVisibility(View.GONE);
            drawchart(mAMPCurveList, mAMPPdfChart, "AMPCurve");
            binding.wellBiowayPdfRealCtTv.setText(getRealCtValues());
        }
        binding.wellPdfBiowayConclusionTv.setText(getTargetConclusion());
        binding.biowayPdfTestTargetTypeTv.setText(getTargetNames());
        binding.biowayPdfTestTargetCtTv.setText(getTargetCtValues());

        // 数据填充完成后异步预渲染 Bitmap，保存 PDF 时直接复用，省去 measure/draw 时间
        final ScrollView pdfView = binding.showPdfView;
        pdfView.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bmp = renderPdfBitmap(pdfView);
                if (mPdfBitmap != null && !mPdfBitmap.isRecycled()) {
                    mPdfBitmap.recycle();
                }
                mPdfBitmap = bmp;
            }
        });
    }

    /** 将单行 / 多行列表拼接为字符串（多行用 "\n" 分隔） */
    private static String joinLines(List<?> list, java.util.function.Function<Object, String> mapper) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) builder.append("\n");
            builder.append(mapper.apply(list.get(i)));
        }
        return builder.toString();
    }

    /** 检测目标名（多行） */
    private String getTargetNames() {
        return joinLines(mTargetNames, obj -> (String) obj);
    }

    /** 实际 Ct 值（多行） */
    private String getRealCtValues() {
        return joinLines(mRealCtValues, obj -> (String) obj);
    }

    /** 目标 Ct 值（多行） */
    private String getTargetCtValues() {
        return joinLines(mTargetCtValues, obj -> String.valueOf(obj));
    }

    /** 目标结论（多行） */
    private String getTargetConclusion() {
        return joinLines(mConclusions, obj -> String.valueOf(obj));
    }

    /**
     * 将形如 "[a,b,c]" 的结论字符串展开为 "1.a 2.b 3.c"
     * 注：本方法当前未被本文件调用，保留供外部（如反射 / 后续重构）使用。
     */
    private String getConclusion(String conclusion) {
        if (StringUtil.isEmpty(conclusion) || conclusion.length() < 2) {
            return conclusion == null ? "" : conclusion;
        }
        String conResult = conclusion.substring(1, conclusion.length() - 1);
        if (conResult.isEmpty()) {
            return "";
        }
        String[] conclusions = conResult.split(",");
        if (conclusions == null || conclusions.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (String s : conclusions) {
            builder.append(index).append(".").append(s.replace("\\n", "")).append(" ");
            index++;
        }
        return builder.toString();
    }

    /** 解析单通道 RealCt / PeakValues */
    private void appendRealCtAndPeaks(TargetResult result) {
        if (result == null) return;
        mRealCtValues.add(df.format(result.getCt()));
        List<MeltPeak> peaks = result.getPeakValues();
        if (peaks != null) {
            mPeakValues.addAll(peaks);
        }
    }

    /** 解析单通道 Results JSON -> 目标名 / Ct / 结论 */
    private void appendResultsFromJson(String json) {
        if (StringUtil.isEmpty(json)) return;
        List<Result> resultList = gson.fromJson(json, new TypeToken<List<Result>>(){}.getType());
        if (resultList == null || resultList.isEmpty()) return;
        for (Result result : resultList) {
            mTargetNames.add(result.getCategory());
            mTargetCtValues.add(result.getValue());
            mConclusions.add(result.getConclusion());
        }
    }

    /** 应用单个通道阈值（字符串 -> ���点） */
    private void applyThreshold(String thresholdStr, boolean[] enabledFlag, double[] valueHolder) {
        if (StringUtil.isEmpty(thresholdStr)) return;
        enabledFlag[0] = true;
        valueHolder[0] = (double) StringUtil.convertToFloat(thresholdStr, 0.00f);
    }

    private void parseWellResult(SLANWellResult slanWellResult) {
        // 解析各通道 TargetResult（用于曲线）
        TargetResult famTargetResult = parseTargetResult(slanWellResult.getFamTargetResults());
        TargetResult hexTargetResult = parseTargetResult(slanWellResult.getHexTargetResults());
        TargetResult roxTargetResult = parseTargetResult(slanWellResult.getRoxTargetResults());
        TargetResult cy5TargetResult = parseTargetResult(slanWellResult.getCy5TargetResults());
        TargetResult quasarTargetResult = parseTargetResult(slanWellResult.getQuasarTargetResults());
        TargetResult attoTargetResult = parseTargetResult(slanWellResult.getAttoResults());

        SLANCurveList fam_amp_curve = parseAmpCurve(slanWellResult, famTargetResult);
        SLANCurveList fam_peak_curve = parsePeakCurve(slanWellResult, famTargetResult);
        SLANCurveList hex_amp_curve = parseAmpCurve(slanWellResult, hexTargetResult);
        SLANCurveList hex_peak_curve = parsePeakCurve(slanWellResult, hexTargetResult);
        SLANCurveList rox_amp_curve = parseAmpCurve(slanWellResult, roxTargetResult);
        SLANCurveList rox_peak_curve = parsePeakCurve(slanWellResult, roxTargetResult);
        SLANCurveList cy5_amp_curve = parseAmpCurve(slanWellResult, cy5TargetResult);
        SLANCurveList cy5_peak_curve = parsePeakCurve(slanWellResult, cy5TargetResult);
        SLANCurveList quasar_amp_curve = parseAmpCurve(slanWellResult, quasarTargetResult);
        SLANCurveList quasar_peak_curve = parsePeakCurve(slanWellResult, quasarTargetResult);
        SLANCurveList atto_amp_curve = parseAmpCurve(slanWellResult, attoTargetResult);
        SLANCurveList atto_peak_curve = parsePeakCurve(slanWellResult, attoTargetResult);

        mTargetNames.clear();
        mTargetCtValues.clear();
        mRealCtValues.clear();
        mConclusions.clear();
        mPeakValues.clear();

        appendRealCtAndPeaks(famTargetResult);
        appendRealCtAndPeaks(hexTargetResult);
        appendRealCtAndPeaks(roxTargetResult);
        appendRealCtAndPeaks(cy5TargetResult);
        appendRealCtAndPeaks(quasarTargetResult);
        appendRealCtAndPeaks(attoTargetResult);

        // 阈值（顺序无关，因为写入的是不同字段）
        applyThreshold(slanWellResult.getFamThreshold(),
                new boolean[]{isFamThreshold}, new double[]{famThreshold});
        applyThreshold(slanWellResult.getHexThreshold(),
                new boolean[]{isHexThreshold}, new double[]{hexThreshold});
        applyThreshold(slanWellResult.getRoxThreshold(),
                new boolean[]{isRoxThreshold}, new double[]{roxThreshold});
        applyThreshold(slanWellResult.getCy5Threshold(),
                new boolean[]{isCy5Threshold}, new double[]{cy5Threshold});
        applyThreshold(slanWellResult.getQuasarThreshold(),
                new boolean[]{isQuaThreshold}, new double[]{quasarThreshold});
        applyThreshold(slanWellResult.getAttoThreshold(),
                new boolean[]{isAttoThreshold}, new double[]{attoThreshold});

        // 结果 JSON（用于 PDF 显示）
        appendResultsFromJson(slanWellResult.getFamResults());
        appendResultsFromJson(slanWellResult.getHexResults());
        appendResultsFromJson(slanWellResult.getRoxResults());
        appendResultsFromJson(slanWellResult.getCy5Results());
        appendResultsFromJson(slanWellResult.getQuasarResults());
        appendResultsFromJson(slanWellResult.getAttoResults());

        // 组装曲线列表
        mAMPCurveList.clear();
        mAMPCurveList.add(fam_amp_curve);
        mAMPCurveList.add(hex_amp_curve);
        mAMPCurveList.add(rox_amp_curve);
        mAMPCurveList.add(cy5_amp_curve);
        mAMPCurveList.add(quasar_amp_curve);
        mAMPCurveList.add(atto_amp_curve);

        mPeakCurveList.clear();
        mPeakCurveList.add(fam_peak_curve);
        mPeakCurveList.add(hex_peak_curve);
        mPeakCurveList.add(rox_peak_curve);
        mPeakCurveList.add(cy5_peak_curve);
        mPeakCurveList.add(quasar_peak_curve);
        mPeakCurveList.add(atto_peak_curve);

        // Quasar / Atto 通道
      /*  appendExtraChannel(slanWellResult, slanWellResult.getQuasarTargetResults(),
                mAMPCurveList, mPeakCurveList);
        appendExtraChannel(slanWellResult, slanWellResult.getAttoTargetResults(),
                mAMPCurveList, mPeakCurveList);*/

        drawchart(mAMPCurveList, mAMPLineChart, "AMPCurve");
        drawchart(mPeakCurveList, mPeakLineChart, "PeakCurve");
    }

    private TargetResult parseTargetResult(String json) {
        if (StringUtil.isEmpty(json)) return null;
        return gson.fromJson(json, TargetResult.class);
    }

    private SLANCurveList parseAmpCurve(SLANWellResult result, TargetResult targetResult) {
        return SLANManager.getInstance()
                .parseTargetResultToAMP(result.getWellName(), result.getSampleName(), targetResult);
    }

    private SLANCurveList parsePeakCurve(SLANWellResult result, TargetResult targetResult) {
        return SLANManager.getInstance()
                .parseTargetResultToPeak(result.getWellName(), result.getSampleName(), targetResult);
    }

    private void appendExtraChannel(SLANWellResult result, String targetJson,
                                    List<SLANCurveList> ampList, List<SLANCurveList> peakList) {
        if (StringUtil.isEmpty(targetJson)) return;
        TargetResult targetResult = parseTargetResult(targetJson);
        ampList.add(parseAmpCurve(result, targetResult));
        peakList.add(parsePeakCurve(result, targetResult));
    }

    public void drawchart(List<SLANCurveList> curve_list, LineChart lineChart, String describe) {
        if (curve_list == null) {
            curve_list = new ArrayList<>();
        }
        if (lineChart == null) {
            return;
        }
        Description description = new Description();
        description.setText(describe);
        lineChart.setDescription(description);
        lineChart.setDrawBorders(true);
        lineChart.setBorderColor(Color.BLACK);
        lineChart.setBorderWidth(1f);

        List<ILineDataSet> lineDataSets = new ArrayList<>();
        for (int i = 0; i < curve_list.size(); i++) {
            SLANCurveList curve = curve_list.get(i);
            LineDataSet set1 = new LineDataSet(curve.getCurve_data(), curve.getCurve_hole());
            set1.setCircleSize(1f);

            int myColor = PcrValue.colorline[0];
            int channelNo = curve.getChannelNo();
            if (channelNo >= CHANNEL_FAM && channelNo <= PcrValue.colorline.length) {
                myColor = PcrValue.colorline[channelNo - 1];
            }

            set1.setColor(myColor);
            set1.setCircleColor(myColor);
            set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set1.setDrawValues(false);
            set1.setDrawCircleHole(true);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);
            lineDataSets.add(set1);
        }
        LineData lineData = new LineData(lineDataSets);
        lineData.setValueTextSize(10);
        lineData.setDrawValues(false);
        lineChart.setData(lineData);

        Legend legend = lineChart.getLegend();
        legend.setTextSize(10f);
        legend.setEnabled(false);

        YAxis rightYAxis = lineChart.getAxisRight();
        rightYAxis.setEnabled(false);
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setDrawGridLines(true);

        if (isFamThreshold) {
            leftAxis.addLimitLine(initLimitLine(famThreshold, "famThreshold", 0));
        }
        if (isHexThreshold) {
            leftAxis.addLimitLine(initLimitLine(hexThreshold, "hexThreshold", 1));
        }
        if (isRoxThreshold) {
            leftAxis.addLimitLine(initLimitLine(roxThreshold, "roxThreshold", 2));
        }
        if (isCy5Threshold) {
            leftAxis.addLimitLine(initLimitLine(cy5Threshold, "cy5Threshold", 3));
        }
        if (isQuaThreshold) {
            leftAxis.addLimitLine(initLimitLine(quasarThreshold, "quasarThreshold", 4));
        }
        if (isAttoThreshold) {
            leftAxis.addLimitLine(initLimitLine(attoThreshold, "attoThreshold", 5));
        }

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setLabelCount(curve_list.size(), false);

        lineChart.invalidate();
    }

    private LimitLine initLimitLine(double threshold, String thresholdName, int number) {
        LimitLine limitLine = new LimitLine((float) threshold, thresholdName);
        limitLine.setLineWidth(2f);
        limitLine.setTextSize(10f);
        int safeIndex = (number >= 0 && number < PcrValue.colorline.length)
                ? number : 0;
        limitLine.setLineColor(PcrValue.colorline[safeIndex]);
        limitLine.setTextColor(R.color.blue);
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        limitLine.disableDashedLine();
        return limitLine;
    }

    /**
     * 截屏操作
     */
    private void screenShot() {
        if (!isAdded() || getContext() == null || binding == null
                || binding.wellDetailRootLayout == null) {
            return;
        }
        if (StringUtil.isEmpty(startTime) || StringUtil.isEmpty(testItem)) {
            Toast.makeText(getContext(), R.string.well_detail_screenshot_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        String aftertime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName = aftertime + "_" + testItem;

        File dir = new File(FileUtils.IMAGE_PATH, subFirName);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        Bitmap bitmap = ScreenShotUtil.saveScreenShot(getActivity(), dir);
        if (bitmap == null) {
            Toast.makeText(getContext(), R.string.well_detail_screenshot_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        final ImageView screenshotBmp = new ImageView(getContext());
        screenshotBmp.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        screenshotBmp.setImageBitmap(bitmap);
        binding.wellDetailRootLayout.addView(screenshotBmp);

        ScaleAnimation scaleAnimation = (ScaleAnimation) AnimationUtils
                .loadAnimation(getContext(), R.anim.screen_cut);
        scaleAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                if (binding != null && binding.wellDetailRootLayout != null) {
                    binding.wellDetailRootLayout.removeView(screenshotBmp);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.well_detail_screenshot_done, Toast.LENGTH_LONG).show();
                }
            }
        });
        screenshotBmp.startAnimation(scaleAnimation);
    }

    /**
     * 把 ScrollView 的全部内容渲染成一张完整 Bitmap（RGB_565）。
     * 解决 ScrollView.draw() 只能画可视窗口、导致 PDF 截断的问题。
     * 注意：必须在主线程调用（view.measure/layout 需要主线程）。
     */
    private Bitmap renderPdfBitmap(ScrollView scrollView) {
        if (scrollView == null) {
            return null;
        }
        View child = scrollView.getChildAt(0);   // ScrollView 内唯一的 LinearLayout
        if (child == null) {
            return null;
        }

        int width = scrollView.getWidth();
        if (width <= 0) {
            width = PDF_PAGE_WIDTH;
        }
        // 用 ScrollView 当前宽度强制 measure，让 LinearLayout 算出完整高度
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        child.measure(widthSpec, heightSpec);
        int height = child.getMeasuredHeight();
        if (height <= 0) {
            return null;
        }
        // 强制 layout，让 LineChart 等真正完成布局，否则画出来是空的
        child.layout(0, 0, width, height);

        // 使用 RGB_565，内存占用减半（1240 * 2500 * 2 ≈ 6MB）
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        // 关键：先铺白底。LinearLayout 没设 background，未被子 View 覆盖的间隙区域
// 会露出 Bitmap 默认的黑色，必须先 fill 一层白底
        canvas.drawColor(Color.WHITE);
        child.draw(canvas);
        return bitmap;
    }

    /**
     * 把 Bitmap 按 A4 高度切片成多页写入 PDF，保证内容完整。
     */
    private boolean createPdfFromBitmap(Bitmap bitmap, String filePath) {
        if (bitmap == null || bitmap.isRecycled() || filePath == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return false;
        }
        PdfDocument document = new PdfDocument();
        try {
            int totalHeight = bitmap.getHeight();
            int pageCount = (totalHeight + PDF_PAGE_HEIGHT - 1) / PDF_PAGE_HEIGHT;

            for (int i = 0; i < pageCount; i++) {
                int y = i * PDF_PAGE_HEIGHT;
                int h = Math.min(PDF_PAGE_HEIGHT, totalHeight - y);
                Bitmap pageBitmap = Bitmap.createBitmap(bitmap, 0, y, PDF_PAGE_WIDTH, h);

                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        PDF_PAGE_WIDTH, h, i + 1).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                page.getCanvas().drawBitmap(pageBitmap, 0, 0, null);
                document.finishPage(page);
                pageBitmap.recycle();
            }

            try (FileOutputStream outputStream = new FileOutputStream(new File(filePath))) {
                document.writeTo(outputStream);
            }
            return true;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "createPdf failed", e);
            }
            return false;
        } finally {
            document.close();
        }
    }

    /**
     * 使用 WeakReference 防止 Handler 持有 Fragment 导致内存泄漏。
     */
    private static class UIHandler extends Handler {
        private final WeakReference<SLANWellDetailFragment> mFragmentRef;

        UIHandler(SLANWellDetailFragment fragment) {
            super(Looper.getMainLooper());
            mFragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            SLANWellDetailFragment fragment = mFragmentRef.get();
            if (fragment == null || !fragment.isAdded()) {
                return;
            }
            switch (msg.what) {
                case MSG_SAVE_PDF:
                    boolean success = (boolean) msg.obj;
                    fragment.hideLoadingDialog();
                    if (success) {
                        fragment.showWarnDialog(
                                fragment.getString(R.string.dialog_title),
                                fragment.getString(R.string.well_detail_dialog_message_save_pdf_success));
                    } else {
                        fragment.showWarnDialog(
                                fragment.getString(R.string.dialog_title),
                                fragment.getString(R.string.well_detail_dialog_message_save_pdf_fail));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private LoadingDialog loadingDialog;

    public void showLoadingDialog(String message) {
        if (!isAdded() || getContext() == null) return;
        if (loadingDialog == null) {
            loadingDialog = new LoadingDialog(getContext(), message);
            loadingDialog.setCancelable(false);
            loadingDialog.setCanceledOnTouchOutside(false);
            loadingDialog.show();
        }
    }

    public void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        loadingDialog = null;
    }

    private void saveSlanWellPdfReport() {
        if (!isAdded() || getContext() == null) return;
        if (StringUtil.isEmpty(startTime) || StringUtil.isEmpty(testItem)) {
            Toast.makeText(getContext(), R.string.well_detail_save_pdf_invalid, Toast.LENGTH_LONG).show();
            return;
        }
        String aftertime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName = aftertime + "_" + testItem;

        File dir = new File(FileUtils.RESULT_EXPORT_PATH, subFirName);
        if (!dir.exists() || !dir.isDirectory()) {
            if (!dir.mkdirs() && BuildConfig.DEBUG) {
                Log.w(TAG, "create dir failed: " + dir.getAbsolutePath());
            }
        }
        final String filePath = FileUtils.RESULT_EXPORT_PATH + subFirName
                + File.separator + wellNum + ".pdf";
        final File file = new File(filePath);
        if (file.exists()) {
            // 同名 PDF 已存在：删除旧文件，按当前最新数据重新生成（而不是直接打印旧文件）
            if (!file.delete() && BuildConfig.DEBUG) {
                Log.w(TAG, "delete old pdf failed: " + filePath);
            }
        }

        final ScrollView contentView = binding != null ? binding.showPdfView : null;
        if (contentView == null) {
            return;
        }

        showLoadingDialog(getString(R.string.well_detail_dialog_message_saving_pdf_file));
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 复用 showPdfContent 阶段预渲染好的 Bitmap；没有缓存才现场渲染
                Bitmap bitmap = mPdfBitmap;
                if (bitmap == null || bitmap.isRecycled()) {
                    // 注意 renderPdfBitmap 必须在主线程跑，这里切回主线程 measure 后再回到当前线程写 PDF
                    final Bitmap[] holder = new Bitmap[1];
                    final Object lock = new Object();
                    contentView.post(new Runnable() {
                        @Override
                        public void run() {
                            holder[0] = renderPdfBitmap(contentView);
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                        }
                    });
                    try {
                        synchronized (lock) {
                            if (holder[0] == null) {
                                lock.wait(2000);
                            }
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    bitmap = holder[0];
                }
                final Bitmap finalBitmap = bitmap;
                boolean saveResult = createPdfFromBitmap(finalBitmap, filePath);
                if (mUIHandler != null) {
                    Message message = Message.obtain();
                    message.obj = saveResult;
                    message.what = MSG_SAVE_PDF;
                    mUIHandler.sendMessage(message);
                }
            }
        }).start();
    }

    private void showNoticeDialog(String message) {
        if (!isAdded() || getContext() == null) return;
        if (noticeDialog != null) {
            noticeDialog.dismiss();
            noticeDialog = null;
        }
        noticeDialog = new NoticeDialog(getContext(), R.style.Dialog);
        noticeDialog.setMessage(message);
        noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
            @Override
            public void onConfirmClick() {
                if (noticeDialog != null) {
                    noticeDialog.dismiss();
                    noticeDialog = null;
                }
                saveSlanWellPdfReport();
            }
        });
        noticeDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
            @Override
            public void onCancelClick() {
                if (noticeDialog != null) {
                    noticeDialog.dismiss();
                    noticeDialog = null;
                }
            }
        });
        noticeDialog.show();
    }

    public void showWarnDialog(String title, String message) {
        if (!isAdded() || getContext() == null) return;
        if (warnDialog != null) {
            warnDialog.dismiss();
            warnDialog = null;
        }
        LayoutInflater inflater = (LayoutInflater) getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_common_hint_layout, null);
        TextView hintTxt = (TextView) view.findViewById(R.id.common_dialog_hint_txt);
        hintTxt.setText(message);

        WarnDialog.Builder builder = new WarnDialog.Builder(getContext());
        builder.setContentView(view);
        builder.setTitle(title);
        builder.setSingleButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (warnDialog != null) {
                    warnDialog.dismiss();
                    warnDialog = null;
                }
            }
        });

        warnDialog = builder.create();
        warnDialog.show();
    }
}