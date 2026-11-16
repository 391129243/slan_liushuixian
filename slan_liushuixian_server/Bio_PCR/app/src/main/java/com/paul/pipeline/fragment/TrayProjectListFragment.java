package com.paul.pipeline.fragment;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.FragmentTrayProjectListBinding;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.LinearSpacingItemDecoration;
import com.paul.pipeline.adapter.TemplateListAdapter;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.data.LocalFileHelper;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.model.TemplateModel;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.fragment.viewmodel.TrayDetailViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 载入模板后跳转的页面
 */
public class TrayProjectListFragment extends Fragment {

    private FragmentTrayProjectListBinding binding;

    public static TrayProjectListFragment newInstance() {
        return new TrayProjectListFragment();
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_tray_project_list, container, false);
        initBinding();
        return binding.getRoot();
    }

    private void initBinding() {
        tempListfull = new ArrayList<>();
        tempListrna = new ArrayList<>();
        tempListpcr = new ArrayList<>();
        mTempalteView = binding.templateListRecycleview;
        templateList = new ArrayList<>();
        initTemplateCycleview();
        binding.templatetypeRg.check(R.id.full_rb);
        binding.templatetypeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId==R.id.full_rb){
                    selectType = ConstantData.TYPE_FULL;
                    setTemplateList(selectType,tempListfull);
                    viewModel.radioGroupTempType.set(selectType);
                    viewModel.tempTypeName.set(getString(R.string.recovery_ex_type_full_procee));
                }else if (checkedId==R.id.rna_rb){
                    selectType = ConstantData.TYPE_RNA;
                    setTemplateList(selectType,tempListrna);
                    viewModel.radioGroupTempType.set(selectType);
                    viewModel.tempTypeName.set(getString(R.string.recovery_ex_type_rna));
                }else if (checkedId==R.id.pcr_rb){
                    selectType = ConstantData.TYPE_PCR;
                    setTemplateList(selectType,tempListpcr);
                    viewModel.radioGroupTempType.set(selectType);
                    viewModel.tempTypeName.set(getString(R.string.recovery_ex_type_pcr));
                }
            }
        });
    }
    TrayDetailViewModel viewModel;
    PipeMainViewModel mainViewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        viewModel = new ViewModelProvider(requireParentFragment()).get(TrayDetailViewModel.class);
        binding.setViewModel(viewModel);
        binding.executePendingBindings();
        init();

    }
    public void init(){
        if(viewModel!=null){
            viewModel.tempTypeName.set(getString(R.string.recovery_ex_type_full_procee));
        }
        selectModel = null;
        selectType = ConstantData.TYPE_FULL;
        initFileList();
        setTemplateList(selectType,tempListfull);
        binding.templatetypeRg.check(R.id.full_rb);
    }
    @Override
    public void onResume() {
        super.onResume();

    }
    public boolean confirmSelected(){
        if(selectModel!=null) {
            viewModel.setTemplateType(selectType);
            viewModel.setTemplateSelected(selectModel);
            return true;
        }else{
            mainViewModel.setAlertInfo(getString(R.string.alert_not_select_project));
            return false;
        }
    }
    private List<String> tempListfull;
    private List<String> tempListrna;
    private List<String> tempListpcr;
    public void initFileList(){
        APLManager.getInstance().initTemplateList();
        tempListfull = APLManager.getInstance().tempListfull;
        tempListrna = APLManager.getInstance().tempListrna;
        tempListpcr = APLManager.getInstance().tempListpcr;
    }
    private int selectType;
    private TemplateModel selectModel;
    private RecyclerView mTempalteView;
    private ArrayList<TemplateModel> templateList;
    private TemplateListAdapter mTempAdapter;
    private void initTemplateCycleview(){
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),LinearLayoutManager.VERTICAL,false);
        mTempalteView.setLayoutManager(layoutManager);
        mTempalteView.addItemDecoration(new LinearSpacingItemDecoration(getContext(), 2));//间距
        mTempAdapter = new TemplateListAdapter(getContext(), templateList, (view, position) -> {
            for (int i=0;i<templateList.size();i++) {
                TemplateModel model = templateList.get(i);
                if(i==position){
                    model.selected.set(true);
                    selectModel = model;
                }else{
                    model.selected.set(false);
                }
            }
            mTempAdapter.notifyDataSetChanged();
        });
        mTempalteView.setAdapter(mTempAdapter);
    }
    public void setTemplateList(int type,List<String> list){
        templateList.clear();
        for (String name:list) {
            TemplateModel model = new TemplateModel();
            model.selectType.set(type);
            if(!StringUtil.isEmpty(name)){
                model.name.set(name);
                if(name.contains("~")){
                    String[] names = name.split("~");
                    if(names != null && names.length > 0){
                        String fname = names[0];
                        if(fname!=null&&fname.contains("-")){
                            String[] fnames = fname.split("-");
                            if(fnames!=null&&fnames.length>0){
                                String tempno = fnames[0];
                                switch(type) {
                                    case ConstantData.TYPE_FULL:
                                        model.templateno.set("KF" + tempno);
                                        break;
                                    case ConstantData.TYPE_RNA:
                                        model.templateno.set("KR" + tempno);
                                        break;
                                    case ConstantData.TYPE_PCR:
                                        model.templateno.set("KP" + tempno);
                                        break;
                                }
                            }
                        }
                        switch (names.length){
                            case 1:
                                String firstName = names[0];
                                model.firstName.set(firstName);
                                model.trim_name.set(firstName);
                                break;
                            case 2:
                                String secondName = names[0] + "\n" + names[1];
                                model.firstName.set(names[0]);
                                model.secondName.set(names[1]);
                                model.trim_name.set(secondName);
                                break;
                            case 3:
                                String thirdName = names[0] + "\n" + names[1] + "\n" + names[2];
                                model.firstName.set(names[0]);
                                model.secondName.set(names[1]);
                                model.thirdName.set(names[2]);
                                model.trim_name.set(thirdName);
                                break;
                        }
                    }
                }
            }
            templateList.add(model);
        }
        mTempAdapter.notifyDataSetChanged();
    }
}