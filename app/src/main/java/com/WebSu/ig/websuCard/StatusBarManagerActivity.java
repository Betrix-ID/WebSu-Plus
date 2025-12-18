package com.WebSu.ig.websuCard;

import android.app.Activity;
import android.content.res.Configuration; 
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager; 
import androidx.core.content.ContextCompat;
import com.WebSu.ig.R;

public class StatusBarManagerActivity extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                setEdgeToEdgeStatusBar();
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                boolean isSystemDark = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

                // 2. Tentukan tema ikon Status Bar:
                //    - Jika isSystemDark = TRUE (Mode Gelap): Kita butuh ikon TERANG/PUTIH. 
                //      Maka, isLight harus FALSE.
                //    - Jika isSystemDark = FALSE (Mode Terang): Kita butuh ikon GELAP/HITAM.
                //      Maka, isLight harus TRUE.
                //    Kita menggunakan kebalikan dari isSystemDark.
                setStatusBarTheme(!isSystemDark); 
            }

        protected void setEdgeToEdgeStatusBar() {
                Window window = getWindow();
                View decor = window.getDecorView();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                        window.setStatusBarColor(Color.TRANSPARENT);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                decor.setSystemUiVisibility(
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                );
                            }

                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        int visibility = decor.getSystemUiVisibility();
                        visibility &= ~View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                        visibility &= ~View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

                        decor.setSystemUiVisibility(visibility);
                    }
            }

        protected void setStatusBarTheme(boolean isLight) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Window window = getWindow();
                        View decor = window.getDecorView();
                        int flags = decor.getSystemUiVisibility();

                        if (isLight) {
                                // isLight = true (Mode Terang/Light) -> Ikon GELAP (hitam)
                                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR; 
                            } else {
                                // isLight = false (Mode Gelap/Dark) -> Ikon TERANG (putih)
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

