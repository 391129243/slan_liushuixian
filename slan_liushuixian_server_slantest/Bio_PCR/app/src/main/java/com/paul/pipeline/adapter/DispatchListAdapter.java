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
import com.gidi.bio_pcr.databinding.LayoutConnectlistRecycleviewItemBinding;
import com.gidi.bio_pcr.databinding.LayoutDispatchlistRecycleviewItemBinding;
import com.paul.pipeline.listener.OnItemChildClickListener;
import com.paul.pipeline.model.ConnectModel;
import com.paul.pipeline.model.DispatchModel;

import java.util.ArrayList;

public class DispatchListAdapter extends RecyclerView.Adapter<DispatchListAdapter.ViewHolder>{
    private ArrayList<DispatchModel> mList = new ArrayList<>();
    private final LayoutInflater mInflater;
    Context context;
    OnItemChildClickListener onItemChildClickListener;

    public OnItemChildClickListener getOnItemChildClickListener() {
        return onItemChildClickListener;
    }

    public void setOnItemChildClickListener(OnItemChildClickListener onItemChildClickListener) {
        this.onItemChildClickListener = onItemChildClickListener;
    }

    public DispatchListAdapter(Context activity, ArrayList<DispatchModel> list, OnItemChildClickListener listener) {
        this.context = activity;
        mList = list;
        mInflater = LayoutInflater.from(activity);
        setOnItemChildClickListener(listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutDispatchlistRecycleviewItemBinding itemBinding = DataBindingUtil.inflate(mInflater,
                R.layout.layout_dispatchlist_recycleview_item, parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DispatchModel model = mList.get(position);
        holder.getBinding().setModel(model);
        holder.getBinding().setRecyclerViewItemClick(new RecyclerViewItemClickMethod(model.index.get(), getOnItemChildClickListener()));
        holder.getBinding().itembgLl.setSelected(model.selected.get());
        if(model.target.get()!=null&&model.target.get()>0){
            holder.getBinding().llTarget.setVisibility(View.VISIBLE);
        }else{
            holder.getBinding().llTarget.setVisibility(View.INVISIBLE);
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

        private final LayoutDispatchlistRecycleviewItemBinding binding;

        public ViewHolder(LayoutDispatchlistRecycleviewItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public LayoutDispatchlistRecycleviewItemBinding getBinding() {
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
        public void itemViewClick(View view) {
            if (null == view) return;
            Log.i("RecycleView", "itemViewClick----:" + position);
            if(onItemChildClickListener!=null) {
                onItemChildClickListener.onItemChildClick(view, position);
            }
        }
    }

}
