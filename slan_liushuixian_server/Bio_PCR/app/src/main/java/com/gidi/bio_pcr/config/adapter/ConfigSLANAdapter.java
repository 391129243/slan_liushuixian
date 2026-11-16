package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.config.ConfigSLANSection;
import com.gidi.bio_pcr.constant.Constants;
import com.gidi.bio_pcr.util.DateFormatUtil;
import com.gidi.bio_pcr.view.CustomStepView;
import com.gidi.bio_pcr.view.MeltStepView;

import java.util.ArrayList;
import java.util.List;

/**
 * 创建/编辑SLAN程序
 */
public class ConfigSLANAdapter extends RecyclerView.Adapter {

    private List<ConfigSLANSection> programs;
    private List<ConfigSLANSection.Step> steps = new ArrayList<ConfigSLANSection.Step>();
    private int selectedPosition = -1;

    private Context context;


    public ConfigSLANAdapter(Context context) {
        this.context = context;
    }

    public void setProgramSteps(List<ConfigSLANSection> list){
        this.programs = list;
        steps.clear();
        for(ConfigSLANSection program : programs){
            steps.addAll(program.getItems());
        }
        this.notifyDataSetChanged();
    }

    public void setSelectedPosition(int selectedPosition){
        this.selectedPosition =selectedPosition;
        notifyDataSetChanged();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if(viewType == Constants.PROGRAM_TYPE_AMP){
            View layout = inflater.inflate(R.layout.item_list_config_slan_step, parent, false);
            final ViewHolder holder1 = new ViewHolder(layout);
            return holder1;
        }else if(viewType == Constants.PROGRAM_TYPE_MELT){
            View view = inflater.inflate(R.layout.item_list_config_slan_melt_step, parent, false);
            final MeltHolder holder2 = new MeltHolder(view);
            return holder2;
        }else{
            return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            // 绑定数据到Type1ViewHolder
            ViewHolder viewHolder = (ViewHolder)holder;
            if (position == 0) {
                viewHolder.lineView.setDrawLeftLine(true);
                viewHolder.lineView.setlastValue(0);
            } else {
                viewHolder.lineView.setDrawLeftLine(true);
                viewHolder.lineView.setlastValue((int)(steps.get(position - 1).getTemperature()));
            }

            String holdTime = DateFormatUtil.secondToMS(steps.get(position).getHoldTime());
            viewHolder.lineView.setStepText((int)(steps.get(position).getTemperature()),holdTime);


            if (selectedPosition == position) {
                viewHolder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.color_item_pressed_bg));
            } else {
                viewHolder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.white));
            }
            int stepIndex = steps.get(position).getStep_index()+1;
            viewHolder.stepTv.setText(context.getResources().getString(R.string.config_step) + stepIndex);

            boolean isAcquisition = steps.get(position).isAcquisition();
            if(isAcquisition){
                viewHolder.acquisitionImg.setImageResource(R.drawable.icon_acquisition_check);
            }else {
                viewHolder.acquisitionImg.setImageResource(R.drawable.icon_acquisition_uncheck);
            }
        } else if (holder instanceof MeltHolder) {
            // 绑定数据到Type2ViewHolder
            MeltHolder viewHolder = (MeltHolder)holder;
            if (position == 0) {
                viewHolder.meltStepView.setDrawLeftLine(true);
                viewHolder.meltStepView.setlastValue(0);
            } else {
                viewHolder.meltStepView.setDrawLeftLine(true);
                viewHolder.meltStepView.setlastValue((int)(steps.get(position-1).getTemperature()));
            }

            if (selectedPosition == position) {
                viewHolder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.color_item_pressed_bg));
            } else {
                viewHolder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.white));
            }

            viewHolder.meltStepView.setStepText((int)(steps.get(position).getLeftTemperature()),
                    (int)steps.get(position).getRightTemperature(),"");
            viewHolder.acquisitionImg.setImageResource(R.drawable.icon_acquisition_check);

        }
    }


    /**
     * 根据返回类型，加载不同布局
     * @param position position to query
     * @return
     */
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
        RelativeLayout rootLayout;
        CustomStepView lineView;
        TextView stepTv;
        ImageView acquisitionImg;
        ImageView timeSetImg;
        ImageView tempSetImg;
        TextView devider;


        public ViewHolder(View itemView) {
            super(itemView);
            rootLayout = (RelativeLayout)itemView.findViewById(R.id.item_pcr_config_step_layout);
          //  devider = (TextView)itemView.findViewById(R.id.item_slan_config_devider);
            stepTv = (TextView)itemView.findViewById(R.id.item_slan_config_step_name_tv) ;
            lineView = (CustomStepView) itemView.findViewById(R.id.item_slan_config_step_view_tv);
            acquisitionImg = (ImageView)itemView.findViewById(R.id.item_slan_config_step_acquisition_img);
            timeSetImg = (ImageView)itemView.findViewById(R.id.item_slan_config_step_time_img) ;
            tempSetImg = (ImageView)itemView.findViewById(R.id.item_slan_config_step_temp_img);
            lineView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 获取当前点击的位置
                    int position = getAdapterPosition();
                    // 如果当前位置和之前选中的位置不同，则更新选中位置，并刷新RecyclerView
                    if (position != -1 && position != selectedPosition) {
                        selectedPosition = position;
                        notifyDataSetChanged();

                        if(onItemClickListener != null){
                            ConfigSLANSection.Step step = steps.get(position);
                            int section_index = step.getSectionIndex();
                            int step_index = step.getStep_index();
                            onItemClickListener.onItemClick(section_index,step_index);
                        }
                    }
                }
            });

            acquisitionImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(-1 != position && onItemClickListener != null){
                        ConfigSLANSection.Step step = steps.get(position);
                        int section_index = step.getSectionIndex();
                        int step_index = step.getStep_index();
                        onItemClickListener.onAcquisitionClick(section_index,step_index);
                    }
                }
            });

            timeSetImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(position != -1 && onItemClickListener != null){
                        ConfigSLANSection.Step step = steps.get(position);
                        int section_index = step.getSectionIndex();
                        int step_index = step.getStep_index();
                        onItemClickListener.onItemTimeOnClick(view,section_index,step_index);
                    }
                }
            });

            tempSetImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(position != -1 && onItemClickListener != null){
                        ConfigSLANSection.Step step = steps.get(position);
                        int section_index = step.getSectionIndex();
                        int step_index = step.getStep_index();
                        onItemClickListener.onItemTempOnClick(view,section_index,step_index,0);
                    }
                }
            });
        }
    }


    class MeltHolder extends RecyclerView.ViewHolder {
        // Type1 item的视图组件
        RelativeLayout rootLayout;
        MeltStepView meltStepView;
        ImageView acquisitionImg;
        ImageView rightTempSetImg;
        ImageView leftTempSetImg;

        public MeltHolder(View itemView) {
            super(itemView);
            // 初始化Type1 item的视图组件
            rootLayout = (RelativeLayout)itemView. findViewById(R.id.item_pcr_melt_step_layout);
            meltStepView = (MeltStepView)itemView.findViewById(R.id.item_slan_melt_step_view_tv);
            acquisitionImg = (ImageView) itemView.findViewById(R.id.item_slan_melt_step_acquisition_img);
            rightTempSetImg = (ImageView)itemView.findViewById(R.id.item_slan_melt_step_right_temp_img) ;
            leftTempSetImg = (ImageView)itemView.findViewById(R.id.item_slan_melt_step_temp_img) ;

            meltStepView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // 获取当前点击的位置
                    int position = getAdapterPosition();
                    // 如果当前位置和之前选中的位置不同，则更新选中位置，并刷新RecyclerView
                    if (position != selectedPosition) {
                        selectedPosition = position;
                        notifyDataSetChanged();

                        if(onItemClickListener != null){
                            ConfigSLANSection.Step step = steps.get(position);
                            int section_index = step.getSectionIndex();
                            int step_index = step.getStep_index();
                            onItemClickListener.onItemClick(section_index,step_index);
                        }
                    }
                }
            });

            rightTempSetImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(onItemClickListener != null){
                        ConfigSLANSection.Step step = steps.get(position);
                        int section_index = step.getSectionIndex();
                        int step_index = step.getStep_index();
                        onItemClickListener.onItemTempOnClick(view,section_index,step_index,2);
                    }
                }
            });

            leftTempSetImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if(onItemClickListener != null){
                        ConfigSLANSection.Step step = steps.get(position);
                        int section_index = step.getSectionIndex();
                        int step_index = step.getStep_index();
                        onItemClickListener.onItemTempOnClick(view,section_index,step_index,1);
                    }
                }
            });
        }
    }


    public interface OnItemClickListener{
        void onItemClick(int sec_index,int step_index);

        void onAcquisitionClick(int sec_index,int step_index);

        void onItemTimeOnClick(View view,int sec_index,int step_index);
        void onItemTempOnClick(View view,int sec_index,int step_index,int tempType);
    }
    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }
}
