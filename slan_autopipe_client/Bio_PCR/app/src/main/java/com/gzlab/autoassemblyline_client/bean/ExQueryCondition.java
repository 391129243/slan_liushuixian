package com.gzlab.autoassemblyline_client.bean;

public class ExQueryCondition {
    //项目开始时间
    private String exStartTime;

    /**
     * 项目项目
     */
    private String exItem;

    private int start;
    private int length;

    public String getExStartTime() {
        return exStartTime;
    }

    public void setExStartTime(String exStartTime) {
        this.exStartTime = exStartTime;
    }

    public String getExItem() {
        return exItem;
    }

    public void setExItem(String exItem) {
        this.exItem = exItem;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
