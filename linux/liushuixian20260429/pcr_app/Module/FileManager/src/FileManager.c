#include <pthread.h>
#include <stdio.h>

#include "FileManager.h"
#include "Logger.h"
#include "cJSON.h"
#include "System.h"
#include "Timer.h"

FileManager s_file_mgr = {0};
static pthread_t s_file_mgr_thread_id = 0;
static PTimerMsgInfo s_file_mgr_timer_info = NULL;

void SetDefaultFileMgrCfg() {
    s_file_mgr.mgr_cfg[0].file_type = MGR_FILE_TYPE_RNA_APP_LOG;
    snprintf(s_file_mgr.mgr_cfg[0].file_dir, sizeof(s_file_mgr.mgr_cfg[0].file_dir), "/logs/rna_app/");
    snprintf(s_file_mgr.mgr_cfg[0].prefix, sizeof(s_file_mgr.mgr_cfg[0].prefix), "RnaApp");
    s_file_mgr.mgr_cfg[0].clean_time = 2;
    s_file_mgr.mgr_cfg[0].clean_threshold = 2*1024*1024;

    s_file_mgr.mgr_cfg[1].file_type = MGR_FILE_TYPE_PCR_APP_LOG;
    snprintf(s_file_mgr.mgr_cfg[1].file_dir, sizeof(s_file_mgr.mgr_cfg[0].file_dir), "/logs/pcr_app/");
    snprintf(s_file_mgr.mgr_cfg[1].prefix, sizeof(s_file_mgr.mgr_cfg[0].prefix), "PcrApp");
    s_file_mgr.mgr_cfg[1].clean_time = 2;
    s_file_mgr.mgr_cfg[1].clean_threshold = 2*1024*1024;

    s_file_mgr.mgr_cfg[2].file_type = MGR_FILE_TYPE_PCR_RESULT;
    snprintf(s_file_mgr.mgr_cfg[2].file_dir, sizeof(s_file_mgr.mgr_cfg[0].file_dir), "/pcr/result/");
    snprintf(s_file_mgr.mgr_cfg[2].prefix, sizeof(s_file_mgr.mgr_cfg[0].prefix), "pcr_result");
    s_file_mgr.mgr_cfg[2].clean_time = 2;
    s_file_mgr.mgr_cfg[2].clean_threshold = 2*1024*1024;

    s_file_mgr.type_num = 3;
    s_file_mgr.disk_used_rate_threshold = 0.7;
}

static int ParseFileMgrCfg(cJSON* root_json, MgrFileType file_type) {
    cJSON* child_json = NULL;
    if (root_json == NULL) {
        err_printf("Input err, json ptr is NULL.\n");
        return -1;
    }

    if (file_type >= MAX_MGR_FILE_TYPE_NUM) {
        err_printf("File type:%d is invalide.\n", file_type);
        return -1;
    }

    s_file_mgr.mgr_cfg[file_type].file_type = file_type;
    child_json = cJSON_GetObjectItem(root_json, "file_dir");
	if (child_json != NULL && child_json->valuestring) {
		memset(s_file_mgr.mgr_cfg[file_type].file_dir, 0, MAX_FILE_NAME_LEN);
		snprintf(s_file_mgr.mgr_cfg[file_type].file_dir, MAX_FILE_NAME_LEN, "%s", child_json->valuestring);
	} else {
        return -1;
    }

	child_json = cJSON_GetObjectItem(root_json, "prefix");
	if (child_json != NULL && child_json->valuestring) {
		memset(s_file_mgr.mgr_cfg[file_type].prefix, 0, MAX_FILE_NAME_LEN);
		snprintf(s_file_mgr.mgr_cfg[file_type].prefix, MAX_FILE_NAME_LEN, "%s", child_json->valuestring);
	} else {
        return -1;
    }

	child_json = cJSON_GetObjectItem(root_json, "clean_time");
	if (child_json != NULL) {
		s_file_mgr.mgr_cfg[file_type].clean_time = child_json->valueint;
	} else {
        s_file_mgr.mgr_cfg[file_type].clean_time = 2; //2day before
    }

    child_json = cJSON_GetObjectItem(root_json, "clean_threshold");
	if (child_json != NULL) {
		s_file_mgr.mgr_cfg[file_type].clean_threshold = child_json->valueint;
	} else {
        s_file_mgr.mgr_cfg[file_type].clean_threshold = 2*1024*1024; //2GB   
    }

    s_file_mgr.type_num += 1;
    return 0;
}

