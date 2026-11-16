package com.paul.pipeline.data;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexFileName_V5;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexConfig;
import com.gidi.bio_pcr.WintersUtils.universalUtils.TimerCaculate;
import com.gidi.bio_pcr.config.ConfigManager;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;

import java.io.File;
import java.util.ArrayList;

public class ProjectTimeHelper {

    public static int getRNAFileTime(String fileName){
        int firstRnaTime = 0;
        if(!StringUtil.isEmpty(fileName)){
            //先判断是V4版本还是V5版本
            String path = null;
            if(PcrApplication.rnaVersion.equals("V4")){
                //从/mnt/sdcard/PCR/hexCreateFile_v4/hexConfig_v4打开所需要的文件
            }else if(PcrApplication.rnaVersion.equals("V5")){
                //从/mnt/sdcard/PCR/hexCreateFile_v5/hexConfig_v5打开所需要的文件
                path = HexFileName_V5.HEXCONFIG_PATH + fileName + ".hexConfig";
            }
            if(null != path){
                File file = new File(path);
                if(file != null && file.exists()&& file.isFile()){
                    HexConfig hexConfig = ConfigManager.getHexConfigFile(file);
                    if(null != hexConfig) {
                        firstRnaTime = TimerCaculate.RNA_Fisrt_Caculate(hexConfig);
                    }
                }
            }else{
//                Toast.makeText(getApplicationContext(),getResources().getString(R.string.config_hex_toast_cannot_open_file),
//                        Toast.LENGTH_LONG).show();
            }

        }
        return firstRnaTime;
    }
    public static int RNA_Second_Caculate(){
        //////默认二次进试剂120秒
        return 120;
    }
    public static int getSLANExFileTime(String fileName){
        int firstPcrTime = 0;
        //打开文件夹
        if(!StringUtil.isEmpty(fileName)&& null != PcrApplication.slanService){
            ArrayList<Project> mConfigProjects = PcrApplication.slanService.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
            if(null != mConfigProjects && mConfigProjects.size()>0){
                for(Project project :mConfigProjects){
                    if(null != project){
                        if(project.getName().equals(fileName)){
                            Program program = project.getProgram();
                            //转化为String
                            firstPcrTime = TimerCaculate.PCR_Caculate(project);
                            return firstPcrTime;
                        }
                    }
                }
            }
        }
        return firstPcrTime;
    }
}
