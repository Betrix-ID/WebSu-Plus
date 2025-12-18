package com.WebSu.ig.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utilitas untuk mengelola SharedPreferences, menggunakan tipe Enum sebagai
 * kunci penyimpanan Boolean, dan dilengkapi dengan caching in-memory.
 *
 * Catatan: Karena Java 1.7 tidak mendukung tipe generik Enum secara langsung
 * dalam konteks ini seperti Kotlin, parameter 'Type' diganti dengan
 * tipe dasar Enum<?> di beberapa tempat. Namun, untuk implementasi di Android,
 * kita harus menggunakan objek Class<T> pada constructor untuk referensi.
 *
 * Karena batasan generik di Java 1.7/Android, kelas ini disederhanakan
 * untuk menerima Enum dan mengelola kunci string-nya.
 */
public class PrefsEnumHelper {

        private final String keyPrefix;
        private final String prefsName;
        private final Map<String, Boolean> cache = new HashMap<String, Boolean>();

        private static final String DEFAULT_PREFS_NAME = "settings";

        /**
         * Constructor untuk PrefsEnumHelper.
         *
         * @param keyPrefix Prefix yang akan ditambahkan ke nama Enum untuk membentuk kunci SharedPreferences.
         * @param prefsName Nama file SharedPreferences.
         */
        public PrefsEnumHelper(String keyPrefix, String prefsName) {
                this.keyPrefix = keyPrefix;
                this.prefsName = prefsName;
            }

        /**
         * Constructor dengan nama SharedPreferences default ("settings").
         *
         * @param keyPrefix Prefix yang akan ditambahkan ke nama Enum untuk membentuk kunci SharedPreferences.
         */
        public PrefsEnumHelper(String keyPrefix) {
                this(keyPrefix, DEFAULT_PREFS_NAME);
            }

        private SharedPreferences getPrefs(Context context) {
                // Context.MODE_PRIVATE adalah nilai integer 0
                return context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
            }

        /**
         * Menyimpan nilai Boolean ke SharedPreferences dan cache.
         *
         * @param context Context aplikasi.
         * @param type Konstanta Enum yang merepresentasikan pengaturan.
         * @param value Nilai boolean yang akan disimpan.
         */
        public void saveState(Context context, Enum<?> type, boolean value) {
                String key = keyPrefix + type.name();
                cache.put(key, value);

                Editor editor = getPrefs(context).edit();
                editor.putBoolean(key, value);
                editor.apply(); // apply() bekerja di background, lebih disukai daripada commit()
            }

        /**
         * Memuat nilai Boolean dari cache atau SharedPreferences.
         *
         * @param context Context aplikasi.
         * @param type Konstanta Enum yang merepresentasikan pengaturan.
         * @param defaultVal Nilai default jika kunci tidak ditemukan.
         * @return Nilai boolean yang dimuat.
         */
        public boolean loadState(Context context, Enum<?> type, boolean defaultVal) {
                String key = keyPrefix + type.name();

                // 1. Cek cache
                if (cache.containsKey(key)) {
                        // Karena kita menyimpan Boolean ke cache, kita aman untuk menggunakannya.
                        Boolean cachedValue = cache.get(key);
                        if (cachedValue != null) {
                                return cachedValue;
                            }
                    }

                // 2. Ambil dari SharedPreferences, lalu simpan ke cache
                boolean value = getPrefs(context).getBoolean(key, defaultVal);
                cache.put(key, value);
                return value;
            }

        /**
         * Memuat nilai Boolean dengan nilai default 'false'.
         *
         * @param context Context aplikasi.
         * @param type Konstanta Enum yang merepresentasikan pengaturan.
         * @return Nilai boolean yang dimuat.
         */
        public boolean loadState(Context context, Enum<?> type) {
                return loadState(context, type, false);
            }

        /**
         * Menghapus semua data dari cache dan SharedPreferences yang terkait.
         *
         * @param context Context aplikasi.
         */
        public void clearAll(Context context) {
                cache.clear();
                Editor editor = getPrefs(context).edit();
                editor.clear();
                editor.apply();
            }

        /**
         * Mengambil semua pasangan kunci-nilai dari SharedPreferences
         * dan memperbarui cache untuk semua nilai Boolean.
         *
         * @param context Context aplikasi.
         * @return Map yang berisi semua data SharedPreferences.
         */
        public Map<String, ?> getAll(Context context) {
                Map<String, ?> all = getPrefs(context).getAll();

                // Memperbarui cache hanya untuk nilai Boolean yang ada
                Set<Map.Entry<String, ?>> entries = all.entrySet();
                for (Map.Entry<String, ?> entry : entries) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        // Hanya cache tipe Boolean (sesuai fungsi asli)
                        if (value instanceof Boolean) {
                                cache.put(key, (Boolean) value);
                            }
                    }
                return all;
            }
    }

