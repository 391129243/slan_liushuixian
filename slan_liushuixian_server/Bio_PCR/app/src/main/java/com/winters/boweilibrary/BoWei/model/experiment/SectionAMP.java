package com.winters.boweilibrary.BoWei.model.experiment;

import java.util.ArrayList;

/**
 * 扩增程序段
 */
public class SectionAMP extends Section{
    private String Name;//名称
    private int Cycles;//循环数
    private ArrayList<Step> Items;

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
        return Items;
    }

    public void setItems(ArrayList<Step> items) {
        Items = items;
    }
}
