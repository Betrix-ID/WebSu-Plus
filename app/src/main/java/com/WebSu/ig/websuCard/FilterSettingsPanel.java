package com.WebSu.ig.websuCard;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;
import com.WebSu.ig.R;
import com.WebSu.ig.TerminalOutputType;

public class FilterSettingsPanel {

        private static final String PREFS_NAME = "TerminalFilterPrefs";
        private static final String KEY_OUTPUT_PREFIX = "OutputFilter_";
        private static final String KEY_SAVE_PREFIX = "SaveFilter_";
        private static final String KEY_VOLUME_PREFIX = "VolumeFilter_";
        private static final String KEY_DELETE_PREFIX = "DeleteFilter_";

        private static final String BLOCKING_VOLUME_UP_KEY = "BLOKING_VOLUME_UP";
        private static final String BLOCKING_VOLUME_DOWN_KEY = "BLOKING_VOLUME_DOWN";

        private static final int CHECKBOX_TINT_COLOR = 0xFF5484A6;

        private ColorStateList checkboxTintList;

        public interface OnDismissListener {
                void onPanelDismissed();
            }

        private OnDismissListener dismissListener;
        private final Context ctx;
        private Dialog dialog;
        private LinearLayout panelContentLayout;
        private final int screenHeight;
        private final float density;

        public FilterSettingsPanel(Context ctx) {
                this.ctx = ctx;
                this.screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;
                this.density = ctx.getResources().getDisplayMetrics().density;
                setupCheckboxTint();
            }

        public void setOnDismissListener(OnDismissListener listener) {
                this.dismissListener = listener;
            }

        private int dp(int value) {
                return Math.round(value * density);
            }

        private void setupCheckboxTint() {
                int[][] states = new int[][] {
                        new int[]{android.R.attr.state_checked}, // checked
                        new int[]{} // default (unchecked)
                    };

                int[] colors = new int[] {
                        CHECKBOX_TINT_COLOR, // Warna saat checked
                        ContextCompat.getColor(ctx, R.color.colorTextSecondary) // Warna saat unchecked
                    };

                checkboxTintList = new ColorStateList(states, colors);
            }

        private void applyCustomTint(CheckBox cb) {
                if (cb != null && checkboxTintList != null) {
                        CompoundButtonCompat.setButtonTintList(cb, checkboxTintList);
                    }
            }

        private void updateRowBackground(RelativeLayout rowLayout, boolean isChecked) {
                if (isChecked) {
                        rowLayout.setBackgroundResource(R.drawable.bg_label);
                    } else {
                        rowLayout.setBackgroundResource(0);
                    }
            }

