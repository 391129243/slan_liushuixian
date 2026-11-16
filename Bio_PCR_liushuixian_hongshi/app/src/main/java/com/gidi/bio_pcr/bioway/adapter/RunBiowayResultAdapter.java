package com.gidi.bio_pcr.bioway.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.CtResult;
import com.gidi.bio_pcr.bean.PeakResult;
import com.gidi.bio_pcr.bean.Result;
import com.winters.boweilibrary.BoWei.ExperimentData.Peak;
import com.winters.boweilibrary.BoWei.bean.BiowayExlResult;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class RunBiowayResultAdapter extends BaseAdapter {

    private DecimalFormat nf = new DecimalFormat( "0.00");
    private List<BiowayExlResult> resultList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context context;

    public RunBiowayResultAdapter(Context context){
        this.context = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setResultList(List<BiowayExlResult> list){
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
        final ViewHolder mHolder;
        final BiowayExlResult exlResult  = resultList.get(position);
        if(null == convertView){
            mHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_run_result_slan_excel_layout,parent,false);
            mHolder.wellNameTv = (TextView)convertView.findViewById(R.id.item_result_slan_excel_wellname_tv);
            mHolder.idCardTv = (TextView)convertView.findViewById(R.id.item_result_slan_excel_idcard_tv);
            mHolder.channelTv = (TextView)convertView.findViewById(R.id.item_result_slan_excel_channel_tv);
            mHolder.controlCtTv = (TextView)convertView.findViewById(R.id.item_result_excel_slan_controlct_tv);
            mHolder.controlPeakTv = (TextView)convertView.findViewById(R.id.item_result_excel_slan_controlpeak_tv);
            mHolder.thresholdTv = (TextView)convertView.findViewById(R.id.item_result_excel_slan_threshold_tv);
            mHolder.peekHighThresholdTv = (TextView)convertView.findViewById(R.id.item_result_excel_slan_peek_high_threshold_tv);
            mHolder.ctTv = (TextView)convertView.findViewById(R.id.item_result_run_slan_excel_ct_tv);
            mHolder.tmValueTv =(TextView)convertView.findViewById(R.id.item_result_run_excel_slan_tmvalue_tv) ;
            mHolder.rmValueTv=(TextView)convertView.findViewById(R.id.item_result_run_slan_excel_rmvalue_tv) ;
            mHolder.ct_conclusionTv = (TextView)convertView.findViewById(R.id.item_result_excel_slan_conclusion_tv);
            mHolder.peak_conclusionTv = (TextView)convertView.findViewById(R.id.item_result_excel_slan_peak_conclusion_tv);
            convertView.setTag(mHolder);
        }else{
            mHolder = (ViewHolder)convertView.getTag();
        }

        StringBuilder peakValueStr = new StringBuilder();
        StringBuilder rmValueStr = new StringBuilder();
        List<Peak> peakValues = exlResult.getPeakValues();
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
        mHolder.tmValueTv.setText(peakValueStr.toString());
        mHolder.rmValueTv.setText(rmValueStr.toString());
        mHolder.ctTv.setText(nf.format(exlResult.getCt())+"");
        mHolder.thresholdTv.setText(nf.format(exlResult.getThreshold())+"");//阈值的值
        mHolder.peekHighThresholdTv.setText(nf.format(exlResult.getPeekHighThreshold())+"");
        if(exlResult.getWellNum().equals("A1")){
            mHolder.wellNameTv.setText(context.getResources().getString(R.string.hole_eight));
        }else if(exlResult.getWellNum().equals("A2")){
            mHolder.wellNameTv.setText(context.getResources().getString(R.string.hole_seven));
        }else if(exlResult.getWellNum().equals("A3")){
            mHolder.wellNameTv.setText(context.getResources().getString(R.string.hole_six));
        }else if(exlResult.getWellNum().equals("A4")){
            mHolder.wellNameTv.setText(context.getResources().getString(R.string.hole_five));
        }else if(exlResult.getWellNum().equals("A5")){
            mHolder.wellNameTv.setText(context.getResources().getString(R.string.hole_four));
        }else if(exlResult.getWellNum().equals("A6")){
            mHolder.wellNameTv.setText(context.getResources().getString(R.string.hole_three));
        }else if(exlResult.getWellNum().equals("A7")){
            mHolder.wellNameTv.setText(context.getResources().getString(R.string.hole_two));
        }else if(exlResult.getWellNum().equals("A8")){
            mHolder.wellNameTv.setText(context.getResources().getString(R.string.hole_one));
        }
        mHolder.idCardTv.setText(exlResult.getIdCard());
        mHolder.channelTv.setText(exlResult.getCurve_hole());

        List<CtResult> results = exlResult.getResults();
        StringBuilder controlCt = new StringBuilder();
        StringBuilder conclusion = new StringBuilder();

        if(null != results){
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
        if(conclusion.toString().contains("阳性")){
            mHolder.ct_conclusionTv.setTextColor(context.getResources().getColor(R.color.red));
        }else{
            mHolder.ct_conclusionTv.setTextColor(context.getResources().getColor(R.color.blue));
        }

        mHolder.ct_conclusionTv.setText(conclusion.toString());
        mHolder.controlCtTv.setText(controlCt.toString());

        List<PeakResult> peakresults = exlResult.getPeakResults();
        StringBuilder controlPeak= new StringBuilder();
        StringBuilder peakconclusion = new StringBuilder();

        if(null != peakresults){
            if(peakresults.size()== 1){
                peakconclusion.append(peakresults.get(0).getCategory()  + ":" + peakresults.get(0).getConclusion());
                controlPeak.append(peakresults.get(0).getValue());
            }else if(peakresults.size()>1){
                for(int i=0;i<peakresults.size();i++){
                    if(i== peakresults.size()-1){
                        peakconclusion.append(peakresults.get(i).getCategory()  + ":" + peakresults.get(i).getConclusion()) ;
                        controlPeak.append(peakresults.get(i).getValue());
                    }else {
                        peakconclusion.append(peakresults.get(i).getCategory()  + ":" + peakresults.get(i).getConclusion()+ "\n") ;
                        controlPeak.append(peakresults.get(i).getValue() + "\n");
                    }
                }
            }
        }
        mHolder.peak_conclusionTv.setText(peakconclusion.toString());
        mHolder.controlPeakTv.setText(controlPeak.toString());
        return convertView;
    }

    static class ViewHolder {

        private TextView wellNameTv;
        private TextView idCardTv;
        private TextView channelTv;
        private TextView controlPeakTv;
        private TextView controlCtTv;
        private TextView thresholdTv;
        private TextView peekHighThresholdTv;
        private TextView ctTv;
        private TextView rmValueTv;//实测峰高
        private TextView tmValueTv;//实测TM
        private TextView ct_conclusionTv;
        private TextView peak_conclusionTv;


    }
}
