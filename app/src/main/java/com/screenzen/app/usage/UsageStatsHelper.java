package com.screenzen.app.usage;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;

import com.screenzen.app.data.model.UsageRow;
import com.screenzen.app.util.PackageUtils;
import com.screenzen.app.util.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 今日各 App 使用时长。
 *
 * PACKAGE_USAGE_STATS 是 appop 特殊权限，不是运行时权限：没授权时
 * queryAndAggregateUsageStats 不会抛异常，只会返回空 Map，所以调用方必须先查权限，
 * 否则界面看起来「今天没用过手机」而不是「没权限」。
 */
public final class UsageStatsHelper {

    /** 少于 1 分钟的忽略掉，列表才干净。 */
    private static final long MIN_VISIBLE_MILLIS = 60_000L;

    private UsageStatsHelper() {
    }

    public static List<UsageRow> todayUsage(Context context) {
        UsageStatsManager manager =
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        if (manager == null) {
            return Collections.emptyList();
        }
        long now = System.currentTimeMillis();
        long start = TimeUtils.startOfToday();
        Map<String, UsageStats> stats = manager.queryAndAggregateUsageStats(start, now);
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyList();
        }

        String self = context.getPackageName();
        List<UsageRow> rows = new ArrayList<>();
        for (UsageStats stat : stats.values()) {
            String pkg = stat.getPackageName();
            if (pkg == null || self.equals(pkg) || pkg.startsWith("com.android.systemui")) {
                continue;
            }
            long foreground = stat.getTotalTimeInForeground();
            if (foreground < MIN_VISIBLE_MILLIS) {
                continue;
            }
            rows.add(new UsageRow(pkg, PackageUtils.labelOf(context, pkg), foreground));
        }
        Collections.sort(rows, new Comparator<UsageRow>() {
            @Override
            public int compare(UsageRow a, UsageRow b) {
                return Long.compare(b.millis, a.millis);
            }
        });
        return rows;
    }
}
