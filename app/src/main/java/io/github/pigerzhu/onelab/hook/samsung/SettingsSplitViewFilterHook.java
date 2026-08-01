package io.github.pigerzhu.onelab.hook.samsung;

import android.content.pm.ApplicationInfo;

import java.util.ArrayList;
import java.util.List;

import io.github.pigerzhu.onelab.hook.core.HookConstants;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class SettingsSplitViewFilterHook {
    private static final String USEFUL_FEATURE_UTILS =
            "com.samsung.android.settings.usefulfeature.UsefulfeatureUtils";

    private SettingsSplitViewFilterHook() {
    }

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> utils = XposedHelpers.findClass(USEFUL_FEATURE_UTILS, lpparam.classLoader);
            XposedBridge.hookAllMethods(
                    utils,
                    "getSplitActivityApplications",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object result = param.getResult();
                            if (!(result instanceof List<?>)) return;

                            List<?> original = (List<?>) result;
                            ArrayList<Object> filtered = new ArrayList<>(original.size());
                            for (Object item : original) {
                                if (item instanceof ApplicationInfo
                                        && HookConstants.TAPTAP_PACKAGE.equals(
                                        ((ApplicationInfo) item).packageName)) {
                                    continue;
                                }
                                filtered.add(item);
                            }
                            if (filtered.size() != original.size()) {
                                param.setResult(filtered);
                            }
                        }
                    });
            XposedBridge.log(HookConstants.TAG + ": installed Settings split-view filter");
        } catch (Throwable throwable) {
            XposedBridge.log(HookConstants.TAG + ": Settings split-view filter failed");
            XposedBridge.log(throwable);
        }
    }
}
