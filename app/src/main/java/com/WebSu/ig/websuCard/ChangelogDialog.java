package com.WebSu.ig.websuCard;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.LinkMovementMethod;
import io.noties.markwon.Markwon;
import com.WebSu.ig.R;

public class ChangelogDialog extends Dialog {

        private final Context ctx;
        private final int screenHeight;
        private final float density;

        private String title;
        private String changelogContent;
        private View.OnClickListener installButtonListener;
        private View.OnClickListener dismissButtonListener;
        private final boolean isMinimizedMode;
        private Markwon markwon; 

        public ChangelogDialog(Context context, String title, String changelogContent, boolean isMinimizedMode) {
                super(context);
                this.ctx = context;
                this.title = title;
                this.changelogContent = changelogContent;
                this.screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;
                this.density = ctx.getResources().getDisplayMetrics().density;
                this.isMinimizedMode = isMinimizedMode;
                this.markwon = Markwon.builder(context)
                    .build();
            }

        public ChangelogDialog(Context context, String title, String changelogContent) {
                this(context, title, changelogContent, false); 
            }


        public void setInstallButtonListener(View.OnClickListener listener) {
                this.installButtonListener = listener;
            }

        public void setDismissButtonListener(View.OnClickListener listener) {
                this.dismissButtonListener = listener;
            }

        private int dp(int value) {
                return Math.round(value * density);
            }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                if (!(ctx instanceof Activity)) {
                        Toast.makeText(ctx, "ChangelogDialog harus dipanggil dari Activity Context.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                final Dialog dialogReference = this;
                View panelContentLayout = ((Activity) ctx).getLayoutInflater().inflate(R.layout.Changelog, null);

                final ScrollView scrollView = new ScrollView(ctx);
                scrollView.setFillViewport(true);
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

                setContentView(wrapper); 
                final Window window = getWindow();

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


                TextView titleTextView = (TextView) panelContentLayout.findViewById(R.id.dialog_title);
                final TextView contentTextView = (TextView) panelContentLayout.findViewById(R.id.changelog_content);
                Button dismissButton = (Button) panelContentLayout.findViewById(R.id.btn_dismiss); 
                Button installButton = (Button) panelContentLayout.findViewById(R.id.btn_up); 

                if (titleTextView != null) titleTextView.setText(title);
                if (contentTextView != null) {
                        // 1. Render konten Markdown
                        markwon.setMarkdown(contentTextView, changelogContent);

                        // 2. AKTIVASI LINK: Ini mutlak diperlukan untuk ClickableSpan Markwon.
                        contentTextView.setMovementMethod(LinkMovementMethod.getInstance());

                        // 3. SET CLICKABLE: Memastikan TextView merespons sentuhan link
                        contentTextView.setClickable(true);

                        // 4. Set Focusable agar dapat menerima sentuhan untuk link
                        contentTextView.setFocusable(true);
                    }

                if (isMinimizedMode) {
                        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                        wrapper.setOnTouchListener(null);
                        scrollView.setOnTouchListener(null);
                    } else {
                        final ScrollView panel = scrollView;
                        panel.post(new Runnable() {
                                    @Override
                                    public void run() {
                                            // Memastikan nilai Y awal terinisialisasi setelah layout
                                            panel.setTag(Integer.MAX_VALUE, panel.getY());
                                        }
                                });

                        panel.setOnTouchListener(new View.OnTouchListener() {
                                    float startY;
                                    float startPanelY;
                                    boolean dragging = false;
                                    float initialX; // Untuk mendeteksi deviasi horizontal

                                    private float getInitialY() {
                                            Object tag = panel.getTag(Integer.MAX_VALUE);
                                            return tag instanceof Float ? ((Float) tag) : panel.getY();
                                        }

                                    @Override
                                    public boolean onTouch(View v, MotionEvent event) {
                                            // Dapatkan nilai Y awal di awal setiap sentuhan
                                            final float initialY = getInitialY();

                                            switch (event.getAction()) {
                                                    case MotionEvent.ACTION_DOWN:
                                                        startY = event.getRawY();
                                                        initialX = event.getRawX(); // Ambil X awal
                                                        startPanelY = panel.getY();
                                                        dragging = false;
                                                        break;

                                                    case MotionEvent.ACTION_MOVE:
                                                        float dy = event.getRawY() - startY;
                                                        float dx = event.getRawX() - initialX;

                                                        // Tentukan ambang batas untuk perbedaan (misalnya, 8dp)
                                                        float touchSlop = dp(8);

                                                        // KONDISI 1: JIKA SUDAH DRAGGING (dismissing)
                                                        if (dragging) {
                                                                float newY = startPanelY + dy;
                                                                if (newY < initialY) newY = initialY; 

                                                                panel.setY(newY);

                                                                // Logic Opacity (sama seperti sebelumnya)
                                                                float translationFromInitial = newY - initialY;
                                                                float maxTranslation = screenHeight - initialY;
                                                                float opacity = 1f - (translationFromInitial / maxTranslation) * 0.9f;
                                                                if (opacity < 0.1f) opacity = 0.1f;
                                                                if (window != null) {
                                                                        window.getDecorView().setAlpha(opacity);
                                                                    }
                                                                return true; 
                                                            }


                                                        // KONDISI 2: CEK APAKAH HARUS MEMULAI DRAG-TO-DISMISS
                                                        if (panel.getScrollY() == 0 && dy > touchSlop && Math.abs(dy) > Math.abs(dx)) {
                                                                // Hanya ambil alih jika di puncak, bergerak ke bawah, dan pergerakan VERTICAL DOMINAN

                                                                // Reset startPanelY dan startY untuk drag smooth
                                                                startPanelY = panel.getY(); 
                                                                startY = event.getRawY();

                                                                dragging = true;
                                                                return true; // Ambil alih event (menggantikan ScrollView)
                                                            }

                                                        // KONDISI 3: Jika di ScrollView tetapi bergerak ke atas (scroll normal)
                                                        // atau jika tidak ada pergerakan dominan (mungkin klik), 
                                                        // biarkan ScrollView menanganinya (return false).
                                                        break;

                                                    case MotionEvent.ACTION_UP:
                                                        if (dragging) {
                                                                float finalY = panel.getY();
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
                                            // PENTING: Jika bukan drag-to-dismiss, kembalikan false.
                                            // Ini memungkinkan event sentuhan diteruskan ke anak (TextView) untuk link.
                                            return false;
                                        }
                                });
                    }
                if (installButton != null) {
                        installButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                            if (installButtonListener != null) installButtonListener.onClick(v);
                                            animateDismiss(scrollView, dialogReference);
                                        }
                                });
                    }
                if (dismissButton != null) {
                        dismissButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                            if (dismissButtonListener != null) dismissButtonListener.onClick(v);
                                            animateDismiss(scrollView, dialogReference);
                                        }
                                });
                    }

                wrapper.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    // Klik di luar panel
                                    if(isMinimizedMode || !scrollView.getTouchables().isEmpty()) {
                                            animateDismiss(scrollView, dialogReference);
                                        }
                                }
                        });
            }

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
                Window window = getWindow();

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

