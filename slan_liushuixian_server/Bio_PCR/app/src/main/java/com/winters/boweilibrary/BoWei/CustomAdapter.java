package com.winters.boweilibrary.BoWei;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRTupeData;
import com.gidi.bio_pcr.util.NumberUtils;

import java.util.List;

public class CustomAdapter extends BaseAdapter {

    private List<BoWeiPCRTupeData> fluorescence;

    public CustomAdapter(List<BoWeiPCRTupeData> fluorescence) {
            this.fluorescence = fluorescence;
        }

        @Override
        public int getCount() {
            return fluorescence.size();
        }

        @Override
        public Object getItem(int position) {
            return fluorescence.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bowei_list_item, parent, false);
            }

            TextView fam = convertView.findViewById(R.id.fam);
            TextView hex = convertView.findViewById(R.id.hex);
            TextView rox = convertView.findViewById(R.id.rox);
            TextView cy5 = convertView.findViewById(R.id.cy5);

            BoWeiPCRTupeData item = fluorescence.get(position);
            fam.setText(NumberUtils.doubleto2String(item.getFluorescence_FAM()));
            hex.setText(NumberUtils.doubleto2String(item.getFluorescence_HEX()));
            rox.setText(NumberUtils.doubleto2String(item.getFluorescence_ROX()));
            cy5.setText(NumberUtils.doubleto2String(item.getFluorescence_CY5()));

            return convertView;
        }

}
