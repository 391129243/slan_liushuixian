package com.gidi.bio_pcr.constant;

import com.gidi.bio_pcr.util.FileUtils;

public class Constants {
    public static String APP_NAME = "PCR";
    public static String DEVICE_MODEL = "ZZK-08A";
    public static String CONTACT = "18711100056";
    public static String PRODUCTION_DATE = "2025-10-10";
    public static final int MSG_UPDATE_SYSTEM_TIME = 100;
    public static final int MSG_WIFI_CONNECT_STATE = 110;
    public static final int MSG_WIFI_TURN_OFF = 111;

    public static final int MSG_PARSE_RUNNING_EX= 112;

    //最初默认的上传url
    public static final String UPLOAD_URL = "";

    public static final int PROGRAM_TYPE_AMP = 0;//定性。定量

    public static final int PROGRAM_TYPE_MELT = 1;//标准熔解曲线

    public static final int MELT_SCANMODE_CONTINUE = 0;
    public static final int MELT_SCANMODE_STEP = 1;

//    /// //SerialPortManager_Robot  常量
//    public static final String ROBOT_ARMCONFIG_FILE_PATH = FileUtils.COMMON_HEX_PATH + "80-robot_arm_config.hex";
//
//    public static final String ROBOT_GET_FILE_PATH = FileUtils.COMMON_HEX_PATH + "81-robot_get.hex";
//
//    public static final String ROBOT_GET_PCR_FILE_PATH = FileUtils.COMMON_HEX_PATH + "82-robot_pcr_get.hex";
//
//    public static final String ROBOT_PUT_FILE_PATH = FileUtils.COMMON_HEX_PATH + "83-robot_put.hex";
//
//    public static final String ROBOT_PUT_PCR_FILE_PATH = FileUtils.COMMON_HEX_PATH + "84-robot_pcr_put.hex";
//
//    public static final String ROBOT_RESET_FILE_PATH = FileUtils.COMMON_HEX_PATH + "85-robot_reset.hex";
//
//    public static final String ROBOT_GET_PCR_COVER_FILE_PATH = FileUtils.COMMON_HEX_PATH + "86-robot_pcr_cover_get.hex";
//
//    public static final String ROBOT_PUT_PCR_COVER_FILE_PATH = FileUtils.COMMON_HEX_PATH + "87-robot_pcr_cover_put.hex";
//
//    public static final String ROBOT_FINISH_FILE_PATH = FileUtils.COMMON_HEX_PATH + "100-finishcmd.hex";
//
//    public static final String CONFIG_GET_XY = "get_xy";
//    public static final String CONFIG_GET_X = "get_x";
//    public static final String CONFIG_GET_Y = "get_y";
//    public static final String CONFIG_GET_Z = "get_z";
//
//    ///RobotWorkManager 常量
//    public static final String ACTION = "ACTION";
//    public static final String KEY_INPUT_DATA = "INPUT";
//    public static final String KEY_RESULT_DATA = "RESULT";
//    public static final String KEY_OUTPUT_DATA = "OUTPUT";
}
