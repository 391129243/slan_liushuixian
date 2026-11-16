package com.paul.pipeline.manager;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.bean.CtRuleConclusion;
import com.gidi.bio_pcr.bean.PeakRuleConclusion;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.paul.pipeline.data.LocalFileHelper;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;

import java.io.File;
import java.util.List;

public class ExperimentManager {
    private static final String TAG = "ExperimentManager";
    private static ExperimentManager mInstance;
    public static ExperimentManager getInstance(){
        if (null == mInstance) {
            synchronized (ExperimentManager.class){
                if (null == mInstance){
                    mInstance = new ExperimentManager();
                }
            }
        }
        return  mInstance;
    }

    /**
     * 更新实验结果
     */
    public void updateExperiment() {
        String ex_testItem = PcrApplication.rootTempName;
        String ex_startTime = PcrApplication.ex_startTime;
        if (!StringUtil.isEmpty(ex_testItem) && !StringUtil.isEmpty(ex_startTime)) {
            updateBiowayResultToPCR(ex_startTime, ex_testItem);
            BiowayManager.getInstance().updateBiowayExperiment(ex_testItem, ex_startTime);
        } else {
            if (StringUtil.isEmpty(ex_testItem)) {
              //  Toast.makeText(getApplicationContext(),"测试项目格式有误",Toast.LENGTH_LONG).show();
                Log.e(TAG,"测试项目格式有误");
            } else if (StringUtil.isEmpty(ex_startTime)) {
              //  Toast.makeText(getApplicationContext(),"保存实验的时间格式有误",Toast.LENGTH_LONG).show();
                Log.e(TAG,"保存实验的时间格式有误");
            }
        }
    }
    /**
     * 更新PCR文件
     */
    private void updateBiowayResultToPCR(String startTime, String testItem) {
        Experiment experiment = BiowayManager.getInstance().getExperiment();
        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName;
        if(StringUtil.isEmpty(description)){
            subFirName = afterTime + "_" + testItem;
        }else{
            subFirName = afterTime + "_" + testItem + "_" + description;
        }
        File jsonDir = new File(FileUtils.BIOWAY_RESULT_PATH, subFirName);
        if (jsonDir.exists()) {
            File[] files = jsonDir.listFiles();
            if (files.length > 0) {
                for (File file : files) {
                    file.delete();
                }
            }
        }
        saveResultToPCR(experiment, subFirName);
    }

    /****************************************实验操作***********************************************/
    public void saveSLANExperiment(Context context, Experiment experiment) {
        boolean isSecret = PreferenceUtil.getInstance(context)
                .getBooleanValue(SharedConstant.SECRET_SETTING,false);
        String testItem = PcrApplication.rootTempName;
        String startTime = PcrApplication.ex_startTime;
        String afterTime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName;
        if(StringUtil.isEmpty(description)){
            subFirName = afterTime + "_" + testItem;
        }else{
            subFirName = afterTime + "_" + testItem + "_" + description;
        }
        Experiment experiment_device = experiment;
        if(isSecret){
            //打开准备的好的.pcr文件
            String path = LocalFileHelper.getPCRFilePath();
            Experiment des_experiment = null;
            if(!path.equals("")){
                des_experiment = BoWeiService.getInstance().openExperiment(path);
            }
            if(null != des_experiment){
                saveResultToPCR(des_experiment,subFirName);
                saveExperimentResult(des_experiment,subFirName);
                String src_starttime = experiment_device.getStartTime();
                String src_endtime = experiment_device.getEndTime();
                String file_name = testItem + PcrApplication.ex_startTime;
                des_experiment.setStartTime(src_starttime);
                des_experiment.setEndTime(src_endtime);
                des_experiment.setName(file_name);
            }else{
                saveResultToPCR(experiment_device, subFirName);
                saveExperimentResult(experiment_device,subFirName);

            }
        }else{
            //数据库
            saveExperimentResult(experiment_device,subFirName);
            //本地
            saveResultToPCR(experiment_device, subFirName);
        }
        //上传实验数据
    }

    public  void saveResultToPCR(Experiment experiment, String subFirName) {

        Log.e("RNA", "saveResultToPCR");
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return ;
        }
        if (experiment == null) {
            return;
        }

        File dir = new File(FileUtils.BIOWAY_RESULT_PATH, subFirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        //////蒋老师意思，屏蔽测试
        String saveFileName = DateFormatUtil.getFileName();
        String savePath = FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;
        BoWeiService.getInstance()
                .saveExperiment(experiment, saveFileName, savePath, new OnSaveExperimentListener() {
                    @Override
                    public void onSaveSuccess() {
                    //    Toast.makeText(getApplicationContext(),"保存PCR成功",Toast.LENGTH_LONG).show();
                        Log.e("save","onSaveSuccess" );
                    }

                    @Override
                    public void onSaveFail() {
                      //  Toast.makeText(getApplicationContext(),"保存PCR失败",Toast.LENGTH_LONG).show();
                        Log.e("save","onSaveFail" );
                    }
                });

    }
    private String description;
    private List<CtRuleConclusion> ctConclusions;
    private List<PeakRuleConclusion> peakConclusions;
    /**
     * 解析和保存数据库
     * @param experiment
     */
    private void saveExperimentResult(Experiment experiment,String subFirName) {

        try {
            if (experiment == null) {
                return;
            } else {
                //解析实验
                String endTime = DateFormatUtil.getSysFullTime();
                String savePath = FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;
                BiowayManager.getInstance().parseExperimentResult(experiment, ctConclusions,peakConclusions);
                BiowayManager.getInstance().saveBiowayExResultToDB(experiment,savePath,
                        PcrApplication.ex_startTime,endTime, PcrApplication.rootTempName,description,
                        ctConclusions,peakConclusions);
            }
        } catch (Exception e) {
            Log.e("getExperimentResult", "getExperimentResult = " + e.getMessage());
        }
    }

    /**
     * 停止实验
     */
    public void stopExperiment() {
        BoWeiService.getInstance().stopExperiment((byte) 0x01);
    }
}
