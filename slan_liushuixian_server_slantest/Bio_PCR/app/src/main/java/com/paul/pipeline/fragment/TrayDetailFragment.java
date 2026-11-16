package com.paul.pipeline.fragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.databinding.DataBindingUtil;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.paul.pipeline.adapter.DetailSampleHoleListAdapter;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.data.ScanCodeHelper;
import com.paul.pipeline.listener.OnItemChildClickListener;
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
                SampleHoleModel hole = viewModel.getSelectedHole().getValue();
                if(hole==null){
                    showLongToast("请选择目标孔位！");
                }else {
                    String snumber = binding.idcardEt.getText().toString();
                    if(snumber!=null&&!snumber.isBlank()&&!snumber.isEmpty()) {
                        hole.serialNum.set(snumber);
                    }else{
                        showLongToast("样本编号 不能为空！");
                        return;
                    }
                    String code = binding.regentEt.getText().toString();
                    if(code!=null&&!code.isBlank()&&!code.isEmpty()) {
                        hole.reagentCode.set(code);
                    }else{
                        showLongToast("试剂条码 不能为空！");
                        return;
                    }
                    hole.status.set(1);
                    saveHole(hole);
                }
            }
        });
        binding.clearallcodeBtn.setOnClickListener(v -> {
            for (int i = 0; i < trayModel.holes.size();i++){
                trayModel.holes.get(i).reagentCode.set("");
                trayModel.holes.get(i).serialNum.set("");
                trayModel.holes.get(i).status.set(0);
                // model.scaninfo.set(getString(R.string.pipe_main_tray_check_inconformity));
            }
        });
//        binding.saveallcodeBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mainViewModel.setTrayChanged(trayindex);
//            }
//        });
        binding.reagentCodeClearImg.setOnClickListener(v->{
            viewModel.getSelectedHole().getValue().reagentCode.set("");
        });
        binding.serialNumClearImg.setOnClickListener(v->{
            viewModel.getSelectedHole().getValue().serialNum.set("");
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
    DetailSampleHoleListAdapter sampleHoleListAdapter;
    private void initSampleHoleListCycleView(){
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(),2);
        //1.设置LinearLayoutManager ListView
        binding.sampleholeRv.setLayoutManager(layoutManager);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        //2.设置固定大小
        binding.sampleholeRv.setHasFixedSize(true);
        //3.设置Adapter
        sampleHoleListAdapter = new DetailSampleHoleListAdapter(getContext(), trayModel.holes, new OnItemChildClickListener() {
            @Override
            public void onItemChildClick(View view, int position) {
                holeChecked(position);
            }
        });
        binding.sampleholeRv.setAdapter(sampleHoleListAdapter);
    }
    private void holeChecked(int position){
        for (int i = 0; i < trayModel.holes.size();i++){
            SampleHoleModel model= trayModel.holes.get(i);
            if(model.index.get()==position){
                model.selected.set(true);
                viewModel.setSelectedHole(model);
            }else{
                model.selected.set(false);
            }
        }
        sampleHoleListAdapter.notifyDataSetChanged();
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