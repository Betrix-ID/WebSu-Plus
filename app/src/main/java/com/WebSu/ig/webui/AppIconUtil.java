package com.WebSu.ig.webui;

import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.webkit.*;
import android.util.Log;
import android.util.LruCache; 
import com.WebSu.ig.webui.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap; // Import untuk thread-safe map

public class AppIconUtil {

        // 1. Mengganti WeakReference dengan LruCache
        // Menggunakan 1/8 dari total memori aplikasi untuk cache ikon (perkiraan)
        private static final int CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 1024 / 8); 
        private static final LruCache<String, Bitmap> iconCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                        // Ukuran dihitung dalam kilobytes
                        return bitmap.getByteCount() / 1024;
                    }
            };

        // 2. Menggunakan ConcurrentHashMap untuk Thread-Safety
        private static final ConcurrentHashMap<String, Result> resultListeners = new ConcurrentHashMap<>();

        // Tag untuk logging
        private static final String TAG = "AppIconUtil";

        /**
         * Memuat ikon aplikasi secara sinkron.
         * @return Bitmap ikon atau null jika gagal.
         */
        public static Bitmap loadAppIconSync(String packageName, int sizePx) {
                // Mengambil dari LruCache
                Bitmap cached = iconCache.get(packageName);
                if (cached != null) return cached;

                try {
                        PackageManager pm = Engine.getApplication().getPackageManager();
                        ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                        Drawable drawable = pm.getApplicationIcon(appInfo);

                        Bitmap raw = drawableToBitmap(drawable, sizePx);
                        // Cek jika DrawableToBitmap mengembalikan null (walaupun jarang)
                        if (raw == null) {
                                Log.e(TAG, "DrawableToBitmap returned null for: " + packageName);
                                return null;
                            }

                        // Scaling dilakukan hanya sekali dan hasilnya ditaruh di cache
                        Bitmap icon = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true); 

                        if (icon != null) {
                                iconCache.put(packageName, icon); // Masukkan ke LruCache
                            }
                        return icon;
                    } catch (PackageManager.NameNotFoundException e) {
                        // 3. Penanganan error spesifik: Aplikasi tidak ditemukan
                        Log.w(TAG, "Package not found or removed: " + packageName);
                        return null;
                    } catch (Exception e) {
                        // Penanganan error umum
                        Log.e(TAG, "Error loading icon sync for " + packageName, e);
                        return null;
                    }
            }

        public static void loadAppIcon(final String packageName, final int sizePx, final Result result) {

                Bitmap cached = iconCache.get(packageName); // Mengambil dari LruCache
                if (cached != null) {
                        result.onIconReady(cached);
                        return;
                    }

                // Hindari pemuatan ganda: Hanya satu pemintaan yang aktif untuk satu packageName
                if (resultListeners.putIfAbsent(packageName, result) != null) {
                        return; // Sudah ada permintaan yang berjalan
                    }

                new Thread(new Runnable() {
                            @Override
                            public void run() {
                                    Bitmap icon = null;
                                    try {
                                            PackageManager pm = Engine.getApplication().getPackageManager();
                                            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                                            Drawable drawable = pm.getApplicationIcon(appInfo);
                                            Bitmap raw = drawableToBitmap(drawable, sizePx);

                                            if (raw != null) {
                                                    icon = Bitmap.createScaledBitmap(raw, sizePx, sizePx, true);
                                                    if (icon != null) {
                                                            iconCache.put(packageName, icon); // Masukkan ke LruCache
                                                        }
                                                }
                                        } catch (PackageManager.NameNotFoundException e) {
                                            Log.w(TAG, "Package not found in async: " + packageName);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error loading icon async for " + packageName, e);
                                        }

                                    // Dapatkan listener yang seharusnya. Ini aman karena ConcurrentHashMap
                                    final Result resultListener = resultListeners.remove(packageName); 

                                    // Pastikan callback dilakukan di Main Thread
                                    if (resultListener != null) {
                                            final Bitmap finalIcon = icon;
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                                resultListener.onIconReady(finalIcon);
                                                            }
                                                    });
                                        }
                                }
                        }).start();
            }

        private static Bitmap drawableToBitmap(Drawable drawable, int size) {
                if (drawable instanceof BitmapDrawable) return ((BitmapDrawable) drawable).getBitmap();

                // Gunakan AdaptiveIconDrawable jika tersedia di API 26+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
                        // Untuk AdaptiveIconDrawable, kita harus merendernya ke Bitmap
                        int w = size;
                        int h = size;
                        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bmp);
                        drawable.setBounds(0, 0, w, h);
                        drawable.draw(canvas);
                        return bmp;
                    }

                // Logika fallback untuk Drawable biasa
                int width = drawable.getIntrinsicWidth();
                int height = drawable.getIntrinsicHeight();

                // Jika intrinsic size tidak valid (<= 0), gunakan ukuran yang diminta (size)
                width = width > 0 ? width : size;
                height = height > 0 ? height : size;

                try {
                        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bmp);
                        drawable.setBounds(0, 0, width, height);
                        drawable.draw(canvas);
                        return bmp;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to create Bitmap from Drawable", e);
                        return null;
                    }
            }

        public interface Result {
                void onIconReady(Bitmap icon);
            }

        public static WebResourceResponse bitmapToWebResponse(Bitmap bitmap) {
                try {
                        if (bitmap == null) return null;
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        // Menggunakan COMPRESS_QUALITY yang lebih baik untuk PNG
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos); 
                        InputStream is = new ByteArrayInputStream(bos.toByteArray());
                        return new WebResourceResponse("image/png", null, is);
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting Bitmap to WebResourceResponse", e);
                        return null;
                    }
            }
    }

