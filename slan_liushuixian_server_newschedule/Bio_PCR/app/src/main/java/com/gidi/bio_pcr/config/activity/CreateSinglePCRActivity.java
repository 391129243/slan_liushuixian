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

import com.gidi.bio_pcr.PcrApplication;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateSinglePCRActivity extends BaseActivity implements View.OnClickListener{
    private static final String TAG = "CreateSinglePCRActivity";
    private RelativeLayout mTitleLayout;
    private EditText mPCRTitleEt,mTotalTimeEt,mSecondNameEt,mThirdNameEt;
    private CustomImageText mPCRTabTv,mCtTabTv,mPeakTabTv;
    private RelativeLayout mPCRFileLayout,mJudgeFileLayout;
    private TextView mPcrFileNameTv,mSavePcrTempTv;
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

    private String pcrFileName,pcrTemplateName,lastPeakName;
    private boolean isEditMode = false;
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
    private TextView peakOneFileTv,peakTwoFileTv,ctOneFileTv,ctTwoFileTv;
    private TextView peakHole1Tv,peakHole2Tv,ctHole1Tv,ctHole2Tv;
    private TextView peakOneTv,peakTwoTv,ctOneTv,ctTwoTv;//在右边显示孔位选择的判别文件内容
    private Map<Integer, String> holeCtFileNameMap = new HashMap<>();
    // 同理，peak文件的映射
    private Map<Integer, String> holePeakFileNameMap = new HashMap<>();
    private List<TextView> peakHoleTvs,ctHoleTvs;
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
        //初始化输入框
        initEditTexts();

        //初始化控温方式选择
        initTempControlMode();

        //初始化核心布局
        initCoreViews();

        //初始化判别文件相关UI
        initJudgeFileViews();

        initHoleSelectViews();
        initData();
    }

    /**
     * 初始化编辑框
     */
    private void initEditTexts(){
        mPCRTitleEt = (EditText) findViewById(R.id.single_pcr_create_template_name_et);
        mPCRTitleEt.addTextChangedListener(new LimitInputTextWatcher(mPCRTitleEt));
        mSecondNameEt = (EditText) findViewById(R.id.single_pcr_create_template_name_second_et);
        mThirdNameEt = (EditText) findViewById(R.id.single_pcr_create_template_name_third_et);
        mSecondNameEt.addTextChangedListener(new LimitInputTextWatcher(mSecondNameEt));
        mThirdNameEt.addTextChangedListener(new LimitInputTextWatcher(mThirdNameEt));
        mTotalTimeEt = (EditText)findViewById(R.id.single_pcr_create_totaltime_et);
    }

    //初始化控温方式选择
    private  void  initTempControlMode(){
        mTempCtrlModeRg = (RadioGroup)findViewById(R.id.single_pcr_tempctrl_mode_rg);
        mTempCtrlFastRb = (RadioButton) findViewById(R.id.single_pcr_tempctrl_fast);
        mTempCtrlStandardRb= (RadioButton)findViewById(R.id.single_pcr_tempctrl_standard);
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
    }

    //初始化核心相关布局
    private void initCoreViews(){
        mPcrFileNameTv = (TextView) findViewById(R.id.single_pcr_create_pcr_name_tv);
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
    }

    private void initJudgeFileViews(){
        mCreateCtJudgeTv = (TextView)findViewById(R.id.single_pcr_create_ct_tv) ;
        mCreatePeakJudgeTv = (TextView)findViewById(R.id.single_pcr_create_peak_tv) ;
        mCtJudgeLayout = (RelativeLayout)findViewById(R.id.config_single_pcr_judge_ct_layout);
        mCtJudgeDetailLv = (ListView)findViewById(R.id.single_pcr_create_judge_ct_list_detail) ;
        judgeCtAdapter = new JudgeCtAdapter(getApplicationContext());
        mCtJudgeDetailLv.setAdapter(judgeCtAdapter);

        mPeakJudgeLayout = (RelativeLayout)findViewById(R.id.config_single_pcr_judge_peak_layout);
        mPeakJudgeDetailRv = (RecyclerView)findViewById(R.id.single_pcr_create_judge_peak_list_detail);
        judgePeakAdapter = new JudgePeakAdapter(getApplicationContext());
        mPeakJudgeDetailRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,
                false));
        mPeakJudgeDetailRv.setAdapter(judgePeakAdapter);
    }

    /**
     * 初始化孔位选择UI
     */
    private void initHoleSelectViews(){
        peakHole1Tv = (TextView) findViewById(R.id.single_hole1_tv);
        peakHole2Tv = (TextView) findViewById(R.id.single_hole2_tv);

        peakOneTv = (TextView)findViewById(R.id.peak_hole_content_1_tv);
        peakTwoTv = (TextView)findViewById(R.id.peak_hole_content_2_tv);

        ctHole1Tv = (TextView)findViewById(R.id.single_pcr_ct_hole1_tv);
        ctHole2Tv = (TextView)findViewById(R.id.single_pcr_ct_hole2_tv);

        ctOneTv = (TextView)findViewById(R.id.ct_hole_content_1_tv);
        ctTwoTv = (TextView)findViewById(R.id.ct_hole_content_2_tv);

        ctOneFileTv = (TextView)findViewById(R.id.single_pcr_ct_1_name_tv) ;
        ctTwoFileTv = (TextView)findViewById(R.id.single_pcr_ct_2_name_tv) ;
        peakOneFileTv = (TextView)findViewById(R.id.single_pcr_peak_1_name_tv);
        peakTwoFileTv = (TextView)findViewById(R.id.single_pcr_peak_2_name_tv);
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
                        String fileName = mPeakFileList.get(click_peak_pos);
                        openJudgeFile(fileName);
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
                if(mCtFileList != null){
                    judgeFileAdapter.setTemplateList(mCtFileList);
                    if(isCtCheck && click_ct_pos != -1){
                        judgeFileAdapter.setSelectedPosition(click_ct_pos);
                        String fileName = mCtFileList.get(click_ct_pos);
                        openJudgeFile(fileName);
                    }
                }
            }
        });
        //初始化孔位选择监听
        initHoleSelectListener();
    }

    //初始化孔位选择监听
    private void initHoleSelectListener() {
        peakHole1Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!peakHole1Tv.isSelected()) {
                    if (peakHole1Tv.isActivated()) {
                        peakHole1Tv.setActivated(false);
                        peakHole1Tv.setSelected(false);
                        if (holePeakFileNameMap != null) {
                            holePeakFileNameMap.remove(1);
                        }
                        peakOneFileTv.setText("");
                        peakOneTv.setText("");
                    } else {
                        judgeFileAdapter.setSelectedPosition(-1);
                        peakHole1Tv.setSelected(true);
                        if (peakHole2Tv.isSelected()
                                && !peakHole2Tv.isActivated()
                                && !StringUtil.isEmpty(peakTwoFileTv.getText().toString())) {
                            peakHole2Tv.setSelected(false);
                            peakHole2Tv.setActivated(true);
                        }
                    }

                } else {
                    peakHole1Tv.setSelected(false);
                    if (holePeakFileNameMap != null) {
                        holePeakFileNameMap.remove(1);
                    }
                    peakOneFileTv.setText("");
                    peakOneTv.setText("");
                }
            }
        });
        peakHole2Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!peakHole2Tv.isSelected()) {
                    if (peakHole2Tv.isActivated()) {
                        peakHole2Tv.setActivated(false);
                        peakHole2Tv.setSelected(false);
                        if (holePeakFileNameMap != null) {
                            holePeakFileNameMap.remove(2);
                        }
                        peakTwoFileTv.setText("");
                        peakTwoTv.setText("");
                    } else {
                        peakHole2Tv.setSelected(true);
                        judgeFileAdapter.setSelectedPosition(-1);
                        if (peakHole1Tv.isSelected()
                                && !peakHole1Tv.isActivated()
                                && !StringUtil.isEmpty(peakOneFileTv.getText().toString())) {
                            peakHole1Tv.setSelected(false);
                            peakHole1Tv.setActivated(true);
                        }

                    }

                } else {
                    peakHole2Tv.setActivated(false);
                    peakHole2Tv.setSelected(false);
                    if (holePeakFileNameMap != null) {
                        holePeakFileNameMap.remove(2);
                    }
                    peakTwoFileTv.setText("");
                    peakTwoTv.setText("");
                }
            }
        });

        //4个孔位的判别文件

        ctHole1Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!ctHole1Tv.isSelected()) {
                    if (ctHole1Tv.isActivated()) {
                        ctHole1Tv.setActivated(false);
                        ctHole1Tv.setSelected(false);
                        if (holeCtFileNameMap != null) {
                            holeCtFileNameMap.remove(1);
                        }
                        ctOneFileTv.setText("");
                        ctOneTv.setText("");
                    } else {
                        judgeFileAdapter.setSelectedPosition(-1);
                        ctHole1Tv.setSelected(true);
                        if (ctHole2Tv.isSelected()
                                && !ctHole2Tv.isActivated()
                                && !StringUtil.isEmpty(ctTwoFileTv.getText().toString())) {
                            ctHole2Tv.setSelected(false);
                            ctHole2Tv.setActivated(true);
                        }
                    }

                } else {
                    ctHole1Tv.setSelected(false);
                    if (holeCtFileNameMap != null) {
                        holeCtFileNameMap.remove(1);
                    }
                    ctOneFileTv.setText("");
                    ctOneTv.setText("");
                }
            }
        });
        ctHole2Tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!ctHole2Tv.isSelected()) {
                    if (ctHole2Tv.isActivated()) {
                        ctHole2Tv.setActivated(false);
                        ctHole2Tv.setSelected(false);
                        if (holeCtFileNameMap != null) {
                            holeCtFileNameMap.remove(2);
                        }
                        ctTwoFileTv.setText("");
                        ctTwoTv.setText("");
                    } else {
                        judgeFileAdapter.setSelectedPosition(-1);
                        ctHole2Tv.setSelected(true);
                        if (ctHole1Tv.isSelected()
                                && !ctHole1Tv.isActivated()
                                && !StringUtil.isEmpty(ctOneFileTv.getText().toString())) {
                            ctHole1Tv.setSelected(false);
                            ctHole1Tv.setActivated(true);
                        }

                    }

                } else {
                    ctHole2Tv.setSelected(false);
                    if (holeCtFileNameMap != null) {
                        holeCtFileNameMap.remove(2);
                    }
                    ctTwoFileTv.setText("");
                    ctTwoTv.setText("");
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
        peakHoleTvs = new ArrayList<>(Arrays.asList(
                peakHole1Tv, peakHole2Tv
        ));
        ctHoleTvs = new ArrayList<>(Arrays.asList(
                ctHole1Tv, ctHole2Tv
        ));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isEditMode = getIntent().getBooleanExtra("isEdit",false);
        pcrTemplateName = getIntent().getStringExtra("templateName");
        if(isEditMode){
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
        if(isEditMode){
            loadEditModeData();
            isEditMode = false;
        }
    }

    //编辑模式下加载数据
    private void loadEditModeData(){
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
                    isCtCheck = true;
                    if(file.getName().startsWith("hole1")){
                        String ctOneFile = file.getName().substring(file.getName().lastIndexOf("_") + 1);
                        ctOneFileTv.setText(ctOneFile);
                        ctOneTv.setText(ctOneFile);
                        int pos = getClickedPosition(ctOneFile,mCtFileList);
                        click_ct_pos= pos;
                        holeCtFileNameMap.put(1,ctOneFile);

                        setMultiSelected(ctHole1Tv, ctHoleTvs.toArray(new TextView[0]));
                    }else if(file.getName().startsWith("hole2")){
                        String ctTwoFile = file.getName().substring(file.getName().lastIndexOf("_") + 1);
                        ctTwoFileTv.setText(ctTwoFile);
                        ctTwoTv.setText(ctTwoFile);
                        int pos = getClickedPosition(ctTwoFile,mCtFileList);
                        click_ct_pos= pos;
                        holeCtFileNameMap.put(2,ctTwoFile);
                        setMultiSelected(ctHole2Tv, ctHoleTvs.toArray(new TextView[0]));
                    }

                }else if(file.getName().endsWith(".peak")){
                    isPeakCheck = true;
                    if(file.getName().startsWith("hole1")){
                        String peakOneFile = file.getName().substring(file.getName().lastIndexOf("_") + 1);
                        peakOneFileTv.setText(peakOneFile);
                        peakOneTv.setText(peakOneFile);
                        int pos = getClickedPosition(peakOneFile,mPeakFileList);
                        click_peak_pos = pos;
                        lastPeakName = peakOneFile;
                        setMultiSelected(peakHole1Tv, peakHoleTvs.toArray(new TextView[0]));
                        holePeakFileNameMap.put(1,peakOneFile);
                    }else if(file.getName().startsWith("hole2")){
                        String peakSecondFile = file.getName().substring(file.getName().lastIndexOf("_") + 1);
                        peakTwoFileTv.setText(peakSecondFile);
                        peakTwoTv.setText(peakSecondFile);
                        int pos = getClickedPosition(peakSecondFile,mPeakFileList);
                        click_peak_pos = pos;
                        lastPeakName = peakSecondFile;
                        setMultiSelected(peakHole2Tv, peakHoleTvs.toArray(new TextView[0]));
                        holePeakFileNameMap.put(2,peakSecondFile);
                    }

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
        if(null != peakHoleTvs){
            peakHoleTvs.clear();
            peakHoleTvs = null;
        }
        if(null != ctHoleTvs){
            ctHoleTvs.clear();
            ctHoleTvs = null;
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
                    String ctFileName = mCtFileList.get(position);
                    //更新UI
                    isCtCheck = true;
                    if(ctHole1Tv.isSelected()){
                        String ctFile_1=  mCtFileList.get(position);
                        ctOneTv.setText(ctFile_1);
                        ctOneFileTv.setText(ctFile_1);
                        holeCtFileNameMap.put(1, ctFile_1);
                    }
                    if(ctHole2Tv.isSelected()){
                        String ctFile_2=  mCtFileList.get(position);
                        ctTwoTv.setText(ctFile_2);
                        ctTwoFileTv.setText(ctFile_2);
                        holeCtFileNameMap.put(2, ctFile_2);
                    }

                    Log.i(TAG,"==holeCtFileNameMap size==="+ holeCtFileNameMap.size());
                    openJudgeFile(ctFileName);
                }else{
                    judgeFileAdapter.setTemplateList(mPeakFileList);
                    judgeFileAdapter.setSelectedPosition(position);
                    click_peak_pos = position;
                    String peakFileName = mPeakFileList.get(position);
                    isPeakCheck = true;
                    if(peakHole1Tv.isSelected()){
                        String peakFile_1=  mPeakFileList.get(position);
                        peakOneTv.setText(peakFile_1);
                        peakOneFileTv.setText(peakFile_1);
                        holePeakFileNameMap.put(1, peakFile_1);
                    }
                    if(peakHole2Tv.isSelected()){
                        String peakFile_2=  mPeakFileList.get(position);
                        peakTwoTv.setText(peakFile_2);
                        peakTwoFileTv.setText(peakFile_2);
                        holePeakFileNameMap.put(2, peakFile_2);
                    }


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
                    //更新UI
                    isCtCheck = false;
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
                    isPeakCheck = false;
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
        if(!StringUtil.isEmpty(fileName)&& null != PcrApplication.slanService){
            ArrayList<Project> mConfigProjects = PcrApplication.slanService.getProjects(FileUtils.CONFIG_SLAN_PATH);//获取目录中的项目
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
        boolean copyAllCtFilesResult = true; // 默认成功，有一个失败则变false
        boolean copyAllPeakFilesResult = true;
        boolean saveTimeResult = saveTotalTime(subFirName);
        if(!StringUtil.isEmpty(pcrFileName)){
            String newPcrPath = CONFIG_SINGLE_PCR_TEMPLATE_PATH +  subFirName + File.separator
                    + pcrFileName+".prj";
            String oldPcrPath =  FileUtils.CONFIG_SLAN_PATH +File.separator +pcrFileName +".prj";
            if(!StringUtil.isEmpty(newPcrPath) && !StringUtil.isEmpty(oldPcrPath)){
                copyPCRResult =  FileUtils.copyFile(oldPcrPath,newPcrPath);
            }
        }

        String holeSourceDir = CONFIG_JUDGE_PATH;
        // 孔文件的目标目录
        String holeTargetDir = CONFIG_SINGLE_PCR_TEMPLATE_PATH + subFirName + File.separator;
        if (holeCtFileNameMap != null && !holeCtFileNameMap.isEmpty()) {
            for (Map.Entry<Integer, String> entry : holeCtFileNameMap.entrySet()) {
                Integer holeNo = entry.getKey();    // 孔号
                String holeFileName = entry.getValue(); // 孔对应的文件名

                if (StringUtil.isEmpty(holeFileName)) {
                    continue;
                }

                // 源路径 / 目标路径
                String oldHolePath = holeSourceDir  + holeFileName;
                String newHolePath = holeTargetDir + "hole"+ holeNo +"_"+ holeFileName;

                Log.i("CreateSingleActivity", "复制孔文件【孔号=" + holeNo + "】源：" + oldHolePath);
                Log.i("CreateSingleActivity", "复制孔文件【孔号=" + holeNo + "】目标：" + newHolePath);

                // 复制文件
                boolean copyOne = FileUtils.copyFile(oldHolePath, newHolePath);
                if (!copyOne) {
                    copyAllCtFilesResult = false; // 只要有一个失败，整体标记失败
                    Log.e("CreateSingleActivity", "孔文件复制失败：" + holeFileName);
                }
            }
        }

        if (holePeakFileNameMap != null && !holePeakFileNameMap.isEmpty()) {
            for (Map.Entry<Integer, String> entry : holePeakFileNameMap.entrySet()) {
                Integer holeNo = entry.getKey();    // 孔号
                String holeFileName = entry.getValue(); // 孔对应的文件名

                if (StringUtil.isEmpty(holeFileName)) {
                    continue;
                }

                // 源路径 / 目标路径
                String oldHolePath = holeSourceDir + holeFileName;
                String newHolePath = holeTargetDir + "hole"+holeNo + "_"+ holeFileName;

                Log.i(TAG, "复制孔文件【孔号=" + holeNo + "】源：" + oldHolePath);
                Log.i(TAG, "复制孔文件【孔号=" + holeNo + "】目标：" + newHolePath);

                // 复制文件
                boolean copyOne = FileUtils.copyFile(oldHolePath, newHolePath);
                if (!copyOne) {
                    copyAllPeakFilesResult = false; // 只要有一个失败，整体标记失败
                    Log.e(TAG, "孔文件复制失败：" + holeFileName);
                }
            }
        }


        String closeDoorFileName = "98-closedoor.hex";
        String oldFilePath = "/mnt/sdcard/PCR/commonHex_v5/98-closedoor.hex";
        String newFilePath  =  CONFIG_SINGLE_PCR_TEMPLATE_PATH +  subFirName + File.separator
                + closeDoorFileName;
        File finishFile = new File(oldFilePath);

        Log.e(TAG,"newFilePath ---" + newFilePath);
        Log.e(TAG,"oldFilePath ---" + finishFile.exists());
        if(!StringUtil.isEmpty(newFilePath) && !StringUtil.isEmpty(oldFilePath)){
            copyClosedoorResult =  FileUtils.copyFile(oldFilePath,newFilePath);
            Log.i(TAG,"copyClosedoorResult ---" + copyClosedoorResult);
        }
        if(copyPCRResult && copyClosedoorResult && saveTimeResult){
            if(holeCtFileNameMap != null && !holeCtFileNameMap.isEmpty()
                    && holePeakFileNameMap != null && !holePeakFileNameMap.isEmpty()){
                if(copyAllPeakFilesResult && copyAllCtFilesResult){
                    //保存单PCR模板成功
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else if(holeCtFileNameMap != null && !holeCtFileNameMap.isEmpty()
                    && holePeakFileNameMap != null && holePeakFileNameMap.isEmpty()){
                if(copyAllCtFilesResult){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_success),Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_pcr_toast_save_fail),Toast.LENGTH_LONG).show();
                }
            }else if(holeCtFileNameMap != null && holeCtFileNameMap.isEmpty()
                    &&  holePeakFileNameMap != null && !holePeakFileNameMap.isEmpty()){
                if(copyAllPeakFilesResult){
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


    /**
     * 多选激活方法：只把传入view设为activated=true，其他原有状态保留，不清除
     * @param targetView 需要标记为已绑定文件的孔位
     * @param allViews 全部孔位数组
     */
    private void setMultiSelected(TextView targetView, TextView... allViews) {
        if (targetView == null || allViews == null) return;
        for (TextView tv : allViews) {
            if (tv == targetView) {
                tv.setSelected(false);
                tv.setActivated(true);
            }
            // 其他view不做任何修改，保留原有activated状态，实现多选
        }
    }
}
