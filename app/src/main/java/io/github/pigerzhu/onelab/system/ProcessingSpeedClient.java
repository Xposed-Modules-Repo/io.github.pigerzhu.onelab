package io.github.pigerzhu.onelab.system;

import android.content.ComponentName;
import android.content.Context;

public final class ProcessingSpeedClient {
    public static final String ACTION_ENHANCED_PROCESSING =
            "com.samsung.android.sm.ACTION_ENHANCED_PROCESSING";

    private static final String DEVICE_CARE_PACKAGE = "com.samsung.android.lool";
    private static final String PROCESSING_SPEED_ACTIVITY =
            "com.samsung.android.sm.battery.ui.setting.EnhancedProcessingActivity";
    private static final String ENHANCED_CPU_TILE =
            "com.samsung.android.sm.enhancedcpu.EnhancedCpuTile";
    private static final String PROCESSING_SPEED_TILE =
            "com.samsung.android.sm.enhancedcpu.ProcessingSpeedTile";

    private final Context context;

    public ProcessingSpeedClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean setNativeComponentsEnabled(boolean enabled) {
        String verb = enabled ? "enable" : "disable";
        return setComponent(verb, PROCESSING_SPEED_ACTIVITY)
                & setComponent(verb, ENHANCED_CPU_TILE)
                & setComponent(verb, PROCESSING_SPEED_TILE);
    }

    public boolean isNativeComponentsEnabled() {
        try {
            ComponentName tile = new ComponentName(DEVICE_CARE_PACKAGE, PROCESSING_SPEED_TILE);
            int state = context.getPackageManager().getComponentEnabledSetting(tile);
            return state != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean openNativePageWithRoot() {
        return Shell.runSu("am start -a " + ACTION_ENHANCED_PROCESSING);
    }

    private boolean setComponent(String verb, String componentClass) {
        return Shell.runSu("pm " + verb + " --user 0 "
                + DEVICE_CARE_PACKAGE + "/" + componentClass);
    }
}
