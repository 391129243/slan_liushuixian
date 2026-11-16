#include "UartProtocol.h"
#include "Logger.h"
#include "Uart.h"

u_int8_t GenCRCCheck(u_int8_t* data, u_int16_t data_len) {
    u_int8_t check_sum = 0;
    int i = 0;

    if (data == NULL || data_len == 0) {
        err_printf("Input err, data ptr is NULL or data_len is 0.\n");
        return 0;
    }

    for (i = 0; i < data_len; i++) {
        check_sum += data[i];
    }

    check_sum = check_sum & 0xFF;
    return check_sum;
}

u_int8_t* UartPacketWrapper(u_int8_t func_id, u_int8_t* data, u_int16_t data_len) {
    UartHeader header = {0};
    u_int16_t packet_len = 0;
    u_int8_t crc = 0;

    if ((data && data_len == 0) || (data == NULL && data_len > 0)) {
        err_printf("Input err, data ptr and data_len is conflict.\n");
        return NULL;
    }

    header.frame_head = HEADER_FLAG;
    header.cmd_type = func_id;
    header.data_len = sizeof(UartHeader) + data_len + 1;
    header.mask = 0x00;

    u_int8_t* packet = malloc(header.data_len);
    if (packet == NULL) {
        err_printf("Malloc mem for packet err.\n");
        return NULL;
    }
    
    memset(packet, 0, header.data_len);
    memcpy(packet, &header, sizeof(UartHeader));
    if (data && data_len > 0) {
        memcpy(packet + sizeof(UartHeader), data, data_len);
    }
    crc = GenCRCCheck(packet, header.data_len-1);
    memcpy(packet + sizeof(UartHeader) + data_len, &crc, sizeof(crc));

    return packet;
}

//pack protocol
u_int8_t* PackSlaveCmdHello(u_int8_t* data_len) {
    *data_len = sizeof(UartHeader) + 1;
    return UartPacketWrapper(SLAVE_CMD_FUNCID_HELLO, NULL, 0);
}

u_int8_t* PackSlaveCmdSetTemp(u_int8_t* temp, u_int8_t* data_len) {
    *data_len = sizeof(UartHeader) + 9 + 1;
    return UartPacketWrapper(SLAVE_CMD_FUNCID_SET_TEMPERATURE, temp, 9);
}

u_int8_t* PackSlaveCmdMotorCtrl(MotorCtrlParam* param, u_int8_t* data_len) {
    if (param->ctrl_flag == SLAVE_MOTOR_RESET_FLAG) {
        *data_len = sizeof(UartHeader) + 1 + 1;
        return UartPacketWrapper(SLAVE_CMD_FUNCID_MOTOR_CTRL, &param->ctrl_flag, 1);
    } else if (param->ctrl_flag == SLAVE_MOTOR_SCAN_FLAG) {
        *data_len = sizeof(UartHeader) + sizeof(MotorCtrlParam) + 1;
        return UartPacketWrapper(SLAVE_CMD_FUNCID_MOTOR_CTRL, param, sizeof(MotorCtrlParam));
    }    
}

u_int8_t* PackSlaveCmdGetTemp(u_int8_t* temp_flag, u_int8_t* data_len) {
    *data_len = sizeof(UartHeader) + 1 + 1;
    return UartPacketWrapper(SLAVE_CMD_FUNCID_GET_TEMPERATURE, temp_flag, 1);
}

u_int8_t* PackSlaveCmdSetCap(u_int16_t* temp, u_int8_t* data_len) {
    *data_len = sizeof(UartHeader) + 2 + 1;
    return UartPacketWrapper(SLAVE_CMD_FUNCID_SET_CAP, temp, 2);
}

u_int8_t* PackSlaveCmdGetPcrData(u_int8_t* data_len) {
    u_int8_t flag[2] = {0};
    flag[0] = 0x01;
    flag[1] = 0x01;
    *data_len = sizeof(UartHeader) + 2 + 1;
    return UartPacketWrapper(SLAVE_CMD_FUNCID_GET_PCR_DATA, &flag, 2);
}

u_int8_t* PackSlaveCmdSetLed(u_int8_t* set_flag, u_int8_t* data_len) {
    *data_len = sizeof(UartHeader) + 1 + 1;
    return UartPacketWrapper(SLAVE_CMD_FUNCID_SET_LED, set_flag, 1);
}

u_int8_t* PackSlaveCmdGetPcrTemp(u_int8_t* data_len) {
    u_int8_t flag = 0x00;
    *data_len = sizeof(UartHeader) + 1 + 1;
    return UartPacketWrapper(SLAVE_CMD_FUNCID_GET_PCR_TEMPERATURE, &flag, 1);
}
