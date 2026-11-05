package com.screenzen.app.data;

import android.content.Context;

import java.util.Collections;
import java.util.Set;

/**
 * 拦截服务在 onAccessibilityEvent 里要判断「这个包名拦不拦」，一秒内可能来好几次事件，
 * 不能每次都查库。这里放内存快照，清单或倒计时一改就 refresh 一次。
 */
public final class WatchConfig {

    private static volatile Set<String> sBlocked = Collections.emptySet();
    private static volatile int sCountdownSeconds = SettingsDao.DEFAULT_COUNTDOWN;
    private static volatile boolean sEnabled = SettingsDao.DEFAULT_ENABLED != 0;

    private WatchConfig() {
    }

    public static void refresh(Context context) {
        Context app = context.getApplicationContext();
        SettingsDao settings = new SettingsDao(app);
        sBlocked = new BlockedAppDao(app).enabledPackages();
        sCountdownSeconds = settings.getCountdownSeconds();
        sEnabled = settings.isEnabled();
    }

    public static boolean isBlocked(String pkg) {
        return sBlocked.contains(pkg);
    }

    /** 主页一键开关：false 时全局放行，任何 App 都不拦。 */
    public static boolean isEnabled() {
        return sEnabled;
    }

    /** 倒计时秒数；0 表示不拦截。 */
    public static int countdownSeconds() {
        return sCountdownSeconds;
    }

    public static int blockedCount() {
        return sBlocked.size();
    }
}
