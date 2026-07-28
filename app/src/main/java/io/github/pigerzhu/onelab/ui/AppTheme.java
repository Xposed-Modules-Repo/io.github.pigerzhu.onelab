package io.github.pigerzhu.onelab.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

public final class AppTheme {
    public static final int MODE_SYSTEM = 0;
    public static final int MODE_LIGHT = 1;
    public static final int MODE_DARK = 2;

    private static final String PREFS = "onelab_appearance";
    private static final String KEY_MODE = "theme_mode";

    private AppTheme() {
    }

    public static Context wrap(Context base) {
        int mode = getMode(base);
        if (mode == MODE_SYSTEM) {
            return base;
        }
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        int nightMode = mode == MODE_DARK
                ? Configuration.UI_MODE_NIGHT_YES
                : Configuration.UI_MODE_NIGHT_NO;
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
        return base.createConfigurationContext(configuration);
    }

    public static int getMode(Context context) {
        return preferences(context).getInt(KEY_MODE, MODE_SYSTEM);
    }

    public static void setMode(Context context, int mode) {
        int safeMode = Math.max(MODE_SYSTEM, Math.min(MODE_DARK, mode));
        preferences(context).edit().putInt(KEY_MODE, safeMode).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
