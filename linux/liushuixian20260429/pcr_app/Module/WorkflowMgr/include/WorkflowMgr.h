#ifndef __WORKFLOW_MGR_H__
#define __WORKFLOW_MGR_H__

#include <sys/time.h>
#include <map>
#include <vector>
#include <string>
#include <set>

#include "cJSON.h"
#include "WssProtocol.h"
#include "UartProtocol.h"

using namespace std;

class WorkflowMgr {
public:
    static WorkflowMgr& GetInstance();

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

    void CleanUp();
    
    void DeinitWorkflowMgr();
private:
    static WorkflowMgr* instance;
    WorkflowMgr() {};
    ~WorkflowMgr();

    u_int32_t seq_id_;
    string template_name_;
    cJSON* experiment_json_;
    u_int16_t section_num_;
    u_int32_t total_step_;

    double temp_pre_;
    double temp_;
    u_int8_t is_pcr_temp_waiting_;

    u_int32_t total_time_;
    timeval start_time_;

    TupeData tupe_sum_[PCR_CHANNEL_NUM];
    long raw_curve_num_;
    double temp_post_[PCR_CHANNEL_NUM];

    vector<Section> section_que_;
    vector<PcrSlaveCmd> cmd_que_;
};


#endif
