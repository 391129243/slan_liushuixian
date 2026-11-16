package com.gzlab.autoassemblyline_client.bean;

import java.io.Serializable;
import java.util.ArrayList;


public class SLANSection {

    private String Name;//名称

    private int Cycles;//循环

    private double rate;//熔解段的升温速率

    private ArrayList<Step> items;

    private int idx;//序号

    private int programType;//是熔解程序段还是其他

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

    public static class Step implements Serializable{

        private double temperature;//设置的温度

        private int stepType;

        private String stepName;

        private int holdTime;//保持时间单位是s

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
    }


}
