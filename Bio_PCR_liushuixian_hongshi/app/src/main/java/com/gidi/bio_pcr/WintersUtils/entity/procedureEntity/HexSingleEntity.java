package com.gidi.bio_pcr.WintersUtils.entity.procedureEntity;

public class HexSingleEntity {

    private String Hex;

    private int Delay;

    private String Description;

    private boolean IsSelected;


    public HexSingleEntity(String hex, int delay, String description, boolean isSelected) {
        Hex = hex;
        Delay = delay;
        Description = description;
        IsSelected = isSelected;
    }

    public String getHex() {
        return Hex;
    }

    public void setHex(String hex) {
        Hex = hex;
    }

    public int getDelay() {
        return Delay;
    }

    public void setDelay(int delay) {
        Delay = delay;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public boolean isSelected() {
        return IsSelected;
    }

    public void setSelected(boolean selected) {
        IsSelected = selected;
    }
}
