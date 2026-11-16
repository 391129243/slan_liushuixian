package com.paul.pipeline.fragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.databinding.DataBindingUtil;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.WellInfo;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.databinding.FragmentTrayDetailBinding;
import com.gidi.bio_pcr.listener.FragmentKeyeventListener;
import com.gidi.bio_pcr.mgr.ScanKeyManager;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.paul.pipeline.activity.PipeMainActivity;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.data.ScanCodeHelper;
import com.paul.pipeline.manager.APLManager;
import com.paul.pipeline.manager.ArmManager;
import com.paul.pipeline.manager.TrayManager;
import com.paul.pipeline.model.DispatchModel;
import com.paul.pipeline.model.TemplateModel;
import com.paul.pipeline.fragment.model.PipeTrayModel;
import com.paul.pipeline.model.SampleHoleModel;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.fragment.viewmodel.TrayDetailViewModel;
import com.gidi.bio_pcr.util.StringUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrayDetailFragment extends Fragment implements FragmentKeyeventListener {
    final String TAG = "TrayDetailFragment";
    private int trayindex=0;
    int opentype=0;
    private ScanKeyManager scanKeyManager;
    /*是否是输入状态（输入时扫码监听不拦截）*/
    PipeMainActivity mainActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity instanceof AppCompatActivity) {
            mainActivity = (PipeMainActivity) activity;
            mainActivity.setFragmentKeyeventListener(this);
        }
    }

    @Override
    public boolean onFragmentKeyEvent(KeyEvent event) {
        if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
            scanKeyManager.analysisKeyEvent(event);
            return true;
        }
        return false;
    }

    TrayProjectInfoFragment projectInfoFragment;
    TrayProjectListFragment projectListFragment;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scanKeyManager = new ScanKeyManager(value -> {
            //获取最后两位
            if(!StringUtil.isEmpty(value)){
                viewModel.setScaninfo(value);
            }
        });
        projectInfoFragment = new TrayProjectInfoFragment();
        projectListFragment = new TrayProjectListFragment();
        getChildFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.project_fragment_container_view,projectListFragment)
                .add(R.id.project_fragment_container_view,projectInfoFragment)
                .hide(projectInfoFragment)
                .commit();
    }

    FragmentTrayDetailBinding binding;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_tray_detail, container, false);
        initBinding();
        return binding.getRoot();
    }

    PipeTrayModel trayModel;
    PipeMainViewModel mainViewModel;
    TrayDetailViewModel viewModel;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
        mainViewModel.getTrayClickEvent().observe(requireActivity(), s -> {
            if(1<=s && s<=8){
                trayindex = s;
                initType(opentype);
            }
        });
        mainViewModel.getTrayEdittype().observe(requireActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                opentype = integer;
                initType(opentype);
            }
        });
        viewModel = new ViewModelProvider(this).get(TrayDetailViewModel.class);
        initViewModel();
        binding.setViewModel(viewModel);
        binding.executePendingBindings();
        initFragment();
    }
    private void initType(int value){
        if(value==ConstantData.TRAY_Add){
            initNewTraymodel();
        }else if(value==ConstantData.TRAY_Edit){
            initTraymodel();
        }
    }
    private void initTraymodel(){
        trayModel = mainViewModel.findPipeTrayModel(getContext(),trayindex);
        //  chengeUrgent(trayModel.urgent.get());
        binding.setTrayModel(trayModel);
        binding.executePendingBindings();
        initSampleHoleListCycleView();
    }
    private void initNewTraymodel(){
        trayModel = new PipeTrayModel(trayindex,getString(R.string.pipe_main_trayname) + trayindex );
        binding.setTrayModel(trayModel);
        binding.executePendingBindings();
        initSampleHoleListCycleView();
        initFragment();
    }
    private void initFragment() {
        showFragment(0);
        if(viewModel!=null) {
            viewModel.selecttitle.set(getString(R.string.pipe_main_proj_choose));
        }
        binding.regentEt.setText("");
        binding.idcardEt.setText("");
        projectListFragment.init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainActivity = null;
//        scanKeyManager = null;
    }

    private void initBinding(){
        binding.ivBack.setOnClickListener(v -> close());
        binding.untouchview.setOnClickListener(v -> {
            //不实现，禁止外围区域触摸
        });
        binding.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkSampleHoleNotEmpty()&& checkProject()&& checkPatient()){
                    if(mtemplateModel!=null) {
                        String templateno = mtemplateModel.templateno.get();
                        String templatename = mtemplateModel.name.get();
                        String batch = TrayManager.getInstance().createBatchCode(templateno,templatename,trayindex);
                        ArrayList<DispatchModel> dispatchlist = APLManager.getInstance().createDispatchList(getContext(), batch, templateno, trayindex, mtemplateModel);
                        if (dispatchlist != null && dispatchlist.size() > 0) {
                            trayModel.dispatchlist.clear();
                            trayModel.dispatchlist.addAll(dispatchlist);
                            trayModel.flagstatus.set(ConstantData.FLAG_RUN);
                            trayModel.status.set(ConstantData.str_RUN);
                            trayModel.batch.set(batch);
                            trayModel.project.set(mtemplateModel.name.get());
                            trayModel.template.set(mtemplateModel);
                            trayModel.errorDispatchindex.set(0);//恢复正常
                            trayModel.dispatchindex.set(1);
                            trayModel.createTime.set(System.currentTimeMillis());
                            TrayManager.getInstance().saveNewModel(trayModel);
                            TrayManager.getInstance().updatePriority();
                            showLongToast(trayModel.name.get() + " " + "保存成功");
                            //                        mainViewModel.onNavClicked(ConstantData.NAV_TRAYMANAGE);
                            mainViewModel.onNavClicked(ConstantData.NAV_MAIN);
                            mainViewModel.setTrayChanged(trayindex);
                            trayOpenWorkDo(trayindex);//执行制冷，led
                        } else {
                            Log.e("ArmTaskWorker", "生成调度表失败！ tray:" + trayindex);
                            showLongToast(trayModel.name.get() + " " + "保存失败，生成调度表失败！");
                        }

//                    TrayManager.getInstance().setDispatchList(trayModel.index.get(), );
//                    for (SampleHoleModel hole: trayModel.holes) {
//                        Log.e("ArmTaskWorker", "index:"+ hole.index.get());
//                        Log.e("ArmTaskWorker", "code:"+ hole.reagentCode.get());
//                        Log.e("ArmTaskWorker", "number:"+ hole.serialNum.get());
//                    }
                    }else {
                        Log.e("ArmTaskWorker", "生成调度表失败！ tray:" + trayindex + " TemplateModel 为空值");
                        showLongToast(trayModel.name.get() + " " + "保存失败，生成调度表失败！"+ " TemplateModel 为空值");
                    }
                }
            }
        });
        binding.selectProjBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFragment(0);
            }
        });
        binding.confirmProjBtn.setOnClickListener(v -> {
           boolean result = projectListFragment.confirmSelected();
            if(result){
              //  viewModel.onNav(ConstantData.NAV_TRAY_PROJECT_Info);
                showFragment(1);
            }
        });
        binding.inputBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SampleHoleModel primary = viewModel.getSelectedHole().getValue();
                if(primary == null){
                    showLongToast("请选择目标孔位！");
                    return;
                }
                String snumber = binding.idcardEt.getText().toString();
                if(snumber==null||snumber.isBlank()||snumber.isEmpty()){
                    showLongToast("样本编号 不能为空！");
                    return;
                }
                String code = binding.regentEt.getText().toString();
                if(code==null||code.isBlank()||code.isEmpty()){
                    showLongToast("试剂条码 不能为空！");
                    return;
                }
                // A 与 B 共享数据:把输入同步到该 pair 中**可点击**的两个孔位
                // 若某一方 TextView 不可点击(无 onClickListener),则只写入可点击方
                int physicalNum = parsePairNumber(primary);  // 1~8
                int sampleNum = 9 - physicalNum;              // 1~8
                SampleHoleModel[] pair = getPairHoles(physicalNum);
                SampleHoleModel aHole = pair[0];
                SampleHoleModel bHole = pair[1];
                if (aHole != null && isHoleTvClickable(HOLE_A_IDS[sampleNum - 1])) {
                    aHole.serialNum.set(snumber);
                    aHole.reagentCode.set(code);
                    aHole.status.set(1);
                }
                if (bHole != null && isHoleTvClickable(HOLE_B_IDS[sampleNum - 1])) {
                    bHole.serialNum.set(snumber);
                    bHole.reagentCode.set(code);
                    bHole.status.set(1);
                }
                saveHole(primary);
                refreshHoleCellsBackground();
            }
        });
        binding.clearallcodeBtn.setOnClickListener(v -> {
            for (int i = 0; i < trayModel.holes.size(); i++){
                SampleHoleModel h = trayModel.holes.get(i);
                if (h == null) continue;
                h.reagentCode.set("");
                h.serialNum.set("");
                h.status.set(0);
            }
            // 同步清空 wellInfolist,否则实验结果页仍会读到旧数据
            clearAllWellInfo();
            refreshHoleCellsBackground();
        });
