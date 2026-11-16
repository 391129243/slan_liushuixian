package com.paul.pipeline.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.paul.pipeline.model.SampleHoleModel;

import java.util.ArrayList;
import java.util.List;

public class SampleHoleListAdapter extends RecyclerView.Adapter<SampleHoleListAdapter.ViewHolder> {

    private final List<SampleHoleModel> mList = new ArrayList<>();
    private final LayoutInflater mInflater;
    // 预缓存 Drawable,避免每次 bind 都重新解析资源
    private final Drawable mBgStroke;
    private final Drawable mBgSolid;

    public SampleHoleListAdapter(Context activity, List<SampleHoleModel> list) {
        this.mInflater = LayoutInflater.from(activity);
        if (list != null) {
            this.mList.addAll(list);
        }
        this.mBgStroke = ContextCompat.getDrawable(activity, R.drawable.shape_oval_blue_stroke_bg);
        this.mBgSolid = ContextCompat.getDrawable(activity, R.drawable.shape_oval_blue_bg);
        // 启用稳定 ID,需要重写 getItemId (本类已实现),让 notifyItemChanged 只刷新目标 item
        setHasStableIds(true);
    }

    public void updateData(List<SampleHoleModel> list) {
        mList.clear();
        if (list != null) {
            mList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 直接 inflate,不再走 DataBindingUtil,显著降低单次 inflate 开销
        View itemView = mInflater.inflate(R.layout.layout_sampleholelist_recycleview_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SampleHoleModel data = mList.get(position);
        holder.holeNameTv.setText(data.name.get());
        holder.itemView.setBackground(data.status.get() == 0 ? mBgStroke : mBgSolid);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public long getItemId(int position) {
        // 用 index 作为稳定 ID,确保 notifyItemChanged 只更新目标 item
        Integer idx = mList.get(position).index.get();
        return idx == null ? position : idx;
    }

    /**
     * ViewHolder — 直接持有 View 引用,避免 DataBinding 反射开销
     */
    static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView holeNameTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // itemView 自身就是 ConstraintLayout (id=hole_rl),可直接使用
            this.holeNameTv = itemView.findViewById(R.id.hole_name_tv);
        }
    }
}
