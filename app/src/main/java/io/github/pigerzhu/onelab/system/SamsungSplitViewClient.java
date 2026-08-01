package io.github.pigerzhu.onelab.system;

import static io.github.pigerzhu.onelab.contract.SettingsKeys.KEY_SPLIT_VIEW_ALLOWED_PACKAGES;

import java.util.LinkedHashSet;
import java.util.Set;

/** Reads the split-activity package snapshot published by the system-server hook. */
public final class SamsungSplitViewClient {
    private final SettingsStore settings;

    public SamsungSplitViewClient(SettingsStore settings) {
        this.settings = settings;
    }

    public Set<String> allowedPackages() {
        LinkedHashSet<String> packages = new LinkedHashSet<>();
        String raw = settings.getGlobal(KEY_SPLIT_VIEW_ALLOWED_PACKAGES, "");
        for (String item : raw.split(",")) {
            String packageName = item.trim();
            if (!packageName.isEmpty()) {
                packages.add(packageName);
            }
        }
        return packages;
    }
}