//        binding.saveallcodeBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mainViewModel.setTrayChanged(trayindex);
//            }
//        });
        binding.reagentCodeClearImg.setOnClickListener(v->{
            binding.regentEt.setText("");
            SampleHoleModel primary = viewModel.getSelectedHole().getValue();
            if (primary == null) return;
            int physicalNum = parsePairNumber(primary);//获取样本号
            if (physicalNum <= 0) return;
            SampleHoleModel[] pair = getPairHoles(physicalNum);
            for (SampleHoleModel h : pair) {
                if (h == null) continue;
                h.reagentCode.set("");
                if("".equals(h.serialNum.get())){
                    h.status.set(0);
                }
            }
            // 同步清空 wellInfolist 中该物理编号 A 行 + B 行的 reagentCode
            syncWellInfo(physicalNum, true, false);
            refreshHoleCellsBackground();
        });
        binding.serialNumClearImg.setOnClickListener(v->{
            binding.idcardEt.setText("");
            SampleHoleModel primary = viewModel.getSelectedHole().getValue();
            if (primary == null) return;
            int physicalNum = parsePairNumber(primary);
            if (physicalNum <= 0) return;
            SampleHoleModel[] pair = getPairHoles(physicalNum);
            for (SampleHoleModel h : pair) {
                if (h == null) continue;
                h.serialNum.set("");
                if("".equals(h.reagentCode.get())){
                    h.status.set(0);
                }
            }
            // 同步清空 wellInfolist 中该物理编号 A 行 + B 行的 sampleName
            syncWellInfo(physicalNum, false, true);
            refreshHoleCellsBackground();
        });
