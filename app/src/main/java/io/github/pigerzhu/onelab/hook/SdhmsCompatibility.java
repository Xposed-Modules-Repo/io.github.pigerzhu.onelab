package io.github.pigerzhu.onelab.hook;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XposedHelpers;

/** Verified SDHMS implementation generations plus structural Binder discovery. */
final class SdhmsCompatibility {
    private static final Profile[] PROFILES = {
            new Profile(
                    "One UI 8",
                    "I1.g",
                    "R1.X1",
                    "v",
                    "n",
                    "t",
                    "u",
                    "Q1.D0",
                    "Q1.b0",
                    "Q1.a0",
                    "Q1.j2",
                    "p"
            ),
            new Profile(
                    "One UI 8.5",
                    "w4.g",
                    "l5.f5",
                    "F",
                    "t",
                    "D",
                    "E",
                    "k5.e2",
                    "k5.b1",
                    "k5.a1",
                    "k5.m5",
                    "o"
            )
    };

    private SdhmsCompatibility() {
    }

    static Profile detect(ClassLoader classLoader) {
        for (Profile profile : PROFILES) {
            if (XposedHelpers.findClassIfExists(profile.serviceClassName, classLoader) != null) {
                return profile;
            }
        }
        return null;
    }

    static Object fieldValueByType(Object owner, String className) {
        if (owner == null || className == null) {
            return null;
        }
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!className.equals(field.getType().getName())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(owner);
                    if (value != null) {
                        return value;
                    }
                } catch (Throwable ignored) {
                    // Continue through other matching fields.
                }
            }
        }
        return null;
    }

    static Object thermalServiceFromBinder(
            Object binderService,
            Class<?> expectedServiceClass
    ) {
        if (binderService == null) {
            return null;
        }
        for (Class<?> type = binderService.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Object host;
                try {
                    field.setAccessible(true);
                    host = field.get(binderService);
                } catch (Throwable ignored) {
                    continue;
                }
                if (host == null || !isMainApplication(host.getClass())) {
                    continue;
                }
                Object service = lookupThermalService(host, expectedServiceClass);
                if (service != null) {
                    return service;
                }
            }
        }
        return null;
    }

    private static boolean isMainApplication(Class<?> type) {
        return "com.sec.android.sdhms.MainApplication".equals(type.getName());
    }

    private static Object lookupThermalService(Object host, Class<?> expectedServiceClass) {
        for (String methodName : new String[]{"c", "b"}) {
            for (Class<?> type = host.getClass(); type != null; type = type.getSuperclass()) {
                try {
                    Method method = type.getDeclaredMethod(methodName, String.class);
                    method.setAccessible(true);
                    Object service = method.invoke(host, "Thermal");
                    if (isExpectedThermalService(service, expectedServiceClass)) {
                        return service;
                    }
                } catch (NoSuchMethodException ignored) {
                    // Try the verified lookup name from the other implementation generation.
                } catch (Throwable ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static boolean isExpectedThermalService(
            Object service,
            Class<?> expectedServiceClass
    ) {
        if (service == null) {
            return false;
        }
        if (expectedServiceClass != null) {
            return expectedServiceClass.isInstance(service);
        }
        try {
            service.getClass().getDeclaredMethod("H", int.class);
            service.getClass().getDeclaredMethod("I", String.class, int.class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static final class Profile {
        final String label;
        final String serviceClassName;
        final String controllerClassName;
        final String controllerSetDeltaMethod;
        final String controllerGetDeltaMethod;
        final String controllerShiftDeltaMethod;
        final String controllerSetFlagsMethod;
        final String gpuCapClassName;
        final String cpuCapClassName;
        final String littleCpuCapClassName;
        final String hiddenLimiterClassName;
        final String temperatureShiftMethod;

        Profile(
                String label,
                String serviceClassName,
                String controllerClassName,
                String controllerSetDeltaMethod,
                String controllerGetDeltaMethod,
                String controllerShiftDeltaMethod,
                String controllerSetFlagsMethod,
                String gpuCapClassName,
                String cpuCapClassName,
                String littleCpuCapClassName,
                String hiddenLimiterClassName,
                String temperatureShiftMethod
        ) {
            this.label = label;
            this.serviceClassName = serviceClassName;
            this.controllerClassName = controllerClassName;
            this.controllerSetDeltaMethod = controllerSetDeltaMethod;
            this.controllerGetDeltaMethod = controllerGetDeltaMethod;
            this.controllerShiftDeltaMethod = controllerShiftDeltaMethod;
            this.controllerSetFlagsMethod = controllerSetFlagsMethod;
            this.gpuCapClassName = gpuCapClassName;
            this.cpuCapClassName = cpuCapClassName;
            this.littleCpuCapClassName = littleCpuCapClassName;
            this.hiddenLimiterClassName = hiddenLimiterClassName;
            this.temperatureShiftMethod = temperatureShiftMethod;
        }
    }
}
