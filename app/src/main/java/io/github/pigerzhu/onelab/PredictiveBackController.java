package io.github.pigerzhu.onelab;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.window.BackEvent;
import android.window.OnBackAnimationCallback;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Bridges Android's predictive-back progress into OneLab's custom page navigation. */
final class PredictiveBackController {
    private static final long CANCEL_ANIMATION_MS = 220L;
    private static final PathInterpolator CANCEL_EASING =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final Activity activity;
    private final Supplier<View> currentView;
    private final Supplier<View> previewView;
    private final BooleanSupplier transitionRunning;
    private final Runnable backAction;
    private final OnBackInvokedCallback callback;

    private View gestureView;
    private View gesturePreview;

    static PredictiveBackController register(
            Activity activity,
            Supplier<View> currentView,
            Supplier<View> previewView,
            BooleanSupplier transitionRunning,
            Runnable backAction
    ) {
        return new PredictiveBackController(
                activity, currentView, previewView, transitionRunning, backAction);
    }

    private PredictiveBackController(
            Activity activity,
            Supplier<View> currentView,
            Supplier<View> previewView,
            BooleanSupplier transitionRunning,
            Runnable backAction
    ) {
        this.activity = activity;
        this.currentView = currentView;
        this.previewView = previewView;
        this.transitionRunning = transitionRunning;
        this.backAction = backAction;
        callback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                ? animationCallback()
                : this::invokeBack;
        activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
    }

    void unregister() {
        activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(callback);
    }

    private OnBackAnimationCallback animationCallback() {
        return new OnBackAnimationCallback() {
            @Override
            public void onBackStarted(BackEvent backEvent) {
                if (transitionRunning.getAsBoolean()) return;
                gestureView = currentView.get();
                gesturePreview = previewView.get();
                if (gestureView != null) {
                    gestureView.animate().cancel();
                }
                if (gesturePreview != null) {
                    gesturePreview.animate().cancel();
                    gesturePreview.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onBackProgressed(BackEvent backEvent) {
                applyProgress(backEvent.getProgress());
            }

            @Override
            public void onBackCancelled() {
                resetGestureView(true);
            }

            @Override
            public void onBackInvoked() {
                invokeBack();
            }
        };
    }

    private void applyProgress(float progress) {
        View view = gestureView;
        if (view == null || view != currentView.get() || transitionRunning.getAsBoolean()) {
            return;
        }
        float clamped = Math.max(0f, Math.min(1f, progress));
        float eased = 1f - (float) Math.pow(1f - clamped, 3);
        float width = view.getWidth();
        float maxTranslation = Math.min(width * 0.28f, dp(220));
        view.setTranslationX(maxTranslation * eased);
        float scale = 1f - (0.035f * eased);
        view.setScaleX(scale);
        view.setScaleY(scale);

        View preview = gesturePreview;
        if (preview != null && preview == previewView.get()) {
            preview.setTranslationX((-width * 0.16f) * (1f - eased));
            preview.setAlpha(0.92f + (0.08f * eased));
        }
    }

    private void invokeBack() {
        View before = currentView.get();
        backAction.run();
        if (!activity.isFinishing() && currentView.get() == before) {
            resetGestureView(true);
        } else {
            gestureView = null;
            gesturePreview = null;
        }
    }

    private void resetGestureView(boolean animate) {
        View view = gestureView;
        View preview = gesturePreview;
        gestureView = null;
        gesturePreview = null;
        if (view == null) {
            resetPreview(preview, animate);
            return;
        }
        view.animate().cancel();
        if (!animate) {
            view.setTranslationX(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
            resetPreview(preview, false);
            return;
        }
        view.animate()
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(CANCEL_ANIMATION_MS)
                .setInterpolator(CANCEL_EASING)
                .start();
        resetPreview(preview, true);
    }

    private void resetPreview(View preview, boolean animate) {
        if (preview == null) return;
        preview.animate().cancel();
        float restingTranslation = -activity.getResources()
                .getDisplayMetrics().widthPixels * 0.16f;
        if (!animate) {
            preview.setTranslationX(restingTranslation);
            preview.setAlpha(0.92f);
            return;
        }
        preview.animate()
                .translationX(restingTranslation)
                .alpha(0.92f)
                .setDuration(CANCEL_ANIMATION_MS)
                .setInterpolator(CANCEL_EASING)
                .start();
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
