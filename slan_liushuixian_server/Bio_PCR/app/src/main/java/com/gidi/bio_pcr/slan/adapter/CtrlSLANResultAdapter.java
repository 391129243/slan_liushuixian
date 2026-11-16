package com.gidi.bio_pcr.slan.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.slan.bean.SLANExlResult;
import com.gidi.bio_pcr.util.StringUtil;
import com.hongshi.slansdk.model.result.MeltPeak;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CtrlSLANResultAdapter extends BaseAdapter {

    private DecimalFormat nf = new DecimalFormat( "0.00");
    private List<SLANExlResult> resultList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;


    public CtrlSLANResultAdapter(Context context){
        this.context = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setResultList(List<SLANExlResult> list){
        this.resultList = list;
        this.notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public Object getItem(int position) {
        return resultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final SLANExlResult exlResult  = resultList.get(position);
        if(null == convertView){
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_result_slan_excel_layout_ctrl,parent,false);
            holder.wellNameTv = (TextView)convertView.findViewById(R.id.item_ex_result_excel_wellname_tv);
            holder.channelTv = (TextView)convertView.findViewById(R.id.item_ex_result_excel_channel_tv);
            holder.controlCtTv = (TextView)convertView.findViewById(R.id.item_ex_result_excel_controlct_tv);
            holder.controlPeakTv = (TextView)convertView.findViewById(R.id.item_result_excel_controlpeak_tv);
            holder.peekHighThdTv = (TextView)convertView.findViewById(R.id.item_ex_result_slan_excel_peek_highThd_tv);
            holder.thresholdTv = (TextView)convertView.findViewById(R.id.item_ex_result_slan_excel_controlrn_tv);
            holder.ctTv = (TextView)convertView.findViewById(R.id.item_ex_result_excel_ct_tv);
            holder.tmValueTv =(TextView)convertView.findViewById(R.id.item_ex_result_excel_peakvalue_tv) ;
            holder.rmValueTv = (TextView)convertView.findViewById(R.id.item_ex_result_excel_rmvalue_tv);
            holder.ct_conclusionTv = (TextView)convertView.findViewById(R.id.item_ex_excel_conclusion_tv);
            holder.peak_conclusionTv = (TextView)convertView.findViewById(R.id.item_ex_excel_conclusion_1_tv);
            holder.regentCodeTv  = (TextView)convertView.findViewById(R.id.item_ex_result_excel_regentCode_tv);
            holder.idCardTv  = (TextView)convertView.findViewById(R.id.item_ex_result_excel_idcard_tv);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder)convertView.getTag();
        }
        if(exlResult.isMelt()){
            StringBuilder peakValueStr = new StringBuilder();
            StringBuilder rmValueStr = new StringBuilder();
            List<MeltPeak> peakValues = exlResult.getPeakValues();
            if(null != peakValues&& peakValues.size()>0){
                if(peakValues.size() == 1){
                    peakValueStr.append(nf.format(peakValues.get(0).getTm()));
                    rmValueStr.append(nf.format(peakValues.get(0).getRm()));
                }else if(peakValues.size()>1){
                    for(int i= 0;i<peakValues.size();i++){
                        if(i == peakValues.size()-1){
                            peakValueStr.append(nf.format(peakValues.get(i).getTm()));
                            rmValueStr.append(nf.format(peakValues.get(i).getRm()));
                        }else{
                            peakValueStr.append(nf.format(peakValues.get(i).getTm())).append("\n");
                            rmValueStr.append(nf.format(peakValues.get(i).getRm())).append("\n");
                        }
                    }
                }
            }
            holder.tmValueTv.setText(peakValueStr.toString());
            holder.rmValueTv.setText(rmValueStr.toString());
        }
        holder.ctTv.setText(nf.format(exlResult.getCt())+"");
        holder.thresholdTv.setText(nf.format(exlResult.getThreshold())+"");//阈值的值
        if(!StringUtil.isEmpty(exlResult.getPeekHighThreshold()+"")){
            holder.peekHighThdTv.setText(nf.format(exlResult.getPeekHighThreshold())+"");
        }else{
            holder.peekHighThdTv.setText("");
        }

        holder.wellNameTv.setText(exlResult.getWellNum());
        holder.channelTv.setText(exlResult.getCurve_hole());


        List<CtResult> results = exlResult.getResults();
        StringBuilder controlCt = new StringBuilder();
        StringBuilder conclusion = new StringBuilder();

        if(null != results && results.size()>0){
            if(results.size()== 1){
                conclusion.append(results.get(0).getCategory()  + ":" + results.get(0).getConclusion());
                controlCt.append(results.get(0).getValue());
            }else if(results.size()>1){
                for(int i=0;i<results.size();i++){
                    if(i == results.size()-1){
                        conclusion.append(results.get(i).getCategory()  + ":" + results.get(i).getConclusion()) ;
                        controlCt.append(results.get(i).getValue());
                    }else{
                        conclusion.append(results.get(i).getCategory()  + ":" + results.get(i).getConclusion()+ "\n") ;
                        controlCt.append(results.get(i).getValue() + "\n");
                    }
                }
            }
        }
        List<PeakResult> peakResults = exlResult.getPeakResults();
        StringBuilder controlpeak = new StringBuilder();
        StringBuilder peak_conclusion = new StringBuilder();
        if(null != peakResults && peakResults.size()>0){
            if(peakResults.size()== 1){
                peak_conclusion.append(peakResults.get(0).getCategory()  + ":" + peakResults.get(0).getConclusion());
                controlpeak.append(peakResults.get(0).getValue());
            }else if(peakResults.size()>1){
                for(int i=0;i<peakResults.size();i++){
                    if(i== peakResults.size()-1){
                        peak_conclusion.append(peakResults.get(i).getCategory()  + ":" + peakResults.get(i).getConclusion()) ;
                        controlpeak.append(peakResults.get(i).getValue());
                    }else {
                        peak_conclusion.append(peakResults.get(i).getCategory()  + ":" + peakResults.get(i).getConclusion()+ "\n") ;
                        controlpeak.append(peakResults.get(i).getValue() + "\n");
                    }
                }
            }
        }
        if(conclusion.toString().contains("阳性")){
            holder.ct_conclusionTv.setTextColor(context.getResources().getColor(R.color.red));
        }else{
            holder.ct_conclusionTv.setTextColor(context.getResources().getColor(R.color.blue));
        }

        if(peak_conclusion.toString().contains("阳性")){
            holder.peak_conclusionTv.setTextColor(context.getResources().getColor(R.color.red));
        }else{
            holder.peak_conclusionTv.setTextColor(context.getResources().getColor(R.color.blue));
        }
        holder.peak_conclusionTv.setText(peak_conclusion.toString());
        holder.ct_conclusionTv.setText(conclusion.toString());
        holder.controlCtTv.setText(controlCt.toString());
        holder.controlPeakTv.setText(controlpeak.toString());
        holder.regentCodeTv.setText(exlResult.getRegentCode());
        holder.idCardTv.setText(exlResult.getIdCard());
        return convertView;
    }

    static class ViewHolder {

        private TextView wellNameTv;
        private TextView channelTv;
        private TextView controlCtTv;
        private TextView controlPeakTv;
        private TextView thresholdTv;
        private TextView peekHighThdTv;
        private TextView ctTv;
        private TextView rmValueTv;
        private TextView tmValueTv;
        private TextView peak_conclusionTv;
        private TextView ct_conclusionTv;
        private TextView regentCodeTv;
        private TextView idCardTv;

    }
}
