package com.paul.pipeline.adapter;

import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.view.CustomView;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.utils.NumberUtil;
import com.paul.pipeline.utils.TimeUtil;
import com.paul.pipeline.view.GreenRedView;

public class Converters {
    @BindingAdapter("doubleToString")
    public static void setDoubleToString(TextView textView, double number) {
        String str = NumberUtil.doubleto1String(number);
        textView.setText(str+"");
    }
    @BindingAdapter("secondToString")
    public static void setSecondToString(TextView textView, long number) {
        String str = TimeUtil.secondsToTimeSimple((int)number);
        textView.setText(str+"");
    }

    @BindingAdapter("longToString")
    public static void setLongToString(TextView textView, long number) {
        textView.setText(String.valueOf(number)+"");
    }
    @BindingAdapter("intToString")
    public static void setIntToString(TextView textView, int number) {
        textView.setText(String.valueOf(number)+"");
    }
    @BindingAdapter("intTrayString")
    public static void setIntTrayString(TextView textView, int number) {
        if(number==0){
            textView.setVisibility(View.INVISIBLE);
        }else{
            textView.setVisibility(View.VISIBLE);
        }
        textView.setText(textView.getContext().getString(R.string.pipe_main_trayname) + String.valueOf(number));
    }

    @BindingAdapter("intAddString")
    public static void setTrayHole(TextView textView,int number) {
        String old = textView.getText().toString();
        textView.setText(old + String.valueOf(number)+"");
    }

    @BindingAdapter("boolToString")
    public static void setBoolToString(TextView textView,boolean value) {
        if(value) {
            textView.setText(textView.getContext().getString(R.string.yes) + "");
        }else{
            textView.setText(textView.getContext().getString(R.string.no) + "");
        }
    }

    @BindingAdapter("armDoorString")
    public static void setArmDoorString(TextView textView,boolean value) {
        if(value) {
            textView.setText(textView.getContext().getString(R.string.arm_door_open) + "");
            textView.setTextColor(textView.getContext().getColor(R.color.red));
        }else{
            textView.setText(textView.getContext().getString(R.string.arm_door_close) + "");
            textView.setTextColor(textView.getContext().getColor(R.color.black));
        }
    }

    @BindingAdapter("intToBool")
    public static void setIntToBool(CheckBox view, int value) {
        if(value==0) {
            view.setChecked(false);
        }else {
            view.setChecked(true);
        }
    }

    @BindingAdapter("booleanToImgbg")
    public static void setBooleanToImgbg(GreenRedView view, boolean value) {
          view.setStateBackgroud(value);
    }

    @BindingAdapter("booleanToImg")
    public static void setBooleanToImg(CustomView view, boolean value) {
        if(value) {
            view.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            view.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }
    }

    @BindingAdapter("existImg")
    public static void setExistImg(CustomView view, boolean value) {
        if(value) {
            view.setStateBackgroud(R.drawable.shape_oval_green_bg);
        }else {
            view.setStateBackgroud(R.drawable.shape_oval_gray_bg);
        }
    }

    @BindingAdapter("statusToImg")
    public static void setStatusToImg(CustomView view, String value) {
        if(value==null||value.isEmpty()||value.isBlank()){
            view.setStateBackgroud(R.drawable.shape_oval_red_bg);
        }else {
            if (value.equalsIgnoreCase(ConstantData.str_ERROR)) {
                view.setStateBackgroud(R.drawable.shape_oval_red_bg);
            } else {
                view.setStateBackgroud(R.drawable.shape_oval_green_bg);
            }
        }
    }

    @BindingAdapter("stringStatusImg")
    public static void setStringStatusImg(View view, String value) {
      //  int status = ConstantData.ConvertStatus(value);
        if(value==null||value.isEmpty()||value.isBlank()){
            view.setBackgroundResource(R.drawable.selector_tray_unused);
        }else {
            switch (value) {
                case ConstantData.str_FREE:
                    view.setBackgroundResource(R.drawable.selector_tray_unused);
                    break;
                case ConstantData.str_ACK:
                    view.setBackgroundResource(R.drawable.selector_tray_acking);
                    break;
                case ConstantData.str_RUN:
                    view.setBackgroundResource(R.drawable.selector_tray_using);
                    break;
                case ConstantData.str_FINISH:
                    view.setBackgroundResource(R.drawable.selector_tray_done);
                    break;
                case ConstantData.str_ERROR:
                    view.setBackgroundResource(R.drawable.selector_tray_error);
                    break;
                default:
                    view.setBackgroundResource(R.drawable.selector_tray_unused);
                    break;
            }
        }
    }
    @BindingAdapter("stringStatusBg")
    public static void setStringStatusBg(View view, String value) {
        // 状态栏背景色：空闲无背景色、异常红色、运行/占用绿色、完成蓝色
        if(value==null||value.isEmpty()||value.isBlank()){
            view.setBackgroundResource(R.color.transparent);
        }else {
            switch (value) {
                case ConstantData.str_FREE:
                    view.setBackgroundResource(R.color.transparent);
                    break;
                case ConstantData.str_RUN:
                case ConstantData.str_ACK:
                    view.setBackgroundResource(R.color.color_spring_green);
                    break;
                case ConstantData.str_FINISH:
                    view.setBackgroundResource(R.color.blue);
                    break;
                case ConstantData.str_ERROR:
                    view.setBackgroundResource(R.color.red);
                    break;
                default:
                    view.setBackgroundResource(R.color.transparent);
                    break;
            }
        }
    }
    @BindingAdapter("stringStatus")
    public static void setStringStatus(TextView view, String value) {
      //  int status = ConstantData.ConvertStatus(value);
        if(value==null||value.isEmpty()||value.isBlank()){
            view.setText(view.getContext().getString(R.string.pipe_main_traystatus_unused));
        }else {
            switch (value) {
                case ConstantData.str_FREE:
                    view.setText(view.getContext().getString(R.string.pipe_main_traystatus_unused));
                    break;
                case ConstantData.str_RUN:
                    view.setText(view.getContext().getString(R.string.pipe_main_traystatus_using));
                    break;
                case ConstantData.str_FINISH:
                    view.setText(view.getContext().getString(R.string.pipe_main_traystatus_done));
                    break;
                case ConstantData.str_ERROR:
                    view.setText(view.getContext().getString(R.string.pipe_main_traystatus_error));
                    break;
                case ConstantData.str_ACK:
                    view.setText(view.getContext().getString(R.string.pipe_main_status_acking));
                    break;
                default:
                    view.setText(view.getContext().getString(R.string.pipe_main_traystatus_unused));
                    break;
            }
        }
    }

