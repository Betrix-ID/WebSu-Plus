package com.WebSu.ig.websuCard;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.WebSu.ig.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.noties.markwon.Markwon;

public class LicenseDialog {

        private static final String TAG = "LicenseDialogManager";
        private static final String LICENSE_URL = "https://raw.githubusercontent.com/github/choosealicense.com/gh-pages/_licenses/gpl-3.0.txt"; 

        private final Context ctx;
        private Dialog dialog;
        private final int screenHeight;
        private final float density;

        private float initialPanelY = -1; 

        private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private final Markwon markwon;

        public LicenseDialog(Context ctx) {
                this.ctx = ctx;
                this.screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;
                this.density = ctx.getResources().getDisplayMetrics().density;

                this.markwon = Markwon.builder(ctx).build();
            }

        public void show() {
                if (dialog != null && dialog.isShowing()) return;

                if (!(ctx instanceof Activity)) {
                        Toast.makeText(ctx, "License Dialog harus dipanggil dari Activity Context.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                final Activity activity = (Activity) ctx;
                final Dialog dlg = new Dialog(activity);
                dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);

                View customLayout;
                try {
                        customLayout = activity.getLayoutInflater().inflate(R.layout.License, null);
                    } catch (Exception e) {
                        Log.e(TAG, "Gagal memuat layout R.layout.License.", e);
                        Toast.makeText(ctx, "Gagal memuat layout Lisensi. Pastikan ID resource sudah benar.", Toast.LENGTH_LONG).show();
                        return;
                    }

                final TextView licenseContent = (TextView) customLayout.findViewById(R.id.license_content);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(ContextCompat.getColor(ctx, R.color.colorBackground)); 
                float radius = dp(28);
                bg.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
                customLayout.setBackgroundDrawable(bg); 

                licenseContent.setText("Memuat konten Lisensi GNU V3..."); 
                loadLicenseContent(licenseContent);

                final ScrollView scrollView = new ScrollView(ctx);
                scrollView.setFillViewport(true);
                scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER); 
                scrollView.setVerticalScrollBarEnabled(false);
                scrollView.addView(customLayout); 

                final FrameLayout wrapper = new FrameLayout(ctx);
                wrapper.setBackgroundColor(0x00000000); 

                FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                wrapper.setLayoutParams(wrapParams);

                FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                wrapper.addView(scrollView, bottomParams);

                final ScrollView panel = scrollView;
                final Dialog dialogReference = dlg;
                final Window window = dlg.getWindow(); 

                // 5. Implementasi Touch Listener (Drag-to-Dismiss)
                panel.setOnTouchListener(new View.OnTouchListener() {

                            float startY;
                            float startPanelY;
                            boolean dragging = false;

                            private float getInitialY() {
                                    if (initialPanelY == -1) {
                                            initialPanelY = panel.getY();
                                        }
                                    return initialPanelY;
                                }

                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                    // DEKLARASI: Deklarasikan initialY di luar switch atau di awal ACTION_UP.
                                    // Kita deklarasikan di sini agar kompiler tahu ia akan selalu diinisialisasi 
                                    // sebelum digunakan di ACTION_UP (melalui getInitialY()).
                                    float initialY = getInitialY(); 

                                    switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN:
                                                startY = event.getRawY();
                                                startPanelY = panel.getY();
                                                dragging = false;
                                                // getInitialY() dipanggil di sini juga, menjaga konsistensi initialPanelY
                                                getInitialY(); 
                                                break;

                                            case MotionEvent.ACTION_MOVE:
                                                float dy = event.getRawY() - startY;

                                                // --- Perbaikan Logic Dragging/Scrolling ---

                                                // KONDISI 1: Jika sudah dalam mode dragging (dismissing)
                                                if (dragging) {
                                                        float newY = startPanelY + dy;

                                                        // Batasi agar tidak bergerak ke atas dari posisi awal
                                                        if (newY < initialY) newY = initialY; 

                                                        panel.setY(newY);

                                                        float translationFromInitial = newY - initialY;
                                                        float maxTranslation = screenHeight - initialY; 

                                                        float opacity = 1f - (translationFromInitial / maxTranslation) * 0.9f;
                                                        if (opacity < 0.1f) opacity = 0.1f;

                                                        if (window != null) {
                                                                window.getDecorView().setAlpha(opacity);
                                                            }

                                                        // Konsumsi event, jangan biarkan ScrollView scroll
                                                        return true; 
                                                    }


                                                // KONDISI 2: Cek apakah harus memulai drag-to-dismiss
                                                if (panel.getScrollY() == 0 && dy > 0) {
                                                        // Ambil alih sentuhan dan inisiasi Dragging

                                                        // Reset startPanelY dan startY agar animasi drag smooth
                                                        startPanelY = panel.getY(); 
                                                        startY = event.getRawY();

                                                        dragging = true;
                                                        return true; // Ambil alih event (menggantikan ScrollView)
                                                    }

                                                break;

                                            case MotionEvent.ACTION_UP:
                                                if (dragging) {
                                                        float finalY = panel.getY();
                                                        // initialY sudah terinisialisasi di awal onTouch()
                                                        float threshold = initialY + (screenHeight - initialY) * 0.25f; 

                                                        if (finalY > threshold) {
                                                                animateDismiss(panel, dialogReference);
                                                            } else {
                                                                animateSnapBack(panel, initialY);
                                                            }

                                                        dragging = false;
                                                        return true; 
                                                    }
                                                break;
                                        }
                                    return false; // Biarkan ScrollView menangani event jika tidak ada dragging aktif.
                                }
                        });

                // 6. Klik di luar panel menutup dialog
                wrapper.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    animateDismiss(panel, dialogReference);
                                }
                        });

                // 7. Pengaturan Window Dialog
                dlg.setContentView(wrapper);

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

        public void hide() {
                if (dialog != null && dialog.isShowing()) dialog.dismiss();
            }

        private int dp(int value) {
                return Math.round(value * density);
            }

        // --- Logika Pemuatan Konten Lisensi (Tidak Berubah) ---

        private void loadLicenseContent(final TextView licenseContent) {
                networkExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                    String content = null;
                                    try {
                                            content = downloadContent(LICENSE_URL);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Gagal mengunduh lisensi", e);
                                            content = "Gagal memuat konten lisensi dari internet. Silakan coba lagi nanti.";
                                        }

                                    final String finalContent = content;
                                    mainHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                        if (finalContent.startsWith("Gagal")) {
                                                                licenseContent.setText(finalContent);
                                                                Toast.makeText(ctx, finalContent, Toast.LENGTH_LONG).show();
                                                            } else {
                                                                markwon.setMarkdown(licenseContent, finalContent.trim());
                                                                licenseContent.setLinksClickable(true); 
                                                            }
                                                    }
                                            });
                                }
                        });
            }

        private String downloadContent(String urlString) throws Exception {
                HttpURLConnection connection = null;
                BufferedReader reader = null;
                StringBuilder content = new StringBuilder();

                try {
                        URL url = new URL(urlString);
                        connection = (HttpURLConnection) url.openConnection();

                        connection.setConnectTimeout(15000);
                        connection.setReadTimeout(15000);

                        connection.connect();

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                String line;
                                while ((line = reader.readLine()) != null) {
                                        content.append(line).append("\n");
                                    }
                                return content.toString();
                            } else {
                                throw new IOException("HTTP error code: " + connection.getResponseCode());
                            }
                    } finally {
                        if (reader != null) {
                                try {
                                        reader.close();
                                    } catch (IOException e) {
                                        Log.e(TAG, "Gagal menutup reader", e);
                                    }
                            }
                        if (connection != null) {
                                connection.disconnect();
                            }
                    }
            }

        // --- Mekanisme Penutupan Baru (Tidak Berubah) ---

        private void animateDismiss(final View panel, final Dialog dialogReference) {
                Window window = dialogReference.getWindow();

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

                if (window != null) {
                        window.getDecorView()
                            .animate()
                            .alpha(0f)
                            .setDuration(250)
                            .start();
                    }
            }

        private void animateSnapBack(final View panel, float initialY) {
                Window window = dialog.getWindow(); 

                panel.animate()
                    .y(initialY)
                    .setDuration(200)
                    .start();

                if (window != null) {
                        window.getDecorView()
                            .animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                    }
            }
    }

