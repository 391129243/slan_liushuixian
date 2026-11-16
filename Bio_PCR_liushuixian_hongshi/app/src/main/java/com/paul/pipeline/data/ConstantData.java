package com.paul.pipeline.data;

public class ConstantData {
    public final static long ARMTASK_PERIOD = 5000L;
    public final static long RNATASK_PERIOD = 5000L;
    public final static long PCRTASK_PERIOD = 5000L;

    public final static int LOG_RNA = 1;//核提
    public final static int LOG_PCR = 2;//PCR
    // LOG_ARM 已删除：ARM 日志改由 RobotWorkManager 直写 logcat，不再单独落盘（与参考工程对齐）

    public final static int NAV_SEIFCHECK = 0;//自检
    public final static int NAV_MAIN = 1;//9主界面
    public final static int NAV_LOGIN = 2;//登录
    public final static int NAV_OPENDOOR = 3;//开门
    public final static int NAV_TRAYDETAIL= 4;//托盘详情
    public final static int NAV_TRAYMANAGE= 5;//托盘管理
    public final static int NAV_DETAILRNA= 6;//rna管理
    public final static int NAV_DETAILPCR= 7;//pcr管理
    public final static int NAV_DETAILARM= 8;//arm管理
    public final static int NAV_RECORDS = 9;//历史记录
    public final static int NAV_SETTING = 10;//设置
    public final static int NAV_WELLDETAIL = 11;//pcr
    public final static int NAV_BiowayExDetail = 12;//pcr

    //1-8托盘
    public final static int TRAY_Edit = 0;//托盘打开项目
    public final static int TRAY_Add = 1;//托盘打开项目

    public final static int TYPE_FULL = 0;//全流程
    public final static int TYPE_RNA = 1;//核提
    public final static int TYPE_PCR = 2;//PCR

    public final static int TEMPLATE_FULL_SINGLE = 1;//全流程
    public final static int TEMPLATE_FULL_MULTI = 2;//全流程
    public final static int TEMPLATE_RNA = 3;//全流程
    public final static int TEMPLATE_PCR = 4;

    public final static int DEVICE_RNA = 1;//核提
    public final static int DEVICE_PCR = 2;//PCR
    public final static int DEVICE_TRAY = 3;//托盘
    public final static int DEVICE_ARM = 4;//机械臂
    public final static int DEVICE_QRSCANER = 5;//扫码枪
    public final static int DEVICE_DOOR = 6;//舱门
    public final static int DEVICE_PCRCOVER = 7;//盖子

    public final static int TASK_ARM_ERROR = 0;//机械臂error
    public final static int TASK_START_RNA = 1;//机械臂rna
    public final static int TASK_FINISH_RNA = 2;//机械臂rna back
    public final static int TASK_START_PCR = 3;//机械臂pcr
    public final static int TASK_FINISH_PCR = 4;//机械臂pcr back
    public final static int TASK_RUN_PCR = 5;//pcr模板运行
    public final static int TASK_RUN_RNA = 6;//rna模板运行
    public final static int TASK_TRAY_LED = 7;//托盘led
    public final static int TASK_TRAY_MOVE = 8;//托盘直接移动

    public final static int TemparatureCtrl_fast = 0;
    public final static int TemparatureCtrl_stardard = 1;

    public final static String str_FREE = "free";//空闲
    public final static String str_ACK = "acking";//等待 rna pcr
    public final static String str_RUN = "running";//运行
    public final static String str_FINISH = "finish";//完成 arm
    public final static String str_ERROR = "error";//错误tray

    public final static int STATUS_FREE = 0;//
    public final static int STATUS_RUN = 1;//
    public final static int STATUS_FINISH = 2;//
    public final static int STATUS_ERROR = -1;//

    public static int ConvertStatus(String value){
        if(value==null||value.isBlank()||value.isEmpty()){
            return STATUS_FREE;
        }
        switch(value){
            case str_FREE:
                return STATUS_FREE;
            case str_RUN:
                return STATUS_RUN;
            case str_FINISH:
                return STATUS_FINISH;
            case str_ERROR:
                return STATUS_ERROR;
        }
        return STATUS_FREE;
    }

