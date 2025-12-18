package com.WebSu.ig;

import android.app.Activity;
import com.WebSu.ig.websuCard.UninstallConfirmationDialog; 
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.content.FileProvider;
import com.WebSu.ig.Dirname.DirectoryManager;
import com.WebSu.ig.shell.CrashLogHandler;
import com.WebSu.ig.viewmodel.ZipFilePicker;
import com.WebSu.ig.websuCard.PluginWebuiManager;
import com.WebSu.ig.viewmodel.WebUiResetManager;
import com.WebSu.ig.websuCard.PluginSetings;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.core.content.ContextCompat;
import com.WebSu.ig.websuCard.StatusBarManagerActivity;
import com.WebSu.ig.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.WebSu.ig.websuCard.LicenseDialog;
import rikka.shizuku.Shizuku;

public class MainActivity extends StatusBarManagerActivity { 

        private LinearLayout errorCard;
        private TextView errorTitle, errorSubtitle, errorVersion, textSELinux;
        private ImageView iconBlock;

        private BottomNavManager bottomNavManager;
        private BottomPanel bottom;
        private static final String TAG = "Dirname";

        private View cachedWebuiView = null;
        private ZipFilePicker zipFilePicker;

        private static PluginWebuiManager webuiManager = null;

        private static final int REQUEST_CODE_INSTALL_NAVIGATE = 100;
        private static final String FILE_PROVIDER_AUTHORITY = "com.WebSu.ig.fileprovider";
        private static final String LOG_DIR_NAME = "logs";
        private boolean isBackButtonIcon = false;
        private ImageButton quickButton;
        private ImageButton pluginSettingsButton;
        private boolean isSettingsIcon = false;

        private static final String PREFS_NAME = "WebSuPrefs";
        private static final String KEY_AUTO_REFRESH = "auto_refresh_enabled";
        private boolean isInitialAutoRefreshDone = false;

        private UninstallConfirmationDialog.UninstallDialogListener uninstallListener = new UninstallConfirmationDialog.UninstallDialogListener() {
                @Override
                public void onUninstallConfirmed(String moduleId, String moduleBasePath) {
                        Log.d(TAG, "Konfirmasi penghapusan diterima dari Dialog.");
                        performModuleDeletion(moduleId, moduleBasePath);
                    }
            };
       
        private PluginSetings.OnSettingsStateListener settingsListener = new PluginSetings.OnSettingsStateListener() {
                @Override
                public void onSettingsOpened() {
                        if (pluginSettingsButton != null) {
                                animateSettingsButton(pluginSettingsButton, true);
                            }
                    }

                @Override
                public void onSettingsClosed() {
                        if (pluginSettingsButton != null) {
                                animateSettingsButton(pluginSettingsButton, false);
                            }
                    }
            };
  
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                CrashLogHandler.attach(this);
                setContentView(R.layout.main);
                
                bottomNavManager = new BottomNavManager(this);
                showInfoLayout();
                ensureExternalWebSuDir();

                boolean isSuccess = DirectoryManager.createAllDirectories(this);
                if (isSuccess) {
                        Log.i(TAG, "Semua direktori berhasil dibuat.");
                    } else {
                        Log.e(TAG, "Gagal membuat beberapa atau semua direktori.");
                    }

