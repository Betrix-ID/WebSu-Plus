package com.WebSu.ig.websuCard;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences; 
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.WindowManager.LayoutParams; 
import android.widget.FrameLayout;
import android.widget.ScrollView;
import com.WebSu.ig.R;
import com.WebSu.ig.logd; 
import android.content.DialogInterface; 

public class PluginSetings {

        public interface OnSettingsStateListener {
                void onSettingsOpened();
                void onSettingsClosed();
            }
       
        private static final int START_Y_TAG_ID = 123456789;
        private static final String PREFS_NAME = "settings";
        private static final String KEY_DEVELOPER_OPTIONS = "enable_developer_options"; 
        private static final String KEY_ENABLE_API_JS = "enable_api_js"; 

        private static int dp(Context ctx, int value) {
                float density = ctx.getResources().getDisplayMetrics().density;
                return Math.round(value * density);
            }

        // Ubah tanda tangan metode show() untuk menerima listener
        public static void show(final Activity activity, final OnSettingsStateListener listener) {

                if (activity == null) return;

                // 1. Panggil listener saat dibuka
                if (listener != null) {
                        listener.onSettingsOpened();
                    }

                final Dialog dialog = new Dialog(activity);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                // 2. Panggil listener saat dialog ditutup
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                    if (listener != null) {
                                            listener.onSettingsClosed();
                                        }
                                }
                        });

                WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                final int screenHeight = size.y;

                final View panelContentLayout = activity.getLayoutInflater().inflate(
                    R.layout.bottom_sheet_developer_options, null);

                final SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

                // --- Logika untuk switch developer options ---
                final logd devSwitch = panelContentLayout.findViewById(R.id.switch_developer_options);

                if (devSwitch != null) {
                        boolean isDevEnabled = prefs.getBoolean(KEY_DEVELOPER_OPTIONS, false);
                        devSwitch.setChecked(isDevEnabled);
                        devSwitch.setOnCheckedChangeListener(new logd.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(View view, boolean isChecked) {
                                            prefs.edit().putBoolean(KEY_DEVELOPER_OPTIONS, isChecked).apply();
                                        }
                                });
                    }

                final logd apiJsSwitch = panelContentLayout.findViewById(R.id.switch_Enable_Api_js); 

                if (apiJsSwitch != null) {
                        boolean isApiJsEnabled = prefs.getBoolean(KEY_ENABLE_API_JS, false);
                        apiJsSwitch.setChecked(isApiJsEnabled);
                        apiJsSwitch.setOnCheckedChangeListener(new logd.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(View view, boolean isChecked) {
                                            prefs.edit().putBoolean(KEY_ENABLE_API_JS, isChecked).apply();
                                        }
                                });
                    }

                final ScrollView scrollView = new ScrollView(activity);
                scrollView.setFillViewport(true);
                scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
                scrollView.setVerticalScrollBarEnabled(false);
                scrollView.addView(panelContentLayout);

                final FrameLayout wrapper = new FrameLayout(activity);
                wrapper.setBackgroundColor(0x00000000);
                FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                wrapper.setLayoutParams(wrapParams);

                FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                wrapper.addView(scrollView, bottomParams);

                dialog.setContentView(wrapper);

                final ScrollView panel = scrollView;
                final Dialog dialogReference = dialog;

                panel.post(new Runnable() {
                            @Override
                            public void run() {
                                    panel.setTag(START_Y_TAG_ID, panel.getY());
                                }
                        });

                wrapper.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                            @Override
                            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                                    int bottomInset = insets.getSystemWindowInsetBottom();
                                    int finalPadding = bottomInset + dp(activity, 16); 

                                    v.setPadding(
                                        v.getPaddingLeft(),
                                        v.getPaddingTop(), 
                                        v.getPaddingRight(),
                                        finalPadding
                                    );

                                    return insets.consumeSystemWindowInsets();
                                }
                        });

                panel.setOnTouchListener(new View.OnTouchListener() {

                            float startY;
                            float startPanelY;
                            boolean dragging = false;

                            private float getStartPanelY() {
                                    Object tag = panel.getTag(START_Y_TAG_ID);
                                    // Ambil posisi Y awal dari tag atau posisi Y saat ini sebagai fallback
                                    return tag instanceof Float ? ((Float) tag) : panel.getY();
                                }

                            @Override
                            public boolean onTouch(View v, MotionEvent event) {

                                    switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN:
                                                startY = event.getRawY();
                                                startPanelY = panel.getY();
                                                dragging = false;
                                                break;

                                            case MotionEvent.ACTION_MOVE:
                                                float dy = event.getRawY() - startY;

                                                // Hanya izinkan drag ke bawah jika ScrollView berada di paling atas
                                                if (panel.getScrollY() == 0 && dy > 0) {

                                                        float newY = startPanelY + dy;
                                                        float initialY = getStartPanelY();

                                                        if (newY >= initialY) {
                                                                panel.setY(newY);

                                                                // Hitung opasitas saat dragging
                                                                float translationFromInitial = newY - initialY;
                                                                float opacity = 1f - (translationFromInitial / (screenHeight - initialY));

                                                                if (dialogReference.getWindow() != null) {
                                                                        dialogReference.getWindow().getDecorView().setAlpha(opacity);
                                                                    }

                                                                dragging = true;
                                                                return true; // Konsumsi event sentuhan
                                                            }
                                                    }
                                                break;

                                            case MotionEvent.ACTION_UP:
                                                if (dragging) {

                                                        float finalY = panel.getY();
                                                        float initialY = getStartPanelY();
                                                        // Tentukan ambang batas untuk dismiss (misalnya 25% dari sisa layar)
                                                        float threshold = initialY + (screenHeight - initialY) * 0.25f;

                                                        if (finalY > threshold) {
                                                                // Dismiss (geser keluar ke bawah)
                                                                panel.animate()
                                                                    .y(screenHeight)
                                                                    .setDuration(250)
                                                                    .withEndAction(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                    // Memanggil dismiss akan memicu OnDismissListener
                                                                                    dialogReference.dismiss();
                                                                                }
                                                                        })
                                                                    .start();

                                                                dialogReference.getWindow()
                                                                    .getDecorView()
                                                                    .animate()
                                                                    .alpha(0f)
                                                                    .setDuration(250)
                                                                    .start();

                                                            } else {
                                                                // Snap back (kembali ke posisi awal)
                                                                panel.animate()
                                                                    .y(initialY)
                                                                    .setDuration(200)
                                                                    .start();

                                                                dialogReference.getWindow()
                                                                    .getDecorView()
                                                                    .animate()
                                                                    .alpha(1f)
                                                                    .setDuration(200)
                                                                    .start();
                                                            }

                                                        dragging = false;
                                                        return true; // Konsumsi event sentuhan
                                                    }
                                                break;
                                        }

                                    return false; // Biarkan event sentuhan lain diteruskan (misalnya untuk scrolling)
                                }
                        });

                wrapper.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    // Menutup dialog jika area di luar panel disentuh
                                    // Memanggil dismiss akan memicu OnDismissListener
                                    dialog.dismiss();
                                }
                        });

                Window window = dialog.getWindow();
                if (window != null) {
                        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        window.setBackgroundDrawable(new ColorDrawable(0x00000000));
                        window.setFlags(
                            LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                            LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        );

                        WindowManager.LayoutParams params = window.getAttributes();
                        params.gravity = Gravity.BOTTOM; 

                        window.setAttributes(params);
                        window.getAttributes().windowAnimations = android.R.style.Animation_InputMethod;
                    }

                dialog.show();
            }
    }

