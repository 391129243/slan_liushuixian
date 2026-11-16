package com.winters.boweilibrary.BoWei.ExperimentData;

import android.util.Log;

import com.hongshi.slansdk.model.experiment.Project;
import com.winters.boweilibrary.BoWei.ExperimentData.Enum.SampleType;
import com.winters.boweilibrary.BoWei.model.experiment.Well;

import java.util.ArrayList;
import java.util.List;

public class Experiment {


    //创建 SingleObject 的一个对象
    private static Experiment instance;

    private static String[] Dyes = { "FAM", "HEX", "ROX", "CY5" };

    //获取唯一可用的对象
    public static Experiment getInstance()
    {
        if(instance == null) {
            instance = new Experiment();
        }
        return instance;
    }


    public static void setExperimentWells(ArrayList<Well> wells){
        if(wells != null && wells.size()>0){
            if(instance.getWellResult() != null){
                instance.getWellResult().clear();
                instance.getTemperatures().clear();
            }
            for(int i = 0; i < 8; i++) {
                WellResult item = new WellResult();

                item.setID("A" + (i + 1));

                item.setSampleType(SampleType.Unknown);

                item.setSampleTypeName("A" + (i + 1));

                item.setSampleName(wells.get(i).getSampleName());//身份證號碼

                PatientInfo patientInfo = new PatientInfo();

                item.setPatientInfo(patientInfo);

                item.getPatientInfo().setID("A" + (i + 1));

                List<TargetResult> targetResults = new ArrayList<>();

                for (int j = 0; j < 4; j++) {
                    TargetResult targetResult = new TargetResult();

                    targetResult.setChannel(j+1);///////////???

                    targetResult.setDye(Dyes[j]);

                    targetResult.setName(Dyes[j]);

                    targetResult.setTubeName(wells.get(i).getTubeName());

                    targetResult.setTubeNo(j+1);

                    targetResults.add(targetResult);
                }

                item.setTargetResults(targetResults);

                instance.getWellResult().add(item);
            }
        }else{
            if(instance.getWellResult() != null){
                instance.getWellResult().clear();
            }
            for(int i = 0; i < 8; i++) {
                WellResult item = new WellResult();

                item.setID("A" + (i + 1));

                item.setSampleType(SampleType.Unknown);

                item.setSampleTypeName("A" + (i + 1));

                item.setSampleName("A" + (i + 1));//身份證號碼

                PatientInfo patientInfo = new PatientInfo();

                item.setPatientInfo(patientInfo);

                item.getPatientInfo().setID("A" + (i + 1));

                List<TargetResult> targetResults = new ArrayList<>();

                for (int j = 0; j < 4; j++) {
                    TargetResult targetResult = new TargetResult();

                    targetResult.setChannel(j+1);///////////???

                    targetResult.setDye(Dyes[j]);

                    targetResult.setName(Dyes[j]);

                    targetResult.setTubeName(Dyes[j]);

                    targetResult.setTubeNo(j+1);

                    targetResults.add(targetResult);
                }

                item.setTargetResults(targetResults);

                instance.getWellResult().add(item);
            }
        }

        Log.i("NNN","instance.getWellResult()" + instance.getWellResult().size());
    }

    public static void instanceReset()
    {
        if (instance != null) {
            instance = null;
        }

    }


    /// <summary>
    /// 实验名称
    /// </summary>
    private String Name = "";

    /// <summary>
    /// 实验开始时间
    /// </summary>
    private String StartTime ="";

    /// <summary>
    /// 实验开始结束时间
    /// </summary>
    private String EndTime ="";

    /// <summary>
    /// 操作者
    /// </summary>
    private String Operator ="";

    /// <summary>
    /// 审核者
    /// </summary>
    private String Auditor ="";

    /// <summary>
    /// 备注
    /// </summary>
    public String Note ="";

    /// <summary>
    /// 版号
    /// </summary>
    private String PlateNo ="";

    /// <summary>
    /// 批次号
    /// </summary>
    private String BatchNo ="";



    /// <summary>
    /// 自定义列名称(与患者信息中的CustomData对应)
    /// </summary>
    private List<String> CustomColumns = new ArrayList<>();


    /// <summary>
    /// 反应孔结果
    /// </summary>
    private List<WellResult> WellResult = new ArrayList<WellResult>();

    public static void setInstance(Experiment instance) {
        Experiment.instance = instance;
    }

    public static String[] getDyes() {
        return Dyes;
    }

