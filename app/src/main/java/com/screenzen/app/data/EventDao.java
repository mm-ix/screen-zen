package com.screenzen.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.screenzen.app.data.model.AppOpenStat;
import com.screenzen.app.data.model.OpenEvent;
import com.screenzen.app.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;

/** open_events 表的读写与聚合。 */
public final class EventDao {

    private final DbHelper helper;

    public EventDao(Context context) {
        this.helper = DbHelper.get(context);
    }

    public long insert(String pkg, String appLabel, String result) {
        return insert(pkg, appLabel, result, System.currentTimeMillis());
    }

    public long insert(String pkg, String appLabel, String result, long createdAt) {
        ContentValues cv = new ContentValues();
        cv.put("pkg", pkg);
        cv.put("app_label", appLabel);
        cv.put("result", result);
        cv.put("created_at", createdAt);
        return helper.getWritableDatabase().insert(DbHelper.T_EVENTS, null, cv);
    }

    /** 今天的某一种结局次数。 */
    public int countToday(String result) {
        return countBetween(TimeUtils.startOfToday(), Long.MAX_VALUE, result);
    }

    /** 累计的某一种结局次数。 */
    public int countTotal(String result) {
        return countBetween(0L, Long.MAX_VALUE, result);
    }

    public int countBetween(long fromInclusive, long toExclusive, String result) {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + DbHelper.T_EVENTS
                        + " WHERE result=? AND created_at>=? AND created_at<?",
                new String[]{result, String.valueOf(fromInclusive), String.valueOf(toExclusive)})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    /**
     * 按 App 聚合，一次查询拿到今日/累计的放行与放弃。
     * 按累计打开次数倒序，只出现在今日、累计为 0 的 App 不会漏（累计至少包含今日）。
     */
    public List<AppOpenStat> perAppStats() {
        long today = TimeUtils.startOfToday();
        String sql = "SELECT pkg,"
                + " MAX(app_label) AS label,"
                + " SUM(CASE WHEN result='release' AND created_at>=? THEN 1 ELSE 0 END) AS today_release,"
                + " SUM(CASE WHEN result='release' THEN 1 ELSE 0 END) AS total_release,"
                + " SUM(CASE WHEN result='abandon' AND created_at>=? THEN 1 ELSE 0 END) AS today_abandon,"
                + " SUM(CASE WHEN result='abandon' THEN 1 ELSE 0 END) AS total_abandon"
                + " FROM " + DbHelper.T_EVENTS
                + " GROUP BY pkg"
                + " ORDER BY total_release DESC, total_abandon DESC";

        String[] args = {String.valueOf(today), String.valueOf(today)};
        List<AppOpenStat> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery(sql, args)) {
            while (c.moveToNext()) {
                out.add(new AppOpenStat(
                        c.getString(0),
                        c.getString(1),
                        c.getInt(2),
                        c.getInt(3),
                        c.getInt(4),
                        c.getInt(5)));
            }
        }
        return out;
    }

    /** 导出用：全部原始记录，按时间正序。 */
    public List<OpenEvent> listAll() {
        List<OpenEvent> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DbHelper.T_EVENTS,
                new String[]{"id", "pkg", "app_label", "result", "created_at"},
                null, null, null, null, "created_at ASC")) {
            while (c.moveToNext()) {
                out.add(new OpenEvent(
                        c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4)));
            }
        }
        return out;
    }

    public int countAll() {
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + DbHelper.T_EVENTS, null)) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }
}
