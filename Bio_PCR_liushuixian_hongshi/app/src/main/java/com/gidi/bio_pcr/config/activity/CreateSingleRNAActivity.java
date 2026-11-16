package com.gidi.bio_pcr.config.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.PcrApplication;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.constant.HexFileName_V5;
import com.gidi.bio_pcr.WintersUtils.entity.TotalTime;
import com.gidi.bio_pcr.WintersUtils.universalUtils.HexConfig;
import com.gidi.bio_pcr.WintersUtils.universalUtils.TimerCaculate;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.ConfigManager;
import com.gidi.bio_pcr.config.adapter.FileNameAdapter;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.gidi.bio_pcr.view.LinearSpacingItemDecoration;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CreateSingleRNAActivity extends BaseActivity implements View.OnClickListener {
    private RelativeLayout mTitleLayout;
    private EditText mHexTitleEt,mTotalTimeEt,mSecondNameEt,mThirdNameEt;

    private TextView mHexFileNameTv,mSaveHexTempTv,mCreateHexTv;
    private RecyclerView mHexFileRv;
    private List<String> mHexFileList;
    private FileNameAdapter hexFileAdapter;
    private int click_hex = -1;
    private String hexFileName;
    private boolean isEdit = false;
    private TextView magneticlive_tv;
    //核酸提取文件详情
    //1:磁珠活化样本体积,2:裂解温度,3:裂解样本体积,4裂解液体积，5裂解时间
    private TextView mMegneticsampleTv,mSpiltTempTv,mLysisTv,mSpiltTimeTv;
    private TextView mWashTemp_1Tv,mWashVolume_1Tv,mWashTime_1Tv,mWashTemp_2Tv,mWashVolume_2Tv,mWashTime_2Tv
            ,mWashTemp_3Tv,mWashVolume_3Tv,mWashTime_3Tv;
    //洗脱,洗脱液体积
    private TextView mElutionTempTv,mElutionVolumeTv,mElutionTimeTv;
    private TextView mQuantifyVolumeTv;//体积定量
    //液态体系
    private TextView mLiquidSysTv;
    private ImageView mCloseDoorCkImg,mMegeticCkImg,mSpliteCkImg,mWash1CkImg,mWash2CkImg,mWash3CkImg,
            mElutionCkImg,mQuantifyCkImg,mSecondCkImg;
    private LinearLayout mTwoWashLayout,mThreeWashLayout;
    String CONFIG_HEX_PATH = HexFileName_V5.HEXCREATEFILE_PATH;
    String CONFIG_SINGLERNA_TEMP_PATH = FileUtils.CONFIG_TEMPLATE_PATH + "singleRNA" + File.separator;
    private  String tempLateName;
    private NoticeDialog noticeDialog;
    private int rnaTime;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_create_hex_template;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTitleLayout = (RelativeLayout)findViewById(R.id.hex_create_title_layout);
        TextView mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.create_single_rna_title));
        ImageView mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mSaveHexTempTv = (TextView) findViewById(R.id.hex_create_save_btn);
        mHexFileNameTv = (TextView) findViewById(R.id.hex_create_rna_name_tv);

        mHexTitleEt = (EditText) findViewById(R.id.hex_create_template_name_et);
        mTotalTimeEt = (EditText)findViewById(R.id.single_rna_create_totaltime_et);
        mSecondNameEt = (EditText) findViewById(R.id.hex_create_template_name_second_et);
        mThirdNameEt = (EditText) findViewById(R.id.hex_create_template_name_third_et);
        mHexTitleEt.addTextChangedListener(new LimitInputTextWatcher(mHexTitleEt));
        mSecondNameEt.addTextChangedListener(new LimitInputTextWatcher(mSecondNameEt));
        mThirdNameEt.addTextChangedListener(new LimitInputTextWatcher(mThirdNameEt));
        mCreateHexTv = (TextView) findViewById(R.id.hex_create_hex_create_tv);
        mHexFileRv = (RecyclerView) findViewById(R.id.hex_create_rna_list);
        magneticlive_tv = (TextView)findViewById(R.id.magneticlive_tv);
        //核酸提取详情
        mMegneticsampleTv = (TextView)findViewById(R.id.hex_create_tv_megneticsample);
        mSpiltTempTv = (TextView)findViewById(R.id.hex_create_splittemperature_tv);

        mLysisTv = (TextView)findViewById(R.id.hex_create_splitlijieye_tv);
        mSpiltTimeTv = (TextView)findViewById(R.id.hex_create_splittime_tv);
        mWashTemp_1Tv = (TextView)findViewById(R.id.hex_create_wash1temp_tv);
        mWashVolume_1Tv = (TextView)findViewById(R.id.hex_create_wash1xidiye_tv);
        mWashTime_1Tv = (TextView)findViewById(R.id.hex_create_wash1time_tv);
        mWashTemp_2Tv = (TextView)findViewById(R.id.hex_create_wash2_2temperature_tv);
        mWashVolume_2Tv = (TextView)findViewById(R.id.hex_create_wash2_2xidiye_tv);
        mWashTime_2Tv = (TextView)findViewById(R.id.hex_create_wash2_2time);
        mWashTemp_3Tv = (TextView)findViewById(R.id.hex_create_wash3_3temperature_tv);
        mWashVolume_3Tv = (TextView)findViewById(R.id.hex_create_wash3_3xidiye_tv);
        mWashTime_3Tv = (TextView)findViewById(R.id.hex_create_wash3_3time_tv);
        mElutionTempTv = (TextView)findViewById(R.id.hex_create_elutiontemperature_tv);
        mElutionVolumeTv = (TextView)findViewById(R.id.hex_create_elutionxituoye_tv);
        mElutionTimeTv = (TextView)findViewById(R.id.hex_create_elutiontime_tv);
        mQuantifyVolumeTv = (TextView)findViewById(R.id.hex_create_quantifyvolume);
        mLiquidSysTv = (TextView)findViewById(R.id.hex_create_freeze_dry_tv);

        mTwoWashLayout = (LinearLayout)findViewById(R.id.hex_create_layout_wash2);
        mThreeWashLayout  = (LinearLayout)findViewById(R.id.hex_create_layout_wash3_3);
        mTwoWashLayout.setVisibility(View.GONE);
        mThreeWashLayout.setVisibility(View.GONE);
        mCloseDoorCkImg = (ImageView)findViewById(R.id.hex_create_close_door_ck_img);
        mMegeticCkImg = (ImageView) findViewById(R.id.hex_create_megnetic_ck_img);
        mSpliteCkImg = (ImageView) findViewById(R.id.hex_create_split_ck_img);
        mWash1CkImg =(ImageView) findViewById(R.id.hex_create_wash1_ck_img);
        mWash2CkImg=(ImageView) findViewById(R.id.hex_create_wash2_ck_img);
        mWash3CkImg=(ImageView) findViewById(R.id.hex_create_wash3_ck_img);
        mElutionCkImg=(ImageView) findViewById(R.id.hex_create_elution_ck_img);
        mQuantifyCkImg=(ImageView) findViewById(R.id.hex_create_quantify_ck_img);
        mSecondCkImg=(ImageView) findViewById(R.id.hex_create_second_ck_img);

        LinearLayoutManager layoutManager = new GridLayoutManager(getApplicationContext(),5);
        mHexFileRv.setLayoutManager(layoutManager);
        mHexFileRv.addItemDecoration(new LinearSpacingItemDecoration(this, 6));
        hexFileAdapter = new FileNameAdapter(getApplicationContext());
        mHexFileList = new ArrayList<String>();
        hexFileAdapter.setTemplateList(mHexFileList);
        mHexFileRv.setAdapter(hexFileAdapter);
        initData();
    }

    @Override
    protected void initListener() {
        mSaveHexTempTv.setOnClickListener(this);
        mCreateHexTv.setOnClickListener(this);
    }

    @Override
    protected void initData() {
        hexFileAdapter.setOnItemClickListener(new FileNameAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                hexFileAdapter.setTemplateList(mHexFileList);
                hexFileAdapter.setSelectedPosition(position);
                click_hex = position;
                hexFileName = mHexFileList.get(position);
                mHexFileNameTv.setText(hexFileName+".hex");
                openRNAFile( hexFileName);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isEdit = getIntent().getBooleanExtra("isEdit",false);
        tempLateName = getIntent().getStringExtra("templateName");
        if(isEdit){
            //模板的文件夹路径
            if(!StringUtil.isEmpty(tempLateName)){
                if(tempLateName.contains("~")){
                    String[] names = tempLateName.split("~");
                    if(names != null &&names.length>0){
                        switch (names.length){
                            case 1:
                                mHexTitleEt.setText(names[0]);
                                break;
                            case 2:
                                mHexTitleEt.setText(names[0]);
                                mSecondNameEt.setText(names[1]);
                                break;
                            case 3:
                                mHexTitleEt.setText(names[0]);
                                mSecondNameEt.setText(names[1]);
                                mThirdNameEt.setText(names[2]);
                                break;
                        }

                    }
                }else{
                    mHexTitleEt.setText(tempLateName);
                }
            }
            mHexTitleEt.setText(tempLateName);
        }else{
            String subFirName = "hex_" + DateFormatUtil.getFileName();
            mHexTitleEt.setText(subFirName);
            mTotalTimeEt.setText(3600+"");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHexFile();
        if(isEdit){
            //编辑模板
            String localHexTempPath = CONFIG_SINGLERNA_TEMP_PATH+ tempLateName+File.separator;
            List<File> editFiles = FileUtils.getFileList(localHexTempPath);
            if(null != editFiles && editFiles.size()>0){
                for(File file :editFiles){
                    if(file != null && file.getName().endsWith(".hex")) {
                        //核酸检测文件
                        hexFileName = file.getName().substring(0, file.getName().length() - 4);
                        //更新UI
                        mHexFileNameTv.setText(hexFileName + ".hex");
                        int pos = getClickedPosition(hexFileName, mHexFileList);
                        click_hex = pos;
                        hexFileAdapter.setSelectedPosition(click_hex);
                        openRNAFile(hexFileName);
                    }else if(file.getName().equals("totaltime.txt")){
                        //全流程时间
                        TotalTime totalTime = ConfigManager.getTotalTimeFile(file);
                        mTotalTimeEt.setText(totalTime.getTotalTime()+"");
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mHexFileList){
            mHexFileList.clear();
            mHexFileList = null;
        }

        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
    }

    private void updateHexFile(){

        File[] files = new File(CONFIG_HEX_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            mHexFileList.clear();
            for (int i = 0; i < files.length; i++) {
                if (null != mHexFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".hex")){
                        String name = files[i].getName().substring(0,files[i].getName().length()-4);
                        mHexFileList.add(name);
                        Collections.sort(mHexFileList);
                    }
                }
            }
        }
        hexFileAdapter.setTemplateList(mHexFileList);
    }

    /**
     * 核酸提取文件夹详情
     * @param fileName
     */
    /**
     * 查看核酸检测文件
     * @param fileName
     */
    private void openRNAFile(String fileName){
        if(!StringUtil.isEmpty(fileName)){
            //先判断是V4版本还是V5版本
            String path = null;
            if(PcrApplication.rnaVersion.equals("V4")){
                //从/mnt/sdcard/PCR/hexCreateFile_v4/hexConfig_v4打开所需要的文件
            }else if(PcrApplication.rnaVersion.equals("V5")){
                //从/mnt/sdcard/PCR/hexCreateFile_v5/hexConfig_v5打开所需要的文件
                path = HexFileName_V5.HEXCONFIG_PATH + fileName + ".hexConfig";
            }
            if(null != path){
                File file = new File(path);

                if(file != null && file.exists()&& file.isFile()){
                    HexConfig hexConfig = ConfigManager.getHexConfigFile(file);
                    initHexVaribale();
                    updateHexFileUI(hexConfig);
                }
            }else{
                Toast.makeText(getApplicationContext(),"无法打开此文件",Toast.LENGTH_LONG).show();
            }
        }
    }

    private void saveHexTempAction(String templateName){
        String subFirName = templateName;
        File dir = new File(CONFIG_SINGLERNA_TEMP_PATH, subFirName);
        if (!dir.exists()) {
            dir.mkdirs();
            saveHexTemplate(subFirName);
        }else{
            showNoticeDialog(getResources().getString(R.string.create_multi_dialog_message_template_exists),
                    subFirName,0);
        }
    }

    /**
     * 保存核酸提取模板
     */
    private void saveHexTemplate(String subFirName){
        File dir = new File(CONFIG_SINGLERNA_TEMP_PATH, subFirName);
        boolean copyHexResult = false;
        boolean saveTimeResult = saveTotalTime(subFirName);
        if(!StringUtil.isEmpty(hexFileName)){
            String newHexPath = CONFIG_SINGLERNA_TEMP_PATH + subFirName + File.separator
                    + hexFileName+".hex";
            String oldHexPath =  CONFIG_HEX_PATH +hexFileName +".hex";
            if(!StringUtil.isEmpty(newHexPath) && !StringUtil.isEmpty(oldHexPath)){
                copyHexResult =  FileUtils.copyFile(oldHexPath,newHexPath);
            }
        }

        if(copyHexResult && saveTimeResult){
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.create_multi_toast_save_success),Toast.LENGTH_LONG).show();

        }else{
            if(!copyHexResult){
                Toast.makeText(getApplicationContext()
                        ,getResources().getString(R.string.create_single_rna_save_fail_ran_file_error),Toast.LENGTH_LONG).show();
            }
            if(!saveTimeResult){
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.create_single_rna_save_fail_ran_time),Toast.LENGTH_LONG).show();
            }
            if(dir.exists()){
                FileUtils.remove(dir);
                //删除文件夹以及文件
            }
        }
    }

    private boolean saveTotalTime(String subFirName){
        String savePath = CONFIG_SINGLERNA_TEMP_PATH + subFirName + File.separator;
        Gson gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();


        long l_time =  (long)rnaTime;
        TotalTime totalTime = new TotalTime();
        totalTime.setTotalTime(l_time);
        totalTime.setFirstRNATime(rnaTime);
        String totalTime_str = gson.toJson(totalTime);
        String fileName = "totaltime";
        return  FileUtils.saveFileInSdcard(totalTime_str,savePath,fileName,".txt");
    }


        private void initHexVaribale(){
        mMegneticsampleTv.setText("");
        //裂解温度
        mSpiltTempTv.setText("");

        //裂解液体积
        mLysisTv.setText("");
        //裂解时间
        mSpiltTimeTv.setText("");
        mWashTemp_1Tv.setText("");
        mWashVolume_1Tv.setText("");
        mWashTime_1Tv.setText("");

        //三部洗涤
        mWashTemp_2Tv.setText("");
        mWashVolume_2Tv.setText("");
        mWashTime_2Tv.setText("");

        mWashTemp_3Tv.setText("");
        mWashVolume_3Tv.setText("");
        mWashTime_3Tv.setText("");


        mElutionTempTv.setText("");
        mElutionVolumeTv.setText("");
        mElutionTimeTv.setText("");
        mQuantifyVolumeTv.setText("");
    }

    private void updateHexFileUI(HexConfig hexConfig){
        if(null != hexConfig){

            rnaTime= TimerCaculate.RNA_Fisrt_Caculate(hexConfig);

            mTotalTimeEt.setText(rnaTime + "");

            if(hexConfig.getMagneticChoice() == 0){
                magneticlive_tv.setText(getResources().getString(R.string.config_hex_step_2_one));
            }else if(hexConfig.getMagneticChoice() == 1){
                magneticlive_tv.setText(getResources().getString(R.string.config_hex_step_2_two));
            }
            if(hexConfig.getFreeze_drying() == 0){
                mLiquidSysTv.setText(getResources().getString(R.string.config_hex_liquid_system));
            }else if(hexConfig.getFreeze_drying() == 1){
                mLiquidSysTv.setText(getResources().getString(R.string.config_hex_freeze_drying_system));
            }
            //磁珠活化样样本体积
            mMegneticsampleTv.setText(hexConfig.getMagneticVolume()+"");
            //裂解温度
            mSpiltTempTv.setText(hexConfig.getSplitTemperature()+"");

            //裂解液体积
            mLysisTv.setText(hexConfig.getSplitLieJieYeVolume()+"");
            //裂解时间
            mSpiltTimeTv.setText(hexConfig.getSplitTime()+"");
            mWashTemp_1Tv.setText(hexConfig.getWash1Temperature()+"");
            mWashVolume_1Tv.setText(hexConfig.getWash1XiDiYeVolume()+"");
            mWashTime_1Tv.setText(hexConfig.getWash1Time()+"");
            //卡盒入位
            if(hexConfig.isCasseteFlag()){
                mCloseDoorCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mCloseDoorCkImg.setImageResource(R.drawable.check_normal);
            }
            //磁珠活化
            if(hexConfig.isMagneticFlag()){
                mMegeticCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mMegeticCkImg.setImageResource(R.drawable.check_normal);
            }
            if(hexConfig.isSplitFlag()){
                mSpliteCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mSpliteCkImg.setImageResource(R.drawable.check_normal);
            }


            if(hexConfig.isElutionFlag()){
                mElutionCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mElutionCkImg.setImageResource(R.drawable.check_normal);
            }
            if(hexConfig.isQuantifyFlag()){
                mQuantifyCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mQuantifyCkImg.setImageResource(R.drawable.check_normal);
            }
            if(hexConfig.isSecondQuantifyFlag()){
                mSecondCkImg.setImageResource(R.drawable.check_pressed);
            }else{
                mSecondCkImg.setImageResource(R.drawable.check_normal);
            }
            if(hexConfig.isWash1Flag()){
                mWash1CkImg.setImageResource(R.drawable.check_pressed);
            }else {
                mWash1CkImg.setImageResource(R.drawable.check_normal);
            }

            if(hexConfig.getWashStep() == 3){
                //三部洗涤
                mTwoWashLayout.setVisibility(View.VISIBLE);
                mThreeWashLayout.setVisibility(View.VISIBLE);
                mWashTemp_2Tv.setText(hexConfig.getWash32Temperature()+"");
                mWashVolume_2Tv.setText(hexConfig.getWash32XiDiYeVolume()+"");
                mWashTime_2Tv.setText(hexConfig.getWash32Time()+"");
                mWashTemp_3Tv.setText(hexConfig.getWash33Temperature()+"");
                mWashVolume_3Tv.setText(hexConfig.getWash33XiDiYeVolume()+"");
                mWashTime_3Tv.setText(hexConfig.getWash33Time()+"");
                if(hexConfig.isWash32Flag()){
                    mWash2CkImg.setImageResource(R.drawable.check_pressed);
                }else {
                    mWash2CkImg.setImageResource(R.drawable.check_normal);
                }
                if(hexConfig.isWash33Flag()){
                    mWash3CkImg.setImageResource(R.drawable.check_pressed);
                }else {
                    mWash3CkImg.setImageResource(R.drawable.check_normal);
                }
            }else if(hexConfig.getWashStep() == 2){

                mTwoWashLayout.setVisibility(View.VISIBLE);
                mThreeWashLayout.setVisibility(View.GONE);
                mWashTemp_2Tv.setText(hexConfig.getWash22Temperature()+"");
                mWashVolume_2Tv.setText(hexConfig.getWash22XiDiYeVolume()+"");
                mWashTime_2Tv.setText(hexConfig.getWash22Time()+"");

                if(hexConfig.isWash22Flag()){
                    mWash2CkImg.setImageResource(R.drawable.check_pressed);
                }else {
                    mWash2CkImg.setImageResource(R.drawable.check_normal);
                }

            }else if(hexConfig.getWashStep() == 1){
                mTwoWashLayout.setVisibility(View.GONE);
                mThreeWashLayout.setVisibility(View.GONE);

            }
            mElutionTempTv.setText(hexConfig.getElutionTemperature()+"");
            mElutionVolumeTv.setText(hexConfig.getElutionVolume()+"");
            mElutionTimeTv.setText(hexConfig.getElutionTime()+"");
            mQuantifyVolumeTv.setText(hexConfig.getQuantifyVolume()+"");
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.hex_create_save_btn){
            String templateName = mHexTitleEt.getText().toString().trim();
            String secondName = mSecondNameEt.getText().toString().trim();
            String thirdName = mThirdNameEt.getText().toString().trim();
            String totalTime = mTotalTimeEt.getText().toString().trim();
            if(!StringUtil.isEmpty(templateName)
                && !StringUtil.isEmpty(totalTime)){
                if(StringUtil.isEmpty(secondName) && StringUtil.isEmpty(thirdName)){
                    String name = templateName;
                    saveHexTempAction(name);
                }else{
                    String name = templateName + "~"+ secondName + "~" + thirdName;
                    saveHexTempAction(name);
                }

            }else{
                if(StringUtil.isEmpty(templateName)){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_multi_toast_template_name),Toast.LENGTH_LONG).show();
                }

                if(StringUtil.isEmpty(totalTime)){
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.create_single_rna_toast_time),Toast.LENGTH_LONG).show();
                }
            }
        }else if(id == R.id.hex_create_hex_create_tv){
            //跳转创建核酸提取文件
            Intent createHexIntent = new Intent(CreateSingleRNAActivity.this, ConfigHexActivity.class);
            startActivity(createHexIntent);
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
                        File dir = new File(CONFIG_SINGLERNA_TEMP_PATH, subFirName);
                        File[] files = dir.listFiles();//将file子目录及子文件放进文件数组
                        if (files != null) {//如果包含文件进行删除操作
                            for (int i = 0; i < files.length; i++) {
                                if (files[i].isFile()) {//删除子文件
                                    files[i].delete();
                                }
                            }
                        }
                        saveHexTemplate(subFirName);
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
