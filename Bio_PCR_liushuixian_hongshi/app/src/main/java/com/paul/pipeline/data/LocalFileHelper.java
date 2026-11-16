package com.paul.pipeline.data;

import android.util.Log;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Step;
import com.hongshi.slansdk.service.SLANService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.hutool.core.io.FileUtil;

public class LocalFileHelper {
    /**
     * 是否是宏石的模板
     *
     * @return
     */
    public static boolean isHongshiPCRTemplate(String subFileDirPath) {
        //每个模板文件夹中的文件
        File[] subFiles = new File(subFileDirPath).listFiles();
        if (subFiles != null && subFiles.length > 0) {
            for (File subTempFile : subFiles) {
                if(subTempFile.isDirectory()){
                    continue;
                }
                String str = subTempFile.getName(); // 要判断的字符串
                boolean isLowerCase = true; // 初始化标志位为true表示全部字母都为小写
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if (!Character.isLetter(c)) {
                        continue; // 如果不是字母则跳过该字符
                    } else if (!Character.isLowerCase(c)) {
                        isLowerCase = false; // 若存在大写字母，将标志位设置为false
                        break; // 结束遍历
                    }
                }
                if(isLowerCase){
                    continue;
                } else{

                    String newPath_temp = subFileDirPath + "/" + subTempFile.getName().toLowerCase() + "_temp";
                    File newFile_temp;
                    String newPath = subFileDirPath + "" + subTempFile.getName().toLowerCase();
                    File newFile = new File(newPath);
                    File oldFile = new File(subTempFile.getAbsolutePath());
                    /////大小写不敏感
                    // 如果旧文件存在，则重命名它
                    if (oldFile.exists()) {
                        /////大小写不敏感,做一个缓冲文件
                        FileUtil.rename(oldFile,newPath_temp,false,true);
                        newFile_temp = new File(newPath_temp);
                        FileUtil.rename(newFile_temp,newPath,false,true);
//                        if (oldFile.renameTo(newFile)) {
//                            Log.i("File", "Renamed successfully");
//                        } else {
//                            Log.i("File", "Rename failed");
//                        }
                    } else {
                        Log.i("File", "Source file does not exist");
                    }

                }
            }
            File[] subFiles_new = new File(subFileDirPath).listFiles();
            for (File subTempFile_new : subFiles_new) {
                if (subTempFile_new.getName().endsWith("prj")) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * 是否是单rna模板
     */
    public static boolean isRNATemplate(String subFileDirPath) {
        //每个模板文件夹中的文件
        File[] subFiles = new File(subFileDirPath).listFiles();
        if (subFiles != null && subFiles.length > 0) {
            for (File subTempFile : subFiles) {
                if(subTempFile.isDirectory()){
                    continue;
                }
                String str = subTempFile.getName(); // 要判断的字符串
                boolean isLowerCase = true; // 初始化标志位为true表示全部字母都为小写
                for (int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if (!Character.isLetter(c)) {
                        continue; // 如果不是字母则跳过该字符
                    } else if (!Character.isLowerCase(c)) {
                        isLowerCase = false; // 若存在大写字母，将标志位设置为false
                        break; // 结束遍历
                    }
                }
                if(isLowerCase){
                    continue;
                } else{

                    String newPath_temp = subFileDirPath + "/" + subTempFile.getName().toLowerCase() + "_temp";
                    File newFile_temp;
                    String newPath = subFileDirPath + "" + subTempFile.getName().toLowerCase();
                    File newFile = new File(newPath);
                    File oldFile = new File(subTempFile.getAbsolutePath());
                    /////大小写不敏感
                    // 如果旧文件存在，则重命名它
                    if (oldFile.exists()) {
                        /////大小写不敏感,做一个缓冲文件
                        FileUtil.rename(oldFile,newPath_temp,false,true);
                        newFile_temp = new File(newPath_temp);
                        FileUtil.rename(newFile_temp,newPath,false,true);
//                        if (oldFile.renameTo(newFile)) {
//                            Log.i("File", "Renamed successfully");
//                        } else {
//                            Log.i("File", "Rename failed");
//                        }
                    } else {
                        Log.i("File", "Source file does not exist");
                    }

                }
            }
            File[] subFiles_new = new File(subFileDirPath).listFiles();
            for (File subTempFile_new : subFiles_new) {
                if (subTempFile_new.getName().endsWith("hex")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 查询本地模板列表
     */
    public static ArrayList getFullTemplateList() {
        //PcrApplication.experimentType == 0
        ArrayList mHongShiTempList = new ArrayList<String>();
        File[] files = new File(FileUtils.FULL_PROCESS_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                //分别进行文件夹查询判断是丁智还是宏石的模板
                String subFileDirPath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator + files[i].getName()
                        + File.separator;
                //每个模板文件夹中的文件
                boolean isHongshiPCRTemplate = isHongshiPCRTemplate(subFileDirPath);
                if (isHongshiPCRTemplate) {
                    if (null != mHongShiTempList) {
                        mHongShiTempList.add(files[i].getName());
                        Collections.sort(mHongShiTempList);
                    }
                }
            }
        }
        return mHongShiTempList;
    }
    public static ArrayList getRNATemplateList(){
        ArrayList mConfigFiles = new ArrayList<String>();
        //PcrApplication.experimentType == 1
            File[] files = new File(FileUtils.SINGLE_RNA_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
            if (files != null && files.length > 0) {
                mConfigFiles.clear();
                for (int i = 0; i < files.length; i++) {
                    String subFileDirPath = FileUtils.SINGLE_RNA_TEMPLATE_PATH + File.separator
                            + files[i].getName()
                            + File.separator;
                    //每个模板文件夹中的文件
                    boolean isSingleRNA = isRNATemplate(subFileDirPath);
                    if (isSingleRNA) {
                        if (null != mConfigFiles) {
                            mConfigFiles.add(files[i].getName());
                            Collections.sort(mConfigFiles);
                        }
                    }
                }
            }
            return mConfigFiles;
        }
     public static ArrayList getPCRTemplateList(){
        //PcrApplication.experimentType == 2
        ArrayList mConfigFiles = new ArrayList<String>();
            //单PCR
            File[] files = new File(FileUtils.SINGLE_PCR_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
            if(files != null && files.length>0) {
                mConfigFiles.clear();
                for (int i = 0; i < files.length; i++) {
                    String subFileDirPath = FileUtils.SINGLE_PCR_TEMPLATE_PATH + File.separator
                            + files[i].getName()
                            + File.separator;
                    //每个模板文件夹中的文件
                    boolean isHongshiPCRTemplate = isHongshiPCRTemplate(subFileDirPath);
                    if(isHongshiPCRTemplate){
                        if (null != mConfigFiles) {
                            mConfigFiles.add(files[i].getName());
                            Collections.sort(mConfigFiles);
                        }
                    }

                }
            }
            return mConfigFiles;
        }

    public static String getPCRFilePath(){
        String path = "";
        if(PcrApplication.rootTempName.contains("01-")){
            File file = new File(FileUtils.SECRET1_3_flu_repeat_fam_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("02-")){
            File file = new File(FileUtils.SECRET2_3_flu_repeat_rox_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("03-")){
            File file = new File(FileUtils.SECRET3_3_flu_repeat_hex_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("04-")){
            File file = new File(FileUtils.SECRET4_3_flu_repeat_cy5_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("05-")){
            File file = new File(FileUtils.SECRET5_5_sample_repeat_high_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("06-")){
            File file = new File(FileUtils.SECRET6_5_sample_repeat_high_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("07-")){
            File file = new File(FileUtils.SECRET7_5_sample_repeat_median_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("08-")){
            File file = new File(FileUtils.SECRET8_5_sample_repeat_median_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("09-")){
            File file = new File(FileUtils.SECRET9_5_sample_repeat_low_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("10-")){
            File file = new File(FileUtils.SECRET10_5_sample_repeat_low_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("11-")){
            File file = new File(FileUtils.SECRET11_6_sample_linear_AMP_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("12-")){
            File file = new File(FileUtils.SECRET12_6_sample_linear_AMP_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("13-")){
            File file = new File(FileUtils.SECRET13_3_sample_linear_AMP_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("14-")){
            File file = new File(FileUtils.SECRET14_8_flu_linear_fam_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("15-")){
            File file = new File(FileUtils.SECRET15_7_flu_linear_fam_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("16-")){
            File file = new File(FileUtils.SECRET16_8_flu_linear_rox_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("17-")){
            File file = new File(FileUtils.SECRET17_7_flu_linear_rox_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("18-")){
            File file = new File(FileUtils.SECRET18_8_flu_linear_hex_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("19-")){
            File file = new File(FileUtils.SECRET19_7_flu_linear_hex_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("20-")){
            File file = new File(FileUtils.SECRET20_8_flu_linear_cy5_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("21-")){
            File file = new File(FileUtils.SECRET21_7_flu_linear_cy5_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("22-")){
            File file = new File(FileUtils.SECRET22_5_extraction_consistence_high_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("23-")){
            File file = new File(FileUtils.SECRET23_5_extraction_consistence_high_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("24-")){
            File file = new File(FileUtils.SECRET24_5_extraction_consistence_median_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("25-")){
            File file = new File(FileUtils.SECRET25_5_extraction_consistence_median_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("26-")){
            File file = new File(FileUtils.SECRET26_5_extraction_consistence_low_1_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }else if(PcrApplication.rootTempName.contains("27-")){
            File file = new File(FileUtils.SECRET27_5_extraction_consistence_low_2_PATH);
            List<File> listFile  =  Arrays.asList(file.listFiles());
            if(listFile != null && listFile.size()!= 0){
                path = listFile.get(0).getAbsolutePath();
            }
        }
        return path;
    }

    public static String getTemplateName(String serialNum,ArrayList<String> tempList){
        if(!StringUtil.isEmpty(serialNum)){
            for(String templateName :tempList){
                if(templateName.startsWith(serialNum)){
                    return templateName;
                }
            }
        }
        return null;
    }

    public static Project openSLANExFile(String path,String fileName){
        //打开文件夹
        Project openProject = null;
        String tempname = fileName;
        try {
            if (!StringUtil.isEmpty(fileName)) {
                if(fileName.contains(".")) {
                    tempname = LocalFileHelper.getFileNameNoEx(fileName);//去掉后缀名.prj
                }
                ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(path);//获取目录中的项目FileUtils.CONFIG_SLAN_PATH
                if (null != mConfigProjects && mConfigProjects.size() > 0) {
                    for (Project project : mConfigProjects) {
                        if (null != project) {
                            if (project.getName().equals(tempname)) {
                                return project;
                                //   Program program = project.getProgram();
                                //转化为String
//                            openProject = project;
//                            ArrayList<Section> sections = program.getSections();
//                            if(null != sections){
//                                getSLANSections(sections);
//                            }

                            }
                        }
                    }
                }
            }
        }catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
        return openProject;
    }


    public List<ConfigSLANSection> getSLANSections(ArrayList<Section> sections){
        List<ConfigSLANSection> mSLANSections = new ArrayList<>();
        if(sections!=null&&sections.size()>0){
            for(int i= 0;i<sections.size();i++){
                Section section = sections.get(i);
                if(section != null){
                    if(section instanceof SectionAMP){
                        SectionAMP sectionAMP = (SectionAMP) section;
                        ConfigSLANSection slanSection = new ConfigSLANSection();
                        slanSection.setName(sectionAMP.getName());
                        slanSection.setCycles(sectionAMP.getCycles());
                        slanSection.setIdx(sectionAMP.getIdx());
                        slanSection.setProgramType(Constants.PROGRAM_TYPE_AMP);//扩增
                        ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<ConfigSLANSection.Step>();
                        if(sectionAMP.getItems() != null && sectionAMP.getItems().size()>0){
                            for(int k=0;k<sectionAMP.getItems().size();k++){
                                Step step = sectionAMP.getItems().get(k);
                                if(null != step){
                                    ConfigSLANSection.Step slanStep = new ConfigSLANSection.Step();
                                    slanStep.setStepName(step.getName());
                                    slanStep.setTemperature(step.getTemperature());
                                    slanStep.setHoldTime(step.getHoldTime());
                                    slanStep.setStepType(Constants.PROGRAM_TYPE_AMP);
                                    slanStep.setStep_index(k);
                                    slanStep.setAcquisition(step.getIsAcquisition());
                                    slanStep.setSectionIndex(sectionAMP.getIdx());
                                    slanItems.add(slanStep);
                                }
                            }
                        }
                        slanSection.setItems(slanItems);
                        mSLANSections.add(slanSection);
                    }else if(section instanceof SectionMelt){
                        SectionMelt sectionMelt = (SectionMelt) section;
                        ConfigSLANSection slanSection = new ConfigSLANSection();
                        slanSection.setName(sectionMelt.getName());
                        slanSection.setIdx(sectionMelt.getIdx());
                        slanSection.setProgramType(Constants.PROGRAM_TYPE_MELT);//熔解段
                        slanSection.setRate(sectionMelt.getRate());
                        slanSection.setScanMode(sectionMelt.getScanMode());
                        slanSection.setInterval(sectionMelt.getInterval());
                        slanSection.setMeltHoldtime(sectionMelt.getHoldTime());

                        ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<>();
                        ConfigSLANSection.Step meltStep = new ConfigSLANSection.Step();
                        meltStep.setStepName("");
                        meltStep.setLeftTemperature(sectionMelt.getStartTemp());
                        meltStep.setRightTemperature(sectionMelt.getEndTemp());
                        meltStep.setSectionIndex(sectionMelt.getIdx());
                        meltStep.setStep_index(0);
                        meltStep.setAcquisition(true);
                        meltStep.setTemperature(sectionMelt.getEndTemp());
                        meltStep.setHoldTime(sectionMelt.getHoldTime());
                        meltStep.setStepType(Constants.PROGRAM_TYPE_MELT);
                        slanItems.add(meltStep);
                        slanSection.setItems(slanItems);
                        mSLANSections.add(slanSection);
                    }
                }

            }
        }
        return mSLANSections;
    }
    /*
     * Java文件操作 获取文件扩展名
     *
     */
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }
    /*
     * Java文件操作 获取不带扩展名的文件名
     */
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot >-1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }
}
