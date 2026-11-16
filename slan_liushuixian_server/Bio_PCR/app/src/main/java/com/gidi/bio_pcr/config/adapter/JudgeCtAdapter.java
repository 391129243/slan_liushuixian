package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.WintersUtils.entity.judgeEntity.CTRuleEntity;

import java.util.ArrayList;
import java.util.List;

public class JudgeCtAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<CTRuleEntity> mCtRuleList;
    private int selectedPosition = -1;
    private Context context;

    public JudgeCtAdapter(Context context){
        this.context = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mCtRuleList = new ArrayList<CTRuleEntity>();
    }

    public void setJudgeCtList(List<CTRuleEntity> list){
        mCtRuleList = list;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mCtRuleList.size();
    }

    @Override
    public Object getItem(int i) {
        return mCtRuleList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ViewHolder holder;
        if(null != view){
            holder = (ViewHolder) view.getTag();
        }else{
            holder = new ViewHolder();
            view = mInflater.inflate(R.layout.item_list_single_ct,viewGroup,false);
            holder.rootLayout = (LinearLayout)view.findViewById(R.id.item_ct_judge_rootlayout);
            holder.checkImg = (ImageView)view.findViewById(R.id.single_create_ck);
            holder.index = (TextView)view.findViewById(R.id.item_single_create_index_tv);
            holder.channel =  (TextView)view.findViewById(R.id.item_single_create_channel_tv);
            holder.type = (TextView)view.findViewById(R.id.item_single_create_type_tv);

            holder.ctValueTv =  (TextView)view.findViewById(R.id.item_single_create_ct_tv);
            holder.ctValueDownTv = (TextView)view.findViewById(R.id.config_judge_single_fam_lower_tv);
            holder.ctValueUpTv = (TextView)view.findViewById(R.id.config_judge_single_ct_upper_tv);
            holder.ctSuspiciousTv = (TextView)view.findViewById(R.id.config_judge_single_ct_suspicious_tv);
            view.setTag(holder);
        }
        CTRuleEntity ctRuleEntity = mCtRuleList.get(position);
//        holder.index.setText(position+"");
        holder.index.setText(position+ 1 + "");
        holder.channel.setText(ctRuleEntity.getChannel());
        holder.type.setText(ctRuleEntity.getCategory());
        holder.ctValueTv.setText(ctRuleEntity.getCTvalue()+"");
        holder.ctValueDownTv.setText(ctRuleEntity.getCTvalueDown()+"");
        holder.ctValueUpTv.setText(ctRuleEntity.getCTvalueUP()+"");
        holder.ctSuspiciousTv.setText(ctRuleEntity.getCTvalueUP_Suspicious()+"");

        if (selectedPosition == position) {
            holder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.color_recycle));
        } else {
            holder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.white));
        }

        holder.index.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if(null != onItemClickListener){
                    onItemClickListener.onItemClick(position);
                }
            }
        });

        holder.checkImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if(null != onItemClickListener){
                    onItemClickListener.onItemClick(position);
                }
            }
        });


        //种类
        holder.type.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if(null != onItemClickListener){
                    onItemClickListener.onItemChangeCategory(position);
                }
            }
        });

        holder.channel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if (null != onItemClickListener){
                    onItemClickListener.onItemChangeChannel(view,position);
                }
            }
        });

        holder.ctValueTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if (null != onItemClickListener){
                    onItemClickListener.onItemChangeNum(position);
                }
            }
        });

        holder.ctValueUpTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if (null != onItemClickListener){
                    onItemClickListener.onItemCtUp(position);
                }
            }
        });

        holder.ctValueDownTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if (null != onItemClickListener){
                    onItemClickListener.onItemCtDown(position);
                }
            }
        });

        holder.ctSuspiciousTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
                if (null != onItemClickListener){
                    onItemClickListener.onItemCtSuspicious(position);
                }
            }
        });
        return view;
    }

    class ViewHolder{
        LinearLayout rootLayout;
        ImageView checkImg;
        TextView index;//序号
        TextView channel;//通道

        TextView type;//种类
        TextView ctValueTv;//ct值
        TextView ctValueDownTv;//下限值
        TextView ctValueUpTv;//上限值
        TextView ctSuspiciousTv;//
    }

    public interface OnItemClickListener{

        void onItemClick(int item_position);
        void onItemChangeChannel(View view,int item_position);

        void onItemChangeCategory(int item_position);

        void onItemChangeNum(int item_position);

        void onItemCtUp(int item_position);
        void onItemCtDown(int item_position);
        void onItemCtSuspicious(int item_position);

    }

    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }
}
