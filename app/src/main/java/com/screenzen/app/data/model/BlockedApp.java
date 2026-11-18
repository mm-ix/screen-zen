package com.screenzen.app.data.model;

/** 拦截清单里的一条。 */
public final class BlockedApp {
    public final String pkg;
    public final String appLabel;
    public final boolean enabled;

    public BlockedApp(String pkg, String appLabel, boolean enabled) {
        this.pkg = pkg;
        this.appLabel = appLabel;
        this.enabled = enabled;
    }
}
