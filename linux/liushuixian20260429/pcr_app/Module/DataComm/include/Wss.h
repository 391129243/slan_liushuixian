#ifndef __WEBSOCKET_CLIENT_H__
#define __WEBSOCKET_CLIENT_H__

#ifdef __cplusplus
extern "C"
{
#endif

#include <stdio.h>
#include <pthread.h>

#include "LoopQueue.h"
#include "mongoose.h"

typedef int (*WSDataHandlerFunc) (char *raw_data, size_t len, u_int8_t data_type);

typedef struct sWssCtx {
    struct mg_mgr ws_mgr;
    struct mg_connection* ws_con;
    struct mg_ws_message* ws_msg;
    char server_url[64];
    LoopQueue recv_queue;
    LoopQueue send_queue;
    pthread_mutex_t lock;
	pthread_t recv_thread_id;
    pthread_t conn_thread_id;
    WSDataHandlerFunc data_handler;
    int connect_status;
	bool quit_flag;
    bool enable_ssl;
    bool ws_init_flag;
} WssCtx;

typedef enum eWssStatus {
    WSS_STATUS_DISCONNECTED = 0,
    WSS_STATUS_CONNECTING,
    WSS_STATUS_CONNECTED
} WssStatus;

int InitWss(WssCtx* *ctx, const char* server_url, bool enable_ssl);

int DeInitWss(WssCtx *ctx);

int WssDataHandlerReg(WSDataHandlerFunc handler);

int WssSendData(const char* data, size_t data_len, uint8_t data_type);

#ifdef __cplusplus
}
#endif

#endif