//        binding.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
//            if (checkedId==R.id.radioButton_show_proj) {
//                viewModel.onNav(1);
//            }else if(checkedId==R.id.radioButton_choose_proj) {
//                viewModel.onNav(0);
//            }
//        });
        binding.cbUrgent.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(trayModel!=null&&trayModel.urgent!=null) {
                    trayModel.urgent.set(isChecked);
                    //    chengeUrgent(isChecked);
                }
            }
        });

        //如果样本1有数据B8和A8是有数据的，则可以点击
        binding.pair1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //获取B8或者A8的serialNum和reagentCode分别显示在idcard_et和regent_et里

            }
        });

    }

    //提示信息不一致
    public void notifyPaitentUncorrect(){
        for (int i = 0; i < trayModel.holes.size();i++){
            trayModel.holes.get(i).scaninfo.set(getString(R.string.pipe_main_tray_check_inconformity));
        }
    }
    public String getTemplateNumber(){
        String number = "00";
        for(int i=0;i<trayModel.holes.size();i++) {
            SampleHoleModel shm = trayModel.holes.get(i);
            String text = shm.infotemplate.get();
            if(text!=null&&!text.isEmpty()&&!text.isBlank()){
                number = text;
                break;
            }
        }
        return number;
    }
    public String getEnddate(){
        String number = "000000";
        for(int i=0;i<trayModel.holes.size();i++) {
            SampleHoleModel shm = trayModel.holes.get(i);
            String text = shm.enddate.get();
            if(text!=null&&!text.isEmpty()&&!text.isBlank()){
                number = text;
                break;
            }
        }
        return number;
    }
    //提示信息不能为空
    public boolean checkSampleHoleNotEmpty(){
        boolean result = false;
        for (int i = 0; i < trayModel.holes.size();i++){
            SampleHoleModel model = trayModel.holes.get(i);
            String code = model.reagentCode.get();
            String num = model.serialNum.get();
            if(code!=null&&!code.isEmpty()&&!code.isBlank()&&num!=null&&!num.isEmpty()&&!num.isBlank()){
                return true;
            }else{
                result = false;
            }
        }
        if(!result){
            showLongToast(getString(R.string.alert_input_samplehole));
        }
        return result;
    }
    public boolean checkProject(){
        boolean result = false;
        if(mtemplateModel!=null){
            return true;
        }else{
            showLongToast(getString(R.string.alert_select_template));
        }
        return result;
    }
    public boolean checkPatient(){
        boolean result = false;
        if(trayModel.templatenumber.get()==null){
            result = false;
        }else if(trayModel.enddate.get()==null){
            result = false;
        }else{
            result = true;
        }
        if(!result) {
            showLongToast(getString(R.string.alert_select_template));
        }
        return result;
    }
    private void close(){
//        mainViewModel.onNavClicked(ConstantData.NAV_TRAYMANAGE);
        mainViewModel.onNavClicked(ConstantData.NAV_MAIN);
    }