int LoadFileManagerConfig()
{
	int ret = -1;
	FILE *fp = NULL;
	cJSON* root_json = NULL;
    cJSON* child_json = NULL;
	char json_data[MAX_JSON_DATA_LEN] = {0};
    int file_type_cnt = 0;

	memset(&s_file_mgr, 0, sizeof(FileManager));
    SetDefaultFileMgrCfg();

	fp = fopen(FILE_MGR_CFG_FILE, "r");
	if (fp == NULL) {
		err_printf("File mgr config file open error, %s.\r\n", FILE_MGR_CFG_FILE);
		return MAX_MGR_FILE_TYPE_NUM;
	}

	int data_len = fread(json_data, 1, MAX_JSON_DATA_LEN, fp);
	
	fclose(fp);
	fp = NULL;

	if ((data_len == 0) || (data_len >= MAX_JSON_DATA_LEN)) {
		err_printf("Read file %s  data_len %d err. \r\n", FILE_MGR_CFG_FILE, data_len);
		return MAX_MGR_FILE_TYPE_NUM;
	}

	root_json = cJSON_Parse(json_data);
	if (root_json == NULL) {
		err_printf("Parse file %s json err. \r\n", FILE_MGR_CFG_FILE);
		return MAX_MGR_FILE_TYPE_NUM;
	}

    child_json = cJSON_GetObjectItem(root_json, "disk");
    if (child_json) {
        cJSON* item_json = cJSON_GetObjectItem(child_json, "used_rate_threshold");
        if (item_json) {
            s_file_mgr.disk_used_rate_threshold = item_json->valuedouble;
        }
    }		

	child_json = cJSON_GetObjectItem(root_json, "rna_log");
    if (child_json) {
        snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].file_dir, MAX_FILE_NAME_LEN, "/logs/rna_app/");
        snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].prefix, MAX_FILE_NAME_LEN, "RnaApp");
        if (ParseFileMgrCfg(child_json, MGR_FILE_TYPE_RNA_APP_LOG) == 0) {
            ret = Mkdir(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].file_dir);
            if (ret < 0) {
                err_printf("mkdir %s failed.\r\n", s_file_mgr.mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].file_dir);
                memset(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].file_dir, 0, MAX_FILE_NAME_LEN);
                snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].file_dir, MAX_FILE_NAME_LEN, "./");
            }
        }
        file_type_cnt++;
    }

	child_json = cJSON_GetObjectItem(root_json, "pcr_log");
    if (child_json) {
        snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].file_dir, MAX_FILE_NAME_LEN, "/logs/pcr_app/");
        snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].prefix, MAX_FILE_NAME_LEN, "PcrApp");
        if (ParseFileMgrCfg(child_json, MGR_FILE_TYPE_PCR_APP_LOG) == 0) {
            ret = Mkdir(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].file_dir);
            if (ret < 0) {
                err_printf("mkdir %s failed.\r\n", s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].file_dir);
                memset(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].file_dir, 0, MAX_FILE_NAME_LEN);
                snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].file_dir, MAX_FILE_NAME_LEN, "./");
            }
        }
        file_type_cnt++;
    }

    child_json = cJSON_GetObjectItem(root_json, "pcr_result");
    if (child_json) {
        snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].file_dir, MAX_FILE_NAME_LEN, "/pcr/result/");
        snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].prefix, MAX_FILE_NAME_LEN, "pcr_result");
        if (ParseFileMgrCfg(child_json, MGR_FILE_TYPE_PCR_RESULT) == 0) {
            ret = Mkdir(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].file_dir);
            if (ret < 0) {
                err_printf("mkdir %s failed.\r\n", s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].file_dir);
                memset(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].file_dir, 0, MAX_FILE_NAME_LEN);
                snprintf(s_file_mgr.mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].file_dir, MAX_FILE_NAME_LEN, "./");
            }
        }
        file_type_cnt++;
    }

	cJSON_Delete(root_json);
	root_json = NULL;
    s_file_mgr.type_num = MAX_MGR_FILE_TYPE_NUM;

	return file_type_cnt;
}

