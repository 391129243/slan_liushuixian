package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentPipePcrBinding;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.viewmodel.PipePcrViewModel;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.message.ResultMessage;

import java.util.Timer;
import java.util.TimerTask;

public class PipePcrFragment extends Fragment {
    private static final String ARG_PARAM2 = "index";
    private int pcrindex;

    public PipePcrFragment() {
        // Required empty public constructor
    }

    public static PipePcrFragment newInstance(int index) {
        PipePcrFragment fragment = new PipePcrFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PARAM2, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            pcrindex = getArguments().getInt(ARG_PARAM2);
        }
    }
    private PipePcrViewModel mViewModel;
    FragmentPipePcrBinding binding;
    View mview;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
     //   return inflater.inflate(R.layout.fragment_pipe_pcr, container, false);
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pipe_pcr, container, false);
        mview = binding.getRoot();
        initBinding();
        return mview;
    }

    private void initBinding() {
        binding.detailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pcrindex>=1&&pcrindex<=6) {
                    mainViewModel.onNavClicked(ConstantData.NAV_DETAILPCR);
                    mainViewModel.setPcrClicked(pcrindex);
                }
            }
        });
    }
    PipeMainViewModel mainViewModel;
    PipePCRModel model;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(PipePcrViewModel.class);
        mViewModel.getRadioGroupOption().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                switch(integer){
                    case FLAG_STEP:
                        changgeFragment(FLAG_STEP);
                        break;
                    case FLAG_CURVE:
                        changgeFragment(FLAG_CURVE);
                        break;
                }
            }
        });
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
//        mainViewModel.getPcrExperiment().observe(requireActivity(), new Observer<Integer>() {
//            @Override
//            public void onChanged(Integer integer) {
//                if(integer==pcrindex){
//                    curveFragment.saveExperiment();
//                }
//            }
//        });
        setModel();
        initUIHandler();
    }
    public void setIndex(int value){
        pcrindex = value;
        setModel();
    }
    public void setModel(){
        if(mainViewModel!=null) {
            if(pcrindex >=1 && pcrindex <= 6) {
                model = mainViewModel.findPipePCRModel(getContext(), pcrindex);
                if(model!=null) {
                    initModel(model);
                    initFragment(pcrindex);
                }
            }
        }
    }
    public void initModel(PipePCRModel m){
        binding.setModel(m);
        binding.executePendingBindings();
    }
//    public void refreshTray(){
//        if(model!=null&&model.currentTray.get()!=null) {
//            APLManager.getInstance().setTrayExperiment(model.currentTray.get(), model.index.get());
//        }
//    }
    public void clearModel(){
        if(mainViewModel!=null) {
            mainViewModel.clearPipePCRModel(getContext(), pcrindex);
        }
        if (curveFragment != null) {
            curveFragment.clearModel();
        }
        if (dataFragment != null) {
            dataFragment.clearModel();
        }
    }
    public boolean saveTestRunExperiment(){
        //测试模板需要保存
        if(model!=null&&model.testrun.get()!=null&&model.testrun.get()){
            ResultMessage result = curveFragment.saveTestExperiment();
            Log.w("ArmTaskWorker", pcrindex + "  saveTestExperiment:"+result);
            if (result.isResult()) {
                Log.w("ArmTaskWorker", "pcr "+pcrindex + "  saveTestExperiment success");
                if(mainViewModel!=null) {
                    mainViewModel.setToastInfo("PCR"+pcrindex+" 保存测试数据成功！");
                }
            } else {
                Log.e("ArmTaskWorker", "pcr "+pcrindex + "  saveTestExperiment fail");
                if(mainViewModel!=null){
                    mainViewModel.setAlertInfo("PCR"+pcrindex+" 保存测试数据失败！ \n" + result.getMessage());
                }
            }
            model.testrun.set(false);
            return true;
        }else{
            return false;
        }
    }
    public void saveExperiment(){
        if(!saveTestRunExperiment()) {
            if (curveFragment != null) {
                //判断模板是否需要保存
                if (curveFragment.checkSavePcr()) {
                    ResultMessage result = curveFragment.saveExperiment();
                    Log.w("ArmTaskWorker", pcrindex + "  saveExperiment:" + result);
                    if (result.isResult()) {
                        Log.w("ArmTaskWorker", "pcr " + pcrindex + "  saveExperiment success");
                    } else {
                        Log.e("ArmTaskWorker", "pcr " + pcrindex + "  saveExperiment fail");
                    }
                }
            }
        }
    }
    public void refreshCurve(){
        if(currentIndex==FLAG_CURVE) {
            if (curveFragment != null) {
                curveFragment.refreshExperiment();
            }
        }
    }
    public void refreshStep(){
        if(currentIndex==FLAG_STEP) {
            if (dataFragment != null) {
                dataFragment.initModel();
            }
        }
    }
    final int FLAG_CURVE = 1;
    final int FLAG_STEP = 0;
    int currentIndex = 0;
    private void changgeFragment(int value){
        currentIndex = value;
        if(value==FLAG_STEP){
            getChildFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .hide(curveFragment)
                    .show(dataFragment)
                    .commit();
        }else if(value==FLAG_CURVE){
            getChildFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .hide(dataFragment)
                    .show(curveFragment)
                    .commit();
        }
    }
    PcrDataFragment dataFragment;
    PcrCurveFragment curveFragment;
    public void initFragment(int index){
        dataFragment = PcrDataFragment.newInstance(index);
        curveFragment = PcrCurveFragment.newInstance(index);
        getChildFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.pcrv_fragment_container_view,curveFragment)
                .add(R.id.pcrv_fragment_container_view,dataFragment)
                .hide(curveFragment)
                .commit();
        binding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId==R.id.radioButton_step) {
                mViewModel.setRadioGroupOption(FLAG_STEP);
                //    binding.pcrViewpager.setCurrentItem(0);
            }else if(checkedId==R.id.radioButton_curve) {
                mViewModel.setRadioGroupOption(FLAG_CURVE);
                //   binding.pcrViewpager.setCurrentItem(1);
            }
        });
//                titles = new String[]{"程序","曲线"};
//        list = new ArrayList<>();
//        list.add(dataFragment);
//        list.add(curveFragment);
//        PcrViewPagerAdapter adapter = new PcrViewPagerAdapter(mActivity.getSupportFragmentManager(),list,titles);
//        binding.pcrViewpager.setId(1+model.index.get());//同一个RecyclerView的ViewPager的Id不能是一样
//        binding.pcrViewpager.setAdapter(adapter);
//        //设置初始页面
//        binding.pcrViewpager.setCurrentItem(0);
    }
    int countrefresh = 0;
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
                    if(countrefresh%5==0){
                        if(handler!=null) {
                            handler.sendEmptyMessage(FLAG_STEP);
                        }
                    }
                    countrefresh++;
                    if(countrefresh>=10){
                        if(handler!=null) {
                            handler.sendEmptyMessage(FLAG_CURVE);
                        }
                        countrefresh = 0;
                    }
                } catch (Exception e) {
                    Log.e("ArmTaskWorker", e.getMessage());
                }
                //  Log.i(TAG, "执行主页刷新");
            }
        };
        timer.schedule(timerTask, 1000L, 4000L);
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
                case FLAG_CURVE:
                    refreshCurve();
                    break;
                case FLAG_STEP:
                    refreshStep();
                    break;
            }
        }
    }
}