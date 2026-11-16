package com.winters.boweilibrary.autopipeline.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.winters.boweilibrary.autopipeline.model.item.ItemProperty;

import java.util.List;

public class ItemPropertyAdapter extends BaseAdapter {

    private List<ItemProperty> itemPropertyList;
    private LayoutInflater Inflater;
    private Context mContext;

    public ItemPropertyAdapter( List<ItemProperty> itemPropertyList, Context mContext)
    {
        this.itemPropertyList = itemPropertyList;
        Inflater = LayoutInflater.from(mContext);
        this.mContext = mContext;
    }

    @Override
    public int getCount() {
        return itemPropertyList.size();
    }

    @Override
    public Object getItem(int position) {
        if(itemPropertyList == null || itemPropertyList.size()==0){
            return null;
        }

        if(position > itemPropertyList.size()){
            return null;
        }else{
            return itemPropertyList.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = Inflater.inflate(R.layout.itemproperty_item,null);
        TextView type_text = (TextView) view.findViewById(R.id.type);
        TextView position_text = (TextView) view.findViewById(R.id.position);
        TextView x_coordinate_text = (TextView) view.findViewById(R.id.x_coordinate);
        TextView y_coordinate_text = (TextView) view.findViewById(R.id.y_coordinate);
        TextView z_coordinate_text = (TextView) view.findViewById(R.id.z_coordinate);

        type_text.setText(itemPropertyList.get(position).getType() + "");
        position_text.setText(itemPropertyList.get(position).getPosition() + "");
        x_coordinate_text.setText(itemPropertyList.get(position).getX() + "");
        y_coordinate_text.setText(itemPropertyList.get(position).getY() + "");
        z_coordinate_text.setText(itemPropertyList.get(position).getZ() + "");

        return view;
    }
}
