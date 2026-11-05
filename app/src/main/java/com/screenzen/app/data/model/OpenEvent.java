package com.screenzen.app.data.model;

/** 一次拦截事件的两种结局。 */
public final class OpenEvent {
    public static final String RESULT_RELEASE = "release";
    public static final String RESULT_ABANDON = "abandon";

    public final long id;
    public final String pkg;
    public final String appLabel;
    public final String result;
    public final long createdAt;

    public OpenEvent(long id, String pkg, String appLabel, String result, long createdAt) {
        this.id = id;
        this.pkg = pkg;
        this.appLabel = appLabel;
        this.result = result;
        this.createdAt = createdAt;
    }

    /** 导出 CSV 时用的中文结果名。 */
    public String resultLabel() {
        return RESULT_ABANDON.equals(result) ? "放弃" : "放行";
    }
}
