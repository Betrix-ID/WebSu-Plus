package com.WebSu.ig;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.WebSu.ig.ShellExecutor; 

public class BusyBoxExecutor {
    private static String busyboxPath = null;

    private static final String BINARY_NAME = "libbusybox.so";
    private static final String TARGET_DIR = " /data/user_de/0/com.android.shell/WebSu/vbin";
    private static final String TARGET_NAME = "busybox";
    private static final String TARGET_PATH = TARGET_DIR + "/" + TARGET_NAME;

    public static void initializePath(Context context) {
        if (busyboxPath != null && busyboxPath.equals(TARGET_PATH)) {
            return;
        }

        String sourcePath = null;
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            String nativeLibraryDir = appInfo.nativeLibraryDir;
            sourcePath = nativeLibraryDir + "/" + BINARY_NAME;
        } catch (Exception e) {
            Log.e("WebSu", "Gagal mendapatkan nativeLibraryDir: " + e.getMessage());
            return;
        }

        if (sourcePath != null) {
            boolean success = copyAndSetPermissions(sourcePath, context);
            if (success) {
                busyboxPath = TARGET_PATH;
            } else {
                Log.e("WebSu", "Gagal menyalin atau mengatur izin Busybox. Busybox mungkin tidak dapat digunakan.");
            }
        }
    }
	
    public static String getPath() {
        return busyboxPath;
    }
	

    private static boolean copyAndSetPermissions(String sourcePath, Context context) {

        String setupCmd = "mkdir -p " + TARGET_DIR + " && chmod 777 " + TARGET_DIR;
        String copyCmd = "cp -f " + sourcePath + " " + TARGET_PATH + " && chmod 777 " + TARGET_PATH;

        try {
            ShellExecutor.Result setupRes = ShellExecutor.execSync(setupCmd, context);
            if (setupRes.exitCode != 0 && !setupRes.stderr.contains("File exists")) {
                Log.e("WebSu", "Gagal setup direktori. stderr: " + setupRes.stderr);
            }

            ShellExecutor.Result copyRes = ShellExecutor.execSync(copyCmd, context);
            if (copyRes.exitCode == 0) {
                Log.d("WebSu", "Busybox berhasil disalin dan diatur izinnya ke: " + TARGET_PATH);
                return true;
            } else {
                Log.e("WebSu", "Gagal menyalin Busybox. ExitCode=" + copyRes.exitCode + ", stderr: " + copyRes.stderr);
                return false;
            }
        } catch (Throwable t) {
            Log.e("WebSu", "Exception saat menyalin Busybox: " + t.getMessage(), t);
            return false;
        }
    }

    public static String run(String[] args, Context context) {
        if (busyboxPath == null || !busyboxPath.equals(TARGET_PATH)) {
            return "ERROR: Busybox path not properly initialized or copied.\n";
        }

        StringBuilder commandBuilder = new StringBuilder();
        commandBuilder.append(busyboxPath); 

        if (args != null) {
            for (String arg : args) {
                String cleanedArg = arg.replace("'", "'\\''");
                commandBuilder.append(" '").append(cleanedArg).append("'");
            }
        }

        final String busyboxCommand = commandBuilder.toString().trim();

        try {
            ShellExecutor.Result res = ShellExecutor.execSync(busyboxCommand, context);
            StringBuilder output = new StringBuilder();

            if (!res.stdout.isEmpty()) {
                output.append(res.stdout); 
            }

            if (!res.stderr.isEmpty()) {
  
                if (output.length() > 0 && !output.toString().endsWith("\n")) {
                    output.append("\n");
                }

                output.append(res.stderr);
                if (!output.toString().endsWith("\n")) {
                    output.append("\n");
                }
            }

            if (output.length() == 0 && res.exitCode != 0) {
				return "Busybox executed with exit code: " + res.exitCode + "\n";
            }
            return output.toString();

        } catch (Throwable e) {
            return "ERROR executing Busybox via ShellExecutor: " + e.getMessage() + "\n";
        }
    }
}

