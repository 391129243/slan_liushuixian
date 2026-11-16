#include "ConfLog.h"
#include "cJSON.h"
#include "Logger.h"

int LoadLogCfgFile(LogCfg *pCfg) {
	int ret = -1;
	FILE *fp = NULL;
	cJSON* root_json = NULL;
	char json_data[MAX_JSON_DATA_LEN] = {0};

    if (pCfg == NULL) {
        err_printf("Input err, cfg ptr is NULL.\n");
        return -1;
    }

	fp = fopen(LOG_CFG_FILE, "r");
	if (fp == NULL) {
		err_printf("log config file open error.\r\n");
		return -1;
	}

	int data_len = fread(json_data, 1, MAX_JSON_DATA_LEN, fp);
	
	fclose(fp);
	fp = NULL;

	if ((data_len == 0) || (data_len >= MAX_JSON_DATA_LEN)) {
		err_printf("Read file %s  data_len %d err. \r\n", LOG_CFG_FILE, data_len);
		return -1;
	}

	root_json = cJSON_Parse(json_data);
	if (root_json == NULL) {
		err_printf("Parse file %s json err. \r\n", LOG_CFG_FILE);
		return -1;
	}		

	cJSON* child_json = NULL;
	child_json = cJSON_GetObjectItem(root_json, "log_dir");
	if (child_json != NULL) {
		snprintf(pCfg->log_dir, sizeof(pCfg->log_dir), "%s", child_json->valuestring);
	}

	child_json = cJSON_GetObjectItem(root_json, "log_prefix");
	if (child_json != NULL) {
		snprintf(pCfg->log_prefix, sizeof(pCfg->log_prefix), "%s", child_json->valuestring);
	}

	child_json = cJSON_GetObjectItem(root_json, "debug_prefix");
	if (child_json != NULL) {
		snprintf(pCfg->debug_prefix, sizeof(pCfg->debug_prefix), "%s", child_json->valuestring);
	}

	child_json = cJSON_GetObjectItem(root_json, "file_size");
	if (child_json != NULL) {
		pCfg->file_size = child_json->valueint;
	}

	cJSON_Delete(root_json);
	root_json = NULL;

	return 0;
}
