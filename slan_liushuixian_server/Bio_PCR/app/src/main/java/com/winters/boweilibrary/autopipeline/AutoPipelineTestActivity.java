package com.winters.boweilibrary.autopipeline;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.AsyncTask.RNAAndPCRAsyncTask;
import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.WintersUtils.entity.procedureEntity.HexDebugEntity;
import com.gidi.bio_pcr.WintersUtils.utils.AssetUtils;
import com.gidi.bio_pcr.WintersUtils.utils.DateUtil;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.WintersUtils.utils.ListDataSaveUtil;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.gidi.bio_pcr.bioway.adapter.RunBiowayResultAdapter;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.activity.ShowConfigActivity;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.pcr.OnExperimentCallback;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomCheckText;
import com.gidi.bio_pcr.view.CustomView;
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
import com.google.gson.reflect.TypeToken;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.hongshi.slansdk.service.SLANService;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.manager.APLManager;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.BoweiTestActivity;
import com.winters.boweilibrary.BoWei.ExperimentData.Enum.SampleType;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.winters.boweilibrary.BoWei.bean.BiowayExlResult;
import com.winters.boweilibrary.BoWei.model.experiment.Well;
import com.winters.boweilibrary.autopipeline.adapter.ItemPropertyAdapter;
import com.winters.boweilibrary.autopipeline.adapter.QRCodeAdapter;
import com.winters.boweilibrary.autopipeline.adapter.WSSConnectionAdapter;
import com.winters.boweilibrary.autopipeline.enumtype.EnumMessageEvent;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_QRCode;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;
import com.winters.boweilibrary.autopipeline.mgr.WSSModuleManager;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.QRCodeItem;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.message.HeartBeatMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRReqMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRResetMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.PCRStopMessage;
import com.winters.boweilibrary.autopipeline.model.message.PCRMessage.SLAN_PCRReqMessage;
import com.winters.boweilibrary.autopipeline.model.message.QueryDeviceMessage;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAReqMessage;
import com.winters.boweilibrary.autopipeline.model.event.QRCodeMessageEvent;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;
import com.winters.boweilibrary.autopipeline.model.event.WSSMessageEvent;
import com.winters.boweilibrary.autopipeline.model.message.RNAMessage.RNAStopMessage;
import com.winters.boweilibrary.autopipeline.model.message.RobotManagerMessage;
import com.winters.boweilibrary.autopipeline.model.message.RobotManagerResultMessage;
import com.winters.boweilibrary.autopipeline.model.message.UpgradeMessage;
import com.winters.boweilibrary.autopipeline.worker.RobotWorkManager;
import com.winters.boweilibrary.autopipeline.Wssclient.SLANPCRWssclient;
import com.winters.boweilibrary.autopipeline.Wssclient.PCRWssclient;
import com.winters.boweilibrary.autopipeline.Wssclient.RNAWssclient;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

