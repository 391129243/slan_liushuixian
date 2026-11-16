package com.winters.boweilibrary.autopipeline.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.gidi.bio_pcr.R;
import com.winters.boweilibrary.autopipeline.model.item.QRCodeItem;
import com.winters.boweilibrary.autopipeline.model.item.WSSConnectionItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class QRCodeAdapter extends BaseAdapter {

    private List<QRCodeItem> qRCodeItems;
    private LayoutInflater Inflater;
    private Context mContext;

    public QRCodeAdapter(List<QRCodeItem> datas, Context mContext)
    {
        qRCodeItems = datas;
        Inflater = LayoutInflater.from(mContext);
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return qRCodeItems.size();
    }

    @Override
    public Object getItem(int position) {

        if(qRCodeItems == null || qRCodeItems.size()==0){
            return null;
        }

        if(position > qRCodeItems.size()){
            return null;
        }else{
            return qRCodeItems.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        if(qRCodeItems == null || qRCodeItems.size()==0){
            return -1;
        }

        if(position > qRCodeItems.size()){
            return -1;
        }else{
            return qRCodeItems.get(position).getIndex();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = Inflater.inflate(R.layout.qrcode_item,null);
        TextView index = (TextView) view.findViewById(R.id.index);
        TextView x_coordinate = (TextView) view.findViewById(R.id.x_coordinate);
        TextView y_coordinate = (TextView) view.findViewById(R.id.y_coordinate);
        TextView qrcode = (TextView) view.findViewById(R.id.qrcode);
        index.setText(qRCodeItems.get(position).getIndex() + "");
        x_coordinate.setText(qRCodeItems.get(position).getX_coordinate() + "");
        y_coordinate.setText(qRCodeItems.get(position).getY_coordinate() + "");
        qrcode.setText(qRCodeItems.get(position).getQrcode());
        return view;
    }
}
