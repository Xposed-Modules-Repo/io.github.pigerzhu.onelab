package io.github.pigerzhu.onelab.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;

import androidx.annotation.DrawableRes;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textview.MaterialTextView;

public final class Ui {
    public static final int HOME_NETWORK = 0;
    public static final int HOME_PERFORMANCE = 1;
    public static final int HOME_SYSTEM = 2;
    public static final int HOME_APPS = 3;
    public static final int HOME_EXPERIMENTS = 4;

    private final Context context;
    private final boolean nightMode;

    public final int colorSurface;
    public final int colorSurfaceContainer;
    public final int colorOnSurface;
    public final int colorOnSurfaceVariant;
    public final int colorPrimary;
    public final int colorPrimaryContainer;
    public final int colorOnPrimaryContainer;

    /** Tracks whether a UI sync is in progress; checked by toggle-group listeners. */
    public boolean syncingUi;

    public Ui(Context context) {
        this.context = context;
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        nightMode = mode == Configuration.UI_MODE_NIGHT_YES;
        colorSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF);
        colorSurfaceContainer = MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceContainer, fallbackContainer());
        colorOnSurface = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F);
        colorOnSurfaceVariant = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF49454F);
        colorPrimary = MaterialColors.getColor(context, android.R.attr.colorAccent, 0xFF6750A4);
        colorPrimaryContainer = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorPrimaryContainer, 0xFFEADDFF);
        colorOnPrimaryContainer = MaterialColors.getColor(context,
                com.google.android.material.R.attr.colorOnPrimaryContainer, 0xFF21005D);
    }

    private int fallbackContainer() {
        int mode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES ? 0xFF211F26 : 0xFFF3EDF7;
    }

    public MaterialTextView text(String value, int sp, boolean bold, int color) {
        MaterialTextView view = new MaterialTextView(context);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1.0f);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return view;
    }

    public MaterialCardView card() {
        MaterialCardView card = new MaterialCardView(context);
        card.setRadius(dp(16));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setStrokeColor(MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, 0x1F000000));
        card.setCardBackgroundColor(colorSurfaceContainer);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);
        return card;
    }

    public LinearLayout cardBody() {
        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(16), dp(18), dp(16));
        return body;
    }

    public View placeholderCard(String message) {
        MaterialCardView card = card();
        LinearLayout body = cardBody();
        card.addView(body);
        body.addView(text(message, 14, false, colorOnSurfaceVariant));
        return card;
    }

    public LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    public void addSpace(LinearLayout parent, int dp) {
        Space space = new Space(context);
        parent.addView(space, new LinearLayout.LayoutParams(1, dp(dp)));
    }

    public int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public View homeButton(
            @DrawableRes int iconRes,
            int palette,
            String title,
            String subtitle,
            View.OnClickListener listener
    ) {
        MaterialCardView card = card();
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(listener);

        LinearLayout body = cardBody();
        body.setOrientation(LinearLayout.HORIZONTAL);
        body.setGravity(Gravity.CENTER_VERTICAL);
        body.setPadding(dp(16), dp(15), dp(14), dp(15));
        card.addView(body);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(homeIconColor(palette));
        icon.setPadding(dp(13), dp(13), dp(13), dp(13));
        GradientDrawable iconBackground = new GradientDrawable();
        iconBackground.setColor(homeIconBackground(palette));
        iconBackground.setCornerRadius(dp(14));
        icon.setBackground(iconBackground);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(52), dp(52));
        iconParams.setMarginEnd(dp(16));
        body.addView(icon, iconParams);

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        body.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        MaterialTextView titleView = text(title, 20, true, colorOnSurface);
        copy.addView(titleView);
        if (subtitle != null && !subtitle.isEmpty()) {
            copy.addView(text(subtitle, 14, false, colorOnSurfaceVariant));
        }

        MaterialTextView arrow = text(">", 28, false, colorOnSurfaceVariant);
        arrow.setGravity(Gravity.CENTER);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        body.addView(arrow, new LinearLayout.LayoutParams(dp(32), dp(48)));
        return card;
    }

    public MaterialButton actionButton(String label) {
        MaterialButton button = new MaterialButton(context);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        return button;
    }

    public View switchRow(String title, String subtitle, MaterialSwitch toggle) {
        return switchRow(title, subtitle, toggle, 16);
    }

    public View switchRow(
            String title,
            String subtitle,
            MaterialSwitch toggle,
            int titleTextSize
    ) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(8));

        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        row.addView(copy, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        copy.addView(text(title, titleTextSize, true, colorOnSurface));
        if (subtitle != null && !subtitle.isEmpty()) {
            copy.addView(text(subtitle, 13, false, colorOnSurfaceVariant));
        }
        row.addView(toggle);
        return row;
    }

    private int homeIconColor(int palette) {
        int[] light = {0xFF00677A, 0xFF8A4E00, 0xFF6542A5, 0xFF196B43, 0xFF9B3B68};
        int[] dark = {0xFF6ED8EA, 0xFFFFC66A, 0xFFC9ADFF, 0xFF78D9A7, 0xFFF3A0C5};
        return (nightMode ? dark : light)[safePalette(palette)];
    }

    private int homeIconBackground(int palette) {
        int[] light = {0xFFD7F3F8, 0xFFFFE7C2, 0xFFECE0FF, 0xFFD9F3E3, 0xFFFFE0EC};
        int[] dark = {0xFF153C46, 0xFF493719, 0xFF372C4B, 0xFF183E2D, 0xFF48263A};
        return (nightMode ? dark : light)[safePalette(palette)];
    }

    private int safePalette(int palette) {
        return Math.max(HOME_NETWORK, Math.min(HOME_EXPERIMENTS, palette));
    }
}
