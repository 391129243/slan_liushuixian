package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;
import com.gidi.bio_pcr.config.ConfigManager;
import com.gidi.bio_pcr.databinding.FragmentTrayProjectInfoBinding;
import com.gidi.bio_pcr.util.FileUtils;
import com.hongshi.slansdk.slanenum.TemperatureControlMode;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.model.TemplateModel;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.fragment.viewmodel.TrayDetailViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrayProjectInfoFragment extends Fragment {
    final String TAG = "TrayProjectInfoFragment";
    private FragmentTrayProjectInfoBinding binding;

    public static TrayProjectInfoFragment newInstance() {
        return new TrayProjectInfoFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_tray_project_info, container, false);
        initBinding();
        return binding.getRoot();
    }

    private void initBinding() {
        hideInfoLayout();
    }
    PipeMainViewModel mViewModel;
    TrayDetailViewModel viewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        viewModel = new ViewModelProvider(requireParentFragment()).get(TrayDetailViewModel.class);
        binding.setViewModel(viewModel);
        binding.executePendingBindings();
        viewModel.getTemplateSelected().observe(getViewLifecycleOwner(), templateModel -> {
            //PcrApplication.rootTempName = templateModel.name.get();
            initModelInfo(templateModel);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(null != peakFileNames){
            peakFileNames.clear();
            peakFileNames = null;
        }
        if(null != ctFileNames){
            ctFileNames.clear();
            ctFileNames.clear();
        }
    }

    private List<File> getConfigTemplateFiles(String path){
        List<File> configFiles = new ArrayList<>();
        File[] files = new File(path).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != configFiles) {
                    configFiles.add(files[i]);
                    Collections.sort(configFiles);
                }
            }
        }
        return configFiles;
    }
