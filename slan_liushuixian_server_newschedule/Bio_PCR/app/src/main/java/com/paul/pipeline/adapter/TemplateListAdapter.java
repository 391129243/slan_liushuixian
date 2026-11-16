package com.paul.pipeline.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.ItemListTemplateNameBinding;
import com.paul.pipeline.listener.OnItemChildClickListener;
import com.paul.pipeline.model.TemplateModel;

import java.util.ArrayList;

public class TemplateListAdapter extends RecyclerView.Adapter<TemplateListAdapter.ViewHolder>{
    private ArrayList<TemplateModel> mList = new ArrayList<>();
    private final LayoutInflater mInflater;
    Context context;
    OnItemChildClickListener onItemChildClickListener;

    public OnItemChildClickListener getOnItemChildClickListener() {
        return onItemChildClickListener;
    }

    public void setOnItemChildClickListener(OnItemChildClickListener onItemChildClickListener) {
        this.onItemChildClickListener = onItemChildClickListener;
    }

    public TemplateListAdapter(Context activity, ArrayList<TemplateModel> list, OnItemChildClickListener listener) {
        this.context = activity;
        mList = list;
        mInflater = LayoutInflater.from(activity);
        setOnItemChildClickListener(listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemListTemplateNameBinding itemBinding = DataBindingUtil.inflate(mInflater, R.layout.item_list_template_name, parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getBinding().setModel(mList.get(position));
        holder.getBinding().setRecyclerViewItemClick(new RecyclerViewItemClickMethod(position, getOnItemChildClickListener()));
        boolean selected = mList.get(position).selected.get();
        if(selected){
            holder.binding.templatelistItemRl.setBackgroundResource(R.drawable.shape_item_user_template_pressed);
            holder.binding.nameTv.setTextColor(context.getResources().getColor(R.color.white));
        } else {
            holder.binding.templatelistItemRl.setBackgroundResource(R.drawable.shape_item_template_normal);
            holder.binding.nameTv.setTextColor(context.getResources().getColor(R.color.color_top_bar_pressed));
        }
        holder.getBinding().executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    /**
     * ViewHolder类
     */

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemListTemplateNameBinding binding;

        public ViewHolder(ItemListTemplateNameBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public ItemListTemplateNameBinding getBinding() {
            return this.binding;
        }
    }

    /**
     * 点击事件
     */

    public static class RecyclerViewItemClickMethod {

        private final int position;
        OnItemChildClickListener onItemChildClickListener;
        public RecyclerViewItemClickMethod(int position, OnItemChildClickListener onItemChildClickListener) {
            this.position = position;
            this.onItemChildClickListener = onItemChildClickListener;
        }

        //点击事件
        public void trayViewClick(View view) {
            if (null == view) return;
            Log.i("RecycleView", "templatelist ViewClick----:" + position);
            if(onItemChildClickListener!=null) {
                onItemChildClickListener.onItemChildClick(view, position);
            }
        }
    }

}
