package com.gzlab.autoassemblyline_client.util;

import java.text.DecimalFormat;

public class NumberUtils {

    public static DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.00");

    public static DecimalFormat Float_FORMAT = new DecimalFormat("0.0");

    public static DecimalFormat Double2Int_FORMAT = new DecimalFormat("0");


    public static double StringtoDouble(String string) {

        //if(string)
        try {
            return Double.parseDouble("".equals(string) ? "0.00" : string.toString());
        }catch (Exception e){
            return 0.00f;
        }

    }

    public static String doubleto0String(double value) {

        return Double2Int_FORMAT.format(value);
    }

    public static String doubleto2String(double value) {

        return DOUBLE_FORMAT.format(value);
    }

    public static float StringtoFloat(String string) {

        //if(string)
        try {
            return Float.parseFloat("".equals(string) ? "0.0" : string.toString());
        }catch (Exception e){
            return 0.0f;
        }

    }

    public static Integer StringtoInt(String string) {

        try {
            return Integer.parseInt("".equals(string) ? "0" : string.toString());
        }catch (Exception e){
            return 0;
        }

    }


}
