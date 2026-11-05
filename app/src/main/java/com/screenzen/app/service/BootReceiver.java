package com.screenzen.app.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 开机后复查无障碍开关。安卓本身会持久化无障碍设置，这层是给会主动关掉无障碍
 * 的 ROM（国产系统居多）兜底 —— 有了 WRITE_SECURE_SETTINGS 就能自己写回去。
 */
public final class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            return;
        }
        boolean restored = AccessibilityKeeper.restore(context);
        Log.d(AppWatchAccessibilityService.TAG, "boot: restore accessibility -> " + restored);
    }
}
