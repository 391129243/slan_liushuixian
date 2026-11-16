package com.gzlab.autoassemblyline_client.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.gzlab.autoassemblyline_client.R;

import java.text.DecimalFormat;

public class RingProgressBar extends View {

    // 画实心圆的画笔
    private Paint mCirclePaint;
    // 画圆环的画笔
    private Paint mRingPaint;
    // 画圆环的画笔背景色
    private Paint mRingPaintBg;
    // 画字体的画笔
    private Paint mTextPaint;
    // 圆形颜色
    private int mCircleColor;
    // 圆环颜色
    private int mRingColor;
    // 圆环背景颜色
    private int mRingBgColor;
    // 字体颜色
    private int mTextColor;
    // 字体大小
    private float mTextSize;
    // 半径
    private float mRadius;
    // 圆环半径
    private float mRingRadius;
    // 圆环宽度
    private float mStrokeWidth;
    // 圆心x坐标
    private int mXCenter;
    // 圆心y坐标
    private int mYCenter;
    // 字的长度
    private float mTxtWidth;
    // 字的高度
    private float mTxtHeight;
    // 总进度
    private int mTotalProgress = 100;
    // 当前进度
    private float mProgress;

    private String mProgressTxt;

    public RingProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
        initVariable();
    }

    private void initAttrs(Context context,AttributeSet attrs) {
        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.RingProgressBar, 0, 0);
        mRadius = typeArray.getDimension(R.styleable.RingProgressBar_circle_radius, dp2px(30));
        mStrokeWidth = typeArray.getDimension(R.styleable.RingProgressBar_stroke_width,dp2px(6));
        mCircleColor = typeArray.getColor(R.styleable.RingProgressBar_circle_color, Color.parseColor("#ffffff"));
        mRingColor = typeArray.getColor(R.styleable.RingProgressBar_ring_cor, Color.parseColor("#3395D2"));
        mRingBgColor = typeArray.getColor(R.styleable.RingProgressBar_ring_background_color,Color.parseColor("#E3E2E2"));
        mTextColor = typeArray.getColor(R.styleable.RingProgressBar_font_color,Color.parseColor("#2E3D45"));
        mTextSize = typeArray.getDimension(R.styleable.RingProgressBar_font_size,mRadius / 3);
        mRingRadius = mRadius + mStrokeWidth / 2;
    }

    private void initVariable() {
        //内圆
        mCirclePaint = getPaint(Paint.Style.FILL,mCircleColor);


        //外圆弧背景
        mRingPaintBg = getPaint(Paint.Style.STROKE,mRingBgColor,mStrokeWidth);

        //外圆弧
        mRingPaint = getPaint(Paint.Style.STROKE,mRingColor,mStrokeWidth);
        mRingPaint.setStrokeCap(Paint.Cap.ROUND);//设置线冒样式，有圆 有方

        //中间字
        mTextPaint = getPaint(Paint.Style.FILL,mTextColor);
        mTextPaint.setTextSize(mTextSize);

        Paint.FontMetrics fm = mTextPaint.getFontMetrics();
        mTxtHeight = (int) Math.ceil(fm.descent - fm.ascent);
    }

    private Paint getPaint(Paint.Style style,int color){
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG); //抗锯齿标志
        paint.setStyle(style);
        paint.setColor(color);
        paint.setAntiAlias(true);
        return paint;
    }
    private Paint getPaint(Paint.Style style,int color,float width){
        Paint paint = getPaint(style,color);
        paint.setStrokeWidth(width);
        return paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        mXCenter = getWidth() / 2;
        mYCenter = getHeight() / 2;

        //内圆
        canvas.drawCircle(mXCenter, mYCenter, mRadius, mCirclePaint);

        //外圆弧背景
        RectF oval1 = new RectF();
        oval1.left = (mXCenter - mRingRadius);
        oval1.top = (mYCenter - mRingRadius);
        oval1.right = mRingRadius * 2 + (mXCenter - mRingRadius);
        oval1.bottom = mRingRadius * 2 + (mYCenter - mRingRadius);
        canvas.drawArc(oval1, 0, 360, false, mRingPaintBg); //圆弧所在的椭圆对象、圆弧的起始角度、圆弧的角度、是否显示半径连线

        //外圆弧
        if (mProgress > 0 ) {
            RectF oval = new RectF();
            oval.left = (mXCenter - mRingRadius);
            oval.top = (mYCenter - mRingRadius);
            oval.right = mRingRadius * 2 + (mXCenter - mRingRadius);
            oval.bottom = mRingRadius * 2 + (mYCenter - mRingRadius);
            canvas.drawArc(oval, -90, -(((float)mProgress / mTotalProgress) * 360), false, mRingPaint); //

            //字体
            String txt = mProgressTxt ;
            mTxtWidth = mTextPaint.measureText(txt, 0, txt.length());
            canvas.drawText(txt, mXCenter - mTxtWidth / 2, mYCenter + mTxtHeight / 4, mTextPaint);
        }
    }

    //设置进度
    public void setProgress(float progress,String progressTxt) {
        final ValueAnimator anim = ValueAnimator.ofFloat(0,progress);
        anim.setDuration(3000);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                mProgress  = (float)animator.getAnimatedValue();
                DecimalFormat df2 = new DecimalFormat("###.00");
                drawText(progressTxt);
                invalidate();
            }
        });
        anim.start();
    }
    protected int dp2px(int dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,dpVal,getResources().getDisplayMetrics());
    }

    private void drawText(String prgressTxt){
        mProgressTxt= prgressTxt;
    }

}
