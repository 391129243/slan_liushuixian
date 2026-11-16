#include <vector>
#include <string>
#include <sstream>
#include <time.h>

#include "WorkflowMgr.h"
#include "Logger.h"
#include "cJSON.h"

using namespace std;

WorkflowMgr* WorkflowMgr::instance = NULL;

WorkflowMgr& WorkflowMgr::GetInstance() {
    if (instance == NULL) {
        instance = new WorkflowMgr();
    }

    return *instance;
}

int WorkflowMgr::InitWorkflowMgr(u_int32_t seq_id, char* template_name, cJSON* experiment_json) {
    vector<string>substrs;

    if (template_name == NULL) {
        err_printf("Input err, template name is NULL.\n");
        return -1;
    }

    seq_id_ = seq_id;
    template_name_.assign(template_name);
    experiment_json_ = experiment_json;
    gettimeofday(&start_time_, NULL);
    
    section_num_ = 0;
    total_step_ = 0;
    temp_pre_ = 0.0;
    temp_ = 0;
    is_pcr_temp_waiting_ = 0;
    total_time_ = 0;
    
    raw_curve_num_ = 0;
    memset(tupe_sum_, 0, sizeof(TupeData)*PCR_CHANNEL_NUM);
    memset(temp_post_, 0, sizeof(double)*PCR_CHANNEL_NUM);

    return 0;
}

int  WorkflowMgr::GetTemplateName(char* name) {
    if (name == NULL) {
        err_printf("Input err, sub name ptr is NULL.\n");
        return -1;
    }
    
    snprintf(name, MAX_TEMPLATE_NAME_LEN, "%s", template_name_.c_str());
    return 0;
}

u_int32_t WorkflowMgr::GetSeqId() {
    return seq_id_;
}

cJSON* WorkflowMgr::GetExperimentJson() {
    return experiment_json_;
}

void WorkflowMgr::SetPcrTempWaitFlag(u_int8_t wait_flag) {
    is_pcr_temp_waiting_ = wait_flag;
}

u_int8_t WorkflowMgr::GetPcrTempWaitFlag() {
    return is_pcr_temp_waiting_;
}

/*void WorkflowMgr::SetSectionsTotalTime() {
    int time = 0;
    double last_temp = 35.0;
    vector<Section>::iterator iter = section_que_.begin();
    int k = 0;

    for (; iter != section_que_.end(); iter++) {
        if (iter->section_type == SECTION_TYPE_AMP) {
            int section_amp_time = 0;
            for (k = 0; k < iter->amp_section.step_num; k++) {
                double current_temp = iter->amp_section.steps[k].temp;
                if (current_temp > last_temp) {
                    section_amp_time += (int)((current_temp - last_temp) / 4);
                } else if (last_temp > current_temp) {
                    section_amp_time += (int)((last_temp - current_temp) / 4);
                }
                last_temp = current_temp;
                section_amp_time += iter->amp_section.steps[k].hold_time;
            }
            time += ((section_amp_time + 8) * iter->amp_section.cycles);
        } else if (iter->section_type == SECTION_TYPE_MELT) {
            int section_melt_time = (int)((iter->melt_section.end_temp - iter->melt_section.start_temp) / iter->melt_section.rate);
            time += (section_melt_time + 20);
            last_temp = iter->melt_section.end_temp;
        }
    }
    
    total_time_ = time;
    warn_printf("Sections total_time:%ld.\n", total_time_);
}*/

void WorkflowMgr::SetSectionsTotalTime(u_int32_t total_time) {
    total_time_ = total_time;
}

u_int32_t WorkflowMgr::GetSectionsTotalTime() {
    return total_time_;
}

long WorkflowMgr::GetTaskUsedTime() {
    timeval t;
    long used_time = 0;

    if (template_name_.empty()) {
        return 0;
    }

    gettimeofday(&t, NULL);
    used_time = (t.tv_sec - start_time_.tv_sec) + (t.tv_usec - start_time_.tv_usec) / 1000000;
    return used_time;
}

