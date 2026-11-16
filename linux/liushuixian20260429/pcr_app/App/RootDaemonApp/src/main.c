#include "Common.h"
#include "Timer.h"
#include "Logger.h"
#include "MsgWrapper.h"
#include "System.h"
#include "ConfLocal.h"

static int s_quit_flag = 0;
static pid_t s_sysdaemon_pid = 0;
static pid_t s_logger_pid = 0;
static int s_heartbeat_count[2] = {0}; //SysDaemon心跳时间计数（s）
static StartParam s_start_param = {0};

static void RootDaemonSigchldHandle(int signo) 
{
    pid_t pid = 0;
    int stat = 0;

    err_printf("RootDaemon child proc: %d exit.\r\n", (int)pid);

    while (1) {
        pid = waitpid(-1, &stat, WNOHANG);
        if (!pid || pid < 0) {
            break;
        }

        err_printf("RootDaemon child proc: %d exit.\r\n", (int)pid);

        //如果正在退出过程中，则不做处理
        if (s_quit_flag == true) {
            continue;
        }
	}
    return;
}

static void StartLoggerApp()
{
	pid_t pid;

	signal(SIGCHLD, &RootDaemonSigchldHandle);

	pid = fork();
	if (pid < 0) {
		err_printf("RootDaemon creat logger proc error! quit! \r\n");
        return;
	}

	if (pid == 0) {
		sleep(1);
		if (execl(LOGGER_APP, LOGGER_APP, (char*)NULL) == -1) {
			err_printf("RootDaemon start debugout proc error! quit! \r\n");
			exit(0);
		}
	} else {
		warn_printf( "RootDaemon creat child proc: debugout %d sucess.\r\n", (int)pid);
		s_logger_pid = pid;
	}
}

static void StopLoggerApp()
{
	if (s_logger_pid <= 0) {
		return;
	}

	warn_printf("Stop LoggerApp.\r\n");

	if (kill(s_logger_pid, SIGINT) != 0) {
		err_printf("RootDaemon kill logger proc %d error! \r\n", (int)s_logger_pid);
	} else {
		warn_printf("RootDaemon kill logger proc %d success! \r\n", (int)s_logger_pid);
	}

	s_logger_pid = 0;
}

static void StartSysDaemonApp()
{
	pid_t pid;

	signal(SIGCHLD, &RootDaemonSigchldHandle);

	pid = fork();
	if (pid < 0) {
		err_printf("RootDaemon creat SysDaemonApp error! quit! \r\n");
		err_printf("Start SysDaemonApp failed.");
		return;
	}

	if (pid == 0) {
		sleep(1);
		if (execl(SYSDAEMON_APP, SYSDAEMON_APP, (char*)NULL) == -1) {
			err_printf("RootDaemon start SysDaemonApp proc error! quit! \r\n");
			err_printf("Start SysDaemonApp failed.");
			exit(0);
		}
	} else {
		warn_printf( "RootDaemon creat child proc: SysDaemon %d success.\r\n", (int)pid);
		s_sysdaemon_pid = pid;
	}

	warn_printf("Start SysDaemonApp.");
}

int SendQuitMsgToSysDaemon(int sub_type)
{
	int ret = 0;
	MsgInfo msg_info = {0};
	msg_info.header.sub_type = sub_type;
	ret = SendMsg(APP_SYSDAEMON_ID, MSG_ID_QUIT, &msg_info);
	if (ret < 0) {
		err_printf("RootDaemon send quit msg to SysDaemon error. \r\n");
	} else {
		warn_printf("RootDaemon send quit msg to SysDaemon success. \r\n");
	}
	return ret;	
}

static void StopSysDaemonApp()
{
	if (s_sysdaemon_pid <= 0) {
		return;
	}

	warn_printf("Stop SysDaemonApp.\r\n");

	//发送退出消息
	//SendQuitMsgToSysDaemon();
	//sleep(1);

	if (kill(s_sysdaemon_pid, SIGINT) != 0) {
		err_printf("RootDaemon kill SysDaemon proc %d error! \r\n", (int)s_sysdaemon_pid);
	} else {
		warn_printf("RootDaemon kill SysDaemon proc %d success! \r\n", (int)s_sysdaemon_pid);
	}

	s_sysdaemon_pid = 0;
}

static void SendTimeOutMsg(void *arg)
{
	int ret = 0;
	ret = SendSimpleMsg(APP_ROOTDAEMON_ID, APP_ROOTDAEMON_ID, MSG_ID_TIMER_OUT);
	if (ret < 0) {
		err_printf("RootDaemon send MSG_ID_TIMER_OUT error. \r\n");
	}
	return;	
}

int InitTimer()
{
	SetMillisecondsTimer(1000, SendTimeOutMsg, NULL);
	return 0;
}

void RestartSysDaemonApp()
{
	s_heartbeat_count[0] = 0;
	warn_printf("Prepare to restart SysDaemonApp.\n");
	StopSysDaemonApp(); //先关闭进程;
	sleep(1);
	StartSysDaemonApp(); //重新启动SysDaemon进程
}

void RestartLoggerApp()
{
	s_heartbeat_count[0] = 0;
	warn_printf("Prepare to restart LoggerApp.\n");
	StopLoggerApp(); //先关闭进程;
	sleep(1);
	StartLoggerApp(); //重新启动LoggerApp进程
}

