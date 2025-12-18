package com.WebSu.ig.viewmodel;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.database.Cursor;
import android.provider.OpenableColumns;
import androidx.core.content.ContextCompat;

import com.WebSu.ig.R;
import com.WebSu.ig.ShellExecutor;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import com.WebSu.ig.domenInstalasi.InstallerActivity;
import android.view.WindowManager;

public class ZipFilePicker {

        public static final int REQUEST_PICK_ZIP = 101;
        public static final int DEFAULT_REQUEST_CODE_INSTALL = 1001; 

        private final Activity activity;
        private int currentRequestCode = DEFAULT_REQUEST_CODE_INSTALL; 

        public ZipFilePicker(final Activity activity, View containerView) {
                this.activity = activity;
                this.currentRequestCode = DEFAULT_REQUEST_CODE_INSTALL; 

                final Button btnOpenFile = (Button) containerView.findViewById(R.id.btn_open_file);
                if (btnOpenFile != null) {
                        btnOpenFile.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                            try {
                                                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                                    intent.setType("application/zip");
                                                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                                                    activity.startActivityForResult( 
                                                        Intent.createChooser(intent, "Pilih file ZIP"),
                                                        REQUEST_PICK_ZIP
                                                    );
                                                } catch (Exception e) {
                                                    Toast.makeText(activity, "Gagal membuka picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    writeLog("ERROR", "Gagal membuka file picker: " + e.getMessage());
                                                }
                                        }
                                });
                    }
            }

        public void handleActivityResult(int requestCode, int resultCode, Intent data) {
                if (requestCode == REQUEST_PICK_ZIP && resultCode == Activity.RESULT_OK) {
                        if (data != null && data.getData() != null) {
                                processZipUri(data.getData());
                            }
                    }
            }

        public void startInstallProcess(Uri zipUri, int requestCode) {
                this.currentRequestCode = requestCode; 
                processZipUri(zipUri);
            }

        private void processZipUri(final Uri zipUri) {
                InputStream in = null;
                OutputStream out = null;
                try {
                        String tempName = getFileNameFromUri(zipUri);
                        if (tempName == null) tempName = "downloaded_module.zip";

                        final String fileName = tempName;
                        File externalDir = new File(activity.getExternalFilesDir(null), "zips");
                        if (!externalDir.exists()) externalDir.mkdirs();

                        final File externalZip = new File(externalDir, fileName);
                        if ("content".equals(zipUri.getScheme())) {
                                in = activity.getContentResolver().openInputStream(zipUri);
                            } else {
                                File sourceFile = new File(zipUri.getPath());
                                if (!sourceFile.exists()) {
                                        throw new IOException("File fisik tidak ditemukan: " + zipUri.getPath());
                                    }
                                in = new FileInputStream(sourceFile);
                            }

                        out = new FileOutputStream(externalZip);
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                                out.write(buffer, 0, len);
                            }
                            
                        showInstallDialog(fileName, externalZip);
                    } catch (Exception e) {
                        Toast.makeText(activity, "Gagal menyalin ZIP: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        writeLog("ERROR", "Kesalahan prosesZipUri: " + e.getMessage());
                    } finally {
                        try {
                                if (in != null) in.close();
                                if (out != null) out.close();
                            } catch (IOException ignored) {}
                    }
            }

        private void showInstallDialog(final String fileName, final File zipFile) {
                final Dialog dialog = new Dialog(activity);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                View view = LayoutInflater.from(activity).inflate(R.layout.dialog_install_plugin, null);

                TextView tvTitle = (TextView) view.findViewById(R.id.dialog_title);
                TextView tvMsg = (TextView) view.findViewById(R.id.dialog_message);
                Button btnInstall = (Button) view.findViewById(R.id.btn_install);

                tvTitle.setText("Instal Plugin WebUI?");
                tvMsg.setText("File ZIP ditemukan. Ingin menginstal modul berikut?\n\n" + fileName);

                view.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    dialog.dismiss();
                                }
                        });

                btnInstall.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    dialog.dismiss();
                                    installZipToShell(zipFile, fileName);
                                    Intent intent = new Intent(activity, InstallerActivity.class);
                                    activity.startActivityForResult(intent, currentRequestCode); 
                                }
                        });

                final FrameLayout wrapper = new FrameLayout(activity);
                wrapper.addView(view, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

                dialog.setContentView(wrapper);
                Window window = dialog.getWindow();
                if (window != null) {
                        window.setLayout(-1, -1);
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.gravity = Gravity.BOTTOM;
                        window.setAttributes(lp);
                    }
                dialog.show();
            }

        private void installZipToShell(File externalZip, String fileName) {
                try {
                        String safeExtPath = escapeShellArg(externalZip.getAbsolutePath());
                        String destDir = "/data/user_de/0/com.android.shell/WebSu/zip";
                        String safeDestPath = escapeShellArg(destDir + "/module.zip");

                        ShellExecutor.execSync("mkdir -p " + escapeShellArg(destDir), activity);
                        ShellExecutor.execSync("chmod 755 " + safeExtPath, activity);
                        ShellExecutor.Result result = ShellExecutor.execSync("cp -f " + safeExtPath + " " + safeDestPath, activity);

                        if (result.exitCode == 0) {
                                writeLog("SUCCESS", "Berhasil copy shell: " + fileName);
                            } else {
                                writeLog("ERROR", "Shell CP fail: " + result.stderr);
                            }
                    } catch (Exception e) {
                        writeLog("ERROR", "Crash installZipToShell: " + e.getMessage());
                    }
            }

        private String getFileNameFromUri(Uri uri) {
                String result = null;
                if ("content".equals(uri.getScheme())) {
                        Cursor cursor = null;
                        try {
                                cursor = activity.getContentResolver().query(uri, null, null, null, null);
                                if (cursor != null && cursor.moveToFirst()) {
                                        int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                        if (idx >= 0) result = cursor.getString(idx);
                                    }
                            } finally {
                                if (cursor != null) cursor.close();
                            }
                    }
                if (result == null && uri != null) {
                        String path = uri.getPath();
                        int cut = path.lastIndexOf('/');
                        result = (cut != -1) ? path.substring(cut + 1) : path;
                    }
                return result;
            }

        private void writeLog(String type, String message) {
                BufferedWriter bw = null;
                try {
                        File logFile = new File(activity.getExternalFilesDir(null), "logs/zip_copy.log");
                        if (!logFile.getParentFile().exists()) logFile.getParentFile().mkdirs();
                        bw = new BufferedWriter(new FileWriter(logFile, true));
                        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                        bw.write("[" + time + "] [" + type + "] " + message + "\n");
                    } catch (IOException ignored) {} finally {
                        if (bw != null) try { bw.close(); } catch (IOException ignored) {}
                    }
            }

        private static String escapeShellArg(String arg) {
                if (arg == null) return "''";
                return "'" + arg.replace("'", "'\\''") + "'";
            }
    }