    @BindingAdapter("intStatusString")
    public static void setIntStatusString(TextView view, int value) {
        switch (value) {
            case ConstantData.STATUS_FREE:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_unused));
                break;
            case ConstantData.STATUS_RUN:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_using));
                break;
            case ConstantData.STATUS_FINISH:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_done));
                break;
            case ConstantData.STATUS_ERROR:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_error));
                break;
            default:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_unused));
                break;
        }
    }

    @BindingAdapter("booleanConnectString")
    public static void setBooleanConnectString(TextView view, boolean value) {
        if(value){
            view.setText(view.getContext().getString(R.string.connected));
        }else{
            view.setText(view.getContext().getString(R.string.dissconnected));
        }
    }
    @BindingAdapter("intStepString")
    public static void setIntStepString(TextView view, com.paul.pipeline.model.ArmStep step) {
        // armStep 现为 ArmStep 枚举，转回 int 复用下方原有分支，展示文案不变
        setIntStepString(view, step == null ? ConstantData.WORK_FREE : step.toInt());
    }

    /**
     * 三参版：armStep + 在位传感器 exist + allStatus 共同决定显示文案。
     * - 当 allStatus==error（设备/Linux 异常）时，优先显示“异常”，与状态栏红底一致。
     * - 当 armStep==BACK 且 exist==false（托盘已离开但状态未复位）时，视为空闲，避免“还托盘”残留。
     * - 其余情况委托原 armStep 文案逻辑，行为不变。
     * allStatus 为可选属性（requireAll=false）：未提供时为 null，退化为原“armStep + exist”行为。
     * 调度逻辑不受任何影响。
     */
    @BindingAdapter(value = {"intStepString", "trayExist", "allStatus"}, requireAll = false)
    public static void setIntStepString(TextView view, com.paul.pipeline.model.ArmStep step, Boolean exist, String allStatus) {
        if (allStatus != null && allStatus.equalsIgnoreCase(ConstantData.str_ERROR)) {
            // 设备异常：显示“异常”，与红色状态栏保持一致
            view.setText(view.getContext().getString(R.string.pipe_main_traystatus_error));
            return;
        }
        if (step == com.paul.pipeline.model.ArmStep.BACK && Boolean.FALSE.equals(exist)) {
            // 托盘已物理离开，状态未复位 → 显示空闲，避免残留"还托盘"
            view.setText(view.getContext().getString(R.string.pipe_main_traystatus_undispatch));
            return;
        }
        // 其余情况委托原逻辑，行为完全不变
        setIntStepString(view, step);
    }

    @BindingAdapter("intStepString")
    public static void setIntStepString(TextView view, int value) {
        switch (value) {
            case ConstantData.WORK_FREE:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_undispatch));
                break;
            case ConstantData.WORK_GET:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_ready));
                break;
            case ConstantData.WORK_RUN:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_dispatching));
                break;
            case ConstantData.WORK_BACK:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_dispatchdone));
                break;
            case ConstantData.WORK_MOVE:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_dispatch_move));
                break;
            case ConstantData.WORK_RECEIVE:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_dispatch_receive));
                break;
            default:
                view.setText(view.getContext().getString(R.string.pipe_main_traystatus_undispatch));
                break;
        }
    }
    @BindingAdapter("booleanUrgentString")
    public static void setBooleanUrgentString(TextView view, boolean value) {
        if(value){
            view.setText(view.getContext().getString(R.string.pipe_main_urgent));
            view.setTextColor(view.getContext().getColor(R.color.red));
        }else{
            view.setText(view.getContext().getString(R.string.pipe_main_unurgent));
            view.setTextColor(view.getContext().getColor(R.color.black));
        }
    }

    @BindingAdapter("intTypeName")
    public static void setIntTypeName(TextView view, int value) {
            switch (value) {
                case ConstantData.DEVICE_RNA:
                    view.setText(view.getContext().getString(R.string.pipe_main_rna_name));
                    break;
                case ConstantData.DEVICE_PCR:
                    view.setText(view.getContext().getString(R.string.pipe_main_pcr_name));
                    break;
                case ConstantData.DEVICE_TRAY:
                    view.setText(view.getContext().getString(R.string.pipe_main_trayname));
                    break;
                default:
                    view.setText("");
                    break;
            }

    }

    @BindingAdapter("booleanSwitchImage")
    public static void setBooleanSwitchImage(ImageView view, boolean value) {
        if(value){
            view.setImageResource(R.drawable.icon_wifi_on);
        }else{
            view.setImageResource(R.drawable.icon_wifi_off);
        }
    }

}
