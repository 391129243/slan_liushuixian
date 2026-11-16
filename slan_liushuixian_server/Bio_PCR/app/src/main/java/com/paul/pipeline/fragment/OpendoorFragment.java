package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentOpendoorBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipeDoorModel;
import com.paul.pipeline.fragment.viewmodel.OpendoorViewModel;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.message.ResultMessage;

public class OpendoorFragment extends Fragment {

    private OpendoorViewModel mViewModel;

    public static OpendoorFragment newInstance() {
        return new OpendoorFragment();
    }
    FragmentOpendoorBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_opendoor, container, false);
        initBinding();
        return binding.getRoot();
       // return inflater.inflate(R.layout.fragment_opendoor, container, false);
    }

    private void initBinding() {
        binding.untouchview.setOnClickListener(v -> {
            //不实现，禁止外围区域触摸
        });
        binding.opendoorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ResultMessage result = APLManager.getInstance().openDoor(getContext());
                showAlertText(result.isResult(),result.getMessage());
            }
        });
        binding.closedoorBtn.setOnClickListener(v -> {
            ResultMessage result = APLManager.getInstance().closeDoor(getContext());
            showAlertText(result.isResult(),result.getMessage());
        });
        binding.ivBack.setOnClickListener(v -> close());
    }
    private void showAlertText(boolean result,String text) {
        if(result) {
            binding.alertTv.setTextColor(getContext().getColor(R.color.chart_green));
            binding.alertTv.setText(text);
        }else{
            binding.alertTv.setTextColor(getContext().getColor(R.color.red));
            binding.alertTv.setText(text);
        }
    }
    private void close(){
        mainViewModel.onNavClicked(ConstantData.NAV_MAIN);
    }
    PipeMainViewModel mainViewModel;
    PipeDoorModel model;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(OpendoorViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        initModel();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
    public void initModel(){
        if(mainViewModel!=null){
            model = mainViewModel.getPipeDoorModel().getValue();
            if(model!=null){
                binding.setModel(model);
                binding.executePendingBindings();
               // refreshTips();
            }
        }
    }
    private void refreshTips(){
        if(!model.armhold.get()) {
            binding.alertTv.setText(getString(R.string.toast_open_door_arm_busy));
        }else{
            binding.alertTv.setText(getString(R.string.text_open_door));
        }
    }

}