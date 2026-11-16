package com.gzlab.autoassemblyline_client.event;

/**
 * PCR 模板信息更新事件
 * 用于通知界面更新当前执行的模板信息
 */
public class PCRTemplateInfoUpdateEvent {
    private String templateName;    // 模板名称
    private String projectName;     // 项目文件名（如 first.prj）
    private String resultPath;      // 结果保存路径
    private String updateTime;      // 更新时间

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getResultPath() {
        return resultPath;
    }

    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "PCRTemplateInfoUpdateEvent{" +
                "templateName='" + templateName + '\'' +
                ", projectName='" + projectName + '\'' +
                ", resultPath='" + resultPath + '\'' +
                ", updateTime='" + updateTime + '\'' +
                '}';
    }
}
