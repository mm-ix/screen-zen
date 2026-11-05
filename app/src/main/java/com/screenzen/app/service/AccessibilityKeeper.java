package com.screenzen.app.service;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

/**
 * 保活：开机后如果无障碍被 ROM 关掉了，直接把开关写回去。
 *
 * 需要 WRITE_SECURE_SETTINGS，它是 signature|privileged 权限，正常安装拿不到，
 * 要用 adb 授一次（授完永久保留，重启和覆盖安装都不掉）：
 *
 *   adb shell pm grant com.screenzen.app android.permission.WRITE_SECURE_SETTINGS
 *
 * 没授权时所有写入都会抛 SecurityException，这里一律吞掉返回 false，
 * 引导页据此提示用户去授权。
 */
public final class AccessibilityKeeper {

    private AccessibilityKeeper() {
    }

    public static ComponentName component(Context context) {
        return new ComponentName(context, AppWatchAccessibilityService.class);
    }

    public static boolean hasWritePermission(Context context) {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 把本服务的组件追加进 ENABLED_ACCESSIBILITY_SERVICES 并打开总开关。
     * 只追加、不覆盖，避免把用户其他的无障碍服务（读屏等）弄没。
     *
     * @return true 表示写入成功
     */
    public static boolean restore(Context context) {
        if (!hasWritePermission(context)) {
            return false;
        }
        try {
            ContentResolver cr = context.getContentResolver();
            String component = component(context).flattenToString();
            String current = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (current == null) {
                current = "";
            }

            if (!current.contains(component)) {
                StringBuilder sb = new StringBuilder(current);
                if (current.length() > 0 && !current.endsWith(":")) {
                    sb.append(':');
                }
                sb.append(component);
                Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, sb.toString());
            }
            Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
            return true;
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
