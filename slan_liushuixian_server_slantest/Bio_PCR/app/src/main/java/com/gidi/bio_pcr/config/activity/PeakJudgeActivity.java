package com.gidi.bio_pcr.config.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKRuleEntity;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKSingleRuleEntity;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.adapter.FileListAdapter;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.adapter.JudgePeakAdapter;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomLargeDialog;
import com.gidi.bio_pcr.view.LimitInputTextWatcher;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.NumKeyboardDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 判别文件
 * 根据输入的判别类型显示是单重的判别还是多重判别
 */
public class PeakJudgeActivity extends BaseActivity implements View.OnClickListener{

    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    private EditText mFileNameEt;

    //左边的文件列表
    private ArrayList<File> showPeakFileList;
    private ListView mShowPeakJudgeLv;
    private FileListAdapter<File> showPeakAdapter;
    private TextView mFileDelTv,mFileEditTv;//ct文件删除编辑


    private LinearLayoutCompat peakJudgeTitle;
    private List<PEAKRuleEntity> peakRuleEntities;//多重判断列表
    private RecyclerView multiRuleRv;//多重判别列表
    private JudgePeakAdapter judgePeakAdapter;
    private TextView mAddPeakTv,mDelPeakTv,mSavePeakTv;
    /////通道
    private List<String> channelList = new ArrayList<String>();
    private CustomSpinnerAdapter channelAdapter;
    private PopupWindow mChannelPopWindow;
    private ListView mChannelLv;
    private boolean isChannelExpanded = false;

