package com.paul.pipeline.activity.viewmodel;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.db.BioWayWellResult;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.db.SLANWellResult;
import com.paul.pipeline.fragment.model.PipeDoorModel;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.RNAManager;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.fragment.model.PipeArmModel;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.fragment.model.PipeRNAModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;

import java.util.ArrayList;
import java.util.Objects;

public class PipeMainViewModel extends ViewModel {
    public PipeMainViewModel() {}

    public void initData(Context context){
        setPipeDoorModel(APLManager.getInstance().getPipeDoorModel());
        setARMList(ArmManager.getInstance().getArmlist());
        setTrayList(TrayManager.getInstance().getTraylist());
        setRNAList(new ArrayList<>(RNAManager.getInstance().getRnalist()));
        setPCRList(new ArrayList<>(PCRManager.getInstance().getPcrlist()));
    }

    //门状态
    private MutableLiveData<PipeDoorModel> doorModel = new MutableLiveData<>();
    public LiveData<PipeDoorModel> getPipeDoorModel() {
        return doorModel;
    }
    public void setPipeDoorModel(PipeDoorModel model){doorModel.setValue(model);}
    //打开主页面导航
    private MutableLiveData<Integer> navClickEvent = new MutableLiveData<>();
    public LiveData<Integer> getNavClickEvent() {
        return navClickEvent;
    }
    public void onNavClicked(int id) {
        navClickEvent.setValue(id);
    }
    //机械臂列表
    private MutableLiveData<ArrayList<PipeArmModel>> armList = new MutableLiveData<>();
    public LiveData<ArrayList<PipeArmModel>> getARMList() {
        return armList;
    }
    public void setARMList(ArrayList<PipeArmModel> list){armList.setValue(list);}
    public PipeArmModel findPipeArmModel(Context context, int index){
        if(armList!=null&&armList.getValue()!=null) {
            for (PipeArmModel model : Objects.requireNonNull(armList.getValue())) {
                if (model.index.get() == index) {
                    return model;
                }
            }
        }
        return new PipeArmModel(index,context.getString(R.string.pipe_main_rna_name) + index);
    }
    //打开机械臂详情
    private MutableLiveData<Integer> armClickEvent = new MutableLiveData<>();
    public LiveData<Integer> getArmClickEvent() {return armClickEvent;}
    public void setArmClickEvent(int id) {armClickEvent.setValue(id);}

    //托盘内容变更
    private final MutableLiveData<Integer> trayChanged = new MutableLiveData<>();
    public void setTrayChanged(Integer item) {
        trayChanged.setValue(item);
    }
    public LiveData<Integer> getTrayChanged() {
        return trayChanged;
    }
    //打开托盘详情
    private MutableLiveData<Integer> trayClickEvent = new MutableLiveData<>();
    public LiveData<Integer> getTrayClickEvent() {return trayClickEvent;}
    public void onTrayClicked(int id) {trayClickEvent.setValue(id);}
    //托盘界面初始化
    private MutableLiveData<Integer> trayClear = new MutableLiveData<>();
    public LiveData<Integer> getTrayClear() {return trayClear;}
    public void setTrayClear(int id) {trayClear.setValue(id);}
    //托盘列表
    private MutableLiveData<ArrayList<PipeTrayModel>> trayList = new MutableLiveData<>();
    public LiveData<ArrayList<PipeTrayModel>> getTrayList() {
        return trayList;
    }
    public void setTrayList(ArrayList<PipeTrayModel> list){trayList.setValue(list);}

    //托盘数据
    public PipeTrayModel findPipeTrayModel(Context context, int index){
        if(trayList!=null&&trayList.getValue()!=null) {
            for (PipeTrayModel model : Objects.requireNonNull(trayList.getValue())) {
                if (model.index.get() == index) {
                    return model;
                }
            }
        }
        return new PipeTrayModel(index,context.getString(R.string.pipe_main_trayname) + index);
    }
    public void clearPipeTrayModel(Context context, int index){
        if(trayList!=null&&trayList.getValue()!=null) {
            ArrayList<PipeTrayModel> list = trayList.getValue();
            for (int i=0;i<list.size();i++) {
                PipeTrayModel model = list.get(i);
                if (model.index.get() == index) {
                    list.get(i).initModel(index,context.getString(R.string.pipe_main_trayname) + index);
//                    list.set(i,new PipeTrayModel(index,context.getString(R.string.pipe_main_trayname) + index));
                    trayList.setValue(list);
                    break;
                }
            }
        }
    }

    //RNA列表
    private MutableLiveData<ArrayList<PipeRNAModel>> rnaList = new MutableLiveData<>();
    public LiveData<ArrayList<PipeRNAModel>> getRNAList() {
        return rnaList;
    }
    public void setRNAList(ArrayList<PipeRNAModel> list){rnaList.setValue(list);}
    public PipeRNAModel findPipeRNAModel(Context context, int index){
        if(rnaList!=null&&rnaList.getValue()!=null) {
            for (PipeRNAModel model : Objects.requireNonNull(rnaList.getValue())) {
                if (model.index.get() == index) {
                    return model;
                }
            }
        }
        return new PipeRNAModel(index,context.getString(R.string.pipe_main_rna_name) + index);
    }

