package com.WebSu.ig;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.graphics.Color;
import android.app.Activity;
import androidx.core.content.ContextCompat;
import com.WebSu.ig.R;

public class updateStatusBarColor {

        public static void updateStatusBarColor(Activity activity) {
                if (activity == null) return;

                Window window = activity.getWindow();
                View decor = window.getDecorView();

                int backgroundColor = ContextCompat.getColor(activity, R.color.colorBackground);
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setStatusBarColor(backgroundColor);
                    }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        boolean isBackgroundLight = isColorLight(backgroundColor);
                        int flags = decor.getSystemUiVisibility();

                        if (isBackgroundLight) {
                                // Warna background terang -> Ikon Status Bar gelap (LIGHT_STATUS_BAR)
                                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                            } else {
                                // Warna background gelap -> Ikon Status Bar terang (default)
                                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                            }
                        decor.setSystemUiVisibility(flags);
                    }
            }
            
        private static boolean isColorLight(int color) {
                double darkness = 1 - (0.299 * Color.red(color) +
                    0.587 * Color.green(color) +
                    0.114 * Color.blue(color)) / 255;
                return darkness < 0.3;
            }
    }

