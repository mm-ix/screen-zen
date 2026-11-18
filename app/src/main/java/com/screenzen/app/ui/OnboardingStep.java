package com.screenzen.app.ui;

/** 引导页的一步。状态由 Activity 每次 onResume 重算后填进来。 */
public final class OnboardingStep {

    public static final int ID_ACCESSIBILITY = 1;
    public static final int ID_OVERLAY = 2;
    public static final int ID_USAGE = 3;
    public static final int ID_BATTERY = 4;
    public static final int ID_KEEPALIVE = 5;
    public static final int ID_AUTOSTART = 6;

    public final int id;
    public final String title;
    public final String desc;

    public boolean satisfied;
    public String statusText = "";
    public int statusColorRes;
    public String buttonText = "";
    /** 额外的说明文本，例如 adb 命令；null 表示整块隐藏。 */
    public String extraText;

    public OnboardingStep(int id, String title, String desc) {
        this.id = id;
        this.title = title;
        this.desc = desc;
    }
}
