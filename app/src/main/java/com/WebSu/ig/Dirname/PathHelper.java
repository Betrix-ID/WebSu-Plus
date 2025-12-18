package com.WebSu.ig.Dirname;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;

import androidx.annotation.Nullable;

import java.io.File;

public class PathHelper {
		public static File getPath(String folderName) {
				return new File(Environment.getExternalStorageDirectory(), folderName);
			}

		public static File getShellPath(String folderName) {
				return new File(PathDirName.folder.SHELL_ROOT, folderName);
			}
			
		public static String getRelativePath(String rootPath, String fullPath) {
				return new File(rootPath).toURI().relativize(new File(fullPath).toURI()).getPath();
			}
			
		@Nullable
		public static String getRealPathFromUri(Context context, Uri uri) {
				String path = null;
				Cursor cursor = null;
				try {
						String[] proj = { android.provider.MediaStore.Files.FileColumns.DATA };
						cursor = context.getContentResolver().query(uri, proj, null, null, null);
						if (cursor != null && cursor.moveToFirst()) {
								int columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATA);
								path = cursor.getString(columnIndex);
							}
						// Kalau path null (misalnya dari File Picker modern), fallback salin ke cache
						if (path == null) {
								String fileName = getFileNameFromUri(context, uri);
								if (fileName == null) fileName = "tempfile";
								File cacheFile = new File(context.getCacheDir(), fileName);
								java.io.InputStream in = context.getContentResolver().openInputStream(uri);
								java.io.FileOutputStream out = new java.io.FileOutputStream(cacheFile);
								byte[] buffer = new byte[4096];
								int len;
								while ((len = in.read(buffer)) > 0) {
										out.write(buffer, 0, len);
									}
								in.close();
								out.close();
								path = cacheFile.getAbsolutePath();
							}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (cursor != null) cursor.close();
					}
				return path;
			}

		@Nullable
		public static String getFileNameFromUri(Context context, Uri uri) {
				Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
				if (cursor != null) {
						try {
								int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
								if (cursor.moveToFirst()) {
										return cursor.getString(nameIndex);
									}
							} finally {
								cursor.close();
							}
					}
				return null;
			}
	}

