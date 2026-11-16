package com.paul.pipeline.data;

import android.util.Log;
import android.widget.Toast;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.util.StringUtil;
import com.paul.pipeline.model.SampleHoleModel;

import java.util.ArrayList;

public class ScanCodeHelper {
    public static final int CodeLength = 19;
    public static SampleHoleModel parseSampleHoleModel(SampleHoleModel hole){
        if(hole!=null){
            if(hole.reagentCode.get()!=null){
               String code = hole.reagentCode.get();
               if(code.length()==CodeLength){
                   hole.produceddate.set(getDate(code));
                   hole.year.set(getYear(code));
                   hole.month.set(getMonth(code));
                   hole.producednum.set(getNumber(code));
                   hole.info.set(getInfo(code));
                   hole.infopath.set(getTempPath(hole.info.get()));
                   hole.infotemplate.set(getTempNumber(hole.info.get()));
                   hole.enddate.set(getEndDate(hole.produceddate.get() ,hole.year.get()));
                   hole.canuse.set(checkEndDate(hole.produceddate.get() ,hole.year.get()));
               }
            }
        }
        return hole;
    }
    public static String getTempPath(String value){
        String text = "";
        if(value!=null&&value.length()==4) {
            text = value.substring(0,value.length() - 2);
        }
        return text;
    }
    public static String getTempNumber(String value){
        String text = "";
        if(value!=null&&value.length()==4) {
            text = value.substring(value.length() - 2, value.length());
        }
        return text;
    }

    public static String getInfo(String value){
        String text = "";
        if(value!=null&&value.length()==CodeLength) {
            text = value.substring(value.length() - 4, value.length());
        }
        return text;
    }

    public static String getDate(String value){
        String text = "";
        if(value!=null&&value.length()==CodeLength) {
            text = value.substring(0,6);
        }
        return text;
    }

    public static String getYear(String value){
        String text = "";
        if(value!=null&&value.length()==CodeLength) {
            text = value.substring(6,7);
        }
        return text;
    }
    public static String getMonth(String value){
        String text = "";
        if(value!=null&&value.length()==CodeLength) {
            text = value.substring(7,9);
        }
        return text;
    }
    public static String getNumber(String value){
        String text = "";
        if(value!=null&&value.length()==CodeLength) {
            text = value.substring(9,15);
        }
        return text;
    }
    public static boolean checkEndDate(String produed,String year){
        String enddate = getEndDate(produed, year);
        String nowDate = DateFormatUtil.getNowDate_2();
        if (enddate!=null&&!enddate.isEmpty()) {
            return DateFormatUtil.compareDate(enddate, nowDate);
        }
        return false;
    }
    public static String getEndDate(String produed,String year){
        String enddatestr = "";
        if(produed!=null&&produed.length()==6){
            String yearnumber = produed.substring(0,2);
            String laststr = produed.substring(2,produed.length());
            try {
               int number = Integer.parseInt(yearnumber);
               int useyear = Integer.parseInt(year);
                int endyear = number + useyear;
                enddatestr = endyear +"" + laststr;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return "";
            }
        }
        return enddatestr;
    }
//    public static void checkScaninfo(String value){
//        String reagentCode = value.substring(24, 32);  //试管名称
//        String endDate = value.substring(14, 20);
//        String serialNum = value.substring(value.length() - 2, value.length());
//        Log.i("scan","endDate:"+endDate);
////            弹出对话框
////            获取当前的日期
//        String nowDate = DateFormatUtil.getNowDate_2();
//        if (DateFormatUtil.compareDate(endDate, nowDate)) {
//            //弹出确定对话框
//            String templateName = LocalFileHelper.getTemplateName(serialNum,temList);
//            if(!StringUtil.isEmpty(templateName)){
                //   PcrApplication.rootTempName = templateName;
//                showNoticeDialog(0,getActivity().getResources().getString(R.string.dialog_title_prompt),
//                        getActivity().getResources().getString(R.string.template_dialog_message_current_select_template)+"\n"+
//                                templateName,reagentCode);
//            }else{
//                showNoticeDialog(1,getActivity().getResources().getString(R.string.dialog_title_prompt),
//                        getActivity().getResources().getString(R.string.template_dialog_message_no_template),reagentCode);
//            }
//        } else {
            //弹出过期提示对话框//提示红色
//            Toast.makeText(getContext(),
//                    getActivity().getResources().getString(R.string.template_dialog_message_expired_reagent),
//                    Toast.LENGTH_LONG).show();
//        }
//    }
}
