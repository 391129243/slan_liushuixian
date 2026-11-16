package com.gzlab.autoassemblyline_client;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.gzlab.autoassemblyline_client.autopipeline.mgr.SLANModuleManager;
import com.gzlab.autoassemblyline_client.autopipeline.mgr.SLANServiceManager;
import com.gzlab.autoassemblyline_client.autopipeline.mgr.WSSModuleManager;
import com.gzlab.autoassemblyline_client.connect.ConnectManager;
import com.gzlab.autoassemblyline_client.constant.SharedConstant;
import com.gzlab.autoassemblyline_client.util.CrashHandler;
import com.gzlab.autoassemblyline_client.util.FileUtils;
//import com.gidi.bio_pcr.util.JavaFsFileSystemCreator;
import com.gzlab.autoassemblyline_client.util.LocaleUtils;
import com.gzlab.autoassemblyline_client.util.LogcatHelper;
import com.gzlab.autoassemblyline_client.util.MemoryMonitor;
import com.gzlab.autoassemblyline_client.util.PermissionHelper;
import com.gzlab.autoassemblyline_client.util.PreferenceUtil;
import com.hongshi.slansdk.model.device.Device;
import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.DeviceType;
import com.hzmct.enjoysdk.api.EnjoySDK;
import com.hzmct.enjoysdk.support.EnjoySDKSupport;
import com.hzmct.enjoysdk.transform.McStateCode;
import com.hzmct.enjoysdk.util.McPlatform;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;



public class PcrApplication extends Application {
    private static final String TAG = "PcrApplication";
    public static Context mAppContext = null;
    public static String deviceId = "SLAN_PCR1111";
    public static int devicePos = 1;
    public static SLANService slanService = null;

    public static String wssServerIP = "192.168.1.100";
    public static String wssServerPort = "8080";

    public static String rnaVersion = "V5";

    public static int deviceTypeSetting = 0;
    public static boolean is_SLAN_PCR_connect = false;
    public static ArrayList<Device> devices;
    public static Device currentDevices;


    private EnjoySDK enjoySDK;


    private static PcrApplication instance;
    public static PcrApplication getInstance() {
        if (instance == null) {
            instance = new PcrApplication();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("WSSClient", "onCreate： ");
        mAppContext = this;
        instance = this;

        deviceTypeSetting = PreferenceUtil.getInstance(getApplicationContext())
                .getIntValue(SharedConstant.DEVICE_TYPE_SETTING,0);


       EnjoySDKSupport EnjoySDKSupport = new EnjoySDKSupport(this);
       initSecure();
        //启动日志收集功能
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                LogcatHelper.getInstance(getAppContext()).start();
            }
        }).start();
        //启动内存监控：每60秒打印一次各类内存占用及增量，便于定位持续上涨的内存
        MemoryMonitor.getInstance().start();
       // LiteORMCtrlMgr.getInstance();
        CrashHandler crashHandler = CrashHandler.getInstance(this);
        crashHandler.init();
        //语言设置

        // 启动诊断：打印 MANAGE_USB 是否已授予（区分“时机问题”还是“签名/权限问题”）
        PermissionHelper.logUsbPermissionStatus(getApplicationContext());

        // 在 SLAN SDK 打开设备前，等待 USB 串口设备(FT232 等)枚举完成并静默授权。
        // 开机时 kiosk 启动很早，USB 往往还没枚举完；这里有限等待(最长 4s)，
        // 一旦设备出现立即静默授权，保证 SDK 的 100ms 重连定时器在
        // hasPermission()==true 之后才打开设备，从而重启后不再弹授权框。
        PermissionHelper.waitAndGrantUsbSilently(getApplicationContext(), 4000);
        deviceId = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.DEVICE_ID,"SLANPCR0000");
        devicePos = PreferenceUtil.getInstance(getApplicationContext())
                .getIntValue(SharedConstant.DEVICE_POS,0);
        //宏石SDK初始化
        initSLANPcrDevice();

        Log.e(TAG, "宏石设备==" + devices);

        WSSModuleManager wssModuleManager = WSSModuleManager.getInstance(getApplicationContext());
        wssModuleManager.initConnection();


        SLANModuleManager.getInstance().init(getApplicationContext());
        Log.i("PCR", "PcrApplication.hongshi" + PcrApplication.is_SLAN_PCR_connect);



/*        //内存泄漏监听
        LeakCanary.Config config = LeakCanary.getConfig().newBuilder()
                .retainedVisibleThreshold(3)
                .computeRetainedHeapSize(false)
                .build();
        LeakCanary.setConfig(config);*/



    }

    public static Context getAppContext() {
        return mAppContext;
    }


    /**
     * 初始化宏石 SLAN PCR 设备:读取设备类型偏好 → 初始化 SDK → 获取 SLANService → 连接 PCR。
     */
    private void initSLANPcrDevice() {
        String deviceTypeStr = PreferenceUtil.getInstance(getApplicationContext())
                .getStringValue(SharedConstant.SLAN_DEVICE_TYPE, "SLAN_16H");
        DeviceType deviceType = SLANServiceManager.fromString(deviceTypeStr, DeviceType.SLAN_16H);
        try {
            SLANServiceManager.getInstance(getApplicationContext()).initSLANService(deviceType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        slanService = SLANServiceManager.getInstance(getApplicationContext()).getSlanService();
        ConnectManager.getInstance().initSLANPcrConnection();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }




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
            enjoySDK = new EnjoySDK(this);
            enjoySDK.setSecurePasswd("Abc12345", "Abc12345");
            enjoySDK.registSafeProgram("Abc12345");
            enjoySDK.setNavigationBarShowStatus(McStateCode.UI_HIDEING);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int result = 0;
                        Thread.sleep(5000);
                        if(PcrApplication.deviceTypeSetting == 0) {
                            result = enjoySDK.setBootanimation(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/file/bootanimation.zip");
                        }else if(PcrApplication.deviceTypeSetting == 1) {
                            result = enjoySDK.setBootanimation(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/file/bootanimation_rotate.zip");
                        }
                        Log.i("McPlatform.PLATFORM:::", result + "");
                    } catch (Exception e) {
                        Log.e("PcrApplication", e.getMessage());
                    }
                }
            }).start();


            int ret = enjoySDK.getSecurePasswdStatus();
            Log.i("McPlatform.PLATFORM:::", McPlatform.PLATFORM);

            if(enjoySDK.getHomePackage().equals("com.gzlab.autoassemblyline_client")){

            }else{
                enjoySDK.setHomePackage("com.gzlab.autoassemblyline_client");
            }



//            if(McPlatform.PLATFORM.contains("R2120")){
//                enjoySDK.setSystemRotation(90);
//            }else if(McPlatform.PLATFORM.contains("A3100")){
//                enjoySDK.setSystemRotation(0);
//            }


//            ret = enjoySDK.whiteRulesSwitch(true);
//            AppRule appRule = new AppRule("com.gidi.bio_pcr",true);
//            ArrayList<AppRule> appRules = enjoySDK.addWhiteRules(appRule);
        }catch (Exception E){
            Log.i("LoginActivity", "EnjoySDKV2 == " + E.getMessage());
        }
    }

}
