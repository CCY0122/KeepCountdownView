package com.KeepCountdownView;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Created by ccy(17022) on 2019/2/26 下午2:10
 * 仿Keep休息倒计时控件
 */
public class KeepCountdownView extends View {

    //默认值
    public static final int ARC_COLOR = 0xff33cc66;
    public static final int NUM_COLOR = 0xff33cc66;
    public static final int ARC_STROKE_WIDTH_IN_DP = 5;
    public static final int NUM_TEXT_SIZE_IN_SP = 70;
    public static final float RADIUS_IN_DP = 90;
    public static final int INIT_DEGREE = 270;
    public static final int MAX_NUM = 20;
    public static final boolean IS_CW = false;
    public static final float PLUS_NUM_ANIM_DURATION = 0.8f;

    //属性相关

    /**
     * 圆弧颜色
     */
    private int arcColor;
    /**
     * 数字颜色
     */
    private int numColor;
    /**
     * 圆弧厚度 px
     */
    private float arcStrokeWidth;
    /**
     * 数字大小 px
     */
    private float numTextSize;
    /**
     * 圆弧半径
     */
    private float radius;
    /**
     * 倒计时开始角度
     */
    private int initDegree;
    /**
     * 是否顺时针倒计时
     */
    private boolean isCW;
    /**
     * 倒计时最大数值（时长）
     */
    private float maxNum;
    /**
     * 见{@link #plus(int)}
     */
    private float maxNumForText; //用于文字
    /**
     * 动态增加、减少倒计时的圆弧动画时长(s)
     */
    private float plusNumAnimDuration;

    //绘图相关

    private Paint arcPaint;
    private Paint numPaint;
    private float arcFraction = 1.0f;
    private float numFraction = 1.0f;
    private RectF arcRectF;
    private CountdownListener countdownListener;

    private AnimatorSet countdownAnim;
    private ValueAnimator numCountdownAnim;
    private ValueAnimator arcCountdownAnim;
    private ValueAnimator plusArcAnim;
    private boolean canceledByOut = false;


    public KeepCountdownView(Context context) {
        this(context, null);
    }