void WorkflowMgr::InitCmdQueue() {
    int i = 0;
    int j = 0;
    int k = 0;
    int ret = 0;

    if (section_num_ == 0) {
        err_printf("Section num is 0, cannot init cmd queue.\n");
        return;
    }

    warn_printf("Section num:%d.\n", section_num_);

    for (i = 0; i < section_num_; i++) {
        if (section_que_[i].section_type == SECTION_TYPE_AMP) {
            for (j = 0; j < section_que_[i].amp_section.cycles; j++) {
                for (k = 0; k < section_que_[i].amp_section.step_num; k++) {
                    PcrSlaveCmd cmd = {0};
                    snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_PCR_SET_TEMP_AMP);
                    snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_PCR_SET_TEMP_AMP);
                    snprintf(cmd.section_name, sizeof(cmd.section_name), "%s", section_que_[i].amp_section.name);
                    cmd.retry_cnt = RETYRY_NUM;
                    cmd.total_cycle = section_que_[i].amp_section.cycles;
                    cmd.loop = 1;
                    cmd.time = section_que_[i].amp_section.steps[k].hold_time;
                    cmd.pcr_temp = section_que_[i].amp_section.steps[k].temp;
                    cmd.current_section = i+1;
                    cmd.current_cycle = j+1;
                    cmd.current_step = k+1;
                    cmd.is_acquisition_amp = 1;

                    if (i == 0 && k == 0) {
                        if (cmd.pcr_temp >= 30) {
                            cmd.is_heat = 1;
                        } else {
                            cmd.is_cold = 1;
                        }
                    } else {
                        if (cmd.pcr_temp > temp_pre_) {
                            cmd.is_heat = 1;
                        } else if (cmd.pcr_temp < temp_pre_) {
                            cmd.is_cold = 1;
                        }
                    }
                    
                    cmd.id = total_step_;
                    cmd_que_.push_back(cmd);
                    total_step_++;
                    temp_pre_ = cmd.pcr_temp;

                    if (section_que_[i].amp_section.steps[k].is_acquisition) {
                        memset(&cmd, 0, sizeof(PcrSlaveCmd));
                        snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_MOTOR_SCAN);
                        snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_MOTOR_SCAN);
                        snprintf(cmd.section_name, sizeof(cmd.section_name), "%s", section_que_[i].amp_section.name);
                        cmd.retry_cnt = RETYRY_NUM;
                        cmd.total_cycle = section_que_[i].amp_section.cycles;
                        cmd.loop = 1;
                        cmd.time = 1;
                        cmd.current_section = i+1;
                        cmd.current_cycle = j+1;
                        cmd.current_step = k+1;
                        cmd.is_acquisition_amp = 1;
                        cmd.id = total_step_;
                        cmd_que_.push_back(cmd);
                        total_step_++;

                        snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP_END);
                        snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_AMP_END);
                        cmd.id = total_step_;
                        cmd_que_.push_back(cmd);
                        total_step_++;
                    }
                }
            }
        } else if (section_que_[i].section_type == SECTION_TYPE_MELT) {
            double melt_total_steps = 0.0;
            PcrSlaveCmd cmd = {0};

            if (section_que_[i].melt_section.interval < 0.1 || section_que_[i].melt_section.interval > 0.2) {
                melt_total_steps = ((section_que_[i].melt_section.end_temp - section_que_[i].melt_section.start_temp) / 0.1) + 1.0;
            } else {
                melt_total_steps = ((section_que_[i].melt_section.end_temp - section_que_[i].melt_section.start_temp) / section_que_[i].melt_section.interval) + 1.0;
            }
            
            snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_PCR_SET_TEMP_AMP);
            snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_PCR_SET_TEMP_AMP);
            snprintf(cmd.section_name, sizeof(cmd.section_name), "%s", section_que_[i].melt_section.name);
            cmd.retry_cnt = RETYRY_NUM;
            cmd.total_cycle = 1;
            cmd.loop = 1;
            cmd.time = 30;
            cmd.pcr_temp = section_que_[i].melt_section.start_temp;
            cmd.current_section = i+1;
            cmd.current_cycle = 1;
            cmd.current_step = 0;
            cmd.is_acquisition_amp = 1;

            if (cmd.pcr_temp > temp_pre_) {
                cmd.is_heat = 1;
            } else if (cmd.pcr_temp < temp_pre_) {
                cmd.is_cold = 1;
            }

            cmd.id = total_step_;
            cmd_que_.push_back(cmd);
            total_step_++;
            temp_pre_ = section_que_[i].melt_section.end_temp;

            memset(&cmd, 0, sizeof(PcrSlaveCmd));
            snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_MOTOR_RESET);
            snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_MOTOR_RESET);
            snprintf(cmd.section_name, sizeof(cmd.section_name), "%s", section_que_[i].melt_section.name);
            cmd.retry_cnt = RETYRY_NUM;
            cmd.total_cycle = 1;
            cmd.loop = 1;
            cmd.time = 1;
            cmd.current_section = i+1;
            cmd.current_cycle = 1;
            cmd.current_step = 0;
            cmd.is_acquisition_amp = 1;
            cmd.id = total_step_;
            cmd_que_.push_back(cmd);
            total_step_++;

            for (k = 0; k < melt_total_steps; k++) {
                double temp = 0.0;
                if (section_que_[i].melt_section.interval < 0.1 || section_que_[i].melt_section.interval > 0.2) {
                    temp = section_que_[i].melt_section.start_temp + k * 0.1;
                } else {
                    temp = section_que_[i].melt_section.start_temp + k * section_que_[i].melt_section.interval;
                }

                /*memset(&cmd, 0, sizeof(PcrSlaveCmd));
                snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_MOTOR_RESET);
                snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_MOTOR_RESET);
                snprintf(cmd.section_name, sizeof(cmd.section_name), "%s", section_que_[i].melt_section.name);
                cmd.retry_cnt = RETYRY_NUM;
                cmd.total_cycle = 1;
                cmd.loop = 1;
                cmd.time = 1;
                cmd.current_section = i+1;
                cmd.current_cycle = 1;
                cmd.current_step = k + 1;
                cmd.is_acquisition_melt = 1;
                cmd.id = total_step_;
                cmd_que_.push_back(cmd);
                total_step_++;*/

                memset(&cmd, 0, sizeof(PcrSlaveCmd));
                snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_PCR_SET_TEMP_AMP);
                snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_PCR_SET_TEMP_AMP);
                snprintf(cmd.section_name, sizeof(cmd.section_name), "%s", section_que_[i].melt_section.name);
                cmd.retry_cnt = RETYRY_NUM;
                cmd.total_cycle = 1;
                cmd.loop = 1;
                cmd.time = 0.2;
                cmd.pcr_temp = temp;
                cmd.current_section = i+1;
                cmd.current_cycle = 1;
                cmd.current_step = k + 1;
                cmd.is_acquisition_melt = 1;
                cmd.id = total_step_;
                cmd_que_.push_back(cmd);
                total_step_++;

                memset(&cmd, 0, sizeof(PcrSlaveCmd));
                snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_MOTOR_SCAN);
                snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_MOTOR_SCAN);
                snprintf(cmd.section_name, sizeof(cmd.section_name), "%s", section_que_[i].melt_section.name);
                cmd.retry_cnt = RETYRY_NUM;
                cmd.total_cycle = 1;
                cmd.loop = 1;
                cmd.time = 1;
                cmd.current_section = i+1;
                cmd.current_cycle = 1;
                cmd.current_step = k+1;
                cmd.is_acquisition_melt = 1;
                cmd.id = total_step_;
                cmd_que_.push_back(cmd);
                total_step_++;

                snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_GET_CURRENT_TEMP_POST);
                snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_GET_CURRENT_TEMP_POST);
                cmd.id = total_step_;
                cmd_que_.push_back(cmd);
                total_step_++;

                snprintf(cmd.cmd_name, sizeof(cmd.cmd_name), "%s", PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_MELT);
                snprintf(cmd.desc, sizeof(cmd.desc), "%s", PCR_SLAVE_CMD_PCR_FLUORESCENCE_GET_MELT);
                cmd.id = total_step_;
                cmd_que_.push_back(cmd);
                total_step_++;
            }
        }
    }

    warn_printf("Total step:%d.\n", total_step_);
}

