package com.WebSu.ig.utils;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {

		private static final int BUFFER_SIZE = 4096;

		public static void copyUriToTempFile(Context context, Uri uri, String destinationPath) throws Exception {
				File destinationFile = new File(destinationPath);

				// Pastikan folder tujuan ada
				File parentDir = destinationFile.getParentFile();
				if (parentDir != null && !parentDir.exists()) {
						if (!parentDir.mkdirs()) {
								throw new Exception("Gagal membuat folder: " + parentDir.getAbsolutePath());
							}
					}

				InputStream inputStream = null;
				OutputStream outputStream = null;

				try {
						inputStream = context.getContentResolver().openInputStream(uri);
						if (inputStream == null) {
								throw new Exception("Gagal mendapatkan InputStream dari URI: " + uri.toString());
							}

						outputStream = new FileOutputStream(destinationFile);

						byte[] buffer = new byte[BUFFER_SIZE];
						int bytesRead;
						while ((bytesRead = inputStream.read(buffer)) != -1) {
								outputStream.write(buffer, 0, bytesRead);
							}
						outputStream.flush();

						// Validasi hasil
						if (destinationFile.length() == 0) {
								throw new Exception("File kosong setelah disalin dari: " + uri.toString());
							}

					} finally {
						if (inputStream != null) {
								try { inputStream.close(); } catch (Exception ignored) {}
							}
						if (outputStream != null) {
								try { outputStream.close(); } catch (Exception ignored) {}
							}
					}
			}
	}
