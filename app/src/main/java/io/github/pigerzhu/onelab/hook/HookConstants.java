package io.github.pigerzhu.onelab.hook;

final class HookConstants {
    static final String TAG = "OneLab";

    static final String ONELAB_PACKAGE = "io.github.pigerzhu.onelab";
    static final String CAPTIVE_PORTAL_PACKAGE = "com.google.android.captiveportallogin";
    static final String GALLERY_PACKAGE = "com.sec.android.gallery3d";
    static final String BILIBILI_PACKAGE = "tv.danmaku.bili";
    static final String QQ_PACKAGE = "com.tencent.mobileqq";
    static final String XHS_PACKAGE = "com.xingin.xhs";
    static final String TONGCHENG_PACKAGE = "com.tongcheng.android";
    static final String XIAOMI_SHOP_PACKAGE = "com.xiaomi.shop";
    static final String GOS_PACKAGE = "com.samsung.android.game.gos";
    static final String SDHMS_PACKAGE = "com.sec.android.sdhms";
    static final String SYSTEM_SERVER_PACKAGE = "android";
    static final String SYSTEM_SERVER_SCOPE = "system";

    static boolean isSystemServerPackage(String packageName) {
        return SYSTEM_SERVER_PACKAGE.equals(packageName)
                || SYSTEM_SERVER_SCOPE.equals(packageName);
    }

    private HookConstants() {
    }
}
