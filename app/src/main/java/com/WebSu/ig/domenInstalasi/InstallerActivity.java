package com.WebSu.ig.domenInstalasi;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.WebSu.ig.R;
import com.WebSu.ig.ShellExecutor;
import com.WebSu.ig.updateStatusBarColor;
import com.WebSu.ig.LogUtils;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class InstallerActivity extends Activity {

        private static final String TAG = "InstallerActivity";

        private Handler handler;
        private ExecutorService ioExecutor;
        private volatile Process currentShellProcess = null;
        public static final String ZIP_PATH = "/data/user_de/0/com.android.shell/WebSu/zip/module.zip";
        public static final String WEBUI_BASE_DIR = "/data/user_de/0/com.android.shell/WebSu/webui/";
        public static final String SYSTEM_SBIN_DIR = "/data/user_de/0/com.android.shell/WebSu/system/sbin";

        private static final String POST_INSTALL_SCRIPT = "Amber.sh";
        private static final String LOSSY_SCRIPT = "lossy.sh"; 
        private static final String BANNER_SEPARATOR = "**************************";
        private static final String BANNER_TEMPLATE =
        "\n" + BANNER_SEPARATOR + "\n" +
        "%s\n" +
        "%s\n" +
        BANNER_SEPARATOR + "\n\n" +
        BANNER_SEPARATOR + "\n" +
        "Powered by WebSu Plus\n" +
        BANNER_SEPARATOR;

        private static final String UI_PRINT_FUNCTION = "ui_print() { echo \"$1\"; }; ";

        private ProgressBar loadingIndicator;
        private TextView loadingStatusText;
        private ScrollView scrollView;
        private TextView logView;
        private TextView titleView;
        private Button openWebuiBtn;
        private String installedModuleId = null;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);

                handler = new Handler(Looper.getMainLooper());
                ioExecutor = Executors.newSingleThreadExecutor();

                requestWindowFeature(Window.FEATURE_NO_TITLE);
                setContentView(R.layout.domen);

                Window window = getWindow();
                if (window != null) {
                        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                        window.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#D9D9D9")));
                    }

                try {
                        updateStatusBarColor.updateStatusBarColor(this);
                    } catch (Throwable t) {
                        LogUtils.writeLog(this, "ERROR", "Gagal memanggil updateStatusBarColor: " + t.getMessage());
                    }

                scrollView = findViewById(R.id.scroll_console);
                logView = findViewById(R.id.text_console);
                titleView = findViewById(R.id.title);
                openWebuiBtn = findViewById(R.id.btn_finished);
                final ImageView btnBack = findViewById(R.id.btn_kembali);

                loadingIndicator = findViewById(R.id.loading_indicator);
                loadingStatusText = findViewById(R.id.text_loading_status);

                if (logView == null || titleView == null || scrollView == null || openWebuiBtn == null || btnBack == null || loadingIndicator == null || loadingStatusText == null) {
                        showToast("Layout 'domen' tidak lengkap!");
                        LogUtils.writeLog(this, "FATAL", "Layout 'domen' tidak lengkap, InstallerActivity gagal dimulai.");
                        finish();
                        return;
                    }

                LogUtils.writeLog(this, "INFO", "InstallerActivity dimulai. Memuat file ZIP dari: " + ZIP_PATH);

                btnBack.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    LogUtils.writeLog(InstallerActivity.this, "INFO", "Instalasi dibatalkan oleh pengguna (tombol kembali).");
                                    stopCurrentLiveProcess();
                                    shutdownExecutorNow();
                                    finish();
                                }
                        });

                View root = findViewById(android.R.id.content);
                if (root != null) {
                        root.setAlpha(0f);
                        root.animate().alpha(1f).setDuration(400).start();
                    }

                titleView.setText("Flashing WebUI");
                titleView.setTextColor(Color.parseColor("#FFA500"));
                openWebuiBtn.setVisibility(View.INVISIBLE);
                openWebuiBtn.setEnabled(false);
                loadingIndicator.setVisibility(View.GONE); 
                loadingStatusText.setVisibility(View.GONE); 

                startInstallation(logView, scrollView, titleView, openWebuiBtn);
            }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        return true;
                    }
                return super.onKeyDown(keyCode, event);
            }

        private void startInstallation(final TextView logView, final ScrollView scrollView,
                                       final TextView titleView, final Button openWebuiBtn) {

                ioExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                    final String ZIP_DIR = new File(ZIP_PATH).getParent();
                                    final String EXTRACTED_PROP_PATH = ZIP_DIR + "/module.prop";
                                    String moduleBanner = "";
                                    String targetDir = null;
                                    String moduleId = null;

                                    try {
                                            handler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                logView.setTypeface(null, android.graphics.Typeface.BOLD);
                                                                logView.setText("");
                                                            }
                                                    });

                                            appendLog(logView, scrollView, "- Extracting module.prop...");
                                            LogUtils.writeLog(InstallerActivity.this, "DEBUG", "Step 1: Mengekstrak module.prop.");
                                            String cmd1 = String.format("cd \"%s\" && unzip -o \"%s\" module.prop", ZIP_DIR, ZIP_PATH);
                                            ShellExecutor.Result res1 = ShellExecutor.execSync(cmd1, InstallerActivity.this);

                                            if (res1 == null || res1.exitCode != 0) {
                                                    appendLog(logView, scrollView, "Error: Gagal mengekstrak module.prop!");
                                                    finishWithError(titleView);
                                                    return;
                                                }
                                            if (res1.stdout != null && !res1.stdout.isEmpty()) {
                                                    appendLog(logView, scrollView, res1.stdout.trim());
                                                }

                                            moduleId = readPropertyFromShell(EXTRACTED_PROP_PATH, "id");
                                            final String moduleName = readPropertyFromShell(EXTRACTED_PROP_PATH, "name");
                                            final String moduleAuthor = readPropertyFromShell(EXTRACTED_PROP_PATH, "author");

                                            if (moduleId == null || moduleId.isEmpty()) {
                                                    appendLog(logView, scrollView, "Error: Tidak menemukan id di " + EXTRACTED_PROP_PATH);
                                                    LogUtils.writeLog(InstallerActivity.this, "ERROR", "ID modul kosong/tidak ditemukan di module.prop.");
                                                    finishWithError(titleView);
                                                    return;
                                                }

                                            targetDir = WEBUI_BASE_DIR + moduleId;
                                            installedModuleId = moduleId;
                                            LogUtils.writeLog(InstallerActivity.this, "INFO", "Target instalasi (MODPATH): " + targetDir);


                                            String nameToDisplay = (moduleName != null && moduleName.length() > 0) ? moduleName : moduleId;
                                            String authorLine = (moduleAuthor != null && moduleAuthor.length() > 0) ? "by " + moduleAuthor : "by Unknown Author";
                                            moduleBanner = String.format(BANNER_TEMPLATE, nameToDisplay, authorLine);
                                            appendLog(logView, scrollView, moduleBanner);

                                            String cmd2 = String.format("mkdir -p \"%s\"", targetDir);
                                            ShellExecutor.Result res2 = ShellExecutor.execSync(cmd2, InstallerActivity.this);

                                            if (res2 == null || res2.exitCode != 0) {
                                                    appendLog(logView, scrollView, "Error: Gagal membuat direktori target!");
                                                    finishWithError(titleView);
                                                    return;
                                                }
                                            appendLog(logView, scrollView, "\n- Extracting module files\n");
                                            LogUtils.writeLog(InstallerActivity.this, "DEBUG", "Step 3: Mengekstrak isi ZIP ke target.");

                                            String cmd3 = String.format("cd \"%s\" && unzip -o \"%s\" -x 'META-INF/*'", targetDir, ZIP_PATH);
                                            ShellExecutor.Result res3 = ShellExecutor.execSync(cmd3, InstallerActivity.this);

                                            if (res3 != null && res3.stdout != null && !res3.stdout.isEmpty()) {
                                                    appendLog(logView, scrollView, res3.stdout.trim());
                                                }

                                            if (res3 == null || res3.exitCode != 0) {
                                                    appendLog(logView, scrollView, "\nError: Unzip failed!");
                                                    finishWithError(titleView);
                                                    return;
                                                }

                                            final String AMBER_IN_MODPATH = targetDir + "/" + POST_INSTALL_SCRIPT;

                                            appendLog(logView, scrollView, "\n- Running post-install script: " + POST_INSTALL_SCRIPT + "\n");
                                            LogUtils.writeLog(InstallerActivity.this, "DEBUG", "Step 4: Mencari dan mengeksekusi " + POST_INSTALL_SCRIPT + ".");
                                            ShellExecutor.Result resFind = ShellExecutor.execSync(String.format("test -f \"%s\"", AMBER_IN_MODPATH), InstallerActivity.this);

                                            if (resFind != null && resFind.exitCode == 0) {
                                                    LogUtils.writeLog(InstallerActivity.this, "DEBUG", POST_INSTALL_SCRIPT + " ditemukan di MODPATH. Melakukan chmod dan eksekusi LIVE.");

                                                    ShellExecutor.execSync(String.format("chmod 777 \"%s\"", AMBER_IN_MODPATH), InstallerActivity.this);

                                                    String cmdAmberExec = String.format(
                                                        "%s export MODPATH=\"%s\"; . \"%s\"", 
                                                        UI_PRINT_FUNCTION,
                                                        targetDir,
                                                        AMBER_IN_MODPATH
                                                    );


                                                    int liveExitCode = -1;
                                                    try {
                                                            liveExitCode = runLiveScript(cmdAmberExec, logView, scrollView); 
                                                        } catch (final Exception e) {
                                                            appendLog(logView, scrollView, "Error: Gagal memulai Live Script: " + e.getMessage());
                                                            LogUtils.writeLog(InstallerActivity.this, "FATAL", "Gagal memulai Live Script: " + e.getMessage());
                                                            liveExitCode = -1;
                                                        }
                                                    if (liveExitCode != 0) {
                                                            appendLog(logView, scrollView, "\nWarning: " + POST_INSTALL_SCRIPT + " failed with exit code " + liveExitCode);
                                                            LogUtils.writeLog(InstallerActivity.this, "WARN", POST_INSTALL_SCRIPT + " gagal. Exit: " + liveExitCode);                                                            
                                                        } else {
                                                            LogUtils.writeLog(InstallerActivity.this, "INFO", POST_INSTALL_SCRIPT + " berhasil dieksekusi. Exit: " + liveExitCode);                           
                                                        }
                                                } else {
                                                    appendLog(logView, scrollView, "\nWarning: " + POST_INSTALL_SCRIPT + " tidak ditemukan di dalam modul. Melewati eksekusi.");
                                                    LogUtils.writeLog(InstallerActivity.this, "WARN", POST_INSTALL_SCRIPT + " tidak ditemukan atau gagal diekstrak/ditemukan.");                                                    
                                                }

                                            appendLog(logView, scrollView, "\n- Checking for system/sbin files...");
                                            LogUtils.writeLog(InstallerActivity.this, "DEBUG", "Step 5: Memeriksa dan menyalin binari system/sbin.");

                                            final String MODPATH_SBIN_DIR = targetDir + "/system/sbin/";

                                            String cmdCopySbin = String.format(
                                                "if [ -d \"%s\" ]; then mkdir -p \"%s\"; cp -af \"%s\"/* \"%s\"; fi",
                                                MODPATH_SBIN_DIR, SYSTEM_SBIN_DIR, MODPATH_SBIN_DIR, SYSTEM_SBIN_DIR
                                            );
                                            ShellExecutor.Result resCopy = ShellExecutor.execSync(cmdCopySbin, InstallerActivity.this);

                                            if (resCopy != null && resCopy.exitCode == 0) {
                                                    appendLog(logView, scrollView, "System/sbin check: Salinan binari selesai (jika ada).");
                                                    LogUtils.writeLog(InstallerActivity.this, "INFO", "Penyalinan binari system/sbin berhasil diselesaikan.");
                                                } else {
                                                    appendLog(logView, scrollView, "System/sbin check: Gagal menyalin binari atau direktori tidak ada.");
                                                    LogUtils.writeLog(InstallerActivity.this, "WARN", "Gagal menyalin binari system/sbin. Exit: " + ((resCopy != null) ? resCopy.exitCode : -1));
                                                }

                                            ShellExecutor.execSync(String.format("rm -f \"%s\"", EXTRACTED_PROP_PATH), InstallerActivity.this);
                                            LogUtils.writeLog(InstallerActivity.this, "DEBUG", "Step 6: Membersihkan module.prop sementara.");

                                            appendLog(logView, scrollView, "\n- Done");
                                            LogUtils.writeLog(InstallerActivity.this, "SUCCESS", "Instalasi modul WebUI '" + installedModuleId + "' berhasil diselesaikan.");
                                            handler.post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                titleView.animate().alpha(0f).setDuration(300)
                                                                    .withEndAction(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                    titleView.setText("Success");
                                                                                    titleView.setTextColor(Color.parseColor("#FF197729"));
                                                                                    titleView.animate().alpha(1f).setDuration(800).start();
                                                                                }
                                                                        }).start();

                                                                openWebuiBtn.setVisibility(View.VISIBLE);
                                                                openWebuiBtn.setAlpha(0f);
                                                                openWebuiBtn.setEnabled(true);
                                                                openWebuiBtn.animate().alpha(1f).setDuration(600).start();
                                                                openWebuiBtn.setOnClickListener(new View.OnClickListener() {
                                                                            @Override
                                                                            public void onClick(View v) {
                                                                                    runLossyScriptAndFinish();
                                                                                }
                                                                        });
                                                            }
                                                    });

                                        } catch (final Throwable t) {
                                            LogUtils.writeLog(InstallerActivity.this, "FATAL", "Kesalahan fatal saat instalasi: " + t.getMessage());
                                            appendLog(logView, scrollView, "Error: " + (t != null ? t.getMessage() : "unknown"));
                                            finishWithError(titleView);
                                            stopCurrentLiveProcess();
                                            shutdownExecutorNow();
                                        }
                                }
                        });
            }

        private void runLossyScriptAndFinish() {
                final String moduleDir = WEBUI_BASE_DIR + installedModuleId;
                final String lossyScriptPath = moduleDir + "/" + LOSSY_SCRIPT;
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    openWebuiBtn.setVisibility(View.GONE);
                                    loadingIndicator.setVisibility(View.VISIBLE);
                                    loadingStatusText.setVisibility(View.VISIBLE);
                                    loadingStatusText.setText("Loading...");
                                    logView.setText("");
                                    logView.setTypeface(null, android.graphics.Typeface.NORMAL);
                                }
                        });


                ioExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                    LogUtils.writeLog(InstallerActivity.this, "DEBUG", "Mengeksekusi " + LOSSY_SCRIPT + " setelah tombol ditekan.");
                                    ShellExecutor.Result resFind = ShellExecutor.execSync(String.format("test -f \"%s\"", lossyScriptPath), InstallerActivity.this);
                                    boolean scriptFound = (resFind != null && resFind.exitCode == 0);

                                    String finalStatusMessage;

                                    if (scriptFound) {
                                            ShellExecutor.execSync(String.format("chmod 777 \"%s\"", lossyScriptPath), InstallerActivity.this);

                                            String cmdLossyExec = String.format(
                                                "%s export MODPATH=\"%s\"; export SYSTEM_SBIN_DIR=\"%s\"; export PATH=\"%s:$PATH\"; . \"%s\"",
                                                UI_PRINT_FUNCTION,
                                                moduleDir,
                                                SYSTEM_SBIN_DIR,
                                                SYSTEM_SBIN_DIR,
                                                lossyScriptPath
                                            );

                                            try {
                                                    runLossyScriptAsDaemon(cmdLossyExec);

                                                    LogUtils.writeLog(InstallerActivity.this, "INFO", LOSSY_SCRIPT + " berhasil diinisiasi sebagai background service.");
                                                    finalStatusMessage = "Successfully Flasing.";

                                                } catch (final Exception e) {
                                                    LogUtils.writeLog(InstallerActivity.this, "FATAL", "Gagal menjalankan Shell Executor (" + LOSSY_SCRIPT + "): " + e.getMessage());
                                                    finalStatusMessage = "Installation Failed.";
                                                }
                                        } else {
                                            LogUtils.writeLog(InstallerActivity.this, "WARN", LOSSY_SCRIPT + " tidak ditemukan.");
                                            finalStatusMessage = "Successfully Flasing."; 
                                        }

                                    try {
                                            Thread.sleep(900); 
                                        } catch (InterruptedException ignored) {}
                                    final String finalMsg = finalStatusMessage;
                                    handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                        String displayMsg = (finalMsg.equals("Installation Failed.")) ? finalMsg : "Successfully Flasing.";
                                                        loadingStatusText.setText(displayMsg);

                                                        handler.postDelayed(new Runnable() { 
                                                                    @Override
                                                                    public void run() {
                                                                            loadingIndicator.setVisibility(View.GONE);
                                                                            loadingStatusText.setVisibility(View.GONE);

                                                                            Intent data = new Intent();
                                                                            data.putExtra("installed_module_id", installedModuleId);
                                                                            setResult(Activity.RESULT_OK, data);

                                                                            shutdownExecutorNow();
                                                                            finish();
                                                                        }
                                                                }, 3000); 
                                                    }
                                            });
                                }
                        });
            }

        private class LiveStreamReader implements Runnable {
                private final BufferedReader reader;
                private final TextView logView;
                private final ScrollView scrollView;

                LiveStreamReader(InputStream inputStream, TextView logView, ScrollView scrollView) {
                        this.reader = new BufferedReader(new InputStreamReader(inputStream));
                        this.logView = logView;
                        this.scrollView = scrollView;
                    }

                @Override
                public void run() {
                        try {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                        appendLog(logView, scrollView, line);
                                    }
                            } catch (IOException ignored) {
                            } finally {
                                try { reader.close(); } catch (IOException ignored) {}
                            }
                    }
            }

        private void runLossyScriptAsDaemon(String cmd) throws Exception {
                String daemonCmd = String.format("nohup sh -c \"%s\" >/dev/null 2>&1 &", cmd.replace("\"", "\\\""));
                LogUtils.writeLog(InstallerActivity.this, "DEBUG", "Mengeksekusi Lossy Script sebagai Daemon (Non-Blokir): " + daemonCmd);
                ShellExecutor.Result res = ShellExecutor.execSync(daemonCmd, this);

                if (res.exitCode != 0) {
                        throw new IOException("Gagal menjalankan Lossy Script sebagai daemon. Exit code: " + res.exitCode + ", Stderr: " + res.stderr);
                    }
            }

        // HAPUS atau NONAKTIFKAN metode lama yang memblokir (runLossyScript)
        /*
         private int runLossyScript(String cmd) throws Exception {
         LogUtils.writeLog(InstallerActivity.this, "DEBUG", "Mengeksekusi Lossy Script LIVE (output suppressed): " + cmd);

         Process process = ShellExecutor.execProcessLive(cmd);
         currentShellProcess = process;

         int exitCode = process.waitFor();
         currentShellProcess = null;
         return exitCode;
         }
         */

        private int runLiveScript(String cmd, TextView logView, ScrollView scrollView) throws Exception {
                LogUtils.writeLog(this, "DEBUG", "Mengeksekusi Live Script menggunakan ShellExecutor: " + cmd);

                Process process = ShellExecutor.execProcessLive(cmd);

                currentShellProcess = process;

                Thread stdoutThread = new Thread(new LiveStreamReader(process.getInputStream(), logView, scrollView));
                Thread stderrThread = new Thread(new LiveStreamReader(process.getErrorStream(), logView, scrollView));

                stdoutThread.start();
                stderrThread.start();

                int exitCode = process.waitFor();

                stdoutThread.join(500);
                stderrThread.join(500);

                currentShellProcess = null;
                return exitCode;
            }


        private void stopCurrentLiveProcess() {
                if (currentShellProcess != null) {
                        try {
                                currentShellProcess.destroyForcibly(); 
                                currentShellProcess = null;
                            } catch (Exception ignored) {
                                LogUtils.writeLog(this, "WARN", "Gagal menghancurkan proses shell live.");
                            }
                    }
            }

        private String readPropertyFromShell(String propPath, String key) {
                if (propPath == null || key == null) return null;

                String cmd = String.format("grep '^%s=' \"%s\" | head -n 1 | cut -d= -f2-", key, propPath);
                ShellExecutor.Result res = ShellExecutor.execSync(cmd, this);

                if (res != null && res.exitCode == 0 && res.stdout != null && !res.stdout.isEmpty()) {
                        String value = res.stdout.trim();
                        if (value.length() == 0) {
                                LogUtils.writeLog(this, "WARN", "Shell berhasil tapi output untuk '" + key + "' kosong.");
                                return null;
                            }
                        LogUtils.writeLog(this, "DEBUG", "Nilai '" + key + "' ditemukan: " + value);
                        return value;
                    } else {
                        int exit = (res != null) ? res.exitCode : -1;
                        LogUtils.writeLog(this, "ERROR", "Gagal membaca '" + key + "' dari module.prop via shell. Exit: " + exit + ". Stderr: " + ((res != null && res.stderr != null) ? res.stderr.trim() : "N/A"));
                        return null;
                    }
            }

        private void appendLog(final TextView textView, final ScrollView scrollView, final String text) {
                if (textView == null || scrollView == null) return;
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    textView.append(text + "\n");
                                    scrollView.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                        scrollView.fullScroll(View.FOCUS_DOWN);
                                                    }
                                            });
                                }
                        });
            }

        private void finishWithError(final TextView titleView) {
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    titleView.animate().alpha(0f).setDuration(400)
                                        .withEndAction(new Runnable() {
                                                @Override
                                                public void run() {
                                                        titleView.setText("Failed Installer");
                                                        titleView.setTextColor(Color.parseColor("#8B1E1E"));
                                                        titleView.animate().alpha(1f).setDuration(400).start();
                                                    }
                                            }).start();
                                }
                        });
            }

        private void showToast(final String msg) {
                handler.post(new Runnable() {
                            @Override
                            public void run() {
                                    Toast.makeText(InstallerActivity.this, msg, Toast.LENGTH_SHORT).show();
                                }
                        });
            }

        private void shutdownExecutorNow() {
                if (ioExecutor != null && !ioExecutor.isShutdown()) {
                        try {
                                ioExecutor.shutdownNow();
                                ioExecutor.awaitTermination(200, TimeUnit.MILLISECONDS);
                            } catch (InterruptedException e) {
                                LogUtils.writeLog(this, "WARN", "Interrupted saat shutdown executor: " + e.getMessage());
                            } catch (Throwable t) {
                                LogUtils.writeLog(this, "WARN", "Error saat shutdown executor: " + t.getMessage());
                            }
                    }
            }

        @Override
        protected void onDestroy() {
                stopCurrentLiveProcess();
                shutdownExecutorNow();
                super.onDestroy();
            }
    }

