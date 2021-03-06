/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.widget.ImageView;

import java.util.function.Consumer;

public class NotificationDozeHelper {
    private final ColorMatrix mGrayscaleColorMatrix = new ColorMatrix();

    public void fadeGrayscale(final ImageView target, final boolean dark, long delay) {
        startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updateGrayscale(target, (float) animation.getAnimatedValue());
            }
        }, dark, delay, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!dark) {
                    target.setColorFilter(null);
                }
            }
        });
    }

    public void updateGrayscale(ImageView target, boolean dark) {
        updateGrayscale(target, dark ? 1 : 0);
    }

    public void updateGrayscale(ImageView target, float darkAmount) {
        if (darkAmount > 0) {
            updateGrayscaleMatrix(darkAmount);
            target.setColorFilter(new ColorMatrixColorFilter(mGrayscaleColorMatrix));
        } else {
            target.setColorFilter(null);
        }
    }

    public void startIntensityAnimation(ValueAnimator.AnimatorUpdateListener updateListener,
            boolean dark, long delay, Animator.AnimatorListener listener) {
        float startIntensity = dark ? 0f : 1f;
        float endIntensity = dark ? 1f : 0f;
        ValueAnimator animator = ValueAnimator.ofFloat(startIntensity, endIntensity);
        animator.addUpdateListener(updateListener);
        animator.setDuration(700 /*NotificationPanelView.DOZE_ANIMATION_DURATION*/);
        animator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        animator.setStartDelay(delay);
        if (listener != null) {
            animator.addListener(listener);
        }
        animator.start();
    }

    public void setIntensityDark(final Consumer<Float> listener, final boolean dark,
                                 boolean animate, final long delay) {
        /*if (animate) {
            startIntensityAnimation(a -> listener.accept((Float) a.getAnimatedValue()), dark, delay,
                    null *//* listener *//*);
            startIntensityAnimation(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    listener.accept((Float) animation.getAnimatedValue());
                }
            }, dark, delay, null *//* listener *//*);
        } else {
            listener.accept(dark ? 1f : 0f);
        }*/ //TODO finish
    }

    public void updateGrayscaleMatrix(float intensity) {
        mGrayscaleColorMatrix.setSaturation(1 - intensity);
    }

    public ColorMatrix getGrayscaleColorMatrix() {
        return mGrayscaleColorMatrix;
    }
}
