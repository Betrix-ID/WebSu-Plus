package com.WebSu.ig;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;

import rikka.shizuku.Shizuku;

public class ShizukuHelper {

    public interface PermissionCallback {
        void onGranted();
        void onDenied();
        void onUnsupported();
        void onNotRunning();
    }

    private static final int REQUEST_CODE = 1001;
    private static Shizuku.OnRequestPermissionResultListener listener;

    public static boolean isGranted() {
        try {
            return Shizuku.pingBinder() &&
                   Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void requestIfNeeded(@NonNull final Context context,
                                       @NonNull final PermissionCallback callback) {
        if (!Shizuku.pingBinder()) {
            Log.w("ShizukuHelper", "Shizuku tidak berjalan");
            callback.onNotRunning();
            return;
        }

        if (Shizuku.isPreV11()) {
            Log.w("ShizukuHelper", "Versi Shizuku tidak didukung");
            callback.onUnsupported();
            return;
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            callback.onGranted();
            return;
        }

        if (Shizuku.shouldShowRequestPermissionRationale()) {
            callback.onDenied();
            return;
        }

        registerListener(callback);
        try {
            Shizuku.requestPermission(REQUEST_CODE);
        } catch (Throwable t) {
            Log.e("ShizukuHelper", "Gagal request permission", t);
            callback.onDenied();
        }
    }

    private static void registerListener(final PermissionCallback callback) {
        unregisterListener(); // Hindari duplikasi listener

        listener = new Shizuku.OnRequestPermissionResultListener() {
            @Override
            public void onRequestPermissionResult(int requestCode, int grantResult) {
                if (requestCode == REQUEST_CODE) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        callback.onGranted();
                    } else {
                        callback.onDenied();
                    }
                }
            }
        };

        Shizuku.addRequestPermissionResultListener(listener);
    }

    public static void unregisterListener() {
        if (listener != null) {
            Shizuku.removeRequestPermissionResultListener(listener);
            listener = null;
        }
    }
}