package com.gidi.bio_pcr.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.PeaktypeOptimization;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.hongshi.slansdk.slanenum.MeltPeakAlgorithm;
import com.hongshi.slansdk.slanenum.PeekType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 峰型优化
 */
public class PeakTypeOptimizeAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater layoutInflater;
    private List<PeaktypeOptimization> optimizations;
    private DecimalFormat nf = new DecimalFormat( "0.0");

    public PeakTypeOptimizeAdapter(Context context){
        this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        optimizations = new ArrayList<PeaktypeOptimization>();
        this.context = context;
    }

    public void setOptimizations(List<PeaktypeOptimization> list){
        optimizations = list;
        this.notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return optimizations.size();
    }

    @Override
    public Object getItem(int i) {
        return optimizations.get(i);
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
            view = layoutInflater.inflate(R.layout.item_list_peakheight_optimize_layout, viewGroup, false);
            holder.dyeTv = (TextView)view.findViewById(R.id.item_peak_optimize_paramter_dye_tv);
            holder.channelTv = (TextView) view.findViewById(R.id.item_peak_optimize_channel_tv);
            holder.enableTv = (TextView) view.findViewById(R.id.item_peak_optimize_enable_tv);
            holder.enhanceLowerTv = (TextView) view.findViewById(R.id.item_peak_optimize_enhance_lower_tv);
            holder.enhanceUpperTv = (TextView) view.findViewById(R.id.item_peak_optimize_enhance_upper_tv);
            holder.enhanceGainTv = (TextView)view.findViewById(R.id.item_peak_optimize_enhance_gain_tv);
            holder.weakenTv = (TextView) view.findViewById(R.id.item_peak_optimize_weaken_tv);
            holder.weakenGainTv = (TextView)view.findViewById(R.id.item_peak_optimize_weaken_gain_tv);
            holder.slopeThresholdTv = (TextView)view.findViewById(R.id.item_peak_optimize_slope_thres_tv);
            view.setTag(holder);
        }
        if(null != optimizations && optimizations.size()>0){
            PeaktypeOptimization optimization = optimizations.get(position);
            if(null != optimization){
                holder.dyeTv.setText(optimization.getDyeName());
                holder.channelTv.setText(optimization.getChannelNo()+"");
                holder.enableTv.setText(optimization.isOptimization_enable()+"");
                holder.enhanceLowerTv.setText(nf.format(optimization.getEnhance_lower()));
                holder.enhanceUpperTv.setText(nf.format(optimization.getEnhance_upper()));
                holder.enhanceGainTv.setText(nf.format(optimization.getEnhanceGain()));
                holder.weakenTv.setText(nf.format(optimization.getWeaken()));
                holder.weakenGainTv.setText(nf.format(optimization.getWeakenGain()));
                holder.slopeThresholdTv.setText(nf.format(optimization.getSlopeThreshold()));

            }
        }
        //是否启用
        holder.enableTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onEnableOptimization(view,position);
                }
            }
        });

        holder.enhanceLowerTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onEnhanceLower(view,position);
                }
            }
        });
        holder.enhanceUpperTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onEnhanceUpper(view,position);
                }
            }
        });

        holder.enhanceGainTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onEnhanceGain(view,position);
                }
            }
        });


        holder.weakenTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onWeaken(view,position);
                }
            }
        });

        holder.weakenGainTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onWeakenGain(view,position);
                }
            }
        });

        holder.slopeThresholdTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onSlopeThreshold(view,position);
                }
            }
        });

        return view;
    }

    class ViewHolder{
        TextView dyeTv;
        TextView channelTv;
        TextView enableTv;//是否启用
        TextView enhanceLowerTv;//增强峰高阈值下限

        TextView enhanceUpperTv;//增强峰高阈值上限
        TextView enhanceGainTv;//增强增益
        TextView weakenTv;//削弱峰高阈值
        TextView weakenGainTv;//削弱增益
        TextView slopeThresholdTv;//斜率阈值
    }

    public interface OnItemClickListener{

        void onEnableOptimization(View view,int position);//峰型优化使能
        void onEnhanceLower(View view,int position);//增强峰高阈值下限

        void onEnhanceUpper(View view,int position);//增强峰高阈值上限


        void onEnhanceGain(View view,int position);//增强增益

        void onWeaken(View view,int position);//削弱峰高阈值
        void onWeakenGain(View view,int position);//削弱增益
        void onSlopeThreshold(View view,int position);//斜率阈值
    }

    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }
}
