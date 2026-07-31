package io.github.pigerzhu.onelab.feature.applications;

import io.github.pigerzhu.onelab.MainActivity;

import static io.github.pigerzhu.onelab.contract.SettingsKeys.KEY_ENABLE_BILI_FOLD_GATE;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import io.github.pigerzhu.onelab.system.SettingsStore;
import io.github.pigerzhu.onelab.ui.Ui;

public final class BiliFoldGateScreen {
    private final MainActivity host;
    private final Ui ui;
    private final SettingsStore settings;

    public BiliFoldGateScreen(MainActivity host, Ui ui, SettingsStore settings) {
        this.host = host;
        this.ui = ui;
        this.settings = settings;
    }

    public View card() {
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
        copy.addView(ui.text("B 站原生大屏适配", 20, true, ui.colorOnSurface));

        MaterialSwitch toggle = new MaterialSwitch(host);
        toggle.setChecked("1".equals(settings.getGlobal(KEY_ENABLE_BILI_FOLD_GATE, "0")));
        toggle.setOnCheckedChangeListener((button, enabled) ->
                settings.setGlobal(KEY_ENABLE_BILI_FOLD_GATE, enabled ? "1" : "0"));
        header.addView(toggle);

        return card;
    }
}
