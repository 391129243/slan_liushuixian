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
import com.gidi.bio_pcr.config.FileBean;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FilesListAdapter extends BaseAdapter {

    private List<FileBean> list;
    private Context context;
    private LayoutInflater mInflater;
    private int selectedPosition = -1;

    public FilesListAdapter(Context context) {
        this.context = context;
        list = new ArrayList<FileBean>();
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setFileList(List<FileBean> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position){
        this.selectedPosition = position;
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
                convertView = mInflater.inflate(R.layout.item_list_exported_file, viewGroup, false);
            }
            return convertView;
        }

        ViewHolder viewHolder;
        if(null != convertView){
            viewHolder = (ViewHolder) convertView.getTag();
            if (viewHolder == null) {
                viewHolder = new ViewHolder();
                viewHolder.fileNameTv = (TextView) convertView.findViewById(R.id.item_file_name_tv);
                viewHolder.icon = (ImageView)convertView.findViewById(R.id.item_file_icon_iv);
                viewHolder.checkImg = (ImageView)convertView.findViewById(R.id.item_file_ck_img);
                viewHolder.itemLayout = (RelativeLayout)convertView.findViewById(R.id.item_file_root_layout);
                convertView.setTag(viewHolder);
            }
        }else {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_list_exported_file, viewGroup, false);
            viewHolder.fileNameTv = (TextView) convertView.findViewById(R.id.item_file_name_tv);
            viewHolder.icon = (ImageView)convertView.findViewById(R.id.item_file_icon_iv);
            viewHolder.checkImg = (ImageView)convertView.findViewById(R.id.item_file_ck_img);
            viewHolder.itemLayout = (RelativeLayout)convertView.findViewById(R.id.item_file_root_layout);
            convertView.setTag(viewHolder);
        }

        FileBean fileBean = list.get(position);
        if (fileBean == null) {
            return convertView;
        }
        //设置文件ICON
        File file = (File)fileBean.getFile();
        if (file == null) {
            return convertView;
        }
        viewHolder.icon.setImageResource(getResIdFromFileName(file.isDirectory(),
                file.getName()));
        viewHolder.fileNameTv.setText(file.getName());

        if(fileBean.isCheck()){
            viewHolder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.color_recycle));
            viewHolder.checkImg.setImageResource(R.drawable.item_file_check);
        }else {
            viewHolder.checkImg.setImageResource(R.drawable.item_file_uncheck);
            viewHolder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.color_login_edit));
        }

        if(fileBean.isShow()){
            viewHolder.checkImg.setVisibility(View.VISIBLE);
        }else {
            viewHolder.checkImg.setVisibility(View.INVISIBLE);
        }
        viewHolder.itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (list == null || position < 0 || position >= list.size()) {
                    return;
                }
                selectedPosition = position;
                FileBean bean1 = list.get(position);
                if (bean1 == null) {
                    return;
                }
                if(bean1.isShow()){
                    if(null != onItemClickListener){
                        onItemClickListener.onItemCheck(position);
                    }
                }else{
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemClick(position);
                    }
                }
            }
        });

        viewHolder.itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {

                selectedPosition = position;
                if(null != onItemClickListener){
                    onItemClickListener.onItemLongClick(position);
                }
                return false;
            }
        });
        return convertView;
    }

    class ViewHolder{
        TextView fileNameTv;
        private ImageView checkImg;
        RelativeLayout itemLayout;

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