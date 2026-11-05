package com.screenzen.app.data.model;

import android.graphics.drawable.Drawable;

/** 已安装的可启动应用，用于「拦截清单」页勾选。 */
public final class InstalledApp {
    public final String pkg;
    public final String label;
    public Drawable icon;
    public boolean blocked;

    public InstalledApp(String pkg, String label) {
        this.pkg = pkg;
        this.label = label;
    }
}
