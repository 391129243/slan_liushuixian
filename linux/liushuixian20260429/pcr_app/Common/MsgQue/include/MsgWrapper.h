#ifndef __APP_INTF_H__
#define __APP_INTF_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "Common.h"

#define MAX_DATA_LEN (1024 * 7)

typedef struct _MsgHeader {
    int  src_app;
    int  dst_app;
    int  data_len;
    int  handle_id;
    int  sub_type;
    short result;
} __attribute__ ((packed)) MsgHeader;

typedef struct _MsgInfo {
    MsgHeader header;
    char data[MAX_DATA_LEN];
} __attribute__ ((packed)) MsgInfo;

int InitMsgQueModule(int app_id);

int SendMsg(int dst_app, int msg_id, MsgInfo *msg);

int SendRawMsg(int dst_app, int msg_id, char *msg, size_t len);

int SendSimpleMsg(int dst_app, int src_app, int msg_id);

int StartMsgHandleLoop(void (* MsgHandler)(int type, MsgInfo *msg), int *exit_flag);

int StartHeartBeatToSysDaemon(int src_app_id);

//int StartMsgNumWatch(int src_app_id, int watch_app_id);

int DelayMsgHandler(void (* MsgHandler)(int type, MsgInfo *msg), int delay);

#ifdef __cplusplus
}
#endif

#endif