package io.github.pigerzhu.onelab.hook;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.pigerzhu.onelab.contract.SettingsKeys;

/**
 * Enables XHS's existing Pad video-detail route inside the XHS process.
 */
final class XhsFoldVideoHook {
    private static final String TAG = "OneLab/XhsFoldVideo";

    private static final String AB_TEST_HELPER =
            "com.xingin.detailfeed.abtest.DetailFeedAbTestHelper";
    private static final String INTENT_DATA =
            "com.xingin.matrix.detail.intent.DetailFeedIntentData";
    private static final String FOLD_CONFIG = "zf2.u";
    private static final String COMMENT_GATE = "ni6.g";
    private static final String COMMENT_PANEL = "qd8.g";
    private static final String WINDOW_GATE = "es.n";

    private static final String DEVICE_INFO_CONTAINER =
            "com.xingin.adaptation.device.DeviceInfoContainer";

    private static final String[] VIDEO_FRAME_FLAGS = {
            "enableNewVideoFeedFrame",
            "padVideoPlayNewFramework",
            "padVideoIsNewVideoFrame",
            "padVideoNewFrameStyleAdjust",
            "padVideoCommentTextOptCombo"
    };
    private static final String[] VIDEO_ROUTE_TRUE = {
            "isNewVideoFeedFrame",
            "y1"
    };
    private static final String VIDEO_ROUTE_FALSE = "isOldVideoFeedStyle";
    private static final String VIDEO_BUSINESS_TYPE = "isVideoFeedBusinessType";

    private static final Set<ClassLoader> INSTALLED_LOADERS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Object INSTALL_LOCK = new Object();
    private static final AtomicBoolean LOGGED_HOME_ACTIVE = new AtomicBoolean();
    private static final AtomicBoolean LOGGED_VIDEO_ACTIVE = new AtomicBoolean();

