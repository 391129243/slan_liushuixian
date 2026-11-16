package com.gzlab.autoassemblyline_client.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.gzlab.autoassemblyline_client.R;

import java.util.List;

/**
 * @author: xtxiaolu
 * @date: 2023/7/7
 * 描述:
 */
public class CustomSpinnerAdapter<T> extends BaseAdapter {

    private OnItemClickListener itemClickListener;

    private Context mContext;
    private List<T>  mDatas;

    public CustomSpinnerAdapter(Context context,List<T> datas){
        this.mContext = context;
        this.mDatas = datas;
    }


    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int i) {
        return mDatas.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list_spinner, null);
            holder.itemView = (TextView) convertView.findViewById(R.id.item_spinner_tv);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.itemView.setText(mDatas.get(position) + "");

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != itemClickListener){
                    itemClickListener.onItemClick(position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(null != itemClickListener){
                    itemClickListener.onItemLongClick(position);
                }
                return true;
            }
        });
        return convertView;
    }

    class ViewHolder{
        TextView itemView;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }


}
