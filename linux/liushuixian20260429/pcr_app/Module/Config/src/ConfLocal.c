#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "ConfLocal.h"
#include "Common.h"
#include "cJSON.h"
#include "Logger.h"
#include "ConfSrv.h"
#include "System.h"

int ParseAppInfo(int argc, char *argv[], AppInfo *app_info)
{
	if (argc < 3) {
        fprintf(stderr,"argv error.\r\n");    
		return -1;
	}

    if (NULL == app_info || NULL == argv) {
        fprintf(stderr,"null pointer error.\r\n");    
		return -1;
    }

	if (NULL != argv[0]) {
		sprintf(app_info->app_name, "%s", argv[0]);
	}
	
	if (NULL != argv[1]) {
		app_info->app_id = atoi(argv[1]);
	}
	
	if (NULL != argv[2]) {
		app_info->debug_level = atoi(argv[2]);
	}

    //fprintf(stdout,"%s app_info->debug_level = %d app_info->app_id = %d.\r\n", app_info->app_name, app_info->debug_level, app_info->app_id); 
	return 0; 
}

static int ReadLocalCfgData(LocalCfg* pCfg, cJSON* root_json)
{
	cJSON* child_json = NULL;
	cJSON* item_json = NULL;

	if ((pCfg == NULL) || (root_json == NULL)) {
		return -1;
	}

	child_json = cJSON_GetObjectItem(root_json, "dev_id");
	if (child_json != NULL && child_json->valuestring != NULL) {
		snprintf(pCfg->dev_id, sizeof(pCfg->dev_id), "%s", child_json->valuestring);
	} else {
		err_printf("Json parse key[dev_id] failed.\n");
		return -1;
	}

	child_json = cJSON_GetObjectItem(root_json, "dev_type");
	if (child_json != NULL && child_json->valuestring != NULL) {
		snprintf(pCfg->dev_type, sizeof(pCfg->dev_type), "%s", child_json->valuestring);
	} else {
		err_printf("Json parse key[dev_type] failed.\n");
		return -1;
	}

	child_json = cJSON_GetObjectItem(root_json, "position");
	if (child_json != NULL) {
		pCfg->position = child_json->valueint;
	} else {
		err_printf("Json parse key[position] failed.\n");
		return -1;
	}

	child_json = cJSON_GetObjectItem(root_json, "uart_param");
	if (child_json != NULL) {
        item_json = cJSON_GetObjectItem(child_json, "dev_name");
		if (item_json && item_json->valuestring) {
			snprintf(pCfg->uart_cfg.dev_name, sizeof(pCfg->uart_cfg.dev_name), "%s", item_json->valuestring);
		} else {
			err_printf("Json parse key[dev_name] failed.\n");
			return -1;
		}

		item_json = cJSON_GetObjectItem(child_json, "baud_rate");
		if (item_json) {
			pCfg->uart_cfg.baud_rate = item_json->valueint;
		} else {
			err_printf("Json parse key[baud_rate] failed.\n");
			return -1;
		}

		item_json = cJSON_GetObjectItem(child_json, "data_bits");
		if (item_json) {
			pCfg->uart_cfg.data_bits = item_json->valueint;
		} else {
			err_printf("Json parse key[data_bits] failed.\n");
			return -1;
		}

		item_json = cJSON_GetObjectItem(child_json, "stop_bits");
		if (item_json) {
			pCfg->uart_cfg.stop_bits = item_json->valueint;
		} else {
			err_printf("Json parse key[stop_bits] failed.\n");
			return -1;
		}

		item_json = cJSON_GetObjectItem(child_json, "parity");
		if (item_json && item_json->valuestring) {
			snprintf(pCfg->uart_cfg.parity, sizeof(pCfg->uart_cfg.parity), "%s", item_json->valuestring);
		} else {
			err_printf("Json parse key[parity] failed.\n");
			return -1;
		}
	} else {
		err_printf("Json parse key[uart_param] failed.\n");
		return -1;
	}

	return 0;
}

void PrintfLocalCfg(LocalCfg* pCfg)
{
	warn_printf("Local config: dev_id = %s, uart dev_name= %s.\r\n", pCfg->dev_id, pCfg->uart_cfg.dev_name);		
}

