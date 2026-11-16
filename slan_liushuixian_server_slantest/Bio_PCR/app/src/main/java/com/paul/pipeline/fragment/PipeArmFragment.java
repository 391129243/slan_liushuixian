package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentPipeArmBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipeDoorModel;
import com.paul.pipeline.fragment.viewmodel.PipeArmViewModel;
import com.paul.pipeline.fragment.model.PipeArmModel;
import com.winters.boweilibrary.autopipeline.mgr.SerialPortManager_Robot;

public class PipeArmFragment extends Fragment {

    private PipeArmViewModel mViewModel;

    private static final String ARG_PARAM2 = "index";
    private int pindex;

    public PipeArmFragment() {
        // Required empty public constructor
    }

    public static PipeArmFragment newInstance(int index) {
        PipeArmFragment fragment = new PipeArmFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM2, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pindex = getArguments().getInt(ARG_PARAM2);
        }
    }

    FragmentPipeArmBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pipe_arm, container, false);
        initBinding();
        return binding.getRoot();
      //  return inflater.inflate(R.layout.fragment_pipe_arm, container, false);
    }

    private void initBinding() {
        binding.mainbgLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pindex>=1) {
                    mainViewModel.onNavClicked(ConstantData.NAV_DETAILARM);
                    mainViewModel.setArmClickEvent(pindex);
                }
            }
        });

    }
    PipeMainViewModel mainViewModel;
    PipeArmModel model;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PipeArmViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        initModel();
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
    public void initModel(){
        if(mainViewModel!=null) {
            if(pindex>=1) {
                model = mainViewModel.findPipeArmModel(getContext(), pindex);
                binding.setModel(model);
                binding.executePendingBindings();
            }
        }
    }

}