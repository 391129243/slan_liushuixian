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
import com.gidi.bio_pcr.databinding.FragmentRnaDetailBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.viewmodel.RnaDetailViewModel;
import com.paul.pipeline.listener.ArmFinishCallback;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.RNAManager;
import com.paul.pipeline.fragment.model.PipeRNAModel;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.model.DispatchModel;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;

public class RnaDetailFragment extends Fragment {
    final String TAG = "RnaDetailFragment";
    private RnaDetailViewModel mViewModel;

    public static RnaDetailFragment newInstance() {
        return new RnaDetailFragment();
    }
    FragmentRnaDetailBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_rna_detail, container, false);
        initBinding();
        return binding.getRoot();
    }

    PipeMainViewModel mainViewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainViewModel.getRnaClickEvent().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                initModel(integer);
            }
        });
        mViewModel = new ViewModelProvider(this).get(RnaDetailViewModel.class);
        binding.setViewModel(mViewModel);
        binding.executePendingBindings();
    }
    private void initBinding() {
        binding.ivBack.setOnClickListener(v -> close());
        binding.untouchview.setOnClickListener(v -> {
            //不实现，禁止外围区域触摸
        });
        binding.stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = RNAManager.getInstance().stopRnaWork(model.index.get());
                if (model.usererror.get() != null) {
                    model.usererror.set(1);
                }
                APLManager.getInstance().NotifyRnaTrayError(model.index.get());
                if(result) {
                    show("RNA" + model.index.get() + "停止成功，" + getString(R.string.do_wait));
                }else{
                    show("RNA" + model.index.get() + "停止失败！");
                }
            }
        });
        binding.resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show("RNA复位"+getString(R.string.do_wait));
                RNAManager.getInstance().ExecuteResetForce(model.key.get());

            }
        });
//        binding.resetexceptionBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                show("RNA异常复位！"+getString(R.string.do_wait));
//                RNAManager.getInstance().ExecuteResetExpetion(model.key.get());
//            }
//        });
        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(model!=null&&model.index.get()!=null) {
                    String result = RNAManager.getInstance().errorContinue(model.index.get());//异常继续
                    if (result != null) {
                        show("RNA" + getString(R.string.main_continue_exception) + " 失败：" + result);
                    } else {
                        show("RNA" + getString(R.string.main_continue_exception) + " 成功！");
                    }
                }else{
                    show("RNA" + getString(R.string.main_continue_exception) + " 失败,数据异常！");
                }
            }
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
                try {
                    if(model.currentTray.get()>0) {
                        int rna = model.index.get();
                        int tray = model.currentTray.get();
                        DispatchModel dispatch = TrayManager.getInstance().getCurrentDispatch(tray);
                        if(dispatch!=null && dispatch.index.get()!=null && dispatch.index.get() > 0) {
                            int dispatchindex = dispatch.index.get();
                            APLManager.getInstance().startRunTemplate(rna, ConstantData.DEVICE_RNA, tray, dispatchindex);
                            show("RNA开始");
                        }else{
                            show("RNA"+model.index.get()+"找不到 托盘"+ tray+" 的调度数据,不能开始");
                        }
                    }else{
                        show("RNA"+model.index.get()+"没有调度数据,不能开始");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    show("RNA开始错误");
                }
            }
        });
        binding.testrunBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = RNAManager.getInstance().testexecute(model.index.get());
                if(result){
                    show("RNA 测试运行 指令发送成功 "+result);
                }else{
                    show("RNA 测试运行 指令发送失败 "+result);
                }
            }
        });
        binding.clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (model.usererror.get() != null) {
                    model.usererror.set(0);
                }
                RNAManager.getInstance().clearDispatch(model.index.get());
                show("核提 " + model.index.get() + " 调度信息已重置。");
            }
        });
        binding.alarmControlLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(model.alarmenable.get()){
                    model.alarmenable.set(false);
                    show("核提 " + model.index.get() + " 报警弹窗已禁用");
                }else{
                    model.alarmenable.set(true);
                    show("核提 " + model.index.get() + " 报警弹窗已开启");
                }
            }
        });
    }
    private boolean sendBackTray(){
        int rnaindex = model.index.get();
        // 放回时自动替用户复位一次：异常设备需先 reset 清错、状态回 FREE，调度的 WORK_BACK 才会取盘。
        // 只试一次；reset 成功才清 usererror；失败则退回原行为（仍置 BACK，等用户稍后手动复位）。
        boolean resetOk = RNAManager.getInstance().reset(rnaindex);
        if (resetOk && model.usererror.get() != null) {
            model.usererror.set(0);
        }
        // 放回=放弃当前调度：先把对应托盘标为异常（str_ERROR + 记 errorDispatchindex），
        // 使 checkTrayEnd 跳过该托盘、不再推进 dispatchindex；改由 checkTrayError 取回并保持异常。
        APLManager.getInstance().NotifyRnaTrayError(rnaindex);
        APLManager.getInstance().checkRnaFinishError(rnaindex);
        if (resetOk) {
            show("核提 " + model.index.get() + " 已复位，等待调度放回！");
        } else {
            show("核提 " + model.index.get() + " 复位发送失败，仍尝试放回，请确认设备状态！");
        }
        return true;
    }
