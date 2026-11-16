package com.gzlab.autoassemblyline_client.autopipeline.bioway.ExperimentData;

public class Peak {
    /// <summary>
    /// 熔解峰Tm
    /// </summary>
    private double Tm = 0.0f;

    /// <summary>
    /// 熔解峰相对高度Rm
    /// </summary>
    private double Rm = 0.0f;

    /// <summary>
    /// 熔解峰宽度Wm
    /// </summary>
    private double Wm = 0.0f;

    /// <summary>
    /// 熔解峰绝对高度Ym
    /// </summary>
    private double Ym = 0.0f;

    public double getTm() {
        return Tm;
    }

    public void setTm(double tm) {
        this.Tm = tm;
    }

    public double getRm() {
        return Rm;
    }

    public void setRm(double rm) {
        this.Rm = rm;
    }

    public double getWm() {
        return Wm;
    }

    public void setWm(double wm) {
        this.Wm = wm;
    }

    public double getYm() {
        return Ym;
    }

    public void setYm(double ym) {
        this.Ym = ym;
    }

    public Peak(double tm, double rm, double wm, double ym) {
        this.Tm = tm;
        this.Rm = rm;
        this.Wm = wm;
        this.Ym = ym;
    }

    public Peak(){

    }
}