    public void clearPipeRNAModel(Context context, int index){
        if(rnaList!=null&&rnaList.getValue()!=null) {
            ArrayList<PipeRNAModel> list = rnaList.getValue();
            for (int i=0;i<list.size();i++) {
                PipeRNAModel model = list.get(i);
                if (model.index.get() == index) {
                    list.get(i).initModel(index,context.getString(R.string.pipe_main_rna_name) + index);
//                    list.set(i,new PipeRNAModel(index,context.getString(R.string.pipe_main_rna_name) + index));
                    rnaList.setValue(list);
                    break;
                }
            }
        }
    }
    //RNA界面初始化
    private MutableLiveData<Integer> rnaClear = new MutableLiveData<>();
    public LiveData<Integer> getRnaClear() {return rnaClear;}
    public void setRnaClear(int id) {rnaClear.setValue(id);}
    //打开RNA详情
    private MutableLiveData<Integer> rnaClickEvent = new MutableLiveData<>();
    public LiveData<Integer> getRnaClickEvent() {return rnaClickEvent;}
    public void setRnaClicked(int id) {rnaClickEvent.setValue(id);}

    //PCR列表
    private MutableLiveData<ArrayList<PipePCRModel>> pcrList = new MutableLiveData<>();
    public LiveData<ArrayList<PipePCRModel>> getPCRList() {
        return pcrList;
    }
    public void setPCRList(ArrayList<PipePCRModel> list){pcrList.setValue(list);}
    public PipePCRModel findPipePCRModel(Context context, int index){
        if(pcrList!=null&&pcrList.getValue()!=null) {
            for (PipePCRModel model : Objects.requireNonNull(pcrList.getValue())) {
                if (model.index.get() == index) {
                    return model;
                }
            }
        }
        return new PipePCRModel(index,context.getString(R.string.pipe_main_pcr_name) + index);
    }
    public void clearPipePCRModel(Context context, int index){
        if(pcrList!=null&&pcrList.getValue()!=null) {
            ArrayList<PipePCRModel> list = pcrList.getValue();
            for (int i=0;i<list.size();i++) {
                PipePCRModel model = list.get(i);
                if (model.index.get() == index) {
                    list.get(i).initModel(index,context.getString(R.string.pipe_main_pcr_name) + index);
//                    list.set(i,new PipePCRModel(index,context.getString(R.string.pipe_main_pcr_name) + index));
                    pcrList.setValue(list);
                    break;
                }
            }
        }
    }
    //PCR界面初始化
    private MutableLiveData<Integer> pcrClear = new MutableLiveData<>();
    public LiveData<Integer> getPcrClear() {return pcrClear;}
    public void setPcrClear(int id) {pcrClear.setValue(id);}
    //打开PCR详情
    private MutableLiveData<Integer> pcrClickEvent = new MutableLiveData<>();
    public LiveData<Integer> getPcrClickEvent() {return pcrClickEvent;}
    public void setPcrClicked(int id) {pcrClickEvent.setValue(id);}

    private final MutableLiveData<String> toastInfo = new MutableLiveData<>();
    public void setToastInfo(String item) {toastInfo.setValue(item);}
    public LiveData<String> getToastInfo() {
        return toastInfo;
    }

    private final MutableLiveData<String> alertInfo = new MutableLiveData<>();
    public void setAlertInfo(String item) {alertInfo.setValue(item);}
    public LiveData<String> getAlertInfo() {
        return alertInfo;
    }

    private final MutableLiveData<String> progressInfo = new MutableLiveData<>();
    public void setProgressInfo(String item) {
        progressInfo.setValue(item);
    }
    public LiveData<String> getProgressInfo() {
        return progressInfo;
    }

    //pcr实验数据刷新
    private MutableLiveData<BioWayWellResult> pcrBioWayWellResult = new MutableLiveData<>();
    public MutableLiveData<BioWayWellResult> getPcrBioWayWellResult() {
        return pcrBioWayWellResult;
    }
    public void setPcrBioWayWellResult(BioWayWellResult index) {
        this.pcrBioWayWellResult.setValue(index);
    }

    //点击样本记录获取样本详情
    //pcr实验数据刷新
    private MutableLiveData<SLANWellResult> pcrSLANWellResult = new MutableLiveData<>();

    public MutableLiveData<SLANWellResult> getPcrSLANWellResult() {
        return pcrSLANWellResult;
    }

    public void setPcrSLANWellResult(SLANWellResult index) {
        this.pcrSLANWellResult.setValue(index);
    }


    //pcr实验数据刷新
    private MutableLiveData<ExperimentModel> pcrExperimentModel = new MutableLiveData<>();
    public MutableLiveData<ExperimentModel> getPcrExperimentModel() {
        return pcrExperimentModel;
    }
    public void setPcrExperimentModel(ExperimentModel index) {
        Log.i("ResultEx","==setPcrExperimentModel" );
        this.pcrExperimentModel.setValue(index);
    }

    //对应托盘的新增或修改
    private MutableLiveData<Integer> trayEdittype = new MutableLiveData<>();
    public void setTrayEdittype(Integer model) {
        trayEdittype.setValue(model);
    }
    public LiveData<Integer> getTrayEdittype() {
        return trayEdittype;
    }
}
