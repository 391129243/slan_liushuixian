package com.paul.pipeline.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentPipeMainBinding;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.manager.PCRManager;
import com.paul.pipeline.manager.RNAManager;
import com.paul.pipeline.manager.TrayManager;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PipeMainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PipeMainFragment extends Fragment {

    final String TAG = "PipeMainFragment";

    public static PipeMainFragment newInstance() {
        return new PipeMainFragment();
    }

    FragmentPipeMainBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pipe_main, container, false);
        initBinding();
        return binding.getRoot();
    }

    private void initBinding() {

    }

    PipeMainViewModel mainviewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainviewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainviewModel.getTrayList().observe(requireActivity(), new Observer<ArrayList<PipeTrayModel>>() {
            @Override
            public void onChanged(ArrayList<PipeTrayModel> pipeTrayModels) {
                if(trayf1!=null) {
                    trayf1.initModel();
                }
                if(trayf2!=null) {
                    trayf2.initModel();
                }
                    if(trayf3!=null) {
                        trayf3.initModel();
                    }
                        if(trayf4!=null) {
                            trayf4.initModel();
                        }
                            if(trayf5!=null) {
                                trayf5.initModel();
                            }
                                if(trayf6!=null) {
                                    trayf6.initModel();
                                }
                                    if(trayf7!=null) {
                                        trayf7.initModel();
                                    }
                                        if(trayf8!=null) {
                                            trayf8.initModel();
                                        }
            }
        });
        mainviewModel.getTrayClear().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                trayClear(integer);
            }
        });
        mainviewModel.getPcrClear().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                pcrClear(integer);
            }
        });
        mainviewModel.getRnaClear().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                rnaClear(integer);
            }
        });
        initViewModel();
        initarmfragment();
        initrnafragment();
        inittrayfragment();
        initpcrfragment();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    private void initViewModel(){
//        mainviewModel.getTrayList().observe(requireActivity(), new Observer<ArrayList<PipeTrayModel>>() {
//            @Override
//            public void onChanged(ArrayList<PipeTrayModel> pipeTrayModels) {
//
//            }
//        });

    }

