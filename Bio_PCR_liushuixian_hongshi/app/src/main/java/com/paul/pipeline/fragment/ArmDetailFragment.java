package com.paul.pipeline.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentArmDetailBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipeDoorModel;
import com.paul.pipeline.fragment.viewmodel.ArmDetailViewModel;
import com.paul.pipeline.listener.ArmFinishCallback;
import com.paul.pipeline.listener.ArmResetCallback;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.fragment.model.PipeArmModel;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;


public class ArmDetailFragment extends Fragment {
    final String TAG = "ArmDetailFragment";
    private ArmDetailViewModel mViewModel;

    public ArmDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }
    FragmentArmDetailBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_arm_detail, container, false);
        initBinding();
        return binding.getRoot();
//        return inflater.inflate(R.layout.fragment_arm_detail, container, false);
    }
    PipeMainViewModel mainViewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainViewModel.getArmClickEvent().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                initModel(integer);
            }
        });
        mViewModel = new ViewModelProvider(this).get(ArmDetailViewModel.class);
        binding.setViewModel(mViewModel);
        binding.executePendingBindings();
        initDoorModel();
    }
    PipeDoorModel doormodel;
    public void initDoorModel(){
        if(mainViewModel!=null){
            doormodel = mainViewModel.getPipeDoorModel().getValue();
            if(doormodel!=null){
                binding.setDoormodel(doormodel);
                binding.executePendingBindings();
            }
        }
    }
    private void initBinding() {
        binding.ivBack.setOnClickListener(v -> close());
        binding.untouchview.setOnClickListener(v -> {
            //不实现，禁止外围区域触摸
        });

        binding.getBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String traystr = binding.targetnumberEt.getText().toString();
                if (traystr != null&&!traystr.isEmpty()&&!traystr.isBlank()) {
                    try{
                        int tray = Integer.parseInt(traystr);
                        if (tray >= 1 && tray <= 8) {
                            String result = ArmManager.getInstance().startGet(ConstantData.DEVICE_TRAY, tray);
                            if (result != null) {
                                show(result);
                            }
                        } else {
                            show("输入范围错误");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        show("输入不合法");
                    }
                } else {
                    show("不能为空值");
                }
            }
        });
        binding.putBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String traystr = binding.targetnumberEt.getText().toString();
                if(traystr!=null&&!traystr.isEmpty()&&!traystr.isBlank()) {
                    try {
                        int tray = Integer.parseInt(traystr);
                        if (tray >= 1 && tray <= 8) {
                            String result = ArmManager.getInstance().startPut(ConstantData.DEVICE_TRAY, tray);
                            if (result != null) {
                                show(result);
                            }
                        } else {
                            show("输入范围错误");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        show("输入不合法");
                    }
                }else{
                    show("不能为空值");
                }
            }
        });
        binding.startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int tray = 0;
                String traystr = binding.targetnumberEt.getText().toString();
                if(traystr!=null&&!traystr.isEmpty()&&!traystr.isBlank()) {
                    try {
                        tray = Integer.parseInt(traystr);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
                if(ArmManager.getInstance().itemProperty_source==null){
                     show("没有来源位置");
                     return;
                }
                if(ArmManager.getInstance().itemProperty_target==null){
                    show("没有目标位置");
                    return;
                }
                int source = ArmManager.getInstance().itemProperty_source.getPosition();
                int sourcetype = ConstantData.ConvertDeviceType(ArmManager.getInstance().itemProperty_source.getType());
                int target = ArmManager.getInstance().itemProperty_target.getPosition();
                int targettype = ConstantData.ConvertDeviceType(ArmManager.getInstance().itemProperty_target.getType());
                String result = ArmManager.getInstance().startArmRobot(source,sourcetype,target,targettype,new ArmFinishCallback() {
                    @Override
                    public void finish(int arm, ItemProperty aim, ItemProperty source) {
                        Log.w(TAG,"finish arm:"+arm
                                +" source:"+source.getPosition()  +" sourcetype:"+source.getType()
                                +" aim:"+aim.getPosition()  +" aimtype:"+source.getType());
                    }

                    @Override
                    public void fail(int arm, ItemProperty aim, ItemProperty source,String error) {
                        Log.e(TAG,"fail arm:"+arm
                                +" source:"+source.getPosition()  +" sourcetype:"+source.getType()
                                +" aim:"+aim.getPosition()  +" aimtype:"+source.getType());
                    }
                },tray);
                if(result!=null){
                    show(result);
                }
            }
        });
        binding.stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArmManager.getInstance().stop();
                show("机械臂停止");
            }
        });
//        binding.configBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                boolean result = ArmManager.getInstance().config();//复位
//                if(result) {
//                    show("机械臂"+getString(R.string.main_config)+" 成功！");
//                }else{
//                    show("机械臂"+getString(R.string.main_config)+" 失败！");
//                }
//            }
//        });
        binding.cleardispatchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = ArmManager.getInstance().clearFinish();//清占用
                if(result) {
                    show("机械臂"+getString(R.string.main_cleardispatch)+" 成功！");
                }else{
                    show("机械臂"+getString(R.string.main_cleardispatch)+" 失败！");
                }
            }
        });
        binding.resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = ArmManager.getInstance().robotReset(new ArmResetCallback() {
                    @Override
                    public void finish(boolean result, String message) {
                        if(result) {
                            show("机械臂" + getString(R.string.main_reset_setting)+" 成功！");
                        }else{
                            show("机械臂" + getString(R.string.main_reset_setting)+" 失败！");
                        }
                    }
                });//异常复位
                if(result) {
                    show("机械臂" + getString(R.string.main_reset_setting)+" 发送成功！");
                }else{
                    show("机械臂" + getString(R.string.main_reset_setting)+" 发送失败！");
                }
            }
        });
        binding.continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String result = ArmManager.getInstance().errorContinue();//异常继续
                if(result!=null) {
                    show("机械臂" + getString(R.string.main_continue_exception)+" 失败："+result);
                }else{
                    show("机械臂" + getString(R.string.main_continue_exception)+" 成功！");
                }
            }
        });
        binding.finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = ArmManager.getInstance().armFinishCMD();//完成
                if(result) {
                    show("机械臂完成"+" 成功！");
                }else{
                    show("机械臂完成"+" 失败！");
                }
            }
        });
    }
    private void close(){
        mainViewModel.onNavClicked(ConstantData.NAV_MAIN);
    }

    private PipeArmModel model;
    private void initModel(int index){
        model = mainViewModel.findPipeArmModel(getContext(),index);
        binding.setModel(model);
        binding.executePendingBindings();
    }

    private void show(String text){
        Toast.makeText(getContext(), ""+text,Toast.LENGTH_LONG).show();
    }

}