package com.WebSu.ig.utils;

/**
 * Kelas utilitas Singleton untuk menampung status penting UI secara global,
 * mirip dengan konsep CompositionLocal atau global context di Java 7.
 *
 * Catatan: Ini adalah pola dasar Singleton untuk memberikan akses global.
 * Penggunaan CompositionLocal yang sebenarnya memerlukan Jetpack Compose.
 */
public class LocalSnackbarHost {

        // 1. Placeholder untuk status yang ingin disimpan (mirip SnackbarHostState)
        // Ganti UISnackbarState dengan kelas status Anda yang sebenarnya jika ada.
        public interface UISnackbarState {
                void showMessage(String message);
                // Tambahkan metode atau properti lain yang diperlukan
            }

        // 2. Instance tunggal dari kelas ini (Singleton)
        private static LocalSnackbarHost instance;

        // 3. Objek status yang akan diakses secara global
        private UISnackbarState snackbarState;

        /**
         * Konstruktor pribadi untuk mencegah instansiasi dari luar.
         */
        private LocalSnackbarHost() {
                // Inisialisasi awal (jika diperlukan)
            }

        /**
         * Metode untuk mendapatkan instance tunggal dari kelas ini.
         * @return Instance tunggal SnackbarStateHolder.
         */
        public static LocalSnackbarHost getInstance() {
                if (instance == null) {
                        // Menggunakan sinkronisasi untuk memastikan thread-safe saat inisialisasi pertama (lazy initialization)
                        synchronized (LocalSnackbarHost.class) {
                                if (instance == null) {
                                        instance = new LocalSnackbarHost();
                                    }
                            }
                    }
                return instance;
            }

        /**
         * Mengatur objek status Snackbar yang sebenarnya.
         * Ini harus dipanggil di awal aplikasi atau framework UI Anda.
         * @param state Objek status UISnackbarState yang akan digunakan secara global.
         */
        public void setSnackbarState(UISnackbarState state) {
                if (state == null) {
                        throw new IllegalArgumentException("Snackbar state cannot be null.");
                    }
                this.snackbarState = state;
            }

        /**
         * Mendapatkan objek status Snackbar.
         * Jika belum diatur, akan melempar RuntimeException (mirip dengan 'error' di Compose).
         * @return Objek UISnackbarState yang sudah diatur.
         */
        public UISnackbarState getSnackbarState() {
                if (snackbarState == null) {
                        // Ini meniru perilaku Compose yang melempar error jika local tidak tersedia.
                        throw new IllegalStateException("SnackbarState not initialized. " +
                                                        "Call setSnackbarState(state) before accessing it.");
                    }
                return snackbarState;
            }

        // Contoh penggunaan metode untuk menampilkan pesan
        public void showMessage(String message) {
                getSnackbarState().showMessage(message);
            }
    }

