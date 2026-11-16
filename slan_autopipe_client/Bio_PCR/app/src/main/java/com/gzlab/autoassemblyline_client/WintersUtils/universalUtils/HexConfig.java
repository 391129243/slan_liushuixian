package com.gzlab.autoassemblyline_client.WintersUtils.universalUtils;

public class HexConfig {


    private String fileName = "first";

    private String description = ""; ///1单重 2多重

    private int type = 1; ///1单重 2多重

    private String fileHexPath = "";

    private String fileHexConfigPath = "";

    //卡盒
    private boolean casseteFlag = true;

    ///磁珠活化参数
    private double magneticVolume = 0.0f;
    private boolean magneticFlag = true;

    public int getMagneticChoice() {
        return magneticChoice;
    }

    public void setMagneticChoice(int magneticChoice) {
        this.magneticChoice = magneticChoice;
    }

    private int magneticChoice = 0;   ////0 样本管磁化   1 裂解液管磁化

    public int getFreeze_drying() {
        return freeze_drying;
    }

    public void setFreeze_drying(int freeze_drying) {
        this.freeze_drying = freeze_drying;
    }

    private int freeze_drying = 0;   ////0 液态体系   1 冻干体系



    ///裂解参数
    private double splitTemperature = 0.0f;
    private double splitLieJieYeVolume  = 0.0f;
    private double splitLieSampleVolume  = 0.0f;
    private double splitTime  = 0.0f;
    private boolean splitFlag = true;

    ///洗涤1参数

    private boolean washFlag = true;
    private int washStep = 2;
    private double wash1Temperature = 0.0f;
    private double wash1XiDiYeVolume = 0.0f;
    private double wash1Time = 0.0f;
    private boolean wash1Flag = true;

    ///洗涤22参数
    private double wash22Temperature = 0.0f;
    private double wash22XiDiYeVolume = 0.0f;
    private double wash22Time = 0.0f;
    private boolean wash22Flag = true;

    ///洗涤32参数
    private double wash32Temperature = 0.0f;
    private double wash32XiDiYeVolume = 420.0f;
    private double wash32Time = 0.0f;
    private boolean wash32Flag = true;

    ///洗涤33参数
    private double wash33Temperature = 0.0f;
    private double wash33XiDiYeVolume = 420.0f;
    private double wash33Time = 0.0f;
    private boolean wash33Flag = true;

    ///洗脱参数
    private boolean elutionFlag = true;
    private double elutionTemperature = 0.0f;
    private double elutionVolume = 0.0f;
    private double elutionTime = 0.0f;

    ///定量参数
    private boolean quantifyFlag = true;
    private double quantifyVolume = 0.0f;
    private double quantifyStandardVolume = 5.0f;

    ///二次参数 ----暂时参数固定
    private boolean secondQuantifyFlag = false;
    private double secondQuantifyParm = 0.0f;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isCasseteFlag() {
        return casseteFlag;
    }

    public void setCasseteFlag(boolean casseteFlag) {
        this.casseteFlag = casseteFlag;
    }

    public boolean isWash1Flag() {
        return wash1Flag;
    }

    public void setWash1Flag(boolean wash1Flag) {
        this.wash1Flag = wash1Flag;
    }

    public boolean isWash22Flag() {
        return wash22Flag;
    }

    public void setWash22Flag(boolean wash22Flag) {
        this.wash22Flag = wash22Flag;
    }

    public boolean isWash32Flag() {
        return wash32Flag;
    }

    public void setWash32Flag(boolean wash32Flag) {
        this.wash32Flag = wash32Flag;
    }

    public boolean isWash33Flag() {
        return wash33Flag;
    }

    public void setWash33Flag(boolean wash33Flag) {
        this.wash33Flag = wash33Flag;
    }

    public double getMagneticVolume() {
        return magneticVolume;
    }

    public void setMagneticVolume(double magneticVolume) {
        this.magneticVolume = magneticVolume;
    }

    public double getSplitTemperature() {
        return splitTemperature;
    }

    public void setSplitTemperature(double splitTemperature) {
        this.splitTemperature = splitTemperature;
    }

    public double getSplitLieJieYeVolume() {
        return splitLieJieYeVolume;
    }

    public void setSplitLieJieYeVolume(double splitLieJieYeVolume) {
        this.splitLieJieYeVolume = splitLieJieYeVolume;
    }

    public double getSplitLieSampleVolume() {
        return splitLieSampleVolume;
    }

    public void setSplitLieSampleVolume(double splitLieSampleVolume) {
        this.splitLieSampleVolume = splitLieSampleVolume;
    }

    public double getSplitTime() {
        return splitTime;
    }

    public void setSplitTime(double splitTime) {
        this.splitTime = splitTime;
    }

    public double getWash1Temperature() {
        return wash1Temperature;
    }

    public void setWash1Temperature(double washTemperature) {
        this.wash1Temperature = washTemperature;
    }

    public double getWash1XiDiYeVolume() {
        return wash1XiDiYeVolume;
    }

    public void setWash1XiDiYeVolume(double washXiDiYeVolume) {
        this.wash1XiDiYeVolume = washXiDiYeVolume;
    }

