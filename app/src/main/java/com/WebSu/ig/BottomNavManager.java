package com.WebSu.ig;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.scottyab.rootbeer.RootBeer;
import rikka.shizuku.Shizuku;

public class BottomNavManager {

        private final LinearLayout navHome, navWebui, navSettings;
        private final LinearLayout homeWrapper, webuiWrapper, settingsWrapper;
        private final TextView navHomeText, navWebuiText, navSettingsText;
        private final ImageView navHomeIcon, navWebuiIcon, navSettingsIcon;
        private final FrameLayout contentFrame;

        private final Activity activity;
        private int currentLayout = -1;
        private boolean permissionGranted = false;
        private final float DP_TO_PX_RATIO;
        private static final int ANIMATION_DURATION = 200; 

        private static final int ITEM_HOME = 1;
        private static final int ITEM_WEBUI = 2;
        private static final int ITEM_SETTINGS = 3;

        public BottomNavManager(final Activity activity) {
                this.activity = activity;

                DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
                DP_TO_PX_RATIO = metrics.density;

                navHome = activity.findViewById(R.id.nav_home);
                navWebui = activity.findViewById(R.id.nav_webui);
                navSettings = activity.findViewById(R.id.nav_settings);

                homeWrapper = activity.findViewById(R.id.nav_home_icon_wrapper);
                webuiWrapper = activity.findViewById(R.id.nav_webui_icon_wrapper);
                settingsWrapper = activity.findViewById(R.id.nav_settings_icon_wrapper);

                navHomeText = activity.findViewById(R.id.nav_home_text);
                navWebuiText = activity.findViewById(R.id.nav_webui_text);
                navSettingsText = activity.findViewById(R.id.nav_settings_text);

                navHomeIcon = activity.findViewById(R.id.nav_home_icon);
                navWebuiIcon = activity.findViewById(R.id.nav_webui_icon);
                navSettingsIcon = activity.findViewById(R.id.nav_settings_icon);

                contentFrame = activity.findViewById(R.id.content_frame);

                setupListeners();
                setActiveInternal(homeWrapper, navHomeIcon, navHomeText, ITEM_HOME);
                ((MainActivity) activity).showInfoLayout(); 

                checkPermissionState();
            }

