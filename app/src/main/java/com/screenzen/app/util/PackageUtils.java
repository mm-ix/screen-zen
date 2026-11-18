package com.screenzen.app.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import com.screenzen.app.data.model.InstalledApp;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 读取已安装的可启动应用，供「拦截清单」页勾选。 */
public final class PackageUtils {

    private PackageUtils() {
    }

    /** 要放在后台线程调用：加载上百个图标不便宜。 */
    public static List<InstalledApp> loadLaunchableApps(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> resolved;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolved = pm.queryIntentActivities(main, PackageManager.ResolveInfoFlags.of(0L));
        } else {
            resolved = pm.queryIntentActivities(main, 0);
        }

        String self = context.getPackageName();
        Map<String, InstalledApp> byPkg = new LinkedHashMap<>();
        for (ResolveInfo ri : resolved) {
            if (ri.activityInfo == null) {
                continue;
            }
            String pkg = ri.activityInfo.packageName;
            // 同一个包可能有多个启动入口，去重；自身不列
            if (pkg == null || self.equals(pkg) || byPkg.containsKey(pkg)) {
                continue;
            }
            CharSequence label = ri.loadLabel(pm);
            InstalledApp app = new InstalledApp(pkg, label == null ? pkg : label.toString());
            app.icon = ri.loadIcon(pm);
            byPkg.put(pkg, app);
        }

        List<InstalledApp> list = new ArrayList<>(byPkg.values());
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Collections.sort(list, new Comparator<InstalledApp>() {
            @Override
            public int compare(InstalledApp a, InstalledApp b) {
                return collator.compare(a.label, b.label);
            }
        });
        return list;
    }

    public static String labelOf(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(info);
            return label == null ? pkg : label.toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
