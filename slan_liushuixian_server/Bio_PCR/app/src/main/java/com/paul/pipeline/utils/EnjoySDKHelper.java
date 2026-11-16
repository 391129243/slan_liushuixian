package com.paul.pipeline.utils;


public class EnjoySDKHelper {
//    public static void initSecure(Context context) {
//        try {
//            Log.i("initSecure", "is EnjoySDK support ? == " + EnjoyUtil.isEnjoySupport());
//            Log.i("initSecure", "enjoy_sdk_version == " +  McEnjoySdkInfo.SDK_VERSION);
//            Log.i("initSecure", "enjoy_version == " +  McEnjoySdkInfo.VERSION);
//            Log.i("initSecure", "enjoy_android_sdk == " +  McEnjoySdkInfo.COMPATIBLE_ANDROID_SDK);
//            McSecure mcSecure = McSecure.getInstance(context);
//            int ret = mcSecure.getSecurePasswdStatus();
//            if (ret == McSecurePasswordState.MC_SECURE_PASSWD_EMPTY){
//                ret = mcSecure.setSecurePasswd(null,"Abc12345");
//                Log.i("initSecure", "setSecurePasswd == " + ret);
//            }
//            mcSecure.registSafeProgram("Abc12345");
//
//            McSystemUi mcSystemUi = McSystemUi.getInstance(context);
//            Log.i("initSecure", "bar status == " + mcSystemUi.getStatusBarShowStatus() + ", navigation status == " + mcSystemUi.getNavigationShowStatus());
//            parseError(mcSystemUi.temporarilySwitchStatusBar(false));
//            parseError(mcSystemUi.temporarilySwitchNavigation(false));
////            parseError(mcSystemUi.disableStatusBarItem(McSystemUiFlag.DISABLE_HOME));
//
//            McBootAnimation mcBootAnimation = McBootAnimation.getInstance(context);
//            mcBootAnimation.setBootanimation(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PCR/file/bootanimation.zip");
//
//            McHome mcHome = McHome.getInstance(context);
//            String packageinfo = AppUtils.getAppPackageName(context);
//            parseError(mcHome.setHomePackage(packageinfo));
//
//            McEthernet mcEthernet = McEthernet.getInstance(context);
//            McEthernetConfig mcEthernetConfig = mcEthernet.getMcEthernetConfig();
//            if (mcEthernetConfig != null) {
//                Log.i("initSecure", "EthMode == " + mcEthernetConfig.getMode());
//                Log.i("initSecure", "EthIpV4 == " + mcEthernetConfig.getIpV4Address());
//                Log.i("initSecure", "EthMask == " + mcEthernetConfig.getSubnetMask());
//                Log.i("initSecure", "EthGateway == " + mcEthernetConfig.getGateway());
//                Log.i("initSecure", "EthDns1 == " + mcEthernetConfig.getDns());
//                Log.i("initSecure", "EthDns2 == " + mcEthernetConfig.getBackupDns());
//            }else{
//                mcEthernetConfig = new McEthernetConfig();
//            }
//            mcEthernetConfig.setMode(McEthernetConfig.STATIC_MODE);
//            mcEthernetConfig.setIpV4Address("192.168.1.100");
//            mcEthernetConfig.setSubnetMask("255.255.255.0");
//            mcEthernetConfig.setGateway("192.168.1.1");
//            mcEthernetConfig.setDns("8.8.8.8");
//            mcEthernetConfig.setBackupDns("8.8.8.8");
//            parseError(mcEthernet.setEthernetConfig(mcEthernetConfig));
//
//        }catch (Exception e){
//            e.printStackTrace();
//            Log.e("initSecure", "EnjoySDK exception: " + e.getMessage());
//        }
//    }
    static final String TAG = "initSecure";
//    private static void parseError(int errorCode) {
//        switch (errorCode) {
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_PASSWD_ALREADY_EMPYT:
//                Log.e(TAG, "errorDump: 密码已经为空");
//                break;
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_PASSWD_CHECK_FAILED:
//                Log.e(TAG, "errorDump: 密码检查不通过");
//                break;
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_PASSWD_FORMAT_ERROR:
//                Log.e(TAG, "errorDump: 密码格式错误");
//                break;
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_PASSWD_SET_FAILED:
//                Log.e(TAG, "errorDump: 密码设置失败");
//                break;
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_PROGRAM_ALREADY_IN_SAFE_PROGRAM_LIST:
//                Log.e(TAG, "errorDump: 已经在安全应用列表内");
//                break;
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_PROGRAM_NOT_IN_SAFE_PROGRAM_LIST:
//                Log.e(TAG, "errorDump: 不在安全应用列表内");
//                break;
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_REGISTER_SAFE_PROGRAM_FAILED:
//                Log.e(TAG, "errorDump: 注册安全应用失败");
//                break;
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_UNREGISTER_SAFE_PROGRAM_FAILED:
//                Log.e(TAG, "errorDump: 注销安全应用失败");
//                break;
//            case McErrorCode.ENJOY_SECURE_MANAGER_ERROR_PASSWD_NOT_INIT:
//                Log.e(TAG, "errorDump: 鉴权密码未初始化");
//                break;
//            case McErrorCode.ENJOY_ROTATION_MANAGER_ERROR_DISPLAY_ROTATION:
//                Log.i("initSecure", "主屏幕旋转错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_ROTATION_MANAGER_ERROR_SYSTEM_ROTATION:
//                Log.i("initSecure", "临时转屏错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_ROTATION_MANAGER_ERROR_VICE_ROTATION:
//                Log.i("initSecure", "副屏旋转错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_ROTATION_MANAGER_ERROR_VICE_FULL:
//                Log.i("initSecure", "副屏幕全屏错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_ROTATION_MANAGER_ERROR_ROTATION_FORMAT:
//                Log.i("initSecure", "旋转角度参数格式错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_ETHERNET_MANAGER_ERROR_CONFIG_INVALID:
//                Log.i("initSecure", "以太网配置错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_COMMON_ERROR_SERVICE_NOT_START:
//                Log.i("initSecure", "以太网配置服务错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_ETHERNET_MANAGER_ERROR_ETH_TETHER_IN_USE:
//                Log.i("initSecure", "以太网共享使用中：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_COMMON_SUCCESSFUL:
//                Log.i("initSecure", "成功");
//                break;
//            case McErrorCode.ENJOY_COMMON_ERROR_SDK_NOT_SUPPORT:
//                Log.e("initSecure", "服务错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_COMMON_ERROR_WRITE_SETTINGS_ERROR:
//                Log.e("initSecure", "设置错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            case McErrorCode.ENJOY_COMMON_ERROR_DEVICE_NOT_SUPPORT:
//                Log.e("initSecure", "设备不支持：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//            default:
//                Log.e("initSecure", "未知错误：" + McErrorCode.errorCode2Str(errorCode));
//                break;
//        }
//    }
}
