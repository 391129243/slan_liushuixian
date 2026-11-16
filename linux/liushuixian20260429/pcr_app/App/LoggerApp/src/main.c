#include "Common.h"
#include "Timer.h"
#include "Logger.h"
#include "ConfLog.h"
#include "cJSON.h"
#include "System.h"
#include "MsgWrapper.h"

#define DEBUG_BUF_LEN 		8000
#define LOG_BUF_LEN 		3200
#define MAX_DEBUG_INFO_LEN 	3200

static int s_quit_flag = false;  //退出标志
static int close_delay_count = 0;
static char s_time_str[128] = {0};

static FILE* s_log_file = NULL;
static char s_log_filename[256] = {0};
static int s_log_byte_num = 0; //调试信息字节数统计

static FILE* s_debug_file = NULL;
static char s_debug_filename[256] = {0};
static int s_debug_byte_num = 0; //调试信息字节数统计
static bool s_debug_buff_flag = false;
static char* s_debug_str_arr[500000] = {0};
static int s_debug_str_num = 0;
static LogCfg s_log_cfg = {0};

static pthread_t s_file_mgr_thread_id = 0;
static PTimerMsgInfo s_file_mgr_timer_info = NULL;

int LoadLogConfig()
{
	int ret = -1;
	FILE *fp = NULL;
	cJSON* root_json = NULL;
	char json_data[MAX_JSON_DATA_LEN] = {0};

	memset(&s_log_cfg, 0, sizeof(s_log_cfg));

	snprintf(s_log_cfg.log_dir, sizeof(s_log_cfg.log_dir), "%s", LOGGER_DEFAULT_LOG_DIR);
	snprintf(s_log_cfg.log_prefix, sizeof(s_log_cfg.log_prefix), "%s", LOGGER_DEFAULT_LOG_FILE_PREFIX);
	snprintf(s_log_cfg.debug_prefix, sizeof(s_log_cfg.debug_prefix), "%s", LOGGER_DEFAULT_DEBUG_FILE_PREFIX);
	s_log_cfg.file_size = 5242880;
	s_log_cfg.dbg_rm_days_before = LOGGER_DEFAULT_RM_DAYS_BEFORE;

	ret = LoadLogCfgFile(&s_log_cfg);
	if (ret != 0) {
		err_printf("Load log cfg file err.\n");
		goto EXIT;
	}

EXIT:
	ret = Mkdir(s_log_cfg.log_dir);
	if (ret < 0) {
		err_printf("mkdir %s failed.\r\n", s_log_cfg.log_dir);
		memset(s_log_cfg.log_dir, 0, sizeof(s_log_cfg.log_dir));
		snprintf(s_log_cfg.log_dir, sizeof(s_log_cfg.log_dir), "./");
	}

	return ret;
}

void PrintDebugInfo(char* info)
{
	int len  = 0;
	char* data = NULL;

	if (info == NULL) return;

	//如果是缓存调试信息
	if ((s_debug_buff_flag == true) && (info != NULL))
	{
		len = strlen(info);
		data = malloc(len + 4);
		if (data != NULL)
		{
			strcpy(data, info);
			data[len] = '\0';
			s_debug_str_arr[s_debug_str_num] = data;
			s_debug_str_num++;
		}
		else
		{
			printf("debug info malloc failed. \r\n");
		}
		return;
	}

	printf("%s", info);
	
	if (s_debug_file != NULL)
	{
		s_debug_byte_num += strlen(info);
		fprintf(s_debug_file,"%s", info);
		fflush(s_debug_file);
	}
}

void PrintLogInfo(char* info)
{
	//printf("%s", info);
	
	if (s_log_file != NULL)
	{
		s_log_byte_num += strlen(info);
		fprintf(s_log_file,"%s", info);
		fflush(s_log_file);
	}
}

void CloseLogFile(int quit_flag)
{
	FILE* tmp_file = NULL;
	s_log_byte_num = 0;
	//FileInfo file_info = {0};
	MsgInfo msg_info = {0};

	if (s_log_file == NULL)
	{
		return;		
	}

	tmp_file = s_log_file;
	s_log_file = NULL;			
	fflush(tmp_file);
	fclose(tmp_file);

	/*if (quit_flag == 0) {
		file_info.file_type = FILE_TYPE_APP_LOG_FILE;
		file_info.file_size = GetFileSizeInByte(s_log_filename);
		snprintf(file_info.file_name, sizeof(file_info.file_name), "%s", s_log_filename);
		memcpy(msg_info.data, &file_info, sizeof(FileInfo));
		SendMsg(APP_COMMMGR_ID, MSG_ID_FILE_BACK_HAUL, &msg_info);
	}*/
}

