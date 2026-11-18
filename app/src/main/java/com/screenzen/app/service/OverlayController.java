package com.screenzen.app.service;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.screenzen.app.R;
import com.screenzen.app.ui.widget.BreathCircleView;

/**
 * 全屏呼吸遮罩。生命周期由 AppWatchAccessibilityService 持有，任何时候都必须能干净地撤掉，
 * 否则会留下一层盖住整个屏幕、点不掉的浮窗。
 */
public final class OverlayController {

    public static final String RESULT_RELEASE = "release";
    public static final String RESULT_ABANDON = "abandon";
    /** 用户切到别的 App / 服务被中断时的静默撤销，不记日志。 */
    public static final String RESULT_SILENT = "silent";

    public interface Callback {
        void onFinished(String pkg, String appLabel, String result);
    }

    private final Context context;
    private final WindowManager windowManager;

    private View view;
    private TextView countdownText;
    private CountDownTimer timer;
    private BreathCircleView breathCircle;

    private String pkg;
    private String appLabel;
    private Callback callback;
    private boolean showing;

    public OverlayController(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public boolean isShowing() {
        return showing;
    }

    public void show(String pkg, String appLabel, long durationMillis, Callback callback) {
        if (showing) {
            return;
        }
        this.pkg = pkg;
        this.appLabel = appLabel;
        this.callback = callback;

        LayoutInflater inflater = LayoutInflater.from(
                new ContextThemeWrapper(context, R.style.Theme_ScreenZen));
        View root = inflater.inflate(R.layout.overlay_breath, null);

        countdownText = root.findViewById(R.id.countdown_text);
        TextView appName = root.findViewById(R.id.app_name_text);
        appName.setText(appLabel == null || appLabel.isEmpty() ? pkg : appLabel);
        // 「算了，不开了」一直显示，不设延迟
        root.findViewById(R.id.abandon_button).setOnClickListener(v -> dismiss(RESULT_ABANDON));

        windowManager.addView(root, buildParams());
        view = root;
        showing = true;

        startBreathing(root.findViewById(R.id.breath_circle));
        startCountdown(durationMillis);
    }

    /** 到点自动放行、点按钮放弃、或静默撤销。 */
    public void dismiss(String result) {
        if (!showing) {
            return;
        }
        String finishedPkg = pkg;
        String finishedLabel = appLabel;
        Callback finishedCallback = callback;
        teardown();
        if (finishedCallback != null && !RESULT_SILENT.equals(result)) {
            finishedCallback.onFinished(finishedPkg, finishedLabel, result);
        }
    }

    private void teardown() {
        showing = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (breathCircle != null) {
            breathCircle.stopBreathing();
            breathCircle = null;
        }
        if (view != null) {
            try {
                windowManager.removeViewImmediate(view);
            } catch (IllegalArgumentException ignored) {
                // 已经被系统摘掉了
            }
            view = null;
        }
        countdownText = null;
        callback = null;
    }

    private WindowManager.LayoutParams buildParams() {
        // 必须能收到点击，所以不能加 FLAG_NOT_TOUCHABLE；
        // 加 FLAG_NOT_FOCUSABLE 让返回键仍然归系统管，避免把用户锁死在遮罩里。
        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 盖住刘海/挖孔区域，否则顶部会漏一条
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        }
        return lp;
    }

    private void startBreathing(BreathCircleView circle) {
        breathCircle = circle;
        breathCircle.startBreathing();
    }

    private void startCountdown(long durationMillis) {
        timer = new CountDownTimer(durationMillis, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (countdownText != null) {
                    long seconds = Math.max(1L, Math.round(millisUntilFinished / 1000.0));
                    countdownText.setText(String.valueOf(seconds));
                }
            }

            @Override
            public void onFinish() {
                // 数到 0 自动放行，无需再点
                dismiss(RESULT_RELEASE);
            }
        };
        timer.start();
    }
}