int ReadFileMgrCfg(FileManager *pCfg) {
    if (pCfg == NULL) {
        err_printf("Input err, FileManager cfg ptr is NULL.\n");
        return -1;
    }

    memcpy(pCfg, &s_file_mgr, sizeof(FileManager));
    return 0;
}

int SetFileMgrCfg(FileManager *pCfg) {
    if (pCfg == NULL) {
        err_printf("Input err, FileManager cfg ptr is NULL.\n");
        return -1;
    }

    memcpy(&s_file_mgr, pCfg, sizeof(FileManager));
    return 0;
}

cJSON* CreateFileMgrCfgJson(FileManager *pCfg) {
	if (pCfg == NULL) {
		return NULL;
	}

	cJSON* json_root = cJSON_CreateObject();
	if (json_root == NULL) {
		return NULL;
	}

	cJSON* rna_log = cJSON_CreateObject();
	if (rna_log == NULL) {
		cJSON_Delete(json_root);
		return NULL;
	}

	cJSON_AddStringToObject(rna_log, "file_dir", pCfg->mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].file_dir);
	cJSON_AddStringToObject(rna_log, "prefix", pCfg->mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].prefix);
	cJSON_AddNumberToObject(rna_log, "clean_time", pCfg->mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].clean_time);
    cJSON_AddNumberToObject(rna_log, "clean_threshold", pCfg->mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].clean_threshold);
	cJSON_AddItemToObject(json_root, "rna_log", rna_log);

    cJSON* pcr_log = cJSON_CreateObject();
	if (pcr_log == NULL) {
		cJSON_Delete(json_root);
		return NULL;
	}

	cJSON_AddStringToObject(pcr_log, "file_dir", pCfg->mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].file_dir);
	cJSON_AddStringToObject(pcr_log, "prefix", pCfg->mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].prefix);
	cJSON_AddNumberToObject(pcr_log, "clean_time", pCfg->mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].clean_time);
    cJSON_AddNumberToObject(pcr_log, "clean_threshold", pCfg->mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].clean_threshold);
	cJSON_AddItemToObject(json_root, "pcr_log", pcr_log);

    cJSON* pcr_result = cJSON_CreateObject();
	if (pcr_result == NULL) {
		cJSON_Delete(json_root);
		return NULL;
	}

	cJSON_AddStringToObject(pcr_result, "file_dir", pCfg->mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].file_dir);
	cJSON_AddStringToObject(pcr_result, "prefix", pCfg->mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].prefix);
	cJSON_AddNumberToObject(pcr_result, "clean_time", pCfg->mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].clean_time);
    cJSON_AddNumberToObject(pcr_result, "clean_threshold", pCfg->mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].clean_threshold);
	cJSON_AddItemToObject(json_root, "pcr_result", pcr_result);

    cJSON* disk = cJSON_CreateObject();
	if (disk == NULL) {
		cJSON_Delete(json_root);
		return NULL;
	}

	cJSON_AddNumberToObject(disk, "used_rate_threshold", pCfg->disk_used_rate_threshold);
	cJSON_AddItemToObject(json_root, "disk", disk);

	return json_root;
}

