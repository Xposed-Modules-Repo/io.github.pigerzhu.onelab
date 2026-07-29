package io.github.pigerzhu.onelab.feature.diagnostics;

import io.github.pigerzhu.onelab.MainActivity;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.pigerzhu.onelab.diagnostics.DiagnosticReport;
import io.github.pigerzhu.onelab.ui.Ui;

public final class DiagnosticsScreen {
    private final MainActivity host;
    private final Ui ui;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DiagnosticsScreen(MainActivity host, Ui ui) {
        this.host = host;
        this.ui = ui;
    }

    public MaterialCardView card() {
        MaterialCardView card = ui.card();
        LinearLayout body = ui.cardBody();
        card.addView(body);
        body.addView(ui.text("诊断与反馈", 20, true, ui.colorOnSurface));
        body.addView(ui.text(
                "复现问题后生成脱敏报告，可直接附加到 GitHub Issue。",
                14, false, ui.colorOnSurfaceVariant));
        ui.addSpace(body, 12);

        MaterialTextView status = ui.text(statusText(), 13, false, ui.colorOnSurfaceVariant);
        body.addView(status);
        ui.addSpace(body, 10);

        MaterialButton start = ui.actionButton("开始记录");
        MaterialButton stop = ui.actionButton("停止记录");
        MaterialButton generate = ui.actionButton("生成并分享");
        MaterialButton clear = ui.actionButton("清除");
        body.addView(generate, ui.matchWrap());
        ui.addSpace(body, 6);

        LinearLayout secondaryActions = new LinearLayout(host);
        secondaryActions.setOrientation(LinearLayout.HORIZONTAL);
        body.addView(secondaryActions, ui.matchWrap());
        secondaryActions.addView(start, weightedButtonParams());
        secondaryActions.addView(stop, weightedButtonParams());
        secondaryActions.addView(clear, weightedButtonParams());

        start.setOnClickListener(v -> {
            DiagnosticReport.startSession(host);
            syncState(status, start, stop, generate);
            Toast.makeText(host, "开始记录，请复现问题", Toast.LENGTH_SHORT).show();
        });
        stop.setOnClickListener(v -> {
            DiagnosticReport.stopSession(host);
            syncState(status, start, stop, generate);
            Toast.makeText(host, "记录已停止，可以生成报告", Toast.LENGTH_SHORT).show();
        });
        generate.setOnClickListener(v -> {
            setBusy(generate, secondaryActions, false);
            status.setText("正在生成报告…");
            executor.execute(() -> {
                try {
                    DiagnosticReport.PublishedReport report =
                            DiagnosticReport.generate(host);
                    host.runOnUiThread(() -> {
                        setBusy(generate, secondaryActions, true);
                        status.setText("已生成 " + report.fileName);
                        syncState(status, start, stop, generate);
                        Toast.makeText(
                                host, "已保存到 " + report.displayPath, Toast.LENGTH_LONG).show();
                        share(report);
                    });
                } catch (Exception error) {
                    host.runOnUiThread(() -> {
                        setBusy(generate, secondaryActions, true);
                        syncState(status, start, stop, generate);
                        status.setText("生成失败：" + error.getClass().getSimpleName());
                        Toast.makeText(host, "诊断报告生成失败", Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
        clear.setOnClickListener(v -> {
            DiagnosticReport.clear(host);
            syncState(status, start, stop, generate);
            Toast.makeText(host, "诊断记录已清除", Toast.LENGTH_SHORT).show();
        });
        syncState(status, start, stop, generate);
        return card;
    }

    public void onDestroy() {
        executor.shutdownNow();
    }

    private void share(DiagnosticReport.PublishedReport report) {
        Uri uri = report.uri;
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("application/zip")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, "OneLab 诊断报告")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("OneLab 诊断报告", uri));
        host.startActivity(Intent.createChooser(intent, "分享诊断报告"));
    }

    private String statusText() {
        if (DiagnosticReport.isRecording(host)) return "正在记录本次复现环境";
        if (DiagnosticReport.hasCompletedSession(host)) return "记录已停止，可以生成报告";
        String latest = DiagnosticReport.latestReportName(host);
        if (latest != null) return "已有报告：" + latest;
        return "尚未开始记录";
    }

    private void syncState(
            MaterialTextView status,
            MaterialButton start,
            MaterialButton stop,
            MaterialButton generate) {
        boolean recording = DiagnosticReport.isRecording(host);
        boolean completed = DiagnosticReport.hasCompletedSession(host);
        status.setText(statusText());
        start.setEnabled(!recording);
        stop.setEnabled(recording);
        generate.setEnabled(completed);
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMarginEnd(ui.dp(6));
        return params;
    }

    private void setBusy(
            MaterialButton primary, LinearLayout actions, boolean enabled) {
        primary.setEnabled(enabled);
        for (int index = 0; index < actions.getChildCount(); index++) {
            actions.getChildAt(index).setEnabled(enabled);
        }
    }
}
