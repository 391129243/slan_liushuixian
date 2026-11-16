package com.winters.boweilibrary.BoWei.bean;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;
import com.gidi.bio_pcr.WintersUtils.utils.GsonUtil;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

public class SaveExperimentTask extends AsyncTask<Experiment,Boolean,Boolean> {
    private Experiment experiment;
    private String saveName;
    private String savePath;

    private OnSaveExperimentListener onSaveExperimentListener;

    public void setOnSaveExperimentListener(OnSaveExperimentListener listener){
        this.onSaveExperimentListener = listener;
    }

    public SaveExperimentTask(Experiment experiment,String name,String path){
        this.experiment = experiment;
        this.saveName = name;
        this.savePath = path;
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
        Boolean result = saveExperiment(experiment,saveName,savePath);
        return result;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if(result){
            onSaveExperimentListener.onSaveSuccess();
        }else {
            onSaveExperimentListener.onSaveFail();
        }
        experiment = null;
    }

    private boolean saveExperiment(Experiment experiment,String saveName,String filePath){
        if(experiment == null){
            return false;
        }
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return false;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();

        try {
            String strJson_Config = gson.toJson(experiment);
            File dir = new File(filePath);
            if(!dir.exists()){
                dir.mkdirs();
            }
            String fileName = saveName + ".boweipcrResult";//保存的博维的文件的名称
            File resultFile = new File( filePath + fileName);
            resultFile.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(filePath + fileName);
            outputStream.write(strJson_Config.getBytes());
            outputStream.flush();
            outputStream.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
