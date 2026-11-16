#ifndef __COMMON_H__
#define __COMMON_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <errno.h>
#include <pthread.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ipc.h>
#include <sys/time.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <time.h>
#include <unistd.h>
#include <sys/stat.h>  
#include <unistd.h>

typedef enum AppId {
    APP_ROOTDAEMON_ID = 0,
    APP_SYSDAEMON_ID,
    APP_LOGGER_ID,
    APP_PCRMGR_ID,
    APP_UPGRADE_ID,
    APP_TOTAL_NUM = 10,
} eAppId;

#define SYSDAEMON_APP APP_PATH_NAME"SysDaemonApp"
#define LOGGER_APP APP_PATH_NAME"LoggerApp"

#define DEVICE_TYPE_RNA "RNA"
#define DEVICE_TYPE_PCR "PCR"
/**************************************************************/
/***********************PATH DEFINITION**********************/
/**************************************************************/
#define APP_PATH_NAME "./bin/"
#define RNA_APP_RUN_PATH "/home/rna_app/apprun"
#define RNA_APP_BAK_PATH "/home/rna_app/appbak"
#define PCR_APP_RUN_PATH "/home/pcr_app/apprun"
#define PCR_APP_BAK_PATH "/home/pcr_app/appbak"
#define LOCAL_LIB_DIR   "./lib/"
#define DEFAULT_CFG_DIR   "./local_config/"
#define CONFIG_DIR_PATH "/config/"
#define ORI_CONFIG_DIR_PATH "./ori_config/"

#define LOCAL_APP_FILE DEFAULT_CFG_DIR"local_app.json"
#define LOG_CFG_FILE DEFAULT_CFG_DIR"log_cfg.json"
#define LOCAL_CFG_FILE CONFIG_DIR_PATH"local_config/local_cfg.json"
#define FILE_MGR_CFG_FILE CONFIG_DIR_PATH"local_config/file_mgr_cfg.json"
#define SERVER_CFG_FILE CONFIG_DIR_PATH"server_config/srv_cfg.json"

#define PCR_RESULT_PATH "/pcr/result"

#define HEARTBEAT_TIME_LEN 3          //心跳时间间隔s
#define HEARTBEAT_TIMEOUT_COUNT 4    //心跳超时次数
#define ID_RESTART_MAGIC_NUMBER 1560  //重启魔术数字，避免异常重启

#define MAX_JSON_DATA_LEN 20480
#define MAX_FILE_NAME_LEN 256

#define MSG_DATA_OFFSET 24
#define HISTORICAL_FILE_REMOVE_SIZE_THRESHOLD 2*1024*1024*1024  //2GB
#define HISTORICAL_FILE_REMOVE_TIME_THRESHOLD 2  //2day bfore

/**************************************************************/
/***********************MSD ID DEFINITION**********************/
/**************************************************************/
#define MSG_ID_HEART 1           //心跳消息
#define MSG_ID_TIMER_OUT 2      //定时器消息
#define MSG_ID_QUIT 3           //退出消息，父进程发送给子进程
#define MSG_ID_RESTART_APP 4    //重启指定应用消息
#define MSG_ID_DEBUG_OUT 5  //调试信息输出
#define MSG_ID_LOG_OUT 6   //日志信息输出

#define MSG_ID_SET_LOG_LEVEL 7
#define MSG_ID_APP_UPGRADE 8
#define MSG_ID_APP_UPGRADE_STATUS 9
#define MSG_ID_REPORT_LOG_FILE 10
#define MSG_ID_UART_DISCONNECT 11
#define MSG_ID_UART_CONNECTED 12
#define MSG_ID_QUERY_START_PARAM 13

/*====================common define==========================*/
typedef struct {
    int dir_cnt;
	char dir_name[500][128];
    int dir_type[500];
} DirList;

typedef struct sRawData {
    u_int8_t data_type;
    unsigned char *data;
    size_t len;
} RawData;

typedef struct sStartParam{
    char app_run[128];
    char app_bak[128];
} StartParam;

#define LOGGER_DEFAULT_DEBUG_FILE_PREFIX       "PcrApp_debug"
#define LOGGER_DEFAULT_LOG_FILE_PREFIX "PcrApp_log"
#define LOGGER_DEFAULT_LOG_DIR         "/logs/pcr_app/"
#define LOGGER_DEFAULT_FILE_SIZE       5242880
#define LOGGER_DEFAULT_RM_DAYS_BEFORE  (30)

#define PCR_SECTION_NAME_LEN 64
#define PCR_STEP_NAME_LEN 64
#define MAX_PCR_STEP_NUM 10
#define PCR_STEP_DESC_LEN 64
#define WSS_CMD_TYPE_LEN 32
#define SLAVE_STATUS_LEN 64
#define SLAVE_ALARM_LEN 128

#define IP_ADDRESS_LEN 32
#define DEVICE_NAME_LEN 32
#define USERNAME_DATA_LEN 32
#define PASSWORD_DATA_LEN 32
#define MAX_FILE_NAME_LEN 256
#define MAX_VERSION_STR_LEN 8
#define TASK_ID_LEN 129
#define DEVICE_ID_LEN 21
#define MAX_TEMPLATE_NAME_LEN 128
#define DATATIME_LEN 24
#define COORDINATE_LEN 16
#define OS_INFO_LEN 32
#define MAX_URL_LEN 128 //URL最长长度
#define MD5_SUM_LEN 64
#define VERSION_LEN 128

#ifdef __cplusplus
}
#endif

#endif