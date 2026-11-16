package com.paul.pipeline.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.SLANStepAdapter;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.databinding.FragmentPcrDataBinding;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.DividerDecoration;
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Step;
import com.paul.pipeline.adapter.MiniSLANStepAdapter;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.data.LocalFileHelper;
import com.paul.pipeline.fragment.model.PipePCRModel;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.model.DispatchModel;
import com.winters.boweilibrary.BoWei.model.experiment.RunningInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class PcrDataFragment extends Fragment {
    private static final String ARG_PARAM2 = "index";
    private int pcrindex;

    public PcrDataFragment() {
        currentProject = null;
    }

    public static PcrDataFragment newInstance(int index) {
        PcrDataFragment fragment = new PcrDataFragment();
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

    FragmentPcrDataBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_pcr_data, container, false);
        rootView =  binding.getRoot();
        initBinding();
        return rootView;
    }
    View rootView;
    PipeMainViewModel maimViewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        maimViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        initModel();
    }
    private void initBinding() {
        initView();
    }
    PipePCRModel pcrmodel;
    public void initModel(){
        if(maimViewModel!=null) {
            pcrmodel = maimViewModel.findPipePCRModel(getContext(), pcrindex);
            if (pcrmodel != null) {
//                Log.i("ArmTaskWorker", "initModel pcr: " +pcrindex);
                binding.setModel(pcrmodel);
                binding.executePendingBindings();
                refreshPcrViews();
            }
        }
    }

    public void clearModel(){
        initModel();
        initView();
        clearStepsView();
    }

    private void clearStepsView(){
        currentProject = null;
        if(null != mProgramTitleLayout){
            mProgramTitleLayout.removeAllViews();
        }
        if(mSLANSections!=null) {
            mSLANSections.clear();
        }
        if(null != stepAdapter) {
            stepAdapter.clearCurrentPosition();
            stepAdapter.notifyDataSetChanged();
        }
        if(null != mProgramRecycleLayout){
            mProgramRecycleLayout.removeAllViews();
        }
        if(programRv!=null){
            programRv.removeAllViews();
        }
    }
    public void refreshPcrViews(){
        if(pcrmodel.status.get()!=null) {
            switch (pcrmodel.status.get()) {
                case ConstantData.str_FREE:
                    clearStepsView();
                    break;
                case ConstantData.str_ACK:
                    break;
                case ConstantData.str_RUN:
                    initSteps();
                    break;
                case ConstantData.str_FINISH:
                    break;
                case ConstantData.str_ERROR:
                    break;
                default:
                    break;
            }
        }
    }

    private RecyclerView programRv;
    private LinearLayout mProgramTitleLayout;
    private LinearLayout mProgramRecycleLayout;
    private List<ConfigSLANSection> mSLANSections;
    private MiniSLANStepAdapter stepAdapter;
    private DecimalFormat nf = new DecimalFormat( "0.00");

    private String before_program = "程序段-1";
    private String before_step = "步骤-1";
    private int before_loop;
    private long time = 0;
    private void initView(){
        mProgramTitleLayout = (LinearLayout)rootView.findViewById(R.id.bioway_first_pcr_program_title_layout);
        mProgramRecycleLayout = (LinearLayout)rootView.findViewById(R.id.bioway_first_pcr_program_recycle_layout);
        programRv = (RecyclerView)rootView.findViewById(R.id.bioway_first_pcr_rv);
        mSLANSections = new ArrayList<ConfigSLANSection>();
        programRv.setLayoutManager(new LinearLayoutManager(getActivity(), RecyclerView.HORIZONTAL, false));
        programRv.addItemDecoration(new DividerDecoration(getActivity(), 2));
        stepAdapter = new MiniSLANStepAdapter(getActivity().getApplicationContext());
        stepAdapter.setProgramSteps(mSLANSections);
        programRv.setAdapter(stepAdapter);
    }

    private void updateRunningState(String cur_program,String cur_step,int loop){
//        RunningInfo runningInfo = new RunningInfo();
//        String cur_program  = runningInfo.getSegment();//当前程序段
//        String cur_step = runningInfo.getStep();//当前步骤
//        int loop = runningInfo.getCycle();
        if(null != cur_step && !cur_step.contains("步骤")){
            cur_step = "步骤"+cur_step;
        }
        // cur_program 可能是数字段号(1-based)，也可能是段名(如"熔解程序段")。
        // 若非纯数字，则按段名查真实位置(getProgramIndex 返回 0-based)，+1 对齐 setCurrentPosition 内部的 -1。
        int program_index = StringUtil.convertToInt(cur_program, -1);
        if(program_index == -1){
            int nameIdx = getProgramIndex(cur_program);
            program_index = (nameIdx == -1) ? 0 : nameIdx + 1;
        }
        if(null != mSLANSections && mSLANSections.size()>0){
            if (!StringUtil.isEmpty(before_program)&&!before_program.equals(cur_program) || !StringUtil.isEmpty(before_step)&&!before_step.equals(cur_step) || before_loop != loop){

                int step_num = -1;
                if(null != cur_step &&cur_step.contains("步骤")){
                    String temp = cur_step.replace("步骤","");
                    step_num = StringUtil.convertToInt(temp,0);
                }else{
                    step_num = 1;
                }
                if(null != stepAdapter){
//                    Log.i("ArmTaskWorker","step_num" +step_num );
                    if(!StringUtil.isEmpty(cur_program) && cur_program.contains("熔解")){// if(!StringUtil.isEmpty(cur_program) && cur_program.equals("熔解程序段")){
                        step_num = 1;
                        stepAdapter.setCurrentPosition(step_num,program_index);
                    }else{
                        stepAdapter.setCurrentPosition(step_num,program_index);
                    }

                    before_program = cur_program;
                    before_step = cur_step;
                    before_loop = loop;
//                    Log.i("ArmTaskWorker","program_index " +program_index + " cur_step " + cur_step +" loop " +loop );
                }

                //更新剩余循环
                if(null != mProgramRecycleLayout){
                    if(program_index != -1 && program_index >= 1 && program_index <= mSLANSections.size()){
                        ConfigSLANSection section =  mSLANSections.get(program_index-1);
                        if(section.getProgramType() == Constants.PROGRAM_TYPE_AMP){
                            View view = mProgramRecycleLayout.getChildAt(program_index);
                            if( view instanceof TextView){
                                TextView textView = (TextView)view;
                                //剩余循环
                                int recylceNum = mSLANSections.get(program_index).getCycles();
                                textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_last_recycle) + (recylceNum-loop));
//                                Log.i("ArmTaskWorker","recylceNum " +recylceNum +" lastloop " +(recylceNum-loop) );
                            }
                        }
                    }
                }
            }
        }
    }
    /**
     * 返程序段的index
     */
    private int getProgramIndex(String cur_program_name){
        if(null != mSLANSections && mSLANSections.size()>0){
            for(int i= 0;i<mSLANSections.size();i++){
                if(mSLANSections.get(i).getName().equals(cur_program_name)){
                    return i;
                }
            }
        }
        return -1;
    }
    private ArrayList<ConfigSLANSection> getSLANSections(ArrayList<Section> sections){
        ArrayList<ConfigSLANSection> SLANSections = new ArrayList<>();
        if(sections.size()>0){
//            mSLANSections.clear();
            for(int i= 0;i<sections.size();i++){
                Section section = sections.get(i);
                if(section != null){
                    if(section instanceof SectionAMP){
                        SectionAMP sectionAMP = (SectionAMP) section;
                        ConfigSLANSection slanSection = new ConfigSLANSection();
                        slanSection.setName(sectionAMP.getName());
                        slanSection.setCycles(sectionAMP.getCycles());
                        slanSection.setIdx(sectionAMP.getIdx());
                        slanSection.setProgramType(Constants.PROGRAM_TYPE_AMP);//扩增
                        ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<ConfigSLANSection.Step>();
                        if(sectionAMP.getItems() != null && sectionAMP.getItems().size()>0){
                            for(int k=0;k<sectionAMP.getItems().size();k++){
                                Step step = sectionAMP.getItems().get(k);
                                if(null != step){
                                    ConfigSLANSection.Step slanStep = new ConfigSLANSection.Step();
                                    slanStep.setStepName(step.getName());
                                    slanStep.setTemperature(step.getTemperature());
                                    slanStep.setHoldTime(step.getHoldTime());
                                    slanStep.setStepType(Constants.PROGRAM_TYPE_AMP);
                                    slanStep.setStep_index(k);
                                    slanStep.setSectionIndex(sectionAMP.getIdx());
                                    slanItems.add(slanStep);
                                }
                            }
                        }
                        slanSection.setItems(slanItems);
                        SLANSections.add(slanSection);

                    }else if(section instanceof SectionMelt){
                        SectionMelt sectionMelt = (SectionMelt) section;
                        ConfigSLANSection slanSection1 = new ConfigSLANSection();
                        slanSection1.setName(sectionMelt.getName());
                        slanSection1.setIdx(sectionMelt.getIdx());
                        slanSection1.setProgramType(Constants.PROGRAM_TYPE_MELT);//熔解段
                        slanSection1.setRate(sectionMelt.getRate());
                        slanSection1.setScanMode(sectionMelt.getScanMode());
                        if(sectionMelt.getInterval()== 0.1f|| sectionMelt.getInterval()== 0.2f
                                || sectionMelt.getInterval() == 0.05f){
                            slanSection1.setInterval(sectionMelt.getInterval());
                        }else{
                            slanSection1.setInterval((double)0.1f);
                        }

                        slanSection1.setMeltHoldtime(sectionMelt.getHoldTime());

                        ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<>();
                        ConfigSLANSection.Step slanStep = new ConfigSLANSection.Step();
                        slanStep.setStepName("");
                        slanStep.setLeftTemperature(sectionMelt.getStartTemp());
                        slanStep.setRightTemperature(sectionMelt.getEndTemp());
                        slanStep.setSectionIndex(sectionMelt.getIdx());
                        slanStep.setStep_index(0);
                        slanStep.setTemperature(sectionMelt.getEndTemp());
                        slanStep.setHoldTime(sectionMelt.getHoldTime());
                        slanStep.setStepType(Constants.PROGRAM_TYPE_MELT);
                        slanItems.add(slanStep);
                        slanSection1.setItems(slanItems);
                        SLANSections.add(slanSection1);
                    }
                }

            }
        }
        return SLANSections;
    }
    Project currentProject = null;
    public void initSteps(){
        try {
            if(pcrmodel!=null) {
                if (pcrmodel.currentTray.get() != null && pcrmodel.dispatchindex.get() != null) {
                    DispatchModel dispatchModel = TrayManager.getInstance().getDispatchModel(pcrmodel.currentTray.get(), pcrmodel.dispatchindex.get());
                    if (dispatchModel != null) {
                        String path = FileUtils.CONFIG_TEMPLATE_PATH;//CONFIG_SLAN_PATH
                        String tname = "first.prj";
                        if (dispatchModel.path.get() != null) {
                            path = dispatchModel.path.get();
                            Log.i("PcrDataFragment", "initSteps pcr: " + pcrindex + " path:" + dispatchModel.path.get());
                        }
                        if (dispatchModel.templatename.get() != null) {
                            tname = dispatchModel.templatename.get();
                            Log.i("PcrDataFragment", "initSteps pcr: " + pcrindex + " template:" + tname);
                        }
                        if (currentProject == null) {
                            currentProject = LocalFileHelper.openSLANExFile(path, tname);
                            if(currentProject!=null) {
                                Log.w("PcrDataFragment", "initSteps project: " + currentProject.getName());
                            }
                        }
                        if (currentProject != null) {
                            mSLANSections.clear();
                            Program program = currentProject.getProgram();
                            //转化为String
                            ArrayList<Section> sections = program.getSections();
                            if (sections != null) {
                                ArrayList<ConfigSLANSection> sLANSections = getSLANSections(sections);
                                if (sLANSections != null) {
                                    mSLANSections.addAll(sLANSections);
                                }
                                Log.w("PcrDataFragment", "initSteps mSLANSections" + mSLANSections.size());
                            }
                            stepAdapter.setProgramSteps(mSLANSections);
                            stepAdapter.notifyDataSetChanged();
                        }
                        if (currentProject != null) {
                            initpcrsuccess();
                            updateSteps();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PcrDataFragment","initSteps:"+e.getMessage());
        }
    }
    public void updateSteps(){
        try {
            if(pcrmodel!=null) {
                String section = "";
                if (pcrmodel.currentSection.get() != null) {
                    section = pcrmodel.currentSection.get();
                }
                String step = "";
                if (pcrmodel.currentStep.get() != null) {
                    step = pcrmodel.currentStep.get();
                }
                int circle = 0;
                if (pcrmodel.currentCircle.get() != null) {
                    circle = pcrmodel.currentCircle.get();
                }
                Log.i("PcrDataFragment", "updateSteps pcr: " +pcrindex +" section:"+section +" step:"+step +" circle:"+circle);
                updateRunningState(section, step, circle);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PcrDataFragment","updateSteps:"+e.getMessage());
        }
    }
    private void initpcrsuccess(){
        try{
            if(null != mSLANSections){
                if(mSLANSections.size()>0) {
                    if (getContext() != null) {
                        if (null != mProgramTitleLayout) {
                            mProgramTitleLayout.removeAllViews();
                        }
                        for (int i = 0; i < mSLANSections.size(); i++) {
                            TextView textView = new TextView(getContext());
                            textView.setBackgroundResource(R.color.blue);
                            textView.setTextSize(10);
                            textView.setGravity(Gravity.CENTER);
                            textView.setTextColor(0xffffffff);
                            int programType = mSLANSections.get(i).getProgramType();
                            if (programType == Constants.PROGRAM_TYPE_AMP) {
                                final LinearLayout.LayoutParams layoutParams =
                                        new LinearLayout.LayoutParams(102 * mSLANSections.get(i).getItems().size(), LinearLayout.LayoutParams.MATCH_PARENT);
                                layoutParams.setMargins(1, 0, 0, 0);//162
                                textView.setLayoutParams(layoutParams);
                                int index = i + 1;
                                textView.setText(getActivity().getResources().getString(R.string.runing_pcr_section) + index);
                            } else if (programType == Constants.PROGRAM_TYPE_MELT) {
                                final LinearLayout.LayoutParams layoutParams =
                                        new LinearLayout.LayoutParams(102 * 2, LinearLayout.LayoutParams.MATCH_PARENT);
                                layoutParams.setMargins(1, 0, 1, 0);//162*2
                                textView.setLayoutParams(layoutParams);
                                textView.setText(getActivity().getResources().getString(R.string.runing_pcr_section_melt));
                            }
                            mProgramTitleLayout.addView(textView);
                        }

                        if (null != mProgramRecycleLayout) {
                            mProgramRecycleLayout.removeAllViews();
                        }
                        for (int i = 0; i < mSLANSections.size(); i++) {
                            TextView textView = new TextView(getActivity().getApplicationContext());
                            textView.setBackgroundResource(R.color.color_recycle);
                            textView.setTextSize(10);
                            textView.setGravity(Gravity.CENTER);
                            textView.setTextColor(0xffffffff);

                            int programType = mSLANSections.get(i).getProgramType();
                            if (programType == Constants.PROGRAM_TYPE_AMP) {
                                final LinearLayout.LayoutParams layoutParams =
                                        new LinearLayout.LayoutParams(102 * mSLANSections.get(i).getItems().size(), LinearLayout.LayoutParams.MATCH_PARENT);
                                layoutParams.setMargins(1, 0, 0, 0);//162
                                textView.setLayoutParams(layoutParams);
                                textView.setText(getActivity().getResources().getString(R.string.running_pcr_recycle_num) + (mSLANSections.get(i).getCycles()));
                            } else if (programType == Constants.PROGRAM_TYPE_MELT) {
                                final LinearLayout.LayoutParams layoutParams =
                                        new LinearLayout.LayoutParams(102 * 2, LinearLayout.LayoutParams.MATCH_PARENT);
                                layoutParams.setMargins(1, 0, 1, 0);//162*2
                                textView.setLayoutParams(layoutParams);
                                //textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_rate) + nf.format(mSLANSections.get(i).getRate())+"℃/s");
                                textView.setText(getActivity().getResources().getString(R.string.ruuning_pcr_scanmode_step) + "  "
                                        + getActivity().getResources().getString(R.string.ruuning_pcr_interval) + nf.format(mSLANSections.get(i).getInterval()) + "℃" + " "
                                        + getActivity().getResources().getString(R.string.ruuning_pcr_melt_holdtime) + mSLANSections.get(i).getMeltHoldtime() + "s");
                            }
                            mProgramRecycleLayout.addView(textView);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("PcrDataFragment","initpcrsuccess:"+e.getMessage());
        }
    }
}