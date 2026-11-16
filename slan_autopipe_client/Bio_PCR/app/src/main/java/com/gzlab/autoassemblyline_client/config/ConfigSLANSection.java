package com.gzlab.autoassemblyline_client.config;

import java.util.ArrayList;

public class ConfigSLANSection {

    private String Name;//名称

    private int Cycles;//循环

    private double rate;//熔解段的升温速率

    private ArrayList<Step> items;

    private int idx;//序号

    private int programType;//是熔解程序段还是其他

    private int scanMode;//扫描模式.只有熔解程序段有

    private double interval;//熔解的step模式扫描间隔
    private int meltHoldtime;

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public int getCycles() {
        return Cycles;
    }

    public void setCycles(int cycles) {
        Cycles = cycles;
    }

    public ArrayList<Step> getItems() {
        return items;
    }

    public void setItems(ArrayList<Step> items) {
        this.items = items;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public int getProgramType() {
        return programType;
    }

    public void setProgramType(int programType) {
        this.programType = programType;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public int getScanMode() {
        return scanMode;
    }

    public void setScanMode(int scanMode) {
        this.scanMode = scanMode;
    }

    public double getInterval() {
        return interval;
    }

    public void setInterval(double interval) {
        this.interval = interval;
    }

    public int getMeltHoldtime() {
        return meltHoldtime;
    }

    public void setMeltHoldtime(int meltHoldtime) {
        this.meltHoldtime = meltHoldtime;
    }

    public static class Step implements Comparable<Step>{

        private int step_index;

        private double temperature;//设置的温度

        private int stepType;

        private String stepName;

        private int holdTime;//保持时间单位是s

        private int sectionIndex;//对应的程序段

        private boolean IsAcquisition = false;

        private double rightTemperature;

        private double leftTemperature;

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }


        public int getStepType() {
            return stepType;
        }

        public void setStepType(int stepType) {
            this.stepType = stepType;
        }

        public int getHoldTime() {
            return holdTime;
        }

        public void setHoldTime(int holdTime) {
            this.holdTime = holdTime;
        }

        public String getStepName() {
            return stepName;
        }

        public void setStepName(String stepName) {
            this.stepName = stepName;
        }

        public int getSectionIndex() {
            return sectionIndex;
        }

        public void setSectionIndex(int sectionIndex) {
            this.sectionIndex = sectionIndex;
        }

        public int getStep_index() {
            return step_index;
        }

        public void setStep_index(int step_index) {
            this.step_index = step_index;
        }

        public boolean isAcquisition() {
            return IsAcquisition;
        }

        public void setAcquisition(boolean acquisition) {
            IsAcquisition = acquisition;
        }

        public double getRightTemperature() {
            return rightTemperature;
        }

        public void setRightTemperature(double rightTemperature) {
            this.rightTemperature = rightTemperature;
        }

        public double getLeftTemperature() {
            return leftTemperature;
        }

        public void setLeftTemperature(double leftTemperature) {
            this.leftTemperature = leftTemperature;
        }

        @Override
        public int compareTo(Step step) {
            if(this.step_index>(step.step_index)){
                return 1;
            }else if(this.step_index == step.step_index){
                return 0;
            }else {
                return -1;
            }
        }
    }


}
