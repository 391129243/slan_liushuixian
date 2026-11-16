package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentTrayManageBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.adapter.DetailSampleHoleListAdapter;
import com.paul.pipeline.adapter.DispatchListAdapter;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.fragment.model.PipeRNAModel;
import com.paul.pipeline.fragment.viewmodel.TrayManageViewModel;
import com.paul.pipeline.listener.OnItemChildClickListener;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.RNAManager;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;

import java.util.ArrayList;

public class TrayManageFragment extends Fragment {

    private TrayManageViewModel mViewModel;

    public static TrayManageFragment newInstance() {
        return new TrayManageFragment();
    }

    FragmentTrayManageBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_tray_manage, container, false);
        initBinding();
        return binding.getRoot();
      //  return inflater.inflate(R.layout.fragment_tray_manage, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(dispatchListAdapter!=null){
            dispatchListAdapter.notifyDataSetChanged();
        }
    }

    private void initBinding() {
        binding.untouchview.setOnClickListener(v -> {
            //不实现，禁止外围区域触摸
        });
        binding.addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(trayModel!=null) {
                    if(trayModel.flagstatus.get()!=null) {
                        int status = trayModel.flagstatus.get();
                        switch (status) {
                            case ConstantData.FLAG_FREE:
                                mainViewModel.onNavClicked(ConstantData.NAV_TRAYDETAIL);
                                mainViewModel.setTrayEdittype(ConstantData.TRAY_Add);
                                break;
                            case ConstantData.FLAG_PAUSE:
                                showLongToast("暂停状态，请结束还盘后进行新增操作！");
                                break;
                            case ConstantData.FLAG_STOP:
                                showLongToast("停止状态，请结束还盘后进行新增操作！");
                                break;
                            case ConstantData.FLAG_RUN:
                                showLongToast("运行状态，请结束还盘后进行新增操作！");
                                break;
                        }
                    }
                }
            }
        });