//    private void chengeUrgent(boolean value){
//        if(value){
//            trayModel.urgentName.set(getString(R.string.pipe_main_urgent));
//        }else{
//            trayModel.urgentName.set(getString(R.string.pipe_main_unurgent));
//        }
//    }
    private void showFragment(int value){
        switch (value){
            case 0:
                getChildFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .hide(projectInfoFragment)
                        .show(projectListFragment)
                        .commit();
                binding.saveBtn.setVisibility(View.INVISIBLE);
                binding.selectProjBtn.setVisibility(View.INVISIBLE);
                binding.confirmProjBtn.setVisibility(View.VISIBLE);
                viewModel.selecttitle.set(getString(R.string.pipe_main_proj_choose));
                break;
            case 1:
                getChildFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .hide(projectListFragment)
                        .show(projectInfoFragment)
                        .commit();
                binding.saveBtn.setVisibility(View.VISIBLE);
                binding.selectProjBtn.setVisibility(View.VISIBLE);
                binding.confirmProjBtn.setVisibility(View.INVISIBLE);
                viewModel.selecttitle.set(getString(R.string.pipe_main_proj_confirm));
                break;
        }
    }

    private void initViewModel(){
        viewModel.getScaninfo().observe(getViewLifecycleOwner(), s -> {
            if(!s.isEmpty() && !s.isBlank()) {
                parseScanInfo(s);
            }
        });
        viewModel.getSelectedHole().observe(getViewLifecycleOwner(), sampleHoleModel -> {
            viewModel.selectholeindex.set(sampleHoleModel.index.get());
            // 由 Java 端手动 setText,避免 DataBinding 在每次点击时
            // 介入 EditText 文本刷新导致主线程阻塞 (ANR)
            if (sampleHoleModel != null) {
                if (binding.regentEt != null) {
                    binding.regentEt.setText(
                            sampleHoleModel.reagentCode.get() == null ? "" : sampleHoleModel.reagentCode.get());
                }
                if (binding.idcardEt != null) {
                    binding.idcardEt.setText(
                            sampleHoleModel.serialNum.get() == null ? "" : sampleHoleModel.serialNum.get());
                }
            }
        });
//        viewModel.getNavEvent().observe(getViewLifecycleOwner(), s -> {
//            if(s == ConstantData.NAV_TRAY_PROJECT_List){
////                viewModel.radioGroupOption.set(0);
//                showFragment(0);
//            }else if(s == ConstantData.NAV_TRAY_PROJECT_Info){
////                viewModel.radioGroupOption.set(1);
//                showFragment(1);
//            }
//        });
        viewModel.getTemplateType().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer integer) {
                switch(integer){
                    case ConstantData.TYPE_FULL:
                        temList = APLManager.getInstance().tempListfull;
                        break;
                    case ConstantData.TYPE_RNA:
                        temList = APLManager.getInstance().tempListrna;
                        break;
                    case ConstantData.TYPE_PCR:
                        temList = APLManager.getInstance().tempListpcr;
                        break;
                }
            }
        });
        viewModel.getTemplateModel().observe(getViewLifecycleOwner(), new Observer<TemplateModel>() {
            @Override
            public void onChanged(TemplateModel templateModel) {
                mtemplateModel = templateModel;
            }
        });
    }
    private TemplateModel mtemplateModel;
    private ArrayList<String> temList = new ArrayList<String>();
    private void parseScanInfo(String value){
        try {
            Log.i(TAG,value);
            if(value.length()==9){
                SampleHoleModel hole = viewModel.getSelectedHole().getValue();
                if(hole==null){
                    hole = new SampleHoleModel(1);
                }
                hole.serialNum.set(value);
                saveHole(hole);
                Log.i(TAG,"serialNum:"+value);
            }else if(value.length()==16){
                SampleHoleModel hole = viewModel.getSelectedHole().getValue();
                if(hole==null){
                    hole = new SampleHoleModel(1);
                }
                hole.reagentCode.set(value);
                saveHole(hole);
                Log.i(TAG,"reagentCode:"+value);
            }else{
                Toast.makeText(getContext(), getActivity().getResources().getString(R.string.result_ex_toast_query_no), Toast.LENGTH_LONG).show();
            }

        }catch (Exception e){
            Log.e(TAG,e.getMessage());
            showLongToast(getActivity().getResources().getString(R.string.template_dialog_message_expired_reagent));
        }
    }
    private void saveHole(SampleHoleModel hole){
        hole  = ScanCodeHelper.parseSampleHoleModel(hole);
        viewModel.setSelectedHole(hole);
        setwellinfo(hole.index.get(),hole.serialNum.get(),hole.reagentCode.get());
        for(int i=0;i<trayModel.holes.size();i++){
           SampleHoleModel gole = trayModel.holes.get(i);
            if(hole.index.get()==gole.index.get())
            {
                if(hole.reagentCode.get()!=null){
                    hole.status.set(1);
                }else if(hole.serialNum.get()!=null){
                    hole.status.set(1);
                }
                trayModel.holes.set(i,hole);
                break;
            }
        }
        trayModel.templatenumber.set(getTemplateNumber());
        trayModel.enddate.set(getEnddate());
        mainViewModel.setTrayChanged(trayindex);
    }
    private void showLongToast(String text){
        Toast.makeText(getContext(),text, Toast.LENGTH_LONG).show();
    }
    private CustomDialog mNoticeDialog;
    private EditText mDescriptionEt;
    private String description = "";
    public void showNoticeDialog(int dialogType,String title, String message,String reagentCode) {
        if (null != mNoticeDialog) {
            mNoticeDialog.dismiss();
            mNoticeDialog = null;
        }
        if (null == mNoticeDialog) {
            mNoticeDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater) this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_template_info_layout, null);
            TextView mHintTxt = (TextView) view.findViewById(R.id.common_dialog_hint_txt);
            mDescriptionEt = (EditText)view.findViewById(R.id.slan_info_discribe_et);
            mDescriptionEt.addTextChangedListener(new LimitInputTextWatcher(mDescriptionEt));
            mHintTxt.setText(message);
            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setTitle(title);
            if(dialogType == 0){
                mDescriptionEt.setVisibility(View.VISIBLE);
            }else if(dialogType == 1){
                mDescriptionEt.setVisibility(View.GONE);
            }else if(dialogType == 2){
                builder.setTitleColor(R.color.red);
                mDescriptionEt.setVisibility(View.GONE);
            }

            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    switch (dialogType){
                        case 0:
                            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.REAGENT_CODE_USER,reagentCode);
                            description = mDescriptionEt.getText().toString();
