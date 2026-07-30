package io.github.pigerzhu.onelab.hook.applications;

import static io.github.pigerzhu.onelab.contract.SettingsKeys.KEY_SPLIT_VIEW_RATIO_OVERRIDES;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.pigerzhu.onelab.contract.SplitViewRatioOverrides;

/**
 * Applies OneLab's configured split ratio to Feishu's own Forseti two-pane
 * container. Feishu renders both panes inside one Activity, so AndroidX
 * Activity Embedding hooks do not participate in this layout.
 */
public final class LarkSplitRatioHook {
    private static final String TAG = "OneLab/LarkSplitRatio";
    private static final String DRAGGING_HELPER =
            "com.ss.android.lark.forseti.util.dragging.ForsetiColumnsDraggingHelper";

    private static final AtomicBoolean INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean LOGGED_FAILURE = new AtomicBoolean();

    private static volatile Float ratio;
    private static volatile boolean observerRegistered;

    private LarkSplitRatioHook() {
    }

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Context context = (Context) param.args[0];
                        initialize(context.getContentResolver(), lpparam.packageName);
                        installForClassLoader(context.getClassLoader());
                    }
                });
    }

    private static void installForClassLoader(ClassLoader classLoader) {
        if (classLoader == null || !INSTALLED.compareAndSet(false, true)) return;
        Class<?> helper = XposedHelpers.findClassIfExists(DRAGGING_HELPER, classLoader);
        if (helper == null) {
            INSTALLED.set(false);
            XposedBridge.log(TAG + ": Forseti helper unavailable");
            return;
        }

        XposedBridge.hookAllMethods(helper, "C", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Float current = ratio;
                if (current == null
                        || param.args.length != 2
                        || !(param.args[0] instanceof Integer)
                        || param.args[1] == null) {
                    return;
                }

                String widthMode = String.valueOf(param.args[1]);
                if (!isLeftPaneWidth(widthMode)) return;

                int totalWidth = (Integer) param.args[0];
                if (totalWidth > 0) {
                    param.setResult(Math.round(totalWidth * current));
                }
            }
        });
        XposedBridge.log(TAG + ": Forseti ratio hook installed");
    }

    private static boolean isLeftPaneWidth(String widthMode) {
        return "AVERAGE_LEVEL".equals(widthMode)
                || widthMode.endsWith("_LEFT_SIDE");
    }

    private static void initialize(ContentResolver resolver, String packageName) {
        refresh(resolver, packageName);
        if (observerRegistered) return;
        synchronized (LarkSplitRatioHook.class) {
            if (observerRegistered) return;
            resolver.registerContentObserver(
                    Settings.Global.getUriFor(KEY_SPLIT_VIEW_RATIO_OVERRIDES),
                    false,
                    new ContentObserver(new Handler(Looper.getMainLooper())) {
                        @Override
                        public void onChange(boolean selfChange) {
                            refresh(resolver, packageName);
                        }
                    });
            observerRegistered = true;
        }
    }

    private static void refresh(ContentResolver resolver, String packageName) {
        try {
            Map<String, Float> values = SplitViewRatioOverrides.parse(
                    Settings.Global.getString(resolver, KEY_SPLIT_VIEW_RATIO_OVERRIDES));
            ratio = values.get(packageName);
        } catch (Throwable throwable) {
            if (LOGGED_FAILURE.compareAndSet(false, true)) {
                XposedBridge.log(TAG + ": ratio setting update failed");
                XposedBridge.log(throwable);
            }
        }
    }
}