int WorkflowMgr::CmdEnQueue(PcrSlaveCmd* cmd) {
    if (cmd == NULL) {
        err_printf("Input err, cmd ptr is NULL.\n");
        return -1;
    }

    PcrSlaveCmd cmd_t = {0};
    memcpy(&cmd_t, cmd, sizeof(PcrSlaveCmd));
    cmd_t.id = total_step_;
    cmd_que_.push_back(cmd_t);
    total_step_++;

    return 0;
}

int WorkflowMgr::GetCmdQueueFront(PcrSlaveCmd* cmd) {
    if (cmd == NULL) {
        err_printf("Input err, cmd ptr is NULL.\n");
        return -1;
    }

    if (cmd_que_.empty()) {
        //err_printf("Cmd queue is empty.\n");
        return -1;
    }

    memcpy(cmd, &cmd_que_.front(), sizeof(PcrSlaveCmd));
    return 0;
}

int WorkflowMgr::GetCmdQueueSize() {
    return cmd_que_.size();
}

int WorkflowMgr::CmdDeQueue() {
    if (cmd_que_.empty()) {
        err_printf("Cmd queue is empty, cannot dequeue.\n");
        return -1;
    }

    cmd_que_.erase(cmd_que_.begin());
    return 0;
}

int WorkflowMgr::SectionEnQueue(Section* section) {
    if (section == NULL) {
        err_printf("Input err, cmd ptr is NULL.\n");
        return -1;
    }

    Section section_t = {0};
    memcpy(&section_t, section, sizeof(Section));
    section_num_++;
    section_t.section_id = section_num_;
    section_que_.push_back(section_t);

    return section_num_;
}

