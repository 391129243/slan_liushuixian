#include "MsgWrapper.h"
#include "WssProtocol.h"
#include "UartProtocol.h"
#include "Uart.h"
#include "Wss.h"
#include "Logger.h"
#include "Timer.h"
#include "CmdHandler.h"

int HandleReportLogFileMsg(MsgInfo* msg_info) {
    u_int8_t* json_str = NULL;
    int ret = 0;
    unsigned long data_len = 0;
    char file_name[MAX_FILE_NAME_LEN] = {0};

    warn_printf("====recv send log msg===.\n");

    if (msg_info == NULL || msg_info->data == NULL) {
        err_printf("Input err, msg data is NULL.\n");
        return -1;
    }

    json_str = PackPCRLogReport(msg_info->data, &data_len);
    if (json_str == NULL) {
        err_printf("Report log file:%s failed, json pack err.\n", msg_info->data);
        return -1;
    }

    ret = WssSendData(json_str, data_len, WEBSOCKET_OP_BINARY);
    if (ret != 0) {
        err_printf("Report log file:%s failed, wss send err.\n", msg_info->data);
    } else {
        warn_printf("====Report log file:%s success.\n", msg_info->data);
        snprintf(file_name, sizeof(file_name), "%s_bk", msg_info->data);
        ret = RenameFile(msg_info->data, file_name);
    }

    free(json_str);
    return ret;
}

int ReportAutoStartLogFile() {
    u_int8_t* json_str = NULL;
    int ret = 0;
    char file_name[MAX_FILE_NAME_LEN] = {0};
    time_t timep;
	struct tm *p = NULL;
	char time_str[128] = {0};
    unsigned long data_len = 0;

	timep = time(NULL);   
	p = localtime(&timep);
	sprintf(time_str, "%04d_%02d_%02d_%02d_%02d_%02d", 
		(1900 + p->tm_year), (p->tm_mon + 1), p->tm_mday, p->tm_hour, p->tm_min, p->tm_sec);
    snprintf(file_name, sizeof(file_name), "/logs/auto_start_%s.log", time_str);
    RenameFile("/logs/auto_start.log", file_name);

    json_str = PackPCRLogReport(file_name, &data_len);
    if (json_str == NULL) {
        err_printf("Report auto start log file failed, json pack err.\n");
        return -1;
    }

    ret = WssSendData(json_str, data_len, WEBSOCKET_OP_BINARY);
    if (ret != 0) {
        err_printf("Report auto start log file failed, wss send err.\n");
    } else {
        warn_printf("====Report auto start log file success.\n");
        RemoveFile(file_name);
    }
    
    free(json_str);
    return ret;
}

int HandleUartDisConnectMsg(MsgInfo* msg_info) {
    PCRStatus status_report = {0};
    PCRRunningStatus running_status = {0};
    PcrSlaveCmd cmd = {0};
    int ret = 0;

    snprintf(g_status.linux_status, sizeof(g_status.linux_status), "error");

    ret = GetCmdQueueFront(&cmd);

    GetTemplateName(status_report.template_name);
    snprintf(status_report.status, sizeof(status_report.status), "%s", g_status.status);
    snprintf(status_report.alarm, sizeof(status_report.alarm), "%s", g_status.alarm);
    snprintf(status_report.linux_status, sizeof(status_report.linux_status), "%s", g_status.linux_status);
    snprintf(status_report.linux_alarm, sizeof(status_report.linux_alarm), "uart disconnect");
    status_report.total_time = GetSectionsTotalTime();  
    if (status_report.total_time > 0) {
        status_report.running_time = GetTaskUsedTime();
        if (status_report.total_time > status_report.running_time) {
            status_report.left_time = status_report.total_time - status_report.running_time;
        } else {
            status_report.left_time = 0;
        }        
    } else {
        status_report.running_time = 0;
        status_report.left_time = 0;
    }  
    status_report.degree = GetCurrentTemp();

    if (ret == 0) {
        status_report.cur_cycle = cmd.current_cycle;
        status_report.total_cycle = cmd.total_cycle;
        status_report.cur_section = cmd.current_section;
        status_report.cur_step = cmd.current_step;
        status_report.waiting_time = cmd.time;
        status_report.target_degree = cmd.pcr_temp; 
        snprintf(status_report.info, sizeof(status_report.info), "%s", cmd.desc);
    }

    if (GetSectionQueueSize() > 0) {
        snprintf(running_status.template_name, sizeof(running_status.template_name), "%s", status_report.template_name);
        snprintf(running_status.status, sizeof(running_status.status), "%s", g_status.status);
        snprintf(running_status.alarm, sizeof(running_status.alarm), "%s", g_status.alarm);
        snprintf(running_status.linux_status, sizeof(running_status.linux_status), "%s", g_status.linux_status);
        snprintf(running_status.linux_alarm, sizeof(running_status.linux_alarm), "uart disconnect");
        running_status.total_time = status_report.total_time;
        running_status.running_time = status_report.running_time;
        running_status.left_time = status_report.left_time;
        running_status.cur_cycle = cmd.current_cycle;
        running_status.total_cycle = cmd.total_cycle;
        running_status.cur_section = cmd.current_section;
        running_status.cur_step = cmd.current_step;
        running_status.degree = status_report.degree;
        running_status.target_degree = cmd.pcr_temp;
        running_status.waiting_time = cmd.time; 
        snprintf(running_status.info, sizeof(running_status.info), "%s", cmd.desc);
        PCRRunningStatusReport(&running_status);
    } else {
        PCRStatusReport(&status_report);
    }
    
    return 0;
}