int SaveFileManagerFile(FileManager *pCfg) {
	char bak_file[MAX_FILE_NAME_LEN] = {0};
	char cmd_str[MAX_FILE_NAME_LEN] = {0};	
	FILE *fp = NULL;
	int count = 0;
	
	if (pCfg == NULL) {
        return -1;
    }

	cJSON* json = CreateFileMgrCfgJson(pCfg);
	if (json == NULL) {
		return -1;
	}
	char* json_str = cJSON_Print(json);
	if (json_str == NULL) {
		cJSON_Delete(json);
		return -1;
	}
	int len = strlen(json_str);

	sprintf(bak_file, "%s/local_config/file_mgr_cfg_bk.json", CONFIG_DIR_PATH);
	if (!access(FILE_MGR_CFG_FILE, W_OK)) {
		sprintf(cmd_str, "cp -f %s %s", FILE_MGR_CFG_FILE, bak_file);
		if (system_call(cmd_str) == 0) {
			debuge_printf("System exec success : %s\r\n", cmd_str);
		} else {
			err_printf("System exec failed, %s\r\n", cmd_str);
			return -1;	
		}
	}

	fp = fopen(FILE_MGR_CFG_FILE,"w+");
	if(fp == NULL) {		
		err_printf("Open %s failed.\r\n", FILE_MGR_CFG_FILE);
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

void ClearHistoricalFiles() {
    int i = 0;
    int ret = 0;
    for (i = 0; i < s_file_mgr.type_num; i++) {
        char cmd[200] = {0};
        long dir_size = GetFileSize(s_file_mgr.mgr_cfg[i].file_dir);
		if (dir_size > (s_file_mgr.mgr_cfg[i].clean_threshold*1024)) {
            if (s_file_mgr.mgr_cfg[i].file_type == MGR_FILE_TYPE_PCR_RESULT) {
                snprintf(cmd, sizeof(cmd), "find %s -name '*%s*' -mtime +%d |xargs rm -rf", 
				    s_file_mgr.mgr_cfg[i].file_dir, s_file_mgr.mgr_cfg[i].prefix, s_file_mgr.mgr_cfg[i].clean_time);
            } else {
                snprintf(cmd, sizeof(cmd), "find %s -name '%s*_bk' -mtime +%d |xargs rm -rf", 
				    s_file_mgr.mgr_cfg[i].file_dir, s_file_mgr.mgr_cfg[i].prefix, s_file_mgr.mgr_cfg[i].clean_time);
            }	        
		    ret = system_call(cmd);
		    warn_printf("Remove %s dir file, excute cmd : %s, ret = %d\r\n", s_file_mgr.mgr_cfg[i].file_dir, cmd, ret);
		}
    }
}

int CheckDiskUseRate() {
    int i = 0;
    int ret = 0;
    if (GetDiskUnuseRate("/") < (1-s_file_mgr.disk_used_rate_threshold)) {
        char cmd[200] = {0};
        for (i = 0; i < s_file_mgr.type_num; i++) {
            snprintf(cmd, sizeof(cmd), "ls -lt %s*%s*|tail -n 1|awk '{print $NF}'|xargs rm -rf", 
                s_file_mgr.mgr_cfg[i].file_dir, s_file_mgr.mgr_cfg[i].prefix);
            ret = system_call(cmd);
            warn_printf("Remove %s dir files, excute cmd : %s, ret = %d\r\n", s_file_mgr.mgr_cfg[i].file_dir, cmd, ret);
        }
        return 1;
    }

    return 0;
}

void FileMgrTimerHandler() {
    static int time_cnt = 0; //counting per 1s
    static int warn_flag = 0;
    
    time_cnt++;
    //check disk use rate per 1s when mem warning has occured
    if (warn_flag == 1) {
        if (CheckDiskUseRate() == 1) {
            warn_flag = 1;
        } else {
            warn_flag = 0;
        }
    }
    if (time_cnt >= 60) {
        ClearHistoricalFiles();

        //check disk use rate per 1min
        if (warn_flag == 0) {
            if (CheckDiskUseRate() == 1) {
                warn_flag = 1;
            } else {
                warn_flag = 0;
            }
        }
        time_cnt = 0;
    }
}

int InitFileManager() {    
    int mgr_file_type_num = 0;

    mgr_file_type_num = LoadFileManagerConfig();
    if (mgr_file_type_num < MAX_MGR_FILE_TYPE_NUM) {
        err_printf("Load file mgr cfg file err, cfg file_num:%d, use default cfg.\n", mgr_file_type_num);
    }

    s_file_mgr_timer_info = ResetMillisecondsTimer(1000, FileMgrTimerHandler, NULL, &s_file_mgr_thread_id);
    return 0;
}

int DeinitFileManager() {
    if (s_file_mgr_thread_id && s_file_mgr_timer_info) {
        s_file_mgr_timer_info->quit_flag = 1;
        pthread_join(s_file_mgr_thread_id, NULL);
    }
    return 0;
}