    private XhsFoldVideoHook() {
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
            AtomicBoolean homeEnabled = new AtomicBoolean(isHomeEnabled(context));
            AtomicBoolean videoEnabled = new AtomicBoolean(isVideoEnabled(context));
            observeEnabledSettings(context, homeEnabled, videoEnabled);
            FoldGate gate = new FoldGate(homeEnabled, videoEnabled);
            int hooks = 0;
            hooks += hookHorizontalFoldDeviceFlag(classLoader, gate);
            hooks += hookVideoFrameFlags(classLoader, gate);
            hooks += hookVideoIntentRoutes(classLoader, gate);
            hooks += hookPadDeviceFlag(classLoader, gate);
            hooks += hookFoldConfig(classLoader, gate);
            hooks += hookCommentPanel(classLoader, gate);
            hooks += hookWindowGate(classLoader, gate);
            Log.i(TAG, "Installed " + hooks + " guarded video hooks");
        } catch (Throwable throwable) {
            synchronized (INSTALL_LOCK) {
                INSTALLED_LOADERS.remove(classLoader);
            }
            XposedBridge.log(TAG + ": installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static int hookVideoFrameFlags(ClassLoader classLoader, FoldGate gate) {
        int hooks = 0;
        for (String methodName : VIDEO_FRAME_FLAGS) {
            hooks += hookAfter(classLoader, AB_TEST_HELPER, methodName, param -> {
                if (gate.isEligible()) gate.setTrue(param);
            });
        }
        return hooks;
    }

    private static int hookVideoIntentRoutes(ClassLoader classLoader, FoldGate gate) {
        int hooks = 0;
        for (String methodName : VIDEO_ROUTE_TRUE) {
            hooks += hookAfter(classLoader, INTENT_DATA, methodName, param -> {
                if (gate.isVideoDetail(param.thisObject)) gate.setTrue(param);
            });
        }
        hooks += hookAfter(classLoader, INTENT_DATA, VIDEO_ROUTE_FALSE, param -> {
            if (gate.isVideoDetail(param.thisObject)) param.setResult(false);
        });
        return hooks;
    }

    private static int hookHorizontalFoldDeviceFlag(ClassLoader classLoader, FoldGate gate) {
        return hookAfter(classLoader, DEVICE_INFO_CONTAINER, "isHorizontalFolderDevice", param -> {
            if (gate.isHomeEnabled()) gate.setHomeTrue(param);
        });
    }

    private static int hookPadDeviceFlag(ClassLoader classLoader, FoldGate gate) {
        return hookAfter(classLoader, DEVICE_INFO_CONTAINER, "isPad", param -> {
            if (gate.isVideoEnabled()) gate.setTrue(param);
        });
    }

    private static int hookFoldConfig(ClassLoader classLoader, FoldGate gate) {
        int hooks = 0;
        for (String methodName : new String[]{"Y0", "Z0", "A0"}) {
            hooks += hookAfter(classLoader, FOLD_CONFIG, methodName, param -> {
                if (gate.isEligible()) gate.setTrue(param);
            });
        }
        return hooks;
    }

    private static int hookCommentPanel(ClassLoader classLoader, FoldGate gate) {
        int hooks = hookAfter(classLoader, COMMENT_GATE, "f", param -> {
            if (gate.isEligible()) gate.setTrue(param);
        });
        hooks += hookBefore(classLoader, COMMENT_PANEL, "e", param -> {
            if (gate.isEligible() && param.args != null && param.args.length == 15
                    && param.args[10] instanceof Boolean) {
                param.args[10] = true;
                gate.logActive();
            }
        });
        hooks += hookBefore(classLoader, COMMENT_PANEL, "f", param -> {
            if (gate.isEligible() && param.args != null && param.args.length == 16
                    && param.args[11] instanceof Boolean) {
                param.args[11] = true;
                gate.logActive();
            }
        });
        return hooks;
    }

    private static int hookWindowGate(ClassLoader classLoader, FoldGate gate) {
        return hookBefore(classLoader, WINDOW_GATE, "j", param -> {
            if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof Context)) {
                return;
            }
            if (gate.isEligible()) {
                gate.setTrue(param);
            }
        });
    }

    private static int hookAfter(ClassLoader classLoader, String className, String methodName,
            HookAction action) {
        try {
            Class<?> type = classLoader.loadClass(className);
            XposedBridge.hookAllMethods(type, methodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        action.apply(param);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int hookBefore(ClassLoader classLoader, String className, String methodName,
            HookAction action) {
        try {
            Class<?> type = classLoader.loadClass(className);
            XposedBridge.hookAllMethods(type, methodName, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        action.apply(param);
                    } catch (Throwable ignored) {
                    }
                }
            });
            return 1;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static void observeEnabledSettings(Context context, AtomicBoolean homeEnabled,
            AtomicBoolean videoEnabled) {
        ContentResolver resolver = context.getContentResolver();
        resolver.registerContentObserver(
                Settings.Global.getUriFor(SettingsKeys.KEY_ENABLE_XHS_FOLD_HOME),
                false,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        homeEnabled.set(isHomeEnabled(context));
                    }
                });
        resolver.registerContentObserver(
                Settings.Global.getUriFor(SettingsKeys.KEY_ENABLE_XHS_FOLD_VIDEO),
                false,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override
                    public void onChange(boolean selfChange) {
                        videoEnabled.set(isVideoEnabled(context));
                    }
                });
    }

    private static boolean isHomeEnabled(Context context) {
        return HookUtils.globalEnabled(
                context.getContentResolver(), SettingsKeys.KEY_ENABLE_XHS_FOLD_HOME, 0);
    }

    private static boolean isVideoEnabled(Context context) {
        return HookUtils.globalEnabled(
                context.getContentResolver(), SettingsKeys.KEY_ENABLE_XHS_FOLD_VIDEO, 0);
    }

    private interface HookAction {
        void apply(XC_MethodHook.MethodHookParam param);
    }

    private static final class FoldGate {
        private final AtomicBoolean homeEnabled;
        private final AtomicBoolean videoEnabled;
        private volatile Class<?> videoBusinessTypeOwner;
        private volatile Method videoBusinessTypeMethod;

        FoldGate(AtomicBoolean homeEnabled, AtomicBoolean videoEnabled) {
            this.homeEnabled = homeEnabled;
            this.videoEnabled = videoEnabled;
        }

        boolean isEligible() {
            return videoEnabled.get();
        }

        boolean isHomeEnabled() {
            return homeEnabled.get();
        }

        boolean isVideoEnabled() {
            return videoEnabled.get();
        }

        boolean isVideoDetail(Object detailIntentData) {
            if (!isEligible() || detailIntentData == null) return false;
            try {
                Method method = videoBusinessTypeMethod(detailIntentData.getClass());
                if (method == null) return false;
                return Boolean.TRUE.equals(method.invoke(detailIntentData));
            } catch (Throwable ignored) {
                return false;
            }
        }

        void setTrue(XC_MethodHook.MethodHookParam param) {
            param.setResult(true);
            logActive();
        }

        void setHomeTrue(XC_MethodHook.MethodHookParam param) {
            param.setResult(true);
            if (LOGGED_HOME_ACTIVE.compareAndSet(false, true)) {
                Log.i(TAG, "Fold home layout enabled");
            }
        }

        void logActive() {
            if (LOGGED_VIDEO_ACTIVE.compareAndSet(false, true)) {
                Log.i(TAG, "Pad video route enabled");
            }
        }

        private Method videoBusinessTypeMethod(Class<?> owner) {
            Method cached = videoBusinessTypeMethod;
            if (owner == videoBusinessTypeOwner && cached != null) return cached;
            synchronized (this) {
                if (owner == videoBusinessTypeOwner && videoBusinessTypeMethod != null) {
                    return videoBusinessTypeMethod;
                }
                try {
                    Method method = owner.getMethod(VIDEO_BUSINESS_TYPE);
                    method.setAccessible(true);
                    videoBusinessTypeOwner = owner;
                    videoBusinessTypeMethod = method;
                    return method;
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }

    }
}
