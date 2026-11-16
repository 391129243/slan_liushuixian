package com.gidi.bio_pcr.fragment.system;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.activity.MainActivity;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.event.MessageEvent;
import com.gidi.bio_pcr.event.MsgType;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.LocaleUtils;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.view.CustomDialog;
//import com.hzmct.enjoysdk.api.EnjoySDK;
//import com.hzmct.enjoysdk.transform.EnjoyApiNotSupportException;
import com.hzmct.enjoysdk.api.EnjoySDK;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.Locale;


/**
 * 通用设置
 * 设置语言和音量
 */
public class GeneralSettingFragment extends BaseFragment implements View.OnClickListener{

    private TextView mLangugeTv,mRotationTv;
    private TextView mVolumeTv,mUploadTv;
    private TextView mDateTv;
    private LinearLayout mLanguageLayout,mUploadLayout;
    private LinearLayout mVolumeLayout,mRotationLayout;
    private LinearLayout mTimeLayout;
    private LinearLayout mEnglishLayout;
    private LinearLayout mChineseLayout;
    //上传设置
    private LinearLayout upload_HttpLayout,upload_Hl7EthernetLayout,upload_Hl7SerialLayout;
    private RadioGroup mUploadModeRg;
    private RadioButton mUpload_HttpRb,mUpload_Hl7EthernetRb,mUpload_Hl7SerialRb;
    private TextView mSysDateTv;
    private TextView mSysTimeTv;
    private TextView mRotation_0Tv,mRotation_180Tv;
    private CustomDialog changeDialog;
    private CustomDialog mDatePickerDialog;
    private CustomDialog mTimePickerDialog;
    private TimePicker timePicker;
    private LinearLayout mSoundLayout;
    private LinearLayout mMuteLayout;
    private boolean isChinese = true;
    private boolean isMute = false;
    private Calendar calendarDate;
    private long total_Time = 0;
    private int mYear, mMonth, mDay;
    private int mHour, mMinute;
    private AudioManager mAudioManager;
    private int alarmVolume;
//    private EnjoySDK enjoySDK;

