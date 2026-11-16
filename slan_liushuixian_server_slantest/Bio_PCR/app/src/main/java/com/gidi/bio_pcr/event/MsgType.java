package com.gidi.bio_pcr.event;


public class MsgType {
    public static final int MSG_FAIL =1;
    public static final int MSG_ENTRY_INFO_DONE = 2;//完成信息录入

    public static final int MSG_OPEN_DOOR = 3;

    public static final int MSG_START_FIRST_PCR = 4;

    public static final int MSG_START_RNA_RUNNING = 8;//第一次RNA开始运行
    public static final int MSG_DONE_RNA_RUNNING = 9;
    public static final int MSG_RNA_FILE_NULL= 10;//中断

    public static final int MSG_START_SECOND_RNA_RUNNING = 11;
    public static final int MSG_DONE_SECOND_RNA_RUNNING = 12;
    public static final int MSG_SECOND_RNA_FILE_NULL = 13;//中断



    public static final int MSG_SHOW_RESULT= 20;//中断

    public static final int MSG_SHOW_FAM_CHANNEL_CURVE = 22;
    public static final int MSG_SHOW_HEX_CHANNEL_CURVE = 23;
    public static final int MSG_SHOW_ROX_CHANNEL_CURVE = 24;
    public static final int MSG_SHOW_CY5_CHANNEL_CURVE = 25;

    public static final int MSG_SHOW_QUASAR_CHANNEL_CURVE = 26;
    public static final int MSG_SHOW_ATTO_CHANNEL_CURVE = 27;
    public static final int MSG_SHOW_ALL_CHANEL_RESULT = 28;


    public static final int MSG_PCR_CONNECT_STATE = 31;
    public static final int MSG_BoWei_CONNECT_STATE = 301;
    public static final int MSG_SINGLE_RNA_DONE = 45;
    public static final int MSG_TEMPLATE_CAPACITY_DONE = 32;

    public static final int MSG_WAIT_OPEN_DONE = 33;
    public static final int MSG_UPDATE_EXPERIMENT_EXCEL = 34;
    public static final int MSG_UPDATE_FRAGMENT_EXPERIMENT_EXCEL = 39;
    public static final int MSG_SHOW_SECOND_RESULT= 35;//中断
    public static final int MSG_SHOW_ALARM_MESSAGE = 36;

    public static final int MSG_PARSE_FINISH = 37;

    public static final int MSG_SHOW_EX_SECOND_RESULT= 40;//中断
    public static final int MSG_UPDATE_SYSTEM_TIME= 41;

    public static final int MSG_UPDATE_RUNNING_EX = 42;

    public static final int MSG_SHOW_EXCEL_RESULT = 43;
    public static final int MSG_UPDATE_EX_RESULT = 44;

    public static final int MSG_TEST_FINISH = 102;

    //更新宏石第一次PCR
    public static final int MSG_START_SLAN_FIRST_PCR = 46;

    public static final int MSG_START_SLAN_FIRST_PCR_SUCCESS = 47;
    public static final int MSG_START_SLAN_SECOND_PCR = 48;
    public static final int MSG_FIRST_SLAN_PCR_RUNNING = 49;
    public static final int MSG_FIRST_SLAN_PCR_ERROR = 50;
    public static final int MSG_FIRST_SLAN_PCR_DONE = 51;
    public static final int MSG_SECOND_SLAN_PCR_RUNNING = 52;
    public static final int MSG_SECOND_SLAN_PCR_ERROR = 53;
    public static final int MSG_FIRST_SLAN_PCR_INTERRUOT = 54;
    public static final int MSG_SECOND_SLAN_PCR_INTERRUOT = 55;
    public static final int MSG_SECOND_SLAN_PCR_DONE = 56;

    public static final int MSG_START_SLAN_SECOND_PCR_SUCCESS = 57;
    public static final int MSG_FINISH_SLAN_PARSE_PCR = 58;

    /*
    * 显示孔位详情
    * */
    public static final int MSG_RUN_CONFIG_SLAN_RUNNING = 59;
    public static final int MSG_UPDATE_CONFIG_RUNNING_EX = 60;

    public static final int MSG_UPDATE_CONFIG_SLAN_ERROR = 61;

    public static final int MSG_UPDATE_CONFIG_SLAN_DONE = 62;

    public static final int MSG_CREATE_SLAN = 63;
    public static final int MSG_CONFIG_SLAN_INFO = 64;
    public static final int MSG_CONFIG_SLAN_RUN= 65;
    //////模块断连
    public static final int MSG_RNA_CONNECT_STATE = 66;

    public static final int MSG_CONFIG_SLAN_EDIT= 67;
    public static final int MSG_BOOT_UP = 100;
    public static final int MSG_UDISK_STATE = 200;
    public static final int MSG_START_COPY_USB = 201;//开始复制本地文件
    public static final int MSG_END_COPY_USB = 202;//结束复制本地文件
    public static final int MSG_COPY_USB_SUCCESS = 203;
    public static final int MSG_COPY_USB_FAIL = 204;
    public static final int MSG_END_COPY_USB_FAIL = 212;

    public static final int MSG_COPY_LOCAL_SUCCESS = 205;//复制本地文件成功
    public static final int MSG_COPY_LOCAL_FAIL = 206;//复制本地文件失败
    public static final int MSG_START_COPY_LOCAL = 207;//开始复制本地文件
    public static final int MSG_END_COPY_LOCAL = 209;//结束复制本地文件
    public static final int MSG_END_COPY_LOCAL_FAIL = 210;//复制本地文件失败

    public static final int MSG_COPY_PROGRESS = 211;

    public static final int MSG_START_CLOSE_DOOR = 212;//singlePCR要关门
    public static final int MSG_END_CLOSE_DOOR = 213;//singlePCR要关门
    public static final int MSG_COUNTDOWN_END = 214;

    public static final int MSG_BUTTON_ENABLE = 215;

    public static final int MSG_START_COUNTDOWN = 216;
    public static final int MSG_END_COUNTDOWN = 217;

    public static final int MSG_SAVE_PDF = 218;
    public static final int MSG_PRINT_ALL_PDF = 219;

    //////机械臂模块断连
    public static final int MSG_ROBOT_CONNECT_STATE = 300;
}
