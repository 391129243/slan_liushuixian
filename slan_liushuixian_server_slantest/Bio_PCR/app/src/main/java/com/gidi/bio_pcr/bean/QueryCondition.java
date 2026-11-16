package com.gidi.bio_pcr.bean;

import java.util.PropertyResourceBundle;

/**
 * 查询条件
 */
public class QueryCondition {
    private String testStartTime;

    private String testItem;

    private String regentCode;

    private String idcardNum;

    private int start;
    private int length;

    public String getTestStartTime() {
        return testStartTime;
    }

    public void setTestStartTime(String testStartTime) {
        this.testStartTime = testStartTime;
    }

    public String getTestItem() {
        return testItem;
    }

    public void setTestItem(String testItem) {
        this.testItem = testItem;
    }

    public String getRegentCode() {
        return regentCode;
    }

    public void setRegentCode(String regentCode) {
        this.regentCode = regentCode;
    }

    public String getIdcardNum() {
        return idcardNum;
    }

    public void setIdcardNum(String idcardNum) {
        this.idcardNum = idcardNum;
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
