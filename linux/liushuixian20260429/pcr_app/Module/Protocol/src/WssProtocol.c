#include "WssProtocol.h"
#include "Logger.h"
#include "cJSON.h"
#include "ConfLog.h"
#include "ConfLocal.h"

static LocalCfg s_local_cfg = {0};
static u_int64_t s_seq_id = 0;
static LogCfg s_log_cfg = {0};

void SetLogCfg() {
    int ret = 0;
    ret = LoadLogCfgFile(&s_log_cfg);
    if (ret != 0) {
        err_printf("Load log cfg file err, set default cfg.\n");
    }
    snprintf(s_log_cfg.log_dir, sizeof(s_log_cfg.log_dir), "%s", LOGGER_DEFAULT_LOG_DIR);
    s_log_cfg.file_size = LOGGER_DEFAULT_FILE_SIZE;
}

int SetLocalCfg(LocalCfg* cfg) {
    if (cfg == NULL) {
        err_printf("Input err. LocalCfg ptr is NULL.\n");
        return -1;
    }

    memcpy(&s_local_cfg, cfg, sizeof(LocalCfg));
    return 0;
}

cJSON* WssDataHeaderParse(WssHeader* header, char* data) {
    if (data == NULL || header == 0) {
        err_printf("Input err, data ptr is NULL or data len is 0.\n");
        return NULL;
    }

    cJSON* root_json = cJSON_Parse(data);
    if (root_json == NULL) {
        err_printf("Json parse err, raw datat:%s.\n", data);
        return NULL;
    }

    cJSON* item_json = cJSON_GetObjectItem(root_json, "Type");
    if (item_json == NULL || item_json->valuestring == NULL) {
        err_printf("Json parse key[Type] err, raw data:%s.\n", data);
        cJSON_Delete(root_json);
        return NULL;
    }
    snprintf(header->type, sizeof(header->type), "%s", item_json->valuestring);

    item_json = cJSON_GetObjectItem(root_json, "SeqID");
    if (item_json == NULL) {
        err_printf("Json parse key[SeqID] err, raw data:%s.\n", data);
        cJSON_Delete(root_json);
        return NULL;
    }
    header->seq_id = item_json->valueint;

    item_json = cJSON_GetObjectItem(root_json, "DeviceID");
    if (item_json == NULL || item_json->valuestring == NULL) {
        err_printf("Json parse key[DeviceID] err, raw data:%s.\n", data);
        cJSON_Delete(root_json);
        return NULL;
    }
    snprintf(header->device_id, sizeof(header->device_id), "%s", item_json->valuestring);
    if (strcmp(header->type, CMD_TYPE_QUERY_DEVICE) != 0) {
        if (strcmp(header->device_id, s_local_cfg.dev_id) != 0) {
            err_printf("Invalide cmd device_id:%s is no local device_id:%s.\n", header->device_id, s_local_cfg.dev_id);
            cJSON_Delete(root_json);
            return NULL;
        }
    }

    return root_json;
}

