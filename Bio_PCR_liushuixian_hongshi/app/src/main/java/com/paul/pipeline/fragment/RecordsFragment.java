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
import android.widget.LinearLayout;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.activity.ResultActivity;
import com.gidi.bio_pcr.fragment.result.ResultBiowayWellFragment;
import com.gidi.bio_pcr.fragment.result.ResultController;
import com.gidi.bio_pcr.fragment.result.ResultExFragment;
import com.paul.pipeline.fragment.viewmodel.RecordsViewModel;

public class RecordsFragment extends Fragment {

    private RecordsViewModel mViewModel;

    public static RecordsFragment newInstance() {
        return new RecordsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
//        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_records, container, false);
//        initBinding();
//        return binding.getRoot();
        View rootv = inflater.inflate(R.layout.fragment_records, container, false);
        mExperimentTab = (LinearLayout)rootv.findViewById(R.id.result_experiment_tab);
        mWellResultTab = (LinearLayout)rootv.findViewById(R.id.result_wellresult_tab);
        return rootv;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(RecordsViewModel.class);
        initListener();
    }

//    public boolean isSettingIn = false;
//    private ResultController resultController;
    private LinearLayout mExperimentTab;
    private LinearLayout mWellResultTab;
    ResultExFragment resultExFragment;
    ResultBiowayWellFragment biowayWellFragment;
    protected void initListener() {
        initData();
        mExperimentTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTabSelection(0);
                mExperimentTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
                mWellResultTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            }
        });
        mWellResultTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTabSelection(1);
                mWellResultTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
                mExperimentTab.setBackgroundResource(R.drawable.shape_sys_tab_normal);
            }
        });
    }
    protected void initData() {
        resultExFragment = new ResultExFragment();
        biowayWellFragment = new ResultBiowayWellFragment();
        getChildFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.result_frame_layout,biowayWellFragment)
                .add(R.id.result_frame_layout,resultExFragment)
                .commit();
       // isSettingIn = getIntent().getBooleanExtra("SettingIn",false);
//        resultController = ResultController.getInstance(this, R.id.result_frame_layout);
        setTabSelection(0);
        mExperimentTab.setBackgroundResource(R.drawable.shape_sys_tab_pressed);
    }
    /**设置底部tab切换**/
    private void setTabSelection(int position){
      //  resultController.showFragment(position);
        switch (position) {
            case 0:
                getChildFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .hide(biowayWellFragment)
                        .show(resultExFragment)
                        .commit();
                break;
            case 1:
                getChildFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .hide(resultExFragment)
                        .show(biowayWellFragment)
                        .commit();
                break;
        }
    }
}