//    private void sendBack(){
//        String traystr = binding.traynumberEt.getText().toString();
//        if (traystr != null&&!traystr.isEmpty()&&!traystr.isBlank()) {
//            try {
//                int tray = Integer.parseInt(traystr);
//                if (tray >= 1 && tray <= 8) {
//                    ArmManager.getInstance().setRNATray(model.index.get(), tray);
//                    String result = ArmManager.getInstance().start(new ArmFinishCallback() {
//                        @Override
//                        public void finish(int arm, ItemProperty aim, ItemProperty source) {
//                            Log.w(TAG, "rna 强制放回 arm finish source:" + source.getPosition() + " sourcetype:" + source.getType()
//                                    + " aim:" + aim.getPosition() + " aimtype:" + source.getType());
//                        }
//
//                        @Override
//                        public void fail(int arm, ItemProperty aim, ItemProperty source,String error) {
//                            Log.e(TAG, "rna 强制放回 arm fail source:" + source.getPosition() + " sourcetype:" + source.getType()
//                                    + " aim:" + aim.getPosition() + " aimtype:" + source.getType());
//                        }
//                    });
//                    if (result == null) {
//                        show("返回托盘指令发送成功 " + result);
//                    } else {
//                        show("返回托盘指令发送失败 " + result);
//                    }
//                } else {
//                    show("输入范围错误");
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                show("输入值解析失敗");
//            }
//        }else{
//            show("不能为空值");
//        }
//    }
    private void close(){
        mainViewModel.onNavClicked(ConstantData.NAV_MAIN);
    }
    private PipeRNAModel model;
    private void initModel(int index){
        model = mainViewModel.findPipeRNAModel(getContext(),index);
        binding.setModel(model);
        binding.executePendingBindings();
//        initStatus();
    }
    private void show(String text){
        DialogUtil.showMessage(getContext(), text);
    }
//    public void initStatus(){
//        if(model!=null) {
//            int status = ConstantData.ConvertStatus(model.status.get());
//            switch (status) {
//                case ConstantData.STATUS_FREE:
//                    model.statusName.set(getContext().getString(R.string.pipe_main_traystatus_unused).toString());
//                    binding.mainbgLl.setBackground(getContext().getDrawable(R.drawable.selector_tray_unused));
//                    break;
//                case ConstantData.STATUS_RUN:
//                    model.statusName.set(getContext().getString(R.string.pipe_main_traystatus_using).toString());
//                    binding.mainbgLl.setBackground(getContext().getDrawable(R.drawable.selector_tray_using));
//                    break;
//                case ConstantData.STATUS_FINISH:
//                    model.statusName.set(getContext().getString(R.string.pipe_main_traystatus_done).toString());
//                    binding.mainbgLl.setBackground(getContext().getDrawable(R.drawable.selector_tray_done));
//                    break;
//                case ConstantData.STATUS_ERROR:
//                    model.statusName.set(getContext().getString(R.string.pipe_main_traystatus_error).toString());
//                    binding.mainbgLl.setBackground(getContext().getDrawable(R.drawable.selector_tray_error));
//                    break;
//            }
//            String connnecttext = "";
//            if(model.connect.get()){
//                connnecttext = getContext().getString(R.string.connected);
//            }else{
//                connnecttext = getContext().getString(R.string.dissconnected);
//            }
//            model.connectText.set(connnecttext);
//        }
//    }
}