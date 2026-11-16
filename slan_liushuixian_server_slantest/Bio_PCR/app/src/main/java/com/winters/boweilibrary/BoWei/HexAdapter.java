package com.winters.boweilibrary.BoWei;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.winters.boweilibrary.BoWei.tempdata.BoWeiPCRStepEntity;

import java.util.List;

public class HexAdapter extends BaseAdapter {

    List<BoWeiPCRStepEntity> BoWeiPCRStepEntityList;

    public HexAdapter(List<BoWeiPCRStepEntity> boWeiPCRStepEntityList) {
        this.BoWeiPCRStepEntityList = boWeiPCRStepEntityList;
    }

    @Override
    public int getCount() {
        return BoWeiPCRStepEntityList.size();
    }

    @Override
    public Object getItem(int position) {
        return BoWeiPCRStepEntityList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.bowei_hexlist_item, parent, false);
        }

        TextView num = convertView.findViewById(R.id.num);
        TextView name = convertView.findViewById(R.id.name);
        TextView hex = convertView.findViewById(R.id.hex);
        TextView cycles = convertView.findViewById(R.id.cycles);
        TextView time = convertView.findViewById(R.id.time);
        TextView description = convertView.findViewById(R.id.description);
        TextView tempreture = convertView.findViewById(R.id.tempreture);
        TextView tempreture_up = convertView.findViewById(R.id.tempreture_up);
        TextView tempreture_down = convertView.findViewById(R.id.tempreture_down);
        TextView fluro_amp = convertView.findViewById(R.id.fluro_amp);
        TextView fluro_melt = convertView.findViewById(R.id.fluro_melt);
        TextView memory = convertView.findViewById(R.id.memory);

        BoWeiPCRStepEntity item = BoWeiPCRStepEntityList.get(position);

        num.setText((position+1) + "");
        name.setText(item.getName());
        hex.setText(item.getPCRCmd());
        cycles.setText(item.getLoop() + "");
        time.setText(item.getTime() + "");
        description.setText(item.getDescription());
        tempreture.setText(item.getPCR_temperature() + "");
        tempreture_up.setText(item.isHeat()? "√" : "");
        tempreture_down.setText(item.isCool()? "√" : "");
        fluro_amp.setText(item.isAcquisitionAMP()? "√" : "");
        fluro_melt.setText(item.isAcquisitionMelt()? "√" : "");
        memory.setText(item.getMemoryNote());

        return convertView;
    }
}
