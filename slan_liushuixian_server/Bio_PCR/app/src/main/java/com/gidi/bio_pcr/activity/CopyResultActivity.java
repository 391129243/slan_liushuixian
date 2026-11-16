package com.gidi.bio_pcr.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.adapter.FileListAdapter;
import com.gidi.bio_pcr.base.BaseActivity;
import com.gidi.bio_pcr.bioway.BiowayManager;
import com.gidi.bio_pcr.pcr.OnExperimentCallback;
import com.gidi.bio_pcr.pcr.save.OnSaveExperimentListener;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.FileUtils;
import com.gidi.bio_pcr.util.LoadingDialog;
import com.gidi.bio_pcr.util.StringUtil;
import com.gidi.bio_pcr.view.CustomLargeDialog;
import com.winters.boweilibrary.BoWei.BoWeiService;
import com.winters.boweilibrary.BoWei.ExperimentData.Experiment;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CopyResultActivity extends BaseActivity implements View.OnClickListener, OnExperimentCallback {
    private RelativeLayout mTitleLayout;
    private TextView mTitleTv;
    private ImageView mBackImg;
    private ListView resultRv;
    private TextView copyTv;
    private FileListAdapter<File> showResultAdapter;
    private List<File> resultFiles;
    private int click_pos = -1;
    private ImageView mFirstCkImg,mSecondCkImg,mThirdCkImg,mFourthCkImg,mFifthCkImg,mSixCkImg,mSevenCkImg,
        mEightCkImg,mNightCkImg;
    private TextView mStartDate_1_Tv,mStartDate_2_Tv,mStartDate_3_Tv,mStartDate_4_Tv,mStartDate_5_Tv,mStartDate_6_Tv,
            mStartDate_7_Tv,mStartDate_8_Tv,mStartDate_9_Tv;

    private TextView mEndDate_1_Tv,mEndDate_2_Tv,mEndDate_3_Tv,mEndDate_4_Tv,mEndDate_5_Tv,mEndDate_6_Tv,mEndDate_7_Tv,
            mEndDate_8_Tv,mEndDate_9_Tv;
    private TextView mNum_1_Tv,mNum_2_Tv,mNum_3_Tv,mNum_4_Tv,mNum_5_Tv,mNum_6_Tv,mNum_7_Tv,mNum_8_Tv,mNum_9_Tv;

    private LinearLayout mFirstCkLayout,mSecondCkLayout,mThirdCkLayout,mFourthCkLayout,mFifthCkLayout,
            mSixCkLayout,mSevenCkLayout,mEighthCkLayout,mNigthCkLayout;
    private LinearLayout mFirstStartLayout,mSecondStartLayout,mThirdStartLayout,mFourthStartLayout,mFifthStartLayout,
            mSixStartLayout,mSevenStartLayout,mEighthStartLayout,mNigthStartLayout;
    private LinearLayout mFirstEndLayout,mSecondEndLayout,mThirdEndLayout,mFourthEndLayout,mFifthEndLayout,
            mSixEndLayout,mSevenEndLayout,mEighthEndLayout,mNigthEndLayout;
    private boolean isFirstCk,isSecondCk,isThirdCk,isFouthCk,isFifthCk,isSixCk,isSevenCk,isEightCk,
        isNighthCk;
    private int firstStartYear,firstStartMonth,firstStartDay,firstStartHour,firstStartMinute;
    private int firstEndYear,firstEndMonth,firstEndDay,firstEndHour,firstEndMinute;

    private int secondStartYear,secondStartMonth,secondStartDay,secondStartHour,secondStartMinute;
    private int secondEndYear,secondEndMonth,secondEndDay,secondEndHour,secondEndMinute;

    private int thirdStartYear,thirdStartMonth,thirdStartDay,thirdStartHour,thirdStartMinute;
    private int thirdEndYear,thirdEndMonth,thirdEndDay,thirdEndHour,thirdEndMinute;
    private int fourthStartYear,fourthStartMonth,fourthStartDay,fourthStartHour,fourthStartMinute;
    private int fifthStartYear,fifthStartMonth,fifthStartDay,fifthStartHour,fifthStartMinute;
    private int sixthStartYear,sixthStartMonth,sixthStartDay,sixthStartHour,sixthStartMinute;
    private int seventhStartYear,seventhStartMonth,seventhStartDay,seventhStartHour,seventhStartMinute;
    private int eighthStartYear,eighthStartMonth,eighthStartDay,eighthStartHour,eighthStartMinute;
    private int nighthStartYear,nighthStartMonth,nighthStartDay,nighthStartHour,nighthStartMinute;
    private int fourthEndYear,fourthEndMonth,fourthEndDay,fourthEndHour,fourthEndMinute;
    private int fifthEndYear,fifthEndMonth,fifthEndDay,fifthEndHour,fifthEndMinute;
    private int sixthEndYear,sixthEndMonth,sixthEndDay,sixthEndHour,sixthEndMinute;
    private int seventhEndYear,seventhEndMonth,seventhEndDay,seventhEndHour,seventhEndMinute;
    private int eighthEndYear,eighthEndMonth,eighthEndDay,eighthEndHour,eighthEndMinute;
    private int nighthEndYear,nighthEndMonth,nighthEndDay,nighthEndHour,nighthEndMinute;
    private String RESULT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Secret/results/";
    @Override
    protected int getLayoutId() {
        return R.layout.module_activity_copy_result;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {

        mFirstCkImg = (ImageView)findViewById(R.id.copy_result_check_img_1);
        mSecondCkImg= (ImageView)findViewById(R.id.copy_result_check_img_2);
        mThirdCkImg = (ImageView)findViewById(R.id.copy_result_check_img_3);
        mFourthCkImg= (ImageView)findViewById(R.id.copy_result_check_img_4);
        mFifthCkImg = (ImageView)findViewById(R.id.copy_result_check_img_5);
        mSixCkImg= (ImageView)findViewById(R.id.copy_result_check_img_6);
        mSevenCkImg = (ImageView)findViewById(R.id.copy_result_check_img_7);
        mEightCkImg= (ImageView)findViewById(R.id.copy_result_check_img_8);
        mNightCkImg= (ImageView)findViewById(R.id.copy_result_check_img_9);

        mStartDate_1_Tv= (TextView) findViewById(R.id.copy_result_startdate_1_tv);
        mStartDate_2_Tv= (TextView)findViewById(R.id.copy_result_startdate_2_tv);
        mStartDate_3_Tv= (TextView)findViewById(R.id.copy_result_startdate_3_tv);
        mStartDate_4_Tv= (TextView)findViewById(R.id.copy_result_startdate_4_tv);
        mStartDate_5_Tv= (TextView) findViewById(R.id.copy_result_startdate_5_tv);
        mStartDate_6_Tv= (TextView)findViewById(R.id.copy_result_startdate_6_tv);
        mStartDate_7_Tv= (TextView)findViewById(R.id.copy_result_startdate_7_tv);
        mStartDate_8_Tv= (TextView)findViewById(R.id.copy_result_startdate_8_tv);
        mStartDate_9_Tv= (TextView)findViewById(R.id.copy_result_startdate_9_tv);

        mEndDate_1_Tv= (TextView) findViewById(R.id.copy_result_enddate_1_tv);
        mEndDate_2_Tv= (TextView)findViewById(R.id.copy_result_enddate_2_tv);
        mEndDate_3_Tv= (TextView)findViewById(R.id.copy_result_enddate_3_tv);
        mEndDate_4_Tv= (TextView)findViewById(R.id.copy_result_enddate_4_tv);
        mEndDate_5_Tv= (TextView) findViewById(R.id.copy_result_enddate_5_tv);
        mEndDate_6_Tv= (TextView)findViewById(R.id.copy_result_enddate_6_tv);
        mEndDate_7_Tv= (TextView)findViewById(R.id.copy_result_enddate_7_tv);
        mEndDate_8_Tv= (TextView)findViewById(R.id.copy_result_enddate_8_tv);
        mEndDate_9_Tv= (TextView)findViewById(R.id.copy_result_enddate_9_tv);

        mNum_1_Tv = (TextView)findViewById(R.id.copy_result_1_tv);
        mNum_2_Tv = (TextView)findViewById(R.id.copy_result_2_tv);
        mNum_3_Tv = (TextView)findViewById(R.id.copy_result_3_tv);
        mNum_4_Tv = (TextView)findViewById(R.id.copy_result_4_tv);
        mNum_5_Tv = (TextView)findViewById(R.id.copy_result_5_tv);
        mNum_6_Tv = (TextView)findViewById(R.id.copy_result_6_tv);
        mNum_7_Tv = (TextView)findViewById(R.id.copy_result_7_tv);
        mNum_8_Tv = (TextView)findViewById(R.id.copy_result_8_tv);
        mNum_9_Tv = (TextView)findViewById(R.id.copy_result_9_tv);

        mFirstStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_1_starttime_layout) ;
        mFirstEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_1_endtime_layout) ;
        mSecondStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_2_starttime_layout) ;
        mSecondEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_2_endtime_layout) ;
        mThirdStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_3_starttime_layout) ;
        mThirdEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_3_endtime_layout) ;
        mFourthStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_4_starttime_layout) ;
        mFourthEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_4_endtime_layout) ;
        mFifthStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_5_starttime_layout) ;
        mFifthEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_5_endtime_layout) ;
        mSixStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_6_starttime_layout) ;
        mSixEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_6_endtime_layout) ;
        mSevenStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_7_starttime_layout) ;
        mSevenEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_7_endtime_layout) ;
        mEighthStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_8_starttime_layout) ;
        mEighthEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_8_endtime_layout) ;
        mNigthStartLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_9_starttime_layout) ;
        mNigthEndLayout = (LinearLayout)findViewById(R.id.copy_result_insert_time_9_endtime_layout) ;



        mFirstCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_1) ;
        mSecondCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_2) ;
        mThirdCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_3) ;
        mFourthCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_4) ;
        mFifthCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_5) ;
        mSixCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_6) ;
        mSevenCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_7) ;
        mEighthCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_8) ;
        mNigthCkLayout = (LinearLayout)findViewById(R.id.copy_result_check_layout_9) ;

        mTitleLayout = (RelativeLayout)findViewById(R.id.copy_result_title_layout);
        mTitleTv = (TextView)mTitleLayout.findViewById(R.id.tab_title_tv);
        mTitleTv.setText(getResources().getString(R.string.copy_result));
        mBackImg = (ImageView)mTitleLayout.findViewById(R.id.tab_back_img);
        resultRv = (ListView) findViewById(R.id.copy_result_list);
        copyTv = (TextView)findViewById(R.id.copy_tv) ;
        showResultAdapter = new FileListAdapter<>(getApplicationContext());
        resultRv.setAdapter(showResultAdapter);
        resultFiles = new ArrayList<File>();
        File[] results = new File(RESULT_PATH).listFiles();
        if(results != null && results.length>0) {
            for (int i = 0; i < results.length; i++) {
                if (null != resultFiles) {
                    resultFiles.add(results[i]);
                    Collections.sort(resultFiles);
                }
            }
        }
        showResultAdapter.setFileList(resultFiles);
        showResultAdapter.notifyDataSetChanged();

        resultRv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(null != showResultAdapter){
                    showResultAdapter.setSelectedPosition(i);
                    click_pos = i;
                }
            }
        });

        mFirstCkLayout.setOnClickListener(this);
        mSecondCkLayout.setOnClickListener(this);
        mThirdCkLayout.setOnClickListener(this);
        mFourthCkLayout.setOnClickListener(this);
        mFifthCkLayout.setOnClickListener(this);
        mSixCkLayout.setOnClickListener(this);
        mSevenCkLayout.setOnClickListener(this);
        mEighthCkLayout.setOnClickListener(this);
        mNigthCkLayout.setOnClickListener(this);

        mFirstStartLayout.setOnClickListener(this);
        mSecondStartLayout.setOnClickListener(this);
        mThirdStartLayout.setOnClickListener(this);
        mFourthStartLayout.setOnClickListener(this);
        mFifthStartLayout.setOnClickListener(this);
        mSixStartLayout.setOnClickListener(this);
        mSevenStartLayout.setOnClickListener(this);
        mEighthStartLayout.setOnClickListener(this);
        mNigthStartLayout.setOnClickListener(this);
        mFirstEndLayout.setOnClickListener(this);
        mSecondEndLayout.setOnClickListener(this);
        mThirdEndLayout.setOnClickListener(this);
        mFourthEndLayout.setOnClickListener(this);
        mFifthEndLayout.setOnClickListener(this);
        mSixEndLayout.setOnClickListener(this);
        mSevenEndLayout.setOnClickListener(this);
        mEighthEndLayout.setOnClickListener(this);
        mNigthEndLayout.setOnClickListener(this);
    }

    @Override
    protected void initListener() {
        BiowayManager.getInstance().setOnExperimentCallback(this);
        mBackImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        copyTv.setOnClickListener(this);
    }

    @Override
    protected void initData() {
        isFirstCk = isSecondCk = isThirdCk = isFouthCk = isFifthCk = false;
        isSixCk = isSevenCk = isEightCk = isNighthCk = false;
        calendarDate = Calendar.getInstance();
        //初始化
        //显示第一个时间和日期
        firstStartHour = firstEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        firstStartMinute = firstEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_1_Tv.setText(
                String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        +" "+ String.format("%02d:%02d",this.firstStartHour, this.firstStartMinute));
        mEndDate_1_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        +" " +String.format("%02d:%02d",this.firstEndHour, this.firstEndMinute));
        if(!StringUtil.isEmpty(mStartDate_1_Tv.getText().toString())){
            firstStartYear= firstEndYear = DateFormatUtil.getYear(mStartDate_1_Tv.getText().toString());
            firstStartMonth = firstEndMonth  = DateFormatUtil.getMonth(mStartDate_1_Tv.getText().toString());
            firstStartDay = firstEndDay  = DateFormatUtil.getDay(mStartDate_1_Tv.getText().toString());
        }



        secondStartHour = secondEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        secondStartMinute = secondEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_2_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.secondStartHour, this.secondStartMinute));
        mEndDate_2_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.secondEndHour, this.secondEndMinute));

        if(!StringUtil.isEmpty(mStartDate_2_Tv.getText().toString())){
            secondStartYear= secondEndYear = DateFormatUtil.getYear(mStartDate_2_Tv.getText().toString());
            secondStartMonth = secondEndMonth  = DateFormatUtil.getMonth(mStartDate_2_Tv.getText().toString());
            secondStartDay = secondEndDay  = DateFormatUtil.getDay(mStartDate_2_Tv.getText().toString());
        }

        thirdStartHour = thirdEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        thirdStartMinute = thirdEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_3_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.thirdStartHour, this.thirdStartMinute));
        mEndDate_3_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.thirdEndHour, this.thirdEndMinute));

        if(!StringUtil.isEmpty(mStartDate_3_Tv.getText().toString())){
            thirdStartYear= thirdEndYear = DateFormatUtil.getYear(mStartDate_3_Tv.getText().toString());
            thirdStartMonth = thirdEndMonth  = DateFormatUtil.getMonth(mStartDate_3_Tv.getText().toString());
            thirdStartDay = thirdEndDay  = DateFormatUtil.getDay(mStartDate_3_Tv.getText().toString());
        }


        fourthStartHour = fourthEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        fourthStartMinute = fourthEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_4_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.fourthStartHour, this.fourthStartMinute));
        mEndDate_4_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.fourthEndHour, this.fourthEndMinute));
        if(!StringUtil.isEmpty(mStartDate_4_Tv.getText().toString())){
            fourthStartYear= fourthEndYear = DateFormatUtil.getYear(mStartDate_4_Tv.getText().toString());
            fourthStartMonth = fourthEndMonth  = DateFormatUtil.getMonth(mStartDate_4_Tv.getText().toString());
            fourthStartDay = fourthEndDay  = DateFormatUtil.getDay(mStartDate_4_Tv.getText().toString());
        }


        fifthStartHour = fifthEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        fifthStartMinute = fifthEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_5_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        +" " + String.format("%02d:%02d",this.fifthStartHour, this.fifthStartMinute));
        mEndDate_5_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.fifthEndHour, this.fifthEndMinute));
        if(!StringUtil.isEmpty(mStartDate_5_Tv.getText().toString())){
            fifthStartYear= fifthEndYear = DateFormatUtil.getYear(mStartDate_5_Tv.getText().toString());
            fifthStartMonth = fifthEndMonth  = DateFormatUtil.getMonth(mStartDate_5_Tv.getText().toString());
            fifthStartDay = fifthEndDay  = DateFormatUtil.getDay(mStartDate_5_Tv.getText().toString());
        }




        sixthStartHour = sixthEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        sixthStartMinute = sixthEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_6_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.sixthStartHour, this.sixthStartMinute));
        mEndDate_6_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        +" " + String.format("%02d:%02d",this.sixthEndHour, this.sixthEndMinute));
        if(!StringUtil.isEmpty(mStartDate_6_Tv.getText().toString())){
            sixthStartYear=  sixthEndYear = DateFormatUtil.getYear(mStartDate_6_Tv.getText().toString());
            sixthStartMonth = sixthEndMonth  = DateFormatUtil.getMonth(mStartDate_6_Tv.getText().toString());
            sixthStartDay = sixthEndDay  = DateFormatUtil.getDay(mStartDate_6_Tv.getText().toString());
            Log.i("CopyResultActivity" ,"sixthStartYear"+ sixthStartYear);
            Log.i("CopyResultActivity" ,"sixthStartMonth"+ sixthStartMonth);
        }


        seventhStartHour = seventhEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        seventhStartMinute = seventhEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_7_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        +" " + String.format("%02d:%02d",this.seventhStartHour, this.seventhStartMinute));
        mEndDate_7_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        +" " + String.format("%02d:%02d",this.seventhEndHour, this.seventhEndMinute));
        if(!StringUtil.isEmpty(mStartDate_7_Tv.getText().toString())){
            seventhStartYear=   seventhEndYear = DateFormatUtil.getYear(mStartDate_7_Tv.getText().toString());
            seventhStartMonth =  seventhEndMonth  = DateFormatUtil.getMonth(mStartDate_7_Tv.getText().toString());
            seventhStartDay =  seventhEndDay  = DateFormatUtil.getDay(mStartDate_7_Tv.getText().toString());
        }


        eighthStartHour = eighthEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        eighthStartMinute = eighthEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_8_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
         +" " + String.format("%02d:%02d",this.eighthStartHour, this.eighthStartMinute));
        mEndDate_8_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        +" " + String.format("%02d:%02d",this.eighthEndHour, this.eighthEndMinute));
        if(!StringUtil.isEmpty(mStartDate_8_Tv.getText().toString())){
            eighthStartYear=   eighthEndYear = DateFormatUtil.getYear(mStartDate_8_Tv.getText().toString());
            eighthStartMonth = eighthEndMonth  = DateFormatUtil.getMonth(mStartDate_8_Tv.getText().toString());
            eighthStartDay =  eighthEndDay  = DateFormatUtil.getDay(mStartDate_8_Tv.getText().toString());
        }


        nighthStartHour = nighthEndHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        nighthStartMinute = nighthEndMinute = calendarDate.get(Calendar.MINUTE);
        mStartDate_9_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        +" " + String.format("%02d:%02d",this.nighthStartHour, this.nighthStartMinute));
        mEndDate_9_Tv.setText(String.format("%04d-%02d-%02d",calendarDate.get(Calendar.YEAR),
                calendarDate.get(Calendar.MONTH)+1,calendarDate.get(Calendar.DAY_OF_MONTH))
        + " " + String.format("%02d:%02d",this.nighthEndHour, this.nighthEndMinute));
        if(!StringUtil.isEmpty(mStartDate_9_Tv.getText().toString())){
            nighthStartYear=   nighthEndYear = DateFormatUtil.getYear(mStartDate_9_Tv.getText().toString());
            nighthStartMonth = nighthEndMonth  = DateFormatUtil.getMonth(mStartDate_9_Tv.getText().toString());
            nighthStartDay =  nighthEndDay  = DateFormatUtil.getDay(mStartDate_9_Tv.getText().toString());
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BiowayManager.getInstance().releaseExperimentCallBack();
        if(null != resultFiles){
            resultFiles.clear();
            resultFiles = null;
        }

        if(null != mDateTimeDialog){
            mDateTimeDialog.dismiss();
            mDateTimeDialog = null;
        }
    }
    Random random = new Random();

    private LoadingDialog loadingDialog ;
    public void showProgressDialog(String message){
        if(null == loadingDialog){
            loadingDialog = new LoadingDialog(CopyResultActivity.this, message);
            loadingDialog.setCancelable(false);
            loadingDialog.show();
        }
    }
    public void hideProgressDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
            loadingDialog = null;
        }
    }


    //获取.PCR的文件名称
    private String getExperimentFilePath(String resultDir){
        File[] subFiles = new File(RESULT_PATH + resultDir).listFiles();
        if(subFiles != null && subFiles.length>0) {
            for (File subFile : subFiles) {
                if (subFile.getName().endsWith(".pcr")) {
                    return subFile.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    int copyResult=0;
    private int CopyCount = 0;
    /**
     * 存数据库文件夹都要
     * @param experiment
     * @param templateName
     * @param startDateTime
     * @param endDateTime
     */
    private void saveExperiment(Experiment experiment,
                                String templateName,
                                LocalDateTime startDateTime,
                                LocalDateTime endDateTime){
        String startTime = startDateTime.format(formatter);
        String endTime = endDateTime.format(formatter);
        String aftertime = DateFormatUtil.convertDateFormat(startTime);
        String subFirName = aftertime + "_" + templateName;
        saveResultToPCR(experiment,subFirName);
        String savePath =  FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;
      //  Log.i("CopyResultActivity","saveExperiment" + templateName + " startTime" + startTime);
     //   Log.i("CopyResultActivity","endTime" + endTime);
        BiowayManager.getInstance().saveBiowayExResultToDB(experiment,savePath,startTime,endTime,templateName,"",null,null);
    }

    @Override
    public void onSaveExToDbResult(boolean result) {
        if(result){
            Log.i("CopyResultActivity","onSaveExToDbResult" + result +" ");
            copyResult++;
        }
        Log.i("CopyResultActivity","copyResult" + copyResult);
        Log.i("CopyResultActivity","CopyCount" + CopyCount);
        if(CopyCount == copyResult){
            copyResult = 0;
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.toast_copy_success),Toast.LENGTH_LONG).show();
            hideProgressDialog();
        }
    }

    @Override
    public void onParseFinish() {

    }

    @Override
    public void onSaveJsonResult(boolean result,String message) {

    }

    @Override
    public void onSecondParseFinish(boolean result) {

    }

    @Override
    public void onUpdateFinish(boolean result) {

    }


    public  void saveResultToPCR(Experiment experiment, String subFirName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
              //  Log.e(TAG, "saveResultToPCR");
                if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    return ;
                }
                if (experiment == null) {
                    return;
                }


                File dir = new File(FileUtils.BIOWAY_RESULT_PATH, subFirName);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                //////蒋老师意思，屏蔽测试
                String saveFileName = DateFormatUtil.getFileName();
                String savePath =  FileUtils.BIOWAY_RESULT_PATH + subFirName + File.separator;

                BoWeiService.getInstance()
                        .saveExperiment(experiment, saveFileName, savePath, new OnSaveExperimentListener() {
                            @Override
                            public void onSaveSuccess() {
                                Toast.makeText(getApplicationContext(),"保存成功",Toast.LENGTH_LONG).show();
                                Log.e("save","onSaveSuccess" );
                            }

                            @Override
                            public void onSaveFail() {
                                Toast.makeText(getApplicationContext(),"保存失败",Toast.LENGTH_LONG).show();
                                Log.e("save","onSaveFail" );
                            }
                        });
               // boolean save_result = BoWeiService.getInstance().saveExperiment(experiment,saveFileName,savePath);
               /* if(save_result){
                    //////FTP上报.PCR服务器
                    FtpUploadManager.getInstance().upLoadFile(savePath + saveFileName,saveFileName,savePath, SLANPcrActivity.this);
                }*/

            }
        }).start();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.copy_tv){
            //复制
            copyResultFile(click_pos);
        }else if(id == R.id.copy_result_insert_time_1_starttime_layout){
           // showDatePickerDialog(1,firstStartYear, firstStartMonth, firstStartDay);
            showDateTimePickerDialog(1,firstStartYear, firstStartMonth, firstStartDay,
                    firstStartHour,firstStartMinute);
        }else if(id == R.id.copy_result_insert_time_1_endtime_layout){
            showDateTimePickerDialog(2,firstEndYear, firstEndMonth, firstEndDay,
                    firstEndHour,firstEndMinute);

        }else if(id == R.id.copy_result_insert_time_2_starttime_layout){
            showDateTimePickerDialog(3,secondStartYear, secondStartMonth, secondStartDay,
                    secondStartHour,secondStartMinute);
        }else if(id == R.id.copy_result_insert_time_2_endtime_layout){
            showDateTimePickerDialog(4,secondEndYear, secondEndMonth, secondEndDay,
                    secondEndHour,secondEndMinute);

        }else if(id == R.id.copy_result_insert_time_3_starttime_layout){
            showDateTimePickerDialog(5,thirdStartYear, thirdStartMonth, thirdStartDay,
                    thirdStartHour,thirdStartMinute);
        }else if(id == R.id.copy_result_insert_time_3_endtime_layout){
            showDateTimePickerDialog(6,thirdEndYear, thirdEndMonth, thirdEndDay,
                    thirdEndHour,thirdEndMinute);

        } else if(id == R.id.copy_result_insert_time_4_starttime_layout){
            showDateTimePickerDialog(7,fourthStartYear, fourthStartMonth, fourthStartDay,
                    fourthStartHour,fourthStartMinute);
        }else if(id == R.id.copy_result_insert_time_4_endtime_layout){
            showDateTimePickerDialog(8,fourthEndYear, fourthEndMonth, fourthEndDay,
                    fourthEndHour,fourthEndMinute);

        }else if(id == R.id.copy_result_insert_time_5_starttime_layout){
            showDateTimePickerDialog(9,fifthStartYear, fifthStartMonth, fifthStartDay,
                    fifthStartHour,fifthStartMinute);
        }else if(id == R.id.copy_result_insert_time_5_endtime_layout){
            showDateTimePickerDialog(10,fifthEndYear, fifthEndMonth, fifthEndDay,
                    fifthEndHour,fifthEndMinute);

        }else if(id == R.id.copy_result_insert_time_6_starttime_layout){
            showDateTimePickerDialog(11,sixthStartYear, sixthStartMonth, sixthStartDay,
                    sixthStartHour,sixthStartMinute);
        }else if(id == R.id.copy_result_insert_time_6_endtime_layout){
            showDateTimePickerDialog(12,sixthEndYear, sixthEndMonth, sixthEndDay,
                    sixthEndHour,sixthEndMinute);

        }else if(id == R.id.copy_result_insert_time_7_starttime_layout){
            showDateTimePickerDialog(13,seventhStartYear, seventhStartMonth, seventhStartDay,
                    seventhStartHour,seventhStartMinute);
        }else if(id == R.id.copy_result_insert_time_7_endtime_layout) {
            showDateTimePickerDialog(14, seventhEndYear, seventhEndMonth, seventhEndDay,
                    seventhEndHour, seventhEndMinute);
        }else if(id == R.id.copy_result_insert_time_8_starttime_layout){
            showDateTimePickerDialog(15,eighthStartYear, eighthStartMonth, eighthStartDay,
                    eighthStartHour,eighthStartMinute);
        }else if(id == R.id.copy_result_insert_time_8_endtime_layout) {
            showDateTimePickerDialog(16, eighthEndYear, eighthEndMonth, eighthEndDay,
                    eighthEndHour, eighthEndMinute);
        }else if(id == R.id.copy_result_insert_time_9_starttime_layout){
            showDateTimePickerDialog(17,nighthStartYear, nighthStartMonth, nighthStartDay,
                    nighthStartHour, nighthStartMinute);
        }else if(id == R.id.copy_result_insert_time_9_endtime_layout) {
            showDateTimePickerDialog(18, nighthEndYear, nighthEndMonth, nighthEndDay,
                    nighthEndHour,nighthEndMinute);
        }else if(id == R.id.copy_result_check_layout_1){
            if(!isFirstCk){
                isFirstCk = true;
                CopyCount++;
                mFirstCkImg.setImageResource(R.drawable.item_checked);
                mNum_1_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_1_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isFirstCk = false;
                CopyCount--;
                mFirstCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_1_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_1_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }else if(id == R.id.copy_result_check_layout_2){
            if(!isSecondCk){
                isSecondCk = true;
                CopyCount++;
                mSecondCkImg.setImageResource(R.drawable.item_checked);
                mNum_2_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_2_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isSecondCk = false;
                CopyCount--;
                mSecondCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_2_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_2_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }else if(id == R.id.copy_result_check_layout_3){
            if(!isThirdCk){
                isThirdCk = true;
                CopyCount++;
                mThirdCkImg.setImageResource(R.drawable.item_checked);
                mNum_3_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_3_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isThirdCk = false;
                CopyCount--;
                mThirdCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_3_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_3_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }else if(id == R.id.copy_result_check_layout_4){
            if(!isFouthCk){
                isFouthCk = true;
                CopyCount++;
                mFourthCkImg.setImageResource(R.drawable.item_checked);
                mNum_4_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_4_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isFouthCk = false;
                CopyCount--;
                mFourthCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_4_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_4_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }else if(id == R.id.copy_result_check_layout_5){
            if(!isFifthCk){
                isFifthCk = true;
                CopyCount++;
                mFifthCkImg.setImageResource(R.drawable.item_checked);
                mNum_5_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_5_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isFifthCk = false;
                CopyCount--;
                mFifthCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_5_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_5_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }else if(id == R.id.copy_result_check_layout_6){
            if(!isSixCk){
                isSixCk = true;
                CopyCount++;
                mSixCkImg.setImageResource(R.drawable.item_checked);
                mNum_6_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_6_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isSixCk = false;
                CopyCount--;
                mSixCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_6_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_6_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }else if(id == R.id.copy_result_check_layout_7){
            if(!isSevenCk){
                isSevenCk = true;
                CopyCount++;
                mSevenCkImg.setImageResource(R.drawable.item_checked);
                mNum_7_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_7_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isSevenCk = false;
                CopyCount--;
                mSevenCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_7_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_7_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }else if(id == R.id.copy_result_check_layout_8){
            if(!isEightCk){
                isEightCk = true;
                CopyCount++;
                mEightCkImg.setImageResource(R.drawable.item_checked);
                mNum_8_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_8_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isEightCk = false;
                CopyCount--;
                mEightCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_8_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_8_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }else if(id == R.id.copy_result_check_layout_9){
            if(!isNighthCk){
                isNighthCk = true;
                CopyCount++;
                mNightCkImg.setImageResource(R.drawable.item_checked);
                mNum_9_Tv.setBackgroundResource(R.drawable.shape_oval_blue_bg);
                mNum_9_Tv.setTextColor(getResources().getColor(R.color.white));
            }else{
                isNighthCk = false;
                CopyCount--;
                mNightCkImg.setImageResource(R.drawable.item_unchecked);
                mNum_9_Tv.setBackgroundResource(R.drawable.shape_oval_blue_stroke_bg);
                mNum_9_Tv.setTextColor(getResources().getColor(R.color.blue));
            }
        }
    }

    private void copyResult(String fileName){
        if(!isFirstCk && !isSecondCk && !isThirdCk && !isFouthCk && !isFifthCk && !isSixCk
            && !isSevenCk && !isEightCk && !isNighthCk){
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_select_time),
                    Toast.LENGTH_LONG).show();
            return;

        }else if(isFirstCk || isSecondCk || isThirdCk || isFouthCk || isFifthCk ||isSixCk
                || isSevenCk || isEightCk || isNighthCk){
            if(isFirstCk){
                //判断时间的合理
                String firstStartTime = mStartDate_1_Tv.getText().toString();
                String firstEndTime = mEndDate_1_Tv.getText().toString();
                boolean result_1 = DateFormatUtil.compare_date(firstStartTime,firstEndTime);
                if(!result_1){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_hint)
                    ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isSecondCk){
                        String secondStartTime = mStartDate_2_Tv.getText().toString();
                        String secondEndTime = mEndDate_2_Tv.getText().toString();
                        if(firstStartTime.equals(secondStartTime) && firstEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isThirdCk){
                        String thirdStartTime = mStartDate_3_Tv.getText().toString();
                        String thirdEndTime = mEndDate_3_Tv.getText().toString();
                        if(firstStartTime.equals(thirdStartTime) && firstEndTime.equals(thirdEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_third_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFouthCk){
                        String fourthStartTime = mStartDate_4_Tv.getText().toString();
                        String fourthEndTime = mEndDate_4_Tv.getText().toString();
                        if(firstStartTime.equals(fourthStartTime) && firstEndTime.equals(fourthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_fourth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFifthCk){
                        String fifthhStartTime = mStartDate_5_Tv.getText().toString();
                        String fifthEndTime = mEndDate_5_Tv.getText().toString();
                        if(firstStartTime.equals(fifthhStartTime) && firstEndTime.equals(fifthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_fifth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSixCk){
                        String sixthStartTime = mStartDate_6_Tv.getText().toString();
                        String sixthEndTime = mEndDate_6_Tv.getText().toString();
                        if(firstStartTime.equals(sixthStartTime) && firstEndTime.equals(sixthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_sixth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSevenCk){
                        String seventhStartTime = mStartDate_7_Tv.getText().toString();
                        String seventhEndTime = mEndDate_7_Tv.getText().toString();
                        if(firstStartTime.equals(seventhStartTime) && firstEndTime.equals(seventhEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_seventh_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isEightCk){
                        String eighthStartTime = mStartDate_8_Tv.getText().toString();
                        String eighthEndTime = mEndDate_8_Tv.getText().toString();
                        if(firstStartTime.equals(eighthStartTime) && firstEndTime.equals(eighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_eighth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isNighthCk){
                        String nighthStartTime = mStartDate_9_Tv.getText().toString();
                        String nighthEndTime = mEndDate_9_Tv.getText().toString();
                        if(firstStartTime.equals(nighthStartTime) && firstEndTime.equals(nighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_one_ninth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            if(isSecondCk){
                String secondStartTime = mStartDate_2_Tv.getText().toString();
                String secondEndTime = mEndDate_2_Tv.getText().toString();
                boolean result_2 = DateFormatUtil.compare_date(secondStartTime,secondEndTime);
                if(!result_2){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_hint)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isFirstCk){
                        String firstStartTime = mStartDate_1_Tv.getText().toString();
                        String firstEndTime = mEndDate_1_Tv.getText().toString();
                        if(firstStartTime.equals(secondStartTime) && firstEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isThirdCk){
                        String thirdStartTime = mStartDate_3_Tv.getText().toString();
                        String thirdEndTime = mEndDate_3_Tv.getText().toString();
                        if(secondStartTime.equals(thirdStartTime) && secondEndTime.equals(thirdEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_third_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFouthCk){
                        String fourthStartTime = mStartDate_4_Tv.getText().toString();
                        String fourthEndTime = mEndDate_4_Tv.getText().toString();
                        if(secondStartTime.equals(fourthStartTime) && secondEndTime.equals(fourthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_fourth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFifthCk){
                        String fifthhStartTime = mStartDate_5_Tv.getText().toString();
                        String fifthEndTime = mEndDate_5_Tv.getText().toString();
                        if(secondStartTime.equals(fifthhStartTime) && secondEndTime.equals(fifthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_fifth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSixCk){
                        String sixthStartTime = mStartDate_6_Tv.getText().toString();
                        String sixthEndTime = mEndDate_6_Tv.getText().toString();
                        if(secondStartTime.equals(sixthStartTime) && secondEndTime.equals(sixthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_sixth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSevenCk){
                        String seventhStartTime = mStartDate_7_Tv.getText().toString();
                        String seventhEndTime = mEndDate_7_Tv.getText().toString();
                        if(secondStartTime.equals(seventhStartTime) && secondEndTime.equals(seventhEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_seventh_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isEightCk){
                        String eighthStartTime = mStartDate_8_Tv.getText().toString();
                        String eighthEndTime = mEndDate_8_Tv.getText().toString();
                        if(secondStartTime.equals(eighthStartTime) && secondEndTime.equals(eighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_eighth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isNighthCk){
                        String nighthStartTime = mStartDate_9_Tv.getText().toString();
                        String nighthEndTime = mEndDate_9_Tv.getText().toString();
                        if(secondStartTime.equals(nighthStartTime) && secondEndTime.equals(nighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_two_ninth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            if(isThirdCk){
                String thirdStartTime = mStartDate_3_Tv.getText().toString();
                String thirdEndTime = mEndDate_3_Tv.getText().toString();
                boolean result_3 = DateFormatUtil.compare_date(thirdStartTime,thirdEndTime);
                if(!result_3){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_hint)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isFirstCk){
                        String firstStartTime = mStartDate_1_Tv.getText().toString();
                        String firstEndTime = mEndDate_1_Tv.getText().toString();
                        if(thirdStartTime.equals(firstStartTime) && thirdEndTime.equals(firstEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_first_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSecondCk){
                        String secondStartTime = mStartDate_2_Tv.getText().toString();
                        String secondEndTime = mEndDate_2_Tv.getText().toString();
                        if(thirdStartTime.equals(secondStartTime) && thirdEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFouthCk){
                        String fourthStartTime = mStartDate_4_Tv.getText().toString();
                        String fourthEndTime = mEndDate_4_Tv.getText().toString();
                        if(thirdStartTime.equals(fourthStartTime) && thirdEndTime.equals(fourthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_fourth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFifthCk){
                        String fifthhStartTime = mStartDate_5_Tv.getText().toString();
                        String fifthEndTime = mEndDate_5_Tv.getText().toString();
                        if(thirdStartTime.equals(fifthhStartTime) && thirdEndTime.equals(fifthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_fifth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSixCk){
                        String sixthStartTime = mStartDate_6_Tv.getText().toString();
                        String sixthEndTime = mEndDate_6_Tv.getText().toString();
                        if(thirdStartTime.equals(sixthStartTime) && thirdEndTime.equals(sixthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_sixth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSevenCk){
                        String seventhStartTime = mStartDate_7_Tv.getText().toString();
                        String seventhEndTime = mEndDate_7_Tv.getText().toString();
                        if(thirdStartTime.equals(seventhStartTime) && thirdEndTime.equals(seventhEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_seventh_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isEightCk){
                        String eighthStartTime = mStartDate_8_Tv.getText().toString();
                        String eighthEndTime = mEndDate_8_Tv.getText().toString();
                        if(thirdStartTime.equals(eighthStartTime) && thirdEndTime.equals(eighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_eighth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isNighthCk){
                        String nighthStartTime = mStartDate_9_Tv.getText().toString();
                        String nighthEndTime = mEndDate_9_Tv.getText().toString();
                        if(thirdStartTime.equals(nighthStartTime) && thirdEndTime.equals(nighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_three_ninth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }


            if(isFouthCk){
                String fourthStartTime = mStartDate_4_Tv.getText().toString();
                String fourthEndTime = mEndDate_4_Tv.getText().toString();
                boolean result_4 = DateFormatUtil.compare_date(fourthStartTime,fourthEndTime);
                if(!result_4){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_hint)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isFirstCk){
                        String firstStartTime = mStartDate_1_Tv.getText().toString();
                        String firstEndTime = mEndDate_1_Tv.getText().toString();
                        if(fourthStartTime.equals(firstStartTime) && fourthEndTime.equals(firstEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_first_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSecondCk){
                        String secondStartTime = mStartDate_2_Tv.getText().toString();
                        String secondEndTime = mEndDate_2_Tv.getText().toString();
                        if(fourthStartTime.equals(secondStartTime) && fourthEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isThirdCk){
                        String thirdStartTime = mStartDate_3_Tv.getText().toString();
                        String thirdEndTime = mEndDate_3_Tv.getText().toString();
                        if(fourthStartTime.equals(thirdStartTime) && fourthEndTime.equals(thirdEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_third_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFifthCk){
                        String fifthStartTime = mStartDate_5_Tv.getText().toString();
                        String fifthEndTime = mEndDate_5_Tv.getText().toString();
                        if(fourthStartTime.equals(fifthStartTime) && fourthEndTime.equals(fifthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_fifth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSixCk){
                        String sixthStartTime = mStartDate_6_Tv.getText().toString();
                        String sixthEndTime = mEndDate_6_Tv.getText().toString();
                        if(fourthStartTime.equals(sixthStartTime) && fourthEndTime.equals(sixthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_sixth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSevenCk){
                        String seventhStartTime = mStartDate_7_Tv.getText().toString();
                        String seventhEndTime = mEndDate_7_Tv.getText().toString();
                        if(fourthStartTime.equals(seventhStartTime) && fourthEndTime.equals(seventhEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_seventh_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isEightCk){
                        String eighthStartTime = mStartDate_8_Tv.getText().toString();
                        String eighthEndTime = mEndDate_8_Tv.getText().toString();
                        if(fourthStartTime.equals(eighthStartTime) && fourthEndTime.equals(eighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_eighth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isNighthCk){
                        String nighthStartTime = mStartDate_9_Tv.getText().toString();
                        String nighthEndTime = mEndDate_9_Tv.getText().toString();
                        if(fourthStartTime.equals(nighthStartTime) && fourthEndTime.equals(nighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_four_ninth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            if(isFifthCk){
                String fifthStartTime = mStartDate_5_Tv.getText().toString();
                String fifthEndTime = mEndDate_5_Tv.getText().toString();
                boolean result_5 = DateFormatUtil.compare_date(fifthStartTime,fifthEndTime);
                if(!result_5){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_hint)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isFirstCk){
                        String firstStartTime = mStartDate_1_Tv.getText().toString();
                        String firstEndTime = mEndDate_1_Tv.getText().toString();
                        if(fifthStartTime.equals(firstStartTime) && fifthEndTime.equals(firstEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_first_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSecondCk){
                        String secondStartTime = mStartDate_2_Tv.getText().toString();
                        String secondEndTime = mEndDate_2_Tv.getText().toString();
                        if(fifthStartTime.equals(secondStartTime) && fifthEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isThirdCk){
                        String thirdStartTime = mStartDate_3_Tv.getText().toString();
                        String thirdEndTime = mEndDate_3_Tv.getText().toString();
                        if(fifthStartTime.equals(thirdStartTime) && fifthEndTime.equals(thirdEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_third_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFouthCk){
                        String fourthStartTime = mStartDate_4_Tv.getText().toString();
                        String fourthEndTime = mEndDate_4_Tv.getText().toString();
                        if(fifthStartTime.equals(fourthStartTime) && fifthEndTime.equals(fourthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_fourth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    if(isSixCk){
                        String sixthStartTime = mStartDate_6_Tv.getText().toString();
                        String sixthEndTime = mEndDate_6_Tv.getText().toString();
                        if(fifthStartTime.equals(sixthStartTime) && fifthEndTime.equals(sixthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_sixth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSevenCk){
                        String seventhStartTime = mStartDate_7_Tv.getText().toString();
                        String seventhEndTime = mEndDate_7_Tv.getText().toString();
                        if(fifthStartTime.equals(seventhStartTime) && fifthEndTime.equals(seventhEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_seventh_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isEightCk){
                        String eighthStartTime = mStartDate_8_Tv.getText().toString();
                        String eighthEndTime = mEndDate_8_Tv.getText().toString();
                        if(fifthStartTime.equals(eighthStartTime) && fifthEndTime.equals(eighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_eighth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isNighthCk){
                        String nighthStartTime = mStartDate_9_Tv.getText().toString();
                        String nighthEndTime = mEndDate_9_Tv.getText().toString();
                        if(fifthStartTime.equals(nighthStartTime) && fifthEndTime.equals(nighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_five_ninth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            if(isSixCk){
                String sixthStartTime = mStartDate_6_Tv.getText().toString();
                String sixthEndTime = mEndDate_6_Tv.getText().toString();
                boolean result_6 = DateFormatUtil.compare_date(sixthStartTime,sixthEndTime);
                if(!result_6){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_hint)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isFirstCk){
                        String firstStartTime = mStartDate_1_Tv.getText().toString();
                        String firstEndTime = mEndDate_1_Tv.getText().toString();
                        if(sixthStartTime.equals(firstStartTime) && sixthEndTime.equals(firstEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_first_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSecondCk){
                        String secondStartTime = mStartDate_2_Tv.getText().toString();
                        String secondEndTime = mEndDate_2_Tv.getText().toString();
                        if(sixthStartTime.equals(secondStartTime) && sixthEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isThirdCk){
                        String thirdStartTime = mStartDate_3_Tv.getText().toString();
                        String thirdEndTime = mEndDate_3_Tv.getText().toString();
                        if(sixthStartTime.equals(thirdStartTime) && sixthEndTime.equals(thirdEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_third_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFouthCk){
                        String fourthStartTime = mStartDate_4_Tv.getText().toString();
                        String fourthEndTime = mEndDate_4_Tv.getText().toString();
                        if(sixthStartTime.equals(fourthStartTime) && sixthEndTime.equals(fourthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_fourth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    if(isFifthCk){
                        String fifthStartTime = mStartDate_5_Tv.getText().toString();
                        String fifthEndTime = mEndDate_5_Tv.getText().toString();
                        if(sixthStartTime.equals(fifthStartTime) && sixthEndTime.equals(fifthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_fifth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSevenCk){
                        String seventhStartTime = mStartDate_7_Tv.getText().toString();
                        String seventhEndTime = mEndDate_7_Tv.getText().toString();
                        if(sixthStartTime.equals(seventhStartTime) && sixthEndTime.equals(seventhEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_seventh_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isEightCk){
                        String eighthStartTime = mStartDate_8_Tv.getText().toString();
                        String eighthEndTime = mEndDate_8_Tv.getText().toString();
                        if(sixthStartTime.equals(eighthStartTime) && sixthEndTime.equals(eighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_eighth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isNighthCk){
                        String nighthStartTime = mStartDate_9_Tv.getText().toString();
                        String nighthEndTime = mEndDate_9_Tv.getText().toString();
                        if(sixthStartTime.equals(nighthStartTime) && sixthEndTime.equals(nighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_six_ninth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            if(isSevenCk){
                String seventhStartTime = mStartDate_7_Tv.getText().toString();
                String seventhEndTime = mEndDate_7_Tv.getText().toString();
                boolean result_7 = DateFormatUtil.compare_date(seventhStartTime,seventhEndTime);
                if(!result_7){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_hint)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isFirstCk){
                        String firstStartTime = mStartDate_1_Tv.getText().toString();
                        String firstEndTime = mEndDate_1_Tv.getText().toString();
                        if(seventhStartTime.equals(firstStartTime) && seventhEndTime.equals(firstEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_first_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSecondCk){
                        String secondStartTime = mStartDate_2_Tv.getText().toString();
                        String secondEndTime = mEndDate_2_Tv.getText().toString();
                        if(seventhStartTime.equals(secondStartTime) && seventhEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isThirdCk){
                        String thirdStartTime = mStartDate_3_Tv.getText().toString();
                        String thirdEndTime = mEndDate_3_Tv.getText().toString();
                        if(seventhStartTime.equals(thirdStartTime) && seventhEndTime.equals(thirdEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_third_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFouthCk){
                        String fourthStartTime = mStartDate_4_Tv.getText().toString();
                        String fourthEndTime = mEndDate_4_Tv.getText().toString();
                        if(seventhStartTime.equals(fourthStartTime) && seventhEndTime.equals(fourthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_fourth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    if(isFifthCk){
                        String fifthStartTime = mStartDate_5_Tv.getText().toString();
                        String fifthEndTime = mEndDate_5_Tv.getText().toString();
                        if(seventhStartTime.equals(fifthStartTime) && seventhEndTime.equals(fifthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_fifth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSixCk){
                        String sixthStartTime = mStartDate_6_Tv.getText().toString();
                        String sixthEndTime = mEndDate_6_Tv.getText().toString();
                        if(seventhStartTime.equals(sixthStartTime) && seventhEndTime.equals(sixthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_sixth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isEightCk){
                        String eighthStartTime = mStartDate_8_Tv.getText().toString();
                        String eighthEndTime = mEndDate_8_Tv.getText().toString();
                        if(seventhStartTime.equals(eighthStartTime) && seventhEndTime.equals(eighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_eighth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isNighthCk){
                        String nighthStartTime = mStartDate_9_Tv.getText().toString();
                        String nighthEndTime = mEndDate_9_Tv.getText().toString();
                        if(seventhStartTime.equals(nighthStartTime) && seventhEndTime.equals(nighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_seven_ninth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            if(isEightCk){
                String eighthStartTime = mStartDate_8_Tv.getText().toString();
                String eighthEndTime = mEndDate_8_Tv.getText().toString();
                boolean result_8 = DateFormatUtil.compare_date(eighthStartTime,eighthEndTime);
                if(!result_8){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_hint)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isFirstCk){
                        String firstStartTime = mStartDate_1_Tv.getText().toString();
                        String firstEndTime = mEndDate_1_Tv.getText().toString();
                        if(eighthStartTime.equals(firstStartTime) && eighthEndTime.equals(firstEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_first_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSecondCk){
                        String secondStartTime = mStartDate_2_Tv.getText().toString();
                        String secondEndTime = mEndDate_2_Tv.getText().toString();
                        if(eighthStartTime.equals(secondStartTime) && eighthEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isThirdCk){
                        String thirdStartTime = mStartDate_3_Tv.getText().toString();
                        String thirdEndTime = mEndDate_3_Tv.getText().toString();
                        if(eighthStartTime.equals(thirdStartTime) && eighthEndTime.equals(thirdEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_third_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFouthCk){
                        String fourthStartTime = mStartDate_4_Tv.getText().toString();
                        String fourthEndTime = mEndDate_4_Tv.getText().toString();
                        if(eighthStartTime.equals(fourthStartTime) && eighthEndTime.equals(fourthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_fourth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    if(isFifthCk){
                        String fifthStartTime = mStartDate_5_Tv.getText().toString();
                        String fifthEndTime = mEndDate_5_Tv.getText().toString();
                        if(eighthStartTime.equals(fifthStartTime) && eighthEndTime.equals(fifthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_fifth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSixCk){
                        String sixthStartTime = mStartDate_6_Tv.getText().toString();
                        String sixthEndTime = mEndDate_6_Tv.getText().toString();
                        if(eighthStartTime.equals(sixthStartTime) && eighthEndTime.equals(sixthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_sixth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSevenCk){
                        String seventhStartTime = mStartDate_7_Tv.getText().toString();
                        String seventhEndTime = mEndDate_7_Tv.getText().toString();
                        if(eighthStartTime.equals(seventhStartTime) && eighthEndTime.equals(seventhEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_seventh_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isNighthCk){
                        String nighthStartTime = mStartDate_9_Tv.getText().toString();
                        String nighthEndTime = mEndDate_9_Tv.getText().toString();
                        if(eighthStartTime.equals(nighthStartTime) && eighthEndTime.equals(nighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_eight_ninth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            if(isNighthCk){
                String nigthStartTime = mStartDate_9_Tv.getText().toString();
                String nigthEndTime = mEndDate_9_Tv.getText().toString();
                boolean result_9 = DateFormatUtil.compare_date(nigthStartTime,nigthEndTime);
                if(!result_9){
                    Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_hint)
                            ,Toast.LENGTH_LONG).show();
                    return;
                }else{
                    if(isFirstCk){
                        String firstStartTime = mStartDate_1_Tv.getText().toString();
                        String firstEndTime = mEndDate_1_Tv.getText().toString();
                        if(nigthStartTime.equals(firstStartTime) && nigthEndTime.equals(firstEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_first_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSecondCk){
                        String secondStartTime = mStartDate_2_Tv.getText().toString();
                        String secondEndTime = mEndDate_2_Tv.getText().toString();
                        if(nigthStartTime.equals(secondStartTime) && nigthEndTime.equals(secondEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_second_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isThirdCk){
                        String thirdStartTime = mStartDate_3_Tv.getText().toString();
                        String thirdEndTime = mEndDate_3_Tv.getText().toString();
                        if(nigthStartTime.equals(thirdStartTime) && nigthEndTime.equals(thirdEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_third_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isFouthCk){
                        String fourthStartTime = mStartDate_4_Tv.getText().toString();
                        String fourthEndTime = mEndDate_4_Tv.getText().toString();
                        if(nigthStartTime.equals(fourthStartTime) && nigthEndTime.equals(fourthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_fourth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }

                    if(isFifthCk){
                        String fifthStartTime = mStartDate_5_Tv.getText().toString();
                        String fifthEndTime = mEndDate_5_Tv.getText().toString();
                        if(nigthStartTime.equals(fifthStartTime) && nigthEndTime.equals(fifthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_fifth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSixCk){
                        String sixthStartTime = mStartDate_6_Tv.getText().toString();
                        String sixthEndTime = mEndDate_6_Tv.getText().toString();
                        if(nigthStartTime.equals(sixthStartTime) && nigthEndTime.equals(sixthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_sixth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isSevenCk){
                        String seventhStartTime = mStartDate_7_Tv.getText().toString();
                        String seventhEndTime = mEndDate_7_Tv.getText().toString();
                        if(nigthStartTime.equals(seventhStartTime) && nigthEndTime.equals(seventhEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_seventh_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    if(isEightCk){
                        String eighthStartTime = mStartDate_8_Tv.getText().toString();
                        String eighthEndTime = mEndDate_8_Tv.getText().toString();
                        if(nigthStartTime.equals(eighthStartTime) && nigthEndTime.equals(eighthEndTime)){
                            Toast.makeText(getApplicationContext(),getResources().getString(R.string.toast_copy_nine_eighth_hint)
                                    ,Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                }
            }
            showProgressDialog(getResources().getString(R.string.copy_result_copying));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String filePath = getExperimentFilePath(fileName);
                    if (!StringUtil.isEmpty(filePath)) {
                        Experiment experiment = BoWeiService.getInstance().openExperiment(filePath);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(null != experiment){
                                    if(isFirstCk){
                                        LocalDateTime startTimeDate = LocalDateTime.of(firstStartYear, firstStartMonth+1,
                                                firstStartDay, firstStartHour, firstStartMinute, random.nextInt(59));
                                        LocalDateTime endTimeDate = LocalDateTime.of(firstEndYear, firstEndMonth+1,
                                                firstEndDay, firstEndHour, firstEndMinute, random.nextInt(59));
                                        saveExperiment(experiment, fileName, startTimeDate, endTimeDate);
                                    }

                                    if(isSecondCk){
                                        LocalDateTime startTimeDate_2 = LocalDateTime.of(secondStartYear, secondStartMonth+1,
                                                secondStartDay, secondStartHour, secondStartMinute, random.nextInt(59));
                                        LocalDateTime endTimeDate_2 = LocalDateTime.of(secondEndYear, secondEndMonth+1, secondEndDay,
                                                secondEndHour, secondEndMinute, random.nextInt(59));
                                        saveExperiment(experiment, fileName, startTimeDate_2, endTimeDate_2);
                                    }

                                   if(isThirdCk){
                                       LocalDateTime startTimeDate_3 = LocalDateTime.of(thirdStartYear, thirdStartMonth+1,
                                               thirdStartDay, thirdStartHour, thirdStartMinute, random.nextInt(59));
                                       LocalDateTime endTimeDate_3 = LocalDateTime.of(thirdEndYear, thirdEndMonth+1,
                                               thirdEndDay, thirdEndHour, thirdEndMinute, random.nextInt(59));

                                       saveExperiment(experiment,fileName, startTimeDate_3, endTimeDate_3);
                                   }

                                   if(isFouthCk){
                                       LocalDateTime startTimeDate_4 = LocalDateTime.of(fourthStartYear, fourthStartMonth+1,
                                               fourthStartDay, fourthStartHour, fourthStartMinute, random.nextInt(59));
                                       LocalDateTime endTimeDate_4 = LocalDateTime.of(fourthEndYear, fourthEndMonth+1, fourthEndDay,
                                               fourthEndHour, fourthEndMinute, random.nextInt(59));
                                       saveExperiment(experiment, fileName, startTimeDate_4, endTimeDate_4);
                                   }

                                    if(isFifthCk){
                                        LocalDateTime startTimeDate_5 = LocalDateTime.of(fifthStartYear, fifthStartMonth+1,
                                                fifthStartDay, fifthStartHour, fifthStartMinute, random.nextInt(59));
                                        LocalDateTime endTimeDate_5 = LocalDateTime.of(fifthEndYear, fifthEndMonth+1,
                                                fifthEndDay, fifthEndHour, fifthEndMinute, random.nextInt(59));
                                        saveExperiment(experiment, fileName, startTimeDate_5, endTimeDate_5);
                                    }

                                    if(isSixCk){
                                        LocalDateTime startTimeDate_6 = LocalDateTime.of(sixthStartYear, sixthStartMonth+1,
                                                sixthStartDay, sixthStartHour, sixthStartMinute, random.nextInt(59));
                                        LocalDateTime endTimeDate_6 = LocalDateTime.of(sixthEndYear, sixthEndMonth+1,
                                                sixthEndDay, sixthEndHour, sixthEndMinute, random.nextInt(59));
                                        saveExperiment(experiment, fileName, startTimeDate_6, endTimeDate_6);
                                    }

                                    if(isSevenCk){
                                        LocalDateTime startTimeDate_7 = LocalDateTime.of(seventhStartYear, seventhStartMonth+1,
                                                seventhStartDay, seventhStartHour, seventhStartMinute, random.nextInt(59));
                                        LocalDateTime endTimeDate_7 = LocalDateTime.of(sixthEndYear, sixthEndMonth+1,
                                                seventhEndDay, seventhEndHour, seventhEndMinute, random.nextInt(59));
                                        saveExperiment(experiment, fileName, startTimeDate_7, endTimeDate_7);
                                    }

                                    if(isEightCk){
                                        LocalDateTime startTimeDate_8 = LocalDateTime.of(eighthStartYear, eighthStartMonth+1,
                                                eighthStartDay, eighthStartHour, eighthStartMinute, random.nextInt(59));
                                        LocalDateTime endTimeDate_8 = LocalDateTime.of(eighthEndYear, eighthEndMonth+1,
                                                eighthEndDay, eighthEndHour, eighthEndMinute, random.nextInt(59));
                                        saveExperiment(experiment, fileName, startTimeDate_8, endTimeDate_8);
                                    }

                                    if(isNighthCk){
                                        LocalDateTime startTimeDate_9 = LocalDateTime.of(nighthStartYear, nighthStartMonth+1,
                                                nighthStartDay, nighthStartHour, nighthStartMinute, random.nextInt(59));
                                        LocalDateTime endTimeDate_9 = LocalDateTime.of(nighthEndYear, nighthEndMonth+1,
                                                nighthEndDay,nighthEndHour, nighthEndMinute, random.nextInt(59));
                                        saveExperiment(experiment, fileName, startTimeDate_9, endTimeDate_9);
                                    }
                                    if(isFirstCk || isSecondCk || isThirdCk || isFouthCk || isFifthCk ||isSixCk
                                        || isSevenCk || isEightCk || isNighthCk){
                                        LocalDateTime startTimeDate_10 = LocalDateTime.of(2024, 4, 24, 7, 0, random.nextInt(59));
                                        LocalDateTime endTimeDate_10 = LocalDateTime.of(2024, 4, 24, 7, 5, random.nextInt(59));
                                        saveExperiment(experiment, fileName, startTimeDate_10, endTimeDate_10);
                                    }

                                }
                            }
                        });
                    }
                }
            }).start();
        }
    }

    private void copyResultFile(int click_pos){
        if(click_pos != -1){
            File file = resultFiles.get(click_pos);
            copyResult(file.getName());
        }else{
            Toast.makeText(getApplicationContext(),getResources().getString(R.string.copy_result_need_to_copy),Toast.LENGTH_LONG).show();
        }

    }

    /**
     * 设置日期的对话框
     * @param
     */


    private Calendar calendarDate;
    private TimePicker timePicker;

    private CustomLargeDialog mDateTimeDialog;
    private void showDateTimePickerDialog(int pos,
                                          int year,
                                          int month,
                                          int dayOfMonth,
                                          int hour,
                                          int minute){
        if(null != mDateTimeDialog){
            mDateTimeDialog.dismiss();
            mDateTimeDialog = null;
        }
        if(null == mDateTimeDialog){
            mDateTimeDialog = new CustomLargeDialog(this);
            LayoutInflater mInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View pickView = mInflater.inflate(R.layout.dialog_date_time_picker_layout, null);
            final DatePicker datePicker = (DatePicker)pickView.findViewById(R.id.date_picker);
            timePicker = (TimePicker)pickView.findViewById(R.id.timer_picker);
            timePicker.setIs24HourView(true);
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {

                @Override
                public void onTimeChanged(TimePicker view, final int hourOfDay, final int minute) {
                    // TODO Auto-generated method stub
                    calendarDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendarDate.set(Calendar.MINUTE, minute);
                }
            });
            datePicker.init(year, month, dayOfMonth, new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker view, int year, int month, int day) {
                    // TODO Auto-generated method stub
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(year, month, day);
                    Log.i("CopyResultActivity" ,"showDatePickerDialog---" +"year--" + year + " month---" + month+" day---" +day);
                }
            });
            CustomLargeDialog.Builder builder = new CustomLargeDialog.Builder(this);
            builder.setContentView(pickView);
            builder.setTitle(this.getResources().getString(R.string.dialog_title_choice_date));
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    //Log.i("AAA", "datePicker.getYear()--" + datePicker.getYear() + " datePicker.getMonth()---" + datePicker.getMonth() + "datePicker.getDayOfMonth()---" + datePicker.getDayOfMonth());
                    datePicker.clearFocus();
                    updateDate(pos,datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth(),calendarDate.get(Calendar.HOUR_OF_DAY), calendarDate.get(Calendar.MINUTE));
                    timePicker.clearFocus();
                    //updateTime(pos,calendarDate.get(Calendar.HOUR_OF_DAY), calendarDate.get(Calendar.MINUTE));
                    mDateTimeDialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mDateTimeDialog.dismiss();
                    mDateTimeDialog = null;
                }
            });
            mDateTimeDialog = builder.create();
            mDateTimeDialog.show();
        }
    }



    private void updateDate(int pos,
                            int year,
                            int monthOfYear,
                            int dayOfMonth,
                            int hourOfDay,
                            int minute){
        switch (pos){
            case 1:
                firstStartHour = hourOfDay;
                firstStartMinute = minute;
           //     mStartTime_1_Tv.setText((String.format("%02d:%02d",this.firstStartHour, this.firstStartMinute)));
                mStartDate_1_Tv.setText((String.format("%04d-%02d-%02d",year,
                        monthOfYear+1,dayOfMonth))
                        +" "
                        + (String.format("%02d:%02d",this.firstStartHour, this.firstStartMinute)));
                if(!StringUtil.isEmpty(mStartDate_1_Tv.getText().toString())){
                    firstStartYear = DateFormatUtil.getYear(mStartDate_1_Tv.getText().toString());
                    firstStartMonth  = DateFormatUtil.getMonth(mStartDate_1_Tv.getText().toString());
                    firstStartDay  = DateFormatUtil.getDay(mStartDate_1_Tv.getText().toString());
                }
                Log.i("CopyResultActivity","firstStartMonth----"+firstStartMonth);
                break;

            case 2:
                firstEndHour = hourOfDay;
                firstEndMinute = minute;
                mEndDate_1_Tv.setText(String.format("%04d-%02d-%02d",year,
                        monthOfYear+1,dayOfMonth)
                        +" "
                        +String.format("%02d:%02d",this.firstEndHour, this.firstEndMinute));
                if(!StringUtil.isEmpty(mEndDate_1_Tv.getText().toString())){
                    firstEndYear = DateFormatUtil.getYear(mEndDate_1_Tv.getText().toString());
                    firstEndMonth  = DateFormatUtil.getMonth(mEndDate_1_Tv.getText().toString());
                    firstEndDay  = DateFormatUtil.getDay(mEndDate_1_Tv.getText().toString());
                }
                break;

            case 3:
                secondStartYear= year;
                secondStartMonth = monthOfYear;
                secondStartDay = dayOfMonth;
                secondStartHour = hourOfDay;
                secondStartMinute = minute;

                mStartDate_2_Tv.setText(String.format("%04d-%02d-%02d",this.secondStartYear,
                        this.secondStartMonth+1,this.secondStartDay)
                +" " + String.format("%02d:%02d",this.secondStartHour, this.secondStartMinute));
                break;
            case 4:
                secondEndYear= year;
                secondEndMonth = monthOfYear;
                secondEndDay = dayOfMonth;
                secondEndHour = hourOfDay;
                secondEndMinute = minute;

                mEndDate_2_Tv.setText(String.format("%04d-%02d-%02d",this.secondEndYear,
                        this.secondEndMonth+1,this.secondEndDay)
                +" " + String.format("%02d:%02d",this.secondEndHour, this.secondEndMinute));
                break;

            case 5:
                thirdStartYear= year;
                thirdStartMonth = monthOfYear;
                thirdStartDay = dayOfMonth;
                thirdStartHour = hourOfDay;
                thirdStartMinute = minute;
                mStartDate_3_Tv.setText(String.format("%04d-%02d-%02d",this.thirdStartYear,
                        this.thirdStartMonth+1,this.thirdStartDay)
                + " " + String.format("%02d:%02d",this.thirdStartHour, this.thirdStartMinute));
                break;

            case 6:
                thirdEndYear= year;
                thirdEndMonth = monthOfYear;
                thirdEndDay = dayOfMonth;
                thirdEndHour = hourOfDay;
                thirdEndMinute = minute;
                mEndDate_3_Tv.setText(String.format("%04d-%02d-%02d",this.thirdEndYear,
                        this.thirdEndMonth+1,this.thirdEndDay)
                +" "+ String.format("%02d:%02d",this.thirdEndHour, this.thirdEndMinute));
                break;

            case 7:
                fourthStartYear= year;
                fourthStartMonth = monthOfYear;
                fourthStartDay = dayOfMonth;
                fourthStartHour = hourOfDay;
                fourthStartMinute = minute;
                mStartDate_4_Tv.setText(String.format("%04d-%02d-%02d",this.fourthStartYear,
                        this.fourthStartMonth+1,this.fourthStartDay)
                + " " + String.format("%02d:%02d",this.fourthStartHour, this.fourthStartMinute));
                break;
            case 8:
                fourthEndYear= year;
                fourthEndMonth = monthOfYear;
                fourthEndDay = dayOfMonth;
                fourthEndHour = hourOfDay;
                fourthEndMinute = minute;
                mEndDate_4_Tv.setText(String.format("%04d-%02d-%02d",this.fourthEndYear,
                        this.fourthEndMonth+1,this.fourthEndDay)
                +  " " + String.format("%02d:%02d",this.fourthEndHour, this.fourthEndMinute));
                break;

            case 9:
                fifthStartYear= year;
                fifthStartMonth = monthOfYear;
                fifthStartDay = dayOfMonth;
                fifthStartHour = hourOfDay;
                fifthStartMinute = minute;
                mStartDate_5_Tv.setText(String.format("%04d-%02d-%02d",this.fifthStartYear,
                        this.fifthStartMonth+1,this.fifthStartDay)
                +" " +  String.format("%02d:%02d",this.fifthStartHour, this.fifthStartMinute));
                break;

            case 10:
                fifthEndYear= year;
                fifthEndMonth = monthOfYear;
                fifthEndDay = dayOfMonth;
                fifthEndHour = hourOfDay;
                fifthEndMinute = minute;
                mEndDate_5_Tv.setText(String.format("%04d-%02d-%02d",this.fifthEndYear,
                        this.fifthEndMonth+1,this.fifthEndDay)
                        +" " +  String.format("%02d:%02d",this.fifthEndHour, this.fifthEndMinute));
                break;
            case 11:
                sixthStartYear= year;
                sixthStartMonth = monthOfYear;
                sixthStartDay = dayOfMonth;
                sixthStartHour = hourOfDay;
                sixthStartMinute = minute;
                mStartDate_6_Tv.setText(String.format("%04d-%02d-%02d",this.sixthStartYear,
                        this.sixthStartMonth+1,this.sixthStartDay)
                        +" " +  String.format("%02d:%02d",this.sixthStartHour, this.sixthStartMinute));
                break;
            case 12:
                sixthEndYear= year;
                sixthEndMonth = monthOfYear;
                sixthEndDay = dayOfMonth;
                sixthEndHour = hourOfDay;
                sixthEndMinute = minute;
                mEndDate_6_Tv.setText(String.format("%04d-%02d-%02d",this.sixthEndYear,
                        this.sixthEndMonth+1,this.sixthEndDay)
                + " " +  String.format("%02d:%02d",this.sixthEndHour, this.sixthEndMinute));
                break;
            case 13:
                seventhStartYear= year;
                seventhStartMonth = monthOfYear;
                seventhStartDay = dayOfMonth;
                seventhStartHour = hourOfDay;
                seventhStartMinute = minute;
                mStartDate_7_Tv.setText(String.format("%04d-%02d-%02d",this.seventhStartYear,
                        this.seventhStartMonth+1,this.seventhStartDay)
                        + " " +  String.format("%02d:%02d",this.seventhStartHour, this.seventhStartMinute));
                break;
            case 14:
                seventhEndYear= year;
                seventhEndMonth = monthOfYear;
                seventhEndDay = dayOfMonth;
                seventhEndHour  = hourOfDay;
                seventhEndMinute = minute;
                mEndDate_7_Tv.setText(String.format("%04d-%02d-%02d",this.seventhEndYear,
                        this.seventhEndMonth+1,this.seventhEndDay)
                        + " " +  String.format("%02d:%02d",this.seventhEndHour, this.seventhEndMinute));
                break;
            case 15:
                eighthStartYear= year;
                eighthStartMonth = monthOfYear;
                eighthStartDay = dayOfMonth;
                eighthStartHour = hourOfDay;
                eighthStartMinute = minute;
                mStartDate_8_Tv.setText(String.format("%04d-%02d-%02d",this.eighthStartYear,
                        this.eighthStartMonth+1,this.eighthStartDay)
                        + " " +  String.format("%02d:%02d",this.eighthStartHour, this.eighthStartMinute));
                break;
            case 16:
                eighthEndYear= year;
                eighthEndMonth = monthOfYear;
                eighthEndDay = dayOfMonth;
                eighthEndHour = hourOfDay;
                eighthEndMinute = minute;
                mEndDate_8_Tv.setText(String.format("%04d-%02d-%02d",this.eighthEndYear,
                        this.eighthEndMonth+1,this.eighthEndDay)
                        + " " +  String.format("%02d:%02d",this.eighthEndHour, this.eighthEndMinute));
                break;
            case 17:
                nighthStartYear= year;
                nighthStartMonth = monthOfYear;
                nighthStartDay = dayOfMonth;
                nighthStartHour = hourOfDay;
                nighthStartMinute = minute;
                mStartDate_9_Tv.setText(String.format("%04d-%02d-%02d",this.nighthStartYear,
                        this.nighthStartMonth+1,this.nighthStartDay)
                        + " " +  String.format("%02d:%02d",this.nighthStartHour, this.nighthStartMinute));
                break;
            case 18:
                nighthEndYear= year;
                nighthEndMonth = monthOfYear;
                nighthEndDay = dayOfMonth;
                nighthEndHour = hourOfDay;
                nighthEndMinute = minute;
                mEndDate_9_Tv.setText(String.format("%04d-%02d-%02d",this.nighthEndYear,
                        this.nighthEndMonth+1,this.nighthEndDay)
                        + " " +  String.format("%02d:%02d",this.nighthEndHour, this.nighthEndMinute));
                break;
            default:
                break;
        }

    }

}
