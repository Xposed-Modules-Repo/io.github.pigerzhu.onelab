package io.github.pigerzhu.onelab;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.json.JSONObject;

import io.github.pigerzhu.onelab.system.SettingsStore;
import io.github.pigerzhu.onelab.ui.Ui;

final class WindowManagementScreen {
    private static final String KEY_MULTISTAR_REPOSITORY = "multistar_setting_json_repository";
    private static final String KEY_MULTISTAR_ALL_REPOSITORY = "multistar_all_setting_repository";
    private static final String KEY_PERSIST_FREEFORM_BOUNDS = "persistFreeformBounds";

    private final MainActivity host;
    private final Ui ui;
    private final SettingsStore settings;

    WindowManagementScreen(MainActivity host, Ui ui, SettingsStore settings) {
        this.host = host;
        this.ui = ui;
        this.settings = settings;
    }

    View persistFreeformBoundsCard() {
        MaterialCardView card = ui.card();
        LinearLayout body = ui.cardBody();
        card.addView(body);

        LinearLayout header = new LinearLayout(host);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        body.addView(header, ui.matchWrap());

        LinearLayout copy = new LinearLayout(host);
        copy.setOrientation(LinearLayout.VERTICAL);
        header.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        copy.addView(ui.text("记住弹出窗口位置", 20, true, ui.colorOnSurface));
        copy.addView(ui.text("让弹出视图尽量从上次的位置和大小恢复。", 14, false,
                ui.colorOnSurfaceVariant));

        MaterialSwitch toggle = new MaterialSwitch(host);
        toggle.setChecked(getMultiStarBoolean(KEY_PERSIST_FREEFORM_BOUNDS, false));
        toggle.setOnCheckedChangeListener((button, enabled) -> {
            if (writeMultiStarBoolean(KEY_PERSIST_FREEFORM_BOUNDS, enabled)) {
                Toast.makeText(host, "已保存", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(host, "保存失败，请授予 WRITE_SECURE_SETTINGS 或 root",
                        Toast.LENGTH_LONG).show();
            }
        });
        header.addView(toggle);
        return card;
    }

    private boolean getMultiStarBoolean(String key, boolean defValue) {
        try {
            String raw = settings.getSecure(KEY_MULTISTAR_REPOSITORY, null);
            if (raw == null || raw.trim().isEmpty()) return defValue;
            JSONObject root = new JSONObject(raw);
            JSONObject settingsNode = root.optJSONObject("settings");
            if (settingsNode == null || !settingsNode.has(key)) return defValue;
            Object value = settingsNode.opt(key);
            return value instanceof Boolean
                    ? (Boolean) value
                    : Boolean.parseBoolean(String.valueOf(value));
        } catch (Exception ignored) {
            return defValue;
        }
    }

    private boolean writeMultiStarBoolean(String key, boolean enabled) {
        boolean mainOk = writeRepository(KEY_MULTISTAR_REPOSITORY, key, enabled);
        writeRepository(KEY_MULTISTAR_ALL_REPOSITORY, key, enabled);
        return mainOk;
    }

    private boolean writeRepository(String repositoryKey, String settingKey, boolean enabled) {
        try {
            String raw = settings.getSecure(repositoryKey, null);
            JSONObject root = raw == null || raw.trim().isEmpty()
                    ? new JSONObject()
                    : new JSONObject(raw);
            root.put("version", root.optInt("version", 1));
            JSONObject settingsNode = root.optJSONObject("settings");
            if (settingsNode == null) {
                settingsNode = new JSONObject();
                root.put("settings", settingsNode);
            }
            settingsNode.put(settingKey, enabled);
            settings.setSecure(repositoryKey, root.toString());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
