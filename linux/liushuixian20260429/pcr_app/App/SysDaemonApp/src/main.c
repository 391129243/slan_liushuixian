
#include "Logger.h"
#include "Common.h"
#include "Timer.h"
#include "MsgWrapper.h"
#include "System.h"
#include "cJSON.h"

#define PRINT_APP_MEM

typedef struct stProcInfo {
	int app_id;
	pid_t proc_pid;
    int heart_beat_count;               //心跳计数
    char path_name[MAX_FILE_NAME_LEN];  //完整路径
    char app_name[MAX_FILE_NAME_LEN];   //文件名
    char arg_info[MAX_FILE_NAME_LEN];   //参数字符串
    char alg_path[MAX_FILE_NAME_LEN];
    int old_mem_used;
    int new_mem_used;
	int debug_level;
} ProcInfo, *PProcInfo;

static int s_debug_level = 0;

/*static PCLAlgCfgData s_video_alg_cfg = {0};
static PCLAlgCfgData s_feature_alg_cfg = {0};
static PCLAlgRunStatData s_alg_status_data = {0};*/

int device_state = 1;
int state_update_time_count = 0;

ProcInfo proc_info_array[APP_TOTAL_NUM] = {0};

int s_quit_flag = false;
bool alg_redef_flag = false;  //软件定义算法重启标志

int SendHearbeatToRootmgt() 
{
    int ret = 0;

	ret = SendSimpleMsg(APP_ROOTDAEMON_ID, APP_SYSDAEMON_ID, MSG_ID_HEART);
    if (ret < 0) {
        err_printf("SysDaemon send heartbeat msg to RootDaemon error. \r\n");
    } else {
        debug_printf("SysDaemon send heartbeat msg to RootDaemon. \r\n");
    }

    return ret;
}

int SendQuitMsgToProc(int app_id) 
{
    int ret = 0;

    if (app_id >= APP_TOTAL_NUM || app_id <= APP_LOGGER_ID) {
		return -1;
	}

    if (proc_info_array[app_id].proc_pid == 0) {
        return -1;
    }

	ret = SendSimpleMsg(app_id, APP_SYSDAEMON_ID, MSG_ID_QUIT);
    if (ret < 0) {
        err_printf("send quit msg to app %d failed. \r\n", app_id);
    }

    return ret;
}

void SysDaemonSigchldHandle(int signo) 
{
    pid_t pid = 0;
    int stat = 0;

    warn_printf("SysDaemon  child proc: %d exit.\r\n", (int)pid);

    while (1) {
        pid = waitpid(-1, &stat, WNOHANG);
        if (!pid || pid < 0) {
            break;
        }

        warn_printf("SysDaemon child proc: %d exit.\r\n", (int)pid);

        //如果正在退出过程中，则不做处理
        if (s_quit_flag == true) {
            continue;
        }
	}
    return;
}

int StartBaseApp(char *path_name, char *app_name, char *argv,
					int debug_level, int app_id)
{
    pid_t pid;
    int ret = 0;
    char cmd_line[MAX_FILE_NAME_LEN] = {0};
	
    if ((app_name == NULL) || (path_name == NULL)) {
		return -1;
	}

    if (!argv || !strlen(argv)) {
	    sprintf(cmd_line, "%s %d %d", path_name, app_id, debug_level);
    } else {
	    sprintf(cmd_line, "%s %s %d %d", path_name, argv, app_id, debug_level);
    }

    if (access(path_name, W_OK) != 0) {
        err_printf("SysDaemon creat app %s proc error for not exist. quit! \r\n",
                   cmd_line);
        return -1;
    }

	char debug_args[10] = {0};
	char appid_args[10] = {0};

	sprintf(debug_args, "%d", debug_level);
	sprintf(appid_args, "%d", app_id);

    signal(SIGCHLD, &SysDaemonSigchldHandle);

    strcpy(proc_info_array[app_id].app_name, app_name);
    strcpy(proc_info_array[app_id].path_name, path_name);
    if (NULL != argv && strlen(argv)) {
        strcpy(proc_info_array[app_id].arg_info, argv);
    }
    proc_info_array[app_id].debug_level = debug_level;

    pid = fork();
    if (pid < 0) {
        err_printf("SysDaemon creat app %s proc error! quit! \r\n", cmd_line);
        return -1;
    }

    if (pid == 0) {
        MillisecondsSleep(50);
        if (!argv || !strlen(argv)) {
		    ret = execl(path_name, app_name, appid_args, debug_args, (char *)NULL);
        } else {
		    ret = execl(path_name, app_name, argv, appid_args, debug_args, (char *)NULL);
        }

        if (ret == -1) {
            err_printf("SysDaemon Start %s proc error! quit! \r\n", cmd_line);
            exit(0);
        } else {
            printf("SysDaemon Start %s proc success! \r\n", cmd_line);
        }
    } else {
        warn_printf("SysDaemon  creat child proc id %d, pid = %d, cmd = %s, success.\r\n", app_id, (int)pid, cmd_line);
        proc_info_array[app_id].proc_pid = pid;
        proc_info_array[app_id].heart_beat_count = 0;
        warn_printf("start app id %d : %s.", app_id, cmd_line);
        return 0;
    }
    return 0;
}

