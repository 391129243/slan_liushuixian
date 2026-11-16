package com.gidi.bio_pcr.slan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.db.ExperimentModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录-项目检测记录
 */
public class ExSLANResultAdapter extends BaseAdapter {

    private List<ExperimentModel> exprimentResults = new ArrayList<ExperimentModel>();
    private int index = 0;
    private int VIEW_COUNT = 10;
    private LayoutInflater mInflater;

    public ExSLANResultAdapter(Context context){
        exprimentResults = new ArrayList<ExperimentModel>();
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setExperimentList(List<ExperimentModel> list){
        this.exprimentResults.clear();
        this.exprimentResults.addAll(list);
        this.notifyDataSetChanged();
    }

    //index 表示第几页
    public void setIndex(int index){
        this.index = index;
        this.notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return exprimentResults.size();
    }

    @Override
    public Object getItem(int position) {
        return exprimentResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ExViewHolder holder;
        if(null != convertView){
            holder = (ExViewHolder) convertView.getTag();
        }else {
            holder = new ExViewHolder();
            convertView = mInflater.inflate(R.layout.item_list_slan_ex_model, viewGroup, false);
            holder.numTv = (TextView)convertView.findViewById(R.id.item_list_ex_num_tv);
        //    holder.deviceIdTv = (TextView)convertView.findViewById(R.id.item_list_ex_deviceid_tv);
            holder.startTimeTv = (TextView)convertView.findViewById(R.id.item_list_ex_starttime_tv);
            holder.endTimeTv = (TextView)convertView.findViewById(R.id.item_list_ex_endtime_tv);
            holder.testItemTv = (TextView)convertView.findViewById(R.id.item_list_ex_result_testitem_tv);
            holder.descriptionTv = (TextView)convertView.findViewById(R.id.item_list_ex_result_description_tv) ;
            holder.itemLayout = (LinearLayout)convertView.findViewById(R.id.item_ex_result_layout);
            holder.itemCk = (ImageView)convertView.findViewById(R.id.item_ex_result_ck);
            convertView.setTag(holder);
        }
        int pos = position + index * VIEW_COUNT;
        ExperimentModel result = exprimentResults.get(position);
        if(null != result){
            holder.numTv.setText(pos+"");
          //  holder.deviceIdTv.setText(result.getDeviceId());
            holder.startTimeTv.setText(result.getStartTime());
            holder.endTimeTv.setText(result.getEndTime());
            holder.testItemTv.setText(result.getTestItem());
            int exp_type = result.getExpType();
            holder.descriptionTv.setText(result.getDescription());
            if(position % 2 == 0){
                holder.itemLayout.setBackgroundResource(R.color.color_item_odd);
            }else {
                holder.itemLayout.setBackgroundResource(R.color.color_item_even);
            }

            if(result.isShow()){
                holder.itemCk.setVisibility(View.VISIBLE);
            }else {
                holder.itemCk.setVisibility(View.GONE);
            }

            if(result.isCheck()){
                holder.itemCk.setImageResource(R.drawable.icon_item_check);
            }else {
                holder.itemCk.setImageResource(R.drawable.icon_item_uncheck);
            }


            holder.itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!result.isShow()){
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
                    if(!result.isShow()){
                        if(null != onItemClickListener){
                            onItemClickListener.onItemLongClick(position);
                        }
                    }

                    return false;
                }
            });
        }
        return convertView;
    }

    class ExViewHolder{
        TextView numTv;
        TextView deviceIdTv;
        TextView startTimeTv;
        TextView endTimeTv;
        TextView testItemTv;
        TextView descriptionTv;//项目类型，是丁智还是宏石
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
