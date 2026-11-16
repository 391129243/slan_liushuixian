package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.util.Log;
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
 * 定量检测范围
 */
public class DetectRangeAdapter extends BaseAdapter {
    private LayoutInflater layoutInflater;
    private List<ConfigTarget> detectRanges;
    private DecimalFormat nf = new DecimalFormat( "0.00");

    public DetectRangeAdapter(Context context){
        this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        detectRanges = new ArrayList<ConfigTarget>();
    }

    public void setRangeList(List<ConfigTarget> list){
        detectRanges = list;
        Collections.sort(detectRanges);
        this.notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return detectRanges.size();
    }

    @Override
    public Object getItem(int i) {
        return detectRanges.get(i);
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
            view = layoutInflater.inflate(R.layout.item_list_amp_detect_range, viewGroup, false);
            holder.channelTv = (TextView) view.findViewById(R.id.item_amp_detect_range_channel_tv);
            holder.detectionMinTv = (TextView) view.findViewById(R.id.item_amp_detect_min_tv);
            holder.detectionMaxTv = (TextView) view.findViewById(R.id.item_amp_detect_max_tv);

            view.setTag(holder);
        }
        if(null != detectRanges && detectRanges.size()>0){
            ConfigTarget configTarget = detectRanges.get(position);
            if(null != configTarget){
                holder.channelTv.setText(configTarget.getChannelNo()+"");
                //True：定性分析 False：定量分析
                holder.detectionMinTv.setText(nf.format(configTarget.getDetectMin())+"");
                holder.detectionMaxTv.setText(nf.format(configTarget.getDetectMax())+"");
            }
        }
        //定量检测范围下限
        holder.detectionMinTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onChangeDetectMin(view,position);
                }
            }
        });

        //定量检测范围上限
        holder.detectionMaxTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onChangeDetectMax(view,position);
                }
            }
        });


        return view;
    }

    class ViewHolder{
        TextView channelTv;
        TextView detectionMinTv;//检测下限
        TextView detectionMaxTv;//检测上限
    }

    public interface OnItemClickListener{

        void onChangeDetectMin(View view,int position);

        void onChangeDetectMax(View view,int position);

    }

    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }
}
