package com.tomandjerry.coolanim.lib.pellet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

/**
 * 用animationset完成
 * 两种颜色交替,黄色和绿色,红色
 * 1.从黄色小圆点->大圆点,有一个膨胀回弹的感觉
 * 2.从中心向外射出8条黄线,然后消失,圆点空心.
 * 3.颜色从内往外渐变,内空心圆放大到一定程度,往回填充变为实心圆
 * 4.
 * Created by yanxing on 16/1/29.
 */
public class ThirdPellet extends Pellet {
    private int GREEN = Color.parseColor("#339966");
    private int YELLOW = Color.parseColor("#FFCC00");
    private int RED = Color.RED;
    private Paint mPaint;
    // 第一个圆或圆环或圆弧的半径和画笔大小
    private float mFiCurR;
    private float mFiStrokeWidth;
    // 第二个圆或圆环或圆弧的半径和画笔大小
    private float mSeCurR;
    private float mSeStrokeWidth;
    // 正常圆(能停留的)最大的直径
    private float STANDARD_MAX_R;
    // 正常圆(能停留的)最小的直径
    private float STANDARD_MAX_STROKE;
    // 正常圆(能停留的)最大的stroke
    private float STANDARD_MIN_R;
    // 前一个值
    private float mPreValue;
    // 当前值
    private float mCurValue;
    // 差值
    private float mDifValue;
    private AnimatorSet mAnimatorSet;
    //根据状态绘制不同的图案
    private int mState;
    // 用于绘制绿色弧线
    private RectF mOval;
    // 用于绿色弧线的角度
    private int mAngle;
    // 绿色弧线开口的角度
    private static final int GAP_ANGLE = 240;
    private int mGapGreenAngle;
    // 用于绘制红色弧线
    private RectF mRedOval;
    // 用于绘制红色弧线的角度
    private int mRedAngle;
    // 用于绘制红色弧线的开口角度
    private int mGapRedAngle;

    // 黄色小球,用于弹出
    private SmallYellowBall mBall;

    public ThirdPellet(int x, int y) {
        super(x, y);
    }

    @Override
    protected void initConfig() {
        mAnimatorSet = new AnimatorSet();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);

        // 初始化黄色小球
        mBall = SmallYellowBall.getInstance();
        mFiCurR = 45;
        mFiStrokeWidth = 30;
        mSeCurR = 15;
        mSeStrokeWidth = 15;
        STANDARD_MAX_R = 50;
        STANDARD_MIN_R = 15;
        // 放大弹出射线

        // 绿色圆弧包围红色圆,内部先产生间隔,红色圆膨胀,然后绿色圆弧和红色圆膨胀效果
        ValueAnimator flattenAnim = createFlattenAnim();
        // 等待黄色圆传递
        ValueAnimator waitForAnim = ValueAnimator.ofFloat(0, 100);
        waitForAnim.setDuration(1000);

        // 黄色圆缩小,绿色弧线出现,旋转从0->-120,从-120->-240,抛出黄色小球,绿色弧线逐渐变成球,
        // 红色弧线绕圈,逐渐合并为圆环,
        ValueAnimator smallerAndRotateAnim = createSmallerAndRotateAnim();

        // 红色弧线往内缩,绿色圆放大,回到膨胀效果
        ValueAnimator backAnim = createBackAnim();

