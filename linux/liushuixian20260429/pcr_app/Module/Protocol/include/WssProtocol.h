#ifndef __WSS_PROTOCOL_H__
#define __WSS_PROTOCOL_H__

#ifdef __cplusplus
extern "C"
{
#endif

#include <sys/types.h>

#include "Common.h"
#include "cJSON.h"
#include "ConfLocal.h"
#include "md5.h"

#define CMD_TYPE_QUERY_DEVICE "QueryDevice"
#define CMD_TYPE_QUERY_DEVICE_REPLY "QueryDeviceReply"
#define CMD_TYPE_HEART_BEAT "HeartBeat"
#define CMD_TYPE_HEART_BEAT_REPLY "HeartBeatReply"
#define CMD_TYPE_PCR_REQ "PCRReq"
#define CMD_TYPE_PCR_ACK "PCRAck"
#define CMD_TYPE_PCR_STOP "PCRStop"
#define CMD_TYPE_PCR_STOP_ACK "PCRFinishAck"
#define CMD_TYPE_PCR_STATUS "PCRStatus"
#define CMD_TYPE_PCR_RUNNING_STATUS "PCRRunningStatus"
#define CMD_TYPE_PCR_DONE_STATUS "PCRFinish"
#define CMD_TYPE_PCR_DONE_STATUS_REPLY "PCRFinnishAck"
#define CMD_TYPE_PCR_RESULT_REPORT "PCRCurve"
#define CMD_TYPE_PCR_LOG_REPORT "PCRLog"
#define CMD_TYPE_UPGRADE "Upgrade"
#define CMD_TYPE_UPGRADE_ACK "UpgradeAck"
#define CMD_TYPE_UPGRADE_DONE_STATUS "UpgradeFinish"
#define CMD_TYPE_SET_LOG_LEVER "SetLogLevel"
#define CMD_TYPE_SET_LOG_LEVER_REPLY "SetLogLevelReply"
#define CMD_TYPE_UPDATE_CONFIG "UpdateConfig"
#define CMD_TYPE_UPDATE_CONFIG_REPLY "UpdateConfigReply"
#define CMD_TYPE_FACTORY_RESET "FactoryReset"
#define CMD_TYPE_FACTORY_RESET_REPLY "FactoryResetReply"

#define PCR_SLAVE_CMD_LED_BLINK "statusled_blink"
#define PCR_SLAVE_CMD_LED_BRIGHT "statusled_bright"
#define PCR_SLAVE_CMD_MOTOR_RESET "motor_reset"
#define PCR_SLAVE_CMD_PCR_SET_TEMP_AMP "pcr_temperature_set"
#define PCR_SLAVE_CMD_MOTOR_SCAN "motor_scan"
#define PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP_END "pcr_fluorescence_get_amp_end"
#define PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP "pcr_fluorescence_get_amp"
#define PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP_START "pcr_fluorescence_get_amp_start"
#define PCR_SLAVE_CMD_GET_CURRENT_TEMP_POST "get_current_temperature_post"
#define PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_MELT "pcr_fluorescence_get_melt"
#define PCR_SLAVE_CMD_GET_CURRENT_TEMP "get_current_temperature"

#define RETYRY_NUM 9
#define RESULT_REPORT_TIME 20

#define WSS_BINARY_HEADER_FLAG 0x5AA5FCFF

typedef struct sWssHeader {
    char type[WSS_CMD_TYPE_LEN];
    char device_id[DEVICE_ID_LEN];
    u_int32_t seq_id;
    u_int32_t desc_data_len;
} WssHeader;

typedef struct sWssBinaryHeader {
    u_int32_t header_flag;
    u_int32_t desc_data_len;
} WssBinaryHeader;

typedef enum eSectionType {
    SECTION_TYPE_AMP,
    SECTION_TYPE_MELT
} SectionType;

typedef struct sAmpStep
{
    char name[PCR_STEP_NAME_LEN];
    double temp;
    int hold_time;
    u_int8_t is_acquisition;
    u_int8_t is_adv_program;
    int start_cycle;
    double temp_var_value;
    int time_var_value;
    double ramp;
} AmpStep;

typedef struct sAmpSection
{
    char name[PCR_SECTION_NAME_LEN];
    int cycles;
    u_int8_t step_num;
    AmpStep steps[MAX_PCR_STEP_NUM];
} AmpSection;

typedef struct sMeltSection
{
    char name[PCR_SECTION_NAME_LEN];
    int scan_mode;
    double rate;
    double interval;
    int hold_time;
    double start_temp;
    double end_temp;
} MeltSection;

typedef struct sSection
{
    u_int16_t section_id;
    u_int8_t section_type;
    AmpSection amp_section;
    MeltSection melt_section;
} Section;

typedef struct sPcrSlaveCmd
{
    u_int32_t id;
    char cmd_name[PCR_STEP_NAME_LEN];
    char section_name[PCR_SECTION_NAME_LEN];
    char desc[PCR_STEP_DESC_LEN];
    u_int16_t current_section;
    u_int16_t current_cycle;
    u_int16_t total_cycle;
    u_int32_t current_step;
    u_int16_t loop;
    double time;
    double hotlid_temp;
    double pcr_temp;
    u_int8_t is_heat;
    u_int8_t is_cold;
    u_int8_t retry_cnt;
    u_int8_t is_acquisition_amp;
    u_int8_t is_acquisition_melt;
} PcrSlaveCmd;

typedef struct sPCRTaskInfo {
    char template_name[MAX_FILE_NAME_LEN];
    u_int32_t seq_id;
    u_int32_t cur_phase;
    u_int32_t cur_cycle;
    u_int32_t cur_section;
    u_int32_t cur_step;
    u_int32_t running_time;
    u_int32_t total_time;
    pthread_t workflow_thread_id;
    char batch_id[TASK_ID_LEN];
} PCRTaskInfo;

typedef struct sPCRStatus {
    char status[SLAVE_STATUS_LEN];
    char alarm[SLAVE_ALARM_LEN];
    char template_name[MAX_TEMPLATE_NAME_LEN];
    int cur_cycle;
    int total_cycle;
    int cur_section;
    int cur_step;
    int running_time;
    int waiting_time;
    int left_time;
    int total_time;
    float degree;
    float target_degree;
    char info[PCR_STEP_DESC_LEN];
    char linux_status[SLAVE_STATUS_LEN];
    char linux_alarm[SLAVE_ALARM_LEN];
} PCRStatus;

typedef struct sPCRRunningStatus {
    char status[SLAVE_STATUS_LEN];
    char alarm[SLAVE_ALARM_LEN];
    char template_name[MAX_TEMPLATE_NAME_LEN];
    int cur_cycle;
    int total_cycle;
    int cur_section;
    int cur_step;
    int running_time;
    int waiting_time;
    int remain_time;
    int left_time;
    int total_time;
    float degree;
    float target_degree;
    char info[PCR_STEP_DESC_LEN];
    char linux_status[SLAVE_STATUS_LEN];
    char linux_alarm[SLAVE_ALARM_LEN];
} PCRRunningStatus;

typedef struct sPCRAck {
    char status[SLAVE_STATUS_LEN];
    char alarm[SLAVE_ALARM_LEN];
} PCRAck;

typedef struct sPCRDoneStatus {
    char status[SLAVE_STATUS_LEN];
    char alarm[SLAVE_ALARM_LEN];
    char template_name[MAX_TEMPLATE_NAME_LEN];
    int cur_cycle;
    int total_cycle;
    int cur_section;
    int cur_step;
    int running_time;
    int waiting_time;
    int left_time;
    int total_time;
    float degree;
    float target_degree;
    char info[PCR_STEP_DESC_LEN];
} PCRDoneStatus;

typedef struct sUpgradeDescInfo {
    u_int64_t file_size;
    char md5[MD5_SUM_LEN];
    char file_name[MAX_FILE_NAME_LEN];
    char version[VERSION_LEN];
} UpgradeDescInfo;

typedef struct sUpgradeDoneStatus {
    char status[SLAVE_STATUS_LEN];
    char alarm[SLAVE_ALARM_LEN];
} UpgradeDoneStatus;

typedef enum eUpgradeStatus {
    UPGRADE_STATUS_SAME_VERSION,
    UPGRADE_STATUS_SUCCESS,
    UPGRADE_STATUS_VERSION_FILE_UNZIP_FAILED,
    UPGRADE_STATUS_VERSION_FILE_BACKUP_FAILED,
    UPGRADE_STATUS_RNA_VERSION_UPDATE_FAILED,
    UPGRADE_STATUS_PCR_VERSION_UPDATE_FAILED
} UpgradeStatus;

typedef enum eSetLogLevelStatus {
    SET_LOG_LEVEL_OK,
    SET_LOG_LEVEL_PARAM_INVALID,
    SET_LOG_LEVEL_JSON_ERR
} SetLogLevelStatus;

int SetLocalCfg(LocalCfg* cfg);

void SetLogCfg();

cJSON* WssDataHeaderParse(WssHeader* header, char* data);

cJSON* WssBinaryDataHeaderParse(WssHeader* header, char* data);

//pack protocol
u_int8_t* PackQueryDeviceReply(u_int32_t seq_id);

u_int8_t* PackHeartBeatReply(u_int32_t seq_id);

u_int8_t* PackPCRStatusReport(PCRStatus* status);

u_int8_t* PackPCRRunningStatusReport(PCRRunningStatus* running_status);

u_int8_t* PackPCRDoneStatusReport(u_int32_t seq_id, PCRDoneStatus* done_status);

u_int8_t* PackPCRAckReport(u_int32_t seq_id, PCRAck* ack);

u_int8_t* PackPCRFinishAckReport(u_int32_t seq_id, PCRAck* ack);

u_int8_t* PackPCRResultReport(cJSON* experiment, char* template_name);

u_int8_t* PackPCRLogReport(char* file_name, unsigned long* data_len);

u_int8_t* PackUpgradeAckReport(u_int32_t seq_id, PCRAck* ack);

u_int8_t* PackUpgradeDoneReport(u_int32_t seq_id, UpgradeDoneStatus* status);

u_int8_t* PackSetLogLevelReply(u_int32_t seq_id, u_int8_t status);

u_int8_t* PackUpdateCfgReply(u_int32_t seq_id, PCRAck* status);

u_int8_t* PackFactoryResetReply(u_int32_t seq_id, PCRAck* status);

//parse protocol
int ParseSection(cJSON* json, Section* section);

int ParseUpgradeDescInfo(cJSON* root_json, UpgradeDescInfo* desc_info);

int ParseUpgradeVersionFile(char* data, UpgradeDescInfo* desc_info);

#ifdef __cplusplus
}
#endif

#endif