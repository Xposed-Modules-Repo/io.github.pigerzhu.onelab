package io.github.pigerzhu.onelab.hook.system;

import io.github.pigerzhu.onelab.hook.core.HookConstants;
import io.github.pigerzhu.onelab.hook.core.HookUtils;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import io.github.pigerzhu.onelab.contract.SettingsKeys;

import de.robv.android.xposed.XposedBridge;

/** In-process settings snapshot for SDHMS hooks. */
final class SdhmsHookConfig {
    private static final Object LOCK = new Object();
    private static volatile Snapshot snapshot = Snapshot.defaults();
    private static volatile ContentResolver resolver;
    private static volatile boolean observerRegistered;

    private SdhmsHookConfig() {
    }

    static Snapshot current(ContentResolver candidate) {
        ensureObserver(candidate);
        return snapshot;
    }

    private static void ensureObserver(ContentResolver candidate) {
        if (candidate == null || observerRegistered) {
            return;
        }
        synchronized (LOCK) {
            if (observerRegistered) {
                return;
            }
            resolver = candidate;
            Handler handler = new Handler(Looper.getMainLooper());
            ContentObserver observer = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange) {
                    reload();
                }
            };
            try {
                register(candidate, observer, SettingsKeys.KEY_ENABLE_SDHMS_THERMAL);
                register(candidate, observer, SettingsKeys.KEY_DISABLE_SDHMS_BRIGHTNESS_LIMIT);
                register(candidate, observer, SettingsKeys.KEY_DISABLE_SDHMS_CP_THERMAL_MITIGATION);
                register(candidate, observer, SettingsKeys.KEY_ENABLE_SDHMS_PERF_CAP_BYPASS);
                register(candidate, observer, SettingsKeys.KEY_ENABLE_SDHMS_CPU_CAP_RELEASE);
                register(candidate, observer, SettingsKeys.KEY_DISABLE_SSRM_MULTIWINDOW_LIMIT);
                register(candidate, observer, SettingsKeys.KEY_SDHMS_GPU_MIN_CAP_MHZ);
                reload();
                observerRegistered = true;
            } catch (Throwable t) {
                XposedBridge.log(HookConstants.TAG + ": SDHMS settings observer failed");
                XposedBridge.log(t);
            }
        }
    }

    private static void register(
            ContentResolver contentResolver,
            ContentObserver observer,
            String key
    ) {
        contentResolver.registerContentObserver(
                Settings.Global.getUriFor(key),
                false,
                observer
        );
    }

    private static void reload() {
        ContentResolver contentResolver = resolver;
        if (contentResolver == null) {
            return;
        }
        try {
            snapshot = new Snapshot(
                    enabled(contentResolver, SettingsKeys.KEY_ENABLE_SDHMS_THERMAL, 0),
                    enabled(contentResolver, SettingsKeys.KEY_DISABLE_SDHMS_BRIGHTNESS_LIMIT, 0),
                    enabled(contentResolver, SettingsKeys.KEY_DISABLE_SDHMS_CP_THERMAL_MITIGATION, 0),
                    enabled(contentResolver, SettingsKeys.KEY_ENABLE_SDHMS_PERF_CAP_BYPASS, 0),
                    enabled(contentResolver, SettingsKeys.KEY_ENABLE_SDHMS_CPU_CAP_RELEASE, 1),
                    enabled(contentResolver, SettingsKeys.KEY_DISABLE_SSRM_MULTIWINDOW_LIMIT, 0),
                    Settings.Global.getInt(
                            contentResolver,
                            SettingsKeys.KEY_SDHMS_GPU_MIN_CAP_MHZ,
                            SettingsKeys.DEFAULT_SDHMS_GPU_MIN_CAP_MHZ
                    )
            );
        } catch (Throwable t) {
            XposedBridge.log(HookConstants.TAG + ": SDHMS settings reload failed");
            XposedBridge.log(t);
        }
    }

    private static boolean enabled(ContentResolver contentResolver, String key, int fallback) {
        return Settings.Global.getInt(contentResolver, key, fallback) == 1;
    }

    static final class Snapshot {
        final boolean thermalEnabled;
        final boolean brightnessLimitDisabled;
        final boolean cpThermalMitigationDisabled;
        final boolean perfCapBypassEnabled;
        final boolean cpuCapReleaseEnabled;
        final boolean ssrmMultiWindowLimitDisabled;
        final int gpuMinCapMhz;

        Snapshot(
                boolean thermalEnabled,
                boolean brightnessLimitDisabled,
                boolean cpThermalMitigationDisabled,
                boolean perfCapBypassEnabled,
                boolean cpuCapReleaseEnabled,
                boolean ssrmMultiWindowLimitDisabled,
                int gpuMinCapMhz
        ) {
            this.thermalEnabled = thermalEnabled;
            this.brightnessLimitDisabled = brightnessLimitDisabled;
            this.cpThermalMitigationDisabled = cpThermalMitigationDisabled;
            this.perfCapBypassEnabled = perfCapBypassEnabled;
            this.cpuCapReleaseEnabled = cpuCapReleaseEnabled;
            this.ssrmMultiWindowLimitDisabled = ssrmMultiWindowLimitDisabled;
            this.gpuMinCapMhz = gpuMinCapMhz;
        }

        static Snapshot defaults() {
            return new Snapshot(
                    false,
                    false,
                    false,
                    false,
                    true,
                    false,
                    SettingsKeys.DEFAULT_SDHMS_GPU_MIN_CAP_MHZ
            );
        }
    }
}
