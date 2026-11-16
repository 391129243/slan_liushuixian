#include <stdio.h>
#include <stdlib.h>
#include <sys/msg.h>

#include "Common.h"
#include "MsgQue.h"

#define FTOK_FNAME  "/home/mq1"
#define MAX_MSG_NUM 300

static int s_msgque_id[APP_TOTAL_NUM] = {0};
static int s_current_app_msgque = -1;

int GetMsgQueNum(int app_id)
{
	struct msqid_ds msqinfo;
    if (s_msgque_id[app_id] < 0) {
        return -1;
    }

	if (msgctl(s_msgque_id[app_id], IPC_STAT, &msqinfo) == 0) {
        fprintf(stdout, "appid = %d msgid = %d, msg_qnum:%ld.\r\n", app_id, s_msgque_id[app_id], msqinfo.msg_qnum);
		return msqinfo.msg_qnum;
	}

	return 0;
}

static int SetMsgQueSize(int msg_queue_id)
{
	int ret = -1;
	struct msqid_ds msqinfo;
	
    //注意需要root权限
	if (msgctl(msg_queue_id, IPC_STAT, &msqinfo) == 0) {
		if (msqinfo.msg_qbytes != (MAX_MSG_NUM * MAX_MSG_DATA_LEN)) {
			msqinfo.msg_qbytes = (MAX_MSG_NUM * MAX_MSG_DATA_LEN);
			if (msgctl(msg_queue_id, IPC_SET, &msqinfo) == 0) {				
				ret = 0;
			}
		}
		else {
			ret = 0;
		}
	}
	return ret;
}

void ClearMsgQue(int app_id)
{
	int ret = 0;
	MsgBuf msg;

	if (s_msgque_id[app_id] < 0) {
        return;
    }
	
	while(1) {
		ret = msgrcv(s_msgque_id[app_id], (void *)&msg, MAX_MSG_DATA_LEN, 0, IPC_NOWAIT);
		if(ret < 0) {
            break;
        }
	}
}

int CreateMsgQue(int id)
{
    key_t key;
    int msgque_id = 0;

    key = ftok(FTOK_FNAME, id);
    msgque_id = msgget(key, 0666|IPC_CREAT);
    if (msgque_id == -1) {
        fprintf(stderr, "queue init error.\r\n");
        return -1;
    }

    if (SetMsgQueSize(msgque_id) < 0) {
        fprintf(stderr, "aset msg queue size error.\r\n");
        return -1;
    }
    return msgque_id;
}

int InitMsgQue(int app_id)
{
	int i = 0;

    if (app_id < 0 || app_id >= APP_TOTAL_NUM) {
        fprintf(stderr,"error app id.\r\n");
        return -1;
    }

    for (; i < APP_TOTAL_NUM; i++) {
        s_msgque_id[i] = CreateMsgQue(i);
        //fprintf(stdout, "appid = %d msgid = %d.\r\n", i, s_msgque_id[i]);
    }

    s_current_app_msgque = s_msgque_id[app_id];
    ClearMsgQue(app_id);
	return 0;
}

int SendMsgQue(int dst_app_id, const MsgBuf *msg)
{
    if (dst_app_id < 0 || dst_app_id >= APP_TOTAL_NUM) {
        fprintf(stderr, "error app id.\r\n");
        return -1;
    }

    if (NULL == msg) {
        fprintf(stderr, "null msg pointer.\r\n");
        return -1;
    }

    int msgque_id = s_msgque_id[dst_app_id];
    //fprintf(stderr, "dst_app_id = %d send msg.\r\n", dst_app_id);

    return msgsnd(msgque_id, msg, MAX_MSG_DATA_LEN, IPC_NOWAIT);
}

int RecvMsgQue(MsgBuf *msg)
{
    if (s_current_app_msgque < 0) {
        fprintf(stderr, "current app msg queue < 0.\r\n");
        return -1;
    }
    
    if (NULL == msg) {
        fprintf(stderr, "null msg pointer.\r\n");
        return -1;
    }

    return msgrcv(s_current_app_msgque, (void *)msg, MAX_MSG_DATA_LEN, 0, IPC_NOWAIT);
}

int RecvMsgQueType(MsgBuf *msg, long int type)
{
    if (s_current_app_msgque < 0) {
        fprintf(stderr, "current app msg queue < 0.\r\n");
        return -1;
    }
    
    if (NULL == msg) {
        fprintf(stderr, "null msg pointer.\r\n");
        return -1;
    }

    return msgrcv(s_current_app_msgque, (void *)msg, MAX_MSG_DATA_LEN, type, 0);
}
