package com.gzlab.autoassemblyline_client.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class CustomStepView extends View {
    private int maxValue = 100;
    private int minValue = 0;
    private int currentValue=20;
    private int lastValue;
    private Paint mPaint;
    private Paint mTextPaint;
    private Paint mBrokenLinePaint;//折线
    private int viewHeight;
    private int viewWidth;
    private int pointX;//所有点的x坐标
    private int pointY;//当前点的Y
    private int rightPointX;
    private int distance;//文字高度
    private boolean drawLeftLine = true;//是否画左边的线
    private String lasttime;
    private float scale;//每一份占多少像素
    public CustomStepView(Context context) {
        super(context);
        init();
    }

    public CustomStepView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        mPaint = new Paint();
        mPaint.setColor(0xff3395D2);
        mPaint.setStrokeWidth(6);
        mPaint.setStyle(Paint.Style.FILL);


        //画文字
        mTextPaint = new Paint();
        mTextPaint.setTextSize(18);
        mTextPaint.setColor(Color.parseColor("#ff8a8a8a"));
        mTextPaint.setStrokeWidth(0);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        //折线画笔
        mBrokenLinePaint = new Paint();
        mBrokenLinePaint.setColor(0xff3395D2);
        mBrokenLinePaint.setStrokeWidth(2);
        mBrokenLinePaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        initValues();
    }

    private void initValues(){
        mPaint.setTextSize(32);
        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        distance = (int) (fontMetrics.descent + Math.abs(fontMetrics.ascent));
        viewHeight = getMeasuredHeight();
        viewWidth = getMeasuredWidth();
        pointX  = viewWidth/2;
        rightPointX = getMeasuredWidth();
        scale = (viewHeight - 2 * distance) / (maxValue - minValue);
    }


    public void setStepText(int value,String time){
        setCurrentValue(value);
        setLastTime(time);
        invalidate();
    }

    private void setCurrentValue(int value){
        this.currentValue = value;

    }

    /**
     * 步骤持续时间
     * @param time
     */
    private void setLastTime(String time){
        this.lasttime = time;
    }


    public void setlastValue(int lastValue) {
        this.lastValue = lastValue;

    }


    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        pointY = (int) (scale * (maxValue - currentValue) + distance);
       // pointY = (int) (scale*(maxValue - currentValue));
        //drawLine(canvas);
        drawLanscapeLine(canvas);
        drawGraph(canvas);
        drawText(canvas);
    }

    /**
     * 画时间
     *
     * @param canvas
     */
    private void drawText(Canvas canvas) {

        Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
        float baseLine1 = pointY - fontMetrics.bottom * 2;
        canvas.drawText(  currentValue+"℃", viewWidth / 2+viewWidth/4, baseLine1, mTextPaint);

        //画出时长
        float baseLine2 = pointY + fontMetrics.bottom * 2;
        canvas.drawText(  lasttime, viewWidth / 2+viewWidth/4, baseLine2, mTextPaint);
    }




    /**
     * 画横线
     * @param canvas
     */
    private void drawLanscapeLine(Canvas canvas){

        canvas.drawLine(pointX, pointY, getMeasuredWidth(), pointY, mPaint);
    }

    /**
     * 画折线
     *
     * @param canvas
     */
    private void drawGraph(Canvas canvas) {

        if (drawLeftLine) {
            float middleY = (scale * (maxValue - lastValue) + distance);
            canvas.drawLine(pointX - viewWidth/2, middleY, pointX, pointY, mBrokenLinePaint);
        }

    }



    /**
     * 画最下面的一条线
     *
     * @param
     *//*
    private void drawLine(Canvas canvas) {
        mPaint.setColor(0xffD3D3D3);
        mPaint.setPathEffect(null);

        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.FILL);
        if (drawLeftLine) {
            canvas.drawLine(0, 500, viewWidth / 2, 500, mPaint);//这两个值表示下面那条横线的Y坐标（第二四个参数）

        }
        if (drawRightLine) {
            canvas.drawLine(viewWidth / 2, 500, viewWidth, 500, mPaint);//这两个值表示下面那条横线的Y坐标（第二四个参数）

        }
    }*/



    public void setDrawLeftLine(boolean drawLeftLine) {
        this.drawLeftLine = drawLeftLine;
    }

}
