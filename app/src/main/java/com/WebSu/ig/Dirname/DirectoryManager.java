package com.WebSu.ig.Dirname;

import android.content.Context;
import android.util.Log;
import java.io.File;
import com.WebSu.ig.ShellExecutor; 

public class DirectoryManager {

		private static final String TAG = "DirectoryManager";

		public static boolean createAllDirectories(Context context) {

					final String BASE_DIR_WITH_SEPARATOR = 
					PathDirName.folder.SHELL_ROOT + PathDirName.folder.PARENT + File.separator;

				// BASE_PATH: /data/user_de/0/com.android.shell/WebSu
				final String BASE_PATH = BASE_DIR_WITH_SEPARATOR.substring(0, BASE_DIR_WITH_SEPARATOR.length() - 1);

				StringBuilder pathsToCreate = new StringBuilder();

				pathsToCreate.append(BASE_PATH).append(" "); 
				
				pathsToCreate.append(BASE_DIR_WITH_SEPARATOR).append(PathDirName.folder.PLUGIN).append(" ");          // .../WebSu/plugins
				pathsToCreate.append(BASE_DIR_WITH_SEPARATOR).append(PathDirName.folder.PLUGIN_UPDATE).append(" ");   // .../WebSu/plugins_update
				pathsToCreate.append(BASE_DIR_WITH_SEPARATOR).append(PathDirName.folder.CACHE).append(" ");           // .../WebSu/cache
				pathsToCreate.append(BASE_DIR_WITH_SEPARATOR).append(PathDirName.folder.LOG).append(" ");             // .../WebSu/logs
				pathsToCreate.append(BASE_DIR_WITH_SEPARATOR).append(PathDirName.folder.BINARY).append(" ");          // .../WebSu/bin
				pathsToCreate.append(BASE_DIR_WITH_SEPARATOR).append(PathDirName.folder.ZIP).append(" ");             // .../WebSu/zip

			    pathsToCreate.append(BASE_DIR_WITH_SEPARATOR).append(PathDirName.folder.EXTERNAL_BINARY); 


				String command = "mkdir -p " + pathsToCreate.toString().trim();
				Log.i(TAG, "Executing mkdir command: " + command);
				ShellExecutor.Result result = ShellExecutor.execSync(command, context);

				if (result.exitCode == 0) {
						Log.d(TAG, "Semua direktori (termasuk sbin lokal) berhasil dibuat.");
						return true;
					} else {
						Log.e(TAG, "Gagal membuat direktori. Exit Code: " + result.exitCode);
						Log.e(TAG, "Stderr:\n" + result.stderr);
						Log.w(TAG, "Stdout:\n" + result.stdout);
						return false;
					}
			}
	}

