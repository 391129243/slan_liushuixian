package com.gzlab.autoassemblyline_client.WintersUtils.entity.judgeEntity;

public class PEAKSingleRuleEntity {

    //

    private boolean IsSelected;

    private boolean IsPostive = false;

    private String Category;

    private double PEAKvalue;

    private double PEAKvalueUP;

    private double PEAKvalueDown;

    private double PEAKvalueRM;

    private double actualRM;
    private double actualTM;

    public PEAKSingleRuleEntity(boolean isSelected, boolean isPostive, String category, double PEAKvalue, double PEAKvalueUP, double PEAKvalueDown, double PEAKvalueRM) {
        IsSelected = isSelected;
        IsPostive = isPostive;
        Category = category;
        this.PEAKvalue = PEAKvalue;
        this.PEAKvalueUP = PEAKvalueUP;
        this.PEAKvalueDown = PEAKvalueDown;
        this.PEAKvalueRM = PEAKvalueRM;
    }

    public boolean isSelected() {

        return IsSelected;

    }

    public void setSelected(boolean selected) {
        IsSelected = selected;
    }

    public String getCategory() {
        return Category;
    }

    public void setCategory(String category) {
        Category = category;
    }

    public double getPEAKvalue() {
        return PEAKvalue;
    }

    public void setPEAKvalue(double PEAKvalue) {
        this.PEAKvalue = PEAKvalue;
    }

    public double getPEAKvalueUP() {
        return PEAKvalueUP;
    }

    public void setPEAKvalueUP(double PEAKvalueUP) {
        this.PEAKvalueUP = PEAKvalueUP;
    }

    public double getPEAKvalueDown() {
        return PEAKvalueDown;
    }

    public void setPEAKvalueDown(double PEAKvalueDown) {
        this.PEAKvalueDown = PEAKvalueDown;
    }

    public boolean isPostive() {
        return IsPostive;
    }

    public void setPostive(boolean postive) {
        IsPostive = postive;
    }

    public double getPEAKvalueRM() {
        return PEAKvalueRM;
    }

    public void setPEAKvalueRM(double PEAKvalueRM) {
        this.PEAKvalueRM = PEAKvalueRM;
    }

    public double getActualRM() {
        return actualRM;
    }

    public void setActualRM(double actualRM) {
        this.actualRM = actualRM;
    }

    public double getActualTM() {
        return actualTM;
    }

    public void setActualTM(double actualTM) {
        this.actualTM = actualTM;
    }
}