//    int hole = 0;
//    int status = 0;
//    private void changeTrayStatus(int id){
//        for(int i = 0;i < mainviewModel.getTrayList().getValue().size();i++){
//            PipeTrayModel model = mainviewModel.getTrayList().getValue().get(i);
//            if(model.index.get()==id){
//                //  model.setStatus(status);
//                model.holes.get(hole++).status.set(1);
////                trayListAdapter.notifyDataSetChanged();
//            }
//        }
//    }
//    TrayListAdapter trayListAdapter;
//    private void initTrayListCycleView(){
//        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),2);
//        //1.设置LinearLayoutManager ListView
//        binding.trayRv.setLayoutManager(layoutManager);
//        binding.trayRv.setHasFixedSize(true);
//        //3.设置Adapter
//        trayListAdapter = new TrayListAdapter(getContext(), mainviewModel.getTrayList().getValue(), (view, position) -> openTrayDetail(position));
//        binding.trayRv.setAdapter(trayListAdapter);
//        trayListAdapter.notifyDataSetChanged();
//    }
    PipeTrayFragment trayf1 ;
    PipeTrayFragment trayf2 ;
    PipeTrayFragment trayf3 ;
    PipeTrayFragment trayf4 ;
    PipeTrayFragment trayf5;
    PipeTrayFragment trayf6;
    PipeTrayFragment trayf7;
    PipeTrayFragment trayf8;
    private void inittrayfragment(){
        trayf1 = PipeTrayFragment.newInstance(1);
        trayf2 = PipeTrayFragment.newInstance(2);
        trayf3 = PipeTrayFragment.newInstance(3);
        trayf4 = PipeTrayFragment.newInstance(4);
        trayf5 = PipeTrayFragment.newInstance(5);
        trayf6 = PipeTrayFragment.newInstance(6);
        trayf7 = PipeTrayFragment.newInstance(7);
        trayf8 = PipeTrayFragment.newInstance(8);
        getChildFragmentManager().beginTransaction()
                .add(binding.trayfragment1.getId(),trayf1)
                .add(binding.trayfragment2.getId(),trayf2)
                .add(binding.trayfragment3.getId(),trayf3)
                .add(binding.trayfragment4.getId(),trayf4)
                .add(binding.trayfragment5.getId(),trayf5)
                .add(binding.trayfragment6.getId(),trayf6)
                .add(binding.trayfragment7.getId(),trayf7)
                .add(binding.trayfragment8.getId(),trayf8)
                .commit();
    }
    PipeArmFragment armf1;
    private void initarmfragment(){
        armf1 = PipeArmFragment.newInstance(1);
        getChildFragmentManager().beginTransaction()
                .add(binding.armfragment1.getId(),armf1)
                .commit();
    }
    PipeRnaFragment rnaf1;
    PipeRnaFragment rnaf2;
    private void initrnafragment(){
        rnaf1 = PipeRnaFragment.newInstance(1);
        rnaf2 = PipeRnaFragment.newInstance(2);
        getChildFragmentManager().beginTransaction()
                .add(binding.rnafragment1.getId(),rnaf1)
                .add(binding.rnafragment2.getId(),rnaf2)
                .commit();
    }
    PipePcrFragment pcrf1 ;
    PipePcrFragment pcrf2 ;
    PipePcrFragment pcrf3 ;
    PipePcrFragment pcrf4 ;
    PipePcrFragment pcrf5;
    PipePcrFragment pcrf6;
    private void initpcrfragment(){
        pcrf1 = PipePcrFragment.newInstance(1);
        pcrf2 = PipePcrFragment.newInstance(2);
        pcrf3 = PipePcrFragment.newInstance(3);
        pcrf4 = PipePcrFragment.newInstance(4);
        pcrf5 = PipePcrFragment.newInstance(5);
        pcrf6 = PipePcrFragment.newInstance(6);
        getChildFragmentManager().beginTransaction()
                .add(binding.pcrfragment1.getId(),pcrf1)
                .add(binding.pcrfragment2.getId(),pcrf2)
                .add(binding.pcrfragment3.getId(),pcrf3)
                .add(binding.pcrfragment4.getId(),pcrf4)
                .add(binding.pcrfragment5.getId(),pcrf5)
                .add(binding.pcrfragment6.getId(),pcrf6)
                .commit();
    }
//    ARMListAdapter armListAdapter;
//    private void initARMListCycleView(){
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
//        //1.设置LinearLayoutManager ListView
//        binding.armRv.setLayoutManager(layoutManager);
//        binding.armRv.setHasFixedSize(true);
//        //3.设置Adapter
//        armListAdapter = new ARMListAdapter(getContext(), mainviewModel.getARMList().getValue(), (view, position) -> openARMDetail(position));
//        binding.armRv.setAdapter(armListAdapter);
//        armListAdapter.notifyDataSetChanged();
//    }
//    private void openARMDetail(int id) {
//        if(id==1) {
//            mainviewModel.onNavClicked(ConstantData.NAV_DETAILARM);
//            mainviewModel.setArmClickEvent(id);
//        }
//    }

//    RNAListAdapter rnaListAdapter;
//    private void initRNAListCycleView(){
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
//        //1.设置LinearLayoutManager ListView
//        binding.rnaRv.setLayoutManager(layoutManager);
//        binding.rnaRv.setHasFixedSize(true);
//        //3.设置Adapter
//        rnaListAdapter = new RNAListAdapter(getContext(), mainviewModel.getRNAList().getValue(), (view, position) -> openRNADetail(position));
//        binding.rnaRv.setAdapter(rnaListAdapter);
//        rnaListAdapter.notifyDataSetChanged();
//    }
//    private void openRNADetail(int id){
//        if(id>=1&&id<=2) {
//            mainviewModel.onNavClicked(ConstantData.NAV_DETAILRNA);
//            mainviewModel.setRnaClicked(id);
//        }
//    }
//    PCRListAdapter pcrListAdapter;
//    private void initPCRListCycleView(){
//        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),2);
//        //1.设置LinearLayoutManager ListView
//        binding.pcrRv.setLayoutManager(layoutManager);
//        binding.pcrRv.setHasFixedSize(true);
//        //3.设置Adapter
//        pcrListAdapter = new PCRListAdapter(getActivity(), mainviewModel.getPCRList().getValue(), (view, position) -> openPCRDetail(position),
//                getChildFragmentManager(),this);
//        binding.pcrRv.setAdapter(pcrListAdapter);
//        pcrListAdapter.notifyDataSetChanged();
//    }

