package io.github.pigerzhu.onelab;

import static io.github.pigerzhu.onelab.contract.SettingsKeys.KEY_ENABLE_XHS_FOLD_HOME;
import static io.github.pigerzhu.onelab.contract.SettingsKeys.KEY_ENABLE_XHS_FOLD_VIDEO;

import android.view.View;
import android.widget.LinearLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import io.github.pigerzhu.onelab.system.SettingsStore;
import io.github.pigerzhu.onelab.ui.Ui;

final class XhsFoldVideoScreen {
    private final MainActivity host;
    private final Ui ui;
    private final SettingsStore settings;

    XhsFoldVideoScreen(MainActivity host, Ui ui, SettingsStore settings) {
        this.host = host;
        this.ui = ui;
        this.settings = settings;
    }

    View card() {
        MaterialCardView card = ui.card();
        LinearLayout body = ui.cardBody();
        card.addView(body);

        body.addView(ui.text("小红书折叠屏适配", 20, true, ui.colorOnSurface));
        ui.addSpace(body, 8);

        MaterialSwitch homeToggle = new MaterialSwitch(host);
        boolean homeEnabled =
                "1".equals(settings.getGlobal(KEY_ENABLE_XHS_FOLD_HOME, "0"));
        boolean videoEnabled =
                "1".equals(settings.getGlobal(KEY_ENABLE_XHS_FOLD_VIDEO, "0"));
        boolean[] syncing = {false};
        homeToggle.setChecked(homeEnabled || videoEnabled);
        homeToggle.setEnabled(!videoEnabled);
        homeToggle.setOnCheckedChangeListener((button, enabled) -> {
            if (!syncing[0]) {
                settings.setGlobal(KEY_ENABLE_XHS_FOLD_HOME, enabled ? "1" : "0");
            }
        });
        body.addView(ui.switchRow("启用首页四列显示", null, homeToggle));

        MaterialSwitch videoToggle = new MaterialSwitch(host);
        videoToggle.setChecked(videoEnabled);
        videoToggle.setOnCheckedChangeListener((button, enabled) -> {
            settings.setGlobal(KEY_ENABLE_XHS_FOLD_VIDEO, enabled ? "1" : "0");
            syncing[0] = true;
            homeToggle.setEnabled(!enabled);
            homeToggle.setChecked(enabled || "1".equals(
                    settings.getGlobal(KEY_ENABLE_XHS_FOLD_HOME, "0")));
            syncing[0] = false;
        });
        body.addView(ui.switchRow("启用新版视频帖布局", null, videoToggle));

        return card;
    }
}
