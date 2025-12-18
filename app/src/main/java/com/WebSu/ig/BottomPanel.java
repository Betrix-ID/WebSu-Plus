package com.WebSu.ig;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.Settings; 
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;

public class BottomPanel {
        public interface OnDismissListener {
                void onPanelDismissed();
            }

        private OnDismissListener dismissListener;

        public void setOnDismissListener(OnDismissListener listener) {
                this.dismissListener = listener;
            }

        private final Context ctx;
        private Dialog dialog;
        private LinearLayout panelContentLayout;
        private final int screenHeight;
        private final float density;

        public BottomPanel(Context ctx) {
                this.ctx = ctx;
                this.screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;
                this.density = ctx.getResources().getDisplayMetrics().density;
            }

        public void show() {
                if (dialog != null && dialog.isShowing()) return;

                if (!(ctx instanceof Activity)) {
                        Toast.makeText(ctx, "BottomPanel harus dipanggil dari Activity Context.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                final Activity activity = (Activity) ctx;
                final Dialog dlg = new Dialog(activity);
                dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dlg.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(android.content.DialogInterface dialog) {
                                    if (dismissListener != null) {
                                            dismissListener.onPanelDismissed();
                                        }
                                }
                        });

                try {
                        View customLayout = activity.getLayoutInflater().inflate(R.layout.panel_tm, null);
                        panelContentLayout = (LinearLayout) customLayout;
                    } catch (Exception e) {
                        Toast.makeText(ctx, "Gagal memuat layout panel_tm.xml. Pastikan ID resource sudah benar.", Toast.LENGTH_LONG).show();
                        return;
                    }

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(ContextCompat.getColor(ctx, R.color.colorBackground)); 
                float radius = dp(28);
                bg.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
                panelContentLayout.setBackground(bg);

                panelContentLayout.setPadding(0, 0, 0, 0);
                LinearLayout terminalLayout = panelContentLayout.findViewById(R.id.terminal_options_layout);
                if (terminalLayout != null) {
                        terminalLayout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                            if (ctx instanceof Activity) {
                                                    Intent intent = new Intent(ctx, QuickTerminalActivity.class); 
                                                    ctx.startActivity(intent);
                                                    hide();
                                                } else {
                                                    Toast.makeText(ctx, "Tidak dapat memulai Activity karena Context bukan Activity.", Toast.LENGTH_SHORT).show();
                                                }
                                        }
                                });
                    }

                LinearLayout perizinanLayout = panelContentLayout.findViewById(R.id.izin_options_layout);
                if (perizinanLayout != null) {
                        perizinanLayout.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                            // Menampilkan Toast edukasi
                                            Toast.makeText(ctx,
                                                           "Mengarahkan ke Pengaturan Aplikasi. Silakan 'Paksa Henti' (Force Stop) atau cabut perizinan di manajer SuperUser/Shizuku.",
                                                           Toast.LENGTH_LONG).show();

                                            // Intent untuk membuka halaman App Info aplikasi ini
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                            Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
                                            intent.setData(uri);

                                            try {
                                                    ctx.startActivity(intent);
                                                } catch (Exception e) {
                                                    Toast.makeText(ctx, "Gagal membuka Pengaturan Aplikasi.", Toast.LENGTH_SHORT).show();
                                                }

                                            hide();
                                        }
                                });
                    }

                final ScrollView scrollView = new ScrollView(ctx);
                scrollView.setFillViewport(true);
                scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
                scrollView.setVerticalScrollBarEnabled(false);
                scrollView.addView(panelContentLayout); 

                final FrameLayout wrapper = new FrameLayout(ctx);
                wrapper.setBackgroundColor(0x00000000); 

                FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                wrapper.setLayoutParams(wrapParams);

                FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                wrapper.addView(scrollView, bottomParams);

                final ScrollView panel = scrollView;
                final Dialog dialogReference = dlg;

                panel.post(new Runnable() {
                            @Override
                            public void run() {
                                    panel.setTag(Integer.MAX_VALUE, panel.getY());
                                }
                        });

                panel.setOnTouchListener(new View.OnTouchListener() {

                            float startY;
                            float startPanelY;
                            boolean dragging = false;

                            private float getInitialY() {
                                    Object tag = panel.getTag(Integer.MAX_VALUE);
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
                                                if (panel.getScrollY() == 0 && dy > 0) {

                                                        float newY = startPanelY + dy;
                                                        float initialY = getInitialY();

                                                        if (newY >= initialY) {
                                                                panel.setY(newY);

                                                                float translationFromInitial = newY - initialY;
                                                                float maxTranslation = screenHeight - initialY;
                                                                float opacity = 1f - (translationFromInitial / maxTranslation) * 0.9f;
                                                                if (opacity < 0.1f) opacity = 0.1f;

                                                                if (dialogReference.getWindow() != null) {
                                                                        dialogReference.getWindow().getDecorView().setAlpha(opacity);
                                                                    }

                                                                dragging = true;
                                                                return true; 
                                                            }
                                                    }
                                                break;

                                            case MotionEvent.ACTION_UP:
                                                if (dragging) {

                                                        float finalY = panel.getY();
                                                        float initialY = getInitialY();
                                                        float threshold = initialY + (screenHeight - initialY) * 0.25f;

                                                        if (finalY > threshold) {

                                                                panel.animate()
                                                                    .y(screenHeight)
                                                                    .setDuration(250)
                                                                    .withEndAction(new Runnable() {
                                                                            @Override
                                                                            public void run() {
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
                                                        return true; 
                                                    }
                                                break;
                                        }
                                    return false; 
                                }
                        });

                wrapper.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    dialogReference.dismiss(); 
                                }
                        });

                dlg.setContentView(wrapper);

                Window window = dlg.getWindow();
                if (window != null) {
                        window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                        window.setBackgroundDrawable(new ColorDrawable(0x00000000));
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        );

                        WindowManager.LayoutParams params = window.getAttributes();
                        params.gravity = Gravity.BOTTOM;
                        window.setAttributes(params);

                        window.getAttributes().windowAnimations = android.R.style.Animation_InputMethod;
                    }

                this.dialog = dlg;
                dlg.show();
            }

        private int dp(int value) {
                return Math.round(value * density);
            }

        public void hide() {
                if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss(); 
                    }
            }

        public boolean isShowing() {
                return dialog != null && dialog.isShowing();
            }
    }

