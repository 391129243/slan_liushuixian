#ifndef __WORKFLOW_MGR_H__WRAPPER_H__
#define __WORKFLOW_MGR_H__WRAPPER_H__

#ifdef __cplusplus
extern "C" {
#endif

#include "cJSON.h"
#include "WssProtocol.h"

int InitWorkflowMgr(u_int32_t seq_id, char* template_name, cJSON* experiment_json);

int GetTemplateName(char* name);

u_int32_t GetSeqId();

cJSON* GetExperimentJson();

void SetPcrTempWaitFlag(u_int8_t wait_flag);

u_int8_t GetPcrTempWaitFlag();

void SetSectionsTotalTime(u_int32_t total_time);

u_int32_t GetSectionsTotalTime();

long GetTaskUsedTime();

void InitCmdQueue();

int CmdEnQueue(PcrSlaveCmd* cmd);

int GetCmdQueueFront(PcrSlaveCmd* cmd);

int GetCmdQueueSize();

int CmdDeQueue();

int SectionEnQueue(Section* section);

int GetSectionQueueSize();

int AddTemperatureDot(double temp);

int AddRawCurveDot(TupeData* tupe_list);

int SumTupe(TupeData* tupe_list);

int GetMeanTupe(TupeData* tupe_list);

void ClearTupe();

int AddRawMeltCurveDot(TupeData* tupe_list);

int SetTempPost(double* temp_list);

double GetCurrentTemp();
    
void DeinitWorkflowMgr();

#ifdef __cplusplus
};
#endif

#endif