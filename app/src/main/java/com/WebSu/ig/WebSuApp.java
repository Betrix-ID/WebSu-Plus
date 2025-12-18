package com.WebSu.ig;

import android.app.Application;
import android.util.Log;
// OKHTTP3 TIDAK DIGUNAKAN DALAM IMPLEMENTASI BARU INI UNTUK MENYEDERHANAKAN DEPENDENSI
// Tapi kita tetap menyertakan method ini jika suatu saat ingin menggunakan OkHttp
/*
 import okhttp3.OkHttpClient;
 import java.util.concurrent.TimeUnit;
 */

public class WebSuApp extends Application {

        private static final String TAG = "WebSuApp";

        // Gunakan HttpURLConnection standar, jadi kita tidak perlu menginisialisasi OkHttp
        /*
         private static OkHttpClient okHttpClient;
         */

        @Override
        public void onCreate() {
                super.onCreate();
                // Inisialisasi sumber daya global di sini
                Log.d(TAG, "WebSuApp initialized.");
                // initOkHttpClient();
            }

        // Ganti implementasi ini, karena OkHttp tidak digunakan lagi di FetchChangelogTask
        /*
         private static void initOkHttpClient() {
         okHttpClient = new OkHttpClient.Builder()
         .connectTimeout(15, TimeUnit.SECONDS)
         .readTimeout(15, TimeUnit.SECONDS)
         .build();
         }

         // Method ini sekarang akan selalu mengembalikan NULL atau harus dihapus 
         // jika kita tidak menggunakan OkHttp. Kita biarkan saja untuk kompatibilitas 
         // jika Anda ingin beralih kembali ke OkHttp.
         public static OkHttpClient getOkHttpClient() {
         return okHttpClient;
         }
         */

        // Karena kita tidak menggunakan OkHttp, kita akan menyediakan method dummy
        // atau Anda harus mengubah kode UpdatePlugin.java untuk menggunakan HttpURLConnection.
        // KITA AKAN MENGUBAH UPDATEPLUGIN.JAVA SEPENUHNYA UNTUK MENGHILANGKAN okhttp3.
    }

