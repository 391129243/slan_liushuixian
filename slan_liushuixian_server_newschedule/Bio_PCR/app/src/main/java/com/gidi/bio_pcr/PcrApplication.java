package com.gidi.bio_pcr;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gidi.bio_pcr.WintersUtils.entity.RNARunningMessage;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.slan.SLANServiceManager;
import com.gidi.bio_pcr.util.CrashHandler;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LocaleUtils;
import com.gidi.bio_pcr.util.LogcatHelper;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.DeviceType;
import com.hzmct.enjoysdk.api.EnjoySDK;
import com.hzmct.enjoysdk.support.EnjoySDKSupport;
import com.hzmct.enjoysdk.transform.McStateCode;
import com.mc.android.mcethernet.McEthernetConfig;
import com.tencent.mmkv.MMKV;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class PcrApplication extends Application {
    private static final String TAG = "PcrApplication";
    public static Context mAppContext = null;
    private static PcrApplication mApplication;
    public static String operator;//当前用户
    public static String rootTempName;//当前模板
    public static String ex_startTime;
    public static long  totalTime;
    public static long  runningTime;

    public static String rna_interuppt_time;
    public static boolean isUsbInsert = false;

    public static String rnaVersion = "V5";

    private static String uartname = "ttyUSB";
    private static String uartnameBoWei = "ttyUSB";

    public static boolean is_RecRunningMesaage = false;
    public static boolean isHasSecondRNA = true;
    public static boolean is_RNA_connect = false;
    public static boolean is_SLAN_PCR_connect = false;// PCR状态可删掉
    public static int experimentType = 0;//0：全流程,1,singleRNA,2:singlePCR
    public static RNARunningMessage rNARunningMessage = new RNARunningMessage(0, "", "", 0, "", "");

    public static boolean isBootUp = false;

    public static boolean isAdmin = false;

    public int count = 0;
    UIHandler mUIHandler;

    public static SLANService slanService = null;


    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = this;
        mApplication = this;
        String rootDir = MMKV.initialize(this);
        Log.i("MMKV", "MMKV root directory: " + rootDir);
//        EnjoySDKHelper.initSecure(this);
        initSecure();
        mUIHandler = new UIHandler();

        // 初始化模组启用管理器（必须在 WSSModuleManager 之前初始化）
        com.gidi.bio_pcr.util.ModuleEnableManager.getInstance(this);
        Log.i("PcrApplication", "ModuleEnableManager 初始化完成");

        // 初始化 LogcatHelper（必须在 start() 之前调用）
        LogcatHelper.getInstance(getApplicationContext()).init(getApplicationContext());
        Log.i("PcrApplication", "LogcatHelper 初始化完成");

        //启动日志收集功能
        mUIHandler.postDelayed(logrunnable, 2000);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Thread.sleep(2000);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//                LogcatHelper.getInstance(getApplicationContext()).start();
//            }
//        }).start();

        CrashHandler crashHandler = CrashHandler.getInstance(this);
        crashHandler.init(this);
           Locale _UserLocale = LocaleUtils.getUserLocale(this);
           LocaleUtils.updateLocale(this, _UserLocale);
        LiteORMCtrlMgr.getInstance();
//        GetTtyUsbPath();
//        Log.i(TAG, "uartname--" + uartname);
//       if (!uartname.equals("ttyUSB")) {
//
//            new Thread(new Runnable() {
//                @Override
//              public void run() {
//                    try {
//                        Thread.sleep(3000);
//                   } catch (InterruptedException e) {
//                        //throw new RuntimeException(e);
//                    }
//                    ConnectManager.getInstance().initConnection(mAppContext, uartname, new ConnectCallback() {
//
//                        @Override
//                       public void serialConnectSuccess(Object object) {
//                           Log.i(TAG, "RNA connect sucess");
//                            is_RNA_connect = true;
//                        }
//
//                        @Override
//                        public void serialConnectFail(Object object) {
//                            Log.i(TAG, "RNA connect fail");
//                           is_RNA_connect = false;
//                       }
//                   });
//                }
//           }).start();
//        } else {
//            Log.i(TAG, "RNA_App not exist");
//        }

        /*if (!uartnameBoWei.equals("ttyUSB")) {

            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(1, 1, 1, 1, 1, 1, 1));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(2, 2, 2, 2, 2, 2, 2));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(3, 3, 3, 3, 3, 3, 3));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(4, 4, 4, 4, 4, 4, 4));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(5, 5, 5, 5, 5, 5, 5));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(6, 6, 6, 6, 6, 6, 6));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(7, 7, 7, 7, 7, 7, 7));
            BoWeiPCRStatus.pCRCurrentTupeDataList.add(new BoWeiPCRTupeData(8, 8, 8, 8, 8, 8, 8));

            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(1, 1));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(2, 2));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(3, 3));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(4, 4));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(5, 5));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(6, 6));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(7, 7));
            BoWeiPCRStatus.pCRCurrentTemperatureList.add(new BoWeiPCRTupeTempretureData(8, 8));

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        //throw new RuntimeException(e);
                    }
                    BoWeiConnectManager.getInstance().initConnection(mAppContext, uartnameBoWei, new BoWeiConnectCallback() {
                        @Override
                        public void serialConnectSuccess(Object object) {
                            Log.i(TAG, "Bioway connect sucess");
                            is_SLAN_PCR_connect = BoWeiConnectManager.is_BoWei_connect = true;
                        }

                        @Override
                        public void serialConnectFail(Object object) {
                            Log.i(TAG, "Bioway connect fail");
                            is_SLAN_PCR_connect = BoWeiConnectManager.is_BoWei_connect = false;
                        }
                    });
                }
            }).start();
        } else {
            Log.i(TAG, "Bioway not exist");
        }*/

//        BoWeiService.getInstance().init(getApplicationContext());
//        if(null != BoWeiService.getInstance()){
//            BoWeiService.getInstance().setDeviceStateListener(new DeviceStateListener() {
//                @Override
//                public void getDeviceInfo(DeviceState deviceState) {
//                    Log.i(TAG, "getDeviceInfo： " + deviceState);
//                 //   Log.i(TAG, "getDeviceInfo： " + deviceState.getDesc());
////                    if (deviceState == DeviceState.Error) {
////                        //通信出错
////                        count++;
////                        if (count >= 0) {
////                            PcrApplication.is_SLAN_PCR_connect = false;
////                            count = 0;
////                        }
////                    } else {
////                        PcrApplication.is_SLAN_PCR_connect = true;
////                        count = 0;
////                    }
//                    MessageEvent stateMsg = new MessageEvent();
//                    stateMsg.setMsgType(MsgType.MSG_PCR_CONNECT_STATE);
//                    EventBus.getDefault().postSticky(stateMsg);
//                }
//            });
//        }

      //  Log.i(TAG, "PcrApplication.bowei" + PcrApplication.is_SLAN_PCR_connect);

        //宏石初始化
        //宏石SDK初始化
        String deviceTypeStr = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.SLAN_DEVICE_TYPE,"SLAN_16H");
        DeviceType deviceType = SLANServiceManager.fromString(deviceTypeStr, DeviceType.SLAN_16H);
        try {
            SLANServiceManager.getInstance(getApplicationContext()).initSLANService(deviceType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //宏石SDK初始化
        slanService = SLANServiceManager.getInstance(getApplicationContext()).getSlanService();
        File commonHexDir = new File(FileUtils.COMMON_HEX_PATH);
        if (!commonHexDir.exists()) {
            commonHexDir.mkdirs();
        }
        File singlePcrDir = new File(FileUtils.SINGLE_PCR_TEMPLATE_PATH);
        if(!singlePcrDir.exists()){
            singlePcrDir.mkdirs();
        }
        File singleRNADir = new File(FileUtils.SINGLE_RNA_TEMPLATE_PATH);
        if(!singleRNADir.exists()){
            singleRNADir.mkdirs();
        }
        File fullProcessDir = new File(FileUtils.FULL_PROCESS_TEMPLATE_PATH);
        if(!fullProcessDir.exists()){
            fullProcessDir.mkdirs();
        }
        boolean isSecret = PreferenceUtil.getInstance(getApplicationContext())
                .getBooleanValue(SharedConstant.SECRET_SETTING,false);

        if(isSecret) {
            File SECRET3_flu_repeat_fam_PATH = new File(FileUtils.SECRET1_3_flu_repeat_fam_PATH);
            File SECRET3_flu_repeat_rox_PATH = new File(FileUtils.SECRET2_3_flu_repeat_rox_PATH);
            File SECRET3_flu_repeat_hex_PATH = new File(FileUtils.SECRET3_3_flu_repeat_hex_PATH);
            File SECRET3_flu_repeat_cy5_PATH = new File(FileUtils.SECRET4_3_flu_repeat_cy5_PATH);
            File SECRET5_sample_repeat_high_1_PATH = new File(FileUtils.SECRET5_5_sample_repeat_high_1_PATH);
            File SECRET5_sample_repeat_high_2_PATH = new File(FileUtils.SECRET6_5_sample_repeat_high_2_PATH);
            File SECRET5_sample_repeat_median_1_PATH = new File(FileUtils.SECRET7_5_sample_repeat_median_1_PATH);
            File SECRET5_sample_repeat_median_2_PATH = new File(FileUtils.SECRET8_5_sample_repeat_median_2_PATH);
            File SECRET5_sample_repeat_low_1_PATH = new File(FileUtils.SECRET9_5_sample_repeat_low_1_PATH);
            File SECRET5_sample_repeat_low_2_PATH = new File(FileUtils.SECRET10_5_sample_repeat_low_2_PATH);
            File SECRET6_sample_linear_AMP_1_PATH = new File(FileUtils.SECRET11_6_sample_linear_AMP_PATH);
            File SECRET6_sample_linear_AMP_2_PATH = new File(FileUtils.SECRET12_6_sample_linear_AMP_PATH);
            File SECRET3_sample_linear_AMP_3_PATH = new File(FileUtils.SECRET13_3_sample_linear_AMP_PATH);
            File SECRET8_flu_linear_fam_PATH = new File(FileUtils.SECRET14_8_flu_linear_fam_PATH);
            File SECRET7_flu_linear_fam_PATH = new File(FileUtils.SECRET15_7_flu_linear_fam_PATH);
            File SECRET8_flu_linear_rox_PATH = new File(FileUtils.SECRET16_8_flu_linear_rox_PATH);
            File SECRET7_flu_linear_rox_PATH = new File(FileUtils.SECRET17_7_flu_linear_rox_PATH);
            File SECRET8_flu_linear_hex_PATH = new File(FileUtils.SECRET18_8_flu_linear_hex_PATH);
            File SECRET7_flu_linear_hex_PATH = new File(FileUtils.SECRET19_7_flu_linear_hex_PATH);
            File SECRET8_flu_linear_cy5_PATH = new File(FileUtils.SECRET20_8_flu_linear_cy5_PATH);
            File SECRET7_flu_linear_cy5_PATH = new File(FileUtils.SECRET21_7_flu_linear_cy5_PATH);
            File SECRET5_extraction_consistence_high_1_PATH = new File(FileUtils.SECRET22_5_extraction_consistence_high_1_PATH);
            File SECRET5_extraction_consistence_high_2_PATH = new File(FileUtils.SECRET23_5_extraction_consistence_high_2_PATH);
            File SECRET5_extraction_consistence_median_1_PATH = new File(FileUtils.SECRET24_5_extraction_consistence_median_1_PATH);
            File SECRET5_extraction_consistence_median_2_PATH = new File(FileUtils.SECRET25_5_extraction_consistence_median_2_PATH);
            File SECRET5_extraction_consistence_low_1_PATH = new File(FileUtils.SECRET26_5_extraction_consistence_low_1_PATH);
            File SECRET5_extraction_consistence_low_2_PATH = new File(FileUtils.SECRET27_5_extraction_consistence_low_2_PATH);

            if (!SECRET3_flu_repeat_fam_PATH.exists()) {
                SECRET3_flu_repeat_fam_PATH.mkdirs();
            }
            if (!SECRET3_flu_repeat_rox_PATH.exists()) {
                SECRET3_flu_repeat_rox_PATH.mkdirs();
            }
            if (!SECRET3_flu_repeat_hex_PATH.exists()) {
                SECRET3_flu_repeat_hex_PATH.mkdirs();
            }
            if (!SECRET3_flu_repeat_cy5_PATH.exists()) {
                SECRET3_flu_repeat_cy5_PATH.mkdirs();
            }
            if (!SECRET5_sample_repeat_high_1_PATH.exists()) {
                SECRET5_sample_repeat_high_1_PATH.mkdirs();
            }
            if (!SECRET5_sample_repeat_high_2_PATH.exists()) {
                SECRET5_sample_repeat_high_2_PATH.mkdirs();
            }
            if (!SECRET5_sample_repeat_median_1_PATH.exists()) {
                SECRET5_sample_repeat_median_1_PATH.mkdirs();
            }
            if (!SECRET5_sample_repeat_median_2_PATH.exists()) {
                SECRET5_sample_repeat_median_2_PATH.mkdirs();
            }
            if (!SECRET5_sample_repeat_low_1_PATH.exists()) {
                SECRET5_sample_repeat_low_1_PATH.mkdirs();
            }
            if (!SECRET5_sample_repeat_low_2_PATH.exists()) {
                SECRET5_sample_repeat_low_2_PATH.mkdirs();
            }
            if (!SECRET6_sample_linear_AMP_1_PATH.exists()) {
                SECRET6_sample_linear_AMP_1_PATH.mkdirs();
            }
            if (!SECRET6_sample_linear_AMP_2_PATH.exists()) {
                SECRET6_sample_linear_AMP_2_PATH.mkdirs();
            }
            if (!SECRET3_sample_linear_AMP_3_PATH.exists()) {
                SECRET3_sample_linear_AMP_3_PATH.mkdirs();
            }
            if (!SECRET8_flu_linear_fam_PATH.exists()) {
                SECRET8_flu_linear_fam_PATH.mkdirs();
            }
            if (!SECRET7_flu_linear_fam_PATH.exists()) {
                SECRET7_flu_linear_fam_PATH.mkdirs();
            }
            if (!SECRET8_flu_linear_rox_PATH.exists()) {
                SECRET8_flu_linear_rox_PATH.mkdirs();
            }
            if (!SECRET7_flu_linear_rox_PATH.exists()) {
                SECRET7_flu_linear_rox_PATH.mkdirs();
            }
            if (!SECRET8_flu_linear_hex_PATH.exists()) {
                SECRET8_flu_linear_hex_PATH.mkdirs();
            }
            if (!SECRET7_flu_linear_hex_PATH.exists()) {
                SECRET7_flu_linear_hex_PATH.mkdirs();
            }
            if (!SECRET8_flu_linear_cy5_PATH.exists()) {
                SECRET8_flu_linear_cy5_PATH.mkdirs();
            }
            if (!SECRET7_flu_linear_cy5_PATH.exists()) {
                SECRET7_flu_linear_cy5_PATH.mkdirs();
            }
            if (!SECRET5_extraction_consistence_high_1_PATH.exists()) {
                SECRET5_extraction_consistence_high_1_PATH.mkdirs();
            }
            if (!SECRET5_extraction_consistence_high_2_PATH.exists()) {
                SECRET5_extraction_consistence_high_2_PATH.mkdirs();
            }
            if (!SECRET5_extraction_consistence_median_1_PATH.exists()) {
                SECRET5_extraction_consistence_median_1_PATH.mkdirs();
            }
            if (!SECRET5_extraction_consistence_median_2_PATH.exists()) {
                SECRET5_extraction_consistence_median_2_PATH.mkdirs();
            }
            if (!SECRET5_extraction_consistence_low_1_PATH.exists()) {
                SECRET5_extraction_consistence_low_1_PATH.mkdirs();
            }
            if (!SECRET5_extraction_consistence_low_2_PATH.exists()) {
                SECRET5_extraction_consistence_low_2_PATH.mkdirs();
            }


            File Template3_flu_repeat_fam_PATH = new File(FileUtils.SECRET1_3_flu_repeat_fam_PATH.replace("temp/result/history","template"));
            File Template3_flu_repeat_rox_PATH = new File(FileUtils.SECRET2_3_flu_repeat_rox_PATH.replace("temp/result/history","template"));
            File Template3_flu_repeat_hex_PATH = new File(FileUtils.SECRET3_3_flu_repeat_hex_PATH.replace("temp/result/history","template"));
            File Template3_flu_repeat_cy5_PATH = new File(FileUtils.SECRET4_3_flu_repeat_cy5_PATH.replace("temp/result/history","template"));
            File Template5_sample_repeat_high_1_PATH = new File(FileUtils.SECRET5_5_sample_repeat_high_1_PATH.replace("temp/result/history","template"));
            File Template5_sample_repeat_high_2_PATH = new File(FileUtils.SECRET6_5_sample_repeat_high_2_PATH.replace("temp/result/history","template"));
            File Template5_sample_repeat_median_1_PATH = new File(FileUtils.SECRET7_5_sample_repeat_median_1_PATH.replace("temp/result/history","template"));
            File Template5_sample_repeat_median_2_PATH = new File(FileUtils.SECRET8_5_sample_repeat_median_2_PATH.replace("temp/result/history","template"));
            File Template5_sample_repeat_low_1_PATH = new File(FileUtils.SECRET9_5_sample_repeat_low_1_PATH.replace("temp/result/history","template"));
            File Template5_sample_repeat_low_2_PATH = new File(FileUtils.SECRET10_5_sample_repeat_low_2_PATH.replace("temp/result/history","template"));
            File Template6_sample_linear_AMP_1_PATH = new File(FileUtils.SECRET11_6_sample_linear_AMP_PATH.replace("temp/result/history","template"));
            File Template6_sample_linear_AMP_2_PATH = new File(FileUtils.SECRET12_6_sample_linear_AMP_PATH.replace("temp/result/history","template"));
            File Template3_sample_linear_AMP_3_PATH = new File(FileUtils.SECRET13_3_sample_linear_AMP_PATH.replace("temp/result/history","template"));
            File Template8_flu_linear_fam_PATH = new File(FileUtils.SECRET14_8_flu_linear_fam_PATH.replace("temp/result/history","template"));
            File Template7_flu_linear_fam_PATH = new File(FileUtils.SECRET15_7_flu_linear_fam_PATH.replace("temp/result/history","template"));
            File Template8_flu_linear_rox_PATH = new File(FileUtils.SECRET16_8_flu_linear_rox_PATH.replace("temp/result/history","template"));
            File Template7_flu_linear_rox_PATH = new File(FileUtils.SECRET17_7_flu_linear_rox_PATH.replace("temp/result/history","template"));
            File Template8_flu_linear_hex_PATH = new File(FileUtils.SECRET18_8_flu_linear_hex_PATH.replace("temp/result/history","template"));
            File Template7_flu_linear_hex_PATH = new File(FileUtils.SECRET19_7_flu_linear_hex_PATH.replace("temp/result/history","template"));
            File Template8_flu_linear_cy5_PATH = new File(FileUtils.SECRET20_8_flu_linear_cy5_PATH.replace("temp/result/history","template"));
            File Template7_flu_linear_cy5_PATH = new File(FileUtils.SECRET21_7_flu_linear_cy5_PATH.replace("temp/result/history","template"));
            File Template5_extraction_consistence_high_1_PATH = new File(FileUtils.SECRET22_5_extraction_consistence_high_1_PATH.replace("temp/result/history","template"));
            File Template5_extraction_consistence_high_2_PATH = new File(FileUtils.SECRET23_5_extraction_consistence_high_2_PATH.replace("temp/result/history","template"));
            File Template5_extraction_consistence_median_1_PATH = new File(FileUtils.SECRET24_5_extraction_consistence_median_1_PATH.replace("temp/result/history","template"));
            File Template5_extraction_consistence_median_2_PATH = new File(FileUtils.SECRET25_5_extraction_consistence_median_2_PATH.replace("temp/result/history","template"));
            File Template5_extraction_consistence_low_1_PATH = new File(FileUtils.SECRET26_5_extraction_consistence_low_1_PATH.replace("temp/result/history","template"));
            File Template5_extraction_consistence_low_2_PATH = new File(FileUtils.SECRET27_5_extraction_consistence_low_2_PATH.replace("temp/result/history","template"));

            if (!Template3_flu_repeat_fam_PATH.exists()) {
                Template3_flu_repeat_fam_PATH.mkdirs();
            }
            if (!Template3_flu_repeat_rox_PATH.exists()) {
                Template3_flu_repeat_rox_PATH.mkdirs();
            }
            if (!Template3_flu_repeat_hex_PATH.exists()) {
                Template3_flu_repeat_hex_PATH.mkdirs();
            }
            if (!Template3_flu_repeat_cy5_PATH.exists()) {
                Template3_flu_repeat_cy5_PATH.mkdirs();
            }
            if (!Template5_sample_repeat_high_1_PATH.exists()) {
                Template5_sample_repeat_high_1_PATH.mkdirs();
            }
            if (!Template5_sample_repeat_high_2_PATH.exists()) {
                Template5_sample_repeat_high_2_PATH.mkdirs();
            }
            if (!Template5_sample_repeat_median_1_PATH.exists()) {
                Template5_sample_repeat_median_1_PATH.mkdirs();
            }
            if (!Template5_sample_repeat_median_2_PATH.exists()) {
                Template5_sample_repeat_median_2_PATH.mkdirs();
            }
            if (!Template5_sample_repeat_low_1_PATH.exists()) {
                Template5_sample_repeat_low_1_PATH.mkdirs();
            }
            if (!Template5_sample_repeat_low_2_PATH.exists()) {
                Template5_sample_repeat_low_2_PATH.mkdirs();
            }
            if (!Template6_sample_linear_AMP_1_PATH.exists()) {
                Template6_sample_linear_AMP_1_PATH.mkdirs();
            }
            if (!Template6_sample_linear_AMP_2_PATH.exists()) {
                Template6_sample_linear_AMP_2_PATH.mkdirs();
            }
            if (!Template3_sample_linear_AMP_3_PATH.exists()) {
                Template3_sample_linear_AMP_3_PATH.mkdirs();
            }
            if (!Template8_flu_linear_fam_PATH.exists()) {
                Template8_flu_linear_fam_PATH.mkdirs();
            }
            if (!Template7_flu_linear_fam_PATH.exists()) {
                Template7_flu_linear_fam_PATH.mkdirs();
            }
            if (!Template8_flu_linear_rox_PATH.exists()) {
                Template8_flu_linear_rox_PATH.mkdirs();
            }
            if (!Template7_flu_linear_rox_PATH.exists()) {
                Template7_flu_linear_rox_PATH.mkdirs();
            }
            if (!Template8_flu_linear_hex_PATH.exists()) {
                Template8_flu_linear_hex_PATH.mkdirs();
            }
            if (!Template7_flu_linear_hex_PATH.exists()) {
                Template7_flu_linear_hex_PATH.mkdirs();
            }
            if (!Template8_flu_linear_cy5_PATH.exists()) {
                Template8_flu_linear_cy5_PATH.mkdirs();
            }
            if (!Template7_flu_linear_cy5_PATH.exists()) {
                Template7_flu_linear_cy5_PATH.mkdirs();
            }
            if (!Template5_extraction_consistence_high_1_PATH.exists()) {
                Template5_extraction_consistence_high_1_PATH.mkdirs();
            }
            if (!Template5_extraction_consistence_high_2_PATH.exists()) {
                Template5_extraction_consistence_high_2_PATH.mkdirs();
            }
            if (!Template5_extraction_consistence_median_1_PATH.exists()) {
                Template5_extraction_consistence_median_1_PATH.mkdirs();
            }
            if (!Template5_extraction_consistence_median_2_PATH.exists()) {
                Template5_extraction_consistence_median_2_PATH.mkdirs();
            }
            if (!Template5_extraction_consistence_low_1_PATH.exists()) {
                Template5_extraction_consistence_low_1_PATH.mkdirs();
            }
            if (!Template5_extraction_consistence_low_2_PATH.exists()) {
                Template5_extraction_consistence_low_2_PATH.mkdirs();
            }

        }

/*        //内存泄漏监听
        LeakCanary.Config config = LeakCanary.getConfig().newBuilder()
                .retainedVisibleThreshold(3)
                .computeRetainedHeapSize(false)
                .build();
        LeakCanary.setConfig(config);*/


        /// /初始化串口扫码设备（8联排二维码）
        /// 初始化机械臂
        SerialPortManager_Robot.getInstance().init(this);
//        SerialPortManager_QRCode.getInstance().openSerialPortManager_QRCode(this);

//        RobotWorkManager.context = this;
//
//        WorkManager.getInstance(this);

    }
    public static Context getAppContext() {
        return mAppContext;
    }

    public static PcrApplication getInstance() {
        return mApplication;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

      //  APLManager.getInstance().stopWSSserver();

    }


//    ///////识别CH340芯片的USB驱动
//    public void GetTtyUsbPath() {
//
//        synchronized (PcrApplication.this) {
//            String devices = "/sys/bus/usb-serial/devices/";
//            File file = new File(devices);
//            File[] ttyUSBList = file.listFiles();
//            uartname = "ttyUSB";
//
//            if (ttyUSBList != null && ttyUSBList.length > 0) {
//                Log.i(TAG, "ttyUSBList.length--" + ttyUSBList.length);
//                for (int i = 0; i < ttyUSBList.length; i++) {
//                    File temp = ttyUSBList[i];
//                    if (temp.isDirectory() && temp.getName().contains("ttyUSB")) {
//                        File usbserial = new File(temp.getPath() + "/uevent");
//                        LineNumberReader readerProduct = null;
//                        try {
//                            readerProduct = new LineNumberReader(new FileReader(usbserial));
//                            String info = readerProduct.readLine();
//                            Log.i(TAG, "info-111--" + info);
//                            if (info.contains("cp210x") || info.contains("pl2303")) {
//                                uartname = temp.getName();
//                                Log.i(TAG, "uartname" + uartname);
//                            } else if (info.contains("ch34")) {
//                                uartname = temp.getName();
//                                Log.i(TAG, "uartnameRNA--" + uartname);
//                            } else if (info.contains("ftdi")) {
//                                uartnameBoWei = temp.getName();
//                                Log.i(TAG, "uartnameBowei---" + uartnameBoWei);
//                            }
//
//                        } catch (Exception e) {
//                            Log.e(TAG, "usbserial : " + e.getMessage());
//                        }
//
//                    }
//
//                }
//            }
//        }
//    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Locale _UserLocale = LocaleUtils.getUserLocale(this);
        //系统语言改变了应用保持之前设置的语言
        if (_UserLocale != null) {
            Locale.setDefault(_UserLocale);
            Configuration _Configuration = new Configuration(newConfig);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                _Configuration.setLocale(_UserLocale);
            } else {
                _Configuration.locale = _UserLocale;
            }
            getResources().updateConfiguration(_Configuration, getResources().getDisplayMetrics());
        }
    }

    private void initSecure() {
        try {
            EnjoySDKSupport EnjoySDKSupport = new EnjoySDKSupport(this);
            Log.i("initSecure", "EnjoySDKSupport getSystemVersion == " + EnjoySDKSupport.getSystemVersion());
            EnjoySDK enjoySDK = new EnjoySDK(this);
            enjoySDK.setSecurePasswd("Abc12345", "Abc12345");
            enjoySDK.registSafeProgram("Abc12345");
            enjoySDK.setNavigationBarShowStatus(McStateCode.UI_SHOWING);
//            enjoySDK.setNavigationBarShowStatus(McStateCode.UI_HIDEING);
            enjoySDK.setBootanimation(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/file/bootanimation.zip");
            enjoySDK.setHomePackage("com.gidi.bio_pcr.pipeline");

//            int ret = enjoySDK.getSecurePasswdStatus();
//            if(McPlatform.PLATFORM.contains("R2120")){
//                enjoySDK.setSystemRotation(90);
//            }else if(McPlatform.PLATFORM.contains("A3100")){
//                enjoySDK.setSystemRotation(0);
//            }

            enjoySDK.switchEthernet(true,"eth0");
            boolean ret = enjoySDK.isEthernetEnable("eth0");
            Log.i("initSecure", "isEthernetEnable == " + ret);
            // V2设备：
            McEthernetConfig config = enjoySDK.getMcEthernetConfig("eth0");
            int mode = 2; // mode为1表示DHCP模式，为2表示静态IP模式
            config.setMode(mode);//在McEthernetConfig类中可查各参数意义
            config.setIpV4Address("192.168.1.100");
            config.setGateway("192.168.1.1");
            config.setDns("114.114.114.114");
            config.setBackupDns("8.8.8.8");
            config.setSubnetMask("255.255.255.0");
            int retconfig = enjoySDK.setEthernetConfig(config,"eth0");
            Log.i("initSecure", "setEthernetConfig == " + retconfig);

        }catch (Exception e){
            e.printStackTrace();
            Log.e("initSecure", "EnjoySDKV2 == " + e.getMessage());
        }
    }
    Runnable logrunnable = new Runnable() {
        @Override
        public void run() {
            mUIHandler.sendEmptyMessage(MSG_START_LOG);
        }
    };
    static final int MSG_START_LOG = 1;
    private class UIHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_START_LOG:
                    LogcatHelper.getInstance(getApplicationContext()).start();
                    break;
            }
        }
    }

}