    public double getWash1Time() {
        return wash1Time;
    }

    public int getWashStep() {
        return washStep;
    }

    public void setWashStep(int washStep) {
        this.washStep = washStep;
    }

    public void setWash1Time(double wash1Time) {
        this.wash1Time = wash1Time;
    }

    public double getWash22Temperature() {
        return wash22Temperature;
    }

    public void setWash22Temperature(double wash22Temperature) {
        this.wash22Temperature = wash22Temperature;
    }

    public double getWash22XiDiYeVolume() {
        return wash22XiDiYeVolume;
    }

    public void setWash22XiDiYeVolume(double wash22XiDiYeVolume) {
        this.wash22XiDiYeVolume = wash22XiDiYeVolume;
    }

    public double getWash22Time() {
        return wash22Time;
    }

    public void setWash22Time(double wash22Time) {
        this.wash22Time = wash22Time;
    }

    public double getWash32Temperature() {
        return wash32Temperature;
    }

    public void setWash32Temperature(double wash32Temperature) {
        this.wash32Temperature = wash32Temperature;
    }

    public double getWash32XiDiYeVolume() {
        return wash32XiDiYeVolume;
    }

    public void setWash32XiDiYeVolume(double wash32XiDiYeVolume) {
        this.wash32XiDiYeVolume = wash32XiDiYeVolume;
    }

    public double getWash32Time() {
        return wash32Time;
    }

    public void setWash32Time(double wash32Time) {
        this.wash32Time = wash32Time;
    }

    public double getWash33Temperature() {
        return wash33Temperature;
    }

    public void setWash33Temperature(double wash33Temperature) {
        this.wash33Temperature = wash33Temperature;
    }

    public double getWash33XiDiYeVolume() {
        return wash33XiDiYeVolume;
    }

    public void setWash33XiDiYeVolume(double wash33XiDiYeVolume) {
        this.wash33XiDiYeVolume = wash33XiDiYeVolume;
    }

    public double getWash33Time() {
        return wash33Time;
    }

    public void setWash33Time(double wash33Time) {
        this.wash33Time = wash33Time;
    }

    public double getSecondParm() {
        return secondQuantifyParm;
    }

    public void setSecondParm(double secondParm) {
        this.secondQuantifyParm = secondParm;
    }

    public double getElutionTemperature() {
        return elutionTemperature;
    }

    public void setElutionTemperature(double elutionTemperature) {
        this.elutionTemperature = elutionTemperature;
    }

    public double getElutionVolume() {
        return elutionVolume;
    }

    public void setElutionVolume(double elutionVolume) {
        this.elutionVolume = elutionVolume;
    }

    public double getElutionTime() {
        return elutionTime;
    }

    public void setElutionTime(double elutionTime) {
        this.elutionTime = elutionTime;
    }



    public double getQuantifyVolume() {
        return quantifyVolume;
    }

    public void setQuantifyVolume(double quantifyVolume) {
        this.quantifyVolume = quantifyVolume;
    }

    public double getQuantifyStandardVolume() {
        return quantifyStandardVolume;
    }

    public void setQuantifyStandardVolume(double quantifyStandardVolume) {
        this.quantifyStandardVolume = quantifyStandardVolume;
    }

    public boolean isMagneticFlag() {
        return magneticFlag;
    }

    public void setMagneticFlag(boolean magneticFlag) {
        this.magneticFlag = magneticFlag;
    }

    public boolean isSplitFlag() {
        return splitFlag;
    }

    public void setSplitFlag(boolean splitFlag) {
        this.splitFlag = splitFlag;
    }

    public boolean isWashFlag() {
        return washFlag;
    }

    public void setWashFlag(boolean washFlag) {
        this.washFlag = washFlag;
    }

    public boolean isElutionFlag() {
        return elutionFlag;
    }

    public void setElutionFlag(boolean elutionFlag) {
        this.elutionFlag = elutionFlag;
    }

    public boolean isQuantifyFlag() {
        return quantifyFlag;
    }

    public void setQuantifyFlag(boolean quantifyFlag) {
        this.quantifyFlag = quantifyFlag;
    }

    public boolean isSecondQuantifyFlag() {
        return secondQuantifyFlag;
    }

    public void setSecondQuantifyFlag(boolean secondFlag) {
        this.secondQuantifyFlag = secondFlag;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getSecondQuantifyParm() {
        return secondQuantifyParm;
    }

    public void setSecondQuantifyParm(double secondQuantifyParm) {
        this.secondQuantifyParm = secondQuantifyParm;
    }

    public String getFileHexPath() {
        return fileHexPath;
    }

    public void setFileHexPath(String fileHexPath) {
        this.fileHexPath = fileHexPath;
    }

    public String getFileHexConfigPath() {
        return fileHexConfigPath;
    }

    public void setFileHexConfigPath(String fileHexConfigPath) {
        this.fileHexConfigPath = fileHexConfigPath;
    }

    @Override
    public String toString() {
        return "HexConfig{" +
                "fileName='" + fileName + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", fileHexPath='" + fileHexPath + '\'' +
                ", fileHexConfigPath='" + fileHexConfigPath + '\'' +
                '}';
    }
}
