package com.gidi.bio_pcr.config.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gidi.bio_pcr.R;
import com.gidi.bio_pcr.config.UsbFileBean;

import java.util.Collections;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

/**
 * 点击的效果
 * 长按选中的效果
 */

public class UsbFileAdapter extends BaseAdapter {
    private static final String TAG = "UsbFileAdapter";
    //默认空列表,保证 list 永远非 null,getCount/getView 不会 NPE
    private List<UsbFileBean> list = Collections.emptyList();
    private final Context context;
    private int selectedPosition = -1;
    private final LayoutInflater mInflater;

    public UsbFileAdapter(Context context){
        this.context = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setFileList(List<UsbFileBean> list) {
        //防御:null 转空列表,避免外部误传 null 后 Adapter 进入不可用状态
        this.list = list != null ? list : Collections.emptyList();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position){
        selectedPosition = position;
        notifyDataSetChanged();
    }


    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public UsbFileBean getItem(int i) {
        if (i < 0 || i >= list.size()) {
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
        //防御:list 为空或 position 越界时直接返回占位 view,避免 Adapter 内崩溃向上传递
        if (list == null || position < 0 || position >= list.size()) {
            Log.w(TAG, "getView 跳过越界访问 position=" + position
                    + " size=" + (list == null ? 0 : list.size()));
            return convertView != null ? convertView : new View(context);
        }

        ViewHolder holder;
        if(null != convertView){
            holder = (ViewHolder) convertView.getTag();
        }else {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_list_exported_usb_file, viewGroup, false);
            holder.fileNameTv = (TextView) convertView.findViewById(R.id.item_file_name_tv);
            holder.icon = (ImageView)convertView.findViewById(R.id.item_file_icon_iv);
            holder.itemLayout = (RelativeLayout)convertView.findViewById(R.id.item_file_root_layout);
            holder.checkImg = (ImageView) convertView.findViewById(R.id.item_file_ck_img);
            convertView.setTag(holder);
        }

        //只访问一次,后续以 final 局部变量持有,屏蔽 list 后续变更
        final UsbFileBean usbFileBean = list.get(position);
        if (usbFileBean == null) {
            return convertView;
        }
        final UsbFile file = usbFileBean.getUsbFile();
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
                //Adapter 内再做一道防御,避免 list 已被外部清空后回调崩溃
                int currentPos = position;
                if (currentPos < 0 || currentPos >= list.size()) {
                    Log.w(TAG, "onClick 跳过越界 position=" + currentPos);
                    return;
                }
                if(!usbFileBean.isShow()){
                    if(null != onItemClickListener){
                        onItemClickListener.onItemClick(currentPos);
                    }
                }else{
                    //多选框显示
                    if(null != onItemClickListener){
                        onItemClickListener.onItemCheck(currentPos);
                    }
                }
            }
        });

        //长按
        holder.itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //Adapter 内再做一道防御
                int currentPos = position;
                if (currentPos < 0 || currentPos >= list.size()) {
                    Log.w(TAG, "onLongClick 跳过越界 position=" + currentPos);
                    return false;
                }
                if(!usbFileBean.isShow()){
                    if(null != onItemClickListener){
                        onItemClickListener.onItemLongClick(currentPos);
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
