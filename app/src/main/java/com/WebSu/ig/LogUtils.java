package com.WebSu.ig;

import android.content.Context;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogUtils {
        private static final String TAG = "WebSuLog";

        public static void writeLog(Context context, String type, String message) {
                BufferedWriter bw = null;
                try {
                        File externalDir = context.getExternalFilesDir(null);
                        if (externalDir == null) {
                                Log.e(TAG, "Gagal menulis log: External storage tidak tersedia.");
                                return;
                            }

                        File logDir = new File(externalDir, "logs");
                        if (!logDir.exists()) logDir.mkdirs();
                        File logFile = new File(logDir, "sosc.log");

                        try {
                                bw = new BufferedWriter(new FileWriter(logFile, true));
                                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                bw.write("[" + time + "] [" + type + "] " + message);
                                bw.newLine();

                                Log.d(TAG, "Log written to sosc.log: [" + type + "] " + message); 
                            } finally {
                                if (bw != null) bw.close();
                            }
                    } catch (Exception e) {
                        Log.e(TAG, "Gagal menulis log ke sosc.log", e);
                    }
            }
    }
