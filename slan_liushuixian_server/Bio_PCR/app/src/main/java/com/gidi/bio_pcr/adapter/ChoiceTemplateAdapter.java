package com.gidi.bio_pcr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;


import java.util.ArrayList;
import java.util.List;


public class ChoiceTemplateAdapter extends RecyclerView.Adapter<ChoiceTemplateAdapter.ViewHolder> {

    private List<String> templateList;
    private int selectedPosition = -1;

    public ChoiceTemplateAdapter(Context context){
        templateList = new ArrayList<String>();
        notifyDataSetChanged();
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_choice_template_layout,parent,false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String name = templateList.get(position);
        holder.templateNameTv.setText(name);
        if (selectedPosition == position) {
            holder.checkImg.setImageResource(R.drawable.check_pressed);
        } else {
            holder.checkImg.setImageResource(R.drawable.check_normal);
        }
    }

    @Override
    public int getItemCount() {
        return templateList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{

        ImageView checkImg;
        TextView templateNameTv;
        LinearLayout itemLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            checkImg = (ImageView) itemView.findViewById(R.id.item_template_check);
            templateNameTv =  (TextView)itemView.findViewById(R.id.item_template_name_tv);
            itemLayout = (LinearLayout)itemView.findViewById(R.id.item_layout_template);

            itemLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if(null != onItemClickListener){
                        onItemClickListener.onItemClick(position);
                    }
                }
            });
        }



    }

    public interface OnItemClickListener{
       void onItemClick(int position);
    }
    private OnItemClickListener onItemClickListener;
    public void setOnItemCliclListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }


}
