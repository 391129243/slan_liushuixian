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
import com.paul.pipeline.model.PairHoleModel;
import com.paul.pipeline.model.SampleHoleModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 一行横排显示 8 对 (A+B) 孔位,顺序由传入列表决定
 * Adapter 内部不维护顺序,顺序由 Fragment 端组装时控制 (如倒序:A8/B8 -> A1/B1)
 */
public class PairHoleListAdapter extends RecyclerView.Adapter<PairHoleListAdapter.ViewHolder> {

    private final List<PairHoleModel> mList = new ArrayList<>();
    private final LayoutInflater mInflater;
    // 预缓存 Drawable,避免每次 bind 都重新解析资源
    private final Drawable mBgStroke;
    private final Drawable mBgSolid;

    public PairHoleListAdapter(Context context, List<PairHoleModel> list) {
        this.mInflater = LayoutInflater.from(context);
        if (list != null) {
            this.mList.addAll(list);
        }
        this.mBgStroke = ContextCompat.getDrawable(context, R.drawable.shape_oval_blue_stroke_bg);
        this.mBgSolid = ContextCompat.getDrawable(context, R.drawable.shape_oval_blue_bg);
        // 启用稳定 ID,确保 notifyItemChanged 只刷新目标 item
        setHasStableIds(true);
    }

    public void updateData(List<PairHoleModel> list) {
        mList.clear();
        if (list != null) {
            mList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.layout_pair_samplehole_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PairHoleModel pair = mList.get(position);
        // A 孔
        if (pair.aHole != null) {
            holder.aNameTv.setText(pair.aHole.name.get());
            holder.aNameTv.setBackground(pair.aHole.status.get() != null && pair.aHole.status.get() == 0
                    ? mBgStroke : mBgSolid);
        }
        // B 孔
        if (pair.bHole != null) {
            holder.bNameTv.setText(pair.bHole.name.get());
            holder.bNameTv.setBackground(pair.bHole.status.get() != null && pair.bHole.status.get() == 0
                    ? mBgStroke : mBgSolid);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public long getItemId(int position) {
        return mList.get(position).stableId();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView aNameTv;
        final TextView bNameTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.aNameTv = itemView.findViewById(R.id.hole_a_name_tv);
            this.bNameTv = itemView.findViewById(R.id.hole_b_name_tv);
        }
    }
}
