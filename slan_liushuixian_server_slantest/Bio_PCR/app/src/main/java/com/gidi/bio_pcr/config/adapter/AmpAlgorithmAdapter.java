package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.hongshi.slansdk.slanenum.DigitalFilter;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 扩增曲线算法
 */
public class AmpAlgorithmAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater layoutInflater;
    private List<ConfigTarget> configTargets;
    private DecimalFormat nf = new DecimalFormat( "0.00");

    public AmpAlgorithmAdapter(Context context){
        this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.context = context;
        configTargets = new ArrayList<ConfigTarget>();
    }

    public void setAmpAlgorithm(List<ConfigTarget> list){
        configTargets = list;
        Collections.sort(configTargets);
        this.notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return configTargets.size();
    }

    @Override
    public Object getItem(int i) {
        return configTargets.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if(null != view){
            holder = (ViewHolder) view.getTag();
        }else {
            holder = new ViewHolder();
            view = layoutInflater.inflate(R.layout.item_list_amp_algorithm, viewGroup, false);
            holder.dyeTv = (TextView)view.findViewById(R.id.item_amp_algrithm_dye_tv);
            holder.channelTv = (TextView) view.findViewById(R.id.item_amp_algrithm_channel_tv);
            holder.analyseTypeTv = (TextView) view.findViewById(R.id.item_amp_algrithm_analysetype_tv);
            holder.beginbaseTv = (TextView) view.findViewById(R.id.item_amp_algrithm_beginbase_tv);
            holder.endbaseTv = (TextView) view.findViewById(R.id.item_amp_algrithm_endbase_tv);
            holder.threshold = (TextView)view.findViewById(R.id.item_amp_threshold_tv);
            holder.autoThresholdImg = (ImageView)view.findViewById(R.id.item_amp_algrithm_autothreshold_img);
            holder.isAutothresholdLayout = (LinearLayout)view.findViewById(R.id.item_amp_algrithm_autothreshold_layout);
            holder.digitalFilterTv = (TextView) view.findViewById(R.id.item_amp_algrithm_digitalfilter_tv);
            holder.optimizationModeTv = (TextView) view.findViewById(R.id.item_amp_algrithm_optimizationmode_tv);

            view.setTag(holder);
        }
        if(null != configTargets && configTargets.size()>0){
            ConfigTarget configTarget = configTargets.get(position);
            if(null != configTarget){
                holder.dyeTv.setText(configTarget.getName());
                holder.channelTv.setText(configTarget.getChannelNo()+"");
                //True：定性分析 False：定量分析
                if(configTarget.isQualitativeAnalysis()){
                    holder.analyseTypeTv.setText(context.getResources().getString(R.string.config_info_ampays_qualitative));
                    holder.isAutothresholdLayout.setEnabled(false);
                    holder.threshold.setEnabled(true);
                }else{
                    holder.analyseTypeTv.setText(context.getResources().getString(R.string.config_info_ampays_quantitative));
                    holder.isAutothresholdLayout.setEnabled(true);
                    holder.threshold.setEnabled(false);
                }
                holder.beginbaseTv.setText(configTarget.getBeginBaseline()+"");
                holder.endbaseTv.setText(configTarget.getEndBaseline()+"");
                holder.threshold.setText(nf.format(configTarget.getThreshold())+"");
                if(configTarget.getDigitalFilter() == DigitalFilter.None.getValue()){
                    holder.digitalFilterTv.setText(context.getResources().getString(R.string.config_info_digitalfilter_none));
                }else if(configTarget.getDigitalFilter() == DigitalFilter.Normal.getValue()){
                    holder.digitalFilterTv.setText(context.getResources().getString(R.string.config_info_digitalfilter_normal));
                }else if(configTarget.getDigitalFilter() == DigitalFilter.High.getValue()){
                    holder.digitalFilterTv.setText(context.getResources().getString(R.string.config_info_digitalfilter_high));
                }

                if(configTarget.getOptimizationMode() == 0){
                    holder.optimizationModeTv.setText(context.getResources().getString(R.string.config_info_optimizationmode_na));
                }else if (configTarget.getOptimizationMode() == 1){
                    holder.optimizationModeTv.setText(context.getResources().getString(R.string.config_info_optimizationmode_auto));
                }else if (configTarget.getOptimizationMode() == 2){
                    holder.optimizationModeTv.setText(context.getResources().getString(R.string.config_info_optimizationmode_manual));
                }

                boolean isAutoThreshold  = configTarget.isAutoThreshold();
                if(isAutoThreshold){
                    holder.autoThresholdImg.setImageResource(R.drawable.check_pressed);
                }else{
                    holder.autoThresholdImg.setImageResource(R.drawable.check_normal);
                }

            }
        }
        //分析类型
        holder.analyseTypeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onQualitativeAnalysis(view,position);
                }
            }
        });

        holder.beginbaseTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onBeginBase(view,position);
                }
            }
        });
        holder.endbaseTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onEndbaseLine(view,position);
                }
            }
        });

        holder.threshold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onChangeThreshold(view,position);
                }
            }
        });

        //基线优化
        holder.optimizationModeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(onItemClickListener != null){
                    onItemClickListener.onOptimizationMode(view,position);
                }
            }
        });

        holder.isAutothresholdLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(onItemClickListener != null){
                    onItemClickListener.onAutoThreshold(position);
                }
            }
        });

        holder.digitalFilterTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onDigitalFilter(view,position);
                }
            }
        });

        return view;
    }

    class ViewHolder{
        TextView dyeTv;
        TextView channelTv;
        TextView analyseTypeTv;//序号
        TextView beginbaseTv;//基线起点

        TextView endbaseTv;//基线终点
        TextView threshold;
        LinearLayout isAutothresholdLayout;
        ImageView autoThresholdImg;
        TextView digitalFilterTv;//数字滤波
        TextView optimizationModeTv;
    }

    public interface OnItemClickListener{

        void onQualitativeAnalysis(View view,int position);//定性定量修改
        void onBeginBase(View view,int position);

        void onEndbaseLine(View view,int position);//基线终点

        void onOptimizationMode(View view,int position);//基线优化方式

        void onChangeThreshold(View view,int position);//手动阈值

        void onDigitalFilter(View view,int position);//数字滤波

        void onAutoThreshold(int position);
    }

    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }
}
