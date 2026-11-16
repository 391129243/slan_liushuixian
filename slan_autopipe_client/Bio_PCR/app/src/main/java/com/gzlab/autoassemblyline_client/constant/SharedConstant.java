package com.gzlab.autoassemblyline_client.constant;

public class SharedConstant {
    public static final String LOCALE_KEY = "LOCALE_KEY";
    public  static final String SERIAL_PORT = "serial_port";

    public static final String SETTING_PASSWORD = "setting_pwd";
    public  static final String LOGIN_ACCOUNT = "login_account";
    public static  final String LOGIN_PASSWORD = "login_pwd";

    public static final String IS_BOOT_UP_NEED_RESET = "IS_BOOT_UP_NEED_RESET";

    public static final String RUNNING_TEMPLATE = "Running_Template";//运行模板

    public static final String PCR_RUNNING_TEMPLATE = "PCR_RUNNING_TEMPLATE";

    public static final String PCR_RUNNING_TEMPLATE_PHASE = "PCR_RUNNING_TEMPLATE_PHASE";

    public static final String PCR_RUNNING_TEMPLATE_PHASE_STEP = "PCR_RUNNING_TEMPLATE_PHASE_STEP";

    public static final String PCR_RUNNING_TEMPLATE_STARTTIME = "PCR_RUNNING_TEMPLATE_STARTTIME";

    public static final String RUNNING_TEMPLATE_ACTIVITY = "RUNNING_TEMPLATE_ACTIVITY";//

    public static final String START_TIME = "start_time";


    public static final String PROCEDURE_RUNNING_TIME = "PROCEDURE_RUNNING_TIME";
    public static final String EX_TOTAL_TIME ="experiment_total_time";

//    public static final String RNA_SYSTEM = "rna_system";
    //运行的实验模式
    public static final String EXPERIMENT_TYPE = "experiment_Type";////0：全流程,1,singleRNA,2:singlePCR

    public static final String SYSTEM_INIT = "SYSTEM_INIT";

    public static final String UPLOAD_URL = "UPLOAD_URL";
    public static final String PORT= "port";
    public static final String HL7_IP = "hl7_ip";
    public static final String HL7_PORT = "hl7_port";
    public static final String HEX_FIRST_JSON = "HEX_FIRST_JSON";

    public static final String REAGENT_CODE_USER= "reagentCode_user";

    public static final String CARD_CODE_ONE = "IdCard_one";
    public static final String CARD_CODE_TWO = "IdCard_two";
    public static final String CARD_CODE_THREE = "IdCard_three";
    public static final String CARD_CODE_FOUR= "IdCard_four";
    public static final String CARD_CODE_FIVE = "IdCard_five";
    public static final String CARD_CODE_SIX = "IdCard_six";
    public static final String CARD_CODE_SEVEN = "IdCard_seven";
    public static final String CARD_CODE_EIGHT= "IdCard_eight";
    public static final String SECRET_SETTING ="secret_setting";

    public static final String DEVICE_TYPE_SETTING ="DEVICE_TYPE_SETTING";

    public static final String REAGENT_CODE = "regent_code";

    public static final String RNA_EXTRACTION_DURATION = "rna_extraction_duration";//核提总时长
    //上传模式选择
    public static final String UPLOAD_MODE = "upload_mode";//0:http网口上传，HL7以太网，HL7串口

    ///////////////////////////////////////////////////
    public static final String DEVICE_ID = "DeviceId";
    public static final String DEVICE_POS = "DevicePos";

    public static final String SLAN_DEVICE_TYPE = "SLAN_DeviceType";//宏石硬件是多少孔

    //崩溃状态：用于标记本次启动是否为崩溃后重启，便于其他模块补传崩溃日志
    public static final String IS_CRASH = "is_crash";
    //上一次崩溃时间（ms），用于判断是否进入"短时间连续崩溃"窗口
    public static final String LAST_CRASH_TIME = "last_crash_time";
    //窗口内的连续崩溃次数
    public static final String CRASH_COUNT = "crash_count";



}