    private int systemRotation;

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_general_setting;
    }

    @Override
    protected void initView(View rootView) {
        mLangugeTv = (TextView)rootView.findViewById(R.id.general_set_language_tv);
        mVolumeTv = (TextView)rootView.findViewById(R.id.general_set_volume_tv);
        mDateTv = (TextView)rootView.findViewById(R.id.general_set_time_tv);
        mRotationTv = (TextView)rootView.findViewById(R.id.general_set_system_rotation_tv) ;
        mLanguageLayout = (LinearLayout)rootView.findViewById(R.id.general_setting_languege_layout);
        mVolumeLayout = (LinearLayout)rootView.findViewById(R.id.general_setting_volume_layout);
        mTimeLayout = (LinearLayout)rootView.findViewById(R.id.general_setting_time_layout);
        mRotationLayout = (LinearLayout)rootView.findViewById(R.id.general_setting_system_rotation_layout);
        mEnglishLayout = (LinearLayout)rootView.findViewById(R.id.general_language_english_layout);
        mChineseLayout = (LinearLayout)rootView.findViewById(R.id.general_language_chinese_layout);
        mSoundLayout = (LinearLayout)rootView.findViewById(R.id.general_volume_sound_layout);
        mMuteLayout = (LinearLayout)rootView.findViewById(R.id.general_volume_mute_layout);
        mSysDateTv = (TextView)rootView.findViewById(R.id.system_setting_date_tv);
        mSysTimeTv = (TextView)rootView.findViewById(R.id.system_setting_time_tv);
        calendarDate = Calendar.getInstance();
//        enjoySDK = new EnjoySDK(getActivity().getApplicationContext());

        mUploadTv = (TextView) rootView.findViewById(R.id.general_set_upload_tv);
        mUploadLayout = (LinearLayout) rootView.findViewById(R.id.general_setting_upload_layout);
        mRotation_0Tv = (TextView) rootView.findViewById(R.id.general_rotation_0_tv);
        mRotation_180Tv = (TextView) rootView.findViewById(R.id.general_rotation_180_tv);

        mUploadModeRg  = (RadioGroup) rootView.findViewById(R.id.general_setting_upload_rg);
        upload_HttpLayout = (LinearLayout) rootView.findViewById(R.id.general_upload_http_port_layout);
        upload_Hl7EthernetLayout = (LinearLayout) rootView.findViewById(R.id.general_upload_hl7_ethernet_layout);
        upload_Hl7SerialLayout = (LinearLayout) rootView.findViewById(R.id.general_upload_hl7_serial_layout);
        mUpload_HttpRb = (RadioButton) rootView.findViewById(R.id.general_setting_upload_http_rb);
        mUpload_Hl7EthernetRb = (RadioButton)rootView.findViewById(R.id.general_setting_upload_hl7_ethernet_rb);
        mUpload_Hl7SerialRb = (RadioButton)rootView.findViewById(R.id.general_setting_upload_hl7_serial_rb);

    }

    @Override
    protected void initListener() {
        mLangugeTv.setOnClickListener(this);
        mVolumeTv.setOnClickListener(this);
        mDateTv.setOnClickListener(this);
        mUploadTv.setOnClickListener(this);
        mEnglishLayout.setOnClickListener(this);
        mChineseLayout.setOnClickListener(this);
        mSoundLayout.setOnClickListener(this);
        mMuteLayout.setOnClickListener(this);
        mSysDateTv.setOnClickListener(this);
        mSysTimeTv.setOnClickListener(this);
        mRotationTv.setOnClickListener(this);
        mRotation_0Tv.setOnClickListener(this);
        mRotation_180Tv.setOnClickListener(this);
        mUploadModeRg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkId) {
                if(checkId ==R.id.general_setting_upload_http_rb){
                    PreferenceUtil.getInstance(getActivity().getApplicationContext())
                                    .setValueByName(SharedConstant.UPLOAD_MODE,1);
                    upload_HttpLayout.setVisibility(View.VISIBLE);
                    upload_Hl7EthernetLayout.setVisibility(View.INVISIBLE);
                    upload_Hl7SerialLayout.setVisibility(View.INVISIBLE);
                }else if(checkId ==R.id.general_setting_upload_hl7_ethernet_rb){
                    PreferenceUtil.getInstance(getActivity().getApplicationContext())
                            .setValueByName(SharedConstant.UPLOAD_MODE,2);
                    upload_HttpLayout.setVisibility(View.INVISIBLE);
                    upload_Hl7EthernetLayout.setVisibility(View.VISIBLE);
                    upload_Hl7SerialLayout.setVisibility(View.INVISIBLE);
                }else if(checkId == R.id.general_setting_upload_hl7_serial_rb) {
                    PreferenceUtil.getInstance(getActivity().getApplicationContext())
                            .setValueByName(SharedConstant.UPLOAD_MODE,3);
                    upload_HttpLayout.setVisibility(View.INVISIBLE);
                    upload_Hl7EthernetLayout.setVisibility(View.INVISIBLE);
                    upload_Hl7SerialLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        registerEventBus();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(hidden){
            unregisterEventBus();
        }else {
            registerEventBus();
            initMuteState();
            initLocaleState();
            setSystemTime();
            getSystemRotation();
            getUploadMode();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterEventBus();
        calendarDate = null;
        if(null != changeDialog){
            changeDialog.dismiss();
            changeDialog = null;
        }

        if(null != mDatePickerDialog){
            mDatePickerDialog.dismiss();
            mDatePickerDialog = null;
        }

        if(null != mTimePickerDialog){
            mTimePickerDialog.dismiss();
            mTimePickerDialog = null;
        }

        if (null != timePicker){
            timePicker = null;
        }
    }

    /**
     * 初始化语言状态
     * @param
     */
    private void initLocaleState(){
        Locale _UserLocale=LocaleUtils.getCurrentLocale(getActivity().getApplicationContext());
        if(_UserLocale.equals(Locale.CHINESE)){
            mChineseLayout.setBackgroundResource(R.color.blue);
            mEnglishLayout.setBackgroundResource(R.color.color_landscape_main_bg);
        }else{
            mEnglishLayout.setBackgroundResource(R.color.blue);
            mChineseLayout.setBackgroundResource(R.color.color_landscape_main_bg);
        }
    }

    /**
     * 初始化音量设置状态
     */
    private void initMuteState(){
        mAudioManager = (AudioManager)getActivity().getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        alarmVolume = (int)((maxVolume * 4)/5);
        int currentVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(0 == currentVol){
            mMuteLayout.setBackgroundResource(R.color.blue);
            mSoundLayout.setBackgroundResource(R.color.color_landscape_main_bg);
            isMute = true;
        }else{
            mSoundLayout.setBackgroundResource(R.color.blue);
            mMuteLayout.setBackgroundResource(R.color.color_landscape_main_bg);
            isMute = false;
        }
    }

    private void getSystemRotation(){
        try {
//            McRotation mcRotation = McRotation.getInstance(getContext());
            EnjoySDK enjoySDK = new EnjoySDK(getContext());
            systemRotation = enjoySDK.getSystemRotation();
            if(systemRotation == 0){
                mRotation_0Tv.setBackgroundResource(R.color.color_main_text);
                mRotation_180Tv.setBackgroundResource(R.color.color_landscape_main_bg);
            }else if(systemRotation == 180){
                mRotation_0Tv.setBackgroundResource(R.color.color_landscape_main_bg);
                mRotation_180Tv.setBackgroundResource(R.color.color_main_text);
            }
        }catch (Exception e){
            e.getMessage();
        }
    }

    private void getUploadMode(){
        int uploadMode = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getIntValue(SharedConstant.UPLOAD_MODE,1);
        switch (uploadMode){
            case 1:
                mUpload_HttpRb.setChecked(true);
                break;
            case 2:
                mUpload_Hl7EthernetRb.setChecked(true);
                break;
            case 3:
                mUpload_Hl7SerialRb.setChecked(true);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.general_set_language_tv) {
            mLangugeTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mVolumeTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mDateTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mUploadTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mRotationTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mLanguageLayout.setVisibility(View.VISIBLE);
            mVolumeLayout.setVisibility(View.GONE);
            mTimeLayout.setVisibility(View.GONE);
            mRotationLayout.setVisibility(View.GONE);
            mUploadLayout.setVisibility(View.GONE);
        } else if (id == R.id.general_set_volume_tv) {
            mVolumeTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mLangugeTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mDateTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mUploadTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mRotationTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mLanguageLayout.setVisibility(View.GONE);
            mVolumeLayout.setVisibility(View.VISIBLE);
            mTimeLayout.setVisibility(View.GONE);
            mRotationLayout.setVisibility(View.GONE);
            mUploadLayout.setVisibility(View.GONE);
        } else if (id == R.id.general_set_time_tv) {
            mDateTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mLangugeTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mVolumeTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mUploadTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mRotationTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mLanguageLayout.setVisibility(View.GONE);
            mVolumeLayout.setVisibility(View.GONE);
            mTimeLayout.setVisibility(View.VISIBLE);
            mRotationLayout.setVisibility(View.GONE);
            mUploadLayout.setVisibility(View.GONE);
        } else if(id == R.id.general_set_upload_tv){
            mUploadTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mDateTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mLangugeTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mVolumeTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mRotationTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mLanguageLayout.setVisibility(View.GONE);
            mVolumeLayout.setVisibility(View.GONE);
            mTimeLayout.setVisibility(View.GONE);
            mRotationLayout.setVisibility(View.GONE);
            mUploadLayout.setVisibility(View.VISIBLE);
        }else if (id == R.id.general_language_english_layout) {
            isChinese = false;
            mEnglishLayout.setBackgroundResource(R.color.color_main_text);
            mChineseLayout.setBackgroundResource(R.color.color_landscape_main_bg);
            showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_change_language)
                    , getActivity().getResources().getString(R.string.dialog_message_change_english), isChinese);
        } else if (id == R.id.general_language_chinese_layout) {
            isChinese = true;
            mChineseLayout.setBackgroundResource(R.color.color_main_text);
            mEnglishLayout.setBackgroundResource(R.color.color_landscape_main_bg);
            showNoticeDialog(getActivity().getResources().getString(R.string.dialog_title_change_language)
                    , getActivity().getResources().getString(R.string.dialog_message_change_chinese), isChinese);
        } else if (id == R.id.general_volume_sound_layout) {
            isMute = false;
/*            mSoundLayout.setBackgroundResource(R.color.color_main_text);
            mMuteLayout.setBackgroundResource(R.color.color_landscape_main_bg);*/
            setVolumeState(isMute);
        } else if (id == R.id.general_volume_mute_layout) {
            isMute = true;
/*            mMuteLayout.setBackgroundResource(R.color.color_main_text);
            mSoundLayout.setBackgroundResource(R.color.color_landscape_main_bg);*/
            setVolumeState(isMute);
        } else if (id == R.id.system_setting_date_tv) {
            showDatePickerDialog(mYear, mMonth, mDay);
        } else if (id == R.id.system_setting_time_tv) {
            showTimePickerDialog(mHour, mMinute);
        }else if(id == R.id.general_set_system_rotation_tv){
            //屏幕设置
            mLangugeTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mVolumeTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mDateTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mUploadTv.setBackgroundResource(R.drawable.shape_general_normal_btn);
            mRotationTv.setBackgroundResource(R.drawable.shape_general_pressed_btn);
            mLanguageLayout.setVisibility(View.GONE);
            mVolumeLayout.setVisibility(View.GONE);
            mTimeLayout.setVisibility(View.GONE);
            mRotationLayout.setVisibility(View.VISIBLE);
            mUploadLayout.setVisibility(View.GONE);
        }else if(id == R.id.general_rotation_0_tv){
            systemRotation = 0;
            mRotation_0Tv.setBackgroundResource(R.color.color_main_text);
            mRotation_180Tv.setBackgroundResource(R.color.color_landscape_main_bg);
            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                    .setValueByName(SharedConstant.SYSTEM_ROTATION,0);
            try {
//                McRotation mcRotation = McRotation.getInstance(getContext());
//                mcRotation.setSystemRotation(0);
                EnjoySDK enjoySDK = new EnjoySDK(getContext());
                enjoySDK.setSystemRotation(0);
            } catch (Exception e) {
              e.printStackTrace();
            }
        }else if(id == R.id.general_rotation_180_tv){
            systemRotation = 180;
            mRotation_180Tv.setBackgroundResource(R.color.color_main_text);
            mRotation_0Tv.setBackgroundResource(R.color.color_landscape_main_bg);
            PreferenceUtil.getInstance(getActivity().getApplicationContext())
                    .setValueByName(SharedConstant.SYSTEM_ROTATION,180);
            try {
//                McRotation mcRotation = McRotation.getInstance(getContext());
//                mcRotation.setSystemRotation(180);
                EnjoySDK enjoySDK = new EnjoySDK(getContext());
                enjoySDK.setSystemRotation(180);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void setVolumeState(boolean is_Mute){
        if(is_Mute){
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
            mMuteLayout.setBackgroundResource(R.color.color_main_text);
            mSoundLayout.setBackgroundResource(R.color.color_landscape_main_bg);
        }else{
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, alarmVolume, AudioManager.FLAG_PLAY_SOUND);
            mSoundLayout.setBackgroundResource(R.color.color_main_text);
            mMuteLayout.setBackgroundResource(R.color.color_landscape_main_bg);
        }
    }

    private void showNoticeDialog(String title,String message,final boolean isChinese){
        if(null != changeDialog){
            changeDialog.dismiss();
            changeDialog = null;
        }
        if(null == changeDialog){
            changeDialog = new CustomDialog(getActivity());
            LayoutInflater mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = (View)mInflater.inflate(R.layout.dialog_common_hint_layout, null);
            TextView mHintTxt = (TextView)view.findViewById(R.id.common_dialog_hint_txt);
            mHintTxt.setText(message);

            CustomDialog.Builder builder = new CustomDialog.Builder(getActivity());
            builder.setContentView(view);
            builder.setTitle(title);
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                  if(isChinese){
                      if (LocaleUtils.needUpdateLocale(getActivity().getApplicationContext(), LocaleUtils.LOCALE_CHINESE)) {
                          LocaleUtils.saveUserLocale(getActivity().getApplicationContext(), LocaleUtils.LOCALE_CHINESE);
                          restartAct();
                      }
                  }else{
                      if (LocaleUtils.needUpdateLocale(getActivity().getApplicationContext(), LocaleUtils.LOCALE_ENGLISH)) {
                          LocaleUtils.saveUserLocale(getActivity().getApplicationContext(), LocaleUtils.LOCALE_ENGLISH);
                          restartAct();
                      }
                  }
                    changeDialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    changeDialog.dismiss();
                    changeDialog = null;
                }
            });
            changeDialog = builder.create();
            changeDialog.show();
        }
    }

    /**
     * 设置日期的对话框
     * @param
     */
    private void showDatePickerDialog(int year, int month, int dayOfMonth){
        if(null != mDatePickerDialog){
            mDatePickerDialog.dismiss();
            mDatePickerDialog = null;
        }
        if(null == mDatePickerDialog){
            mDatePickerDialog = new CustomDialog(this.getActivity());
            LayoutInflater mInflater = (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View pickView = mInflater.inflate(R.layout.dialog_datepicker_layout, null);
            final DatePicker datePicker = (DatePicker)pickView.findViewById(R.id.date_picker);
            datePicker.init(year, month, dayOfMonth, new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker view, int year, int month, int day) {
                    // TODO Auto-generated method stub
                    calendarDate.set(year, month, day);
                    //Log.i("AAA" ,"showDatePickerDialog---" +"year--" + year + " month---" + month+" day---" +day);
                }
            });
            CustomDialog.Builder builder = new CustomDialog.Builder(this.getActivity());
            builder.setContentView(pickView);
            builder.setTitle(this.getActivity().getResources().getString(R.string.dialog_title_choice_date));
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                  //  Log.i("AAA", "datePicker.getYear()--" + datePicker.getYear() + " datePicker.getMonth()---" + datePicker.getMonth() + "datePicker.getDayOfMonth()---" + datePicker.getDayOfMonth());
                    datePicker.clearFocus();
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
     * 设置时间对话框
     * @param hour
     * @param minute
     */
    private void showTimePickerDialog(int hour,int minute){
        if(null != mTimePickerDialog){
            mTimePickerDialog.dismiss();
            mTimePickerDialog = null;
        }
        if(null == mTimePickerDialog){
            mTimePickerDialog = new CustomDialog(this.getActivity());
            LayoutInflater mInflater = (LayoutInflater)this.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View pickView = mInflater.inflate(R.layout.dialog_timepicker_layout, null);
            timePicker = (TimePicker)pickView.findViewById(R.id.timer_picker);
            timePicker.setIs24HourView(true);
            timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {

                @Override
                public void onTimeChanged(TimePicker view, final int hourOfDay, final int minute) {
                    // TODO Auto-generated method stub
                    calendarDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendarDate.set(Calendar.MINUTE, minute);
                }
            });
            CustomDialog.Builder builder = new CustomDialog.Builder(this.getActivity());
            builder.setContentView(pickView);
            builder.setTitle(this.getActivity().getResources().getString(R.string.dialog_title_choice_time));
            builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    //Log.i("TimeSettingActivity", "datePicker.getYear()--" + datePicker.getYear() + " datePicker.getMonth()---" + datePicker.getMonth() + "datePicker.getDayOfMonth()---" + datePicker.getDayOfMonth());
                    timePicker.clearFocus();
                    updateTime(calendarDate.get(Calendar.HOUR_OF_DAY), calendarDate.get(Calendar.MINUTE));
                    mTimePickerDialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    // TODO Auto-generated method stub
                    mTimePickerDialog.dismiss();
                    mTimePickerDialog = null;
                }
            });
            mTimePickerDialog = builder.create();
            mTimePickerDialog.show();
        }
    }

    private void setSystemTime(){

        mYear = calendarDate.get(Calendar.YEAR);
        mMonth = calendarDate.get(Calendar.MONTH);//获取的值比实际月份少一天
        mDay = calendarDate.get(Calendar.DAY_OF_MONTH);
        mHour = calendarDate.get(Calendar.HOUR_OF_DAY);
        mMinute = calendarDate.get(Calendar.MINUTE);

        mSysDateTv.setText(DateFormatUtil.getNowDate());
        mSysTimeTv.setText(DateFormatUtil.getNowTimeHM());
    }

    private void updateDate(int year,  int monthOfYear, int dayOfMonth){
        mYear = year;
        mMonth = monthOfYear;
        mDay = dayOfMonth;
        calendarDate.set(Calendar.YEAR, year);
        calendarDate.set(Calendar.MONTH, monthOfYear);
        calendarDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        long when_data = calendarDate.getTimeInMillis();
        total_Time = when_data;
        if ((total_Time / 1000 )< Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(total_Time);
        }
    }

    private void updateTime(int hourOfDay, int minute){
        mHour = hourOfDay;
        mMinute = minute;
        calendarDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendarDate.set(Calendar.MINUTE, minute);
        calendarDate.set(Calendar.SECOND, 0);
        calendarDate.set(Calendar.MILLISECOND, 0);
        long when_time = calendarDate.getTimeInMillis();

        if (when_time / 1000 < Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(when_time);
        }
        // TODO Auto-generated method stub
        System.out.println(hourOfDay + ":" + hourOfDay);

    }

    /**
     * 重启当前Activity
     */
    private void restartAct() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // 杀掉进程
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
        //清除Activity退出和进入的动画
        //overridePendingTransition(0, 0)
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MessageEvent messageEvent) {
        switch (messageEvent.getMsgType()){
            case MsgType.MSG_UPDATE_SYSTEM_TIME:

                mSysDateTv.setText(DateFormatUtil.getNowDate());
                mSysTimeTv.setText(DateFormatUtil.getNowTimeHM());;
                break;
        }
    }

    /**
     * 迈冲科技版本设置时间和日期
     */
//    private void setDateByEnjoySDK(){
//        try {
//            boolean state = enjoySDK.isAutoDateAndTime();
//        }catch (Exception e){
//            e.getMessage();
//        }
//
//    }

}
