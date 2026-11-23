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
import com.paul.pipeline.manager.ArmManager;
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
        // continueBtn 已删除，不再支持暂停后继续

        // pauseBtn 已删除，停止按钮作为唯一的停止操作
        binding.stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTrayExecution();
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
    /**
     * 统一的停止执行方法
     *
     * 新策略：停止后让机械臂继续完成当前队列中的所有worker，完成后自动触发还盘
     * 这样可以避免所有中途中断的复杂场景（RNA门打开、PCR盖子卡住等）
     */
    private void stopTrayExecution(){
        Log.w("TrayManageFragment", "【用户点停止】=== stopTrayExecution() 被调用 ===");

        if(trayModel == null) {
            Log.e("TrayManageFragment", "【用户点停止】【错误】trayModel为null");
            showLongToast("托盘信息为空！");
            return;
        }

        int trayIndex = trayModel.index.get();
        Log.i("TrayManageFragment", "【用户点停止】托盘索引：" + trayIndex);
        Log.i("TrayManageFragment", "【用户点停止】当前状态：flagstatus=" + trayModel.flagstatus.get());

        // 1. 设置状态为STOP，停止后不可恢复
        Log.i("TrayManageFragment", "【用户点停止】【步骤1】设置状态为FLAG_STOP");
        trayModel.flagstatus.set(ConstantData.FLAG_STOP);
        Log.d("TrayManageFragment", "【用户点停止】【步骤1】已设置flagstatus=" + ConstantData.FLAG_STOP);

        // 2. 标记为用户中断 - 阻止新任务分配，但让当前队列继续完成
        Log.i("TrayManageFragment", "【用户点停止】【步骤2】检查当前调度任务");
        DispatchModel dm = TrayManager.getInstance().getCurrentDispatch(trayIndex);
        if (dm != null && dm.targettype.get() != null && dm.target.get() != null) {
            Log.i("TrayManageFragment", "【用户点停止】【步骤2】检测到调度任务");
            Log.d("TrayManageFragment", "【用户点停止】【步骤2】  - targettype：" + dm.targettype.get());
            Log.d("TrayManageFragment", "【用户点停止】【步骤2】  - target：" + dm.target.get());

            if (dm.targettype.get() == ConstantData.DEVICE_PCR) {
                Log.i("TrayManageFragment", "【用户点停止】【步骤2】检测到PCR设备，索引：" + dm.target.get());
                PipePCRModel model = PCRManager.getInstance().getPipePCRModel(dm.target.get());
                if (model != null) {
                    Log.i("TrayManageFragment", "【用户点停止】【步骤2】标记PCR为用户中断");
                    Log.d("TrayManageFragment", "【用户点停止】【步骤2】  - 修改前：usererror=" + model.usererror.get());
                    model.usererror.set(1);  // 标记为用户中断，不立即停止
                    Log.d("TrayManageFragment", "【用户点停止】【步骤2】  - 修改后：usererror=1");
                    Log.w("TrayManageFragment", "【用户点停止】【步骤2】标记PCR为用户中断，让当前队列继续");
                }else{
                    Log.w("TrayManageFragment", "【用户点停止】【步骤2】【警告】PCRModel为null");
                }
            } else if (dm.targettype.get() == ConstantData.DEVICE_RNA) {
                Log.i("TrayManageFragment", "【用户点停止】【步骤2】检测到RNA设备，索引：" + dm.target.get());
                PipeRNAModel model = RNAManager.getInstance().getPipeRNAModel(dm.target.get());
                if (model != null) {
                    Log.i("TrayManageFragment", "【用户点停止】【步骤2】标记RNA为用户中断");
                    Log.d("TrayManageFragment", "【用户点停止】【步骤2】  - 修改前：usererror=" + model.usererror.get());
                    model.usererror.set(1);  // 标记为用户中断，不立即停止
                    Log.d("TrayManageFragment", "【用户点停止】【步骤2】  - 修改后：usererror=1");
                    Log.w("TrayManageFragment", "【用户点停止】【步骤2】标记RNA为用户中断，让当前队列继续");
                }else{
                    Log.w("TrayManageFragment", "【用户点停止】【步骤2】【警告】RNAModel为null");
                }
            } else {
                Log.w("TrayManageFragment", "【用户点停止】【步骤2】【警告】未知的targettype：" + dm.targettype.get());
            }
        }else{
            Log.i("TrayManageFragment", "【用户点停止】【步骤2】未检测到调度任务（dm=null 或 targettype=null 或 target=null）");
        }

        // 3. 【关键】注册"停止后还盘" - 让ArmManager在队列完成后自动还盘
        //    这里不要立即执行还盘逻辑，而是让RobotWorkManager继续完成队列
        Log.i("TrayManageFragment", "【用户点停止】【步骤3】【关键】注册停止后还盘");
        Log.w("TrayManageFragment", "【用户点停止】【步骤3】调用 ArmManager.setStopAndReturnTray(" + trayIndex + ")");
        ArmManager.getInstance().setStopAndReturnTray(trayIndex);
        Log.i("TrayManageFragment", "【用户点停止】【步骤3】setStopAndReturnTray() 已调用完成");

        // 4. 保存托盘状态
        Log.i("TrayManageFragment", "【用户点停止】【步骤4】保存托盘状态");
        TrayManager.getInstance().saveModel(trayIndex);
        Log.d("TrayManageFragment", "【用户点停止】【步骤4】saveModel(" + trayIndex + ") 已调用");

        Log.w("TrayManageFragment", "【用户点停止】【步骤5】显示提示信息");
        showLongToast("托盘已停止，等待机械臂完成当前动作...");

        Log.w("TrayManageFragment", "【用户点停止】=== stopTrayExecution() 完成 ===");
        Log.w("TrayManageFragment", "【用户点停止】现在等待RobotWorkManager继续完成队列...");
    }

    /**
     * 【废弃】原来的triggerArmReturnTray方法不再使用
     * 现在还盘逻辑已移到ArmManager.triggerReturnTrayAfterStop()
     */
    @Deprecated
    private void triggerArmReturnTray(int trayIndex){
        // 此方法已废弃，还盘逻辑已移到ArmManager中
        Log.w("TrayManageFragment", "【已废弃】triggerArmReturnTray()不再使用，请使用ArmManager.setStopAndReturnTray()");
    }

    private void stopRnawork(int index){
        Log.i("TrayManageFragment", "【停止RNA】stopRnawork(" + index + ") 被调用");
        boolean result = RNAManager.getInstance().stopRnaWork(index);
        Log.i("TrayManageFragment", "【停止RNA】RNAManager.stopRnaWork(" + index + ") 返回：" + result);

        PipeRNAModel model= RNAManager.getInstance().getPipeRNAModel(index);
        if (model!=null && model.usererror.get() != null) {
            Log.d("TrayManageFragment", "【停止RNA】设置RNA" + index + "的usererror=1");
            model.usererror.set(1);
        }else{
            Log.w("TrayManageFragment", "【停止RNA】【警告】RNAModel为null或usererror为null");
        }

        Log.i("TrayManageFragment", "【停止RNA】调用 APLManager.NotifyRnaTrayError(" + index + ")");
        APLManager.getInstance().NotifyRnaTrayError(index);

        if(result) {
            Log.i("TrayManageFragment", "【停止RNA】停止成功");
            showLongToast("RNA" + index + "停止成功，" + getString(R.string.do_wait));
        }else{
            Log.e("TrayManageFragment", "【停止RNA】停止失败");
            showLongToast("RNA" + index + "停止失败！");
        }
    }

    private void stopPcrwork(int index){
        Log.i("TrayManageFragment", "【停止PCR】stopPcrwork(" + index + ") 被调用");
        boolean result = PCRManager.getInstance().stop(index);
        Log.i("TrayManageFragment", "【停止PCR】PCRManager.stop(" + index + ") 返回：" + result);

        PipePCRModel model= PCRManager.getInstance().getPipePCRModel(index);
        if (model!=null && model.usererror.get() != null) {
            Log.d("TrayManageFragment", "【停止PCR】设置PCR" + index + "的usererror=1");
            model.usererror.set(1);
        }else{
            Log.w("TrayManageFragment", "【停止PCR】【警告】PCRModel为null或usererror为null");
        }

        Log.i("TrayManageFragment", "【停止PCR】调用 APLManager.NotifyPcrTrayError(" + index + ")");
        APLManager.getInstance().NotifyPcrTrayError(index);

        if(result) {
            Log.i("TrayManageFragment", "【停止PCR】停止成功");
            showLongToast("PCR" + index + "停止成功，" + getString(R.string.do_wait));
        }else{
            Log.e("TrayManageFragment", "【停止PCR】停止失败");
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