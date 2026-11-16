package com.winters.boweilibrary.autopipeline.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.winters.boweilibrary.autopipeline.model.item.PCRStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.RNAStatusItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

public class WSSConnectionAdapter extends BaseAdapter {

    private ConcurrentHashMap<String,WSSConnectionItem> wssConnectionItems;
    private LayoutInflater Inflater;
    private Context mContext;
    public WSSConnectionAdapter(ConcurrentHashMap<String,WSSConnectionItem> datas, Context mContext)
    {
        wssConnectionItems = datas;
        Inflater =LayoutInflater.from(mContext);
    }
    /*
     * 返回item（列表项）的个数
     * @return
     */
    @Override
    public int getCount()//获取列表中每一项的个数
    {
        return wssConnectionItems.size();
    }
    /*
     * 返回每一个item（列表项）
     * @param i
     * @return
     */
    @Override
    public Object getItem(int i)//根据索引获取列表中每一项的数据
    {

        // 将 LinkedHashMap 转换为列表
        int index1 = 0;
        String text = "";
        for (String key : wssConnectionItems.keySet()) {
            if(index1 == i){
                text = key;
                break;
            }else{
                index1++;
            }
        }

        WSSConnectionItem item = wssConnectionItems.get(text);
        return item;
    }
    /*
     * 返回每一个item（列表项）的id
     * @param i
     * @return
     */
    @Override
    public long getItemId(int i)//获取列表中每一项的id
    {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent)//获取列表中每一项的显示内容
    {
        View view = Inflater.inflate(R.layout.wssconnection_item,null);
        TextView index = (TextView) view.findViewById(R.id.index);
        TextView ip_port = (TextView) view.findViewById(R.id.ip_port);
        TextView device_type = (TextView) view.findViewById(R.id.device_type);
        TextView position = (TextView) view.findViewById(R.id.position);
        TextView deviceid = (TextView) view.findViewById(R.id.deviceid);


        TextView status = (TextView) view.findViewById(R.id.status);
        TextView degree = (TextView) view.findViewById(R.id.degree);
        TextView no = (TextView) view.findViewById(R.id.no);
        TextView alarm = (TextView) view.findViewById(R.id.alarm);
        TextView template = (TextView) view.findViewById(R.id.template);

        TextView sendalarm = (TextView) view.findViewById(R.id.sendalarm);
        TextView currenttray = (TextView) view.findViewById(R.id.currenttray);
        TextView runninginfo = (TextView) view.findViewById(R.id.runninginfo);
        TextView info = (TextView) view.findViewById(R.id.info);
        TextView lefttime = (TextView) view.findViewById(R.id.lefttime);
        TextView totaltime = (TextView) view.findViewById(R.id.totaltime);


        TextView currentsection = (TextView) view.findViewById(R.id.currentsection);
        TextView currentstep = (TextView) view.findViewById(R.id.currentstep);
        TextView currentcycle = (TextView) view.findViewById(R.id.currentcycle);
        TextView totalcycle = (TextView) view.findViewById(R.id.totalcycle);
        TextView runningtime = (TextView) view.findViewById(R.id.runningtime);
        TextView waittime = (TextView) view.findViewById(R.id.waittime);

        TextView linuxstatus = (TextView) view.findViewById(R.id.linuxstatus);
        TextView linuxalarm = (TextView) view.findViewById(R.id.linuxalarm);

        TextView degreetarget = (TextView) view.findViewById(R.id.degreetarget);


        TextView version = (TextView) view.findViewById(R.id.version);


        // 将 LinkedHashMap 转换为列表
        int index1 = 0;
        String text = "";
        for (String key : wssConnectionItems.keySet()) {
           if(index1 == i){
               text = key;
               break;
           }else{
               index1++;
           }
        }

        WSSConnectionItem item = wssConnectionItems.get(text);

        if(item == null){
            Log.e("WSSConnectionAdapter", "null item");
            return view;
        }
        index.setText((i + 1) + "");

        ip_port.setText(item.getDeviceIP() + "");

        device_type.setText(item.getDeviceType() + "");

        position.setText(item.getPosition() + "");

        deviceid.setText(item.getDeviceID() + "");

        version.setText(item.getVersion());

        if(item instanceof RNAStatusItem){
            RNAStatusItem rNAStatusItem = (RNAStatusItem) item;
            status.setText(rNAStatusItem.getrNAStatus() + "");
            degree.setText(rNAStatusItem.getrNADegree() + "");
            no.setText(rNAStatusItem.getNO() + "");
            alarm.setText(rNAStatusItem.getrNAAlarm() + "");
            template.setText(rNAStatusItem.getrNACurrentTemplate() + "");

            sendalarm.setText(rNAStatusItem.getrNASendAlarm() + "");
            currenttray.setText(rNAStatusItem.getrNACurrentTray() + "");
            runninginfo.setText(rNAStatusItem.getrNARunninginfo() + "");
            info.setText(rNAStatusItem.getInfo() + "");

            lefttime.setText(rNAStatusItem.getrNALeftTime() + "");
            totaltime.setText(rNAStatusItem.getrNATotalTime() + "");

            linuxstatus.setText(rNAStatusItem.getrNALinuxStatus() + "");
            linuxalarm.setText(rNAStatusItem.getrNALinuxAlarm() + "");

        }

        if(item instanceof PCRStatusItem){
            PCRStatusItem pCRStatusItem = (PCRStatusItem) item;
            status.setText(pCRStatusItem.getpCRStatus() + "");
            degree.setText(pCRStatusItem.getpCRDegree() + "");
            alarm.setText(pCRStatusItem.getpCRAlarm() + "");
            template.setText(pCRStatusItem.getpCRCurrentTemplate() + "");

            sendalarm.setText(pCRStatusItem.getpCRSendAlarm() + "");
            currenttray.setText(pCRStatusItem.getpCRCurrentTray() + "");

            info.setText(pCRStatusItem.getInfo() + "");

            lefttime.setText(pCRStatusItem.getpCRLeftTime() + "");
            totaltime.setText(pCRStatusItem.getpCRTotalTime() + "");


            currentsection.setText(pCRStatusItem.getpCRCurrentSection() + "");
            currentstep.setText(pCRStatusItem.getpCRCurrentStep() + "");
            currentcycle.setText(pCRStatusItem.getpCRCurrentCycle() + "");
            totalcycle.setText(pCRStatusItem.getpCRTotalCycle() + "");
            runningtime.setText(pCRStatusItem.getpCRRunningTime() + "");
            waittime.setText(pCRStatusItem.getpCRWaitTime() + "");

            linuxstatus.setText(pCRStatusItem.getpCRLinuxStatus() + "");
            linuxalarm.setText(pCRStatusItem.getpCRLinuxAlarm() + "");

            degreetarget.setText(pCRStatusItem.getDegreeTarget() + "");

        }

        return view;
    }
}

