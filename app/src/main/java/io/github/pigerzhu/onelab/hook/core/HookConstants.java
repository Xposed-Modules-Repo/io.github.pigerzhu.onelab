package io.github.pigerzhu.onelab.hook.core;


public final class HookConstants {
    public static final String TAG = "OneLab";

    public static final String ONELAB_PACKAGE = "io.github.pigerzhu.onelab";
    public static final String CAPTIVE_PORTAL_PACKAGE = "com.google.android.captiveportallogin";
    public static final String GALLERY_PACKAGE = "com.sec.android.gallery3d";
    public static final String BILIBILI_PACKAGE = "tv.danmaku.bili";
    public static final String QQ_PACKAGE = "com.tencent.mobileqq";
    public static final String XHS_PACKAGE = "com.xingin.xhs";
    public static final String TONGCHENG_PACKAGE = "com.tongcheng.android";
    public static final String XIAOMI_SHOP_PACKAGE = "com.xiaomi.shop";
    public static final String MEITUAN_PACKAGE = "com.sankuai.meituan";
    public static final String GOS_PACKAGE = "com.samsung.android.game.gos";
    public static final String SDHMS_PACKAGE = "com.sec.android.sdhms";
    public static final String SYSTEM_SERVER_PACKAGE = "android";
    public static final String SYSTEM_SERVER_SCOPE = "system";

    public static boolean isSystemServerPackage(String packageName) {
        return SYSTEM_SERVER_PACKAGE.equals(packageName)
                || SYSTEM_SERVER_SCOPE.equals(packageName);
    }

    private HookConstants() {
    }
}
