package com.WebSu.ig.utils;

import android.content.Context;
import android.os.Environment;
import java.io.File;

public final class FileLocator {

		/**
		 * Mendapatkan path absolut ke direktori Internal Storage private (files/) aplikasi Anda.
		 * Ini adalah lokasi yang paling aman dan akurat untuk data internal aplikasi.
		 * @param context Konteks aplikasi.
		 * @return Path file (Contoh: /data/data/com.paketanda/files)
		 */
		public static String getInternalAppFilesPath(Context context) {
				// File getFilesDir() tersedia di Java 1.7 dan Android 1.0+
				return context.getFilesDir().getAbsolutePath();
			}

		/**
		 * Mendapatkan path absolut ke direktori Cache private aplikasi Anda.
		 * Cocok untuk data sementara.
		 * @param context Konteks aplikasi.
		 * @return Path file (Contoh: /data/data/com.paketanda/cache)
		 */
		public static String getInternalAppCachePath(Context context) {
				return context.getCacheDir().getAbsolutePath();
			}

		/**
		 * Mendapatkan path ke External Storage (Storage Bersama) milik aplikasi Anda.
		 * Path ini biasanya dihapus ketika aplikasi di-uninstall.
		 * @param context Konteks aplikasi.
		 * @param type Sub-direktori spesifik (misalnya: Environment.DIRECTORY_DOWNLOADS)
		 * @return Path file (Contoh: /storage/emulated/0/Android/data/com.paketanda/files/Download)
		 */
		public static String getExternalAppFilesPath(Context context, String type) {
				// Metode ini tersedia sejak API 8 (Android 2.2 Froyo)
				File externalFile = context.getExternalFilesDir(type);
				if (externalFile != null) {
						return externalFile.getAbsolutePath();
					}
				return null; // Return null jika External Storage tidak tersedia/dimount
			}

		/**
		 * Mendapatkan path ke root External Storage (Public Shared Storage).
		 * PENTING: Penggunaan langsung path ini TIDAK DISARANKAN di Android modern
		 * dan memerlukan izin READ_EXTERNAL_STORAGE.
		 * @return Path root External Storage (Contoh: /storage/emulated/0)
		 */
		@Deprecated // Hindari ini untuk aplikasi modern
		public static String getExternalStorageRootPath() {
				// Metode ini tersedia sejak API 1 (Android 1.0)
				return Environment.getExternalStorageDirectory().getAbsolutePath();
			}
	}

