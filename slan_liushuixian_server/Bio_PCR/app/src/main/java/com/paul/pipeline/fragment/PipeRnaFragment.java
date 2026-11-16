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
import com.gidi.bio_pcr.databinding.FragmentPipeRnaBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.viewmodel.PipeRnaViewModel;
import com.paul.pipeline.fragment.model.PipeRNAModel;

public class PipeRnaFragment extends Fragment {

    private PipeRnaViewModel mViewModel;


    private static final String ARG_PARAM = "index";
    private int pindex;

    public PipeRnaFragment() {
        // Required empty public constructor
    }

    public static PipeRnaFragment newInstance(int index) {
        PipeRnaFragment fragment = new PipeRnaFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pindex = getArguments().getInt(ARG_PARAM);
        }
    }
    FragmentPipeRnaBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_pipe_rna, container, false);
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pipe_rna, container, false);
        initBinding();
        return binding.getRoot();
    }

    private void initBinding() {
        binding.mainbgLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pindex>=1&&pindex<=2) {
                    mainViewModel.onNavClicked(ConstantData.NAV_DETAILRNA);
                    mainViewModel.setRnaClicked(pindex);
                }
            }
        });
    }
    PipeMainViewModel mainViewModel;
    PipeRNAModel model;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PipeRnaViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        initModel();
    }
    public void initModel(){
        if(mainViewModel!=null) {
            if(pindex>=1&&pindex<=2) {
                model = mainViewModel.findPipeRNAModel(getContext(), pindex);
                binding.setModel(model);
                binding.executePendingBindings();
            }
        }
    }

    public void clearModel(){
        if(mainViewModel!=null) {
            mainViewModel.clearPipeRNAModel(getContext(), pindex);
        }
        initModel();
    }
}