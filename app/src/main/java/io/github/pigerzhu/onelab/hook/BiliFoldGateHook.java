package io.github.pigerzhu.onelab.hook;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
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

/** Lets Bilibili evaluate its own Fold large-screen gate without forcing any layout policy. */
final class BiliFoldGateHook {
    private static final String TAG = "OneLab/BiliFoldGate";
    private static final String KCONFIG_CLASS = "kntr.base.config.KConfig";
    private static final String CONFIG_METHOD = "config";
    private static final String LARGE_SCREEN_KEY = "dd_screen_adjust_xiaomi_864";

    private static final Object INSTALL_LOCK = new Object();
    private static final Set<ClassLoader> INSTALLED_LOADERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final AtomicBoolean LOGGED_REWRITE = new AtomicBoolean();

    private BiliFoldGateHook() {
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
            Class<?> configClass = classLoader.loadClass(KCONFIG_CLASS);
            XposedBridge.hookAllMethods(configClass, CONFIG_METHOD, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (!enabled.get() || param.args == null || param.args.length < 1
                            || !LARGE_SCREEN_KEY.equals(param.args[0])) {
                        return;
                    }
                    param.setResult("large");
                    if (LOGGED_REWRITE.compareAndSet(false, true)) {
                        Log.i(TAG, LARGE_SCREEN_KEY + " off -> large");
                    }
                }
            });
            Log.i(TAG, "Hooked KConfig.config for " + LARGE_SCREEN_KEY);
        } catch (Throwable t) {
            synchronized (INSTALL_LOCK) {
                INSTALLED_LOADERS.remove(classLoader);
            }
            XposedBridge.log(TAG + ": KConfig hook installation failed");
            XposedBridge.log(t);
        }
    }

    private static void observeEnabledSetting(Context context, AtomicBoolean enabled) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                Settings.Global.getUriFor(SettingsKeys.KEY_ENABLE_BILI_FOLD_GATE),
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
                context.getContentResolver(), SettingsKeys.KEY_ENABLE_BILI_FOLD_GATE, 0);
    }
}
