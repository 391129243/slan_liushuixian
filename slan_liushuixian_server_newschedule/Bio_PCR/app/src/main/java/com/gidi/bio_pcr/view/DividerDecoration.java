package com.gidi.bio_pcr.view;


import android.content.Context;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class DividerDecoration extends RecyclerView.ItemDecoration {
    private int spacing;//间距padding

    public DividerDecoration(Context context, int spacings) {
        //获取需要设置的间距值dp，这里不写死，调用时传过来
//        spacing = context.getResources().getDimensionPixelSize(R.dimen.d10);
        spacing = spacings;
    }

    @Override
    public void getItemOffsets(Rect outRect,View view, RecyclerView parent,  RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        outRect.left = spacing;//左边间距
    }
}

