package io.github.pigerzhu.onelab.ui;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;

public final class ChoiceGroup extends LinearLayout {
    public interface OnChoiceChangedListener {
        void onChoiceChanged(int value);
    }

    private final Ui ui;
    private final List<Option> options = new ArrayList<>();
    private OnChoiceChangedListener listener;
    private int selectedValue = Integer.MIN_VALUE;

    public ChoiceGroup(Context context, Ui ui) {
        super(context);
        this.ui = ui;
        setOrientation(VERTICAL);
    }

    public void addOption(String title, String description, int value) {
        if (!options.isEmpty()) {
            View divider = new View(getContext());
            divider.setBackgroundColor(MaterialColors.getColor(getContext(),
                    com.google.android.material.R.attr.colorOutlineVariant, 0x1F000000));
            addView(divider, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ui.dp(1)));
        }

        LinearLayout row = new LinearLayout(getContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(HORIZONTAL);
        row.setMinimumHeight(ui.dp(description == null || description.isEmpty() ? 56 : 68));
        row.setPadding(ui.dp(4), ui.dp(6), 0, ui.dp(6));
        row.setClickable(true);
        row.setFocusable(true);
        TypedValue selectable = new TypedValue();
        if (getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, selectable, true)) {
            row.setBackgroundResource(selectable.resourceId);
        }

        LinearLayout copy = new LinearLayout(getContext());
        copy.setOrientation(VERTICAL);
        copy.addView(ui.text(title, 16, true, ui.colorOnSurface));
        if (description != null && !description.isEmpty()) {
            copy.addView(ui.text(description, 13, false, ui.colorOnSurfaceVariant));
        }
        row.addView(copy, new LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        MaterialRadioButton radio = new MaterialRadioButton(getContext());
        radio.setClickable(false);
        radio.setFocusable(false);
        row.addView(radio, new LayoutParams(ui.dp(48), ui.dp(48)));

        Option option = new Option(value, row, radio);
        options.add(option);
        row.setOnClickListener(v -> select(value, true));
        addView(row, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void setValue(int value) {
        select(value, false);
    }

    public int value() {
        return selectedValue;
    }

    public void setOnChoiceChangedListener(OnChoiceChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Option option : options) {
            option.row.setEnabled(enabled);
            option.radio.setEnabled(enabled);
            option.row.setAlpha(enabled ? 1f : 0.45f);
        }
    }

    private void select(int value, boolean notify) {
        if (selectedValue == value) {
            return;
        }
        selectedValue = value;
        for (Option option : options) {
            option.radio.setChecked(option.value == value);
        }
        if (notify && listener != null) {
            listener.onChoiceChanged(value);
        }
    }

    private static final class Option {
        final int value;
        final LinearLayout row;
        final MaterialRadioButton radio;

        Option(int value, LinearLayout row, MaterialRadioButton radio) {
            this.value = value;
            this.row = row;
            this.radio = radio;
        }
    }
}
