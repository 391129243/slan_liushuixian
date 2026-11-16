package com.gzlab.autoassemblyline_client.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.gzlab.autoassemblyline_client.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

public class FileListAdapter<T> extends BaseAdapter {

    private List<T> list;
    private Context context;

    private int selectedPosition = -1;

    public FileListAdapter(Context context){
        this.context = context;
        list = new ArrayList<T>();
    }

    public void setSelectedPosition(int position){
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public void setFileList(List<T> list){
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.item_usb_file_list, null);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        T t = list.get(position);
        if (t instanceof UsbFile) {
            UsbFile usbFile = (UsbFile) t;
            //设置文件ICON
            holder.icon.setImageResource(getResIdFromFileName(usbFile.isDirectory(),
                    usbFile.getName()));
            holder.tv.setText(usbFile.getName());

        } else if (t instanceof File) {
            File file = (File) t;
            //设置文件ICON
            holder.icon.setImageResource(getResIdFromFileName(file.isDirectory(),
                    file.getName()));
            holder.tv.setText(file.getName());

        }

        if (selectedPosition == position) {
            holder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.color_recycle));
        } else {
            holder.rootLayout.setBackgroundColor(context.getResources().getColor(R.color.white));
        }


        return convertView;
    }

    /**
     * 文件大小转换
     *
     * @param size 文件大小
     * @return 文件大小（B / KB / MB）
     */
    private String getFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return (size / 1024) + " kB";
        } else {
            return (size / 1024 / 1024) + " MB";
        }
    }

    /**
     * 通过文件名（路径）返回对应的 ICON 资源 ID
     *
     * @param isFolder 是否是文件夹
     * @param fileName 文件名（路径）
     * @return 对应的资源ID
     */
    private static int getResIdFromFileName(boolean isFolder, String fileName) {
        fileName = fileName.toLowerCase();
        if (isFolder) {
            //文件夹
            return R.drawable.icon_folder;
        } else {
            if(fileName.endsWith(".hex")|| fileName.endsWith(".proj")){
                return R.drawable.icon_file;
            }else{
                return R.drawable.icon_file;
            }

        }
    }

    private class ViewHolder {

        private TextView tv;
        private ImageView icon;

        private LinearLayout rootLayout;

        ViewHolder(View view) {
            tv = (TextView) view.findViewById(R.id.file_name_tv);
            icon = (ImageView) view.findViewById(R.id.file_icon_iv);
            rootLayout = (LinearLayout) view.findViewById(R.id.item_file_rootlayout);
        }
    }
}
