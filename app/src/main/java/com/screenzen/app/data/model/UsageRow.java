package com.screenzen.app.data.model;

/** 今日使用时长排行的一行。 */
public final class UsageRow {
    public final String pkg;
    public final String label;
    public final long millis;

    public UsageRow(String pkg, String label, long millis) {
        this.pkg = pkg;
        this.label = label;
        this.millis = millis;
    }
}
