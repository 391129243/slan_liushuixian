package com.gidi.bio_pcr.config.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.CTRuleEntity;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKRuleEntity;
import com.gidi.bio_pcr.WintersUtils.universalUtils.TimerCaculate;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.ConfigManager;
import com.gidi.bio_pcr.config.ConfigSLANActivity;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.config.adapter.ConfigSLANAdapter;
import com.gidi.bio_pcr.config.adapter.FileNameAdapter;
import com.gidi.bio_pcr.config.adapter.JudgeCtAdapter;
import com.gidi.bio_pcr.config.adapter.JudgePeakAdapter;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomImageText;
import com.gidi.bio_pcr.view.DividerDecoration;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.gidi.bio_pcr.view.LinearSpacingItemDecoration;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.hongshi.slansdk.model.experiment.Program;
import com.hongshi.slansdk.model.experiment.Project;
import com.hongshi.slansdk.model.experiment.Section;
import com.hongshi.slansdk.model.experiment.SectionAMP;
import com.hongshi.slansdk.model.experiment.SectionMelt;
import com.hongshi.slansdk.model.experiment.Step;
import com.hongshi.slansdk.service.SLANService;
import com.hongshi.slansdk.slanenum.TemperatureControlMode;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateSinglePCRActivity extends BaseActivity implements View.OnClickListener{
    private RelativeLayout mTitleLayout;
    private EditText mPCRTitleEt,mTotalTimeEt,mSecondNameEt,mThirdNameEt;
    private CustomImageText mPCRTabTv,mCtTabTv,mPeakTabTv;
    private RelativeLayout mPCRFileLayout,mJudgeFileLayout;
    private TextView mPcrFileNameTv,mSavePcrTempTv,mCtFileNameTv,mPeakFileNameTv;
    private TextView mCreatePcrTv,mCreateCtJudgeTv,mCreatePeakJudgeTv;

    private RecyclerView mPcrFileRv,mJudgeFileRv;
    private List<String> mPCRFileList,mPeakFileList,mCtFileList;//上方显示的文件列表
    private FileNameAdapter pcrFileAdapter,judgeFileAdapter;

    ////////判别文件的详情
    private ListView mCtJudgeDetailLv;//判别文件的详情列表
    private JudgeCtAdapter judgeCtAdapter;//单重判别文件lv
    private List<CTRuleEntity> ctRuleEntities;
    private List<PEAKRuleEntity> peakRuleEntities;
    private RecyclerView mPeakJudgeDetailRv;//判别文件的详情列表
    private JudgePeakAdapter judgePeakAdapter;//多重判别文件lv
    private RelativeLayout mPeakJudgeLayout;
    private RelativeLayout mCtJudgeLayout;

    private RecyclerView programRv;//配置的所有的项目的列表
    private LinearLayout mProgramTitleLayout;
    private LinearLayout mProgramRecycleLayout;
    private ConfigSLANAdapter configSLANAdapter;
    private List<ConfigSLANSection>mSLANSections;
    private int click_pcr_pos = -1,click_ct_pos = -1,click_peak_pos = -1;
    private boolean isPeakCheck = false,isCtCheck = false,isPcrCheck = false;

    private String pcrFileName,pcrTemplateName,ctFileName,peakFileName;
    private boolean isEdit = false;
    private boolean isPeak = true;
    private NoticeDialog noticeDialog;
    private String CONFIG_JUDGE_PATH = FileUtils.CONFIG_PATH + File.separator + "judge" + File.separator;
    private String CONFIG_SINGLE_PCR_TEMPLATE_PATH = FileUtils.CONFIG_TEMPLATE_PATH + "singlePCR" +File.separator;
    private DecimalFormat nf = new DecimalFormat( "0.00");
    private int pcrTime = 0;
    //控温方式
    private RadioGroup mTempCtrlModeRg;
    private RadioButton mTempCtrlFastRb,mTempCtrlStandardRb;
    private int temperatureCtrlMode = TemperatureControlMode.Fast.getValue();

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_single_pcr;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTitleLayout = (RelativeLayout)findViewById(R.id.single_pcr_create_title_layout);
        TextView mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.create_single_pcr_title));
        ImageView mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mPCRTitleEt = (EditText) findViewById(R.id.single_pcr_create_template_name_et);
        mPCRTitleEt.addTextChangedListener(new LimitInputTextWatcher(mPCRTitleEt));
        mSecondNameEt = (EditText) findViewById(R.id.single_pcr_create_template_name_second_et);
        mThirdNameEt = (EditText) findViewById(R.id.single_pcr_create_template_name_third_et);
        mSecondNameEt.addTextChangedListener(new LimitInputTextWatcher(mSecondNameEt));
        mThirdNameEt.addTextChangedListener(new LimitInputTextWatcher(mThirdNameEt));
        mTotalTimeEt = (EditText)findViewById(R.id.single_pcr_create_totaltime_et);
        mTempCtrlModeRg = (RadioGroup)findViewById(R.id.single_pcr_tempctrl_mode_rg);
        mTempCtrlFastRb = (RadioButton) findViewById(R.id.single_pcr_tempctrl_fast);
        mTempCtrlStandardRb= (RadioButton)findViewById(R.id.single_pcr_tempctrl_standard);

        mPcrFileNameTv = (TextView) findViewById(R.id.single_pcr_create_pcr_name_tv);
        mCtFileNameTv = (TextView)findViewById(R.id.single_pcr_create_ct_name_tv);
        mPeakFileNameTv = (TextView)findViewById(R.id.single_pcr_create_peak_name_tv);
        mSavePcrTempTv = (TextView) findViewById(R.id.single_pcr_create_save_btn);
        mPCRFileLayout = (RelativeLayout)findViewById(R.id.single_pcr_create_pcr_layout);
        mJudgeFileLayout = (RelativeLayout)findViewById(R.id.single_pcr_create_judge_file_layout);

        mPcrFileRv = (RecyclerView) findViewById(R.id.single_pcr_rv);
        mJudgeFileRv = (RecyclerView)findViewById(R.id.single_pcr_create_judge_list);
        mCreatePcrTv  = (TextView) findViewById(R.id.single_pcr_create_tv);

        mPCRTabTv = (CustomImageText)findViewById(R.id.single_pcr_create_pcr_title_tv);
        mPeakTabTv = (CustomImageText)findViewById(R.id.single_pcr_create_peak_title_tv);
        mCtTabTv = (CustomImageText)findViewById(R.id.single_pcr_create_ct_title_tv);
        programRv = (RecyclerView) findViewById(R.id.single_pcr_create_slan_program_rv);
        mProgramTitleLayout = (LinearLayout) findViewById(R.id.single_pcr_create_program_title_layout);
        mProgramRecycleLayout = (LinearLayout) findViewById(R.id.single_pcr_create_program_recycle_layout);
        //判别文件详情
        mCreateCtJudgeTv = (TextView)findViewById(R.id.single_pcr_create_ct_tv) ;
        mCreatePeakJudgeTv = (TextView)findViewById(R.id.single_pcr_create_peak_tv) ;
        mCtJudgeLayout = (RelativeLayout)findViewById(R.id.config_single_pcr_judge_ct_layout);
        mCtJudgeDetailLv = (ListView)mCtJudgeLayout.findViewById(R.id.config_create_judge_ct_detail_lv) ;
        judgeCtAdapter = new JudgeCtAdapter(getApplicationContext());
        mCtJudgeDetailLv.setAdapter(judgeCtAdapter);

        mPeakJudgeLayout = (RelativeLayout)findViewById(R.id.config_single_pcr_judge_peak_layout);
        mPeakJudgeDetailRv = (RecyclerView)mPeakJudgeLayout.findViewById(R.id.config_judge_peak_list_detail_rv);
        judgePeakAdapter = new JudgePeakAdapter(getApplicationContext());
        mPeakJudgeDetailRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,
                false));
        mPeakJudgeDetailRv.setAdapter(judgePeakAdapter);
        initData();
    }

    @Override
    protected void initListener() {

        mSavePcrTempTv.setOnClickListener(this);
        mCreatePcrTv.setOnClickListener(this);
        mCreateCtJudgeTv.setOnClickListener(this);
        mCreatePeakJudgeTv.setOnClickListener(this);
        mTempCtrlModeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkId) {
                if(checkId == R.id.single_pcr_tempctrl_fast){
                    temperatureCtrlMode = TemperatureControlMode.Fast.getValue();
                }else if(checkId == R.id.single_pcr_tempctrl_standard){
                    temperatureCtrlMode = TemperatureControlMode.Standard.getValue();
                }
            }
        });
        mPCRTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPCRFileLayout.setVisibility(View.VISIBLE);
                mJudgeFileLayout.setVisibility(View.GONE);
                if(click_pcr_pos != -1 && mPCRFileList != null){
                    if(mPCRFileList.size()>0){
                        pcrFileAdapter.setSelectedPosition(click_pcr_pos);
                        openSLANExFile(pcrFileName);
                    }
                }
            }
        });

        mPeakTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.VISIBLE);
                mCtJudgeLayout.setVisibility(View.GONE);
                mPeakJudgeLayout.setVisibility(View.VISIBLE);
                judgeFileAdapter.setSelectedPosition(-1);
                isPeak = true;
                if(mPeakFileList != null){
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    if(isPeakCheck && click_peak_pos != -1){
                        judgeFileAdapter.setSelectedPosition(click_peak_pos);
                        openJudgeFile(peakFileName);
                    }
                }
            }
        });

        mCtTabTv.setOnImageButtonClickListener(new CustomImageText.OnImageButtonClickListener() {
            @Override
            public void CustomImageButtonClicked(View v) {
                mCtTabTv.setImageBtnResoure(R.drawable.shape_oval_orange_bg,R.color.orange);
                mPeakTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);
                mPCRTabTv.setImageBtnResoure(R.drawable.shape_oval_top_bg,R.color.blue);

                mPCRFileLayout.setVisibility(View.GONE);
                mJudgeFileLayout.setVisibility(View.VISIBLE);
                mCtJudgeLayout.setVisibility(View.VISIBLE);
                mPeakJudgeLayout.setVisibility(View.GONE);
                judgeFileAdapter.setSelectedPosition(-1);
                isPeak = false;
                // Log.i("AAA","isJudgeCheck--" + isJudgeCheck);
                if(mCtFileList != null){
                    judgeFileAdapter.setTemplateList(mCtFileList);
                    if(isCtCheck && click_ct_pos != -1){
                        judgeFileAdapter.setSelectedPosition(click_ct_pos);
                        openJudgeFile(ctFileName);
                    }
                }
            }
        });
    }

    @Override
    protected void initData() {
        ctRuleEntities = new ArrayList<CTRuleEntity>();
        peakRuleEntities = new ArrayList<PEAKRuleEntity>();
        mSLANSections = new ArrayList<ConfigSLANSection>();
        programRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        programRv.addItemDecoration(new DividerDecoration(this ,2));
        configSLANAdapter = new ConfigSLANAdapter(getApplicationContext());
        mSLANSections.clear();
        configSLANAdapter.setProgramSteps(mSLANSections);
        programRv.setAdapter(configSLANAdapter);
        initPcrFileRv();
        initJudgeFileRv();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isEdit = getIntent().getBooleanExtra("isEdit",false);
        pcrTemplateName = getIntent().getStringExtra("templateName");
        if(isEdit){
            //模板的文件夹路径
            if(!StringUtil.isEmpty(pcrTemplateName)){
                if(pcrTemplateName.contains("~")){
                    String[] names = pcrTemplateName.split("~");
                    if(names != null &&names.length>0){
                        switch (names.length){
                            case 1:
                                mPCRTitleEt.setText(names[0]);
                                break;
                            case 2:
                                mPCRTitleEt.setText(names[0]);
                                mSecondNameEt.setText(names[1]);
                                break;
                            case 3:
                                mPCRTitleEt.setText(names[0]);
                                mSecondNameEt.setText(names[1]);
                                mThirdNameEt.setText(names[2]);
                                break;
                        }

                    }
                }else{
                    mPCRTitleEt.setText(pcrTemplateName);
                }
            }
        }else{
            String subFirName = "pcr_" + DateFormatUtil.getFileName();
            mPCRTitleEt.setText(subFirName);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePCRFile();
        updateJudgeFile();
        if(isEdit){
            String localTempPath = CONFIG_SINGLE_PCR_TEMPLATE_PATH + pcrTemplateName +File.separator;
            List<File> editFiles = FileUtils.getFileList(localTempPath);
            if(null != editFiles &&editFiles.size()>0){
                for(File file :editFiles){
                    if(file.getName().endsWith(".prj")){
                        pcrFileName = file.getName().substring(0, file.getName().length() - 4);
                        mPcrFileNameTv.setText(pcrFileName + ".prj");
                        int pos = getClickedPosition(pcrFileName,mPCRFileList);
                        click_pcr_pos = pos;
                    }else if(file.getName().endsWith(".ct")){
                        ctFileName = file.getName();
                        isCtCheck = true;
                        mCtFileNameTv.setText(ctFileName);
                        int pos = getClickedPosition(ctFileName,mCtFileList);
                        click_ct_pos= pos;
                    }else if(file.getName().endsWith(".peak")){
                        peakFileName = file.getName();
                        isPeakCheck = true;
                        mPeakFileNameTv.setText(peakFileName);
                        int pos = getClickedPosition(peakFileName,mPeakFileList);
                        click_peak_pos = pos;
                    }else if(file.getName().equals("totaltime.txt")){
                        //全流程时间
                        TotalTime totalTime = ConfigManager.getTotalTimeFile(file);
                        mTotalTimeEt.setText(totalTime.getTotalTime()+"");
                        temperatureCtrlMode = totalTime.getTemperatureCtrlMode();
                        if(temperatureCtrlMode == TemperatureControlMode.Fast.getValue()){
                            //快速
                            mTempCtrlFastRb.setChecked(true);
                        }else if(temperatureCtrlMode == TemperatureControlMode.Standard.getValue()){
                            //标准
                            mTempCtrlStandardRb.setChecked(true);
                        }
                    }
                }
            }
            isEdit = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mSLANSections){
            mSLANSections.clear();
            mSLANSections = null;
        }
        if(null != mPCRFileList){
            mPCRFileList.clear();
            mPCRFileList = null;
        }
        if(null != mCtFileList){
            mCtFileList.clear();
            mCtFileList = null;
        }
        if(null != mPeakFileList){
            mPeakFileList.clear();
            mPeakFileList = null;
        }
        if(null != peakRuleEntities){
            peakRuleEntities.clear();
            peakRuleEntities = null;
        }
        if(null != ctRuleEntities){
            ctRuleEntities.clear();
            ctRuleEntities = null;
        }
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
    }

    private void initPcrFileRv(){
        //编辑的prj文件夹是config找到所有的文件
        mPCRFileList = new ArrayList<String>();
        LinearLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),5);
        mPcrFileRv.setLayoutManager(layoutManager);
        mPcrFileRv.addItemDecoration(new LinearSpacingItemDecoration(this, 6));
        pcrFileAdapter = new FileNameAdapter(getApplicationContext());
        pcrFileAdapter.setTemplateList(mPCRFileList);
        mPcrFileRv.setAdapter(pcrFileAdapter);
        pcrFileAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                pcrFileAdapter.setTemplateList(mPCRFileList);
                pcrFileAdapter.setSelectedPosition(position);
                click_pcr_pos = position;
                pcrFileName = mPCRFileList.get(position);
                //更新UI
                mPcrFileNameTv.setText(pcrFileName + ".prj");
                openSLANExFile(pcrFileName);
            }
        });
    }

    private void initJudgeFileRv(){
        mCtFileList = new ArrayList<String>();
        mPeakFileList = new ArrayList<String>();
        //编辑的prj文件夹是config找到所有的文件
        LinearLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),5);
        mJudgeFileRv.setLayoutManager(layoutManager);
        mJudgeFileRv.addItemDecoration(new LinearSpacingItemDecoration(this, 6));
        judgeFileAdapter = new FileNameAdapter(getApplicationContext());
        mJudgeFileRv.setAdapter(judgeFileAdapter);
        judgeFileAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if(!isPeak){
                    judgeFileAdapter.setTemplateList(mCtFileList);
                    judgeFileAdapter.setSelectedPosition(position);
                    click_ct_pos = position;
                    ctFileName = mCtFileList.get(position);
                    //更新UI
                    isCtCheck = true;
                    mCtFileNameTv.setText(ctFileName);
                    openJudgeFile(ctFileName);
                }else{
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    judgeFileAdapter.setSelectedPosition(position);
                    click_peak_pos = position;
                    peakFileName = mPeakFileList.get(position);
                    isPeakCheck = true;
                    mPeakFileNameTv.setText(peakFileName);
                    openJudgeFile(peakFileName);
                }
            }
        });

        judgeFileAdapter.setOnItemLongClickListener(new FileNameAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position) {
                if(!isPeak){
                    judgeFileAdapter.setTemplateList(mCtFileList);
                    judgeFileAdapter.setSelectedPosition(-1);
                    click_ct_pos = -1;
                    ctFileName ="";
                    //更新UI
                    isCtCheck = false;
                    mCtFileNameTv.setText(ctFileName);
                    mCtJudgeLayout.setVisibility(View.VISIBLE);
                    mPeakJudgeLayout.setVisibility(View.GONE);
                    if(ctRuleEntities != null){
                        ctRuleEntities.clear();
                        judgeCtAdapter.setJudgeCtList(ctRuleEntities);
                    }
                }else{
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    judgeFileAdapter.setSelectedPosition(-1);
                    click_peak_pos = -1;
                    peakFileName = "";
                    isPeakCheck = false;
                    mPeakFileNameTv.setText(peakFileName);
                    mPeakJudgeLayout.setVisibility(View.VISIBLE);
                    mCtJudgeLayout.setVisibility(View.GONE);
                    if(null != peakRuleEntities){
                        peakRuleEntities.clear();
                        judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                    }
                }
            }
        });
    }


    private void updatePCRFile(){
        if(null != mPCRFileList){
            mPCRFileList.clear();
            File[] files = new File(FileUtils.CONFIG_SLAN_PATH).listFiles();//文件夹下的所有文件
            if(files != null && files.length>0) {
                for (int i = 0; i < files.length; i++) {
                    if (null != mPCRFileList) {
                        if (files[i].getName() != null && files[i].getName().contains(".prj")) {
                            String name = files[i].getName().substring(0, files[i].getName().length() - 4);
                            mPCRFileList.add(name);
                            Collections.sort(mPCRFileList);
                        }
                    }
                }
            }
            pcrFileAdapter.setTemplateList(mPCRFileList);
        }
    }

    private  void openSLANExFile(String fileName){
        //打开文件夹
        if(!StringUtil.isEmpty(fileName)&& null != SLANService.INSTANCE){
            ArrayList<Project> mConfigProjects = SLANService.INSTANCE.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
            if(null != mConfigProjects && mConfigProjects.size()>0){
                for(Project project :mConfigProjects){
                    if(null != project){
                        if(project.getName().equals(fileName)){
                            Program program = project.getProgram();
                            //转化为String
                           // this.project = project;
                            pcrTime = TimerCaculate.PCR_Caculate(project);
                            ArrayList<Section> sections = program.getSections();
                            if(null != sections){
                                getSLANSections(sections);
                            }
                        }
                    }
                }

                updateProgramUI();
                mTotalTimeEt.setText(pcrTime +"");
            }
        }
    }

    private void getSLANSections(ArrayList<Section> sections){
        if(sections.size()>0){
            mSLANSections.clear();
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
                                    slanStep.setAcquisition(step.getIsAcquisition());
                                    slanStep.setSectionIndex(sectionAMP.getIdx());
                                    slanItems.add(slanStep);
                                }
                            }
                        }
                        slanSection.setItems(slanItems);
                        mSLANSections.add(slanSection);
                    }else if(section instanceof SectionMelt){
                        SectionMelt sectionMelt = (SectionMelt) section;
                        ConfigSLANSection slanSection = new ConfigSLANSection();
                        slanSection.setName(sectionMelt.getName());
                        slanSection.setIdx(sectionMelt.getIdx());
                        slanSection.setProgramType(Constants.PROGRAM_TYPE_MELT);//熔解段
                        slanSection.setRate(sectionMelt.getRate());
                        slanSection.setScanMode(sectionMelt.getScanMode());
                        slanSection.setMeltHoldtime(sectionMelt.getHoldTime());
                        slanSection.setInterval(sectionMelt.getInterval());

                        ArrayList<ConfigSLANSection.Step> slanItems = new ArrayList<>();
                        ConfigSLANSection.Step meltStep = new ConfigSLANSection.Step();
                        meltStep.setStepName("");
                        meltStep.setLeftTemperature(sectionMelt.getStartTemp());
                        meltStep.setRightTemperature(sectionMelt.getEndTemp());
                        meltStep.setSectionIndex(sectionMelt.getIdx());
                        meltStep.setStep_index(0);
                        meltStep.setAcquisition(true);
                        meltStep.setTemperature(sectionMelt.getEndTemp());
                        meltStep.setHoldTime(sectionMelt.getHoldTime());
                        meltStep.setStepType(Constants.PROGRAM_TYPE_MELT);
                        slanItems.add(meltStep);
                        slanSection.setItems(slanItems);
                        mSLANSections.add(slanSection);
                    }
                }

            }
        }else{
            mSLANSections.clear();
        }
    }

    private void updateProgramUI(){
        if(null != mSLANSections){
            if(mSLANSections.size() == 0){
                if(null != mProgramTitleLayout){
                    mProgramTitleLayout.removeAllViews();
                }
                configSLANAdapter.setProgramSteps(mSLANSections);
                if(null != mProgramRecycleLayout){
                    mProgramRecycleLayout.removeAllViews();
                }
            }else if(mSLANSections.size()>0){
                if(null != mProgramTitleLayout){
                    mProgramTitleLayout.removeAllViews();
                }
                for(int i = 0; i<mSLANSections.size();i++){
                    TextView textView = new TextView(getApplicationContext());
                    textView.setBackgroundResource(R.color.blue);
                    textView.setTextSize(20);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(0xffffffff);

                    int programType = mSLANSections.get(i).getProgramType();
                    if(programType == Constants.PROGRAM_TYPE_AMP){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(162*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(0, 0, 2, 0);
                        textView.setLayoutParams(layoutParams);
                        int index = i+1;
                        textView.setText(getResources().getString(R.string.runing_pcr_section)+ index);
                    }else if(programType == Constants.PROGRAM_TYPE_MELT){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(0, 0, 2, 0);
                        textView.setLayoutParams(layoutParams);
                        textView.setText(getResources().getString(R.string.runing_pcr_section_melt));
                    }
                    mProgramTitleLayout.addView(textView);
                }

                configSLANAdapter.setProgramSteps(mSLANSections);

                if(null != mProgramRecycleLayout){
                    mProgramRecycleLayout.removeAllViews();
                }
                for(int i = 0; i<mSLANSections.size();i++){
                    TextView textView = new TextView(getApplicationContext());
                    textView.setBackgroundResource(R.color.color_recycle);
                    textView.setTextSize(18);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(0xffffffff);

                    int programType = mSLANSections.get(i).getProgramType();
                    if(programType == Constants.PROGRAM_TYPE_AMP){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(162*mSLANSections.get(i).getItems().size(),LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(0, 0, 2, 0);
                        textView.setLayoutParams(layoutParams);
                        textView.setText(getResources().getString(R.string.running_pcr_recycle_num) + (mSLANSections.get(i).getCycles()));
                    }else if(programType == Constants.PROGRAM_TYPE_MELT){
                        final LinearLayout.LayoutParams layoutParams =
                                new LinearLayout.LayoutParams(163*2,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(0, 0, 2, 0);
                        textView.setLayoutParams(layoutParams);
                        int scanmode = mSLANSections.get(i).getScanMode();
                        if(scanmode == Constants.MELT_SCANMODE_CONTINUE){
                            textView.setText(getResources().getString(R.string.ruuning_pcr_scanmode_continuous) + " "
                                    + getResources().getString(R.string.ruuning_pcr_rate) + nf.format(mSLANSections.get(i).getRate())+"℃/s" );
                        }else if(scanmode == Constants.MELT_SCANMODE_STEP){
                            textView.setText(getResources().getString(R.string.ruuning_pcr_scanmode_step) + " "
                                    + getResources().getString(R.string.ruuning_pcr_interval) + nf.format(mSLANSections.get(i).getInterval())+"℃" + " "
                                    + getResources().getString(R.string.ruuning_pcr_melt_holdtime) + mSLANSections.get(i).getMeltHoldtime()+"s");
                        }
                    }
                    mProgramRecycleLayout.addView(textView);

                }
            }
            //更新参数
        }
    }

    //////////////////////////////////////////判别文件///////////////////////////////////////
    /**
     * 打开显示判别文件详情
     * @param fileName
     */
    private void openJudgeFile(String fileName){
        if(!StringUtil.isEmpty(fileName)){
            File file = new File(FileUtils.CONFIG_PATH+ File.separator + "judge/" + fileName);
            if(fileName.endsWith(".ct")){
                mPeakJudgeLayout.setVisibility(View.GONE);
                mCtJudgeLayout.setVisibility(View.VISIBLE);
                if(null != ctRuleEntities){
                    ctRuleEntities.clear();
                    ctRuleEntities = CTRuleEntity.getCTRuleList(file);
                    judgeCtAdapter.setJudgeCtList(ctRuleEntities);
                    judgeCtAdapter.notifyDataSetChanged();
                }

            }else if(fileName.endsWith(".peak")){
                mPeakJudgeLayout.setVisibility(View.VISIBLE);
                mCtJudgeLayout.setVisibility(View.GONE);
                if(null != peakRuleEntities){
                    peakRuleEntities.clear();
                    peakRuleEntities = PEAKRuleEntity.getPEAKRuleList(file);
                    judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                    judgePeakAdapter.notifyDataSetChanged();
                }

            }
        }
    }
    private void updateJudgeFile(){
        mPeakFileList.clear();
        mCtFileList.clear();
        File[] files = new File(FileUtils.CONFIG_PATH + File.separator + "judge").listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {

            for (int i = 0; i < files.length; i++) {
                if (null != mPeakFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".peak")){
                        String name = files[i].getName();
                        mPeakFileList.add(name);
                        Collections.sort(mPeakFileList);
                    }
                }
                if (null != mCtFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".ct")){
                        String name = files[i].getName();
                        mCtFileList.add(name);
                        Collections.sort(mCtFileList);
                    }
                }
            }
        }
        if(isPeak){
            judgeFileAdapter.setTemplateList(mPeakFileList);
        }else {
            judgeFileAdapter.setTemplateList(mCtFileList);
        }
    }
