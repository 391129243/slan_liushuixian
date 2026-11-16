package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.paul.pipeline.utils.DialogUtil;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentPcrDetailBinding;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.viewmodel.PcrDetailViewModel;
import com.paul.pipeline.listener.ArmFinishCallback;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.manager.RNAManager;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;

public class PcrDetailFragment extends Fragment {
    final String TAG = "PcrDetailFragment";
    private PcrDetailViewModel mViewModel;

    public static PcrDetailFragment newInstance() {
        return new PcrDetailFragment();
    }
    FragmentPcrDetailBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
      //  return inflater.inflate(R.layout.fragment_pcr_detail, container, false);
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pcr_detail, container, false);
        initBinding();
        return binding.getRoot();
    }

    PipeMainViewModel mainViewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainViewModel.getPcrClickEvent().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                initModel(integer);
            }
        });
        mViewModel = new ViewModelProvider(this).get(PcrDetailViewModel.class);
        binding.setViewModel(mViewModel);
        binding.executePendingBindings();
    }
    private void initBinding() {
        binding.ivBack.setOnClickListener(v -> close());
        binding.untouchview.setOnClickListener(v -> {
            //不实现，禁止外围区域触摸
        });
        binding.sendoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBackTray();
            }
        });
        binding.startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String result = PCRManager.getInstance().execute(model.index.get());
                if(result==null){
                    show("PCR "+model.index.get() +" 运行 指令发送成功 ");
                }else{
                    show("PCR "+model.index.get() +" 运行 指令发送失败 "+result);
                }
            }
        });
        binding.stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(model.index.get()!=null) {
                    boolean result = PCRManager.getInstance().stop(model.index.get());
                    if (model.usererror.get() != null) {
                        model.usererror.set(1);
                    }
                    APLManager.getInstance().NotifyPcrTrayError(model.index.get());
                    if (result) {
                        show("PCR " + model.index.get() + " 停止 指令发送成功 " + result);
                    } else {
                        show("PCR " + model.index.get() + " 停止 指令发送失败 " + result);
                    }
                }else{
                    show("PCR " + model.index.get() + " 停止 指令发送失败 ");
                }
            }
        });
        binding.resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = PCRManager.getInstance().reset(model.index.get());
                if(result){
                    show("PCR "+model.index.get() +" 重置 指令发送成功 "+result);
                }else{
                    show("PCR "+model.index.get() +" 重置 指令发送失败 "+result);
                }
            }
        });
        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(model!=null&&model.index.get()!=null) {
                    String result = PCRManager.getInstance().errorContinue(model.index.get());//异常继续
                    if (result!=null) {
                        show("PCR" + getString(R.string.main_continue_exception) + " 失败!");
                    } else {
                        show("PCR" + getString(R.string.main_continue_exception) + " 成功！");
                    }
                }else{
                    show("PCR" + getString(R.string.main_continue_exception) + " 失败,数据异常！");
                }
            }
        });
        binding.testrunBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = PCRManager.getInstance().testexecute1(model.index.get());
                if(result){
                    show("PCR " + model.index.get() + " 测试模板熔解 运行指令发送成功 "+result);
                }else{
                    show("PCR " + model.index.get() + " 测试模板熔解 运行指令发送失败 "+result);
                }
            }
        });
        binding.testrunsecondBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = PCRManager.getInstance().testexecute2(model.index.get());
                if(result){
                    show("PCR " + model.index.get() + " 测试模板扩增 运行指令发送成功 "+result);
                }else{
                    show("PCR " + model.index.get() + " 测试模板扩增 运行指令发送失败 "+result);
                }
            }
        });
        binding.cleardispatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (model.usererror.get() != null) {
                    model.usererror.set(0);
                }
                PCRManager.getInstance().clearDispatch(model.index.get());
                show("PCR " + model.index.get() + " 调度信息已重置。");
            }
        });
