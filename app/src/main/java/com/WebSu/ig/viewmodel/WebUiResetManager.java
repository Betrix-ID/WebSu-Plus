package com.WebSu.ig.viewmodel;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import java.io.File;

import com.WebSu.ig.ShellExecutor;
import com.WebSu.ig.websuCard.PluginWebuiManager;

import java.io.File;

public class WebUiResetManager {

        private static final String TAG = "WebUiResetManager";
        private final Activity activity;
        private final View rootView;

        public WebUiResetManager(Activity activity, View rootView) {
                this.activity = activity;
                this.rootView = rootView;
            }

        public void attachResetAction(int buttonId, final Runnable onResetSuccess) {
                View btnReset = rootView.findViewById(buttonId);

                if (btnReset != null) {
                        btnReset.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                            performResetAll(onResetSuccess);
                                        }
                                });
                    }
            }

        private void performResetAll(final Runnable onSuccess) {
                Toast.makeText(activity, "Mereset semua WebUI...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Memicu Global Reset untuk semua WebUI.");

                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    String targetDir = PluginWebuiManager.WEBUI_BASE_DIR + "*";
                                    String cmd = "rm -rf " + targetDir;

                                    ShellExecutor.execSync(cmd, activity);

                                    activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                        Toast.makeText(activity, "WebUI berhasil direset bersih.", Toast.LENGTH_SHORT).show();

                                                        if (onSuccess != null) {
                                                                onSuccess.run();
                                                            }
                                                    }
                                            });
                                }
                        }).start();
            }

        public void deleteSpecificModule(final String dirPath, final Runnable onModuleDeleteComplete) {

                Toast.makeText(activity, "Menghapus modul di: " + dirPath, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Memicu penghapusan modul spesifik: " + dirPath);

                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    final String resultMessage = executeDelete(dirPath);
                                    deleteCachedBanner(dirPath);

                                    activity.runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                        Toast.makeText(activity, resultMessage, Toast.LENGTH_SHORT).show();

                                                        if (onModuleDeleteComplete != null) {
                                                                onModuleDeleteComplete.run();
                                                            }
                                                    }
                                            });
                                }
                        }).start();
            }

        private String executeDelete(String dirPath) {
                String safeDirPath = escapeShellArg(dirPath);
                String cmd = String.format("rm -rf %s", safeDirPath);

                ShellExecutor.Result res = ShellExecutor.execSync(cmd, activity);

                String moduleName = new File(dirPath).getName();

                if (res.exitCode == 0) {
                        Log.i(TAG, "Modul berhasil dihapus: " + dirPath);
                        return "Modul '" + moduleName + "' berhasil dihapus.";
                    } else {
                        String error = res.stderr != null ? res.stderr.trim() : "";
                        Log.e(TAG, "Gagal menghapus modul: " + dirPath + " | Exit: " + res.exitCode + " | Stderr: " + error);
                        return "Gagal menghapus modul '" + moduleName + "'. Cek log.";
                    }
            }

        private void deleteCachedBanner(String moduleBasePath) {
                try {
                        String moduleId = new File(moduleBasePath).getName();
                        File externalFilesDir = activity.getExternalFilesDir(null);
                        if (externalFilesDir == null) return;

                        File bannerDir = new File(externalFilesDir, "webui_banners");
                        if (bannerDir.exists() && bannerDir.isDirectory()) {
                                File[] files = bannerDir.listFiles();
                                if (files != null) {
                                        for (File file : files) {
                                                if (file.getName().startsWith(moduleId + "_")) {
                                                        if (file.delete()) {
                                                                Log.d(TAG, "Banner cache dihapus: " + file.getName());
                                                            } else {
                                                                Log.w(TAG, "Gagal menghapus banner cache: " + file.getName());
                                                            }
                                                    }
                                            }
                                    }
                            }
                    } catch (Exception e) {
                        Log.e(TAG, "Error saat menghapus banner cache: " + e.getMessage());
                    }
            }

        private static String escapeShellArg(String arg) {
                if (arg == null) return "''";
                return "'" + arg.replace("'", "'\\''") + "'";
            }
    }

