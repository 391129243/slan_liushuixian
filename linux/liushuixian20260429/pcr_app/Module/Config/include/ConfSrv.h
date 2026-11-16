#ifndef __CONFIG_SRV_H__
#define __CONFIG_SRV_H__

#ifdef __cplusplus
extern "C" {
#endif

typedef struct sSrvCfg {
	char srv_ip[64]; //FPGA srv IP
	int port; 
	int heartbeat;
} SrvCfg;

int LoadSrvCfgFile(SrvCfg *pCfg);

int SaveSrvCfgFile(SrvCfg *pCfg);

#ifdef __cplusplus
}
#endif

#endif
