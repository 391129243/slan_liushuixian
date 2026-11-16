package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.config.ConfigTarget;
import com.hongshi.slansdk.slanenum.MeltPeakAlgorithm;
import com.hongshi.slansdk.slanenum.PeekType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 熔解曲线参数
 */
public class MeltParamterAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater layoutInflater;
    private List<ConfigTarget> configTargets;
    private DecimalFormat nf = new DecimalFormat( "0.00");

    public MeltParamterAdapter(Context context){
        this.layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        configTargets = new ArrayList<ConfigTarget>();
        this.context = context;
    }

    public void setMeltParamter(List<ConfigTarget> list){
        configTargets = list;
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
            view = layoutInflater.inflate(R.layout.item_list_melt_paramter, viewGroup, false);
            holder.dyeTv = (TextView)view.findViewById(R.id.item_melt_paramter_dye_tv);
            holder.channelTv = (TextView) view.findViewById(R.id.item_melt_paramter_channel_tv);
            holder.minTempTv = (TextView) view.findViewById(R.id.item_melt_paramter_min_temp_tv);
            holder.maxTempTv = (TextView) view.findViewById(R.id.item_melt_paramter_max_temp_tv);
            holder.meltPeakAlgorithmTv = (TextView) view.findViewById(R.id.item_melt_paramter_peakalgorithm_tv);
            holder.peekHighthresholdTv = (TextView)view.findViewById(R.id.item_melt_paramter_peekhigh_threshold_tv);
            holder.peekTypeTv = (TextView) view.findViewById(R.id.item_melt_paramter_peektype_tv);
            view.setTag(holder);
        }
        if(null != configTargets && configTargets.size()>0){
            ConfigTarget configTarget = configTargets.get(position);
            if(null != configTarget){
                holder.dyeTv.setText(configTarget.getName());
                holder.channelTv.setText(configTarget.getChannelNo()+"");
                holder.minTempTv.setText(configTarget.getMinTemperature()+"");
                holder.maxTempTv.setText(configTarget.getMaxTemperature()+"");
                holder.peekHighthresholdTv.setText(nf.format(configTarget.getPeekHighThreshold())+"");
                Log.i("MeltParamterAdapter","configTarget.getMeltPeakAlgorithm()"
                        + context.getResources().getString(R.string.config_info_meltpeakalgorithm_advance));
                if(configTarget.getMeltPeakAlgorithm() == MeltPeakAlgorithm.Standard.getValue()){
                    holder.meltPeakAlgorithmTv.setText(context.getResources().getString(R.string.config_info_meltpeakalgorithm_standard));
                }else if(configTarget.getMeltPeakAlgorithm() == MeltPeakAlgorithm.Advance.getValue()){
                    holder.meltPeakAlgorithmTv.setText(context.getResources().getString(R.string.config_info_meltpeakalgorithm_advance));
                }

                if(configTarget.getPeekType() == PeekType.Positive.getValue()){
                    holder.peekTypeTv.setText(context.getResources().getString(R.string.config_info_peak_type_positive));
                }else if (configTarget.getPeekType() == PeekType.Negative.getValue()){
                    holder.peekTypeTv.setText(context.getResources().getString(R.string.config_info_peak_type_negative));
                }else if (configTarget.getPeekType() == PeekType.Positive_Negative.getValue()){
                    holder.peekTypeTv.setText(context.getResources().getString(R.string.config_info_peak_type_positive_negative));
                }
            }
        }
        //分析类型
        holder.minTempTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onChangeMinTemperature(view,position);
                }
            }
        });

        holder.maxTempTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onChangeMaxTemperature(view,position);
                }
            }
        });
        holder.meltPeakAlgorithmTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onMeltPeakAlgorithmTv(view,position);
                }
            }
        });

        holder.peekHighthresholdTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onChangePeekHighthreshold(view,position);
                }
            }
        });


        holder.peekTypeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onPeekType(view,position);
                }
            }
        });

        return view;
    }

    class ViewHolder{
        TextView dyeTv;
        TextView channelTv;
        TextView minTempTv;//最低温度
        TextView maxTempTv;//最高温度

        TextView meltPeakAlgorithmTv;//熔解曲线算法
        TextView peekHighthresholdTv;
        TextView peekTypeTv;//熔解峰型
    }

    public interface OnItemClickListener{

        void onChangeMinTemperature(View view,int position);
        void onChangeMaxTemperature(View view,int position);

        void onMeltPeakAlgorithmTv(View view,int position);


        void onChangePeekHighthreshold(View view,int position);

        void onPeekType(View view,int position);//数字滤波
    }

    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }
}
