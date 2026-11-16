#ifndef __DATACOMM_UART_H__
#define __DATACOMM_UART_H__

#ifdef __cplusplus
extern "C"
{
#endif

#include <pthread.h>

#include "LoopQueue.h"
#include "ConfLocal.h"

#define MAX_UART_DATA_BUF_LEN (65535*2)
#define MAX_PACKET_LEN 256
#define RETRY_NUM 3
#define HEADER_FLAG 0x5E

typedef int (*PacketHandlerFunc) (unsigned char *packet, unsigned int len);

typedef struct sUartCtx
{
    int fd;
    bool sync;
    LoopQueue recv_queue;
    LoopQueue send_queue;
    pthread_mutex_t lock;
    unsigned char* data_buf;
    pthread_t parse_thread_id;
    pthread_t recv_thread_id;
    pthread_t send_thread_id;
    u_int16_t recv_data_len;
    u_int8_t inited;
    u_int8_t quit_flag;
    PacketHandlerFunc packet_handler;
} UartCtx;

#pragma pack(1)
typedef struct sUartHeader {
    u_int8_t frame_head;
    u_int8_t cmd_type;
    u_int8_t mask;
    u_int8_t data_len;
} UartHeader;
#pragma()

int InitUartComm(ConfUart* uart_cfg, bool sync);

int DeinitUartComm();

int UartSendAsync(unsigned char *data, unsigned int data_len);

int UartSendSync(unsigned char* data, unsigned int data_len);

int UartPacketHandlerReg(PacketHandlerFunc packer_handler);

int UartHeaderParse(unsigned char* data, unsigned int packet_len, UartHeader* header);

#ifdef __cplusplus
}
#endif

#endif