//    private boolean isHasSecFile(String path){
//        File[] files = new File(path).listFiles();//文件夹下的所有文件
//        if(files != null && files.length>0) {
//            for (int i = 0; i < files.length; i++) {
//                if(files[i].getName().contains("V5-12多重二次试剂")){
//                    return true;
//                }
//
//            }
//        }
//        return false;
//    }

    private String CONFIG_FULLPROCESS_TEMPLATE_PATH = FileUtils.FULL_PROCESS_TEMPLATE_PATH;
    private String CONFIG_SINGLERNA_TEMP_PATH = FileUtils.SINGLE_RNA_TEMPLATE_PATH ;
    private String CONFIG_SINGLE_PCR_TEMPLATE_PATH = FileUtils.SINGLE_PCR_TEMPLATE_PATH ;

    private void initModelInfo(TemplateModel model){
        switch(model.selectType.get()){
            case ConstantData.TYPE_FULL:
                //编辑模板
                String localTempPath = CONFIG_FULLPROCESS_TEMPLATE_PATH + File.separator
                        + model.name.get() + File.separator;
                //找到文件夹中的所有文件
                List<File> exportFiles = getConfigTemplateFiles(localTempPath);
                Log.v(TAG,model.name.get()+" exportFiles_full:"+exportFiles.size());
                if (null != exportFiles && exportFiles.size() > 0) {
//                    boolean isHasSecFile = isHasSecFile(localTempPath);
//                    Log.v(TAG," isHasSecFile:"+isHasSecFile);
                    Log.v(TAG,"initModelInfo files :"+exportFiles.size());
                    if (exportFiles.size() >= 5 ) {//&& isHasSecFile
                        model=  initFullNulti(model,exportFiles);
                    }else if(exportFiles.size()>=3 && exportFiles.size()<=7 ){//&& !isHasSecFile
                        model = initFullSingle(model,exportFiles);
                    }
                }
                break;
            case ConstantData.TYPE_RNA:
                //编辑模板
                String localTempPath_rna = CONFIG_SINGLERNA_TEMP_PATH + File.separator
                        + model.name.get() + File.separator;
                //找到文件夹中的所有文件
                List<File> exportFiles_rna = getConfigTemplateFiles(localTempPath_rna);
                Log.v(TAG,model.name.get()+" exportFiles_rna:"+exportFiles_rna.size());
                model=initRna(model,exportFiles_rna);
                break;
            case ConstantData.TYPE_PCR:
                //编辑模板
                String localTempPath_pcr = CONFIG_SINGLE_PCR_TEMPLATE_PATH + File.separator
                        + model.name.get() + File.separator;
                //找到文件夹中的所有文件
                List<File> exportFiles_pcr = getConfigTemplateFiles(localTempPath_pcr);
                Log.v(TAG,model.name.get()+" exportFiles_pcr:"+exportFiles_pcr.size());
                model=initPcr(model,exportFiles_pcr);
                break;
        }
        viewModel.setTemplateModel(model);
        binding.setModel(model);
        binding.executePendingBindings();
    }
    private void hideInfoLayout(){
        binding.llFullMulti.setVisibility(View.GONE);
        binding.llFullSingle.setVisibility(View.GONE);
        binding.llRna.setVisibility(View.GONE);
        binding.llPcr.setVisibility(View.GONE);
    }

    //============全流程单重格式=================//
    List<String> peakFileNames = new ArrayList<>();
    List<String> ctFileNames = new ArrayList<>();
    private TemplateModel initFullSingle(TemplateModel model,List<File> editFiles){
        hideInfoLayout();
        binding.llFullSingle.setVisibility(View.VISIBLE);
        model.templateType.set(ConstantData.TEMPLATE_FULL_SINGLE);
        String localTempPath = CONFIG_FULLPROCESS_TEMPLATE_PATH +File.separator+ model.name.get();
        model.path.set(localTempPath);
//        List<File> editFiles = FileUtils.getFileList(localTempPath);
        ctFileNames.clear();
        peakFileNames.clear();
        Log.v(TAG,"single editFiles:"+editFiles.size());
        if(null != editFiles && editFiles.size()>0) {
            for (File file : editFiles) {
                if (file.getName().endsWith(".hex")) {
                    //核酸检测文件
                    String hexFileName = file.getName().substring(0, file.getName().length() - 4);
                    binding.singleFullRnaNameTv.setText(hexFileName + ".hex");
                    model.single_hex.set(hexFileName + ".hex");
                } else if (file.getName().endsWith(".prj")) {
                    String pcrFileName = file.getName().substring(0, file.getName().length() - 4);
                    binding.singleFullPcrNameTv.setText(pcrFileName + ".prj");
                    model.single_prj.set(pcrFileName + ".prj");
                } else if (file.getName().endsWith(".ct")) {
                    String ctFileName = file.getName();
                    ctFileNames.add(ctFileName);
                   // binding.singleFullJudgeNameTv.setText(ctFileName);
                   // model.ct.set(ctFileName);//xxs修改 20260725

                } else if (file.getName().endsWith(".peak")) {
                    String peakFileName = file.getName();
                    peakFileNames.add(peakFileName);
                    //binding.singleFullPeakNameTv.setText(peakFileName);
                   // model.peak.set(peakFileName);//xxs修改20260725
                } else if (file.getName().equals("totaltime.txt")) {
                    //全流程时间
                    TotalTime totalTime = ConfigManager.getTotalTimeFile(file);
                    binding.singleFullTotaltimeEt.setText(totalTime.getTotalTime() + "");
                    model.totaltime.set(totalTime);
                    int temparatureCtrlMode = totalTime.getTemperatureCtrlMode();
                    Log.i("info", "temparatureCtrlMode" + temparatureCtrlMode);
                    if (temparatureCtrlMode == TemperatureControlMode.Fast.getValue()) {
                        //快速
                        binding.singleFullTempctrlFast.setChecked(true);
                        model.temparatureCtrl.set(ConstantData.TemparatureCtrl_fast);
                    } else if (temparatureCtrlMode == TemperatureControlMode.Standard.getValue()) {
                        //标准
                        binding.singleFullTempctrlStandard.setChecked(true);
                        model.temparatureCtrl.set(ConstantData.TemparatureCtrl_stardard);
                    }
                }
            }
        }
        // 显示所有峰值文件（用逗号分隔）xxs修改
        if (!peakFileNames.isEmpty()) {
            String allPeakFiles = TextUtils.join(" | ", peakFileNames);
            binding.singleFullPeakNameTv.setText(allPeakFiles);
            model.peakList.set(peakFileNames);
         /*   // 多个峰值文件时，用分隔符拼接
            model.peak.set(TextUtils.join(",", peakFileNames));*/
        }
        if (!ctFileNames.isEmpty()) {
            String allCtFiles = TextUtils.join(" | ", ctFileNames);
            binding.singleFullJudgeNameTv.setText(allCtFiles);
            model.ctList.set(ctFileNames);
     /*       // 多个峰值文件时，用分隔符拼接
            model.peak.set(TextUtils.join(",", peakFileNames));*/
        }
        return model;
    }
    //======
    private TemplateModel initFullNulti(TemplateModel model,List<File> editFiles) {
        hideInfoLayout();
        binding.llFullMulti.setVisibility(View.VISIBLE);
        model.templateType.set(ConstantData.TEMPLATE_FULL_MULTI);
        peakFileNames.clear();
        ctFileNames.clear();
        //编辑多重模板文件
        String localTempPath = CONFIG_FULLPROCESS_TEMPLATE_PATH + File.separator+ model.name.get() ;
        model.path.set(localTempPath);
//        List<File> editFiles = getConfigTemplateFiles(localTempPath);
        if (null != editFiles && editFiles.size() > 0) {
            for (File file : editFiles) {
                if (file.getName().endsWith(".hex") && file.getName().startsWith("first")) {
                    //核酸检测文件
                    String firstHexFileName = file.getName();
                    model.multi_hex_first.set(firstHexFileName );
                    binding.multiFullFirstRnaNameTv.setText(firstHexFileName );
//                    openRNAFile(firstHexFileName);
                } else if (file.getName().endsWith(".prj") && file.getName().startsWith("first")) {
                    String firstPCRName = file.getName();
                    model.multi_prj_first.set(firstPCRName );
                    binding.multiFullFirstPcrNameTv.setText(firstPCRName );
                } else if (file.getName().endsWith(".hex") && file.getName().startsWith("second")) {
                    //核酸检测文件
                    String secHexFileName = file.getName();
                    model.multi_hex_second.set(secHexFileName );
                    binding.multiFullSecondRnaNameTv.setText(secHexFileName);
                } else if (file.getName().endsWith(".prj") && file.getName().startsWith("second")) {
                    String secondPcrName = file.getName();
                    model.multi_prj_second.set(secondPcrName);
                    binding.multiFullSecondPcrNameTv.setText(secondPcrName);
                } else if (file.getName().endsWith(".peak")) {
                    String peakFileName = file.getName();
                    peakFileNames.add(peakFileName);
                   // model.peak.set(peakFileName);
                   // binding.multiFullPeakFileNameTv.setText(peakFileName);//peak
                } else if (file.getName().endsWith(".ct")) {
                    String ctFileName = file.getName();
                    ctFileNames.add(ctFileName);
                    //model.ct.set(ctFileName);
                    //binding.multiFullJudgeNameTv.setText(ctFileName);//ct
                } else if (file.getName().equals("totaltime.txt")) {
                    //全流程时间
                    TotalTime totalTime = ConfigManager.getTotalTimeFile(file);
                    binding.multiFullTotaltimeEt.setText(totalTime.getTotalTime() + "");
                    model.totaltime.set(totalTime);
                    int temparatureCtrlMode = totalTime.getTemperatureCtrlMode();
                    if (temparatureCtrlMode == TemperatureControlMode.Fast.getValue()) {
                        //快速
                        binding.multiFullTempctrlFast.setChecked(true);
                        model.temparatureCtrl.set(ConstantData.TemparatureCtrl_fast);
                    } else if (temparatureCtrlMode == TemperatureControlMode.Standard.getValue()) {
                        //标准
                        binding.multiFullTempctrlStandard.setChecked(true);
                        model.temparatureCtrl.set(ConstantData.TemparatureCtrl_stardard);
                    }
                }
            }

        }
        if (!peakFileNames.isEmpty()) {
            String allPeakFiles = TextUtils.join(" | ", peakFileNames);
            binding.multiFullPeakFileNameTv.setText(allPeakFiles);
            model.peakList.set(peakFileNames);
         /*   // 多个峰值文件时，用分隔符拼接
            model.peak.set(TextUtils.join(",", peakFileNames));*/
            Log.i(TAG,"==allPeakFiles=="+ allPeakFiles);
        }
        if (!ctFileNames.isEmpty()) {
            String allCtFiles = TextUtils.join(" | ", ctFileNames);
            binding.multiFullJudgeNameTv.setText(allCtFiles);
            model.ctList.set(ctFileNames);
     /*       // 多个峰值文件时，用分隔符拼接
            model.peak.set(TextUtils.join(",", peakFileNames));*/
            Log.i(TAG,"==allCtFiles=="+ allCtFiles);
        }
        return model;
    }
    private TemplateModel initRna(TemplateModel model,List<File> editFiles){
        hideInfoLayout();
        binding.llRna.setVisibility(View.VISIBLE);
        model.templateType.set(ConstantData.TEMPLATE_RNA);
        //编辑模板
        String localHexTempPath = CONFIG_SINGLERNA_TEMP_PATH+File.separator+ model.name.get();
        model.path.set(localHexTempPath);
//        List<File> editFiles = FileUtils.getFileList(localHexTempPath);
        if(null != editFiles && editFiles.size()>0){
            for(File file :editFiles){
                if(file != null && file.getName().endsWith(".hex")) {
                    //核酸检测文件
                    String  hexFileName = file.getName().substring(0, file.getName().length() - 4);
                    binding.rnaNameTv.setText(hexFileName + ".hex");
                    model.rna_hex.set(hexFileName + ".hex");
//                    openRNAFile(hexFileName);
                }else if(file.getName().equals("totaltime.txt")){
                    //全流程时间
                    TotalTime totalTime = ConfigManager.getTotalTimeFile(file);
                    binding.rnaTotaltimeEt.setText(totalTime.getTotalTime()+"");
                    model.totaltime.set(totalTime);
                }
            }
        }
        return model;
    }

    private TemplateModel initPcr(TemplateModel model,List<File> editFiles){
        hideInfoLayout();
        binding.llPcr.setVisibility(View.VISIBLE);
        model.templateType.set(ConstantData.TEMPLATE_PCR);
        String localTempPath = CONFIG_SINGLE_PCR_TEMPLATE_PATH+File.separator + model.name.get();
        model.path.set(localTempPath);
        ctFileNames.clear();
        peakFileNames.clear();
//        List<File> editFiles = FileUtils.getFileList(localTempPath);
        if(null != editFiles &&editFiles.size()>0){
            for(File file :editFiles){
                if(file.getName().endsWith(".prj")){
                    String pcrFileName = file.getName().substring(0, file.getName().length() - 4);
                    binding.pcrNameTv.setText(pcrFileName + ".prj");
                    model.pcr_prj.set(pcrFileName + ".prj");
                }else if(file.getName().endsWith(".ct")){
                    String ctFileName = file.getName();
                    ctFileNames.add(ctFileName);
                   // binding.pcrCtNameTv.setText(ctFileName);
                   // model.ct.set(ctFileName);
                }else if(file.getName().endsWith(".peak")){
                    String peakFileName = file.getName();
                    peakFileNames.add(peakFileName);
                    //binding.pcrPeakNameTv.setText(peakFileName);
                   // model.peak.set(peakFileName);
                }else if(file.getName().equals("totaltime.txt")){
                    //全流程时间
                    TotalTime totalTime = ConfigManager.getTotalTimeFile(file);
                    binding.pcrTotaltimeEt.setText(totalTime.getTotalTime()+"");
                    model.totaltime.set(totalTime);
                   int temparatureCtrlMode = totalTime.getTemperatureCtrlMode();
                    if(temparatureCtrlMode == TemperatureControlMode.Fast.getValue()){
                        //快速
                        binding.pcrTempctrlFast.setChecked(true);
                        model.temparatureCtrl.set(ConstantData.TemparatureCtrl_fast);
                    }else if(temparatureCtrlMode == TemperatureControlMode.Standard.getValue()){
                        //标准
                        binding.pcrTempctrlStandard.setChecked(true);
                        model.temparatureCtrl.set(ConstantData.TemparatureCtrl_stardard);
                    }
                }
            }
        }
        if (!peakFileNames.isEmpty()) {
            String allPeakFiles = TextUtils.join(" | ", peakFileNames);
            binding.pcrPeakNameTv.setText(allPeakFiles);
            model.peakList.set(peakFileNames);
         /*   // 多个峰值文件时，用分隔符拼接
            model.peak.set(TextUtils.join(",", peakFileNames));*/
            Log.i(TAG,"==allPeakFiles=="+ allPeakFiles);
        }
        if (!ctFileNames.isEmpty()) {
            String allCtFiles = TextUtils.join(" | ", ctFileNames);
            binding.pcrCtNameTv.setText(allCtFiles);
            model.ctList.set(ctFileNames);
     /*       // 多个峰值文件时，用分隔符拼接
            model.peak.set(TextUtils.join(",", peakFileNames));*/
            Log.i(TAG,"==allCtFiles=="+ allCtFiles);
        }
        return model;
    }
}