FILE* OpenLogFile(bool new_flag)
{ 
	static int log_count = 0;

	s_log_byte_num = 0;
	if (new_flag == true)
	{
		log_count = 0;
	} else {
		log_count++;
	}

	CloseLogFile(0);

	sprintf(s_log_filename, "%s%s_%s_%03d.txt", s_log_cfg.log_dir, s_log_cfg.log_prefix, s_time_str, log_count);	
	s_log_file = fopen(s_log_filename, "wb+");	 
    if (s_log_file != NULL) {
		warn_printf("Open log file %s.\r\n", s_log_filename);
	}

	return s_log_file;
}

void CloseDebugFile(int quit_flag)
{
	int i = 0;
	int count = 0;
	FILE* tmp_file = NULL;
	//FileInfo file_info = {0};
	MsgInfo msg_info = {0};

	s_debug_byte_num = 0;
	count = s_debug_str_num;
	
	if (s_debug_file == NULL) {
		return;
	}

	//如果是缓存数据，则全部写入文件中，并释放内存
	s_debug_str_num = 0;
	if (s_debug_buff_flag == true)
	{
		for(i = 0; i < count; i++)
		{
			fprintf(s_debug_file,"%s", s_debug_str_arr[i]);
			free(s_debug_str_arr[i]);
			s_debug_str_arr[i] = NULL;
		}
	}
	
	warn_printf("Close debug file: %s.\r\n", s_debug_filename);
	
	tmp_file = s_debug_file;
	s_debug_file = NULL;		
	fflush(tmp_file);
	fclose(tmp_file);

	if (quit_flag == 0) {
		snprintf(msg_info.data, MAX_FILE_NAME_LEN, "%s", s_debug_filename);
		SendMsg(APP_PCRMGR_ID, MSG_ID_REPORT_LOG_FILE, &msg_info);
	}
}

FILE* OpenDebugFile(bool new_flag)
{
	time_t timep;
	struct tm *p = NULL;
	char time_str[128] = {0};
	static int count = 0;
	
	count++;
	timep = time(NULL);   
	p = localtime(&timep);
	sprintf(time_str, "%04d_%02d_%02d_%02d_%02d_%02d", 
		(1900 + p->tm_year), (p->tm_mon + 1), p->tm_mday, p->tm_hour, p->tm_min, p->tm_sec);

	s_debug_byte_num = 0;

	if (new_flag == 0) {
		CloseDebugFile(0);
	}
	
	sprintf(s_debug_filename, "%s%s_%s_%03d.txt", s_log_cfg.log_dir, s_log_cfg.debug_prefix, time_str, count);
	s_debug_file=fopen(s_debug_filename, "wb+");	 
    if (s_debug_file != NULL) {
		warn_printf("Open log file %s.\r\n", s_debug_filename);
	}
	
	s_debug_str_num = 0;

	return s_debug_file;
}

//处理调试信息输出消息
int HandleDebugOutMsg(MsgInfo *pMsg)      
{	
	char* data = NULL;
	if (pMsg == NULL) {
		return -1;
	}
	
	data = (pMsg->data);
	PrintDebugInfo(data);
	
	return 0;			
}

//处理日志信息输出消息
int HandleLogOutMsg(MsgInfo *pMsg)      
{	
	char* data = NULL;
	if (pMsg == NULL) {
		return -1;
	}

	data = (pMsg->data);
	PrintLogInfo(data);

	PrintDebugInfo("log -- ");
	PrintDebugInfo(data);
	
	return 0;			
}


void CheckFileSizeTimerHandler()
{
	if (s_log_byte_num > s_log_cfg.file_size)
	{
		OpenLogFile(false);		
	}

	//如果调试信息字节数统计超过5M，则保存一次新的调试文件，避免太大
	if (s_debug_byte_num > s_log_cfg.file_size)
	{
		OpenDebugFile(false);		
	}

	//延迟退出，保存所有调试信息
	if (close_delay_count > 0)
	{
		close_delay_count--;
		if (close_delay_count == 0)
		{
			s_quit_flag = true;
		}
	}
}

