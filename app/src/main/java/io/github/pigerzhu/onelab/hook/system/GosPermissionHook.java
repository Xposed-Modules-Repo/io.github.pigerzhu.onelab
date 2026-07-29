package io.github.pigerzhu.onelab.hook.system;

import io.github.pigerzhu.onelab.hook.core.HookConstants;
import io.github.pigerzhu.onelab.hook.core.HookUtils;

import android.util.Log;
import android.util.Pair;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class GosPermissionHook {
    private static final String GOS_ENDPOINT_BASE = "com.samsung.android.game.gos.endpoint.a";

    private GosPermissionHook() {
    }

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> endpointBaseClass = XposedHelpers.findClass(GOS_ENDPOINT_BASE, lpparam.classLoader);
            XposedBridge.hookAllMethods(endpointBaseClass, "b", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (HookUtils.packageForCallingUid(param.thisObject, HookConstants.ONELAB_PACKAGE)) {
                        param.setResult(new Pair<>(Boolean.TRUE, HookConstants.ONELAB_PACKAGE));
                        Log.i(HookConstants.TAG, "Allowed OneLab to call GOS service");
                    }
                }
            });
            Log.i(HookConstants.TAG, "Hooked GOS endpoint permission check");
        } catch (Throwable t) {
            XposedBridge.log(HookConstants.TAG + ": GOS permission hook failed");
            XposedBridge.log(t);
        }
    }
}
