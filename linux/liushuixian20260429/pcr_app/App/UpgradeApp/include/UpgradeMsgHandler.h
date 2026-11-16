#ifndef __UPGRADE_MSG_HANDLER_H__
#define __UPGRADE_MSG_HANDLER_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "MsgWrapper.h"

int HandleAppUpgradeMsg(MsgInfo* msg_info);

int HandleQueryStartParamMsg(MsgInfo* msg_info);

#ifdef __cplusplus
}
#endif

#endif