    public static void setDyes(String[] dyes) {
        Experiment.Dyes = dyes;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public String getStartTime() {
        return StartTime;
    }

    public void setStartTime(String startTime) {
        this.StartTime = startTime;
    }

    public String getEndTime() {
        return EndTime;
    }

    public void setEndTime(String endTime) {
        this.EndTime = endTime;
    }


    public String getOperator() {
        return Operator;
    }

    public void setOperator(String operator) {
        Operator = operator;
    }

    public String getAuditor() {
        return Auditor;
    }

    public void setAuditor(String auditor) {
        this.Auditor = auditor;
    }

    public String getNote() {
        return Note;
    }

    public void setNote(String note) {
        this.Note = note;
    }

    public String getPlateNo() {
        return PlateNo;
    }

    public void setPlateNo(String plateNo) {
        this.PlateNo = plateNo;
    }

    public String getBatchNo() {
        return BatchNo;
    }

    public void setBatchNo(String batchNo) {
        this.BatchNo = batchNo;
    }

    public List<String> getCustomColumns() {
        return CustomColumns;
    }

    public void setCustomColumns(List<String> customColumns) {
        this.CustomColumns = customColumns;
    }

    public List<WellResult> getWellResult() {
        return WellResult;
    }

    public void setWellResult(List<WellResult> wellResult) {
        this.WellResult = wellResult;
    }



    private Project Project = new Project();

    public Project getProject() {
        return Project;
    }

    public void setProject(Project project) {
        this.Project = project;
    }

    private ArrayList<Temperature> Temperatures = new ArrayList<Temperature>();

    public ArrayList<Temperature> getTemperatures() {
        return Temperatures;
    }

    public void setTemperatures(ArrayList<Temperature> temperatures) {
        Temperatures = temperatures;
    }

    public Experiment(String name, String startTime, String endTime, String operatorName, String auditor, String note, String plateNo, String batchNo, List<String> customColumns, List<WellResult> wellResult, Project project) {
        this.Name = name;
        this.StartTime = startTime;
        this.EndTime = endTime;
        this.Operator = operatorName;
        this.Auditor = auditor;
        this.Note = note;
        this.PlateNo = plateNo;
        this.BatchNo = batchNo;
        this.CustomColumns = customColumns;
        this.WellResult = wellResult;
        this.Project = project;
    }



    public Experiment() {
    }


    /////Loess滤波步长
    private int stepPreLoess = 100;
    private int stepPostLoess = 100;

    /////////////////////////////新增波形优化参数
    //////临时加入后续需要放进Project的meltParameter里边
    ///private static String[] Dyes = { "FAM", "HEX", "ROX", "CY5" };
    ///true, 2, 5, 1.5, 2,0.5, 0.3
//    bool	optimization_enable	峰型优化使能
//    double	enhance_lower	增强峰高阈值下限
//    double	enhance_upper	增强峰高阈值上限
//    double	enhance_gain	增强增益
//    double	weaken	削弱峰高阈值
//    double	weaken_gain	削弱增益
//    double	slope_thres	斜率阈值
    private boolean optimization_enabl_FAM = true;
    private double enhance_lower_FAM = 20;
    private double enhance_upper_FAM = 50;
    private double enhance_gain_FAM = 1.5;
    private double weaken_FAM = 20;
    private double weaken_gain_FAM = 0.5;
    private double slope_thres_FAM = 0.3;

    private boolean optimization_enabl_HEX = true;
    private double enhance_lower_HEX = 20;
    private double enhance_upper_HEX = 50;
    private double enhance_gain_HEX = 1.5;
    private double weaken_HEX = 20;
    private double weaken_gain_HEX = 0.5;
    private double slope_thres_HEX = 0.3;

    private boolean optimization_enabl_ROX = true;
    private double enhance_lower_ROX = 20;
    private double enhance_upper_ROX = 50;
    private double enhance_gain_ROX = 1.5;
    private double weaken_ROX = 20;
    private double weaken_gain_ROX = 0.5;
    private double slope_thres_ROX = 0.3;


    private boolean optimization_enabl_CY5 = true;
    private double enhance_lower_CY5 = 20;
    private double enhance_upper_CY5 = 50;
    private double enhance_gain_CY5 = 1.5;
    private double weaken_CY5 = 20;
    private double weaken_gain_CY5 = 0.5;
    private double slope_thres_CY5 = 0.3;

    public boolean isOptimization_enabl_FAM() {
        return optimization_enabl_FAM;
    }

    public void setOptimization_enabl_FAM(boolean optimization_enabl_FAM) {
        this.optimization_enabl_FAM = optimization_enabl_FAM;
    }

    public double getEnhance_lower_FAM() {
        return enhance_lower_FAM;
    }

    public void setEnhance_lower_FAM(double enhance_lower_FAM) {
        this.enhance_lower_FAM = enhance_lower_FAM;
    }

    public double getEnhance_upper_FAM() {
        return enhance_upper_FAM;
    }

    public void setEnhance_upper_FAM(double enhance_upper_FAM) {
        this.enhance_upper_FAM = enhance_upper_FAM;
    }

    public double getEnhance_gain_FAM() {
        return enhance_gain_FAM;
    }

    public void setEnhance_gain_FAM(double enhance_gain_FAM) {
        this.enhance_gain_FAM = enhance_gain_FAM;
    }

    public double getWeaken_FAM() {
        return weaken_FAM;
    }

    public void setWeaken_FAM(double weaken_FAM) {
        this.weaken_FAM = weaken_FAM;
    }

    public double getWeaken_gain_FAM() {
        return weaken_gain_FAM;
    }

    public void setWeaken_gain_FAM(double weaken_gain_FAM) {
        this.weaken_gain_FAM = weaken_gain_FAM;
    }

    public double getSlope_thres_FAM() {
        return slope_thres_FAM;
    }

    public void setSlope_thres_FAM(double slope_thres_FAM) {
        this.slope_thres_FAM = slope_thres_FAM;
    }

    public boolean isOptimization_enabl_HEX() {
        return optimization_enabl_HEX;
    }

    public void setOptimization_enabl_HEX(boolean optimization_enabl_HEX) {
        this.optimization_enabl_HEX = optimization_enabl_HEX;
    }

    public double getEnhance_lower_HEX() {
        return enhance_lower_HEX;
    }

    public void setEnhance_lower_HEX(double enhance_lower_HEX) {
        this.enhance_lower_HEX = enhance_lower_HEX;
    }

    public double getEnhance_upper_HEX() {
        return enhance_upper_HEX;
    }

    public void setEnhance_upper_HEX(double enhance_upper_HEX) {
        this.enhance_upper_HEX = enhance_upper_HEX;
    }

    public double getEnhance_gain_HEX() {
        return enhance_gain_HEX;
    }

    public void setEnhance_gain_HEX(double enhance_gain_HEX) {
        this.enhance_gain_HEX = enhance_gain_HEX;
    }

    public double getWeaken_HEX() {
        return weaken_HEX;
    }

    public void setWeaken_HEX(double weaken_HEX) {
        this.weaken_HEX = weaken_HEX;
    }

    public double getWeaken_gain_HEX() {
        return weaken_gain_HEX;
    }

    public void setWeaken_gain_HEX(double weaken_gain_HEX) {
        this.weaken_gain_HEX = weaken_gain_HEX;
    }

    public double getSlope_thres_HEX() {
        return slope_thres_HEX;
    }

    public void setSlope_thres_HEX(double slope_thres_HEX) {
        this.slope_thres_HEX = slope_thres_HEX;
    }

    public boolean isOptimization_enabl_ROX() {
        return optimization_enabl_ROX;
    }

    public void setOptimization_enabl_ROX(boolean optimization_enabl_ROX) {
        this.optimization_enabl_ROX = optimization_enabl_ROX;
    }

    public double getEnhance_lower_ROX() {
        return enhance_lower_ROX;
    }

    public void setEnhance_lower_ROX(double enhance_lower_ROX) {
        this.enhance_lower_ROX = enhance_lower_ROX;
    }

    public double getEnhance_upper_ROX() {
        return enhance_upper_ROX;
    }

    public void setEnhance_upper_ROX(double enhance_upper_ROX) {
        this.enhance_upper_ROX = enhance_upper_ROX;
    }

    public double getEnhance_gain_ROX() {
        return enhance_gain_ROX;
    }

    public void setEnhance_gain_ROX(double enhance_gain_ROX) {
        this.enhance_gain_ROX = enhance_gain_ROX;
    }

    public double getWeaken_ROX() {
        return weaken_ROX;
    }

    public void setWeaken_ROX(double weaken_ROX) {
        this.weaken_ROX = weaken_ROX;
    }

    public double getWeaken_gain_ROX() {
        return weaken_gain_ROX;
    }

    public void setWeaken_gain_ROX(double weaken_gain_ROX) {
        this.weaken_gain_ROX = weaken_gain_ROX;
    }

    public double getSlope_thres_ROX() {
        return slope_thres_ROX;
    }

    public void setSlope_thres_ROX(double slope_thres_ROX) {
        this.slope_thres_ROX = slope_thres_ROX;
    }

    public boolean isOptimization_enabl_CY5() {
        return optimization_enabl_CY5;
    }

    public void setOptimization_enabl_CY5(boolean optimization_enabl_CY5) {
        this.optimization_enabl_CY5 = optimization_enabl_CY5;
    }

    public double getEnhance_lower_CY5() {
        return enhance_lower_CY5;
    }

    public void setEnhance_lower_CY5(double enhance_lower_CY5) {
        this.enhance_lower_CY5 = enhance_lower_CY5;
    }

    public double getEnhance_upper_CY5() {
        return enhance_upper_CY5;
    }

    public void setEnhance_upper_CY5(double enhance_upper_CY5) {
        this.enhance_upper_CY5 = enhance_upper_CY5;
    }

    public double getEnhance_gain_CY5() {
        return enhance_gain_CY5;
    }

    public void setEnhance_gain_CY5(double enhance_gain_CY5) {
        this.enhance_gain_CY5 = enhance_gain_CY5;
    }

    public double getWeaken_CY5() {
        return weaken_CY5;
    }

    public void setWeaken_CY5(double weaken_CY5) {
        this.weaken_CY5 = weaken_CY5;
    }

    public double getWeaken_gain_CY5() {
        return weaken_gain_CY5;
    }

    public void setWeaken_gain_CY5(double weaken_gain_CY5) {
        this.weaken_gain_CY5 = weaken_gain_CY5;
    }

    public double getSlope_thres_CY5() {
        return slope_thres_CY5;
    }

    public void setSlope_thres_CY5(double slope_thres_CY5) {
        this.slope_thres_CY5 = slope_thres_CY5;
    }
}
