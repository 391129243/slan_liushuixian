package com.winters.boweilibrary.BoWei;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfRenderer;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.bean.PeaktypeOptimization;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.gidi.bio_pcr.config.adapter.AmpAlgorithmAdapter;
import com.gidi.bio_pcr.config.adapter.MeltParamterAdapter;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.view.NumKeyboardDialog;
import com.github.mikephil.charting.components.LimitLine;
import com.hongshi.slansdk.model.experiment.AnalyseParamter;
import com.hongshi.slansdk.model.experiment.Target;
import com.hongshi.slansdk.model.experiment.TargetParamter;
import com.hongshi.slansdk.model.experiment.TargetParamterMelt;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.BoWei.bean.BiowayCurveList;
import com.winters.boweilibrary.BoWei.bean.BiowayExlResult;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.winters.boweilibrary.BoWei.bean.RunBiowayResult;
import com.gidi.bio_pcr.bioway.adapter.RunBiowayResultAdapter;
import com.winters.boweilibrary.BoWei.event.BoWeiPCRProcedureInfoEvent;
import com.winters.boweilibrary.BoWei.event.BoWeiProcedureFinishEvent;
import com.winters.boweilibrary.BoWei.event.BoWeiRecvDataInfo;
import com.winters.boweilibrary.BoWei.model.experiment.ExperimentParam;
import com.winters.boweilibrary.BoWei.serialcom.BoWeiConnectManager;
import com.winters.boweilibrary.BoWei.task.BoWeiPCRAsyncTask;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;
import com.winters.boweilibrary.BoWei.utils.BoWeiTaiKeHexUtils;
import com.gidi.bio_pcr.WintersUtils.NumUtils;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.config.activity.ShowConfigActivity;
import com.gidi.bio_pcr.config.adapter.FileNameAdapter;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.PcrValue;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.pcr.OnExperimentCallback;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.NumberUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomCheckText;
import com.gidi.bio_pcr.view.LineChartMarkView;
import com.gidi.bio_pcr.view.LinearSpacingItemDecoration;
import com.gidi.bio_pcr.view.WarnDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
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
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Step;
import com.hongshi.slansdk.service.SLANService;
import com.winters.boweilibrary.BoWei.utils.PdfPrintHelper;
import com.winters.boweilibrary.BoweiJni;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static com.gidi.bio_pcr.receiver.UsbBroadCastReceiver.ACTION_USB_PERMISSION;

public class BoweiTestActivity extends Activity implements View.OnClickListener{

    private Button heartbeat;
    private Button get_pcr_tempreture;
    private Button led_on;
    private Button printter;
    private Button led_blight;
    private Button set_pcr_tempreture;
    private Button motor_reset;
    private Button motor_scan;
    private Button get_fluro;

    private Button config_pcr;

    private Button load_pcr;


    private EditText pcr_tempreture;

    private TextView log;

    private ScrollView scrollView;

    private TextView tempreture1;
    private TextView tempreture2;
    private TextView tempreture3;

    private ListView listview_flu;
    private ListView listview_hex;

    private Button create_hex;

    private Button run_hex;
    private Button stop_hex;

    private EditText current_segment;
    private EditText current_cycle;
    private EditText current_step;
    private EditText current_action;

    private EditText current_step_remaintime;

    private Chronometer mChronTimer;

    private RecyclerView projectRv;
    private FileNameAdapter slanFileAdapter;

    private RecyclerView experimentRv;
    private FileNameAdapter boweiExperimentAdapter;
    private String experimentName;
    private List<String> mExperimentFileNames;
    private int click_position_bowei = -1;
    private File boweiExperimentFile;

    private List<String> mSlanFileNames;

    private String fileName;
    private Project project = null;
    private int click_position = -1;

    List<BoWeiPCRStepEntity> BoWeiPCRStepEntityList = new ArrayList<>();


    private List<ConfigSLANSection> mSLANSections = new ArrayList<ConfigSLANSection>();

    public WarnDialog mNoticeDialog;

