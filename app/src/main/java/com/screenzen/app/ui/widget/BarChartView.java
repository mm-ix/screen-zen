package com.screenzen.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.screenzen.app.R;
import com.screenzen.app.data.model.DayCount;

import java.util.ArrayList;
import java.util.List;

/** 手写的柱状图，避免为了几张图引入第三方图表库。 */
public final class BarChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private List<DayCount> data = new ArrayList<>();
    private float progress = 1f;
    private ValueAnimator animator;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        barPaint.setColor(context.getColor(R.color.chart_bar));
        trackPaint.setColor(context.getColor(R.color.chart_bar_track));
        textPaint.setColor(context.getColor(R.color.chart_text));
        textPaint.setTextSize(11f * density);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void setData(List<DayCount> newData) {
        data = newData == null ? new ArrayList<DayCount>() : newData;
        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600L);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float textHeight = textPaint.getTextSize();
        float chartTop = textHeight * 1.9f;
        float chartBottom = height - textHeight * 1.9f;
        float chartHeight = chartBottom - chartTop;
        if (chartHeight <= 0) {
            return;
        }

        if (data.isEmpty()) {
            canvas.drawText("—", width / 2f, chartBottom, textPaint);
            return;
        }

        int max = 1;
        for (DayCount day : data) {
            max = Math.max(max, day.count);
        }

        float density = getResources().getDisplayMetrics().density;
        int count = data.size();
        float slot = (float) width / count;
        float barWidth = Math.min(slot * 0.5f, 34f * density);
        float radius = barWidth / 2f;

        for (int i = 0; i < count; i++) {
            DayCount day = data.get(i);
            float centerX = slot * i + slot / 2f;

            rect.set(centerX - barWidth / 2f, chartTop, centerX + barWidth / 2f, chartBottom);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            float ratio = (float) day.count / max;
            float barHeight = chartHeight * ratio * progress;
            if (barHeight > 0f) {
                rect.set(centerX - barWidth / 2f, chartBottom - barHeight,
                        centerX + barWidth / 2f, chartBottom);
                canvas.drawRoundRect(rect, radius, radius, barPaint);
            }

            canvas.drawText(String.valueOf(day.count),
                    centerX, chartBottom - barHeight - textHeight * 0.4f, textPaint);
            canvas.drawText(day.label, centerX, height - textHeight * 0.5f, textPaint);
        }
    }
}
