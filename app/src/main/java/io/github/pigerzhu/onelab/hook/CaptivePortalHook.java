package io.github.pigerzhu.onelab.hook;

import android.content.ContentResolver;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import io.github.pigerzhu.onelab.contract.SettingsKeys;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

final class CaptivePortalHook {
    private static final String CAPTIVE_PORTAL_ACTIVITY =
            "com.android.captiveportallogin.CaptivePortalLoginActivity";
    private static final Set<Object> delayedActivities =
            Collections.newSetFromMap(new WeakHashMap<>());

    private CaptivePortalHook() {
    }

    static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> activityClass = XposedHelpers.findClass(CAPTIVE_PORTAL_ACTIVITY, lpparam.classLoader);
            XposedBridge.hookAllMethods(activityClass, "handleCapabilitiesChanged", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 2
                            || !(param.args[1] instanceof NetworkCapabilities)) {
                        return;
                    }

                    ContentResolver resolver = (ContentResolver) XposedHelpers.callMethod(
                            param.thisObject, "getContentResolver");
                    if (!isKeeperEnabled(resolver)) {
                        return;
                    }

                    NetworkCapabilities capabilities = (NetworkCapabilities) param.args[1];
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        param.setResult(null);
                        Log.i(HookConstants.TAG, "Suppressed CaptivePortalLogin validated-network auto dismiss");
                    }
                }
            });
            Log.i(HookConstants.TAG, "Hooked CaptivePortalLoginActivity.handleCapabilitiesChanged");

            XposedBridge.hookAllMethods(activityClass, "done", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length != 1 || !isDismissed(param.args[0])) {
                        return;
                    }

                    Object activity = param.thisObject;
                    ContentResolver resolver = (ContentResolver) XposedHelpers.callMethod(activity, "getContentResolver");
                    if (!isKeeperEnabled(resolver)) {
                        return;
                    }

                    long delayMs = getDelayMs(resolver);
                    if (delayMs <= 0L || delayedActivities.contains(activity)) {
                        return;
                    }

                    delayedActivities.add(activity);
                    param.setResult(Boolean.FALSE);
                    Method method = (Method) param.method;
                    Object result = param.args[0];

                    if (delayMs == Long.MAX_VALUE) {
                        Log.i(HookConstants.TAG, "Suppressed CaptivePortalLogin auto dismiss");
                        return;
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            XposedBridge.invokeOriginalMethod(method, activity, new Object[]{result});
                        } catch (Throwable t) {
                            Log.w(HookConstants.TAG, "Delayed captive portal dismiss failed", t);
                        } finally {
                            delayedActivities.remove(activity);
                        }
                    }, delayMs);

                    Log.i(HookConstants.TAG, "Delayed CaptivePortalLogin auto dismiss by " + delayMs + " ms");
                }
            });
            Log.i(HookConstants.TAG, "Hooked CaptivePortalLoginActivity.done");
        } catch (Throwable t) {
            XposedBridge.log(HookConstants.TAG + ": captive portal hook failed");
            XposedBridge.log(t);
        }
    }

    private static boolean isKeeperEnabled(ContentResolver resolver) {
        return HookUtils.globalEnabled(resolver, SettingsKeys.KEY_ENABLE_CAPTIVE_KEEPER, 0);
    }

    private static long getDelayMs(ContentResolver resolver) {
        return Settings.Global.getLong(
                resolver,
                SettingsKeys.KEY_CAPTIVE_DELAY_MS,
                SettingsKeys.DEFAULT_CAPTIVE_DELAY_MS
        );
    }

    private static boolean isDismissed(Object result) {
        return result != null && "DISMISSED".equals(String.valueOf(result));
    }
}
