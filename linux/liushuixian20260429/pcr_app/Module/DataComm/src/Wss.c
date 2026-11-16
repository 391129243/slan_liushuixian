#include <pthread.h>

#include "Logger.h"
#include "mongoose.h"
#include "LoopQueue.h"
#include "cJSON.h"
#include "Wss.h"
#include "Timer.h"
#include "Common.h"

static WssCtx s_ws_ctx = {0};

static void WssSendMgr() {
    RawData *raw_data = NULL;
    if (s_ws_ctx.connect_status != WSS_STATUS_CONNECTED) {
        debuge_printf("WSS connect is not ready, waiting...\n");
        usleep(10000);
        return;
    }

    pthread_mutex_lock(&s_ws_ctx.lock);
    raw_data = (RawData*)OutQueue(&(s_ws_ctx.send_queue));
    pthread_mutex_unlock(&s_ws_ctx.lock);
    if (raw_data == NULL) {
        usleep(10000);
        return;
    }

    if (raw_data->data) {
        mg_ws_send(s_ws_ctx.ws_con, raw_data->data, raw_data->len, raw_data->data_type);
        free(raw_data->data);
    }    
    debugw_printf("======WssSendMgr send data ok, data_len:%ld, data_type:%d.\n", raw_data->len, raw_data->data_type);
    free(raw_data);
}

static void EventHandlerFunc(struct mg_connection *con, int evt, void *evt_data, void *func_data) {
    if (con == NULL) {
        err_printf("Wss EventHandlerFunc, mg_connection ptr is NULL.\n");
        return;
    }
    debug_printf("===Wss===evt:%d.\n", evt);
    switch (evt)
    {
    case MG_EV_ERROR:
    {
        pthread_mutex_lock(&s_ws_ctx.lock);
        s_ws_ctx.connect_status = WSS_STATUS_DISCONNECTED;
        pthread_mutex_unlock(&s_ws_ctx.lock);

        err_printf("Wss catch an err event, err msg:%s.\n", (char*)evt_data);
        break;
    }

    case MG_EV_WS_OPEN:
    {
        pthread_mutex_lock(&s_ws_ctx.lock);
        s_ws_ctx.connect_status = WSS_STATUS_CONNECTED;
        pthread_mutex_unlock(&s_ws_ctx.lock);
        warn_printf("Wss is connected success.\n");
        break;
    }

    case MG_EV_CONNECT:
    {
        pthread_mutex_lock(&s_ws_ctx.lock);
        s_ws_ctx.connect_status = WSS_STATUS_CONNECTED;
        pthread_mutex_unlock(&s_ws_ctx.lock);
        if (s_ws_ctx.enable_ssl) {
            struct mg_tls_opts opts = {};
            mg_tls_init(s_ws_ctx.ws_con, &opts);
        }
        break;
    }

    case MG_EV_WS_MSG:
    {
        if (evt_data == NULL) {
            err_printf("Err: wss evt_data is NULL.\n");
            break;
        }
        
        struct mg_ws_message *ws_msg = (struct mg_ws_message*)evt_data;
        RawData *raw_data = (RawData *)malloc(sizeof(RawData));
		if (raw_data == NULL) {
			err_printf("Malloc memory for RawData ptr failed, lost wss message data len = %lu.\n", ws_msg->data.len);
		} else {
            raw_data->data = (char*)malloc(ws_msg->data.len+1);
            if (raw_data->data == NULL) {
                err_printf("Malloc memory for raw data failed, lost wss message data len = %lu.\n", ws_msg->data.len);
                free(raw_data);
                break;
            }
            raw_data->data_type = ws_msg->flags & 15;
            raw_data->len = ws_msg->data.len;
            memcpy(raw_data->data, ws_msg->data.ptr, raw_data->len);

            int ret = InQueue(&(s_ws_ctx.recv_queue), (void*)raw_data);
            int que_len = GetQueueLen(&(s_ws_ctx.recv_queue));
            if (ret == FALSE) {
                err_printf("Lost wss message data len %lu for queue full len = %d.\n", raw_data->len, que_len);
                free(raw_data->data);
                free(raw_data);
            }
        }
        break;
    }

    case MG_EV_CLOSE:
    {
        pthread_mutex_lock(&s_ws_ctx.lock);
        s_ws_ctx.connect_status = WSS_STATUS_DISCONNECTED;
        if (s_ws_ctx.ws_init_flag) {
            mg_mgr_free(&s_ws_ctx.ws_mgr);
            s_ws_ctx.ws_init_flag = false;
        } 
        pthread_mutex_unlock(&s_ws_ctx.lock);
        break;
    }

    case MG_EV_POLL:
    {
        WssSendMgr();
        break;
    }
  
    }
}

