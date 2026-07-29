package io.github.pigerzhu.onelab;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;

/** Owns OneLab's page-to-page animation so gesture takeover has one animation source. */
final class PageTransitionController {
    private static final long DURATION_MS = 360L;
    private static final PathInterpolator EASING =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private ValueAnimator animator;

    boolean isRunning() {
        return animator != null && animator.isRunning();
    }

    void animateIn(View previousPage, View nextPage, int width) {
        interrupt();
        nextPage.setTranslationX(width);
        nextPage.setScaleX(1f);
        nextPage.setScaleY(1f);
        nextPage.setAlpha(1f);
        previousPage.setTranslationX(0f);
        previousPage.setScaleX(1f);
        previousPage.setScaleY(1f);
        previousPage.setAlpha(1f);

        start(progress -> {
            previousPage.setTranslationX(lerp(0f, -width * 0.16f, progress));
            previousPage.setAlpha(lerp(1f, 0.92f, progress));
            nextPage.setTranslationX(lerp(width, 0f, progress));
        }, null);
    }

    void animateOut(
            ViewGroup pageHost,
            View leavingPage,
            View restoredPage,
            int width
    ) {
        interrupt();
        float leavingTranslation = leavingPage.getTranslationX();
        float leavingScaleX = leavingPage.getScaleX();
        float leavingScaleY = leavingPage.getScaleY();
        float restoredTranslation = restoredPage.getTranslationX();
        float restoredAlpha = restoredPage.getAlpha();
        float restoredScaleX = restoredPage.getScaleX();
        float restoredScaleY = restoredPage.getScaleY();

        restoredPage.setVisibility(View.VISIBLE);
        leavingPage.bringToFront();
        start(progress -> {
            leavingPage.setTranslationX(lerp(leavingTranslation, width, progress));
            leavingPage.setScaleX(lerp(leavingScaleX, 1f, progress));
            leavingPage.setScaleY(lerp(leavingScaleY, 1f, progress));
            restoredPage.setTranslationX(lerp(restoredTranslation, 0f, progress));
            restoredPage.setAlpha(lerp(restoredAlpha, 1f, progress));
            restoredPage.setScaleX(lerp(restoredScaleX, 1f, progress));
            restoredPage.setScaleY(lerp(restoredScaleY, 1f, progress));
        }, () -> pageHost.removeView(leavingPage));
    }

    void interrupt() {
        if (animator == null) return;
        ValueAnimator running = animator;
        animator = null;
        running.cancel();
    }

    private void start(FrameUpdater updater, Runnable completion) {
        ValueAnimator nextAnimator = ValueAnimator.ofFloat(0f, 1f);
        animator = nextAnimator;
        nextAnimator.setDuration(DURATION_MS);
        nextAnimator.setInterpolator(EASING);
        nextAnimator.addUpdateListener(animation ->
                updater.update((float) animation.getAnimatedValue()));
        nextAnimator.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (animator == animation) animator = null;
                if (!canceled && completion != null) completion.run();
            }
        });
        nextAnimator.start();
    }

    private static float lerp(float start, float end, float progress) {
        return start + ((end - start) * progress);
    }

    private interface FrameUpdater {
        void update(float progress);
    }
}
