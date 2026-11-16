package com.gzlab.autoassemblyline_client.autopipeline.data;

import com.gzlab.autoassemblyline_client.autopipeline.message.SLAN_PCRReqMessage;

/**
 * 保存模板的结果
 */
public class SaveReqResult {
    //模板请求消息
    SLAN_PCRReqMessage slan_pcrReqMessage;
    //模板保存结果
    private  boolean saveResult;
    //pcr模板名称
    private String templateName;
    private String pcrFileName;//PCR文件名称
    //模板保存路径
    private String saveFilePath;

    private String saveDirPath;

    public SLAN_PCRReqMessage getSlan_pcrReqMessage() {
        return slan_pcrReqMessage;
    }

    public void setSlan_pcrReqMessage(SLAN_PCRReqMessage slan_pcrReqMessage) {
        this.slan_pcrReqMessage = slan_pcrReqMessage;
    }

    public boolean isSaveResult() {
        return saveResult;
    }

    public void setSaveResult(boolean saveResult) {
        this.saveResult = saveResult;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getSaveFilePath() {
        return saveFilePath;
    }

    public void setSaveFilePath(String saveFilePath) {
        this.saveFilePath = saveFilePath;
    }


    public String getSaveDirPath() {
        return saveDirPath;
    }

    public void setSaveDirPath(String saveDirPath) {
        this.saveDirPath = saveDirPath;
    }

    public String getPcrFileName() {
        return pcrFileName;
    }

    public void setPcrFileName(String pcrFileName) {
        this.pcrFileName = pcrFileName;
    }
}
