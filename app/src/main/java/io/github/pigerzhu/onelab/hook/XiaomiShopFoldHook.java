package io.github.pigerzhu.onelab.hook;

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

/** Restores Xiaomi Shop's native Activity Embedding path on expanded Fold screens. */
final class XiaomiShopFoldHook {
    private static final String TAG = "OneLab/XiaomiShopFold";
    private static final String DEVICE_UTIL = "com.xiaomi.shop2.util.DeviceUtil";
    private static final int LARGE_SCREEN_DP = 600;

    private static final Object INSTALL_LOCK = new Object();
    private static final Set<ClassLoader> INSTALLED_LOADERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final AtomicBoolean LOGGED_ACTIVE = new AtomicBoolean();

    private XiaomiShopFoldHook() {
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

            Class<?> deviceUtil = classLoader.loadClass(DEVICE_UTIL);
            XposedBridge.hookAllMethods(deviceUtil, "isPadOrFold", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!enabled.get() || !isExpanded(context)) return;
                    param.setResult(true);
                    if (LOGGED_ACTIVE.compareAndSet(false, true)) {
                        Log.i(TAG, "Native expanded-screen layout enabled");
                    }
                }
            });
            Log.i(TAG, "Installed guarded Xiaomi Shop Fold hook");
        } catch (Throwable throwable) {
            synchronized (INSTALL_LOCK) {
                INSTALLED_LOADERS.remove(classLoader);
            }
            XposedBridge.log(TAG + ": installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static boolean isExpanded(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        return configuration.screenWidthDp >= LARGE_SCREEN_DP
                && configuration.smallestScreenWidthDp >= LARGE_SCREEN_DP;
    }

    private static void observeEnabledSetting(Context context, AtomicBoolean enabled) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                Settings.Global.getUriFor(SettingsKeys.KEY_ENABLE_XIAOMI_SHOP_FOLD),
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
                context.getContentResolver(),
                SettingsKeys.KEY_ENABLE_XIAOMI_SHOP_FOLD,
                0);
    }
}
