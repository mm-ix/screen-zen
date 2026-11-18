package com.screenzen.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 三张表：open_events（每次动作一条）、blocked_apps（拦截清单）、settings（键值）。
 * 记录量很小（每天几十条），不做自动清理，统计全靠时间范围查询。
 */
public final class DbHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "screenzen.db";
    public static final int DB_VERSION = 1;

    public static final String T_EVENTS = "open_events";
    public static final String T_BLOCKED = "blocked_apps";
    public static final String T_SETTINGS = "settings";

    private static volatile DbHelper sInstance;

    private DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static DbHelper get(Context context) {
        if (sInstance == null) {
            synchronized (DbHelper.class) {
                if (sInstance == null) {
                    sInstance = new DbHelper(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_EVENTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "pkg TEXT NOT NULL,"
                + "app_label TEXT,"
                + "result TEXT NOT NULL,"
                + "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX idx_events_created ON " + T_EVENTS + "(created_at)");
        db.execSQL("CREATE INDEX idx_events_pkg ON " + T_EVENTS + "(pkg)");

        db.execSQL("CREATE TABLE " + T_BLOCKED + " ("
                + "pkg TEXT PRIMARY KEY,"
                + "app_label TEXT,"
                + "enabled INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE " + T_SETTINGS + " ("
                + "key TEXT PRIMARY KEY,"
                + "value TEXT)");

        db.execSQL("INSERT INTO " + T_SETTINGS + " (key,value) VALUES (?,?)",
                new Object[]{SettingsDao.KEY_COUNTDOWN, String.valueOf(SettingsDao.DEFAULT_COUNTDOWN)});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 是初版，暂无迁移。
    }
}