//                            ProjectInfo projectInfo = new ProjectInfo();
//                            projectInfo.setDescription(description);
//                            projectInfo.setReagentCode(reagentCode);
//                            MessageEvent messageEvent = new MessageEvent();
//                            messageEvent.setMsgType(MsgType.MSG_TEMPLATE_CAPACITY_DONE);
//                            messageEvent.setData(projectInfo);
//                            EventBus.getDefault().post(messageEvent);
                            break;
                    }
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mNoticeDialog.dismiss();
                    mNoticeDialog = null;
                }
            });

            mNoticeDialog = builder.create();
            mNoticeDialog.setCanceledOnTouchOutside(true);
            mNoticeDialog.show();
        }
    }
    // 8 对孔位(B1~B8、A1~A8)对应的 TextView id 数组
    // XML 中从左到右依次为 pair_1、pair_2、...、pair_8,
    // 物理孔位: pair_N -> B(9-N) + A(9-N)
    // 例如: pair_1 = B8+A8, pair_8 = B1+A1
    private final int[] HOLE_B_IDS = new int[]{
            R.id.tv_hole_b8, R.id.tv_hole_b7, R.id.tv_hole_b6, R.id.tv_hole_b5,
            R.id.tv_hole_b4, R.id.tv_hole_b3, R.id.tv_hole_b2, R.id.tv_hole_b1
    };
    private final int[] HOLE_A_IDS = new int[]{
            R.id.tv_hole_a8, R.id.tv_hole_a7, R.id.tv_hole_a6, R.id.tv_hole_a5,
            R.id.tv_hole_a4, R.id.tv_hole_a3, R.id.tv_hole_a2, R.id.tv_hole_a1
    };
    // 8 个 pair 容器 id: 下标 N-1 对应"样本 N"
    private final int[] PAIR_IDS = new int[]{
            R.id.pair_1, R.id.pair_2, R.id.pair_3, R.id.pair_4,
            R.id.pair_5, R.id.pair_6, R.id.pair_7, R.id.pair_8
    };
    // 当前选中的样本号 (1~8),0 表示未选中
    private int currentSelectedSampleNum = 0;

    /**
     * 根据 layout 中静态声明的 8 对孔位 TextView,
     * 按 name(A1~A8、B1~B8)找到对应 SampleHoleModel,
     * 有数据则实心(选中态),无数据则虚线背景
     */
    private void initSampleHoleListCycleView(){
        if(trayModel == null || getContext() == null){
            Log.w(TAG, "initSampleHoleListCycleView skipped: trayModel=" + trayModel + ", ctx=" + getContext());
            return;
        }
        // 防御:若 holes 数据缺失或不足 16 个,重新 init
        if (trayModel.holes == null || trayModel.holes.size() < 16) {
            Log.w(TAG, "holes 数据异常 (size="
                    + (trayModel.holes == null ? "null" : trayModel.holes.size())
                    + "),重新调用 initModel 重建");
            trayModel.initModel(trayindex, getString(R.string.pipe_main_trayname) + trayindex);
        }

        // 建立 name -> SampleHoleModel 映射,避免依赖 holes 列表顺序
        Map<String, SampleHoleModel> holeByName = new HashMap<>();
        if (trayModel.holes != null) {
            for (SampleHoleModel hole : trayModel.holes) {
                if (hole == null) continue;
                String name = hole.name.get();
                if (name != null && !name.isEmpty()) {
                    holeByName.put(name, hole);
                }
            }
        }

        // 按 1..8 顺序遍历,与 XML 中 pair_1 -> pair_8 顺序一致
        // n 是"样本编号",物理孔位号 = 9 - n
        for (int n = 1; n <= 8; n++) {
            int physical = 9 - n;
            bindHoleTextView(HOLE_B_IDS[n - 1], "B" + physical, holeByName.get("B" + physical));
            bindHoleTextView(HOLE_A_IDS[n - 1], "A" + physical, holeByName.get("A" + physical));
            bindPairClick(PAIR_IDS[n - 1], n);
        }
        // 进入时同步初始化 8 个 pair 容器的背景(默认都未选中)
        currentSelectedSampleNum = 0;
        refreshPairFramesBackground();
    }

    /**
     * 把单个孔位 TextView 绑定到 SampleHoleModel:
     * - 有数据:实心椭圆(选中态)
     * - 无数据:虚线椭圆(空)
     * - 绑定点击事件,写入 viewModel.selectedHole
     */
    /**
     * 给 pair_N 容器绑定点击事件:点击该样本框任意空白处
     * (B/A 上的 onClickListener 由 bindHoleTextView 处理,子 View 优先消费)
     * 触发 holeChecked(sampleNum):
     * - 该样本物理孔位的 serialNum / reagentCode 通过 @={} 双向绑定
     *   自动回显到顶部 regent_et / idcard_et
     * - 后续 inputBtn / reagentCodeClearImg / serialNumClearImg 都会
     *   把编辑结果回写到 holes 中对应 SampleHoleModel
     */
    private void bindPairClick(int pairId, int sampleNum){
        View pair = binding.sampleholeContainer.findViewById(pairId);
        if (pair == null) return;
        pair.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                holeChecked(sampleNum);
                SampleHoleModel[] pair = getPairHoles(sampleNum);
                SampleHoleModel aHole = pair[0];
                SampleHoleModel bHole = pair[1];
                binding.selectPairIndex.setText("样本 "+sampleNum);
                if(bHole != null || aHole!= null){
                    if(aHole != null){
                        binding.idcardEt.setText(aHole.serialNum.get());
                        binding.regentEt.setText(aHole.reagentCode.get());
                    }
                    if(bHole != null){
                        binding.idcardEt.setText(bHole.serialNum.get());
                        binding.regentEt.setText(bHole.reagentCode.get());
                    }
                }

            }
        });
     //   pair.setOnClickListener(v -> holeChecked(sampleNum));
    }

    private void bindHoleTextView(int tvId, String holeName, SampleHoleModel hole){
        TextView tv = binding.sampleholeContainer.findViewById(tvId);
        if (tv == null) return;
        if (hole != null) {
            tv.setText(holeName);
            tv.setBackground(getHoleBgDrawable(hole));
            final int physicalNum = parsePairNumber(hole);  // 1~8
            final int sampleNum = 9 - physicalNum;          // 1~8
            tv.setOnClickListener(v -> {
                if (sampleNum > 0) {
                    holeChecked(sampleNum);
                }
            });
        } else {
            tv.setText("");
            tv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.shape_oval_blue_stroke_bg));
            tv.setOnClickListener(null);
        }
    }

    /**
     * 从 SampleHoleModel.name (A1~A8, B1~B8) 中解析出样本序号 1~8
     */
    private int parsePairNumber(SampleHoleModel hole){
        if (hole == null) return 0;
        String name = hole.name.get();
        if (name == null || name.length() < 2) return 0;
        try {
            return Integer.parseInt(name.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 根据样本序号 1~8 返回 (A 行, B 行) SampleHoleModel,任一为空时该位置为 null
     */
    private SampleHoleModel[] getPairHoles(int pairNum){
        SampleHoleModel aHole = null;
        SampleHoleModel bHole = null;
        if (trayModel == null || trayModel.holes == null) return new SampleHoleModel[]{aHole, bHole};
        for (SampleHoleModel h : trayModel.holes) {
            if (h == null) continue;
            String name = h.name.get();
            if (name == null || name.length() < 2) continue;
            if (name.charAt(0) == 'A' || name.charAt(0) == 'a') {
                if (parsePairNumber(h) == pairNum) aHole = h;
            } else if (name.charAt(0) == 'B' || name.charAt(0) == 'b') {
                if (parsePairNumber(h) == pairNum) bHole = h;
            }
        }
        return new SampleHoleModel[]{aHole, bHole};
    }

    /**
     * 根据 SampleHoleModel.status 返回对应背景:
     * status==0 -> 虚线椭圆,否则 -> 实心椭圆
     */
    private Drawable getHoleBgDrawable(SampleHoleModel hole){
        boolean isEmpty = hole.status.get() != null && hole.status.get() == 0;
        return ContextCompat.getDrawable(getContext(),
                isEmpty ? R.drawable.shape_oval_blue_stroke_bg : R.drawable.shape_oval_blue_bg);
    }

    /**
     * 选中 pairNum 对应的样本(A 行 + B 行):
     * - 该样本的 A、B 两个孔位均 selected=true
     * - 其他 14 个孔位 selected=false
     * - 把 B 行(优先)写入 viewModel.selectedHole,顶部编辑框 @={viewModel.selectedHole.*} 自动回显
     */
    private void holeChecked(int sampleNum){
        if (trayModel == null || trayModel.holes == null) return;
        // 同一 pair 重复点击 -> 跳过刷新,避免主线程无意义阻塞
        if (sampleNum == currentSelectedSampleNum) {
            return;
        }
        currentSelectedSampleNum = sampleNum;
        int physicalNum = 9 - sampleNum;
        SampleHoleModel[] pair = getPairHoles(physicalNum);
        SampleHoleModel aHole = pair[0];
        SampleHoleModel bHole = pair[1];
        // 选中态:sampleNum 对应 A/B 都 true,其他都 false
        for (SampleHoleModel h : trayModel.holes) {
            if (h == null) continue;
            int n = parsePairNumber(h);  // 物理编号
            h.selected.set(n == physicalNum);
        }
        // 把 B 行(或 A 行)作为 selectedHole,编辑框自动绑定显示
        SampleHoleModel primary = bHole != null ? bHole : aHole;
        if (primary != null) {
            viewModel.setSelectedHole(primary);
        }
        // 背景刷新放到下一个消息循环,避免与 setSelectedHole 同步触发造成主线程峰值
        View container = binding.sampleholeContainer;
        if (container != null) {
            container.post(() -> {
                refreshHoleCellsBackground();
                refreshPairFramesBackground();
            });
        }
    }

    /**
     * 根据当前 SampleHoleModel.status 重新刷新 16 个孔位 TextView 的背景
     */
    private void refreshHoleCellsBackground(){
        if (trayModel == null || trayModel.holes == null || getContext() == null) return;
        Map<String, SampleHoleModel> holeByName = new HashMap<>();
        for (SampleHoleModel hole : trayModel.holes) {
            if (hole == null) continue;
            String name = hole.name.get();
            if (name != null && !name.isEmpty()) {
                holeByName.put(name, hole);
            }
        }
        for (int n = 1; n <= 8; n++) {
            int physical = 9 - n;
            applyBgToHoleTv(HOLE_B_IDS[n - 1], holeByName.get("B" + physical));
            applyBgToHoleTv(HOLE_A_IDS[n - 1], holeByName.get("A" + physical));
        }
    }

    private void applyBgToHoleTv(int tvId, SampleHoleModel hole){
        TextView tv = binding.sampleholeContainer.findViewById(tvId);
        if (tv == null) return;
        if (hole != null) {
            tv.setBackground(getHoleBgDrawable(hole));
        } else {
            tv.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.shape_oval_blue_stroke_bg));
        }
    }

    /**
     * 刷新 8 个 pair 容器的背景:
     * - 当前选中的样本对应 pair 用 shape_hole_pair_frame_selected(高亮)
     * - 其他 7 个用 shape_hole_pair_frame(普通描边)
     */
    private void refreshPairFramesBackground(){
        if (getContext() == null) return;
        Drawable bgDefault = ContextCompat.getDrawable(getContext(), R.drawable.shape_hole_pair_frame);
        Drawable bgSelected = ContextCompat.getDrawable(getContext(), R.drawable.shape_hole_pair_frame_selected);
        for (int sampleNum = 1; sampleNum <= 8; sampleNum++) {
            View pair = binding.sampleholeContainer.findViewById(PAIR_IDS[sampleNum - 1]);
            if (pair == null) continue;
            pair.setBackground(sampleNum == currentSelectedSampleNum ? bgSelected : bgDefault);
        }
    }

    /**
     * 判断指定 id 的孔位 TextView 是否可点击
     * (在 bindHoleTextView 中,无数据的孔位 setOnClickListener(null))
     */
    private boolean isHoleTvClickable(int tvId){
        TextView tv = binding.sampleholeContainer.findViewById(tvId);
        return tv != null && tv.hasOnClickListeners();
    }
    //设置孔位信息
    private void setwellinfo(int hole,String sample,String reagentCode){
        WellInfo holewellInfo = new WellInfo("A"+hole,sample,reagentCode);
        for(int i=0;i<trayModel.wellInfolist.size();i++){
            if(holewellInfo.getWellName().equalsIgnoreCase(trayModel.wellInfolist.get(i).getWellName())){
                trayModel.wellInfolist.set(i,holewellInfo);
            }
        }
    }

    /**
     * 同步 wellInfolist 中物理编号 physicalNum 对应 A 行 + B 行的 WellInfo。
     * 实验结果页是读 wellInfolist,所以清空 SampleHoleModel 时必须同步,
     * 否则结果中仍能看到旧数据(典型场景:清空 B7/A7,B7/A7 的结果还在)。
     *
     * @param physicalNum    物理孔位号 1~8
     * @param clearReagent   是否清空 reagentCode
     * @param clearSample    是否清空 sampleName (serialNum)
     */
    private void syncWellInfo(int physicalNum, boolean clearReagent, boolean clearSample){
        if (trayModel == null || trayModel.wellInfolist == null) return;
        String aKey = "A" + physicalNum;
        String bKey = "B" + physicalNum;
        for (int i = 0; i < trayModel.wellInfolist.size(); i++) {
            WellInfo wi = trayModel.wellInfolist.get(i);
            if (wi == null) continue;
            String name = wi.getWellName();
            if (name == null) continue;
            if (!name.equalsIgnoreCase(aKey) && !name.equalsIgnoreCase(bKey)) continue;
            String newReagent = clearReagent ? "" : wi.getReagentCode();
            String newSample  = clearSample  ? "" : wi.getSampleName();
            trayModel.wellInfolist.set(i, new WellInfo(name, newSample, newReagent));
        }
    }

    /**
     * 清空 wellInfolist 中所有 16 个 WellInfo 的 sampleName 和 reagentCode。
     * 配合 clearallcode_btn 使用。
     */
    private void clearAllWellInfo(){
        if (trayModel == null || trayModel.wellInfolist == null) return;
        for (int i = 0; i < trayModel.wellInfolist.size(); i++) {
            WellInfo wi = trayModel.wellInfolist.get(i);
            if (wi == null) continue;
            trayModel.wellInfolist.set(i, new WellInfo(wi.getWellName(), "", ""));
        }
    }
    public void sendMessage(int index,Object value){
        if(mUIHandler!=null) {
            Message msg = mUIHandler.obtainMessage();
            msg.what = index;
            msg.obj = value;
            mUIHandler.sendMessage(msg);
        }
    }
    UIHandler mUIHandler = new UIHandler();
    public void trayOpenWorkDo(int trayindex){
        sendMessage(MSG_TRAY_OPEN_COOLER,trayindex);
        mUIHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // 休眠代码
                try {
                    sendMessage(MSG_TRAY_OPEN_LED,trayindex);
                } catch (Exception e) {
                    Log.e(TAG, "setLed" + e.getMessage());
                }
            }
        }, 300);
    }
    public static final int MSG_TRAY_OPEN_COOLER = 1;
    public static final int MSG_TRAY_OPEN_LED = 2;
    private class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TRAY_OPEN_COOLER:
                    if (msg.obj != null) {
                        int tray = (int) msg.obj;
                        ArmManager.getInstance().setCooler(tray,10);//制冷
                        Log.e("ArmTaskWorker", "MSG_TRAY_OPEN_COOLER:" + tray);
                    }
                    break;
                case MSG_TRAY_OPEN_LED:
                    if (msg.obj != null) {
                        int tray = (int) msg.obj;
                        ArmManager.getInstance().setLed(tray,1);//开led
                        Log.e("ArmTaskWorker", "MSG_TRAY_OPEN_LED:" + tray);
                    }
                    break;
            }
        }
    }
    //    public void refreshView(){
//        initTrayStatus();
//        sampleHoleListAdapter.notifyDataSetChanged();
//    }
//
//    SampleHoleListAdapter sampleHoleListAdapter;
//    private void initSampleHoleListCycleView(){
//        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
//        //1.设置LinearLayoutManager ListView
//        binding.sampleholeRv.setLayoutManager(layoutManager);
//        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
//        //2.设置固定大小
//        binding.sampleholeRv.setHasFixedSize(true);
//        //3.设置Adapter
//        sampleHoleListAdapter = new SampleHoleListAdapter(getContext(), trayModel.getHoles());
//        binding.sampleholeRv.setAdapter(sampleHoleListAdapter);
//    }
}