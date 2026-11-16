package com.gidi.bio_pcr.slan;

import android.os.AsyncTask;
import android.os.Environment;

import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hongshi.slansdk.model.experiment.Experiment;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Type;


public class SaveSLANJsonAsyncTask extends AsyncTask<Experiment,Boolean,Boolean> {
    private Experiment mExperiment;
    private String saveFolderPath;

    private OnSaveExperimentListener onSaveExperimentListener;


    public void setOnSaveExperimentListener(OnSaveExperimentListener listener){
        this.onSaveExperimentListener = listener;
    }

    public SaveSLANJsonAsyncTask(Experiment experiment,String saveFolderPath){
        this.mExperiment = experiment;
        this.saveFolderPath = saveFolderPath;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected void onCancelled(Boolean result) {
        super.onCancelled(result);
    }

    @Override
    protected Boolean doInBackground(Experiment... experiments) {
        Boolean result = saveResultToJSON(mExperiment,saveFolderPath);
        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(null != onSaveExperimentListener){
            if(result){
                onSaveExperimentListener.onSaveSuccess();
            }else{
                onSaveExperimentListener.onSaveFail();
            }
        }
    }



    /**
     *
     * @param experiment //
     * @return
     */
    public boolean saveResultToJSON(Experiment experiment,String saveFoldPath){
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
       /* TypeUtils.compatibleWithJavaBean =true;
        String save_experiment = JSON.toJSONString(experiment, SerializerFeature.DisableCircularReferenceDetect
        ,SerializerFeature.WriteNullStringAsEmpty);*/
        String save_experiment = gson.toJson(experiment);
        //开始时间作为子文件夹+路径
        File dir = new File(saveFoldPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            String saveFileName = DateFormatUtil.getFileName() + FileUtils.JSON_SUFFIX;
            File file = new File(dir, saveFileName);

            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(save_experiment.getBytes());
            fos.flush();
            fos.close();
            return true;
        }catch (Exception e){
            return false;
        }
    }



}
