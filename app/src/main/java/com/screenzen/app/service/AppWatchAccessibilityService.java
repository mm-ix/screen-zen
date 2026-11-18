package com.screenzen.app.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.screenzen.app.data.EventDao;
import com.screenzen.app.data.WatchConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 拦截核心：监听前台窗口变化，命中拦截清单就盖上呼吸遮罩。
 *
 * 这里刻意不把 AccessibilityServiceInfo.packageNames 收窄成拦截清单 —— 收窄之后
 * 就收不到「用户切走到别的 App」的事件，遮罩会一直挂在别的界面上撤不掉。
 */
public final class AppWatchAccessibilityService extends AccessibilityService {

    public static final String TAG = "ScreenZen";

    private OverlayController overlay;
    private EventDao eventDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    /** 当前这次「进入」被拦的包名。离开该 App 后清空，所以同一次进入只拦一次。 */
    private String currentPkg;
    private String currentLabel;

    private final OverlayController.Callback finishCallback = (pkg, label, result) -> {
        // 遮罩倒计时期间总开关被关掉的话，这次结果不再计入统计——关了就不该再产出记录
        if (WatchConfig.isEnabled()) {
            io.execute(() -> eventDao.insert(pkg, label, result));
        }
        if (OverlayController.RESULT_ABANDON.equals(result)) {
            // 「算了，不开了」-> 回桌面。currentPkg 不在这里清空，
            // 等桌面的窗口事件到达时统一走「前台已切走」那条路，避免中间出现空档重复拦截。
            performGlobalAction(GLOBAL_ACTION_HOME);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        eventDao = new EventDao(this);
        overlay = new OverlayController(this);
        WatchConfig.refresh(this);
        applyServiceInfo();
        Log.d(TAG, "accessibility connected, blocked=" + WatchConfig.blockedCount()
                + ", countdown=" + WatchConfig.countdownSeconds());
    }

    private void applyServiceInfo() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            return;
        }
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 0;
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.packageNames = null;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || overlay == null) {
            return;
        }
        // 主页一键开关关掉：撤掉可能残留的遮罩，之后一律放行、不记日志
        if (!WatchConfig.isEnabled()) {
            if (overlay.isShowing()) {
                overlay.dismiss(OverlayController.RESULT_SILENT);
            }
            currentPkg = null;
            currentLabel = null;
            return;
        }
        CharSequence pkgChars = event.getPackageName();
        if (pkgChars == null) {
            return;
        }
        String pkg = pkgChars.toString();

        // 自家包名（含遮罩自身）跳过，否则遮罩一出现就会触发自己
        if (getPackageName().equals(pkg)) {
            return;
        }
        if ("com.android.systemui".equals(pkg)) {
            return;
        }
        // Toast 是独立窗口，包名可能挂在被拦 App 上，但不该触发拦截
        CharSequence className = event.getClassName();
        if (className != null && "android.widget.Toast".equals(className.toString())) {
            return;
        }

        // 前台已经不在被拦的那个 App 里了：静默撤掉遮罩并重置会话（不记日志）
        if (currentPkg != null && !currentPkg.equals(pkg)) {
            if (overlay.isShowing()) {
                overlay.dismiss(OverlayController.RESULT_SILENT);
            }
            currentPkg = null;
            currentLabel = null;
        }

        if (overlay.isShowing() || currentPkg != null) {
            return;
        }
        if (!WatchConfig.isBlocked(pkg)) {
            return;
        }

        int seconds = WatchConfig.countdownSeconds();
        if (seconds <= 0) {
            // 0 = 不出现遮罩，直接放行，且不记任何日志
            return;
        }

        currentPkg = pkg;
        currentLabel = resolveLabel(pkg);
        overlay.show(pkg, currentLabel, seconds * 1000L, finishCallback);
        Log.d(TAG, "blocked " + pkg + " (" + currentLabel + ") for " + seconds + "s");
    }

    @Override
    public void onInterrupt() {
        if (overlay != null) {
            overlay.dismiss(OverlayController.RESULT_SILENT);
        }
        currentPkg = null;
        currentLabel = null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (overlay != null) {
            overlay.dismiss(OverlayController.RESULT_SILENT);
        }
        currentPkg = null;
        currentLabel = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        if (overlay != null) {
            overlay.dismiss(OverlayController.RESULT_SILENT);
        }
        super.onDestroy();
    }

    private String resolveLabel(String pkg) {
        if (android.text.TextUtils.isEmpty(pkg)) {
            return pkg;
        }
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(info);
            return label == null ? pkg : label.toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