int StartApp(char *path_name, char *app_name, char *argv,
			int debug_level, int app_id, bool is_alg_app, char* alg_path) 
{
    int ret = 0;
    ret = StartBaseApp(path_name, app_name, argv, debug_level, app_id);

    return ret;
}

void StopAPPProc(int proc_id) 
{
    if (proc_id >= APP_TOTAL_NUM || proc_id <= APP_LOGGER_ID) {
		return;
	}

    if (proc_info_array[proc_id].proc_pid == 0) {
        return;
    }

    //发送退出消息
    SendQuitMsgToProc(proc_id);
    //等待50ms
    MillisecondsSleep(50);

    if (kill(proc_info_array[proc_id].proc_pid, SIGINT) != 0) {
        err_printf("SysDaemon Kill proc id %d, pid %d, name %s error! \r\n",
                   proc_id, proc_info_array[proc_id].proc_pid,
                   proc_info_array[proc_id].app_name);
    } else {
        warn_printf("SysDaemon Kill proc id %d, pid %d, name %s success! \r\n",
                   proc_id, proc_info_array[proc_id].proc_pid,
                   proc_info_array[proc_id].app_name);
    }

    //进程id和心跳计数清0
    proc_info_array[proc_id].proc_pid = 0;
    proc_info_array[proc_id].heart_beat_count = 0;
}

void RestartApp(int proc_id) {
    StopAPPProc(proc_id);
    bool is_alg_proc = false;

    StartApp(proc_info_array[proc_id].path_name,
                 proc_info_array[proc_id].app_name,
                 proc_info_array[proc_id].arg_info,
                 proc_info_array[proc_id].debug_level, proc_id, is_alg_proc, proc_info_array[proc_id].alg_path);
}

void StopChildApp() {
    int i = 0;
    int proc_pid = 0;

    for (i = APP_LOGGER_ID + 1; i < APP_TOTAL_NUM; i++) {
        proc_pid = proc_info_array[i].proc_pid;
        if (proc_pid > 0) {
            //进程id和心跳计数先清0
            proc_info_array[i].proc_pid = 0;
            proc_info_array[i].heart_beat_count = 0;
            if (kill(proc_pid, SIGINT) != 0) {
                err_printf(
                    "SysDaemon Kill proc id %d, pid %d, name %s error! \r\n", i,
                    proc_pid, proc_info_array[i].app_name);
            } else {
                warn_printf(
                    "SysDaemon Kill proc id %d, pid %d, name %s success! \r\n", i,
                    proc_pid, proc_info_array[i].app_name);
            }
        }
    }
}