cJSON* WssBinaryDataHeaderParse(WssHeader* header, char* data) {
    char desc_data[MAX_JSON_DATA_LEN] = {0};
    WssBinaryHeader bin_header = {0};
    char dev_type[32] = {0};

    if (data == NULL || header == 0) {
        err_printf("Input err, data ptr is NULL or data len is 0.\n");
        return NULL;
    }

    memcpy(&bin_header, data, sizeof(WssBinaryHeader));
    if (ntohl(bin_header.header_flag) != WSS_BINARY_HEADER_FLAG) {
        err_printf("Parse binary header flag failed.\n");
        return NULL;
    }

    header->desc_data_len = ntohl(bin_header.desc_data_len);
    memcpy(desc_data, data+sizeof(WssBinaryHeader), header->desc_data_len);
    cJSON* root_json = cJSON_Parse(desc_data);
    if (root_json == NULL) {
        err_printf("Json parse err, raw datat:%s.\n", desc_data);
        return NULL;
    }

    cJSON* item_json = cJSON_GetObjectItem(root_json, "Type");
    if (item_json == NULL || item_json->valuestring == NULL) {
        err_printf("Json parse key[Type] err, raw data:%s.\n", desc_data);
        cJSON_Delete(root_json);
        return NULL;
    }
    snprintf(header->type, sizeof(header->type), "%s", item_json->valuestring);

    item_json = cJSON_GetObjectItem(root_json, "DeviceID");
    if (item_json == NULL || item_json->valuestring == NULL) {
        err_printf("Json parse key[DeviceID] err, raw data:%s.\n", desc_data);
        cJSON_Delete(root_json);
        return NULL;
    }
    snprintf(header->device_id, sizeof(header->device_id), "%s", item_json->valuestring);
    if (strcmp(header->device_id, s_local_cfg.dev_id) != 0) {
        err_printf("Invalide cmd device_id:%s is no local device_id:%s.\n", header->device_id, s_local_cfg.dev_id);
        cJSON_Delete(root_json);
        return NULL;
    }

    item_json = cJSON_GetObjectItem(root_json, "DeviceType");
    if (item_json == NULL || item_json->valuestring == NULL) {
        err_printf("Json parse key[DeviceType] err, raw data:%s.\n", desc_data);
        cJSON_Delete(root_json);
        return NULL;
    }
    snprintf(dev_type, sizeof(dev_type), "%s", item_json->valuestring);
    if (strcmp(dev_type, s_local_cfg.dev_type) != 0) {
        err_printf("Invalide cmd device_type:%s is no local device_type:%s.\n", dev_type, s_local_cfg.dev_type);
        cJSON_Delete(root_json);
        return NULL;
    }

    item_json = cJSON_GetObjectItem(root_json, "SeqID");
    if (item_json == NULL) {
        err_printf("Json parse key[SeqID] err, raw data:%s.\n", desc_data);
        cJSON_Delete(root_json);
        return NULL;
    }
    header->seq_id = item_json->valueint;

    return root_json;
}

