package com.gidi.bio_pcr.slan.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.ChoiceTemplateAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bean.ExQueryCondition;
import com.gidi.bio_pcr.db.ExperimentModel;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.slan.adapter.ExSLANResultAdapter;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.CustomLargeDialog;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/**
 * 宏石实验列表
 */
public class ResultExSLANFragment extends BaseFragment implements View.OnClickListener{

    private ListView mExperimentRv;
    private Button searchBtn;
    private ImageView mItemArrowImg,mTimeArrowImg;
    private TextView mSearchTimeTv;
    private TextView mTestItemTv;//选择项目
    private CustomLargeDialog mDatePickerDialog;
    private CustomDialog mTemplateDialog;//容量
    private int year,month,day;
    private ExSLANResultAdapter mExAdapter;
    private List<ExperimentModel> mExperiments;
    private List<ExperimentModel> mSelectExperiments;
    private ResultSearchAsyncTask resultSearchAsyncTask;

    private ChoiceTemplateAdapter choiceTemplateAdapter;
    private List<String> templateList;
    private String choiceItem;
    private String searchTime;

    private TextView mFirstPageTv;
    private TextView mPrePageTv;
    private TextView mNextPageTv;
    private TextView mLastPageTv;
    private TextView mAllCheckTv;
    private TextView mDeleteTv;
    private TextView mCancelTv;

    private int index = 0;//初始的页数

    private int pageCount = 1;//总共的页数
    private boolean isAllCheck = false;
    private boolean isCkShow = false;
    private NoticeDialog noticeDialog;

