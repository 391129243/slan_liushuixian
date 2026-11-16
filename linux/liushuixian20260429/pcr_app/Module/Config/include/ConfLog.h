#ifndef __CONFIG_LOG_H__
#define __CONFIG_LOG_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "Common.h"

typedef struct sLogCfg {
	char log_dir[100];
	char log_prefix[30];
	char debug_prefix[30];
	unsigned int file_size;
	int dbg_rm_days_before;
} LogCfg;

int LoadLogCfgFile(LogCfg *pCfg);

#ifdef __cplusplus
}
#endif

#endif