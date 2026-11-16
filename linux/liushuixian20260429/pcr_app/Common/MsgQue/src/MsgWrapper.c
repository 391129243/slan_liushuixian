#include "Common.h"
#include "Timer.h"
#include "MsgQue.h"
#include "MsgWrapper.h"

typedef struct _Heartbeat {
	int src_app;
	int dst_app;
} stHeartBeatInfo;

typedef struct _MsgWatcher
{
	int src_app_id;
	int watch_app_id;
} stMsgWatcher;

//static int s_msg_watch_thread[APP_ALG_ID_END+1] = {0};

int InitMsgQueModule(int app_id)
{
	return InitMsgQue(app_id);
}

int SendMsg(int dst_app, int msg_id, MsgInfo *msg)
{
	int ret = 0;
	MsgBuf msg_buff = {0};

	if (NULL == msg) {
		printf("null msg pointer err.\r\n");
		return -1;
	}

	msg_buff.mtype = msg_id;
	memcpy(msg_buff.mtext, msg, sizeof(MsgInfo));
    ret = SendMsgQue(dst_app, &msg_buff);
	if(ret < 0) {
		printf("send msg %d to app %d error %d %s. \r\n", msg_id, dst_app,
				errno, strerror(errno));
	}
	return ret;	
}

int SendRawMsg(int dst_app, int msg_id, char *msg, size_t len)
{
	int ret = 0;
	MsgBuf msg_buff = {0};

	if (NULL == msg) {
		printf("null msg pointer err.\r\n");
		return -1;
	}

	msg_buff.mtype = msg_id;
	memcpy(msg_buff.mtext, msg, len);
    ret = SendMsgQue(dst_app, &msg_buff);
	if (ret < 0) {
		printf("send msg %d to app %d error %d %s. \r\n", msg_id, dst_app,
				errno, strerror(errno));
	}
	return ret;	
}

int SendSimpleMsg(int dst_app, int src_app, int msg_id)
{
	int ret = 0;
	MsgInfo msg = {0};
	msg.header.src_app = src_app;
	msg.header.handle_id = 0;
    ret = SendMsg(dst_app, msg_id, &msg);
	if(ret < 0) {
		printf("send msg %d to app %d error %d %s. \r\n", msg_id, dst_app,
				errno, strerror(errno));
	}
	return ret;	
}

static void SendHeatBeatMsg(void *data)
{
	int ret = 0;
	stHeartBeatInfo *info = (stHeartBeatInfo *)data;

	if (NULL == info) {
		printf("SendHeatBeatMsg error, null heart beat info pointer. \r\n");
		return;
	}

    ret = SendSimpleMsg(info->dst_app, info->src_app, MSG_ID_HEART);
    if (ret < 0) {
		printf("send MSG_ID_HEART error. \r\n");
    }
	return;	
}

int StartHeartBeat(int dst_app, int src_app)
{
	static stHeartBeatInfo heartbeat_info;
	heartbeat_info.src_app = src_app;
	heartbeat_info.dst_app = dst_app;
	SetMillisecondsTimer(HEARTBEAT_TIME_LEN * 1000, SendHeatBeatMsg, (void *)&heartbeat_info);
	return 0;
}

int StartHeartBeatToSysDaemon(int src_app)
{
	static stHeartBeatInfo heartbeat_info;
	heartbeat_info.src_app = src_app;
	heartbeat_info.dst_app = APP_SYSDAEMON_ID;
	SetMillisecondsTimer(HEARTBEAT_TIME_LEN * 1000, SendHeatBeatMsg, (void *)&heartbeat_info);
	return 0;
}

int StartMsgHandleLoop(void (* MsgHandler)(int type, MsgInfo *msg), int *exit_flag)
{
    int ret = 0;
    MsgBuf msg_buff;

	if (NULL == MsgHandler || NULL == exit_flag) {
		printf("null para pointer.\r\n");
		return -1;
	}

    while (1) {
		memset(msg_buff.mtext, 0, MAX_MSG_DATA_LEN);

		ret = RecvMsgQue((void *)&msg_buff);
		if(ret < 0) {
			//pid_t cur_pid = getpid();
            //printf("receive msg error= %d %s! sleep a little time, cur_pid:%d.\r\n", ret, strerror(errno), cur_pid);
            sleep(1);
        } else {
			MsgHandler(msg_buff.mtype, (MsgInfo *)msg_buff.mtext);
		}

		if ((*exit_flag) == true) {
			pid_t cur_pid = getpid();
			printf("===Quit MsgHandleLoop, exit flag true, cur_pid:%d.===\r\n", cur_pid);
			break; 
		}		
    }

    return 0;
}

