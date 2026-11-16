#include <sys/time.h>
#include <pthread.h>

#include "Logger.h"
#include "WssProtocol.h"
#include "MsgWrapper.h"
#include "UartProtocol.h"
#include "Wss.h"
#include "WorkflowMgrWrapper.h"
#include "AsyncCond.h"
#include "ConfLocal.h"
#include "Timer.h"
#include "Uart.h"
#include "CmdHandler.h"
#include "FileManager.h"

//static unsigned short ResetSystermTimeCnt = 720;
static u_int8_t s_time_syn_flag = 0;

static bool s_task_exist = false;
static bool s_query_exist = false;
static bool s_hold_time_exist = false;
static double s_hold_time_waiting_time = 0.0;
static u_int8_t s_slave_cmd_status = 0;
static AsyncCond s_cond = NULL;
static pthread_t s_temp_query_thread_id = 0;
static PTimerMsgInfo s_temp_query_timer_info = NULL;
static pthread_t s_time_hold_thread_id = 0;
static PTimerMsgInfo s_time_hold_timer_info = NULL;
static pthread_t s_report_result_thread_id = 0;
static PTimerMsgInfo s_report_result_timer_info = NULL;
static pthread_t s_report_pcr_finish_status_thread_id = 0;
static PTimerMsgInfo s_report_pcr_finish_status_timer_info = NULL;
static PCRRunningStatus s_running_status = {0};
static PCRDoneStatus s_done_status = {0};
static int s_pcr_finish_status_ack = 0;
PCRStatus g_status = {0};
static int8_t s_uart_status = -1;
static pthread_mutex_t s_lock;

void InitLock() {
    pthread_mutex_init(&s_lock, NULL);
}

void DestroyLock() {
    pthread_mutex_destroy(&s_lock);
}

int SendHelloToSlave() {
    int ret = 0;
    char* json_str = NULL;
    u_int8_t data_len = 0;

    json_str = PackSlaveCmdHello(&data_len);
    if (json_str == NULL) {
        err_printf("Slave hello json packe err.\n");
        return -1;
    }
    ret = UartSendSync(json_str, data_len);
    if (ret != 0) {
        err_printf("Send hello to slave failed.\n");
    }

    free(json_str);
    return ret;
}

void PCRStatusRegularReport(void* arg) {
    if (s_uart_status == -1) {
        snprintf(g_status.status, sizeof(g_status.status), "error");
        snprintf(g_status.alarm, sizeof(g_status.alarm), "slave uart err");
        snprintf(g_status.linux_status, sizeof(g_status.linux_status), "error");
        snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "slave uart err");
        PCRStatusReport(&g_status);

        snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "");
        snprintf(g_status.alarm, sizeof(g_status.alarm), "");
        SendHelloToSlave();
    } else {
        if (GetSectionQueueSize() == 0 && GetCmdQueueSize() == 0) {
            snprintf(g_status.linux_status, sizeof(g_status.linux_status), "free");
            snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "");
            SendHelloToSlave();
        } else {
            PcrSlaveCmd cmd = {0};
            int ret = 0;

            g_status.total_time = GetSectionsTotalTime();  
            if (g_status.total_time > 0) {
                g_status.running_time = GetTaskUsedTime();
                if (g_status.total_time > g_status.running_time) {
                    g_status.left_time = g_status.total_time - g_status.running_time;
                } else {
                    g_status.left_time = 0;
                }        
            } else {
                g_status.running_time = 0;
                g_status.left_time = 0;
            }
            GetTemplateName(g_status.template_name);
            g_status.degree = GetCurrentTemp();

            ret = GetCmdQueueFront(&cmd);
            if (ret == 0) {         
                g_status.cur_cycle = cmd.current_cycle;
                g_status.total_cycle = cmd.total_cycle;
                g_status.cur_section = cmd.current_section;
                g_status.cur_step = cmd.current_step;
                g_status.waiting_time = cmd.time;
                g_status.target_degree = cmd.pcr_temp;            
                snprintf(g_status.info, sizeof(g_status.info), "%s", cmd.desc);
            }

            snprintf(g_status.status, sizeof(g_status.status), "running");
            snprintf(g_status.linux_status, sizeof(g_status.linux_status), "running");
            PCRStatusReport(&g_status);
            snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "");
            snprintf(g_status.alarm, sizeof(g_status.alarm), "");
        }
    }        
}

void StartStatusRegularReport() {
    SetMillisecondsTimer(HEARTBEAT_INTERVAL, PCRStatusRegularReport, &g_status);
}

int SlaveCmdHelloRspHandler(u_int8_t* data) {
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    PcrSlaveCmd cmd = {0};
    int ret = 0;
    u_int8_t status = data[0];

    g_status.total_time = GetSectionsTotalTime();  
    if (g_status.total_time > 0) {
        g_status.running_time = GetTaskUsedTime();
        if (g_status.total_time > g_status.running_time) {
            g_status.left_time = g_status.total_time - g_status.running_time;
        } else {
            g_status.left_time = 0;
        }        
    } else {
        g_status.running_time = 0;
        g_status.left_time = 0;
    }
    GetTemplateName(g_status.template_name);
    g_status.degree = GetCurrentTemp();

    if (status == 0) {
        s_uart_status = 1;
        ret = GetCmdQueueFront(&cmd);
        if (ret == 0) {  
            snprintf(g_status.status, sizeof(g_status.status), "running");
            g_status.cur_cycle = cmd.current_cycle;
            g_status.total_cycle = cmd.total_cycle;
            g_status.cur_section = cmd.current_section;
            g_status.cur_step = cmd.current_step;
            g_status.waiting_time = cmd.time;
            g_status.target_degree = cmd.pcr_temp;            
            snprintf(g_status.info, sizeof(g_status.info), "%s", cmd.desc);
        } else {
            snprintf(g_status.status, sizeof(g_status.status), "free");
        }   
    } else {
        s_uart_status = 0;
        snprintf(g_status.status, sizeof(g_status.status), "error");
        snprintf(g_status.alarm, sizeof(g_status.alarm), "slave hello cmd rsp status:%x", status);
    }

    PCRStatusReport(&g_status);
    snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "");

    return 0;
}

void HoldTime(void* arg) {
    static u_int32_t hold_time_cnt = 0;

    if (arg == NULL) {
        err_printf("Hold time timed task input arg ptr is NULL.\n");
        return;
    }
    hold_time_cnt++;

    PCRRunningStatus* running_status = (PCRRunningStatus*)arg;
    if (s_hold_time_waiting_time == 0.0) {
        s_hold_time_waiting_time = running_status->waiting_time;
    }
    warn_printf("Hold time pthread entry, waiting_time:%f.\n", s_hold_time_waiting_time);
    s_hold_time_waiting_time -= 0.2;

    if (s_hold_time_waiting_time <= 0) {
        running_status->remain_time = 0;
        s_hold_time_waiting_time = 0.0;

        pthread_mutex_lock(&s_lock);
        if (s_time_hold_timer_info != NULL) {
            s_time_hold_timer_info->quit_flag = 1;
            pthread_join(s_time_hold_thread_id, NULL);
            s_time_hold_thread_id = 0;
            s_time_hold_timer_info = NULL;
        }
        
        if (s_temp_query_timer_info != NULL) {
            s_temp_query_timer_info->quit_flag = 1;
            pthread_join(s_temp_query_thread_id, NULL);
            s_temp_query_thread_id = 0;
            s_temp_query_timer_info = NULL;
        }
        
        s_query_exist = 1;
        s_hold_time_exist = 1;
        pthread_mutex_unlock(&s_lock);
        warn_printf("Hold time pthread exit.\n");
    } else {
        int actual_time = s_hold_time_waiting_time - 3;
        if (actual_time <= 0) {
            actual_time = 0;
        }
        running_status->remain_time = actual_time;
    }

    if (!(hold_time_cnt % 10)) {
        PCRRunningStatusReport(running_status);
    }  
}

void QueryTemperature() {
    char* json_str = NULL;
    int ret = 0;
    u_int8_t data_len = 0;

    if (s_query_exist) {
        return;
    }

    json_str = PackSlaveCmdGetPcrTemp(&data_len);
    if (json_str == NULL) {
        err_printf("Json pack slave ctrl cmd err err, sleep 300ms and retry.\n");
        return;
    }

    ret = UartSendSync(json_str, data_len);
    if (ret != 0) {
        err_printf("Uart send slave ctrl cmd err, cmd:%s, sleep 300ms and retry.\n", PCR_SLAVE_CMD_GET_CURRENT_TEMP);
     } else {
        free(json_str);
        json_str = NULL;
    }
}

int SlaveCmdSetTempRspHandler(u_int8_t* data) {
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    u_int8_t status = data[0];
    if (status == 0) {
        SetPcrTempWaitFlag(1);
        s_slave_cmd_status = 1;
        if (s_temp_query_timer_info == NULL) {
            pthread_mutex_lock(&s_lock);
            s_query_exist = 0;
            pthread_mutex_unlock(&s_lock);
            s_temp_query_timer_info = ResetMillisecondsTimer(TEMP_QUERY_TIME*100, QueryTemperature, NULL, &s_temp_query_thread_id);
        }
    } else {
        s_slave_cmd_status = 0;
    }
    
    if (s_cond) {
        SignalAsyncCond(s_cond, NULL, NULL);
    }

    return 0;
}

