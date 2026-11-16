#ifndef __UART_PROTOCOL_H__
#define __UART_PROTOCOL_H__

#ifdef __cplusplus
extern "C"
{
#endif

#include <sys/types.h>
#include "Common.h"

#define FLUORESCENCE_FAM "FAM"
#define FLUORESCENCE_CY5 "CY5"
#define FLUORESCENCE_HEX "HEX"
#define FLUORESCENCE_ROX "ROX"

#define SLAVE_CMD_FUNCID_RSP_FLAG 0x7F
#define SLAVE_CMD_FUNCID_HELLO 0x01
#define SLAVE_CMD_FUNCID_SET_TEMPERATURE 0x06
#define SLAVE_CMD_FUNCID_MOTOR_CTRL 0x07
#define SLAVE_CMD_FUNCID_GET_TEMPERATURE 0x09
#define SLAVE_CMD_FUNCID_SET_CAP 0x0A
#define SLAVE_CMD_FUNCID_GET_PCR_DATA 0x0C
#define SLAVE_CMD_FUNCID_SET_LED 0x0F
#define SLAVE_CMD_FUNCID_GET_PCR_TEMPERATURE 0x43

#define SLAVE_LED_SET_BRIGHT_FLAG 1
#define SLAVE_LED_SET_BLINK_FLAG 2
#define SLAVE_MOTOR_RESET_FLAG 0x13
#define SLAVE_MOTOR_SCAN_FLAG 0x15

#define PCR_CHANNEL_NUM 8
#define TEMP_QUERY_TIME 3
#define HOLD_TIME_TIMER 2
#define SLAVE_CMD_PCR_TIMEOUT 10 //time unit:s
#define HEARTBEAT_INTERVAL 3*1000 //time unit:ms

typedef struct sMotorCtrlParam {
    u_int8_t ctrl_flag;
    u_int8_t pulse_h;
    u_int8_t pulse_l;
    u_int8_t direct;
    u_int8_t speed_h;
    u_int8_t speed_l;
} MotorCtrlParam;

typedef struct sTupeData
{
    int num;
    double temp_pre;
    double temp_post;
    double fluorescence_FAM;
    double fluorescence_CY5;
    double fluorescence_HEX;
    double fluorescence_ROX;
} TupeData;

typedef struct sTupeTempData
{
    int num;
    double temp;
} TupeTempData;

typedef struct sPcrStatus
{
    int heartbeat_cnt;
    int heartbeat_cnt_reconnect;
    float hotlid_temp;
    u_int8_t is_hotlid_temp_reach;
    TupeData tupe_data[PCR_CHANNEL_NUM];
    float temp_1;
    float temp_2;
    float temp_3;
    TupeTempData tupe_temp_data[PCR_CHANNEL_NUM];
    u_int8_t is_pcr_temp_reach;
} PcrStatus;

u_int8_t GenCRCCheck(u_int8_t* data, u_int16_t data_len);

//pack protocol
u_int8_t* PackSlaveCmdHello(u_int8_t* data_len);

u_int8_t* PackSlaveCmdSetTemp(u_int8_t* temp, u_int8_t* data_len);

u_int8_t* PackSlaveCmdMotorCtrl(MotorCtrlParam* param, u_int8_t* data_len);

u_int8_t* PackSlaveCmdGetTemp(u_int8_t* temp_flag, u_int8_t* data_len);

u_int8_t* PackSlaveCmdSetCap(u_int16_t* temp, u_int8_t* data_len);

u_int8_t* PackSlaveCmdGetPcrData(u_int8_t* data_len);

u_int8_t* PackSlaveCmdSetLed(u_int8_t* set_flag, u_int8_t* data_len);

u_int8_t* PackSlaveCmdGetPcrTemp(u_int8_t* data_len);

#ifdef __cplusplus
}
#endif

#endif