//pack protocol
u_int8_t* PackQueryDeviceReply(u_int32_t seq_id) {
    char* json_str= NULL;

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_QUERY_DEVICE_REPLY);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "Reply", "OK");
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackHeartBeatReply(u_int32_t seq_id) {
    char* json_str= NULL;

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_HEART_BEAT_REPLY);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Version", SOFTWARE_VERSION);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackPCRStatusReport(PCRStatus* status) {
    char* json_str= NULL;

    if (status == NULL) {
        err_printf("Input err, status ptr is NULL.\n");
        return -1;
    }

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_PCR_STATUS);
    cJSON_AddNumberToObject(root_json, "SeqID", s_seq_id++);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Status", status->status);
    cJSON_AddStringToObject(root_json, "Alarm", status->alarm);
    cJSON_AddStringToObject(root_json, "Template", status->template_name);
    cJSON_AddStringToObject(root_json, "Info", status->info);
    cJSON_AddNumberToObject(root_json, "CurrentCycle", status->cur_cycle);
    cJSON_AddNumberToObject(root_json, "TotalCycle", status->total_cycle);
    cJSON_AddNumberToObject(root_json, "CurrentSection", status->cur_section);
    cJSON_AddNumberToObject(root_json, "CurrentStep", status->cur_step);
    cJSON_AddNumberToObject(root_json, "RunningTime", status->running_time);
    cJSON_AddNumberToObject(root_json, "WaitingTime", status->waiting_time);
    cJSON_AddNumberToObject(root_json, "LeftTime", status->left_time);
    cJSON_AddNumberToObject(root_json, "TotalTime", status->total_time);
    cJSON_AddNumberToObject(root_json, "Degree", status->degree);
    cJSON_AddNumberToObject(root_json, "DegreeTarget", status->target_degree);
    cJSON_AddStringToObject(root_json, "LinuxStatus", status->linux_status);
    cJSON_AddStringToObject(root_json, "LinuxAlarm", status->linux_alarm);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackPCRRunningStatusReport(PCRRunningStatus* running_status) {
    char* json_str= NULL;

    if (running_status == NULL) {
        err_printf("Input err, running_status ptr is NULL.\n");
        return -1;
    }

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_PCR_RUNNING_STATUS);
    cJSON_AddNumberToObject(root_json, "SeqID", s_seq_id++);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Status", running_status->status);
    cJSON_AddStringToObject(root_json, "Alarm", running_status->alarm);
    cJSON_AddStringToObject(root_json, "Template", running_status->template_name);
    cJSON_AddStringToObject(root_json, "Info", running_status->info);
    cJSON_AddNumberToObject(root_json, "CurrentCycle", running_status->cur_cycle);
    cJSON_AddNumberToObject(root_json, "TotalCycle", running_status->total_cycle);
    cJSON_AddNumberToObject(root_json, "CurrentSection", running_status->cur_section);
    cJSON_AddNumberToObject(root_json, "CurrentStep", running_status->cur_step);
    cJSON_AddNumberToObject(root_json, "RunningTime", running_status->running_time);
    cJSON_AddNumberToObject(root_json, "WaitingTime", running_status->waiting_time);
    cJSON_AddNumberToObject(root_json, "RemainTime", running_status->remain_time);
    cJSON_AddNumberToObject(root_json, "LeftTime", running_status->left_time);
    cJSON_AddNumberToObject(root_json, "TotalTime", running_status->total_time);
    cJSON_AddNumberToObject(root_json, "Degree", running_status->degree);
    cJSON_AddNumberToObject(root_json, "DegreeTarget", running_status->target_degree);
    cJSON_AddStringToObject(root_json, "LinuxStatus", running_status->linux_status);
    cJSON_AddStringToObject(root_json, "LinuxAlarm", running_status->linux_alarm);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackPCRAckReport(u_int32_t seq_id, PCRAck* ack) {
    char* json_str= NULL;

    if (ack == NULL) {
        err_printf("Input err, ack ptr is NULL.\n");
        return -1;
    }

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_PCR_ACK);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Reply", ack->status);
    cJSON_AddStringToObject(root_json, "Alarm", ack->alarm);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackPCRFinishAckReport(u_int32_t seq_id, PCRAck* ack) {
    char* json_str= NULL;

    if (ack == NULL) {
        err_printf("Input err, ack ptr is NULL.\n");
        return -1;
    }

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_PCR_STOP_ACK);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Reply", ack->status);
    cJSON_AddStringToObject(root_json, "Alarm", ack->alarm);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackPCRDoneStatusReport(u_int32_t seq_id, PCRDoneStatus* done_status) {
    char* json_str= NULL;

    if (done_status == NULL) {
        err_printf("Input err, done_status ptr is NULL.\n");
        return -1;
    }

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_PCR_DONE_STATUS);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Status", done_status->status);
    cJSON_AddStringToObject(root_json, "Alarm", done_status->alarm);
    cJSON_AddStringToObject(root_json, "Template", done_status->template_name);
    cJSON_AddStringToObject(root_json, "Info", done_status->info);
    cJSON_AddNumberToObject(root_json, "CurrentCycle", done_status->cur_cycle);
    cJSON_AddNumberToObject(root_json, "TotalCycle", done_status->total_cycle);
    cJSON_AddNumberToObject(root_json, "CurrentSection", done_status->cur_section);
    cJSON_AddNumberToObject(root_json, "CurrentStep", done_status->cur_step);
    cJSON_AddNumberToObject(root_json, "RunningTime", done_status->running_time);
    cJSON_AddNumberToObject(root_json, "WaitingTime", done_status->waiting_time);
    cJSON_AddNumberToObject(root_json, "LeftTime", done_status->left_time);
    cJSON_AddNumberToObject(root_json, "TotalTime", done_status->total_time);
    cJSON_AddNumberToObject(root_json, "Degree", done_status->degree);
    cJSON_AddNumberToObject(root_json, "DegreeTarget", done_status->target_degree);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackPCRResultReport(cJSON* experiment, char* template_name) {
    char* json_str= NULL;

    if (experiment == NULL) {
        err_printf("Input err, experiment ptr is NULL.\n");
        return -1;
    }

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON* experiment_json = cJSON_Duplicate(experiment, 1);

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_PCR_RESULT_REPORT);
    cJSON_AddNumberToObject(root_json, "SeqID", s_seq_id++);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Status", "running");
    cJSON_AddItemToObject(root_json, "Experiment", experiment_json);
    cJSON_AddStringToObject(root_json, "Template", template_name);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackPCRLogReport(char* file_name, unsigned long* data_len) {
    char* json_str= NULL;
    FILE* fp = NULL;
    u_int32_t file_size = 0;
    char md5[MD5_STR_LEN] = {0};
    WssBinaryHeader header = {0};

    if (file_name == NULL) {
        err_printf("Input err, file_name ptr is NULL.\n");
        return NULL;
    }

    fp = fopen(file_name, "rb");
    if (fp == NULL) {
        err_printf("Open log file:%s err.\n", file_name);
        return NULL;
    }
    fseek(fp, 0, SEEK_END);
	file_size = ftell(fp);
	rewind(fp);

    ComputeFileMD5(file_name, md5);

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        fclose(fp);       
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_PCR_LOG_REPORT);
    cJSON_AddNumberToObject(root_json, "SeqID", ++s_seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "FileName", file_name);
    cJSON_AddNumberToObject(root_json, "FileSize", file_size);
    cJSON_AddStringToObject(root_json, "CheckSum", md5);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        fclose(fp);
        cJSON_Delete(root_json);
        return NULL;
    }
    cJSON_Delete(root_json);

    unsigned long total_len = sizeof(WssBinaryHeader) + strlen(json_str) + file_size;
    char* file_data = malloc(total_len);
    if (file_data == NULL) {
        err_printf("Malloc mem for file err.\n");
        fclose(fp);
        free(json_str);
        return NULL;
    }
    memset(file_data, 0, total_len);

    header.header_flag = htonl(WSS_BINARY_HEADER_FLAG);
    header.desc_data_len = htonl(strlen(json_str));

    memcpy(file_data, &header, sizeof(WssBinaryHeader));
    memcpy(file_data + sizeof(WssBinaryHeader), json_str, strlen(json_str));
    fread(file_data + sizeof(WssBinaryHeader) + strlen(json_str), 1, file_size, fp);
    
    fclose(fp);
    free(json_str);
    *data_len = total_len;

    return (u_int8_t*)file_data;
}

