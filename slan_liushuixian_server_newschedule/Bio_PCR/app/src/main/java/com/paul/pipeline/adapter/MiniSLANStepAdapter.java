package com.paul.pipeline.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.view.CustomStepView;
import com.gidi.bio_pcr.view.MeltStepView;

import java.util.ArrayList;
import java.util.List;

public class MiniSLANStepAdapter extends RecyclerView.Adapter {

    private List<ConfigSLANSection> programs;
    private List<ConfigSLANSection.Step> steps = new ArrayList<ConfigSLANSection.Step>();
    private int cur_step = 0;
    private int program_index = 0;


    public MiniSLANStepAdapter(Context context) {

    }

    public void setProgramSteps(List<ConfigSLANSection> list){
        this.programs = list;
        steps.clear();
        for(ConfigSLANSection program : programs){
            steps.addAll(program.getItems());
        }
        this.notifyDataSetChanged();
    }

    public void setCurrentPosition(int _cur_step,int _program_index){
        cur_step = _cur_step - 1;
        program_index = _program_index - 1;
        this.notifyDataSetChanged();
    }
    public void clearCurrentPosition(){
        if(steps!=null) {
            steps.clear();
        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if(viewType == Constants.PROGRAM_TYPE_AMP){
            View layout = inflater.inflate(R.layout.item_list_slan_config_run_step_mini, parent, false);
            return new ViewHolder(layout);
        }else if(viewType == Constants.PROGRAM_TYPE_MELT){
            View view = inflater.inflate(R.layout.item_list_slan_config_run_melt_step_mini, parent, false);
            return new MeltHolder(view);
        }else{
            return null;
        }
    }



    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ViewHolder){
           ViewHolder viewHolder = (ViewHolder)holder;
           int lastvalue = 0;
            if (position == 0) {
                viewHolder.lineView.setDrawLeftLine(true);
//                viewHolder.lineView.setlastValue(0);
                lastvalue = 0;
            } else {
                viewHolder.lineView.setDrawLeftLine(true);
                lastvalue = (int)(steps.get(position - 1).getTemperature());
               // viewHolder.lineView.setlastValue((int)(steps.get(position - 1).getTemperature()));
            }

            int type = steps.get(position).getStepType();
            if(type == Constants.PROGRAM_TYPE_AMP){
                String holdTime = DateFormatUtil.secondToMS(steps.get(position).getHoldTime());
                viewHolder.lineView.setStepText(lastvalue,(int)(steps.get(position).getTemperature()),holdTime);
            }else{
                viewHolder.lineView.setStepText(lastvalue,(int)(steps.get(position).getTemperature()),"");
            }
            viewHolder.stepTv.setText(steps.get(position).getStepName());

            if(null != programs){
                int count = 0;
                for(int i= 0;i<programs.size();i++){
                    if(i<program_index) {
                        count += programs.get(i).getItems().size();
                    }
                }
                if(count +  cur_step == position){
                    viewHolder.runImg.setVisibility(View.VISIBLE);
                }else{
                    viewHolder.runImg.setVisibility(View.INVISIBLE);
                }
            }
        }else if(holder instanceof MeltHolder){
            MeltHolder viewHolder = (MeltHolder)holder;
            if (position == 0) {
                viewHolder.meltStepView.setDrawLeftLine(true);
                viewHolder.meltStepView.setlastValue(0);
            } else {
                viewHolder.meltStepView.setDrawLeftLine(true);
                viewHolder.meltStepView.setlastValue((int)(steps.get(position-1).getTemperature()));
            }

            viewHolder.meltStepView.setStepText((int)(steps.get(position).getLeftTemperature()), (int)steps.get(position).getRightTemperature(),"");



            if(null != programs){
                int count = 0;
                for(int i= 0;i<program_index;i++){
                    count += programs.get(i).getItems().size();
                }

                if(count +  cur_step == position){
                    viewHolder.runImg.setVisibility(View.VISIBLE);
                }else{
                    viewHolder.runImg.setVisibility(View.INVISIBLE);
                }
            }
        }

    }

    @Override
    public int getItemViewType(int position) {
        int type = steps.get(position).getStepType();
        if(type == Constants.PROGRAM_TYPE_AMP){
            return Constants.PROGRAM_TYPE_AMP;
        }else {
            return Constants.PROGRAM_TYPE_MELT;
        }
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CustomStepView lineView;
        TextView stepTv;
        ImageView runImg;


        public ViewHolder(View itemView) {
            super(itemView);
            stepTv = (TextView)itemView.findViewById(R.id.item_slan_run_step_name_tv) ;
            lineView = (CustomStepView) itemView.findViewById(R.id.item_slan_run_step_view_tv);
            runImg = (ImageView)itemView.findViewById(R.id.item_slan_run_step_running_img);
        }
    }

    class MeltHolder extends RecyclerView.ViewHolder {
        // Type1 item的视图组件
        MeltStepView meltStepView;
        ImageView runImg;

        public MeltHolder(View itemView) {
            super(itemView);
            // 初始化Type1 item的视图组件
            meltStepView = (MeltStepView) itemView.findViewById(R.id.item_slan_run_melt_step_view_tv);
            runImg = (ImageView) itemView.findViewById(R.id.item_slan_run_melt_step_run_img);

        }
    }

}
