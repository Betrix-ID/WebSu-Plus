package com.WebSu.ig.websuCard;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.Button;
import android.widget.TextView;
import android.widget.FrameLayout;
import com.WebSu.ig.R;
import com.WebSu.ig.ShellExecutor;
import com.WebSu.ig.LogUtils; // Asumsi LogUtils ada

public class UninstallConfirmationDialog extends Dialog {

        private static final String TAG = "UninstallDialog";
        private static final String UNINSTALL_SCRIPT = "uninstall.sh";
        private static final String SYSTEM_SBIN_DIR = "/data/user_de/0/com.android.shell/WebSu/system/sbin";

        public interface UninstallDialogListener {
                void onUninstallConfirmed(String moduleId, String moduleBasePath);
            }

        private UninstallDialogListener listener;
        private final String moduleId;
        private final String moduleBasePath;

        private View customLoadingView;
        private TextView loadingTextView;
        private FrameLayout wrapper;
        private View dialogContentView;
        private final Context context;

        // ** METODE BARU: Memungkinkan listener disetel secara ekplisit **
        // Inilah yang akan dipanggil oleh MainActivity yang baru.
        public void setUninstallDialogListener(UninstallDialogListener listener) {
                this.listener = listener;
            }

        public UninstallConfirmationDialog(Context context, String moduleId, String moduleBasePath) {
                super(context);
                this.context = context;
                this.moduleId = moduleId;
                this.moduleBasePath = moduleBasePath;

                // ** PERBAIKAN PENTING: Hapus logic casting yang berisiko **
                // Kita tidak lagi memaksa Context menjadi listener. Listener akan disuntikkan
                // melalui setUninstallDialogListener() di MainActivity.
                /*
                 try {
                 // Memastikan Activity (Context) mengimplementasikan listener
                 listener = (UninstallDialogListener) context;
                 } catch (ClassCastException e) {
                 Log.e(TAG, context.toString() + " must implement UninstallDialogListener");
                 if (context instanceof Activity) {
                 throw new ClassCastException(context.toString()
                 + " must implement UninstallDialogListener");
                 }
                 }
                 */
            }

