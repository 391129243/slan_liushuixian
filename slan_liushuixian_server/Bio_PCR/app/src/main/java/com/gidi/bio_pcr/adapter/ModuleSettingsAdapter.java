package com.gidi.bio_pcr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.bean.ConnectModel;
import com.paul.pipeline.data.ConstantData;

import java.util.ArrayList;
import java.util.List;

public class ModuleSettingsAdapter extends BaseAdapter {


    private LayoutInflater mInflater;
    private List<ConnectModel> moduleDevices;
    private Context context;
    private int selectedPosition = -1;


    public ModuleSettingsAdapter(Context context){
        this.context = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        moduleDevices = new ArrayList<ConnectModel>();
    }

    public void setModuleList(List<ConnectModel> list) {
        this.moduleDevices = list;
        this.notifyDataSetChanged();
    }



    @Override
    public int getCount() {
        return moduleDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return moduleDevices.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    // ===================== 🔥 获取当前选中 =====================
    public int getSelectedPosition() {
        return selectedPosition;
    }
    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ModuleSettingsAdapter.ViewHolder holder;
        if(null != view){
            holder = (ModuleSettingsAdapter.ViewHolder) view.getTag();
        }else{
            holder = new ModuleSettingsAdapter.ViewHolder();
            view = mInflater.inflate(R.layout.item_list_module_manager,viewGroup,false);
            holder.rootLl = (LinearLayout)view.findViewById(R.id.root_ll) ;
            holder.indexTv = (TextView)view.findViewById(R.id.item_module_manager_index_tv);
            holder.moduleIdv =  (TextView)view.findViewById(R.id.item_module_name_tv);
            holder.moduleTypeTv = (TextView)view.findViewById(R.id.item_module_type_tv);

            holder.modulePosTv =  (TextView)view.findViewById(R.id.item_module_number_tv);
            holder.moduleIpTv = (TextView)view.findViewById(R.id.item_module_device_ip_tv);
            holder.deviceVersionTv = (TextView)view.findViewById(R.id.item_module_device_version_tv);
            holder.deviceStateTv = (TextView)view.findViewById(R.id.item_module_device_state_tv) ;

            view.setTag(holder);
        }

        ConnectModel connectModel = moduleDevices.get(position);
        holder.indexTv.setText(position+ 1 + "");
        holder.moduleIdv.setText(connectModel.deviceId.get());
        holder.deviceVersionTv.setText(connectModel.deviceVersion.get());
        holder.moduleTypeTv.setText(ConstantData.namePCR);
        holder.moduleIpTv.setText(connectModel.deviceIp.get());
        holder.modulePosTv.setText(connectModel.position.get()+"");
        if(connectModel.deviceConnect.get()){
            if(connectModel.status.get().equals(ConstantData.str_FREE)){
                holder.deviceStateTv.setText(R.string.pipe_main_traystatus_unused);
                holder.deviceStateTv.setTextColor(context.getResources().getColor(R.color.chart_green));
            }else if(connectModel.status.get().equals(ConstantData.str_RUN)){
                holder.deviceStateTv.setText(R.string.pipe_main_traystatus_using);
                holder.deviceStateTv.setTextColor(context.getResources().getColor(R.color.chart_green));
            }else if(connectModel.status.get().equals(ConstantData.str_ERROR)){
                holder.deviceStateTv.setText(R.string.pipe_main_traystatus_error);
                holder.deviceStateTv.setTextColor(context.getResources().getColor(R.color.red));
            }else if(connectModel.status.get().equals(ConstantData.str_FINISH)){
                holder.deviceStateTv.setText(R.string.pipe_main_traystatus_done);
                holder.deviceStateTv.setTextColor(context.getResources().getColor(R.color.white));
            }
        }else{
            holder.deviceStateTv.setText(R.string.dissconnected);
            holder.deviceStateTv.setTextColor(context.getResources().getColor(R.color.red));
        }

        holder.moduleIdv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null !=onItemClickListener ){
                    onItemClickListener.onItemDeviceIdClick(view,position);
                }
            }
        });

        holder.modulePosTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onItemClickListener){
                    onItemClickListener.onItemPositionClick(view,position);
                }
            }
        });



        return view;
    }

    class ViewHolder{
        LinearLayout rootLl;
        TextView indexTv;
        TextView moduleIdv;//设备名称//deviceID
        TextView moduleTypeTv;//设备类型,一体机
        TextView modulePosTv;//设备编号
        TextView moduleIpTv;//设备IP
        TextView deviceVersionTv;//设备编号
        TextView deviceStateTv;//设备编号

    }

    private OnItemClickListener onItemClickListener;
    public void setOnItemClickListener(OnItemClickListener listener){
        this.onItemClickListener = listener;
    }
    public interface OnItemClickListener {

        void onItemDeviceIdClick(View v,int item_position);

        void onItemPositionClick(View v,int item_position);

    }
}

