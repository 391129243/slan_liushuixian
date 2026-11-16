#include "UpgradeMsgHandler.h"
#include "Logger.h"
#include "Timer.h"
#include "Common.h"
#include "ConfLocal.h"

static AppInfo s_AppInfo = {0};
static int s_quit_flag = false;

static void MsgHandle(int type, MsgInfo *msg)
{
	if (NULL == msg) {
		return;
	}
	debug_printf("%s receive msg type = %d, proc_id = %d.\r\n", 
		s_AppInfo.app_name, type, msg->header.src_app);		
    
    switch (type)
    {
    case MSG_ID_APP_UPGRADE:
        HandleAppUpgradeMsg(msg);
        break;

    case MSG_ID_QUERY_START_PARAM:
        HandleQueryStartParamMsg(msg);
        break;

    case MSG_ID_QUIT:
        s_quit_flag = true;
        break;
    
    default:
        break;
    }

	return;
}

static void QuitProc(int signo) 
{
	warn_printf("===Capture signal:%d, %s quit.===\n", signo, s_AppInfo.app_name);
	if (!s_quit_flag) {
		s_quit_flag = true;
	}
	warn_printf("%s exit.\n", s_AppInfo.app_name);
}

int main(int argc, char* argv[])
{
	int ret = 0;
    
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

	StartHeartBeatToSysDaemon(s_AppInfo.app_id);

    SendSimpleMsg(APP_ROOTDAEMON_ID, APP_UPGRADE_ID, MSG_ID_QUERY_START_PARAM);

    ret = StartMsgHandleLoop(MsgHandle, &s_quit_flag);
    if (ret < 0) {
        err_printf("StartMsgHandleLoop err.\r\n");
        return -1;
    }

    warn_printf("%s exit.\n", s_AppInfo.app_name);
    return 0;
}