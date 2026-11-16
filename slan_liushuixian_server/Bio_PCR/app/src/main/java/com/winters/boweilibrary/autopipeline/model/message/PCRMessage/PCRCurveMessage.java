package com.winters.boweilibrary.autopipeline.model.message.PCRMessage;

import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.winters.boweilibrary.autopipeline.model.message.BaseMessage;

public class PCRCurveMessage extends BaseMessage {

//    "Status": "",    //FREE、RUNNING、ERROR
//            "Experiment": "", //具体参考http://43.254.153.207:8081/sdk/slansdk-linux.html#Experiment，实时曲线数据、实时温度数据
//            "Template": "13~新冠~first.prj"

    private String Status;

    private String Template;

    private Experiment Experiment;

    public String getStatus() {
        return Status;
    }

    public void setStatus(String status) {
        Status = status;
    }

    public String getTemplate() {
        return Template;
    }

    public void setTemplate(String template) {
        Template = template;
    }

    public Experiment getExperiment() {
        return Experiment;
    }

    public void setExperiment(Experiment experiment) {
        Experiment = experiment;
    }
}
