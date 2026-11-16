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
import com.gidi.bio_pcr.databinding.LayoutDetailSampleholelistRecycleviewItemBinding;
import com.paul.pipeline.listener.OnItemChildClickListener;
import com.paul.pipeline.model.SampleHoleModel;

import java.util.ArrayList;
import java.util.List;

public class DetailSampleHoleListAdapter extends RecyclerView.Adapter<DetailSampleHoleListAdapter.ViewHolder>{
    private List<SampleHoleModel> mList = new ArrayList<>();
    private final LayoutInflater mInflater;
    Context context;
    OnItemChildClickListener onItemChildClickListener;

    public OnItemChildClickListener getOnItemChildClickListener() {
        return onItemChildClickListener;
    }

    public void setOnItemChildClickListener(OnItemChildClickListener onItemChildClickListener) {
        this.onItemChildClickListener = onItemChildClickListener;
    }
    public DetailSampleHoleListAdapter(Context activity, List<SampleHoleModel> list,OnItemChildClickListener listener) {
        this.context = activity;
        mList = list;
        mInflater = LayoutInflater.from(activity);
        setOnItemChildClickListener(listener);
    }
    boolean circleEnable = true;
    public void setCircleVisible(boolean value){
        circleEnable = value;
        notifyDataSetChanged();
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutDetailSampleholelistRecycleviewItemBinding itemBinding = DataBindingUtil.inflate(mInflater,
                R.layout.layout_detail_sampleholelist_recycleview_item, parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getBinding().setData(mList.get(position));
        holder.getBinding().setRecyclerViewItemClick(new RecyclerViewItemClickMethod(mList.get(position).index.get(), getOnItemChildClickListener()));
        holder.getBinding().mainbgLl.setSelected(mList.get(position).selected.get());
         if(circleEnable){
            holder.getBinding().sampleholeCb.setVisibility(View.VISIBLE);
             if(mList.get(position).selected.get()){
                 holder.getBinding().sampleholeCb.setImageDrawable(context.getDrawable( R.drawable.item_file_check));
             }else{
                 holder.getBinding().sampleholeCb.setImageDrawable(context.getDrawable( R.drawable.item_file_uncheck));
             }
        }else{
            holder.getBinding().sampleholeCb.setVisibility(View.INVISIBLE);
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

        private final LayoutDetailSampleholelistRecycleviewItemBinding binding;

        public ViewHolder(LayoutDetailSampleholelistRecycleviewItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public LayoutDetailSampleholelistRecycleviewItemBinding getBinding() {
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
            Log.i("RecycleView", "detailtrayViewClick----:" + position);
            if(onItemChildClickListener!=null) {
                onItemChildClickListener.onItemChildClick(view, position);
            }
        }
    }
}
