package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.config.UsbFileBean;

import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

/**
 * 点击的效果
 * 长按选中的效果
 */

public class UsbFileAdapter extends BaseAdapter {
    private List<UsbFileBean> list;
    private Context context;
    private int selectedPosition = -1;
    private LayoutInflater mInflater;

    public UsbFileAdapter(Context context){
        this.context = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setFileList(List<UsbFileBean> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position){
        selectedPosition = position;
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        if (list == null) return 0;
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        if (list == null || i < 0 || i >= list.size()) {
            return null;
        }
        return list.get(i);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (list == null || position < 0 || position >= list.size()) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_list_exported_usb_file, viewGroup, false);
            }
            return convertView;
        }

        ViewHolder holder;
        if(null != convertView){
            holder = (ViewHolder) convertView.getTag();
            if (holder == null) {
                holder = new ViewHolder();
                holder.fileNameTv = (TextView) convertView.findViewById(R.id.item_file_name_tv);
                holder.icon = (ImageView)convertView.findViewById(R.id.item_file_icon_iv);
                holder.itemLayout = (RelativeLayout)convertView.findViewById(R.id.item_file_root_layout);
                holder.checkImg = (ImageView) convertView.findViewById(R.id.item_file_ck_img);
                convertView.setTag(holder);
            }
        }else {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_list_exported_usb_file, viewGroup, false);
            holder.fileNameTv = (TextView) convertView.findViewById(R.id.item_file_name_tv);
            holder.icon = (ImageView)convertView.findViewById(R.id.item_file_icon_iv);
            holder.itemLayout = (RelativeLayout)convertView.findViewById(R.id.item_file_root_layout);
            holder.checkImg = (ImageView) convertView.findViewById(R.id.item_file_ck_img);
            convertView.setTag(holder);
        }
        UsbFileBean usbFileBean = list.get(position);
        if (usbFileBean == null) {
            return convertView;
        }
        UsbFile file = (UsbFile) usbFileBean.getUsbFile();
        if (file == null) {
            return convertView;
        }
        holder.icon.setImageResource(getResIdFromFileName(file.isDirectory(),
                file.getName()));
        holder.fileNameTv.setText(file.getName());
        if(usbFileBean.isCheck()){
            holder.checkImg.setImageResource(R.drawable.item_file_check);
            holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.color_recycle));
        }else {
            holder.checkImg.setImageResource(R.drawable.item_file_uncheck);
            holder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.color_login_edit));
        }

        if(usbFileBean.isShow()){
            holder.checkImg.setVisibility(View.VISIBLE);
        }else {
            holder.checkImg.setVisibility(View.INVISIBLE);
        }


        holder.itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!usbFileBean.isShow()){
                    if(null != onItemClickListener){
                        onItemClickListener.onItemClick(position);
                    }
                }else{
                    //多选框显示
                    if(null != onItemClickListener){
                        onItemClickListener.onItemCheck(position);
                    }
                }
            }
        });

        //长按
        holder.itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(!usbFileBean.isShow()){
                    if(null != onItemClickListener){
                        onItemClickListener.onItemLongClick(position);
                    }
                }

                return false;
            }
        });

        return convertView;
    }

    class ViewHolder{
        TextView fileNameTv;
        RelativeLayout itemLayout;

        private ImageView checkImg;
        private ImageView icon;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onItemLongClick(int position);
        void onItemCheck(int position);

    }
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    private int getResIdFromFileName(boolean isFolder, String fileName) {
        fileName = fileName.toLowerCase();
        if (isFolder) {
            //文件夹
            return R.drawable.icon_item_file;
        } else {
            if(fileName.endsWith(".txt")|| fileName.endsWith(".log")){
                return R.drawable.icon_file_txt;
            }else if(fileName.endsWith(".rar")||fileName.endsWith(".war")){
                return R.drawable.icon_file_rar;
            }else{
                return R.drawable.icon_file_unknown;
            }
        }
    }
}