    private int PAGE_SIZE = 10;//每一页查询10个
    private boolean isQueryCondition = false;//是否条件查询

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_experiment;
    }

    @Override
    protected void initView(View rootView) {
        mExperimentRv = (ListView) rootView.findViewById(R.id.result_experiment_list_recycleview);
        searchBtn = (Button)rootView.findViewById(R.id.result_experiment_search_btn);
        mItemArrowImg = (ImageView)rootView.findViewById(R.id.result_experiment_item_arrow_img);
        mTimeArrowImg = (ImageView)rootView.findViewById(R.id.result_experiment_time_arrow_img);
        mSearchTimeTv = (TextView)rootView.findViewById(R.id.result_experiment_search_time_tv);
        mTestItemTv = (TextView)rootView.findViewById(R.id.result_experiment_search_item_tv);

        ImageView clearImg = (ImageView)rootView.findViewById(R.id.result_experiment_time_clear_img);
        clearImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchTimeTv.setText("");
            }
        });

        ImageView clearItem = (ImageView)rootView.findViewById(R.id.result_experiment_item_clear_img);
        clearItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTestItemTv.setText("");
            }
        });
        mFirstPageTv = (TextView)rootView.findViewById(R.id.result_ex_list_first_tv);
        mPrePageTv = (TextView)rootView.findViewById(R.id.result_ex_list_prepage_tv);
        mNextPageTv = (TextView)rootView.findViewById(R.id.result_ex_list_nextpage_tv);
        mLastPageTv = (TextView)rootView.findViewById(R.id.result_ex_list_lastpage_tv);
        mDeleteTv = (TextView)rootView.findViewById(R.id.result_ex_list_delete_tv) ;
        mAllCheckTv = (TextView)rootView.findViewById(R.id.result_ex_list_allcheck_tv) ;
        mCancelTv = (TextView)rootView.findViewById(R.id.result_ex_list_cancel_tv) ;

    }

    PipeMainViewModel mainViewModel;
    @Override
    public void onViewCreated(@NonNull View view, @androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(PipeMainViewModel.class);
    }


    @Override
    protected void initListener() {
        initData();
        searchBtn.setOnClickListener(this);
        mItemArrowImg.setOnClickListener(this);
        mTimeArrowImg.setOnClickListener(this);
        mFirstPageTv.setOnClickListener(this);
        mPrePageTv.setOnClickListener(this);
        mNextPageTv.setOnClickListener(this);
        mLastPageTv.setOnClickListener(this);
        mDeleteTv.setOnClickListener(this);
        mAllCheckTv.setOnClickListener(this);
        mCancelTv.setOnClickListener(this);
        mExAdapter.setOnItemCliclListener(new ExSLANResultAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {

                ExperimentModel experimentModel = mExperiments.get(position);
                int expType = experimentModel.getExpType();
                Log.i("ResultEx","==当前的实验类型expType" + expType);
                if(null != experimentModel){
                    //1是宏石
                    if(expType == 1){
                        if(mainViewModel!=null) {
                            mainViewModel.setPcrExperimentModel(experimentModel);
                            mainViewModel.onNavClicked(ConstantData.NAV_SLANExDetail);
                        }
                    }
                }

            }

            @Override
            public void onItemLongClick(int position) {
                //长按显示checkbox
                if(!isCkShow){
                    isCkShow = true;
                    //显示全选按钮
                    mAllCheckTv.setVisibility(View.VISIBLE);
                    mCancelTv.setVisibility(View.VISIBLE);
                    for(ExperimentModel bean : mExperiments){
                        bean.setShow(true);
                    }
                    mExAdapter.setExperimentList(mExperiments);
                }
            }

            @Override
            public void onItemCheck(int position) {
                //选中/取消选中checkbox
                ExperimentModel experimentResult = mExperiments.get(position);
                if(experimentResult.isCheck()){
                    experimentResult.setCheck(false);
                }else {
                    experimentResult.setCheck(true);
                }
                if (experimentResult.isCheck() && !mSelectExperiments.contains(experimentResult)) {
                    mSelectExperiments.add(experimentResult);
                } else if (!experimentResult.isCheck() && mSelectExperiments.contains(experimentResult)) {
                    mSelectExperiments.remove(experimentResult);
                }
                mExAdapter.setExperimentList(mExperiments);
            }
        });
    }

    private void initData(){
        templateList = new ArrayList<String>();
        mExAdapter = new ExSLANResultAdapter(getActivity().getApplicationContext());
        mExperiments = new ArrayList<ExperimentModel>();
        mSelectExperiments = new ArrayList<ExperimentModel>();
        mExAdapter.setExperimentList(mExperiments);
        mExperimentRv.setAdapter(mExAdapter);
        mExAdapter.notifyDataSetChanged();

        researchExperiment(0,PAGE_SIZE);
        //同时获取个数

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
       // resultActivity = (ResultActivity) activity;
    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden){
            cancelTask();
            cancelDialog();
        }else{
            isAllCheck = false;
            isCkShow = false;
            mAllCheckTv.setVisibility(View.INVISIBLE);
            mCancelTv.setVisibility(View.INVISIBLE);
            researchExperiment(0,PAGE_SIZE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mSelectExperiments) {
            mSelectExperiments.clear();
            mSelectExperiments = null;
        }

        if (null != mExperiments) {
            mExperiments.clear();
            mExperiments = null;

        }
        //resultActivity = null;
    }



    /**
     * 第一次查询数据库
     */
    private void researchExperiment(int start,int length){
        ExQueryCondition queryCondition = new ExQueryCondition();
        queryCondition.setStart(start);
        queryCondition.setLength(PAGE_SIZE);
        if(resultSearchAsyncTask == null){
            resultSearchAsyncTask = new ResultSearchAsyncTask(this,queryCondition);
            resultSearchAsyncTask.execute();
        }
    }

    private void cancelTask(){
        if(null != resultSearchAsyncTask){
            resultSearchAsyncTask.cancel(true);
            resultSearchAsyncTask = null;
        }
    }

    private void cancelDialog(){
        if(null != mTemplateDialog){
            mTemplateDialog.dismiss();
            mTemplateDialog  = null;
        }
        if(null != mDatePickerDialog){
            mDatePickerDialog.dismiss();
            mDatePickerDialog = null;
        }
        if(noticeDialog != null){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
    }




    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.result_experiment_search_btn) {
            String startTime = mSearchTimeTv.getText().toString().trim();
            String testItem = mTestItemTv.getText().toString().trim();
            if(StringUtil.isEmpty(startTime) && StringUtil.isEmpty(testItem)){
                researchExperiment(0,PAGE_SIZE);
                isQueryCondition = false;
                index = 0;
            }else{
                searchExperimentResult(0,PAGE_SIZE);
                isQueryCondition = true;
                index = 0;
            }

        } else if (id == R.id.result_experiment_item_arrow_img) {//显示所有的项目
            showTemplateDialog();
        } else if (id == R.id.result_experiment_time_arrow_img) {//弹出时间选择框
            String endDate = mSearchTimeTv.getText().toString().trim();
            if(!StringUtil.isEmpty(endDate)){
                year = DateFormatUtil.getYear(endDate);
                month = DateFormatUtil.getMonth(endDate);
                day = DateFormatUtil.getDay(endDate);
                if(year != -1 && month != -1 && day != -1){
                    showDatePickerDialog(year, month, day);
                }
            }else{
                //没有日期无法查询
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_ex_toast_no_time),Toast.LENGTH_LONG).show();
                return;
            }
        }else if(id == R.id.result_ex_list_first_tv){
            //首页
            onFirstPage();

        }else if(id == R.id.result_ex_list_prepage_tv){
            onPrePage();

        }else if(id == R.id.result_ex_list_nextpage_tv){
            onNextPage();

        }else if(id == R.id.result_ex_list_lastpage_tv){
            onLastPage();

        }else if(id == R.id.result_ex_list_delete_tv){
            if(null != mSelectExperiments && mSelectExperiments.size()>0){
                showNoticeDialog(getActivity().getResources().getString(R.string.result_ex_dialog_delete_ex_result)
                        + mSelectExperiments.size()
                        +getActivity().getResources().getString(R.string.result_export_dialog_message_items));
            }else {
                //请选择要删除的项目结果
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_ex_toast_delete_ex_result),Toast.LENGTH_LONG).show();
            }
        }else if(id == R.id.result_ex_list_allcheck_tv){
            if(null != mExperiments && mExperiments.size()>0){
                if(isAllCheck){
                    isAllCheck = false;
                    for(ExperimentModel result : mExperiments){
                        result.setCheck(false);
                    }
                    mExAdapter.setExperimentList(mExperiments);
                    mSelectExperiments.clear();
                }else {
                    isAllCheck = true;
                    for (ExperimentModel result : mExperiments) {
                        result.setCheck(true);
                    }
                    mExAdapter.setExperimentList(mExperiments);
                    mSelectExperiments.clear();
                    for (int i = 0; i < mExperiments.size(); i++) {
                        mSelectExperiments.add(mExperiments.get(i));
                    } ;
                }
            }


        }else if(id == R.id.result_ex_list_cancel_tv){
            for(ExperimentModel result : mExperiments){
                result.setShow(false);
                result.setCheck(false);
            }
            mExAdapter.setExperimentList(mExperiments);
            mExAdapter.notifyDataSetChanged();
            mSelectExperiments.clear();
            mCancelTv.setVisibility(View.INVISIBLE);
            mAllCheckTv.setVisibility(View.INVISIBLE);
            isCkShow = false;
            isAllCheck = false;
        }
    }

    /**
     * 第一页
     */
    private void onFirstPage(){
        if(index == 0){
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_first_page),Toast.LENGTH_SHORT).show();
        }else {
            index = 0;
            mExAdapter.setIndex(index);
        }

        if(isQueryCondition){
            //条件查询
            searchExperimentResult(index*PAGE_SIZE,PAGE_SIZE);
        }else{
            researchExperiment(index*PAGE_SIZE,PAGE_SIZE);
        }
    }

    /**
     * 上一页
     */
    private void onPrePage(){
        if(index == 0){
            index = 0;
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_first_page_no_previous),Toast.LENGTH_SHORT).show();
        }else {
            index--;
            mExAdapter.setIndex(index);
        }
        Log.i("Test","onPrePage index" + index );
        if(isQueryCondition){
            //条件查询
            searchExperimentResult(index*PAGE_SIZE,PAGE_SIZE);
        }else{
            researchExperiment(index*PAGE_SIZE,PAGE_SIZE);
        }
    }

    /**
     * 下一页
     */
    private void onNextPage(){
        index = (index+1);
        if(index<pageCount){
            mExAdapter.setIndex(index);
        }else{
            index = pageCount -1;
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_last_page_no_next),Toast.LENGTH_SHORT).show();
        }
        Log.i("Test","onNextPage index" + index );
        if(isQueryCondition){
            //条件查询
            searchExperimentResult(index*PAGE_SIZE,PAGE_SIZE);
        }else{
            researchExperiment(index*PAGE_SIZE,PAGE_SIZE);
        }
    }

    /**
     * 最后一页
     */
    private void onLastPage(){
        if(index == pageCount -1){
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_last_page),Toast.LENGTH_SHORT).show();
        }else{
            index = pageCount -1;
            mExAdapter.setIndex(index);
            Log.i("Test","onLastPage index" + index );
        }
        if(isQueryCondition){
            //条件查询
            searchExperimentResult(index*PAGE_SIZE,PAGE_SIZE);
        }else{
            researchExperiment(index*PAGE_SIZE,PAGE_SIZE);
        }
    }


    private void searchExperimentResult(int start,int length){
        String startTime = mSearchTimeTv.getText().toString().trim();
        String testItem = mTestItemTv.getText().toString().trim();
       // Log.e("Test","條件查詢" + startTime + "--項目" + testItem);
        ExQueryCondition exQueryCondition = new ExQueryCondition();
        exQueryCondition.setExItem(testItem);
        exQueryCondition.setExStartTime(startTime);
        exQueryCondition.setStart(start);
        exQueryCondition.setLength(length);
        if(null == resultSearchAsyncTask){
            resultSearchAsyncTask = new ResultSearchAsyncTask(this,exQueryCondition);
            resultSearchAsyncTask.execute();
        }
    }


    /**
     * 显示模板下容量选择
     */
    private void showTemplateDialog(){
        if(null != mTemplateDialog){
            mTemplateDialog.dismiss();
            mTemplateDialog = null;
        }
        if(null == mTemplateDialog){
            mTemplateDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_capaties_layout, null);
            RecyclerView capacityView = (RecyclerView) view.findViewById(R.id.detect_capacity_rv);

            choiceTemplateAdapter = new ChoiceTemplateAdapter(getActivity().getApplicationContext());
            LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            layoutManager.setReverseLayout(false);
            capacityView.setLayoutManager(layoutManager);
            capacityView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
            choiceTemplateAdapter.setTemplateList(templateList);
            capacityView.setAdapter(choiceTemplateAdapter);
            choiceTemplateAdapter.notifyDataSetChanged();

            choiceTemplateAdapter.setOnItemCliclListener(new ChoiceTemplateAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(int position) {

                    choiceItem = templateList.get(position);
                    choiceTemplateAdapter.setTemplateList(templateList);
                    choiceTemplateAdapter.setSelectedPosition(position);
                }
            });


            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setTitle(R.string.dialog_title_select_testitem);
            builder.setPositiveButton(R.string.dialog_ok,  new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mTestItemTv.setText(choiceItem);
                    mTemplateDialog.dismiss();
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mTemplateDialog.dismiss();
                    mTemplateDialog = null;
                }
            });
            mTemplateDialog = builder.create();
            mTemplateDialog.setCanceledOnTouchOutside(true);
            mTemplateDialog.show();
        }
    }


    private void showDatePickerDialog(int year, int month, int dayOfMonth){
        if(null != mDatePickerDialog){
            mDatePickerDialog.dismiss();
            mDatePickerDialog = null;
        }
        if(null == mDatePickerDialog){
            mDatePickerDialog = new CustomLargeDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View pickView = mInflater.inflate(R.layout.dialog_datepicker_layout, null);
            final DatePicker datePicker = (DatePicker)pickView.findViewById(R.id.date_picker);
            datePicker.init(year, month, dayOfMonth, new DatePicker.OnDateChangedListener() {

                @Override
                public void onDateChanged(DatePicker view, int year, int month, int day) {
                    // TODO Auto-generated method stub
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month, day);
                }
            });
            CustomLargeDialog.Builder builder = new CustomLargeDialog.Builder(getActivity());
            builder.setContentView(pickView);
            builder.setTitle(this.getResources().getString(R.string.dialog_title_choice_date));
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    Log.i("ResultExFragment", "Year--" + datePicker.getYear() + " month---" + datePicker.getMonth() + "day---" + datePicker.getDayOfMonth());
                    updateDate(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                    mDatePickerDialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mDatePickerDialog.dismiss();
                    mDatePickerDialog = null;
                }
            });
            mDatePickerDialog = builder.create();
            mDatePickerDialog.show();
        }
    }





    private void updateDate(final int year, final int month, final int day){
        this.year = year;
        this.month = month + 1;
        this.day = day;
        mSearchTimeTv.setText((String.format("%04d-%02d-%02d",this.year,this.month,this.day)));
        this.searchTime = mSearchTimeTv.getText().toString().trim();

    }

    /**
     * 删除选中的数据
     */
    private void deleteExperimentAction(){
        if (mSelectExperiments != null && mSelectExperiments.size() > 0) {
            deleteExperiment(mSelectExperiments);
            mSelectExperiments.clear();
        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_delete_ex_result), Toast.LENGTH_SHORT).show();
        }
        isCkShow = false;
    }

    ///////////////////////////////////////////////////////////////////////////////////
    //删除所有的项目记录
    private void deleteAllExperiments(){
        LiteORMCtrlMgr.getInstance().deleteAllExperiments();
        mCancelTv.setVisibility(View.INVISIBLE);
        mAllCheckTv.setVisibility(View.INVISIBLE);
        isCkShow = false;
        isAllCheck = false;
        //删除result文件夹下所有的文件
        File resultFile = new File(FileUtils.SLAN_RESULT_PATH);
        if(null != resultFile){
            boolean result = FileUtils.removeAll(resultFile);
            Log.i("ResultExFragment","删除文件夹下的所有记录" + result);
        }

    }

    //删除选中的项目记录以及项目对应的孔位记录
    private void deleteExperiment(List<ExperimentModel> selectExperiments){
        int del_result = LiteORMCtrlMgr.getInstance().deleteExperiments(selectExperiments);
        if(del_result != -1){
            //表示删除成功，重新刷新数据库
            //删除对应的.pcr文件
            Log.i("Test","del_result---" + del_result);
            if(isQueryCondition){
                //条件查询
                searchExperimentResult(index*PAGE_SIZE,PAGE_SIZE);
            }else{
                researchExperiment(index*PAGE_SIZE,PAGE_SIZE);
            }

            for(ExperimentModel experimentModel : selectExperiments){
                String startTime = experimentModel.getStartTime();
                String testItem = experimentModel.getTestItem();
                String aftertime = DateFormatUtil.convertDateFormat(startTime);
                String resultDirPath = experimentModel.getFilePath();
                boolean del_pcr = FileUtils.deleteDirectory(resultDirPath);
            }
        }else{
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_cannot_delete), Toast.LENGTH_SHORT).show();
        }
        mCancelTv.setVisibility(View.INVISIBLE);
        mAllCheckTv.setVisibility(View.INVISIBLE);
        isCkShow = false;
        isAllCheck = false;

    }

    private long experimentCount = 0;
    public static class ResultSearchAsyncTask extends AsyncTask<Void, Integer, List<ExperimentModel>> {

        private WeakReference<ResultExSLANFragment> weakfrag;
        private ExQueryCondition queryCondition;

        public ResultSearchAsyncTask(ResultExSLANFragment fragment, ExQueryCondition queryCondition){
            weakfrag = new WeakReference<ResultExSLANFragment>(fragment);
            this.queryCondition = queryCondition;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            final ResultExSLANFragment mFragment = weakfrag.get();
            super.onPreExecute();
            if(null == mFragment){
                return;
            }
        }

        //后台进程的执行
        @Override
        protected List<ExperimentModel> doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            final ResultExSLANFragment mFragment = weakfrag.get();
            mFragment.experimentCount =  LiteORMCtrlMgr.getInstance().getExperimentNumber(queryCondition);
            mFragment.mExperiments = LiteORMCtrlMgr.getInstance().searchExprement(queryCondition);
             Log.e("Record","==查询实验数目mFragment.experimentCount--" + mFragment.experimentCount);
            return  mFragment.mExperiments;
        }

        @Override
        protected void onPostExecute(List<ExperimentModel> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            final ResultExSLANFragment mFragment = weakfrag.get();
            if(null != mFragment.mExperiments){
                mFragment.showExResultList();
                mFragment.getPageCountNum();//20250211
                mFragment.getAllRecordTemplate();
                if(mFragment.mExperiments.size() == 0){
                    Toast.makeText(mFragment.getActivity().getApplicationContext()
                            ,mFragment.getActivity().getResources().getString(R.string.result_ex_toast_query_no)
                            ,Toast.LENGTH_LONG).show();
                }
            }
            mFragment.cancelTask();
        }
    }


    /**
     * 查询完显示结果
     */
    private void showExResultList(){
        isCkShow = false;
        String time = "";
        if(null != mExperiments && mExperiments.size()>0){
            time = DateFormatUtil.getDate(mExperiments.get(0).getStartTime());
        }
        mSearchTimeTv.setText(time);
       // index = 0;//重新查询的才显示
        mExAdapter.setIndex(index);
        mExAdapter.setExperimentList(mExperiments);
        mExAdapter.notifyDataSetChanged();
    }

    private void getPageCountNum(){
        if(experimentCount >0){
            if(experimentCount%10==0){
                pageCount =  (int)experimentCount/10;
            }else{
                pageCount = (int)experimentCount/10 + 1;
            }

        }else {
            pageCount = 1;
        }
    }

    private void getAllRecordTemplate(){
        if(null != templateList){
            templateList.clear();
            if(null != mExperiments && mExperiments.size()>0){
               for(ExperimentModel experimentModel :mExperiments){
                   String templateName = experimentModel.getTestItem();
                   if(!templateList.contains(templateName)){
                       templateList.add(templateName);
                       Collections.sort(templateList);
                   }
               }
            }
        }
    }

    private void showNoticeDialog(String message){
        if(null != noticeDialog){
            noticeDialog.dismiss();
            noticeDialog =null;
        }
        if(null == noticeDialog){
            noticeDialog =new NoticeDialog(getActivity(),R.style.Dialog);
            noticeDialog.setMessage(message);
            noticeDialog.setOnOkClickListener(new NoticeDialog.OnOkClickListener() {
                @Override
                public void onConfirmClick() {
                    noticeDialog.dismiss();
                    deleteExperimentAction();
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
