package com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData;

public class MidValue {
    /// <summary>
    /// 名称
    /// </summary>
    private String Name = "";

    /// <summary>
    /// 内容
    /// </summary>
    private String ValueContent = "";

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public String getValueContent() {
        return ValueContent;
    }

    public void setValueContent(String valueContent) {
        this.ValueContent = valueContent;
    }

    public MidValue(String name, String valueContent) {
        this.Name = name;
        this.ValueContent = valueContent;
    }

    public MidValue(){

    }
}
