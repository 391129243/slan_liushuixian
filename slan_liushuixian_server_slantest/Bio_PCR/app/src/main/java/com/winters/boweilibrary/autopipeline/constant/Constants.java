package com.winters.boweilibrary.autopipeline.constant;

import com.gidi.bio_pcr.util.FileUtils;

public class Constants {

    /// //SerialPortManager_Robot  常量
    ///

    public static final String ROBOT_LED_FILE_PATH = FileUtils.COMMON_HEX_PATH + "79-robot_set_led.hex";

    public static final String ROBOT_ARMCONFIG_FILE_PATH = FileUtils.COMMON_HEX_PATH + "80-robot_arm_config.hex";

    public static final String ROBOT_GET_FILE_PATH = FileUtils.COMMON_HEX_PATH + "81-robot_get.hex";

    public static final String ROBOT_GET_RESET_FILE_PATH = FileUtils.COMMON_HEX_PATH + "81-robot_get_reset.hex";

//    public static final String ROBOT_GET_PCR_FILE_PATH = FileUtils.COMMON_HEX_PATH + "82-robot_pcr_get.hex";

    public static final String ROBOT_GET_RNA_FILE_PATH = FileUtils.COMMON_HEX_PATH + "82-robot_RNA_get.hex";

    public static final String ROBOT_PUT_FILE_PATH = FileUtils.COMMON_HEX_PATH + "83-robot_put.hex";

//    public static final String ROBOT_PUT_PCR_FILE_PATH = FileUtils.COMMON_HEX_PATH + "84-robot_pcr_put.hex";

    public static final String ROBOT_PUT_RNA_FILE_PATH = FileUtils.COMMON_HEX_PATH + "84-robot_RNA_put.hex";

    public static final String ROBOT_RESET_FILE_PATH = FileUtils.COMMON_HEX_PATH + "85-robot_arm_reset_XYZ.hex";

    public static final String ROBOT_GET_PCR_COVER_FILE_PATH = FileUtils.COMMON_HEX_PATH + "86-robot_pcr_cover_get.hex";

    public static final String ROBOT_PUT_PCR_COVER_FILE_PATH = FileUtils.COMMON_HEX_PATH + "87-robot_pcr_cover_put.hex";

    public static final String ROBOT_SET_COOLER_FILE_PATH = FileUtils.COMMON_HEX_PATH + "88-robot_set_cooler.hex";

    public static final String ROBOT_OPERATE_DOOR_FILE_PATH = FileUtils.COMMON_HEX_PATH + "89-robot_operate_door.hex";

    public static final String ROBOT_FINISH_FILE_PATH = FileUtils.COMMON_HEX_PATH + "100-finishcmd.hex";


    public static final String CONFIG_GET_XY = "get_xy";
    public static final String CONFIG_GET_X = "get_x";
    public static final String CONFIG_GET_Y = "get_y";
    public static final String CONFIG_GET_Z = "get_z";

    ///RobotWorkManager 常量
    public static final String ACTION = "ACTION";
    public static final String KEY_INPUT_DATA = "INPUT";
    public static final String KEY_PANNUM_DATA = "PANNUM";
    public static final String KEY_RESULT_DATA = "RESULT";
    public static final String KEY_OUTPUT_DATA = "OUTPUT";

}
