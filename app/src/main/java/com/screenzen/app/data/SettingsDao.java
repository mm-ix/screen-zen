package com.screenzen.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** settings 表的键值读写。 */
public final class SettingsDao {

    public static final String KEY_COUNTDOWN = "countdown_seconds";
    public static final int DEFAULT_COUNTDOWN = 10;
    public static final int MIN_COUNTDOWN = 0;
    public static final int MAX_COUNTDOWN = 60;

    /** 主页那个一键开关：1 = 拦截生效，0 = 全局放行。老库没有这条键时按默认开启。 */
    public static final String KEY_ENABLED = "block_enabled";
    public static final int DEFAULT_ENABLED = 1;

    private final DbHelper helper;

    public SettingsDao(Context context) {
        this.helper = DbHelper.get(context);
    }

    /** 倒计时秒数，范围 0~60。0 表示不出现遮罩、等同关闭拦截。 */
    public int getCountdownSeconds() {
        return clamp(getInt(KEY_COUNTDOWN, DEFAULT_COUNTDOWN));
    }

    public void setCountdownSeconds(int seconds) {
        putInt(KEY_COUNTDOWN, clamp(seconds));
    }

    public static int clamp(int seconds) {
        if (seconds < MIN_COUNTDOWN) return MIN_COUNTDOWN;
        if (seconds > MAX_COUNTDOWN) return MAX_COUNTDOWN;
        return seconds;
    }

    /** 拦截总开关。默认开启，兼容没有该键的旧库。 */
    public boolean isEnabled() {
        return getInt(KEY_ENABLED, DEFAULT_ENABLED) != 0;
    }

    public void setEnabled(boolean enabled) {
        putInt(KEY_ENABLED, enabled ? 1 : 0);
    }

    public int getInt(String key, int fallback) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DbHelper.T_SETTINGS, new String[]{"value"},
                "key=?", new String[]{key}, null, null, null)) {
            if (c.moveToFirst()) {
                String raw = c.getString(0);
                if (raw != null) {
                    try {
                        return Integer.parseInt(raw.trim());
                    } catch (NumberFormatException ignored) {
                        return fallback;
                    }
                }
            }
        }
        return fallback;
    }

    public void putInt(String key, int value) {
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", String.valueOf(value));
        helper.getWritableDatabase()
                .insertWithOnConflict(DbHelper.T_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
