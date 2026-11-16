#ifndef __CONFIG_LOCAL_H__
#define __CONFIG_LOCAL_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "Common.h"

#define MAX_APP_NAME (40)

typedef struct _AppInfo {
	int app_id;
	int debug_level;
	char app_name[MAX_APP_NAME];
} AppInfo;

typedef struct sConfUart
{
	char dev_name[DEVICE_NAME_LEN];
	int baud_rate;
	int data_bits;
	int stop_bits;
	char parity[2];
} ConfUart;

typedef struct sLocalCfg {
	char dev_id[DEVICE_ID_LEN];
	char dev_type[DEVICE_ID_LEN];
	int position;
	ConfUart uart_cfg;
} LocalCfg;

int ParseAppInfo(int argc, char *argv[], AppInfo *app_info);

int LoadLocalCfgFile(LocalCfg *pCfg);

int SaveLocalCfgFile(LocalCfg *pCfg);

#ifdef __cplusplus
}
#endif

#endif