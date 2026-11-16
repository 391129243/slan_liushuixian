package com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData;

public class Dot {
    /// <summary>
    /// X
    /// </summary>
    private double X = 0.0f;

    /// <summary>
    /// Y
    /// </summary>
    private double Y = 0.0f;

    public double getX() {
        return X;
    }

    public void setX(double x) {
        this.X = x;
    }

    public double getY() {
        return Y;
    }

    public void setY(double y) {
        this.Y = y;
    }

    public Dot(double x, double y) {
        this.X = x;
        this.Y = y;
    }

    public Dot() {
    }
}