public class AutoPipelineTestActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "AutoPipelineTestActivity";

    private Button wss_server;
    private Button slanpcr_wss_client;
    private Button pcr_wss_client;
    private Button rna_wss_client;
    private Button QueryDevicePCR1;
    private Button QueryDevicePCR2;
    private Button QueryDevicePCR3;
    private Button QueryDevicePCR4;
    private Button QueryDevicePCR5;
    private Button QueryDevicePCR6;
    private Button QueryDeviceRNA1;
    private Button QueryDeviceRNA2;


    private Button QueryDeviceSLAN1;
    private Button QueryDeviceSLAN2;
    private Button QueryDeviceSLAN3;
    private Button QueryDeviceSLAN4;
    private Button QueryDeviceSLAN5;
    private Button QueryDeviceSLAN6;

    private Button HeartBeat;

    private Button ExecuteRNA;

    private Button ExecuteReset;

    private Button ExecuteResetForce;

    private Button ExecuteResetExpetion;

    private Button ExecutePCRFirst;

    private Button ExecutePCRSecond;

    private Button ExecuteSLANPCRFirst;

    private Button ExecuteSLANPCRSecond;

    private Button upgradeRNA;

    private Button upgradePCR;

    private Button upgradeRNA1;

    private Button upgradePCR1;

    private Button TestQRCode;

    private ScrollView recevie_data_RobotRunningMessage;

    private ScrollView recevie_data_RobotWorkManagerRunningMessage;

    private TextView selectItem;

    private TextView wss_server_log;

    private ListView listView_wssconnection;

    private ListView listView_QRcode;

    private Spinner Spinner_source;

    private Spinner Spinner_target;

    private Button Robot_Start_get;

    private Button Robot_Start_put;

    private Button Robot_Stop;

    private Button Robot_reset;

    private Button Robot_start;

    private Button Robot_close_rna;
    private Button Robot_open_rna;
    private Button Robot_put_pan;
    private Button Robot_put_cover;
    private Button Robot_put_cover_to_pcr;

    private Button Robot_error_continue;

    private Button finish_btn;

    private Button RobotArm_config;

    private Button StopRNA;

    private Button ResumeRNA;

    private Button StopPCR;

    private Button RestPCR;

    private Button ExecuteOpenDoor;
    private Button ExecuteCloseDoor;
    private Button ExecuteCoolerPan1;
    private Button ExecuteCloseCoolerPan1;

    private Button ExecuteLedOpenPan4;
    private Button ExecuteLedClosePan4;

    private CustomView PAN1_state_img,PAN2_state_img,PAN3_state_img,
            PAN4_state_img,PAN5_state_img,PAN6_state_img,
            PAN7_state_img,PAN8_state_img;

    private CustomView RNA1_state_img,RNA2_state_img;

    private CustomView PCR1_state_img,PCR2_state_img,PCR3_state_img,
            PCR4_state_img,PCR5_state_img,PCR6_state_img;

    private CustomView ARM_state_img;

    private CustomView COVER0_state_img,COVER1_state_img,COVER2_state_img,COVER3_state_img,
            COVER4_state_img,COVER5_state_img,COVER6_state_img;

    private CustomView DOOR_state_img;

    private Context mContext;


    private CustomView robot_state_img;

    private TextView robot_alarm_msg_tv;

    private WSSConnectionAdapter wssConnectionAdapter;

    private QRCodeAdapter qRCodeAdapter;

    private List<QRCodeItem> qRCodeItems = new ArrayList<>();

    private WSSConnectionItem selectWSSConnectionItem;
    private String selectWSSConnectionItemKey = null;


    private List<ItemProperty> itemPropertyList = new ArrayList<>();

    private ItemProperty itemProperty_source;

    private ItemProperty itemProperty_target;

    ///////////////////////////////////////////////显示曲线结果/////////////////////////////////////////////////
    private LineChart resultChart;
    private TextView openFileTv;
    private TextView mOneTv,mTwoTv,mThreeTv,mFourTv,mFiveTv,mSixTv,mSevenTv,mEightTv,mAllHoleTv;
    private CustomCheckText mAllCheckTv,mFamChannelTv,mHexChannelTv,mRoxChannelTv,mCY5ChannelTv;

    private boolean isAllChanelCheck = false;
    private boolean isAllCheck = false;
    private boolean isFAMCheck = false;
    private boolean isHEXCheck = false;
    private boolean isROXCheck = false;
    private boolean isCY5Check = false;
    private RadioGroup mCurveChartGg;
    private boolean isA1Check,isA2Check,isA3Check,isA4Check,
            isA5Check,isA6Check,isA7Check,isA8Check = false;
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
    private int currentView = 1;

    private HorizontalScrollView mResultExlLayout;//表格界面
    private LinearLayout mResultChartLayout;
    private boolean isChartVisible = true;
    private boolean isExcelVisible = false;
    private boolean isSecAysVisible = false;
    //参数设置
    private boolean isMelt = false;
    private RelativeLayout mSecAnalyseLayout;
    private TextView mBasicParamTitleTv,mMeltParamTitleTv,mAmpAlgorithmTv;
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
    private List<ConfigTarget> configTargets;
    private boolean iscy5Threshold,isFamThreshold, isHexThreshold, isRoxThreshold = false;
    private double famThreshold,hexThreshold,roxThreshold,cy5Threshold;

    //显示表格结果
    private ListView mResultListView;
    private RunBiowayResultAdapter resultAdapter;

    //二次分析按钮
    private TextView mSecAnalyseTv;


    private TextView RobotRunningMessageText;

    private TextView RobotWorkManagerRunningMessageText;

    private Gson gson;

    private DecimalFormat nf = new DecimalFormat( "0.00");

    private Handler wssUpdateHandler = new Handler();
    private Runnable wssUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (wssConnectionAdapter != null) {
                wssConnectionAdapter.notifyDataSetChanged();
            }
            wssUpdateHandler.postDelayed(this, 2000); // 每秒刷新一次
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_pipeline_test);

        mContext = this;
        Button closebtn = (Button)this.findViewById(R.id.closebtn);
        closebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        wss_server = (Button)this.findViewById(R.id.wss_server);
        slanpcr_wss_client = (Button)this.findViewById(R.id.slanpcr_wss_client);
        pcr_wss_client = (Button)this.findViewById(R.id.pcr_wss_client);
        rna_wss_client = (Button)this.findViewById(R.id.rna_wss_client);
        QueryDevicePCR1 = (Button)this.findViewById(R.id.QueryDevicePCR1);
        QueryDevicePCR2 = (Button)this.findViewById(R.id.QueryDevicePCR2);
        QueryDevicePCR3 = (Button)this.findViewById(R.id.QueryDevicePCR3);
        QueryDevicePCR4 = (Button)this.findViewById(R.id.QueryDevicePCR4);
        QueryDevicePCR5 = (Button)this.findViewById(R.id.QueryDevicePCR5);
        QueryDevicePCR6 = (Button)this.findViewById(R.id.QueryDevicePCR6);

        QueryDeviceSLAN1 = (Button)this.findViewById(R.id.QueryDeviceSLANPCR1);
        QueryDeviceSLAN2 = (Button)this.findViewById(R.id.QueryDeviceSLANPCR2);
        QueryDeviceSLAN3 = (Button)this.findViewById(R.id.QueryDeviceSLANPCR3);
        QueryDeviceSLAN4 = (Button)this.findViewById(R.id.QueryDeviceSLANPCR4);
        QueryDeviceSLAN5 = (Button)this.findViewById(R.id.QueryDeviceSLANPCR5);
        QueryDeviceSLAN6 = (Button)this.findViewById(R.id.QueryDeviceSLANPCR6);

        QueryDeviceRNA1 = (Button)this.findViewById(R.id.QueryDeviceRNA1);
        QueryDeviceRNA2 = (Button)this.findViewById(R.id.QueryDeviceRNA2);

        HeartBeat = (Button)this.findViewById(R.id.HeartBeat);

        ExecuteRNA  = (Button)this.findViewById(R.id.ExecuteRNA);

        ExecuteReset = (Button)this.findViewById(R.id.ExecuteReset);

        ExecuteResetExpetion = (Button)this.findViewById(R.id.ExecuteResetExpetion);

        ExecuteResetForce = (Button)this.findViewById(R.id.ExecuteResetForce);

        ExecutePCRFirst = (Button)this.findViewById(R.id.ExecutePCRFirst);

        ExecutePCRSecond = (Button)this.findViewById(R.id.ExecutePCRSecond);

        ExecuteSLANPCRFirst = (Button)this.findViewById(R.id.ExecuteSLANPCRFirst);

        ExecuteSLANPCRSecond = (Button)this.findViewById(R.id.ExecuteSLANPCRSecond);

        upgradeRNA = (Button)this.findViewById(R.id.upgradeRNA);

        upgradePCR = (Button)this.findViewById(R.id.upgradePCR);

        upgradeRNA1 = (Button)this.findViewById(R.id.upgradeRNA1);

        upgradePCR1 = (Button)this.findViewById(R.id.upgradePCR1);

        TestQRCode  = (Button)this.findViewById(R.id.TestQRCode);

        Spinner_source = (Spinner)this.findViewById(R.id.Spinner_source);

        Spinner_target = (Spinner)this.findViewById(R.id.Spinner_target);

        Robot_Start_get = (Button)this.findViewById(R.id.Robot_Start_get);
        Robot_Start_put = (Button)this.findViewById(R.id.Robot_Start_put);
        Robot_reset = (Button)this.findViewById(R.id.Robot_reset);
        Robot_Stop = (Button)this.findViewById(R.id.Robot_Stop);
        RobotArm_config  = (Button)this.findViewById(R.id.RobotArm_config);
        Robot_start  = (Button)this.findViewById(R.id.Robot_start);
        finish_btn  = (Button)this.findViewById(R.id.finish_btn);

        Robot_close_rna  = (Button)this.findViewById(R.id.Robot_close_rna);
        Robot_open_rna  = (Button)this.findViewById(R.id.Robot_open_rna);
        Robot_put_pan  = (Button)this.findViewById(R.id.Robot_put_pan);
        Robot_put_cover  = (Button)this.findViewById(R.id.Robot_put_cover);
        Robot_put_cover_to_pcr = (Button)this.findViewById(R.id.Robot_put_cover_to_pcr);


        Robot_error_continue = (Button)this.findViewById(R.id.Robot_error_continue);

        StopRNA = (Button)this.findViewById(R.id.StopRNA);
        ResumeRNA = (Button)this.findViewById(R.id.ResumeRNA);
        StopPCR = (Button)this.findViewById(R.id.StopPCR);
        RestPCR = (Button)this.findViewById(R.id.RestPCR);

        ExecuteOpenDoor = (Button)this.findViewById(R.id.ExecuteOpenDoor);
        ExecuteCloseDoor = (Button)this.findViewById(R.id.ExecuteCloseDoor);
        ExecuteCoolerPan1 = (Button)this.findViewById(R.id.ExecuteCoolerPan1);
        ExecuteCloseCoolerPan1 = (Button)this.findViewById(R.id.ExecuteCloseCoolerPan1);

        ExecuteLedOpenPan4 = (Button)this.findViewById(R.id.ExecuteLedOpenPan4);
        ExecuteLedClosePan4 = (Button)this.findViewById(R.id.ExecuteLedClosePan4);

        PAN1_state_img = (CustomView)findViewById(R.id.PAN1_state_img) ;
        PAN2_state_img = (CustomView)findViewById(R.id.PAN2_state_img) ;
        PAN3_state_img = (CustomView)findViewById(R.id.PAN3_state_img) ;
        PAN4_state_img = (CustomView)findViewById(R.id.PAN4_state_img) ;
        PAN5_state_img = (CustomView)findViewById(R.id.PAN5_state_img) ;
        PAN6_state_img = (CustomView)findViewById(R.id.PAN6_state_img) ;
        PAN7_state_img = (CustomView)findViewById(R.id.PAN7_state_img) ;
        PAN8_state_img = (CustomView)findViewById(R.id.PAN8_state_img) ;

        RNA1_state_img = (CustomView)findViewById(R.id.RNA1_state_img) ;
        RNA2_state_img = (CustomView)findViewById(R.id.RNA2_state_img) ;

        PCR1_state_img = (CustomView)findViewById(R.id.PCR1_state_img) ;
        PCR2_state_img = (CustomView)findViewById(R.id.PCR2_state_img) ;
        PCR3_state_img = (CustomView)findViewById(R.id.PCR3_state_img) ;
        PCR4_state_img = (CustomView)findViewById(R.id.PCR4_state_img) ;
        PCR5_state_img = (CustomView)findViewById(R.id.PCR5_state_img) ;
        PCR6_state_img = (CustomView)findViewById(R.id.PCR6_state_img) ;

        ARM_state_img = (CustomView)findViewById(R.id.ARM_state_img) ;

        COVER0_state_img = (CustomView)findViewById(R.id.COVER0_state_img) ;
        COVER1_state_img = (CustomView)findViewById(R.id.COVER1_state_img) ;
        COVER2_state_img = (CustomView)findViewById(R.id.COVER2_state_img) ;
        COVER3_state_img = (CustomView)findViewById(R.id.COVER3_state_img) ;
        COVER4_state_img = (CustomView)findViewById(R.id.COVER4_state_img) ;
        COVER5_state_img = (CustomView)findViewById(R.id.COVER5_state_img) ;
        COVER6_state_img = (CustomView)findViewById(R.id.COVER6_state_img) ;

        DOOR_state_img = (CustomView)findViewById(R.id.DOOR_state_img) ;


        robot_state_img = (CustomView)findViewById(R.id.robot_state_img) ;

        robot_alarm_msg_tv = (TextView)findViewById(R.id.robot_alarm_msg_tv);

        RobotRunningMessageText = (TextView)findViewById(R.id.RobotRunningMessageText);

        RobotRunningMessageText.setMovementMethod(new ScrollingMovementMethod());

        RobotWorkManagerRunningMessageText = (TextView)findViewById(R.id.RobotWorkManagerRunningMessageText);

        RobotWorkManagerRunningMessageText.setMovementMethod(new ScrollingMovementMethod());


        robot_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        robot_alarm_msg_tv.setText("E3-机械臂断连");

        wss_server.setOnClickListener(this);
        slanpcr_wss_client.setOnClickListener(this);
        pcr_wss_client.setOnClickListener(this);
        rna_wss_client.setOnClickListener(this);
        QueryDevicePCR1.setOnClickListener(this);
        QueryDevicePCR2.setOnClickListener(this);
        QueryDevicePCR3.setOnClickListener(this);
        QueryDevicePCR4.setOnClickListener(this);
        QueryDevicePCR5.setOnClickListener(this);
        QueryDevicePCR6.setOnClickListener(this);
        QueryDeviceRNA1.setOnClickListener(this);
        QueryDeviceRNA2.setOnClickListener(this);

        QueryDeviceSLAN1.setOnClickListener(this);
        QueryDeviceSLAN2.setOnClickListener(this);
        QueryDeviceSLAN3.setOnClickListener(this);
        QueryDeviceSLAN4.setOnClickListener(this);
        QueryDeviceSLAN5.setOnClickListener(this);
        QueryDeviceSLAN6.setOnClickListener(this);

        HeartBeat.setOnClickListener(this);
        ExecuteRNA.setOnClickListener(this);
        ExecuteReset.setOnClickListener(this);
        ExecuteResetExpetion.setOnClickListener(this);

        upgradeRNA.setOnClickListener(this);
        upgradePCR.setOnClickListener(this);

        upgradeRNA1.setOnClickListener(this);
        upgradePCR1.setOnClickListener(this);

        ExecuteResetForce.setOnClickListener(this);

        ExecutePCRFirst.setOnClickListener(this);
        ExecutePCRSecond.setOnClickListener(this);

        ExecuteSLANPCRFirst.setOnClickListener(this);
        ExecuteSLANPCRSecond.setOnClickListener(this);

        TestQRCode.setOnClickListener(this);

        Robot_Start_get.setOnClickListener(this);
        Robot_Start_put.setOnClickListener(this);
        Robot_reset.setOnClickListener(this);
        Robot_Stop.setOnClickListener(this);
        RobotArm_config.setOnClickListener(this);
        Robot_start.setOnClickListener(this);
        finish_btn.setOnClickListener(this);

        Robot_close_rna.setOnClickListener(this);
        Robot_open_rna.setOnClickListener(this);
        Robot_put_pan.setOnClickListener(this);
        Robot_put_cover.setOnClickListener(this);
        Robot_put_cover_to_pcr.setOnClickListener(this);

        Robot_error_continue.setOnClickListener(this);

        StopPCR.setOnClickListener(this);
        StopRNA.setOnClickListener(this);
        ResumeRNA.setOnClickListener(this);
        RestPCR.setOnClickListener(this);

        ExecuteOpenDoor.setOnClickListener(this);;
        ExecuteCloseDoor.setOnClickListener(this);;
        ExecuteCoolerPan1.setOnClickListener(this);;
        ExecuteCloseCoolerPan1.setOnClickListener(this);;
        ExecuteLedOpenPan4.setOnClickListener(this);;
        ExecuteLedClosePan4.setOnClickListener(this);;



        recevie_data_RobotRunningMessage  =  (ScrollView)this.findViewById(R.id.recevie_data_RobotRunningMessage);
        recevie_data_RobotRunningMessage.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recevie_data_RobotRunningMessage.post(new Runnable() {
                    public void run() {
                        recevie_data_RobotRunningMessage.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        recevie_data_RobotWorkManagerRunningMessage  =  (ScrollView)this.findViewById(R.id.recevie_data_RobotWorkManagerRunningMessage);
        recevie_data_RobotWorkManagerRunningMessage.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recevie_data_RobotWorkManagerRunningMessage.post(new Runnable() {
                    public void run() {
                        recevie_data_RobotWorkManagerRunningMessage.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });


        selectItem = (TextView)this.findViewById(R.id.selectItem);

        wss_server_log = (TextView)this.findViewById(R.id.wss_server_log);

        listView_wssconnection = (ListView)this.findViewById(R.id.listView_wssconnection);

        wssConnectionAdapter = new WSSConnectionAdapter(WSSModuleManager.getInstance().getWssConnectionItems(),mContext);

        listView_wssconnection.setAdapter(wssConnectionAdapter);

        // 启动定时刷新
        wssUpdateHandler.postDelayed(wssUpdateRunnable, 1000);

        registerEventBus();

        listView_wssconnection.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                WSSConnectionAdapter adapter = (WSSConnectionAdapter) listView_wssconnection.getAdapter();

                selectWSSConnectionItem = (WSSConnectionItem) adapter.getItem(position);

                selectWSSConnectionItemKey = selectWSSConnectionItem.getDeviceID();

                selectItem.setText(selectWSSConnectionItemKey);

                return false;
            }
        });


        listView_QRcode = (ListView)this.findViewById(R.id.listView_QRcode);

        qRCodeAdapter = new QRCodeAdapter(qRCodeItems,AutoPipelineTestActivity.this);

        listView_QRcode.setAdapter(qRCodeAdapter);


//        itemPropertyList.add(new ItemProperty("PAN", 1,1, 1, 1));
//        itemPropertyList.add(new ItemProperty("PAN", 2,2, 2, 2));
//        itemPropertyList.add(new ItemProperty("PAN", 3,3, 3, 3));
//        itemPropertyList.add(new ItemProperty("PAN", 4,4, 4, 4));
//        itemPropertyList.add(new ItemProperty("PAN", 5,5, 5, 5));
//        itemPropertyList.add(new ItemProperty("PAN", 6,6, 6, 6));
//        itemPropertyList.add(new ItemProperty("PAN", 7,7, 7, 7));
//        itemPropertyList.add(new ItemProperty("PAN", 8,8, 8, 8));
//
//        itemPropertyList.add(new ItemProperty("RNA", 1,1, 1, 1));
//        itemPropertyList.add(new ItemProperty("RNA", 2,2, 2, 2));
//
//        itemPropertyList.add(new ItemProperty("PCR", 1,1, 1, 1));
//        itemPropertyList.add(new ItemProperty("PCR", 2,2, 2, 2));
//        itemPropertyList.add(new ItemProperty("PCR", 3,3, 3, 3));
//        itemPropertyList.add(new ItemProperty("PCR", 4,4, 4, 4));
//        itemPropertyList.add(new ItemProperty("PCR", 5,5, 5, 5));
//        itemPropertyList.add(new ItemProperty("PCR", 6,6, 6, 6));
//
//
//        itemPropertyList.add(new ItemProperty("ROBOT_FREE", 1,0, 0, 0));
//
//        String itemPropertyListText = GsonUtil.toJson(itemPropertyList, true);
//
//        boolean result = FileUtils.saveFileInSdcard(itemPropertyListText,Environment.getExternalStorageDirectory().getAbsolutePath()
//                + "/PCR/","module",".itemproperty");
//        if(result){
//            Toast.makeText(getApplicationContext(),"保存位置信息文件成功",Toast.LENGTH_LONG).show();
//
//        }else {
//            Toast.makeText(getApplicationContext(),"保存位置信息文件失败",Toast.LENGTH_LONG).show();
//        }
//        try {
//            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
//                    + "/PCR/" + "module_old.itemproperty");
//
//            if(!file.exists()){
//
//                AssetUtils.copyFromAsset(AutoPipelineTestActivity.this, "module_old.itemproperty", true);
//            }
//
//            FileInputStream inputStream = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath()
//                    + "/PCR/" + "module_old.itemproperty");
//
//            String strJson = FileUtils.fileToStr(inputStream);
//
//            itemPropertyList = GsonUtil.fromJson(strJson,new TypeToken<List<ItemProperty>>() {});
//
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }


        itemPropertyList = SerialPortManager_Robot.getInstance().getItemPropertyList();

        ItemPropertyAdapter itemPropertyAdapter =
                new ItemPropertyAdapter(itemPropertyList, AutoPipelineTestActivity.this);

        Spinner_source.setAdapter(itemPropertyAdapter);

        Spinner_target.setAdapter(itemPropertyAdapter);

        Spinner_source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                itemProperty_source = (ItemProperty) itemPropertyAdapter.getItem(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                itemProperty_source = null;
            }
        });

        Spinner_target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                itemProperty_target = (ItemProperty) itemPropertyAdapter.getItem(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                itemProperty_target = null;
            }
        });





        ///显示结果
        resultChart = (LineChart)this.findViewById(R.id.bowei_running_result_slan_curve_chart) ;

        mOneTv = (TextView)this.findViewById(R.id.bowei_result_slan_hole_one_btn) ;
        mTwoTv = (TextView)this.findViewById(R.id.bowei_result_slan_hole_two_btn) ;
        mThreeTv = (TextView)this.findViewById(R.id.bowei_result_slan_hole_three_btn) ;
        mFourTv = (TextView)this.findViewById(R.id.bowei_result_slan_hole_four_btn) ;
        mFiveTv = (TextView)this.findViewById(R.id.bowei_result_slan_hole_five_btn) ;
        mSixTv = (TextView)this.findViewById(R.id.bowei_result_slan_hole_six_btn) ;
        mSevenTv = (TextView)this.findViewById(R.id.bowei_result_slan_hole_seven_btn) ;
        mEightTv= (TextView)this.findViewById(R.id.bowei_result_slan_hole_eight_btn) ;
        mAllHoleTv = (TextView)this.findViewById(R.id.bowei_result_slan_running_all_hole_ck) ;
        mAllCheckTv = (CustomCheckText)this.findViewById(R.id.bowei_result_slan_all_channel_check_tv);
        mFamChannelTv = (CustomCheckText)this.findViewById(R.id.bowei_running_slan_result_fam_tv) ;
        mHexChannelTv = (CustomCheckText)this.findViewById(R.id.bowei_running_slan_result_hex_tv) ;
        mRoxChannelTv= (CustomCheckText)this.findViewById(R.id.bowei_running_slan_result_rox_tv) ;
        mCY5ChannelTv= (CustomCheckText)this.findViewById(R.id.bowei_running_slan_result_cy5_tv) ;
        initChartViewListener();


        RobotWorkManager.getInstance().init(AutoPipelineTestActivity.this);



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止定时刷新，防止 Activity 销毁后继续刷新
        wssUpdateHandler.removeCallbacks(wssUpdateRunnable);
        unRegisterEventBus();
     //   WSSModuleManager.stopWSSserver();
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        QueryDeviceMessage queryDeviceMessage = new QueryDeviceMessage();
        queryDeviceMessage.setType("QueryDevice");

        if (id == R.id.wss_server) {
            boolean result = WSSModuleManager.getInstance().startWSSserver();
//            if(result){
//                Toast.makeText(mContext,"startWSSserver succeed",Toast.LENGTH_LONG).show();
//            }else{
//                Toast.makeText(mContext,"startWSSserver error",Toast.LENGTH_LONG).show();
//            }
        }if (id == R.id.slanpcr_wss_client) {
            SLANPCRWssclient wssClient = SLANPCRWssclient.getInstance();
            if(wssClient != null){
                wss_server_log.setText("start SLANPCRWssclient succeed");
            }else{
                wss_server_log.setText("start SLANPCRWssclient error");
            }
        }if (id == R.id.pcr_wss_client) {
            PCRWssclient wssClient = PCRWssclient.getInstance();
            if(wssClient != null){
                wss_server_log.setText("start PCRWssclient succeed");
            }else{
                wss_server_log.setText("start PCRWssclient error");
            }
        }if (id == R.id.rna_wss_client) {
            RNAWssclient wssClient = RNAWssclient.getInstance();
            if(wssClient != null){
                wss_server_log.setText("start RNAWssclient succeed");
            }else{
                wss_server_log.setText("start RNAWssclient error");
            }
        }else if (id == R.id.QueryDevicePCR1) {

            queryDeviceMessage.setSeqId(1);
            queryDeviceMessage.setPosition(1);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("PCR" + "1111");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDevicePCR2) {
            queryDeviceMessage.setSeqId(2);
            queryDeviceMessage.setPosition(2);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("PCR" + "2222");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDevicePCR3) {
            queryDeviceMessage.setSeqId(3);
            queryDeviceMessage.setPosition(3);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("PCR" + "3333");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的PCR块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDevicePCR4) {
            queryDeviceMessage.setSeqId(4);
            queryDeviceMessage.setPosition(4);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("PCR" + "4444");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDevicePCR5) {
            queryDeviceMessage.setSeqId(5);
            queryDeviceMessage.setPosition(5);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("PCR" + "5555");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDevicePCR6) {
            queryDeviceMessage.setSeqId(6);
            queryDeviceMessage.setPosition(6);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("PCR" + "6666");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDeviceSLANPCR1) {

            queryDeviceMessage.setSeqId(1);
            queryDeviceMessage.setPosition(1);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("SLANPCR" + "1111");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDeviceSLANPCR2) {

            queryDeviceMessage.setSeqId(1);
            queryDeviceMessage.setPosition(2);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("SLANPCR" + "2222");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDeviceSLANPCR3) {

            queryDeviceMessage.setSeqId(1);
            queryDeviceMessage.setPosition(3);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("SLANPCR" + "3333");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDeviceSLANPCR4) {

            queryDeviceMessage.setSeqId(1);
            queryDeviceMessage.setPosition(4);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("SLANPCR" + "4444");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDeviceSLANPCR5) {

            queryDeviceMessage.setSeqId(1);
            queryDeviceMessage.setPosition(5);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("SLANPCR" + "5555");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDeviceSLANPCR6) {

            queryDeviceMessage.setSeqId(1);
            queryDeviceMessage.setPosition(6);
            queryDeviceMessage.setDeviceType("PCR");
            queryDeviceMessage.setDeviceID("SLANPCR" + "6666");
            queryDeviceMessage.setCommand("setPCR_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDeviceRNA1) {
            queryDeviceMessage.setSeqId(1);
            queryDeviceMessage.setPosition(1);
            queryDeviceMessage.setDeviceType("RNA");
            queryDeviceMessage.setDeviceID("RNA" + "1111");
            queryDeviceMessage.setCommand("setRNA_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.QueryDeviceRNA2) {
            queryDeviceMessage.setSeqId(2);
            queryDeviceMessage.setPosition(2);
            queryDeviceMessage.setDeviceType("RNA");
            queryDeviceMessage.setDeviceID("RNA" + "2222");
            queryDeviceMessage.setCommand("setRNA_position");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送模板的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,queryDeviceMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"QueryDevice error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.HeartBeat) {
            HeartBeatMessage heartBeatMessage = new HeartBeatMessage();
            heartBeatMessage.setSeqId(2);
//            heartBeatMessage.setDeviceID("master");
//            heartBeatMessage.setDeviceType("master");
            heartBeatMessage.setType("HeartBeat");
            heartBeatMessage.setTime(DateUtil.getTime());
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送消息的模块",Toast.LENGTH_LONG).show();
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,heartBeatMessage);
            if(!result.contains("error")){
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"heartBeatMessage succeed",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(mContext,"sendWSSMessage:" + selectWSSConnectionItemKey + "--" +"heartBeatMessage error",Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.ExecuteRNA) {
            RNAReqMessage rNAReqMessage = new RNAReqMessage();
            rNAReqMessage.setSeqId(111);
            rNAReqMessage.setBatchID(123+"");
            rNAReqMessage.setTotalTime(1000);
//            rNAReqMessage.setDeviceID("master");
//            rNAReqMessage.setDeviceType("master");
            rNAReqMessage.setType("RNAReq");
            rNAReqMessage.setTemplate("13~xinguan~first.hex");
            rNAReqMessage.setCurrentPhase(0);
            rNAReqMessage.setCurrentPhaseStep(0);

            try {
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/" + "first.hex";

                List<HexDebugEntity> HexDebugEntityList =
                        ListDataSaveUtil.getHexDebugEntityList(new File(filePath));
                if (filePath.contains("first") || filePath.contains("second")){
                    String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "97-downandopen.hex";
                    File downandopenfile = new File(downandopen_filePath);
                    List<HexDebugEntity> HexDebugEntityList_downandopen = ListDataSaveUtil.getHexDebugEntityList(downandopenfile);
                    HexDebugEntityList.addAll(HexDebugEntityList_downandopen);
                }

                rNAReqMessage.setWorkTemplate(HexDebugEntityList);

                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的RNA模块",Toast.LENGTH_LONG).show();
                }
                String result = WSSModuleManager.getInstance().sendRNATemplateWSSMessage(selectWSSConnectionItemKey,rNAReqMessage);
                Toast.makeText(mContext, "sendRNATemplateWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.ResumeRNA) {
            RNAReqMessage rNAReqMessage = new RNAReqMessage();
            rNAReqMessage.setSeqId(111);
            rNAReqMessage.setBatchID(123+"");
            rNAReqMessage.setTotalTime(1000);
            rNAReqMessage.setType("RNAReq");
            rNAReqMessage.setTemplate("13~xinguan~first.hex");
            rNAReqMessage.setForce(1);

            try {
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/" + "first.hex";

                List<HexDebugEntity> HexDebugEntityList =
                        ListDataSaveUtil.getHexDebugEntityList(new File(filePath));
                if (filePath.contains("first") || filePath.contains("second")){
                    String downandopen_filePath = FileUtils.COMMON_HEX_PATH + "97-downandopen.hex";
                    File downandopenfile = new File(downandopen_filePath);
                    List<HexDebugEntity> HexDebugEntityList_downandopen = ListDataSaveUtil.getHexDebugEntityList(downandopenfile);
                    HexDebugEntityList.addAll(HexDebugEntityList_downandopen);
                }

                rNAReqMessage.setWorkTemplate(HexDebugEntityList);

                WSSConnectionItem wSSConnectionItem = WSSModuleManager.getInstance().getWssConnectionItems().get(selectWSSConnectionItemKey);

                if(wSSConnectionItem == null){
                    Toast.makeText(mContext, "请选择要发送模板的RNA模块",Toast.LENGTH_LONG).show();
                    return;
                }

                if(wSSConnectionItem instanceof RNAStatusItem == false){
                    Toast.makeText(mContext, "请选择RNA模块",Toast.LENGTH_LONG).show();
                    return;

                }

                String status = ((RNAStatusItem)wSSConnectionItem).getrNAStatus();

                String lastRunningInfo = ((RNAStatusItem)wSSConnectionItem).getNO();

                if(status.equals("free") || status.equals("ok")|| status.equals("running")){
                    Toast.makeText(mContext, "当前模块状态为"+ status + "，不能恢复运行",Toast.LENGTH_LONG).show();
                    return;

                }else{
                    Log.i("ResumeRNA","lastRunningInfo" + lastRunningInfo);
                    if(lastRunningInfo!=null){
                        String[] info = lastRunningInfo.split("\\|",-1);
                        if(info != null && info.length>=5){
                            String rna_interuppt_time = info[4];
                            rNAReqMessage.setCurrentPhase(Integer.parseInt(info[1]));
                            rNAReqMessage.setCurrentPhaseStep(Integer.parseInt(info[2]));
                            Log.i("ResumeRNA","ResumeRNA" + rna_interuppt_time + "--" + info[1] + "--" + info[2]);

                        }else if(info != null && info.length>=4){
                            String rna_interuppt_time = "";
                            rNAReqMessage.setCurrentPhase(Integer.parseInt(info[1]));
                            rNAReqMessage.setCurrentPhaseStep(Integer.parseInt(info[2]));
                            Log.i("ResumeRNA","ResumeRNA" + rna_interuppt_time + "--" + info[1] + "--" + info[2]);
                        }
                    }
                }

                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的RNA模块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendRNATemplateWSSMessage(selectWSSConnectionItemKey,rNAReqMessage);
                Toast.makeText(mContext, "sendRNATemplateWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.ExecuteReset) {
            RNAReqMessage rNAReqMessage = new RNAReqMessage();
            rNAReqMessage.setSeqId(111);
            rNAReqMessage.setBatchID(124+"");
            rNAReqMessage.setTotalTime(100);
            //rNAReqMessage.setDeviceID("master");
            //rNAReqMessage.setDeviceType("master");
            rNAReqMessage.setType("RNAReq");
            rNAReqMessage.setTemplate("99~reset~reset.hex");
            //rNAReqMessage.setTemplate("99");
            rNAReqMessage.setCurrentPhase(0);
            rNAReqMessage.setCurrentPhaseStep(0);

            try {
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()
                        + "/PCR/commonHex_v5/" + "99-reset.hex";

                List<HexDebugEntity> HexDebugEntityList =
                        ListDataSaveUtil.getHexDebugEntityList(new File(filePath));

                rNAReqMessage.setWorkTemplate(HexDebugEntityList);

                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的RNA块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendRNATemplateWSSMessage(selectWSSConnectionItemKey,rNAReqMessage);
                Toast.makeText(mContext, "sendRNATemplateWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.ExecuteResetExpetion) {

            String result = WSSModuleManager.getInstance().sendRNAResetWSSMessage(selectWSSConnectionItemKey,0);
            Toast.makeText(mContext, "sendRNATemplateWSSMessage-----" +  result,Toast.LENGTH_LONG).show();
            //wssConnectionAdapter.notifyDataSetChanged();

        }else if (id == R.id.ExecuteResetForce) {

            String result = WSSModuleManager.getInstance().sendRNAResetWSSMessage(selectWSSConnectionItemKey,1);
            Toast.makeText(mContext, "sendRNATemplateWSSMessage-----" +  result,Toast.LENGTH_LONG).show();
            //wssConnectionAdapter.notifyDataSetChanged();

        }else if (id == R.id.TestQRCode) {

            SerialPortManager_QRCode.getInstance().sendStart();

        }else if (id == R.id.ExecutePCRFirst) {
            PCRReqMessage pCRReqMessage = new PCRReqMessage();
            pCRReqMessage.setSeqId(111);
            pCRReqMessage.setBatchID(1234  + "");
//            pCRReqMessage.setDeviceID("master");
//            pCRReqMessage.setDeviceType("master");
            pCRReqMessage.setType("PCRReq");
//            pCRReqMessage.setPosition(1);
            pCRReqMessage.setTemplate("13~新冠~first.prj");
            pCRReqMessage.setCurrentPhase(0);
            pCRReqMessage.setCurrentCycle(0);
            pCRReqMessage.setCurrentSection("");
            pCRReqMessage.setCurrentStep("");
            pCRReqMessage.setRunningTime(0);
            pCRReqMessage.setTotalTime(1000);

            ArrayList<Project> mConfigProjects = PcrApplication.slanService.getProjects(Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/PCR/");//获取目录中的项目

            Program program = null;

            Project projectSend = null;

            if(null != mConfigProjects && mConfigProjects.size()>0) {
                for (Project project : mConfigProjects) {
                    if (null != project) {
                        if (project.getName().contains("first")) {

                            program = project.getProgram();

                            projectSend = project;

                        }
                    }
                }
            }

            if(program == null){
                Toast.makeText(mContext, "first.prj 不存在",Toast.LENGTH_LONG).show();
                return;
            }

            try {

                Experiment.getInstance().setProject(projectSend);

                ArrayList<Well> wells = new ArrayList<>();

                for (int i = 1; i <= 8; i++) {
                    Well well = new Well();
                    //孔名称
                    well.setName("A" + (i));
                    //项目名称
                    well.setProjectName("first.prj");
                    //患者信息.唯一标示
                    well.setID("A" + (i));
                    //姓名
                    well.setPatientName("PatientName");
                    //性别
                    well.setSex("Sex");
                    //年龄
                    well.setAge("Age");
                    //标本类型
                    well.setSpecimen("Specimen");
                    //采样日期
                    well.setSpecimenDate("SpecimenDate");
                    //送检医生
                    well.setDoctor("Doctor");
                    //送检科室
                    well.setOffice("Office");
                    //诊断
                    well.setDiagnosis("Diagnosis");
                    //病历号
                    well.setCaseID("caseID");
                    //病床号
                    well.setBedID("BedID");
                    //住院号
                    well.setInPatientID("InPatientID");
                    //门诊号
                    well.setOutPatientID("OutPatientID");
                    //备注
                    well.setRemark("Remark");
                    //样本名称
                    well.setSampleName("A" + (i));//身份證號碼
                    //试管编号
                    well.setTubeNo(i);//编号
                    //试管名称
                    well.setTubeName("A" + (i));
                    //样本类型
                    well.setType(SampleType.Unknown.getValue());
                    //病历号
                    well.setCaseID("A" + (i));
                    //属性
                    well.setProperty("Property");
                    wells.add(well);
                }

                Experiment.getInstance().setExperimentWells(wells);

                Experiment.getInstance().setAuditor("winters");
                Experiment.getInstance().setBatchNo("20251231");
                Experiment.getInstance().setNote("test");
                Experiment.getInstance().setOperator("winters");
                Experiment.getInstance().setName("test");
                Experiment.getInstance().setCustomColumns(new ArrayList<String>());
                Experiment.getInstance().setPlateNo("test");

                Experiment.getInstance().setExperimentWells(wells);

                pCRReqMessage.setExperiment(Experiment.getInstance());

                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的PCR模块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendPCRTemplateWSSMessage(selectWSSConnectionItemKey,pCRReqMessage);
                Toast.makeText(mContext, "sendRNATemplateWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.ExecutePCRSecond) {
            PCRReqMessage pCRReqMessage = new PCRReqMessage();
            pCRReqMessage.setBatchID(1234 + "");
            pCRReqMessage.setSeqId(111);
//            pCRReqMessage.setDeviceID("master");
//            pCRReqMessage.setDeviceType("master");
            pCRReqMessage.setType("PCRReq");
//            pCRReqMessage.setPosition(2);
            pCRReqMessage.setTemplate("13~新冠~second.prj");
            pCRReqMessage.setCurrentPhase(0);
            pCRReqMessage.setCurrentCycle(0);
            pCRReqMessage.setCurrentSection("");
            pCRReqMessage.setCurrentStep("");
            pCRReqMessage.setRunningTime(0);
            pCRReqMessage.setTotalTime(1000);

            ArrayList<Project> mConfigProjects = PcrApplication.slanService.getProjects(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/PCR/");//获取目录中的项目

            Program program = null;

            Project projectSend = null;

            if(null != mConfigProjects && mConfigProjects.size()>0) {
                for (Project project : mConfigProjects) {
                    if (null != project) {
                        if (project.getName().contains("second")) {

                            program = project.getProgram();

                            projectSend = project;


                        }
                    }
                }
            }

            if(program == null){
                Toast.makeText(mContext, "second.prj 不存在",Toast.LENGTH_LONG).show();
                return;
            }


            try {

                Experiment.getInstance().setProject(projectSend);

                ArrayList<Well> wells = new ArrayList<>();

                for (int i = 1; i <= 8; i++) {
                    Well well = new Well();
                    //孔名称
                    well.setName("A" + (i));
                    //项目名称
                    well.setProjectName("first.prj");
                    //患者信息.唯一标示
                    well.setID("A" + (i));
                    //姓名
                    well.setPatientName("PatientName");
                    //性别
                    well.setSex("Sex");
                    //年龄
                    well.setAge("Age");
                    //标本类型
                    well.setSpecimen("Specimen");
                    //采样日期
                    well.setSpecimenDate("SpecimenDate");
                    //送检医生
                    well.setDoctor("Doctor");
                    //送检科室
                    well.setOffice("Office");
                    //诊断
                    well.setDiagnosis("Diagnosis");
                    //病历号
                    well.setCaseID("caseID");
                    //病床号
                    well.setBedID("BedID");
                    //住院号
                    well.setInPatientID("InPatientID");
                    //门诊号
                    well.setOutPatientID("OutPatientID");
                    //备注
                    well.setRemark("Remark");
                    //样本名称
                    well.setSampleName("A" + (i));//身份證號碼
                    //试管编号
                    well.setTubeNo(i);//编号
                    //试管名称
                    well.setTubeName("A" + (i));
                    //样本类型
                    well.setType(SampleType.Unknown.getValue());
                    //病历号
                    well.setCaseID("A" + (i));
                    //属性
                    well.setProperty("Property");
                    wells.add(well);
                }

                Experiment.getInstance().setExperimentWells(wells);

                Experiment.getInstance().setAuditor("winters");
                Experiment.getInstance().setBatchNo("20251231");
                Experiment.getInstance().setNote("test");
                Experiment.getInstance().setOperator("winters");
                Experiment.getInstance().setName("test");
                Experiment.getInstance().setCustomColumns(new ArrayList<String>());
                Experiment.getInstance().setPlateNo("test");

                Experiment.getInstance().setExperimentWells(wells);

                pCRReqMessage.setExperiment(Experiment.getInstance());

                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的PCR模块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendPCRTemplateWSSMessage(selectWSSConnectionItemKey,pCRReqMessage);
                Toast.makeText(mContext, "sendRNATemplateWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.i(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.ExecuteSLANPCRFirst) {
            SLAN_PCRReqMessage pCRReqMessage = new SLAN_PCRReqMessage();
            pCRReqMessage.setSeqId(111);
            pCRReqMessage.setBatchID(1234 + "");
            pCRReqMessage.setDeviceID("SLANPCR111");
            pCRReqMessage.setDeviceType("PCR");
            pCRReqMessage.setType("SLANPCRReq");
            pCRReqMessage.setPosition(1);
            pCRReqMessage.setTemplate("13~~first.prj");
            pCRReqMessage.setCurrentPhase(0);
            pCRReqMessage.setCurrentCycle(0);
            pCRReqMessage.setCurrentSection("");
            pCRReqMessage.setCurrentStep("");
            pCRReqMessage.setRunningTime(0);
            pCRReqMessage.setTotalTime(1000);

            ArrayList<Project> mConfigProjects = PcrApplication.slanService.getProjects(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/PCR/");//获取目录中的项目

            Program program = null;

            Project projectSend = null;

            if(null != mConfigProjects && mConfigProjects.size()>0) {
                for (Project project : mConfigProjects) {
                    if (null != project) {
                        if (project.getName().contains("first")) {

                            program = project.getProgram();

                            projectSend = project;

                        }
                    }
                }
            }

            if(program == null){
                Toast.makeText(mContext, "first.prj 不存在",Toast.LENGTH_LONG).show();
                return;
            }

         /*   try {*/

            Experiment.getInstance().setProject(projectSend);
            ArrayList<Well> wells = new ArrayList<>();
            // A、B两行，每行8孔，共16孔
            char[] rows = {'A', 'B'};
            int tubeIndex = 1;
            for (char row : rows) {
                for (int col = 1; col <= 8; col++) {
                    String wellCode = row + String.valueOf(col);
                    Well well = new Well();
                    //孔名称
                    well.setName(wellCode);
                    //项目名称
                    well.setProjectName("first");
                    //患者信息.唯一标示
                    well.setID(wellCode);
                    //姓名
                    well.setPatientName("PatientName");
                    //性别
                    well.setSex("Sex");
                    //年龄
                    well.setAge("Age");
                    //标本类型
                    well.setSpecimen("Specimen");
                    //采样日期
                    well.setSpecimenDate("SpecimenDate");
                    //送检医生
                    well.setDoctor("Doctor");
                    //送检科室
                    well.setOffice("Office");
                    //诊断
                    well.setDiagnosis("Diagnosis");
                    //病历号
                    well.setCaseID(wellCode);
                    //病床号
                    well.setBedID("BedID");
                    //住院号
                    well.setInPatientID("InPatientID");
                    //门诊号
                    well.setOutPatientID("OutPatientID");
                    //备注
                    well.setRemark("Remark");
                    //样本名称
                    well.setSampleName(wellCode);//身份证号码
                    //试管编号 连续1~16
                    well.setTubeNo(1);
                    //试管名称
                    well.setTubeName(wellCode);
                    //样本类型
                    well.setType(SampleType.Unknown.getValue());
                    //属性
                    well.setProperty("Property");
                    wells.add(well);
                }
            }

                Experiment.getInstance().setExperimentWells(wells);

                Experiment.getInstance().setAuditor("winters");
                Experiment.getInstance().setBatchNo("20251231");
                Experiment.getInstance().setNote("test");
                Experiment.getInstance().setOperator("winters");
                Experiment.getInstance().setName("test");
                Experiment.getInstance().setCustomColumns(new ArrayList<String>());
                Experiment.getInstance().setPlateNo("test");

                Experiment.getInstance().setExperimentWells(wells);

                pCRReqMessage.setExperiment(Experiment.getInstance());

                pCRReqMessage.setWells(wells);

                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的PCR模块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendSLAN_PCRTemplateWSSMessage(
                        selectWSSConnectionItemKey,
                        pCRReqMessage,
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/PCR/first.prj");
                Log.e("slanpcr","==result=="+ result);
                Toast.makeText(mContext, "sendSLANPCRTemplateWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            /*} catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Log.e("slan_pcr", e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }*/

        }else if (id == R.id.ExecuteSLANPCRSecond) {
            SLAN_PCRReqMessage pCRReqMessage = new SLAN_PCRReqMessage();
            pCRReqMessage.setSeqId(111);
            pCRReqMessage.setBatchID(1234 + "");
//            pCRReqMessage.setDeviceID("master");
//            pCRReqMessage.setDeviceType("master");
            pCRReqMessage.setType("SLANPCRReq");
//            pCRReqMessage.setPosition(1);
            pCRReqMessage.setTemplate("13~~second~second.prj");
            pCRReqMessage.setCurrentPhase(0);
            pCRReqMessage.setCurrentCycle(0);
            pCRReqMessage.setCurrentSection("");
            pCRReqMessage.setCurrentStep("");
            pCRReqMessage.setRunningTime(0);
            pCRReqMessage.setTotalTime(1000);

            ArrayList<Project> mConfigProjects = PcrApplication.slanService.getProjects(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/PCR/");//获取目录中的项目

            Program program = null;
            Project projectSend = null;

            if (null != mConfigProjects && mConfigProjects.size() > 0) {
                for (Project project : mConfigProjects) {
                    if (null != project) {
                        if (project.getName().contains("second")) {
                            program = project.getProgram();
                            projectSend = project;
                        }
                    }
                }
            }

            if (program == null) {
                Toast.makeText(mContext, "second.prj 不存在", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                Experiment.getInstance().setProject(projectSend);
                ArrayList<Well> wells = new ArrayList<>();
                // A、B两行，每行8孔，共16孔
                char[] rows = {'A', 'B'};
                int tubeIndex = 1;
                for (char row : rows) {
                    for (int col = 1; col <= 8; col++) {
                        String wellCode = row + String.valueOf(col);
                        Well well = new Well();
                        //孔名称
                        well.setName(wellCode);
                        //项目名称
                        well.setProjectName("second");
                        //患者信息.唯一标示
                        well.setID(wellCode);
                        //姓名
                        well.setPatientName("PatientName");
                        //性别
                        well.setSex("Sex");
                        //年龄
                        well.setAge("Age");
                        //标本类型
                        well.setSpecimen("Specimen");
                        //采样日期
                        well.setSpecimenDate("SpecimenDate");
                        //送检医生
                        well.setDoctor("Doctor");
                        //送检科室
                        well.setOffice("Office");
                        //诊断
                        well.setDiagnosis("Diagnosis");
                        //病历号
                        well.setCaseID(wellCode);
                        //病床号
                        well.setBedID("BedID");
                        //住院号
                        well.setInPatientID("InPatientID");
                        //门诊号
                        well.setOutPatientID("OutPatientID");
                        //备注
                        well.setRemark("Remark");
                        //样本名称
                        well.setSampleName(wellCode);//身份证号码
                        //试管编号 连续1~16
                        well.setTubeNo(1);
                        //试管名称
                        well.setTubeName(wellCode);
                        //样本类型
                        well.setType(SampleType.Unknown.getValue());
                        //属性
                        well.setProperty("Property");
                        wells.add(well);
                    }
                }

                Experiment.getInstance().setExperimentWells(wells);
                Experiment.getInstance().setAuditor("winters");
                Experiment.getInstance().setBatchNo("20251231");
                Experiment.getInstance().setNote("test");
                Experiment.getInstance().setOperator("winters");
                Experiment.getInstance().setName("test");
                Experiment.getInstance().setCustomColumns(new ArrayList<String>());
                Experiment.getInstance().setPlateNo("test");
                Experiment.getInstance().setExperimentWells(wells);

                pCRReqMessage.setExperiment(Experiment.getInstance());
                pCRReqMessage.setWells(wells);

                if (selectWSSConnectionItemKey == null) {
                    Toast.makeText(mContext, "请选择要发送模板的PCR模块", Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendSLAN_PCRTemplateWSSMessage(
                        selectWSSConnectionItemKey,
                        pCRReqMessage,
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/PCR/second.prj");
                Toast.makeText(mContext, "sendSLANPCRTemplateWSSMessage" + result, Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.i(TAG, e.getMessage(), e); // 打印完整异常堆栈，方便排错
                Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.upgradeRNA) {
            String filename = "rna_app_rna_01_00_18_20251009.tar.gz";
            UpgradeMessage upgradeMessage = new UpgradeMessage();
            upgradeMessage.setForce(1);
            upgradeMessage.setSeqId(111);
            upgradeMessage.setType("Upgrade");
            upgradeMessage.setVersion(filename.replace("rna_app_","").replace(".tar.gz",""));
            try {
                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的RNA模块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendRNAUpgradeWSSMessage(
                        selectWSSConnectionItemKey,
                        upgradeMessage,
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/PCR/" + filename);
                Toast.makeText(mContext, "sendRNAUpgradeWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        } else if (id == R.id.upgradeRNA1) {
            String filename = "rna_app_rna_01_00_19_20251009.tar.gz";
            UpgradeMessage upgradeMessage = new UpgradeMessage();
            upgradeMessage.setForce(1);
            upgradeMessage.setSeqId(111);
            upgradeMessage.setType("Upgrade");
            upgradeMessage.setVersion(filename.replace("rna_app_","").replace(".tar.gz",""));
            try {
                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的RNA模块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendRNAUpgradeWSSMessage(
                        selectWSSConnectionItemKey,
                        upgradeMessage,
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/PCR/" + filename);
                Toast.makeText(mContext, "sendRNAUpgradeWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        } else if (id == R.id.upgradePCR) {
            String filename = "pcr_app_pcr_01_00_18_20251009.tar.gz";
            UpgradeMessage upgradeMessage = new UpgradeMessage();
            upgradeMessage.setSeqId(111);
            upgradeMessage.setForce(1);
            upgradeMessage.setType("Upgrade");
            upgradeMessage.setVersion(filename.replace("pcr_app_","").replace(".tar.gz",""));
            try {
                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的PCR模块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendPCRUpgradeWSSMessage(
                        selectWSSConnectionItemKey,
                        upgradeMessage,
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/pcr/"+ filename);
                Toast.makeText(mContext, "sendPCRUpgradeWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.upgradePCR1) {
            String filename = "pcr_app_pcr_01_00_19_20251009.tar.gz";
            UpgradeMessage upgradeMessage = new UpgradeMessage();
            upgradeMessage.setSeqId(111);
            upgradeMessage.setForce(1);
            upgradeMessage.setType("Upgrade");
            upgradeMessage.setVersion(filename.replace("pcr_app_","").replace(".tar.gz",""));
            try {
                if(selectWSSConnectionItemKey == null){
                    Toast.makeText(mContext, "请选择要发送模板的PCR模块",Toast.LENGTH_LONG).show();
                    return;
                }
                String result = WSSModuleManager.getInstance().sendPCRUpgradeWSSMessage(
                        selectWSSConnectionItemKey,
                        upgradeMessage,
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/pcr/"+ filename);
                Toast.makeText(mContext, "sendPCRUpgradeWSSMessage" +  result,Toast.LENGTH_LONG).show();
                //wssConnectionAdapter.notifyDataSetChanged();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext,e.getMessage(),Toast.LENGTH_LONG).show();
            }

        } else if (id == R.id.Robot_Start_get) {
            if(itemProperty_source == null ){
                Toast.makeText(mContext,"请选择源位置或者目标位置",Toast.LENGTH_LONG).show();
            }else{
                SerialPortManager_Robot.getInstance().robotGet(itemProperty_source);
            }


        }else if (id == R.id.Robot_Start_put) {
            if(itemProperty_target == null){
                Toast.makeText(mContext,"请选择源位置或者目标位置",Toast.LENGTH_LONG).show();
            }else{
                SerialPortManager_Robot.getInstance().robotPut(itemProperty_target);
            }

        }else if (id == R.id.Robot_close_rna) {
            if(itemProperty_target == null ){
                Toast.makeText(mContext,"请选择源位置或者目标位置",Toast.LENGTH_LONG).show();
            }if(!itemProperty_target.getType().equals("RNA")){
                Toast.makeText(mContext,"请选择目标关门RNA",Toast.LENGTH_LONG).show();
            }else{
                boolean result = RobotWorkManager.getInstance().startSingleAcion(null,itemProperty_target,"close_rna_pan");

                if(!result){
                    Log.i(TAG, "startSingleAcion close_rna_pan: error");
                    Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
                }
            }

        }else if (id == R.id.Robot_open_rna) {
            if(itemProperty_target == null ){
                Toast.makeText(mContext,"请选择源位置或者目标位置",Toast.LENGTH_LONG).show();
            }
            if(!itemProperty_target.getType().equals("RNA")){
                Toast.makeText(mContext,"请选择目标开门RNA",Toast.LENGTH_LONG).show();
            }else{
                boolean result = RobotWorkManager.getInstance().startSingleAcion(null,itemProperty_target,"open_rna_pan");

                if(!result){
                    Log.i(TAG, "startSingleAcion open_rna_pan : error");
                    Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
                }
            }

        }else if (id == R.id.Robot_put_pan) {
            if(itemProperty_target == null){
                Toast.makeText(mContext,"请选择目标位置",Toast.LENGTH_LONG).show();
            }
            if(!itemProperty_target.getType().equals("PAN")){
                Toast.makeText(mContext,"请选择目标放回盘位",Toast.LENGTH_LONG).show();
            }else{
                boolean result = RobotWorkManager.getInstance().startSingleAcion(null,itemProperty_target,"put_to_pan");
                if(!result){
                    Log.i(TAG, "startSingleAcion put_to_pan : error");
                    Toast.makeText(mContext, "startSingleAcion : error",Toast.LENGTH_LONG).show();
                }
            }

        }else if (id == R.id.Robot_put_cover) {
            if(itemProperty_source == null ){
                Toast.makeText(mContext,"请选择源位置",Toast.LENGTH_LONG).show();
                return;
            }
            if(itemProperty_target == null){
                Toast.makeText(mContext,"目标位置",Toast.LENGTH_LONG).show();
                return;
            }
            if(!itemProperty_source.getType().equals("PCR_COVER") || !itemProperty_target.getType().equals("PCR_COVER")){
                Toast.makeText(mContext,"请选择源位置为PCR临时盖子位置，目标位置PCR盖子位置",Toast.LENGTH_LONG).show();
                return;
            }

            boolean result = RobotWorkManager.getInstance().startSingleAcion(itemProperty_source, itemProperty_target,"put_pcr_cover_from_temp_to_pcr");

            if(!result){
                Log.i(TAG, "startSingleAcion put_pcr_cover_from_temp_to_pcr : error");
                Toast.makeText(mContext, "startSingleAcion  put_pcr_cover_from_temp_to_pcr : error",Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.Robot_put_cover_to_pcr) {
            if(itemProperty_target == null){
                Toast.makeText(mContext,"目标位置",Toast.LENGTH_LONG).show();
                return;
            }
            if(!itemProperty_target.getType().equals("PCR_COVER")){
                Toast.makeText(mContext,"请选目标位置PCR盖子位置",Toast.LENGTH_LONG).show();
                return;
            }

            boolean result = RobotWorkManager.getInstance().startSingleAcion(null, itemProperty_target,"put_pcr_cover_to_pcr");

            if(!result){
                Log.i(TAG, "startSingleAcion put_pcr_cover_to_pcr : error");
                Toast.makeText(mContext, "startSingleAcion  put_pcr_cover_to_pcr : error",Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.Robot_Stop) {
            RobotWorkManager.getInstance().cancelAllWork();
            SerialPortManager_Robot.getInstance().robotStop();
        }else if (id == R.id.Robot_reset) {
            SerialPortManager_Robot.getInstance().robotReset();
        }else if (id == R.id.finish_btn) {
            SerialPortManager_Robot.getInstance().finishCMD();
        }else if (id == R.id.Robot_start) {
            if(itemProperty_source == null ){
                Toast.makeText(mContext,"请选择源位置或者目标位置",Toast.LENGTH_LONG).show();
                return;
            }
            if(itemProperty_target == null){
                Toast.makeText(mContext,"请选择源位置或者目标位置",Toast.LENGTH_LONG).show();
                return;
            }
            if(itemProperty_source.getType().equals("PAN")&&itemProperty_target.getType().equals("PAN")){
                Toast.makeText(mContext,"请选择源和目标，机械臂中只能为PAN及PCR或者PAN及RNA组合",Toast.LENGTH_LONG).show();
                return;
            }
            if(itemProperty_source.getType().equals("PCR")&&itemProperty_target.getType().equals("PCR")){
                Toast.makeText(mContext,"请选择源和目标，机械臂中只能为PAN及PCR或者PAN及RNA组合",Toast.LENGTH_LONG).show();
                return;
            }
            if(itemProperty_source.getType().equals("RNA")&&itemProperty_target.getType().equals("RNA")){
                Toast.makeText(mContext,"请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合",Toast.LENGTH_LONG).show();
                return;
            }

            if(itemProperty_source.getType().equals("PCR_COVER")||itemProperty_target.getType().equals("PCR_COVER")){
                Toast.makeText(mContext,"请选择源和目标，其中只能为PAN及PCR或者PAN及RNA组合",Toast.LENGTH_LONG).show();
                return;
            }
            int panNum = 0;
            if(itemProperty_source.getType().equals("PAN")){
                panNum = itemProperty_source.getPosition();
            }else if(itemProperty_target.getType().equals("PAN")){
                panNum = itemProperty_target.getPosition();
            }else{
                panNum = 1; ///临时保存，需要记录pan的number
            }
            boolean result = RobotWorkManager.getInstance().startRobotWork(itemProperty_source, itemProperty_target,qRCodeItems,panNum);

            if(!result){
                Log.i(TAG, "startRobotWork : error");
                Toast.makeText(mContext, "startRobotWork : error",Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.Robot_error_continue) {

            //RobotWorkManager.getInstance().resumeRobotWork(ItemProperty itemProperty_source ,ItemProperty itemProperty_target, List<QRCodeItem> qRCodeItemsTargrt,int panNum, String resumeText )

            if(lastAction == null ||lastAction.equals("")){
                Toast.makeText(mContext, "resumeRobotWork : error lastAction = null",Toast.LENGTH_LONG).show();
                return;
            }

            String[] actions = lastAction.split(":");

            ItemProperty itemProperty_temp_source = null;

            ItemProperty itemProperty_temp_target = null;

            if(actions[1]!= null){
                for(ItemProperty itemProperty : itemPropertyList){
                    if((itemProperty.getType() + itemProperty.getPosition()).equals(actions[1])){
                        itemProperty_temp_source = itemProperty;
                    }
                }
            }

            if(actions[2]!= null){
                for(ItemProperty itemProperty : itemPropertyList){
                    if((itemProperty.getType() + itemProperty.getPosition()).equals(actions[2])){
                        itemProperty_temp_target = itemProperty;
                    }
                }
            }

            Log.i(TAG, "resumeRobotWork : actions[1] " + actions[1] );
            Log.i(TAG, "resumeRobotWork : actions[2] " + actions[2] );

            if(itemProperty_temp_source == null || itemProperty_temp_target == null){
                Toast.makeText(mContext, "resumeRobotWork : error",Toast.LENGTH_LONG).show();
            }

            int panNum = 0;
            if(itemProperty_source.getType().equals("PAN")){
                panNum = itemProperty_source.getPosition();
            }else if(itemProperty_target.getType().equals("PAN")){
                panNum = itemProperty_target.getPosition();
            }else{
                panNum = itemProperty_target.getPosition();
            }

            boolean result = RobotWorkManager.getInstance().resumeRobotWork(itemProperty_temp_source, itemProperty_temp_target, qRCodeItems, panNum, lastAction);

            if(!result){
                Log.i(TAG, "resumeRobotWork : error");
                Toast.makeText(mContext, "resumeRobotWork : error",Toast.LENGTH_LONG).show();
            }

        }else if (id == R.id.RobotArm_config) {
            SerialPortManager_Robot.getInstance().robotArmConfig();
        }else if (id == R.id.StopRNA) {
            RNAStopMessage rNAStopMessage = new RNAStopMessage();
            rNAStopMessage.setSeqId(111);
            rNAStopMessage.setType("RNAStop");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送的RNA模块",Toast.LENGTH_LONG).show();
                return;
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,rNAStopMessage);
            Toast.makeText(mContext, "StopRNA" +  result,Toast.LENGTH_LONG).show();
            //wssConnectionAdapter.notifyDataSetChanged();

        }else if (id == R.id.StopPCR) {
            PCRStopMessage pCRStopMessage = new PCRStopMessage();
            pCRStopMessage.setSeqId(111);
            pCRStopMessage.setType("PCRStop");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送的PCR模块",Toast.LENGTH_LONG).show();
                return;
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,pCRStopMessage);
            Toast.makeText(mContext, "StopPCR--" +  result,Toast.LENGTH_LONG).show();
            //wssConnectionAdapter.notifyDataSetChanged();
        }else if (id == R.id.RestPCR) {
            PCRResetMessage pcrResetMessage = new PCRResetMessage();
            pcrResetMessage.setSeqId(111);
            pcrResetMessage.setType("PCRReset");
            if(selectWSSConnectionItemKey == null){
                Toast.makeText(mContext, "请选择要发送的PCR模块",Toast.LENGTH_LONG).show();
                return;
            }
            String result = WSSModuleManager.getInstance().sendWSSMessage(selectWSSConnectionItemKey,pcrResetMessage);
            Toast.makeText(mContext, "ResetPCR--" +  result,Toast.LENGTH_LONG).show();
        }else if (id == R.id.ExecuteOpenDoor) {
            try {
                boolean result = SerialPortManager_Robot.getInstance().operateDoor(1);
                Toast.makeText(mContext, "ExecuteOpenDoor:" + result, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext, "ExecuteOpenDoor:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.ExecuteCloseDoor) {
            try {
                boolean result = SerialPortManager_Robot.getInstance().operateDoor(0);
                Toast.makeText(mContext, "ExecuteCloseDoor:" + result, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext, "ExecuteCloseDoor:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.ExecuteCoolerPan1) {
            try {
                boolean result = SerialPortManager_Robot.getInstance().setCooler(1, 10);
                Toast.makeText(mContext, "setCooler:" + result, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext, "setCooler:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.ExecuteLedOpenPan4) {
            try {
                boolean result = SerialPortManager_Robot.getInstance().setLed(4, 1);
                Toast.makeText(mContext, "ExecuteLedOpenPan4:" + result, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext, "ExecuteLedOpenPan4:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }else if (id == R.id.ExecuteLedClosePan4) {
            try {
                boolean result = SerialPortManager_Robot.getInstance().setLed(4, 0);
                Toast.makeText(mContext, "ExecuteLedClosePan4:" + result, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(mContext, "ExecuteLedClosePan4:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(QRCodeMessageEvent qRCodeMessageEvent) {

        String text = qRCodeMessageEvent.getQRCodeMessage();

        qRCodeItems.clear();

        String regex = "#P#|#D#"; // 按照 "#P#" 和 "#D#" 分割

        // 使用正则表达式分割字符串
        String[] parts = text.split(regex);

        // 输出分割结果
        int i = 1;
        for (String part : parts) {
            if (!part.isEmpty()) { // 过滤空字符串
                // (438,140)0100614141999996901234567890
                // 使用正则表达式提取括号内的数字和后续的字符串
                String regex1 = "\\((\\d+),(\\d+)\\)(\\d+)";
                Pattern pattern = Pattern.compile(regex1);
                Matcher matcher = pattern.matcher(part);

                if (matcher.find()) {
                    String x = matcher.group(1); // 提取第一个数字 438
                    String y = matcher.group(2); // 提取第二个数字 140
                    String qrcode = matcher.group(3); // 提取后续的字符串 0100614141999996901234567890

                    QRCodeItem qRCodeItem = new QRCodeItem();

                    qRCodeItem.setIndex(i++);
                    qRCodeItem.setX_coordinate(NumberUtils.StringtoInt(x));
                    qRCodeItem.setY_coordinate(NumberUtils.StringtoInt(y));
                    qRCodeItem.setQrcode(qrcode);
                    qRCodeItems.add(qRCodeItem);

                    Log.i(TAG,"Part 1: " + x);
                    Log.i(TAG,"Part 2: " + y);
                    Log.i(TAG,"Part 3: " + qrcode);
                }
            }
        }

        qRCodeAdapter.notifyDataSetChanged();


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(WSSMessageEvent messageEvent) {


        if(messageEvent.getMsgType().equals(EnumMessageEvent.WSS_ServerStart)){
            wss_server_log.setText((String)messageEvent.getData());
        }if(messageEvent.getMsgType().equals(EnumMessageEvent.WSS_Error)){
            wss_server_log.setText((String)messageEvent.getData());
        }else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_UpdateConnection)){
            Log.i(TAG, "updateConnection:" + (String)messageEvent.getData());

            //wssConnectionAdapter.notifyDataSetChanged();

        }else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_UpdateRNAStatus)){

            Log.i(TAG, "WSSManager_UpdateRNAStatus:" + messageEvent.getData());

            //wssConnectionAdapter.notifyDataSetChanged();

        }else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_UpdateRNARunningStatus)){

            Log.i(TAG, "WSSManager_UpdateRNARunningStatus:" + messageEvent.getData());

            //wssConnectionAdapter.notifyDataSetChanged();

        }
        else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_RNAAck_OK)){

            Log.i(TAG, "WSSManager_RNAAck_OK_Message:" + messageEvent.getData());
            //wssConnectionAdapter.notifyDataSetChanged();

        }else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_RNAAck_Error)){

            Log.i(TAG, "WSSManager_RNAAck_Error_AckMessage:" + messageEvent.getData());
            //wssConnectionAdapter.notifyDataSetChanged();

        }else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_UpdatePCRStatus)){

            Log.i(TAG, "WSSManager_UpdatePCRStatus:" + messageEvent.getData());

            //wssConnectionAdapter.notifyDataSetChanged();

        }else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_UpdatePCRRunningStatus)){

            Log.i(TAG, "WSSManager_UpdatePCRRunningStatus:" + messageEvent.getData());

            //wssConnectionAdapter.notifyDataSetChanged();

        }
        else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_PCRAck_OK)){

            Log.i(TAG, "WSSManager_PCRAck_OK_Message:" + messageEvent.getData());
            //wssConnectionAdapter.notifyDataSetChanged();

        }else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_PCRAck_Error)){

            Log.i(TAG, "WSSManager_PCRAck_Error_AckMessage:" + messageEvent.getData());
            //wssConnectionAdapter.notifyDataSetChanged();

        }else if(messageEvent.getMsgType().equals(EnumMessageEvent.WSSManager_UpdatePCRCurve)){
            Log.i(TAG, "WSSManager_UpdatePCRCurve:" + messageEvent.getData());
            PCRStatusItem pCRStatusItem = (PCRStatusItem) messageEvent.getwSSConnectionItem();

            Experiment experiment = pCRStatusItem.getExperiment();

            BoWeiService.getInstance().saveExperiment(experiment, "temp", Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/", new OnSaveExperimentListener() {
                @Override
                public void onSaveSuccess() {
                   // Toast.makeText(getApplicationContext(),"保存PCR成功",Toast.LENGTH_LONG).show();
                    Log.e(TAG,"save temp.boweipcrResult onSaveSuccess" );
                    // save_result = true;

                    //BoweiJni.analysisExperimentLoessFilter(experiment);
                    String resultFilePath = "/sdcard/PCR/temp.boweipcrResult";
                    Log.i("AAA","filePath"+resultFilePath);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Experiment open_experiment = BoWeiService.getInstance().openExperiment(resultFilePath);

                            //BoweiJni boweiJni = new BoweiJni();
                            //BoweiJni.analysisExperimentLoessFilter(open_experiment);
                            //    String content = GsonUtil.toJson(open_experiment,false);
                            //     Log.e("AAA","content"+content);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if(null != open_experiment){
                                        openBiowayExperiment(open_experiment);
                                    }
                                }
                            });
                        }
                    }).start();
                }

                @Override
                public void onSaveFail() {
                    //Toast.makeText(getApplicationContext(),"保存PCR失败",Toast.LENGTH_LONG).show();
                    Log.e(TAG,"save temp.boweipcrResult onSaveFail" );
                }
            });

        }
    }


    /**
     * 指令正在执行
     * @param robotRunningMessage
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RNARunningMessage robotRunningMessage) {
        Log.i("ROBOT", "ROBOT指令运行1" + robotRunningMessage.getID() + "getStatus" + robotRunningMessage.getStatus());
        Log.i("ROBOT", "ROBOT指令运行2" + robotRunningMessage.getID() + "getNO" + robotRunningMessage.getNO());
        Log.i("ROBOT", "ROBOT指令运行3" + robotRunningMessage.getID() + "getDegree" + robotRunningMessage.getDegree());

        if(robotRunningMessage != null )
        {
            String text = GsonUtil.toJson(robotRunningMessage,false);
            RobotRunningMessageText.setText(text);
//            RobotRunningMessageText.append(text + "\n");
//            RobotRunningMessageText.scrollTo(0,0);
        }

        SerialPortManager_Robot.RobotStatus robotStatus = SerialPortManager_Robot.getInstance().getRobotStatus();
        if(robotStatus.getSenseByKey("PAN1") == true){
            PAN1_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else{
            PAN1_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PAN2") == true){
            PAN2_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else{
            PAN2_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PAN3") == true){
            PAN3_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PAN3_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PAN4") == true){
            PAN4_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PAN4_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PAN5") == true){
            PAN5_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PAN5_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PAN6") == true){
            PAN6_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PAN6_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PAN7") == true){
            PAN7_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PAN7_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PAN8") == true){
            PAN8_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PAN8_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("RNA1") == true){
            RNA1_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            RNA1_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("RNA2") == true){
            RNA2_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            RNA2_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR1") == true){
            PCR1_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PCR1_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR2") == true){
            PCR2_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PCR2_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR3") == true){
            PCR3_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PCR3_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR4") == true){
            PCR4_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PCR4_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR5") == true){
            PCR5_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PCR5_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR6") == true){
            PCR6_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            PCR6_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("ARM") == true){
            ARM_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            ARM_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }

        if(robotStatus.getSenseByKey("PCR_COVER0") == true){
            COVER0_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            COVER0_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }

        if(robotStatus.getSenseByKey("PCR_COVER1") == true){
            COVER1_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            COVER1_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR_COVER2") == true){
            COVER2_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            COVER2_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR_COVER3") == true){
            COVER3_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            COVER3_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR_COVER4") == true){
            COVER4_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            COVER4_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR_COVER5") == true){
            COVER5_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            COVER5_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("PCR_COVER6") == true){
            COVER6_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            COVER6_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
        if(robotStatus.getSenseByKey("DOOR") == true){
            DOOR_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            DOOR_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }

    }

    /**
     * 指令正在执行
     * @param robotManagerMessage
     */
    private String lastAction;
    private String lastDetail;
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RobotManagerResultMessage robotManagerResultMessage) {

        if(robotManagerResultMessage != null )
        {
            String text = GsonUtil.toJson(robotManagerResultMessage,false);
            RobotWorkManagerRunningMessageText.append(text + "\n");
            //RobotWorkManagerRunningMessageText.scrollTo(0,0);
            //RobotWorkManagerRunningMessageText.setText(text);
            if(robotManagerResultMessage.getResult() == WorkInfo.State.FAILED){
                Log.i("ROBOT", "onEventMainThread(RobotManagerResultMessage)" + "--" +
                        robotManagerResultMessage.getAction() + "--" +
                        robotManagerResultMessage.getResult().name() + "--" +
                        robotManagerResultMessage.getDetail());
                lastAction = robotManagerResultMessage.getAction();
                lastDetail = robotManagerResultMessage.getDetail();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RobotManagerMessage robotManagerMessage) {

        if(robotManagerMessage != null )
        {
            String text = GsonUtil.toJson(robotManagerMessage,false);
            RobotWorkManagerRunningMessageText.append(text + "\n");
            RobotWorkManagerRunningMessageText.scrollTo(0,0);
            //RobotWorkManagerRunningMessageText.setText(text);
            if(robotManagerMessage.getResult() == WorkInfo.State.FAILED){
                Log.i("ROBOT", "onEventMainThread(robotManagerMessage)" + "--" +
                        robotManagerMessage.getAction() + "--" +
                        robotManagerMessage.getResult().name() + "--" +
                        robotManagerMessage.getDetail());
            }
        }
    }
    /*
     * EventBus接收订阅的消息
     * @param
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {
        switch (messageEvent.getMsgType()){
            case MsgType.MSG_ROBOT_CONNECT_STATE:
                Log.e("MSG_ROBOT_CONNECT_STATE", "is_ROBOT_connect"+ SerialPortManager_Robot.getInstance().isIs_ROBOT_connect());
                if(SerialPortManager_Robot.getInstance().isIs_ROBOT_connect()){
                    robot_state_img.setStateBackgroud(R.drawable.shape_oval_green_bg);
                    robot_alarm_msg_tv.setText("");
                }else{
                    robot_state_img.setStateBackgroud(R.drawable.shape_oval_red_bg);
                    robot_alarm_msg_tv.setText("E3-机械臂断连");
                }
                break;
            case MsgType.MSG_SHOW_RESULT:
                Log.e("MSG_SHOW_RESULT", "MSG_SHOW_RESULT");
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
                    if(curveList.getCurve_hole().equals("FAM")){
                        isFAMCheck = true;
                    }else if(curveList.getCurve_hole().equals("HEX")){
                        isHEXCheck = true;
                    }else if(curveList.getCurve_hole().equals("ROX")){
                        isROXCheck = true;
                    }else if(curveList.getCurve_hole().equals("CY5")){
                        isCY5Check = true;
                    }
                }

                checkWellState();
                setChannelState();
                drawchart(mCheckRawList,resultChart,"RawCurve");
                getExperimentParamter();
                break;
            case MsgType.MSG_SHOW_ALL_CHANEL_RESULT:
            case MsgType.MSG_SHOW_FAM_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_HEX_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_ROX_CHANNEL_CURVE:
            case MsgType.MSG_SHOW_CY5_CHANNEL_CURVE:
                switch (currentView) {
                    case 1:
                        //raw原始数据
                        getClickRawCurve();
                        drawRawCurve();
                        break;

                    case 2:
                        //扩增数据
                        getClickAMPCurve();
                        drawAMPCurve();
                        break;

                    case 3:
                        //熔解数据
                        getClickMeltCurve();
                        drawMeltCurve();
                        break;

                    case 4:
                        //峰值曲线
                        getClickPeakCurve();
                        drawPeakCurve();
                        break;
                    case 5:
                        getShowExlResults();
                        if(null != resultAdapter){
                            Collections.sort(mCheckExlResults);
                            resultAdapter.setResultList(mCheckExlResults);
                            resultAdapter.notifyDataSetChanged();
                        }
                        break;
                }
                break;
            case MsgType.MSG_SHOW_EXCEL_RESULT:
                if(null != resultAdapter){
                    Collections.sort(mCheckExlResults);
                    resultAdapter.setResultList(mCheckExlResults);
                    resultAdapter.notifyDataSetChanged();
                }
                break;

            case MsgType.MSG_SHOW_SECOND_RESULT:
                //二次分析结果
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
                    }else if(curveList.getChannelNo() == 4){
                        isCY5Check = true;
                    }
                }
                setChannelState();
                checkWellState();
                getExperimentParamter();

                switch (currentView){
                    case 1:
                        getClickRawCurve();
                        drawRawCurve();
                        break;
                    case 2:
                        getClickAMPCurve();
                        drawAMPCurve();
                        break;
                    case 3:
                        getClickMeltCurve();
                        drawMeltCurve();
                        break;
                    case 4:
                        getClickPeakCurve();
                        drawPeakCurve();
                        break;
                    case 5:
                        getShowExlResults();
                        if(null != resultAdapter){
                            Collections.sort(mCheckExlResults);
                            resultAdapter.setResultList(mCheckExlResults);
                            resultAdapter.notifyDataSetChanged();
                        }
                        break;

                }
                //弹出对对话框提示是否需要保存
                //onSaveSecondParseResult();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        APLManager.getInstance().SelfCheck = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        APLManager.getInstance().SelfCheck = true;
        wssUpdateHandler.removeCallbacks(wssUpdateRunnable);
        unRegisterEventBus();
//        WSSModuleManager.stopWSSserver();
    }

    private void initVariable(){
        gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
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

        initParamSettingView();
    }
    private void initParamSettingView(){
        TextView reportlook = (TextView)this.findViewById(R.id.bowei_running_slan_result_report_tv);
        reportlook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AutoPipelineTestActivity.this, BoweiTestActivity.class);
                startActivity(intent);
            }
        });
        mSecAnalyseTv = (TextView)this.findViewById(R.id.bowei_running_slan_sec_analyse_tv);
        mResultChartLayout = (LinearLayout)this.findViewById(R.id.bowei_result_slan_curve_chart_layout);
        mSecAnalyseLayout = (RelativeLayout) this.findViewById(R.id.bowei_slan_result_sec_analyse_layout);
        mBasicParamTitleTv = (TextView)this.findViewById(R.id.bowei_slan_result_basic_param_title_tv);
        mMeltParamTitleTv = (TextView)this.findViewById(R.id.bowei_slan_result_melt_param_title_tv);
        mAmpAlgorithmLv = (ListView)this.findViewById(R.id.bowei_slan_result_sec_analyse_amp_algorithm_lv);
        mAmpParamterLayout = (LinearLayout)this.findViewById(R.id.bowei_slan_result_sec_analyse_amp_param_layout);
        mMeltParamterLayout = (LinearLayout)this.findViewById(R.id.bowei_slan_result_sec_analyse_melt_param_layout);
        mMeltParamterLv = (ListView)this.findViewById(R.id.bowei_slan_result_sec_analyse_melt_paramter_lv);
        mAmpAlgorithmAdapter = new AmpAlgorithmAdapter(getApplicationContext());
        mMeltParamterAdapter = new MeltParamterAdapter(getApplicationContext());
        mAmpAlgorithmLv.setAdapter(mAmpAlgorithmAdapter);
        mMeltParamterLv.setAdapter(mMeltParamterAdapter);

        mResultExlLayout = (HorizontalScrollView)this.findViewById(R.id.bowei_result_slan_excel_list_scrollview) ;
        mResultListView = (ListView)this.findViewById(R.id.bowei_result_slan_list_view) ;
        resultAdapter = new RunBiowayResultAdapter(getApplicationContext());
        resultAdapter.setResultList(mCheckExlResults);
        mResultListView.setAdapter(resultAdapter);

        initAmpDigitalFilter();
        initAmpAysTypeWindow();
        initMeltPeakAlgorithm();
        initOptimizationModeWindow();
        initPeekTypeWindow();
    }

    private void initChartViewListener(){
        initVariable();
        mCurveChartGg = (RadioGroup)this.findViewById(R.id.bowei_result_slan_curve_layout);
        mBasicParamTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //基本参数
                mBasicParamTitleTv.setTextColor(getResources().getColor(R.color.white));
                mBasicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
                mMeltParamTitleTv.setTextColor(getResources().getColor(R.color.black));
                mMeltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
                mAmpParamterLayout.setVisibility(View.VISIBLE);
                mMeltParamterLayout.setVisibility(View.GONE);
                if(null != mAmpAlgorithmAdapter){
                    mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
                }
            }
        });
        mMeltParamTitleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //熔解曲线参数
                mMeltParamTitleTv.setTextColor(getResources().getColor(R.color.white));
                mMeltParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue);
                mBasicParamTitleTv.setTextColor(getResources().getColor(R.color.black));
                mBasicParamTitleTv.setBackgroundResource(R.drawable.shape_rect_blue_stroke);
                mAmpParamterLayout.setVisibility(View.GONE);
                mMeltParamterLayout.setVisibility(View.VISIBLE);
                if(null != mMeltParamterAdapter){
                    mMeltParamterAdapter.setMeltParamter(configTargets);
                }
            }
        });
        mCurveChartGg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.bowei_result_slan_rawcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        //  mChannelLayout.setVisibility(View.VISIBLE);
                        //   mHoleLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 1;
                    getClickRawCurve();
                    drawRawCurve();
                } else if (checkedId == R.id.bowei_result_slan_ampcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        //  mChannelLayout.setVisibility(View.VISIBLE);
                        //  mHoleLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 2;
                    getClickAMPCurve();
                    drawAMPCurve();
                } else if (checkedId == R.id.bowei_result_slan_meltcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        //  mChannelLayout.setVisibility(View.VISIBLE);
                        //  mHoleLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 3;
                    getClickMeltCurve();
                    drawMeltCurve();
                } else if (checkedId == R.id.bowei_result_slan_peakcurve_rb) {
                    if (!isChartVisible) {
                        isChartVisible = true;
                        isExcelVisible = false;
                        isSecAysVisible = false;
                    }
                    if(isChartVisible){
                        // mChannelLayout.setVisibility(View.VISIBLE);
                        // mHoleLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                        mResultExlLayout.setVisibility(View.GONE);
                        mResultChartLayout.setVisibility(View.VISIBLE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                    }
                    currentView = 4;
                    getClickPeakCurve();
                    drawPeakCurve();
                } else if (checkedId == R.id.bowei_result_slan_list_rb) {
                    currentView = 5;
                    if (!isExcelVisible) {
                        isChartVisible = false;
                        isExcelVisible = true;
                        isSecAysVisible = false;
                    }
                    if(isExcelVisible){
                        // mChannelLayout.setVisibility(View.VISIBLE);
                        //   mHoleLayout.setVisibility(View.VISIBLE);
                        mResultExlLayout.setVisibility(View.VISIBLE);
                        mResultChartLayout.setVisibility(View.GONE);
                        mSecAnalyseLayout.setVisibility(View.GONE);
                        mSecAnalyseTv.setVisibility(View.GONE);
                    }
                    getShowExlResults();
                    MessageEvent messageEvent = new MessageEvent();
                    messageEvent.setMsgType(MsgType.MSG_SHOW_EXCEL_RESULT);
                    EventBus.getDefault().post(messageEvent);
                }else if(checkedId ==R.id.bowei_result_slan_set_paramter_rb){
                    //参数设置
                    currentView = 6;
                    if (!isSecAysVisible) {
                        isChartVisible = false;
                        isExcelVisible = false;
                        isSecAysVisible = true;
                    }
                    if(isSecAysVisible){
                        //  mChannelLayout.setVisibility(View.GONE);
                        //  mHoleLayout.setVisibility(View.INVISIBLE);
                        mSecAnalyseTv.setVisibility(View.VISIBLE);
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
        BiowayManager.getInstance().setOnExperimentCallback(new OnExperimentCallback() {
            @Override
            public void onSaveExToDbResult(boolean result) {

            }

            @Override
            public void onParseFinish() {
                //解析完成显示数据
                Log.i("AAA","onParseFinish");
                MessageEvent resultMsg = new MessageEvent();
                resultMsg.setMsgType(MsgType.MSG_SHOW_RESULT);
                EventBus.getDefault().postSticky(resultMsg);
            }

            @Override
            public void onSaveJsonResult(boolean result, String message) {

            }

            @Override
            public void onSecondParseFinish(boolean result) {
                Log.i("BBB","二次分析解析" +result);
                if (result) {
                    MessageEvent resultMsg = new MessageEvent();
                    resultMsg.setMsgType(MsgType.MSG_SHOW_SECOND_RESULT);
                    EventBus.getDefault().postSticky(resultMsg);
                } else {
                    /*String failMsg = getResources().getString(R.string.toast_second_analyse_fail);
                    MessageEvent failEvent = new MessageEvent();
                    failEvent.setMsgType(1);
                    failEvent.setData(failMsg);
                    EventBus.getDefault().postSticky(failEvent);*/
                }
            }

            @Override
            public void onUpdateFinish(boolean result) {

            }
        });
        openFileTv = (TextView)this.findViewById(R.id.bowei_open_file_tv) ;
        openFileTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    openBiowayExFile();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        isA1Check = isA2Check = isA3Check = isA4Check = isA5Check = isA6Check = isA7Check = isA8Check = false;
        mOneTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isA1Check = !isA1Check;
                if (!isA1Check) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isA1Check, (TextView) view);
                updateWellResult();
            }
        });
        mTwoTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isA2Check = !isA2Check;
                if (!isA2Check) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isA2Check, (TextView) view);
                updateWellResult();
            }
        });
        mThreeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isA3Check = !isA3Check;
                if (!isA3Check) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isA3Check, (TextView) view);
                updateWellResult();
            }
        });
        mFourTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isA4Check = !isA4Check;
                if (!isA4Check) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isA4Check, (TextView) view);
                updateWellResult();
            }
        });
        mFiveTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isA5Check = !isA5Check;
                if (!isA5Check) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isA5Check, (TextView) view);
                updateWellResult();
            }
        });
        mSixTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isA6Check = !isA6Check;
                if (!isA6Check) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isA6Check, (TextView) view);
                updateWellResult();
            }
        });
        mSevenTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isA7Check = !isA7Check;
                if (!isA7Check) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isA7Check, (TextView) view);
                updateWellResult();
            }
        });
        mEightTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isA8Check = !isA8Check;
                if (!isA8Check) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isA8Check, (TextView) view);
                updateWellResult();
            }
        });
        mAllHoleTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isAllCheck) {
                    isAllCheck = false;
                    setAllHoleUnCheck();
                } else {
                    isAllCheck = true;
                    setAllHoleCheck();

                }
                updateWellResult();
                setHodeState(isAllCheck, mAllHoleTv);
            }
        });

        mFamChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isFAMCheck = isCheck;
                if(!isFAMCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isHEXCheck){
                        isAllChanelCheck = true;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mFamMsg = new MessageEvent();
                mFamMsg.setMsgType(MsgType.MSG_SHOW_FAM_CHANNEL_CURVE);
                EventBus.getDefault().post(mFamMsg);
            }
        });
        mHexChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isHEXCheck = isCheck;
                if(!isHEXCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isCY5Check && isFAMCheck ){
                        isAllChanelCheck = true;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mHEXMsg = new MessageEvent();
                mHEXMsg.setMsgType(MsgType.MSG_SHOW_HEX_CHANNEL_CURVE);
                EventBus.getDefault().post(mHEXMsg);
            }
        });
        mRoxChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isROXCheck = isCheck;
                if(!isROXCheck){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isFAMCheck && isCY5Check && isHEXCheck){
                        isAllChanelCheck = true;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mROXMsg = new MessageEvent();
                mROXMsg.setMsgType(MsgType.MSG_SHOW_ROX_CHANNEL_CURVE);
                EventBus.getDefault().post(mROXMsg);
            }
        });
        mCY5ChannelTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
            @Override
            public void OnButtonClick(boolean isCheck) {
                isCY5Check = isCheck;
                if(!isCY5Check){
                    if(isAllChanelCheck){
                        isAllChanelCheck = false;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
                    }
                }else{
                    if(isROXCheck && isHEXCheck && isFAMCheck){
                        isAllChanelCheck = true;
                        mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
                    }
                }
                MessageEvent mCY5Msg = new MessageEvent();
                mCY5Msg.setMsgType(MsgType.MSG_SHOW_CY5_CHANNEL_CURVE);
                EventBus.getDefault().post(mCY5Msg);
            }
        });
        mAllCheckTv.setOnClickListener(new CustomCheckText.OnCheckBtnClickListener() {
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

        //二次分析
        mSecAnalyseTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                secondAnalayse();
            }
        });
    }

    /*二次分析
     * 对话框输入数值点击确认二次分析，调用SLANManager方法
     */
    private void secondAnalayse(){
        if(null != configTargets && configTargets.size()>0){
            showLoadingDialog("二次分析中...");
            //将模板下的判断文件传入
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BiowayManager.getInstance().secondSLANRunExAnalyse(configTargets, null,0,null,null,null);
                }
            }).start();
        }

    }

    private void  setChannelState(){
        if(isFAMCheck){
            mFamChannelTv.setCheckState(R.drawable.item_chanel_check,isFAMCheck);
        }else {
            mFamChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isFAMCheck);
        }
        if(isHEXCheck){
            mHexChannelTv.setCheckState(R.drawable.item_chanel_check,isHEXCheck);
        }else{
            mHexChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isHEXCheck);
        }

        if(isROXCheck){
            mRoxChannelTv.setCheckState(R.drawable.item_chanel_check,isROXCheck);
        }else{
            mRoxChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isROXCheck);
        }

        if(isCY5Check){
            mCY5ChannelTv.setCheckState(R.drawable.item_chanel_check,isCY5Check);
        }else{
            mCY5ChannelTv.setCheckState(R.drawable.item_chanel_uncheck,isCY5Check);
        }


        if(isCY5Check && isFAMCheck &&isROXCheck && isHEXCheck ){
            isAllChanelCheck = true;
            mAllCheckTv.setCheckState(R.drawable.item_chanel_check,isAllChanelCheck);
        }else{
            isAllChanelCheck = false;
            mAllCheckTv.setCheckState(R.drawable.item_chanel_uncheck,isAllChanelCheck);
        }
    }

    /**
     *
     * @param check
     * @param textView
     */
    private void setHodeState(boolean check,TextView textView){
        textView.setClickable(true);

        if(!check){
            textView.setBackgroundResource(R.drawable.shape_oval_gray_bg);
            textView.setTextColor(getResources().getColor(R.color.blue));
        }else{
            textView.setBackgroundResource(R.drawable.shape_oval_blue_bg);
            textView.setTextColor(getResources().getColor(R.color.white));
        }
    }

    private void checkAllHole(){
        if (isA1Check && isA2Check && isA3Check && isA4Check
                && isA5Check && isA6Check && isA7Check && isA8Check) {
            if(!isAllCheck){
                isAllCheck = true;
                setHodeState(isAllCheck, mAllHoleTv);
            }
        }
    }

    private void setAllHoleCheck(){
        if(isA1Check && isA2Check && isA3Check && isA4Check
                && isA5Check && isA6Check && isA7Check && isA8Check){
            isAllCheck = true;
            setHodeState(isAllCheck,mAllHoleTv);
        }else {
            isA1Check = true;isA2Check = true;isA3Check = true;isA4Check = true;
            isA5Check = true;isA6Check = true;isA7Check = true;isA8Check = true;
            isAllCheck = true;
            setHodeState(isA1Check,mOneTv);
            setHodeState(isA2Check, mTwoTv);
            setHodeState(isA3Check, mThreeTv);
            setHodeState(isA4Check, mFourTv);
            setHodeState(isA5Check, mFiveTv);
            setHodeState(isA6Check, mSixTv);
            setHodeState(isA7Check, mSevenTv);
            setHodeState(isA8Check, mEightTv);
        }

    }


    private void setAllHoleUnCheck(){
        isA1Check = false;isA2Check = false;isA3Check = false;
        isA4Check = false;isA5Check = false;isA6Check = false;
        isA7Check = false; isA8Check = false;
        setHodeState(isA1Check,mOneTv);
        setHodeState(isA2Check, mTwoTv);
        setHodeState(isA3Check, mThreeTv);
        setHodeState(isA4Check, mFourTv);
        setHodeState(isA5Check, mFiveTv);
        setHodeState(isA6Check, mSixTv);
        setHodeState(isA7Check, mSevenTv);
        setHodeState(isA8Check, mEightTv);
    }


    private void updateWellResult(){
        //判断哪个没有选中
        getClickAMPCurve();
        getClickRawCurve();
        getClickMeltCurve();
        getClickPeakCurve();
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


    private void getShowExlResults(){
        mExlResults.clear();
        List<BiowayExlResult> mAllExResult = BiowayManager.getInstance().getExlResults();
        for(BiowayExlResult exlResult : mAllExResult) {

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
       /* if(isScaleXRaw){
            xAxis.setAxisMaximum(max_Raw_X);
            xAxis.setAxisMinimum(min_Raw_X);
            yAxis.setAxisMaximum(max_Raw_Y);
            yAxis.setAxisMinimum(min_Raw_Y);
        }else{
            yAxis.resetAxisMinimum();
            yAxis.resetAxisMaximum();
            xAxis.resetAxisMaximum();
            xAxis.resetAxisMinimum();
        }*/
        drawchart(mCheckRawList,resultChart,"RawCurve");
    }

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
        /*if(!isFamThreshold && !isHexThreshold && !isRoxThreshold && !iscy5Threshold ){
            drawchart(mCheckAMPList, resultChart,"AMPCurve");
        }else{
            XAxis xAxis = resultChart.getXAxis();
            YAxis yAxis = resultChart.getAxisLeft();
            *//*if(isScaleXAmp){
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
            }*//*
            drawchart(mCheckAMPList, resultChart,"AMPCurve");
        }*/
        drawchart(mCheckAMPList, resultChart,"AMPCurve");
    }

    private void drawMeltCurve() {
        mCheckMeltList.clear();
        if (null != mMeltCurveList) {
            if (mMeltCurveList.size() > 0) {
                for (BiowayCurveList curveList : mMeltCurveList) {
                    if (curveList.getCurve_hole().equals("FAM")) {
                        if (isFAMCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("HEX")) {
                        if (isHEXCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("ROX")) {
                        if (isROXCheck) {
                            mCheckMeltList.add(curveList);
                        }
                    } else if (curveList.getCurve_hole().equals("CY5")) {
                        if (isCY5Check) {
                            mCheckMeltList.add(curveList);
                        }
                    }
                }
            }

        }
        clearChart();
      /*  XAxis xAxis = resultChart.getXAxis();
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
        }*/
       /* String melt = gson.toJson(mCheckMeltList);
        FileUtils.dumpDeviceInfo(melt,"Melt");
*/

        drawchart(mCheckMeltList, resultChart, "MeltCurve");

    }

    private void drawPeakCurve(){
        if(null != mPeakCurveList) {
            mCheckPeakList.clear();
            if (mPeakCurveList.size() > 0) {
                for (BiowayCurveList curveList : mPeakCurveList) {
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
            clearChart();
       /* XAxis xAxis = resultChart.getXAxis();
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
        }*/
            drawchart(mCheckPeakList, resultChart, "PeakCurve");
        }
    }
    private void getClickRawCurve(){
        mRawCurveList.clear();
        List<BiowayCurveList> rawList = BiowayManager.getInstance().getRawCurve();
        for(BiowayCurveList curveList : rawList){
            if(isA1Check && curveList.getWellNum().equals("A1")){
                mRawCurveList.add(curveList);
            }else if (isA2Check && curveList.getWellNum().equals("A2")) {
                mRawCurveList.add(curveList);
            } else if (isA3Check && curveList.getWellNum().equals("A3")) {
                mRawCurveList.add(curveList);
            } else if (isA4Check && curveList.getWellNum().equals("A4")) {
                mRawCurveList.add(curveList);
            } else if (isA5Check && curveList.getWellNum().equals("A5")) {
                mRawCurveList.add(curveList);
            } else if (isA6Check && curveList.getWellNum().equals("A6")) {
                mRawCurveList.add(curveList);
            } else if (isA7Check && curveList.getWellNum().equals("A7")) {
                mRawCurveList.add(curveList);
            } else if (isA8Check && curveList.getWellNum().equals("A8")) {
                mRawCurveList.add(curveList);
            }
        }
    }
    //
    private void getClickAMPCurve(){
        mAMPCurveList.clear();
        List<BiowayCurveList> AMPList = BiowayManager.getInstance().getAMPCurve();
        for(BiowayCurveList curveList : AMPList){
            if(isA1Check && curveList.getWellNum().equals("A1")){
                mAMPCurveList.add(curveList);
            }else if (isA2Check && curveList.getWellNum().equals("A2")) {
                mAMPCurveList.add(curveList);

            } else if (isA3Check && curveList.getWellNum().equals("A3")) {
                mAMPCurveList.add(curveList);
            } else if (isA4Check && curveList.getWellNum().equals("A4")) {
                mAMPCurveList.add(curveList);

            } else if (isA5Check && curveList.getWellNum().equals("A5")) {
                mAMPCurveList.add(curveList);

            } else if (isA6Check && curveList.getWellNum().equals("A6")) {
                mAMPCurveList.add(curveList);

            } else if (isA7Check && curveList.getWellNum().equals("A7")) {
                mAMPCurveList.add(curveList);

            } else if (isA8Check && curveList.getWellNum().equals("A8")) {
                mAMPCurveList.add(curveList);
            }
        }
    }

    private void getClickMeltCurve(){
        mMeltCurveList.clear();
        List<BiowayCurveList> meltList = BiowayManager.getInstance().getMeltCurve();
        if(meltList.size()>0){
            for(BiowayCurveList curveList : meltList){
                if(isA1Check && curveList.getWellNum().equals("A1")){
                    mMeltCurveList.add(curveList);
                }else if (isA2Check && curveList.getWellNum().equals("A2")) {
                    mMeltCurveList.add(curveList);

                } else if (isA3Check && curveList.getWellNum().equals("A3")) {

                    mMeltCurveList.add(curveList);

                } else if (isA4Check && curveList.getWellNum().equals("A4")) {
                    mMeltCurveList.add(curveList);

                } else if (isA5Check && curveList.getWellNum().equals("A5")) {
                    mMeltCurveList.add(curveList);

                } else if (isA6Check && curveList.getWellNum().equals("A6")) {
                    mMeltCurveList.add(curveList);

                } else if (isA7Check && curveList.getWellNum().equals("A7")) {
                    mMeltCurveList.add(curveList);

                } else if (isA8Check && curveList.getWellNum().equals("A8")) {
                    mMeltCurveList.add(curveList);
                }
            }
        }

        Log.i("AAA", "getClickMeltCurveResult" +mMeltCurveList.size() );
    }

    private void getClickPeakCurve(){
        mPeakCurveList.clear();
        List<BiowayCurveList> peakLisk = BiowayManager.getInstance().getPeakCurve();
        if(peakLisk.size()>0){
            for(BiowayCurveList curveList : peakLisk){

                if(isA1Check && curveList.getWellNum().equals("A1")){
                    mPeakCurveList.add(curveList);
                }else if (isA2Check && curveList.getWellNum().equals("A2")) {
                    mPeakCurveList.add(curveList);

                } else if (isA3Check && curveList.getWellNum().equals("A3")) {

                    mPeakCurveList.add(curveList);

                } else if (isA4Check && curveList.getWellNum().equals("A4")) {
                    mPeakCurveList.add(curveList);

                } else if (isA5Check && curveList.getWellNum().equals("A5")) {
                    mPeakCurveList.add(curveList);

                } else if (isA6Check && curveList.getWellNum().equals("A6")) {
                    mPeakCurveList.add(curveList);

                } else if (isA7Check && curveList.getWellNum().equals("A7")) {
                    mPeakCurveList.add(curveList);

                } else if (isA8Check && curveList.getWellNum().equals("A8")) {
                    mPeakCurveList.add(curveList);
                }
            }
        }

    }

    /**清除图表**/
    public void clearChart(){
        resultChart.clear();
        resultChart.notifyDataSetChanged();
        resultChart.invalidate();
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
                set1.setDrawValues(false);// 不显示点值
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
            // xAxis.setLabelCount(curve_list.size(), false);
            lineChart.post(new Runnable() {
                @Override
                public void run() {
                    markView = new LineChartMarkView(getApplicationContext());
                    lineChart.setMarker(markView);
                }
            });
            lineChart.invalidate();
        }
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
        }
        limitLine.setTextColor(R.color.blue);  //设置限制线上label字体的颜色
        limitLine.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);//标签位置
        limitLine.disableDashedLine();
        return limitLine;

    }

    /**
     * 打开保存的结果文件 ????????????????????????????
     */
    private void openBiowayExFile() throws JSONException {
        Log.i("AAA","openBiowayExFile");


        String resultFilePath = "/sdcard/PCR/temp.boweipcrResult";
        // String resultFilePath = "/mnt/sdcard/PCR/BoWeiResult/产物标准熔曲(005).boweipcrResult";
        Experiment experiment = BoWeiService.getInstance().openExperiment(resultFilePath);
        Log.i("AAA","experiment" + experiment);
        //保存
        /*if(experiment != null){
            BoWeiService.getInstance(getApplicationContext())
                    .saveExperiment(experiment,"aaa","/sdcard/PCR/BoWeiResult/");
        }*/

        //解析文件显示结果
        if(experiment != null){
            Log.i("AAA","experiment:" + "parseExperimentResult");
            BiowayManager.getInstance().parseExperimentResult(experiment,null,null);
        }else{
            Toast.makeText(getApplicationContext(),"没有结果文件",Toast.LENGTH_LONG);
        }

        //参数设置

    }

    /**
     * 设置所有的孔位位选定状态
     */
    private void checkWellState(){

        for(BiowayCurveList curveList : showCurveList){

            if(curveList.getWellNum().equals("A1")){
                isA1Check = true;
                setHodeState(isA1Check, mOneTv);
            }else if(curveList.getWellNum().equals("A2")){
                isA2Check = true;
                setHodeState(isA2Check, mTwoTv);
            }else if(curveList.getWellNum().equals("A3")){
                isA3Check = true;
                setHodeState(isA3Check, mThreeTv);
            } else if(curveList.getWellNum().equals("A4")){
                isA4Check = true;
                setHodeState(isA4Check, mFourTv);
            } else if(curveList.getWellNum().equals("A5")){
                isA5Check = true;
                setHodeState(isA5Check, mFiveTv);
            } else if(curveList.getWellNum().equals("A6")){
                isA6Check = true;
                setHodeState(isA6Check, mSixTv);
            } else if(curveList.getWellNum().equals("A7")){
                isA7Check = true;
                setHodeState(isA7Check, mSevenTv);
            } else if(curveList.getWellNum().equals("A8")){
                isA8Check = true;
                setHodeState(isA8Check, mEightTv);
            }
        }

        if (isA1Check && isA2Check && isA3Check && isA4Check
                && isA5Check && isA6Check && isA7Check && isA8Check) {
            isAllCheck = true;
            setHodeState(isAllCheck, mAllHoleTv);
        }
    }

    private void getExperimentParamter(){
        iscy5Threshold = isFamThreshold = isHexThreshold = isRoxThreshold = false;
        configTargets.clear();
        if(true){
            if(null != BiowayManager.getInstance().getExperiment()){
                Experiment experiment = BiowayManager.getInstance().getExperiment();
                ArrayList<AnalyseParamter> analyseParamters = BoWeiService.getInstance()
                        .getExperimentParamter(experiment);
                if(analyseParamters != null && analyseParamters.size()>0){
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
                            }else if(target.getChannelNo()== 4){
                                iscy5Threshold = true;
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
                            }
                        }
                    }
                }

            }
        }
        Log.i("AAA","isMelt" + isMelt);
        if(null != configTargets){
            if(null != mAmpAlgorithmAdapter){
                mAmpAlgorithmAdapter.setAmpAlgorithm(configTargets);
            }
            if(null != mMeltParamterAdapter){
                mMeltParamterAdapter.setMeltParamter(configTargets);
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
                        int endbase = (int) StringUtil.convertToFloat(s,1.0f);
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
                        //f峰高阈值
                        double peekHighThreshold = (double) StringUtil.convertToFloat(s,0.00f);
                        ConfigTarget configTarget = configTargets.get(meltParamter_pos);
                        configTarget.setPeekHighThreshold(peekHighThreshold);
                        mMeltParamterAdapter.setMeltParamter(configTargets);
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

    private void initAmpAysTypeWindow(){
        mAmpAysTypes = new ArrayList<String>();
        mAmpAysTypes.add("定性");
        mAmpAysTypes.add("定量");
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

    private boolean isOptimizationModeExpanded = false;//
    //扩增曲线算法
    private void collapseOptimizationMode(){
        if (mOptimizationModeWindow != null && mOptimizationModeWindow.isShowing()) {
            mOptimizationModeWindow.dismiss();
        }
        isOptimizationModeExpanded = false;
    }

    //展开扩增曲线算法
    private void expandOptimizationMode(View view){
        if(mOptimizationModes== null && mOptimizationModes.isEmpty()){
            return;
        }
        mOptimizationModeWindow.showAsDropDown(view);
        isOptimizationModeExpanded = true;
    }

    private void initOptimizationModeWindow(){
        mOptimizationModes = new ArrayList<String>();
        mOptimizationModes.add("不优化");
        mOptimizationModes.add("自动优化");
        mOptimizationModes.add("手动优化");
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

    private boolean isDigitalFilterExpanded = false;
    //扩增数字滤波
    private void collapseDigitalFilter(){
        if (mDigitalFilterWindow != null && mDigitalFilterWindow.isShowing()) {
            mDigitalFilterWindow.dismiss();
        }
        isDigitalFilterExpanded = false;
    }

    //展开扩增曲线算法
    private void expandDigitalFilter(View view){
        if(mDighitalFilters== null && mDighitalFilters.isEmpty()){
            return;
        }
        mDigitalFilterWindow.showAsDropDown(view);
        isDigitalFilterExpanded = true;
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
    //扩增曲线算法
    private void collapsePeekType(){
        if (mPeekTypeWindow != null && mPeekTypeWindow.isShowing()) {
            mPeekTypeWindow.dismiss();
        }
    }

    //展开扩增曲线算法
    private void expandPeekType(View view){
        if( mPeekTypes== null && mPeekTypes.isEmpty()){
            return;
        }
        mPeekTypeWindow.showAsDropDown(view);
    }

    private void initPeekTypeWindow(){
        mPeekTypes = new ArrayList<String>();
        mPeekTypes.add("正峰");
        mPeekTypes.add("负峰");
        mPeekTypes.add("正负峰");
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
        mMeltPeakAlgorithms.add("标准算法");
        mMeltPeakAlgorithms.add("高级算法");
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

    private LoadingDialog loadingDialog ;
    public void showLoadingDialog(String message){
        if(null == loadingDialog){
            loadingDialog = new LoadingDialog(this, message);
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

    /**
     * 打开保存的结果文件
     */
    private void openBiowayExperiment(Experiment experiment){
        Log.i("AAA","openBiowayExperiment");
        if(experiment != null){
            Log.i("AAA","openBiowayExperiment1");
            BiowayManager.getInstance().parseExperimentResult(experiment,null,null);
        }

    }



}

