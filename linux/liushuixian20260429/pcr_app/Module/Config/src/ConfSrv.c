#include "Common.h"
#include "cJSON.h"
#include "Logger.h"
#include "ConfSrv.h"
#include "System.h"

static int ReadSrvCfgData(SrvCfg *pCfg, cJSON *root_json)
{
	cJSON *child_json = NULL;
	cJSON *c_child_json = NULL;

	if ((pCfg == NULL) || (root_json == NULL)) {
		return -1;
	}

	child_json = cJSON_GetObjectItem(root_json, "ethenet_info");
	if (child_json != NULL)
	{
        c_child_json = cJSON_GetObjectItem(child_json, "ip");
		if (c_child_json != NULL && c_child_json->valuestring)
		{
			snprintf(pCfg->srv_ip, sizeof(pCfg->srv_ip), "%s", c_child_json->valuestring);
		} else {
            err_printf("Parse srv_info key[ip] failed.\n");
            return -1;
        }

		c_child_json = cJSON_GetObjectItem(child_json, "port");
		if (c_child_json != NULL)
		{
			pCfg->port = c_child_json->valueint;
		} else {
            err_printf("Parse srv_info key[port] failed.\n");
            return -1;
        }

		c_child_json = cJSON_GetObjectItem(child_json, "heartbeat_interval");
		if (c_child_json != NULL)
		{
			pCfg->heartbeat = c_child_json->valueint;
		} else {
            err_printf("Parse srv_info key[heartbeat_interval] failed, default value is 5s.\n");
            pCfg->heartbeat = 5;
        }
	}

	return 0;
}

void PrintfServerCfg(SrvCfg* pCfg)
{
	warn_printf("Server config: ethenet_info: ip = %s, port= %d, heartbeat_interval = %d.\r\n", 
                pCfg->srv_ip, pCfg->port, pCfg->heartbeat);		
}

int LoadSrvCfgFile(SrvCfg *pCfg)
{
	FILE *fp = NULL;
	int json_count = 0;
	char json_data[MAX_JSON_DATA_LEN] = {0};
	cJSON *root_json = NULL;

	if (NULL == pCfg) {
		return -1;
	}
	memset(pCfg, 0, sizeof(SrvCfg));

	fp = fopen(SERVER_CFG_FILE, "r");
	if (fp == NULL) {
		return -1;
	}

	memset(json_data, 0, MAX_JSON_DATA_LEN);
	json_count = fread(json_data, 1, MAX_JSON_DATA_LEN, fp);
	fclose(fp);

	debug_printf("Config module read file %s count = %d. \r\n", SERVER_CFG_FILE, json_count);

	if ((json_count == 0) || (json_count >= MAX_JSON_DATA_LEN)) {
		err_printf("Config module read file %s err. \r\n", SERVER_CFG_FILE);
		return -1;
	}

	root_json = cJSON_Parse(json_data);
	if (root_json == NULL) {
		err_printf("Config module read file %s json err. \r\n", SERVER_CFG_FILE);
		return -1;
	}
	if (ReadSrvCfgData(pCfg, root_json) != 0) {
        cJSON_Delete(root_json);
        return -1;
    }

	PrintfServerCfg(pCfg);

	cJSON_Delete(root_json);
	return 0;
}

cJSON* CreateSrvCfgJson(SrvCfg *pCfg) {
	if (pCfg == NULL) {
		return NULL;
	}

	cJSON* json_root = cJSON_CreateObject();
	if (json_root == NULL) {
		return NULL;
	}
	cJSON* json_ethenet_info = cJSON_CreateObject();
	if (json_ethenet_info == NULL) {
		cJSON_Delete(json_root);
		return NULL;
	}

	cJSON_AddStringToObject(json_ethenet_info, "ip", pCfg->srv_ip);
	cJSON_AddNumberToObject(json_ethenet_info, "port", pCfg->port);
    cJSON_AddNumberToObject(json_ethenet_info, "heartbeat_interval", pCfg->heartbeat);

	cJSON_AddItemToObject(json_root, "ethenet_info", json_ethenet_info);

	return json_root;
}

int SaveSrvCfgFile(SrvCfg *pCfg) {
	char bak_file[MAX_FILE_NAME_LEN] = {0};
	char cmd_str[MAX_FILE_NAME_LEN] = {0};	
	FILE *fp = NULL;
	int count = 0;
	
	if (pCfg == NULL) {
        return -1;
    }

	cJSON* json = CreateSrvCfgJson(pCfg);
	if (json == NULL) {
		return -1;
	}
	char* json_str = cJSON_Print(json);
	if (json_str == NULL) {
		cJSON_Delete(json);
		return -1;
	}
	int len = strlen(json_str);

	sprintf(bak_file, "%s/server_config/srv_cfg_bk.json", CONFIG_DIR_PATH);
	if (!access(SERVER_CFG_FILE, W_OK)) {
		sprintf(cmd_str, "cp -f %s %s", SERVER_CFG_FILE, bak_file);
		if (system_call(cmd_str) == 0) {
			debuge_printf("System exec success : %s\r\n", cmd_str);
		} else {
			err_printf("System exec failed, %s\r\n", cmd_str);
			return -1;	
		}
	}

	fp = fopen(SERVER_CFG_FILE,"w+");
	if(fp == NULL) {		
		err_printf("Open %s failed.\r\n", SERVER_CFG_FILE);
        return -1;	
	}

	count = fwrite(json_str, 1, len, fp);
	if (count != len) {
		err_printf("Save new %s failed. data len = %d, write len = %d.\r\n", strlen(json_str), len, count);
        return -1;
	}

	fclose(fp);	
	cJSON_Delete(json);
	free(json_str);
    return 0;
}
