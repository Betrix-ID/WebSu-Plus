package com.WebSu.ig.webui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.webkit.internal.AssetHelper;

import com.WebSu.ig.ShellExecutor;
import com.WebSu.ig.LogUtils; 
import com.WebSu.ig.Dirname.PathDirName;
import com.WebSu.ig.Dirname.WebSuLoader;

import java.io.*;

// Import MimeUtil yang baru disediakan
import com.WebSu.ig.webui.MimeUtil; 

public class wsPathHandler implements WebSuLoader.wsPathHandler {

        private static final String TAG = "wsPathHandler";

        private static final String[] FORBIDDEN_DATA_DIRS = new String[] {"/data/data", "/data/system"};
        private static final String ALLOWED_DATA_DIRS = PathDirName.folder.SHELL_ROOT;

        // --- PENAMBAHAN WHITELIST ---
        private static final String AXMANAGER_PLUGINS_ROOT = "/data/data/com.android.shell/AxManager/plugins/";
        private static final String ADB_MODULES_ROOT = "/data/adb/modules/"; 
        // --- AKHIR PENAMBAHAN ---

        @NonNull
        private final File mDirectory;

        @SuppressLint("RestrictedApi")
        public wsPathHandler(@NonNull Context context, @NonNull File directory) {
                try {
                        mDirectory = new File(AssetHelper.getCanonicalDirPath(directory));

                        if (!isAllowedInternalStorageDir()) {
                                String errorMsg = "The given directory \"" + directory
                                    + "\" doesn't exist under an allowed app internal storage directory";
                                LogUtils.writeLog(context, "PATH_ERROR", "Constructor check failed: " + errorMsg);
                                throw new IllegalArgumentException(errorMsg);
                            }
                    } catch (IOException e) {
                        String errorMsg = "Failed to resolve the canonical path for the given directory: " + directory.getPath();
                        LogUtils.writeLog(context, "PATH_ERROR", errorMsg + ": " + e.getMessage());
                        throw new IllegalArgumentException(errorMsg, e);
                    }
            }

        private boolean isAllowedInternalStorageDir() throws IOException {
                @SuppressLint("RestrictedApi") String dir = AssetHelper.getCanonicalDirPath(mDirectory);

                // LOGIKA PENGECUALIAN AXMANAGER & ADB MODULES
                if (dir.startsWith(AXMANAGER_PLUGINS_ROOT) || dir.startsWith(ADB_MODULES_ROOT)) {
                        return true;
                    }

                // Logika pemeriksaan keamanan yang ada:
                for (String forbiddenPath : FORBIDDEN_DATA_DIRS) {
                        // Tolak jika dimulai dengan path terlarang, DAN BUKAN path internal yang diizinkan (SHELL_ROOT)
                        if (dir.startsWith(forbiddenPath) && !dir.startsWith(ALLOWED_DATA_DIRS)) {
                                return false;
                            }
                    }
                return true;
            }

        @SuppressLint("RestrictedApi")
        @Nullable
        @Override
        public WebResourceResponse handle(WebView view, WebResourceRequest request) {
                String path = request.getUrl().getPath();
                if (path == null) return new WebResourceResponse(null, null, null);

                try {
                        // 1. Bersihkan path dari trailing slash
                        if (path.length() > 1 && path.endsWith("/")) {
                                path = path.substring(0, path.length() - 1);
                            }

                        // 2. Resolve file
                        File file = AssetHelper.getCanonicalFileIfChild(mDirectory, path);

                        if (file != null) {
                                String finalPath = file.getCanonicalPath();

                                // 3. Dapatkan InputStream menggunakan ShellExecutor
                                InputStream is = ShellExecutor.getFileInputStream(finalPath);

                                if (is == null) {
                                        LogUtils.writeLog(view.getContext(), "HANDLER_FAIL", "File content unreadable or 0 byte: " + finalPath);
                                        return null;
                                    }

                                // 4. DETEKSI MIME TYPE LENGKAP MENGGUNAKAN MimeUtil BARU
                                String mimeType = getAutoMimeType(finalPath); 

                                // 5. TENTUKAN ENCODING
                                String encoding = null;
                                if (isTextBasedMimeType(mimeType)) {
                                        encoding = "UTF-8";
                                    }

                                // Logging detail
                                LogUtils.writeLog(view.getContext(), "RESOLVE", "File: " + finalPath + " | Size: " + file.length() + " bytes | Mime: " + mimeType + " | Enc: " + encoding);

                                return new WebResourceResponse(mimeType, encoding, is);

                            } else {
                                String msg = String.format("File %s is outside mounted dir %s", path, mDirectory.getCanonicalPath());
                                Log.e(TAG, msg);
                                LogUtils.writeLog(view.getContext(), "SECURITY_ALERT", msg);
                            }
                    } catch (IOException e) {
                        Log.e(TAG, "Error opening path: " + path, e);
                        LogUtils.writeLog(view.getContext(), "IO_ERROR", "Exception: " + e.getMessage());
                    }
                return new WebResourceResponse(null, null, null);
            }

        /**
         * Helper untuk mendeteksi MIME Type secara lengkap.
         * Menggunakan MimeUtil yang disediakan.
         */
        @SuppressLint("RestrictedApi")
        private String getAutoMimeType(String path) {
                // LANGSUNG PANGGIL IMPLEMENTASI MimeUtil
                String mimeType = MimeUtil.getMimeFromFileName(path); 

                // Fallback terakhir
                if (mimeType != null && !mimeType.isEmpty()) {
                        return mimeType;
                    }

                // Default untuk resource tak dikenal
                return "application/octet-stream";
            }

        private boolean isTextBasedMimeType(String mimeType) {
                if (mimeType == null) return false;

                // Logika penentuan teks-based
                return mimeType.startsWith("text/") ||
                    mimeType.equals("application/javascript") ||
                    mimeType.equals("application/json") ||
                    mimeType.equals("application/xml") ||
                    mimeType.contains("xml"); 
            }
    }

