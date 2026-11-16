#ifndef __MGR_MSG_HANDLER_H__
#define __MGR_MSG_HANDLER_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "MsgWrapper.h"

int HandleReportLogFileMsg(MsgInfo* msg_info);

int ReportAutoStartLogFile();

int HandleUpgradeStatusMsg(MsgInfo* msg_info);

int HandleUartDisConnectMsg(MsgInfo* msg_info);

int HandleUartConnectedMsg(MsgInfo* msg_info);

#ifdef __cplusplus
}
#endif

#endif