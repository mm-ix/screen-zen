package com.screenzen.app.data.model;

/** 柱状图的一根柱子：某一天的打开次数。 */
public final class DayCount {
    public final String label;
    public final int count;

    public DayCount(String label, int count) {
        this.label = label;
        this.count = count;
    }
}
