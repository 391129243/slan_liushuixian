package com.gidi.bio_pcr.bean;

import com.github.mikephil.charting.data.Entry;

import java.util.List;
import java.util.Map;

public class Conclusion {

    String wellNum;//孔号

    Map<String,List<Result>>conlusionMap;

    List<String> wellConclusion;


    public String getWellNum() {
        return wellNum;
    }

    public void setWellNum(String wellNum) {
        this.wellNum = wellNum;
    }


    public Map<String, List<Result>> getConlusionMap() {
        return conlusionMap;
    }

    public void setConlusionMap(Map<String, List<Result>> conlusionMap) {
        this.conlusionMap = conlusionMap;
    }

    public List<String> getWellConclusion() {
        return wellConclusion;
    }

    public void setWellConclusion(List<String> wellConclusion) {
        this.wellConclusion = wellConclusion;
    }
}
