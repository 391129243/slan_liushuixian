package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentPipeTrayBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.adapter.SampleHoleListAdapter;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.viewmodel.PipeTrayViewModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.model.SampleHoleModel;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PipeTrayFragment extends Fragment {
    private static final String ARG_PARAM2 = "index";

    public PipeTrayFragment() {
        // Required empty public constructor
    }

    public static PipeTrayFragment newInstance(int index) {
        PipeTrayFragment fragment = new PipeTrayFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM2, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            trayindex = getArguments().getInt(ARG_PARAM2);
        }
    }
    PipeTrayViewModel mViewModel;
    FragmentPipeTrayBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pipe_tray, container, false);
        initBinding();
        return binding.getRoot();
//        return inflater.inflate(R.layout.fragment_pipe_tray, container, false);
    }

    private void initBinding() {
        binding.mainbgLl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(trayindex==8){
                    mainViewModel.setToastInfo("该位置已调整为盖板位！");
                }else {
                    openTrayDetail(trayindex);
                }
            }
        });
    }
    private void openTrayDetail(int id){
        if(id>=1&&id<=8) {
            mainViewModel.onNavClicked(ConstantData.NAV_TRAYMANAGE);
            mainViewModel.onTrayClicked(id);
        }
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PipeTrayViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainViewModel.getTrayChanged().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                if(integer==trayindex){
                    initModel();
                }
            }
        });
        initModel();
        initUIHandler();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeUIHandler();
    }

    int trayindex;
    PipeMainViewModel mainViewModel;
    PipeTrayModel model;

    public void initModel(){
        if(trayindex>=1&&trayindex<=8) {
            if(mainViewModel!=null) {
                model = mainViewModel.findPipeTrayModel(getContext(), trayindex);
                binding.setModel(model);
                binding.executePendingBindings();
                initSampleHoleListCycleView();
            }
        }
    }
    public void clearModel(){
        if(mainViewModel!=null) {
            mainViewModel.clearPipeTrayModel(getContext(), trayindex);
        }
    }
    SampleHoleListAdapter sampleHoleListAdapter;
    private void initSampleHoleListCycleView(){
        if(model!=null && model.holes!=null && model.holes.size()>0 && getContext()!=null) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            //1.设置LinearLayoutManager ListView
            binding.sampleholeRv.setLayoutManager(layoutManager);
            layoutManager.setOrientation(RecyclerView.HORIZONTAL);
            //2.设置固定大小
            binding.sampleholeRv.setHasFixedSize(true);
            //3.设置Adapter
            sampleHoleListAdapter = new SampleHoleListAdapter(getContext(), model.holes);
            binding.sampleholeRv.setAdapter(sampleHoleListAdapter);
            sampleHoleListAdapter.notifyDataSetChanged();
        }
    }
   private void refreshModel(){
       if(model!=null) {
           binding.executePendingBindings();
       }
        if(sampleHoleListAdapter!=null){
            sampleHoleListAdapter.notifyDataSetChanged();
        }
   }
    Timer timer;
    TimerTask timerTask;
    private void initUIHandler() {
        handler = new UIHandler();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 在这里执行你的后台任务
                try{
                    if(handler!=null) {
                        handler.sendEmptyMessage(ConstantData.MSG_UPDATE_UI);
                    }
                } catch (Exception e) {
                    Log.e("ArmTaskWorker", e.getMessage());
                }
                //  Log.i(TAG, "执行刷新");
            }
        };
        timer.schedule(timerTask, 1000L, 1000L);
    }
    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }
    private void removeUIHandler(){
        stopTimer();
        if (null != handler) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    UIHandler handler;
    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case ConstantData.MSG_UPDATE_UI:
                    initModel();
                    break;
            }
        }
    }
}