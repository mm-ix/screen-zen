package com.screenzen.app.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 本地时区的「天」边界计算。SQL 里对 created_at 做 /86400000 是按 UTC 切的，
 * 时区会错，所以天数一律在 Java 里算好再查询。
 */
public final class TimeUtils {

    private TimeUtils() {
    }

    public static long startOfToday() {
        return startOfDay(0);
    }

    /** daysAgo = 0 表示今天 0 点，1 表示昨天 0 点，以此类推。 */
    public static long startOfDay(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /** 柱状图 X 轴标签，例如 10/3。 */
    public static String dayLabel(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH);
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(millis));
    }

    /** 导出文件名的时间戳。 */
    public static String fileStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    /** 把毫秒时长转成「1小时23分」这种可读形式。 */
    public static String formatDuration(long millis) {
        long totalMinutes = millis / 60000L;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) {
            return hours + " 小时 " + minutes + " 分";
        }
        if (minutes > 0) {
            return minutes + " 分";
        }
        return "不足 1 分";
    }
}
