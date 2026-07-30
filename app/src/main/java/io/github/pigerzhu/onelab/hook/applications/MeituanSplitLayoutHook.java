package io.github.pigerzhu.onelab.hook.applications;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Display;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.pigerzhu.onelab.contract.SettingsKeys;
import io.github.pigerzhu.onelab.hook.core.HookUtils;

/**
 * Preserves Meituan's full-canvas layout scale inside Samsung's half-width
 * split pane. The affected pages otherwise size components for the unfolded
 * display and render them at that scale inside the narrower pane.
 */
public final class MeituanSplitLayoutHook {
    private static final String TAG = "OneLab/MeituanSplitLayout";
    private static final int LARGE_SCREEN_DP = 600;
    private static final float HALF_WIDTH_RATIO = 1.75f;
    private static final Set<String> SCALED_ACTIVITIES = Set.of(
            "com.meituan.android.hotel.reuse.htchomepage.HtcHomepageActivity",
            "com.sankuai.waimai.business.restaurant.poicontainer.WMRestaurantActivity"
    );
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();
    private static final AtomicBoolean LOGGED_ACTIVE = new AtomicBoolean();

    private MeituanSplitLayoutHook() {
    }

    public static void install(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!INSTALLED.compareAndSet(false, true)) return;

        AtomicBoolean enabled = new AtomicBoolean();
        XposedHelpers.findAndHookMethod(
                ContextThemeWrapper.class,
                "attachBaseContext",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.args == null || param.args.length != 1
                                || !(param.args[0] instanceof Context)
                                || !SCALED_ACTIVITIES.contains(
                                param.thisObject.getClass().getName())) {
                            return;
                        }
                        Context base = (Context) param.args[0];
                        if (!enabled.get() || !isHalfWidthExpandedPane(base)) return;

                        DisplayMetrics metrics =
                                base.getResources().getDisplayMetrics();
                        int densityDpi = targetDensityDpi(base, metrics);
                        int targetWidthDp = pixelsToDp(
                                metrics.widthPixels, densityDpi);
                        int targetHeightDp = pixelsToDp(
                                metrics.heightPixels, densityDpi);
                        Configuration override = new Configuration();
                        override.densityDpi = densityDpi;
                        override.screenWidthDp = targetWidthDp;
                        override.screenHeightDp = targetHeightDp;
                        override.smallestScreenWidthDp = targetWidthDp;
                        param.args[0] = base.createConfigurationContext(override);

                        if (LOGGED_ACTIVE.compareAndSet(false, true)) {
                            Log.i(TAG, "Applied half-pane layout scale");
                        }
                    }
                });

        XposedBridge.hookAllMethods(
                android.app.Application.class,
                "attach",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.args == null || param.args.length == 0
                                || !(param.args[0] instanceof Context)) {
                            return;
                        }
                        Context context = (Context) param.args[0];
                        enabled.set(isEnabled(context));
                        observeEnabledSetting(context, enabled);
                    }
                });
        Log.i(TAG, "Installed stable Activity layout hook");
    }

    private static boolean isHalfWidthExpandedPane(Context context) {
        Configuration configuration =
                context.getResources().getConfiguration();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        Display display = context.getDisplay();
        if (display == null || configuration.screenWidthDp >= LARGE_SCREEN_DP) {
            return false;
        }
        int physicalWidth = display.getMode().getPhysicalWidth();
        return physicalWidth >= Math.round(metrics.widthPixels * HALF_WIDTH_RATIO);
    }

    private static int targetDensityDpi(Context context, DisplayMetrics metrics) {
        Display display = context.getDisplay();
        int physicalWidth = display == null
                ? metrics.widthPixels * 2
                : display.getMode().getPhysicalWidth();
        int fullWidthDp = Math.max(
                LARGE_SCREEN_DP,
                Math.round(physicalWidth * DisplayMetrics.DENSITY_DEFAULT
                        / (float) metrics.densityDpi));
        return Math.max(
                DisplayMetrics.DENSITY_LOW,
                Math.round(metrics.widthPixels * DisplayMetrics.DENSITY_DEFAULT
                        / (float) fullWidthDp));
    }

    private static int pixelsToDp(int pixels, int densityDpi) {
        return Math.round(
                pixels * DisplayMetrics.DENSITY_DEFAULT / (float) densityDpi);
    }

    private static void observeEnabledSetting(
            Context context,
            AtomicBoolean enabled) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                Settings.Global.getUriFor(
                        SettingsKeys.KEY_ENABLE_MEITUAN_SPLIT_RULES),
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
                SettingsKeys.KEY_ENABLE_MEITUAN_SPLIT_RULES,
                0);
    }
}
