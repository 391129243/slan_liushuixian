package com.gidi.bio_pcr.slan.bean;

import java.util.Arrays;
import java.util.List;

public enum SampleMapping {
    SAMPLE_1(Arrays.asList(WellConstant.WELL_A8, WellConstant.WELL_B8)),
    SAMPLE_2(Arrays.asList(WellConstant.WELL_A7, WellConstant.WELL_B7)),
    SAMPLE_3(Arrays.asList(WellConstant.WELL_A6, WellConstant.WELL_B6)),
    SAMPLE_4(Arrays.asList(WellConstant.WELL_A5, WellConstant.WELL_B5)),
    SAMPLE_5(Arrays.asList(WellConstant.WELL_A4, WellConstant.WELL_B4)),
    SAMPLE_6(Arrays.asList(WellConstant.WELL_A3, WellConstant.WELL_B3)),
    SAMPLE_7(Arrays.asList(WellConstant.WELL_A2, WellConstant.WELL_B2)),
    SAMPLE_8(Arrays.asList(WellConstant.WELL_A1, WellConstant.WELL_B1));

    private final List<String> wells;
    SampleMapping(List<String> wells){
        this.wells = wells;
    }
    public List<String> getWells(){
        return wells;
    }
}
