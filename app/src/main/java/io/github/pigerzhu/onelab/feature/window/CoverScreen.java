package io.github.pigerzhu.onelab.feature.window;

import io.github.pigerzhu.onelab.MainActivity;
import io.github.pigerzhu.onelab.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.pigerzhu.onelab.system.DeviceStateClient;
import io.github.pigerzhu.onelab.ui.ChoiceGroup;
import io.github.pigerzhu.onelab.ui.Ui;

public final class CoverScreen {
    private static final int REQ_PICK_COVER_IMAGE = 4201;
    private static final String PREF_OUTER_SYSTEM_ENABLED = "outer_system_enabled";
    private static final String PREF_OUTER_SYSTEM_BOOT_COUNT = "outer_system_boot_count";

    private final MainActivity host;
    private final Ui ui;
    private final DeviceStateClient deviceState = new DeviceStateClient();
    private final ExecutorService deviceStateExecutor = Executors.newSingleThreadExecutor();
    private CoverDisplayPresenter coverPresenter;

    public CoverScreen(MainActivity host, Ui ui) {
        this.host = host;
        this.ui = ui;
    }

    public View outerSystemCard() {
        MaterialCardView card = ui.card();
        LinearLayout body = ui.cardBody();
        card.addView(body);

        TextView status = ui.text("", 14, false, ui.colorOnSurfaceVariant);
        status.setVisibility(View.GONE);
        MaterialSwitch toggle = new MaterialSwitch(host);
        toggle.setChecked(cachedOuterSystemEnabled());

        LinearLayout header = new LinearLayout(host);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.addView(ui.text("展开时使用完整外屏", 20, true, ui.colorOnSurface),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(toggle);
        body.addView(header, ui.matchWrap());

        body.addView(status);

        toggle.setOnCheckedChangeListener((button, enabled) -> {
            setOuterSystemState(toggle, status, enabled);
        });
        return card;
    }

    private void setOuterSystemState(MaterialSwitch toggle, TextView status, boolean enabled) {
        toggle.setEnabled(false);
        status.setVisibility(View.VISIBLE);
        status.setText(enabled ? "正在切换到外屏..." : "正在恢复内屏...");
        deviceStateExecutor.execute(() -> {
            boolean supported = deviceState.supportsOuterDefault();
            int state = supported
                    ? deviceState.setOuterDefault(enabled)
                    : DeviceStateClient.STATE_UNKNOWN;
            boolean success = enabled
                    ? state == DeviceStateClient.STATE_OUTER_DEFAULT
                    : state != DeviceStateClient.STATE_UNKNOWN
                    && state != DeviceStateClient.STATE_OUTER_DEFAULT;
            host.runOnUiThread(() -> {
                if (success) {
                    saveOuterSystemEnabled(enabled);
                    applyOuterSystemState(toggle, status, true, state);
                } else {
                    toggle.setOnCheckedChangeListener(null);
                    toggle.setChecked(!enabled);
                    toggle.setOnCheckedChangeListener((button, checked) ->
                            setOuterSystemState(toggle, status, checked));
                    toggle.setEnabled(true);
                    status.setText(supported
                            ? "切换失败，请检查 root 授权"
                            : "这台设备不支持完整外屏切换");
                }
                Toast.makeText(host,
                        success ? (enabled ? "已切换到完整外屏" : "已恢复内屏")
                                : (supported ? "切换失败，请检查 root 授权"
                                : "这台设备不支持完整外屏切换"),
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void applyOuterSystemState(MaterialSwitch toggle, TextView status,
                                       boolean supported, int state) {
        toggle.setOnCheckedChangeListener(null);
        toggle.setChecked(state == DeviceStateClient.STATE_OUTER_DEFAULT);
        toggle.setOnCheckedChangeListener((button, enabled) -> {
            setOuterSystemState(toggle, status, enabled);
        });
        toggle.setEnabled(supported && state != DeviceStateClient.STATE_UNKNOWN);
        if (!supported) {
            status.setText("这台设备没有完整外屏状态，或尚未授予 root");
        } else if (state == DeviceStateClient.STATE_UNKNOWN) {
            status.setText("无法读取设备状态，请检查 root 授权");
        } else if (state == DeviceStateClient.STATE_OUTER_DEFAULT) {
            status.setText("当前：完整系统运行在外屏");
        } else {
            status.setText("当前：系统运行在内屏");
        }
    }

    private boolean cachedOuterSystemEnabled() {
        SharedPreferences prefs = coverPrefs();
        return prefs.getInt(PREF_OUTER_SYSTEM_BOOT_COUNT, -1) == currentBootCount()
                && prefs.getBoolean(PREF_OUTER_SYSTEM_ENABLED, false);
    }

    private void saveOuterSystemEnabled(boolean enabled) {
        coverPrefs().edit()
                .putBoolean(PREF_OUTER_SYSTEM_ENABLED, enabled)
                .putInt(PREF_OUTER_SYSTEM_BOOT_COUNT, currentBootCount())
                .apply();
    }

    private int currentBootCount() {
        return Settings.Global.getInt(
                host.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
    }

    public View card() {
        MaterialCardView card = ui.card();
        LinearLayout body = ui.cardBody();
        card.addView(body);

        body.addView(ui.text("外屏显示内容（双屏并发）", 20, true, ui.colorOnSurface));

        ui.addSpace(body, 12);
        TextView status = ui.text("正在检测外屏可用性...", 14, false, ui.colorOnSurfaceVariant);
        body.addView(status);

        if (coverPresenter == null) {
            coverPresenter = new CoverDisplayPresenter(host);
        }

        ui.addSpace(body, 14);
        ChoiceGroup modeGroup = new ChoiceGroup(host, ui);
        body.addView(modeGroup, ui.matchWrap());
        modeGroup.addOption("时钟", "在外屏显示时间与日期", CoverDisplayPresenter.MODE_CLOCK);
        modeGroup.addOption("文字", "在外屏显示自定义文字", CoverDisplayPresenter.MODE_TEXT);
        modeGroup.addOption("图片", "在外屏显示选择的图片", CoverDisplayPresenter.MODE_IMAGE);

        ui.addSpace(body, 10);
        MaterialButton editButton = new MaterialButton(
                host, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        editButton.setText("设置文字 / 选择图片");
        body.addView(editButton, ui.matchWrap());

        ui.addSpace(body, 14);
        LinearLayout actions = new LinearLayout(host);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        body.addView(actions, ui.matchWrap());

        MaterialButton startButton = new MaterialButton(host);
        startButton.setText("点亮外屏");
        startButton.setEnabled(false);
        actions.addView(startButton, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        MaterialButton stopButton = new MaterialButton(
                host, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        stopButton.setText("停止");
        stopButton.setEnabled(false);
        LinearLayout.LayoutParams stopParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        stopParams.setMarginStart(ui.dp(12));
        actions.addView(stopButton, stopParams);

        // Restore the saved content choice.
        SharedPreferences prefs = coverPrefs();
        int savedMode = prefs.getInt("mode", CoverDisplayPresenter.MODE_CLOCK);
        coverPresenter.setContentMode(savedMode);
        coverPresenter.setCustomText(prefs.getString("text", ""));
        String savedUri = prefs.getString("image_uri", null);
        if (savedUri != null) {
            coverPresenter.setImageUri(Uri.parse(savedUri));
        }
        modeGroup.setValue(savedMode);
        editButton.setEnabled(savedMode != CoverDisplayPresenter.MODE_CLOCK);

        modeGroup.setOnChoiceChangedListener(mode -> {
            coverPrefs().edit().putInt("mode", mode).apply();
            coverPresenter.setContentMode(mode);
            editButton.setEnabled(mode != CoverDisplayPresenter.MODE_CLOCK);
        });
        editButton.setOnClickListener(v -> {
            if (coverPresenter.contentMode() == CoverDisplayPresenter.MODE_IMAGE) {
                pickCoverImage();
            } else {
                showCoverTextDialog();
            }
        });

        coverPresenter.setStatusListener((text, canPresent, active) -> host.runOnUiThread(() -> {
            status.setText(text);
            status.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            startButton.setEnabled(canPresent);
            stopButton.setEnabled(active);
        }));
        startButton.setOnClickListener(v -> coverPresenter.present());
        stopButton.setOnClickListener(v -> coverPresenter.end());
        coverPresenter.startListening();
        return card;
    }

    private SharedPreferences coverPrefs() {
        return host.getSharedPreferences("onelab_cover", Activity.MODE_PRIVATE);
    }

    private void showCoverTextDialog() {
        EditText input = new EditText(host);
        input.setText(coverPrefs().getString("text", ""));
        input.setSelectAllOnFocus(true);
        FrameLayout container = new FrameLayout(host);
        container.setPadding(ui.dp(22), ui.dp(8), ui.dp(22), 0);
        container.addView(input, ui.matchWrap());
        new AlertDialog.Builder(host)
                .setTitle("外屏文字")
                .setView(container)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    String value = input.getText().toString();
                    coverPrefs().edit().putString("text", value).apply();
                    if (coverPresenter != null) {
                        coverPresenter.setCustomText(value);
                    }
                })
                .show();
    }

    private void pickCoverImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            host.startActivityForResult(intent, REQ_PICK_COVER_IMAGE);
        } catch (Exception e) {
            Toast.makeText(host, "无法打开图片选择器", Toast.LENGTH_SHORT).show();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQ_PICK_COVER_IMAGE || resultCode != Activity.RESULT_OK
                || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        try {
            host.getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Some providers don't grant persistable access; the URI still works this session.
        }
        coverPrefs().edit().putString("image_uri", uri.toString()).apply();
        if (coverPresenter != null) {
            coverPresenter.setImageUri(uri);
        }
    }

    public void onDestroy() {
        deviceStateExecutor.shutdownNow();
        if (coverPresenter != null) {
            coverPresenter.stopListening();
            coverPresenter.end();
        }
    }
}