int WorkflowMgr::GetSectionQueueSize() {
    return section_que_.size();
}

int WorkflowMgr::AddTemperatureDot(double temp) {
    if (experiment_json_ == NULL) {
        err_printf("Experiment json is NULL.\n");
        return -1;
    }

    timeval t;
    long running_time = 0;
    gettimeofday(&t, NULL);
    running_time = (t.tv_sec - start_time_.tv_sec) + (t.tv_usec - start_time_.tv_usec) / 1000000;

    temp_ = temp;

    cJSON* temp_json = cJSON_GetObjectItem(experiment_json_, "Temperatures");
    if (temp_json == NULL) {
        err_printf("Experiment json wrong, key[Temperatures] is not found.\n");
        return -1;
    }

    cJSON* temp_item_json = cJSON_CreateObject();
    if (temp_item_json == NULL) {
        err_printf("Create temperature json object err.\n");
        return -1;
    }

    cJSON_AddNumberToObject(temp_item_json, "BlockTemperature", temp);
    cJSON_AddNumberToObject(temp_item_json, "HotlidTemperature", 0);
    cJSON_AddNumberToObject(temp_item_json, "TubeTemperature", 0);
    cJSON_AddNumberToObject(temp_item_json, "Time", running_time);
    cJSON_AddItemToArray(temp_json, temp_item_json);

    return 0;
}

int WorkflowMgr::AddRawCurveDot(TupeData* tupe_list) {
    int i = 0;
    int result_num = 0;
    int j = 0;
    int fluorescence_num = 0;
    char fluorescence_type[4] = {0};

    if (tupe_list == NULL) {
        err_printf("Input err, tupe_list ptr is NULL.\n");
        return -1;
    }

    if (experiment_json_ == NULL) {
        err_printf("Experiment json is NULL.\n");
        return -1;
    }

    cJSON* well_result_json = cJSON_GetObjectItem(experiment_json_, "WellResult");
    if (well_result_json == NULL) {
        err_printf("Experiment json wrong, key[WellResult] is not found.\n");
        return -1;
    }
    result_num = cJSON_GetArraySize(well_result_json);
    if (result_num != PCR_CHANNEL_NUM) {
        err_printf("Experiment json wrong, WellResult item num:%d is not equal to PCR_CHANNEL_NUM:%d.\n", result_num, PCR_CHANNEL_NUM);
        return -1;
    }

    for (i = 0; i < result_num; i++) {
        TupeData* tupe = &(tupe_list[i]);
        cJSON* well_result_item_json = cJSON_GetArrayItem(well_result_json, i);
        cJSON* target_result_json = cJSON_GetObjectItem(well_result_item_json, "TargetResults");
        if (target_result_json == NULL) {
            err_printf("Experiment json wrong, %dth WellResult item key[TargetResults] is not found.\n", i);
            return -1;
        }

        fluorescence_num = cJSON_GetArraySize(target_result_json);
        for (j = 0; j < fluorescence_num; j++) {
            cJSON*target_result_item_json = cJSON_GetArrayItem(target_result_json, j);
            if (target_result_item_json == NULL) {
                err_printf("Experiment json wrong, get %dth TargetResults %dth item err.\n", i, j);
                return -1;
            }

            cJSON* obj_json = cJSON_GetObjectItem(target_result_item_json, "Dye");
            if (obj_json == NULL || obj_json->valuestring == NULL) {
                err_printf("Experiment json wrong, get %dth TargetResults %dth item key[Dye] err.\n", i, j);
                return -1;
            }
            snprintf(fluorescence_type, sizeof(fluorescence_type), "%s", obj_json->valuestring);

            obj_json = cJSON_GetObjectItem(target_result_item_json, "RawCurve");
            if (obj_json == NULL) {
                err_printf("Experiment json wrong, get %dth TargetResults %dth item key[RawCurve] err.\n", i, j);
                return -1;
            }

            if (strcmp(fluorescence_type, FLUORESCENCE_FAM) == 0) {
                cJSON_AddItemToArray(obj_json, cJSON_CreateNumber(tupe->fluorescence_FAM));                
            } else if (strcmp(fluorescence_type, FLUORESCENCE_CY5) == 0) {
                cJSON_AddItemToArray(obj_json, cJSON_CreateNumber(tupe->fluorescence_CY5));                
            } else if (strcmp(fluorescence_type, FLUORESCENCE_HEX) == 0) {
                cJSON_AddItemToArray(obj_json, cJSON_CreateNumber(tupe->fluorescence_HEX));                
            } else if (strcmp(fluorescence_type, FLUORESCENCE_ROX) == 0) {
                cJSON_AddItemToArray(obj_json, cJSON_CreateNumber(tupe->fluorescence_ROX));                
            }
        }
    }

    return 0;
}

