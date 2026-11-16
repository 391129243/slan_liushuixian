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

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.CTRuleEntity;
import com.gidi.bio_pcr.adapter.CustomSpinnerAdapter;
import com.gidi.bio_pcr.adapter.FileListAdapter;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.config.adapter.JudgeCtAdapter;
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
public class JudgeCtActivity extends BaseActivity implements View.OnClickListener{

    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;

    private ArrayList<File> showCtFileList;
    private ListView mShowCtJudgeLv;
    private FileListAdapter<File> showCtAdapter;
    private TextView mFileDelTv,mFileEditTv;//ct文件删除编辑


    private LinearLayoutCompat ctJudgeTitle;
    private EditText mFileNameEt;

    private List<CTRuleEntity> singleRuleEntities;//单重判断列表

    private ListView singleRuleRv;//单重判别列表
    private JudgeCtAdapter judgeCtAdapter;
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
    private int ct_item_pos = -1;
    private int ct_file_pos = -1;//选择编辑或删除的ct文件的pos
    private NoticeDialog noticeDialog;
    private NumKeyboardDialog numKeyboardDialog;
    private CustomLargeDialog categoryDialog;
    private String JUDGE_PATH = FileUtils.CONFIG_PATH + File.separator + "judge"
            +File.separator;
    private Gson gson;

    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_judge_ct;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        mTitleLayout = (RelativeLayout)findViewById(R.id.judge_ct_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.judge_ct_title));
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        mShowCtJudgeLv = (ListView) findViewById(R.id.judge_ct_show_list);//显示的列表
        ctJudgeTitle = (LinearLayoutCompat)findViewById(R.id.create_ct_list_title);
        mFileNameEt = (EditText)findViewById(R.id.single_judge_filename_tv);
        mFileNameEt.addTextChangedListener(new LimitInputTextWatcher(mFileNameEt));
        singleRuleRv = (ListView) findViewById(R.id.create_ct_list_detail);
        judgeCtAdapter = new JudgeCtAdapter(getApplicationContext());
        singleRuleRv.setAdapter(judgeCtAdapter);
        mAddPeakTv = (TextView) findViewById(R.id.add_ct_tv);
        mDelPeakTv = (TextView) findViewById(R.id.delete_ct_tv);
        mSavePeakTv = (TextView) findViewById(R.id.save_ct_tv);
        mFileDelTv = (TextView)findViewById(R.id.judge_ct_delete_tv) ;
        mFileEditTv = (TextView)findViewById(R.id.judge_ct_edit_tv) ;
        initChannelPopWindow();
        initCategories();
        initShowCtFile();
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
                if(ct_item_pos != -1){
                    CTRuleEntity peakRule = singleRuleEntities.get(ct_item_pos);
                    peakRule.setChannel(channel);
                    judgeCtAdapter.setJudgeCtList(singleRuleEntities);
                }
             collapseChannel();
            }

            @Override
            public void onItemLongClick(int position) {

            }
        });
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
            Collections.addAll(mCategoryList, categorArray);
        }

    }

    private void initShowCtFile(){
        showCtAdapter = new FileListAdapter<>(this.getApplicationContext());
        mShowCtJudgeLv.setAdapter(showCtAdapter);
        showCtFileList = new ArrayList<File>();
        showCtFileList.clear();
        File[] files = new File(JUDGE_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            for (int i = 0; i < files.length; i++) {
                if (null != showCtFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".ct")){
                        showCtFileList.add(files[i]);
                        Collections.sort(showCtFileList);
                    }
                }
            }
        }
        showCtAdapter.setFileList(showCtFileList);
        showCtAdapter.notifyDataSetChanged();

        mShowCtJudgeLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(null != showCtAdapter){
                    showCtAdapter.setSelectedPosition(i);
                    ct_file_pos = i;
                }
            }
        });
    }


    private void expandChannel(){
        if(channelList == null && channelList.isEmpty()){
            return;
        }
        mChannelPopWindow.showAsDropDown(ctJudgeTitle,400,0);
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
       judgeCtAdapter.setOnItemClickListener(new JudgeCtAdapter.OnItemClickListener() {
           @Override
           public void onItemClick(int item_position) {
               ct_item_pos =item_position;
           }

           @Override
           public void onItemChangeChannel(View view, int item_position) {
               ct_item_pos =item_position;
               expandChannel();
           }

           @Override
           public void onItemChangeCategory(int item_position) {
               ct_item_pos =item_position;
               CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
               String category = ctRule.getCategory();
               showCategoryDialog(category);
           }

           @Override
           public void onItemChangeNum(int item_position) {
               ct_item_pos =item_position;
               CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
               double ctValue = ctRule.getCTvalue();
               showNumKeyboardDialog(1,String.valueOf(ctValue));
           }

           @Override
           public void onItemCtUp(int item_position) {
               ct_item_pos =item_position;
               CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
               double ctValueUp = ctRule.getCTvalueUP();
               showNumKeyboardDialog(2,String.valueOf(ctValueUp));
           }

           @Override
           public void onItemCtDown(int item_position) {
               ct_item_pos =item_position;
               CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
               double ctValueDown = ctRule.getCTvalueDown();
               showNumKeyboardDialog(3,String.valueOf(ctValueDown));
           }

           @Override
           public void onItemCtSuspicious(int item_position) {
               ct_item_pos =item_position;
               CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
               double ctValueSuspicious = ctRule.getCTvalueUP_Suspicious();
               showNumKeyboardDialog(4,String.valueOf(ctValueSuspicious));
           }
       });
    }

    @Override
    protected void initData() {
        initCtValue();
    }

    private void initCtValue(){
        singleRuleEntities = new ArrayList<CTRuleEntity>();
        CTRuleEntity add_ctRule = new CTRuleEntity(true,false,false,"FAM"
                ,"甲流",(double) 28.5,(double)20,(double) 0.0,(double) 0.0);
        singleRuleEntities.add(add_ctRule);
        judgeCtAdapter.setJudgeCtList(singleRuleEntities);
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
        if(null != singleRuleEntities){
            singleRuleEntities.clear();
            singleRuleEntities = null;
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
        if(null != categoryDialog){
            categoryDialog.dismiss();
            categoryDialog = null;
        }
        if(null != showCtFileList){
            showCtFileList.clear();
            showCtFileList = null;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.add_ct_tv){
            if(ct_item_pos !=-1){
                showNoticeDialog("",getResources().getString(R.string.judge_ct_dialog_message_add_category),1);
            }

        }else if(id == R.id.delete_ct_tv){
            if(ct_item_pos != -1){
                //弹出对话框提示是否删除种类
                showNoticeDialog("",getResources().getString(R.string.judge_ct_dialog_message_delete_category),2);
            }

        }else if(id == R.id.save_ct_tv){
            String fileName = mFileNameEt.getText().toString().trim();
            if(StringUtil.isEmpty(fileName)){
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.judge_ct_toast_file_name),Toast.LENGTH_LONG).show();
            }else {
                if(null != singleRuleEntities && singleRuleEntities.size()>0){
                    //查看是否有相同名字的列表，有的话要提示是否进行覆盖
                    if(isSameNameFile(fileName)){
                        //弹出对话框提示
                        showNoticeDialog(fileName,getResources().getString(R.string.judge_ct_dialog_same_file),4);
                    }else {
                        saveCtJudgeFile(fileName);
                    }
                }else {
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.judge_ct_toast_file_empty),Toast.LENGTH_LONG).show();

                }
            }
        }else if(id == R.id.judge_ct_delete_tv){
            //确定删除文件
            if(ct_file_pos != -1){
                showNoticeDialog(""
                        ,getResources().getString(R.string.judge_ct_dialog_message_delete_ct_file),3);
            }else{
                Toast.makeText(getApplicationContext()
                        ,getResources().getString(R.string.judge_ct_toast_delete_file),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.judge_ct_edit_tv){
            editCtJudgeFile();
        }
    }

    private boolean isSameNameFile(String fileName){
        if(null != showCtFileList && showCtFileList.size()>0){
            for(int i = 0;i<showCtFileList.size();i++){
                String file = fileName+".ct";
                if(showCtFileList.get(i).getName().equals(file)){
                    //提示
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 保存多重判定文件
     */
    private void saveCtJudgeFile(String fileName){
        String ctJudgeString = gson.toJson(singleRuleEntities);
        boolean result = FileUtils.saveFileInSdcard(ctJudgeString,JUDGE_PATH,fileName,".ct");
        if(result){
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.judge_ct_toast_save_success),Toast.LENGTH_LONG).show();
            //更新列表
            updateCtJudgeList();
        }else {
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.judge_ct_toast_save_fail),Toast.LENGTH_LONG).show();
        }
        saveCategory();
    }

    /**
     * 删除
     */
    private void deleteCategory(){
        CTRuleEntity del_ctRule = singleRuleEntities.get(ct_item_pos);
        singleRuleEntities.remove(del_ctRule);
        judgeCtAdapter.setJudgeCtList(singleRuleEntities);
    }


    /**
     * 添加通道内的额种类
     */
    private void addCategory(){
        CTRuleEntity add_ctRule = new CTRuleEntity(true,false,false,"ROX"
                ,"乙流",(double) 28.5,(double)20,(double) 0.0,(double) 0.0);
        singleRuleEntities.add(add_ctRule);
        judgeCtAdapter.setJudgeCtList(singleRuleEntities);
    }

    private void deleteCtJudgeFile(){
        if(ct_file_pos != -1){
            File file = showCtFileList.get(ct_file_pos);
            //删除所选的文件
            String deletePath = JUDGE_PATH + file.getName();
            boolean result =  FileUtils.deleteByPath(deletePath);//删除存储
            showCtFileList.remove(file);
            showCtAdapter.setFileList(showCtFileList);
            showCtAdapter.notifyDataSetChanged();
            ct_file_pos = -1;
            showCtAdapter.setSelectedPosition(ct_file_pos);
            Log.i("JudgeCtActivity","删除ct列表" + result);
        }
    }

    /**
     * 覆盖文件。删除+新建
     */
    private void coverCtJudgeFile(String fileName){

        String deletePath = JUDGE_PATH + fileName + ".ct";
        boolean result =  FileUtils.deleteByPath(deletePath);//删除存储
        if(result){
            //删除成功再覆盖
            saveCtJudgeFile(fileName);
        }
    }

    private void editCtJudgeFile(){
        //读取Ct判别文件
        if(ct_file_pos != -1){
            File file = showCtFileList.get(ct_file_pos);
            //
            String fileName = file.getName();
            if(!StringUtil.isEmpty(fileName)){
                mFileNameEt.setText(fileName.substring(0,fileName.length()-3));
                File ctFile = new File(JUDGE_PATH + fileName);
                singleRuleEntities.clear();
                singleRuleEntities = CTRuleEntity.getCTRuleList(ctFile);
                judgeCtAdapter.setJudgeCtList(singleRuleEntities);
                ct_file_pos = -1;
                showCtAdapter.setSelectedPosition(ct_file_pos);
            }
        }else{
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.judge_ct_toast_file_edit),Toast.LENGTH_LONG).show();
        }

    }

    /**
     * 更新单重判断文件列表
     */
    private void updateCtJudgeList(){
        File[] files = new File(JUDGE_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0) {
            showCtFileList.clear();
            for (int i = 0; i < files.length; i++) {
                if (null != showCtFileList) {
                    if(files[i].getName() != null && files[i].getName().contains(".ct")){
                        showCtFileList.add(files[i]);
                        Collections.sort(showCtFileList);
                    }
                }
            }
        }

        showCtAdapter.setFileList(showCtFileList);
        showCtAdapter.notifyDataSetChanged();
    }


    /**
     *
     * @param message
     * @param dialog_Type 1：添加 2：删除,3.删除ct文件
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
                        addCategory();
                     }else if(dialog_Type == 2){
                        deleteCategory();
                     }else if(dialog_Type == 3){
                        deleteCtJudgeFile();
                     }else if(dialog_Type == 4){
                         //覆盖
                         coverCtJudgeFile(fileName);
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
     * 1:ctValue
     * 2:ctValueUp
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
                       //ctValue
                       double ctValue = (double)StringUtil.convertToFloat(s,0.00f);
                       CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
                       ctRule.setCTvalue(ctValue);
                       judgeCtAdapter.setJudgeCtList(singleRuleEntities);

                   }else if(dialogType == 2){
                       double ctValueUp= (double)StringUtil.convertToFloat(s,0.0f);
                       CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
                       ctRule.setCTvalueUP(ctValueUp);
                       judgeCtAdapter.setJudgeCtList(singleRuleEntities);
                   }else if(dialogType == 3){
                       double ctValueDown =(double)StringUtil.convertToFloat(s,0.0f);
                       CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
                       ctRule.setCTvalueDown(ctValueDown);
                       judgeCtAdapter.setJudgeCtList(singleRuleEntities);
                   }else if(dialogType == 4){
                       double ctValueSSuspicious = (double)StringUtil.convertToFloat(s,0.0f);
                       CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
                       ctRule.setCTvalueUP_Suspicious(ctValueSSuspicious);
                       judgeCtAdapter.setJudgeCtList(singleRuleEntities);
                   }
                    numKeyboardDialog.dismiss();
                }
            });

            numKeyboardDialog.show();
        }
    }


    /**
     * 病毒种类编辑对话框
     */

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

                    if(ct_item_pos != -1){
                        CTRuleEntity ctRule = singleRuleEntities.get(ct_item_pos);
                        if(ctRule != null){
                            ctRule.setCategory(category);
                            judgeCtAdapter.setJudgeCtList(singleRuleEntities);
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
                AlertDialog alertDialog = new AlertDialog.Builder(JudgeCtActivity.this,R.style.AlertDialogCustom)
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
