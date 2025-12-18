package com.WebSu.ig.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build; // Digunakan untuk pemeriksaan versi API

/**
 * Utilitas untuk mengkonversi objek Drawable menjadi Bitmap.
 */
public class DrawableUtils {

        // Ukuran default untuk Bitmap yang dibuat
        private static final int DEFAULT_SIZE = 96;

        /**
         * Mengkonversi Drawable menjadi Bitmap.
         * Secara aman menangani BitmapDrawable dan AdaptiveIconDrawable.
         * Jika Drawable adalah tipe lain, akan dibuat Bitmap baru dengan ukuran yang ditentukan
         * dan Drawable tersebut digambar di atasnya.
         *
         * @param drawable Objek Drawable yang akan dikonversi.
         * @return Bitmap hasil konversi.
         */
        public static Bitmap toBitmapSafely(Drawable drawable) {
                return toBitmapSafely(drawable, DEFAULT_SIZE);
            }

        /**
         * Mengkonversi Drawable menjadi Bitmap dengan ukuran spesifik.
         *
         * @param drawable Objek Drawable yang akan dikonversi.
         * @param size Ukuran (lebar dan tinggi) Bitmap yang diinginkan.
         * @return Bitmap hasil konversi.
         */
        public static Bitmap toBitmapSafely(Drawable drawable, int size) {
                if (drawable == null) {
                        return null;
                    }

                // 1. Tangani kasus BitmapDrawable: langsung ambil Bitmap-nya
                if (drawable instanceof BitmapDrawable) {
                        return ((BitmapDrawable) drawable).getBitmap();
                    }

                // 2. Tangani kasus AdaptiveIconDrawable (hanya tersedia di API 26 / Android 8.0 ke atas)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable) {
                        // AdaptiveIconDrawable perlu di-render ke Bitmap baru
                        // Membuat Bitmap baru
                        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);

                        // Mengatur batas dan menggambar
                        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        drawable.draw(canvas);

                        return bitmap;
                    }

                // 3. Fallback/Kasus umum: membuat Bitmap baru dan menggambar Drawable ke atasnya
                try {
                        // Membuat Bitmap baru
                        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);

                        // Mengatur batas dan menggambar
                        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                        drawable.draw(canvas);

                        return bitmap;
                    } catch (Exception e) {
                        // Log error jika diperlukan, misalnya: Log.e("DrawableUtils", "Gagal mengkonversi Drawable ke Bitmap", e);
                        e.printStackTrace();
                        return null;
                    }
            }
    }