int WorkflowMgr::SumTupe(TupeData* tupe_list) {
    int i = 0;
    if (tupe_list == NULL) {
        err_printf("Input err, tupe_list ptr is NULL.\n");
        return -1;
    }

    for (i = 0; i < PCR_CHANNEL_NUM; i++) {
        TupeData* tupe = &(tupe_list[i]);
        tupe_sum_[i].fluorescence_FAM += tupe->fluorescence_FAM;
        tupe_sum_[i].fluorescence_CY5 += tupe->fluorescence_CY5;
        tupe_sum_[i].fluorescence_HEX += tupe->fluorescence_HEX;
        tupe_sum_[i].fluorescence_ROX += tupe->fluorescence_ROX;
    }

    raw_curve_num_++;
    return 0;
}

int WorkflowMgr::GetMeanTupe(TupeData* tupe_list) {
    int i = 0;
    if (tupe_list == NULL) {
        err_printf("Input err, tupe_list ptr is NULL.\n");
        return -1;
    }

    for (i = 0; i < PCR_CHANNEL_NUM; i++) {
        TupeData* tupe = &(tupe_list[i]);
        tupe->fluorescence_FAM = tupe_sum_[i].fluorescence_FAM / raw_curve_num_;
        tupe->fluorescence_CY5 = tupe_sum_[i].fluorescence_CY5 / raw_curve_num_;
        tupe->fluorescence_HEX = tupe_sum_[i].fluorescence_HEX / raw_curve_num_;
        tupe->fluorescence_ROX = tupe_sum_[i].fluorescence_ROX / raw_curve_num_;
    }

    return 0;
}

void WorkflowMgr::ClearTupe() {
    int i = 0;

    for (i = 0; i < PCR_CHANNEL_NUM; i++) {
        tupe_sum_[i].fluorescence_FAM = 0;
        tupe_sum_[i].fluorescence_CY5 = 0;
        tupe_sum_[i].fluorescence_HEX = 0;
        tupe_sum_[i].fluorescence_ROX = 0;
    }
    raw_curve_num_ = 0;
}

