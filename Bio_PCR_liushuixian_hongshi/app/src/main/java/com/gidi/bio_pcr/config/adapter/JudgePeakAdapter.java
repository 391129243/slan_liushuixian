package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKRuleEntity;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.PEAKSingleRuleEntity;

import java.util.ArrayList;
import java.util.List;

import cn.hutool.poi.exceptions.POIException;

/**
 * @author xxs :多重判别文件的Adapter
 */
public class JudgePeakAdapter extends RecyclerView.Adapter {
    private List<PEAKRuleEntity> groups;
    private Context context;
    private int selectedPosition = -1;

    public JudgePeakAdapter(Context context){
        this.context = context;
        groups = new ArrayList<PEAKRuleEntity>();
    }

    public void setJudgePeakList(List<PEAKRuleEntity> groups) {
        this.groups = groups;
        this.notifyDataSetChanged();
    }

    public void setSelectedPosition(int selectedPosition){
        this.selectedPosition = selectedPosition;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        int count = 0;
        if(groups != null){
            for (PEAKRuleEntity group : groups) {
                count += group.getPEAKSingleRuleEntityList().size() + 1;//
            }
        }

        return count;
    }

    @Override
    public int getItemViewType(int position) {
        int count = 0;
        for (PEAKRuleEntity group : groups) {
            int size = group.getPEAKSingleRuleEntityList().size() + 1;
            if (position < count + size) {
                return position == count ? 0 : 1;
            }
            count += size;
        }
        throw new IllegalArgumentException("Invalid position");
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_multi_header, parent, false);
            return new GroupViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_multi_peak, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int count = 0;
        for (PEAKRuleEntity group : groups) {
            int size = group.getPEAKSingleRuleEntityList().size() + 1;
            if (position < count + size) {
                if (position == count) {
                    GroupViewHolder groupViewHolder = (GroupViewHolder) holder;
                    groupViewHolder.channelType.setText(group.getChannel());

                    if (selectedPosition == position) {
                        groupViewHolder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.color_item_pressed_bg));
                    } else {
                        groupViewHolder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.white));
                    }
                } else {

                    ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
                    List<PEAKSingleRuleEntity> peakSingleRuleEntities = group.getPEAKSingleRuleEntityList();
                    PEAKSingleRuleEntity peakSingleRule = peakSingleRuleEntities.get(position - count - 1);
//                    itemViewHolder.indexTv.setText((position - count - 1)+"");
                    itemViewHolder.indexTv.setText((position - count)+"");
                    itemViewHolder.categoryTv.setText(peakSingleRule.getCategory());
                    itemViewHolder.peakValueTv.setText(peakSingleRule.getPEAKvalue()+"");
                    itemViewHolder.peakUpTv.setText(peakSingleRule.getPEAKvalueUP()+"");
                    itemViewHolder.peakDownTv.setText(peakSingleRule.getPEAKvalueDown()+"");
                    itemViewHolder.peakValueRMTv.setText(peakSingleRule.getPEAKvalueRM()+"");
                    if (selectedPosition == position) {
                        itemViewHolder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.color_recycle));
                    } else {
                        itemViewHolder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.white));
                    }
                }
                return;
            }
            count += size;
        }
        throw new IllegalArgumentException("Invalid position");
    }

    private class GroupViewHolder extends RecyclerView.ViewHolder {
        private TextView channelType;

        private LinearLayout rootLayout;
        public GroupViewHolder(View itemView) {
            super(itemView);
            channelType = itemView.findViewById(R.id.item_multi_header_channel_title);
            rootLayout = itemView.findViewById(R.id.item_multi_header_root_layout);

            channelType.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();//当前点击的位置
                    int header_pos = getHeaderPosition(position);
                    selectedPosition = position;
                    notifyDataSetChanged();
                    if(null != onItemClickListener){
                        onItemClickListener.onHeaderChannel(header_pos);
                    }
                }
            });

            rootLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();//当前点击的位置

                    int header_pos = getHeaderPosition(position);
                    if (position != selectedPosition) {
                        selectedPosition = position;
                        notifyDataSetChanged();
                        if(null != onItemClickListener){
                            onItemClickListener.onHeaderClick(header_pos);
                        }
                    }

                }
            });
        }
    }

    private int getHeaderPosition(int position){
        int count = 0;
        for(int i=0;i<groups.size();i++){
            int size = groups.get(i).getPEAKSingleRuleEntityList().size()+1;
            if(position == count){
                return i;
            }
            count+= size;
        }
        return 0;
    }

    private int getItemInPosition(int position){
        int count = 0;
        for(int i=0;i<groups.size();i++){
            int size = groups.get(i).getPEAKSingleRuleEntityList().size()+1;
            count+= size;
            if(position < count){
                return i;
            }
        }
        return 0;
    }

    /**
     *
     * @param position 点击显示的pos
     * @param header_pos
     * @return
     */
    private int getCurItemPosition(int position,int header_pos){
        int count =0;
        for(int i=0;i<header_pos;i++){
            int size = groups.get(i).getPEAKSingleRuleEntityList().size()+1;
            count += size;
        }
        //每个子列表从0开始算
        return (position-count)-1;
    }

    private class ItemViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootLayout;
        TextView indexTv;
        TextView categoryTv;//种类
        TextView peakValueTv;
        TextView peakUpTv;
        TextView peakDownTv;
        TextView peakValueRMTv;

        public ItemViewHolder(View itemView) {
            super(itemView);
            indexTv = (TextView) itemView.findViewById(R.id.item_multi_index_tv);
            categoryTv = (TextView) itemView.findViewById(R.id.item_multi_category_tv);//种类
            peakValueTv = (TextView) itemView.findViewById(R.id.item_multi_PEAKvalue_tv);
            peakUpTv = (TextView) itemView.findViewById(R.id.item_multi_PEAKvalueUP_tv);
            peakDownTv = (TextView) itemView.findViewById(R.id.item_multi_PEAKvalueDown_tv);
            peakValueRMTv = (TextView) itemView.findViewById(R.id.item_multi_PEAKvalueRM_tv);
            rootLayout = (LinearLayout)itemView.findViewById(R.id.item_multi_item_root_layout);

            indexTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();//当前点击的位置
                    selectedPosition = position;
                    notifyDataSetChanged();
                    int cur_header_pos = getItemInPosition(position);//获取当前的上一个position
                    int cur_item_position = getCurItemPosition(position,cur_header_pos);
                    Log.i("JudgePeakAdapter","cur_header_pos"+ cur_header_pos);
                    Log.i("JudgePeakAdapter","cur_item_position"+ cur_item_position);
                    if(null != onItemClickListener){
                        onItemClickListener.onItemClick(cur_header_pos,cur_item_position);
                    }

                }
            });

            categoryTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();//当前点击的位置
                    selectedPosition = position;
                    notifyDataSetChanged();

                    int cur_header_pos = getItemInPosition(position);//获取当前的上一个position
                    int cur_item_position = getCurItemPosition(position,cur_header_pos);
                    if(null != onItemClickListener){
                        onItemClickListener.onItemChangeCategory(view,cur_header_pos,cur_item_position);
                    }
                }
            });

            peakValueTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();//当前点击的位置
                    selectedPosition = position;
                    notifyDataSetChanged();

                    int cur_header_pos = getItemInPosition(position);//获取当前的上一个position
                    int cur_item_position = getCurItemPosition(position,cur_header_pos);
                    if(null != onItemClickListener){
                        onItemClickListener.onItemChangeNum(cur_header_pos,cur_item_position);
                    }

                }
            });

            peakUpTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();//当前点击的位置
                    selectedPosition = position;
                    notifyDataSetChanged();

                    int cur_header_pos = getItemInPosition(position);//获取当前的上一个position
                    int cur_item_position = getCurItemPosition(position,cur_header_pos);
                    if(null != onItemClickListener){
                        onItemClickListener.onItemPeakUp(cur_header_pos,cur_item_position);
                    }

                }
            });

            peakDownTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();//当前点击的位置
                    selectedPosition = position;
                    notifyDataSetChanged();

                    int cur_header_pos = getItemInPosition(position);//获取当前的上一个position
                    int cur_item_position = getCurItemPosition(position,cur_header_pos);
                    if(null != onItemClickListener){
                        onItemClickListener.onItemPeakDown(cur_header_pos,cur_item_position);
                    }

                }
            });

            peakValueRMTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();//当前点击的位置

                    selectedPosition = position;
                    notifyDataSetChanged();
                    int cur_header_pos = getItemInPosition(position);//获取当前的上一个position
                    int cur_item_position = getCurItemPosition(position,cur_header_pos);
                    if(null != onItemClickListener){
                        onItemClickListener.onItemPeakValueRM(cur_header_pos,cur_item_position);
                    }
                }
            });
        }
    }

    public interface OnItemClickListener{
        void onHeaderChannel(int position);
        void onHeaderClick(int position);
        void onItemClick(int header_position ,int item_position);

        void onItemChangeCategory(View view,int header_position ,int item_position);

        void onItemChangeNum(int header_position ,int item_position);

        void onItemPeakUp(int header_position ,int item_position);
        void onItemPeakDown(int header_position ,int item_position);
        void onItemPeakValueRM(int header_position ,int item_position);

    }

    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }

}