u_int8_t* PackUpgradeAckReport(u_int32_t seq_id, PCRAck* ack) {
    char* json_str= NULL;

    if (ack == NULL) {
        err_printf("Input err, ack ptr is NULL.\n");
        return -1;
    }

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_UPGRADE_ACK);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Reply", ack->status);
    cJSON_AddStringToObject(root_json, "Alarm", ack->alarm);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackUpgradeDoneReport(u_int32_t seq_id, UpgradeDoneStatus* status) {
    char* json_str= NULL;

    if (status == NULL) {
        err_printf("Input err, status ptr is NULL.\n");
        return -1;
    }

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_UPGRADE_DONE_STATUS);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Status", status->status);
    cJSON_AddStringToObject(root_json, "Alarm", status->alarm);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackSetLogLevelReply(u_int32_t seq_id, u_int8_t status) {
    char* json_str= NULL;

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_SET_LOG_LEVER_REPLY);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Version", SOFTWARE_VERSION);

    switch (status)
    {
    case SET_LOG_LEVEL_OK:
        cJSON_AddStringToObject(root_json, "Status", "ok");
        cJSON_AddStringToObject(root_json, "ErrMsg", "");
        break;

    case SET_LOG_LEVEL_PARAM_INVALID:
        cJSON_AddStringToObject(root_json, "Status", "error");
        cJSON_AddStringToObject(root_json, "ErrMsg", "param is invalid");
        break;

    case SET_LOG_LEVEL_JSON_ERR:
        cJSON_AddStringToObject(root_json, "Status", "error");
        cJSON_AddStringToObject(root_json, "ErrMsg", "json parse err");
        break;
    
    default:
        break;
    }

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackUpdateCfgReply(u_int32_t seq_id, PCRAck* status) {
    char* json_str= NULL;

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_UPDATE_CONFIG_REPLY);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Version", SOFTWARE_VERSION);
    cJSON_AddStringToObject(root_json, "Status", status->status);
    cJSON_AddStringToObject(root_json, "ErrMsg", status->alarm);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

u_int8_t* PackFactoryResetReply(u_int32_t seq_id, PCRAck* status) {
    char* json_str= NULL;

    cJSON* root_json = cJSON_CreateObject();
    if (root_json == NULL) {
        err_printf("Create json object err.\n");
        return NULL;
    }

    cJSON_AddStringToObject(root_json, "Type", CMD_TYPE_FACTORY_RESET_REPLY);
    cJSON_AddNumberToObject(root_json, "SeqID", seq_id);
    cJSON_AddStringToObject(root_json, "DeviceID", s_local_cfg.dev_id);
    cJSON_AddStringToObject(root_json, "DeviceType", s_local_cfg.dev_type);
    cJSON_AddNumberToObject(root_json, "Position", s_local_cfg.position);
    cJSON_AddStringToObject(root_json, "Version", SOFTWARE_VERSION);
    cJSON_AddStringToObject(root_json, "Status", status->status);
    cJSON_AddStringToObject(root_json, "ErrMsg", status->alarm);

    json_str = cJSON_PrintUnformatted(root_json);
    if (json_str == NULL) {
        err_printf("Json print err.\n");
        cJSON_Delete(root_json);
        return NULL;
    }

    cJSON_Delete(root_json);
    return (u_int8_t*)json_str;
}

