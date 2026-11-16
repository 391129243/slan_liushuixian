package com.gidi.bio_pcr.config.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.ConfigFileListAdapter;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.ConfigManager;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.gidi.bio_pcr.view.NoticeDialog;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 显示配置中所有的构建的模板列表
 */
public class ShowConfigActivity extends BaseActivity implements View.OnClickListener{
    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    private ListView configTempLv;
    private ListView exportTempLv;
    private ConfigFileListAdapter configAdapter,exportAdapter;
    private List<File> configList,exportList;
    private TextView createMultiTv,createSingleTv,exportFormalTv,mEditTemplateTv;
    private TextView deleteTempTv;
    private TextView configBackTv,exportedBackTv;
    private String configCurrentPath="";//已配置的路径
    private String exportCurrentPath ="";//已导出的当前路径
    private NoticeDialog exportDialog;
    private CustomDialog mExportDialog;//容量
    private UIHandler mUIHandler;
    private static final int EXPORT_TEMLPATE_SUCESS = 400;
    private static final int UPDATE_EXPORT_PROGRESS = 401;

    private RelativeLayout mRightHexLayout,mRightFullProceeLayout,mRightPcrLayout;


    private TextView createHexTv,editHexTv,deleteHexTv,exportHexTv;//创建核酸提取模板
    private TextView mConfigHexBackTv,mExportBackHexTv;
    private LinearLayout mFullProcessTabTv,mHexTabTv,mPcrTabTv;
    private ImageView mHexTabImg,mFullPrcTabImg,mPcrTabImg;

    private ListView mConfigHexLv,mExportedHexLv;
    private List<File> configHexList,exportHexList;
    private ConfigFileListAdapter configHexAdapter,exportHexAdapter;
    private String curConfigHexPath,exportCurHexPath = "";
    private int configHexPos = -1;//配置的核酸提取
    private int exportHexPos = -1;//导出的核酸提取

    //pcr列表
    private TextView mCreatePcrTv,mEditPcrTv,mDeletePcrTv,mExportPcrTv;//创建singlePCR模板
    private TextView mBackConfigPcrTv,mBackExportPcrTv;
    private ListView mConfigPcrLv,mExportedPcrLv;
    private List<File> configPcrList,exportPcrList;
    private ConfigFileListAdapter configPcrAdapter,exportPcrAdapter;
    private String curConfigPcrPath,curExportPcrPath = "";
    private int configPcrPos = -1;//配置的核酸提取
    private int exportPcrPos = -1;//导出的核酸提取
    private static final int COPY_FAIL = 402;
    private boolean isStartCopy = false;
    private int result = 0;
    private int click_export_pos = -1;
    private int click_fomal_pos = -1;//正式的模板
    private int copyCount = 0;
    private boolean isCopyMultiTemplate = false;
    private int copyType = -1;//0:单重，1：多重，2：single hex 3:：single pcr
    private String CONFIG_FULLPROCESS_TEMPLATE_PATH = FileUtils.CONFIG_TEMPLATE_PATH + "fullProcess" ;
    private String CONFIG_HEX_TEMPLATE_PATH = FileUtils.CONFIG_TEMPLATE_PATH + "singleRNA" ;
    private String CONFIG_PCR_TEMPLATE_PATH = FileUtils.CONFIG_TEMPLATE_PATH + "singlePCR" ;
    private String EXPORT_HEX_TEMPLATE_PATH = FileUtils.SINGLE_RNA_TEMPLATE_PATH;
    private String EXPORT_PCR_TEMPLATE_PATH = FileUtils.SINGLE_PCR_TEMPLATE_PATH;
    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_show_configtemplate;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTitleLayout = (RelativeLayout)findViewById(R.id.show_config_template_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(R.string.setting_procedure_configuration);
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        configTempLv = (ListView) findViewById(R.id.show_config_template_list);
        exportTempLv = (ListView) findViewById(R.id.show_export_template_list);
        createMultiTv = (TextView)findViewById(R.id.show_create_multi_template_tv) ;
        createSingleTv = (TextView)findViewById(R.id.show_create_single_template_tv) ;
        createHexTv = (TextView)findViewById(R.id.show_create_hex_template_tv);
        configBackTv = (TextView)findViewById(R.id.show_config_temp_back_tv) ;
        exportedBackTv = (TextView)findViewById(R.id.show_config_exported_back_tv) ;
        //导出临时文件
        exportFormalTv = (TextView)findViewById(R.id.show_export_formal_template_tv);
        mEditTemplateTv = (TextView) findViewById(R.id.show_config_edit_template_tv);
        deleteTempTv = (TextView)findViewById(R.id.show_config_edit_delete_tv);
        configAdapter = new ConfigFileListAdapter(getApplicationContext());
        exportAdapter = new ConfigFileListAdapter(getApplicationContext());
        configTempLv.setAdapter(configAdapter);
        exportTempLv.setAdapter(exportAdapter);

        mRightHexLayout = (RelativeLayout)findViewById(R.id.show_config_rna_layout);
        mRightFullProceeLayout = (RelativeLayout)findViewById(R.id.show_config_full_procee_layout);
        mRightPcrLayout = (RelativeLayout)findViewById(R.id.show_config_pcr_layout);

        //核酸提取配置
        editHexTv = (TextView)findViewById(R.id.show_config_edit_hex_template_tv);
        deleteHexTv = (TextView)findViewById(R.id.show_config_delete_hex_tv);
        exportHexTv = (TextView)findViewById(R.id.show_config_export_hex_template_tv);
        mConfigHexBackTv = (TextView)findViewById(R.id.show_config_hex_back_tv);
        mExportBackHexTv = (TextView)findViewById(R.id.show_config_exported_hex_back_tv);
        mConfigHexLv = (ListView) findViewById(R.id.show_config_hex_template_lv);
        mExportedHexLv = (ListView) findViewById(R.id.show_export_hex_lv);
        configHexAdapter = new ConfigFileListAdapter(getApplicationContext());
        exportHexAdapter = new ConfigFileListAdapter(getApplicationContext());
        mConfigHexLv.setAdapter(configHexAdapter);
        mExportedHexLv.setAdapter(exportHexAdapter);


        mCreatePcrTv = (TextView)findViewById(R.id.show_create_pcr_template_tv) ;
        mEditPcrTv= (TextView)findViewById(R.id.show_config_edit_pcr_template_tv) ;
        mDeletePcrTv= (TextView)findViewById(R.id.show_config_delete_pcr_tv) ;
        mExportPcrTv = (TextView) findViewById(R.id.show_config_export_pcr_template_tv);
        mBackConfigPcrTv = (TextView)findViewById(R.id.show_config_pcr_back_tv) ;
        mBackExportPcrTv = (TextView)findViewById(R.id.show_export_pcr_back_tv) ;
        mConfigPcrLv = (ListView) findViewById(R.id.show_config_pcr_template_lv);
        mExportedPcrLv = (ListView) findViewById(R.id.show_export_pcr_lv);
        configPcrAdapter = new ConfigFileListAdapter(getApplicationContext());
        exportPcrAdapter = new ConfigFileListAdapter(getApplicationContext());
        mConfigPcrLv.setAdapter(configPcrAdapter);
        mExportedPcrLv.setAdapter(exportPcrAdapter);

        mFullProcessTabTv = (LinearLayout) findViewById(R.id.show_config_tab_full_process_tv);
        mHexTabTv = (LinearLayout) findViewById(R.id.show_config_tab_hex_tv);
        mPcrTabTv = (LinearLayout) findViewById(R.id.show_config_tab_pcr_tv);
        mPcrTabImg = (ImageView) findViewById(R.id.config_tab_pcr_img);
        mHexTabImg  = (ImageView) findViewById(R.id.config_tab_hex_img);
        mFullPrcTabImg = (ImageView) findViewById(R.id.config_tab_fullprocee_img);

    }

