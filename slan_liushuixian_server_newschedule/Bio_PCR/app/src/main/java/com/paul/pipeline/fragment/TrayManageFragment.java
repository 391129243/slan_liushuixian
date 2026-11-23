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

import com.paul.pipeline.utils.DialogUtil;

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
import com.paul.pipeline.model.ArmStep;
import com.paul.pipeline.model.DeviceRuntimeState;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;

import java.util.ArrayList;
import java.util.List;

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
                    // 还盘中禁止清空：清空会清掉 dispatchlist，BACK 分支找不到调度数据会放弃还盘，盘留在设备里。
                    // 非还盘状态允许清空（可手动还盘）。
                    String backing = findBackingDevice(trayModel.index.get());
                    if (backing != null) {
                        Log.w("TrayStop", "托盘:" + trayModel.index.get() + " " + backing + " 还盘中，拒绝清空");
                        showLongToast(backing + "还盘中，请稍后再清空！");
                        return;
                    }
                    // 清空托盘调度信息（设为空闲、清 dispatchlist/批次/项目等），保留样本；
                    // 不触碰设备状态，设备 currentTray/armStep 保留用于还盘。
                    // dispatchlist 清空后 getCurrentDispatch 返回 null，设备不再被下发模板，流程不再触发。
                    TrayManager.getInstance().clearTrayKeepSamples(trayModel.index.get());
                    // 本地引用同步为最新（clearKeepSamples 内部已置空闲），刷新界面状态栏
                    trayModel.flagstatus.set(ConstantData.FLAG_FREE);
                    trayModel.status.set(ConstantData.str_FREE);
                    mainViewModel.setTrayChanged(trayModel.index.get());
                    showLongToast("托盘信息已清空！");
                }
            }
        });
        binding.ivBack.setOnClickListener(v -> close());
    }
    private void stopTrayDispatch(){
        Log.w("TrayStop", "=== stopTrayDispatch() 被调用 ===");

        if(trayModel == null){
            Log.e("TrayStop", "trayModel为null");
            showLongToast("托盘信息为空！");
            return;
        }
        int trayIndex = trayModel.index.get();
        Log.i("TrayStop", "托盘:" + trayIndex + " flagstatus=" + trayModel.flagstatus.get());

        // 停止不可恢复：托盘标为停止，界面显示异常（红色）
        trayModel.flagstatus.set(ConstantData.FLAG_STOP);
        trayModel.status.set(ConstantData.str_ERROR);

        // 找出 currentTray 指向本托盘的设备，按机械臂步骤分别处理。
        // 盘已在设备里或正在送进设备：强制复位后置 BACK，由调度的异常还盘链取回；
        // 取盘尚未下发：盘还在托盘架上，释放设备即可，不能置 BACK 去空设备取盘。
        int handled = 0;
        List<PipeRNAModel> rnalist = RNAManager.getInstance().getRnalist();
        if (rnalist != null) {
            for (PipeRNAModel m : rnalist) {
                if (m != null && m.currentTray.get() != null && m.currentTray.get() == trayIndex) {
                    stopDeviceForTray(ConstantData.DEVICE_RNA, m.index.get(), m.armStep.get(), m.runtime);
                    handled++;
                }
            }
        }
        List<PipePCRModel> pcrlist = PCRManager.getInstance().getPcrlist();
        if (pcrlist != null) {
            for (PipePCRModel m : pcrlist) {
                if (m != null && m.currentTray.get() != null && m.currentTray.get() == trayIndex) {
                    stopDeviceForTray(ConstantData.DEVICE_PCR, m.index.get(), m.armStep.get(), m.runtime);
                    handled++;
                }
            }
        }
        Log.i("TrayStop", "托盘:" + trayIndex + " 处理设备数:" + handled);

        TrayManager.getInstance().saveModel(trayIndex);
        showLongToast("托盘调度已停止，等待还盘！");
        Log.w("TrayStop", "=== stopTrayDispatch() 完成 ===");
    }

    /**
     * 停止本托盘占用的一台设备。
     * 盘在设备上或正在被送进设备时，强制复位并置 WORK_BACK，交给调度的异常还盘链取回
     * （还盘动作串自带开门、关门、取盖、放盖，无需另做复位）；
     * 取盘尚未下发时盘还在托盘架上，释放设备，避免去空设备取盘。
     */
    private void stopDeviceForTray(int deviceType, int index, ArmStep step, DeviceRuntimeState runtime){
        String name = deviceType == ConstantData.DEVICE_RNA ? "RNA" : "PCR";
        if (step == null) {
            Log.e("TrayStop", name + index + " armStep为null，跳过");
            return;
        }
        switch (step) {
            case RUN:
                // 盘在设备里，强制复位后置 BACK，交给调度的异常还盘链取回
                forceResetAndBack(deviceType, index);
                break;
            case MOVE:
                if (runtime != null && runtime.isMovetraystart()) {
                    // 移盘已下发：盘正被机械臂送往目标设备，源设备盘已取走。
                    // 目标设备在 startArmTaskMoveTray 里已被写入 currentTray，会以 GET 被本方法单独处理，
                    // 这里只释放源设备，避免 errorDispatch 记到空设备上导致还盘取空。
                    Log.i("TrayStop", name + index + " 移盘进行中，盘已离开，释放源设备");
                    APLManager.getInstance().markDeviceScheduleDone(deviceType, index);
                } else {
                    // 移盘未下发：盘还在本设备里
                    forceResetAndBack(deviceType, index);
                }
                break;
            case GET:
                if (runtime != null && runtime.isGettraystart()) {
                    // 取盘进行中：盘正被送进设备，置 BACK 后取盘回调不会再切 RUN、也不会启动模板
                    forceResetAndBack(deviceType, index);
                } else {
                    // 取盘未下发：盘还在托盘架上，释放设备
                    Log.i("TrayStop", name + index + " 取盘未开始，释放设备");
                    APLManager.getInstance().markDeviceScheduleDone(deviceType, index);
                }
                break;
            case BACK:
                Log.i("TrayStop", name + index + " 已在还盘，不重复处理");
                break;
            case FREE:
            case RECEIVE:
                Log.i("TrayStop", name + index + " armStep=" + step + "，不处理");
                break;
        }
    }

    /** 强制复位（消息带 force 标志，设备在运行中也会执行）并把设备置为 WORK_BACK，交给调度还盘。 */
    private void forceResetAndBack(int deviceType, int index){
        String name = deviceType == ConstantData.DEVICE_RNA ? "RNA" : "PCR";
        boolean resetOk = deviceType == ConstantData.DEVICE_RNA
                ? RNAManager.getInstance().reset(index)
                : PCRManager.getInstance().reset(index);
        Log.w("TrayStop", name + index + " 强制复位发送" + (resetOk ? "成功" : "失败"));
        if (deviceType == ConstantData.DEVICE_RNA) {
            APLManager.getInstance().NotifyRnaTrayError(index);
            APLManager.getInstance().checkRnaFinishError(index);
        } else {
            APLManager.getInstance().NotifyPcrTrayError(index);
            APLManager.getInstance().checkPcrFinishError(index);
        }
        Log.w("TrayStop", name + index + " 已置 WORK_BACK，等待调度还盘");
    }
    /**
     * 查找正在为本托盘还盘的设备（currentTray==本托盘 且 armStep==BACK）。
     * 还盘完成后 finishBacktray 会把 armStep 置 FREE、currentTray 清 0，届时返回 null。
     * @return 设备名（如 "RNA1"），无则 null
     */
    private String findBackingDevice(int trayIndex){
        List<PipeRNAModel> rnalist = RNAManager.getInstance().getRnalist();
        if (rnalist != null) {
            for (PipeRNAModel m : rnalist) {
                if (m != null && m.currentTray.get() != null && m.currentTray.get() == trayIndex
                        && m.armStep.get() == ArmStep.BACK) {
                    return "RNA" + m.index.get();
                }
            }
        }
        List<PipePCRModel> pcrlist = PCRManager.getInstance().getPcrlist();
        if (pcrlist != null) {
            for (PipePCRModel m : pcrlist) {
                if (m != null && m.currentTray.get() != null && m.currentTray.get() == trayIndex
                        && m.armStep.get() == ArmStep.BACK) {
                    return "PCR" + m.index.get();
                }
            }
        }
        return null;
    }

    private void close(){
        mainViewModel.onNavClicked(ConstantData.NAV_MAIN);
    }

    private void showLongToast(String text){
        DialogUtil.showMessage(getContext(), text);
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