//parse protocol
int ParseSection(cJSON* json, Section* section) {
    cJSON* item_json = NULL;
    cJSON* array_obj_json = NULL;
    cJSON* array_item_json = NULL;
    int i = 0;

    if (json == NULL || section == NULL) {
        err_printf("Input err, json or section ptr is NULL.\n");
        return -1;
    }

    item_json = cJSON_GetObjectItem(json, "ScanMode");
    if (item_json) {
        section->section_type = SECTION_TYPE_MELT;
        section->melt_section.scan_mode = item_json->valueint;
    } else {
        section->section_type = SECTION_TYPE_AMP;
    }

    switch (section->section_type)
    {
    case SECTION_TYPE_MELT:
        item_json = cJSON_GetObjectItem(json, "Rate");
        if (item_json) {
            section->melt_section.rate = item_json->valuedouble;
        } else {
            err_printf("Json parse key[Rate] err.\n");
            return -1;
        }

        item_json = cJSON_GetObjectItem(json, "Interval");
        if (item_json) {
            section->melt_section.interval = item_json->valuedouble;
        } else {
            err_printf("Json parse key[Interval] err.\n");
            return -1;
        }

        item_json = cJSON_GetObjectItem(json, "HoldTime");
        if (item_json) {
            section->melt_section.hold_time = item_json->valueint;
        } else {
            err_printf("Json parse key[HoldTime] err.\n");
            return -1;
        }

        item_json = cJSON_GetObjectItem(json, "StartTemp");
        if (item_json) {
            section->melt_section.start_temp = item_json->valuedouble;
        } else {
            err_printf("Json parse key[StartTemp] err.\n");
            return -1;
        }

        item_json = cJSON_GetObjectItem(json, "EndTemp");
        if (item_json) {
            section->melt_section.end_temp = item_json->valuedouble;
        } else {
            err_printf("Json parse key[EndTemp] err.\n");
            return -1;
        }

        item_json = cJSON_GetObjectItem(json, "Name");
        if (item_json && item_json->valuestring) {
            snprintf(section->melt_section.name, sizeof(section->melt_section.name), "%s", item_json->valuestring);
        } else {
            err_printf("Json parse key[Name] err.\n");
            return -1;
        }
        break;

    case SECTION_TYPE_AMP:
        item_json = cJSON_GetObjectItem(json, "Cycles");
        if (item_json) {
            if (item_json->valueint > 0) {
                section->amp_section.cycles = item_json->valueint;
            } else if (item_json->valueint == 0) {
                section->amp_section.cycles = 1;
            } else {
                err_printf("Json parse key[Cycles] err, value is invalid.\n");
                return -1;
            }           
        } else {
            err_printf("Json parse key[Cycles] err.\n");
            return -1;
        }

        item_json = cJSON_GetObjectItem(json, "Name");
        if (item_json && item_json->valuestring) {
            snprintf(section->amp_section.name, sizeof(section->amp_section.name), "%s", item_json->valuestring);
        } else {
            err_printf("Json parse key[Name] err.\n");
            return -1;
        }

        item_json = cJSON_GetObjectItem(json, "Items");
        if (item_json) {
            section->amp_section.step_num = cJSON_GetArraySize(item_json);
            for (i = 0; i < section->amp_section.step_num; i++) {
                array_obj_json = cJSON_GetArrayItem(item_json, i);
                if (array_obj_json == NULL) {
                    err_printf("Json parse err, get [Items] array %d item err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "Name");
                if (array_item_json && array_item_json->valuestring) {
                    snprintf(section->amp_section.steps[i].name, sizeof(section->amp_section.steps[i].name), "%s", array_item_json->valuestring);
                } else {
                    err_printf("Json parse array %d item key[Name] err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "Temperature");
                if (array_item_json) {
                    section->amp_section.steps[i].temp = array_item_json->valuedouble;
                } else {
                    err_printf("Json parse array %d item key[Temperature] err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "HoldTime");
                if (array_item_json) {
                    section->amp_section.steps[i].hold_time = array_item_json->valueint;
                } else {
                    err_printf("Json parse array %d item key[HoldTime] err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "IsAcquisition");
                if (array_item_json) {
                    section->amp_section.steps[i].is_acquisition = array_item_json->valueint;
                } else {
                    err_printf("Json parse array %d item key[IsAcquisition] err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "IsAdvProgram");
                if (array_item_json) {
                    section->amp_section.steps[i].is_adv_program = array_item_json->valueint;
                } else {
                    err_printf("Json parse array %d item key[IsAdvProgram] err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "StartCycle");
                if (array_item_json) {
                    section->amp_section.steps[i].start_cycle = array_item_json->valueint;
                } else {
                    err_printf("Json parse array %d item key[StartCycle] err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "TempVarValue");
                if (array_item_json) {
                    section->amp_section.steps[i].temp_var_value = array_item_json->valuedouble;
                } else {
                    err_printf("Json parse array %d item key[TempVarValue] err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "TimeVarValue");
                if (array_item_json) {
                    section->amp_section.steps[i].time_var_value = array_item_json->valueint;
                } else {
                    err_printf("Json parse array %d item key[TimeVarValue] err.\n", i);
                    return -1;
                }

                array_item_json = cJSON_GetObjectItem(array_obj_json, "Ramp");
                if (array_item_json) {
                    section->amp_section.steps[i].ramp = array_item_json->valueint;
                } else {
                    err_printf("Json parse array %d item key[Ramp] err.\n", i);
                    return -1;
                }
            }
        } else {
            err_printf("Json parse key[Items] err.\n");
            return -1;
        }
        break;
    
    default:
        break;
    }

    return 0;
}

int ParseUpgradeDescInfo(cJSON* root_json, UpgradeDescInfo* desc_info) {
    if (root_json == NULL || desc_info == NULL) {
        err_printf("Input err, JSON or UpgradeDescInfo ptr is NULL.\n");
        return -1;
    }

    cJSON* item_json = cJSON_GetObjectItem(root_json, "Version");
    if (item_json == NULL || item_json->valuestring == NULL) {
        err_printf("JSON parse key[Version] err.\n");
        return -1;
    }
    snprintf(desc_info->version, sizeof(desc_info->version), "%s", item_json->valuestring);

    item_json = cJSON_GetObjectItem(root_json, "FileName");
    if (item_json == NULL) {
        err_printf("JSON parse key[FileName] err.\n");
        return -1;
    }
    snprintf(desc_info->file_name, sizeof(desc_info->file_name), "%s", item_json->valuestring);

    item_json = cJSON_GetObjectItem(root_json, "FileSize");
    if (item_json == NULL) {
        err_printf("JSON parse key[FileSize] err.\n");
        return -1;
    }
    desc_info->file_size = item_json->valueint;

    item_json = cJSON_GetObjectItem(root_json, "CheckSum");
    if (item_json == NULL) {
        err_printf("JSON parse key[CheckSum] err.\n");
        return -1;
    }
    snprintf(desc_info->md5, sizeof(desc_info->md5), "%s", item_json->valuestring);

    return 0;
}

int ParseUpgradeVersionFile(char* data, UpgradeDescInfo* desc_info) {
    FILE* fp = NULL;
    char file_path[MAX_FILE_NAME_LEN] = {0};
    char md5[MD5_STR_LEN] = {0};

    if (data == NULL || desc_info == NULL) {
        err_printf("Input err, data or UpgradeDescInfo ptr is NULL.\n");
        return -1;
    }

    snprintf(file_path, sizeof(file_path), "%s%s", CONFIG_DIR_PATH, desc_info->file_name);
    fp = fopen(file_path, "wb");
    if (fp == NULL) {
        err_printf("Open file:%s err.\n", file_path);
        return -1;
    }
    fwrite(data, 1, desc_info->file_size, fp);
    fclose(fp);

    ComputeFileMD5(file_path, md5);
    if (strcmp(md5, desc_info->md5) == 0) {
        return 0;
    } else {
        return -2;
    }
}
