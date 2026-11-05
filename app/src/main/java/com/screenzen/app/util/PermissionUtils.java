package com.screenzen.app.util;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import java.util.List;

/** 各项权限的状态检测与跳转。 */
public final class PermissionUtils {

    private PermissionUtils() {
    }

    public static boolean canDrawOverlays(Context context) {
        return Settings.canDrawOverlays(context);
    }

    /**
     * 用 AccessibilityManager 的实时列表判断，比读 Settings.Secure 字符串可靠
     * （后者在服务被 ROM 杀掉后可能还是旧值）。
     */
    public static boolean isAccessibilityEnabled(Context context) {
        AccessibilityManager am =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) {
            return false;
        }
        List<AccessibilityServiceInfo> enabled =
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        if (enabled == null) {
            return false;
        }
        String self = context.getPackageName();
        for (AccessibilityServiceInfo info : enabled) {
            ResolveInfo ri = info.getResolveInfo();
            if (ri != null && ri.serviceInfo != null && self.equals(ri.serviceInfo.packageName)) {
                return true;
            }
        }
        return false;
    }

    /** 使用情况访问是 appop 特殊权限，没授权时查询会静默返回空。 */
    public static boolean hasUsageAccess(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOps == null) {
            return false;
        }
        int mode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mode = appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        } else {
            mode = appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.getPackageName());
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static boolean isIgnoringBatteryOptimizations(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    // ---------------- 跳转 ----------------

    public static void openAccessibilitySettings(Context context) {
        startSafely(context, new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
    }

    public static void openOverlaySettings(Context context) {
        startSafely(context, new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName())));
    }

    public static void openUsageAccessSettings(Context context) {
        startSafely(context, new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
    }

    public static void openBatteryOptimizationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + context.getPackageName()));
        startSafely(context, intent);
    }

    public static void openAppDetailsSettings(Context context) {
        startSafely(context, new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.getPackageName())));
    }

    /**
     * 自启动没有公开 API，只能按厂商组件名挨个试。
     * @return true 表示成功跳到了某个厂商的自启动管理页
     */
    public static boolean openAutoStartSettings(Context context) {
        String[][] candidates = {
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"},
                {"com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"},
                {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"},
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                {"com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"},
        };
        for (String[] candidate : candidates) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(candidate[0], candidate[1]));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
                return true;
            } catch (Exception ignored) {
                // 该厂商组件不存在，试下一个
            }
        }
        openAppDetailsSettings(context);
        return false;
    }

    private static void startSafely(Context context, Intent intent) {
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            // 个别 ROM 缺少某个 settings 页面，退到应用详情页
            try {
                context.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.getPackageName()))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception ignored) {
                // 真的没有可跳的页面，只能让用户自己去设置里找
            }
        }
    }
}
