#include "WorkflowMgr.h"
#include "WorkflowMgrWrapper.h"

int InitWorkflowMgr(u_int32_t seq_id, char* template_name, cJSON* experiment_json) {
    return WorkflowMgr::GetInstance().InitWorkflowMgr(seq_id, template_name, experiment_json);
}

int GetTemplateName(char* name) {
    return WorkflowMgr::GetInstance().GetTemplateName(name);
}

u_int32_t GetSeqId() {
    return WorkflowMgr::GetInstance().GetSeqId();
}

cJSON* GetExperimentJson() {
    return WorkflowMgr::GetInstance().GetExperimentJson();
}

void SetPcrTempWaitFlag(u_int8_t wait_flag) {
    WorkflowMgr::GetInstance().SetPcrTempWaitFlag(wait_flag);
}

u_int8_t GetPcrTempWaitFlag() {
    return WorkflowMgr::GetInstance().GetPcrTempWaitFlag();
}

void SetSectionsTotalTime(u_int32_t total_time) {
    WorkflowMgr::GetInstance().SetSectionsTotalTime(total_time);
}

u_int32_t GetSectionsTotalTime() {
    return WorkflowMgr::GetInstance().GetSectionsTotalTime();
}

long GetTaskUsedTime() {
    return WorkflowMgr::GetInstance().GetTaskUsedTime();
}

void InitCmdQueue() {
    WorkflowMgr::GetInstance().InitCmdQueue();
}

int CmdEnQueue(PcrSlaveCmd* cmd) {
    return WorkflowMgr::GetInstance().CmdEnQueue(cmd);
}

int GetCmdQueueFront(PcrSlaveCmd* cmd) {
    return WorkflowMgr::GetInstance().GetCmdQueueFront(cmd);
}

int GetCmdQueueSize() {
    return WorkflowMgr::GetInstance().GetCmdQueueSize();
}

int CmdDeQueue() {
    return WorkflowMgr::GetInstance().CmdDeQueue();
}

int SectionEnQueue(Section* section) {
    return WorkflowMgr::GetInstance().SectionEnQueue(section);
}

int GetSectionQueueSize() {
    return WorkflowMgr::GetInstance().GetSectionQueueSize();
}

int AddTemperatureDot(double temp) {
    return WorkflowMgr::GetInstance().AddTemperatureDot(temp);
}

int AddRawCurveDot(TupeData* tupe_list) {
    return WorkflowMgr::GetInstance().AddRawCurveDot(tupe_list);
}

int SumTupe(TupeData* tupe_list) {
    return WorkflowMgr::GetInstance().SumTupe(tupe_list);
}

int GetMeanTupe(TupeData* tupe_list) {
    return WorkflowMgr::GetInstance().GetMeanTupe(tupe_list);
}

void ClearTupe() {
    WorkflowMgr::GetInstance().ClearTupe();
}

int AddRawMeltCurveDot(TupeData* tupe_list) {
    return WorkflowMgr::GetInstance().AddRawMeltCurveDot(tupe_list);
}

int SetTempPost(double* temp_list) {
    return WorkflowMgr::GetInstance().SetTempPost(temp_list);
}

double GetCurrentTemp() {
    return WorkflowMgr::GetInstance().GetCurrentTemp();
}
    
void DeinitWorkflowMgr() {
    WorkflowMgr::GetInstance().DeinitWorkflowMgr();
}