    public KeepCountdownView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeepCountdownView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.KeepCountdownView);
        arcColor = ta.getColor(R.styleable.KeepCountdownView_arcColor, ARC_COLOR);
        numColor = ta.getColor(R.styleable.KeepCountdownView_numColor, NUM_COLOR);
        arcStrokeWidth = ta.getDimension(R.styleable.KeepCountdownView_arcStrokeWidth,
                dp2px(ARC_STROKE_WIDTH_IN_DP));
        numTextSize = ta.getDimension(R.styleable.KeepCountdownView_numTextSize,
                sp2px(NUM_TEXT_SIZE_IN_SP));
        radius = ta.getDimension(R.styleable.KeepCountdownView_radius, dp2px(RADIUS_IN_DP));
        initDegree = ta.getInt(R.styleable.KeepCountdownView_initDegree, INIT_DEGREE);
        maxNum = maxNumForText = ta.getInt(R.styleable.KeepCountdownView_maxNum, MAX_NUM);
        isCW = ta.getBoolean(R.styleable.KeepCountdownView_isCW, IS_CW);
        plusNumAnimDuration = ta.getFloat(R.styleable.KeepCountdownView_plusNumAnimDuration,
                PLUS_NUM_ANIM_DURATION);
        ta.recycle();
        initDegree = initDegree % 360;

        initPaint();

        initData();
    }

    private void initPaint() {
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(arcColor);
        arcPaint.setStrokeWidth(arcStrokeWidth);
        arcPaint.setStyle(Paint.Style.STROKE);
//        arcPaint.setStrokeCap(Paint.Cap.ROUND); //圆角

        numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numPaint.setColor(numColor);
        numPaint.setTextSize(numTextSize);
        numPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void initData() {
        arcRectF = new RectF(-radius, -radius, radius, radius);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);

        //WRAP_CONTENT
        if (wMode != MeasureSpec.EXACTLY) {
            wSize = calculateMinWidth();
        }
        if (hMode != MeasureSpec.EXACTLY) {
            hSize = calculateMinWidth();
        }
        setMeasuredDimension(wSize, hSize);
    }

    /**
     * 计算控件最小边长
     *
     * @return
     */
    private int calculateMinWidth() {
        float minWidth = (arcStrokeWidth / 2.0f + radius) * 2;
        return (int) (minWidth + dp2px(4));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.translate(getMeasuredWidth() / 2.0f, getMeasuredHeight() / 2.0f);

        drawArc(canvas);

        drawNum(canvas);
    }

    private void drawNum(Canvas canvas) {
        canvas.save();

        Paint.FontMetrics metrics = numPaint.getFontMetrics();
        String currentNum = "" + (int) Math.ceil(getCurrentNumByFraction(numFraction,
                maxNumForText));
        canvas.drawText(currentNum
                , 0
                , 0 - (metrics.ascent + metrics.descent) / 2 //真正居中);
                , numPaint);

        canvas.restore();
    }

    private void drawArc(Canvas canvas) {
        canvas.save();

        float currentSweepDegree = getCurrentSweepDegree(arcFraction, 360);
        float startAngle, sweepAngle;
        if (isCW) {
            startAngle = initDegree - currentSweepDegree;
            sweepAngle = currentSweepDegree;
        } else {
            startAngle = initDegree;
            sweepAngle = currentSweepDegree;
        }
        canvas.drawArc(arcRectF
                , startAngle
                , sweepAngle
                , false
                , arcPaint);

        canvas.restore();
    }


    public void startCountDown() {
        if (countdownAnim != null && countdownAnim.isRunning()) {
            countdownAnim.cancel();
            countdownAnim = null;
        }
        countdownAnim = new AnimatorSet();
        countdownAnim.playTogether(getNumAnim(), getArcAnim());
        countdownAnim.setInterpolator(new LinearInterpolator());
        countdownAnim.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationCancel(Animator animation) {
                canceledByOut = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (canceledByOut) {
                    canceledByOut = false;
                    return;
                }
                if (countdownListener != null) {
                    countdownListener.onEnd();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (countdownListener != null) {
                    countdownListener.onStart();
                }
            }
        });
        countdownAnim.start();


    }

    private ValueAnimator getNumAnim() {
        if (numCountdownAnim != null) {
            numCountdownAnim.cancel();
            numCountdownAnim = null;
        }
        numCountdownAnim = ValueAnimator.ofFloat(numFraction, 0.0f);
        numCountdownAnim.setInterpolator(new LinearInterpolator());
        numCountdownAnim.setDuration((long) (getCurrentNumByFraction(numFraction, maxNumForText) * 1000));
        numCountdownAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                numFraction = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        return numCountdownAnim;
    }

    private ValueAnimator getArcAnim() {
        if (arcCountdownAnim != null) {
            arcCountdownAnim.cancel();
            arcCountdownAnim = null;
        }
        arcCountdownAnim = ValueAnimator.ofFloat(arcFraction, 0.0f);
        arcCountdownAnim.setInterpolator(new LinearInterpolator());
        arcCountdownAnim.setDuration((long) (getCurrentNumByFraction(arcFraction, maxNum) * 1000));
        arcCountdownAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                arcFraction = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });

        return arcCountdownAnim;
    }


    /**
     * 重置
     */
    public void reset() {
        if (countdownAnim != null) {
            countdownAnim.cancel();
            countdownAnim = null;
        }
        if (plusArcAnim != null) {
            plusArcAnim.cancel();
            plusArcAnim = null;
        }
        if (numCountdownAnim != null) {
            numCountdownAnim.cancel();
            numCountdownAnim = null;
        }
        if (arcCountdownAnim != null) {
            arcCountdownAnim.cancel();
            arcCountdownAnim = null;
        }
        arcFraction = 1.0f;
        numFraction = 1.0f;
        invalidate();
    }

    /**
     * 增加、减少倒计时时间
     * 1、当plusNum为正数，若plusNum + currentNum > maxNum，那么圆弧会增长到超过360度，所以一旦到360度后，应当重新根据新maxNum重置动画速度
     * 2、当plusNum为负数，若plusNum + currentNum < 0，即倒计时直接减到结束了
     * 3、计算时要考虑到圆弧增长动画时长plusNumDuration的影响。
     *
     * @param plusNum
     */
    public void plus(int plusNum) {
        if (countdownAnim != null) {
            countdownAnim.cancel();
        }
        if (numCountdownAnim != null) {
            numCountdownAnim.cancel();
        }
        if (arcCountdownAnim != null) {
            arcCountdownAnim.cancel();
        }

        if (plusArcAnim != null && plusArcAnim.isRunning()) {
            //正在增长动画变化中，不允许重叠调用
            return;
        }

        float gotoNum = plusNum + getCurrentNumByFraction(numFraction, maxNum);
        float gotoFraction = getCurrentFractionByNum(gotoNum, maxNum);

        //如果增加、减少的时间比圆弧增长动画还短，那么直接变过去
        if (Math.abs(plusNum) <= plusNumAnimDuration) {
            //情况1：直接减到了0，那么动画结束
            if (gotoNum <= 0) {
                numFraction = arcFraction = 0.0f;
                invalidate();
                if (countdownListener != null) {
                    countdownListener.onEnd();
                }
                return;
            }
            //情况2：加到了比原来maxNum还大，那么直接变回360度然后重新倒计时
            if (gotoNum > maxNum) {
                maxNum = maxNumForText = gotoNum;
                numFraction = arcFraction = 1.0f;
            } else {
                //情况3：正常变到对应位置
                numFraction = arcFraction = gotoFraction;
            }
            startCountDown();
            return;
        }

        //增加、减少的时间比圆弧增长动画长，可以做动画
        if (plusNum > 0) {
            //情况1：减掉圆弧增长动画时长后仍比原来maxNum还大，那么就是圆弧变回360度然后重新根据剩余时间进行倒计时
            if (gotoNum - plusNumAnimDuration > maxNum) {
                //数字直接更新到最新值并开始倒计时，圆弧则要动画到最新值
                maxNum = maxNumForText = gotoNum;
                gotoFraction = 1.0f;
                numFraction = 1.0f;
                getNumAnim().start();

                plusArcAnim = ValueAnimator.ofFloat(arcFraction, gotoFraction);
                plusArcAnim.setInterpolator(new LinearInterpolator());
                plusArcAnim.setDuration((long) (plusNumAnimDuration * 1000));
                plusArcAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        arcFraction = (float) animation.getAnimatedValue();
                        postInvalidate();
                    }
                });
                plusArcAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        canceledByOut = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (canceledByOut) {
                            canceledByOut = false;
                            return;
                        }
                        //文字是直接变到新的值然后动画倒计时的，但是圆弧需要花费plusNumAnimDuration的时间去做
                        //增长动画，等增长动画结束后（也就是增回到360度后），此时圆弧开始重新倒计时，其倒计时时间
                        //是减去plusNumAnimDuration所剩下的时间，这是区别于文字的，这是额外定义maxNumForText的原因
                        maxNum -= plusNumAnimDuration;
                        getArcAnim().start();
                    }
                });
                plusArcAnim.start();
            } else {
                //情况2：减掉增长动画时长后比原来maxNum小

                //数字直接更新到最新值并开始倒计时动画，圆弧则要动画到最新值
                numFraction = gotoFraction;
                getNumAnim().start();

                gotoFraction = getCurrentFractionByNum(gotoNum - plusNumAnimDuration, maxNum);
                plusArcAnim = ValueAnimator.ofFloat(arcFraction, gotoFraction);
                plusArcAnim.setInterpolator(new LinearInterpolator());
                plusArcAnim.setDuration((long) (plusNumAnimDuration * 1000));
                plusArcAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        arcFraction = (float) animation.getAnimatedValue();
                        postInvalidate();
                    }
                });
                plusArcAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        canceledByOut = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (canceledByOut) {
                            canceledByOut = false;
                            return;
                        }
                        getArcAnim().start();
                    }
                });
                plusArcAnim.start();
            }

        } else if (plusNum < 0) {
            //todo 思路同上。略
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        reset();
        super.onDetachedFromWindow();

    }


    /**
     * 根据当前倒计时进度比例和倒计时时长换算出当前倒计时值
     *
     * @param numFraction 当前倒计时进度比例
     * @param maxNum      倒计时最大值（倒计时时长）
     * @return 当前倒计时值(s ）
     */
    public float getCurrentNumByFraction(float numFraction, float maxNum) {
        return numFraction * maxNum;
    }

    /**
     * 根据当前倒计时值和倒计时时长换算出进度比例
     *
     * @param currentNum 当前倒计时值(s)
     * @param maxNum     倒计时最大值（倒计时时长）
     * @return 进度比例
     */
    public float getCurrentFractionByNum(float currentNum, float maxNum) {
        return currentNum / maxNum;
    }

    /**
     * 圆弧当前弧度计算
     *
     * @param arcFraction
     * @param maxDegree
     * @return
     */
    public float getCurrentSweepDegree(float arcFraction, int maxDegree) {
        return maxDegree * arcFraction;
    }

    // todo 各属性的getter、setter略。
    // todo 注意setter逻辑要根据具体情况调用invalidate、requestLayout、重置动画等操作。


    private float dp2px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private float sp2px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp,
                getResources().getDisplayMetrics());
    }

    public CountdownListener getCountdownListener() {
        return countdownListener;
    }

    public void setCountdownListener(CountdownListener countdownListener) {
        this.countdownListener = countdownListener;
    }


    /**
     * 监听接口
     */
    public interface CountdownListener {

        void onStart();

        void onEnd();

    }


}
