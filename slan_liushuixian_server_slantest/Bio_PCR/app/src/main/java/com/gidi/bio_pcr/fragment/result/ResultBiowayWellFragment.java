package com.gidi.bio_pcr.fragment.result;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
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
import com.gidi.bio_pcr.activity.ResultActivity;
import com.gidi.bio_pcr.adapter.ChoiceTemplateAdapter;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.bean.QueryCondition;
import com.gidi.bio_pcr.bioway.activity.BiowayWellDetailActivity;
import com.gidi.bio_pcr.bioway.adapter.BiowayWellAdapter;
import com.gidi.bio_pcr.db.BioWayWellResult;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.listener.KeyBoardActionListener;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomDialog;
import com.gidi.bio_pcr.view.NoticeDialog;
import com.gidi.bio_pcr.view.SystemKeyBoardEditText;
import com.paul.pipeline.activity.viewmodel.PipeMainViewModel;
import com.paul.pipeline.data.ConstantData;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class ResultBiowayWellFragment extends BaseFragment implements View.OnClickListener {

  //  private ResultActivity resultActivity;
    private ListView resultRv;
    private List<BioWayWellResult> biowayWellResults;
    private List<BioWayWellResult> selectWellResults;
    private Button searchBtn;
    private ImageView mItemArrowImg;
    private ImageView mTimeArrowImg;
    private TextView mSearchTimeTv;
    private TextView mTestItemTv;
    private CustomDialog mDatePickerDialog;
    private CustomDialog mTempalateDialog;//容量
    private BiowayWellAdapter wellAdapter;
    private SystemKeyBoardEditText mRegentCodeEt;
    private SystemKeyBoardEditText mIDcardEt;

    private ChoiceTemplateAdapter choiceTemplateAdapter;
    private List<String> templateList;
    private ResultSearchAsyncTask resultSearchAsyncTask;
    private String choiceItem;
    private String searchTime;
    private int year,month,day;
    private TextView mFirstPageTv;
    private TextView mPrePageTv;
    private TextView mNextPageTv;
    private TextView mLastPageTv;
    private TextView mAllCheckTv,mDeleteTv,mCancelTv;

    private int index = 0;//初始的页数
    private int pageCount = 1;//总共的页数
    private boolean isAllCheck = false;
    private boolean isCkShow = false;
//    private boolean isFirst= true;

    private int PAGE_SIZE = 10;//每一页查询10个
    private boolean isQueryCondition = false;//是否条件查询

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_bioway_result_well;
    }

    @Override
    protected void initView(View rootView) {

        resultRv = (ListView) rootView.findViewById(R.id.result_bioway_list_recycleview);
        searchBtn = (Button)rootView.findViewById(R.id.result_bioway_search_btn);
        mRegentCodeEt = (SystemKeyBoardEditText)rootView.findViewById(R.id.result_bioway_search_regent_et);
        mIDcardEt = (SystemKeyBoardEditText)rootView.findViewById(R.id.result_bioway_search_idcard_et);
        mItemArrowImg = (ImageView)rootView.findViewById(R.id.result_bioway_item_arrow_img);
        mTimeArrowImg = (ImageView)rootView.findViewById(R.id.result_bioway_time_arrow_img);
        mSearchTimeTv = (TextView)rootView.findViewById(R.id.result_bioway_search_time_tv);
        mTestItemTv = (TextView)rootView.findViewById(R.id.result_bioway_search_item_tv);
        ImageView clearImg = (ImageView)rootView.findViewById(R.id.result_bioway_time_clear_img);
        clearImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchTimeTv.setText("");
            }
        });

        ImageView clearItem = (ImageView)rootView.findViewById(R.id.result_bioway_item_clear_img);
        clearItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTestItemTv.setText("");
            }
        });

        mFirstPageTv = (TextView)rootView.findViewById(R.id.result_bioway_list_first_tv);
        mPrePageTv = (TextView)rootView.findViewById(R.id.result_bioway_list_prepage_tv);
        mNextPageTv = (TextView)rootView.findViewById(R.id.result_bioway_list_nextpage_tv);
        mLastPageTv = (TextView)rootView.findViewById(R.id.result_bioway_list_lastpage_tv);
        mAllCheckTv = (TextView) rootView.findViewById(R.id.result_bioway_list_allcheck_tv);
        mDeleteTv = (TextView) rootView.findViewById(R.id.result_bioway_list_delete_tv);
        mCancelTv = (TextView) rootView.findViewById(R.id.result_bioway_list_cancel_tv);

