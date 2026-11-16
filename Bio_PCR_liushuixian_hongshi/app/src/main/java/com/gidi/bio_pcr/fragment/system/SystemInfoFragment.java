package com.gidi.bio_pcr.fragment.system;

import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.base.BaseFragment;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.constant.SharedConstant;
import com.gidi.bio_pcr.db.LiteORMCtrlMgr;
import com.gidi.bio_pcr.util.CommonUtil;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.PreferenceUtil;
import com.gidi.bio_pcr.view.RingProgressBar;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 系统信息
 */
public class SystemInfoFragment extends BaseFragment {

    private RingProgressBar mDiskSizeBar;
    private RingProgressBar mExperimentBar;
    private RingProgressBar mWellBar;
    private RingProgressBar mValidBar;
    private RingProgressBar mRNADurationBar;
    private LinearLayout mManufactorLayout;
    private LinearLayout mModelLayout;//型号
    private LinearLayout mVersionLayout;//版本
    private LinearLayout mFullVersionLayout;//版本
    private LinearLayout mContactLayout;//联系人
    private LinearLayout mProductDateLayout;

    private String validDay;
    private int totalDay = 0;
    private int usedDay = 0;

    private long used_ex_size = 0;
    private long used_well_size = 0;
    //显示测试样本种类

    private long rna_extraction_duration = 0;//核提总时长
    private SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    protected int getLayoutResource() {
        return R.layout.module_fragment_setting_sysinfo;
    }

    @Override
    protected void initView(View rootView) {
        mDiskSizeBar = (RingProgressBar)rootView.findViewById(R.id.sys_info_disk_bar);
        mExperimentBar = (RingProgressBar)rootView.findViewById(R.id.sys_info_test_total_bar);
        mWellBar = (RingProgressBar)rootView.findViewById(R.id.sys_info_well_total_bar) ;
        mValidBar = (RingProgressBar)rootView.findViewById(R.id.sys_info_valid_bar);
        mRNADurationBar = (RingProgressBar)rootView.findViewById(R.id.sys_info_rna_extraction_total_time_bar);
        mManufactorLayout = (LinearLayout)rootView.findViewById(R.id.sys_info_manufactor_layout);
        mModelLayout = (LinearLayout)rootView.findViewById(R.id.sys_info_device_model_layout);
        mVersionLayout = (LinearLayout)rootView.findViewById(R.id.sys_info_version_layout);
        mFullVersionLayout  = (LinearLayout)rootView.findViewById(R.id.sys_info_full_version_layout);
        mContactLayout = (LinearLayout)rootView.findViewById(R.id.sys_info_contact_layout);
        mProductDateLayout = (LinearLayout)rootView.findViewById(R.id.sys_info_production_date_layout);

    }

