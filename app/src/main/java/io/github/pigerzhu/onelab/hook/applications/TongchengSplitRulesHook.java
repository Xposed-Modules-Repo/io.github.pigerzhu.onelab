package io.github.pigerzhu.onelab.hook.applications;

import io.github.pigerzhu.onelab.hook.core.HookConstants;
import io.github.pigerzhu.onelab.hook.core.HookUtils;

import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.pigerzhu.onelab.contract.SettingsKeys;

/**
 * Keeps Tongcheng's native Activity Embedding rules while removing broad
 * always-expand exceptions from normal content pages.
 */
public final class TongchengSplitRulesHook {
    private static final String TAG = "OneLab/TongchengSplit";
    private static final String SPLIT_CONTROLLER =
            "androidx.window.embedding.SplitController";
    private static final String ACTIVITY_RULE =
            "androidx.window.embedding.ActivityRule";
    private static final String SAFE_FULLSCREEN_PREFIX =
            "com.tongcheng.android.module.recognition.activity.";

    private static final Object INSTALL_LOCK = new Object();
    private static final Set<ClassLoader> INSTALLED_LOADERS =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private TongchengSplitRulesHook() {
    }

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedBridge.hookAllMethods(Application.class, "attach", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (param.args == null || param.args.length == 0
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
            AtomicReference<Set<?>> originalRules = new AtomicReference<>();
            AtomicBoolean applying = new AtomicBoolean();
            Class<?> controllerClass = classLoader.loadClass(SPLIT_CONTROLLER);
            Class<?> activityRuleClass = classLoader.loadClass(ACTIVITY_RULE);
            Method setStaticRules =
                    controllerClass.getDeclaredMethod("setStaticSplitRules", Set.class);
            setStaticRules.setAccessible(true);

            XposedBridge.hookAllMethods(
                    controllerClass, "setStaticSplitRules", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param)
                                throws Throwable {
                            if (applying.get() || param.args == null
                                    || param.args.length != 1
                                    || !(param.args[0] instanceof Set)) {
                                return;
                            }
                            Set<?> incoming = new LinkedHashSet<>((Set<?>) param.args[0]);
                            originalRules.set(incoming);
                            if (enabled.get()) {
                                param.args[0] = rewriteRules(
                                        incoming, activityRuleClass);
                            }
                        }
                    });

            observeSetting(context, enabled, originalRules, applying, controllerClass,
                    activityRuleClass, setStaticRules);
            Log.i(TAG, "Installed native split-rule patch");
        } catch (Throwable t) {
            synchronized (INSTALL_LOCK) {
                INSTALLED_LOADERS.remove(classLoader);
            }
            XposedBridge.log(TAG + ": installation failed");
            XposedBridge.log(t);
        }
    }

    private static Set<?> rewriteRules(Set<?> rules, Class<?> activityRuleClass)
            throws Exception {
        LinkedHashSet<Object> rewritten = new LinkedHashSet<>();
        Method getFilters = activityRuleClass.getMethod("getFilters");
        Constructor<?> constructor =
                activityRuleClass.getConstructor(Set.class, boolean.class);

        for (Object rule : rules) {
            if (!activityRuleClass.isInstance(rule)) {
                rewritten.add(rule);
                continue;
            }
            LinkedHashSet<Object> safeFilters = new LinkedHashSet<>();
            for (Object filter : (Set<?>) getFilters.invoke(rule)) {
                Method getComponentName =
                        filter.getClass().getMethod("getComponentName");
                ComponentName component = (ComponentName) getComponentName.invoke(filter);
                if (component.getClassName().startsWith(SAFE_FULLSCREEN_PREFIX)) {
                    safeFilters.add(filter);
                }
            }
            if (!safeFilters.isEmpty()) {
                rewritten.add(constructor.newInstance(safeFilters, true));
            }
        }
        return rewritten;
    }

    private static void observeSetting(
            Context context,
            AtomicBoolean enabled,
            AtomicReference<Set<?>> originalRules,
            AtomicBoolean applying,
            Class<?> controllerClass,
            Class<?> activityRuleClass,
            Method setStaticRules) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                android.provider.Settings.Global.getUriFor(
                        SettingsKeys.KEY_ENABLE_TONGCHENG_SPLIT_RULES),
                false,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        boolean next = isEnabled(context);
                        enabled.set(next);
                        Set<?> original = originalRules.get();
                        if (original == null) return;
                        try {
                            Set<?> rules = next
                                    ? rewriteRules(original, activityRuleClass)
                                    : original;
                            Object instance =
                                    controllerClass.getMethod("getInstance").invoke(null);
                            applying.set(true);
                            setStaticRules.invoke(instance, rules);
                        } catch (Throwable t) {
                            XposedBridge.log(TAG + ": failed to refresh rules");
                            XposedBridge.log(t);
                        } finally {
                            applying.set(false);
                        }
                    }
                });
    }

    private static boolean isEnabled(Context context) {
        return HookUtils.globalEnabled(
                context.getContentResolver(),
                SettingsKeys.KEY_ENABLE_TONGCHENG_SPLIT_RULES,
                0);
    }
}