int SlaveCmdMotorCtrlRspHandler(u_int8_t* data) {
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    u_int8_t status = data[0];
    PcrSlaveCmd cmd = {0};
    int ret = 0;

    ret = GetCmdQueueFront(&cmd);
    if (ret != 0) {
        err_printf("Get current cmd failed.\n");
        return -1;
    }

    if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_MOTOR_RESET) == 0) {
        if (status == 0) {
            s_slave_cmd_status = 1;
            warn_printf("Motor reset OK.\n");
        } else {
            s_slave_cmd_status = 0;
            warn_printf("Motor reset error.\n");
            sleep(1);
        }
        usleep(300000);
    } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_MOTOR_SCAN) == 0) {
        if (status == 0) {
            s_slave_cmd_status = 1;
        } else {
            s_slave_cmd_status = 0;
        }
    }

    if (s_cond) {
        SignalAsyncCond(s_cond, NULL, NULL);
    }

    return 0;
}

//cmd abanton
int SlaveCmdGetTempRspHandler(u_int8_t* data) {
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    u_int16_t temp = 0;
    u_int8_t is_reach = 0;
    u_int8_t status = 0;

    memcpy(&temp, data, 2);
    temp = temp / 100;
    is_reach = data[2];
    status = data[3];

    if (status == 0) {
        //todo: do nothing
    } else {
        //todo: retry cmd
    }

    return 0;
}

//cmd abanton
int SlaveCmdSetCapRspHandler(u_int8_t* data) {
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    u_int8_t status = data[0];
    if (status == 0) {
        //todo: do nothing
    } else {
        //todo: retry cmd
    }

    return 0;
}

void PCRExperimentResultReport(u_int8_t write_flag) {
    cJSON* root_json = NULL;
    char* experiment_json_str = NULL;
    int ret = 0;
    char tempalte_name[MAX_TEMPLATE_NAME_LEN] = {0};

    GetTemplateName(tempalte_name);

    root_json = GetExperimentJson();
    if (root_json == NULL) {
        err_printf("Get experiment json err.\n");
        return;
    }

    experiment_json_str = PackPCRResultReport(root_json, tempalte_name);
    if (experiment_json_str == NULL) {
        err_printf("Result report json pack err.\n");
        return;
    }

    if (write_flag == 1) {
        FILE* fp;
        char result_file_name[MAX_FILE_NAME_LEN] = {0};
        time_t timep;
        struct tm *p = NULL;
        char time_str[128] = {0};

        timep = time(NULL);   
        p = localtime(&timep);
        sprintf(time_str, "%04d_%02d_%02d_%02d_%02d_%02d", 
            (1900 + p->tm_year), (p->tm_mon + 1), p->tm_mday, p->tm_hour, p->tm_min, p->tm_sec);

        snprintf(result_file_name, sizeof(result_file_name), "%s/%s_pcr_result_%s.json", PCR_RESULT_PATH, tempalte_name, time_str);
        fp = fopen(result_file_name, "wb");
        if (fp == NULL) {
            err_printf("Write template:%s pcr resutl file:%s failed.\n", tempalte_name, result_file_name);
            free(experiment_json_str);
            return;
        } else {
            fwrite(experiment_json_str, 1, strlen(experiment_json_str), fp);
            fclose(fp);
            warn_printf("Write template:%s pcr resutl file:%s success.\n", tempalte_name, result_file_name);
        }
    }

    ret = WssSendData(experiment_json_str, strlen(experiment_json_str), WEBSOCKET_OP_TEXT);
    if (ret < 0) {
		err_printf("Result report failed, WSS send failed, ret:%d, data_len:%d.\n", ret, strlen(experiment_json_str));
    } else {
        warn_printf("Result report success, data_len:%d.\n",  strlen(experiment_json_str));
    }

    free(experiment_json_str);
}

int SlaveCmdGetPcrDataRspHandler(u_int8_t* data) {
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    PCRRunningStatus running_status = {0};
    u_int8_t i = 0;
    int ret = 0;
    PcrSlaveCmd cmd = {0};
    PcrStatus pcr_status = {0};
    u_int8_t status = data[128];
    float fluorescence_FAM = 0.0f;
    float fluorescence_CY5 = 0.0f;
    float fluorescence_HEX = 0.0f;
    float fluorescence_ROX = 0.0f;

    if (s_report_result_thread_id == 0 && s_report_result_timer_info == NULL) {
        warn_printf("====create report result timer.\n");
        s_report_result_timer_info = ResetMillisecondsTimer(RESULT_REPORT_TIME*1000, PCRExperimentResultReport, NULL, &s_report_result_thread_id);
    } else {
        warn_printf("====report result time exist, thread_id:%d.\n", s_report_result_thread_id);
    }
    
    ret = GetCmdQueueFront(&cmd);
    if (ret != 0) {
        err_printf("Get current cmd failed.\n");
        return -1;
    }

    if (status == 0) {
        s_slave_cmd_status = 1;

        for (i = 0; i < PCR_CHANNEL_NUM; i++) {
            fluorescence_FAM = (float)((data[i*2] & 0xFF) + (data[i*2+1] & 0xFF) * 256);
            pcr_status.tupe_data[i].fluorescence_FAM = fluorescence_FAM;
            fluorescence_CY5 = (float)((data[i*2+32] & 0xFF) + (data[i*2+1+32] & 0xFF) * 256);
            pcr_status.tupe_data[i].fluorescence_CY5 = fluorescence_CY5;
            fluorescence_HEX = (float)((data[i*2+64] & 0xFF) + (data[i*2+1+64] & 0xFF) * 256);
            pcr_status.tupe_data[i].fluorescence_HEX = fluorescence_HEX;
            fluorescence_ROX = (float)((data[i*2+96] & 0xFF) + (data[i*2+1+96] & 0xFF) * 256);
            pcr_status.tupe_data[i].fluorescence_ROX = fluorescence_ROX;
        }

        if (strstr(cmd.cmd_name, PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP)) {
            if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP_START) == 0) {
                ClearTupe();
                SumTupe(&pcr_status.tupe_data[0]);
            } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP) == 0) {
                SumTupe(&pcr_status.tupe_data[0]);
            } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP_END) == 0) {
                TupeData tupe_mean[PCR_CHANNEL_NUM] = {0};
                SumTupe(&pcr_status.tupe_data[0]);
                GetMeanTupe(&tupe_mean[0]);
                AddRawCurveDot(&tupe_mean[0]);
                ClearTupe();
            }
            
        } else if (strstr(cmd.cmd_name, PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_MELT)) {
            AddRawMeltCurveDot(&pcr_status.tupe_data[0]);
        }

        running_status.cur_cycle = cmd.current_cycle;
        running_status.total_cycle = cmd.total_cycle;
        running_status.cur_section = cmd.current_section;
        running_status.cur_step = cmd.current_step;
        running_status.degree = GetCurrentTemp();
        running_status.target_degree = cmd.pcr_temp;
        snprintf(running_status.status, sizeof(running_status.status), "%s", g_status.status);
        snprintf(running_status.alarm, sizeof(running_status.alarm), "%s", g_status.alarm);
        snprintf(running_status.linux_status, sizeof(running_status.linux_status), "running");
        GetTemplateName(running_status.template_name);
        running_status.total_time = GetSectionsTotalTime();
        running_status.waiting_time = cmd.time;
        snprintf(running_status.info, sizeof(running_status.info), "%s", cmd.desc);
        PCRRunningStatusReport(&running_status);
    } else {
        s_slave_cmd_status = 0;
    }

    if (s_cond) {
        SignalAsyncCond(s_cond, NULL, NULL);
    }

    return 0;
}

int SlaveCmdSetLedRspHandler(u_int8_t* data) {
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    u_int8_t status = data[0];
    if (status == 0) {
        s_slave_cmd_status = 1;
    } else {
        s_slave_cmd_status = 0;
    }

    if (s_cond) {
        SignalAsyncCond(s_cond, NULL, NULL);
    }

    return 0;
}