void StartChildApp() 
{
    FILE *fp = NULL;
    int json_count = 0;
    char json_data[MAX_JSON_DATA_LEN] = {0};
    char *appName = NULL;
    cJSON *root_json = NULL;
    cJSON *local_app_json = NULL;
    cJSON *app_json = NULL;
    cJSON *child_json = NULL;
    int app_count = 0;
    int i = 0, id = 0;
    bool start_flag = true;
    int debug_flag = 0;
    char path_name[MAX_FILE_NAME_LEN] = {0};

    fp = fopen(LOCAL_APP_FILE, "r");
    if (fp == NULL) {
        err_printf("SysDaemon Read file %s is not exist. \r\n", LOCAL_APP_FILE);
        return;
    }

    memset(json_data, 0, MAX_JSON_DATA_LEN);
    json_count = fread(json_data, 1, MAX_JSON_DATA_LEN, fp);
    fclose(fp);

    debug_printf("SysDaemon Read file %s count = %d. \r\n", LOCAL_APP_FILE,
                 json_count);

    if ((json_count == 0) || (json_count >= MAX_JSON_DATA_LEN)) {
        err_printf("SysDaemon Read file %s err. \r\n", LOCAL_APP_FILE);
        return;
    }

    root_json = cJSON_Parse(json_data);
    if (root_json == NULL) {
        err_printf("SysDaemon Read file %s json root err. \r\n", LOCAL_APP_FILE);
        return;
    }

    local_app_json = cJSON_GetObjectItem(root_json, "local_app");
    if (local_app_json == NULL) {
        err_printf("SysDaemon Read file %s json local app err. \r\n",
                   LOCAL_APP_FILE);
        cJSON_Delete(root_json);
        return;
    }

    app_count = cJSON_GetArraySize(local_app_json);
    if (app_count == 0) {
        err_printf("SysDaemon Read file %s json no local app err. \r\n",
                   LOCAL_APP_FILE);
        cJSON_Delete(root_json);
        return;
    }

    //循环读取和启动所有local app
    for (i = 0; i < app_count; i++) {
        app_json = cJSON_GetArrayItem(local_app_json, i);
        if (app_json != NULL) {
            child_json = cJSON_GetObjectItem(app_json, "id");
            if (child_json == NULL) continue;
            id = child_json->valueint;

            child_json = cJSON_GetObjectItem(app_json, "app_name");
            if (child_json == NULL) continue;
            appName = child_json->valuestring;

            start_flag = true;
            child_json = cJSON_GetObjectItem(app_json, "start_flag");
            if (child_json != NULL) {
                start_flag = child_json->valueint;
            }

            debug_flag = -1;
            child_json = cJSON_GetObjectItem(app_json, "debug_level");
            if (child_json != NULL) {
                debug_flag = child_json->valueint;
            }

			if ((id == APP_SYSDAEMON_ID)) {
				s_debug_level = debug_flag;
			}

            if ((id == APP_SYSDAEMON_ID) || 
				(id == APP_ROOTDAEMON_ID) ||
				(id == APP_LOGGER_ID)) {
				continue;
			}

            //如果不用启动，则跳过
            if (start_flag == false) {
                warn_printf("SysDaemon skip start id %d app_name %s for start_flag = %d.\r\n", id, appName, start_flag);
                continue;
            }

            sprintf(path_name, "%s/%s", APP_PATH_NAME, appName);
            if (debug_flag < 0) {
                debuge_printf("SysDaemon start app %d, name = %s. \r\n", id,
                              path_name);
                StartApp(path_name, appName, NULL, LOGGER_NONE, id, false, NULL);
            } else {
                StartApp(path_name, appName, NULL, debug_flag, id, false, NULL);
            }
        }
    }
    cJSON_Delete(root_json);
}

int HandleHeartbeatMsg(MsgInfo *pMsg) 
{
    int app_id = 0;

    if (pMsg == NULL) {
		return -1;
	}

    app_id = pMsg->header.src_app;
    if (app_id < APP_TOTAL_NUM) {
		proc_info_array[app_id].heart_beat_count = 0;
	}
    //debuge_printf("SysDaemon receive hearbeat msg from app id %d. \r\n", app_id);
    return 0;
}

int HandleQuitMsg(MsgInfo *pMsg) 
{
    int i = 0;
    int ret = -1;
    int proc_id = 0;

    if (pMsg == NULL) {
		return -1;
	}

    proc_id = pMsg->header.src_app;
    if (proc_id != APP_ROOTDAEMON_ID) {
        err_printf("SysDaemon receive error quit msg from proc %d.\r\n", proc_id);
        return 0;
    }

    MsgInfo msg_info = {0};
    msg_info.header.sub_type = pMsg->header.sub_type;
    for (i = APP_LOGGER_ID + 1; i < APP_TOTAL_NUM; i++) {
        if (proc_info_array[i].proc_pid > 0) {
            ret = SendMsg(i, MSG_ID_QUIT, &msg_info);
            if (ret < 0) {
                err_printf("Send quit msg to app %d failed (%s).\r\n", i, strerror(errno));
            } else {
                warn_printf("Send quit msg to app %d success. \r\n", i);
            }
        }	
    }

    return ret;
}

