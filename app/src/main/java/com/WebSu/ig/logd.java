package com.WebSu.ig;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.core.content.ContextCompat;

public class logd extends View {

        private boolean isChecked = false;
        private boolean isEnabled = true;
        private boolean isPressedDown = false;

        private Paint paintBackground;
        private Paint paintCircle;
        private Paint paintBorder;

        private OnCheckedChangeListener listener;

        private float thumbScale = 0.75f;
        private float pressScale = 1.0f;
        private float trackPressAlpha = 0.15f;

        public logd(Context context) {
                super(context);
                init();
            }

        public logd(Context context, AttributeSet attrs) {
                super(context, attrs);
                init();
            }

        public logd(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
                init();
            }

        private void init() {
                setLayerType(LAYER_TYPE_SOFTWARE, null);

                paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintBackground.setStyle(Paint.Style.FILL);

                paintCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintCircle.setStyle(Paint.Style.FILL);

                paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
                paintBorder.setStyle(Paint.Style.STROKE);
                paintBorder.setStrokeWidth(2.0f);

                setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    if (isEnabled) toggle();
                                }
                        });

                setOnTouchListener(new OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                    if (!isEnabled) return false;
                                    switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN:
                                                isPressedDown = true;
                                                animatePress(true);
                                                break;
                                            case MotionEvent.ACTION_UP:
                                            case MotionEvent.ACTION_CANCEL:
                                                isPressedDown = false;
                                                animatePress(false);
                                                break;
                                        }
                                    return false;
                                }
                        });

                updateColors();
            }

        private void updateColors() {
                boolean dark = (getContext().getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

                int colorBgTrackOff = ContextCompat.getColor(getContext(), R.color.bg_thumb_track);
                int colorThumbOff = ContextCompat.getColor(getContext(), R.color.thumb_off_button);
                int colorShadowOff = ContextCompat.getColor(getContext(), R.color.shadow_off_border);

                if (!isEnabled) {
                        paintBackground.setColor(dark ? Color.parseColor("#1C1B1F") : Color.parseColor("#2C2A2E"));
                        paintCircle.setColor(dark ? Color.parseColor("#8E8E8E") : Color.parseColor("#8E8991"));
                        paintBorder.setColor(dark ? Color.parseColor("#A6A6A6") : Color.parseColor("#A69FA8"));
                        paintBorder.setStrokeWidth(1.5f);
                        thumbScale = 0.35f;

                        paintCircle.clearShadowLayer();

                    } else if (isChecked) {
                        paintBackground.setColor(Color.parseColor("#FF5484A6"));
                        paintCircle.setColor(Color.WHITE);

                        paintBorder.setColor(Color.TRANSPARENT);
                        paintBorder.setStrokeWidth(0f);
                        thumbScale = 0.75f;

                    } else {
                        paintBackground.setColor(colorBgTrackOff);
                        paintCircle.setColor(colorThumbOff);
                        paintBorder.setColor(colorShadowOff);
                        paintBorder.setStrokeWidth(3.5f);
                        thumbScale = 0.50f;
                    }

                if (isPressedDown && isEnabled) {
                        int shadowColor = Color.parseColor("#40000000");
                        paintCircle.setShadowLayer(12f, 0, 4f, shadowColor);

                        paintBackground.setColor(blendColor(paintBackground.getColor(), Color.WHITE, trackPressAlpha));
                    } else {
                        paintCircle.clearShadowLayer();
                    }

                invalidate();
            }

        private void animatePress(final boolean pressed) {
                final float startScale = pressScale;
                final float endScale = pressed ? 1.15f : 1.0f;

                final float startTrack = trackPressAlpha;
                final float endTrack = pressed ? 0.15f : 0f;

                ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
                anim.setDuration(150);
                anim.setInterpolator(new DecelerateInterpolator());
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator a) {
                                    float t = (float) a.getAnimatedValue();

                                    pressScale = startScale + (endScale - startScale) * t;

                                    trackPressAlpha = startTrack + (endTrack - startTrack) * t;

                                    updateColors();
                                }
                        });
                anim.start();
            }

        public void toggle() {
                setChecked(!isChecked);
            }

        public void setChecked(boolean checked) {
                if (this.isChecked != checked) {
                        this.isChecked = checked;
                        updateColors();
                        if (listener != null) {
                                listener.onCheckedChanged(this, isChecked);
                            }
                    }
            }

        public boolean isChecked() {
                return isChecked;
            }

        public void setOnCheckedChangeListener(OnCheckedChangeListener l) {
                this.listener = l;
            }

        @Override
        protected void onDraw(Canvas canvas) {
                float w = getWidth();
                float h = getHeight();
                float radius = h / 2f;

                float thumbRadius = radius * thumbScale * pressScale;

                float cx = isChecked ? (w - radius) : radius;
                float cy = radius;

                canvas.drawRoundRect(0, 0, w, h, radius, radius, paintBackground);

                if (paintBorder.getColor() != Color.TRANSPARENT && paintBorder.getStrokeWidth() > 0) {
                        float border = paintBorder.getStrokeWidth() / 2f;
                        canvas.drawRoundRect(border, border, w - border, h - border,
                                             radius - border, radius - border, paintBorder);
                    }

                canvas.drawCircle(cx, cy, thumbRadius, paintCircle);
            }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
                int minWidth = 80;
                int minHeight = 40;
                setMeasuredDimension(resolveSize(minWidth, widthSpec), resolveSize(minHeight, heightSpec));
            }

        @Override
        public void setEnabled(boolean enabled) {
                this.isEnabled = enabled;
                updateColors();
            }

        @Override
        public boolean isEnabled() {
                return isEnabled;
            }

        private int blendColor(int color1, int color2, float ratio) {
                float inverse = 1 - ratio;
                int a = (int) (Color.alpha(color1) * inverse + Color.alpha(color2) * ratio);
                int r = (int) (Color.red(color1) * inverse + Color.red(color2) * ratio);
                int g = (int) (Color.green(color1) * inverse + Color.green(color2) * ratio);
                int b = (int) (Color.blue(color1) * inverse + Color.blue(color2) * ratio);
                return Color.argb(a, r, g, b);
            }

        public interface OnCheckedChangeListener {
                void onCheckedChanged(View view, boolean isChecked);
            }
    }

