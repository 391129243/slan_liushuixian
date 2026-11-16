#ifndef __MSG_QUE_H__
#define __MSG_QUE_H__

#ifdef __cplusplus
extern "C" {
#endif

#define MAX_MSG_DATA_LEN (1024 * 8)

typedef struct MsgDataBuf{
    long int mtype;
    char mtext[MAX_MSG_DATA_LEN];
} MsgBuf;

int CreateMsgQue(int id);

void ClearMsgQue(int app_id);

int InitMsgQue(int app_id);

int SendMsgQue(int dst_app_id, const MsgBuf *msg);

int RecvMsgQue(MsgBuf *msg);

int RecvMsgQueType(MsgBuf *msg, long int type);

int GetMsgQueNum(int msg_queue_id);

#ifdef __cplusplus
}
#endif

#endif