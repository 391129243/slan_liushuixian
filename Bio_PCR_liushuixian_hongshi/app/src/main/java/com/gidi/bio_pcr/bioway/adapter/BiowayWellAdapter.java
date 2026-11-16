package com.gidi.bio_pcr.bioway.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.db.BioWayWellResult;

import java.util.ArrayList;
import java.util.List;

public class BiowayWellAdapter extends BaseAdapter {
    int VIEW_COUNT = 10;
    private int index = 0;
    private List<BioWayWellResult> wellResults;
    private LayoutInflater mInflater;

    public BiowayWellAdapter(Context context){
        wellResults = new ArrayList<BioWayWellResult>();
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setDeviceList(List<BioWayWellResult> list){
        this.wellResults.clear();
        this.wellResults.addAll(list);
        notifyDataSetChanged();

    }

    public void setIndex(int index){
        this.index = index;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return wellResults.size();
    }

    @Override
    public Object getItem(int position) {
        return wellResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        WellHolder holder;
        if(null != convertView){
            holder = (WellHolder) convertView.getTag();
        }else{
            holder = new WellHolder();
            convertView = mInflater.inflate(R.layout.item_list_detect_result,viewGroup,false);
            holder.numTv = (TextView)convertView.findViewById(R.id.item_list_result_num_tv);
            holder.regentCodeTv =  (TextView)convertView.findViewById(R.id.item_list_result_regent_tv);
            holder.testtimeTv = (TextView)convertView.findViewById(R.id.item_list_result_time_tv);

            holder. idCardTv =  (TextView)convertView.findViewById(R.id.item_list_result_idcard_tv);
            holder.testItemTv = (TextView)convertView.findViewById(R.id.item_list_result_testitem_tv);
            holder.itemLayout = (LinearLayout)convertView.findViewById(R.id.item_detect_result_layout);
            holder.itemCk = (ImageView)convertView.findViewById(R.id.item_well_result_ck);
            convertView.setTag(holder);
        }
        int pos = position + index * VIEW_COUNT;
        BioWayWellResult bean = wellResults.get(position);
        holder.numTv.setText(pos+"");
        holder.regentCodeTv.setText(bean.getReagentCode());
        holder.idCardTv.setText(bean.getSampleName());//样本编号就是身份证号
        holder.testItemTv.setText(bean.getTestItem());
        holder.testtimeTv.setText(bean.getStartTime());

        if(bean.isShow()){
            holder.itemCk.setVisibility(View.VISIBLE);
        }else {
            holder.itemCk.setVisibility(View.GONE);
        }

        if(bean.isCheck()){
            holder.itemCk.setImageResource(R.drawable.icon_item_check);
        }else {
            holder.itemCk.setImageResource(R.drawable.icon_item_uncheck);
        }

        if(position % 2 == 0){
            holder.itemLayout.setBackgroundResource(R.color.color_item_odd);
        }else {
            holder.itemLayout.setBackgroundResource(R.color.color_item_even);
        }


        holder.itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!bean.isShow()){
                    if(null != onItemClickListener){
                        onItemClickListener.onItemClick(position);
                    }
                }else{
                    //多选框显示
                    if(null != onItemClickListener){
                        onItemClickListener.onItemCheck(position);
                    }
                }
            }
        });

        holder.itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(!bean.isShow()){
                    if(null != onItemClickListener){
                        onItemClickListener.onItemLongClick(position);
                    }
                }

                return false;
            }
        });

        return convertView;
    }

    class WellHolder {
        TextView numTv;
        TextView regentCodeTv;
        TextView testtimeTv;
        TextView idCardTv;
        TextView testItemTv;
        LinearLayout itemLayout;
        ImageView itemCk;
    }

    public interface OnItemClickListener{
        void onItemClick(int position);

        void onItemLongClick(int position);

        void onItemCheck(int position);
    }
    private OnItemClickListener onItemClickListener;
    public void setOnItemCliclListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }

}