    private boolean isFlag = false;
    private DecimalFormat nf1 = new DecimalFormat("#0.000");
    private DecimalFormat nf = new DecimalFormat( "0.00");
    private Gson gson;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bowei_test);
        heartbeat = (Button)this.findViewById(R.id.heartbeat);
        get_pcr_tempreture = (Button)this.findViewById(R.id.get_pcr_tempreture);
        led_on = (Button)this.findViewById(R.id.led_on);
        led_blight = (Button)this.findViewById(R.id.led_blight);
        set_pcr_tempreture = (Button)this.findViewById(R.id.set_pcr_tempreture);
        motor_reset = (Button)this.findViewById(R.id.motor_reset);
        motor_scan = (Button)this.findViewById(R.id.motor_scan);
       // get_fluro = (Button)this.findViewById(R.id.get_fluro);
        config_pcr = (Button)this.findViewById(R.id.config_pcr);
        load_pcr = (Button)this.findViewById(R.id.load_pcr);
        create_hex = (Button)this.findViewById(R.id.create_hex);

        led_on = (Button)this.findViewById(R.id.printter);

        run_hex = (Button)this.findViewById(R.id.run_hex);
        stop_hex = (Button)this.findViewById(R.id.stop_hex);

        current_segment = (EditText) this.findViewById(R.id.current_segment);
        current_cycle = (EditText)this.findViewById(R.id.current_cycle);
        current_step = (EditText)this.findViewById(R.id.current_step);
        current_action = (EditText)this.findViewById(R.id.current_action);
        current_step_remaintime = (EditText)this.findViewById(R.id.current_step_remaintime);


        heartbeat.setOnClickListener(this);
        get_pcr_tempreture.setOnClickListener(this);
        led_on.setOnClickListener(this);
        led_blight.setOnClickListener(this);
        set_pcr_tempreture.setOnClickListener(this);
        motor_reset.setOnClickListener(this);
        motor_scan.setOnClickListener(this);
       // get_fluro.setOnClickListener(this);
        config_pcr.setOnClickListener(this);
        load_pcr.setOnClickListener(this);
        create_hex.setOnClickListener(this);
        run_hex.setOnClickListener(this);
        stop_hex.setOnClickListener(this);

        pcr_tempreture = (EditText)this.findViewById(R.id.pcr_tempreture);

        log = (TextView)this.findViewById(R.id.log);
        log.setMovementMethod(new ScrollingMovementMethod());

        scrollView =  (ScrollView)this.findViewById(R.id.recevie_data);

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.post(new Runnable() {
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });

        tempreture1 =  (TextView)this.findViewById(R.id.tempreture1);
        tempreture2 =  (TextView)this.findViewById(R.id.tempreture2);
        tempreture3 =  (TextView)this.findViewById(R.id.tempreture3);

        listview_flu =  (ListView)this.findViewById(R.id.listview_flu);
        listview_hex =  (ListView)this.findViewById(R.id.listview_hex);

        View headerView = LayoutInflater.from(this).inflate(R.layout.fluro_header_layout, listview_flu, false);
        listview_flu.addHeaderView(headerView);

        View headerView_hex = LayoutInflater.from(this).inflate(R.layout.bowei_hexlist_header, listview_flu, false);
        listview_hex.addHeaderView(headerView_hex);

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


        //////显示结果/////////////
        registerEventBus();


        mSlanFileNames = new ArrayList<String>();
        File config_slan_dir = new File(FileUtils.CONFIG_SLAN_PATH);
        if(!config_slan_dir.exists()){
            config_slan_dir.mkdirs();
        }
        File[] files = new File(FileUtils.CONFIG_SLAN_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != mSlanFileNames) {
                    if(files[i].getName() != null && files[i].getName().contains(".prj")){
                        String name = files[i].getName().substring(0,files[i].getName().length()-4);
                        mSlanFileNames.add(name);
                        Collections.sort(mSlanFileNames);
                    }
                }
            }
        }

        projectRv = (RecyclerView)this.findViewById(R.id.config_info_project_recycleview);
        LinearLayoutManager layoutManager = new GridLayoutManager(BoweiTestActivity.this.getApplicationContext(),2);
        projectRv.setLayoutManager(layoutManager);
        projectRv.addItemDecoration(new LinearSpacingItemDecoration(BoweiTestActivity.this, 6));
        slanFileAdapter = new FileNameAdapter(BoweiTestActivity.this.getApplicationContext());
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


        mExperimentFileNames = new ArrayList<String>();
        File BoWeiResult_dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/BoWeiResult/");
        if(!BoWeiResult_dir.exists()){
            BoWeiResult_dir.mkdirs();
        }
        File[] files_result = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/BoWeiResult/").listFiles();//文件夹下的所有文件
        if(files_result != null && files_result.length>0) {
            for (int i = 0; i < files_result.length; i++) {
                if (null != mExperimentFileNames) {
                    if(files_result[i].getName() != null && files_result[i].getName().contains(".boweipcrResult")){
                        String name = files_result[i].getName().substring(0,files_result[i].getName().length());
                        mExperimentFileNames.add(name);
                        Collections.sort(mExperimentFileNames);
                    }
                }
            }
        }

        experimentRv = (RecyclerView)this.findViewById(R.id.config_info_experiment_recycleview);
        LinearLayoutManager layoutManager1 = new GridLayoutManager(BoweiTestActivity.this.getApplicationContext(),2);
        experimentRv.setLayoutManager(layoutManager1);
        experimentRv.addItemDecoration(new LinearSpacingItemDecoration(BoweiTestActivity.this, 6));
        boweiExperimentAdapter = new FileNameAdapter(BoweiTestActivity.this.getApplicationContext());
        boweiExperimentAdapter.setTemplateList(mExperimentFileNames);
        experimentRv.setAdapter(boweiExperimentAdapter);
        boweiExperimentAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                click_position_bowei = position;
                boweiExperimentAdapter.setTemplateList(mExperimentFileNames);
                boweiExperimentAdapter.setSelectedPosition(click_position_bowei);
                experimentName = mExperimentFileNames.get(position);
                boweiExperimentFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/BoWeiResult/" + experimentName);
            }
        });

        mChronTimer = (Chronometer)this.findViewById(R.id.chronometer);
        mChronTimer.setText("00:00:00");
        mChronTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                int textLength = mChronTimer.getText().length();
                if(textLength == 5){
                    mChronTimer.setText("00:" + mChronTimer.getText().toString());
                }else if(textLength == 7){
                    mChronTimer.setText("0" + mChronTimer.getText().toString());
                }
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gson = null;
        unRegisterEventBus();
        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
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
        clearAllList();
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.heartbeat) {
            //onBack();
            BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("set_heartbeat", "握手", "5E 01 00 05 64", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
            BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
        } else if (id == R.id.get_pcr_tempreture) {
            BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("get_current_temperature", "获取PCR当前温度", "5E 43 00 06 00 A7", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
            BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
        }else if (id == R.id.led_on) {
            BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("statusled_bright", "状态灯亮", "5E 0F 00 06 01 74", new ArrayList<Double>(), 1, 1, 0, 0,0,0,0);
            BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
        }else if (id == R.id.led_blight) {
            BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("statusled_blink", "状态灯闪", "5E 0F 00 06 02 75", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
            BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
        }else if (id == R.id.set_pcr_tempreture) {
            double tempreture = Double.parseDouble(pcr_tempreture.getText().toString());
            BoWeiPCRStepEntity pCRStepEntityBoWei = new BoWeiPCRStepEntity("pcr_temperature_set", "PCR温度设定", "", new ArrayList<Double>(), 1, 1, 0, tempreture, 0, 0, 0);
            byte[] pcr_temperature_message = { 0x5E ,0x06, 0x00, 0x0E, 0x17,0x70, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
            int pcr_temperature_int = (int)(tempreture * 100);
            pcr_temperature_message[4] = (byte)((pcr_temperature_int >> 8) & 0xff);
            pcr_temperature_message[5] = (byte)(pcr_temperature_int & 0xff);
            int crc = 0;
            for (int i = 0; i < pcr_temperature_message.length-1; i++)
            {
                crc = crc + pcr_temperature_message[i]&0xFF;
            }
            pcr_temperature_message[pcr_temperature_message.length - 1] = (byte)(crc & 0xFF);
            pCRStepEntityBoWei.setPCRCmd(NumUtils.HexListToString16(pcr_temperature_message));
            BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pCRStepEntityBoWei);
        }else if (id == R.id.motor_reset) {
            BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("motor_reset", "扫描电机复位", "5E 07 00 06 13 7E", new ArrayList<Double>(), 1, 1,0,0, 0, 0, 0);
            BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
        }else if (id == R.id.motor_scan) {
            BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("motor_scan", "扫描荧光（4通道FAM、CY5、HEX、ROX）", "5E 07 00 0B 15 1D 4C 01 32 C8 E9", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
            BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
        }/*else if (id == R.id.get_fluro) {
            BoWeiPCRStepEntity pcrDebugEntity = new BoWeiPCRStepEntity("pcr_fluorescence_get", "获取PCR数据（4通道FAM、CY5、HEX、ROX）", "5E 0C 00 07 01 01 73", new ArrayList<Double>(), 1, 1, 0, 0, 0, 0, 0);
            BoWeiConnectManager.getInstance().sendSerialCmdMsg_NoAck(pcrDebugEntity);
        }*/else if (id == R.id.config_pcr) {
            Intent intent = new Intent(BoweiTestActivity.this, ShowConfigActivity.class);
            startActivity(intent);
        }else if (id == R.id.load_pcr) {
            //////////////////////////
//            FileInputStream inputStream = null;
            try {

//                inputStream = new FileInputStream(this.boweiExperimentFile);
//
//                String strJson = fileToStr(inputStream);
//
//                if (null == strJson || strJson == "") {
//                    return;
//                }else{
//
//                }
//
//                Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {
//
//                    @Override
//                    public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
//                        if (src == src.longValue())
//                            return new JsonPrimitive(src.longValue());
//                        return new JsonPrimitive(src);
//                    }
//                }).create();
//                Experiment experiment = gson.fromJson(strJson, new TypeToken<Experiment>() {}.getType());
                String filePath = this.boweiExperimentFile.getAbsolutePath();
                Log.i("AAA","filePath"+filePath);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Experiment open_experiment = BoWeiService.getInstance().openExperiment(filePath);

                        //BoweiJni boweiJni = new BoweiJni();
                        BoweiJni.analysisExperimentLoessFilter(open_experiment);
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
                /*Experiment experiment = BoWeiService.getInstance().openExperiment(this.boweiExperimentFile.getAbsolutePath());

                BoweiJni boweiJni = new BoweiJni();
                String text = boweiJni.analysisExperimentLoessFilter(experiment);*/

//                inputStream.close();

/////////////////////临时保存experiment文件
//                experiment.setProject(this.project);
//
               /* String content = GsonUtil.toJson(experiment,false);
//
                String fileName = this.boweiExperimentFile.getName().replace(".boweipcrResult","") + "_result" + ".boweipcrResult";
                File resultFile = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/BoWeiResult/" + fileName);
                resultFile.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/BoWeiResult/" + fileName);
                outputStream.write(content.getBytes());
                outputStream.flush();
                outputStream.close();*/



            } catch (Exception e) {
                e.printStackTrace();
                Log.e("BoweiTestActivity",e.getMessage());
            }finally {
//                if(inputStream != null){
//                    try{
//                        inputStream.close();
//                        inputStream = null;
//                    }catch (Exception e){
//                    }
//                }
            }

        } else if (id == R.id.create_hex) {
            if(this.project != null){
                createPCRStepEntityList(project);
                HexAdapter hexAdapter = new HexAdapter(BoWeiPCRStepEntityList);
                listview_hex.setAdapter(hexAdapter);
            }
        }else if (id == R.id.run_hex) {
            //开始运行PCR
           /* if(BoWeiPCRAsyncTask.getInstance().ismIsrunning()){
                Toast.makeText(this.getApplicationContext(), "PCR已经在运行，请停止", Toast.LENGTH_LONG).show();
                return;
            }
            if(this.project != null){
                BoWeiPCRAsyncTask.current_template = fileName;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    //根据不同的api采用并行模式进行开启
                    //////启动流程的API
                    BoWeiPCRAsyncTask.getInstance().executeOnExecutor(THREAD_POOL_EXECUTOR, this.project);
                } else {
                    BoWeiPCRAsyncTask.getInstance().execute(this.project);
                }
                onChronometerStart();
            }else{
                Toast.makeText(this.getApplicationContext(), "请先加载PCR模板", Toast.LENGTH_LONG).show();
            }*/
            if(null != runExParam){
                RunBiowayResult runBiowayResult = BoWeiService.getInstance().runExperiment(runExParam);
            }


        }else if (id == R.id.stop_hex) {
         //   BoweiJni testJni = new BoweiJni();
         //   log.append(testJni.getMessage());
            onChronometerStop();
            if(BoWeiPCRAsyncTask.getInstance().isRunning()){
                BoWeiPCRAsyncTask.getInstance().setInteruppt(true);
                BoWeiPCRAsyncTask.getInstance().stopMachine();
            }else{
                Toast.makeText(this.getApplicationContext(), "PCR已停止工作", Toast.LENGTH_LONG).show();
                BoWeiPCRAsyncTask.getInstance().setInteruppt(true);
                BoWeiPCRAsyncTask.getInstance().stopMachine();
            }
        }else if (id == R.id.printter) {
            printDocument("null.pdf");
        }
    }




    public boolean isCscidDevice(UsbDevice device) {
        if (device == null) {
            return false;
        }
        if (device.getInterfaceCount() == 0) {
            return false;
        }

        String name = device.getProductName();
        if (name.contains("HP LaserJet")) {
            return true;
        }
        return false;
    }

    private void printDocument(String filename) {
        // 准备要打印的位图列表
        List<Bitmap> pages = new ArrayList<>();

        // 添加页面（这里只是示例，你需要根据实际情况创建位图）
        try {
            // 从assets加载PDF并转换为位图
            PdfRenderer pdfRenderer = new PdfRenderer(getSeekableFileDescriptor(filename));
            for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);

                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth(),
                        page.getHeight(),
                        Bitmap.Config.ARGB_8888);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                pages.add(bitmap);

                page.close();
            }
            pdfRenderer.close();

            // 创建打印助手并开始打印
            PdfPrintHelper printHelper = new PdfPrintHelper(this);
            printHelper.printPdf(pages, "打印文档.pdf");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从assets获取PDF文件的FileDescriptor
    private ParcelFileDescriptor getSeekableFileDescriptor(String assetName) throws IOException {
        File tempFile = new File(getCacheDir(), assetName);
        // 复制assets文件到缓存
        copyAssetToCache(assetName, tempFile);

        return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private void copyAssetToCache(String assetName, File outFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getAssets().open(assetName);
            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }




    /**start/restart chronometer**/
    private void onChronometerStart(){
        if(!isFlag){
            isFlag = !isFlag;
            mChronTimer.setBase(SystemClock.elapsedRealtime());
            if(mChronTimer.getText().equals("00:00")){
                mChronTimer.setText("00:00:00");
            }
            mChronTimer.start();
        }
    }

    /**stop chronometer**/
    private void onChronometerStop(){
        if(isFlag && null!= mChronTimer){
            isFlag = !isFlag;
            mChronTimer.stop();
            //mRecordTime = SystemClock.elapsedRealtime();
        }
    }
    /**reset chronometer*/
    private void onChronometerReset(){
        mChronTimer.stop();
        mChronTimer.setBase(SystemClock.elapsedRealtime());
        //mRecordTime = 0;
        if(mChronTimer.getText().equals("00:00")){
            mChronTimer.setText("00:00:00");
        }
        isFlag = false;
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






    private void createPCRStepEntityList(Project boweiProject){

        BoWeiPCRStepEntityList = new ArrayList<>();

        BoWeiPCRStepEntityList.clear();

        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_StatusLedBlink_Message());
        ///没有热盖
        //PCRStepEntity.PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Hotlid_Temperature_Message(105));

        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Moter_Reset_Message(0, 1, 0));

        double temperturePre = 0.0f;

        Double a = Double.valueOf(1.0f);

        for (int i = 0 ; i < boweiProject.getProgram().getSections().size(); i++)
        {

            if(boweiProject.getProgram().getSections().get(i) instanceof SectionAMP)
            {
                //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Moter_Reset_AMP_Message(i + 1, 1, 1));

                SectionAMP sectionAMP = (SectionAMP) boweiProject.getProgram().getSections().get(i);

                if(sectionAMP.getCycles() == 1 || sectionAMP.getCycles() == 0)
                {
                    for (int k = 0; k < sectionAMP.getItems().size(); k++)
                    {
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_PCR_SETTemperature_AMP_Message(sectionAMP.getItems().get(k).getTemperature() , sectionAMP.getItems().get(k).getHoldTime(),i + 1, sectionAMP.getName(),1,k + 1));
                        if (i == 0 && k == 0)
                        {
                            if (sectionAMP.getItems().get(k).getTemperature() >= 30 )
                            {
                                BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                            }
                            else
                            {
                                BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                            }
                        }
                        else
                        {
                            if (sectionAMP.getItems().get(k).getTemperature() > temperturePre)
                            {
                                BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                            }
                            else if(sectionAMP.getItems().get(k).getTemperature() < temperturePre)
                            {
                                BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                            }
                        }


                        temperturePre = sectionAMP.getItems().get(k).getTemperature();



                        if (sectionAMP.getItems().get(k).getIsAcquisition())
                        {
                            //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Moter_Reset_AMP_Message(i + 1, 1, k + 1));
                            /////增加5次采样及滤波
                            //for (int oo = 0;oo < 5; oo++)
                            //{
                            //    if (oo == 0)
                            //    {
                            //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, 1, k + 1));
                            //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message_Start(i + 1, 1, k + 1));
                            //    }else if(oo == 4)
                            //    {
                            BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, sectionAMP.getName(),1, k + 1));
                            BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message_End(i + 1, sectionAMP.getName(),1, k + 1));
                            //    }else
                            //    {
                            //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, 1, k + 1));
                            //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message(i + 1, 1, k + 1));
                            //    }
                            //}
                        }
                    }
                }
                else
                {
                    //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Loopstart_Message(sectionAMP.Cycles));
                    for (int j = 0; j < sectionAMP.getCycles(); j++)
                    {
                        for (int k = 0; k < sectionAMP.getItems().size(); k++)
                        {
                            BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_PCR_SETTemperature_AMP_Message(sectionAMP.getItems().get(k).getTemperature(), sectionAMP.getItems().get(k).getHoldTime(),i + 1, sectionAMP.getName(),j + 1, k + 1));
                            if (i == 0 && k == 0)
                            {
                                if (sectionAMP.getItems().get(k).getTemperature() >= 30)
                                {
                                    BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                                }
                                else
                                {
                                    BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                                }
                            }
                            else
                            {
                                if (sectionAMP.getItems().get(k).getTemperature() > temperturePre)
                                {
                                    BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                                }
                                else if(sectionAMP.getItems().get(k).getTemperature() < temperturePre)
                                {
                                    BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                                }
                            }


                            temperturePre = sectionAMP.getItems().get(k).getTemperature();


                            if (sectionAMP.getItems().get(k).getIsAcquisition())
                            {
                                //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Moter_Reset_AMP_Message(i + 1, j + 1, k + 1));
                                //for (int oo = 0; oo < 5; oo++)
                                //{
                                //    if (oo == 0)
                                //    {
                                //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, j + 1, k + 1));
                                //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message_Start(i + 1, j + 1, k + 1));
                                //    }
                                //    else if (oo == 4)
                                //    {
                                BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, sectionAMP.getName(),1, k + 1));
                                BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message_End(i + 1, sectionAMP.getName(),j + 1, k + 1));
                                //    }
                                //    else
                                //    {
                                //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_AMP_Message(i + 1, j + 1, k + 1));
                                //        PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetAMPFluorescenceData_Message(i + 1, 1, k + 1));
                                //    }

                                //}

                            }
                        }
                    }

                    //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Loopend_Message(sectionAMP.Cycles));
                }
            }
            else if (boweiProject.getProgram().getSections().get(i) instanceof SectionMelt)
            {

                SectionMelt sectionMelt = (SectionMelt) boweiProject.getProgram().getSections().get(i);
                String sectionName = sectionMelt.getName();//熔解程序段
                double totalSteps_Melt;
                //double totalSteps_Melt = ((sectionMelt.getEndTemp() - sectionMelt.getStartTemp()) / (sectionMelt.getInterval() * 0.1f)) + 1.0f ;
                if(sectionMelt.getInterval() < 0.1f){
                    totalSteps_Melt = ((sectionMelt.getEndTemp() - sectionMelt.getStartTemp()) / (0.1f)) + 1.0f ;
                }else{
                    totalSteps_Melt = ((sectionMelt.getEndTemp() - sectionMelt.getStartTemp()) / (sectionMelt.getInterval())) + 1.0f ;
                }


                //if (sectionMelt.getScanMode() == MeltScanMode.Step)
                if (true)
                {

                    BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_PCR_SETTemperature_AMP_Message(sectionMelt.getStartTemp(), 30 , i + 1, sectionMelt.getName(),1, 0));
                    //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Scan_Melt_Message(i + 1, 1, 0));////获取荧光点数值
                    if (sectionMelt.getStartTemp() > temperturePre)
                    {
                        BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setHeat(true);
                    }
                    else if(sectionMelt.getStartTemp() < temperturePre)
                    {
                        BoWeiPCRStepEntityList.get(BoWeiPCRStepEntityList.size()-1).setCool(true);
                    }
                    temperturePre = sectionMelt.getEndTemp();
                    BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Moter_Reset_Melt_Message(i + 1, sectionMelt.getName(),1, 0));
                    for (int k = 0; k < totalSteps_Melt; k++)
                    {
                        double temperature = sectionMelt.getStartTemp() + k * sectionMelt.getInterval();
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_PCR_SETTemperature_Melt_Message(temperature, 0.5, i + 1,sectionMelt.getName(), 1, k + 1));/////需要停留多长时间？？？？？
                        //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Moter_Reset_Melt_Message(i + 1, 1, k + 1));
                        //PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_GetCurrentPCRTemperature_Message_Pre(i + 1, 1, k + 1));////获取前温度
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Scan_Melt_Message(i + 1, sectionMelt.getName(),1, k + 1));////获取荧光点数值
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_GetCurrentPCRTemperature_Message_Post(i + 1,sectionMelt.getName(), 1, k + 1));////获取后温度
                        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_GetMeltFluorescenceData_Message(i + 1, sectionMelt.getName(),1, k + 1));
                    }
                }
            }
        }
        //关闭热盖
        ////PCRStepEntity.PCRStepEntityList.Add(BoWeiTaiKeHexUtils.Create_Hotlid_Temperature_Message(200));
        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_Moter_Reset_Message(0,0,0));
        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_StopPeltier_Message());
        BoWeiPCRStepEntityList.add(BoWeiTaiKeHexUtils.Create_StatusLedBright_Message());
        for (int t = 0; t < BoWeiPCRStepEntityList.size(); t++)
        {
            BoWeiPCRStepEntityList.get(t).setMemoryNote(t + "");
        }
    }

    private ExperimentParam runExParam = new ExperimentParam();

    private void openSLANExFile(String fileName){
        //打开文件夹
        if(!StringUtil.isEmpty(fileName)){
            ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
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
                        }
                    }
                }
            }
            runExParam.setPath(FileUtils.CONFIG_SLAN_PATH);
            runExParam.setName(fileName);
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoWeiRecvDataInfo boWeiRecvDataInfo) {
        if(boWeiRecvDataInfo.getCmd().contains("set_heartbeat")){
            return;
        }
        log.append(boWeiRecvDataInfo.getRecvData());
        log.scrollTo(0,0);
        if(boWeiRecvDataInfo.getCmd().contains("get_current_temperature")){
            tempreture1.setText(NumberUtils.doubleto2String(boWeiRecvDataInfo.getTemperature().get(0).getTemperature()));
            tempreture2.setText(NumberUtils.doubleto2String(boWeiRecvDataInfo.getTemperature().get(1).getTemperature()));
            tempreture3.setText(NumberUtils.doubleto2String(boWeiRecvDataInfo.getTemperature().get(2).getTemperature()));
        }
        if(boWeiRecvDataInfo.getCmd().contains("pcr_fluorescence_get")) {
            CustomAdapter adapter = new CustomAdapter(boWeiRecvDataInfo.getFluorescence());
            listview_flu.setAdapter(adapter);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoWeiPCRProcedureInfoEvent boWeiPCRProcedureInfoEvent) {
        current_segment.setText(boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getSectionName()+ "");
        current_cycle.setText(boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getCurrentCycle()+ "");
        current_step.setText(boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getCurrentStep()+ "");
        current_action.setText(boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getMemoryNote() +
                "-" + boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getName() + "-" +
                boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getDescription() + "-" +
                boWeiPCRProcedureInfoEvent.getCurrentBoWeiPCRStepEntity().getPCR_temperature());

        current_step_remaintime.setText(boWeiPCRProcedureInfoEvent.getHoldTime()+"");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(BoWeiProcedureFinishEvent procedureFinishEvent) {
//        if(procedureFinishEvent.getTemplateName().contains("reset")){
        if(true){
            if(procedureFinishEvent.getStatus().equals("finish")){
                showNoticeDialog("PCR完成","PCR完成");
            }else if(procedureFinishEvent.getStatus().equals("finish error")){
                Toast.makeText(BoweiTestActivity.this,"BoweiTestActivity  " + "finish error",Toast.LENGTH_LONG).show();
                showNoticeDialog("PCR运行异常","PCR运行异常，请检查!");
            }
            onChronometerStop();
        }
    }

    public void showNoticeDialog(String title, String message){
        if(null != mNoticeDialog){
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        if(null == mNoticeDialog){
            mNoticeDialog = new WarnDialog(this);
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
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });
            mNoticeDialog = builder.create();
            mNoticeDialog.show();
        }
    }

    /**
     * 打开保存的结果文件
     */
    private void openBiowayExperiment(Experiment experiment){
        Log.i("AAA","openBiowayExFile");
        if(experiment != null){
            BiowayManager.getInstance().parseExperimentResult(experiment,null,null);
        }

    }


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
    private boolean isOneCheck,isTwoCheck,isThreeCheck,isFourCheck,
            isFiveCheck,isSixCheck,isSevenCheck,isEightCheck = false;
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
                MessageEvent resultMsg = new MessageEvent();
                resultMsg.setMsgType(MsgType.MSG_SHOW_RESULT);
                EventBus.getDefault().postSticky(resultMsg);
            }

            @Override
            public void onSaveJsonResult(boolean result,String message) {

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

        isOneCheck = isTwoCheck = isThreeCheck = isFourCheck = isFiveCheck = isSixCheck = isSevenCheck = isEightCheck = false;
        mOneTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isOneCheck = !isOneCheck;
                if (!isOneCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isOneCheck, (TextView) view);
                updateWellResult();
            }
        });
        mTwoTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isTwoCheck = !isTwoCheck;
                if (!isTwoCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isTwoCheck, (TextView) view);
                updateWellResult();
            }
        });
        mThreeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isThreeCheck = !isThreeCheck;
                if (!isThreeCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isThreeCheck, (TextView) view);
                updateWellResult();
            }
        });
        mFourTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFourCheck = !isFourCheck;
                if (!isFourCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isFourCheck, (TextView) view);
                updateWellResult();
            }
        });
        mFiveTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFiveCheck = !isFiveCheck;
                if (!isFiveCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isFiveCheck, (TextView) view);
                updateWellResult();
            }
        });
        mSixTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSixCheck = !isSixCheck;
                if (!isSixCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isSixCheck, (TextView) view);
                updateWellResult();
            }
        });
        mSevenTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isSevenCheck = !isSevenCheck;
                if (!isSevenCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isSevenCheck, (TextView) view);
                updateWellResult();
            }
        });
        mEightTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isEightCheck = !isEightCheck;
                if (!isEightCheck) {
                    if (isAllCheck) {
                        isAllCheck = false;
                        setHodeState(isAllCheck, mAllHoleTv);
                    }
                } else {
                    checkAllHole();
                }
                setHodeState(isEightCheck, (TextView) view);
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
    List<PeaktypeOptimization> optimizations = new ArrayList<PeaktypeOptimization>();
    private void secondAnalayse(){
        if(null != configTargets && configTargets.size()>0){
            showLoadingDialog("二次分析中...");
            //将模板下的判断文件传入
            new Thread(new Runnable() {
                @Override
                public void run() {
                    BiowayManager.getInstance().secondSLANRunExAnalyse(configTargets, optimizations,0,null,null,PcrApplication.ex_startTime);
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
        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck) {
            if(!isAllCheck){
                isAllCheck = true;
                setHodeState(isAllCheck, mAllHoleTv);
            }
        }
    }

    private void setAllHoleCheck(){
        if(isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck){
            isAllCheck = true;
            setHodeState(isAllCheck,mAllHoleTv);
        }else {
            isOneCheck = true;isTwoCheck = true;isThreeCheck = true;isFourCheck = true;
            isFiveCheck = true;isSixCheck = true;isSevenCheck = true;isEightCheck = true;
            isAllCheck = true;
            setHodeState(isOneCheck,mOneTv);
            setHodeState(isTwoCheck, mTwoTv);
            setHodeState(isThreeCheck, mThreeTv);
            setHodeState(isFourCheck, mFourTv);
            setHodeState(isFiveCheck, mFiveTv);
            setHodeState(isSixCheck, mSixTv);
            setHodeState(isSevenCheck, mSevenTv);
            setHodeState(isEightCheck, mEightTv);
        }

    }


    private void setAllHoleUnCheck(){
        isOneCheck = false;isTwoCheck = false;isThreeCheck = false;
        isFourCheck = false;isFiveCheck = false;isSixCheck = false;
        isSevenCheck = false; isEightCheck = false;
        setHodeState(isOneCheck,mOneTv);
        setHodeState(isTwoCheck, mTwoTv);
        setHodeState(isThreeCheck, mThreeTv);
        setHodeState(isFourCheck, mFourTv);
        setHodeState(isFiveCheck, mFiveTv);
        setHodeState(isSixCheck, mSixTv);
        setHodeState(isSevenCheck, mSevenTv);
        setHodeState(isEightCheck, mEightTv);
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
    //
    private void getClickAMPCurve(){
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

    private void getClickMeltCurve(){
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

        Log.i("AAA", "getClickMeltCurveResult" +mMeltCurveList.size() );
    }

    private void getClickPeakCurve(){
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


        String resultFilePath = "/sdcard/PCR/temp2.boweipcrResult";
       // String resultFilePath = "/mnt/sdcard/PCR/BoWeiResult/产物标准熔曲(005).boweipcrResult";
        Experiment experiment = BoWeiService.getInstance().openExperiment(resultFilePath);
        BoweiJni.analysisExperimentLoessFilter(experiment);
        Log.i("AAA","experiment" + experiment);
        //保存
        /*if(experiment != null){
            BoWeiService.getInstance(getApplicationContext())
                    .saveExperiment(experiment,"aaa","/sdcard/PCR/BoWeiResult/");
        }*/

        //解析文件显示结果
        if(experiment != null){
            BiowayManager.getInstance().parseExperimentResult(experiment,null,null);
        }else{
            Toast.makeText(getApplicationContext(),"没有结果文件",Toast.LENGTH_LONG);
        }

        //参数设置

    }



    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(MessageEvent messageEvent) {

        switch (messageEvent.getMsgType()) {
            case MsgType.MSG_SHOW_RESULT:
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
                //弹出对话框提示是否需要保存
                //onSaveSecondParseResult();
                break;
        }
    }

    /**
     * 设置所有的孔位位选定状态
     */
    private void checkWellState(){

        for(BiowayCurveList curveList : showCurveList){

            if(curveList.getWellNum().equals("A1")){
                isOneCheck = true;
                setHodeState(isOneCheck, mOneTv);
            }else if(curveList.getWellNum().equals("A2")){
                isTwoCheck = true;
                setHodeState(isTwoCheck, mTwoTv);
            }else if(curveList.getWellNum().equals("A3")){
                isThreeCheck = true;
                setHodeState(isThreeCheck, mThreeTv);
            } else if(curveList.getWellNum().equals("A4")){
                isFourCheck = true;
                setHodeState(isFourCheck, mFourTv);
            } else if(curveList.getWellNum().equals("A5")){
                isFiveCheck = true;
                setHodeState(isFiveCheck, mFiveTv);
            } else if(curveList.getWellNum().equals("A6")){
                isSixCheck = true;
                setHodeState(isSixCheck, mSixTv);
            } else if(curveList.getWellNum().equals("A7")){
                isSevenCheck = true;
                setHodeState(isSevenCheck, mSevenTv);
            } else if(curveList.getWellNum().equals("A8")){
                isEightCheck = true;
                setHodeState(isEightCheck, mEightTv);
            }
        }

        if (isOneCheck && isTwoCheck && isThreeCheck && isFourCheck
                && isFiveCheck && isSixCheck && isSevenCheck && isEightCheck) {
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
}