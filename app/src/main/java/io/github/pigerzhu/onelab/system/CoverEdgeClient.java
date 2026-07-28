package io.github.pigerzhu.onelab.system;

import android.content.Context;
import android.provider.Settings;

public final class CoverEdgeClient {
    private static final String KEY_TSP_THRESHOLD_COVER = "setting_tsp_threshold_cover";

    private final Context context;

    public CoverEdgeClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public String read() {
        return Settings.Secure.getString(context.getContentResolver(), KEY_TSP_THRESHOLD_COVER);
    }

    public boolean write(String value) {
        boolean written = false;
        try {
            written = Settings.Secure.putString(
                    context.getContentResolver(), KEY_TSP_THRESHOLD_COVER, value);
        } catch (SecurityException ignored) {
        }
        if (!written) {
            written = value == null
                    ? Shell.runSu("settings delete secure " + KEY_TSP_THRESHOLD_COVER)
                    : Shell.runSu("settings put secure " + KEY_TSP_THRESHOLD_COVER
                    + " " + shellQuote(value));
        }
        if (!written) return false;
        String actual = read();
        return value == null ? actual == null : value.equals(actual);
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
