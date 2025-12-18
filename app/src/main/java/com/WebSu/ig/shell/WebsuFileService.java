package com.WebSu.ig.shell;

import android.content.Context;
import android.util.Log;

import com.WebSu.ig.ShellExecutor;

import java.io.*;
public class WebsuFileService {

		private static final String TAG = "WebsuFileService";
		private final Context context;

		public WebsuFileService(Context context) {
				this.context = context.getApplicationContext();
			}

		/**
		 * Membuka stream ke path target.
		 * @param path   path tujuan file
		 * @param create buat parent dan file jika belum ada
		 * @param append append mode
		 */
		public StreamSession getStreamSession(String path, boolean create, boolean append) throws IOException {
				File file = new File(path);

				// Buat direktori/file jika belum ada
				if (create) {
						File parent = file.getParentFile();
						if (parent != null && !parent.exists()) {
								parent.mkdirs();
							}
						if (!file.exists()) {
								try {
										file.createNewFile();
									} catch (IOException e) {
										Log.w(TAG, "Tidak bisa buat file langsung: " + e.getMessage());
									}
							}
					}

				// Jalankan mode cat lewat ShellExecutor (root/shizuku/normal)
				final boolean useAppend = append;
				String redirect = useAppend ? ">>" : ">";
				final String cmd = "cat " + redirect + " '" + path.replace("'", "'\"'\"'") + "'";
				Log.d(TAG, "Menulis lewat shell: " + cmd);

				try {
						final Process process = ShellExecutor.execProcess(cmd, context);
						final OutputStream shellStdin = new BufferedOutputStream(process.getOutputStream());

						return new StreamSession(shellStdin, process);
					} catch (Throwable t) {
						Log.e(TAG, "Gagal lewat shell, fallback ke FileOutputStream: " + t.getMessage());
						return new StreamSession(new FileOutputStream(file, append), null);
					}
			}

		// ------------------------------------------------------------

		public static class StreamSession {
				private final OutputStream outputStream;
				private final Process process;

				public StreamSession(OutputStream outputStream, Process process) {
						this.outputStream = outputStream;
						this.process = process;
					}

				public OutputStream getOutputStream() {
						return outputStream;
					}

				public void close() {
						try {
								outputStream.flush();
							} catch (IOException ignored) {}

						try {
								outputStream.close();
							} catch (IOException ignored) {}

						if (process != null) {
								try {
										process.waitFor();
									} catch (InterruptedException ignored) {}
								try {
										process.destroy();
									} catch (Throwable ignored) {}
							}
					}
			}
	}