int SlaveCmdGetPcrTempRspHandler(u_int8_t* data) {
    static u_int32_t temp_rsp_cnt = 0;
    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }
    temp_rsp_cnt++;

    PCRRunningStatus running_status = {0};
    PcrSlaveCmd cmd = {0};
    int ret = 0;
    PcrStatus pcr_status = {0};
    u_int8_t is_reach = data[12];
    u_int8_t status = data[13];
    float temp_1 = 0.0f;
    float temp_2 = 0.0f;
    float temp_3 = 0.0f;
    double temp_post[PCR_CHANNEL_NUM] = {0.0};

    if (status == 0) {
        temp_1 = ((data[0] & 0xFF) * 256 + (data[1] & 0xFF)) / 100.0;
        temp_2 = ((data[2] & 0xFF) * 256 + (data[3] & 0xFF)) / 100.0;
        temp_3 = ((data[4] & 0xFF) * 256 + (data[5] & 0xFF)) / 100.0;

        pcr_status.temp_1 = temp_1;
        pcr_status.temp_2 = temp_2;
        pcr_status.temp_3 = temp_3;

        pcr_status.tupe_temp_data[0].temp = temp_1;
        pcr_status.tupe_temp_data[1].temp = temp_1;
        pcr_status.tupe_temp_data[2].temp = temp_1;

        pcr_status.tupe_temp_data[3].temp = temp_2;
        pcr_status.tupe_temp_data[4].temp = temp_2;
        pcr_status.tupe_temp_data[5].temp = temp_2;

        pcr_status.tupe_temp_data[6].temp = temp_3;
        pcr_status.tupe_temp_data[7].temp = temp_3;

        pcr_status.tupe_data[0].temp_pre = temp_1;
        pcr_status.tupe_data[1].temp_pre = temp_1;
        pcr_status.tupe_data[2].temp_pre = temp_1;

        pcr_status.tupe_data[3].temp_pre = temp_2;
        pcr_status.tupe_data[4].temp_pre = temp_2;
        pcr_status.tupe_data[5].temp_pre = temp_2;

        pcr_status.tupe_data[6].temp_pre = temp_3;
        pcr_status.tupe_data[7].temp_pre = temp_3;

        pcr_status.tupe_data[0].temp_post = temp_1;
        pcr_status.tupe_data[1].temp_post = temp_1;
        pcr_status.tupe_data[2].temp_post = temp_1;

        pcr_status.tupe_data[3].temp_post = temp_2;
        pcr_status.tupe_data[4].temp_post = temp_2;
        pcr_status.tupe_data[5].temp_post = temp_2;

        pcr_status.tupe_data[6].temp_post = temp_3;
        pcr_status.tupe_data[7].temp_post = temp_3;

        temp_post[0] = temp_1;
        temp_post[1] = temp_1;
        temp_post[2] = temp_1;
        temp_post[3] = temp_1;
        temp_post[4] = temp_1;
        temp_post[5] = temp_1;
        temp_post[6] = temp_1;
        temp_post[7] = temp_1;

        pcr_status.is_pcr_temp_reach = is_reach;

        s_slave_cmd_status = 1;
    } else {
        err_printf("Get pcr temperature response status err.\n");
        s_slave_cmd_status = 0;
        if (s_cond) {
            SignalAsyncCond(s_cond, NULL, NULL);
        }
        return -1;
    }

    ret = GetCmdQueueFront(&cmd);
    if (ret != 0) {
        err_printf("Get current cmd failed.\n");
        if (s_cond) {
            SignalAsyncCond(s_cond, NULL, NULL);
        }
        return -1;
    }

    AddTemperatureDot(pcr_status.tupe_temp_data[0].temp);
    s_running_status.degree = temp_1;
    
    running_status.cur_cycle = cmd.current_cycle;
    running_status.total_cycle = cmd.total_cycle;
    running_status.cur_section = cmd.current_section;
    running_status.cur_step = cmd.current_step;
    running_status.degree = temp_1;
    running_status.target_degree = cmd.pcr_temp;
    snprintf(running_status.status, sizeof(running_status.status), "%s", g_status.status);
    snprintf(running_status.alarm, sizeof(running_status.alarm), "%s", g_status.alarm);
    snprintf(running_status.linux_status, sizeof(running_status.linux_status), "running");
    GetTemplateName(running_status.template_name);
    running_status.total_time = GetSectionsTotalTime();
    running_status.waiting_time = cmd.time;
    snprintf(running_status.info, sizeof(running_status.info), "%s", cmd.desc);

    if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_SET_TEMP_AMP) == 0) {
        if (GetPcrTempWaitFlag() && pcr_status.is_pcr_temp_reach) {
            SetPcrTempWaitFlag(0);
            if (cmd.is_acquisition_amp) {
                if (s_time_hold_timer_info == NULL) {
                    memcpy(&s_running_status, &running_status, sizeof(PCRRunningStatus));
                    s_running_status.waiting_time = cmd.time + 3;
                    warn_printf("AMP temp reach, create hold time timer pthread, waiting_time:%f.\n", s_running_status.waiting_time);
                    s_time_hold_timer_info = ResetMillisecondsTimer(HOLD_TIME_TIMER*100, HoldTime, &s_running_status, &s_time_hold_thread_id);
                } 
            } else if (cmd.is_acquisition_melt) {
                if (s_temp_query_timer_info) {
                    pthread_mutex_lock(&s_lock);
                    s_temp_query_timer_info->quit_flag = 1;
                    pthread_join(s_temp_query_thread_id, NULL);
                    s_temp_query_thread_id = 0;
                    s_temp_query_timer_info = NULL;
                    s_query_exist = 1;
                    pthread_mutex_unlock(&s_lock);
                }  

                if (s_time_hold_timer_info == NULL) {
                    warn_printf("Melt temp reach, create hold time timer pthread, waiting_time:%f.\n", s_running_status.waiting_time);
                    memcpy(&s_running_status, &running_status, sizeof(PCRRunningStatus));
                    s_time_hold_timer_info = ResetMillisecondsTimer(HOLD_TIME_TIMER*100, HoldTime, &s_running_status, &s_time_hold_thread_id);
                }
                pthread_mutex_lock(&s_lock);
                s_hold_time_exist = 1;
                pthread_mutex_unlock(&s_lock);
            }    
        }
    } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_GET_CURRENT_TEMP_POST) == 0) {
        SetTempPost(&temp_post[0]);
    }

    if (s_time_hold_timer_info == NULL && !(temp_rsp_cnt % 10)) {
        PCRRunningStatusReport(&running_status);
    }

    if (s_cond) {
        SignalAsyncCond(s_cond, NULL, NULL);
    }

    return 0;
}

int UartPacketParseErrReport() {
    PCRStatus status_report = {0};
    PCRRunningStatus running_status = {0};
    PcrSlaveCmd cmd = {0};
    int ret = 0;

    ret = GetCmdQueueFront(&cmd);

    GetTemplateName(status_report.template_name);
    snprintf(status_report.status, sizeof(status_report.status), "%s", g_status.status);
    snprintf(status_report.alarm, sizeof(status_report.alarm), "%s", g_status.alarm);
    snprintf(status_report.linux_status, sizeof(status_report.linux_status), "%s", g_status.linux_status);
    snprintf(status_report.linux_alarm, sizeof(status_report.alarm), "uart packet parse err");
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
        snprintf(status_report.status, sizeof(status_report.status), "%s", g_status.status);
        snprintf(status_report.alarm, sizeof(status_report.alarm), "%s", g_status.alarm);
        snprintf(status_report.linux_status, sizeof(status_report.linux_status), "%s", g_status.linux_status);
        snprintf(status_report.linux_alarm, sizeof(status_report.alarm), "uart packet parse err");
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

int UartPacketHandler(unsigned char *packet, unsigned int len) {
    int ret = 0;
    UartHeader header = {0};
    u_int8_t* data = NULL;
    u_int8_t crc = 0;
    u_int8_t func_id = 0;

    if (packet == NULL || len < sizeof(UartHeader)) {
        err_printf("Input err, packet ptr is NULL or len is invalid.\n");
        UartPacketParseErrReport();
        return -1;
    }
    
    memcpy(&header, packet, sizeof(UartHeader));
    if (len < header.data_len) {
        err_printf("Packet is invalid, recv len:%d is less than ori len:%d.\n", len, header.data_len);
        UartPacketParseErrReport();
        return -1;
    }
    
    crc = packet[header.data_len-1];
    if (GenCRCCheck(packet, header.data_len-1) != crc) {
        err_printf("Packet is invalid, crc check err.\n");
        UartPacketParseErrReport();
        return -1;
    }
    
    data = packet + sizeof(UartHeader);
    func_id = header.cmd_type & SLAVE_CMD_FUNCID_RSP_FLAG;

    warn_printf("uart handler,func_id:%x\n", func_id);

    switch (func_id) {
        case SLAVE_CMD_FUNCID_HELLO:
            SlaveCmdHelloRspHandler(data);
            break;

        case SLAVE_CMD_FUNCID_SET_TEMPERATURE:
            SlaveCmdSetTempRspHandler(data);
            break;

        case SLAVE_CMD_FUNCID_MOTOR_CTRL:
            SlaveCmdMotorCtrlRspHandler(data);  
            break;

        case SLAVE_CMD_FUNCID_GET_TEMPERATURE:
            SlaveCmdGetTempRspHandler(data);   //cmd abanton
            break;

        case SLAVE_CMD_FUNCID_SET_CAP:
            SlaveCmdSetCapRspHandler(data);   //cmd abanton
            break;

        case SLAVE_CMD_FUNCID_GET_PCR_DATA:
            SlaveCmdGetPcrDataRspHandler(data);
            break;

        case SLAVE_CMD_FUNCID_SET_LED:
            SlaveCmdSetLedRspHandler(data);
            break;

        case SLAVE_CMD_FUNCID_GET_PCR_TEMPERATURE:
            SlaveCmdGetPcrTempRspHandler(data);
            break;

        default:
            break;
    }
    
    return 0;
}


int LedBlinkCmdEnQueue() {
    PcrSlaveCmd cmd = {0};
    snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_LED_BLINK);
    snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_LED_BLINK);
    cmd.retry_cnt = RETYRY_NUM;
    cmd.loop = 1;
    cmd.time = 1;
    cmd.current_section = 0;
    cmd.current_cycle = 0;
    cmd.current_step = 0;
    
    return CmdEnQueue(&cmd);
}

int LedBrightCmdEnQueue() {
    PcrSlaveCmd cmd = {0};
    snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_LED_BRIGHT);
    snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_LED_BRIGHT);
    cmd.retry_cnt = RETYRY_NUM;
    cmd.loop = 1;
    cmd.time = 1;
    cmd.current_section = 0;
    cmd.current_cycle = 0;
    cmd.current_step = 0;
    
    return CmdEnQueue(&cmd);
}

