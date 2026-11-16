package com.paul.pipeline.utils;

import java.text.DecimalFormat;

public class NumberUtil {
    public static DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");

    public static DecimalFormat Float_FORMAT = new DecimalFormat("0.0");

    public static DecimalFormat Int_FORMAT = new DecimalFormat("0");

    public static String doubleto0String(double value) {

        return Int_FORMAT.format(value);
    }
    public static String doubleto1String(double value) {

        return Float_FORMAT.format(value);
    }
    public static String doubleto2String(double value) {

        return DOUBLE_FORMAT.format(value);
    }


}