                try {
                        Shizuku.addRequestPermissionResultListener(new Shizuku.OnRequestPermissionResultListener() {
                                    @Override
                                    public void onRequestPermissionResult(int requestCode, int grantResult) {
                                            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                                                    ShellExecutor.invalidateCache();
                                                    updateConnectionStatus();
                                                    if (bottomNavManager != null) {
                                                            bottomNavManager.checkPermissionState();
                                                        }
                                                    Toast.makeText(MainActivity.this, "Izin Shizuku disetujui.", Toast.LENGTH_SHORT).show();
                                                }
                                        }
                                });
                    } catch (Throwable ignored) {}
            }

        @Override
        protected void onResume() {
                super.onResume();
                ShellExecutor.invalidateCache();
                updateConnectionStatus();
                if (bottomNavManager != null) {
                        bottomNavManager.checkPermissionState();
                    }
            }

        public void showInfoLayout() {
                FrameLayout container = findViewById(R.id.content_frame);
                container.removeAllViews();

                View infoView = getLayoutInflater().inflate(R.layout.info, container, false);
                container.addView(infoView);

                errorCard = infoView.findViewById(R.id.error_card);
                errorTitle = infoView.findViewById(R.id.text_status);
                errorSubtitle = infoView.findViewById(R.id.error_subtitle);
                errorVersion = infoView.findViewById(R.id.error_version);
                iconBlock = infoView.findViewById(R.id.icon_block);
                textSELinux = infoView.findViewById(R.id.text_selinux);

                TextView textKernel = infoView.findViewById(R.id.text_kernel_under_title);
                TextView textAndroid = infoView.findViewById(R.id.text_android_version);
                TextView textBuildType = infoView.findViewById(R.id.text_hook);
                TextView textChipset = infoView.findViewById(R.id.Chipset_value);

                textKernel.setText(getKernelVersion());
                textChipset.setText(Build.HARDWARE);
                textAndroid.setText(Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
                textBuildType.setText(Build.TYPE);
                textSELinux.setText(getSELinuxStatus());

                errorCard.setGravity(Gravity.CENTER_VERTICAL);
                errorCard.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    Intent intent = new Intent(MainActivity.this, ActivateActivity.class);
                                    startActivity(intent);
                                }
                        });

                bottom = new BottomPanel(this);
                final ImageButton quick = findViewById(R.id.Olik);
                this.quickButton = quick;

                bottom.setOnDismissListener(new BottomPanel.OnDismissListener() {
                            @Override
                            public void onPanelDismissed() {
                                    if (quickButton != null) {
                                            animateQuickButton(quickButton, false);
                                        }
                                }
                        });
          
                if (isBackButtonIcon) {
                        quick.setImageResource(R.drawable.ic_arrow_back_white);
                    } else {
                        quick.setImageResource(R.drawable.ic_bar);
                    }

                quick.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    if (bottom.isShowing()) {
                                            bottom.hide();
                                        } else {
                                            animateQuickButton(quick, true);
                                            bottom.show();
                                        }
                                }
                        });

                updateConnectionStatus();
            }

        private void animateQuickButton(final ImageButton quick, boolean targetIsBack) {
                if (targetIsBack == isBackButtonIcon) {
                        return;
                    }

                float startRotation;
                float endRotation;
                final int newIconRes;

                if (targetIsBack) {
                        startRotation = 0f;
                        endRotation = 180f;
                        newIconRes = R.drawable.ic_arrow_back_white;
                    } else {
                        startRotation = -180f;
                        endRotation = 0f;
                        newIconRes = R.drawable.ic_bar;
                    }

                quick.setRotation(startRotation);

                quick.animate()
                    .rotation(endRotation)
                    .setDuration(250)
                    .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                    quick.setRotation(0f);
                                    quick.setImageResource(newIconRes);
                                }
                        })
                    .start();
                isBackButtonIcon = targetIsBack;
            }

        private void animateSettingsButton(final ImageButton button, boolean targetIsSettings) {
                if (targetIsSettings == isSettingsIcon) {
                        return;
                    }

                float startRotation;
                float endRotation;
                final int newIconRes;

                if (targetIsSettings) {
                        startRotation = 0f;
                        endRotation = 180f;
                        newIconRes = R.drawable.ic_arrow_back_white;
                    } else {
                        startRotation = -180f;
                        endRotation = 0f;
                        newIconRes = R.drawable.ic_bar;
                    }

                button.setImageResource(newIconRes == R.drawable.ic_arrow_back_white ? R.drawable.ic_bar : R.drawable.ic_bar);
                button.setRotation(startRotation);

                button.animate()
                    .rotation(endRotation)
                    .setDuration(250)
                    .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                    button.setRotation(0f);
                                    button.setImageResource(newIconRes);
                                }
                        })
                    .start();

                isSettingsIcon = targetIsSettings;
            }

        public void showWebuiLayout() {
                final FrameLayout container = findViewById(R.id.content_frame);
                container.removeAllViews();

                final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                final boolean autoRefreshEnabled = prefs.getBoolean(KEY_AUTO_REFRESH, false);

                if (cachedWebuiView == null) {
                        Log.d(TAG, "WebUI Cache View kosong.");

                        cachedWebuiView = getLayoutInflater().inflate(R.layout.plugin_web, container, false);
                        PluginWebuiManager.OnModuleActionListener moduleActionListener = new PluginWebuiManager.OnModuleActionListener() {
                                @Override
                                public void onDeleteModule(final String moduleId, final String moduleBasePath) {
                                        Log.d(TAG, "Permintaan penghapusan diterima. Menampilkan dialog konfirmasi...");
                                        UninstallConfirmationDialog dialog = new UninstallConfirmationDialog(MainActivity.this, moduleId, moduleBasePath);
                                        dialog.setUninstallDialogListener(uninstallListener); 
                                        dialog.show();
                                    }
                            };

                        webuiManager = PluginWebuiManager.getInstance(this, cachedWebuiView);
                        webuiManager.setModuleActionListener(moduleActionListener);

                        zipFilePicker = new ZipFilePicker(this, cachedWebuiView);
                        webuiManager.loadModules();

                        ImageButton btnPilihan = cachedWebuiView.findViewById(R.id.btn_pilihan);
                        this.pluginSettingsButton = btnPilihan;

                        if (btnPilihan != null) {
                                btnPilihan.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                    PluginSetings.show(MainActivity.this, settingsListener); 
                                                }
                                        });
                                if (!isSettingsIcon) {
                                        btnPilihan.setImageResource(R.drawable.ic_bar);
                                    }
                            }

                        final SwipeRefreshLayout swipeRefreshLayout = cachedWebuiView.findViewById(R.id.swipe_refresh_webui);
                        if (swipeRefreshLayout != null) {
                                swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                                            @Override
                                            public void onRefresh() {
                                                    if (webuiManager != null) {
                                                            Log.d(TAG, "Swipe detected. Refreshing WebUI plugins.");
                                                            webuiManager.refreshPlugins();
                                                        }
                                                    swipeRefreshLayout.setRefreshing(false);
                                                }
                                        });

                                if (autoRefreshEnabled && !isInitialAutoRefreshDone) {
                                        Log.i(TAG, "Auto-refresh dipicu setelah launch.");
                                        swipeRefreshLayout.setRefreshing(true);

                                        swipeRefreshLayout.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                            if (webuiManager != null) {
                                                                    webuiManager.refreshPlugins();
                                                                    isInitialAutoRefreshDone = true;
                                                                }
                                                            swipeRefreshLayout.setRefreshing(false);
                                                        }
                                                }, 50);
                                    }
                            }

                        WebUiResetManager resetManager = new WebUiResetManager(this, cachedWebuiView);
                        resetManager.attachResetAction(R.id.btn_reset_webui, new Runnable() {
                                    @Override
                                    public void run() {
                                            if (webuiManager != null) {
                                                    webuiManager.refreshPlugins();
                                                }
                                            cachedWebuiView = null;
                                            showWebuiLayout();
                                        }
                                });

                    } else {
                        if (cachedWebuiView.getParent() != null) {
                                ((ViewGroup) cachedWebuiView.getParent()).removeView(cachedWebuiView);
                            }

                        ImageButton btnPilihan = cachedWebuiView.findViewById(R.id.btn_pilihan);
                        this.pluginSettingsButton = btnPilihan;

                        if (btnPilihan != null) {
                                btnPilihan.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                    PluginSetings.show(MainActivity.this, settingsListener);
                                                }
                                        });
                                btnPilihan.setImageResource(isSettingsIcon ? R.drawable.ic_arrow_back_white : R.drawable.ic_bar);
                            }

                        PluginWebuiManager.OnModuleActionListener moduleActionListener = new PluginWebuiManager.OnModuleActionListener() {
                                @Override
                                public void onDeleteModule(final String moduleId, final String moduleBasePath) {
                                        Log.d(TAG, "Permintaan penghapusan diterima. Menampilkan dialog konfirmasi...");
                                        UninstallConfirmationDialog dialog = new UninstallConfirmationDialog(MainActivity.this, moduleId, moduleBasePath);
                                        dialog.setUninstallDialogListener(uninstallListener);
                                        dialog.show();
                                    }
                            };

                        webuiManager = PluginWebuiManager.getInstance(this, cachedWebuiView);
                        webuiManager.setModuleActionListener(moduleActionListener);
                        webuiManager.loadModules();
                        Log.d(TAG, "WebUI Cache View ditemukan.");
                    }
                container.addView(cachedWebuiView);
            }


        public void showSettingsLayout() {
                FrameLayout container = findViewById(R.id.content_frame);
                container.removeAllViews();
                View settingsView = getLayoutInflater().inflate(R.layout.settings, container, false);
                container.addView(settingsView);

                final SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                boolean isAutoRefreshEnabled = prefs.getBoolean(KEY_AUTO_REFRESH, true);

                com.WebSu.ig.logd switchAutoCheck = settingsView.findViewById(R.id.switch_auto_check);

                if (switchAutoCheck != null) {
                        switchAutoCheck.setChecked(isAutoRefreshEnabled);

                        switchAutoCheck.setOnCheckedChangeListener(new com.WebSu.ig.logd.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(View view, boolean isChecked) {
                                            prefs.edit().putBoolean(KEY_AUTO_REFRESH, isChecked).apply();

                                            if (isChecked) {
                                                    Toast.makeText(MainActivity.this, "Auto Refresh AKTIF.", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(MainActivity.this, "Auto Refresh NONAKTIF.", Toast.LENGTH_SHORT).show();
                                                }
                                        }
                                });
                    }

                RelativeLayout amber = settingsView.findViewById(R.id.amber);
                amber.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    DialogAppearance.show(MainActivity.this); 
                                }
                        });

                RelativeLayout sendLogItem = settingsView.findViewById(R.id.send_log_item);
                sendLogItem.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    shareLogFiles();
                                }
                        });

                RelativeLayout aboutItem = settingsView.findViewById(R.id.about_item);
                aboutItem.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    new LicenseDialog(MainActivity.this).show();
                                }
                        });
            }

        private void shareLogFiles() {

                File externalFilesDir = getExternalFilesDir(null);
                if (externalFilesDir == null) {
                        Toast.makeText(this, "Penyimpanan eksternal tidak tersedia.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                File logDir = new File(externalFilesDir, LOG_DIR_NAME);
                File[] logFiles = logDir.listFiles();

                if (!logDir.exists() || logFiles == null || logFiles.length == 0) {
                        Toast.makeText(this, "Tidak ada file log ditemukan.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                ArrayList<Uri> fileUris = new ArrayList<>();
                int filesCount = 0;

                for (File file : logFiles) {
                        if (file.isFile()) {
                                try {
                                        Uri fileUri = FileProvider.getUriForFile(
                                            this,
                                            FILE_PROVIDER_AUTHORITY,
                                            file
                                        );
                                        fileUris.add(fileUri);
                                        filesCount++;
                                    } catch (IllegalArgumentException e) {
                                        Log.e(TAG, "Gagal mendapatkan Uri untuk file log: " + file.getName(), e);
                                    }
                            }
                    }

                if (filesCount == 0) {
                        Toast.makeText(this, "File log tidak valid.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                shareIntent.setType("text/plain");
                shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                        startActivity(Intent.createChooser(shareIntent, "Kirim Log melalui..."));
                    } catch (Exception e) {
                        Log.e(TAG, "Gagal memulai intent sharing.", e);
                        Toast.makeText(this, "Tidak ada aplikasi yang tersedia untuk berbagi file.", Toast.LENGTH_LONG).show();
                    }
            }

        private void updateConnectionStatus() {
                if (errorCard == null) return;

                boolean shizukuGranted = ShellExecutor.hasShizuku();
                String rootType = null;

                if (ShellExecutor.hasRoot()) {
                        rootType = detectRootType();
                    }

                if (shizukuGranted) {
                        setCardStyle(
                            R.drawable.bg_card_purple,
                            R.drawable.ic_success,
                            "Working",
                            "API Shizuku",
                            0xFFFFFFFF,
                            R.drawable.bg_badge_purple
                        );
                        if (errorSubtitle != null) errorSubtitle.setTextColor(0xFF0F1018);
                        if (iconBlock != null) iconBlock.setColorFilter(android.graphics.Color.parseColor("#0F1018"), android.graphics.PorterDuff.Mode.SRC_IN);
                        errorVersion.setText("Version: v0.0.3.8-251211@WebSuPlus-main");

                    } else if (rootType != null) {
                        String label = "Root Access";
                        if ("magisk".equals(rootType)) label = "Build Magisk";
                        else if ("kernelsu".equals(rootType)) label = "Build KSU";
                        else if ("sukisu".equals(rootType)) label = "Build SukiSu";

                        setCardStyle(
                            R.drawable.bg_card_green,
                            R.drawable.ic_success,
                            "Working",
                            label,
                            0xFFFFFFFF,
                            R.drawable.bg_badge_green
                        );
                        if (errorSubtitle != null) errorSubtitle.setTextColor(0xFF2F105C);
                        if (iconBlock != null) iconBlock.setColorFilter(0xFF9C96E1, android.graphics.PorterDuff.Mode.SRC_IN);
                            errorVersion.setText("Version: v0.0.3.8-251211@WebSuPlus-main");
                            
                    } else {
                        setCardStyle(
                            R.drawable.bg_card_red,
                            R.drawable.ic_warning,
                            "No Connected",
                            0xFFFFFFFF
                        );
                        if (iconBlock != null) iconBlock.setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
                        errorVersion.setText("WebSu Plus saat ini hanya mendukung API Shizuku dan Root");
                    }
            }

        private void setCardStyle(int bg, int icon, String title, String subtitle, int color, int badgeBg) {
                errorCard.setBackgroundResource(bg);
                iconBlock.setImageResource(icon);
                iconBlock.setColorFilter(color);
                errorTitle.setText(title);
                errorSubtitle.setText(subtitle);
                errorSubtitle.setTextColor(color);
                errorSubtitle.setBackgroundResource(badgeBg);
                errorSubtitle.setPadding(20, 8, 20, 8);
                errorSubtitle.setVisibility(View.VISIBLE);
            }

        private void setCardStyle(int bg, int icon, String title, int color) {
                errorCard.setBackgroundResource(bg);
                iconBlock.setImageResource(icon);
                iconBlock.setColorFilter(color);
                errorTitle.setText(title);
                errorSubtitle.setVisibility(View.GONE);
            }

        private String detectRootType() {
                try {
                        ShellExecutor.Result result = ShellExecutor.execSync("su -v", this);

                        if (result.exitCode == 0 && !result.stdout.isEmpty()) {
                                String line = result.stdout.trim().toLowerCase();
                                if (line.contains("magisk")) return "magisk";
                                if (line.contains("kernelsu")) return "kernelsu";
                                if (line.contains("sukisu")) return "sukisu";
                            }
                    } catch (Throwable ignored) {
                    }
                return null;
            }

        private String getKernelVersion() {
                try {
                        ShellExecutor.Result result = ShellExecutor.execSync("uname -r", this);
                        if (result.exitCode == 0 && !result.stdout.isEmpty()) {
                                return result.stdout.trim().split("\n")[0];
                            }
                    } catch (Throwable ignored) {
                    }
                return "Unknown";
            }

        private String getSELinuxStatus() {
                try {
                        ShellExecutor.Result result = ShellExecutor.execSync("getenforce", this);
                        if (result.exitCode == 0 && !result.stdout.isEmpty()) {
                                return result.stdout.trim().split("\n")[0];
                            }
                    } catch (Throwable ignored) {
                    }
                return "Unknown";
            }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);
                if (zipFilePicker != null) {
                        zipFilePicker.handleActivityResult(requestCode, resultCode, data);
                    }
                if (requestCode == REQUEST_CODE_INSTALL_NAVIGATE && resultCode == Activity.RESULT_OK) {
                        if (bottomNavManager != null) bottomNavManager.selectWebuiTab();
                        ShellExecutor.invalidateCache();
                        if (webuiManager != null) {
                                Log.d(TAG, "Instalasi selesai. Memaksa refresh PluginWebuiManager.");
                                webuiManager.refreshPlugins();
                            }
                        showWebuiLayout();
                    }
            }

        private void performModuleDeletion(final String moduleId, final String moduleBasePath) {
                final WebUiResetManager resetManager = new WebUiResetManager(this, cachedWebuiView);
                final Runnable onModuleDeleteComplete = new Runnable() {
                        @Override
                        public void run() {
                                if (webuiManager != null) {
                                        webuiManager.refreshPlugins();
                                    }
                            }
                    };
                resetManager.deleteSpecificModule(moduleBasePath, onModuleDeleteComplete);
            }
            
        private void ensureExternalWebSuDir() {
                try {
                        File externalDir = getExternalFilesDir(null);
                        if (externalDir == null) {
                                Log.e(TAG, "External storage tidak tersedia!");
                                return;
                            }

                        File pluginDir = new File(externalDir, LOG_DIR_NAME);
                        if (!pluginDir.exists()) {
                                boolean created = pluginDir.mkdirs();
                                if (created) {
                                        Log.d(TAG, "Folder logs dibuat.");
                                    } else {
                                        Log.e(TAG, "Gagal membuat folder logs.");
                                    }
                            }

                        File marker = new File(pluginDir, ".keep");
                        if (!marker.exists()) {
                                try (FileOutputStream fos = new FileOutputStream(marker)) {
                                        fos.write("keep\n Apk ini akan terus berkembang seiring waktu".getBytes());
                                    }
                            }

                    } catch (Throwable t) {
                        Log.e(TAG, "Gagal membuat folder eksternal", t);
                    }
            }

        @Override
        protected void onDestroy() {
                super.onDestroy();
                cachedWebuiView = null;
            }
    }