int MotorResetCmdEnQueue(u_int16_t current_section, u_int16_t current_cycle, u_int16_t current_step) {
    PcrSlaveCmd cmd = {0};
    snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_MOTOR_RESET);
    snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_MOTOR_RESET);
    cmd.retry_cnt = RETYRY_NUM;
    cmd.loop = 1;
    cmd.time = 1;
    cmd.current_section = current_section;
    cmd.current_cycle = current_cycle;
    cmd.current_step = current_step;
    cmd.is_acquisition_amp = 0;
    
    return CmdEnQueue(&cmd);
}

void* PCRDoneStatusReportTimerHandler(void* arg) {
    if (s_pcr_finish_status_ack == 0) {
        warn_printf("Timer handler, resend PCR done status.\n");
        PCRDoneStatusReport(GetSeqId(), &s_done_status);
    }    
    return NULL;
}

int PCRDoneStatusReport(u_int32_t seq_id, PCRDoneStatus* status) {
    char* json_str = NULL;
    int ret = 0;

    if (status == NULL) {
        err_printf("Input err, status ptr is NULL.\n");
        return -1;
    }

    if (strlen(status->template_name) == 0 && strlen(status->status) == 0) {
        err_printf("Input err, template name and status is NULL.\n");
        return -1;
    }

    status->running_time = GetTaskUsedTime();
    if (status->total_time > status->running_time) {
        status->left_time = status->total_time - status->running_time;
    } else {
        status->left_time = 0;
    }

    json_str = PackPCRDoneStatusReport(seq_id, status);
    if (json_str == NULL) {
        err_printf("Report PCR done status failed, json pack err.\n");
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Report PCR done status failed, wss send err.\n");
    } else {
        warn_printf("Report PCR done status success.\n");
        s_pcr_finish_status_ack = 0;
    }

    free(json_str);
    if (s_report_pcr_finish_status_timer_info == NULL) {
        s_report_pcr_finish_status_timer_info = ResetMillisecondsTimer(30000, PCRDoneStatusReportTimerHandler, NULL, &s_report_pcr_finish_status_thread_id);
        pthread_detach(s_report_pcr_finish_status_thread_id);
        warn_printf("Create PCR done status report timer success.\n");
    }

    return ret;
}

int PCRStatusReport(PCRStatus* status) {
    char* json_str = NULL;
    int ret = 0;

    if (status == NULL) {
        err_printf("Input err, status ptr is NULL.\n");
        return -1;
    }

    json_str = PackPCRStatusReport(status);
    if (json_str == NULL) {
        err_printf("Report PCR status failed, json pack err.\n");
        return -1;
    }   

    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Report PCR status failed, wss send err.\n");
    }

    free(json_str);
    return ret;
}

int PCRRunningStatusReport(PCRRunningStatus* status) {
    char* json_str = NULL;
    int ret = 0;

    if (status == NULL) {
        err_printf("Input err, status ptr is NULL.\n");
        return -1;
    }

    status->running_time = GetTaskUsedTime();
    if (status->total_time > status->running_time) {
        status->left_time = status->total_time - status->running_time;
    } else {
        status->left_time = 0;
    }

    json_str = PackPCRRunningStatusReport(status);
    if (json_str == NULL) {
        err_printf("Report PCR running status failed, json pack err.\n");
        return -1;
    }

    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Report PCR running status failed, wss send err.\n");
    }

    free(json_str);
    return ret;
}

int QueryDeviceHandler(WssHeader* header, cJSON* json) {
    int ret = 0;
    char* json_str = NULL;
    LocalCfg local_cfg = {0};
    cJSON* item_json = NULL;
    int position = 0;
    char device_id[DEVICE_ID_LEN] = {0};
    char device_type[DEVICE_ID_LEN] = {0};

    if (header == NULL) {
        err_printf("Input err, WssHeader ptr is NULL.\n");
        return -1;
    }

    item_json = cJSON_GetObjectItem(json, "Position");
    if (item_json == NULL) {
        err_printf("Json parse failed, get key[Position] err.\n");
        return -1;
    }
    position = item_json->valueint;

    item_json = cJSON_GetObjectItem(json, "DeviceID");
    if (item_json == NULL || item_json->valuestring == NULL) {
        err_printf("Json parse failed, get key[DeviceID] err.\n");
        return -1;
    }
    snprintf(device_id, sizeof(device_id), "%s", item_json->valuestring);

    item_json = cJSON_GetObjectItem(json, "DeviceType");
    if (item_json == NULL) {
        err_printf("Json parse failed, get key[DeviceType] err.\n");
        return -1;
    }
    snprintf(device_type, sizeof(device_type), "%s", item_json->valuestring);

    ret = LoadLocalCfgFile(&local_cfg);
    if (ret != 0) {
        err_printf("Load local config failed.\n");
        return -1;
    }
    local_cfg.position = position;
    snprintf(local_cfg.dev_id, sizeof(local_cfg.dev_id), "%s", device_id);
    snprintf(local_cfg.dev_type, sizeof(local_cfg.dev_type), "%s", device_type);
    if (strcmp(device_type, DEVICE_TYPE_RNA) == 0) {
        local_cfg.uart_cfg.baud_rate = 38400;
    } else if (strcmp(device_type, DEVICE_TYPE_PCR) == 0) {
        local_cfg.uart_cfg.baud_rate = 9600;
    }
    SetLocalCfg(&local_cfg);
    ret = SaveLocalCfgFile(&local_cfg);
    if (ret != 0) {
        err_printf("Save position to local config file failed.\n");
        return -1;
    }

    json_str = PackQueryDeviceReply(header->seq_id);
    if (json_str == NULL) {
        err_printf("Reply to cmd type:%s failed, reply json pack err.\n", header->type);
        return -1;
    }

    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret == 0) {
        warn_printf("Reply to cmd type:%s success.\n", header->type);
    } else {
        err_printf("Reply to cmd type:%s failed, wss send err.\n", header->type);
    }

    free(json_str);
    warn_printf("<!Notice!> Query device set finished, device will reboot after 3s!!!");
    sleep(3);
    warn_printf("<!Notice!> Reboot Now!!!\n");
    system_call("reboot now");
    return ret;
}

int HeartBeatHandler(WssHeader* header, cJSON* json) {
    int ret = 0;
    char* json_str = NULL;
    char time_str[DATATIME_LEN] = {0};
    char cmd[200] = {0};
    static unsigned short time_cnt = 0;

    if (header == NULL) {
        err_printf("Input err, WssHeader ptr is NULL.\n");
        return -1;
    }

    time_cnt++;
    cJSON* item_json = cJSON_GetObjectItem(json, "Time");
    if (item_json && item_json->valuestring) {
        snprintf(time_str, sizeof(time_str), "%s", item_json->valuestring);
        /*if (GetSectionQueueSize() == 0 && GetCmdQueueSize() == 0) {
            if (time_cnt >= ResetSystermTimeCnt) {
                time_cnt = 0;
                snprintf(cmd, sizeof(cmd), "date -s \"%s\"", time_str);
                ret = system_call(cmd);
                warn_printf("Set local time, excute cmd : %s, ret = %d\r\n", cmd, ret);
            }
        }*/
        if (s_time_syn_flag == 0) {
            snprintf(cmd, sizeof(cmd), "date -s \"%s\"", time_str);
            ret = system_call(cmd);
            warn_printf("Set local time, excute cmd : %s, ret = %d\r\n", cmd, ret);
            s_time_syn_flag = 1;
        }
    }

    json_str = PackHeartBeatReply(header->seq_id);
    if (json_str == NULL) {
        err_printf("Reply to cmd type:%s failed, reply json pack err.\n", header->type);
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Reply to cmd type:%s failed, wss send err.\n", header->type);
    }

    free(json_str);
    return ret;
}

int PCRAckReply(u_int32_t seq_id, PCRAck* ack) {
    char* json_str = NULL;
    int ret = 0;

    if (ack == NULL) {
        err_printf("Input err, ack ptr is NULL.\n");
        return -1;
    }

    json_str = PackPCRAckReport(seq_id, ack);
    if (json_str == NULL) {
        err_printf("Reply PCR ack failed, reply json pack err.\n");
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Reply PCR ack failed, wss send err.\n");
    }

    free(json_str);
    return ret;
}

int PCRStopAckReply(u_int32_t seq_id, PCRAck* ack) {
    char* json_str = NULL;
    int ret = 0;

    if (ack == NULL) {
        err_printf("Input err, ack ptr is NULL.\n");
        return -1;
    }

    json_str = PackPCRFinishAckReport(seq_id, ack);
    if (json_str == NULL) {
        err_printf("Reply PCR stop ack failed, reply json pack err.\n");
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Reply PCR stop ack failed, wss send err.\n");
    }

    free(json_str);
    return ret;
}

int ProgramSectionsHandler(cJSON* array_json) {
    cJSON* item_json = NULL;
    int array_size = 0;
    int i = 0;
    int ret = 0;

    if (array_json == NULL) {
        err_printf("Input err, array json is NULL.\n");
        return -1;
    }

    array_size = cJSON_GetArraySize(array_json);
    for (i = 0; i < array_size; i++) {
        item_json = cJSON_GetArrayItem(array_json, i);
        if (item_json == NULL) {
            err_printf("Json parse failed, get %dth array item err.\n", i);
            return -1;
        }

        Section section = {0};
        ret = ParseSection(item_json, &section);
        if (ret != 0) {
            err_printf("Array %dth item json parse err.\n", i);
            return -1;
        }
        SectionEnQueue(&section);
    }

    //SetSectionsTotalTime();   
    return 0;
}