int HandleUartConnectedMsg(MsgInfo* msg_info) {
    PCRStatus status_report = {0};
    PCRRunningStatus running_status = {0};
    PcrSlaveCmd cmd = {0};
    int ret = 0;

    ret = GetCmdQueueFront(&cmd);

    GetTemplateName(status_report.template_name);
    snprintf(status_report.status, sizeof(status_report.status), "%s", g_status.status);
    snprintf(status_report.alarm, sizeof(status_report.alarm), "%s", g_status.alarm);
    snprintf(status_report.linux_alarm, sizeof(status_report.linux_alarm), "");
    status_report.total_time = GetSectionsTotalTime();  
    if (status_report.total_time > 0) {
        status_report.running_time = GetTaskUsedTime();
        if (status_report.total_time > status_report.running_time) {
            status_report.left_time = status_report.total_time - status_report.running_time;
        } else {
            status_report.left_time = 0;
        }        
    } else {
        status_report.running_time = 0;
        status_report.left_time = 0;
    }  
    status_report.degree = GetCurrentTemp();

    if (ret == 0) {
        status_report.cur_cycle = cmd.current_cycle;
        status_report.total_cycle = cmd.total_cycle;
        status_report.cur_section = cmd.current_section;
        status_report.cur_step = cmd.current_step;
        status_report.waiting_time = cmd.time;
        status_report.target_degree = cmd.pcr_temp; 
        snprintf(status_report.info, sizeof(status_report.info), "%s", cmd.desc);
    }

    if (GetSectionQueueSize() > 0) {
        snprintf(g_status.linux_status, sizeof(g_status.linux_status), "running");

        snprintf(running_status.template_name, sizeof(running_status.template_name), "%s", status_report.template_name);
        snprintf(running_status.status, sizeof(running_status.status), "%s", g_status.status);
        snprintf(running_status.alarm, sizeof(running_status.alarm), "%s", g_status.alarm);
        snprintf(running_status.linux_status, sizeof(running_status.linux_status), "%s", g_status.linux_status);
        snprintf(running_status.linux_alarm, sizeof(running_status.linux_alarm), "");
        running_status.total_time = status_report.total_time;
        running_status.running_time = status_report.running_time;
        running_status.left_time = status_report.left_time;
        running_status.cur_cycle = cmd.current_cycle;
        running_status.total_cycle = cmd.total_cycle;
        running_status.cur_section = cmd.current_section;
        running_status.cur_step = cmd.current_step;
        running_status.degree = status_report.degree;
        running_status.target_degree = cmd.pcr_temp;
        running_status.waiting_time = cmd.time; 
        snprintf(running_status.info, sizeof(running_status.info), "%s", cmd.desc);
        PCRRunningStatusReport(&running_status);
    } else {
        snprintf(g_status.linux_status, sizeof(g_status.linux_status), "free");
        snprintf(status_report.linux_status, sizeof(status_report.linux_status), "%s", g_status.linux_status);

        PCRStatusReport(&status_report);
    }
    
    return 0;
}

int HandleUpgradeStatusMsg(MsgInfo* msg_info) {
    UpgradeDoneStatus status = {0};

    if (msg_info == NULL) {
        err_printf("Input err, msg_info ptr is NULL.\n");
        return -1;
    }

    switch (msg_info->header.result)
    {
    case UPGRADE_STATUS_SAME_VERSION:
        snprintf(status.status, sizeof(status.status), "ok");
        snprintf(status.alarm, sizeof(status.alarm), "Version is same as old version");
        break;

    case UPGRADE_STATUS_SUCCESS:
        snprintf(status.status, sizeof(status.status), "ok");
        break;

    case UPGRADE_STATUS_VERSION_FILE_UNZIP_FAILED:
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Version file unzip err");
        break;

    case UPGRADE_STATUS_VERSION_FILE_BACKUP_FAILED:
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Version file backup err");
        break;

    case UPGRADE_STATUS_RNA_VERSION_UPDATE_FAILED:
    case UPGRADE_STATUS_PCR_VERSION_UPDATE_FAILED:
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Version file update err");
        break;

    default:
        break;
    }

    UpgradeDoneStatusReport(msg_info->header.handle_id, &status);

    if (msg_info->header.result == UPGRADE_STATUS_SUCCESS) {
        warn_printf("====Update success, system will reboot after 5s====\n");
	    sleep(5);
	    system("sync;reboot");
    }

    return 0;
}
