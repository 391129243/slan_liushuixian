package com.gidi.bio_pcr.WintersUtils.entity.procedureEntity;

import java.util.List;

public class HexDebugEntity {

    private boolean IsSelected;

    private String Name;

    private double[] Parms;

    private String Description;

    private String NO;

    private int Loop;

    private int Sleep;

    private int retryCount = 3;

    private List<HexSingleEntity> HexActionList;

    public HexDebugEntity(boolean isSelected, String name, double[] parms, String description, int loop, List<HexSingleEntity> hexActionList) {
        IsSelected = isSelected;
        Name = name;
        Parms = parms;
        Description = description;
        Loop = loop;
        HexActionList = hexActionList;
    }

    public HexDebugEntity(boolean isSelected, String name, double[] parms, String description, int loop, List<HexSingleEntity> hexActionList,int sleep) {
        IsSelected = isSelected;
        Name = name;
        Parms = parms;
        Description = description;
        Loop = loop;
        HexActionList = hexActionList;
        Sleep = sleep;
    }

    public HexDebugEntity() {

    }

    public boolean isSelected() {
        return IsSelected;
    }

    public void setSelected(boolean selected) {
        IsSelected = selected;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public double[] getParms() {
        return Parms;
    }

    public void setParms(double[] parms) {
        Parms = parms;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public int getLoop() {
        return Loop;
    }

    public void setLoop(int loop) {
        Loop = loop;
    }

    public List<HexSingleEntity> getHexActionList() {
        return HexActionList;
    }

    public void setHexActionList(List<HexSingleEntity> hexActionList) {
        HexActionList = hexActionList;
    }

    public String getNO() {
        return NO;
    }

    public void setNO(String NO) {
        this.NO = NO;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