void* PCRTaskWorkflowMgr(void* arg) {
    PCRTaskInfo* task_info = (PCRTaskInfo*)arg;
    int ret = 0;
    char* json_str = NULL;
    u_int8_t data_len = 0;

    warn_printf("=======PCRTaskWorkflowMgr entry============.\n");
    memset(&s_done_status, 0, sizeof(PCRDoneStatus));
    GetTemplateName(s_done_status.template_name);
    s_done_status.total_time = GetSectionsTotalTime();
    s_cond = CreateAsyncCond(task_info->template_name);
    if (s_cond == NULL) {
        err_printf("Create async cond err.\n");
        snprintf(s_done_status.status, sizeof(s_done_status.status), "error");
        snprintf(s_done_status.alarm, sizeof(s_done_status.alarm), "Create async cond err");
        PCRDoneStatusReport(task_info->seq_id, &s_done_status);
        snprintf(g_status.linux_status, sizeof(g_status.linux_status), "error");
        snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "Create async cond err");
        DeinitWorkflowMgr();
        warn_printf("=======PCRTaskWorkflowMgr exit============.\n");
        pthread_exit((void *)0);
    }

    while (!s_task_exist) {
        if (GetCmdQueueSize() == 0) {
            warn_printf("All cmd is done, template:%s task is finished.\n", task_info->template_name);
            break;
        }

        PcrSlaveCmd cmd = {0};
        GetCmdQueueFront(&cmd);

        s_done_status.cur_cycle = cmd.current_cycle;
        s_done_status.cur_section = cmd.current_section;
        s_done_status.cur_step = cmd.current_step;
        s_done_status.total_cycle = cmd.total_cycle;
        s_done_status.waiting_time = cmd.time;
        s_done_status.degree = GetCurrentTemp();
        s_done_status.target_degree = cmd.pcr_temp;
        snprintf(s_done_status.info, sizeof(s_done_status.info), "%s", cmd.desc);

        if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_LED_BLINK) == 0) {
            u_int8_t set_flag = SLAVE_LED_SET_BLINK_FLAG;
            json_str = PackSlaveCmdSetLed(&set_flag, &data_len);
        } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_LED_BRIGHT) == 0) {
            u_int8_t set_flag = SLAVE_LED_SET_BRIGHT_FLAG;
            json_str = PackSlaveCmdSetLed(&set_flag, &data_len);
        } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_MOTOR_RESET) == 0) {
            MotorCtrlParam param = {0};
            param.ctrl_flag = SLAVE_MOTOR_RESET_FLAG;
            json_str = PackSlaveCmdMotorCtrl(&param, &data_len);
        } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_MOTOR_SCAN) == 0) {
            MotorCtrlParam param = {0};
            param.ctrl_flag = SLAVE_MOTOR_SCAN_FLAG;
            param.pulse_h = 0x1D;
            param.pulse_l = 0x4C;
            param.direct = 01;
            param.speed_h = 0x32;
            param.speed_l = 0xC8;
            json_str = PackSlaveCmdMotorCtrl(&param, &data_len);
        } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_SET_TEMP_AMP) == 0) {
            u_int8_t temp[9] = {0};
            temp[0] = ((int)(cmd.pcr_temp * 100) >> 8) & 0xFF;
            temp[1] = (int)(cmd.pcr_temp * 100) & 0xFF;
            warn_printf("Set amp temp:%f.\n", cmd.pcr_temp);
            json_str = PackSlaveCmdSetTemp(&temp, &data_len);
            if (json_str != NULL) {
                SetPcrTempWaitFlag(0);
            }
        } else if ((strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP_END) == 0) || 
                    (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_MELT) == 0)) {
            json_str = PackSlaveCmdGetPcrData(&data_len);
        } else if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_GET_CURRENT_TEMP_POST) == 0) {
            json_str = PackSlaveCmdGetPcrTemp(&data_len);
        }

        if (json_str == NULL) {
            err_printf("Json pack slave ctrl cmd err, cmd:%s.\n", cmd.cmd_name);
            snprintf(s_done_status.status, sizeof(s_done_status.status), "error");
            snprintf(s_done_status.alarm, sizeof(s_done_status.alarm), "Json pack slave ctrl cmd err, cmd:%s, cmd_id:%d.\n", cmd.cmd_name, cmd.id);
            PCRDoneStatusReport(task_info->seq_id, &s_done_status);
            snprintf(g_status.linux_status, sizeof(g_status.linux_status), "error");
            snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "Json pack slave ctrl cmd err, cmd:%s, cmd_id:%d.\n", cmd.cmd_name, cmd.id);
            DestroyAsyncCond(s_cond);
            s_cond = NULL;
            DeinitWorkflowMgr();
            warn_printf("=======PCRTaskWorkflowMgr exit============.\n");
            pthread_exit((void *)0);
        }

RETRY:
        while (cmd.retry_cnt)
        {
            if (s_task_exist) {
                cmd.retry_cnt = 0;
                break;
            }

            ret = UartSendSync(json_str, data_len);
            if (ret != 0) {
                snprintf(s_done_status.alarm, sizeof(s_done_status.alarm), "Uart send slave ctrl cmd err, cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d.\n", 
                    cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step);
                snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "Uart send slave ctrl cmd err, cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d.\n", 
                    cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step);
                cmd.retry_cnt--;
                usleep(200000);
                err_printf("Uart send slave ctrl cmd err, cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d, remain retry cnt:%d.\n", 
                    cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step, cmd.retry_cnt);
            } else {
                warn_printf("Uart send slave ctrl cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d success.\n", 
                    cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step);
                if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_SET_TEMP_AMP) == 0) {
                    pthread_mutex_lock(&s_lock);
                    s_hold_time_exist = 0;
                    pthread_mutex_unlock(&s_lock);
                }
                if (WaitAsyncCond(s_cond, SLAVE_CMD_PCR_TIMEOUT) < 0) {
                    snprintf(s_done_status.alarm, sizeof(s_done_status.alarm), "Slave ctrl cmd timeout, cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d.\n", 
                        cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step);
                    snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "Slave ctrl cmd timeout, cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d.\n", 
                        cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step);
                    cmd.retry_cnt--;
                    err_printf("Slave ctrl cmd timeout, cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d, remain retry cnt:%d.\n", 
                        cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step, cmd.retry_cnt);
                } else {
                    if (s_task_exist) {
                        cmd.retry_cnt = 0;
                    }
                    break;
                }
            }
        }

        if (cmd.retry_cnt == 0) {
            pthread_mutex_lock(&s_lock);
            s_task_exist = true;
            pthread_mutex_unlock(&s_lock);
        } else {
            if (s_slave_cmd_status) {
                warn_printf("Exec slave ctrl cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d success,cmd que size:%d.\n", 
                    cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step, GetCmdQueueSize());
                if (strcmp(cmd.cmd_name, PCR_SLAVE_CMD_PCR_SET_TEMP_AMP) == 0) {
                    while (!s_task_exist && !s_hold_time_exist) {
                        sleep(3);
                    }
                }
                CmdDeQueue();
            } else {                
                snprintf(s_done_status.alarm, sizeof(s_done_status.alarm), "Slave ctrl cmd exec err, cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d.\n", 
                    cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step);
                snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "Slave ctrl cmd exec err, cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d.\n", 
                    cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step);
                cmd.retry_cnt--;
                err_printf("Exec slave ctrl cmd:%s, cmd_id:%d, section:%d, cycle:%d, step:%d err, remain retry cnt:%d.\n", 
                    cmd.cmd_name, cmd.id, cmd.current_section, cmd.current_cycle, cmd.current_step, cmd.retry_cnt);
                if (cmd.retry_cnt > 0) {
                    goto RETRY;
                } else {
                    pthread_mutex_lock(&s_lock);
                    s_task_exist = true;
                    pthread_mutex_unlock(&s_lock);
                }
            }  
        }
        free(json_str);
    }

    if (s_temp_query_timer_info) {
        s_temp_query_timer_info->quit_flag = 1;
        if (s_temp_query_thread_id != 0) {
            pthread_join(s_temp_query_thread_id, NULL);
            s_temp_query_thread_id = 0;
        }
        s_temp_query_timer_info = NULL;
    }

    if (s_report_result_timer_info) {
        pthread_mutex_lock(&s_lock);
        s_report_result_timer_info->quit_flag = 1;
        pthread_join(s_report_result_thread_id, NULL);
        s_report_result_timer_info = NULL;
        s_report_result_thread_id = 0;
        pthread_mutex_unlock(&s_lock);
    }
    PCRExperimentResultReport(1);

    if (s_task_exist) {
        snprintf(s_done_status.status, sizeof(s_done_status.status), "error");
        PCRDoneStatusReport(task_info->seq_id, &s_done_status);
        snprintf(g_status.linux_status, sizeof(g_status.linux_status), "error");
    } else {
        snprintf(s_done_status.status, sizeof(s_done_status.status), "ok");
        PCRDoneStatusReport(task_info->seq_id, &s_done_status);
        snprintf(g_status.linux_status, sizeof(g_status.linux_status), "free");
        snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "");
    }

    DeinitWorkflowMgr();
    if (s_cond) {
        DestroyAsyncCond(s_cond);
        s_cond = NULL;
    }
    warn_printf("=======PCRTaskWorkflowMgr exit============.\n");
    pthread_exit((void *)0);
}