int InitLogFile()
{
	/*time_t timep;
	struct tm *p = NULL;

	timep = time(NULL);   
	p = localtime(&timep);
	sprintf(s_time_str, "%04d_%02d_%02d_%02d_%02d_%02d", 
		(1900 + p->tm_year), (p->tm_mon + 1), p->tm_mday, p->tm_hour, p->tm_min, p->tm_sec);

	OpenLogFile(true);
	if (NULL == s_log_file) {
		fprintf(stderr,"Open log file err.\r\n");
		return -1;
	}*/

	OpenDebugFile(true);	
	if (NULL == s_debug_file) {
		fprintf(stderr,"Open debug file err.\r\n");
		return -1;
	}
	return 0;
}

int DeinitLogFile()
{
	//CloseLogFile(1);
	CloseDebugFile(1);
	return 0;
}

static int SendHearbeatToRootDaemon() 
{
    int ret = 0;

	ret = SendSimpleMsg(APP_ROOTDAEMON_ID, APP_LOGGER_ID, MSG_ID_HEART);
    if (ret < 0) {
        err_printf("LoggerApp send heartbeat msg to RootDaemon error. \r\n");
    } else {
        debug_printf("LoggerApp send heartbeat msg to RootDaemon. \r\n");
    }

    return ret;
}

int SendReportLogFileMsg() {
	MsgInfo msg_info = {0};
	snprintf(msg_info.data, MAX_FILE_NAME_LEN, "%s", s_debug_filename);
	warn_printf("===Send MSG_ID_REPORT_LOG_FILE to PCR APP.\n");
	return SendMsg(APP_PCRMGR_ID, MSG_ID_REPORT_LOG_FILE, &msg_info);
}

void LoggerMsgHandle(int type, MsgInfo *msg)
{
	if (NULL == msg) {
		return;
	}

	debug_printf("debug_out receive msg type = %d, src = %d.\r\n", type, msg->header.src_app);		

	switch (type)
	{
		case MSG_ID_DEBUG_OUT:
			HandleDebugOutMsg(msg);
			break;

		case MSG_ID_LOG_OUT:
			HandleLogOutMsg(msg);
			break;

		case MSG_ID_TIMER_OUT:
			SendHearbeatToRootDaemon();
			CheckFileSizeTimerHandler();
			break;

		case MSG_ID_REPORT_LOG_FILE:
			SendReportLogFileMsg();
			break;
		
		default:
			debug_printf("debug_out receive msg type = %d, src = %d.\r\n", type, msg->header.src_app);		
			break;
	}

	return;
}

static void SendTimeOutMsg(void *data)
{
	int ret = 0;
    ret = SendSimpleMsg(APP_LOGGER_ID, APP_LOGGER_ID, MSG_ID_TIMER_OUT);
    if (ret < 0) {
		err_printf("send MSG_ID_TIMER_OUT error. \r\n");
    }
	return;	
}

int InitTimer()
{
	SetMillisecondsTimer(HEARTBEAT_TIME_LEN * 1000, SendTimeOutMsg, NULL);
	return 0;
}

void QuitProc(int signo) 
{
	debuge_printf("logger app will quit after 6s.\r\n");	
	close_delay_count = 2;

	if (s_file_mgr_timer_info != NULL) {
        s_file_mgr_timer_info->quit_flag = 1;
        pthread_join(s_file_mgr_thread_id, NULL);
        s_file_mgr_timer_info = NULL;
    }

	return; 
}

int main()
{    
    int ret = 0;
	
	LoadLogConfig();

	InitLoggerModule(LOGGER_INFO_E);

	ret = InitMsgQueModule(APP_LOGGER_ID);
    if (ret < 0) {
        fprintf(stderr,"InitMsgQue APP_LOGGER_ID err.\r\n");        
        return -1;
    }

	signal(SIGINT, QuitProc); 

	ret = InitLogFile();
    if (ret < 0) {
        fprintf(stderr,"InitLogFile err.\r\n"); 
        return -1;
    }

	InitTimer();

	StartMsgHandleLoop(LoggerMsgHandle, &s_quit_flag);

	DeinitLogFile();
    return 0;
}
