package com.winters.boweilibrary.BoWei.tempdata;

import java.util.List;

public class BoWeiPCRStepEntity {

    private String name = "";

    private String description ="";

    private int loop;

    private double time;

    private String pCRCmd = "";

    private String memoryNote = "";

    private int currentSection = 0;

    private int currentCycle = 0;

    private int currentStep = 0;

    private String sectionName;

    private List<Double> parms;

    private int reTryCount = 3;

    private boolean isAcquisitionAMP;

    private boolean isAcquisitionMelt;

    private boolean _isSelected;

    private double hotlid_temperature;

    private boolean isHeat;

    private boolean isCool;

    private double pCR_temperature;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getLoop() {
        return loop;
    }

    public void setLoop(int loop) {
        this.loop = loop;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public String getPCRCmd() {
        return pCRCmd;
    }

    public void setPCRCmd(String pCRCmd) {
        this.pCRCmd = pCRCmd;
    }

    public String getMemoryNote() {
        return memoryNote;
    }

    public void setMemoryNote(String memoryNote) {
        this.memoryNote = memoryNote;
    }

    public int getCurrentSection() {
        return currentSection;
    }

    public void setCurrentSection(int currentSection) {
        this.currentSection = currentSection;
    }

    public int getCurrentCycle() {
        return currentCycle;
    }

    public void setCurrentCycle(int currentCycle) {
        this.currentCycle = currentCycle;
    }

    public int getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
    }

    public List<Double> getParms() {
        return parms;
    }

    public void setParms(List<Double> parms) {
        this.parms = parms;
    }

    public int getReTryCount() {
        return reTryCount;
    }

    public void setReTryCount(int reTryCount) {
        this.reTryCount = reTryCount;
    }

    public boolean isAcquisitionAMP() {
        return isAcquisitionAMP;
    }

    public void setAcquisitionAMP(boolean acquisitionAMP) {
        isAcquisitionAMP = acquisitionAMP;
    }

    public boolean isAcquisitionMelt() {
        return isAcquisitionMelt;
    }

    public void setAcquisitionMelt(boolean acquisitionMelt) {
        isAcquisitionMelt = acquisitionMelt;
    }

    public boolean is_isSelected() {
        return _isSelected;
    }

    public void set_isSelected(boolean _isSelected) {
        this._isSelected = _isSelected;
    }

    public double getHotlid_temperature() {
        return hotlid_temperature;
    }

    public void setHotlid_temperature(double hotlid_temperature) {
        this.hotlid_temperature = hotlid_temperature;
    }

    public boolean isHeat() {
        return isHeat;
    }

    public void setHeat(boolean heat) {
        isHeat = heat;
    }

    public boolean isCool() {
        return isCool;
    }

    public void setCool(boolean cool) {
        isCool = cool;
    }

    public double getPCR_temperature() {
        return pCR_temperature;
    }

    public void setPCR_temperature(double pCR_temperature) {
        this.pCR_temperature = pCR_temperature;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public BoWeiPCRStepEntity(String name, String description, String pCRCmd, List<Double> parms, int loop, double time, double hotlid_temperature, double pCR_temperature, int currentSection, int currentCycle, int currentStep)
    {
        this.currentSection = currentSection;
        this.currentCycle = currentCycle;
        this.currentStep = currentStep;
        this.name = name;
        this.description = description;
        this.pCRCmd = pCRCmd;
        this.parms = parms;
        this.loop = loop;
        this.time = time;
        this.hotlid_temperature = hotlid_temperature;
        this.pCR_temperature = pCR_temperature;
    }





}
