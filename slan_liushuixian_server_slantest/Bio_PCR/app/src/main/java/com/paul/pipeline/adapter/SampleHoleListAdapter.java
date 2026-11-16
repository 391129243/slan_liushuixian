package com.paul.pipeline.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.LayoutSampleholelistRecycleviewItemBinding;
import com.paul.pipeline.model.SampleHoleModel;

import java.util.ArrayList;
import java.util.List;

public class SampleHoleListAdapter extends RecyclerView.Adapter<SampleHoleListAdapter.ViewHolder>{
    private List<SampleHoleModel> mList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;
    public SampleHoleListAdapter(Context activity, List<SampleHoleModel> list) {
        this.context = activity;
        this.mList = list;
        this.mInflater = LayoutInflater.from(activity);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutSampleholelistRecycleviewItemBinding itemBinding = DataBindingUtil.inflate(mInflater, R.layout.layout_sampleholelist_recycleview_item, parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getBinding().setData(mList.get(position));
        int status = mList.get(position).status.get();
        if(status==0){
            holder.getBinding().holeRl.setBackground(context.getDrawable(R.drawable.shape_oval_blue_stroke_bg));
        }else{
            holder.getBinding().holeRl.setBackground(context.getDrawable(R.drawable.shape_oval_blue_bg));
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

        private final LayoutSampleholelistRecycleviewItemBinding binding;

        public ViewHolder(LayoutSampleholelistRecycleviewItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public LayoutSampleholelistRecycleviewItemBinding getBinding() {
            return this.binding;
        }
    }

}
