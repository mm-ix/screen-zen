package com.screenzen.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import com.screenzen.app.R;

/**
 * 呼吸的圆。
 *
 * 用 Canvas 直接画，而不是拿一个 oval 形状的 XML drawable 当背景 ——
 * 后者在遮罩这种场景下会被渲染成带直边的多边形，看着像方不方圆不圆的八边形。
 * 动画也直接改半径，比缩放整个 View 更接近「呼吸」的观感。
 */
public final class BreathCircleView extends View {

    private static final float MIN_FACTOR = 0.78f;
    private static final float MAX_FACTOR = 1.00f;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float factor = MAX_FACTOR;
    private ValueAnimator animator;

    public BreathCircleView(Context context) {
        this(context, null);
    }

    public BreathCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(context.getColor(R.color.overlay_circle));
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2f * density);
        ringPaint.setColor(context.getColor(R.color.overlay_circle_ring));
    }

    public void startBreathing() {
        stopBreathing();
        animator = ValueAnimator.ofFloat(MIN_FACTOR, MAX_FACTOR);
        animator.setDuration(1800L);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                factor = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    public void stopBreathing() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopBreathing();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float radius = Math.min(centerX, centerY) * factor;
        if (radius <= 0f) {
            return;
        }
        canvas.drawCircle(centerX, centerY, radius, fillPaint);
        canvas.drawCircle(centerX, centerY, radius, ringPaint);
    }
}
