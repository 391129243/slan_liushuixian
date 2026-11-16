#include "ConfLocal.h"
#include "ConfSrv.h"
#include "Logger.h"
#include "Timer.h"
#include "Common.h"
#include "MsgWrapper.h"
#include "MsgHandler.h"
#include "CmdHandler.h"
#include "WssProtocol.h"
#include "CmdHandler.h"
#include "Wss.h"
#include "Uart.h"
#include "FileManager.h"

static AppInfo s_AppInfo = {0};
static int s_quit_flag = false;
static LocalCfg local_cfg = {0};
static SrvCfg svr_cfg = {0};
static WssCtx* wss_ctx = NULL;

int HandleQuitMsg(MsgInfo *msg);

static void MsgHandle(int type, MsgInfo *msg)
{
	if (NULL == msg) {
		return;
	}
	printf("%s receive msg type = %d, proc_id = %d.\r\n", 
		s_AppInfo.app_name, type, msg->header.src_app);		
	
	switch (type) {
		case MSG_ID_QUIT:
			HandleQuitMsg(msg);
			break;

		case MSG_ID_REPORT_LOG_FILE:
			HandleReportLogFileMsg(msg);
			break;

		case MSG_ID_UART_DISCONNECT:
			HandleUartDisConnectMsg(msg);
			break;

		case MSG_ID_UART_CONNECTED:
			HandleUartConnectedMsg(msg);
			break;

		case MSG_ID_APP_UPGRADE_STATUS:
			HandleUpgradeStatusMsg(msg);
			break;

		default:
			break;
	}

	return;
}

int HandleQuitMsg(MsgInfo *msg) {
	warn_printf("===Recv quit msg, %s quit.===\n", s_AppInfo.app_name);

	if (!s_quit_flag) {
		s_quit_flag = true;			
	}
	sleep(1);

	DelayMsgHandler(MsgHandle, 4);

	DeinitUartComm();
	DeInitWss(wss_ctx);
	DeinitFileManager();

	return 0;
}

static void QuitProc(int signo) 
{
	warn_printf("===Capture signal:%d, %s quit.===\n", signo, s_AppInfo.app_name);
	
	if (!s_quit_flag) {
		s_quit_flag = true;			
	}
	sleep(1);

	DelayMsgHandler(MsgHandle, 4);

	DeinitUartComm();
	DeInitWss(wss_ctx);
	DestroyLock();
	DeinitFileManager();
	warn_printf("%s exit.\n", s_AppInfo.app_name);
}

void WCHJTest(int signo) {
	FILE* fp = NULL;
	char data[100000] = {0};
	int len = 0;
	FILE* fp_json = NULL;

	fp = fopen("/config/wchjtest.json", "rb");
	if (fp == NULL) {
		err_printf("Open /config/wchjtest.json failed.\n");
		return;
	}

	len = fread(data, 1, sizeof(data), fp);
	if (len <= 0) {
		err_printf("Read file err.\n");
	} else {
		warn_printf("Read file len:%d.\n", len);
	}
	fclose(fp);

	WssDataHandler(data, len, WEBSOCKET_OP_TEXT);
}

int main(int argc, char* argv[])
{
	int ret = 0;
	char svr_url[MAX_URL_LEN] = {0};
    
	ret = ParseAppInfo(argc, argv, &s_AppInfo);
	if (ret < 0) {
        fprintf(stderr,"ParseAppInfo err.\r\n");    
		return -1;
	}

	ret = InitMsgQueModule(s_AppInfo.app_id);
    if (ret < 0) {
        fprintf(stderr,"InitMsgQue %d err.\r\n", s_AppInfo.app_id);
        return -1;
    }

	InitLoggerModule(s_AppInfo.debug_level);

	signal(SIGINT, QuitProc);

	signal(__SIGRTMIN+20, WCHJTest);

	ret = LoadLocalCfgFile(&local_cfg);
	if (ret < 0) {
		err_printf("Load local config file failed.\n");
		return -1;
	}
	SetLocalCfg(&local_cfg);
	SetLogCfg();

	ret = LoadSrvCfgFile(&svr_cfg);
	if (ret < 0) {
		err_printf("Load server config file failed.\n");
		return -1;
	}

	StartHeartBeatToSysDaemon(s_AppInfo.app_id);

	InitLock();
	snprintf(svr_url, sizeof(svr_url), "ws://%s:%d/websocket", svr_cfg.srv_ip, svr_cfg.port);
	ret = InitWss(&wss_ctx, svr_url, 0);
	if (ret < 0) {
		err_printf("Wss communication init err.\n");
		return -1;
	}
	WssDataHandlerReg(WssDataHandler);

	ret = InitUartComm(&local_cfg.uart_cfg, 1);
	if (ret < 0) {
		err_printf("Uart communication init err.\n");
		//return -1;
	}
	UartPacketHandlerReg(UartPacketHandler);
	SendHelloToSlave();
	
	snprintf(g_status.linux_status, sizeof(g_status.linux_status), "free");
	sleep(1);
	StartStatusRegularReport();

	ReportAutoStartLogFile();

	InitFileManager();

    ret = StartMsgHandleLoop(MsgHandle, &s_quit_flag);
    if (ret < 0) {
        err_printf("StartMsgHandleLoop err.\r\n");
        return -1;
    }

    log_printf("%s exit.\n", s_AppInfo.app_name);
    return 0;
}