        private void setupListeners() {
                navHome.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    setActiveInternal(homeWrapper, navHomeIcon, navHomeText, ITEM_HOME);
                                    ((MainActivity) activity).showInfoLayout();
                                }
                        });

                navWebui.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    if (!permissionGranted) {
                                            Toast.makeText(activity, "Izin Root/Shizuku belum diberikan untuk WebUI.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    setActiveInternal(webuiWrapper, navWebuiIcon, navWebuiText, ITEM_WEBUI);
                                    ((MainActivity) activity).showWebuiLayout();
                                }
                        });

                navSettings.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    setActiveInternal(settingsWrapper, navSettingsIcon, navSettingsText, ITEM_SETTINGS);
                                    ((MainActivity) activity).showSettingsLayout();
                                }
                        });
            }

        public void selectWebuiTab() {
                if (!permissionGranted) {
                        checkPermissionState();
                        if (!permissionGranted) {
                                Toast.makeText(activity, "Instalasi sukses, namun izin Root/Shizuku belum tersedia untuk WebUI.", Toast.LENGTH_LONG).show();
                                return;
                            }
                    }
                setActiveInternal(webuiWrapper, navWebuiIcon, navWebuiText, ITEM_WEBUI);
                ((MainActivity) activity).showWebuiLayout();
            }

        public void checkPermissionState() {
                boolean hasShizuku = false;
                boolean hasRoot = false;

                try {
                        if (Shizuku.pingBinder()) {
                                hasShizuku = (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED);
                            }

                        if (!hasShizuku) {
                                RootBeer rootBeer = new RootBeer(activity);
                                hasRoot = rootBeer.isRooted();
                            }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                permissionGranted = (hasShizuku || hasRoot);
                updatePermissionUI();
            }

        public void updatePermissionUI() {
                navWebui.setAlpha(permissionGranted ? 1.0f : 0.4f);
                navWebui.setEnabled(permissionGranted);
            }

        private void setActiveInternal(LinearLayout activeWrapper, ImageView activeIcon, TextView activeText, int layoutId) {
                if (currentLayout == layoutId) return;
                resetNav(homeWrapper, navHomeIcon, navHomeText);
                resetNav(webuiWrapper, navWebuiIcon, navWebuiText);
                resetNav(settingsWrapper, navSettingsIcon, navSettingsText);

                currentLayout = layoutId;

                activeWrapper.setBackgroundResource(R.drawable.bg_nav_active);
                int filledIconRes = getFilledIconResource(activeIcon.getId());
                if (filledIconRes != 0) activeIcon.setImageResource(filledIconRes);
                activeText.setTextColor(ContextCompat.getColor(activity, R.color.colorOnPrimary));
                activeText.setVisibility(View.VISIBLE);

                float moveUpPx = 2* DP_TO_PX_RATIO; 

                activeIcon.animate()
                    .setDuration(ANIMATION_DURATION) 
                    .scaleX(1.1f)  
                    .scaleY(1.1f)
                    .translationY(-moveUpPx) 
                    .setInterpolator(new AccelerateDecelerateInterpolator()) 
                    .start();

                activeText.setAlpha(0f); 
                activeText.setTranslationY(moveUpPx / 2); 
                activeText.animate()
                    .setDuration(ANIMATION_DURATION) 
                    .alpha(1f)      
                    .translationY(0f) 
                    .setStartDelay(100)
                    .start();
            }

        private void resetNav(final LinearLayout wrapper, final ImageView icon, final TextView text) {
                icon.animate()
                    .setDuration(ANIMATION_DURATION)
                    .scaleX(1.0f) 
                    .scaleY(1.0f) 
                    .translationY(10f) 
                    .setInterpolator(new AccelerateDecelerateInterpolator()) 
                    .withEndAction(new Runnable() { 
                            @Override
                            public void run() {
                                    wrapper.setBackgroundResource(android.R.color.transparent);
                                    int outlineIconRes = getOutlineIconResource(icon.getId());
                                    if (outlineIconRes != 0) icon.setImageResource(outlineIconRes);

                                    icon.setAlpha(1.0f);
                                }
                        })
                    .start();

                text.animate()
                    .setDuration(ANIMATION_DURATION / 2)
                    .alpha(0f) 
                    .translationY(0f) 
                    .withEndAction(new Runnable() { 
                            @Override
                            public void run() {
                                    text.setTextColor(0x80FFFFFF); 
                                    text.setVisibility(View.GONE);
                                }
                        })
                    .start();
            }

        private int getFilledIconResource(int iconId) {
                if (iconId == R.id.nav_home_icon) return R.drawable.ic_home_outline; 
                if (iconId == R.id.nav_webui_icon) return R.drawable.ic_webui_outline;
                if (iconId == R.id.nav_settings_icon) return R.drawable.ic_settings_outline;
                return 0;               
            }

        private int getOutlineIconResource(int iconId) {
                if (iconId == R.id.nav_home_icon) return R.drawable.ic_home; 
                if (iconId == R.id.nav_webui_icon) return R.drawable.ic_web;
                if (iconId == R.id.nav_settings_icon) return R.drawable.ic_settings;
                return 0;
            }

        public void showLayout(int layoutResId) {
                if (currentLayout == layoutResId) return;
                currentLayout = layoutResId;

                contentFrame.removeAllViews();
                LayoutInflater inflater = activity.getLayoutInflater();
                View newContent = inflater.inflate(layoutResId, contentFrame, false);

                newContent.setLayoutParams(new FrameLayout.LayoutParams(
                                               ViewGroup.LayoutParams.MATCH_PARENT, 
                                               ViewGroup.LayoutParams.MATCH_PARENT));

                contentFrame.addView(newContent);
            }
    }