////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * 保存单PCR模板文件
     * @param pcrTemplateName
     */
    private void saveSinglePCRAction(String pcrTemplateName){
        File dir = new File(CONFIG_SINGLE_PCR_TEMPLATE_PATH, pcrTemplateName);
        if (!dir.exists()) {
            dir.mkdirs();
            saveSinglePCR(pcrTemplateName);
        }else{
            showNoticeDialog(getResources().getString(R.string.create_multi_dialog_message_template_exists),pcrTemplateName,0);
        }
    }

    private void saveSinglePCR(String subFirName){
        File dir = new File(CONFIG_SINGLE_PCR_TEMPLATE_PATH, subFirName);
        boolean copyPCRResult = false;
        boolean copyClosedoorResult = false;
        boolean copyCtResult = false;
        boolean copyPeakResult = false;
        boolean saveTimeResult = saveTotalTime(subFirName);
        if(!StringUtil.isEmpty(pcrFileName)){
            String newPcrPath = CONFIG_SINGLE_PCR_TEMPLATE_PATH +  subFirName + File.separator
                    + pcrFileName+".prj";
            String oldPcrPath =  FileUtils.CONFIG_SLAN_PATH +File.separator +pcrFileName +".prj";
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyPCRResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        if(!StringUtil.isEmpty(ctFileName)){
            String newPcrPath = CONFIG_SINGLE_PCR_TEMPLATE_PATH + subFirName + File.separator
                    + ctFileName;
            String oldPcrPath = CONFIG_JUDGE_PATH +ctFileName;
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyCtResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        if(!StringUtil.isEmpty(peakFileName)){
            String newPcrPath = CONFIG_SINGLE_PCR_TEMPLATE_PATH + subFirName + File.separator
                    + peakFileName;
            String oldPcrPath = CONFIG_JUDGE_PATH +peakFileName;
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyPeakResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        String closeDoorFileName = "100-finishcmd";
        String oldFilePath = FileUtils.COMMON_HEX_PATH +closeDoorFileName+".hex";
        String newFilePath  =  CONFIG_SINGLE_PCR_TEMPLATE_PATH +  subFirName + File.separator
                + closeDoorFileName+".hex";
      //  Log.i("AAA","newFilePath ---" + newFilePath);
        if(!StringUtil.isEmpty(newFilePath) && !StringUtil.isEmpty(oldFilePath)){
            copyClosedoorResult =  FileUtils.copyFile(oldFilePath,newFilePath);
            Log.i("AAA","copyClosedoorResult ---" + copyClosedoorResult);
        }
        if(copyPCRResult && copyClosedoorResult && saveTimeResult){
            if(!StringUtil.isEmpty(ctFileName) && !StringUtil.isEmpty(peakFileName)){
                if(copyCtResult && copyPeakResult){
                    //保存单PCR模板成功
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else if(!StringUtil.isEmpty(ctFileName) && StringUtil.isEmpty(peakFileName)){
                if(copyCtResult){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else if(StringUtil.isEmpty(ctFileName) && !StringUtil.isEmpty(peakFileName)){
                if(copyPeakResult){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.create_single_pcr_toast_save_success),Toast.LENGTH_LONG).show();
            }


        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.create_single_pcr_toast_save_fail),Toast.LENGTH_LONG).show();
            if(dir.exists()){
                FileUtils.remove(dir);
                //删除文件夹以及文件
            }
        }
    }

    private boolean saveTotalTime(String subFirName){
        String savePath = CONFIG_SINGLE_PCR_TEMPLATE_PATH + subFirName + File.separator;
        String time = mTotalTimeEt.getText().toString().trim();
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();

        long l_time =  Long.parseLong(time);
        TotalTime totalTime = new TotalTime();
        totalTime.setTotalTime(l_time);
        totalTime.setFirstPcrTime(pcrTime);
        totalTime.setTemperatureCtrlMode(temperatureCtrlMode);
        String totalTime_str = gson.toJson(totalTime);
        String fileName = "totaltime";
        return  FileUtils.saveFileInSdcard(totalTime_str,savePath,fileName,".txt");
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.single_pcr_create_save_btn){
            String templateName = mPCRTitleEt.getText().toString().trim();
            String totalTime = mTotalTimeEt.getText().toString().trim();
            String secondName = mSecondNameEt.getText().toString().trim();
            String thirdName = mThirdNameEt.getText().toString().trim();
            if(!StringUtil.isEmpty(templateName)
                    && !StringUtil.isEmpty(totalTime)){
                if(StringUtil.isEmpty(secondName) && StringUtil.isEmpty(thirdName)){
                    String name = templateName;
                    saveSinglePCRAction(name);
                }else{
                    String name = templateName + "~"+ secondName + "~" + thirdName;
                    saveSinglePCRAction(name);
                }

            }else{
                if(StringUtil.isEmpty(templateName)){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_template_name),Toast.LENGTH_LONG).show();
                }

                if(StringUtil.isEmpty(totalTime)){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_pcr_time),Toast.LENGTH_LONG).show();
                }
            }
        }else if(id == R.id.single_pcr_create_tv){
            Intent createHexIntent = new Intent(CreateSinglePCRActivity.this, ConfigSLANActivity.class);
            startActivity(createHexIntent);
        }else if(id == R.id.single_pcr_create_ct_tv){
            Intent createCtIntent = new Intent(CreateSinglePCRActivity.this, JudgeCtActivity.class);
            startActivity(createCtIntent);
        }else if(id == R.id.single_pcr_create_peak_tv){
            Intent createPeakIntent = new Intent(CreateSinglePCRActivity.this, PeakJudgeActivity.class);
            startActivity(createPeakIntent);
        }
    }

    private void showNoticeDialog(String message,String subFirName,int dialogType){
        if(null != noticeDialog ){
            noticeDialog.dismiss();
            noticeDialog =null;
        }
        if(null == noticeDialog){
            noticeDialog =new NoticeDialog(this,R.style.Dialog);
            noticeDialog.setMessage(message+ subFirName);
            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    noticeDialog.dismiss();
                    if(dialogType == 0) {
                        //
                        //清空文件夹中的文件
                        File dir = new File(CONFIG_SINGLE_PCR_TEMPLATE_PATH, subFirName);
                        File[] files = dir.listFiles();//将file子目录及子文件放进文件数组
                        if (files != null) {//如果包含文件进行删除操作
                            for (int i = 0; i < files.length; i++) {
                                if (files[i].isFile()) {//删除子文件
                                    files[i].delete();
                                }
                            }
                        }
                        saveSinglePCR(subFirName);
                    }
                }
            });

            noticeDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                @Override
                public void onCancelClick() {
                    noticeDialog.dismiss();
                }
            });
            noticeDialog.show();
        }
    }
}