static void* WssRecvMgr(void* arg) {
    RawData *raw_data = NULL;
    
    while (!s_ws_ctx.quit_flag) {
        if (s_ws_ctx.data_handler == NULL) {
            debuge_printf("Data handler is NULL, please register data handler function.\n");
            MillisecondsSleep(100);
            continue;
        }

        raw_data = (RawData*)OutQueue(&(s_ws_ctx.recv_queue));
        if (raw_data == NULL || raw_data->data == NULL) {
            MillisecondsSleep(100);
            continue;
        }

        s_ws_ctx.data_handler(raw_data->data, raw_data->len, raw_data->data_type);
        if (raw_data->data != NULL) {
            free(raw_data->data);
        }
        free(raw_data);
    }

    pthread_exit((void *)0);
}

static void* WssConnectMgr(void* arg) {
    int connect_status = WSS_STATUS_DISCONNECTED;
    int ret = 0;

    while (!s_ws_ctx.quit_flag) {
        pthread_mutex_lock(&s_ws_ctx.lock);
        connect_status = s_ws_ctx.connect_status;
        pthread_mutex_unlock(&s_ws_ctx.lock);
        
        debugw_printf("===Wss===connect_status:%d.\n", connect_status);
        if (connect_status != WSS_STATUS_CONNECTED) {
            pthread_mutex_lock(&s_ws_ctx.lock);
            if (!s_ws_ctx.ws_init_flag) {
                mg_mgr_init(&s_ws_ctx.ws_mgr);
            }
            s_ws_ctx.ws_init_flag = true;
            s_ws_ctx.ws_con = mg_ws_connect(&s_ws_ctx.ws_mgr, s_ws_ctx.server_url, EventHandlerFunc, &s_ws_ctx.connect_status, NULL, NULL);
            s_ws_ctx.connect_status = WSS_STATUS_CONNECTING;
            pthread_mutex_unlock(&s_ws_ctx.lock);
            if (s_ws_ctx.ws_con == NULL) {
                err_printf("Wss create connection to server:%s failed, sleep 10s.\n", s_ws_ctx.server_url);
                sleep(10);
                continue;
            } else {
                if (s_ws_ctx.recv_thread_id == 0) {
                    ret = pthread_create(&s_ws_ctx.recv_thread_id, NULL, WssRecvMgr, NULL);
                    if(ret != 0)
                    {
                        err_printf("Create Wss receive manager thread failed.\r\n");
                        if (s_ws_ctx.ws_init_flag) {
                            mg_mgr_free(&s_ws_ctx.ws_mgr);
                            s_ws_ctx.ws_init_flag = false;
                        }
                        continue;
                    } else {
                        pthread_detach(s_ws_ctx.recv_thread_id);
                    }
                }

                warn_printf("Wss create connection to server:%s.\n", s_ws_ctx.server_url);
            }
        }
        debugw_printf("===Wss===connect_status:%d.\n", s_ws_ctx.connect_status);
        while (s_ws_ctx.ws_con && s_ws_ctx.connect_status != WSS_STATUS_DISCONNECTED) {
            debug_printf("===Wss===connect_status:%d.\n", s_ws_ctx.connect_status);
            mg_mgr_poll(&s_ws_ctx.ws_mgr, 10);
        }
        debugw_printf("===Wss===connect_status:%d.\n", s_ws_ctx.connect_status);
        sleep(3);
    }

    return NULL;
}