//        binding.clearpcrBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mainViewModel.setPcrClear(model.index.get());
//                show("PCR " + model.index.get() + " 界面已初始化。");
//            }
//        });
        binding.alarmControlLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(model.alarmenable.get()){
                    model.alarmenable.set(false);
                    show("PCR " + model.index.get() + " 报警弹窗已禁用");
                }else{
                    model.alarmenable.set(true);
                    show("PCR " + model.index.get() + " 报警弹窗已开启");
                }
            }
        });
    }
    private boolean sendBackTray(){
        if(model.index.get()!=null) {
            int pcrindex = model.index.get();
//        if(model.currentTray.get()==null||model.dispatch.get()==null||model.dispatch.get().index.get()==null){
//            show("PCR " + rnaindex + " 调度信息不存在，无法放回！");
////            sendBack();
//            return false;
//        }else {
//            int trayindex = model.currentTray.get();
//            int dispatchindex = model.dispatch.get().index.get();
//            APLManager.getInstance().createArmBackTray(rnaindex, ConstantData.DEVICE_PCR, trayindex, dispatchindex);
            // 放回时自动替用户复位一次：异常设备需先 reset 清错、状态回 FREE，调度的 WORK_BACK 才会取盘。
            // 只试一次；reset 成功才清 usererror；失败则退回原行为（仍置 BACK，等用户稍后手动复位）。
            boolean resetOk = PCRManager.getInstance().reset(pcrindex);
            if (resetOk && model.usererror.get() != null) {
                model.usererror.set(0);
            }
            // 放回=放弃当前调度：先把对应托盘标为异常（str_ERROR + 记 errorDispatchindex），
            // 使 checkTrayEnd 跳过该托盘、不再推进 dispatchindex；改由 checkTrayError 取回并保持异常。
            APLManager.getInstance().NotifyPcrTrayError(pcrindex);
            APLManager.getInstance().checkPcrFinishError(pcrindex);
            if (resetOk) {
                show("PCR " + pcrindex + " 已复位，等待调度放回！");
            } else {
                show("PCR " + pcrindex + " 复位发送失败，仍尝试放回，请确认设备状态！");
            }
            return true;
//        }
        }else{
            show("放回失败，数据错误！");
            return false;
        }
    }
//    private void sendBack(){
//        String traystr = binding.traynumberEt.getText().toString();
//        if (traystr != null&&!traystr.isEmpty()&&!traystr.isBlank()) {
//            try {
//                int tray = Integer.parseInt(traystr);
////                    int tray = model.currentTray.get();
//                if (tray >= 1 && tray <= 8) {
//                    ArmManager.getInstance().setPCRTray(model.index.get(), tray);
//                    String result = ArmManager.getInstance().start(new ArmFinishCallback() {
//                        @Override
//                        public void finish(int arm, ItemProperty aim, ItemProperty source) {
//                            Log.w("ArmTaskWorker", "pcr 强制放回 arm finish source:" + source.getPosition() + " sourcetype:" + source.getType()
//                                    + " aim:" + aim.getPosition() + " aimtype:" + source.getType());
//                        }
//
//                        @Override
//                        public void fail(int arm, ItemProperty aim, ItemProperty source,String error) {
//                            Log.e("ArmTaskWorker", "pcr 强制放回 arm fail source:" + source.getPosition() + " sourcetype:" + source.getType()
//                                    + " aim:" + aim.getPosition() + " aimtype:" + source.getType());
//                        }
//                    });
//                    if (result != null) {
//                        show(result);
//                    }
//                } else {
//                    show("输入范围错误");
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//                show("输入值解析错误");
//            }
//        }else{
//            show("托盘序号 不能为空值");
//        }
//    }
    private void show(String text){
        DialogUtil.showMessage(getContext(), text);
    }
    private void close(){
        mainViewModel.onNavClicked(ConstantData.NAV_MAIN);
    }
    private PipePCRModel model;
    private void initModel(int index){
        model = mainViewModel.findPipePCRModel(getContext(),index);
        binding.setModel(model);
        binding.executePendingBindings();
    }
}