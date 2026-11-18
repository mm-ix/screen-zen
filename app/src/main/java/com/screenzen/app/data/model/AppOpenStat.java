package com.screenzen.app.data.model;

/** 按 App 聚合的计数：今日 + 累计，放行和放弃分开。 */
public final class AppOpenStat {
    public final String pkg;
    public final String appLabel;
    public final int todayRelease;
    public final int totalRelease;
    public final int todayAbandon;
    public final int totalAbandon;

    public AppOpenStat(String pkg, String appLabel,
                       int todayRelease, int totalRelease,
                       int todayAbandon, int totalAbandon) {
        this.pkg = pkg;
        this.appLabel = appLabel;
        this.todayRelease = todayRelease;
        this.totalRelease = totalRelease;
        this.todayAbandon = todayAbandon;
        this.totalAbandon = totalAbandon;
    }

    public String displayName() {
        return (appLabel == null || appLabel.isEmpty()) ? pkg : appLabel;
    }
}