        public void show() {
                if (dialog != null && dialog.isShowing()) return;

                if (!(ctx instanceof Activity)) {
                        Toast.makeText(ctx, "Panel harus dipanggil dari Activity Context.", Toast.LENGTH_SHORT).show();
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
                        LayoutInflater inflater = LayoutInflater.from(ctx);
                        View customLayout = inflater.inflate(R.layout.panel_filter_settings, null);
                        panelContentLayout = (LinearLayout) customLayout;
                    } catch (Exception e) {
                        Log.e("FilterPanel", "Error loading layout: " + e.getMessage());
                        Toast.makeText(ctx, "Gagal memuat layout panel_filter_settings.xml.", Toast.LENGTH_LONG).show();
                        return;
                    }

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(ContextCompat.getColor(ctx, R.color.colorBackground));
                float radius = dp(28);
                bg.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
                panelContentLayout.setBackground(bg);

                final ScrollView scrollView = new ScrollView(ctx);
                scrollView.setFillViewport(true);
                scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
                scrollView.setVerticalScrollBarEnabled(false);
                scrollView.addView(panelContentLayout);

                final FrameLayout wrapper = new FrameLayout(ctx);
                wrapper.setBackgroundColor(Color.TRANSPARENT);

                FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                wrapper.setLayoutParams(wrapParams);

                FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
                wrapper.addView(scrollView, bottomParams);

                // --- Setup Sections ---
                final LinearLayout filterOutputHeader = panelContentLayout.findViewById(R.id.filter_output_header);
                final LinearLayout filterOutputContent = panelContentLayout.findViewById(R.id.filter_output_content);
                final ImageView filterOutputIcon = panelContentLayout.findViewById(R.id.icon_dropdown_output);
                setupFilterSection(filterOutputHeader, filterOutputContent, filterOutputIcon, KEY_OUTPUT_PREFIX);

                final LinearLayout filterLogSaveHeader = panelContentLayout.findViewById(R.id.filter_log_save_header);
                final LinearLayout filterLogSaveContent = panelContentLayout.findViewById(R.id.filter_log_save_content);
                final ImageView filterLogSaveIcon = panelContentLayout.findViewById(R.id.icon_dropdown_log_save);
                setupFilterSection(filterLogSaveHeader, filterLogSaveContent, filterLogSaveIcon, KEY_SAVE_PREFIX);

                final LinearLayout filterVolumeHeader = panelContentLayout.findViewById(R.id.filter_volume_header);
                final LinearLayout filterVolumeContent = panelContentLayout.findViewById(R.id.filter_volume_content);
                final ImageView filterVolumeIcon = panelContentLayout.findViewById(R.id.icon_dropdown_volume);
                setupFilterSection(filterVolumeHeader, filterVolumeContent, filterVolumeIcon, KEY_VOLUME_PREFIX);

                final LinearLayout filterDeleteHeader = panelContentLayout.findViewById(R.id.filter_delete_header);
                final LinearLayout filterDeleteContent = panelContentLayout.findViewById(R.id.filter_delete_content);
                final ImageView filterDeleteIcon = panelContentLayout.findViewById(R.id.icon_dropdown_delete);
                setupFilterSection(filterDeleteHeader, filterDeleteContent, filterDeleteIcon, KEY_DELETE_PREFIX);

                final Dialog dialogReference = dlg;

                scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                    scrollView.setTag(Integer.MAX_VALUE, scrollView.getY());
                                }
                        });

                scrollView.setOnTouchListener(new View.OnTouchListener() {

                            float startY;
                            float startPanelY;
                            boolean dragging = false;

                            private float getInitialY() {
                                    Object tag = scrollView.getTag(Integer.MAX_VALUE);
                                    return tag instanceof Float ? ((Float) tag).floatValue() : screenHeight;
                                }

                            @Override
                            public boolean onTouch(View v, MotionEvent event) {

                                    switch (event.getAction()) {
                                            case MotionEvent.ACTION_DOWN:
                                                startY = event.getRawY();
                                                startPanelY = scrollView.getY();
                                                dragging = false;
                                                break;

                                            case MotionEvent.ACTION_MOVE:
                                                float dy = event.getRawY() - startY;
                                                if (scrollView.getScrollY() == 0 && dy > 0) {

                                                        float newY = startPanelY + dy;
                                                        float initialY = getInitialY();

                                                        if (newY >= initialY) {
                                                                scrollView.setY(newY);

                                                                float translationFromInitial = newY - initialY;
                                                                float maxTranslation = screenHeight - initialY;
                                                                if (maxTranslation <= 0) maxTranslation = 1.0f;

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

                                                        float finalY = scrollView.getY();
                                                        float initialY = getInitialY();
                                                        float threshold = initialY + (screenHeight - initialY) * 0.25f;

                                                        if (finalY > threshold) {
                                                                scrollView.animate()
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
                                                                scrollView.animate()
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
                        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

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

        private void setupFilterSection(final LinearLayout header, final LinearLayout content, final ImageView icon, final String keyPrefix) {
                if (content == null || icon == null || header == null) {
                        Log.w("FilterPanel", "Filter section is missing components for prefix: " + keyPrefix);
                        return;
                    }

             final int contentId = content.getId();

                boolean isExpanded = loadExpansionState(contentId);
                content.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                icon.setImageResource(isExpanded ? R.drawable.ic_up : R.drawable.ic_donw);

                header.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    if (content.getVisibility() == View.VISIBLE) {
                                            content.setVisibility(View.GONE);
                                            icon.setImageResource(R.drawable.ic_donw);
                                            saveExpansionState(contentId, false);
                                        } else {
                                            content.setVisibility(View.VISIBLE);
                                            icon.setImageResource(R.drawable.ic_up);
                                            saveExpansionState(contentId, true);
                                        }
                                }
                        });
                loadAndSetupCheckboxes(content, keyPrefix);
            }


        private void loadAndSetupCheckboxes(LinearLayout filterContent, final String keyPrefix) {
                if (keyPrefix.equals(KEY_VOLUME_PREFIX)) {

                        final String[] volumeKeys = new String[]{
                                BLOCKING_VOLUME_UP_KEY,
                                BLOCKING_VOLUME_DOWN_KEY
                            };

                        int keyIndex = 0;
                        for (int i = 0; i < filterContent.getChildCount(); i++) {
                                View childView = filterContent.getChildAt(i);
                                if (!(childView instanceof RelativeLayout)) continue;

                                final RelativeLayout rl = (RelativeLayout) childView;
                                CheckBox cb = null;
                                TextView tv = null;

                                for (int j = 0; j < rl.getChildCount(); j++) {
                                        View innerView = rl.getChildAt(j);
                                        if (innerView instanceof CheckBox) {
                                                cb = (CheckBox) innerView;
                                            } else if (innerView instanceof TextView) {
                                                tv = (TextView) innerView;
                                            }
                                    }

                                if (cb != null && tv != null && keyIndex < volumeKeys.length) {
                                        applyCustomTint(cb);

                                        final String baseKey = volumeKeys[keyIndex];
                                        final String key = keyPrefix + baseKey.toUpperCase();

                                        // 1. Set status awal
                                        boolean isChecked = loadFilterState(key);
                                        cb.setChecked(isChecked);
                                        // 2. Terapkan background awal
                                        updateRowBackground(rl, isChecked);

                                        // 3. Setup Listener
                                        cb.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                            boolean newState = ((CheckBox) v).isChecked();
                                                            saveFilterState(key, newState);
                                                            updateRowBackground(rl, newState); // Perbarui background
                                                        }
                                                });
                                        keyIndex++;
                                    }
                            }
                        return;
                    }

                if (keyPrefix.equals(KEY_DELETE_PREFIX)) {
                        final String[] deleteKeys = new String[]{
                                TerminalOutputType.TYPE_COMMAND.name(),
                                TerminalOutputType.TYPE_STDOUT.name(),
                                TerminalOutputType.TYPE_STDIN.name()
                            };

                        int keyIndex = 0;

                        for (int i = 0; i < filterContent.getChildCount(); i++) {
                                View childView = filterContent.getChildAt(i);
                                if (!(childView instanceof RelativeLayout)) continue;

                                final RelativeLayout rl = (RelativeLayout) childView;

                                CheckBox cb = null;
                                for (int j = 0; j < rl.getChildCount(); j++) {
                                        View innerView = rl.getChildAt(j);
                                        if (innerView instanceof CheckBox) {
                                                cb = (CheckBox) innerView;
                                                break;
                                            }
                                    }

                                if (cb != null && keyIndex < deleteKeys.length) {
                                        applyCustomTint(cb);

                                        final String typeName = deleteKeys[keyIndex];
                                        final String key = keyPrefix + typeName;

                                        // 1. Set status awal
                                        boolean isChecked = loadFilterState(key);
                                        cb.setChecked(isChecked);
                                        // 2. Terapkan background awal
                                        updateRowBackground(rl, isChecked);

                                        // 3. Setup Listener
                                        cb.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                            boolean newState = ((CheckBox) v).isChecked();
                                                            saveFilterState(key, newState);
                                                            updateRowBackground(rl, newState); // Perbarui background
                                                        }
                                                });
                                        keyIndex++;
                                    }
                            }
                        return;
                    }

                // Logika untuk KEY_OUTPUT_PREFIX dan KEY_SAVE_PREFIX
                TerminalOutputType[] types = TerminalOutputType.values();
                int rlIndex = 0;

                for (int i = 0; i < filterContent.getChildCount(); i++) {
                        View childView = filterContent.getChildAt(i);

                        if (!(childView instanceof RelativeLayout)) continue;

                        final RelativeLayout rl = (RelativeLayout) childView;

                        CheckBox cb = null;
                        TextView tv = null;
                        for (int j = 0; j < rl.getChildCount(); j++) {
                                View innerView = rl.getChildAt(j);
                                if (innerView instanceof CheckBox) {
                                        cb = (CheckBox) innerView;
                                    } else if (innerView instanceof TextView) {
                                        tv = (TextView) innerView;
                                    }
                            }

                        if (cb != null && tv != null) {
                                applyCustomTint(cb);

                                if (rlIndex < types.length) {
                                        final TerminalOutputType currentType = types[rlIndex];
                                        final String key = keyPrefix + currentType.name();

                                        // 1. Set status awal
                                        boolean isChecked = loadFilterState(key);
                                        cb.setChecked(isChecked);
                                        // 2. Terapkan background awal
                                        updateRowBackground(rl, isChecked);

                                        String displayDescription;
                                        if (keyPrefix.equals(KEY_OUTPUT_PREFIX)) {
                                                displayDescription = currentType.getDescriptionOutput();
                                            } else if (keyPrefix.equals(KEY_SAVE_PREFIX)) {
                                                displayDescription = currentType.getDescriptionSave();
                                            } else {
                                                displayDescription = currentType.getDescription();
                                            }

                                        tv.setText(displayDescription);

                                        // 3. Setup Listener
                                        cb.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                            boolean newState = ((CheckBox) v).isChecked();
                                                            saveFilterState(key, newState);
                                                            updateRowBackground(rl, newState); // Perbarui background
                                                        }
                                                });
                                    }
                                rlIndex++;
                            }
                    }
            }


        private SharedPreferences getPrefs() {
                return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }

        /**
         * Menyimpan status (checked/unchecked) sebuah filter ke SharedPreferences.
         */
        public void saveFilterState(String key, boolean isChecked) {
                getPrefs().edit().putBoolean(key, isChecked).apply();
                Log.d("FilterPanel", "Saved " + key + ": " + isChecked);
            }

        public boolean loadFilterState(String key) {
                boolean defaultValue = true;
                if (key.startsWith(KEY_DELETE_PREFIX) || key.startsWith(KEY_VOLUME_PREFIX)) {
                        defaultValue = false;
                    } else if (key.startsWith(KEY_OUTPUT_PREFIX) || key.startsWith(KEY_SAVE_PREFIX)) {
                        defaultValue = true;
                    }

                return getPrefs().getBoolean(key, defaultValue);
            }

        /**
         * Menyimpan status expand/collapse untuk Content Panel
         */
        private void saveExpansionState(int viewId, boolean isExpanded) {
                String key = "Expansion_" + viewId;
                getPrefs().edit().putBoolean(key, isExpanded).apply();
            }

        private boolean loadExpansionState(int viewId) {
                String key = "Expansion_" + viewId;
                return getPrefs().getBoolean(key, true);
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

