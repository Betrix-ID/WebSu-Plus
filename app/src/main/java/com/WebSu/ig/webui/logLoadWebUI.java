package com.WebSu.ig.webui;

import android.content.Context;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class logLoadWebUI {
        private static final String TAG = "WebSuLog";

        public static void writeLog(Context context, String type, String message) {
                String callerClassName = "Unknown";
                try {
                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                        if (stackTrace != null && stackTrace.length >= 4) {
                                String fullClassName = stackTrace[3].getClassName();
                                callerClassName = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
                            }
                    } catch (Exception e) {
                        callerClassName = "UnknownClass";
                    }

                BufferedWriter bw = null;
                try {
                        File externalDir = context.getExternalFilesDir(null);
                        if (externalDir == null) {
                                Log.e(TAG, "Gagal menulis log: External storage tidak tersedia.");
                                return;
                            }

                        File logDir = new File(externalDir, "logs");
                        if (!logDir.exists()) logDir.mkdirs();
                        File logFile = new File(logDir, "webui.log");

                        try {
                                bw = new BufferedWriter(new FileWriter(logFile, true));
                                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                String logContent = "[" + time + "] [" + callerClassName + "] [" + type + "] " + message;

                                bw.write(logContent);
                                bw.newLine();
                                
                                Log.d(TAG, logContent); 
                            } finally {
                                if (bw != null) bw.close();
                            }
                    } catch (Exception e) {
                        Log.e(TAG, "Gagal menulis log ke webui.log", e);
                    }
            }
    }