int WorkflowMgr::AddRawMeltCurveDot(TupeData* tupe_list) {
    int i = 0;
    int result_num = 0;
    int j = 0;
    int fluorescence_num = 0;
    char fluorescence_type[4] = {0};

    if (tupe_list == NULL) {
        err_printf("Input err, tupe_list ptr is NULL.\n");
        return -1;
    }

    if (experiment_json_ == NULL) {
        err_printf("Experiment json is NULL.\n");
        return -1;
    }

    cJSON* well_result_json = cJSON_GetObjectItem(experiment_json_, "WellResult");
    if (well_result_json == NULL) {
        err_printf("Experiment json wrong, key[WellResult] is not found.\n");
        return -1;
    }
    result_num = cJSON_GetArraySize(well_result_json);
    if (result_num != PCR_CHANNEL_NUM) {
        err_printf("Experiment json wrong, WellResult item num:%d is not equal to PCR_CHANNEL_NUM:%d.\n", result_num, PCR_CHANNEL_NUM);
        return -1;
    }

    for (i = 0; i < result_num; i++) {
        TupeData* tupe = &(tupe_list[i]);
        cJSON* well_result_item_json = cJSON_GetArrayItem(well_result_json, i);
        cJSON* target_result_json = cJSON_GetObjectItem(well_result_item_json, "TargetResults");
        if (target_result_json == NULL) {
            err_printf("Experiment json wrong, %dth WellResult item key[TargetResults] is not found.\n", i);
            return -1;
        }

        fluorescence_num = cJSON_GetArraySize(target_result_json);
        for (j = 0; j < fluorescence_num; j++) {
            cJSON*target_result_item_json = cJSON_GetArrayItem(target_result_json, j);
            if (target_result_item_json == NULL) {
                err_printf("Experiment json wrong, get %dth TargetResults %dth item err.\n", i, j);
                return -1;
            }

            cJSON* obj_json = cJSON_GetObjectItem(target_result_item_json, "Dye");
            if (obj_json == NULL || obj_json->valuestring == NULL) {
                err_printf("Experiment json wrong, get %dth TargetResults %dth item key[Dye] err.\n", i, j);
                return -1;
            }
            snprintf(fluorescence_type, sizeof(fluorescence_type), "%s", obj_json->valuestring);

            obj_json = cJSON_GetObjectItem(target_result_item_json, "RawMeltCurve");
            if (obj_json == NULL) {
                err_printf("Experiment json wrong, get %dth TargetResults %dth item key[RawMeltCurve] err.\n", i, j);
                return -1;
            }

            cJSON* dot_json = cJSON_CreateObject();
            if (dot_json == NULL) {
                err_printf("Create RawMeltCurve dot json err.\n");
                return -1;
            }
            cJSON_AddNumberToObject(dot_json, "X", temp_post_[i]);
            if (strcmp(fluorescence_type, FLUORESCENCE_FAM) == 0) {
                cJSON_AddNumberToObject(dot_json, "Y", tupe->fluorescence_FAM);
                cJSON_AddItemToArray(obj_json, dot_json);
            } else if (strcmp(fluorescence_type, FLUORESCENCE_CY5) == 0) {
                cJSON_AddNumberToObject(dot_json, "Y", tupe->fluorescence_CY5);
                cJSON_AddItemToArray(obj_json, dot_json);
            } else if (strcmp(fluorescence_type, FLUORESCENCE_HEX) == 0) {
                cJSON_AddNumberToObject(dot_json, "Y", tupe->fluorescence_HEX);
                cJSON_AddItemToArray(obj_json, dot_json);
            } else if (strcmp(fluorescence_type, FLUORESCENCE_ROX) == 0) {
                cJSON_AddNumberToObject(dot_json, "Y", tupe->fluorescence_ROX);
                cJSON_AddItemToArray(obj_json, dot_json);
            }
        }
    }

    return 0;
}

int WorkflowMgr::SetTempPost(double* temp_list) {
    int i = 0;
    if (temp_list == NULL) {
        err_printf("Input err, temp_list ptr is NULL.\n");
        return -1;
    }

    for (i = 0; i < PCR_CHANNEL_NUM; i++) {
        temp_post_[i] = temp_list[i];
    }

    return 0;
}

double WorkflowMgr::GetCurrentTemp() {
    return temp_;
}

void WorkflowMgr::CleanUp() {
    if (!section_que_.empty()) {
        section_que_.clear();
    }

    if (!cmd_que_.empty()) {
        cmd_que_.clear();
    }

    if (experiment_json_) {
        cJSON_Delete(experiment_json_);
        experiment_json_ = NULL;
    }

    if (!template_name_.empty()) {
        template_name_.clear();
    }
    
    total_time_ = 0;
    start_time_.tv_sec = 0;
    start_time_.tv_usec = 0;

    total_step_ = 0;
    section_num_ = 0;
    temp_pre_ = 0;
    temp_ = 0;
    is_pcr_temp_waiting_ = 0;

    raw_curve_num_ = 0;
    memset(tupe_sum_, 0, sizeof(TupeData)*PCR_CHANNEL_NUM);
    memset(temp_post_, 0, sizeof(double)*PCR_CHANNEL_NUM);
}

void WorkflowMgr::DeinitWorkflowMgr() {
    CleanUp();
}

WorkflowMgr::~WorkflowMgr() {
    CleanUp();
}
