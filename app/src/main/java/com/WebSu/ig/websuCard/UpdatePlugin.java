package com.WebSu.ig.websuCard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.WebSu.ig.utils.Downloader;
import com.WebSu.ig.utils.Downloader.OnDownloadCompleteListener;
import com.WebSu.ig.LogUtils; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdatePlugin {

        private static final String TAG = "UpdatePlugin";
        private final Context applicationContext;
        private final Handler mainHandler = new Handler(Looper.getMainLooper());

        private BroadcastReceiver downloadReceiver;

        public interface OnUpdateFlowListener {
                void onChangelogReady(String changelog, String latestVersion);
                void showToast(String message);
                void setLoading(boolean isLoading);
                void onInstallReady(Uri fileUri);
                void onError(String message);
            }

        private final OnUpdateFlowListener updateFlowListener;

        public UpdatePlugin(Context activityContext, OnUpdateFlowListener listener) {
                this.applicationContext = activityContext.getApplicationContext();
                this.updateFlowListener = listener;
            }

        public void startUpdateProcess(final String downloadUrl, final String latestVersion, 
                                       final String changelogUrl, final String moduleName) {

                if (changelogUrl == null || changelogUrl.isEmpty()) {
                        LogUtils.writeLog(applicationContext, "ERROR", "Changelog URL empty for " + moduleName);
                        updateFlowListener.onError("URL Changelog tidak ditemukan.");
                        return;
                    }

                LogUtils.writeLog(applicationContext, "INFO", "Starting update process for " + moduleName);
                updateFlowListener.setLoading(true);
                new FetchChangelogTask(changelogUrl, latestVersion, moduleName, updateFlowListener, applicationContext).execute();
            }

        // --- LOGIKA DOWNLOAD ---

        public void startDownloadConfirmed(final String downloadUrl, final String fileName) {
                if (downloadUrl == null || downloadUrl.isEmpty()) {
                        LogUtils.writeLog(applicationContext, "ERROR", "Download URL empty for " + fileName);
                        updateFlowListener.onError("URL unduhan tidak ditemukan.");
                        return;
                    }

                LogUtils.writeLog(applicationContext, "INFO", "User confirmed download: " + fileName);
                updateFlowListener.setLoading(true);
                updateFlowListener.showToast("Memulai unduhan: " + fileName);

                // 1. Bersihkan receiver lama
                unregisterListener();

                // 2. Mulai download
                LogUtils.writeLog(applicationContext, "INFO", "Enqueuing download via DownloadManager: " + fileName);
                final long downloadId = Downloader.startDownload(
                    applicationContext,
                    downloadUrl,
                    fileName,
                    "application/zip",
                    "Mengunduh Plugin: " + fileName
                );

                if (downloadId < 0) {
                        LogUtils.writeLog(applicationContext, "ERROR", "Failed to enqueue download: " + fileName);
                        updateFlowListener.onError("Gagal memulai unduhan.");
                        updateFlowListener.setLoading(false);
                        return;
                    } else if (downloadId == 0) {
                        LogUtils.writeLog(applicationContext, "INFO", "Download already in progress or completed for: " + fileName);
                        // Jika return 0 (sudah ada), Downloader biasanya sudah handle Toast/status.
                        return;
                    }

                // 3. Daftarkan Listener
                LogUtils.writeLog(applicationContext, "INFO", "Download enqueued. ID: " + downloadId + ". Waiting for completion...");
                downloadReceiver = Downloader.registerDownloadListener(
                    applicationContext,
                    downloadId,
                    new OnDownloadCompleteListener() {
                            @Override
                            public void onDownloadComplete(final Uri fileUri) {
                                    mainHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                        LogUtils.writeLog(applicationContext, "INFO", "Download completed. ID: " + downloadId + ", URI: " + fileUri.toString());
                                                        updateFlowListener.onInstallReady(fileUri);
                                                        updateFlowListener.setLoading(false);
                                                        unregisterListener();
                                                    }
                                            });
                                }

                            @Override
                            public void onDownloadInProgress() {
                                    // Optional: Log progress if available
                                }

                            @Override
                            public void onDownloadFailed(final long id, final String reason) {
                                    mainHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                        LogUtils.writeLog(applicationContext, "ERROR", "Download failed. ID: " + id + ", Reason: " + reason);
                                                        updateFlowListener.showToast("Unduhan gagal: " + reason);
                                                        updateFlowListener.setLoading(false);
                                                        unregisterListener();
                                                    }
                                            });
                                }
                        }
                );
            }

        public void unregisterListener() {
                if (downloadReceiver != null) {
                        try {
                                applicationContext.unregisterReceiver(downloadReceiver);
                                LogUtils.writeLog(applicationContext, "INFO", "Unregistered download receiver.");
                            } catch (Exception e) {
                                // Ignore, already unregistered
                            }
                        downloadReceiver = null;
                    }
            }

        private static class FetchChangelogTask extends AsyncTask<Void, Void, String> {
                private final String changelogUrl;
                private final String latestVersion;
                private final String moduleName;
                private final WeakReference<OnUpdateFlowListener> listenerRef;
                private final Context context; 
                private String errorMessage = null;

                public FetchChangelogTask(String changelogUrl, String latestVersion, String moduleName, OnUpdateFlowListener listener, Context ctx) {
                        this.changelogUrl = changelogUrl;
                        this.latestVersion = latestVersion;
                        this.moduleName = moduleName;
                        this.listenerRef = new WeakReference<OnUpdateFlowListener>(listener);
                        this.context = ctx.getApplicationContext();
                    }

                @Override
                protected String doInBackground(Void... voids) {
                        HttpURLConnection connection = null;
                        BufferedReader reader = null;
                        try {
                                URL url = new URL(changelogUrl);
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("GET");
                                connection.setConnectTimeout(10000);
                                connection.connect();

                                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                        StringBuilder sb = new StringBuilder();
                                        String line;
                                        while ((line = reader.readLine()) != null) {
                                                sb.append(line).append('\n');
                                            }
                                        return sb.toString();
                                    } else {
                                        errorMessage = "HTTP Error " + connection.getResponseCode();
                                        return null;
                                    }
                            } catch (IOException e) {
                                errorMessage = e.getMessage();
                                return null;
                            } finally {
                                if (reader != null) try { reader.close(); } catch (Exception ignored) {}
                                if (connection != null) connection.disconnect();
                            }
                    }

                @Override
                protected void onPostExecute(String result) {
                        OnUpdateFlowListener listener = listenerRef.get();
                        if (listener == null) return;

                        listener.setLoading(false);
                        if (result != null) {
                                LogUtils.writeLog(context, "INFO", "Changelog fetched successfully for " + moduleName);
                                listener.onChangelogReady(result, latestVersion);
                            } else {
                                LogUtils.writeLog(context, "ERROR", "Changelog fetch failed for " + moduleName + ": " + errorMessage);
                                listener.onError(errorMessage);
                            }
                    }
            }
    }