        mAnimatorSet.playSequentially(flattenAnim, waitForAnim, smallerAndRotateAnim, backAnim);
        mAnimatorSet.start();
        mAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimatorSet.start();
            }
        });
    }

    /**
     * 红色弧线往内缩,绿色圆放大,回到膨胀效果
     * @return
     */
    protected ValueAnimator createBackAnim() {
        final float rate = (STANDARD_MAX_R - 45) / 30F;
        ValueAnimator backAnim = ValueAnimator.ofFloat(45, STANDARD_MIN_R);
        backAnim.setDuration(1500);
        backAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mState = 4;
                mCurValue = (float) animation.getAnimatedValue();
                mDifValue = mPreValue - mCurValue;
                // 红色圆弧缩小
                mFiStrokeWidth = 15;
                mFiCurR -= mDifValue * (1 + rate);

                // 绿色圆放大
                mSeStrokeWidth = 30;
                mSeCurR += mDifValue;
                mPreValue = mCurValue;
            }
        });
        backAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mState = 4;
                mPreValue = 45;
                mCurValue = 45;
            }
        });
        return backAnim;
    }

    /**
     * 绿色圆弧包围红色圆,内部先产生间隔,红色圆膨胀,然后绿色圆弧和红色圆膨胀效果
     * @return
     */
    protected ValueAnimator createFlattenAnim() {
        // 产生间隔大小
        int gap = 4;
        // 红色内圆膨胀大小
        float redFlattenValue = mFiStrokeWidth - gap - mSeStrokeWidth;
        // 减去多一个值是因为内圆膨胀的时候,半径也会增加
        float bothFlattenValue = STANDARD_MAX_R - mSeCurR - redFlattenValue / 2;
        // 绿色弧放大的速度,红色圆放大速率为1
        final float rate = (STANDARD_MAX_R - mFiCurR) / bothFlattenValue;
        // 第一个参数
        float fiv = 0;
        // 第二个参数,外圆内半径扩大距离
        final float sev = gap;
        // 第三个参数,内圆半径扩大,stroke增大的范围
        final float thv = sev + redFlattenValue;
        // 第四个参数,内外圆同时扩大,最后同大小的范围
        float fov = thv + bothFlattenValue;
        ValueAnimator flattenAnim = ValueAnimator.ofFloat(fiv, sev, thv, fov);
        flattenAnim.setDuration(1500);
        flattenAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mState = 1;
                mCurValue = (float) animation.getAnimatedValue();
                mDifValue = mCurValue - mPreValue;
                if (mCurValue <= sev) {
                    // 外圆内半径扩大
                    mFiStrokeWidth -= mDifValue;
                    mFiCurR += mDifValue / 2;
                } else if (mCurValue <= thv) {
                    // 内圆半径扩大,stroke增大
                    mSeStrokeWidth += mDifValue;
                    mSeCurR += mDifValue / 2;
                } else {
                    // 内外圆同时扩大,最后同大小
                    mSeCurR += mDifValue;
                    mFiCurR += mDifValue * rate;
                }
                mPreValue = mCurValue;
            }
        });
        flattenAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mState = 1;
                mFiCurR = 45;
                mFiStrokeWidth = 30;
                mSeCurR = 15;
                mSeStrokeWidth = 15;
                // 小球不显示
                mBall.setShow(false);
            }
        });
        return flattenAnim;
    }

    /**
     * 黄色圆缩小,绿色弧线出现,旋转从0->-120,从-120->-320(此时小球第一次击地,红色圆弧出现),从-320->-120(红色圆弧绕圈)
     * 抛出黄色小球,绿色弧线逐渐变成球,
     * 红色弧线绕圈,逐渐合并为圆环,
     * @return
     */
    protected ValueAnimator createSmallerAndRotateAnim() {
        mOval = new RectF(getCurX(),getCurY(),getCurX(),getCurY());
        mRedOval = new RectF(getCurX() - STANDARD_MAX_R + 5, getCurY() - STANDARD_MAX_R + 5,
                getCurX() + STANDARD_MAX_R - 5, getCurY() + STANDARD_MAX_R - 5);
        mAngle = 0;
        // 绿色弧线默认弧长GAP_ANGLE=240
        mGapGreenAngle = GAP_ANGLE;
        // 根据角度来调整圆大小的缩放比例
        final float rate1 = (STANDARD_MAX_R - STANDARD_MIN_R) / 120;
        final float rate2 = (STANDARD_MAX_R - STANDARD_MIN_R) / 120;
        mRedAngle = 60;
        mGapRedAngle = 0;
        // 红色弧线开口逐渐合并
        final float gapRate = 360 / 420f;

        // 0->300->420->720
        ValueAnimator smallerAnim = ValueAnimator.ofFloat(0, 720);
        smallerAnim.setDuration(5000);
        smallerAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurValue = (float) animation.getAnimatedValue();
                if (mCurValue <= 120) {
                    // 小黄球缩小
                    mState = 2;
                    mSeCurR = STANDARD_MAX_R - mCurValue * rate1;
                    mSeStrokeWidth = mSeCurR;
                    mDifValue = mCurValue * rate1 + STANDARD_MIN_R - mFiStrokeWidth / 2;
                    mOval.set(getCurX() - mDifValue, getCurY() - mDifValue, getCurX() + mDifValue, getCurY() + mDifValue);
                } else if (mCurValue < 300) {
                    // 小黄球停留
                } else {
                    mState = 3;

                    // 绿色圆弧缩小
                    mGapGreenAngle = (int) (GAP_ANGLE + mCurValue - 300);
                    if (mCurValue > 300 && mCurValue <= 420) {
                        mDifValue = STANDARD_MAX_R - (mCurValue - 300) * rate2 - mFiStrokeWidth / 2;
                        mOval.set(getCurX() - mDifValue, getCurY() - mDifValue, getCurX() + mDifValue, getCurY() + mDifValue);
                    }

                    // 红色弧线开始运动,逐渐合并为圆的状态,从开始到合并为圆弧0->420
                    mSeStrokeWidth = 12;
                    mGapRedAngle = (int) ((mCurValue - 300) * gapRate);
                    mRedAngle = (int) mCurValue - 240;
                }
                if (mCurValue > 300 && mCurValue <= 310) {
                    // 角度为300时, 抛出小黄球, 红色弧线开始运动
                    mState = 3;
                    if (!mBall.isShow()) {
                        mBall.setCurX(getCurX());
                        mBall.setCurY(getCurY());
                        mBall.setShow(true);
                        mBall.throwOut();
                    }
                }
                // 绿色圆弧绕圈
                mAngle = -(int) (float) animation.getAnimatedValue();
            }
        });
        smallerAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mState = 4;
                mFiStrokeWidth = 15;
                mSeStrokeWidth = 30;
                mSeCurR = 16f;
            }
        });
        return smallerAnim;
    }

    @Override
    public void startAnimation() {

    }

    @Override
    public void drawSelf(Canvas canvas) {
        switch (mState) {
            case 1:
                mPaint.setStrokeWidth(mFiStrokeWidth);
                mPaint.setColor(GREEN);
                canvas.drawCircle(getCurX(), getCurY(), mFiCurR - mFiStrokeWidth / 2, mPaint);

                mPaint.setStrokeWidth(mSeStrokeWidth);
                mPaint.setColor(Color.RED);
                canvas.drawCircle(getCurX(), getCurY(), mSeCurR - mSeStrokeWidth / 2, mPaint);
                break;
            case 2:
                mPaint.setColor(GREEN);
                mPaint.setStrokeWidth(mFiStrokeWidth);
                canvas.drawArc(mOval, mAngle, GAP_ANGLE, false, mPaint);

                mPaint.setStrokeWidth(mSeStrokeWidth);
                mPaint.setColor(YELLOW);
                canvas.drawCircle(getCurX(), getCurY(), mSeCurR - mSeStrokeWidth / 2, mPaint);
                break;
            case 3:
                // 绘制红色弧线
                mPaint.setStrokeWidth(mSeStrokeWidth);
                mPaint.setColor(RED);
                canvas.drawArc(mRedOval, mRedAngle, mGapRedAngle, false, mPaint);

                mPaint.setColor(GREEN);
                mPaint.setStrokeWidth(mFiStrokeWidth);
                canvas.drawArc(mOval, mAngle, mGapGreenAngle, false, mPaint);

                break;
            case 4:
                // 绘制红色圆弧
                mPaint.setStrokeWidth(mSeStrokeWidth);
                mPaint.setColor(GREEN);
                canvas.drawCircle(getCurX(), getCurY(), mSeCurR - mSeStrokeWidth / 2, mPaint);

                // 绘制绿色圆
                mPaint.setStrokeWidth(mFiStrokeWidth);
                mPaint.setColor(RED);
                canvas.drawCircle(getCurX(), getCurY(), mFiCurR - mFiStrokeWidth / 2, mPaint);

                break;
            default:
                break;
        }
        // 绘制小球
        mBall.drawSelf(canvas);
    }
}
