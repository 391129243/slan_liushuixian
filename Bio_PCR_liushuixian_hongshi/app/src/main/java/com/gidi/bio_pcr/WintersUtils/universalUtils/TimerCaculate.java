package com.gidi.bio_pcr.WintersUtils.universalUtils;


import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Step;

import java.util.ArrayList;

public class TimerCaculate {
//
//    四、全流程时间计算
//①总时间=PCR时间+核提时间+进二次试剂时间
//②PCR时间:各循环流程时间之和*1.1
//③进二次试剂时间:120s
//核提时间T0=(T2+T3+T4)*1.1+T1+T5
//4
//1)上下机时间T1=120s
//2)磁珠活化时间T2:
//a.管1活化:T2=(样本体积/19.625)*5/V1
//b.管2活化:T2=(裂解液体积/19.625)*5/V1
//3)反应时间T3=裂解时间+洗涤时间(洗涤1+2+3)+洗脱时间
//4)磁吸时间T4=(样本体积+裂解液体积+洗涤液体积(1+2+3)+洗脱液体积*4) * 4/19.625/(V1/10)
//5)去残留时间T5=300s

    private static double areaParm = HexUtils.areaParm;

    private static double rate = 1.1;

    public static int RNA_Fisrt_Caculate(HexConfig hexConfig){

        double totalTime = 0;
        double T1 = 0;
        double T2 = 0;
        double T3 = 0;
        double T4 = 0;
        double T5 = 0;
        double T6 = 0;

        //上下机时间T1=15 + 45 = 60s
        if(hexConfig.isCasseteFlag()){
            T1 = 60;
        }
        //磁珠活化时间T2:
        if(hexConfig.isMagneticFlag()){
            if(hexConfig.getMagneticChoice()==0){
                T2  = hexConfig.getSplitLieSampleVolume() /areaParm * 10 / HexUtils.fastSpeed;
            }else if(hexConfig.getMagneticChoice()==1){
                T2  = hexConfig.getSplitLieJieYeVolume() /areaParm * 10 / HexUtils.fastSpeed;
            }
        }

        //3)反应时间T3=裂解时间+洗涤时间(洗涤1+2+3)+洗脱时间
        if(hexConfig.isSplitFlag()){
            T3 = hexConfig.getSplitTime();
        }
        if(hexConfig.isWashFlag()){
            T3 = T3 + hexConfig.getWash1Time();

            if(hexConfig.getWashStep() == 2){
                T3 = T3 + hexConfig.getWash22Time();
            }
            if(hexConfig.getWashStep() == 3){
                T3 = T3 + hexConfig.getWash32Time();
                T3 = T3 + hexConfig.getWash33Time();
            }
        }
        if(hexConfig.isElutionFlag()){
            T3 = T3 + hexConfig.getElutionTime();
        }

        //4)磁吸时间T4=(样本体积+裂解液体积+洗涤液体积(1+2+3)+洗脱液体积*4) * 4/19.625/(V1/10)
        double volume = hexConfig.getSplitLieSampleVolume() + hexConfig.getSplitLieJieYeVolume();



        volume = volume * 4 + hexConfig.getElutionVolume() * 16;

        T4 = volume / areaParm / (HexUtils.fastSpeed/10) * rate;

        //5)去残留时间T5=250s
        T5 = 250;

        T6 = (T2 + T3 + T4) * 0.2;


        totalTime = T1 + T2 + T3 + T4 + T5 + T6;

        return (int) totalTime;
    }

    public static int RNA_Second_Caculate(){
        //////默认二次进试剂120秒
        return 120;
    }


    private static double tempRate = 4.0f;
    private static double compensate = 240.0f;////快速升温模式 温度点 pid 1-3秒     正常升温模式 11-13秒
    public static int PCR_Caculate(Project project){

        if(project == null){
            return 0;
        }

        double totalTime = 0;

        Program program = project.getProgram();
        //转化为String
        ArrayList<Section> sections = program.getSections();
        double preTemp = 35;
        if(null != sections && sections.size()>0){
            for(int i= 0;i<sections.size();i++){
                Section section = sections.get(i);
                if(section != null){
                    if(section instanceof SectionAMP){

                        SectionAMP sectionAMP = (SectionAMP) section;

                        int cycles = sectionAMP.getCycles();

                        if(sectionAMP.getItems() != null && sectionAMP.getItems().size()>0) {

                            for (int j = 0; j < cycles; j++) {

                                for (int k = 0; k < sectionAMP.getItems().size(); k++) {

                                    Step step = sectionAMP.getItems().get(k);

                                    totalTime = totalTime + Math.abs(preTemp - step.getTemperature()) / tempRate;

                                    totalTime = totalTime + step.getHoldTime();

                                    preTemp = step.getTemperature();

                                }
                            }
                        }
                    }else if(section instanceof SectionMelt){

                        SectionMelt sectionMelt = (SectionMelt) section;

                        if(sectionMelt.getScanMode() == 0){

                            totalTime = totalTime + Math.abs(preTemp - sectionMelt.getStartTemp()) / tempRate;

                            totalTime = totalTime + Math.abs(sectionMelt.getStartTemp() - sectionMelt.getEndTemp())/sectionMelt.getRate();

                            preTemp = sectionMelt.getEndTemp();
                        }else if(sectionMelt.getScanMode() == 1){
                            totalTime = totalTime + Math.abs(preTemp - sectionMelt.getStartTemp()) / tempRate;
                            ////加上升温时间
                            totalTime = totalTime + Math.abs(sectionMelt.getStartTemp() - sectionMelt.getEndTemp())/tempRate;

                            totalTime = totalTime + Math.abs(preTemp - sectionMelt.getStartTemp()) / sectionMelt.getInterval() * sectionMelt.getHoldTime();

                            preTemp = sectionMelt.getEndTemp();
                        }
                    }
                }

            }
        }
        return (int)(totalTime + compensate);
    }

}