    @Override
    protected void initListener() {
        mFullProcessTabTv.setOnClickListener(this);
        mHexTabTv.setOnClickListener(this);
        mPcrTabTv.setOnClickListener(this);
        exportFormalTv.setOnClickListener(this);
        createMultiTv.setOnClickListener(this);
        createSingleTv.setOnClickListener(this);
        configBackTv.setOnClickListener(this);
        exportedBackTv.setOnClickListener(this);
        mEditTemplateTv.setOnClickListener(this);
        deleteTempTv.setOnClickListener(this);
        createHexTv.setOnClickListener(this);
        editHexTv.setOnClickListener(this);
        deleteHexTv.setOnClickListener(this);
        exportHexTv.setOnClickListener(this);
        mConfigHexBackTv.setOnClickListener(this);
        mExportBackHexTv.setOnClickListener(this);

        mCreatePcrTv.setOnClickListener(this);
        mEditPcrTv.setOnClickListener(this);
        mDeletePcrTv.setOnClickListener(this);
        mExportPcrTv.setOnClickListener(this);
        mBackConfigPcrTv.setOnClickListener(this);
        mBackExportPcrTv.setOnClickListener(this);

        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        configTempLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //打开看下一层文件
                if(configCurrentPath.equals(CONFIG_FULLPROCESS_TEMPLATE_PATH)){
                    click_export_pos = position;
                    configAdapter.setSelectedPosition(click_export_pos);
                    click_fomal_pos = -1;
                    exportAdapter.setSelectedPosition(click_fomal_pos);
                }
            }
        });

        configTempLv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(configCurrentPath.equals(CONFIG_FULLPROCESS_TEMPLATE_PATH)){
                    openConfigFile(configList.get(i));
                    click_export_pos = -1;
                }
                return true;
            }
        });
        exportTempLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //查看已经导出的模板列表
                if(exportCurrentPath.equals(FileUtils.FULL_PROCESS_TEMPLATE_PATH)){
                    click_fomal_pos = i;
                    exportAdapter.setSelectedPosition(click_fomal_pos);
                    click_export_pos = -1;
                    configAdapter.setSelectedPosition(click_export_pos);
                }
            }
        });
        exportTempLv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(exportCurrentPath.equals(FileUtils.FULL_PROCESS_TEMPLATE_PATH)){
                    openExportedFile(exportList.get(i));
                    click_fomal_pos= -1;
                }
                return true;
            }
        });

        //核酸提取列表
        mConfigHexLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //打开看下一层文件
                if(curConfigHexPath.equals(CONFIG_HEX_TEMPLATE_PATH)){
                    configHexPos = position;
                    configHexAdapter.setSelectedPosition(configHexPos);
                    exportHexPos = -1;
                    exportHexAdapter.setSelectedPosition(exportHexPos);
                }
            }
        });

        //长按显示详情
        mConfigHexLv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(curConfigHexPath.equals(CONFIG_HEX_TEMPLATE_PATH)){
                    openConfigHexFile(configHexList.get(i));
                    configHexPos = -1;
                }
                return true;
            }
        });
        mExportedHexLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //查看已经导出的模板列表
                //  openExportedFile(exportList.get(i));
                if(exportCurHexPath.equals(EXPORT_HEX_TEMPLATE_PATH)){
                    exportHexPos = i;
                    exportHexAdapter.setSelectedPosition(exportHexPos);
                    configHexPos = -1;
                    configHexAdapter.setSelectedPosition(configHexPos);
                }
            }
        });
        mExportedHexLv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(exportCurHexPath.equals(EXPORT_HEX_TEMPLATE_PATH)){
                    openExportedHexFile(exportHexList.get(i));
                    exportHexPos= -1;
                }
                return true;
            }
        });


        ///pcr
        mConfigPcrLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //打开看下一层文件
                if(curConfigPcrPath.equals(CONFIG_PCR_TEMPLATE_PATH)){
                    configPcrPos = position;
                    configPcrAdapter.setSelectedPosition(configPcrPos);
                    exportPcrPos = -1;
                    exportPcrAdapter.setSelectedPosition(exportPcrPos);
                }
            }
        });

        //长按显示详情
        mConfigPcrLv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(curConfigPcrPath.equals(CONFIG_PCR_TEMPLATE_PATH)){
                    openConfigSinglePCRFile(configPcrList.get(i));
                    configPcrPos = -1;
                }
                return true;
            }
        });
        mExportedPcrLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //查看已经导出的模板列表
                //  openExportedFile(exportList.get(i));
             
                if(curExportPcrPath.equals(EXPORT_PCR_TEMPLATE_PATH)){
                    exportPcrPos = i;
                    exportPcrAdapter.setSelectedPosition(exportPcrPos);
                    configPcrPos = -1;
                    configPcrAdapter.setSelectedPosition(configPcrPos);
                }


            }
        });
        mExportedPcrLv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(curExportPcrPath.equals(EXPORT_PCR_TEMPLATE_PATH)){
                    openExportSinglePCRFile(exportPcrList.get(i));
                    exportPcrPos= -1;
                }
                return true;
            }
        });
    }

    @Override
    protected void initData() {
        mUIHandler = new UIHandler(this);
        configCurrentPath = FileUtils.CONFIG_TEMPLATE_PATH + "fullProcess";
        exportCurrentPath = FileUtils.FULL_PROCESS_TEMPLATE_PATH ;
        configList = new ArrayList<File>();
        exportList = new ArrayList<File>();
        File fullProcessTempDir = new File(configCurrentPath);
        if(!fullProcessTempDir.exists()){
            fullProcessTempDir.mkdirs();
        }

        configHexList = new ArrayList<File>();
        exportHexList = new ArrayList<File>();
        curConfigHexPath = CONFIG_HEX_TEMPLATE_PATH;
        exportCurHexPath = EXPORT_HEX_TEMPLATE_PATH ;
        File configHexTemplateDir = new File(curConfigHexPath);
        if(!configHexTemplateDir.exists()){
            configHexTemplateDir.mkdirs();
        }

        configPcrList = new ArrayList<File>();
        exportPcrList = new ArrayList<File>();
        curConfigPcrPath = CONFIG_PCR_TEMPLATE_PATH;
        curExportPcrPath = EXPORT_PCR_TEMPLATE_PATH ;
        File configPcrTempDir = new File(curConfigPcrPath);
        if(!configPcrTempDir.exists()){
            configPcrTempDir.mkdirs();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //更新
        initConfigFiles();
        initExportFiles();

        initConfigHexFiles();
        initExportHexFiles();

        initConfigPcrFiles();
        initExportPcrFiles();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(null != configList){
            configList.clear();
            configList = null;
        }
        if(null != exportList){
            exportList.clear();
            exportList = null;
        }
        if(null != mExportDialog){
            mExportDialog.dismiss();
            mExportDialog = null;
        }
        if(null != exportDialog){
            exportDialog.dismiss();
            exportDialog = null;
        }
        if(null != mUIHandler){
            mUIHandler.removeCallbacksAndMessages(null);
            mUIHandler = null;
        }
        if(null != configHexList){
            configHexList.clear();
            configHexList = null;
        }
        if(null != exportHexList){
            exportHexList.clear();
            exportHexList = null;
        }

        if(null != configPcrList){
            configPcrList.clear();
            configPcrList = null;
        }
        if(null != exportPcrList){
            exportPcrList.clear();
            exportPcrList = null;
        }
    }



    private void initConfigFiles(){
        configList.clear();
        File[] files = new File(CONFIG_FULLPROCESS_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != configList) {
                    configList.add(files[i]);
                    Collections.sort(configList);
                }
            }
        }
        configAdapter.setFileList(configList);
        configAdapter.notifyDataSetChanged();
    }

    private void initExportFiles(){
        exportList.clear();
        File[] files = new File(FileUtils.FULL_PROCESS_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != exportList) {
                    exportList.add(files[i]);
                    Collections.sort(exportList);
                }
            }
        }
        exportAdapter.setFileList(exportList);
        exportAdapter.notifyDataSetChanged();
    }


    //初始化核酸提取文件
    private void initConfigHexFiles(){
        if(configHexList != null){
            configHexList.clear();
            File[] files = new File(CONFIG_HEX_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
            if(files != null && files.length>0) {
                for (int i = 0; i < files.length; i++) {
                    if (null != configHexList) {
                        configHexList.add(files[i]);
                        Collections.sort(configHexList);
                    }
                }
            }
            configHexAdapter.setFileList(configHexList);
            configHexAdapter.notifyDataSetChanged();
        }

    }

    private void initExportHexFiles(){
        if(exportHexList != null){
            exportHexList.clear();
            File[] files = new File(EXPORT_HEX_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
            if(files != null && files.length>0) {
                for (int i = 0; i < files.length; i++) {
                    if (null != exportHexList) {
                        exportHexList.add(files[i]);
                        Collections.sort(exportHexList);
                    }
                }
            }
            exportHexAdapter.setFileList(exportHexList);
            exportHexAdapter.notifyDataSetChanged();
        }
    }

    private void initConfigPcrFiles(){
        if(configPcrList != null){
            configPcrList.clear();
            File[] files = new File(CONFIG_PCR_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
            Log.i("AAA","files" + files.length);
            if(files != null && files.length>0) {
                for (int i = 0; i < files.length; i++) {
                    if (null != configPcrList) {
                        configPcrList.add(files[i]);
                        Collections.sort(configPcrList);
                    }
                }
            }
            configPcrAdapter.setFileList(configPcrList);
            configPcrAdapter.notifyDataSetChanged();
        }

    }

    private void initExportPcrFiles(){
        if(exportPcrList != null){
            exportPcrList.clear();
            File[] files = new File(EXPORT_PCR_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
            if(files != null && files.length>0) {
                for (int i = 0; i < files.length; i++) {
                    if (null != exportPcrList) {
                        exportPcrList.add(files[i]);
                        Collections.sort(exportPcrList);
                    }
                }
            }
            exportPcrAdapter.setFileList(exportPcrList);
            exportPcrAdapter.notifyDataSetChanged();
        }
    }
    private void openConfigFile(File file){
        if (file.isDirectory()) {
            //文件夹更新列表
            configList.clear();
            Collections.addAll(configList, file.listFiles());
            configAdapter.notifyDataSetChanged();
            configCurrentPath = file.getAbsolutePath();
        }
    }


    /**
     * 打开已导出的模板列表
     * @param file
     */
    private void openExportedFile(File file){
        if (file.isDirectory()) {
            //文件夹更新列表
            exportList.clear();
            Collections.addAll(exportList, file.listFiles());
            exportAdapter.notifyDataSetChanged();
            exportCurrentPath = file.getAbsolutePath();

        }
    }

    /**
     * 删除模板
     */
    private void deleteTemplateFiles(File file){
       String path = CONFIG_FULLPROCESS_TEMPLATE_PATH + File.separator+ file.getName() + File.separator;
       boolean result = FileUtils.deleteDirectory(path);
        if(result){
            //删除已配置模板成功
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_config_template_success),Toast.LENGTH_LONG).show();
        }else {
            //删除已配置模板失败
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_config_template_fail),Toast.LENGTH_LONG).show();
        }
        initConfigFiles();
        click_export_pos = -1;
        configAdapter.setSelectedPosition(click_export_pos);
    }

    private void deleteExportedFiles(File file){
        String path = FileUtils.FULL_PROCESS_TEMPLATE_PATH+File.separator+ file.getName() + File.separator;
        boolean result = FileUtils.deleteDirectory(path);
        if(result){
            //删除已导出模板成功
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_exported_template_success),Toast.LENGTH_LONG).show();
        }else {
            //删除已导出模板失败
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_exported_template_fail),Toast.LENGTH_LONG).show();
        }
        initExportFiles();
        click_fomal_pos = -1;
        exportAdapter.setSelectedPosition(click_fomal_pos);
    }

    private void exportFormalTemplate(String subDirName){
        File file = configList.get(click_export_pos);

        // 使用新的方法获取下一个可用序号
        int nextSerialNum = getNextAvailableSerialNumber(FileUtils.FULL_PROCESS_TEMPLATE_PATH);
        String exportIndex = formatSerialNumber(nextSerialNum);

        String localTempPath = CONFIG_FULLPROCESS_TEMPLATE_PATH + File.separator+ file.getName()+File.separator;
        //找到文件夹中的所有文件
        List<File> exportFiles = getConfigTemplateFiles(localTempPath);
        if(null != exportFiles && exportFiles.size()>0){
            isCopyMultiTemplate = isHasSecFile(localTempPath);//单重模板
            if(!isCopyMultiTemplate){
                if(null != exportFiles && exportFiles.size()>0){
                    if(exportFiles.size()>=3 && exportFiles.size()<=11 && !isCopyMultiTemplate){
                        String exportTempDirPath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator
                                + exportIndex +"-"+subDirName + File.separator;//创建导出后的文件夹
                        File exportTempDir = new File(exportTempDirPath);
                        if(!exportTempDir.exists()) {
                            exportTempDir.mkdirs();
                        }
                        copySingleTemplate(exportFiles,exportTempDirPath);
                    }else{
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.show_config_toast_copy_tempate_incompleted),Toast.LENGTH_LONG).show();
                        return;
                    }
                }

            }else{
                if(null != exportFiles && exportFiles.size()>0){
                    if(exportFiles.size() >=5 && isCopyMultiTemplate){
                        String exportTempDirPath = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator
                                + exportIndex +"-"+ subDirName + File.separator;//创建导出后的文件夹
                        File exportTempDir = new File(exportTempDirPath);
                        if(!exportTempDir.exists()) {
                            exportTempDir.mkdirs();
                        }
                        copyMultiemplate(exportFiles,exportTempDirPath);
                    }else{
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.show_config_toast_copy_tempate_incompleted),Toast.LENGTH_LONG).show();
                    }
                }

            }
        }else{
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.show_config_toast_copy_not_file),Toast.LENGTH_LONG).show();
        }
    }


    /**
     * 启动线程进行复制更新进度
     * @param exportFiles 需要被复制的文件夹
     * @param exportTempDirPath
     */

    private void copyMultiemplate(List<File> exportFiles,String exportTempDirPath){
        showLoadingDialog(getResources().getString(R.string.show_config_dialog_coying_template));
        copyType = 1;
        copyCount = exportFiles.size();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!isStartCopy){
                    isStartCopy = true;
                }
                for (File configFile : exportFiles) {
                    if(configFile.getName().startsWith("1-") && configFile.getName().endsWith(".hex")){
                        if(isStartCopy){
                            copyFileNoThread(configFile,exportTempDirPath,"first.hex");
                        }
                    }else if(configFile.getName().startsWith("2-") && configFile.getName().endsWith(".hex")){
                        if(isStartCopy){
                            copyFileNoThread(configFile,exportTempDirPath,"second.hex");
                        }
                    }else if(configFile.getName().startsWith("1-") && configFile.getName().endsWith(".prj")){
                        if(isStartCopy){
                            copyFileNoThread(configFile,exportTempDirPath,"first.prj");
                        }
                    }else if(configFile.getName().startsWith("2-")&&configFile.getName().endsWith(".prj")){
                        if(isStartCopy){
                            copyFileNoThread(configFile,exportTempDirPath,"second.prj");
                        }
                    }else if (configFile.getName().endsWith(".peak")) {
                        if(isStartCopy){
                            if(configFile.getName().startsWith("hole1")){
                                copyFileNoThread(configFile, exportTempDirPath, "hole1.peak");
                            }else if(configFile.getName().startsWith("hole2")){
                                copyFileNoThread(configFile, exportTempDirPath, "hole2.peak");
                            }else if(configFile.getName().startsWith("hole3")){
                                copyFileNoThread(configFile, exportTempDirPath, "hole3.peak");
                            }else if(configFile.getName().startsWith("hole4")){
                                copyFileNoThread(configFile, exportTempDirPath, "hole4.peak");
                            }

                        }
                    }else if (configFile.getName().endsWith(".ct")) {
                        if(isStartCopy){
                            if(configFile.getName().startsWith("hole1")){
                                copyFileNoThread(configFile, exportTempDirPath, "hole1.ct");
                            }else if(configFile.getName().startsWith("hole2")){
                                copyFileNoThread(configFile, exportTempDirPath, "hole2.ct");
                            }else if(configFile.getName().startsWith("hole3")){
                                copyFileNoThread(configFile, exportTempDirPath, "hole3.ct");
                            }else if(configFile.getName().startsWith("hole4")){
                                copyFileNoThread(configFile, exportTempDirPath, "hole4.ct");
                            }

                        }
                    }else if (configFile.getName().equals("totaltime.txt")) {
                        if(isStartCopy){
                            copyFileNoThread(configFile, exportTempDirPath, "totaltime.txt");
                        }
                    }
                }
            }
        }).start();
    }



    private void copySingleTemplate(List<File> exportFiles,String exportTempDirPath){
        copyType = 0;
        showLoadingDialog(getResources().getString(R.string.show_config_dialog_coying_template));
        copyCount = exportFiles.size();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(null != exportFiles && exportFiles.size()>0) {
                    //单重
                    if(!isStartCopy){
                        isStartCopy = true;
                    }
                    for (File configFile : exportFiles) {
                        if(configFile.getName().endsWith(".hex")){
                            if(isStartCopy){
                                copyFileNoThread(configFile,exportTempDirPath,"first.hex");
                            }
                        }else if(configFile.getName().endsWith(".prj")){
                            if(isStartCopy){
                                copyFileNoThread(configFile,exportTempDirPath,"first.prj");
                            }
                        }else if (configFile.getName().endsWith(".ct")||configFile.getName().endsWith(".peak")) {
                            if(isStartCopy){
                                if(configFile.getName().endsWith(".ct")){
                                    if(configFile.getName().startsWith("hole1")){
                                        copyFileNoThread(configFile, exportTempDirPath, "hole1.ct");
                                    }else if(configFile.getName().startsWith("hole2")){
                                        copyFileNoThread(configFile, exportTempDirPath, "hole2.ct");
                                    }

                                }else if(configFile.getName().endsWith(".peak")){
                                    if(configFile.getName().startsWith("hole1")){
                                        copyFileNoThread(configFile, exportTempDirPath, "hole1.peak");
                                    }else if(configFile.getName().startsWith("hole2")){
                                        copyFileNoThread(configFile, exportTempDirPath, "hole2.peak");
                                    }
                                }
                            }
                        }else if (configFile.getName().equals("totaltime.txt")) {
                            if(isStartCopy){
                                copyFileNoThread(configFile, exportTempDirPath, "totaltime.txt");
                            }
                        }
                    }
                }
            }
        }).start();
     //   showProgressDialog("正在复制模板");
    }


    private void copyFileNoThread(File sourceFile,String toFileDirPath,String toFileName){
        final boolean result = ConfigManager.copyFile(sourceFile, toFileDirPath, toFileName, new ConfigManager.CopyProgressListener() {
            @Override
            public void copyProgress(int progress) {

                String text = "From " + sourceFile.getName()
                        + "\nTo export " + toFileDirPath+toFileName
                        + "\n Progress : " + progress;
                if(null != mUIHandler){
                    Message msg = Message.obtain();
                    msg.obj = text;
                    msg.what = UPDATE_EXPORT_PROGRESS;
                    mUIHandler.sendMessage(msg);
                }
            }
        });

        if(result){
            //更新复制后的列表
            mUIHandler.sendEmptyMessage(EXPORT_TEMLPATE_SUCESS);
        }else {
            if(null != mUIHandler){
                Message msg = Message.obtain();
                msg.obj = toFileDirPath;
                msg.what = COPY_FAIL;
                mUIHandler.sendMessage(msg);
            }

        }
    }


    private boolean isHasSecFile(String path){
        File[] files = new File(path).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if(files[i].getName().contains("V5-12多重二次试剂")){
                    return true;
                }

            }
        }
        return false;
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
    ////////////////////////////////////核酸提取模板/////////////////////////////////////////////////

    /**
     * 导出核酸提取模板
     */
    private void exportSingleRNATemplate(String subDirName){

        File file = configHexList.get(configHexPos);

        // 使用新的方法获取下一个可用序号
        int nextSerialNum = getNextAvailableSerialNumber(EXPORT_HEX_TEMPLATE_PATH);
        String exportIndex = formatSerialNumber(nextSerialNum);

        String localTempPath = CONFIG_HEX_TEMPLATE_PATH + File.separator + file.getName()+File.separator;
        //找到文件夹中的所有文件
        List<File> exportFiles = getConfigTemplateFiles(localTempPath);
        if(null != exportFiles && exportFiles.size()>0){
            String exportHexDirPath = EXPORT_HEX_TEMPLATE_PATH + File.separator+
                        exportIndex +"-"+ subDirName + File.separator;//创建导出后的文件夹
                File exportHexDir = new File(exportHexDirPath);
                if(!exportHexDir.exists()) {
                    exportHexDir.mkdirs();
                }
            copySingleRNATemplate(exportFiles,exportHexDirPath);
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_copy_not_file),Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 复制Hex模板文件
     * @param exportFiles 导出的模板文件
     * @param exportHexDirPath 导出后模板的路径
     */
    private void copySingleRNATemplate(List<File> exportFiles,String exportHexDirPath){
        showLoadingDialog(getResources().getString(R.string.show_config_dialog_coying_rna_template));
        copyType = 2;
        copyCount = exportFiles.size();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!isStartCopy){
                    isStartCopy = true;
                }
                for (File configFile : exportFiles) {
                    if(configFile.getName().endsWith(".hex")){
                        if(isStartCopy){
                            copyFileNoThread(configFile,exportHexDirPath,"first.hex");
                        }
                    }else if (configFile.getName().equals("totaltime.txt")) {
                        if(isStartCopy){
                            copyFileNoThread(configFile, exportHexDirPath, "totaltime.txt");
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 打开配置的核酸模板
     *
     */
    private void openConfigHexFile(File file){
        if (file.isDirectory()) {
            //文件夹更新列表
            configHexList.clear();
            Collections.addAll(configHexList, file.listFiles());
            configHexAdapter.notifyDataSetChanged();
            curConfigHexPath = file.getAbsolutePath();
        }
    }

    /**
     * 打开已导出的模板列表
     * @param file
     */
    private void openExportedHexFile(File file){
        if (file.isDirectory()) {
            //文件夹更新列表
            exportHexList.clear();
            Collections.addAll(exportHexList, file.listFiles());
            exportHexAdapter.notifyDataSetChanged();
            exportCurHexPath = file.getAbsolutePath();

        }
    }

    /**
     * 删除已配置的核酸提取模板
     *
     */
    private void deleteConfigSingleRNA(File file){
        String path = CONFIG_HEX_TEMPLATE_PATH + File.separator+ file.getName() + File.separator;
        boolean result = FileUtils.deleteDirectory(path);
        if(result){
            //删除已配置的核酸提取模板成功
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_config_rna_template_success),Toast.LENGTH_LONG).show();
        }else {
            //删除已配置的核酸提取模板失败
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_config_rna_template_fail),Toast.LENGTH_LONG).show();
        }
        initConfigHexFiles();
        configHexPos = -1;
        configHexAdapter.setSelectedPosition(configHexPos);
    }

    /**
     * 删除已导出的核酸提取模板
     * @param file
     */
    private void deleteExportSingleRNA(File file){
        String path = EXPORT_HEX_TEMPLATE_PATH + File.separator
                +file.getName() + File.separator;
        boolean result = FileUtils.deleteDirectory(path);
        if(result){
            //删除已导出核酸提取模板成功
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_exported_rna_template_success),Toast.LENGTH_LONG).show();
        }else {
            //删除已导出核酸提取模板失败
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_exported_rna_template_fail),Toast.LENGTH_LONG).show();
        }
        initExportHexFiles();
        exportHexPos = -1;
        exportHexAdapter.setSelectedPosition(exportHexPos);
    }

    /////////////////////////////////////////////////singlePCR模板/////////////////////////////////////////////////

    /**
     * 导出singlePCR模板
     * @param subDirName
     */
    private void exportSinglePCRTemplate(String subDirName){

        File file = configPcrList.get(configPcrPos);

        // 使用新的方法获取下一个可用序号
        int nextSerialNum = getNextAvailableSerialNumber(EXPORT_PCR_TEMPLATE_PATH);
        String exportIndex = formatSerialNumber(nextSerialNum);

        String localTempPath = CONFIG_PCR_TEMPLATE_PATH + File.separator + file.getName()+File.separator;
        //找到文件夹中的所有文件
        List<File> exportFiles = getConfigTemplateFiles(localTempPath);
        if(null != exportFiles && exportFiles.size()>0){
            String exportPcrDirPath = EXPORT_PCR_TEMPLATE_PATH + File.separator+
                    exportIndex +"-"+ subDirName + File.separator;//创建导出后的文件夹
            File exportHexDir = new File(exportPcrDirPath);
            if(!exportHexDir.exists()) {
                exportHexDir.mkdirs();
            }
            copySinglePcrTemplate(exportFiles,exportPcrDirPath);
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_copy_not_file),Toast.LENGTH_LONG).show();
        }
    }

    private void copySinglePcrTemplate(List<File>exportFiles,String exportPcrDirPath){
        showLoadingDialog(getResources().getString(R.string.show_config_dialog_coying_pcr_template));
        copyType = 3;
        copyCount = exportFiles.size();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!isStartCopy){
                    isStartCopy = true;
                }
                for (File configFile : exportFiles) {
                    if(configFile.getName().endsWith(".prj")){
                        if(isStartCopy){
                            copyFileNoThread(configFile,exportPcrDirPath,"first.prj");
                        }
                    }else if(configFile.getName().endsWith(".hex")){
                        if(isStartCopy){
                            copyFileNoThread(configFile,exportPcrDirPath,"first.hex");
                        }
                    }else if (configFile.getName().endsWith(".ct")||configFile.getName().endsWith(".peak")) {
                        if(isStartCopy){
                            if(configFile.getName().endsWith(".ct")){
                                if(configFile.getName().startsWith("hole1")){
                                    copyFileNoThread(configFile, exportPcrDirPath, "hole1.ct");
                                }else if(configFile.getName().startsWith("hole2")){
                                    copyFileNoThread(configFile, exportPcrDirPath, "hole2.ct");
                                }else if(configFile.getName().startsWith("hole3")){
                                    copyFileNoThread(configFile, exportPcrDirPath, "hole3.ct");
                                }else if(configFile.getName().startsWith("hole4")){
                                    copyFileNoThread(configFile, exportPcrDirPath, "hole4.ct");
                                }

                            }else if(configFile.getName().endsWith(".peak")){
                                if(configFile.getName().startsWith("hole1")){
                                    copyFileNoThread(configFile, exportPcrDirPath, "hole1.peak");
                                }else if(configFile.getName().startsWith("hole2")){
                                    copyFileNoThread(configFile, exportPcrDirPath, "hole2.peak");
                                }else if(configFile.getName().startsWith("hole3")){
                                    copyFileNoThread(configFile, exportPcrDirPath, "hole3.peak");
                                }else if(configFile.getName().startsWith("hole4")){
                                    copyFileNoThread(configFile, exportPcrDirPath, "hole4.peak");
                                }

                            }
                        }
                    }else if (configFile.getName().equals("totaltime.txt")) {
                        if(isStartCopy){
                            copyFileNoThread(configFile, exportPcrDirPath, "totaltime.txt");
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 删除已配置的PCR模板
     *
     */
    private void deleteConfigSinglePCR(File file){
        String path = CONFIG_PCR_TEMPLATE_PATH + File.separator+ file.getName() + File.separator;
        boolean result = FileUtils.deleteDirectory(path);
        if(result){
            //删除已配置PCR模板成功
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_config_pcr_template_success),Toast.LENGTH_LONG).show();
        }else {
            //删除已配置PCR模板失败
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_config_pcr_template_fail),Toast.LENGTH_LONG).show();
        }
        initConfigPcrFiles();
        configPcrPos = -1;
        configPcrAdapter.setSelectedPosition(configPcrPos);
    }

    /**
     * 删除已导出的PCR模板
     * @param file
     */
    private void deleteExportSinglePCR(File file){
        String path = EXPORT_PCR_TEMPLATE_PATH + File.separator
                +file.getName() + File.separator;
        boolean result = FileUtils.deleteDirectory(path);
        if(result){
            //删除已导出PCR模板成功
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_exported_pcr_template_success),Toast.LENGTH_LONG).show();
        }else {
            //删除已导出PCR模板失败
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.show_config_toast_delete_exported_pcr_template_fail),Toast.LENGTH_LONG).show();
        }
        initExportPcrFiles();
        exportPcrPos = -1;
        exportPcrAdapter.setSelectedPosition(exportPcrPos);
    }

    /**
     * 打开已导出的PCR模板列表
     * @param file
     */
    private void openExportSinglePCRFile(File file){
        if (file.isDirectory()) {
            //文件夹更新列表
            exportPcrList.clear();
            Collections.addAll(exportPcrList, file.listFiles());
            exportPcrAdapter.notifyDataSetChanged();
            curExportPcrPath = file.getAbsolutePath();
        }
    }


    /**
     * 打开配置的核酸模板
     *
     */
    private void openConfigSinglePCRFile(File file){
        if (file.isDirectory()) {
            //文件夹更新列表
            configPcrList.clear();
            Collections.addAll(configPcrList, file.listFiles());
            configPcrAdapter.notifyDataSetChanged();
            curConfigPcrPath = file.getAbsolutePath();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.show_create_single_template_tv){
            Intent single = new Intent(ShowConfigActivity.this,CreateSingleActivity.class);
            single.putExtra("isEdit",false);
            startActivity(single);
        }else if(id == R.id.show_create_multi_template_tv){
            Intent multi = new Intent(ShowConfigActivity.this,CreateMultiActivity.class);
            multi.putExtra("isEdit",false);
            startActivity(multi);
        }else if(id == R.id.show_config_temp_back_tv){
            //全流程本地目录的返回
            if (!configCurrentPath.equals(CONFIG_FULLPROCESS_TEMPLATE_PATH)){
                //不是根目录，返回上一目录
                openConfigFile(new File(configCurrentPath).getParentFile());

            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_config_root_directory),Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.show_config_exported_back_tv) {
            if (!exportCurrentPath.equals(FileUtils.FULL_PROCESS_TEMPLATE_PATH)) {
                //不是根目录，返回上一目录
                openExportedFile(new File(exportCurrentPath).getParentFile());

            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_exported_root_directory),Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.show_export_formal_template_tv) {
            //弹出对话框提示是否导出正式文件
            if (click_export_pos != -1) {
                File file = configList.get(click_export_pos);
                //导出临时文件
                // showExportDialog("是否导出正式模板", file, 1);//导出的目录
                showTemplateDialog(
                        getResources().getString(R.string.show_config_dialog_exported_template), file,0);//导出的目录
            } else {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_select_exported_template), Toast.LENGTH_LONG).show();
            }
        } else if (id == R.id.show_config_edit_template_tv) {
            if (click_export_pos != -1) {
                File file = configList.get(click_export_pos);
                //编辑模板
                String localTempPath = CONFIG_FULLPROCESS_TEMPLATE_PATH + File.separator
                        + file.getName() + File.separator;
                //找到文件夹中的所有文件
                List<File> exportFiles = getConfigTemplateFiles(localTempPath);
                boolean isHasSecFile = isHasSecFile(localTempPath);
                if (null != exportFiles && exportFiles.size() > 0) {
                    if (exportFiles.size() >=5 && isHasSecFile) {
                        Intent multi = new Intent(ShowConfigActivity.this, CreateMultiActivity.class);
                        multi.putExtra("isEdit", true);
                        multi.putExtra("templateName", file.getName());
                        startActivity(multi);

                    } else if(exportFiles.size()>=3 && exportFiles.size()<=11 && !isHasSecFile){
                        Intent single = new Intent(ShowConfigActivity.this, CreateSingleActivity.class);
                        single.putExtra("isEdit", true);
                        single.putExtra("templateName", file.getName());
                        startActivity(single);
                    } else {
                        Toast.makeText(getApplicationContext(), "模板内文件不全", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "请选择要编辑的文件", Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_edit_delete_tv){
            //删除模板
            if (click_export_pos != -1 || click_fomal_pos !=-1) {
                if(click_export_pos != -1){
                    File file = configList.get(click_export_pos);
                    //删除文件
                    showExportDialog("是否确定删除已配置的模板", file, 2);//导出的目录
                }else if(click_fomal_pos != -1){
                    File file = exportList.get(click_fomal_pos);
                    //删除文件
                    showExportDialog("是否确定删除已导出的模板", file, 3);//导出的目录
                }
            } else {
                Toast.makeText(getApplicationContext(), "请选择要删除的文件", Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_tab_full_process_tv){
            //显示全流程
            mRightHexLayout.setVisibility(View.GONE);
            mRightPcrLayout.setVisibility(View.GONE);
            mRightFullProceeLayout.setVisibility(View.VISIBLE);

            mFullProcessTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_press);
            mPcrTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_normal);
            mHexTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_normal);
            mFullPrcTabImg.setImageResource(R.drawable.icon_config_fullprocee_press);
            mHexTabImg.setImageResource(R.drawable.icon_config_rna_normal);
            mPcrTabImg.setImageResource(R.drawable.icon_config_pcr_normal);
        }else if(id == R.id.show_config_tab_hex_tv){
            //显示核酸配置
            mRightHexLayout.setVisibility(View.VISIBLE);
            mRightPcrLayout.setVisibility(View.GONE);
            mRightFullProceeLayout.setVisibility(View.GONE);
            mHexTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_press);
            mFullProcessTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_normal);
            mPcrTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_normal);
            mFullPrcTabImg.setImageResource(R.drawable.icon_config_fullprocee_normal);
            mHexTabImg.setImageResource(R.drawable.icon_config_rna_press);
            mPcrTabImg.setImageResource(R.drawable.icon_config_pcr_normal);
            if(null != configHexAdapter){
                configHexAdapter.setFileList(configHexList);
                configHexAdapter.notifyDataSetChanged();
            }

        }else if(id == R.id.show_config_tab_pcr_tv){
            mRightHexLayout.setVisibility(View.GONE);
            mRightPcrLayout.setVisibility(View.VISIBLE);
            mRightFullProceeLayout.setVisibility(View.GONE);
            mPcrTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_press);
            mFullProcessTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_normal);
            mHexTabTv.setBackgroundResource(R.drawable.bg_config_left_tab_normal);
            mFullPrcTabImg.setImageResource(R.drawable.icon_config_fullprocee_normal);
            mHexTabImg.setImageResource(R.drawable.icon_config_rna_normal);
            mPcrTabImg.setImageResource(R.drawable.icon_config_pcr_press);
            if(null != configPcrAdapter){
                configPcrAdapter.setFileList(configPcrList);
                configPcrAdapter.notifyDataSetChanged();
            }
        }else if(id == R.id.show_create_hex_template_tv){
            //创建核酸提取模板
            Intent multi = new Intent(ShowConfigActivity.this, CreateSingleRNAActivity.class);
            multi.putExtra("isEdit",false);
            startActivity(multi);
        }else if(id == R.id.show_config_edit_hex_template_tv){
            //载入核酸提取模板
            if (configHexPos != -1) {
                File file = configHexList.get(configHexPos);
                //编辑模板
                Intent editHexIntent = new Intent(ShowConfigActivity.this, CreateSingleRNAActivity.class);
                editHexIntent.putExtra("isEdit", true);
                editHexIntent.putExtra("templateName", file.getName());
                startActivity(editHexIntent);
            } else {
                //请选择要编辑的模板
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_select_edit_template), Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_delete_hex_tv){
            //删除核酸提取模板
            if (configHexPos != -1 || exportHexPos !=-1) {
                if(configHexPos != -1){
                    File file = configHexList.get(configHexPos);
                    //是否确定删除已配置的核酸提取模板
                    showExportDialog(
                            getResources().getString(R.string.show_config_dialog_delete_config_rna_template), file, 4);//配置的目录
                }else if(exportHexPos != -1){
                    File file = exportHexList.get(exportHexPos);
                    //删除文件是否确定删除已导出的核酸提取模板
                    showExportDialog(
                            getResources().getString(R.string.show_config_dialog_delete_exported_rna_template), file, 5);//导出的目录
                }
            } else {
                //请选择要删除的模板
                Toast.makeText(getApplicationContext(), getResources().getString(R.string.show_config_toast_select_delete_template), Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_export_hex_template_tv){
            //导出核酸提取模板
            if (configHexPos != -1 && configHexList.size()>0) {
                File file = configHexList.get(configHexPos);
                showTemplateDialog(getResources().getString(R.string.show_config_dialog_export_rna_template), file,1);//导出的目录
            } else {
                //请选择要导出的文件
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_select_exported_template), Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_exported_hex_back_tv){
            //导出核酸返回键
            if (!exportCurHexPath.equals(EXPORT_HEX_TEMPLATE_PATH)) {
                //不是根目录，返回上一目录
                openExportedHexFile(new File(exportCurHexPath).getParentFile());
            }else{
                //当前是已导出模板的根目录
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_exported_root_directory),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_hex_back_tv){
            //配置核酸返回键
            if (!curConfigHexPath.equals(CONFIG_HEX_TEMPLATE_PATH)) {
                //不是根目录，返回上一目录
                openConfigHexFile(new File(curConfigHexPath).getParentFile());
            }else{
                //当前是已配置模板的根目录
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_config_root_directory),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_create_pcr_template_tv){
            //创建singlePCR模板
            Intent createPCR = new Intent(ShowConfigActivity.this, CreateSinglePCRActivity.class);
            createPCR.putExtra("isEdit",false);
            startActivity(createPCR);
        }else if(id == R.id.show_config_edit_pcr_template_tv){
            //编辑singlePCR模板
            if (configPcrPos != -1) {
                File file = configPcrList.get(configPcrPos);
                //编辑模板
                Intent editPCR = new Intent(ShowConfigActivity.this, CreateSinglePCRActivity.class);
                editPCR.putExtra("isEdit", true);
                editPCR.putExtra("templateName", file.getName());
                startActivity(editPCR);
            } else {
                //请选择要编辑的PCR模板文件
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_select_edit_pcr_template), Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_delete_pcr_tv){
            if (configPcrPos != -1 || exportPcrPos !=-1) {
                if(configPcrPos != -1){
                    File file = configPcrList.get(configPcrPos);
                    //删除文件是否确定删除已配置的PCR模板
                    showExportDialog(
                            getResources().getString(R.string.show_config_dialog_delete_config_pcr_template), file, 6);//导出的目录
                }else if(exportPcrPos != -1){
                    File file = exportPcrList.get(exportPcrPos);
                    //删除文件是否确定删除已导出的PCR模板
                    showExportDialog(
                            getResources().getString(R.string.show_config_dialog_delete_exported_pcr_template), file, 7);//导出的目录
                }
            } else {
                //请选择要删除的模板
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_select_delete_template), Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_export_pcr_template_tv){
            //导出singlePCR模板
            if (configPcrPos != -1 && configPcrList.size()>0) {
                File file = configPcrList.get(configPcrPos);
                //是否导出正式的PCR模板
                showTemplateDialog(getResources().getString(R.string.show_config_dialog_export_pcr_template), file,2);//导出的目录
            } else {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_select_exported_template), Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_export_pcr_back_tv){
            if (!curExportPcrPath.equals(EXPORT_PCR_TEMPLATE_PATH)) {
                //不是根目录，返回上一目录
                openExportSinglePCRFile(new File(curExportPcrPath).getParentFile());
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_exported_root_directory),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.show_config_pcr_back_tv){
            //配置核酸返回键
            if (!curConfigPcrPath.equals(CONFIG_PCR_TEMPLATE_PATH)) {
                //不是根目录，返回上一目录
                openConfigSinglePCRFile(new File(curConfigPcrPath).getParentFile());
            }else{
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.show_config_toast_config_root_directory),Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showTemplateDialog(String message,File file,int dialogType){
        if(null != mExportDialog){
            mExportDialog.dismiss();
            mExportDialog = null;
        }
        if(null == mExportDialog){
            mExportDialog = new CustomDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_export_template_layout, null);
            EditText editText = (EditText) view.findViewById(R.id.dialog_export_temp_name_et);
            editText.addTextChangedListener(new LimitInputTextWatcher(editText));
            editText.setText(file.getName());
            CustomDialog.Builder builder = new CustomDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(message);
            builder.setPositiveButton(R.string.dialog_ok,  new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    String subDirName = editText.getText().toString();
                    if(StringUtil.isEmpty(subDirName)){
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.show_config_toast_template_name_empty),Toast.LENGTH_LONG).show();
                        return;
                    }
                    mExportDialog.dismiss();
                    switch (dialogType){
                        case 0:
                            exportFormalTemplate(subDirName);
                            break;
                        case 1:
                            //导出核酸提取模板
                            exportSingleRNATemplate(subDirName);
                            break;
                        case 2:
                            exportSinglePCRTemplate(subDirName);
                            break;
                        default:
                            break;
                    }

                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mExportDialog.dismiss();
                    mExportDialog = null;
                }
            });
            mExportDialog = builder.create();
            mExportDialog.show();
        }
    }


    private void showExportDialog(String message,final File file,int dialogType){
        if(null != exportDialog ){
            exportDialog.dismiss();
            exportDialog =null;
        }
        if(null == exportDialog){
            exportDialog =new NoticeDialog(this,R.style.Dialog);
            if(null != file){
                exportDialog.setMessage(message + file.getName());
            }else{
                exportDialog.setMessage(message);
            }
            exportDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    exportDialog.dismiss();
                    if(dialogType == 1){
                        //exportFormalFiles();
                    }else if(dialogType == 2){
                        deleteTemplateFiles(file);
                    }else if(dialogType == 3){
                        deleteExportedFiles(file);
                    }else if(dialogType == 4){
                        //删除配置的核酸提取模板
                        deleteConfigSingleRNA(file);
                    }else if(dialogType == 5){
                        //删除导出的核酸提取模板
                        deleteExportSingleRNA(file);
                    }else if(dialogType == 6){
                        //删除配置的核酸提取模板
                        deleteConfigSinglePCR(file);
                    }else if(dialogType == 7){
                        //删除导出的PCR模板
                        deleteExportSinglePCR(file);
                    }

                }
            });

            exportDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                @Override
                public void onCancelClick() {
                    exportDialog.dismiss();
                }
            });
            exportDialog.show();
        }
    }


    private static class UIHandler extends Handler {

        private final WeakReference<ShowConfigActivity> mActivity;

        public UIHandler(ShowConfigActivity activity) {
            mActivity = new WeakReference<ShowConfigActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            if (null == mActivity.get()) {
                return;
            }
            switch (msg.what) {
                case EXPORT_TEMLPATE_SUCESS:
                    switch (mActivity.get().copyType){
                        case 0:
                            //单重
                            if(!mActivity.get().isCopyMultiTemplate){
                                mActivity.get().result++;
                                if(mActivity.get().result == mActivity.get().copyCount){
                                    mActivity.get().showExportDialog(mActivity.get().getResources().getString(R.string.show_config_dialog_single_template_copy_success)
                                            + mActivity.get().copyCount + "项",null,-1);
                                   /* Toast.makeText(mActivity.get().getApplicationContext(),
                                            "单重模板复制成功",Toast.LENGTH_LONG).show();*/
                                    mActivity.get().initExportFiles();
                                    mActivity.get().hideLoadingDialog();
                                    mActivity.get().isStartCopy = false;
                                    mActivity.get().result = 0;
                                    mActivity.get().copyCount = 0;
                                    mActivity.get().click_export_pos = -1;
                                    mActivity.get().configAdapter.setSelectedPosition(mActivity.get().click_export_pos);
                                }
                            }
                            break;

                        case 1:
                            //多重
                            if(mActivity.get().isCopyMultiTemplate) {
                                mActivity.get().result++;
                                if (mActivity.get().result == mActivity.get().copyCount) {
                                    mActivity.get().showExportDialog(mActivity.get().getResources().getString(R.string.show_config_dialog_multi_template_copy_success)
                                            + mActivity.get().copyCount
                                            +mActivity.get().getResources().getString(R.string.result_export_dialog_message_items), null, -1);
                                   // Toast.makeText(mActivity.get().getApplicationContext(), "多重模板复制成功", Toast.LENGTH_LONG).show();
                                    mActivity.get().initExportFiles();
                                    mActivity.get().hideLoadingDialog();
                                    mActivity.get().isStartCopy = false;
                                    mActivity.get().result = 0;
                                    mActivity.get().copyCount = 0;
                                    mActivity.get().click_export_pos = -1;
                                    mActivity.get().copyType = -1;
                                    mActivity.get().configAdapter.setSelectedPosition(mActivity.get().click_export_pos);
                                }
                            }
                            break;
                        case 2:
                            //single hex
                            mActivity.get().result++;
                            if (mActivity.get().result == mActivity.get().copyCount) {
                                mActivity.get().showExportDialog(mActivity.get().getResources().getString(R.string.show_config_dialog_rna_template_copy_success)
                                        + mActivity.get().copyCount
                                        + mActivity.get().getResources().getString(R.string.result_export_dialog_message_items), null, -1);
                              //  Toast.makeText(mActivity.get().getApplicationContext(), "核酸提取模板复制成功", Toast.LENGTH_LONG).show();
                                mActivity.get().initExportHexFiles();
                                mActivity.get().hideLoadingDialog();
                                mActivity.get().isStartCopy = false;
                                mActivity.get().result = 0;
                                mActivity.get().copyCount = 0;
                                mActivity.get().configHexPos= -1;
                                mActivity.get().copyType = -1;
                                mActivity.get().configHexAdapter.setSelectedPosition(mActivity.get().configHexPos);
                            }
                            break;
                        case 3:
                            //single pcr
                            mActivity.get().result++;
                            if (mActivity.get().result == mActivity.get().copyCount) {
                                mActivity.get().showExportDialog(mActivity.get().getResources().getString(R.string.show_config_dialog_pcr_template_copy_success)
                                        + mActivity.get().copyCount
                                        + mActivity.get().getResources().getString(R.string.result_export_dialog_message_items), null, -1);
                                //Toast.makeText(mActivity.get().getApplicationContext(), "PCR模板复制成功", Toast.LENGTH_LONG).show();
                                mActivity.get().initExportPcrFiles();
                                mActivity.get().hideLoadingDialog();
                                mActivity.get().isStartCopy = false;
                                mActivity.get().result = 0;
                                mActivity.get().copyCount = 0;
                                mActivity.get().configPcrPos= -1;
                                mActivity.get().copyType = -1;
                                mActivity.get().configPcrAdapter.setSelectedPosition(mActivity.get().configPcrPos);
                            }
                            break;
                    }

                    break;

                case UPDATE_EXPORT_PROGRESS:
                    String progress = (String) msg.obj;
                    break;
                case COPY_FAIL:
                    switch (mActivity.get().copyType){
                        case 0:
                        case 1:
                            //单重
                            mActivity.get().isStartCopy = false;
                            mActivity.get().result = 0;
                            mActivity.get().copyCount = 0;
                            mActivity.get().copyType = -1;
                            String text = (String) msg.obj;
                            FileUtils.deleteDirectory(text);
                            mActivity.get().hideLoadingDialog();
                            mActivity.get().showExportDialog(mActivity.get().getResources().getString(R.string.show_config_dialog_copy_fail)
                                    + mActivity.get().copyCount
                                    + mActivity.get().getResources().getString(R.string.result_export_dialog_message_items),null,-1);
                           // Toast.makeText(mActivity.get().getApplicationContext(),"复制失败",Toast.LENGTH_LONG).show();
                            mActivity.get().click_export_pos = -1;
                            mActivity.get().exportAdapter.setSelectedPosition(mActivity.get().click_export_pos);
                            break;

                        case 2:
                            //single hex
                            mActivity.get().isStartCopy = false;
                            mActivity.get().result = 0;
                            mActivity.get().copyCount = 0;
                            mActivity.get().copyType = -1;
                            String text2 = (String) msg.obj;
                            FileUtils.deleteDirectory(text2);
                            mActivity.get().hideLoadingDialog();
                            mActivity.get().showExportDialog(mActivity.get().getResources().getString(R.string.show_config_dialog_copy_fail)
                                    + mActivity.get().copyCount
                                    +  mActivity.get().getResources().getString(R.string.result_export_dialog_message_items),null,-1);
                            //Toast.makeText(mActivity.get().getApplicationContext(),"复制失败",Toast.LENGTH_LONG).show();
                            mActivity.get().configHexPos = -1;
                            mActivity.get().exportHexAdapter.setSelectedPosition(mActivity.get().exportHexPos);
                            break;

                        case 3:
                            //single pcr
                            mActivity.get().isStartCopy = false;
                            mActivity.get().result = 0;
                            mActivity.get().copyCount = 0;
                            mActivity.get().copyType = -1;
                            String text3 = (String) msg.obj;
                            FileUtils.deleteDirectory(text3);
                            mActivity.get().hideLoadingDialog();
                            mActivity.get().showExportDialog(mActivity.get().getResources().getString(R.string.show_config_dialog_copy_fail)
                                    + mActivity.get().copyCount
                                    +  mActivity.get().getResources().getString(R.string.result_export_dialog_message_items),null,-1);
                           // Toast.makeText(mActivity.get().getApplicationContext(),"复制失败",Toast.LENGTH_LONG).show();
                            mActivity.get().exportPcrPos = -1;
                            mActivity.get().exportPcrAdapter.setSelectedPosition(mActivity.get().exportPcrPos);
                            break;
                    }
                    break;
            }
        }
    }

    //===========================工具方法=====================================
    /**
     * 获取指定目录下文件夹的最大序号
     * @param dirPath 目录路径
     * @return 最大序号（如果没有任何文件夹返回0）
     */
    private int getMaxSerialNumber(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return 0;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return 0;
        }

        int maxNum = 0;
        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                // 匹配格式：数字-名称 或 数字-名称
                // 例如：01-模板名称
                String[] parts = name.split("-");
                if (parts.length > 0) {
                    try {
                        int num = Integer.parseInt(parts[0]);
                        if (num > maxNum) {
                            maxNum = num;
                        }
                    } catch (NumberFormatException e) {
                        // 忽略不是数字开头的文件夹
                    }
                }
            }
        }
        return maxNum;
    }

    /**
     * 获取下一个可用的序号（自动补间隙）
     * @param dirPath 目录路径
     * @return 下一个可用序号
     */
    private int getNextAvailableSerialNumber(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            return 1;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return 1;
        }

        // 收集所有已存在的序号
        Set<Integer> existingNumbers = new HashSet<>();
        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                String[] parts = name.split("-");
                if (parts.length > 0) {
                    try {
                        int num = Integer.parseInt(parts[0]);
                        existingNumbers.add(num);
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }
            }
        }

        // 从1开始查找最小可用序号
        int nextNum = 1;
        while (existingNumbers.contains(nextNum)) {
            nextNum++;
        }
        return nextNum;
    }

    /**
     * 格式化序号（个位数补0）
     * @param num 序号
     * @return 格式化后的字符串
     */
    private String formatSerialNumber(int num) {
        if (num < 10) {
            return "0" + num;
        } else {
            return String.valueOf(num);
        }
    }
}
