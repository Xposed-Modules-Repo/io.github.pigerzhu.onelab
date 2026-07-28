package io.github.pigerzhu.onelab.system;

import android.content.Context;
import android.provider.Settings;
import android.widget.Toast;

public final class SettingsStore {

    private final Context context;

    public SettingsStore(Context context) {
        this.context = context;
    }

    public String getGlobal(String key, String defValue) {
        String value = Settings.Global.getString(context.getContentResolver(), key);
        return value == null ? defValue : value;
    }

    public int getGlobalInt(String key, int defValue) {
        try {
            return Integer.parseInt(getGlobal(key, String.valueOf(defValue)));
        } catch (NumberFormatException ignored) {
            return defValue;
        }
    }

    public void setGlobal(String key, String value) {
        try {
            Settings.Global.putString(context.getContentResolver(), key, value);
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            if (Shell.runSu("settings put global " + key + " " + value)) {
                Toast.makeText(context, "已通过 root 保存", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "保存失败，请授予 WRITE_SECURE_SETTINGS 或 root", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void putGlobalQuietly(String key, String value) {
        try {
            Settings.Global.putString(context.getContentResolver(), key, value);
        } catch (SecurityException e) {
            Shell.runSu("settings put global " + key + " '" + value + "'");
        }
    }

    public String getSystem(String key, String defValue) {
        String value;
        try {
            value = Settings.System.getString(context.getContentResolver(), key);
        } catch (SecurityException e) {
            value = runSuForOutput("settings get system " + key);
        }
        return value == null ? defValue : value;
    }

    public String getSecure(String key, String defValue) {
        String value = Settings.Secure.getString(context.getContentResolver(), key);
        return value == null ? defValue : value;
    }

    public int getSecureInt(String key, int defValue) {
        try {
            return Integer.parseInt(getSecure(key, String.valueOf(defValue)));
        } catch (NumberFormatException ignored) {
            return defValue;
        }
    }

    public void setSecure(String key, String value) {
        try {
            Settings.Secure.putString(context.getContentResolver(), key, value);
        } catch (SecurityException e) {
            Shell.runSu("settings put secure " + key + " " + value);
        }
    }

    public void setSecureWithToast(String key, String value) {
        try {
            Settings.Secure.putString(context.getContentResolver(), key, value);
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            if (Shell.runSu("settings put secure " + key + " " + value)) {
                Toast.makeText(context, "已通过 root 保存", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "保存失败，请授予 WRITE_SECURE_SETTINGS 或 root", Toast.LENGTH_LONG).show();
            }
        }
    }

    public boolean runSu(String command) {
        return Shell.runSu(command);
    }

    public String runSuForOutput(String command) {
        return Shell.runSuForOutput(command);
    }
}
