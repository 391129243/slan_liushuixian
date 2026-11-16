package com.gzlab.autoassemblyline_client.view;

import android.content.Context;
import android.widget.TextView;

import com.gzlab.autoassemblyline_client.R;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

public class LineChartMarkView extends MarkerView {
    private TextView contentTv;
    /**
     * Constructor. Sets up the MarkerView with a custom layout resource.
     *
     * @param context
     *
     */
    public LineChartMarkView(Context context) {
        super(context, R.layout.layout_linechart_markview);
        initView();
    }

    private void initView(){
        contentTv = (TextView)findViewById(R.id.mark_view_tv);

    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        super.refreshContent(e, highlight);
        contentTv.setText("x="+ e.getX() + " y=" + e.getY());
    }

    private MPPointF mOffset;


    @Override
    public MPPointF getOffset() {
        //设置MarkerView的偏移量，就是提示框显示的位置
        if (mOffset == null) {
            // center the marker horizontally and vertically
            mOffset = new MPPointF(-(getWidth() / 2), -getHeight());
        }

        return mOffset;
    }



    private MPPointF mOffset2 = new MPPointF();

    @Override
    public MPPointF getOffsetForDrawingAtPoint(float posX, float posY) {

        MPPointF offset = getOffset();
        mOffset2.x = offset.x;
        mOffset2.y = offset.y;

        Chart chart = getChartView();

        float width = getWidth();
        float height = getHeight();

        if (posX + mOffset2.x < 0) {
            //第一个数据的时候，超出屏幕了，实现父类方法改一改
            mOffset2.x = offset.x;
        } else if (chart != null && posX + width + mOffset2.x > chart.getWidth()) {
            mOffset2.x = chart.getWidth() - posX - width;
        }

        if (posY + mOffset2.y < 0) {
            mOffset2.y = -posY;
        } else if (chart != null && posY + height + mOffset2.y > chart.getHeight()) {
            mOffset2.y = chart.getHeight() - posY - height;
        }

        return mOffset2;
    }
}
