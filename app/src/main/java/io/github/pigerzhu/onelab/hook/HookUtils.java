package io.github.pigerzhu.onelab.hook;

import android.content.ContentResolver;
import android.os.Binder;
import android.provider.Settings;

import java.lang.reflect.Method;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class HookUtils {
    private HookUtils() {
    }

    static boolean globalEnabled(ContentResolver resolver, String key, int fallback) {
        return resolver != null && Settings.Global.getInt(resolver, key, fallback) == 1;
    }

    static Object findFieldValue(Object target, String name) {
        if (target == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                java.lang.reflect.Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    static Object invokeStaticNoArg(String className, String methodName, ClassLoader classLoader) {
        try {
            Class<?> type = XposedHelpers.findClass(className, classLoader);
            Method method = type.getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static ContentResolver resolverFromController(Object controller) {
        Object context = findFieldValue(controller, "mContext");
        if (context == null) {
            return null;
        }
        return resolverFromContextObject(context);
    }

    static ContentResolver resolverFromContextObject(Object context) {
        if (context == null) {
            return null;
        }
        try {
            return (ContentResolver) XposedHelpers.callMethod(context, "getContentResolver");
        } catch (Throwable ignored) {
            return null;
        }
    }

    static ContentResolver resolverFromAnyContext(Object target) {
        ContentResolver resolver = resolverFromContextObject(findFieldValue(target, "f5510a"));
        if (resolver != null) {
            return resolver;
        }
        resolver = resolverFromContextObject(findFieldValue(target, "mContext"));
        if (resolver != null) {
            return resolver;
        }
        resolver = resolverFromContextObject(findFieldValue(target, "context"));
        if (resolver != null) {
            return resolver;
        }
        Object application = invokeStaticNoArg(
                "android.app.ActivityThread",
                "currentApplication",
                target == null ? HookUtils.class.getClassLoader() : target.getClass().getClassLoader()
        );
        return resolverFromContextObject(application);
    }

    static Object firstContextFromObject(Object target) {
        Object context = findFieldValue(target, "f5510a");
        if (context == null) {
            context = findFieldValue(target, "f3031a");
        }
        if (context == null) {
            context = findFieldValue(target, "mContext");
        }
        if (context == null) {
            context = findFieldValue(target, "context");
        }
        return context;
    }

    static boolean isCallingOneLabOrShell(Object context) {
        int uid = Binder.getCallingUid();
        if (uid == 2000) {
            return true;
        }
        return packageForCallingUid(context, HookConstants.ONELAB_PACKAGE);
    }

    static boolean packageForCallingUid(Object context, String expectedPackage) {
        if (context == null) {
            return false;
        }
        try {
            Object packageManager = XposedHelpers.callMethod(context, "getPackageManager");
            String[] packages = (String[]) XposedHelpers.callMethod(
                    packageManager,
                    "getPackagesForUid",
                    Binder.getCallingUid()
            );
            if (packages == null) {
                return false;
            }
            for (String packageName : packages) {
                if (expectedPackage.equals(packageName)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            return false;
        }
        return false;
    }

    static void log(Throwable throwable) {
        XposedBridge.log(throwable);
    }
}
