package io.github.pigerzhu.onelab.system;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Reads the same Samsung split-activity allowlist used by Settings. */
public final class SamsungSplitViewClient {
    private static final String MULTI_WINDOW_MANAGER =
            "com.samsung.android.multiwindow.MultiWindowManager";

    public Set<String> allowedPackages() {
        try {
            Class<?> managerClass = Class.forName(MULTI_WINDOW_MANAGER);
            Method getInstance = managerClass.getMethod("getInstance");
            Object manager = getInstance.invoke(null);
            Method getPackages = managerClass.getMethod("getSplitActivityAllowPackages");
            Object value = getPackages.invoke(manager);
            if (!(value instanceof List)) return Collections.emptySet();

            LinkedHashSet<String> packages = new LinkedHashSet<>();
            for (Object item : (List<?>) value) {
                if (item instanceof String && !((String) item).isEmpty()) {
                    packages.add((String) item);
                }
            }
            return packages;
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }
}