    ////////////////////////////////////
    private List<String> mCategoryList = new ArrayList<String>();//种类列表
    private CustomSpinnerAdapter spinnerAdapter;
    private ListView mCategoryLv;
    private EditText mCategoryEt;
    ////////////
    private int peak_header_pos = -1;
    private int peak_item_pos = -1;
    private int peak_file_pos = -1;//选择编辑或删除的peak文件的pos
    private NoticeDialog noticeDialog;
    private NumKeyboardDialog numKeyboardDialog;
    private CustomLargeDialog categoryDialog;
    private boolean isHeader = false;
    private String JUDGE_PATH = FileUtils.CONFIG_PATH + File.separator + "judge"
            +File.separator;
    private Gson gson;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_config_judge;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTitleLayout = (RelativeLayout)findViewById(R.id.judge_peak_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.judge_ct_title));
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mFileNameEt = (EditText)findViewById(R.id.multi_judge_filename_tv);
        mFileNameEt.addTextChangedListener(new LimitInputTextWatcher(mFileNameEt));
        mShowPeakJudgeLv = (ListView) findViewById(R.id.judge_peak_show_list);//显示的列表
        peakJudgeTitle = (LinearLayoutCompat)findViewById(R.id.create_peak_list_title);

        multiRuleRv = (RecyclerView)findViewById(R.id.create_peak_list_detail);
        multiRuleRv.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL,
                false));
        judgePeakAdapter = new JudgePeakAdapter(getApplicationContext());
        multiRuleRv.setAdapter(judgePeakAdapter);
        mAddPeakTv = (TextView) findViewById(R.id.add_peak_tv);
        mDelPeakTv = (TextView) findViewById(R.id.delete_peak_tv);
        mSavePeakTv = (TextView) findViewById(R.id.save_peak_tv);
        mFileDelTv = (TextView)findViewById(R.id.judge_peak_delete_tv) ;
        mFileEditTv = (TextView)findViewById(R.id.judge_peak_edit_tv) ;
        initChannelPopWindow();
        initShowPeakFile();
        initCategories();
    }

    private void initCategories(){
        //从file文件中获取
        mCategoryList = new ArrayList<String>();
        mCategoryList.clear();
        gson = new GsonBuilder().registerTypeAdapter(Double.class, new JsonSerializer<Double>() {

            @Override
            public JsonElement serialize(Double src, Type typeOfSrc, JsonSerializationContext context) {
                if (src == src.longValue())
                    return new JsonPrimitive(src.longValue());
                return new JsonPrimitive(src);
            }
        }).create();
        String category_filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                +"PCR/file"+File.separator + "Category.gson";
        String categoryStr = FileUtils.getStringFromSDFile(category_filePath);
        if(!StringUtil.isEmpty(categoryStr)){
            mCategoryList = gson.fromJson(categoryStr,List.class);
        }else{
            String[] categorArray= getResources().getStringArray(R.array.Category);
          //  mCategoryList = new ArrayList<String>(categorArray.length);
            Collections.addAll(mCategoryList, categorArray);
        }

    }

    private void initChannelPopWindow(){
        String[] channelAarray= getResources().getStringArray(R.array.Channel);
        channelList = new ArrayList<String>(channelAarray.length);
        Collections.addAll(channelList, channelAarray);
        View view= LayoutInflater.from(this).inflate(R.layout.layout_custom_spinner,null);
        channelAdapter=new CustomSpinnerAdapter(this.getApplicationContext(),channelList);
        mChannelLv= (ListView) view.findViewById(R.id.spinner_listview);
        mChannelLv.setAdapter(channelAdapter);
        mChannelPopWindow =new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mChannelPopWindow.setOutsideTouchable(true);
        mChannelPopWindow.setFocusable(true);
        mChannelPopWindow.setContentView(view);
        mChannelPopWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_system_rb_bg_normal)));
        mChannelPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
              collapseChannel();
            }
        });

        channelAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String channel =channelList.get(position);
                if(peak_header_pos != -1){
                    PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                    peakRule.setChannel(channel);
                    judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                }
             collapseChannel();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
    }

    private void initShowPeakFile(){
        showPeakAdapter = new FileListAdapter<>(this.getApplicationContext());
        mShowPeakJudgeLv.setAdapter(showPeakAdapter);
        showPeakFileList = new ArrayList<File>();
        showPeakFileList.clear();
        File[] files = new File(JUDGE_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != showPeakFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".peak")){
                        showPeakFileList.add(files[i]);
                        Collections.sort(showPeakFileList);
                    }
                }
            }
        }
        showPeakAdapter.setFileList(showPeakFileList);
        showPeakAdapter.notifyDataSetChanged();

        mShowPeakJudgeLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                peak_file_pos = i;
                if(showPeakAdapter != null){
                    showPeakAdapter.setSelectedPosition(peak_file_pos);
                }
            }
        });
    }




    private void expandChannel(){
        if(channelList == null && channelList.isEmpty()){
            return;
        }
        mChannelPopWindow.showAsDropDown(peakJudgeTitle,100,0);
        isChannelExpanded = true;
    }

    //收缩
    private void collapseChannel(){
        if (mChannelPopWindow != null && mChannelPopWindow.isShowing()) {
            mChannelPopWindow.dismiss();
        }
        isChannelExpanded = false;
    }


    @Override
    protected void initListener() {
        mAddPeakTv.setOnClickListener(this);
        mDelPeakTv.setOnClickListener(this);
        mSavePeakTv.setOnClickListener(this);
        mFileDelTv.setOnClickListener(this);
        mFileEditTv.setOnClickListener(this);
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        judgePeakAdapter.setOnItemClickListener(new JudgePeakAdapter.OnItemClickListener() {
            @Override
            public void onHeaderChannel(int position) {
                isHeader = true;
                peak_header_pos = position;
                expandChannel();
            }

            @Override
            public void onHeaderClick(int position) {
                isHeader = true;
                peak_header_pos = position;
            }

            @Override
            public void onItemClick(int header_position, int item_position) {
                isHeader = false;
                peak_header_pos = header_position;
                peak_item_pos = item_position;
            }

            @Override
            public void onItemChangeCategory(View view,int header_position, int item_position) {
                //选择种类显示下拉列表
                isHeader = false;
                peak_header_pos = header_position;
                peak_item_pos = item_position;
               // expand();
                PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                    PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                    String category = singleCategory.getCategory();
                    showCategoryDialog(category);
                }

            }

            @Override
            public void onItemChangeNum(int header_position, int item_position) {
                //peakValue
                //获取当前的value值
                peak_header_pos = header_position;
                peak_item_pos = item_position;
                PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                    PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                    double peakValue = singleCategory.getPEAKvalue();
                    showNumKeyboardDialog(1,String.valueOf(peakValue));
                }
            }

            @Override
            public void onItemPeakUp(int header_position, int item_position) {
                peak_header_pos = header_position;
                peak_item_pos = item_position;
                PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                    PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                    double peakValueUp = singleCategory.getPEAKvalueUP();
                    showNumKeyboardDialog(2,String.valueOf(peakValueUp));
                }
            }

            @Override
            public void onItemPeakDown(int header_position, int item_position) {
                peak_header_pos = header_position;
                peak_item_pos = item_position;
                PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                    PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                    double peakValueDown = singleCategory.getPEAKvalueDown();
                    showNumKeyboardDialog(3,String.valueOf(peakValueDown));
                }
            }

            @Override
            public void onItemPeakValueRM(int header_position, int item_position) {
                peak_header_pos = header_position;
                peak_item_pos = item_position;
                PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                    PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                    double peakValueRM = singleCategory.getPEAKvalueRM();
                    showNumKeyboardDialog(4,String.valueOf(peakValueRM));
                }
            }


        });
    }

    @Override
    protected void initData() {

        initPeakValue();
    }

    private void initPeakValue(){
        peakRuleEntities = new ArrayList<PEAKRuleEntity>();
        List<PEAKSingleRuleEntity> peakSingleRuleEntityArrayList = new ArrayList<PEAKSingleRuleEntity>();
        PEAKSingleRuleEntity  peakSingleRuleEntity = new PEAKSingleRuleEntity(true,false,"博卡病毒",
                (double)73.07,(double)3.0,(double)3.0,(double)3.0);
        peakSingleRuleEntityArrayList.add(peakSingleRuleEntity);
        PEAKRuleEntity peakRuleEntity = new PEAKRuleEntity(true,"FAM",peakSingleRuleEntityArrayList);
        peakRuleEntities.add(peakRuleEntity);

        List<PEAKSingleRuleEntity> peakSingleRuleEntityArrayList2 = new ArrayList<PEAKSingleRuleEntity>();
        PEAKSingleRuleEntity  peakSingleRuleEntity2 = new PEAKSingleRuleEntity(true,false,"甲型流感病毒",
                (double)71.86,(double)1.5,(double)1.5,(double)3.0);
        peakSingleRuleEntityArrayList2.add(peakSingleRuleEntity2);
        PEAKRuleEntity peakRuleEntity2 = new PEAKRuleEntity(true,"CY5",peakSingleRuleEntityArrayList2);
        peakRuleEntities.add(peakRuleEntity2);
        judgePeakAdapter.setJudgePeakList(peakRuleEntities);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gson = null;
        if(null != peakRuleEntities){
            peakRuleEntities.clear();
            peakRuleEntities = null;
        }

        if(null != mCategoryList){
            mCategoryList.clear();
            mCategoryList = null;
        }

        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog = null;
        }

        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
        if(null != channelList){
            channelList.clear();
            channelList = null;
        }

        if(null != showPeakFileList){
            showPeakFileList.clear();
            showPeakFileList = null;
        }

        if(null != categoryDialog){
            categoryDialog.dismiss();
            categoryDialog = null;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.add_peak_tv){
            if(null != peakRuleEntities){
                if(peakRuleEntities.size()== 0){
                    initPeakValue();
                    peak_header_pos = -1;
                }else if(peakRuleEntities.size() >0){
                    if(isHeader){
                        if(peak_header_pos != -1){
                            //弹出对话框提示是否添加通道
                            showNoticeDialog("",getResources().getString(R.string.judge_peak_dialog_message_add_channel),1);
                        }else{
                            //添加通道前,请选中任意通道
                            Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.judge_peak_toast_select_channel),Toast.LENGTH_LONG).show();
                        }
                    }else {
                        if(peak_header_pos != -1 && peak_item_pos !=-1){
                            //是否添加通道
                            showNoticeDialog(""
                                    ,getResources().getString(R.string.judge_ct_dialog_message_add_category),3);
                        }else{
                            //添加种类前,请选中同一通道的病毒种类
                            Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.judge_peak_toast_add_category),Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }


        }else if(id == R.id.delete_peak_tv){
            if(isHeader){
                if(peak_header_pos != -1){
                    //弹出对话框提示是否添加通道,是否删除通道
                    showNoticeDialog("",getResources().getString(R.string.judge_peak_dialog_message_delete_channel),2);
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.judge_peak_toast_delete_channel),Toast.LENGTH_LONG).show();
                }
            }else {
                if(peak_header_pos != -1 && peak_item_pos !=-1){
                    showNoticeDialog(""
                            ,getResources().getString(R.string.judge_ct_dialog_message_delete_category),4);
                }else{
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.judge_peak_toast_delete_category),Toast.LENGTH_LONG).show();
                }
            }



        }else if(id == R.id.save_peak_tv){
            String fileName = mFileNameEt.getText().toString().toString();
            if(StringUtil.isEmpty(fileName)){
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.judge_ct_toast_file_name),Toast.LENGTH_LONG).show();
            }else {
                if(null != peakRuleEntities && peakRuleEntities.size()>0){
                    if(isSameNameFile(fileName)){
                        showNoticeDialog(fileName,getResources().getString(R.string.judge_ct_dialog_same_file),6);
                    }else{
                        savePeakJudgeFile(fileName);
                    }
                }else {
                    Toast.makeText(getApplicationContext(),
                            getResources().getString(R.string.judge_ct_toast_file_empty),Toast.LENGTH_LONG).show();
                }

            }

        }else if(id == R.id.judge_peak_delete_tv){
            if(peak_file_pos != -1){
                showNoticeDialog("",
                        getResources().getString(R.string.judge_ct_dialog_message_delete_ct_file),5);
            }else {
                Toast.makeText(getApplicationContext()
                        ,getResources().getString(R.string.judge_ct_toast_delete_file),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.judge_peak_edit_tv){
            if(peak_file_pos != -1){
                editPeakJudgeFile();
            }else {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.judge_ct_toast_file_edit),Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 更新单重判断文件列表
     */
    private void updatePeakJudgeList(){
        File[] files = new File(JUDGE_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            showPeakFileList.clear();
            for (int i = 0; i < files.length; i++) {
                if (null != showPeakFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".peak")){
                        showPeakFileList.add(files[i]);
                        Collections.sort(showPeakFileList);
                    }
                }
            }
        }

        showPeakAdapter.setFileList(showPeakFileList);
        showPeakAdapter.notifyDataSetChanged();
    }

    /**
     * 保存多重判定文件
     */
    private void savePeakJudgeFile(String fileName){
        saveCategory();
        String peakJudgeString = gson.toJson(peakRuleEntities);
        boolean result = FileUtils.saveFileInSdcard(peakJudgeString,JUDGE_PATH,fileName,".peak");
        if(result){
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.judge_peak_toast_save_success),Toast.LENGTH_LONG).show();
            updatePeakJudgeList();
        }else {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.judge_peak_toast_save_fail),Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 删除
     */
    private void deleteCategory(){
        if(peak_header_pos != -1){
            PEAKRuleEntity deletePeakRuleEntity = peakRuleEntities.get(peak_header_pos);
            List<PEAKSingleRuleEntity> peakSingleRuleEntities = deletePeakRuleEntity.getPEAKSingleRuleEntityList();
            if(peakSingleRuleEntities.size()>1){
                PEAKSingleRuleEntity  singleCategoryRule = peakSingleRuleEntities.get(peak_item_pos);
                peakSingleRuleEntities.remove(singleCategoryRule);
            }else{
                peakRuleEntities.remove(deletePeakRuleEntity);
            }
            judgePeakAdapter.setJudgePeakList(peakRuleEntities);
            peak_header_pos = -1;
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.judge_peak_toast_delete_virus_type),Toast.LENGTH_LONG).show();
        }


    }


    /**
     * 添加通道内的额种类
     */
    private void addCategory(){
        if(peak_header_pos != -1){
            PEAKRuleEntity addPeakRuleEntity = peakRuleEntities.get(peak_header_pos);
            List<PEAKSingleRuleEntity> peakSingleRuleEntities = addPeakRuleEntity.getPEAKSingleRuleEntityList();
            PEAKSingleRuleEntity  peakSingleCategoryRule = new PEAKSingleRuleEntity(true,false,"",
                    (double)0.00f,(double)0.0f,(double)0.0f,(double)0.0);
            peakSingleRuleEntities.add(peakSingleCategoryRule);
            judgePeakAdapter.setJudgePeakList(peakRuleEntities);
            judgePeakAdapter.setSelectedPosition(-1);
            peak_header_pos = -1;
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.judge_peak_toast_add_category),Toast.LENGTH_LONG).show();
        }

    }


    private void addPeakChannel(){
        if(peak_header_pos != -1){
            List<PEAKSingleRuleEntity> peakSingleRuleEntityArrayList = new ArrayList<PEAKSingleRuleEntity>();
            PEAKSingleRuleEntity  peakSingleRuleEntity = new PEAKSingleRuleEntity(true,false,"",
                    (double)0.00f,(double)0.0f,(double)0.0f,(double)0.0);
            peakSingleRuleEntityArrayList.add(peakSingleRuleEntity);
            PEAKRuleEntity peakRuleEntity = new PEAKRuleEntity(true,"ROX",peakSingleRuleEntityArrayList);
            peakRuleEntities.add(peakRuleEntity);
            judgePeakAdapter.setJudgePeakList(peakRuleEntities);
            judgePeakAdapter.setSelectedPosition(-1);
            peak_header_pos = -1;
        }else{
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.judge_peak_toast_select_channel),Toast.LENGTH_LONG).show();
        }

    }

    /**
     * 删除选中的通道
     */

    private void deletePeakChannel(){
        if(peak_header_pos  != -1){
            PEAKRuleEntity peakRuleEntity = peakRuleEntities.get(peak_header_pos);
            peakRuleEntities.remove(peakRuleEntity);
            judgePeakAdapter.setJudgePeakList(peakRuleEntities);
            judgePeakAdapter.setSelectedPosition(-1);
            peak_header_pos = -1;
        }


    }

    /**
     * 删除判别文件
     */
    private void deletePeakJudgeFile(){
        if(peak_file_pos != -1){
            File file = showPeakFileList.get(peak_file_pos);
            //删除所选的文件
            String deletePath = JUDGE_PATH + file.getName();
            boolean result =  FileUtils.deleteByPath(deletePath);//删除存储
            showPeakFileList.remove(file);
            showPeakAdapter.setFileList(showPeakFileList);
            showPeakAdapter.notifyDataSetChanged();
            peak_file_pos = -1;
            showPeakAdapter.setSelectedPosition(peak_file_pos);
        }

    }

    /**
     * 编辑多重判别文件
     */
    private void editPeakJudgeFile(){
        File file = showPeakFileList.get(peak_file_pos);
        //
        String fileName = file.getName();
        if(!StringUtil.isEmpty(fileName)) {

            mFileNameEt.setText(fileName.substring(0,fileName.length()-5));
            File peakFile = new File(JUDGE_PATH + fileName);
            peakRuleEntities.clear();
            peakRuleEntities = PEAKRuleEntity.getPEAKRuleList(peakFile);
            judgePeakAdapter.setJudgePeakList(peakRuleEntities);
            peak_file_pos = -1;
            showPeakAdapter.setSelectedPosition(peak_file_pos);
        }else {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.judge_peak_toast_file_name),Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 覆盖保存
     * @param fileName
     */
    private void coverPeakJudgeFile(String fileName){

        String deletePath = JUDGE_PATH + fileName + ".peak";
        boolean result =  FileUtils.deleteByPath(deletePath);//删除存储
        if(result){
            //删除成功再覆盖
            savePeakJudgeFile(fileName);
        }
    }

    private boolean isSameNameFile(String fileName){
        if(null != showPeakFileList && showPeakFileList.size()>0){
            for(int i = 0;i<showPeakFileList.size();i++){
                String file = fileName+".peak";
                if(showPeakFileList.get(i).getName().equals(file)){
                    //提示
                    return true;
                }
            }
        }
        return false;
    }


    /**
     *
     * @param message
     * @param dialog_Type 1：添加 2：删除
     *                    4:删除通道内的种类
     */
    private void showNoticeDialog(String fileName,String message,int dialog_Type){
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog =null;
        }

        if(null == noticeDialog){
            noticeDialog =new NoticeDialog(this,R.style.Dialog);
            noticeDialog.setMessage(message);
            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    noticeDialog.dismiss();
                    //删除
                    if(dialog_Type == 1){
                        addPeakChannel();
                    }else if(dialog_Type == 2){
                        deletePeakChannel();
                    }else if(dialog_Type == 3){
                        addCategory();
                    }else if(dialog_Type == 4){
                        deleteCategory();
                    }else if(dialog_Type == 5){
                        deletePeakJudgeFile();
                    }else if(dialog_Type == 6){
                        coverPeakJudgeFile(fileName);
                    }

                }
            });

            noticeDialog.setOnCancelOnclickListener(new NoticeDialog.OnCancelClickListener() {
                @Override
                public void onCancelClick() {
                    noticeDialog.dismiss();
                    noticeDialog = null;
                }
            });

            noticeDialog.show();
        }
    }


    /**
     *
     * @param dialogType
     * 1:peakValue
     * 2:peakValueUp
     * 3:peakValueDown
     * 4:peakValueRM
     * @param s
     */
    private void showNumKeyboardDialog(int dialogType,String s){
        if(null != numKeyboardDialog){
            numKeyboardDialog.dismiss();
            numKeyboardDialog = null;
        }
        if(numKeyboardDialog == null){
            numKeyboardDialog = new NumKeyboardDialog(this);

            numKeyboardDialog.setEditText(s);
            numKeyboardDialog.setOnCanlceListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.setOnKeyBoardDoneListener(new NumKeyboardDialog.OnKeyBoardDoneListener() {
                @Override
                public void onKeyBoardDone(String s) {
                   if(dialogType == 1){
                       //peakValue
                       double peakValue = (double)StringUtil.convertToFloat(s,0.00f);
                       PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                       List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                       if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                           PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                           singleCategory.setPEAKvalue(peakValue);
                           judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                       }
                   }else if(dialogType == 2){
                       double peakValueUp = (double)StringUtil.convertToFloat(s,0.00f);
                       PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                       List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                       if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                           PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                           singleCategory.setPEAKvalueUP(peakValueUp);
                           judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                       }
                   }else if(dialogType == 3){
                       double peakValueDown = (double)StringUtil.convertToFloat(s,0.00f);
                       PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                       List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                       if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                           PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                           singleCategory.setPEAKvalueDown(peakValueDown);
                           judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                       }
                   }else if(dialogType == 4){
                       double peakValueRM = (double)StringUtil.convertToFloat(s,0.00f);
                       PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                       List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                       if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                           PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                           singleCategory.setPEAKvalueRM(peakValueRM);
                           judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                       }
                   }
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.show();
        }
    }

    private void showCategoryDialog(String category) {
        if (null != categoryDialog) {
            categoryDialog.dismiss();
            categoryDialog = null;
        }
        if (null == categoryDialog) {
            categoryDialog = new CustomLargeDialog(this);
            LayoutInflater mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View) mInflater.inflate(R.layout.dialog_choice_category_layout, null);
            initCategoryView(view,category);
            CustomLargeDialog.Builder builder = new CustomLargeDialog.Builder(this);
            builder.setContentView(view);
            builder.setTitle(R.string.dialog_title_category);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    String category = mCategoryEt.getText().toString();
                    if(StringUtil.isEmpty(category)){
                        Toast.makeText(getApplicationContext(),getResources().getString(R.string.judge_ct_toast_select_category)
                                ,Toast.LENGTH_LONG).show();
                        return;
                    }
                    if(!mCategoryList.contains(category)){
                        mCategoryList.add(category);
                        saveCategory();
                    }
                    if(peak_header_pos != -1 && peak_item_pos != -1){
                        PEAKRuleEntity peakRule = peakRuleEntities.get(peak_header_pos);
                        List<PEAKSingleRuleEntity> peakSingleRuleEntities = peakRule.getPEAKSingleRuleEntityList();
                        if(peakSingleRuleEntities != null && peakSingleRuleEntities.size()>0){
                            PEAKSingleRuleEntity singleCategory = peakSingleRuleEntities.get(peak_item_pos);
                            singleCategory.setCategory(category);
                            judgePeakAdapter.setJudgePeakList(peakRuleEntities);
                        }
                    }
                    //保存到了文件
                    categoryDialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    categoryDialog.dismiss();

                }
            });

            categoryDialog = builder.create();
            categoryDialog.show();
        }
    }

    private void initCategoryView(View view,String category){
        mCategoryEt = (EditText)view.findViewById(R.id.dig_category_edit);
        mCategoryLv= (ListView) view.findViewById(R.id.dig_category_lv);
        mCategoryEt.setText(category);
        spinnerAdapter=new CustomSpinnerAdapter(this.getApplicationContext(),mCategoryList);
        mCategoryLv.setAdapter(spinnerAdapter);
        spinnerAdapter.notifyDataSetChanged();
        spinnerAdapter.setOnItemClickListener(new CustomSpinnerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                String category = mCategoryList.get(position);
                mCategoryEt.setText(category);
            }

            @Override
            public void onItemLongClick(int position) {
                //确认删除病原体种类
                AlertDialog alertDialog = new AlertDialog.Builder(PeakJudgeActivity.this,R.style.AlertDialogCustom)
                        .setMessage(getResources().getString(R.string.judge_ct_dialog_message_delete_category_type))
                    .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            for (String text :mCategoryList) {
                                if(text.equals(mCategoryEt.getText().toString())){
                                    mCategoryEt.setText("");
                                }
                            }
                            mCategoryList.remove(position);
                            saveCategory();
                            spinnerAdapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).create();
                alertDialog.show();
            }
        });
    }

    private void saveCategory(){
        try {
            String category = gson.toJson(mCategoryList);
            File file = new File( Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
                    +"PCR/file"+File.separator + "Category.gson");
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(category.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }
    }
}