int InitWss(WssCtx* *ctx, const char* server_url, bool enable_ssl) {
    if (server_url ==  NULL) {
        err_printf("Input err, server_url is NULL.\n");
        return -1;
    }

    memset(&s_ws_ctx, 0, sizeof(WssCtx));

    InitQueue(&(s_ws_ctx.recv_queue), MAX_DATA_QUEUE_LEN);
    InitQueue(&(s_ws_ctx.send_queue), MAX_DATA_QUEUE_LEN);
    pthread_mutex_init(&s_ws_ctx.lock, NULL);

    snprintf(s_ws_ctx.server_url, sizeof(s_ws_ctx.server_url), "%s", server_url);
    s_ws_ctx.enable_ssl = enable_ssl;

    int ret = pthread_create(&s_ws_ctx.conn_thread_id, NULL, WssConnectMgr, NULL);
    if(ret != 0)
    {
        err_printf("Create Wss client connect manager thread failed.\r\n");
        return -1;
    } else {
        pthread_detach(s_ws_ctx.conn_thread_id);
    }

    *ctx = &s_ws_ctx;
    return 0;
}

int DeInitWss(WssCtx *ctx) {
    if (ctx != &s_ws_ctx) {
        err_printf("Input err, WssCtx ptr is wrong.\n");
        return -1;
    }
    s_ws_ctx.quit_flag = true;

    pthread_join(s_ws_ctx.conn_thread_id, NULL);
	pthread_join(s_ws_ctx.recv_thread_id, NULL);

    DeinitQueue(&(s_ws_ctx.recv_queue));
    DeinitQueue(&(s_ws_ctx.send_queue));
 
    if (s_ws_ctx.ws_init_flag) {
        mg_mgr_free(&s_ws_ctx.ws_mgr);
        s_ws_ctx.ws_init_flag = false;
    }

    pthread_mutex_destroy(&s_ws_ctx.lock);
    ctx = NULL;
    return 0;
}

int WssDataHandlerReg( WSDataHandlerFunc handler) {
    if (handler == NULL) {
        err_printf("Input err, function handler ptr is NULL.\n");
        return -1;
    }

	s_ws_ctx.data_handler = handler;
	return 0;
}

int WssSendData(const char* data, size_t data_len, uint8_t data_type) {
    int ret = 0;
    int que_len = 0;
    RawData *raw_data = NULL;

    if (data == NULL) {
        err_printf("Input err, data ptr is NULL.\n");
        return -1;
    }

    if (s_ws_ctx.connect_status != WSS_STATUS_CONNECTED) {
        debuge_printf("WSS connect is not ready, drop data.\n");
        return -1;
    }

    if (data_type != WEBSOCKET_OP_TEXT && data_type != WEBSOCKET_OP_BINARY) {
        err_printf("Unsupported data_type:%d, only support text or binary data.\n", data_type);
        return -1;
    }
    
    que_len = GetQueueLen(&(s_ws_ctx.send_queue));
    if (que_len == MAX_DATA_QUEUE_LEN) {
        err_printf("Send wss data len %lu failed. Queue is full.\n", data_len, que_len);
        return -2;
    }

    raw_data = (RawData*)(malloc(sizeof(RawData)));
    if (raw_data == NULL) {
        err_printf("Malloc memory for WSRawData failed.\n");
        return -1;
    }

    raw_data->data = (char*)malloc(data_len);
    if (raw_data->data == NULL) {
        err_printf("Malloc memory for raw data failed, data_len:%d.\n", data_len);
        free(raw_data);
        return -1;
    }
    memcpy(raw_data->data, data, data_len);
    raw_data->len = data_len;
    raw_data->data_type = data_type;

    pthread_mutex_lock(&s_ws_ctx.lock);
    ret = InQueue(&(s_ws_ctx.send_queue), (void*)raw_data);
    pthread_mutex_unlock(&s_ws_ctx.lock);
    if (ret == FALSE) {
        err_printf("Send wss data len %lu failed. Queue len = %d.\n", raw_data->len, que_len);
        free(raw_data->data);
        free(raw_data);
        return -1;
    }
    
    return 0;
}
