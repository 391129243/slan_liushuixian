package com.gzlab.autoassemblyline_client.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * 自定义熔解步骤View
 */
public class MeltStepView extends View {
    private int maxValue = 100;
    private int minValue = 0;
    private int leftValue = 60;
    private int rightValue = 90;
    private int lastValue;
    private Paint mPaint;
    private Paint mTextPaint;
    private Paint mBrokenLinePaint;//折线
    private int viewHeight;
    private int viewWidth;

    private int leftPointY;
    private int rightPonitY;
    private int rightPointX;
    private int leftPointX;
    private int centerPointX;
    private int rightStartPointX;
    private int distance;//文字高度
    private boolean drawLeftLine = true;//是否画左边的线
    private String lasttime;
    private float scale;//每一份占多少像素
    public MeltStepView(Context context) {
        super(context);
        init();
    }

    public MeltStepView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        //画横线
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

        leftPointX = viewWidth/4;
        centerPointX = viewWidth/2;

        rightStartPointX = 3*viewWidth/4;
        rightPointX = getMeasuredWidth();
        scale = (viewHeight - 2 * distance) / (maxValue - minValue);
    }


    public void setStepText(int value,int rightValue,String time){
     //   setCurrentValue(value);
        setLeftValue(value);
        setRightValue(rightValue);
        setLastTime(time);
        invalidate();
    }


    private void setLeftValue(int value){
        this.leftValue = value;
    }

    private void setRightValue(int value){
        this.rightValue = value;
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
       // pointY = (int) (scale * (maxValue - currentValue) + distance);

        leftPointY = (int) (scale * (maxValue - leftValue) + distance);
        rightPonitY = (int) (scale * (maxValue - rightValue) + distance);
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
        float baseLine1 = leftPointY - fontMetrics.bottom * 2;
        canvas.drawText(  leftValue+"℃", viewWidth / 4+viewWidth/6, baseLine1, mTextPaint);

        float baseLine2 = rightPonitY - fontMetrics.bottom * 2;
        canvas.drawText(  rightValue+"℃", 3*viewWidth / 4+viewWidth/6, baseLine2, mTextPaint);

        //画出时长
        /*float baseLine3 = pointY + fontMetrics.bottom * 2;
        canvas.drawText(  lasttime, viewWidth / 4+viewWidth/4, baseLine3, mTextPaint);*/
    }




    /**
     * 画横线
     * @param canvas
     */
    private void drawLanscapeLine(Canvas canvas){

        //canvas.drawLine(pointX, pointY, getMeasuredWidth(), pointY, mPaint);
        drawLeftLanscapeLine(canvas);
        drawRightLanscapeLine(canvas);
    }

    //画左边的横线
    private void drawLeftLanscapeLine(Canvas canvas){
        canvas.drawLine(leftPointX, leftPointY, centerPointX, leftPointY, mPaint);
    }

    //画左边的横线
    private void drawRightLanscapeLine(Canvas canvas){
        canvas.drawLine(rightStartPointX, rightPonitY, getMeasuredWidth(), rightPonitY, mPaint);
    }

    /**
     * 画折线
     *
     * @param canvas
     */
    private void drawGraph(Canvas canvas) {

        //画与前一个连接的折线
        if (drawLeftLine) {
            float middleY = (scale * (maxValue - lastValue) + distance);
            canvas.drawLine(0, middleY, leftPointX, leftPointY, mBrokenLinePaint);
        }
        //画之间两个点的折线
        float middleY = (scale * (maxValue - leftValue) + distance);
        canvas.drawLine(centerPointX, middleY, rightStartPointX, rightPonitY, mBrokenLinePaint);
    }


    public void setDrawLeftLine(boolean drawLeftLine) {
        this.drawLeftLine = drawLeftLine;
    }

}
