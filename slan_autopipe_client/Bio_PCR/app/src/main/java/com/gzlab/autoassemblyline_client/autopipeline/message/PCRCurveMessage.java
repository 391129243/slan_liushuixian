package com.gzlab.autoassemblyline_client.autopipeline.message;


import com.gzlab.autoassemblyline_client.autopipeline.message.base.BaseMessage;


public class PCRCurveMessage extends BaseMessage {

//    "Status": "",    //FREE、RUNNING、ERROR
//            "Experiment": "", //具体参考http://43.254.153.207:8081/sdk/slansdk-linux.html#Experiment，实时曲线数据、实时温度数据
//            "Template": "13~新冠~first.prj"

    private String Status;

    private String Template;

    private com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment Experiment;//对象解析后上报

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

    public com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment getExperiment() {
        return Experiment;
    }

    public void setExperiment(com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData.Experiment experiment) {
        Experiment = experiment;
    }
}
