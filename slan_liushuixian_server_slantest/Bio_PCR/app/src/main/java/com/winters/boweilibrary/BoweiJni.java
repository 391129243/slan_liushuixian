package com.winters.boweilibrary;

import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;

public class BoweiJni {

    static {
        System.loadLibrary("BoweiJni");
    }


    public static native void analysisExperimentLoessFilter(Experiment experiment);

}