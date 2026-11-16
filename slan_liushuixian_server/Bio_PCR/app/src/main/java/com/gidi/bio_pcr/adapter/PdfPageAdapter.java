package com.gidi.bio_pcr.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gidi.bio_pcr.R;

import java.util.List;

/**
 * 显示pdf帮助文档
 */
public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.ViewHolder>{
    private Context context;
    private List<Bitmap> pageList;

    public PdfPageAdapter(Context context) {
        this.context = context;
    }

    public void setPdfList(List<Bitmap> list){
        this.pageList = list;
        this.notifyDataSetChanged();;
    }
    @NonNull
    @Override
    public PdfPageAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_list_pdf, null));
    }

    @Override
    public void onBindViewHolder(@NonNull PdfPageAdapter.ViewHolder holder, int position) {
        Bitmap item = pageList.get(position);
      //  holder.itemIv.setLayoutParams(new FrameLayout.LayoutParams(context.getResources().getDisplayMetrics().widthPixels, context.getResources().getDisplayMetrics().heightPixels - 300));
        holder.itemIv.setImageBitmap(item);
    }

    @Override
    public int getItemCount() {
        return pageList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView itemIv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIv = itemView.findViewById(R.id.pdf_page_iv);
        }
    }
}
