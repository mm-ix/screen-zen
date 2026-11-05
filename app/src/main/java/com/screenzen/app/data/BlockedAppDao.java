package com.screenzen.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.screenzen.app.data.model.BlockedApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** blocked_apps 表的读写。不提供默认清单，用户勾选即入库。 */
public final class BlockedAppDao {

    private final DbHelper helper;

    public BlockedAppDao(Context context) {
        this.helper = DbHelper.get(context);
    }

    public List<BlockedApp> listAll() {
        List<BlockedApp> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DbHelper.T_BLOCKED,
                new String[]{"pkg", "app_label", "enabled"},
                null, null, null, null, "app_label ASC")) {
            while (c.moveToNext()) {
                out.add(new BlockedApp(c.getString(0), c.getString(1), c.getInt(2) == 1));
            }
        }
        return out;
    }

    /** 拦截服务的高频查询路径：一次拿到全部启用中的包名。 */
    public Set<String> enabledPackages() {
        Set<String> out = new HashSet<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DbHelper.T_BLOCKED, new String[]{"pkg"},
                "enabled=1", null, null, null, null)) {
            while (c.moveToNext()) {
                out.add(c.getString(0));
            }
        }
        return out.isEmpty() ? Collections.<String>emptySet() : out;
    }

    public boolean isEnabled(String pkg) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DbHelper.T_BLOCKED, new String[]{"enabled"},
                "pkg=?", new String[]{pkg}, null, null, null)) {
            return c.moveToFirst() && c.getInt(0) == 1;
        }
    }

    public int enabledCount() {
        return enabledPackages().size();
    }

    /** 勾选/取消勾选。取消勾选保留行，只把 enabled 置 0，这样应用名快照还在。 */
    public void setEnabled(String pkg, String appLabel, boolean enabled) {
        ContentValues cv = new ContentValues();
        cv.put("pkg", pkg);
        cv.put("app_label", appLabel);
        cv.put("enabled", enabled ? 1 : 0);
        helper.getWritableDatabase()
                .insertWithOnConflict(DbHelper.T_BLOCKED, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
