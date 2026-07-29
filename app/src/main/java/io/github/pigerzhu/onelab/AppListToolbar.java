package io.github.pigerzhu.onelab;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.function.Consumer;

import io.github.pigerzhu.onelab.ui.Ui;

/** Fixed app-picker toolbar, including its transient search mode. */
final class AppListToolbar {
    static final int SORT_NAME = 0;
    static final int SORT_RECENTLY_INSTALLED = 1;
    static final int SORT_RECENTLY_UPDATED = 2;
    private static final int MENU_DESCENDING = 100;

    interface SortListener {
        void onSortChanged(int sortMode, boolean descending);
    }

    private final MainActivity host;
    private final Ui ui;
    private final Runnable parentBackAction;
    private final Consumer<String> queryChanged;
    private final SortListener sortChanged;

    private final LinearLayout view;
    private final TextView titleView;
    private final EditText searchInput;
    private final ImageButton searchButton;
    private final ImageButton moreButton;
    private final ImageButton closeSearchButton;
    private int sortMode;
    private boolean descending;
    private boolean searching;

    AppListToolbar(
            MainActivity host,
            Ui ui,
            String title,
            Runnable parentBackAction,
            int initialSortMode,
            boolean initialDescending,
            Consumer<String> queryChanged,
            SortListener sortChanged
    ) {
        this.host = host;
        this.ui = ui;
        this.parentBackAction = parentBackAction;
        this.sortMode = initialSortMode;
        this.descending = initialDescending;
        this.queryChanged = queryChanged;
        this.sortChanged = sortChanged;

        view = new LinearLayout(host);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setOrientation(LinearLayout.HORIZONTAL);

        ImageButton backButton = button(R.drawable.ic_arrow_back, "返回");
        view.addView(backButton, new LinearLayout.LayoutParams(ui.dp(52), ui.dp(52)));

        titleView = ui.text(
                title == null || title.isEmpty() ? "选择应用" : title,
                28, true, ui.colorOnSurface);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        titleParams.setMarginStart(ui.dp(10));
        view.addView(titleView, titleParams);

        searchInput = new EditText(host);
        searchInput.setSingleLine(true);
        searchInput.setHint("搜索应用");
        searchInput.setTextSize(20);
        searchInput.setTextColor(ui.colorOnSurface);
        searchInput.setHintTextColor(ui.colorOnSurfaceVariant);
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setPadding(0, 0, ui.dp(8), 0);
        searchInput.setVisibility(View.GONE);
        view.addView(searchInput, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        searchButton = button(R.drawable.ic_search, "搜索");
        moreButton = button(R.drawable.ic_more_vert, "排序");
        closeSearchButton = button(R.drawable.ic_close, "关闭搜索");
        closeSearchButton.setVisibility(View.GONE);
        view.addView(searchButton, new LinearLayout.LayoutParams(ui.dp(52), ui.dp(52)));
        view.addView(moreButton, new LinearLayout.LayoutParams(ui.dp(44), ui.dp(52)));
        view.addView(closeSearchButton, new LinearLayout.LayoutParams(ui.dp(52), ui.dp(52)));

        backButton.setOnClickListener(v -> {
            if (searching) {
                closeSearch();
            } else if (parentBackAction != null) {
                host.nestedBackAction = null;
                parentBackAction.run();
            }
        });
        closeSearchButton.setOnClickListener(v -> closeSearch());
        searchButton.setOnClickListener(v -> openSearch());
        moreButton.setOnClickListener(v -> showSortMenu());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence value, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence value, int start, int before, int count) {
                queryChanged.accept(value == null ? "" : value.toString());
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
    }

    View view() {
        return view;
    }

    private void openSearch() {
        searching = true;
        host.setPredictiveParentPreviewEnabled(false);
        titleView.setVisibility(View.GONE);
        searchButton.setVisibility(View.GONE);
        moreButton.setVisibility(View.GONE);
        searchInput.setVisibility(View.VISIBLE);
        closeSearchButton.setVisibility(View.VISIBLE);
        host.nestedBackAction = this::closeSearch;
        searchInput.requestFocus();
        keyboard().showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);
    }

    private void closeSearch() {
        if (!searching) return;
        searching = false;
        host.setPredictiveParentPreviewEnabled(true);
        searchInput.setText("");
        searchInput.setVisibility(View.GONE);
        closeSearchButton.setVisibility(View.GONE);
        titleView.setVisibility(View.VISIBLE);
        searchButton.setVisibility(View.VISIBLE);
        moreButton.setVisibility(View.VISIBLE);
        host.nestedBackAction = parentBackAction;
        keyboard().hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
    }

    private void showSortMenu() {
        PopupMenu popup = new PopupMenu(host, moreButton);
        popup.getMenu().add(1, SORT_NAME, 0, "按名称排序")
                .setCheckable(true)
                .setChecked(sortMode == SORT_NAME);
        popup.getMenu().add(1, SORT_RECENTLY_INSTALLED, 1, "按安装时间排序")
                .setCheckable(true)
                .setChecked(sortMode == SORT_RECENTLY_INSTALLED);
        popup.getMenu().add(1, SORT_RECENTLY_UPDATED, 2, "按更新时间排序")
                .setCheckable(true)
                .setChecked(sortMode == SORT_RECENTLY_UPDATED);
        popup.getMenu().setGroupCheckable(1, true, true);
        popup.getMenu().add(2, MENU_DESCENDING, 3, "倒序排列")
                .setCheckable(true)
                .setChecked(descending);
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_DESCENDING) {
                descending = !descending;
            } else {
                sortMode = item.getItemId();
            }
            sortChanged.onSortChanged(sortMode, descending);
            return true;
        });
        popup.show();
    }

    private InputMethodManager keyboard() {
        return (InputMethodManager) host.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    private ImageButton button(int iconRes, String description) {
        ImageButton button = new ImageButton(host);
        button.setImageResource(iconRes);
        button.setImageTintList(ColorStateList.valueOf(ui.colorOnSurface));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(ui.dp(13), ui.dp(13), ui.dp(13), ui.dp(13));
        button.setContentDescription(description);
        button.setTooltipText(description);
        return button;
    }
}
