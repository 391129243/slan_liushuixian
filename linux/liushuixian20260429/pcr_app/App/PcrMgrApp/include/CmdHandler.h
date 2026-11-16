#ifndef __COMM_MGR_CMD_HANDLER_H__
#define __COMM_MGR_CMD_HANDLER_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "WssProtocol.h"

extern PCRStatus g_status;

void InitLock();

void DestroyLock();

int UartPacketHandler(unsigned char *packet, unsigned int len);

int WssDataHandler (char *data, size_t len, u_int8_t data_type);

int SendHelloToSlave();

int PCRStatusReport(PCRStatus* status);

void StartStatusRegularReport();

int UpgradeDoneStatusReport(u_int32_t seq_id, UpgradeDoneStatus* status);

int PCRDoneStatusReport(u_int32_t seq_id, PCRDoneStatus* status);

#ifdef __cplusplus
}
#endif

#endif