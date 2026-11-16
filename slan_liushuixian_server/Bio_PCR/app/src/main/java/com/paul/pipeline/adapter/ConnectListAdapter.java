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
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.listener.OnItemChildClickListener;
import com.paul.pipeline.model.ConnectModel;

import java.util.ArrayList;

public class ConnectListAdapter extends RecyclerView.Adapter<ConnectListAdapter.ViewHolder>{
    private ArrayList<ConnectModel> mList = new ArrayList<>();
    private final LayoutInflater mInflater;
    Context context;
    OnItemChildClickListener onItemChildClickListener;

    public OnItemChildClickListener getOnItemChildClickListener() {
        return onItemChildClickListener;
    }

    public void setOnItemChildClickListener(OnItemChildClickListener onItemChildClickListener) {
        this.onItemChildClickListener = onItemChildClickListener;
    }

    public ConnectListAdapter(Context activity, ArrayList<ConnectModel> list, OnItemChildClickListener listener) {
        this.context = activity;
        mList = list;
        mInflater = LayoutInflater.from(activity);
        setOnItemChildClickListener(listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutConnectlistRecycleviewItemBinding itemBinding = DataBindingUtil.inflate(mInflater, R.layout.layout_connectlist_recycleview_item, parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getBinding().setModel(mList.get(position));
        holder.getBinding().setRecyclerViewItemClick(new RecyclerViewItemClickMethod(mList.get(position).index.get(), getOnItemChildClickListener()));
        holder.getBinding().executePendingBindings();
        if(mList.get(position).type.get()!=null){
            if(mList.get(position).type.get().equalsIgnoreCase(ConstantData.namePCR)){
                holder.getBinding().pcrCoverLl.setVisibility(View.VISIBLE);
            }else{
                holder.getBinding().pcrCoverLl.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    /**
     * ViewHolder类
     */

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final LayoutConnectlistRecycleviewItemBinding binding;

        public ViewHolder(LayoutConnectlistRecycleviewItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public LayoutConnectlistRecycleviewItemBinding getBinding() {
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