//        if(null != resultActivity){
//            if(resultActivity.isSettingIn){
//                mDeleteTv.setVisibility(View.VISIBLE);
//            }else {
//                mDeleteTv.setVisibility(View.GONE);
//            }
//        }
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
        mRegentCodeEt.setOnKeyboardActionListener(new KeyBoardActionListener() {

            @Override
            public void onTextChange(Editable editable) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onComplete() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onClearAll() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onClear() {
                // TODO Auto-generated method stub
            }
        });
        mIDcardEt.setOnKeyboardActionListener(new KeyBoardActionListener() {

            @Override
            public void onTextChange(Editable editable) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onComplete() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onClearAll() {
                // TODO Auto-generated method stub

            }

            @Override
            public void onClear() {
                // TODO Auto-generated method stub
            }
        });

        wellAdapter.setOnItemCliclListener(new BiowayWellAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                //弹出对话框确定
                //得到每个信息的目标值
                BioWayWellResult slanWellResult = biowayWellResults.get(position);
                if(mainViewModel!=null) {
                    mainViewModel.onNavClicked(ConstantData.NAV_WELLDETAIL);
                    mainViewModel.setPcrBioWayWellResult(slanWellResult);
                }
//                Intent intent = new Intent(getActivity(), BiowayWellDetailActivity.class);
//                intent.putExtra("item",slanWellResult);
//                startActivity(intent);
            }

            //长按
            @Override
            public void onItemLongClick(int position) {
               if(!isCkShow){
                   isCkShow = true;
                   //显示全选按钮
                   mAllCheckTv.setVisibility(View.VISIBLE);
                   mCancelTv.setVisibility(View.VISIBLE);
                   for(BioWayWellResult bean : biowayWellResults){
                       bean.setShow(true);
                   }
                   wellAdapter.setDeviceList(biowayWellResults);
                   wellAdapter.notifyDataSetChanged();
               }
            }

            @Override
            public void onItemCheck(int position) {
                BioWayWellResult wellResult = biowayWellResults.get(position);
                if(wellResult.isCheck()){
                    wellResult.setCheck(false);
                }else {
                    wellResult.setCheck(true);
                }
                if (wellResult.isCheck() && !selectWellResults.contains(wellResult)) {
                    selectWellResults.add(wellResult);
                } else if (!wellResult.isCheck() && selectWellResults.contains(wellResult)) {
                    selectWellResults.remove(wellResult);
                }
                wellAdapter.setDeviceList(biowayWellResults);
            }
        });


    }

    protected void initData() {
        templateList = new ArrayList<String>();
        wellAdapter = new BiowayWellAdapter(getActivity().getApplicationContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setReverseLayout(false);
        biowayWellResults = new ArrayList<BioWayWellResult>();
        selectWellResults = new ArrayList<BioWayWellResult>();
        wellAdapter.setDeviceList(biowayWellResults);
        resultRv.setAdapter(wellAdapter);
        wellAdapter.notifyDataSetChanged();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
    //    resultActivity = (ResultActivity) activity;
//    }


    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!hidden){
            initMain();
        }else{
            cancelTask();
        }
    }
    private void initMain(){
        isAllCheck = false;
        isCkShow = false;
        mAllCheckTv.setVisibility(View.INVISIBLE);
        mCancelTv.setVisibility(View.INVISIBLE);
        researchWellResults(0,PAGE_SIZE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(null != biowayWellResults){
            biowayWellResults.clear();
            biowayWellResults = null;
        }
        if(null != selectWellResults){
            selectWellResults.clear();
            selectWellResults = null;
        }
        dismissDialog();
        cancelTask();
     //   resultActivity = null;
    }



    private void cancelTask(){
        hideProgressDialog();
        if(null != resultSearchAsyncTask){
            resultSearchAsyncTask.cancel(true);
            resultSearchAsyncTask = null;
        }

    }

    private LoadingDialog loadingDialog ;
    public void showProgressDialog(String message){
        if(null == loadingDialog){
            loadingDialog = new LoadingDialog(getActivity(), message);
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }
    }

    /*
     * 隐藏提示加载
     */
    public void hideProgressDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }


    private void dismissDialog(){
        if(null != mTempalateDialog){
            mTempalateDialog.dismiss();
            mTempalateDialog  = null;
        }
        if(null != mDatePickerDialog){
            mDatePickerDialog.dismiss();
            mDatePickerDialog = null;
        }
        if(null != loadingDialog){
            loadingDialog.dismiss();
            loadingDialog = null;
        }
        if(noticeDialog != null){
            noticeDialog.dismiss();
            noticeDialog = null;
        }
    }

    private void getAllTemplates(){
        templateList.clear();
        File[] files = new File(FileUtils.FULL_PROCESS_TEMPLATE_PATH).listFiles();//文件夹下的所有文件
        if(files != null && files.length>0){
            for(int i=0;i<files.length;i++){
                if(null != templateList){
                    templateList.add(files[i].getName());
                    Collections.sort(templateList);
                }
            }
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.result_bioway_search_btn) {
            String regentCode = mRegentCodeEt.getText().toString().trim();
            String idCard = mIDcardEt.getText().toString().trim();
            String testItem = mTestItemTv.getText().toString().trim();
            String searchTime = mSearchTimeTv.getText().toString().trim();
            if(StringUtil.isEmpty(regentCode) && StringUtil.isEmpty(idCard)
                    && StringUtil.isEmpty(testItem) && StringUtil.isEmpty(searchTime)){
                researchWellResults(0,PAGE_SIZE);
                index = 0;
                isQueryCondition = false;
            }else{
                searchWellResult(0,PAGE_SIZE);
                index  = 0;
                isQueryCondition = true;
            }
        } else if (id == R.id.result_bioway_item_arrow_img) {//显示所有的项目
            showTemplateDialog();
        } else if (id == R.id.result_bioway_time_arrow_img) {//弹出时间选择框
            String endDate = mSearchTimeTv.getText().toString().trim();
            if(!StringUtil.isEmpty(endDate)){
                year = DateFormatUtil.getYear(endDate);
                month = DateFormatUtil.getMonth(endDate);
                day = DateFormatUtil.getDay(endDate);
                if(year != -1 && month != -1 && day != -1){
                    showDatePickerDialog(year, month, day);
                }
            }else{
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_ex_toast_no_time),Toast.LENGTH_LONG).show();
                return;
            }

        }else if(id == R.id.result_bioway_list_first_tv){
            //首页
            onFirstPage();

        }else if(id == R.id.result_bioway_list_prepage_tv){
            onPrePage();

        }else if(id == R.id.result_bioway_list_nextpage_tv){
            onNextPage();

        }else if(id == R.id.result_bioway_list_lastpage_tv){
            onLastPage();

        }else if(id == R.id.result_bioway_list_delete_tv){
            if(null != selectWellResults && selectWellResults.size()>0){
                showNoticeDialog(getActivity().getResources().getString(R.string.result_well_dialog_delete_well_result)
                        + "" + selectWellResults.size()
                        +getActivity().getResources().getString(R.string.result_export_dialog_message_items));
            }else {
                Toast.makeText(getActivity().getApplicationContext(),
                        getActivity().getResources().getString(R.string.result_well_toast_delete_well_result),Toast.LENGTH_LONG).show();
            }

        }else if(id == R.id.result_bioway_list_allcheck_tv){
            if(null != biowayWellResults && biowayWellResults.size()>0){
                if(isAllCheck){
                    isAllCheck = false;
                    for(BioWayWellResult result : biowayWellResults){
                        result.setCheck(false);
                    }
                    wellAdapter.setDeviceList(biowayWellResults);
                    selectWellResults.clear();
                }else {
                    isAllCheck = true;
                    for(BioWayWellResult result : biowayWellResults){
                        result.setCheck(true);
                    }
                    wellAdapter.setDeviceList(biowayWellResults);
                    selectWellResults.clear();
                    for(int i=  0;i<biowayWellResults.size();i++){
                        selectWellResults.add(biowayWellResults.get(i));
                    }
                }
            }

        }else if(id == R.id.result_bioway_list_cancel_tv){
            for(BioWayWellResult result : biowayWellResults){
                result.setShow(false);
                result.setCheck(false);
            }
            wellAdapter.setDeviceList(biowayWellResults);
            selectWellResults.clear();
            mCancelTv.setVisibility(View.INVISIBLE);
            mAllCheckTv.setVisibility(View.INVISIBLE);
            isCkShow = false;
        }
    }

    private void onFirstPage(){
        if(index == 0){
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_first_page),Toast.LENGTH_SHORT).show();
        }else{
            index = 0;
        }
        wellAdapter.setIndex(index);
        if(isQueryCondition){
            //条件查询
            searchWellResult(index*PAGE_SIZE,PAGE_SIZE);
        }else{
            researchWellResults(index*PAGE_SIZE,PAGE_SIZE);
        }
    }

    private void onPrePage(){
        if(index == 0){
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_first_page_no_previous),Toast.LENGTH_LONG).show();
        }else {
            index--;
            wellAdapter.setIndex(index);
        }
        if(isQueryCondition){
            //条件查询
            searchWellResult(index*PAGE_SIZE,PAGE_SIZE);
        }else{
            researchWellResults(index*PAGE_SIZE,PAGE_SIZE);
        }
    }

    private void onNextPage(){
        index = (index+1);
        if(index<pageCount){
            wellAdapter.setIndex(index);
        }else{
            index = pageCount -1;
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_last_page_no_next),Toast.LENGTH_SHORT).show();
        }
        if(isQueryCondition){
            //条件查询
            searchWellResult(index*PAGE_SIZE,PAGE_SIZE);
        }else{
            researchWellResults(index*PAGE_SIZE,PAGE_SIZE);
        }
    }

    private void onLastPage(){
        if(index == pageCount -1){
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_ex_toast_last_page),Toast.LENGTH_SHORT).show();
        }else{
            index = pageCount -1;
        }
        wellAdapter.setIndex(index);
        // Log.i("Test","onLastPage index" + index );
        if(isQueryCondition){
            //条件查询
            searchWellResult(index*PAGE_SIZE,PAGE_SIZE);
        }else{
            researchWellResults(index*PAGE_SIZE,PAGE_SIZE);
        }
    }

    //////////////////////////////////////数据操作/////////////////////////////////////////////
    //删除选中的孔位信息
    private void deleteWellResult(List<BioWayWellResult> selectWellResults){
        int del_result =  LiteORMCtrlMgr.getInstance().delBiowayWellResult(selectWellResults);
        mCancelTv.setVisibility(View.INVISIBLE);
        mAllCheckTv.setVisibility(View.INVISIBLE);
        isCkShow = false;
        isAllCheck = false;
        if(del_result>0){
            //删除成功重新查询
            Log.i("ResultWellSLANFragment","del_result---" + del_result);
            if(isQueryCondition){
                //条件查询
                searchWellResult(index*PAGE_SIZE,PAGE_SIZE);
            }else{
                researchWellResults(index*PAGE_SIZE,PAGE_SIZE);
            }
        }else{
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_well_toast_delete_fail),Toast.LENGTH_LONG).show();
        }
    }



    private long wellResultCount = 0;
    public static class ResultSearchAsyncTask extends AsyncTask<Void, Integer, List<BioWayWellResult>> {

        private WeakReference<ResultBiowayWellFragment> weakfrag;
        private QueryCondition queryCondition;

        public ResultSearchAsyncTask(ResultBiowayWellFragment fragment, QueryCondition queryCondition){
            weakfrag = new WeakReference<ResultBiowayWellFragment>(fragment);
            this.queryCondition = queryCondition;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            final ResultBiowayWellFragment mFragment = weakfrag.get();
            super.onPreExecute();
            if(null == mFragment){
                return;
            }
            if(null !=mFragment.biowayWellResults ){
                mFragment.biowayWellResults.clear();
            }

        }

        //后台进程的执行
        @Override
        protected List<BioWayWellResult> doInBackground(Void... arg0) {
            // TODO Auto-generated method stub
            final ResultBiowayWellFragment mFragment = weakfrag.get();
            mFragment.wellResultCount = LiteORMCtrlMgr.getInstance().getBiowayWellResultNum(queryCondition);
            mFragment.biowayWellResults = LiteORMCtrlMgr.getInstance().searchBiowayWellResult(queryCondition,queryCondition.getStart(),queryCondition.getLength());
            Log.i("Bioway","mFragment.wellResultCount" + mFragment.wellResultCount);
            Log.i("Bioway","mFragment.biowayWellResults" + mFragment.biowayWellResults.size());
            return  mFragment.biowayWellResults;
        }

        @Override
        protected void onPostExecute(List<BioWayWellResult> result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            final ResultBiowayWellFragment mFragment = weakfrag.get();
            if(null != mFragment.biowayWellResults){
                mFragment.showWellResultList();
                mFragment.getPageCountNum();//20250211
//                if(mFragment.isFirst){
//                    mFragment.isFirst = false;
                    mFragment.getAllRecordTemplate();
//                }
                if(mFragment.biowayWellResults.size() == 0){
                    Toast.makeText(mFragment.getActivity().getApplicationContext(),
                            mFragment.getActivity().getResources().getString(R.string.result_ex_toast_query_no),Toast.LENGTH_LONG).show();
                }
            }
            mFragment.cancelTask();
        }
    }

    private void showWellResultList(){
        isCkShow = false;
//        if(isFirst){
            String time = "";
            if(null != biowayWellResults && biowayWellResults.size()>0){
                time = DateFormatUtil.getDate(biowayWellResults.get(0).getStartTime());
            }
            mSearchTimeTv.setText(time);
//        }
        //  index = 0;
        wellAdapter.setIndex(index);
        wellAdapter.setDeviceList(biowayWellResults);
    }

    private void getPageCountNum(){
        if(wellResultCount>0){
            if(wellResultCount%10==0){
                pageCount =  (int)wellResultCount/10;
            }else {
                pageCount =  (int)wellResultCount/10 + 1;
            }
        }else {
            pageCount =  1;
        }
        Log.i("Test","pageCount----" + pageCount);
    }

    private void researchWellResults(int start, int length){
        Log.i("Test","researchWellResults");
        //线程查询
//        isFirst = true;
          showProgressDialog(getActivity().getResources().getString(R.string.dialog_message_query_data));
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setStart(start);
        queryCondition.setLength(length);
        if(null == resultSearchAsyncTask) {
            resultSearchAsyncTask = new ResultSearchAsyncTask(this, queryCondition);
            resultSearchAsyncTask.execute();
        }
    }

    private void searchWellResult(int start, int length){
        Log.i("Test","searchWellResult");
        String regentCode = mRegentCodeEt.getText().toString().trim();
        String idCard = mIDcardEt.getText().toString().trim();
        String testItem = mTestItemTv.getText().toString().trim();
        String searchTime = mSearchTimeTv.getText().toString().trim();
        QueryCondition queryCondition = new QueryCondition();
        queryCondition.setIdcardNum(idCard);
        queryCondition.setRegentCode(regentCode);
        queryCondition.setTestItem(testItem);
        queryCondition.setTestStartTime(searchTime);
        queryCondition.setStart(start);
        queryCondition.setLength(length);
        if(null == resultSearchAsyncTask){
            resultSearchAsyncTask = new ResultSearchAsyncTask(this,queryCondition);
            resultSearchAsyncTask.execute();
        }
    }

    /**
     * 获取所有记录的项目
     */
    private void getAllRecordTemplate(){
        if(null != templateList){
            templateList.clear();
            if(null != biowayWellResults && biowayWellResults.size()>0){
                for(BioWayWellResult wellResult :biowayWellResults){
                    String templateName = wellResult.getTestItem();
                    if(!templateList.contains(templateName)){
                        templateList.add(templateName);
                        Collections.sort(templateList);
                    }
                }
            }
        }
    }

    private void showDatePickerDialog(int year, int month, int dayOfMonth){
        if(null != mDatePickerDialog){
            mDatePickerDialog.dismiss();
            mDatePickerDialog = null;
        }
        if(null == mDatePickerDialog){
            mDatePickerDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View pickView = mInflater.inflate(R.layout.dialog_datepicker_dialog, null);
            final DatePicker datePicker = (DatePicker)pickView.findViewById(R.id.date_picker);
            datePicker.init(year, month, dayOfMonth, new DatePicker.OnDateChangedListener() {

                @Override
                public void onDateChanged(DatePicker view, int year, int month, int day) {
                    // TODO Auto-generated method stub
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month, day);
                }
            });
            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(pickView);
            builder.setTitle(this.getResources().getString(R.string.dialog_title_choice_date));
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                   // Log.i("AlarmFragment", "Year--" + datePicker.getYear() + " month---" + datePicker.getMonth() + "day---" + datePicker.getDayOfMonth());
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

    /**
     * 显示模板下容量选择
     */
    private void showTemplateDialog(){
        if(null != mTempalateDialog){
            mTempalateDialog.dismiss();
            mTempalateDialog = null;
        }
        if(null == mTempalateDialog){
            mTempalateDialog = new CustomDialog(getActivity());
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
                    mTempalateDialog.dismiss();
                }
            });

            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mTempalateDialog.dismiss();
                    mTempalateDialog = null;
                }
            });
            mTempalateDialog = builder.create();
            mTempalateDialog.show();
        }
    }

    private void updateDate(final int year, final int month, final int day){
        this.year = year;
        this.month = month + 1;
        this.day = day;
        mSearchTimeTv.setText((String.format("%04d-%02d-%02d",this.year,this.month,this.day)));
        this.searchTime = mSearchTimeTv.getText().toString().trim();

    }

    private NoticeDialog noticeDialog;
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
                    deleteWellResultAction();
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

    private void deleteWellResultAction(){
        if (selectWellResults != null && selectWellResults.size() > 0) {
            deleteWellResult(selectWellResults);
            selectWellResults.clear();
        } else {
            Toast.makeText(getActivity().getApplicationContext(),
                    getActivity().getResources().getString(R.string.result_well_toast_delete_well_result), Toast.LENGTH_SHORT).show();
        }
        isCkShow = false;
    }
}