//    private void openPCRDetail(int id){
//        if(id>=1&&id<=6) {
//            mainviewModel.onNavClicked(ConstantData.NAV_DETAILPCR);
//            mainviewModel.setPcrClicked(id);
//        }
//    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        initUIHandler();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        removeUIHandler();
    }

    public void rnaClear(int index) {
        Log.e("ArmTaskWorker", "rnaClear:" + index);
        switch (index) {
            case 1:
                rnaf1.clearModel();
                break;
            case 2:
                rnaf2.clearModel();
                break;
        }
    }

    public void pcrFinish(int index){
        Log.e("ArmTaskWorker", "pcrFinish:"+index);
        switch(index){
            case 1:
                pcrf1.saveExperiment();
                break;
            case 2:
                pcrf2.saveExperiment();
                break;
            case 3:
                pcrf3.saveExperiment();
                break;
            case 4:
                pcrf4.saveExperiment();
                break;
            case 5:
                pcrf5.saveExperiment();
                break;
            case 6:
                pcrf6.saveExperiment();
                break;
        }
    }
    public void pcrClear(int index){
        Log.e("ArmTaskWorker", "pcrClear:"+index);
        switch(index){
            case 1:
                pcrf1.clearModel();
                break;
            case 2:
                pcrf2.clearModel();
                break;
            case 3:
                pcrf3.clearModel();
                break;
            case 4:
                pcrf4.clearModel();
                break;
            case 5:
                pcrf5.clearModel();
                break;
            case 6:
                pcrf6.clearModel();
                break;
        }
    }
    public void trayClear(int index){
        Log.e("ArmTaskWorker", "trayClear:"+index);
        switch(index){
            case 1:
                trayf1.clearModel();
                break;
            case 2:
                trayf2.clearModel();
                break;
            case 3:
                trayf3.clearModel();
                break;
            case 4:
                pcrf4.clearModel();
                break;
            case 5:
                trayf5.clearModel();
                break;
            case 6:
                trayf6.clearModel();
                break;
            case 7:
                trayf7.clearModel();
                break;
            case 8:
                trayf8.clearModel();
                break;
        }
    }
//    int countrefresh = 0;
    Timer timer;
    TimerTask timerTask;
    private void initUIHandler() {
        handler = new UIHandler();
        initTimer();
    }
    private void initTimer(){
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                // 在这里执行你的后台任务
                try{
                    if(handler!=null) {
                        handler.sendEmptyMessage(ConstantData.MSG_UPDATE_UI);
//                        countrefresh++;
//                        if(countrefresh>10){
//                            handler.sendEmptyMessage(ConstantData.MSG_UPDATE_PCRUI);
//                            countrefresh = 0;
//                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
                //  Log.i(TAG, "执行主页刷新");
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
                    refreshARM();
                    refreshRNA();
                    refreshPCR();
                    break;
            }
        }
    }
    public void refreshTray() {
        mainviewModel.setTrayList(TrayManager.getInstance().getTraylist());
//        trayListAdapter.notifyDataSetChanged();
    }
    public void refreshPCR() {
        mainviewModel.setPCRList(PCRManager.getInstance().getPcrlist());
//        pcrListAdapter.notifyDataSetChanged();
    }
    public void refreshRNA() {
        mainviewModel.setRNAList(RNAManager.getInstance().getRnalist());
      //  rnaListAdapter.notifyDataSetChanged();
    }
    public void refreshARM(){
        mainviewModel.setARMList(ArmManager.getInstance().getArmlist());
      //  armListAdapter.notifyDataSetChanged();
    }
    public void refreshAll(){
        try{
            refreshTray();
            refreshARM();
            refreshRNA();
            refreshPCR();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}