int PCRReqHandler(WssHeader* header, cJSON* root_json) {
    int ret = 0;
    cJSON* child_json = NULL;
    cJSON* item_json = NULL;
    cJSON* program_json = NULL;
    cJSON* section_json = NULL;
    cJSON* experiment_json = NULL;
    PCRAck ack = {0};
    static PCRTaskInfo task_info = {0};
    int force = 0;

    if (header == NULL || root_json == NULL) {
        err_printf("Input err, WssHeader or json ptr is NULL.\n");
        return -1;
    }

    item_json = cJSON_GetObjectItem(root_json, "Force");
    if (item_json != NULL) {
        force = item_json->valueint;
    } else {
        force = 0;
    }
    if (GetSectionQueueSize() && GetCmdQueueSize()) {
        if (force == 1) {
            pthread_mutex_lock(&s_lock);
            s_task_exist = true;
            if (s_cond) {
                SignalAsyncCond(s_cond, NULL, NULL);
            }
            pthread_mutex_unlock(&s_lock);
            sleep(1);
                    
            if (s_temp_query_timer_info) {
                pthread_mutex_lock(&s_lock);
                s_temp_query_timer_info->quit_flag = 1;
                pthread_mutex_unlock(&s_lock);

                usleep(10000);

                pthread_mutex_lock(&s_lock);
                if (s_temp_query_thread_id != 0) {
                    pthread_join(s_temp_query_thread_id, NULL);
                    s_temp_query_thread_id = 0;
                }    
                s_temp_query_timer_info = NULL;
                s_query_exist = true;
                pthread_mutex_unlock(&s_lock);
            }

            if (s_time_hold_timer_info) {
                pthread_mutex_lock(&s_lock);
                s_time_hold_timer_info->quit_flag = 1;
                pthread_mutex_unlock(&s_lock);

                usleep(10000);

                pthread_mutex_lock(&s_lock);
                if (s_time_hold_thread_id != 0) {
                    pthread_join(s_time_hold_thread_id, NULL);
                    s_time_hold_thread_id = 0;
                }
                s_time_hold_timer_info = NULL;
                s_hold_time_exist = true;
                s_hold_time_waiting_time = 0.0;
                pthread_mutex_unlock(&s_lock);
            }

            sleep(1);
            PCRDoneStatus status = {0};
            snprintf(status.template_name, sizeof(status.template_name), "%s", task_info.template_name);
            snprintf(status.status, sizeof(status.status), "error");
            snprintf(status.alarm, sizeof(status.alarm), "PCR workflow force to exit");
            PCRDoneStatusReport(task_info.seq_id, &status);
            usleep(10000);
            pthread_mutex_lock(&s_lock);
            if (s_report_pcr_finish_status_timer_info) {               
                s_report_pcr_finish_status_timer_info->quit_flag = 1;
                if (s_report_pcr_finish_status_thread_id != 0) {
                    s_report_pcr_finish_status_thread_id = 0;
                }
                s_pcr_finish_status_ack = 1;               
            }
            pthread_mutex_unlock(&s_lock);

            warn_printf("Template:%s is force to stop.\n", task_info.template_name);
        } else {
            if (GetSectionQueueSize() && GetCmdQueueSize()) {
                snprintf(ack.alarm, sizeof(ack.alarm), "PCR template:%s is running, cannot create new workflow.\n", task_info.template_name);
                err_printf("PCR template:%s is running, cannot create new workflow.\n", task_info.template_name);
                goto FAIL;
            }
        }
    }

    memset(&task_info, 0, sizeof(PCRTaskInfo));
    s_hold_time_waiting_time = 0.0;
    pthread_mutex_lock(&s_lock);
    s_task_exist = false;
    pthread_mutex_unlock(&s_lock);
    task_info.seq_id = header->seq_id;

    child_json = cJSON_GetObjectItem(root_json, "Template");
    if (child_json == NULL || child_json->valuestring == NULL) {
        err_printf("Json parse key[Template] err.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        goto FAIL;
    }
    snprintf(task_info.template_name, sizeof(task_info.template_name), "%s", child_json->valuestring);

    child_json = cJSON_GetObjectItem(root_json, "BatchID");
    if (child_json == NULL || child_json->valuestring == NULL) {
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        err_printf("Json parse key[BatchID].\n");
        goto FAIL;
    }
    snprintf(task_info.batch_id, sizeof(task_info.batch_id), "%s", child_json->valuestring);
    
    child_json = cJSON_GetObjectItem(root_json, "CurrentPhase");
    if (child_json) {
        task_info.cur_phase = child_json->valueint;
    }

    child_json = cJSON_GetObjectItem(root_json, "CurrentCycle");
    if (child_json) {
        task_info.cur_cycle = child_json->valueint;
    }

    child_json = cJSON_GetObjectItem(root_json, "CurrentSection");
    if (child_json) {
        task_info.cur_section = child_json->valueint;
    }

    child_json = cJSON_GetObjectItem(root_json, "CurrentStep");
    if (child_json) {
        task_info.cur_step = child_json->valueint;
    }

    child_json = cJSON_GetObjectItem(root_json, "RunningTime");
    if (child_json) {
        task_info.running_time = child_json->valueint;
    }

    child_json = cJSON_GetObjectItem(root_json, "TotalTime");
    if (child_json == NULL) {
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        err_printf("Json parse key[TotalTime].\n");
        goto FAIL;
    }
    task_info.total_time = child_json->valueint;

    child_json = cJSON_GetObjectItem(root_json, "Experiment");
    if (child_json == NULL) {
        err_printf("Json parse key[Experiment] err.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        goto FAIL;
    }

    experiment_json = cJSON_Duplicate(child_json, 1);
    if (experiment_json == NULL) {
        err_printf("Duplicate experiment json dailed.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        goto FAIL;
    }

    ret = InitWorkflowMgr(task_info.seq_id, task_info.template_name, experiment_json);
    if (ret != 0) {
        err_printf("Init workflow mgr failed.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Workflow init err");
        goto FAIL;
    }
    SetSectionsTotalTime(task_info.total_time);

    item_json = cJSON_GetObjectItem(child_json, "Project");
    if (item_json == NULL) {
        err_printf("Json parse key[Project] err.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        goto FAIL;
    }
    program_json = cJSON_GetObjectItem(item_json, "Program");
    if (program_json == NULL) {
        err_printf("Json parse key[Program] err.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        goto FAIL;
    }
    section_json = cJSON_GetObjectItem(program_json, "Sections");
    if (section_json == NULL) {
        err_printf("Json parse key[Sections] err.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        goto FAIL;
    }

    ret = ProgramSectionsHandler(section_json);
    if (ret != 0) {
        err_printf("Program sections json parse err.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        DeinitWorkflowMgr();
        goto FAIL;
    }

    LedBlinkCmdEnQueue();
    MotorResetCmdEnQueue(0, 1, 0);
    InitCmdQueue();
    MotorResetCmdEnQueue(0, 0, 0);
    LedBrightCmdEnQueue();

    warn_printf("Cmd queue size:%d.\n", GetCmdQueueSize());

    ret = pthread_create(&task_info.workflow_thread_id, NULL, PCRTaskWorkflowMgr, &task_info);
    if (ret != 0) {
        err_printf("Create PCR task workflow pthread err.\n");
        snprintf(ack.alarm, sizeof(ack.alarm), "Create PCR workflow pthread err");
        DeinitWorkflowMgr();
        goto FAIL;
    } else {
        snprintf(g_status.linux_status, sizeof(g_status.linux_status), "running");
        snprintf(g_status.linux_alarm, sizeof(g_status.linux_alarm), "");
        pthread_detach(task_info.workflow_thread_id);
    }

    snprintf(ack.status, sizeof(ack.status), "ok");
    PCRAckReply(header->seq_id, &ack);

    return 0;

FAIL:    
    snprintf(ack.status, sizeof(ack.status), "error");
    PCRAckReply(header->seq_id, &ack);
    return -1;
}

int PCRStopHandler(WssHeader* header) {
    PCRAck ack = {0};

    if (header == NULL) {
        err_printf("Input err, WssHeader is NULL.\n");
        return -1;
    }

    if (GetSectionQueueSize() && GetCmdQueueSize()) {
        pthread_mutex_lock(&s_lock);
        s_task_exist = true;
        if (s_cond) {
            SignalAsyncCond(s_cond, NULL, NULL);
        }
        pthread_mutex_unlock(&s_lock);
        sleep(1);
                    
        if (s_temp_query_timer_info) {
            pthread_mutex_lock(&s_lock);
            s_temp_query_timer_info->quit_flag = 1;
            pthread_mutex_unlock(&s_lock);

            usleep(10000);

            pthread_mutex_lock(&s_lock);
            if (s_temp_query_thread_id != 0) {
                pthread_join(s_temp_query_thread_id, NULL);
                s_temp_query_thread_id = 0;
            }    
            s_temp_query_timer_info = NULL;
            s_query_exist = true;
            pthread_mutex_unlock(&s_lock);
        }

        if (s_time_hold_timer_info) {
            pthread_mutex_lock(&s_lock);
            s_time_hold_timer_info->quit_flag = 1;
            pthread_mutex_unlock(&s_lock);

            usleep(10000);

            pthread_mutex_lock(&s_lock);
            if (s_time_hold_thread_id != 0) {
                pthread_join(s_time_hold_thread_id, NULL);
                s_time_hold_thread_id = 0;
            }
            s_time_hold_timer_info = NULL;
            s_hold_time_exist = true;
            s_hold_time_waiting_time = 0.0;
            pthread_mutex_unlock(&s_lock);
        }

        sleep(3);
    }

    snprintf(ack.status, sizeof(ack.status), "ok");
    PCRStopAckReply(header->seq_id, &ack);
    return 0;
}

int UpgradeDoneStatusReport(u_int32_t seq_id, UpgradeDoneStatus* status) {
    char* json_str = NULL;
    int ret = 0;

    if (status == NULL) {
        err_printf("Input err, status ptr is NULL.\n");
        return -1;
    }

    json_str = PackUpgradeDoneReport(seq_id, status);
    if (json_str == NULL) {
        err_printf("Report upgrade done status failed, json packe err.\n");
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Report upgrade done status failed, wss send err.\n");
    }

    free(json_str);
    return ret;
}

int UpgradeAckReply(u_int32_t seq_id, PCRAck* ack) {
    char* json_str = NULL;
    int ret = 0;

    if (ack == NULL) {
        err_printf("Input err, ack ptr is NULL.\n");
        return -1;
    }

    json_str = PackUpgradeAckReport(seq_id, ack);
    if (json_str == NULL) {
        err_printf("Reply upgrade ack failed, reply json pack err.\n");
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Reply upgrade ack failed, wss send err.\n");
    }

    free(json_str);
    return ret;
}

int UpgradeHandler(WssHeader* header, cJSON* root_json, char* data) {
    PCRAck ack = {0};
    UpgradeDescInfo desc_info = {0};
    UpgradeDoneStatus status = {0};
    MsgInfo msg = {0};
    int ret = 0;

    if (header == NULL || root_json == NULL || data == NULL) {
        err_printf("Input err, WssHeader or json or data ptr is NULL.\n");
        return -1;
    }

    snprintf(ack.status, sizeof(ack.status), "ok");

    ret = ParseUpgradeDescInfo(root_json, &desc_info);
    if (ret != 0) {
        err_printf("Upgrade desc info json parse err.\n");
        snprintf(ack.status, sizeof(ack.status), "error");
        snprintf(ack.alarm, sizeof(ack.alarm), "Upgrade desc info json parse error");
        UpgradeAckReply(header->seq_id, &ack);
        return -1;
    }

    ret = ParseUpgradeVersionFile(data+sizeof(WssBinaryHeader)+header->desc_data_len, &desc_info);
    if (ret == -1) {
        err_printf("Upgrade version file parse err.\n");
        snprintf(ack.status, sizeof(ack.status), "error");
        snprintf(ack.alarm, sizeof(ack.alarm), "Upgrade version file parse error");
        UpgradeAckReply(header->seq_id, &ack);
        return -1;
    } else if (ret == -2) {
        err_printf("Upgrade version file md5 check err.\n");
        snprintf(ack.status, sizeof(ack.status), "error");
        snprintf(ack.alarm, sizeof(ack.alarm), "Upgrade version file md5 check error");
        UpgradeAckReply(header->seq_id, &ack);
        return -1;
    }

    UpgradeAckReply(header->seq_id, &ack);

    if (GetSectionQueueSize() && GetCmdQueueSize()) {
        err_printf("PCR template is running, cannot start upgrade, please stop PCR before upgrade.\n");
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "PCR task is running");
        UpgradeDoneStatusReport(header->seq_id, &status);
        return -1;
    }

    msg.header.src_app = APP_PCRMGR_ID;
    msg.header.dst_app = APP_UPGRADE_ID;
    msg.header.data_len = sizeof(UpgradeDescInfo);
    memcpy(msg.data, &desc_info, sizeof(UpgradeDescInfo));
    ret = SendMsg(APP_UPGRADE_ID, MSG_ID_APP_UPGRADE, &msg);
    if (ret != 0) {
        err_printf("Send upgrade msg to UpgradeApp failed.\n");
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Send upgrade msg to UpgradeApp failed");
        UpgradeDoneStatusReport(header->seq_id, &status);
        return -1;
    }

    return 0;
}

int PCRDoneStatusReplyHandler(void* arg) {
    warn_printf("Recieve PCR done status reply.\n");
    s_pcr_finish_status_ack = 1;
    pthread_mutex_lock(&s_lock);
    if (s_report_pcr_finish_status_timer_info) {       
        s_report_pcr_finish_status_timer_info->quit_flag = 1;
        if (s_report_pcr_finish_status_thread_id != 0) {
            s_report_pcr_finish_status_thread_id = 0;
        } 
        s_report_pcr_finish_status_timer_info = NULL;      
    }
    pthread_mutex_unlock(&s_lock);
    memset(&s_done_status, 0, sizeof(PCRDoneStatus));

    return 0;
}

int SetLogLevelHandler(WssHeader* header, cJSON* json) {
    int ret = 0;
    char* json_str = NULL;
    u_int8_t log_level = 2;
    char cmd[200] = {0};
    u_int8_t status = 0;

    if (header == NULL) {
        err_printf("Input err, WssHeader ptr is NULL.\n");
        return -1;
    }

    cJSON* item_json = cJSON_GetObjectItem(json, "LogLevel");
    if (item_json) {
        log_level = item_json->valueint;
        if (log_level < LOGGER_ERR || log_level > LOGGER_INFO_D) {
            err_printf("Set log level failed, log_level:%d is invalid, 0 < log_level < 6.\n", log_level);
            status = 1;
        } else {
            SetLoggerLevel(log_level);
            warn_printf("PcrMgrApp set log_level:%d successfully.\n", log_level);
        }
    } else {
        err_printf("Json parse key[LogLevel] err.\n");
        status = 2;
    }

    json_str = PackSetLogLevelReply(header->seq_id, status);
    if (json_str == NULL) {
        err_printf("Reply to cmd type:%s failed, reply json pack err.\n", header->type);
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Reply to cmd type:%s failed, wss send err.\n", header->type);
    }

    free(json_str);
    return ret;
}

int UpdateConfigReply(u_int32_t seq_id, PCRAck* status) {
    int ret = 0;
    char* json_str = NULL;

    if (status == NULL) {
        err_printf("Input err, status ptr is NULL, update config reply failed.\n");
        return -1;
    }

    json_str = PackUpdateCfgReply(seq_id, status);
    if (json_str == NULL) {
        err_printf("Reply to cmd type:%s failed, reply json pack err.\n", CMD_TYPE_UPDATE_CONFIG);
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Reply to cmd type:%s failed, wss send err.\n", CMD_TYPE_UPDATE_CONFIG);
    }

    free(json_str);
    return ret;
}

int UpdateConfigHandler(WssHeader* header, cJSON* json) {
    int ret = 0;
    char* json_str = NULL;
    u_int8_t log_level = 2;
    char cmd[200] = {0};
    PCRAck status = {0};

    if (header == NULL) {
        err_printf("Input err, WssHeader ptr is NULL.\n");
        return -1;
    }

     if (GetSectionQueueSize() > 0) {
        err_printf("Pcr task is running, cannot update config.\n");
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Pcr task is running, cannot update config.\n");
        UpdateConfigReply(header->seq_id, &status);
        return -1;
    }

    cJSON* child_json = cJSON_GetObjectItem(json, "Config");
    if (child_json) {
        cJSON* item_json = NULL;
        cJSON* child_item_json = NULL;
        cJSON* uart_cfg_json = cJSON_GetObjectItem(child_json, "Uart");
        if (uart_cfg_json) {
            LocalCfg local_cfg = {0};
            LoadLocalCfgFile(&local_cfg);
            item_json = cJSON_GetObjectItem(uart_cfg_json, "BaudRate");
            if (item_json) {
                local_cfg.uart_cfg.baud_rate = item_json->valueint;
            }

            item_json = cJSON_GetObjectItem(uart_cfg_json, "DataBits");
            if (item_json) {
                local_cfg.uart_cfg.data_bits = item_json->valueint;
            }

            item_json = cJSON_GetObjectItem(uart_cfg_json, "StopBits");
            if (item_json) {
                local_cfg.uart_cfg.stop_bits = item_json->valueint;
            }

            item_json = cJSON_GetObjectItem(uart_cfg_json, "Parity");
            if (item_json && item_json->valuestring) {
                snprintf(local_cfg.uart_cfg.parity, sizeof(local_cfg.uart_cfg.parity), "%s", item_json->valuestring);
            }

            DeinitUartComm();
            sleep(3);
            ret = InitUartComm(&local_cfg.uart_cfg, 1);
            if (ret < 0) {
                err_printf("Uart communication use new cfg init err.\n");
                snprintf(status.status, sizeof(status.status), "error");
                snprintf(status.alarm, sizeof(status.alarm), "Uart cfg update failed, uart restart failed.\n");
                UpdateConfigReply(header->seq_id, &status);
                return -1;
            } else {
                UartPacketHandlerReg(UartPacketHandler);
                SendHelloToSlave();
                SaveLocalCfgFile(&local_cfg);
            }    
        }

        cJSON* file_mgr_cfg_json = cJSON_GetObjectItem(child_json, "FileMgr");
        if (file_mgr_cfg_json) {
            FileManager file_mgf_cfg = {0};
            ReadFileMgrCfg(&file_mgf_cfg);
            item_json = cJSON_GetObjectItem(file_mgr_cfg_json, "RnaLog");
            if (item_json) {
                child_item_json = cJSON_GetObjectItem(item_json, "CleanTime");
                if (child_item_json) {
                    file_mgf_cfg.mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].clean_time = child_item_json->valueint;
                }

                child_item_json = cJSON_GetObjectItem(item_json, "CleanThreshold");
                if (child_item_json) {
                    file_mgf_cfg.mgr_cfg[MGR_FILE_TYPE_RNA_APP_LOG].clean_threshold = child_item_json->valueint;
                }
            }

            item_json = cJSON_GetObjectItem(file_mgr_cfg_json, "PcrLog");
            if (item_json) {
                child_item_json = cJSON_GetObjectItem(item_json, "CleanTime");
                if (child_item_json) {
                    file_mgf_cfg.mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].clean_time = child_item_json->valueint;
                }

                child_item_json = cJSON_GetObjectItem(item_json, "CleanThreshold");
                if (child_item_json) {
                    file_mgf_cfg.mgr_cfg[MGR_FILE_TYPE_PCR_APP_LOG].clean_threshold = child_item_json->valueint;
                }
            }

            item_json = cJSON_GetObjectItem(file_mgr_cfg_json, "PcrResult");
            if (item_json) {
                child_item_json = cJSON_GetObjectItem(item_json, "CleanTime");
                if (child_item_json) {
                    file_mgf_cfg.mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].clean_time = child_item_json->valueint;
                }

                child_item_json = cJSON_GetObjectItem(item_json, "CleanThreshold");
                if (child_item_json) {
                    file_mgf_cfg.mgr_cfg[MGR_FILE_TYPE_PCR_RESULT].clean_threshold = child_item_json->valueint;
                }
            }

            item_json = cJSON_GetObjectItem(file_mgr_cfg_json, "Disk");
            if (item_json) {
                child_item_json = cJSON_GetObjectItem(item_json, "UsedRateThreshold");
                if (child_item_json) {
                    file_mgf_cfg.disk_used_rate_threshold = child_item_json->valuedouble;
                }
            }

            SetFileMgrCfg(&file_mgf_cfg);
            SaveFileManagerFile(&file_mgf_cfg);
        }
    } else {
        err_printf("Json parse key[Config] err.\n");
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Json parse key[Config] err.\n");
        UpdateConfigReply(header->seq_id, &status);
    }

    snprintf(status.status, sizeof(status.status), "ok");
    UpdateConfigReply(header->seq_id, &status);

    return ret;
}

int FactoryResetReply(u_int32_t seq_id, PCRAck* status) {
    int ret = 0;
    char* json_str = NULL;

    if (status == NULL) {
        err_printf("Input err, status ptr is NULL, reset factory config reply failed.\n");
        return -1;
    }

    json_str = PackFactoryResetReply(seq_id, status);
    if (json_str == NULL) {
        err_printf("Reply to cmd type:%s failed, reply json pack err.\n", CMD_TYPE_FACTORY_RESET);
        return -1;
    }
    ret = WssSendData(json_str, strlen(json_str), WEBSOCKET_OP_TEXT);
    if (ret != 0) {
        err_printf("Reply to cmd type:%s failed, wss send err.\n", CMD_TYPE_FACTORY_RESET);
    }

    free(json_str);
    return ret;
}

int FactoryResetHandler(WssHeader* header, cJSON* json) {
    int ret = 0;
    int delay = 5;
    char cmd[512] = {0};
    PCRAck status = {0};

    if (header == NULL) {
        err_printf("Input err, WssHeader ptr is NULL.\n");
        return -1;
    }

    if (GetSectionQueueSize() > 0) {
        err_printf("Pcr task is running, cannot reset factory config.\n");
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Pcr task is running, cannot reset factory config.\n");
        FactoryResetReply(header->seq_id, &status);
        return -1;
    }

    cJSON* item_json = cJSON_GetObjectItem(json, "Delay");
    if (item_json) {
        delay = item_json->valueint;
    } else {
        err_printf("Json parse key[Delay] err.\n");
    }

    snprintf(cmd, sizeof(cmd), "cp -f %s %slocal_config/local_cfg_bk.json; cp -f %s %slocal_config/file_mgr_cfg_bk.json; cp -f %s %sserver_config/srv_cfg_bk.json;", 
        LOCAL_CFG_FILE, CONFIG_DIR_PATH, FILE_MGR_CFG_FILE, CONFIG_DIR_PATH, SERVER_CFG_FILE, CONFIG_DIR_PATH);
    if (system_call(cmd) != 0) {
		err_printf("Back up current config files failed. Call sys_cmd:%s failed.\n", cmd);
        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Back up current config files failed.\n");
        FactoryResetReply(header->seq_id, &status);
        return -1;
	} else {
		warn_printf("Back up current config files success. Call sys_cmd:%s success.\n", cmd);
	}

    memset(cmd, 0, sizeof(cmd));
    snprintf(cmd, sizeof(cmd), "cp -f %slocal_config/local_cfg.json %s; cp -f %slocal_config/file_mgr_cfg.json %s; cp -f %sserver_config/srv_cfg.json %s;", 
        ORI_CONFIG_DIR_PATH, LOCAL_CFG_FILE, ORI_CONFIG_DIR_PATH, FILE_MGR_CFG_FILE, ORI_CONFIG_DIR_PATH, SERVER_CFG_FILE);
    if (system_call(cmd) != 0) {
		err_printf("Reset ori factory config files failed. Call sys_cmd:%s failed.\n", cmd);
        memset(cmd, 0, sizeof(cmd));
        snprintf(cmd, sizeof(cmd), "cp -f %slocal_config/local_cfg_bk.json %s; cp -f %slocal_config/file_mgr_cfg_bk.json %s; cp -f %sserver_config/srv_cfg_bk.json %s;", 
            CONFIG_DIR_PATH, LOCAL_CFG_FILE, CONFIG_DIR_PATH, FILE_MGR_CFG_FILE, CONFIG_DIR_PATH, SERVER_CFG_FILE);
        if (system_call(cmd) != 0) {
            err_printf("Restore config files failed. Call sys_cmd:%s failed.\n", cmd);
        } else {
            warn_printf("Restore config files success. Call sys_cmd:%s success.\n", cmd);
        }

        snprintf(status.status, sizeof(status.status), "error");
        snprintf(status.alarm, sizeof(status.alarm), "Reset ori factory config files failed.\n");
        FactoryResetReply(header->seq_id, &status);
        return -1;
	} else {
		warn_printf("Reset ori factor config files success. Call sys_cmd:%s success.\n", cmd);
        snprintf(status.status, sizeof(status.status), "ok");
        FactoryResetReply(header->seq_id, &status);
	}

    warn_printf("<!Notice!> Factory reset is finished, device will reboot after %ds.\n", delay);
    sleep(delay);
    warn_printf("<!Notice!> Reboot Now!!!\n");
    system_call("reboot now");
    return ret;
}

int WssDataHandler (char *data, size_t len, u_int8_t data_type) {
    int ret = 0;
    char* json_str = NULL;
    WssHeader header = {0};
    cJSON* root_json = NULL;
    PCRAck ack = {0};

    if (data == NULL || len == 0) {
        err_printf("Input err, data ptr is NULL or len is 0.\n");
        return -1;
    }

    if (data_type == WEBSOCKET_OP_TEXT) {
        data[len] = '\0';
        warn_printf("Recv wss data:%s.\n", data);
        root_json = WssDataHeaderParse(&header, data);
    } else if (data_type == WEBSOCKET_OP_BINARY) {
        root_json = WssBinaryDataHeaderParse(&header, data);
    } else {
        err_printf("Undefined data_type:%d.\n", data_type);
        return -1;
    }

    if (root_json == NULL) {
        err_printf("Wss parse data header err.\n");
        snprintf(ack.status, sizeof(ack.status), "error");
        snprintf(ack.alarm, sizeof(ack.alarm), "Json parse error");
        if (strcmp(header.type, CMD_TYPE_PCR_REQ) == 0) {
            PCRAckReply(header.seq_id, &ack);
        } else if (strcmp(header.type, CMD_TYPE_PCR_STOP) == 0) {
            PCRStopAckReply(header.seq_id, &ack);
        } else if (strcmp(header.type, CMD_TYPE_UPGRADE) == 0) {
            UpgradeAckReply(header.seq_id, &ack);
        }
        return -1;
    }

    if (strcmp(header.type, CMD_TYPE_QUERY_DEVICE) == 0) {
        ret = QueryDeviceHandler(&header, root_json);
    } else if (strcmp(header.type, CMD_TYPE_HEART_BEAT) == 0) {
        ret = HeartBeatHandler(&header, root_json);
    } else if (strcmp(header.type, CMD_TYPE_PCR_REQ) == 0) {
        ret = PCRReqHandler(&header, root_json);
    } else if (strcmp(header.type, CMD_TYPE_PCR_STOP) == 0) {
        ret = PCRStopHandler(&header);
    } else if (strcmp(header.type, CMD_TYPE_UPGRADE) == 0) {
        ret = UpgradeHandler(&header, root_json, data);
    } else if (strcmp(header.type, CMD_TYPE_PCR_DONE_STATUS_REPLY) == 0) {
        ret = PCRDoneStatusReplyHandler(root_json);
    } else if (strcmp(header.type, CMD_TYPE_SET_LOG_LEVER) == 0) {
        ret = SetLogLevelHandler(&header, root_json);
    } else if (strcmp(header.type, CMD_TYPE_UPDATE_CONFIG) == 0) {
        ret = UpdateConfigHandler(&header, root_json);
    } else if (strcmp(header.type, CMD_TYPE_FACTORY_RESET) == 0) {
        ret = FactoryResetHandler(&header, root_json);
    }

    cJSON_Delete(root_json);

    return ret;
}
