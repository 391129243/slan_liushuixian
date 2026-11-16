#include "UpgradeMsgHandler.h"
#include "Logger.h"
#include "WssProtocol.h"
#include "ConfLocal.h"
#include "System.h"
#include "Common.h"

static StartParam s_start_param = {0};

int SendUpgradeStatus(unsigned short seq_id, short status) {
    int ret = 0;
    MsgInfo msg_info = {0};
    msg_info.header.src_app = APP_UPGRADE_ID;
    msg_info.header.handle_id = seq_id;
    msg_info.header.result = status;
    msg_info.header.dst_app = APP_PCRMGR_ID;

    ret = SendMsg(APP_PCRMGR_ID, MSG_ID_APP_UPGRADE_STATUS, &msg_info);
    if (ret == 0) {
        warn_printf("Send MSG_ID_APP_UPGRADE_STATUS to PcrMgrApp successfully, status:%d.\n", status);
    } else {
        err_printf("Send MSG_ID_APP_UPGRADE_STATUS to PcrMgrApp failed, status:%d.\n", status);
    }

    return ret;
}

int UpdateVersionFile(UpgradeDescInfo* desc_info) {
    char cmd_str[256] = {0};
    char tmp_file[256] = {0};
    char version_file[256] = {0};
    int ret = 0;

    if (desc_info == NULL) {
        err_printf("Input err, UpgradeDescInfo ptr is NULL.\n");
        return -1;
    }

    snprintf(version_file, sizeof(version_file), "%s%s", CONFIG_DIR_PATH, desc_info->file_name);
    snprintf(cmd_str, sizeof(cmd_str), "tar -zxvf %s", version_file);
	if (system_call(cmd_str) != 0) {
		err_printf("Unzip version file failed. Call sys_cmd:%s failed.\n", cmd_str);
        RemoveFile(version_file);
		return UPGRADE_STATUS_VERSION_FILE_UNZIP_FAILED;
	} else {
		warn_printf("Unzip version file success. Call sys_cmd:%s success.\n", cmd_str);
        snprintf(tmp_file, sizeof(tmp_file), "%sapp", CONFIG_DIR_PATH);
        RemoveFile(tmp_file); //check if version file can unzip successfully
	}

    snprintf(cmd_str, sizeof(cmd_str), "rm %s/*tar*; mv %s/*tar* %s/;", PCR_APP_BAK_PATH, PCR_APP_RUN_PATH, PCR_APP_BAK_PATH);
	if (system_call(cmd_str) != 0) {
		err_printf("Backup pcr_app old version file failed. Call sys_cmd:%s failed.\n", cmd_str);
        RemoveFile(version_file);
		return UPGRADE_STATUS_VERSION_FILE_BACKUP_FAILED;
	} else {
		warn_printf("Backup pcr_app old version file success. Call sys_cmd:%s success.\n", cmd_str);
	}

    snprintf(cmd_str, sizeof(cmd_str), "mv %s %s", version_file, PCR_APP_RUN_PATH);
    ret = system_call(cmd_str);
    if (ret != 0) {
        err_printf("Copy pcr_app version file:%s to file path:%s failed.\n", version_file, PCR_APP_RUN_PATH);
        RemoveFile(version_file);
        return UPGRADE_STATUS_PCR_VERSION_UPDATE_FAILED;
    } else {
        warn_printf("Copy pcr_app version file:%s to file path:%s success.\n", version_file, PCR_APP_RUN_PATH);
    }

    return 0;
}

int RollBackVersionFile(int fail_code) {
    char cmd_str[256] = {0};
    int ret = 0;

    if (fail_code == UPGRADE_STATUS_RNA_VERSION_UPDATE_FAILED) {
        snprintf(cmd_str, sizeof(cmd_str), "rm %s/*.tar.gz; cp %s/*.tar.gz %s", RNA_APP_RUN_PATH, RNA_APP_BAK_PATH, RNA_APP_RUN_PATH);
        ret = system_call(cmd_str);
    } else if (fail_code == UPGRADE_STATUS_PCR_VERSION_UPDATE_FAILED) {
        snprintf(cmd_str, sizeof(cmd_str), "rm %s/*.tar.gz; cp %s/*.tar.gz %s", PCR_APP_RUN_PATH, PCR_APP_BAK_PATH, PCR_APP_RUN_PATH);
        ret = system_call(cmd_str);
    }
    
	if (ret != 0) {
		err_printf("Rollback version failed. Call sys_cmd:%s failed.\n", cmd_str);
	} else {
		warn_printf("Rollback version success. Call sys_cmd:%s success.\n", cmd_str);
	}

    return ret;
}

int HandleAppUpgradeMsg(MsgInfo* msg_info) {
    UpgradeDescInfo desc_info = {0};
    int ret = 0;
    
    warn_printf("Recv app upgrade msg.\n");
    if (msg_info == NULL || msg_info->data == NULL) {
        err_printf("Input err, msg_info or msg data ptr is NULL.\n");
        return -1;
    }
    memcpy(&desc_info, msg_info->data, msg_info->header.data_len);

    /*if (desc_info.version == SOFTWARE_VERSION) {
        warn_printf("Version is same, no need to upgrade, new version:%d, old version:%d.\n", desc_info.version, SOFTWARE_VERSION);
        SendUpgradeStatus(msg_info->header.handle_id, UPGRADE_STATUS_SAME_VERSION);
        return 0;
    }*/
    warn_printf("New version:%s, old version:%s, start update processing...\n", desc_info.version, SOFTWARE_VERSION);

    ret = UpdateVersionFile(&desc_info);
    if (ret == 0) {
        warn_printf("Update version file success.\n");
    } else {
        err_printf("Update version file failed.\n");
        goto FAIL;
    }

    warn_printf("App upgrade success, new version:%s.\n", desc_info.version);
    SendUpgradeStatus(msg_info->header.handle_id, UPGRADE_STATUS_SUCCESS);
    return 0;

FAIL:
    err_printf("App upgrade failed, start rollback...\n");
    RollBackVersionFile(ret);
    SendUpgradeStatus(msg_info->header.handle_id, ret);
    return -1;
}

int HandleQueryStartParamMsg(MsgInfo* msg_info) {
    char cmd_str[256] = {0};

    if (msg_info == NULL || msg_info->data == NULL) {
        err_printf("Input err, MsgInfo or msg data ptr is NULL.\n");
        return -1;
    }

    memcpy(&s_start_param, msg_info->data, sizeof(StartParam));
    
    if (strcmp(s_start_param.app_run, PCR_APP_RUN_PATH) != 0) {
        err_printf("App start path is %s, app rollback to old version:%s.\n", s_start_param.app_run, SOFTWARE_VERSION);
        snprintf(cmd_str, sizeof(cmd_str), "rm %s/*.tar; cp %s/*.tar %s", PCR_APP_RUN_PATH, s_start_param.app_run, PCR_APP_RUN_PATH);
        if (system_call(cmd_str) != 0) {
            err_printf("Rollback old version:%s file failed. Call sys_cmd:%s failed.\n", SOFTWARE_VERSION, cmd_str);
            return -1;
        } else {
            warn_printf("Rollback old version:%s file success. Call sys_cmd:%s success.\n", SOFTWARE_VERSION, cmd_str);
        }
    }
    return 0;
}