        private void showLoadingOverlay(String message) {
                if (wrapper != null) {
                        if (customLoadingView == null) {
                                // Menggunakan context untuk LayoutInflater
                                customLoadingView = LayoutInflater.from(context).inflate(R.layout.layout_custom_loading, wrapper, false);
                                loadingTextView = customLoadingView.findViewById(R.id.loading_text);
                                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    Gravity.CENTER);

                                wrapper.addView(customLoadingView, params);
                            }

                        if (dialogContentView != null) {
                                dialogContentView.setVisibility(View.GONE);
                            }

                        if (loadingTextView != null) {
                                loadingTextView.setText(message);
                                loadingTextView.setVisibility(View.VISIBLE);
                            }
                        customLoadingView.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Menampilkan loading overlay: " + message);
                        LogUtils.writeLog(context, "INFO", "Memulai proses uninstall untuk modul: " + moduleId);
                    }
            }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                requestWindowFeature(Window.FEATURE_NO_TITLE);

                dialogContentView = LayoutInflater.from(context).inflate(R.layout.dialog_delete, null);

                TextView messageTextView = dialogContentView.findViewById(R.id.dialog_pesan);
                final Button btnCancel = dialogContentView.findViewById(R.id.btn_batal);
                final Button btnUninstall = dialogContentView.findViewById(R.id.btn_uninstall);

                if (messageTextView != null) {
                        messageTextView.setText("Are you sure you want to uninstall module: " + moduleId + " ?");
                    }

                if (btnCancel != null) {
                        btnCancel.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                            Log.d(TAG, "Penghapusan dibatalkan.");
                                            LogUtils.writeLog(context, "INFO", "Uninstall dibatalkan oleh pengguna.");
                                            dismiss(); 
                                        }
                                });
                    }

                if (btnUninstall != null) {
                        btnUninstall.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                            showLoadingOverlay("Loading...");
                                            btnUninstall.setEnabled(false);
                                            btnCancel.setEnabled(false);

                                            if (listener != null) {
                                                    // Panggil listener, ini akan memicu performModuleDeletion di MainActivity
                                                    listener.onUninstallConfirmed(moduleId, moduleBasePath);
                                                }

                                            new Thread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                final String MODPATH = moduleBasePath;
                                                                final String uninstallScriptPath = MODPATH + "/" + UNINSTALL_SCRIPT;

                                                                String cmd = String.format(
                                                                    "export MODPATH=\"%s\"; " +
                                                                    "export PATH=\"%s:$PATH\"; " +
                                                                    "echo \"Attempting to run uninstall script (optional)....\"; " +
                                                                    ". \"%s\" 2>/dev/null || true",
                                                                    MODPATH,
                                                                    SYSTEM_SBIN_DIR,
                                                                    uninstallScriptPath
                                                                );

                                                                Log.d(TAG, "Mengeksekusi perintah: " + cmd);
                                                                LogUtils.writeLog(context, "DEBUG", "Perintah Shell Uninstall: " + cmd);

                                                                String finalMessageForLog;
                                                                String displayMessage;
                                                                Context threadContext = context; 

                                                                try {
                                                                        ShellExecutor.Result scriptResult = ShellExecutor.execSync(cmd, threadContext);

                                                                        if (scriptResult.exitCode == 0) {
                                                                                Log.i(TAG, "uninstall.sh berhasil dieksekusi atau dilewati karena tidak ditemukan. Exit: 0.");
                                                                                LogUtils.writeLog(threadContext, "SUCCESS", "Uninstall shell selesai. Exit: 0. Output: " + scriptResult.stdout.trim());

                                                                                finalMessageForLog = "Uninstall process finished successfully.";
                                                                                displayMessage = "Successfully.";
                                                                            } else {
                                                                                Log.w(TAG, "Shell execution failed unexpectedly. Exit: " + scriptResult.exitCode);
                                                                                LogUtils.writeLog(threadContext, "WARNING", "Shell execution failed unexpectedly. Exit: " + scriptResult.exitCode + ". Stderr: " + scriptResult.stderr.trim());

                                                                                finalMessageForLog = "Uninstall process completed with non-zero exit code (Exit: " + scriptResult.exitCode + ").";
                                                                                displayMessage = "Successfully.";
                                                                            }
                                                                    } catch (Throwable t) {

                                                                        Log.e(TAG, "Gagal mengeksekusi shell uninstall.sh", t);
                                                                        LogUtils.writeLog(threadContext, "ERROR", "Gagal mengeksekusi shell uninstall.sh: " + t.getMessage());

                                                                        finalMessageForLog = "Error executing uninstall script: " + t.getMessage();
                                                                        displayMessage = "Failed.";
                                                                    }

                                                                if (threadContext instanceof Activity) {
                                                                        final String msg = displayMessage;
                                                                        ((Activity) threadContext).runOnUiThread(new Runnable() {
                                                                                    @Override
                                                                                    public void run() {

                                                                                            if (loadingTextView != null) loadingTextView.setText(msg);
                                                                                            wrapper.postDelayed(new Runnable() {
                                                                                                        @Override
                                                                                                        public void run() {
                                                                                                                dismiss();
                                                                                                            }
                                                                                                    }, 3000);
                                                                                        }
                                                                                });
                                                                    }
                                                            }
                                                    }).start();
                                        }
                                });
                    }

                // Pengaturan layout Dialog
                wrapper = new FrameLayout(context);
                wrapper.setBackgroundColor(Color.TRANSPARENT);

                FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM);

                wrapper.addView(dialogContentView, bottomParams);
                wrapper.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    // Hanya dismiss jika loading overlay tidak ditampilkan
                                    if (customLoadingView == null || customLoadingView.getVisibility() != View.VISIBLE) {
                                            dismiss();
                                        }
                                }
                        });

                setContentView(wrapper); // Set content view dari Dialog

                Window window = getWindow();
                if (window != null) {
                        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

                        WindowManager.LayoutParams params = window.getAttributes();
                        params.gravity = Gravity.BOTTOM;
                        window.setAttributes(params);
                        window.getAttributes().windowAnimations = android.R.style.Animation_InputMethod;
                    }
            }
    }

