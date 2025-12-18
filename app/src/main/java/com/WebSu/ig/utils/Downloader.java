package com.WebSu.ig.utils;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat; 
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Downloader {

        private static final String TAG = "WebSuDownloader";
        private static final String GITHUB_RELEASE_URL = "https://github.com/Betrix-ID";

        public static class LatestVersionInfo {
                public final int versionCode;
                public final String downloadUrl;
                public final String changelog;

                public LatestVersionInfo() {
                        this.versionCode = 0;
                        this.downloadUrl = "";
                        this.changelog = "";
                    }

                public LatestVersionInfo(int versionCode, String downloadUrl, String changelog) {
                        this.versionCode = versionCode;
                        this.downloadUrl = downloadUrl;
                        this.changelog = changelog;
                    }
            }

        public interface OnDownloadCompleteListener {
                void onDownloadComplete(Uri localUri);
                void onDownloadInProgress();
                void onDownloadFailed(long downloadId, String reason);
            }

        @SuppressLint("Range")
        public static long startDownload(
            Context context,
            String url,
            String fileName,
            String mimeType,
            String description) {

                final DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                if (downloadManager == null) {
                        Log.e(TAG, "DownloadManager service not available.");
                        return -1;
                    }

                DownloadManager.Query query = new DownloadManager.Query();
                try (Cursor cursor = downloadManager.query(query)) {
                        if (cursor != null) {
                                while (cursor.moveToNext()) {
                                        String uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI));
                                        String columnTitle = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE));
                                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                                        long id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID));

                                        if (url.equals(uri) || fileName.equals(columnTitle)) {
                                                if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_PAUSED) {
                                                        Log.d(TAG, "Download sedang berjalan/tertunda: " + fileName);
                                                        Toast.makeText(context, "Pengunduhan sedang berjalan...", Toast.LENGTH_SHORT).show();
                                                        return 0; 
                                                    } else {
                                                        Log.d(TAG, "Menghapus record lama database (ID: " + id + ") untuk download ulang.");
                                                        downloadManager.remove(id);
                                                    }
                                            }
                                    }
                            }
                    }

                try {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setMimeType(mimeType)
                            .setTitle(fileName)
                            .setDescription(description);

                        long downloadId = downloadManager.enqueue(request);
                        Log.i(TAG, "Download Baru Dimulai: " + fileName + ", ID: " + downloadId);
                        Toast.makeText(context, "Pengunduhan dimulai...", Toast.LENGTH_SHORT).show();
                        return downloadId;
                    } catch (Exception e) {
                        Log.e(TAG, "Gagal memulai download: " + e.getMessage());
                        Toast.makeText(context, "Gagal memulai unduhan.", Toast.LENGTH_LONG).show();
                        return -1;
                    }
            }

        public static LatestVersionInfo checkNewVersion() {
                String urlString = GITHUB_RELEASE_URL;
                LatestVersionInfo defaultValue = new LatestVersionInfo();
                HttpURLConnection connection = null;
                BufferedReader reader = null;

                try {
                        URL url = new URL(urlString);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.connect();

                        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return defaultValue;

                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) response.append(line);

                        JSONObject json = new JSONObject(response.toString());
                        String changelog = json.optString("body");

                        JSONArray assets = json.getJSONArray("assets");
                        for (int i = 0; i < assets.length(); i++) {
                                JSONObject asset = assets.getJSONObject(i);
                                String name = asset.getString("name");

                                if (!name.endsWith(".apk")) continue;

                                Pattern pattern = Pattern.compile("v(.+?)_(\\d+)-");
                                Matcher matcher = pattern.matcher(name);

                                if (matcher.find() && matcher.groupCount() >= 2) {
                                        int versionCode = Integer.parseInt(matcher.group(2)); 
                                        String downloadUrl = asset.getString("browser_download_url");
                                        return new LatestVersionInfo(versionCode, downloadUrl, changelog);
                                    }
                            }
                    } catch (Exception e) {
                        Log.e(TAG, "Error check version", e);
                    } finally {
                        if (reader != null) try { reader.close(); } catch (IOException ignored) {}
                        if (connection != null) connection.disconnect();
                    }
                return defaultValue;
            }

        public static BroadcastReceiver registerDownloadListener(
            final Context context,
            final long targetDownloadId,
            final OnDownloadCompleteListener listener) {

                BroadcastReceiver receiver = new BroadcastReceiver() {
                        @SuppressLint("Range")
                        @Override
                        public void onReceive(Context context, Intent intent) {
                                if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;

                                long receivedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                                if (targetDownloadId != -1 && receivedId != targetDownloadId) return;

                                DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                                DownloadManager.Query query = new DownloadManager.Query().setFilterById(receivedId);
                                try (Cursor cursor = dm.query(query)) {
                                        if (cursor != null && cursor.moveToFirst()) {
                                                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                                        String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                                                        listener.onDownloadComplete(Uri.parse(uriString));
                                                    } else if (status == DownloadManager.STATUS_FAILED) {
                                                        int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                                                        listener.onDownloadFailed(receivedId, "Reason: " + reason);
                                                    }
                                            }
                                    }
                            }
                    };

                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    ContextCompat.RECEIVER_EXPORTED
                );

                return receiver;
            }

        private static String getReasonString(int reasonCode) {
                switch (reasonCode) {
                        case DownloadManager.ERROR_CANNOT_RESUME: return "Cannot Resume";
                        case DownloadManager.ERROR_DEVICE_NOT_FOUND: return "Device Not Found";
                        case DownloadManager.ERROR_FILE_ALREADY_EXISTS: return "File Already Exists";
                        case DownloadManager.ERROR_FILE_ERROR: return "File Error";
                        case DownloadManager.ERROR_HTTP_DATA_ERROR: return "HTTP Data Error";
                        case DownloadManager.ERROR_INSUFFICIENT_SPACE: return "Insufficient Space";
                        case DownloadManager.ERROR_TOO_MANY_REDIRECTS: return "Too Many Redirects";
                        case DownloadManager.ERROR_UNHANDLED_HTTP_CODE: return "Unhandled HTTP Code";
                        default: return "Unknown Error";
                    }
            }

        public static File getDownloadedFile(String fileName) {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File targetFile = new File(dir, fileName);
                return (targetFile.exists() && targetFile.length() > 0) ? targetFile : null;
            }
    }

