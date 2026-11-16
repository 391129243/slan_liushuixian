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
import com.gidi.bio_pcr.config.FileBean;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class FilesListAdapter extends BaseAdapter {

    private static final String TAG = "FilesListAdapter";
    //默认空列表,保证 list 永远非 null
    private List<FileBean> list = Collections.emptyList();
    private final Context context;
    private final LayoutInflater mInflater;
    private int selectedPosition = -1;

    public FilesListAdapter(Context context) {
        this.context = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setFileList(List<FileBean> list) {
        //防御:null 转空列表,避免外部误传 null 后 Adapter 进入不可用状态
        this.list = list != null ? list : Collections.emptyList();
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position){
        this.selectedPosition = position;
        //修复 C2:同步刷新,使选中态真正反映到 UI
        notifyDataSetChanged();
    }



    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public FileBean getItem(int i) {
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
        //防御:list 为空或 position 越界时直接返回占位 view
        if (list == null || position < 0 || position >= list.size()) {
            Log.w(TAG, "getView 跳过越界访问 position=" + position
                    + " size=" + (list == null ? 0 : list.size()));
            return convertView != null ? convertView : new View(context);
        }

        ViewHolder viewHolder;
        if(null != convertView){
            viewHolder = (ViewHolder) convertView.getTag();
        }else {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_list_exported_file, viewGroup, false);
            viewHolder.fileNameTv = (TextView) convertView.findViewById(R.id.item_file_name_tv);
            viewHolder.icon = (ImageView)convertView.findViewById(R.id.item_file_icon_iv);
            viewHolder.checkImg = (ImageView)convertView.findViewById(R.id.item_file_ck_img);
            viewHolder.itemLayout = (RelativeLayout)convertView.findViewById(R.id.item_file_root_layout);
            convertView.setTag(viewHolder);
        }

        //只访问一次,后续以 final 局部变量持有,屏蔽 list 后续变更
        final FileBean fileBean = list.get(position);
        if (fileBean == null) {
            return convertView;
        }
        File file = fileBean.getFile();
        if (file == null) {
            return convertView;
        }
        //设置文件ICON
        viewHolder.icon.setImageResource(getResIdFromFileName(file.isDirectory(),
                file.getName()));
        viewHolder.fileNameTv.setText(file.getName());

        //修复 C1:让 selectedPosition 真正在 UI 上反映(选中态使用更明显的背景色 + 勾选图标)
        final boolean isSelected = (position == selectedPosition);
        if(fileBean.isCheck() || isSelected){
            viewHolder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.color_recycle));
            viewHolder.checkImg.setImageResource(R.drawable.item_file_check);
        }else {
            viewHolder.checkImg.setImageResource(R.drawable.item_file_uncheck);
            viewHolder.itemLayout.setBackgroundColor(context.getResources().getColor(R.color.color_login_edit));
        }

        if(fileBean.isShow() || isSelected){
            viewHolder.checkImg.setVisibility(View.VISIBLE);
        }else {
            viewHolder.checkImg.setVisibility(View.INVISIBLE);
        }

        viewHolder.itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Adapter 内防御:既检查下界也检查上界
                int currentPos = position;
                if (currentPos < 0 || currentPos >= list.size()) {
                    Log.w(TAG, "onClick 跳过越界 position=" + currentPos);
                    return;
                }
                selectedPosition = currentPos;
                FileBean bean1 = list.get(currentPos);
                if (bean1 == null) {
                    return;
                }
                if(bean1.isShow()){
                    if(null != onItemClickListener){
                        onItemClickListener.onItemCheck(currentPos);
                    }
                }else{
                    if (null != onItemClickListener) {
                        onItemClickListener.onItemClick(currentPos);
                    }
                }
            }
        });

        viewHolder.itemLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                //Adapter 内防御
                int currentPos = position;
                if (currentPos < 0 || currentPos >= list.size()) {
                    Log.w(TAG, "onLongClick 跳过越界 position=" + currentPos);
                    return false;
                }
                selectedPosition = currentPos;
                if(null != onItemClickListener){
                    onItemClickListener.onItemLongClick(currentPos);
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