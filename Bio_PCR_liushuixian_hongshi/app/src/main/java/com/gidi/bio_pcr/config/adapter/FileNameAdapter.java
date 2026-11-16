package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;

import java.util.ArrayList;
import java.util.List;

public class FileNameAdapter extends RecyclerView.Adapter<FileNameAdapter.TempHolder> {

    private List<String> templateList;
    private int selectedPosition = -1;
    private Context context;

    public FileNameAdapter(Context context) {
        templateList = new ArrayList<String>();
        this.context = context;
    }

    public void setTemplateList(List<String> list) {
        this.templateList = list;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position){
        selectedPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public TempHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_config_info_projectinfo, parent, false);
        TempHolder holder = new TempHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(TempHolder holder, final int position) {
        String name = templateList.get(position);
        holder.tempNameTv.setText(name);
        if (selectedPosition == position) {
            holder.itemLayout.setBackgroundResource(R.drawable.shape_item_template_pressed);
            holder.tempNameTv.setTextColor(context.getResources().getColor(R.color.white));
        } else {
            holder.itemLayout.setBackgroundResource(R.drawable.shape_item_template_normal);
            holder.tempNameTv.setTextColor(context.getResources().getColor(R.color.blue));
        }

    }

    @Override
    public int getItemCount() {
        return templateList.size();
    }

    class TempHolder extends RecyclerView.ViewHolder {

        TextView tempNameTv;
        RelativeLayout itemLayout;

        public TempHolder(View itemView) {
            super(itemView);
            tempNameTv = (TextView) itemView.findViewById(R.id.item_temp_name_tv);
            itemLayout = (RelativeLayout)itemView.findViewById(R.id.template_item_layout);

            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemClick(position);
                    }
                }
            });

            itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    int position = getAdapterPosition();
                    if(position == selectedPosition){
                        if (null != onItemLongClickListener) {
                            onItemLongClickListener.onItemLongClick(position);
                        }
                    }

                    return false;
                }
            });
        }

    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    private OnItemLongClickListener onItemLongClickListener;
    public void setOnItemLongClickListener(OnItemLongClickListener listener){
        this.onItemLongClickListener = listener;
    }
}