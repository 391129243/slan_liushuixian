package com.paul.pipeline.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.databinding.LayoutDevicelistRecycleviewItemBinding;
import com.paul.pipeline.data.ConstantData;
import com.paul.pipeline.listener.OnItemChildClickListener;
import com.paul.pipeline.model.LinuxDeviceModel;

import java.util.ArrayList;

public class DevicesListAdapter extends RecyclerView.Adapter<DevicesListAdapter.ViewHolder>{
    private ArrayList<LinuxDeviceModel> mList = new ArrayList<>();
    private final LayoutInflater mInflater;
    Context context;
    OnDeviceItemChildClickListener onItemChildClickListener;

    public OnDeviceItemChildClickListener getOnItemChildClickListener() {
        return onItemChildClickListener;
    }

    public void setOnItemChildClickListener(OnDeviceItemChildClickListener onItemChildClickListener) {
        this.onItemChildClickListener = onItemChildClickListener;
    }

    public DevicesListAdapter(Context activity, ArrayList<LinuxDeviceModel> list, OnDeviceItemChildClickListener listener) {
        this.context = activity;
        mList = list;
        mInflater = LayoutInflater.from(activity);
        setOnItemChildClickListener(listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutDevicelistRecycleviewItemBinding itemBinding = DataBindingUtil.inflate(mInflater, R.layout.layout_devicelist_recycleview_item, parent, false);
        return new ViewHolder(itemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.getBinding().setLdm(mList.get(position));
        RecyclerViewItemClickMethod recyclerViewItemClickMethod = new RecyclerViewItemClickMethod(mList.get(position), getOnItemChildClickListener());
        holder.getBinding().setRecyclerViewItemClick(recyclerViewItemClickMethod);
        holder.getBinding().selectedCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mList.get(position).selected.setValue(isChecked);
                if(!getOnItemChildClickListener().onItemChildClick(buttonView,mList.get(position))){
                    holder.getBinding().selectedCb.setChecked(!isChecked);
                }
            }
        });
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

        private final LayoutDevicelistRecycleviewItemBinding binding;

        public ViewHolder(LayoutDevicelistRecycleviewItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public LayoutDevicelistRecycleviewItemBinding getBinding() {
            return this.binding;
        }
    }

    /**
     * 点击事件
     */

    public static class RecyclerViewItemClickMethod {

        private final LinuxDeviceModel position;
        OnDeviceItemChildClickListener onItemChildClickListener;
        public RecyclerViewItemClickMethod(LinuxDeviceModel position, OnDeviceItemChildClickListener onItemChildClickListener) {
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
    public interface OnDeviceItemChildClickListener {
        /**
         * 子组件点击事件
         * @param view
         * @param value
         */
        boolean onItemChildClick(View view, LinuxDeviceModel value);
    }
}
