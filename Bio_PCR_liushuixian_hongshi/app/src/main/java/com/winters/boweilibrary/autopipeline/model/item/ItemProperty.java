package com.winters.boweilibrary.autopipeline.model.item;

public class ItemProperty {

    private String Type;

    private int Position;

    private double X;

    private double Y;

    private double Z;

    private double Z1;

    public int getPosition() {
        return Position;
    }

    public void setPosition(int position) {
        Position = position;
    }

    public String getType() {
        return Type;
    }

    public void setType(String type) {
        Type = type;
    }

    public double getX() {
        return X;
    }

    public void setX(double x) {
        X = x;
    }

    public double getY() {
        return Y;
    }

    public void setY(double y) {
        Y = y;
    }

    public double getZ() {
        return Z;
    }

    public void setZ(double z) {
        Z = z;
    }

    public double getZ1() {
        return Z1;
    }

    public void setZ1(double z1) {
        Z1 = z1;
    }

    public ItemProperty(String type, int position, int x, int y, int z, int z1) {
        Type = type;
        Position = position;
        X = x;
        Y = y;
        Z = z;
        Z1 = z1;
    }
}
