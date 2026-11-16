package com.winters.boweilibrary.BoWei.bean;

import com.hongshi.slansdk.model.experiment.Project;
import com.winters.boweilibrary.BoWei.bioweyenum.RunExperimentResult;

public class RunBiowayResult {
    RunExperimentResult runExperimentResult;//运行实验结果枚举
    Project project;//实验项目

    public RunExperimentResult getRunExperimentResult() {
        return runExperimentResult;
    }

    public void setRunExperimentResult(RunExperimentResult runExperimentResult) {
        this.runExperimentResult = runExperimentResult;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
