package io.github.pigerzhu.onelab.hook;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.pigerzhu.onelab.contract.SettingsKeys;

/**
 * Restores QQ's native Fold classification when HMS Push presents a Huawei environment.
 */
final class QqFoldLayoutHook {
    private static final String TAG = "OneLab/QqFoldLayout";
    private static final String PAD_UTIL = "com.tencent.common.config.pad.PadUtil";
    private static final String DEVICE_TYPE = "com.tencent.common.config.pad.DeviceType";
    private static final String PAD_LAYOUT_UTIL = "com.tencent.mobileqq.pad.c";
    private static final int LARGE_SCREEN_DP = 600;

    private static final Object INSTALL_LOCK = new Object();
    private static final Set<ClassLoader> INSTALLED_LOADERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final AtomicBoolean LOGGED_ACTIVE = new AtomicBoolean();

    private QqFoldLayoutHook() {
    }

    static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args == null || param.args.length < 1
                        || !(param.args[0] instanceof Context)) {
                    return;
                }
                Context context = (Context) param.args[0];
                installForClassLoader(context, context.getClassLoader());
            }
        });
    }

    private static void installForClassLoader(Context context, ClassLoader classLoader) {
        if (classLoader == null) return;
        synchronized (INSTALL_LOCK) {
            if (!INSTALLED_LOADERS.add(classLoader)) return;
        }

        try {
            AtomicBoolean enabled = new AtomicBoolean(isEnabled(context));
            observeEnabledSetting(context, enabled);

            Class<?> deviceType = classLoader.loadClass(DEVICE_TYPE);
            Object fold = deviceType.getField("FOLD").get(null);
            hookDeviceType(classLoader, enabled, fold);
            hookExpandedState(classLoader, enabled);
            Log.i(TAG, "Installed guarded QQ Fold hooks");
        } catch (Throwable throwable) {
            synchronized (INSTALL_LOCK) {
                INSTALLED_LOADERS.remove(classLoader);
            }
            XposedBridge.log(TAG + ": installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void hookDeviceType(
            ClassLoader classLoader, AtomicBoolean enabled, Object fold) throws Throwable {
        Class<?> padUtil = classLoader.loadClass(PAD_UTIL);
        XposedBridge.hookAllMethods(padUtil, "a", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!enabled.get() || param.args == null || param.args.length != 1
                        || !(param.args[0] == null || param.args[0] instanceof Context)) {
                    return;
                }
                param.setResult(fold);
                logActive();
            }
        });
    }

    private static void hookExpandedState(
            ClassLoader classLoader, AtomicBoolean enabled) throws Throwable {
        Class<?> padLayoutUtil = classLoader.loadClass(PAD_LAYOUT_UTIL);
        XposedBridge.hookAllMethods(padLayoutUtil, "b", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (!enabled.get() || param.args == null || param.args.length != 1
                        || !(param.args[0] instanceof Activity)) {
                    return;
                }
                param.setResult(isExpanded((Activity) param.args[0]));
            }
        });
    }

    private static boolean isExpanded(Activity activity) {
        Configuration configuration = activity.getResources().getConfiguration();
        return configuration.screenWidthDp >= LARGE_SCREEN_DP
                && configuration.smallestScreenWidthDp >= LARGE_SCREEN_DP;
    }

    private static void observeEnabledSetting(Context context, AtomicBoolean enabled) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                Settings.Global.getUriFor(SettingsKeys.KEY_ENABLE_QQ_FOLD_LAYOUT),
                false,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        enabled.set(isEnabled(context));
                    }
                });
    }

    private static boolean isEnabled(Context context) {
        return HookUtils.globalEnabled(
                context.getContentResolver(), SettingsKeys.KEY_ENABLE_QQ_FOLD_LAYOUT, 0);
    }

    private static void logActive() {
        if (LOGGED_ACTIVE.compareAndSet(false, true)) {
            Log.i(TAG, "QQ native Fold classification enabled");
        }
    }
}
