package com.gzlab.autoassemblyline_client.bean;

import com.github.mikephil.charting.data.Entry;

import java.util.List;

/**
 * 显示运行中的温度曲线
 */
public class ExTemperature {

   //private List<Entry> blockTemperatures;
   private List<Entry> tubeTemperatures;
   //private List<Entry> hotlidTemperatures;


   public ExTemperature(){

   }
    public List<Entry> getTubeTemperatures() {
        return tubeTemperatures;
    }

    public void setTubeTemperatures(List<Entry> tubeTemperatures) {
        this.tubeTemperatures = tubeTemperatures;
    }

/*    public List<Entry> getHotlidTemperatures() {
        return hotlidTemperatures;
    }

    public void setHotlidTemperatures(List<Entry> hotlidTemperatures) {
        this.hotlidTemperatures = hotlidTemperatures;
    }

    public List<Entry> getBlockTemperatures() {
        return blockTemperatures;
    }

    public void setBlockTemperatures(List<Entry> blockTemperatures) {
        this.blockTemperatures = blockTemperatures;
    }*/


}
