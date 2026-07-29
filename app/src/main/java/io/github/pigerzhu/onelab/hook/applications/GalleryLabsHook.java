package io.github.pigerzhu.onelab.hook.applications;

import io.github.pigerzhu.onelab.hook.core.HookConstants;
import io.github.pigerzhu.onelab.hook.core.HookUtils;

import android.content.ContentResolver;
import android.util.Log;

import io.github.pigerzhu.onelab.contract.SettingsKeys;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class GalleryLabsHook {
    private static final String GALLERY_POC_FEATURES =
            "com.samsung.android.gallery.support.utils.PocFeatures";

    private GalleryLabsHook() {
    }

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> pocFeaturesClass = XposedHelpers.findClass(GALLERY_POC_FEATURES, lpparam.classLoader);
            XposedBridge.hookAllMethods(pocFeaturesClass, "isEnabled", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length != 1 || param.args[0] == null) {
                        return;
                    }
                    ContentResolver resolver = resolverFromFeature(param.args[0]);
                    if (!HookUtils.globalEnabled(
                            resolver, SettingsKeys.KEY_ENABLE_GALLERY_DEV_LABS, 0)) {
                        return;
                    }
                    String name = String.valueOf(param.args[0]);
                    if ("GalleryLabs".equals(name) || "GalleryLabsDev".equals(name)) {
                        param.setResult(Boolean.TRUE);
                    }
                }
            });
            Log.i(HookConstants.TAG, "Hooked Gallery PocFeatures.isEnabled");
        } catch (Throwable t) {
            XposedBridge.log(HookConstants.TAG + ": gallery labs hook failed");
            XposedBridge.log(t);
        }
    }

    private static ContentResolver resolverFromFeature(Object feature) {
        Object appContext = HookUtils.invokeStaticNoArg(
                "com.samsung.android.gallery.support.utils.AppResources",
                "getAppContext",
                feature.getClass().getClassLoader()
        );
        return HookUtils.resolverFromContextObject(appContext);
    }
}
