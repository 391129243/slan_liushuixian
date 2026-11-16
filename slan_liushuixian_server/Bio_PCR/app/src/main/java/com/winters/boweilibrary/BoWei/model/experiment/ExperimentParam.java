package com.winters.boweilibrary.BoWei.model.experiment;

import java.util.ArrayList;

public class ExperimentParam {
    //private String serialNo;
    private String path;
    private String name;
    private ArrayList<Well> wells;
    private Byte block;
    private int TemperatureControlMode;//控温方式枚举

    public ExperimentParam(String path, String name,ArrayList<Well> wells, Byte block, int temperatureControlMode) {
     //   this.serialNo = serialNo;
        this.path = path;
        this.name = name;
        this.wells = wells;
        this.block = block;
        this.TemperatureControlMode = temperatureControlMode;
    }

    public ExperimentParam(){

    }

 /*   public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }*/

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ArrayList<Well> getWells() {
        return wells;
    }

    public void setWells(ArrayList<Well> wells) {
        this.wells = wells;
    }

    public Byte getBlock() {
        return block;
    }

    public void setBlock(Byte block) {
        this.block = block;
    }

    public int getTemperatureControlMode() {
        return TemperatureControlMode;
    }

    public void setTemperatureControlMode(int temperatureControlMode) {
        TemperatureControlMode = temperatureControlMode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