int LoadLocalCfgFile(LocalCfg *pCfg)
{
	FILE *fp = NULL;
	int json_count = 0;
	char json_data[MAX_JSON_DATA_LEN] = {0};
	cJSON *root_json = NULL;

	if (NULL == pCfg) {
		return -1;
	}
	memset(pCfg, 0, sizeof(LocalCfg));

	fp = fopen(LOCAL_CFG_FILE, "r");
	if (fp == NULL) {
		return -1;
	}

	memset(json_data, 0, MAX_JSON_DATA_LEN);
	json_count = fread(json_data, 1, MAX_JSON_DATA_LEN, fp);
	fclose(fp);

	debug_printf("Config module read file %s count = %d. \r\n", LOCAL_CFG_FILE, json_count);

	if ((json_count == 0) || (json_count >= MAX_JSON_DATA_LEN)) {
		err_printf("Config module read file %s err. \r\n", LOCAL_CFG_FILE);
		return -1;
	}

	root_json = cJSON_Parse(json_data);
	if (root_json == NULL) {
		err_printf("Config module read file %s json err. \r\n", LOCAL_CFG_FILE);
		return -1;
	}
	if (ReadLocalCfgData(pCfg, root_json) != 0) {
        cJSON_Delete(root_json);
        return -1;
    }

	PrintfLocalCfg(pCfg);

	cJSON_Delete(root_json);
	return 0;
}

cJSON* CreateLocalCfgJson(LocalCfg *pCfg) {
	if (pCfg == NULL) {
		return NULL;
	}

	cJSON* json_root = cJSON_CreateObject();
	if (json_root == NULL) {
		return NULL;
	}

	cJSON* json_uart = cJSON_CreateObject();
	if (json_uart == NULL) {
		cJSON_Delete(json_root);
		return NULL;
	}

	cJSON_AddStringToObject(json_root, "dev_id", pCfg->dev_id);
	cJSON_AddStringToObject(json_root, "dev_type", pCfg->dev_type);
	cJSON_AddNumberToObject(json_root, "position", pCfg->position);

	cJSON_AddStringToObject(json_uart, "dev_name", pCfg->uart_cfg.dev_name);
	cJSON_AddNumberToObject(json_uart, "baud_rate", pCfg->uart_cfg.baud_rate);
	cJSON_AddNumberToObject(json_uart, "data_bits", pCfg->uart_cfg.data_bits);
	cJSON_AddNumberToObject(json_uart, "stop_bits", pCfg->uart_cfg.stop_bits);
	cJSON_AddStringToObject(json_uart, "parity", pCfg->uart_cfg.parity);
	cJSON_AddItemToObject(json_root, "uart_param", json_uart);

	return json_root;
}

int SaveLocalCfgFile(LocalCfg *pCfg) {
	char bak_file[MAX_FILE_NAME_LEN] = {0};
	char cmd_str[MAX_FILE_NAME_LEN] = {0};	
	FILE *fp = NULL;
	int count = 0;
	
	if (pCfg == NULL) {
        return -1;
    }

	cJSON* json = CreateLocalCfgJson(pCfg);
	if (json == NULL) {
		return -1;
	}
	char* json_str = cJSON_Print(json);
	if (json_str == NULL) {
		cJSON_Delete(json);
		return -1;
	}
	int len = strlen(json_str);

	sprintf(bak_file, "%s/local_config/local_cfg_bk.json", CONFIG_DIR_PATH);
	if (!access(LOCAL_CFG_FILE, W_OK)) {
		sprintf(cmd_str, "cp -f %s %s", LOCAL_CFG_FILE, bak_file);
		if (system_call(cmd_str) == 0) {
			debuge_printf("System exec success : %s\r\n", cmd_str);
		} else {
			err_printf("System exec failed, %s\r\n", cmd_str);
			return -1;	
		}
	}

	fp = fopen(LOCAL_CFG_FILE,"w+");
	if(fp == NULL) {		
		err_printf("Open %s failed.\r\n", LOCAL_CFG_FILE);
        return -1;	
	}

	count = fwrite(json_str, 1, len, fp);
	if (count != len) {
		err_printf("Save new %s failed. data len = %d, write len = %d.\r\n", LOCAL_CFG_FILE, len, count);
        return -1;
	}

	fclose(fp);	
	cJSON_Delete(json);
	free(json_str);
    return 0;
}
