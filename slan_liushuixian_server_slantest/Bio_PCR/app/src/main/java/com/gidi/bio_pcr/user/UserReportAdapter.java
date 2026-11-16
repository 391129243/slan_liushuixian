package com.gidi.bio_pcr.user;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.ReportResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户报告
 */
public class UserReportAdapter extends BaseAdapter {
    private List<ReportResult> reportResults = new ArrayList<>();
    private LayoutInflater mInflater;

    public UserReportAdapter(Context context){
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setResultList(List<ReportResult> list){
        this.reportResults = list;
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return reportResults.size();
    }

    @Override
    public Object getItem(int i) {
        return reportResults.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        final ViewHolder mHolder;
        final ReportResult exlResult  = reportResults.get(position);
        if(null == view){
            mHolder = new ViewHolder();
            view = mInflater.inflate(R.layout.item_result_user_report_layout,viewGroup,false);
            mHolder.numTv = (TextView)view.findViewById(R.id.item_user_result_report_num_tv);
            mHolder.itemTv = (TextView)view.findViewById(R.id.item_user_result_report_item_tv);
            mHolder.ctTv = (TextView)view.findViewById(R.id.item_user_result_ct_value_tv);
            mHolder.referenceTv = (TextView)view.findViewById(R.id.item_user_result_reference_tv);
            mHolder.resultsTv = (TextView)view.findViewById(R.id.item_user_result_results_tv);
            view.setTag(mHolder);
        }else{
            mHolder = (ViewHolder)view.getTag();
        }
        mHolder.ctTv.setText(exlResult.getCtValue());
        mHolder.itemTv.setText(exlResult.getCategory());
        mHolder.resultsTv.setText(exlResult.getConclusion());
        mHolder.referenceTv.setText(exlResult.getReference());
        mHolder.numTv.setText(position +"");
        return view;

    }

    static class ViewHolder {

        private TextView numTv;
        private TextView itemTv;
        private TextView ctTv;
        private TextView referenceTv;
        private TextView resultsTv;
    }
}