void RootTimerHandler()
{
	s_heartbeat_count[0]++;
    s_heartbeat_count[1]++;

	if (s_heartbeat_count[0] >= (HEARTBEAT_TIME_LEN * HEARTBEAT_TIMEOUT_COUNT)) {
		err_printf("RootDaemon SysDaemon heartbeat timeout %d s. restart SysDaemon. \r\n", s_heartbeat_count[0]);
		err_printf("SysDaemonApp hearbeat timeout to restart.");
		RestartSysDaemonApp();
	}

    if (s_heartbeat_count[1] >= (HEARTBEAT_TIME_LEN * HEARTBEAT_TIMEOUT_COUNT)) {
		err_printf("RootDaemon LoggerApp heartbeat timeout %d s. restart LoggerApp. \r\n", s_heartbeat_count[1]);
		err_printf("LoggerApp hearbeat timeout to restart.\n");
		RestartLoggerApp();
	} 
}

void HeartBeatHandler(MsgInfo *msg) {
    if (msg == NULL) {
        return;
    }

    debug_printf("RootDaemon receive heartbeat msg for proc_id %d.\r\n", msg->header.src_app);

    if (msg->header.src_app == APP_SYSDAEMON_ID) {
        s_heartbeat_count[0] = 0;
    } else if (msg->header.src_app == APP_LOGGER_ID) {
        s_heartbeat_count[1] = 0;
    }
}

void SendStartParam(MsgInfo *msg) {
	if (msg == NULL) {
        return;
    }

	MsgInfo rsp_msg = {0};
	int ret = 0;
	rsp_msg.header.src_app = APP_ROOTDAEMON_ID;
	rsp_msg.header.dst_app = msg->header.src_app;
	rsp_msg.header.data_len = sizeof(StartParam);

	memcpy(rsp_msg.data, &s_start_param, sizeof(StartParam));
	ret = SendMsg(msg->header.src_app, MSG_ID_QUERY_START_PARAM, &rsp_msg);
	if (ret == 0) {
        warn_printf("Send MSG_ID_QUERY_START_PARAM to app_id:%d success.\n", msg->header.src_app);
    } else {
        err_printf("Send MSG_ID_QUERY_START_PARAM to app_id:%d failed.\n", msg->header.src_app);
    }
}

void RootDaemonMsgHandle(int type, MsgInfo *msg)
{
    if (NULL == msg) {
        return;
    }
	debug_printf("RootDaemon receive msg type = %d, proc_id = %d.\r\n", 
		type, msg->header.src_app);		
	
	switch (type) {
		case MSG_ID_HEART:
			HeartBeatHandler(msg);			
			break;
		
		case MSG_ID_RESTART_APP:
			RestartSysDaemonApp();
			break;

		case MSG_ID_TIMER_OUT:
			RootTimerHandler();
			break;

		case MSG_ID_QUERY_START_PARAM:
			SendStartParam(msg);
			break;

		case MSG_ID_QUIT:
			SendQuitMsgToSysDaemon(msg->header.sub_type);
			StopLoggerApp();
			_exit(0);
			
		default:
			break;
	}

	return;
}

void QuitProc(int signo) 
{
	warn_printf("Capture signal:%d, RootDaemon exit.\n", signo);
	s_quit_flag = true;
}

void UpdateVersionInfo()
{
	char cmd[300] = {0};
	char version_file[] = "app_version";
	snprintf(cmd, sizeof(cmd), "rm -rf ./%s && echo %s > ./%s", version_file, SOFTWARE_VERSION, version_file);
	system(cmd);
}

int ParseArgs(int argc, char *argv[]) {
	if (argc < 3) {
        fprintf(stderr,"argv error.\r\n");    
		return -1;
	}

    if (NULL == argv) {
        fprintf(stderr,"null pointer error.\r\n");    
		return -1;
    }
	
	if (NULL != argv[1]) {
		snprintf(s_start_param.app_run, sizeof(s_start_param.app_run), "%s", argv[1]);
	}
	
	if (NULL != argv[2]) {
		snprintf(s_start_param.app_bak, sizeof(s_start_param.app_bak), "%s", argv[2]);
	}

	return 0;
}

int main(int argc, char* argv[])
{
	int ret = 0;

	snprintf(s_start_param.app_run, sizeof(s_start_param.app_run), "%s", PCR_APP_RUN_PATH);
	snprintf(s_start_param.app_bak, sizeof(s_start_param.app_bak), "%s", PCR_APP_BAK_PATH);
	ParseArgs(argc, argv);
    
	ret = InitMsgQueModule(APP_ROOTDAEMON_ID);
    if (ret < 0) {
        fprintf(stderr,"InitMsgQue APP_ROOTDAEMON_ID err.\r\n");        
        return -1;
    }

	signal(SIGINT, QuitProc);

	StartLoggerApp();
	
	sleep(3);
	
	InitLoggerModule(LOGGER_INFO_E);

	StartSysDaemonApp();

	InitTimer();

    warn_printf("RootDaemon start software version %s.\n", SOFTWARE_VERSION);
	UpdateVersionInfo();

    StartMsgHandleLoop(RootDaemonMsgHandle, &s_quit_flag);
	
	SendSimpleMsg(APP_LOGGER_ID, APP_ROOTDAEMON_ID, MSG_ID_REPORT_LOG_FILE);
	StopSysDaemonApp();
	StopLoggerApp();

	warn_printf("RootDaemon exit.\n");
    return 0;
}