    public final static String nameTray = "PAN";//托盘
    public final static String namePCR = "PCR";//pcr
    public final static String nameRNA = "RNA";//核提
    public final static String nameARM = "ARM";//机械臂
    public final static String namePCRCover = "PCR_COVER";//pcr cover
    public static int ConvertDeviceType(String type){
        switch(type) {
            case ConstantData.nameTray:
                return ConstantData.DEVICE_TRAY;
            case ConstantData.namePCR:
                return ConstantData.DEVICE_PCR;
            case ConstantData.nameRNA:
                return ConstantData.DEVICE_RNA;
            case ConstantData.nameARM:
                return ConstantData.DEVICE_ARM;
            case ConstantData.namePCRCover:
                return ConstantData.DEVICE_PCRCOVER;
            default:
                return 0;
        }
    }
    public final static int WORK_FREE = 0;//无操作
    public final static int WORK_GET = 1;//取托盘
    public final static int WORK_RUN = 2;//跑模板
    public final static int WORK_BACK = 3;//送托盘
    public final static int WORK_MOVE = 4;//移托盘
    public final static int WORK_RECEIVE = 5;//接托盘

    public final static int FLAG_FREE = 0;//tray free
    public final static int FLAG_RUN = 1;//tray
    public final static int FLAG_PAUSE = 2;//tray
    public final static int FLAG_STOP = 3;//tray

    public final static int ALARM_ACK = 1;//
    public final static int ALARM_DEVICE = 2;//
    public final static int ALARM_LINUX = 3;//

    //机械臂任务
    public final static int ARM_FREE = 0;//默认
    public final static int ARM_RESET = 1;//
    public final static int ARM_CONTINUE = 2;//
    public final static int ARM_START = 3;//托盘-PCR-核提
    public final static int ARM_STOP = 4;//
    public final static int ARM_GET = 5;//取托盘
    public final static int ARM_PUT = 6;//放托盘动作
    public final static int ARM_CLOSERNA = 7;//
    public final static int ARM_OPENRNA = 8;
    public final static int ARM_PCRCOVER = 9;//机械臂上放回pcr
    public final static int ARM_TEMPPCRCOVER = 10;//临时盖子位放回pcr
    public final static int ARM_PUTTRAY = 11;//机械臂上放托盘

    public final static String RESET_EXCEPTION = "reset";//
    public final static String RNA_opendoor = "opendoor";//
    public final static String RNA_closedoor = "closedoor";//
    public final static String STR_RESET = "robot_reset";//
    public final static String STR_ARMRESET = "robot_arm_reset";//
    public final static String STR_FINISH = "finish";//
    public final static String STR_FINISHERROR = "finish error";//

    public static final int MSG_TOAST = 200;
    public static final int MSG_UPDATE_UI = 201;
    public static final int MSG_ALARM = 202;
    public static final int MSG_ALARMSound = 203;
    public static final int MSG_ARMTASK = 204;
    public static final int MSG_RESTARTWSS = 205;
    public static final int MSG_RESTOPWSS = 206;
    public static final int MSG_START_PCR = 207;
    public static final int MSG_Finish_PCR = 208;
    public static final int MSG_Reset_PCR = 209;
    public static final int MSG_ARM_ERROR = 210;
    public static final int MSG_SoundBB = 211;
    public static final int MSG_START_RNA = 212;
    public static final int MSG_Finish_RNA = 213;
    public static final int MSG_FinishError_RNA = 214;
    public static final int MSG_Reset_RNA = 215;
    public static final int MSG_ARM_WORK = 216;
    public static final int MSG_RNA_ERROR = 217;
    public static final int MSG_PCR_ERROR = 218;
    public static final int MSG_CLEAR_PCR = 219;
    public static final int MSG_CLEAR_RNA = 220;
    public static final int MSG_FinishError_PCR = 221;
    public static final int MSG_SAVE_PCR = 222;
    public static final int MSG_SAVE_RNA = 223;
    public static final int MSG_SAVE_TRAY = 224;
    public static final int MSG_SAVE_ARM = 225;
    public static final int MSG_CLEAR_TRAY = 226;
    public static final int MSG_CLEAR_ALL = 227;
    public static final int MSG_AckError_RNA = 228;//RNA 模板下发 ack 失败/超时，标记异常并取回托盘
    public static final int MSG_AckError_PCR = 229;//PCR 模板下发 ack 失败/超时，标记异常并取回托盘
}
