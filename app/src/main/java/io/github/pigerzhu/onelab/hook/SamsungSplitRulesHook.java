package io.github.pigerzhu.onelab.hook;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Bridges verified app-supplied fold rules into Samsung's split-activity repository. */
final class SamsungSplitRulesHook {
    private static final String TAG = "OneLab/SamsungSplitRules";
    private static final String CONTROLLER_CLASS =
            "com.android.server.wm.MultiTaskingController";
    private static final String BINDER_CLASS =
            "com.android.server.wm.MultiTaskingBinder";
    private static final String REPOSITORY_CLASS =
            "com.android.server.wm.SplitActivityInfoRepository";
    private static final String ACTIVITY_STARTER_CLASS =
            "com.android.server.wm.ActivityStarter";
    private static final Object LOCK = new Object();
    private static final Set<String> INJECTED_PACKAGES = new HashSet<>();

    private static volatile Object activeRepository;
    private static volatile boolean observersRegistered;

    private SamsungSplitRulesHook() {
    }

    static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> repositoryClass =
                    XposedHelpers.findClass(REPOSITORY_CLASS, lpparam.classLoader);
            XposedBridge.hookAllMethods(
                    repositoryClass,
                    "onPackageFeatureDataChanged",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            synchronized (LOCK) {
                                if (param.thisObject != activeRepository) return;
                                INJECTED_PACKAGES.clear();
                                applyLocked(param.thisObject);
                            }
                        }
                    });

            Class<?> controllerClass =
                    XposedHelpers.findClass(CONTROLLER_CLASS, lpparam.classLoader);
            XposedBridge.hookAllMethods(
                    controllerClass,
                    "getSplitActivityInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args == null || param.args.length < 3) return;
                            String packageName = String.valueOf(param.args[0]);
                            String targetActivity = String.valueOf(param.args[2]);
                            if (isForcedFullscreen(packageName, targetActivity)) {
                                param.setResult(null);
                            }
                        }
                    });
            XposedBridge.hookAllConstructors(controllerClass, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    initialize(param.thisObject);
                }
            });

            Class<?> activityStarterClass =
                    XposedHelpers.findClass(ACTIVITY_STARTER_CLASS, lpparam.classLoader);
            XposedBridge.hookAllMethods(
                    activityStarterClass,
                    "reparentActivitiesToActivityGroupIfNeeded",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args == null || param.args.length < 3) return;
                            Object targetRecord = param.args[2];
                            String packageName = activityRecordPackageName(targetRecord);
                            String activityName = activityRecordClassName(targetRecord);
                            if (isForcedFullscreen(packageName, activityName)) {
                                param.setResult(null);
                            }
                        }
                    });

            Class<?> binderClass =
                    XposedHelpers.findClass(BINDER_CLASS, lpparam.classLoader);
            XposedBridge.hookAllMethods(
                    binderClass,
                    "getSplitActivityAllowPackages",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            initializeFromBinder(param.thisObject);
                        }
                    });
            Log.i(TAG, "Installed Samsung split-rule bridge");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void initializeFromBinder(Object binder) {
        Object atm = HookUtils.findFieldValue(binder, "mAtm");
        Object controller = HookUtils.findFieldValue(atm, "mMultiTaskingController");
        if (controller != null) initialize(controller);
    }

    private static void initialize(Object controller) {
        try {
            Object repository = HookUtils.findFieldValue(
                    controller, "mSplitActivityInfoRepository");
            Object atm = HookUtils.findFieldValue(controller, "mAtm");
            Object context = HookUtils.findFieldValue(atm, "mContext");
            ContentResolver resolver = HookUtils.resolverFromContextObject(context);
            if (repository == null || resolver == null) {
                Log.w(TAG, "Samsung split repository is unavailable");
                return;
            }

            synchronized (LOCK) {
                activeRepository = repository;
                refreshEnabledStates(resolver);
                registerObserversLocked(resolver);
                applyLocked(repository);
            }
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + ": initialization failed");
            XposedBridge.log(throwable);
        }
    }

    private static void refreshEnabledStates(ContentResolver resolver) {
        for (SamsungSplitRuleCatalog.RuleSet ruleSet
                : SamsungSplitRuleCatalog.RULE_SETS) {
            ruleSet.enabled.set(HookUtils.globalEnabled(
                    resolver, ruleSet.settingKey, 0));
        }
    }

    private static void registerObserversLocked(ContentResolver resolver) {
        if (observersRegistered) return;
        Handler handler = new Handler(Looper.getMainLooper());
        for (SamsungSplitRuleCatalog.RuleSet ruleSet
                : SamsungSplitRuleCatalog.RULE_SETS) {
            resolver.registerContentObserver(
                    Settings.Global.getUriFor(ruleSet.settingKey),
                    false,
                    new ContentObserver(handler) {
                        @Override
                        public void onChange(boolean selfChange) {
                            ruleSet.enabled.set(HookUtils.globalEnabled(
                                    resolver, ruleSet.settingKey, 0));
                            synchronized (LOCK) {
                                applyLocked(activeRepository);
                            }
                        }
                    });
        }
        observersRegistered = true;
    }

    private static void applyLocked(Object repository) {
        if (repository == null) return;
        Map<?, ?> rules = repositoryMap(repository);
        if (rules == null) return;

        for (SamsungSplitRuleCatalog.RuleSet ruleSet
                : SamsungSplitRuleCatalog.RULE_SETS) {
            if (!ruleSet.enabled.get()) {
                if (INJECTED_PACKAGES.remove(ruleSet.packageName)) {
                    rules.remove(ruleSet.packageName);
                    Log.i(TAG, "Removed split rules for " + ruleSet.packageName);
                }
                continue;
            }
            if (rules.containsKey(ruleSet.packageName)) continue;

            try {
                for (SamsungSplitRuleCatalog.ActivityPair pair : ruleSet.pairs) {
                    XposedHelpers.callMethod(
                            repository,
                            "add",
                            ruleSet.packageName,
                            pair.source,
                            pair.target);
                }
                INJECTED_PACKAGES.add(ruleSet.packageName);
                Log.i(TAG, "Injected " + ruleSet.pairs.length
                        + " split rules for " + ruleSet.packageName);
            } catch (Throwable throwable) {
                rules.remove(ruleSet.packageName);
                INJECTED_PACKAGES.remove(ruleSet.packageName);
                XposedBridge.log(TAG + ": rule injection failed for "
                        + ruleSet.packageName);
                XposedBridge.log(throwable);
            }
        }
    }

    private static boolean isForcedFullscreen(String packageName, String activityName) {
        for (SamsungSplitRuleCatalog.RuleSet ruleSet
                : SamsungSplitRuleCatalog.RULE_SETS) {
            if (ruleSet.enabled.get()
                    && ruleSet.packageName.equals(packageName)
                    && ruleSet.fullscreenActivities.contains(activityName)) {
                return true;
            }
        }
        return false;
    }

    private static String activityRecordPackageName(Object activityRecord) {
        Object value = HookUtils.findFieldValue(activityRecord, "packageName");
        return value instanceof String ? (String) value : "";
    }

    private static String activityRecordClassName(Object activityRecord) {
        Object activityInfo = HookUtils.findFieldValue(activityRecord, "info");
        Object value = HookUtils.findFieldValue(activityInfo, "name");
        return value instanceof String ? (String) value : "";
    }

    @SuppressWarnings("unchecked")
    private static Map<?, ?> repositoryMap(Object repository) {
        Object value = HookUtils.findFieldValue(repository, "mRepository");
        return value instanceof Map ? (Map<?, ?>) value : null;
    }

}