    @Override
    protected void initListener() {

    }
    private void  initData(){


        TextView manufactTitleTv = (TextView)mManufactorLayout.findViewById(R.id.sys_info_item_title);
        TextView manufactTv = (TextView)mManufactorLayout.findViewById(R.id.sys_info_item_tv);
        manufactTitleTv.setText(R.string.sys_info_manufacturer);
       // manufactTv.setText(R.string.sys_info_guangzhou_lab);

        TextView modelTitleTv = (TextView)mModelLayout.findViewById(R.id.sys_info_item_title);
        TextView modelTv = (TextView)mModelLayout.findViewById(R.id.sys_info_item_tv);
        modelTitleTv.setText(R.string.sys_info_device_model);
      //  modelTv.setText(Constants.DEVICE_MODEL);

        TextView versionTitleTv = (TextView)mVersionLayout.findViewById(R.id.sys_info_item_title);
        TextView versionTv = (TextView)mVersionLayout.findViewById(R.id.sys_info_item_tv);
        String version = CommonUtil.getAppVersionName(getActivity().getApplicationContext());
        versionTitleTv.setText(R.string.sys_info_software_version);
        versionTv.setText("V1");

        TextView fullVersionTitleTv = (TextView)mFullVersionLayout.findViewById(R.id.sys_info_item_title);
        TextView fullVerVionTv = (TextView)mFullVersionLayout.findViewById(R.id.sys_info_item_tv);
        String fullVersion = CommonUtil.getAppVersionName(getActivity().getApplicationContext());
        fullVersionTitleTv.setText(R.string.sys_info_software_complete_version);
        fullVerVionTv.setText(fullVersion);

        TextView contactTitleTv = (TextView)mContactLayout.findViewById(R.id.sys_info_item_title);
        TextView contactTv = (TextView)mContactLayout.findViewById(R.id.sys_info_item_tv);
        contactTitleTv.setText(R.string.sys_info_contact);
     //   contactTv.setText(Constants.CONTACT);

        TextView produceTitleTv = (TextView)mProductDateLayout.findViewById(R.id.sys_info_item_title);
        TextView productTv = (TextView)mProductDateLayout.findViewById(R.id.sys_info_item_tv);
        produceTitleTv.setText(R.string.sys_info_production_date);
        productTv.setText(Constants.PRODUCTION_DATE);


        if(totalDay >= usedDay){
//            float percent =   (int)(((float)usedDay/(float) totalDay)*100);
//            mValidBar.setProgress(percent,percent+"%"+"/"+"5年");
            float percent =   (int)(((float)usedDay/(float) totalDay)*100);
            float usedYears =   (float)(((float)usedDay/(float) totalDay)*8);
            String text = (new DecimalFormat("#0.00")).format(usedYears);
            mValidBar.setProgress(percent,text+getActivity().getResources().getString(R.string.sys_info_year)
                    +"/"+"8"+getActivity().getResources().getString(R.string.sys_info_year) );
        }


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initVariable();
        getExperimentNum();
        getSlanWellNum();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        initData();
        getUsedSize();
        getRNAExtractionDuration();

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if(!isHidden()){
            getRNAExtractionDuration();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /**
     * 获取有效期的日期
     * @return
     */
    private String getValidDay(){
        String productDay = Constants.PRODUCTION_DATE;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar thisDay = Calendar.getInstance();
        thisDay.set(Calendar.YEAR, Integer.valueOf(productDay.substring(0,4)));
        thisDay.set(Calendar.MONTH, Integer.valueOf(productDay.substring(5,7))-1);	//月份 从0 开始
        thisDay.set(Calendar.DAY_OF_MONTH, Integer.valueOf(productDay.substring(8,10)));
        thisDay.add(Calendar.YEAR,5);
        return sdf.format(thisDay.getTime());

    }





    private void initVariable(){
        validDay = getValidDay();
        totalDay = DateFormatUtil.getDaysBetweenDay(validDay, Constants.PRODUCTION_DATE);
        usedDay = DateFormatUtil.getDaysBetweenDay(DateFormatUtil.getNowDate(),Constants.PRODUCTION_DATE);
    }

    private void getUsedSize(){

        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        long total =  totalBlocks * blockSize;
        long availableBlocks = stat.getAvailableBlocksLong();
        long available =  availableBlocks * blockSize;


//       long sdcardTotalSize =  CommonUtil.getSDcardTotalSize();
//       long usedTotalSize = getPCRDirSize();
//        long no_used_size = 2*1024*1024*1024L;
//        long available_size = sdcardTotalSize - no_used_size;
        if(total >= available){
            float percent =  (float)((float)(total - available)*100/(float) total);
            DecimalFormat df2 = new DecimalFormat("###0.00");
            mDiskSizeBar.setProgress(percent,df2.format(percent)+"%"+"/"+Formatter.formatFileSize(this.getActivity(),total));
        }
    }


    /**
     * 获取核提总时长
     */
    private void getRNAExtractionDuration(){
        rna_extraction_duration = PreferenceUtil.getInstance(getActivity().getApplicationContext())
                .getLongValue(SharedConstant.RNA_EXTRACTION_DURATION,(long)0);
        if(94608000>=rna_extraction_duration){
            //小于3年
            float percent =  (float)((float)(rna_extraction_duration)*100/(float) 94608000);
            mRNADurationBar.setProgress(percent,DateFormatUtil.secondToYearMonthDayTime(rna_extraction_duration));
        }
    }



    /**
     * PCR文件夹的容量
     */
    private long getPCRDirSize(){

        long dewit_size = 0;
        long pcr_size = 0;
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/PCR");
        if(null != file){
            pcr_size = dirSize(file);
        }
        File dewit_File = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/FQPCR");
        if(null != dewit_File){
            dewit_size = dirSize(dewit_File);
        }

        return pcr_size + dewit_size;
    }

    private static long dirSize(File dir) {
        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();//pcr中文文件夹
            if (fileList != null) {
                for(int i = 0; i < fileList.length; i++) {
                    // Recursive call if it's a directory
                    if(fileList[i].isDirectory()) {//还是一个文件夹
                        result += dirSize(fileList[i]);
                    } else {
                        // Sum the file size in bytes
                        result += fileList[i].length();
                    }
                }
            }
            return result; // return the file size
        }
        return 0;
    }

    /**
     * 样本测试和实验测试量
     */

    private void getExperimentNum(){
        new Thread(new Runnable() {
             @Override
             public void run() {
                 used_ex_size = LiteORMCtrlMgr.getInstance().getAllExperimentNumber();
                 if(null != getActivity() && isAdded()){
                     getActivity().runOnUiThread(new Runnable() {
                         @Override
                         public void run() {
                             if(null == getActivity() || !isAdded()) return;
                             if(3000 >= used_ex_size){
                                 float percent =  (float)((float)used_ex_size*100/(float) 3500);
                                 mExperimentBar.setProgress(percent, used_ex_size
                                         + getActivity().getResources().getString(R.string.sys_info_records));
                             }else{
                                 mExperimentBar.setProgress(100, used_ex_size
                                         + getActivity().getResources().getString(R.string.sys_info_records));
                             }
                         }
                     });
                 }
          }
       }).start();
    }


    private void getSlanWellNum(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                used_well_size = LiteORMCtrlMgr.getInstance().getAllBiowayWellResultNum();
                if(null != getActivity() && isAdded()){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(null == getActivity() || !isAdded()) return;
                            if(24000 >= used_well_size){
                                float percent =  (float)((float)used_well_size*100/(float) 24000);
                                mWellBar.setProgress(percent, used_well_size
                                        + getActivity().getResources().getString(R.string.sys_info_records));
                            }else{
                                mWellBar.setProgress(100, used_well_size
                                        + getActivity().getResources().getString(R.string.sys_info_records));
                            }
                        }
                    });
                }

            }
        }).start();
    }

}