//        binding.modifyBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mainViewModel.onNavClicked(ConstantData.NAV_TRAYDETAIL);
//                mainViewModel.setTrayEdittype(ConstantData.TRAY_Edit);
//            }
//        });
        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedDispatchModel!=null){
                    TrayManager.getInstance().continueTray(trayModel.index.get());
                    TrayManager.getInstance().saveModel(trayModel.index.get());
                    executeDispatchTask();
                    showLongToast("继续执行！");
                }else{
                    showLongToast("请选择目标！");
                }
            }
        });
        binding.pauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(trayModel!=null&&trayModel.flagstatus.get()!=null) {
                    int status = trayModel.flagstatus.get();
                    switch (status) {
                        case ConstantData.FLAG_FREE:
                            trayModel.flagstatus.set(ConstantData.FLAG_PAUSE);
                            TrayManager.getInstance().stopTray(trayModel.index.get());
                            TrayManager.getInstance().saveModel(trayModel.index.get());
                            break;
                        case ConstantData.FLAG_PAUSE:
                            showLongToast("已经暂停！");
                            break;
                        case ConstantData.FLAG_STOP:
                            showLongToast("已经处于停止状态！");
                            break;
                    }
                }
            }
        });
        binding.stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrayDispatch();
            }
        });
        binding.clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(trayModel!=null) {
                    trayModel.flagstatus.set(ConstantData.FLAG_FREE);
                }
            }
        });
        binding.ivBack.setOnClickListener(v -> close());
    }
    private void stopTrayDispatch(){
        if(trayModel!=null) {
            trayModel.flagstatus.set(ConstantData.FLAG_STOP);
            if (trayModel.exist.get() != null && !trayModel.exist.get()) {
                if (trayModel.index.get() != null) {
                    DispatchModel dm = TrayManager.getInstance().getCurrentDispatch(trayModel.index.get());
                    if (dm!=null && dm.targettype.get()!=null){
                        if(dm.targettype.get() == ConstantData.DEVICE_PCR){
                            if(dm.target.get()!=null) {
                                stopPcrwork(dm.target.get());
                            }else{
                                showLongToast("目标位置信息为空！");
                            }
                        }else if(dm.targettype.get() == ConstantData.DEVICE_RNA){
                            if(dm.target.get()!=null) {
                                stopRnawork(dm.target.get());
                            }else{
                                showLongToast("目标位置信息为空！");
                            }
                        }else{
                            showLongToast("调度位置信息为空！");
                        }
                    }else{
                        showLongToast("调度信息为空！");
                    }
                }
            }else{
                showLongToast("托盘调度停止成功！");
            }
        }else{
            showLongToast("托盘信息为空！");
        }
    }
    private void stopRnawork(int index){
        boolean result = RNAManager.getInstance().stopRnaWork(index);
        PipeRNAModel model= RNAManager.getInstance().getPipeRNAModel(index);
        if (model!=null && model.usererror.get() != null) {
            model.usererror.set(1);
        }
        APLManager.getInstance().NotifyRnaTrayError(index);
        if(result) {
            showLongToast("RNA" + index + "停止成功，" + getString(R.string.do_wait));
        }else{
            showLongToast("RNA" + index + "停止失败！");
        }
    }
    private void stopPcrwork(int index){
        boolean result = PCRManager.getInstance().stop(index);
        PipePCRModel model= PCRManager.getInstance().getPipePCRModel(index);
        if (model!=null && model.usererror.get() != null) {
            model.usererror.set(1);
        }
        APLManager.getInstance().NotifyPcrTrayError(index);
        if(result) {
            showLongToast("PCR" + index + "停止成功，" + getString(R.string.do_wait));
        }else{
            showLongToast("PCR" + index + "停止失败！");
        }
    }
    private void executeDispatchTask(){
        int tray = trayModel.index.get();
        int dispatchindex = selectedDispatchModel.index.get();
        ArrayList<DispatchModel> dispatchlist = TrayManager.getInstance().getPipeTrayModel(tray).dispatchlist;
        for(int i=0;i<dispatchlist.size();i++) {
            if(dispatchindex==dispatchlist.get(i).index.get()) {
                DispatchModel dm = dispatchlist.get(i);
                if(dm.status.get()== ConstantData.STATUS_FREE) {
                    if (dm.targettype.get() == ConstantData.DEVICE_RNA) {
                        PipeRNAModel rm = RNAManager.getInstance().getFreeModel();
                        if(rm!=null) {
                            dm.target.set(rm.index.get());
                            dm.status.set(ConstantData.STATUS_RUN);

                            if(i>=1 && dispatchlist.get(i-1).nextdevice.get()!=null) {
                                RNAManager.getInstance().setCurrentTemplate(rm.index.get(), dm, dispatchlist.get(i-1).nextdevice.get());
                            }else{
                                RNAManager.getInstance().setCurrentTemplate(rm.index.get(), dm, 0);
                            }
                            TrayManager.getInstance().setCurrentDispatch(tray, dispatchindex);
                        }
                    } else if (dm.targettype.get() == ConstantData.DEVICE_PCR) {
                        PipePCRModel rm = PCRManager.getInstance().getFreeModel();
                        if(rm!=null) {
                            dm.target.set(rm.index.get());
                            dm.status.set(ConstantData.STATUS_RUN);

                            if(i>=1 && dispatchlist.get(i-1).nextdevice.get()!=null) {
                                PCRManager.getInstance().setCurrentTemplate(rm.index.get(), dm,dispatchlist.get(i-1).nextdevice.get());
                            }else{
                                PCRManager.getInstance().setCurrentTemplate(rm.index.get(), dm,0);
                            }
                            TrayManager.getInstance().setCurrentDispatch(tray, dispatchindex);
                        }
                    }
                }
                break;
            }
        }
    }

    private void close(){
        mainViewModel.onNavClicked(ConstantData.NAV_MAIN);
    }

    private void showLongToast(String text){
        Toast.makeText(getContext(),text, Toast.LENGTH_LONG).show();
    }

    private int trayindex;
    PipeMainViewModel mainViewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(TrayManageViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainViewModel.getTrayClickEvent().observe(requireActivity(), s -> {
            if(1<=s&&s<=8){
                trayindex = s;
                initTraymodel();
            }
        });
        mainViewModel.getTrayChanged().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(trayindex==integer) {
                 //   Log.e("ArmTaskWorker", "manager tray :"+ trayindex);
                    initTraymodel();
                }
            }
        });
    }
    PipeTrayModel trayModel;
    private void initTraymodel(){
        if(1<=trayindex&&trayindex<=8) {
            trayModel = mainViewModel.findPipeTrayModel(getContext(), trayindex);
            binding.setTrayModel(trayModel);
            binding.executePendingBindings();
            initDispatchCycleView();
            initHoleListCycleView();
        }
    }
    DetailSampleHoleListAdapter sampleHoleListAdapter;
    private void initHoleListCycleView(){
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),2);
        //1.设置LinearLayoutManager ListView
        binding.sampleholeRv.setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        //2.设置固定大小
        binding.sampleholeRv.setHasFixedSize(true);
        //3.设置Adapter
        sampleHoleListAdapter = new DetailSampleHoleListAdapter(getContext(), trayModel.holes, new OnItemChildClickListener() {
            @Override
            public void onItemChildClick(View view, int position) {

            }
        });
        sampleHoleListAdapter.setCircleVisible(false);
        binding.sampleholeRv.setAdapter(sampleHoleListAdapter);
        sampleHoleListAdapter.notifyDataSetChanged();
    }

    DispatchListAdapter dispatchListAdapter;
    private void initDispatchCycleView(){
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
        //1.设置LinearLayoutManager ListView
        binding.dispatchRv.setLayoutManager(layoutManager);
        binding.dispatchRv.setHasFixedSize(true);
        //3.设置Adapter
        dispatchListAdapter = new DispatchListAdapter(getContext(), trayModel.dispatchlist, (view, position) -> openItem(position));
        binding.dispatchRv.setAdapter(dispatchListAdapter);
        dispatchListAdapter.notifyDataSetChanged();
    }
    DispatchModel selectedDispatchModel;
    private void openItem(int id){
        ArrayList<DispatchModel> dispatchlist = TrayManager.getInstance().getPipeTrayModel(trayindex).dispatchlist;
        for(int i=0;i< dispatchlist.size();i++){
            if(dispatchlist.get(i).index.get()==id){
                dispatchlist.get(i).selected.set(true);
                selectedDispatchModel = dispatchlist.get(i);
            }else{
                dispatchlist.get(i).selected.set(false);
            }
        }
    }

}