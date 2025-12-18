package com.WebSu.ig.viewmodel;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.util.Log;

import com.WebSu.ig.Dirname.PathHelper;
import com.WebSu.ig.Dirname.PathDirName;
import com.WebSu.ig.shell.WebsuFileService;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public final class PathFile {

		private static final String TAG = "PathFile";

		private final Context application;
		private final WebsuFileService wsFS;

		public PathFile(Context context, WebsuFileService fileService) {
				this.application = context.getApplicationContext();
				this.wsFS = fileService;
			}

		public static String BUSYBOX(Context context) {
				ApplicationInfo applicationInfo = context.getApplicationInfo();
				return applicationInfo.nativeLibraryDir + "/libbusybox.so";
			}

		public static String RESETPROP(Context context) {
				ApplicationInfo applicationInfo = context.getApplicationInfo();
				return applicationInfo.nativeLibraryDir + "/libresetprop.so";
			}

		public static final String WEBSUVBIN =
		PathHelper.getShellPath(PathDirName.folder.PARENT_BINARY).getAbsolutePath();

		public static final String WEBSUPLUGIN =
		PathHelper.getShellPath(PathDirName.folder.PARENT_PLUGIN).getAbsolutePath();

		// Callback interface
		public interface OutputHandler {
				void handle(String text);
			}

		// Fungsi hanya untuk menyalin ZIP tanpa mengeksekusi script
		public FlashResult flashPlugin(
            Uri uri,
            OutputHandler onStdout,
            OutputHandler onStderr
		) {
				ContentResolver resolver = application.getContentResolver();
				InputStream inputStream = null;
				OutputStream fos = null;

				try {
						inputStream = resolver.openInputStream(uri);

						File file = new File(
							PathHelper.getShellPath(PathDirName.folder.PARENT_ZIP),
							"module.zip"
						);
						
						Log.d(TAG, "Flash target path: " + file.getAbsolutePath());

						fos = wsFS.getStreamSession(file.getAbsolutePath(), true, false).getOutputStream();

						byte[] buffer = new byte[8 * 1024];
						int bytesRead;
						long totalBytes = 0;

						while ((bytesRead = inputStream.read(buffer)) != -1) {
								fos.write(buffer, 0, bytesRead);
								totalBytes += bytesRead;
							}

						fos.flush();

						Log.i(TAG, "Copied module.zip (" + totalBytes + " bytes) from " + uri);

						if (onStdout != null) {
								onStdout.handle("File copied to: " + file.getAbsolutePath());
							}

						return new FlashResult(new ResultExec(0, "Copy success", ""));

					} catch (Exception e) {
						Log.e(TAG, "flashPlugin copy error: " + e.getMessage(), e);

						if (onStderr != null) {
								onStderr.handle("Error copying file: " + e.getMessage());
							}

						return new FlashResult(new ResultExec(-1, "", e.getMessage()));

					} finally {
						if (inputStream != null) {
								try {
										inputStream.close();
									} catch (IOException ignored) {}
							}
						if (fos != null) {
								try {
										fos.close();
									} catch (IOException ignored) {}
							}
					}
			}

		public static class FlashResult {

				private final int code;
				private final String err;
				private final boolean showReboot;

				public FlashResult(int code, String err, boolean showReboot) {
						this.code = code;
						this.err = err;
						this.showReboot = showReboot;
					}

				public FlashResult(ResultExec result, boolean showReboot) {
						this(result.getCode(), result.getErr(), showReboot);
					}

				public FlashResult(ResultExec result) {
						this(result, result.isSuccess());
					}

				public int getCode() {
						return code;
					}

				public String getErr() {
						return err;
					}

				public boolean isShowReboot() {
						return showReboot;
					}

				@Override
				public String toString() {
						return "FlashResult{" +
							"code=" + code +
							", err='" + err + '\'' +
							", showReboot=" + showReboot +
							'}';
					}
			}

		public static class ResultExec {

				@SerializedName("errno")
				private final int code;

				@SerializedName("stdout")
				private final String out;

				@SerializedName("stderr")
				private final String err;

				public ResultExec(int code, String out, String err) {
						this.code = code;
						this.out = out != null ? out : "";
						this.err = err != null ? err : "";
					}

				public int getCode() {
						return code;
					}

				public String getOut() {
						return out;
					}

				public String getErr() {
						return err;
					}

				public boolean isSuccess() {
						return code == 0;
					}

				@Override
				public String toString() {
						return "ResultExec{" +
							"code=" + code +
							", out='" + out + '\'' +
							", err='" + err + '\'' +
							'}';
					}
			}
	}