int DelayMsgHandler(void (* MsgHandler)(int type, MsgInfo *msg), int delay) {
	int i = 0;
	int ret = 0;
    MsgBuf msg_buff;
	
	while (i < delay * 1000) {
		memset(msg_buff.mtext, 0, MAX_MSG_DATA_LEN);

		ret = RecvMsgQue((void *)&msg_buff);
		if(ret < 0) {
			pid_t cur_pid = getpid();
            usleep(50);
        } else {
			MsgHandler(msg_buff.mtype, (MsgInfo *)msg_buff.mtext);
		}	

		i += 50;
    }

	return 0;
}

/*static void* WatchMsgNum(void* arg) {
	if (arg == NULL) {
		return NULL;
	}
	stMsgWatcher* msg_watch_ptr = (stMsgWatcher*)arg;
	stMsgWatcher msg_watch = {0};
	MsgInfo msg = {0};
	memcpy(&msg_watch, msg_watch_ptr, sizeof(stMsgWatcher));
	free(msg_watch_ptr);

	int num = 0;
	int recover_flag = 0;
	msg.header.src_app = msg_watch.watch_app_id;
	printf("watch msg num thread. watch_app_id:%d.\n", msg_watch.watch_app_id);

	while (1) {
		num = GetMsgQueNum(msg_watch.watch_app_id);
		msg.header.result = num;
		if (num >= STOP_DIS_MSG_THRESHOLD_NUM) {
			recover_flag = 1;
			if (SendMsg(msg_watch.src_app_id, MSG_ID_STOP_DISTRIBUTE_FRAME, &msg) < 0) {
				printf("MsgQue num:%d of app_id:%d is reach stop_distribute_frame threshold:%d, but send MSG_ID_STOP_DISTRIBUTE_FRAME failed.\n",
							num, msg_watch.watch_app_id, STOP_DIS_MSG_THRESHOLD_NUM);
			} else {
				printf("MsgQue num:%d of app_id:%d is reach stop_distribute_frame threshold:%d, send MSG_ID_STOP_DISTRIBUTE_FRAME successfully.\n",
							num, msg_watch.watch_app_id, STOP_DIS_MSG_THRESHOLD_NUM);
			}
		} else if (num >= ADAPT_DROP_MSG_THRESHOLD_NUM) {
			recover_flag = 1;
			if (SendMsg(msg_watch.src_app_id, MSG_ID_ADAPT_DROP_FRAME, &msg) < 0) {
				printf("MsgQue num:%d of app_id:%d is reach adapt_drop_frame threshold:%d, but send MSG_ID_ADAPT_DROP_FRAME failed.\n",
							num, msg_watch.watch_app_id, ADAPT_DROP_MSG_THRESHOLD_NUM);
			} else {
				printf("MsgQue num:%d of app_id:%d is reach adapt_drop_frame threshold:%d, send MSG_ID_ADAPT_DROP_FRAME successfully.\n",
							num, msg_watch.watch_app_id, ADAPT_DROP_MSG_THRESHOLD_NUM);
			}
		} else if (recover_flag) {
			recover_flag = 0;
			if (SendMsg(msg_watch.src_app_id, MSG_ID_RECOVER_DISTRIBUTE, &msg) < 0) {
				printf("MsgQue num:%d of app_id:%d is decreased lowere than adapt_drop_frame threshold:%d, but send MSG_ID_RECOVER_DISTRIBUTE failed.\n",
							num, msg_watch.watch_app_id, ADAPT_DROP_MSG_THRESHOLD_NUM);
			} else {
				printf("MsgQue num:%d of app_id:%d is decreased lowere than  adapt_drop_frame threshold:%d, send MSG_ID_RECOVER_DISTRIBUTE successfully.\n",
							num, msg_watch.watch_app_id, ADAPT_DROP_MSG_THRESHOLD_NUM);
			}
		}

		sleep(1);
	}

	return NULL;
}

int StartMsgNumWatch(int src_app_id, int watch_app_id)
{
	if (s_msg_watch_thread[watch_app_id] != 0) {
		return 0;
	}

	stMsgWatcher* msg_watcher = malloc(sizeof(stMsgWatcher));
	if (msg_watcher == NULL) {
		return -1;
	}
	memset(msg_watcher, 0 , sizeof(stMsgWatcher));
	msg_watcher->src_app_id = src_app_id;
	msg_watcher->watch_app_id = watch_app_id;

	pthread_t watch_msg_num_thread_id;
	int ret = pthread_create(&watch_msg_num_thread_id, NULL, WatchMsgNum, msg_watcher);
    if(ret != 0)
	{
		printf("Create watch_msg_num thread failed.\r\n");	
		return -1; 
	} else {
		printf("Create watch_msg_num thread success.\r\n");
	}
	s_msg_watch_thread[watch_app_id] = watch_msg_num_thread_id;
	pthread_detach(watch_msg_num_thread_id);

	return 0;
}*/