void SysDaemonTimerHandler() 
{
    int i = 0;
    static int mem_count = 0;
    static int mem_time = 0;
    static int count = 0;
    int new_len = 0;

    count++;
    mem_count++;

    //定时向rootmgt进程发心跳消息
    SendHearbeatToRootmgt();

    for (i = APP_LOGGER_ID + 1; i < APP_TOTAL_NUM; i++) {
        //消息队列和进程都存在时，才进行心跳计数
        if (proc_info_array[i].proc_pid != 0) {
            proc_info_array[i].heart_beat_count++;
        } else {
            continue;
        }

#ifdef PRINT_APP_MEM
        //定时打印每个进程的内存占用，正常情况下关闭
        if ((mem_count == 1) && (proc_info_array[i].proc_pid != 0)) {
            proc_info_array[i].old_mem_used = proc_info_array[i].new_mem_used;
            proc_info_array[i].new_mem_used = GetProcMem(proc_info_array[i].proc_pid);
            new_len = proc_info_array[i].new_mem_used - proc_info_array[i].old_mem_used;
            warn_printf("[%03d]: %s %s mem_used = %dkBytes, mem_add = %dkBytes.\n",
                       mem_time, proc_info_array[i].app_name,
                       proc_info_array[i].arg_info,
                       proc_info_array[i].new_mem_used, new_len);
        }

        /*if ((mem_count) == 1 && (proc_info_array[i].proc_pid != 0)) {
            GetProcNet(proc_info_array[i].proc_pid, 30, proc_info_array[i].app_name);
        }*/
#endif

        //如果心跳计数超出范围，则清零，然后重启对应应用
        if (proc_info_array[i].heart_beat_count >= HEARTBEAT_TIMEOUT_COUNT) {
            //心跳计数先清0
            proc_info_array[i].heart_beat_count = 0;
            err_printf("SysDaemon kill proc %d for no heart beat.\r\n", i);
            err_printf("kill app id %d for no heart beat: %s.\n", i,
                       proc_info_array[i].app_name);
            //重启指定应用
            RestartApp(i);
        }
    }

#ifdef PRINT_APP_MEM
    if (mem_count >= 10) {
        warn_printf("---------------------------------------------------\n");
        mem_time++;
        mem_count = 0;
    }
#endif

}

static void SendRestartAppMsg()
{
	int ret = 0;
    ret = SendSimpleMsg(APP_ROOTDAEMON_ID, APP_SYSDAEMON_ID, MSG_ID_RESTART_APP);
    if (ret < 0) {
		err_printf("Send MSG_ID_RESTART_APP error. \r\n");
    }
	return;	
}

void SysDaemonMsgHandle(int type, MsgInfo *msg) 
{
    if (NULL == msg) {
        return;
    }

    debug_printf("SysDaemon receive msg type = %d, from proc = %d \r\n",
                 type, msg->header.src_app);

    switch (type) {
        case MSG_ID_HEART:
            HandleHeartbeatMsg(msg);
            break;

        case MSG_ID_QUIT:
            HandleQuitMsg(msg);
            _exit(0);

        case MSG_ID_TIMER_OUT:
            SysDaemonTimerHandler();
            break;

        default:
            break;
    }

    return;
}

static void SendTimeOutMsg(void *data)
{
	int ret = 0;
    ret = SendSimpleMsg(APP_SYSDAEMON_ID, APP_SYSDAEMON_ID, MSG_ID_TIMER_OUT);
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
    s_quit_flag = true;
    StopChildApp();
	//MillisecondsSleep(500);
    sleep(2);
    warn_printf("SysDaemon proc quit.\r\n");
    _exit(0);
}

int main() 
{
	int ret = 0;

	ret = InitMsgQueModule(APP_SYSDAEMON_ID);
    if (ret < 0) {
        fprintf(stderr,"InitMsgQue APP_SYSDAEMON_ID err.\r\n");        
        return -1;
    }

    signal(SIGINT, QuitProc);

    StartChildApp();

	InitLoggerModule(s_debug_level);

	InitTimer();

    warn_printf("SysDaemon start!\r\n");

    StartMsgHandleLoop(SysDaemonMsgHandle, &s_quit_flag);

    StopChildApp();

    return 0;
}
