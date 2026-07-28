package io.github.pigerzhu.onelab.hook;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Entry implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (HookConstants.CAPTIVE_PORTAL_PACKAGE.equals(lpparam.packageName)) {
            CaptivePortalHook.install(lpparam);
        } else if (HookConstants.GALLERY_PACKAGE.equals(lpparam.packageName)) {
            GalleryLabsHook.install(lpparam);
        } else if (HookConstants.BILIBILI_PACKAGE.equals(lpparam.packageName)) {
            BiliFoldGateHook.install(lpparam);
        } else if (HookConstants.QQ_PACKAGE.equals(lpparam.packageName)) {
            QqFoldLayoutHook.install(lpparam);
        } else if (HookConstants.XHS_PACKAGE.equals(lpparam.packageName)) {
            XhsFoldVideoHook.install(lpparam);
        } else if (HookConstants.TONGCHENG_PACKAGE.equals(lpparam.packageName)) {
            TongchengSplitRulesHook.install(lpparam);
        } else if (HookConstants.XIAOMI_SHOP_PACKAGE.equals(lpparam.packageName)) {
            XiaomiShopFoldHook.install(lpparam);
        } else if (HookConstants.GOS_PACKAGE.equals(lpparam.packageName)) {
            GosPermissionHook.install(lpparam);
        } else if (HookConstants.SDHMS_PACKAGE.equals(lpparam.packageName)) {
            SdhmsThermalHook.install(lpparam);
        } else if (HookConstants.isSystemServerPackage(lpparam.packageName)) {
            AspectRatioHook.install(lpparam);
            SamsungSplitRulesHook.install(lpparam);
            RefreshRateHook.install(lpparam);
        }
    }
}
